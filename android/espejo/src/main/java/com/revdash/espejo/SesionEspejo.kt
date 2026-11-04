package com.revdash.espejo

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.revdash.espejo.protocolo.cabecera
import com.revdash.espejo.protocolo.leerToque
import com.revdash.espejo.protocolo.paquete
import com.revdash.espejo.protocolo.tamanoVideo
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

internal class SesionEspejo(
    private val contexto: Context,
    private val resultado: Int,
    private val permiso: Intent,
    private val alTerminar: () -> Unit,
) {
    private val bloqueo = Any()
    @Volatile private var detenido = false
    private var servidor: ServerSocket? = null
    private var cliente: Socket? = null
    private var proyeccion: MediaProjection? = null
    private var pantalla: VirtualDisplay? = null
    private val principal = Handler(Looper.getMainLooper())
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            detener()
            alTerminar()
        }
    }

    fun iniciar() {
        thread(name = "espejo-video") { ejecutar() }
    }

    fun detener() {
        synchronized(bloqueo) {
            detenido = true
            liberar("socket cliente") { cliente?.close() }
            liberar("socket servidor") { servidor?.close() }
        }
    }

    private fun ejecutar() {
        try {
            iniciarProyeccion()
            atenderClientes()
        } catch (error: Exception) {
            if (!detenido) Log.e(ETIQUETA, "No se pudo mantener el duplicado", error)
        } finally {
            detener()
            liberar("pantalla virtual") { pantalla?.release() }
            liberar("callback") { proyeccion?.unregisterCallback(callback) }
            liberar("proyección") { proyeccion?.stop() }
            principal.post(alTerminar)
        }
    }

    private fun iniciarProyeccion() {
        if (detenido) return
        val gestor = contexto.getSystemService(MediaProjectionManager::class.java)
        proyeccion = gestor.getMediaProjection(resultado, permiso)
        proyeccion?.registerCallback(callback, principal)
    }

    private fun atenderClientes() {
        val escucha = ServerSocket()
        synchronized(bloqueo) {
            if (detenido) {
                escucha.close()
                return
            }
            servidor = escucha
        }
        escucha.reuseAddress = true
        escucha.bind(InetSocketAddress(PUERTO))
        while (!detenido) atenderCliente(escucha.accept())
    }

    private fun atenderCliente(socket: Socket) {
        synchronized(bloqueo) {
            if (detenido) {
                socket.close()
                return
            }
            cliente = socket
        }
        try {
            socket.tcpNoDelay = true
            val (ancho, alto) = tamanoPantalla(contexto)
            transmitirCliente(socket, tamanoVideo(ancho, alto))
        } catch (error: Exception) {
            if (!detenido) Log.w(ETIQUETA, "Cliente desconectado", error)
        } finally {
            liberar("socket cliente") { socket.close() }
            synchronized(bloqueo) { cliente = null }
        }
    }

    private fun transmitirCliente(socket: Socket, video: Pair<Int, Int>) {
        val lector = thread(name = "espejo-toques") { leerToques(socket) }
        try {
            codificarCliente(socket, video.first, video.second)
        } finally {
            liberar("socket cliente") { socket.close() }
            lector.join()
            ServicioToques.activo?.cancelarTrazo()
        }
    }

    private fun codificarCliente(socket: Socket, ancho: Int, alto: Int) {
        val codificador = MediaCodec.createEncoderByType("video/avc")
        var superficie: Surface? = null
        try {
            codificador.configure(formatoVideo(ancho, alto), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            superficie = codificador.createInputSurface()
            codificador.start()
            conectarPantalla(superficie, ancho, alto)
            val salida = socket.getOutputStream()
            salida.write(cabecera(ancho, alto))
            enviarCuadros(codificador, socket, salida)
        } finally {
            liberar("superficie de pantalla") { pantalla?.surface = null }
            liberar("codificador") { codificador.release() }
            liberar("superficie de entrada") { superficie?.release() }
        }
    }

    private fun conectarPantalla(superficie: Surface, ancho: Int, alto: Int) {
        val existente = pantalla
        if (existente != null) {
            // Android 14 permite una sola pantalla virtual por permiso; cada cliente reemplaza su superficie.
            existente.resize(ancho, alto, contexto.resources.displayMetrics.densityDpi)
            existente.surface = superficie
            return
        }
        pantalla = checkNotNull(proyeccion).createVirtualDisplay(
            "RevDash Espejo", ancho, alto, contexto.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, superficie, null, principal,
        )
    }

    private fun enviarCuadros(codificador: MediaCodec, socket: Socket, salida: OutputStream) {
        val informacion = MediaCodec.BufferInfo()
        while (!detenido && !socket.isClosed) {
            val indice = codificador.dequeueOutputBuffer(informacion, 10_000)
            if (indice < 0) continue
            val reloj = System.currentTimeMillis()
            try {
                enviarBuffer(codificador, indice, informacion, reloj, salida)
            } finally {
                codificador.releaseOutputBuffer(indice, false)
            }
        }
    }

    private fun enviarBuffer(codificador: MediaCodec, indice: Int, informacion: MediaCodec.BufferInfo, reloj: Long, salida: OutputStream) {
        val buffer = checkNotNull(codificador.getOutputBuffer(indice))
        buffer.position(informacion.offset)
        buffer.limit(informacion.offset + informacion.size)
        val datos = ByteArray(informacion.size)
        buffer.get(datos)
        salida.write(paquete(tipoPaquete(informacion.flags), reloj, datos, 0, datos.size))
    }

    private fun leerToques(socket: Socket) {
        try {
            val entrada = DataInputStream(socket.getInputStream())
            val bytes = ByteArray(9)
            while (!detenido && !socket.isClosed) {
                entrada.readFully(bytes)
                leerToque(bytes)?.let { ServicioToques.activo?.recibir(it) }
            }
        } catch (_: IOException) {
            // Cerrar el socket también desbloquea el escritor cuando el receptor deja de leer.
        } finally {
            liberar("socket de toques") { socket.close() }
        }
    }

    private fun formatoVideo(ancho: Int, alto: Int): MediaFormat = MediaFormat.createVideoFormat("video/avc", ancho, alto).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
    }

    private fun tipoPaquete(flags: Int): Int {
        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return 0
        if (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) return 2
        return 1
    }

    private fun liberar(recurso: String, accion: () -> Unit) {
        try {
            accion()
        } catch (error: Exception) {
            Log.w(ETIQUETA, "No se pudo liberar $recurso", error)
        }
    }

    private companion object {
        const val PUERTO = 27183
        const val ETIQUETA = "RevDashEspejo"
    }
}

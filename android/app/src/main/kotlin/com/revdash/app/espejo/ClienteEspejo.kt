package com.revdash.app.espejo

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

enum class EstadoEspejo { CONECTANDO, VIENDO, ERROR, CERRADO }

class ClienteEspejo(private val alRecibirCabecera: (CabeceraRde1) -> Unit = {}) {
    private val principal = Handler(Looper.getMainLooper())
    @Volatile private var sesion: SesionEspejo? = null

    @Synchronized
    fun conectar(host: String, puerto: Int, superficie: Surface, alCambiarEstado: (EstadoEspejo) -> Unit) {
        sesion?.cerrar()
        val nueva = SesionEspejo(superficie, alCambiarEstado)
        sesion = nueva
        nueva.conectar(host, puerto)
    }

    fun enviarToque(accion: Int, x: Float, y: Float) {
        sesion?.enviarToque(toque(accion, x, y))
    }

    @Synchronized
    fun cerrar() {
        sesion?.cerrar()
    }

    private inner class SesionEspejo(
        private val superficie: Surface,
        private val alCambiarEstado: (EstadoEspejo) -> Unit,
    ) {
        private val socket = Socket()
        private val cerrada = AtomicBoolean(false)
        private val toques = ArrayBlockingQueue<ByteArray>(256)
        @Volatile private var estado = EstadoEspejo.CONECTANDO
        private var escritor: Thread? = null

        fun conectar(host: String, puerto: Int) {
            publicarEstado()
            thread(name = "espejo-entrada", isDaemon = true) { recibir(host, puerto) }
        }

        fun enviarToque(bytes: ByteArray) {
            if (cerrada.get()) return
            if (!toques.offer(bytes)) fallar(IOException("Cola de toques llena"))
        }

        fun cerrar() {
            terminar(EstadoEspejo.CERRADO)
        }

        private fun recibir(host: String, puerto: Int) {
            var codec: MediaCodec? = null
            var salida: Thread? = null
            try {
                abrirSocket(host, puerto)
                val entrada = DataInputStream(socket.getInputStream())
                val cabecera = recibirCabecera(entrada)
                codec = crearDecodificador(cabecera)
                salida = iniciarSalida(codec)
                iniciarEscritor()
                recibirPaquetes(entrada, codec)
            } catch (error: Exception) {
                fallar(error)
            } finally {
                terminar(EstadoEspejo.CERRADO)
                esperarSalida(salida)
                liberarDecodificador(codec)
            }
        }

        private fun abrirSocket(host: String, puerto: Int) {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, puerto), 5_000)
        }

        private fun recibirCabecera(entrada: DataInputStream): CabeceraRde1 {
            val cabecera = leerCabecera(entrada.leerBytes(8)) ?: throw IOException("Cabecera RDE1 inválida")
            if (cabecera.ancho <= 0 || cabecera.alto <= 0) throw IOException("Dimensiones RDE1 inválidas")
            principal.post {
                if (sesion === this && !cerrada.get()) alRecibirCabecera(cabecera)
            }
            return cabecera
        }

        private fun crearDecodificador(cabecera: CabeceraRde1): MediaCodec {
            val formato = MediaFormat.createVideoFormat("video/avc", cabecera.ancho, cabecera.alto)
            if (Build.VERSION.SDK_INT >= 30) formato.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            val codec = MediaCodec.createDecoderByType("video/avc")
            try {
                codec.configure(formato, superficie, null, 0)
                codec.start()
                return codec
            } catch (error: Exception) {
                codec.release()
                throw error
            }
        }

        private fun recibirPaquetes(entrada: DataInputStream, codec: MediaCodec) {
            while (!cerrada.get()) {
                val encabezado = leerEncabezado(entrada.leerBytes(13))
                    ?: throw IOException("Encabezado RDE1 inválido")
                val datos = entrada.leerBytes(encabezado.largo)
                alimentarDecodificador(codec, encabezado, datos)
            }
        }

        private fun alimentarDecodificador(codec: MediaCodec, encabezado: EncabezadoPaquete, datos: ByteArray) {
            val indice = esperarEntrada(codec) ?: return
            val buffer = codec.getInputBuffer(indice) ?: throw IOException("Buffer de entrada ausente")
            buffer.clear()
            if (datos.size > buffer.remaining()) throw IOException("Paquete mayor que el buffer del decodificador")
            buffer.put(datos)
            val banderas = if (encabezado.tipo == 0) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
            codec.queueInputBuffer(indice, 0, datos.size, encabezado.relojMs * 1000, banderas)
        }

        private fun esperarEntrada(codec: MediaCodec): Int? {
            while (!cerrada.get()) {
                val indice = codec.dequeueInputBuffer(10_000)
                if (indice >= 0) return indice
            }
            return null
        }

        private fun iniciarSalida(codec: MediaCodec): Thread =
            thread(name = "espejo-salida", isDaemon = true) {
                try {
                    drenarSalida(codec)
                } catch (error: Exception) {
                    fallar(error)
                }
            }

        private fun drenarSalida(codec: MediaCodec) {
            val informacion = MediaCodec.BufferInfo()
            while (!cerrada.get()) {
                val indice = codec.dequeueOutputBuffer(informacion, 10_000)
                if (indice >= 0) renderizar(codec, indice)
            }
        }

        private fun renderizar(codec: MediaCodec, indice: Int) {
            codec.releaseOutputBuffer(indice, true)
            synchronized(this) {
                if (cerrada.get() || estado == EstadoEspejo.VIENDO) return
                estado = EstadoEspejo.VIENDO
                publicarEstado()
            }
        }

        @Synchronized
        private fun iniciarEscritor() {
            if (cerrada.get()) return
            escritor = thread(name = "espejo-toques", isDaemon = true) {
                try {
                    escribirToques()
                } catch (error: Exception) {
                    fallar(error)
                }
            }
        }

        private fun escribirToques() {
            val salida = socket.getOutputStream()
            while (!cerrada.get()) salida.write(toques.take())
        }

        private fun fallar(error: Exception) {
            if (terminar(EstadoEspejo.ERROR)) Log.w("ClienteEspejo", "Falló la sesión RDE1", error)
        }

        @Synchronized
        private fun terminar(final: EstadoEspejo): Boolean {
            if (!cerrada.compareAndSet(false, true)) return false
            estado = final
            runCatching { socket.close() }.onFailure { Log.w("ClienteEspejo", "No se pudo cerrar el socket", it) }
            escritor?.interrupt()
            toques.clear()
            publicarEstado()
            return true
        }

        private fun publicarEstado() {
            val nuevo = estado
            principal.post {
                if (sesion === this && estado == nuevo) alCambiarEstado(nuevo)
            }
        }
    }
}

private fun DataInputStream.leerBytes(largo: Int): ByteArray = ByteArray(largo).also { readFully(it) }

private fun esperarSalida(salida: Thread?) {
    if (salida == null) return
    var interrumpido = Thread.interrupted()
    while (salida.isAlive) {
        try {
            salida.join()
        } catch (_: InterruptedException) {
            interrumpido = true
        }
    }
    if (interrumpido) Thread.currentThread().interrupt()
}

private fun liberarDecodificador(codec: MediaCodec?) {
    if (codec == null) return
    runCatching { codec.stop() }.onFailure { Log.w("ClienteEspejo", "No se pudo detener el decodificador", it) }
    runCatching { codec.release() }.onFailure { Log.w("ClienteEspejo", "No se pudo liberar el decodificador", it) }
}

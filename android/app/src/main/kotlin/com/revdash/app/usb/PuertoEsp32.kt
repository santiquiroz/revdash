package com.revdash.app.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

fun lineasUsb(contexto: Context, baudios: Int = 921600): Flow<String> = callbackFlow {
    val app = contexto.applicationContext
    val administrador = app.getSystemService(UsbManager::class.java)
    val puerto = primerPuerto(administrador)
    if (puerto == null) {
        close()
        return@callbackFlow
    }
    val sesion = SesionUsb(app, administrador, puerto, baudios, this)
    try {
        sesion.iniciar()
    } catch (error: Exception) {
        sesion.terminarPorError(error)
    }
    awaitClose { sesion.cerrar() }
}.flowOn(Dispatchers.IO)

fun hayEsp32(contexto: Context): Boolean =
    primerPuerto(contexto.getSystemService(UsbManager::class.java)) != null

private fun primerPuerto(administrador: UsbManager): UsbSerialPort? =
    UsbSerialProber.getDefaultProber().findAllDrivers(administrador)
        .firstNotNullOfOrNull { it.ports.firstOrNull() }

private class SesionUsb(
    private val contexto: Context,
    private val administrador: UsbManager,
    private val puerto: UsbSerialPort,
    private val baudios: Int,
    private val salida: ProducerScope<String>,
) {
    private val bloqueo = Any()
    private val accionPermiso = "${contexto.packageName}.PERMISO_USB.${UUID.randomUUID()}"
    private var receptorRegistrado = false
    private var cerrada = false
    private var solicitud: PendingIntent? = null
    private var conexion: UsbDeviceConnection? = null
    private var lector: SerialInputOutputManager? = null
    private var recibidas = 0L
    private val lineas = SeparadorLineas { linea ->
        if (recibidas++ % MUESTREO_REGISTRO == 0L) Log.d("PuertoEsp32", "línea $recibidas: $linea")
        val envio = salida.trySend(linea)
        if (envio.isFailure && !envio.isClosed) {
            terminarPorError(IOException("No se pudo entregar una línea USB"))
        }
    }
    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(contexto: Context, intento: Intent) {
            if (intento.action != accionPermiso) return
            if (!intento.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                salida.close()
                return
            }
            salida.launch(Dispatchers.IO) { abrirConPermiso() }
        }
    }

    fun iniciar() = synchronized(bloqueo) {
        if (administrador.hasPermission(puerto.driver.device)) abrirPuerto() else pedirPermiso()
    }

    private fun pedirPermiso() {
        ContextCompat.registerReceiver(
            contexto, receptor, IntentFilter(accionPermiso), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receptorRegistrado = true
        val banderas = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val intento = Intent(accionPermiso).setPackage(contexto.packageName)
        solicitud = PendingIntent.getBroadcast(contexto, 0, intento, banderas)
        administrador.requestPermission(puerto.driver.device, solicitud)
    }

    private fun abrirConPermiso() = synchronized(bloqueo) {
        if (cerrada || lector != null) return@synchronized
        try {
            abrirPuerto()
        } catch (error: Exception) {
            terminarPorError(error)
        }
    }

    private fun abrirPuerto() {
        conexion = administrador.openDevice(puerto.driver.device)
            ?: throw IOException("No se pudo abrir el dispositivo USB")
        puerto.open(conexion)
        configurarPuerto()
        val nuevoLector = SerialInputOutputManager(puerto, escucha())
        lector = nuevoLector
        nuevoLector.start()
        Log.i("PuertoEsp32", "Puerto abierto a $baudios baudios")
    }

    private fun configurarPuerto() {
        puerto.setParameters(baudios, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        puerto.dtr = false
        puerto.rts = false
    }

    private fun escucha() = object : SerialInputOutputManager.Listener {
        override fun onNewData(datos: ByteArray) = lineas.recibir(datos)

        override fun onRunError(error: Exception) {
            terminarPorError(error)
        }
    }

    fun terminarPorError(error: Exception) {
        Log.w("PuertoEsp32", "Se interrumpió la conexión USB", error)
        salida.close()
    }

    fun cerrar() = synchronized(bloqueo) {
        if (cerrada) return@synchronized
        cerrada = true
        limpiar { lector?.stop() }
        limpiar { puerto.close() }
        limpiar { conexion?.close() }
        limpiar { solicitud?.cancel() }
        if (receptorRegistrado) limpiar { contexto.unregisterReceiver(receptor) }
    }

    private fun limpiar(accion: () -> Unit) {
        try {
            accion()
        } catch (error: Exception) {
            Log.w("PuertoEsp32", "No se pudo liberar un recurso USB", error)
        }
    }
}

private const val MUESTREO_REGISTRO = 200L

private class SeparadorLineas(private val emitir: (String) -> Unit) {
    private val pendiente = StringBuilder()
    private var descartando = false

    fun recibir(datos: ByteArray) {
        datos.forEach { recibirCaracter((it.toInt() and 0xff).toChar()) }
    }

    private fun recibirCaracter(caracter: Char) {
        when (caracter) {
            '\n' -> terminarLinea()
            '\r' -> Unit
            else -> agregarCaracter(caracter)
        }
    }

    private fun terminarLinea() {
        if (!descartando) emitir(pendiente.toString())
        pendiente.setLength(0)
        descartando = false
    }

    private fun agregarCaracter(caracter: Char) {
        if (descartando) return
        if (pendiente.length == 256) {
            pendiente.setLength(0)
            descartando = true
            return
        }
        pendiente.append(caracter)
    }
}

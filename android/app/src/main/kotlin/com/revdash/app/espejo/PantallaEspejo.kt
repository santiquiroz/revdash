package com.revdash.app.espejo

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Texto
import com.revdash.app.ui.BotonNavegacion
import kotlin.math.roundToInt

object ConfigEspejo {
    const val HOST = "127.0.0.1"
    const val PUERTO = 27183
}

@Composable
fun PantallaEspejo(alVolver: () -> Unit, modifier: Modifier = Modifier) {
    var estado by remember { mutableStateOf(EstadoEspejo.CONECTANDO) }
    val contexto = LocalContext.current
    val vista = remember(contexto) { VistaEspejo(contexto) { estado = it } }
    DisposableEffect(vista) { onDispose { vista.cerrar() } }
    BackHandler(onBack = alVolver)
    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize())
        if (estado != EstadoEspejo.VIENDO) {
            Text(
                textoEstado(estado), Modifier.align(Alignment.Center), color = Texto,
                fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 24.sp,
            )
        }
        BotonNavegacion("TABLERO", alVolver, Modifier.align(Alignment.BottomEnd).padding(24.dp))
    }
}

private fun textoEstado(estado: EstadoEspejo): String =
    when (estado) {
        EstadoEspejo.CONECTANDO -> "CONECTANDO"
        EstadoEspejo.VIENDO -> ""
        EstadoEspejo.ERROR -> "ERROR DE CONEXIÓN"
        EstadoEspejo.CERRADO -> "CONEXIÓN CERRADA"
    }

private class VistaEspejo(contexto: Context, alCambiarEstado: (EstadoEspejo) -> Unit) : FrameLayout(contexto) {
    private val superficie = SurfaceView(contexto)
    private var cabecera: CabeceraRde1? = null
    private val cliente = ClienteEspejo { cabecera = it; ajustarSuperficie() }
    private val gesto = GestoEspejo(cliente::enviarToque)
    private val cicloSuperficie = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            cliente.conectar(ConfigEspejo.HOST, ConfigEspejo.PUERTO, holder.surface, alCambiarEstado)
        }

        override fun surfaceChanged(holder: SurfaceHolder, formato: Int, ancho: Int, alto: Int) = Unit

        override fun surfaceDestroyed(holder: SurfaceHolder) = cerrarSesion()
    }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        addView(superficie, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        superficie.holder.addCallback(cicloSuperficie)
        superficie.setOnTouchListener { _, evento -> enviarToque(evento) }
    }

    override fun onSizeChanged(ancho: Int, alto: Int, anteriorAncho: Int, anteriorAlto: Int) {
        super.onSizeChanged(ancho, alto, anteriorAncho, anteriorAlto)
        ajustarSuperficie()
    }

    fun cerrar() {
        superficie.holder.removeCallback(cicloSuperficie)
        cerrarSesion()
    }

    private fun cerrarSesion() {
        gesto.limpiar()
        cliente.cerrar()
    }

    private fun ajustarSuperficie() {
        val video = cabecera ?: return
        val area = areaVideo(width, height, video.ancho, video.alto) ?: return
        superficie.layoutParams = LayoutParams(area.ancho.roundToInt(), area.alto.roundToInt(), Gravity.CENTER)
    }

    private fun enviarToque(evento: MotionEvent): Boolean {
        val accion = accionToque(evento.actionMasked) ?: return false
        val video = cabecera ?: return true
        val punto = aNormalizado(
            evento.x + superficie.left, evento.y + superficie.top,
            width, height, video.ancho, video.alto,
        )
        gesto.recibir(accion, punto)
        if (evento.actionMasked == MotionEvent.ACTION_UP) superficie.performClick()
        return true
    }
}

internal class GestoEspejo(private val alEnviar: (Int, Float, Float) -> Unit) {
    private var ultimoPunto: Pair<Float, Float>? = null

    fun recibir(accion: Int, punto: Pair<Float, Float>?) {
        when (accion) {
            0 -> iniciar(punto)
            1 -> mover(punto)
            2 -> terminar(punto)
        }
    }

    fun limpiar() {
        ultimoPunto = null
    }

    private fun iniciar(punto: Pair<Float, Float>?) {
        limpiar()
        if (punto == null) return
        enviar(0, punto)
    }

    private fun mover(punto: Pair<Float, Float>?) {
        if (ultimoPunto == null) return
        if (punto == null) return
        enviar(1, punto)
    }

    private fun terminar(punto: Pair<Float, Float>?) {
        val anterior = ultimoPunto ?: return
        enviar(2, punto ?: anterior)
        limpiar()
    }

    private fun enviar(accion: Int, punto: Pair<Float, Float>) {
        ultimoPunto = punto
        alEnviar(accion, punto.first, punto.second)
    }
}

private fun accionToque(accion: Int): Int? =
    when (accion) {
        MotionEvent.ACTION_DOWN -> 0
        MotionEvent.ACTION_MOVE -> 1
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> 2
        else -> null
    }

package com.revdash.espejo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.revdash.espejo.protocolo.EventoToque
import com.revdash.espejo.protocolo.aPixeles

class ServicioToques : AccessibilityService() {
    private val principal = Handler(Looper.getMainLooper())
    private var trazo: Path? = null
    private var inicio = 0L

    override fun onServiceConnected() {
        activo = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        desconectar()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        desconectar()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        cancelarTrazo()
    }

    fun recibir(evento: EventoToque) {
        val recibido = SystemClock.uptimeMillis()
        principal.post { aplicarEvento(evento, recibido) }
    }

    fun cancelarTrazo() {
        principal.post { trazo = null }
    }

    private fun aplicarEvento(evento: EventoToque, recibido: Long) {
        if (activo !== this) return
        val (ancho, alto) = tamanoPantalla(this)
        val (px, py) = aPixeles(evento, ancho, alto)
        val x = px.coerceIn(0f, (ancho - 1).toFloat())
        val y = py.coerceIn(0f, (alto - 1).toFloat())
        when (evento.accion) {
            0 -> iniciarTrazo(x, y, recibido)
            1 -> trazo?.lineTo(x, y)
            2 -> terminarTrazo(x, y, recibido)
        }
    }

    private fun iniciarTrazo(x: Float, y: Float, recibido: Long) {
        inicio = recibido
        trazo = Path().apply { moveTo(x, y) }
    }

    private fun terminarTrazo(x: Float, y: Float, recibido: Long) {
        val recorrido = trazo ?: return
        trazo = null
        recorrido.lineTo(x, y)
        val duracion = (recibido - inicio).coerceIn(1L, 10_000L)
        // ponytail: máximo 10 s y sin arrastre en vivo; continueStroke permitiría transmitirlo por partes.
        val gesto = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(recorrido, 0, duracion))
            .build()
        dispatchGesture(gesto, null, null)
    }

    private fun desconectar() {
        if (activo === this) activo = null
        principal.removeCallbacksAndMessages(null)
        trazo = null
    }

    companion object {
        @Volatile var activo: ServicioToques? = null
            private set
    }
}

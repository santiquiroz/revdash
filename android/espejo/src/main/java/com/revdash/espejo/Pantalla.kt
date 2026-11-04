package com.revdash.espejo

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

internal fun tamanoPantalla(contexto: Context): Pair<Int, Int> {
    val gestor = contexto.getSystemService(WindowManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val limites = gestor.currentWindowMetrics.bounds
        return limites.width() to limites.height()
    }
    return tamanoPantallaAnterior(gestor)
}

@Suppress("DEPRECATION")
private fun tamanoPantallaAnterior(gestor: WindowManager): Pair<Int, Int> {
    val tamano = Point()
    gestor.defaultDisplay.getRealSize(tamano)
    return tamano.x to tamano.y
}

package com.revdash.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val ANCHO_DISENO_DP = 1024f
private const val ALTO_DISENO_DP = 600f

fun escalaDiseno(anchoDp: Float, altoDp: Float): Float =
    minOf(anchoDp / ANCHO_DISENO_DP, altoDp / ALTO_DISENO_DP)

@Composable
fun EscalaDiseno(contenido: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val escala = escalaDiseno(maxWidth.value, maxHeight.value)
        // Tablero de tamaños fijos: la escala de fuente del sistema recortaría las tarjetas.
        val densidad = Density(LocalDensity.current.density * escala, fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides densidad) { contenido() }
    }
}

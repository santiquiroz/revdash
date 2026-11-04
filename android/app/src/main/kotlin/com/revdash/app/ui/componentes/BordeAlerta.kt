package com.revdash.app.ui.componentes

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Rojo
import com.revdash.app.ui.ModoPantalla
import com.revdash.nucleo.Alerta
import com.revdash.nucleo.LadoAlerta

@Composable
fun BordeAlerta(peligro: Alerta?, modifier: Modifier = Modifier, modo: ModoPantalla = ModoPantalla.COMPLETO) {
    if (peligro == null) return
    val transicion = rememberInfiniteTransition(label = "alerta")
    val pulso by transicion.animateFloat(1f, 0.25f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "pulso")
    val color = Rojo.copy(alpha = if (movimientoReducido()) 1f else pulso)
    Box(modifier) {
        when (peligro.lado) {
            LadoAlerta.IZQUIERDA, LadoAlerta.NINGUNO -> {
                Box(Modifier.align(Alignment.CenterStart).width(16.dp).fillMaxHeight().background(color))
                if (modo == ModoPantalla.COLUMNA) {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                } else {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = alturaEtiqueta(modo)))
                }
            }
            LadoAlerta.DERECHA -> {
                Box(Modifier.align(Alignment.CenterEnd).width(16.dp).fillMaxHeight().background(color))
                if (modo == ModoPantalla.COLUMNA) {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                } else {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = alturaEtiqueta(modo)))
                }
            }
            LadoAlerta.ATRAS -> {
                Box(Modifier.align(Alignment.BottomCenter).height(16.dp).fillMaxWidth().background(color))
                Etiqueta(peligro.texto, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
            }
            LadoAlerta.ADELANTE -> {
                Box(Modifier.align(Alignment.TopCenter).height(16.dp).fillMaxWidth().background(color))
                if (modo == ModoPantalla.FRANJA) {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopCenter).padding(top = 80.dp))
                } else {
                    Etiqueta(peligro.texto, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
                }
            }
        }
    }
}

// Etiqueta pegada al borde pero por debajo de la fila de direccionales y testigos,
// siempre arriba del número de velocidad. En COLUMNA va arriba del recuadro de
// Android Auto, no sobre la columna de instrumentos.
private fun alturaEtiqueta(modo: ModoPantalla) =
    when (modo) {
        ModoPantalla.COMPLETO -> 88.dp
        ModoPantalla.COLUMNA -> 8.dp
        ModoPantalla.FRANJA -> 80.dp
    }

@Composable
private fun Etiqueta(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier.background(Rojo).padding(horizontal = 14.dp, vertical = 8.dp),
        color = Color.Black,
        fontFamily = Barlow,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        letterSpacing = 1.8.sp,
    )
}

// Sin movimiento si el sistema tiene las animaciones desactivadas.
@Composable
private fun movimientoReducido(): Boolean {
    val contexto = LocalContext.current
    return remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

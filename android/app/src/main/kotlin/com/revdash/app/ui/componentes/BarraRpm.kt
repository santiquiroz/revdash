package com.revdash.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Ambar
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Fondo
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.Rojo
import com.revdash.app.tema.Texto
import com.revdash.app.tema.Verde
import com.revdash.nucleo.Lectura

const val MAX_RPM = 12_000.0

@Composable
fun BarraRpm(rpm: Lectura<Double>, modifier: Modifier = Modifier) {
    val fraccion =
        when (rpm) {
            Lectura.SinDato -> 0f
            is Lectura.Valor -> (rpm.valor / MAX_RPM).toFloat().coerceIn(0f, 1f)
            is Lectura.Viejo -> (rpm.valor / MAX_RPM).toFloat().coerceIn(0f, 1f)
        }
    val pincel = Brush.horizontalGradient(
        0f to Verde, 0.55f to Verde,
        0.55f to Ambar, 0.8f to Ambar,
        0.8f to Rojo,
    )
    Column(modifier) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).background(pincel),
        ) {
            val tapa = 1f - fraccion
            if (tapa > 0f) {
                Box(
                    Modifier.align(Alignment.CenterEnd).fillMaxWidth(tapa).fillMaxHeight()
                        .background(Fondo.copy(alpha = 0.85f)),
                ) {
                    if (fraccion > 0f) {
                        Box(Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight().background(Texto))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "2", "4", "6", "8", "10").forEach { marca ->
                Text(marca, color = Mudo, fontFamily = Barlow, fontSize = 13.sp)
            }
            Text("12 ×1000 rpm", color = Mudo, fontFamily = Barlow, fontSize = 13.sp)
        }
    }
}

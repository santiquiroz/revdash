package com.revdash.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Ambar
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.Panel
import com.revdash.app.tema.Rojo
import com.revdash.nucleo.EstadoDashcam

@Composable
fun ChipDashcam(estado: EstadoDashcam, modifier: Modifier = Modifier) {
    when (estado) {
        EstadoDashcam.Apagada -> return
        is EstadoDashcam.Grabando -> Pastilla(modifier) {
            Punto(Rojo)
            Text(
                "REC · ${estado.camaras} CÁM",
                color = Mudo,
                fontFamily = Barlow,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp,
            )
        }
        is EstadoDashcam.Falla -> Pastilla(modifier) {
            Punto(Ambar)
            Text(
                estado.texto,
                color = Ambar,
                fontFamily = Barlow,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
private fun Pastilla(modifier: Modifier, contenido: @Composable () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        contenido()
    }
}

@Composable
private fun Punto(color: Color) {
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

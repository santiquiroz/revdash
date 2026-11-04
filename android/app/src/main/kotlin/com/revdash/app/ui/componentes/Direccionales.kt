package com.revdash.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.FlechaApagada
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.Verde
import com.revdash.nucleo.Lectura

@Composable
fun Direccionales(
    izq: Lectura<Boolean>,
    der: Lectura<Boolean>,
    modifier: Modifier = Modifier,
    tamano: TextUnit = 44.sp,
    distribucion: Arrangement.Horizontal = Arrangement.spacedBy(24.dp),
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = distribucion) {
        FlechaDireccional(izquierda = true, lectura = izq, tamano = tamano)
        FlechaDireccional(izquierda = false, lectura = der, tamano = tamano)
    }
}

@Composable
fun FlechaDireccional(izquierda: Boolean, lectura: Lectura<Boolean>, tamano: TextUnit = 36.sp) {
    val color =
        when (lectura) {
            is Lectura.Valor -> if (lectura.valor) Verde else FlechaApagada
            is Lectura.Viejo -> if (lectura.valor) Mudo else FlechaApagada
            Lectura.SinDato -> FlechaApagada
        }
    Text(if (izquierda) "◀" else "▶", color = color, fontSize = tamano, fontWeight = FontWeight.Black)
}

package com.revdash.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Apagado
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.FondoApagado

@Composable
fun Testigo(
    texto: String,
    encendido: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 52.dp,
    textoTamano: TextUnit = 18.sp,
) {
    val fondo = if (encendido) color.copy(alpha = 0.18f) else FondoApagado
    val tinta = if (encendido) color else Apagado
    Box(
        modifier = modifier.size(tamano).clip(RoundedCornerShape(10.dp)).background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            color = tinta,
            fontFamily = Barlow,
            fontWeight = FontWeight.Bold,
            fontSize = textoTamano,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
        )
    }
}

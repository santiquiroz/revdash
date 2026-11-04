package com.revdash.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Ambar
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.BaseTabular
import com.revdash.app.tema.Condensada
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.Rojo
import com.revdash.app.tema.Texto
import com.revdash.app.ui.comoTexto
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.alertas.limiteSuperado

@Composable
fun VelocidadGrande(
    velocidad: Lectura<Double>,
    limiteKmh: Int?,
    numeroSp: TextUnit,
    modifier: Modifier = Modifier,
    unidadSp: TextUnit = 24.sp,
    insigniaDp: Dp = 64.dp,
) {
    val excede = limiteSuperado(velocidad, limiteKmh)
    val marco =
        if (excede) {
            Modifier.border(4.dp, Ambar, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp)
        } else {
            Modifier
        }
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(marco) {
            val estilo = BaseTabular.copy(
                fontFamily = Condensada,
                fontWeight = FontWeight.Bold,
                fontSize = numeroSp,
                lineHeight = numeroSp,
            )
            when (velocidad) {
                Lectura.SinDato -> Text("—", style = estilo.copy(color = Mudo))
                is Lectura.Valor -> Text(velocidad.comoTexto(), style = estilo.copy(color = Texto))
                is Lectura.Viejo -> Text(velocidad.comoTexto(), style = estilo.copy(color = Mudo))
            }
        }
        Column(
            Modifier.padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (limiteKmh != null) InsigniaLimite(limiteKmh, insigniaDp)
            Text(
                "km/h",
                color = Mudo,
                fontFamily = Barlow,
                fontWeight = FontWeight.SemiBold,
                fontSize = unidadSp,
                letterSpacing = 1.8.sp,
            )
        }
    }
}

// Señal europea de límite, como en la maqueta.
@Composable
private fun InsigniaLimite(limiteKmh: Int, diametro: Dp) {
    Box(
        Modifier.size(diametro).background(Color.White, CircleShape).border(diametro / 8, Rojo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            limiteKmh.toString(),
            color = Color.Black,
            fontFamily = Condensada,
            fontWeight = FontWeight.Bold,
            fontSize = (diametro.value * 0.4f).sp,
        )
    }
}

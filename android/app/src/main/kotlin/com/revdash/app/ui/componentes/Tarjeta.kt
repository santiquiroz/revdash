package com.revdash.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.Panel

@Composable
fun Tarjeta(titulo: String, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Panel).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            titulo,
            color = Mudo,
            fontFamily = Barlow,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 1.8.sp,
        )
        contenido()
    }
}

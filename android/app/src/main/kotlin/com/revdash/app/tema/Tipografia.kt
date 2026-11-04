package com.revdash.app.tema

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.revdash.app.R

val Condensada = FontFamily(
    Font(R.font.barlowcondensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlowcondensed_bold, FontWeight.Bold),
)

val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

// Cifras tabulares: la velocidad no baila al cambiar de dígito.
val BaseTabular = TextStyle(fontFamily = Barlow, fontFeatureSettings = "tnum")

package com.revdash.app.ui

import com.revdash.nucleo.Lectura
import java.util.Locale

fun Lectura<Double>.comoTexto(decimales: Int = 0): String =
    when (this) {
        Lectura.SinDato -> "—"
        is Lectura.Valor -> valor.formateado(decimales)
        is Lectura.Viejo -> valor.formateado(decimales)
    }

// Punto decimal siempre, venga de la configuración que venga.
private fun Double.formateado(decimales: Int): String =
    String.format(Locale.US, "%.${decimales}f", this)

fun Lectura<*>.esViejo(): Boolean = this is Lectura.Viejo

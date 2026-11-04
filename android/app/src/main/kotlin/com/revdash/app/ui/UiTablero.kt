package com.revdash.app.ui

import com.revdash.nucleo.AlertasVisibles
import com.revdash.nucleo.EstadoDashcam
import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Odometro

enum class ModoPantalla { COMPLETO, COLUMNA, FRANJA }

data class UiTablero(
    val estado: EstadoMoto = EstadoMoto(),
    val odometro: Odometro = Odometro(totalKm = 10_780.0),
    val alertas: AlertasVisibles = AlertasVisibles(),
    val dashcam: EstadoDashcam = EstadoDashcam.Apagada,
    val limiteKmh: Int? = null,
    val modo: ModoPantalla = ModoPantalla.COMPLETO,
    val hora: String = "--:--",
)

package com.revdash.nucleo.alertas

import com.revdash.nucleo.Alerta
import com.revdash.nucleo.AlertasVisibles
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.NivelAlerta

const val TOLERANCIA_LIMITE_KMH = 2.0

fun alertasVisibles(activas: List<Alerta>): AlertasVisibles =
    AlertasVisibles(
        peligro = activas.filter { it.nivel == NivelAlerta.P1 }.menorTtc(),
        advertencias = activas.filter { it.nivel == NivelAlerta.P2 },
        estados = activas.filter { it.nivel == NivelAlerta.P3 },
    )

fun limiteSuperado(velocidad: Lectura<Double>, limiteKmh: Int?): Boolean {
    if (limiteKmh == null) return false
    val valor = (velocidad as? Lectura.Valor) ?: return false
    return valor.valor > limiteKmh + TOLERANCIA_LIMITE_KMH
}

// Sin TTC cuenta como infinito; en empate minWithOrNull devuelve la primera.
private fun List<Alerta>.menorTtc(): Alerta? =
    minWithOrNull(compareBy { it.tiempoAlChoqueS ?: Double.POSITIVE_INFINITY })

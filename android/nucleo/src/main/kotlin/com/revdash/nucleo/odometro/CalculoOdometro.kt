package com.revdash.nucleo.odometro

import com.revdash.nucleo.MuestraVelocidad
import com.revdash.nucleo.Odometro

fun Odometro.con(muestra: MuestraVelocidad): Odometro {
    val anterior = ultimaMuestra ?: return copy(ultimaMuestra = muestra)
    val dtMs = muestra.tMs - anterior.tMs
    if (dtMs <= 0 || dtMs > HUECO_MAX_MS) return copy(ultimaMuestra = muestra)
    val sumaKm = (anterior.kmh + muestra.kmh) / 2 * dtMs / MS_POR_HORA
    return copy(totalKm = totalKm + sumaKm, viajeKm = viajeKm + sumaKm, ultimaMuestra = muestra)
}

fun Odometro.reiniciarViaje(): Odometro = copy(viajeKm = 0.0)

fun debePersistir(guardadoKm: Double, actual: Odometro): Boolean =
    actual.totalKm - guardadoKm >= UMBRAL_PERSISTENCIA_KM

private const val HUECO_MAX_MS = 1000L
private const val MS_POR_HORA = 3_600_000.0
private const val UMBRAL_PERSISTENCIA_KM = 0.1

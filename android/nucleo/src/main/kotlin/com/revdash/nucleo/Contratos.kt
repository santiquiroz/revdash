package com.revdash.nucleo

import kotlinx.coroutines.flow.Flow

class TramaCan(val tMs: Long, val id: Int, val datos: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is TramaCan && tMs == other.tMs && id == other.id && datos.contentEquals(other.datos)

    override fun hashCode(): Int = 31 * (31 * tMs.hashCode() + id) + datos.contentHashCode()

    override fun toString(): String =
        "TramaCan(tMs=$tMs, id=${id.toString(16)}, datos=${datos.joinToString("") { "%02X".format(it) }})"
}

fun interface FuenteTramas {
    fun tramas(): Flow<TramaCan>
}

sealed interface Lectura<out T> {
    data object SinDato : Lectura<Nothing>
    data class Valor<T>(val valor: T, val tMs: Long) : Lectura<T>
    data class Viejo<T>(val valor: T, val tMs: Long) : Lectura<T>
}

data class EstadoMoto(
    val velocidadKmh: Lectura<Double> = Lectura.SinDato,
    val rpm: Lectura<Double> = Lectura.SinDato,
    val marcha: Lectura<String> = Lectura.SinDato,
    val aceleradorPct: Lectura<Double> = Lectura.SinDato,
    val tempMotorC: Lectura<Double> = Lectura.SinDato,
    val tempAireC: Lectura<Double> = Lectura.SinDato,
    val pataAbajo: Lectura<Boolean> = Lectura.SinDato,
    val embrague: Lectura<Boolean> = Lectura.SinDato,
    val killSwitchRun: Lectura<Boolean> = Lectura.SinDato,
    val motorEnMarcha: Lectura<Boolean> = Lectura.SinDato,
    // Por cable, no por CAN: las llena el puente ESP32 (Fase 1) o el modo demo.
    val direccionalIzq: Lectura<Boolean> = Lectura.SinDato,
    val direccionalDer: Lectura<Boolean> = Lectura.SinDato,
    val luzAlta: Lectura<Boolean> = Lectura.SinDato,
    val checkEngine: Lectura<Boolean> = Lectura.SinDato,
    val abs: Lectura<Boolean> = Lectura.SinDato,
    val combustiblePct: Lectura<Double> = Lectura.SinDato,
)

data class Odometro(
    val totalKm: Double,
    val viajeKm: Double = 0.0,
    val ultimaMuestra: MuestraVelocidad? = null,
)

data class MuestraVelocidad(val kmh: Double, val tMs: Long)

enum class NivelAlerta { P1, P2, P3 }

enum class LadoAlerta { IZQUIERDA, DERECHA, ATRAS, ADELANTE, NINGUNO }

data class Alerta(
    val id: String,
    val nivel: NivelAlerta,
    val lado: LadoAlerta,
    val texto: String,
    val tiempoAlChoqueS: Double? = null,
)

data class AlertasVisibles(
    val peligro: Alerta? = null,
    val advertencias: List<Alerta> = emptyList(),
    val estados: List<Alerta> = emptyList(),
)

sealed interface EstadoDashcam {
    data object Apagada : EstadoDashcam
    data class Grabando(val camaras: Int) : EstadoDashcam
    data class Falla(val texto: String) : EstadoDashcam
}

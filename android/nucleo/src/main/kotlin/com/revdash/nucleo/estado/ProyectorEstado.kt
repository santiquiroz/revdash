package com.revdash.nucleo.estado

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.TramaCan
import com.revdash.nucleo.perfil.ClavesSenal
import com.revdash.nucleo.perfil.DecodificadorSenal
import com.revdash.nucleo.perfil.PerfilCan
import com.revdash.nucleo.perfil.SenalCan

data class ValorCrudo(val numero: Double, val texto: String?, val tMs: Long)

data class UltimosValores(val porClave: Map<String, ValorCrudo> = emptyMap())

class ProyectorEstado(
    private val perfil: PerfilCan,
    private val claveVelocidad: String = ClavesSenal.VELOCIDAD,
) {
    private val porClave: Map<String, SenalCan> = perfil.senales.associate { it.clave to it }

    fun aplicar(ultimos: UltimosValores, trama: TramaCan): UltimosValores {
        var mapa = ultimos.porClave
        for (senal in perfil.senales) {
            if (senal.id.toIntOrNull(16) != trama.id) continue
            val numero = DecodificadorSenal.valor(senal, trama.datos) ?: continue
            val texto = DecodificadorSenal.texto(senal, trama.datos)
            mapa = mapa + (senal.clave to ValorCrudo(numero, texto, trama.tMs))
        }
        return UltimosValores(mapa)
    }

    fun estado(ultimos: UltimosValores, ahoraMs: Long): EstadoMoto =
        EstadoMoto(
            velocidadKmh = velocidadFresca(ultimos, ahoraMs),
            rpm = numeroFresco(ultimos, ClavesSenal.RPM, ahoraMs),
            marcha = marchaFresca(ultimos, ahoraMs),
            aceleradorPct = numeroFresco(ultimos, ClavesSenal.ACELERADOR, ahoraMs),
            tempMotorC = numeroFresco(ultimos, ClavesSenal.TEMP_MOTOR, ahoraMs),
            tempAireC = numeroFresco(ultimos, ClavesSenal.TEMP_AIRE, ahoraMs),
            pataAbajo = banderaFresca(ultimos, ClavesSenal.PATA_LATERAL, ahoraMs),
            embrague = banderaFresca(ultimos, ClavesSenal.EMBRAGUE, ahoraMs),
            killSwitchRun = banderaFresca(ultimos, ClavesSenal.KILL_SWITCH, ahoraMs),
            motorEnMarcha = banderaFresca(ultimos, ClavesSenal.MOTOR_EN_MARCHA, ahoraMs),
        )

    private fun velocidadFresca(ultimos: UltimosValores, ahoraMs: Long): Lectura<Double> {
        if (porClave[claveVelocidad] == null) return Lectura.SinDato
        val crudo = ultimos.porClave[claveVelocidad] ?: return Lectura.SinDato
        return if (ahoraMs - crudo.tMs > VELOCIDAD_MAX_MS) Lectura.SinDato
        else Lectura.Valor(crudo.numero, crudo.tMs)
    }

    private fun numeroFresco(ultimos: UltimosValores, clave: String, ahoraMs: Long): Lectura<Double> {
        val crudo = ultimos.porClave[clave] ?: return Lectura.SinDato
        val senal = porClave[clave] ?: return Lectura.SinDato
        return if (ahoraMs - crudo.tMs > limiteViejoMs(senal)) Lectura.Viejo(crudo.numero, crudo.tMs)
        else Lectura.Valor(crudo.numero, crudo.tMs)
    }

    private fun marchaFresca(ultimos: UltimosValores, ahoraMs: Long): Lectura<String> {
        val crudo = ultimos.porClave[ClavesSenal.GEAR] ?: return Lectura.SinDato
        val senal = porClave[ClavesSenal.GEAR] ?: return Lectura.SinDato
        val texto = crudo.texto ?: return Lectura.SinDato
        return if (ahoraMs - crudo.tMs > limiteViejoMs(senal)) Lectura.Viejo(texto, crudo.tMs)
        else Lectura.Valor(texto, crudo.tMs)
    }

    private fun banderaFresca(ultimos: UltimosValores, clave: String, ahoraMs: Long): Lectura<Boolean> =
        when (val lectura = numeroFresco(ultimos, clave, ahoraMs)) {
            is Lectura.Valor -> Lectura.Valor(lectura.valor != 0.0, lectura.tMs)
            is Lectura.Viejo -> Lectura.Viejo(lectura.valor != 0.0, lectura.tMs)
            is Lectura.SinDato -> Lectura.SinDato
        }

    private fun limiteViejoMs(senal: SenalCan): Long = 3 * maxOf(senal.periodoMs, PERIODO_MIN_MS)

    private companion object {
        const val VELOCIDAD_MAX_MS = 500L
        const val PERIODO_MIN_MS = 50L
    }
}

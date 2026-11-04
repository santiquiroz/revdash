package com.revdash.nucleo.tramas

import com.revdash.nucleo.TramaCan
import java.math.BigDecimal
import java.math.RoundingMode

object ParserCandump {

    private val marcaValida = Regex("""\((\d+\.\d+)\)""")
    private val idValido = Regex("[0-9A-Fa-f]{3}|[0-9A-Fa-f]{8}")
    private val datosValidos = Regex("[0-9A-Fa-f]*")

    fun linea(texto: String): TramaCan? {
        val partes = texto.trim().split(Regex("\\s+"))
        if (partes.size != 3) return null
        val tMs = milis(partes[0]) ?: return null
        val (id, datos) = trama(partes[2]) ?: return null
        return TramaCan(tMs, id, datos)
    }

    private fun milis(marca: String): Long? {
        val segundos = marcaValida.matchEntire(marca)?.groupValues?.get(1) ?: return null
        // BigDecimal en vez de Double para no perder el milisegundo en marcas grandes.
        return try {
            BigDecimal(segundos).multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    private fun trama(cruda: String): Pair<Int, ByteArray>? {
        val idYDatos = cruda.split('#')
        if (idYDatos.size != 2) return null
        val (idTexto, datosTexto) = idYDatos
        if (!idValido.matches(idTexto)) return null
        if (datosTexto.equals("R", ignoreCase = true)) return null
        if (datosTexto.length > 16 || datosTexto.length % 2 != 0 || !datosValidos.matches(datosTexto)) return null
        val id = try {
            idTexto.toInt(16)
        } catch (_: NumberFormatException) {
            return null
        }
        val datos = try {
            ByteArray(datosTexto.length / 2) { i -> datosTexto.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (_: NumberFormatException) {
            return null
        }
        return id to datos
    }
}

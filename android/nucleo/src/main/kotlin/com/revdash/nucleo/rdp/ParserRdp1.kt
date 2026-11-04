package com.revdash.nucleo.rdp

object ParserRdp1 {
    private val patron = Regex("([HSEFCK]),(\\d+)")
    private val aridad = mapOf('H' to 2, 'S' to 1, 'E' to 2, 'F' to 2, 'C' to 3, 'K' to 1)
    private const val ID_MAX = 0x1FFFFFFFL

    fun linea(texto: String): EventoRdp? {
        try {
            val limpio = texto.trim()
            val hallazgo = patron.find(limpio) ?: return null
            val tipo = hallazgo.groupValues[1][0]
            val ms = hallazgo.groupValues[2].toLongOrNull() ?: return null
            val resto = limpio.substring(hallazgo.range.last + 1)
            if (resto.isNotEmpty() && !resto.startsWith(",")) return null
            val campos = if (resto.isEmpty()) emptyList() else partir(resto.substring(1))
            if (campos.size != aridad[tipo]) return null
            return when (tipo) {
                'H' -> hola(ms, campos)
                'S' -> estado(ms, campos)
                'E' -> flanco(ms, campos)
                'F' -> frecuencia(ms, campos)
                'C' -> can(ms, campos)
                else -> kline(ms, campos)
            }
        } catch (_: Exception) {
            return null
        }
    }

    // split() de Kotlin descarta el campo vacío final ("x," pierde el ""); se parte a mano.
    private fun partir(cuerpo: String): List<String> {
        val salida = mutableListOf<String>()
        var inicio = 0
        for (i in cuerpo.indices) {
            if (cuerpo[i] == ',') {
                salida.add(cuerpo.substring(inicio, i))
                inicio = i + 1
            }
        }
        salida.add(cuerpo.substring(inicio))
        return salida
    }

    private fun Char.esHex(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun hexADatos(texto: String, maxBytes: Int): ByteArray? {
        if (texto.length % 2 != 0) return null
        if (texto.length / 2 > maxBytes) return null
        if (texto.any { !it.esHex() }) return null
        val salida = ByteArray(texto.length / 2)
        for (i in salida.indices) {
            val alto = texto[2 * i].digitToIntOrNull(16) ?: return null
            val bajo = texto[2 * i + 1].digitToIntOrNull(16) ?: return null
            salida[i] = ((alto shl 4) or bajo).toByte()
        }
        return salida
    }

    private fun hola(ms: Long, campos: List<String>): EventoRdp? {
        if (campos[0] != "RDP1" || campos[1].isEmpty()) return null
        return EventoRdp.Hola(ms, campos[1])
    }

    private fun estado(ms: Long, campos: List<String>): EventoRdp? {
        val perdidas = campos[0].toLongOrNull() ?: return null
        if (perdidas < 0) return null
        return EventoRdp.Estado(ms, perdidas)
    }

    private fun canal(texto: String): Int? {
        val canal = texto.toIntOrNull() ?: return null
        if (canal < 0) return null
        return canal
    }

    private fun flanco(ms: Long, campos: List<String>): EventoRdp? {
        val canal = canal(campos[0]) ?: return null
        val nivel = when (campos[1]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        return EventoRdp.Flanco(ms, canal, nivel)
    }

    private fun frecuencia(ms: Long, campos: List<String>): EventoRdp? {
        val canal = canal(campos[0]) ?: return null
        val milliHz = campos[1].toLongOrNull() ?: return null
        if (milliHz < 0) return null
        return EventoRdp.Frecuencia(ms, canal, milliHz)
    }

    private fun can(ms: Long, campos: List<String>): EventoRdp? {
        val idTexto = campos[0]
        if (idTexto.isEmpty() || idTexto.any { !it.esHex() }) return null
        val id = idTexto.toLongOrNull(16) ?: return null
        if (id > ID_MAX) return null
        val extendida = when (campos[1]) {
            "x" -> true
            "s" -> false
            else -> return null
        }
        val datos = hexADatos(campos[2], maxBytes = 8) ?: return null
        return EventoRdp.Can(ms, id.toInt(), extendida, datos)
    }

    private fun kline(ms: Long, campos: List<String>): EventoRdp? {
        val datos = hexADatos(campos[0], maxBytes = Int.MAX_VALUE) ?: return null
        if (datos.isEmpty()) return null
        return EventoRdp.Kline(ms, datos)
    }
}

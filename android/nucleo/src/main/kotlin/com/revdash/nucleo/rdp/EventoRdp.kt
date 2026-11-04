package com.revdash.nucleo.rdp

sealed interface EventoRdp {
    data class Hola(val ms: Long, val version: String) : EventoRdp
    data class Estado(val ms: Long, val perdidas: Long) : EventoRdp
    data class Flanco(val ms: Long, val canal: Int, val nivel: Boolean) : EventoRdp
    data class Frecuencia(val ms: Long, val canal: Int, val milliHz: Long) : EventoRdp
    data class Can(val ms: Long, val id: Int, val extendida: Boolean, val datos: ByteArray) : EventoRdp {
        override fun equals(other: Any?): Boolean =
            other is Can && ms == other.ms && id == other.id &&
                extendida == other.extendida && datos.contentEquals(other.datos)

        override fun hashCode(): Int {
            var resultado = ms.hashCode()
            resultado = 31 * resultado + id
            resultado = 31 * resultado + extendida.hashCode()
            resultado = 31 * resultado + datos.contentHashCode()
            return resultado
        }
    }
    data class Kline(val ms: Long, val datos: ByteArray) : EventoRdp {
        override fun equals(other: Any?): Boolean =
            other is Kline && ms == other.ms && datos.contentEquals(other.datos)

        override fun hashCode(): Int = 31 * ms.hashCode() + datos.contentHashCode()
    }
}

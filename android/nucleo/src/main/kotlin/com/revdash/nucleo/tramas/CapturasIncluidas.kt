package com.revdash.nucleo.tramas

object CapturasIncluidas {

    fun empujando(): List<String> = leer("/capturas/empujando.log")

    fun aceleradorBarrido(): List<String> = leer("/capturas/acelerador_barrido.log")

    private fun leer(recurso: String): List<String> {
        val flujo = CapturasIncluidas::class.java.getResourceAsStream(recurso)
            ?: throw IllegalStateException("Recurso incluido no encontrado: $recurso")
        return flujo.bufferedReader(Charsets.UTF_8).use { it.readLines() }.filter { it.isNotBlank() }
    }
}

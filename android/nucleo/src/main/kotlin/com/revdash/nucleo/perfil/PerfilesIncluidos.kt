package com.revdash.nucleo.perfil

object PerfilesIncluidos {

    fun apache(): PerfilCan = CodecPerfilCan.leer(texto("/perfiles_can/tvs.apache_rtr_160_4v.2024.json")).getOrThrow()

    private fun texto(recurso: String): String {
        val flujo = PerfilesIncluidos::class.java.getResourceAsStream(recurso)
            ?: throw IllegalStateException("Recurso incluido no encontrado: $recurso")
        return flujo.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

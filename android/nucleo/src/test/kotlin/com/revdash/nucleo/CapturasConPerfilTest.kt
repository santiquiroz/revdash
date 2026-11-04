package com.revdash.nucleo

import com.revdash.nucleo.perfil.ClavesSenal
import com.revdash.nucleo.perfil.DecodificadorSenal
import com.revdash.nucleo.perfil.PerfilesIncluidos
import com.revdash.nucleo.tramas.CapturasIncluidas
import com.revdash.nucleo.tramas.ParserCandump
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturasConPerfilTest {

    private val perfil by lazy { PerfilesIncluidos.apache() }

    private fun valores(lineas: List<String>, clave: String): List<Double> {
        val senal = perfil.senales.first { it.clave == clave }
        val id = senal.id.toInt(16)
        return lineas.mapNotNull(ParserCandump::linea)
            .filter { it.id == id }
            .mapNotNull { DecodificadorSenal.valor(senal, it.datos) }
    }

    @Test
    fun `el acelerador barre de menos de 20 a mas de 99`() {
        val v = valores(CapturasIncluidas.aceleradorBarrido(), ClavesSenal.ACELERADOR)
        assertTrue("sin muestras del acelerador", v.isNotEmpty())
        assertTrue("minimo ${v.minOrNull()} > 20", v.minOrNull()!! <= 20.0)
        assertTrue("maximo ${v.maxOrNull()} < 99", v.maxOrNull()!! >= 99.0)
    }

    @Test
    fun `empujando la rueda llega a unos 6 kmh`() {
        val rueda = valores(CapturasIncluidas.empujando(), ClavesSenal.VELOCIDAD_RUEDA)
        assertTrue("sin muestras de rueda", rueda.isNotEmpty())
        assertTrue("maximo ${rueda.maxOrNull()} fuera de 5..7", rueda.maxOrNull()!! in 5.0..7.0)
        val obd = valores(CapturasIncluidas.empujando(), ClavesSenal.VELOCIDAD)
        assertTrue("sin muestras de velocidad", obd.isNotEmpty())
        assertTrue("maximo ${obd.maxOrNull()} fuera de 5..6", obd.maxOrNull()!! in 5.0..6.0)
    }

    @Test
    fun `toda linea de las capturas se interpreta`() {
        val todas = CapturasIncluidas.empujando() + CapturasIncluidas.aceleradorBarrido()
        assertTrue(todas.isNotEmpty())
        for (linea in todas) assertNotNull("sin interpretar: '$linea'", ParserCandump.linea(linea))
    }
}

package com.revdash.nucleo.tramas

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParserCandumpTest {

    @Test
    fun `linea valida de 11 bits`() {
        val trama = ParserCandump.linea("(1793640325.016000) can0 301#0100FFFF02010000")
        assertNotNull(trama)
        assertEquals(1793640325016L, trama!!.tMs)
        assertEquals(0x301, trama.id)
        assertArrayEquals(
            byteArrayOf(0x01, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0x02, 0x01, 0x00, 0x00),
            trama.datos,
        )
    }

    @Test
    fun `id de 29 bits`() {
        val trama = ParserCandump.linea("(1793640325.016000) can0 18DAF110#0201")
        assertNotNull(trama)
        assertEquals(0x18DAF110, trama!!.id)
        assertEquals(1793640325016L, trama.tMs)
        assertArrayEquals(byteArrayOf(0x02, 0x01), trama.datos)
    }

    @Test
    fun `trama sin datos`() {
        val trama = ParserCandump.linea("(1.000000) can0 123#")
        assertNotNull(trama)
        assertEquals(0x123, trama!!.id)
        assertEquals(1000L, trama.tMs)
        assertEquals(0, trama.datos.size)
    }

    @Test
    fun `ignora espacios al inicio y al final`() {
        val trama = ParserCandump.linea("  (0.010000) can0 301#0200  ")
        assertNotNull(trama)
        assertEquals(10L, trama!!.tMs)
        assertEquals(0x301, trama.id)
    }

    @Test
    fun `devuelve null ante lineas mal formadas`() {
        val casos = listOf(
            "",
            "   ",
            "(1.000000) can0 301",
            "(1.000000) can0 301#123",
            "(1.000000) can0 301#001122334455667788",
            "(1.000000) can0 3011#00",
            "(1.000000) can0 123#R",
            "basura total",
            "(1.000000) can0 30G#00",
        )
        for (caso in casos) assertNull("deberia ser null: '$caso'", ParserCandump.linea(caso))
    }
}

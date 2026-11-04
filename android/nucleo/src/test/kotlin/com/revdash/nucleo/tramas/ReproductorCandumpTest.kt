package com.revdash.nucleo.tramas

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReproductorCandumpTest {

    private val tresLineas = listOf(
        "(0.000000) can0 301#0100",
        "(0.010000) can0 301#0200",
        "(0.030000) can0 301#0300",
    )

    private fun hex(datos: ByteArray): String = datos.joinToString("") { "%02X".format(it) }

    @Test
    fun `emite con el reloj de emision y termina sin bucle`() {
        runTest {
            val reproductor = ReproductorCandump(tresLineas, bucle = false) { testScheduler.currentTime }
            val recibidas = reproductor.tramas().toList()
            assertEquals(listOf(0L, 10L, 30L), recibidas.map { it.tMs })
            assertEquals(listOf("0100", "0200", "0300"), recibidas.map { hex(it.datos) })
        }
    }

    @Test
    fun `con bucle repite desde el inicio`() {
        runTest {
            val reproductor = ReproductorCandump(tresLineas, bucle = true) { testScheduler.currentTime }
            val recibidas = reproductor.tramas().take(6).toList()
            assertEquals(
                listOf("0100", "0200", "0300", "0100", "0200", "0300"),
                recibidas.map { hex(it.datos) },
            )
        }
    }

    @Test
    fun `sin lineas validas termina sin emitir`() {
        runTest {
            val reproductor = ReproductorCandump(listOf("", "basura", "(1.0) can0 123#R"), bucle = false)
            assertTrue(reproductor.tramas().toList().isEmpty())
        }
    }
}

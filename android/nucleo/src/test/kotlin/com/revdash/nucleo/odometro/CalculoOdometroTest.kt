package com.revdash.nucleo.odometro

import com.revdash.nucleo.MuestraVelocidad
import com.revdash.nucleo.Odometro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculoOdometroTest {

    @Test
    fun `36 kmh constantes durante 10 s en pasos de 100 ms dan 0,1 km`() {
        var odo = Odometro(totalKm = 0.0).con(MuestraVelocidad(36.0, 0))
        repeat(100) { paso -> odo = odo.con(MuestraVelocidad(36.0, (paso + 1) * 100L)) }
        assertEquals(0.1, odo.totalKm, 1e-9)
        assertEquals(0.1, odo.viajeKm, 1e-9)
    }

    @Test
    fun `primera muestra solo se guarda`() {
        val muestra = MuestraVelocidad(36.0, 500)
        val odo = Odometro(totalKm = 7.0).con(muestra)
        assertEquals(7.0, odo.totalKm, 0.0)
        assertEquals(0.0, odo.viajeKm, 0.0)
        assertEquals(muestra, odo.ultimaMuestra)
    }

    @Test
    fun `hueco de 2 s no suma pero guarda la muestra`() {
        val base = Odometro(totalKm = 1.0, viajeKm = 0.5, ultimaMuestra = MuestraVelocidad(36.0, 1000))
        val trasHueco = base.con(MuestraVelocidad(36.0, 3000))
        assertEquals(1.0, trasHueco.totalKm, 0.0)
        assertEquals(0.5, trasHueco.viajeKm, 0.0)
        assertEquals(MuestraVelocidad(36.0, 3000), trasHueco.ultimaMuestra)
    }

    @Test
    fun `dt negativo no suma`() {
        val base = Odometro(totalKm = 1.0, viajeKm = 0.5, ultimaMuestra = MuestraVelocidad(36.0, 1000))
        val atras = base.con(MuestraVelocidad(36.0, 950))
        assertEquals(1.0, atras.totalKm, 0.0)
        assertEquals(0.5, atras.viajeKm, 0.0)
    }

    @Test
    fun `reiniciarViaje pone el viaje a 0 y deja el total`() {
        val odo = Odometro(totalKm = 5.0, viajeKm = 2.0).reiniciarViaje()
        assertEquals(5.0, odo.totalKm, 0.0)
        assertEquals(0.0, odo.viajeKm, 0.0)
    }

    @Test
    fun `debePersistir en 0,09 es falso y en 0,1 es verdadero`() {
        assertFalse(debePersistir(0.0, Odometro(totalKm = 0.09)))
        assertTrue(debePersistir(0.0, Odometro(totalKm = 0.1)))
    }
}

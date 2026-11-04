package com.revdash.nucleo.alertas

import com.revdash.nucleo.Alerta
import com.revdash.nucleo.AlertasVisibles
import com.revdash.nucleo.LadoAlerta
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.NivelAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeleccionAlertasTest {

    @Test
    fun `dos P1 gana la de menor TTC`() {
        val lenta = alerta("a", NivelAlerta.P1, ttc = 5.0)
        val urgente = alerta("b", NivelAlerta.P1, ttc = 2.0)
        assertEquals(urgente, alertasVisibles(listOf(lenta, urgente)).peligro)
    }

    @Test
    fun `en empate gana la primera y sin TTC pierde contra una con TTC`() {
        val primera = alerta("a", NivelAlerta.P1, ttc = 2.0)
        val segunda = alerta("b", NivelAlerta.P1, ttc = 2.0)
        assertEquals(primera, alertasVisibles(listOf(primera, segunda)).peligro)
        val sinTtc = alerta("c", NivelAlerta.P1)
        val conTtc = alerta("d", NivelAlerta.P1, ttc = 9.0)
        assertEquals(conTtc, alertasVisibles(listOf(sinTtc, conTtc)).peligro)
    }

    @Test
    fun `P2 y P3 van separadas y en orden`() {
        val p2a = alerta("a", NivelAlerta.P2)
        val p3 = alerta("b", NivelAlerta.P3)
        val p2b = alerta("c", NivelAlerta.P2)
        val p1 = alerta("d", NivelAlerta.P1, ttc = 1.0)
        val visibles = alertasVisibles(listOf(p2a, p3, p2b, p1))
        assertEquals(p1, visibles.peligro)
        assertEquals(listOf(p2a, p2b), visibles.advertencias)
        assertEquals(listOf(p3), visibles.estados)
    }

    @Test
    fun `lista vacia da visibles vacios`() {
        assertEquals(AlertasVisibles(), alertasVisibles(emptyList()))
    }

    @Test
    fun `limite sin dato o sin limite nunca supera`() {
        assertFalse(limiteSuperado(Lectura.SinDato, 60))
        assertFalse(limiteSuperado(Lectura.Viejo(70.0, 0), 60))
        assertFalse(limiteSuperado(Lectura.Valor(90.0, 0), null))
    }

    @Test
    fun `limite 61 con tope 60 no supera y 62,5 si`() {
        assertFalse(limiteSuperado(Lectura.Valor(61.0, 0), 60))
        assertFalse(limiteSuperado(Lectura.Valor(62.0, 0), 60))
        assertTrue(limiteSuperado(Lectura.Valor(62.5, 0), 60))
    }

    private companion object {
        fun alerta(id: String, nivel: NivelAlerta, ttc: Double? = null) =
            Alerta(id, nivel, LadoAlerta.NINGUNO, id, ttc)
    }
}

package com.revdash.app

import com.revdash.app.ui.comoTexto
import com.revdash.app.ui.esViejo
import com.revdash.nucleo.Lectura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatoTest {
    @Test
    fun sinDatoEsRaya() {
        assertEquals("—", Lectura.SinDato.comoTexto())
        assertEquals("—", Lectura.SinDato.comoTexto(2))
    }

    @Test
    fun valorSinDecimalesPorDefecto() {
        assertEquals("42", Lectura.Valor(42.0, 0).comoTexto())
    }

    @Test
    fun valorRespetaDecimales() {
        assertEquals("62.5", Lectura.Valor(62.5, 0).comoTexto(1))
        assertEquals("92.40", Lectura.Valor(92.4, 0).comoTexto(2))
    }

    @Test
    fun viejoMuestraElValor() {
        assertEquals("92", Lectura.Viejo(92.4, 0).comoTexto())
    }

    @Test
    fun soloViejoEsViejo() {
        assertFalse(Lectura.SinDato.esViejo())
        assertFalse(Lectura.Valor(1.0, 0).esViejo())
        assertTrue(Lectura.Viejo(1.0, 0).esViejo())
    }
}

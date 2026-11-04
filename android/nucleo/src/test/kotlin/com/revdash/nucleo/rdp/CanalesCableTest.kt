package com.revdash.nucleo.rdp

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Lectura
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue

class CanalesCableTest {
    @Test fun canalCoincidenteActualizaSoloEseCampo() {
        val salida = EstadoMoto().conFlanco(
            EventoRdp.Flanco(100, 3, true),
            CanalesCable(direccionalIzq = 3, direccionalDer = 4, luzAlta = 5),
            tMs = 200,
        )
        assertEquals(Lectura.Valor(true, 200), salida.direccionalIzq)
        assertTrue(salida.direccionalDer is Lectura.SinDato)
        assertTrue(salida.luzAlta is Lectura.SinDato)
    }

    @Test fun activoEnBajoInvierteElNivel() {
        val canales = CanalesCable(direccionalDer = 5, activoEnAlto = false)
        val apagado = EstadoMoto().conFlanco(EventoRdp.Flanco(6, 5, true), canales, tMs = 7)
        val encendido = EstadoMoto().conFlanco(EventoRdp.Flanco(6, 5, false), canales, tMs = 7)
        assertEquals(Lectura.Valor(false, 7), apagado.direccionalDer)
        assertEquals(Lectura.Valor(true, 7), encendido.direccionalDer)
    }

    @Test fun sinCanalCoincidenteDevuelveElMismoEstado() {
        val estado = EstadoMoto()
        assertSame(estado, estado.conFlanco(EventoRdp.Flanco(6, 9, true), CanalesCable(), tMs = 7))
    }
}

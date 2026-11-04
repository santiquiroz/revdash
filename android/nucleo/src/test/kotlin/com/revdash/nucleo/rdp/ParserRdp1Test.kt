package com.revdash.nucleo.rdp

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class ParserRdp1Test {
    @Test fun holaValido() {
        assertEquals(EventoRdp.Hola(0, "0.1.0"), ParserRdp1.linea("H,0,RDP1,0.1.0"))
    }

    @Test fun estadoValido() {
        assertEquals(EventoRdp.Estado(5000, 2), ParserRdp1.linea("S,5000,2"))
    }

    @Test fun flancoValido() {
        assertEquals(EventoRdp.Flanco(1234, 3, true), ParserRdp1.linea("E,1234,3,1"))
        assertEquals(EventoRdp.Flanco(10, 2, false), ParserRdp1.linea("E,10,2,0"))
    }

    @Test fun frecuenciaValida() {
        assertEquals(EventoRdp.Frecuencia(500, 0, 104000), ParserRdp1.linea("F,500,0,104000"))
    }

    @Test fun canEstandarValida() {
        assertEquals(
            EventoRdp.Can(10, 0x1A0, false, byteArrayOf(0x01, 0xAB.toByte())),
            ParserRdp1.linea("C,10,1A0,s,01AB"),
        )
    }

    @Test fun canExtendidaConDatosVacios() {
        assertEquals(
            EventoRdp.Can(10, 0x18DAF110, true, byteArrayOf()),
            ParserRdp1.linea("C,10,18DAF110,x,"),
        )
    }

    @Test fun klineValida() {
        assertEquals(
            EventoRdp.Kline(7, byteArrayOf(0xC1.toByte(), 0x33)),
            ParserRdp1.linea("K,7,C133"),
        )
    }

    @Test fun basuraAntesDelPatronSeIgnora() {
        assertEquals(
            EventoRdp.Frecuencia(500, 0, 104000),
            ParserRdp1.linea("12:01:02.345 F,500,0,104000"),
        )
        assertEquals(
            EventoRdp.Hola(0, "0.1.0"),
            ParserRdp1.linea("rst:0x1 (POWERON) H,0,RDP1,0.1.0"),
        )
    }

    @Test fun camposDeMasOMenosSonNull() {
        assertNull(ParserRdp1.linea("E,1,2,1,9"))
        assertNull(ParserRdp1.linea("S,1,2,3"))
        assertNull(ParserRdp1.linea("F,500,0"))
        assertNull(ParserRdp1.linea("H,0,RDP1"))
        assertNull(ParserRdp1.linea("C,10,1A0,s"))
        assertNull(ParserRdp1.linea("S,1"))
        assertNull(ParserRdp1.linea("K,7"))
    }

    @Test fun hexadecimalImparEsNull() {
        assertNull(ParserRdp1.linea("C,10,1A0,s,123"))
        assertNull(ParserRdp1.linea("K,7,ABC"))
    }

    @Test fun nivelDistintoDe0o1EsNull() {
        assertNull(ParserRdp1.linea("E,1,2,2"))
    }

    @Test fun canDe9BytesEsNull() {
        assertNull(ParserRdp1.linea("C,10,1A0,s,001122334455667788"))
    }

    @Test fun lineasMalformadasSonNull() {
        assertNull(ParserRdp1.linea(""))
        assertNull(ParserRdp1.linea("   "))
        assertNull(ParserRdp1.linea("Z,1,2"))
        assertNull(ParserRdp1.linea("E,abc,1,1"))
        assertNull(ParserRdp1.linea("C,10,ZZ,s,01"))
        assertNull(ParserRdp1.linea("C,10,1A0,q,01"))
        assertNull(ParserRdp1.linea("H,0,OTRO,1.0"))
    }
}

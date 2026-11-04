package com.revdash.nucleo.rdp

import com.revdash.nucleo.TramaCan
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class TramasDeRdpTest {
    @Test fun soloSalenLasCanConElTmsDelReloj() = runTest {
        val tiempos = listOf(100L, 200L)
        var indice = 0
        val salidas = tramasDeRdp(
            flowOf(
                "H,0,RDP1,0.1.0",
                "E,1,2,1",
                "C,10,1A0,s,01AB",
                "F,500,0,104000",
                "C,11,2B0,s,",
                "K,7,C133",
                "basura sin patron",
            ),
            reloj = { tiempos[indice++] },
        ).toList()
        assertEquals(
            listOf(
                TramaCan(100, 0x1A0, byteArrayOf(0x01, 0xAB.toByte())),
                TramaCan(200, 0x2B0, byteArrayOf()),
            ),
            salidas,
        )
    }
}

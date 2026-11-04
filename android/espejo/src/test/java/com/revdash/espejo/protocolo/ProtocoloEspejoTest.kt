package com.revdash.espejo.protocolo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ProtocoloEspejoTest {
    @Test
    fun reducePantallaVerticalConDimensionesPares() {
        assertEquals(590 to 1280, tamanoVideo(1440, 3120))
    }

    @Test
    fun reducePantallaHorizontalConDimensionesPares() {
        assertEquals(1280 to 590, tamanoVideo(3120, 1440))
    }

    @Test
    fun conservaVideoQueYaCabe() {
        assertEquals(1280 to 720, tamanoVideo(1280, 720))
    }

    @Test
    fun noAmpliaVideoPequeno() {
        assertEquals(100 to 100, tamanoVideo(100, 100))
    }

    @Test
    fun redondeaDimensionesImparesHaciaAbajo() {
        assertEquals(100 to 98, tamanoVideo(101, 99))
    }

    @Test
    fun respetaAmbosLimitesPersonalizados() {
        assertEquals(300 to 300, tamanoVideo(1000, 1000, 600, 300))
    }

    @Test
    fun rechazaDimensionesInvalidas() {
        assertThrows(IllegalArgumentException::class.java) { tamanoVideo(0, 100) }
        assertThrows(IllegalArgumentException::class.java) { tamanoVideo(100, -1) }
        assertThrows(IllegalArgumentException::class.java) { tamanoVideo(100, 100, 1, 1) }
    }

    @Test
    fun escribeCabeceraRde1ConEnterosBigEndian() {
        val esperado = byteArrayOf(0x52, 0x44, 0x45, 0x31, 0x02, 0x4e, 0x05, 0x00)
        assertArrayEquals(esperado, cabecera(590, 1280))
    }

    @Test
    fun escribeDimensionesU16SinExtensionDeSigno() {
        val esperado = byteArrayOf(0x52, 0x44, 0x45, 0x31, -128, 0, -1, -1)
        assertArrayEquals(esperado, cabecera(32768, 65535))
    }

    @Test
    fun rechazaDimensionesFueraDeU16() {
        assertThrows(IllegalArgumentException::class.java) { cabecera(65536, 720) }
        assertThrows(IllegalArgumentException::class.java) { cabecera(1280, 0) }
    }

    @Test
    fun escribePaqueteConRelojDe64BitsYSubrangoDeDatos() {
        val datos = byteArrayOf(0x55, 0x66, 0, 0, 0, 1, -128, -1, 0x77)
        val esperado = byteArrayOf(
            2,
            1, 2, 3, 4, 5, 6, 7, 8,
            0, 0, 0, 6,
            0, 0, 0, 1, -128, -1,
        )
        assertArrayEquals(esperado, paquete(2, 0x0102030405060708L, datos, 2, 6))
    }

    @Test
    fun conservaTodosLosBitsDelRelojU64() {
        val esperado = byteArrayOf(0, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0)
        assertArrayEquals(esperado, paquete(0, -1L, byteArrayOf(), 0, 0))
    }

    @Test
    fun escribePaqueteDeCuadroVacio() {
        val esperado = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertArrayEquals(esperado, paquete(1, 0L, byteArrayOf(5), 1, 0))
    }

    @Test
    fun rechazaSubrangoFueraDeDatos() {
        val datos = byteArrayOf(1, 2)
        assertThrows(IllegalArgumentException::class.java) { paquete(1, 0, datos, -1, 1) }
        assertThrows(IllegalArgumentException::class.java) { paquete(1, 0, datos, 1, 2) }
        assertThrows(IllegalArgumentException::class.java) { paquete(1, 0, datos, 0, -1) }
    }

    @Test
    fun leeBajarConFlotantesBigEndian() {
        val datos = byteArrayOf(0, 0x3f, 0, 0, 0, 0x3e, -128, 0, 0)
        assertEquals(EventoToque(0, 0.5f, 0.25f), leerToque(datos))
    }

    @Test
    fun leeMoverEnExtremosDelVideo() {
        val datos = byteArrayOf(1, 0, 0, 0, 0, 0x3f, -128, 0, 0)
        assertEquals(EventoToque(1, 0f, 1f), leerToque(datos))
    }

    @Test
    fun leeSubirEnOrigen() {
        assertEquals(EventoToque(2, 0f, 0f), leerToque(byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun rechazaLongitudesDistintasDeNueve() {
        assertNull(leerToque(ByteArray(0)))
        assertNull(leerToque(ByteArray(8)))
        assertNull(leerToque(ByteArray(10)))
    }

    @Test
    fun rechazaAccionesFueraDelProtocolo() {
        assertNull(leerToque(byteArrayOf(3, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertNull(leerToque(byteArrayOf(-1, 0, 0, 0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun rechazaCoordenadasFueraDeRango() {
        assertNull(leerToque(byteArrayOf(0, -66, -128, 0, 0, 0, 0, 0, 0)))
        assertNull(leerToque(byteArrayOf(0, 0, 0, 0, 0, 0x3f, -64, 0, 0)))
    }

    @Test
    fun rechazaNanEInfinitosEnAmbosEjes() {
        val invalidos = listOf(
            byteArrayOf(0x7f, -64, 0, 0),
            byteArrayOf(0x7f, -128, 0, 0),
            byteArrayOf(-1, -128, 0, 0),
        )
        for (coordenada in invalidos) {
            assertNull(leerToque(byteArrayOf(0) + coordenada + byteArrayOf(0, 0, 0, 0)))
            assertNull(leerToque(byteArrayOf(0, 0, 0, 0, 0) + coordenada))
        }
    }

    @Test
    fun convierteCoordenadasNormalizadasAPixeles() {
        assertEquals(540f to 600f, aPixeles(EventoToque(1, 0.5f, 0.25f), 1080, 2400))
    }

    @Test
    fun convierteExtremosDelVideoALosExtremosDeLaPantalla() {
        assertEquals(0f to 2400f, aPixeles(EventoToque(2, 0f, 1f), 1080, 2400))
    }
}

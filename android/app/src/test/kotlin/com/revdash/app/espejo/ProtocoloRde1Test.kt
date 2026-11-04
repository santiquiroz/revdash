package com.revdash.app.espejo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProtocoloRde1Test {
    @Test
    fun cabeceraLeeResolucionBigEndian() {
        assertEquals(CabeceraRde1(590, 1280), leerCabecera(bytes("52 44 45 31 02 4e 05 00")))
    }

    @Test
    fun cabeceraLeeDimensionesSinSigno() {
        assertEquals(CabeceraRde1(65535, 32768), leerCabecera(bytes("52 44 45 31 ff ff 80 00")))
    }

    @Test
    fun cabeceraRechazaFirmaIncorrecta() {
        assertNull(leerCabecera(bytes("52 44 45 32 02 4e 05 00")))
    }

    @Test
    fun cabeceraRequiereOchoBytesExactos() {
        val cabecera = bytes("52 44 45 31 02 4e 05 00")
        assertNull(leerCabecera(cabecera.copyOf(7)))
        assertNull(leerCabecera(cabecera.copyOf(9)))
        assertNull(leerCabecera(byteArrayOf()))
    }

    @Test
    fun encabezadoLeeRelojYLargoBigEndian() {
        val datos = bytes("02 01 02 03 04 05 06 07 08 00 01 02 03")
        assertEquals(EncabezadoPaquete(2, 0x0102030405060708L, 0x010203), leerEncabezado(datos))
    }

    @Test
    fun encabezadoAceptaLosTresTipos() {
        for (tipo in 0..2) {
            val datos = ByteArray(13).also { it[0] = tipo.toByte() }
            assertEquals(EncabezadoPaquete(tipo, 0L, 0), leerEncabezado(datos))
        }
    }

    @Test
    fun encabezadoRechazaTiposDesconocidos() {
        for (tipo in listOf(3, 128, 255)) {
            assertNull(leerEncabezado(ByteArray(13).also { it[0] = tipo.toByte() }))
        }
    }

    @Test
    fun encabezadoRequiereTreceBytesExactos() {
        assertNull(leerEncabezado(ByteArray(12)))
        assertNull(leerEncabezado(ByteArray(14)))
        assertNull(leerEncabezado(byteArrayOf()))
    }

    @Test
    fun encabezadoAceptaHastaCuatroMiB() {
        assertEquals(4 * 1024 * 1024, leerEncabezado(bytes("01 00 00 00 00 00 00 00 00 00 40 00 00"))?.largo)
        assertNull(leerEncabezado(bytes("01 00 00 00 00 00 00 00 00 00 40 00 01")))
    }

    @Test
    fun encabezadoRechazaLongitudesQueDesbordanInt() {
        assertNull(leerEncabezado(bytes("01 00 00 00 00 00 00 00 00 ff ff ff ff")))
        assertNull(leerEncabezado(bytes("01 00 00 00 00 00 00 00 00 80 00 00 00")))
    }

    @Test
    fun toqueCodificaFlotantesBigEndian() {
        assertArrayEquals(bytes("01 3e 80 00 00 3f 40 00 00"), toque(1, 0.25f, 0.75f))
    }

    @Test
    fun toqueRecortaCoordenadas() {
        assertArrayEquals(bytes("00 00 00 00 00 3f 80 00 00"), toque(0, -3f, 2f))
        assertArrayEquals(bytes("02 3f 80 00 00 00 00 00 00"), toque(2, 8f, -1f))
    }

    @Test
    fun videoVerticalQuedaCentradoConBandasLaterales() {
        assertEquals(AreaVideo(361.71875f, 0f, 276.5625f, 600f), areaVideo(1000, 600, 590, 1280))
        verificarPunto(0.5f, 0.5f, aNormalizado(500f, 300f, 1000, 600, 590, 1280))
        verificarPunto(0.25f, 0.75f, aNormalizado(430.859375f, 450f, 1000, 600, 590, 1280))
    }

    @Test
    fun videoHorizontalQuedaCentradoConBandasArribaYAbajo() {
        assertEquals(AreaVideo(0f, 69.53125f, 1000f, 460.9375f), areaVideo(1000, 600, 1280, 590))
        verificarPunto(0.5f, 0.5f, aNormalizado(500f, 300f, 1000, 600, 1280, 590))
        verificarPunto(0.25f, 0.75f, aNormalizado(250f, 415.234375f, 1000, 600, 1280, 590))
    }

    @Test
    fun bandasNoProducenToques() {
        assertNull(aNormalizado(100f, 300f, 1000, 600, 590, 1280))
        assertNull(aNormalizado(900f, 300f, 1000, 600, 590, 1280))
        assertNull(aNormalizado(500f, 30f, 1000, 600, 1280, 590))
        assertNull(aNormalizado(500f, 580f, 1000, 600, 1280, 590))
    }

    @Test
    fun bordesDelVideoSonCoordenadasCeroYUno() {
        verificarPunto(0f, 0f, aNormalizado(361.71875f, 0f, 1000, 600, 590, 1280))
        verificarPunto(1f, 1f, aNormalizado(638.28125f, 600f, 1000, 600, 590, 1280))
        verificarPunto(0f, 0f, aNormalizado(0f, 69.53125f, 1000, 600, 1280, 590))
        verificarPunto(1f, 1f, aNormalizado(1000f, 530.46875f, 1000, 600, 1280, 590))
    }

    @Test
    fun dimensionesInvalidasNoProducenAreaNiToque() {
        assertNull(areaVideo(0, 600, 590, 1280))
        assertNull(areaVideo(1000, -1, 590, 1280))
        assertNull(areaVideo(1000, 600, 0, 1280))
        assertNull(aNormalizado(500f, 300f, 1000, 600, 590, 0))
    }

    @Test
    fun coordenadasNoFinitasNoProducenToques() {
        assertNull(aNormalizado(Float.NaN, 300f, 1000, 600, 590, 1280))
        assertNull(aNormalizado(500f, Float.POSITIVE_INFINITY, 1000, 600, 590, 1280))
    }

    @Test
    fun subirFueraDelVideoTerminaEnElUltimoPuntoAceptado() {
        val eventos = mutableListOf<Triple<Int, Float, Float>>()
        val gesto = GestoEspejo { accion, x, y -> eventos.add(Triple(accion, x, y)) }

        gesto.recibir(0, 0.25f to 0.5f)
        gesto.recibir(1, 0.75f to 0.5f)
        gesto.recibir(1, null)
        gesto.recibir(2, null)
        gesto.recibir(2, 0.1f to 0.1f)

        assertEquals(
            listOf(Triple(0, 0.25f, 0.5f), Triple(1, 0.75f, 0.5f), Triple(2, 0.75f, 0.5f)),
            eventos,
        )
    }

    @Test
    fun cancelarFueraDelVideoTerminaInclusoSinMovimiento() {
        val eventos = mutableListOf<Triple<Int, Float, Float>>()
        val gesto = GestoEspejo { accion, x, y -> eventos.add(Triple(accion, x, y)) }

        gesto.recibir(0, 0.25f to 0.75f)
        gesto.recibir(2, null)
        gesto.recibir(1, 0.5f to 0.5f)

        assertEquals(listOf(Triple(0, 0.25f, 0.75f), Triple(2, 0.25f, 0.75f)), eventos)
    }

    @Test
    fun movimientoYSubidaSinBajadaSeIgnoran() {
        val eventos = mutableListOf<Triple<Int, Float, Float>>()
        val gesto = GestoEspejo { accion, x, y -> eventos.add(Triple(accion, x, y)) }

        gesto.recibir(1, 0.5f to 0.5f)
        gesto.recibir(2, 0.5f to 0.5f)
        gesto.recibir(2, null)

        assertEquals(emptyList<Triple<Int, Float, Float>>(), eventos)
    }

    @Test
    fun bajadaEnUnaBandaNoIniciaGestoAlEntrarAlVideo() {
        val eventos = mutableListOf<Triple<Int, Float, Float>>()
        val gesto = GestoEspejo { accion, x, y -> eventos.add(Triple(accion, x, y)) }

        gesto.recibir(0, aNormalizado(100f, 300f, 1000, 600, 590, 1280))
        gesto.recibir(1, aNormalizado(500f, 300f, 1000, 600, 590, 1280))
        gesto.recibir(2, 0.5f to 0.5f)

        assertEquals(emptyList<Triple<Int, Float, Float>>(), eventos)
    }

    @Test
    fun limpiarDescartaElGestoAntesDeCerrarLaSesion() {
        val eventos = mutableListOf<Triple<Int, Float, Float>>()
        val gesto = GestoEspejo { accion, x, y -> eventos.add(Triple(accion, x, y)) }

        gesto.recibir(0, 0.25f to 0.75f)
        gesto.limpiar()
        gesto.recibir(1, 0.5f to 0.5f)
        gesto.recibir(2, null)
        gesto.recibir(0, 0.5f to 0.5f)
        gesto.recibir(2, 0.75f to 0.25f)

        assertEquals(
            listOf(Triple(0, 0.25f, 0.75f), Triple(0, 0.5f, 0.5f), Triple(2, 0.75f, 0.25f)),
            eventos,
        )
    }

    private fun verificarPunto(x: Float, y: Float, punto: Pair<Float, Float>?) {
        assertNotNull(punto)
        assertEquals(x, punto!!.first, 0.0001f)
        assertEquals(y, punto.second, 0.0001f)
    }

    private fun bytes(hex: String): ByteArray = hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()
}

package com.revdash.nucleo.estado

import com.revdash.nucleo.Lectura
import com.revdash.nucleo.TramaCan
import com.revdash.nucleo.perfil.ClavesSenal
import com.revdash.nucleo.perfil.EstadoSenal
import com.revdash.nucleo.perfil.OrdenBytes
import com.revdash.nucleo.perfil.PerfilCan
import com.revdash.nucleo.perfil.SenalCan
import com.revdash.nucleo.perfil.TipoSenal
import com.revdash.nucleo.perfil.VehiculoPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProyectorEstadoTest {

    private val proyector = ProyectorEstado(perfilPrueba())

    @Test
    fun `acelerador 0x50 en trama 300 da 40 por ciento`() {
        val ultimos = proyector.aplicar(UltimosValores(), trama300())
        assertEquals(Lectura.Valor(40.0, T), proyector.estado(ultimos, T).aceleradorPct)
    }

    @Test
    fun `rpm big-endian y marcha 0 da N`() {
        val ultimos = proyector.aplicar(UltimosValores(), trama300())
        val estado = proyector.estado(ultimos, T)
        assertEquals(Lectura.Valor(1024.0, T), estado.rpm)
        assertEquals(Lectura.Valor("N", T), estado.marcha)
    }

    @Test
    fun `valor invalido en GEAR no pisa el anterior`() {
        val conMarcha = proyector.aplicar(UltimosValores(), trama300())
        val trasInvalido = proyector.aplicar(conMarcha, trama300(gear = 0x07))
        assertEquals(Lectura.Valor("N", T), proyector.estado(trasInvalido, T).marcha)
    }

    @Test
    fun `velocidad a 500 ms es valor y a 501 ms es sin dato`() {
        val ultimos = proyector.aplicar(UltimosValores(), TramaCan(T, 0x301, byteArrayOf(100)))
        assertEquals(Lectura.Valor(100.0, T), proyector.estado(ultimos, T + 500).velocidadKmh)
        assertEquals(Lectura.SinDato, proyector.estado(ultimos, T + 501).velocidadKmh)
    }

    @Test
    fun `temperatura con periodo 100 a 300 ms es valor y a 301 ms es vieja`() {
        val ultimos = proyector.aplicar(UltimosValores(), TramaCan(T, 0x302, byteArrayOf(90)))
        assertEquals(Lectura.Valor(90.0, T), proyector.estado(ultimos, T + 300).tempMotorC)
        assertEquals(Lectura.Viejo(90.0, T), proyector.estado(ultimos, T + 301).tempMotorC)
    }

    @Test
    fun `clave ausente queda sin dato`() {
        val ultimos = proyector.aplicar(UltimosValores(), trama300())
        val estado = proyector.estado(ultimos, T)
        assertEquals(Lectura.SinDato, estado.tempAireC)
        assertEquals(Lectura.SinDato, estado.pataAbajo)
    }

    @Test
    fun `aplicar no muta la entrada`() {
        val antes = UltimosValores()
        proyector.aplicar(antes, trama300())
        assertTrue(antes.porClave.isEmpty())
    }

    private companion object {
        const val T = 1_000L

        fun trama300(gear: Byte = 0x00) =
            TramaCan(T, 0x300, byteArrayOf(0x50, 0x10, 0x00, gear))

        fun senal(
            clave: String,
            id: String,
            bitInicio: Int,
            largo: Int,
            factor: Double = 1.0,
            tipo: TipoSenal = TipoSenal.NUMERO,
            valores: Map<Long, String> = emptyMap(),
            invalidos: Set<Long> = emptySet(),
            periodoMs: Long = 100,
        ) = SenalCan(
            clave, clave, id, bitInicio, largo, OrdenBytes.BIG, false,
            factor, 0.0, "", tipo, valores, invalidos, periodoMs,
            EstadoSenal.SIN_VERIFICAR, "prueba",
        )

        fun perfilPrueba() = PerfilCan(
            version = 1,
            id = "prueba",
            vehiculo = VehiculoPerfil("T", "M", "2024", "MOTORCYCLE"),
            calibracionesEcu = emptyList(),
            protocolo = "6",
            licencia = "",
            autor = "",
            atribucion = "",
            senales = listOf(
                senal(ClavesSenal.ACELERADOR, "300", bitInicio = 0, largo = 8, factor = 0.5),
                senal(ClavesSenal.RPM, "300", bitInicio = 8, largo = 16, factor = 0.25),
                senal(
                    ClavesSenal.GEAR, "300", bitInicio = 24, largo = 8,
                    tipo = TipoSenal.ENUM, valores = mapOf(0L to "N"), invalidos = setOf(7L),
                ),
                senal(ClavesSenal.VELOCIDAD, "301", bitInicio = 0, largo = 8),
                senal(ClavesSenal.TEMP_MOTOR, "302", bitInicio = 0, largo = 8),
            ),
        )
    }
}

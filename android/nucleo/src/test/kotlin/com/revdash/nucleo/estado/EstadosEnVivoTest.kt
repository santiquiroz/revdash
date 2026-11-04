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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EstadosEnVivoTest {

    @Test
    fun `emite cada 50 ms con la primera en t 0`() = runTest {
        val tiempos = mutableListOf<Long>()
        estadosEnVivo(emptyFlow(), proyector(), periodoMs = 50) { testScheduler.currentTime }
            .onEach { tiempos += testScheduler.currentTime }
            .take(3).toList()
        assertEquals(listOf(0L, 50L, 100L), tiempos)
    }

    @Test
    fun `refleja la trama nueva y sigue emitiendo sin tramas`() = runTest {
        val trama = TramaCan(0, 0x300, byteArrayOf(0x50))
        val estados = estadosEnVivo(flowOf(trama), proyector(), periodoMs = 50) { testScheduler.currentTime }
            .take(3).toList()
        assertEquals(3, estados.size)
        assertEquals(Lectura.Valor(40.0, 0L), estados.last().aceleradorPct)
    }

    private companion object {
        fun proyector(): ProyectorEstado {
            val acel = SenalCan(
                ClavesSenal.ACELERADOR, "Acelerador", "300", 0, 8, OrdenBytes.BIG, false,
                0.5, 0.0, "%", TipoSenal.NUMERO, emptyMap(), emptySet(), 100,
                EstadoSenal.SIN_VERIFICAR, "prueba",
            )
            val perfil = PerfilCan(
                version = 1,
                id = "prueba",
                vehiculo = VehiculoPerfil("T", "M", "2024", "MOTORCYCLE"),
                calibracionesEcu = emptyList(),
                protocolo = "6",
                licencia = "",
                autor = "",
                atribucion = "",
                senales = listOf(acel),
            )
            return ProyectorEstado(perfil)
        }
    }
}

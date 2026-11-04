package com.revdash.nucleo.rdp

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.estado.ProyectorEstado
import com.revdash.nucleo.perfil.ClavesSenal
import com.revdash.nucleo.perfil.EstadoSenal
import com.revdash.nucleo.perfil.OrdenBytes
import com.revdash.nucleo.perfil.PerfilCan
import com.revdash.nucleo.perfil.SenalCan
import com.revdash.nucleo.perfil.TipoSenal
import com.revdash.nucleo.perfil.VehiculoPerfil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EstadosDesdeRdpTest {
    @Test
    fun `emite desde t cero y sigue tras terminar las lineas`() = runTest {
        val tiempos = mutableListOf<Long>()
        val estados = estadosDesdeRdp(emptyFlow(), proyector(), CanalesCable(), { testScheduler.currentTime })
            .onEach { tiempos += testScheduler.currentTime }
            .take(3).toList()

        assertEquals(listOf(0L, 50L, 100L), tiempos)
        assertEquals(List(3) { EstadoMoto() }, estados)
    }

    @Test
    fun `CAN usa el reloj local y proyecta rpm en la emision siguiente`() = runTest {
        val lineas = flow {
            delay(1)
            emit("C,99999,300,s,0010A00000000000")
        }
        val estados = estadosDesdeRdp(lineas, proyector(), CanalesCable(), { testScheduler.currentTime })
            .take(2).toList()

        assertEquals(Lectura.SinDato, estados.first().rpm)
        assertEquals(Lectura.Valor(4256.0, 1L), estados.last().rpm)
    }

    @Test
    fun `flancos prenden y apagan el canal configurado`() = runTest {
        val lineas = flow {
            delay(1)
            emit("E,100,2,1")
            delay(50)
            emit("E,200,2,0")
        }
        val estados = estadosDesdeRdp(
            lineas, proyector(), CanalesCable(direccionalIzq = 2), { testScheduler.currentTime },
        ).take(3).toList()

        assertEquals(Lectura.Valor(true, 1L), estados[1].direccionalIzq)
        assertEquals(Lectura.Valor(false, 51L), estados[2].direccionalIzq)
        assertEquals(Lectura.SinDato, estados.last().direccionalDer)
    }

    @Test
    fun `los tres campos caducan solo despues de 1000 ms de silencio`() = runTest {
        val lineas = flow {
            delay(1)
            emit("E,0,2,1")
            emit("E,0,3,0")
            emit("E,0,4,1")
        }
        val estados = estadosDesdeRdp(
            lineas, proyector(), CanalesCable(2, 3, 4), { testScheduler.currentTime }, periodoMs = 1,
        ).take(1003).toList()

        assertEquals(Lectura.Valor(true, 1L), estados[1001].direccionalIzq)
        assertEquals(Lectura.Valor(false, 1L), estados[1001].direccionalDer)
        assertEquals(Lectura.Valor(true, 1L), estados[1001].luzAlta)
        assertEquals(EstadoMoto(), estados[1002])
    }

    @Test
    fun `la muestra real no inventa CAN ni niveles de cable`() = runTest {
        val lineas = flow {
            for (linea in muestraEsp32()) {
                delay(100)
                emit(linea)
            }
        }
        val estados = estadosDesdeRdp(
            lineas, proyector(), CanalesCable(2, 3, 4), { testScheduler.currentTime },
        ).take(muestraEsp32().size * 2 + 2).toList()

        assertTrue(estados.all { it == EstadoMoto() })
    }

    @Test
    fun `la muestra real mantiene vivo un nivel previo sin nuevos flancos`() = runTest {
        val muestra = muestraEsp32()
        val lineas = flow {
            emit("E,0,2,1")
            for (linea in muestra) {
                delay(100)
                emit(linea)
            }
        }
        val estados = estadosDesdeRdp(
            lineas, proyector(), CanalesCable(direccionalIzq = 2), { testScheduler.currentTime },
        ).take(muestra.size * 2 + 2).toList()

        assertTrue(testScheduler.currentTime > 1000)
        assertEquals(Lectura.Valor(true, 0L), estados.last().direccionalIzq)
        assertEquals(Lectura.SinDato, estados.last().rpm)
    }

    @Test
    fun `lineas invalidas no renuevan la senal de vida`() = runTest {
        val lineas = flow {
            emit("E,0,2,1")
            delay(900)
            emit("E,900,2,invalid o")
        }
        val estados = estadosDesdeRdp(
            lineas, proyector(), CanalesCable(direccionalIzq = 2), { testScheduler.currentTime },
            periodoMs = 1,
        ).take(1002).toList()

        assertEquals(Lectura.Valor(true, 0L), estados[1000].direccionalIzq)
        assertEquals(Lectura.SinDato, estados[1001].direccionalIzq)
    }

    @Test
    fun `rpm envejece aunque el puente siga vivo`() = runTest {
        val lineas = flow {
            emit("C,90000,300,s,0010A00000000000")
            delay(250)
            emit("S,1,0")
        }
        val estados = estadosDesdeRdp(lineas, proyector(), CanalesCable(), { testScheduler.currentTime })
            .take(8).toList()

        assertEquals(Lectura.Valor(4256.0, 0L), estados[6].rpm)
        assertEquals(Lectura.Viejo(4256.0, 0L), estados[7].rpm)
    }

    private companion object {
        fun muestraEsp32(): List<String> =
            checkNotNull(EstadosDesdeRdpTest::class.java.getResourceAsStream("/rdp/esp32_sin_transceptor.txt"))
                .bufferedReader().use { it.readLines() }

        fun proyector(): ProyectorEstado {
            val rpm = SenalCan(
                ClavesSenal.RPM, "RPM", "300", 8, 16, OrdenBytes.BIG, false,
                1.0, 0.0, "rpm", TipoSenal.NUMERO, emptyMap(), emptySet(), 100,
                EstadoSenal.SIN_VERIFICAR, "prueba",
            )
            return ProyectorEstado(
                PerfilCan(
                    1, "prueba", VehiculoPerfil("T", "M", "2024", "MOTORCYCLE"),
                    emptyList(), "6", "", "", "", listOf(rpm),
                ),
            )
        }
    }
}

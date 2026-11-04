package com.revdash.nucleo.rdp

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.TramaCan
import com.revdash.nucleo.estado.ProyectorEstado
import com.revdash.nucleo.estado.UltimosValores
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun estadosDesdeRdp(
    lineas: Flow<String>,
    proyector: ProyectorEstado,
    canales: CanalesCable,
    reloj: () -> Long,
    periodoMs: Long = 50,
    silencioMaxMs: Long = 1000,
): Flow<EstadoMoto> = channelFlow {
    // Una sola referencia mantiene coherentes los datos y su última señal de vida entre hilos.
    val puente = AtomicReference(EstadoPuente())
    launch {
        lineas.collect { linea ->
            val evento = ParserRdp1.linea(linea) ?: return@collect
            val ahora = reloj()
            puente.updateAndGet { it.conEvento(evento, proyector, canales, ahora) }
        }
    }
    while (true) {
        send(puente.get().proyectar(proyector, reloj(), silencioMaxMs))
        delay(periodoMs)
    }
}

private data class EstadoPuente(
    val ultimos: UltimosValores = UltimosValores(),
    val cable: EstadoMoto = EstadoMoto(),
    val ultimaLineaMs: Long? = null,
) {
    fun conEvento(
        evento: EventoRdp,
        proyector: ProyectorEstado,
        canales: CanalesCable,
        ahora: Long,
    ): EstadoPuente = when (evento) {
        is EventoRdp.Can -> copy(
            ultimos = proyector.aplicar(ultimos, TramaCan(ahora, evento.id, evento.datos)),
            ultimaLineaMs = ahora,
        )
        is EventoRdp.Flanco -> copy(cable = cable.conFlanco(evento, canales, ahora), ultimaLineaMs = ahora)
        else -> copy(ultimaLineaMs = ahora)
    }

    fun proyectar(proyector: ProyectorEstado, ahora: Long, silencioMaxMs: Long): EstadoMoto {
        val estado = proyector.estado(ultimos, ahora)
        val niveles = cableVigente(ahora, silencioMaxMs)
        return estado.copy(
            direccionalIzq = niveles.direccionalIzq,
            direccionalDer = niveles.direccionalDer,
            luzAlta = niveles.luzAlta,
        )
    }

    private fun cableVigente(ahora: Long, silencioMaxMs: Long): EstadoMoto {
        val ultima = ultimaLineaMs ?: return EstadoMoto()
        if (ahora - ultima > silencioMaxMs) return EstadoMoto()
        return cable
    }
}

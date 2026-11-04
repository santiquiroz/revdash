package com.revdash.nucleo.estado

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.TramaCan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun estadosEnVivo(
    tramas: Flow<TramaCan>,
    proyector: ProyectorEstado,
    periodoMs: Long = 50,
    reloj: () -> Long,
): Flow<EstadoMoto> = channelFlow {
    // Lector y escritor pueden quedar en hilos distintos con un despachador multihilo.
    val ultimos = AtomicReference(UltimosValores())
    launch { tramas.collect { trama -> ultimos.updateAndGet { proyector.aplicar(it, trama) } } }
    send(proyector.estado(ultimos.get(), reloj()))
    while (true) {
        delay(periodoMs)
        send(proyector.estado(ultimos.get(), reloj()))
    }
}

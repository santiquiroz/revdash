package com.revdash.nucleo.tramas

import com.revdash.nucleo.FuenteTramas
import com.revdash.nucleo.TramaCan
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReproductorCandump(
    lineas: List<String>,
    private val bucle: Boolean = true,
    private val reloj: () -> Long = System::currentTimeMillis,
) : FuenteTramas {

    private val tramas = lineas.mapNotNull(ParserCandump::linea)

    override fun tramas(): Flow<TramaCan> = flow {
        if (tramas.isEmpty()) return@flow
        do {
            reproducir(tramas)
            if (bucle) delay(PAUSA_BUCLE_MS)
        } while (bucle)
    }

    private suspend fun FlowCollector<TramaCan>.reproducir(tramas: List<TramaCan>) {
        var anterior = tramas.first().tMs
        for ((indice, original) in tramas.withIndex()) {
            if (indice > 0) {
                val espera = original.tMs - anterior
                if (espera > 0) delay(espera)
            }
            emit(TramaCan(reloj(), original.id, original.datos))
            anterior = original.tMs
        }
    }

    companion object {
        private const val PAUSA_BUCLE_MS = 10L
    }
}

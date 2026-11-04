package com.revdash.nucleo.rdp

import com.revdash.nucleo.TramaCan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

fun tramasDeRdp(lineas: Flow<String>, reloj: () -> Long): Flow<TramaCan> =
    lineas.mapNotNull { texto -> ParserRdp1.linea(texto) as? EventoRdp.Can }
        .map { evento -> TramaCan(reloj(), evento.id, evento.datos) }

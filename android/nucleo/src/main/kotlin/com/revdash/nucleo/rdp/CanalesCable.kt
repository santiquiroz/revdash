package com.revdash.nucleo.rdp

import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Lectura

data class CanalesCable(
    val direccionalIzq: Int? = null,
    val direccionalDer: Int? = null,
    val luzAlta: Int? = null,
    val activoEnAlto: Boolean = true,
)

fun EstadoMoto.conFlanco(flanco: EventoRdp.Flanco, canales: CanalesCable, tMs: Long): EstadoMoto {
    val encendido = Lectura.Valor(flanco.nivel == canales.activoEnAlto, tMs)
    if (flanco.canal == canales.direccionalIzq) return copy(direccionalIzq = encendido)
    if (flanco.canal == canales.direccionalDer) return copy(direccionalDer = encendido)
    if (flanco.canal == canales.luzAlta) return copy(luzAlta = encendido)
    return this
}

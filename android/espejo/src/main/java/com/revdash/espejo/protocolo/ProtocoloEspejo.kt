package com.revdash.espejo.protocolo

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun tamanoVideo(
    anchoPx: Int,
    altoPx: Int,
    maxLargo: Int = 1280,
    maxCorto: Int = 720,
): Pair<Int, Int> {
    require(anchoPx >= 2 && altoPx >= 2)
    require(maxLargo >= 2 && maxCorto >= 2)
    require(maxLargo >= maxCorto)
    val escalaLarga = maxLargo.toDouble() / max(anchoPx, altoPx)
    val escalaCorta = maxCorto.toDouble() / min(anchoPx, altoPx)
    val escala = min(1.0, min(escalaLarga, escalaCorta))
    val ancho = parInferior(anchoPx * escala)
    val alto = parInferior(altoPx * escala)
    require(ancho >= 2 && alto >= 2)
    return ancho to alto
}

private fun parInferior(valor: Double): Int = floor(valor / 2.0).toInt() * 2

fun cabecera(ancho: Int, alto: Int): ByteArray {
    require(ancho in 1..65535 && alto in 1..65535)
    return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        .put(byteArrayOf(0x52, 0x44, 0x45, 0x31))
        .putShort(ancho.toShort())
        .putShort(alto.toShort())
        .array()
}

fun paquete(tipo: Int, relojMs: Long, datos: ByteArray, desde: Int, largo: Int): ByteArray {
    require(tipo in 0..2)
    require(desde in 0..datos.size)
    require(largo in 0..(datos.size - desde))
    require(largo <= Int.MAX_VALUE - 13)
    return ByteBuffer.allocate(13 + largo).order(ByteOrder.BIG_ENDIAN)
        .put(tipo.toByte())
        .putLong(relojMs)
        .putInt(largo)
        .put(datos, desde, largo)
        .array()
}

data class EventoToque(val accion: Int, val x: Float, val y: Float)

fun leerToque(b: ByteArray): EventoToque? {
    if (b.size != 9) return null
    val entrada = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
    val evento = EventoToque(entrada.get().toInt() and 0xff, entrada.float, entrada.float)
    if (evento.accion !in 0..2) return null
    if (!coordenadaValida(evento.x)) return null
    if (!coordenadaValida(evento.y)) return null
    return evento
}

private fun coordenadaValida(valor: Float): Boolean = valor.isFinite() && valor in 0f..1f

fun aPixeles(e: EventoToque, anchoPx: Int, altoPx: Int): Pair<Float, Float> {
    require(anchoPx > 0 && altoPx > 0)
    return e.x * anchoPx to e.y * altoPx
}

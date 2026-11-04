package com.revdash.app.espejo

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val FIRMA_RDE1 = 0x52444531
private const val MAX_PAQUETE = 4 * 1024 * 1024

data class CabeceraRde1(val ancho: Int, val alto: Int)

data class EncabezadoPaquete(val tipo: Int, val relojMs: Long, val largo: Int)

data class AreaVideo(val izquierda: Float, val arriba: Float, val ancho: Float, val alto: Float)

fun leerCabecera(b: ByteArray): CabeceraRde1? {
    if (b.size != 8) return null
    val datos = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
    if (datos.int != FIRMA_RDE1) return null
    return CabeceraRde1(datos.short.toInt() and 0xffff, datos.short.toInt() and 0xffff)
}

fun leerEncabezado(b: ByteArray): EncabezadoPaquete? {
    if (b.size != 13) return null
    val datos = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
    val tipo = datos.get().toInt() and 0xff
    if (tipo !in 0..2) return null
    val relojMs = datos.long
    val largo = datos.int
    if (largo !in 0..MAX_PAQUETE) return null
    return EncabezadoPaquete(tipo, relojMs, largo)
}

fun toque(accion: Int, x: Float, y: Float): ByteArray =
    ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
        .put(accion.toByte())
        .putFloat(x.coerceIn(0f, 1f))
        .putFloat(y.coerceIn(0f, 1f))
        .array()

fun areaVideo(anchoVista: Int, altoVista: Int, anchoVideo: Int, altoVideo: Int): AreaVideo? {
    if (minOf(anchoVista, altoVista, anchoVideo, altoVideo) <= 0) return null
    val escala = minOf(anchoVista.toFloat() / anchoVideo, altoVista.toFloat() / altoVideo)
    val ancho = anchoVideo * escala
    val alto = altoVideo * escala
    return AreaVideo((anchoVista - ancho) / 2f, (altoVista - alto) / 2f, ancho, alto)
}

fun aNormalizado(
    xPx: Float,
    yPx: Float,
    anchoVista: Int,
    altoVista: Int,
    anchoVideo: Int,
    altoVideo: Int,
): Pair<Float, Float>? {
    val area = areaVideo(anchoVista, altoVista, anchoVideo, altoVideo) ?: return null
    val x = (xPx - area.izquierda) / area.ancho
    val y = (yPx - area.arriba) / area.alto
    if (x !in 0f..1f) return null
    if (y !in 0f..1f) return null
    return x to y
}

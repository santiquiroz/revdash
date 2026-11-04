package com.revdash.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revdash.app.tema.Ambar
import com.revdash.app.tema.Azul
import com.revdash.app.tema.Barlow
import com.revdash.app.tema.Barra
import com.revdash.app.tema.BaseTabular
import com.revdash.app.tema.Condensada
import com.revdash.app.tema.Fondo
import com.revdash.app.tema.FondoAuto
import com.revdash.app.tema.Linea
import com.revdash.app.tema.LineaAuto
import com.revdash.app.tema.Mudo
import com.revdash.app.tema.MudoAuto
import com.revdash.app.tema.Panel
import com.revdash.app.tema.Rojo
import com.revdash.app.tema.Texto
import com.revdash.app.tema.TextoAuto
import com.revdash.app.tema.Verde
import com.revdash.app.ui.componentes.BarraRpm
import com.revdash.app.ui.componentes.BordeAlerta
import com.revdash.app.ui.componentes.ChipDashcam
import com.revdash.app.ui.componentes.Direccionales
import com.revdash.app.ui.componentes.FlechaDireccional
import com.revdash.app.ui.componentes.Tarjeta
import com.revdash.app.ui.componentes.Testigo
import com.revdash.app.ui.componentes.VelocidadGrande
import com.revdash.nucleo.EstadoDashcam
import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.Lectura
import java.util.Locale

@Composable
fun PantallaTablero(ui: UiTablero, alCambiarModo: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Fondo)) {
        when (ui.modo) {
            ModoPantalla.COMPLETO -> ContenidoCompleto(ui, alCambiarModo)
            ModoPantalla.COLUMNA -> ContenidoColumna(ui, alCambiarModo)
            ModoPantalla.FRANJA -> ContenidoFranja(ui, alCambiarModo)
        }
        BordeAlerta(ui.alertas.peligro, Modifier.fillMaxSize(), ui.modo)
        ChipDashcam(ui.dashcam, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 16.dp))
    }
}

@Composable
private fun ContenidoCompleto(ui: UiTablero, alCambiarModo: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1.35f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            FilaTestigos(ui.estado)
            VelocidadGrande(ui.estado.velocidadKmh, ui.limiteKmh, numeroSp = 230.sp)
            BarraRpm(ui.estado.rpm)
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Tarjeta("MARCHA", Modifier.weight(1f).fillMaxWidth()) {
                    MarchaGrande(ui.estado.marcha)
                }
                Tarjeta("MOTOR · AIRE", Modifier.weight(1f).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextoDato(ui.estado.tempMotorC, 0, " °C", 30.sp, color = colorMotor(ui.estado.tempMotorC))
                        Text("·", color = Mudo, fontSize = 30.sp, fontFamily = Barlow)
                        TextoDato(ui.estado.tempAireC, 0, " °C", 30.sp)
                    }
                }
                Tarjeta("VIAJE · HORA", Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        "${"%.1f".format(Locale.US, ui.odometro.viajeKm)} km · ${ui.hora}",
                        color = Texto,
                        fontFamily = Barlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        style = BaseTabular.copy(fontFamily = Barlow),
                    )
                }
            }
            BotonModo(ui.modo, alCambiarModo, Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun ContenidoColumna(ui: UiTablero, alCambiarModo: () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(0.28f).fillMaxHeight().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Direccionales(
                ui.estado.direccionalIzq,
                ui.estado.direccionalDer,
                Modifier.fillMaxWidth(),
                36.sp,
                Arrangement.SpaceBetween,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VelocidadGrande(ui.estado.velocidadKmh, ui.limiteKmh, 120.sp, unidadSp = 18.sp, insigniaDp = 48.dp)
            }
            MarchaGrande(ui.estado.marcha, tamano = 28.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Testigo("ALTA", encendida(ui.estado.luzAlta), Azul, tamano = 52.dp, textoTamano = 12.sp)
                Testigo("CHECK", encendida(ui.estado.checkEngine), Ambar, tamano = 52.dp, textoTamano = 12.sp)
                Testigo("ABS", encendida(ui.estado.abs), Ambar, tamano = 52.dp, textoTamano = 12.sp)
                Testigo("PATA", pataAbajo(ui.estado), Ambar, tamano = 52.dp, textoTamano = 12.sp)
            }
            BarraRpm(ui.estado.rpm)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilaHoraRec(ui.hora, ui.dashcam)
                BotonModo(ui.modo, alCambiarModo)
            }
        }
        VerticalDivider(Modifier.fillMaxHeight(), 3.dp, Linea)
        Row(Modifier.weight(0.72f).fillMaxHeight().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CajaAndroidAuto("Android Auto · Mapa", "navegación del celular", Modifier.weight(1.5f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CajaAndroidAuto("Música", "", Modifier.weight(1f).fillMaxWidth())
                CajaAndroidAuto("Llamada / aviso", "", Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ContenidoFranja(ui: UiTablero, alCambiarModo: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).background(Barra).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlechaDireccional(izquierda = true, lectura = ui.estado.direccionalIzq)
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Text(
                    ui.estado.velocidadKmh.comoTexto(),
                    style = BaseTabular.copy(
                        fontFamily = Condensada,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        lineHeight = 56.sp,
                        color = if (ui.estado.velocidadKmh.esViejo()) Mudo else Texto,
                    ),
                )
                Text("KM/H", color = Mudo, fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text("·", color = Mudo, fontFamily = Barlow, fontSize = 24.sp)
                MarchaGrande(ui.estado.marcha, tamano = 28.sp)
                Text("·", color = Mudo, fontFamily = Barlow, fontSize = 24.sp)
                Text(ui.hora, color = Texto, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                TextoDato(ui.estado.tempMotorC, 0, " °C", 24.sp, color = colorMotor(ui.estado.tempMotorC))
            }
            BotonModo(ui.modo, alCambiarModo)
            FlechaDireccional(izquierda = false, lectura = ui.estado.direccionalDer)
        }
        HorizontalDivider(Modifier, 3.dp, Linea)
        Row(Modifier.weight(1f).fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CajaAndroidAuto("Android Auto · Mapa", "casi pantalla completa", Modifier.weight(1.5f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CajaAndroidAuto("Música", "", Modifier.weight(1f).fillMaxWidth())
                CajaAndroidAuto("Llamada", "", Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FilaTestigos(estado: EstadoMoto) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Direccionales(estado.direccionalIzq, estado.direccionalDer)
        Box(Modifier.size(12.dp))
        Testigo("N", esNeutro(estado), colorMarcha(estado.marcha), textoTamano = 20.sp)
        Testigo("ALTA", encendida(estado.luzAlta), Azul, textoTamano = 14.sp)
        Testigo("CHECK", encendida(estado.checkEngine), Ambar, textoTamano = 14.sp)
        Testigo("ABS", encendida(estado.abs), Ambar, textoTamano = 14.sp)
        Testigo("PATA", pataAbajo(estado), Ambar, textoTamano = 14.sp)
    }
}

@Composable
private fun MarchaGrande(marcha: Lectura<String>, tamano: TextUnit = 44.sp) {
    val estilo = BaseTabular.copy(fontFamily = Condensada, fontWeight = FontWeight.Bold, fontSize = tamano)
    when (marcha) {
        Lectura.SinDato -> Text("—", style = estilo.copy(color = Mudo))
        is Lectura.Valor -> Text(marcha.valor, style = estilo.copy(color = colorMarcha(marcha)))
        is Lectura.Viejo -> Text(marcha.valor, style = estilo.copy(color = Mudo))
    }
}

@Composable
private fun TextoDato(
    lectura: Lectura<Double>,
    decimales: Int,
    sufijo: String,
    tamano: TextUnit,
    color: Color = Texto,
) {
    when (lectura) {
        Lectura.SinDato -> {
            Text("—", color = Mudo, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = tamano)
        }
        is Lectura.Valor -> {
            Text(
                lectura.comoTexto(decimales) + sufijo,
                color = color,
                fontFamily = Barlow,
                fontWeight = FontWeight.Bold,
                fontSize = tamano,
            )
        }
        is Lectura.Viejo -> {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    lectura.comoTexto(decimales) + sufijo,
                    color = Mudo,
                    fontFamily = Barlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = tamano,
                )
                Text(
                    "sin dato",
                    color = Ambar,
                    fontFamily = Barlow,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun FilaHoraRec(hora: String, dashcam: EstadoDashcam) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(hora, color = Mudo, fontFamily = Barlow, fontSize = 18.sp)
        if (dashcam is EstadoDashcam.Grabando) {
            Box(Modifier.size(8.dp).background(Rojo, CircleShape))
            Text("REC", color = Mudo, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CajaAndroidAuto(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(FondoAuto)) {
        Canvas(Modifier.fillMaxSize()) {
            val paso = 28.dp.toPx()
            val grosor = 14.dp.toPx()
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(LineaAuto, Offset(x, 0f), Offset(x + size.height, size.height), grosor)
                x += paso
            }
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(titulo, color = TextoAuto, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            if (subtitulo.isNotEmpty()) {
                Text(subtitulo, color = MudoAuto, fontFamily = Barlow, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun BotonModo(modo: ModoPantalla, alCambiarModo: () -> Unit, modifier: Modifier = Modifier) {
    val siguiente = when (modo) {
        ModoPantalla.COMPLETO -> "2"
        ModoPantalla.COLUMNA -> "3"
        ModoPantalla.FRANJA -> "1"
    }
    Button(
        onClick = alCambiarModo,
        modifier = modifier.sizeIn(minWidth = 72.dp, minHeight = 72.dp)
            .semantics { contentDescription = "Cambiar modo de pantalla" },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Texto),
        border = BorderStroke(1.dp, Linea),
    ) {
        Text("VISTA $siguiente", fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

private fun encendida(lectura: Lectura<Boolean>): Boolean =
    when (lectura) {
        is Lectura.Valor -> lectura.valor
        is Lectura.Viejo -> lectura.valor
        Lectura.SinDato -> false
    }

private fun pataAbajo(estado: EstadoMoto): Boolean = encendida(estado.pataAbajo)

private fun esNeutro(estado: EstadoMoto): Boolean =
    when (val marcha = estado.marcha) {
        is Lectura.Valor -> marcha.valor == "N"
        is Lectura.Viejo -> marcha.valor == "N"
        Lectura.SinDato -> false
    }

private fun colorMarcha(marcha: Lectura<String>): Color =
    when (marcha) {
        is Lectura.Viejo -> Mudo
        else -> Verde
    }

private fun colorMotor(temperatura: Lectura<Double>): Color {
    val valor = (temperatura as? Lectura.Valor)?.valor ?: return Texto
    return when {
        valor >= 115 -> Rojo
        valor >= 105 -> Ambar
        else -> Texto
    }
}

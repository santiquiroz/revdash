package com.revdash.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.revdash.app.ui.ModoPantalla
import com.revdash.app.ui.PantallaTablero
import com.revdash.app.ui.UiTablero
import com.revdash.nucleo.Alerta
import com.revdash.nucleo.EstadoDashcam
import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.LadoAlerta
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.NivelAlerta
import com.revdash.nucleo.Odometro
import com.revdash.nucleo.alertas.alertasVisibles
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h600dp-land-mdpi")
class PantallaTableroCapturaTest {
    @get:Rule
    val compose = createComposeRule()

    private fun motoBase() = EstadoMoto(
        velocidadKmh = Lectura.Valor(42.0, 0),
        rpm = Lectura.Valor(3_800.0, 0),
        marcha = Lectura.Valor("N", 0),
        aceleradorPct = Lectura.Valor(30.0, 0),
        tempMotorC = Lectura.Valor(92.0, 0),
        tempAireC = Lectura.Valor(31.0, 0),
        pataAbajo = Lectura.Valor(false, 0),
        embrague = Lectura.Valor(false, 0),
        killSwitchRun = Lectura.Valor(true, 0),
        motorEnMarcha = Lectura.Valor(true, 0),
        direccionalIzq = Lectura.Valor(true, 0),
        direccionalDer = Lectura.Valor(false, 0),
        luzAlta = Lectura.Valor(false, 0),
        checkEngine = Lectura.Valor(false, 0),
    )

    private fun uiBase() = UiTablero(
        estado = motoBase(),
        odometro = Odometro(totalKm = 10_780.0, viajeKm = 82.4),
        hora = "12:14",
    )

    @Test
    fun completo() {
        compose.setContent { PantallaTablero(uiBase(), {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/completo.png")
    }

    @Test
    fun columna() {
        compose.setContent { PantallaTablero(uiBase().copy(modo = ModoPantalla.COLUMNA), {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/columna.png")
    }

    @Test
    fun franja() {
        compose.setContent { PantallaTablero(uiBase().copy(modo = ModoPantalla.FRANJA), {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/franja.png")
    }

    @Test
    fun completo_alerta() {
        val peligro = Alerta("trasera-izq", NivelAlerta.P1, LadoAlerta.IZQUIERDA, "ATRÁS IZQ", 2.5)
        val ui = uiBase().copy(
            estado = motoBase().copy(
                velocidadKmh = Lectura.Valor(68.0, 0),
                marcha = Lectura.Valor("4", 0),
            ),
            alertas = alertasVisibles(listOf(peligro)),
            dashcam = EstadoDashcam.Grabando(2),
            limiteKmh = 60,
        )
        compose.setContent { PantallaTablero(ui, {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/completo_alerta.png")
    }

    @Test
    fun completo_sin_dato() {
        compose.setContent { PantallaTablero(UiTablero(hora = "12:14"), {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/completo_sin_dato.png")
    }
}

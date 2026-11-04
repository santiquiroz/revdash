package com.revdash.app.demo

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revdash.app.ui.ModoPantalla
import com.revdash.app.ui.UiTablero
import com.revdash.app.usb.hayEsp32
import com.revdash.app.usb.lineasUsbConReintento
import com.revdash.nucleo.Alerta
import com.revdash.nucleo.AlertasVisibles
import com.revdash.nucleo.EstadoDashcam
import com.revdash.nucleo.EstadoMoto
import com.revdash.nucleo.LadoAlerta
import com.revdash.nucleo.Lectura
import com.revdash.nucleo.MuestraVelocidad
import com.revdash.nucleo.NivelAlerta
import com.revdash.nucleo.Odometro
import com.revdash.nucleo.alertas.alertasVisibles
import com.revdash.nucleo.estado.ProyectorEstado
import com.revdash.nucleo.estado.estadosEnVivo
import com.revdash.nucleo.odometro.con
import com.revdash.nucleo.odometro.debePersistir
import com.revdash.nucleo.perfil.PerfilesIncluidos
import com.revdash.nucleo.rdp.CanalesCable
import com.revdash.nucleo.rdp.estadosDesdeRdp
import com.revdash.nucleo.tramas.CapturasIncluidas
import com.revdash.nucleo.tramas.ReproductorCandump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Context.prefsTablero by preferencesDataStore("tablero")
private val CLAVE_ODOMETRO = doublePreferencesKey("odometro_km")
private val CANALES_CABLE = CanalesCable()

private const val TOTAL_INICIAL_KM = 10_780.0
private const val LIMITE_DEMO_KMH = 30
private const val CICLO_DEMO_MS = 20_000L

class DemoTablero(app: Application) : AndroidViewModel(app) {
    private val prefs = app.applicationContext.prefsTablero
    private val fuenteUsb = hayEsp32(app)

    private val modo = MutableStateFlow(ModoPantalla.COMPLETO)
    private val odometro = MutableStateFlow(Odometro(totalKm = TOTAL_INICIAL_KM))
    private var guardadoKm = TOTAL_INICIAL_KM

    // Mismo reloj en la fuente y en el proyector: la frescura se calcula sobre una sola base.
    private val reproductor = ReproductorCandump(CapturasIncluidas.empujando(), reloj = SystemClock::elapsedRealtime)
    private val proyector = ProyectorEstado(PerfilesIncluidos.apache())
    private val estados = fuenteEstados(app)
        .flowOn(Dispatchers.Default)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val ui: StateFlow<UiTablero> = combine(estados, latidoDemo(), odometro, modo) { estado, ahora, odo, modoActual ->
        armarUi(estado, ahora, odo, modoActual)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiTablero())

    init {
        viewModelScope.launch {
            val previo = prefs.data.map { it[CLAVE_ODOMETRO] }.first()
            if (previo != null) {
                odometro.value = Odometro(totalKm = previo)
                guardadoKm = previo
            }
            // Solo después de leer lo guardado: si no, el valor inicial podría pisar el odómetro real.
            estados.collect { alimentarOdometro(it) }
        }
    }

    private var modoTablero = ModoPantalla.COMPLETO

    fun cambiarModo() {
        modo.value =
            when (modo.value) {
                ModoPantalla.COMPLETO -> ModoPantalla.COLUMNA
                ModoPantalla.COLUMNA -> ModoPantalla.FRANJA
                ModoPantalla.FRANJA -> ModoPantalla.COMPLETO
                ModoPantalla.ESPEJO -> modoTablero
            }
    }

    fun abrirEspejo() {
        if (modo.value == ModoPantalla.ESPEJO) return
        modoTablero = modo.value
        modo.value = ModoPantalla.ESPEJO
    }

    fun volverAlTablero() {
        modo.value = modoTablero
    }

    private fun fuenteEstados(app: Application): Flow<EstadoMoto> =
        if (fuenteUsb) {
            estadosDesdeRdp(lineasUsbConReintento(app), proyector, CANALES_CABLE, reloj = SystemClock::elapsedRealtime)
        } else {
            estadosEnVivo(reproductor.tramas(), proyector, reloj = SystemClock::elapsedRealtime)
        }

    private fun armarUi(estado: EstadoMoto, ahora: Long, odo: Odometro, modoActual: ModoPantalla): UiTablero {
        if (fuenteUsb) return armarUiUsb(estado, odo, modoActual)
        val ciclo = ahora % CICLO_DEMO_MS
        val simulado = estado.copy(
            // 1,5 Hz = periodo de 667 ms; encendido la mitad de cada periodo.
            direccionalIzq = Lectura.Valor(ciclo in 2_000L..7_000L && ciclo % 667L < 333L, ahora),
            direccionalDer = Lectura.Valor(false, ahora),
            luzAlta = Lectura.Valor(ciclo in 10_000L..14_000L, ahora),
        )
        val peligro =
            if (ciclo in 8_000L..11_000L) {
                Alerta("demo-trasera-izq", NivelAlerta.P1, LadoAlerta.IZQUIERDA, "ATRÁS IZQ", 2.5)
            } else {
                null
            }
        return UiTablero(
            estado = simulado,
            odometro = odo,
            alertas = if (peligro == null) AlertasVisibles() else alertasVisibles(listOf(peligro)),
            dashcam = EstadoDashcam.Grabando(2),
            limiteKmh = LIMITE_DEMO_KMH,
            modo = modoActual,
            hora = horaLocal(),
        )
    }

    private fun armarUiUsb(estado: EstadoMoto, odo: Odometro, modoActual: ModoPantalla): UiTablero =
        UiTablero(
            estado = estado,
            odometro = odo,
            // Datos reales: sin cámaras ni lector de señales todavía, no se inventan.
            dashcam = EstadoDashcam.Apagada,
            limiteKmh = null,
            modo = modoActual,
            hora = horaLocal() + " · USB",
        )

    private fun alimentarOdometro(estado: EstadoMoto) {
        val lectura = estado.velocidadKmh
        if (lectura !is Lectura.Valor) return
        val nuevo = odometro.value.con(MuestraVelocidad(lectura.valor, lectura.tMs))
        odometro.value = nuevo
        if (debePersistir(guardadoKm, nuevo)) {
            guardadoKm = nuevo.totalKm
            viewModelScope.launch { prefs.edit { it[CLAVE_ODOMETRO] = nuevo.totalKm } }
        }
    }

    private fun latidoDemo(): Flow<Long> =
        flow {
            while (true) {
                emit(SystemClock.elapsedRealtime())
                delay(100)
            }
        }

    private fun horaLocal(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
}

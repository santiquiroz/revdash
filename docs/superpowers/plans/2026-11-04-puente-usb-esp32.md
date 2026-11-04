# Plan: puente USB con el ESP32 (Fase 1)

Contexto: en la Fase 1 el ESP32 con el firmware de sondeo (`firmware/probe/`) va por USB (puente CP210x) a la
ROCK 4D o, para probar, a un celular con USB-C OTG. Emite líneas RDP1 a 921600 baudios (formatos en
`docs/superpowers/plans/2026-10-19-fase0-ingenieria-inversa.md`, Task 2). Ya existe en `nucleo`:
`rdp/ParserRdp1`, `rdp/EventoRdp`, `rdp/CanalesCable` + `EstadoMoto.conFlanco`, `rdp/tramasDeRdp`,
`estado/ProyectorEstado`, `estado/estadosEnVivo` (ver `docs/superpowers/plans/2026-11-04-app-fase1a.md`).
Muestra real del ESP32 sin transceptor (líneas `F`, `S`, `K`): `android/nucleo/src/test/resources/rdp/esp32_sin_transceptor.txt`.

Estilo: el de los planes anteriores (funciones cortas que hacen una sola cosa, sin KDoc, inmutable, nombres
en español, JUnit 4 + kotlinx-coroutines-test con tiempo virtual).

## Tarea E: estado de la moto desde RDP1 (`nucleo`, paquete `com.revdash.nucleo.rdp`)

Archivo `rdp/EstadosDesdeRdp.kt`:

```kotlin
fun estadosDesdeRdp(
    lineas: Flow<String>,
    proyector: ProyectorEstado,
    canales: CanalesCable,
    reloj: () -> Long,
    periodoMs: Long = 50,
    silencioMaxMs: Long = 1000,
): Flow<EstadoMoto>
```

- `channelFlow` con estado compartido en `AtomicReference` (como `estadosEnVivo`): una corrutina consume
  `lineas`; cada línea válida (`ParserRdp1.linea != null`) actualiza `ultimaLineaMs = reloj()`. Un evento
  `Can` hace `proyector.aplicar(ultimos, TramaCan(reloj(), id, datos))`. Un `Flanco` actualiza un
  `EstadoMoto` de cable con `conFlanco(flanco, canales, reloj())`. Los demás eventos solo cuentan como señal
  de vida.
- Otra corrutina emite cada `periodoMs`, empezando en t = 0: `proyector.estado(ultimos, ahora)` con los
  campos `direccionalIzq`, `direccionalDer` y `luzAlta` copiados del estado de cable. Si nunca llegó una
  línea válida, o pasaron más de `silencioMaxMs` desde la última, esos tres campos van en `SinDato` (ESP32
  desconectado o colgado).
- Si `lineas` termina, el flujo sigue emitiendo (los datos envejecen solos).

Pruebas `EstadosDesdeRdpTest` (tiempo virtual, `reloj = { testScheduler.currentTime }`, perfil mínimo armado
en la prueba con la señal RPM `0C` en el ID 300, bits 8-23 big-endian):
- una línea `C,…,300,s,0010A00000000000` deja `rpm = Valor(4256.0)` en la emisión siguiente;
- con `CanalesCable(direccionalIzq = 2)`, `E,…,2,1` deja `direccionalIzq = Valor(true)` y `E,…,2,0` la
  vuelve `false`;
- tras 1001 ms sin líneas, los tres campos de cable vuelven a `SinDato`;
- las líneas de `esp32_sin_transceptor.txt` (leídas del recurso) no producen CAN ni excepciones, y mantienen
  vivo el puente (campos de cable con canal configurado pero sin flanco = `SinDato`, porque aún no hubo nivel).

## Tarea F: USB serie en la app (`app`, paquete `com.revdash.app.usb`)

Dependencia ya agregada: `com.github.mik3y:usb-serial-for-android:3.11.0` (`libs.usb.serial`).

1. `res/xml/dispositivos_usb.xml`: filtro para CP210x (VID 0x10C4 / PID 0xEA60), CH340 (0x1A86 / 0x7523),
   CH9102 (0x1A86 / 0x55D4) y FTDI FT232R (0x0403 / 0x6001), en decimal como pide Android.
2. `AndroidManifest.xml`: en `MainActivity`, `intent-filter` con `android.hardware.usb.action.USB_DEVICE_ATTACHED`
   y `meta-data` `android.hardware.usb.action.USB_DEVICE_ATTACHED` → `@xml/dispositivos_usb`;
   `uses-feature android:name="android.hardware.usb.host" android:required="false"`.
3. `usb/PuertoEsp32.kt`:
   - `fun lineasUsb(contexto: Context, baudios: Int = 921600): Flow<String>`: `callbackFlow` que busca el
     primer puerto con `UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)`; si no hay, cierra
     el flujo sin error. Si no hay permiso, lo pide con un `PendingIntent` (`FLAG_MUTABLE` en API 31+) y
     espera la respuesta con un `BroadcastReceiver` registrado con `ContextCompat.RECEIVER_NOT_EXPORTED`
     (o el equivalente sin `ContextCompat` si no está disponible). Abre el puerto a 921600 8N1, `dtr = false`,
     `rts = false` (no reiniciar el ESP32), lee con `SerialInputOutputManager` y parte por `\n`, quitando `\r`;
     líneas de más de 256 caracteres sin salto se descartan. `awaitClose` cierra el puerto y quita el
     receptor.
   - `fun hayEsp32(contexto: Context): Boolean`.
4. `demo/DemoTablero.kt` (archivo existente): si `hayEsp32(app)` al crear el ViewModel, la fuente es
   `estadosDesdeRdp(lineasUsb(app), proyector, CANALES_CABLE, reloj = SystemClock::elapsedRealtime)` con
   `private val CANALES_CABLE = CanalesCable()` (sin canales hasta tener el pinout) y **sin** las señales
   simuladas ni la alerta de demo; si no, todo sigue igual que hoy. Exponer `val enVivo: Boolean` en
   `UiTablero` no hace falta: agregar al final de la hora el texto ` · USB` cuando la fuente es el ESP32.
   No cambiar nada más del archivo.
5. Sin pruebas instrumentadas; el orquestador compila y prueba en un celular con OTG.

## Tarea G: niveles de las entradas al arrancar y cada segundo (`firmware/probe`)

Problema: el firmware solo emite `E` en los flancos. Una luz alta encendida desde el arranque, o una app que
abre el puerto después del arranque, nunca ve el nivel.

1. `lib/signal/debouncer.h` y `lib/signal/debouncer.cpp`: agregar `bool level() const` (el nivel estable actual).
2. `src/config.h`: `constexpr uint32_t kLevelRefreshMs = 1000;`.
3. `src/inputs.cpp`: al final de `inputsBegin`, y luego cada `kLevelRefreshMs` dentro de `inputTask`, emitir
   `rdp::formatEdge(now, ch, deb[ch]->level())` para los 8 canales. Sin cambiar el formato RDP1.
4. Prueba nativa en `test/test_signal/test_main.cpp`: `level()` devuelve el inicial y cambia solo cuando
   `update` confirma un flanco.
5. No compilar ni flashear (el orquestador corre `python -m platformio test -e native` y
   `python -m platformio run -e esp32dev -t upload`).

# Plan: prueba de concepto del duplicado de pantalla

Spec: `docs/superpowers/specs/2026-11-04-duplicado-pantalla-design.md` (protocolo RDE1 ahí; respetarlo
byte por byte). Estilo: el de los planes anteriores (funciones cortas que hacen una sola cosa, sin KDoc ni
docstrings, nombres en español, inmutable donde se pueda). No hacer `git add/commit`; dejar los archivos en el
árbol de trabajo.

## Tarea H: app compañera (módulo Gradle `:espejo` en `android/`)

- `android/settings.gradle.kts`: `include(":espejo")`. `android/espejo/build.gradle.kts` como `app` pero sin
  Compose ni Roborazzi: `namespace`/`applicationId` `com.revdash.espejo`, compileSdk 36, minSdk 29, targetSdk 35,
  Java/JVM 17, dependencias solo `libs.coroutines.android` (si hace falta) y `testImplementation(libs.junit)`.
  Interfaz con vistas clásicas (un `Activity` con un botón y un texto de estado), nada de AppCompat si no hace falta.
- `AndroidManifest.xml`: permisos `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`,
  `POST_NOTIFICATIONS`; `ServicioEspejo` con `android:foregroundServiceType="mediaProjection"`;
  `ServicioToques` (accesibilidad) con `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`,
  intent-filter `android.accessibilityservice.AccessibilityService` y `res/xml/servicio_toques.xml` con
  `android:canPerformGestures="true"` y una descripción en `strings.xml`.
- `MainActivity`: botón "Duplicar" → `createScreenCaptureIntent()`; con el resultado OK arranca
  `ServicioEspejo` (`startForegroundService`) pasándole `resultCode` y el `Intent`. Botón "Detener". Texto de
  estado: IP local del Wi-Fi + puerto 27183, y si el servicio de accesibilidad está activo (si no, botón que abre
  `Settings.ACTION_ACCESSIBILITY_SETTINGS`).
- `ServicioEspejo`: `startForeground` con notificación y tipo `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` ANTES de
  `getMediaProjection` (obligatorio en Android 14). Registrar `MediaProjection.Callback` (obligatorio en 14) que
  detiene todo. Servidor TCP en 27183 (`ServerSocket`, un cliente a la vez; al desconectarse, acepta otro).
  Por cliente: `VirtualDisplay` del tamaño de `tamanoVideo(...)`, codificador `video/avc` con superficie de
  entrada, 4 Mbps, 30 fps, `KEY_I_FRAME_INTERVAL` 1, `KEY_REPEAT_PREVIOUS_FRAME_AFTER` 100 000 µs; escribe la
  cabecera y un paquete por cada `dequeueOutputBuffer` (tipo 0 si `BUFFER_FLAG_CODEC_CONFIG`, 2 si
  `BUFFER_FLAG_KEY_FRAME`, si no 1). Un hilo lee eventos de 9 bytes del mismo socket y se los pasa a
  `ServicioToques`. Todo en hilos/corrutinas de IO, nunca en el principal.
- `ServicioToques`: instancia estática accesible (`@Volatile var activo: ServicioToques?`, asignada en
  `onServiceConnected`, null en `onUnbind`/`onDestroy`). Recibe eventos normalizados, los convierte a píxeles con
  el tamaño real de la pantalla (`WindowManager.currentWindowMetrics.bounds`) y acumula el trazo de bajar a
  subir; al subir despacha un solo `GestureDescription` con duración = tiempo real entre bajar y subir, acotada a
  1..10 000 ms. Comentario `ponytail:` con el techo (sin arrastre en vivo; mejora con `continueStroke`).
- Código puro (sin Android) en `com.revdash.espejo.protocolo`, con pruebas JUnit 4 en `src/test`:
  - `fun tamanoVideo(anchoPx: Int, altoPx: Int, maxLargo: Int = 1280, maxCorto: Int = 720): Pair<Int, Int>`:
    conserva la proporción, cabe en maxLargo×maxCorto según la orientación, dimensiones pares. Pruebas:
    1440×3120 → 590×1280; 3120×1440 → 1280×590 (redondeo hacia abajo a par); 1280×720 → 1280×720; 100×100 → 100×100.
  - `fun cabecera(ancho: Int, alto: Int): ByteArray` (8 bytes) y
    `fun paquete(tipo: Int, relojMs: Long, datos: ByteArray, desde: Int, largo: Int): ByteArray`. Pruebas con
    bytes exactos.
  - `data class EventoToque(val accion: Int, val x: Float, val y: Float)` y
    `fun leerToque(b: ByteArray): EventoToque?` (null si no son 9 bytes, acción fuera de 0..2 o x/y fuera de 0..1).
    `fun aPixeles(e: EventoToque, anchoPx: Int, altoPx: Int): Pair<Float, Float>`. Pruebas incluidas.
- No tocar los módulos `nucleo` ni `app`.

## Tarea I: receptor en el PC (`tools/espejo_receptor.py`)

- `python tools/espejo_receptor.py --host 192.168.x.x [--puerto 27183] [--adb]`. Dependencias en
  `tools/espejo-requirements.txt`: `av`, `opencv-python`, `numpy`.
- Conecta, lee la cabecera, decodifica con PyAV (`av.CodecContext.create("h264", "r")`, `parse` + `decode`) y
  muestra con `cv2.imshow` en una ventana redimensionable. Ratón: bajar/mover (con botón apretado)/subir →
  eventos de 9 bytes normalizados al tamaño del video mostrado.
- Latencia: con `--adb`, al arrancar calcula el desfase de reloj PC−celular con `adb shell date +%s%3N` (punto
  medio de la llamada). Cada 2 s imprime p50/p90 de `ahoraMs - relojMs - desfase` y los cuadros por segundo.
- Funciones puras con pruebas en `tools/tests/test_espejo_receptor.py` (pytest, sin red): `leer_cabecera(bytes)`,
  `leer_encabezado_paquete(bytes)` → `(tipo, reloj_ms, largo)`, `empaquetar_toque(accion, x, y)` (recorta x/y a
  0..1), `percentiles(lista)`.
- Lectura de socket con un helper `leer_exacto(sock, n)` que lanza `ConnectionError` si el socket se cierra.

## Verificación (el orquestador)

- `cd android && ./gradlew :espejo:testDebugUnitTest :espejo:assembleDebug`.
- `python -m pytest tools/tests/test_espejo_receptor.py`.
- Prueba real: instalar en el S25 por ADB, activar la accesibilidad, conectar el receptor.

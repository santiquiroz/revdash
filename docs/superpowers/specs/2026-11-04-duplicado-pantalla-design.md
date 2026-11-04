# Duplicado de la pantalla del celular en el tablero

Estado: diseño + prueba de concepto (2026-11-04). Complementa Android Auto (spec principal): Android Auto solo
muestra apps adaptadas; el duplicado muestra cualquier app del celular, con táctil.

## Decisiones

- **Táctil completo siempre**, también en marcha. Decisión del dueño (2026-11-04); sin bloqueo por velocidad por
  ahora. Si se agrega después, la velocidad ya llega del CAN.
- **App compañera en el celular**, sin root ni opciones de desarrollador:
  - video: `MediaProjection` → `VirtualDisplay` → `MediaCodec` H.264;
  - toques: `AccessibilityService.dispatchGesture`.
- **Descartado como modo principal:** scrcpy/ADB (exige opciones de desarrollador, el puerto cambia, apps con
  antifraude lo detectan) y Miracast (sin toques).
- **Transporte con interfaz única** (flujo de bytes bidireccional), dos implementaciones:
  - **TCP** sobre Wi-Fi Direct o el hotspot del tablero. Wi-Fi Direct no toca los datos móviles del celular. Riesgo:
    no hay reportes de Wi-Fi Direct ni SoftAP 5 GHz funcionando en la ROCK 4D con Android
    (`docs/investigacion/placa-sbc.md`); se prueba el primer día con la placa.
  - **USB por AOA** (Android Open Accessory, lo que usa Android Auto por cable): el tablero es host, el celular
    accesorio. Requiere un puerto USB expuesto en la carcasa.

## Restricciones conocidas

- Android 14+ pide confirmar la captura de pantalla en cada sesión (un toque al empezar el turno). En Android 14
  el servicio en primer plano de tipo `mediaProjection` debe arrancar antes de `getMediaProjection`.
- Las ventanas con `FLAG_SECURE` (bancos, pagos) salen en negro. Verificar app por app.
- Google Play restringe el uso de accesibilidad que no es para accesibilidad: la app compañera se distribuye
  por fuera de Play (APK firmado / F-Droid).
- Latencia esperada 50-150 ms por Wi-Fi; menos por USB.
- **Seguridad (bloqueante antes de cualquier uso real):** el servidor de la prueba acepta a cualquier cliente de
  la red sin autenticación, y quien se conecte ve la pantalla e inyecta toques. El producto necesita emparejamiento
  (código mostrado en el tablero, clave por sesión) y cifrado, además del aislamiento que da Wi-Fi Direct o USB.

## Protocolo RDE1 (versión de la prueba)

Celular = servidor TCP en el puerto 27183; el receptor se conecta. Enteros big-endian.

Celular → receptor:
- Cabecera, una vez: `"RDE1"` (4 bytes ASCII), `u16 ancho`, `u16 alto` (resolución del video).
- Paquete por cada salida del codificador: `u8 tipo` (0 = configuración SPS/PPS, 1 = cuadro, 2 = cuadro clave),
  `u64 relojMs` (`System.currentTimeMillis()` al salir del codificador, para medir latencia), `u32 largo`, `largo`
  bytes de H.264 Annex B.

Receptor → celular, 9 bytes por evento: `u8 accion` (0 = bajar, 1 = mover, 2 = subir), `f32 x`, `f32 y`
normalizados a 0..1 sobre el video.

## Prueba de concepto (sin la ROCK 4D)

S25 como emisor, PC como tablero (`tools/espejo_receptor.py`), por la Wi-Fi de la casa o `adb forward`.
Mide: latencia celular → PC, fluidez, consumo, y qué apps salen en negro. Contrato en
`docs/superpowers/plans/2026-11-04-espejo-poc.md`.

# CLAUDE.md — RevDash

Tablero abierto con Android Auto para la TVS Apache RTR 160 4V del dueño. Repo hermano de RevScope (`github.com/santiquiroz/revscope`, app Android OBD2). Este archivo lleva el contexto que no está en la spec, para seguir el trabajo desde cualquier PC (el portátil del garaje incluido).

- Spec: `docs/superpowers/specs/2026-10-19-revdash-design.md`
- Plan Fase 0: `docs/superpowers/plans/2026-10-19-fase0-ingenieria-inversa.md`
- Guía de campo: `docs/fase0/` (sondas, procedimiento, plantilla de pinout)

## Estado (2026-11-04)

- App RevDash Fase 1a (sin hardware) en `android/`: módulo `nucleo` (Kotlin puro: perfil CAN, candump,
  estado de la moto, odómetro, alertas, RDP1) y `app` (Compose, los tres estados, modo demo que reproduce
  una captura real en bucle). Spec `docs/superpowers/specs/2026-11-04-app-revdash-design.md`, plan
  `docs/superpowers/plans/2026-11-04-app-fase1a.md`, reglas visuales `docs/diseno/sistema-visual.md`.
  Comandos: `cd android && ./gradlew :nucleo:test :app:testDebugUnitTest :app:assembleDebug`; capturas de
  pantalla con `:app:recordRoborazziDebug` (en `app/src/test/screenshots/`).
- Duplicado de pantalla (prueba de concepto): app compañera `android/espejo` (servidor RDE1, sin autenticación) y
  receptor dentro de la app (botón ESPEJO, conecta a `127.0.0.1:27183` por ahora) o en el PC
  (`tools/espejo_receptor.py`). Spec `docs/superpowers/specs/2026-11-04-duplicado-pantalla-design.md`.
- Releases: APK de release firmadas con la clave de depuración (como RevScope) + firmware de la sonda unido con
  `esptool merge_bin` (se flashea en 0x0). CI en `.github/workflows/ci.yml` (sin verifyRoborazzi: Linux cambia los píxeles).
- Cámaras y detección = Fase 3 (`docs/investigacion/camaras-ia.md`). Grabar rutas con el celular sirve desde ya
  para probar detectores y empezar un dataset de señales colombianas.
- Pendiente para la app: confirmar la escala de velocidad con la prueba de ruta (301 ×0,01 frente a 500 b1
  ×0,125; en `empujando.log` dan 6,08 y 5,5 km/h de máximo), puente USB serie con el ESP32 cuando lleguen
  los SN65HVD230, open-headunit.

- Fase 0 (ingeniería inversa del conector del tablero) en curso. Las 6 tareas del plan tienen código (2026-11-01/02):
  - Guía de campo y sondas: `docs/fase0/`.
  - Firmware de sondeo: `firmware/probe/` (`python -m platformio test -e native` con gcc de WinLibs en el PATH; `python -m platformio run -e esp32dev` y `-e esp32dev_selftest` compilan). Falta la prueba de lazo cerrado en banco: puente GPIO 23 → GPIO 34, flashear `esp32dev_selftest`, esperar `F,…,0,≈100000` cada 250 ms.
  - Herramientas del PC: `tools/` (`python -m pytest tools/tests`): `capture.py` (graba el ESP32), `calibrate_speed.py` (factor de velocidad contra RevScope), `diff_can.py` (diferencia base contra acción; lee RDP1 y candump).
- RevScope (app Android del dueño, `github.com/santiquiroz/revscope`) gana el modo escucha CAN con el Vgate y 4 herramientas MCP (`escuchar_can`, `listar_capturas_can`, `leer_captura_can`, `diferencia_capturas_can`): Claude en el portátil puede capturar el bus por el OBD desde el celular y comparar capturas. Ver `docs/superpowers/specs/2026-11-01-modo-escucha-can-design.md` en ese repo.
- Investigación de Android Auto, herramientas CAN y regulación: `docs/investigacion/` y spec §12 (riesgos 7 y 9).
- Analizador lógico recibido y funcionando en el PC de escritorio (2026-11-01): driver WinUSB instalado, `sigrok-cli --scan` lo ve como "fx2lafw - Saleae Logic with 8 channels" y captura a 1 MHz. Los canales sueltos leen 1. Siguiente paso: llevar portátil + analizador + sondas al garaje y seguir `docs/fase0/README.md`.
- El tablero original está muerto (humedad / falso contacto en el mazo bajo la chapa, zona de las exploradoras). Las señales siguen llegando al conector.
- Pendiente del dueño: kilometraje exacto (≈ 10 780 km) para arrancar el odómetro propio.

## Replicabilidad

La Apache es el primer modelo, no el único: todo lo de la Fase 0 debe poder repetirse en otra moto.

- El procedimiento de `docs/fase0/` se escribe genérico; lo propio de la Apache va en su archivo de pinout.
- Anotar en el pinout el tiempo que tomó cada paso y la referencia del conector (marca y número de parte): son el costo de ingeniería por modelo.
- Las señales CAN que se descubran se guardan también en un archivo DBC por modelo (`docs/fase0/<modelo>.dbc`), formato estándar que leen SavvyCAN y `cantools`.
- El código no lleva nada propio de un modelo: pinout, señales y calibraciones entran como datos (perfil por modelo).

## Hardware

| Pieza | Estado | Notas |
|---|---|---|
| ESP32 DevKit | en mano | ESP32-D0WD-V3 (rev v3.1), puente USB CP210x. Entorno PlatformIO `esp32dev`. Tiene PCNT y TWAI. |
| Analizador lógico HiLetgo 24 MHz 8 canales | en mano | Clon Saleae (USB `VID_0925&PID_3881`, chip CY7C68013A). En sigrok/PulseView: driver `fx2lafw`. Umbral de entrada ≈ 2.0 V; el Saleae original admite hasta 5.25 V, el clon no lo publica: límite práctico 3.6 V como el ESP32. |
| Multímetro | en mano | |
| Componentes de sondas | en el carrito de Amazon (2026-11-01, compra pendiente del dueño) | Lista y estado en `docs/fase0/sondas.md` §Materiales; diodos BAT85 de patas en vez de BAT54S. |
| Radxa ROCK 4D 6 GB (RK3576) | en el carrito de Amazon (B0HJ88SCF2, US$131.99), compra en diciembre de 2026 | Android 14 oficial; imagen propia después. Reemplazó a la Zero 3W (ver `docs/investigacion/placa-sbc.md`). |
| Riverdi RVT70HSHNWC00-B 7" HDMI | llega ≈ 2026-12-06 | Energía, enable y PWM por J5 (7-30 V); verificar con multímetro que sean entradas. |

## Reglas de seguridad (no negociables)

- Solo escuchar: nada transmite al bus de la moto (TWAI en `TWAI_MODE_LISTEN_ONLY`, K-line solo RX, ningún GPIO conectado a la moto como salida).
- Nunca 12 V directo a un pin del ESP32 ni del analizador: siempre por una sonda de `docs/fase0/sondas.md`.
- Divisor corregido el 2026-11-01: **15 kΩ / 4.7 kΩ** (12 V → 2.86 V). El 22 kΩ / 4.7 kΩ del plan original daba 2.11 V, por debajo del umbral del ESP32 (0.75·3.3 V = 2.475 V).
- Señales de lado bajo (llevadas a masa por la ECU o un interruptor) quedan flotando con el tablero muerto: usar la sonda de lado bajo (pull-up a 3V3 + Schottky), no el divisor.
- Nada se corta en el mazo original: medir por detrás del conector.
- Portátil a batería (sin cargador) mientras está conectado a la moto: evita lazos de tierra. GND se conecta primero y se quita último.

## Decisiones del dueño

- Radxa ROCK 4D 6 GB + Android 14 + Riverdi 7" HDMI (2026-11-02; antes Zero 3W + Android 11); montaje fijo; solo táctil.
- Unidad única: placa + fuente propia + Riverdi en una carcasa, un solo cable a la moto. Retención de apagado con batería (sin supercondensador, consumo cero apagada).
- Estanca, sin entradas de aire: disipación por placa de aluminio, membrana Gore, conector Deutsch DT o Superseal, conformal coating.
- Sin OBD en RevDash: el puerto queda libre para RevScope. El OBD (Vgate + RevScope) solo sirve de referencia durante la validación de la Fase 0.
- El ESP32 es solo para la ingeniería inversa; el diseño final lee los pines desde la ROCK 4D.
- RevDash ↔ RevScope por BLE (el dueño temía que el Wi-Fi le cortara los datos móviles).
- Audio de Android Auto al intercomunicador XGP X10.
- Distribución de pantalla B+C (+ franja). Testigo ABS imprescindible.
- Licencia AGPL-3.0 (lo compartido con RevScope, Apache-2.0).
- Ideas de fases futuras: dashcam por CSI (sin modo parqueo, microSD High Endurance, clip protegido por caída detectada en RevScope); NPU RK3566 (≈ 0.8 TOPS, RKNN) solo útil con cámara.

## Preparar el portátil (Windows)

1. `git clone https://github.com/santiquiroz/revdash.git` y abrir Claude Code en esa carpeta.
2. PulseView: `winget install --id Sigrok.PulseView -e`. Queda en `C:\Program Files (x86)\sigrok\PulseView\`.
3. Driver del analizador (con el analizador conectado): en un PowerShell como administrador, `powershell -ExecutionPolicy Bypass -File tools\windows\install-winusb-fx2.ps1`. Instala WinUSB del propio Windows para `USB\VID_0925&PID_3881` con un catálogo firmado por un certificado local cuya clave privada se borra, igual que Zadig; el registro queda en `%TEMP%
evdash-winusb\install.log`. Probado el 2026-11-01 en Windows 11. Alternativa manual: `zadig.exe` de la carpeta de PulseView → Options → List All Devices → dispositivo `0925 3881` → WinUSB → Install Driver. Comprobar en PulseView: dispositivo "fx2lafw (generic driver for FX2 based LAs)" → Scan → "Saleae Logic".
   - Opcional, para capturar sin interfaz gráfica: `sigrok-cli` 0.7.2 (instalador en sigrok.org/download/binary/sigrok-cli/; se puede extraer con 7-Zip sin instalar). `sigrok-cli --scan` y `sigrok-cli -d fx2lafw --config samplerate=1m --samples 1000000 -o prueba.sr`.
4. Driver del ESP32: CP210x de Silicon Labs (Windows Update suele instalarlo solo; si no aparece un COM, bajarlo de silabs.com).
5. Python 3.11+, `pip install pyserial pytest platformio` (usar `python -m platformio` si `pio` no queda en el PATH).
6. Solo para correr las pruebas nativas del firmware (`pio test -e native`): gcc, `winget install BrechtSanders.WinLibs.POSIX.UCRT`.
7. Prueba de banco antes de salir: paso 0 de `docs/fase0/README.md`.

## Convenciones

- Documentación, textos visibles y mensajes de commit en español. Commits `tipo: descripción` (feat, fix, docs, test, chore). Nunca agregar `Co-Authored-By`.
- Código: funciones atómicas, baja complejidad ciclomática, sin comentarios de documentación (solo una línea cuando el porqué no es obvio).
- Capturas de PulseView (`.sr`) en `capturas/` (ignorada por git). Las que sirvan de evidencia se exportan como PNG a `docs/fase0/img/` y se citan en el pinout.
- Maquetas del README: `docs/img/mockups/*.html` → PNG con captura del selector `.scr`.

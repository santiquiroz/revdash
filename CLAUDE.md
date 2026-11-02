# CLAUDE.md — RevDash

Tablero abierto con Android Auto para la TVS Apache RTR 160 4V del dueño. Repo hermano de RevScope (`github.com/santiquiroz/revscope`, app Android OBD2). Este archivo lleva el contexto que no está en la spec, para seguir el trabajo desde cualquier PC (el portátil del garaje incluido).

- Spec: `docs/superpowers/specs/2026-10-19-revdash-design.md`
- Plan Fase 0: `docs/superpowers/plans/2026-10-19-fase0-ingenieria-inversa.md`
- Guía de campo: `docs/fase0/` (sondas, procedimiento, plantilla de pinout)

## Estado (2026-11-01)

- Fase 0 (ingeniería inversa del conector del tablero) en curso. Plan de 6 tareas; la Tarea 1 (guía de campo) es la única con contenido, el resto (firmware ESP32, `tools/*.py`) sin empezar.
- Analizador lógico recibido y funcionando en el PC de escritorio (2026-11-01): driver WinUSB instalado, `sigrok-cli --scan` lo ve como "fx2lafw - Saleae Logic with 8 channels" y captura a 1 MHz. Los canales sueltos leen 1. Siguiente paso: llevar portátil + analizador + sondas al garaje y seguir `docs/fase0/README.md`.
- El tablero original está muerto (humedad / falso contacto en el mazo bajo la chapa, zona de las exploradoras). Las señales siguen llegando al conector.
- Pendiente del dueño: kilometraje exacto (≈ 10 780 km) para arrancar el odómetro propio.

## Hardware

| Pieza | Estado | Notas |
|---|---|---|
| ESP32 DevKit | en mano | ESP32-D0WD-V3 (rev v3.1), puente USB CP210x. Entorno PlatformIO `esp32dev`. Tiene PCNT y TWAI. |
| Analizador lógico HiLetgo 24 MHz 8 canales | en mano | Clon Saleae (USB `VID_0925&PID_3881`, chip CY7C68013A). En sigrok/PulseView: driver `fx2lafw`. Umbral de entrada ≈ 2.0 V; el Saleae original admite hasta 5.25 V, el clon no lo publica: límite práctico 3.6 V como el ESP32. |
| Multímetro | en mano | |
| Componentes de sondas | verificar | Lista en `docs/fase0/sondas.md` (resistencias, BAT54S, SN65HVD230, agujas de back-probing). |
| Radxa Zero 3W 2 GB con GPIO | llega ≈ 2026-11-15 | Android 11 primero, imagen propia después. |
| Riverdi RVT70HSHNWC00-B 7" HDMI | llega ≈ 2026-12-06 | Energía, enable y PWM por J5 (7-30 V); verificar con multímetro que sean entradas. |

## Reglas de seguridad (no negociables)

- Solo escuchar: nada transmite al bus de la moto (TWAI en `TWAI_MODE_LISTEN_ONLY`, K-line solo RX, ningún GPIO conectado a la moto como salida).
- Nunca 12 V directo a un pin del ESP32 ni del analizador: siempre por una sonda de `docs/fase0/sondas.md`.
- Divisor corregido el 2026-11-01: **15 kΩ / 4.7 kΩ** (12 V → 2.86 V). El 22 kΩ / 4.7 kΩ del plan original daba 2.11 V, por debajo del umbral del ESP32 (0.75·3.3 V = 2.475 V).
- Señales de lado bajo (llevadas a masa por la ECU o un interruptor) quedan flotando con el tablero muerto: usar la sonda de lado bajo (pull-up a 3V3 + Schottky), no el divisor.
- Nada se corta en el mazo original: medir por detrás del conector.
- Portátil a batería (sin cargador) mientras está conectado a la moto: evita lazos de tierra. GND se conecta primero y se quita último.

## Decisiones del dueño

- Radxa Zero 3W (no la 3E: no tiene Wi-Fi) + Android 11 + Riverdi 7" HDMI; montaje fijo; solo táctil.
- Unidad única: Zero + fuente propia + Riverdi en una carcasa, un solo cable a la moto. Retención de apagado con batería (sin supercondensador, consumo cero apagada).
- Estanca, sin entradas de aire: disipación por placa de aluminio, membrana Gore, conector Deutsch DT o Superseal, conformal coating.
- Sin OBD en RevDash: el puerto queda libre para RevScope. El OBD (Vgate + RevScope) solo sirve de referencia durante la validación de la Fase 0.
- El ESP32 es solo para la ingeniería inversa; el diseño final lee los pines desde la Zero.
- RevDash ↔ RevScope por BLE (el dueño temía que el Wi-Fi le cortara los datos móviles).
- Audio de Android Auto al intercomunicador XGP X10.
- Distribución de pantalla B+C (+ franja). Testigo ABS imprescindible.
- Licencia AGPL-3.0 (lo compartido con RevScope, Apache-2.0).
- Ideas de fases futuras: dashcam por CSI (sin modo parqueo, microSD High Endurance, clip protegido por caída detectada en RevScope); NPU RK3566 (≈ 0.8 TOPS, RKNN) solo útil con cámara.

## Preparar el portátil (Windows)

1. `git clone https://github.com/santiquiroz/revdash.git` y abrir Claude Code en esa carpeta.
2. PulseView: `winget install --id Sigrok.PulseView -e`. Queda en `C:\Program Files (x86)\sigrok\PulseView\`.
3. Driver del analizador (con el analizador conectado): en un PowerShell como administrador, `powershell -ExecutionPolicy Bypass -File tools\windows\install-winusb-fx2.ps1`. Instala WinUSB del propio Windows para `USB\VID_0925&PID_3881` con un catálogo firmado por un certificado local cuya clave privada se borra, igual que Zadig; el registro queda en `%TEMP%evdash-winusb\install.log`. Probado el 2026-11-01 en Windows 11. Alternativa manual: `zadig.exe` de la carpeta de PulseView → Options → List All Devices → dispositivo `0925 3881` → WinUSB → Install Driver. Comprobar en PulseView: dispositivo "fx2lafw (generic driver for FX2 based LAs)" → Scan → "Saleae Logic".
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

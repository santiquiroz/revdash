# Fase 0 — guía de campo

Identifica el conector del tablero sin cortar el mazo ni transmitir a la moto.
Usa siempre las protecciones descritas en [sondas.md](sondas.md). Llena la
[plantilla de pinout](pinout-apache-rtr160-4v.md) a medida que encuentres
evidencia.

## Procedimiento

### Paso 0 — antes del garaje

- Revisa equipo y software en **“Preparar el portátil”** de
  [`CLAUDE.md`](../../CLAUDE.md); no salgas sin PulseView y driver del analizador
  verificados.
- Prueba de banco: conecta un canal del analizador a 3V3 de la ESP32 y otro a
  GND. Captura 1 s en PulseView. Debes ver un nivel 1 constante y un nivel 0
  constante.
- Deja el portátil a batería, sin cargador. Lleva multímetro, agujas de
  back-probing y las sondas montadas.

### Paso 1 — documentar el conector

Fotografía el conector y numera los pines desde la vista del lado de los cables.
Anota el color de cada cable. No cortes ni perfores el aislamiento.

### Paso 2 — identificar alimentación y buses con multímetro

Con llave OFF y ON, busca GND, alimentación permanente y alimentación con llave.
Pistas:

- **GND:** ≈ 0 Ω a la carcasa o al negativo de batería.
- **+12 V permanente:** presente con llave OFF.
- **+12 V con llave:** aparece al poner la llave en ON.
- **Posible par CAN:** dos pines a ≈ 2.3–2.7 V con llave ON.
- **Posible K-line:** ≈ 12 V en reposo, con caídas breves.
- **Posible lado bajo flotante:** la señal no se mueve y queda cerca de 0 V;
  probarla con la sonda de lado bajo.

Registra las medidas; no identifiques una señal solo por el color del cable.

### Paso 3 — accionar controles

Con llave ON, acciona por separado direccional izquierda/derecha, luz alta,
neutro, interruptor de apagado y botones del tablero. Anota qué pin cambia en
cada acción.

### Paso 4 — comprobar testigos

Al dar llave, observa el chequeo de bombillo de check engine y ABS. Registra
qué pin pulsa y si la señal parece de lado alto o lado bajo.

### Paso 5 — buscar pulsos

Con la rueda trasera girando en el soporte central o rodando despacio, busca
el pin con pulsos proporcionales a la velocidad. Con el motor encendido, busca
pulsos proporcionales a RPM. No hagas pruebas que comprometan el control de la
moto.

### Configuración de PulseView para los pasos 3–5

- Discretos y pulsos: **1 MHz**.
- CAN a 500 kbps: **4–8 MHz**.
- Si hace falta más resolución: **12–16 MHz**, dentro del límite del analizador.
- Duración: **10–30 s**, incluyendo la acción y el estado en reposo.
- Guarda cada captura como
  `capturas/<fecha>_<pin>_<accion>.sr` (fecha `AAAA-MM-DD`; acción sin espacios).
  Las capturas `.sr` no van a git. Exporta como PNG las evidencias útiles a
  `docs/fase0/img/` y cita sus nombres en la plantilla.

### Paso 6 — buscar y diferenciar tramas

Busca un par trenzado o pines con tramas. Prueba CAN a 500 y 250 kbps, o
UART/K-line a 10400 baudios.

Si hay CAN, graba con el ESP32 `base.rdp` durante 30 s con llave ON y sin tocar
nada. Graba capturas de la misma duración por cada acción del paso 3. Cuando
exista la herramienta de la Task 6, compara:

```powershell
python tools/diff_can.py --base base.rdp --action <accion>.rdp
```

Registra los IDs y bytes que cambian. La herramienta es una ayuda para acotar
tramas, no una identificación concluyente del significado de cada byte.

### Paso 7 — validar con el ESP32 y RevScope

Cuando exista el firmware (Task 3), ajusta `config.h` con el pinout encontrado. Graba un trayecto con RevScope
registrando al mismo tiempo con el OBD conectado al Vgate. El OBD sirve solo
como referencia de validación; RevDash no lo usará.

### Paso 8 — calibrar velocidad

Cuando exista la herramienta (Task 5), ejecuta `calibrate_speed.py` con las capturas del sensor y la referencia de
velocidad. Anota el factor de velocidad y sus datos de validación en la
plantilla.

## Criterio de salida

Imprescindibles: velocidad, direccionales, neutro, luz alta, check engine,
ABS y combustible. Deseables: RPM, modo de manejo y botones. Si una señal
deseable no aparece, la Fase 1 puede avanzar mostrándola como “no disponible”
hasta encontrar otra vía.

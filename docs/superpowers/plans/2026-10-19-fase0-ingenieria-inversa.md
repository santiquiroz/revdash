# Fase 0 — Ingeniería inversa del conector del tablero: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar listas las herramientas y la guía para descubrir qué lleva cada pin del conector del tablero de la Apache RTR 160 4V: un firmware de sondeo para ESP32 que transmite las señales por USB, herramientas en el PC para grabar y calibrar la velocidad contra RevScope, y la guía de campo con el pinout a llenar.

**Architecture:** El ESP32 lee hasta 8 canales digitales (cada uno tras un divisor con protección), cuenta pulsos por hardware en los canales de velocidad/RPM, escucha CAN en modo solo-escucha y K-line por UART RX, y emite líneas de texto del protocolo `RDP1` por USB serie. En el PC (o el celular con una app de terminal serie), `capture.py` graba esas líneas; `calibrate_speed.py` alinea la frecuencia de pulsos con la velocidad OBD exportada por RevScope y calcula el factor km/h por Hz. La lógica pura del firmware se prueba en el PC (`pio test -e native`); la parte de hardware, con un auto-test de lazo cerrado.

**Tech Stack:** PlatformIO + framework Arduino (ESP32, driver TWAI y PCNT de ESP-IDF), Unity para pruebas nativas; Python 3.11+ solo con stdlib + `pyserial`, pytest.

**Spec:** `docs/superpowers/specs/2026-10-19-revdash-design.md` (§9 Fase 0, §5 anti-objetivos, §13).

## Global Constraints

- Repo: `C:\personal\revdash` (git local, rama `main`). No hacer `git push` ni crear el repo en GitHub sin confirmación explícita del dueño.
- Licencia del repo: AGPL-3.0 (texto oficial de https://www.gnu.org/licenses/agpl-3.0.txt, sin modificar).
- Mensajes de commit: formato `tipo: descripción` en español (feat, fix, docs, test, chore). **Nunca** agregar la línea `Co-Authored-By`.
- Documentación y textos visibles en español.
- **Solo escuchar**: el firmware nunca transmite al bus de la moto (TWAI en `TWAI_MODE_LISTEN_ONLY`; K-line solo RX; ningún GPIO conectado a la moto se configura como salida).
- **Nunca 12 V directo a un pin**: toda entrada pasa por el divisor de `docs/fase0/sondas.md`.
- **Nada se corta en el mazo original**: medición por detrás del conector (back-probing).
- ESP32 objetivo: **ESP32 clásico (ESP32-WROOM-32 DevKit)**, entorno `esp32dev`. Si el ESP32 del dueño es un C3 (sin PCNT), el conteo usa interrupciones de GPIO; se confirma con el dueño antes de la Tarea 3.
- Código según la filosofía del dueño: funciones atómicas, baja complejidad ciclomática, sin comentarios de documentación (solo una línea cuando el porqué no es obvio).

## Review Focus

1. Canal desconectado o con ruido: no debe generar una avalancha de eventos `E` → prueba del rebote con pulsos más cortos que `stableMs` (Tarea 2).
2. Direccional a ~1.5 Hz: debe producir cada flanco, no ser filtrada por el antirrebote → prueba con pulsos de 333 ms (Tarea 2).
3. Moto detenida (0 pulsos): debe seguir emitiendo `F` con 0, no callarse → prueba `mhzFromCount(0, 250) == 0` y emisión por ventana (Tareas 2 y 3).
4. Línea basura o cortada al reconectar el USB, o con prefijo de la app de terminal: el parser la ignora sin romperse → pruebas en `test_rdp.py` (Tarea 4).
5. Calibración con datos sin variación o sin traslape: error claro, no división por cero → prueba en `test_calibrate.py` (Tarea 5).

---

### Task 1: Base del repo y guía de campo de la Fase 0

**Files:**
- Create: `README.md`, `LICENSE`, `docs/fase0/README.md`, `docs/fase0/sondas.md`, `docs/fase0/pinout-apache-rtr160-4v.md`

**Interfaces:**
- Produces: la tabla de canales `D0..D7` y los nombres de señal que usan `config.h` (Tarea 3) y las herramientas (Tareas 4-5): `ch` = índice 0-7.

- [ ] **Step 1: `README.md`** — qué es RevDash (resumen de la spec §1, §3), estado ("Fase 0: ingeniería inversa"), enlace a RevScope (`github.com/santiquiroz/revscope`), enlace a la spec y a `docs/fase0/`, licencia AGPL-3.0.
- [ ] **Step 2: `LICENSE`** — texto AGPL-3.0 oficial descargado de gnu.org, sin cambios.
- [ ] **Step 3: `docs/fase0/sondas.md`** — circuito por canal, con estos valores:
  - Digital 12 V → ESP32/analizador: R1 = 22 kΩ en serie desde la señal, R2 = 4.7 kΩ a GND (15 V → 2.64 V; 12 V → 2.11 V), diodo doble Schottky BAT54S a 3.3 V/GND en el nodo, resistencia de 1 kΩ entre el nodo y el pin.
  - CAN (si existe): CAN-H/CAN-L a un SN65HVD230 alimentado a 3.3 V; su RX a GPIO 22; su TX a GPIO 21 (el controlador en solo-escucha nunca lo pone en dominante). Sin terminación de 120 Ω adicional.
  - K-line (si existe): mismo divisor que digital, a GPIO 16 (UART2 RX).
  - Analizador lógico (HiLetgo 24 MHz 8 canales): los mismos divisores; GND común con la moto.
  - Lista de materiales con cantidades para 8 canales.
- [ ] **Step 4: `docs/fase0/README.md`** — procedimiento de campo en orden:
  1. Fotografiar el conector y numerar los pines (vista desde el lado de los cables).
  2. Con multímetro, llave OFF/ON: identificar GND, +12 V permanente, +12 V con llave.
  3. Con el analizador (PulseView) y llave ON: accionar una por una direccional izq/der, luz alta, neutro (palanca), interruptor de apagado, botones del tablero; anotar qué pin cambia.
  4. Prueba de testigos al dar llave (check engine y ABS hacen su chequeo de bombillo): registrar qué pin pulsa.
  5. Rueda trasera girando en el soporte central o rodando despacio: buscar el pin con pulsos proporcionales a la velocidad; motor encendido: buscar pulsos proporcionales a RPM.
  6. Buscar par de cables trenzados o pin con tramas: decodificar en PulseView como CAN (probar 500 y 250 kbps) o UART/K-line (10400 baudios).
  7. Pasar al ESP32 con `config.h` ajustado y grabar un viaje con RevScope grabando a la vez (OBD conectado al Vgate) — el OBD es solo referencia de validación.
  8. Correr `calibrate_speed.py` y anotar el factor.
  - Criterio de salida (spec §9): imprescindibles velocidad, direccionales, neutro, luz alta, check engine, ABS y combustible; RPM, modo y botones deseables.
- [ ] **Step 5: `docs/fase0/pinout-apache-rtr160-4v.md`** — plantilla: tabla `Pin | Color de cable | Señal | Tipo (12V discreto / pulsos / bus / alimentación) | Nivel activo | Evidencia (captura)` con una fila por pin y secciones "Bus de datos", "Factor de velocidad", "Pendientes".
- [ ] **Step 6: Commit**

```bash
git add README.md LICENSE docs/fase0
git commit -m "docs: guía de campo y sondas de la Fase 0"
```

---

### Task 2: Lógica pura del firmware (protocolo RDP1, antirrebote, frecuencia)

**Files:**
- Create: `firmware/probe/platformio.ini`, `firmware/probe/lib/rdp/rdp_frame.h`, `firmware/probe/lib/rdp/rdp_frame.cpp`, `firmware/probe/lib/signal/debouncer.h`, `firmware/probe/lib/signal/debouncer.cpp`, `firmware/probe/lib/signal/frequency.h`, `firmware/probe/lib/signal/frequency.cpp`
- Test: `firmware/probe/test/test_rdp/test_main.cpp`, `firmware/probe/test/test_signal/test_main.cpp`

**Interfaces:**
- Produces (todas en `namespace rdp`, sin dependencias de Arduino):
  - `std::string formatHello(uint32_t ms, const char* fwVersion)` → `"H,<ms>,RDP1,<fw>"`
  - `std::string formatStatus(uint32_t ms, uint32_t dropped)` → `"S,<ms>,<dropped>"`
  - `std::string formatEdge(uint32_t ms, uint8_t ch, bool level)` → `"E,<ms>,<ch>,<0|1>"`
  - `std::string formatFrequency(uint32_t ms, uint8_t ch, uint32_t milliHz)` → `"F,<ms>,<ch>,<mHz>"`
  - `std::string formatCan(uint32_t ms, uint32_t id, bool extended, const uint8_t* data, uint8_t len)` → `"C,<ms>,<id hex mayúsculas sin 0x>,<x|s>,<data hex mayúsculas, vacío si len=0>"`
  - `std::string formatKline(uint32_t ms, const uint8_t* data, uint8_t len)` → `"K,<ms>,<hex mayúsculas>"`
  - `class EdgeDebouncer { EdgeDebouncer(uint32_t stableMs, bool initial); std::optional<bool> update(uint32_t nowMs, bool raw); }` — devuelve el nuevo nivel solo cuando `raw` se mantuvo distinto del nivel estable durante ≥ `stableMs`.
  - `uint32_t mhzFromCount(uint32_t pulses, uint32_t windowMs)` → `pulses * 1000000 / windowMs` (entero, sin desbordar para `pulses ≤ 100000`); `windowMs == 0` devuelve 0.

- [ ] **Step 1: `platformio.ini`** con entornos `esp32dev` (framework arduino, `monitor_speed = 921600`, `test_ignore = *`) y `native` (`platform = native`, `build_flags = -std=gnu++17`, `test_framework = unity`).
- [ ] **Step 2: Escribir las pruebas que fallan**

```cpp
// test_rdp/test_main.cpp
void test_edge() { TEST_ASSERT_EQUAL_STRING("E,1234,3,1", rdp::formatEdge(1234, 3, true).c_str()); }
void test_frequency() { TEST_ASSERT_EQUAL_STRING("F,500,0,104000", rdp::formatFrequency(500, 0, 104000).c_str()); }
void test_can_std() { uint8_t d[] = {0x01, 0xAB}; TEST_ASSERT_EQUAL_STRING("C,10,1A0,s,01AB", rdp::formatCan(10, 0x1A0, false, d, 2).c_str()); }
void test_can_ext_empty() { TEST_ASSERT_EQUAL_STRING("C,10,18DAF110,x,", rdp::formatCan(10, 0x18DAF110, true, nullptr, 0).c_str()); }
void test_kline() { uint8_t d[] = {0xC1, 0x33}; TEST_ASSERT_EQUAL_STRING("K,7,C133", rdp::formatKline(7, d, 2).c_str()); }
void test_hello_status() {
  TEST_ASSERT_EQUAL_STRING("H,0,RDP1,0.1.0", rdp::formatHello(0, "0.1.0").c_str());
  TEST_ASSERT_EQUAL_STRING("S,5000,2", rdp::formatStatus(5000, 2).c_str());
}

// test_signal/test_main.cpp
void test_turn_signal_333ms_pulses_pass() {
  rdp::EdgeDebouncer d(5, false);
  TEST_ASSERT_FALSE(d.update(0, true).has_value());
  auto on = d.update(6, true);  TEST_ASSERT_TRUE(on.has_value()); TEST_ASSERT_TRUE(*on);
  TEST_ASSERT_FALSE(d.update(333, false).has_value());
  auto off = d.update(339, false); TEST_ASSERT_TRUE(off.has_value()); TEST_ASSERT_FALSE(*off);
}
void test_alternating_noise_never_emits() {
  rdp::EdgeDebouncer d(5, false);
  int emitted = 0;
  for (uint32_t t = 0; t < 1000; t += 1) if (d.update(t, t % 4 < 2).has_value()) emitted++;
  TEST_ASSERT_EQUAL(0, emitted);
}
void test_frequency_values() {
  TEST_ASSERT_EQUAL_UINT32(0, rdp::mhzFromCount(0, 250));
  TEST_ASSERT_EQUAL_UINT32(104000, rdp::mhzFromCount(26, 250));
  TEST_ASSERT_EQUAL_UINT32(0, rdp::mhzFromCount(10, 0));
  TEST_ASSERT_EQUAL_UINT32(400000000, rdp::mhzFromCount(100000, 250));
}
```

- [ ] **Step 3: Correr y verificar que fallan** — `cd firmware/probe && pio test -e native` → FAIL (símbolos no definidos).
- [ ] **Step 4: Implementar** los formateadores (hex con `snprintf("%X")`/`"%02X"`), `EdgeDebouncer` (guarda nivel estable, candidato y desde cuándo) y `mhzFromCount` (aritmética en `uint64_t`).
- [ ] **Step 5: Correr y verificar que pasan** — `pio test -e native` → todas PASS.
- [ ] **Step 6: Commit** — `git add firmware/probe && git commit -m "feat: protocolo RDP1, antirrebote y frecuencia del firmware de sondeo"`

---

### Task 3: Firmware de sondeo en el ESP32

**Files:**
- Create: `firmware/probe/src/config.h`, `firmware/probe/src/main.cpp`, `firmware/probe/src/inputs.h/.cpp`, `firmware/probe/src/pulse_counter.h/.cpp`, `firmware/probe/src/can_listener.h/.cpp`, `firmware/probe/src/kline_listener.h/.cpp`, `firmware/probe/src/selftest.h/.cpp`

**Interfaces:**
- Consumes: todo `rdp::` de la Tarea 2.
- Produces: líneas RDP1 por `Serial` a 921600 baudios; firmware versión `"0.1.0"`.
- `config.h` (constantes `constexpr`):
  - `kDigitalPins[8] = {34, 35, 36, 39, 32, 33, 25, 26}` (canales D0..D7; 34-39 son solo-entrada).
  - `kPulseChannels[] = {0, 1}` (canales con conteo PCNT; ajustables tras la exploración).
  - `kDebounceMs = 5`, `kFrequencyWindowMs = 250`, `kStatusPeriodMs = 5000`.
  - `kCanEnabled = true`, `kCanRxPin = 22`, `kCanTxPin = 21`, `kCanBitrates[] = {500000, 250000}` (prueba en orden; usa el primero que reciba tramas sin error en 3 s).
  - `kKlineEnabled = true`, `kKlineRxPin = 16`, `kKlineBaud = 10400`.
  - `kSelftestPwmPin = 23`, `kSelftestHz = 100`.

- [ ] **Step 1: Implementar los módulos**:
  - `inputs`: configura los 8 pines como entrada (sin pull internos en 34-39), muestrea cada 1 ms en una tarea, pasa cada canal por su `EdgeDebouncer` y encola `E`.
  - `pulse_counter`: una unidad PCNT por canal de `kPulseChannels` (flanco de subida, filtro de glitch de 1 µs); cada `kFrequencyWindowMs` lee y limpia el contador y encola `F` **siempre, aunque sea 0**.
  - `can_listener`: TWAI en `TWAI_MODE_LISTEN_ONLY`, filtro aceptar-todo, detección de bitrate según `kCanBitrates`; encola `C` por trama.
  - `kline_listener`: `Serial2.begin(kKlineBaud, SERIAL_8N1, kKlineRxPin, -1)` (sin TX); agrupa bytes en tramas de hasta 16 o 20 ms de silencio y encola `K`.
  - `main`: cola FreeRTOS de líneas (capacidad 512); un solo escritor a `Serial`; envío con `xQueueSend(..., 0)` y contador `dropped` cuando la cola está llena; `H` al arrancar y `S` cada `kStatusPeriodMs`.
  - `selftest` (solo con `-DRDP_SELFTEST`): genera `kSelftestHz` por LEDC en `kSelftestPwmPin`.
- [ ] **Step 2: Compilar** — `pio run -e esp32dev` → SUCCESS. Agregar entorno `esp32dev_selftest` (`extends = env:esp32dev`, `build_flags = -DRDP_SELFTEST`) y compilarlo.
- [ ] **Step 3: Prueba en banco (lazo cerrado)** — flashear `esp32dev_selftest`, puente de GPIO 23 a GPIO 34 (D0, canal de pulsos), `pio device monitor`. Esperado: `H,…,RDP1,0.1.0`, luego `F,<ms>,0,1000xx` aprox. 100000 mHz (±2 %) cada 250 ms, y `S` cada 5 s con `dropped` = 0. Quitar el puente: `F,…,0,0` sigue saliendo.
- [ ] **Step 4: Commit** — `git add firmware/probe && git commit -m "feat: firmware de sondeo ESP32 (entradas, pulsos, CAN y K-line en solo-escucha)"`

---

### Task 4: Parser RDP1 y grabador en el PC

**Files:**
- Create: `tools/rdp.py`, `tools/capture.py`, `tools/requirements.txt` (`pyserial`), `tools/tests/test_rdp.py`

**Interfaces:**
- Produces (`tools/rdp.py`):
  - `@dataclass(frozen=True) class Frame: kind: str; ms: int; fields: tuple[str, ...]`
  - `parse_line(line: str) -> Frame | None` — busca la primera aparición de `H,|S,|E,|F,|C,|K,` seguida de dígitos (tolera prefijos como la hora que agrega una app de terminal); valida el número de campos por tipo (H 2, S 1, E 2, F 2, C 3, K 1); si no cuadra, `None`.
  - `frequency_series(frames: Iterable[Frame], ch: int) -> list[tuple[int, float]]` → `(ms, hz)` de los `F` de ese canal.
- `capture.py`: `python tools/capture.py --port COM5 --baud 921600 --out viaje.rdp` escribe cada línea recibida tal cual, precedida por la hora del PC en epoch ms y un espacio; Ctrl+C cierra limpio.

- [ ] **Step 1: Escribir las pruebas que fallan**

```python
def test_parses_edge():
    assert parse_line("E,1234,3,1") == Frame("E", 1234, ("3", "1"))

def test_ignores_terminal_prefix():
    assert parse_line("12:01:02.345 F,500,0,104000") == Frame("F", 500, ("0", "104000"))

def test_ignores_capture_prefix():
    assert parse_line("1792434050123 S,5000,0") == Frame("S", 5000, ("0",))

def test_garbage_and_truncated_lines_are_none():
    for line in ["", "\x00\xffE,12", "F,500,0", "Z,1,2", "E,abc,1,1"]:
        assert parse_line(line) is None

def test_can_with_empty_data():
    assert parse_line("C,10,18DAF110,x,") == Frame("C", 10, ("18DAF110", "x", ""))

def test_frequency_series_filters_channel_and_converts_hz():
    frames = [parse_line("F,0,0,104000"), parse_line("F,0,1,5000"), parse_line("F,250,0,0")]
    assert frequency_series(frames, 0) == [(0, 104.0), (250, 0.0)]
```

- [ ] **Step 2: Correr y verificar que fallan** — `python -m pytest tools/tests/test_rdp.py -v` → FAIL (import).
- [ ] **Step 3: Implementar** `rdp.py` (regex + tabla de aridad por tipo) y `capture.py` (pyserial, `argparse`).
- [ ] **Step 4: Correr y verificar que pasan** — `python -m pytest tools/tests/test_rdp.py -v` → PASS.
- [ ] **Step 5: Commit** — `git add tools && git commit -m "feat: parser RDP1 y grabador serie"`

---

### Task 5: Calibración de velocidad contra RevScope

**Files:**
- Create: `tools/calibrate_speed.py`, `tools/tests/test_calibrate.py`

**Interfaces:**
- Consumes: `parse_line`, `frequency_series` (Tarea 4).
- Formato de entrada de RevScope (export completo del viaje, `revscope_trip_<id>.csv`): encabezado `section,timestamp,key,value1,value2`; filas OBD `obd,<epoch_ms>,<pid>,<valor>,`; velocidad = pid `0D` (km/h).
- Produces:
  - `load_revscope_speed(path: Path) -> list[tuple[int, float]]` → `(epoch_ms, kmh)` de las filas `obd` con `key == "0D"`.
  - `resample(series: list[tuple[int, float]], step_ms: int) -> list[float]` — retención del último valor sobre una rejilla regular desde el primer hasta el último timestamp (inclusive).
  - `best_offset(a: list[float], b: list[float], max_shift: int) -> int` — devuelve `s` en `[-max_shift, max_shift]` tal que `b[i]` se parece más a `a[i + s]` (máxima correlación de Pearson sobre el traslape); solo se consideran desplazamientos con al menos 60 muestras de traslape.
  - `fit_factor(hz: list[float], kmh: list[float]) -> tuple[float, float]` → `(k, r2)` con `kmh ≈ k·hz` por mínimos cuadrados sin intercepto.
  - `class CalibrationError(Exception)` — se lanza si alguna serie no tiene variación (desviación estándar 0), si el traslape es < 60 muestras o si `sum(hz²) == 0`.
  - CLI: `python tools/calibrate_speed.py --probe viaje.rdp --revscope revscope_trip_12.csv --channel 0` imprime `k (km/h por Hz)`, `pulsos por km = 3600 / k`, `R²` y el desfase en segundos. Paso de rejilla 500 ms; `max_shift` = 10 minutos. Los tiempos de cada fuente se tratan por separado (el reloj del ESP32 y el del celular no se comparan): la alineación sale solo de `best_offset`.

- [ ] **Step 1: Escribir las pruebas que fallan**

```python
def test_fit_factor_recovers_known_k():
    hz = [0, 10, 20, 30, 40]
    k, r2 = fit_factor(hz, [h * 1.8 for h in hz])
    assert abs(k - 1.8) < 1e-9 and r2 > 0.999

def test_best_offset_finds_shift():
    a = [float((i * i) % 17) for i in range(200)]
    b = a[3:] + [0.0, 0.0, 0.0]
    assert best_offset(a, b, max_shift=10) == 3

def test_constant_series_raises():
    with pytest.raises(CalibrationError):
        best_offset([1.0] * 200, [1.0] * 200, max_shift=10)

def test_all_zero_hz_raises():
    with pytest.raises(CalibrationError):
        fit_factor([0.0] * 100, [10.0] * 100)

def test_load_revscope_speed_reads_only_0d(tmp_path):
    p = tmp_path / "t.csv"
    p.write_text("section,timestamp,key,value1,value2\nobd,1000,0D,42.0,\nobd,1000,0C,3000,\ngps,1000,6.2,-75.5,40\n")
    assert load_revscope_speed(p) == [(1000, 42.0)]

def test_resample_holds_last_value():
    assert resample([(0, 1.0), (1200, 3.0)], 500) == [1.0, 1.0, 1.0]
```

- [ ] **Step 2: Correr y verificar que fallan** — `python -m pytest tools/tests/test_calibrate.py -v` → FAIL.
- [ ] **Step 3: Implementar** con stdlib (`statistics`, `math`, `csv`).
- [ ] **Step 4: Correr toda la suite** — `python -m pytest tools/tests -v` → PASS.
- [ ] **Step 5: Commit** — `git add tools && git commit -m "feat: calibración del sensor de velocidad contra la velocidad OBD de RevScope"`

---

### Después del plan (trabajo de campo del dueño, no de código)

Comprar el analizador lógico ([HiLetgo 24 MHz 8CH](https://www.amazon.com/dp/B077LSG5P2)) y los componentes de `sondas.md`; seguir `docs/fase0/README.md`; llenar `pinout-apache-rtr160-4v.md`. Con el pinout lleno se escribe el plan de la Fase 1.

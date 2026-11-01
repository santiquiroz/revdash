# Sondas de entrada — Fase 0

Las señales de la moto no se conectan directamente al ESP32 ni al analizador.
La referencia de todas las sondas es el pin **3V3 de la ESP32**, alimentada
desde el USB del portátil. El analizador HiLetgo de 24 MHz / 8 canales
(CY7C68013A, VIH ≈ 2.0 V), el ESP32 y la moto comparten GND.

## Señales digitales de lado alto

Para direccionales, luz alta y K-line:

```text
Señal ── R1 15 kΩ ──┬── Rserie 1 kΩ ── pin ESP32 / canal analizador
                     ├── R2 4.7 kΩ ── GND
                     ├── BAT54S ── 3V3
                     └── BAT54S ── GND
```

El divisor tiene factor `4.7 kΩ / (15 kΩ + 4.7 kΩ) = 0.2386`.

| Señal | Nodo calculado |
|---:|---:|
| 10.4 V | 2.48 V |
| 12 V | 2.86 V |
| 14.4 V | 3.44 V |
| 15 V | 3.58 V |

El VIH del ESP32 es `0.75 × 3.3 V = 2.475 V`. Por eso 10.4 V queda apenas por
encima del umbral. No usar 22 kΩ / 4.7 kΩ: a 12 V entrega 2.11 V, por debajo
del VIH del ESP32.

**Límites de entrada:** el ESP32 admite hasta `VDD + 0.3 V = 3.6 V`; el Saleae
Logic original, del que el HiLetgo es clon, admite de −0.5 V a 5.25 V (el clon
no publica el suyo: tomar 3.6 V como límite común). El divisor queda dentro
hasta 15 V (3.58 V), así que se puede medir con el motor en marcha (≈ 14.4 V).
Por encima de 15 V (picos de la moto) el BAT54S lleva el nodo a ≈ 3.6 V y el
1 kΩ limita la corriente hacia el pin. Antes de conectar cada canal, medir el
nodo con multímetro: debe quedar entre 0 y 3.6 V.

## Señales digitales de lado bajo

Usar para señales que la ECU o un interruptor llevan a masa: neutro y testigos
check engine/ABS cuando su driver sea de lado bajo. Con el tablero muerto estas
líneas pueden quedar flotantes; el divisor de lado alto no las detecta de forma
fiable.

```text
3V3 ── pull-up 10 kΩ ──┬── Rserie 1 kΩ ── pin ESP32 / canal analizador
                       │
                     BAT54  (ánodo en el nodo)
                       │
                       └── (cátodo) ── señal de la moto (ECU / interruptor)
GND común con la moto
```

- Señal a masa: el diodo conduce y el nodo queda ≈ 0.3 V (**0**).
- Señal flotante o a 12 V: el diodo bloquea y el nodo queda en 3.3 V (**1**).
- La orientación evita que la tensión de 12 V entre al nodo.
- La lógica queda invertida: masa significa activo / lectura 0.

Puede usarse un BAT54 individual o una mitad de un BAT54S.

## CAN (si existe)

Conectar CAN-H y CAN-L al transceptor **SN65HVD230**, alimentado a 3V3. Unir
GND del transceptor a GND común.

```text
CAN-H / CAN-L ── SN65HVD230
SN65HVD230 RX ── GPIO 22 del ESP32 + un canal del analizador
SN65HVD230 TX ── GPIO 21 del ESP32
```

El firmware mantiene el ESP32 en solo escucha y no pone el bus en dominante.
En sesiones solo con el analizador, dejar TX del transceptor a 3V3 mediante
una resistencia de 10 kΩ para que permanezca recesivo. No añadir terminación
de 120 Ω.

No conectar CAN-H/CAN-L directamente al analizador: CAN es diferencial
(≈ 2.5 V recesivo y ≈ 3.5/1.5 V dominante). El umbral fijo del analizador no
interpreta ese par diferencial.

## K-line

K-line usa la sonda digital de lado alto, sin conexión de transmisión. Llevar
la salida protegida a **GPIO 16 (UART2 RX)** y, si se desea, a un canal del
analizador.

## Seguridad de conexión

- Nunca conectar 12 V directamente a un pin del ESP32 o del analizador.
- Medir por detrás del conector con agujas de back-probing. No cortar cables.
- Mantener el portátil a batería, sin cargador, para evitar lazos de tierra.
- Conectar GND primero y desconectarlo último.
- Revisar polaridad, continuidad y tensión del nodo antes de conectar cada
  canal.

## Materiales

Cantidades para 8 canales digitales de lado alto, 4 de lado bajo y un canal
CAN; el margen cubre repuestos.

| Material | Cantidad sugerida | Estado |
|---|---:|---|
| ESP32 DevKit ESP32-D0WD-V3, USB CP210x | 1 | Ya lo tiene el dueño |
| Analizador HiLetgo 24 MHz, 8 canales | 1 | Ya lo tiene el dueño |
| Multímetro | 1 | Ya lo tiene el dueño |
| Resistencias 15 kΩ, 1/4 W (R1) | 10 | Comprar |
| Resistencias 4.7 kΩ, 1/4 W (R2) | 10 | Comprar |
| Resistencias 10 kΩ, 1/4 W (pull-up de lado bajo y TX CAN) | 8 | Comprar |
| Resistencias 1 kΩ, 1/4 W (serie a cada entrada) | 16 | Comprar |
| Diodos dobles Schottky BAT54S | 10 | Comprar |
| Diodos Schottky BAT54 individuales | 5 | Comprar |
| Módulo transceptor SN65HVD230 de 3V3 | 1 | Comprar |
| Protoboard o placa perforada | 1 | Comprar |
| Agujas de back-probing | 1 juego | Comprar |
| Cables Dupont | 1 juego | Comprar |
| Pinzas de prueba | 1 juego | Comprar |

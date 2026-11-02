# Hallazgos CAN por el OBD — 2026-11-02

Captura con RevScope v1.23.0 (modo escucha, `AT MA` + `AT CSM1`) y Vgate iCar Pro BT,
moto en el doble gato, llave en ON, motor apagado salvo que se indique.

## El bus del tablero llega al OBD

Protocolo 6 (CAN 11 bits, 500 kbit/s). En 10 s: 3276 tramas, 0 desbordes, 0 huecos.

| ID | Hz | Notas |
|---|---|---|
| 064 | 100 | |
| 300 | 20 | b0 acelerador (/2 = %), b1-b2 RPM, b3 embrague |
| 301 | 100 | b0-b1 velocidad de rueda delantera (cruda) |
| 395, 397, 399 | 20 | todo ceros incluso rodando (desconocido) |
| 400 | 10 | b1 alterna 00/01 solo; b3 desconocido (00 → 01 → 02, cambia tras mover la rueda y al pasar a 4ª) |
| 402 | 10 | b6-b7 cambian solos (¿contador/checksum?) |
| 500 | 10 | temperaturas, marcha, pata, kill switch, motor en marcha |
| 502 | 10 | b1 bit 5 = embrague; b1 bit 3 se apaga con motor en marcha; b6-b7 acumulador |
| 504 | 10 | b6-b7 cambian solos (¿contador/checksum?) |

## Señales confirmadas

| Señal | ID | Byte / bit | Valores |
|---|---|---|---|
| Marcha | 500 | b4 | 00 = N, 01-05 = 1ª-5ª, 07 = entre marchas |
| Pata lateral | 500 | b5 bit 6 | 0x40 = abajo, 0x00 = arriba |
| Embrague | 300 | b3 | 01 = apretado |
| Embrague (redundante) | 502 | b1 bit 5 | 0x20 = apretado |
| Kill switch | 500 | b7 bit 2 | 0x04 = RUN, 0x00 = OFF |
| RPM | 300 | b1-b2 (16 bits big endian) | rpm directas (2251 = PID 0C) |
| Acelerador | 300 | b0 | valor / 2 = % apertura (00 cerrado, C8 = 100 %; barrido con motor apagado) |
| Motor en marcha | 500 | b7 bit 3 | 0x08 = en marcha |
| Temperatura aire admisión | 500 | b6 | valor − 40 = °C (0x4F = 39 °C = PID 0F) |
| Velocidad rueda delantera | 301 | b0-b1 (16 bits BE) | ≈ 0.01 km/h por unidad (empujando: 224-608 = 2.2-6.1 km/h); paso 32; < 224 → 0. Confirmar contra GPS |
| Velocidad filtrada | 500 | b1 | ≈ 0.125 km/h por unidad, amortiguada (sigue a 301 con retardo). Confirmar contra GPS |
| Temperatura motor | 500 | b2 | valor − 40 = °C (0x45 = 29 °C, coincide con PID 05) |

## No están en el CAN

- Direccionales (diff vacío): van por el flasher al mazo del tablero → back-probe.
- Luz alta (diff vacío con motor en marcha); PASS se asume igual → back-probe.
- Velocidad con la rueda trasera girando en 1ª: PID 0D = 0. El sensor está en la rueda delantera
  (ABS de un canal): empujando la moto aparece en 301 y 500 b1 (ver arriba).
- Freno delantero y freno trasero (diff vacío) → back-probe en el interruptor de luz de freno.
- Botón de arranque con kill switch en OFF (diff vacío).
- Botones MODE e i (diff vacío; MODE probado con motor apagado y en ralentí). Con el tablero
  original muerto nadie publica el modo: el tablero lee los botones y se lo comunica a la ECU.
  Elegir modo desde RevDash exigiría escribir en el CAN (fase posterior, requiere capturar el
  mensaje con un tablero sano).

## ABS y pilotos

- Solo la ECU de motor (7E0/7E8) responde a diagnóstico; el ABS no contesta en 7E0-7E7 ni 700-7C0.
- La velocidad de la rueda delantera se publica a 100 Hz: el sensor y, casi seguro, el módulo ABS
  están vivos. El piloto ABS ya no prende porque lo dibujaba el tablero muerto. No prueba que la
  hidráulica actúe.
- ECU sin DTC y MIL apagado; el check engine con motor apagado es la autoprueba del piloto.
  Candidato a su estado en el bus: 502 b1 bit 3.

## Desconocidos

- 502 b1 bit 3 (0x08): presente con motor apagado, se apaga al arrancar (¿testigo de aceite o carga?).
- 502 b6-b7: acumulador que sube ~135-145 por actualización (1 Hz) en ralentí y a 3600 rpm; no escala con carga.
- 300 b6-b7: D340 en ralentí en neutro, D200 acelerado o en marcha.
- 400 b3: 00 → 01 → 02 durante la sesión; vuelve a 00 al ciclar la llave.
- 402 y 504 b6-b7: cambian solos.

## Pendiente

Prueba de ruta: escala de velocidad (contra GPS), ABS en frenada, odómetro. Back-probe: luces, direccionales, frenos, combustible (analógico).

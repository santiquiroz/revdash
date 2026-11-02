# Ingeniería inversa del CAN de motos: herramientas, técnicas y proceso repetible

Fecha: 2026-11-01. Caso piloto: TVS Apache RTR 160 4V (2026, inyección Bosch, ABS de un canal, tablero con Bluetooth SmartXonnect).
Equipo: analizador lógico de 24 MHz y 8 canales (sigrok/PulseView), ESP32 clásico con SN65HVD230, Vgate iCar Pro 2S (ELM327 "v2.3"), RevScope (Android/Kotlin, servidor MCP).

Etiquetas: **[fuente]** = leído en la fuente citada (código, datasheet o documento). **[inferencia]** = conclusión propia o conocimiento general que no verifiqué en una fuente. Hay que medirlo antes de usarlo.

---

## 0. Resumen ejecutivo

1. **Formato de captura:** conviene emitir el **log de candump** (`(1699999999.123456) can0 123#DEADBEEF`), con los metadatos en un `.json` aparte. Lo leen can-utils, python-can, cantools, SavvyCAN y Wireshark. El spec de RevScope `docs/superpowers/specs/2026-11-01-modo-escucha-can-design.md` ya lo usa. Un detalle: **no metas comentarios dentro del `.log`**, porque el lector de python-can falla con ellos. `canplayer` sí los ignora.
2. **Herramientas de PC:** SavvyCAN (MIT, activo en oct-2026) es la navaja suiza: sniffer, comparador de archivos, Range State, gráficas y DBC, y lee y escribe una docena de formatos. Cabana de comma.ai (ahora en ImGui) trae las mejores búsquedas automáticas ("Find Signal" y "Find Similar Bits"), pero solo lee en vivo (SocketCAN o panda), no archivos. La automatización va con python-can y cantools. Kayak está archivado y CANalyzat0r, quieto desde 2022.
3. **Técnica núcleo:** primero una captura base y otra con la acción. Después un mapa de bits que cambian, que separa constantes, contadores y checksums. Luego la correlación contra una referencia: búsqueda exhaustiva de (ID, bit de inicio, largo, endianness, signo) con desfase temporal. Se ajusta primero la escala y después el offset, y se **verifica en otra captura**. CSS Electronics publicó en 2026 skills MIT de Claude Code que automatizan justo ese flujo. Son el modelo a copiar para las herramientas MCP de RevScope.
4. **ELM327 como sniffer:** sirve, pero con un búfer de transmisión de **512 bytes** [fuente: datasheet]. Con `AT H1 S0 L0` cada trama de 11 bits ocupa unos 20 caracteres, así que a 115,2 kbaud el techo es de unas **575 tramas/s** y a 38,4 kbaud de unas **193**. Con espacios (`AT S1`, como pide el spec actual) cae un 30 %. Un bus como el de la KTM 690 (~350 tramas/s) cabe con S0 a 115,2 k y queda justo con S1.
5. **Vgate iCar Pro 2S:** no hay documentación pública de su chip ni de si soporta `AT CSM` o comandos ST. Hay que probarlo en banco (sección 3.4). ELM Electronics cerró en 2022 y el v2.3 era su última versión, así que un "v2.3" barato es casi seguro un clon [fuente: Wikipedia; inferencia sobre el Vgate].
6. **El sniffer serio es el ESP32:** TWAI en solo escucha más el SN65HVD230, con marcas de tiempo propias y sin el cuello de botella del ELM. ESP32RET (MIT, activo) lo convierte en un dispositivo GVRET que SavvyCAN lee en vivo. La mejor combinación: **el ELM pide PIDs OBD mientras el ESP32 escucha todo**. Las respuestas `7E8` quedan en el mismo log como referencia exacta en el tiempo.
7. **TVS Apache:** **nadie ha publicado IDs CAN ni el protocolo del tablero**; tampoco aparece qué protocolo usa el conector OBD. Sí hay ingeniería inversa del **BLE SmartXonnect** (servicio `5456534D-5647-5341-5342-454E544F5251`, tramas de 20 bytes). El tablero **emite por BLE velocidad, odómetro, combustible, trip y RPM** (trama `5A 10`). Eso es una **referencia gratis de lo que muestra el tablero** para correlacionar con el CAN. Ojo: el repo más completo es CC BY-NC 4.0 (no comercial, incompatible con la AGPL de RevDash). Usar solo los hechos del protocolo y escribir el código desde cero.
8. **Motos con material público:** KTM (IDs de la 690/1290, python), Ducati (Panigale/Multistrada, hoja colectiva, ESP32), BMW (hoja colectiva de Motorrad, sniffer del R1200GS). Para Honda, Yamaha, Kawasaki, Royal Enfield y Bajaj no encontré nada útil. Las motos pequeñas asiáticas suelen diagnosticar por K-line [inferencia].
9. **Señales analógicas:** los flotadores de combustible varían muchísimo. Un Bosch de Ducati mide 20 Ω lleno y 320 Ω vacío [fuente]; el patrón japonés típico es ~5-10 Ω lleno y ~80-100 Ω vacío [inferencia/foros]. Siempre medir. El analizador lógico es **solo digital**: para el combustible hace falta multímetro o el ADC del ESP32.
10. **Herramientas MCP prioritarias** (más allá de las 4 del spec): captura asíncrona con marcadores de evento, prueba guiada de excitación ON/OFF, mapa de bits con clasificación de contador/checksum, correlación contra referencia con ajuste de escala, verificación en otra captura y DBC por modelo con exportación.

---

## 1. Flujo profesional, herramientas y formatos

### 1.1 Flujo en 8 pasos (CSS Electronics), adaptado a moto

CSS Electronics resume el flujo manual que su skill automatiza [fuente: `re-methodology.md`]:

1. Decidir la señal y su tipo: **discreta o continua**, **estacionaria** (se provoca parado: direccional, luces, botones) **o de marcha** (velocidad, RPM, temperatura).
2. Elegir el cable. En una moto: conector OBD propietario o empalme al arnés del tablero.
3. Detectar el bitrate y conectar **en solo escucha** ("no transmitir nada en un vehículo vivo; nunca en movimiento").
4. Comparar el mundo real con el CAN: capturar con anotación sincronizada y hacer un sondeo de qué bytes se mueven.
5. Ubicar bit y largo: correlación que ordena candidatos y búsqueda exhaustiva para refinar.
6. Escala y offset: **primero la escala, después el offset**; nunca las dos a la vez.
7. Escribir la entrada DBC.
8. Transmitir. Para RevDash solo hace falta en banco, con el tablero fuera de la moto.

Dos reglas de esa metodología que valen oro [fuente]:
- **La verificación es una compuerta.** Un candidato puede correlacionar y hasta acertar en dos o tres puntos siendo la codificación equivocada. La señal típica: coincide parado y diverge en movimiento. Hay que probar parado y en marcha por separado; si no pasan los dos, queda "sin confirmar".
- **Barrido para identificar, retenciones para calibrar.** Una variación continua discrimina: la señal real da ~1,0 y los contadores ~0. Unos pocos escalones discretos dejan que un contador "correlacione" de casualidad. Las retenciones (valores fijos conocidos) sirven para fijar la escala.

### 1.2 Herramientas

| Herramienta | Qué aporta a la ingeniería inversa | Formatos | Estado y licencia | Veredicto |
|---|---|---|---|---|
| **SavvyCAN** | Sniffer estilo `cansniffer` (bytes que suben en verde, que bajan en rojo, IDs que dejan de llegar desaparecen), **File Comparator** (un archivo de interés contra un lote: IDs y bits presentes solo de un lado), **Range State** (busca campos que varían de forma coherente), **Frame Details** (por ID: intervalo, bits que cambian, histograma por byte), Flow View, Graphing, Bisector, editor DBC, UDS/ISO-TP, fuzzing | Carga y guarda GVRET CSV, candump (guarda; también lo carga), Vector ASC, BLF (carga), PCAN TRC, BusMaster, CRTD, IXXAT, CAN-DO, Vehicle Spy, Cabana CSV, PCAP de Wireshark/SocketCAN, entre otros | MIT; push 2026-10-29; rama Qt6 probada en 2026-03 | **Herramienta principal de PC** [fuente] |
| **Cabana** (comma.ai, en `openpilot/tools/cabana`) | **Find Signal**: busca por fuerza bruta todos los campos de tamaño mínimo a máximo, little o big endian, con factor y offset, que cumplan `=, >, >=, !=, <, <=, between` en una ventana de tiempo. **Find Similar Bits**: bits que siguen a un bit dado (<50 % de desacuerdo). Heatmap de cambios por bit. Editor DBC conectado a opendbc | Entradas: rutas de comma, panda, **SocketCAN**, dispositivo remoto. **No abre candump**: en Linux o WSL, reproducir con `canplayer` sobre `vcan0` | MIT (openpilot); muy activo (push 2026-10-24); reescrito en ImGui | **El mejor buscador de señales**; requiere Linux [fuente] |
| **python-can** | Biblioteca de E/S y buses; `can.LogReader` y `can.Logger` según la extensión | Lee y escribe `.asc`, `.blf`, `.csv`, `.db` (SQLite), `.log` (candump), `.mf4` (con asammdf), `.trc`; cualquiera también en `.gz` | LGPL-3.0; activo | Base de los scripts [fuente] |
| **cantools** | DBC, KCD, SYM, ARXML 3/4 y CDD; `candump can0 \| cantools decode x.dbc`; `cantools plot` lee un log de candump por stdin; `generate_c_source` para el firmware del tablero | DBC, KCD, SYM, ARXML, CDD | MIT; push 2026-10-29 | **Verificación y generación de C para el ESP32** [fuente] |
| **can-utils** | `candump -L`/`-l`, `canplayer`, `cansniffer`, `log2asc`/`asc2log`, `log2long` | candump log | Kernel Linux; activo | Referencia del formato [fuente] |
| **CANdevStudio** | Simulación con bloques (CanDevice, CanRawView, CanSignalViewer), soporta DBC, backends de Qt CAN bus | Vía Qt SerialBus/SocketCAN | MPL-2.0; push 2025-07 | Útil para **simular el bus en banco**, no para descubrir señales [fuente] |
| **cangaroo** | Analizador Qt (traza, DBC) | SocketCAN y candleLight | GPL-2.0; HubertD sin cambios desde 2023 | Opcional [fuente] |
| **Kayak** | GUI Java sobre socketcand; origen del formato **KCD** | KCD | LGPL-3.0; **archivado** (2020) | No usar [fuente] |
| **CANalyzat0r** | Proyectos en SQLite, sniffing, comparación de dumps, "buscar paquetes de una acción con filtrado de ruido de fondo" (repetición activa), fuzzing UDS | SocketCAN | GPL-3.0; quieto desde 2022 | Solo Linux; su búsqueda transmite, así que **no sirve en la moto** [fuente] |
| **Brent Stone, CAN_Reverse_Engineering** | Pipeline automático de tesis (AFIT): análisis léxico (tokenización), semántico (clustering) y correlación con J1979; acepta logs de can-utils desde 2020 | candump | GPL-3.0; 459 estrellas | Referencia de algoritmo [fuente] |
| **CSS Electronics, `can-bus-reverse-engineering-skills`** | **Skills de Claude Code** con scripts `survey`, `correlate`, `bitsearch`, `calibrate`, `build_dbc`, `verify`, `merge_dbc` y `decode_reference`; un DBC por señal y luego uno combinado | CSV de webCAN, streaming de CANsub | MIT; 242 estrellas | **Plantilla directa para el MCP de RevScope** [fuente] |
| **delsum** / **crcbeagle** / CRC RevEng | Encuentran el algoritmo de checksum o CRC a partir de muestras (suma, XOR, Fletcher, CRC con parámetros) | Hex o bytes | MIT / BSD-3 | Para el byte de checksum [fuente para delsum y crcbeagle] |

### 1.3 Formatos de captura

| Formato | Ejemplo o estructura | Quién lo lee | Comentario |
|---|---|---|---|
| **candump log** (can-utils) | `(1699999999.123456) can0 123#DEADBEEF` | can-utils, python-can, cantools, SavvyCAN, Wireshark (desde 2019) | Texto, una línea por trama, admite append. **El estándar de facto** [fuente] |
| **GVRET native CSV** (SavvyCAN) | encabezado `Time Stamp,ID,Extended,Dir,Bus,LEN,D1,...,D8` y luego `39747828,000005EB,false,Rx,0,8,E8,45,85,4B,4A,28,36,69,` | SavvyCAN | Tiempo en **µs**, ID con 8 dígitos hex, coma final. CAN FD: LEN hasta 64 y columna `FD+BRS` [fuente: `framefileio.cpp`] |
| **Cabana CSV** | `time,addr,bus,data`, con la dirección en **decimal** | SavvyCAN | Poco útil fuera del ecosistema comma [fuente] |
| **python-can CSV** | `timestamp,arbitration_id,extended,remote,error,dlc,data` con los datos en **base64** | python-can | No compatible con SavvyCAN [fuente] |
| **Vector ASC** | texto: tiempo relativo, canal, ID, `Rx`, `d`, DLC y bytes | CANalyzer, SavvyCAN, python-can, `log2asc` | Útil para entregar a talleres con Vector [fuente: soporte; ejemplo de línea de memoria] |
| **Vector BLF** | binario comprimido (propietario) | Vector, python-can, SavvyCAN (carga) | Compacto para capturas largas [fuente] |
| **ASAM MF4** | binario estándar (loggers CANedge) | asammdf, python-can | Si algún día se usan loggers profesionales [fuente] |
| **PCAN TRC** | texto de PEAK | SavvyCAN, python-can | [fuente] |

### 1.4 Bases de señales

- **DBC** (Vector): el formato universal; lo leen SavvyCAN, cabana, cantools, CANdevStudio y python-can (con cantools). **Es el entregable por modelo**.
- **KCD** (XML de Kayak): solo cantools; no vale la pena.
- **SYM** (PCAN Symbol Editor), **ARXML** (AUTOSAR), **CDD** (CANdela): cantools los lee; no aplican aquí.
- **opendbc** (comma.ai, MIT, push 2026-10-31): repositorio público de DBC de carros, **sin motos**. Su guía de estilo (mínimo de bits, unidades físicas sensatas) sirve como convención para los DBC de RevDash [fuente].

### 1.5 Qué formato emitir desde RevScope, y su gramática exacta

**Recomendación: `.log` de candump (datos) más un `.json` (metadatos y marcadores).** Para pasar a otro formato basta `log2asc` (can-utils), `python -m can.logconvert` o "Guardar como" en SavvyCAN.

Gramática que imprime `candump -L` (o `-l` a archivo) [fuente: `candump.c` y `lib.h`]:

```
'(' SEGUNDOS '.' MICROSEGUNDOS ')' ' ' INTERFAZ ' ' TRAMA [' ' ('R'|'T')] '\n'

SEGUNDOS      = epoch en decimal; candump lo rellena con ceros a 10 dígitos ("%010llu")
MICROSEGUNDOS = exactamente 6 dígitos ("%06llu"); con -N se usan 9 dígitos (ns)
INTERFAZ      = "can0", "vcan0", "slcan0"... (candump la alinea a la derecha con espacios
                si hay varias interfaces de nombres distintos)
TRAMA (CAN clásico) = ID '#' { 'R' [len] | DATOS } [ '_' DLC9..F ]
  ID     = 3 dígitos hex (11 bits, ≤7FF) o 8 dígitos hex (29 bits; ese largo marca "extendido")
  DATOS  = 0 a 8 bytes, 2 dígitos hex cada uno, en MAYÚSCULAS, sin separador ("123#" = 0 bytes)
  'R'    = trama remota; "123#R" = len 0, "123#R7" = len 7
TRAMA (CAN FD) = ID '##' FLAGS DATOS      (FLAGS = 1 dígito hex: 1 = BRS, 2 = ESI)
Sufijo " T"/" R" = solo con -x (dirección TX/RX); python-can lo acepta y lo puede omitir
```

Ejemplos válidos:

```
(1730000000.123000) can0 7E8#03410D00
(1730000000.124512) can0 12345678#0102
(1730000000.130000) can0 123#
(1730000000.140000) can0 123#R
```

Trampas:
- **Nada de comentarios** (`# base`, `; acción`) dentro del `.log`. `CanutilsLogReader` de python-can hace `temp.split()` en cada línea no vacía y falla. `canplayer` salta las líneas que no empiezan con `(`. Los marcadores van al `.json` o a un `.events.csv` aparte [fuente: código de ambos].
- El ID de 8 dígitos es lo **único** que marca "extendido". Un ID de 11 bits jamás se escribe con 8 dígitos.
- Hex en mayúsculas (así escribe candump; los lectores aceptan minúsculas).
- Marcas de tiempo absolutas y crecientes. Si el reloj salta, python-can reutiliza la marca anterior. Con el ELM la marca es la hora de llegada al teléfono (sección 3.3): **guardar en el JSON el origen de la marca** (`"reloj": "telefono"` o `"hw"`).
- Interfaz: usar siempre `can0`. SavvyCAN escribe `vcan0`; da igual.
- El spec de RevScope ya produce `(1730000000.123000) can0 7E8#03410D00` con milisegundos y tres ceros. Es correcto; si un día se usa el ESP32 con marca de µs, el mismo formato admite los 6 dígitos reales.

Metadatos mínimos en el JSON: modelo de moto, VIN parcial, etiqueta, marcadores `[{t, etiqueta}]`, bitrate, adaptador y firmware (`AT I`, `AT @1`, `STI`), si fue silencioso (`AT CSM1` → `OK`), filtros, número de `BUFFER FULL` e intervalos sin datos, duración y estado del motor (contacto o ralentí).

---

## 2. Técnicas para identificar señales

### 2.1 Taxonomía primero [fuente: CSS]
- **Discreta** (testigo, botón, neutro, marcha): casi siempre 1-2 bits; escala 1, offset 0.
- **Continua** (velocidad, RPM, temperatura, combustible): casi siempre 1-2 bytes, a menudo alineada a byte, big o little endian; necesita escala y offset.
- Las estacionarias se provocan a voluntad: base contra acción. Las de marcha necesitan una referencia registrada en el tiempo.

### 2.2 Diferencia entre la captura base y la de acción
- **Método del spec de RevScope** (correcto): ignorar los bytes que ya variaban en la base (contadores, checksums) y bajar a nivel de bit dentro de esos bytes, para no perder un testigo empaquetado junto a un contador.
- **Mejora clave: excitación repetida en una sola captura con marcadores** en vez de dos capturas separadas. Por ejemplo: direccional izquierda 5 s ON y 5 s OFF, tres veces. Luego se correlaciona cada bit con la onda cuadrada de los marcadores (o se exige que el bit cambie en las seis transiciones con un retardo menor a ~300 ms). Elimina falsos positivos por coincidencia, como el ventilador que arranca justo durante la "acción", y no depende de que dos capturas tengan el mismo estado de fondo [inferencia; misma idea que el "File Comparator" de SavvyCAN y los "holds" de CSS].
- Para la direccional: el bit **parpadea** a ~1-2 Hz. Hay que buscar la frecuencia del bit, no solo "cambió".
- La **ausencia** también es señal: IDs que aparecen solo con la acción (por ejemplo, un mensaje de evento) o solo con el motor encendido.

### 2.3 Mapa de bits que cambian (heatmap)
- Por cada ID y cada uno de los 64 bits: cuántas veces cambió entre tramas consecutivas del mismo ID. Cabana lo calcula así: `diff = data[i] ^ previo[i]`, cuenta bits y bytes que cambiaron, y **no cuenta como cambio un byte que aparece por primera vez** [fuente: `core/heatmap.h`].
- Patrones que se leen de inmediato [fuente: READ y resumen de patente; prácticas]:
  - **Constante** (0 cambios): relleno o una señal que no excitaste.
  - **Contador**: el bit menos significativo cambia en cada trama (tasa ~1,0), el siguiente en la mitad (~0,5), luego ~0,25... (tasa que se divide a la mitad hacia el bit más significativo). Típico en motos: un nibble 0-F o un byte 0-FF [inferencia].
  - **Checksum o CRC**: todos sus bits con tasa ~0,5, sin estructura; cambia siempre que cambia cualquier otro byte.
  - **Señal física**: tasa alta en los bits bajos que decrece hacia los altos, y que **se corta** en el borde del campo. **READ** marca un límite de señal cuando el log10 de la tasa de cambio de un bit es menor que el del bit anterior; LibreCAN agrega un umbral de caída porcentual [fuente].

### 2.4 Contadores y checksums
- Contador: `(valor_n - valor_{n-1}) mod 2^k == 1` en >95 % de las tramas. Suele ir en el nibble bajo o alto del último o del primer byte [inferencia].
- Checksum: probar en orden (es barato hacerlo en el teléfono):
  1. XOR de los otros bytes;
  2. suma mod 256 y su complemento (`0xFF - suma`, `(-suma) & 0xFF`);
  3. suma que incluye el ID o el contador;
  4. CRC-8 con polinomios comunes (SAE J1850 `0x1D`, AUTOSAR `0x2F`, `0x07`), init y xorout `0x00`/`0xFF`, con y sin el ID.
  Si nada calza, exportar muestras a **delsum** o **crcbeagle** en el PC [fuente de las herramientas; lista de algoritmos = inferencia].
- Para qué sirve: (a) marcar esos bits para que el diff y la correlación los ignoren; (b) más adelante, en banco, poder **generar** tramas válidas para el tablero original. Dato de referencia: el protocolo BLE de TVS usa "suma en complemento a 1" en una variante [fuente: RR310 Nav Bridge].

### 2.5 Correlación con una referencia
Referencias posibles, de mejor a peor:
1. **Una referencia decodificada dentro del mismo log.** Ejemplo: las respuestas OBD `7E8` a los PID `0D` (velocidad) y `0C` (RPM), sniffeadas por el ESP32 mientras el ELM las pide. CSS reporta correlación ≈0,99 contra ≈0,55 de una rampa a mano. **Excluir los IDs de la referencia** (`7DF`, `7E0`, `7E8`) de la búsqueda, o se encuentra a sí misma [fuente: CSS].
2. **La telemetría BLE del tablero TVS** (velocidad, RPM, combustible y odómetro tal como los muestra el tablero; sección 4.2). Es la "verdad" de lo que el tablero de reemplazo tiene que replicar. Solo marcas TVS.
3. **La velocidad GPS del teléfono** (1-10 Hz, con retardo de ~0,5-1 s, inútil por debajo de ~5 km/h) [inferencia].
4. **Video del tablero** sincronizado (CSS tiene `vision_reference.py`, que lee el valor del tablero con visión) [fuente].

Algoritmo, el estilo de CSS `correlate`/`bitsearch` y de cabana "Find Signal":
- Remuestrear la referencia a los instantes de cada trama del ID candidato (interpolación lineal; mantener el último valor si es escalón).
- Para cada ID, bit de inicio de 0 a 63, largo de 1 a 16, LE o BE, con o sin signo: extraer la serie cruda y calcular **Spearman** (robusto a escala y offset desconocidos y a relaciones monótonas no lineales) con **búsqueda de desfase** de ±1-2 s.
- Ordenar y quedarse con los 20 mejores. Desempatar con un "prior" de plausibilidad: alineado a byte, 8 o 16 bits, valor dentro de un rango físico [fuente: CSS `signal-encoding.md`].
- Ajuste: regresión robusta raw→referencia; **redondear la escala** a valores "de fabricante" (0,1, 0,125, 0,25, 0,5, 1/256, 0,01...) y recién entonces el offset.
- **Verificar** en otra captura (otro viaje) separando parado y en movimiento.

### 2.6 Endianness, signo y escala
- Intel (little endian): `raw = (int.from_bytes(p,'little') >> start) & mask`. Motorola (big endian): en DBC/cantools el `start` es el **MSB** en numeración "diente de sierra" (`start = byte*8 + 7` para un campo que empieza en un byte) [fuente: CSS]. Es el error clásico al pasar a DBC: validar siempre decodificando con cantools.
- Señal de endianness equivocada: un campo de 16 bits leído al revés **salta** cada vez que el byte bajo da la vuelta (dientes de sierra). El correcto es suave.
- El TVS BLE usa **big endian** (odómetro de 24 bits BE ÷ 10 = km). Leído LE daba un factor ~16,8 equivocado y "parecía un número" [fuente: TVSRideCompanion]. Moraleja: **un test construido con el mismo codificador que el decodificador no detecta el error**; solo los bytes reales del bus lo detectan.
- Signo: complemento a 2. Lo delatan valores alrededor de 0 que saltan entre 0x00 y 0xFF (inclinación, aceleración).
- Mín/máx del DBC = **rango representable** por los bits, no el observado [fuente: CSS].

### 2.7 Lo que dicen los papers (para no reinventar)
| Trabajo | Idea | Resultado o nota |
|---|---|---|
| **READ** (Marchetti y Stabili, IEEE TIFS 2019) | Límites de señal por caída de la tasa de cambio de bits; etiqueta contadores y CRC | Base de todos los demás [fuente] |
| **LibreCAN** (Pesé et al., ACM CCS 2019) | READ con umbral, más correlación con OBD-II y sensores del teléfono; también datos de carrocería | "Matriz de comunicación en menos de 2 minutos" [fuente] |
| **CAN-D** (Verma et al., ORNL, IEEE TVT 2021) | 4 pasos: límites (clasificador), **endianness**, signo, interpretación física con diagnóstico | Con endianness el espacio pasa de 128 a 4,72E21 tokenizaciones [fuente] |
| **ACTT** (Verma et al.) | Tokenización y traducción | [fuente: citado en ByCAN] |
| **CANMatch** (Buscemi et al., IEEE TVT 2021) | Reutiliza IDs entre modelos (comparación de tramas), sin esfuerzo manual | Interesante para **reutilizar entre modelos de una misma marca** [fuente] |
| **ByCAN** (2024) | Características por byte y por bit + DBSCAN; DTW contra PIDs OBD | Precisión de segmentación 80 % contra 64 % de CAN-D y 52 % de READ [fuente] |
| **SAE 2026-01-0070** | Ingeniería inversa capturando tráfico de un probador de diagnóstico y repitiéndolo | Coincide con el "modo escucha del Taller" de RevScope [fuente] |

### 2.8 Lista accionable
1. 60 s con contacto y motor apagado → inventario de IDs, Hz, DLC y bits que cambian.
2. 60 s en ralentí → qué aparece o cambia con el motor (RPM, temperatura, carga).
3. Clasificar cada byte o bit: constante, contador, checksum o candidato. Excluir contadores y checksums.
4. Por cada señal discreta: prueba guiada ON/OFF ×3 con marcadores → bits que siguen la onda.
5. Por cada continua: viaje con referencia (BLE TVS, GPS, PID por ESP32+ELM) → correlación exhaustiva → ajuste → verificación en otro viaje.
6. Escribir el DBC del modelo; decodificar con cantools un log nuevo y comparar.
7. Banco (opcional): reproducir hacia el tablero original con el ESP32, **fuera de la moto**, para confirmar.

---

## 3. El ELM327 (Vgate) como sniffer

### 3.1 Comandos que importan [fuente: datasheet de ELM327, salvo que se indique otra]

| Comando | Qué hace | Detalle |
|---|---|---|
| `AT MA` | Monitorea **todo** lo que ve en el bus | Silencioso: no manda ACK en CAN "salvo que apagues el modo silencioso con CSM0". Se detiene con **cualquier carácter** por RS232; termina la línea que estaba imprimiendo, escribe `STOPPED` y vuelve a `>`. El carácter que lo detiene se descarta. Hace un "protocol close" interno; al volver a pedir OBD se reinicializa (puede salir `SEARCHING...`). **Respeta los filtros previos** (CF, CM, CRA): si quedó `CRA 4B0`, `MA` solo ve 4B0 |
| `AT MR hh` | Solo mensajes **para** el receptor `hh` | En CAN de 11 bits mira los bits 8-10 del ID; en 29 bits, los bits 8-15. Sobrescribe esos bits del filtro |
| `AT MT hh` | Solo mensajes **del** transmisor `hh` | En CAN mira los bits 0-7 del ID |
| `AT CRA hhh` / `hhhhhhhh` | Dirección de recepción exacta (con `X` como comodín, por ejemplo `CRA 1XX`) | `AT CRA` sin argumento, o `AT AR`, restablece filtros y máscaras |
| `AT CF hhh` + `AT CM hhh` | Filtro y máscara de ID: deja pasar si `(id & máscara) == (filtro & máscara)` | Con `CM 700` + `CF 100` pasa 0x100-0x1FF. Base para el **barrido por máscaras** |
| `AT CSM0`/`CSM1` | Monitoreo silencioso apagado o encendido | Valor por defecto según PP 21 (de fábrica `FF` = encendido). Aparece en **v1.4b**. "Mantener el modo silencioso hasta estar seguro del baudrate"; con el baud equivocado y sin modo silencioso **se perturba el bus** |
| `AT H1` | Muestra los encabezados (el ID CAN) | **Imprescindible** para sniffear |
| `AT S0` | Sin espacios | Ahorra ~30 % de caracteres (sección 3.3) |
| `AT CAF0` | Sin formato automático CAN | Muestra los 8 bytes crudos, sin interpretar el PCI de ISO-TP |
| `AT D1` | Muestra el DLC | Opcional con S0 (el largo sale del número de caracteres) |
| `AT L0`, `AT E0` | Sin salto de línea extra y sin eco | Menos bytes |
| `AT SP 6` / `AT SP 8` | ISO 15765-4 CAN de 11 bits a 500 k / a 250 k (7 y 9 = 29 bits) | Para un bus no OBD: `AT PB xx yy` (protocolo B de usuario) con bitrate = 500k/n; el ejemplo del datasheet a 33,3 k es `AT PB C0 0F` |
| `AT CS` | Contadores de error TX y RX (y en v2.2, la frecuencia CAN) | Sirve para diagnosticar el bitrate antes de capturar |
| `AT FT` | Capa extra de filtro | Solo en v2.3 genuino [fuente: Wikipedia] |

### 3.2 Secuencia recomendada (ajustes propuestos al spec de RevScope)

```
ATZ / ATD                 (o el estado que deje la app)
ATE0  ATL0
ATSP6                     (o ATSP0 + 0100 + ATDPN para descubrir el protocolo)
ATH1  ATS0  ATCAF0  ATD0
ATCSM1                    → "OK" = silencioso; "?" = no lo soporta (clon con CSM fijo encendido, o versión <1.4b)
ATCRA / ATCF+ATCM         (opcional, para filtrar)
ATMA                      → leer líneas; si llega "BUFFER FULL" o ">": registrar el hueco y relanzar ATMA hasta cumplir la duración
<cualquier carácter>      → esperar "STOPPED" y ">"
ATCAF1 ATAR ATS0 ATH0     (restaurar en finally a los valores de ElmCommandBuilder, como dice el spec)
```

Diferencias con el spec actual:
- **`AT S0` en lugar de `AT S1`.** El parser ya conoce `bits29` por el protocolo, así que un ID de 3 u 8 dígitos se separa sin espacios. El propio spec admite líneas sin espacios. Gana ~30 % de capacidad.
- **Relanzar automáticamente después de `BUFFER FULL`** (hoy el spec termina antes con `>`), y guardar en el JSON la lista de huecos `[t_inicio, t_fin]`. Una captura con huecos sigue sirviendo para el diff, pero la medición de Hz tiene que descontarlos.
- Advertencia del kernel de Linux (driver `can327`): en los **clones** el CSM está fijo encendido, así que `AT MA` siempre es silencioso. Después de **transmitir** un pedido, el chip queda en modo respuesta y **sí manda ACK** hasta que el bus calla o vence el tiempo. No mezclar pedidos OBD con escucha si se quiere silencio total [fuente: `can327`].

### 3.3 Búfer y throughput reales
- El búfer de transmisión RS232 es de **512 bytes** desde v2.0 (256 antes) [fuente: datasheet y Wikipedia; STN dice "256 bytes del ELM327"].
- El datasheet mide, con `H1 S0 L0` y 8 bytes de datos, un **tiempo medio de envío por trama** de 11 bits de **5180 µs a 38,4 k**, **1740 µs a 115,2 k** y **400 µs a 500 k** (29 bits: 6475, 2175 y 500). Es decir, **~193, ~575 y ~2500 tramas/s** sostenidas [fuente]. A 40 % de carga caben de 28 a 38 tramas en el búfer antes del desborde; "con buses muy cargados vemos ~150 mensajes antes de BUFFER FULL; con S0 y L0 normalmente no aparece" (eso es con RS232 a 500 k) [fuente].
- Cálculo por línea (10 bits por carácter en el UART) [inferencia aritmética]:

| Línea (8 bytes) | Caracteres | 38,4 k | 115,2 k | 500 k |
|---|---|---|---|---|
| 11 bits, S0 | 20 | 192/s | 576/s | 2500/s |
| 11 bits, S1 | 28 | 137/s | 411/s | 1785/s |
| 29 bits, S0 | 25 | 154/s | 461/s | 2000/s |
| 29 bits, S1 | 36 | 107/s | 320/s | 1390/s |

- Referencia de carga real: la KTM 690 tiene 7 IDs a 10-100 ms → **~350 tramas/s** [cálculo desde la tabla de `ktm-can`]. Una moto pequeña india probablemente tenga menos IDs [inferencia], pero hay que medir.
- **En los adaptadores Bluetooth el UART interno entre el chip y el módulo BT lo fija el fabricante** (a menudo 38,4 k o 115,2 k en los clones de módulo HC-05 y similares) y `AT BRD` puede dejarlo incomunicado [inferencia]. Si el Vgate integra MCU y BT en un solo chip, el límite pasa al enlace BT. **Medir:** 10 s de `AT MA` sin filtros, contar líneas por segundo y `BUFFER FULL`, comparando S0 contra S1 y Classic contra BLE.
- **BT Classic (SPP) contra BLE** [inferencia]: SPP suele ser más rápido y estable para un flujo continuo. En BLE el techo depende del intervalo de conexión (pedir `CONNECTION_PRIORITY_HIGH`) y del MTU (las notificaciones de 20 bytes ≈ una línea por notificación).
- **Marcas de tiempo:** el ELM no las pone. RevScope marca la llegada, y BT entrega en ráfagas (varias líneas por paquete), así que el jitter es de decenas de ms [inferencia]. Sirve para el diff y para el Hz promedio de una ventana larga, **no** para medir el período de un ID ni para correlación fina. El ESP32 marca en hardware.

### 3.4 Vgate iCar Pro 2S: qué se sabe y qué probar
- Público: se anuncia como "ELM327 V2.3", Bluetooth 5.3, para Android, iOS y PC; un review mide unos 21 ms de respuesta por PID contra ~7 ms de un vLinker. **Nada público sobre `AT MA`, `CSM` ni comandos ST** [fuente: listados y reviews].
- Contexto: ELM Electronics anunció en 2020 su cierre (efectivo en junio de 2022). Las versiones reales llegan hasta v2.3 (`AT FT`, modos de flow control) y nunca existió un v1.5. Los clones suelen llevar firmware v1.4 copiado y declaran cualquier versión [fuente: Wikipedia]. **Un Vgate "v2.3" es casi seguro firmware propio compatible, no un PIC de ELM** [inferencia].
- Prueba de banco (5 minutos, con RevScope por `comando_lectura`, o con una terminal BT):
  1. `AT I`, `AT @1`, `AT @2` → cadena de identidad.
  2. `STI`, `STDI` → si responde, es un **STN** (OBDLink); si da `?`, no lo es.
  3. `AT CSM1` → `OK` o `?`. `AT PP 21` no existe en clones (`?`).
  4. `AT FT` (solo v2.3 genuino) → probablemente `?`.
  5. Con una fuente de tramas en banco (el ESP32 transmitiendo IDs conocidos a una tasa conocida, con un terminador de 120 Ω): medir tramas/s máximas sin `BUFFER FULL`, con S0 y con S1, por SPP y por BLE. Ese número define si el ELM alcanza para el bus de la moto o si hay que filtrar o usar el ESP32.
  6. Verificar el silencio: con el ESP32 como **único** nodo transmisor y el Vgate en `AT MA`, si nadie da ACK el ESP32 entra en error pasivo y retransmite. Si el Vgate diera ACK, la trama se aceptaría. Así se comprueba si el CSM es real [inferencia de método].

### 3.5 Trucos para no perder tramas
1. `H1 S0 L0 E0 CAF0 D0`: líneas mínimas.
2. **Filtrar por ID** cuando ya se sabe qué buscar (`AT CRA 3XX`, o `CF`/`CM`).
3. **Barrido por máscaras:** partir el espacio de 11 bits en 8 bloques (`AT CM 700` + `AT CF 000`, `100`... `700`) y repetir la misma excitación en cada bloque. Para el diff no hace falta que todos los IDs se capturen a la vez [inferencia].
4. **Relanzar `AT MA` automáticamente** después de `BUFFER FULL` y registrar los huecos.
5. Leer el socket con un hilo dedicado y un búfer grande (que la app nunca sea el cuello de botella).
6. **No usar el ELM para medir período ni latencia.**
7. Si el bus supera la capacidad incluso filtrado: **ESP32**.

### 3.6 STN11xx/STN21xx (OBDLink) contra ELM327 [fuente: OBDLink FRPM]
- **`STMA`** monitorea todo, pero "en protocolos CAN trata todos los mensajes como ISO 15765". **Para CAN crudo se usa `STM`** con los filtros actuales (`STFPA` o `STFAP` de paso, `STFBA` de bloqueo, `STFPC` para limpiar). Ambos aceptan `n` mensajes (1-32767) antes de salir solos.
- Filtros dinámicos múltiples de paso, bloqueo y flow control. Cada uno reserva RAM: si se agregan demasiados, `OUT OF MEMORY`.
- Búfer: **2 KB** (STN1110/STN1170, OBDLink SX/LX/MX) y **4 KB** (STN2100/STN2120, OBDLink EX/MX+), contra 256-512 bytes del ELM.
- UART de hasta **2 Mbps** (1 Mbps con eco) y cambio "amable" con `STSBR`. `BUFFER FULL` es "poco común en OBDLink".
- Conclusión: un OBDLink (USB o MX+) sería el ELM ideal para sniffear. **Con lo que ya hay, el ESP32 lo supera.**

### 3.7 ESP32 + SN65HVD230: el sniffer recomendado
- TWAI en **modo solo escucha**: sin ACK y sin transmitir. Marcas de tiempo de µs. Salida por USB serie de alta velocidad, por WiFi o por BLE [inferencia de capacidades; RevDash ya lo exige así en `CLAUDE.md`].
- **ESP32RET** (MIT, push 2026-10-22): firmware que hace al ESP32 hablar GVRET con **SavvyCAN en vivo** (USB o WiFi), con soporte LAWICEL experimental y **emulación de ELM327 por Bluetooth** [fuente]. Alternativa: un firmware propio que escupa líneas candump por BLE a RevScope.
- **Combinación ganadora:** el ESP32 escucha todo (es la captura); el Vgate pide PID `0D`/`0C`/`05`/`11` por OBD (la referencia). Las respuestas `7E8` quedan **en el mismo log con la marca de tiempo del ESP32**, y la referencia queda perfectamente sincronizada [inferencia, basada en la regla "referencia decodificada en el log" de CSS].
- El analizador lógico sirve para **confirmar el bitrate y el cableado**: medir el **RXD del SN65HVD230** (lógica de 3,3 V), no CAN-H y CAN-L directamente; decodificar con el decoder `can` de sigrok (24 MHz sobra para 500 kbps). En K-line: decoder UART a 10400 baudios, con un divisor o un transceptor tipo L9637 [inferencia].

---

## 4. Motos: lo que ya está publicado

### 4.1 Otras marcas

| Marca | Recurso | Qué tiene | Licencia o nota |
|---|---|---|---|
| **KTM** | [blalor/ktm-can](https://github.com/blalor/ktm-can) | 690 Enduro R 2020 (decodificación original de la 1290 SuperDuke 2017): `120` RPM, acelerador, interruptor de parada y mapa (20 ms); `129` marcha y embrague (20 ms); `12A` mapa pedido; `12B` velocidad de rueda delantera y trasera, inclinación (10 ms); `290` presión de freno delantero (10 ms); `450` botón de control de tracción; `540` RPM, marcha, pata lateral, temperatura (100 ms). Conector de diagnóstico Sumitomo MT 090 de 6 pines; scripts con python-can para correlacionar | Python [fuente] |
| KTM | [hilo de ADVrider sobre la SuperDuke 1290](https://www.advrider.com/f/threads/results-from-hacking-the-ktm-superduke-1290-can-bus.1200087/) | Origen de esos IDs | Foro [fuente] |
| KTM 390 Gen1 | [lab-vex/OBDuke](https://github.com/lab-vex/OBDuke) | Diagnóstico por pedidos propietarios de KTM (RPM con `2121`, marcha con `22F010`). Nota: "la velocidad de las ruedas viene del ABS Bosch y KTM nunca publicó sus IDs"; sin captura pública de la 390 | [fuente] |
| **Ducati** | [renatobo/DucatiPanigaleCanBus](https://github.com/renatobo/DucatiPanigaleCanBus) | ECU Mitsubishi (Panigale 899-1299, Multistrada 1200 2010-14, Diavel): velocidad de rueda, RPM, marcha, acelerador a 20-25 Hz; temperaturas y batería a 1 Hz. **ESP32 + SN65HVD230 → BLE** (Harry Lap Timer). Es literalmente el patrón de RevDash | MIT [fuente] |
| Ducati | [hoja colectiva de mensajes CAN de Ducati](https://docs.google.com/spreadsheets/d/1-NJ9OlGQYTGMzBzwDPYn-aI_7_ign9SCiscKZufx3Uw/edit) | IDs decodificados por la comunidad | [fuente: awesome-automotive-can-id] |
| Ducati 696 | [Pulse Security, parte II](https://pulsesecurity.co.nz/articles/ducati-696-part2) | Tráfico ECU→tablero: `0x100` RPM y velocidad; método documentado | [fuente] |
| **BMW** | [hoja "BMW Motorrad CAN messages"](https://docs.google.com/spreadsheets/d/1tUrOES5fQZa92Robr6uP8v2dzQDq9ohHjUiTU3isqdc/edit) y [4G-Gregg/BMW-GS-CAN-Sniffer](https://github.com/4G-Gregg/BMW-GS-CAN-Sniffer) | R1200GS 2010 con Arduino y shield CAN; hoja colectiva | [fuente] |
| Buell y generalidades | Charla de DEF CON 28 (Car Hacking Village) y ROOTCON 14 "Bikes can do CAN too" | 500 kbps, IDs de 11 bits, terminación de 120 Ω; "la mayoría de fabricantes europeos y japoneses adoptaron CAN hacia 2003" | [fuente: resumen de búsqueda; el PDF de ROOTCON era demasiado grande para leerlo] |
| Índice | [iDoka/awesome-automotive-can-id](https://github.com/iDoka/awesome-automotive-can-id) | Su sección de motos solo lista BMW, Ducati y KTM | [fuente] |
| Honda, Yamaha, Kawasaki, Royal Enfield, Bajaj | — | **No encontré** repos ni DBC. Las búsquedas sobre la Himalayan 450 (tablero Tripper) y Pulsar/Dominar solo dieron marketing | Hueco de mercado = oportunidad |

### 4.2 TVS Apache RTR 160/200 4V

**CAN y tablero:** **nada publicado** (búsquedas en inglés de GitHub, foros y Team-BHP, este último bloqueado con 403). No hay pinout público del conector OBD de la Apache ni confirmación de si es CAN (ISO 15765) o K-line (ISO 14230). Lo que sí consta [fuente]:
- TVS dijo que pasaría todo su portafolio a **OBD-2B antes de marzo de 2025** (empezó con el Jupiter 110). La India exige OBD-II en dos ruedas desde BS6 fase II (abril de 2023). El borrador del GTR habla del conector ISO 15031-3 de 16 pines, pero no confirmé el conector real de TVS.
- La RTR 160 4V es la única TVS con **ABS de un canal**. Bosch ofrece un ABS 10 de un canal (solo rueda delantera), pero **no confirmé que sea Bosch el ABS de esta moto**.
- Hipótesis a probar [inferencia]: con modos de manejo, ABS e inyección, ECU, ABS y tablero casi seguro comparten un bus (CAN a 500 k es lo más probable). En la mayoría de motos pequeñas sin gateway, el conector de diagnóstico está **en el mismo bus** que el tablero. Primera prueba: `ATSP0` + `0100` + `ATDPN` con el Vgate; si responde 6 u 8, es CAN.

**BLE SmartXonnect (sí hay ingeniería inversa, 2026):**

| Repo | Qué cubre | Licencia |
|---|---|---|
| [athulkrishna2015/TVSRideCompanion](https://github.com/athulkrishna2015/TVSRideCompanion) (`docs/PROTOCOL.md`, oct-2026) | Ntorq, Jupiter, Apache, Raider, iQube. Navegación y telemetría; simulador en Python | **CC BY-NC 4.0** (no comercial) |
| [72141299/TVS-APACHE-RR310-MODIFIED-APP](https://github.com/72141299/TVS-APACHE-RR310-MODIFIED-APP) | Apache RR 310: puente de navegación; "XOR `0xEA`, checksum de suma en complemento a 1" | Sin licencia (todos los derechos reservados) |
| [sirajju/tvs-rea](https://github.com/sirajju/tvs-rea) (`BLE_PROTOCOL.md`) | Keyless (find, lock, unlock, trunk) por desafío y respuesta AES-CTR; XOR con byte configurable | Sin licencia |
| [Hilo de TechEnclave](https://techenclave.com/t/making-alternative-to-tvs-connect-app-need-some-help/423446) | Alternativa a TVS Connect para el Jupiter | Foro (403 al leerlo) |

Hechos del protocolo [fuente: `PROTOCOL.md` de TVSRideCompanion; varios marcados allí como verificados en el APK, otros solo en el Ntorq o el Jupiter]:
- GATT: servicio `5456534D-5647-5341-5342-454E544F5251` (ASCII "TVSM-VGSASBEN TORQ"), escritura `00005352-0000-1000-8000-00805f9b34fb`, lectura y notificación `00005354-0000-1000-8000-00805f9b34fb`. Las clases de configuración del APK declaran **las mismas UUID para Ntorq, Jupiter, Apache e iQube** (el U388 eléctrico es distinto). **No hay Bluetooth Classic.** El tablero **no anuncia la UUID del servicio**: hay que buscarlo por nombre.
- Tramas de **20 bytes** que terminan en `0xFF`. Inicio `5A`/`5B` del teléfono al tablero y `9A`/`9B` en el desafío. Un keepalive `5B 4A ...` cada 2 s.
- **Telemetría del tablero al teléfono:** `5A 10` (según el parser del APK: byte 2 = velocidad, bytes 3-5 = **odómetro BE ÷ 10**, byte 6 = combustible en barras en el nibble bajo, bytes 14-16 = trip, bytes 17-18 = **RPM**), `5A 11` (estado y versión), `5A 16` (frecuente, sin decodificar), `5A 18` (diagnóstico del motor). En el Ntorq 125 se midieron ~1 trama/s de cada una.
- Handshake: desafío `9B F2` con 16 bytes, respuesta `9A F1` con AES-128-CTR (el IV es el desafío). La clave está **dentro del APK** y dos apps distintas la traen casi igual. En el Ntorq 125 medido **no hubo desafío**. NVD registra **CVE-2024-35537** (criptografía débil en TVS Connect 4.6.0 Android / 5.0.0 iOS).
- El byte de "combustible" del Ntorq **no se comportaba como un nivel** (sin cambio en 772 tramas; tanque vacío = 7). Está abierto.

**Por qué importa para RevDash:**
1. **Referencia de verdad para correlacionar** el CAN de la Apache: velocidad y RPM **tal como las muestra el tablero**, más el odómetro, por BLE y sin tocar el arnés [inferencia; hay que confirmar que la Apache 160 4V usa el mismo layout `5A 10`, porque el APK tiene un paquete `protocol/apache` propio].
2. El tablero de reemplazo puede **mantener las funciones SmartXonnect** (navegación, llamadas) si implementa ese protocolo; o puede pedirle al teléfono los mismos datos.
3. **Licencias:** para RevDash (AGPL) y cualquier uso comercial, **no copiar código** de TVSRideCompanion (NC) ni de los repos sin licencia. Los hechos de un protocolo (UUID, layout) no son código, pero conviene hacer una implementación limpia propia y validarla con tus propias capturas (`btsnoop` de Android con la app oficial). Las leyes de interoperabilidad varían por país [inferencia; no es consejo legal].

---

## 5. Señales analógicas típicas de tableros de motos pequeñas

> El analizador lógico de 24 MHz (clon FX2) es **solo digital**, con entradas de ~3,3 V y tolerancia de 5 V [inferencia general sobre estos clones]. **Nunca** conectar 12 V directo: divisor (por ejemplo 10 kΩ / 3,3 kΩ → 12 V ≈ 3 V; 15 V ≈ 3,7 V) más un clamp a 3,3 V (RevDash ya tiene las sondas con BAT85). Para el combustible y las temperaturas: multímetro, o el ADC del ESP32 con un divisor.

### 5.1 Combustible (flotador con resistencia variable)
- Valores muy dispares según la marca:
  - **Bosch en Ducati Scrambler 800 (2019+): 320 ± 3,5 Ω vacío y 20 ± 2,5 Ω lleno** [fuente: boletín de Ducati en NHTSA].
  - Yamaha XS11 (foro): 6-10 Ω lleno, 28,5-36,5 Ω a la mitad y 75-85 Ω vacío [fuente de foro, no oficial].
  - Patrón japonés e indio frecuente: **~5-10 Ω lleno y ~90-110 Ω vacío** [inferencia; confirmar].
  - Estándares de carro para comparar: VDO europeo 10-180 Ω; GM y EE. UU. 240-33 Ω [inferencia].
- Medición: (a) sin el conector, ohmímetro entre los pines del flotador con tanque lleno y en reserva, sin sacar el tanque (Ducati advierte no soltar el flotador); (b) en circuito: el tablero alimenta el flotador por una resistencia de pull-up a 5 V o 12 V y lee la tensión con el ADC. Medir tensión contra nivel de 0 a 100 % (lleno → litros conocidos → tabla).
- **El tablero filtra mucho** (decenas de segundos) y cuantiza en barras con histéresis. El tablero de reemplazo tiene que hacer lo mismo o la aguja baila con las vibraciones y en curvas.
- Variante: algunos japoneses usan un **termistor** solo para el testigo de reserva [inferencia].
- En TVS, la telemetría BLE `5A 10` byte 6 da las barras que el tablero calcula, lo que sirve para calibrar ohmios contra barras [fuente del layout; fiabilidad abierta].

### 5.2 Velocidad
- Tres arquitecturas [inferencia general]:
  1. **Hall de 3 hilos** (V+ de 5 o 12 V, masa, señal de colector abierto con pull-up en el tablero) sobre un rotor en el **piñón de salida, la caja o la rueda**.
  2. **Sensor ABS activo de 2 hilos** (corriente de ~7/14 mA) leído por la unidad ABS sobre una rueda fónica de **decenas de dientes**. **El tablero no lo lee**: el ABS manda la velocidad por **CAN** o por una salida de pulsos acondicionada. Con ABS de un canal (solo delantera), lo más probable es que la velocidad del tablero salga de la rueda delantera vía ABS, o de un Hall en la caja [hipótesis a confirmar en la Apache].
  3. Inductivo o reed en motos viejas.
- Factor: `pulsos/km = (1000 / circunferencia_m) × pulsos_por_vuelta_de_rueda`. Si el sensor está en el piñón, multiplicar por la relación final `corona/piñón` [fuente: datasheet de un kit de velocímetro].
- Medir: moto en el soporte, marcar la rueda, girar N vueltas a mano y contar flancos en PulseView; o rodar en marcha y comparar con la velocidad BLE del tablero o con el GPS.

### 5.3 Neutro y marcha
- **Neutro:** interruptor en la caja **a masa** (lado bajo). El testigo del tablero va entre +12 V y ese cable, que se cierra solo en N. A menudo también entra a la ECU (bloqueo de arranque con la pata lateral) [inferencia general; en Suzuki el cable azul de neutro "a masa enciende el testigo", fuente de foro].
- **Marcha:** dos variantes. (a) **Sensor de posición resistivo o multicontacto** en el tambor de cambios, con una tensión por marcha (alimentado por la ECU; ejemplo de foro: ZX6R ~3,0 V en 1.ª y 2,5 V en 2.ª; sin alimentación mide circuito abierto). (b) **Calculada** por la ECU o el tablero a partir de RPM y velocidad.
- **Prueba rápida [inferencia]:** detenido, con el embrague apretado, meter 1.ª, 2.ª... Si el indicador cambia **parado**, hay un sensor físico; si solo aparece en movimiento, es calculada (y viaja por CAN o la calcula el tablero).

### 5.4 Testigos [inferencia general, confirmar con el analizador]

| Testigo | Lado típico | Notas |
|---|---|---|
| Direccionales | **Alto**: 12 V pulsante desde el relé | ~1-2 Hz (la normativa típica es de 90 ± 30 destellos/min); a veces un solo testigo común para ambos lados |
| Luz alta | Alto: 12 V | |
| Neutro | **Bajo**: el interruptor da masa | |
| Presión o temperatura de aceite | Bajo: el interruptor cierra a masa | En motos refrigeradas por aceite como la Apache, puede no existir o venir por CAN |
| MIL (check engine) | Salida de lado bajo de la ECU **o por CAN** | Se enciende unos segundos con el contacto (prueba de bombillas) |
| ABS | Lado bajo de la unidad ABS **o por CAN** | **Queda encendido hasta superar ~5-7 km/h** (autodiagnóstico). Esa transición es una firma excelente para encontrar el mensaje del ABS en el CAN |
| Pata lateral | Interruptor hacia la ECU | |
| Batería | El tablero mide su propia alimentación | |
| RPM | Por CAN desde la ECU, o un pulso de tacómetro (en un monocilíndrico con chispa perdida: ~1 pulso por vuelta) | Nunca en el primario de la bobina sin aislamiento |

---

## 6. Herramientas MCP para RevScope (para que un agente haga la ingeniería inversa)

**Base ya diseñada** en `docs/superpowers/specs/2026-11-01-modo-escucha-can-design.md`: `escuchar_can`, `listar_capturas_can`, `leer_captura_can` y `diferencia_capturas_can`, con candump más JSON, CSM1, restauración en `finally`, bloqueo durante un viaje y un tope de 200 capturas. Lo que sigue es lo que falta, ordenado por cuánto acelera al agente y cuánto hace **repetible** el proceso entre motos.

### P0: sin esto el agente adivina

1. **`iniciar_escucha_can` / `marcar_evento_can` / `detener_escucha_can` (captura asíncrona con marcadores).**
   La herramienta síncrona `escuchar_can(segundos)` obliga a dos capturas separadas (base y acción), con distinto estado de fondo. Con una captura larga y marcadores (`{t, etiqueta: "direccional_izq_on"}`), el agente le dice al usuario "ahora sí" y marca, y el análisis se hace por segmentos de **la misma** captura. Reutiliza el patrón `iniciar_captura`/`detener_captura` que ya existe en `MuestreoTools`. Los marcadores van al JSON, nunca al `.log`.

2. **`prueba_guiada_can(señal, patron)`: excitación ON/OFF scriptada.**
   La app le dice al usuario por voz o pantalla "direccional izquierda: ON 5 s, OFF 5 s, ×3" y **pone los marcadores sola**. Esto convierte el proceso en un **protocolo idéntico para cada moto**, que es lo que lo hace replicable. Reutiliza la infraestructura de `PruebaGuiadaTools`. Salida: los bits que siguen la onda (correlación con la cuadrada y transiciones con su retardo), con el puntaje.

3. **`mapa_bits_can(captura, id?, segmento?)`: heatmap y clasificación.**
   Por ID y bit: tasa de cambio. Por byte: mín, máx, valores distintos y entropía. Clasificación automática: `constante`, `contador(k bits)`, `checksum_probable`, `flag`, `señal_continua_candidata`, `límites_READ`. Es lo primero que mira un ingeniero en SavvyCAN o cabana, y en JSON un modelo de IA lo razona mejor que en una tabla de 64 celdas.

4. **`detectar_checksum_can(captura, id)`.**
   Prueba XOR, suma, complemento y CRC-8 (`0x1D`, `0x2F`, `0x07`; init y xorout `00`/`FF`; con o sin ID y contador) sobre todas las tramas y devuelve el algoritmo que acierta el 100 %. Sirve para excluir ruido del diff y, más adelante, para que el firmware del banco genere tramas válidas.

5. **`correlacionar_can(captura, referencia, opciones)`.**
   Búsqueda exhaustiva de ID, bit de inicio, largo de 1 a 16, LE/BE y signo contra una serie de referencia, con Spearman y desfase de ±2 s. Devuelve los N mejores con el ajuste (escala redondeada a valores de fabricante, después el offset) y el "prior" de plausibilidad. Es el cuello de botella manual que CSS y cabana automatizan; en el teléfono corre en segundos para buses de decenas de IDs. Referencias admitidas: (a) PIDs OBD registrados en la misma sesión, (b) **telemetría BLE del tablero TVS**, (c) GPS del teléfono, (d) valores dictados por el usuario con su marca de tiempo.

6. **`verificar_senal_can(definicion, captura, referencia)`.**
   Decodifica con la definición candidata **en otra captura** y devuelve el error contra la referencia, separado parado y en movimiento, más una prueba de suavidad (sin saltos de vuelta). Veredicto `CONFIRMADA` o `SIN_CONFIRMAR`. Es la compuerta de calidad de CSS: impide que el agente "racionalice" un candidato malo.

### P1: entregables y escala

7. **`guardar_senal_dbc(modelo, senal)` / `exportar_dbc(modelo)` / `leer_dbc(modelo)`.**
   Un DBC por modelo, que vive en el teléfono y se exporta, con el estado de cada señal (`confirmada`, `candidata`) en comentarios `CM_`. Es lo que leen SavvyCAN, cantools (`generate_c_source` para el ESP32 del tablero) y el siguiente técnico.

8. **`decodificar_captura_can(captura, modelo)`.**
   Aplica el DBC y devuelve series de tiempo (paginadas y resumidas). Sirve para verificar, para depurar el firmware del tablero y para que el agente "vea" la moto con nombres en vez de hex.

9. **`exportar_captura_can(id, formato)`** (`candump`, `savvycan_csv`, `asc`) y **un enlace HTTP de descarga** desde el servidor MCP. Así se abre en SavvyCAN o cabana en el PC sin cable.

10. **`buscar_valor_can(captura, valor, ventana, comparador, factores?)`.**
    El "Find Signal" de cabana: el usuario lee el odómetro (por ejemplo, 1234,5 km), el trip o el reloj del tablero, y el agente busca los campos que valen eso con factores típicos (×1, ×10, ×100). Encuentra **odómetro, trip y reloj** sin necesidad de rodar.

11. **`estado_adaptador_can()`.**
    `AT I`, `AT @1`, `STI`, `AT CSM1?`, `AT CS` (contadores de error y frecuencia) y una **prueba de capacidad** (líneas/s y `BUFFER FULL` en 5 s). El agente decide con datos si filtra, barre por máscaras o pide el ESP32.

12. **`escuchar_ble_tablero_tvs(segundos)`** (o el genérico `capturar_referencia(fuente: ble_tvs|gps|obd)`).
    Captura la telemetría `5A 10`/`5A 18` como referencia en el mismo eje de tiempo que la captura CAN. Solo TVS, pero cubre Apache, Raider, Ntorq y Jupiter: buena parte del parque indio.

### P2: proceso y replicabilidad

13. **`proyecto_ingenieria(modelo)`**: una lista de chequeo por moto con las señales objetivo (velocidad, RPM, marcha, neutro, combustible, temperatura, direccionales, luz alta, ABS, MIL, modo, botones), el estado de cada una, la captura que la prueba y el pinout. **`informe_ingenieria(modelo)`** genera el Markdown o PDF de entrega. Esto es lo que hace repetible el proceso.
14. **`barrido_filtros_can(etiqueta, bloques)`**: captura automática por bloques de máscara cuando el ELM no alcanza.
15. **Fuente ESP32**: que las mismas herramientas acepten un `origen: "esp32"` (BLE o USB-OTG con candump o GVRET), de modo que el agente use el mismo flujo con el sniffer bueno.
16. **`diferencia_capturas_can` mejorada**: aceptar segmentos (`captura:marcador_a..marcador_b`) además de capturas enteras, y devolver también "IDs cuya frecuencia cambió" (mensajes de evento).

Reglas para todas: **solo escucha** (nada de TX al bus de la moto desde el MCP), errores en el estilo de `ComandoLecturaTool`, respuestas paginadas y resumidas (el agente no tiene que tragarse 50 000 líneas) y permiso de LECTURA.

---

## 7. Proceso repetible por moto (resumen operativo)

1. **Identificar el bus:** multímetro en CAN-H/CAN-L (~2,5 V en reposo, 60 Ω con terminación en ambos extremos o 120 Ω si falta una, por ejemplo sin el tablero). Con el Vgate: `ATSP0`, `0100`, `ATDPN`. Si no hay CAN, analizador lógico buscando K-line o UART (10400 o 9600 baudios).
2. **Bitrate:** ESP32 en solo escucha probando 500, 250 y 1000 k (sin errores = correcto), o sigrok sobre el RXD del transceptor.
3. **Inventario** con contacto y en ralentí (sección 2.8).
4. **Discretas** con `prueba_guiada_can`. **Continuas** con un viaje y referencia (BLE del tablero, PID por ESP32+ELM, GPS).
5. **Analógicas:** pinout del conector del tablero con el analizador lógico y el multímetro; tabla de combustible ohmios→barras; pulsos/km.
6. **DBC + pinout + calibraciones** en `revdash/docs/fase0/<modelo>.*` e informe.
7. **Banco:** el ESP32 reproduce un log hacia el tablero original desconectado de la moto, para confirmar el decodificado de punta a punta.
8. Recordatorio legal: el tablero de reemplazo debe conservar el **odómetro real**. Manipularlo es delito en muchos países.

---

## Fuentes

Formatos y herramientas
- can-utils, `lib.h` (gramática de tramas ASCII): https://github.com/linux-can/can-utils/blob/master/lib.h
- can-utils, `candump.c` (formato de log y marcas de tiempo): https://github.com/linux-can/can-utils/blob/master/candump.c
- can-utils, `canplayer.c` (salta líneas que no empiezan con `(`): https://github.com/linux-can/can-utils/blob/master/canplayer.c
- Man page de candump: https://dyn.manpages.debian.org/bullseye/can-utils/candump.1
- python-can, E/S de archivos: https://python-can.readthedocs.io/en/stable/file_io.html
- python-can, lector de candump: https://github.com/hardbyte/python-can/blob/main/can/io/canutils.py
- cantools: https://cantools.readthedocs.io/en/latest/ · https://github.com/cantools/cantools
- SavvyCAN: https://github.com/collin80/SavvyCAN · formatos: https://github.com/collin80/SavvyCAN/blob/master/framefileio.cpp · ayuda: https://github.com/collin80/SavvyCAN/tree/master/help
- ESP32RET: https://github.com/collin80/ESP32RET
- Cabana: https://github.com/commaai/openpilot/tree/master/openpilot/tools/cabana (`ui/tools/findsignal.cc`, `ui/tools/findsimilarbits.cc`, `core/heatmap.h`)
- comma.ai, panda y cabana: https://blog.comma.ai/a-panda-and-a-cabana-how-to-get-started-car-hacking-with-comma-ai/
- opendbc: https://github.com/commaai/opendbc
- CANdevStudio: https://github.com/GENIVI/CANdevStudio
- cangaroo: https://github.com/HubertD/cangaroo
- Kayak (archivado): https://github.com/dschanoeh/Kayak
- CANalyzat0r: https://github.com/schutzwerk/CANalyzat0r
- awesome-canbus: https://github.com/iDoka/awesome-canbus
- Wireshark, lector de candump (2019): https://lists.wireshark.org/archives/wireshark-commits/201907/msg00107.html
- delsum: https://github.com/8051Enthusiast/delsum · crcbeagle: https://github.com/colinoflynn/crcbeagle

Técnicas y papers
- CSS Electronics, skills de Claude Code: https://github.com/CSS-Electronics/can-bus-reverse-engineering-skills (`.claude/skills/cansub-reverse-engineering/references/re-methodology.md` y `signal-encoding.md`)
- READ (Marchetti y Stabili): https://iris.unimore.it/handle/11380/1185929 · PDF: https://iris.unimore.it/bitstream/11380/1185929/1/08466914_mio.pdf
- LibreCAN (Pesé et al., CCS 2019): https://rtcl.eecs.umich.edu/rtclweb/assets/publications/2019/fv075-peseA.pdf
- CAN-D (Verma et al.): https://arxiv.org/abs/2006.05993
- CANMatch (Buscemi et al.): https://orbilu.uni.lu/handle/10993/48502
- Buscemi et al., enfoque mínimo basado en datos: https://vehicularlab.uni.lu/hugotest/publication/buscemi2020data/buscemi2020data.pdf
- ByCAN (2024): https://arxiv.org/abs/2408.09265
- Brent Stone, pipeline automático: https://github.com/brent-stone/CAN_Reverse_Engineering
- SAE 2026-01-0070, "The Simple Complexity of Reverse Engineering a Vehicle CAN Bus": https://saemobilus.sae.org/papers/simple-complexity-reverse-engineering-a-vehicle-bus-2026-01-0070

ELM327, STN y Vgate
- Datasheet de ELM327 (comandos, búfer de 512 B, tiempos de envío): https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327DS.pdf
- Driver `can327` del kernel de Linux: https://docs.kernel.org/networking/device_drivers/can/can327.html
- ELM327 en Wikipedia (versiones, clones, cierre): https://en.wikipedia.org/wiki/ELM327
- OBDLink FRPM (STM, STMA, filtros, búferes, STBR): https://www.scantool.net/scantool/downloads/682/obdlink_frpm_f.pdf
- STN2100: https://www.obdsol.com/solutions/chips/stn2100/
- Review del Vgate iCar Pro 2S: https://iamcarhacker.com/vgate-icar-pro-2s-review

Motos
- KTM: https://github.com/blalor/ktm-can · https://www.advrider.com/f/threads/results-from-hacking-the-ktm-superduke-1290-can-bus.1200087/ · https://github.com/lab-vex/OBDuke
- Ducati: https://github.com/renatobo/DucatiPanigaleCanBus · https://docs.google.com/spreadsheets/d/1-NJ9OlGQYTGMzBzwDPYn-aI_7_ign9SCiscKZufx3Uw/edit · https://pulsesecurity.co.nz/articles/ducati-696-part2
- BMW: https://github.com/4G-Gregg/BMW-GS-CAN-Sniffer · https://docs.google.com/spreadsheets/d/1tUrOES5fQZa92Robr6uP8v2dzQDq9ohHjUiTU3isqdc/edit
- Índice de IDs: https://github.com/iDoka/awesome-automotive-can-id
- ROOTCON 14, "2 wires and 2 wheels: Bikes can do CAN too": https://media.rootcon.org/ROOTCON%2014%20(Recovery%20Mode)/Talks/2%20wires%20and%202%20wheels-%20Bikes%20can%20do%20CAN%20too.pdf
- DEF CON 28 Car Hacking Village, "All Aboard the CAN Bus or Motorcycle" (transcripción): https://media.defcon.org/DEF%20CON%2028/DEF%20CON%20Safe%20Mode%20villages/DEF%20CON%20Safe%20Mode%20-%20villages%20captions/DEF%20CON%20Safe%20Mode%20-%20Car%20Hacking%20Village%20-%20All%20Aboard%20the%20CAN%20Bus%20or%20Motorcycle.eng.text

TVS
- TVSRideCompanion (protocolo BLE SmartXonnect): https://github.com/athulkrishna2015/TVSRideCompanion · https://github.com/athulkrishna2015/TVSRideCompanion/blob/main/docs/PROTOCOL.md
- RR 310 Nav Bridge: https://github.com/72141299/TVS-APACHE-RR310-MODIFIED-APP
- SmartXonnect+ (keyless): https://github.com/sirajju/tvs-rea
- TechEnclave, alternativa a TVS Connect: https://techenclave.com/t/making-alternative-to-tvs-connect-app-need-some-help/423446
- NVD, CVE-2024-35537: https://nvd.nist.gov/vuln/detail/CVE-2024-35537
- Manual del propietario de la Apache RTR 160 4V: https://www.tvsmotor.com/ss/-/media/Feature/IB/Documents/Common/Owner-Manual/OM_Apache-RTR-160-4V.pdf
- ABS de un canal en la RTR 160 4V: https://www.outlookindia.com/automobiles/automobiles-tvs-apache-rtr-160-4v-fi-gets-single-channel-abs-news-325565
- TVS y OBD-2B: https://www.mobilityoutlook.com/news/tvs-motor-rolls-out-obd-2b-compliant-vehicles-starting-with-tvs-jupiter-110/ · https://www.tvsmotor.com/Media/Blog/bs6-phase-2-rde-and-obd-2-compliance-explained
- Bosch ABS 10 de un canal: https://newatlas.com/bosch-motorcycle-division-abs10/41362/

Señales analógicas
- Boletín de Ducati en NHTSA (flotador Bosch de 320/20 Ω): https://static.nhtsa.gov/odi/tsbs/2020/MC-10183581-0001.pdf
- Foro XS11 (resistencias de flotador Yamaha): https://xs11.club/forum/idle-talk-forum/xs11-xj11-discussion/45019-about-fuel-sensor-resistance
- Foro MoTeC (cableado del sensor de marcha de Suzuki): https://forum.motec.com.au/viewtopic.php?p=9813
- Fórmula de pulsos/km (datasheet de un kit de velocímetro): https://bikers-store.fr/data/fichesTechniques/docs/index1%2Epdf

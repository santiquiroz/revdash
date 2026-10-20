# RevDash — tablero abierto para moto con Android Auto

Fecha: 2026-10-19 · Estado: borrador para aprobación · Repo: `github.com/santiquiroz/revdash` (por crear)

## 1. Intención y por qué

El tablero (LCD) de la TVS Apache RTR 160 4V del dueño murió tras una instalación de exploradoras con la
farola destapada y un viaje largo bajo lluvia fuerte: primero encendía solo al girar el manubrio a fondo a
la izquierda (falso contacto en el mazo que pasa por la dirección), hoy ya no enciende. Tras buscar el fallo
sin éxito, el dueño decidió no repararlo y **reemplazarlo por un tablero propio, abierto y programable**.

RevDash es ese tablero: una pantalla de moto dedicada (estilo "CarPlay de moto", alimentada por la batería
de la moto) que arranca mostrando todo lo que mostraba el tablero de fábrica y además ofrece Android Auto
del teléfono del dueño (Samsung S25 Ultra). Es **hermano de RevScope** (`github.com/santiquiroz/revscope`,
app Android de telemetría OBD2 del mismo dueño, cuya base de pruebas fue esta misma moto): comparten
piezas y se complementan, pero cada uno funciona solo.

Por qué abierto: el dueño quiere durabilidad, actualizaciones, personalización por programación y no
depender de una caja cerrada; todo se publica como código libre con documentación detallada.

## 2. La moto

- TVS Apache RTR 160 4V XC FI Racing, modelo 2026 (Auteco, Colombia), inyección, ABS solo delantero,
  modos de manejo Urban/Sport/Rain, SmartXonnect (Bluetooth del tablero).
- Tablero original: LCD monocromo. Muestra (foto del dueño): velocidad, barra de RPM (0-12k), marcha y N,
  modo, velocidad promedio, odómetro y trip, reloj, combustible, check engine, y testigos de direccionales,
  luz alta y ABS.
- **Los modos de manejo funcionan con el tablero apagado** (verificado por el dueño): el botón de modo no
  depende del tablero.
- Tiene puerto OBD estándar: RevScope ya lee por él con un Vgate iCar Pro (ELM327 v2.3): 16 PIDs base + 8
  extendidos, ~20 lecturas/s. El DID propietario del modo de manejo no se ha encontrado.
- No existe pinout ni protocolo publicado del conector del tablero (investigación 2026-10-19).
- Intercomunicador del casco: XGP X10 (Bluetooth), usado también con otros motociclistas.

## 3. Objetivos

1. Reemplazar por completo el tablero de fábrica con una pantalla propia que muestre **toda** su
   información, legible a pleno sol, que aguante lluvia fuerte y calor.
2. Al girar la llave, la pantalla arranca **directo en el tablero**, sin celular.
3. Android Auto inalámbrico del S25 (navegación, música, llamadas) en la misma pantalla.
4. Con el celular presente, RevDash y RevScope **comparten datos y se complementan**.
5. Código, diseño de hardware, carcasa e ingeniería inversa publicados como código libre, con
   documentación detallada (incluido el primer pinout/protocolo público del tablero de la Apache).
6. Que no quede obsoleto rápido: el cerebro y el sistema se pueden actualizar.

## 4. No-objetivos (por ahora)

- **No usar el puerto OBD.** Queda libre para RevScope y para diagnóstico de taller. RevDash saca todo del
  conector del tablero, como el original.
- Botones físicos en el manubrio (el dueño eligió solo táctil por ahora; posible mejora futura).
- Apple CarPlay: requiere chip de certificación de Apple, incompatible con un proyecto libre; el teléfono
  del dueño es Android, así que se usa Android Auto.
- Reemplazar SmartXonnect (el módulo está en el tablero muerto).
- Android más nuevo que 11 en la primera versión (ver §9, subproyecto opcional).

## 5. Anti-objetivos (fallaría aunque "funcione")

- Consumo fantasma: con la llave en OFF el consumo debe ser **cero** (sin reposo, sin sleep).
- Escribir en el bus o en los cables de la moto: RevDash **solo escucha**; nunca transmite a la ECU/ABS.
- Una tableta con batería interna de litio sobre la moto (el dueño la descartó).
- Un tablero que se apague, reinicie o corrompa su almacenamiento por los picos/caídas de voltaje de la moto
  o por cortar la llave.
- Que el audio de Android Auto termine en la pantalla en vez del intercomunicador.
- Un tablero que dependa del celular para mostrar velocidad y testigos.

## 6. Restricciones

- Clima de Colombia: sol fuerte (el sol sube la superficie de la pantalla 40-50 °C sobre el ambiente) y
  lluvia intensa; humedad (fue lo que mató el tablero original).
- Uso con guantes de moto y bajo lluvia.
- Alimentación desde 12 V de la moto, conmutada por la llave.
- Tiempo de arranque: **no es problema** para el dueño (se acepta Android tardando decenas de segundos, y
  por tanto arrancar a rodar sin velocidad ni testigos durante ese lapso). No se agrega un microcontrolador
  de arranque instantáneo.
- Intervención en la moto: el dueño ya decidió reemplazar el tablero. Aun así, **mi decisión**: nada se corta
  en el mazo original; la Fase 0 mide por detrás del conector (back-probing) y la conexión final usa un
  arnés adaptador con el conector correspondiente, para poder volver al estado de fábrica.
- Tamaño: puede ser un poco más grande que el tablero original; montaje sobre la base original en la fase final.
- Presupuesto orientativo de hardware principal: unos US$250-320 (pantalla + placa + fuente), más envío a
  Colombia. Compra prevista: la placa el próximo mes.

## 7. Decisiones tomadas por el dueño

| Tema | Decisión |
|---|---|
| Forma | Pantalla dedicada de moto ("CarPlay"), no tableta; alimentada por la batería de la moto |
| Fases | Fase 1: pantalla aparte (montaje provisional); Fase 2: reemplazo total en la montura original |
| Cerebro | **Radxa Zero 3W** (RK3566, Wi-Fi 6 + BT 5.4 en la variante AIC8800; NO la 3E, que no tiene Wi-Fi) |
| Sistema | **Android** (Android 11 de Radxa para empezar; imagen propia después) |
| Pantalla | **Riverdi RVT70HSHNWC00-B**: 7" 1024×600, 850 nits, optical bonding, táctil PCAP UxTouch (doble guante y agua), HDMI + táctil por USB-C, entrada 7-30 V, -20..70 °C |
| Entrada | Solo táctil por ahora |
| ESP32 | Se usa el ESP32 que el dueño ya tiene para la ingeniería inversa. **El diseño final no lleva ESP32**: la Zero lee las señales con sus propios pines (a través de una placa de interfaz) |
| OBD | No se usa (§4) |
| Audio | El audio de Android Auto no debe ir a la pantalla; debe seguir yendo del celular al XGP X10 |
| Distribución | Combinación B + C (§10) |
| Repo | Repo hermano de RevScope llamado **RevDash**, en el GitHub personal del dueño |
| Obsolescencia | Preferencia por que no quede obsoleto; Android más nuevo como mejora futura |
| Montaje | **Fijo** en la moto (no se retira al parquear); queda al sol y a la intemperie estacionada |
| Testigo ABS | El tablero original muestra el piloto de ABS: es **imprescindible** replicarlo |
| Herramientas | Comprar un analizador lógico USB de 24 MHz y 8 canales (unos US$10-13) para la Fase 0 |
| Compilación | La imagen de Android se compila en el PC de escritorio del dueño (le sobran RAM y disco) |

## 8. Arquitectura

```
 Moto (conector del tablero)          RevDash (en la moto)                        Celular (S25)
 ─────────────────────────           ──────────────────────────────               ─────────────────
 cables discretos 12 V  ──►  Placa de interfaz  ──►  Pines Radxa Zero 3W           RevScope
 bus de datos (si hay) ──►  (aislamiento,           (servicio de sistema)  ◄──►   (OBD, GPS, pulso,
                             transceptor RX)            │                          taller)
 12 V conmutado por llave ─► Fuente + retención ────► Zero + Riverdi              Android Auto
                             de apagado                 │  App RevDash (tablero)  ◄──►  (Wi-Fi + BT)
                                                        │  open-headunit (AA)
                                                        └─ HDMI ► Riverdi ◄─ USB táctil
```

### 8.1 Hardware

- **Radxa Zero 3W** (2-4 GB RAM es suficiente; la cantidad exacta la decide el constructor).
- **Riverdi RVT70HSHNWC00-B** por micro-HDMI → HDMI y táctil por USB.
- **Fuente**: 12 V de la línea de la llave → protección (fusible, TVS para transitorios tipo ISO 7637-2 /
  load dump, protección contra inversión) → buck a 5 V para la Zero; la Riverdi acepta 7-30 V (puede ir por
  su propia rama protegida). Debe soportar la caída de voltaje al arrancar el motor sin reiniciar
  (buck-boost o condensador de respaldo). La falla número uno de las pantallas comerciales de moto es la
  alimentación.
- **Retención de apagado (mi decisión, ver §13)**: al cortar la llave, un supercondensador o
  circuito de retención mantiene viva la Zero unos segundos para que Android se apague limpio (evita
  corromper el almacenamiento y perder odómetro/trip). La Zero detecta "llave OFF" por un pin, guarda y se
  apaga; después el consumo cae a cero. No es reposo: es un apagado completo con energía prestada.
  Duración: se mide en la Fase 1 cuánto tarda Android 11 en apagarse en la Zero y se dimensiona con margen
  (orden esperado 10-30 s, sin verificar). Además, el odómetro y el trip se guardan de forma transaccional
  cada pocos segundos, para que un corte brusco pierda como máximo ese intervalo aunque la retención falle.
- **Placa de interfaz moto → Zero** (diseño final): optoacopladores o divisores con protección para cada
  señal de 12 V, contador por hardware o captura para pulsos (velocidad/RPM si llegan como pulsos) y
  transceptor en modo solo recepción si el conector lleva un bus (CAN o K-line). Su diseño exacto depende
  de la Fase 0.
- **Carcasa**: impresa en 3D en ASA (resistente a UV/calor) de color claro, sellada contra lluvia, con
  visera; montaje provisional en Fase 1 y sobre la base original en Fase 2. Sensor de temperatura (NTC)
  dentro de la carcasa.

### 8.2 Software en la Zero

- **Android 11** (imagen de Radxa en Fase 1; imagen propia en Fase 2).
- **App RevDash** (Kotlin + Compose, igual que RevScope): es el **launcher** (pantalla de inicio fija,
  modo kiosco). Dibuja el tablero y gestiona los estados de §10.
- **open-headunit** (AGPL, `github.com/andreknieriem/open-headunit`, activo, v3.5.0 2026-10-17) para
  recibir Android Auto inalámbrico; se controla por Intents desde la app RevDash. Debe configurarse para no
  declarar canales de audio de medios y voz (ver §12, riesgo 1).
- **Servicio de señales**: lee los pines/bus y publica los datos a la app. En Fase 1 (imagen de Radxa) la
  fuente provisional es el ESP32 por USB (§11); en Fase 2 es un servicio de sistema en la imagen propia
  con permisos sobre GPIO/UART (Android no deja a las apps abrir esos dispositivos sin root/permisos de
  sistema; con imagen propia se resuelve limpio).
- **Gestión térmica**: con el NTC, bajar el brillo de la luz de fondo por encima de ~60 °C y apagarla
  (dejando el sistema vivo) cerca de 65-70 °C, que es el límite de la Riverdi.
- **Odómetro y trip propios**: el tablero original ya no cuenta; RevDash calcula distancia a partir de la
  velocidad, la persiste de forma segura ante cortes y permite fijar el valor inicial (unos 10 780 km según
  el dueño el 2026-10-19; el valor exacto lo confirmará con una app que muestra el odómetro).

### 8.3 Imagen propia de Android (Fase 2)

Base: rama `Android11_Radxa_rk12` / `rockchip-r-release.xml` de `radxa/manifests` (hay reportes de errores
de compilación con clang nuevo: hay que corregir). Requiere ~16 GB RAM y ~250 GB de disco (WSL2/Docker en el
PC del dueño). Contenido: RevDash como launcher, open-headunit preinstalado, servicio de señales con
permisos, sin apps sobrantes, arranque recortado, actualizaciones OTA por Wi-Fi. Todo reproducible y
documentado en el repo.

## 9. Fases

### Fase 0 — Ingeniería inversa del conector del tablero (ya, con el ESP32 del dueño)

1. Identificar cada pin del conector del tablero: alimentaciones, tierras, y qué señal lleva cada cable
   (direccional izq/der, neutro, luz alta, check engine, ABS, combustible, velocidad, RPM, modo, botones),
   y si hay un bus de datos (CAN o K-line) y a qué velocidad.
2. Herramientas: multímetro; analizador lógico USB 24 MHz / 8 canales compatible con PulseView/sigrok
   (primera exploración: ver formas de onda y decodificar CAN/UART/K-line); ESP32 con protecciones de
   entrada (grabación larga rodando y transmisión de señales ya entendidas). Ninguno de los dos aguanta
   12 V directo: divisores o sondas con protección en cada canal. Con el tablero muerto, las señales siguen
   llegando al conector.
3. Grabar señales rodando y compararlas con RevScope por OBD (velocidad, RPM, temperatura, check engine)
   como referencia. Ojo: el OBD se usa **solo como referencia durante la validación**, no en RevDash.
4. Entregable: documento público de pinout y protocolo en `docs/` del repo, con fotos y capturas; firmware
   del ESP32 que lee y transmite las señales decodificadas.
5. Criterio de salida (**mi decisión**, con el ABS por pedido del dueño): imprescindibles = velocidad,
   direccionales, neutro, luz alta, check engine, **ABS** y combustible. RPM, modo de manejo y botones son
   deseables: si alguno no aparece, la Fase 1
   avanza sin él y la pantalla lo muestra como "no disponible" hasta encontrarlo (por ejemplo, el modo puede
   venir después por otra vía).

### Fase 1 — Pantalla aparte

- Zero 3W + Riverdi + fuente + carcasa provisional, montadas junto a la base del tablero.
- Primeras pruebas, antes de construir lo demás: Android 11 de Radxa arranca, video por hardware, Wi-Fi y
  BT del AIC8800 funcionan, Android Auto inalámbrico con el S25, y que el audio llegue al X10 (§12).
- App RevDash con los tres estados de §10, alimentada por el ESP32 por USB.
- Validación: velocidad contra GPS y contra RevScope/OBD; testigos contra lo que se ve en la moto.

### Fase 2 — Reemplazo total

- Imagen propia de Android con el servicio de señales; placa de interfaz que reemplaza al ESP32.
- Carcasa definitiva sobre la montura original del tablero.
- Actualizaciones OTA.

### Subproyecto opcional — Android más nuevo

Portar el kit Android 13/14 de Rockchip para RK356x a la Zero 3W (existe para RK3568 en FriendlyElec y hay
productos RK3566 con Android 13). Obstáculo principal: no hay driver de Android público para el Wi-Fi AIC8800.
Alternativa: Android mainline (GloDroid). Solo después de la Fase 2.

## 10. Pantalla: estados y distribución (aprobado con maquetas)

1. **Tablero completo (B)**: al encender o sin celular. Izquierda: direccionales y testigos arriba,
   velocidad enorme, barra de RPM. Derecha: tarjetas que se cambian con un toque (marcha+modo,
   combustible/autonomía, viaje+reloj, y datos que lleguen de RevScope).
2. **Tablero fijo + Android Auto (C)**: columna izquierda (~28 %) con velocidad, direccionales, marcha,
   modo, testigos, RPM, combustible y reloj, siempre visible; el resto es Android Auto con su vista
   dividida normal (mapa + música/llamada).
3. **Android Auto casi completo**: franja superior delgada con velocidad, direccionales, marcha, modo y
   testigos; Android Auto debajo. Es el respaldo si Android Auto no muestra su vista dividida en el espacio
   del estado 2 (~740×600), y una opción para el dueño.

Paso entre estados con un toque; al conectar/desconectar el celular, cambio automático (1 ↔ 2). Las
direccionales y los testigos críticos (check engine, ABS, aceite/temperatura si existen) son visibles en
todos los estados. Maquetas: `.superpowers/brainstorm/` (layout-v2.html).

## 11. Relación con RevScope

- **Repos hermanos**, objetivos distintos: RevScope (celular) = telemetría, taller y viajes por OBD;
  RevDash (moto) = el tablero.
- **Comparten**: modelo de datos de telemetría, protocolo de comunicación pantalla↔celular, sistema de
  diseño (colores, medidores, tipografía) y lógica de alertas. Las piezas compartidas se quedan en
  Apache-2.0 (la licencia de RevScope).
- **Se complementan**: RevDash envía a RevScope las señales del tablero (direccionales, combustible, modo,
  odómetro); RevScope envía a RevDash lo que solo él tiene (datos OBD como ajustes de mezcla y códigos de
  falla, GPS, pulso del Galaxy Watch). Sin celular, RevDash funciona completo con lo suyo.
- **Transporte (decisión del dueño)**: **BLE**. RevDash expone un servidor GATT y RevScope se conecta como
  cliente, igual que hoy se conecta a adaptadores OBD BLE. Funciona con o sin Android Auto y no toca el
  Wi-Fi del celular (el dueño no quiere ningún riesgo sobre sus datos móviles). El Wi-Fi de la Zero queda
  solo para Android Auto. Sincronización pesada por Wi-Fi (historial de viajes, actualizaciones): mejora
  futura, fuera de alcance.
- Fase 1 provisional: el ESP32 conectado a la Zero por USB (puerto serie) como fuente de señales.

## 12. Riesgos y cómo se prueban primero

0. **Topología Bluetooth/Wi-Fi (mi decisión)**: llamadas, música y voces de navegación van siempre al
   casco. El S25 queda emparejado al X10 por Bluetooth como hoy; la Zero hace de punto de acceso Wi-Fi para
   Android Auto (como un carro) y el teléfono mantiene sus datos móviles. Verificar en Fase 1 que One UI no
   corte los datos ni Android Auto al unirse a una red sin internet.
1. **Audio al intercomunicador.** Con Android Auto inalámbrico, el teléfono manda el audio a la cabeza.
   Plan: que la cabeza no declare canales de audio de medios/voz para que el teléfono lo conserve y lo envíe
   al X10. Un proyecto similar (Hudiy) permite desactivarlos, pero un usuario reportó que su teléfono siguió
   eligiendo la cabeza. Primera prueba de la Fase 1. Respaldo: la Zero reenvía el audio al X10 por Bluetooth
   (A2DP); las llamadas por ese camino son más complejas.
2. **Wi-Fi/BT del AIC8800 en Android 11.** Sin Wi-Fi no hay Android Auto inalámbrico. Se prueba con la
   imagen de Radxa antes de construir nada más. Respaldo: Orange Pi 3B (RK3566, Android 11 oficial, Wi-Fi 5 +
   BT 5) o Android Auto por cable.
3. **Calor.** La Riverdi opera hasta 70 °C; su almacenamiento (apagada) probablemente llega a 80 °C (dato de
   un modelo hermano, sin verificar en su ficha). El dueño la quiere **fija**, así que estará al sol
   parqueada. Mitigación pasiva: ASA claro, visera que dé sombra al vidrio, holgura y ventilación entre
   panel y carcasa, y una funda reflectiva opcional para parqueos largos al sol. En la Fase 1 se registra
   la temperatura interna también con la moto apagada (con un registrador aparte o el ESP32 a batería
   propia) para saber si se pasa de 80 °C. Antirrobo: tornillería de seguridad y conector no accesible
   desde fuera. Si no basta: paneles LVDS de -30..85 °C (Kyocera
   TCG070WVLEGENN-AN41, Winstar GKTW70VNIDIE0) con puente HDMI→LVDS (más costo y piezas).
4. **Táctil con agua corriendo**: cualquier táctil marca toques falsos con agua corriendo; la UI no debe
   depender de toques en marcha para nada crítico.
5. **Conector del tablero sin documentar**: si las señales vienen por un bus propietario cifrado o
   desconocido, la Fase 0 se alarga. Mitigación: lo discreto (direccionales, neutro, luz alta) casi seguro
   es cableado directo.
6. **Corte de energía al quitar la llave**: corrupción del almacenamiento. Mitigado con la retención de
   apagado (§8.1).
7. **Legalidad en Colombia**: revisar si la revisión técnico-mecánica exige velocímetro, odómetro o testigos
   del tablero original funcionando. Si lo exige, RevDash debe mostrar esos datos de forma que pase la
   inspección, o la Fase 2 conserva algo del original. Verificar antes de la Fase 2.
8. **Android Auto con touch**: open-headunit en Android 11 sin verificar; open-headunit y la vista dividida
   de Android Auto en 740×600 sin verificar.

## 13. Decisiones mías (revisar)

- Licencia de RevDash: **AGPL-3.0** (como los otros proyectos de hardware del dueño); piezas compartidas con
  RevScope en Apache-2.0. open-headunit es AGPL, compatible.
- Retención de apagado con supercondensador para apagado limpio sin consumo fantasma.
- ESP32 por USB como fuente provisional de señales en la Fase 1.
- Gestión térmica por NTC con umbrales ~60/65-70 °C.
- RevDash nunca transmite al bus de la moto (solo escucha).
- Odómetro propio con valor inicial configurable.

## 14. Lo que queda al constructor

- Esquemático de la fuente, la retención y la placa de interfaz (según Fase 0).
- Mecanismo para compartir código con RevScope (submódulo, build compuesto o librería publicada).
- Formato exacto de mensajes RevDash↔RevScope.
- Diseño visual detallado (tipografía, colores día/noche, animaciones) dentro de la distribución de §10.
- Modelo 3D de la carcasa y el soporte.
- Estructura del repo, CI y pruebas.

## 15. Documentación y publicación

Repo `santiquiroz/revdash` con LICENSE reconocida (verificar que GitHub la detecte), README con fotos,
lista de compras con enlaces, guía de armado, pinout de la Apache, guía de la imagen de Android, y
referencias cruzadas con RevScope. Nada se publica sin aprobación del dueño.

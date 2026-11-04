# Sistema visual de RevDash

Se aplica a la app RevDash (Kotlin + Compose) en la Riverdi de 7" (1024×600, horizontal). Parte de las
maquetas aprobadas (`docs/img/estado-{1,2,3}-*.png`, tokens en `docs/img/mockups/estados.css`) y las
vuelve reglas.

## Principios

1. **Se lee en un vistazo de menos de un segundo, con sol y a ~60 cm.** Si algo no se entiende en ese
   tiempo, sobra o está mal jerarquizado.
2. **La velocidad y las direccionales nunca se tapan.** Ninguna alerta, ventana ni Android Auto las cubre.
3. **Un dato que falta se muestra como "—", nunca como 0.** Un 0 km/h falso es peor que no mostrar nada.
4. **El color sigue la convención de testigos de vehículos (ISO 2575), y nunca va solo**: cada testigo
   lleva además su forma o su texto.
5. **Nada se mueve sin motivo.** El movimiento solo se usa para avisar.

## Tokens

| Token | Valor | Uso |
|---|---|---|
| `fondo` | `#0B0D10` | Fondo general (casi negro: máximo contraste con sol y menos brillo de noche) |
| `panel` | `#13171C` | Tarjetas |
| `linea` | `#1D232A` | Separadores |
| `texto` | `#E8EDF2` | Números y texto principal (contraste ~16:1 sobre `fondo`) |
| `mudo` | `#7D8790` | Etiquetas (≥4,5:1 sobre `panel`) |
| `apagado` | `#2A2F35` sobre `#15181C` | Testigo apagado (se ve la forma, no compite) |
| `verde` | `#3DDC6B` | Direccionales, neutro, sistema activo |
| `azul` | `#4AA3FF` | Luz alta |
| `ambar` | `#FFB020` | Advertencia: check engine, ABS, pata abajo, límite superado, dato desactualizado |
| `rojo` | `#FF3B3B` | Peligro: alerta de choque, temperatura crítica, REC con falla grave |

Espaciado en múltiplos de 4 dp; radios de 10 dp (testigos) y 14 dp (tarjetas). Sin sombras: la
profundidad sale del contraste entre `fondo` y `panel`.

## Tipografía

- **Barlow Condensed** (números grandes) y **Barlow** (etiquetas), las dos con licencia OFL y empaquetadas en
  la app (sin descargar fuentes). La versión condensada deja que "188" quepa grande en la columna estrecha del estado 2.
- Números siempre con **cifras tabulares**: la velocidad no "baila" al cambiar de 9 a 10.
- Escala: velocidad 230 sp (estado 1) / 120 sp (estado 2) / 56 sp (estado 3); marcha 96 / 64 / 40 sp;
  valores de tarjeta 30 sp; etiquetas 15 sp en mayúsculas con 0,12 em de espaciado. Nada por debajo de 14 sp.

## Testigos

| Testigo | Color | Encendido cuando |
|---|---|---|
| Direccional izq/der | verde, parpadeo al ritmo real del relé | Señal del cable (no está en el CAN) |
| Neutro | verde | Marcha = N (CAN 500 b4) |
| Luz alta | azul | Señal del cable |
| Check engine | ámbar | MIL (candidato CAN 502 b1 bit3, por confirmar) |
| ABS | ámbar | Pendiente de encontrar; mientras tanto "—" |
| Pata lateral abajo | ámbar | CAN 500 b5 bit6 |
| Temperatura del motor | ámbar ≥ 105 °C, rojo ≥ 115 °C (calibrar con el manual) | CAN 500 b2 − 40 |

## Datos que faltan o están viejos

- Sin dato nunca recibido: "—" en `mudo`.
- Dato que dejó de llegar (más de 3 periodos): el último valor en `mudo` y la etiqueta "sin dato" en `ambar`.
  La velocidad pasa a "—" a los 0,5 s: no se congela un valor viejo.

## Alertas

Tres niveles, de mayor a menor:

| Nivel | Ejemplos | Forma |
|---|---|---|
| P1 peligro | Vehículo acercándose rápido por detrás o el costado; choque frontal | Barra roja de 16 dp en el borde del lado de la amenaza (izquierda, derecha, abajo para atrás, arriba para adelante), con ícono y 1-3 palabras ("ATRÁS IZQ"). Pulso de 2 Hz mientras dure, desaparece 1 s después de terminar |
| P2 advertencia | Límite superado; cámara trasera sin señal; temperatura alta | Insignia ámbar junto al dato afectado (el límite va pegado a la velocidad) |
| P3 estado | Dashcam grabando; clip protegido | Chip pequeño en la esquina superior derecha; punto rojo fijo solo mientras graba |

Reglas: nunca más de una alerta P1 a la vez (gana la de menor tiempo al choque); las P1 no se apilan
sobre la velocidad; sin sonido por defecto; respetar "reducir movimiento" del sistema (el pulso pasa a
fijo).

## Toque

Objetivos táctiles de **72 dp como mínimo** (guantes) y 16 dp de separación. El paso entre estados se hace con un
toque en una zona grande y marcada, no con gestos finos. Sin deslizamientos horizontales (conflicto con
Android Auto).

## Maquetas

- `docs/img/estado-{1,2,3}-*.png`: los tres estados aprobados.
- `docs/img/estado-1-alerta.png`: alerta P1 trasera izquierda + límite superado + dashcam grabando.
- `docs/img/estado-2-alerta.png`: lo mismo con Android Auto, para ver que la columna sigue legible.

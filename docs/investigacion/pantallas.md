# Pantalla: HDMI o MIPI DSI

Fecha: 2026-11-04. Marcas: **[H]** hecho con fuente, **[I]** inferencia mía o dato por verificar.

## Requisitos (del spec)

7", 1024×600, ≥ 800 nits para sol directo, táctil que funcione con guante y agua, -20 a 70 °C, y
preferiblemente unión óptica (sin aire entre vidrio y panel: menos reflejo, no se empaña).

## Salidas de la ROCK 4D

- **HDMI 2.1**: cualquier pantalla HDMI funciona con la imagen oficial de Radxa, sin tocar el sistema [H].
- **MIPI DSI de 4 carriles**: conector propio de 39 pines y 0,3 mm; del lado del panel Radxa usa 40 pines y
  0,5 mm con una cinta dedicada [H]. Único panel probado por Radxa: Radxa Display 8 HD (8" vertical, 800×1280),
  documentado solo en Linux con un overlay de `rsetup` [H]. El controlador DSI del RK3576 es el DW MIPI DSI2,
  un canal, hasta 2560×1600 a 60 Hz [H].
- Un panel DSI distinto exige describirlo en el device tree del kernel (y su secuencia de inicio si el panel
  la necesita): solo es viable con la imagen propia de Android (Fase 2) [I].

## Opciones encontradas

| Panel | Interfaz | Brillo | Unión óptica | Táctil | Temp. | Precio | Notas |
|---|---|---|---|---|---|---|---|
| Riverdi RVT70HSHNWC00-B (`SM-` en Mouser) | HDMI + táctil USB | 850 nits | Sí | PCAP con guante | -20..70 °C | US$177,68 [H] | Entrada 7-30 V. Funciona en la Fase 1 sin tocar el sistema |
| Riverdi RVT70HSDNWC00-B | MIPI DSI (puente SN65DSI83 a LVDS) | 850 nits | Sí | PCAP, ILI2132A, USB/I²C | -20..70 °C | US$121,90 [H] | Mismo vidrio que la HDMI. 5 V. Conector Riverdi de 34 pines: hace falta un adaptador hacia los 39 pines de la ROCK 4D. El SN65DSI83 tiene driver en Linux mainline (`ti-sn65dsi83`) [I] |
| Winstar WF70A8SYAHMNN0 | MIPI DSI 4 carriles | 1100 nits | No | No trae | -20..70 °C [I] | Sin precio publicado | Panel desnudo (controladores EK79007AD3 + EK73217BCGA [H]): falta táctil, vidrio y secuencia de inicio |
| Paneles genéricos de 7" EK79007 (Alibaba) | MIPI DSI | 300-500 nits típicos [I] | No | Opcional | Sin datos | ~US$9 en lotes de 5000 [H], US$20-35 en muestras [I] | Solo sirve para producir en volumen tras validar brillo y temperatura |
| Waveshare 7" DSI (E) | DSI | No publicado | Sí | 10 puntos, I²C | No publicado | US$66,99 [H] | Waveshare dice "solo Raspberry Pi" [H] |

Descartadas por brillo de interiores (≤ 400 nits) o por ser monitores con carcasa propia: Waveshare,
Elecrow y Hosyond HDMI, NORQIN (250-350 nits en su línea), Lilliput 719/T (IP65 pero con carcasa
propia) y los CarPlay de moto (caja cerrada; los que instalan apps cuestan US$380-410).

## Conclusión

- **Fase 1:** no hace falta comprar pantalla. Se desarrolla con la placa y cualquier monitor HDMI.
- **Para la moto:** la Riverdi HDMI sigue siendo la opción sin riesgo. La versión DSI de Riverdi es el mismo
  vidrio por US$56 menos, pero suma un adaptador de 34 a 39 pines, alimentación de 5 V y trabajo de device
  tree en la imagen propia. Solo compensa si se van a hacer varias unidades.
- **Para bajar costo en serio** (paneles de US$20-35): hay que validar brillo y temperatura con muestras y
  conseguir la secuencia de inicio del fabricante. Es trabajo de la Fase 2 o posterior.

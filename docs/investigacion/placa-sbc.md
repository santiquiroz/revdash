# RevDash: qué placa SBC comprar (diciembre de 2026)

Fecha de la investigación: 1 de noviembre de 2026.
Convención: **[H]** = hecho verificado en una fuente citada; **[I]** = inferencia mía, no verificada.

---

## 0. Conclusión corta

- **Comprar: Radxa ROCK 4D (RK3576), 4 GB u 8 GB.** Tiene Android 14 oficial con código fuente y una guía de compilación publicada (rama, manifiesto y `lunch` documentados), Wi-Fi 6 + BT 5.4 con dos conectores de antena, HDMI, dos CSI, NPU de 6 TOPS y CAN en el GPIO. Consume 2,7 W en reposo y ~7 W con la CPU al máximo, y Radxa garantiza fabricarla hasta septiembre de 2034. Cuesta entre 40 y 75 USD.
- **Alternativa: ArmSoM Sige5 (RK3576).** Tiene el rango térmico más amplio de las placas comerciales (0–80 °C), acepta de 5 a 23 V por USB-C PD y trae Wi-Fi Synaptics. A cambio es más cara, solo viene con 8 o 16 GB y cuesta más conseguirla en Amazon.
- **Radxa Zero 3W: no para el producto final.** Sirve como banco de desarrollo para la app del tablero si ya la tienen. Su Android 11 está congelado desde la imagen del 30-11-2023, el ambiente máximo nominal es 50 °C y hay varios reportes de Wi-Fi/BT débil o roto en Android.
- **La decisión de arquitectura más importante no es la placa.** El propio README de open-headunit recomienda un **dongle USB de Android Auto inalámbrico** (tipo Motorola MA1 o AAWireless) como modo principal. El dongle se encarga del Bluetooth y del Wi-Fi 5 GHz con el teléfono y la placa solo ve Android Auto por cable USB. Así deja de depender de que el driver Wi-Fi de una imagen Rockchip soporte SoftAP 5 GHz o Wi-Fi Direct, que es el punto con menos evidencia de todo este análisis.

---

## 1. Radxa Zero 3W: estado del soporte Android

| Punto | Hallazgo |
|---|---|
| Versión de Android | **[H]** Solo Android 11. La página oficial de descargas lista una única imagen, `Radxa-Zero3WE-20231130-gpt.zip` (tag `radxa-zero3-we-android11-rkr12-20240111`). No encontré Android 12/13/14 oficial ni comunitario. |
| Compilarlo uno mismo | **[H]** Hay una rama `Android11_Radxa_rk12` con `rockchip-r-release.xml`, pero un usuario del foro reporta que el tutorial no trae un `lunch` para la Zero 3W. Hay errores de compilación con workarounds de foro. **[I]** La Fase 2 (imagen propia) parte de un BSP sin target oficial para la placa. |
| Wi-Fi/BT (AIC8800) en Android | **[H]** La imagen del 09-11-2023 no detecta redes ni dispositivos BT. La del 30-11-2023 arregla el Wi-Fi (hilos de nov-2025 y sep-2025). Otro reporte (mar-2025, imagen rkr12-20240111) mide 5–10 Mbps a 2 m de un AP de 5 GHz y BT con pérdidas y unos 5 s de latencia. Otro usuario necesitó una imagen especial de Radxa con la antena externa activada. **No encontré ningún reporte de SoftAP 5 GHz, Wi-Fi Direct ni Android Auto funcionando.** En Linux, el BT del AIC8800 por UART1 requiere servicios de reinicio de UART y puede colgarse tras una desconexión. |
| Temperatura | **[H]** Ambiente nominal **0–50 °C**. Radxa dice que "para carga sostenida o ambiente elevado puede requerir refrigeración adicional". |
| Alimentación y tamaño | **[H]** 5 V/2 A por USB-C o por los pines 2/4. Mide 65 × 30 mm. Micro HDMI hasta 1080p60, 1 CSI de 4 carriles, hasta 5 UART. |
| ¿Se sigue fabricando? | **[H]** Sí. El product brief rev. 1.12 (20-09-2026) garantiza disponibilidad **hasta al menos septiembre de 2030**. No hay aviso de EOL. |
| Precio y disponibilidad | **[H]** El stock varía según la tienda: Ameridroid ~44 USD (agotado al consultar), RS Polonia sin stock, Hubtronics India ₹3.699–5.760. Allnet lista la versión de 4/32 GB. No pude verificar un listado activo en Amazon con envío a Colombia. |

**Veredicto [I]:** como placa final para algo que "no quede obsoleto pronto" es la peor opción del grupo. Android 11 ya está fuera de soporte de seguridad de Google, el BSP no tiene target y los 50 °C de ambiente quedan justo en el rango que se alcanza dentro de una caja estanca al sol.

---

## 2. Comparación de alternativas

Leyenda: "Android" = imagen oficial del fabricante y fecha del build más reciente que encontré.

| Placa | SoC / NPU | RAM | Android oficial (último build) | Wi-Fi/BT | Video / cámara / E/S | Consumo | T. operación | Tamaño | Precio / disponibilidad |
|---|---|---|---|---|---|---|---|---|---|
| **Radxa Zero 3W** | RK3566 (22 nm [I]), sin NPU útil | 1–8 GB | 11 (30-11-2023) [H] | AIC8800, Wi-Fi 6, BT 5.4 [H] | micro HDMI 1080p60, 1 CSI 4L, 5 UART [H] | No hay cifra fiable; ~2–3 W según el foro [H, no medido formalmente] | **0–50 °C** [H] | 65×30 mm [H] | ~15–45 USD; garantía hasta 09-2030 [H] |
| **Radxa ROCK 4D** | RK3576, **6 TOPS** [H] | 2/4/8/16 GB LPDDR5 [H] | **14** (SD/NVMe 27-05-2025; UFS-update con GMS 14-10-2025); código fuente y build documentados [H] | Quectel FCU760K, Wi-Fi 6, BT 5.4, 2× u.FL [H] | HDMI 2.1, DSI 4L, CSI 4L (o 2×2L) + CSI 2L, 5 UART, **CAN**, ADC [H] | **2,7 W reposo / ~7 W CPU al 100 %** [H] | 0–60 °C (RK3576); −40–85 °C (RK3576J, difícil de comprar) [H] | 87×58×20 mm [H] | Lanzamiento 30/40/60/100 USD; Amazon 4 GB 59,99 USD de terceros (ene-2026, agotado); Tinytronics 8 GB 74,50 €; **garantía hasta 09-2034** [H] |
| **ArmSoM Sige5** | RK3576, 6 TOPS [H] | 8/16 GB LPDDR4X [H] | 14 "oficial Rockchip" (Google Drive, sin fecha visible) [H] | **Synaptics SYN43752**, Wi-Fi 6, BT 5.3 [H] | HDMI 2.1, 2× CSI 4L, 40 pines [H] | Sin dato | **0–80 °C** [H] | 92×62×14,6 mm [H] | No encontré precio vigente; ArmSoM suele ser más cara que Radxa [H] |
| **Luckfox Omni3576** | RK3576, 6 TOPS [H] | 4/8 GB [H] | 14 [H] | Wi-Fi 6, BT 5.2 (chip sin nombre) [H] | HDMI 2.1, 2× CSI 4L, 2× GbE, M.2 [H] | Sin dato | 0–60 °C [H] | No publicado (39 g) | **210,99 USD, agotado** en Waveshare; Spotpear lo marca "off-production" [H] |
| **Orange Pi 3B** | RK3566, sin NPU útil | hasta 8 GB | 11 [H] | v2.1: AP6256 Wi-Fi 5 + BT 5.0 [H] | HDMI, CSI, M.2 [H] | Sin dato | Sin dato | ~85×56 mm [I] | Barata; mismo techo de SoC que la Zero 3W [I] |
| **Orange Pi 5 / 5 Pro** | RK3588S, 6 TOPS [H] | hasta 16 GB | **12** (todas las fuentes) [H] | 5 Pro: Wi-Fi 5/BT 5 [I] | 2× HDMI (5 Pro), CSI | Más alto que RK3576 [I] | Sin dato | ~89×56 mm [I] | 80–150 USD [I] |
| **Radxa ROCK 5C** | RK3588S2, 6 TOPS [H] | 2–16 GB | **12** (03-09-2024) [H] | Wi-Fi 6, BT 5.4 [H] | HDMI, CSI, 40 pines | Más alto que RK3576 [I] | Sin dato | ~85×56 mm [I] | Hubtronics, Amazon de terceros [H] |
| **Khadas Edge2** | RK3588S, 6 TOPS | 8/16 GB | **14** (V250708, julio de 2025) [H] | AP6275P Wi-Fi 6 2T2R, BT 5.0 [H] | HDMI, sin Ethernet [H] | Fuente USB-C PD de 30 W [H] | No hay rango oficial; 61,9 °C de pico con 28 °C de ambiente [H] | Delgada | 199–240 USD [H] |
| **Raspberry Pi 5 + KonstaKANG** | BCM2712, **sin NPU** | 4–16 GB | No oficial: AOSP 17 (30-10-2026), LineageOS 22.2/23.2 [H] | CYW43455; tethering Wi-Fi/BT OK y **Wi-Fi Direct añadido** [H] | HDMI, **CSI "con varios problemas"** [H], GPIO OK | 4–5 W reposo, ~12 W pico; throttling a 80 °C [H] | No encontré cifra oficial | 85×56 mm | Fácil de conseguir. **Licencia CC BY-NC-SA (no comercial)** [H] |
| **Radxa Zero 3E + Wi-Fi USB** | RK3566 | 1–8 GB | 11 [H] | Ninguno integrado | Igual que la 3W + Ethernet | — | 0–50 °C [I, misma base que la 3W] | 65×30 mm | **[I] Mala idea en Android:** un adaptador Wi-Fi USB necesita su driver compilado en el kernel de la imagen; no es plug-and-play. |
| **Radxa Cubie A7Z** (nueva) | Allwinner A733, 3 TOPS [H] | 1–16 GB | 13 [H] | Wi-Fi 6, BT 5.4 [H] | Formato Zero (65×30) [H] | Sin dato | Sin dato | 65×30 mm [H] | Desde 15 USD; garantía hasta 09-2034 [H]. **[I]** Las imágenes Android de Allwinner tienen peor historial que las de Rockchip. |
| **Head unit china UIS7862 (FYT, etc.)** | Unisoc UIS7862 | 4–8 GB | 12–13 del fabricante, cerrado [H parcial] | Integrado, con AA/CarPlay inalámbrico propio (ZLINK) [I] | Sin CSI ni GPIO; entradas AHD y UART del CAN-box [I] | Diseñada para 12 V [I] | Grado auto [I] | Chasis 1-2 DIN, pantalla LVDS propia [I] | **[I]** Sin código fuente, root según el modelo, no acepta la Riverdi por HDMI. open-headunit trae soporte experimental para su módulo BT `/dev/auto_serial` [H]. |

Notas sobre la tabla:
- **[H]** Rockchip RK3576 ya tiene BSP de **Android 15** de terceros (Advantech AOM-3841, beta v0.1.7, 18-11-2025; MYIR anunció 14→15 en sep-2026). No encontré Android 16 para RK3576. **[I]** La línea RK3576 todavía recibe versiones nuevas; la RK3566 se quedó en 11–13.
- **[H]** La pantalla Riverdi RVT70HSHNWC00-B opera de −20 a 70 °C, da 850 cd/m², su retroiluminación consume hasta ~2,6 W y la rev. 1.6 acepta de **7 a 30 V DC** en su jack. **[I]** Puede tomar los 12 V de la moto directamente (con protección) y la SBC lleva su propio buck a 5 V.

---

## 3. Riesgos por opción en este caso concreto

### Calor en caja estanca (el riesgo dominante)
- **[I]** Dentro de una caja sellada al sol en Colombia, el aire interior puede pasar de 50–60 °C. La disipación por placa de aluminio solo funciona si el SoC está pegado al aluminio con un pad térmico; el aire interior no sirve.
- Zero 3W: **0–50 °C** de ambiente [H]. Es la más ajustada. **[I]** En la práctica el aire interior importa menos que la ruta conductiva SoC → aluminio, pero una placa de 30 mm con el SoC en una cara y el eMMC en la otra es difícil de acoplar bien.
- ROCK 4D: 0–60 °C [H]. **[I]** Su SoC de 8 nm consume poco por tarea (2,7 W en reposo [H]) y sus 87×58 mm permiten un disipador plano grande. La versión RK3576J (−40–85 °C) sería la ideal, pero no encontré dónde comprarla (hilo del foro de ago-2025 sin respuesta [H]).
- Sige5: **0–80 °C** [H]. Es el mejor rango nominal de las comerciales.
- RK3588S (OPi 5, ROCK 5C, Edge2): **[I]** consumen más que el RK3576 y no aportan nada que este caso necesite.
- Pi 5: 4–5 W en reposo [H] y throttling a 80 °C. **[I]** Es la peor relación calor/beneficio aquí, y no tiene NPU.

### Soporte de Android a largo plazo
- ROCK 4D: **[H]** Android 14 con build documentado (`Android14_RK3576_Radxa_rk6`, `rockchip-u-release.xml`, `lunch rk3576_rock_4d-userdebug`). Radxa sacó una actualización UFS con GMS el 14-10-2025 y garantiza fabricarla hasta 2034. Es la mejor base para la Fase 2.
- Sige5 / Luckfox: **[H]** Android 14. **[I]** Comunidades más chicas, y el Luckfox está agotado o descontinuado.
- Zero 3W / OPi 3B: **[H]** Android 11. **[I]** No hay camino oficial a versiones nuevas.
- Pi 5: **[H]** builds nuevas cada mes (AOSP 17 del 30-10-2026), pero dependen de una sola persona y la licencia es no comercial. **[I]** Riesgo si RevDash deja de ser solo personal.

### Wi-Fi/BT para Android Auto inalámbrico
- **[H]** open-headunit (AGPL-3.0) ofrece estos modos: dongle USB (recomendado), Native (Wi-Fi Direct o hotspot del head unit; recomiendan 5 GHz), Headunit Server y un relé Bluetooth Classic con un **ESP32 original** (no S2/S3/C3/C6). Desde Android Auto 17.4 los disparadores inalámbricos antiguos (Self-Mode, Wireless Helper) dejan de funcionar; Native, el dongle y el Headunit Server siguen funcionando. Con teléfonos Android 12+ y Wi-Fi Direct puede hacer falta un BSSID estático (v3.1.1+).
- **[H]** No encontré ningún reporte, para ninguna placa Rockchip con Android (Zero 3W, ROCK 4D, Sige5), de SoftAP 5 GHz o Wi-Fi Direct funcionando con open-headunit. La Pi 5 con KonstaKANG es la única con Wi-Fi Direct listado como funcional.
- **[I]** Synaptics/Broadcom (Sige5, Pi 5, Edge2) suele tener mejor soporte de P2P/SoftAP en Android que AIC8800. No sé qué chip lleva el Quectel FCU760K de la ROCK 4D.
- **Mitigación [I]:** usar un **dongle USB de AA inalámbrico**, que es lo que recomienda open-headunit. Si se quiere evitar el dongle, el relé con ESP32 saca el Bluetooth de la placa y deja solo el Wi-Fi 5 GHz como riesgo, y ese Wi-Fi se puede probar el primer día.

### 12 V de la moto
- **[H]** Todas las SBC del grupo se alimentan a 5 V, salvo la Sige5 (5–23 V por PD, así que necesita negociar PD) y la Edge2 (PD). La ROCK 4D pide una fuente de ≥10 W, o 25 W con USB y PCIe cargados.
- **[I]** Usar un buck automotriz de 12→5 V/3–5 A con TVS contra picos de carga (load dump) y polaridad inversa. La Riverdi va directo a 12 V. Para que el arranque sea rápido, alimentar desde una línea que no se corte durante la marcha.

---

## 4. Recomendación para comprar en diciembre de 2026

### Opción principal: **Radxa ROCK 4D 4 GB** (8 GB si se quiere margen para la NPU y la dashcam)
Por qué:
1. Es la única del grupo que reúne Android 14 oficial, código fuente con un `lunch` de la placa ya documentado (base directa para la Fase 2) y garantía de fabricación escrita hasta **septiembre de 2034** [H].
2. Cubre todas las fases sin cambiar de placa [H]: NPU de 6 TOPS para visión, dos CSI (dashcam más una segunda cámara), CAN y UART en el GPIO para leer la moto, HDMI para la Riverdi y H.265/H.264 por hardware para el video de Android Auto.
3. Consumo bajo para su clase (2,7 W / ~7 W) [H] y un precio de 40–75 USD.

Condiciones:
- Medir la temperatura del SoC dentro de la caja con la Riverdi al 100 % y Android Auto proyectando antes de cerrar el diseño térmico [I].
- Usar las 2 antenas u.FL por fuera de la caja o con una tapa que deje pasar RF. Radxa advierte que sin antenas la señal es "muy pobre" [H].
- Probar SoftAP 5 GHz y Wi-Fi Direct en la imagen oficial **el primer día**. Si fallan, usar el dongle de AA inalámbrico o el relé con ESP32 [I].
- **[I]** En Amazon US, la ROCK 4D aparece sobre todo por vendedores externos y con stock intermitente (ASIN B0G32YL29F en camelcamelcamel). Si no hay stock con envío a Colombia, Arace y AliExpress son la ruta habitual de Radxa.

### Alternativa: **ArmSoM Sige5 8 GB**
- Conviene si el diseño térmico sale ajustado: su rango de 0–80 °C es 20 °C mejor que el de la ROCK 4D. Además acepta 12 V por PD y su Wi-Fi Synaptics probablemente se porta mejor como AP/P2P [H para el rango, la entrada y el chip; I para el comportamiento del Wi-Fi].
- Contras: más cara, no hay versión de 4 GB, el Android 14 sale de un Google Drive sin fecha visible, la comunidad es más chica y no encontré listado vigente en Amazon [H].

### ¿Mantener la Zero 3W?
- **Si ya está comprada:** usarla como banco de desarrollo de la app del tablero. Android 11 corre de sobra una app con minSdk bajo. No invertir en la Fase 2 sobre ella.
- **Si no está comprada:** no comprarla. Ahorra unos 25 USD frente a la ROCK 4D, pero queda en Android 11, con 50 °C de ambiente, sin NPU útil y con Wi-Fi/BT que aparecen débiles en varios reportes [H + I].

---

## Fuentes

Radxa Zero 3W
- https://docs.radxa.com/en/zero/zero3/other-os/android/download
- https://dl2.radxa.com/zero3/docs/hw/3w/radxa_zero_3w_product_brief.pdf (rev. 1.12, 20-09-2026)
- https://forum.radxa.com/t/zero-3w-android-bluetooth-and-wi-fi-problems/29721
- https://forum.radxa.com/t/zero-3w-android-img-wifi-problems/28767
- https://forum.radxa.com/t/extremely-weak-signal-when-using-android-on-radxa-zero-3w/26414
- https://forum.radxa.com/t/radxa-3w-android-how-to-switch-to-external-wifi-antenna/22225
- https://forum.radxa.com/t/how-to-build-android-img-for-zero3w/22234
- https://forum.radxa.com/t/bluetooth-not-working-zero-3w-official-os/24072
- https://ameridroid.com/products/radxa-zero-3w
- https://pl.rs-online.com/web/p/plytki-rock-sbc/2564693
- https://hubtronics.in/radxa-zero-3w-2gbram
- https://shop.allnet.de/detail/index/sarticle/359780

Radxa ROCK 4D
- https://docs.radxa.com/en/rock4/rock4d
- https://docs.radxa.com/en/rock4/rock4d/download
- https://docs.radxa.com/en/rock4/rock4d/other-os/android/low-level-dev/build-system
- https://github.com/radxa/manifests/releases/tag/radxa-rock4d-20250528
- https://dl.radxa.com/rock4/4d/docs/radxa_rock4d_product_brief.pdf (rev. 1.3, 26-06-2026)
- https://www.cnx-software.com/2025/05/28/radxa-rock-4d-sbc-raspberry-pi-lookalike-powered-by-rockchip-rk3576-edge-ai-soc/
- https://linuxgizmos.com/radxa-rock-4d-with-rk3576-soc-pcie-gen2-x1-gigabit-ethernet-and-poe-support/
- https://www.linuxlinks.com/radxa-rock-4d-single-board-computer-running-linux-power-consumption/
- https://forum.radxa.com/t/where-to-buy-the-industrial-grade-rock-4d-version/28724
- https://camelcamelcamel.com/product/B0G32YL29F
- https://www.tinytronics.nl/en/development-boards/single-board-computers/radxa-rock-4d-rk3576-8gb
- https://www.quectel.com/product/wi-fi-bluetooth-fcu760k/

Otras placas
- https://docs.armsom.org/armsom-sige5
- https://www.waveshare.com/luckfox-omni3576.htm?sku=29090
- https://spotpear.com/index.php/shop/Luckfox-Omni3576-Core3576-Rockchip-RK3576.html
- https://www.cnx-software.com/2024/06/07/orange-pi-3b-v2-1-sbc-better-wifi-5-m2-2280-nvme-sata-ssd-socket/
- https://openelab.com/products/orange-pi-5-pro
- https://www.cnx-software.com/2024/04/12/orange-pi-5-pro-low-cost-rockchip-rk3588s-sbc-16gb-lpddr5-ram-dual-hdmi/
- https://docs.radxa.com/en/rock5/rock5c/download
- https://forum.khadas.com/t/edge2-android-14-v250708-rom-release/22302
- https://www.cnx-software.com/2022/09/13/khadas-edge2-ultra-thin-sbc-is-powered-by-rockchip-rk3588s-processor/
- https://www.techradar.com/pro/khadas-edge2-review
- https://konstakang.com/devices/rpi5/AOSP17
- https://konstakang.com/devices/rpi5/LineageOS22/
- https://www.raspberrypi.com/news/heating-and-cooling-raspberry-pi-5/
- https://docs.radxa.com/en/cubie/a7z
- https://liliputing.com/radxa-cubie-a7z-is-a-raspberry-pi-zero-sized-allwinner-a33-octa-core-pc-for-15-and-up/
- https://android-headunits.com/the-fastest-android-headunit_2022/
- https://docs.aim-linux.advantech.com/docs/processors/rockchip/RK3576/AOM-3841_wiki_data/AOM-3841_Android15_Release_Note
- https://en.myir.cn/ProductUpdates/169.html

Software y pantalla
- https://github.com/andreknieriem/open-headunit
- https://github.com/andreknieriem/headunit-revived/wiki/Wireless
- https://www.xda-developers.com/motorola-ma1/
- https://unisystem.com/product/displays-lcd-tft/rvt70hshnwc00-b-riverdi-hdmi-display
- https://download.unisystem.com/LCD-TFT/RVT70HSHNWC00/RVT70HSHNWC00.pdf

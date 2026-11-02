# Android Auto en un tablero de moto (Radxa Zero 3W + Android 11): licencia, riesgos y alternativas

Fecha de la investigación: 1 de noviembre de 2026. Caso: RevDash, tablero de reemplazo para TVS Apache RTR 160 4V (uso propio), y qué cambia si alguien distribuye o vende hardware con RevDash.

Convención: **[H]** = hecho verificado con fuente (o verificado por mí directamente, se indica). **[I]** = inferencia mía. **[V]** = afirmación de un vendedor o de una fuente débil que no pude confirmar. Nada de esto es asesoría legal: para distribuir o vender hardware con esto hay que consultar a un abogado de propiedad intelectual.

---

## Resumen ejecutivo

1. **No existe una vía pública para que un fabricante pequeño implemente un receptor de Android Auto con licencia.** Google no publica ningún SDK ni formulario de partners. Las marcas que lo hacen (automotrices, Tier-1 y marcas de estéreos) trabajan con un contacto en Google y certifican a través de laboratorios acreditados (3PL). Los costos no son públicos. [H]
2. **El bloqueo técnico es un certificado emitido por Google.** El teléfono solo acepta receptores con un certificado TLS emitido por la raíz "Google Automotive Link". Todas las implementaciones abiertas que funcionan usan un certificado de terceros cuya clave privada es pública. Lo comprobé en el código: aasdk incluye uno emitido a **JVC Kenwood** y Open Headunit uno de la **referencia de Google**. [H]
3. **En motos, Android Auto oficial existe pero solo a nivel de fabricante.** Lo tienen Honda (Gold Wing y Africa Twin), Harley-Davidson Touring y SYM TTLBT. Indian y Can-Am solo traen CarPlay, y KTM usa Android Automotive. Google no publica un "modo moto" ni requisitos para dos ruedas. TVS aparece como ícono en Android Auto desde enero de 2025, pero no hay anuncio, y su Apache usa una app propia (SmartXonnect). [H]
4. **Las pantallas aftermarket para moto (Chigee, Carpuride, Aoocci, Ottocast) no muestran certificación de Google en público.** Se presentan como "receptores" que emulan una unidad de auto. En julio de 2026 Google atribuyó una falla de Android Auto 17.2 a "uncertified aftermarket head units": el mercado no certificado es grande, y Google lo tolera pero no lo garantiza. [H/I]
5. **Para el piloto personal:** Open Headunit / Headunit Revived (AGPL-3.0, en Google Play, muy activo) corre directamente en Android 11 sobre la Radxa. Es la opción más rápida y de riesgo bajo para uso propio. [H/I]
6. **Para distribuir o vender hardware con RevDash:** no conviene vender el equipo con aasdk/Open Headunit preinstalado ni anunciar "Android Auto". El núcleo debe ser un tablero propio con una app propia en el celular (BLE), siguiendo el modelo de TVS SmartXonnect y Royal Enfield Tripper. Android Auto o CarPlay quedarían como opción solo mediante un módulo de un proveedor que demuestre la certificación. [I]

---

## 1. Qué exige Google para implementar un receptor de Android Auto

### Programa y proceso
- **No hay programa público ni SDK descargable.** La página oficial de Android for Cars solo indica que, para tipos de partner y acceso, hay que hablar con "your Google point of contact", y no da enlace ni formulario. [H] (developers.google.com/cars)
- **Una "receiver library" de Google existe, pero no es pública.** La documentación de AARE (ingeniería inversa del protocolo) menciona "factory units built on Google's receiver library". [H, fuente técnica de terceros]
- **La certificación es obligatoria y pasa por laboratorios 3PL acreditados por Google.** Proveedores como Harman, Ubikon y Symbio venden ese servicio. Las pruebas incluyen banco y vehículo, sobre Bluetooth, Wi-Fi, USB y los modos de proyección. Harman dice haber certificado más de 150 unidades. Concept Reply afirma que "no OEM can ship a vehicle with AA/CP without passing the official certification process". Entre los documentos obligatorios está el "Android Auto AAP Application & Project Overview". [H, fuentes comerciales de los mismos laboratorios]
- **Los fabricantes de motos sí pasan por esa certificación.** Una vacante de Hero MotoCorp pide experiencia en "Certification process for Android Auto and Apple Car Play". [H]
- **Costos y regalías no son públicos.** Concept Reply dice "both technologies are available for free" (se refiere a la plataforma), pero hay que pagar laboratorio, consultoría y hardware de prueba. No encontré cifras de cuotas por unidad. [H cita / I interpretación]
- **Sí ha habido marcas aftermarket pequeñas.** En 2018 Google sumó como partners de receptores a UniMax, BAUHN, Jenson, Axxera y Dual Electronics, que son marcas de gama baja o media. Siempre fue por acuerdo directo con Google, no por un proceso abierto. [H]

### Requisitos técnicos conocidos
- **Autenticación mutua por TLS.** El teléfono funciona como servidor TLS y el receptor como cliente. Android Auto solo confía en la raíz "Google Automotive Link": "Google's Android Auto only talks to head units with a certificate issued by Google". Los fabricantes pueden obtener uno y los proyectos hobby no. [H, AARE]
- **No hay chip de autenticación tipo MFi.** Lo público describe una autenticación por certificado X.509 y clave en el receptor, sin coprocesador dedicado. CarPlay es distinto: el programa MFi de Apple sí incluye coprocesadores de autenticación. [I para Android Auto; H para MFi]
- **Wireless Android Auto usa Bluetooth para el arranque y Wi-Fi (Direct o hotspot) para el video.** El estándar de facto es Wi-Fi de 5 GHz. El módulo D8 (AIC8800) de la Radxa Zero 3W figura como Wi-Fi 6 de 2,4 y 5 GHz con BT 5.4. [H fuente secundaria / I]
- **Uso de la marca.** Las reglas de marca de Android restringen el uso de "Android" en hardware y marketing a dispositivos compatibles. El marketing con marcas de Google requiere aprobación de Google. Un producto no certificado no puede anunciarse como "Android Auto". [H reglas generales / I aplicación]

### Vía para pequeños fabricantes o integradores
- **No hay una vía pública.** Lo realista es integrar hardware que ya esté certificado, de un Tier-1 o un ODM. Por ejemplo, Harman lanzó en marzo de 2026 **Ready Ride**, una plataforma para dos ruedas con CarPlay y Android Auto, sobre Snapdragon. Está orientada a OEM y no publica precios. [H producto / I sobre la accesibilidad para volúmenes pequeños]

---

## 2. Android Auto en motos: estado actual

| Fabricante / modelo | Android Auto | CarPlay | Nota |
|---|---|---|---|
| Honda Gold Wing | Sí (desde 2020) | Sí (2018, primera moto con CarPlay) | Fábrica [H] |
| Honda Africa Twin CRF1100L (2020+) | Sí (actualización en concesionario, feb. 2021) | Sí | [H] |
| Harley-Davidson Touring (Boom! Box GTS) | Sí (primera marca con AA) | Sí | [H] |
| SYM TTLBT (maxi-scooter) | Sí | Sí | Según SlashGear, jul. 2025 [H secundaria] |
| Indian (RIDE COMMAND 7") | No confirmado | Sí (2020+, software de julio 2025) | La fuente oficial solo menciona CarPlay [H] |
| Can-Am Pulse / Origin | En desarrollo / no confirmado | Sí | [H secundaria] |
| BMW Motorrad | No (usa la app BMW Connected y Bosch mySPIN) | No | [H secundaria] |
| KTM | No: usa **Android Automotive OS** embebido (oct. 2024) | — | Otro producto, no es proyección [H] |
| TVS | Ícono "TVS" en Android Auto 13.5 (ene. 2025), sin anuncio | — | La Apache RTR 160 4V usa SmartXonnect (BT, flechas) y en 2026 una TFT de 5" con "Google Maps Mirroring" [H] |

- **"Modo moto".** No encontré un modo moto oficial de Android Auto ni requisitos de certificación específicos para dos ruedas. Lo que sí existe [H]:
  - Desde Android Auto 13.5, Google cambió "car" por "vehicle" en los ajustes (enero de 2025).
  - Waze permite elegir el tipo de vehículo "Motorcycle" también dentro de Android Auto.
  - Sygic lanzó su "Motorbike Mode" para Android Auto y CarPlay en julio de 2026.
  - En el celular, Google Maps tiene modo motocicleta en Colombia y Waze Moto llegó a Colombia en julio de 2026.
- **La afirmación de que la EULA de Android Auto lo limita a vehículos de cuatro ruedas viene de un blog de Aoocci y no cita ninguna fuente.** No la pude confirmar. Además, Honda y Harley la contradicen en la práctica. [V]
- **Conclusión:** Google certifica Android Auto en motos cuando lo pide un OEM. Que un tablero aftermarket genérico sea un "head unit" certificable es otra historia, y no hay evidencia pública de que Google lo haga para motos aftermarket. [I]

---

## 3. Pantallas aftermarket para motos: cómo lo hacen

- **Arquitectura común.** Son pantallas autónomas: el teléfono se conecta por Bluetooth y Wi-Fi y proyecta CarPlay o Android Auto. Chigee AIO-5 pide Android 11 o superior y lee los datos de la moto por OBD. [H]
- **Cómo se presentan.** Aoocci dice que su chip V851S "emula un head unit de auto, así que el teléfono identifica la moto como un auto". También dice que sus equipos "no están sujetos" a los requisitos de certificación de fábrica, y se describe como "Legal Proxy" o "Projection Host". No nombra ninguna licencia de Google. [V, página del propio vendedor]
- **Certificación.** No encontré ninguna afirmación pública de certificación de Google para Chigee, Carpuride, Aoocci ni Ottocast. Un foro los describe como "no different than other Chinese head units, just slicker marketing". [H por ausencia / V]
- **Middleware chino.** Muchas unidades Android chinas usan **Zlink** (Beijing Zjinnova), que hace que el teléfono trate la unidad como un sistema certificado y se activa con un token del servidor del fabricante. Como alternativas con licencia real, los foros mencionan Sony y Pioneer. [H fuentes secundarias]
- **Dongles Carlinkit (CPC200-CCPA/Autokit).** Emulan un head unit, crean su propio hotspot y se usan con la app AutoKit en unidades Android. No hay información pública sobre de dónde sale su certificado de Google. [H foro y vendedor / I]
- **Evidencia de escala.** En julio de 2026 Android Auto 17.2 provocó caídas, y la declaración de Google difundida por la prensa fue: "we identified an issue with uncertified aftermarket head units". Google sacó un parche (17.2.662638 y 17.3.662813). Esto muestra que existe un mercado grande de unidades no certificadas. Google las parcheó en vez de bloquearlas, pero no promete nada. [H / I]
- **Lo que se sabe en resumen:** casi todo el aftermarket barato es no certificado y emula el protocolo. Una minoría de marcas establecidas (Pioneer, Kenwood, Sony y otras de autos) sí está certificada. [I con base en lo anterior]

---

## 4. Implementaciones abiertas o no oficiales: estado en 2025-2026

Metadatos consultados por mí en la API de GitHub el 2026-11-01:

| Proyecto | Tipo | Licencia | Última actividad | Estado |
|---|---|---|---|---|
| f1xpl/aasdk + openauto | Librería y receptor Linux originales | GPLv3 (según la comunidad; GitHub no detecta SPDX) | aasdk 2023-07 | Congelado; el README dice "for R&D purposes" [H] |
| opencardev/aasdk + openauto | Fork mantenido | GPL-3.0 (openauto) | 2026-06 / 2026-02 | Activo, pocos mantenedores [H] |
| Crankshaft (opencardev) | Distro para Raspberry Pi basada en openauto/aasdk | GPL-3.0 | 2026-10-04 | Activo, 2,7k estrellas [H] |
| OpenAuto Pro (BlueWave Studio) | Comercial, cerrado | Propietaria | — | **La empresa desapareció sin liberar el código.** El intento de revivirlo (mrmees) está archivado [H] |
| Hudiy (wiboma) | Plataforma de head unit para Linux y Raspberry Pi | Propietaria, licencia paga por instancia | 2026-10 | Activo. Para CarPlay pide un Carlinkit CPC200-CCPA [H]. No encontré confirmación de que sea sucesor de BlueWave |
| **Open Headunit / Headunit Revived** (andreknieriem) | **App Android** que convierte una tablet en receptor | **AGPL-3.0** | v3.5.0 (2026-10-17), v4.0.0-beta1 (2026-10-27) | Muy activo, 3,1k estrellas, **publicada en Google Play** [H] |
| Headunit Reloaded (gb.xxy.hr) | App Android paga y cerrada (Emil Borconi), basada en la de Michael Reid | Propietaria | Ficha actualizada en 2026 | En Play [H secundaria] |
| aa-proxy-rs | Proxy de Android Auto con cable o inalámbrico | GPL-2.0 | 2026-10-30 | Activo [H] |
| AARE / Flywheel | Lado del teléfono (alternativa a la app de Google) | GPL-3.0 | 2026-11 / 2026-10 | No sirve para tu caso, pero documenta el protocolo [H] |

### Estado técnico
- **Certificados (verificado por mí con openssl):**
  - `opencardev/aasdk/cert/headunit.crt` tiene subject `O=JVC Kenwood`, issuer `O=Google Automotive Link` y vigencia de 2014-07-04 a 2045-04-29. Viene con su `headunit.key`, y el README lo llama "publicly available ... not a secret".
  - `open-headunit/app/src/main/res/raw/cert` tiene subject `O=Google-Android-Reference`, issuer `Google Automotive Link`, vigencia de 2014 a 2044, y está acompañado de `privkey`. [H]
  - Por eso funcionan con la app oficial: usan certificados emitidos por Google a terceros, con claves filtradas o publicadas.
- **Google rota sus propias claves.** Rota las del lado del teléfono en cada versión. AARE indica que el certificado del teléfono "CarService" vence el 2026-12-31; los receptores que revisan la vigencia fallarían, pero openauto, Crankshaft y Headunit Revived no la revisan. [H]
- **Cambios que rompen.** El README de Open Headunit advierte que **"Android Auto 17.4 and newer can prevent legacy wireless triggers"**, como Wireless Helper y Self-Mode. Ofrece estas salidas: dongle USB inalámbrico, modo nativo por Wi-Fi Direct o hotspot, "Headunit Server" en modo desarrollador o un workaround con root. [H] La documentación de Google describe "Start head unit server" como herramienta del Desktop Head Unit para **probar apps**. [H] Usarla en un producto es abusar de una función de desarrollo. [I]
- **Riesgo de bloqueo.** Google puede dejar de confiar en esos certificados filtrados en cualquier versión: es un riesgo permanente, de impacto alto y probabilidad incierta. Hasta noviembre de 2026 no lo ha hecho, porque los proyectos siguen funcionando y actualizándose. [I]

### Riesgo de vender un producto con estas implementaciones [I, salvo donde se indica]
1. **Uso de un certificado y una clave ajenos.** Se usa una identidad criptográfica emitida a JVC Kenwood, o a la referencia de Google, sin autorización. En un producto vendido esto es lo más grave: suplantación del receptor, posible competencia desleal o incumplimiento de términos.
2. **Normas anti-elusión.** En Colombia, la Ley 1915 de 2018 (TLC con EE. UU.) sanciona eludir "medidas tecnológicas efectivas" que controlan el acceso a una obra y también comercializar dispositivos que las evaden. [H: la ley existe] Que el chequeo del certificado cuente como una de esas medidas es una pregunta para un abogado. [I] En EE. UU. aplicaría la DMCA §1201.
3. **Marca.** No se puede anunciar "Android Auto" ni usar su logo.
4. **Copyleft.** Con GPL o AGPL hay que entregar el código fuente, incluidas tus modificaciones, a cada comprador. Es compatible con vender, pero obliga. La AGPL de Open Headunit también cubre el uso por red.
5. **Garantía y soporte.** Una actualización de Google puede inutilizar la función estrella del producto, como pasó con 17.2 y 17.4. Frente al Estatuto del Consumidor (Ley 1480 de 2011), eso es un riesgo de reclamos. [I, no investigado en detalle]
6. **Para uso personal el riesgo práctico es bajo.** Hay miles de usuarios de Crankshaft y Headunit Revived, y la app está en Google Play. [I]

---

## 5. Alternativas legales para hardware que se distribuya

| Opción | Cómo funciona | Legalidad | Costo aproximado | Pros | Contras |
|---|---|---|---|---|---|
| **A. Receptor propio certificado** | Acuerdo con Google, receiver library y certificación 3PL | Plena | No público; laboratorio, consultoría y meses de trabajo [I: inviable con bajo volumen] | Android Auto "de verdad" | Sin vía pública; Google prioriza OEM y Tier-1 |
| **B. Módulo ya certificado de un Tier-1 u ODM** (p. ej. Harman Ready Ride; ODMs de clusters TFT) | El módulo hace Android Auto/CarPlay y entrega video al tablero | Plena **si** el proveedor muestra la certificación (pedir carta o reporte 3PL) | No público; orientado a OEM [I] | Traspasa el problema de licencia | Difícil de conseguir para volúmenes pequeños; integración HDMI o LVDS no estándar |
| **B'. Dongle o caja retail (Carlinkit, Ottocast) y app AutoKit en la Radxa** | La caja hace el protocolo y el Android muestra el video | **Gris:** no hay evidencia pública de certificación [I] | Retail de decenas a cientos de USD por unidad [I, sin fuente de precio] | Rápido; además trae CarPlay (Hudiy y pi-carplay lo usan así) | Depende de un tercero opaco; se rompe con las actualizaciones; no se puede anunciar como "Android Auto" |
| **C. App propia en el celular y BLE al tablero** (modelo TVS SmartXonnect, Royal Enfield Tripper, Honda RoadSync) | La app calcula la ruta y envía maniobras, llamadas y notificaciones por BLE; el tablero dibuja | **Plena** | Desarrollo propio. Navigation SDK de Google Maps: pago por destino, **1.000 destinos al mes gratis** y precio rebajado a la mitad o más en 2025. Alternativas: Mapbox o HERE (pagos), OSM con GraphHopper, Valhalla u OsmAnd (gratis; OsmAnd es GPL) [H precios Google] | Sin dependencia de Google; diferenciación (modo moto, consumo, OBD); funciona con iPhone | Mucho más trabajo; no tiene apps de terceros como Spotify o WhatsApp en pantalla, salvo las que implementes |
| **C'. Leer las notificaciones de navegación de Google Maps o Waze en el celular** (NotificationListener) y enviar flechas por BLE | El usuario navega con Google Maps en el teléfono y tu app reenvía la maniobra | Gris claro: no elude nada, pero depende del formato de las notificaciones; revisar los términos de Google Maps [I] | Bajo | Maps y Waze con modo moto en Colombia, sin licencias | Frágil ante cambios de formato; solo flechas y distancias |
| **D. Android Automotive OS en el tablero** | El tablero corre AAOS | AOSP es libre (Apache 2.0). **GAS** (Maps, Play, Assistant) exige acuerdo con Google, certificación y requisitos de hardware del CDD | AOSP gratis; GAS con costos no públicos y solo para OEM [H] | Sin GAS: OS de auto libre | **No proyecta el celular**; sin GAS no hay Google Maps; portar AAOS al RK3566 es un proyecto grande [I] |
| **E. Mirroring genérico** (Miracast, scrcpy por Wi-Fi, MediaProjection con app propia) | Se duplica la pantalla del teléfono | Plena | Bajo | Simple | La UI del teléfono no está pensada para manejar; consumo y calor; retorno táctil complejo; mala experiencia y riesgo vial [I] |
| **F. Dispositivo Android abierto en el que el cliente instala Open Headunit desde Play** | Vendes hardware Android y el usuario instala la app | Gris [I]: reduce tu exposición frente a preinstalar, pero no anuncies "Android Auto" | Bajo | Aprovecha un proyecto muy activo | Soporte y roturas por actualizaciones; consultar a un abogado |

Nota sobre CarPlay: requiere el programa MFi de Apple con coprocesador de autenticación. Los proyectos abiertos (Hudiy, pi-carplay) lo delegan a un dongle Carlinkit. [H]

---

## 6. Recomendación concreta

### Piloto personal (uso propio)
1. Instalar **Open Headunit / Headunit Revived** (AGPL-3.0) en la Radxa con Android 11, desde Play o compilado desde GitHub, en modo **Native (Wi-Fi Direct o hotspot del head unit)**. Antes, comprobar que el módulo D8 de la Radxa levanta 5 GHz y que el BT funciona: hay reportes de BT inestable con ese módulo en otras imágenes. [H/I]
2. Si el teléfono tiene Android Auto 17.4 o superior y falla el arranque inalámbrico, usar las salidas del README: modo nativo, "Headunit Server" en modo desarrollador, o un dongle USB inalámbrico de Android Auto conectado al puerto host de la Radxa. [H]
3. Mantener los instrumentos vitales (velocidad, RPM, testigos) en una capa propia que no dependa de Android Auto: si Google rompe la proyección, el tablero sigue siendo legal y usable. [I]
4. Usar Waze o Sygic en modo moto dentro de Android Auto, y Google Maps con modo moto en Colombia. Usar solo comandos de voz o botones mientras se rueda: el Código de Tránsito colombiano (art. 131, C.38) sanciona usar dispositivos móviles al manejar salvo manos libres. [H]
5. Riesgo legal práctico para uso propio: bajo. [I]

### Si se distribuye o vende hardware con RevDash
1. **No vender** equipos con aasdk, openauto u Open Headunit preinstalados usando el certificado de JVC Kenwood o el de referencia de Google, y **no anunciar "Android Auto"**. Es el escenario de mayor riesgo legal y de soporte. [I]
2. Hacer del **núcleo diferencial** el tablero propio (CAN/OBD/sensores, alertas, modo moto) y una **app propia en el celular por BLE** (opción C), con navegación de Google Maps Navigation SDK (gratis hasta 1.000 destinos al mes) o una pila OSM. Es lo que hacen TVS y Royal Enfield, y no depende de la buena voluntad de Google. [I con precedentes H]
3. Si el mercado exige Android Auto o CarPlay, ofrecerlo **solo mediante un módulo certificado de un tercero** (opción B), pidiendo prueba documental de la certificación de Google y Apple. Como camino transitorio, vender hardware Android abierto sin preinstalar ni anunciar la app (opción F), solo después de consultar a un abogado de PI en Colombia (Ley 1915 de 2018 y marcas) y de revisar el Estatuto del Consumidor (Ley 1480 de 2011). [I]
4. Con volumen (cientos o miles de unidades al año), pedir contacto con Google por medio de un laboratorio 3PL o de un Tier-1 que haga de puente. [I]
5. Fuera del alcance pero pendiente: homologación del tablero como reemplazo del original (revisión técnico-mecánica) y homologación de radios BT/Wi-Fi para la venta en Colombia. No los investigué. [I]

---

## Fuentes

### Google, programa y certificación
- Android for Cars (Google): https://developers.google.com/cars
- Lista oficial de compatibilidad de Android Auto: https://www.android.com/auto/compatibility/
- Ayuda de Google, Android Auto inalámbrico: https://support.google.com/androidauto/answer/6348029?hl=en
- Desktop Head Unit (Google): https://developer.android.com/training/cars/testing/dhu
- Términos de producto de Android Auto (Transparency Center): https://transparency.google/our-policies/product-terms/android-auto
- Reglas de marca de Android: https://developer.android.com/distribute/tools/promote/brand
- Harman, certificación AAP y 3PL: https://www.businesswire.com/news/home/20211004005194/en/HARMAN-Receives-Third-Android-Auto-Projection-AAP-Certification-Expanding-Capabilities-to-Deliver-Connected-Vehicle-Displays-Across-Europe
- Concept Reply, certificación CarPlay y Android Auto: https://www.reply.com/concept-reply/en/carplay-androidauto-certification
- Symbio, 3PL: https://www.symbio.com/?p=557
- Android Headlines, nuevos partners de receptores (2018): https://www.androidheadlines.com/2018/03/google-lines-five-new-partners-android-auto-receivers.html
- Harman Ready Ride (2026): https://www.automotiveworld.com/news/harman-launches-ready-ride-two-wheeler-platform/
- Vacante de Hero MotoCorp (certificación AA/CarPlay): https://jobs.heromotocorp.com/job/Jaipur-Android-Cluster-Infotainment-Design-RJ-302028/1163881901
- Luxoft, GAS o no GAS: https://www.luxoft.com/blog/gas-or-not-gas-optimizing-aaos-for-automotive-ivi-systems
- Programa MFi de Apple: https://mfi.apple.com/en/how-it-works

### Certificados y protocolo
- AARE, certificates.md: https://raw.githubusercontent.com/prototux/AARE/master/docs/certificates.md
- Flywheel wiki, Certificates and Keys: https://github.com/Benjamin-Wiegand/Flywheel/wiki/Certificates-and-Keys
- aasdk (opencardev), carpeta cert: https://github.com/opencardev/aasdk/tree/HEAD/cert
- Open Headunit, README y certificado: https://github.com/andreknieriem/open-headunit

### Motos
- Honda Africa Twin con Android Auto: https://www.cyclenews.com/2021/02/article/honda-to-bring-android-auto-integration-to-africa-twin/
- Honda Canadá, comunicado: https://hondanews.ca/en-CA/hci-power/releases/release-e2de67a7e51cddf6ecf2e2896877622f047fcdb0-honda-int-gre-android-auto-l-africa-twin
- Harley-Davidson con Android Auto: https://motorcyclecruiser.com/story/news/harley-announces-android-auto-support
- SlashGear, 12 motos con CarPlay o Android Auto (jul. 2025): https://www.slashgear.com/1899644/motorcycles-with-carplay-android-auto
- Indian, CarPlay en RIDE COMMAND: https://www.indianmotorcycle.com/en-us/self-help/article/KA-04588/
- KTM con Android Automotive: https://www.autoevolution.com/news/what-android-auto-google-s-upgraded-experience-launches-on-motorcycles-241304.html
- Teardown de Android Auto 13.5 (ícono TVS): https://www.androidpolice.com/android-auto-support-motorcycle-gemini-higher-resolution/
- Android Authority, mismo teardown: https://androidauthority.com/android-auto-motorcycle-support-apk-3513842/
- Sygic Motorbike Mode (jul. 2026): https://www.androidauthority.com/android-auto-sygic-motorbike-mode-rollout-3684971/
- 9to5Google, mismo lanzamiento: https://9to5google.com/2026/07/07/android-auto-motorcycle-navigation-app/
- Waze, tipo de vehículo: https://support.google.com/waze/answer/7579259?hl=en
- Google Maps con modo moto en Colombia: https://www.enter.co/chips-bits/apps-software/google-maps-ahora-tiene-modo-motocicletas-en-colombia
- Waze Moto en Colombia (jul. 2026): https://www.publimetro.co/autos-motos/2026/07/13/waze-estrena-modo-motocicleta-en-colombia-y-promete-rutas-mas-inteligentes-para-dos-ruedas/
- TVS, TFT de la Apache RTR 160 4V con Google Maps (oct. 2026): https://www.tvsmotor.com/Media/Blog/tvs-apache-rtr-160-4v-tft-display-google-maps
- TVS SmartXonnect: https://www.tvsmotor.com/Media/Press-Release/tvs-motor-company-launches-2021-tvs-apache-rtr-160-4v-with-bluetooth-enabled-tvs-smartxonnect-in-bangladesh
- Royal Enfield Tripper (Wikipedia): https://en.wikipedia.org/wiki/Tripper_navigation_system
- Royal Enfield Tripper Dash: https://www.royalenfield.com/in/en/tripper-dash
- Bosch, pantalla dividida para motos y mySPIN: https://www.bosch-mobility.com/en/company/current-news/integrated-split-screen-for-motorcycles/

### Aftermarket
- Aoocci, ¿permite Google Android Auto en motos? [V]: https://aoocci.com/blogs/motorcycle/google-allow-android-auto-motorcycle
- 1000PS, prueba del Chigee AIO-5 Play: https://www.1000ps.com/en-us/article/3012284/android-auto-and-apple-carplay-for-bmw-chigee-aio-5-play-reviewed
- Carpuride W702: https://carpuride.com/products/carpuride-w702-wireless-portable-dual-bluetooth-waterproof-ip67-motorcycle-stereo
- Ultimate Motorcycling, Chigee vs Carpuride: https://ultimatemotorcycling.com/2024/08/29/chigee-aio-5-play-vs-carpuride-w702b-comparison/
- Experiencias con Chigee y Carpuride (foro): https://mracommunity.nodebb.com/post/56110
- Zlink: https://cartechstudio.com/blogs/in-vehicle-apps/zlink-android-auto
- Team-BHP, alternativas con licencia: https://www.team-bhp.com/forum/car-entertainment/210960-android-head-unit-buying-guide-26.html
- Carlinkit CPC200-CCPA: https://carlinkitcarplay.com/collections/all/products/carlinkit-wirless-adapter-for-android-head-unit-support-carplay-and-android-auto
- Hudiy, comunidad: https://hudiy.eu/community/viewtopic.php?p=337
- Android Auto 17.2 y unidades no certificadas: https://www.androidheadlines.com/2026/07/google-rolls-out-fix-for-android-autos-recent-crashing-issues.html
- Android Authority, parche de 17.2: https://androidauthority.com/android-auto-fix-3688012
- Autoevolution, versiones 17.2 y 17.3: https://www.autoevolution.com/news/google-releases-emergency-android-auto-update-for-versions-172-and-173-272955.html
- Gadget Hacks, preguntas abiertas sobre 17.2: https://android.gadgethacks.com/news/android-auto-172-crash-fix-patched-builds-and-open-questions/

### Proyectos abiertos y no oficiales
- Crankshaft: https://github.com/opencardev/crankshaft
- openauto (opencardev): https://github.com/opencardev/openauto
- openauto original (f1xpl): https://github.com/f1xpl/openauto
- OpenAuto Pro, intento de revivirlo: https://github.com/mrmees/openauto-pro-community
- Hudiy: https://github.com/wiboma/hudiy
- Hudiy, FAQ: https://hudiy.eu/faq/
- aa-proxy-rs: https://github.com/aa-proxy/aa-proxy-rs
- pi-carplay: https://github.com/RGreeneRI/pi-carplay
- Headunit original (Michael Reid): https://github.com/mikereidis/headunit
- Headunit Reloaded: https://similarweb.com/app/google/gb.xxy.hr

### Navegación y SDKs
- Google, nuevos precios del Navigation SDK: https://mapsplatform.google.com/resources/blog/making-navigation-sdks-more-accessible-with-new-pricing/
- Google Maps Platform, resumen de precios: https://developers.google.com/maps/billing/overview/

### Hardware
- Radxa Zero 3W, anuncio oficial: https://radxa.com/blog/ZERO-3W-Official-Announcement
- Radxa Wireless Module D8: https://techinfodepot.shoutwiki.com/wiki/Radxa_Wireless_Module_D8
- Notebookcheck, actualización de BT y Wi-Fi de la Zero 3W: https://www.notebookcheck.net/Radxa-Zero-3W-Single-board-computer-now-available-with-improved-capabilities-thanks-to-Bluetooth-and-Wi-Fi-upgrades.780686.0.html

### Colombia
- Ley 1915 de 2018 (texto): https://normograma.mintic.gov.co/mintic/compilacion/docs/ley_1915_2018.htm
- Forvis Mazars, comentario a la Ley 1915 de 2018: https://www.forvismazars.com/co/es/acerca-de-nosotros/noticias-publicaciones-y-media/nuestras-noticias-y-publicaciones/actualidad-juridica-y-tributaria/2018/ley-1915-de-2018
- Celular al manejar, C.38 (El País): https://www.elpais.com.co/economia/movilidad-de-cuanto-es-la-multa-que-se-debe-pagar-por-conducir-utilizando-el-celular-3017.html

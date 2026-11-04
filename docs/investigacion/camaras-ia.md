# Cámaras delantera y trasera: dashcam y detección con la NPU

Fecha: 2026-11-04. Marcas: **[H]** hecho con fuente, **[I]** inferencia mía o dato por verificar en la placa.

## Resumen

- La ROCK 4D tiene **dos entradas CSI** (una de 4 carriles con conector de 31 pines y 0,3 mm, y otra de 2
  carriles con conector de 15 pines y 1 mm, compatible con Raspberry Pi) y una **NPU de 6 TOPS** [H]. Alcanza
  para grabar dos cámaras y correr un detector pequeño sobre las dos [I].
- **La cámara trasera no puede ir por CSI.** La cinta CSI mide del orden de 15-30 cm y es frágil con
  vibración; la cola de la moto queda a ~1,5 m del tablero [I]. Propuesta: delantera por CSI (junto al
  tablero) y trasera **USB (UVC) a prueba de agua**, cuyo cable sí llega a varios metros [I].
- Lo que más vale para seguridad en moto es **el aviso de vehículo acercándose por detrás o en el punto
  ciego**, después el aviso de choque frontal y en tercer lugar leer el **límite de velocidad**. El
  semáforo aporta poco en moto: queda como dato, no como alerta.
- Hay un producto comercial que valida la idea: **Ride Vision** (Israel) usa dos cámaras gran angular
  (delante y detrás), una unidad de cómputo y LEDs en los espejos; avisa de choque frontal, punto ciego (de
  6 a 10 pies, por encima de 12 mph) y choque trasero, y afirma que la cámara funciona mejor que el radar
  con la moto inclinada [H].
- **Licencias:** Ultralytics YOLO es AGPL-3.0, compatible con RevDash (AGPL) pero no con software cerrado
  [H]. Los datasets públicos de señales son de uso no comercial o de investigación (TT100K CC BY-NC 2.0,
  GTSDB sin licencia explícita, Mapillary gratis solo para investigación con edición comercial aparte) [H].
  Para señales colombianas hace falta **dataset propio**, publicable con licencia libre.
- Restricciones del spec que aplican: **sin modo parqueo** (consumo cero con la llave en OFF) y **sin
  corrupción por corte de llave**. Por eso se graba en segmentos cortos que sobreviven a un corte.

## Qué dice la comunidad (último mes, /last30days)

La conversación de motociclistas en Reddit, YouTube y TikTok está en **dashcams comerciales "tontas"**
(1080p o 4K, delantera y trasera, IP67, grabación en bucle), no en detección. Un comentario de YouTube resume la
demanda sin cubrir: «qué bueno sería ver lo de atrás desde una pantalla adelante mientras manejas para
quitarte si alguien no te está viendo». En r/motorcycles hay desconfianza general hacia "la IA". Lo que
más se lee es que si lo usamos tiene que ser una ayuda discreta y confiable, no un juguete. En el último
mes no apareció nada sobre RK3576 con dashcam. La evidencia técnica sale de fabricantes (ver abajo).
Datos crudos: `~/Documents/Last30Days/motorcycle-dashcam-ai-...-raw-v3.md`.

## Hardware

| Pieza | Propuesta | Por qué |
|---|---|---|
| Cámara delantera | CSI de 4 carriles: Radxa Camera 4K (IMX415) o 13M 214 (IMX214), las dos de la lista de cámaras soportadas de la ROCK 4D [H] | El ISP de la placa procesa la imagen; latencia baja. La 13M 214 y la 8M 219 figuran con "color de vista previa débil" [H]; la 4K es la candidata |
| Cámara trasera | USB UVC IP67 gran angular 1080p, cable a la cola por dentro del chasis [I] | CSI no llega. USB 2.0 alcanza 5 m. Alternativa: cámara AHD analógica + decodificador (más piezas) |
| Almacenamiento | microSD High Endurance (ya prevista en el spec) o NVMe por el conector PCIe FPC [I] | Escritura continua de video |
| Térmica | Medir dos codificadores + NPU dentro de la carcasa sellada [I] | El RK3576 comercial opera de 0 a 60 °C [H]; la gestión térmica del spec ya baja brillo |

Verificar en la placa: que la cámara 4K enumere en Android 14 (Camera2 HAL), que una cámara UVC funcione al mismo
tiempo que la CSI, y que el codificador aguante dos flujos (1080p30 + 1080p30) sin bajar fps.

## Funciones, de más a menos valor

1. **Vehículo acercándose por detrás o en el punto ciego** (cámara trasera). Detectar carros y motos,
   seguirlos entre cuadros y calcular el tiempo al choque por cómo crece la caja. Aviso en el borde de la
   pantalla del lado de la amenaza.
2. **Choque frontal** (cámara delantera). La misma técnica, con umbral por velocidad propia (del CAN).
3. **Límite de velocidad.** Leer la señal circular de borde rojo (en Colombia la SR-30 del Manual de
   Señalización Vial [I]) y avisar solo si la velocidad la supera. Necesita dataset propio.
4. **Baches.** Cruce con PotholeSync de RevScope; mejor por acelerómetro que por cámara [I].
5. **Semáforo.** La clase "traffic light" de COCO más un clasificador de color simple. Valor bajo en
   moto: solo como información, nunca como alerta.

Clips protegidos (no se sobrescriben en el bucle): frenada fuerte (derivada de la velocidad del CAN),
caída detectada por RevScope (vía BLE) o un toque en la pantalla.

## Modelos y licencias

| Opción | Licencia | Nota |
|---|---|---|
| Ultralytics YOLOv8/11 | AGPL-3.0 [H] | Sirve para el proyecto libre; software cerrado necesitaría la licencia Enterprise |
| YOLOX, PP-YOLOE, RTMDet | Apache-2.0 [I] | Alternativas sin copyleft; confirmar cuáles trae `rknn_model_zoo` para RK3576 |
| Pesos entrenados en COCO | Anotaciones CC BY 4.0, imágenes con términos de Flickr [I] | COCO ya tiene carro, moto, bus, camión, persona, bicicleta, semáforo y señal de pare |

Rendimiento reportado por fabricantes en el RK3576 (sirve de punto de partida, no de garantía) [H]:
~80 fps con YOLO11 a 640×640 solo en la NPU (Circuit Rocks), ~30 fps de punta a punta con YOLOv8 y cámara
USB (Forlinx). El RK3588 rinde más con varias transmisiones simultáneas por su bus de memoria más ancho (Geniatech).
Con dos cámaras basta con 10-15 fps de detección por cámara [I].

Datasets de señales: TT100K (China, CC BY-NC 2.0), GTSDB (Alemania, investigación), Mapillary MTSD
(global, gratis para investigación, edición comercial aparte) [H]. Ninguno trae señales colombianas en
cantidad [I].

## Software en Android

```
Camera2 (CSI) ─┬─► MediaCodec H.265 ─► segmentos de 1 min (MP4 fragmentado) ─► bucle en microSD
UVC (trasera) ─┤
               └─► ImageReader 640 px ─► RKNN runtime (JNI, librknnrt) ─► seguimiento IoU ─► reglas de alerta ─► UI
```

- **MP4 fragmentado o MPEG-TS** por segmento: un corte de llave pierde a lo sumo el final del segmento en
  curso; el resto queda reproducible.
- RKNN en una app normal de Android: Rockchip publica demos de Android en `rknn_model_zoo`; falta confirmar
  en la ROCK 4D que una app sin privilegios abre la NPU [I].
- La detección corre en un servicio aparte de la UI del tablero; si se cae, el tablero sigue (regla del
  spec: el tablero nunca depende de otra cosa para velocidad y testigos).

## Seguridad, UX y legal

- Las alertas son ayudas, no decisiones. Nunca tapan la velocidad ni las direccionales. Parpadeo por
  debajo de 3 Hz (fotosensibilidad). Sin sonido por defecto: el audio va al intercomunicador y es del
  dueño.
- Antes de avisar en ruta, validar falsos positivos con grabaciones reales en modo sombra (se registra pero no
  se muestra).
- Colombia: grabar la vía pública desde el vehículo propio es lícito en general; compartir video con caras
  o placas toca la Ley 1581 de datos personales [I]. Todo local, sin nube.

## Lo que se puede adelantar sin la placa

1. **Grabar rutas con el celular en el soporte** (video delantero a 1080p). Sirve para probar detectores en
   el PC y para empezar el dataset de señales colombianas.
2. Pipeline en el PC: exportar el modelo a ONNX, convertir con `rknn-toolkit2` (x86 Linux o WSL2) y medir
   precisión sobre esos videos.
3. En la app RevDash: la capa de alertas y el indicador de la dashcam, alimentados por datos simulados (ver
   `docs/diseno/sistema-visual.md`).

## Compras cuando llegue la placa

Radxa Camera 4K (confirmar precio y disponibilidad), cámara USB UVC IP67 gran angular para la trasera, y una
microSD High Endurance de 128 GB o más. No comprar nada de esto antes de verificar la ROCK 4D.

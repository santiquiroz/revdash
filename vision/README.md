# Visión: herramienta de evaluación en el PC

Mide qué detecta un modelo COCO (YOLOX-s) sobre videos de ruta grabados con el
celular: vehículos, personas, semáforos y señal de pare, tiempo al choque y
falsos positivos. Sirve para decidir antes de convertir nada a RKNN.

Advertencia: es una herramienta de evaluación, no la detección de la moto.

## Instalación (venv fuera del repo)

El venv vive fuera del repo (la carpeta del repo cuelga a pip):

```bat
py -3.12 -m venv %LOCALAPPDATA%\revdash-vision\venv
%LOCALAPPDATA%\revdash-vision\venv\Scripts\activate
pip install -r vision/requirements.txt
```

En este PC (RX 7800 XT) la inferencia va por `onnxruntime-directml`; en otra
máquina queda `onnxruntime` de CPU (ver `vision/requirements.txt`).

## Modelo

YOLOX-s oficial de Megvii (Apache-2.0), publicado en
`https://github.com/Megvii-BaseDetection/YOLOX/releases/download/0.1.1rc0/yolox_s.onnx`
(36 MB). No se versiona: se descarga a `vision/modelos/` (ignorado por git):

```bat
curl -L -o vision/modelos/yolox_s.onnx https://github.com/Megvii-BaseDetection/YOLOX/releases/download/0.1.1rc0/yolox_s.onnx
```

## Uso

```bat
python vision/analizar_video.py VIDEO --modelo vision/modelos/yolox_s.onnx [--salida DIR] [--cada N] [--camara delantera|trasera]
```

Procesa uno de cada N cuadros (por defecto 2) y escribe en DIR:

- `<nombre>_anotado.mp4`: cajas con clase, id y TTC (rojo si < 3 s).
- `<nombre>_detecciones.csv`: `t_s,id,clase,puntaje,x1,y1,x2,y2,ttc_s,lado,color_semaforo`.

Al final imprime cuadros/s de inferencia, conteo por clase y cuántas veces hubo
TTC < 3 s. `--camara trasera` solo cambia la etiqueta de alerta ("ATRÁS").

## Pruebas

Sin modelo ni GPU:

```bat
python -m pytest vision/tests
```

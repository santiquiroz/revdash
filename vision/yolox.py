import sys
from dataclasses import dataclass

import cv2
import numpy as np

CLASES_INTERES = {
    0: "persona",
    1: "bicicleta",
    2: "carro",
    3: "moto",
    5: "bus",
    7: "camion",
    9: "semaforo",
    11: "pare",
}


@dataclass(frozen=True)
class Deteccion:
    clase: str
    puntaje: float
    caja: tuple[float, float, float, float]


def letterbox(imagen_bgr, tamano=640):
    alto, ancho = imagen_bgr.shape[:2]
    escala = tamano / max(alto, ancho)
    nuevo_alto, nuevo_ancho = round(alto * escala), round(ancho * escala)
    reducida = cv2.resize(imagen_bgr, (nuevo_ancho, nuevo_alto), interpolation=cv2.INTER_LINEAR)
    lienzo = np.full((tamano, tamano, 3), 114, dtype=np.uint8)
    lienzo[:nuevo_alto, :nuevo_ancho] = reducida
    return lienzo.transpose(2, 0, 1)[None].astype(np.float32), escala


def decodificar(salida, tamano=640, strides=(8, 16, 32)):
    cruda = np.asarray(salida, dtype=np.float64)
    if cruda.ndim == 3:
        cruda = cruda[0]
    partes = []
    inicio = 0
    for paso in strides:
        celdas = tamano // paso
        fin = inicio + celdas * celdas
        bloque = cruda[inicio:fin].copy()
        rejilla_x, rejilla_y = np.meshgrid(np.arange(celdas), np.arange(celdas))
        bloque[:, 0] = (bloque[:, 0] + rejilla_x.reshape(-1)) * paso
        bloque[:, 1] = (bloque[:, 1] + rejilla_y.reshape(-1)) * paso
        bloque[:, 2] = np.exp(bloque[:, 2]) * paso
        bloque[:, 3] = np.exp(bloque[:, 3]) * paso
        partes.append(bloque)
        inicio = fin
    return np.concatenate(partes, axis=0)


def nms(cajas_xyxy, puntajes, umbral_iou=0.45):
    cajas = np.asarray(cajas_xyxy, dtype=np.float64)
    notas = np.asarray(puntajes, dtype=np.float64)
    orden = np.argsort(-notas, kind="stable")
    elegidas = []
    while orden.size:
        primera = orden[0]
        elegidas.append(primera)
        if orden.size == 1:
            break
        resto = cajas[orden[1:]]
        inter_x1 = np.maximum(cajas[primera, 0], resto[:, 0])
        inter_y1 = np.maximum(cajas[primera, 1], resto[:, 1])
        inter_x2 = np.minimum(cajas[primera, 2], resto[:, 2])
        inter_y2 = np.minimum(cajas[primera, 3], resto[:, 3])
        inter = np.maximum(0.0, inter_x2 - inter_x1) * np.maximum(0.0, inter_y2 - inter_y1)
        area_p = (cajas[primera, 2] - cajas[primera, 0]) * (cajas[primera, 3] - cajas[primera, 1])
        area_r = (resto[:, 2] - resto[:, 0]) * (resto[:, 3] - resto[:, 1])
        traslape = inter / np.maximum(area_p + area_r - inter, 1e-9)
        orden = orden[1:][traslape <= umbral_iou]
    return np.array(elegidas, dtype=np.intp)


def detectar(salida_decodificada, escala, umbral=0.35, clases=CLASES_INTERES):
    filas = np.asarray(salida_decodificada, dtype=np.float64)
    cajas_por_clase = {}
    puntajes_por_clase = {}
    for fila in filas:
        cx, cy, ancho, alto, objeto = fila[:5]
        indice = int(np.argmax(fila[5:]))
        if indice not in clases:
            continue
        puntaje = float(objeto * fila[5 + indice])
        if puntaje < umbral:
            continue
        caja = (
            (cx - ancho / 2) / escala,
            (cy - alto / 2) / escala,
            (cx + ancho / 2) / escala,
            (cy + alto / 2) / escala,
        )
        cajas_por_clase.setdefault(indice, []).append(caja)
        puntajes_por_clase.setdefault(indice, []).append(puntaje)
    hallazgos = []
    for indice, cajas in cajas_por_clase.items():
        puntajes = puntajes_por_clase[indice]
        for elegida in nms(cajas, puntajes):
            x1, y1, x2, y2 = cajas[int(elegida)]
            hallazgos.append(
                Deteccion(
                    clase=clases[indice],
                    puntaje=puntajes[int(elegida)],
                    caja=(float(x1), float(y1), float(x2), float(y2)),
                )
            )
    hallazgos.sort(key=lambda d: d.puntaje, reverse=True)
    return hallazgos


def crear_sesion(ruta_modelo):
    import onnxruntime

    if "DmlExecutionProvider" in onnxruntime.get_available_providers():
        proveedores = ["DmlExecutionProvider", "CPUExecutionProvider"]
    else:
        proveedores = ["CPUExecutionProvider"]
    sesion = onnxruntime.InferenceSession(str(ruta_modelo), providers=proveedores)
    print(f"proveedor: {sesion.get_providers()}", file=sys.stderr)
    return sesion

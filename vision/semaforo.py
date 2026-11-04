import cv2
import numpy as np


def color_semaforo(recorte_bgr):
    if recorte_bgr is None or recorte_bgr.size == 0:
        return None
    hsv = cv2.cvtColor(recorte_bgr, cv2.COLOR_BGR2HSV)
    matiz = hsv[:, :, 0].astype(np.int16)
    vivos = (hsv[:, :, 1] > 100) & (hsv[:, :, 2] > 100)
    conteos = {
        "rojo": int((((matiz <= 10) | (matiz >= 160)) & vivos).sum()),
        "amarillo": int((((matiz >= 20) & (matiz <= 35)) & vivos).sum()),
        "verde": int((((matiz >= 40) & (matiz <= 85)) & vivos).sum()),
    }
    mejor = max(conteos, key=lambda c: conteos[c])
    if conteos[mejor] <= 0.03 * matiz.size:
        return None
    return mejor

import sys
from pathlib import Path

import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from semaforo import color_semaforo


def _recorte(matiz):
    hsv = np.full((20, 20, 3), (matiz, 200, 200), dtype=np.uint8)
    return cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)


def test_rojo_con_matiz_alto():
    assert color_semaforo(_recorte(175)) == "rojo"


def test_amarillo():
    assert color_semaforo(_recorte(28)) == "amarillo"


def test_verde():
    assert color_semaforo(_recorte(60)) == "verde"


def test_gris_da_none():
    gris = np.full((20, 20, 3), 128, dtype=np.uint8)
    assert color_semaforo(gris) is None

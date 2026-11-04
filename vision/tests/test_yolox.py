import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import yolox
from yolox import CLASES_INTERES


def test_letterbox_escala_y_relleno():
    imagen = np.zeros((720, 1280, 3), dtype=np.uint8)
    tensor, escala = yolox.letterbox(imagen)
    assert tensor.shape == (1, 3, 640, 640)
    assert tensor.dtype == np.float32
    assert escala == pytest.approx(0.5)
    assert (tensor[0, :, 0, 0] == 0).all()
    assert (tensor[0, :, 500, 0] == 114).all()


def test_decodificar_una_celda_conocida():
    salida = np.zeros((4, 85), dtype=np.float64)
    salida[1, 4] = 1.0
    salida[1, 5] = 1.0
    decod = yolox.decodificar(salida, tamano=32, strides=(16,))
    assert decod.shape == (4, 85)
    assert decod[1, 0] == pytest.approx(16.0)
    assert decod[1, 1] == pytest.approx(0.0)
    assert decod[1, 2] == pytest.approx(16.0)
    assert decod[1, 3] == pytest.approx(16.0)


def test_nms_elimina_solapada():
    cajas = [[0, 0, 10, 10], [1, 1, 11, 11], [50, 50, 60, 60]]
    elegidas = set(int(i) for i in yolox.nms(cajas, [0.9, 0.8, 0.7]))
    assert elegidas == {0, 2}


def test_detectar_filtra_clases_y_deshace_escala():
    filas = np.zeros((3, 85), dtype=np.float64)
    filas[0, :5] = [100, 100, 40, 40, 0.9]
    filas[0, 5 + 2] = 0.9
    filas[1, :5] = [100, 100, 40, 40, 0.9]
    filas[1, 5 + 4] = 0.9
    filas[2, :5] = [300, 300, 40, 40, 0.1]
    filas[2, 5 + 2] = 0.9
    hallazgos = yolox.detectar(filas, escala=0.5)
    assert len(hallazgos) == 1
    assert hallazgos[0].clase == CLASES_INTERES[2] == "carro"
    assert hallazgos[0].caja == pytest.approx((160.0, 160.0, 240.0, 240.0))

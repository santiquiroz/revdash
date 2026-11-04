import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from seguimiento import Pista, actualizar, lado, tiempo_al_choque
from yolox import Deteccion


def _pista(id=0, clase="carro", caja=(10, 10, 50, 60), t=0.0):
    return Pista(id, clase, caja, ((t, caja[3] - caja[1]),))


def test_misma_caja_conserva_id():
    pistas, _ = actualizar((_pista(),), [Deteccion("carro", 0.9, (12, 12, 52, 62))], 0.1)
    assert [p.id for p in pistas] == [0]


def test_otra_clase_no_empareja():
    pistas, proximo = actualizar((_pista(),), [Deteccion("persona", 0.9, (12, 12, 52, 62))], 0.1, siguiente_id=1)
    assert sorted(p.id for p in pistas) == [0, 1]
    assert proximo == 2


def test_pista_desaparece_tras_mas_de_medio_segundo():
    pistas, _ = actualizar((_pista(),), [], 0.6)
    assert pistas == ()


def test_tiempo_al_choque_creciendo():
    historial = tuple((round(i * 0.1, 1), 100 + i * 4) for i in range(6))
    pista = Pista(0, "carro", (0, 0, 50, 120), historial)
    assert tiempo_al_choque(pista) == pytest.approx(3.0, abs=0.3)


def test_tiempo_al_choque_achicando_da_none():
    historial = tuple((round(i * 0.1, 1), 120 - i * 4) for i in range(6))
    pista = Pista(0, "carro", (0, 0, 50, 100), historial)
    assert tiempo_al_choque(pista) is None


def test_lado_por_tercios():
    assert lado((0, 0, 10, 10), 90) == "IZQUIERDA"
    assert lado((40, 0, 50, 10), 90) == "CENTRO"
    assert lado((80, 0, 90, 10), 90) == "DERECHA"

import struct

import pytest

from tools.espejo_receptor import (
    empaquetar_toque,
    leer_cabecera,
    leer_encabezado_paquete,
    leer_exacto,
    percentiles,
)


class _SocketFalso:
    def __init__(self, trozos):
        self._trozos = list(trozos)

    def recv(self, n):
        if not self._trozos:
            return b""
        return self._trozos.pop(0)[:n]


def test_cabecera_valida():
    assert leer_cabecera(bytes.fromhex("52 44 45 31 05 00 02 d0")) == (1280, 720)


def test_cabecera_rechaza_magia_largo():
    with pytest.raises(ValueError):
        leer_cabecera(bytes.fromhex("58 44 45 31 05 00 02 d0"))
    with pytest.raises(ValueError):
        leer_cabecera(bytes.fromhex("52 44 45 31 05 00 02"))
    with pytest.raises(ValueError):
        leer_cabecera(bytes.fromhex("52 44 45 31 05 00 02 d0 00"))


def test_encabezado_paquete_valido():
    assert leer_encabezado_paquete(bytes.fromhex("02 00000000000003e8 0000000a")) == (2, 1000, 10)


def test_encabezado_paquete_rechaza_largo_y_tipo():
    with pytest.raises(ValueError):
        leer_encabezado_paquete(bytes.fromhex("02 00000000000003e8 000000"))
    with pytest.raises(ValueError):
        leer_encabezado_paquete(bytes.fromhex("05 00000000000003e8 0000000a"))


def test_empaquetar_toque_bytes_exactos():
    assert empaquetar_toque(0, 0.5, 0.25) == bytes.fromhex("00 3f000000 3e800000")


def test_empaquetar_toque_recorta():
    assert empaquetar_toque(1, -2.0, 3.0) == struct.pack(">Bff", 1, 0.0, 1.0)


def test_empaquetar_toque_rechaza_accion():
    with pytest.raises(ValueError):
        empaquetar_toque(3, 0.5, 0.5)


def test_percentiles_p50_p90():
    p50, p90 = percentiles([1, 2, 3, 4])
    assert p50 == pytest.approx(2.5)
    assert p90 == pytest.approx(3.7)


def test_percentiles_un_elemento_y_sin_orden():
    assert percentiles([7]) == (7.0, 7.0)
    assert percentiles([4, 1, 3, 2]) == percentiles([1, 2, 3, 4])


def test_percentiles_lista_vacia_falla():
    with pytest.raises(ValueError):
        percentiles([])


def test_leer_exacto_une_trozos():
    sock = _SocketFalso([b"RD", b"E1", b"\x05\x00\x02\xd0"])
    assert leer_exacto(sock, 8) == bytes.fromhex("52 44 45 31 05 00 02 d0")


def test_leer_exacto_falla_si_cierra():
    with pytest.raises(ConnectionError):
        leer_exacto(_SocketFalso([b"RD"]), 8)

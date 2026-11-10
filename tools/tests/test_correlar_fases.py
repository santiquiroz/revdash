import pytest

from tools.correlar_fases import Resultado, correlar, formatear, leer_captura


def _leer(lineas):
    return leer_captura(lineas)


def _marcas(ciclos=3, fase_ms=2000, nombre="freno-trasero"):
    lineas = [f"0 FASE OFF 0 {nombre}"]
    for ciclo in range(1, ciclos + 1):
        lineas.append(f"{(2 * ciclo - 1) * fase_ms} FASE ON {ciclo} {nombre}")
        lineas.append(f"{2 * ciclo * fase_ms} FASE OFF {ciclo} {nombre}")
    lineas.append(f"{(2 * ciclos + 1) * fase_ms} FASE FIN {ciclos} {nombre}")
    return lineas


def _linea_can(ms, value, ident="1F4"):
    return f"{ms} C,0,{ident},s,{value:02X}"


def _captura_can(valor_fase, ciclos=3, fase_ms=2000):
    lineas = _marcas(ciclos, fase_ms)
    for indice in range(2 * ciclos + 1):
        fase = "OFF" if indice % 2 == 0 else "ON"
        marca_ms = indice * fase_ms
        valor = valor_fase[fase]
        lineas.append(_linea_can(marca_ms + 1100, valor))
    return _leer(lineas)


def test_bit_que_alterna_en_tres_ciclos_se_reporta():
    marcas, muestras = _captura_can({"ON": 0x40, "OFF": 0x00})
    resultado = correlar(marcas, muestras, 0)

    assert ("1F4", 0, 6, 1, 0) in resultado.bits


def test_contador_que_cambia_dentro_de_fase_no_se_reporta():
    lineas = _marcas()
    for indice in range(7):
        fase = "OFF" if indice % 2 == 0 else "ON"
        base = indice * 2000 + 1100
        for paso in range(4):
            lineas.append(_linea_can(base + paso * 100, (indice + paso) % 4))
    marcas, muestras = _leer(lineas)
    resultado = correlar(marcas, muestras, 0)

    assert not resultado.bits
    assert not resultado.bytes


def test_bit_que_falla_un_ciclo_no_se_reporta():
    lineas = _marcas()
    valores_on = [0x40, 0x40, 0x00]
    for indice in range(7):
        fase = "OFF" if indice % 2 == 0 else "ON"
        valor = 0 if fase == "OFF" else valores_on[(indice - 1) // 2]
        lineas.append(_linea_can(indice * 2000 + 1100, valor))
    marcas, muestras = _leer(lineas)
    resultado = correlar(marcas, muestras, 0)

    assert not any(bit == 6 for _, _, bit, _, _ in resultado.bits)


def test_margen_descarta_valores_tardios_de_inicio_de_fase():
    lineas = _marcas()
    for indice in range(7):
        fase = "OFF" if indice % 2 == 0 else "ON"
        base = indice * 2000
        estable = 0x00 if fase == "OFF" else 0x40
        tardio = 0x40 if fase == "OFF" else 0x00
        lineas.append(_linea_can(base + 200, tardio))
        lineas.append(_linea_can(base + 1200, estable))
    marcas, muestras = _leer(lineas)
    resultado = correlar(marcas, muestras, 1000)

    assert ("1F4", 0, 6, 1, 0) in resultado.bits


def test_entrada_digital_con_flancos_y_arrastre_se_reporta():
    lineas = _marcas()
    niveles = [1, 0, 1, 0, 1, 0, 1]
    for indice, nivel in enumerate(niveles):
        lineas.append(f"{indice * 2000} E,0,3,{nivel}")
    marcas, muestras = _leer(lineas)
    resultado = correlar(marcas, muestras, 1000)

    assert (3, 0, 1) in resultado.entradas


def test_id_solo_en_ventanas_on_se_reporta():
    lineas = _marcas()
    for indice in range(7):
        fase = "OFF" if indice % 2 == 0 else "ON"
        if fase == "ON":
            lineas.append(f"{indice * 2000 + 1100} C,0,2B0,s,AA")
    marcas, muestras = _leer(lineas)

    assert correlar(marcas, muestras, 0).ids == ["2B0"]


def test_una_sola_ventana_on_lanza_value_error():
    lineas = _marcas(ciclos=1)
    marcas, muestras = _leer(lineas)

    with pytest.raises(ValueError, match="^faltan ciclos:"):
        correlar(marcas, muestras, 0)


def test_lineas_sin_prefijo_y_basura_se_ignoran():
    marcas, muestras = _leer([
        "sin tiempo FASE ON 1 accion",
        "100 basura",
        "200 FASE ON x accion",
        "300 C,1,1F4,s,GG",
        "400 E,0,1,8",
    ])

    assert marcas == []
    assert muestras == []


def test_formatear_resultado_vacio():
    resultado = Resultado(None, 2, 1.0, [], [], [], [])

    assert "ningún bit, byte ni entrada siguió todas las fases" in formatear(resultado)

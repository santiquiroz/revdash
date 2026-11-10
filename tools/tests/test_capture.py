import pytest

from tools.capture import _parse_args, mark_line, plan_fases


def test_plan_fases_con_dos_ciclos():
    assert plan_fases(2, 5.0) == [
        (0.0, "OFF", 0),
        (5.0, "ON", 1),
        (10.0, "OFF", 1),
        (15.0, "ON", 2),
        (20.0, "OFF", 2),
        (25.0, "FIN", 2),
    ]


def test_mark_line():
    assert mark_line(1700000000000, "ON", 1, "freno") == "1700000000000 FASE ON 1 freno"


@pytest.mark.parametrize(
    "opciones",
    [
        ["--guion", "a b"],
        ["--ciclos", "1"],
        ["--fase-s", "0.5"],
        ["--fase-s", "nan"],
    ],
)
def test_parse_args_rechaza_opciones_invalidas(opciones):
    with pytest.raises(SystemExit):
        _parse_args(["--port", "COM6", "--out", "captura.rdp", *opciones])


def test_parse_args_acepta_opciones_validas():
    args = _parse_args([
        "--port", "COM6", "--out", "captura.rdp", "--guion", "freno-trasero",
        "--ciclos", "3", "--fase-s", "2.5",
    ])

    assert (args.guion, args.ciclos, args.fase_s) == ("freno-trasero", 3, 2.5)

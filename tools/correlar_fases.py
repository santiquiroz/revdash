import argparse
import re
import sys
from collections.abc import Iterable
from dataclasses import dataclass
from pathlib import Path

try:
    from tools.diff_can import split_data
    from tools.rdp import Frame, parse_line
except ImportError:
    from diff_can import split_data
    from rdp import Frame, parse_line

_PC_LINE = re.compile(r"^(\d+) (.*)$")
_MARK = re.compile(r"^FASE (OFF|ON|FIN) (\d+)(?: (\S+))?$")
_HEX = re.compile(r"^[0-9A-Fa-f]*$")


@dataclass(frozen=True)
class Marca:
    ms: int
    fase: str
    ciclo: int
    nombre: str | None


@dataclass(frozen=True)
class Muestra:
    ms: int
    frame: Frame


@dataclass(frozen=True)
class Ventana:
    inicio_ms: int
    fin_ms: int
    fase: str
    ciclo: int


@dataclass(frozen=True)
class Resultado:
    nombre: str | None
    ciclos: int
    margen_s: float
    bits: list[tuple[str, int, int, int, int]]
    entradas: list[tuple[int, int, int]]
    bytes: list[tuple[str, int, set[str], set[str]]]
    ids: list[str]


def leer_captura(lineas: Iterable[str]) -> tuple[list[Marca], list[Muestra]]:
    marcas = []
    muestras = []
    for linea in lineas:
        encontrado = _PC_LINE.match(linea.rstrip("\r\n"))
        if encontrado is None:
            continue
        pc_ms, resto = encontrado.groups()
        ms = int(pc_ms)
        marca = _leer_marca(ms, resto)
        if marca is not None:
            marcas.append(marca)
            continue
        frame = parse_line(resto)
        if frame is not None and _muestra_valida(frame):
            muestras.append(Muestra(ms, frame))
    return marcas, muestras


def _muestra_valida(frame: Frame) -> bool:
    if frame.kind == "E":
        try:
            int(frame.fields[0])
            return frame.fields[1] in ("0", "1")
        except (ValueError, IndexError):
            return False
    if frame.kind != "C":
        return False
    encontrado = split_data(frame)
    if encontrado is None:
        return False
    ident, datos = encontrado
    raw_ident, flag, raw_data = frame.fields
    return (
        flag in ("s", "x")
        and re.fullmatch(r"[0-9A-Fa-f]+", raw_ident) is not None
        and len(raw_data) <= 16
        and _HEX.fullmatch(raw_data) is not None
        and len(ident) > 0
        and len(datos) <= 8
    )


def _leer_marca(ms: int, texto: str) -> Marca | None:
    encontrado = _MARK.fullmatch(texto)
    if encontrado is None:
        return None
    fase, ciclo, nombre = encontrado.groups()
    return Marca(ms, fase, int(ciclo), nombre)


def ventanas(marcas: list[Marca], fin_ms: int | None, margen_ms: int) -> list[Ventana]:
    resultado = []
    for indice, marca in enumerate(marcas):
        if marca.fase == "FIN":
            continue
        fin = _fin_ventana(marcas, indice, fin_ms)
        inicio = marca.ms + margen_ms
        if fin is not None and inicio < fin:
            resultado.append(Ventana(inicio, fin, marca.fase, marca.ciclo))
    return resultado


def _fin_ventana(marcas: list[Marca], indice: int, fin_ms: int | None) -> int | None:
    if indice + 1 < len(marcas):
        return marcas[indice + 1].ms
    return fin_ms


def valores_por_ventana(
    muestras: list[Muestra], ventanas_captura: list[Ventana]
) -> list[dict[tuple, set[int | str]]]:
    resultado = []
    for ventana in ventanas_captura:
        valores = _valores_can(muestras, ventana)
        valores.update(_valores_entradas(muestras, ventana))
        resultado.append(valores)
    return resultado


def _valores_can(muestras: list[Muestra], ventana: Ventana) -> dict[tuple, set[int | str]]:
    valores = {}
    for muestra in muestras:
        if muestra.frame.kind != "C" or not _en_ventana(muestra.ms, ventana):
            continue
        encontrado = split_data(muestra.frame)
        if encontrado is None:
            continue
        ident, datos = encontrado
        for indice, byte_hex in enumerate(datos):
            try:
                byte = int(byte_hex, 16)
            except ValueError:
                continue
            _agregar(valores, ("byte", ident, indice), byte_hex)
            for bit in range(8):
                _agregar(valores, ("bit", ident, indice, bit), (byte >> bit) & 1)
    return valores


def _valores_entradas(muestras: list[Muestra], ventana: Ventana) -> dict[tuple, set[int | str]]:
    flancos = [_leer_entrada(muestra) for muestra in muestras if muestra.frame.kind == "E"]
    flancos = [flanco for flanco in flancos if flanco is not None]
    canales = {canal for canal, _, _ in flancos}
    valores = {}
    for canal in canales:
        previos = [(ms, nivel) for otro, ms, nivel in flancos if otro == canal and ms < ventana.inicio_ms]
        if previos:
            _agregar(valores, ("entrada", canal), max(previos)[1])
        for otro, ms, nivel in flancos:
            if otro == canal and _en_ventana(ms, ventana):
                _agregar(valores, ("entrada", canal), nivel)
    return valores


def _leer_entrada(muestra: Muestra) -> tuple[int, int, int] | None:
    try:
        canal, nivel = muestra.frame.fields
        canal_num = int(canal)
        nivel_num = int(nivel)
    except (ValueError, TypeError):
        return None
    if nivel_num not in (0, 1):
        return None
    return canal_num, muestra.ms, nivel_num


def _en_ventana(ms: int, ventana: Ventana) -> bool:
    return ventana.inicio_ms <= ms < ventana.fin_ms


def _agregar(valores: dict[tuple, set[int | str]], clave: tuple, valor: int | str) -> None:
    valores.setdefault(clave, set()).add(valor)


def correlar(marcas: list[Marca], muestras: list[Muestra], margen_ms: int) -> Resultado:
    ventanas_captura = ventanas(marcas, _ultimo_ms(muestras), margen_ms)
    on = [i for i, ventana in enumerate(ventanas_captura) if ventana.fase == "ON"]
    off = [i for i, ventana in enumerate(ventanas_captura) if ventana.fase == "OFF"]
    if len(on) < 2 or len(off) < 2:
        raise ValueError(f"faltan ciclos: {len(on)} ventanas ON y {len(off)} OFF; se requieren al menos 2 de cada una")
    por_ventana = valores_por_ventana(muestras, ventanas_captura)
    claves = set().union(*(valores.keys() for valores in por_ventana))
    bits = _bits_correlacionados(claves, por_ventana, on, off)
    entradas = _entradas_correlacionadas(claves, por_ventana, on, off)
    bytes_ok = _bytes_correlacionados(claves, por_ventana, on, off)
    ids = _ids_solo_on(muestras, ventanas_captura, on, off)
    nombre = next((marca.nombre for marca in marcas if marca.nombre), None)
    return Resultado(nombre, len(on), margen_ms / 1000.0, bits, entradas, bytes_ok, ids)


def _ultimo_ms(muestras: list[Muestra]) -> int | None:
    return max((muestra.ms for muestra in muestras), default=None)


def _bits_correlacionados(claves, por_ventana, on, off):
    resultado = []
    for clave in claves:
        if clave[0] != "bit" or not _aparece_todas(clave, por_ventana):
            continue
        valores_on = [_unico(por_ventana[indice][clave]) for indice in on]
        valores_off = [_unico(por_ventana[indice][clave]) for indice in off]
        if None in valores_on or None in valores_off:
            continue
        if len(set(valores_on)) == len(set(valores_off)) == 1 and valores_on[0] != valores_off[0]:
            resultado.append((clave[1], clave[2], clave[3], valores_on[0], valores_off[0]))
    return sorted(resultado, key=lambda item: (item[0], item[1], -item[2]))


def _entradas_correlacionadas(claves, por_ventana, on, off):
    resultado = []
    for clave in claves:
        if clave[0] != "entrada" or not _aparece_todas(clave, por_ventana):
            continue
        valores_on = [_unico(por_ventana[indice][clave]) for indice in on]
        valores_off = [_unico(por_ventana[indice][clave]) for indice in off]
        if None in valores_on or None in valores_off:
            continue
        if len(set(valores_on)) == len(set(valores_off)) == 1 and valores_on[0] != valores_off[0]:
            resultado.append((clave[1], valores_on[0], valores_off[0]))
    return sorted(resultado)


def _bytes_correlacionados(claves, por_ventana, on, off):
    resultado = []
    for clave in claves:
        if clave[0] != "byte" or not _aparece_todas(clave, por_ventana):
            continue
        valores_on = [_unico(por_ventana[indice][clave]) for indice in on]
        valores_off = [_unico(por_ventana[indice][clave]) for indice in off]
        if None in valores_on or None in valores_off or set(valores_on) & set(valores_off):
            continue
        resultado.append((clave[1], clave[2], set(valores_on), set(valores_off)))
    return sorted(resultado, key=lambda item: (item[0], item[1]))


def _aparece_todas(clave, por_ventana) -> bool:
    return all(clave in valores for valores in por_ventana)


def _unico(valores: set[int | str]) -> int | str | None:
    return next(iter(valores)) if len(valores) == 1 else None


def _ids_solo_on(muestras, ventanas_captura, on, off):
    ids_por_ventana = []
    for ventana in ventanas_captura:
        presentes = set()
        for muestra in muestras:
            if muestra.frame.kind != "C" or not _en_ventana(muestra.ms, ventana):
                continue
            encontrado = split_data(muestra.frame)
            if encontrado is not None:
                presentes.add(encontrado[0])
        ids_por_ventana.append(presentes)
    comunes_on = set.intersection(*(ids_por_ventana[indice] for indice in on))
    comunes_off = set.union(*(ids_por_ventana[indice] for indice in off))
    return sorted(comunes_on - comunes_off)


def formatear(resultado: Resultado) -> str:
    lineas = [f"acción: {resultado.nombre or '(sin nombre)'} · {resultado.ciclos} ciclos · margen {resultado.margen_s:.1f} s"]
    if resultado.bits or resultado.entradas:
        lineas.append("bits que siguen la acción:")
        lineas.extend(_formatear_bit(item) for item in resultado.bits)
        lineas.extend(_formatear_entrada(item) for item in resultado.entradas)
    if resultado.bytes:
        lineas.append("bytes que siguen la acción:")
        lineas.extend(_formatear_byte(item) for item in resultado.bytes)
    if resultado.ids:
        lineas.append("IDs solo al accionar: " + ", ".join(resultado.ids))
    if not (resultado.bits or resultado.entradas or resultado.bytes or resultado.ids):
        lineas.append("ningún bit, byte ni entrada siguió todas las fases")
    return "\n".join(lineas)


def _formatear_bit(item) -> str:
    ident, byte, bit, on, off = item
    return f"  {ident} byte {byte} bit {bit}: ON={on} OFF={off}"


def _formatear_entrada(item) -> str:
    canal, on, off = item
    return f"  entrada {canal}: ON={on} OFF={off}"


def _formatear_byte(item) -> str:
    ident, byte, on, off = item
    valores_on = ",".join(sorted(on))
    valores_off = ",".join(sorted(off))
    return f"  {ident} byte {byte}: ON={{{valores_on}}} OFF={{{valores_off}}}"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("captura", type=Path)
    parser.add_argument("--margen-s", type=float, default=1.0)
    args = parser.parse_args(argv)
    with open(args.captura, encoding="utf-8", errors="replace") as entrada:
        marcas, muestras = leer_captura(entrada)
    try:
        resultado = correlar(marcas, muestras, round(args.margen_s * 1000))
    except ValueError as error:
        print(error, file=sys.stderr)
        return 2
    print(formatear(resultado))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

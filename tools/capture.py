import argparse
import math
import re
import sys
import time


def _parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Graba líneas RDP1 del ESP32 con marca de tiempo del PC.")
    parser.add_argument("--port", required=True)
    parser.add_argument("--baud", type=int, default=921600)
    parser.add_argument("--out", required=True)
    parser.add_argument("--guion")
    parser.add_argument("--ciclos", type=int, default=5)
    parser.add_argument("--fase-s", type=float, default=5.0)
    args = parser.parse_args(argv)
    if args.guion is not None and re.fullmatch(r"[A-Za-z0-9_-]+", args.guion) is None:
        parser.error("--guion solo admite letras, números, guion y guion bajo")
    if args.ciclos < 2:
        parser.error("--ciclos debe ser al menos 2")
    if not math.isfinite(args.fase_s) or args.fase_s < 1.0:
        parser.error("--fase-s debe ser al menos 1.0")
    return args


def plan_fases(ciclos: int, fase_s: float) -> list[tuple[float, str, int]]:
    fases = [(0.0, "OFF", 0)]
    for ciclo in range(1, ciclos + 1):
        fases.append(((2 * ciclo - 1) * fase_s, "ON", ciclo))
        fases.append((2 * ciclo * fase_s, "OFF", ciclo))
    fases.append(((2 * ciclos + 1) * fase_s, "FIN", ciclos))
    return fases


def mark_line(pc_ms: int, fase: str, ciclo: int, nombre: str) -> str:
    return f"{pc_ms} FASE {fase} {ciclo} {nombre}"


def anunciar(fase: str) -> None:
    instrucciones = {
        "ON": "ACCIONA y mantén",
        "OFF": "SUELTA, no toques nada",
        "FIN": "Fin de la captura",
    }
    print(instrucciones[fase])
    if sys.platform != "win32":
        print("\a", end="", flush=True)
        return
    import winsound
    if fase == "ON":
        winsound.Beep(1800, 300)
    elif fase == "OFF":
        winsound.Beep(600, 300)
    else:
        for _ in range(3):
            winsound.Beep(1200, 120)


def cuenta_regresiva() -> None:
    for numero in (3, 2, 1):
        inicio = time.monotonic()
        print(numero, flush=True)
        if sys.platform == "win32":
            import winsound
            winsound.Beep(1000, 150)
        else:
            print("\a", end="", flush=True)
        time.sleep(max(0.0, 1.0 - (time.monotonic() - inicio)))


def _stamped(raw: bytes) -> str | None:
    if not raw:
        return None
    text = raw.decode("utf-8", errors="replace").rstrip("\r\n")
    return f"{int(time.time() * 1000)} {text}"


def main() -> None:
    args = _parse_args()
    import serial
    if args.guion is not None:
        _captura_guiada(args, serial)
        return
    port = serial.Serial(args.port, args.baud, timeout=1)
    try:
        with open(args.out, "a", encoding="utf-8") as out:
            try:
                while True:
                    line = _stamped(port.readline())
                    if line is None:
                        continue
                    out.write(line + "\n")
                    out.flush()
            except KeyboardInterrupt:
                pass
    finally:
        port.close()


def _captura_guiada(args, serial) -> None:
    try:
        cuenta_regresiva()
    except KeyboardInterrupt:
        return
    port = serial.Serial(args.port, args.baud, timeout=1)
    try:
        if hasattr(port, "set_buffer_size"):
            port.set_buffer_size(rx_size=1_048_576)
        port.reset_input_buffer()
        t0 = time.monotonic()
        fases = plan_fases(args.ciclos, args.fase_s)
        indice = 0
        with open(args.out, "a", encoding="utf-8") as out:
            indice = _escribir_transiciones(out, fases, indice, t0, args.guion)
            while indice < len(fases):
                line = _stamped(port.readline())
                if line is not None:
                    out.write(line + "\n")
                    out.flush()
                indice = _escribir_transiciones(out, fases, indice, t0, args.guion)
                if fases[indice - 1][1] == "FIN":
                    break
    except KeyboardInterrupt:
        pass
    finally:
        port.close()


def _escribir_transiciones(out, fases, indice, t0, nombre) -> int:
    elapsed = time.monotonic() - t0
    while indice < len(fases) and elapsed >= fases[indice][0]:
        _, fase, ciclo = fases[indice]
        out.write(mark_line(int(time.time() * 1000), fase, ciclo, nombre) + "\n")
        out.flush()
        anunciar(fase)
        indice += 1
    return indice


if __name__ == "__main__":
    main()

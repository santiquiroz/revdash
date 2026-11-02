import argparse
import csv
import statistics
import sys
from pathlib import Path

try:
    from tools.rdp import frequency_series, parse_line
except ImportError:
    from rdp import frequency_series, parse_line

STEP_MS = 500
MAX_SHIFT = 1200
MIN_OVERLAP = 60


class CalibrationError(Exception):
    pass


def load_revscope_speed(path: Path) -> list[tuple[int, float]]:
    rows = []
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            try:
                if row["section"] != "obd" or row["key"] != "0D":
                    continue
                rows.append((int(row["timestamp"]), float(row["value1"])))
            except (KeyError, TypeError, ValueError):
                continue
    return rows


def resample(series: list[tuple[int, float]], step_ms: int) -> list[float]:
    if not series:
        return []
    out = []
    idx = 0
    last = series[0][1]
    end = series[-1][0]
    t = series[0][0]
    while t <= end:
        while idx + 1 < len(series) and series[idx + 1][0] <= t:
            idx += 1
            last = series[idx][1]
        out.append(last)
        t += step_ms
    return out


def best_offset(a: list[float], b: list[float], max_shift: int) -> int:
    if not a or not b:
        raise CalibrationError("series vacías: traslape menor a 60 muestras")
    if statistics.pstdev(a) == 0 or statistics.pstdev(b) == 0:
        raise CalibrationError("serie sin variación")
    best_s = None
    best_r = None
    for s in range(-max_shift, max_shift + 1):
        if s >= 0:
            n = min(len(a) - s, len(b))
            seg_a = a[s:s + n]
            seg_b = b[:n]
        else:
            n = min(len(a), len(b) + s)
            seg_a = a[:n]
            seg_b = b[-s:-s + n]
        if n < MIN_OVERLAP:
            continue
        try:
            r = statistics.correlation(seg_a, seg_b)
        except statistics.StatisticsError:
            continue
        if best_r is None or r > best_r:
            best_r = r
            best_s = s
    if best_s is None:
        raise CalibrationError("sin desplazamiento válido: traslape menor a 60 muestras o sin variación")
    return best_s


def fit_factor(hz: list[float], kmh: list[float]) -> tuple[float, float]:
    shh = sum(h * h for h in hz)
    if shh == 0:
        raise CalibrationError("suma de hz al cuadrado es 0")
    k = sum(h * v for h, v in zip(hz, kmh)) / shh
    mean_v = sum(kmh) / len(kmh)
    ss_tot = sum((v - mean_v) ** 2 for v in kmh)
    if ss_tot == 0:
        return k, 0.0
    ss_res = sum((v - k * h) ** 2 for h, v in zip(hz, kmh))
    return k, 1 - ss_res / ss_tot


def crop_overlap(a: list[float], b: list[float], s: int) -> tuple[list[float], list[float]]:
    if s >= 0:
        n = min(len(a) - s, len(b))
        return a[s:s + n], b[:n]
    n = min(len(a), len(b) + s)
    return a[:n], b[-s:-s + n]


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Calibra el factor de velocidad contra RevScope")
    parser.add_argument("--probe", required=True, type=Path)
    parser.add_argument("--revscope", required=True, type=Path)
    parser.add_argument("--channel", type=int, default=0)
    args = parser.parse_args(argv)
    try:
        frames = []
        with open(args.probe, encoding="utf-8", errors="replace") as f:
            for line in f:
                frame = parse_line(line)
                if frame is not None:
                    frames.append(frame)
        hz = resample(frequency_series(frames, args.channel), STEP_MS)
        kmh = resample(load_revscope_speed(args.revscope), STEP_MS)
        if not hz or not kmh:
            raise CalibrationError("sin datos de frecuencia o de velocidad para calibrar")
        s = best_offset(hz, kmh, MAX_SHIFT)
        hz_cut, kmh_cut = crop_overlap(hz, kmh, s)
        k, r2 = fit_factor(hz_cut, kmh_cut)
        if k == 0:
            raise CalibrationError("factor k es 0: no se puede calcular pulsos por km")
    except CalibrationError as e:
        print("calibrate_speed: error de calibración:", e, file=sys.stderr)
        return 2
    except OSError as e:
        print("calibrate_speed: no se pudo leer un archivo:", e, file=sys.stderr)
        return 2
    print(f"k = {k:.4f} km/h por Hz")
    print(f"pulsos por km = {3600 / k:.1f}")
    print(f"R² = {r2:.4f}")
    print(f"desfase = {s * 0.5:.1f} s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

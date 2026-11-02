import argparse
import re
from collections.abc import Iterable
from pathlib import Path

try:
    from tools.rdp import Frame, parse_line
except ImportError:
    from rdp import Frame, parse_line

_CANDUMP = re.compile(r"^\s*\((\d+)\.(\d+)\)\s+\S+\s+([0-9A-Fa-f]+)#(\S*)\s*$")
_HEX = re.compile(r"^[0-9A-Fa-f]*$")


def parse_candump(line: str) -> Frame | None:
    if not isinstance(line, str):
        return None
    match = _CANDUMP.match(line)
    if match is None:
        return None
    whole, frac, raw_id, raw_data = match.groups()
    if len(raw_id) > 8:
        return None
    if raw_data[:1] in ("R", "r"):
        return None
    if _HEX.match(raw_data) is None:
        return None
    if len(raw_data) % 2 == 1:
        return None
    if len(raw_data) // 2 > 8:
        return None
    ms = int(whole) * 1000 + int((frac + "000")[:3])
    flag = "x" if len(raw_id) == 8 else "s"
    return Frame("C", ms, (raw_id.upper(), flag, raw_data.upper()))


def load_frames(path: Path) -> list[Frame]:
    frames = []
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            frame = parse_candump(line)
            if frame is None:
                frame = parse_line(line)
            if frame is not None:
                frames.append(frame)
    return frames


def normalize_id(ident: str, flag: str) -> str:
    width = 8 if flag == "x" else 3
    try:
        return format(int(ident, 16), "0" + str(width) + "X")
    except ValueError:
        return ident.upper()


def split_data(frame: Frame | None) -> tuple[str, list[str]] | None:
    if frame is None or frame.kind != "C":
        return None
    try:
        ident, flag, data = frame.fields
    except ValueError:
        return None
    if len(data) % 2 == 1:
        return None
    return normalize_id(ident, flag), [data[i:i + 2].upper() for i in range(0, len(data), 2)]


def byte_values(frames: Iterable[Frame]) -> dict[tuple[str, int], set[str]]:
    values: dict[tuple[str, int], set[str]] = {}
    for frame in frames:
        found = split_data(frame)
        if found is None:
            continue
        ident, data_bytes = found
        for index, byte in enumerate(data_bytes):
            values.setdefault((ident, index), set()).add(byte)
    return values


def can_ids(frames: Iterable[Frame]) -> set[str]:
    ids = set()
    for frame in frames:
        if frame is not None and frame.kind == "C" and len(frame.fields) == 3:
            ids.add(normalize_id(frame.fields[0], frame.fields[1]))
    return ids


def new_ids(base: Iterable[Frame], action: Iterable[Frame]) -> list[str]:
    return sorted(can_ids(action) - can_ids(base))


def changed_bytes(base_values, action_values) -> list[tuple[str, int, str, list[str]]]:
    changes = []
    for key, seen in action_values.items():
        if key not in base_values:
            continue
        if len(base_values[key]) != 1:
            continue
        base_byte = next(iter(base_values[key]))
        fresh = sorted(seen - base_values[key])
        if fresh:
            changes.append((key[0], key[1], base_byte, fresh))
    changes.sort(key=lambda item: (item[0], item[1]))
    return changes


def report(ids: list[str], changes: list[tuple[str, int, str, list[str]]]) -> None:
    if not ids and not changes:
        print("sin diferencias")
        return
    if ids:
        print("IDs nuevos: " + ", ".join(ids))
    for ident, index, base_byte, fresh in changes:
        print(ident + " byte " + str(index) + ": " + base_byte + " -> " + " ".join(fresh))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True, type=Path)
    parser.add_argument("--action", required=True, type=Path)
    args = parser.parse_args()
    base_frames = load_frames(args.base)
    action_frames = load_frames(args.action)
    ids = new_ids(base_frames, action_frames)
    changes = changed_bytes(byte_values(base_frames), byte_values(action_frames))
    report(ids, changes)


if __name__ == "__main__":
    main()

import re
from collections.abc import Iterable
from dataclasses import dataclass

_ARITY = {"H": 2, "S": 1, "E": 2, "F": 2, "C": 3, "K": 1}
_PATTERN = re.compile(r"([HSEFCK]),(\d+)")


@dataclass(frozen=True)
class Frame:
    kind: str
    ms: int
    fields: tuple[str, ...]


def parse_line(line: str) -> Frame | None:
    try:
        if not isinstance(line, str):
            return None
        text = line.strip()
        match = _PATTERN.search(text)
        if match is None:
            return None
        rest = text[match.end():]
        if rest and not rest.startswith(","):
            return None
        fields = tuple(rest[1:].split(",")) if rest else ()
        if len(fields) != _ARITY[match.group(1)]:
            return None
        return Frame(match.group(1), int(match.group(2)), fields)
    except Exception:
        return None


def frequency_series(frames: Iterable[Frame], ch: int) -> list[tuple[int, float]]:
    series = []
    for frame in frames:
        try:
            if frame is None or frame.kind != "F":
                continue
            if int(frame.fields[0]) != ch:
                continue
            series.append((frame.ms, int(frame.fields[1]) / 1000.0))
        except (ValueError, IndexError, AttributeError, TypeError):
            continue
    return series

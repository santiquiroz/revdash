import argparse
import time


def _parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Graba líneas RDP1 del ESP32 con marca de tiempo del PC.")
    parser.add_argument("--port", required=True)
    parser.add_argument("--baud", type=int, default=921600)
    parser.add_argument("--out", required=True)
    return parser.parse_args(argv)


def _stamped(raw: bytes) -> str | None:
    if not raw:
        return None
    text = raw.decode("utf-8", errors="replace").rstrip("\r\n")
    return f"{int(time.time() * 1000)} {text}"


def main() -> None:
    args = _parse_args()
    import serial
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


if __name__ == "__main__":
    main()

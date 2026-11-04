import argparse
import socket
import struct
import subprocess
import time

MAGIA = b"RDE1"
PUERTO = 27183
TAMANO_CABECERA = 8
TAMANO_PAQUETE = 13
INTERVALO_REPORTE = 2.0


def leer_cabecera(datos: bytes) -> tuple[int, int]:
    if len(datos) != TAMANO_CABECERA or datos[:4] != MAGIA:
        raise ValueError("cabecera RDE1 inválida")
    ancho, alto = struct.unpack(">HH", datos[4:])
    return ancho, alto


def leer_encabezado_paquete(datos: bytes) -> tuple[int, int, int]:
    if len(datos) != TAMANO_PAQUETE:
        raise ValueError("encabezado de paquete inválido")
    tipo, reloj_ms, largo = struct.unpack(">BQI", datos)
    if tipo not in (0, 1, 2):
        raise ValueError("tipo de paquete inválido")
    return tipo, reloj_ms, largo


def empaquetar_toque(accion: int, x: float, y: float) -> bytes:
    if accion not in (0, 1, 2):
        raise ValueError("acción inválida")
    return struct.pack(">Bff", accion, _recortar(x), _recortar(y))


def _recortar(valor: float) -> float:
    return min(1.0, max(0.0, valor))


def percentiles(valores: list) -> tuple[float, float]:
    if not valores:
        raise ValueError("lista vacía")
    ordenados = sorted(valores)
    return _cuantil(ordenados, 0.5), _cuantil(ordenados, 0.9)


def _cuantil(ordenados: list, q: float) -> float:
    ultimo = len(ordenados) - 1
    if ultimo == 0:
        return float(ordenados[0])
    pos = q * ultimo
    base = int(pos)
    resto = pos - base
    if base >= ultimo:
        return float(ordenados[ultimo])
    return ordenados[base] + resto * (ordenados[base + 1] - ordenados[base])


def leer_exacto(sock, n: int) -> bytes:
    partes = []
    pendiente = n
    while pendiente > 0:
        trozo = sock.recv(pendiente)
        if not trozo:
            raise ConnectionError("el socket se cerró")
        partes.append(trozo)
        pendiente -= len(trozo)
    return b"".join(partes)


def _ahora_ms() -> int:
    return int(time.time() * 1000)


def desfase_reloj() -> int:
    antes = _ahora_ms()
    salida = subprocess.run(["adb", "shell", "date", "+%s%3N"], capture_output=True, text=True, check=True)
    despues = _ahora_ms()
    return (antes + despues) // 2 - int(salida.stdout.strip())


def _parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Muestra el duplicado de pantalla del celular.")
    parser.add_argument("--host", required=True)
    parser.add_argument("--puerto", type=int, default=PUERTO)
    parser.add_argument("--adb", action="store_true")
    return parser.parse_args(argv)


def _enviar(sock, accion: int, x: float, y: float, ancho: int, alto: int) -> None:
    sock.sendall(empaquetar_toque(accion, x / ancho, y / alto))


def _reportar(latencias: list, cuadros: int, segundos: float) -> None:
    fps = cuadros / segundos
    if not latencias:
        print(f"sin cuadros fps={fps:.1f}")
        return
    p50, p90 = percentiles(latencias)
    print(f"latencia ms p50={p50:.0f} p90={p90:.0f} fps={fps:.1f}")


def _bucle(sock, desfase: int) -> None:
    import av
    import cv2
    ancho, alto = leer_cabecera(leer_exacto(sock, TAMANO_CABECERA))
    codigo = av.CodecContext.create("h264", "r")
    ventana = "espejo"
    cv2.namedWindow(ventana, cv2.WINDOW_NORMAL)
    pulsado = False

    def raton(evento, x, y, banderas, _extra):
        nonlocal pulsado
        if evento == cv2.EVENT_LBUTTONDOWN:
            pulsado = True
            _enviar(sock, 0, x, y, ancho, alto)
        elif evento == cv2.EVENT_MOUSEMOVE and banderas & cv2.EVENT_FLAG_LBUTTON and pulsado:
            _enviar(sock, 1, x, y, ancho, alto)
        elif evento == cv2.EVENT_LBUTTONUP:
            pulsado = False
            _enviar(sock, 2, x, y, ancho, alto)

    cv2.setMouseCallback(ventana, raton)
    latencias: list[float] = []
    cuadros = 0
    inicio = time.monotonic()
    while True:
        _tipo, reloj_ms, largo = leer_encabezado_paquete(leer_exacto(sock, TAMANO_PAQUETE))
        carga = leer_exacto(sock, largo)
        for unidad in codigo.parse(carga):
            for cuadro in codigo.decode(unidad):
                cv2.imshow(ventana, cuadro.to_ndarray(format="bgr24"))
                cuadros += 1
                latencias.append(_ahora_ms() - reloj_ms - desfase)
        tecla = cv2.waitKey(1) & 0xFF
        momento = time.monotonic()
        if tecla in (27, ord("q")):
            break
        if momento - inicio >= INTERVALO_REPORTE:
            _reportar(latencias, cuadros, momento - inicio)
            latencias = []
            cuadros = 0
            inicio = momento
    cv2.destroyAllWindows()


def main(argv=None) -> None:
    args = _parse_args(argv)
    desfase = desfase_reloj() if args.adb else 0
    sock = socket.create_connection((args.host, args.puerto))
    try:
        _bucle(sock, desfase)
    except KeyboardInterrupt:
        pass
    finally:
        sock.close()


if __name__ == "__main__":
    main()

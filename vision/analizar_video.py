import argparse
import csv
import time
from pathlib import Path

import cv2

import yolox
from seguimiento import actualizar, lado, tiempo_al_choque
from semaforo import color_semaforo

COLUMNAS = ("t_s", "id", "clase", "puntaje", "x1", "y1", "x2", "y2", "ttc_s", "lado", "color_semaforo")


def argumentos(argv=None):
    parser = argparse.ArgumentParser(description="Evalua YOLOX sobre un video de ruta.")
    parser.add_argument("video")
    parser.add_argument("--modelo", required=True)
    parser.add_argument("--salida", default=".")
    parser.add_argument("--cada", type=int, default=2)
    parser.add_argument("--camara", choices=["delantera", "trasera"], default="delantera")
    return parser.parse_args(argv)


def recortar(imagen, caja):
    alto, ancho = imagen.shape[:2]
    x1 = max(0, int(caja[0]))
    y1 = max(0, int(caja[1]))
    x2 = min(ancho, int(caja[2]))
    y2 = min(alto, int(caja[3]))
    return imagen[y1:y2, x1:x2]


def dibujar(imagen, caja, texto, alerta):
    x1, y1, x2, y2 = (int(v) for v in caja)
    color = (0, 0, 255) if alerta else (0, 255, 0)
    cv2.rectangle(imagen, (x1, y1), (x2, y2), color, 2)
    cv2.putText(imagen, texto, (x1, max(0, y1 - 8)), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)


def puntaje_de(pista, detecciones, umbral_iou=0.3):
    from seguimiento import iou

    mejor = 0.0
    for det in detecciones:
        if det.clase == pista.clase and iou(pista.caja, det.caja) >= umbral_iou:
            mejor = max(mejor, det.puntaje)
    return mejor


def main():
    args = argumentos()
    sesion = yolox.crear_sesion(args.modelo)
    nombre_entrada = sesion.get_inputs()[0].name
    entrada = cv2.VideoCapture(args.video)
    fps = entrada.get(cv2.CAP_PROP_FPS) or 30.0
    ancho = int(entrada.get(cv2.CAP_PROP_FRAME_WIDTH))
    alto = int(entrada.get(cv2.CAP_PROP_FRAME_HEIGHT))
    destino = Path(args.salida)
    destino.mkdir(parents=True, exist_ok=True)
    nombre = Path(args.video).stem
    escritor = cv2.VideoWriter(
        str(destino / f"{nombre}_anotado.mp4"),
        cv2.VideoWriter_fourcc(*"mp4v"),
        fps,
        (ancho, alto),
    )
    archivo_csv = open(destino / f"{nombre}_detecciones.csv", "w", newline="", encoding="utf-8")
    tabla = csv.writer(archivo_csv)
    tabla.writerow(COLUMNAS)
    etiqueta = "ATRÁS" if args.camara == "trasera" else "ADELANTE"
    pistas = ()
    proximo_id = 0
    cuadro = 0
    procesados = 0
    inferencia_s = 0.0
    conteo = {}
    alertas = 0
    while True:
        leido, imagen = entrada.read()
        if not leido:
            break
        if cuadro % args.cada == 0:
            t_s = cuadro / fps
            tensor, escala = yolox.letterbox(imagen)
            inicio = time.perf_counter()
            salida = sesion.run(None, {nombre_entrada: tensor})[0]
            inferencia_s += time.perf_counter() - inicio
            procesados += 1
            detecciones = yolox.detectar(yolox.decodificar(salida), escala)
            pistas, proximo_id = actualizar(pistas, detecciones, t_s, siguiente_id=proximo_id)
            for pista in pistas:
                if not pista.historial or pista.historial[-1][0] != t_s:
                    continue
                conteo[pista.clase] = conteo.get(pista.clase, 0) + 1
                ttc = tiempo_al_choque(pista)
                alerta = ttc is not None and ttc < 3.0
                if alerta:
                    alertas += 1
                color = None
                if pista.clase == "semaforo":
                    color = color_semaforo(recortar(imagen, pista.caja))
                texto = f"{pista.clase} #{pista.id}"
                if ttc is not None:
                    texto += f" TTC {ttc:.1f}s"
                    if alerta:
                        texto += f" {etiqueta}"
                dibujar(imagen, pista.caja, texto, alerta)
                x1, y1, x2, y2 = pista.caja
                tabla.writerow(
                    [
                        f"{t_s:.3f}",
                        pista.id,
                        pista.clase,
                        f"{puntaje_de(pista, detecciones):.3f}",
                        f"{x1:.1f}",
                        f"{y1:.1f}",
                        f"{x2:.1f}",
                        f"{y2:.1f}",
                        f"{ttc:.2f}" if ttc is not None else "",
                        lado(pista.caja, ancho),
                        color or "",
                    ]
                )
        cuadro += 1
        escritor.write(imagen)
    entrada.release()
    escritor.release()
    archivo_csv.close()
    velocidad = procesados / inferencia_s if inferencia_s > 0 else 0.0
    print(f"cuadros/s de inferencia: {velocidad:.1f}")
    for clase, total in sorted(conteo.items()):
        print(f"{clase}: {total}")
    print(f"veces con TTC < 3 s: {alertas}")


if __name__ == "__main__":
    main()

from dataclasses import dataclass


@dataclass(frozen=True)
class Pista:
    id: int
    clase: str
    caja: tuple
    historial: tuple[tuple[float, float], ...]


def iou(a, b):
    x1 = max(a[0], b[0])
    y1 = max(a[1], b[1])
    x2 = min(a[2], b[2])
    y2 = min(a[3], b[3])
    inter = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    area_a = max(0.0, a[2] - a[0]) * max(0.0, a[3] - a[1])
    area_b = max(0.0, b[2] - b[0]) * max(0.0, b[3] - b[1])
    union = area_a + area_b - inter
    return inter / union if union > 0 else 0.0


def actualizar(pistas, detecciones, t_s, umbral_iou=0.3, siguiente_id=0):
    libres = [True] * len(detecciones)
    nuevas = []
    proximo = siguiente_id
    for pista in pistas:
        mejor = -1
        mejor_iou = umbral_iou
        for i, det in enumerate(detecciones):
            if not libres[i] or det.clase != pista.clase:
                continue
            valor = iou(pista.caja, det.caja)
            if valor >= mejor_iou:
                mejor = i
                mejor_iou = valor
        if mejor >= 0:
            det = detecciones[mejor]
            libres[mejor] = False
            nuevas.append(_mover(pista, det, t_s))
        elif pista.historial and t_s - pista.historial[-1][0] <= 0.5:
            nuevas.append(pista)
    for i, det in enumerate(detecciones):
        if libres[i]:
            nuevas.append(Pista(proximo, det.clase, det.caja, ((t_s, det.caja[3] - det.caja[1]),)))
            proximo += 1
    return tuple(nuevas), proximo


def _mover(pista, deteccion, t_s):
    alto = deteccion.caja[3] - deteccion.caja[1]
    return Pista(pista.id, deteccion.clase, deteccion.caja, (*pista.historial, (t_s, alto))[-15:])


def tiempo_al_choque(pista):
    if not pista.historial:
        return None
    ahora = pista.historial[-1][0]
    recientes = [(t, h) for t, h in pista.historial if t >= ahora - 0.7]
    if len(recientes) < 5:
        return None
    n = len(recientes)
    media_t = sum(t for t, _ in recientes) / n
    media_h = sum(h for _, h in recientes) / n
    num = sum((t - media_t) * (h - media_h) for t, h in recientes)
    den = sum((t - media_t) ** 2 for t, _ in recientes)
    if den == 0:
        return None
    tasa = num / den
    if tasa <= 0:
        return None
    return recientes[-1][1] / tasa


def lado(caja, ancho_imagen):
    centro = (caja[0] + caja[2]) / 2
    if centro < ancho_imagen / 3:
        return "IZQUIERDA"
    if centro < 2 * ancho_imagen / 3:
        return "CENTRO"
    return "DERECHA"

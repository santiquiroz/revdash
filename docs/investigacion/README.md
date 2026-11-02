# Investigación

Notas de investigación que sostienen decisiones de la spec (`docs/superpowers/specs/2026-10-19-revdash-design.md`). Cada afirmación lleva su fuente o se marca como inferencia. Nada de esto es asesoría legal.

| Archivo | Fecha | Tema | Decisión que generó |
|---|---|---|---|
| [android-auto.md](android-auto.md) | 2026-11-01 | Cómo autentica Android Auto a un receptor, estado de aasdk/open-headunit y alternativas | Spec §12 riesgo 9: el tablero nunca depende de Android Auto; open-headunit es opcional y no va en imágenes distribuidas |
| [herramientas-can.md](herramientas-can.md) | 2026-11-01 | Flujo de ingeniería inversa CAN, formatos (candump, DBC), ELM327 como sniffer, protocolo BLE SmartXonnect de TVS, herramientas MCP para RevScope | Formato candump en RevScope y `tools/diff_can.py`; `AT S0` en el monitor; hoja de ruta de herramientas MCP |
| [placa-sbc.md](placa-sbc.md) | 2026-11-02 | Radxa Zero 3W frente a ROCK 4D (RK3576), Orange Pi, Pi 5 y otras: Android, Wi-Fi/BT, calor, consumo | El dueño eligió la ROCK 4D 6 GB (2026-11-02); spec §7 y §8 |

La regulación colombiana relevante para el tablero (revisión técnico-mecánica, odómetro, infracción B.23, radios) está resumida en la spec, §12 riesgo 7.

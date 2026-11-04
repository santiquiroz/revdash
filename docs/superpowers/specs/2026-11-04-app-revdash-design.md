# App RevDash: Fase 1a (sin hardware)

Fecha: 2026-11-04. Estado: decisiones mías tomadas en ausencia del dueño (pidió "avanza con lo que
puedas"); revisar al volver. Spec madre: `2026-10-19-revdash-design.md` (§8.2, §10). Sistema visual:
`docs/diseno/sistema-visual.md`.

## Por qué ahora

La placa y la pantalla no han llegado, pero la app es el camino crítico de la Fase 1 y no las necesita:
corre en el celular o en el emulador alimentada por **capturas CAN reales de la Apache** reproducidas con
su ritmo original. Cuando lleguen la ROCK 4D y el puente ESP32, solo cambia la fuente de tramas.

## Hallazgo que cambia la arquitectura

La Fase 0 encontró que casi todo viaja por el CAN que llega al conector del tablero (marcha, RPM,
acelerador, temperaturas, pata, embrague, kill switch, motor en marcha y velocidad de rueda candidata;
ver `docs/fase0/hallazgos-can-2026-11-02.md`). Por cable quedan solo las direccionales, la luz alta, los
frenos y el combustible (analógico). El "servicio de señales" de §8.2 pasa a ser sobre todo un
**decodificador de CAN guiado por un perfil**, el mismo formato `PerfilCan` v1 de RevScope.

## Alcance

1. **Módulo `nucleo` (Kotlin puro, JVM, sin Android)**: todo lo que se puede probar rápido.
   - Perfil CAN: modelo, lectura JSON y decodificador de señales, copiados de RevScope
     (`core/obd/perfilcan`, Apache-2.0, con aviso de origen). El perfil de la Apache va como recurso.
   - Tramas: parser de líneas candump (`(seg.micro) can0 ID#DATOS`) y reproductor que respeta los
     tiempos originales y puede repetir en bucle.
   - Estado de la moto: proyección de tramas + perfil a `EstadoMoto`, con datos faltantes y viejos
     explícitos (regla de `sistema-visual.md`: "—", nunca 0).
   - Odómetro y trip: integración de la velocidad; huecos de más de 1 s no suman; valor inicial
     configurable.
   - Alertas: modelo (P1/P2/P3, lado, texto) y elección de la alerta visible (una P1 a la vez).
   - Estado de la dashcam: modelo (apagada, grabando N cámaras, falla con texto). Sin cámaras reales todavía.
2. **Módulo `app` (Android, Compose)**: launcher del tablero.
   - Los tres estados de §10 con los tokens y la tipografía de `sistema-visual.md` (Barlow y Barlow
     Condensed empaquetadas, OFL).
   - Capa de alertas y chip de dashcam.
   - **Modo demo** (el único por ahora): reproduce `empujando.log` en bucle y una secuencia de alertas
     simuladas para ver la UI viva. Las direccionales y la luz alta se simulan (no están en el CAN).
   - Persistencia del odómetro (DataStore) cada 100 m y al pasar a segundo plano.
   - Paso entre estados con un toque en una zona marcada de 72 dp o más; sin gestos horizontales.
3. **Pruebas**: unitarias en `nucleo` con las capturas reales como datos de prueba; capturas de pantalla
   de la UI en la JVM con Roborazzi (mismo esquema que RevScope).

## Fuera de alcance (después)

Puente ESP32 por USB (Fase 1, cuando lleguen los transceptores), open-headunit y el cambio automático de
estado por Android Auto, modo kiosco y HOME, cámaras reales y detección (Fase 3, ver
`docs/investigacion/camaras-ia.md`), gestión térmica, ajustes en pantalla.

## Decisiones

| Decisión | Por qué |
|---|---|
| Dos módulos (`nucleo` JVM + `app` Android), sin Hilt | Pruebas rápidas sin Robolectric para la lógica; con 1 pantalla la inyección manual alcanza |
| Copiar el código de perfiles de RevScope, no compartir librería | Una sola copia pequeña (≈250 líneas). Extraer una librería común cuando haya un tercer usuario |
| RevDash usa todas las señales del perfil, verificadas o no | El perfil de la moto propia ya está mapeado; verificar es tarea del asistente de RevScope. Se revisa cuando haya perfiles de terceros |
| Velocidad: `0D` (500 b1 ×0,125) por defecto, `VELOCIDAD_RUEDA` (301 ×0,01) elegible en código | Ninguna de las dos escalas está confirmada; la prueba de ruta decide. Una constante, no un ajuste de usuario |
| Velocidad pasa a "—" a los 500 ms sin trama; el resto de señales a los 3 periodos | Una velocidad congelada es peligrosa; un 92 °C de hace 1 s no |
| Paquete `com.revdash`, minSdk 29, target/compile 35/36, mismas versiones de AGP/Kotlin/Compose que RevScope | Toolchain ya probada en este PC |

## Criterios de aceptación

- `./gradlew :nucleo:test :app:testDebugUnitTest :app:assembleDebug` en verde.
- Con `acelerador_barrido.log` el acelerador decodificado recorre al menos de 0 a 90 %; con
  `empujando.log` la velocidad de rueda pasa de 0.
- Capturas de pantalla de los estados 1, 2 y 3, de la alerta P1 trasera y de "sin dato", comparables con
  las maquetas `docs/img/estado-*.png`.
- La APK de demo corre en el S25 en horizontal y se ve como las maquetas.

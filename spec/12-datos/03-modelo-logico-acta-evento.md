# Modelo lógico — ActaEvento

## Finalidad

`ActaEvento` es la entidad lógica que registra la trazabilidad relevante del expediente.

Su función es conservar los hechos, decisiones, transiciones, resultados e incidencias que explican la evolución del Acta a lo largo de su ciclo de vida.

---

## Rol dentro del modelo

Mientras `Acta` representa la identidad principal del expediente, `ActaEvento` representa su memoria trazable.

Debe permitir reconstruir:

- qué ocurrió
- cuándo ocurrió
- qué efecto produjo
- con qué otras piezas del sistema se vinculó el hecho

---

## Tabla principal

### `ActaEvento`

Entidad de trazabilidad del expediente.

Debe contener, como mínimo a nivel lógico:

- `Id`
- referencia obligatoria a `Acta`
- tipo de evento
- fecha/hora relevante
- orden lógico suficiente para reconstrucción
- datos mínimos de contexto
- referencias opcionales a otras entidades involucradas
- metadatos básicos de registración

---

## Qué debe registrar

En `ActaEvento` deben vivir los hechos con valor real de:

- trazabilidad funcional
- reconstrucción del expediente
- auditoría administrativa
- correlación con documentos, notificaciones, actores o integraciones

Puede registrar, entre otros:

- labrado, captura o incorporación del expediente
- validación, observación, corrección, anulación o cambio de curso
- generación, firma, emisión, recepción u observación de documentos
- emisión, acuse, rechazo, incidencia, reintento o resultado de notificaciones
- imposición, levantamiento o resultado de medidas
- presentaciones, pagos, comparecencias, derivaciones o apelaciones
- cierres, archivos, reaperturas, reingresos o resultados externos

La lista exacta de tipos pertenece a catálogos y reglas del sistema.

---

## Qué no debe registrar

`ActaEvento` no debe usarse para guardar:

- snapshot completo del expediente
- duplicación extensa de documentos o notificaciones
- bitácora técnica de bajo nivel
- auditoría de infraestructura
- estados redundantes sin hecho asociado
- texto libre sin semántica clara

---

## Reglas principales

- Todo `ActaEvento` pertenece a una `Acta`.
- No todo cambio del sistema debe convertirse en evento.
- El evento debe registrar el hecho relevante, no reemplazar otras entidades del modelo.
- `ActaEvento` no reemplaza el estado actual del expediente.
- `ActaEvento` no reemplaza al snapshot operativo.
- Una vez registrado, un evento debe tratarse con criterio conservador respecto de su mutación.

---

## Relaciones clave

`ActaEvento` puede vincularse con:

- `Documento`, cuando el hecho registrado tenga impacto documental
- `Notificacion`, cuando refleje hitos relevantes del proceso de notificación
- `Dependencia` e `Inspector`, cuando agreguen contexto real de trazabilidad
- integraciones o procesos externos, cuando generen efectos relevantes sobre el expediente

Estas referencias complementan el contexto del evento, pero no reemplazan las asociaciones principales del expediente.

---

## Criterio de compactación

`ActaEvento` debe mantenerse como núcleo semántico de trazabilidad.

Cuando un tipo de detalle crezca demasiado, debe separarse en anexos o referencias especializadas en lugar de convertir `ActaEvento` en una tabla excesivamente amplia o ambigua.
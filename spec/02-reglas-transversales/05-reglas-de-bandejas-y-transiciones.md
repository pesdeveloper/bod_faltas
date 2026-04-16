# Reglas de bandejas y transiciones

## Finalidad

Este archivo define las reglas transversales de bandejas y transiciones del expediente.

Su objetivo es dejar claro:

- qué representan las bandejas
- cómo se relacionan con estados y eventos
- cómo se determina la ubicación operativa visible del expediente
- cómo se proyecta esa ubicación en snapshot

---

## Regla principal

Las bandejas representan la ubicación operativa actual del expediente dentro del proceso.

No son la fuente primaria de verdad.

La fuente primaria de verdad sigue estando en:

- el expediente / acta
- sus eventos
- sus documentos
- sus notificaciones
- sus resultados y decisiones asociadas

Las bandejas son una proyección operativa útil para trabajo diario, control y priorización.

---

## Regla de determinación

La bandeja visible del expediente debe derivarse de la combinación de:

- bloque funcional actual
- estado procesal actual
- situación administrativa actual
- eventos recientes
- documentos relevantes
- notificaciones y acuses
- plazos operativos vigentes
- restricciones o pendientes que impidan avanzar

---

## Regla de transición

Una transición de bandeja no debe modelarse como un cambio aislado e independiente.

La transición debe ser consecuencia de hechos del expediente, por ejemplo:

- validación
- observación
- generación documental
- firma
- notificación
- acuse
- presentación
- pago
- reingreso
- derivación externa
- archivo
- cierre

---

## Regla de snapshot

El snapshot operativo debe poder reflejar, como mínimo:

- bloque actual
- estado procesal actual
- situación administrativa actual
- código de bandeja
- visibilidad en bandeja
- prioridad operativa si aplica

La bandeja visible proyectada no reemplaza la lógica de negocio ni la historia del expediente.

---

## Regla de visibilidad

No todo expediente debe estar visible en toda bandeja.

La visibilidad depende de:

- la etapa operativa real
- la necesidad de acción
- la existencia de bloqueos, pendientes o plazos
- la regla funcional del circuito

---

## Regla de prioridad

Cuando una bandeja requiera orden operativo, la prioridad debe derivarse de criterios de negocio, por ejemplo:

- plazo próximo a vencer
- necesidad de actuación humana
- estado crítico
- reingreso reciente
- pendiente material o notificatorio

La prioridad proyectada es auxiliar y no debe reemplazar la evaluación funcional del caso.

---

## Regla de consistencia

Si cambia la situación relevante del expediente, debe actualizarse la proyección de bandeja.

La bandeja nunca debe quedar como fuente “manual” desacoplada de la realidad del expediente.

---

## Regla de regeneración

La ubicación de bandeja debe poder recalcularse desde la fuente primaria.

Esto implica que:

- una proyección inconsistente puede regenerarse
- una bandeja no debe depender de edición manual directa como fuente principal

---

## Resultado esperado

Las bandejas deben operar como una proyección clara, regenerable y consistente de la situación operativa del expediente, sin reemplazar la verdad principal del dominio.
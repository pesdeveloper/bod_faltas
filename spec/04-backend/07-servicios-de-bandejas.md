# 07-servicios-de-bandejas.md

## Finalidad

Este archivo define las responsabilidades del backend respecto de las bandejas operativas del sistema.

Su objetivo es dejar claro cómo se exponen listados, filtros, badges e indicadores operativos a partir del expediente y su proyección.

---

## Regla principal

La implementación de bandejas debe apoyarse prioritariamente en `ActaSnapshot` como proyección operativa única del expediente, evitando reconstrucciones costosas del núcleo para operaciones rutinarias de listado y filtrado.

La lógica de bandejas sigue derivando del expediente y sus hechos, pero su consulta operativa debe resolverse preferentemente desde snapshot.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- listar expedientes por bandeja
- aplicar filtros operativos
- ordenar por prioridad o referencia temporal
- exponer indicadores visibles
- justificar la ubicación de un expediente en una bandeja determinada

---

## Qué no debe hacer

Este bloque no debe:

- reconstruir historia completa del expediente
- reemplazar la lógica de expediente
- duplicar indebidamente lógica documental o notificatoria
- depender de joins excesivos para operaciones rutinarias

---

## Relación con otros servicios

### Con expediente
La bandeja refleja situación operativa derivada del expediente.

### Con snapshot
Snapshot es la fuente principal de consulta para bandejas.

### Con notificación
Estados resumidos de notificación y reintentos deben llegar proyectados o accesibles operativamente.

### Con gestión externa
La condición y resultado resumido de gestión externa debe ser visible si afecta la bandeja.

---

## Idea clave

Las bandejas son una vista operativa del expediente, no una fuente autónoma de verdad. Deben resolverse de forma rápida, consistente y apoyada en snapshot.
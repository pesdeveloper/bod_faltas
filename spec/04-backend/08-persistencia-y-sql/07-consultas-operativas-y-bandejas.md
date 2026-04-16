# Consultas operativas y bandejas

## Finalidad

Este archivo define criterios de SQL y persistencia para consultas operativas, bandejas y listados del sistema.

---

## Regla base

Las bandejas y consultas operativas no deben reconstruir todo el expediente en cada lectura.

Deben apoyarse preferentemente en:

- `ActaSnapshot`
- proyecciones equivalentes
- joins controlados con tablas núcleo cuando haga falta ampliar contexto

---

## Alcance

Este bloque cubre:

- bandejas operativas
- listados
- filtros
- búsquedas
- lecturas resumidas del expediente
- consultas de control administrativo

No cubre:

- reconstrucción histórica completa
- trazabilidad exhaustiva
- detalle documental completo
- detalle técnico de jobs o workers

---

## Fuentes principales de consulta

Las consultas operativas deben apoyarse, según el caso, en:

- `ActaSnapshot`, como fuente principal de lectura rápida
- `Acta`, para identidad principal del expediente
- referencias resumidas a `Dependencia`, `Inspector` y contexto vigente proyectado
- indicadores documentales, de notificación, medidas u otras piezas ya proyectadas

Cuando una consulta requiera mayor detalle, debe ampliar desde el núcleo sin convertir esa excepción en regla general.

---

## Reglas de diseño

- Las bandejas deben consultar estado actual proyectado, no reconstrucción completa del expediente.
- Los filtros principales deben resolverse sobre snapshot o proyecciones equivalentes.
- Las búsquedas por número visible, identidad técnica o referencias administrativas deben apoyarse en estructuras preparadas para ese fin.
- Las consultas no deben depender de joins excesivos para resolver operaciones rutinarias.
- La semántica de bandeja no debe quedar embebida de forma caótica en SQL aislado sin apoyo en snapshot o proyección estable.
- `ActaSnapshot` debe contener explícitamente la información operativa mínima necesaria para resolver bandejas, notificaciones relevantes, gestión externa, situación económica resumida y plazos sin exigir joins excesivos para consultas rutinarias.

---

## Consultas que siguen en núcleo

Deben seguir consultando directamente el núcleo transaccional, entre otras:

- reconstrucción histórica del expediente
- trazabilidad completa
- lectura detallada de eventos
- lectura documental completa
- auditoría administrativa profunda
- validaciones de integridad o reproceso

Estas consultas no deben descargarse sobre snapshot.

---

## Relación con snapshot

`ActaSnapshot` es la base preferente para:

- bandejas
- listados
- filtros
- búsqueda operativa
- lectura rápida del estado actual

Si una bandeja necesita demasiada lógica fuera del snapshot, debe evaluarse si falta una proyección operativa adicional o un ajuste de reproyección.

---

## Relación con SQL explícito

Dado que el backend se implementará con SQL explícito:

- las consultas deben ser legibles
- los joins deben ser controlados
- los filtros deben ser predecibles
- las proyecciones deben responder a necesidades operativas claras
- no debe delegarse la lógica de acceso en mecanismos automáticos de mapeo

---

## Resultado esperado

Este bloque debe dejar resuelto que las bandejas y consultas operativas se apoyan principalmente en snapshot o proyecciones equivalentes, mientras que la reconstrucción completa del expediente sigue perteneciendo al núcleo transaccional.
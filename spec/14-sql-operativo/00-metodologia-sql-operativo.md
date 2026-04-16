# [14-SQL-OPERATIVO] 00 - METODOLOGIA SQL OPERATIVO

## Finalidad

Este bloque define cómo se usa la estructura física documentada en `spec/13-ddl/` para resolver operaciones reales del sistema.

- `spec/13-ddl/` = estructura física
- `spec/14-sql-operativo/` = uso operativo real de esa estructura

---

## Cómo leer este bloque

- transacciones, secuencias y padre/hijos → [`01-patrones-transaccionales.md`](./01-patrones-transaccionales.md)
- bandejas → [`02-sql-bandejas.md`](./02-sql-bandejas.md)
- formularios y lookups → [`03-sql-formularios-y-lookups.md`](./03-sql-formularios-y-lookups.md)
- acta → [`04-sql-crud-acta.md`](./04-sql-crud-acta.md)
- documental → [`05-sql-crud-documental.md`](./05-sql-crud-documental.md)
- notificación → [`06-sql-crud-notificacion.md`](./06-sql-crud-notificacion.md)
- referenciales, numeración y storage → [`07-sql-crud-referenciales-y-transversales.md`](./07-sql-crud-referenciales-y-transversales.md)
- snapshot y reproceso → [`08-sql-proyecciones-y-reproceso.md`](./08-sql-proyecciones-y-reproceso.md)

---

## Alcance

Este bloque cubre:

- SQL transaccional de escritura
- SQL de lectura operativa
- SQL de bandejas
- SQL de formularios y lookups
- reglas de secuencias
- patrones padre/hijos
- proyecciones y reproceso

No cubre:

- DDL estructural
- creación física de tablas
- tuning fino
- implementación Java final
- infraestructura

---

## Reglas madre

- Diseñar por caso de uso, no por tabla.
- Informix 12.10 es el motor objetivo real.
- Separar escritura, lectura, bandejas, lookups y reproceso.
- Primero corrección funcional; después optimización.
- Toda secuencia debe ser explícita.
- Toda operación padre/hijos debe documentar orden, IDs y alcance transaccional.
- Lo histórico append-only no se reescribe con updates silenciosos.
- Las bandejas deben apoyarse preferentemente en `FalActaSnapshot`.
- Los lookups UX deben ser rápidos, acotados y orientados a selección.

---

## Nomenclatura física relevante

Respetar [`../12-datos/00-convencion-nombres-fisicos.md`](../12-datos/00-convencion-nombres-fisicos.md).

Resumen:

- `Fal*` = dominio propio de faltas
- `Num*` = numeración / talonarios transversal
- `Stor*` = storage documental transversal
- las entidades compartidas conservan su nombre real, por ejemplo:
  - `RubroCom`
  - `RubroComVersion`

---

## Relación con otros bloques

Leer junto con:

- `spec/01-dominio/`
- `spec/02-reglas-transversales/`
- `spec/03-bandejas/`
- `spec/04-backend/`
- `spec/13-ddl/`

Prioridad si hubiera contradicción:

1. `01-dominio`
2. `02-reglas-transversales`
3. `03-bandejas`
4. `13-ddl`
5. `14-sql-operativo`

---

## Resultado esperado

Este bloque debe permitir:

- implementar repositorios SQL explícitos
- implementar servicios sin improvisación
- resolver altas compuestas con secuencias
- construir bandejas operativas
- construir lookups UX sólidos
- mantener `FalActaSnapshot` y otras proyecciones
- reducir ambigüedad en la persistencia real

---

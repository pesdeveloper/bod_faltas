# Versionado de entidades referenciales

## Finalidad

Este archivo define la regla conceptual de versionado aplicable a ciertas entidades referenciales del dominio cuyo contexto debe permanecer estable una vez utilizado por el expediente.

Su función es preservar la inmutabilidad histórica del contexto del acta sin duplicar snapshots textuales completos dentro de la propia entidad principal.

---

## Problema que resuelve

Algunas entidades referenciales del dominio, como por ejemplo Dependencia o Inspector, pueden cambiar sus datos con el tiempo.

Si el expediente solo conservara una referencia viva a esas entidades, cualquier cambio posterior podría alterar indirectamente el contexto histórico de actas ya emitidas o capturadas.

Eso no es aceptable cuando el expediente debe conservar estabilidad administrativa y documental.

---

## Regla general

Cuando una entidad referencial forme parte del contexto relevante del expediente y sus datos puedan cambiar a futuro, el sistema debe contemplar una noción de versión aplicable a esa entidad.

La Acta no debe depender únicamente de la entidad “actual”, sino también de la versión efectiva que correspondía al momento en que el contexto quedó fijado para el expediente.

---

## Objetivo del versionado

El versionado de entidades referenciales busca:

- preservar estabilidad histórica
- evitar mutaciones indirectas del contenido contextual del expediente
- no copiar snapshots textuales completos dentro del Acta
- mantener consistencia entre referencia viva y referencia histórica
- permitir trazabilidad del contexto utilizado al momento del caso

---

## Qué entidades puede alcanzar

Esta regla aplica a entidades referenciales cuyo contexto impacte materialmente en el expediente.

Ejemplos claros en este dominio:

- Dependencia
- Inspector

Podría extenderse a otras entidades si más adelante se detecta la misma necesidad.

---

## Qué no implica

El versionado de entidades referenciales no implica:

- convertir cada pequeño cambio técnico en un evento del expediente
- incrustar texto libre duplicado en el Acta
- reemplazar la trazabilidad del expediente
- reemplazar el snapshot operativo

Tampoco implica que todas las entidades del sistema deban versionarse.

Solo debe aplicarse cuando haya valor histórico o administrativo real.

---

## Relación con el expediente

Cuando el expediente utilice una entidad referencial versionada, debe poder quedar asociado al menos a dos planos distintos:

- la entidad referencial principal
- la versión efectiva aplicable al momento del expediente

De ese modo, el sistema conserva tanto la referencia estructural como el contexto histórico correcto.

---

## Relación con snapshot

El versionado de entidades referenciales no reemplaza al snapshot operativo.

El snapshot puede usar información derivada de esas referencias versionadas, pero la fuente de verdad del contexto histórico sigue estando en la relación del expediente con la entidad y su versión aplicable.

---

## Relaciones clave

Esta regla se relaciona conceptualmente con:

- Acta, como expediente que consume contexto referencial estable
- Dependencia, como entidad referencial versionable
- Inspector, como entidad referencial versionable
- modelo lógico de datos, donde luego se resolverá la estructura concreta de entidad y versión
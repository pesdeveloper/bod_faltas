# 06-medidas-y-liberaciones.md

## Finalidad

Este archivo resume el tratamiento conceptual de:

- medidas preventivas
- levantamientos
- liberaciones
- restituciones materiales
- pendientes que impactan en la operatoria del expediente

Su objetivo es fijar cómo estas piezas afectan:

- generación documental
- snapshot operativo
- notificación
- archivo
- cierre

No define todavía tablas ni detalle físico de persistencia.

---

## Regla general

Las medidas y liberaciones no son elementos accesorios del expediente.

Tienen efecto material y operativo real sobre el flujo del sistema.

Por lo tanto, deben influir en:

- documentos a generar
- pendientes de firma
- bloqueos de notificación
- bloqueos de cierre
- visibilidad en bandejas
- archivo y cerrada
- snapshot operativo

---

## 1. Medidas preventivas

Las medidas preventivas son decisiones o actos con efecto operativo sobre el expediente que pueden requerir:

- generación documental
- firma
- notificación
- seguimiento posterior
- levantamiento futuro

### Regla de multiplicidad
Una Acta puede tener una o varias medidas preventivas.

El sistema debe poder contemplar, al menos:

- cantidad total de medidas
- cuáles están activas
- cuáles fueron levantadas
- cuáles están pendientes de firma
- cuáles siguen bloqueando avance o cierre

---

## 2. Relación con la continuidad del trámite

### Regla de notificación
Si para continuar el trámite existen documentos requeridos vinculados a medidas preventivas y alguno todavía no fue generado o firmado, el expediente no debe avanzar al proceso de notificación.

### Regla de firma
Cuando ya fueron generadas medidas preventivas u otras piezas requeridas, el expediente puede pasar a pendientes de firma si todavía falta la firma correspondiente.

---

## 3. Levantamiento de medidas

El levantamiento de una medida es un hecho relevante del expediente.

No debe confundirse:

- medida existente
- medida activa
- medida levantada
- documento de levantamiento
- ejecución material del levantamiento, si correspondiera

### Regla general
Una medida puede estar:

- vigente
- levantada
- en trámite de levantamiento
- con documentación pendiente vinculada a su levantamiento

Estas situaciones deben impactar en snapshot y en la posibilidad de cierre.

---

## 4. Pendientes materiales de liberación o restitución

Existen situaciones que no necesariamente deben tratarse como medida preventiva formal, pero que igualmente impiden el cierre material del expediente.

### Ejemplos
- rodado secuestrado pendiente de liberación
- documentación retenida pendiente de restitución
- licencia retenida pendiente de reintegro
- otra entrega material pendiente

### Regla general
La emisión del documento de liberación no equivale por sí sola a liberación material efectiva.

El sistema debe distinguir entre:

- documento generado
- documento firmado
- liberación material efectivamente realizada

---

## 5. Caso especial de tránsito

En materia de tránsito pueden existir situaciones como:

- liberación de rodado
- restitución de documentación retenida

Estas situaciones pueden requerir documento propio y también un hecho material posterior de entrega o restitución.

### Regla general
Mientras la liberación o restitución material no esté efectivamente registrada, el expediente no debe pasar a cerrada.

---

## 6. Relación con archivo y cerrada

### Regla de archivo
Un expediente puede quedar en archivo cuando el trámite principal ya está resuelto o suficientemente avanzado, pero todavía no puede pasar a cerrada porque persiste alguna situación como:

- medida preventiva activa
- pendiente material de liberación
- pendiente de restitución
- otra causal operativa equivalente

### Regla de cerrada
Un expediente solo puede pasar a cerrada cuando ya no existan:

- medidas preventivas activas
- pendientes materiales de liberación o restitución
- otras causales operativas que impidan el cierre

---

## 7. Relación con snapshot

El snapshot debe resumir, al menos:

### Medidas preventivas
- existencia
- cantidad total
- cantidad activas
- cantidad levantadas
- cantidad pendientes de firma
- indicador de que todas están firmadas
- indicador de que todas están levantadas

### Pendientes materiales
- existencia
- cantidad total
- cantidad activas
- cantidad ya liberadas o restituidas
- indicador de bloqueo de cierre

### Bloqueos operativos
- bloqueo de notificación por medidas pendientes
- bloqueo de cierre por medidas activas
- bloqueo de cierre por pendientes materiales
- posibilidad o no de pasar a cerrada

---

## 8. Regla de fuente de verdad

El detalle de medidas, levantamientos, liberaciones y restituciones no debe vivir solo en el snapshot.

La fuente de verdad debe vivir en las piezas del dominio que correspondan, por ejemplo:

- documentos
- eventos
- constancias materiales
- estados propios de la medida o liberación

El snapshot solo resume su efecto operativo.

---

## 9. Regla de UI

La UI debe permitir ver claramente, al menos:

- si existen medidas preventivas
- cuántas están activas
- si hay documentos pendientes vinculados
- si existe liberación o restitución pendiente
- qué bloquea la notificación
- qué bloquea el cierre

---

## 10. Regla de trazabilidad

Toda medida, levantamiento, liberación o restitución con efecto operativo relevante debe dejar trazabilidad suficiente en el expediente.

Esto incluye, según corresponda:

- generación documental
- firma
- notificación
- levantamiento
- entrega material
- restitución efectiva

---

## Archivos relacionados

- [Mapa de dominio](00-mapa-dominio.md)
- [Snapshot operativo](05-snapshot-operativo.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Bandeja de archivo](../03-bandejas/11-bandeja-archivo.md)
- [Bandeja de cerradas](../03-bandejas/12-bandeja-cerradas.md)
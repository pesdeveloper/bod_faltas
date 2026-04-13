# 02-servicios-documentales.md

## Finalidad

Este archivo resume los servicios de backend centrados en la gestión documental del expediente.

Su objetivo es identificar las responsabilidades principales del bloque documental dentro del backend compartido del sistema de faltas.

No define todavía endpoints, DTOs ni firmas técnicas concretas.

---

## Regla principal

El sistema de faltas se comporta como un gestor documental orientado al expediente.

Por lo tanto, el bloque documental no es accesorio: es una de las piezas centrales del backend.

Su responsabilidad es administrar las piezas documentales del expediente y su situación operativa.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- generar documentos del expediente
- recuperar documentos existentes
- relacionar documentos con el expediente
- administrar estado documental
- distinguir documentos que requieren firma o notificación
- exponer trazabilidad documental suficiente
- soportar lectura documental para web y apps del ecosistema

---

## Responsabilidades principales

### 1. Generación documental
Debe permitir producir piezas documentales del expediente, por ejemplo:

- acta
- resolución
- fallo
- medida preventiva
- documento de notificación
- documento de liberación
- otras piezas administrativas

La generación documental debe impactar en la situación operativa del expediente.

---

### 2. Gestión de estado documental
Debe permitir conocer y administrar, al menos:

- documento generado
- pendiente de firma
- firmado
- pendiente de notificación
- en notificación
- notificado
- observado
- anulado
- reemplazado

El listado definitivo se alinea con el catálogo documental vigente.

---

### 3. Asociación entre expediente y documento
Debe permitir que cada documento quede correctamente vinculado al expediente y a su contexto operativo.

Esto incluye, según corresponda:

- tipo documental
- estado
- relación con medidas, fallo o notificación
- origen o motivo documental
- trazabilidad suficiente

---

### 4. Consulta documental
Debe permitir:

- obtener documentos de un expediente
- listar documentos por situación
- identificar piezas pendientes
- recuperar metadata documental relevante
- soportar lectura resumida y detallada

---

### 5. Efecto documental sobre el expediente
Debe exponer o provocar cambios que impacten en el expediente cuando ocurran hechos como:

- documento generado
- documento firmado
- documento observado
- documento anulado
- documento listo para notificación
- documento reemplazado

El documento no debe tratarse como archivo aislado, sino como pieza con efecto operativo.

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria la lógica detallada de:

- motor de firma externo
- canales concretos de notificación
- cálculo completo de snapshot
- storage físico detallado
- agenda operativa de notificador
- gestión externa del expediente

Debe coordinar con esos bloques, no reemplazarlos.

---

## Relación con otros servicios

### Con servicios de expediente
Los documentos pertenecen al expediente y modifican su situación, pero la coordinación general del caso vive en el bloque de expediente.

### Con servicios de firma
Un documento puede requerir firma, pero la firma real se resuelve mediante integración externa o sandbox.

### Con servicios de notificación
Un documento puede convertirse en pieza notificable, pero la lógica de notificación vive en su bloque específico.

### Con servicios de snapshot
El estado documental impacta en snapshot, pero el snapshot sigue siendo derivado.

### Con storage documental
El bloque documental necesita acceso a metadata y ubicación lógica, pero no debería acoplarse al storage físico.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- generar documento
- obtener documento
- listar documentos por expediente
- cambiar estado documental
- marcar documento como pendiente de firma
- marcar documento como firmado
- marcar documento como pendiente de notificación
- marcar documento como notificado
- anular o reemplazar documento
- consultar documentos pendientes

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con UI

La UI del sistema debe poder usar este bloque para:

- ver documentos de un expediente
- identificar qué documentos existen
- saber cuáles están pendientes de firma
- saber cuáles están pendientes de notificación
- distinguir documentos activos, observados, anulados o reemplazados
- navegar el estado documental del expediente

---

## Relación con snapshot y bandejas

El bloque documental es uno de los principales proveedores de información para:

- pendientes de firma
- notificaciones
- archivo
- cierre
- badges e indicadores del expediente

No todas estas reglas viven en el bloque documental, pero su estado es clave para el cálculo operativo.

---

## Idea clave

En este sistema, un documento no es solo un archivo almacenado.

Es una pieza del expediente con efecto real sobre su situación operativa.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios de firma](03-servicios-de-firma.md)
- [Servicios de notificación](04-servicios-de-notificacion.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Catálogos y estados](../01-dominio/07-catalogos-y-estados.md)
- [Integración de storage documental](../09-integraciones/04-integracion-storage-documental.md)
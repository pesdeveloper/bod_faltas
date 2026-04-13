# 01-servicios-de-expediente.md

## Finalidad

Este archivo resume los servicios de backend centrados en la gestión del expediente / acta.

Su objetivo es identificar las responsabilidades principales del bloque de expediente dentro del backend compartido del sistema de faltas.

No define todavía endpoints, DTOs ni firmas técnicas concretas.

---

## Regla principal

El bloque de expediente es el núcleo funcional del backend.

Su responsabilidad es administrar la situación general de la Acta / Expediente y coordinar su relación con:

- eventos
- documentos
- notificaciones
- snapshot
- medidas
- bandejas
- reingresos
- archivo y cierre

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- crear expedientes / actas
- recuperar expedientes
- consultar su situación operativa
- registrar hechos relevantes del expediente
- coordinar cambios de situación
- exponer trazabilidad suficiente
- soportar lectura de expediente para las distintas apps del ecosistema

---

## Responsabilidades principales

### 1. Alta y recuperación de expediente
Debe permitir:

- registrar el expediente inicial
- recuperar expediente por identificador
- exponer datos mínimos de contexto
- soportar lectura resumida y lectura detallada

---

### 2. Consulta de situación operativa
Debe permitir conocer, al menos:

- bandeja visible actual, consumiendo snapshot como resumen operativo principal
- estado operativo general
- bloqueos relevantes
- próximos pasos habilitados
- relaciones con snapshot y documentación del expediente

---

### 3. Registro de hechos relevantes
Debe permitir registrar hechos con impacto real sobre el expediente, por ejemplo:

- generación documental
- firma
- notificación
- acuse
- presentación
- pago
- medida aplicada o levantada
- liberación material
- resultado externo
- archivo
- cierre
- paralización
- reanudación

La fuente de verdad de estos hechos no debe reducirse a una simple bandera en el expediente.

---

### 4. Coordinación de cambios de situación
Debe permitir coordinar cambios de situación del expediente sin convertir el servicio en un workflow rígido.

Debe poder reaccionar a hechos como:

- documento generado
- documento firmado
- pieza notificada
- resultado de notificación
- actuación incorporada
- medida o liberación
- reingreso externo
- decisión de archivo o cierre

---

### 5. Exposición para detalle y contexto operativo
Debe servir como base para:

- detalle completo de expediente
- lectura de pendientes y bloqueos
- contexto operativo del expediente individual
- consumo por web y apps móviles, según corresponda

Los listados, filtros y badges por bandeja pertenecen principalmente al servicio de bandejas.

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria la lógica detallada de:

- firma externa
- notificación por canal
- storage documental
- snapshot como cálculo completo
- gestión externa detallada

Debe coordinar esas piezas, no reemplazarlas.

---

## Relación con otros servicios

### Con servicios documentales
El expediente necesita conocer qué documentos tiene y qué efecto producen, pero el detalle documental vive en el bloque documental.

### Con servicios de firma
El expediente reacciona a firma pendiente o completada, pero no implementa el motor de firma.

### Con servicios de notificación
El expediente reacciona al efecto operativo de la notificación, pero no reemplaza la lógica de integración por canal.

### Con servicios de snapshot
El expediente consume y provoca cambios que impactan en snapshot, pero el cálculo o refresco puede vivir en su propio bloque.

### Con servicios de gestión externa
El expediente registra derivación y reingreso, pero el tratamiento externo detallado vive en su bloque específico.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- crear expediente
- obtener expediente resumido
- obtener expediente detallado
- consultar trazabilidad
- registrar hecho relevante
- reencauzar expediente
- pasar expediente a archivo
- pasar expediente a cerrada
- reingresar expediente al circuito
- consultar situación operativa actual

No implica que estas operaciones deban transformarse una a una en endpoints directos.

---

## Relación con bandejas

El servicio de expediente debe ser capaz de exponer la información mínima necesaria para determinar por qué un expediente aparece en una bandeja dada y qué acciones tiene habilitadas.

No debería obligar a la UI a reconstruir por su cuenta toda la lógica del expediente.

---

## Relación con snapshot

Este servicio debe trabajar en coordinación con snapshot para:

- consultar situación operativa resumida
- detectar bloqueos
- determinar visibilidad en bandejas
- habilitar acciones

El snapshot no reemplaza la lectura detallada del expediente, pero sí debe apoyar este bloque.

---

## Relación con UI

La web interna y las apps consumidoras deben poder usar este bloque para:

- ver listados de expedientes
- abrir detalle
- entender situación actual
- visualizar bloqueos y próximos pasos
- ejecutar acciones válidas sobre el expediente

Cada superficie consumirá un recorte distinto, pero el centro sigue siendo el expediente.

---

## Idea clave

El backend no se organiza alrededor de documentos aislados ni de tareas sueltas.

Se organiza alrededor del expediente y de los servicios que reflejan o modifican su situación operativa.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Servicios documentales](02-servicios-documentales.md)
- [Servicios de snapshot](05-servicios-de-snapshot.md)
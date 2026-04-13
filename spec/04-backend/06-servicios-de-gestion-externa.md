# 06-servicios-de-gestion-externa.md

## Finalidad

Este archivo resume los servicios de backend vinculados a la gestión externa del expediente.

Su objetivo es identificar las responsabilidades del backend respecto de la derivación, seguimiento, resultado y reingreso de expedientes que salen del circuito interno principal hacia una gestión externa formal.

No define todavía endpoints, DTOs ni contratos técnicos detallados.

---

## Regla principal

La gestión externa no debe modelarse como una simple salida definitiva del expediente.

El backend debe permitir:

- registrar la derivación externa
- mantener trazabilidad de esa derivación
- registrar resultados o actuaciones externas
- reingresar el expediente cuando el resultado externo tenga efecto material
- actualizar la situación operativa del expediente según corresponda

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- derivar un expediente a gestión externa
- registrar destino externo
- consultar situación externa general
- registrar actuaciones o resultados externos
- coordinar reingreso al circuito interno
- impactar en snapshot, bandejas y situación operativa del expediente

---

## Responsabilidades principales

### 1. Registro de derivación externa
Debe permitir dejar constancia de que el expediente fue derivado formalmente a una gestión externa.

Esto incluye, según corresponda:

- destino externo general
- fecha o momento de derivación
- contexto mínimo de la salida
- trazabilidad suficiente

---

### 2. Consulta de situación externa
Debe permitir conocer, al menos:

- si el expediente está en gestión externa
- cuál es el destino externo general
- si existen actuaciones o resultados registrados
- si el expediente sigue en espera o ya quedó listo para reingreso, archivo o cierre

---

### 3. Registro de resultados externos
Debe permitir registrar hechos relevantes provenientes del circuito externo, por ejemplo:

- resultado sin novedad
- necesidad de reingreso
- necesidad de nuevo fallo
- rectificación o modificación
- pago externo
- cierre externo
- otra actuación externa con efecto material

---

### 4. Reingreso al circuito interno
Debe permitir reencauzar el expediente cuando el resultado externo produzca efectos que obliguen a volver al circuito interno.

Destinos típicos de reingreso:

- análisis / presentaciones / pagos
- pendientes de fallo
- archivo
- cerrada
- otra bandeja, según la nueva situación real del expediente

---

### 5. Impacto sobre expediente y snapshot
Debe permitir que la gestión externa modifique la situación operativa del expediente cuando corresponda.

Por ejemplo, para:

- dejar visible el expediente en gestión externa
- habilitar reingreso
- cambiar bloqueos o condición final
- actualizar snapshot y bandejas

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria:

- la lógica completa del organismo externo
- el proceso interno detallado de apremios
- el proceso interno detallado del Juzgado de Paz
- decisiones jurídicas o administrativas ajenas al sistema de faltas
- integraciones técnicas de bajo nivel que no impacten en el expediente

Debe registrar y administrar el efecto externo sobre el expediente, no reemplazar el sistema externo.

---

## Relación con otros servicios

### Con servicios de expediente
La gestión externa modifica la situación del expediente, pero la coordinación general del caso sigue viviendo en el bloque de expediente.

### Con servicios de snapshot
La derivación y los resultados externos impactan en snapshot porque cambian bandeja, visibilidad y próximo paso.

### Con servicios documentales
La gestión externa puede incorporar documentos o constancias externas relevantes, pero el tratamiento documental sigue perteneciendo al bloque documental.

### Con servicios de notificación
Puede existir una secuencia donde una notificación válida habilite luego una derivación externa o donde un resultado externo genere nuevas necesidades de notificación.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- derivar expediente a gestión externa
- registrar destino externo
- consultar estado externo general
- registrar resultado externo
- registrar actuación externa
- reingresar expediente
- enviar a archivo
- enviar a cerrada

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con bandejas

Este bloque se relaciona especialmente con:

- bandeja de gestión externa
- pendientes de fallo, si el resultado exige nueva decisión
- análisis, si el resultado requiere evaluación material
- archivo, si el expediente quedó resuelto pero no cerrable
- cerrada, si ya cumple completamente las reglas de cierre

---

## Relación con la UI

La UI debe poder usar este bloque para:

- ver a qué destino externo fue derivado el expediente
- saber si existe resultado externo
- entender si el expediente debe reingresar
- identificar si quedó listo para archivo o cierre
- registrar o consultar actuaciones externas relevantes

---

## Idea clave

La gestión externa no saca al expediente del sistema.

Lo mantiene trazable y listo para producir efectos materiales sobre su situación operativa, incluyendo reingreso cuando corresponda.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios de snapshot](05-servicios-de-snapshot.md)
- [Bandeja de gestión externa](../03-bandejas/09-bandeja-gestion-externa.md)
- [Bandeja de pendientes de fallo](../03-bandejas/05-bandeja-pendientes-fallo.md)
- [Bandeja de archivo](../03-bandejas/11-bandeja-archivo.md)
- [Bandeja de cerradas](../03-bandejas/12-bandeja-cerradas.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
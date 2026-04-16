# 02-servicios-documentales.md

## Finalidad

Este archivo define las responsabilidades del backend vinculadas al bloque documental del sistema.

Su objetivo es dejar claro qué debe resolver el backend respecto de producción, estado y consulta de documentos del expediente, sin confundir documento lógico con almacenamiento físico ni con el motor de firma.

---

## Regla principal

El backend documental debe tratar al documento como una entidad lógica del expediente.

Debe poder resolver:

- su generación
- su numeración cuando corresponda
- su estado documental
- su relación con el expediente
- su referencia técnica de almacenamiento mediante `StorageKey`

El archivo firmado reemplaza al archivo previo no firmado dentro del storage documental principal.

La gestión documental no requiere conservar simultáneamente múltiples versiones materiales del mismo documento dentro del circuito operativo estándar.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- generar documentos del expediente
- consultar documentos existentes
- conocer su estado documental
- vincular documentos con el expediente
- resolver si requieren firma
- persistir su referencia de storage
- exponer contexto documental para otras capas del backend

---

## Responsabilidades principales

### 1. Producción documental
Debe permitir:

- generar el documento lógico
- registrar tipo documental
- registrar estado documental
- vincularlo al expediente
- dejar preparado el documento para numeración o firma si corresponde

---

### 2. Relación con numeración
La numeración documental debe resolverse mediante:

- talonario, cuando corresponda
- política de numeración aplicable al talonario

La lógica de numeración no debe quedar embebida dentro del documento como política propia.

---

### 3. Relación con storage
El documento no debe quedar acoplado a una ruta física.

Debe resolverse mediante:

- `StorageKey`
- backend de storage
- metadata técnica asociada

El backend documental debe trabajar con referencia desacoplada y no con paths absolutos embebidos en la lógica de dominio.

---

### 4. Estado documental
Debe permitir conocer, por ejemplo:

- si el documento está en borrador
- si fue generado
- si está pendiente de firma
- si fue firmado
- si fue incorporado firmado
- si fue anulado

---

### 5. Consulta documental
Debe permitir:

- recuperar documentos de un expediente
- conocer el rol del documento en el expediente
- identificar documento principal o accesorio
- consultar metadata documental mínima

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria:

- la lógica de firma externa
- la lógica de notificación por canal
- la lógica de snapshot
- la lógica de bandejas
- la resolución física final del archivo más allá de la referencia por `StorageKey`

---

## Relación con otros servicios

### Con expediente
El documento siempre forma parte del expediente y debe quedar ligado a su contexto operativo.

### Con firma
El backend documental debe exponer qué documento requiere firma y actualizar su estado según el resultado recibido, sin reemplazar el motor de firma.

### Con notificación
El backend documental provee el documento notificable, pero la notificación pertenece a su propio bloque transversal.

### Con storage
El backend documental debe apoyarse en el bloque de storage para ubicar físicamente el archivo, sin modelar el storage como parte del documento lógico.

---

## Idea clave

El sistema no gestiona “archivos sueltos”.

Gestiona documentos lógicos del expediente, cuyo soporte material se resuelve técnicamente por storage desacoplado y cuya firma se integra con un servicio externo cuando corresponde.
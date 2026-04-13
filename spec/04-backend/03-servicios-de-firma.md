# 03-servicios-de-firma.md

## Finalidad

Este archivo resume los servicios de backend vinculados al tratamiento de firma de documentos del expediente.

Su objetivo es identificar las responsabilidades del backend de faltas respecto de la firma, sin confundirlas con el motor de firma real, que es externo.

No define todavía endpoints, DTOs ni contratos técnicos detallados.

---

## Regla principal

El backend de faltas no implementa el motor de firma.

Su responsabilidad es:

- identificar documentos que requieren firma
- reflejar el estado de firma de esos documentos
- integrarse con el motor externo o con sandbox/testing
- reaccionar al resultado de firma
- actualizar documento, expediente y snapshot según corresponda

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- detectar documentos pendientes de firma
- exponer al sistema cuáles son las firmas requeridas
- registrar o reflejar resultado de firma
- coordinar el cambio de estado documental
- producir el efecto operativo de la firma sobre el expediente
- soportar modo sandbox / testing en primera etapa

---

## Responsabilidades principales

### 1. Identificación de documentos firmables
Debe permitir determinar qué documentos del expediente requieren firma y cuáles aún la tienen pendiente.

Esto incluye, según corresponda:

- fallos
- resoluciones
- medidas preventivas
- documentos notificables firmables
- otras piezas documentales definidas por regla de negocio

---

### 2. Gestión de estado de firma
Debe permitir reflejar estados o resultados equivalentes a:

- pendiente de firma
- firmado
- rechazado
- error
- cancelado

El detalle final de estados deberá alinearse con el contrato real del motor de firma o con la simulación sandbox.

---

### 3. Reacción al resultado de firma
Debe permitir que, cuando un documento pase a firmado o a otro estado relevante, el sistema de faltas pueda:

- actualizar el documento
- registrar trazabilidad
- recalcular o actualizar snapshot
- habilitar el siguiente paso operativo del expediente
- modificar la visibilidad del expediente en bandejas cuando corresponda

---

### 4. Coordinación con expedientes con múltiples firmas
Un expediente puede tener varios documentos pendientes de firma al mismo tiempo.

Este bloque debe poder reflejar si:

- existe al menos una firma pendiente
- se completó o no el conjunto de firmas necesarias para continuar
- el expediente sigue en pendientes de firma
- ya puede pasar a notificación, archivo o cierre según corresponda

---

### 5. Soporte de sandbox / testing
Durante la etapa inicial, debe permitir una simulación controlada del resultado de firma para:

- pruebas de bandejas
- pruebas de snapshot
- pruebas de cambio de estado documental
- pruebas de continuidad del expediente

El sandbox no reemplaza la integración real, pero debe producir los mismos efectos operativos dentro de faltas.

---

## Qué no debe hacer

Este bloque no debe absorber de forma innecesaria la lógica técnica del sistema externo de firma, por ejemplo:

- cola general del motor de firma
- estampado PDF
- numeración documental transversal
- integración con token, HSM o firma remota
- UI del programa firmador
- detalle técnico del agente local o proceso externo

---

## Relación con otros servicios

### Con servicios documentales
La firma modifica el estado de documentos del expediente, pero el documento sigue siendo administrado por el bloque documental.

### Con servicios de expediente
El expediente cambia su situación operativa cuando cambian los resultados de firma, pero la coordinación general del caso vive en el bloque de expediente.

### Con servicios de snapshot
La firma puede habilitar el siguiente paso del expediente y por eso debe impactar en snapshot.

### Con integraciones
La firma real se resuelve mediante integración externa o sandbox, no dentro de la lógica propia del dominio de faltas.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- consultar documentos pendientes de firma
- marcar documento como pendiente de firma
- registrar resultado de firma
- reflejar documento firmado
- reflejar documento rechazado
- consultar si el expediente ya completó las firmas necesarias para continuar
- simular firma en modo sandbox

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con bandejas

Este bloque se relaciona especialmente con:

- bandeja de pendientes de firma
- bandeja de notificaciones
- archivo
- cerrada

porque una firma pendiente o completada puede cambiar de forma directa la visibilidad operativa del expediente.

---

## Relación con la UI

La UI debe poder usar este bloque para:

- ver qué expedientes tienen firmas pendientes
- ver qué documentos requieren firma
- saber si existe más de una firma pendiente
- saber si al completar la firma el expediente ya puede continuar
- accionar la simulación sandbox cuando corresponda

---

## Idea clave

El backend de faltas no firma documentos.

Refleja y administra el efecto operativo de la firma sobre documentos y expedientes.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios documentales](02-servicios-documentales.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Bandeja de pendientes de firma](../03-bandejas/06-bandeja-pendientes-firma.md)
- [Reglas de firma](../02-reglas-transversales/01-reglas-de-firma.md)
- [Integración con motor de firma](../09-integraciones/01-integracion-motor-firma.md)
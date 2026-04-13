# 01-reglas-de-firma.md

## Finalidad

Este archivo fija las reglas generales del proceso de firma dentro del sistema de faltas.

La firma se entiende como un hecho documental con efecto operativo sobre el expediente.

---

## Regla principal

El sistema de faltas no firma documentos por sí mismo.

El sistema de faltas:

- genera o administra documentos del expediente
- refleja que un documento requiere firma
- reacciona al resultado de firma
- actualiza estado documental, snapshot y situación operativa del expediente

El motor de firma se considera externo al sistema de faltas.

---

## Qué documentos pueden requerir firma

El sistema debe contemplar que ciertas piezas documentales del expediente requieran firma para producir efectos válidos o para habilitar continuidad operativa.

Entre ellas, según corresponda:

- fallos
- resoluciones
- medidas preventivas
- documentos notificables firmables
- otras piezas administrativas que la regla de negocio defina

El listado definitivo de tipos documentales firmables deberá alinearse con el catálogo documental vigente.

---

## Regla de pendiente de firma

Cuando un documento del expediente ya fue generado pero todavía no cuenta con la firma requerida, el expediente puede quedar visible como expediente con pendientes de firma.

La unidad operativa sigue siendo el expediente / acta, no el documento aislado.

---

## Regla de multiplicidad

Un expediente puede tener uno o varios documentos pendientes de firma al mismo tiempo.

El sistema debe permitir reflejar, al menos:

- si existen pendientes de firma
- cuántos documentos relevantes faltan firmar
- si el conjunto necesario para continuar ya está completo o no

Mientras falte alguna firma requerida para el siguiente paso, el expediente debe seguir tratado como expediente con pendientes de firma.

---

## Regla de efecto sobre el expediente

La firma de un documento no es un hecho aislado.

Cuando un documento del expediente pasa a firmado, el sistema debe poder:

- actualizar el estado del documento
- registrar trazabilidad del hecho
- recalcular o actualizar snapshot
- habilitar el siguiente paso operativo, si corresponde
- modificar la visibilidad del expediente en bandejas cuando aplique

---

## Regla de continuidad

La firma puede habilitar distintos efectos según el tipo de documento firmado.

Ejemplos típicos:

- habilitar notificación
- completar el paquete documental mínimo requerido
- permitir pase a archivo
- permitir pase a cerrada
- mantener el expediente pendiente por otras condiciones aún no resueltas

No existe un único efecto obligatorio para todas las firmas.

---

## Regla de firma completa para continuar

Cuando el expediente dependa de varias piezas firmables para avanzar, el sistema no debe asumir continuidad solo por una firma parcial.

La continuidad se habilita recién cuando se complete el conjunto de firmas necesarias para el siguiente paso operativo.

Esto aplica especialmente cuando existan:

- varias medidas preventivas
- varias resoluciones asociadas
- varias piezas documentales previas a notificación

---

## Regla de firma y notificación

Si una pieza requiere firma antes de poder notificarse, el expediente no debe pasar al proceso de notificación mientras esa firma no esté completa.

Por lo tanto:

- documento generado sin firma → puede dejar el expediente en pendientes de firma
- documento firmado → puede habilitar pase a notificaciones, si no existen otros bloqueos

---

## Regla de firma y snapshot

El snapshot operativo debe resumir, al menos:

- si el expediente tiene documentos pendientes de firma
- si existen firmas requeridas aún incompletas
- si ya se completó el conjunto documental firmado necesario para continuar
- cuál es el bloqueo operativo derivado de la falta de firma, si existe

---

## Regla de trazabilidad

Toda firma relevante debe dejar trazabilidad suficiente en el expediente.

Esto incluye, según corresponda:

- documento afectado
- fecha relevante
- resultado
- cambio de estado documental
- efecto sobre la situación operativa del expediente

---

## Relación con el motor de firma

El sistema de faltas debe integrarse con un sistema externo de firma o con un modo sandbox/testing.

En ambos casos, el efecto esperado dentro del sistema de faltas es el mismo:

- el documento cambia de situación
- el expediente actualiza su estado operativo
- el snapshot se recalcula o actualiza
- se habilita, o no, el siguiente paso

---

## Relación con la UI

La UI debe permitir ver con claridad:

- qué expedientes tienen pendientes de firma
- qué documentos faltan firmar
- si existe más de un documento pendiente
- si al completarse las firmas el expediente ya queda listo para continuar
- qué acción o bandeja se habilita después

---

## Idea clave

El expediente no se envía a una bandeja para recién ser firmado.

El expediente queda visible como expediente con pendientes de firma porque ya existen documentos generados cuya firma todavía no se completó.

---

## Archivos relacionados

- [Bandeja de pendientes de firma](../03-bandejas/06-bandeja-pendientes-firma.md)
- [Integración con motor de firma](../09-integraciones/01-integracion-motor-firma.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Regla del sistema como gestor documental](00-regla-sistema-como-gestor-documental.md)
- [Catálogos y estados](../01-dominio/07-catalogos-y-estados.md)
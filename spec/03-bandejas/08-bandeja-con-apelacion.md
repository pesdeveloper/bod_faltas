# 08-bandeja-con-apelacion.md

## Finalidad

Esta bandeja reúne expedientes en los que efectivamente existe apelación o recurso equivalente.

Su función es dar visibilidad específica a los casos en los que el expediente entra en un tramo recursivo o de revisión posterior.

La visibilidad de esta bandeja puede apoyarse también en plazos proyectados del expediente, especialmente cuando exista apelación presentada o apelación todavía habilitada dentro del plazo aplicable.

---

## Qué contiene

Contiene expedientes en los que:

- se presentó apelación
- se admitió un recurso equivalente
- existe una actuación recursiva con efecto real sobre el expediente

La apelación no se trata como paso lineal obligatorio del flujo.  
Solo existe cuando efectivamente se presenta.

---

## Qué no contiene

No contiene expedientes que todavía no tengan apelación real.

No contiene expedientes que solo están esperando plazo post fallo sin actuación recursiva concreta.

Esos casos deben resolverse por estado, filtro o permanencia en la bandeja que corresponda.

---

## Función operativa

Su función es permitir ver y tratar expedientes cuyo curso cambió por una apelación o recurso.

Desde aquí puede definirse, según el caso:

- nueva resolución o redacción
- nuevo fallo
- pase a firma
- nueva notificación
- gestión externa
- archivo
- cierre
- paralización

---

## Regla de entrada

Un expediente entra a esta bandeja cuando se registra efectivamente una apelación o recurso equivalente con efecto operativo sobre el expediente.

Típicamente ingresa luego de una etapa previa de fallo o acto notificable, cuando la actuación recursiva queda formalmente incorporada.

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras la apelación o el recurso no haya producido todavía una decisión suficiente para definir su siguiente situación operativa.

---

## Regla de salida

La salida de esta bandeja depende del resultado del tratamiento recursivo.

Destinos típicos:

- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- gestión externa
- archivo
- cerradas
- paralizadas

No existe una única salida obligatoria para todos los casos con apelación.

---

## Acciones típicas

- analizar apelación
- registrar decisión sobre recurso
- reencauzar a resolución / redacción
- reencauzar a fallo
- enviar a pendientes de firma
- enviar a notificaciones
- enviar a gestión externa
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Relación con fallo

La apelación suele relacionarse con etapas posteriores a fallo, pero no debe forzarse como una continuación obligatoria del flujo.

Solo debe aparecer cuando exista una actuación recursiva real.

También puede derivar en:

- nuevo fallo
- rectificación
- modificación
- nueva notificación
- confirmación sin nuevas piezas
- otra consecuencia válida según el caso

---

## Relación con gestión externa

Según el resultado del tratamiento recursivo, el expediente puede:

- volver al circuito interno
- pasar a gestión externa
- quedar archivado
- quedar cerrado

Esto dependerá del efecto material de la apelación sobre el expediente.

---

## Relación con snapshot

El snapshot debe permitir identificar, al menos:

- que el expediente tiene apelación o recurso activo
- si la apelación sigue pendiente de resolución
- si el expediente ya quedó listo para reencauzarse a otra bandeja

---

## Relación con la UI

La UI debe permitir ver con claridad:

- que existe apelación
- en qué estado general se encuentra
- qué efecto puede producir
- qué acciones están habilitadas
- cuál será el siguiente paso del expediente una vez resuelta

---

## Idea clave

La apelación no es una etapa obligatoria del sistema.

Es una situación especial del expediente que solo existe cuando efectivamente aparece una actuación recursiva real.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de pendientes de fallo](05-bandeja-pendientes-fallo.md)
- [Bandeja de notificaciones](07-bandeja-notificaciones.md)
- [Bandeja de gestión externa](09-bandeja-gestion-externa.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
# 03-bandeja-analisis-presentaciones-pagos.md

## Finalidad

Esta bandeja reúne expedientes con presentaciones, pagos, solicitudes o novedades que requieren análisis material.

Su función es concentrar los casos donde el expediente ya no está solo en revisión inicial documental, sino que necesita una evaluación de fondo sobre actuaciones o hechos incorporados.

---

## Qué contiene

Contiene expedientes que presentan una o más situaciones como estas:

- solicitud de pago voluntario
- descargo
- presentación espontánea
- pago registrado o informado
- antecedentes o novedades incorporadas
- actuación que exige decidir el siguiente paso material del expediente

---

## Qué no contiene

No contiene expedientes cuya necesidad actual sea solo:

- completar documentación inicial
- esperar firmas
- gestionar notificación
- permanecer en archivo por bloqueo de cierre

Esas situaciones pertenecen a otras bandejas.

---

## Función operativa

Su función es permitir analizar hechos o actuaciones que pueden modificar el curso del expediente.

Desde aquí se determina, por ejemplo, si corresponde:

- producir una resolución
- producir un fallo
- generar una pieza firmable
- iniciar o continuar notificación
- enviar a archivo
- pasar a cerrada
- paralizar
- reencauzar el expediente a otra bandeja

---

## Regla de entrada

Un expediente entra a esta bandeja cuando se incorpora una actuación o novedad que exige análisis material.

Puede ingresar, por ejemplo, por:

- solicitud de pago voluntario
- presentación o descargo
- pago
- novedad relevante posterior
- reencauce desde otra bandeja
- resultado de notificación
- resultado de gestión externa
- levantamiento de paralización

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras la actuación o novedad incorporada no haya sido suficientemente analizada como para definir su siguiente situación operativa.

---

## Regla de salida

La salida de esta bandeja se habilita cuando el análisis material ya permitió definir el próximo documento, decisión o situación operativa del expediente.

La salida no es única. Depende del resultado del análisis.

Destinos típicos:

- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- archivo
- cerradas
- paralizadas

---

## Acciones típicas

- analizar presentación
- analizar descargo
- analizar solicitud de pago voluntario
- analizar pago
- definir próximo documento o decisión
- enviar a pendientes de resolución / redacción
- enviar a pendientes de fallo
- enviar a pendientes de firma
- enviar a notificaciones
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Relación con pago voluntario

Esta bandeja es la principal candidata para agrupar expedientes que solicitaron o registraron pago voluntario.

Debe permitir ver con claridad:

- si la solicitud existe
- si ya hubo pago
- si el pago fue suficiente
- si todavía se requiere otra actuación
- si el expediente puede seguir a otra etapa o quedar archivado / cerrado según corresponda

---

## Relación con notificación

Un expediente puede llegar a esta bandeja desde notificaciones cuando el resultado de una diligencia o la falta de acuse obliga a una decisión material o a un reencauce.

La bandeja no reemplaza la lógica de notificación, pero sí absorbe los casos donde la notificación ya produjo una consecuencia que requiere análisis.

---

## Relación con archivo y cerrada

Desde esta bandeja puede definirse que:

- el expediente todavía necesita otra actuación
- el expediente debe pasar a archivo porque el trámite principal ya está resuelto pero no puede cerrarse
- el expediente ya cumple completamente las reglas de cierre

La decisión dependerá del estado real del expediente y de sus bloqueos.

---

## Relación con snapshot

El snapshot debe permitir identificar, al menos:

- que el expediente está en análisis
- cuál es la actuación o motivo principal que justifica su permanencia aquí
- si existe pago o presentación relevante
- si el expediente ya quedó listo para otra bandeja

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- qué actuación motivó el ingreso
- qué análisis está pendiente
- si existe pago o presentación
- qué decisiones están habilitadas
- cuál sería el siguiente paso del expediente

---

## Idea clave

Esta bandeja no es solo una “espera”.

Es el lugar donde el expediente queda visible cuando una presentación, pago o novedad exige una evaluación material para definir su siguiente paso operativo.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de enriquecimiento](02-bandeja-enriquecimiento.md)
- [Bandeja de pendientes de resolución / redacción](04-bandeja-pendientes-resolucion-redaccion.md)
- [Bandeja de pendientes de fallo](05-bandeja-pendientes-fallo.md)
- [Bandeja de notificaciones](07-bandeja-notificaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
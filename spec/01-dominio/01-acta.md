# Acta

## Finalidad

El Acta es la unidad principal de gestión del sistema.

Sobre el Acta / Expediente se organiza la trazabilidad del caso, la producción documental, las notificaciones, las medidas, las presentaciones y las decisiones administrativas posteriores.

---

## Qué representa en el sistema

El Acta representa la apertura e identidad principal del expediente administrativo de faltas.

Es el objeto central alrededor del cual el sistema registra:

- su estado operativo actual
- su historial de eventos
- los documentos asociados
- las notificaciones emitidas y sus resultados
- las medidas y liberaciones vinculadas
- las actuaciones y presentaciones que impactan en su evolución

El sistema no se organiza alrededor de una multa aislada ni de un documento aislado, sino alrededor del expediente cuyo eje es el Acta.

La identidad del Acta no se agota en una sola referencia: el expediente puede requerir simultáneamente una identidad interna del sistema, una identidad técnica de origen para captura/sincronización y una identidad administrativa visible para operación y soporte documental.

---

## Relación con el expediente

En este sistema, Acta y Expediente forman la misma unidad principal de gestión.

El Acta constituye la identidad base del expediente y permite unificar en una sola referencia operativa:

- el seguimiento administrativo
- la consulta documental
- la trazabilidad
- la operación por bandejas
- la integración con procesos transversales

---

## Identidad del acta

El Acta puede convivir con distintos planos de identidad, cada uno con una finalidad distinta dentro del sistema.

### Identidad interna

Corresponde a la identidad propia del sistema para persistencia, relación y operación interna.

### Identidad técnica de origen

Corresponde a la identidad generada en captura, especialmente en escenarios móviles u offline, y permite independencia respecto de la base central, sincronización posterior y correlación técnica del expediente desde su origen.

### Identidad administrativa visible

Corresponde al número de acta utilizado en la operatoria administrativa y documental.

Esta identidad visible puede provenir de políticas de numeración asociadas a talonarios de actas, incluyendo modalidades automáticas o manuales/preimpresas según la dependencia responsable.

El talonario de actas debe entenderse vinculado a la dependencia y no al inspector en forma individual.

El inspector se relaciona con el acta como actor del labrado o intervención, pero no como titular de la numeración administrativa del acta.

Estas identidades no se reemplazan entre sí: conviven porque resuelven necesidades distintas del expediente.

---

## Reglas principales

- El Acta es la referencia principal del caso dentro del sistema.
- El historial del caso no vive en el Acta, sino en los eventos asociados.
- El snapshot operativo no reemplaza al Acta ni al historial; es una proyección derivada.
- Los documentos, notificaciones y demás piezas del expediente se vinculan al Acta como unidad central.
- Archivo y cerrada no son equivalentes: una actuación puede archivarse sin agotar todas las nociones de cierre administrativo del expediente.
- El motor de firma no forma parte del dominio interno del Acta; solo existe integración externa cuando corresponde firmar documentos del expediente.
- La numeración administrativa del acta no reemplaza la identidad interna ni la identidad técnica de origen.
- Cuando la operatoria lo requiera, la numeración del acta puede depender de talonarios asociados a dependencias y de políticas de numeración definidas externamente a la entidad misma.
- El inspector interviene en el expediente como actor operativo del labrado o actuación, pero no como responsable de la asignación del talonario de actas.

---

## Qué no es

El Acta no es:

- el historial completo del caso
- la bandeja operativa
- el snapshot regenerable
- la notificación
- el documento
- el proceso de firma

Tampoco debe asumirse como una simple cabecera de multa o de movimiento económico.

---

## Relaciones clave

El Acta se relaciona conceptualmente con:

- ActaEvento, como fuente principal de trazabilidad
- Documento, como soporte documental del expediente
- Notificacion, como proceso transversal de comunicación formal
- Snapshot operativo, como proyección regenerable para operación
- Medidas, liberaciones, presentaciones y decisiones, como elementos que modifican o acompañan la evolución del expediente
- Dependencia, como ámbito organizacional competente y como fuente de contexto operativo inicial del expediente
- Inspector, como actor interviniente en el labrado o actuación sobre el acta, asignado a una dependencia
- Talonarios de actas, como mecanismo de numeración administrativa visible cuando la operatoria lo requiera
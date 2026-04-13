# Notificacion

## Finalidad

Notificacion es la pieza de dominio que representa el proceso formal de comunicación de una actuación, documento, decisión o resultado del expediente hacia uno o más destinatarios.

Su función es dejar trazado, de manera uniforme, el circuito de emisión, envío, recepción, acuse, rechazo, incidencia, resultado y eventual reintento de una comunicación formal.

---

## Qué representa en el sistema

Notificacion representa una operación formal de comunicación vinculada al expediente.

Puede utilizarse para comunicar, según corresponda:

- el acta
- un acto administrativo
- una resolución
- un fallo
- una constancia
- otra pieza o resultado que deba ponerse formalmente en conocimiento

La notificación no se limita a “mandar un documento”, sino que modela el proceso completo de comunicación formal y su resultado.

---

## Relación con el expediente

Toda Notificacion existe vinculada a un expediente.

La notificación actúa como proceso transversal sobre el expediente y puede afectar su evolución operativa según:

- el objeto notificado
- el canal utilizado
- el resultado obtenido
- la existencia o no de acuse
- la necesidad de reintento, corrección, nueva preparación o cambio de curso

El expediente puede requerir múltiples notificaciones a lo largo de su vida.

---

## Reglas principales

- La notificación es un proceso transversal único del sistema.
- Notificación y documento no son equivalentes, aunque puedan estar vinculados estrechamente.
- Una notificación puede tener como objeto un documento, una decisión o un resultado formal del expediente.
- El resultado de la notificación puede producir efectos sobre la evolución operativa del expediente.
- La notificación debe permitir trazabilidad de emisión, envío, recepción, acuse, rechazo, incidencia y resultado.
- La notificación puede requerir reintentos, cambios de canal o tratamiento posterior según las reglas aplicables.
- El detalle de canales, acuses, plazos y reglas operativas pertenece a las reglas transversales y no se redefine en esta pieza conceptual.

---

## Qué no es

Notificacion no es:

- el expediente completo
- el documento en sí mismo
- una simple salida técnica de mensajería
- una bandeja
- un snapshot
- una mera marca de “enviado”

Tampoco debe confundirse con el storage documental ni con la generación del documento notificable.

---

## Objeto de notificación

La notificación recae sobre un objeto notificable del expediente.

Ese objeto puede ser:

- un documento concreto
- una actuación formal
- un acto administrativo
- un resultado que deba comunicarse

La definición exacta de qué objetos son notificables y bajo qué reglas pertenece al bloque de reglas transversales.

---

## Resultado y trazabilidad

La notificación debe dejar trazado, como mínimo en términos de dominio:

- qué se notificó
- a quién o a qué destino se intentó notificar
- por qué canal
- en qué estado o resultado quedó
- qué incidencias ocurrieron
- si hubo acuse, rechazo, imposibilidad, reintento o cierre del proceso

La historia funcional de la notificación puede generar eventos relevantes dentro del expediente.

---

## Relación con documento

Un documento puede ser el objeto principal de una notificación, pero la notificación no se agota en el documento.

El documento expresa contenido formal del expediente.  
La notificación expresa el proceso formal de comunicación de ese contenido.

Por eso:

- un documento puede existir sin haber sido notificado
- una notificación puede referirse a un documento o a otro objeto formal
- el resultado de la notificación debe modelarse de manera separada del ciclo documental

---

## Relación con eventos y snapshot

La notificación puede generar hechos relevantes para la trazabilidad del expediente, por lo que sus resultados significativos pueden reflejarse en ActaEvento.

A su vez, el snapshot operativo puede derivar parte de su estado actual a partir de la situación de las notificaciones en curso, pendientes, fallidas o concluidas.

Sin embargo, ni el evento ni el snapshot reemplazan a la notificación como pieza de dominio específica.

---

## Relaciones clave

Notificacion se relaciona conceptualmente con:

- Acta, como expediente al que pertenece
- Documento, cuando exista un objeto documental notificable
- ActaEvento, cuando la notificación produzca hechos relevantes de trazabilidad
- snapshot operativo, cuando su resultado impacte en el estado visible del expediente
- integraciones externas de mensajería, correo, cédula, publicación u otros canales admitidos
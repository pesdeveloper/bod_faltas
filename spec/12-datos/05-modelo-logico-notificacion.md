# Modelo lógico — Notificacion

## Finalidad

`Notificacion` representa el proceso formal de comunicación de un objeto notificable del expediente.

Permite registrar emisión, envío, recepción, acuse, rechazo, incidencia, reintento y resultado, sin confundir la notificación con el documento ni con el expediente.

---

## Tabla principal

### `Notificacion`

Debe contener, como mínimo a nivel lógico:

- `Id`
- referencia obligatoria a `Acta`
- tipo u objeto notificable
- estado principal de notificación
- canal utilizado
- destino o destinatario
- fechas relevantes del proceso
- resultado principal
- referencias opcionales a documento, evento o integración involucrada
- metadatos básicos de registración

---

## Qué guarda

`Notificacion` debe guardar la identidad y metadata principal del proceso de notificación, incluyendo cuando corresponda:

- qué se notifica
- a quién se intenta notificar
- por qué canal
- en qué estado se encuentra
- qué resultado produjo
- si hubo acuse, rechazo, incidencia o reintento
- qué pieza documental o acto está involucrado

---

## Qué no guarda

`Notificacion` no debe usarse para guardar:

- el expediente completo
- el contenido completo del documento notificado
- toda la trazabilidad del expediente
- bitácora técnica de mensajería de bajo nivel
- snapshot operativo completo
- semántica completa del sistema externo de notificación

Tampoco debe reducirse a una simple marca de “enviado”.

---

## Reglas principales

- Toda `Notificacion` pertenece a un expediente.
- La notificación es un proceso transversal separado del documento y del expediente.
- El objeto notificable puede ser un documento, un acto o un resultado formal del expediente.
- Una notificación puede existir en distintas etapas antes de tener resultado final.
- El resultado de la notificación puede impactar en la evolución operativa del expediente.
- Los hitos relevantes de la notificación pueden reflejarse en `ActaEvento`, pero no la reemplazan.
- La notificación puede requerir reintentos, cambio de canal o tratamiento posterior según las reglas del caso.

---

## Objeto notificable

La notificación debe poder identificar con claridad qué objeto del expediente se está comunicando.

Ese objeto puede ser, según corresponda:

- un documento
- un acto administrativo
- una resolución o fallo
- una constancia
- otro resultado formal notificable

La definición exacta de objetos notificables pertenece a reglas y catálogos del sistema.

---

## Resultado y trazabilidad

La notificación debe permitir registrar, como mínimo:

- emisión o preparación
- envío o salida efectiva
- recepción o acuse, si existe
- rechazo, imposibilidad o incidencia
- reintento
- resultado final

Cuando esos hitos tengan relevancia para el expediente, podrán reflejarse también en eventos.

---

## Relaciones clave

`Notificacion` se relaciona con:

- `Acta`, como expediente al que pertenece
- `Documento`, cuando exista pieza documental notificable
- `ActaEvento`, cuando la notificación produzca hechos relevantes de trazabilidad
- integraciones externas de mensajería, correo, cédula, publicación u otros canales admitidos
- snapshot operativo, cuando su estado impacte en la lectura actual del expediente

---

## Criterio de compactación

`Notificacion` debe mantenerse como entidad principal del proceso de comunicación formal.

Cuando crezcan demasiado los detalles de destinos, acuses, incidencias o integraciones, deben separarse en anexos o entidades auxiliares en lugar de sobrecargar la tabla principal.
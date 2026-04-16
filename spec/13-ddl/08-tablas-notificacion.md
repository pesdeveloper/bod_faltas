# Tablas notificación

## Finalidad

Este archivo define el bloque físico de notificación del sistema.

Incluye:

- `Notificacion`
- `NotificacionIntento`
- `NotificacionAcuse`
- `NotificacionObservacion`

Este bloque resuelve la gestión de notificación como proceso transversal único, desacoplado del documento lógico pero vinculado a él cuando corresponde.

---

## Criterios generales del bloque

- la notificación es un proceso transversal único del dominio
- una notificación puede referir a:
  - acta
  - acto administrativo
  - u otro documento notificable del circuito
- la notificación no reemplaza al documento
- una notificación puede tener:
  - uno o más intentos
  - acuse o ausencia de acuse
- cada intento se envía a un único destino efectivo
- el resultado notificatorio debe poder proyectarse luego en snapshot

---

## Tabla: Notificacion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdNotif` | `INT8` | No | PK |
| `TipoNotif` | `SMALLINT` | No | Tipo de notificación |
| `EstadoNotif` | `SMALLINT` | No | Estado actual de notificación |
| `IdDocu` | `INT8` | No | Documento notificado |
| `IdActa` | `INT8` | No | Acta relacionada |
| `FhGeneracion` | `DATETIME YEAR TO SECOND` | Sí | Momento de generación de la notificación |
| `FhEmision` | `DATETIME YEAR TO SECOND` | Sí | Momento de emisión efectiva |
| `CanalNotif` | `SMALLINT` | No | Canal principal |
| `SiRequiereAcuse` | `SMALLINT` | No | 0/1 |
| `SiAcuseRecibido` | `SMALLINT` | No | 0/1 |
| `EstadoAcuse` | `SMALLINT` | Sí | Estado resumido del acuse |
| `FhAcuse` | `DATETIME YEAR TO SECOND` | Sí | Momento del acuse si existe |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- `Notificacion` representa la entidad transversal de notificación.
- Lo que se notifica siempre es un documento.
- En este modelo, toda notificación pertenece a una acta.
- `CanalNotif` representa el canal principal de la notificación.
- `SiRequiereAcuse` permite distinguir circuitos donde el acuse no resulta necesario.
- `SiAcuseRecibido`, `EstadoAcuse` y `FhAcuse` son datos resumidos útiles para acceso rápido.
- El control de plazos posteriores debe resolverse desde la fecha de notificación/acuse fehaciente, no desde un vencimiento propio de la entidad notificación.

---

## Tabla: NotificacionIntento

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdNotif` | `INT8` | No | Notificación |
| `NroIntento` | `SMALLINT` | No | Número de intento |
| `CanalNotif` | `SMALLINT` | No | Canal usado en el intento |
| `TipoDestNotif` | `SMALLINT` | No | Tipo de destino |
| `DestNotif` | `VARCHAR(150)` | No | Destino efectivo utilizado |
| `EstadoIntento` | `SMALLINT` | No | Estado del intento |
| `FhIntento` | `DATETIME YEAR TO SECOND` | No | Momento del intento |
| `FhResultado` | `DATETIME YEAR TO SECOND` | Sí | Momento del resultado si existe |
| `ResultadoIntento` | `VARCHAR(255)` | Sí | Resultado breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Cada intento concreto de notificación queda registrado acá.
- Esta tabla permite reintentos sin perder trazabilidad.
- El destino efectivo utilizado se persiste directamente en el intento.
- No se requiere una tabla separada de destinos si la notificación se envía a un único destino por vez.
- El acuse formal, si existe, se documenta en `NotificacionAcuse`.

---

## Tabla: NotificacionAcuse

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdNotif` | `INT8` | No | Notificación |
| `IdIntentoNotif` | `INT8` | Sí | Intento asociado si corresponde |
| `TipoAcuse` | `SMALLINT` | No | Tipo de acuse |
| `EstadoAcuse` | `SMALLINT` | No | Estado del acuse |
| `FhAcuse` | `DATETIME YEAR TO SECOND` | No | Fecha/hora del acuse |
| `StorageKeyAcuse` | `VARCHAR(255)` | Sí | Ubicación de constancia/archivo si aplica |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla documenta el acuse formal o su constancia.
- `StorageKeyAcuse` es opcional.
- No todos los canales producirán el mismo tipo de acuse.
- El estado resumido del acuse puede proyectarse luego en `Notificacion`.

---

## Tabla: NotificacionObservacion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdNotif` | `INT8` | No | Notificación |
| `ObsNotif` | `VARCHAR(255)` | No | Observación breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Fecha/hora alta |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla documenta observaciones propias del circuito de notificación.
- No reemplaza el uso de eventos del expediente cuando el cambio tenga impacto procesal.

---

## Reglas generales del bloque

- Una notificación puede tener múltiples intentos.
- Cada intento se envía a un único destino efectivo.
- El acuse debe poder documentarse sin depender solo del resultado textual del intento.
- El resultado final de la notificación debe poder proyectarse luego en snapshot del acta.
- La notificación se mantiene desacoplada del documento, pero siempre lo referencia explícitamente.

---

## Enumeraciones del bloque

### TipoNotif
- `1 = NOTIFICACION_ACTA`
- `2 = NOTIFICACION_ACTO_ADMINISTRATIVO`
- `3 = NOTIFICACION_MEDIDA_PREVENTIVA`
- `4 = OTRA_NOTIFICACION`

### EstadoNotif
- `1 = NO_GENERADA`
- `2 = PENDIENTE_EMISION`
- `3 = EMITIDA`
- `4 = EN_PROCESO`
- `5 = NOTIFICADA`
- `6 = SIN_ACUSE`
- `7 = VENCIDA`
- `8 = FALLIDA`
- `9 = ANULADA`

### CanalNotif
- `1 = DOMICILIO_ELECTRONICO`
- `2 = EMAIL`
- `3 = POSTAL`
- `4 = BLUEMAIL`
- `5 = NOTIFICADOR_MUNICIPAL`
- `6 = PORTAL_CIUDADANO`
- `7 = OTRO`

### TipoDestNotif
- `1 = DOMICILIO_ELECTRONICO`
- `2 = EMAIL`
- `3 = DOMICILIO_POSTAL`
- `4 = REFERENCIA_OPERATIVA`
- `5 = OTRO`

### EstadoIntento
- `1 = GENERADO`
- `2 = ENVIADO`
- `3 = ENTREGADO`
- `4 = RECIBIDO`
- `5 = RECHAZADO`
- `6 = SIN_RESULTADO`
- `7 = FALLIDO`

### TipoAcuse
- `1 = ACUSE_ELECTRONICO`
- `2 = ACUSE_POSTAL`
- `3 = ACUSE_BLUEMAIL`
- `4 = ACUSE_NOTIFICADOR`
- `5 = CONSTANCIA_PORTAL`
- `6 = OTRO`

### EstadoAcuse
- `1 = PENDIENTE`
- `2 = RECIBIDO`
- `3 = RECHAZADO`
- `4 = VENCIDO`
- `5 = INVALIDADO`

### SiRequiereAcuse
- `0 = NO`
- `1 = SI`

### SiAcuseRecibido
- `0 = NO`
- `1 = SI`
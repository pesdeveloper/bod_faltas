# [ANEXO] DDL LÓGICO — CAPA 03 — NOTIFICACIÓN

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 03 — Notificación.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 03 modela el proceso formal de notificación dentro del sistema de faltas.

Debe resolver:

- selección y persistencia de domicilios vinculados al acta
- generación de intentos de notificación
- asociación entre notificación y documentos a notificar
- gestión de reintentos sin sobrescribir historial
- registro de resultados / acuses
- registro de observaciones
- agrupación operativa en lotes
- trazabilidad completa del proceso

La capa sigue los principios ya acordados del sistema:

- modelo event-driven
- persistencia append-only en historial relevante
- no se reabre: se reingresa por evento
- snapshots operativos controlados
- catálogos cerrados mediante enums / constantes
- consistencia con Capa 01 (Acta) y Capa 02 (Documento)

---

# 2) ENTIDADES DE LA CAPA

Las entidades de esta capa son:

- `ActaDomicilio`
- `Notificacion`
- `NotificacionDocumento`
- `NotificacionResultado`
- `NotificacionObservacion`
- `LoteNotificacion`
- `LoteNotificacionDetalle`

---

# 3) CRITERIOS GENERALES DE DISEÑO

## 3.1) Domicilio separado de Acta

El domicilio vinculado al infractor / destinatario no vive embebido dentro de `Acta`.

Se modela mediante `ActaDomicilio`, ya incorporada desde Capa 01.

Esto permite:

- múltiples domicilios por acta
- identificación de domicilio principal
- conservación del domicilio original del acta
- selección de distintos destinos para distintas notificaciones
- reutilización del domicilio en varias notificaciones

---

## 3.2) La notificación es simple

`Notificacion` representa un intento formal de notificación.

No debe transformarse en una mini-BPM ni en un motor paralelo de workflow.

La lógica compleja del proceso vive en:

- el flujo D1–D8
- los eventos de `ActaEvento`
- las reglas operativas de cada bandeja

La capa de notificación solo debe representar:

- a quién se intentó notificar
- por qué canal
- cuándo
- con qué documentos
- cuál fue el resultado
- si fue reintento de otra notificación

---

## 3.3) Los reintentos generan nueva notificación

Un reintento no modifica ni recicla la fila anterior.

Se genera una nueva fila en `Notificacion`.

Si corresponde, la nueva fila referencia la anterior mediante:

- `IdNotificacionOrigen`

Esto preserva trazabilidad completa.

---

## 3.4) Una notificación puede incluir múltiples documentos

La relación entre notificación y documento se resuelve mediante:

- `NotificacionDocumento`

Esto permite:

- notificar un único documento
- notificar varios documentos en un mismo acto
- dejar explícita la composición documental de la notificación

---

## 3.5) El resultado se registra aparte

El resultado / acuse / devolución / constancia no se guarda como simple flag dentro de `Notificacion`.

Se registra en:

- `NotificacionResultado`

Esto permite:

- mantener historial
- soportar múltiples cargas
- registrar anulaciones sin borrar
- desacoplar intento de notificación vs. resultado informado

---

## 3.6) El lote es entidad real

Cuando exista envío batch o preparación grupal, el lote debe modelarse como entidad propia:

- `LoteNotificacion`
- `LoteNotificacionDetalle`

La tabla `Notificacion` no lleva `IdLoteNotificacion`.

La relación se resuelve desde el detalle del lote.

Esto evita acoplamiento rígido y permite:

- una notificación fuera de lote
- trazabilidad de generación / envío / información del lote
- reenvíos o lotes sucesivos sin distorsionar la estructura principal

---

# 4) TABLA: ActaDomicilio

## Finalidad

Representa un domicilio vinculado a un acta.

Puede corresponder al domicilio original consignado al labrar el acta o a otros domicilios posteriormente incorporados para fines operativos, administrativos o de notificación.

Esta tabla ya forma parte del diseño consolidado entre Capa 01 y Capa 03.

---

## PK

- `PK_ActaDomicilio (Id)`

---

## FKs

- `FK_ActaDomicilio_Acta (IdActa -> Acta.Id)`
- `FK_ActaDomicilio_Provincia (IdProvincia -> Provincia.Id)` *(si la referencia existe así en el modelo físico final)*
- `FK_ActaDomicilio_Partido (IdPartido -> Partido.Id)` *(idem)*
- `FK_ActaDomicilio_Localidad (IdLocalidad -> Localidad.Id)` *(idem)*

---

## Campos

### Identificación

- `Id` → `INT8`  
  Identificador único del domicilio del acta.

- `IdActa` → `INT8`  
  Acta a la que pertenece el domicilio.

---

### Clasificación

- `TipoDomicilio` → `SMALLINT`  
  Enum / catálogo cerrado del tipo de domicilio.

  Ejemplos posibles a nivel de aplicación:

  - 1 = ACTA
  - 2 = REAL
  - 3 = CONSTITUIDO
  - 4 = ELECTRONICO
  - 5 = ALTERNATIVO
  - 6 = NOTIFICACION_ESPECIAL

  > Los valores concretos se consolidarán en el catálogo general de enums.

- `EsDomicilioActa` → `SMALLINT`  
  Flag lógico 0/1.  
  Indica que es el domicilio originalmente consignado al momento del labrado.

- `EsPrincipal` → `SMALLINT`  
  Flag lógico 0/1.  
  Indica que es el domicilio principal actual para uso operativo.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.  
  Permite inactivar domicilios sin borrar historial.

---

### Auditoría mínima

- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`  
  Fecha y hora de alta del domicilio.

---

### Componentes del domicilio

- `CalleTexto` → `VARCHAR(48)`  
  Calle o denominación textual.

- `NumeroPuerta` → `INT`  
  Altura / número de puerta.

- `Piso` → `VARCHAR(10)`  
  Piso.

- `Depto` → `VARCHAR(10)`  
  Departamento.

- `Torre` → `VARCHAR(20)`  
  Torre / bloque / sector.

- `IdProvincia` → `SMALLINT`  
  Provincia referenciada.

- `IdPartido` → `INT`  
  Partido / departamento / municipio referenciado.

- `IdLocalidad` → `INT`  
  Localidad referenciada.

- `CodigoPostal` → `CHAR(7)`  
  Código postal.

- `DomicilioTexto` → `VARCHAR(255)`  
  Versión libre o renderizada del domicilio completo.

---

### Datos de contacto asociados

- `Email` → `VARCHAR(150)`  
  Email asociado al domicilio o al receptor para este contexto.

- `Telefono` → `VARCHAR(14)`  
  Teléfono en formato corto controlado según criterio actual acordado.

---

### Observaciones

- `Observaciones` → `VARCHAR(255)`  
  Observaciones operativas del domicilio.

---

## Reglas de negocio relevantes

- Al crearse un acta, debe existir al menos un `ActaDomicilio`.
- Debe existir exactamente un domicilio marcado como principal por acta a nivel lógico-operativo, salvo estados transitorios muy controlados.
- El domicilio principal se proyecta en `Acta.IdActaDomicilioPrincipal`.
- `EsDomicilioActa = 1` identifica el domicilio originalmente consignado al labrar.
- `Activo = 0` no implica borrado ni pérdida de trazabilidad.
- Un domicilio puede ser usado por múltiples notificaciones.
- `ActaDomicilio` no depende de la existencia de una notificación.

---

## Índices sugeridos

- `IX_ActaDomicilio_IdActa`
- `IX_ActaDomicilio_IdActa_Activo`
- `IX_ActaDomicilio_IdActa_EsPrincipal`
- `IX_ActaDomicilio_IdActa_TipoDomicilio`

---

# 5) TABLA: Notificacion

## Finalidad

Representa un intento formal de notificación sobre una acta, por un canal determinado, dirigido a un domicilio / destinatario concreto y eventualmente originado como reintento de otra notificación.

---

## PK

- `PK_Notificacion (Id)`

---

## FKs

- `FK_Notificacion_Acta (IdActa -> Acta.Id)`
- `FK_Notificacion_ActaDomicilio (IdActaDomicilio -> ActaDomicilio.Id)`
- `FK_Notificacion_Origen (IdNotificacionOrigen -> Notificacion.Id)`

---

## Campos

### Identificación

- `Id` → `INT8`  
  Identificador único de la notificación.

- `IdActa` → `INT8`  
  Acta a la que pertenece.

- `IdActaDomicilio` → `INT8`  
  Domicilio utilizado para esta notificación.

- `IdNotificacionOrigen` → `INT8 NULL`  
  Referencia a la notificación previa cuando esta fila representa un reintento o derivación operativa de una anterior.

---

### Tipo / canal / estado

- `CanalNotificacion` → `SMALLINT`  
  Enum / catálogo cerrado.

  Valores preliminares acordados:

  - 1 = POSTAL
  - 2 = ELECTRONICA
  - 3 = NOTIFICADOR_MUNICIPAL

- `EstadoNotificacion` → `SMALLINT`  
  Estado principal de la notificación como intento operativo.

  Ejemplo de catálogo posible:

  - 1 = GENERADA
  - 2 = PREPARADA
  - 3 = ENVIADA
  - 4 = INFORMADA
  - 5 = ANULADA

  > El resultado material concreto vive en `NotificacionResultado`.  
  > Este estado describe la situación operativa de la notificación.

- `MotivoNotificacion` → `SMALLINT`  
  Enum / catálogo cerrado para indicar el motivo u objeto operativo.

  Ejemplos posibles:

  - 1 = NOTIFICACION_ACTA
  - 2 = NOTIFICACION_ACTO_RESOLUTIVO
  - 3 = NOTIFICACION_REINTENTO
  - 4 = NOTIFICACION_COMPLEMENTARIA

---

### Fechas operativas

- `FechaHoraGeneracion` → `DATETIME YEAR TO SECOND`  
  Momento en que la notificación fue creada en el sistema.

- `FechaHoraEnvio` → `DATETIME YEAR TO SECOND NULL`  
  Momento en que efectivamente fue despachada / enviada / entregada al canal.

- `FechaHoraAnulacion` → `DATETIME YEAR TO SECOND NULL`  
  Momento de anulación, si corresponde.

---

### Receptor / vínculo

- `VinculoReceptor` → `SMALLINT`  
  Enum / catálogo cerrado que describe el vínculo del receptor con la notificación.

  Ejemplos posibles:

  - 1 = TITULAR
  - 2 = INFRACTOR
  - 3 = RESPONSABLE
  - 4 = APODERADO
  - 5 = TERCERO_IDENTIFICADO
  - 6 = DESTINATARIO_ELECTRONICO

- `NombreReceptor` → `VARCHAR(100)`  
  Nombre visible del receptor informado o destinatario.

- `TipoDocumentoReceptor` → `SMALLINT NULL`  
  Tipo de documento del receptor.  
  Enum / catálogo cerrado.

- `NumeroDocumentoReceptor` → `INT8 NULL`  
  Número de documento del receptor.  
  Debe ser numérico, no texto libre.

---

### Observación breve y control

- `Observaciones` → `VARCHAR(255)`  
  Observación operativa breve de la notificación.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.

---

## Reglas de negocio relevantes

- Cada `Notificacion` pertenece a una única `Acta`.
- Cada `Notificacion` utiliza un único `ActaDomicilio`.
- Un reintento genera una nueva fila y opcionalmente referencia `IdNotificacionOrigen`.
- Una notificación puede existir aunque todavía no se haya informado resultado.
- El resultado material no debe inferirse solo desde `EstadoNotificacion`.
- `ANULADA` es un estado válido del intento operativo.
- La asociación a lote, si existe, no vive en esta tabla.

---

## Índices sugeridos

- `IX_Notificacion_IdActa`
- `IX_Notificacion_IdActaDomicilio`
- `IX_Notificacion_IdNotificacionOrigen`
- `IX_Notificacion_CanalNotificacion_EstadoNotificacion`
- `IX_Notificacion_FechaHoraGeneracion`
- `IX_Notificacion_FechaHoraEnvio`

---

# 6) TABLA: NotificacionDocumento

## Finalidad

Resuelve la relación N:M lógica entre una notificación y los documentos incluidos en ese acto de notificación.

En la práctica actual, una notificación puede incluir uno o varios documentos.

---

## PK

- `PK_NotificacionDocumento (Id)`

---

## FKs

- `FK_NotificacionDocumento_Notificacion (IdNotificacion -> Notificacion.Id)`
- `FK_NotificacionDocumento_Documento (IdDocumento -> Documento.Id)`

---

## Uniques sugeridas

- `UQ_NotificacionDocumento (IdNotificacion, IdDocumento)`

---

## Campos

- `Id` → `INT8`  
  Identificador único de la relación.

- `IdNotificacion` → `INT8`  
  Notificación.

- `IdDocumento` → `INT8`  
  Documento incluido en la notificación.

- `RolDocumentoNotificacion` → `SMALLINT`  
  Rol del documento dentro del acto notificatorio.

  Ejemplos posibles:

  - 1 = PRINCIPAL
  - 2 = ADJUNTO
  - 3 = ANEXO
  - 4 = CONSTANCIA

- `Orden` → `SMALLINT`  
  Orden lógico de presentación dentro de la notificación.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.

---

## Reglas de negocio relevantes

- Una notificación puede tener uno o varios documentos.
- Un mismo documento podría, si el negocio algún día lo admite, reutilizarse en más de una notificación; por eso la relación es independiente.
- Debe evitarse duplicar el mismo documento dentro de una misma notificación.
- El rol del documento no reemplaza `RolDocumento` propio de Capa 02; lo complementa para el contexto notificatorio.

---

## Índices sugeridos

- `IX_NotificacionDocumento_IdNotificacion`
- `IX_NotificacionDocumento_IdDocumento`
- `IX_NotificacionDocumento_IdNotificacion_Orden`

# 7) TABLA: NotificacionResultado

## Finalidad

Registra el resultado informado de una notificación.

Permite representar:

- acuse positivo
- acuse negativo
- imposibilidad de entrega
- rechazo
- devolución
- carga errónea posteriormente anulada
- múltiples informes sobre una misma notificación sin perder historial

---

## PK

- `PK_NotificacionResultado (Id)`

---

## FKs

- `FK_NotificacionResultado_Notificacion (IdNotificacion -> Notificacion.Id)`

---

## Campos

### Identificación

- `Id` → `INT8`  
  Identificador único del resultado.

- `IdNotificacion` → `INT8`  
  Notificación a la que corresponde.

---

### Resultado

- `TipoResultadoNotificacion` → `SMALLINT`  
  Enum / catálogo cerrado del resultado informado.

  Ejemplos posibles:

  - 1 = ENTREGADA
  - 2 = RECIBIDA
  - 3 = RECHAZADA
  - 4 = DEVUELTA
  - 5 = DOMICILIO_INEXISTENTE
  - 6 = DESTINATARIO_AUSENTE
  - 7 = SIN_ACUSE
  - 8 = ERROR_INFORMADO
  - 9 = IMPOSIBLE_DILIGENCIAR

- `FechaHoraResultado` → `DATETIME YEAR TO SECOND`  
  Fecha y hora del resultado material informado o tomado como válido operativamente.

- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`  
  Fecha y hora en que el resultado fue registrado en el sistema.

---

### Constancias / datos complementarios

- `DescripcionResultado` → `VARCHAR(255)`  
  Texto operativo breve asociado al resultado.

- `NombreReceptorInformado` → `VARCHAR(100)`  
  Nombre del receptor informado en el acuse, si aplica.

- `TipoDocumentoReceptor` → `SMALLINT NULL`  
  Tipo documental del receptor informado, si aplica.

- `NumeroDocumentoReceptor` → `INT8 NULL`  
  Documento numérico del receptor informado, si aplica.

- `FechaHoraRecepcionInformada` → `DATETIME YEAR TO SECOND NULL`  
  Fecha/hora consignada en el acuse o constancia, si fuese distinta de la fecha de carga.

---

### Control de anulación

- `Anulado` → `SMALLINT`  
  Flag lógico 0/1.  
  Indica que este resultado fue dejado sin efecto.

- `FechaHoraAnulacion` → `DATETIME YEAR TO SECOND NULL`  
  Fecha y hora de anulación.

- `MotivoAnulacion` → `VARCHAR(255) NULL`  
  Motivo breve de anulación.

---

### Auditoría mínima

- `UsuarioRegistro` → `VARCHAR(50) NULL`  
  Usuario que cargó el resultado, si en esta capa se desea dejar referencia textual o técnica.

---

## Reglas de negocio relevantes

- `NotificacionResultado` no reemplaza a `Notificacion`; la complementa.
- Puede existir más de un resultado por notificación, pero a nivel operativo normalmente habrá uno vigente.
- Si un resultado fue cargado incorrectamente, no se elimina: se marca `Anulado = 1`.
- La vigencia operativa deberá resolverse por reglas de aplicación (por ejemplo: último no anulado).
- `NumeroDocumentoReceptor` es numérico, no texto libre.
- El sistema debe evitar perder historial de devoluciones, acuses o rectificaciones.

---

## Índices sugeridos

- `IX_NotificacionResultado_IdNotificacion`
- `IX_NotificacionResultado_IdNotificacion_Anulado`
- `IX_NotificacionResultado_TipoResultadoNotificacion`
- `IX_NotificacionResultado_FechaHoraResultado`

---

# 8) TABLA: NotificacionObservacion

## Finalidad

Registra observaciones operativas o administrativas vinculadas a una notificación.

No reemplaza al resultado ni al historial de eventos del acta.

Sirve para notas complementarias de gestión.

---

## PK

- `PK_NotificacionObservacion (Id)`

---

## FKs

- `FK_NotificacionObservacion_Notificacion (IdNotificacion -> Notificacion.Id)`

---

## Campos

- `Id` → `INT8`  
  Identificador único.

- `IdNotificacion` → `INT8`  
  Notificación observada.

- `TipoObservacionNotificacion` → `SMALLINT`  
  Tipo de observación.

  Ejemplos posibles:

  - 1 = OPERATIVA
  - 2 = ADMINISTRATIVA
  - 3 = ERROR_CARGA
  - 4 = ACLARACION
  - 5 = INCIDENCIA_CANAL

- `Observacion` → `VARCHAR(500)`  
  Texto de la observación.

- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`  
  Momento de carga.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.

---

## Reglas de negocio relevantes

- Las observaciones no cambian por sí mismas el estado principal de la notificación.
- No deben utilizarse como sustituto de campos estructurados cuando el dato requiere modelado formal.
- Deben conservar historial.

---

## Índices sugeridos

- `IX_NotificacionObservacion_IdNotificacion`
- `IX_NotificacionObservacion_IdNotificacion_Activo`
- `IX_NotificacionObservacion_FechaHoraRegistro`

---

# 9) TABLA: LoteNotificacion

## Finalidad

Representa un lote operativo de notificaciones para preparación, envío o procesamiento conjunto.

Aplica especialmente a canales batch como postal o procesos de despacho agrupado.

---

## PK

- `PK_LoteNotificacion (Id)`

---

## Campos

### Identificación

- `Id` → `INT8`  
  Identificador único del lote.

- `TipoLoteNotificacion` → `SMALLINT`  
  Tipo de lote.

  Ejemplos posibles:

  - 1 = POSTAL
  - 2 = ELECTRONICO_BATCH
  - 3 = NOTIFICADOR_MUNICIPAL
  - 4 = MIXTO_CONTROLADO

- `EstadoLoteNotificacion` → `SMALLINT`  
  Estado del lote.

  Estados preliminares acordados:

  - 1 = GENERADO
  - 2 = CERRADO
  - 3 = ENVIADO
  - 4 = INFORMADO
  - 5 = ANULADO

---

### Fechas operativas

- `FechaHoraGeneracion` → `DATETIME YEAR TO SECOND`  
  Alta del lote.

- `FechaHoraCierre` → `DATETIME YEAR TO SECOND NULL`  
  Momento en que el lote quedó cerrado para nuevas incorporaciones.

- `FechaHoraEnvio` → `DATETIME YEAR TO SECOND NULL`  
  Momento de despacho o envío del lote.

- `FechaHoraInformado` → `DATETIME YEAR TO SECOND NULL`  
  Momento en que el lote fue informado / conciliado.

- `FechaHoraAnulacion` → `DATETIME YEAR TO SECOND NULL`  
  Momento de anulación.

---

### Datos de control

- `Descripcion` → `VARCHAR(255)`  
  Descripción operativa del lote.

- `Observaciones` → `VARCHAR(500)`  
  Observaciones administrativas / operativas.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.

---

## Reglas de negocio relevantes

- El lote es entidad real y autónoma.
- Una notificación puede no pertenecer a ningún lote.
- El estado del lote no reemplaza el estado individual de cada notificación.
- La pertenencia de notificaciones al lote se resuelve por `LoteNotificacionDetalle`.
- El lote puede anularse sin borrar historial.

---

## Índices sugeridos

- `IX_LoteNotificacion_TipoLoteNotificacion_EstadoLoteNotificacion`
- `IX_LoteNotificacion_FechaHoraGeneracion`
- `IX_LoteNotificacion_FechaHoraEnvio`

---

# 10) TABLA: LoteNotificacionDetalle

## Finalidad

Relaciona un lote con las notificaciones incluidas en él.

---

## PK

- `PK_LoteNotificacionDetalle (Id)`

---

## FKs

- `FK_LoteNotificacionDetalle_Lote (IdLoteNotificacion -> LoteNotificacion.Id)`
- `FK_LoteNotificacionDetalle_Notificacion (IdNotificacion -> Notificacion.Id)`

---

## Uniques sugeridas

- `UQ_LoteNotificacionDetalle (IdLoteNotificacion, IdNotificacion)`

---

## Campos

- `Id` → `INT8`  
  Identificador único del detalle.

- `IdLoteNotificacion` → `INT8`  
  Lote.

- `IdNotificacion` → `INT8`  
  Notificación incluida.

- `Orden` → `INT`  
  Orden interno dentro del lote.

- `Activo` → `SMALLINT`  
  Flag lógico 0/1.

- `Observaciones` → `VARCHAR(255)`  
  Observación breve del detalle.

---

## Reglas de negocio relevantes

- La relación lote ↔ notificación vive aquí, no en `Notificacion`.
- Una notificación podrá pertenecer o no a lote según el canal y la operatoria.
- En principio, una misma notificación no debería duplicarse dentro del mismo lote.
- Si a futuro se necesitara permitir reagrupación en lotes sucesivos, deberá resolverse por reglas de negocio, no por borrado físico.

---

## Índices sugeridos

- `IX_LoteNotificacionDetalle_IdLoteNotificacion`
- `IX_LoteNotificacionDetalle_IdNotificacion`
- `IX_LoteNotificacionDetalle_IdLoteNotificacion_Orden`

---

# 11) RELACIONES PRINCIPALES DE LA CAPA

## Acta ↔ ActaDomicilio

- `Acta 1 ----- N ActaDomicilio`

Un acta puede tener múltiples domicilios.

---

## Acta ↔ Notificacion

- `Acta 1 ----- N Notificacion`

Un acta puede tener múltiples intentos de notificación.

---

## ActaDomicilio ↔ Notificacion

- `ActaDomicilio 1 ----- N Notificacion`

Un domicilio puede ser utilizado en varias notificaciones.

---

## Notificacion ↔ Notificacion (origen / reintento)

- `Notificacion 1 ----- N Notificacion`

Autorelación por `IdNotificacionOrigen`.

---

## Notificacion ↔ Documento

- `Notificacion 1 ----- N NotificacionDocumento`
- `Documento 1 ----- N NotificacionDocumento`

Esto resuelve que una notificación pueda incluir múltiples documentos.

---

## Notificacion ↔ NotificacionResultado

- `Notificacion 1 ----- N NotificacionResultado`

Permite historial y anulación lógica de resultados erróneos.

---

## Notificacion ↔ NotificacionObservacion

- `Notificacion 1 ----- N NotificacionObservacion`

Permite notas operativas complementarias.

---

## LoteNotificacion ↔ Notificacion

- `LoteNotificacion 1 ----- N LoteNotificacionDetalle`
- `Notificacion 1 ----- N LoteNotificacionDetalle`

La relación se modela exclusivamente por detalle.

---

# 12) CONSISTENCIA CON OTRAS CAPAS

## Con Capa 01

- `ActaDomicilio` ya debe existir también en Capa 01.
- `Acta.IdActaDomicilioPrincipal` proyecta el domicilio principal actual.
- Los cambios relevantes del proceso deben seguir registrándose en `ActaEvento`.
- La capa de notificación no reemplaza la trazabilidad event-driven del sistema.

---

## Con Capa 02

- `Documento` sigue siendo entidad propia.
- `NotificacionDocumento` resuelve explícitamente que una notificación puede incluir múltiples documentos.
- No se modifica la definición central de `Documento`, `ActaDocumento`, `DocumentoFirma` ni `DocumentoObservacion`.

---

# 13) OBSERVACIONES DE IMPLEMENTACIÓN

## 13.1) Sobre enums / catálogos

Todos los siguientes campos deben resolverse mediante enums / constantes cerradas del sistema o tablas catálogo controladas según la estrategia general:

- `TipoDomicilio`
- `CanalNotificacion`
- `EstadoNotificacion`
- `MotivoNotificacion`
- `VinculoReceptor`
- `TipoDocumentoReceptor`
- `TipoResultadoNotificacion`
- `TipoObservacionNotificacion`
- `TipoLoteNotificacion`
- `EstadoLoteNotificacion`
- `RolDocumentoNotificacion`

No deben persistirse estados semánticos importantes como texto libre.

---

## 13.2) Sobre append-only e historial

Aunque no toda la capa sea puramente append-only en sentido estricto, el criterio general debe ser:

- no sobrescribir historial relevante
- no borrar resultados erróneos: anular
- no reciclar notificaciones para reintentos
- no perder trazabilidad de lotes, documentos y resultados

---

## 13.3) Sobre el modelo físico futuro

En la implementación física podrán agregarse:

- constraints de unicidad adicionales
- checks lógicos
- defaults
- índices compuestos
- auditoría expandida
- columnas técnicas de integración

sin alterar la semántica aquí definida.

---

# 14) RESUMEN DE TABLAS DE CAPA 03

- `ActaDomicilio`
- `Notificacion`
- `NotificacionDocumento`
- `NotificacionResultado`
- `NotificacionObservacion`
- `LoteNotificacion`
- `LoteNotificacionDetalle`

---

# 15) CIERRE

La Capa 03 queda definida como la capa de notificación formal del sistema, manteniendo:

- simplicidad estructural
- trazabilidad completa
- soporte de reintentos
- soporte documental múltiple
- soporte de resultados y anulaciones
- consistencia con el modelo event-driven general
- reutilización del domicilio como entidad propia

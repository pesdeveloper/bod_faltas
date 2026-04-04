# [CAPA 02] DOCUMENTAL

## Finalidad de la capa

Esta capa define el subsistema documental formal del sistema de faltas.

Debe resolver:

- existencia del **DOCUMENTO** como entidad propia
- vínculo entre **ACTA** y **DOCUMENTO**
- soporte para múltiples documentos por acta
- clasificación documental
- generación documental
- numeración documental
- firma
- anulación documental
- observaciones documentales
- almacenamiento físico / lógico del archivo
- control de hash e integridad
- proyección del estado documental formal sobre `Acta`

---

## Alcance de la capa

Esta capa modela los **documentos formales del proceso**.

Ejemplos:

- acta
- notificación
- fallo
- resolución
- constancia
- actuación
- presentación
- nota
- documento de apelación
- documento externo incorporado al expediente documental

---

## Qué NO modela esta capa

Esta capa **no modela por sí sola** las evidencias del acta.

Las evidencias:

- nacen en D1
- pueden agregarse o invalidarse en D2
- forman parte del contenido probatorio del trámite
- deberán resolverse como entidad específica en un ajuste posterior del núcleo operativo

Pendientes a revisar luego de esta capa:

- `ActaEvidencia`
- `ActaObservacion`

---

## Principios de diseño

### 1. El documento es entidad propia

No se modela como atributo embebido de `Acta`.

---

### 2. Un acta puede tener múltiples documentos

Cada documento cumple un rol procesal, administrativo o documental distinto dentro del trámite.

---

### 3. El estado documental real vive en esta capa

`Acta` solo mantiene el snapshot proyectado:

- `EstadoDocumentalActual`
- `CantidadDocsPendFirma`
- `CantidadDocsFirmados`

La verdad documental real vive en las tablas de esta capa.

---

### 4. La firma forma parte del ciclo documental

La firma no se resuelve con un único flag aislado.

Debe poder trazarse:

- si requiere firma o no
- si fue enviada a firmar
- si fue firmada
- si hubo error
- qué firmante intervino
- con qué mecanismo / proveedor se firmó
- qué operación técnica se ejecutó

---

### 5. El documento no tiene “firma parcial” como estado final

A nivel documento, el estado es determinístico:

- firmado
- no firmado

La granularidad adicional, si hiciera falta, vive en el detalle de firma y no en el estado principal del documento.

---

### 6. “Observado” no es estado principal del documento

Las observaciones documentales existen, pero se registran en una tabla aparte.

No se modelan como estado principal del documento.

---

### 7. “Anulado” sí forma parte del ciclo documental

Un documento puede quedar anulado, independientemente de si fue o no firmado.

---

### 8. La relación Acta ↔ Documento se resuelve por tabla intermedia

Se utiliza `ActaDocumento` y no un FK directo en `Documento`, para permitir:

- rol documental
- orden lógico
- flexibilidad futura

Por ahora, la regla operativa es:

- cada documento se asocia a **una sola acta**

---

### 9. Los tipos y roles documentales deben ser cerrados y controlados

No deben depender de catálogos abiertos de negocio.

Se persisten como:

- `SMALLINT`
- enum / constantes claras en aplicación

---

## Entidades de esta capa

### Núcleo documental
- `Documento`

### Relacionales
- `ActaDocumento`

### Firma
- `DocumentoFirma`

### Observaciones
- `DocumentoObservacion`

---

# 1) TABLA: Documento

## Propósito

Representa la unidad documental formal del proceso.

Ejemplos posibles:

- ACTA
- ACTA_COMPLEMENTARIA
- FALLO
- RESOLUCION
- RESOLUCION_CONCESION_APELACION
- RESOLUCION_IMPROCEDENCIA_APELACION
- NOTIFICACION
- NOTIFICACION_RESULTADO_JUDICIAL
- DDJJ_DOMICILIO_PRESENCIAL
- ACTUACION
- NOTA_ABOGADO
- NOTA_INFRACTOR
- PRESENTACION
- CONSTANCIA
- OTRO

---

## Estructura lógica

### Identidad
- `Id : INT8 NOT NULL`
- `IdTecnico : CHAR(36) NOT NULL`

### Clasificación
- `TipoDocumento : SMALLINT NOT NULL`
- `EstadoDocumento : SMALLINT NOT NULL`
- `FirmaTipo : SMALLINT NOT NULL`

### Numeración
- `NumeroDocumento : VARCHAR(20) NULL`
- `FuePreNumerado : SMALLINT NOT NULL`

### Archivo / storage / integridad
- `StorageKey : VARCHAR(256) NULL`
- `UbicacionStorage : VARCHAR(256) NULL`
- `NombreOriginal : VARCHAR(128) NULL`
- `MimeTypeCodigo : SMALLINT NOT NULL`
- `Extension : VARCHAR(8) NULL`
- `HashContenido : CHAR(64) NULL`
- `TamanoBytes : INT8 NULL`

### Fechas
- `FechaHoraCreacionRegistro : DATETIME YEAR TO SECOND NOT NULL`
- `FechaHoraGeneracionContenido : DATETIME YEAR TO SECOND NULL`
- `FechaHoraFirma : DATETIME YEAR TO SECOND NULL`
- `FechaHoraAnulacion : DATETIME YEAR TO SECOND NULL`

### Snapshot documental
- `EstaFirmado : SMALLINT NOT NULL`
- `EstaAnulado : SMALLINT NOT NULL`
- `TieneObservaciones : SMALLINT NOT NULL`
- `EsDocumentoExterno : SMALLINT NOT NULL`

### Auditoría técnica
- `UsuarioCreacion : CHAR(36) NOT NULL`
- `UsuarioUltimaActualizacion : CHAR(36) NULL`

---

## Observaciones de diseño

### a) `NumeroDocumento`
Es `VARCHAR` porque su conformación depende del talonario.

La política de numeración deberá definirse en la configuración de talonarios, incluyendo:

- prefijo opcional
- año / AAAA
- número correlativo
- separadores
- longitud máxima

---

### b) `StorageKey`
Representa la clave técnica exacta del archivo en el storage.

Ejemplos posibles:

- `8451_DF-2026-000123_firmado.pdf`
- `4b6c3f2d-8c7a-4b7e-9b20-1bb8a1234567.pdf`
- `2026/04/8451/documento_firmado.pdf`

---

### c) `UbicacionStorage`
Representa la ubicación lógica o contenedor base del archivo.

Ejemplos posibles:

- `/faltas/documentos/2026/04/`
- `s3://faltas-prod/documentos/2026/04/`
- `azure://faltas/documentos/resoluciones/`

---

### d) `HashContenido`
Se prevé almacenar hash SHA-256 hexadecimal del contenido del archivo.

Por eso `CHAR(64)` es suficiente.

---

### e) `MimeTypeCodigo`
No se guarda como texto libre.

Se modela como enum / constantes de aplicación.

Ejemplos:
- PDF
- JPG
- JPEG
- PNG
- TIFF
- WEBP
- OTRO

---

### f) `EsDocumentoExterno`
Distingue documentos generados por el sistema de documentos externos incorporados.

---

# 2) TABLA: ActaDocumento

## Propósito

Relaciona el documento con el acta y define su rol documental dentro del trámite.

Permite saber:

- a qué acta pertenece
- qué rol cumple el documento
- cuál es su orden lógico dentro de la secuencia documental

---

## Estructura lógica

- `Id : INT8 NOT NULL`
- `IdActa : INT8 NOT NULL`
- `IdDocumento : INT8 NOT NULL`
- `RolDocumento : SMALLINT NOT NULL`
- `OrdenLogico : SMALLINT NOT NULL`
- `FechaHoraAsociacion : DATETIME YEAR TO SECOND NOT NULL`
- `UsuarioAsociacion : CHAR(36) NULL`

---

## Regla operativa actual

Por ahora, se asume que cada documento se asocia a **una sola acta**.

Aunque la tabla intermedia permitiría evolución futura, no se modela en esta etapa el caso de un documento compartido por múltiples actas.

---

## Observaciones de diseño

### `RolDocumento`
Debe resolverse por enum / constantes claras del dominio.

No debe depender de un catálogo abierto de negocio.

Valores sugeridos iniciales:

- `NO_APLICA`
- `DOCUMENTO_BASE_ACTA`
- `NOTIFICACION_ACTA`
- `ACTUACION_INTERNA`
- `PRESENTACION_INFRACTOR`
- `PRESENTACION_ABOGADO`
- `CONSTANCIA`
- `RESOLUCION`
- `FALLO`
- `NOTIFICACION_ACTO`
- `RESOLUCION_CONCESION_APELACION`
- `RESOLUCION_IMPROCEDENCIA_APELACION`
- `NOTIFICACION_RESULTADO_JUDICIAL`
- `DDJJ_DOMICILIO_PRESENCIAL`
- `ACTA_COMPLEMENTARIA`
- `NOTA_ARCHIVO`
- `DOCUMENTO_APREMIO`
- `DOCUMENTO_EXTERNO_INCORPORADO`
- `ANEXO_DOCUMENTAL`
- `OTRO`

---

# 3) TABLA: DocumentoFirma

## Propósito

Registrar el estado y los datos de firma del documento.

Existe una sola fila por documento.

Permite registrar:

- si requiere firma o no
- si la firma está pendiente
- si fue firmada
- si hubo error
- qué firmante intervino
- qué versión del firmante se utilizó
- con qué mecanismo / proveedor se firmó
- qué operación técnica de firma se ejecutó

---

## Estructura lógica

- `Id : INT8 NOT NULL`
- `IdDocumento : INT8 NOT NULL`
- `EstadoFirma : SMALLINT NOT NULL`
- `IdFirmante : CHAR(36) NULL`
- `VersionFirmante : INT NULL`
- `FechaHoraSolicitudFirma : DATETIME YEAR TO SECOND NULL`
- `FechaHoraFirma : DATETIME YEAR TO SECOND NULL`
- `ProveedorFirma : SMALLINT NULL`
- `ReferenciaOperacionFirma : VARCHAR(128) NULL`
- `HashFirmado : CHAR(64) NULL`

---

## Observaciones de diseño

### a) `VersionFirmante`
Se utiliza para preservar inmutabilidad de los datos del firmante, siguiendo la misma lógica de snapshots versionados usada para inspectores.

---

### b) `ProveedorFirma`
Debe resolverse como enum / constantes de aplicación.

Ejemplos posibles:

- `NO_APLICA`
- `FIRMADOR_LOCAL`
- `HSM_CENTRAL`
- `FIRMA_REMOTA`
- `RESUBIDA_MANUAL`
- `SELLO_TECNICO`

---

### c) `ReferenciaOperacionFirma`
Es el identificador técnico de la operación de firma en el sistema que ejecuta o coordina la firma.

Ejemplo:

- `OPF-2026-04-000452`

Sirve para:

- correlacionar logs del motor de firma
- auditar la operación concreta
- investigar errores
- rastrear una firma determinada en sistemas externos o internos

---

### d) `HashFirmado`
Permite conservar hash del archivo ya firmado, si se decide persistirlo.

---

# 4) TABLA: DocumentoObservacion

## Propósito

Registrar observaciones internas del documento con trazabilidad.

No se usa un único campo libre dentro de `Documento`.

Cada observación queda registrada con:

- fecha/hora
- usuario
- tipo
- texto

---

## Estructura lógica

- `Id : INT8 NOT NULL`
- `IdDocumento : INT8 NOT NULL`
- `FechaHoraObservacion : DATETIME YEAR TO SECOND NOT NULL`
- `IdUsuario : CHAR(36) NOT NULL`
- `TipoObservacion : SMALLINT NOT NULL`
- `Observacion : VARCHAR(255) NOT NULL`

---

## Observaciones de diseño

### `TipoObservacion`
Debe resolverse como enum / constantes claras.

Ejemplos iniciales posibles:

- `TECNICA`
- `ADMINISTRATIVA`
- `JURIDICA`
- `FIRMA`
- `NUMERACION`
- `STORAGE`
- `OTRA`

---

# Estados / enums importantes de la capa

## EstadoDocumento

Se persiste como `SMALLINT`.

Valores base:

- `0 = NO_APLICA`
- `1 = BORRADOR`
- `2 = GENERADO`
- `3 = PENDIENTE_FIRMA`
- `4 = FIRMADO`
- `5 = ANULADO`

### Nota
`BORRADOR` solo tiene sentido cuando el documento existe pero aún está en edición o incompleto.

No se modela `PENDIENTE_GENERACION` como estado del documento, porque eso pertenece más al workflow o a la regla de negocio que a la existencia documental concreta.

---

## FirmaTipo

Se persiste como `SMALLINT`.

Valores base:

- `0 = NINGUNA`
- `1 = DIGITAL`
- `2 = OLOGRAFA_RESUBIDA`
- `3 = SELLO_TECNICO_INMUTABILIDAD`

---

## EstadoFirma

Se persiste como `SMALLINT`.

Valores base:

- `0 = NO_REQUIERE`
- `1 = PENDIENTE`
- `2 = FIRMADA`
- `3 = ERROR`
- `4 = ANULADA`

---

## MimeTypeCodigo

Se persiste como `SMALLINT`.

Valores iniciales sugeridos:

- `0 = NO_DEFINIDO`
- `1 = PDF`
- `2 = JPG`
- `3 = JPEG`
- `4 = PNG`
- `5 = TIFF`
- `6 = WEBP`
- `7 = OTRO`

---

## RolDocumento

Se persiste como `SMALLINT`.

Debe resolverse por enum / constantes de aplicación y no por catálogo abierto de negocio.

---

# Relación con la Capa 01

Esta capa proyecta sobre `Acta` al menos los siguientes snapshots:

- `EstadoDocumentalActual`
- `CantidadDocsPendFirma`
- `CantidadDocsFirmados`
- `IdDocumentoUltimo`

---

# Lo que esta capa todavía NO resuelve por completo

Esta capa aún no define en detalle:

- motor de numeración / talonarios
- modelo completo del firmante y snapshots del firmante
- relación con evidencias
- observaciones del acta
- anexos probatorios fuera del documento formal

Estos temas se resolverán en capas o ajustes posteriores.

---

# Resumen de la capa

Esta capa define el subsistema documental formal del sistema:

- `Documento` como entidad propia
- `ActaDocumento` como relación documental con el trámite
- `DocumentoFirma` como trazabilidad de firma
- `DocumentoObservacion` como log interno de observaciones
- estados documentales reales
- almacenamiento, hash e integridad
- base para proyectar el estado documental sobre `Acta`
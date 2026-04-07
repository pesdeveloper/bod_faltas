# [ANEXO] DDL LÓGICO — CAPA 02 — DOCUMENTAL

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 02.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) TABLA: Documento

## PK
- `PK_Documento (Id)`

## Uniques
- `UQ_Documento_IdTecnico`
- `UQ_Documento_NumeroDocumento` *(si la política del talonario garantiza unicidad global)*

## Campos

| Campo | Tipo | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdTecnico` | `CHAR(36)` | No | GUID técnico |
| `TipoDocumento` | `SMALLINT` | No | Enum |
| `EstadoDocumento` | `SMALLINT` | No | Enum |
| `FirmaTipo` | `SMALLINT` | No | Enum |
| `NumeroDocumento` | `VARCHAR(20)` | Sí | Numeración por talonario |
| `FuePreNumerado` | `SMALLINT` | No | 0/1 |
| `StorageKey` | `VARCHAR(256)` | Sí | Clave exacta del archivo |
| `UbicacionStorage` | `VARCHAR(256)` | Sí | Ruta / contenedor lógico |
| `NombreOriginal` | `VARCHAR(128)` | Sí | Nombre del archivo |
| `MimeTypeCodigo` | `SMALLINT` | No | Enum |
| `Extension` | `VARCHAR(8)` | Sí | Extensión |
| `HashContenido` | `CHAR(64)` | Sí | SHA-256 hex |
| `TamanoBytes` | `INT8` | Sí | Tamaño del archivo |
| `FechaHoraCreacionRegistro` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `FechaHoraGeneracionContenido` | `DATETIME YEAR TO SECOND` | Sí | Generación del archivo |
| `FechaHoraFirma` | `DATETIME YEAR TO SECOND` | Sí | Firma final |
| `FechaHoraAnulacion` | `DATETIME YEAR TO SECOND` | Sí | Anulación |
| `EstaFirmado` | `SMALLINT` | No | 0/1 |
| `EstaAnulado` | `SMALLINT` | No | 0/1 |
| `TieneObservaciones` | `SMALLINT` | No | 0/1 |
| `EsDocumentoExterno` | `SMALLINT` | No | 0/1 |
| `UsuarioCreacion` | `CHAR(36)` | No | Subject |
| `UsuarioUltimaActualizacion` | `CHAR(36)` | Sí | Subject |

---

# 2) TABLA: ActaDocumento

## PK
- `PK_ActaDocumento (Id)`

## FK
- `FK_ActaDocumento_Acta (IdActa -> Acta.Id)`
- `FK_ActaDocumento_Documento (IdDocumento -> Documento.Id)`

## Regla operativa
- Cada documento queda asociado a una sola acta en esta etapa.

## Campos

| Campo | Tipo | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActa` | `INT8` | No | FK |
| `IdDocumento` | `INT8` | No | FK |
| `RolDocumento` | `SMALLINT` | No | Enum |
| `OrdenLogico` | `SMALLINT` | No | Orden dentro de la secuencia |
| `FechaHoraAsociacion` | `DATETIME YEAR TO SECOND` | No | Fecha de asociación |
| `UsuarioAsociacion` | `CHAR(36)` | Sí | Subject |

---

# 3) TABLA: DocumentoFirma

## PK
- `PK_DocumentoFirma (Id)`

## FK
- `FK_DocumentoFirma_Documento (IdDocumento -> Documento.Id)`

## Restricción lógica
- Una sola fila por documento.

## Campos

| Campo | Tipo | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdDocumento` | `INT8` | No | FK |
| `EstadoFirma` | `SMALLINT` | No | Enum |
| `IdFirmante` | `CHAR(36)` | Sí | Firmante |
| `VersionFirmante` | `INT` | Sí | Snapshot versionado |
| `FechaHoraSolicitudFirma` | `DATETIME YEAR TO SECOND` | Sí | Solicitud |
| `FechaHoraFirma` | `DATETIME YEAR TO SECOND` | Sí | Firma efectiva |
| `ProveedorFirma` | `SMALLINT` | Sí | Enum |
| `ReferenciaOperacionFirma` | `VARCHAR(128)` | Sí | Correlación técnica |
| `HashFirmado` | `CHAR(64)` | Sí | Hash del archivo firmado |

---

# 4) TABLA: DocumentoObservacion

## PK
- `PK_DocumentoObservacion (Id)`

## FK
- `FK_DocumentoObservacion_Documento (IdDocumento -> Documento.Id)`

## Campos

| Campo | Tipo | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdDocumento` | `INT8` | No | FK |
| `FechaHoraObservacion` | `DATETIME YEAR TO SECOND` | No | Momento del registro |
| `IdUsuario` | `CHAR(36)` | No | Subject |
| `TipoObservacion` | `SMALLINT` | No | Enum |
| `Observacion` | `VARCHAR(255)` | No | Texto |

---

# 5) ENUMS / CONSTANTES IMPORTANTES

## EstadoDocumento
- `0 = NO_APLICA`
- `1 = BORRADOR`
- `2 = GENERADO`
- `3 = PENDIENTE_FIRMA`
- `4 = FIRMADO`
- `5 = ANULADO`

## FirmaTipo
- `0 = NINGUNA`
- `1 = DIGITAL`
- `2 = OLOGRAFA_RESUBIDA`
- `3 = SELLO_TECNICO_INMUTABILIDAD`

## EstadoFirma
- `0 = NO_REQUIERE`
- `1 = PENDIENTE`
- `2 = FIRMADA`
- `3 = ERROR`
- `4 = ANULADA`

## MimeTypeCodigo
- `0 = NO_DEFINIDO`
- `1 = PDF`
- `2 = JPG`
- `3 = JPEG`
- `4 = PNG`
- `5 = TIFF`
- `6 = WEBP`
- `7 = OTRO`

## RolDocumento
Valores iniciales sugeridos:
- `0 = NO_APLICA`
- `1 = DOCUMENTO_BASE_ACTA`
- `2 = NOTIFICACION_ACTA`
- `3 = ACTUACION_INTERNA`
- `4 = PRESENTACION_INFRACTOR`
- `5 = PRESENTACION_ABOGADO`
- `6 = CONSTANCIA`
- `7 = RESOLUCION`
- `8 = FALLO`
- `9 = NOTIFICACION_ACTO`
- `10 = RESOLUCION_CONCESION_APELACION`
- `11 = RESOLUCION_IMPROCEDENCIA_APELACION`
- `12 = NOTIFICACION_RESULTADO_JUDICIAL`
- `13 = DDJJ_DOMICILIO_PRESENCIAL`
- `14 = ACTA_COMPLEMENTARIA`
- `15 = NOTA_ARCHIVO`
- `16 = DOCUMENTO_APREMIO`
- `17 = DOCUMENTO_EXTERNO_INCORPORADO`
- `18 = ANEXO_DOCUMENTAL`
- `19 = OTRO`

## TipoObservacion
Valores iniciales sugeridos:
- `1 = TECNICA`
- `2 = ADMINISTRATIVA`
- `3 = JURIDICA`
- `4 = FIRMA`
- `5 = NUMERACION`
- `6 = STORAGE`
- `7 = OTRA`

---

# 6) NOTAS FINALES

- Esta capa define el documento formal del proceso, no la evidencia.
- Las evidencias y observaciones propias del acta se revisarán luego como ampliación del núcleo operativo.
- El SQL físico final, índices definitivos, triggers, secuencias y constraints adicionales se definirán en la etapa de implementación.
- Este anexo congela la estructura lógica base de la **Capa 02**.
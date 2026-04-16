# Tablas documentales

## Finalidad

Este archivo define el bloque físico documental del sistema.

Incluye:

- `Documento`
- `ActaDocumento`
- `DocumentoFirma`
- `DocumentoObservacion`

Este bloque resuelve la identidad lógica del documento, su vínculo con el acta, su estado documental y la trazabilidad mínima de firma e incidencias documentales.

El storage físico del contenido se mantiene desacoplado mediante `StorageKey`.

---

## Criterios generales del bloque

- el documento tiene identidad lógica separada del archivo físico
- el storage del archivo se resuelve por `StorageKey`
- un documento puede tener número visible o no
- un documento puede:
  - generarse en borrador
  - firmarse digitalmente por integración externa
  - imprimirse y firmarse físicamente
  - reemplazarse o complementarse con versión firmada incorporada
- el documento no se acopla al motor de firma; solo guarda la información necesaria del resultado o estado documental

---

## Tabla: Documento

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdDocu` | `INT8` | No | PK |
| `TipoDocu` | `SMALLINT` | No | Tipo de documento |
| `EstadoDocu` | `SMALLINT` | No | Estado documental actual |
| `NroDocu` | `VARCHAR(30)` | Sí | Número visible si corresponde |
| `IdTalonario` | `INT` | Sí | Talonario usado si aplica |
| `TipoFirmaReq` | `SMALLINT` | Sí | Tipo de firma requerida |
| `StorageKey` | `VARCHAR(255)` | Sí | Referencia al storage físico/lógico |
| `HashDocu` | `VARCHAR(128)` | Sí | Hash del archivo vigente |
| `FhGeneracion` | `DATETIME YEAR TO SECOND` | Sí | Fecha/hora generación |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- `Documento` representa la entidad documental lógica.
- `StorageKey` desacopla el documento del mecanismo físico de almacenamiento.
- `NroDocu` puede ser nulo si todavía no corresponde numeración.
- `IdTalonario` aplica cuando el documento usa el bloque de numeración/talonarios.
- El archivo borrador no se conserva luego de la firma; el archivo firmado reemplaza al archivo previo.
- `HashDocu` representa el hash del archivo vigente almacenado.
- `HashDocu` con longitud `128` alcanza para hashes textuales habituales como SHA-256 hex (`64`) y SHA-512 hex (`128`).

---

## Tabla: ActaDocumento

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | Acta vinculada |
| `IdDocu` | `INT8` | No | Documento vinculado |
| `RolDocuActa` | `SMALLINT` | No | Rol del documento respecto del acta |
| `SiPrincipal` | `SMALLINT` | No | 0/1 documento principal para ese rol |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla resuelve la relación entre acta y documento.
- El mismo acta puede tener múltiples documentos.
- `RolDocuActa` evita que la relación sea una simple unión sin semántica.
- `SiPrincipal` permite distinguir el documento principal cuando hay más de uno del mismo rol.

---

## Tabla: DocumentoFirma

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdDocu` | `INT8` | No | Documento |
| `TipoFirma` | `SMALLINT` | No | Digital u ológrafa |
| `EstadoFirma` | `SMALLINT` | No | Estado de firma |
| `IdUserFirma` | `CHAR(36)` | Sí | Usuario que firmó |
| `FhSolicitud` | `DATETIME YEAR TO SECOND` | Sí | Fecha/hora solicitud |
| `FhFirma` | `DATETIME YEAR TO SECOND` | Sí | Fecha/hora firma |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla no reemplaza al motor de firma.
- Solo persiste trazabilidad mínima del proceso de firma.
- No se conserva archivo firmado separado del documento vigente.
- El usuario firmante puede registrarse mediante `IdUserFirma`.
- No se modelan roles de firma porque no aportan valor al dominio en este punto.

---

## Tabla: DocumentoObservacion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdDocu` | `INT8` | No | Documento |
| `ObsDocu` | `VARCHAR(255)` | No | Observación breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Fecha/hora alta |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla documenta observaciones específicas del documento.
- No reemplaza `Observacion` transversal del dominio si después se decide usarla para ciertos casos.
- Se deja separada porque el bloque documental suele requerir observaciones propias de circuito.

---

## Reglas generales del bloque

- El documento debe poder existir antes de la firma.
- La numeración puede producirse antes o durante la firma según política.
- El documento firmado reemplaza al archivo no firmado previamente almacenado.
- No se conservan versiones intermedias del archivo dentro de este modelo.
- La relación acta-documento debe ser explícita y con rol.
- El bloque documental debe mantenerse desacoplado del motor externo de firma.

---

## Enumeraciones del bloque

### TipoDocu
- `1 = ACTA`
- `2 = NOTIFICACION_ACTA`
- `3 = MEDIDA_PREVENTIVA`
- `4 = ACTO_ADMINISTRATIVO`
- `5 = NOTIFICACION_ACTO`
- `6 = CONSTANCIA`
- `7 = ANEXO`
- `8 = OTRO`

### EstadoDocu
- `1 = BORRADOR`
- `2 = GENERADO`
- `3 = PENDIENTE_FIRMA`
- `4 = FIRMADO`
- `5 = IMPRESO_PARA_FIRMA_OLOGRAFA`
- `6 = INCORPORADO_FIRMADO`
- `7 = ANULADO`

### RolDocuActa
- `1 = DOCUMENTO_PRINCIPAL_ACTA`
- `2 = DOCUMENTO_NOTIFICACION_ACTA`
- `3 = DOCUMENTO_MEDIDA_PREVENTIVA`
- `4 = DOCUMENTO_ACTO_ADMINISTRATIVO`
- `5 = DOCUMENTO_NOTIFICACION_ACTO`
- `6 = DOCUMENTO_ANEXO`
- `7 = DOCUMENTO_CONSTANCIA`

### TipoFirmaReq
- `1 = DIGITAL`
- `2 = OLOGRAFA`

### TipoFirma
- `1 = DIGITAL`
- `2 = OLOGRAFA`

### EstadoFirma
- `1 = PENDIENTE`
- `2 = EN_PROCESO`
- `3 = FIRMADO`
- `4 = RECHAZADO`
- `5 = ANULADO`
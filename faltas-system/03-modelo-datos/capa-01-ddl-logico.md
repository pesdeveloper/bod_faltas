# [ANEXO] DDL LÓGICO — CAPA 01 — NÚCLEO OPERATIVO

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 01.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) TABLA: Acta

## PK
- `PK_Acta (Id)`

## Uniques
- `UQ_Acta_IdTecnico (IdTecnico)`
- `UQ_Acta_NumeroActa` *(aplicable cuando el número exista y la política final lo requiera; puede resolverse como unique parcial o regla de aplicación según estrategia física)*

## FKs
- `FK_Acta_Inspector (IdInspector -> Inspectores.Id)`
- `FK_Acta_DomicilioPrincipal (IdActaDomicilioPrincipal -> ActaDomicilio.Id)`

## Campos

- `Id` → `INT8`
- `IdTecnico` → `CHAR(36)`
- `NumeroActa` → `VARCHAR(50)` NULL
- `TipoActa` → `SMALLINT`
- `SubtipoActa` → `SMALLINT` NULL
- `EstadoActual` → `SMALLINT`
- `EstadoAdministrativoActual` → `SMALLINT` NULL
- `FechaHoraLabrado` → `DATETIME YEAR TO SECOND`
- `FechaHoraHecho` → `DATETIME YEAR TO SECOND` NULL
- `FechaHoraUltimoEvento` → `DATETIME YEAR TO SECOND` NULL
- `IdInspector` → `INT`
- `VersionInspector` → `INT`
- `Latitud` → `DECIMAL(14,8)` NULL
- `Longitud` → `DECIMAL(14,8)` NULL
- `LugarReferencia` → `VARCHAR(255)` NULL
- `IdActaDomicilioPrincipal` → `INT8` NULL
- `SituacionDocumentalActual` → `SMALLINT` NULL
- `SituacionNotificacionActual` → `SMALLINT` NULL
- `SituacionPagoActual` → `SMALLINT` NULL
- `UltimoTipoEvento` → `SMALLINT` NULL
- `CanalOrigen` → `SMALLINT` NULL
- `FueOffline` → `SMALLINT`
- `Sincronizada` → `SMALLINT`
- `FechaHoraSync` → `DATETIME YEAR TO SECOND` NULL
- `Activa` → `SMALLINT`
- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`
- `UsuarioRegistro` → `VARCHAR(64)` NULL
- `ObservacionesInternas` → `VARCHAR(255)` NULL

## Observaciones de diseño

- `Acta` ya no contiene campos domiciliarios embebidos de persona/infractor.
- Se eliminan expresamente:
  - `CallePersonaTexto`
  - `NumeroPuertaPersona`
  - `IdProvinciaPersona`
  - `IdPartidoPersona`
  - `IdLocalidadPersona`
- El domicilio principal se resuelve mediante `IdActaDomicilioPrincipal`.
- La obligatoriedad de tener al menos un domicilio se resuelve por regla transaccional de aplicación.
- `EstadoActual`, `TipoActa`, `SituacionDocumentalActual`, `SituacionNotificacionActual` y `SituacionPagoActual` deben resolverse con enum/catálogo cerrado.

---

# 2) TABLA: ActaEvento

## PK
- `PK_ActaEvento (Id)`

## FKs
- `FK_ActaEvento_Acta (IdActa -> Acta.Id)`

## Índices sugeridos
- `IX_ActaEvento_IdActa_FechaHora`
- `IX_ActaEvento_TipoEvento`
- `IX_ActaEvento_OrigenEvento`

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `TipoEvento` → `SMALLINT`
- `OrigenEvento` → `SMALLINT`
- `FechaHoraEvento` → `DATETIME YEAR TO SECOND`
- `IdUsuario` → `VARCHAR(64)` NULL
- `Detalle` → `VARCHAR(255)` NULL
- `IdDocumento` → `INT8` NULL
- `IdDetalleActa` → `INT8` NULL
- `IdEventoOrigen` → `INT8` NULL
- `EsAnulado` → `SMALLINT`
- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`

## Observaciones de diseño

- Tabla append-only.
- Si un evento fue cargado de manera errónea, la corrección se expresa con otro evento; no borrando historia.
- `OrigenEvento` previsto:
  - `MANUAL`
  - `SISTEMA`
  - `PROCESO`
  - `INTEGRACION`

---

# 3) TABLA: ActaDomicilio

## PK
- `PK_ActaDomicilio (Id)`

## FKs
- `FK_ActaDomicilio_Acta (IdActa -> Acta.Id)`

## Índices sugeridos
- `IX_ActaDomicilio_IdActa`
- `IX_ActaDomicilio_IdActa_Principal`
- `IX_ActaDomicilio_IdActa_TipoDomicilio_Activo`

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `TipoDomicilio` → `SMALLINT`
- `EsDomicilioActa` → `SMALLINT`
- `EsPrincipal` → `SMALLINT`
- `Activo` → `SMALLINT`
- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`
- `CalleTexto` → `VARCHAR(48)`
- `NumeroPuerta` → `INT` NULL
- `Piso` → `VARCHAR(10)` NULL
- `Depto` → `VARCHAR(10)` NULL
- `Torre` → `VARCHAR(20)` NULL
- `IdProvincia` → `SMALLINT` NULL
- `IdPartido` → `INT` NULL
- `IdLocalidad` → `INT` NULL
- `CodigoPostal` → `CHAR(7)` NULL
- `DomicilioTexto` → `VARCHAR(255)` NULL
- `Email` → `VARCHAR(150)` NULL
- `Telefono` → `VARCHAR(14)` NULL
- `Observaciones` → `VARCHAR(255)` NULL

## Observaciones de diseño

- Toda acta debe tener al menos un registro en esta tabla.
- El domicilio inicial del acta debe nacer desde Capa 01.
- `EsDomicilioActa = 1` identifica el domicilio registrado como propio del acta en su origen.
- `EsPrincipal = 1` indica el domicilio principal operativo.
- `Acta.IdActaDomicilioPrincipal` debe referenciar uno de los domicilios de su misma acta.
- `DomicilioTexto` permite una versión consolidada o capturada del domicilio.
- `Email` y `Telefono` pertenecen al contexto del domicilio vinculado al acta.

---

# 4) TABLA: ActaEvidencia

## PK
- `PK_ActaEvidencia (Id)`

## FKs
- `FK_ActaEvidencia_Acta (IdActa -> Acta.Id)`

## Índices sugeridos
- `IX_ActaEvidencia_IdActa`
- `IX_ActaEvidencia_TipoEvidencia`

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `TipoEvidencia` → `SMALLINT`
- `MimeTypeCodigo` → `SMALLINT` NULL
- `NombreOriginal` → `VARCHAR(255)` NULL
- `StorageKey` → `VARCHAR(255)` NULL
- `UbicacionStorage` → `VARCHAR(255)` NULL
- `HashContenido` → `CHAR(64)` NULL
- `Observaciones` → `VARCHAR(255)` NULL
- `FechaHoraCaptura` → `DATETIME YEAR TO SECOND` NULL
- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`
- `Activa` → `SMALLINT`

## Observaciones de diseño

- Las evidencias pertenecen al acta, no al documento formal.
- La ubicación geográfica del hecho vive en `Acta`, no en cada evidencia.

---

# 5) TABLA: ActaObservacion

## PK
- `PK_ActaObservacion (Id)`

## FKs
- `FK_ActaObservacion_Acta (IdActa -> Acta.Id)`

## Índices sugeridos
- `IX_ActaObservacion_IdActa`
- `IX_ActaObservacion_TipoObservacion`

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `TipoObservacion` → `SMALLINT`
- `Observacion` → `VARCHAR(255)`
- `FechaHoraRegistro` → `DATETIME YEAR TO SECOND`
- `IdUsuario` → `VARCHAR(64)` NULL
- `Activa` → `SMALLINT`

## Observaciones de diseño

- Las observaciones operativas del acta tienen entidad propia.
- No reemplazan eventos jurídicamente relevantes.

---

# 6) TABLA: ActaTransito

## PK
- `PK_ActaTransito (IdActa)`

## FKs
- `FK_ActaTransito_Acta (IdActa -> Acta.Id)`

## Campos

- `IdActa` → `INT8`
- `TieneRetencionLicencia` → `SMALLINT`
- `TieneRetencionVehiculo` → `SMALLINT`
- `TieneSecuestroVehiculo` → `SMALLINT`
- `DominioVehiculo` → `VARCHAR(20)` NULL
- `Observaciones` → `VARCHAR(255)` NULL

## Observaciones de diseño

- Extensión 1:1 para tránsito.
- Las banderas operativas de tránsito no equivalen a medidas preventivas formales con resolución del intendente.

---

# 7) TABLA: ActaContravencion

## PK
- `PK_ActaContravencion (IdActa)`

## FKs
- `FK_ActaContravencion_Acta (IdActa -> Acta.Id)`

## Campos

- `IdActa` → `INT8`
- `RubroContravencional` → `SMALLINT` NULL
- `TieneMedidaPreventiva` → `SMALLINT`
- `Observaciones` → `VARCHAR(255)` NULL

## Observaciones de diseño

- Extensión 1:1 para contravenciones no tránsito.

---

# 8) TABLA: ActaContravencionMedida

## PK
- `PK_ActaContravencionMedida (Id)`

## FKs
- `FK_ActaContravencionMedida_ActaContravencion (IdActa -> ActaContravencion.IdActa)`

## Índices sugeridos
- `IX_ActaContravencionMedida_IdActa`
- `IX_ActaContravencionMedida_TipoMedida`

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `TipoMedida` → `SMALLINT`
- `EstadoMedida` → `SMALLINT`
- `FechaHoraInicio` → `DATETIME YEAR TO SECOND`
- `FechaHoraFin` → `DATETIME YEAR TO SECOND` NULL
- `Observaciones` → `VARCHAR(255)` NULL
- `Activa` → `SMALLINT`

## Observaciones de diseño

- Múltiples medidas por acta contravencional.
- No reemplaza el eventual documento resolutivo formal.

---

# 9) TABLA: Inspectores

## PK
- `PK_Inspectores (Id)`

## Uniques
- `UQ_Inspectores_Legajo (Legajo)` *(si aplica en el dominio)*

## Campos

- `Id` → `INT`
- `Legajo` → `VARCHAR(30)` NULL
- `ApellidoNombre` → `VARCHAR(120)`
- `Dependencia` → `VARCHAR(120)` NULL
- `Cargo` → `VARCHAR(120)` NULL
- `Activo` → `SMALLINT`
- `VersionActual` → `INT`
- `FechaHoraActualizacion` → `DATETIME YEAR TO SECOND` NULL

## Observaciones de diseño

- Catálogo vivo de inspectores.
- No garantiza por sí solo integridad histórica.

---

# 10) TABLA: InspectoresSnapshot

## PK
- `PK_InspectoresSnapshot (IdInspector, VersionInspector)`

## FKs
- `FK_InspectoresSnapshot_Inspectores (IdInspector -> Inspectores.Id)`

## Campos

- `IdInspector` → `INT`
- `VersionInspector` → `INT`
- `Legajo` → `VARCHAR(30)` NULL
- `ApellidoNombre` → `VARCHAR(120)`
- `Dependencia` → `VARCHAR(120)` NULL
- `Cargo` → `VARCHAR(120)` NULL
- `FechaHoraSnapshot` → `DATETIME YEAR TO SECOND`

## Observaciones de diseño

- La acta referencia historia mediante:
  - `IdInspector`
  - `VersionInspector`
- Se preserva el contexto histórico aunque el inspector cambie luego en el catálogo vivo.

---

# 11) RELACIÓN FUTURA: CmteXActa

## Estado
- prevista
- no detallada aún en esta capa

## Observación
Se deja reservada como futura extensión para integración con comprobantes u otras proyecciones transversales.

---

# 12) Notas de consistencia entre tablas

## Regla 1
Toda `Acta` debe tener al menos un `ActaDomicilio`.

## Regla 2
`Acta.IdActaDomicilioPrincipal`, si no es null, debe apuntar a un domicilio perteneciente a la misma acta.

## Regla 3
La secuencia normal de alta es:

1. insertar `Acta`
2. insertar `ActaDomicilio`
3. marcar principal
4. actualizar `Acta.IdActaDomicilioPrincipal`

## Regla 4
`ActaEvento` es append-only.

## Regla 5
Los snapshots de `Acta` no reemplazan el historial ni las capas específicas posteriores.
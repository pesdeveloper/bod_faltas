# Tablas núcleo expediente

## Finalidad

Este archivo define el núcleo físico mínimo y operativo del expediente.

Incluye:

- `Acta`
- `ActaSnapshot`
- `ActaEvento`
- `Observacion`

Aunque el modelo completo del acta incluye más tablas, este bloque concentra la cabecera principal, la proyección operativa, el historial y la observación transversal.

---

## Tabla: Acta

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdTecnico` | `CHAR(36)` | No | GUID técnico único |
| `NroActa` | `VARCHAR(20)` | No | Número visible obligatorio en central |
| `TipoActa` | `SMALLINT` | No | Enum tipo de acta |
| `OrigenCaptura` | `SMALLINT` | No | Enum origen de captura |
| `FhActa` | `DATETIME YEAR TO SECOND` | No | Fecha/hora de labrado o emisión final |
| `IdDep` | `INT` | No | Dependencia del acta |
| `VerDep` | `SMALLINT` | No | Versión congelada de dependencia |
| `IdInsp` | `INT` | No | Inspector asignado |
| `VerInsp` | `SMALLINT` | No | Versión congelada de inspector |
| `IdTcaInfr` | `CHAR(5)` | Sí | Calle de infracción desde catálogo municipal |
| `VerTcaInfr` | `SMALLINT` | Sí | Versión de calle municipal |
| `AltInfr` | `INT` | Sí | Altura de infracción |
| `IdTcaE1Infr` | `CHAR(5)` | Sí | Entrecalle 1 |
| `VerTcaE1Infr` | `SMALLINT` | Sí | Versión entrecalle 1 |
| `IdTcaE2Infr` | `CHAR(5)` | Sí | Entrecalle 2 |
| `VerTcaE2Infr` | `SMALLINT` | Sí | Versión entrecalle 2 |
| `IdLocInfr` | `CHAR(2)` | Sí | Localidad local de infracción |
| `VerLocInfr` | `SMALLINT` | Sí | Versión localidad local |
| `IdBarInfr` | `SMALLINT` | Sí | Barrio de infracción |
| `SiDomTxtInfr` | `SMALLINT` | No | 0/1, habilita domicilio textual excepcional |
| `DomTxtInfr` | `VARCHAR(150)` | Sí | Domicilio textual si no pudo normalizarse |
| `SiEjeUrb` | `SMALLINT` | Sí | 0/1 obtenido de catálogo territorial |
| `NomInfct` | `VARCHAR(64)` | Sí | Nombre y apellido o razón social |
| `DocPrefInfct` | `SMALLINT` | Sí | Prefijo documento/CUIT/CUIL |
| `DocNroInfct` | `INT` | Sí | Número principal de documento |
| `DocDigVerInfct` | `SMALLINT` | Sí | Dígito verificador |
| `TipoPersInfct` | `SMALLINT` | Sí | 1 física / 2 jurídica |
| `IdProvInfct` | `SMALLINT` | Sí | Provincia domicilio infractor |
| `VerProvInfct` | `SMALLINT` | Sí | Versión provincia |
| `IdMuniInfct` | `INT` | Sí | Municipio lógico del infractor |
| `VerMuniInfct` | `SMALLINT` | Sí | Versión municipio |
| `IdDptoInfct` | `INT` | Sí | Departamento fallback/real |
| `VerDptoInfct` | `SMALLINT` | Sí | Versión departamento |
| `IdLocInfct` | `INT8` | Sí | Localidad del infractor |
| `VerLocInfct` | `SMALLINT` | Sí | Versión localidad |
| `IdLocCenInfct` | `INT8` | Sí | Localidad censal |
| `VerLocCenInfct` | `SMALLINT` | Sí | Versión localidad censal |
| `IdCalleInfct` | `INT8` | Sí | Calle INDEC del infractor |
| `VerCalleInfct` | `SMALLINT` | Sí | Versión calle INDEC |
| `AltInfct` | `INT` | Sí | Altura domicilio infractor |
| `PisoInfct` | `VARCHAR(10)` | Sí | Piso |
| `DeptoInfct` | `VARCHAR(10)` | Sí | Depto |
| `ObsDomInfct` | `VARCHAR(120)` | Sí | Aclaración breve domicilio infractor |
| `CodPosInfct` | `VARCHAR(10)` | Sí | Código postal editable |
| `ObsActa` | `LVARCHAR` | Sí | Texto libre propio del acta |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario que dio alta |

### Notas
- `NroActa` no puede ser `NULL` en la base central.
- El estado `BORRADOR` puede existir a nivel de proceso, pero no invalida que el número visible exista al persistir en central si así lo exige el mecanismo de numeración.
- El domicilio de la infracción debe ser preferentemente normalizado.
- El partido no se persiste como dato del domicilio de infracción.
- `SiDomTxtInfr = 1` habilita `DomTxtInfr`.
- `SiEjeUrb` se obtiene a partir de calle + altura y puede persistirse como dato derivado útil.
- `ObsActa` representa contenido propio del acta, no auditoría interna.

---

## Tabla: ActaSnapshot

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK 1:1 a acta |
| `BloqueActual` | `SMALLINT` | No | Bloque operativo actual |
| `EstProcAct` | `SMALLINT` | No | Estado de proceso actual |
| `SitAdmAct` | `SMALLINT` | No | Situación administrativa global |
| `SiCerrada` | `SMALLINT` | No | 0/1 |
| `SiPermiteReing` | `SMALLINT` | No | 0/1 |
| `TipoCierreAct` | `SMALLINT` | Sí | Tipo de cierre actual |
| `EstPagoAct` | `SMALLINT` | No | Estado de pago actual |
| `SiTieneDesc` | `SMALLINT` | No | 0/1 tiene descargo |
| `SiTieneMedPrev` | `SMALLINT` | No | 0/1 tiene medida preventiva |
| `EstDocAct` | `SMALLINT` | No | Estado documental actual |
| `EstNotifAct` | `SMALLINT` | No | Estado notificación actual |
| `CantDocsPendFirma` | `SMALLINT` | No | Cantidad de docs pendientes de firma |
| `CantDocsFirmados` | `SMALLINT` | No | Cantidad de docs firmados |
| `IdEvtUlt` | `INT8` | Sí | Último evento proyectado |
| `IdDocuUlt` | `INT8` | Sí | Último documento relacionado |
| `IdNotifUlt` | `INT8` | Sí | Última notificación relacionada |
| `FhUltMod` | `DATETIME YEAR TO SECOND` | Sí | Última modificación proyectada |
| `IdUserUltMod` | `CHAR(36)` | Sí | Usuario última modificación proyectada |
| `FhSnapshot` | `DATETIME YEAR TO SECOND` | No | Momento de proyección |

### Notas
- `ActaSnapshot` es derivado y regenerable.
- No es fuente primaria de verdad.
- Su función es facilitar consulta operativa, navegación por bandejas y performance.
- `FhUltMod` e `IdUserUltMod` se persisten acá como dato proyectado, no como dato propio de `Acta`.

---

## Tabla: ActaEvento

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActa` | `INT8` | No | FK a acta |
| `FhEvt` | `DATETIME YEAR TO SECOND` | No | Fecha/hora del evento |
| `TipoEvt` | `SMALLINT` | No | Enum de evento |
| `OrigenEvt` | `SMALLINT` | No | Enum origen evento |
| `BloqueFunc` | `SMALLINT` | No | Bloque funcional al que aplica |
| `EstProcAnt` | `SMALLINT` | Sí | Estado proceso anterior |
| `EstProcNvo` | `SMALLINT` | Sí | Estado proceso nuevo |
| `SitAdmAnt` | `SMALLINT` | Sí | Situación administrativa anterior |
| `SitAdmNva` | `SMALLINT` | Sí | Situación administrativa nueva |
| `IdDocuRel` | `INT8` | Sí | Documento relacionado |
| `IdNotifRel` | `INT8` | Sí | Notificación relacionada |
| `IdPresRel` | `INT8` | Sí | Presentación relacionada |
| `IdUserEvt` | `CHAR(36)` | Sí | Usuario que generó el evento |
| `SiEvtCierre` | `SMALLINT` | No | 0/1 |
| `SiEvtExt` | `SMALLINT` | No | 0/1 |
| `SiPermiteReing` | `SMALLINT` | No | 0/1 derivado del evento |

### Notas
- `ActaEvento` es append-only.
- No lleva texto libre embebido.
- Las observaciones operativas o justificativas se resuelven en `Observacion`.
- `BloqueFunc`, `SiEvtCierre`, `SiEvtExt` y `SiPermiteReing` forman parte de la interpretación operativa del evento.

---

## Tabla: Observacion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdRef` | `SMALLINT` | No | Enum que identifica tabla origen |
| `IdFk` | `INT8` | No | Id del registro origen |
| `Obs` | `VARCHAR(255)` | No | Observación breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Fecha/hora alta |
| `IdUser` | `CHAR(36)` | No | Usuario autor |

### Notas
- `Observacion` es transversal y no reemplaza campos propios de texto largo dentro de una entidad.
- Si se mantiene la regla de edición, solo debería poder editarla el usuario autor.
- Toda edición relevante debe generar evento de auditoría.

---

## Enumeraciones del bloque

### TipoActa
- `1 = TRANSITO`
- `2 = CONTRAVENCION`
- `3 = SUSTANCIAS_ALIMENTICIAS`

### OrigenCaptura
- `1 = MOBILE_ONLINE`
- `2 = MOBILE_OFFLINE`
- `3 = WEB`
- `4 = CARGA_ADMINISTRATIVA`
- `5 = IMPORTACION`
- `6 = INTEGRACION_EXTERNA`

### TipoPersInfct
- `1 = FISICA`
- `2 = JURIDICA`

### BloqueActual
- `1 = D1_LABRADO`
- `2 = D2_VALIDACION_ENRIQUECIMIENTO`
- `3 = D3_NOTIFICACION_ACTA`
- `4 = D4_ANALISIS_PRESENTACIONES_PAGOS`
- `5 = D5_ACTO_ADMINISTRATIVO`
- `6 = D6_NOTIFICACION_ACTO`
- `7 = D7_APELACION`
- `8 = D8_GESTION_EXTERNA`
- `9 = D8_ARCHIVO_CIERRE`

### EstProcAct
- `0 = BORRADOR`
- `1 = LABRADA`
- `2 = EN_REVISION`
- `3 = VALIDADA`
- `4 = OBSERVADA`
- `5 = LISTA_PARA_NOTIFICAR`
- `6 = NOTIFICACION_EN_PROCESO`
- `7 = NOTIFICADA`
- `8 = EN_ANALISIS`
- `9 = PENDIENTE_ACTO`
- `10 = ACTO_EN_PROCESO`
- `11 = ACTO_FIRMADO`
- `12 = PENDIENTE_NOTIFICACION_ACTO`
- `13 = NOTIFICACION_ACTO_EN_PROCESO`
- `14 = APELADA`
- `15 = EN_GESTION_EXTERNA`
- `16 = ARCHIVADA`
- `17 = CERRADA`

### SitAdmAct
- `1 = PRE_ADMINISTRATIVA`
- `2 = ADMINISTRATIVA_ACTIVA`
- `3 = EN_ANALISIS`
- `4 = EN_APELACION`
- `5 = EN_GESTION_EXTERNA`
- `6 = PARALIZADA`
- `7 = ARCHIVADA`
- `8 = CERRADA`

### TipoCierreAct
- `1 = ARCHIVO_ADMINISTRATIVO`
- `2 = CIERRE_PAGO`
- `3 = CIERRE_RESOLUCION`
- `4 = CIERRE_GESTION_EXTERNA`
- `5 = CIERRE_OTRO`

### EstPagoAct
- `1 = SIN_PAGO`
- `2 = INTENCION_DE_PAGO`
- `3 = PAGO_PENDIENTE_CONFIRMACION`
- `4 = PAGO_CONFIRMADO`
- `5 = PAGO_PARCIAL`
- `6 = PAGO_RECHAZADO`
- `7 = CONDONADO`
- `8 = NO_APLICA`

### EstDocAct
- `1 = SIN_DOCUMENTOS`
- `2 = DOCUMENTO_BORRADOR`
- `3 = PENDIENTE_FIRMA`
- `4 = FIRMADO`
- `5 = IMPRESO_PARA_FIRMA_OLOGRAFA`
- `6 = INCORPORADO_FIRMADO`
- `7 = ANULADO`

### EstNotifAct
- `1 = NO_GENERADA`
- `2 = PENDIENTE_EMISION`
- `3 = EMITIDA`
- `4 = EN_PROCESO`
- `5 = NOTIFICADA`
- `6 = SIN_ACUSE`
- `7 = VENCIDA`
- `8 = FALLIDA`
- `9 = NO_APLICA`

### TipoEvt
- `0 = ACTA_CREADA_EN_BORRADOR`
- `1 = ACTA_LABRADA`
- `2 = BORRADOR_DESCARTADO`
- `3 = ACTA_VALIDADA`
- `4 = ACTA_OBSERVADA`
- `5 = ACTA_ANULADA`
- `6 = DOCUMENTO_GENERADO`
- `7 = DOCUMENTO_FIRMADO`
- `8 = NOTIFICACION_EMITIDA`
- `9 = NOTIFICACION_CONFIRMADA`
- `10 = PRESENTACION_REGISTRADA`
- `11 = PAGO_REGISTRADO`
- `12 = ACTO_ADMINISTRATIVO_DICTADO`
- `13 = APELACION_INTERPUESTA`
- `14 = DERIVACION_EXTERNA`
- `15 = ARCHIVO_DISPUESTO`
- `16 = REINGRESO_DISPUESTO`
- `17 = MEDIDA_PREVENTIVA_APLICADA`
- `18 = MEDIDA_PREVENTIVA_LEVANTADA`

### OrigenEvt
- `1 = USUARIO_INTERNO`
- `2 = SISTEMA`
- `3 = INTEGRACION_EXTERNA`
- `4 = PROCESO_BATCH`
- `5 = PORTAL_CIUDADANO`
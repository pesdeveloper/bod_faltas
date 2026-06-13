# Inventario de spec y persistencia para cruce con dominio real

**Fecha:** 2026-06-13
**Slice:** Inventario previo al gap analysis
**Documento siguiente propuesto:** `DOMINIO_REAL_CRUCE_SPEC_PERSISTENCIA.md`
**Fuente de referencia del prototipo:** `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md`

---

## Nota de contexto tecnologico

La spec DDL existente esta disenada para **Informix 12.10**.

**Decision tecnica incorporada a este slice:** el sistema real **no usara Informix**. El destino sera **MySQL**.

En este documento:
- No se adaptan scripts ni tipos a MySQL.
- No se modifica ninguna fuente de persistencia.
- Las tablas de `spec/13-ddl/` y `spec/14-sql-operativo/` se tratan como modelo conceptual/historico.
- Se identifican tipos, construcciones y decisiones acoplados a Informix.
- Se marca cada estructura como **portable**, **portable con ajuste menor** o **requiere rediseno**.
- Se agregan observaciones de compatibilidad MySQL donde son evidentes.

---

## 1. Fuentes revisadas

| Fuente | Estado | Archivos relevantes revisados | Observaciones |
|---|---|---|---|
| `spec/01-dominio/` | Revisada completa | 12 archivos .md | Todos leidos sin truncar |
| `spec/02-reglas-transversales/` | Revisada completa | 8 archivos .md | Todos leidos sin truncar |
| `spec/04-backend/` | Revisada completa | 11 archivos .md + 7 en subdirectorio | Incluye `08-persistencia-y-sql/` completo |
| `spec/12-datos/` | Revisada completa | 8 archivos .md | Incluye `00-convencion-nombres-fisicos.md` |
| `spec/13-ddl/` | Revisada completa | 15 archivos .md + 4 diagramas .mmd | Enums extraidos de cada tabla |
| `spec/14-sql-operativo/` | Revisada completa | 16 archivos (todos) | Completados en este slice: `03a`, `03b`, `03c`, `07a`, `07b`, `07c`, `07d`, `08` |
| `spec/03-bandejas/` | Revisada completa | 14 archivos .md | 12 bandejas operativas + indice maestro + documento prototipo-validacion |
| `backend/api-faltas-prototipo/docs/DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md` | Revisada completa | 1 archivo (621 lineas) | Documento base del prototipo validado |
| `backend/persistencia/` | No existe como directorio propio | -- | La persistencia vive en `spec/12-datos/`, `spec/13-ddl/`, `spec/14-sql-operativo/` |

**Fuentes parciales declaradas:** ninguna. Todas las fuentes de `spec/` relevantes fueron revisadas en este slice o en el slice anterior. El inventario queda completo.

---

## 2. Inventario spec/dominio

### 2.1 Entidades y agregados definidos

| Nombre exacto (spec) | Archivo principal | Descripcion breve | Tipo |
|---|---|---|---|
| `Acta` / `Expediente` | `01-acta.md`, `00-mapa-dominio.md` | Unidad principal del expediente de faltas | Agregado raiz |
| `ActaEvento` | `02-acta-evento.md` | Historial append-only de hechos del expediente | Entidad de trazabilidad |
| `Documento` | `03-documento.md` | Pieza formal del expediente | Entidad documental |
| `Notificacion` | `04-notificacion.md` | Proceso transversal de comunicacion sobre documentos | Entidad de proceso |
| `SnapshotOperativo` | `05-snapshot-operativo.md` | Proyeccion derivada para bandejas y operacion | Proyeccion regenerable |
| `Dependencia` | `08-dependencia.md` | Entidad organizacional con versionado | Entidad referencial versionada |
| `Inspector` | `09-inspector.md` | Actor operativo del labrado, versionado | Entidad referencial versionada |
| `Talonario y numeracion` | `11-talonario-y-numeracion.md` | Motor de numeracion administrativa transversal | Mecanismo transversal |
| Medidas y liberaciones | `06-medidas-y-liberaciones.md` | Concepto agregado: medidas preventivas y pendientes materiales | Bloque conceptual |
| Pendiente material | `07-reglas-de-pendientes-materiales.md` | Situacion material no resuelta que impide cierre | Concepto operativo |
| `StorageKey` | `03-documento.md`, `00-regla-gestor-documental.md` | Referencia tecnica al archivo en storage externo | Referencia tecnica |

**Nota:** La spec dominio deliberadamente evita DDL. No define tablas, campos, tipos de dato, claves ni indices -- eso esta diferido a `spec/12-datos/` y `spec/13-ddl/`.

### 2.2 Estados y enums definidos en spec/dominio

| Nombre del enum/grupo (spec) | Archivo | Valores listados |
|---|---|---|
| Estados del expediente / acta | `07-catalogos-y-estados.md` | borrador, labrada, en revision, en analisis, pendiente de resolucion, pendiente de fallo, pendiente de firma, en notificacion, con apelacion, en gestion externa, paralizada, en archivo, cerrada |
| Tipos de evento del expediente | `07-catalogos-y-estados.md` | creacion en borrador, descarte, generacion documental, firma, notificacion, acuse, presentacion, pago, medida aplicada, medida levantada, liberacion material, archivo, cierre, paralizacion, reanudacion, reingreso |
| Tipos de documento | `07-catalogos-y-estados.md` | acta, resolucion, fallo, medida preventiva, documento de notificacion, documento de liberacion, sentencia, constancia, otra pieza administrativa |
| Estados del documento | `07-catalogos-y-estados.md` | generado, pendiente de firma, firmado, pendiente de notificacion, en notificacion, notificado, observado, anulado, reemplazado |
| Canales de notificacion | `07-catalogos-y-estados.md` | domicilio electronico, email, postal, bluemail, notificador municipal, portal ciudadano, otro canal formal |
| Estados de notificacion | `07-catalogos-y-estados.md` | pendiente de notificar, en proceso, acuse pendiente, acuse positivo, acuse negativo, vencida sin acuse, reintento pendiente, decision manual pendiente, finalizada |
| Estados de medida preventiva | `07-catalogos-y-estados.md` | pendiente de generar, generada, pendiente de firma, firmada, activa, en levantamiento, levantada |
| Tipos de pendientes materiales | `07-catalogos-y-estados.md` | liberacion de rodado pendiente, restitucion de documentacion pendiente, devolucion de licencia pendiente, otra restitucion material pendiente |
| Motivos de archivo | `07-catalogos-y-estados.md` | medida preventiva activa, liberacion pendiente, restitucion pendiente, bloqueo operativo de cierre, otro motivo |
| Motivos de cierre | `07-catalogos-y-estados.md` | pago cumplido sin bloqueos, absolucion sin bloqueos, nulidad sin bloqueos, otra causal valida |
| Resultados de gestion externa | `07-catalogos-y-estados.md` | sin novedad, requiere reingreso, requiere nuevo fallo, pago externo, cierre externo, otro resultado relevante |
| Origen de actuacion o evento | `07-catalogos-y-estados.md` | web Direccion de Faltas, mobile inspectores, mobile notificador, mobile liberaciones, integracion externa, proceso interno |
| Modalidades de numeracion | `11-talonario-y-numeracion.md` | electronica, manual fisica / preimpresa |

### 2.3 Eventos definidos en spec/dominio (lista conceptual)

La spec conceptual no enumera tipos de evento con nombres exactos de codigo. Solo describe categorias funcionales:
- creacion/incorporacion del expediente
- validaciones, observaciones, anulaciones, cambios de curso
- generacion, firma, emision, recepcion de documentos
- inicio, resultado o novedad de notificaciones
- imposicion, levantamiento o resultado de medidas
- presentaciones, comparecencias, pagos, apelaciones, decisiones
- derivaciones, reingresos, cierres, archivos, reaperturas
- paralizacion y reanudacion

Los nombres exactos de codigo de eventos estan en el catalogo DDL (`TipoEvt` en `spec/13-ddl/02-tablas-nucleo-expediente.md`) y en el prototipo (seccion 4 de `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md`).

### 2.4 Reglas de negocio clave (relevantes para el cruce)

- `Acta` = unidad principal; historial vive en `ActaEvento`, no en el acta misma.
- `SnapshotOperativo` = derivado y regenerable; no es fuente de verdad.
- Bandejas = proyecciones operativas, no fuente primaria.
- `Documento` != `Notificacion` -- entidades separadas aunque relacionadas.
- Archivo != Cerrada -- distincion critica.
- Firma externa al dominio; el sistema reacciona al resultado.
- Reingreso = reactivacion sin borrar historia; nuevo evento, no nuevo expediente.
- Medida activa o pendiente material bloquean cierre; pueden llevar a archivo.

### 2.5 Plazos numericos explicitos en spec

- Notificacion electronica: **7 dias** de espera.
- Fallo notificado: **5 dias** antes del siguiente destino natural.

### 2.6 Decisiones pendientes / notas abiertas en spec/dominio

- Catalogo definitivo de tipos de medida preventiva pendiente de operatoria real.
- Listado definitivo de tipos documentales firmables a alinear con catalogo.
- Estructura concreta del motor de numeracion diferida a modelo logico y persistencia/SQL.

---

## 3. Inventario spec/03-bandejas

### 3.1 Modelo de bandeja

Las bandejas son **proyecciones operativas**, no fuente de verdad primaria.

Citado literalmente de `00-indice-maestro-bandejas.md`:

> "La ubicacion operativa de cada expediente en estas bandejas debe poder proyectarse en `ActaSnapshot` mediante codigo de bandeja, visibilidad operativa y estado procesal actual."

El mecanismo tecnico de proyeccion es el campo `CodBandeja VARCHAR(50)` de `FalActaSnapshot`. La bandeja actual del expediente se proyecta desde el snapshot, no al reves.

Esto confirma formalmente: **bandeja = proyeccion derivada**. La fuente de verdad sigue siendo `FalActa` + `FalActaEvento`.

### 3.2 Bandejas definidas (12 bandejas operativas)

| # | Nombre bandeja | Archivo | Regla de entrada | Destinos de salida tipicos | Campos snapshot clave |
|---|---|---|---|---|---|
| 1 | Labradas | `01-bandeja-labradas.md` | Acta labrada, primer ingreso al circuito operativo | Enriquecimiento, analisis, nulidad, pago voluntario inicial | `BloqueActual=1`, `CodBandeja`, `FhActa` |
| 2 | Enriquecimiento | `02-bandeja-enriquecimiento.md` | Salio de labradas; requiere revision o completitud documental inicial | Pendientes firma, notificaciones, fallo, resolucion, analisis, archivo, cerradas | `BloqueActual=2`, `CodBandeja`, flags de completitud documental |
| 3 | Analisis / Presentaciones / Pagos | `03-bandeja-analisis-presentaciones-pagos.md` | Actuacion o novedad que exige analisis material (pago, presentacion, descargo, reingreso desde notificacion o gestion externa) | Pendientes resolucion, fallo, firma, notificaciones, archivo, cerradas, paralizadas | `SiPagoVolunt`, `FhPagoVolunt`, `MontoActa`, `EstPagoAct`, `SiPlanPago` |
| 4 | Pendientes de resolucion / redaccion | `04-bandeja-pendientes-resolucion-redaccion.md` | Se definio que necesita pieza no-fallo (resolucion, nulidad, medida preventiva, rectificacion u otra pieza administrativa) | Pendientes firma, notificaciones, archivo, cerradas, paralizadas | `CodBandeja`, `EstProcAct`, tipo de pieza esperada |
| 5 | Pendientes de fallo | `05-bandeja-pendientes-fallo.md` | Condicion suficiente de requerir fallo: vencimiento de plazo, sin comparecencia, reingreso de gestion externa, rectificacion de fallo | Pendientes firma, analisis, archivo, paralizadas | `CodBandeja`, `EstProcAct`, `FhVtoPresentacion` |
| 6 | Pendientes de firma | `06-bandeja-pendientes-firma.md` | Existe al menos un documento generado pendiente de firma; firmas necesarias aun no completadas | Notificaciones, archivo, cerradas, analisis, paralizadas | `EstDocAct`, `BloqueActual` |
| 7 | Notificaciones | `07-bandeja-notificaciones.md` | Existe pieza notificable y corresponde iniciar o continuar su notificacion (acta, medida preventiva, resolucion, fallo, sentencia, liberacion u otra pieza formal) | Cualquier bandeja segun resultado + canal + acuse + plazo | `SiNotifActa`, `SiNotifFallo`, `SiNotifMedPrev`, `SiNotifActaProc`, `SiNotifFalloProc`, `CantReintNotif` |
| 8 | Con apelacion | `08-bandeja-con-apelacion.md` | Se registro apelacion o recurso equivalente con efecto real (NO solo por plazo habilitado; requiere actuacion recursiva concreta) | Resolucion/redaccion, fallo, firma, notificaciones, gestion externa, archivo, cerradas, paralizadas | `BloqueActual=7`, `FhVtoApelacion` |
| 9 | Gestion externa | `09-bandeja-gestion-externa.md` | Derivacion formal a apremios, Juzgado de Paz u otra gestion externa formal admitida | Pendientes fallo, analisis, archivo, cerradas, paralizadas | `SiGestionExt`, `TipoGestionExt`, `ResultadoGestionExt`, `SiReingresoGestionExt`, `FhVtoApremio` |
| 10 | Paralizadas | `10-bandeja-paralizadas.md` | Decision fundada de paralizacion desde cualquier bandeja activa | Analisis, fallo, resolucion, firma, notificaciones, gestion externa, archivo, cerradas | `SitAdmAct=6 (PARALIZADA)` -- sin campo de motivo en snapshot: **gap critico** |
| 11 | Archivo | `11-bandeja-archivo.md` | Tramite principal resuelto pero persiste causal bloqueante de cierre (medida activa, liberacion pendiente, restitucion pendiente, otro bloqueo operativo) | Cerradas, analisis, resolucion, fallo, firma, notificaciones, paralizadas | Sin campo de motivo de archivo ni causales bloqueantes estructuradas en snapshot: **gap critico** |
| 12 | Cerradas | `12-bandeja-cerradas.md` | Cumple completamente todas las condiciones de cierre (sin bloqueos de ningun tipo) | Sin salida operativa normal; solo reingreso excepcional formal y trazable | `EstProcAct=17 (CERRADA)`, `SitAdmAct=8 (CERRADA)` |

**Archivo adicional:** `99-prototipo-validacion-direccion.md` -- No es una bandeja operativa. Es el documento de alcance del prototipo de validacion funcional con la Direccion de Faltas. Confirma decision tecnica: estado en memoria, dataset mock, sin BD real, sin Informix. Sin impacto en el gap analysis de persistencia.

### 3.3 Relacion bandejas vs BloqueActual (DDL)

| Bandeja | BloqueActual DDL | Suficiencia del mapeo |
|---|---|---|
| 1 Labradas | `D1_LABRADO = 1` | Directo |
| 2 Enriquecimiento | `D2_VALIDACION_ENRIQUECIMIENTO = 2` | Directo |
| 3 Analisis/Pagos | No tiene bloque exclusivo -- combina `EstProcAct` + `CodBandeja` | Parcial -- comparte espacio con estados post-notificacion |
| 4 Pendientes resolucion | No tiene bloque exclusivo -- combina `EstProcAct` + `CodBandeja` | Parcial -- no existe `D4_PENDIENTE_RESOLUCION` en DDL |
| 5 Pendientes fallo | No tiene bloque exclusivo -- combina `EstProcAct` + `CodBandeja` | Parcial -- no existe `D5_PENDIENTE_FALLO` en DDL; `D5_ACTO_ADMINISTRATIVO=5` cubre el proceso del acto |
| 6 Pendientes firma | No tiene bloque exclusivo -- combina `EstDocAct` + `CodBandeja` | Parcial -- `D3_NOTIFICACION_ACTA=3` es lo mas cercano pero no exclusivo |
| 7 Notificaciones | `D3_NOTIFICACION_ACTA = 3` / `D6_NOTIFICACION_ACTO = 6` | Parcial -- 2 bloques para una bandeja unificada |
| 8 Con apelacion | `D7_APELACION = 7` | Directo |
| 9 Gestion externa | `D8_GESTION_EXTERNA = 8` | Directo |
| 10 Paralizadas | `SitAdmAct = 6` (PARALIZADA) -- no es BloqueActual propio | Indirecto -- no existe bloque propio de paralizacion |
| 11 Archivo | `D8_ARCHIVO_CIERRE = 9` (parcial) | Parcial -- archivo y cerrada comparten bloque 9 |
| 12 Cerradas | `D8_ARCHIVO_CIERRE = 9` (parcial) | Parcial -- mismo bloque que archivo |

**Gap critico BloqueActual vs Bandejas:** El DDL define 9 valores de `BloqueActual` (D1 a D8_ARCHIVO_CIERRE). La spec de bandejas define 12 situaciones operativas distintas. Las bandejas 3, 4, 5, 6, 10, 11 no tienen bloques exclusivos en DDL. La diferenciacion requiere combinacion de `BloqueActual + EstProcAct + CodBandeja`. El campo `CodBandeja VARCHAR(50)` en `FalActaSnapshot` es el discriminador operativo clave.

### 3.4 Hallazgos criticos por bandeja

**Bandeja 7 Notificaciones -- transversal unificada:**
`07-bandeja-notificaciones.md` confirma explicitamente que notificacion es UNA sola bandeja para todos los tipos de documento notificable. No se separa por tipo de pieza. Filtros operativos: por tipo de pieza notificada, por canal, por estado, por plazo. Esto valida el modelo de `FalNotificacion` con `TipoNotif` (4 valores) en lugar de tablas separadas por tipo.

Plazos confirmados: **7 dias** para notificacion electronica; **5 dias** post fallo antes del siguiente destino natural. Ambos ya documentados en spec/dominio.

**Bandeja 3 Analisis/Pagos -- principal soporte de PagoVoluntario:**
Esta bandeja es el lugar natural de gestion de pagos voluntarios, presentaciones y descargos. La spec menciona explicitamente: solicitud de pago voluntario, plan de pagos, reingreso para nuevo analisis. El soporte en snapshot es via `SiPagoVolunt`, `FhPagoVolunt`, `MontoActa`, `SiPagoTotal`, `SiPlanPago`, `CantCuotasPlan`, `EstPagoAct`. No se define tabla propia de PagoVoluntario en spec de bandejas ni en spec de dominio.

**Bandeja 5 Pendientes fallo -- confirma Fallo como documento, no entidad:**
`05-bandeja-pendientes-fallo.md` usa "fallo" como la pieza a producir. La condicion de esta bandeja se determina por estado procesal del expediente. El fallo es un `TipoDocu=4` (ACTO_ADMINISTRATIVO). No se define tabla propia de Fallo.

**Bandeja 8 Con apelacion -- solo actuacion recursiva real:**
La spec es explicita: esta bandeja NO contiene expedientes que solo tienen plazo de apelacion habilitado sin actuacion. Solo existe cuando hay apelacion formalmente presentada. `FhVtoApelacion` en snapshot cubre el plazo; la apelacion real requiere `TipoEvt=13 (APELACION_INTERPUESTA)` y `BloqueActual=7`.

**Bandeja 9 Gestion externa -- reingreso obligatorio:**
`09-bandeja-gestion-externa.md` enfatiza que gestion externa no es salida sin retorno. El sistema debe contemplar reingreso cuando el resultado externo produzca efectos materiales. `SiReingresoGestionExt` en snapshot ya cubre este caso. Sin tabla propia de GestionExterna.

**Bandeja 10 Paralizadas -- gap de campo motivo:**
La spec exige que el snapshot muestre "cual es el motivo general de paralizacion". El snapshot actual (`FalActaSnapshot`) no tiene campo `MotivoParalizacion`. El DDL tampoco tiene tabla de paralizaciones. Este es un **gap critico** para la operacion de esta bandeja.

**Bandeja 11 Archivo -- gap de causales bloqueantes:**
La spec exige que el snapshot muestre: motivo de archivo, existencia de medidas activas, pendientes materiales, condicion que falta para cerrar. Los flags `SiNotifMedPrev*` son parciales. No existe campo estructurado de "motivo de archivo" ni de "causales bloqueantes" en `FalActaSnapshot`. **Gap critico.**

**LoteCorreo -- ausente de spec bandejas:**
`07-bandeja-notificaciones.md` menciona canal "correo / carta documento" como canal de notificacion, pero no define ninguna entidad `LoteCorreo` ni mecanismo de agrupacion de envios postales. Este agregado del prototipo sigue completamente ausente de toda fuente de spec.

---

## 4. Inventario spec/backend

**Nota importante:** La spec backend es conceptual/arquitectonica. No define nombres exactos de clases Java, rutas HTTP, DTOs concretos ni status codes HTTP. Define bloques de responsabilidad, reglas y entidades logicas.

### 4.1 Bloques de servicio propuestos

| Bloque (nombre spec) | Archivo | Responsabilidades principales | Entidades relacionadas |
|---|---|---|---|
| Gestion de expediente | `01-servicios-de-expediente.md` | Alta, recuperacion, consulta situacion, registro hechos, coordinacion cambios, reingreso, archivo, cierre | `Acta`, `ActaEvento`, `ActaSnapshot`, medidas, bandejas |
| Gestion documental | `02-servicios-documentales.md` | Produccion documental, numeracion, storage, estado, consulta | `Documento`, `ActaDocumento`, `StorageKey` |
| Gestion de firma | `03-servicios-de-firma.md` | Preparacion firma, registro resultado, impacto sobre documento | `Documento`, `DocumentoFirma`, `StorageKey` |
| Gestion de notificacion | `04-servicios-de-notificacion.md` | Inicio circuito, intentos, acuse, impacto operativo | `Notificacion`, `NotificacionIntento`, `NotificacionAcuse` |
| Gestion de snapshot | `05-servicios-de-snapshot.md` | Construir, refrescar, exponer, regenerar `ActaSnapshot` | `ActaSnapshot` |
| Gestion de bandejas | `07-servicios-de-bandejas.md` | Listar por bandeja, filtros, ordenamiento, indicadores | `ActaSnapshot`, `Acta` |
| Gestion externa | `06-servicios-de-gestion-externa.md` | Derivacion, resultado externo, reingreso | `Acta`, `ActaSnapshot` |
| Jobs, workers y procesos | `09-jobs-workers-y-procesos.md` | Reproyeccion snapshot, firma diferida, notificacion, numeracion, integraciones | Todas las entidades |

### 4.2 Operaciones conceptuales definidas (sin endpoint HTTP)

| Operacion conceptual | Bloque | Entidades involucradas |
|---|---|---|
| Crear expediente | Gestion de expediente | `Acta`, satelites, `ActaEvento`, `ActaSnapshot` |
| Obtener expediente resumido | Gestion de expediente | `Acta`, `ActaSnapshot` |
| Obtener expediente detallado | Gestion de expediente | `Acta`, `ActaEvento`, `Documento`, `Notificacion` |
| Consultar trazabilidad | Gestion de expediente | `ActaEvento` |
| Registrar hecho relevante | Gestion de expediente | `ActaEvento`, `ActaSnapshot` |
| Reencauzar expediente | Gestion de expediente | `Acta`, `ActaEvento` |
| Pasar a archivo / cerrada | Gestion de expediente | `Acta`, `ActaEvento`, `ActaSnapshot` |
| Reingresar expediente | Gestion de expediente | `Acta`, `ActaEvento`, `ActaSnapshot` |
| Generar documento logico | Gestion documental | `Documento`, `ActaDocumento` |
| Asociar storage | Gestion documental | `Documento`, `StorageObjeto` |
| Registrar firma | Gestion de firma | `DocumentoFirma`, `Documento` |
| Crear notificacion | Gestion de notificacion | `Notificacion`, `NotificacionIntento` |
| Registrar intento | Gestion de notificacion | `NotificacionIntento` |
| Registrar acuse | Gestion de notificacion | `NotificacionAcuse` |
| Construir / refrescar snapshot | Gestion de snapshot | `ActaSnapshot` |
| Listar bandeja | Gestion de bandejas | `ActaSnapshot` |
| Derivar a gestion externa | Gestion externa | `Acta`, `ActaSnapshot` |
| Registrar resultado externo | Gestion externa | `Acta`, `ActaEvento`, `ActaSnapshot` |
| Reingresar desde externa | Gestion externa | `Acta`, `ActaEvento` |

### 4.3 Reglas de implementacion (spec/04-backend/10-implementacion-base-spring-jdbc.md)

- Spring Boot moderno, code-first, sin XML.
- Persistencia: Spring JDBC, API preferida `JdbcClient`.
- SQL nativo explicito; **no ORM, no JPA/Hibernate**.
- Java Records para DTOs.
- SQL en archivos `.sql` bajo `resources/sql/...`.
- `@Transactional` en caso de uso/servicio de aplicacion.
- Capas: `web`, `application`, `domain`, `infrastructure/persistence/jdbc`, `infrastructure/config`.

**Nota MySQL:** La regla "SQL nativo orientado a Informix" debe interpretarse como "SQL nativo orientado a MySQL" dado el cambio de motor. La API `JdbcClient` es independiente del motor.

### 4.4 Tablas logicas del esquema relacional (segun spec/04-backend/08-persistencia-y-sql/)

| Grupo | Tablas logicas |
|---|---|
| Nucleo expediente | `Acta`, `ActaEvento`, `Observacion` |
| Satelites del acta | `ActaTransito`, `ActaTransitoAlcoholemia`, `ActaVehiculo`, `ActaContravencion`, `ActaSustanciasAlimenticias`, `ActaMedidaPreventiva`, `ActaEvidencia` |
| Referencias versionadas | `Dependencia`, `DependenciaVersion`, `Inspector`, `InspectorVersion`, `Alcoholimetro`, `AlcoholimetroVersion`, `RubroCom`, `RubroComVersion` |
| Documental | `Documento`, `ActaDocumento`, `DocumentoFirma`, `DocumentoObservacion` |
| Notificaciones | `Notificacion`, `NotificacionIntento`, `NotificacionAcuse`, `NotificacionObservacion` |
| Numeracion | `PoliticaNumeracion`, `Talonario`, `TalonarioDependencia`, `TalonarioInspector`, `TalonarioMovimiento` |
| Snapshot | `ActaSnapshot` |
| Storage | `StorageBackend`, `StoragePolitica`, `StorageObjeto` |
| Normativa | `NormativaFaltas`, `ArticuloNormativaFaltas`, `TarifarioUnidadFaltas`, `ActaArticuloInfringido`, `ActaArticuloAuditoria` |
| Catalogos | Tipos, estados, canales, resultados (sin tablas propias en DDL; como enums/constantes) |

---

## 5. Inventario de persistencia (spec/12-datos + spec/13-ddl + spec/14-sql-operativo)

### 5.1 Nota sobre la fuente

No existe un directorio `backend/persistencia/` independiente en el repositorio. La persistencia esta documentada en:
- `spec/12-datos/` -- modelo logico (entidades con campos logicos)
- `spec/13-ddl/` -- DDL fisico para Informix (tablas con tipos, PK, FK, indices)
- `spec/14-sql-operativo/` -- SQL operativo por caso de uso

**Prefijos fisicos usados:**
- `Fal` -- dominio de faltas (ej. `FalActa`, `FalDocumento`)
- `Num` -- numeracion y talonarios (ej. `NumPolitica`, `NumTalonario`)
- `Stor` -- storage documental (ej. `StorBackend`, `StorObjeto`)
- Sin prefijo -- compartidas existentes (ej. `RubroCom`, tablas geo_*)

**Nota:** En `spec/13-ddl/` las tablas aparecen sin prefijo en la definicion. En `spec/14-sql-operativo/` se usan los prefijos fisicos. El mapeo es: `Acta` -> `FalActa`, `Documento` -> `FalDocumento`, etc.

### 5.2 Tablas definidas en spec/13-ddl -- inventario completo

#### GRUPO: Nucleo expediente (`02-tablas-nucleo-expediente.md`)

**Tabla: `Acta` (fisica: `FalActa`)**
- Proposito: Cabecera principal del expediente/acta
- PK: `Id INT8`
- Columnas clave: `IdTecnico CHAR(36)`, `NroActa VARCHAR(20)`, `TipoActa SMALLINT`, `OrigenCaptura SMALLINT`, `FhActa DATETIME YEAR TO SECOND`, `IdDep INT`, `VerDep SMALLINT`, `IdInsp INT`, `VerInsp SMALLINT`
- Datos del infractor: `NomInfct VARCHAR(64)`, `DocPrefInfct SMALLINT`, `DocNroInfct INT`, `TipoPersInfct SMALLINT`
- Domicilio infractor normalizado: referencias a tablas geo + `SiCalleTxtInfct`, `CalleTxtInfct`, `SiNormParcialInfct`, `AltInfct`, `PisoInfct`, `DeptoInfct`, `ObsDomInfct`, `CodPosInfct`
- Domicilio infraccion normalizado: referencias geo + `SiDomTxtInfr`, `DomTxtInfr`, `SiEjeUrb`
- Licencia: `IdProvLicEmi`, `IdMuniLicEmi`, `IdDptoLicEmi`, `TipoJurLicEmi`
- GPS: `LatInfr DECIMAL(12,8)`, `LonInfr DECIMAL(12,8)`
- Auditoria: `ObsActa LVARCHAR`, `FhAlta DATETIME YEAR TO SECOND`, `IdUserAlta CHAR(36)`
- Total columnas: ~64

**Tabla: `ActaEvidencia` (fisica: `FalActaEvidencia`)**
- Proposito: Evidencias adjuntas al acta digital (sin binario en BD)
- PK: `Id INT8`, FK: `IdActa INT8`
- Columnas: `TipoEvid SMALLINT`, `StorageKey VARCHAR(255)`, `NomArchivo VARCHAR(120)`, `MimeType VARCHAR(80)`, `HashEvid VARCHAR(128)`, `ObsEvid VARCHAR(255)`, `OrdenEvid SMALLINT`, `FhCaptura DATETIME YEAR TO SECOND`, `FhAlta`, `IdUserAlta`

**Tabla: `ActaEvento` (fisica: `FalActaEvento`)**
- Proposito: Historial append-only de eventos del acta
- PK: `Id INT8`, FK: `IdActa INT8`
- Columnas: `FhEvt DATETIME YEAR TO SECOND`, `TipoEvt SMALLINT`, `OrigenEvt SMALLINT`, `BloqueFunc SMALLINT`, `EstProcAnt SMALLINT`, `EstProcNvo SMALLINT`, `SitAdmAnt SMALLINT`, `SitAdmNva SMALLINT`, `IdDocuRel INT8`, `IdNotifRel INT8`, `IdPresRel INT8`, `IdUserEvt CHAR(36)`, `SiEvtCierre SMALLINT`, `SiEvtExt SMALLINT`, `SiPermiteReing SMALLINT`
- Restriccion semantica: append-only

**Tabla: `Observacion` (fisica: `FalObservacion`)**
- Proposito: Observaciones transversales polimorficas
- PK: `Id INT8`
- Columnas: `IdRef SMALLINT`, `IdFk INT8`, `Obs VARCHAR(255)`, `FhAlta DATETIME YEAR TO SECOND`, `IdUser CHAR(36)`

#### GRUPO: Referenciales y versionado (`03-tablas-referenciales-y-versionado.md`)

**Tabla: `Dependencia` (fisica: `FalDependencia`)**
- PK: `IdDep INT`
- Columnas: `CodDep VARCHAR(20)`, `NomDep VARCHAR(120)`, `IdDepPadre INT`, `SiActiva SMALLINT`

**Tabla: `DependenciaVersion` (fisica: `FalDependenciaVersion`)**
- PK compuesta implicita: `(IdDep, VerDep)`
- Columnas: `NomDep VARCHAR(120)`, `IdDepPadre INT`, `VerDepPadre SMALLINT`, `FhVigDesde DATETIME YEAR TO DAY`, `FhVigHasta DATETIME YEAR TO DAY`, `SiActiva SMALLINT`

**Tabla: `Inspector` (fisica: `FalInspector`)**
- PK: `IdInsp INT`
- Columnas: `IdUser INT`, `LegajoInsp INT`, `NomInsp VARCHAR(120)`, `SiActivo SMALLINT`

**Tabla: `InspectorVersion` (fisica: `FalInspectorVersion`)**
- PK compuesta implicita: `(IdInsp, VerInsp)`
- Columnas: `LegajoInsp INT`, `NomInsp VARCHAR(120)`, `IdDep INT`, `VerDep SMALLINT`, `FhVigDesde DATETIME YEAR TO DAY`, `FhVigHasta DATETIME YEAR TO DAY`, `SiActivo SMALLINT`

**Tabla: `MedidaPreventiva` (fisica: `FalMedidaPreventiva`)**
- PK compuesta implicita: `(IdMedPrev, VerMedPrev)`
- Columnas: `NomMedPrev VARCHAR(120)`, `DescMedPrev VARCHAR(255)`, `SiActiva SMALLINT`, `IdDep INT`, `VerDep SMALLINT`

#### GRUPO: Acta satelites (`04-tablas-acta-y-satelites.md`)

**Tabla: `ActaTransito`** -- PK/FK `IdActa`; campos: `NroLic CHAR(8)`, referencias geograficas de licencia, `SiRetLic`, `SiRetVeh`, `SiCtrlAlcoh`, referencia alcoholimetro, resultado alcoholemia

**Tabla: `ActaTransitoAlcoholemia`** -- PK `Id INT8`, FK `IdActa`; campos: `OrdenMed`, `TipoPrueba`, `ResCuali`, `ResNum DECIMAL`, `UniMed`, `SiResFin` (solo una fila con `SiResFin=1`)

**Tabla: `ActaVehiculo`** -- PK/FK `IdActa`; campos: `DomVeh`, `TipoVeh`, `TipoVehTxt`, `MarcaVeh`, `ModeloVeh`

**Tabla: `ActaContravencion`** -- PK/FK `IdActa`; campos: `IdSuj`, `IdBie`, `Circ`, `Secc`, `Frac`, `Mza`, `Parc`, `UFun`, `UComp`, `OrigenNomencl`, `IdRub`, `VerRub`, `AmbitoCtv`, `AmbitoCtvTxt`

**Tabla: `ActaSustanciasAlimenticias`** -- PK/FK `IdActa`; campos: `IdRub`, `VerRub`, `AmbitoAct`, `AmbitoActTxt`

**Tabla: `ActaMedidaPreventiva`** -- PK `Id INT8`, FK `IdActa`; campos: `IdMedPrev`, `VerMedPrev`, `MedPrevTxt`, `FhAlta`, `IdUserAlta`

**Tabla: `NormativaFaltas`** -- PK compuesta `(IdNorma VARCHAR(8), VerNorma)`; campos: `NomNorma`, `SiTransito`, `SiActiva`

**Tabla: `ArticuloNormativaFaltas`** -- PK compuesta `(IdNorma, VerNorma, IdArtNorma, VerArtNorma)`; campos: `NomArtNorma`, `UniMedBase`, `ValorBase`, `SiActiva`

**Tabla: `TarifarioUnidadFaltas`** -- PK `IdTarifario INT`; campos: `UniMed`, `Valor DECIMAL`, `FhVigDesde`, `FhVigHasta`, `SiActiva`

**Tabla: `ActaArticuloInfringido`** -- PK `Id INT8`; referencia articulo normativa + `ValorBase`, `ValorApl`, `SiActiva`, `SiEditManual`

**Tabla: `ActaArticuloAuditoria`** -- PK `Id INT8`; campos: `IdActaArt`, `FhEvt`, `IdUserEvt`, `TipoAccion`, `CampoMod`, `ValorAnt VARCHAR(255)`, `ValorNvo VARCHAR(255)`, `Motivo`

#### GRUPO: Equipos y catalogos operativos (`06-tablas-equipos-y-catalogos-operativos.md`)

**Tabla: `Alcoholimetro` / `AlcoholimetroVersion`** -- Equipos versionados con `IdAlcoholimetro`, `VerAlcoholimetro`, series, vigencias, `SiDeshabilitado`

**Tabla: `RubroCom` / `RubroComVersion`** -- Rubros comerciales versionados con `IdRub CHAR(5)`, `VerRub SMALLINT`, `SiActivo`

#### GRUPO: Talonarios y numeracion (`05-tablas-talonarios-y-numeracion.md`)

**Tabla: `PoliticaNumeracion` (fisica: `NumPolitica`)** -- PK `IdPolNum`; campos: `NomPolNum`, `TipoTalonario`, `SiUsaPrefijo`, `Prefijo`, `SiUsaAnio`, `SiUsaSerie`, `LongNro`, separadores, `ProxNro`, `SiActiva`, vigencias

**Tabla: `Talonario` (fisica: `NumTalonario`)** -- PK `IdTalonario`; campos: `IdPolNum`, `CodTalonario`, `TipoTalonario`, `AmbitoTalonario`, `Serie`, `NroDesde`, `NroHasta`, `UltNroUsado`, `SiBloqueado`, `CodDesbloqueo`, `SiActiva`, `ObsTalonario`, `FhAlta`, `IdUserAlta`

**Tabla: `TalonarioDependencia` (fisica: `NumTalonarioDependencia`)** -- PK compuesta implicita; campos: `IdTalonario`, `IdDep`, `VerDep`, `FhDesde`, `FhHasta`, `SiActiva`

**Tabla: `TalonarioInsp` (fisica: `NumTalonarioInspector`)** -- campos: `IdTalonario`, `IdInsp`, `VerInsp`, `FhDesde`, `FhHasta`, `SiActiva`

**Tabla: `TalonarioMovimiento` (fisica: `NumTalonarioMovimiento`)** -- PK `Id INT8`; campos: `IdTalonario`, `NroUsado`, `EstadoNro`, `MotivoMov`, `IdActa`, `IdDep`, `VerDep`, `IdInsp`, `VerInsp`, `FhMov`, `IdUserMov`

#### GRUPO: Documental (`07-tablas-documentales.md`)

**Tabla: `Documento` (fisica: `FalDocumento`)** -- PK `IdDocu INT8`; campos: `TipoDocu SMALLINT`, `EstadoDocu SMALLINT`, `NroDocu VARCHAR(30)`, `IdTalonario INT`, `TipoFirmaReq SMALLINT`, `StorageKey VARCHAR(255)`, `HashDocu VARCHAR(128)`, `FhGeneracion DATETIME YEAR TO SECOND`, `FhAlta`, `IdUserAlta`

**Tabla: `ActaDocumento` (fisica: `FalActaDocumento`)** -- PK compuesta implicita `(IdActa, IdDocu)`; campos: `RolDocuActa SMALLINT`, `SiPrincipal SMALLINT`, `FhAlta`, `IdUserAlta`

**Tabla: `DocumentoFirma` (fisica: `FalDocumentoFirma`)** -- PK `Id INT8`; campos: `IdDocu INT8`, `TipoFirma SMALLINT`, `EstadoFirma SMALLINT`, `IdUserFirma CHAR(36)`, `FhSolicitud`, `FhFirma`, `FhAlta`, `IdUserAlta`

**Tabla: `DocumentoObservacion` (fisica: `FalDocumentoObservacion`)** -- PK `Id INT8`; campos: `IdDocu INT8`, `ObsDocu VARCHAR(255)`, `FhAlta`, `IdUserAlta`

#### GRUPO: Notificaciones (`08-tablas-notificacion.md`)

**Tabla: `Notificacion` (fisica: `FalNotificacion`)** -- PK `IdNotif INT8`; campos: `TipoNotif SMALLINT`, `EstadoNotif SMALLINT`, `IdDocu INT8`, `IdActa INT8`, `FhGeneracion`, `FhEmision`, `CanalNotif SMALLINT`, `SiRequiereAcuse SMALLINT`, `SiAcuseRecibido SMALLINT`, `EstadoAcuse SMALLINT`, `FhAcuse`, `FhAlta`, `IdUserAlta`

**Tabla: `NotificacionIntento` (fisica: `FalNotificacionIntento`)** -- PK `Id INT8`; campos: `IdNotif INT8`, `NroIntento SMALLINT`, `CanalNotif SMALLINT`, `TipoDestNotif SMALLINT`, `DestNotif VARCHAR(255)`, `EstadoIntento SMALLINT`, `FhIntento`, `FhResultado`, `ResultadoIntento SMALLINT`, `FhAlta`, `IdUserAlta`

**Tabla: `NotificacionAcuse` (fisica: `FalNotificacionAcuse`)** -- PK `Id INT8`; campos: `IdNotif INT8`, `IdIntentoNotif INT8`, `TipoAcuse SMALLINT`, `EstadoAcuse SMALLINT`, `FhAcuse`, `StorageKeyAcuse VARCHAR(255)`, `FhAlta`, `IdUserAlta`

**Tabla: `NotificacionObservacion` (fisica: `FalNotificacionObservacion`)** -- PK `Id INT8`; campos: `IdNotif INT8`, `ObsNotif VARCHAR(255)`, `FhAlta`, `IdUserAlta`

#### GRUPO: Snapshot (`09-tablas-snapshot-auxiliares-y-proyecciones.md`)

**Tabla: `ActaSnapshot` (fisica: `FalActaSnapshot`)** -- PK/FK `IdActa INT8` (1:1 con `FalActa`)

Campos completos (43 columnas):
- Identificacion: `FhActa`, `IdDep`, `VerDep`, `IdInsp`, `VerInsp`
- Bandeja: `BloqueActual SMALLINT`, `EstProcAct SMALLINT`, `SitAdmAct SMALLINT`, `CodBandeja VARCHAR(50)`, `SiVisibleBandeja SMALLINT`, `Prioridad SMALLINT`
- Notificaciones: `SiNotifActa`, `SiNotifActaProc`, `SiNotifActaAcusePend`, `SiNotifMedPrev`, `SiNotifMedPrevProc`, `SiNotifMedPrevAcusePend`, `SiNotifFallo`, `SiNotifFalloProc`, `SiNotifFalloAcusePend`, `CantReintNotif SMALLINT`
- Pago: `SiPagoVolunt`, `FhPagoVolunt`, `MontoActa DECIMAL`, `SiPagoTotal`, `SiPlanPago`, `CantCuotasPlan SMALLINT`, `ValorCuotaPlan DECIMAL`, `CantCaidasPlan SMALLINT`
- Gestion externa: `SiGestionExt`, `TipoGestionExt SMALLINT`, `SiReingresoGestionExt`, `ResultadoGestionExt SMALLINT`
- Plazos: `FhVtoPresentacion`, `FhVtoApelacion`, `FhVtoApremio`
- Punteros: `IdEvtUlt INT8`, `IdDocuUlt INT8`, `IdNotifUlt INT8`
- Auditoria snapshot: `FhUltMod`, `IdUserUltMod CHAR(36)`, `FhSnapshot`

#### GRUPO: Storage documental (`10-tablas-storage-documental.md`)

**Tabla: `StorageBackend` (fisica: `StorBackend`)** -- PK `IdStorageBackend INT`; campos: `NomStorageBackend VARCHAR(60)`, `TipoBackend SMALLINT`, `BasePath VARCHAR(255)`, `BucketContenedor VARCHAR(120)`, `PrefijoRuta VARCHAR(80)`, `SiDefault SMALLINT`, `SiActivo SMALLINT`, `ObsBackend VARCHAR(255)`, `FhAlta`, `IdUserAlta`

**Tabla: `StoragePolitica` (fisica: `StorPolitica`)** -- PK `IdStoragePolitica INT`; campos: `Sistema VARCHAR(20)`, `Familia VARCHAR(30)`, `TipoObjeto VARCHAR(30)`, `IdStorageBackend INT`, `Prioridad SMALLINT`, `SiActiva SMALLINT`, `FhVigDesde`, `FhVigHasta`, `ObsPolitica VARCHAR(255)`

**Tabla: `StorageObjeto` (fisica: `StorObjeto`)** -- PK `StorageKey VARCHAR(64)`; campos: `IdStorageBackend INT`, `Sistema VARCHAR(20)`, `Familia VARCHAR(30)`, `TipoObjeto VARCHAR(30)`, `Anio SMALLINT`, `Mes SMALLINT`, `Bucket VARCHAR(60)`, `RefNegocio VARCHAR(80)`, `NomArchivo VARCHAR(120)`, `ExtArchivo VARCHAR(10)`, `MimeType VARCHAR(80)`, `TamBytes INT8`, `HashArchivo VARCHAR(128)`, `RutaRelativa VARCHAR(512)`, `EstadoStorage SMALLINT`, `FhAlta`, `IdUserAlta`

#### GRUPO: Tablas territoriales externas (sin prefijo `Fal`)

Las tablas `geo_ign_*`, `geo_indec_*`, `geo_bahra_*` son externas/compartidas, no gobernadas por el modulo de faltas. Calidad de datos heredada con posibles huerfonos e inconsistencias.

Las tablas `localidad`, `calle`, `barrio`, `geo_calle_alturas_barrio`, `manzana`, `callexmza` son tablas locales de Malvinas.

Las tablas `geo_gmat_*` usan **PostGIS** con geometrias `MultiPolygon`, `MultiLineString`, `Point` en proyeccion `EPSG:22195`.

### 5.3 Enumeraciones del DDL (valores exactos, spec/13-ddl)

#### BloqueActual (`ActaEvento.BloqueFunc`, `ActaSnapshot.BloqueActual`)
```
1 = D1_LABRADO
2 = D2_VALIDACION_ENRIQUECIMIENTO
3 = D3_NOTIFICACION_ACTA
4 = D4_ANALISIS_PRESENTACIONES_PAGOS
5 = D5_ACTO_ADMINISTRATIVO
6 = D6_NOTIFICACION_ACTO
7 = D7_APELACION
8 = D8_GESTION_EXTERNA
9 = D8_ARCHIVO_CIERRE
```

#### EstProcAct (`ActaEvento`, `ActaSnapshot`)
```
0 = BORRADOR           6 = NOTIFICACION_EN_PROCESO   12 = PENDIENTE_NOTIFICACION_ACTO
1 = LABRADA            7 = NOTIFICADA                 13 = NOTIFICACION_ACTO_EN_PROCESO
2 = EN_REVISION        8 = EN_ANALISIS                14 = APELADA
3 = VALIDADA           9 = PENDIENTE_ACTO             15 = EN_GESTION_EXTERNA
4 = OBSERVADA         10 = ACTO_EN_PROCESO            16 = ARCHIVADA
5 = LISTA_PARA_NOTIFICAR  11 = ACTO_FIRMADO           17 = CERRADA
```

#### SitAdmAct (`ActaEvento`, `ActaSnapshot`)
```
1 = PRE_ADMINISTRATIVA     5 = EN_GESTION_EXTERNA
2 = ADMINISTRATIVA_ACTIVA  6 = PARALIZADA
3 = EN_ANALISIS            7 = ARCHIVADA
4 = EN_APELACION           8 = CERRADA
```

#### TipoCierreAct
```
1 = ARCHIVO_ADMINISTRATIVO   4 = CIERRE_GESTION_EXTERNA
2 = CIERRE_PAGO              5 = CIERRE_OTRO
3 = CIERRE_RESOLUCION
```

#### EstPagoAct (`ActaSnapshot`)
```
1 = SIN_PAGO                 5 = PAGO_PARCIAL
2 = INTENCION_DE_PAGO        6 = PAGO_RECHAZADO
3 = PAGO_PENDIENTE_CONFIRMACION  7 = CONDONADO
4 = PAGO_CONFIRMADO          8 = NO_APLICA
```

#### EstDocAct (`ActaSnapshot`)
```
1 = SIN_DOCUMENTOS              5 = IMPRESO_PARA_FIRMA_OLOGRAFA
2 = DOCUMENTO_BORRADOR          6 = INCORPORADO_FIRMADO
3 = PENDIENTE_FIRMA             7 = ANULADO
4 = FIRMADO
```

#### EstNotifAct (`ActaSnapshot`)
```
1 = NO_GENERADA    4 = EN_PROCESO    7 = VENCIDA
2 = PENDIENTE_EMISION  5 = NOTIFICADA   8 = FALLIDA
3 = EMITIDA        6 = SIN_ACUSE    9 = NO_APLICA
```

#### TipoEvt (`ActaEvento.TipoEvt`) -- 19 valores (0--18)
```
0 = ACTA_CREADA_EN_BORRADOR         10 = PRESENTACION_REGISTRADA
1 = ACTA_LABRADA                    11 = PAGO_REGISTRADO
2 = BORRADOR_DESCARTADO             12 = ACTO_ADMINISTRATIVO_DICTADO
3 = ACTA_VALIDADA                   13 = APELACION_INTERPUESTA
4 = ACTA_OBSERVADA                  14 = DERIVACION_EXTERNA
5 = ACTA_ANULADA                    15 = ARCHIVO_DISPUESTO
6 = DOCUMENTO_GENERADO              16 = REINGRESO_DISPUESTO
7 = DOCUMENTO_FIRMADO               17 = MEDIDA_PREVENTIVA_APLICADA
8 = NOTIFICACION_EMITIDA            18 = MEDIDA_PREVENTIVA_LEVANTADA
9 = NOTIFICACION_CONFIRMADA
```

#### OrigenEvt (`ActaEvento.OrigenEvt`)
```
1 = USUARIO_INTERNO    4 = PROCESO_BATCH
2 = SISTEMA            5 = PORTAL_CIUDADANO
3 = INTEGRACION_EXTERNA
```

#### TipoDocu (`Documento.TipoDocu`)
```
1 = ACTA                    5 = NOTIFICACION_ACTO
2 = NOTIFICACION_ACTA       6 = CONSTANCIA
3 = MEDIDA_PREVENTIVA       7 = ANEXO
4 = ACTO_ADMINISTRATIVO     8 = OTRO
```

#### EstadoDocu (`Documento.EstadoDocu`)
```
1 = BORRADOR                    5 = IMPRESO_PARA_FIRMA_OLOGRAFA
2 = GENERADO                    6 = INCORPORADO_FIRMADO
3 = PENDIENTE_FIRMA             7 = ANULADO
4 = FIRMADO
```

#### RolDocuActa (`ActaDocumento.RolDocuActa`)
```
1 = DOCUMENTO_PRINCIPAL_ACTA         5 = DOCUMENTO_NOTIFICACION_ACTO
2 = DOCUMENTO_NOTIFICACION_ACTA      6 = DOCUMENTO_ANEXO
3 = DOCUMENTO_MEDIDA_PREVENTIVA      7 = DOCUMENTO_CONSTANCIA
4 = DOCUMENTO_ACTO_ADMINISTRATIVO
```

#### TipoNotif (`Notificacion.TipoNotif`)
```
1 = NOTIFICACION_ACTA                   3 = NOTIFICACION_MEDIDA_PREVENTIVA
2 = NOTIFICACION_ACTO_ADMINISTRATIVO    4 = OTRA_NOTIFICACION
```

#### EstadoNotif (`Notificacion.EstadoNotif`)
```
1 = NO_GENERADA    4 = EN_PROCESO    7 = VENCIDA
2 = PENDIENTE_EMISION  5 = NOTIFICADA   8 = FALLIDA
3 = EMITIDA        6 = SIN_ACUSE    9 = ANULADA
```

#### CanalNotif
```
1 = DOMICILIO_ELECTRONICO    5 = NOTIFICADOR_MUNICIPAL
2 = EMAIL                    6 = PORTAL_CIUDADANO
3 = POSTAL                   7 = OTRO
4 = BLUEMAIL
```

#### EstadoIntento (`NotificacionIntento.EstadoIntento`)
```
1 = GENERADO    4 = RECIBIDO    7 = FALLIDO
2 = ENVIADO     5 = RECHAZADO
3 = ENTREGADO   6 = SIN_RESULTADO
```

#### TipoAcuse / EstadoAcuse (`NotificacionAcuse`)
```
TipoAcuse:        EstadoAcuse:
1 = ACUSE_ELECTRONICO   1 = PENDIENTE
2 = ACUSE_POSTAL        2 = RECIBIDO
3 = ACUSE_BLUEMAIL      3 = RECHAZADO
4 = ACUSE_NOTIFICADOR   4 = VENCIDO
5 = CONSTANCIA_PORTAL   5 = INVALIDADO
6 = OTRO
```

#### TipoTalonario / EstadoNro
```
TipoTalonario:              EstadoNro:
ELECTRONICO                 USADO
MANUAL_FISICO               ANULADO
```

#### TipoGestionExt / ResultadoGestionExt (`ActaSnapshot`)
Mencionados como campos; valores exactos no definidos explicitamente en `spec/13-ddl/09-...` -- pendiente de cruce con enums del prototipo.

### 5.4 Secuencias definidas en spec/12-datos/00-convencion-nombres-fisicos.md

```
SeqFalActa     SeqFalActEv    SeqFalDoc      SeqFalNotif
SeqNumPol      SeqNumTal      SeqNumTalMov
SeqStorBack    SeqStorPol     SeqStorObj
SeqRubComVer
```

**Nota MySQL:** Las secuencias de Informix son el mecanismo de generacion de PKs. En MySQL se reemplazaran por `AUTO_INCREMENT` (en columnas individuales) o por la API de secuencias de MySQL 8.0+.

### 5.5 Patrones SQL operativos (spec/14-sql-operativo)

| Patron | Nombre | Tablas ejemplo |
|---|---|---|
| 1 | Alta simple | `FalObservacion` |
| 2 | Alta agregado padre/hijos | `FalActa`+satelites, `FalDocumento`+`FalActaDocumento`, `FalNotificacion`+intento |
| 3 | Asociacion entre existentes | `FalActaDocumento`, `StorObjeto`->`FalDocumento` |
| 4 | Correccion controlada | UPDATE permitido en mutables |
| 5 | Versionado cierre+nueva version | `FalDependenciaVersion`, `FalInspectorVersion`, `FalAlcoholimetroVersion`, `RubroComVersion` |
| 6 | Insercion con idempotencia | Verificar clave unica antes de insertar (sync movil) |
| 7 | Operacion con parte diferida | Transaccion principal + tarea externa (firma, storage, numeracion) |

**Pasos comunes en toda transaccion de escritura:**
- A. Registrar `FalActaEvento` (append-only)
- B. Actualizar/insertar `FalActaSnapshot`
- C. Registrar `FalObservacion` (opcional)

### 5.6 SQL operativo -- lookups de formularios (03a, 03b, 03c)

| Archivo | Proposito | Tablas principales | Acoplamiento Informix | Portable MySQL |
|---|---|---|---|---|
| `03a-sql-lookups-domicilio-infractor.md` | Lookups geograficos para domicilio del infractor: Malvinas (local) y exterior (nacional) | `localidad`, `calle`, `geo_calle_alturas_barrio`, `barrio`, `manzana`, `callexmza` (Malvinas); `geo_ign_provincia`, `geo_ign_municipio`, `geo_ign_departamento`, `geo_indec_localidad`, `geo_indec_localidad_censal`, `geo_indec_calles`, `geo_bahra_asentamiento` (exterior) | Ninguno detectado -- SQL conceptual | Si -- directo |
| `03b-sql-lookups-licencia-y-jurisdiccion.md` | Lookups para municipio emisor de licencia de conducir (municipio real o departamento fallback) | `geo_ign_provincia`, `geo_ign_municipio`, `geo_ign_departamento` | Ninguno detectado | Si -- directo |
| `03c-sql-lookups-catalogos-y-validaciones.md` | Lookups UX de catalogos operativos propios de faltas; validaciones rapidas (unicidad, disponibilidad, vigencia) | `FalDependenciaVersion`, `FalInspectorVersion`, `FalAlcoholimetroVersion`, `FalMedidaPreventiva`, `RubroCom`, `RubroComVersion`, `NumPolitica`, `NumTalonario`, `NumTalonarioDependencia`, `NumTalonarioInspector`, `StorBackend`, `StorPolitica`, `FalNormativaFaltas`, `FalArticuloNormativaFaltas`, `FalTarifarioUnidadFaltas` | Ninguno detectado | Si -- directo |

**Observacion clave (03a):** La persistencia del domicilio del infractor admite normalizacion parcial explicita. Modo Malvinas: calle local + barrio resuelto + indicador `SiNormParcialInfct`. Modo exterior: `provincia_id`, `municipio_id` (nullable), `departamento_id`, `localidad_id`, `localidad_censal_id`, `calle_id` o calle textual libre. Todo ya modelado en columnas de `FalActa`.

**Observacion clave (03b):** El municipio emisor de licencia puede ser municipio real o departamento fallback. La persistencia distingue el tipo de jurisdiccion. Ya modelado en `FalActaTransito` via columnas de referencias geograficas de licencia.

**Observacion clave (03c):** Las validaciones rapidas de UX (existencia de `NroActa`, unicidad de `IdTecnico`, disponibilidad de talonario, vigencia de version referencial) requieren indices en columnas clave. Sin SQL Informix-especifico; todos los lookups son portables.

### 5.7 SQL operativo -- referenciales y transversales (07a, 07b, 07c, 07d)

| Archivo | Proposito | Tablas principales | Patron transaccional | Acoplamiento Informix | Portable MySQL |
|---|---|---|---|---|---|
| `07a-sql-referenciales-versionados.md` | CRUD de dependencias, inspectores, alcoholimetros (raiz + version versionada) | `FalDependencia`, `FalDependenciaVersion`, `FalInspector`, `FalInspectorVersion`, `FalAlcoholimetro`, `FalAlcoholimetroVersion`, `FalMedidaPreventiva` | Alta raiz+version; cierre version+nueva version; lectura vigente; lectura historica | Secuencias (`NEXT VALUE FOR Seq*`) -- mismo acoplamiento que en otros patrones | Si -- con `AUTO_INCREMENT` |
| `07b-sql-rubros-compartidos.md` | Lectura y versionado de rubros comerciales compartidos. Sin control total del CRUD (rubros son entidad compartida con sistema de ingresos) | `RubroCom`, `RubroComVersion` | Lectura vigente; lectura historica por fecha o id; alta/nueva version (solo si autorizado) | Ninguno detectado | Si -- directo |
| `07c-sql-numeracion-transversal.md` | CRUD de politicas de numeracion, talonarios, asignaciones a dependencias/inspectores, movimientos, consulta de disponibilidad | `NumPolitica`, `NumTalonario`, `NumTalonarioDependencia`, `NumTalonarioInspector`, `NumTalonarioMovimiento` | Alta simple; asociacion entre entidades; registro de movimiento; consulta de disponibilidad y proximo numero logico | Secuencias (`NEXT VALUE FOR Seq*`) | Si -- con `AUTO_INCREMENT` |
| `07d-sql-storage-transversal.md` | CRUD de backends, politicas de storage, objetos fisicos. Resolucion de politica efectiva. Lookup por `StorageKey` | `StorBackend`, `StorPolitica`, `StorObjeto` | Alta simple; resolucion de politica efectiva por sistema/familia/tipo; lookup por `StorageKey` | Ninguno detectado -- `StorageKey` es PK VARCHAR, sin secuencia numerica | Si -- directo |

**Observacion critica (07a/07c):** El patron de secuencias Informix (`NEXT VALUE FOR SeqXxx`) es el UNICO acoplamiento detectado en estos archivos. Es acoplamiento tecnico de DDL, no logico. En MySQL: reemplazar con `BIGINT AUTO_INCREMENT` por tabla o con la API de secuencias MySQL 8.0+. El patron logico (alta raiz + alta version, cierre de vigencia, lectura de vigente, lectura historica) es completamente portable sin cambios conceptuales.

**Observacion (07d):** `StorObjeto` tiene `StorageKey VARCHAR(64)` como PK natural (no numerica). No usa secuencia. Portable directamente a MySQL sin cambios en el mecanismo de PK.

**Observacion (07b):** `RubroCom` es compartida con el sistema de ingresos. Faltas debe consumirla como dependencia; no tiene control total del CRUD. Relacion similar a las tablas territoriales externas.

### 5.8 SQL operativo -- proyecciones y reproceso (08)

| Archivo | Proposito | Tabla principal | Tipos de operacion | Fuentes de verdad para reproceso | Acoplamiento Informix | Portable MySQL |
|---|---|---|---|---|---|---|
| `08-sql-proyecciones-y-reproceso.md` | Mantenimiento, actualizacion incremental y reproceso completo de `FalActaSnapshot` | `FalActaSnapshot` | Inicializacion al crear acta; actualizacion incremental en linea (por evento relevante); reproceso completo (consistencia global) | `FalActa`, `FalActaEvento`, `FalDocumento`, `FalDocumentoFirma`, `FalNotificacion`, satelites relevantes | Ninguno detectado | Si -- directo |

**Hallazgo critico (08):** La spec confirma explicitamente que `FalActaSnapshot` es **regenerable** desde las fuentes de verdad. El reproceso completo puede recomponer el snapshot leyendo las tablas base. Regla de idempotencia: reprocesar una misma acta multiples veces no produce duplicacion ni degradacion del estado proyectado. Esto confirma formalmente: snapshot = proyeccion derivada, no fuente primaria.

**Criterio de actualizacion incremental (08):** Solo cuando el cambio impacta directamente la operacion inmediata y puede resolverse sin releer todo el expediente. Casos tipicos: alta inicial de acta; nuevo evento con impacto en bloque o estado; cambio documental que afecte flags visibles; apertura o cierre de notificacion; cambios en pendientes materiales.

---

## 6. Mapeo preliminar contra agregados candidatos del prototipo

| Agregado candidato (prototipo) | En spec/dominio | En spec/backend | En spec/13-ddl (tablas) | Nombres exactos encontrados | Archivos clave | Requiere cruce detallado |
|---|---|---|---|---|---|---|
| **Acta / Expediente de Faltas** | Si (01-acta.md) | Si (01-servicios-expediente) | Si (`FalActa`, `FalActaEvento`, `FalActaEvidencia`) | `Acta`, `ActaEvento`, `Expediente` | 01-acta, 02-tablas-nucleo, 04-sql-crud-acta | Si -- divergencia de estados |
| **Documento / Pieza** | Si (03-documento.md) | Si (02-servicios-documentales) | Si (`FalDocumento`, `FalActaDocumento`, `FalDocumentoFirma`) | `Documento`, `ActaDocumento`, `DocumentoFirma` | 03-documento, 07-tablas-documentales | Si -- estados y roles |
| **Notificacion** | Si (04-notificacion.md) | Si (04-servicios-notificacion) | Si (`FalNotificacion`, `FalNotificacionIntento`, `FalNotificacionAcuse`) | `Notificacion`, `NotificacionIntento`, `NotificacionAcuse` | 04-notificacion, 08-tablas-notificacion | Si -- estados divergen |
| **PagoVoluntario** | Parcial (evento `pago` en catalogos) | No (sin bloque dedicado) | No (solo flags en `FalActaSnapshot`) | `EstPagoAct`, `SiPagoVolunt`, `MontoActa`, `SiPlanPago`, `CantCuotasPlan` | 07-catalogos, 09-snapshot, 03-bandeja-analisis-presentaciones-pagos | Si -- bandeja D3 confirma lugar operativo; sin tabla propia en ningun nivel de spec |
| **PagoCondena** | Parcial (evento `pago` en catalogos) | No (sin bloque dedicado) | No (solo flags en `FalActaSnapshot`) | `SiPagoTotal`, `EstPagoAct` | 07-catalogos, 09-snapshot, 03-bandeja-analisis-presentaciones-pagos | Si -- igual que PagoVoluntario; sin tabla propia en ningun nivel de spec |
| **Fallo** | Parcial (`TipoDocu = ACTO_ADMINISTRATIVO`) | No (sin bloque dedicado) | Parcial (`TipoDocu=4`, `TipoEvt=12`) | `ACTO_ADMINISTRATIVO_DICTADO`, `TipoDocu.ACTO_ADMINISTRATIVO` | 07-tablas-documentales, 02-tablas-nucleo, 05-bandeja-pendientes-fallo | Si -- bandeja D5 usa "fallo" como pieza documental (`TipoDocu=4`); sin entidad separada en spec |
| **Apelacion** | Parcial (`TipoEvt=APELACION_INTERPUESTA`) | No (sin bloque dedicado) | Parcial (`TipoEvt=13`, bloque `D7_APELACION`) | `APELACION_INTERPUESTA`, `D7_APELACION`, `BloqueActual=7`, `FhVtoApelacion` | 02-tablas-nucleo, 08-bandeja-con-apelacion | Si -- bandeja D8 solo con apelacion real; `FhVtoApelacion` cubre plazo; sin tabla propia |
| **BloqueanteCierreMaterial** | Si (06-medidas, 07-pendientes) | No (sin bloque dedicado) | Parcial (`ActaMedidaPreventiva`, concepto en snapshot) | `ActaMedidaPreventiva`, `FalMedidaPreventiva`, `SiRetVeh`, `SiRetLic` | 03-referenciales, 04-satelites, 11-bandeja-archivo, 12-bandeja-cerradas | Si -- bandejas D11/D12 exigen ver causales bloqueantes en snapshot; gap critico: sin campo estructurado de causales en `FalActaSnapshot` |
| **GestionExterna** | Si (06-servicios-gestion-externa) | Si (06-servicios-gestion-externa) | No (solo flags en `FalActaSnapshot`) | `SiGestionExt`, `TipoGestionExt`, `ResultadoGestionExt`, `SiReingresoGestionExt` | 06-servicios-gestion-externa, 09-snapshot, 09-bandeja-gestion-externa | Si -- bandeja D9 enfatiza reingreso obligatorio; `SiReingresoGestionExt` ya existe en snapshot; sin tabla propia |
| **Paralizacion** | Si (como estado `PARALIZADA` en SitAdmAct) | No (sin bloque dedicado) | No (solo valor en `SitAdmAct=6`) | `PARALIZADA`, `SitAdmAct.PARALIZADA` | 02-tablas-nucleo, 07-catalogos, 10-bandeja-paralizadas | Si -- bandeja D10 exige campo motivo de paralizacion en snapshot; gap critico: sin `MotivoParalizacion` en DDL ni en `FalActaSnapshot` |
| **LoteCorreo** | No | No | No | -- | -- (canal postal en D7 pero sin entidad LoteCorreo en spec de bandejas) | Si -- completamente ausente de toda fuente de spec incluyendo bandejas |
| **EventoAuditoria** | Si (`ActaEvento`) | No (solo como parte de expediente) | Si (`FalActaEvento`) | `ActaEvento`, `TipoEvt` (19 valores) | 02-tablas-nucleo | Si -- 62 eventos prototipo vs 19 en DDL |

---

## 7. Mapeo preliminar de estados/enums

| Enum en prototipo | Nombre DDL equivalente | Tabla DDL | Valores DDL encontrados | Valores prototipo | Coincidencia | Archivos |
|---|---|---|---|---|---|---|
| `bloqueActual` | `BloqueActual` | `FalActaEvento`, `FalActaSnapshot` | D1_LABRADO ... D8_ARCHIVO_CIERRE (9 valores) | D1_CAPTURA, D2_ENRIQUECIMIENTO, D4_NOTIFICACION, D5_ANALISIS, GESTION_EXTERNA | Parcial -- nombres distintos | 02-tablas-nucleo |
| `estadoProcesoActual` | `EstProcAct` | `FalActaEvento`, `FalActaSnapshot` | BORRADOR ... CERRADA (18 valores 0--17) | PENDIENTE_REVISION, PENDIENTE_FIRMA, PENDIENTE_ENVIO, EN_GESTION_EXTERNA (pocos listados) | Parcial -- DDL mas granular | 02-tablas-nucleo |
| `situacionAdministrativaActual` | `SitAdmAct` | `FalActaEvento`, `FalActaSnapshot` | PRE_ADMINISTRATIVA ... CERRADA (8 valores) | ACTIVA, PARALIZADA (pocos listados) | Parcial | 02-tablas-nucleo |
| `resultadoFinal` | No hay campo directo | `FalActaSnapshot` (calculado) | `TipoCierreAct` (1--5) parcialmente relacionado | SIN_RESULTADO_FINAL, ABSUELTO, PAGO_CONFIRMADO, CONDENADO, CONDENA_FIRME | No directo -- requiere cruce | 09-snapshot |
| `situacionPago` | `EstPagoAct` | `FalActaSnapshot` | SIN_PAGO ... NO_APLICA (8 valores) | SIN_PAGO, SOLICITADO, PAGO_INFORMADO, PENDIENTE_CONFIRMACION, CONFIRMADO, OBSERVADO, VENCIDO | Parcial -- valores distintos | 09-snapshot |
| `situacionPagoCondena` | No hay campo directo | `FalActaSnapshot` (parcial) | Solo flag `SiPagoTotal` | NO_APLICA, PENDIENTE, INFORMADO, CONFIRMADO, OBSERVADO | No directo | 09-snapshot |
| `tipoPago` | No hay campo directo | No existe en DDL | -- | NO_APLICA, VOLUNTARIO, CONDENA | Ausente en DDL | -- |
| `estadoDocumento` | `EstadoDocu` | `FalDocumento` | BORRADOR ... ANULADO (7 valores) | EMITIDO, PENDIENTE_FIRMA, FIRMADO, ADJUNTO | Parcial -- prototipo simplificado | 07-tablas-documentales |
| `estadoNotificacion` | `EstadoNotif` | `FalNotificacion` | NO_GENERADA ... ANULADA (9 valores) | PENDIENTE_PREPARACION, LISTA_PARA_ENVIO, ENVIADA, ENTREGADA, NEGATIVA, VENCIDA, SIN_EFECTO | Parcial -- distintos | 08-tablas-notificacion |
| `resultadoNotificacion` | `ResultadoIntento` / `EstadoAcuse` | `FalNotificacionIntento`, `FalNotificacionAcuse` | EstadoIntento (7 val), EstadoAcuse (5 val) | SIN_RESULTADO, POSITIVA, NEGATIVA, VENCIDA, SUPERADA_POR_PORTAL | Parcial -- distribuido en 2 tablas | 08-tablas-notificacion |
| `motivoArchivo` | `TipoCierreAct` parcial | `FalActaSnapshot` | ARCHIVO_ADMINISTRATIVO, CIERRE_PAGO, CIERRE_RESOLUCION, CIERRE_GESTION_EXTERNA, CIERRE_OTRO | ARCHIVO_DESDE_ANALISIS_DIRECTO, POST_EVALUACION_VENCIMIENTO, NULIDAD | No coincide -- distintos | 02-tablas-nucleo |
| `motivoParalizacion` | No hay campo en DDL | -- | -- | ESPERA_DOCUMENTAL, ESPERA_INFORME_EXTERNO, ESPERA_OTRA_DEPENDENCIA, ESPERA_RESOLUCION_RELACIONADA, OTRO | Ausente en DDL | -- |
| `origenBloqueanteMaterial` | No hay campo en DDL | -- | -- | MEDIDA_PREVENTIVA_ACTIVA, RODADO_SECUESTRADO, DOCUMENTACION_RETENIDA | Ausente en DDL | -- |
| `tipoGestionExterna` | `TipoGestionExt` | `FalActaSnapshot` | Valores no definidos en spec | APREMIO, JUZGADO_DE_PAZ | Pendiente -- valores DDL sin definir | 09-snapshot |
| `resultadoExternoPostGestion` | `ResultadoGestionExt` | `FalActaSnapshot` | Valores no definidos en spec | MODIFICA_MONTO, ABSUELVE | Pendiente -- valores DDL sin definir | 09-snapshot |

---

## 8. Mapeo preliminar de eventos

### 8.1 Modelo de evento en DDL

Existe el modelo de evento en spec/13-ddl. Tabla: `ActaEvento` (fisica: `FalActaEvento`), append-only.

El campo `TipoEvt SMALLINT` almacena el tipo. 19 valores definidos (0--18, ver seccion 4.3).

No existe catalogo de eventos como tabla separada; los tipos son un enum fisico controlado.

### 8.2 Eventos del prototipo vs eventos DDL

| Grupo | Eventos prototipo (62 total) | Cobertura en TipoEvt DDL (19 valores) |
|---|---|---|
| Ciclo del expediente (10) | `ACTA_LABRADA`, `ACTA_ENRIQUECIDA`, `ACTA_ENVIADA_A_NOTIFICACION`, `ACTA_CERRADA`, `ACTA_ARCHIVADA`, `ACTA_ARCHIVADA_POR_VENCIMIENTO`, `ACTA_REINGRESADA`, `ACTA_PARALIZADA`, `ACTA_REACTIVADA`, `ACTA_NULIDAD_REGISTRADA` | Parcial: `ACTA_LABRADA`(1), `ARCHIVO_DISPUESTO`(15), `REINGRESO_DISPUESTO`(16). El resto sin equivalente directo. |
| Documentos y firma (3) | `DOCUMENTO_GENERADO`, `DOCUMENTO_FIRMADO`, `FIRMA_CERRADA` | `DOCUMENTO_GENERADO`(6), `DOCUMENTO_FIRMADO`(7). `FIRMA_CERRADA` ausente. |
| Notificaciones (11) | `NOTIFICACION_PREPARADA`, `NOTIFICACION_POSITIVA`, `NOTIFICACION_NEGATIVA`, `NOTIFICACION_VENCIDA`, `REINTENTO_*`, `NOTIFICACION_PORTAL_CONFIRMADA`, `NOTIFICACION_SUPERADA_POR_PORTAL`, `ACUSE_NOTIFICADOR_MUNICIPAL`, `LOTE_CORREO_GENERADO`, `LOTE_CORREO_ANULADO` | Solo `NOTIFICACION_EMITIDA`(8) y `NOTIFICACION_CONFIRMADA`(9). 9 eventos sin equivalente. |
| Fallo y apelacion (8) | `FALLO_ABSOLUTORIO_DICTADO`, `FALLO_CONDENATORIO_DICTADO`, `RESULTADO_FINAL_*`, `APELACION_REGISTRADA`, `VENCIMIENTO_PLAZO_APELACION`, `APELACION_RESUELTA`, `ABSOLUCION_MARCADA_DIRECTO` | Solo `ACTO_ADMINISTRATIVO_DICTADO`(12), `APELACION_INTERPUESTA`(13). 6 sin equivalente. |
| Pagos (12) | `PAGO_VOLUNTARIO_SOLICITADO`, `MONTO_FIJADO`, `PAGO_INFORMADO_*`, `COMPROBANTE_*`, `PAGO_VOLUNTARIO_*`, `PAGO_CONDENA_*`, `CONDENA_CONSENTIDA_Y_PAGO_REGISTRADO` | Solo `PAGO_REGISTRADO`(11). 11 sin equivalente. |
| Bloqueantes materiales (5) | `CONSTATACION_MATERIAL_TEMPRANA`, `MEDIDA_POSTERIOR_A_LABRADO`, `ORIGEN_BLOQUEANTE_RECONOCIDO`, `RESOLUCION_DOCUMENTAL_BLOQUEANTE`, `CUMPLIMIENTO_MATERIAL_BLOQUEANTE` | `MEDIDA_PREVENTIVA_APLICADA`(17), `MEDIDA_PREVENTIVA_LEVANTADA`(18). 3 sin equivalente. |
| Gestion externa (9) | `DERIVADO_A_APREMIO`, `DERIVADO_A_JUZGADO_DE_PAZ`, `APREMIO_*` (5), `JUZGADO_*` (3) | Solo `DERIVACION_EXTERNA`(14). 8 sin equivalente. |
| Portal infractor (4) | `VISUALIZACION_PORTAL`, `APELACION_PORTAL_REGISTRADA`, `PAGO_VOLUNTARIO_PORTAL_*` | Sin equivalente directo. |

**Gap critico:** El DDL define 19 tipos de evento genericos; el prototipo validado requiere 62 eventos especializados. Se necesitan al menos 43 tipos de evento adicionales o una decision de diseno sobre granularidad.

### 8.3 Tabla de eventos: arquitectura

- **DDL:** `FalActaEvento` es la unica tabla de eventos; no existe catalogo separado.
- **Prototipo:** `ActaEventoMock` almacena el tipo de evento como `String` libre.
- **Decision pendiente:** Ampliar `TipoEvt` a 62+ valores, o usar un catalogo de tabla, o hibridar con `TipoEvt` generico + campos contextuales.

### 8.4 Eventos por categoria (existencia en DDL o SQL operativo)

| Categoria | Tabla DDL relacionada | Observaciones |
|---|---|---|
| Eventos de acta | `FalActaEvento` | Tabla existe; 19 tipos definidos |
| Eventos de documentos | `FalActaEvento` (`TipoEvt` 6--7) | Cubiertos parcialmente |
| Eventos de notificacion | `FalActaEvento` (`TipoEvt` 8--9) | Solo 2 de 11 cubiertos |
| Eventos de pagos | `FalActaEvento` (`TipoEvt` 11) | Solo 1 de 12 cubierto |
| Eventos de fallo/apelacion | `FalActaEvento` (`TipoEvt` 12--13) | 2 de 8 cubiertos |
| Eventos de gestion externa | `FalActaEvento` (`TipoEvt` 14) | 1 de 9 cubierto |
| Modelo de auditoria de articulos | `FalActaArticuloAuditoria` | Tabla separada para cambios en articulos infringidos |

---

## 9. Analisis de compatibilidad Informix -> MySQL

### 9.1 Tipos de dato: tabla de compatibilidad

| Tipo Informix | Portable a MySQL | Equivalente MySQL | Ajuste requerido | Aparece en |
|---|---|---|---|---|
| `INT8` | Si (con ajuste) | `BIGINT` | Renombrar; misma semantica | PKs, FKs principales |
| `INT` | Si -- identico | `INT` | Ninguno | PKs medianas, cantidades |
| `SMALLINT` | Si -- identico | `SMALLINT` o `TINYINT` | Opcional: usar `TINYINT(1)` para booleanos | Flags, enums, versiones |
| `DECIMAL(n,m)` | Si -- identico | `DECIMAL(n,m)` | Ninguno | Montos, coordenadas GPS |
| `CHAR(n)` | Si -- identico | `CHAR(n)` | Ninguno | GUIDs (`CHAR(36)`), codigos fijos |
| `VARCHAR(n)` | Si -- identico | `VARCHAR(n)` | Ninguno | Nombres, textos cortos |
| `LVARCHAR` | Requiere ajuste menor | `TEXT` o `MEDIUMTEXT` | Cambiar tipo | `ObsActa`, textos largos |
| `DATETIME YEAR TO SECOND` | Requiere rediseno sintactico | `DATETIME` | Cambiar tipo y sintaxis | Fechas con hora completa |
| `DATETIME YEAR TO DAY` | Requiere rediseno sintactico | `DATE` | Cambiar tipo y sintaxis | Vigencias, fechas administrativas |
| `DATETIME HOUR TO SECOND` | Requiere rediseno sintactico | `TIME` | Cambiar tipo y sintaxis | Si aparece |
| PostGIS `MultiPolygon`, `MultiLineString`, `Point` | Requiere rediseno | MySQL Spatial / MariaDB Geometry | Funciones distintas, SRID distinto | tablas `geo_gmat_*` |

### 9.2 Mecanismos de generacion de claves (Secuencias)

La spec usa **secuencias Informix** (`SeqFalActa`, `SeqFalActEv`, etc.) para generar PKs.

| Elemento | En Informix | En MySQL | Portabilidad |
|---|---|---|---|
| Secuencias (Seq*) | Objetos `SEQUENCE` nativos | `AUTO_INCREMENT` (MySQL) o `CREATE SEQUENCE` (MySQL 8.0+, MariaDB) | Portable con ajuste de DDL |
| `SeqFalActa` | `NEXT VALUE FOR SeqFalActa` | `AUTO_INCREMENT` o funcion sequence | Ajuste menor de DDL |
| PKs compuestas sin autoincrement | Tablas de version: `(IdDep, VerDep)` | Identico -- MySQL soporta PK compuesta | Portable |

**Recomendacion:** En MySQL, las tablas con PK `INT8` / `BIGINT` pueden usar `BIGINT AUTO_INCREMENT`. Las tablas con PK compuesta siguen igual.

### 9.3 Convenciones DDL acopladas a Informix

| Convencion | Descripcion | En MySQL |
|---|---|---|
| Nombres de objetos max 30 caracteres | Limitacion Informix para indices, constraints, secuencias | MySQL permite 64 caracteres -- puede relajarse |
| Sin `SERIAL`; usar secuencias | Estilo Informix | MySQL usa `AUTO_INCREMENT` directamente |
| `SMALLINT` para booleanos (`0/1`) | Idioma Informix | MySQL prefiere `TINYINT(1)` o `BOOLEAN`; `SMALLINT` funciona igual |
| `PascalCase` en nombres de tablas | Convencion propia del proyecto | MySQL es case-insensitive en Windows pero case-sensitive en Linux -- requiere atencion en deploy |
| Prefijos `Fal`, `Num`, `Stor` | Convencion propia -- no es Informix | Portable sin cambios |

### 9.4 Estructuras portable sin cambios

- Modelo relacional general (tablas, columnas, FKs logicas, indices por unicidad)
- `INT` y `SMALLINT` simples
- `DECIMAL(n,m)`
- `CHAR(n)`, `VARCHAR(n)`
- PKs simples y compuestas
- Logica de versionado referencial (patron tabla principal + tabla version)
- Modelo de evento append-only
- Convenciones de nombres de tablas y prefijos

### 9.5 Estructuras que requieren ajuste menor (sintaxis/tipo)

- `DATETIME YEAR TO SECOND` -> `DATETIME`
- `DATETIME YEAR TO DAY` -> `DATE`
- `LVARCHAR` -> `TEXT`
- Secuencias -> `AUTO_INCREMENT` o equivalente MySQL 8.0
- Nombres de constraints / indices (relajar limite de 30 chars)
- `SMALLINT` booleanos -> opcionalmente `TINYINT(1)` o `BOOLEAN`

### 9.6 Estructuras que requieren rediseno real

| Estructura | Motivo | Alcance del rediseno |
|---|---|---|
| Tablas PostGIS (`geo_gmat_*`) | PostGIS no existe en MySQL puro; MySQL Spatial usa funciones distintas y SRID diferente (`EPSG:22195` -> requiere validacion) | Medio -- funciones geoespaciales a reemplazar; modelo de datos similar |
| Tablas territoriales externas (`geo_ign_*`, `geo_indec_*`) | Son tablas externas/compartidas, no del modulo faltas -- calidad de datos heredada | Fuera del alcance del modulo; depende de equipo territorial |
| Sintaxis DDL Informix completa | Tipos `DATETIME`, `LVARCHAR`, `SERIAL`, secuencias, constraints style Informix | Ajuste sistematico de DDL -- no conceptual sino sintactico |
| SQL operativo con funciones Informix especificas | Si existen funciones como `EXTEND()`, `MDY()`, `TODAY` -- no leidos completamente | Requiere lectura de `07a`--`07d` y `08` para inventario completo |

### 9.7 Decisiones pendientes de tomar sobre MySQL

1. Estrategia de PKs: `AUTO_INCREMENT` en cada tabla vs secuencias MySQL 8.0.
2. Booleanos: `SMALLINT(0/1)` vs `TINYINT(1)` / `BOOLEAN`.
3. Geoespacial: MySQL Spatial vs PostGIS via extension -- si se mantienen capas PostGIS.
4. Case-sensitivity en nombres de tabla: documentar convencion de deploy para Linux.
5. Motor de tabla: `InnoDB` (recomendado para FK y transacciones) vs otros.
6. Charset: `utf8mb4` con collation `utf8mb4_unicode_ci` o equivalente.
7. `TEXT` vs `MEDIUMTEXT` para campos `LVARCHAR` segun tamano real esperado.

### 9.8 SQL operativo completo -- conclusion de acoplamiento Informix

Los 8 archivos pendientes de `spec/14-sql-operativo/` fueron revisados en este slice: `03a`, `03b`, `03c`, `07a`, `07b`, `07c`, `07d`, `08`.

**No se encontraron funciones SQL especificas de Informix** en ninguno de estos archivos. El SQL esta descrito como patrones y operaciones conceptuales, sin sentencias DML o DDL literales Informix-especificas.

| Archivo SQL | Acoplamiento Informix nuevo detectado |
|---|---|
| `03a` -- lookups domicilio infractor | Ninguno |
| `03b` -- lookups licencia y jurisdiccion | Ninguno |
| `03c` -- lookups catalogos y validaciones | Ninguno |
| `07a` -- referenciales versionados | Secuencias (`NEXT VALUE FOR Seq*`) -- ya documentado en 9.2 |
| `07b` -- rubros compartidos | Ninguno |
| `07c` -- numeracion transversal | Secuencias (`NEXT VALUE FOR Seq*`) -- ya documentado en 9.2 |
| `07d` -- storage transversal | Ninguno |
| `08` -- proyecciones y reproceso | Ninguno |

**Conclusion:** El acoplamiento Informix ya documentado (tipos `DATETIME YEAR TO SECOND`, `LVARCHAR`, secuencias `NEXT VALUE FOR`) sigue siendo el UNICO detectado en todo el SQL operativo. No hay funciones propietarias como `EXTEND()`, `MDY()`, `TODAY`, ni extensiones de Informix en ninguna parte del SQL operativo. El modelo es portable a MySQL con los ajustes menores ya documentados en secciones 9.1 a 9.7.

---

## 10. Proximo cruce recomendado

### 10.1 Fuentes completas para gap analysis

**Todas las fuentes relevantes han sido revisadas.** No quedan fuentes pendientes.

| Fuente | Estado final | Aporte al gap analysis |
|---|---|---|
| `spec/01-dominio/` | Completa | Entidades, estados y reglas de negocio |
| `spec/02-reglas-transversales/` | Completa | Reglas operativas transversales |
| `spec/03-bandejas/` | Completa -- revisada en este slice | 12 bandejas; modelo de proyeccion via snapshot; gaps de Paralizacion y Archivo |
| `spec/04-backend/` | Completa | Bloques de servicio y reglas de implementacion |
| `spec/12-datos/` | Completa | Modelo logico con campos por entidad |
| `spec/13-ddl/` | Completa | DDL fisico; 41 tablas; enums; secuencias |
| `spec/14-sql-operativo/` | Completa -- revisada en este slice | Patrones transaccionales; SQL conceptual; sin Informix-especifico adicional |
| `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md` | Completa | 12 agregados, 62 eventos, enums y dudas del prototipo |

### 10.2 Fuentes que faltan o quedaron parciales

**Ninguna.** El inventario queda completo con este slice.

Nota: El SQL operativo no tiene funciones Informix-especificas en ningun archivo revisado. El acoplamiento Informix detectado (secuencias, tipos DATETIME/LVARCHAR) ya esta documentado y no requiere lectura adicional para el gap conceptual.

### 10.3 Agregados con material suficiente para gap analysis inmediato

| Agregado | Material disponible | Tipo de gap esperable |
|---|---|---|
| Acta / Expediente | Excelente -- spec, DDL, prototipo, SQL, bandejas | Divergencia de estados entre prototipo y DDL |
| Documento / Pieza | Bueno -- spec, DDL, prototipo, bandejas (D5/D6) | Divergencia de estados; tipos de documento |
| Notificacion | Bueno -- spec, DDL, prototipo, bandejas (D7 unificada) | Distribucion en 3 tablas vs entidad unica del prototipo |
| EventoAuditoria | Bueno -- DDL `FalActaEvento`, prototipo | 19 vs 62 eventos -- gap critico de granularidad |
| SnapshotOperativo | Excelente -- DDL `FalActaSnapshot`, prototipo, spec/08 SQL, bandejas | Campos faltantes para Paralizacion y Archivo |
| PagoVoluntario | Suficiente -- bandeja D3 confirma lugar operativo; solo flags en snapshot | Sin tabla propia en spec; solo flags snapshot; decision de diseno requerida |
| Fallo | Suficiente -- bandeja D5 confirma es pieza documental (`TipoDocu=4`) | Sin entidad separada en spec; nombre diverge |
| Apelacion | Suficiente -- bandeja D8 confirma solo con apelacion real; `FhVtoApelacion` en snapshot | Sin tabla propia; solo evento `TipoEvt=13`; decision de diseno requerida |
| GestionExterna | Suficiente -- bandeja D9 aclara reingreso obligatorio; flags en snapshot | Sin tabla propia; `SiReingresoGestionExt` ya existe |

### 10.4 Agregados que siguen requiriendo decision de diseno antes del gap definitivo

| Agregado | Estado tras inventario completo | Decision pendiente |
|---|---|---|
| PagoCondena | Igual que PagoVoluntario -- solo flags snapshot | Tabla propia vs proyeccion de `FalActaEvento` + `FalActaSnapshot` |
| Paralizacion | Bandeja D10 exige campo motivo en snapshot; sin campo ni tabla en DDL | Agregar campo `MotivoParalizacion` en snapshot vs tabla propia de paralizacion |
| BloqueanteCierreMaterial | Bandejas D11/D12 exigen ver causales en snapshot; sin campo estructurado | Agregar campos de causales bloqueantes en snapshot vs tabla separada |
| LoteCorreo | Completamente ausente de toda fuente de spec incluyendo bandejas | Decidir si se modela como entidad propia o se absorbe en otro concepto |

### 10.5 Propuesta de documento siguiente

**Recomendacion: proceder al gap analysis completo.**

El inventario esta completo. Hay material suficiente para iniciar `DOMINIO_REAL_CRUCE_SPEC_PERSISTENCIA.md`.

`DOMINIO_REAL_CRUCE_SPEC_PERSISTENCIA.md`

Contenido propuesto:
1. **Gap analysis por agregado** -- comparar campo a campo prototipo vs DDL para todos los agregados con material suficiente: Acta, Documento, Notificacion, EventoAuditoria, SnapshotOperativo, PagoVoluntario, Fallo, Apelacion, GestionExterna.
2. **Decision sobre eventos** -- proponer estrategia para TipoEvt (19 -> 62+): ampliar enum, agregar tabla catalogo, o hibrido.
3. **Decision sobre entidades sin tabla** -- para PagoVoluntario, PagoCondena, Fallo, Apelacion, GestionExterna, Paralizacion, LoteCorreo: definir si son tablas propias o proyecciones de `FalActaEvento` + `FalActaSnapshot`.
4. **Decision sobre gaps de snapshot** -- agregar campo `MotivoParalizacion`; agregar campo/s de causales bloqueantes de archivo.
5. **Decision MySQL** -- confirmar estrategia de tipos, PKs y booleanos antes de que impacte el gap.
6. **BloqueanteCierreMaterial** -- decision de separacion formal entre medidas preventivas y otros bloqueantes (rodado, documentacion).
7. **LoteCorreo** -- decision de diseno desde cero (completamente ausente de spec).

---

## 11. Resumen ejecutivo de hallazgos

### Fuentes revisadas: inventario completo

| Bloque | Cantidad de archivos revisados |
|---|---|
| `spec/01-dominio/` | 12 archivos |
| `spec/02-reglas-transversales/` | 8 archivos |
| `spec/03-bandejas/` | 14 archivos (nuevo en este slice) |
| `spec/04-backend/` | 18 archivos |
| `spec/12-datos/` | 8 archivos |
| `spec/13-ddl/` | 19 archivos |
| `spec/14-sql-operativo/` | 16 archivos (completado en este slice) |
| `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md` | 1 archivo |
| **Total** | **96 archivos** |

### Entidades/agregados encontrados total: 11 en spec/dominio + 4 grupos referenciales + storage + numeracion

### Tablas encontradas en DDL: 41 tablas definidas (mas tablas territoriales externas)

**Tablas con DDL completo:**
`FalActa`, `FalActaEvidencia`, `FalActaEvento`, `FalObservacion`, `FalDependencia`, `FalDependenciaVersion`, `FalInspector`, `FalInspectorVersion`, `FalMedidaPreventiva`, `FalActaTransito`, `FalActaTransitoAlcoholemia`, `FalActaVehiculo`, `FalActaContravencion`, `FalActaSustanciasAlimenticias`, `FalActaMedidaPreventiva`, `NormativaFaltas`, `ArticuloNormativaFaltas`, `TarifarioUnidadFaltas`, `FalActaArticuloInfringido`, `FalActaArticuloAuditoria`, `Alcoholimetro`, `AlcoholimetroVersion`, `RubroCom`, `RubroComVersion`, `NumPolitica`, `NumTalonario`, `NumTalonarioDependencia`, `NumTalonarioInspector`, `NumTalonarioMovimiento`, `FalDocumento`, `FalActaDocumento`, `FalDocumentoFirma`, `FalDocumentoObservacion`, `FalNotificacion`, `FalNotificacionIntento`, `FalNotificacionAcuse`, `FalNotificacionObservacion`, `FalActaSnapshot`, `StorBackend`, `StorPolitica`, `StorObjeto`

### Bandejas definidas en spec/03-bandejas: 12 bandejas operativas

D1 Labradas, D2 Enriquecimiento, D3 Analisis/Pagos, D4 Pendientes resolucion, D5 Pendientes fallo, D6 Pendientes firma, D7 Notificaciones (transversal unificada), D8 Con apelacion, D9 Gestion externa, D10 Paralizadas, D11 Archivo, D12 Cerradas.

**Hallazgo clave bandejas:** Las bandejas son proyecciones operativas del snapshot (`CodBandeja` en `FalActaSnapshot`), no fuente de verdad. El DDL define 9 `BloqueActual`; la spec define 12 bandejas. Gap de 3 situaciones sin bloque propio.

### Estados/enums encontrados: 21 enums con valores definidos en DDL

`BloqueActual`(9), `EstProcAct`(18), `SitAdmAct`(8), `TipoCierreAct`(5), `EstPagoAct`(8), `EstDocAct`(7), `EstNotifAct`(9), `TipoEvt`(19), `OrigenEvt`(5), `TipoEvid`(5), `TipoDocu`(8), `EstadoDocu`(7), `RolDocuActa`(7), `TipoNotif`(4), `EstadoNotif`(9), `CanalNotif`(7), `EstadoIntento`(7), `TipoAcuse`(6), `EstadoAcuse`(5), `TipoTalonario`(2), `EstadoNro`(2)

### Eventos/auditoria encontrados

- Tabla `FalActaEvento`: modelo de evento con 19 tipos definidos
- Tabla `FalActaArticuloAuditoria`: auditoria especifica de cambios en articulos infringidos
- Gap critico: 62 eventos del prototipo vs 19 tipos en DDL

### Agregados con material suficiente para cruce inmediato

Acta, Documento, Notificacion, EventoAuditoria, SnapshotOperativo, PagoVoluntario, Fallo, Apelacion, GestionExterna

### Agregados que requieren decision de diseno antes del gap definitivo

PagoCondena, Paralizacion, BloqueanteCierreMaterial, LoteCorreo

### Hallazgo MySQL

La spec de persistencia esta totalmente orientada a Informix. El modelo conceptual (tablas, relaciones, campos) es portable. El DDL fisico (tipos de dato, secuencias, sintaxis) requiere adaptacion sistematica pero no conceptual, excepto las capas PostGIS. El SQL operativo no tiene funciones Informix-especificas adicionales; el unico acoplamiento detectado es en secuencias y tipos de dato ya documentados.

### Hallazgo gaps criticos detectados en este slice

1. **BloqueActual vs Bandejas:** 9 bloques DDL, 12 bandejas operativas -- discriminacion requiere `CodBandeja`.
2. **Paralizacion:** sin campo `MotivoParalizacion` en `FalActaSnapshot` ni tabla propia.
3. **Archivo:** sin campo estructurado de causales bloqueantes en `FalActaSnapshot`.
4. **LoteCorreo:** completamente ausente de toda fuente de spec incluyendo bandejas.
5. **TipoEvt:** 19 valores en DDL vs 62 eventos del prototipo -- decision de granularidad pendiente.

### Recomendacion

**Proceder al gap analysis completo.** El inventario esta completo. Crear `DOMINIO_REAL_CRUCE_SPEC_PERSISTENCIA.md`.

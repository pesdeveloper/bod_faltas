# Propuesta revisable de modelo de tablas MySQL para dominio real de Faltas

**Fecha:** 2026-06-13
**Estado:** PROPUESTA REVISABLE — no es dominio cerrado ni verdad final
**Destino tecnico:** MySQL 8.x
**Fuentes principales:**
- `DOMINIO_REAL_CRUCE_DECISIONAL_PRE_MYSQL.md`
- `DOMINIO_REAL_INVENTARIO_SPEC_PERSISTENCIA.md`
- `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md`

**Documento siguiente propuesto:** DDL MySQL fisico (solo despues de revision humana completa, tabla por tabla y campo por campo)

---

## 1. Resumen ejecutivo

### 1.1 Naturaleza de este documento

Este documento es una **propuesta revisable**. No cierra el dominio. No es la verdad final del modelo de datos.

El armado final del modelo — tabla por tabla, campo por campo — debe hacerse **manualmente** con revision humana explicita. Solo despues de esa revision se puede avanzar al DDL MySQL fisico, a los repositorios Spring JDBC y a la lectura real de datos.

Cursor preparo esta propuesta como insumo de revision. La decision final es humana.

### 1.2 Destino tecnico

- **Motor:** MySQL 8.x (InnoDB, `utf8mb4`, decision de collation pendiente)
- **Persistencia:** Spring JDBC / `JdbcClient` — sin ORM, sin JPA/Hibernate
- **SQL:** explicito y nativo; portable desde patrones de `spec/14-sql-operativo/`
- **DDL Informix:** tratado como referencia conceptual e historica; no es el destino tecnico
- **Nombres de tablas y columnas:** `snake_case` en minusculas (evita problema de case-sensitivity Linux/Windows)

### 1.3 Principios del modelo propuesto

- Eventos append-only: ningun evento se edita ni borra
- `fal_acta_snapshot` es cache regenerable; no es fuente de verdad primaria
- Bandeja = proyeccion derivada; `cod_bandeja` en snapshot es cache, no fuente primaria
- `fal_acta` + `fal_acta_evento` son la fuente de verdad del expediente
- Separacion estricta entre pago voluntario y pago condena (circuitos distintos)
- Separacion entre archivo y cierre (no son equivalentes)
- Separacion entre bloqueantes documentales y bloqueantes materiales
- Portal infractor no expone estados internos (`bandeja_actual`, `accion_pendiente`, marcas operativas)
- Strings legibles para API/QA/Direccion; preferir `SMALLINT` con catalogo documentado sobre `ENUM`

### 1.4 Tablas casi seguras

Tienen soporte solido de spec + DDL Informix historico. El trabajo es adaptar tipos y sintaxis a MySQL.

| Tabla MySQL candidata | Base de soporte | Decision necesaria |
|---|---|---|
| `fal_acta` | spec + DDL `FalActa` (~64 cols) + prototipo | Alinear estados; agregar campos faltantes |
| `fal_acta_evento` | spec + DDL `FalActaEvento` + prototipo | Decision `TipoEvt` pendiente (ver sec. 6) |
| `fal_acta_snapshot` | spec + DDL `FalActaSnapshot` + SQL/08 | Agregar campos faltantes criticos |
| `fal_acta_evidencia` | DDL `FalActaEvidencia` | — |
| `fal_observacion` | DDL `FalObservacion` | — |
| `fal_documento` | spec + DDL `FalDocumento` | Alinear estados y tipos |
| `fal_acta_documento` | spec + DDL `FalActaDocumento` | Alinear roles |
| `fal_documento_firma` | spec + DDL `FalDocumentoFirma` | — |
| `fal_notificacion` | spec + DDL `FalNotificacion` | Agregar `lote_id`, alinear estados |
| `fal_notificacion_intento` | spec + DDL `FalNotificacionIntento` | Agregar `lote_id`, `referencia_externa` |
| `fal_notificacion_acuse` | spec + DDL `FalNotificacionAcuse` | — |
| `fal_dependencia` / `_version` | DDL `FalDependencia` + `FalDependenciaVersion` | — |
| `fal_inspector` / `_version` | DDL `FalInspector` + `FalInspectorVersion` | — |
| `fal_medida_preventiva` | DDL `FalMedidaPreventiva` | — |
| `fal_acta_medida_preventiva` | DDL `FalActaMedidaPreventiva` | Confirmar relacion con bloqueante cierre material |
| `num_politica` / `num_talonario` / satelites | DDL `NumPolitica`, `NumTalonario`, etc. | — |
| `stor_backend` / `stor_politica` / `stor_objeto` | DDL `StorBackend`, `StorPolitica`, `StorObjeto` | — |
| `fal_normativa_faltas` / `fal_articulo_normativa_faltas` | DDL | — |
| `fal_tarifario_unidad_faltas` | DDL | — |
| `fal_acta_articulo_infringido` / `_auditoria` | DDL | — |
| `fal_acta_transito` / `_vehiculo` / `_contravencion` / `_alcoholemia` | DDL satelites | Segun tipo de acta |

### 1.5 Tablas con veredicto humano pendiente

Surgen del prototipo validado o de gaps criticos de spec. Requieren decision antes de DDL.

| Tabla MySQL candidata | Decision pendiente | Prioridad |
|---|---|---|
| `fal_acta_pago_voluntario` | Tabla propia vs evento+snapshot (rec: tabla propia) | Alta |
| `fal_acta_pago_condena` | Tabla separada — casi obligatoria | Alta |
| `fal_acta_paralizacion` | Tabla historica + campo en snapshot (rec: ambas) | Alta |
| `fal_acta_bloqueante_cierre_material` | Tabla propia vs extension medida preventiva | Alta |
| `fal_acta_fallo` | Entidad separada vs doc enriquecido vs hibrido | Alta |
| `fal_acta_apelacion` | Tabla propia vs evento enriquecido | Media |
| `fal_acta_gestion_externa` | Tabla propia vs flags+eventos | Media |
| `fal_acta_archivo` | Tabla historica vs campos en acta | Media |
| `fal_lote_correo` | Diferir vs tabla propia (rec: diferir) | Baja |
| `fal_acta_qr_acceso` | Diferir vs campo en documento (rec: diferir) | Baja |

---

## 2. Nucleo expediente

### 2.1 Clasificacion de tablas del nucleo

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta` | Agregado raiz del expediente de faltas | Principal | Obligatoria primera version | spec + DDL + prototipo | Casi segura — alinear campos |
| `fal_acta_evento` | Historial append-only de hechos | Historica | Obligatoria primera version | spec + DDL + prototipo | Casi segura — decision TipoEvt pendiente |
| `fal_acta_snapshot` | Proyeccion operativa regenerable | Snapshot/cache | Obligatoria primera version | spec + DDL + prototipo | Casi segura — agregar campos |
| `fal_acta_evidencia` | Evidencias digitales adjuntas | Hija | Probable | DDL | Casi segura |
| `fal_observacion` | Observaciones polimorficas | Hija/catalogo | Probable | DDL | Casi segura |

### 2.2 Tabla `fal_acta` — campos propuestos

> Ver tambien seccion 5 para definicion detallada.

| Campo sugerido | Tipo logico | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Consumidor probable | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|---|---|
| `id` | PK numerica | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador interno del acta | Clave primaria | Todos | DDL `FalActa.Id INT8` | — |
| `id_tecnico` | UUID | `CHAR(36)` | NO | UNIQUE | Identificador tecnico externo (UUID) | Unicidad tecnica desde mobile/labrado | Sincronizacion mobile, APIs externas | DDL `FalActa.IdTecnico CHAR(36)` | Confirmar si se sigue usando UUID o cambia a ULID |
| `nro_acta` | Cadena | `VARCHAR(30)` | SI | IDX | Numero visible del acta (talonario) | Numeracion por talonario; puede ser NULL en borrador | Bandejas, busqueda, portal | DDL `FalActa.NroActa VARCHAR(20)` | Ampliar a VARCHAR(30)? confirmar con numeracion |
| `tipo_acta` | Entero | `SMALLINT` | NO | IDX | Tipo de acta (transito, bromatologia, contravencional, etc.) | Determina satelites y circuitos | Busqueda, UI formulario | DDL `FalActa.TipoActa SMALLINT` | Catalogo de tipos pendiente de decision; valores exactos |
| `origen_captura` | Entero | `SMALLINT` | NO | — | Canal de origen del labrado (web, mobile, etc.) | Trazabilidad de origen | Auditoria, estadisticas | DDL `FalActa.OrigenCaptura SMALLINT` | Valores del enum a confirmar |
| `fh_acta` | Fecha+hora | `DATETIME(6)` | NO | IDX | Fecha y hora del labrado del acta | Ordenamiento, plazos | Bandejas, busqueda, timeline | DDL `FalActa.FhActa DATETIME YEAR TO SECOND` | Precision: MySQL DATETIME(6) para microsegundos |
| `id_dep` | FK entero | `BIGINT` | NO | FK+IDX | Dependencia que labro el acta | Adscripcion organizacional | Filtro por dependencia | DDL `FalActa.IdDep INT` | Confirmar INT vs BIGINT segun escala |
| `ver_dep` | Entero | `SMALLINT` | NO | — | Version de la dependencia al momento del labrado | Snapshot de la dependencia vigente al labrar | Trazabilidad | DDL `FalActa.VerDep SMALLINT` | — |
| `id_insp` | FK entero | `BIGINT` | SI | FK+IDX | Inspector que labro el acta | Actor principal del labrado | Filtro por inspector | DDL `FalActa.IdInsp INT` | NULL en borrador; confirmar si puede no tener inspector |
| `ver_insp` | Entero | `SMALLINT` | SI | — | Version del inspector al momento del labrado | Snapshot de inspector vigente | Trazabilidad | DDL `FalActa.VerInsp SMALLINT` | — |
| `nom_infct` | Texto | `VARCHAR(120)` | SI | IDX | Nombre del infractor | Identificacion del infractor | Busqueda, bandejas, portal | DDL `FalActa.NomInfct VARCHAR(64)` | Ampliar a 120? confirmar limite real |
| `doc_pref_infct` | Entero | `SMALLINT` | SI | — | Prefijo del documento del infractor (DNI, CUIT, etc.) | Identificacion del infractor | Busqueda | DDL `FalActa.DocPrefInfct SMALLINT` | Catalogo de prefijos a documentar |
| `doc_nro_infct` | Entero | `BIGINT` | SI | IDX | Numero del documento del infractor | Busqueda por documento | Busqueda | DDL `FalActa.DocNroInfct INT` | Ampliar a BIGINT para CUIT |
| `tipo_pers_infct` | Entero | `SMALLINT` | SI | — | Tipo de persona del infractor (fisica/juridica) | Determina datos requeridos del infractor | Formulario, busqueda | DDL `FalActa.TipoPersInfct SMALLINT` | Confirmar valores |
| `resumen_hecho` | Texto libre | `TEXT` | SI | — | Descripcion narrativa del hecho infractivo | Contenido del acta | UI detalle, PDF | DDL `FalActa.ObsActa LVARCHAR` -> `TEXT` | Confirmar si hay limite real de longitud |
| `bloque_actual` | Entero | `SMALLINT` | NO | IDX | Bloque juridico/procesal actual | Discriminador de bandeja (junto con est_proc_act) | Bandejas, busqueda | DDL `FalActaSnapshot.BloqueActual` — en acta por decision | Confirmar si bloque vive en acta o solo en snapshot |
| `est_proc_act` | Entero | `SMALLINT` | NO | IDX | Estado del proceso administrativo actual | Estado operativo del expediente | Bandejas, filtros | DDL `FalActaEvento.EstProcAnt/Nvo` | Confirmar catalogo definitivo 18 valores |
| `sit_adm_act` | Entero | `SMALLINT` | NO | IDX | Situacion administrativa actual | Discriminador paralizada/activa/cerrada | Bandejas, filtros | DDL `FalActaEvento.SitAdmAnt/Nva` | Confirmar catalogo 8 valores DDL |
| `resultado_final` | Entero | `SMALLINT` | SI | IDX | Resultado juridico final del expediente | Determina cerrabilidad; no hay campo directo en DDL Informix | Cierre, bandeja cerradas, portal | Prototipo — ausente en DDL | Valores: 0=SIN_RESULTADO, 1=ABSUELTO, 2=PAGO_CONFIRMADO, 3=CONDENADO, 4=CONDENA_FIRME — confirmar |
| `esta_cerrada` | Booleano | `BOOLEAN` | NO | IDX | Flag de cierre definitivo del expediente | Cierre solo por endpoint explicito; no automatico | Bandejas, cerrabilidad | Prototipo — DDL implícito via EstProcAct=17 | Confirmar si se materializa como campo o se deriva |
| `motivo_archivo` | Entero | `SMALLINT` | SI | — | Motivo del archivo del expediente | Distingue origen de archivo; afecta permite_reingreso | Bandeja archivo, reingreso | DDL `TipoCierreAct` parcial — prototipo extiende | Divergencia DDL vs prototipo: alinear valores |
| `permite_reingreso` | Booleano | `BOOLEAN` | SI | — | Indica si el archivo permite reingreso | Nulidad siempre permite; otros pendiente de definicion | Bandeja archivo, reingreso | Prototipo — ausente en DDL | Confirmar regla para cada valor de motivo_archivo |
| `fh_cierre` | Fecha+hora | `DATETIME(6)` | SI | — | Fecha y hora del cierre definitivo | Trazabilidad de cierre | Auditoria, reporte | Nuevo — no en DDL Informix | — |
| `fh_archivo` | Fecha+hora | `DATETIME(6)` | SI | — | Fecha y hora del archivo | Trazabilidad de archivo | Auditoria, reporte | Nuevo — no en DDL Informix | Puede quedar en tabla historica de archivo en lugar de aqui |
| `id_tipo_gestion_ext_act` | Entero | `SMALLINT` | SI | — | Tipo de gestion externa activa (si aplica) | Discriminador de sub-estado en bandeja gestion externa | Bandeja gestion externa | DDL `FalActaSnapshot.TipoGestionExt` — en acta | Confirmar si vive en acta o solo en gestion_externa |
| `fh_alta` | Fecha+hora | `DATETIME(6)` | NO | — | Fecha y hora de alta del registro | Auditoria de alta | Auditoria | DDL `FalActa.FhAlta` | — |
| `id_user_alta` | UUID | `CHAR(36)` | NO | — | Usuario que creo el registro | Auditoria de alta | Auditoria | DDL `FalActa.IdUserAlta CHAR(36)` | Confirmar tipo segun sistema de usuarios |
| `fh_ult_mod` | Fecha+hora | `DATETIME(6)` | NO | — | Fecha de ultima modificacion del registro | Control de concurrencia | Auditoria | Nuevo | — |
| `id_user_ult_mod` | UUID | `CHAR(36)` | SI | — | Usuario de ultima modificacion | Auditoria | Auditoria | Nuevo | — |

**Campos de domicilio del infractor (bloque):** Los campos de domicilio del infractor en `FalActa` (~20 columnas de referencias geograficas + textos libres) se proponen como grupo. Incluyen: referencias a tablas `localidad`, `calle`, indicador `si_calle_txt_infct`, `calle_txt_infct`, `si_norm_parcial_infct`, `alt_infct`, `piso_infct`, `depto_infct`, `obs_dom_infct`, `cod_pos_infct`. Ver DDL `FalActa` para detalle completo.

**Campos de domicilio de la infraccion (bloque):** Idem para el lugar del hecho. Referencias geo + `si_dom_txt_infr`, `dom_txt_infr`, `si_eje_urb`.

**Campos GPS:** `lat_infr DECIMAL(12,8)`, `lon_infr DECIMAL(12,8)`.

**Dudas pendientes globales de `fal_acta`:**
- Confirmar si `bloque_actual` y `est_proc_act` viven en `fal_acta` o solo en `fal_acta_snapshot` (snapshot tiene que poder reconstruirse desde acta+eventos).
- Confirmar si `resultado_final` vive en `fal_acta` o solo en snapshot (recomendacion: en acta, porque es parte del estado permanente del expediente).
- Confirmar valores definitivos de `motivo_archivo` (DDL tiene 5 valores, prototipo tiene 3 distintos).
- Confirmar si `esta_cerrada` es campo bool directo o se deriva de `est_proc_act=17`.

### 2.3 Tabla `fal_acta_evento` — campos propuestos

> Ver tambien seccion 6 para definicion detallada y gap analysis TipoEvt.

| Campo sugerido | Tipo logico | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Rol en modelo | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|---|
| `id` | PK numerica | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del evento | PK | DDL `FalActaEvento.Id INT8` | — |
| `acta_id` | FK numerica | `BIGINT` | NO | FK+IDX | Acta a la que pertenece el evento | Relacion con expediente | DDL `FalActaEvento.IdActa INT8` | — |
| `tipo_evt` | Entero | `SMALLINT` | NO | IDX | Tipo de evento (catalogo) | Discriminador del evento | DDL `FalActaEvento.TipoEvt SMALLINT` (19 vals) | Decision pendiente: 19 DDL vs 62+ prototipo |
| `origen_evt` | Entero | `SMALLINT` | NO | — | Origen del actor que genera el evento | Trazabilidad de canal | DDL `FalActaEvento.OrigenEvt SMALLINT` (5 vals) | — |
| `fh_evt` | Fecha+hora | `DATETIME(6)` | NO | IDX | Fecha y hora del evento (precision alta) | Ordenamiento del historial | DDL `FalActaEvento.FhEvt` | Usar DATETIME(6) para microsegundos |
| `bloque_func` | Entero | `SMALLINT` | NO | — | Bloque funcional en que ocurrio el evento | Contexto del bloque al momento del evento | DDL `FalActaEvento.BloqueFunc SMALLINT` | — |
| `est_proc_ant` | Entero | `SMALLINT` | SI | — | Estado del proceso antes del evento | Reconstruccion del historial de estados | DDL `FalActaEvento.EstProcAnt SMALLINT` | NULL en primer evento (creacion) |
| `est_proc_nvo` | Entero | `SMALLINT` | SI | — | Estado del proceso despues del evento | Reconstruccion del historial de estados | DDL `FalActaEvento.EstProcNvo SMALLINT` | — |
| `sit_adm_ant` | Entero | `SMALLINT` | SI | — | Situacion administrativa antes del evento | Reconstruccion del historial | DDL `FalActaEvento.SitAdmAnt SMALLINT` | — |
| `sit_adm_nva` | Entero | `SMALLINT` | SI | — | Situacion administrativa despues del evento | Reconstruccion del historial | DDL `FalActaEvento.SitAdmNva SMALLINT` | — |
| `actor_tipo` | Entero | `SMALLINT` | SI | — | Tipo de actor que ejecuta el evento | Trazabilidad de actor | Nuevo — no en DDL | Definir catalogo: OPERADOR, INSPECTOR, INFRACTOR, SISTEMA, INTEGRACION |
| `actor_id` | UUID/texto | `VARCHAR(60)` | SI | — | Identificador del actor | Trazabilidad de actor | Nuevo | Confirmar tipo segun sistema de usuarios |
| `canal` | Entero | `SMALLINT` | SI | — | Canal por el que se ejecuto la accion | Auditoria de canal | DDL `OrigenEvt` parcialmente cubre esto | Confirmar si se agrega canal separado o alcanza con origen_evt |
| `id_docu_rel` | FK numerica | `BIGINT` | SI | FK | Documento relacionado con este evento | Trazabilidad documental | DDL `FalActaEvento.IdDocuRel INT8` | — |
| `id_notif_rel` | FK numerica | `BIGINT` | SI | FK | Notificacion relacionada con este evento | Trazabilidad notificacion | DDL `FalActaEvento.IdNotifRel INT8` | — |
| `id_pres_rel` | FK numerica | `BIGINT` | SI | — | Presentacion relacionada (si existe entidad) | Trazabilidad de presentacion | DDL `FalActaEvento.IdPresRel INT8` | Entidad Presentacion no modelada aun |
| `id_user_evt` | UUID | `CHAR(36)` | SI | — | Usuario que ejecuto el evento | Auditoria de usuario | DDL `FalActaEvento.IdUserEvt CHAR(36)` | — |
| `si_evt_cierre` | Booleano | `BOOLEAN` | NO | — | Flag: este evento es un evento de cierre | Semantica de cierre | DDL `FalActaEvento.SiEvtCierre SMALLINT` | — |
| `si_evt_ext` | Booleano | `BOOLEAN` | NO | — | Flag: evento generado por integracion externa | Trazabilidad de fuente | DDL `FalActaEvento.SiEvtExt SMALLINT` | — |
| `si_permite_reing` | Booleano | `BOOLEAN` | NO | — | Flag: el estado resultante permite reingreso | Logica de reingreso | DDL `FalActaEvento.SiPermiteReing SMALLINT` | — |
| `descripcion_legible` | Texto | `VARCHAR(255)` | SI | — | Descripcion en lenguaje natural del evento | Legibilidad para API/QA | Nuevo — no en DDL | Util para log y UI de timeline |
| `correlacion_id` | UUID/texto | `VARCHAR(60)` | SI | — | ID de correlacion para trazabilidad distribuida | Trazabilidad de request | Nuevo | Opcional; considerar si hay sistema de correlacion |
| `payload_json` | JSON | `JSON` | SI | — | Payload especifico del tipo de evento | Detalle contextual del evento | Nuevo — estrategia hibrida si se adopta | Solo si se elige estrategia hibrida de TipoEvt; ver sec 6 |

**Restriccion semantica:** Esta tabla es append-only. Ningun evento se puede editar, actualizar ni borrar una vez insertado.

### 2.4 Tabla `fal_acta_snapshot` — campos propuestos

> Ver tambien seccion 7 para definicion detallada.

| Campo sugerido | Tipo logico | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Tipo campo | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|---|
| `acta_id` | FK/PK | `BIGINT` | NO | PK (1:1 con acta) | Acta a la que pertenece este snapshot | Fuente real | DDL `FalActaSnapshot.IdActa INT8` | — |
| `fh_acta` | Fecha+hora | `DATETIME(6)` | SI | IDX | Cache de fecha del acta | Cache derivado | DDL `FalActaSnapshot.FhActa` | Para filtros rapidos de bandeja |
| `id_dep` | FK | `BIGINT` | SI | IDX | Cache de dependencia del acta | Cache derivado | DDL snapshot | — |
| `ver_dep` | Entero | `SMALLINT` | SI | — | Version de dependencia cacheada | Cache derivado | DDL snapshot | — |
| `id_insp` | FK | `BIGINT` | SI | — | Cache de inspector | Cache derivado | DDL snapshot | — |
| `ver_insp` | Entero | `SMALLINT` | SI | — | Version de inspector cacheada | Cache derivado | DDL snapshot | — |
| `bloque_actual` | Entero | `SMALLINT` | NO | IDX | Bloque juridico/procesal actual | Cache derivado | DDL `BloqueActual SMALLINT` | — |
| `est_proc_act` | Entero | `SMALLINT` | NO | IDX | Estado del proceso actual | Cache derivado | DDL `EstProcAct SMALLINT` | — |
| `sit_adm_act` | Entero | `SMALLINT` | NO | IDX | Situacion administrativa actual | Cache derivado | DDL `SitAdmAct SMALLINT` | — |
| `cod_bandeja` | Texto | `VARCHAR(50)` | SI | IDX | Codigo de bandeja operativa actual | Cache derivado — discriminador clave | DDL `CodBandeja VARCHAR(50)` | Confirmar valores de las 12 bandejas; no editar manualmente |
| `si_visible_bandeja` | Booleano | `BOOLEAN` | NO | IDX | Flag de visibilidad en bandeja | Cache derivado | DDL `SiVisibleBandeja SMALLINT` | — |
| `prioridad` | Entero | `SMALLINT` | SI | IDX | Prioridad de ordenamiento en bandeja | Cache derivado | DDL `Prioridad SMALLINT` | — |
| `resultado_final` | Entero | `SMALLINT` | SI | IDX | Resultado juridico final cacheado | Cache derivado | Nuevo — ausente en DDL | Derivado de fal_acta.resultado_final |
| `motivo_paralizacion_act` | Entero | `SMALLINT` | SI | IDX | Motivo de la paralizacion activa actual | Cache derivado — **gap critico D10** | Nuevo — gap critico | NULL si no paralizada; derivado de tabla paralizacion o evento |
| `tipo_cierre_act` | Entero | `SMALLINT` | SI | — | Tipo de cierre/archivo del expediente | Cache derivado | DDL `TipoCierreAct` parcial | Alinear con valores finales de motivo_archivo |
| `accion_pendiente` | Entero/texto | `SMALLINT` | SI | IDX | Accion pendiente principal del expediente | Cache derivado o campo derivado | Nuevo — decision pendiente | Ver decision 8.10; puede ser SMALLINT con catalogo |
| `si_bloqueante_med_prev` | Booleano | `BOOLEAN` | NO | IDX | Flag: tiene medida preventiva activa bloqueante | Cache derivado — **gap critico D11** | Nuevo | Derivado de tabla bloqueante_cierre_material |
| `si_bloqueante_rodado` | Booleano | `BOOLEAN` | NO | IDX | Flag: tiene rodado secuestrado bloqueante | Cache derivado — **gap critico D11** | Nuevo | Derivado de tabla bloqueante_cierre_material |
| `si_bloqueante_doc_ret` | Booleano | `BOOLEAN` | NO | IDX | Flag: tiene documentacion retenida bloqueante | Cache derivado — **gap critico D11** | Nuevo | Derivado de tabla bloqueante_cierre_material |
| `si_notif_acta` | Booleano | `BOOLEAN` | NO | — | Flag: tiene notificacion de acta activa | Cache derivado | DDL `SiNotifActa SMALLINT` | — |
| `si_notif_acta_proc` | Booleano | `BOOLEAN` | NO | — | Flag: notificacion de acta en proceso | Cache derivado | DDL `SiNotifActaProc SMALLINT` | — |
| `si_notif_acta_acuse_pend` | Booleano | `BOOLEAN` | NO | — | Flag: acuse de notificacion acta pendiente | Cache derivado | DDL `SiNotifActaAcusePend SMALLINT` | — |
| `si_notif_med_prev` | Booleano | `BOOLEAN` | NO | — | Flag: notificacion de medida preventiva | Cache derivado | DDL `SiNotifMedPrev SMALLINT` | — |
| `si_notif_med_prev_proc` | Booleano | `BOOLEAN` | NO | — | Flag: notificacion medida preventiva en proceso | Cache derivado | DDL | — |
| `si_notif_med_prev_acuse_pend` | Booleano | `BOOLEAN` | NO | — | Flag: acuse notificacion medida pendiente | Cache derivado | DDL | — |
| `si_notif_fallo` | Booleano | `BOOLEAN` | NO | — | Flag: tiene notificacion de fallo | Cache derivado | DDL `SiNotifFallo SMALLINT` | — |
| `si_notif_fallo_proc` | Booleano | `BOOLEAN` | NO | — | Flag: notificacion de fallo en proceso | Cache derivado | DDL `SiNotifFalloProc SMALLINT` | — |
| `si_notif_fallo_acuse_pend` | Booleano | `BOOLEAN` | NO | — | Flag: acuse notificacion fallo pendiente | Cache derivado | DDL | — |
| `cant_reint_notif` | Entero | `SMALLINT` | NO | — | Cantidad de reintentos de notificacion | Cache derivado | DDL `CantReintNotif SMALLINT` | — |
| `si_pago_volunt` | Booleano | `BOOLEAN` | NO | IDX | Flag: tiene solicitud de pago voluntario | Cache derivado | DDL `SiPagoVolunt SMALLINT` | — |
| `fh_pago_volunt` | Fecha | `DATE` | SI | — | Fecha de pago voluntario | Cache derivado | DDL `FhPagoVolunt` | — |
| `monto_acta` | Decimal | `DECIMAL(14,2)` | SI | — | Monto del acta (para pago voluntario) | Cache derivado | DDL `MontoActa DECIMAL` | — |
| `est_pago_act` | Entero | `SMALLINT` | NO | IDX | Estado del pago voluntario actual | Cache derivado | DDL `EstPagoAct SMALLINT` (8 vals) | Alinear con situacion_pago de tabla pago_voluntario |
| `si_pago_total` | Booleano | `BOOLEAN` | NO | — | Flag: pago condena total confirmado | Cache derivado | DDL `SiPagoTotal SMALLINT` | — |
| `si_plan_pago` | Booleano | `BOOLEAN` | NO | — | Flag: tiene plan de pago | Cache derivado | DDL `SiPlanPago SMALLINT` | — |
| `cant_cuotas_plan` | Entero | `SMALLINT` | SI | — | Cantidad de cuotas del plan | Cache derivado | DDL `CantCuotasPlan SMALLINT` | — |
| `valor_cuota_plan` | Decimal | `DECIMAL(14,2)` | SI | — | Valor de cuota del plan | Cache derivado | DDL `ValorCuotaPlan DECIMAL` | — |
| `cant_caidas_plan` | Entero | `SMALLINT` | SI | — | Cantidad de cuotas caidas del plan | Cache derivado | DDL `CantCaidasPlan SMALLINT` | — |
| `si_gestion_ext` | Booleano | `BOOLEAN` | NO | IDX | Flag: expediente en gestion externa | Cache derivado | DDL `SiGestionExt SMALLINT` | — |
| `tipo_gestion_ext` | Entero | `SMALLINT` | SI | — | Tipo de gestion externa (APREMIO/JUZGADO_DE_PAZ) | Cache derivado | DDL `TipoGestionExt SMALLINT` | Valores exactos pendientes |
| `si_reingreso_gestion_ext` | Booleano | `BOOLEAN` | NO | — | Flag: reingresado desde gestion externa | Cache derivado | DDL `SiReingresoGestionExt SMALLINT` | — |
| `resultado_gestion_ext` | Entero | `SMALLINT` | SI | — | Resultado de la gestion externa | Cache derivado | DDL `ResultadoGestionExt SMALLINT` | Valores exactos pendientes |
| `fh_vto_presentacion` | Fecha | `DATE` | SI | IDX | Fecha de vencimiento de presentacion | Cache derivado | DDL `FhVtoPresentacion` | — |
| `fh_vto_apelacion` | Fecha | `DATE` | SI | IDX | Fecha de vencimiento del plazo de apelacion | Cache derivado | DDL `FhVtoApelacion` | — |
| `fh_vto_apremio` | Fecha | `DATE` | SI | IDX | Fecha de vencimiento del apremio | Cache derivado | DDL `FhVtoApremio` | — |
| `id_evt_ult` | FK | `BIGINT` | SI | FK | Ultimo evento registrado | Puntero de control | DDL `IdEvtUlt INT8` | — |
| `id_docu_ult` | FK | `BIGINT` | SI | FK | Ultimo documento generado | Puntero de control | DDL `IdDocuUlt INT8` | — |
| `id_notif_ult` | FK | `BIGINT` | SI | FK | Ultima notificacion | Puntero de control | DDL `IdNotifUlt INT8` | — |
| `fh_ult_mod` | Fecha+hora | `DATETIME(6)` | NO | — | Fecha de ultima actualizacion del snapshot | Control de version | DDL `FhUltMod` | — |
| `id_user_ult_mod` | UUID | `CHAR(36)` | SI | — | Usuario que actualizo el snapshot | Auditoria | DDL `IdUserUltMod CHAR(36)` | — |
| `fh_snapshot` | Fecha+hora | `DATETIME(6)` | NO | — | Fecha de generacion de este snapshot | Control de rebuild | DDL `FhSnapshot` | — |
| `rebuild_id` | Entero | `BIGINT` | SI | — | ID del ultimo ciclo de rebuild global | Control de consistencia | Nuevo | Util para detectar snapshots desactualizados |

**Reglas criticas del snapshot:**
- Ningun campo del snapshot debe editarse manualmente fuera del proceso de rebuild/actualizacion incremental.
- El snapshot se invalida y recalcula cuando se registra un evento relevante en `fal_acta_evento`.
- Todos los campos marcados como "Cache derivado" son proyecciones que NO se deben leer como fuente de verdad primaria.
- El snapshot es regenerable desde `fal_acta` + `fal_acta_evento` + entidades satelite en cualquier momento sin perdida de informacion.

### 2.5 Tabla `fal_acta_evidencia` — campos propuestos

| Campo sugerido | Tipo MySQL sugerido | Nullable | Descripcion | Origen |
|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO (PK) | Identificador | DDL `FalActaEvidencia.Id` |
| `acta_id` | `BIGINT` | NO (FK+IDX) | Acta que contiene la evidencia | DDL `IdActa` |
| `tipo_evid` | `SMALLINT` | NO | Tipo de evidencia (foto, video, documento) | DDL `TipoEvid` |
| `storage_key` | `VARCHAR(255)` | NO (IDX) | Clave de referencia al storage externo | DDL `StorageKey` |
| `nom_archivo` | `VARCHAR(120)` | SI | Nombre original del archivo | DDL |
| `mime_type` | `VARCHAR(80)` | SI | MIME type del archivo | DDL |
| `hash_evid` | `VARCHAR(128)` | SI | Hash de integridad del archivo | DDL |
| `obs_evid` | `VARCHAR(255)` | SI | Observacion sobre la evidencia | DDL |
| `orden_evid` | `SMALLINT` | NO | Orden de presentacion | DDL |
| `fh_captura` | `DATETIME(6)` | SI | Fecha/hora de captura de la evidencia | DDL |
| `fh_alta` | `DATETIME(6)` | NO | Fecha de alta del registro | DDL |
| `id_user_alta` | `CHAR(36)` | NO | Usuario que cargo la evidencia | DDL |

---

## 3. Documentos

### 3.1 Clasificacion de tablas documentales

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_documento` | Pieza documental firmable del expediente | Principal | Obligatoria primera version | spec + DDL | Casi segura — alinear estados y tipos |
| `fal_acta_documento` | Relacion acta-documento con rol funcional | Hija | Obligatoria primera version | spec + DDL | Casi segura — alinear roles |
| `fal_documento_firma` | Registro de firma individual por pieza | Hija | Obligatoria primera version | spec + DDL | Casi segura |
| `fal_documento_observacion` | Observaciones sobre documentos | Hija | Probable | DDL | Casi segura |

### 3.2 Tabla `fal_documento`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Consumidor | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del documento | — | Todos | DDL `FalDocumento.IdDocu INT8` | — |
| `tipo_docu` | `SMALLINT` | NO | IDX | Tipo de documento (acta, notificacion, fallo, etc.) | Determina circuito de firma y notificacion | Firma, notificacion, bandeja | DDL `TipoDocu SMALLINT` (8 vals) | Alinear valores: ACTA=1, NOTIF_ACTA=2, MED_PREV=3, ACTO_ADMIN=4, NOTIF_ACTO=5, CONSTANCIA=6, ANEXO=7, OTRO=8 |
| `estado_docu` | `SMALLINT` | NO | IDX | Estado del documento | Determina acciones disponibles | Firma, bandeja | DDL `EstadoDocu SMALLINT` (7 vals) | Alinear con estados del prototipo (EMITIDO, PENDIENTE_FIRMA, FIRMADO, ADJUNTO) |
| `nro_docu` | `VARCHAR(30)` | SI | IDX | Numero del documento (talonario) | Numeracion administrativa | UI, PDF | DDL `NroDocu VARCHAR(30)` | — |
| `id_talonario` | `BIGINT` | SI | FK | Talonario usado para numeracion | Vinculo con sistema de numeracion | Numeracion | DDL `IdTalonario INT` | — |
| `tipo_firma_req` | `SMALLINT` | NO | — | Tipo de firma requerida | Determina flujo de firma | Firma | DDL `TipoFirmaReq SMALLINT` | Confirmar catalogo de tipos de firma |
| `storage_key` | `VARCHAR(255)` | SI | IDX | Referencia al archivo en storage externo | Vinculo con storage documental | Storage, descarga | DDL `StorageKey VARCHAR(255)` | NULL hasta que se almacena; no binario en BD |
| `hash_docu` | `VARCHAR(128)` | SI | — | Hash de integridad del archivo | Verificacion de integridad | Firma, auditoria | DDL `HashDocu VARCHAR(128)` | — |
| `fh_generacion` | `DATETIME(6)` | SI | IDX | Fecha de generacion del documento | Ordenamiento, plazos | Timeline, bandeja | DDL `FhGeneracion` | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta del registro | Auditoria | Auditoria | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | Auditoria | Auditoria | DDL | — |

### 3.3 Tabla `fal_acta_documento`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK compuesta + FK+IDX | Acta que contiene el documento | DDL | — |
| `documento_id` | `BIGINT` | NO | PK compuesta + FK | Documento asociado al acta | DDL | — |
| `rol_docu_acta` | `SMALLINT` | NO | IDX | Rol funcional del documento en el acta | DDL `RolDocuActa SMALLINT` (7 vals) | Alinear valores con tipos de documento |
| `si_principal` | `BOOLEAN` | NO | — | Flag: es el documento principal del acta | DDL `SiPrincipal SMALLINT` | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de asociacion | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que realizo la asociacion | DDL | — |

### 3.4 Tabla `fal_documento_firma`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del registro de firma | DDL `FalDocumentoFirma.Id` | — |
| `documento_id` | `BIGINT` | NO | FK+IDX | Documento que se firma | DDL `IdDocu INT8` | — |
| `tipo_firma` | `SMALLINT` | NO | — | Tipo de firma aplicada | DDL `TipoFirma SMALLINT` | Confirmar catalogo |
| `estado_firma` | `SMALLINT` | NO | IDX | Estado del proceso de firma | DDL `EstadoFirma SMALLINT` | — |
| `id_user_firma` | `CHAR(36)` | SI | — | Usuario firmante | DDL | — |
| `fh_solicitud` | `DATETIME(6)` | SI | — | Fecha de solicitud de firma | DDL | — |
| `fh_firma` | `DATETIME(6)` | SI | — | Fecha efectiva de firma | DDL | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que inicio el proceso | DDL | — |

---

## 4. Notificaciones

### 4.1 Clasificacion de tablas de notificacion

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_notificacion` | Proceso de notificacion al infractor | Principal | Obligatoria primera version | spec + DDL | Casi segura — agregar campos faltantes |
| `fal_notificacion_intento` | Intento individual de notificacion por canal | Hija | Obligatoria primera version | spec + DDL | Casi segura — agregar lote_id |
| `fal_notificacion_acuse` | Acuse de recibo de notificacion | Hija | Obligatoria primera version | spec + DDL | Casi segura |
| `fal_notificacion_observacion` | Observaciones sobre notificaciones | Hija | Probable | DDL | Casi segura |
| `fal_lote_correo` | Agrupacion de envios postales | Integracion | Baja (diferir) | Prototipo | Requiere veredicto — rec: diferir |

### 4.2 Tabla `fal_notificacion`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de la notificacion | — | DDL `FalNotificacion.IdNotif INT8` | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta a la que pertenece | — | DDL `IdActa INT8` | — |
| `documento_id` | `BIGINT` | SI | FK+IDX | Documento que se notifica | Notificacion es sobre un documento | DDL `IdDocu INT8` | — |
| `tipo_notif` | `SMALLINT` | NO | IDX | Tipo de notificacion (acta, acto, medida preventiva, otra) | Determina efectos al confirmar | DDL `TipoNotif SMALLINT` (4 vals) | — |
| `estado_notif` | `SMALLINT` | NO | IDX | Estado actual de la notificacion | — | DDL `EstadoNotif SMALLINT` (9 vals) | Alinear con estados del prototipo |
| `canal_notif` | `SMALLINT` | NO | IDX | Canal de notificacion principal | Determina proceso de intento | DDL `CanalNotif SMALLINT` (7 vals) | Confirmar si PORTAL_CIUDADANO = PORTAL_INFRACTOR |
| `si_requiere_acuse` | `BOOLEAN` | NO | — | Flag: requiere acuse de recibo | — | DDL `SiRequiereAcuse SMALLINT` | — |
| `si_acuse_recibido` | `BOOLEAN` | NO | IDX | Flag: acuse ya recibido | — | DDL `SiAcuseRecibido SMALLINT` | — |
| `estado_acuse` | `SMALLINT` | SI | — | Estado del acuse de recibo | — | DDL `EstadoAcuse SMALLINT` | — |
| `fh_acuse` | `DATETIME(6)` | SI | — | Fecha del acuse | — | DDL `FhAcuse` | — |
| `destinatario_nombre` | `VARCHAR(120)` | SI | — | Nombre del destinatario | — | Prototipo | — |
| `domicilio_texto` | `VARCHAR(255)` | SI | — | Domicilio en texto libre | — | Prototipo | — |
| `fh_generacion` | `DATETIME(6)` | NO | IDX | Fecha de generacion de la notificacion | — | DDL `FhGeneracion` | — |
| `fh_emision` | `DATETIME(6)` | SI | IDX | Fecha de emision efectiva | — | DDL `FhEmision` | — |
| `referencia_externa` | `VARCHAR(120)` | SI | — | Referencia externa (ej. numero de correo) | Trazabilidad postal/municipal | Nuevo — gap | — |
| `lote_id` | `VARCHAR(50)` | SI | IDX | ID del lote de correo postal (si aplica) | Trazabilidad de lote postal | Prototipo — gap en DDL | Si se decide tabla lote_correo, cambia a FK |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta del registro | — | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que genero la notificacion | — | DDL | — |

### 4.3 Tabla `fal_notificacion_intento`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del intento | DDL `FalNotificacionIntento.Id` | — |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | Notificacion a la que pertenece | DDL `IdNotif INT8` | — |
| `nro_intento` | `SMALLINT` | NO | — | Numero de intento (1, 2, 3...) | DDL `NroIntento SMALLINT` | — |
| `canal_notif` | `SMALLINT` | NO | IDX | Canal utilizado en este intento | DDL `CanalNotif SMALLINT` | — |
| `tipo_dest_notif` | `SMALLINT` | SI | — | Tipo de destinatario | DDL `TipoDestNotif SMALLINT` | Confirmar catalogo |
| `dest_notif` | `VARCHAR(255)` | SI | — | Direccion/email/domicilio de destino | DDL `DestNotif VARCHAR(255)` | — |
| `estado_intento` | `SMALLINT` | NO | IDX | Estado del intento (7 valores) | DDL `EstadoIntento SMALLINT` | — |
| `fh_intento` | `DATETIME(6)` | NO | IDX | Fecha del intento | DDL `FhIntento` | — |
| `fh_resultado` | `DATETIME(6)` | SI | — | Fecha del resultado del intento | DDL `FhResultado` | — |
| `resultado_intento` | `SMALLINT` | SI | IDX | Resultado del intento | DDL `ResultadoIntento SMALLINT` | — |
| `referencia_externa` | `VARCHAR(120)` | SI | — | Referencia en sistema externo de envio | Nuevo — gap | — |
| `lote_id` | `VARCHAR(50)` | SI | IDX | ID del lote postal asociado | Prototipo — gap | Si se decide tabla lote_correo, cambia a FK |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que registro el intento | DDL | — |

### 4.4 Tabla `fal_notificacion_acuse`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del acuse | DDL `FalNotificacionAcuse.Id` | — |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | Notificacion que se acusa | DDL `IdNotif INT8` | — |
| `intento_id` | `BIGINT` | SI | FK | Intento especifico asociado | DDL `IdIntentoNotif INT8` | — |
| `tipo_acuse` | `SMALLINT` | NO | — | Tipo de acuse (6 valores) | DDL `TipoAcuse SMALLINT` | — |
| `estado_acuse` | `SMALLINT` | NO | IDX | Estado del acuse (5 valores) | DDL `EstadoAcuse SMALLINT` | — |
| `fh_acuse` | `DATETIME(6)` | NO | IDX | Fecha del acuse | DDL `FhAcuse` | — |
| `storage_key_acuse` | `VARCHAR(255)` | SI | — | Documento de constancia del acuse | DDL `StorageKeyAcuse VARCHAR(255)` | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | DDL | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que registro el acuse | DDL | — |

### 4.5 Tabla `fal_lote_correo` — solo si se decide no diferir

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del lote | Prototipo | — |
| `lote_codigo` | `VARCHAR(50)` | NO | UNIQUE | Codigo unico del lote (referenciado en notificaciones) | Prototipo | Confirmar si es UUID o secuencial |
| `fecha_generacion` | `DATETIME(6)` | NO | IDX | Fecha de generacion del lote | Prototipo | — |
| `estado` | `SMALLINT` | NO | IDX | Estado del lote (ACTIVO=1, ANULADO=2) | Prototipo | — |
| `referencia_externa` | `VARCHAR(120)` | SI | — | Referencia en sistema postal externo | Prototipo | — |
| `cantidad_items` | `SMALLINT` | NO | — | Cantidad de notificaciones en el lote | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta del lote | Prototipo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que genero el lote | Prototipo | — |

**Estado de decision:** Diferir. Agregar solo `lote_id VARCHAR(50)` en `fal_notificacion_intento` como placeholder. Crear tabla completa cuando haya integracion postal real definida.

---

## 5. Fallo y apelacion

### 5.1 Clasificacion de tablas de fallo y apelacion

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_fallo` | Decision juridica (absolutoria/condenatoria) con monto | Hija | Alta | Prototipo + DDL parcial | Requiere veredicto (ver decision 8.2) |
| `fal_acta_apelacion` | Apelacion presentada por infractor con resultado | Hija | Media | Prototipo + DDL parcial | Requiere veredicto (ver decision 8.4) |

### 5.2 Tabla `fal_acta_fallo` — propuesta si se decide entidad separada

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del fallo | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta a la que pertenece | 1:N posible (fallo post gestion externa) | Prototipo | — |
| `tipo_fallo` | `SMALLINT` | NO | IDX | Tipo de fallo (ABSOLUTORIO=1, CONDENATORIO=2) | Determina circuito siguiente | Prototipo | — |
| `monto_condena` | `DECIMAL(14,2)` | SI | — | Monto de condena (solo si condenatorio) | Fijado al dictar fallo condenatorio | Prototipo | NULL si absolutorio |
| `fecha_dictado` | `DATETIME(6)` | NO | IDX | Fecha y hora del dictado del fallo | — | Prototipo | — |
| `id_user_dictado` | `CHAR(36)` | NO | — | Operador que dicto el fallo | — | Prototipo | — |
| `documento_id` | `BIGINT` | SI | FK | Documento del fallo (ACTO_ADMINISTRATIVO tipo 4) | Vinculo con circuito documental | DDL parcial — TipoDocu=4 | — |
| `fh_vto_apelacion` | `DATE` | SI | IDX | Fecha de vencimiento del plazo de apelacion | Solo si condenatorio; 5 dias post notificacion positiva | Prototipo + DDL snapshot | Confirmar si vive aqui o en snapshot |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que registro el fallo | — | Nuevo | — |

**Estado de decision:** Requiere veredicto. Opciones:
- A: Tabla propia `fal_acta_fallo` (esta propuesta)
- B: Documento enriquecido `fal_documento` tipo 4 + evento tipo 12
- C: Hibrido: `fal_documento` como pieza firmable + `fal_acta_fallo` para metadatos juridicos

Recomendacion de este documento: Opcion C. El documento va al circuito de firma; la entidad fallo captura el monto y el resultado juridico.

### 5.3 Tabla `fal_acta_apelacion` — propuesta si se decide entidad separada

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de la apelacion | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta apelada | 1 acta : 0..1 apelacion activa | Prototipo | — |
| `fallo_id` | `BIGINT` | SI | FK | Fallo que se apela | Vinculo con la decision apelada | Prototipo | NULL si no hay tabla fallo separada |
| `canal` | `SMALLINT` | NO | — | Canal de registro (PRESENCIAL=1, PORTAL_INFRACTOR=2) | — | Prototipo | — |
| `fecha_registro` | `DATETIME(6)` | NO | IDX | Fecha de registro de la apelacion | — | Prototipo | — |
| `estado` | `SMALLINT` | NO | IDX | Estado (PRESENTADA=1, RESUELTA=2) | — | Prototipo | — |
| `resultado_resolucion` | `SMALLINT` | SI | — | Resultado (RECHAZADA=1, ACEPTADA_ABSUELVE=2) | Si ACEPTADA_ABSUELVE: resultado_final=ABSUELTO | Prototipo | NULL si aun no resuelta |
| `fecha_resolucion` | `DATETIME(6)` | SI | — | Fecha de resolucion de la apelacion | — | Prototipo | — |
| `id_user_resolucion` | `CHAR(36)` | SI | — | Operador que resolvio | — | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que registro la apelacion | — | Nuevo | — |

**Estado de decision:** Requiere veredicto. Opciones:
- A: Tabla propia `fal_acta_apelacion` (esta propuesta)
- B: Solo evento enriquecido `TipoEvt=13` + payload JSON

Recomendacion de este documento: Opcion A. La apelacion tiene vida propia con resultado y canal; merece entidad.

---

## 6. Pagos

### 6.1 Clasificacion de tablas de pagos

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_pago_voluntario` | Circuito de pago voluntario previo a fallo | Hija | Alta | Prototipo validado | Requiere veredicto — rec: tabla propia |
| `fal_acta_pago_condena` | Pago de condena firme post fallo | Hija | Alta | Prototipo validado | Casi obligatoria — rec: tabla separada |

### 6.2 Tabla `fal_acta_pago_voluntario`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del pago voluntario | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta a la que pertenece | 1 acta : 0..1 pago voluntario activo | Prototipo | Confirmar si puede haber historial de intentos como registros distintos |
| `situacion_pago` | `SMALLINT` | NO | IDX | Estado del pago voluntario | SIN_PAGO/SOLICITADO/PAGO_INFORMADO/PEND_CONFIRMACION/CONFIRMADO/OBSERVADO/VENCIDO | Prototipo | Alinear con EstPagoAct del snapshot |
| `monto_pago_voluntario` | `DECIMAL(14,2)` | SI | — | Monto habilitado por el operador | Fijado por operador; NULL hasta que se fija | Prototipo | — |
| `fecha_solicitud` | `DATETIME(6)` | SI | IDX | Fecha de solicitud del pago voluntario | — | Prototipo | — |
| `fecha_fijacion_monto` | `DATETIME(6)` | SI | — | Fecha en que el operador fijo el monto | — | Prototipo | — |
| `fecha_pago_informado` | `DATETIME(6)` | SI | — | Fecha en que el infractor informo el pago | — | Prototipo | — |
| `comprobante_ref` | `VARCHAR(100)` | SI | — | Referencia del comprobante de pago (placeholder EM/RC/Nro) | Evidencia del pago | Prototipo | Modelo exacto de comprobante (EM, RC, Cmte/Pref/Nro) pendiente |
| `tipo_comprobante` | `SMALLINT` | SI | — | Tipo de comprobante | — | Prototipo | Pendiente de cruce con spec de ingresos |
| `fecha_confirmacion` | `DATETIME(6)` | SI | — | Fecha de confirmacion del pago | — | Prototipo | — |
| `fecha_vencimiento` | `DATE` | SI | IDX | Fecha de vencimiento del plazo de pago | Vencido sin pago -> situacion_pago=VENCIDO | Prototipo | — |
| `observacion` | `TEXT` | SI | — | Observacion del operador | — | Prototipo | — |
| `id_user_solicitud` | `CHAR(36)` | SI | — | Actor que solicito el pago | — | Prototipo | — |
| `canal_solicitud` | `SMALLINT` | SI | — | Canal de la solicitud (INTERNO, PORTAL) | — | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | — | Nuevo | — |

**Estado de decision:** Requiere veredicto. Recomendacion: tabla propia (Opcion B del cruce decisional). Cubre el circuito completo con historial de intentos. Los flags del snapshot siguen siendo utiles para bandejas; se derivan de esta tabla.

### 6.3 Tabla `fal_acta_pago_condena`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del pago condena | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta a la que pertenece | Solo aplica con CONDENA_FIRME | Prototipo | — |
| `fallo_id` | `BIGINT` | SI | FK | Fallo cuya condena se paga | Vinculo con la condena | Prototipo | NULL si no hay tabla fallo |
| `situacion_pago_condena` | `SMALLINT` | NO | IDX | Estado del pago de condena | NO_APLICA/PENDIENTE/INFORMADO/CONFIRMADO/OBSERVADO | Prototipo | — |
| `monto_condena` | `DECIMAL(14,2)` | SI | — | Monto de la condena a pagar | Fijado al dictar fallo condenatorio | Prototipo | — |
| `tipo_pago` | `SMALLINT` | SI | — | Tipo de pago (NO_APLICA/VOLUNTARIO/CONDENA) | — | Prototipo | — |
| `fecha_informe` | `DATETIME(6)` | SI | — | Fecha en que el infractor informo el pago | — | Prototipo | — |
| `comprobante_ref` | `VARCHAR(100)` | SI | — | Referencia del comprobante de pago condena | Evidencia del pago | Prototipo | Pendiente de cruce con spec de ingresos |
| `tipo_comprobante` | `SMALLINT` | SI | — | Tipo de comprobante | — | Prototipo | — |
| `fecha_confirmacion` | `DATETIME(6)` | SI | — | Fecha de confirmacion del pago | Habilita cierre del expediente | Prototipo | — |
| `observacion` | `TEXT` | SI | — | Observacion del operador | — | Prototipo | — |
| `consentimiento_registrado` | `BOOLEAN` | NO | — | Flag: infractor consintio la condena (portal) | Consentimiento + pago pueden ser un solo acto | Prototipo | — |
| `canal_informado` | `SMALLINT` | SI | — | Canal del informe (INTERNO, PORTAL) | — | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | — | Nuevo | — |

**Estado de decision:** Casi obligatoria. La separacion de pago voluntario y pago condena es una regla del dominio, no una preferencia de implementacion. El circuito de condena solo aplica con `CONDENA_FIRME`.

---

## 7. Bloqueantes

### 7.1 Clasificacion de tablas de bloqueantes

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_bloqueante_cierre_material` | Los tres origenes de bloqueo con sus dos pasos | Hija | Alta | Prototipo + spec medidas | Requiere veredicto — rec: tabla propia |

### 7.2 Tabla `fal_acta_bloqueante_cierre_material`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del bloqueante | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta bloqueada | 1 acta : N bloqueantes (hasta 3 origenes) | Prototipo | — |
| `origen` | `SMALLINT` | NO | IDX | Origen del bloqueo: MEDIDA_PREVENTIVA_ACTIVA=1, RODADO_SECUESTRADO=2, DOCUMENTACION_RETENIDA=3 | Tres origenes distintos con ancla documental | Prototipo | — |
| `estado_resolucion_documental` | `SMALLINT` | NO | IDX | Estado documental: PENDIENTE=1, RESUELTO=2 | Primer paso: emision y firma del resolutorio | Prototipo | — |
| `estado_cumplimiento_material` | `SMALLINT` | NO | IDX | Estado material: PENDIENTE=1, CUMPLIDO=2 | Segundo paso: cumplimiento fisico efectivo | Prototipo | — |
| `id_documento_resolvent` | `BIGINT` | SI | FK | Documento resolutorio asociado | Ancla documental del bloqueo | Prototipo | NULL hasta que se emite el resolutorio |
| `fecha_constatacion` | `DATETIME(6)` | NO | — | Fecha en que se constato el hecho bloqueante | — | Prototipo | — |
| `fecha_resolucion_documental` | `DATETIME(6)` | SI | — | Fecha de resolucion documental | — | Prototipo | — |
| `fecha_cumplimiento_material` | `DATETIME(6)` | SI | — | Fecha de cumplimiento material efectivo | — | Prototipo | — |
| `observacion` | `TEXT` | SI | — | Descripcion del hecho bloqueante | — | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que registro el bloqueante | — | Nuevo | — |

**Regla critica:** Resolucion documental y cumplimiento material son pasos distintos. El cierre requiere ambos para cada bloqueante activo. Los flags en `fal_acta_snapshot` (`si_bloqueante_med_prev`, `si_bloqueante_rodado`, `si_bloqueante_doc_ret`) deben derivarse de esta tabla.

---

## 8. Archivo, cierre y paralizacion

### 8.1 Clasificacion de tablas de archivo/cierre/paralizacion

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_paralizacion` | Historial de paralizaciones con motivo | Historica | Alta | Prototipo + gap critico D10 | Requiere veredicto — rec: tabla historica |
| `fal_acta_archivo` | Historial de archivos con causales estructuradas | Historica | Media | Prototipo + gap critico D11 | Requiere veredicto |

**Nota:** El cierre explícito se modela via campo `esta_cerrada` + evento en `fal_acta_evento`. No se propone tabla separada de cierre.

### 8.2 Tabla `fal_acta_paralizacion`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de la paralizacion | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta paralizada | 1 acta : N paralizaciones historicas | Prototipo | — |
| `motivo_paralizacion` | `SMALLINT` | NO | IDX | Motivo: ESPERA_DOCUMENTAL=1, ESPERA_INFORME_EXTERNO=2, ESPERA_OTRA_DEPENDENCIA=3, ESPERA_RESOLUCION_RELACIONADA=4, OTRO=5 | Gap critico D10: snapshot necesita este motivo | Prototipo | Confirmar si hay mas valores en spec |
| `observacion` | `TEXT` | SI | — | Texto libre de observacion de la paralizacion | Hoy volatil en prototipo; debe persistirse | Prototipo | — |
| `fecha_paralizacion` | `DATETIME(6)` | NO | IDX | Fecha de la paralizacion | — | Prototipo | — |
| `bandeja_previa` | `VARCHAR(50)` | SI | — | Codigo de bandeja antes de paralizar | Para reactivacion con contexto | Prototipo | Confirmar si se necesita o alcanza con evento |
| `accion_previa_preservada` | `VARCHAR(100)` | SI | — | Marca de accion pendiente al moment de paralizar | Preserva el contexto de trabajo | Prototipo | — |
| `fecha_reactivacion` | `DATETIME(6)` | SI | — | Fecha de reactivacion | NULL si aun paralizada | Prototipo | — |
| `id_user_paralizacion` | `CHAR(36)` | NO | — | Operador que paralizo | — | Nuevo | — |
| `id_user_reactivacion` | `CHAR(36)` | SI | — | Operador que reactivo | — | Nuevo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta del registro | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | — | Nuevo | — |

**Regla del snapshot:** El campo `motivo_paralizacion_act` en `fal_acta_snapshot` se deriva del registro activo (sin `fecha_reactivacion`) de esta tabla. Es NULL si no hay paralizacion activa.

### 8.3 Tabla `fal_acta_archivo` — propuesta si se decide tabla historica

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del evento de archivo | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta archivada | 1 acta : N eventos de archivo (reingreso posible) | Prototipo | — |
| `motivo_archivo` | `SMALLINT` | NO | IDX | Motivo del archivo (enum a alinear DDL vs prototipo) | Distingue origen del archivo | Prototipo + DDL parcial | Valores DDL: ARCH_ADMIN=1, CIERRE_PAGO=2, CIERRE_RESOLUCION=3, etc. vs prototipo: ANALISIS_DIRECTO, POST_VENCIMIENTO, NULIDAD — alinear |
| `permite_reingreso` | `BOOLEAN` | NO | — | Indica si este archivo permite reingreso | Nulidad siempre permite; otros segun regla | Prototipo | — |
| `causal_bloqueante_med_prev` | `BOOLEAN` | NO | — | Flag: medida preventiva activa bloquea cierre | Gap critico D11 | Prototipo | — |
| `causal_bloqueante_rodado` | `BOOLEAN` | NO | — | Flag: rodado secuestrado bloquea cierre | Gap critico D11 | Prototipo | — |
| `causal_bloqueante_doc_ret` | `BOOLEAN` | NO | — | Flag: documentacion retenida bloquea cierre | Gap critico D11 | Prototipo | — |
| `fecha_archivo` | `DATETIME(6)` | NO | IDX | Fecha del archivo | — | Prototipo | — |
| `fecha_reingreso` | `DATETIME(6)` | SI | — | Fecha de reingreso desde archivo | NULL si no reingresado | Prototipo | — |
| `observacion` | `TEXT` | SI | — | Observacion sobre el archivo | — | Prototipo | — |
| `id_user_archivo` | `CHAR(36)` | NO | — | Operador que archivo | — | Nuevo | — |
| `id_user_reingreso` | `CHAR(36)` | SI | — | Operador que reingrepo | — | Nuevo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | — | Nuevo | — |

**Estado de decision:** Requiere veredicto. Opciones:
- A: Campos en `fal_acta` (solo cubre ultimo archivo, sin historial)
- B: Tabla historica `fal_acta_archivo` (esta propuesta, cubre N archivos con reingreso)
- C: Evento enriquecido en `fal_acta_evento` + campos en acta para estado actual

Recomendacion: Opcion B + campos basicos en `fal_acta` para estado actual (`motivo_archivo`, `permite_reingreso`, `esta_cerrada`).

---

## 9. Gestion externa

### 9.1 Clasificacion de tablas de gestion externa

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_gestion_externa` | Derivacion con resultado y trazabilidad de N gestiones | Hija | Media | Prototipo + spec servicio | Requiere veredicto — rec: tabla propia |

### 9.2 Tabla `fal_acta_gestion_externa`

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Regla de dominio | Origen | Duda pendiente |
|---|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de la gestion externa | — | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta derivada | 1 acta : N gestiones historicas | Prototipo | — |
| `tipo` | `SMALLINT` | NO | IDX | Tipo: APREMIO=1, JUZGADO_DE_PAZ=2 | Determina procesos de reingreso | Prototipo | — |
| `fecha_derivacion` | `DATETIME(6)` | NO | IDX | Fecha de la derivacion formal | — | Prototipo | — |
| `estado` | `SMALLINT` | NO | IDX | Estado: ACTIVA=1, CERRADA=2 | — | Prototipo | — |
| `resultado_externo` | `SMALLINT` | SI | — | Resultado: MODIFICA_MONTO=1, ABSUELVE=2 | Puede modificar monto condena | Prototipo | NULL si aun activa |
| `monto_sugerido_post_gestion` | `DECIMAL(14,2)` | SI | — | Monto sugerido por la gestion externa | Solo si resultado=MODIFICA_MONTO | Prototipo | — |
| `fecha_reingreso` | `DATETIME(6)` | SI | — | Fecha de reingreso desde gestion externa | — | Prototipo | — |
| `accion_post_reingreso` | `VARCHAR(100)` | SI | — | Marca de accion al reingresar | Discrimina las variantes de reingreso | Prototipo | Confirmar si es SMALLINT catalogo o texto |
| `observacion` | `TEXT` | SI | — | Observacion sobre la gestion | — | Prototipo | — |
| `id_user_derivacion` | `CHAR(36)` | NO | — | Operador que realizo la derivacion | — | Nuevo | — |
| `id_user_reingreso` | `CHAR(36)` | SI | — | Operador que registro el reingreso | — | Nuevo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | — | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que creo el registro | — | Nuevo | — |

**Estado de decision:** Requiere veredicto. Recomendacion: tabla propia. El historial de N derivaciones es operativamente necesario. Los flags del snapshot se siguen usando para la vista de bandeja D9.

---

## 10. Portal y QR

### 10.1 Clasificacion de tablas de portal/QR

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_acta_qr_acceso` | Acceso via QR al portal del infractor | Integracion | Baja (diferir) | Prototipo | Diferible — mecanismo QR pendiente de diseno |

### 10.2 Tabla `fal_acta_qr_acceso` — solo si se decide no diferir

| Campo sugerido | Tipo MySQL sugerido | Nullable | Clave/indice | Descripcion funcional | Origen | Duda pendiente |
|---|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador | Prototipo | — |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta accesible via QR | Prototipo | — |
| `codigo_qr` | `VARCHAR(64)` | NO | UNIQUE+IDX | Codigo QR que resuelve al actaId | Prototipo | Confirmar longitud y mecanismo de generacion |
| `estado` | `SMALLINT` | NO | — | Estado: ACTIVO=1, EXPIRADO=2 | Prototipo | — |
| `fecha_generacion` | `DATETIME(6)` | NO | — | Fecha de generacion del QR | Prototipo | — |
| `fecha_primer_acceso` | `DATETIME(6)` | SI | — | Fecha del primer acceso via QR | Prototipo | — |
| `conteo_accesos` | `INT` | NO | — | Cantidad de accesos | Prototipo | — |
| `fh_alta` | `DATETIME(6)` | NO | — | Fecha de alta | Nuevo | — |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario que genero el QR | Nuevo | — |

**Estado de decision:** Diferir. Para MVP usar `codigo_qr VARCHAR(64)` como campo en `fal_acta` o en `fal_documento` con indice UNIQUE. Crear tabla de acceso solo si se necesita trazabilidad de accesos individuales.

---

## 11. Catalogos y referenciales

### 11.1 Clasificacion de tablas referenciales

| Tabla | Responsabilidad | Tipo | Prioridad | Fuente | Estado decision |
|---|---|---|---|---|---|
| `fal_dependencia` | Dependencia organizacional (raiz) | Catalogo | Obligatoria | DDL | Casi segura |
| `fal_dependencia_version` | Version historica de dependencia | Historica | Obligatoria | DDL | Casi segura |
| `fal_inspector` | Inspector actor del labrado (raiz) | Catalogo | Obligatoria | DDL | Casi segura |
| `fal_inspector_version` | Version historica de inspector | Historica | Obligatoria | DDL | Casi segura |
| `fal_medida_preventiva` | Referencial de tipos de medida preventiva | Catalogo | Probable | DDL | Casi segura |
| `fal_normativa_faltas` | Normativa de faltas vigente | Catalogo | Probable | DDL | Casi segura |
| `fal_articulo_normativa_faltas` | Articulos de la normativa | Catalogo | Probable | DDL | Casi segura |
| `fal_tarifario_unidad_faltas` | Tarifario de unidades de multa | Catalogo | Probable | DDL | Casi segura |
| `num_politica` | Politica de numeracion de talonarios | Catalogo | Obligatoria | DDL | Casi segura |
| `num_talonario` | Talonario de numeracion | Catalogo | Obligatoria | DDL | Casi segura |
| `num_talonario_dependencia` | Asignacion talonario-dependencia | Hija | Obligatoria | DDL | Casi segura |
| `num_talonario_inspector` | Asignacion talonario-inspector | Hija | Obligatoria | DDL | Casi segura |
| `num_talonario_movimiento` | Movimientos de uso de talonario | Historica | Obligatoria | DDL | Casi segura |
| `stor_backend` | Backend de storage documental | Catalogo | Obligatoria | DDL | Casi segura |
| `stor_politica` | Politica de storage por tipo de objeto | Catalogo | Obligatoria | DDL | Casi segura |
| `stor_objeto` | Objeto fisico almacenado en storage | Integracion | Obligatoria | DDL | Casi segura |

### 11.2 Campos clave de tablas referenciales ya en DDL

**`fal_dependencia`:** `id BIGINT AUTO_INCREMENT`, `cod_dep VARCHAR(20)`, `nom_dep VARCHAR(120)`, `id_dep_padre BIGINT`, `si_activa BOOLEAN`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`

**`fal_dependencia_version`:** PK compuesta `(id_dep, ver_dep)`, `nom_dep VARCHAR(120)`, `id_dep_padre BIGINT`, `ver_dep_padre SMALLINT`, `fh_vig_desde DATE`, `fh_vig_hasta DATE`, `si_activa BOOLEAN`

**`fal_inspector`:** `id BIGINT AUTO_INCREMENT`, `id_user BIGINT`, `legajo_insp INT`, `nom_insp VARCHAR(120)`, `si_activo BOOLEAN`

**`fal_inspector_version`:** PK compuesta `(id_insp, ver_insp)`, `legajo_insp INT`, `nom_insp VARCHAR(120)`, `id_dep BIGINT`, `ver_dep SMALLINT`, `fh_vig_desde DATE`, `fh_vig_hasta DATE`, `si_activo BOOLEAN`

**`stor_objeto`:** PK natural `storage_key VARCHAR(64)`, `id_storage_backend INT`, `sistema VARCHAR(20)`, `familia VARCHAR(30)`, `tipo_objeto VARCHAR(30)`, `anio SMALLINT`, `mes SMALLINT`, `bucket VARCHAR(60)`, `ref_negocio VARCHAR(80)`, `nom_archivo VARCHAR(120)`, `ext_archivo VARCHAR(10)`, `mime_type VARCHAR(80)`, `tam_bytes BIGINT`, `hash_archivo VARCHAR(128)`, `ruta_relativa VARCHAR(512)`, `estado_storage SMALLINT`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`. **Nota:** esta tabla no usa secuencia; `storage_key` es PK natural VARCHAR — portable directamente a MySQL.

**`fal_observacion`:** `id BIGINT AUTO_INCREMENT`, `id_ref SMALLINT`, `id_fk BIGINT`, `obs VARCHAR(255)`, `fh_alta DATETIME(6)`, `id_user CHAR(36)`. Observaciones polimorficas via `id_ref` (discriminador de entidad) + `id_fk` (FK logica).

---

## 12. Definicion detallada: `fal_acta`

### 12.1 Proposito

`fal_acta` es el agregado raiz del sistema. Representa el expediente de faltas. Es la fuente de verdad primaria junto con `fal_acta_evento`. Todos los demas datos del expediente se derivan desde ella.

### 12.2 Grupos de campos propuestos

**Bloque identidad:**
- `id`, `id_tecnico`, `nro_acta`, `tipo_acta`, `origen_captura`

**Bloque temporal:**
- `fh_acta`, `fh_alta`, `fh_ult_mod`, `fh_cierre`, `fh_archivo`

**Bloque organizacional:**
- `id_dep`, `ver_dep`, `id_insp`, `ver_insp`

**Bloque infractor:**
- `nom_infct`, `doc_pref_infct`, `doc_nro_infct`, `tipo_pers_infct`
- Domicilio infractor (~15 campos de referencias geograficas + textos libres)
- `email_infct VARCHAR(120)` (nuevo — no en DDL; para notificacion electronica)

**Bloque domicilio infraccion:**
- Referencias geograficas del lugar del hecho + `dom_txt_infr`, `si_eje_urb`
- `lat_infr DECIMAL(12,8)`, `lon_infr DECIMAL(12,8)`

**Bloque licencia (actas de transito):**
- `id_prov_lic_emi`, `id_muni_lic_emi`, `id_dpto_lic_emi`, `tipo_jur_lic_emi`

**Bloque contenido:**
- `resumen_hecho TEXT`

**Bloque estado:**
- `bloque_actual SMALLINT`, `est_proc_act SMALLINT`, `sit_adm_act SMALLINT`
- `resultado_final SMALLINT`
- `esta_cerrada BOOLEAN`

**Bloque archivo:**
- `motivo_archivo SMALLINT`, `permite_reingreso BOOLEAN`

**Bloque gestion externa actual:**
- `id_tipo_gestion_ext_act SMALLINT`

**Bloque auditoria:**
- `id_user_alta CHAR(36)`, `id_user_ult_mod CHAR(36)`, `fh_ult_mod DATETIME(6)`

### 12.3 Que NO debe persistirse en `fal_acta`

| Concepto | Donde debe vivir |
|---|---|
| `bandeja_actual` | `fal_acta_snapshot.cod_bandeja` (cache) |
| `accion_pendiente` | `fal_acta_snapshot.accion_pendiente` (cache derivado) |
| `motivo_paralizacion` | `fal_acta_paralizacion` (tabla historica) + `fal_acta_snapshot.motivo_paralizacion_act` (cache) |
| `situacion_pago` | `fal_acta_pago_voluntario.situacion_pago` |
| `situacion_pago_condena` | `fal_acta_pago_condena.situacion_pago_condena` |
| `monto_condena` | `fal_acta_fallo.monto_condena` o campo en acta (decision pendiente) |
| `cerrabilidad` | Calculada por servicio de dominio; nunca persistida |
| Conteos de bandeja | Calculados desde snapshot en tiempo de consulta |

---

## 13. Definicion detallada: `fal_acta_evento`

### 13.1 Principio append-only

Ningun registro de `fal_acta_evento` puede editarse, actualizarse ni borrarse una vez insertado. Esta tabla es el registro inmutable de la historia del expediente.

Los pasos de cada transaccion de escritura (patron SQL/08):
1. INSERT en `fal_acta_evento` (append)
2. UPDATE/INSERT en `fal_acta_snapshot` (actualizacion incremental)
3. INSERT opcional en `fal_observacion`

### 13.2 Campos que reconstruyen el snapshot

Los campos de `fal_acta_evento` que permiten reconstruir `fal_acta_snapshot` desde cero:
- `bloque_func` → `bloque_actual` en snapshot
- `est_proc_nvo` → `est_proc_act` en snapshot
- `sit_adm_nva` → `sit_adm_act` en snapshot
- `tipo_evt` + `payload_json` → flags de pago, notificacion, gestion externa en snapshot
- `si_evt_cierre` → `esta_cerrada` en acta
- `si_permite_reing` → `permite_reingreso` en acta
- `id_docu_rel` → puntero `id_docu_ult` en snapshot
- `id_notif_rel` → puntero `id_notif_ult` en snapshot

### 13.3 Gap 19 DDL vs 62 eventos prototipo

El DDL Informix tiene 19 valores de `TipoEvt` (0-18). El prototipo valida 62 eventos distintos. Hay ~43 eventos sin equivalente en el DDL.

**Tres estrategias posibles (ver decision 14.1):**

| Estrategia | Descripcion | Ventaja | Riesgo |
|---|---|---|---|
| A: Ampliar enum | Agregar valores hasta 62+ en SMALLINT | Simple; sin cambio de modelo | SMALLINT grande; mas dificil de mantener |
| B: Catalogo de tabla | Crear `fal_tipo_evento` con id, codigo, descripcion | Extensible sin DDL; FK referencial | Joins adicionales |
| C: Hibrido tipo+payload | Mantener tipos genericos existentes + `payload_json JSON` para contexto especifico | Menor cantidad de tipos; payload flexible | Consultas sobre payload complejas |

**Esta decision debe tomarse antes de crear el DDL de `fal_acta_evento`.**

### 13.4 Eventos cubiertos exactamente por el DDL (7 de 62)

| Evento prototipo | TipoEvt DDL |
|---|---|
| `ACTA_LABRADA` | 1 = ACTA_LABRADA |
| `ACTA_ARCHIVADA` | 15 = ARCHIVO_DISPUESTO |
| `ACTA_REINGRESADA` | 16 = REINGRESO_DISPUESTO |
| `DOCUMENTO_GENERADO` | 6 = DOCUMENTO_GENERADO |
| `DOCUMENTO_FIRMADO` | 7 = DOCUMENTO_FIRMADO |
| `APELACION_REGISTRADA` | 13 = APELACION_INTERPUESTA |
| `NOTIFICACION_POSITIVA` (parcial) | 9 = NOTIFICACION_CONFIRMADA |

Los ~55 eventos restantes requieren ampliar el catalogo de TipoEvt bajo cualquiera de las 3 estrategias.

---

## 14. Definicion detallada: `fal_acta_snapshot`

### 14.1 Naturaleza del snapshot

`fal_acta_snapshot` es una proyeccion derivada. No es fuente de verdad. Es un cache regenerable.

- **Relacion:** 1:1 con `fal_acta` (una sola fila por acta)
- **Patron de actualizacion:** incremental en linea (en la misma transaccion del evento relevante)
- **Patron de rebuild:** reproceso completo desde `fal_acta` + `fal_acta_evento` + satelites
- **Idempotencia:** reprocesar multiples veces no produce degradacion

### 14.2 Campos que NO se deben editar manualmente

Todos los campos de `fal_acta_snapshot` son campos derivados. Ningun campo del snapshot debe editarse manualmente fuera del proceso de actualizacion incremental o rebuild.

### 14.3 Campos nuevos criticos (ausentes en DDL Informix)

| Campo | Por que es critico |
|---|---|
| `resultado_final` | Gap: DDL no tiene campo directo; prototipo lo usa en toda cerrabilidad |
| `motivo_paralizacion_act` | Gap critico D10: bandeja paralizadas exige este campo en el snapshot |
| `si_bloqueante_med_prev` | Gap critico D11: bandeja archivo exige ver causales bloqueantes |
| `si_bloqueante_rodado` | Gap critico D11: idem |
| `si_bloqueante_doc_ret` | Gap critico D11: idem |
| `accion_pendiente` | Marca operativa para sub-bandeja; hoy volatil en prototipo |

### 14.4 Relacion snapshot vs tablas nuevas

| Campo snapshot | Derivado de |
|---|---|
| `resultado_final` | `fal_acta.resultado_final` |
| `motivo_paralizacion_act` | Registro activo de `fal_acta_paralizacion` |
| `si_bloqueante_med_prev` | `fal_acta_bloqueante_cierre_material` donde origen=1 y estado=PENDIENTE |
| `si_bloqueante_rodado` | `fal_acta_bloqueante_cierre_material` donde origen=2 y estado=PENDIENTE |
| `si_bloqueante_doc_ret` | `fal_acta_bloqueante_cierre_material` donde origen=3 y estado=PENDIENTE |
| `est_pago_act` | `fal_acta_pago_voluntario.situacion_pago` |
| `si_pago_total` | `fal_acta_pago_condena.situacion_pago_condena = CONFIRMADO` |
| `si_gestion_ext` | Registro activo de `fal_acta_gestion_externa` |
| `tipo_gestion_ext` | `fal_acta_gestion_externa.tipo` del registro activo |
| `accion_pendiente` | Calculado desde estado + eventos + predicados (decision pendiente) |

---

## 15. Decisiones humanas pendientes

### Decision 15.1: Estrategia TipoEvt (19 DDL vs 62 eventos prototipo)

| Aspecto | Detalle |
|---|---|
| **Decision** | Que estrategia usar para el catalogo de tipos de evento |
| **Evidencia** | DDL tiene 19 valores (SMALLINT 0-18); prototipo valido 62 eventos distintos; ~43 sin equivalente |
| **Opcion A** | Ampliar SMALLINT hasta 62+ valores; agregar valores nuevos al catalogo |
| **Opcion B** | Crear tabla `fal_tipo_evento` (id, codigo VARCHAR, descripcion) como catalogo extensible |
| **Opcion C** | Mantener ~25 tipos genericos + `payload_json JSON` para contexto especifico de cada evento |
| **Recomendacion** | Opcion C: hibrido con tipos semanticamente significativos (~30-35) + payload para detalle. Equilibrio entre legibilidad y extensibilidad |
| **Impacto** | Alta: bloquea el DDL de `fal_acta_evento`; todos los repositorios de eventos dependen de esta decision |
| **Riesgo si A** | Catalogo grande dificil de mantener; pero simple de implementar |
| **Riesgo si B** | Join adicional en toda consulta de eventos |
| **Riesgo si C** | Consultas sobre `payload_json` complejas; no indexable directo |
| **Veredicto** | PENDIENTE |

### Decision 15.2: Fallo — entidad separada vs documento enriquecido vs hibrido

| Aspecto | Detalle |
|---|---|
| **Decision** | Como modelar el fallo juridico |
| **Evidencia** | DDL lo trata como TipoDocu=4 + TipoEvt=12; prototipo lo trata como decision juridica con monto y consecuencias; puede haber N fallos por acta (post gestion externa) |
| **Opcion A** | Solo documento enriquecido (fal_documento tipo 4) + evento tipo 12 |
| **Opcion B** | Tabla propia `fal_acta_fallo` con monto, tipo, fechas |
| **Opcion C** | Hibrido: `fal_documento` como pieza firmable + `fal_acta_fallo` para metadatos juridicos |
| **Recomendacion** | Opcion C: el documento va al circuito de firma y notificacion; la entidad fallo captura el monto y el resultado juridico que el documento no puede almacenar |
| **Impacto** | Alto: afecta circuito de firma, notificacion, pago condena, apelacion |
| **Riesgo si A** | El monto de condena no tiene lugar natural en `fal_documento` |
| **Riesgo si B** | Nueva entidad fuera de spec; sin documento en circuito |
| **Riesgo si C** | Mayor cantidad de tablas; sincronizacion entre documento y entidad fallo |
| **Veredicto** | PENDIENTE |

### Decision 15.3: PagoVoluntario — tabla propia vs evento+snapshot

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir el circuito de pago voluntario |
| **Evidencia** | DDL tiene solo flags en snapshot; prototipo valido circuito completo con 7 eventos y comprobantes; 1 acta : 0..1 pago activo con historial de intentos observados |
| **Opcion A** | Campos en `fal_acta` o `fal_acta_snapshot`; sin tabla propia |
| **Opcion B** | Tabla `fal_acta_pago_voluntario` con historial de intentos |
| **Opcion C** | Evento enriquecido + snapshot; sin tabla propia |
| **Recomendacion** | Opcion B: tabla propia. El comprobante, la fecha de vencimiento y el historial de intentos no tienen lugar natural en snapshot ni acta |
| **Impacto** | Alto: afecta bandeja D3, cerrabilidad, resultado final |
| **Riesgo** | Nueva entidad fuera de spec; pendiente de cruce con spec de ingresos para comprobante |
| **Veredicto** | PENDIENTE |

### Decision 15.4: PagoCondena — tabla separada

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir el circuito de pago de condena |
| **Evidencia** | DDL tiene solo flag `SiPagoTotal`; prototipo valido circuito completo; separacion de pago voluntario es regla de dominio |
| **Opcion A** | Campos en `fal_acta` |
| **Opcion B** | Tabla `fal_acta_pago_condena` separada (recomendada) |
| **Recomendacion** | Opcion B: tabla separada. La separacion con pago voluntario es obligatoria |
| **Impacto** | Alto: afecta cerrabilidad, bandeja archivo, portal infractor |
| **Riesgo** | Nueva entidad fuera de spec; pendiente de cruce con spec de ingresos |
| **Veredicto** | PENDIENTE |

### Decision 15.5: Apelacion — tabla propia vs evento enriquecido

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir la apelacion |
| **Evidencia** | DDL tiene TipoEvt=13 + FhVtoApelacion en snapshot; prototipo modela canal, estado, resultado; 1 acta : 0..1 apelacion activa |
| **Opcion A** | Solo evento enriquecido TipoEvt=13 + payload canal/resultado |
| **Opcion B** | Tabla `fal_acta_apelacion` con canal, estado, resultado, fechas |
| **Recomendacion** | Opcion B: la apelacion tiene vida propia (presentada / resuelta / con resultado); merece entidad |
| **Impacto** | Medio: afecta bandeja D8, resultado final, condena firme |
| **Riesgo si A** | El resultado de resolucion no tiene lugar claro en fal_acta_evento |
| **Veredicto** | PENDIENTE |

### Decision 15.6: GestionExterna — tabla propia vs flags+eventos

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir la gestion externa |
| **Evidencia** | DDL tiene flags en snapshot; spec tiene bloque de servicio dedicado; prototipo valido N gestiones historicas y resultado que puede modificar monto |
| **Opcion A** | Solo flags en snapshot + eventos enriquecidos; sin tabla propia |
| **Opcion B** | Tabla `fal_acta_gestion_externa` con trazabilidad de N gestiones |
| **Recomendacion** | Opcion B si el historial de N derivaciones es requisito operativo real |
| **Impacto** | Medio: afecta bandeja D9, resultado final, monto condena |
| **Veredicto** | PENDIENTE |

### Decision 15.7: Paralizacion — tabla historica + campo en snapshot

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir la paralizacion con motivo |
| **Evidencia** | Gap critico D10: bandeja exige motivo en snapshot; DDL no tiene tabla ni campo; prototipo tiene mapa volatil; 1 acta : N paralizaciones historicas |
| **Opcion A** | Solo campo `motivo_paralizacion_act` en snapshot |
| **Opcion B** | Tabla historica `fal_acta_paralizacion` + campo en snapshot |
| **Recomendacion** | Opcion B: tabla historica para trazabilidad completa; campo en snapshot para consulta rapida de bandeja D10 |
| **Impacto** | Alto: gap critico de bandeja D10; sin tabla el motivo no persiste |
| **Veredicto** | PENDIENTE |

### Decision 15.8: Archivo — tabla historica vs campos en acta

| Aspecto | Detalle |
|---|---|
| **Decision** | Como persistir el historial de archivos |
| **Evidencia** | Gap critico D11: bandeja exige causales bloqueantes; TipoCierreAct en DDL es parcial; prototipo distingue 3 origenes de archivo; reingreso -> nuevo archivo es posible |
| **Opcion A** | Campos en `fal_acta`: motivo_archivo, permite_reingreso, fh_archivo |
| **Opcion B** | Tabla historica `fal_acta_archivo` + campos en acta para estado actual |
| **Opcion C** | Evento enriquecido + campos en acta |
| **Recomendacion** | Opcion B: tabla historica para N archivos con causales; campos en acta para estado actual rapido |
| **Impacto** | Medio: afecta bandeja D11, reingreso, trazabilidad |
| **Veredicto** | PENDIENTE |

### Decision 15.9: BloqueanteCierreMaterial — tabla propia vs extension medida preventiva

| Aspecto | Detalle |
|---|---|
| **Decision** | Como modelar los tres origenes de bloqueo con sus dos pasos |
| **Evidencia** | Gap critico D11/D12; FalActaMedidaPreventiva cubre solo origen 1; origenes 2 y 3 (rodado, documentacion) no tienen tabla; dos pasos distintos por origen |
| **Opcion A** | Extension de `fal_acta_medida_preventiva` para cubrir los 3 origenes |
| **Opcion B** | Tabla propia `fal_acta_bloqueante_cierre_material` |
| **Recomendacion** | Opcion B: tabla propia. Los tres origenes tienen reglas distintas; mezclar en medida preventiva contamina ese concepto |
| **Impacto** | Alto: gap critico bandejas D11/D12; flags del snapshot se derivan de esta tabla |
| **Veredicto** | PENDIENTE |

### Decision 15.10: AccionPendiente — derivada vs snapshot vs persistida

| Aspecto | Detalle |
|---|---|
| **Decision** | Como modelar la accion pendiente del expediente |
| **Evidencia** | Hoy mapa volatil en prototipo; discriminador de sub-bandeja; puede ser campo persistido, derivado de estados, o derivado de eventos |
| **Opcion A** | Campo persistido en `fal_acta` actualizado en cada transicion |
| **Opcion B** | Campo calculado desde el ultimo evento significativo del historial |
| **Opcion C** | Campo en snapshot derivado de combinacion de estados (resultado_final + situacion_pago + situacion_pago_condena + bloqueantes activos) |
| **Recomendacion** | Opcion C: cache en snapshot calculado en actualizacion incremental. Alineado con el patron de FalActaSnapshot |
| **Impacto** | Medio: afecta sub-bandejas, filtros operativos de UI |
| **Veredicto** | PENDIENTE |

### Decision 15.11: Charset/collation MySQL

| Aspecto | Detalle |
|---|---|
| **Decision** | Que charset y collation usar en toda la base |
| **Evidencia** | Spec no define charset; DDL Informix no tiene equivalente directo |
| **Opcion A** | `utf8mb4` con `utf8mb4_unicode_ci` (compatible con MySQL 5.7 y 8.0) |
| **Opcion B** | `utf8mb4` con `utf8mb4_0900_ai_ci` (MySQL 8.0+ por defecto; mas performante) |
| **Recomendacion** | Opcion B si se confirma MySQL 8.0+; case-insensitive para nombres y textos; es el default de MySQL 8 |
| **Impacto** | Debe ser consistente en toda la base antes de crear tablas |
| **Veredicto** | PENDIENTE |

### Decision 15.12: Estrategia de IDs — BIGINT AUTO_INCREMENT vs UUID/ULID

| Aspecto | Detalle |
|---|---|
| **Decision** | Estrategia de generacion de PKs |
| **Evidencia** | DDL Informix usa secuencias por tabla (SeqFalActa, SeqFalActEv, etc.) |
| **Opcion A** | `BIGINT AUTO_INCREMENT` por tabla (equivalente directo a secuencias Informix) |
| **Opcion B** | UUID v4 como PK (16 bytes; fragmentacion de indice en MySQL) |
| **Opcion C** | ULID como `CHAR(26)` (ordenable; sin fragmentacion; sin soporte nativo MySQL) |
| **Recomendacion** | Opcion A: `BIGINT AUTO_INCREMENT`. Simple, estandar, compatible con todas las tablas DDL existentes. Mantener `id_tecnico CHAR(36)` como UUID externo para sincronizacion mobile donde se necesite |
| **Impacto** | Todos los DDL; decision global |
| **Veredicto** | PENDIENTE |

---

## 16. Tabla de veredicto humano pendiente

| Tabla / Area | Decision | Recomendacion de este doc | Alternativas | Impacto si se acepta rec | Impacto si se rechaza rec | Veredicto |
|---|---|---|---|---|---|---|
| `fal_acta_evento` | Estrategia TipoEvt | Hibrido C (~35 tipos + payload) | A: ampliar enum; B: catalogo tabla | Flexibilidad sin join adicional | A: enum grande; B: joins en todo query de eventos | PENDIENTE |
| `fal_acta_fallo` | Tabla propia vs doc enriquecido | Hibrido C (doc + entidad fallo) | A: solo doc; B: solo tabla fallo | Monto condena con lugar natural | A: sin monto; B: sin documento en circuito firma | PENDIENTE |
| `fal_acta_pago_voluntario` | Tabla propia vs flags/eventos | Tabla propia (Opcion B) | A: campos en acta; C: solo eventos | Historial de intentos; comprobante estructurado | Sin historial; comprobante como texto libre | PENDIENTE |
| `fal_acta_pago_condena` | Tabla separada | Tabla separada (casi obligatoria) | A: campos en acta | Separacion limpia de circuitos | Mezcla de circuitos; sin historial condena | PENDIENTE |
| `fal_acta_apelacion` | Tabla propia vs evento | Tabla propia (Opcion B) | A: solo evento TipoEvt=13 | Canal y resultado con lugar propio | Resultado de resolucion sin campo claro | PENDIENTE |
| `fal_acta_gestion_externa` | Tabla propia vs flags | Tabla propia (Opcion B) | A: solo flags en snapshot | Historial de N derivaciones | Sin historial; resultado externo sin trazabilidad | PENDIENTE |
| `fal_acta_paralizacion` | Tabla historica + campo snapshot | Ambas (Opcion B) | A: solo campo snapshot | Trazabilidad completa; gap D10 resuelto | Gap D10 sin resolver; motivo no persiste | PENDIENTE |
| `fal_acta_archivo` | Tabla historica + campos acta | Ambas (Opcion B) | A: solo campos acta; C: solo eventos | Historial N archivos; gap D11 resuelto | Sin historial; causales sin campo estructurado | PENDIENTE |
| `fal_acta_bloqueante_cierre_material` | Tabla propia | Tabla propia (Opcion B) | A: extension medida preventiva | Gap D11/D12 resuelto; 3 origenes modelados | Mezcla de conceptos en medida preventiva | PENDIENTE |
| `accion_pendiente` | Campo snapshot derivado | Opcion C (cache en snapshot) | A: campo en acta; B: derivado de eventos | Cache disponible sin recalculo | A: requiere actualizacion consistente; B: recalculo costoso | PENDIENTE |
| Charset/collation | utf8mb4_0900_ai_ci | utf8mb4_0900_ai_ci (MySQL 8.0+) | utf8mb4_unicode_ci | Consistencia en toda la base | Inconsistencias si se mezclan | PENDIENTE |
| Estrategia IDs | BIGINT AUTO_INCREMENT | BIGINT AUTO_INCREMENT por tabla | UUID/ULID | Simple; compatible con DDL existente | Fragmentacion de indice (UUID) | PENDIENTE |
| `fal_lote_correo` | Diferir vs tabla propia | Diferir | Tabla propia ahora | MVP sin overhead de entidad no usada | Sin trazabilidad de lote postal | PENDIENTE |
| `fal_acta_qr_acceso` | Diferir vs tabla | Diferir (campo en documento) | Tabla de acceso completa | MVP sin overhead | Sin trazabilidad de accesos individuales | PENDIENTE |

---

## 17. Criterios MySQL

### 17.1 Tipos de dato: traduccion desde DDL Informix

| Tipo Informix | Tipo MySQL 8.x sugerido | Notas |
|---|---|---|
| `LVARCHAR` / `VARCHAR(n)` | `TEXT` o `VARCHAR(n)` | `TEXT` para narrativas largas; `VARCHAR(n)` para textos cortos con longitud conocida |
| `DATETIME YEAR TO SECOND` | `DATETIME(6)` | DATETIME(6) da precision de microsegundos; suficiente para todos los casos |
| `DATETIME YEAR TO DAY` | `DATE` | Equivalente exacto |
| `INT8` / `INT` | `BIGINT` / `INT` | Usar BIGINT para PKs y FKs; INT donde el rango es conocido y acotado |
| `SMALLINT` | `SMALLINT` | Equivalente directo |
| `DECIMAL(n,m)` | `DECIMAL(14,2)` | 14,2 para montos de faltas; 12,8 para coordenadas GPS |
| `CHAR(36)` | `CHAR(36)` | Para UUIDs; portable directo |
| `VARCHAR(64)` | `VARCHAR(64)` | Para storage_key; portable directo |
| Booleanos `SMALLINT(0/1)` | `BOOLEAN` o `TINYINT(1)` | BOOLEAN es alias de TINYINT(1) en MySQL; semanticamente mas claro |
| Secuencias `NEXT VALUE FOR Seq*` | `BIGINT AUTO_INCREMENT` | Equivalente funcional; una secuencia por tabla |

### 17.2 Charset y collation

- **Recomendado:** `utf8mb4` con `utf8mb4_0900_ai_ci` (MySQL 8.0+ por defecto)
- `utf8mb4` es obligatorio para soporte completo de Unicode (incluye caracteres especiales en observaciones)
- La collation afecta comparaciones y ordenamiento; debe ser consistente en toda la base
- **Estado:** Pendiente de decision humana (ver decision 15.11)

### 17.3 Motor de tabla

- **Obligatorio:** `InnoDB` para todas las tablas de dominio
- `InnoDB`: soporte de FK, transacciones COMMIT/ROLLBACK, SELECT ... FOR UPDATE
- No usar MyISAM ni otros motores para tablas de dominio

### 17.4 Nombres de tablas y columnas

- **Convencion:** `snake_case` en minusculas para todos los nombres
- Evita el problema de case-sensitivity entre Windows (case-insensitive) y Linux (case-sensitive)
- Prefijos: `fal_` para dominio faltas; `num_` para numeracion; `stor_` para storage
- No usar PascalCase (ej. `FalActa`) — reservado para referencia historica al DDL Informix

### 17.5 JSON: solo si se justifica

- MySQL 8.x soporta `JSON` con validacion y funciones de acceso (`JSON_EXTRACT`, `->>`).
- Uso recomendado solo para: `payload_json` en `fal_acta_evento` (estrategia hibrida), metadatos adicionales de storage, configuracion flexible.
- **No usar JSON** para estados de dominio que deben ser consultables con SQL simple.
- **No usar JSON** para campos que participan en `WHERE`, `ORDER BY` o indices frecuentes.

### 17.6 ENUM: evitar

- MySQL `ENUM` tiene costo de ALTER TABLE en tablas grandes.
- **Preferir** `SMALLINT` o `TINYINT` con catalogo documentado en comentario o tabla de referencia.
- Si se usa ENUM: solo para valores verdaderamente fijos y conocidos en tiempo de DDL (ej: ACTIVO/INACTIVO).

### 17.7 Indices propuestos por area

**Bandejas y snapshot (consultas mas frecuentes):**
```
fal_acta_snapshot (cod_bandeja, si_visible_bandeja, prioridad)
fal_acta_snapshot (bloque_actual, est_proc_act, sit_adm_act)
fal_acta_snapshot (resultado_final)
fal_acta_snapshot (motivo_paralizacion_act)  -- bandeja D10
fal_acta_snapshot (si_bloqueante_med_prev, si_bloqueante_rodado, si_bloqueante_doc_ret)  -- bandeja D11/D12
fal_acta_snapshot (est_pago_act, si_pago_volunt)  -- bandeja D3
```

**Busqueda operativa de actas:**
```
fal_acta (nro_acta)
fal_acta (id_tecnico)  -- UNIQUE
fal_acta (doc_nro_infct, doc_pref_infct)  -- busqueda por documento infractor
fal_acta (nom_infct)  -- busqueda por nombre (posiblemente FULLTEXT)
fal_acta (fh_acta)
fal_acta (id_dep, fh_acta)  -- busqueda por dependencia
fal_acta (id_insp, fh_acta)  -- busqueda por inspector
```

**Historial de eventos:**
```
fal_acta_evento (acta_id, fh_evt)  -- historial del expediente
fal_acta_evento (acta_id, tipo_evt)  -- busqueda por tipo de evento
fal_acta_evento (tipo_evt)  -- consultas globales por tipo
```

**Notificaciones pendientes:**
```
fal_notificacion (acta_id, estado_notif, tipo_notif)
fal_notificacion (estado_notif, canal_notif)  -- batch de envio
fal_notificacion_intento (notificacion_id, fh_intento)
```

**Documentos pendientes de firma:**
```
fal_documento (estado_docu, tipo_docu)
fal_acta_documento (acta_id, rol_docu_acta)
fal_documento_firma (documento_id, estado_firma)
```

**Pagos y gestion externa:**
```
fal_acta_pago_voluntario (acta_id, situacion_pago)
fal_acta_pago_condena (acta_id, situacion_pago_condena)
fal_acta_gestion_externa (acta_id, estado)
```

**Bloqueantes:**
```
fal_acta_bloqueante_cierre_material (acta_id, origen, estado_cumplimiento_material)
```

### 17.8 PKs y estrategia de IDs

- `BIGINT AUTO_INCREMENT` por tabla para todas las tablas con ID numerico
- Equivalente directo a las secuencias Informix (`SeqFalActa`, `SeqFalActEv`, etc.)
- Excepcion: `stor_objeto` usa `storage_key VARCHAR(64)` como PK natural (portable directo)
- Excepcion: tablas con PK compuesta (versionado): `fal_dependencia_version`, `fal_inspector_version` — mantener PK compuesta

### 17.9 Geoespacial / PostGIS

- Las tablas `geo_gmat_*` usan PostGIS; no son del modulo faltas
- `fal_acta` conserva `lat_infr DECIMAL(12,8)` y `lon_infr DECIMAL(12,8)` como campos simples (portable directo a MySQL)
- Si se necesita funcionalidad geoespacial avanzada: MySQL 8.x tiene soporte nativo (GEOMETRY, POINT) pero no es compatible directamente con PostGIS

---

## 18. Proximo paso recomendado

### 18.1 Secuencia de revision humana

1. **Revision tabla por tabla y campo por campo** de este documento
2. Registrar veredicto en la columna "Veredicto" de la seccion 16
3. Para cada tabla con veredicto pendiente: decidir entre las opciones propuestas
4. Para `fal_acta` y `fal_acta_snapshot`: revisar campo por campo los bloques de domicilio (los ~15 campos geo son criticos para el labrado)
5. Alinear nombres de tablas/campos con convencion definitiva del equipo
6. Decidir charset/collation y estrategia de IDs (decisiones 15.11 y 15.12)

### 18.2 Solo despues de revision humana completa

Solo despues de que cada tabla tenga veredicto registrado:

1. Generar DDL MySQL fisico (CREATE TABLE statements)
2. Crear archivos `.sql` bajo `resources/sql/ddl/`
3. Pensar en scripts de migracion si hay esquema previo
4. Escribir repositorios Spring JDBC (`JdbcClient`) por tabla
5. Conectar `PrototipoApiController` con persistencia real (etapas 1-3 del plan de integracion)

### 18.3 Que no debe hacerse antes del veredicto

- No crear DDL fisico con decisiones abiertas
- No escribir repositorios Java que dependen de tablas no decididas
- No implementar migraciones sobre un modelo inestable
- No cerrar la estrategia TipoEvt implementando antes de decidir

---

*Documento generado: Jun 2026. Sin cambios de codigo. Sin modificacion de specs existentes. Sin modificacion de DDL existente. Sin migraciones. Sin DDL fisico definitivo.*
*Propuesta revisable — la revision humana, tabla por tabla y campo por campo, define la verdad final del modelo de datos.*

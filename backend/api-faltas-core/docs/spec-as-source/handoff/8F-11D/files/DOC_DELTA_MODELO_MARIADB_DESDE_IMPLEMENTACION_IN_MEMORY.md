# Delta modelo MariaDB desde implementacion in-memory

Este archivo registra diferencias y ajustes descubiertos durante la implementacion in-memory del backend pi-faltas-core.

No reemplaza el modelo MariaDB base.
No reemplaza la matriz de proceso.
Sirve como lista controlada de cambios candidatos para una futura version corregida del modelo MariaDB, seeds de catalogos y repositorios JDBC/MariaDB.

## Estado actual

- Backend in-memory implementado hasta Micro-slice 8C-5B (Enviar documento a firma con numeracion automatica).
- Build actual: 766/766 tests pasando. BUILD SUCCESS.
- Persistencia MariaDB real todavia no implementada.

## Cambios consolidados por implementacion

### Resultado final

Valores productivos vigentes en Java/spec:

- `SIN_RESULTADO_FINAL`
- `PAGO_VOLUNTARIO_CONFIRMADO`
- `ABSUELTO`
- `CONDENA_FIRME`
- `CONDENA_FIRME_PAGADA`

Valores no productivos / reemplazados:

- `FALLO_ABSOLUTORIO`
- `FALLO_CONDENATORIO_PAGADO`
- `PAGO_VOLUNTARIO`
- `PAGO_INFORMADO`

### Eventos de pago condena

Valores vigentes:

- `PCOINF`
- `PCOCNF`
- `PCOOBS`

Valores prohibidos/reemplazados:

- `PAGCON`

### Eventos de apelacion

Valores vigentes:

- `APEPRE`
- `APERAZ`
- `APEABS`

Valores prohibidos/reemplazados:

- `APELAC`

### Eventos de cierre

Valor vigente:

- `CIERRA`

Valor prohibido/reemplazado:

- `ACTCER`

### Bloques productivos

Valores vigentes:

- `CAPT`
- `ENRI`
- `NOTI`
- `ANAL`
- `GEXT`
- `ARCH`
- `CERR`

Valores prohibidos/no productivos:

- `D1_CAPTURA`
- `D2_ENRIQUECIMIENTO`
- `D3`
- `D3_DOCUMENTAL`
- `DOCUMENTAL`
- `D4_NOTIFICACION`
- `D5_ANALISIS`

### Acciones pendientes

Valor eliminado (micro-slice pre-Slice 6):

- `GESTIONAR_CONDENA_FIRME` - no existe en AccionPendiente

Valor vigente para condena firme pendiente de pago:

- `GESTIONAR_PAGO_CONDENA`

### Gestion externa (Slice 6A completado)

Eventos implementados y registrables:
- `EXTDER` = derivar a gestion externa (Slice 6A implementado)

Eventos reservados pero no emitidos todavia:
- `EXTRET` = reingresar desde gestion externa (Slice 6B pendiente)
- `PAGAPR` = pago apremio / pago externo (Slice 6C o posterior)

Evento prohibido (no existe en TipoEventoActa):
- `DRVEXT` - eliminado y rechazado por TipoEventoActa.deCodigo()

Entidades nuevas implementadas en Slice 6A:
- `FalGestionExterna` - entidad de gestion externa (tabla candidata: fal_gestion_externa)

Enums nuevos implementados en Slice 6A:
- `TipoGestionExterna` (APREMIO, JUZGADO_DE_PAZ, OTRA)
- `EstadoGestionExterna` (DERIVADA, EN_CURSO, REINGRESADA, CERRADA_EXTERNA, ANULADA)
- `ResultadoGestionExterna` - catalogo productivo (alineado Slice 6D-0):
  (SIN_RESULTADO, SIN_CAMBIOS, PAGO_REGISTRADO, SIN_PAGO, ABSUELVE, CONFIRMA_CONDENA, MODIFICA_MONTO)
- `ModoReingresoGestionExterna` - catalogo productivo (alineado Slice 6D-0):
  (REINGRESO_CON_PAGO, REINGRESO_SIN_PAGO, REINGRESO_CON_DICTAMEN, REINGRESO_PARA_NUEVO_FALLO,
   REINGRESO_PARA_CIERRE, REINGRESO_PARA_REVISION)

Slice 6A implementa solo DERIVADA / SIN_RESULTADO / null como valores activos.
Los restantes se habilitan en Slices 6B, 6C, 6D-0, 6D-1 y 6D-2.

Campos trazabilidad de origen (para reingreso determinista Slice 6B):
- `bloqueOrigen` - BloqueActual antes de la derivacion
- `situacionAdministrativaOrigen` - SituacionAdministrativaActa antes de la derivacion
- `codigoBandejaOrigen` - CodigoBandeja del snapshot antes de la derivacion
- `accionPendienteOrigen` - AccionPendiente del snapshot antes de la derivacion

## Nota obligatoria

Antes de implementar MariaDB/JDBC:
1. revisar este delta;
2. reconciliarlo con el modelo MariaDB base;
3. generar contrato Java/spec/MariaDB;
4. generar seeds o validaciones de catalogos;
5. recien despues implementar repositorios JDBC/MariaDB.

---

### Gestion externa (Slice 6B completado)

Eventos implementados y registrables:
- `EXTDER` = derivar a gestion externa (Slice 6A)
- `EXTRET` = reingresar desde gestion externa (Slice 6B implementado)

Evento reservado pero no emitido todavia:
- `PAGAPR` = pago apremio / pago externo (Slice 6C o posterior)

Campos nuevos en `FalGestionExterna` (Slice 6B):
- `motivoReingreso` (String, nullable)
- `observacionesReingreso` (String, nullable)
- `fechaReingreso` (LocalDateTime, nullable)

Metodo nuevo en `GestionExternaRepository`:
- `buscarPorHistorico(actaId)` - devuelve la gestion externa independientemente de `siActiva`

Modos habilitados en Slice 6B (nombres del catalogo productivo, alineados en Slice 6D-0):
- `REINGRESO_PARA_REVISION` - activo, implementado (reemplaza REINGRESAR_A_ANALISIS)
- `REINGRESO_SIN_PAGO` - activo, implementado (reemplaza REINGRESAR_A_PAGO_CONDENA)

Modos declarados pero prohibidos temporalmente en Slice 6B:
- `REINGRESO_PARA_CIERRE` - reservado, sigue bloqueado
- `REINGRESO_CON_PAGO` - reservado, sigue bloqueado
- `REINGRESO_PARA_NUEVO_FALLO` - reservado en 6B; habilitado desde Slice 6D-2 con resultado ABSUELVE
- `REINGRESO_CON_DICTAMEN` - reservado en 6B; habilitado desde Slice 6D-2 con CONFIRMA_CONDENA o MODIFICA_MONTO

Efecto del reingreso sobre el acta:
- `situacionAdministrativa = ACTIVA`
- `bloqueActual = ANAL`
- `FalGestionExterna.siActiva = false`
- `FalGestionExterna.estadoGestionExterna = REINGRESADA`

Estado del delta en relacion al modelo MariaDB base:

| Campo/Tabla | Modelo MariaDB base | In-Memory Slice 6B | Nota |
|-------------|--------------------|--------------------|------|
| fal_gestion_externa.motivo_reingreso | presente | presente | Alineado |
| fal_gestion_externa.fecha_reingreso | presente | presente | Alineado |
| fal_gestion_externa.observaciones_reingreso | presente | presente | Alineado |
| fal_gestion_externa.si_activa | presente | presente | Alineado |
| fal_gestion_externa.modo_reingreso | presente | presente | Alineado |

Nota: si el modelo MariaDB base no contempla alguno de estos campos, agregarlos al DDL
antes de implementar Slice 9 (MariaDB/JDBC).


---

### Gestion externa (Slice 6C completado)

**Estado del modulo:** 262/262 tests passing. BUILD SUCCESS.

**Evento activo desde Slice 6C:**
- `PAGAPR` = pago externo registrado en gestion externa (apremio / juzgado de paz / otra)

**Valores de enums que pasan a activos en Slice 6C:**

| Enum | Valor | Estado anterior | Estado en 6C |
|------|-------|----------------|--------------|
| `EstadoGestionExterna` | `CERRADA_EXTERNA` | Declarado, no usado | Activo |
| `ResultadoGestionExterna` | `PAGO_REGISTRADO` | Declarado, no usado | Activo (reemplaza PAGO_EXTERNO_INFORMADO alineado en 6D-0) |
| `TipoEventoActa` | `PAGAPR` | Declarado, no emitido | Emitido |
| `ResultadoFinalActa` | `CONDENA_FIRME_PAGADA` | Activo desde Slice 5 | Sin cambio |

**Campo nuevo en `FalGestionExterna` (tabla fisica: `fal_acta_gestion_externa`):**

| Campo Java | Columna fisica candidata | Tipo DDL | Nullable | Observacion |
|-----------|--------------------------|----------|----------|-------------|
| `fechaCierreGestionExterna` | `fecha_cierre_gestion_externa` | `DATETIME NULL` | si | Poblada solo por camino PAGAPR. NULL para ciclos cerrados por EXTRET. |

**Columnas que NO se agregan (prohibidas por arquitectura):**

| Columna rechazada | Motivo |
|-------------------|--------|
| `observaciones_cierre_gestion_externa` | Texto libre prohibido en tablas de dominio. Las observaciones van a `fal_observacion`. |
| `motivo_cierre_gestion_externa` | Texto libre prohibido. Si en el futuro se necesita motivo estructurado: enum/catalogo, no texto libre. |

**Deuda tecnica - Slice 9/JDBC:**

| Deuda | Detalle |
|-------|---------|
| `FalObservacion` / `fal_observacion` | No implementado en modulo in-memory. En Slice 6C las observaciones del evento PAGAPR viajan en `FalActaEvento.descripcion` como puente transitorio. En Slice 9/JDBC deben registrarse en `fal_observacion` con `entidad_tipo = GESTION_EXTERNA` (ID 10 en catalogo cerrado del modelo MariaDB base). |
| `entidad_tipo = 10 = GESTION_EXTERNA` | Ya definido en catalogo del modelo MariaDB base (seccion 2.7). Solo requiere implementacion del repositorio en Slice 9. |

**Nota sobre nombre fisico de tabla:**
En el modelo MariaDB base la tabla se llama `fal_acta_gestion_externa`.
En el modulo Java in-memory la entidad es `FalGestionExterna` (sin prefijo `acta`).
Esta diferencia se reconcilia en Slice 9 al implementar repositorios JDBC.

**SnapshotRecalculador - nuevo caso de routing:**
Cuando `resultadoFinal == CONDENA_FIRME_PAGADA` y `situacionAdministrativa == ACTIVA`
(estado transitorio por bloqueantes activos tras PAGAPR):
- Bandeja: `PENDIENTE_ANALISIS`
- Accion: `NINGUNA`
- Implementado en `SnapshotRecalculador.derivarBandejaYAccion()` desde Slice 6C.


---

### Gestion externa (Slice 6D-0 completado)

**Objetivo:** Reconciliar catalogos de gestion externa con el modelo productivo MariaDB.
**Estado:** 300/300 tests passing.

**Cambios de catalogo (renombres y eliminaciones):**

| Valor anterior (transitorio) | Valor productivo vigente | Accion |
|-----------------------------|--------------------------|--------|
| `PAGO_EXTERNO_INFORMADO` | `PAGO_REGISTRADO` | Renombrado |
| `DEVUELTA_PARA_REVISION` | `SIN_CAMBIOS` | Reemplazado |
| `NO_GESTIONABLE` | (no existe en productivo) | Eliminado |
| `NO_APLICA` | (campo nullable) | Eliminado; campo inicializa en null |
| `REINGRESAR_A_ANALISIS` | `REINGRESO_PARA_REVISION` | Renombrado |
| `REINGRESAR_A_PAGO_CONDENA` | `REINGRESO_SIN_PAGO` | Renombrado |
| `REINGRESAR_A_CIERRE` | `REINGRESO_PARA_CIERRE` | Renombrado (sigue reservado) |
| `REINGRESAR_A_ARCHIVO` | (no existe en productivo) | Eliminado |

**Estado del catalogo productivo tras 6D-0:**

`ResultadoGestionExterna` (valores vigentes):
- `SIN_RESULTADO` - estado inicial al derivar; no informable en reingreso manual
- `SIN_CAMBIOS` - reingresa sin cambios (habilitado 6D-1)
- `PAGO_REGISTRADO` - exclusivo de PAGAPR (habilitado 6C)
- `SIN_PAGO` - reingresa sin pago (habilitado 6D-1)
- `ABSUELVE` - propone absolucion externa (habilitado 6D-2)
- `CONFIRMA_CONDENA` - confirma condena externa (habilitado 6D-2)
- `MODIFICA_MONTO` - modifica monto externo (habilitado 6D-2)

`ModoReingresoGestionExterna` (valores vigentes):
- `REINGRESO_CON_PAGO` - reservado, bloqueado (fuera de PAGAPR)
- `REINGRESO_SIN_PAGO` - habilitado desde 6B/6D-1
- `REINGRESO_CON_DICTAMEN` - habilitado desde 6D-2
- `REINGRESO_PARA_NUEVO_FALLO` - habilitado desde 6D-2
- `REINGRESO_PARA_CIERRE` - reservado, siempre bloqueado
- `REINGRESO_PARA_REVISION` - habilitado desde 6B/6D-1

**Alineacion con modelo MariaDB base:** los valores de ambos catalogos coinciden ahora con los
definidos en la tabla `resultado_gestion_ext` y `modo_reingreso_gestion_ext` del modelo productivo.

---

### Gestion externa (Slice 6D-1 completado)

**Objetivo:** Habilitar reingreso SIN_PAGO y SIN_CAMBIOS con validacion de pares.
**Estado:** 315/315 tests passing.

**Comportamientos habilitados:**

| Resultado | Modo | Efecto sobre acta |
|-----------|------|-------------------|
| `SIN_PAGO` | `REINGRESO_SIN_PAGO` | ACTIVA/ANAL; resultadoFinal conserva CONDENA_FIRME |
| `SIN_CAMBIOS` | `REINGRESO_PARA_REVISION` | ACTIVA/ANAL; resultadoFinal sin cambio |

**Invariantes:**
- No emite CIERRA, PAGAPR ni PCOCNF.
- `fechaCierreGestionExterna` permanece null.
- `estadoGestionExterna = REINGRESADA`.
- Despues de EXTRET, PAGAPR ya no es posible en el mismo ciclo.

**No hay cambios de esquema para este slice:** todos los campos necesarios ya estaban en la tabla.

---

### Gestion externa (Slice 6D-2 completado)

**Objetivo:** Habilitar reingreso con dictamen externo (ABSUELVE, CONFIRMA_CONDENA, MODIFICA_MONTO).
**Estado:** 330/330 tests passing.

**Campo nuevo en `FalGestionExterna` (tabla fisica: `fal_acta_gestion_externa`):**

| Campo Java | Columna fisica candidata | Tipo DDL | Nullable | Descripcion |
|-----------|--------------------------|----------|----------|-------------|
| `montoResultado` | `monto_resultado` | `DECIMAL(14,2) NULL` | si | Monto externo informado. Obligatorio y > 0 solo para MODIFICA_MONTO. NULL para todos los demas resultados. |

**Alineacion con modelo MariaDB base:**
El campo `monto_resultado DECIMAL(14,2) NULL` ya esta definido en el modelo MariaDB base
en la tabla `fal_acta_gestion_externa`. La implementacion in-memory usa `BigDecimal montoResultado` (alineado).

**Comportamientos habilitados:**

| Resultado | Modo | Precondicion | Efecto sobre resultadoFinal |
|-----------|------|-------------|----------------------------|
| `ABSUELVE` | `REINGRESO_PARA_NUEVO_FALLO` | CONDENA_FIRME | No cambia; nuevo fallo posterior |
| `CONFIRMA_CONDENA` | `REINGRESO_CON_DICTAMEN` | CONDENA_FIRME | Confirmado CONDENA_FIRME |
| `MODIFICA_MONTO` | `REINGRESO_CON_DICTAMEN` | CONDENA_FIRME; montoResultado > 0 | No cambia; registra montoResultado |

**Invariantes de 6D-2:**
- No pase automatico a pago ni cierre tras ABSUELVE ni MODIFICA_MONTO.
- No fallo absolutorio automatico tras ABSUELVE.
- REINGRESO_PARA_CIERRE sigue bloqueado.
- REINGRESO_CON_PAGO sigue bloqueado fuera de PAGAPR.
- PAGO_REGISTRADO sigue exclusivo de PAGAPR.
- SIN_RESULTADO no es informable como resultado de reingreso.

**Deuda tecnica para Slice 9/JDBC:**
- Verificar que `fal_acta_gestion_externa.monto_resultado` este definido como `DECIMAL(14,2) NULL`.
- Si el modelo MariaDB base no lo incluye, agregarlo al DDL antes de implementar Slice 9.


---

### Firmantes y autoridades (Micro-slice 8A-3D completado)

**Fecha:** 2026-06-30
**Estado:** Ajuste de modelo productivo. Build: 368/368 tests passing. No hay cambios Java en este micro-slice.

**Detecci?n:**
Durante el diagn?stico previo al Slice 8A-3, se detect? que `fal_firmante` y `fal_firmante_version` estaban ausentes del modelo productivo. Solo `fal_documento_firma` exist?a, pero registra el hecho hist?rico de una firma concreta, no el padr?n/habilitaci?n vigente de firmantes o autoridades.

**Cambios incorporados al modelo productivo:**

| Cambio | Detalle |
|--------|---------|
| `fal_firmante` agregado | Maestro del firmante/autorizado para firma. Administra el padr?n de usuarios habilitados. |
| `fal_firmante_version` agregado | Versionado hist?rico de rol, cargo, dependencia y vigencia del firmante. |
| `fal_documento_firma` vinculado | Agregados `id_firmante` y `ver_firmante` como FK nullable al firmante versionado. |
| Snapshots hist?ricos mantenidos | `id_user_firma`, `rol_firmante`, `nombre_firmante` se conservan en `fal_documento_firma`. |

**Separaci?n correcta:**

- `fal_firmante` / `fal_firmante_version`: maestro versionado de usuarios/personas habilitadas para firmar.
- `fal_documento_firma`: registro hist?rico de una firma concreta sobre un documento concreto.

**Reglas cr?ticas incorporadas:**

- `rol_firmante` en `fal_firmante_version` debe ser compatible con `fal_documento.rol_firma_req`.
- `tipo_firma` NO va en `fal_firmante_version`; `tipo_firma` es el mecanismo/naturaleza de la firma.
- Si cambia rol, cargo o dependencia del firmante, se crea nueva versi?n. No se actualizan versiones anteriores.
- Los snapshots de `fal_documento_firma` no se reescriben por cambios posteriores en el firmante.
- Si se informa `id_dep`, debe informarse `ver_dep` (y debe existir en `fal_dependencia_version`).
- `id_firmante`/`ver_firmante` en `fal_documento_firma` son nullable para admitir firmas de sistema.

**Deuda t?cnica para Slice 9/JDBC:**

| Deuda | Detalle |
|-------|---------|
| DDL `fal_firmante` | Crear tabla seg?n modelo productivo actualizado (Secci?n 1.7). |
| DDL `fal_firmante_version` | Crear tabla seg?n modelo productivo actualizado (Secci?n 1.8). |
| DDL `fal_documento_firma` | Agregar columnas `id_firmante BIGINT NULL FK` y `ver_firmante SMALLINT NULL FK`. |
| Repositorios JDBC | `FirmanteRepository` e implementaci?n JDBC pendientes hasta Slice 9. |

**Pr?ximo slice:**
Slice 8A-3 - Firmantes/autoridades in-memory, alineados al modelo productivo actualizado.


---

### Firmantes, habilitaciones y requerimientos de firma (Micro-slice 8A-3D.1 completado)

**Fecha:** 2026-06-30
**Estado:** Cierre completo del modelo productivo de firma documental. Build: 368/368 tests passing. Sin cambios Java.

**Detección:**
Durante la revisión previa al Slice 8A-3, se detectó que `fal_firmante` y `fal_firmante_version` no alcanzaban para el flujo real.
Faltaban dos piezas centrales:

1. Qué puede firmar cada versión de firmante (habilitación).
2. Qué firmas necesita cada documento concreto (requisitos de firma).

La bandeja de firma no puede resolverse con lógica hardcodeada. Debe salir del cruce: documento + requisito pendiente + firmante vigente + habilitación compatible.

**Cambios incorporados al modelo productivo:**

| Cambio | Detalle |
|--------|---------|
| `fal_firmante_version_habilitacion` agregada | Nueva tabla (Sección 1.9). Define qué tipo de documento y rol puede satisfacer cada versión de firmante. |
| `fal_documento_firma_req` agregada | Nueva tabla (Sección 5.4). Define qué firmas necesita cada documento concreto. Es la fuente para la bandeja de firma. |
| `estado_firma_req` definido | Catálogo: PENDIENTE, FIRMADO, ANULADO, VENCIDO, REEMPLAZADO. |
| `seq_firma_req` en `fal_documento_firma` | Campo nuevo que vincula la firma histórica al requisito que satisfizo. |
| `rol_firmante` en `fal_firmante_version` corregido | Pasa a SÍ nullable y queda como campo descriptivo/institucional opcional. No define autorización documental. |
| Regla de bandeja de firma documentada | Sección 5.5 del modelo productivo. |
| Regla de firma efectiva documentada | Sección 5.6 del modelo productivo. |

**Separación definitiva:**

- `fal_firmante` / `fal_firmante_version`: maestro versionado de usuarios/personas habilitadas para firmar.
- `fal_firmante_version_habilitacion`: define qué puede firmar una versión concreta (por tipo_docu + rol_firma_req + mecanismo opcional).
- `fal_documento_firma_req`: define qué firmas necesita un documento concreto. Fuente de la bandeja.
- `fal_documento_firma`: registro histórico de una firma concreta. Nunca se modifica.

**Reglas críticas incorporadas:**

- Solo `PENDIENTE` entra en bandeja de firma.
- La compatibilidad se evalúa contra tipo_docu + rol_firma_req + mecanismo + habilitación del firmante.
- Si el requisito tiene firmante asignado, solo esa versión puede satisfacerlo.
- Al firmar: se crea `fal_documento_firma` + se actualiza `fal_documento_firma_req` (estado=FIRMADO, fh_firma, id_firma).
- Los snapshots de `fal_documento_firma` no se reescriben por cambios posteriores.
- No modificar firmas históricas por cambios de firmante, cargo, rol, dependencia o habilitación.
- `rol_firmante` en `fal_documento_firma` es snapshot del claim/bearer al momento de firmar, no fuente de autorización.

**Deuda técnica para Slice 9/JDBC:**

| Deuda | Detalle |
|-------|---------|
| DDL `fal_firmante_version_habilitacion` | Crear tabla según modelo productivo actualizado (Sección 1.9). |
| DDL `fal_documento_firma_req` | Crear tabla según modelo productivo actualizado (Sección 5.4). |
| DDL `fal_documento_firma` | Agregar columna `seq_firma_req SMALLINT NULL FK`. |
| DDL `fal_firmante_version` | Actualizar columna `rol_firmante` a nullable. |
| Repositorios JDBC | `FirmanteVersionHabilitacionRepository`, `DocumentoFirmaReqRepository` pendientes hasta Slice 9. |

**Próximo slice:**
Slice 8A-3 - Firmantes/autoridades in-memory, implementando también `FalFirmanteVersionHabilitacion`.

---

## Micro-slice 8B-3D - Catalogo estado_asignacion_talonario

**Fecha:** 2026-06-30

### Contexto

El slice 8B-3 fue bloqueado correctamente porque `num_talonario_inspector.estado_asignacion` es `SMALLINT NOT NULL`
pero el modelo productivo no tenia codigos numericos explicitos para los estados funcionales.

### Cambio aplicado

Se completo el catalogo productivo `estado_asignacion_talonario` en la Seccion 9.5 del modelo.

**Catalogo estado_asignacion_talonario:**

| ID | Codigo | Descripcion |
|---:|---|---|
| 1 | `ENTREGADO` | Talonario manual fisico entregado al inspector y asignacion activa |
| 2 | `DEVUELTO` | Talonario manual fisico devuelto por el inspector |
| 3 | `CERRADO` | Asignacion cerrada administrativamente |
| 4 | `OBSERVADO` | Asignacion observada por inconsistencia, rendicion pendiente o control administrativo |

### Resultado

- Modelo productivo actualizado: Seccion 9.5 `num_talonario_inspector` ahora incluye tabla de catalogo con IDs numericos.
- El campo `estado_asignacion` sigue siendo `SMALLINT NOT NULL`. No cambia el DDL.
- No se implemento Java en este micro-slice.
- No se inicio MariaDB/JDBC.

**Desbloqueo:**
El slice 8B-3 queda desbloqueado. Puede implementarse `EstadoAsignacionTalonario` con:
- `ENTREGADO = 1`
- `DEVUELTO = 2`
- `CERRADO = 3`
- `OBSERVADO = 4`

**Proximo slice:**
8B-3 - NumTalonarioInspector y asignacion de talonario manual fisico a inspector.

---

## Micro-slice 8C-0D - Base documental (2026-07-01)

**Fecha:** 2026-07-01
**Estado:** Cierre de catálogos y base documental. Build: 601/601 tests passing. Sin cambios Java.

### Paso 0 - Gap NumPolitica resuelto (Opción A)

`num_politica` tenía `fhAlta`/`idUserAlta` en Java pero no en el modelo MariaDB.

**Resolución:** Agregados `fh_alta DATETIME(6) NOT NULL` e `id_user_alta CHAR(36) NOT NULL` al modelo productivo de `num_politica` (Sección 9.2).

| Campo nuevo | Tabla | Tipo | Null | Motivo |
|---|---|---|---|---|
| `fh_alta` | `num_politica` | `DATETIME(6)` | NO | Auditoría estándar productiva |
| `id_user_alta` | `num_politica` | `CHAR(36)` | NO | Auditoría estándar productiva |

### Cambios en `fal_acta`

| Campo nuevo | Tipo | Null | Descripción |
|---|---|---|---|
| `resultado_firma_infractor` | `SMALLINT` | NO | Resultado firma ólógrafa del infractor |

Catálogo `resultado_firma_infractor` (nuevo, 5 valores): FIRMADA, SE_NIEGA_A_FIRMAR, INFRACTOR_NO_PRESENTE, IMPOSIBILITADO_PARA_FIRMAR, NO_CAPTURADA_POR_FALLA_TECNICA.

### Cambios en `fal_inspector_version`

| Campo nuevo | Tipo | Null | Descripción |
|---|---|---|---|

### Cambios en `fal_acta_evidencia`

Catálogo `tipo_evid` actualizado: se agrega valor 6 = `FIRMA_OLOGRAFA_INFRACTOR`.

### Cambios en `fal_documento`

| Campo | Cambio |
|---|---|
| `nro_docu` | `VARCHAR(20)` -> `VARCHAR(30)` |
| `requisito_firma` | Renombrado a `tipo_firma_req` con nuevo catálogo |
| `rol_firma_req` | Eliminado del nivel de tabla; vive en `fal_documento_firma_req` |
| `mecanismo_firma_req` | Eliminado del nivel de tabla; vive en `fal_documento_firma_req` |
| `plantilla_id` | BIGINT SÍ FK+IDX. Nuevo campo de trazabilidad. |

**Deuda técnica para Slice 9/JDBC:** El módulo in-memory puede tener `rol_firma_req` y `mecanismo_firma_req` en `FalDocumento`. Deben removerse antes de implementar repositorio JDBC. **[CERRADO en 8C-0A: `rolFirmaReq` y `mecanismoFirmaReq` ya removidos de `FalDocumento`. Ver seccion 8C-0A en este DELTA.]**

Catálogo `tipo_firma_req` (nuevo, reemplaza `requisito_firma`):

| ID | Código |
|---:|---|
| 0 | `NO_REQUIERE` |
| 1 | `FIRMA_INTERNA` |
| 2 | `FIRMA_INSPECTOR` |
| 3 | `FIRMA_AUTORIDAD` |
| 4 | `FIRMA_DIGITAL` |
| 5 | `FIRMA_MULTIPLE` |

`FIRMA_MULTIPLE` reemplaza `FIRMA_MIXTA`. No usar `FIRMA_MIXTA`.

Catálogo `estado_docu` (ahora explícito en modelo):

| ID | Código |
|---:|---|
| 1 | `BORRADOR` |
| 2 | `EMITIDO` |
| 3 | `PENDIENTE_FIRMA` |
| 4 | `FIRMADO` |
| 5 | `ADJUNTO` |
| 6 | `ANULADO` |
| 7 | `REEMPLAZADO` |

### Tablas nuevas

| Tabla | Sección | Descripción |
|---|---|---|
| `fal_documento_plantilla` | 5.7 | Catálogo/base documental de plantillas de la aplicación |
| `fal_documento_plantilla_firma_req` | 5.8 | Requisitos base de firma de cada plantilla |

### Catálogos nuevos

| Catálogo | Descripción |
|---|---|
| `accion_documental` | 11 valores. Mapea acciones del sistema con plantillas documentales. |
| `momento_numeracion_docu` | 5 valores: NO_APLICA, AL_CREAR, AL_EMITIR, AL_ENVIAR_A_FIRMA, AL_FIRMAR. |

### Reglas documentadas

- Separación explícita entre numeración de actas y numeración documental.
- Firma automática del inspector en actas (no captura manual).
- Firma ólógrafa del infractor: opcional, evidencia, no institucional.
- `resultado_firma_infractor` obligatorio en acta formal.
- `FIRMA_OLOGRAFA_INFRACTOR` como evidencia del acta.

### Deuda técnica para Slice 9/JDBC

| Deuda | Detalle |
|---|---|
| DDL `num_politica` | Agregar `fh_alta DATETIME(6) NOT NULL` e `id_user_alta CHAR(36) NOT NULL`. |
| DDL `fal_acta` | Agregar `resultado_firma_infractor SMALLINT NOT NULL`. |
| DDL `fal_inspector_version` | firma_storage_key, firma_hash, fh_firma_registrada ELIMINADOS del modelo (decision 8F-11B). |
| DDL `fal_acta_evidencia` | Catálogo `tipo_evid` ahora tiene 6 valores; agregar seed con ID 6. |
| DDL `fal_documento` | `nro_docu VARCHAR(30)`; renombrar `requisito_firma` -> `tipo_firma_req`; eliminar `rol_firma_req` y `mecanismo_firma_req` de la tabla; agregar `plantilla_id BIGINT NULL FK`. |
| DDL nuevas tablas | Crear `fal_documento_plantilla` y `fal_documento_plantilla_firma_req` según modelo 5.7/5.8. |
| Seeds catálogos | `tipo_firma_req`, `estado_docu`, `accion_documental`, `momento_numeracion_docu`, `resultado_firma_infractor`. |
| Java deuda | ~~`FalDocumento`: remover `rolFirmaReq` y `mecanismoFirmaReq` (van a `FalDocumentoFirmaReq`); renombrar `requisitoFirma` -> `tipoFirmaReq`; agregar `plantillaId`.~~ **[CERRADO en 8C-0A. Ver seccion 8C-0A de este DELTA.]** |

---

## Micro-slice 8C-0B - Alineacion de IDs de acta a Long [COMPLETADO]

**Fecha:** 2026-07-01
**Objetivo:** Alinear el modelo Java in-memory con el modelo productivo MariaDB respecto de IDs tecnicos de actas.
**Build:** 601/601 tests passing. BUILD SUCCESS.
**Tipo:** Refactor tecnico de tipos. No se implemento Java funcional nuevo. No se toco Angular. No se inicio MariaDB/JDBC.

### Cambios Java realizados

| Campo | Tipo anterior | Tipo nuevo |
|---|---|---|
| FalActa.id | String | Long |
| NumTalonarioMovimiento.actaId | String | Long |
| FalActaEvento.idActa | String | Long |
| FalActaSnapshot.idActa | String | Long |
| FalActaFallo.actaId | String | Long |
| FalActaApelacion.actaId | String | Long |
| FalActaFirmezaCondena.actaId | String | Long |
| FalGestionExterna.actaId | String | Long |
| FalPagoCondena.actaId | String | Long |
| FalPagoVoluntario.actaId | String | Long |
| FalNotificacion.idActa | String | Long |
| FalDocumento.idActa | String | Long |
| FalDocumentoFirma.idActa | String | Long |
| FalBloqueanteMaterial.actaId | String | Long |
| ComandoResultado.idActa | String | Long |

### Generacion de IDs

- InMemoryActaRepository: agregado AtomicLong idCounter y metodo 
extId().
- ActaRepository interface: agregado metodo Long nextId().
- ActaService.labrar(): usa ctaRepository.nextId() en lugar de UUID.randomUUID().toString().

### Repositories actualizados

- InMemoryFalloActaRepository: Map<String, FalActaFallo> -> Map<Long, FalActaFallo>
- InMemoryPagoCondenaRepository: Map<String, FalPagoCondena> -> Map<Long, FalPagoCondena>
- InMemoryPagoVoluntarioRepository: Map<String, FalPagoVoluntario> -> Map<Long, FalPagoVoluntario>
- InMemoryGestionExternaRepository: Map<String, FalGestionExterna> -> Map<Long, FalGestionExterna>
- Todas las interfaces de repositorio: firmas de metodos actualizadas a Long actaId.

### Controllers actualizados

- Todos los @PathVariable String idActa/id de acta cambiados a @PathVariable Long.
- NotificacionController: IDs de notificacion individuales (UUID String) se mantienen como String.

### Reglas preservadas

- al_acta.nro_acta sigue siendo String visible administrativo.
- 
roActa no fue renombrado a 
umeroActa.
- Identificadores tipo ACTA-0001 eliminados como PK tecnica.
- POL-ACTA-01 (codigo de politica de numeracion) es string funcional, no PK tecnica: se preserva.

### Verificacion final

`
rg "String.*actaId|actaId.*String|String.*idActa|idActa.*String" src/main/java
# Resultado: 0 referencias donde actaId/idActa representa PK tecnica de acta

rg "ACTA-[0-9]" src
# Resultado: solo "POL-ACTA-01" (codigo de politica, no PK tecnica)
`

### Deuda cerrada

- FalActa.id: deuda de tipo String cerrada. Ahora es Long alineado con al_acta.id BIGINT AUTO_INCREMENT.
- NumTalonarioMovimiento.actaId: deuda de tipo String cerrada. Ahora es Long alineado con 
um_talonario_movimiento.acta_id BIGINT.
- Preparacion completada para 8C-0A (FalDocumento contra modelo MariaDB) y 8C-1 (catalogos documentales).

---

## Micro-slice 8C-0A - Alineacion FalDocumento contra modelo MariaDB [COMPLETADO 2026-07-01]

### Estado al cierre

Build: 601/601 tests passing. BUILD SUCCESS.

### Cambios Java

#### FalDocumento.java (model)

- `id`: String (UUID) -> Long (BIGINT AUTO_INCREMENT)
- `tipoDocumento` -> `tipoDocu` (renombrado; tipo TipoDocumento hasta 8C-1)
- `estado` -> `estadoDocu` (renombrado; tipo EstadoDocumento hasta 8C-1)
- `numeroVisible` -> `nroDocu` (renombrado; String VARCHAR(30) nullable)
- `tipoFirmaReq` agregado: short (SMALLINT, 0=NO_REQUIERE por defecto)
- `plantillaId` agregado: Long nullable (BIGINT FK a plantilla documental)
- `idTalonario` agregado: Long nullable (BIGINT FK a talonario)
- `nroTalonarioUsado` agregado: Integer nullable (INT)
- `rolFirmaReq` y `mecanismoFirmaReq` NO se agregaron: pertenecen a fal_documento_firma_req (slice 8C-3)
- Getters/setters actualizados: `getTipoDocu()`, `getEstadoDocu()`, `getNroDocu()`

#### FalActaFallo.java

- `documentoId`: String -> Long

#### FalDocumentoFirma.java (record)

- `idDocumento`: String -> Long
- Validacion de compact constructor: isBlank() -> == null

#### FalNotificacion.java

- `idDocumento`: String -> Long

#### DocumentoNoEncontradoException.java

- Constructor adicional: DocumentoNoEncontradoException(Long idDocumento)

#### Repositories

- DocumentoRepository: buscarPorId(String) -> buscarPorId(Long), nuevo nextId()
- DocumentoFirmaRepository: buscarPorDocumento(String) -> buscarPorDocumento(Long)
- InMemoryDocumentoRepository: Map<String, FalDocumento> -> Map<Long, FalDocumento>, AtomicLong
- InMemoryDocumentoFirmaRepository: filtrado por Long idDocumento

#### Commands y DTOs

- FirmarDocumentoCommand.idDocumento: String -> Long
- EnviarNotificacionCommand.idDocumento: String -> Long
- EnviarNotificacionRequest.idDocumento: String -> Long

#### Controllers

- DocumentoController: @PathVariable String idDocumento -> @PathVariable Long

#### Services

- DocumentoService: usa nextId() para generar id documento, String.valueOf(docId) para eventos
- NotificacionService: getEstadoDocu(), getTipoDocu(), String.valueOf() para ids en eventos
- FalloActaService: usa nextId(), String.valueOf() para ids en eventos, getTipoDocu()
- SnapshotRecalculador: getEstado() -> getEstadoDocu() para FalDocumento

#### Tests

- 7 test files actualizados: String idDocFallo -> Long, FirmarDocumentoCommand con Long.parseLong()
- EnviarNotificacionCommand con Long idDocumento
- getTipoDocumento() -> getTipoDocu() en tests de FalloActaTest y FlujoCompletoTest
- getEstado() -> getEstadoDocu() en assertions de FalDocumento
- FalActaEvento.idDocumento permanece String (campo audit log, se almacena String.valueOf(Long))

### Guardrails preservados

- FalDocumentoPlantilla: no implementado
- FalDocumentoPlantillaFirmaReq: no implementado
- FalDocumentoFirmaReq: no implementado
- TipoDocu.java, EstadoDocu.java, TipoFirmaReq.java: no creados (pendiente 8C-1)
- Generacion documental: no implementada
- Firma documental efectiva: no implementada
- Numeracion documental: no implementada
- MariaDB/JDBC: no usado
- Angular: no tocado

### Deuda cerrada

- FalDocumento.id: String -> Long, alineado con fal_documento.id BIGINT AUTO_INCREMENT
- FalDocumento.nroDocu: campo renombrado desde numeroVisible, alineado con nro_docu VARCHAR(30) nullable
- FalDocumento.tipoFirmaReq: agregado, alineado con tipo_firma_req SMALLINT
- FalDocumento.plantillaId: agregado, alineado con plantilla_id BIGINT nullable
- FalDocumento.idTalonario: agregado, alineado con id_talonario BIGINT nullable
- FalDocumento.nroTalonarioUsado: agregado, alineado con nro_talonario_usado INT nullable
- rolFirmaReq y mecanismoFirmaReq removidos del nivel documento: corresponden a fal_documento_firma_req

### Pendientes proximos

- ~~8C-1: Implementar enums documentales Java (TipoDocu, EstadoDocu, TipoFirmaReq, AccionDocumental, MomentoNumeracionDocu)~~ [COMPLETADO 2026-07-01]
- 8C-2: Implementar FalDocumentoPlantilla y FalDocumentoPlantillaFirmaReq in-memory
- 8C-3: Implementar FalDocumentoFirmaReq y generacion documental desde plantilla
### 8C-1 cerrado [2026-07-01]

Catalogos documentales Java implementados. Enums definitivos con codigos SMALLINT alineados con MariaDB:
- TipoDocu (12 valores, codigo 1-12): reemplaza TipoDocumento provisional.
- EstadoDocu (7 valores, codigo 1-7): reemplaza EstadoDocumento provisional. Agrega ANULADO y REEMPLAZADO.
- EstadoFirma (6 valores, codigo 1-6): reemplaza EstadoFirmaDocumento provisional.
- TipoFirmaReq (6 valores, codigo 0-5): FIRMA_MULTIPLE implementado. FIRMA_MIXTA prohibido.
- AccionDocumental (11 valores): para plantillas documentales futuras.
- MomentoNumeracionDocu (5 valores): solo documentos, no actas.
- ResultadoFirmaInfractor (5 valores): enum creado en 8C-1; campo fal_acta.resultado_firma_infractor implementado en 8C-6A (2026-07-01).

FalDocumento: usa TipoDocu, EstadoDocu, TipoFirmaReq (enum, default NO_REQUIERE).
FalDocumentoFirma: usa EstadoFirma.
FalNotificacion: usa TipoDocu.

Fallos (absolutorio/condenatorio) generan documentos tipo ACTO_ADMINISTRATIVO.
La distincion absolutorio/condenatorio vive en FalActaFallo.tipoFallo, no en TipoDocu.

Enums provisionales eliminados: TipoDocumento, EstadoDocumento, EstadoFirmaDocumento.

Build: 620/620 tests. BUILD SUCCESS.

### 8C-2 cerrado [2026-07-01]

Plantillas documentales in-memory implementadas.

#### Java creado

- domain/model/FalDocumentoPlantilla — modelo de plantilla documental. Sin idTalonario, politicaNumeracionId ni claseTalonario.
- domain/model/FalDocumentoPlantillaFirmaReq — requisito de firma de plantilla. Sin relacion con FalDocumentoFirmaReq (slice 8C-4).
- domain/exception/DocumentoPlantillaNoEncontradaException
- domain/exception/DocumentoPlantillaInvalidaException
- domain/exception/DocumentoPlantillaDuplicadaException
- 
epository/DocumentoPlantillaRepository — interfaz: nextPlantillaId, nextFirmaReqId, guardar, buscarPorId, buscarPorCodigo, listar, buscarPorAccion, buscarActivasPorAccion, guardarFirmaReq, listarFirmaReqPorPlantilla, buscarFirmaReqPorId.
- 
epository/memory/InMemoryDocumentoPlantillaRepository — implementacion in-memory con ConcurrentHashMap y AtomicLong.
- pplication/command/CrearDocumentoPlantillaCommand
- pplication/command/AgregarFirmaReqPlantillaCommand
- pplication/command/ActivarDocumentoPlantillaCommand
- pplication/command/DesactivarDocumentoPlantillaCommand
- pplication/service/DocumentoPlantillaService — crear, agregarFirmaReq, activar, desactivar, obtener, listar, listarPorAccion, listarActivasPorAccion, listarFirmaReq.
- web/DocumentoPlantillaController — 8 endpoints REST bajo /api/faltas/documentos/plantillas.
- web/dto/CrearDocumentoPlantillaRequest
- web/dto/AgregarFirmaReqPlantillaRequest
- web/dto/DocumentoPlantillaResponse
- web/dto/DocumentoPlantillaFirmaReqResponse
- 	est/application/DocumentoPlantillaTest — 37 tests nuevos.

#### Validaciones implementadas

- Codigo unico por plantilla.
- siRequiereNumeracion=false => momentoNumeracionDocu=NO_APLICA.
- siRequiereNumeracion=true  => momentoNumeracionDocu != NO_APLICA.
- fhVigHasta posterior o igual a fhVigDesde.
- Activacion: vigencia no vencida, coherencia numeracion, reglas por TipoFirmaReq:
  - NO_REQUIERE: cero requisitos obligatorios activos.
  - FIRMA_MULTIPLE: al menos dos requisitos obligatorios activos.
  - Resto: al menos un requisito obligatorio activo.
- seqFirmaReq y rolFirmaReq > 0.
- seqFirmaReq unico por plantilla entre requisitos activos.

#### Guardrails preservados

- FalDocumentoPlantilla: sin idTalonario, politicaNumeracionId, claseTalonario.
- FalDocumentoFirmaReq: no implementado (slice 8C-4).
- Generacion documental desde plantilla: no implementada (slice 8C-3).
- Numeracion documental: no implementada.
- MomentoNumeracionDocu: solo en plantillas, no en FalActa.
- FIRMA_MIXTA: no existe.
- Sin MariaDB/JDBC. Sin Angular.

#### Build

- 657/657 tests (37 nuevos). BUILD SUCCESS.

#### Pendientes proximos

- 8C-3: Generar FalDocumento desde plantilla activa, snapshot con FalDocumentoFirmaReq.
- 8C-4: Implementar FalDocumentoFirmaReq (snapshot desde FalDocumentoPlantillaFirmaReq).
- 8C-6A completado: ResultadoFirmaInfractor en FalActa, FalActaEvidencia, TipoEvidenciaActa con FIRMA_OLOGRAFA_INFRACTOR (2026-07-01).

---

## Delta 8C-3 � Generacion de FalDocumento snapshot desde plantilla [2026-07-01]

### Objetivo

Implementar creacion de FalDocumento concreto desde plantilla activa.

### Java implementado

#### Nuevos archivos

- pplication/command/GenerarDocumentoDesdePlantillaCommand (record: idActa, plantillaId, idUserAlta)
- web/dto/GenerarDocumentoDesdePlantillaRequest (record: idActa, plantillaId, idUserAlta)
- web/dto/DocumentoResponse (record con factory rom(FalDocumento))
- 	est/application/DocumentoGeneracionDesdePlantillaTest � 28 tests nuevos

#### Archivos modificados

- domain/model/FalDocumento � segundo constructor (id, idActa, tipoDocu, fechaGeneracion, descripcion, estadoDocu, tipoFirmaReq, plantillaId)
- pplication/service/DocumentoService � campo DocumentoPlantillaRepository inyectado + metodo generarDesdePlantilla(GenerarDocumentoDesdePlantillaCommand)
- web/DocumentoController � endpoint POST /api/faltas/documentos/desde-plantilla, handlers para DocumentoPlantillaNoEncontradaException y DocumentoPlantillaInvalidaException
- 7 tests existentes � constructor DocumentoService actualizado con 
ew InMemoryDocumentoPlantillaRepository()

### Reglas implementadas

- Generacion desde plantilla activa y vigente.
- Validacion: idActa, plantillaId, idUserAlta obligatorios.
- Validacion: acta debe existir.
- Validacion: plantilla debe existir, estar activa, y vigente.
- Documento nace EstadoDocu.BORRADOR.
- Snapshot copia: tipoDocu, tipoFirmaReq, plantillaId, idActa.
- Numeracion documental: nroDocu/idTalonario/nroTalonarioUsado = null.
- Plantilla con momentoNumeracionDocu = AL_CREAR: rechazada (numeracion documental no implementada todavia).

### Guardrails preservados

- FalDocumentoFirmaReq: no implementado.
- Materializacion de requisitos de firma: no implementada.
- Numeracion documental: no implementada.
- Consumo de talonario documental: no implementado.
- PDF/storage: no generado.
- Sin MariaDB/JDBC. Sin Angular.

### Build

- 685/685 tests (28 nuevos). BUILD SUCCESS.

### Pendientes proximos

- 8C-4: Implementar FalDocumentoFirmaReq (snapshot desde FalDocumentoPlantillaFirmaReq).
- 8C-5: Enviar documento a firma (BORRADOR -> PENDIENTE_FIRMA, materializar firma_req).
- Slice posterior: Numeracion documental (resolver num_talonario_ambito clase DOCUMENTO, generar nroDocu).

## Micro-slice 8C-4 � FalDocumentoFirmaReq snapshot de requisitos de firma [COMPLETADO 2026-07-01]

### Diagnostico modelo

- `fal_documento_firma_req` verificada en modelo productivo (Seccion 5.4).
- PK productiva compuesta: `(id_docu, seq_firma_req)`. In-memory usa id sintetico.
- `estado_firma_req` tiene catalogo PROPIO, distinto de `estado_firma`:
  - 1: PENDIENTE, 2: FIRMADO, 3: ANULADO, 4: VENCIDO, 5: REEMPLAZADO.
  - Implementado como enum `EstadoFirmaReq` separado de `EstadoFirma`.
- `plantilla_firma_req_id` no existe en el modelo productivo de `fal_documento_firma_req`. No incluido.

### Nuevos archivos Java

- `domain/enums/EstadoFirmaReq` - enum propio (PENDIENTE/FIRMADO/ANULADO/VENCIDO/REEMPLAZADO).
- `domain/model/FalDocumentoFirmaReq` - snapshot de requisito de firma por documento concreto.
- `domain/exception/DocumentoFirmaReqYaMaterializadaException`
- `repository/DocumentoFirmaReqRepository` - interfaz.
- `repository/memory/InMemoryDocumentoFirmaReqRepository` - impl con AtomicLong + ConcurrentHashMap.
- `application/command/MaterializarFirmaReqDocumentoCommand` - record.
- `application/service/DocumentoFirmaReqService` - materializacion y consulta.
- `web/DocumentoFirmaReqController` - endpoints REST.
- `web/dto/MaterializarFirmaReqDocumentoRequest` - request DTO.
- `web/dto/DocumentoFirmaReqResponse` - response DTO.
- `test/.../DocumentoFirmaReqTest` - 30 tests.

### Archivos modificados

- `test/.../FirmanteTest` - guardrail tests de `FalDocumentoFirmaReq` y `EstadoFirmaReq` actualizados: de isFalse a isTrue (clases ahora existen en 8C-4).

### Endpoints implementados

- `POST /api/faltas/documentos/{documentoId}/firma-req/materializar`
- `GET /api/faltas/documentos/{documentoId}/firma-req`
- `GET /api/faltas/documentos/firma-req/{id}`

### Reglas implementadas

- Snapshot de seq/rol/mecanismo/obligatoriedad desde FalDocumentoPlantillaFirmaReq.
- Estado inicial de cada requisito: EstadoFirmaReq.PENDIENTE.
- No duplicar materializacion (DocumentoFirmaReqYaMaterializadaException si ya existe).
- NO_REQUIERE sin obligatorios: devuelve lista vacia.
- NO_REQUIERE con obligatorios activos: falla por inconsistencia.
- FIRMA_AUTORIDAD/FIRMA_INSPECTOR/FIRMA_INTERNA/FIRMA_DIGITAL: al menos un requisito obligatorio activo.
- FIRMA_MULTIPLE: al menos dos requisitos obligatorios activos.
- No cambia estadoDocu del documento.
- Documento sigue BORRADOR.
- No crea FalDocumentoFirma.
- No valida firmantes habilitados.
- No numera documento.
- No consume talonario.

### Guardrails preservados

- BORRADOR -> PENDIENTE_FIRMA: no implementado (slice 8C-5).
- Numeracion documental: no implementada.
- Consumo de talonario documental: no implementado.
- FalDocumentoFirma: no creado en este slice.
- PDF/storage: no generado.
- FIRMA_MIXTA: no existe.
- Sin MariaDB/JDBC. Sin Angular.

### Build

- 715/715 tests (30 nuevos). BUILD SUCCESS.

### Proximos pendientes recomendados

1. **8C-5**: Enviar documento a firma: validar BORRADOR, materializar firma_req si no existe, BORRADOR -> PENDIENTE_FIRMA. Si momentoNumeracionDocu=AL_ENVIAR_A_FIRMA, evaluar slice de numeracion documental previo o simultaneo.
2. **Slice de numeracion documental**: resolver num_talonario_ambito con clase DOCUMENTO, generar nroDocu, registrar movimiento.
3. **Slice de firma real**: registrar FalDocumentoFirma, validar firmante habilitado, cumplir requisito, cerrar documento como FIRMADO cuando corresponda.

---

## 8C-5A — Numeracion documental reusable

**Estado**: CERRADO

**Fecha**: 2026-07-01

### Alcance

Implementacion de numeracion documental reusable usando el sistema de talonarios existente con `clase_talonario = DOCUMENTO`.

Este slice implementa el servicio reusable de numeracion que los siguientes slices (8C-5B, firma real) consumiran cuando corresponda segun `momentoNumeracionDocu`.

### Archivos nuevos

- `application/command/NumerarDocumentoCommand` - comando publico para numerar un documento.
- `application/command/EmitirNumeroDocumentoCommand` - comando interno para TalonarioService.
- `application/result/NumeroDocumentoEmitidoResponse` - respuesta de emision documental.
- `web/dto/NumerarDocumentoRequest` - request HTTP para numerar manualmente.
- `test/.../DocumentoNumeracionTest` - 31 tests del slice.

### Archivos modificados

- `repository/DependenciaRepository` - agregado `findByCodDep(String codDep)`.
- `repository/memory/InMemoryDependenciaRepository` - implementado `findByCodDep`.
- `application/service/TalonarioService` - agregado `emitirNumeroDocumento(EmitirNumeroDocumentoCommand)`.
- `application/service/DocumentoService` - constructor actualizado (+TalonarioService, +DependenciaRepository); agregado `numerarDocumento(NumerarDocumentoCommand)`; `generarDesdePlantilla` auto-numera si `momentoNumeracionDocu = AL_CREAR`.
- `web/DocumentoController` - agregado endpoint `POST /api/faltas/documentos/{documentoId}/numerar`.
- 9 archivos de test existentes: constructores de `DocumentoService` actualizados con los nuevos parametros.

### Endpoints implementados

- `POST /api/faltas/documentos/{documentoId}/numerar`

### Reglas implementadas

- Numeracion documental reusable via `num_talonario_ambito` con `clase_talonario = DOCUMENTO`.
- Usa `tipoDocu` del documento para filtrar ambitos.
- Usa `dependencia/version` desde `FalActa.idDependencia` (codDep) -> `findByCodDep` -> `idDep/verDep`.
- Genera correlativo incremental dentro del talonario.
- Construye `nroDocu` visible usando la politica (`num_politica.formatoVisible`).
- Guarda `nroDocu`, `idTalonario`, `nroTalonarioUsado` en `FalDocumento`.
- Registra `NumTalonarioMovimiento` con `documentoId` y `actaId = null`.
- Valida que no se numere dos veces el mismo documento.
- No permite elegir talonario desde request.
- No consume talonario ACTA.
- `AL_CREAR`: auto-numera al crear documento desde plantilla (en `generarDesdePlantilla`).
- `AL_ENVIAR_A_FIRMA`: el servicio `numerarDocumento` queda disponible para que 8C-5B lo invoque antes de cambiar a `PENDIENTE_FIRMA`.
- `AL_FIRMAR`, `AL_EMITIR`: sin implementar; el servicio reusable ya existe.
- `NO_APLICA`: falla controladamente si se intenta numerar.

### Decision funcional corregida: momento_numeracion_docu

- `AL_ENVIAR_A_FIRMA`: numerar ANTES de cambiar a `PENDIENTE_FIRMA` (8C-5B lo implementara).
- `AL_FIRMAR`: numerar antes de hash/firma digital (slice de firma real lo implementara).
- `AL_CREAR`: numerar al crear (implementado en este slice).
- NO queda bloqueado como regla funcional en ningun valor.

### Guardrails preservados

- No envio a firma. No firma real. No `FalDocumentoFirma`. No `FalDocumentoFirmaReq` al numerar.
- No PDF/storage. No hash. No notificacion.
- No MariaDB/JDBC. No Angular.
- `FIRMA_MIXTA`: no existe.

### Build

- 746/746 tests (31 nuevos). BUILD SUCCESS.

### Proximos pendientes recomendados

1. **8C-5B — Enviar documento a firma**: si `momentoNumeracionDocu = AL_ENVIAR_A_FIRMA`, llamar `numerarDocumento` antes de `BORRADOR -> PENDIENTE_FIRMA`; si `NO_APLICA`, enviar sin numerar; si `AL_FIRMAR`, enviar sin numerar y numerar en acto de firma; si `AL_CREAR` y no esta numerado, fallar por inconsistencia.
2. **Slice de firma real**: registrar `FalDocumentoFirma`, validar firmante habilitado, si `momentoNumeracionDocu = AL_FIRMAR` numerar antes de hash/firma digital, marcar `FalDocumentoFirmaReq` como FIRMADO, cerrar documento como FIRMADO cuando todos los obligatorios esten firmados.
3. **Slice de convalidacion de documento escaneado**: incorporar escaneo, convalidar firma existente, registrar evidencia/storage, cumplir requisito si corresponde.

## 8C-5B - Enviar documento a firma con numeracion automatica cuando corresponda

**Estado**: CERRADO

**Fecha**: 2026-07-01

### Alcance

Implementacion del envio de documento a firma: transicion BORRADOR -> PENDIENTE_FIRMA.

Integra la numeracion documental reusable de 8C-5A: si momentoNumeracionDocu = AL_ENVIAR_A_FIRMA,
llama automaticamente a numerarDocumento antes de cambiar el estado.

### Archivos nuevos

- application/command/EnviarAFirmaCommand - comando publico para enviar documento a firma.
- web/dto/EnviarAFirmaRequest - request HTTP para el endpoint.
- test/.../DocumentoEnvioFirmaTest - 20 tests del slice.

### Archivos modificados

- domain/model/FalDocumento - agregado helper esBorrador().
- application/service/DocumentoService - constructor actualizado (+DocumentoFirmaReqRepository); agregado enviarAFirma(EnviarAFirmaCommand) y helper privado materializarFirmaReqDesdeInternal.
- web/DocumentoController - agregado endpoint POST /api/faltas/documentos/{id}/enviar-a-firma.
- 10 archivos de test existentes: constructores de DocumentoService actualizados con DocumentoFirmaReqRepository.

### Endpoints implementados

- POST /api/faltas/documentos/{documentoId}/enviar-a-firma

### Reglas implementadas

- Validacion: documento debe estar en BORRADOR.
- Validacion: documento debe tener plantillaId.
- Validacion: tipoFirmaReq != NO_REQUIERE (documentos sin firma no pasan por PENDIENTE_FIRMA).
- AL_ENVIAR_A_FIRMA: llama numerarDocumento (8C-5A) antes de cambiar estado.
- AL_CREAR + nroDocu == null: inconsistencia, falla con PrecondicionVioladaException.
- AL_FIRMAR, AL_EMITIR, NO_APLICA: envia sin numerar.
- Materializacion automatica de firma_req desde plantilla si no existia previamente.
- Sin re-materializacion si firma_req ya estaba materializada.

### Guardrails preservados

- No firma real. No FalDocumentoFirma. No hash. No PDF.
- No MariaDB/JDBC. No Angular.
- Estado del documento permanece BORRADOR si la numeracion falla (AL_ENVIAR_A_FIRMA).

### Build

- 766/766 tests (20 nuevos). BUILD SUCCESS.

---

## Entrada 8C-6B-0 (2026-07-01)

**Tipo:** Diagnostico y documentacion. Sin cambios Java ni MariaDB.

### Diagnostico

Se realizo diagnostico tecnico completo del estado de la firma real documental previo a la implementacion.

### FalDocumentoFirma - delta detectado

La implementacion in-memory actual de `FalDocumentoFirma` es naive/simplificada:

| Campo modelo MariaDB | Estado Java actual |
|---|---|
| `id` (BIGINT) | `String` UUID - OK en memoria; JDBC necesitara BIGINT |
| `documento_id` | `idDocumento` Long - OK |
| `id_acta` | **NO existe en modelo**; Java tiene `idActa` como campo extra |
| `id_firmante` (nullable) | ausente |
| `ver_firmante` (nullable) | ausente |
| `seq_firma_req` (nullable) | ausente |
| `tipo_firma` (SMALLINT) | `tipoFirma` String - debe ser Short/enum |
| `estado_firma` | `estadoFirma` EstadoFirma - OK |
| `id_user_firma` (nullable) | ausente |
| `rol_firmante` (nullable) | ausente |
| `nombre_firmante` (nullable) | `firmante` String - OK parcial |
| `fh_firma` | `fechaFirma` - OK |
| `hash_documento` (nullable) | ausente |
| `referencia_firma_ext` (nullable) | ausente |
| `mensaje_error` (nullable) | `observaciones` - renombrar |
| `fh_alta` | ausente |
| `id_user_alta` | ausente |

La refactorizacion completa se delega a 8C-6B-1.

### FalDocumentoFirmaReq - estado

Bien alineado. `ordenFirma` presente pero siempre nulo al materializar (plantilla no tiene orden_firma).
`idFirma` es Long; debe alinearse con el tipo del nuevo `id` de `FalDocumentoFirma` en 8C-6B-1.

### DocumentoFirmaRepository - gap

`buscarPorDocumento` retorna `Optional` (supone 1 firma). El modelo admite N firmas por documento.
Cambiar a `List` en 8C-6B-1.

### firmarDocumento actual - estado

Implementacion naive usada para avanzar estado en tests de flujo completo.
No valida firmante habilitado, no respeta ordenFirma, no actualiza FalDocumentoFirmaReq, no maneja AL_FIRMAR.
Se reemplaza en 8C-6B-1.

### numerarDocumento - compatibilidad AL_FIRMAR

Confirmado: `numerarDocumento` NO restringe por `estadoDocu`. Puede llamarse con documento en `PENDIENTE_FIRMA`.
No se requiere ajuste. La logica de cuando llamarlo vive en el servicio de firma real.

### TipoFirma - enum pendiente

Catalogo cerrado en modelo: DIGITAL=1, ELECTRONICA=2, OLOGRAFA=3, SISTEMA=4.
Enum Java `TipoFirma` no existe todavia. Se crea en 8C-6B-1.

### Proximo slice

8C-6B-1: Registrar firma real documental. Contrato documentado en spec-as-source 99.



---

## 8C-6B-1 — Firma real documental (2026-07-01) — CERRADO

### FalDocumentoFirma - refactorizado y alineado

Clase refactorizada desde modelo naive (record con String id) a clase Java alineada con MariaDB:
- id: Long (antes: String UUID)
- idActa: eliminado (derivable desde FalDocumento.idActa)
- Agregados: seqFirmaReq, idFirmante, erFirmante, idUserFirma, 
olFirmante, 
ombreFirmante, TipoFirma tipoFirma, hashDocumento, 
eferenciaFirmaExt, storageKey, mensajeError, hFirma, hAlta, idUserAlta
- Validaciones de entidad en constructor (id, idDocumento, tipoFirma, estadoFirma, fhFirma, fhAlta, idUserFirma, etc.)
- Si tipoFirma=DIGITAL: hashDocumento y referenciaFirmaExt obligatorios

### TipoFirma - enum creado

Enum r.gob.malvinas.faltas.core.domain.enums.TipoFirma:
- DIGITAL=1, ELECTRONICA=2, OLOGRAFA=3, SISTEMA=4

### FalDocumentoFirmaReq - marcarFirmado

Metodo marcarFirmado(Long idFirma, LocalDateTime fhFirma, Long idFirmanteAsig, short verFirmanteAsig) agregado.
Solo puede pasar de PENDIENTE a FIRMADO. Actualiza: estadoFirmaReq, idFirma, fhFirma, idFirmanteAsig, verFirmanteAsig.

### DocumentoFirmaRepository - alineado

Interface actualizada:
- Long nextId()
- FalDocumentoFirma guardar(FalDocumentoFirma firma)
- Optional<FalDocumentoFirma> buscarPorId(Long id)
- List<FalDocumentoFirma> buscarPorDocumento(Long idDocumento)
- Optional<FalDocumentoFirma> buscarPorDocumentoYSeq(Long idDocumento, short seqFirmaReq)
Implementacion in-memory usa AtomicLong y Map<Long, FalDocumentoFirma>.

### DocumentoService.registrarFirmaDocumental - implementado

Validaciones implementadas:
- documentoId, seqFirmaReq, idFirmante, tipoFirma, idUserFirma obligatorios
- Documento debe existir y estar en PENDIENTE_FIRMA con plantilla
- Requisito debe existir, estar activo y en PENDIENTE
- Firmante debe existir, tener version vigente y habilitacion activa para tipoDocu+rolFirmaReq
- Si req.mecanismoFirmaReq != null, habilitacion debe tener mismo mecanismo
- ordenFirma respetado: si req.ordenFirma != null, no puede haber obligatorio activo con orden menor pendiente
- Si momentoNumeracionDocu=AL_FIRMAR y doc no tiene nroDocu: numerar antes de firmar
- Si tipoFirma=DIGITAL: hashDocumento y referenciaFirmaExt obligatorios

### FalDocumento.marcarFirmado - implementado

Metodo marcarFirmado() agrega transicion PENDIENTE_FIRMA -> FIRMADO.
Se invoca cuando todos los requisitos obligatorios activos quedan FIRMADO.

### Endpoint REST

POST /api/faltas/documentos/{documentoId}/firmar-real
Request: RegistrarFirmaDocumentalRequest
Response: DocumentoFirmaResponse

### Gap pendiente post-8C-6B-1

- firmarDocumento (legacy) mantiene implementacion naive para compatibilidad con tests de flujo.
- ordenFirma siempre null en reqs materializados (AgregarFirmaReqPlantillaCommand no tiene ese campo).
  Para probar orden: crear FalDocumentoFirmaReq directamente en el repo con ordenFirma seteado.
- No PDF real, no storage real, no proveedor de firma digital.
- hashDocumento aceptado como input simulado (no calculado sobre PDF).

---

## 8C-6C-0 -- Diagnostico emision formal (2026-07-01)

### Campos ausentes en FalDocumento vs modelo MariaDB (deuda activa)

Durante el diagnostico 8C-6C-0 se identificaron dos campos del modelo MariaDB productivo (al_documento) que no
existen todavia en FalDocumento.java:

| Campo MariaDB  | Tipo             | Null | Descripcion              | Estado Java                |
|----------------|------------------|------|--------------------------|----------------------------|
| hash_docu    | VARCHAR(128)     | SI   | Hash de integridad       | AUSENTE - agregar en 8C-6C-1 |
| h_generacion| DATETIME(6)      | SI   | Fecha de emision formal  | AUSENTE - agregar en 8C-6C-1 |

**Distincion importante:**
- FalDocumento.fechaGeneracion (final, LocalDateTime) mapea a h_alta DATETIME(6) NOT NULL (alta tecnica).
- h_generacion del modelo es nullable y representa la fecha de emision formal, no el alta tecnica.
- Son dos campos distintos con semantica diferente. echaGeneracion en Java no cubre h_generacion.

**Deuda para Slice 9/JDBC:**
- al_documento.hash_docu VARCHAR(128) NULL: ya definido en modelo productivo. Solo falta en Java.
- al_documento.fh_generacion DATETIME(6) NULL: ya definido en modelo productivo. Solo falta en Java.
- Los campos se agregan en in-memory en 8C-6C-1 antes de la migracion JDBC.

### Estado de EstadoDocu.EMITIDO

- EstadoDocu.EMITIDO (codigo=2) esta declarado en el enum Java desde 8C-0D.
- NO esta siendo usado en ningun servicio actualmente (confirmado en diagnostico 8C-6C-0).
- Se implementa en 8C-6C-1 como estado terminal del flujo documental.

### Flujo de estados documentales confirmado

`
NO_REQUIERE:   BORRADOR -> EMITIDO
Con firma:     BORRADOR -> PENDIENTE_FIRMA -> FIRMADO -> EMITIDO
Adjunto:       ADJUNTO (flujo de convalidacion 8C-6D)
`

### TipoEventoActa para emision documental

Verificar si existe o agregar DOCEMI en el catalogo TipoEventoActa para el evento de emision formal.
Pendiente de confirmacion en 8C-6C-1.



**Confirmado en diagnostico 8C-6C-0:** `DOCEMI` NO existe en `TipoEventoActa`.
El enum tiene `DOCGEN` (documento generado) y `DOCFIR` (documento firmado) pero NO `DOCEMI`.
Agregar en 8C-6C-1: `DOCEMI("DOCEMI", "Documento emitido formalmente")`.

---

## 8C-6C-1 -- Emision formal in-memory (2026-07-01)

### Estado del build

855/855 tests passing. BUILD SUCCESS.
39 tests nuevos agregados (DocumentoEmisionFormalTest).

### Cambios Java

#### FalDocumento

- Agregado campo `hashDocu` (String, nullable): mapea a `hash_docu VARCHAR(128) NULL` en MariaDB.
- Agregado campo `fhGeneracion` (LocalDateTime, nullable): mapea a `fh_generacion DATETIME(6) NULL` en MariaDB.
- Agregado metodo `marcarEmitido(String storageKey, String hashDocu, LocalDateTime fhGeneracion)`.
- Agregado helper `estaEmitido()`.
- Agregado getters/setters para `hashDocu` y `fhGeneracion`.
- `fechaGeneracion` (final, mapea a `fh_alta`) no modificado: conserva su semantica de alta tecnica.

#### TipoEventoActa

- Agregado `DOCEMI("DOCEMI", "Documento emitido formalmente")`.
- `DOCGEN` y `DOCFIR` intactos.

#### Nuevos archivos

- `EmitirDocumentoCommand` (command)
- `EmitirDocumentoRequest` (web DTO)
- `DocumentoEmisionFormalTest` (test: 39 casos)

#### Archivos actualizados

- `DocumentoResponse`: agrega `storageKey`, `hashDocu`, `fhGeneracion`.
- `DocumentoService`: agrega `emitirDocumento(EmitirDocumentoCommand)`.
- `DocumentoController`: agrega endpoint `POST /api/faltas/documentos/{documentoId}/emitir`.

### Reglas implementadas

- `BORRADOR -> EMITIDO` para tipoFirmaReq=NO_REQUIERE.
- `FIRMADO -> EMITIDO` para documentos con firma requerida.
- `PENDIENTE_FIRMA` no puede emitirse directamente.
- `AL_EMITIR`: numera automaticamente antes de consolidar storage/hash.
- `AL_CREAR`, `AL_ENVIAR_A_FIRMA`, `AL_FIRMAR`: doc debe llegar ya numerado; si no, falla por inconsistencia.
- `NO_APLICA`: emite sin numerar.
- `siGeneraPdf=true`: storageKey y hashDocu obligatorios.
- `siGeneraPdf=false`: storageKey y hashDocu pueden ser null.
- `fhGeneracion` se setea siempre al momento de emitir.
- Registro de evento `DOCEMI` al emitir.
- No notificacion automatica en este slice.
- No PDF/storage real.
- No firma nueva.
- No MariaDB/JDBC.
- No Angular.

### Gap pendiente para Slice 9/JDBC

- `fal_documento.hash_docu` y `fal_documento.fh_generacion`: campos ya definidos en modelo MariaDB productivo.
  En in-memory: implementados y funcionando. En JDBC: mapear en repositorio y queries.
- Columnas a incluir en SELECT y UPDATE de fal_documento.
- Antes de implementar MariaDB/JDBC, reconciliar este delta con el modelo productivo.

---

## 8C-6D-0 -- Diagnostico y contrato de convalidacion de documento escaneado

**Fecha:** 2026-07-01
**Estado:** Solo diagnostico/documentacion. Sin cambios funcionales Java. Build base confirmado: 855/855 tests passing.

---

### Diagnostico: EstadoDocu.ADJUNTO

**Existe en enum:** Si. EstadoDocu.ADJUNTO((short) 5). Declarado en modelo productivo desde el inicio.

**Uso actual en Java:**
- SnapshotRecalculador.java: trata ADJUNTO equivalente a FIRMADO/EMITIDO en el calculo 	odosDocsFirmados.
  Un documento ADJUNTO ya cuenta como pieza documental finalizada para el snapshot del acta.
- PagoVoluntarioService.java: menciona adjunto en comentarios sobre PAGCMP (comprobante de pago).
- No hay flujo activo de creacion de documentos con estado ADJUNTO inicial.
- No hay tests que cubran el camino de creacion de adjuntos.

**Contrato definido:**

| Propiedad | Valor para ADJUNTO |
|---|---|
| Semantica | Documento externo incorporado al expediente |
| Estado inicial | ADJUNTO directo (no pasa por BORRADOR) |
| storageKey | Obligatorio |
| hashDocu | Obligatorio |
| hGeneracion | Seteado al momento de incorporacion (fecha de alta del registro) |
| echaGeneracion | Fecha tecnica de alta del registro en base (h_alta) |
| 
roDocu | Null por defecto. Sin numeracion automatica al incorporar |
| 	ipoFirmaReq | NO_REQUIERE por defecto. Puede ser otro si se configura explicitamente |
| plantillaId | Null para adjuntos externos (aceptado por modelo MariaDB) |
| 	ipoDocu | Cualquier tipo valido del catalogo tipo_docu (CONSTANCIA, ANEXO, OTRO, etc.) |

**Diferencia clave con BORRADOR:**
BORRADOR es un documento en preparacion generado por el sistema. ADJUNTO es un artefacto externo ya existente
que se incorpora al expediente. No pasa por plantilla, no pasa por firma digital, no pasa por numeracion automatica.

---

### Diagnostico: plantilla para adjuntos

**Opcion recomendada: Opcion B -- Adjuntos sin plantilla obligatoria.**

**Justificacion:**
El modelo MariaDB productivo ya establece explicitamente:
> plantilla_id es obligatorio para docs generados por sistema; nullable para documentos externos adjuntos o migrados.

Por lo tanto plantillaId = null es un valor valido y esperado para documentos adjuntos.
No hace falta crear plantillas especiales para incorporar documentos externos.

**Consecuencia tecnica para 8C-6D-1:**
- DocumentoService.emitirDocumento() hoy exige plantillaId != null. Ese control es correcto
  para el flujo de emision de documentos generados por sistema.
- El nuevo flujo de incorporacion de adjuntos es un metodo separado (incorporarDocumentoEscaneado)
  que NO pasa por emitirDocumento(). No requiere modificar la validacion existente.

---

### Diagnostico: storage/hash de escaneo

**Contrato:**

| Campo | Comportamiento para adjunto |
|---|---|
| FalDocumento.storageKey | Obligatorio. Clave del archivo escaneado en storage |
| FalDocumento.hashDocu | Obligatorio. Hash del archivo escaneado (integridad) |
| FalDocumento.fhGeneracion | Obligatorio. Fecha de incorporacion al sistema |
| FalDocumento.fechaGeneracion | Fecha tecnica del registro (h_alta). No modificar semantica |

**Sobre fhGeneracion vs fhIncorporacion:**
No se necesita campo nuevo hIncorporacion. El campo hGeneracion en MariaDB esta definido como nullable
con semantica funcional (no tecnica). Para adjuntos representa la fecha de incorporacion al expediente.
La distincion ya estaba capturada en el modelo existente.

**In-memory:**
- No se calcula hash real. Se recibe como input simulado.
- No se usa storage real. storageKey es una clave referencial simulada.
- Consistente con el contrato de 8C-6C-1 (mismos principios).

---

### Diagnostico: convalidacion de firma existente

**Modelo disponible para convalidacion:**

FalDocumentoFirma ya tiene todos los campos necesarios:

| Campo | Uso en convalidacion |
|---|---|
| 	ipoFirma = OLOGRAFA | Tipo de firma convalidada |
| 
eferenciaFirmaExt | Referencia a la firma visible dentro del escaneo (coordenada, anotacion, pagina) |
| storageKey | Clave del archivo escaneado (mismo que FalDocumento.storageKey) |
| hashDocumento | Hash del archivo escaneado (mismo que FalDocumento.hashDocu) |
| idFirmante | FK al firmante/convalidador institucional |
| erFirmante | Version vigente del firmante al momento de convalidar |
| 
olFirmante | Rol institucional del convalidador |
| 
ombreFirmante | Snapshot del nombre del convalidador |
| estadoFirma = FIRMADA | Estado final: convalidacion aceptada |

**Observacion tecnica sobre referenciaFirmaExt:**
Actualmente 
eferenciaFirmaExt solo es obligatorio para TipoFirma.DIGITAL en DocumentoService.
Para TipoFirma.OLOGRAFA en convalidacion, se recomienda que sea opcional pero utilizable
para indicar la ubicacion/referencia de la firma manuscrita en el documento escaneado.

**Sobre firmante/convalidador:**
El firmante/convalidador debe:
- Existir en FalFirmante / FalFirmanteVersion
- Tener version vigente a la fecha
- Tener habilitacion activa para el 	ipoDocu y 
olFirmaReq del documento si se quiere cumplir un requisito

Si el documento adjunto no tiene FalDocumentoFirmaReq, la firma se registra igualmente
como trazabilidad de convalidacion sin cumplir un requisito formal.

---

### Diagnostico: cumplir FalDocumentoFirmaReq con firma escaneada/convalidada

**Regla definida:**

Una firma escaneada/convalidada puede cumplir un FalDocumentoFirmaReq si:

1. El documento existe y tiene storageKey y hashDocu.
2. El requisito existe, esta activo (siActiva=true) y en estado PENDIENTE.
3. El firmante/convalidador existe con version vigente.
4. El firmante tiene habilitacion activa para el 	ipoDocu y 
olFirmaReq del requisito.
5. Si el requisito tiene mecanismoFirmaReq != null, debe ser compatible con olografa/convalidada.
6. Se crea FalDocumentoFirma con TipoFirma.OLOGRAFA.
7. Se marca el requisito como FIRMADO con el idFirma de la firma creada.
8. Si todos los obligatorios activos estan firmados, se cierra el documento como FIRMADO.

**Si no hay requisito documental:**
El escaneo puede incorporarse como ADJUNTO sin requisito de firma.
No se crea FalDocumentoFirmaReq. No se crea firma institucional.
El documento queda en ADJUNTO permanentemente hasta eventual anulacion/reemplazo.

**Observacion sobre el servicio actual:**
DocumentoService.registrarFirmaDocumental() siempre exige FalDocumentoFirmaReq previo.
El flujo de convalidacion en 8C-6D-1 puede reusar este metodo si el documento tiene requisitos.
Si el documento no tiene requisitos, se registra la firma como convalidacion simple
(metodo nuevo o rama especial segun lo que decida el slice de implementacion).

---

### Diagnostico: estados del documento escaneado

**Escaneo simple (sin requisito de firma):**
- Estado unico: ADJUNTO.
- No pasa a FIRMADO, no pasa a EMITIDO.
- Representa una incorporacion documental externa.
- Puede quedar en ADJUNTO indefinidamente.

**Escaneo con convalidacion de firma:**
- Incorporacion inicial: ADJUNTO.
- Convalidacion de firma: se registra FalDocumentoFirma con TipoFirma.OLOGRAFA.
- Si hay FalDocumentoFirmaReq y se cumple todos los obligatorios activos:
  el documento pasa de ADJUNTO a FIRMADO.
- No pasa automaticamente a EMITIDO. La emision es un flujo explicito separado.

**Sobre marcarFirmado():**
El metodo FalDocumento.marcarFirmado() actualmente solo acepta desde PENDIENTE_FIRMA.
Para convalidacion de adjunto, la transicion seria ADJUNTO -> FIRMADO, no PENDIENTE_FIRMA -> FIRMADO.
8C-6D-1 debe agregar esta transicion de forma controlada (nuevo metodo o ampliar precondicion).

---

### Diagnostico: eventos documentales para adjuntos

**Eventos actuales:**
- DOCGEN: Documento generado (por sistema, desde plantilla)
- DOCFIR: Documento firmado
- DOCEMI: Documento emitido formalmente

**Eventos no existentes:**
- DOCADJ (Documento adjuntado/incorporado): NO EXISTE en TipoEventoActa.
- DOCCON (Documento convalidado): NO EXISTE en TipoEventoActa.

**Recomendacion para 8C-6D-1:**
Agregar DOCADJ como nuevo evento con valor real de trazabilidad:
representa la incorporacion de un documento externo al expediente, distinguible de DOCGEN
(que es generacion por sistema). Este evento tiene valor auditoriable real.

Para convalidacion de firma, reutilizar DOCFIR es semanticamente compatible
(registro de que una firma fue aceptada/convalidada sobre el documento).
Solo agregar DOCCON si la distincion entre firma real y convalidacion es necesaria
para la bandeja o el audit trail. Decisor: slice 8C-6D-1.

**Propuesta minima para 8C-6D-1:**
- DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente"): nuevo, agregar.
- DOCFIR: reutilizar para convalidacion de firma en adjunto.
- DOCCON: reservar para slice futuro si se necesita distincion auditoria.

---

### Contrato para 8C-6D-1: incorporar documento escaneado

**Command:** IncorporarDocumentoEscaneadoCommand

Campos obligatorios:
- idActa: Long
- 	ipoDocu: TipoDocu
- storageKey: String
- hashDocu: String
- idUserAlta: String

Campos opcionales:
- descripcion: String
- plantillaId: Long (null para externos puros)

**Validaciones:**
1. Acta existe.
2. 	ipoDocu valido (enum valido, no null).
3. storageKey no null ni en blanco.
4. hashDocu no null ni en blanco.
5. idUserAlta no null ni en blanco.
6. Estado inicial: ADJUNTO directo.
7. hGeneracion seteado al momento de la llamada.
8. 	ipoFirmaReq = NO_REQUIERE por defecto.
9. plantillaId = null si no se provee; no validar plantilla si es null.
10. Sin numeracion automatica.
11. Sin firma automatica.
12. Sin emision automatica.
13. Registrar evento DOCADJ.
14. Recalcular snapshot.

---

### Contrato para 8C-6D-1: convalidar firma en documento escaneado

**Command:** ConvalidarFirmaEscaneadaCommand

Campos obligatorios:
- documentoId: Long
- idFirmante: Long
- 	ipoFirma: TipoFirma (debe ser OLOGRAFA para esta operacion)
- idUserAlta: String

Campos opcionales:
- seqFirmaReq: Short (null si no hay requisito formal)
- 
eferenciaFirmaExt: String (referencia a firma visible en escaneo)
- storageKey: String (si se provee, apunta al archivo escaneado)
- hashDocumento: String (hash del archivo escaneado)

**Validaciones:**
1. Documento existe.
2. Documento tiene storageKey y hashDocu.
3. 	ipoFirma es OLOGRAFA (o compatible con convalidacion).
4. Firmante existe y tiene version vigente.
5. Si seqFirmaReq no null:
   a. Requisito existe y esta activo y en estado PENDIENTE.
   b. Firmante tiene habilitacion activa para el 	ipoDocu y 
olFirmaReq del requisito.
   c. Si mecanismoFirmaReq != null, verificar compatibilidad.
6. Crear FalDocumentoFirma con TipoFirma.OLOGRAFA y estadoFirma = FIRMADA.
7. Si seqFirmaReq no null: marcar requisito como FIRMADO.
8. Si todos los obligatorios activos firmados Y documento en ADJUNTO:
   transicion ADJUNTO -> FIRMADO (nueva regla de transicion).
9. Registrar evento DOCFIR.
10. Recalcular snapshot.
11. No generar firma digital.
12. No recalcular PDF.
13. No notificar automaticamente.

---

### Pendiente tecnico para 8C-6D-1: transicion ADJUNTO -> FIRMADO

FalDocumento.marcarFirmado() actualmente solo acepta desde PENDIENTE_FIRMA.
8C-6D-1 debe ampliar o agregar una transicion marcarFirmadoDesdeAdjunto() que acepte desde ADJUNTO.
Alternativa: modificar marcarFirmado() para aceptar tambien desde ADJUNTO.

Antes de implementar, decidir si la transicion ADJUNTO -> FIRMADO es:
- Solo para adjuntos con requisito cumplido, o
- Para cualquier convalidacion de firma en adjunto.

**Recomendacion:** solo cuando hay requisito cumplido. Sin requisito, queda en ADJUNTO.

---

### Gap pendiente para Slice 9/JDBC

| Deuda | Detalle |
|---|---|
| DDL al_documento para ADJUNTO | plantilla_id NULL, storage_key NOT NULL, hash_docu NOT NULL para estado ADJUNTO. Restriccion a nivel aplicacion, no DDL. |
| Nuevo evento DOCADJ | Agregar al catalogo 	ipo_evento_acta en MariaDB si se confirma en 8C-6D-1. |
| Repositorio JDBC FalDocumento | Mapear storage_key, hash_docu, h_generacion para adjuntos. |


---

## 8C-6D-1 — Documento escaneado y convalidacion de firma olografa (CERRADO 2026-07-02)

### Implementado in-memory

#### TipoEventoActa
- DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente") agregado.

#### FalDocumento
- crearAdjunto(...): factory estatica para adjuntos externos. Estado inicial ADJUNTO. storageKey, hashDocu, hGeneracion obligatorios. plantillaId nullable.
- marcarFirmadoDesdeAdjunto(): transicion ADJUNTO -> FIRMADO. Solo acepta desde ADJUNTO.
- estaAdjunto(): helper booleano.

#### Nuevos commands/requests/responses
- IncorporarDocumentoEscaneadoCommand: idActa, 	ipoDocu, storageKey, hashDocu, idUserAlta, plantillaId nullable.
- ConvalidarFirmaEscaneadaCommand: documentoId, seqFirmaReq (Short nullable), idFirmante, idUserFirma, 
eferenciaFirmaExt.
- IncorporarDocumentoEscaneadoRequest: DTO con validaciones @NotNull/@NotBlank.
- ConvalidarFirmaEscaneadaRequest: DTO. seqFirmaReq nullable.
- ConvalidacionFirmaEscaneadaResponse: response para convalidacion simple sin requisito.
- ConvalidacionEscaneadaResultado: result record con documento + irma nullable.

#### DocumentoService
- incorporarDocumentoEscaneado(IncorporarDocumentoEscaneadoCommand): crea documento en ADJUNTO, registra DOCADJ, no numera, no emite, no firma.
- convalidarFirmaEscaneada(ConvalidarFirmaEscaneadaCommand): 
  - Sin seqFirmaReq: trazabilidad simple, DOCFIR event, documento permanece ADJUNTO. No crea FalDocumentoFirma.
  - Con seqFirmaReq: crea FalDocumentoFirma con TipoFirma.OLOGRAFA, marca requisito FIRMADO, si todos obligatorios activos firmados -> ADJUNTO -> FIRMADO, registra DOCFIR.
- Helpers privados extraidos: 
esolverFirmante, 
esolverVersionVigente, 
esolverHabilitacion, alidarMecanismo, alidarOrdenFirma.

#### DocumentoController
- POST /api/faltas/actas/{idActa}/documentos/escaneados: incorporar escaneado.
- POST /api/faltas/documentos/{documentoId}/convalidar-firma-escaneada: convalidar firma.

#### Tests
- DocumentoAdjuntoTest.java: 53 casos cubriendo incorporacion, convalidacion simple, convalidacion con requisito, validaciones, guardrails.

### Reglas implementadas
- Adjunto externo: sin plantilla obligatoria, storageKey/hashDocu/hGeneracion obligatorios.
- ADJUNTO como estado inicial para escaneados.
- DOCADJ para incorporacion.
- Convalidacion simple sin requisito: no inventa seqFirmaReq=0, solo DOCFIR event.
- Convalidacion con requisito: TipoFirma.OLOGRAFA, hashDocumento = doc.hashDocu, storageKey = doc.storageKey.
- Transicion ADJUNTO -> FIRMADO solo cuando todos los obligatorios activos estan FIRMADO.
- No firma digital, no emision automatica, no notificacion.
- No MariaDB/JDBC, no Angular.

### Deuda resuelta de 8C-6D-0
- Pendiente tecnico marcarFirmadoDesdeAdjunto(): implementado.
- Decision ADJUNTO -> FIRMADO solo con requisito cumplido: implementado correctamente.

### Gap pendiente para Slice 9/JDBC (actualizado)
| Deuda | Detalle |
|---|---|
| DDL para DOCADJ | Agregar al catalogo 	ipo_evento_acta en MariaDB. |
| Repositorio JDBC adjuntos | FalDocumento con storage_key, hash_docu, h_generacion para estado ADJUNTO. |
| plantillaId = NULL | Restriccion DDL: plantilla_id nullable para adjuntos externos. |


### Nota roadmap OCR/IA (8C-6D-1)

Los documentos escaneados podran ser procesados posteriormente por OCR o IA para extraccion
asistida de metadata. Esa extraccion debe generar datos sugeridos y auditables, y requerir
validacion humana antes de impactar datos del dominio.

Esta funcionalidad NO esta implementada en 8C-6D-1 ni en el modulo in-memory actual.
Queda como roadmap posterior, sin impacto en el circuito actual de incorporacion y convalidacion.


---

## 8C-6E - Auditoria pre-JDBC/MariaDB (2026-07-02) - CERRADO

**Tipo:** Auditoria y documentacion. Sin cambios funcionales Java.
**Build base:** 908/908 tests passing. BUILD SUCCESS.

### DELTA RECONCILIADO

A continuacion se clasifica el estado de cada item del delta:

#### CERRADOS (implementados en slices anteriores a 8C-6E)

IDs Long para actas (FalActa.id, FalDocumento.idActa, etc.): CERRADO en 8C-0B
FalDocumento alineado con modelo MariaDB: CERRADO en 8C-0A, 8C-0D
  - id: Long (era String UUID)
  - tipoDocu: TipoDocu enum (era TipoDocumento provisional)
  - estadoDocu: EstadoDocu enum (era EstadoDocumento provisional)
  - nroDocu: renombrado desde numeroVisible
  - tipoFirmaReq: agregado
  - plantillaId: agregado
  - idTalonario, nroTalonarioUsado: agregados
  - rolFirmaReq, mecanismoFirmaReq: removidos (van a FalDocumentoFirmaReq)
Enums documentales Java: CERRADO en 8C-1
  TipoDocu (12), EstadoDocu (7), EstadoFirma (6), TipoFirmaReq (6), AccionDocumental (11), MomentoNumeracionDocu (5)
  TipoDocumento, EstadoDocumento, EstadoFirmaDocumento: ELIMINADOS en 8C-1
  FIRMA_MIXTA: NO EXISTE. FIRMA_MULTIPLE implementado. Guardrails en tests.
FalDocumentoPlantilla y FalDocumentoPlantillaFirmaReq: CERRADO en 8C-2
Generacion FalDocumento desde plantilla: CERRADO en 8C-3
FalDocumentoFirmaReq snapshot: CERRADO en 8C-4
Numeracion documental reusable (todos los momentos): CERRADO en 8C-5A
Envio a firma con numeracion automatica: CERRADO en 8C-5B
ResultadoFirmaInfractor (5 valores) + FIRMA_OLOGRAFA_INFRACTOR evidencia: CERRADO en 8C-6A
  NO_REQUERIDA: NO EXISTE. Guardrail en tests.
  obsFirmaInfractor: NO EXISTE en FalActa. Guardrail en tests.
FalDocumentoFirma refactorizado: CERRADO en 8C-6B-1
  id: Long (era String UUID)
  idActa: ELIMINADO (derivable desde FalDocumento.idActa)
  seqFirmaReq, idFirmante, verFirmante, idUserFirma, rolFirmante, nombreFirmante: AGREGADOS
  tipoFirma: TipoFirma enum (era String naive)
  hashDocumento, referenciaFirmaExt, storageKey, mensajeError, fhFirma, fhAlta, idUserAlta: AGREGADOS
TipoFirma enum (DIGITAL=1, ELECTRONICA=2, OLOGRAFA=3, SISTEMA=4): CERRADO en 8C-6B-1
hashDocu, fhGeneracion en FalDocumento: CERRADO en 8C-6C-1
DOCEMI en TipoEventoActa: CERRADO en 8C-6C-1
Emision formal (BORRADOR/FIRMADO -> EMITIDO): CERRADO en 8C-6C-1
DOCADJ en TipoEventoActa: CERRADO en 8C-6D-1
incorporarDocumentoEscaneado (estado ADJUNTO): CERRADO en 8C-6D-1
convalidarFirmaEscaneada + TipoFirma.OLOGRAFA: CERRADO en 8C-6D-1
marcarFirmadoDesdeAdjunto (ADJUNTO -> FIRMADO): CERRADO en 8C-6D-1
Catalogos gestion externa alineados (ResultadoGestionExterna, ModoReingresoGestionExterna): CERRADO en 6D-0
FalFirmante + FalFirmanteVersion + FalFirmanteVersionHabilitacion in-memory: CERRADO en 8A-3D.1
Eventos prohibidos (ACTCER, PAGCON, DRVEXT, APELAC): NO EXISTEN. Guardrails activos.
Bloques legacy (D1_CAPTURA, D3_DOCUMENTAL, etc.): NO EXISTEN. Rechazados por BloqueActual.deCodigo().

#### ACTIVOS PRE-JDBC (a resolver en Slice 9)

version_row ausente en FalDocumento: Locking optimista; incorporar en JDBC.
num_politica DDL: fh_alta y id_user_alta deben agregarse al DDL productivo.
fal_acta.resultado_firma_infractor SMALLINT NOT NULL: Java alineado; DDL debe agregar la columna.
fal_acta_evidencia: seed tipo_evid=6 (FIRMA_OLOGRAFA_INFRACTOR) debe estar en DDL.
fal_documento DDL: nro_docu VARCHAR(30); renombrar requisito_firma->tipo_firma_req; agregar plantilla_id.
fal_documento_plantilla y fal_documento_plantilla_firma_req: tablas nuevas a crear en DDL.
Seeds catalogos documentales: tipo_firma_req, estado_docu, accion_documental, momento_numeracion_docu.
fal_firmante y fal_firmante_version: tablas nuevas a crear (Secciones 1.7, 1.8 del modelo MariaDB).
fal_firmante_version_habilitacion: tabla nueva a crear (Seccion 1.9).
fal_documento_firma_req: tabla nueva a crear (Seccion 5.4).
fal_documento_firma: columnas id_firmante, ver_firmante, seq_firma_req a agregar.
fal_firmante_version: rol_firmante pasa a nullable.
fal_acta_gestion_externa: monto_resultado DECIMAL(14,2) NULL a verificar en DDL.
FalObservacion/fal_observacion: no implementado; PAGAPR usa descripcion de evento transitoriamente.
IDs UUID String en apelaciones, fallos, pagos, bloqueantes, gestiones externas:
  Decision a tomar en Slice 9: BIGINT AUTO_INCREMENT vs mantener UUID CHAR(36).
generarDocumento/firmarDocumento legacy: activos, controlados, deprecar post-Slice 9.
fal_inspector_version campos firma: ELIMINADOS del modelo (decision 8F-11B). La firma maestra del inspector no se registra.

#### ROADMAP POST-JDBC

OCR/IA: extraccion asistida metadata; datos sugeridos; validacion humana. No implementado.
PDF/storage real: in-memory usa simulado. JDBC inicial puede mantener simulado.
Proveedor firma digital real: TipoFirma.DIGITAL existe; proveedor externo: slice posterior.
Notificacion automatica desde emision: siNotificable no dispara. Slice posterior.
Angular/frontend: no tocar hasta decision de etapa.
Validacion criptografica real de hash: slice posterior.
fal_inspector_version campos firma: para modulo de firma del inspector.
fal_observacion completo: slice posterior.

#### HISTORICO / NO APLICA

FALLO_ABSOLUTORIO, FALLO_CONDENATORIO_PAGADO, PAGO_VOLUNTARIO, PAGO_INFORMADO: reemplazados.
PAGCON, APELAC, ACTCER, DRVEXT: prohibidos. Guardrails activos.
D1_CAPTURA, D2_ENRIQUECIMIENTO, D3_DOCUMENTAL, D4_NOTIFICACION, D5_ANALISIS: prohibidos.
GESTIONAR_CONDENA_FIRME: eliminado; reemplazado por GESTIONAR_PAGO_CONDENA.
FalDocumentoFirma naive (String id, String firmante, String tipoFirma): reemplazado en 8C-6B-1.
TipoDocumento, EstadoDocumento, EstadoFirmaDocumento provisionales: eliminados en 8C-1.
numeroVisible: renombrado a nroDocu en 8C-0A.
tipoDocumento: renombrado a tipoDocu en 8C-0A.
estado (FalDocumento): renombrado a estadoDocu en 8C-0A.
requisito_firma: renombrado a tipo_firma_req en 8C-0D/8C-1.
rolFirmaReq, mecanismoFirmaReq en FalDocumento: removidos; estan en FalDocumentoFirmaReq.
Modos gestion externa viejos (REINGRESAR_A_ANALISIS, etc.): renombrados en 6D-0.

### Estado final del DELTA

El DELTA queda como historial controlado y fuente de referencia para Slice 9.
No debe usarse como lista ambigua de deudas activas.
Las deudas activas estan en la seccion ACTIVOS PRE-JDBC de este entry y del documento 101.

Ver documento completo de auditoria:
backend/api-faltas-core/docs/spec-as-source/101-auditoria-pre-jdbc-mariadb.md


---

## Decisiones cerradas en Slice 9-0 (2026-07-02)

Las siguientes decisiones quedaron cerradas en el slice de estrategia JDBC:

### PK interna: BIGINT vs UUID

CERRADO: BIGINT AUTO_INCREMENT como PK interna para todas las entidades productivas.

UUID/String tecnico solo como clave alternativa cuando se requiera correlacion offline,
integracion externa, importacion, sincronizacion o idempotencia.

No usar UUID String como PK interna salvo caso excepcional explicitamente justificado.

Regla DDL:
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY
  id_tecnico_offline VARCHAR(36) NULL UNIQUE   -- si aplica
  id_externo         VARCHAR(...)  NULL         -- si aplica
  id_origen          VARCHAR(...)  NULL         -- si aplica

### enum Java + SMALLINT en MariaDB

CERRADO: los catalogos cerrados del dominio NO se modelan como tablas fisicas administrables.

Persistencia: enum Java con codigo short + columna SMALLINT en MariaDB.

Sin tabla fisica para:
  EstadoDocu, TipoDocu, TipoFirmaReq, MomentoNumeracionDocu, TipoFirma,
  ResultadoFirmaInfractor, EstadoFirma, EstadoFirmaReq, TipoEventoActa, TipoEvidenciaActa,
  ClaseNumeracion, TipoTalonario, AlcanceTalonario, EstadoNumeroTalonario,
  MotivoAnulacionTalonario, EstadoAsignacionTalonario.

Sin seeds para esos enums (no hay tabla fisica que poblar).

### Descripciones SQL de enums cerrados

CERRADO: si se requiere descripcion SQL legible, usar funciones SQL deterministas,
vistas con CASE o CASE directo en consultas.
La fuente de verdad funcional sigue siendo el enum Java.
No crear tablas catalogo para enums cerrados.

### Primer piloto JDBC

CERRADO: DocumentoPlantillaRepository con JdbcClient.

### Proximo

Slice 9-1: Infraestructura JDBC base (MariaDB driver, JdbcClient, perfiles, test de conexion).

Ver plan completo: backend/api-faltas-core/docs/spec-as-source/102-slice-9-estrategia-jdbc-mariadb.md

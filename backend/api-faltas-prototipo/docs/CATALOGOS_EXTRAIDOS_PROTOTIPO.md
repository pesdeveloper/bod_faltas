# Catálogos extraídos del prototipo de Faltas

> Documento de extracción (read-only). No es DDL ni fuente de verdad por sí mismo.
> Objetivo: consolidar todos los catálogos / enums / estados / eventos / acciones
> reales del prototipo (backend + Angular) para decidir qué congelar en el modelo
> físico.

## Alcance y método

- **Backend leído:** `backend/api-faltas-prototipo/` (`src/main/java`, `src/test/java`, y su `docs/`).
- **Angular leído:** `apps/web-direccion-faltas/angular/src/app` (`core/**`, `features/**`), accedido vía terminal porque `.cursorignore` bloquea `apps/**` para las herramientas de lectura.
- **No leído (fuera de alcance / bloqueado):** `sql/**`, `docs/**` raíz, otros módulos backend, `infra/**`.
- **Prioridad de fuente:** backend funcional validado > tests > mocks/demo > Angular/UI > documentación.
- Los valores se reportan **tal cual aparecen en código** (sin convertir strings a números, sin renombrar).
- `backend/api-faltas-prototipo/docs/DOMINIO_REAL_*` es **documentación MySQL**, NO fuente de verdad del prototipo; cuando contradice al código se marca como *drift*.

### Convención de recomendación por catálogo

| Marca | Significado |
|---|---|
| `CONGELAR` | Literal en backend + validado por lógica/tests. Listo para DDL. |
| `DECISION_HUMANA` | Existe pero con drift de naming, doble eje, valor muerto o string libre. Requiere decisión antes de congelar. |
| `NO_DDL_UI_ONLY` | Valor solo de presentación en Angular. No debe ir a DDL. |
| `NO_ENCONTRADO` | No existe literal en el prototipo (solo docs/spec o nada). Definir desde spec/dominio, no desde el prototipo. |

### Hallazgo estructural

El prototipo modela rico el **circuito sancionatorio operativo** (bandejas, eventos, notificación, firma, fallo, pago, materiales de cierre, gestión externa, paralización/archivo). **No modela** persona, domicilio, evidencia tipificada, valorización tarifaria, satélites de vehículo/alcoholemia/CTV, requisitos de firma, acuse como entidad, formas/movimientos de pago ni talonarios. Angular es un **consumidor delgado**: no agrega catálogos de dominio nuevos; solo una bandeja agregada (`NOTIFICACIONES`), códigos de subfiltro y un par de claves de copy.

---

# 1. Catálogos backend extraídos

## 1.1 tipo_acta — `DECISION_HUMANA`
Enum `TipoActaDemo` (`PrototipoStore.java:376-379`): `TRANSITO`, `CONTRAVENCION`, `BROMATOLOGIA`.
- Solo se asigna vía `crear-acta-mock-demo`. El mock precargado NO lo popula.
- Conviven 3 conceptos distintos: `tipoActaDemo` ≠ `dependenciaDemo` (`TRANSITO`/`INSPECCIONES`/`FISCALIZACION`/`BROMATOLOGIA`, `:370-373`) ≠ `dominioReferencia` (`TRANSITO_URBANO`/`TRANSITO`/`SEGURIDAD_VIAL`/`BROMATOLOGIA`/`INSPECCIONES`/`FISCALIZACION_OBRA`).

## 1.2 origen_captura — `NO_ENCONTRADO`
No existe literal. Inferido desde bloque inicial `D1_CAPTURA` + evento `LABRADO_MOCK`. Solo nombre de columna en docs MySQL.

## 1.3 bloque_actual — `CONGELAR` (con nota)
| Código | Fuente | Nota |
|---|---|---|
| `D1_CAPTURA` | `PrototipoConstantes.java:29` | |
| `D2_ENRIQUECIMIENTO` | `:32` | |
| `D3_DOCUMENTAL` | `MockDataFactory.java` | **solo mock**, no está en `PrototipoConstantes` → `DECISION_HUMANA` |
| `D4_NOTIFICACION` | `:60` | |
| `D5_ANALISIS` | `:35` | |
| `GESTION_EXTERNA` | `:100` | homónimo bandeja/estado/sit_adm |
| `ARCHIVO` | `ArchivoReingresoSupport.java:49` | homónimo |
| `CERRADA` | `CierreSupport.java:38,83-85` | distinto de bandeja `CERRADAS` |

Eventos usan `bloqueOrigen`+`bloqueDestino`; no hay campo `bloque_actual` en el evento.

## 1.4 est_proc_act (estadoProcesoActual) — `DECISION_HUMANA`
String libre (no enum). Valores observados: `EN_CURSO`, `PENDIENTE_PRODUCCION_PIEZAS`, `PENDIENTE_GENERACION`*, `PENDIENTE_FIRMA_PIEZAS`, `PENDIENTE_FIRMA`, `PENDIENTE_ENVIO`, `PENDIENTE_NOTIFICACION`*, `NOTIFICACION_EN_CURSO`*, `EN_ENVIO`, `PENDIENTE_REVISION`, `PENDIENTE_RESOLUCION`*, `PENDIENTE_NULIDAD`*, `PENDIENTE_MEDIDA_PREVENTIVA`*, `ARCHIVADA_OPERATIVA`, `ARCHIVADA_JURIDICA`*, `EN_GESTION_EXTERNA`, `PARALIZADA`, `CERRADA`. (* = solo mock precargado). Homónimos con bandejas y estados documentales.

## 1.5 sit_adm_act — `CONGELAR`
`ACTIVA`, `PARALIZADA`, `ARCHIVO`, `GESTION_EXTERNA`, `CERRADA`. Fuentes: `ParalizacionReactivacionSupport.java:92-93,154`, `ArchivoReingresoSupport.java:51`, `MockDataFactory.java:1278`, `CierreSupport.java:85`. Muchas transiciones exigen `ACTIVA`.

## 1.6 resultado_final — `CONGELAR`
Enum `ResultadoFinalCierreMock` (`PrototipoStore.java:751-756`): `SIN_RESULTADO_FINAL`, `ABSUELTO`, `PAGO_CONFIRMADO`, `CONDENADO`, `CONDENA_FIRME`.
Regla: dictar fallo NO cambia resultado; lo fija la notificación positiva / vencimiento / resolución.

## 1.7 cod_bandeja (`bandejaActual`) — `CONGELAR`
Orden fijo `ORDEN_BANDEJAS_DEMO` (`PrototipoStore.java:45-58`), 13 valores:
`ACTAS_EN_ENRIQUECIMIENTO`, `PENDIENTE_PREPARACION_DOCUMENTAL`, `PENDIENTE_FIRMA`, `PENDIENTE_NOTIFICACION`, `EN_NOTIFICACION`, `PENDIENTE_ANALISIS`, `PENDIENTES_RESOLUCION_REDACCION`, `PENDIENTES_FALLO`, `CON_APELACION`, `GESTION_EXTERNA`, `PARALIZADAS`, `ARCHIVO`, `CERRADAS`.
Etiquetas en `BandejaNombres.java:10-23`. Terminales: `ARCHIVO`/`CERRADAS`. Suspendidas/externas: `PARALIZADAS`/`GESTION_EXTERNA`.

> **`PENDIENTE_NOTIFICACION` y `EN_NOTIFICACION` son bandejas backend REALES** (no agregados). En Angular se fusionan visualmente bajo `NOTIFICACIONES` (ver §2 y §3).
> **`PENDIENTE_PREPARACION_DOCUMENTAL` es bandeja backend REAL**, ocultada/fusionada visualmente en Angular dentro de `ACTAS_EN_ENRIQUECIMIENTO`.

## 1.8 Sub-bandejas (`SubBandejaCodigo`) — `CONGELAR`
68 valores, string = `.name()`. Definición `SubBandejaCodigo.java:13-468`, clasificación `SubBandejaClasificador.java`. Agrupadas por bandeja padre:

- **ACTAS_EN_ENRIQUECIMIENTO:** `CAPTURA_INICIAL`, `PAGO_VOLUNTARIO_ORIGINADO`, `ENRIQUECIMIENTO_GENERAL`
- **PENDIENTE_PREPARACION_DOCUMENTAL:** `GENERACION_ACTA_PENDIENTE`, `GENERACION_PIEZAS_PENDIENTE`, `REVISION_DOCUMENTAL`
- **PENDIENTE_FIRMA:** `FIRMA_FALLO_CONDENATORIO`, `FIRMA_FALLO_ABSOLUTORIO`, `FIRMA_ACTA_INICIAL`, `FIRMA_OTRAS_PIEZAS`
- **PENDIENTE_NOTIFICACION:** `NOTIF_FALLO_CONDENATORIO_LISTA`, `NOTIF_FALLO_ABSOLUTORIO_LISTA`, `NOTIF_ACTA_LISTA_ENVIO`, `NOTIF_LISTA_OTRO`
- **EN_NOTIFICACION:** `NOTIF_NEGATIVA_PENDIENTE_DECISION`, `NOTIF_VENCIDA_PENDIENTE_DECISION`, `NOTIF_EN_CORREO_POSTAL`, `NOTIF_EN_NOTIFICADOR_MUNICIPAL`, `NOTIF_EN_DOMICILIO_ELECTRONICO`, `NOTIF_EN_OTRO_CANAL`
- **PENDIENTE_ANALISIS (16):** `ANALISIS_BLOQUEO_OPERATIVO`, `CONDENA_LISTO_CIERRE`, `CONDENA_PAGO_CONFIRMADO`, `CONDENA_PAGO_OBSERVADO`, `CONDENA_PAGO_INFORMADO`, `CONDENA_PAGO_PENDIENTE_INFORMAR`, `ANALISIS_LISTO_DERIVAR_EXTERNA`, `ANALISIS_NOTIF_VENCIDA`, `ANALISIS_NOTIF_NEGATIVA`, `ANALISIS_POST_GESTION_EXTERNA`, `ANALISIS_POST_REINGRESO`, `ANALISIS_PAGO_INFORMADO`, `ANALISIS_PAGO_SOLICITADO`, `ANALISIS_PENDIENTE_FALLO`, `ANALISIS_NOTIF_POSITIVA`, `ANALISIS_REVISION_GENERAL` (fallback)
- **PENDIENTES_RESOLUCION_REDACCION:** `REDACCION_NULIDAD`, `REDACCION_MEDIDA`, `REDACCION_RECTIFICACION`, `REDACCION_RESOLUCION`, `REDACCION_GENERAL`
- **PENDIENTES_FALLO:** `FALLO_TRAS_PAGO_INFORMADO`, `FALLO_LISTO_ABSOLUTORIO`, `FALLO_LISTO_CONDENATORIO`
- **CON_APELACION:** `APELACION_EN_ANALISIS`, `APELACION_RESUELTA`, `APELACION_PENDIENTE_RESOLUCION`
- **PARALIZADAS:** `PARALIZ_ESPERA_DOCUMENTAL`, `PARALIZ_TRAMITE_EXTERNO`, `PARALIZ_CAUSA_ADMINISTRATIVA`
- **GESTION_EXTERNA:** `EXT_APREMIO`, `EXT_JUZGADO_PAZ`, `EXT_PENDIENTE_REINGRESO`, `EXT_SEGUIMIENTO`
- **ARCHIVO:** `ARCHIVO_POST_VENCIMIENTO`, `ARCHIVO_DESDE_ANALISIS`, `ARCHIVO_JURIDICO`, `ARCHIVO_REINGRESO_PERMITIDO`, `ARCHIVO_DEFINITIVO`, `ARCHIVO_OPERATIVO`
- **CERRADAS:** `CERRADA_PAGO_VOLUNTARIO`, `CERRADA_PAGO_CONDENA`, `CERRADA_ABSOLUCION`, `CERRADA_NULIDAD`, `CERRADA_ARCHIVO_DEFINITIVO`, `CERRADA_OTRA_CAUSA`

## 1.9 accion_pendiente — `DECISION_HUMANA`
String libre (`accionPendientePorActa`). Constantes store: `COMPLETAR_ENRIQUECIMIENTO`, `REINTENTAR_NOTIFICACION`, `EVALUAR_NOTIFICACION_VENCIDA`, `REVISION_POST_REINGRESO`, `DERIVAR_GESTION_EXTERNA`, `REVISION_POST_GESTION_EXTERNA`, `DICTAR_FALLO_POST_GESTION_EXTERNA`, `REVISION_POST_REACTIVACION`, `PARALIZACION_ESPERA_DOCUMENTAL`, `PARALIZACION_ESPERA_INFORME_EXTERNO`, `PARALIZACION_ESPERA_OTRA_DEPENDENCIA`, `PARALIZACION_ESPERA_RESOLUCION_RELACIONADA`, `PARALIZACION_OTRO`, `EVALUAR_PAGO_VOLUNTARIO`, `VERIFICAR_PAGO_INFORMADO` (`PrototipoStore.java:66-156`).
Solo mock: `GENERAR_BORRADOR_ACTA`, `REVISION_APELACION`, `PARALIZACION_TRAMITE_EXTERNO`, `PARALIZACION_CAUSA_ADMINISTRATIVA`.

## 1.10 tipo_evt — `CONGELAR` (runtime) / `DECISION_HUMANA` (mock)
Eventos `ActaEventoMock.tipoEvento`, append-only. ~73 tipos emitidos por lógica runtime. Familias: alta/labrado, constataciones D1/D2, reconocimiento de origen, cumplimiento material, paralización/reactivación, archivo/reingreso/nulidad, derivación/retorno gestión externa, pago voluntario/condena/apremio, generación/firma de piezas, fallo (dictado/notificado/presencial), plazo/apelación/consentimiento, notificación (entregada/no entregada/vencida/reintento), portal infractor, correo postal (lote/individual/anulación). Además ~25 tipos solo en mock precargado (`ALTA`, `ASIGNACION`, `PASE_BANDEJA`, `FALLO_EMITIDO`, etc.). Detalle completo con líneas, clasificación runtime/mock y set productivo recomendado en `CATALOGOS_TIPO_EVT_ANEXO.md` (no repetido aquí por extensión).

## 1.11 origen_evt — `NO_ENCONTRADO`
No hay campo en `ActaEventoMock`. Solo docs MySQL.

## 1.12 actor_tipo — `NO_ENCONTRADO`
No existe catálogo. Solo texto narrativo ("Sistema (labrado demo)") y `"SISTEMA_EXTERNO"` como `origen` textual de pago.

## 1.13 tipo_persona — `NO_ENCONTRADO`
Solo docs ("física/jurídica"). En código: único sujeto aplanado `infractorNombre`+`infractorDocumento`.

## 1.14 tipo_doc (identidad) — `DECISION_HUMANA` (parcial)
Prefijos embebidos en `infractorDocumento`: `"DNI"`, `"CUIT"` (mock). Labels de búsqueda `"CUIT"`/`"CUIL"`/`"DOC"` (`ActaBusquedaHelper.java:339-341`). No hay campo `tipo_doc` desacoplado. Conflicto de nombre con `tipo_documento` (documento de expediente).

## 1.15 tipo_domicilio / 1.16 origen_domicilio — `NO_ENCONTRADO`
Solo docs. UI usa `domicilioTexto` libre.

## 1.17 tipo_evid — `NO_ENCONTRADO`
No hay entidad evidencia. Lo más cercano: documento `FOTO_INFRACCION` vía `tipoDocumento` (mock).

## 1.18 mime_type — `NO_ENCONTRADO`
Solo extensiones `.jpg`/`.pdf` en `nombreArchivo`. Sin campo MIME.

## 1.19 tipo_unidad — `NO_ENCONTRADO`
Análogo mock `DependenciaActaDemo` (concepto distinto, no renombrar).

## 1.20 Valorización (estado_valorizacion, tipo_valorizacion_acta, origen_valorizacion, criterio_tarifario, tipo_valorizacion_item, motivo_manual) — `NO_ENCONTRADO`
No modelado. El prototipo solo guarda montos `BigDecimal` (`montoPagoVoluntario`, `montoCondena`).

## 1.21 tipo_vehiculo / estado_general_vehiculo — `NO_ENCONTRADO`
Solo patente (`"ABC123"`). El eje RODADO se modela como bloqueante de cierre material, no como estado de vehículo.

## 1.22 Alcoholemia (tipo_prueba_alcoholemia, resultado_cualitativo_alcoholemia, unidad_medida_alcoholemia) — `NO_ENCONTRADO`
Solo texto narrativo "alcohotest positivo". Campos `TipoPrueba`/`ResCuali`/`UniMed` solo en docs. `POSITIVA`/`NEGATIVA` pertenecen a notificación, no a alcoholemia.

## 1.23 ambito_ctv — `NO_ENCONTRADO`
Campo `AmbitoCtv` solo en docs. Los dominios demo (`TRANSITO_URBANO`, etc.) son otro concepto.

## 1.24 tipo_documento — `CONGELAR`
Canónicos (lógica + constantes): `FALLO_ABSOLUTORIO`, `FALLO_CONDENATORIO`, `DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA`, `DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF`, `DOC_LIBERACION_RODADO`, `DOC_RESTITUCION_DOCUMENTACION`, `ACTA_RETENCION`, `CONSTATACION_RETENCION_DOCUMENTACION`, `MEDIDA_PREVENTIVA`, `NOTIFICACION_ACTA`, `NULIDAD`, `RESOLUCION`, `RECTIFICACION` (`PrototipoConstantes.java`, `PiezasFirmaSupport.java`, `CerrabilidadSupport.java`).
Solo mock: `ACTA_FIRMADA`, `BORRADOR_ACTA`, `INFORME_ALCOHOTEST`, `FOTO_INFRACCION`, `CONSTANCIA_RADAR`, `COMPROBANTE_PAGO`, `RESOLUCION_ARCHIVO`, `NOTA_INTERNA_DUPLICIDAD`, `INFORME_VICIO_FORMAL`.
**Deprecated:** `FALLO` (legacy genérico). → ver §6.

## 1.25 estado_documento — `CONGELAR`
`EMITIDO`, `PENDIENTE_FIRMA`, `FIRMADO`, `ADJUNTO` (`PrototipoConstantes.java:157-172`).

## 1.26 tipo_firma_req / rol_firma_req / tipo_firma / estado_firma — `NO_ENCONTRADO`
No hay entidad de firma tipificada. El estado de firma se proyecta en `estadoDocumento` (`PENDIENTE_FIRMA`/`FIRMADO`).

## 1.27 tipo_notif — `CONGELAR`
Enum `TipoNotificacion` (`TipoNotificacion.java:4-6`): `ACTA_INFRACCION`, `FALLO_ABSOLUTORIO`, `FALLO_CONDENATORIO`.

## 1.28 canal_notif — `CONGELAR` (con legacy)
Enum `CanalNotificacion` (`CanalNotificacion.java:4-9`): `EMAIL`, `CORREO_POSTAL`, `NOTIFICADOR_MUNICIPAL`, `DOMICILIO_ELECTRONICO`, `PRESENCIAL`, `PORTAL_INFRACTOR`.
Legacy: campo DTO `canal` admite `POSTAL` (≠ enum `CORREO_POSTAL`). → §6/§7.

## 1.29 estado_notif — `CONGELAR` (con legacy)
Enum `EstadoNotificacion` (`EstadoNotificacion.java:4-15`): `PENDIENTE_PREPARACION`, `LISTA_PARA_ENVIO`, `ENVIADA`, `ENTREGADA`, `NEGATIVA`, `VENCIDA`, `SIN_EFECTO`.
Legacy paralelo `estadoNotificacion`: `PENDIENTE_ENVIO`, `EN_TRAMITE`, `NO_ENTREGADA`, `SUPERADA_POR_PORTAL`. → §6/§7.

## 1.30 estado_intento / resultado_intento — `DECISION_HUMANA`
No hay entidad "intento". El resultado vive a nivel notificación en enum `ResultadoNotificacion` (`ResultadoNotificacion.java:4-13`): `SIN_RESULTADO`, `POSITIVA`, `NEGATIVA`, `VENCIDA`, `SUPERADA_POR_PORTAL`.

## 1.31 tipo_acuse / estado_acuse — `NO_ENCONTRADO`
El acuse municipal usa campo `resultado` (`ResultadoNotificacion`) y actualiza directo estado+resultado de la notificación. No hay entidad acuse.

## 1.32 Catálogos de pago

### tipo_obligacion_pago → enum `TipoPago` — `DECISION_HUMANA`
`NO_APLICA`, `VOLUNTARIO`, `CONDENA` (`PrototipoStore.java:763-767`). `APREMIO` NO es tipo de pago (es gestión externa). Nombre real ≠ nombre DDL.

### estado_obligacion_pago → dos ejes — `DECISION_HUMANA`
- `SituacionPagoMock` (`situacionPago`): `SIN_PAGO`, `SOLICITADO`, `PAGO_INFORMADO`*, `PENDIENTE_CONFIRMACION`, `CONFIRMADO`, `OBSERVADO`, `VENCIDO` (`:716-723`). *`PAGO_INFORMADO` declarado pero nunca asignado → §6.
- `SituacionPagoCondena` (`situacionPagoCondena`): `NO_APLICA`, `PENDIENTE`, `INFORMADO`, `CONFIRMADO`, `OBSERVADO` (`:732-736`).

### tipo_forma_pago / estado_forma_pago / medio_pago / tipo_mov_pago / estado_mov_pago / estado_plan_pago — `NO_ENCONTRADO`
No modelado. `medioPago` es campo DTO libre sin catálogo; el código usa `origen` con `"SISTEMA_EXTERNO"`/`"PORTAL_INFRACTOR"`.

## 1.33 tipo_fallo — `DECISION_HUMANA`
Literal real = tipo de documento `FALLO_ABSOLUTORIO`/`FALLO_CONDENATORIO`. Drift: docs proponen `ABSOLUTORIO`/`CONDENATORIO` sin prefijo. → §7.

## 1.34 estado_fallo / resultado_fallo — `NO_ENCONTRADO`
Sin campo propio. El estado vive en `estadoDocumento`; el resultado en `resultadoFinal`.

## 1.35 canal_apelacion / origen_presentacion_apelacion — `CONGELAR`
Enum `CanalPresentacionApelacionMock` (`PrototipoStore.java:1133-1134`): `PORTAL_INFRACTOR`, `PRESENCIAL_DIRECCION`. Drift docs (`PORTAL`/`PRESENCIAL`). → §7.

## 1.36 tipo_presentacion_apelacion / tipo_doc_apelacion — `NO_ENCONTRADO`
No hay tipo de presentación adicional ni documento de apelación (la apelación es flag + eventos).

## 1.37 estado_apelacion — `DECISION_HUMANA`
Sin campo de estado; flags `apelacionPresentadaPorActa`/`apelacionResueltaPorActa` + sub-bandeja UX. Docs proponen `PRESENTADA`/`RESUELTA`.

## 1.38 resultado_resolucion_apelacion — `CONGELAR`
Enum `ResultadoResolucionApelacionMock` (`:1157-1158`): `RECHAZADA` (→`CONDENA_FIRME`), `ACEPTADA_ABSUELVE` (→`ABSUELTO`). No existen `CONFIRMA`/`REVOCA`.

## 1.39 motivo_paralizacion — `CONGELAR`
Enum `MotivoParalizacionActa` (`:517-521`): `ESPERA_DOCUMENTAL`, `ESPERA_INFORME_EXTERNO`, `ESPERA_OTRA_DEPENDENCIA`, `ESPERA_RESOLUCION_RELACIONADA`, `OTRO`.

## 1.40 motivo_archivo — `DECISION_HUMANA`
3 constantes: `ARCHIVO_DESDE_ANALISIS_DIRECTO`, `ARCHIVO_POST_EVALUACION_VENCIMIENTO`, `NULIDAD` (`:164,171,196`). Solo `NULIDAD` habilita reingreso a enriquecimiento. Drift: docs listan otros valores.

## 1.41 tipo_gestion_ext — `CONGELAR`
`APREMIO`, `JUZGADO_DE_PAZ` (`:179,188`). Lista cerrada `:2003`.

## 1.42 estado_gestion_ext — `NO_ENCONTRADO`
Sin campo. Solo `EN_GESTION_EXTERNA` como estado proceso general.

## 1.43 resultado_gestion_ext — `DECISION_HUMANA`
Enum `ResultadoExternoPostGestion` (`:498-499`): `MODIFICA_MONTO`, `ABSUELVE`. Conviven 3 capas paralelas (enum + eventos `RESULTADO_GESTION_EXTERNA_*` + string compuesto `resolucion` con `CONFIRMA_CONDENA`, `APREMIO_MODIFICA_MONTO`, etc.).

## 1.44 tipo_bloqueante_cierre_material — `CONGELAR`
- Origen (`OrigenBloqueanteCierreMaterialMock`, `:793-806`): `MEDIDA_PREVENTIVA_ACTIVA`, `RODADO_SECUESTRADO`, `DOCUMENTACION_RETENIDA`.
- Pendiente/tarea (`PendienteBloqueanteCierreMock`, `:773-775`): `LEVANTAMIENTO_MEDIDA_PREVENTIVA`, `LIBERACION_RODADO`, `ENTREGA_DOCUMENTACION`.
- Params API reconocimiento: `MEDIDA_ACTIVA`, `SECUESTRO_RODADO`, `RETENCION_DOCUMENTAL`.

## 1.45 estado_bloqueante_cierre_material — `DECISION_HUMANA`
Proxy `FaseEjeHechoMaterial` (`:824-833`): `NO_APLICA`, `SITUACION_PENDIENTE_DE_RESOLUTORIO`, `RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL`, `CUMPLIMIENTO_MATERIAL_VERIFICADO`.

## 1.46 Talonarios (tipo_talonario, clase_talonario, ambito_talonario, estado_numero_talonario, tipo_movimiento_talonario, motivo_anulacion_talonario) — `NO_ENCONTRADO`
Cero ocurrencias en Java. Hints solo en docs/spec: `ELECTRONICO`/`MANUAL_FISICO`, `USADO`/`ANULADO`.

---

# 2. Catálogos Angular/UI extraídos

> Angular no agrega catálogos de dominio nuevos. Casi todos los estados se muestran **crudos** (chips de `acta-badges.presenter.ts` pintan `situacionPago`, `accionPendiente`, `motivoArchivo`, `tipoGestionExterna`, `resultadoFinal`, `motivoNoCerrable`, `pendientesBloqueantesCierre` tal cual). Traducción de copy solo en `demo-shell.component.ts` (`ETIQUETA_*`).

## 2.1 Bandejas visuales (lateral)
`BANDEJAS_LATERAL` (`bandejas-demo.constants.ts`): `ACTAS_EN_ENRIQUECIMIENTO`, `PENDIENTE_ANALISIS`, `PENDIENTES_RESOLUCION_REDACCION`, `PENDIENTES_FALLO`, `PENDIENTE_FIRMA`, **`NOTIFICACIONES`**, `CON_APELACION`, `GESTION_EXTERNA`, `PARALIZADAS`, `ARCHIVO`, `CERRADAS`.
`BANDEJAS_OCULTAS_LATERAL`: `LABRADAS`, `PENDIENTE_NOTIFICACION`, `EN_NOTIFICACION`, `PENDIENTE_PREPARACION_DOCUMENTAL`.

- **`NOTIFICACIONES` = agregador UI, NO bandeja real.** Suma `PENDIENTE_NOTIFICACION` + `EN_NOTIFICACION` (`transformarBandejasParaLateral`, `bandejaBackendALateral`). → `NO_DDL_UI_ONLY`.
- **`LABRADAS` = legacy / referencia muerta.** Declarada en `BandejaCodigo`, abreviaturas e íconos y consultada como bandeja real, pero el backend NUNCA la expone. → `NO_DDL_UI_ONLY` (limpiar).
- **`PENDIENTE_NOTIFICACION` / `EN_NOTIFICACION` = bandejas backend reales**, ocultas en lateral (absorbidas por `NOTIFICACIONES`).
- **`PENDIENTE_PREPARACION_DOCUMENTAL` = bandeja backend real**, ocultada/fusionada en `ACTAS_EN_ENRIQUECIMIENTO`.

## 2.2 Subfiltros operativos (UI-only)
**Enriquecimiento** (`FILTROS_OPERATIVOS_ENRIQUECIMIENTO`):
| Código UI | Label | Resuelve a backend |
|---|---|---|
| `LABRADAS` | Labradas | bandeja `LABRADAS` (inexistente) / `D1_CAPTURA` |
| `CAPTURA_INICIAL` | Captura inicial | sub `CAPTURA_INICIAL` (coincide) |
| `REVISION_INICIAL` | Revisión inicial | `estadoProcesoActual==PENDIENTE_REVISION` (UI-only) |
| `COMPLETITUD_DOCUMENTAL` | Completitud documental | agrupa subs + bandeja `PENDIENTE_PREPARACION_DOCUMENTAL` (UI-only) |

**Notificaciones** (`FILTROS_OPERATIVOS_NOTIFICACIONES`):
| Código UI | Label | Resuelve a backend |
|---|---|---|
| `PENDIENTES_ENVIO` | Pendientes de envío | bandeja `PENDIENTE_NOTIFICACION` |
| `EN_CURSO` | En curso | bandeja `EN_NOTIFICACION` + subs `NOTIF_EN_*` |
| `POSITIVAS` | Positivas | sin fuente (conteo hardcodeado `0`) |
| `NEGATIVAS` | Negativas | sub `NOTIF_NEGATIVA_PENDIENTE_DECISION` |
| `VENCIDAS` | Vencidas | sub `NOTIF_VENCIDA_PENDIENTE_DECISION` |
| `CLASIFICACION_PIEZA` | Fallo / acto / acta | subs `NOTIF_FALLO_*_LISTA` + `NOTIF_ACTA_LISTA_ENVIO` |

## 2.3 Filtros de Correo Postal (`demo-correo-postal-page.component.ts`)
- Tipo (`FILTROS_TIPO`): `TODOS`(UI-only), `ACTA_INFRACCION`, `FALLO_CONDENATORIO`, `FALLO_ABSOLUTORIO` (estos 3 = `TipoNotificacion`).
- Estado lote (`FILTROS_ESTADO_LOTE`): `EN_TRABAJO`, `PROCESADO`, `ANULADO`, `TODOS`.
  - **`EN_TRABAJO` = alias UI de `PENDIENTE_RESPUESTA`** (backend `EstadoLoteCorreo`). `PROCESADO`/`ANULADO` coinciden; `TODOS` UI-only.

## 2.4 Enums de acción UI (replican backend 1:1, vía endpoints)
`MotivoParalizacionDemo`, `CanalPresentacionApelacionDemo`, `ResultadoResolucionApelacionDemo`, `TipoGestionExternaDemo`, `TipoCumplimientoMaterialBloqueante`, `TipoResolucionBloqueoCierre`, `AccionPagoVoluntarioDemo` (`SOLICITAR`/`FIJAR_MONTO`/`INFORMAR`/`ADJUNTAR_COMPROBANTE`/`CONFIRMAR`/`OBSERVAR`), `AccionPagoCondenaDemo` (`INFORMAR`/`CONFIRMAR`/`OBSERVAR`), `SituacionPagoDemo`, `SituacionPagoCondenaDemo`, `ResultadoNotificacionDemo` (`POSITIVA`/`NEGATIVA`/`VENCIDA`). Todos = catálogos backend (sin valores nuevos).

## 2.5 Copy-control / etiquetas (demo-shell `ETIQUETA_*`)
Mapas código→texto. Claves = catálogos backend, salvo dos claves UI-only en `ETIQUETA_BADGE_COMPACTA`:
- **`ENTREGA_DOC` = abreviatura UI de `ENTREGA_DOCUMENTACION`** ("Entrega documentación").
- **`FALTA_RESULTADO_FINAL`** = clave de copy UI-only (no es enum backend).

Otros mapas (todas claves backend): `ETIQUETA_DEPENDENCIA_DEMO`, `ETIQUETA_SITUACION_PAGO_CONDENA`, `ETIQUETA_ACCION_PAGO(_EN_CURSO)`, `ETIQUETA_ACCION_PAGO_CONDENA(_EN_CURSO)`, `ETIQUETA_RESULTADO_NOTIFICACION`, `ETIQUETA_CUMPLIMIENTO_MATERIAL`, `ETIQUETA_RESOLUCION_BLOQUEO_CIERRE(_EN_CURSO)`, `ETIQUETA_DERIVACION_GESTION_EXTERNA(_EN_CURSO)`, `BANDEJA_ABREVIATURAS`, `BANDEJA_ICONOS`.

## 2.6 Botones que disparan cambios de estado (back-office, demo-shell)
Cada `(click)` → método → endpoint real:

| Botón | Endpoint | Catálogo afectado |
|---|---|---|
| Cerrar acta | `cerrar-acta` | →`CERRADAS` |
| Enviar a notificación | `enviar-a-notificacion` | D2→D4 |
| Anular acta (nulidad) | `anular-acta` | `motivoArchivo=NULIDAD` |
| Reingresar | `reingresar-acta` | →`PENDIENTE_ANALISIS` |
| Archivar | `archivar-acta` | →`ARCHIVO` |
| Paralizar | `paralizar-acta` (body `motivo`) | `motivo_paralizacion` |
| Reactivar | `reactivar-acta` | `PARALIZADAS`→`PENDIENTE_ANALISIS` |
| Derivar apremio/juzgado | `derivar-a-apremio` / `derivar-a-juzgado-de-paz` | `tipo_gestion_ext` |
| Apremio registrar pago / reingresar sin pago | `apremio-registrar-pago` / `apremio-reingresar-sin-pago` | gestión externa |
| Juzgado absuelto/condena/monto | `juzgado-reingresar-absuelto` / `-condena-confirmada` / `-monto-modificado` | `ABSUELVE`/`CONFIRMA_CONDENA`/`MODIFICA_MONTO` |
| Resultado notif. | `registrar-notificacion-positiva/negativa/vencida` | `ResultadoNotificacion` |
| Reintentar notificación | `reintentar-notificacion` | →`PENDIENTE_NOTIFICACION` |
| Dictar fallo abs./cond. | `dictar-fallo-absolutorio` / `-condenatorio` (body `montoCondena`) | docs fallo |
| Apelación presencial | `registrar-apelacion` (`PRESENCIAL_DIRECCION`) | `canal_apelacion` |
| Vencimiento plazo apelación | `registrar-vencimiento-plazo-apelacion` | →`CONDENA_FIRME` |
| Rechazar / aceptar apelación | `resolver-apelacion` (`RECHAZADA`/`ACEPTADA_ABSUELVE`) | resultado resolución |
| Consentir condena + pago | `consentir-condena-y-registrar-pago` | `CONDENA_FIRME`+pago |
| Acciones pago voluntario | `registrar-solicitud-pago-voluntario` / `fijar-monto-pago-voluntario` / `registrar-pago-informado` / `adjuntar-comprobante-pago-informado` / `confirmar-pago-informado` / `observar-pago-informado` | `SituacionPagoMock` |
| Vencimiento pago voluntario | `registrar-vencimiento-pago-voluntario` | `VENCIDO` |
| Acciones pago condena | `informar-pago-condena` / `confirmar-pago-condena` / `observar-pago-condena` | `SituacionPagoCondena` |
| Generar piezas | `generar-nulidad/medida-preventiva/notificacion-acta/resolucion/rectificacion` | docs pieza |
| Firmar documento | `firmar-documento/{id}` | `PENDIENTE_FIRMA`→`FIRMADO` |
| Resolver bloqueo cierre | `registrar-resolucion-bloqueo-cierre?tipo=` | bloqueante material |
| Cumplir materialmente | `registrar-cumplimiento-material-bloqueo-cierre?tipo=` | cumplimiento material |
| Reset mocks (QA) | `reset` | reinicia dataset |

## 2.7 Botones Portal Infractor (`features/infractor`)
| Botón | Endpoint |
|---|---|
| Solicitar pago voluntario (info) | `infractor/.../solicitar-pago-voluntario` |
| Pagar voluntario | `.../pagar-voluntario` |
| Pagar condena | `.../pagar-condena` |
| Consentir condena | `.../consentir-condena` |
| Confirmar visualización notif. | `.../confirmar-visualizacion-notificacion` |
| Presentar apelación (canal forzado `PORTAL_INFRACTOR`) | `.../registrar-apelacion` |
| Ver documento | `.../documentos/{tipo}/ver` |

## 2.8 Botones Notificador Municipal
`registrarAcuse(n, 'POSITIVA'|'NEGATIVA'|'VENCIDA')` → `notificaciones/notificador-municipal/{id}/acuse` (body `resultado`).

---

# 3. Cruce backend vs UI

| Concepto | Backend | UI | Relación |
|---|---|---|---|
| Notificaciones | `PENDIENTE_NOTIFICACION` + `EN_NOTIFICACION` (bandejas reales) | `NOTIFICACIONES` (agregador) | UI agrupa 2 bandejas reales |
| Preparación documental | `PENDIENTE_PREPARACION_DOCUMENTAL` (bandeja real) | fusionada en `ACTAS_EN_ENRIQUECIMIENTO` | oculta visualmente |
| Labradas | (no existe) | `LABRADAS` (referenciada) | referencia muerta en UI |
| Estado lote correo | `PENDIENTE_RESPUESTA` | `EN_TRABAJO` | alias UI |
| Entrega documentación | `ENTREGA_DOCUMENTACION` | `ENTREGA_DOC` (copy) | abreviatura UI |
| Sub-bandejas | 68 `SubBandejaCodigo` | pasan crudas por `acta.subBandeja`; subfiltros propios solo en enriquecimiento/notificaciones | UI no las traduce |
| Canal notif. | enum + legacy `POSTAL` | modelo dual `canal`/`canalTipificado` | UI refleja ambos |
| Estado notif. | enum + legacy `estadoNotificacion` | modelo dual `estado`/`estadoNotificacion` | UI refleja ambos |
| Tipo notif. | `ACTA_INFRACCION`/`FALLO_*` | filtros correo idénticos (+`TODOS`) | coincide |
| Acciones / enums sensibles | enums backend | enums UI 1:1 | coincide (sin valores nuevos) |

**Conclusión:** la UI no introduce catálogos de dominio nuevos; agrega 1 bandeja agregada, códigos de subfiltro de presentación y 2 claves de copy.

---

# 4. Valores listos para congelar (CONGELAR)

- `sit_adm_act`, `resultado_final`, `cod_bandeja` (13), sub-bandejas `SubBandejaCodigo` (68), `bloque_actual` (revisar `D3_DOCUMENTAL`).
- `tipo_documento`, `estado_documento`.
- `tipo_notif`, `canal_notif` (sin legacy `POSTAL`), `estado_notif` (sin legacy), `ResultadoNotificacion`.
- `tipo_fallo` (decidir prefijo primero), `canal_apelacion`/`origen_presentacion_apelacion`, `resultado_resolucion_apelacion`.
- `motivo_paralizacion`, `tipo_gestion_ext`, `resultado_gestion_ext` (capa enum), `tipo_bloqueante_cierre_material` (origen+pendiente).
- `tipo_evt` (set runtime; depurar los "solo mock").

---

# 5. Valores UI-only que NO deben ir a DDL (NO_DDL_UI_ONLY)

- Bandeja `NOTIFICACIONES` (agregador UI).
- Subfiltros: `REVISION_INICIAL`, `COMPLETITUD_DOCUMENTAL`, `PENDIENTES_ENVIO`, `EN_CURSO`, `POSITIVAS` (sin datos), `CLASIFICACION_PIEZA`.
- Filtros correo: `TODOS`, `EN_TRABAJO` (alias de `PENDIENTE_RESPUESTA`).
- Claves de copy: `FALTA_RESULTADO_FINAL`, `ENTREGA_DOC`.
- Tipos internos de UI: `CorreoPostalAccion` (`GENERAR_LOTE`/`PROCESAR_RESPUESTA`/`ANULAR_LOTE`/`ENVIAR_INDIVIDUAL`), `VistaCorreoPostal` (`actas`/`lotes`).

---

# 6. Valores legacy / deprecated

| Valor | Dónde | Estado |
|---|---|---|
| `LABRADAS` (bandeja) | UI (`BandejaCodigo`, constantes, abreviaturas, íconos) | Referencia muerta: el backend no la expone. Limpiar. |
| `FALLO` (tipo documento) | `MockDataFactory.java:1137,1236,3457`; `SubBandejaClasificador.java:105,357` | Legacy genérico; tests nuevos exigen su ausencia. |
| `PAGO_INFORMADO` (situacionPago) | enum `SituacionPagoMock` (`PrototipoStore.java:718`) | Declarado pero NUNCA asignado (flujo salta a `PENDIENTE_CONFIRMACION`). |
| `POSTAL` (canal notif.) | DTO legacy `canal` | Alias legacy de `CORREO_POSTAL`. |
| `estadoNotificacion` legacy (`PENDIENTE_ENVIO`/`EN_TRAMITE`/`NO_ENTREGADA`/`SUPERADA_POR_PORTAL`) | DTO legacy paralelo a `estado` (enum) | Decidir si sobrevive al DDL. |
| `D3_DOCUMENTAL` (bloque) | mock, ausente en `PrototipoConstantes` | Inconsistente con constantes. |
| `PARALIZACION_TRAMITE_EXTERNO`, `PARALIZACION_CAUSA_ADMINISTRATIVA` (accion pendiente) | solo mock; no producidos por `paralizarActa` | Drift sub-bandeja vs circuito. |

---

# 7. Conflictos de naming

1. `tipo_fallo`: backend `FALLO_ABSOLUTORIO`/`FALLO_CONDENATORIO` vs docs `ABSOLUTORIO`/`CONDENATORIO`.
2. `canal_apelacion`: backend `PORTAL_INFRACTOR`/`PRESENCIAL_DIRECCION` vs docs `PORTAL`/`PRESENCIAL`.
3. `motivo_archivo`: backend (3 valores) vs docs (5 valores).
4. `LABRADAS` (UI, como bandeja real) vs backend (no existe; usa `ACTAS_EN_ENRIQUECIMIENTO`).
5. `EN_TRABAJO` (UI estado lote) vs `PENDIENTE_RESPUESTA` (backend `EstadoLoteCorreo`).
6. `ENTREGA_DOC` (UI copy) vs `ENTREGA_DOCUMENTACION` (backend).
7. Notificación: campos duales `canal`/`canalTipificado` y `estadoNotificacion`/`estado` (legacy + enum) coexisten en API y modelo UI.
8. `tipo_doc` (identidad) vs `tipo_documento`/`tipoDocumento` (documento de expediente): mismo nombre raíz, dominios distintos.
9. `tipo_acta` ambiguo: `tipoActaDemo` vs `dependenciaDemo` vs `dominioReferencia`.
10. `resultado_gestion_ext` en 3 representaciones paralelas (enum / evento / string compuesto).

---

# 8. Catálogos no encontrados literal (NO_ENCONTRADO)

Solo aparecen como nombre de columna en docs MySQL del módulo o no aparecen:
`origen_captura`, `origen_evt`, `actor_tipo`, `tipo_persona`, `tipo_domicilio`, `origen_domicilio`, `tipo_evid`, `mime_type`, `tipo_unidad`, valorización (`estado_valorizacion`, `tipo_valorizacion_acta`, `origen_valorizacion`, `criterio_tarifario`, `tipo_valorizacion_item`, `motivo_manual`), `tipo_vehiculo`, `estado_general_vehiculo`, `tipo_prueba_alcoholemia`, `resultado_cualitativo_alcoholemia`, `unidad_medida_alcoholemia`, `ambito_ctv`, `tipo_firma_req`, `rol_firma_req`, `tipo_firma`, `estado_firma`, `estado_intento`*, `tipo_acuse`, `estado_acuse`, `tipo_forma_pago`, `estado_forma_pago`, `medio_pago`, `tipo_mov_pago`, `estado_mov_pago`, `estado_plan_pago`, `estado_fallo`, `resultado_fallo`, `tipo_presentacion_apelacion`, `tipo_doc_apelacion`, `estado_gestion_ext`, talonarios (`tipo_talonario`, `clase_talonario`, `ambito_talonario`, `estado_numero_talonario`, `tipo_movimiento_talonario`, `motivo_anulacion_talonario`).
(* `estado_intento`/`resultado_intento` colapsados en `ResultadoNotificacion`.)

---

# 9. Recomendación final por catálogo

| # | Catálogo | Recomendación |
|---|---|---|
| 1 | tipo_acta | DECISION_HUMANA |
| 2 | origen_captura | NO_ENCONTRADO |
| 3 | bloque_actual | CONGELAR (revisar `D3_DOCUMENTAL`) |
| 4 | est_proc_act | DECISION_HUMANA |
| 5 | sit_adm_act | CONGELAR |
| 6 | resultado_final | CONGELAR |
| 7 | cod_bandeja | CONGELAR |
| 8 | sub-bandejas (SubBandejaCodigo) | CONGELAR |
| 9 | accion_pendiente | DECISION_HUMANA |
| 10 | tipo_evt | CONGELAR (runtime) / DECISION_HUMANA (mock) |
| 11 | origen_evt | NO_ENCONTRADO |
| 12 | actor_tipo | NO_ENCONTRADO |
| 13 | tipo_persona | NO_ENCONTRADO |
| 14 | tipo_doc (identidad) | DECISION_HUMANA |
| 15 | tipo_domicilio | NO_ENCONTRADO |
| 16 | origen_domicilio | NO_ENCONTRADO |
| 17 | tipo_evid | NO_ENCONTRADO |
| 18 | mime_type | NO_ENCONTRADO |
| 19 | tipo_unidad | NO_ENCONTRADO |
| 20 | estado_valorizacion | NO_ENCONTRADO |
| 21 | tipo_valorizacion_acta | NO_ENCONTRADO |
| 22 | origen_valorizacion | NO_ENCONTRADO |
| 23 | criterio_tarifario | NO_ENCONTRADO |
| 24 | tipo_valorizacion_item | NO_ENCONTRADO |
| 25 | motivo_manual | NO_ENCONTRADO |
| 26 | tipo_vehiculo | NO_ENCONTRADO |
| 27 | estado_general_vehiculo | NO_ENCONTRADO |
| 28 | tipo_prueba_alcoholemia | NO_ENCONTRADO |
| 29 | resultado_cualitativo_alcoholemia | NO_ENCONTRADO |
| 30 | unidad_medida_alcoholemia | NO_ENCONTRADO |
| 31 | ambito_ctv | NO_ENCONTRADO |
| 32 | tipo_documento | CONGELAR |
| 33 | estado_documento | CONGELAR |
| 34 | tipo_firma_req | NO_ENCONTRADO |
| 35 | rol_firma_req | NO_ENCONTRADO |
| 36 | tipo_firma | NO_ENCONTRADO |
| 37 | estado_firma | NO_ENCONTRADO |
| 38 | tipo_notif | CONGELAR |
| 39 | canal_notif | CONGELAR (sin legacy `POSTAL`) |
| 40 | estado_notif | CONGELAR (sin legacy) |
| 41 | estado_intento | DECISION_HUMANA (colapsado) |
| 42 | resultado_intento (ResultadoNotificacion) | CONGELAR |
| 43 | tipo_acuse | NO_ENCONTRADO |
| 44 | estado_acuse | NO_ENCONTRADO |
| 45 | tipo_obligacion_pago (TipoPago) | DECISION_HUMANA |
| 46 | estado_obligacion_pago (SituacionPago + SituacionPagoCondena) | DECISION_HUMANA |
| 47 | tipo_forma_pago | NO_ENCONTRADO |
| 48 | estado_forma_pago | NO_ENCONTRADO |
| 49 | medio_pago | NO_ENCONTRADO |
| 50 | tipo_mov_pago | NO_ENCONTRADO |
| 51 | estado_mov_pago | NO_ENCONTRADO |
| 52 | estado_plan_pago | NO_ENCONTRADO |
| 53 | tipo_fallo | DECISION_HUMANA (naming) |
| 54 | estado_fallo | NO_ENCONTRADO |
| 55 | resultado_fallo | NO_ENCONTRADO |
| 56 | tipo_presentacion_apelacion | NO_ENCONTRADO |
| 57 | canal_apelacion | CONGELAR (naming) |
| 58 | estado_apelacion | DECISION_HUMANA |
| 59 | resultado_resolucion_apelacion | CONGELAR |
| 60 | tipo_doc_apelacion | NO_ENCONTRADO |
| 61 | origen_presentacion_apelacion | CONGELAR (= canal_apelacion) |
| 62 | motivo_paralizacion | CONGELAR |
| 63 | motivo_archivo | DECISION_HUMANA |
| 64 | tipo_gestion_ext | CONGELAR |
| 65 | estado_gestion_ext | NO_ENCONTRADO |
| 66 | resultado_gestion_ext | DECISION_HUMANA |
| 67 | tipo_bloqueante_cierre_material | CONGELAR |
| 68 | estado_bloqueante_cierre_material | DECISION_HUMANA |
| 69 | tipo_talonario | NO_ENCONTRADO |
| 70 | clase_talonario | NO_ENCONTRADO |
| 71 | ambito_talonario | NO_ENCONTRADO |
| 72 | estado_numero_talonario | NO_ENCONTRADO |
| 73 | tipo_movimiento_talonario | NO_ENCONTRADO |
| 74 | motivo_anulacion_talonario | NO_ENCONTRADO |

## Marcas especiales (resumen)

- **`NOTIFICACIONES`**: agregador UI, NO bandeja real → `NO_DDL_UI_ONLY`.
- **`LABRADAS`**: legacy / referencia muerta en UI → `NO_DDL_UI_ONLY` (limpiar).
- **`EN_TRABAJO`**: alias UI de `PENDIENTE_RESPUESTA` → `NO_DDL_UI_ONLY`.
- **`ENTREGA_DOC`**: abreviatura UI de `ENTREGA_DOCUMENTACION` → `NO_DDL_UI_ONLY`.
- **`PENDIENTE_NOTIFICACION`** y **`EN_NOTIFICACION`**: bandejas backend reales → `CONGELAR`.
- **`PENDIENTE_PREPARACION_DOCUMENTAL`**: bandeja backend real, ocultada/fusionada en Angular → `CONGELAR`.

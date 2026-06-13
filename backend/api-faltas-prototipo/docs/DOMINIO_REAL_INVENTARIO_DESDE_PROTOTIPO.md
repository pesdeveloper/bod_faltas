# Inventario de dominio real desde prototipo validado

## Alcance y criterio de construccion

Este documento inventaria entidades, agregados, eventos y estados persistibles
que surgen de las reglas ya validadas en el prototipo
(`backend/api-faltas-prototipo`). No implementa codigo ni diseña tablas salvo
cuando la necesidad es evidente. Marca como "pendiente de cruce con spec" todo
lo que no puede cerrarse solo con lo que aportan los supports.

Fuentes consultadas: `PLAN_INTEGRACION_DOMINIO_REAL.md`, `PrototipoStore.java`,
`CerrabilidadSupport.java`, `FalloPlazoApelacionSupport.java`,
`NotificacionSupport.java`, `PagoVoluntarioSupport.java`,
`PagoInformadoSupport.java`, `PagoCondenaSupport.java`,
`GestionExternaSupport.java`, `ArchivoReingresoSupport.java`,
`ParalizacionReactivacionSupport.java`, `PiezasFirmaSupport.java`,
`PortalInfractorSupport.java`, `ActaMock.java`, `ActaNotificacionMock.java`,
`ActaDocumentoMock.java`, `ActaEventoMock.java`, `PrototipoConstantes.java` y
enums del dominio.

---

## 1. Reglas persistibles relevadas desde el prototipo

### 1.1 Ciclo principal: labrado y enriquecimiento

- El acta nace en bloque `D1_CAPTURA` / bandeja `ACTAS_EN_ENRIQUECIMIENTO`.
- Puede recibir constatacion material temprana (secuestro de rodado, retencion
  documental, medida preventiva aplicable) durante D1/D2 antes de avanzar.
- Puede recibir solicitud de pago voluntario desde cualquier bandeja interna
  operable, incluyendo labradas y enriquecimiento.
- La anulacion por nulidad desde enriquecimiento archiva el acta con
  `motivoArchivo=NULIDAD` y `permiteReingreso=true`.

### 1.2 Ciclo documental: firma

- Las piezas resolutorias se producen en estado `PENDIENTE_FIRMA`.
- Cada pieza se firma individualmente; la firma de todas las piezas cierra el
  circuito documental y transiciona la bandeja.
- La firma de nulidad archiva el expediente: es una precondicion critica.
- Tipos de pieza: medida preventiva, notificacion del acta, nulidad, resolucion,
  rectificacion, fallo absolutorio, fallo condenatorio.
- Estados de documento: `EMITIDO`, `PENDIENTE_FIRMA`, `FIRMADO`, `ADJUNTO`.

### 1.3 Ciclo de notificacion

- Una notificacion nace preparada (`LISTA_PARA_ENVIO`) y transiciona a
  `ENVIADA`, `ENTREGADA`, `NEGATIVA`, `VENCIDA` o `SIN_EFECTO`.
- Canales: `EMAIL`, `CORREO_POSTAL`, `NOTIFICADOR_MUNICIPAL`,
  `DOMICILIO_ELECTRONICO`, `PRESENCIAL`, `PORTAL_INFRACTOR`.
- Tipos: `ACTA_INFRACCION`, `FALLO_ABSOLUTORIO`, `FALLO_CONDENATORIO`.
- Resultados: `SIN_RESULTADO`, `POSITIVA`, `NEGATIVA`, `VENCIDA`,
  `SUPERADA_POR_PORTAL`.
- La notificacion positiva de fallo materializa el resultado final del acta y
  abre el plazo de apelacion si corresponde.
- La notificacion por portal descarta notificaciones paralelas pendientes del
  mismo acta: quedan `SIN_EFECTO` / `SUPERADA_POR_PORTAL`.
- El notificador municipal agrega `loteId` y `referenciaExterna`; el correo
  postal agrega `loteId` con trazabilidad de lote.
- La notificacion negativa devuelve el acta a `PENDIENTE_ANALISIS` con marca
  `REINTENTAR_NOTIFICACION`.
- La notificacion vencida devuelve el acta a `PENDIENTE_ANALISIS` con marca
  `EVALUAR_NOTIFICACION_VENCIDA`.

### 1.4 Ciclo de fallo y apelacion

- El fallo (absolutorio o condenatorio) se dicta desde `PENDIENTE_ANALISIS`.
- El fallo genera una pieza documental que entra en circuito de firma.
- Tras firma, la pieza entra en circuito de notificacion.
- La notificacion positiva del fallo fija `resultadoFinal` (`ABSUELTO` o
  `CONDENADO`) y devuelve el acta a `PENDIENTE_ANALISIS`.
- El fallo condenatorio fija el `montoCondena` y abre el plazo de apelacion.
- El vencimiento del plazo sin apelacion lleva el resultado a `CONDENA_FIRME`.
- La apelacion se puede registrar desde `PENDIENTE_ANALISIS` (canal presencial)
  o desde el portal infractor (canal portal).
- La apelacion resuelta puede ser `RECHAZADA` o `ACEPTADA_ABSUELVE`.

### 1.5 Circuito de pago voluntario

- Solicitud: cualquier bandeja interna operable; genera marca
  `EVALUAR_PAGO_VOLUNTARIO` en analisis.
- Fijacion de monto: operador fija el monto habilitado.
- Registro de pago informado: el infractor informa que pago.
- Adjuntar comprobante: se adjunta referencia del comprobante.
- Confirmacion: operador confirma; `situacionPago` pasa a `CONFIRMADO` y
  `resultadoFinal` a `PAGO_CONFIRMADO`.
- Observacion: operador observa el pago; `situacionPago` pasa a `OBSERVADO`.
- Vencimiento: si vence sin pago, `situacionPago` pasa a `VENCIDO`.
- Confirmacion externa: integracion con sistema externo (pendiente de diseno).
- `situacionPago` es independiente de `situacionPagoCondena`: son circuitos
  separados.

### 1.6 Circuito de pago condena

- Solo aplica cuando el resultado es `CONDENA_FIRME`.
- Informar pago condena: el infractor informa que pago la condena.
- Confirmar / observar: operador confirma u observa.
- Consentir condena y registrar pago: variante portal donde el infractor
  consiente y paga en un solo acto.
- `SituacionPagoCondena`: `NO_APLICA`, `PENDIENTE`, `INFORMADO`, `CONFIRMADO`,
  `OBSERVADO`.
- Existen comprobantes especificos (EM, RC, Cmte/Pref/Nro): pendiente de cruce
  con spec.

### 1.7 Bloqueantes materiales

- Tres origenes de bloqueo de cierre: `MEDIDA_PREVENTIVA_ACTIVA`,
  `RODADO_SECUESTRADO`, `DOCUMENTACION_RETENIDA`.
- Cada origen tiene un ancla documental en el expediente (pieza de tipo
  `MEDIDA_PREVENTIVA`, `ACTA_RETENCION`, `CONSTATACION_RETENCION_DOCUMENTACION`).
- El cierre de un bloqueo requiere dos pasos distintos: resolucion documental
  (emision y firma del resolutorio) y cumplimiento material efectivo (accion
  separada).
- Los bloqueantes documentales y los materiales son categorias distintas con
  reglas distintas: no colapsar.
- Los resolutorios de bloqueo tienen circuitos propios: firma directa o firma +
  notificacion segun el tipo.
- Pendientes bloqueantes: `LEVANTAMIENTO_MEDIDA_PREVENTIVA`,
  `LIBERACION_RODADO`, `ENTREGA_DOCUMENTACION`.

### 1.8 Archivo y reingreso

- El archivo puede originarse desde analisis (directo), post evaluacion de
  vencimiento, por nulidad, o desde firma de pieza de nulidad.
- `motivoArchivo` distingue el origen: `ARCHIVO_DESDE_ANALISIS_DIRECTO`,
  `ARCHIVO_POST_EVALUACION_VENCIMIENTO`, `NULIDAD`.
- El archivo de nulidad tiene `permiteReingreso=true`; los demas pueden o no
  permitirlo (pendiente de cruce con spec).
- El reingreso desde archivo devuelve el acta a `PENDIENTE_ANALISIS` con marca
  `REVISION_POST_REINGRESO`.

### 1.9 Gestion externa

- Dos tipos: `APREMIO` y `JUZGADO_DE_PAZ`.
- La derivacion requiere marca `DERIVAR_GESTION_EXTERNA` activa.
- El reingreso desde gestion externa devuelve a `PENDIENTE_ANALISIS` con marca
  `REVISION_POST_GESTION_EXTERNA` o `DICTAR_FALLO_POST_GESTION_EXTERNA`.
- La gestion externa puede devolver un resultado: `MODIFICA_MONTO` o
  `ABSUELVE`.
- El apremio puede registrar pago externo o reingresar sin pago.
- El juzgado puede absolver, confirmar condena o modificar monto.

### 1.10 Paralizacion y reactivacion

- La paralizacion es transversal: puede ocurrir desde cualquier bandeja interna
  operable.
- Motivos: `ESPERA_DOCUMENTAL`, `ESPERA_INFORME_EXTERNO`,
  `ESPERA_OTRA_DEPENDENCIA`, `ESPERA_RESOLUCION_RELACIONADA`, `OTRO`.
- La observacion de paralizacion debe persistirse: hoy vive en un mapa
  volatil del store.
- La reactivacion devuelve el acta a la bandeja anterior con marca
  `REVISION_POST_REACTIVACION`.

### 1.11 Cierre explicito

- El cierre solo ocurre por invocacion explicita del endpoint `/cerrar-acta`.
- No ocurre automaticamente por ninguna condicion calculada.
- Precondicion: cerrabilidad debe ser `CERRABLE` segun `CerrabilidadSupport`.
- Condiciones habilitantes: `resultadoFinal` en `ABSUELTO` o `PAGO_CONFIRMADO`,
  mas ausencia de pendientes bloqueantes activos.

### 1.12 Portal infractor

- El portal resuelve el acta por codigo QR (mecanismo de lookup pendiente de
  diseno).
- Solo expone lo visible para el ciudadano: documentos notificados, posibilidad
  de pago, posibilidad de apelacion.
- No expone estados internos: `bandejaActual`, `accionPendiente`, marcas
  operativas.
- El portal puede registrar apelacion, confirmar visualizacion de notificacion,
  solicitar pago voluntario, pagar voluntario, pagar condena y consentir condena.
- `PARALIZADAS` se expone como `EN_TRAMITE` al infractor.

---

## 2. Agregados y entidades candidatos

### 2.1 Acta / Expediente de Faltas

| Campo | Detalle |
|---|---|
| **Tipo** | Agregado raiz |
| **Responsabilidad** | Representa la unidad principal del tramite. Contiene identidad, partes y estado actual del expediente. |
| **Datos minimos persistibles** | `actaId`, `numeroActa`, `dominioReferencia`, `bloqueActual`, `estadoProcesoActual`, `situacionAdministrativaActual`, `estaCerrada`, `permiteReingreso`, `fechaCreacion`, `infractorNombre`, `infractorDocumento`, `inspectorNombre`, `resumenHecho`, `bandejaActual` (calculada o cache), `motivoArchivo`, `tipoGestionExterna`, `observacionParalizacion` |
| **Reglas asociadas** | Cierre explicito; bandeja terminal no admite acciones internas; paralizacion transversal |
| **Eventos que emite** | Todos los eventos del expediente (ver seccion 4) |
| **Relacion con Acta** | Es el agregado raiz |
| **Dudas pendientes** | Formato de `numeroActa` en datos reales; catalogo de tipos de acta (transito, bromatologia, general); separacion entre `bloqueActual` / `estadoProcesoActual` / `situacionAdministrativaActual` en schema real |

### 2.2 Documento / Pieza

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Representa una pieza documental del expediente con su ciclo de vida (emision, firma, notificacion). |
| **Datos minimos persistibles** | `documentoId`, `actaId`, `tipoDocumento`, `estadoDocumento`, `nombreArchivo`, `fechaEmision`, `fechaFirma` |
| **Reglas asociadas** | Firma individual por pieza; firma de nulidad cierra expediente; tipos de pieza determinan circuito (firma directa vs firma+notificacion) |
| **Eventos que emite** | `DOCUMENTO_GENERADO`, `DOCUMENTO_FIRMADO`, `FIRMA_CERRADA` |
| **Relacion con Acta** | 1 acta : N documentos |
| **Dudas pendientes** | Modelo de archivo fisico (binario / referencia externa / repositorio documental): pendiente de cruce con spec |

### 2.3 Notificacion

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Representa un acto de notificacion al infractor, con canal, resultado y trazabilidad postal/municipal. |
| **Datos minimos persistibles** | `notificacionId`, `actaId`, `tipo` (`TipoNotificacion`), `canal` (`CanalNotificacion`), `estado` (`EstadoNotificacion`), `resultado` (`ResultadoNotificacion`), `destinatarioNombre`, `domicilioTexto`, `fechaPreparacion`, `fechaEnvio`, `fechaResultado`, `referencia`, `eventoRelacionado`, `loteId`, `referenciaExterna`, `observacion`, `destinatarioEmail`, `domicilioElectronicoVerificado`, `diasPlazoNotificacionElectronica` |
| **Reglas asociadas** | Notificacion positiva de fallo fija resultado final; superada por portal queda `SIN_EFECTO`; reintento post negativa; evaluacion post vencimiento |
| **Eventos que emite** | `NOTIFICACION_POSITIVA`, `NOTIFICACION_NEGATIVA`, `NOTIFICACION_VENCIDA`, `REINTENTO_NOTIFICACION`, `NOTIFICACION_PORTAL_CONFIRMADA`, `NOTIFICACION_SUPERADA_POR_PORTAL`, `ACUSE_NOTIFICADOR_MUNICIPAL`, `LOTE_CORREO_GENERADO` |
| **Relacion con Acta** | 1 acta : N notificaciones (multiples intentos, multiples piezas) |
| **Dudas pendientes** | Estructura de lote de correo postal como entidad propia; modelo de domicilio electronico verificado; pendiente de cruce con spec de notificaciones |

### 2.4 Pago voluntario

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta (circuito separado de pago condena) |
| **Responsabilidad** | Modela el ciclo de solicitud, fijacion de monto, registro de pago informado y confirmacion del pago voluntario previo a fallo. |
| **Datos minimos persistibles** | `pagoVoluntarioId`, `actaId`, `situacionPago` (`SituacionPagoMock`), `montoPagoVoluntario`, `fechaSolicitud`, `fechaFijacionMonto`, `fechaPagoInformado`, `comprobante`, `fechaConfirmacion`, `fechaVencimiento`, `observacion` |
| **Reglas asociadas** | Separacion estricta de pago condena; solicitud desde cualquier bandeja interna operable; monto fijado por operador; confirmacion lleva `resultadoFinal` a `PAGO_CONFIRMADO` |
| **Eventos que emite** | `PAGO_VOLUNTARIO_SOLICITADO`, `MONTO_PAGO_VOLUNTARIO_FIJADO`, `PAGO_INFORMADO_REGISTRADO`, `PAGO_INFORMADO_CONFIRMADO`, `PAGO_INFORMADO_OBSERVADO`, `PAGO_VOLUNTARIO_VENCIDO`, `PAGO_VOLUNTARIO_EXTERNO_CONFIRMADO` |
| **Relacion con Acta** | 1 acta : 0..1 pago voluntario activo (puede haber historial de intentos observados) |
| **Dudas pendientes** | Modelo de comprobante fisico (EM, RC, Cmte/Pref/Nro): pendiente de cruce con spec; confirmacion externa pendiente de diseno |

### 2.5 Pago condena

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta (circuito separado de pago voluntario) |
| **Responsabilidad** | Modela el pago de la condena firme post fallo condenatorio y vencimiento de plazo de apelacion. |
| **Datos minimos persistibles** | `pagoCondenaId`, `actaId`, `situacionPagoCondena` (`SituacionPagoCondena`), `montoCondena`, `tipoPago` (`TipoPago`), `fechaInforme`, `comprobante`, `fechaConfirmacion`, `observacion`, `consentimientoRegistrado` |
| **Reglas asociadas** | Solo aplicable con `CONDENA_FIRME`; separacion estricta de pago voluntario; consentir condena y pagar pueden ser un solo acto (portal) |
| **Eventos que emite** | `PAGO_CONDENA_INFORMADO`, `PAGO_CONDENA_CONFIRMADO`, `PAGO_CONDENA_OBSERVADO`, `CONDENA_CONSENTIDA_Y_PAGO_REGISTRADO` |
| **Relacion con Acta** | 1 acta : 0..1 pago condena |
| **Dudas pendientes** | Comprobantes de pago condena (pendiente de cruce con spec); modelo de integracion con sistema de cobros |

### 2.6 Fallo

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Registro del resultado juridico dictado (absolutorio o condenatorio), con monto de condena si corresponde. |
| **Datos minimos persistibles** | `falloId`, `actaId`, `tipoFallo` (`ABSOLUTORIO` / `CONDENATORIO`), `montoCondena` (si condenatorio), `fechaDictado`, `operadorQueDict o` |
| **Reglas asociadas** | Solo desde `PENDIENTE_ANALISIS`; genera pieza documental; la notificacion positiva fija resultado final |
| **Eventos que emite** | `FALLO_ABSOLUTORIO_DICTADO`, `FALLO_CONDENATORIO_DICTADO` |
| **Relacion con Acta** | 1 acta : 0..N fallos (puede dictarse nuevo fallo post gestion externa) |
| **Dudas pendientes** | Si `Fallo` es entidad separada o parte de `ActaEvento` enriquecido: pendiente de cruce con spec |

### 2.7 Apelacion

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Registro del recurso de apelacion presentado por el infractor, con canal y resultado de la resolucion. |
| **Datos minimos persistibles** | `apelacionId`, `actaId`, `canal` (`PORTAL_INFRACTOR` / `PRESENCIAL_DIRECCION`), `fechaRegistro`, `estado` (presentada / resuelta), `resultadoResolucion` (`RECHAZADA` / `ACEPTADA_ABSUELVE`), `fechaResolucion` |
| **Reglas asociadas** | Solo con plazo de apelacion abierto; la resolucion puede cambiar resultado final a `ABSUELTO` si acepta |
| **Eventos que emite** | `APELACION_REGISTRADA`, `VENCIMIENTO_PLAZO_APELACION`, `APELACION_RESUELTA` |
| **Relacion con Acta** | 1 acta : 0..1 apelacion activa |
| **Dudas pendientes** | Elevacion de recurso a instancia externa: no modelado en prototipo; pendiente de cruce con spec |

### 2.8 Bloqueante de cierre material

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Representa un hecho operativo (medida preventiva, rodado secuestrado, documentacion retenida) que bloquea el cierre del expediente hasta su resolucion documental y cumplimiento material. |
| **Datos minimos persistibles** | `bloqueanteid`, `actaId`, `origen` (`OrigenBloqueanteCierreMaterialMock`), `estadoResolucionDocumental` (pendiente / resuelto), `estadoCumplimientoMaterial` (pendiente / cumplido), `documentoResolventId`, `fechaConstatacion`, `fechaResolucionDocumental`, `fechaCumplimientoMaterial` |
| **Reglas asociadas** | Resolucion documental y cumplimiento material son pasos distintos; el cierre requiere ambos; bloqueantes documentales y materiales no se colapsan |
| **Eventos que emite** | `BLOQUEANTE_REGISTRADO`, `RESOLUCION_DOCUMENTAL_BLOQUEANTE`, `CUMPLIMIENTO_MATERIAL_BLOQUEANTE` |
| **Relacion con Acta** | 1 acta : N bloqueantes (hasta 3 origenes distintos) |
| **Dudas pendientes** | Separacion formal entre bloqueantes documentales y bloqueantes materiales: pendiente de cruce con spec |

### 2.9 Gestion externa

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Registro de la derivacion a apremio o juzgado de paz, con tipo y resultado al reingresar. |
| **Datos minimos persistibles** | `gestionExternaId`, `actaId`, `tipo` (`APREMIO` / `JUZGADO_DE_PAZ`), `fechaDerivacion`, `estado` (activa / cerrada), `resultadoExterno` (`MODIFICA_MONTO` / `ABSUELVE`), `montoSugeridoPostGestion`, `fechaReingreso`, `accionPostReingreso` |
| **Reglas asociadas** | Derivacion requiere marca `DERIVAR_GESTION_EXTERNA`; resultado puede modificar monto condena; distintas variantes de reingreso segun tipo |
| **Eventos que emite** | `DERIVADO_A_APREMIO`, `DERIVADO_A_JUZGADO_DE_PAZ`, `REINGRESO_DESDE_GESTION_EXTERNA` y variantes especificas |
| **Relacion con Acta** | 1 acta : N gestiones externas historicas (puede reingresar y derivar de nuevo) |
| **Dudas pendientes** | Integracion con sistemas de apremio y juzgado: no modelada en prototipo |

### 2.10 Paralizacion

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad bajo Acta |
| **Responsabilidad** | Registro de la suspension administrativa con motivo y observacion; preserve trazabilidad de la bandeja anterior. |
| **Datos minimos persistibles** | `paralizacionId`, `actaId`, `motivoParalizacion` (`MotivoParalizacionActa`), `observacion`, `fechaParalizacion`, `bandejaPrevia`, `accionPreviaPreservada`, `fechaReactivacion` |
| **Reglas asociadas** | Transversal: desde cualquier bandeja operable; observacion debe preservarse; reactivacion vuelve a `PENDIENTE_ANALISIS` con marca `REVISION_POST_REACTIVACION` |
| **Eventos que emite** | `ACTA_PARALIZADA`, `ACTA_REACTIVADA` |
| **Relacion con Acta** | 1 acta : N paralizaciones historicas |
| **Dudas pendientes** | Si se necesita preservar la bandeja previa exacta al reingresar o solo la marca: pendiente de cruce con spec |

### 2.11 Lote de correo postal

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad independiente (no bajo Acta directamente) |
| **Responsabilidad** | Agrupa notificaciones pendientes de envio postal para su procesamiento masivo con referencia externa y trazabilidad. |
| **Datos minimos persistibles** | `loteId`, `fechaGeneracion`, `estado` (activo / anulado), `referenciaExterna`, `cantidadItems` |
| **Reglas asociadas** | Anulacion de lote es posible; cada notificacion referencia su loteId; el resultado del lote se procesa en batch |
| **Eventos que emite** | `LOTE_CORREO_GENERADO`, `LOTE_CORREO_ANULADO` |
| **Relacion con Acta** | Via notificacion (N notificaciones pueden pertenecer al mismo lote) |
| **Dudas pendientes** | Modelo de integracion postal real: pendiente de diseno |

### 2.12 Evento de auditoria

| Campo | Detalle |
|---|---|
| **Tipo** | Entidad append-only bajo Acta |
| **Responsabilidad** | Registro inmutable de cada accion ejecutada sobre el expediente, con actor, canal, bloque origen y destino. |
| **Datos minimos persistibles** | `eventoId`, `actaId`, `fechaHora`, `tipoEvento`, `bloqueOrigen`, `bloqueDestino`, `descripcion`, `actor`, `canal` |
| **Reglas asociadas** | Append-only: ningun evento se edita ni se borra; historial siempre disponible |
| **Eventos que emite** | Es en si mismo el evento persistible |
| **Relacion con Acta** | 1 acta : N eventos (historial completo) |
| **Dudas pendientes** | Estructura de `actor` / `canal` en dominio real: pendiente de cruce con spec |

---

## 3. Estados persistibles vs proyecciones

### 3.1 Estados que deben persistirse

| Estado | Donde vive | Descripcion |
|---|---|---|
| `bloqueActual` | Acta | Bloque juridico/procesal actual: `D1_CAPTURA`, `D2_ENRIQUECIMIENTO`, `D4_NOTIFICACION`, `D5_ANALISIS`, `GESTION_EXTERNA` |
| `estadoProcesoActual` | Acta | Estado agregador dentro del bloque: `PENDIENTE_REVISION`, `PENDIENTE_FIRMA`, `PENDIENTE_ENVIO`, `EN_GESTION_EXTERNA`, etc. |
| `situacionAdministrativaActual` | Acta | Situacion administrativa del expediente: `ACTIVA`, `PARALIZADA`, etc. |
| `estaCerrada` | Acta | Flag booleano: el expediente fue cerrado por accion explicita |
| `permiteReingreso` | Acta | Flag booleano: el expediente archivado puede reingresarse |
| `motivoArchivo` | Acta | Causa del archivo: `ARCHIVO_DESDE_ANALISIS_DIRECTO`, `ARCHIVO_POST_EVALUACION_VENCIMIENTO`, `NULIDAD` |
| `tipoGestionExterna` | GestionExterna / Acta | `APREMIO` o `JUZGADO_DE_PAZ` mientras el acta esta en `GESTION_EXTERNA` |
| `observacionParalizacion` | Paralizacion | Texto libre de motivo; hoy volatil en el store, debe persistirse |
| `resultadoFinal` | Acta / CerrabilidadSupport | `SIN_RESULTADO_FINAL`, `ABSUELTO`, `PAGO_CONFIRMADO`, `CONDENADO`, `CONDENA_FIRME` |
| `situacionPago` | PagoVoluntario | `SIN_PAGO`, `SOLICITADO`, `PAGO_INFORMADO`, `PENDIENTE_CONFIRMACION`, `CONFIRMADO`, `OBSERVADO`, `VENCIDO` |
| `situacionPagoCondena` | PagoCondena | `NO_APLICA`, `PENDIENTE`, `INFORMADO`, `CONFIRMADO`, `OBSERVADO` |
| `tipoPago` | Acta / PagoVoluntario | `NO_APLICA`, `VOLUNTARIO`, `CONDENA` |
| `estadoDocumento` | Documento | `EMITIDO`, `PENDIENTE_FIRMA`, `FIRMADO`, `ADJUNTO` |
| `estadoNotificacion` | Notificacion | `PENDIENTE_PREPARACION`, `LISTA_PARA_ENVIO`, `ENVIADA`, `ENTREGADA`, `NEGATIVA`, `VENCIDA`, `SIN_EFECTO` |
| `resultadoNotificacion` | Notificacion | `SIN_RESULTADO`, `POSITIVA`, `NEGATIVA`, `VENCIDA`, `SUPERADA_POR_PORTAL` |
| `montoCondena` | Fallo / PagoCondena | Monto fijado al dictar fallo condenatorio |
| `montoPagoVoluntario` | PagoVoluntario | Monto habilitado por el operador para pago voluntario |
| `resultadoExternoPostGestion` | GestionExterna | `MODIFICA_MONTO` o `ABSUELVE` |
| `motivoParalizacion` | Paralizacion | Enum: `ESPERA_DOCUMENTAL`, `ESPERA_INFORME_EXTERNO`, etc. |
| `origenBloqueanteMaterial` | BloqueanteciErreMaterial | `MEDIDA_PREVENTIVA_ACTIVA`, `RODADO_SECUESTRADO`, `DOCUMENTACION_RETENIDA` |
| `estadoCumplimientoMaterial` | BloqueanteCierreMaterial | Pendiente / cumplido |

### 3.2 Proyecciones que NO son verdad primaria

| Proyeccion | Descripcion |
|---|---|
| `bandejaActual` | Calculada desde `bloqueActual` + `estadoProcesoActual` + `estaCerrada` + `motivoArchivo`; puede cachearse con invalidacion por evento pero no es verdad primaria |
| `subBandeja` | Calculada desde `accionPendiente` + `situacionPago` + `resultadoFinal` + `cerrable`; es filtro operativo |
| `accionPendiente` | Marca operativa calculada o derivada del ultimo evento relevante; hoy vive en mapa volatil del store; pendiente de definir si se persiste como campo derivado en Acta o se recalcula desde eventos |
| `cerrabilidad` | Calculada por `CerrabilidadSupport` desde `resultadoFinal` + `pendientesBloqueantes` + `situacionPagoCondena`; nunca se persiste directa |
| `accionesDisponibles` | Calculadas en backend desde estado + predicados; no se persisten |
| `pendientesBloqueantes` | Calculados desde bloqueantes activos sin cumplimiento material; no se persisten directos |
| Conteos de bandeja | Calculados desde proyeccion real de bandejas |
| `resultadoFinalHelper` | Resultado calculado en `PrototipoResultadoFinalHelper`; deriva de eventos y estados |

### 3.3 Aclaracion sobre `accionPendiente`

La marca `accionPendiente` es hoy un mapa volatil en memoria
(`accionPendientePorActa`). En el dominio real puede modelarse como:

- campo persistido en Acta que se actualiza en cada transicion relevante, o
- campo calculado desde el ultimo evento significativo del historial.

Pendiente de decision: cruzar con spec sobre si es estado real o proyeccion
operativa.

---

## 4. Eventos persistibles

### 4.1 Ciclo del expediente

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `ACTA_LABRADA` | Alta del acta en el sistema | `actaId`, `numeroActa`, `inspector`, `fecha`, `resumenHecho`, `tipoActa`, `dependencia` | Inspector / sistema labrado | Acta en `D1_CAPTURA` / `ACTAS_EN_ENRIQUECIMIENTO` | `MockDataFactory` / alta real (futura) |
| `ACTA_ENRIQUECIDA` | Completado el enriquecimiento D2 | `actaId`, `fecha`, `operador` | Operador | Avance a siguiente bandeja | `ACCION_COMPLETAR_ENRIQUECIMIENTO` / pendiente |
| `ACTA_ENVIADA_A_NOTIFICACION` | Envio manual a circuito de notificacion | `actaId`, `fecha`, `operador` | Operador | Bandeja a `PENDIENTE_NOTIFICACION` | `PrototipoArchivoParalizacionController#enviarANotificacion` |
| `ACTA_CERRADA` | Cierre explicito del expediente | `actaId`, `fecha`, `operador`, `motivoCierre` | Operador | `estaCerrada=true`, bandeja `CERRADAS` | `CierreSupport` / `cerrar-acta` |
| `ACTA_ARCHIVADA` | Archivo administrativo | `actaId`, `fecha`, `operador`, `motivoArchivo` | Operador | Bandeja `ARCHIVO` | `ArchivoReingresoSupport` / `archivar-acta` |
| `ACTA_ARCHIVADA_POR_VENCIMIENTO` | Archivo post evaluacion de notificacion vencida | `actaId`, `fecha`, `operador`, `motivoArchivo` | Operador | Bandeja `ARCHIVO` | `ArchivoReingresoSupport` / `archivar-por-vencimiento` |
| `ACTA_REINGRESADA` | Reingreso desde archivo | `actaId`, `fecha`, `operador`, `motivoArchivoPrevio` | Operador | Bandeja `PENDIENTE_ANALISIS`, marca `REVISION_POST_REINGRESO` | `ArchivoReingresoSupport` / `reingresar-acta` |
| `ACTA_PARALIZADA` | Paralizacion administrativa | `actaId`, `fecha`, `operador`, `motivo`, `observacion` | Operador | Bandeja `PARALIZADAS` | `ParalizacionReactivacionSupport` / `paralizar-acta` |
| `ACTA_REACTIVADA` | Reactivacion desde paralizada | `actaId`, `fecha`, `operador` | Operador | Bandeja `PENDIENTE_ANALISIS`, marca `REVISION_POST_REACTIVACION` | `ParalizacionReactivacionSupport` / `reactivar-acta` |
| `ACTA_NULIDAD_REGISTRADA` | Anulacion por nulidad | `actaId`, `fecha`, `operador` | Operador | Bandeja `ARCHIVO`, `motivoArchivo=NULIDAD`, `permiteReingreso=true` | `PrototipoArchivoParalizacionController#anularActa` |

### 4.2 Documentos y firma

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `DOCUMENTO_GENERADO` | Generacion de pieza documental | `actaId`, `documentoId`, `tipoDocumento`, `fecha`, `operador` | Operador | Documento en `PENDIENTE_FIRMA`; bandeja puede transicionar a `PENDIENTE_FIRMA` | `PiezasFirmaSupport` / `generar-*` |
| `DOCUMENTO_FIRMADO` | Firma individual de una pieza | `actaId`, `documentoId`, `tipoDocumento`, `fecha`, `firmante` | Operador | Documento en `FIRMADO` | `PiezasFirmaSupport` / `firmar-documento/{documentoId}` |
| `FIRMA_CERRADA` | Todas las piezas del circuito firmadas | `actaId`, `fecha` | Sistema | Transicion de bandeja (a `PENDIENTE_NOTIFICACION` o `CERRADAS` por nulidad) | `PiezasFirmaSupport` / `firmar-documento/{documentoId}` |

### 4.3 Notificaciones

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `NOTIFICACION_PREPARADA` | Generacion de notificacion lista para envio | `actaId`, `notificacionId`, `tipo`, `canal`, `destinatario`, `fecha` | Sistema / operador | Notificacion en `LISTA_PARA_ENVIO` | `NotificacionSupport` / preparacion interna |
| `NOTIFICACION_POSITIVA` | Notificacion entregada | `actaId`, `notificacionId`, `fecha`, `canal` | Operador / notificador | Acta puede transicionar; si es fallo, fija resultado final | `NotificacionSupport` / `registrar-notificacion-positiva` |
| `NOTIFICACION_NEGATIVA` | Notificacion no entregada | `actaId`, `notificacionId`, `fecha`, `canal` | Operador / notificador | `PENDIENTE_ANALISIS` con marca `REINTENTAR_NOTIFICACION` | `NotificacionSupport` / `registrar-notificacion-negativa` |
| `NOTIFICACION_VENCIDA` | Notificacion vencida sin resultado | `actaId`, `notificacionId`, `fecha`, `canal` | Operador / sistema | `PENDIENTE_ANALISIS` con marca `EVALUAR_NOTIFICACION_VENCIDA` | `NotificacionSupport` / `registrar-notificacion-vencida` |
| `REINTENTO_NOTIFICACION` | Reintento por no entrega | `actaId`, `notificacionId`, `fecha` | Operador | Bandeja a `PENDIENTE_NOTIFICACION` o `EN_NOTIFICACION` | `NotificacionSupport` / `reintentar-notificacion` |
| `REINTENTO_NOTIFICACION_VENCIDA` | Reintento post vencimiento | `actaId`, `notificacionId`, `fecha` | Operador | Bandeja a `PENDIENTE_NOTIFICACION` | `NotificacionSupport` / `reintentar-notificacion-vencida` |
| `NOTIFICACION_PORTAL_CONFIRMADA` | Confirmacion de visualizacion en portal | `actaId`, `notificacionId`, `fecha`, `codigoQr` | Infractor / portal | Notificacion `ENTREGADA`; otras pendientes quedan `SIN_EFECTO` | `PortalInfractorSupport` / `confirmar-visualizacion-notificacion` |
| `NOTIFICACION_SUPERADA_POR_PORTAL` | Notificacion paralela descartada por portal | `actaId`, `notificacionId`, `fecha` | Sistema | Notificacion `SIN_EFECTO` | `PortalInfractorSupport` / efecto lateral |
| `ACUSE_NOTIFICADOR_MUNICIPAL` | Acuse de notificacion via notificador municipal | `actaId`, `notificacionId`, `fecha`, `referencia` | Notificador municipal | Estado de notificacion actualizado | `NotificadorMunicipalSupport` / `acuse` |
| `LOTE_CORREO_GENERADO` | Generacion de lote postal | `loteId`, `fecha`, `cantidadItems` | Sistema | Notificaciones del lote en `ENVIADA` | `CorreoPostalNotificacionSupport` / `generar-lote` |
| `LOTE_CORREO_ANULADO` | Anulacion de lote postal | `loteId`, `fecha`, `operador` | Operador | Lote anulado | `CorreoPostalNotificacionSupport` / `anular-lote` |

### 4.4 Fallo y apelacion

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `FALLO_ABSOLUTORIO_DICTADO` | Fallo absolutorio dictado | `actaId`, `falloId`, `fecha`, `operador` | Operador | Genera pieza `FALLO_ABSOLUTORIO` en `PENDIENTE_FIRMA`; bandeja a `PENDIENTE_FIRMA` | `FalloPlazoApelacionSupport` / `dictar-fallo-absolutorio` |
| `FALLO_CONDENATORIO_DICTADO` | Fallo condenatorio dictado | `actaId`, `falloId`, `fecha`, `operador`, `montoCondena` | Operador | Genera pieza `FALLO_CONDENATORIO` en `PENDIENTE_FIRMA`; fija `montoCondena` | `FalloPlazoApelacionSupport` / `dictar-fallo-condenatorio` |
| `RESULTADO_FINAL_ABSUELTO` | Notificacion positiva de fallo absolutorio | `actaId`, `fecha` | Sistema | `resultadoFinal=ABSUELTO`; bandeja a `PENDIENTE_ANALISIS` | `FalloPlazoApelacionSupport` / efecto de notificacion positiva |
| `RESULTADO_FINAL_CONDENADO` | Notificacion positiva de fallo condenatorio | `actaId`, `fecha` | Sistema | `resultadoFinal=CONDENADO`; plazo de apelacion abierto | `FalloPlazoApelacionSupport` / efecto de notificacion positiva |
| `APELACION_REGISTRADA` | Registro de apelacion por infractor | `actaId`, `apelacionId`, `canal`, `fecha` | Infractor / operador | Marca apelacion presentada | `FalloPlazoApelacionSupport` / `registrar-apelacion` |
| `VENCIMIENTO_PLAZO_APELACION` | Vencimiento de plazo sin apelacion | `actaId`, `fecha` | Operador / sistema | `resultadoFinal=CONDENA_FIRME`; `situacionPagoCondena=PENDIENTE` | `FalloPlazoApelacionSupport` / `registrar-vencimiento-plazo-apelacion` |
| `APELACION_RESUELTA` | Resolucion del recurso | `actaId`, `apelacionId`, `resultado`, `fecha`, `operador` | Operador | Si acepta: `resultadoFinal=ABSUELTO`; si rechaza: `CONDENA_FIRME` | `FalloPlazoApelacionSupport` / `resolver-apelacion` |
| `ABSOLUCION_MARCADA_DIRECTO` | Absolucion sin pasar por circuito de fallo | `actaId`, `fecha`, `operador` | Operador | `resultadoFinal=ABSUELTO` | `FalloPlazoApelacionSupport` / `marcar-resultado-final-absuelto` |

### 4.5 Pagos

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `PAGO_VOLUNTARIO_SOLICITADO` | Solicitud inicial de pago voluntario | `actaId`, `fecha`, `canal` | Infractor / portal | `situacionPago=SOLICITADO`; marca `EVALUAR_PAGO_VOLUNTARIO` en analisis | `PagoVoluntarioSupport` / `registrar-solicitud-pago-voluntario` |
| `MONTO_PAGO_VOLUNTARIO_FIJADO` | Operador fija el monto habilitado | `actaId`, `monto`, `fecha`, `operador` | Operador | `montoPagoVoluntario` asignado | `PagoVoluntarioSupport` / `fijar-monto-pago-voluntario` |
| `PAGO_INFORMADO_REGISTRADO` | Infractor registra que pago | `actaId`, `comprobante`, `fecha` | Infractor / portal | `situacionPago=PAGO_INFORMADO`; marca `VERIFICAR_PAGO_INFORMADO` | `PagoInformadoSupport` / `registrar-pago-informado` |
| `COMPROBANTE_ADJUNTADO` | Adjuntar comprobante de pago | `actaId`, `comprobante`, `fecha` | Infractor / operador | Comprobante registrado | `PagoInformadoSupport` / `adjuntar-comprobante-pago-informado` |
| `PAGO_INFORMADO_CONFIRMADO` | Operador confirma el pago | `actaId`, `fecha`, `operador` | Operador | `situacionPago=CONFIRMADO`; `resultadoFinal=PAGO_CONFIRMADO` | `PagoInformadoSupport` / `confirmar-pago-informado` |
| `PAGO_INFORMADO_OBSERVADO` | Operador observa el pago | `actaId`, `motivo`, `fecha`, `operador` | Operador | `situacionPago=OBSERVADO` | `PagoInformadoSupport` / `observar-pago-informado` |
| `PAGO_VOLUNTARIO_VENCIDO` | Vence el plazo de pago voluntario | `actaId`, `fecha` | Operador / sistema | `situacionPago=VENCIDO` | `PagoVoluntarioSupport` / `registrar-vencimiento-pago-voluntario` |
| `PAGO_VOLUNTARIO_EXTERNO_CONFIRMADO` | Confirmacion desde sistema externo | `actaId`, `referencia`, `fecha` | Sistema externo | `situacionPago=CONFIRMADO`; `resultadoFinal=PAGO_CONFIRMADO` | `PagoVoluntarioSupport` / `confirmar-pago-voluntario-externo` |
| `PAGO_CONDENA_INFORMADO` | Infractor informa pago de condena | `actaId`, `comprobante`, `fecha` | Infractor / portal | `situacionPagoCondena=INFORMADO` | `PagoCondenaSupport` / `informar-pago-condena` |
| `PAGO_CONDENA_CONFIRMADO` | Operador confirma pago de condena | `actaId`, `fecha`, `operador` | Operador | `situacionPagoCondena=CONFIRMADO`; habilita cierre | `PagoCondenaSupport` / `confirmar-pago-condena` |
| `PAGO_CONDENA_OBSERVADO` | Operador observa pago de condena | `actaId`, `motivo`, `fecha`, `operador` | Operador | `situacionPagoCondena=OBSERVADO` | `PagoCondenaSupport` / `observar-pago-condena` |
| `CONDENA_CONSENTIDA_Y_PAGO_REGISTRADO` | Consentimiento + pago en un solo acto (portal) | `actaId`, `comprobante`, `fecha` | Infractor / portal | `situacionPagoCondena=INFORMADO`; `resultadoFinal=CONDENA_FIRME` preservado | `PagoCondenaSupport` / `consentir-condena-y-registrar-pago` |

### 4.6 Bloqueantes materiales

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `CONSTATACION_MATERIAL_TEMPRANA` | Registro de hecho material en D1/D2 | `actaId`, `tipo` (`TipoConstatacionMaterialTemprana`), `documentoId`, `fecha` | Inspector / operador | Ancla documental en expediente; activa pendiente bloqueante | `CerrabilidadSupport` / `registrar-constatacion-material-temprana` |
| `MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO` | Medida preventiva generada en tramite | `actaId`, `documentoId`, `fecha`, `operador` | Operador | Activa pendiente bloqueante `MEDIDA_PREVENTIVA_ACTIVA` | `CerrabilidadSupport` / `registrar-medida-preventiva-posterior` |
| `ORIGEN_BLOQUEANTE_RECONOCIDO` | Reconocimiento formal del origen del bloqueo | `actaId`, `origen`, `fecha`, `operador` | Operador | Traza de trazabilidad; idempotente si ya existe ancla | `CerrabilidadSupport` / `reconocer-origen-bloqueo-cierre-material` |
| `RESOLUCION_DOCUMENTAL_BLOQUEANTE` | Emision del documento resolutorio | `actaId`, `origen`, `documentoId`, `tipoDocumento`, `fecha`, `operador` | Operador | Resolucion documental registrada; cumplimiento material sigue pendiente | `CerrabilidadSupport` / `registrar-resolucion-bloqueo-cierre` |
| `CUMPLIMIENTO_MATERIAL_BLOQUEANTE` | Cumplimiento material efectivo | `actaId`, `origen`, `fecha`, `operador` | Operador | Bloqueo levantado; puede habilitar cerrabilidad | `CerrabilidadSupport` / `registrar-cumplimiento-material-bloqueo-cierre` |

### 4.7 Gestion externa

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `DERIVADO_A_APREMIO` | Derivacion efectiva a apremio | `actaId`, `fecha`, `operador` | Operador | Bandeja `GESTION_EXTERNA`; `tipoGestionExterna=APREMIO` | `GestionExternaSupport` / `derivar-a-apremio` |
| `DERIVADO_A_JUZGADO_DE_PAZ` | Derivacion efectiva a juzgado | `actaId`, `fecha`, `operador` | Operador | Bandeja `GESTION_EXTERNA`; `tipoGestionExterna=JUZGADO_DE_PAZ` | `GestionExternaSupport` / `derivar-a-juzgado-de-paz` |
| `APREMIO_REINGRESO_SIN_PAGO` | Apremio devuelve sin pago | `actaId`, `fecha`, `operador` | Operador | Bandeja `PENDIENTE_ANALISIS`; marca `REVISION_POST_GESTION_EXTERNA` | `GestionExternaSupport` / `apremio-reingresar-sin-pago` |
| `APREMIO_PAGO_REGISTRADO` | Apremio registra pago externo | `actaId`, `monto`, `fecha`, `operador` | Operador | Pago condena confirmado externamente | `GestionExternaSupport` / `apremio-registrar-pago` |
| `APREMIO_REINGRESO_MONTO_MODIFICADO` | Apremio devuelve con monto modificado | `actaId`, `nuevoMonto`, `fecha`, `operador` | Operador | Monto condena actualizado; marca `DICTAR_FALLO_POST_GESTION_EXTERNA` | `GestionExternaSupport` / `apremio-reingresar-monto-modificado` |
| `APREMIO_REINGRESO_ABSUELTO` | Apremio devuelve con absolucion | `actaId`, `fecha`, `operador` | Operador | `resultadoExterno=ABSUELVE`; marca `DICTAR_FALLO_POST_GESTION_EXTERNA` | `GestionExternaSupport` / `apremio-reingresar-absuelto` |
| `JUZGADO_REINGRESO_ABSUELTO` | Juzgado devuelve con absolucion | `actaId`, `fecha`, `operador` | Operador | `resultadoExterno=ABSUELVE`; marca `DICTAR_FALLO_POST_GESTION_EXTERNA` | `GestionExternaSupport` / `juzgado-reingresar-absuelto` |
| `JUZGADO_CONDENA_CONFIRMADA` | Juzgado confirma condena | `actaId`, `fecha`, `operador` | Operador | `CONDENA_FIRME` confirmada | `GestionExternaSupport` / `juzgado-reingresar-condena-confirmada` |
| `JUZGADO_REINGRESO_MONTO_MODIFICADO` | Juzgado devuelve con monto modificado | `actaId`, `nuevoMonto`, `fecha`, `operador` | Operador | Monto condena actualizado; marca `DICTAR_FALLO_POST_GESTION_EXTERNA` | `GestionExternaSupport` / `juzgado-reingresar-monto-modificado` |

### 4.8 Portal infractor

| Nombre sugerido | Cuando ocurre | Datos minimos | Actor/Canal | Impacto en estado | Support / Endpoint actual |
|---|---|---|---|---|---|
| `VISUALIZACION_PORTAL` | Infractor consulta su acta via QR | `actaId`, `codigoQr`, `fecha` | Infractor / portal | Registro de acceso; no cambia estado | `PortalInfractorSupport` / `GET /infractor/actas/{codigoQr}` |
| `APELACION_PORTAL_REGISTRADA` | Infractor registra apelacion desde portal | `actaId`, `fecha` | Infractor / portal | Misma logica que `APELACION_REGISTRADA` con `canal=PORTAL_INFRACTOR` | `PortalInfractorSupport` / `registrar-apelacion` |
| `PAGO_VOLUNTARIO_PORTAL_SOLICITADO` | Infractor solicita pago voluntario desde portal | `actaId`, `fecha` | Infractor / portal | Misma logica que solicitud interna | `PortalInfractorSupport` / `solicitar-pago-voluntario` |
| `PAGO_VOLUNTARIO_PORTAL_REGISTRADO` | Infractor paga desde portal | `actaId`, `comprobante`, `fecha` | Infractor / portal | `situacionPago=PAGO_INFORMADO` | `PortalInfractorSupport` / `pagar-voluntario` |

---

## 5. Cosas que NO deben modelarse como dominio real

| Artefacto | Razon |
|---|---|
| `PrototipoStore` | Store en memoria transitorio; sera reemplazado en su totalidad por persistencia real |
| Mapas mock (`actas`, `eventosPorActa`, `documentosPorActa`, etc.) | Estructuras volatiles sin persistencia; cada una tendra equivalente real separado |
| `MockDataFactory` | Genera dataset de demo; no es dominio; se retirara cuando las Etapas 1-3 esten validadas |
| `crearActaMockDemo` / `POST /actas/mock` | Infraestructura de demo; no aplica en produccion |
| `POST /reset` | Reinicializa el store de demo; no aplica en produccion |
| `accionPendientePorActa` como mapa volatil | Es un mapa de marcas operativas en memoria; debe convertirse en campo persistido o calculado desde eventos |
| `situacionPagoPorActa` como mapa volatil | Debe persistirse como parte de la entidad `PagoVoluntario` real |
| `pagoInformadoPorActa` como mapa volatil | Idem |
| `montoPagoVoluntarioPorActa` como mapa volatil | Idem |
| `montoCondenaPorActa` como mapa volatil | Debe persistirse en `Fallo` o `Acta` |
| `situacionPagoCondenaPorActa` como mapa volatil | Debe persistirse en `PagoCondena` real |
| `tipoPagoPorActa` como mapa volatil | Idem |
| `resultadoExternoPostGestionPorActa` como mapa volatil | Debe persistirse en `GestionExterna` real |
| `montoCondenaSugeridoPostGestionExternaPorActa` como mapa volatil | Idem |
| `observacionParalizacionPorActa` como mapa volatil | Debe persistirse en `Paralizacion` real |
| `contadorActaLabradoMockDemo` | Contador de infraestructura de demo; sin equivalente en dominio real |
| `dependenciaDemoPorActa`, `tipoActaDemoPorActa`, `patenteVehiculoPorActa` | Datos demo; en el dominio real viven en la entidad `Acta` real o en relaciones propias |
| Bandeja como verdad primaria | Bandeja es proyeccion calculada desde el estado del expediente |
| Sub-bandeja como verdad primaria | Sub-bandeja es proyeccion operativa; no debe persistirse |
| Conteos demo de bandeja | Calculados en tiempo de consulta desde la proyeccion real |
| Respuestas demo de correo/notificador | Endpoints `/respuestas/procesar-demo` son infraestructura de simulacion; no aplican en produccion |
| `plazoApelacionAbiertoPorActa` como mapa volatil en `FalloPlazoApelacionSupport` | Debe persistirse como campo en `Apelacion` o derivarse desde evento `RESULTADO_FINAL_CONDENADO` |
| `apelacionPresentadaPorActa`, `apelacionResueltaPorActa` como mapas volatiles | Deben persistirse en entidad `Apelacion` real |
| `origenesBloqueantesPorActa`, `cumplimientoMaterialEfectivoPorActa` como mapas volatiles | Deben persistirse en entidades `BloqueanteCierreMaterial` real |
| `resultadoFinalPorActa` como mapa volatil en `CerrabilidadSupport` | Debe persistirse en `Acta` o derivarse desde historial de eventos |

---

## 6. Tabla de supports vs dominio real

| Support prototipo | Regla de dominio que representa | Entidad/agregado candidato | Evento persistible asociado | Reusable / Transitorio / Proyeccion | Observaciones |
|---|---|---|---|---|---|
| `CerrabilidadSupport` | Condicion de cierre: resultado final habilitante + ausencia de bloqueantes materiales | `Acta`, `BloqueanteCierreMaterial` | `CUMPLIMIENTO_MATERIAL_BLOQUEANTE`, `RESOLUCION_DOCUMENTAL_BLOQUEANTE` | Logica de calculo reusable como servicio de dominio | El calculo mismo no persiste; persisten las entidades que consulta |
| `CierreSupport` | Cierre explicito del expediente | `Acta` (campo `estaCerrada`) | `ACTA_CERRADA` | Logica reusable como caso de uso de cierre | Una unica puerta de cierre: no cambiar |
| `FalloPlazoApelacionSupport` | Fallo, plazo de apelacion, condena firme | `Fallo`, `Apelacion`, `Acta` | `FALLO_ABSOLUTORIO_DICTADO`, `FALLO_CONDENATORIO_DICTADO`, `APELACION_REGISTRADA`, `VENCIMIENTO_PLAZO_APELACION`, `APELACION_RESUELTA`, `RESULTADO_FINAL_CONDENADO`, `RESULTADO_FINAL_ABSUELTO` | Logica reusable como servicio de dominio juridico | Los mapas volatiles deben convertirse en entidades persistidas |
| `PagoVoluntarioSupport` | Circuito de pago voluntario previo a fallo | `PagoVoluntario` | `PAGO_VOLUNTARIO_SOLICITADO`, `MONTO_PAGO_VOLUNTARIO_FIJADO`, `PAGO_INFORMADO_REGISTRADO`, `PAGO_INFORMADO_CONFIRMADO`, `PAGO_INFORMADO_OBSERVADO`, `PAGO_VOLUNTARIO_VENCIDO`, `PAGO_VOLUNTARIO_EXTERNO_CONFIRMADO` | Logica reusable como servicio de pago voluntario | Separado completamente de pago condena |
| `PagoInformadoSupport` | Registro y confirmacion de comprobante de pago | `PagoVoluntario` (parte del circuito) | `PAGO_INFORMADO_REGISTRADO`, `COMPROBANTE_ADJUNTADO`, `PAGO_INFORMADO_CONFIRMADO`, `PAGO_INFORMADO_OBSERVADO` | Transitorio (puede fusionarse con `PagoVoluntarioSupport` en dominio real) | Hoy son supports separados pero modelan la misma entidad |
| `PagoCondenaSupport` | Pago de condena firme | `PagoCondena` | `PAGO_CONDENA_INFORMADO`, `PAGO_CONDENA_CONFIRMADO`, `PAGO_CONDENA_OBSERVADO`, `CONDENA_CONSENTIDA_Y_PAGO_REGISTRADO` | Logica reusable como servicio de pago condena | Separado completamente de pago voluntario |
| `GestionExternaSupport` | Derivacion a apremio/juzgado y resultados de retorno | `GestionExterna` | `DERIVADO_A_APREMIO`, `DERIVADO_A_JUZGADO_DE_PAZ`, variantes de reingreso | Logica reusable como servicio de gestion externa | El resultado puede modificar monto condena: trazabilidad critica |
| `ArchivoReingresoSupport` | Archivo con motivo y reingreso desde archivo | `Acta` (campos `motivoArchivo`, `permiteReingreso`) | `ACTA_ARCHIVADA`, `ACTA_ARCHIVADA_POR_VENCIMIENTO`, `ACTA_REINGRESADA` | Logica reusable como servicio de archivo | `motivoArchivo` distingue origenes; pendiente cruzar con spec cuales permiten reingreso |
| `ParalizacionReactivacionSupport` | Paralizacion administrativa transversal | `Paralizacion` | `ACTA_PARALIZADA`, `ACTA_REACTIVADA` | Logica reusable como servicio de paralizacion | La observacion de paralizacion es volatil hoy; debe persistirse |
| `BandejaConsultaSupport` | Proyeccion de bandejas y filtros operativos | Proyeccion (no entidad) | — | Proyeccion: reescribir como consulta real sobre datos persistidos | La bandeja no es verdad primaria |
| `PortalInfractorSupport` | Vista ciudadana y acciones del portal | Proyeccion + acciones sobre `Acta`, `PagoVoluntario`, `Apelacion`, `Notificacion` | `VISUALIZACION_PORTAL`, `APELACION_PORTAL_REGISTRADA`, `PAGO_VOLUNTARIO_PORTAL_SOLICITADO`, `PAGO_VOLUNTARIO_PORTAL_REGISTRADO`, `NOTIFICACION_PORTAL_CONFIRMADA` | Logica de predicado reusable; resolcion por QR pendiente de diseno | No exponer estados internos en respuestas del portal |
| `PiezasFirmaSupport` | Produccion de piezas documentales y firma individual | `Documento` | `DOCUMENTO_GENERADO`, `DOCUMENTO_FIRMADO`, `FIRMA_CERRADA` | Logica reusable como servicio de firma; depende de servicio documental real | La firma de nulidad cierra el expediente: precondicion critica |
| `NotificacionSupport` | Notificacion interna (positiva, negativa, vencida, reintento) | `Notificacion` | `NOTIFICACION_POSITIVA`, `NOTIFICACION_NEGATIVA`, `NOTIFICACION_VENCIDA`, `REINTENTO_NOTIFICACION`, `REINTENTO_NOTIFICACION_VENCIDA` | Logica reusable como servicio de notificacion | Coordinar con `FalloPlazoApelacionSupport` para notificacion de fallo |
| `CorreoPostalNotificacionSupport` | Generacion de lotes y trazabilidad postal | `Notificacion`, `LoteCorreo` | `LOTE_CORREO_GENERADO`, `LOTE_CORREO_ANULADO`, respuestas de resultado | Transitorio (tiene dependencias externas); pendiente de diseno de integracion real | Integrar al ultimo segun `PLAN_INTEGRACION_DOMINIO_REAL.md` |
| `NotificadorMunicipalSupport` | Notificacion via notificador municipal y acuse | `Notificacion` | `ACUSE_NOTIFICADOR_MUNICIPAL` | Transitorio (tiene dependencias externas); pendiente de diseno de integracion real | Integrar junto con correo postal |
| `PrototipoStoreUtil` | Utilidades internas del store | — | — | Descartable (infraestructura mock) | Sin equivalente en dominio real; las utilidades pasan a servicios y repositorios reales |
| `MockDataFactory` | Generacion de dataset de demo | — | — | Descartable | No es dominio; retirar segun criterio de `PLAN_INTEGRACION_DOMINIO_REAL.md` sec. 9 |

---

## 7. Pendientes para el proximo slice (cruce con spec)

### 7.1 Entidades que parecen ya definidas en spec (verificar)

- `Acta` como unidad principal: verificar campos en `spec/dominio`.
- `ActaEvento` como append-only: verificar estructura en `spec/dominio` y
  `spec/backend`.
- `ActaSnapshot` como proyeccion operativa derivada: verificar si hay schema en
  `backend/persistencia`.
- Enums de estados: `bloqueActual`, `estadoProcesoActual`,
  `situacionAdministrativaActual`: verificar catalogo real en spec.
- Bandejas como proyeccion: verificar en `spec/03-bandejas/`.

### 7.2 Entidades que parecen faltar en spec o pendientes de diseno

- `Documento` / `Pieza`: no hay referencia a esquema real de repositorio
  documental ni a servicio de firma real.
- `Notificacion` como entidad real: la estructura mock existe, pero no hay
  diseno de schema real.
- `PagoVoluntario` y `PagoCondena`: separados en el prototipo pero sin diseno
  de entidad real.
- `Fallo`: no hay entidad explicita; puede ser evento enriquecido o entidad
  propia.
- `Apelacion`: no hay entidad explicita.
- `BloqueanteCierreMaterial`: no hay entidad explicita en spec.
- `GestionExterna`: no hay entidad explicita.
- `Paralizacion` como entidad con observacion persistida: no hay diseno.
- `LoteCorreo` como entidad propia: no hay diseno.
- Mecanismo de resolucion de `codigoQr` a `actaId`: no definido.

### 7.3 Estados / enums a verificar contra spec

- Valores de `bloqueActual` (`D1_CAPTURA`, `D2_ENRIQUECIMIENTO`,
  `D4_NOTIFICACION`, `D5_ANALISIS`, `GESTION_EXTERNA`): verificar contra
  `spec/dominio` o `spec/backend`.
- Valores de `situacionAdministrativaActual`: verificar catalogo.
- `ResultadoFinalCierreMock` (`SIN_RESULTADO_FINAL`, `ABSUELTO`,
  `PAGO_CONFIRMADO`, `CONDENADO`, `CONDENA_FIRME`): verificar contra spec.
- `SituacionPagoMock` y `SituacionPagoCondena`: verificar catalogo en spec.
- `MotivoParalizacionActa`: verificar si hay mas valores en spec.
- `motivoArchivo`: verificar si hay valores adicionales en spec.

### 7.4 Eventos a verificar

- Nombres de eventos sugeridos en la seccion 4: verificar si spec tiene
  nomenclatura propia.
- Estructura de `actor` y `canal` en eventos: pendiente de definicion.
- Eventos de labrado y enriquecimiento: no modelados en profundidad en el
  prototipo; verificar spec de D1/D2.
- Evento de envio a notificacion (`ACTA_ENVIADA_A_NOTIFICACION`): verificar si
  es explicito en spec o derivado.

### 7.5 Tablas / campos a buscar en `backend/persistencia` y `spec/backend`

- Schema de tabla de actas: verificar equivalencia con campos de `ActaMock`.
- Schema de tabla de eventos: verificar estructura de `ActaEventoMock`.
- Schema de tabla de documentos: verificar tipos y estados.
- Schema de tabla de notificaciones: verificar si existe o hay que disenarla.
- Schema de tabla de pagos: verificar separacion voluntario/condena.
- Schema de tabla de bloqueantes: verificar si existe.
- Schema de catalogo de bandejas: verificar si hay tabla de referencia.

### 7.6 Decisiones pendientes

- Modelo de `accionPendiente`: estado persistido vs. calculado desde eventos.
- Modelo de `bandejaActual`: calculada vs. cache con invalidacion por evento.
- Criterio de invalidacion de cache de bandeja (mencionado en
  `PLAN_INTEGRACION_DOMINIO_REAL.md` sec. 2).
- Catalogo de tipos de acta (transito, bromatologia, general, contravencional):
  pendiente de definicion como entidad real.
- Modelo de comprobantes de pago (EM, RC, Cmte/Pref/Nro): pendiente de diseno.
- Mecanismo de lookup de `codigoQr` a `actaId`: pendiente de diseno.
- Servicio de firma documental real: pendiente de diseno.
- Modelo de domicilio electronico verificado en notificaciones: pendiente.
- Integracion postal real: pendiente de diseno de contrato externo.
- Separacion formal entre bloqueantes documentales y materiales en schema real:
  pendiente de decision.

---

*Documento generado: Jun 2026. Sin cambios de codigo. Sin modificacion de specs existentes.*

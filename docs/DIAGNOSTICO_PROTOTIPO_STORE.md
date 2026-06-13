# Diagnóstico técnico: PrototipoStore y supports

**Fecha:** 2026-06-12
**Scope:** `backend/api-faltas-prototipo` — paquete `store/`
**Objetivo:** inventariar el estado real del store y sus supports para planificar
futuras descomposiciones seguras, sin cambiar código funcional.

---

## 1. Resumen ejecutivo

`PrototipoStore` es un monolito en memoria de **4 043 líneas**.
Contiene tres responsabilidades estructuralmente mezcladas:

1. **Declaraciones de tipos** (enums + records públicos, ~1 300 líneas)
2. **Estado compartido** (22 mapas + 1 contador atómico, ~80 líneas)
3. **Lógica de dominio** (métodos públicos y privados, ~2 600 líneas)

La situación es manejable: ya existe un ecosistema de **12 supports** que
encapsulan las áreas más voluminosas. El riesgo principal no es el tamaño total
sino la densidad de declaraciones de tipos embebidas y el estado compartido mutable.

---

## 2. Estado actual del paquete store/

### 2.1 Líneas por archivo

| Archivo | Líneas | Rol |
|---|---|---|
| MockDataFactory.java | 4 729 | Precarga de datos mock |
| PrototipoStore.java | 4 043 | Store principal + tipos públicos |
| CerrabilidadSupport.java | 1 426 | Condiciones de cierre y materiales |
| CorreoPostalNotificacionSupport.java | 1 119 | Correo postal externo |
| FalloPlazoApelacionSupport.java | 1 017 | Fallo + apelación + plazo |
| GestionExternaSupport.java | 713 | Apremio + juzgado de paz |
| PiezasFirmaSupport.java | 697 | Documentos + firma |
| NotificacionSupport.java | 548 | Notificación interna |
| ArchivoReingresoSupport.java | 528 | Archivo + reingreso |
| PagoVoluntarioSupport.java | 485 | Pago voluntario |
| PagoInformadoSupport.java | 331 | Pago informado |
| PrototipoConstantes.java | 299 | Constantes package-private |
| NotificadorMunicipalSupport.java | 257 | Acuses del notificador municipal |
| PagoCondenaSupport.java | 171 | Pago de condena |
| CierreSupport.java | 138 | Cierre efectivo del expediente |
| **Total paquete store/** | **~17 500** | |

### 2.2 Dependencias externas de PrototipoStore

domain.ActaMock, ActaEventoMock, ActaDocumentoMock, ActaNotificacionMock,
ActaPiezasRequeridasMock, ActaTransitoMock, ActaBromatologiaMock,
CanalNotificacion, EstadoNotificacion, ResultadoNotificacion, TipoNotificacion
bandeja.SubBandejaAsignacion/Clasificador/Codigo/Contexto
web.dto.CrearActaMockDemoRequest
java.util.*, BigDecimal, java.time.*

### 2.3 Estado interno (22 mapas + 1 contador)

| Campo | Tipo | Supports que lo usan |
|---|---|---|
| actas | Map<String,ActaMock> | todos |
| eventosPorActa | Map<String,List<ActaEventoMock>> | todos |
| documentosPorActa | Map<String,List<ActaDocumentoMock>> | piezasFirma, cerrabilidad, archivoReingreso |
| notificacionesPorActa | Map<String,List<ActaNotificacionMock>> | notificacion, correoPostal, notificadorMunicipal |
| piezasRequeridasPorActa | Map<String,ActaPiezasRequeridasMock> | piezasFirma |
| accionPendientePorActa | Map<String,String> | archivoReingreso, notificacion, gestionExterna, pagoVoluntario, pagoInformado, cierre |
| observacionParalizacionPorActa | Map<String,String> | store directo |
| situacionPagoPorActa | Map<String,SituacionPagoMock> | pagoVoluntario, pagoInformado, pagoCondena, gestionExterna |
| pagoInformadoPorActa | Map<String,PagoInformadoMock> | pagoInformado |
| montoPagoVoluntarioPorActa | Map<String,BigDecimal> | pagoVoluntario, pagoInformado |
| montoCondenaPorActa | Map<String,BigDecimal> | pagoCondena, gestionExterna |
| resultadoExternoPostGestionPorActa | Map<String,ResultadoExternoPostGestion> | gestionExterna |
| montoCondenaSugeridoPostGestionExternaPorActa | Map<String,BigDecimal> | gestionExterna |
| situacionPagoCondenaPorActa | Map<String,SituacionPagoCondena> | cerrabilidad, pagoCondena, gestionExterna |
| tipoPagoPorActa | Map<String,TipoPago> | pagoVoluntario, pagoCondena, gestionExterna |
| actaTransitoMockPorActa | Map<String,ActaTransitoMock> | store + MockDataFactory |
| actaBromatologiaMockPorActa | Map<String,ActaBromatologiaMock> | store + MockDataFactory |
| dependenciaDemoPorActa | Map<String,String> | store + consulta |
| tipoActaDemoPorActa | Map<String,String> | store + consulta |
| patenteVehiculoPorActa | Map<String,String> | store + consulta |
| contadorActaLabradoMockDemo | AtomicInteger | store (creación mock demo) |

### 2.4 Constantes public static final String en PrototipoStore (contrato Angular)

ACCION_REINTENTAR_NOTIFICACION, ACCION_EVALUAR_NOTIFICACION_VENCIDA,
ACCION_REVISION_POST_REINGRESO, ACCION_DERIVAR_GESTION_EXTERNA,
ACCION_REVISION_POST_GESTION_EXTERNA, ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA,
ACCION_REVISION_POST_REACTIVACION, ACCION_PARALIZACION_ESPERA_DOCUMENTAL,
ACCION_PARALIZACION_ESPERA_INFORME_EXTERNO, ACCION_PARALIZACION_ESPERA_OTRA_DEPENDENCIA,
ACCION_PARALIZACION_ESPERA_RESOLUCION_RELACIONADA, ACCION_PARALIZACION_OTRO,
ACCION_EVALUAR_PAGO_VOLUNTARIO, ACCION_VERIFICAR_PAGO_INFORMADO,
MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO, MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO,
MOTIVO_ARCHIVO_NULIDAD, TIPO_GESTION_EXTERNA_APREMIO, TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
ACCION_COMPLETAR_ENRIQUECIMIENTO

No deben renombrarse: son parte del contrato JSON con Angular.

---

## 3. Inventario de supports existentes

### ArchivoReingresoSupport (528 líneas)
Responsabilidad: archivo directo desde análisis, archivo post evaluación de
vencimiento, reingreso desde archivo, cálculo de motivoArchivo y accionPendiente.
Métodos: reingresarDesdeArchivoNulidad, inferirAccionPendientePostReingreso,
tieneDocumentoConTipoYEstado, ultimoEventoNotificacionRelevante, registrarEvento.
Cubre: archivarActaDesdeAnalisis, archivarPorVencimiento, reingresarActaDesdeArchivo.
Usado por: PrototipoArchivoParalizacionController.

### NotificacionSupport (548 líneas)
Responsabilidad: notificación positiva/negativa/vencida, reintentos, movimientos de bandeja.
Métodos: moverAAnalisis, moverAPendienteNotificacion, actualizarOCrearNotificacionActa,
tipoNotificacionParaDocumento, registrarEvento.
Cubre: registrarNotificacionPositiva/Negativa/Vencida, reintentarNotificacion,
reintentarNotificacionVencida.
Usado por: PrototipoNotificacionesInternasController, PiezasFirmaSupport (dependencia cruzada).

### CerrabilidadSupport (1 426 líneas — el más grande)
Responsabilidad: resultado final, pendientes bloqueantes, cerrabilidad, hechos materiales,
resolutorio documental, cumplimiento material, reconocimiento de origen bloqueante.
Métodos principales: getCerrabilidadActa, getHechosMaterialesActa, marcarResultadoAbsuelto,
resolverPendienteBloqueanteCierre, registrarResolucionBloqueoCierreDocumental (x2),
registrarCumplimientoMaterialBloqueoCierre, reconocerOrigenBloqueante* (x3),
setResultadoFinalDemo, setOrigenesBloqueantesCierreMaterialDemo.
Cubre: todo el subsistema de cierre/materiales/bloqueantes.
Usado por: PrototipoFalloApelacionController, PrototipoMaterialesController,
PagoCondenaSupport, GestionExternaSupport, CierreSupport, PiezasFirmaSupport.
Es el support con más dependientes del sistema.

### PiezasFirmaSupport (697 líneas)
Responsabilidad: producción de piezas documentales, firma individual, transiciones de bandeja.
Métodos: producirPieza, moverTrasProducirPieza, moverACerradaPorNulidadFirmada,
moverAPendienteNotificacion (delega a NotificacionSupport),
reutilizarMedidaPreventivaConAnclaTempranaSiCorresponde, agregarDocumentoPendienteFirma.
Cubre: generarMedidaPreventiva, generarNotificacionActa, generarNulidad, generarResolucion,
generarRectificacion, firmarDocumento.
Usado por: PrototipoDocumentosFirmaController.
Dependencia cruzada: llama a NotificacionSupport al cerrar firma.

### FalloPlazoApelacionSupport (1 017 líneas)
Responsabilidad: fallo absolutorio/condenatorio, plazo de apelación, registro de apelación,
resolución de apelación, consentimiento de condena, notificación de fallo.
Métodos: dictarFallo, registrarVencimientoPlazoApelacion, registrarApelacion,
resolverApelacion, consentirCondena, consentirCondenaYRegistrarPago,
agregarOActualizarNotificacionEntregadaDeFallo.
Cubre: dictarFalloAbsolutorio/Condenatorio, registrarVencimientoPlazoApelacion,
registrarApelacion, resolverApelacion, consentirCondena, consentirCondenaYRegistrarPago,
hayFalloPendienteDeNotificacion, hayFalloCondenatorioPendienteDeNotificacion,
hayFalloCondenatorioDictado, hayFalloDictado, puedePresentarApelacion, hayApelacionPresentada.
Usado por: PrototipoFalloApelacionController, PrototipoPortalInfractorController.

### GestionExternaSupport (713 líneas)
Responsabilidad: derivación a apremio/juzgado, verificación condena firme,
reingreso con resultado externo, gestión de tipoGestionExterna.
Métodos: derivar, condenaFirmeDerivable, registrarResultadoExternoModificaMonto,
registrarResultadoExternoProponeAbsolver, reingresarParaNuevoFallo.
Cubre: derivarAApremio, derivarAJuzgadoDePaz, reingresarActaDesdeGestionExterna,
reingresarDesdeApremioSinPago, registrarPagoEnApremio, reingresarDesdeJuzgado*,
reingresarDesdeApremio*.
Usado por: PrototipoGestionExternaController.

### PagoVoluntarioSupport (485 líneas)
Responsabilidad: solicitud pago voluntario (análisis y portal), fijación de monto,
vencimiento, confirmación externa.
Métodos: registrarSolicitudPagoVoluntario, solicitarPagoVoluntarioDesdePortal,
fijarMontoPagoVoluntario, registrarVencimientoPagoVoluntario,
confirmarPagoVoluntarioExterno, informarPagoVoluntarioDesdePortal, registrarEvento.
Cubre: 6 métodos de pago voluntario.
Usado por: PrototipoPagosController, PrototipoPortalInfractorController.

### PagoInformadoSupport (331 líneas)
Responsabilidad: circuito de pago informado (registro, comprobante, confirmar, observar).
Métodos: registrarPagoInformado, adjuntarComprobantePagoInformado, confirmarPagoInformado,
observarPagoInformado, setSituacionPago, getSituacionPago, limpiarAccionVerificarPagoInformado.
Cubre: 4 métodos de pago informado.
Usado por: PrototipoPagosController.

### PagoCondenaSupport (171 líneas)
Responsabilidad: pago de condena firme (informar, confirmar, observar) desde dirección y portal.
Métodos: informarPagoCondena, informarPagoCondenaDesdePortal, confirmarPagoCondena,
observarPagoCondena, validarPrecondicionComun.
Cubre: 4 métodos de pago de condena.
Usado por: PrototipoPagosController, PrototipoPortalInfractorController.

### CierreSupport (138 líneas — el más pequeño)
Responsabilidad: cierre efectivo del expediente desde análisis.
Métodos: cerrarActaDesdeAnalisis, registrarEvento.
Cubre: cerrarActaDesdeAnalisis.
Usado por: PrototipoArchivoParalizacionController.

### CorreoPostalNotificacionSupport (1 119 líneas)
Responsabilidad: correo postal mock completo: lote, CSV, anulación, envío individual, trazabilidad.
Métodos: generarLoteCorreoPostalDemo, procesarRespuestaCorreoPostalDemo,
anularLoteCorreoPostalDemo, enviarIndividualCorreoPostalDemo,
listarNotificacionesCorreoListasParaLote, listarLotesCorreoGenerados,
buscarTrazabilidadCorreoPorActa.
Cubre: todo el subsistema de correo postal.
Usado por: PrototipoNotificacionesExternasController.

### NotificadorMunicipalSupport (257 líneas)
Responsabilidad: acuses del notificador municipal (positivo/no positivo), listado.
Métodos: listarNotificacionesNotificadorMunicipal, registrarAcuseNotificadorMunicipal.
Cubre: 2 métodos del subsistema de notificador municipal.
Usado por: PrototipoNotificacionesExternasController.

### PrototipoConstantes (299 líneas)
Responsabilidad: constantes package-private: bandejas, bloques, estados de documentos,
tipos de documentos. No forma parte del contrato público.

---

## 4. Mapa de responsabilidades agrupadas

| Grupo funcional | Dónde vive hoy |
|---|---|
| Admin / reset / mock demo | PrototipoStore directo |
| Consultas / bandejas / detalle | PrototipoStore directo |
| Portal infractor | PrototipoStore directo (helpers privados) |
| Notificaciones internas | NotificacionSupport |
| Notificaciones externas / correo postal | CorreoPostalNotificacionSupport |
| Notificador municipal | NotificadorMunicipalSupport |
| Pagos voluntarios | PagoVoluntarioSupport |
| Pago informado | PagoInformadoSupport |
| Pago de condena | PagoCondenaSupport |
| Fallo / apelación / consentimiento | FalloPlazoApelacionSupport |
| Gestión externa (apremio / juzgado) | GestionExternaSupport |
| Cierre efectivo | CierreSupport |
| Archivo / reingreso | ArchivoReingresoSupport |
| Paralización / reactivación | PrototipoStore directo — PENDIENTE DE EXTRACCIÓN |
| Materiales / bloqueantes / cerrabilidad | CerrabilidadSupport |
| Documentos / firma | PiezasFirmaSupport |
| Eventos / auditoría | registrarEvento(...) privado en cada support |
| Constantes compartidas | PrototipoConstantes |

---

## 5. Residuo de lógica en PrototipoStore directo (no delegado)

| Bloque | Líneas estimadas | Complejidad |
|---|---|---|
| Declaraciones de tipos (enums + records) | ~1 300 | Baja |
| Estado + construcción de supports | ~150 | Baja |
| Portal infractor (8 métodos privados) | ~200 | Media |
| Paralización / reactivación | ~120 | Baja |
| Bandeja / sub-bandeja (5 sobrecargas + helpers) | ~250 | Media |
| Consultas básicas | ~80 | Baja |
| Creación mock demo (crearActaMockDemo + 4 helpers) | ~300 | Media-alta |
| Setters/getters operativos (~30 métodos) | ~250 | Baja |
| Total residuo directo | ~2 650 | |

---

## 6. Candidatos de extracción segura

### Candidato A — ParalizacionReactivacionSupport (RIESGO: BAJO)

Métodos a mover:
- paralizarActa (pública)
- reactivarActa (pública)
- puedeParalizarActa (pública e privada)
- accionPendienteParalizacion (privada)
- registrarEventoParalizacion (privada)

Por qué es seguro: subsistema autónomo, sin dependencias a otros supports.
Solo usa: actas, eventosPorActa, accionPendientePorActa, observacionParalizacionPorActa.
Reducción de store: ~120 líneas.
Tests: ParalizarActaIT, ReactivacionActa0114IT.

### Candidato B — BandejaConsultaSupport (RIESGO: BAJO)

Métodos a mover:
- listarBandejasConConteoOrdenadas
- listarBandejasConResumenOperativo
- construirContextoSubBandeja
- clasificarSubBandeja
- esSubBandejaValidaParaBandeja
- construirSubBandejasVisibles (privada)
- listarActasPorBandeja (x5 sobrecargas)

Por qué es seguro: consultas puras de lectura, sin mutación de estado.
Solo usan: actas, accionPendientePorActa, mapas de motivo/tipoGestion (lectura), subBandejaClasificador.
Reducción de store: ~250 líneas.
Tests: BandejaListadoConcurrenteIT, MatrizSanidadActasDemoIT, SubBandejaDinamicaIT.

### Candidato C — PortalInfractorSupport (RIESGO: BAJO-MEDIO)

Métodos a mover:
- actaEnRevisionParaPortal
- listarDocumentosVisiblesPortal
- verDocumentoPortal
- confirmarVisualizacionNotificacionPortal
- 8 helpers privados del portal

Por qué es BAJO-MEDIO: confirmarVisualizacionNotificacionPortal genera una
notificación positiva interna, requiere pasar referencia a NotificacionSupport.
Reducción de store: ~200 líneas.
Tests: AccesoInfractorPorCodigoQrIT, PortalActaEnRevisionIT,
PortalFalloFirmadoPendienteNotificacionIT, PortalDocumentoYaNotificadoIdempotenteIT.

### Candidato D — AdminDemoSupport (RIESGO: MEDIO)

Métodos a mover:
- clearAll
- crearActaMockDemo
- agregarEventoCrear (privada)
- agregarAnclasMaterialesDesdeTransito (privada)
- agregarOAncla (privada)
- anclaMedida (privada)
- anclaSoloMedida (privada)
- validarBanderasConDependencia (privada)
- validarTipoConDependencia (privada)

Por qué es MEDIO: crearActaMockDemo toca ~12 mapas y llama a setters demo de
cerrabilidad. Requiere pasar muchos mapas o referencia al store.
Reducción de store: ~300 líneas.
Tests: AltaActaMockDemoIT, MatrizSanidadActasDemoIT, DemoRecorridoPuntaAPuntaActa0021IT.

### Candidato E — Extracción de tipos a archivos separados (RIESGO: ALTO)

Qué mover: ~120 enums y records públicos declarados dentro de PrototipoStore (~1 300 líneas).
Por qué es ALTO: todos los controllers referencian estos tipos como
PrototipoStore.XxxResultado / PrototipoStore.XxxEstado. Requiere actualizar
imports en 11 controllers y en todos los supports.
Recomendación: aplazar hasta después de A, B, C y D.
Con este slice: store queda ~1 800 líneas (solo estado + supports + delegaciones).

---

## 7. Riesgos detectados

### 7.1 Estado compartido mutable sin barreras

Todos los supports reciben los mismos mapas por referencia. Cualquier support
puede modificar el estado de otro sin restricción. Es intencional para el
prototipo en memoria.

Métodos de alto acoplamiento multi-mapa:

| Método | Mapas tocados |
|---|---|
| crearActaMockDemo | ~12 mapas directamente |
| confirmarPagoInformado | situacionPagoPorActa, pagoInformadoPorActa, tipoPagoPorActa + puede desencadenar cierre |
| resolverApelacion | múltiples mapas via FalloPlazoApelacionSupport + CerrabilidadSupport |
| reingresarDesdeJuzgadoMontoModificado | montoCondena, resultadoExterno, eventos, bandeja |

### 7.2 Métodos con side effects simultáneos múltiples

| Método | Side effects |
|---|---|
| confirmarVisualizacionNotificacionPortal | notificación positiva + evento + mueve bandeja |
| firmarDocumento (nulidad) | mueve a CERRADAS + evento + puede activar notificación |
| dictarFalloCondenatorio | genera documento FALLO + mueve a PENDIENTE_FIRMA + puede notificar fallo |
| crearActaMockDemo | crea acta + N documentos + N eventos + anclas materiales + pago demo |

### 7.3 Dependencia de orden de inicialización de supports

CerrabilidadSupport DEBE declararse ANTES que PiezasFirmaSupport (ver comentario
en store, línea ~1 517). El orden de los campos de instancia importa: PiezasFirmaSupport
recibe la referencia ya construida de cerrabilidad. Reordenar rompe en runtime.

### 7.4 Strings de dominio que no deben renombrarse

- Nombres de bandejas en PrototipoConstantes
- Tipos de documento en PrototipoConstantes
- Constantes ACCION_* y MOTIVO_* en PrototipoStore (contrato JSON con Angular)

### 7.5 Duplicación técnica dentro del paquete

| Duplicado | Aparece en |
|---|---|
| registrarEvento(...) privado | los 12 supports (deliberado; cada uno con firma propia) |
| sufijoActa(String) | NotificacionSupport, PiezasFirmaSupport, FalloPlazoApelacionSupport |
| primerNoVacio(String, String) | CorreoPostalNotificacionSupport, NotificadorMunicipalSupport |

sufijoActa y primerNoVacio son candidatos a consolidar en helper package-private sin riesgo.

---

## 8. Orden recomendado de próximos slices

| # | Slice | Reducción de store | Riesgo |
|---|---|---|---|
| 1 | Extraer ParalizacionReactivacionSupport | ~120 líneas | BAJO |
| 2 | Extraer BandejaConsultaSupport | ~250 líneas | BAJO |
| 3 | Extraer PortalInfractorSupport | ~200 líneas | BAJO-MEDIO |
| 4 | Consolidar sufijoActa y primerNoVacio en helper | limpieza técnica | BAJO |
| 5 | Extraer AdminDemoSupport | ~300 líneas | MEDIO |
| 6 | Extraer tipos públicos a archivos separados | ~1 300 líneas | ALTO |

Con slices 1-3: store en ~3 400 líneas.
Con slices 1-5: store en ~3 100 líneas.
Con slice 6: store en ~1 800 líneas.

---

## 9. Validaciones antes de cada extracción

1. mvn clean test — todos los ITs en verde antes de tocar nada.
2. Verificar que el método a mover no aparece referenciado por literal string desde Angular.
3. git diff --check — detectar caracteres no-ASCII o mojibake post-cambio.
4. Mantener misma firma pública en el store (mismo nombre, mismos params, mismo retorno).
5. Correr IT específico del área antes del commit.

---

## 10. Controllers y métodos de store que invocan

| Controller | Líneas | Métodos de store |
|---|---|---|
| PrototipoAdminController | 76 | clearAll, getActas, crearActaMockDemo |
| PrototipoConsultaActasController | 225 | listarBandejasConResumenOperativo, listarActasPorBandeja, findActa, existeActa, listarEventos*, listarDocumentos*, listarNotificaciones*, clasificarSubBandeja, getSituacionPago, getAccionPendiente, getMotivoArchivo, getTipoGestionExterna, getCerrabilidadActa, getDependenciaDemo |
| PrototipoArchivoParalizacionController | 241 | cerrarActaDesdeAnalisis, archivarActaDesdeAnalisis, archivarPorVencimiento, reingresarActaDesdeArchivo, reactivarActa, paralizarActa, enviarActaANotificacion, anularActaPorNulidad |
| PrototipoDocumentosFirmaController | 158 | generarMedidaPreventiva, generarNotificacionActa, generarNulidad, generarResolucion, generarRectificacion, firmarDocumento |
| PrototipoFalloApelacionController | 255 | marcarResultadoAbsuelto, getCerrabilidadActa, dictarFalloAbsolutorio, dictarFalloCondenatorio, registrarVencimientoPlazoApelacion, registrarApelacion, resolverApelacion |
| PrototipoGestionExternaController | 297 | derivarAApremio, derivarAJuzgadoDePaz, reingresarActaDesdeGestionExterna, reingresarDesdeApremioSinPago, registrarPagoEnApremio, reingresarDesde* (x4) |
| PrototipoMaterialesController | 307 | registrarConstatacionMaterialTemprana, registrarMedidaPreventivaPosterior, reconocerOrigenBloqueante* (x3), registrarResolucionBloqueoCierreDocumental, registrarCumplimientoMaterialBloqueoCierre |
| PrototipoNotificacionesExternasController | 199 | listarNotificacionesNotificadorMunicipal, registrarAcuseNotificadorMunicipal, listarNotificacionesCorreoListasParaLote, listarLotesCorreoGenerados, anularLoteCorreoPostalDemo, enviarIndividualCorreoPostalDemo, generarLoteCorreoPostalDemo, buscarTrazabilidadCorreoPorActa, procesarRespuestaCorreoPostalDemo |
| PrototipoNotificacionesInternasController | 120 | registrarNotificacionPositiva, registrarNotificacionNegativa, registrarNotificacionVencida, reintentarNotificacion, reintentarNotificacionVencida |
| PrototipoPagosController | 362 | registrarSolicitudPagoVoluntario, fijarMontoPagoVoluntario, getAccionPendiente, registrarVencimientoPagoVoluntario, registrarPagoInformado, adjuntarComprobantePagoInformado, confirmarPagoInformado, confirmarPagoVoluntarioExterno, observarPagoInformado, informarPagoCondena, confirmarPagoCondena, observarPagoCondena, consentirCondenaYRegistrarPago |
| PrototipoPortalInfractorController | 248 | findActaPorCodigoQr, registrarApelacion, confirmarVisualizacionNotificacionPortal, solicitarPagoVoluntarioDesdePortal, informarPagoVoluntarioDesdePortal, verDocumentoPortal, informarPagoCondenaDesdePortal, consentirCondena |

---

Documento de diagnostico estatico — ningun comportamiento fue alterado.
Ningun metodo fue movido. Ningun DTO ni ruta fue modificada.
# Arquitectura de controllers del prototipo de Faltas

## Objetivo

El antiguo `PrototipoApiController.java` fue descompuesto en 11 controllers especializados por
responsabilidad funcional. Cada controller cubre un area de dominio claramente delimitada.
Las rutas publicas se mantuvieron identicas: todas bajo `/api/prototipo/...`.
El contrato API con Angular no cambio.

---

## Estado final

- `PrototipoApiController.java`: **eliminado** (borrado del filesystem; git lo reporta como `D` deleted).
- Todos los endpoints que existian en el monolito fueron migrados a los controllers especializados.
- Ningun controller especializado quedo vacio.

---

## Controllers

| Controller | Responsabilidad | Endpoints | Dependencias principales |
|---|---|---|---|
| `PrototipoAdminController` | Salud, reset de demo y alta mock de actas | 3 | `PrototipoStore`, `MockDataFactory`, `ActaDetalleMapper` |
| `PrototipoConsultaActasController` | Bandejas, detalle de acta, busqueda, eventos, documentos, notificaciones | 7 | `PrototipoStore`, `ActaBusquedaHelper`, `ActaDetalleMapper` |
| `PrototipoPortalInfractorController` | Acceso ciudadano por codigoQr: consulta, apelacion, pago voluntario, consentimiento de condena | 8 | `PrototipoStore`, `ActaInfractorMapper` |
| `PrototipoNotificacionesExternasController` | Notificador municipal y correo postal (lotes, trazabilidad, respuestas) | 9 | `PrototipoStore` |
| `PrototipoNotificacionesInternasController` | Resultado de notificaciones internas (positiva, negativa, vencida, reintento) | 5 | `PrototipoStore` |
| `PrototipoPagosController` | Ciclo completo de pagos: voluntario, informado, condena, consentimiento y confirmacion externa | 12 | `PrototipoStore` |
| `PrototipoFalloApelacionController` | Resultado final, fallos absolutorio/condenatorio, apelacion y resolucion | 6 | `PrototipoStore` |
| `PrototipoArchivoParalizacionController` | Cierre, archivo, reingreso, paralizacion, reactivacion, envio a notificacion, anulacion | 8 | `PrototipoStore` |
| `PrototipoMaterialesController` | Bloqueantes materiales de cierre: constatacion, reconocimiento, resolucion y cumplimiento | 6 | `PrototipoStore` |
| `PrototipoGestionExternaController` | Derivacion y retorno desde Apremio y Juzgado de Paz | 10 | `PrototipoStore` |
| `PrototipoDocumentosFirmaController` | Generacion de documentos (medida preventiva, notificacion, nulidad, resolucion, rectificacion) y firma | 6 | `PrototipoStore` |

**Total: 80 endpoints distribuidos en 11 controllers.**

---

## Inventario de endpoints

### PrototipoAdminController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| GET | `/api/prototipo/health` | `health()` | - | `PrototipoHealthResponse` | Estado de salud del servicio y conteo de actas en memoria |
| POST | `/api/prototipo/reset` | `reset()` | - | `PrototipoResetResponse` | Reinicializa el dataset mock al estado inicial |
| POST | `/api/prototipo/actas/mock` | `crearActaMockDemo(request)` | `CrearActaMockDemoRequest` body | `ActaDetalleResponse` | Alta minima de acta mock de demo (numeracion ACTA-DEMO-nnnn) |

---

### PrototipoConsultaActasController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| GET | `/api/prototipo/bandejas` | `listarBandejas()` | - | `List<BandejaResponse>` | Lista todas las bandejas con resumen operativo y subbandejas |
| GET | `/api/prototipo/bandejas/{codigo}/actas` | `listarActasPorBandeja(...)` | Query params: `accionPendiente`, `situacionPago`, `resultadoFinal`, `cerrable`, `pendienteBloqueante`, `subBandeja` | `List<ActaBandejaItemResponse>` | Lista actas de una bandeja con filtros opcionales |
| GET | `/api/prototipo/actas/{id}` | `detalleActa(id)` | - | `ActaDetalleResponse` | Detalle completo de un acta por su ID interno |
| GET | `/api/prototipo/actas/buscar` | `buscarActas(q)` | Query param: `q` | `List<PrototipoActaBusquedaResponse>` | Busqueda global por numero, actaId o fragmento; limite MAX_RESULTADOS |
| GET | `/api/prototipo/actas/{id}/eventos` | `listarEventosActa(id)` | - | `List<ActaEventoResponse>` | Log de eventos append-only del acta |
| GET | `/api/prototipo/actas/{id}/documentos` | `listarDocumentosActa(id)` | - | `List<ActaDocumentoResponse>` | Documentos asociados al acta |
| GET | `/api/prototipo/actas/{id}/notificaciones` | `listarNotificacionesActa(id)` | - | `List<ActaNotificacionResponse>` | Notificaciones del acta |

Nota: `GET /api/prototipo/actas/buscar` y `GET /api/prototipo/actas/{id}` no colisionan. Spring MVC prioriza segmentos literales sobre path variables.

---

### PrototipoPortalInfractorController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| GET | `/api/prototipo/infractor/actas/{codigoQr}` | `obtenerActaInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | Vista ciudadana minima del acta por codigo QR opaco |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/registrar-apelacion` | `registrarApelacionInfractorPorCodigoQr(codigoQr, request?)` | `RegistrarApelacionAccionRequest` body opcional | `ActaInfractorResponse` | Apelacion desde el portal; canal fijado como PORTAL_INFRACTOR |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/confirmar-visualizacion-notificacion` | `confirmarVisualizacionNotificacionInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | El infractor confirma apertura de la notificacion desde el portal |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/solicitar-pago-voluntario` | `solicitarPagoVoluntarioInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | Solicitud de pago voluntario iniciada por el infractor |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/pagar-voluntario` | `pagarVoluntarioInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | El infractor informa el pago voluntario desde el portal |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/documentos/{tipoDocumento}/ver` | `verDocumentoInfractorPorCodigoQr(codigoQr, tipoDocumento)` | - | `ActaInfractorResponse` | El infractor abre un documento notificable; confirma visualizacion |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/pagar-condena` | `pagarCondenaInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | El infractor informa el pago de condena desde el portal |
| POST | `/api/prototipo/infractor/actas/{codigoQr}/acciones/consentir-condena` | `consentirCondenaInfractorPorCodigoQr(codigoQr)` | - | `ActaInfractorResponse` | El infractor consiente la condena; resultadoFinal pasa a CONDENA_FIRME |

---

### PrototipoNotificacionesExternasController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| GET | `/api/prototipo/notificaciones/notificador-municipal` | `listarNotificacionesNotificadorMunicipal()` | - | `List<NotificadorMunicipalNotificacionResponse>` | Lista notificaciones pendientes para el notificador municipal |
| POST | `/api/prototipo/notificaciones/notificador-municipal/{notificacionId}/acuse` | `registrarAcuseNotificadorMunicipal(notificacionId, request?)` | `NotificadorMunicipalAcuseRequest` body | `NotificadorMunicipalAcuseResponse` | Registra acuse (ENTREGADA/NO_ENTREGADA) del notificador municipal |
| GET | `/api/prototipo/notificaciones/correo/listas-para-lote` | `listarNotificacionesCorreoListasParaLote()` | - | `List<CorreoPostalNotificacionListaItem>` | Notificaciones listas para incluir en un lote de correo |
| GET | `/api/prototipo/notificaciones/correo/lotes` | `listarLotesCorreoGenerados()` | - | `List<CorreoLoteResumen>` | Lista lotes de correo postal generados |
| POST | `/api/prototipo/notificaciones/correo/lotes/{loteId}/anular` | `anularLoteCorreoPostalDemo(loteId)` | - | `AnularLoteCorreoResultado` | Anula un lote de correo postal generado |
| POST | `/api/prototipo/notificaciones/correo/{notificacionId}/enviar-individual` | `enviarIndividualCorreoPostalDemo(notificacionId)` | - | `EnviarIndividualCorreoResultado` | Envia una notificacion de correo postal de forma individual |
| POST | `/api/prototipo/notificaciones/correo/lotes/generar` | `generarLoteCorreoPostalDemo(tipo?, body?)` | Query param `tipo`; `GenerarLoteCorreoPostalRequest` body opcional | `GenerarLoteCorreoResultado` | Genera un lote CSV de correo postal demo |
| GET | `/api/prototipo/notificaciones/correo/trazabilidad` | `buscarTrazabilidadCorreoPostal(acta)` | Query param: `acta` requerido | `List<CorreoPostalTrazabilidadItem>` | Trazabilidad de correo postal por acta |
| POST | `/api/prototipo/notificaciones/correo/respuestas/procesar-demo` | `procesarRespuestaCorreoPostalDemo(loteId?)` | Query param: `loteId` opcional | `ProcesarRespuestaCorreoResultado` | Procesa CSV de respuesta de correo postal demo |

---

### PrototipoNotificacionesInternasController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/registrar-notificacion-positiva` | `registrarNotificacionPositiva(id)` | - | `RegistrarNotificacionPositivaAccionResponse` | Resultado positivo de notificacion; acta pasa a analisis |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-notificacion-negativa` | `registrarNotificacionNegativa(id)` | - | `RegistrarNotificacionNegativaAccionResponse` | Resultado negativo; acta retorna a analisis con accion pendiente |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-notificacion-vencida` | `registrarNotificacionVencida(id)` | - | `RegistrarNotificacionVencidaAccionResponse` | Notificacion vencida; acta retorna a analisis con accion pendiente |
| POST | `/api/prototipo/actas/{id}/acciones/reintentar-notificacion` | `reintentarNotificacion(id)` | - | `ReintentarNotificacionAccionResponse` | Reintenta la notificacion; acta vuelve a PENDIENTE_NOTIFICACION |
| POST | `/api/prototipo/actas/{id}/acciones/reintentar-notificacion-vencida` | `reintentarNotificacionVencida(id)` | - | `ReintentarNotificacionVencidaAccionResponse` | Decision posterior al vencimiento: reintenta notificacion desde PENDIENTE_ANALISIS |

---

### PrototipoPagosController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/registrar-solicitud-pago-voluntario` | `registrarSolicitudPagoVoluntario(id, request?)` | `RegistrarSolicitudPagoVoluntarioAccionRequest` body con `monto` | `RegistrarSolicitudPagoVoluntarioAccionResponse` | Direccion fija monto y habilita pago voluntario |
| POST | `/api/prototipo/actas/{id}/acciones/fijar-monto-pago-voluntario` | `fijarMontoPagoVoluntario(id, request?)` | `RegistrarSolicitudPagoVoluntarioAccionRequest` body con `monto` | `RegistrarSolicitudPagoVoluntarioAccionResponse` | Direccion fija monto cuando el infractor ya solicito pago desde el portal |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-vencimiento-pago-voluntario` | `registrarVencimientoPagoVoluntario(id)` | - | `RegistrarVencimientoPagoVoluntarioAccionResponse` | Registra vencimiento de pago voluntario sin pago efectivo |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-pago-informado` | `registrarPagoInformado(id)` | - | `RegistrarPagoInformadoAccionResponse` | Pago voluntario informado; queda pendiente de confirmacion |
| POST | `/api/prototipo/actas/{id}/acciones/adjuntar-comprobante-pago-informado` | `adjuntarComprobantePagoInformado(id, nombreArchivo?)` | Query param: `nombreArchivo` opcional | `AdjuntarComprobantePagoInformadoAccionResponse` | Adjunta comprobante al pago informado |
| POST | `/api/prototipo/actas/{id}/acciones/confirmar-pago-informado` | `confirmarPagoInformado(id)` | - | `ConfirmarPagoInformadoAccionResponse` | Confirmacion interna del pago voluntario informado |
| POST | `/api/prototipo/actas/{id}/acciones/confirmar-pago-voluntario-externo` | `confirmarPagoVoluntarioExterno(id, request?)` | `ConfirmarPagoVoluntarioExternoAccionRequest` body con `monto` y `origen` | `ConfirmarPagoVoluntarioExternoAccionResponse` | Confirmacion de pago voluntario desde sistema externo de cobro |
| POST | `/api/prototipo/actas/{id}/acciones/observar-pago-informado` | `observarPagoInformado(id)` | - | `ObservarPagoInformadoAccionResponse` | Observa/no confirma el pago voluntario informado |
| POST | `/api/prototipo/actas/{id}/acciones/informar-pago-condena` | `informarPagoCondena(id)` | - | `PagoCondenaAccionResponse` | Informa el pago de condena |
| POST | `/api/prototipo/actas/{id}/acciones/confirmar-pago-condena` | `confirmarPagoCondena(id)` | - | `PagoCondenaAccionResponse` | Confirma el pago de condena |
| POST | `/api/prototipo/actas/{id}/acciones/observar-pago-condena` | `observarPagoCondena(id)` | - | `PagoCondenaAccionResponse` | Observa el pago de condena |
| POST | `/api/prototipo/actas/{id}/acciones/consentir-condena-y-registrar-pago` | `consentirCondenaYRegistrarPago(id)` | - | `ConsentirCondenaYRegistrarPagoAccionResponse` | Consentimiento presencial de condena y registro de pago en un unico acto |

---

### PrototipoFalloApelacionController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/marcar-resultado-final-absuelto` | `marcarResultadoFinalAbsuelto(id)` | - | `MarcarResultadoAbsueltoAccionResponse` | Marca resultado final como ABSUELTO (no implica cierre automatico) |
| POST | `/api/prototipo/actas/{id}/acciones/dictar-fallo-absolutorio` | `dictarFalloAbsolutorio(id)` | - | `DictarFalloAccionResponse` | Dicta fallo absolutorio; produce documento FALLO_ABSOLUTORIO en PENDIENTE_FIRMA |
| POST | `/api/prototipo/actas/{id}/acciones/dictar-fallo-condenatorio` | `dictarFalloCondenatorio(id, request?)` | `DictarFalloCondenatorioAccionRequest` body con `montoCondena` | `DictarFalloAccionResponse` | Dicta fallo condenatorio; produce documento FALLO_CONDENATORIO en PENDIENTE_FIRMA |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-vencimiento-plazo-apelacion` | `registrarVencimientoPlazoApelacion(id)` | - | `RegistrarVencimientoPlazoApelacionAccionResponse` | Registra vencimiento del plazo de apelacion sin presentacion; resultadoFinal = CONDENA_FIRME |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-apelacion` | `registrarApelacion(id, request?)` | `RegistrarApelacionAccionRequest` body con `canal` requerido | `RegistrarApelacionAccionResponse` | Registra presentacion de apelacion/recurso administrativo |
| POST | `/api/prototipo/actas/{id}/acciones/resolver-apelacion` | `resolverApelacion(id, request?)` | `ResolverApelacionAccionRequest` body con `resultado` | `ResolverApelacionAccionResponse` | Resuelve la apelacion: RECHAZADA (CONDENA_FIRME) o ACEPTADA_ABSUELVE (ABSUELTO) |

---

### PrototipoArchivoParalizacionController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/cerrar-acta` | `cerrarActa(id)` | - | `CerrarActaAccionResponse` | Cierra el acta desde analisis |
| POST | `/api/prototipo/actas/{id}/acciones/archivar-acta` | `archivarActa(id)` | - | `ArchivarActaAccionResponse` | Archiva el acta directamente desde analisis |
| POST | `/api/prototipo/actas/{id}/acciones/archivar-por-vencimiento` | `archivarPorVencimiento(id)` | - | `ArchivarPorVencimientoAccionResponse` | Archivo especifico para casos de notificacion vencida |
| POST | `/api/prototipo/actas/{id}/acciones/reingresar-acta` | `reingresarActa(id)` | - | `ReingresarActaAccionResponse` | Reingreso desde macro-bandeja ARCHIVO; acta vuelve a PENDIENTE_ANALISIS |
| POST | `/api/prototipo/actas/{id}/acciones/reactivar-acta` | `reactivarActa(id)` | - | `ReactivarActaAccionResponse` | Reactivacion desde macro-bandeja PARALIZADAS; acta vuelve a PENDIENTE_ANALISIS |
| POST | `/api/prototipo/actas/{id}/acciones/paralizar-acta` | `paralizarActa(id, request?)` | `ParalizarActaRequest` body con `motivo` y `observacion` | `ParalizarActaAccionResponse` | Paraliza el acta administrativamente desde cualquier bandeja interna activa |
| POST | `/api/prototipo/actas/{id}/acciones/enviar-a-notificacion` | `enviarANotificacion(id)` | - | `EnviarANotificacionAccionResponse` | Envia el acta desde enriquecimiento (D2) a PENDIENTE_NOTIFICACION |
| POST | `/api/prototipo/actas/{id}/acciones/anular-acta` | `anularActa(id)` | - | `AnularActaPorNulidadAccionResponse` | Anula el acta desde enriquecimiento; motivoArchivo = NULIDAD |

---

### PrototipoMaterialesController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/resolver-pendiente-bloqueante-cierre` | `resolverPendienteBloqueanteCierre(id, tipo)` | Query param: `tipo` requerido | `RegistrarCumplimientoMaterialBloqueoCierreAccionResponse` | Alias de registrar-cumplimiento-material-bloqueo-cierre |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-constatacion-material-temprana` | `registrarConstatacionMaterialTemprana(id, tipo)` | Query param: `tipo` (SECUESTRO_RODADO, RETENCION_DOCUMENTAL, MEDIDA_PREVENTIVA_APLICABLE) | `RegistrarConstatacionMaterialTempranaAccionResponse` | Constatacion en D1/D2; incorpora ancla documental al expediente |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-medida-preventiva-posterior` | `registrarMedidaPreventivaPosterior(id)` | - | `RegistrarMedidaPreventivaPosteriorAccionResponse` | Medida preventiva nacida durante el tramite en PENDIENTE_ANALISIS |
| POST | `/api/prototipo/actas/{id}/acciones/reconocer-origen-bloqueo-cierre-material` | `reconocerOrigenBloqueoCierreMaterial(id, tipo)` | Query param: `tipo` (MEDIDA_ACTIVA, SECUESTRO_RODADO, RETENCION_DOCUMENTAL) | `ReconocerOrigenBloqueanteMaterialAccionResponse` | Reconoce el hecho material que origina el bloqueante de cierre |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-resolucion-bloqueo-cierre` | `registrarResolucionBloqueoCierreDocumental(id, tipo, docConCircuito)` | Query params: `tipo` requerido, `documentoConCircuitoFirmaNotif` (defecto false) | `RegistrarResolucionBloqueoCierreAccionResponse` | Agrega documento resolutorio; el bloqueo persiste hasta cumplimiento material |
| POST | `/api/prototipo/actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre` | `registrarCumplimientoMaterialBloqueoCierre(id, tipo)` | Query param: `tipo` requerido | `RegistrarCumplimientoMaterialBloqueoCierreAccionResponse` | Registro de cumplimiento material efectivo (medida levantada, rodado liberado, documentacion entregada) |

---

### PrototipoGestionExternaController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/derivar-a-apremio` | `derivarAApremio(id)` | - | `DerivarAGestionExternaAccionResponse` | Deriva el acta a gestion externa; tipoGestionExterna = APREMIO |
| POST | `/api/prototipo/actas/{id}/acciones/derivar-a-juzgado-de-paz` | `derivarAJuzgadoDePaz(id)` | - | `DerivarAGestionExternaAccionResponse` | Deriva el acta a gestion externa; tipoGestionExterna = JUZGADO_DE_PAZ |
| POST | `/api/prototipo/actas/{id}/acciones/reingresar-desde-gestion-externa` | `reingresarDesdeGestionExterna(id)` | - | `ReingresarDesdeGestionExternaAccionResponse` | Retorno generico desde GESTION_EXTERNA a PENDIENTE_ANALISIS |
| POST | `/api/prototipo/actas/{id}/acciones/apremio-reingresar-sin-pago` | `apremioReingresarSinPago(id)` | - | `ReingresarDesdeApremioSinPagoAccionResponse` | Apremio: retorno sin pago; condena firme pendiente |
| POST | `/api/prototipo/actas/{id}/acciones/apremio-registrar-pago` | `apremioRegistrarPago(id)` | - | `RegistrarPagoEnApremioAccionResponse` | Apremio: registra pago efectuado en el proceso externo |
| POST | `/api/prototipo/actas/{id}/acciones/apremio-reingresar-monto-modificado` | `apremioReingresarMontoModificado(id, request)` | `JuzgadoMontoModificadoRequest` body con `nuevoMonto` | `RegistrarResolucionJuzgadoAccionResponse` | Apremio: reingresa con propuesta de modificacion de monto de condena |
| POST | `/api/prototipo/actas/{id}/acciones/apremio-reingresar-absuelto` | `apremioReingresarAbsuelto(id)` | - | `RegistrarResolucionJuzgadoAccionResponse` | Apremio: reingresa con propuesta de absolucion |
| POST | `/api/prototipo/actas/{id}/acciones/juzgado-reingresar-absuelto` | `juzgadoReingresarAbsuelto(id)` | - | `RegistrarResolucionJuzgadoAccionResponse` | Juzgado de Paz: resolucion absolutoria; resultadoFinal = ABSUELTO |
| POST | `/api/prototipo/actas/{id}/acciones/juzgado-reingresar-condena-confirmada` | `juzgadoReingresarCondenaConfirmada(id)` | - | `RegistrarResolucionJuzgadoAccionResponse` | Juzgado de Paz: confirma condena original; CONDENA_FIRME pendiente de pago |
| POST | `/api/prototipo/actas/{id}/acciones/juzgado-reingresar-monto-modificado` | `juzgadoReingresarMontoModificado(id, request)` | `JuzgadoMontoModificadoRequest` body con `nuevoMonto` | `RegistrarResolucionJuzgadoAccionResponse` | Juzgado de Paz: modifica monto de condena; CONDENA_FIRME pendiente de pago |

---

### PrototipoDocumentosFirmaController

`@RequestMapping("/api/prototipo")`

| Metodo | Ruta completa | Metodo Java | Request | Response | Descripcion |
|---|---|---|---|---|---|
| POST | `/api/prototipo/actas/{id}/acciones/generar-medida-preventiva` | `generarMedidaPreventiva(id)` | - | `GenerarMedidaPreventivaAccionResponse` | Genera el documento MEDIDA_PREVENTIVA pendiente de firma |
| POST | `/api/prototipo/actas/{id}/acciones/generar-notificacion-acta` | `generarNotificacionActa(id)` | - | `GenerarNotificacionActaAccionResponse` | Genera el documento de notificacion del acta pendiente de firma |
| POST | `/api/prototipo/actas/{id}/acciones/generar-nulidad` | `generarNulidad(id)` | - | `GenerarNulidadAccionResponse` | Produce pieza NULIDAD en PENDIENTES_RESOLUCION_REDACCION |
| POST | `/api/prototipo/actas/{id}/acciones/generar-resolucion` | `generarResolucion(id)` | - | `GenerarResolucionAccionResponse` | Produce pieza RESOLUCION; al firmarse pasa a PENDIENTE_NOTIFICACION |
| POST | `/api/prototipo/actas/{id}/acciones/generar-rectificacion` | `generarRectificacion(id)` | - | `GenerarRectificacionAccionResponse` | Produce pieza RECTIFICACION; al firmarse pasa a PENDIENTE_NOTIFICACION |
| POST | `/api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}` | `firmarDocumento(id, documentoId)` | - | `FirmarDocumentoAccionResponse` | Firma un documento por ID; puede disparar transiciones de bandeja |

---

## Rutas duplicadas

No se detectaron rutas duplicadas. Verificacion completa de combinaciones metodo HTTP + ruta
sobre los 80 endpoints: ninguna colision encontrada.

La aparente ambiguedad `GET /actas/buscar` vs `GET /actas/{id}` es comportamiento estandar de
Spring MVC: los segmentos literales tienen prioridad absoluta sobre variables de path. No es un conflicto.

---

## Helper centralizado: resultadoFinalVigente

La funcion `resultadoFinalVigente(store, actaId)` esta centralizada en:

    domain/PrototipoResultadoFinalHelper.java

Es importada como static import en tres lugares:

- `ActaDetalleMapper.java`
- `PrototipoFalloApelacionController.java`
- `PrototipoPagosController.java`

No existe duplicacion de cuerpo. Es un helper unico reutilizado correctamente.

---

## Compatibilidad Angular

Angular no requiere cambios.

La desmonolitizacion fue un refactor interno del backend. Desde la perspectiva del consumidor HTTP:

- Todas las rutas publicas se mantuvieron identicas bajo `/api/prototipo/...`.
- Todos los DTOs de request y response se conservaron sin modificaciones.
- Todos los codigos HTTP de respuesta (200, 201, 400, 404, 409, 500) se conservaron.
- El cambio fue exclusivamente una redistribucion de clases Java dentro del package `web/`.

---

## Validaciones ejecutadas

| Comando | Resultado |
|---|---|
| `mvn clean test -q` en `backend/api-faltas-prototipo` | EXIT 0 -- todos los tests verdes |
| `git diff --check` | EXIT 0 -- sin problemas de whitespace |
| `rg mojibake` sobre `prototipo/web/` | Sin coincidencias -- sin mojibake |

---

## Hallazgos / deuda pendiente

Sin deuda en la desmonolitizacion. Los 11 controllers estan completos, sin endpoints faltantes
y sin rutas duplicadas.

Sin mojibake detectado en los archivos del package `web/`.

Helper `resultadoFinalVigente` correctamente centralizado. No hay duplicacion de cuerpo.

El `PrototipoApiController.java` fue eliminado del filesystem. La eliminacion aun no fue
registrada en el index de git: pendiente `git add -A` o `git rm` en el proximo commit.

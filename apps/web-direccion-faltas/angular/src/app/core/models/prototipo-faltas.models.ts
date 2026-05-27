export type BandejaCodigo =
  | 'NOTIFICACIONES'
  | 'LABRADAS'
  | 'ACTAS_EN_ENRIQUECIMIENTO'
  | 'PENDIENTE_ANALISIS'
  | 'PENDIENTES_RESOLUCION_REDACCION'
  | 'PENDIENTES_FALLO'
  | 'PENDIENTE_FIRMA'
  | 'PENDIENTE_NOTIFICACION'
  | 'EN_NOTIFICACION'
  | 'CON_APELACION'
  | 'GESTION_EXTERNA'
  | 'PARALIZADAS'
  | 'ARCHIVO'
  | 'CERRADAS'
  | 'PENDIENTE_PREPARACION_DOCUMENTAL';

export type DependenciaActaDemo =
  | 'TRANSITO'
  | 'INSPECCIONES'
  | 'FISCALIZACION'
  | 'BROMATOLOGIA';

export interface CrearActaMockDemoRequest {
  dependencia: DependenciaActaDemo;
  tipoActaDemo?: 'TRANSITO' | 'CONTRAVENCION' | 'BROMATOLOGIA';
  ejeUrbano?: boolean;
  rodadoRetenidoOSecuestrado?: boolean;
  documentacionRetenida?: boolean;
  medidaPreventivaClausura?: boolean;
  medidaPreventivaParalizacionObra?: boolean;
  decomisoSustanciasAlimenticias?: boolean;
}

export interface CerrabilidadDemo {
  resultadoFinal: string | null;
  cerrable: boolean;
  pendientesBloqueantesCierre: string[];
  motivoNoCerrable: string | null;
}

export interface ComprobanteMockDemo {
  id: string;
  nombreArchivo: string;
}

export interface PagoInformadoDemo {
  fechaInformado: string;
  comprobante: ComprobanteMockDemo | null;
}

export interface HechosMaterialesEjeDemo {
  clave: string;
  etiqueta: string;
  fase: string;
  bloqueaCierre: boolean;
  descripcion: string;
  ejeBloqueanteCierre: string | null;
}

export interface HechosMaterialesActaDemo {
  ejes: HechosMaterialesEjeDemo[];
  lecturaOperativa: string | null;
}

export interface TransitoDatoDemo {
  ejeUrbano: boolean;
  rodadoRetenidoOSecuestrado: boolean;
  documentacionRetenida: boolean;
  medidaPreventivaAplicable: boolean;
}

export interface BromatologiaDatoDemo {
  decomisoSustanciasAlimenticias: boolean;
}

export interface ActaResumenDemo {
  id: string;
  numeroActa: string;
  infractorNombre: string;
  bloqueActual: string;
  estadoProcesoActual: string;
  situacionAdministrativaActual: string;
  bandejaActual: string;
  situacionPago: string;
  accionPendiente: string | null;
  motivoArchivo: string | null;
  tipoGestionExterna: string | null;
  cerrabilidad: CerrabilidadDemo;
}


/** Contrato GET /bandejas - SubBandejaResumenResponse. */
export interface SubBandejaResumen {
  codigo: string;
  label: string;
  cantidad: number;
}

/** Contrato GET /bandejas - BandejaResponse. */
export interface BandejaResponse {
  codigo: BandejaCodigo;
  label: string;
  cantidad: number;
  subBandejas: SubBandejaResumen[];
}

/** Contrato GET /bandejas/{codigo}/actas - ActaBandejaItemResponse. */
export interface ActaBandejaItem extends ActaResumenDemo {
  subBandeja: string;
  subBandejaLabel: string;
  chip: string | null;
  accionPrincipal: string | null;
  prioridadSubBandeja: number;
  chipsSecundarios: string[];
  dependenciaDemo: string | null;
}

/** Contrato ActaNotificacionResponse en GET /actas/{id} (detalle). */
export interface ActaNotificacionTipificadaDemo {
  id: string;
  actaId: string;
  canal: string | null;
  estadoNotificacion: string | null;
  destinatarioResumen: string | null;
  tipo: string | null;
  canalTipificado: string | null;
  estado: string | null;
  resultado: string | null;
  referencia: string | null;
  eventoRelacionado: string | null;
  loteId: string | null;
  referenciaExterna: string | null;
  fechaPreparacion: string | null;
  fechaEnvio: string | null;
  fechaResultado: string | null;
  observacion: string | null;
  destinatarioNombre: string | null;
  destinatarioEmail: string | null;
  domicilioTexto: string | null;
  domicilioElectronicoVerificado: boolean | null;
  diasPlazoNotificacionElectronica: number | null;
}

export interface ActaDetalleDemo extends ActaResumenDemo {
  estaCerrada: boolean;
  permiteReingreso: boolean;
  fechaCreacion: string;
  infractorDocumento: string;
  inspectorNombre: string;
  resumenHecho: string;
  tieneDocumentos: boolean;
  tieneNotificaciones: boolean;
  piezasRequeridas: string[];
  piezasGeneradas: string[];
  pagoInformado: PagoInformadoDemo | null;
  accionesPagoVoluntarioDisponibles: string[];
  situacionPagoCondena: SituacionPagoCondenaDemo;
  accionesPagoCondenaDisponibles: string[];
  accionesGestionExternaDisponibles: string[];
  hechosMateriales: HechosMaterialesActaDemo | null;
  dependenciaDemo: string | null;
  tipoActaDemo: string | null;
  datosTransito: TransitoDatoDemo | null;
  datosBromatologia: BromatologiaDatoDemo | null;
  /**
   * Monto del acta fijado por Direccion de Faltas al habilitar el pago
   * voluntario. Null si la accion administrativa aun no se ejecuto. No
   * implica generacion de comprobantes (sin EM, sin RC, sin Cmte/Pref/Nro).
   */
  montoPagoVoluntario: number | null;
  /**
   * Monto de condena fijado al dictar fallo condenatorio. Distinto de
   * montoPagoVoluntario. Null si aun no se dicto fallo condenatorio.
   */
  montoCondena: number | null;
  /** Notificaciones tipificadas del acta (detalle). */
  notificaciones?: ActaNotificacionTipificadaDemo[] | null;
}

export interface BadgeDemo {
  etiqueta: string;
  tono: 'neutral' | 'info' | 'warn' | 'ok';
}

export interface PrototipoHealthResponse {
  status: string;
  modulo: string;
  modo: string;
  cantidadActas: number;
}

export interface PrototipoResetResponse {
  resultado: string;
  mensaje: string;
  cantidadActas: number;
}

/** Contrato GET /actas/{id}/documentos - ActaDocumentoResponse */
export interface DocumentoActaDemo {
  id: string;
  actaId: string;
  tipoDocumento: string;
  estadoDocumento: string;
  nombreArchivo: string;
}

/** Contrato GET /actas/{id}/eventos - ActaEventoResponse */
export interface EventoActaDemo {
  id: string;
  fechaHora: string;
  tipoEvento: string;
  bloqueOrigen: string;
  bloqueDestino: string;
  descripcion: string;
}

/**
 * Contrato POST /actas/{id}/acciones/firmar-documento/{documentoId}
 * - FirmarDocumentoAccionResponse.
 */
export interface FirmarDocumentoAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  documentoId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}
/**
 * Contrato POST /actas/{id}/acciones/generar-nulidad
 * - GenerarNulidadAccionResponse.
 *
 * Sin body. Precondicion backend: acta en PENDIENTES_RESOLUCION_REDACCION
 * con pieza NULIDAD requerida y aun no generada. Efecto: materializa
 * documento NULIDAD en PENDIENTE_FIRMA y actualiza piezasGeneradas; la
 * bandeja/estado los define el agregador del backend.
 */
export interface GenerarNulidadAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}

/**
 * Contrato POST /actas/{id}/acciones/generar-medida-preventiva
 * - GenerarMedidaPreventivaAccionResponse.
 */
export interface GenerarMedidaPreventivaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}

/**
 * Contrato POST /actas/{id}/acciones/generar-notificacion-acta
 * - GenerarNotificacionActaAccionResponse.
 */
export interface GenerarNotificacionActaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}


export type TipoCumplimientoMaterialBloqueante =
  | 'LEVANTAMIENTO_MEDIDA_PREVENTIVA'
  | 'LIBERACION_RODADO'
  | 'ENTREGA_DOCUMENTACION';

/**
 * Tipos aceptados por el endpoint de resolucion documental de bloqueante
 * de cierre. Coinciden 1 a 1 con TipoCumplimientoMaterialBloqueante: la
 * resolucion documental es el paso previo al cumplimiento material y el
 * backend valida origen y bandeja (no se inventan tipos en Angular).
 */
export type TipoResolucionBloqueoCierre = TipoCumplimientoMaterialBloqueante;

/**
 * Contrato POST /actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre
 * - RegistrarCumplimientoMaterialBloqueoCierreAccionResponse.
 */
export interface RegistrarCumplimientoMaterialBloqueoCierreAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  pendienteCumplido: string;
  cerrabilidad: CerrabilidadDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-resolucion-bloqueo-cierre
 * - RegistrarResolucionBloqueoCierreAccionResponse.
 *
 * Incorpora el documento resolutorio mock previo al cumplimiento material
 * efectivo. El bloqueante persiste hasta que se invoque
 * registrar-cumplimiento-material-bloqueo-cierre sobre el mismo tipo.
 * El backend no cambia bandeja al registrar resolucion: la mantiene.
 */
export interface RegistrarResolucionBloqueoCierreAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  documentoId: string;
  tipoDocumento: string;
  pendienteAtendido: string;
  cerrabilidad: CerrabilidadDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/cerrar-acta
 * - CerrarActaAccionResponse.
 */
export interface CerrarActaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}

export type ResultadoNotificacionDemo = 'POSITIVA' | 'NEGATIVA' | 'VENCIDA';


export interface NotificacionCorreoLoteItemDemo {
  notificacionId: string;
  actaId: string;
  acta: string;
  tipo: string;
  canal: string;
  referenciaExterna: string;
  estadoNotificacion: string;
  resultadoNotificacion: string;
  destinatario: string | null;
  domicilio: string | null;
  observacion: string | null;
}

export interface GenerarLoteCorreoRequestDemo {
  tipo?: string;
  notificacionIds?: string[];
}

export interface GenerarLoteCorreoResultadoDemo {
  loteId: string;
  cantidad: number;
  nombreArchivo: string;
  rutaArchivo: string | null;
  notificaciones: NotificacionCorreoLoteItemDemo[];
}



export interface CorreoPostalNotificacionListaDemo {
  notificacionId: string;
  actaId: string;
  acta: string;
  tipo: string;
  canal: string;
  estado: string;
  resultado: string;
  destinatario: string | null;
  domicilio: string | null;
  observacion: string | null;
}

export interface CorreoLoteResumenDemo {
  loteId: string;
  cantidad: number;
  nombreArchivo: string;
  rutaArchivo: string | null;
  estado: string;
  fechaGeneracion: string | null;
  tiposIncluidos: string[];
  tipoDominante: string | null;
  positivas: number;
  negativas: number;
  vencidas: number;
  notificaciones: NotificacionCorreoLoteItemDemo[];
}

export interface CorreoPostalTrazabilidadDemo {
  acta: string;
  actaId: string;
  notificacionId: string;
  tipo: string;
  canal: string;
  estadoNotificacion: string;
  resultadoNotificacion: string;
  loteId: string | null;
  estadoLote: string | null;
  fechaGeneracion: string | null;
  fechaProcesamiento: string | null;
  observacion: string | null;
  referenciaExterna: string | null;
}

export interface AnularLoteCorreoResultadoDemo {
  estado: string;
  mensaje: string;
  loteId: string;
}

export interface EnviarIndividualCorreoResultadoDemo {
  estado: string;
  mensaje: string;
  notificacionId: string;
  notificacion: NotificacionCorreoLoteItemDemo | null;
}

export interface ProcesarRespuestaCorreoResultadoDemo {
  total: number;
  positivas: number;
  negativas: number;
  vencidas: number;
  errores: number;
  detalleErrores: string[];
  nombreArchivo: string | null;
  rutaArchivoProcesado: string | null;
}


export interface NotificadorMunicipalNotificacionDemo {
  notificacionId: string;
  actaId: string;
  acta: string;
  tipo: string;
  canal: string;
  estado: string;
  resultado: string;
  destinatario: string | null;
  domicilio: string | null;
  observacion: string | null;
  qrNotificacion: string | null;
  fechaPreparacion: string | null;
  fechaEnvio: string | null;
}

export interface NotificadorMunicipalAcuseRequestDemo {
  resultado: ResultadoNotificacionDemo;
  observacion?: string;
}

export interface NotificadorMunicipalAcuseResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  acta: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  notificacion: ActaNotificacionTipificadaDemo;
  vista: NotificadorMunicipalNotificacionDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-notificacion-positiva
 * - RegistrarNotificacionPositivaAccionResponse.
 */
export interface RegistrarNotificacionPositivaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-notificacion-negativa
 * - RegistrarNotificacionNegativaAccionResponse.
 */
export interface RegistrarNotificacionNegativaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  accionPendiente: string;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-notificacion-vencida
 * - RegistrarNotificacionVencidaAccionResponse.
 */
export interface RegistrarNotificacionVencidaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  accionPendiente: string;
}

/**
 * Contrato POST /actas/{id}/acciones/reintentar-notificacion
 * - ReintentarNotificacionAccionResponse.
 *
 * Precondicion backend: acta en PENDIENTE_ANALISIS con
 * accionPendiente = REINTENTAR_NOTIFICACION (caso producido por
 * notificacion negativa). Efecto: vuelve a PENDIENTE_NOTIFICACION
 * reutilizando la notificacion existente y limpia la marca operativa
 * de reintento; el evento se registra como NOTIFICACION_REINTENTADA.
 */
export interface ReintentarNotificacionAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
}

/**
 * Situacion de pago mock expuesta por el backend (SituacionPagoMock).
 * Refleja el enum del prototipo sin que Angular agregue reglas adicionales.
 */
export type SituacionPagoDemo =
  | 'SIN_PAGO'
  | 'SOLICITADO'
  | 'PAGO_INFORMADO'
  | 'PENDIENTE_CONFIRMACION'
  | 'CONFIRMADO'
  | 'OBSERVADO';

/**
 * Acciones del flujo de pago voluntario soportadas por la UI demo.
 * Cada valor mapea 1 a 1 con un endpoint real del backend prototipo.
 */
export type AccionPagoVoluntarioDemo =
  | 'SOLICITAR'
  | 'INFORMAR'
  | 'ADJUNTAR_COMPROBANTE'
  | 'CONFIRMAR'
  | 'OBSERVAR';

export type SituacionPagoCondenaDemo =
  | 'NO_APLICA'
  | 'PENDIENTE'
  | 'INFORMADO'
  | 'CONFIRMADO'
  | 'OBSERVADO';

export type AccionPagoCondenaDemo = 'INFORMAR' | 'CONFIRMAR' | 'OBSERVAR';

/**
 * Body de POST /actas/{id}/acciones/registrar-solicitud-pago-voluntario.
 * Monto fijado por Direccion de Faltas al habilitar el pago voluntario.
 * El backend valida que sea estrictamente mayor a cero; no genera
 * comprobantes (sin EM, sin RC, sin Cmte/Pref/Nro).
 */
export interface RegistrarSolicitudPagoVoluntarioAccionRequest {
  monto: number;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-solicitud-pago-voluntario
 * - RegistrarSolicitudPagoVoluntarioAccionResponse.
 */
export interface RegistrarSolicitudPagoVoluntarioAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  accionPendiente: string;
  montoPagoVoluntario: number | null;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-pago-informado
 * - RegistrarPagoInformadoAccionResponse.
 */
export interface RegistrarPagoInformadoAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  situacionPago: string;
}

/**
 * Contrato POST /actas/{id}/acciones/adjuntar-comprobante-pago-informado
 * - AdjuntarComprobantePagoInformadoAccionResponse.
 */
export interface AdjuntarComprobantePagoInformadoAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  situacionPago: string;
  accionPendiente: string;
  pagoInformado: PagoInformadoDemo | null;
}

/**
 * Contrato POST /actas/{id}/acciones/confirmar-pago-informado
 * - ConfirmarPagoInformadoAccionResponse.
 */
export interface ConfirmarPagoInformadoAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  situacionPago: string;
}

/**
 * Contrato POST /actas/{id}/acciones/observar-pago-informado
 * - ObservarPagoInformadoAccionResponse.
 */
export interface ObservarPagoInformadoAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  situacionPago: string;
}

export interface PagoCondenaAccionResponseDemo {
  status: string;
  mensaje: string;
  actaId: string;
  situacionPagoCondena: SituacionPagoCondenaDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/archivar-acta
 * - ArchivarActaAccionResponse.
 *
 * Precondicion backend: acta en bandeja PENDIENTE_ANALISIS. Efecto: mueve
 * el acta a la macro-bandeja ARCHIVO y deja explicito el motivoArchivo
 * recibido del backend. Angular no infiere motivos.
 */
export interface ArchivarActaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  motivoArchivo: string;
}

/**
 * Contrato POST /actas/{id}/acciones/reingresar-acta
 * - ReingresarActaAccionResponse.
 *
 * Precondicion backend: acta en bandeja ARCHIVO y permiteReingreso=true.
 * Efecto: vuelve a PENDIENTE_ANALISIS con accionPendiente operativa
 * REVISION_POST_REINGRESO. El motivoArchivoPrevio se preserva como
 * trazabilidad y puede ser null si el acta fue archivada antes de
 * registrar motivo.
 */
export interface ReingresarActaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  accionPendiente: string;
  motivoArchivoPrevio: string | null;
}

/**
 * Tipo de derivacion a gestion externa expuesto como par de endpoints
 * tipados del backend (no se inventan tipos en Angular). Cada valor mapea
 * 1 a 1 con un POST distinto:
 *  - APREMIO ......... POST /acciones/derivar-a-apremio
 *  - JUZGADO_DE_PAZ .. POST /acciones/derivar-a-juzgado-de-paz
 *
 * Las precondiciones (PENDIENTE_ANALISIS + marca operativa habilitadora)
 * son responsabilidad exclusiva del backend; Angular solo dispara la
 * accion contra el endpoint correspondiente.
 */
export type TipoGestionExternaDemo = 'APREMIO' | 'JUZGADO_DE_PAZ';

/**
 * Contrato POST /actas/{id}/acciones/derivar-a-apremio
 * y POST /actas/{id}/acciones/derivar-a-juzgado-de-paz
 * - DerivarAGestionExternaAccionResponse.
 *
 * Precondicion backend: acta en PENDIENTE_ANALISIS con accionPendiente
 * DERIVAR_GESTION_EXTERNA (primera derivacion) o
 * REVISION_POST_GESTION_EXTERNA (re-derivacion despues de un retorno).
 * Efecto: el acta sale del circuito interno y pasa a la macro-bandeja
 * GESTION_EXTERNA con el tipoGestionExterna asignado.
 */
export interface DerivarAGestionExternaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  tipoGestionExterna: string;
}

/**
 * Contrato POST /actas/{id}/acciones/reingresar-desde-gestion-externa
 * - ReingresarDesdeGestionExternaAccionResponse.
 *
 * Precondicion backend: acta en GESTION_EXTERNA con permiteReingreso=true.
 * Efecto: vuelve a PENDIENTE_ANALISIS con accionPendiente operativa
 * REVISION_POST_GESTION_EXTERNA. El tipoGestionExternaPrevia se preserva
 * como trazabilidad sintetica de la gestion externa de la que provino
 * (puede ser null si no constaba tipo).
 */
export interface ReingresarDesdeGestionExternaAccionResponseDemo {
  resultado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  accionPendiente: string;
  tipoGestionExternaPrevia: string | null;
}

/**
 * Contrato GET /infractor/actas/{codigoQr} - ActaInfractorResponse.
 * Vista ciudadana minima del acta para el portal del infractor.
 */

/**
 * Canales validos para registrar la presentacion de apelacion/recurso.
 * Coinciden con CanalPresentacionApelacionMock del backend prototipo.
 */
export type CanalPresentacionApelacionDemo = 'PORTAL_INFRACTOR' | 'PRESENCIAL_DIRECCION';

/**
 * Body de POST /actas/{id}/acciones/registrar-apelacion.
 */
export interface RegistrarApelacionAccionRequestDemo {
  canal: CanalPresentacionApelacionDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/registrar-apelacion
 * - RegistrarApelacionAccionResponse.
 */
export interface RegistrarApelacionAccionResponseDemo {
  estado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  resultadoFinal: string;
  canal: string;
}

/**
 * Resultados validos para POST /actas/{id}/acciones/resolver-apelacion.
 */
export type ResultadoResolucionApelacionDemo = 'RECHAZADA' | 'ACEPTADA_ABSUELVE';

/**
 * Body de POST /actas/{id}/acciones/resolver-apelacion.
 */
export interface ResolverApelacionAccionRequestDemo {
  resultado: ResultadoResolucionApelacionDemo;
}

/**
 * Contrato POST /actas/{id}/acciones/resolver-apelacion
 * - ResolverApelacionAccionResponse.
 */
export interface ResolverApelacionAccionResponseDemo {
  estado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  resultadoFinal: string;
  resultadoResolucion: string;
}


/**
 * Contrato POST /actas/{id}/acciones/registrar-vencimiento-plazo-apelacion
 * - RegistrarVencimientoPlazoApelacionAccionResponse.
 */
export interface RegistrarVencimientoPlazoApelacionAccionResponseDemo {
  estado: string;
  mensaje: string;
  actaId: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  resultadoFinal: string;
}

export interface DictarFalloCondenatorioAccionRequestDemo {
  montoCondena: number;
}

/**
 * Contrato POST /actas/{id}/acciones/dictar-fallo-absolutorio
 * y POST /actas/{id}/acciones/dictar-fallo-condenatorio
 * - DictarFalloAccionResponse.
 *
 * El acta queda en PENDIENTE_FIRMA con el documento del fallo en
 * PENDIENTE_FIRMA; el resultadoFinal se materializa al notificar
 * positivamente el fallo.
 */
export interface DictarFalloAccionResponseDemo {
  estado: string;
  mensaje: string;
  actaId: string;
  documentoId: string;
  tipoDocumento: string;
  bandejaActual: string;
  estadoProcesoActual: string;
  montoCondena?: number | null;
}

export interface NotificacionPortalPendienteDemo {
  id: string;
  tipo: string;
  canal: string;
  estado: string;
  resultado: string;
  destinatario: string | null;
  resumen: string | null;
  mensajeVisible: string | null;
}

export interface ActaInfractorResponseDemo {
  acta: string;
  codigoQr: string;
  estadoVisible: string;
  situacionPago: string;
  resultadoFinal: string;
  montoPagoVoluntario: number | null;
  montoCondena: number | null;
  puedeConsultarEstado: boolean;
  puedeSolicitarPagoVoluntario: boolean;
  puedePagar: boolean;
  puedePresentarApelacion: boolean;
  puedeConfirmarVisualizacionNotificacion: boolean;
  notificacionPortalPendiente: NotificacionPortalPendienteDemo | null;
  domicilioElectronicoVerificado: boolean | null;
  mensajeVisible: string | null;
}

/**
 * Construye el codigo QR/codigo ciudadano demo a partir del actaId interno.
 * Formato: QR-{actaId}-DEMO. Reemplazar cuando el backend administrativo
 * exponga codigoQr directamente en ActaDetalleResponse.
 */
export function codigoQrDemoParaActa(actaId: string): string {
  return `QR-${actaId}-DEMO`;
}

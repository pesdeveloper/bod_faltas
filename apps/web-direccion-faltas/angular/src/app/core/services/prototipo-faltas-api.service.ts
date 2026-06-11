import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CONFIG, ApiConfig } from '../config/api.config';
import {
  ActaBandejaItem,
  ActaDetalleDemo,
  ActaInfractorResponseDemo,
  ActaResumenDemo,
  BandejaResponse,
  AdjuntarComprobantePagoInformadoAccionResponseDemo,
  ArchivarActaAccionResponseDemo,
  BandejaCodigo,
  CerrarActaAccionResponseDemo,
  ConfirmarPagoInformadoAccionResponseDemo,
  CrearActaMockDemoRequest,
  DerivarAGestionExternaAccionResponseDemo,
  DictarFalloAccionResponseDemo,
  DictarFalloCondenatorioAccionRequestDemo,
  DocumentoActaDemo,
  EventoActaDemo,
  FirmarDocumentoAccionResponseDemo,
  AnularLoteCorreoResultadoDemo,
  CorreoLoteResumenDemo,
  CorreoPostalNotificacionListaDemo,
  CorreoPostalTrazabilidadDemo,
  EnviarIndividualCorreoResultadoDemo,
  GenerarLoteCorreoResultadoDemo,
  GenerarMedidaPreventivaAccionResponseDemo,
  GenerarNotificacionActaAccionResponseDemo,
  GenerarNulidadAccionResponseDemo,
  GenerarRectificacionAccionResponseDemo,
  GenerarResolucionAccionResponseDemo,
  NotificadorMunicipalAcuseRequestDemo,
  NotificadorMunicipalAcuseResponseDemo,
  NotificadorMunicipalNotificacionDemo,
  ObservarPagoInformadoAccionResponseDemo,
  PagoCondenaAccionResponseDemo,
  ConsentirCondenaYRegistrarPagoAccionResponseDemo,
  ProcesarRespuestaCorreoResultadoDemo,
  PrototipoHealthResponse,
  PrototipoResetResponse,
  RegistrarCumplimientoMaterialBloqueoCierreAccionResponseDemo,
  RegistrarNotificacionNegativaAccionResponseDemo,
  RegistrarNotificacionPositivaAccionResponseDemo,
  RegistrarNotificacionVencidaAccionResponseDemo,
  ReintentarNotificacionAccionResponseDemo,
  RegistrarPagoInformadoAccionResponseDemo,
  RegistrarResolucionBloqueoCierreAccionResponseDemo,
  RegistrarSolicitudPagoVoluntarioAccionRequest,
  RegistrarSolicitudPagoVoluntarioAccionResponseDemo,
  RegistrarVencimientoPagoVoluntarioAccionResponseDemo,
  ReactivarActaAccionResponseDemo,
  ReingresarActaAccionResponseDemo,
  ReingresarDesdeGestionExternaAccionResponseDemo,
  ReingresarDesdeApremioSinPagoAccionResponseDemo,
  RegistrarPagoEnApremioAccionResponseDemo,
  RegistrarResolucionJuzgadoAccionResponseDemo,
  RegistrarApelacionAccionRequestDemo,
  ResolverApelacionAccionRequestDemo,
  ResolverApelacionAccionResponseDemo,
  RegistrarApelacionAccionResponseDemo,
  RegistrarVencimientoPlazoApelacionAccionResponseDemo,
  TipoCumplimientoMaterialBloqueante,
  TipoResolucionBloqueoCierre,
  PrototipoActaBusquedaResponse,
  EnviarANotificacionAccionResponseDemo,
  AnularActaPorNulidadAccionResponseDemo,
} from '../models/prototipo-faltas.models';

@Injectable({ providedIn: 'root' })
export class PrototipoFaltasApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_CONFIG) private readonly api: ApiConfig,
  ) {}

  health(): Observable<PrototipoHealthResponse> {
    return this.http.get<PrototipoHealthResponse>(`${this.api.baseUrl}/health`);
  }

  reset(): Observable<PrototipoResetResponse> {
    return this.http.post<PrototipoResetResponse>(`${this.api.baseUrl}/reset`, null);
  }

  listarBandejas(): Observable<BandejaResponse[]> {
    return this.http.get<BandejaResponse[]>(`${this.api.baseUrl}/bandejas`);
  }

  listarActasPorBandeja(
    bandeja: BandejaCodigo,
    subBandeja?: string | null,
  ): Observable<ActaBandejaItem[]> {
    let params = new HttpParams();
    if (subBandeja && subBandeja.trim().length > 0) {
      params = params.set('subBandeja', subBandeja.trim());
    }
    return this.http.get<ActaBandejaItem[]>(`${this.api.baseUrl}/bandejas/${bandeja}/actas`, {
      params,
    });
  }

  obtenerActa(id: string): Observable<ActaDetalleDemo> {
    return this.http.get<ActaDetalleDemo>(`${this.api.baseUrl}/actas/${id}`);
  }

  getActaInfractorPorCodigoQr(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.get<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}`,
    );
  }


  registrarApelacionInfractorPorCodigoQr(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/registrar-apelacion`,
      null,
    );
  }

  solicitarPagoVoluntarioInfractor(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/solicitar-pago-voluntario`,
      null,
    );
  }

  informarPagoVoluntario(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/pagar-voluntario`,
      null,
    );
  }

  confirmarVisualizacionNotificacionInfractorPorCodigoQr(
    codigoQr: string,
  ): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/confirmar-visualizacion-notificacion`,
      null,
    );
  }

  verDocumentoInfractor(
    codigoQr: string,
    tipoDocumento: string,
  ): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/documentos/${encodeURIComponent(tipoDocumento)}/ver`,
      null,
    );
  }

  pagarCondena(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/pagar-condena`,
      null,
    );
  }

  consentirCondena(codigoQr: string): Observable<ActaInfractorResponseDemo> {
    return this.http.post<ActaInfractorResponseDemo>(
      `${this.api.baseUrl}/infractor/actas/${encodeURIComponent(codigoQr)}/acciones/consentir-condena`,
      null,
    );
  }

    listarDocumentosActa(actaId: string): Observable<DocumentoActaDemo[]> {
    return this.http.get<DocumentoActaDemo[]>(`${this.api.baseUrl}/actas/${actaId}/documentos`);
  }

  listarEventosActa(actaId: string): Observable<EventoActaDemo[]> {
    return this.http.get<EventoActaDemo[]>(`${this.api.baseUrl}/actas/${actaId}/eventos`);
  }

  crearActaMockDemo(request: CrearActaMockDemoRequest): Observable<ActaDetalleDemo> {
    return this.http.post<ActaDetalleDemo>(`${this.api.baseUrl}/actas/mock`, request);
  }

  firmarDocumento(
    actaId: string,
    documentoId: string,
  ): Observable<FirmarDocumentoAccionResponseDemo> {
    return this.http.post<FirmarDocumentoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/firmar-documento/${documentoId}`,
      null,
    );
  }

  generarNulidad(actaId: string): Observable<GenerarNulidadAccionResponseDemo> {
    return this.http.post<GenerarNulidadAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/generar-nulidad`,
      null,
    );
  }

  generarMedidaPreventiva(
    actaId: string,
  ): Observable<GenerarMedidaPreventivaAccionResponseDemo> {
    return this.http.post<GenerarMedidaPreventivaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/generar-medida-preventiva`,
      null,
    );
  }

  generarNotificacionActa(
    actaId: string,
  ): Observable<GenerarNotificacionActaAccionResponseDemo> {
    return this.http.post<GenerarNotificacionActaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/generar-notificacion-acta`,
      null,
    );
  }

  generarResolucion(actaId: string): Observable<GenerarResolucionAccionResponseDemo> {
    return this.http.post<GenerarResolucionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/generar-resolucion`,
      null,
    );
  }

  generarRectificacion(actaId: string): Observable<GenerarRectificacionAccionResponseDemo> {
    return this.http.post<GenerarRectificacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/generar-rectificacion`,
      null,
    );
  }

  cerrarActa(actaId: string): Observable<CerrarActaAccionResponseDemo> {
    return this.http.post<CerrarActaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/cerrar-acta`,
      null,
    );
  }

  registrarCumplimientoMaterialBloqueoCierre(
    actaId: string,
    tipo: TipoCumplimientoMaterialBloqueante,
  ): Observable<RegistrarCumplimientoMaterialBloqueoCierreAccionResponseDemo> {
    const params = new HttpParams().set('tipo', tipo);
    return this.http.post<RegistrarCumplimientoMaterialBloqueoCierreAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-cumplimiento-material-bloqueo-cierre`,
      null,
      { params },
    );
  }

  registrarResolucionBloqueoCierreDocumental(
    actaId: string,
    tipo: TipoResolucionBloqueoCierre,
  ): Observable<RegistrarResolucionBloqueoCierreAccionResponseDemo> {
    const params = new HttpParams().set('tipo', tipo);
    return this.http.post<RegistrarResolucionBloqueoCierreAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-resolucion-bloqueo-cierre`,
      null,
      { params },
    );
  }


  listarNotificacionesNotificadorMunicipal(): Observable<NotificadorMunicipalNotificacionDemo[]> {
    return this.http.get<NotificadorMunicipalNotificacionDemo[]>(
      `${this.api.baseUrl}/notificaciones/notificador-municipal`,
    );
  }


  listarNotificacionesCorreoListasParaLote(): Observable<CorreoPostalNotificacionListaDemo[]> {
    return this.http.get<CorreoPostalNotificacionListaDemo[]>(
      `${this.api.baseUrl}/notificaciones/correo/listas-para-lote`,
    );
  }

  listarLotesCorreoGenerados(): Observable<CorreoLoteResumenDemo[]> {
    return this.http.get<CorreoLoteResumenDemo[]>(`${this.api.baseUrl}/notificaciones/correo/lotes`);
  }

  anularLoteCorreoPostalDemo(loteId: string): Observable<AnularLoteCorreoResultadoDemo> {
    return this.http.post<AnularLoteCorreoResultadoDemo>(
      `${this.api.baseUrl}/notificaciones/correo/lotes/${encodeURIComponent(loteId)}/anular`,
      null,
    );
  }

  enviarIndividualCorreoPostalDemo(
    notificacionId: string,
  ): Observable<EnviarIndividualCorreoResultadoDemo> {
    return this.http.post<EnviarIndividualCorreoResultadoDemo>(
      `${this.api.baseUrl}/notificaciones/correo/${encodeURIComponent(notificacionId)}/enviar-individual`,
      null,
    );
  }

  generarLoteCorreoPostalDemo(
    tipo?: string,
    notificacionIds?: string[],
  ): Observable<GenerarLoteCorreoResultadoDemo> {
    const params = tipo ? new HttpParams().set('tipo', tipo) : undefined;
    const body =
      notificacionIds && notificacionIds.length > 0
        ? { tipo, notificacionIds }
        : null;
    return this.http.post<GenerarLoteCorreoResultadoDemo>(
      `${this.api.baseUrl}/notificaciones/correo/lotes/generar`,
      body,
      params ? { params } : {},
    );
  }

  buscarTrazabilidadCorreoPostal(acta: string): Observable<CorreoPostalTrazabilidadDemo[]> {
    const params = new HttpParams().set('acta', acta.trim());
    return this.http.get<CorreoPostalTrazabilidadDemo[]>(
      `${this.api.baseUrl}/notificaciones/correo/trazabilidad`,
      { params },
    );
  }

  procesarRespuestaCorreoPostalDemo(loteId?: string): Observable<ProcesarRespuestaCorreoResultadoDemo> {
    const params = loteId ? new HttpParams().set('loteId', loteId) : undefined;
    return this.http.post<ProcesarRespuestaCorreoResultadoDemo>(
      `${this.api.baseUrl}/notificaciones/correo/respuestas/procesar-demo`,
      null,
      params ? { params } : {},
    );
  }

  registrarAcuseNotificadorMunicipal(
    notificacionId: string,
    body: NotificadorMunicipalAcuseRequestDemo,
  ): Observable<NotificadorMunicipalAcuseResponseDemo> {
    return this.http.post<NotificadorMunicipalAcuseResponseDemo>(
      `${this.api.baseUrl}/notificaciones/notificador-municipal/${encodeURIComponent(notificacionId)}/acuse`,
      body,
    );
  }

  registrarNotificacionPositiva(
    actaId: string,
  ): Observable<RegistrarNotificacionPositivaAccionResponseDemo> {
    return this.http.post<RegistrarNotificacionPositivaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-notificacion-positiva`,
      null,
    );
  }

  registrarNotificacionNegativa(
    actaId: string,
  ): Observable<RegistrarNotificacionNegativaAccionResponseDemo> {
    return this.http.post<RegistrarNotificacionNegativaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-notificacion-negativa`,
      null,
    );
  }

  registrarNotificacionVencida(
    actaId: string,
  ): Observable<RegistrarNotificacionVencidaAccionResponseDemo> {
    return this.http.post<RegistrarNotificacionVencidaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-notificacion-vencida`,
      null,
    );
  }

  reintentarNotificacion(
    actaId: string,
  ): Observable<ReintentarNotificacionAccionResponseDemo> {
    return this.http.post<ReintentarNotificacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/reintentar-notificacion`,
      null,
    );
  }

  registrarSolicitudPagoVoluntario(
    actaId: string,
    body: RegistrarSolicitudPagoVoluntarioAccionRequest,
  ): Observable<RegistrarSolicitudPagoVoluntarioAccionResponseDemo> {
    return this.http.post<RegistrarSolicitudPagoVoluntarioAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-solicitud-pago-voluntario`,
      body,
    );
  }

  fijarMontoPagoVoluntario(
    actaId: string,
    body: RegistrarSolicitudPagoVoluntarioAccionRequest,
  ): Observable<RegistrarSolicitudPagoVoluntarioAccionResponseDemo> {
    return this.http.post<RegistrarSolicitudPagoVoluntarioAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/fijar-monto-pago-voluntario`,
      body,
    );
  }

  registrarVencimientoPagoVoluntario(
    actaId: string,
  ): Observable<RegistrarVencimientoPagoVoluntarioAccionResponseDemo> {
    return this.http.post<RegistrarVencimientoPagoVoluntarioAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-vencimiento-pago-voluntario`,
      null,
    );
  }

  registrarPagoInformado(
    actaId: string,
  ): Observable<RegistrarPagoInformadoAccionResponseDemo> {
    return this.http.post<RegistrarPagoInformadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-pago-informado`,
      null,
    );
  }

  adjuntarComprobantePagoInformado(
    actaId: string,
    nombreArchivo?: string,
  ): Observable<AdjuntarComprobantePagoInformadoAccionResponseDemo> {
    let params = new HttpParams();
    if (nombreArchivo && nombreArchivo.trim().length > 0) {
      params = params.set('nombreArchivo', nombreArchivo);
    }
    return this.http.post<AdjuntarComprobantePagoInformadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/adjuntar-comprobante-pago-informado`,
      null,
      { params },
    );
  }

  confirmarPagoInformado(
    actaId: string,
  ): Observable<ConfirmarPagoInformadoAccionResponseDemo> {
    return this.http.post<ConfirmarPagoInformadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/confirmar-pago-informado`,
      null,
    );
  }

  observarPagoInformado(
    actaId: string,
  ): Observable<ObservarPagoInformadoAccionResponseDemo> {
    return this.http.post<ObservarPagoInformadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/observar-pago-informado`,
      null,
    );
  }

  informarPagoCondena(actaId: string): Observable<PagoCondenaAccionResponseDemo> {
    return this.http.post<PagoCondenaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/informar-pago-condena`,
      null,
    );
  }

  confirmarPagoCondena(actaId: string): Observable<PagoCondenaAccionResponseDemo> {
    return this.http.post<PagoCondenaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/confirmar-pago-condena`,
      null,
    );
  }

  observarPagoCondena(actaId: string): Observable<PagoCondenaAccionResponseDemo> {
    return this.http.post<PagoCondenaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/observar-pago-condena`,
      null,
    );
  }

  archivarActa(actaId: string): Observable<ArchivarActaAccionResponseDemo> {
    return this.http.post<ArchivarActaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/archivar-acta`,
      null,
    );
  }

  reingresarActa(actaId: string): Observable<ReingresarActaAccionResponseDemo> {
    return this.http.post<ReingresarActaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/reingresar-acta`,
      null,
    );
  }


  enviarANotificacion(actaId: string): Observable<EnviarANotificacionAccionResponseDemo> {
    return this.http.post<EnviarANotificacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/enviar-a-notificacion`,
      null,
    );
  }

  anularActa(actaId: string): Observable<AnularActaPorNulidadAccionResponseDemo> {
    return this.http.post<AnularActaPorNulidadAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/anular-acta`,
      null,
    );
  }

  reactivarActa(actaId: string): Observable<ReactivarActaAccionResponseDemo> {
    return this.http.post<ReactivarActaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/reactivar-acta`,
      null,
    );
  }

  derivarAApremio(
    actaId: string,
  ): Observable<DerivarAGestionExternaAccionResponseDemo> {
    return this.http.post<DerivarAGestionExternaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/derivar-a-apremio`,
      null,
    );
  }

  derivarAJuzgadoDePaz(
    actaId: string,
  ): Observable<DerivarAGestionExternaAccionResponseDemo> {
    return this.http.post<DerivarAGestionExternaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/derivar-a-juzgado-de-paz`,
      null,
    );
  }

  reingresarDesdeGestionExterna(
    actaId: string,
  ): Observable<ReingresarDesdeGestionExternaAccionResponseDemo> {
    return this.http.post<ReingresarDesdeGestionExternaAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/reingresar-desde-gestion-externa`,
      null,
    );
  }

  apremioReingresarSinPago(
    actaId: string,
  ): Observable<ReingresarDesdeApremioSinPagoAccionResponseDemo> {
    return this.http.post<ReingresarDesdeApremioSinPagoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/apremio-reingresar-sin-pago`,
      null,
    );
  }

  apremioRegistrarPago(
    actaId: string,
  ): Observable<RegistrarPagoEnApremioAccionResponseDemo> {
    return this.http.post<RegistrarPagoEnApremioAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/apremio-registrar-pago`,
      null,
    );
  }

  juzgadoReingresarAbsuelto(
    actaId: string,
  ): Observable<RegistrarResolucionJuzgadoAccionResponseDemo> {
    return this.http.post<RegistrarResolucionJuzgadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/juzgado-reingresar-absuelto`,
      null,
    );
  }

  juzgadoReingresarCondenaConfirmada(
    actaId: string,
  ): Observable<RegistrarResolucionJuzgadoAccionResponseDemo> {
    return this.http.post<RegistrarResolucionJuzgadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/juzgado-reingresar-condena-confirmada`,
      null,
    );
  }

  juzgadoReingresarMontoModificado(
    actaId: string,
    nuevoMonto: number,
  ): Observable<RegistrarResolucionJuzgadoAccionResponseDemo> {
    return this.http.post<RegistrarResolucionJuzgadoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/juzgado-reingresar-monto-modificado`,
      { nuevoMonto },
    );
  }
  registrarApelacion(
    actaId: string,
    body: RegistrarApelacionAccionRequestDemo,
  ): Observable<RegistrarApelacionAccionResponseDemo> {
    return this.http.post<RegistrarApelacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-apelacion`,
      body,
    );
  }


  registrarVencimientoPlazoApelacion(
    actaId: string,
  ): Observable<RegistrarVencimientoPlazoApelacionAccionResponseDemo> {
    return this.http.post<RegistrarVencimientoPlazoApelacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/registrar-vencimiento-plazo-apelacion`,
      null,
    );
  }

    resolverApelacion(
    actaId: string,
    body: ResolverApelacionAccionRequestDemo,
  ): Observable<ResolverApelacionAccionResponseDemo> {
    return this.http.post<ResolverApelacionAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/resolver-apelacion`,
      body,
    );
  }

  dictarFalloAbsolutorio(actaId: string): Observable<DictarFalloAccionResponseDemo> {
    return this.http.post<DictarFalloAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/dictar-fallo-absolutorio`,
      null,
    );
  }

  dictarFalloCondenatorio(
    actaId: string,
    body: DictarFalloCondenatorioAccionRequestDemo,
  ): Observable<DictarFalloAccionResponseDemo> {
    return this.http.post<DictarFalloAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/dictar-fallo-condenatorio`,
      body,
    );
  }
  consentirCondenaYRegistrarPago(
    actaId: string,
  ): Observable<ConsentirCondenaYRegistrarPagoAccionResponseDemo> {
    return this.http.post<ConsentirCondenaYRegistrarPagoAccionResponseDemo>(
      `${this.api.baseUrl}/actas/${actaId}/acciones/consentir-condena-y-registrar-pago`,
      null,
    );
  }

  buscarActasGlobal(q: string): Observable<PrototipoActaBusquedaResponse[]> {
    const params = new HttpParams().set('q', q.trim());
    return this.http.get<PrototipoActaBusquedaResponse[]>(`${this.api.baseUrl}/actas/buscar`, { params });
  }
}





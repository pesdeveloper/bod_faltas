import { Component, DestroyRef, ElementRef, NgZone, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Observable, Subject, catchError, debounceTime, finalize, forkJoin, map, of, take } from 'rxjs';
import {
  BandejaLateralCodigo,
  BandejaLateralResponse,
  bandejaBackendALateral,
  esBandejaCodigoDemo,
  esBandejaLateralCodigo,
  etiquetaBandeja,
} from '../../core/constants/bandejas-demo.constants';
import {
  aplicarFiltroOperativoLateral,
  codigosBandejaEnResumen,
  filtrosOperativosVisibles,
  filtrarBandejasLateralVisibles,
  subBandejaBackendParaConsulta,
  transformarBandejasParaLateral,
} from '../../core/services/bandeja-lateral.presenter';
import {
  AccionPagoCondenaDemo,
  AccionPagoVoluntarioDemo,
  ActaBandejaItem,
  ActaDetalleDemo,
  ActaResumenDemo,
  BandejaResponse,
  SubBandejaResumen,
  BadgeDemo,
  BandejaCodigo,
  CrearActaMockDemoRequest,
  DependenciaActaDemo,
  DocumentoActaDemo,
  EventoActaDemo,
  HechosMaterialesEjeDemo,
  ResultadoNotificacionDemo,
  SituacionPagoCondenaDemo,
  SituacionPagoDemo,
  TipoCumplimientoMaterialBloqueante,
  TipoGestionExternaDemo,
  TipoResolucionBloqueoCierre,
  codigoQrDemoParaActa,
  MotivoParalizacionDemo,
  ParalizarActaRequestDemo,
  ParalizarActaAccionResponseDemo,
  PrototipoActaBusquedaResponse,
} from '../../core/models/prototipo-faltas.models';
import { badgesDesdeActaResumen } from '../../core/services/acta-badges.presenter';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';
import { DemoActaListComponent } from './demo-acta-list.component';
import { DemoActaNotificacionesComponent } from './demo-acta-notificaciones.component';
import { DemoCorreoPostalPageComponent } from './demo-correo-postal-page.component';

type CargaEstado = 'idle' | 'loading' | 'ready' | 'error';

const ESTADO_DOCUMENTO_PENDIENTE_FIRMA = 'PENDIENTE_FIRMA';
const BANDEJA_REDACCION = 'PENDIENTES_RESOLUCION_REDACCION';
const BANDEJA_PENDIENTE_FIRMA = 'PENDIENTE_FIRMA';
const PIEZA_NULIDAD = 'NULIDAD';
const PIEZA_MEDIDA_PREVENTIVA = 'MEDIDA_PREVENTIVA';
const PIEZA_NOTIFICACION_ACTA = 'NOTIFICACION_ACTA';
const PIEZA_RESOLUCION = 'RESOLUCION';
const PIEZA_RECTIFICACION = 'RECTIFICACION';
const BANDEJA_PENDIENTE_ANALISIS = 'PENDIENTE_ANALISIS';
const TIPOS_DOCUMENTO_FALLO: ReadonlyArray<string> = ['FALLO_ABSOLUTORIO', 'FALLO_CONDENATORIO'];
const EVENTOS_FALLO_DICTADO: ReadonlyArray<string> = [
  'FALLO_ABSOLUTORIO_DICTADO',
  'FALLO_CONDENATORIO_DICTADO',
];
const RESULTADOS_FINAL_SIN_PAGO_VOLUNTARIO: ReadonlyArray<string> = [
  'ABSUELTO',
  'CONDENADO',
  'CONDENA_FIRME',
  'PAGO_CONFIRMADO',
];

const ETIQUETA_DEPENDENCIA_DEMO: Record<string, string> = {
  TRANSITO: 'Tránsito',
  INSPECCIONES: 'Inspecciones',
  FISCALIZACION: 'Fiscalización',
  BROMATOLOGIA: 'Bromatología',
};

const SIN_DEPENDENCIA_RESUMEN = '__SIN_DEPENDENCIA__';

const CODIGOS_DEPENDENCIA_RESUMEN: readonly DependenciaActaDemo[] = [
  'TRANSITO',
  'INSPECCIONES',
  'FISCALIZACION',
  'BROMATOLOGIA',
];

interface DependenciaResumenFila {
  codigo: string;
  label: string;
  cantidad: number;
}
type TipoFalloDemo = 'ABSOLUTORIO' | 'CONDENATORIO';

const TIPOS_CUMPLIMIENTO_MATERIAL: ReadonlyArray<TipoCumplimientoMaterialBloqueante> = [
  'LEVANTAMIENTO_MEDIDA_PREVENTIVA',
  'LIBERACION_RODADO',
  'ENTREGA_DOCUMENTACION',
];

const ETIQUETA_CUMPLIMIENTO_MATERIAL: Record<TipoCumplimientoMaterialBloqueante, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Registrar levantamiento efectivo de medida preventiva',
  LIBERACION_RODADO: 'Registrar retiro (app)',
  ENTREGA_DOCUMENTACION: 'Registrar entrega efectiva de documentación',
};

// Fases del eje material reportadas por el backend (HechosMaterialesEje.fase).
// La etapa SITUACION_PENDIENTE_DE_RESOLUTORIO exige documentar el resolutorio
// antes que cualquier registro de cumplimiento material efectivo.
const FASE_EJE_PENDIENTE_RESOLUTORIO = 'SITUACION_PENDIENTE_DE_RESOLUTORIO';
const FASE_EJE_RESOLUTORIO_EN_EXPEDIENTE = 'RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL';

const ETIQUETA_RESOLUCION_BLOQUEO_CIERRE: Record<TipoResolucionBloqueoCierre, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Generar resolución de levantamiento de medida preventiva',
  LIBERACION_RODADO: 'Generar resolución de liberación de rodado',
  ENTREGA_DOCUMENTACION: 'Generar resolución de restitución de documentación',
};

const ETIQUETA_RESOLUCION_BLOQUEO_CIERRE_EN_CURSO: Record<TipoResolucionBloqueoCierre, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Generando resolución...',
  LIBERACION_RODADO: 'Generando resolución...',
  ENTREGA_DOCUMENTACION: 'Generando resolución...',
};

const BANDEJAS_NOTIFICABLES: ReadonlyArray<string> = [
  'NOTIFICACIONES',
  'PENDIENTE_NOTIFICACION',
  'EN_NOTIFICACION',
];

// Bandejas terminales o no operables para acciones internas (pago, cierre,
// cumplimiento material). No aplica como apagado global: ARCHIVO conserva
// reingreso y GESTION_EXTERNA conserva retorno.
const BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS: ReadonlyArray<BandejaCodigo> = [
  'CERRADAS',
  'ARCHIVO',
  'GESTION_EXTERNA',
  'PARALIZADAS',
];

const MOTIVOS_PARALIZACION: ReadonlyArray<{ valor: MotivoParalizacionDemo; etiqueta: string }> = [
  { valor: 'ESPERA_DOCUMENTAL', etiqueta: 'Espera documental' },
  { valor: 'ESPERA_INFORME_EXTERNO', etiqueta: 'Espera informe externo' },
  { valor: 'ESPERA_OTRA_DEPENDENCIA', etiqueta: 'Espera otra dependencia' },
  { valor: 'ESPERA_RESOLUCION_RELACIONADA', etiqueta: 'Espera resolucion relacionada' },
  { valor: 'OTRO', etiqueta: 'Otro' },
];

const ETIQUETA_RESULTADO_NOTIFICACION: Record<ResultadoNotificacionDemo, string> = {
  POSITIVA: 'Registrar notificacion positiva',
  NEGATIVA: 'Registrar notificacion negativa',
  VENCIDA: 'Registrar notificacion vencida',
};

const ETIQUETA_ACCION_PAGO: Record<AccionPagoVoluntarioDemo, string> = {
  SOLICITAR: 'Pago voluntario',
  FIJAR_MONTO: 'Fijar monto pago voluntario',
  INFORMAR: 'Informar pago',
  ADJUNTAR_COMPROBANTE: 'Adjuntar comprobante mock',
  CONFIRMAR: 'Confirmar pago',
  OBSERVAR: 'Observar pago',
};

const ETIQUETA_ACCION_PAGO_EN_CURSO: Record<AccionPagoVoluntarioDemo, string> = {
  SOLICITAR: 'Registrando...',
  FIJAR_MONTO: 'Fijando monto...',
  INFORMAR: 'Informando...',
  ADJUNTAR_COMPROBANTE: 'Adjuntando...',
  CONFIRMAR: 'Confirmando...',
  OBSERVAR: 'Observando...',
};

const ETIQUETA_SITUACION_PAGO_CONDENA: Record<SituacionPagoCondenaDemo, string> = {
  NO_APLICA: 'No aplica',
  PENDIENTE: 'Pendiente',
  INFORMADO: 'Informado',
  CONFIRMADO: 'Confirmado',
  OBSERVADO: 'Observado',
};

const ETIQUETA_ACCION_PAGO_CONDENA: Record<AccionPagoCondenaDemo, string> = {
  INFORMAR: 'Informar pago de condena',
  CONFIRMAR: 'Confirmar acreditacion',
  OBSERVAR: 'Observar acreditacion',
};

const ETIQUETA_ACCION_PAGO_CONDENA_EN_CURSO: Record<AccionPagoCondenaDemo, string> = {
  INFORMAR: 'Informando...',
  CONFIRMAR: 'Confirmando...',
  OBSERVAR: 'Observando...',
};

const SITUACIONES_PAGO_CONOCIDAS: ReadonlyArray<SituacionPagoDemo> = [
  'SIN_PAGO',
  'SOLICITADO',
  'PAGO_INFORMADO',
  'PENDIENTE_CONFIRMACION',
  'CONFIRMADO',
  'OBSERVADO',
  'VENCIDO',
];

const SITUACIONES_PAGO_CONDENA_CONOCIDAS: ReadonlyArray<SituacionPagoCondenaDemo> = [
  'NO_APLICA',
  'PENDIENTE',
  'INFORMADO',
  'CONFIRMADO',
  'OBSERVADO',
];

// Conjunto de acciones de pago voluntario que conoce la UI. Sirve solo
// como filtro defensivo sobre la lista enviada por el backend; la
// disponibilidad real (precondiciones de bandeja + situacionPago) la
// computa el backend en accionesPagoVoluntarioDisponibles.
const ACCIONES_PAGO_CONOCIDAS: ReadonlyArray<AccionPagoVoluntarioDemo> = [
  'SOLICITAR',
  'FIJAR_MONTO',
  'INFORMAR',
  'ADJUNTAR_COMPROBANTE',
  'CONFIRMAR',
  'OBSERVAR',
];

const ACCIONES_PAGO_CONDENA_CONOCIDAS: ReadonlyArray<AccionPagoCondenaDemo> = [
  'INFORMAR',
  'CONFIRMAR',
  'OBSERVAR',
];

const NOMBRE_ARCHIVO_COMPROBANTE_MOCK = 'comprobante-mock.pdf';

const BANDEJA_ABREVIATURAS: Record<BandejaCodigo, string> = {
  LABRADAS: 'LAB',
  ACTAS_EN_ENRIQUECIMIENTO: 'ENR',
  PENDIENTE_ANALISIS: 'ANA',
  PENDIENTES_RESOLUCION_REDACCION: 'RED',
  PENDIENTES_FALLO: 'FAL',
  PENDIENTE_FIRMA: 'FIR',
  NOTIFICACIONES: 'NOT',
  PENDIENTE_NOTIFICACION: 'NOT',
  EN_NOTIFICACION: 'ENV',
  CON_APELACION: 'APE',
  GESTION_EXTERNA: 'EXT',
  PARALIZADAS: 'PAR',
  ARCHIVO: 'ARC',
  CERRADAS: 'CER',
  PENDIENTE_PREPARACION_DOCUMENTAL: 'DOC',
};

const BANDEJA_ICONOS: Record<BandejaCodigo, string> = {
  LABRADAS: 'description',
  ACTAS_EN_ENRIQUECIMIENTO: 'playlist_add_check',
  PENDIENTE_ANALISIS: 'manage_search',
  PENDIENTES_RESOLUCION_REDACCION: 'edit_note',
  PENDIENTES_FALLO: 'gavel',
  PENDIENTE_FIRMA: 'draw',
  NOTIFICACIONES: 'outgoing_mail',
  PENDIENTE_NOTIFICACION: 'outgoing_mail',
  EN_NOTIFICACION: 'local_shipping',
  CON_APELACION: 'balance',
  GESTION_EXTERNA: 'account_balance',
  PARALIZADAS: 'pause_circle',
  ARCHIVO: 'inventory_2',
  CERRADAS: 'task_alt',
  PENDIENTE_PREPARACION_DOCUMENTAL: 'edit_document',
};

const BANDEJA_ICONOS_LOOKUP: Record<string, string> = BANDEJA_ICONOS;

const STORAGE_SEGUIR_ACTA_AL_CAMBIAR_BANDEJA = 'faltas.demo.seguir-acta-al-cambiar-bandeja';

interface AccionConBandejaResponseLike {
  actaId?: string;
  bandejaActual?: string;
}

const ETIQUETA_BADGE_COMPACTA: Record<string, string> = {
  SIN_PAGO: 'Sin pago',
  SOLICITADO: 'Pago voluntario habilitado',
  PAGO_INFORMADO: 'Pago informado',
  PENDIENTE_CONFIRMACION: 'Pendiente confirmación',
  CONFIRMADO: 'Pago confirmado',
  OBSERVADO: 'Pago observado',
  VENCIDO: 'Pago voluntario vencido',
  SIN_RESULTADO_FINAL: 'Sin resultado final',
  FALTA_RESULTADO_FINAL: 'Falta resultado final',
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Levantamiento medida',
  ENTREGA_DOCUMENTACION: 'Entrega documentación',
  ENTREGA_DOC: 'Entrega documentación',
  LIBERACION_RODADO: 'Liberación rodado',
  APREMIO: 'Apremio',
  JUZGADO_DE_PAZ: 'Juzgado de paz',
};

// Etiquetas UX para acciones de gestion externa. Los valores APREMIO /
// JUZGADO_DE_PAZ mapean 1 a 1 con los endpoints reales del backend; el
// caso RETORNAR es la accion de retorno desde GESTION_EXTERNA.
const ETIQUETA_DERIVACION_GESTION_EXTERNA: Record<TipoGestionExternaDemo, string> = {
  APREMIO: 'Derivar a apremio',
  JUZGADO_DE_PAZ: 'Derivar a juzgado de paz',
};

const ETIQUETA_DERIVACION_GESTION_EXTERNA_EN_CURSO: Record<TipoGestionExternaDemo, string> = {
  APREMIO: 'Derivando a apremio...',
  JUZGADO_DE_PAZ: 'Derivando a juzgado de paz...',
};

@Component({
  selector: 'app-demo-shell',
  standalone: true,
  imports: [
    FormsModule,
    MatSidenavModule,
    MatToolbarModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatProgressSpinnerModule,
    DemoActaListComponent,
    DemoCorreoPostalPageComponent,
    DemoActaNotificacionesComponent,
  ],
  templateUrl: './demo-shell.component.html',
  styleUrl: './demo-shell.component.scss',
})
export class DemoShellComponent implements OnInit {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ngZone = inject(NgZone);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private restoringFromUrl = true;
  private actaToRestoreFromUrl: string | null = null;
  @ViewChild(DemoActaListComponent) private readonly actaListComponent?: DemoActaListComponent;
  @ViewChild('actaDetailContainer') private readonly actaDetailContainer?: ElementRef<HTMLElement>;

  readonly etiquetaBandeja = etiquetaBandeja;
  readonly bandejasColapsadas = signal<boolean>(false);
  readonly bandejasResumen = signal<BandejaLateralResponse[]>([]);
  /** Solo bandejas UX visibles en el menú lateral (nunca códigos backend absorbidos). */
  readonly bandejasLateral = signal<BandejaLateralResponse[]>([]);
  private readonly bandejasBackendCodigos = signal<ReadonlySet<string>>(new Set());
  readonly enCorreoPostal = signal(false);
  readonly enCorreoPostalActas = signal(false);
  readonly enCorreoPostalLotes = signal(false);
  readonly enNotificadorMunicipal = signal(false);

  readonly bandejaSeleccionada = signal<BandejaLateralCodigo>('ACTAS_EN_ENRIQUECIMIENTO');
  readonly subBandejaSeleccionada = signal<string | null>(null);
  readonly actas = signal<ActaBandejaItem[]>([]);
  readonly actaSeleccionadaId = signal<string | null>(null);
  private static readonly BREAKPOINT_VISTA_ESTRECHA = '(max-width: 959.98px)';
  readonly vistaEstrecha = signal(false);
  readonly detalleMovilAbierto = signal(false);
  readonly overlayResumenBandejaAbierto = signal(false);
  readonly detalle = signal<ActaDetalleDemo | null>(null);

  readonly documentos = signal<DocumentoActaDemo[]>([]);
  readonly documentosEstado = signal<CargaEstado>('idle');
  readonly documentosError = signal<string | null>(null);

  readonly eventos = signal<EventoActaDemo[]>([]);
  readonly eventosEstado = signal<CargaEstado>('idle');
  readonly eventosError = signal<string | null>(null);

  readonly listadoEstado = signal<CargaEstado>('idle');
  readonly listadoError = signal<string | null>(null);
  readonly detalleEstado = signal<CargaEstado>('idle');
  readonly detalleError = signal<string | null>(null);

  readonly creandoDemo = signal<DependenciaActaDemo | null>(null);
  readonly altaDemoError = signal<string | null>(null);
  readonly altaDemoMensaje = signal<string | null>(null);

  readonly firmandoDocumentoId = signal<string | null>(null);
  readonly firmaDocumentoError = signal<string | null>(null);
  readonly firmaDocumentoMensaje = signal<string | null>(null);

  readonly cumpliendoMaterial = signal<TipoCumplimientoMaterialBloqueante | null>(null);
  readonly cumplimientoMaterialError = signal<string | null>(null);
  readonly cumplimientoMaterialMensaje = signal<string | null>(null);

  readonly resolviendoBloqueoCierre = signal<TipoResolucionBloqueoCierre | null>(null);
  readonly resolucionBloqueoCierreError = signal<string | null>(null);
  readonly resolucionBloqueoCierreMensaje = signal<string | null>(null);

  readonly cerrandoActa = signal<boolean>(false);
  readonly cierreActaError = signal<string | null>(null);
  readonly cierreActaMensaje = signal<string | null>(null);

  readonly notificandoResultado = signal<ResultadoNotificacionDemo | null>(null);
  readonly reintentandoNotificacion = signal<boolean>(false);
  readonly notificacionError = signal<string | null>(null);
  readonly notificacionMensaje = signal<string | null>(null);

  readonly registrandoApelacion = signal<boolean>(false);
  readonly apelacionError = signal<string | null>(null);
  readonly apelacionMensaje = signal<string | null>(null);

  readonly registrandoVencimientoPlazoApelacion = signal<boolean>(false);
  readonly vencimientoPlazoApelacionError = signal<string | null>(null);
  readonly vencimientoPlazoApelacionMensaje = signal<string | null>(null);
  readonly ejecutandoConsentirCondenaYRegistrarPago = signal<boolean>(false);
  readonly consentirCondenaYRegistrarPagoError = signal<string | null>(null);
  readonly consentirCondenaYRegistrarPagoMensaje = signal<string | null>(null);

  readonly resolviendoApelacion = signal<boolean>(false);
  readonly resolucionApelacionError = signal<string | null>(null);
  readonly resolucionApelacionMensaje = signal<string | null>(null);

  readonly ejecutandoPagoAccion = signal<AccionPagoVoluntarioDemo | null>(null);
  readonly registrandoVencimientoPagoVoluntario = signal<boolean>(false);
  readonly pagoError = signal<string | null>(null);
  readonly pagoMensaje = signal<string | null>(null);

  readonly ejecutandoPagoCondenaAccion = signal<AccionPagoCondenaDemo | null>(null);
  readonly pagoCondenaError = signal<string | null>(null);
  readonly pagoCondenaMensaje = signal<string | null>(null);

  // Monto del acta a fijar al ejecutar la accion administrativa
  // "Pago voluntario" (SOLICITAR). Se mantiene como string en la UI
  // para no perder precision al renderizar; se parsea a number en el
  // momento de invocar el backend.
  readonly montoPagoVoluntarioInput = signal<string>('');

  readonly montoCondenaInput = signal<string>('');

  readonly ejecutandoArchivoAccion = signal<'ARCHIVAR' | 'REINGRESAR' | null>(null);
  readonly archivoError = signal<string | null>(null);
  readonly archivoMensaje = signal<string | null>(null);

  readonly ejecutandoEnriquecimientoAccion = signal<'ENVIAR_NOTIFICACION' | 'ANULAR_ACTA' | null>(null);
  readonly enriquecimientoMensaje = signal<string | null>(null);
  readonly enriquecimientoError = signal<string | null>(null);

  // Accion de gestion externa en curso. APREMIO/JUZGADO_DE_PAZ corresponden
  // a las dos derivaciones tipadas del backend; RETORNAR corresponde al
  // reingreso desde GESTION_EXTERNA. Solo una accion puede estar en curso
  // a la vez para evitar conflictos en el detalle/listado refrescados.
  readonly ejecutandoGestionExternaAccion = signal<TipoGestionExternaDemo | 'RETORNAR' | 'APREMIO_SIN_PAGO' | 'APREMIO_PAGO' | 'JUZGADO_ABSUELTO' | 'JUZGADO_CONDENA' | 'JUZGADO_MONTO' | null>(null);
  readonly nuevoMontoJuzgado = signal<string>('');
  readonly gestionExternaError = signal<string | null>(null);
  readonly gestionExternaMensaje = signal<string | null>(null);

  readonly ejecutandoReactivacionAccion = signal<boolean>(false);
  readonly reactivacionError = signal<string | null>(null);
  readonly reactivacionMensaje = signal<string | null>(null);

  readonly ejecutandoParalizarAccion = signal<boolean>(false);
  readonly paralizarError = signal<string | null>(null);
  readonly paralizarMensaje = signal<string | null>(null);
  readonly paralizarFormAbierto = signal<boolean>(false);
  readonly paralizarMotivoSeleccionado = signal<string>('');
  readonly paralizarObservacion = signal<string>('');
  readonly motivosParalizacion = MOTIVOS_PARALIZACION;

  readonly generandoNulidad = signal<boolean>(false);
  readonly nulidadError = signal<string | null>(null);
  readonly nulidadMensaje = signal<string | null>(null);

  readonly generandoMedidaPreventiva = signal<boolean>(false);
  readonly medidaPreventivaError = signal<string | null>(null);
  readonly medidaPreventivaMensaje = signal<string | null>(null);

  readonly generandoNotificacionActa = signal<boolean>(false);
  readonly notificacionActaError = signal<string | null>(null);
  readonly notificacionActaMensaje = signal<string | null>(null);

  readonly generandoResolucion = signal<boolean>(false);
  readonly resolucionPiezaError = signal<string | null>(null);
  readonly resolucionPiezaMensaje = signal<string | null>(null);

  readonly generandoRectificacion = signal<boolean>(false);
  readonly rectificacionPiezaError = signal<string | null>(null);
  readonly rectificacionPiezaMensaje = signal<string | null>(null);

  readonly dictandoFallo = signal<TipoFalloDemo | null>(null);
  readonly falloError = signal<string | null>(null);
  readonly falloMensaje = signal<string | null>(null);

  readonly textoBusquedaActa = signal('');
  readonly dependenciaDemoSeleccionada = signal<string | null>(null);
  readonly seguirActaAlCambiarBandeja = signal(false);
  readonly busquedaGlobal = signal(false);
  readonly resultadosBusquedaGlobal = signal<PrototipoActaBusquedaResponse[]>([]);
  readonly cargandoBusquedaGlobal = signal(false);
  readonly errorBusquedaGlobal = signal<string | null>(null);
  readonly actaPreviewGlobal = signal<PrototipoActaBusquedaResponse | null>(null);
  private readonly busquedaGlobalSubject = new Subject<string>();
  readonly seguimientoActaMensaje = signal<string | null>(null);

  readonly copiaEstadoMensaje = signal<string | null>(null);
  readonly copiaEstadoError = signal<string | null>(null);
  private static readonly SCROLL_VISIBILITY_MARGIN_PX = 8;
  private static readonly SCROLL_VERIFY_DELAY_MS = 150;

  private pendingScrollToActaId: string | null = null;
  private pendingScrollRetryDisponible = false;
  private scrollVerifyTimeoutId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.inicializarDeteccionVistaEstrecha();
    this.restaurarPreferenciaSeguirActa();
    this.leerYAplicarQueryParamsIniciales();
    if (this.actaToRestoreFromUrl) {
      this.actaSeleccionadaId.set(this.actaToRestoreFromUrl);
      this.cargarDetalle();
    }
    this.actualizarVistaCorreoPostal(this.router.url);
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => this.actualizarVistaCorreoPostal(event.urlAfterRedirects));
    this.busquedaGlobalSubject.pipe(
      debounceTime(350),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((q) => this.ejecutarBusquedaGlobal(q));

    this.cargarBandejasResumen();
    this.cargarListado({ actaIdScrollObjetivo: this.actaToRestoreFromUrl });
    Promise.resolve().then(() => {
      this.restoringFromUrl = false;
      if (this.busquedaGlobal() && this.textoBusquedaActa().trim()) {
        this.ejecutarBusquedaGlobal(this.textoBusquedaActa());
      }
    });
  }


  ngAfterViewInit(): void {}

  resumenBandejaInfoDeshabilitado(): boolean {
    if (this.vistaEstrecha()) {
      return this.actaSeleccionadaId() !== null;
    }
    return this.actaSeleccionadaId() === null;
  }

  private inicializarDeteccionVistaEstrecha(): void {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }

    const consulta = window.matchMedia(DemoShellComponent.BREAKPOINT_VISTA_ESTRECHA);
    const actualizar = (): void => {
      const estrecha = consulta.matches;
      this.vistaEstrecha.set(estrecha);
      if (!estrecha) {
        this.detalleMovilAbierto.set(false);
        this.overlayResumenBandejaAbierto.set(false);
      }
    };

    actualizar();
    consulta.addEventListener('change', actualizar);
    this.destroyRef.onDestroy(() => consulta.removeEventListener('change', actualizar));
  }

  private cerrarOverlaysMoviles(): void {
    this.detalleMovilAbierto.set(false);
    this.overlayResumenBandejaAbierto.set(false);
  }

  cerrarPanelMovil(): void {
    if (this.overlayResumenBandejaAbierto()) {
      this.overlayResumenBandejaAbierto.set(false);
      return;
    }
    this.cerrarDetalleMovil();
  }

  cerrarDetalleMovil(): void {
    this.detalleMovilAbierto.set(false);
  }

  actaEnSeguimiento(): ActaBandejaItem | null {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || !this.seguirActaAlCambiarBandeja()) {
      return null;
    }
    if (this.actas().some((acta) => acta.id === actaId)) {
      return null;
    }
    const det = this.detalle();
    if (!det || det.id !== actaId) {
      return null;
    }
    return this.actaBandejaDesdeDetalle(det);
  }

  actasFiltradas(): ActaBandejaItem[] {
    const dependencia = this.dependenciaDemoSeleccionada();
    const termino = this.textoBusquedaActa().trim().toLowerCase();
    let items = this.actas();
    if (dependencia) {
      if (dependencia === SIN_DEPENDENCIA_RESUMEN) {
        items = items.filter((acta) => !acta.dependenciaDemo?.trim());
      } else {
        items = items.filter((acta) => acta.dependenciaDemo === dependencia);
      }
    }
    if (!termino) {
      return items;
    }
    return items.filter((acta) => acta.numeroActa.trim().toLowerCase().includes(termino));
  }

  dependenciasDisponiblesEnListado(): { codigo: string; label: string }[] {
    const vistos = new Set<string>();
    const opciones: { codigo: string; label: string }[] = [];
    for (const acta of this.actas()) {
      const codigo = acta.dependenciaDemo?.trim();
      if (!codigo || vistos.has(codigo)) {
        continue;
      }
      vistos.add(codigo);
      opciones.push({ codigo, label: this.etiquetaDependenciaDemo(codigo) });
    }
    return opciones.sort((a, b) => a.label.localeCompare(b.label, 'es'));
  }

  actualizarTextoBusquedaActa(valor: string): void {
    this.textoBusquedaActa.set(valor);
    this.actualizarQueryParams({ q: valor || null }, true);
    if (this.busquedaGlobal()) {
      this.busquedaGlobalSubject.next(valor);
    }
  }

  cambiarBusquedaGlobal(activo: boolean): void {
    this.busquedaGlobal.set(activo);
    this.actualizarQueryParams({ busquedaGlobal: activo || null }, true);
    if (activo) {
      const q = this.textoBusquedaActa().trim();
      if (q) {
        this.ejecutarBusquedaGlobal(q);
      } else {
        this.resultadosBusquedaGlobal.set([]);
      }
    } else {
      this.resultadosBusquedaGlobal.set([]);
      this.errorBusquedaGlobal.set(null);
      this.actaPreviewGlobal.set(null);
    }
  }

  limpiarBusquedaGlobal(): void {
    this.textoBusquedaActa.set('');
    this.resultadosBusquedaGlobal.set([]);
    this.errorBusquedaGlobal.set(null);
    this.actaPreviewGlobal.set(null);
    this.actualizarQueryParams({ q: null }, true);
  }

  private ejecutarBusquedaGlobal(q: string): void {
    const termino = q.trim();
    if (!termino) {
      this.resultadosBusquedaGlobal.set([]);
      this.errorBusquedaGlobal.set(null);
      return;
    }
    this.cargandoBusquedaGlobal.set(true);
    this.errorBusquedaGlobal.set(null);
    this.api.buscarActasGlobal(termino)
      .pipe(
        catchError(() => {
          this.errorBusquedaGlobal.set('No se pudo realizar la búsqueda global.');
          return of<PrototipoActaBusquedaResponse[]>([]);
        }),
        finalize(() => this.cargandoBusquedaGlobal.set(false)),
        take(1),
      )
      .subscribe((resultados) => this.resultadosBusquedaGlobal.set(resultados));
  }

  seleccionarResultadoGlobal(resultado: PrototipoActaBusquedaResponse): void {
    const esBandejaActual = resultado.bandeja === this.bandejaSeleccionada();
    if (esBandejaActual) {
      this.actaPreviewGlobal.set(null);
      this.seleccionarActa(resultado.actaId);
    } else {
      this.actaPreviewGlobal.set(resultado);
      this.actaSeleccionadaId.set(resultado.actaId);
      this.actualizarQueryParams({ acta: resultado.actaId }, false);
      this.detalle.set(null);
      this.detalleEstado.set('loading');
      if (this.vistaEstrecha()) {
        this.detalleMovilAbierto.set(true);
        this.overlayResumenBandejaAbierto.set(false);
      }
      this.cargarDetalle();
    }
  }

  confirmarSeleccionActaGlobal(): void {
    const resultado = this.actaPreviewGlobal();
    if (!resultado) {
      return;
    }
    const bandejaDestino = bandejaBackendALateral(resultado.bandeja);
    this.actaPreviewGlobal.set(null);
    this.resultadosBusquedaGlobal.set([]);
    this.errorBusquedaGlobal.set(null);
    this.cargandoBusquedaGlobal.set(false);
    this.textoBusquedaActa.set('');
    this.bandejaSeleccionada.set(bandejaDestino);
    this.subBandejaSeleccionada.set(null);
    this.dependenciaDemoSeleccionada.set(null);
    this.actaSeleccionadaId.set(resultado.actaId);
    this.actualizarQueryParams({
      bandeja: bandejaDestino,
      filtro: null,
      dependencia: null,
      acta: resultado.actaId,
      q: null,
      busquedaGlobal: true,
    }, true);
    this.cargarListado({ actaIdScrollObjetivo: resultado.actaId });
    this.cargarDetalle();
  }

  actualizarDependenciaDemo(valor: string | null): void {
    this.dependenciaDemoSeleccionada.set(valor);
    this.actualizarQueryParams({ dependencia: valor }, true);
  }

  etiquetaFiltroOperativoActivo(): string {
    const subCodigo = this.subBandejaSeleccionada();
    if (!subCodigo) {
      return 'Todos';
    }
    const sub = this.subBandejasActuales().find((item) => item.codigo === subCodigo);
    return sub?.label ?? subCodigo;
  }

  etiquetaDependenciaActiva(): string {
    const dependencia = this.dependenciaDemoSeleccionada();
    if (!dependencia) {
      return 'Todas';
    }
    if (dependencia === SIN_DEPENDENCIA_RESUMEN) {
      return 'Sin dependencia';
    }
    return this.etiquetaDependenciaDemo(dependencia);
  }

  dependenciaResumenDeshabilitada(codigo: string): boolean {
    const fila = this.filasResumenPorDependencia().find((item) => item.codigo === codigo);
    return !fila || fila.cantidad <= 0;
  }

  filtroOperativoResumenDeshabilitado(subCodigo: string): boolean {
    const sub = this.subBandejasActuales().find((item) => item.codigo === subCodigo);
    return !sub || sub.cantidad <= 0;
  }

  cambiarSeguirActaAlCambiarBandeja(activo: boolean): void {
    this.seguirActaAlCambiarBandeja.set(activo);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_SEGUIR_ACTA_AL_CAMBIAR_BANDEJA, JSON.stringify(activo));
    }
    this.actualizarQueryParams({ seguirActa: activo || null }, true);
  }


  tituloVistaPrincipal(): string {
    if (this.enCorreoPostalActas()) {
      return 'Actas para correo postal';
    }
    if (this.enCorreoPostalLotes()) {
      return 'Lotes de correo postal';
    }
    const bandeja = this.bandejasResumen().find((item) => item.codigo === this.bandejaSeleccionada());
    const labelBandeja = bandeja?.label ?? etiquetaBandeja(this.bandejaSeleccionada());
    const subCodigo = this.subBandejaSeleccionada();
    if (!subCodigo) {
      return labelBandeja;
    }
    const subLabel =
      bandeja?.subBandejas.find((sub) => sub.codigo === subCodigo)?.label ?? subCodigo;
    return `${labelBandeja} › ${subLabel}`;
  }

  subBandejasVisibles(bandeja: BandejaLateralResponse): SubBandejaResumen[] {
    return filtrosOperativosVisibles(bandeja);
  }

  subBandejasActuales(): SubBandejaResumen[] {
    const bandeja = this.bandejasResumen().find((item) => item.codigo === this.bandejaSeleccionada());
    if (!bandeja) {
      return [];
    }
    return this.subBandejasVisibles(bandeja);
  }

  bandejaActivaResumen(): BandejaLateralResponse | null {
    return this.bandejasResumen().find((item) => item.codigo === this.bandejaSeleccionada()) ?? null;
  }

  labelBandejaActiva(): string {
    const bandeja = this.bandejaActivaResumen();
    return bandeja?.label ?? etiquetaBandeja(this.bandejaSeleccionada());
  }

  cantidadTotalBandejaActiva(): number {
    return this.bandejaActivaResumen()?.cantidad ?? 0;
  }

  conteosDependenciaEnListadoActual(): Map<string, number> {
    const conteos = new Map<string, number>();
    for (const acta of this.actas()) {
      const codigo = acta.dependenciaDemo?.trim();
      const key = codigo ? codigo : SIN_DEPENDENCIA_RESUMEN;
      conteos.set(key, (conteos.get(key) ?? 0) + 1);
    }
    return conteos;
  }

  filasResumenPorDependencia(): DependenciaResumenFila[] {
    const conteos = this.conteosDependenciaEnListadoActual();
    const filas: DependenciaResumenFila[] = CODIGOS_DEPENDENCIA_RESUMEN.map((codigo) => ({
      codigo,
      label: this.etiquetaDependenciaDemo(codigo),
      cantidad: conteos.get(codigo) ?? 0,
    }));
    const sinDependencia = conteos.get(SIN_DEPENDENCIA_RESUMEN) ?? 0;
    if (sinDependencia > 0) {
      filas.push({
        codigo: SIN_DEPENDENCIA_RESUMEN,
        label: 'Sin dependencia',
        cantidad: sinDependencia,
      });
    }
    return filas;
  }

  dependenciaResumenActiva(codigo: string): boolean {
    return this.dependenciaDemoSeleccionada() === codigo;
  }

  seleccionarDependenciaDesdeResumen(codigo: string): void {
    if (this.dependenciaResumenDeshabilitada(codigo)) {
      return;
    }
    this.overlayResumenBandejaAbierto.set(false);
    if (this.dependenciaDemoSeleccionada() === codigo) {
      this.actualizarDependenciaDemo(null);
      return;
    }
    this.actualizarDependenciaDemo(codigo);
  }

  seleccionarFiltroOperativoDesdeResumen(subCodigo: string): void {
    if (this.filtroOperativoResumenDeshabilitado(subCodigo)) {
      return;
    }
    this.overlayResumenBandejaAbierto.set(false);
    this.cambiarFiltroOperativo(subCodigo);
  }

  abrirCorreoPostalActas(): void {
    void this.router.navigate(['/correo-postal/actas']);
  }

  abrirCorreoPostalLotes(): void {
    void this.router.navigate(['/correo-postal/lotes']);
  }

  abrirNotificadorMunicipal(): void {
    void this.router.navigate(['/notificador-municipal']);
  }

  refrescarLuegoDeOperacionCorreoPostal(): void {
    this.cargarBandejasResumen();
    this.cargarListado();
  }

  reintentarScrollPendienteAlRenderizarListado(): void {
    this.reintentarScrollPendienteAlRenderizarListadoInterno();
  }

  enVistaOperativaNotificaciones(): boolean {
    return this.enCorreoPostal() || this.enNotificadorMunicipal();
  }

  private actualizarVistaCorreoPostal(url: string): void {
    const enCorreo = url.startsWith('/correo-postal');
    this.enCorreoPostal.set(enCorreo);
    this.enCorreoPostalActas.set(enCorreo && !url.startsWith('/correo-postal/lotes'));
    this.enCorreoPostalLotes.set(url.startsWith('/correo-postal/lotes'));
    this.enNotificadorMunicipal.set(url.startsWith('/notificador-municipal'));
  }

  vistaCorreoPostal(): 'actas' | 'lotes' {
    return this.enCorreoPostalLotes() ? 'lotes' : 'actas';
  }

  toggleBandejas(): void {
    this.bandejasColapsadas.update((colapsadas) => !colapsadas);
  }

  abreviaturaBandeja(bandeja: BandejaLateralCodigo): string {
    return BANDEJA_ABREVIATURAS[bandeja];
  }

  iconoBandeja(bandeja: string): string {
    return BANDEJA_ICONOS_LOOKUP[bandeja] ?? 'inbox';
  }

  formatearCantidadBandeja(cantidad: number): string {
    return cantidad.toLocaleString('es-AR');
  }

  seleccionarBandeja(bandeja: BandejaLateralCodigo): void {
    const volverDesdeCorreoPostal =
      this.enVistaOperativaNotificaciones() && this.bandejaSeleccionada() === bandeja;
    if (this.enVistaOperativaNotificaciones()) {
      void this.router.navigate(['/']);
    }

    const mismaBandeja = this.bandejaSeleccionada() === bandeja;
    if (mismaBandeja && !volverDesdeCorreoPostal && this.subBandejaSeleccionada() === null) {
      return;
    }

    this.limpiarScrollActaPendiente();
    this.cerrarOverlaysMoviles();
    const volverATodasEnMismaBandeja = mismaBandeja && this.subBandejaSeleccionada() !== null;
    this.subBandejaSeleccionada.set(null);
    this.dependenciaDemoSeleccionada.set(null);

    if (volverATodasEnMismaBandeja) {
      this.limpiarSeguimientoActaFeedback();
      this.limpiarCopiaEstadoFeedback();
      this.actualizarQueryParams({ filtro: null, dependencia: null }, true);
      this.cargarListado({ actaIdScrollObjetivo: this.actaSeleccionadaId() });
      return;
    }

    if (mismaBandeja && !volverDesdeCorreoPostal) {
      return;
    }

    this.bandejaSeleccionada.set(bandeja);
    this.dependenciaDemoSeleccionada.set(null);
    this.actaSeleccionadaId.set(null);
    this.detalle.set(null);
    this.detalleEstado.set('idle');
    this.limpiarSubRecursosDetalle();
    this.limpiarFirmaFeedback();
    this.limpiarCumplimientoMaterialFeedback();
    this.limpiarResolucionBloqueoCierreFeedback();
    this.limpiarCierreActaFeedback();
    this.limpiarNotificacionFeedback();
    this.limpiarPagoFeedback();
    this.limpiarPagoCondenaFeedback();
    this.limpiarApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.limpiarVencimientoPlazoApelacionFeedback();
    this.limpiarMontoPagoVoluntarioInput();
    this.limpiarMontoCondenaInput();
    this.limpiarArchivoFeedback();
    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.limpiarParalizarFeedback();
    this.paralizarFormAbierto.set(false);
    this.paralizarMotivoSeleccionado.set('');
    this.paralizarObservacion.set('');
    this.limpiarRedaccionFeedback();
    this.limpiarFalloFeedback();
    this.limpiarSeguimientoActaFeedback();
    this.limpiarCopiaEstadoFeedback();
    this.actualizarQueryParams({ bandeja, filtro: null, dependencia: null, acta: null }, false);
    this.cargarListado();
  }


  cambiarFiltroOperativo(subCodigo: string | null): void {
    if (this.enVistaOperativaNotificaciones()) {
      void this.router.navigate(['/']);
    }

    if (this.subBandejaSeleccionada() === subCodigo) {
      return;
    }

    this.subBandejaSeleccionada.set(subCodigo);
    this.dependenciaDemoSeleccionada.set(null);
    this.actualizarQueryParams({ filtro: subCodigo, dependencia: null }, true);
    this.limpiarScrollActaPendiente();
    this.cerrarOverlaysMoviles();
    this.limpiarSeguimientoActaFeedback();
    this.limpiarCopiaEstadoFeedback();
    this.cargarListado({ actaIdScrollObjetivo: this.actaSeleccionadaId() });
  }

  mostrarResumenBandeja(): void {
    if (this.vistaEstrecha()) {
      if (this.actaSeleccionadaId() !== null) {
        return;
      }
      this.overlayResumenBandejaAbierto.set(true);
      return;
    }

    if (!this.actaSeleccionadaId()) {
      return;
    }

    this.limpiarScrollActaPendiente();
    this.actaSeleccionadaId.set(null);
    this.actualizarQueryParams({ acta: null }, true);
    this.detalle.set(null);
    this.detalleEstado.set('idle');
    this.detalleError.set(null);
    this.limpiarSubRecursosDetalle();
    this.limpiarFirmaFeedback();
    this.limpiarCumplimientoMaterialFeedback();
    this.limpiarResolucionBloqueoCierreFeedback();
    this.limpiarCierreActaFeedback();
    this.limpiarNotificacionFeedback();
    this.limpiarPagoFeedback();
    this.limpiarPagoCondenaFeedback();
    this.limpiarApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.limpiarVencimientoPlazoApelacionFeedback();
    this.limpiarMontoPagoVoluntarioInput();
    this.limpiarMontoCondenaInput();
    this.limpiarArchivoFeedback();
    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.limpiarRedaccionFeedback();
    this.limpiarFalloFeedback();
    this.limpiarCopiaEstadoFeedback();
  }

  seleccionarActa(id: string): void {
    if (this.actaSeleccionadaId() === id) {
      if (this.vistaEstrecha() && !this.detalleMovilAbierto()) {
        this.detalleMovilAbierto.set(true);
        this.overlayResumenBandejaAbierto.set(false);
      }
      return;
    }
    this.limpiarScrollActaPendiente();
    this.actaSeleccionadaId.set(id);
    this.actualizarQueryParams({ acta: id }, false);
    if (this.vistaEstrecha()) {
      this.detalleMovilAbierto.set(true);
      this.overlayResumenBandejaAbierto.set(false);
    }
    this.limpiarFirmaFeedback();
    this.limpiarCumplimientoMaterialFeedback();
    this.limpiarResolucionBloqueoCierreFeedback();
    this.limpiarCierreActaFeedback();
    this.limpiarNotificacionFeedback();
    this.limpiarPagoFeedback();
    this.limpiarPagoCondenaFeedback();
    this.limpiarApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.limpiarVencimientoPlazoApelacionFeedback();
    this.limpiarMontoPagoVoluntarioInput();
    this.limpiarMontoCondenaInput();
    this.limpiarArchivoFeedback();
    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.limpiarParalizarFeedback();
    this.paralizarFormAbierto.set(false);
    this.paralizarMotivoSeleccionado.set('');
    this.paralizarObservacion.set('');
    this.limpiarRedaccionFeedback();
    this.limpiarFalloFeedback();
    this.limpiarCopiaEstadoFeedback();
    this.cargarDetalle();
  }

  recargarListado(): void {
    this.cargarListado();
  }

  recargarDetalle(): void {
    this.cargarDetalle();
  }

  recargarDocumentos(): void {
    const id = this.actaSeleccionadaId();
    if (id) {
      this.cargarDocumentos(id);
    }
  }

  recargarEventos(): void {
    const id = this.actaSeleccionadaId();
    if (id) {
      this.cargarEventos(id);
    }
  }

  crearDemo(dependencia: DependenciaActaDemo): void {
    if (this.creandoDemo()) {
      return;
    }
    this.altaDemoError.set(null);
    this.altaDemoMensaje.set(null);
    this.creandoDemo.set(dependencia);

    const payload = payloadCrearActaMockDemo(dependencia);
    this.api
      .crearActaMockDemo(payload)
      .pipe(
        catchError((err) => {
          this.altaDemoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.creandoDemo.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((creada) => {
        if (!creada) {
          return;
        }
        this.bandejaSeleccionada.set('ACTAS_EN_ENRIQUECIMIENTO');
        this.actaSeleccionadaId.set(creada.id);
        this.actualizarQueryParams({ bandeja: 'ACTAS_EN_ENRIQUECIMIENTO', filtro: null, dependencia: null, acta: creada.id, q: null }, false);
        this.detalle.set(creada);
        this.detalleEstado.set('ready');
        this.detalleError.set(null);
        this.altaDemoMensaje.set(`Acta demo creada: ${creada.numeroActa}`);
        this.cargarSubRecursosDetalle(creada.id);
        this.cargarListadoTrasAlta(creada.id);
      });
  }

  documentoEsFirmable(documento: DocumentoActaDemo): boolean {
    if (documento.estadoDocumento !== ESTADO_DOCUMENTO_PENDIENTE_FIRMA) {
      return false;
    }
    return this.detalle()?.bandejaActual === BANDEJA_PENDIENTE_FIRMA;
  }

  firmarDocumento(documento: DocumentoActaDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.firmandoDocumentoId() !== null) {
      return;
    }
    if (!this.documentoEsFirmable(documento)) {
      return;
    }

    this.limpiarFirmaFeedback();
    this.firmandoDocumentoId.set(documento.id);

    this.api
      .firmarDocumento(actaId, documento.id)
      .pipe(
        catchError((err) => {
          this.firmaDocumentoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.firmandoDocumentoId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.firmaDocumentoMensaje.set('Documento firmado.');
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  esTipoCumplimientoMaterialConocido(
    pendiente: string,
  ): pendiente is TipoCumplimientoMaterialBloqueante {
    return (TIPOS_CUMPLIMIENTO_MATERIAL as readonly string[]).includes(pendiente);
  }

  etiquetaCumplimientoMaterial(pendiente: string): string {
    if (this.esTipoCumplimientoMaterialConocido(pendiente)) {
      return ETIQUETA_CUMPLIMIENTO_MATERIAL[pendiente];
    }
    return pendiente;
  }

  cumplirMaterialmente(pendiente: string): void {
    if (!this.puedeMostrarAccionCumplimientoMaterial(pendiente)) {
      return;
    }
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.cumpliendoMaterial() !== null) {
      return;
    }

    this.limpiarCumplimientoMaterialFeedback();
    this.cumpliendoMaterial.set(pendiente);

    this.api
      .registrarCumplimientoMaterialBloqueoCierre(actaId, pendiente)
      .pipe(
        catchError((err) => {
          this.cumplimientoMaterialError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.cumpliendoMaterial.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.cumplimientoMaterialMensaje.set(
          pendiente === 'LIBERACION_RODADO'
            ? 'Retiro de rodado registrado (app).'
            : 'Cumplimiento material registrado: ' + respuesta.pendienteCumplido + '.',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  actaEsCerrableDesdeBackend(): boolean {
    return this.detalle()?.cerrabilidad?.cerrable === true;
  }

  bandejaActaSinAccionesInternasOperativas(): boolean {
    const bandeja = this.detalle()?.bandejaActual;
    if (!bandeja) {
      return false;
    }
    return (BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS as readonly string[]).includes(bandeja);
  }

  puedeMostrarPagoVoluntario(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    if (this.actaTieneDocumentoFallo()) {
      return false;
    }
    if (this.actaTieneEventoFalloDictado()) {
      return false;
    }
    const resultado = det.cerrabilidad?.resultadoFinal;
    if (
      resultado != null &&
      resultado.trim() !== '' &&
      (RESULTADOS_FINAL_SIN_PAGO_VOLUNTARIO as readonly string[]).includes(resultado)
    ) {
      return false;
    }
    return true;
  }

  etiquetaDependenciaDemo(codigo: string | null | undefined): string {
    if (codigo == null || codigo.trim() === '') {
      return '-';
    }
    return ETIQUETA_DEPENDENCIA_DEMO[codigo] ?? codigo;
  }

  actaSinResultadoFinal(): boolean {
    const resultado = this.detalle()?.cerrabilidad?.resultadoFinal;
    if (resultado == null || resultado.trim() === '') {
      return true;
    }
    return resultado === 'SIN_RESULTADO_FINAL';
  }

  actaTieneDocumentoFallo(): boolean {
    return this.documentos().some((doc) => TIPOS_DOCUMENTO_FALLO.includes(doc.tipoDocumento));
  }

  actaTieneEventoFalloDictado(): boolean {
    return this.eventos().some((evento) => EVENTOS_FALLO_DICTADO.includes(evento.tipoEvento));
  }

  puedeMostrarFalloResolucionFondo(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.accionesUi?.falloFondo != null) {
      return det.accionesUi.falloFondo;
    }
    if (det.estaCerrada || det.bandejaActual === 'CERRADAS') {
      return false;
    }
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    if (det.bandejaActual !== BANDEJA_PENDIENTE_ANALISIS) {
      return false;
    }
    if (!this.actaSinResultadoFinal()) {
      return false;
    }
    if (det.accionPendiente === 'VERIFICAR_PAGO_INFORMADO') {
      return false;
    }
    if (this.pendientesBloqueantes(det).length > 0) {
      return false;
    }
    if (this.actaTieneDocumentoFallo()) {
      return false;
    }
    if (this.actaTieneEventoFalloDictado()) {
      return false;
    }
    return true;
  }

  dictarFalloAbsolutorio(): void {
    this.ejecutarDictarFallo('ABSOLUTORIO');
  }

  dictarFalloCondenatorio(): void {
    this.ejecutarDictarFallo('CONDENATORIO');
  }

  actualizarMontoCondenaInput(valor: string): void {
    this.montoCondenaInput.set(valor ?? '');
  }

  montoCondenaParseado(): number | null {
    const raw = this.montoCondenaInput().trim();
    if (raw.length === 0) {
      return null;
    }
    const normalizado = raw.replace(',', '.');
    const valor = Number(normalizado);
    if (!Number.isFinite(valor) || valor <= 0) {
      return null;
    }
    return valor;
  }

  montoCondenaInputEsValido(): boolean {
    return this.montoCondenaParseado() !== null;
  }

  private ejecutarDictarFallo(tipo: TipoFalloDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || !this.puedeMostrarFalloResolucionFondo() || this.dictandoFallo()) {
      return;
    }
    if (tipo === 'CONDENATORIO' && !this.montoCondenaInputEsValido()) {
      return;
    }

    this.limpiarFalloFeedback();
    this.dictandoFallo.set(tipo);

    const request =
      tipo === 'ABSOLUTORIO'
        ? this.api.dictarFalloAbsolutorio(actaId)
        : this.api.dictarFalloCondenatorio(actaId, {
            montoCondena: this.montoCondenaParseado() ?? 0,
          });

    request
      .pipe(
        catchError((err) => {
          this.falloError.set(mensajeErrorDictarFalloHttp(err));
          return of(null);
        }),
        finalize(() => this.dictandoFallo.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        const mensajeOk =
          tipo === 'ABSOLUTORIO'
            ? 'Fallo absolutorio dictado correctamente.'
            : 'Fallo condenatorio dictado correctamente.';
        this.falloMensaje.set(mensajeOk);
        this.refrescarLuegoDeAccion(respuesta);
      });
  }


  puedeMostrarRegistrarApelacionPresencial(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.estaCerrada || det.bandejaActual === 'CERRADAS') {
      return false;
    }
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    if (det.cerrabilidad?.resultadoFinal !== 'CONDENADO') {
      return false;
    }
    if (this.actaTieneApelacionPresentada()) {
      return false;
    }
    return true;
  }

  ejecutarConsentirCondenaYRegistrarPago(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoConsentirCondenaYRegistrarPago()) {
      return;
    }
    this.limpiarConsentirCondenaYRegistrarPagoFeedback();
    this.ejecutandoConsentirCondenaYRegistrarPago.set(true);
    this.api
      .consentirCondenaYRegistrarPago(actaId)
      .pipe(
        catchError((err) => {
          this.consentirCondenaYRegistrarPagoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoConsentirCondenaYRegistrarPago.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.consentirCondenaYRegistrarPagoMensaje.set(
          'Condena consentida y pago registrado. La acreditacion quedo pendiente de confirmacion.',
        );
        this.refrescarLuegoDeAccion({ actaId: respuesta.actaId });
      });
  }

  actaTieneApelacionPresentada(): boolean {
    return this.eventos().some((evento) => evento.tipoEvento === 'APELACION_PRESENTADA');
  }

  actaTieneFalloCondenatorioNotificado(): boolean {
    return this.eventos().some((evento) => evento.tipoEvento === 'FALLO_CONDENATORIO_NOTIFICADO');
  }

  actaTienePlazoApelacionVencido(): boolean {
    return this.eventos().some((evento) => evento.tipoEvento === 'PLAZO_APELACION_VENCIDO');
  }

  puedeMostrarRegistrarVencimientoPlazoApelacion(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.estaCerrada || det.bandejaActual === 'CERRADAS') {
      return false;
    }
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    if (det.cerrabilidad?.resultadoFinal !== 'CONDENADO') {
      return false;
    }
    if (!this.actaTieneFalloCondenatorioNotificado()) {
      return false;
    }
    if (this.actaTieneApelacionPresentada()) {
      return false;
    }
    if (this.actaTieneApelacionRechazada()) {
      return false;
    }
    if (this.actaTieneApelacionAceptadaAbsuelve()) {
      return false;
    }
    if (this.actaTienePlazoApelacionVencido()) {
      return false;
    }
    return true;
  }

  consentirCondenaYRegistrarPagoHabilitado(): boolean {
    return this.detalle()?.accionesUi?.consentirCondenaYRegistrarPago ?? false;
  }

  registrarApelacionPresencial(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (!this.puedeMostrarRegistrarApelacionPresencial()) {
      return;
    }
    if (this.registrandoApelacion()) {
      return;
    }

    this.limpiarApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.registrandoApelacion.set(true);

    this.api
      .registrarApelacion(actaId, { canal: 'PRESENCIAL_DIRECCION' })
      .pipe(
        catchError((err) => {
          this.apelacionError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.registrandoApelacion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.apelacionMensaje.set(
          'Apelacion presencial registrada (' +
            (respuesta.mensaje ?? 'sin mensaje del backend') +
            ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  registrarVencimientoPlazoApelacion(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (!this.puedeMostrarRegistrarVencimientoPlazoApelacion()) {
      return;
    }
    if (this.registrandoVencimientoPlazoApelacion() || this.registrandoApelacion()) {
      return;
    }

    this.limpiarVencimientoPlazoApelacionFeedback();
    this.limpiarApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.registrandoVencimientoPlazoApelacion.set(true);

    this.api
      .registrarVencimientoPlazoApelacion(actaId)
      .pipe(
        catchError((err) => {
          this.vencimientoPlazoApelacionError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.registrandoVencimientoPlazoApelacion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.vencimientoPlazoApelacionMensaje.set(
          respuesta.mensaje?.trim() || 'Vencimiento de plazo registrado correctamente.',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  actaTieneApelacionRechazada(): boolean {
    return this.eventos().some((evento) => evento.tipoEvento === 'APELACION_RECHAZADA');
  }

  actaTieneApelacionAceptadaAbsuelve(): boolean {
    return this.eventos().some((evento) => evento.tipoEvento === 'APELACION_ACEPTADA_ABSUELVE');
  }

  puedeMostrarResolucionApelacion(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.estaCerrada || det.bandejaActual === 'CERRADAS') {
      return false;
    }
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    if (det.cerrabilidad?.resultadoFinal !== 'CONDENADO') {
      return false;
    }
    if (!this.actaTieneApelacionPresentada()) {
      return false;
    }
    if (this.actaTieneApelacionRechazada() || this.actaTieneApelacionAceptadaAbsuelve()) {
      return false;
    }
    return true;
  }

  rechazarApelacion(): void {
    this.ejecutarResolucionApelacion('RECHAZADA', 'Apelación rechazada correctamente.');
  }

  aceptarApelacionYAbsolver(): void {
    this.ejecutarResolucionApelacion(
      'ACEPTADA_ABSUELVE',
      'Apelación aceptada: el acta quedó absuelta.',
    );
  }

  private ejecutarResolucionApelacion(
    resultado: 'RECHAZADA' | 'ACEPTADA_ABSUELVE',
    mensajeOk: string,
  ): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || !this.puedeMostrarResolucionApelacion() || this.resolviendoApelacion()) {
      return;
    }

    this.limpiarResolucionApelacionFeedback();
    this.resolviendoApelacion.set(true);

    this.api
      .resolverApelacion(actaId, { resultado })
      .pipe(
        catchError((err) => {
          this.resolucionApelacionError.set(mensajeErrorResolverApelacionHttp(err));
          return of(null);
        }),
        finalize(() => this.resolviendoApelacion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.resolucionApelacionMensaje.set(mensajeOk);
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  puedeMostrarCierreActa(): boolean {
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    return this.actaEsCerrableDesdeBackend();
  }

  puedeMostrarBloqueCierreActa(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.cierreActaMensaje() || this.cierreActaError()) {
      return true;
    }
    if (det.estaCerrada || det.bandejaActual === 'CERRADAS') {
      return true;
    }
    return this.puedeMostrarCierreActa();
  }

  resultadoFinalOcultaArchivoReingresoDemo(): boolean {
    const resultadoFinal = this.detalle()?.cerrabilidad?.resultadoFinal;
    return resultadoFinal === 'CONDENADO' || resultadoFinal === 'CONDENA_FIRME';
  }

  puedeMostrarBloqueEnriquecimiento(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    return (
      det.bandejaActual === 'ACTAS_EN_ENRIQUECIMIENTO' ||
      !!(this.enriquecimientoMensaje() || this.enriquecimientoError())
    );
  }

  private limpiarEnriquecimientoFeedback(): void {
    this.enriquecimientoMensaje.set(null);
    this.enriquecimientoError.set(null);
  }

  enviarActaANotificacion(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoEnriquecimientoAccion() !== null) {
      return;
    }
    this.limpiarEnriquecimientoFeedback();
    this.ejecutandoEnriquecimientoAccion.set('ENVIAR_NOTIFICACION');
    this.api
      .enviarANotificacion(actaId)
      .pipe(
        catchError((err) => {
          this.enriquecimientoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoEnriquecimientoAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.enriquecimientoMensaje.set('Acta enviada a notificacion.');
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  anularActaPorNulidad(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoEnriquecimientoAccion() !== null) {
      return;
    }
    this.limpiarEnriquecimientoFeedback();
    this.ejecutandoEnriquecimientoAccion.set('ANULAR_ACTA');
    this.api
      .anularActa(actaId)
      .pipe(
        catchError((err) => {
          this.enriquecimientoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoEnriquecimientoAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.enriquecimientoMensaje.set('Acta anulada y archivada por nulidad.');
        this.refrescarLuegoDeAccion(respuesta);
      });
  }
  puedeMostrarBloqueArchivoReingreso(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.resultadoFinalOcultaArchivoReingresoDemo()) {
      return !!(this.archivoMensaje() || this.archivoError());
    }
    if (this.archivoMensaje() || this.archivoError()) {
      return true;
    }
    if (det.bandejaActual === 'ARCHIVO') {
      return true;
    }
    // PENDIENTE_ANALISIS: solo mostramos el bloque si efectivamente se
    // puede archivar. Si la acta ya es cerrable, la accion operativa
    // correcta es cerrar el acta, no archivarla.
    return this.actaPuedeArchivarseDesdeBackend();
  }

  puedeMostrarBloqueCondenaFirme(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    return (
      det.cerrabilidad?.resultadoFinal === 'CONDENA_FIRME' ||
      this.pagoCondenaMensaje() !== null ||
      this.pagoCondenaError() !== null
    );
  }

  puedeMostrarBloqueGestionExterna(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.gestionExternaMensaje() || this.gestionExternaError()) {
      return true;
    }
    if (det.bandejaActual === 'GESTION_EXTERNA') {
      return true;
    }
    return this.actaPuedeDerivarseAGestionExternaDesdeBackend();
  }

  puedeMostrarBloqueParalizadas(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.reactivacionMensaje() || this.reactivacionError()) {
      return true;
    }
    return det.bandejaActual === 'PARALIZADAS';
  }

  etiquetaMotivoParalizacion(): string | null {
    const motivo = this.detalle()?.motivoParalizacion;
    if (!motivo) return null;
    const found = MOTIVOS_PARALIZACION.find((m) => m.valor === motivo);
    return found?.etiqueta ?? motivo;
  }

  puedeMostrarBloqueParalizarActa(): boolean {
    if (this.paralizarMensaje() || this.paralizarError()) {
      return true;
    }
    return this.detalle()?.accionesUi?.paralizarActa === true;
  }

  puedeMostrarBloqueNotificacionActa(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (this.notificacionMensaje() || this.notificacionError()) {
      return true;
    }
    if (this.actaPuedeReintentarNotificacionDesdeBackend()) {
      return true;
    }
    return this.actaEsNotificableDesdeBackend();
  }

  puedeMostrarCumplimientosMateriales(): boolean {
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.accionesUi != null && det.accionesUi.cumplimientoMaterial === false) {
      return false;
    }
    return this.pendientesBloqueantes(det).some((pendiente) =>
      this.esTipoCumplimientoMaterialConocido(pendiente),
    );
  }

  // El cumplimiento material aplica cuando el eje tiene resolutorio ya FIRMADO
  // (fase RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL). Igual para los tres tipos:
  // ENTREGA_DOCUMENTACION, LEVANTAMIENTO_MEDIDA_PREVENTIVA y LIBERACION_RODADO.
  puedeMostrarAccionCumplimientoMaterial(
    pendiente: string,
  ): pendiente is TipoCumplimientoMaterialBloqueante {
    if (!this.puedeMostrarCumplimientosMateriales()) {
      return false;
    }
    if (!this.esTipoCumplimientoMaterialConocido(pendiente)) {
      return false;
    }
    return this.faseEjeParaPendiente(pendiente) === FASE_EJE_RESOLUTORIO_EN_EXPEDIENTE;
  }

  // Visibilidad de generación de resolutorio de bloqueante material. Los tres tipos
  // (ENTREGA_DOCUMENTACION, LEVANTAMIENTO_MEDIDA_PREVENTIVA, LIBERACION_RODADO)
  // siguen el mismo ciclo: generar resolutorio  firmar  cumplimiento.
  // Se muestra cuando la fase es SITUACION_PENDIENTE_DE_RESOLUTORIO.
  puedeMostrarAccionResolucionBloqueoCierre(
    pendiente: string,
  ): pendiente is TipoResolucionBloqueoCierre {
    if (this.bandejaActaSinAccionesInternasOperativas()) {
      return false;
    }
    const det = this.detalle();
    if (det?.accionesUi != null && det.accionesUi.resolucionBloqueante === false) {
      return false;
    }
    if (!this.esTipoCumplimientoMaterialConocido(pendiente)) {
      return false;
    }
    return this.faseEjeParaPendiente(pendiente) === FASE_EJE_PENDIENTE_RESOLUTORIO;
  }

  // Devuelve la fase del eje material cuyo ejeBloqueanteCierre coincide
  // con el pendiente bloqueante recibido. Si no hay detalle o no hay eje
  // asociado, retorna null (la UI no puede ofrecer accion material).
  faseEjeParaPendiente(pendiente: string): string | null {
    const det = this.detalle();
    if (!det) {
      return null;
    }
    const eje = this.ejesHechosMateriales(det).find(
      (e) => e.ejeBloqueanteCierre === pendiente,
    );
    return eje?.fase ?? null;
  }

  etiquetaResolucionBloqueoCierre(pendiente: string): string {
    if (this.esTipoCumplimientoMaterialConocido(pendiente)) {
      return ETIQUETA_RESOLUCION_BLOQUEO_CIERRE[pendiente];
    }
    return pendiente;
  }

  etiquetaResolucionBloqueoCierreEnCurso(pendiente: TipoResolucionBloqueoCierre): string {
    return ETIQUETA_RESOLUCION_BLOQUEO_CIERRE_EN_CURSO[pendiente];
  }

  resolverBloqueoCierre(pendiente: string): void {
    if (!this.puedeMostrarAccionResolucionBloqueoCierre(pendiente)) {
      return;
    }
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.resolviendoBloqueoCierre() !== null) {
      return;
    }

    this.limpiarResolucionBloqueoCierreFeedback();
    this.resolviendoBloqueoCierre.set(pendiente);

    this.api
      .registrarResolucionBloqueoCierreDocumental(actaId, pendiente)
      .pipe(
        catchError((err) => {
          this.resolucionBloqueoCierreError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.resolviendoBloqueoCierre.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.resolucionBloqueoCierreMensaje.set(
          'Resolucion documental registrada para ' + respuesta.pendienteAtendido + '.',
        );
        this.refrescarLuegoDeAccion({ actaId: respuesta.actaId });
      });
  }

  cerrarActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.cerrandoActa()) {
      return;
    }
    if (!this.puedeMostrarCierreActa()) {
      return;
    }

    this.limpiarCierreActaFeedback();
    this.cerrandoActa.set(true);

    this.api
      .cerrarActa(actaId)
      .pipe(
        catchError((err) => {
          this.cierreActaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.cerrandoActa.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.cierreActaMensaje.set('Acta cerrada (' + respuesta.bandejaActual + ').');
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  actaEsNotificableDesdeBackend(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    return (BANDEJAS_NOTIFICABLES as readonly string[]).includes(det.bandejaActual);
  }

  etiquetaResultadoNotificacion(resultado: ResultadoNotificacionDemo): string {
    return ETIQUETA_RESULTADO_NOTIFICACION[resultado];
  }

  registrarResultadoNotificacion(resultado: ResultadoNotificacionDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.notificandoResultado() !== null) {
      return;
    }
    if (!this.actaEsNotificableDesdeBackend()) {
      return;
    }

    this.limpiarNotificacionFeedback();
    this.notificandoResultado.set(resultado);

    const peticion$ = this.peticionNotificacion(actaId, resultado);

    peticion$
      .pipe(
        catchError((err) => {
          this.notificacionError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.notificandoResultado.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.notificacionMensaje.set(
          'Notificacion registrada (' +
            resultado +
            '); acta paso a ' +
            respuesta.bandejaActual +
            '.',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  private peticionNotificacion(actaId: string, resultado: ResultadoNotificacionDemo) {
    switch (resultado) {
      case 'POSITIVA':
        return this.api.registrarNotificacionPositiva(actaId);
      case 'NEGATIVA':
        return this.api.registrarNotificacionNegativa(actaId);
      case 'VENCIDA':
        return this.api.registrarNotificacionVencida(actaId);
    }
  }

  // Disponibilidad del reintento de notificacion. El backend acepta
  // POST /acciones/reintentar-notificacion cuando el acta esta en
  // PENDIENTE_ANALISIS y la marca operativa accionPendiente es una de:
  //   - REINTENTAR_NOTIFICACION (caso producido por notificacion negativa);
  //   - EVALUAR_NOTIFICACION_VENCIDA (caso producido por vencimiento de plazo).
  // Angular refleja esa precondicion sin inventar tipos.
  actaPuedeReintentarNotificacionDesdeBackend(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.bandejaActual !== 'PENDIENTE_ANALISIS') {
      return false;
    }
    return (
      det.accionPendiente === 'REINTENTAR_NOTIFICACION' ||
      det.accionPendiente === 'EVALUAR_NOTIFICACION_VENCIDA'
    );
  }

  // Hay alguna accion del area notificacion en curso (registrar resultado
  // o reintentar). Sirve como disabled compartido para evitar disparar
  // dos POST simultaneos sobre el mismo bloque.
  accionNotificacionEnCurso(): boolean {
    return this.notificandoResultado() !== null || this.reintentandoNotificacion();
  }

  reintentarNotificacion(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.accionNotificacionEnCurso()) {
      return;
    }
    if (!this.actaPuedeReintentarNotificacionDesdeBackend()) {
      return;
    }

    this.limpiarNotificacionFeedback();
    this.reintentandoNotificacion.set(true);

    this.api
      .reintentarNotificacion(actaId)
      .pipe(
        catchError((err) => {
          this.notificacionError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.reintentandoNotificacion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.notificacionMensaje.set(
          'Reintento de notificacion solicitado; acta vuelve a ' +
            respuesta.bandejaActual +
            '.',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  etiquetaAccionPago(accion: AccionPagoVoluntarioDemo): string {
    return ETIQUETA_ACCION_PAGO[accion];
  }

  etiquetaAccionPagoEnCurso(accion: AccionPagoVoluntarioDemo): string {
    return ETIQUETA_ACCION_PAGO_EN_CURSO[accion];
  }

  // Acciones de pago realmente disponibles segun el backend. El backend
  // computa la lista a partir de las precondiciones que aplican los
  // handlers de cada accion (bandeja + situacionPago + no cerrada);
  // Angular no infiere, solo refleja y filtra defensivamente a las
  // acciones que esta version de la UI sabe renderizar.
  accionesPagoDisponiblesDesdeBackend(): AccionPagoVoluntarioDemo[] {
    const lista = this.detalle()?.accionesPagoVoluntarioDisponibles ?? [];
    return lista.filter((accion): accion is AccionPagoVoluntarioDemo =>
      this.esAccionPagoConocida(accion),
    );
  }

  hayAccionesPagoDisponibles(): boolean {
    if (!this.puedeMostrarPagoVoluntario()) {
      return false;
    }
    return this.accionesPagoDisponiblesDesdeBackend().length > 0 ||
      this.puedeRegistrarVencimientoPagoVoluntario();
  }

  puedeRegistrarVencimientoPagoVoluntario(): boolean {
    return this.puedeMostrarPagoVoluntario() &&
      this.detalle()?.accionesUi?.vencimientoPagoVoluntario === true;
  }

  situacionPagoSinValorDesdeBackend(): boolean {
    const valor = this.detalle()?.situacionPago;
    return valor === undefined || valor === null || valor === '';
  }

  situacionPagoDesconocidaDesdeBackend(): boolean {
    const valor = this.detalle()?.situacionPago;
    if (valor === undefined || valor === null || valor === '') {
      return false;
    }
    return !this.esSituacionPagoConocida(valor);
  }

  private esSituacionPagoConocida(valor: string): valor is SituacionPagoDemo {
    return (SITUACIONES_PAGO_CONOCIDAS as readonly string[]).includes(valor);
  }

  private esAccionPagoConocida(valor: string): valor is AccionPagoVoluntarioDemo {
    return (ACCIONES_PAGO_CONOCIDAS as readonly string[]).includes(valor);
  }

  etiquetaSituacionPagoCondena(valor: SituacionPagoCondenaDemo | string | null | undefined): string {
    if (this.esSituacionPagoCondenaConocida(valor)) {
      return ETIQUETA_SITUACION_PAGO_CONDENA[valor];
    }
    return valor ?? '-';
  }

  etiquetaAccionPagoCondena(accion: AccionPagoCondenaDemo): string {
    return ETIQUETA_ACCION_PAGO_CONDENA[accion];
  }

  etiquetaAccionPagoCondenaEnCurso(accion: AccionPagoCondenaDemo): string {
    return ETIQUETA_ACCION_PAGO_CONDENA_EN_CURSO[accion];
  }

  accionesPagoCondenaDisponiblesDesdeBackend(): AccionPagoCondenaDemo[] {
    const lista = this.detalle()?.accionesPagoCondenaDisponibles ?? [];
    return lista.filter((accion): accion is AccionPagoCondenaDemo =>
      this.esAccionPagoCondenaConocida(accion),
    );
  }

  accionesGestionExternaDisponiblesDesdeBackend(): TipoGestionExternaDemo[] {
    const lista = this.detalle()?.accionesGestionExternaDisponibles ?? [];
    return lista.filter((accion): accion is TipoGestionExternaDemo =>
      accion === 'APREMIO' || accion === 'JUZGADO_DE_PAZ',
    );
  }

  hayAccionesPagoCondenaDisponibles(): boolean {
    return this.accionesPagoCondenaDisponiblesDesdeBackend().length > 0;
  }

  puedeEjecutarAccionPagoCondena(accion: AccionPagoCondenaDemo): boolean {
    return this.ejecutandoPagoCondenaAccion() === null &&
      this.accionesPagoCondenaDisponiblesDesdeBackend().includes(accion);
  }

  ejecutarAccionPagoCondena(accion: AccionPagoCondenaDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || !this.puedeEjecutarAccionPagoCondena(accion)) {
      return;
    }

    this.limpiarPagoCondenaFeedback();
    this.ejecutandoPagoCondenaAccion.set(accion);

    this.peticionPagoCondena(actaId, accion)
      .pipe(
        catchError((err) => {
          this.pagoCondenaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoPagoCondenaAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.pagoCondenaMensaje.set(
          this.etiquetaAccionPagoCondena(accion) + ' OK (' + respuesta.mensaje + ').',
        );
        this.refrescarLuegoDeAccion({ actaId: respuesta.actaId });
      });
  }

  private esSituacionPagoCondenaConocida(
    valor: string | null | undefined,
  ): valor is SituacionPagoCondenaDemo {
    return valor != null &&
      (SITUACIONES_PAGO_CONDENA_CONOCIDAS as readonly string[]).includes(valor);
  }

  private esAccionPagoCondenaConocida(valor: string): valor is AccionPagoCondenaDemo {
    return (ACCIONES_PAGO_CONDENA_CONOCIDAS as readonly string[]).includes(valor);
  }

  private peticionPagoCondena(actaId: string, accion: AccionPagoCondenaDemo) {
    switch (accion) {
      case 'INFORMAR':
        return this.api.informarPagoCondena(actaId);
      case 'CONFIRMAR':
        return this.api.confirmarPagoCondena(actaId);
      case 'OBSERVAR':
        return this.api.observarPagoCondena(actaId);
    }
  }

  ejecutarAccionPago(accion: AccionPagoVoluntarioDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (!this.puedeMostrarPagoVoluntario()) {
      return;
    }
    if (this.ejecutandoPagoAccion() !== null || this.registrandoVencimientoPagoVoluntario()) {
      return;
    }
    if (this.accionPagoRequiereMonto(accion) && !this.montoPagoVoluntarioInputEsValido()) {
      // Boton no debe llegar aca: gating extra defensivo.
      return;
    }

    this.limpiarPagoFeedback();
    this.ejecutandoPagoAccion.set(accion);

    const peticion$ = this.peticionPago(actaId, accion);

    peticion$
      .pipe(
        catchError((err) => {
          this.pagoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoPagoAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.pagoMensaje.set(
          this.etiquetaAccionPago(accion) +
            ' OK (' +
            (respuesta.mensaje ?? 'sin mensaje del backend') +
            ').',
        );
        if (this.accionPagoRequiereMonto(accion)) {
          this.limpiarMontoPagoVoluntarioInput();
        }
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  registrarVencimientoPagoVoluntario(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || !this.puedeRegistrarVencimientoPagoVoluntario()) {
      return;
    }
    if (this.ejecutandoPagoAccion() !== null || this.registrandoVencimientoPagoVoluntario()) {
      return;
    }

    this.limpiarPagoFeedback();
    this.registrandoVencimientoPagoVoluntario.set(true);

    this.api
      .registrarVencimientoPagoVoluntario(actaId)
      .pipe(
        catchError((err) => {
          this.pagoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.registrandoVencimientoPagoVoluntario.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.pagoMensaje.set(
          respuesta.mensaje ??
            'Vencimiento de pago voluntario registrado. El tramite puede continuar a fallo.',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }
  actualizarMontoPagoVoluntarioInput(valor: string): void {
    this.montoPagoVoluntarioInput.set(valor ?? '');
  }

  /**
   * Parsea el input de monto del acta. Devuelve null si esta vacio o no
   * representa un numero valido y estrictamente mayor a cero. El backend
   * vuelve a validar; este parseo solo habilita el boton "Pago voluntario".
   */
  montoPagoVoluntarioParseado(): number | null {
    const raw = this.montoPagoVoluntarioInput().trim();
    if (raw.length === 0) {
      return null;
    }
    const normalizado = raw.replace(',', '.');
    const valor = Number(normalizado);
    if (!Number.isFinite(valor) || valor <= 0) {
      return null;
    }
    return valor;
  }

  montoPagoVoluntarioInputEsValido(): boolean {
    return this.montoPagoVoluntarioParseado() !== null;
  }

  /** True cuando hay que pedir monto al ejecutar la accion. */
  accionPagoRequiereMonto(accion: AccionPagoVoluntarioDemo): boolean {
    return accion === 'SOLICITAR' || accion === 'FIJAR_MONTO';
  }

  /**
   * Habilitacion del boton por accion: SOLICITAR exige monto valido. Las
   * demas acciones solo dependen de que no haya otra accion en curso.
   */
  puedeEjecutarAccionPago(accion: AccionPagoVoluntarioDemo): boolean {
    if (this.ejecutandoPagoAccion() !== null || this.registrandoVencimientoPagoVoluntario()) {
      return false;
    }
    if (this.accionPagoRequiereMonto(accion)) {
      return this.montoPagoVoluntarioInputEsValido();
    }
    return true;
  }

  private peticionPago(
    actaId: string,
    accion: AccionPagoVoluntarioDemo,
  ): Observable<AccionConBandejaResponseLike & { mensaje?: string } | null> {
    switch (accion) {
      case 'SOLICITAR': {
        // Direccion fija el monto del acta; el backend valida > 0.
        const monto = this.montoPagoVoluntarioParseado();
        return this.api.registrarSolicitudPagoVoluntario(actaId, { monto: monto ?? 0 });
      }
      case 'FIJAR_MONTO': {
        const monto = this.montoPagoVoluntarioParseado();
        return this.api.fijarMontoPagoVoluntario(actaId, { monto: monto ?? 0 });
      }
      case 'INFORMAR':
        return this.api.registrarPagoInformado(actaId);
      case 'ADJUNTAR_COMPROBANTE':
        return this.api.adjuntarComprobantePagoInformado(
          actaId,
          NOMBRE_ARCHIVO_COMPROBANTE_MOCK,
        );
      case 'CONFIRMAR':
        return this.api.confirmarPagoInformado(actaId);
      case 'OBSERVAR':
        return this.api.observarPagoInformado(actaId);
    }
  }

  // Disponibilidad de archivo segun bandeja real informada por el backend.
  // El backend acepta archivar solo desde PENDIENTE_ANALISIS y rechaza con
  // 409 si la acta ya cumple condicion de cierre operativo. Reflejamos
  // esa precondicion: si cerrabilidad.cerrable === true, la accion
  // operativa correcta es cerrar el acta, no archivarla.
  actaPuedeArchivarseDesdeBackend(): boolean {
    const det = this.detalle();
    if (det?.bandejaActual !== 'PENDIENTE_ANALISIS') {
      return false;
    }
    return det.cerrabilidad?.cerrable !== true;
  }

  // Disponibilidad de reingreso desde archivo. Backend exige bandeja
  // ARCHIVO y permiteReingreso=true (reingreso queda consumido luego de
  // ejecutarse).
  actaPuedeReingresarDesdeBackend(): boolean {
    const det = this.detalle();
    return det?.bandejaActual === 'ARCHIVO' && det?.permiteReingreso === true;
  }

  archivarActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoArchivoAccion() !== null) {
      return;
    }
    if (!this.actaPuedeArchivarseDesdeBackend()) {
      return;
    }

    this.limpiarArchivoFeedback();
    this.ejecutandoArchivoAccion.set('ARCHIVAR');

    this.api
      .archivarActa(actaId)
      .pipe(
        catchError((err) => {
          this.archivoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoArchivoAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.archivoMensaje.set(
          'Acta archivada (' + respuesta.bandejaActual + '; motivo ' + respuesta.motivoArchivo + ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  reingresarActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoArchivoAccion() !== null) {
      return;
    }
    if (!this.actaPuedeReingresarDesdeBackend()) {
      return;
    }

    this.limpiarArchivoFeedback();
    this.ejecutandoArchivoAccion.set('REINGRESAR');

    this.api
      .reingresarActa(actaId)
      .pipe(
        catchError((err) => {
          this.archivoError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoArchivoAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.archivoMensaje.set(
          'Acta reingresada (' + respuesta.bandejaActual + '; accion pendiente ' + respuesta.accionPendiente + ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  // Disponibilidad de derivacion a gestion externa segun bandeja y marca
  // operativa real informadas por el backend. La derivacion solo aplica
  // desde PENDIENTE_ANALISIS con accionPendiente DERIVAR_GESTION_EXTERNA
  // (primera derivacion) o REVISION_POST_GESTION_EXTERNA (re-derivacion
  // tras un retorno). Angular no decide cerrabilidad ni inventa tipos.
  actaPuedeDerivarseAGestionExternaDesdeBackend(): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    if (det.bandejaActual !== 'PENDIENTE_ANALISIS') {
      return false;
    }
    return this.accionesGestionExternaDisponiblesDesdeBackend().length > 0;
  }

  // Disponibilidad de retorno desde gestion externa. Backend exige bandeja
  // GESTION_EXTERNA y permiteReingreso=true. El reingreso no se consume
  // por ida y vuelta: la bandera se preserva en true mientras el caso
  // siga siendo reingresable.
  actaPuedeRetornarDesdeGestionExternaDesdeBackend(): boolean {
    const det = this.detalle();
    return det?.bandejaActual === 'GESTION_EXTERNA' && det?.permiteReingreso === true;
  }

  etiquetaDerivacionGestionExterna(tipo: TipoGestionExternaDemo): string {
    return ETIQUETA_DERIVACION_GESTION_EXTERNA[tipo];
  }

  etiquetaDerivacionGestionExternaEnCurso(tipo: TipoGestionExternaDemo): string {
    return ETIQUETA_DERIVACION_GESTION_EXTERNA_EN_CURSO[tipo];
  }

  derivarGestionExterna(tipo: TipoGestionExternaDemo): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoGestionExternaAccion() !== null) {
      return;
    }
    if (!this.actaPuedeDerivarseAGestionExternaDesdeBackend()) {
      return;
    }

    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.ejecutandoGestionExternaAccion.set(tipo);

    const peticion$ =
      tipo === 'APREMIO'
        ? this.api.derivarAApremio(actaId)
        : this.api.derivarAJuzgadoDePaz(actaId);

    peticion$
      .pipe(
        catchError((err) => {
          this.gestionExternaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.gestionExternaMensaje.set(
          'Acta derivada a gestion externa (' +
            respuesta.bandejaActual +
            '; tipo ' +
            respuesta.tipoGestionExterna +
            ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  retornarGestionExterna(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoGestionExternaAccion() !== null) {
      return;
    }
    if (!this.actaPuedeRetornarDesdeGestionExternaDesdeBackend()) {
      return;
    }

    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.ejecutandoGestionExternaAccion.set('RETORNAR');

    this.api
      .reingresarDesdeGestionExterna(actaId)
      .pipe(
        catchError((err) => {
          this.gestionExternaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        const tipoPrevio = respuesta.tipoGestionExternaPrevia;
        const sufijoTipo = tipoPrevio ? '; tipo previo ' + tipoPrevio : '';
        this.gestionExternaMensaje.set(
          'Retorno desde gestion externa (' +
            respuesta.bandejaActual +
            '; accion pendiente ' +
            respuesta.accionPendiente +
            sufijoTipo +
            ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  apremioReingresarSinPago(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoGestionExternaAccion() !== null) return;
    this.limpiarGestionExternaFeedback();
    this.ejecutandoGestionExternaAccion.set('APREMIO_SIN_PAGO');
    this.api.apremioReingresarSinPago(actaId).pipe(
      catchError((err) => { this.gestionExternaError.set(mensajeErrorHttp(err)); return of(null); }),
      finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => {
      if (!r) return;
      this.gestionExternaMensaje.set(r.mensaje);
      this.refrescarLuegoDeAccion(r);
    });
  }

  apremioRegistrarPago(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoGestionExternaAccion() !== null) return;
    this.limpiarGestionExternaFeedback();
    this.ejecutandoGestionExternaAccion.set('APREMIO_PAGO');
    this.api.apremioRegistrarPago(actaId).pipe(
      catchError((err) => { this.gestionExternaError.set(mensajeErrorHttp(err)); return of(null); }),
      finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => {
      if (!r) return;
      this.gestionExternaMensaje.set(r.mensaje);
      this.refrescarLuegoDeAccion(r);
    });
  }

  juzgadoReingresarAbsuelto(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoGestionExternaAccion() !== null) return;
    this.limpiarGestionExternaFeedback();
    this.ejecutandoGestionExternaAccion.set('JUZGADO_ABSUELTO');
    this.api.juzgadoReingresarAbsuelto(actaId).pipe(
      catchError((err) => { this.gestionExternaError.set(mensajeErrorHttp(err)); return of(null); }),
      finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => {
      if (!r) return;
      this.gestionExternaMensaje.set(r.mensaje);
      this.refrescarLuegoDeAccion(r);
    });
  }

  juzgadoReingresarCondenaConfirmada(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoGestionExternaAccion() !== null) return;
    this.limpiarGestionExternaFeedback();
    this.ejecutandoGestionExternaAccion.set('JUZGADO_CONDENA');
    this.api.juzgadoReingresarCondenaConfirmada(actaId).pipe(
      catchError((err) => { this.gestionExternaError.set(mensajeErrorHttp(err)); return of(null); }),
      finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => {
      if (!r) return;
      this.gestionExternaMensaje.set(r.mensaje);
      this.refrescarLuegoDeAccion(r);
    });
  }

  juzgadoReingresarMontoModificado(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.ejecutandoGestionExternaAccion() !== null) return;
    const montoStr = this.nuevoMontoJuzgado().trim();
    const monto = parseFloat(montoStr);
    if (!montoStr || isNaN(monto) || monto <= 0) {
      this.gestionExternaError.set('Ingrese un monto de condena válido mayor a 0.');
      return;
    }
    this.limpiarGestionExternaFeedback();
    this.ejecutandoGestionExternaAccion.set('JUZGADO_MONTO');
    this.api.juzgadoReingresarMontoModificado(actaId, monto).pipe(
      catchError((err) => { this.gestionExternaError.set(mensajeErrorHttp(err)); return of(null); }),
      finalize(() => this.ejecutandoGestionExternaAccion.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => {
      if (!r) return;
      this.gestionExternaMensaje.set(r.mensaje);
      this.nuevoMontoJuzgado.set('');
      this.refrescarLuegoDeAccion(r);
    });
  }

  reactivarActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoReactivacionAccion()) {
      return;
    }

    this.limpiarReactivacionFeedback();
    this.ejecutandoReactivacionAccion.set(true);

    this.api
      .reactivarActa(actaId)
      .pipe(
        catchError((err) => {
          this.reactivacionError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoReactivacionAccion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.reactivacionMensaje.set(
          'Acta reactivada correctamente (' + respuesta.bandejaActual + ').',
        );
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  abrirFormParalizarActa(): void {
    this.limpiarParalizarFeedback();
    this.paralizarFormAbierto.set(true);
  }

  cancelarParalizarActa(): void {
    this.paralizarFormAbierto.set(false);
    this.paralizarMotivoSeleccionado.set('');
    this.paralizarObservacion.set('');
    this.limpiarParalizarFeedback();
  }

  confirmarParalizarActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId) {
      return;
    }
    if (this.ejecutandoParalizarAccion()) {
      return;
    }
    const motivo = this.paralizarMotivoSeleccionado();
    if (!motivo) {
      return;
    }

    this.limpiarParalizarFeedback();
    this.ejecutandoParalizarAccion.set(true);

    const body: ParalizarActaRequestDemo = {
      motivo: motivo as MotivoParalizacionDemo,
      observacion: this.paralizarObservacion().trim() || null,
    };

    this.api
      .paralizarActa(actaId, body)
      .pipe(
        catchError((err) => {
          this.paralizarError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.ejecutandoParalizarAccion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.paralizarMensaje.set(
          'Acta paralizada correctamente (' + respuesta.bandeja + ').',
        );
        this.paralizarFormAbierto.set(false);
        this.paralizarMotivoSeleccionado.set('');
        this.paralizarObservacion.set('');
        this.refrescarLuegoDeAccion({ actaId: respuesta.actaId, bandejaActual: respuesta.bandeja });
      });
  }


  actaPuedeGenerarNulidadDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_NULIDAD);
  }

  actaPuedeGenerarMedidaPreventivaDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_MEDIDA_PREVENTIVA);
  }

  actaPuedeGenerarNotificacionActaDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_NOTIFICACION_ACTA);
  }

  actaPuedeGenerarResolucionDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_RESOLUCION);
  }

  actaPuedeGenerarRectificacionDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_RECTIFICACION);
  }

  hayAccionesRedaccionDisponiblesDesdeBackend(): boolean {
    return (
      this.actaPuedeGenerarNulidadDesdeBackend() ||
      this.actaPuedeGenerarMedidaPreventivaDesdeBackend() ||
      this.actaPuedeGenerarNotificacionActaDesdeBackend() ||
      this.actaPuedeGenerarResolucionDesdeBackend() ||
      this.actaPuedeGenerarRectificacionDesdeBackend()
    );
  }

  accionRedaccionEnCurso(): boolean {
    return (
      this.generandoNulidad() ||
      this.generandoMedidaPreventiva() ||
      this.generandoNotificacionActa() ||
      this.generandoResolucion() ||
      this.generandoRectificacion()
    );
  }

  private piezaRedaccionPendienteDesdeBackend(pieza: string): boolean {
    const det = this.detalle();
    if (!det) {
      return false;
    }
    const requeridas = det.piezasRequeridas ?? [];
    const generadas = det.piezasGeneradas ?? [];
    return (
      det.bandejaActual === BANDEJA_REDACCION &&
      requeridas.includes(pieza) &&
      !generadas.includes(pieza)
    );
  }

  actaMuestraSeccionPiezasRedaccion(det: ActaDetalleDemo): boolean {
    return (det.piezasRequeridas?.length ?? 0) > 0 || (det.piezasGeneradas?.length ?? 0) > 0;
  }

  generarNulidad(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.accionRedaccionEnCurso() || !this.actaPuedeGenerarNulidadDesdeBackend()) {
      return;
    }

    this.limpiarNulidadFeedback();
    this.generandoNulidad.set(true);

    this.api
      .generarNulidad(actaId)
      .pipe(
        catchError((err) => {
          this.nulidadError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.generandoNulidad.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.nulidadMensaje.set(this.mensajeRedaccionExitosa(respuesta));
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  generarMedidaPreventiva(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.accionRedaccionEnCurso() || !this.actaPuedeGenerarMedidaPreventivaDesdeBackend()) {
      return;
    }

    this.limpiarMedidaPreventivaFeedback();
    this.generandoMedidaPreventiva.set(true);

    this.api
      .generarMedidaPreventiva(actaId)
      .pipe(
        catchError((err) => {
          this.medidaPreventivaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.generandoMedidaPreventiva.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.medidaPreventivaMensaje.set(this.mensajeRedaccionExitosa(respuesta));
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  generarNotificacionActa(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.accionRedaccionEnCurso() || !this.actaPuedeGenerarNotificacionActaDesdeBackend()) {
      return;
    }

    this.limpiarNotificacionActaRedaccionFeedback();
    this.generandoNotificacionActa.set(true);

    this.api
      .generarNotificacionActa(actaId)
      .pipe(
        catchError((err) => {
          this.notificacionActaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.generandoNotificacionActa.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.notificacionActaMensaje.set(this.mensajeRedaccionExitosa(respuesta));
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  generarResolucion(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.accionRedaccionEnCurso() || !this.actaPuedeGenerarResolucionDesdeBackend()) {
      return;
    }

    this.limpiarResolucionPiezaFeedback();
    this.generandoResolucion.set(true);

    this.api
      .generarResolucion(actaId)
      .pipe(
        catchError((err) => {
          this.resolucionPiezaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.generandoResolucion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.resolucionPiezaMensaje.set(this.mensajeRedaccionExitosa(respuesta));
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  generarRectificacion(): void {
    const actaId = this.actaSeleccionadaId();
    if (!actaId || this.accionRedaccionEnCurso() || !this.actaPuedeGenerarRectificacionDesdeBackend()) {
      return;
    }

    this.limpiarRectificacionPiezaFeedback();
    this.generandoRectificacion.set(true);

    this.api
      .generarRectificacion(actaId)
      .pipe(
        catchError((err) => {
          this.rectificacionPiezaError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.generandoRectificacion.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.rectificacionPiezaMensaje.set(this.mensajeRedaccionExitosa(respuesta));
        this.refrescarLuegoDeAccion(respuesta);
      });
  }

  private mensajeRedaccionExitosa(respuesta: {
    mensaje: string;
    bandejaActual: string;
    estadoProcesoActual: string;
  }): string {
    return respuesta.mensaje + ' (' + respuesta.bandejaActual + '; ' + respuesta.estadoProcesoActual + ').';
  }

  badgesDe(acta: ActaResumenDemo): BadgeDemo[] {
    const permiteReingreso = (acta as ActaDetalleDemo).permiteReingreso;
    return badgesDesdeActaResumen(acta, { permiteReingreso });
  }

  claseBadge(badge: BadgeDemo): string {
    return 'badge-' + badge.tono;
  }

  etiquetaBadgeCompacta(valor: string): string {
    const texto = valor.trim();
    if (!texto) {
      return valor;
    }
    if (texto.startsWith('Falta resultado final')) {
      return 'Falta resultado final';
    }
    return ETIQUETA_BADGE_COMPACTA[texto] ?? this.humanizarCodigoVisual(texto);
  }

  tituloBadge(valor: string): string {
    return valor;
  }

  lecturaOperativa(det: ActaDetalleDemo): string | null {
    const texto = det.hechosMateriales?.lecturaOperativa?.trim();
    return texto ? texto : null;
  }

  pendientesBloqueantes(det: ActaDetalleDemo): string[] {
    return det.cerrabilidad?.pendientesBloqueantesCierre ?? [];
  }

  ejesHechosMateriales(det: ActaDetalleDemo): HechosMaterialesEjeDemo[] {
    return det.hechosMateriales?.ejes ?? [];
  }

  tienePiezasEnDetalle(det: ActaDetalleDemo): boolean {
    return (det.piezasRequeridas?.length ?? 0) > 0 || (det.piezasGeneradas?.length ?? 0) > 0;
  }

  etiquetaSiNo(valor: boolean): string {
    return valor ? 'Si' : 'No';
  }

  formatoFechaHora(valor: string | null | undefined): string | null {
    if (!valor?.trim()) {
      return null;
    }
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }
    return fecha.toLocaleString('es-AR', {
      dateStyle: 'short',
      timeStyle: 'short',
    });
  }

  transicionEvento(evento: EventoActaDemo): string | null {
    const origen = evento.bloqueOrigen?.trim();
    const destino = evento.bloqueDestino?.trim();
    if (origen && destino) {
      return `${origen} -> ${destino}`;
    }
    return origen || destino || null;
  }

  private humanizarCodigoVisual(valor: string): string {
    if (!/^[A-Z0-9_]+$/.test(valor)) {
      return valor;
    }
    const minusculas = valor.replace(/_/g, ' ').toLocaleLowerCase('es-AR');
    return minusculas.charAt(0).toLocaleUpperCase('es-AR') + minusculas.slice(1);
  }



  codigoQrActaSeleccionada(): string | null {
    const actaId = this.actaSeleccionadaId()?.trim();
    if (!actaId) {
      return null;
    }
    return codigoQrDemoParaActa(actaId);
  }

  abrirPortalInfractor(): void {
    const codigoQr = this.codigoQrActaSeleccionada();
    if (!codigoQr) {
      return;
    }
    const url = `/infractor/actas/${encodeURIComponent(codigoQr)}`;
    window.open(url, '_blank', 'noopener');
  }
  async copiarEstadoActaActual(): Promise<void> {
    if (!this.detalle()) {
      return;
    }

    this.copiaEstadoMensaje.set(null);
    this.copiaEstadoError.set(null);

    const snapshot = this.construirSnapshotCompactoActaActual();
    const texto = JSON.stringify(snapshot, null, 2);

    try {
      await navigator.clipboard.writeText(texto);
      this.copiaEstadoMensaje.set('Estado copiado.');
    } catch {
      this.copiaEstadoError.set('No se pudo copiar el estado.');
    }
  }

  construirSnapshotCompactoActaActual(): Record<string, unknown> {
    const det = this.detalle();
    if (!det) {
      return {};
    }

    const cerrabilidad = det.cerrabilidad;

    return {
      acta: det.numeroActa,
      actaId: det.id,
      bandeja: det.bandejaActual,
      estadoProceso: det.estadoProcesoActual,
      situacionAdministrativa: det.situacionAdministrativaActual,
      situacionPago: det.situacionPago ?? null,
      montoPagoVoluntario: det.montoPagoVoluntario ?? null,
      montoCondena: det.montoCondena ?? null,
      situacionPagoCondena: det.situacionPagoCondena ?? null,
      resultadoFinal: cerrabilidad?.resultadoFinal ?? null,
      cerrable: cerrabilidad?.cerrable ?? false,
      motivoCerrabilidad: cerrabilidad?.motivoNoCerrable ?? null,
      accionPendiente: det.accionPendiente ?? null,
      motivoParalizacion: det.motivoParalizacion ?? null,
      observacionParalizacion: det.observacionParalizacion ?? null,
      permiteReingreso: det.permiteReingreso ?? false,
      tipoGestionExterna: det.tipoGestionExterna ?? null,
      motivoArchivo: det.motivoArchivo ?? null,
      accionesUi: this.obtenerAccionesUiSnapshot(),
      piezas: {
        requeridas: [...(det.piezasRequeridas ?? [])],
        generadas: [...(det.piezasGeneradas ?? [])],
      },
      documentos: this.obtenerDocumentosCompactos(),
      bloqueantes: [...this.pendientesBloqueantes(det)],
      hechosMateriales: this.obtenerHechosMaterialesCompactos(det),
      eventosUltimos: this.obtenerEventosUltimosCompactos(),
      feedback: this.obtenerFeedbackSnapshot(),
    };
  }

  private obtenerAccionesUiSnapshot(): Record<string, boolean> {
    const det = this.detalle();
    const tieneCumplimientoMaterialAccionable =
      det != null &&
      this.pendientesBloqueantes(det).some((pendiente) =>
        this.puedeMostrarAccionCumplimientoMaterial(pendiente),
      );
    const tieneResolucionBloqueoCierreAccionable =
      det != null &&
      this.pendientesBloqueantes(det).some((pendiente) =>
        this.puedeMostrarAccionResolucionBloqueoCierre(pendiente),
      );

    return {
      pagoVoluntario: this.hayAccionesPagoDisponibles(),
      vencimientoPagoVoluntario: det?.accionesUi?.vencimientoPagoVoluntario === true,
      pagoCondena: this.accionesPagoCondenaDisponiblesDesdeBackend().includes('INFORMAR'),
      confirmarPagoCondena: this.accionesPagoCondenaDisponiblesDesdeBackend().includes('CONFIRMAR'),
      observarPagoCondena: this.accionesPagoCondenaDisponiblesDesdeBackend().includes('OBSERVAR'),
      cierre: this.puedeMostrarCierreActa(),
      archivoReingreso:
        !this.resultadoFinalOcultaArchivoReingresoDemo() &&
        (this.actaPuedeArchivarseDesdeBackend() || this.actaPuedeReingresarDesdeBackend()),
      gestionExterna:
        this.actaPuedeDerivarseAGestionExternaDesdeBackend() ||
        this.actaPuedeRetornarDesdeGestionExternaDesdeBackend(),
      notificacion: this.actaEsNotificableDesdeBackend(),
      apelacionPresencial: this.puedeMostrarRegistrarApelacionPresencial(),
      vencimientoPlazoApelacion: this.puedeMostrarRegistrarVencimientoPlazoApelacion(),
      resolucionApelacion: this.puedeMostrarResolucionApelacion(),
      reintentarNotificacion: this.actaPuedeReintentarNotificacionDesdeBackend(),
      cumplimientoMaterial: tieneCumplimientoMaterialAccionable,
      resolucionBloqueante: tieneResolucionBloqueoCierreAccionable,
      redaccion: this.hayAccionesRedaccionDisponiblesDesdeBackend(),
      firmaPendiente: this.documentos().some((doc) => this.documentoEsFirmable(doc)),
      falloFondo: this.puedeMostrarFalloResolucionFondo(),
      consentirCondenaYRegistrarPago: this.consentirCondenaYRegistrarPagoHabilitado(),
      paralizarActa: det?.accionesUi?.paralizarActa ?? false,
    };
  }

  private obtenerDocumentosCompactos(): string[] {
    return this.documentos().map((doc) => `${doc.tipoDocumento}:${doc.estadoDocumento}`);
  }

  private obtenerEventosUltimosCompactos(cantidad = 5): string[] {
    const items = this.eventos();
    if (items.length === 0) {
      return [];
    }

    return items.slice(-cantidad).map((evento) => {
      const origen = evento.bloqueOrigen?.trim() ?? '';
      const destino = evento.bloqueDestino?.trim() ?? '';
      const transicion =
        origen && destino ? `${origen}->${destino}` : origen || destino || 'sin-transicion';
      return `${evento.tipoEvento}:${transicion}`;
    });
  }

  private obtenerHechosMaterialesCompactos(det: ActaDetalleDemo): string[] {
    return this.ejesHechosMateriales(det).map((eje) => {
      if (eje.ejeBloqueanteCierre) {
        return `${eje.clave}:${eje.fase}:${eje.ejeBloqueanteCierre}`;
      }
      if (eje.fase === 'NO_APLICA') {
        return `${eje.clave}:NO_APLICA`;
      }
      return `${eje.clave}:${eje.fase}`;
    });
  }

  private obtenerFeedbackSnapshot(): { ultimo: string | null; error: string | null } {
    const mensajes = [
      this.firmaDocumentoMensaje(),
      this.cumplimientoMaterialMensaje(),
      this.resolucionBloqueoCierreMensaje(),
      this.cierreActaMensaje(),
      this.notificacionMensaje(),
      this.apelacionMensaje(),
      this.vencimientoPlazoApelacionMensaje(),
      this.resolucionApelacionMensaje(),
      this.pagoMensaje(),
      this.archivoMensaje(),
      this.gestionExternaMensaje(),
      this.nulidadMensaje(),
      this.medidaPreventivaMensaje(),
      this.notificacionActaMensaje(),
      this.falloMensaje(),
    ].filter((valor): valor is string => !!valor?.trim());

    const errores = [
      this.firmaDocumentoError(),
      this.cumplimientoMaterialError(),
      this.resolucionBloqueoCierreError(),
      this.cierreActaError(),
      this.notificacionError(),
      this.apelacionError(),
      this.vencimientoPlazoApelacionError(),
      this.resolucionApelacionError(),
      this.pagoError(),
      this.archivoError(),
      this.gestionExternaError(),
      this.nulidadError(),
      this.medidaPreventivaError(),
      this.notificacionActaError(),
      this.falloError(),
    ].filter((valor): valor is string => !!valor?.trim());

    return {
      ultimo: mensajes.at(-1) ?? null,
      error: errores.at(-1) ?? null,
    };
  }

  private limpiarSubRecursosDetalle(): void {
    this.documentos.set([]);
    this.documentosEstado.set('idle');
    this.documentosError.set(null);
    this.eventos.set([]);
    this.eventosEstado.set('idle');
    this.eventosError.set(null);
  }

  private limpiarCopiaEstadoFeedback(): void {
    this.copiaEstadoMensaje.set(null);
    this.copiaEstadoError.set(null);
  }

  private limpiarFirmaFeedback(): void {
    this.firmaDocumentoError.set(null);
    this.firmaDocumentoMensaje.set(null);
  }

  private limpiarCumplimientoMaterialFeedback(): void {
    this.cumplimientoMaterialError.set(null);
    this.cumplimientoMaterialMensaje.set(null);
  }

  private limpiarResolucionBloqueoCierreFeedback(): void {
    this.resolucionBloqueoCierreError.set(null);
    this.resolucionBloqueoCierreMensaje.set(null);
  }

  private limpiarCierreActaFeedback(): void {
    this.cierreActaError.set(null);
    this.cierreActaMensaje.set(null);
  }

  private limpiarNotificacionFeedback(): void {
    this.notificacionError.set(null);
    this.notificacionMensaje.set(null);
  }

  private limpiarApelacionFeedback(): void {
    this.apelacionError.set(null);
    this.apelacionMensaje.set(null);
  }

  private limpiarVencimientoPlazoApelacionFeedback(): void {
    this.vencimientoPlazoApelacionError.set(null);
    this.vencimientoPlazoApelacionMensaje.set(null);
  }

  private limpiarConsentirCondenaYRegistrarPagoFeedback(): void {
    this.consentirCondenaYRegistrarPagoError.set(null);
    this.consentirCondenaYRegistrarPagoMensaje.set(null);
  }

  private limpiarResolucionApelacionFeedback(): void {
    this.resolucionApelacionError.set(null);
    this.resolucionApelacionMensaje.set(null);
  }

  private limpiarPagoFeedback(): void {
    this.pagoError.set(null);
    this.pagoMensaje.set(null);
  }

  private limpiarPagoCondenaFeedback(): void {
    this.pagoCondenaError.set(null);
    this.pagoCondenaMensaje.set(null);
  }

  private limpiarMontoPagoVoluntarioInput(): void {
    this.montoPagoVoluntarioInput.set('');
  }

  private limpiarMontoCondenaInput(): void {
    this.montoCondenaInput.set('');
  }

  private limpiarArchivoFeedback(): void {
    this.archivoError.set(null);
    this.archivoMensaje.set(null);
  }

  private limpiarGestionExternaFeedback(): void {
    this.gestionExternaError.set(null);
    this.gestionExternaMensaje.set(null);
  }


  private limpiarReactivacionFeedback(): void {
    this.reactivacionError.set(null);
    this.reactivacionMensaje.set(null);
  }

  private limpiarParalizarFeedback(): void {
    this.paralizarError.set(null);
    this.paralizarMensaje.set(null);
  }
  private limpiarNulidadFeedback(): void {
    this.nulidadError.set(null);
    this.nulidadMensaje.set(null);
  }

  private limpiarMedidaPreventivaFeedback(): void {
    this.medidaPreventivaError.set(null);
    this.medidaPreventivaMensaje.set(null);
  }

  private limpiarNotificacionActaRedaccionFeedback(): void {
    this.notificacionActaError.set(null);
    this.notificacionActaMensaje.set(null);
  }

  private limpiarResolucionPiezaFeedback(): void {
    this.resolucionPiezaError.set(null);
    this.resolucionPiezaMensaje.set(null);
  }

  private limpiarRectificacionPiezaFeedback(): void {
    this.rectificacionPiezaError.set(null);
    this.rectificacionPiezaMensaje.set(null);
  }

  private limpiarFalloFeedback(): void {
    this.falloError.set(null);
    this.falloMensaje.set(null);
  }

  private limpiarRedaccionFeedback(): void {
    this.limpiarNulidadFeedback();
    this.limpiarMedidaPreventivaFeedback();
    this.limpiarNotificacionActaRedaccionFeedback();
    this.limpiarResolucionPiezaFeedback();
    this.limpiarRectificacionPiezaFeedback();
  }

  private cargarSubRecursosDetalle(actaId: string): void {
    this.cargarDocumentos(actaId);
    this.cargarEventos(actaId);
  }


  private restaurarPreferenciaSeguirActa(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      const raw = localStorage.getItem(STORAGE_SEGUIR_ACTA_AL_CAMBIAR_BANDEJA);
      if (raw === null) {
        return;
      }
      this.seguirActaAlCambiarBandeja.set(JSON.parse(raw) === true);
    } catch {
      this.seguirActaAlCambiarBandeja.set(false);
    }
  }

  private leerYAplicarQueryParamsIniciales(): void {
    const params = this.route.snapshot.queryParams;

    const bandeja = params['bandeja'];
    if (bandeja && this.esBandejaCodigo(bandeja)) {
      this.bandejaSeleccionada.set(bandeja as BandejaLateralCodigo);
    }

    const filtro = params['filtro'];
    if (filtro) {
      this.subBandejaSeleccionada.set(filtro);
    }

    const dependencia = params['dependencia'];
    if (dependencia) {
      this.dependenciaDemoSeleccionada.set(dependencia);
    }

    const q = params['q'];
    if (q) {
      this.textoBusquedaActa.set(q);
    }

    const seguirActa = params['seguirActa'];
    if (seguirActa === 'true') {
      this.seguirActaAlCambiarBandeja.set(true);
    }

    const acta = params['acta'];
    if (acta) {
      this.actaToRestoreFromUrl = acta;
    }

    const busquedaGlobal = params['busquedaGlobal'];
    if (busquedaGlobal === 'true') {
      this.busquedaGlobal.set(true);
    }
  }

  private actualizarQueryParams(
    params: {
      bandeja?: string | null;
      filtro?: string | null;
      dependencia?: string | null;
      acta?: string | null;
      q?: string | null;
      seguirActa?: boolean | null;
      busquedaGlobal?: boolean | null;
    },
    replace = true,
  ): void {
    if (this.restoringFromUrl) {
      return;
    }
    if (this.enCorreoPostal()) {
      return;
    }
    const current: Record<string, string> = { ...this.route.snapshot.queryParams };
    const next: Record<string, string> = { ...current };

    const aplicar = (key: string, valor: string | boolean | null | undefined): void => {
      if (valor === null || valor === undefined || valor === '' || valor === false) {
        delete next[key];
      } else {
        next[key] = String(valor);
      }
    };

    if ('bandeja' in params) aplicar('bandeja', params.bandeja);
    if ('filtro' in params) aplicar('filtro', params.filtro);
    if ('dependencia' in params) aplicar('dependencia', params.dependencia);
    if ('acta' in params) aplicar('acta', params.acta);
    if ('q' in params) aplicar('q', params.q);
    if ('seguirActa' in params) aplicar('seguirActa', params.seguirActa);
    if ('busquedaGlobal' in params) aplicar('busquedaGlobal', params.busquedaGlobal);

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: next,
      replaceUrl: replace,
    });
  }


  private actaBandejaDesdeDetalle(det: ActaDetalleDemo): ActaBandejaItem {
    return {
      id: det.id,
      numeroActa: det.numeroActa,
      infractorNombre: det.infractorNombre,
      bloqueActual: det.bloqueActual,
      estadoProcesoActual: det.estadoProcesoActual,
      situacionAdministrativaActual: det.situacionAdministrativaActual,
      bandejaActual: det.bandejaActual,
      situacionPago: det.situacionPago,
      accionPendiente: det.accionPendiente,
      motivoArchivo: det.motivoArchivo,
      tipoGestionExterna: det.tipoGestionExterna,
      cerrabilidad: det.cerrabilidad,
      subBandeja: '',
      subBandejaLabel: '',
      chip: null,
      accionPrincipal: det.accionPendiente,
      prioridadSubBandeja: 0,
      chipsSecundarios: [],
      dependenciaDemo: det.dependenciaDemo,
    };
  }

  private limpiarSeguimientoActaFeedback(): void {
    this.seguimientoActaMensaje.set(null);
  }

  private esBandejaCodigo(valor: string): valor is BandejaLateralCodigo {
    return esBandejaLateralCodigo(valor);
  }

  private esBandejaBackendCodigo(valor: string): valor is BandejaCodigo {
    return esBandejaCodigoDemo(valor);
  }

  private refrescarLuegoDeAccion(respuesta: AccionConBandejaResponseLike | null | undefined): void {
    this.cargarBandejasResumen();
    const actaId = respuesta?.actaId ?? this.actaSeleccionadaId();
    const bandejaNueva = respuesta?.bandejaActual;

    if (
      this.seguirActaAlCambiarBandeja() &&
      bandejaNueva &&
      this.esBandejaBackendCodigo(bandejaNueva) &&
      bandejaBackendALateral(bandejaNueva) !== this.bandejaSeleccionada()
    ) {
      this.cambiarBandejaYSeguirActa(bandejaNueva, actaId);
      return;
    }

    this.refrescarActaSeleccionadaDesdeBackend(actaId ?? null);
  }

  private cambiarBandejaYSeguirActa(bandejaBackend: BandejaCodigo, actaId: string | null): void {
    this.limpiarSeguimientoActaFeedback();
    this.subBandejaSeleccionada.set(null);
    this.bandejaSeleccionada.set(bandejaBackendALateral(bandejaBackend));
    this.actualizarQueryParams({ bandeja: bandejaBackendALateral(bandejaBackend), filtro: null }, false);
    this.limpiarFirmaFeedback();
    this.limpiarCumplimientoMaterialFeedback();
    this.limpiarResolucionBloqueoCierreFeedback();
    this.limpiarCierreActaFeedback();
    this.limpiarNotificacionFeedback();
    this.limpiarPagoFeedback();
    this.limpiarPagoCondenaFeedback();
    this.limpiarApelacionFeedback();
    this.limpiarVencimientoPlazoApelacionFeedback();
    this.limpiarResolucionApelacionFeedback();
    this.limpiarMontoPagoVoluntarioInput();
    this.limpiarMontoCondenaInput();
    this.limpiarArchivoFeedback();
    this.limpiarGestionExternaFeedback();
    this.limpiarReactivacionFeedback();
    this.limpiarParalizarFeedback();
    this.paralizarFormAbierto.set(false);
    this.paralizarMotivoSeleccionado.set('');
    this.paralizarObservacion.set('');
    this.limpiarRedaccionFeedback();
    this.limpiarFalloFeedback();
    this.limpiarCopiaEstadoFeedback();

    if (actaId) {
      this.actaSeleccionadaId.set(actaId);
      this.cargarDetalle();
      this.cargarListado({ actaIdScrollObjetivo: actaId, evaluarSeguimientoBandeja: true });
      return;
    }

    this.actaSeleccionadaId.set(null);
    this.detalle.set(null);
    this.detalleEstado.set('idle');
    this.limpiarSubRecursosDetalle();
    this.cargarListado();
  }

  private programarScrollActaSeleccionada(actaId: string): void {
    this.pendingScrollToActaId = actaId;
    this.pendingScrollRetryDisponible = true;
    this.programarIntentoScrollActaPendiente();
  }

  private programarIntentoScrollActaPendiente(): void {
    this.ngZone.onStable
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const actaId = this.pendingScrollToActaId;
        if (actaId) {
          this.scrollSelectedActaIntoView(actaId, 0);
        }
      });
  }

  private reintentarScrollPendienteAlRenderizarListadoInterno(): void {
    if (!this.pendingScrollToActaId) {
      return;
    }
    // Mantener retry activo: el listado puede re-renderizarse dos veces
    // (spinner loading y filas finales) durante un refresh automático.
    this.pendingScrollRetryDisponible = true;
    this.programarIntentoScrollActaPendiente();
  }

  private scrollSelectedActaIntoView(actaId: string, attempt: number): void {
    if (this.pendingScrollToActaId !== actaId) {
      return;
    }

    const container = this.actaListComponent?.contenedor;
    if (!container) {
      if (attempt === 0) {
        setTimeout(() => this.scrollSelectedActaIntoView(actaId, 0));
      }
      return;
    }

    const row =
this.actaListComponent?.buscarFila(actaId) ?? null;

    if (!row) {
      if (this.pendingScrollRetryDisponible && attempt === 0) {
        return;
      }
      if (attempt === 0) {
        setTimeout(() => this.scrollSelectedActaIntoView(actaId, 0));
        return;
      }
      this.limpiarScrollActaPendiente();
      return;
    }

    if (this.esFilaVisibleEnContenedor(row, container)) {
      this.limpiarScrollActaPendiente();
      return;
    }

    this.desplazarFilaEnContenedor(row, container, attempt === 0 ? 'smooth' : 'auto');
    this.programarVerificacionScrollActa(actaId, attempt);
  }

  private esFilaVisibleEnContenedor(
    row: HTMLElement,
    container: HTMLElement,
    margin = DemoShellComponent.SCROLL_VISIBILITY_MARGIN_PX,
  ): boolean {
    const containerRect = container.getBoundingClientRect();
    const rowRect = row.getBoundingClientRect();
    return (
      rowRect.top >= containerRect.top + margin &&
      rowRect.bottom <= containerRect.bottom - margin
    );
  }

  private desplazarFilaEnContenedor(
    row: HTMLElement,
    container: HTMLElement,
    behavior: ScrollBehavior,
  ): void {
    const containerRect = container.getBoundingClientRect();
    const rowRect = row.getBoundingClientRect();
    const offsetObjetivo = Math.max(24, container.clientHeight / 4);
    const delta = rowRect.top - containerRect.top;
    const top = container.scrollTop + delta - offsetObjetivo;
    container.scrollTo({ top: Math.max(0, top), behavior });
  }

  private programarVerificacionScrollActa(actaId: string, attempt: number): void {
    this.cancelarVerificacionScrollActa();
    this.scrollVerifyTimeoutId = setTimeout(() => {
      this.scrollVerifyTimeoutId = null;
      if (this.pendingScrollToActaId !== actaId) {
        return;
      }

      const container = this.actaListComponent?.contenedor;
      const row = container?.querySelector<HTMLElement>(`[data-acta-id="${CSS.escape(actaId)}"]`) ?? null;
      if (!container || !row) {
        if (this.listadoEstado() === 'loading') {
          this.pendingScrollRetryDisponible = true;
          this.programarIntentoScrollActaPendiente();
          return;
        }
        this.limpiarScrollActaPendiente();
        return;
      }

      if (this.esFilaVisibleEnContenedor(row, container)) {
        this.limpiarScrollActaPendiente();
        return;
      }

      if (attempt === 0) {
        this.scrollSelectedActaIntoView(actaId, 1);
        return;
      }

      this.limpiarScrollActaPendiente();
    }, DemoShellComponent.SCROLL_VERIFY_DELAY_MS);
  }

  private cancelarVerificacionScrollActa(): void {
    if (this.scrollVerifyTimeoutId !== null) {
      clearTimeout(this.scrollVerifyTimeoutId);
      this.scrollVerifyTimeoutId = null;
    }
  }

  private limpiarScrollActaPendiente(): void {
    this.cancelarVerificacionScrollActa();
    this.pendingScrollToActaId = null;
    this.pendingScrollRetryDisponible = false;
  }
  private refrescarActaSeleccionadaDesdeBackend(actaIdObjetivo: string | null): void {
    this.cargarDetalle();
    this.cargarListado({ actaIdScrollObjetivo: actaIdObjetivo });
  }

  private cargarListadoTrasAlta(actaId: string): void {
    this.actaSeleccionadaId.set(actaId);
    this.cargarListado({ bandeja: 'ACTAS_EN_ENRIQUECIMIENTO', actaIdScrollObjetivo: actaId });
  }

  private cargarBandejasResumen(): void {
    this.api
      .listarBandejas()
      .pipe(
        catchError(() => of([] as BandejaResponse[])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((bandejas) => {
        this.bandejasBackendCodigos.set(codigosBandejaEnResumen(bandejas));
        const lateral = transformarBandejasParaLateral(bandejas);
        this.bandejasResumen.set(lateral);
        this.bandejasLateral.set(filtrarBandejasLateralVisibles(lateral));
      });
  }

  private cargarListado(opciones?: {
    actaIdScrollObjetivo?: string | null;
    evaluarSeguimientoBandeja?: boolean;
    bandeja?: BandejaLateralCodigo;
    subBandeja?: string | null;
  }): void {
    const actaIdScrollObjetivo = opciones?.actaIdScrollObjetivo ?? null;
    const evaluarSeguimientoBandeja = opciones?.evaluarSeguimientoBandeja ?? false;
    const bandeja = opciones?.bandeja ?? this.bandejaSeleccionada();
    const subBandeja = opciones?.subBandeja ?? this.subBandejaSeleccionada();

    this.cargarBandejasResumen();
    this.listadoEstado.set('loading');
    this.listadoError.set(null);

    const consultas = subBandejaBackendParaConsulta(bandeja, subBandeja, this.bandejasBackendCodigos());
    const peticiones = consultas.map((consulta) =>
      this.api.listarActasPorBandeja(consulta.bandeja, consulta.subBandeja).pipe(
        catchError((err) => {
          this.listadoError.set(mensajeErrorHttp(err));
          this.listadoEstado.set('error');
          return of([] as ActaBandejaItem[]);
        }),
      ),
    );

    forkJoin(peticiones.length > 0 ? peticiones : [of([] as ActaBandejaItem[])])
      .pipe(
        map((listas) => {
          const dedupe = new Map<string, ActaBandejaItem>();
          for (const lista of listas) {
            for (const acta of lista) {
              dedupe.set(acta.id, acta);
            }
          }
          return aplicarFiltroOperativoLateral([...dedupe.values()], bandeja, subBandeja);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        this.actas.set(items);
        this.limpiarDependenciaSeleccionadaSiInvalida();
        if (this.listadoEstado() !== 'error') {
          this.listadoEstado.set('ready');
        }

        if (!actaIdScrollObjetivo) {
          return;
        }

        if (evaluarSeguimientoBandeja) {
          const encontrada = items.some((acta) => acta.id === actaIdScrollObjetivo);
          if (!encontrada) {
            const bandejaResumen = this.bandejasResumen().find((item) => item.codigo === bandeja);
            const labelBandeja = bandejaResumen?.label ?? etiquetaBandeja(bandeja);
            const subLabel = subBandeja
              ? bandejaResumen?.subBandejas.find((sub) => sub.codigo === subBandeja)?.label ?? subBandeja
              : null;
            const destino = subLabel ? labelBandeja + ' › ' + subLabel : labelBandeja;
            this.seguimientoActaMensaje.set(
              'El acta sigue seleccionada, pero no aparece en el listado de ' + destino + '.',
            );
            return;
          }
        }

        if (this.actaSeleccionadaId() === actaIdScrollObjetivo) {
          this.programarScrollActaSeleccionada(actaIdScrollObjetivo);
        }
      });
  }

  private limpiarDependenciaSeleccionadaSiInvalida(): void {
    const seleccionada = this.dependenciaDemoSeleccionada();
    if (!seleccionada) {
      return;
    }
    if (seleccionada === SIN_DEPENDENCIA_RESUMEN) {
      const tieneSinDependencia = this.actas().some((acta) => !acta.dependenciaDemo?.trim());
      if (!tieneSinDependencia) {
        this.dependenciaDemoSeleccionada.set(null);
        this.actualizarQueryParams({ dependencia: null }, true);
      }
      return;
    }
    const disponibles = new Set(
      this.dependenciasDisponiblesEnListado().map((dependencia) => dependencia.codigo),
    );
    if (!disponibles.has(seleccionada)) {
      this.dependenciaDemoSeleccionada.set(null);
      this.actualizarQueryParams({ dependencia: null }, true);
    }
  }

  private cargarDetalle(): void {
    const id = this.actaSeleccionadaId();
    if (!id) {
      return;
    }
    this.detalleEstado.set('loading');
    this.detalleError.set(null);
    this.detalle.set(null);
    this.limpiarSubRecursosDetalle();
    this.cargarSubRecursosDetalle(id);

    this.api
      .obtenerActa(id)
      .pipe(
        catchError((err) => {
          this.detalleError.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => {
          if (this.detalleEstado() === 'loading' && !this.detalle() && !this.detalleError()) {
            this.detalleEstado.set('error');
            this.detalleError.set('No se recibio detalle del acta.');
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((det) => {
        if (det) {
          this.detalle.set(det);
          this.detalleEstado.set('ready');
        } else if (this.detalleError()) {
          this.detalleEstado.set('error');
        }
      });
  }

  private cargarDocumentos(actaId: string): void {
    this.documentosEstado.set('loading');
    this.documentosError.set(null);
    this.documentos.set([]);
    this.api
      .listarDocumentosActa(actaId)
      .pipe(
        catchError((err) => {
          this.documentosError.set(mensajeErrorHttp(err));
          this.documentosEstado.set('error');
          return of([] as DocumentoActaDemo[]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        if (this.documentosEstado() !== 'error') {
          this.documentos.set(items);
          this.documentosEstado.set('ready');
        }
      });
  }

  private cargarEventos(actaId: string): void {
    this.eventosEstado.set('loading');
    this.eventosError.set(null);
    this.eventos.set([]);
    this.api
      .listarEventosActa(actaId)
      .pipe(
        catchError((err) => {
          this.eventosError.set(mensajeErrorHttp(err));
          this.eventosEstado.set('error');
          return of([] as EventoActaDemo[]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        if (this.eventosEstado() !== 'error') {
          this.eventos.set(items);
          this.eventosEstado.set('ready');
        }
      });
  }
}

function payloadCrearActaMockDemo(dependencia: DependenciaActaDemo): CrearActaMockDemoRequest {
  switch (dependencia) {
    case 'TRANSITO':
      return {
        dependencia: 'TRANSITO',
        ejeUrbano: true,
        rodadoRetenidoOSecuestrado: true,
        documentacionRetenida: true,
      };
    case 'INSPECCIONES':
      return {
        dependencia: 'INSPECCIONES',
        medidaPreventivaClausura: true,
      };
    case 'FISCALIZACION':
      return {
        dependencia: 'FISCALIZACION',
        medidaPreventivaParalizacionObra: true,
      };
    case 'BROMATOLOGIA':
      return {
        dependencia: 'BROMATOLOGIA',
        decomisoSustanciasAlimenticias: true,
      };
  }
}

function mensajeErrorHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    if (status === 0) {
      return 'Sin conexion con el backend prototipo.';
    }
    return 'Error HTTP ' + String(status);
  }
  return 'Error inesperado al consultar el backend.';
}

function mensajeErrorDictarFalloHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    if (status === 409) {
      return 'No es posible dictar fallo en este momento.';
    }
    if (status === 0) {
      return 'Sin conexion con el backend prototipo.';
    }
    return 'Error HTTP ' + String(status);
  }
  return 'Error inesperado al consultar el backend.';
}

function mensajeErrorResolverApelacionHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    if (status === 409) {
      return 'No es posible resolver la apelación en este momento.';
    }
    if (status === 400) {
      return 'Resultado de apelación inválido.';
    }
    if (status === 0) {
      return 'Sin conexion con el backend prototipo.';
    }
    return 'Error HTTP ' + String(status);
  }
  return 'Error inesperado al consultar el backend.';
}








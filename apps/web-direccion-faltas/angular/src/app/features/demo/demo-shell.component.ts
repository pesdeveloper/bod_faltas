import { AfterViewInit, Component, DestroyRef, ElementRef, NgZone, OnInit, QueryList, ViewChild, ViewChildren, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Observable, catchError, finalize, of, take } from 'rxjs';
import { BANDEJAS_DEMO, etiquetaBandeja } from '../../core/constants/bandejas-demo.constants';
import {
  AccionPagoCondenaDemo,
  AccionPagoVoluntarioDemo,
  ActaDetalleDemo,
  ActaResumenDemo,
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
} from '../../core/models/prototipo-faltas.models';
import { badgesDesdeActaResumen } from '../../core/services/acta-badges.presenter';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type CargaEstado = 'idle' | 'loading' | 'ready' | 'error';

const ESTADO_DOCUMENTO_PENDIENTE_FIRMA = 'PENDIENTE_FIRMA';
const BANDEJA_REDACCION = 'PENDIENTES_RESOLUCION_REDACCION';
const BANDEJA_PENDIENTE_FIRMA = 'PENDIENTE_FIRMA';
const PIEZA_NULIDAD = 'NULIDAD';
const PIEZA_MEDIDA_PREVENTIVA = 'MEDIDA_PREVENTIVA';
const PIEZA_NOTIFICACION_ACTA = 'NOTIFICACION_ACTA';
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
type TipoFalloDemo = 'ABSOLUTORIO' | 'CONDENATORIO';

const TIPOS_CUMPLIMIENTO_MATERIAL: ReadonlyArray<TipoCumplimientoMaterialBloqueante> = [
  'LEVANTAMIENTO_MEDIDA_PREVENTIVA',
  'LIBERACION_RODADO',
  'ENTREGA_DOCUMENTACION',
];

const ETIQUETA_CUMPLIMIENTO_MATERIAL: Record<TipoCumplimientoMaterialBloqueante, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Registrar levantamiento material',
  LIBERACION_RODADO: 'Registrar liberacion de rodado',
  ENTREGA_DOCUMENTACION: 'Registrar entrega de documentacion',
};

// Fases del eje material reportadas por el backend (HechosMaterialesEje.fase).
// La etapa SITUACION_PENDIENTE_DE_RESOLUTORIO exige documentar el resolutorio
// antes que cualquier registro de cumplimiento material efectivo.
const FASE_EJE_PENDIENTE_RESOLUTORIO = 'SITUACION_PENDIENTE_DE_RESOLUTORIO';
const FASE_EJE_RESOLUTORIO_EN_EXPEDIENTE = 'RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL';

const ETIQUETA_RESOLUCION_BLOQUEO_CIERRE: Record<TipoResolucionBloqueoCierre, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Resolver levantamiento de medida preventiva',
  LIBERACION_RODADO: 'Resolver liberacion de rodado',
  ENTREGA_DOCUMENTACION: 'Resolver entrega de documentacion',
};

const ETIQUETA_RESOLUCION_BLOQUEO_CIERRE_EN_CURSO: Record<TipoResolucionBloqueoCierre, string> = {
  LEVANTAMIENTO_MEDIDA_PREVENTIVA: 'Resolviendo levantamiento...',
  LIBERACION_RODADO: 'Resolviendo liberacion...',
  ENTREGA_DOCUMENTACION: 'Resolviendo entrega...',
};

const BANDEJAS_NOTIFICABLES: ReadonlyArray<BandejaCodigo> = [
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
];

const ETIQUETA_RESULTADO_NOTIFICACION: Record<ResultadoNotificacionDemo, string> = {
  POSITIVA: 'Registrar notificacion positiva',
  NEGATIVA: 'Registrar notificacion negativa',
  VENCIDA: 'Registrar notificacion vencida',
};

const ETIQUETA_ACCION_PAGO: Record<AccionPagoVoluntarioDemo, string> = {
  SOLICITAR: 'Pago voluntario',
  INFORMAR: 'Informar pago',
  ADJUNTAR_COMPROBANTE: 'Adjuntar comprobante mock',
  CONFIRMAR: 'Confirmar pago',
  OBSERVAR: 'Observar pago',
};

const ETIQUETA_ACCION_PAGO_EN_CURSO: Record<AccionPagoVoluntarioDemo, string> = {
  SOLICITAR: 'Registrando...',
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
  CONFIRMAR: 'Confirmar pago de condena',
  OBSERVAR: 'Observar pago de condena',
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
  ACTAS_EN_ENRIQUECIMIENTO: 'ENR',
  PENDIENTE_ANALISIS: 'ANA',
  PENDIENTES_RESOLUCION_REDACCION: 'RED',
  PENDIENTE_FIRMA: 'FIR',
  PENDIENTE_NOTIFICACION: 'NOT',
  EN_NOTIFICACION: 'ENV',
  GESTION_EXTERNA: 'EXT',
  ARCHIVO: 'ARC',
  CERRADAS: 'CER',
};

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
  ],
  templateUrl: './demo-shell.component.html',
  styleUrl: './demo-shell.component.scss',
})
export class DemoShellComponent implements OnInit, AfterViewInit {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ngZone = inject(NgZone);
  @ViewChild('actasListContainer') private readonly actasListContainer?: ElementRef<HTMLElement>;
  @ViewChildren('actaRow') private readonly actaRows?: QueryList<ElementRef<HTMLElement>>;

  readonly bandejas = BANDEJAS_DEMO;
  readonly etiquetaBandeja = etiquetaBandeja;
  readonly bandejasColapsadas = signal<boolean>(false);

  readonly bandejaSeleccionada = signal<BandejaCodigo>('ACTAS_EN_ENRIQUECIMIENTO');
  readonly actas = signal<ActaResumenDemo[]>([]);
  readonly actaSeleccionadaId = signal<string | null>(null);
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

  readonly resolviendoApelacion = signal<boolean>(false);
  readonly resolucionApelacionError = signal<string | null>(null);
  readonly resolucionApelacionMensaje = signal<string | null>(null);

  readonly ejecutandoPagoAccion = signal<AccionPagoVoluntarioDemo | null>(null);
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

  // Accion de gestion externa en curso. APREMIO/JUZGADO_DE_PAZ corresponden
  // a las dos derivaciones tipadas del backend; RETORNAR corresponde al
  // reingreso desde GESTION_EXTERNA. Solo una accion puede estar en curso
  // a la vez para evitar conflictos en el detalle/listado refrescados.
  readonly ejecutandoGestionExternaAccion = signal<TipoGestionExternaDemo | 'RETORNAR' | null>(null);
  readonly gestionExternaError = signal<string | null>(null);
  readonly gestionExternaMensaje = signal<string | null>(null);

  readonly generandoNulidad = signal<boolean>(false);
  readonly nulidadError = signal<string | null>(null);
  readonly nulidadMensaje = signal<string | null>(null);

  readonly generandoMedidaPreventiva = signal<boolean>(false);
  readonly medidaPreventivaError = signal<string | null>(null);
  readonly medidaPreventivaMensaje = signal<string | null>(null);

  readonly generandoNotificacionActa = signal<boolean>(false);
  readonly notificacionActaError = signal<string | null>(null);
  readonly notificacionActaMensaje = signal<string | null>(null);

  readonly dictandoFallo = signal<TipoFalloDemo | null>(null);
  readonly falloError = signal<string | null>(null);
  readonly falloMensaje = signal<string | null>(null);

  readonly textoBusquedaActa = signal('');
  readonly seguirActaAlCambiarBandeja = signal(false);
  readonly seguimientoActaMensaje = signal<string | null>(null);

  readonly copiaEstadoMensaje = signal<string | null>(null);
  readonly copiaEstadoError = signal<string | null>(null);
  private static readonly SCROLL_VISIBILITY_MARGIN_PX = 8;
  private static readonly SCROLL_VERIFY_DELAY_MS = 150;

  private pendingScrollToActaId: string | null = null;
  private pendingScrollRetryDisponible = false;
  private scrollVerifyTimeoutId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.restaurarPreferenciaSeguirActa();
    this.cargarListado();
  }

  ngAfterViewInit(): void {
    this.actaRows?.changes.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.reintentarScrollPendienteAlRenderizarListado();
    });
  }

  actasFiltradas(): ActaResumenDemo[] {
    const termino = this.textoBusquedaActa().trim().toLowerCase();
    const items = this.actas();
    if (!termino) {
      return items;
    }
    return items.filter((acta) => acta.numeroActa.trim().toLowerCase().includes(termino));
  }

  actualizarTextoBusquedaActa(valor: string): void {
    this.textoBusquedaActa.set(valor);
  }

  cambiarSeguirActaAlCambiarBandeja(activo: boolean): void {
    this.seguirActaAlCambiarBandeja.set(activo);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_SEGUIR_ACTA_AL_CAMBIAR_BANDEJA, JSON.stringify(activo));
    }
  }


  tituloBandejaActual(): string {
    return etiquetaBandeja(this.bandejaSeleccionada());
  }

  toggleBandejas(): void {
    this.bandejasColapsadas.update((colapsadas) => !colapsadas);
  }

  abreviaturaBandeja(bandeja: BandejaCodigo): string {
    return BANDEJA_ABREVIATURAS[bandeja];
  }

  seleccionarBandeja(bandeja: BandejaCodigo): void {
    if (this.bandejaSeleccionada() === bandeja) {
      return;
    }
    this.limpiarScrollActaPendiente();
    this.bandejaSeleccionada.set(bandeja);
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
    this.limpiarRedaccionFeedback();
    this.limpiarFalloFeedback();
    this.limpiarSeguimientoActaFeedback();
    this.limpiarCopiaEstadoFeedback();
    this.cargarListado();
  }

  seleccionarActa(id: string): void {
    if (this.actaSeleccionadaId() === id) {
      return;
    }
    this.limpiarScrollActaPendiente();
    this.actaSeleccionadaId.set(id);
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
          'Cumplimiento material registrado: ' + respuesta.pendienteCumplido + '.',
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
    return this.pendientesBloqueantes(det).some((pendiente) =>
      this.esTipoCumplimientoMaterialConocido(pendiente),
    );
  }

  // El cumplimiento material solo aplica si el eje material asociado al
  // bloqueante ya tiene resolutorio documental incorporado en el expediente
  // (fase RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL). Antes de esa fase
  // el backend responde 409: la accion correcta es resolver el bloqueante.
  puedeMostrarAccionCumplimientoMaterial(
    pendiente: string,
  ): pendiente is TipoCumplimientoMaterialBloqueante {
    return (
      this.puedeMostrarCumplimientosMateriales() &&
      this.esTipoCumplimientoMaterialConocido(pendiente) &&
      this.faseEjeParaPendiente(pendiente) === FASE_EJE_RESOLUTORIO_EN_EXPEDIENTE
    );
  }

  // Visibilidad de resolucion documental de bloqueante. El backend exige
  // origen reconocido y resolutorio inexistente: se refleja chequeando la
  // fase SITUACION_PENDIENTE_DE_RESOLUTORIO en el eje asociado. Las
  // bandejas terminales/no operables internamente quedan excluidas igual
  // que el cumplimiento material.
  puedeMostrarAccionResolucionBloqueoCierre(
    pendiente: string,
  ): pendiente is TipoResolucionBloqueoCierre {
    if (this.bandejaActaSinAccionesInternasOperativas()) {
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
    return this.accionesPagoDisponiblesDesdeBackend().length > 0;
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
    if (this.ejecutandoPagoAccion() !== null) {
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
    return accion === 'SOLICITAR';
  }

  /**
   * Habilitacion del boton por accion: SOLICITAR exige monto valido. Las
   * demas acciones solo dependen de que no haya otra accion en curso.
   */
  puedeEjecutarAccionPago(accion: AccionPagoVoluntarioDemo): boolean {
    if (this.ejecutandoPagoAccion() !== null) {
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


  actaPuedeGenerarNulidadDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_NULIDAD);
  }

  actaPuedeGenerarMedidaPreventivaDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_MEDIDA_PREVENTIVA);
  }

  actaPuedeGenerarNotificacionActaDesdeBackend(): boolean {
    return this.piezaRedaccionPendienteDesdeBackend(PIEZA_NOTIFICACION_ACTA);
  }

  hayAccionesRedaccionDisponiblesDesdeBackend(): boolean {
    return (
      this.actaPuedeGenerarNulidadDesdeBackend() ||
      this.actaPuedeGenerarMedidaPreventivaDesdeBackend() ||
      this.actaPuedeGenerarNotificacionActaDesdeBackend()
    );
  }

  accionRedaccionEnCurso(): boolean {
    return (
      this.generandoNulidad() ||
      this.generandoMedidaPreventiva() ||
      this.generandoNotificacionActa()
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

  private mensajeRedaccionExitosa(respuesta: {
    mensaje: string;
    bandejaActual: string;
    estadoProcesoActual: string;
  }): string {
    return respuesta.mensaje + ' (' + respuesta.bandejaActual + '; ' + respuesta.estadoProcesoActual + ').';
  }

  badgesDe(acta: ActaResumenDemo): BadgeDemo[] {
    return badgesDesdeActaResumen(acta);
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

  private limpiarFalloFeedback(): void {
    this.falloError.set(null);
    this.falloMensaje.set(null);
  }

  private limpiarRedaccionFeedback(): void {
    this.limpiarNulidadFeedback();
    this.limpiarMedidaPreventivaFeedback();
    this.limpiarNotificacionActaRedaccionFeedback();
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

  private limpiarSeguimientoActaFeedback(): void {
    this.seguimientoActaMensaje.set(null);
  }

  private esBandejaCodigo(valor: string): valor is BandejaCodigo {
    return (BANDEJAS_DEMO as readonly string[]).includes(valor);
  }

  private refrescarLuegoDeAccion(respuesta: AccionConBandejaResponseLike | null | undefined): void {
    const actaId = respuesta?.actaId ?? this.actaSeleccionadaId();
    const bandejaNueva = respuesta?.bandejaActual;

    if (
      this.seguirActaAlCambiarBandeja() &&
      bandejaNueva &&
      this.esBandejaCodigo(bandejaNueva) &&
      bandejaNueva !== this.bandejaSeleccionada()
    ) {
      this.cambiarBandejaYSeguirActa(bandejaNueva, actaId);
      return;
    }

    this.refrescarActaSeleccionadaDesdeBackend(actaId ?? null);
  }

  private cambiarBandejaYSeguirActa(bandeja: BandejaCodigo, actaId: string | null): void {
    this.limpiarSeguimientoActaFeedback();
    this.bandejaSeleccionada.set(bandeja);
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

  private reintentarScrollPendienteAlRenderizarListado(): void {
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

    const container = this.actasListContainer?.nativeElement;
    if (!container) {
      if (attempt === 0) {
        setTimeout(() => this.scrollSelectedActaIntoView(actaId, 0));
      }
      return;
    }

    const row =
      container.querySelector<HTMLElement>(`[data-acta-id="${CSS.escape(actaId)}"]`) ??
      this.actaRows?.find((item) => item.nativeElement.dataset['actaId'] === actaId)?.nativeElement ??
      null;

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

      const container = this.actasListContainer?.nativeElement;
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

  private cargarListado(opciones?: {
    actaIdScrollObjetivo?: string | null;
    evaluarSeguimientoBandeja?: boolean;
    bandeja?: BandejaCodigo;
  }): void {
    const actaIdScrollObjetivo = opciones?.actaIdScrollObjetivo ?? null;
    const evaluarSeguimientoBandeja = opciones?.evaluarSeguimientoBandeja ?? false;
    const bandeja = opciones?.bandeja ?? this.bandejaSeleccionada();

    this.listadoEstado.set('loading');
    this.listadoError.set(null);
    this.api
      .listarActasPorBandeja(bandeja)
      .pipe(
        catchError((err) => {
          this.listadoError.set(mensajeErrorHttp(err));
          this.listadoEstado.set('error');
          return of([] as ActaResumenDemo[]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        this.actas.set(items);
        if (this.listadoEstado() !== 'error') {
          this.listadoEstado.set('ready');
        }

        if (!actaIdScrollObjetivo) {
          return;
        }

        if (evaluarSeguimientoBandeja) {
          const encontrada = items.some((acta) => acta.id === actaIdScrollObjetivo);
          if (!encontrada) {
            this.seguimientoActaMensaje.set(
              'El acta sigue seleccionada, pero no aparece en el listado de la bandeja ' +
                etiquetaBandeja(bandeja) +
                '.',
            );
            return;
          }
        }

        if (this.actaSeleccionadaId() === actaIdScrollObjetivo) {
          this.programarScrollActaSeleccionada(actaIdScrollObjetivo);
        }
      });
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

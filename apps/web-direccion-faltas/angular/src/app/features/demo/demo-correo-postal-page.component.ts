import { Component, DestroyRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import {
  CorreoLoteResumenDemo,
  CorreoPostalNotificacionListaDemo,
  CorreoPostalTrazabilidadDemo,
  EnviarIndividualCorreoResultadoDemo,
  GenerarLoteCorreoResultadoDemo,
  ProcesarRespuestaCorreoResultadoDemo,
} from '../../core/models/prototipo-faltas.models';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type VistaCorreoPostal = 'actas' | 'lotes';
type CorreoPostalAccion = 'GENERAR_LOTE' | 'PROCESAR_RESPUESTA' | 'ANULAR_LOTE' | 'ENVIAR_INDIVIDUAL';
type FiltroTipoCorreo = 'TODOS' | 'ACTA_INFRACCION' | 'FALLO_CONDENATORIO' | 'FALLO_ABSOLUTORIO';
type FiltroEstadoLoteCorreo = 'EN_TRABAJO' | 'PROCESADO' | 'ANULADO' | 'TODOS';
type CorreoPostalQueryKey = 'tipo' | 'estado' | 'acta' | 'lote';

const FILTROS_TIPO: readonly FiltroTipoCorreo[] = [
  'TODOS',
  'ACTA_INFRACCION',
  'FALLO_CONDENATORIO',
  'FALLO_ABSOLUTORIO',
];

const FILTROS_ESTADO_LOTE: readonly FiltroEstadoLoteCorreo[] = [
  'EN_TRABAJO',
  'PROCESADO',
  'ANULADO',
  'TODOS',
];

const PAGE_SIZE_CANDIDATAS_CORREO = 25;

@Component({
  selector: 'app-demo-correo-postal-page',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './demo-correo-postal-page.component.html',
  styleUrl: './demo-correo-postal-page.component.scss',
})
export class DemoCorreoPostalPageComponent implements OnInit, OnChanges {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  @Input({ required: true }) vista: VistaCorreoPostal = 'actas';

  @Output() readonly operacionCompletada = new EventEmitter<void>();

  readonly filtrosTipo = FILTROS_TIPO;
  readonly filtrosEstadoLote = FILTROS_ESTADO_LOTE;

  readonly cargando = signal<boolean>(false);
  readonly accionEnCurso = signal<CorreoPostalAccion | null>(null);
  readonly notificacionEnCursoId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly advertencia = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly loteUrlAdvertencia = signal<string | null>(null);
  readonly notificacionesListas = signal<CorreoPostalNotificacionListaDemo[]>([]);
  readonly lotesGenerados = signal<CorreoLoteResumenDemo[]>([]);
  readonly loteSeleccionadoId = signal<string | null>(null);
  readonly ultimaRespuesta = signal<ProcesarRespuestaCorreoResultadoDemo | null>(null);
  readonly candidatasSeleccionadas = signal<Set<string>>(new Set());
  private readonly candidatasExcluidas = signal<Set<string>>(new Set());

  readonly filtroTipo = signal<FiltroTipoCorreo>('TODOS');
  readonly filtroEstadoLote = signal<FiltroEstadoLoteCorreo>('EN_TRABAJO');
  readonly paginaActual = signal(1);
  consultaActa = '';
  private ultimoActaParamUrl = '';
  private seleccionCandidatasTocada = false;

  readonly trazabilidadCargada = signal<boolean>(false);
  readonly trazabilidadCargando = signal<boolean>(false);
  readonly trazabilidad = signal<CorreoPostalTrazabilidadDemo[]>([]);
  readonly trazabilidadError = signal<string | null>(null);

  private aplicandoParamsUrl = false;

  readonly listasFiltradas = computed(() => {
    const tipo = this.filtroTipo();
    const listas = this.notificacionesListas();
    if (tipo === 'TODOS') {
      return listas;
    }
    return listas.filter((item) => item.tipo === tipo);
  });

  readonly lotesFiltrados = computed(() => {
    const filtro = this.filtroEstadoLote();
    const lotes = this.lotesGenerados();
    if (filtro === 'TODOS') {
      return lotes;
    }
    const estadoBackend = mapFiltroEstadoLoteABackend(filtro);
    return lotes.filter((lote) => lote.estado === estadoBackend);
  });

  readonly cantidadCandidatas = computed(() => this.listasFiltradas().length);

  readonly totalPaginasCandidatas = computed(() => {
    const total = this.cantidadCandidatas();
    return Math.max(1, Math.ceil(total / PAGE_SIZE_CANDIDATAS_CORREO));
  });

  readonly listasFiltradasPagina = computed(() => {
    const items = this.listasFiltradas();
    const pagina = this.paginaActual();
    const inicio = (pagina - 1) * PAGE_SIZE_CANDIDATAS_CORREO;
    return items.slice(inicio, inicio + PAGE_SIZE_CANDIDATAS_CORREO);
  });

  readonly hayBusquedaActaActiva = computed(
    () => this.consultaActa.trim().length > 0 || this.ultimoActaParamUrl.trim().length > 0,
  );

  readonly cantidadSeleccionadas = computed(() => {
    const visibles = new Set(this.listasFiltradas().map((item) => item.notificacionId));
    return [...this.candidatasSeleccionadas()].filter((id) => visibles.has(id)).length;
  });

  readonly todasCandidatasVisiblesSeleccionadas = computed(() => {
    const visibles = this.listasFiltradasPagina();
    if (visibles.length === 0) {
      return false;
    }
    const seleccion = this.candidatasSeleccionadas();
    return visibles.every((item) => seleccion.has(item.notificacionId));
  });

  readonly puedeGenerarLote = computed(() => {
    return (
      this.cantidadSeleccionadas() > 0
      && this.accionEnCurso() === null
      && !this.cargando()
    );
  });

  readonly loteSeleccionado = computed(() => {
    const loteId = this.loteSeleccionadoId();
    if (!loteId) {
      return null;
    }
    return this.lotesGenerados().find((lote) => lote.loteId === loteId) ?? null;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['vista'] && !changes['vista'].firstChange) {
      this.limpiarFeedback();
      this.cargarDatos();
    }
  }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => this.consumirQueryParams(params));

    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.error.set(null);

    if (this.vista === 'actas') {
      this.api
        .listarNotificacionesCorreoListasParaLote()
        .pipe(
          catchError((err) => {
            this.error.set(mensajeErrorHttp(err));
            return of([] as CorreoPostalNotificacionListaDemo[]);
          }),
          finalize(() => this.cargando.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((listas) => {
          this.notificacionesListas.set(listas);
          this.resetPaginaCandidatas();
          this.sincronizarSeleccionCandidatasTrasRecarga();
        });
      return;
    }

    this.api
      .listarLotesCorreoGenerados()
      .pipe(
        catchError((err) => {
          this.error.set(mensajeErrorHttp(err));
          return of([] as CorreoLoteResumenDemo[]);
        }),
        finalize(() => this.cargando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((lotes) => {
        this.lotesGenerados.set(lotes);
        this.syncSeleccionLote(lotes);
      });
  }

  seleccionarFiltroTipo(tipo: FiltroTipoCorreo): void {
    this.resetPaginaCandidatas();
    this.filtroTipo.set(tipo);
    this.loteSeleccionadoId.set(null);
    this.seleccionCandidatasTocada = false;
    this.candidatasExcluidas.set(new Set());
    this.resetSeleccionCandidatas();
    this.actualizarQueryParams({ tipo, lote: null });
  }

  seleccionarFiltroEstadoLote(estado: FiltroEstadoLoteCorreo): void {
    this.filtroEstadoLote.set(estado);
    this.loteSeleccionadoId.set(null);
    this.actualizarQueryParams({ estado, lote: null });
  }

  labelFiltroTipo(tipo: FiltroTipoCorreo): string {
    if (tipo === 'TODOS') {
      return 'Todos';
    }
    return labelTipoNotificacion(tipo);
  }

  labelFiltroEstadoLote(estado: FiltroEstadoLoteCorreo): string {
    switch (estado) {
      case 'EN_TRABAJO':
        return 'En trabajo';
      case 'PROCESADO':
        return 'Procesado';
      case 'ANULADO':
        return 'Anulado';
      case 'TODOS':
        return 'Todos';
      default:
        return estado;
    }
  }

  labelTipoNotificacion(tipo: string): string {
    return labelTipoNotificacion(tipo);
  }

  labelEstadoLote(estado: string): string {
    switch (estado) {
      case 'PENDIENTE_RESPUESTA':
        return 'Pendiente de respuesta';
      case 'PROCESADO':
        return 'Procesado';
      case 'ANULADO':
        return 'Anulado';
      default:
        return estado;
    }
  }

  labelEstadoNotificacion(estado: string): string {
    switch (estado) {
      case 'PENDIENTE_PREPARACION':
        return 'Pendiente de preparacion';
      case 'LISTA_PARA_ENVIO':
        return 'Lista para envio';
      case 'ENVIADA':
        return 'Enviada';
      case 'ENTREGADA':
        return 'Entregada';
      case 'NEGATIVA':
        return 'Negativa';
      case 'VENCIDA':
        return 'Vencida';
      default:
        return estado;
    }
  }

  labelResultadoNotificacion(resultado: string): string {
    switch (resultado) {
      case 'SIN_RESULTADO':
        return 'Sin resultado';
      case 'POSITIVA':
        return 'Positiva';
      case 'NEGATIVA':
        return 'Negativa';
      case 'VENCIDA':
        return 'Vencida';
      default:
        return resultado;
    }
  }

  mensajeConteoListas(): string {
    const total = this.cantidadCandidatas();
    const seleccionadas = this.cantidadSeleccionadas();
    const tipo = this.filtroTipo();
    const base =
      total === 1
        ? '1 notificación lista para lote con el filtro actual.'
        : `${total} notificaciones listas para lote con el filtro actual.`;
    const seleccion =
      seleccionadas === 1
        ? '1 seleccionada entrara en el proximo lote.'
        : `${seleccionadas} seleccionadas entraran en el proximo lote.`;
    if (tipo === 'TODOS') {
      return `${base} ${seleccion}`;
    }
    return `${total === 1 ? '1 notificación' : `${total} notificaciones`} de ${this.labelTipoNotificacion(tipo)} visibles. ${seleccion}`;
  }

  descripcionTrazabilidad(item: CorreoPostalTrazabilidadDemo): string {
    if (item.estadoLote === 'PENDIENTE_RESPUESTA') {
      return 'En lote pendiente de respuesta.';
    }
    if (item.estadoLote === 'PROCESADO') {
      return `Procesada con resultado ${this.labelResultadoNotificacion(item.resultadoNotificacion)}.`;
    }
    if (item.estadoLote === 'ANULADO' && item.estadoNotificacion === 'LISTA_PARA_ENVIO') {
      return 'Lote anulado / pendiente de nuevo envio.';
    }
    if (item.estadoLote === 'ANULADO') {
      return 'Lote anulado.';
    }
    if (item.estadoNotificacion === 'LISTA_PARA_ENVIO' && item.resultadoNotificacion === 'SIN_RESULTADO') {
      return 'Pendiente de nuevo envio.';
    }
    return '-';
  }

  estaCandidataSeleccionada(notificacionId: string): boolean {
    return this.candidatasSeleccionadas().has(notificacionId);
  }

  toggleCandidata(notificacionId: string, seleccionada: boolean): void {
    this.seleccionCandidatasTocada = true;
    this.candidatasExcluidas.update((actual) => {
      const next = new Set(actual);
      if (seleccionada) {
        next.delete(notificacionId);
      } else {
        next.add(notificacionId);
      }
      return next;
    });
    this.candidatasSeleccionadas.update((actual) => {
      const next = new Set(actual);
      if (seleccionada) {
        next.add(notificacionId);
      } else {
        next.delete(notificacionId);
      }
      return next;
    });
  }

  toggleTodasCandidatasVisibles(seleccionar: boolean): void {
    this.seleccionCandidatasTocada = true;
    const visibles = this.listasFiltradasPagina().map((item) => item.notificacionId);
    this.candidatasExcluidas.update((actual) => {
      const next = new Set(actual);
      for (const id of visibles) {
        if (seleccionar) {
          next.delete(id);
        } else {
          next.add(id);
        }
      }
      return next;
    });
    this.candidatasSeleccionadas.update((actual) => {
      const next = new Set(actual);
      for (const id of visibles) {
        if (seleccionar) {
          next.add(id);
        } else {
          next.delete(id);
        }
      }
      return next;
    });
  }

  paginaAnterior(): void {
    if (this.paginaActual() <= 1) {
      return;
    }
    this.paginaActual.update((pagina) => pagina - 1);
  }

  paginaSiguiente(): void {
    if (this.paginaActual() >= this.totalPaginasCandidatas()) {
      return;
    }
    this.paginaActual.update((pagina) => pagina + 1);
  }


  enviarIndividual(item: CorreoPostalNotificacionListaDemo): void {
    if (this.accionEnCurso() !== null || this.cargando()) {
      return;
    }
    const confirmacion = window.confirm(
      `Enviar de forma individual la notificación ${item.notificacionId} del acta ${item.acta}?`,
    );
    if (!confirmacion) {
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('ENVIAR_INDIVIDUAL');
    this.notificacionEnCursoId.set(item.notificacionId);

    this.api
      .enviarIndividualCorreoPostalDemo(item.notificacionId)
      .pipe(
        catchError((err) => {
          this.error.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => {
          this.accionEnCurso.set(null);
          this.notificacionEnCursoId.set(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.mensaje.set(respuesta.mensaje);
        this.operacionCompletada.emit();
        this.cargarDatos();
      });
  }

  enviandoIndividual(notificacionId: string): boolean {
    return this.accionEnCurso() === 'ENVIAR_INDIVIDUAL'
      && this.notificacionEnCursoId() === notificacionId;
  }
  generarLote(): void {
    if (!this.puedeGenerarLote() || this.accionEnCurso() !== null) {
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('GENERAR_LOTE');
    const tipo = this.filtroTipo();
    const tipoParam = tipo === 'TODOS' ? undefined : tipo;
    const notificacionIds = this.listasFiltradas()
      .map((item) => item.notificacionId)
      .filter((id) => this.candidatasSeleccionadas().has(id));

    this.api
      .generarLoteCorreoPostalDemo(tipoParam, notificacionIds)
      .pipe(
        catchError((err) => {
          this.error.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.accionEnCurso.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        if (respuesta.cantidad === 0) {
          this.advertencia.set('No hay notificaciones de correo postal listas para enviar.');
        } else {
          this.mensaje.set(
            `Lote correo ${respuesta.loteId} generado con ${respuesta.cantidad} notificaciones seleccionadas. Consúltelo en Lotes de correo postal.`,
          );
        }
        this.seleccionCandidatasTocada = false;
        this.candidatasExcluidas.set(new Set());
        this.operacionCompletada.emit();
        this.cargarDatos();
      });
  }

  buscarActa(): void {
    const acta = this.consultaActa.trim();
    if (!acta) {
      this.limpiarBusquedaActa();
      return;
    }
    this.actualizarQueryParams({ acta });
  }

  limpiarBusquedaActa(): void {
    this.consultaActa = '';
    this.ultimoActaParamUrl = '';
    this.limpiarEstadoTrazabilidad();
    this.actualizarQueryParams({ acta: null });
  }

  procesarRespuestaCorreoPostalDemo(): void {
    const lote = this.loteSeleccionado();
    if (!lote || !this.puedeProcesarRespuesta(lote) || this.accionEnCurso() !== null) {
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('PROCESAR_RESPUESTA');

    this.api
      .procesarRespuestaCorreoPostalDemo(lote.loteId)
      .pipe(
        catchError((err) => {
          this.error.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.accionEnCurso.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.ultimaRespuesta.set(respuesta);
        const base = `Respuesta correo procesada desde ${respuesta.nombreArchivo ?? 'CSV demo local'}: ${respuesta.total} filas, ${respuesta.errores} errores.`;
        if (respuesta.errores > 0) {
          this.advertencia.set(base);
        } else {
          this.mensaje.set(base);
          this.filtroEstadoLote.set('PROCESADO');
          this.actualizarQueryParams({ estado: 'PROCESADO', lote: lote.loteId });
        }
        this.operacionCompletada.emit();
        this.cargarDatos();
        this.refrescarTrazabilidadSiActiva();
      });
  }

  anularLoteSeleccionado(): void {
    const lote = this.loteSeleccionado();
    if (!lote || this.accionEnCurso() !== null) {
      return;
    }
    if (lote.estado === 'PROCESADO' || lote.estado === 'ANULADO') {
      this.advertencia.set('Este lote no puede anularse en su estado actual.');
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('ANULAR_LOTE');

    this.api
      .anularLoteCorreoPostalDemo(lote.loteId)
      .pipe(
        catchError((err) => {
          this.error.set(mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.accionEnCurso.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.mensaje.set(respuesta.mensaje);
        this.filtroEstadoLote.set('ANULADO');
        this.actualizarQueryParams({ estado: 'ANULADO', lote: lote.loteId });
        this.operacionCompletada.emit();
        this.cargarDatos();
        this.refrescarTrazabilidadSiActiva();
      });
  }

  seleccionarLote(loteId: string): void {
    this.actualizarQueryParams({ lote: loteId });
  }

  puedeAnularLote(lote: CorreoLoteResumenDemo | null): boolean {
    return lote != null && lote.estado === 'PENDIENTE_RESPUESTA';
  }

  puedeProcesarRespuesta(lote: CorreoLoteResumenDemo | null): boolean {
    return lote != null && lote.estado === 'PENDIENTE_RESPUESTA';
  }

  loteYaProcesado(lote: CorreoLoteResumenDemo | null): boolean {
    return lote != null && lote.estado === 'PROCESADO';
  }

  badgeClassEstadoLote(estado: string): string {
    switch (estado) {
      case 'PENDIENTE_RESPUESTA':
        return 'badge badge--pendiente';
      case 'PROCESADO':
        return 'badge badge--procesado';
      case 'ANULADO':
        return 'badge badge--anulado';
      default:
        return 'badge';
    }
  }

  textoOpcional(valor: string | null | undefined): string {
    return valor && valor.trim().length > 0 ? valor : '-';
  }

  private consumirQueryParams(params: ParamMap): void {
    this.aplicandoParamsUrl = true;
    try {
      if (this.vista === 'actas') {
        this.resetPaginaCandidatas();
        this.filtroTipo.set(parseTipoParam(params.get('tipo')));
        return;
      }

      this.filtroEstadoLote.set(parseEstadoParam(params.get('estado')));
      const actaParam = params.get('acta') ?? '';
      if (actaParam !== this.ultimoActaParamUrl) {
        this.ultimoActaParamUrl = actaParam;
        this.consultaActa = actaParam;
      }
      this.loteSeleccionadoId.set(params.get('lote'));
      this.loteUrlAdvertencia.set(null);

      const acta = actaParam.trim();
      if (acta) {
        this.cargarTrazabilidad(acta);
      } else {
        this.limpiarEstadoTrazabilidad();
      }
    } finally {
      this.aplicandoParamsUrl = false;
    }
  }

  private actualizarQueryParams(cambios: Partial<Record<CorreoPostalQueryKey, string | null>>): void {
    if (this.aplicandoParamsUrl) {
      return;
    }

    const queryParams = this.construirQueryParamsNavegacion(cambios);

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true,
    });
  }

  private construirQueryParamsNavegacion(
    cambios: Partial<Record<CorreoPostalQueryKey, string | null>>,
  ): Partial<Record<CorreoPostalQueryKey, string>> {
    const queryParams: Partial<Record<CorreoPostalQueryKey, string>> = {};

    if (this.vista === 'actas') {
      const tipo = cambios.tipo ?? this.filtroTipo();
      if (tipo) {
        queryParams.tipo = tipo;
      }
      return queryParams;
    }

    const estado = cambios.estado ?? this.filtroEstadoLote();
    if (estado) {
      queryParams.estado = estado;
    }

    const lote = cambios.lote !== undefined ? cambios.lote : this.loteSeleccionadoId();
    if (lote) {
      queryParams.lote = lote;
    }

    const acta = this.resolverActaParaNavegacion(cambios.acta);
    if (acta) {
      queryParams.acta = acta;
    }

    return queryParams;
  }

  private resolverActaParaNavegacion(actaCambio: string | null | undefined): string | null {
    if (actaCambio === null) {
      return null;
    }
    if (actaCambio !== undefined) {
      const acta = actaCambio.trim();
      return acta.length > 0 ? acta : null;
    }

    const actaMemoria = this.consultaActa.trim() || this.ultimoActaParamUrl.trim();
    return actaMemoria.length > 0 ? actaMemoria : null;
  }

  private limpiarEstadoTrazabilidad(): void {
    this.trazabilidadCargada.set(false);
    this.trazabilidad.set([]);
    this.trazabilidadError.set(null);
  }

  private resetPaginaCandidatas(): void {
    this.paginaActual.set(1);
  }

  private resetSeleccionCandidatas(): void {
    this.candidatasSeleccionadas.set(
      new Set(this.listasFiltradas().map((item) => item.notificacionId)),
    );
  }

  private sincronizarSeleccionCandidatasTrasRecarga(): void {
    const visibleIds = this.listasFiltradas().map((item) => item.notificacionId);
    if (!this.seleccionCandidatasTocada) {
      this.candidatasExcluidas.set(new Set());
      this.candidatasSeleccionadas.set(new Set(visibleIds));
      return;
    }
    const excluidas = this.candidatasExcluidas();
    const next = new Set<string>();
    for (const id of visibleIds) {
      if (!excluidas.has(id)) {
        next.add(id);
      }
    }
    this.candidatasSeleccionadas.set(next);
  }

  private refrescarTrazabilidadSiActiva(): void {
    const acta = this.consultaActa.trim();
    if (acta) {
      this.cargarTrazabilidad(acta);
    }
  }

  private cargarTrazabilidad(acta: string): void {
    this.trazabilidadCargando.set(true);
    this.trazabilidadError.set(null);

    this.api
      .buscarTrazabilidadCorreoPostal(acta)
      .pipe(
        catchError((err) => {
          this.trazabilidadError.set(mensajeErrorHttp(err));
          return of([] as CorreoPostalTrazabilidadDemo[]);
        }),
        finalize(() => {
          this.trazabilidadCargando.set(false);
          this.trazabilidadCargada.set(true);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        if (this.trazabilidadError()) {
          this.trazabilidad.set([]);
          return;
        }
        this.trazabilidad.set(items);
      });
  }

  private syncSeleccionLote(lotes: CorreoLoteResumenDemo[]): void {
    const loteId = this.loteSeleccionadoId();
    if (loteId) {
      const existe = lotes.some((lote) => lote.loteId === loteId);
      if (!existe) {
        this.loteUrlAdvertencia.set(`El lote ${loteId} no existe o ya no esta disponible.`);
        this.loteSeleccionadoId.set(null);
        this.actualizarQueryParams({ lote: null });
        return;
      }

      const visible = this.lotesFiltrados().some((lote) => lote.loteId === loteId);
      if (!visible) {
        this.loteUrlAdvertencia.set(
          `El lote ${loteId} no esta disponible con el filtro de estado actual.`,
        );
        this.loteSeleccionadoId.set(null);
        this.actualizarQueryParams({ lote: null });
      }
      return;
    }

    const visibles = this.lotesFiltrados();
    if (visibles.length === 0) {
      return;
    }
  }

  private integrarLoteGenerado(respuesta: GenerarLoteCorreoResultadoDemo): void {
    const tipos = [...new Set(respuesta.notificaciones.map((n) => n.tipo))];
    const resumen: CorreoLoteResumenDemo = {
      loteId: respuesta.loteId,
      cantidad: respuesta.cantidad,
      nombreArchivo: respuesta.nombreArchivo,
      rutaArchivo: respuesta.rutaArchivo,
      estado: 'PENDIENTE_RESPUESTA',
      fechaGeneracion: new Date().toISOString(),
      tiposIncluidos: tipos,
      tipoDominante: tipos.length === 1 ? tipos[0] : null,
      positivas: 0,
      negativas: 0,
      vencidas: 0,
      notificaciones: respuesta.notificaciones,
    };
    this.lotesGenerados.update((items) => [...items, resumen]);
  }

  private limpiarFeedback(): void {
    this.error.set(null);
    this.advertencia.set(null);
    this.mensaje.set(null);
  }
}

function parseTipoParam(value: string | null): FiltroTipoCorreo {
  if (
    value === 'TODOS'
    || value === 'ACTA_INFRACCION'
    || value === 'FALLO_CONDENATORIO'
    || value === 'FALLO_ABSOLUTORIO'
  ) {
    return value;
  }
  return 'TODOS';
}

function parseEstadoParam(value: string | null): FiltroEstadoLoteCorreo {
  if (value === 'PROCESADOS') {
    return 'PROCESADO';
  }
  if (value === 'ANULADOS') {
    return 'ANULADO';
  }
  if (value === 'EN_TRABAJO' || value === 'PROCESADO' || value === 'ANULADO' || value === 'TODOS') {
    return value;
  }
  return 'EN_TRABAJO';
}

function mapFiltroEstadoLoteABackend(filtro: FiltroEstadoLoteCorreo): string {
  switch (filtro) {
    case 'EN_TRABAJO':
      return 'PENDIENTE_RESPUESTA';
    case 'PROCESADO':
      return 'PROCESADO';
    case 'ANULADO':
      return 'ANULADO';
    default:
      return filtro;
  }
}

function labelTipoNotificacion(tipo: string): string {
  switch (tipo) {
    case 'ACTA_INFRACCION':
      return 'Acta de infraccion';
    case 'FALLO_CONDENATORIO':
      return 'Fallo condenatorio';
    case 'FALLO_ABSOLUTORIO':
      return 'Fallo absolutorio';
    case 'TODOS':
      return 'Todos';
    default:
      return tipo;
  }
}

function mensajeErrorHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    const detalle = mensajeBackend(err);
    if (status === 0) {
      return 'Sin conexion con el backend prototipo.';
    }
    return detalle ? `Error HTTP ${String(status)}: ${detalle}` : 'Error HTTP ' + String(status);
  }
  return 'Error inesperado al consultar el backend.';
}

function mensajeBackend(err: unknown): string | null {
  const body = (err as { error?: unknown })?.error;
  if (typeof body === 'string') {
    return body.trim() || null;
  }
  if (!body || typeof body !== 'object') {
    return null;
  }
  const candidate = body as {
    message?: unknown;
    detail?: unknown;
    title?: unknown;
    error?: unknown;
    mensaje?: unknown;
  };
  for (const value of [candidate.message, candidate.detail, candidate.error, candidate.title, candidate.mensaje]) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return null;
}

import { AfterViewInit, Component, DestroyRef, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActaBandejaItem, BadgeDemo, SubBandejaResumen } from '../../core/models/prototipo-faltas.models';
import { badgesDesdeActaResumen } from '../../core/services/acta-badges.presenter';

type CargaEstado = 'idle' | 'loading' | 'ready' | 'error';

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

@Component({
  selector: 'app-demo-acta-list',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './demo-acta-list.component.html',
  styleUrl: './demo-acta-list.component.scss',
})
export class DemoActaListComponent implements AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);

  @Input() actas: ActaBandejaItem[] = [];
  @Input() hayActas = false;
  @Input() listadoEstado: CargaEstado = 'idle';
  @Input() listadoError: string | null = null;
  @Input() actaSeleccionadaId: string | null = null;
  @Input() textoBusquedaActa = '';
  @Input() seguirActaAlCambiarBandeja = false;
  @Input() seguimientoActaMensaje: string | null = null;
  @Input() subBandejas: SubBandejaResumen[] = [];
  @Input() subBandejaActiva: string | null = null;

  @Output() readonly textoBusquedaActaChange = new EventEmitter<string>();
  @Output() readonly seguirActaAlCambiarBandejaChange = new EventEmitter<boolean>();
  @Output() readonly recargarListado = new EventEmitter<void>();
  @Output() readonly seleccionarActa = new EventEmitter<string>();
  @Output() readonly mostrarResumenBandejaChange = new EventEmitter<void>();
  @Output() readonly filtroOperativoChange = new EventEmitter<string | null>();

  get valorFiltroOperativo(): string | null {
    return this.subBandejaActiva;
  }


  cambiarFiltroOperativo(subCodigo: string | null): void {
    this.filtroOperativoChange.emit(subCodigo);
  }

  get filtroOperativoEsTodos(): boolean {
    return this.valorFiltroOperativo === null;
  }

  limpiarFiltroOperativo(): void {
    if (this.filtroOperativoEsTodos) {
      return;
    }
    this.cambiarFiltroOperativo(null);
  }

  mostrarResumenBandeja(): void {
    if (!this.actaSeleccionadaId) {
      return;
    }
    this.mostrarResumenBandejaChange.emit();
  }
  @Output() readonly rowsRenderizadas = new EventEmitter<void>();

  @ViewChild('actasListContainer') private readonly actasListContainer?: ElementRef<HTMLElement>;
  @ViewChildren('actaRow') private readonly actaRows?: QueryList<ElementRef<HTMLElement>>;

  get contenedor(): HTMLElement | null {
    return this.actasListContainer?.nativeElement ?? null;
  }

  ngAfterViewInit(): void {
    this.actaRows?.changes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.rowsRenderizadas.emit());
    this.rowsRenderizadas.emit();
  }

  buscarFila(actaId: string): HTMLElement | null {
    const container = this.contenedor;
    return (
      container?.querySelector<HTMLElement>(`[data-acta-id="${CSS.escape(actaId)}"]`) ??
      this.actaRows?.find((item) => item.nativeElement.dataset['actaId'] === actaId)?.nativeElement ??
      null
    );
  }

  badgesDe(acta: ActaBandejaItem): BadgeDemo[] {
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

  private humanizarCodigoVisual(valor: string): string {
    if (!/^[A-Z0-9_]+$/.test(valor)) {
      return valor;
    }
    const minusculas = valor.replace(/_/g, ' ').toLocaleLowerCase('es-AR');
    return minusculas.charAt(0).toLocaleUpperCase('es-AR') + minusculas.slice(1);
  }
}

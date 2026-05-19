import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { catchError, of } from 'rxjs';
import { BANDEJAS_DEMO, etiquetaBandeja } from '../../core/constants/bandejas-demo.constants';
import {
  ActaDetalleDemo,
  ActaResumenDemo,
  BadgeDemo,
  BandejaCodigo,
} from '../../core/models/prototipo-faltas.models';
import {
  badgesDesdeActaResumen,
} from '../../core/services/acta-badges.presenter';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type CargaEstado = 'idle' | 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-demo-shell',
  standalone: true,
  imports: [
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
export class DemoShellComponent implements OnInit {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly bandejas = BANDEJAS_DEMO;
  readonly etiquetaBandeja = etiquetaBandeja;

  readonly bandejaSeleccionada = signal<BandejaCodigo>('ACTAS_EN_ENRIQUECIMIENTO');
  readonly actas = signal<ActaResumenDemo[]>([]);
  readonly actaSeleccionadaId = signal<string | null>(null);
  readonly detalle = signal<ActaDetalleDemo | null>(null);

  readonly listadoEstado = signal<CargaEstado>('idle');
  readonly listadoError = signal<string | null>(null);
  readonly detalleEstado = signal<CargaEstado>('idle');
  readonly detalleError = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarListado();
  }

  tituloBandejaActual(): string {
    return etiquetaBandeja(this.bandejaSeleccionada());
  }

  seleccionarBandeja(bandeja: BandejaCodigo): void {
    if (this.bandejaSeleccionada() === bandeja) {
      return;
    }
    this.bandejaSeleccionada.set(bandeja);
    this.actaSeleccionadaId.set(null);
    this.detalle.set(null);
    this.detalleEstado.set('idle');
    this.cargarListado();
  }

  seleccionarActa(id: string): void {
    if (this.actaSeleccionadaId() === id) {
      return;
    }
    this.actaSeleccionadaId.set(id);
    this.cargarDetalle();
  }

  recargarListado(): void {
    this.cargarListado();
  }

  recargarDetalle(): void {
    this.cargarDetalle();
  }

  badgesDe(acta: ActaResumenDemo): BadgeDemo[] {
    return badgesDesdeActaResumen(acta);
  }

  claseBadge(badge: BadgeDemo): string {
    return 'badge-' + badge.tono;
  }

  private cargarListado(): void {
    this.listadoEstado.set('loading');
    this.listadoError.set(null);
    this.api
      .listarActasPorBandeja(this.bandejaSeleccionada())
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
    this.api
      .obtenerActa(id)
      .pipe(
        catchError((err) => {
          this.detalleError.set(mensajeErrorHttp(err));
          return of(null);
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
}

function mensajeErrorHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    if (status === 0) {
      return 'Sin conexión con el backend prototipo.';
    }
    return 'Error HTTP ' + String(status);
  }
  return 'Error inesperado al consultar el backend.';
}

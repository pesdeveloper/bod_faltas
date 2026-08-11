import {
  Component, ChangeDetectionStrategy, inject, input, signal, effect, DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, switchMap, of } from 'rxjs';
import { GeoParcelaService } from '../../../core/services/geo-parcela.service';
import { ParcelaRef, ParcelaDetalle } from '../../../core/models/geo.models';

@Component({
  selector: 'app-parcela-detail-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule, MatDividerModule],
  templateUrl: './parcela-detail-panel.component.html',
  styleUrl: './parcela-detail-panel.component.scss',
})
export class ParcelaDetailPanelComponent {
  private readonly parcelaService = inject(GeoParcelaService);
  private readonly destroyRef = inject(DestroyRef);

  readonly parcelaRef = input<ParcelaRef | null>(null);

  readonly loading  = signal(false);
  readonly detalle  = signal<ParcelaDetalle | null>(null);
  readonly errorMsg = signal<string | null>(null);

  private readonly ref$ = new Subject<ParcelaRef | null>();

  constructor() {
    effect(() => this.ref$.next(this.parcelaRef()));

    this.ref$.pipe(
      switchMap(ref => {
        if (!ref) {
          this.detalle.set(null);
          this.errorMsg.set(null);
          this.loading.set(false);
          return of(null);
        }
        this.loading.set(true);
        this.errorMsg.set(null);
        this.detalle.set(null);
        return this.parcelaService.getDetalle(ref);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (d) => {
        if (d !== null) {
          this.detalle.set(d);
          this.loading.set(false);
        }
      },
      error: (err) => {
        const msg = err?.status === 404
          ? 'Sin inmueble registrado para esta parcela.'
          : 'Error al cargar el detalle.';
        this.errorMsg.set(msg);
        this.loading.set(false);
      },
    });
  }
}
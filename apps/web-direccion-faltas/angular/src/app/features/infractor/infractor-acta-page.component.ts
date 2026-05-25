import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { catchError, of } from 'rxjs';
import { ActaInfractorResponseDemo } from '../../core/models/prototipo-faltas.models';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type CargaEstado = 'loading' | 'ready' | 'not-found' | 'error';
type ApelacionPortalMensajeTipo = 'success' | 'error';

@Component({
  selector: 'app-infractor-acta-page',
  standalone: true,
  imports: [MatToolbarModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './infractor-acta-page.component.html',
  styleUrl: './infractor-acta-page.component.scss',
})
export class InfractorActaPageComponent implements OnInit {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly estado = signal<CargaEstado>('loading');
  readonly acta = signal<ActaInfractorResponseDemo | null>(null);
  readonly registrandoApelacion = signal(false);
  readonly apelacionPortalMensaje = signal<string | null>(null);
  readonly apelacionPortalMensajeTipo = signal<ApelacionPortalMensajeTipo | null>(null);
  readonly pagoVoluntarioPortalMensaje = signal<string | null>(null);

  private codigoQrActual: string | null = null;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const codigoQr = params.get('codigoQr')?.trim();
      this.codigoQrActual = codigoQr ?? null;
      if (!codigoQr) {
        this.estado.set('not-found');
        this.acta.set(null);
        return;
      }
      this.cargarActa(codigoQr);
    });
  }


  solicitarPagoVoluntarioInformativo(): void {
    this.pagoVoluntarioPortalMensaje.set(
      'La solicitud de pago voluntario desde portal queda pendiente de implementar.',
    );
  }

  pagarInformativo(): void {
    this.pagoVoluntarioPortalMensaje.set(
      'El proceso de pago desde portal queda pendiente de implementar en demo.',
    );
  }

  presentarApelacion(): void {
    const codigoQr = this.codigoQrActual ?? this.acta()?.codigoQr ?? null;
    if (!codigoQr || this.registrandoApelacion()) {
      return;
    }

    this.registrandoApelacion.set(true);
    this.apelacionPortalMensaje.set(null);
    this.apelacionPortalMensajeTipo.set(null);

    this.api
      .registrarApelacionInfractorPorCodigoQr(codigoQr)
      .pipe(
        catchError((err: unknown) => {
          const status =
            err && typeof err === 'object' && 'status' in err
              ? (err as { status?: number }).status
              : undefined;

          if (status === 404) {
            this.estado.set('not-found');
            this.acta.set(null);
          } else if (status === 409) {
            this.apelacionPortalMensaje.set('No es posible presentar la apelación en este momento.');
            this.apelacionPortalMensajeTipo.set('error');
          } else {
            this.apelacionPortalMensaje.set('No pudimos registrar la apelación en este momento.');
            this.apelacionPortalMensajeTipo.set('error');
          }

          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        this.registrandoApelacion.set(false);
        if (!respuesta) {
          return;
        }

        this.acta.set(respuesta);
        this.estado.set('ready');
        this.apelacionPortalMensaje.set('Apelación presentada correctamente.');
        this.apelacionPortalMensajeTipo.set('success');
      });
  }

  private cargarActa(codigoQr: string): void {
    this.estado.set('loading');
    this.acta.set(null);

    this.api
      .getActaInfractorPorCodigoQr(codigoQr)
      .pipe(
        catchError((err: unknown) => {
          if (err && typeof err === 'object' && 'status' in err && (err as { status?: number }).status === 404) {
            this.estado.set('not-found');
          } else {
            this.estado.set('error');
          }
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.acta.set(respuesta);
        this.estado.set('ready');
      });
  }
}

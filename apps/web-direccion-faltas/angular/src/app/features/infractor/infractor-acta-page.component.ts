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
type VisualizacionPortalMensajeTipo = 'success' | 'error';

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
  readonly registrandoVisualizacionNotificacion = signal(false);
  readonly registrandoPagoVoluntario = signal(false);
  readonly apelacionPortalMensaje = signal<string | null>(null);
  readonly apelacionPortalMensajeTipo = signal<ApelacionPortalMensajeTipo | null>(null);
  readonly visualizacionPortalMensaje = signal<string | null>(null);
  readonly visualizacionPortalMensajeTipo = signal<VisualizacionPortalMensajeTipo | null>(null);
  readonly pagoVoluntarioPortalMensaje = signal<string | null>(null);
  readonly pagoVoluntarioPortalMensajeTipo = signal<'success' | 'error' | null>(null);

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
    const codigoQr = this.codigoQrActual ?? this.acta()?.codigoQr ?? null;
    if (!codigoQr || this.registrandoPagoVoluntario()) {
      return;
    }

    this.registrandoPagoVoluntario.set(true);
    this.pagoVoluntarioPortalMensaje.set(null);
    this.pagoVoluntarioPortalMensajeTipo.set(null);

    this.api
      .solicitarPagoVoluntarioInfractor(codigoQr)
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
            this.pagoVoluntarioPortalMensaje.set(
              'No es posible solicitar pago voluntario en el estado actual del acta.',
            );
            this.pagoVoluntarioPortalMensajeTipo.set('error');
          } else {
            this.pagoVoluntarioPortalMensaje.set(
              'No pudimos registrar la solicitud en este momento.',
            );
            this.pagoVoluntarioPortalMensajeTipo.set('error');
          }
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        this.registrandoPagoVoluntario.set(false);
        if (!respuesta) {
          return;
        }
        this.acta.set(respuesta);
        this.estado.set('ready');
        this.pagoVoluntarioPortalMensaje.set(
          'Solicitud de pago voluntario registrada. Direcci\u00f3n de Faltas evaluar\u00e1 el expediente.',
        );
        this.pagoVoluntarioPortalMensajeTipo.set('success');
      });
  }

  pagarVoluntario(): void {
    const codigoQr = this.codigoQrActual ?? this.acta()?.codigoQr ?? null;
    if (!codigoQr || this.registrandoPagoVoluntario()) {
      return;
    }

    this.registrandoPagoVoluntario.set(true);
    this.pagoVoluntarioPortalMensaje.set(null);
    this.pagoVoluntarioPortalMensajeTipo.set(null);

    this.api
      .informarPagoVoluntario(codigoQr)
      .pipe(
        catchError((err: unknown) => {
          const status =
            err && typeof err === 'object' && 'status' in err
              ? (err as { status?: number }).status
              : undefined;
          this.registrandoPagoVoluntario.set(false);
          if (status === 404) {
            this.estado.set('not-found');
            this.acta.set(null);
          } else if (status === 409) {
            const msg =
              err && typeof err === 'object' && 'error' in err
                ? ((err as { error?: { message?: string } }).error?.message ?? '')
                : '';
            this.pagoVoluntarioPortalMensaje.set(
              msg || 'No es posible registrar el pago en el estado actual del acta.',
            );
            this.pagoVoluntarioPortalMensajeTipo.set('error');
          } else {
            this.pagoVoluntarioPortalMensaje.set('No pudimos registrar el pago en este momento.');
            this.pagoVoluntarioPortalMensajeTipo.set('error');
          }
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        this.registrandoPagoVoluntario.set(false);
        if (!respuesta) {
          return;
        }
        this.acta.set(respuesta);
        this.estado.set('ready');
        this.pagoVoluntarioPortalMensaje.set('Pago en proceso de acreditaci\u00f3n. Direcci\u00f3n de Faltas verificar\u00e1 la acreditaci\u00f3n.');
        this.pagoVoluntarioPortalMensajeTipo.set('success');
      });
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


  confirmarVisualizacionNotificacion(): void {
    const codigoQr = this.codigoQrActual ?? this.acta()?.codigoQr ?? null;
    if (!codigoQr || this.registrandoVisualizacionNotificacion()) {
      return;
    }

    this.registrandoVisualizacionNotificacion.set(true);
    this.visualizacionPortalMensaje.set(null);
    this.visualizacionPortalMensajeTipo.set(null);

    this.api
      .confirmarVisualizacionNotificacionInfractorPorCodigoQr(codigoQr)
      .pipe(
        catchError((err: unknown) => {
          const status =
            err && typeof err === 'object' && 'status' in err
              ? (err as { status?: number }).status
              : undefined;

          if (status === 404) {
            this.estado.set('not-found');
            this.acta.set(null);
          } else if (status === 400) {
            this.visualizacionPortalMensaje.set('No hay una notificación pendiente para confirmar desde el portal.');
            this.visualizacionPortalMensajeTipo.set('error');
          } else {
            this.visualizacionPortalMensaje.set('No pudimos confirmar la visualización en este momento.');
            this.visualizacionPortalMensajeTipo.set('error');
          }

          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        this.registrandoVisualizacionNotificacion.set(false);
        if (!respuesta) {
          return;
        }

        this.acta.set(respuesta);
        this.estado.set('ready');
        this.visualizacionPortalMensaje.set('Visualización confirmada. La notificación fue registrada como recibida.');
        this.visualizacionPortalMensajeTipo.set('success');
      });
  }

  private cargarActa(codigoQr: string): void {
    this.estado.set('loading');
    this.acta.set(null);
    this.visualizacionPortalMensaje.set(null);
    this.visualizacionPortalMensajeTipo.set(null);

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

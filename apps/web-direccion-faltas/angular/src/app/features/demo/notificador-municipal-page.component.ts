import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, finalize, of } from 'rxjs';
import {
  NotificadorMunicipalNotificacionDemo,
  ResultadoNotificacionDemo,
} from '../../core/models/prototipo-faltas.models';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type CargaEstado = 'idle' | 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-notificador-municipal-page',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <header class="page-header">
      <div>
        <h1>Notificador municipal</h1>
        <p class="hint">Bandeja operativa para localizar notificaciones físicas y cargar acuses.</p>
      </div>
      <div class="header-actions">
        <button mat-stroked-button type="button" (click)="volverADireccion()">Dirección de Faltas</button>
        <button mat-stroked-button type="button" [disabled]="estado() === 'loading'" (click)="cargar()">Refrescar</button>
      </div>
    </header>

    <main class="page">
      <section class="buscadores" aria-label="Búsqueda de notificaciones">
        <label>
          <span>Numero de acta</span>
          <input
            type="search"
            [(ngModel)]="busquedaActa"
            placeholder="Ej. A-2026-0031 o ACTA-0031"
            (keyup.enter)="buscar()"
          />
        </label>
        <label>
          <span>Código QR de notificación</span>
          <input
            type="search"
            [(ngModel)]="busquedaQr"
            placeholder="Ej. QR-NOT-NOT-0031-01-DEMO"
            (keyup.enter)="buscar()"
          />
        </label>
        <div class="buscadores-actions">
          <button mat-flat-button color="primary" type="button" (click)="buscar()">Buscar</button>
          <button mat-stroked-button type="button" (click)="limpiar()">Limpiar</button>
        </div>
      </section>

      @if (mensaje()) {
        <p class="feedback ok" role="status">{{ mensaje() }}</p>
      }
      @if (error()) {
        <p class="feedback error" role="alert">{{ error() }}</p>
      }

      @if (estado() === 'loading') {
        <section class="state">
          <mat-spinner diameter="36"></mat-spinner>
          <p>Cargando notificaciones municipales...</p>
        </section>
      } @else if (estado() === 'error') {
        <section class="state">
          <p>No se pudo cargar el listado.</p>
          <button mat-button type="button" (click)="cargar()">Reintentar</button>
        </section>
      } @else if (visibles().length === 0) {
        <section class="state">
          @if (hayFiltrosActivos()) {
            <p>No hay notificaciones que coincidan con la búsqueda.</p>
          } @else {
            <p>No hay notificaciones pendientes para el notificador municipal.</p>
          }
        </section>
      } @else {
        <section class="listado" aria-label="Listado compacto de notificaciones municipales">
          <p class="resumen">{{ visibles().length }} notificación(es) visible(s)</p>
          @for (n of visibles(); track n.notificacionId) {
            <article class="fila" [class.seleccionada]="seleccionadaId() === n.notificacionId">
              <div class="fila-main" (click)="seleccionar(n.notificacionId)">
                <div class="fila-top">
                  <strong>{{ n.acta }}</strong>
                  <span class="chip">{{ n.tipo }}</span>
                </div>
                <div class="meta">
                  <span><b>ActaId</b> {{ n.actaId }}</span>
                  <span><b>Estado</b> {{ n.estado }}</span>
                  <span><b>Resultado</b> {{ n.resultado }}</span>
                </div>
                <div class="meta">
                  <span><b>Destinatario</b> {{ textoOpcional(n.destinatario) }}</span>
                </div>
                <div class="meta">
                  <span><b>Domicilio</b> {{ textoOpcional(n.domicilio) }}</span>
                </div>
                @if (n.observacion) {
                  <div class="meta obs-previa">
                    <span><b>Observación</b> {{ n.observacion }}</span>
                  </div>
                }
                <div class="meta qr">
                  <span><b>QR</b> <code>{{ codigoQr(n) }}</code></span>
                </div>
              </div>

              <label class="obs" (click)="$event.stopPropagation()">
                <span>Observación de acuse</span>
                <input
                  type="text"
                  [ngModel]="observacion(n.notificacionId)"
                  (ngModelChange)="actualizarObservacion(n.notificacionId, $event)"
                  placeholder="Ej. Entregada en domicilio"
                />
              </label>

              <div class="actions" (click)="$event.stopPropagation()">
                <button
                  mat-flat-button
                  color="primary"
                  type="button"
                  [disabled]="acuseEnCurso() === n.notificacionId"
                  (click)="registrarAcuse(n, 'POSITIVA')"
                >
                  Acuse positivo
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  [disabled]="acuseEnCurso() === n.notificacionId"
                  (click)="registrarAcuse(n, 'NEGATIVA')"
                >
                  Acuse negativo
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  [disabled]="acuseEnCurso() === n.notificacionId"
                  (click)="registrarAcuse(n, 'VENCIDA')"
                >
                  Acuse vencido
                </button>
              </div>
            </article>
          }
        </section>
      }
    </main>
  `,
  styles: [`
    .page-header {
      align-items: center;
      background: #1a237e;
      color: #fff;
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      justify-content: space-between;
      padding: 1rem;
    }
    .page-header h1 { font-size: 1.25rem; margin: 0; }
    .hint { font-size: .85rem; margin: .25rem 0 0; opacity: .9; }
    .header-actions { display: flex; flex-wrap: wrap; gap: .5rem; }
    .page { padding: 1rem; }
    .buscadores {
      background: #f7f8fc;
      border: 1px solid #dde3f0;
      border-radius: .75rem;
      display: grid;
      gap: .75rem;
      margin-bottom: 1rem;
      padding: 1rem;
    }
    .buscadores label { display: grid; gap: .35rem; }
    .buscadores span { color: #444; font-size: .85rem; font-weight: 600; }
    .buscadores input {
      border: 1px solid #bbb;
      border-radius: .5rem;
      font: inherit;
      padding: .55rem .65rem;
    }
    .buscadores-actions { display: flex; flex-wrap: wrap; gap: .5rem; }
    .feedback { border-radius: .5rem; margin: 0 0 1rem; padding: .75rem 1rem; }
    .feedback.ok { background: #e8f5e9; color: #1b5e20; }
    .feedback.error { background: #ffebee; color: #b71c1c; }
    .state {
      align-items: center;
      border: 1px solid #ddd;
      border-radius: .75rem;
      display: flex;
      flex-direction: column;
      gap: .75rem;
      justify-content: center;
      padding: 2rem;
      text-align: center;
    }
    .resumen { color: #555; font-size: .9rem; margin: 0 0 .75rem; }
    .listado { display: grid; gap: .75rem; }
    .fila {
      border: 1px solid #d5dbe8;
      border-radius: .65rem;
      display: grid;
      gap: .65rem;
      padding: .75rem;
    }
    .fila.seleccionada { border-color: #3f51b5; box-shadow: 0 0 0 1px #3f51b5 inset; }
    .fila-main { cursor: pointer; display: grid; gap: .35rem; }
    .fila-top { align-items: center; display: flex; flex-wrap: wrap; gap: .5rem; justify-content: space-between; }
    .chip {
      background: #eef3ff;
      border-radius: 999px;
      color: #244887;
      font-size: .8rem;
      padding: .15rem .55rem;
    }
    .meta { color: #333; display: flex; flex-wrap: wrap; font-size: .88rem; gap: .35rem 1rem; }
    .meta b { color: #666; font-weight: 600; margin-right: .2rem; }
    .obs-previa { color: #555; }
    .qr code { background: #f3f5fa; border-radius: .35rem; font-size: .8rem; padding: .1rem .35rem; }
    .obs { display: grid; gap: .3rem; }
    .obs input {
      border: 1px solid #bbb;
      border-radius: .5rem;
      font: inherit;
      padding: .45rem .55rem;
    }
    .actions { display: flex; flex-wrap: wrap; gap: .5rem; }
  `],
})
export class NotificadorMunicipalPageComponent implements OnInit {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly estado = signal<CargaEstado>('idle');
  readonly todas = signal<NotificadorMunicipalNotificacionDemo[]>([]);
  readonly filtrosAplicados = signal<{ acta: string; qr: string } | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly acuseEnCurso = signal<string | null>(null);
  readonly seleccionadaId = signal<string | null>(null);
  private readonly observaciones = signal<Record<string, string>>({});

  busquedaActa = '';
  busquedaQr = '';

  readonly visibles = computed(() => this.filtrar(this.todas(), this.filtrosAplicados()));
  readonly hayFiltrosActivos = computed(() => {
    const f = this.filtrosAplicados();
    return f !== null && (f.acta.trim().length > 0 || f.qr.trim().length > 0);
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.estado.set('loading');
    this.error.set(null);
    this.api.listarNotificacionesNotificadorMunicipal()
      .pipe(
        catchError((err: unknown) => {
          this.error.set(this.mensajeErrorHttp(err));
          this.estado.set('error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items: NotificadorMunicipalNotificacionDemo[] | null) => {
        if (!items) {
          return;
        }
        this.todas.set(items);
        this.estado.set('ready');
      });
  }

  buscar(): void {
    this.mensaje.set(null);
    this.error.set(null);
    this.filtrosAplicados.set({
      acta: this.busquedaActa.trim(),
      qr: this.busquedaQr.trim(),
    });
  }

  limpiar(): void {
    this.busquedaActa = '';
    this.busquedaQr = '';
    this.filtrosAplicados.set(null);
    this.mensaje.set(null);
    this.error.set(null);
  }

  seleccionar(notificacionId: string): void {
    this.seleccionadaId.set(notificacionId);
  }

  registrarAcuse(n: NotificadorMunicipalNotificacionDemo, resultado: ResultadoNotificacionDemo): void {
    if (this.acuseEnCurso()) {
      return;
    }
    this.mensaje.set(null);
    this.error.set(null);
    this.acuseEnCurso.set(n.notificacionId);
    this.api.registrarAcuseNotificadorMunicipal(n.notificacionId, {
      resultado,
      observacion: this.observacion(n.notificacionId) || this.observacionDefault(resultado),
    })
      .pipe(
        catchError((err: unknown) => {
          this.error.set(this.mensajeErrorHttp(err));
          return of(null);
        }),
        finalize(() => this.acuseEnCurso.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((respuesta) => {
        if (!respuesta) {
          return;
        }
        this.mensaje.set(`${respuesta.mensaje} ${respuesta.acta}: ${respuesta.notificacion.resultado}.`);
        this.actualizarObservacion(n.notificacionId, '');
        this.cargar();
      });
  }

  codigoQr(n: NotificadorMunicipalNotificacionDemo): string {
    if (n.qrNotificacion && n.qrNotificacion.trim().length > 0) {
      return n.qrNotificacion;
    }
    return `QR-NOT-${n.notificacionId}-DEMO`;
  }

  observacion(notificacionId: string): string {
    return this.observaciones()[notificacionId] ?? '';
  }

  actualizarObservacion(notificacionId: string, valor: string): void {
    this.observaciones.update((actual) => ({ ...actual, [notificacionId]: valor }));
  }

  textoOpcional(valor: string | null | undefined): string {
    return valor && valor.trim().length > 0 ? valor : '-';
  }

  volverADireccion(): void {
    window.location.href = '/';
  }

  private filtrar(
    items: NotificadorMunicipalNotificacionDemo[],
    filtros: { acta: string; qr: string } | null,
  ): NotificadorMunicipalNotificacionDemo[] {
    if (!filtros) {
      return items;
    }
    const acta = filtros.acta.toLowerCase();
    const qr = filtros.qr.toLowerCase();
    if (!acta && !qr) {
      return items;
    }
    return items.filter((n) => {
      const matchActa = !acta
        || n.acta.toLowerCase().includes(acta)
        || n.actaId.toLowerCase().includes(acta);
      const matchQr = !qr || this.codigoQr(n).toLowerCase() === qr;
      return matchActa && matchQr;
    });
  }

  private observacionDefault(resultado: ResultadoNotificacionDemo): string {
    if (resultado === 'POSITIVA') {
      return 'Entregada por notificador municipal';
    }
    if (resultado === 'NEGATIVA') {
      return 'Acuse negativo informado por notificador municipal';
    }
    return 'Notificación vencida informada por notificador municipal';
  }

  private mensajeErrorHttp(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const error = (err as { error?: { message?: string } | string }).error;
      if (typeof error === 'string' && error.trim().length > 0) {
        return error;
      }
      if (error && typeof error === 'object' && typeof error.message === 'string') {
        return error.message;
      }
    }
    return 'No se pudo completar la operación.';
  }
}

import { Component, DestroyRef, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { catchError, finalize, of } from 'rxjs';
import {
  GenerarLoteCorreoResultadoDemo,
  ProcesarRespuestaCorreoResultadoDemo,
} from '../../core/models/prototipo-faltas.models';
import { PrototipoFaltasApiService } from '../../core/services/prototipo-faltas-api.service';

type CorreoPostalAccion = 'GENERAR_LOTE' | 'PROCESAR_RESPUESTA';

@Component({
  selector: 'app-demo-correo-postal-panel',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './demo-correo-postal-panel.component.html',
  styleUrl: './demo-correo-postal-panel.component.scss',
})
export class DemoCorreoPostalPanelComponent {
  private readonly api = inject(PrototipoFaltasApiService);
  private readonly destroyRef = inject(DestroyRef);

  @Output() readonly cerrar = new EventEmitter<void>();
  @Output() readonly operaciónCompletada = new EventEmitter<void>();

  readonly accionEnCurso = signal<CorreoPostalAccion | null>(null);
  readonly error = signal<string | null>(null);
  readonly advertencia = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly ultimoLote = signal<GenerarLoteCorreoResultadoDemo | null>(null);
  readonly lotesGenerados = signal<GenerarLoteCorreoResultadoDemo[]>([]);
  readonly loteSeleccionadoId = signal<string | null>(null);
  readonly ÚltimaRespuesta = signal<ProcesarRespuestaCorreoResultadoDemo | null>(null);
  readonly loteSeleccionado = computed(() => {
    const loteId = this.loteSeleccionadoId();
    const lotes = this.lotesGenerados();
    return lotes.find((lote) => lote.loteId === loteId) ?? this.ultimoLote();
  });

  generarLoteCorreoPostal(): void {
    if (this.accionEnCurso() !== null) {
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('GENERAR_LOTE');

    this.api
      .generarLoteCorreoPostalDemo()
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
        this.ultimoLote.set(respuesta);
        this.lotesGenerados.update((lotes) => [...lotes, respuesta]);
        this.loteSeleccionadoId.set(respuesta.loteId);
        if (respuesta.cantidad === 0) {
          this.advertencia.set('No hay notificaciónes de correo postal listas para enviar.');
        } else {
          this.mensaje.set(
            `Lote correo ${respuesta.loteId} generado con ${respuesta.cantidad} notificaciónes.`,
          );
        }
        this.operaciónCompletada.emit();
      });
  }

  procesarRespuestaCorreoPostalDemo(): void {
    if (this.accionEnCurso() !== null) {
      return;
    }

    this.limpiarFeedback();
    this.accionEnCurso.set('PROCESAR_RESPUESTA');

    this.api
      .procesarRespuestaCorreoPostalDemo()
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
        this.ÚltimaRespuesta.set(respuesta);
        const base = `Respuesta correo procesada desde ${respuesta.nombreArchivo ?? 'CSV demo local'}: ${respuesta.total} filas, ${respuesta.errores} errores.`;
        if (respuesta.errores > 0) {
          this.advertencia.set(base);
        } else {
          this.mensaje.set(base);
        }
        this.operaciónCompletada.emit();
      });
  }

  seleccionarLote(loteId: string): void {
    this.loteSeleccionadoId.set(loteId);
  }

  private limpiarFeedback(): void {
    this.error.set(null);
    this.advertencia.set(null);
    this.mensaje.set(null);
  }
}

function mensajeErrorHttp(err: unknown): string {
  if (err && typeof err === 'object' && 'status' in err) {
    const status = (err as { status?: number }).status;
    const detalle = mensajeBackend(err);
    if (status === 0) {
      return 'Sin conexión con el backend prototipo.';
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
  const candidate = body as { message?: unknown; detail?: unknown; title?: unknown; error?: unknown };
  for (const value of [candidate.message, candidate.detail, candidate.error, candidate.title]) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return null;
}

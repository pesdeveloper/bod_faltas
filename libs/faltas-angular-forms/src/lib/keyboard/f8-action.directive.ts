import {
  Directive, HostListener, OnDestroy, OnInit, Input, inject
} from '@angular/core';
import { KeyboardAction, KeyboardActionService } from './keyboard-action.service';

/**
 * Directiva F8: registra la accion primaria del contexto activo.
 *
 * El componente declara su accion con el input [appF8Action];
 * la directiva la registra en KeyboardActionService al inicializarse
 * y la desregistra al destruirse.
 *
 * Escucha F8 globalmente (document:keydown). Cuando el panel de autocompletado
 * o cualquier modal esta abierto, el enfoque ya no es el formulario, por lo que
 * F8 no ejecuta la accion si el contexto activo no la registro.
 *
 * Uso tipico:
 *   <form [appF8Action]="miAccion">
 *
 * Implementacion canonica — no duplicar en aplicaciones individuales.
 * @faltas/angular-forms
 */
@Directive({
  selector: '[appF8Action]',
  standalone: true,
})
export class F8ActionDirective implements OnInit, OnDestroy {
  @Input({ required: true, alias: 'appF8Action' }) action!: KeyboardAction;

  private readonly kbService = inject(KeyboardActionService);

  ngOnInit(): void {
    if (this.action) {
      this.kbService.register(this.action);
    }
  }

  ngOnDestroy(): void {
    if (this.action) {
      this.kbService.unregister(this.action.id);
    }
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: Event): void {
    const ke = event as KeyboardEvent;
    if (ke.key === 'F8') {
      ke.preventDefault();
      this.kbService.execute();
    }
  }
}

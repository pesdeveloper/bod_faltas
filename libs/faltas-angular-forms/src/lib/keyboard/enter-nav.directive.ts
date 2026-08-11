import {
  Directive, ElementRef, HostListener, Input, OnDestroy, inject
} from '@angular/core';

/**
 * Directiva reutilizable de navegacion por teclado en formularios.
 *
 * Comportamiento:
 *  - Enter en un campo de datos avanza al siguiente control habilitado/visible.
 *  - Shift+Enter vuelve al control anterior.
 *  - En textarea, Enter crea una nueva linea (comportamiento nativo conservado).
 *  - En botones y checkboxes, Enter/Espacio conservan su activacion nativa.
 *  - En autocomplete (mat-autocomplete): Enter primero selecciona la sugerencia activa;
 *    el foco avanza solo cuando el panel esta cerrado.
 *  - No envia formularios al presionar Enter desde un campo de datos.
 *
 * Aplicable en: GeoDomicilios, formulario web del acta, futuros formularios Angular.
 * Implementacion canonica — no duplicar en aplicaciones individuales.
 *
 * @faltas/angular-forms
 */
@Directive({
  selector: '[appEnterNav]',
  standalone: true,
})
export class EnterNavDirective implements OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>);

  @Input() enterNavSelector =
    'input:not([type="hidden"]):not([disabled]), ' +
    'select:not([disabled]), ' +
    'textarea:not([disabled]), ' +
    'button:not([disabled]), ' +
    '[tabindex]:not([tabindex="-1"]):not([disabled])';

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter') return;

    const target = event.target as HTMLElement;
    if (!target) return;

    if (target.tagName === 'TEXTAREA') return;

    const role = target.getAttribute('role');
    if (
      target.tagName === 'BUTTON' ||
      target.tagName === 'A' ||
      role === 'button' ||
      role === 'option' ||
      (target.tagName === 'INPUT' &&
        ['checkbox', 'radio', 'submit', 'button', 'reset'].includes(
          (target as HTMLInputElement).type
        ))
    ) {
      return;
    }

    const autocompleteOpen =
      !!document.querySelector('.mat-mdc-autocomplete-panel') ||
      !!document.querySelector('mat-option:focus');
    if (autocompleteOpen) return;

    const selectOpen = !!document.querySelector('.mat-select-panel');
    if (selectOpen) return;

    event.preventDefault();
    event.stopPropagation();

    const focusables = this.getFocusables();
    if (focusables.length === 0) return;

    const currentIndex = focusables.indexOf(target);
    if (currentIndex === -1) return;

    const delta = event.shiftKey ? -1 : 1;
    const nextIndex = (currentIndex + delta + focusables.length) % focusables.length;
    const next = focusables[nextIndex];
    next.focus();
  }

  private getFocusables(): HTMLElement[] {
    const all = Array.from(
      this.host.nativeElement.querySelectorAll(this.enterNavSelector)
    ) as HTMLElement[];
    return all.filter(el => !el.closest('[hidden]') && this.isVisible(el));
  }

  private isVisible(el: HTMLElement): boolean {
    const style = window.getComputedStyle(el);
    return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
  }

  ngOnDestroy(): void {}
}

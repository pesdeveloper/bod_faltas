/*
 * @faltas/angular-forms — API publica de la libreria compartida.
 *
 * Solo se exporta lo que las aplicaciones consumidoras necesitan.
 * No reexportar implementacion interna.
 */

// Teclado / navegacion de formularios
export { EnterNavDirective }     from './lib/keyboard/enter-nav.directive';
export { F8ActionDirective }     from './lib/keyboard/f8-action.directive';
export { KeyboardActionService } from './lib/keyboard/keyboard-action.service';
export type { KeyboardAction }   from './lib/keyboard/keyboard-action.service';

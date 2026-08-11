import { Injectable } from '@angular/core';

/** Contrato de una accion primaria invocable via F8. */
export interface KeyboardAction {
  /** Identificador unico de la accion en el contexto. */
  id: string;
  /** Etiqueta legible (puede mostrarse en UI). */
  label: string;
  /** Funcion que ejecuta la accion. */
  fn: () => void;
  /** Cuando false, F8 no dispara la accion. */
  enabled: boolean;
  /** Cuando true, la accion ya esta en curso; F8 no la reencola. */
  loading?: boolean;
}

/**
 * Registro de la accion primaria activa en el contexto visible.
 *
 * Solo un componente (tab, vista) registra su accion a la vez.
 * Al destruirse, la desregistra. El servicio no mantiene pila.
 *
 * Implementacion canonica — no duplicar en aplicaciones individuales.
 * @faltas/angular-forms
 */
@Injectable({ providedIn: 'root' })
export class KeyboardActionService {
  private currentAction: KeyboardAction | null = null;

  /** Registra la accion primaria del contexto activo. */
  register(action: KeyboardAction): void {
    this.currentAction = action;
  }

  /** Desregistra si el id coincide con la accion activa. */
  unregister(id: string): void {
    if (this.currentAction?.id === id) {
      this.currentAction = null;
    }
  }

  /** Devuelve la accion activa o null. */
  getCurrentAction(): KeyboardAction | null {
    return this.currentAction;
  }

  /**
   * Ejecuta la accion activa si existe, esta habilitada y no esta cargando.
   * No lanza excepcion si no hay accion registrada.
   */
  execute(): void {
    const action = this.currentAction;
    if (!action) return;
    if (!action.enabled) return;
    if (action.loading) return;
    action.fn();
  }
}

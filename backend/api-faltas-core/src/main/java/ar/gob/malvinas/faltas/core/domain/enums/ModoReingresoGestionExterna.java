package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Modo en que el expediente reingresara al circuito interno al retornar de gestion externa.
 *
 * Valores del catalogo productivo modo_reingreso_gestion_ext.
 * El campo es nullable: NULL antes de que ocurra el reingreso (estado DERIVADA o EN_CURSO).
 *
 * Implementados funcionalmente:
 *   REINGRESO_PARA_REVISION    - revision interna generica (Slice 6B / 6D-1).
 *                                Reemplaza REINGRESAR_A_ANALISIS. Par valido: SIN_CAMBIOS.
 *   REINGRESO_SIN_PAGO         - vuelve sin pago para continuar circuito interno de cobro
 *                                (Slice 6B / 6D-1). Reemplaza REINGRESAR_A_PAGO_CONDENA.
 *                                Par valido: SIN_PAGO. Requiere CONDENA_FIRME.
 *   REINGRESO_PARA_NUEVO_FALLO - vuelve para dictar nuevo fallo (Slice 6D-2).
 *                                Par valido: ABSUELVE. Requiere CONDENA_FIRME.
 *   REINGRESO_CON_DICTAMEN     - vuelve con dictamen/resolucion externa (Slice 6D-2).
 *                                Pares validos: CONFIRMA_CONDENA o MODIFICA_MONTO. Requiere CONDENA_FIRME.
 *
 * Reservados (bloqueados con PrecondicionVioladaException):
 *   REINGRESO_CON_PAGO    - reservado para slice posterior (fuera de PAGAPR, no habilitado).
 *   REINGRESO_PARA_CIERRE - reservado; requiere decision de cierre definitivo.
 */
public enum ModoReingresoGestionExterna {
    REINGRESO_CON_PAGO,
    REINGRESO_SIN_PAGO,
    REINGRESO_CON_DICTAMEN,
    REINGRESO_PARA_NUEVO_FALLO,
    REINGRESO_PARA_CIERRE,
    REINGRESO_PARA_REVISION
}

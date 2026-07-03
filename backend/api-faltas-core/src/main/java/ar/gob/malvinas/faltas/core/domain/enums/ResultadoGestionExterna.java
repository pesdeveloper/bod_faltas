package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado final de la gestion externa.
 *
 * Valores del catalogo productivo resultado_gestion_ext.
 *
 * Implementados funcionalmente:
 *   SIN_RESULTADO    - estado inicial al derivar (Slice 6A). No valido para reingreso manual.
 *   PAGO_REGISTRADO  - pago externo registrado (Slice 6C, reemplaza PAGO_EXTERNO_INFORMADO).
 *                      Exclusivo de PAGAPR. No valido para reingreso manual.
 *   SIN_CAMBIOS      - reingresa sin cambios sustantivos (Slice 6D-1). Par: REINGRESO_PARA_REVISION.
 *   SIN_PAGO         - reingresa sin pago (Slice 6D-1). Par: REINGRESO_SIN_PAGO. Requiere CONDENA_FIRME.
 *   ABSUELVE         - el externo propone absolver (Slice 6D-2). Par: REINGRESO_PARA_NUEVO_FALLO.
 *   CONFIRMA_CONDENA - el externo confirma condena (Slice 6D-2). Par: REINGRESO_CON_DICTAMEN.
 *   MODIFICA_MONTO   - el externo modifica monto (Slice 6D-2). Par: REINGRESO_CON_DICTAMEN.
 *                      Requiere montoResultado > 0.
 */
public enum ResultadoGestionExterna {
    SIN_RESULTADO,
    SIN_CAMBIOS,
    PAGO_REGISTRADO,
    SIN_PAGO,
    ABSUELVE,
    CONFIRMA_CONDENA,
    MODIFICA_MONTO
}

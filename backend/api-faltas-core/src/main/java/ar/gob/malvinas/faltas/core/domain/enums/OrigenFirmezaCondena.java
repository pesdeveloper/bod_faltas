package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de la firmeza de condena declarada.
 * Indica por que via quedo firme el fallo condenatorio.
 *
 * VENCIMIENTO_PLAZO_APELACION: el plazo de apelacion vencio sin presentacion.
 * APELACION_RECHAZADA: la apelacion presentada fue rechazada.
 */
public enum OrigenFirmezaCondena {
    VENCIMIENTO_PLAZO_APELACION,
    APELACION_RECHAZADA
}
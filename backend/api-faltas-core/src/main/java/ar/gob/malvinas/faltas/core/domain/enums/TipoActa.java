package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de acta.
 *
 * Refleja el catalogo real de fal_dependencia_version.tipo_acta en MariaDB.
 * Cada dependencia versionada define un unico tipo_acta que puede labrar.
 *
 * ALCOHOLEMIA no es un tipo de acta: es un satelite eventual de un acta de transito.
 *
 * Slice 8A-1: implementacion in-memory. Slice 9: persiste como columna enum MariaDB.
 */
public enum TipoActa {
    TRANSITO,
    CONTRAVENCION,
    SUSTANCIAS_ALIMENTICIAS,
    COMERCIO
}

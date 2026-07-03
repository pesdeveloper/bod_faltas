package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Situacion administrativa general del expediente.
 *
 * Representa el estado operativo/administrativo del acta, separado del estado
 * procesal (EstadoProcesalActa) y del bloque de trabajo actual (BloqueActual).
 *
 * Alimenta el routing de bandejas para situaciones transversales:
 * paralizacion, archivo, cierre y anulacion.
 */
public enum SituacionAdministrativaActa {

    ACTIVA,

    PARALIZADA,

    EN_GESTION_EXTERNA,

    ARCHIVADA,

    CERRADA,

    ANULADA
}
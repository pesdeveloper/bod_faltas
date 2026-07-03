package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado procesal del acta en el circuito administrativo.
 *
 * Representa la situacion juridico-procesal del tramite, no la situacion
 * administrativa operativa (esa vive en SituacionAdministrativaActa).
 *
 * Codigo persistible como CHAR(10) en MariaDB.
 */
public enum EstadoProcesalActa {

    EN_TRAMITE,

    CONCLUIDO,

    PRESCRIPTO
}
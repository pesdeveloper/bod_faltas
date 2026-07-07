package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Situacion administrativa general del expediente.
 *
 * Representa el estado operativo/administrativo del acta, separado del estado
 * procesal (EstadoProcesalActa) y del bloque de trabajo actual (BloqueActual).
 *
 * Codigo estable CHAR(4) persistible en MariaDB (sit_adm_act / sit_adm_ant / sit_adm_nva).
 * No persistir ordinales.
 *
 * Alimenta el routing de bandejas para situaciones transversales:
 * paralizacion, archivo, cierre y anulacion.
 */
public enum SituacionAdministrativaActa {

    ACTIVA("ACTV"),
    PARALIZADA("PARA"),
    EN_GESTION_EXTERNA("GEXT"),
    ARCHIVADA("ARCH"),
    CERRADA("CERR"),
    ANULADA("ANUL");

    private final String codigo;

    SituacionAdministrativaActa(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static SituacionAdministrativaActa deCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("SituacionAdministrativaActa: codigo no puede ser nulo o vacio");
        }
        for (SituacionAdministrativaActa v : values()) {
            if (v.codigo.equals(codigo.trim())) return v;
        }
        throw new IllegalArgumentException("SituacionAdministrativaActa no reconocido: '" + codigo + "'");
    }
}
package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado procesal del acta en el circuito administrativo.
 *
 * Representa la situacion juridico-procesal del tramite, no la situacion
 * administrativa operativa (esa vive en SituacionAdministrativaActa).
 *
 * Codigo estable CHAR(4) persistible en MariaDB (est_proc_act / est_proc_ant / est_proc_nvo).
 * No persistir ordinales.
 */
public enum EstadoProcesalActa {

    EN_TRAMITE("TRAM"),
    CONCLUIDO("CONC"),
    PRESCRIPTO("PRSC");

    private final String codigo;

    EstadoProcesalActa(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static EstadoProcesalActa deCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("EstadoProcesalActa: codigo no puede ser nulo o vacio");
        }
        for (EstadoProcesalActa v : values()) {
            if (v.codigo.equals(codigo.trim())) return v;
        }
        throw new IllegalArgumentException("EstadoProcesalActa no reconocido: '" + codigo + "'");
    }
}

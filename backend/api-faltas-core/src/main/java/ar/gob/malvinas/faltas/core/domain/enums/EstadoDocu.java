package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de estados del documento.
 *
 * Refleja el catalogo estado_docu (SMALLINT) de MariaDB.
 * 7 valores definitivos cerrados en 8C-0D.
 *
 * Ciclo de vida consolidado del documento (no confundir con estado_firma individual).
 * FIRMADO en este enum es el estado agregado del documento, no de una firma individual.
 */
public enum EstadoDocu {

    BORRADOR((short) 1),
    EMITIDO((short) 2),
    PENDIENTE_FIRMA((short) 3),
    FIRMADO((short) 4),
    ADJUNTO((short) 5),
    ANULADO((short) 6),
    REEMPLAZADO((short) 7);

    private final short codigo;

    EstadoDocu(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoDocu desdeCodigo(short codigo) {
        for (EstadoDocu e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoDocu desconocido: " + codigo);
    }
}

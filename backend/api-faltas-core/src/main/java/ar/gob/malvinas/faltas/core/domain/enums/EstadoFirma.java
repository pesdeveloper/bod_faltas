package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de estados de firma de documento.
 *
 * Refleja el catalogo estado_firma (SMALLINT) de fal_documento_firma en MariaDB.
 * 6 valores definitivos cerrados en 8C-0D.
 *
 * Aplica a fal_documento_firma.estado_firma.
 * No reemplaza EstadoDocu (que es el estado agregado del documento).
 */
public enum EstadoFirma {

    PENDIENTE((short) 1),
    SOLICITADA((short) 2),
    FIRMADA((short) 3),
    RECHAZADA((short) 4),
    ANULADA((short) 5),
    ERROR((short) 6);

    private final short codigo;

    EstadoFirma(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoFirma desdeCodigo(short codigo) {
        for (EstadoFirma e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoFirma desconocido: " + codigo);
    }
}

package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de requisito de firma documental.
 *
 * Refleja el catalogo tipo_firma_req (SMALLINT) de MariaDB.
 * 6 valores definitivos cerrados en 8C-0D.
 *
 * FIRMA_MULTIPLE reemplaza al valor prohibido FIRMA_MIXTA.
 * FIRMA_MULTIPLE significa que el documento requiere dos o mas requisitos
 * de firma documental o administrativa activos simultaneamente.
 *
 * No incluye firma olografa del infractor (ese circuito es propio del acta).
 * No incluye firma automatica del inspector en el acta.
 */
public enum TipoFirmaReq {

    NO_REQUIERE((short) 0),
    FIRMA_INTERNA((short) 1),
    FIRMA_INSPECTOR((short) 2),
    FIRMA_AUTORIDAD((short) 3),
    FIRMA_DIGITAL((short) 4),
    FIRMA_MULTIPLE((short) 5);

    private final short codigo;

    TipoFirmaReq(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoFirmaReq desdeCodigo(short codigo) {
        for (TipoFirmaReq t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("TipoFirmaReq desconocido: " + codigo);
    }
}

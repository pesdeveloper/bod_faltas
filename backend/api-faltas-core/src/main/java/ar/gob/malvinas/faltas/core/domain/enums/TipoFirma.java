package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de firma registrada en fal_documento_firma.
 *
 * Refleja el catalogo tipo_firma (SMALLINT) de MariaDB.
 * Slice 8C-6B-1.
 *
 * Distinto de TipoFirmaReq (requisito de firma del documento).
 * Distinto de EstadoFirma y EstadoFirmaReq.
 * No representa la firma del infractor en el acta (FIRMA_OLOGRAFA_INFRACTOR).
 */
public enum TipoFirma {

    DIGITAL((short) 1),
    ELECTRONICA((short) 2),
    OLOGRAFA((short) 3),
    SISTEMA((short) 4);

    private final short codigo;

    TipoFirma(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoFirma desdeCodigo(short codigo) {
        for (TipoFirma t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("TipoFirma desconocido: " + codigo);
    }
}

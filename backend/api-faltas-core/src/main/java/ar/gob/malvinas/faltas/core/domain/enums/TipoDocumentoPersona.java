package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de documento de identidad de la persona.
 * SMALLINT en fal_persona.tipo_doc.
 *
 * Valores definitivos del catalogo productivo.
 * DNI es el tipo principal para personas fisicas en Argentina.
 * CUIT y CUIL incluyen el codigo de verificacion.
 */
public enum TipoDocumentoPersona {
    DNI((short) 1),
    CUIT((short) 2),
    CUIL((short) 3),
    PASAPORTE((short) 4),
    DNI_EXTRANJERO((short) 5),
    OTRO((short) 9);

    private final short codigo;

    TipoDocumentoPersona(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoDocumentoPersona fromCodigo(short codigo) {
        for (TipoDocumentoPersona v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoDocumentoPersona desconocido: " + codigo);
    }
}

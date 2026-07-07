package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de documento presentado dentro de una apelacion (tipo_doc_apelacion en fal_acta_apelacion_documento).
 */
public enum TipoDocumentoApelacion {

    ESCRITO_APELACION((short) 1),
    NOTA_INFRACTOR((short) 2),
    ESCRITO_ABOGADO((short) 3),
    EVIDENCIA_INFRACTOR((short) 4),
    EVIDENCIA_ABOGADO((short) 5),
    DOCUMENTACION_RESPALDATORIA((short) 6),
    CONSTANCIA_PRESENTACION((short) 7),
    OTRO((short) 8);

    private final short codigo;

    TipoDocumentoApelacion(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoDocumentoApelacion fromCodigo(short cod) {
        for (TipoDocumentoApelacion v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("TipoDocumentoApelacion desconocido: " + cod);
    }
}
package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de presentacion de la apelacion (tipo_presentacion en fal_acta_apelacion).
 */
public enum TipoPresentacion {

    TEXTO((short) 1),
    DOCUMENTOS((short) 2),
    MIXTA((short) 3);

    private final short codigo;

    TipoPresentacion(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoPresentacion fromCodigo(short cod) {
        for (TipoPresentacion v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("TipoPresentacion desconocido: " + cod);
    }
}

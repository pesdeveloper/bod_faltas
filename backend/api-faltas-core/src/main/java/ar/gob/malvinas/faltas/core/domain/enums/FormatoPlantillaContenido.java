package ar.gob.malvinas.faltas.core.domain.enums;

public enum FormatoPlantillaContenido {
    TEXTO_PLANO((short) 1),
    HTML_SIMPLE((short) 2),
    MARKDOWN_SIMPLE((short) 3);

    private final short codigo;

    FormatoPlantillaContenido(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() { return codigo; }

    public static FormatoPlantillaContenido desdeCodigo(short codigo) {
        for (FormatoPlantillaContenido f : values()) {
            if (f.codigo == codigo) return f;
        }
        throw new IllegalArgumentException("FormatoPlantillaContenido desconocido: " + codigo);
    }
}

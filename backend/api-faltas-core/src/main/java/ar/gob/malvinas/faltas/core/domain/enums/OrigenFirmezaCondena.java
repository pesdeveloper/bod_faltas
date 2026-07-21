package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de la firmeza de condena declarada.
 * Indica por que via quedo firme el fallo condenatorio.
 */
public enum OrigenFirmezaCondena {
    VENCIMIENTO_PLAZO_APELACION((short) 1),
    APELACION_RECHAZADA((short) 2),
    CONSENTIMIENTO_EXPRESO((short) 3);

    private final short codigo;

    OrigenFirmezaCondena(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static OrigenFirmezaCondena fromCodigo(short codigo) {
        for (OrigenFirmezaCondena v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenFirmezaCondena sin codigo: " + codigo);
    }
}

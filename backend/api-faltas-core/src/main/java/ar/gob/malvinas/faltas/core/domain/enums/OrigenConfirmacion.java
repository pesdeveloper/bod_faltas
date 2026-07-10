
package ar.gob.malvinas.faltas.core.domain.enums;

public enum OrigenConfirmacion {
    TESORERIA((short) 1),
    CAJA((short) 2),
    INGRESOS((short) 3),
    ENTIDAD_RECAUDADORA((short) 4),
    USUARIO_FALTAS((short) 5),
    INTEGRACION((short) 6);

    private final short codigo;

    OrigenConfirmacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenConfirmacion fromCodigo(short codigo) {
        for (OrigenConfirmacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenConfirmacion desconocido: " + codigo);
    }
}

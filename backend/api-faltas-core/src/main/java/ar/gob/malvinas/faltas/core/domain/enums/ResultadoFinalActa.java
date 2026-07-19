package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado final del circuito del acta.
 */
public enum ResultadoFinalActa {

    SIN_RESULTADO_FINAL((short) 0),
    PAGO_VOLUNTARIO_PAGADO((short) 1),
    ABSUELTO((short) 2),
    CONDENA_FIRME((short) 3),
    CONDENA_FIRME_PAGADA((short) 4),
    FALLO_CONDENATORIO_PAGADO((short) 5),
    FALLO_CONDENATORIO_GESTION_EXTERNA((short) 6),
    PRESCRIPTO((short) 7),
    ANULADO((short) 8),
    NULIDAD((short) 9);

    private final short codigo;

    ResultadoFinalActa(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static ResultadoFinalActa fromCodigo(short codigo) {
        for (ResultadoFinalActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("ResultadoFinalActa sin codigo: " + codigo);
    }
}

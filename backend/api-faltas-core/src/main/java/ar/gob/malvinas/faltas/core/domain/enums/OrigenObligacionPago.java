package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de una obligacion de pago de faltas (DECISION_DDL-PAGO-03).
 *
 * FALTAS:   obligacion determinada internamente por el sistema de faltas (default).
 * APREMIO:  obligacion originada por un proceso de apremio externo.
 * JUZGADO:  obligacion determinada por resolucion judicial.
 *
 * Codigo numerico persistible: SMALLINT, prohibido ordinal().
 */
public enum OrigenObligacionPago {
    FALTAS((short) 1),
    APREMIO((short) 2),
    JUZGADO((short) 3);

    private final short codigo;

    OrigenObligacionPago(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static OrigenObligacionPago desdeCodigo(short codigo) {
        for (OrigenObligacionPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenObligacionPago sin codigo: " + codigo);
    }
}

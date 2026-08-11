package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Bloques productivos del circuito de un acta de faltas.
 *
 * Código de 4 caracteres persistible como CHAR(4) en MariaDB.
 * D3_DOCUMENTAL no es un bloque: se representa mediante bandeja documental,
 * fal_documento y estados de firma.
 * CAPT no es un bloque: el alta es integral y el acta nace directamente en
 * ENRI (DECISION-ACTA-LIFECYCLE-SIN-CAPT-01).
 */
public enum BloqueActual {

    ENRI("ENRI", "Enriquecimiento/completitud del acta; bloque de nacimiento del acta"),
    NOTI("NOTI", "Notificación del acta, fallo u otra pieza"),
    ANAL("ANAL", "Análisis, resolución, fallo, pagos y apelación"),
    GEXT("GEXT", "Gestión externa: apremio / juzgado de paz"),
    ARCH("ARCH", "Archivo administrativo/procesal"),
    CERR("CERR", "Cierre definitivo del circuito");

    private final String codigo;
    private final String descripcion;

    BloqueActual(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String codigo() { return codigo; }
    public String descripcion() { return descripcion; }

    public static BloqueActual deCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("bloque_actual no puede ser nulo ni vacio");
        }
        for (BloqueActual b : values()) {
            if (b.codigo.equals(codigo.trim())) return b;
        }
        throw new IllegalArgumentException("bloque_actual no reconocido: '" + codigo + "'");
    }
}

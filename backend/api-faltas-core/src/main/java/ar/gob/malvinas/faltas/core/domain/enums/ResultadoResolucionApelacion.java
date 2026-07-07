package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado de la resolucion de una apelacion (resultado_resolucion en fal_acta_apelacion).
 */
public enum ResultadoResolucionApelacion {

    RECHAZADA((short) 1),
    ACEPTADA_ABSUELVE((short) 2),
    MODIFICA_CONDENA((short) 3),
    NULIDAD((short) 4);

    private final short codigo;

    ResultadoResolucionApelacion(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static ResultadoResolucionApelacion fromCodigo(short cod) {
        for (ResultadoResolucionApelacion v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("ResultadoResolucionApelacion desconocido: " + cod);
    }
}
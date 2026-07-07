package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivos de paralizacion de un acta.
 * Codigos enteros unicos. Sin ordinal.
 *
 * OTRO requiere observacion vinculada al ciclo.
 */
public enum MotivoParalizacion {

    ESPERA_DOCUMENTAL(1),
    OTRO(2);

    private final int codigo;

    MotivoParalizacion(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static MotivoParalizacion fromCodigo(int codigo) {
        for (MotivoParalizacion m : values()) {
            if (m.codigo == codigo) return m;
        }
        throw new IllegalArgumentException("MotivoParalizacion no reconocido para codigo: " + codigo);
    }
}

package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Canal por el que se presento la apelacion (canal_apelacion en fal_acta_apelacion).
 */
public enum CanalApelacion {

    PORTAL_INFRACTOR((short) 1),
    PRESENCIAL((short) 2),
    MESA_ENTRADA((short) 3),
    EXTERNO((short) 4);

    private final short codigo;

    CanalApelacion(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static CanalApelacion fromCodigo(short cod) {
        for (CanalApelacion v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("CanalApelacion desconocido: " + cod);
    }
}

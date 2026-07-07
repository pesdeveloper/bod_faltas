package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Canal por el cual se realizo un acceso via QR al expediente.
 * Unico valor persistible: tabla fal_acta_qr_acceso.canal_acceso SMALLINT.
 */
public enum CanalAccesoQr {

    PORTAL((short) 1),
    APP((short) 2),
    INTEGRACION((short) 3),
    OTRO((short) 4);

    private final short codigo;

    CanalAccesoQr(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static CanalAccesoQr fromCodigo(short codigo) {
        for (CanalAccesoQr c : values()) {
            if (c.codigo == codigo) return c;
        }
        throw new IllegalArgumentException("CanalAccesoQr no reconocido: " + codigo);
    }
}

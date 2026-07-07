package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado del acceso via QR.
 *
 * Solo se persiste VALIDO. Los casos invalidos, expirados, rechazados
 * o con error NO se insertan en fal_acta_qr_acceso.
 * La tabla es auditable y solo contiene accesos tecnicamente resueltos.
 */
public enum ResultadoAccesoQr {

    VALIDO((short) 1);

    private final short codigo;

    ResultadoAccesoQr(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static ResultadoAccesoQr fromCodigo(short codigo) {
        for (ResultadoAccesoQr r : values()) {
            if (r.codigo == codigo) return r;
        }
        throw new IllegalArgumentException("ResultadoAccesoQr no reconocido: " + codigo);
    }
}

package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de estados de requisito de firma documental.
 *
 * Refleja el catalogo estado_firma_req (SMALLINT) de fal_documento_firma_req en MariaDB.
 * Es un catalogo DISTINTO de EstadoFirma (que aplica a fal_documento_firma).
 *
 * Valores cerrados en 8C-4.
 *
 * Aplica a fal_documento_firma_req.estado_firma_req.
 */
public enum EstadoFirmaReq {

    PENDIENTE((short) 1),
    FIRMADO((short) 2),
    ANULADO((short) 3),
    VENCIDO((short) 4),
    REEMPLAZADO((short) 5);

    private final short codigo;

    EstadoFirmaReq(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoFirmaReq desdeCodigo(short codigo) {
        for (EstadoFirmaReq e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoFirmaReq desconocido: " + codigo);
    }
}

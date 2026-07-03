package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de evidencia del acta (tipo_evid en fal_acta_evidencia).
 *
 * Valor 6 = FIRMA_OLOGRAFA_INFRACTOR: imagen/captura de la firma olografa del infractor.
 * No representa firma institucional. No participa en FalDocumentoFirma ni FalDocumentoFirmaReq.
 */
public enum TipoEvidenciaActa {

    FIRMA_OLOGRAFA_INFRACTOR((short) 6);

    private final short codigo;

    TipoEvidenciaActa(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoEvidenciaActa desdeCodigo(short codigo) {
        for (TipoEvidenciaActa t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("TipoEvidenciaActa desconocido: " + codigo);
    }
}

package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de evidencia del acta (tipo_evid en fal_acta_evidencia).
 *
 * Codigos en multiplos de 4; espacios 28-44 reservados para futuros tipos.
 * HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10
 *
 * 4  = FOTO: evidencia fotografica o imagen.
 * 8  = VIDEO: archivo de video.
 * 12 = AUDIO: archivo de audio.
 * 16 = PDF: documento PDF.
 * 20 = DOCUMENTO_OFIMATICO: DOC, DOCX, ODT y formatos equivalentes.
 * 24 = PLANILLA_CALCULO: XLS, XLSX, ODS y formatos equivalentes.
 * 48 = FIRMA_OLOGRAFA_INFRACTOR: firma manuscrita o digitalizada del infractor.
 *
 * FIRMA_OLOGRAFA_INFRACTOR no representa firma institucional.
 * No participa en FalDocumentoFirma ni FalDocumentoFirmaReq.
 * La comprobacion de firma debe hacerse por identidad semantica:
 *   tipo == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR
 * No usar ordinal(), valor maximo ni rango >= 48.
 */
public enum TipoEvidenciaActa {

    FOTO((short) 4),
    VIDEO((short) 8),
    AUDIO((short) 12),
    PDF((short) 16),
    DOCUMENTO_OFIMATICO((short) 20),
    PLANILLA_CALCULO((short) 24),
    FIRMA_OLOGRAFA_INFRACTOR((short) 48);

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

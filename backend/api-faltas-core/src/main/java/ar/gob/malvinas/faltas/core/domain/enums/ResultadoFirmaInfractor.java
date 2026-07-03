package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de resultados de firma del infractor en el acta.
 *
 * Refleja el catalogo resultado_firma_infractor (SMALLINT) de fal_acta en MariaDB.
 * 5 valores definitivos cerrados en 8C-0D.
 *
 * Aplica a fal_acta.resultado_firma_infractor.
 * Implementado en FalActa en 8C-6A (2026-07-01).
 * La evidencia FIRMA_OLOGRAFA_INFRACTOR esta implementada en FalActaEvidencia/TipoEvidenciaActa.
 */
public enum ResultadoFirmaInfractor {

    FIRMADA((short) 1),
    SE_NIEGA_A_FIRMAR((short) 2),
    INFRACTOR_NO_PRESENTE((short) 3),
    IMPOSIBILITADO_PARA_FIRMAR((short) 4),
    NO_CAPTURADA_POR_FALLA_TECNICA((short) 5);

    private final short codigo;

    ResultadoFirmaInfractor(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static ResultadoFirmaInfractor desdeCodigo(short codigo) {
        for (ResultadoFirmaInfractor r : values()) {
            if (r.codigo == codigo) return r;
        }
        throw new IllegalArgumentException("ResultadoFirmaInfractor desconocido: " + codigo);
    }
}

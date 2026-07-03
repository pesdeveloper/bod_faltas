package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de documento.
 *
 * Refleja el catalogo tipo_docu (SMALLINT) de MariaDB.
 * 12 valores definitivos cerrados en 8C-0D.
 *
 * Los fallos (absolutorio/condenatorio) son ACTO_ADMINISTRATIVO.
 * La distincion absolutorio/condenatorio vive en FalActaFallo.tipoFallo.
 */
public enum TipoDocu {

    ACTA_INFRACCION((short) 1),
    NOTIFICACION_ACTA((short) 2),
    MEDIDA_PREVENTIVA((short) 3),
    ACTO_ADMINISTRATIVO((short) 4),
    NOTIFICACION_ACTO_ADMINISTRATIVO((short) 5),
    CONSTANCIA((short) 6),
    ANEXO((short) 7),
    NULIDAD((short) 8),
    RESOLUTORIO_BLOQUEANTE((short) 9),
    INTIMACION_PAGO((short) 10),
    INTIMACION_INCUMPLIMIENTO_PLAN((short) 11),
    OTRO((short) 12);

    private final short codigo;

    TipoDocu(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoDocu desdeCodigo(short codigo) {
        for (TipoDocu t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("TipoDocu desconocido: " + codigo);
    }
}

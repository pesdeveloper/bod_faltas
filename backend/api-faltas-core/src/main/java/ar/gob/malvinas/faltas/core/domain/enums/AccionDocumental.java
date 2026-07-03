package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de acciones documentales.
 *
 * Refleja el catalogo accion_documental (SMALLINT) de fal_documento_plantilla en MariaDB.
 * 11 valores definitivos cerrados en 8C-0D.
 *
 * Se usara en plantillas documentales futuras (slice 8C-2).
 * No se usa todavia en logica funcional hasta que existan plantillas.
 */
public enum AccionDocumental {

    GENERAR_ACTA_INFRACCION((short) 1),
    EMITIR_NOTIFICACION_ACTA((short) 2),
    EMITIR_MEDIDA_PREVENTIVA((short) 3),
    EMITIR_FALLO((short) 4),
    EMITIR_NOTIFICACION_FALLO((short) 5),
    EMITIR_NULIDAD((short) 6),
    EMITIR_CONSTANCIA((short) 7),
    EMITIR_ANEXO((short) 8),
    EMITIR_INTIMACION_PAGO((short) 9),
    EMITIR_INTIMACION_INCUMPLIMIENTO_PLAN((short) 10),
    EMITIR_RESOLUTORIO_BLOQUEANTE((short) 11);

    private final short codigo;

    AccionDocumental(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static AccionDocumental desdeCodigo(short codigo) {
        for (AccionDocumental a : values()) {
            if (a.codigo == codigo) return a;
        }
        throw new IllegalArgumentException("AccionDocumental desconocido: " + codigo);
    }
}

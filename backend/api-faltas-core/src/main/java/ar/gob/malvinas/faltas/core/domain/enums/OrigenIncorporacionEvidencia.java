package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de la operacion que incorporo una fotografia a {@code fal_acta_evidencia}
 * (columna {@code origen_incorporacion}, exclusiva de {@code tipo_evid = FOTO}).
 *
 * <p>Es trazabilidad operativa (Wave 4 Parte 3D S15), no clasificacion de la
 * fotografia en si: todas las imagenes siguen siendo "fotografias generales"
 * (Parte 3D S5.1). Codigos en multiplos de 4, mismo estilo que
 * {@link TipoEvidenciaActa} (DECISION_DDL-EVID-03).
 *
 * <p>ALTA_INICIAL: la fotografia se incorporo dentro de la misma transaccion
 * del alta del acta ({@code ActaService.labrar}, comando con fotografias).
 *
 * <p>CARGA_POSTERIOR: la fotografia se incorporo en una operacion posterior e
 * independiente sobre un acta ya existente (agregar fotografias a cualquier
 * etapa del ciclo de vida).
 */
public enum OrigenIncorporacionEvidencia {

    ALTA_INICIAL((short) 4),
    CARGA_POSTERIOR((short) 8);

    private final short codigo;

    OrigenIncorporacionEvidencia(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenIncorporacionEvidencia desdeCodigo(short codigo) {
        for (OrigenIncorporacionEvidencia o : values()) {
            if (o.codigo == codigo) return o;
        }
        throw new IllegalArgumentException("OrigenIncorporacionEvidencia desconocido: " + codigo);
    }
}

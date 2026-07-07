package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipos de entidad que pueden recibir observaciones en fal_observacion.
 * Codigos enteros unicos, persistibles como SMALLINT en MariaDB.
 * Sin ordinal: usar codigo() y fromCodigo().
 */
public enum EntidadTipoObservada {

    ACTA(1),
    PERSONA(2),
    DOMICILIO(3),
    DOCUMENTO(4),
    EVIDENCIA(5),
    NOTIFICACION(6),
    NOTIFICACION_INTENTO(7),
    FALLO(8),
    APELACION(9),
    GESTION_EXTERNA(10),
    PARALIZACION(11),
    ARCHIVO(12),
    MEDIDA_PREVENTIVA(13),
    BLOQUEANTE_CIERRE_MATERIAL(14),
    ARTICULO_INFRINGIDO(15),
    VALORIZACION(16),
    OBLIGACION_PAGO(17),
    FORMA_PAGO(18),
    PLAN_PAGO(19),
    MOVIMIENTO_PAGO(20),
    TALONARIO(21),
    MOVIMIENTO_TALONARIO(22);

    private final int codigo;

    EntidadTipoObservada(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static EntidadTipoObservada fromCodigo(int codigo) {
        for (EntidadTipoObservada e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EntidadTipoObservada no reconocida para codigo: " + codigo);
    }
}

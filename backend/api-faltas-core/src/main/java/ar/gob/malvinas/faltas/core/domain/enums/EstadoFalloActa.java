package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado canonico del fallo en el circuito de firma y notificacion.
 *
 * PENDIENTE_FIRMA        -> fallo dictado; documento generado; pendiente de firma obligatoria.
 * PENDIENTE_NOTIFICACION -> ultima firma completada; fhFirma registrado; cola notificatoria preparada.
 * NOTIFICADO             -> resultado notificatorio positivo registrado; fhNotificacion registrado.
 * FIRME                  -> firmeza de condena declarada; fhFirmeza registrado.
 * REEMPLAZADO            -> fallo reemplazado por uno nuevo (estado lateral).
 * SIN_EFECTO             -> fallo anulado sin efecto (estado lateral).
 *
 * DICTADO y FIRMADO no son estados: son hechos persistidos en fhDictado y fhFirma.
 *
 * Codigo numerico persistible (DECISION_DDL-ENUM-01): SMALLINT, prohibido ordinal().
 */
public enum EstadoFalloActa {
    PENDIENTE_FIRMA((short) 1),
    PENDIENTE_NOTIFICACION((short) 2),
    NOTIFICADO((short) 3),
    FIRME((short) 4),
    REEMPLAZADO((short) 5),
    SIN_EFECTO((short) 6);

    private final short codigo;

    EstadoFalloActa(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static EstadoFalloActa desdeCodigo(short codigo) {
        for (EstadoFalloActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoFalloActa sin codigo: " + codigo);
    }
}

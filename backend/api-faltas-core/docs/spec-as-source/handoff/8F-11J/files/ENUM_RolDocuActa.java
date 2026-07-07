package ar.gob.malvinas.faltas.core.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Catalogo de roles funcionales de un documento dentro del expediente.
 *
 * No confundir con TipoDocu (naturaleza de la pieza documental).
 * rolDocuActa define la funcion operativa concreta de esa pieza
 * dentro del ciclo del expediente.
 *
 * Compatibilidad rol -> tiposDocu permitidos:
 *
 *   ACTA_PRINCIPAL        -> ACTA_INFRACCION
 *   FALLO                 -> ACTO_ADMINISTRATIVO
 *   NOTIFICACION          -> NOTIFICACION_ACTA, NOTIFICACION_ACTO_ADMINISTRATIVO
 *   MEDIDA_PREVENTIVA     -> MEDIDA_PREVENTIVA
 *   RESOLUCION            -> ACTO_ADMINISTRATIVO
 *   NULIDAD               -> NULIDAD
 *   RESOLUTORIO_BLOQUEANTE-> RESOLUTORIO_BLOQUEANTE
 *   INTIMACION_PAGO       -> INTIMACION_PAGO
 *   INTIMACION_PLAN       -> INTIMACION_INCUMPLIMIENTO_PLAN
 *   CONSTANCIA            -> CONSTANCIA
 *   ANEXO                 -> ANEXO, CONSTANCIA, OTRO
 *   OTRO                  -> cualquier TipoDocu (controlado en service)
 *
 * ACTA_PRINCIPAL: exactamente uno por acta formalizada.
 * FALLO / RESOLUCION / NOTIFICACION: como maximo uno principal vigente por acta+rol.
 * ANEXO / CONSTANCIA: no admiten principalidad.
 */
public enum RolDocuActa {

    ACTA_PRINCIPAL((short) 1),
    FALLO((short) 2),
    NOTIFICACION((short) 3),
    MEDIDA_PREVENTIVA((short) 4),
    RESOLUCION((short) 5),
    NULIDAD((short) 6),
    RESOLUTORIO_BLOQUEANTE((short) 7),
    INTIMACION_PAGO((short) 8),
    INTIMACION_PLAN((short) 9),
    CONSTANCIA((short) 10),
    ANEXO((short) 11),
    OTRO((short) 12);

    private final short codigo;

    RolDocuActa(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static RolDocuActa desdeCodigo(short codigo) {
        for (RolDocuActa r : values()) {
            if (r.codigo == codigo) return r;
        }
        throw new IllegalArgumentException("RolDocuActa desconocido: " + codigo);
    }

    /** Roles que admiten marca de principal (siPrincipal=true). */
    public static final Set<RolDocuActa> ROLES_CON_PRINCIPAL = EnumSet.of(
            ACTA_PRINCIPAL, FALLO, NOTIFICACION, MEDIDA_PREVENTIVA,
            RESOLUCION, NULIDAD, RESOLUTORIO_BLOQUEANTE,
            INTIMACION_PAGO, INTIMACION_PLAN
    );

    /** Roles donde puede existir como maximo un documento principal vigente por acta. */
    public static final Set<RolDocuActa> ROLES_UNICIDAD_PRINCIPAL = EnumSet.of(
            ACTA_PRINCIPAL, FALLO, NOTIFICACION, RESOLUCION, NULIDAD
    );

    public boolean admitePrincipal() {
        return ROLES_CON_PRINCIPAL.contains(this);
    }

    public boolean exigeUnicidadPrincipal() {
        return ROLES_UNICIDAD_PRINCIPAL.contains(this);
    }

    /**
     * Tipos documentales permitidos para este rol.
     * La validacion completa vive en ActaDocumentoService.validarCompatibilidad().
     */
    public Set<TipoDocu> tiposPermitidos() {
        return switch (this) {
            case ACTA_PRINCIPAL        -> EnumSet.of(TipoDocu.ACTA_INFRACCION);
            case FALLO                 -> EnumSet.of(TipoDocu.ACTO_ADMINISTRATIVO);
            case NOTIFICACION          -> EnumSet.of(TipoDocu.NOTIFICACION_ACTA, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO);
            case MEDIDA_PREVENTIVA     -> EnumSet.of(TipoDocu.MEDIDA_PREVENTIVA);
            case RESOLUCION            -> EnumSet.of(TipoDocu.ACTO_ADMINISTRATIVO);
            case NULIDAD               -> EnumSet.of(TipoDocu.NULIDAD);
            case RESOLUTORIO_BLOQUEANTE-> EnumSet.of(TipoDocu.RESOLUTORIO_BLOQUEANTE);
            case INTIMACION_PAGO       -> EnumSet.of(TipoDocu.INTIMACION_PAGO);
            case INTIMACION_PLAN       -> EnumSet.of(TipoDocu.INTIMACION_INCUMPLIMIENTO_PLAN);
            case CONSTANCIA            -> EnumSet.of(TipoDocu.CONSTANCIA);
            case ANEXO                 -> EnumSet.of(TipoDocu.ANEXO, TipoDocu.CONSTANCIA, TipoDocu.OTRO);
            case OTRO                  -> EnumSet.allOf(TipoDocu.class);
        };
    }
}
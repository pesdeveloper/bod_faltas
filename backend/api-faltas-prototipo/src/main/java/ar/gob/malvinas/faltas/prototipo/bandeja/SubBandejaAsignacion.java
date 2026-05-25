package ar.gob.malvinas.faltas.prototipo.bandeja;

import java.util.List;

/**
 * Resultado de clasificación operativa de una acta dentro de su bandeja principal.
 */
public record SubBandejaAsignacion(
        String subBandeja,
        String subBandejaLabel,
        String chip,
        String accionPrincipal,
        int prioridadSubBandeja,
        List<String> chipsSecundarios) {

    public static SubBandejaAsignacion de(SubBandejaCodigo codigo) {
        return de(codigo, List.of());
    }

    public static SubBandejaAsignacion de(SubBandejaCodigo codigo, List<String> chipsSecundarios) {
        return new SubBandejaAsignacion(
                codigo.codigo(),
                codigo.label(),
                codigo.chip(),
                codigo.accionPrincipal(),
                codigo.prioridad(),
                chipsSecundarios == null ? List.of() : List.copyOf(chipsSecundarios));
    }
}

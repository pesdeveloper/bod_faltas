package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;

import java.time.LocalDateTime;

public record TalonarioInspectorResponse(
        Long id,
        Long idTalonario,
        Long idInsp,
        short verInsp,
        LocalDateTime fhEntrega,
        String idUserEntrega,
        LocalDateTime fhDevolucion,
        String idUserDevolucion,
        EstadoAsignacionTalonario estadoAsignacion,
        boolean siActiva,
        Long talonarioIdActivo) {

    public static TalonarioInspectorResponse de(NumTalonarioInspector a) {
        return new TalonarioInspectorResponse(
                a.getId(), a.getIdTalonario(), a.getIdInsp(), a.getVerInsp(),
                a.getFhEntrega(), a.getIdUserEntrega(),
                a.getFhDevolucion(), a.getIdUserDevolucion(),
                a.getEstadoAsignacion(), a.isSiActiva(), a.getTalonarioIdActivo());
    }
}

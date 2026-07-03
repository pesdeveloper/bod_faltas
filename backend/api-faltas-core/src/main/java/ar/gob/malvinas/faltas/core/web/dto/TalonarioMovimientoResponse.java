package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;

import java.time.LocalDateTime;

public record TalonarioMovimientoResponse(
        Long id,
        Long idTalonario,
        int nroTalonario,
        EstadoNumeroTalonario estadoNumero,
        MotivoAnulacionTalonario motivoAnulacion,
        String observacion,
        Long actaId,
        Long documentoId,
        Long idDep,
        Short verDep,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {
    public static TalonarioMovimientoResponse de(NumTalonarioMovimiento m) {
        return new TalonarioMovimientoResponse(
                m.getId(), m.getIdTalonario(), m.getNroTalonario(),
                m.getEstadoNumero(), m.getMotivoAnulacion(), m.getObservacion(),
                m.getActaId(), m.getDocumentoId(),
                m.getIdDep(), m.getVerDep(), m.getIdInsp(), m.getVerInsp(),
                m.getFhMovimiento(), m.getIdUserMovimiento());
    }
}


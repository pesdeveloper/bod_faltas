package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;

import java.time.LocalDateTime;

/**
 * Movimiento individual de un numero de talonario.
 * Registra cada numero emitido, anulado, devuelto o justificado dentro de un talonario.
 * Alineado con num_talonario_movimiento del modelo MariaDB productivo.
 * Restriccion productiva: UNIQUE(id_talonario, nro_talonario). No se borran movimientos.
 * Slice 8B-4: implementacion in-memory, solo flujo USADO.
 * Slice 8B-6: flujo ANULADO, DEVUELTO_SIN_USAR, RENDIDO, JUSTIFICADO.
 * Slice 9: persiste en num_talonario_movimiento via JDBC.
 */
public class NumTalonarioMovimiento {

    private final Long id;
    private final Long idTalonario;
    private final int nroTalonario;
    private final EstadoNumeroTalonario estadoNumero;
    private final MotivoAnulacionTalonario motivoAnulacion;
    private final String observacion;
    private final Long actaId;
    private final Long documentoId;
    private final Long idDep;
    private final Short verDep;
    private final Long idInsp;
    private final Short verInsp;
    private final LocalDateTime fhMovimiento;
    private final String idUserMovimiento;

    public NumTalonarioMovimiento(
            Long id, Long idTalonario, int nroTalonario,
            EstadoNumeroTalonario estadoNumero, MotivoAnulacionTalonario motivoAnulacion,
            String observacion, Long actaId, Long documentoId,
            Long idDep, Short verDep, Long idInsp, Short verInsp,
            LocalDateTime fhMovimiento, String idUserMovimiento) {
        this.id = id;
        this.idTalonario = idTalonario;
        this.nroTalonario = nroTalonario;
        this.estadoNumero = estadoNumero;
        this.motivoAnulacion = motivoAnulacion;
        this.observacion = observacion;
        this.actaId = actaId;
        this.documentoId = documentoId;
        this.idDep = idDep;
        this.verDep = verDep;
        this.idInsp = idInsp;
        this.verInsp = verInsp;
        this.fhMovimiento = fhMovimiento;
        this.idUserMovimiento = idUserMovimiento;
    }

    public Long getId() { return id; }
    public Long getIdTalonario() { return idTalonario; }
    public int getNroTalonario() { return nroTalonario; }
    public EstadoNumeroTalonario getEstadoNumero() { return estadoNumero; }
    public MotivoAnulacionTalonario getMotivoAnulacion() { return motivoAnulacion; }
    public String getObservacion() { return observacion; }
    public Long getActaId() { return actaId; }
    public Long getDocumentoId() { return documentoId; }
    public Long getIdDep() { return idDep; }
    public Short getVerDep() { return verDep; }
    public Long getIdInsp() { return idInsp; }
    public Short getVerInsp() { return verInsp; }
    public LocalDateTime getFhMovimiento() { return fhMovimiento; }
    public String getIdUserMovimiento() { return idUserMovimiento; }
}


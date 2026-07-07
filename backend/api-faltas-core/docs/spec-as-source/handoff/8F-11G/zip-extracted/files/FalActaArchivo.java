package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;

import java.time.LocalDateTime;

/**
 * Ciclo de archivo de un acta (fal_acta_archivo).
 *
 * Guarda snapshot del origen: bloque, situacion, bandeja, sub-bandeja, accion pendiente.
 * Los flags siPermiteReingresoSnapshot y siNulidadSnapshot se copian del motivo al crear el ciclo.
 * Nunca se reinterpretan consultando el motivo actual.
 *
 * Un acta puede tener N ciclos historicos pero solo uno activo a la vez (siActivo=true).
 */
public class FalActaArchivo {

    private Long id;
    private int versionRow;
    private final Long actaId;
    private final Long idMotivoArchivo;
    private final boolean siPermiteReingresoSnapshot;
    private Long documentoId;
    private final boolean siNulidadSnapshot;
    private final EstadoProcesalActa estProcActOrigen;
    private final SituacionAdministrativaActa sitAdmActOrigen;
    private final BloqueActual bloqueOrigen;
    private String codBandejaOrigen;
    private String subBandejaOrigen;
    private String accionPendienteOrigen;
    private Long observacionId;
    private String eventoArchivoId;
    private final LocalDateTime fhArchivo;
    private final String idUserArchivo;
    private LocalDateTime fhReingreso;
    private String idUserReingreso;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalActaArchivo(
            Long id,
            Long actaId,
            Long idMotivoArchivo,
            boolean siPermiteReingresoSnapshot,
            boolean siNulidadSnapshot,
            EstadoProcesalActa estProcActOrigen,
            SituacionAdministrativaActa sitAdmActOrigen,
            BloqueActual bloqueOrigen,
            LocalDateTime fhArchivo,
            String idUserArchivo,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (actaId == null) throw new IllegalArgumentException("actaId obligatorio");
        if (idMotivoArchivo == null) throw new IllegalArgumentException("idMotivoArchivo obligatorio");
        if (fhArchivo == null) throw new IllegalArgumentException("fhArchivo obligatorio");
        this.id = id;
        this.actaId = actaId;
        this.idMotivoArchivo = idMotivoArchivo;
        this.siPermiteReingresoSnapshot = siPermiteReingresoSnapshot;
        this.siNulidadSnapshot = siNulidadSnapshot;
        this.estProcActOrigen = estProcActOrigen;
        this.sitAdmActOrigen = sitAdmActOrigen;
        this.bloqueOrigen = bloqueOrigen;
        this.fhArchivo = fhArchivo;
        this.idUserArchivo = idUserArchivo;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.siActivo = true;
        this.versionRow = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }
    public Long getIdMotivoArchivo() { return idMotivoArchivo; }
    public boolean isSiPermiteReingresoSnapshot() { return siPermiteReingresoSnapshot; }
    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long documentoId) { this.documentoId = documentoId; }
    public boolean isSiNulidadSnapshot() { return siNulidadSnapshot; }
    public EstadoProcesalActa getEstProcActOrigen() { return estProcActOrigen; }
    public SituacionAdministrativaActa getSitAdmActOrigen() { return sitAdmActOrigen; }
    public BloqueActual getBloqueOrigen() { return bloqueOrigen; }
    public String getCodBandejaOrigen() { return codBandejaOrigen; }
    public void setCodBandejaOrigen(String codBandejaOrigen) { this.codBandejaOrigen = codBandejaOrigen; }
    public String getSubBandejaOrigen() { return subBandejaOrigen; }
    public void setSubBandejaOrigen(String subBandejaOrigen) { this.subBandejaOrigen = subBandejaOrigen; }
    public String getAccionPendienteOrigen() { return accionPendienteOrigen; }
    public void setAccionPendienteOrigen(String accionPendienteOrigen) { this.accionPendienteOrigen = accionPendienteOrigen; }
    public Long getObservacionId() { return observacionId; }
    public void setObservacionId(Long observacionId) { this.observacionId = observacionId; }
    public String getEventoArchivoId() { return eventoArchivoId; }
    public void setEventoArchivoId(String eventoArchivoId) { this.eventoArchivoId = eventoArchivoId; }
    public LocalDateTime getFhArchivo() { return fhArchivo; }
    public String getIdUserArchivo() { return idUserArchivo; }
    public LocalDateTime getFhReingreso() { return fhReingreso; }
    public void setFhReingreso(LocalDateTime fhReingreso) { this.fhReingreso = fhReingreso; }
    public String getIdUserReingreso() { return idUserReingreso; }
    public void setIdUserReingreso(String idUserReingreso) { this.idUserReingreso = idUserReingreso; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public FalActaArchivo copia() {
        FalActaArchivo c = new FalActaArchivo(id, actaId, idMotivoArchivo,
                siPermiteReingresoSnapshot, siNulidadSnapshot,
                estProcActOrigen, sitAdmActOrigen, bloqueOrigen,
                fhArchivo, idUserArchivo, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.documentoId = this.documentoId;
        c.codBandejaOrigen = this.codBandejaOrigen;
        c.subBandejaOrigen = this.subBandejaOrigen;
        c.accionPendienteOrigen = this.accionPendienteOrigen;
        c.observacionId = this.observacionId;
        c.eventoArchivoId = this.eventoArchivoId;
        c.fhReingreso = this.fhReingreso;
        c.idUserReingreso = this.idUserReingreso;
        c.siActivo = this.siActivo;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}

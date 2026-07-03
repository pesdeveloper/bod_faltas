package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FalActa {

    private final Long id;
    private final String uuidTecnico;
    private String nroActa;
    private Long idTalonario;
    private Integer nroTalonarioUsado;
    private final String tipoActa;
    private final String idDependencia;
    private final String idInspector;
    private final LocalDate fechaActa;
    private final LocalDateTime fechaLabrado;
    private final String domicilioHecho;
    private final String domicilioInfractor;
    private final String observaciones;
    private final Double latInfr;
    private final Double lonInfr;
    private final String infractorNombre;
    private final String infractorDocumento;
    private final ResultadoFirmaInfractor resultadoFirmaInfractor;

    private BloqueActual bloqueActual;
    private EstadoProcesalActa estadoProcesal;
    private SituacionAdministrativaActa situacionAdministrativa;
    private ResultadoFinalActa resultadoFinal;

    public FalActa(
            Long id,
            String uuidTecnico,
            String tipoActa,
            String idDependencia,
            String idInspector,
            LocalDate fechaActa,
            LocalDateTime fechaLabrado,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            String infractorDocumento,
            ResultadoFirmaInfractor resultadoFirmaInfractor) {
        if (resultadoFirmaInfractor == null) {
            throw new IllegalArgumentException("resultadoFirmaInfractor es obligatorio en el acta");
        }
        this.id = id;
        this.uuidTecnico = uuidTecnico;
        this.tipoActa = tipoActa;
        this.idDependencia = idDependencia;
        this.idInspector = idInspector;
        this.fechaActa = fechaActa;
        this.fechaLabrado = fechaLabrado;
        this.domicilioHecho = domicilioHecho;
        this.domicilioInfractor = domicilioInfractor;
        this.observaciones = observaciones;
        this.latInfr = latInfr;
        this.lonInfr = lonInfr;
        this.infractorNombre = infractorNombre;
        this.infractorDocumento = infractorDocumento;
        this.resultadoFirmaInfractor = resultadoFirmaInfractor;
        this.bloqueActual = BloqueActual.CAPT;
        this.estadoProcesal = EstadoProcesalActa.EN_TRAMITE;
        this.situacionAdministrativa = SituacionAdministrativaActa.ACTIVA;
        this.resultadoFinal = ResultadoFinalActa.SIN_RESULTADO_FINAL;
    }

    public Long getId() { return id; }
    public String getUuidTecnico() { return uuidTecnico; }

    public String getNroActa() { return nroActa; }
    public void setNroActa(String nroActa) { this.nroActa = nroActa; }

    public Long getIdTalonario() { return idTalonario; }
    public void setIdTalonario(Long idTalonario) { this.idTalonario = idTalonario; }

    public Integer getNroTalonarioUsado() { return nroTalonarioUsado; }
    public void setNroTalonarioUsado(Integer nroTalonarioUsado) { this.nroTalonarioUsado = nroTalonarioUsado; }

    public String getTipoActa() { return tipoActa; }
    public String getIdDependencia() { return idDependencia; }
    public String getIdInspector() { return idInspector; }
    public LocalDate getFechaActa() { return fechaActa; }
    public LocalDateTime getFechaLabrado() { return fechaLabrado; }
    public String getDomicilioHecho() { return domicilioHecho; }
    public String getDomicilioInfractor() { return domicilioInfractor; }
    public String getObservaciones() { return observaciones; }
    public Double getLatInfr() { return latInfr; }
    public Double getLonInfr() { return lonInfr; }
    public String getInfractorNombre() { return infractorNombre; }
    public String getInfractorDocumento() { return infractorDocumento; }
    public ResultadoFirmaInfractor getResultadoFirmaInfractor() { return resultadoFirmaInfractor; }

    public BloqueActual getBloqueActual() { return bloqueActual; }
    public void setBloqueActual(BloqueActual bloqueActual) { this.bloqueActual = bloqueActual; }

    public EstadoProcesalActa getEstadoProcesal() { return estadoProcesal; }
    public void setEstadoProcesal(EstadoProcesalActa estadoProcesal) { this.estadoProcesal = estadoProcesal; }

    public SituacionAdministrativaActa getSituacionAdministrativa() { return situacionAdministrativa; }
    public void setSituacionAdministrativa(SituacionAdministrativaActa situacionAdministrativa) {
        this.situacionAdministrativa = situacionAdministrativa;
    }

    public ResultadoFinalActa getResultadoFinal() { return resultadoFinal; }
    public void setResultadoFinal(ResultadoFinalActa resultadoFinal) { this.resultadoFinal = resultadoFinal; }

    public boolean estaCerrada() {
        return situacionAdministrativa == SituacionAdministrativaActa.CERRADA
                || situacionAdministrativa == SituacionAdministrativaActa.ANULADA;
    }

    public boolean estaParalizada() {
        return situacionAdministrativa == SituacionAdministrativaActa.PARALIZADA;
    }
}

package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expediente de acta de faltas.
 *
 * FK de persona y domicilios (desde 8F-11C):
 *   idPersonaInfractor  -> fal_persona.id  (nullable durante transicion)
 *   idDomicilioInfractorAct -> fal_persona_domicilio.id (nullable)
 *   idDomicilioNotifAct     -> fal_persona_domicilio.id (nullable)
 *
 * Los datos de nombre, documento y domicilio del infractor NO se almacenan
 * en esta entidad: se obtienen de FalPersona y FalPersonaDomicilio via las FK.
 * El snapshot y los DTOs los proyectan como vista derivada.
 *
 * domicilioHecho es el LUGAR DE LA INFRACCION, no el domicilio del infractor.
 */
public class FalActa {

    private final Long id;
    private final String uuidTecnico;
    private String nroActa;
    private Long idTalonario;
    private Integer nroTalonarioUsado;
    private final TipoActa tipoActa;
    private final Long idDependencia;
    private final Long idInspector;
    private final LocalDate fechaActa;
    private final LocalDateTime fechaLabrado;
    private final String domicilioHecho;
    private final String observaciones;
    private final Double latInfr;
    private final Double lonInfr;
    private final ResultadoFirmaInfractor resultadoFirmaInfractor;

    private Long idPersonaInfractor;
    private Long idDomicilioInfractorAct;
    private Long idDomicilioNotifAct;

    private BloqueActual bloqueActual;
    private EstadoProcesalActa estadoProcesal;
    private SituacionAdministrativaActa situacionAdministrativa;
    private ResultadoFinalActa resultadoFinal;    private String codigoQr;

    private int versionRow;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalActa(
            Long id,
            String uuidTecnico,
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            LocalDateTime fechaLabrado,
            String domicilioHecho,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            Long idPersonaInfractor,
            LocalDateTime fhAlta,
            String idUserAlta) {
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
        this.observaciones = observaciones;
        this.latInfr = latInfr;
        this.lonInfr = lonInfr;
        this.resultadoFirmaInfractor = resultadoFirmaInfractor;
        this.idPersonaInfractor = idPersonaInfractor;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.bloqueActual = BloqueActual.CAPT;
        this.estadoProcesal = EstadoProcesalActa.EN_TRAMITE;
        this.situacionAdministrativa = SituacionAdministrativaActa.ACTIVA;
        this.resultadoFinal = ResultadoFinalActa.SIN_RESULTADO_FINAL;
        this.versionRow = 0;
    }

    public Long getId() { return id; }
    public String getUuidTecnico() { return uuidTecnico; }

    public String getNroActa() { return nroActa; }
    public void setNroActa(String nroActa) { this.nroActa = nroActa; }

    public Long getIdTalonario() { return idTalonario; }
    public void setIdTalonario(Long idTalonario) { this.idTalonario = idTalonario; }

    public Integer getNroTalonarioUsado() { return nroTalonarioUsado; }
    public void setNroTalonarioUsado(Integer nroTalonarioUsado) { this.nroTalonarioUsado = nroTalonarioUsado; }

    public TipoActa getTipoActa() { return tipoActa; }
    public Long getIdDependencia() { return idDependencia; }
    public Long getIdInspector() { return idInspector; }
    public LocalDate getFechaActa() { return fechaActa; }
    public LocalDateTime getFechaLabrado() { return fechaLabrado; }
    public String getDomicilioHecho() { return domicilioHecho; }
    public String getObservaciones() { return observaciones; }
    public Double getLatInfr() { return latInfr; }
    public Double getLonInfr() { return lonInfr; }
    public ResultadoFirmaInfractor getResultadoFirmaInfractor() { return resultadoFirmaInfractor; }

    public Long getIdPersonaInfractor() { return idPersonaInfractor; }
    public void setIdPersonaInfractor(Long idPersonaInfractor) { this.idPersonaInfractor = idPersonaInfractor; }

    public Long getIdDomicilioInfractorAct() { return idDomicilioInfractorAct; }
    public void setIdDomicilioInfractorAct(Long idDomicilioInfractorAct) {
        this.idDomicilioInfractorAct = idDomicilioInfractorAct;
    }

    public Long getIdDomicilioNotifAct() { return idDomicilioNotifAct; }
    public void setIdDomicilioNotifAct(Long idDomicilioNotifAct) {
        this.idDomicilioNotifAct = idDomicilioNotifAct;
    }

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

    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public boolean estaCerrada() {
        return situacionAdministrativa == SituacionAdministrativaActa.CERRADA
                || situacionAdministrativa == SituacionAdministrativaActa.ANULADA;
    }

    public boolean estaParalizada() {
        return situacionAdministrativa == SituacionAdministrativaActa.PARALIZADA;
    }

    public FalActa copia() {
        FalActa c = new FalActa(id, uuidTecnico, tipoActa, idDependencia, idInspector,
                fechaActa, fechaLabrado, domicilioHecho, observaciones,
                latInfr, lonInfr, resultadoFirmaInfractor, idPersonaInfractor,
                fhAlta, idUserAlta);
        c.nroActa = this.nroActa;
        c.idTalonario = this.idTalonario;
        c.nroTalonarioUsado = this.nroTalonarioUsado;
        c.idDomicilioInfractorAct = this.idDomicilioInfractorAct;
        c.idDomicilioNotifAct = this.idDomicilioNotifAct;
        c.bloqueActual = this.bloqueActual;
        c.estadoProcesal = this.estadoProcesal;
        c.situacionAdministrativa = this.situacionAdministrativa;
        c.resultadoFinal = this.resultadoFinal;
        c.versionRow = this.versionRow;
        c.codigoQr = this.codigoQr;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}


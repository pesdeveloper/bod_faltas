package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ModoReingresoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de gestion externa asociada a un expediente de faltas.
 *
 * Slice 6A:   derivacion (EXTDER).
 * Slice 6B:   reingreso (EXTRET) con modos REINGRESO_PARA_REVISION y REINGRESO_SIN_PAGO.
 * Slice 6C:   pago externo (PAGAPR). Cierra la gestion con estadoGestionExterna=CERRADA_EXTERNA.
 * Slice 6D-1: habilita pares SIN_PAGO+REINGRESO_SIN_PAGO y SIN_CAMBIOS+REINGRESO_PARA_REVISION.
 * Slice 6D-2: habilita ABSUELVE+REINGRESO_PARA_NUEVO_FALLO, CONFIRMA_CONDENA+REINGRESO_CON_DICTAMEN,
 *             MODIFICA_MONTO+REINGRESO_CON_DICTAMEN. Agrega campo montoResultado.
 *
 * Separacion de campos por camino de cierre:
 *   - Cierre por EXTRET (Slice 6B+): motivoReingreso, observacionesReingreso, fechaReingreso.
 *   - Cierre por PAGAPR (Slice 6C): fechaCierreGestionExterna.
 *     Las observaciones textuales de PAGAPR viajan en FalActaEvento.descripcion
 *     como puente transitorio hasta implementar FalObservacion en Slice 9/JDBC.
 *     El modelo MariaDB base ya define fal_observacion con entidad_tipo=10=GESTION_EXTERNA.
 *
 * Campo montoResultado (Slice 6D-2):
 *   Corresponde a monto_resultado DECIMAL(14,2) NULL en fal_acta_gestion_externa (MariaDB).
 *   Obligatorio y mayor a cero solo cuando resultado == MODIFICA_MONTO.
 *   NULL para todos los demas caminos de reingreso.
 */
public class FalGestionExterna {

    private final Long id;
    private final Long actaId;

    private TipoGestionExterna tipoGestionExterna;
    private EstadoGestionExterna estadoGestionExterna;
    private ResultadoGestionExterna resultadoGestionExterna;
    private ModoReingresoGestionExterna modoReingresoGestionExterna;

    private String motivoDerivacion;
    private String observacionesDerivacion;
    private LocalDateTime fechaDerivacion;

    private BloqueActual bloqueOrigen;
    private SituacionAdministrativaActa situacionAdministrativaOrigen;
    private CodigoBandeja codigoBandejaOrigen;
    private AccionPendiente accionPendienteOrigen;

    private String motivoReingreso;
    private String observacionesReingreso;
    private LocalDateTime fechaReingreso;

    private LocalDateTime fechaCierreGestionExterna;

    private BigDecimal montoResultado;

    private boolean siActiva;
    private LocalDateTime fhAlta;
    private String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalGestionExterna(Long id, Long actaId, LocalDateTime fhAlta, String idUserAlta) {
        this.id = id;
        this.actaId = actaId;
        this.estadoGestionExterna = EstadoGestionExterna.DERIVADA;
        this.resultadoGestionExterna = ResultadoGestionExterna.SIN_RESULTADO;
        this.modoReingresoGestionExterna = null;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime v) { this.fhUltMod = v; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String v) { this.idUserUltMod = v; }

    public TipoGestionExterna getTipoGestionExterna() { return tipoGestionExterna; }
    public void setTipoGestionExterna(TipoGestionExterna v) { this.tipoGestionExterna = v; }

    public EstadoGestionExterna getEstadoGestionExterna() { return estadoGestionExterna; }
    public void setEstadoGestionExterna(EstadoGestionExterna v) { this.estadoGestionExterna = v; }

    public ResultadoGestionExterna getResultadoGestionExterna() { return resultadoGestionExterna; }
    public void setResultadoGestionExterna(ResultadoGestionExterna v) { this.resultadoGestionExterna = v; }

    public ModoReingresoGestionExterna getModoReingresoGestionExterna() { return modoReingresoGestionExterna; }
    public void setModoReingresoGestionExterna(ModoReingresoGestionExterna v) { this.modoReingresoGestionExterna = v; }

    public String getMotivoDerivacion() { return motivoDerivacion; }
    public void setMotivoDerivacion(String v) { this.motivoDerivacion = v; }

    public String getObservacionesDerivacion() { return observacionesDerivacion; }
    public void setObservacionesDerivacion(String v) { this.observacionesDerivacion = v; }

    public LocalDateTime getFechaDerivacion() { return fechaDerivacion; }
    public void setFechaDerivacion(LocalDateTime v) { this.fechaDerivacion = v; }

    public BloqueActual getBloqueOrigen() { return bloqueOrigen; }
    public void setBloqueOrigen(BloqueActual v) { this.bloqueOrigen = v; }

    public SituacionAdministrativaActa getSituacionAdministrativaOrigen() { return situacionAdministrativaOrigen; }
    public void setSituacionAdministrativaOrigen(SituacionAdministrativaActa v) { this.situacionAdministrativaOrigen = v; }

    public CodigoBandeja getCodigoBandejaOrigen() { return codigoBandejaOrigen; }
    public void setCodigoBandejaOrigen(CodigoBandeja v) { this.codigoBandejaOrigen = v; }

    public AccionPendiente getAccionPendienteOrigen() { return accionPendienteOrigen; }
    public void setAccionPendienteOrigen(AccionPendiente v) { this.accionPendienteOrigen = v; }

    public String getMotivoReingreso() { return motivoReingreso; }
    public void setMotivoReingreso(String v) { this.motivoReingreso = v; }

    public String getObservacionesReingreso() { return observacionesReingreso; }
    public void setObservacionesReingreso(String v) { this.observacionesReingreso = v; }

    public LocalDateTime getFechaReingreso() { return fechaReingreso; }
    public void setFechaReingreso(LocalDateTime v) { this.fechaReingreso = v; }

    public LocalDateTime getFechaCierreGestionExterna() { return fechaCierreGestionExterna; }
    public void setFechaCierreGestionExterna(LocalDateTime v) { this.fechaCierreGestionExterna = v; }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean v) { this.siActiva = v; }

    public BigDecimal getMontoResultado() { return montoResultado; }
    public void setMontoResultado(BigDecimal v) { this.montoResultado = v; }

    public boolean estaDerivada() {
        return estadoGestionExterna == EstadoGestionExterna.DERIVADA;
    }

    public boolean estaActivaParaReingreso() {
        return siActiva
                && (estadoGestionExterna == EstadoGestionExterna.DERIVADA
                    || estadoGestionExterna == EstadoGestionExterna.EN_CURSO);
    }
}

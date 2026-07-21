package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CanalApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPresentacion;

import java.time.LocalDateTime;

/**
 * Tramite de apelacion sobre un fallo del acta (fal_acta_apelacion).
 * Los documentos presentados viven en FalActaApelacionDocumento.
 */
public class FalActaApelacion {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private final Long falloId;
    private CanalApelacion canalApelacion;
    private TipoPresentacion tipoPresentacion;
    private String textoApelacion;
    private final LocalDateTime fhRegistro;
    private String idUserRegistro;
    private EstadoApelacionActa estadoApelacion;
    private ResultadoResolucionApelacion resultadoResolucion;
    private LocalDateTime fhResolucion;
    private String idUserResolucion;
    private Long documentoResolucionId;
    private String fundamentosResolucion;
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated
    private String observacionesResolucion;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalActaApelacion(
            Long id,
            Long actaId,
            Long falloId,
            CanalApelacion canalApelacion,
            TipoPresentacion tipoPresentacion,
            String textoApelacion,
            LocalDateTime fhRegistro,
            String idUserRegistro,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.actaId = actaId;
        this.falloId = falloId;
        this.canalApelacion = canalApelacion;
        this.tipoPresentacion = tipoPresentacion;
        this.textoApelacion = textoApelacion;
        this.fhRegistro = fhRegistro;
        this.idUserRegistro = idUserRegistro;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.estadoApelacion = EstadoApelacionActa.PRESENTADA;
        this.versionRow = 0;
    }

    /**
     * @deprecated Constructor legacy Slice 3. Usar el constructor canonico con CanalApelacion/TipoPresentacion.
     * Parametros: id, actaId, falloId, estadoApelacion, fechaPresentacion, presentante, fundamentos, observaciones, siActiva, fhAlta, idUserAlta.
     */
    @Deprecated
    @SuppressWarnings("unused")
    public FalActaApelacion(
            Long id,
            Long actaId,
            Long falloId,
            EstadoApelacionActa estadoApelacion,
            LocalDateTime fechaPresentacion,
            String presentante,
            String fundamentos,
            String observaciones,
            boolean siActiva,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this(id, actaId, falloId, null, null, fundamentos, fechaPresentacion, presentante, fhAlta, idUserAlta);
        this.estadoApelacion = estadoApelacion;
    }

    // -----------------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getActaId() { return actaId; }
    public Long getFalloId() { return falloId; }
    public CanalApelacion getCanalApelacion() { return canalApelacion; }
    public void setCanalApelacion(CanalApelacion c) { this.canalApelacion = c; }
    public TipoPresentacion getTipoPresentacion() { return tipoPresentacion; }
    public void setTipoPresentacion(TipoPresentacion t) { this.tipoPresentacion = t; }
    public String getTextoApelacion() { return textoApelacion; }
    public void setTextoApelacion(String t) { this.textoApelacion = t; }
    public LocalDateTime getFhRegistro() { return fhRegistro; }
    public String getIdUserRegistro() { return idUserRegistro; }
    public void setIdUserRegistro(String u) { this.idUserRegistro = u; }
    public EstadoApelacionActa getEstadoApelacion() { return estadoApelacion; }
    public void setEstadoApelacion(EstadoApelacionActa e) { this.estadoApelacion = e; }
    public ResultadoResolucionApelacion getResultadoResolucion() { return resultadoResolucion; }
    public void setResultadoResolucion(ResultadoResolucionApelacion r) { this.resultadoResolucion = r; }
    public LocalDateTime getFhResolucion() { return fhResolucion; }
    public void setFhResolucion(LocalDateTime f) { this.fhResolucion = f; }
    public String getIdUserResolucion() { return idUserResolucion; }
    public void setIdUserResolucion(String u) { this.idUserResolucion = u; }
    public Long getDocumentoResolucionId() { return documentoResolucionId; }
    public void setDocumentoResolucionId(Long d) { this.documentoResolucionId = d; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime f) { this.fhUltMod = f; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String u) { this.idUserUltMod = u; }

    // -----------------------------------------------------------------------
    // Compatibilidad backward (campos anteriores delegados a los canonicos)
    // -----------------------------------------------------------------------

    /** @deprecated Usar getFhRegistro() */
    @Deprecated public LocalDateTime getFechaPresentacion() { return fhRegistro; }
    /** @deprecated Usar getTextoApelacion() */
    @Deprecated public String getFundamentos() { return textoApelacion; }
    /** @deprecated Usar setTextoApelacion() */
    @Deprecated public void setFundamentos(String f) { this.textoApelacion = f; }
    /** @deprecated Usar getIdUserRegistro() */
    @Deprecated public String getPresentante() { return idUserRegistro; }
    /** @deprecated Usar setIdUserRegistro() */
    @Deprecated public void setPresentante(String p) { this.idUserRegistro = p; }
    /** @deprecated campo eliminado del modelo */
    @Deprecated public String getObservaciones() { return null; }
    /** @deprecated campo eliminado del modelo */
    @Deprecated public void setObservaciones(String o) { }
    /** @deprecated Usar getFhResolucion() */
    @Deprecated public LocalDateTime getFechaResolucion() { return fhResolucion; }
    /** @deprecated Usar setFhResolucion() */
    @Deprecated public void setFechaResolucion(LocalDateTime f) { this.fhResolucion = f; }
    public String getFundamentosResolucion() { return fundamentosResolucion; }
    public void setFundamentosResolucion(String f) { this.fundamentosResolucion = f; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated public String getObservacionesResolucion() { return observacionesResolucion; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated public void setObservacionesResolucion(String o) { this.observacionesResolucion = o; }
    /**
     * @deprecated Usar EstadoApelacionActa.RESUELTA + getResultadoResolucion()
     * Esta propiedad se mantiene para compatibilidad con SnapshotRecalculador y tests del Slice 3.
     */
    @Deprecated public boolean isSiActiva() {
        return estadoApelacion == EstadoApelacionActa.PRESENTADA
                || estadoApelacion == EstadoApelacionActa.EN_ANALISIS;
    }
    /** @deprecated campo obsoleto; ignorado */
    @Deprecated public void setSiActiva(boolean v) { }

    // -----------------------------------------------------------------------
    // Operaciones de dominio
    // -----------------------------------------------------------------------

    /** Resolucion atomica: establece RESUELTA, resultado, fecha, usuario y doc opcional. */
    public void resolver(ResultadoResolucionApelacion resultado, LocalDateTime fhResolucion,
                         String idUserResolucion, Long documentoResolucionId) {
        if (resultado == null) throw new IllegalArgumentException("resultado requerido");
        if (fhResolucion == null) throw new IllegalArgumentException("fhResolucion requerida");
        this.estadoApelacion = EstadoApelacionActa.RESUELTA;
        this.resultadoResolucion = resultado;
        this.fhResolucion = fhResolucion;
        this.idUserResolucion = idUserResolucion;
        this.documentoResolucionId = documentoResolucionId;
        this.fhUltMod = fhResolucion;
        this.idUserUltMod = idUserResolucion;
    }

    // -----------------------------------------------------------------------
    // Copia defensiva
    // -----------------------------------------------------------------------

    public FalActaApelacion copia() {
        FalActaApelacion c = new FalActaApelacion(
                id, actaId, falloId, canalApelacion, tipoPresentacion,
                textoApelacion, fhRegistro, idUserRegistro, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.estadoApelacion = this.estadoApelacion;
        c.resultadoResolucion = this.resultadoResolucion;
        c.fhResolucion = this.fhResolucion;
        c.idUserResolucion = this.idUserResolucion;
        c.documentoResolucionId = this.documentoResolucionId;
        c.fundamentosResolucion = this.fundamentosResolucion;
        c.observacionesResolucion = this.observacionesResolucion;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}

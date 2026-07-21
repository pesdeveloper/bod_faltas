package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;

import java.time.LocalDateTime;

/**
 * Talonario concreto de numeracion, electronico o manual fisico.
 *
 * Referencia a SEQUENCE nativa de MariaDB via nombreSecuencia.
 * En in-memory, nombreSecuencia es solo un identificador logico; la emision de
 * numeros reales (NEXT VALUE FOR) se implementa en Slice 8B-4.
 *
 * Alineado con num_talonario del modelo MariaDB productivo.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: persiste en num_talonario via JDBC.
 */
public class NumTalonario {

    private final Long id;
    private int versionRow;
    private final Long politicaId;
    private final String codigo;
    private final String descripcion;
    private final TipoTalonario tipoTalonario;
    private final ClaseNumeracion claseTalonario;
    private final Short anio;
    private final String serie;
    private final int nroDesde;
    private final Integer nroHasta;
    private final String nombreSecuencia;
    private boolean siActivo;
    private boolean siBloqueado;
    private String codDesbloqueo;
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated
    private String obsTalonario;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public NumTalonario(
            Long id,
            int versionRow,
            Long politicaId,
            String codigo,
            String descripcion,
            TipoTalonario tipoTalonario,
            ClaseNumeracion claseTalonario,
            Short anio,
            String serie,
            int nroDesde,
            Integer nroHasta,
            String nombreSecuencia,
            boolean siActivo,
            boolean siBloqueado,
            String codDesbloqueo,
            String obsTalonario,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.versionRow = versionRow;
        this.politicaId = politicaId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tipoTalonario = tipoTalonario;
        this.claseTalonario = claseTalonario;
        this.anio = anio;
        this.serie = serie;
        this.nroDesde = nroDesde;
        this.nroHasta = nroHasta;
        this.nombreSecuencia = nombreSecuencia;
        this.siActivo = siActivo;
        this.siBloqueado = siBloqueado;
        this.codDesbloqueo = codDesbloqueo;
        this.obsTalonario = obsTalonario;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getPoliticaId() { return politicaId; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public TipoTalonario getTipoTalonario() { return tipoTalonario; }
    public ClaseNumeracion getClaseTalonario() { return claseTalonario; }
    public Short getAnio() { return anio; }
    public String getSerie() { return serie; }
    public int getNroDesde() { return nroDesde; }
    public Integer getNroHasta() { return nroHasta; }
    public String getNombreSecuencia() { return nombreSecuencia; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public boolean isSiBloqueado() { return siBloqueado; }
    public void setSiBloqueado(boolean siBloqueado) { this.siBloqueado = siBloqueado; }
    public String getCodDesbloqueo() { return codDesbloqueo; }
    public void setCodDesbloqueo(String codDesbloqueo) { this.codDesbloqueo = codDesbloqueo; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated public String getObsTalonario() { return obsTalonario; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Las notas van a fal_observacion. */
    @Deprecated public void setObsTalonario(String obsTalonario) { this.obsTalonario = obsTalonario; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean estaOperativo() {
        return siActivo && !siBloqueado;
    }
}

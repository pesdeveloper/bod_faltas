package ar.gob.malvinas.faltas.core.domain.model;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class FalArticuloNormativaFaltas {
    private final Long id;
    private final Long normativaId;
    private final String codigoArticulo;
    private final int versionArticulo;
    private final String nombreArticulo;
    private String descripcionArticulo;
    private final BigDecimal cantidadUnidades;
    private final TipoUnidad tipoUnidad;
    private final boolean siTienePagoVoluntario;
    private BigDecimal cantidadUnidadesPagoVoluntario;
    private TipoUnidad tipoUnidadPagoVoluntario;
    private boolean siActivo;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    public FalArticuloNormativaFaltas(Long id, Long normativaId, String codigoArticulo,
            int versionArticulo, String nombreArticulo, BigDecimal cantidadUnidades,
            TipoUnidad tipoUnidad, boolean siTienePagoVoluntario,
            LocalDate fhVigDesde, LocalDateTime fhAlta, String idUserAlta) {
        this.id = id; this.normativaId = normativaId; this.codigoArticulo = codigoArticulo;
        this.versionArticulo = versionArticulo; this.nombreArticulo = nombreArticulo;
        this.cantidadUnidades = cantidadUnidades; this.tipoUnidad = tipoUnidad;
        this.siTienePagoVoluntario = siTienePagoVoluntario;
        this.fhVigDesde = fhVigDesde; this.siActivo = true;
        this.fhAlta = fhAlta; this.idUserAlta = idUserAlta;
    }
    public Long getId() { return id; }
    public Long getNormativaId() { return normativaId; }
    public String getCodigoArticulo() { return codigoArticulo; }
    public int getVersionArticulo() { return versionArticulo; }
    public String getNombreArticulo() { return nombreArticulo; }
    public String getDescripcionArticulo() { return descripcionArticulo; }
    public void setDescripcionArticulo(String v) { this.descripcionArticulo = v; }
    public BigDecimal getCantidadUnidades() { return cantidadUnidades; }
    public TipoUnidad getTipoUnidad() { return tipoUnidad; }
    public boolean isSiTienePagoVoluntario() { return siTienePagoVoluntario; }
    public BigDecimal getCantidadUnidadesPagoVoluntario() { return cantidadUnidadesPagoVoluntario; }
    public void setCantidadUnidadesPagoVoluntario(BigDecimal v) { this.cantidadUnidadesPagoVoluntario = v; }
    public TipoUnidad getTipoUnidadPagoVoluntario() { return tipoUnidadPagoVoluntario; }
    public void setTipoUnidadPagoVoluntario(TipoUnidad v) { this.tipoUnidadPagoVoluntario = v; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean v) { this.siActivo = v; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate v) { this.fhVigHasta = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActivo) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        if (fhVigHasta != null && !fecha.isBefore(fhVigHasta)) return false;
        return true;
    }
}

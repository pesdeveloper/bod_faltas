package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Articulo normativo infractor, versionado historicamente.
 * Mapeada a {@code fal_articulo_normativa_faltas} en MariaDB.
 *
 * No contiene datos economicos: la formula (cantidad de unidades u monto fijo,
 * ordinaria y pago voluntario) vive exclusivamente en
 * {@link FalTarifarioUnidadFaltas}, que es POR ARTICULO
 * (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.3). Esta separacion cierra la
 * incompatibilidad estructural documentada en el CHECKPOINT de Wave 4 §3.
 *
 * {@code versionArticulo} es la version FUNCIONAL (historia reconstruible);
 * {@code versionRow} es OCC de la fila, distinta de la anterior.
 */
public class FalArticuloNormativaFaltas {

    private final Long id;
    private final Long normativaId;
    private final String codigoArticulo;
    private final int versionArticulo;
    private final String nombreArticulo;
    private String descripcionArticulo;
    private final TipoInfraccion tipoInfraccion;
    private CategoriaInfraccion categoriaInfraccion;
    private boolean siAdmiteMedidaPreventiva;
    private boolean siActivo;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private int versionRow;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalArticuloNormativaFaltas(
            Long id,
            Long normativaId,
            String codigoArticulo,
            int versionArticulo,
            String nombreArticulo,
            TipoInfraccion tipoInfraccion,
            LocalDate fhVigDesde,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (normativaId == null) throw new IllegalArgumentException("normativaId es obligatorio");
        if (codigoArticulo == null || codigoArticulo.isBlank()) throw new IllegalArgumentException("codigoArticulo es obligatorio");
        if (versionArticulo < 1) throw new IllegalArgumentException("versionArticulo debe ser >= 1");
        if (nombreArticulo == null || nombreArticulo.isBlank()) throw new IllegalArgumentException("nombreArticulo es obligatorio");
        if (tipoInfraccion == null) throw new IllegalArgumentException("tipoInfraccion es obligatorio");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.normativaId = normativaId;
        this.codigoArticulo = codigoArticulo;
        this.versionArticulo = versionArticulo;
        this.nombreArticulo = nombreArticulo;
        this.tipoInfraccion = tipoInfraccion;
        this.fhVigDesde = fhVigDesde;
        this.siActivo = true;
        this.versionRow = 0;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getNormativaId() { return normativaId; }
    public String getCodigoArticulo() { return codigoArticulo; }
    public int getVersionArticulo() { return versionArticulo; }
    public String getNombreArticulo() { return nombreArticulo; }
    public String getDescripcionArticulo() { return descripcionArticulo; }
    public void setDescripcionArticulo(String v) { this.descripcionArticulo = v; }
    public TipoInfraccion getTipoInfraccion() { return tipoInfraccion; }
    public CategoriaInfraccion getCategoriaInfraccion() { return categoriaInfraccion; }
    public void setCategoriaInfraccion(CategoriaInfraccion v) { this.categoriaInfraccion = v; }
    public boolean isSiAdmiteMedidaPreventiva() { return siAdmiteMedidaPreventiva; }
    public void setSiAdmiteMedidaPreventiva(boolean v) { this.siAdmiteMedidaPreventiva = v; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean v) { this.siActivo = v; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate v) { this.fhVigHasta = v; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActivo) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        return fhVigHasta == null || fecha.isBefore(fhVigHasta);
    }

    public FalArticuloNormativaFaltas copia() {
        FalArticuloNormativaFaltas c = new FalArticuloNormativaFaltas(
                id, normativaId, codigoArticulo, versionArticulo, nombreArticulo, tipoInfraccion, fhVigDesde, fhAlta, idUserAlta);
        c.descripcionArticulo = this.descripcionArticulo;
        c.categoriaInfraccion = this.categoriaInfraccion;
        c.siAdmiteMedidaPreventiva = this.siAdmiteMedidaPreventiva;
        c.siActivo = this.siActivo;
        c.fhVigHasta = this.fhVigHasta;
        c.versionRow = this.versionRow;
        return c;
    }
}

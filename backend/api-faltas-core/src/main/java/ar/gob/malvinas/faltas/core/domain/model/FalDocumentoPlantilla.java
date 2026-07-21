package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plantilla documental del sistema.
 *
 * Alineado con fal_documento_plantilla MariaDB.
 * {@code codigo} persiste como VARCHAR(12) y {@code nombre} como VARCHAR(64).
 * No existe un campo {@code descripcion}: el nombre comunica el uso y la intencion.
 * La plantilla NO define idTalonario, politicaNumeracionId ni claseTalonario.
 *
 * FULL-R1.2-CORRECCION-04.
 *
 * Invariante:
 * - Si siRequiereNumeracion=false => momentoNumeracionDocu = NO_APLICA.
 * - Si siRequiereNumeracion=true  => momentoNumeracionDocu != NO_APLICA.
 * - Si fhVigHasta no es null, no puede ser anterior a fhVigDesde.
 */
public class FalDocumentoPlantilla {

    private final Long id;
    private final String codigo;
    private String nombre;
    private final TipoDocu tipoDocu;
    private final AccionDocumental accionDocumental;
    private final TipoActa tipoActa;
    private TipoFirmaReq tipoFirmaReq;
    private final boolean siRequiereNumeracion;
    private final MomentoNumeracionDocu momentoNumeracionDocu;
    private boolean siNotificable;
    private boolean siGeneraPdf;
    private boolean siSeleccionable;
    private boolean siActiva;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoPlantilla(
            Long id,
            String codigo,
            String nombre,
            TipoDocu tipoDocu,
            AccionDocumental accionDocumental,
            TipoActa tipoActa,
            TipoFirmaReq tipoFirmaReq,
            boolean siRequiereNumeracion,
            MomentoNumeracionDocu momentoNumeracionDocu,
            boolean siNotificable,
            boolean siGeneraPdf,
            boolean siSeleccionable,
            boolean siActiva,
            LocalDate fhVigDesde,
            LocalDate fhVigHasta,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoDocu = tipoDocu;
        this.accionDocumental = accionDocumental;
        this.tipoActa = tipoActa;
        this.tipoFirmaReq = tipoFirmaReq;
        this.siRequiereNumeracion = siRequiereNumeracion;
        this.momentoNumeracionDocu = momentoNumeracionDocu;
        this.siNotificable = siNotificable;
        this.siGeneraPdf = siGeneraPdf;
        this.siSeleccionable = siSeleccionable;
        this.siActiva = siActiva;
        this.fhVigDesde = fhVigDesde;
        this.fhVigHasta = fhVigHasta;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoDocu getTipoDocu() { return tipoDocu; }
    public AccionDocumental getAccionDocumental() { return accionDocumental; }
    public TipoActa getTipoActa() { return tipoActa; }
    public TipoFirmaReq getTipoFirmaReq() { return tipoFirmaReq; }
    public void setTipoFirmaReq(TipoFirmaReq tipoFirmaReq) { this.tipoFirmaReq = tipoFirmaReq; }
    public boolean isSiRequiereNumeracion() { return siRequiereNumeracion; }
    public MomentoNumeracionDocu getMomentoNumeracionDocu() { return momentoNumeracionDocu; }
    public boolean isSiNotificable() { return siNotificable; }
    public void setSiNotificable(boolean siNotificable) { this.siNotificable = siNotificable; }
    public boolean isSiGeneraPdf() { return siGeneraPdf; }
    public void setSiGeneraPdf(boolean siGeneraPdf) { this.siGeneraPdf = siGeneraPdf; }
    public boolean isSiSeleccionable() { return siSeleccionable; }
    public void setSiSeleccionable(boolean siSeleccionable) { this.siSeleccionable = siSeleccionable; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) { this.fhVigHasta = fhVigHasta; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}

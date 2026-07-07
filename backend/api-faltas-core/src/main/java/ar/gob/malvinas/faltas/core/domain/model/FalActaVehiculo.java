package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;

import java.time.LocalDateTime;

/**
 * Datos del vehiculo registrado en el acta.
 * Mapeada a fal_acta_vehiculo en MariaDB.
 * 1:1 con fal_acta. Un vehiculo por acta.
 *
 * Si se informa modelo normalizado, debe existir y pertenecer a la marca informada.
 * No se bloquea el labrado por ausencia de catalogo: se permite fallback textual.
 */
public class FalActaVehiculo {

    private final Long actaId;
    private String dominioVehiculo;
    private TipoVehiculo tipoVehiculo;
    private Long marcaVehiculoId;
    private String marcaVehiculoTxt;
    private Long modeloVehiculoId;
    private String modeloVehiculoTxt;
    private Short anioVehiculo;
    private String colorVehiculo;
    private EstadoGeneralVehiculo estadoGeneralVehiculo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaVehiculo(Long actaId, LocalDateTime fhAlta, String idUserAlta) {
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.actaId = actaId;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getActaId() { return actaId; }

    public String getDominioVehiculo() { return dominioVehiculo; }
    public void setDominioVehiculo(String dominio) {
        if (dominio != null && dominio.length() > 10)
            throw new IllegalArgumentException("dominioVehiculo max 10 caracteres");
        this.dominioVehiculo = dominio;
    }

    public TipoVehiculo getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(TipoVehiculo tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; }

    public Long getMarcaVehiculoId() { return marcaVehiculoId; }
    public String getMarcaVehiculoTxt() { return marcaVehiculoTxt; }
    public Long getModeloVehiculoId() { return modeloVehiculoId; }
    public String getModeloVehiculoTxt() { return modeloVehiculoTxt; }

    public void setMarca(Long marcaId, String marcaTxt) {
        if (marcaTxt != null && marcaTxt.length() > 24)
            throw new IllegalArgumentException("marcaVehiculoTxt max 24 caracteres");
        this.marcaVehiculoId = marcaId;
        this.marcaVehiculoTxt = marcaTxt;
    }

    public void setModelo(Long modeloId, String modeloTxt) {
        if (modeloTxt != null && modeloTxt.length() > 24)
            throw new IllegalArgumentException("modeloVehiculoTxt max 24 caracteres");
        this.modeloVehiculoId = modeloId;
        this.modeloVehiculoTxt = modeloTxt;
    }

    public Short getAnioVehiculo() { return anioVehiculo; }
    public void setAnioVehiculo(Short anioVehiculo) { this.anioVehiculo = anioVehiculo; }

    public String getColorVehiculo() { return colorVehiculo; }
    public void setColorVehiculo(String color) {
        if (color != null && color.length() > 24)
            throw new IllegalArgumentException("colorVehiculo max 24 caracteres");
        this.colorVehiculo = color;
    }

    public EstadoGeneralVehiculo getEstadoGeneralVehiculo() { return estadoGeneralVehiculo; }
    public void setEstadoGeneralVehiculo(EstadoGeneralVehiculo estado) { this.estadoGeneralVehiculo = estado; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalActaVehiculo copia() {
        FalActaVehiculo c = new FalActaVehiculo(actaId, fhAlta, idUserAlta);
        c.dominioVehiculo = this.dominioVehiculo;
        c.tipoVehiculo = this.tipoVehiculo;
        c.marcaVehiculoId = this.marcaVehiculoId;
        c.marcaVehiculoTxt = this.marcaVehiculoTxt;
        c.modeloVehiculoId = this.modeloVehiculoId;
        c.modeloVehiculoTxt = this.modeloVehiculoTxt;
        c.anioVehiculo = this.anioVehiculo;
        c.colorVehiculo = this.colorVehiculo;
        c.estadoGeneralVehiculo = this.estadoGeneralVehiculo;
        return c;
    }
}

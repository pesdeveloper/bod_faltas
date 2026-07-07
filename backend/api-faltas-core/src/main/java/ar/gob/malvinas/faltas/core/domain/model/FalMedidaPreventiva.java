package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;

import java.time.LocalDateTime;

/**
 * Catálogo de medidas preventivas disponibles en el sistema de faltas.
 * Mapeada a fal_medida_preventiva en MariaDB.
 *
 * Soporta múltiples versiones históricas por código.
 * Solo una versión activa por código en un momento dado.
 * La medida no crea bloqueante por sí sola: solo informa el tipo default sugerido.
 */
public class FalMedidaPreventiva {

    private final Long id;
    private final String codigo;
    private final short versionMedida;
    private final String descripcion;
    private String descripcionDetalle;
    private Long idDep;
    private Short verDep;
    private boolean siActiva;
    private boolean siPuedeBloquearCierre;
    private OrigenBloqueanteMaterial tipoBloqueanteDefault;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalMedidaPreventiva(
            Long id,
            String codigo,
            short versionMedida,
            String descripcion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo es obligatorio");
        if (codigo.trim().length() > 12) throw new IllegalArgumentException("codigo max 12 caracteres");
        if (versionMedida < 1) throw new IllegalArgumentException("versionMedida debe ser >= 1");
        if (descripcion == null || descripcion.isBlank()) throw new IllegalArgumentException("descripcion es obligatoria");
        if (descripcion.length() > 160) throw new IllegalArgumentException("descripcion max 160 caracteres");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.codigo = codigo.trim().toUpperCase();
        this.versionMedida = versionMedida;
        this.descripcion = descripcion;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public short getVersionMedida() { return versionMedida; }
    public String getDescripcion() { return descripcion; }

    public String getDescripcionDetalle() { return descripcionDetalle; }
    public void setDescripcionDetalle(String descripcionDetalle) {
        if (descripcionDetalle != null && descripcionDetalle.length() > 255)
            throw new IllegalArgumentException("descripcionDetalle max 255 caracteres");
        this.descripcionDetalle = descripcionDetalle;
    }

    public Long getIdDep() { return idDep; }
    public Short getVerDep() { return verDep; }
    public void setDependencia(Long idDep, Short verDep) {
        if ((idDep == null) != (verDep == null))
            throw new IllegalArgumentException("idDep y verDep deben informarse juntos o ambos null");
        this.idDep = idDep;
        this.verDep = verDep;
    }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public boolean isSiPuedeBloquearCierre() { return siPuedeBloquearCierre; }
    public void setSiPuedeBloquearCierre(boolean siPuedeBloquearCierre) {
        this.siPuedeBloquearCierre = siPuedeBloquearCierre;
        if (!siPuedeBloquearCierre) {
            this.tipoBloqueanteDefault = null;
        }
    }

    public OrigenBloqueanteMaterial getTipoBloqueanteDefault() { return tipoBloqueanteDefault; }
    public void setTipoBloqueanteDefault(OrigenBloqueanteMaterial tipoBloqueanteDefault) {
        if (tipoBloqueanteDefault != null && !siPuedeBloquearCierre)
            throw new IllegalArgumentException("tipoBloqueanteDefault requiere siPuedeBloquearCierre=true");
        this.tipoBloqueanteDefault = tipoBloqueanteDefault;
    }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalMedidaPreventiva copia() {
        FalMedidaPreventiva c = new FalMedidaPreventiva(id, codigo, versionMedida, descripcion, fhAlta, idUserAlta);
        c.descripcionDetalle = this.descripcionDetalle;
        c.idDep = this.idDep;
        c.verDep = this.verDep;
        c.siActiva = this.siActiva;
        c.siPuedeBloquearCierre = this.siPuedeBloquearCierre;
        c.tipoBloqueanteDefault = this.tipoBloqueanteDefault;
        return c;
    }
}

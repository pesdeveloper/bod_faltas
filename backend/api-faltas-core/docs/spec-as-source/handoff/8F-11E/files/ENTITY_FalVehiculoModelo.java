package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Catalogo normalizado de modelos de vehiculo por marca.
 * Mapeada a fal_vehiculo_modelo en MariaDB.
 * codigo y nombre unicos dentro de la misma marca.
 */
public class FalVehiculoModelo {

    private final Long id;
    private final Long marcaVehiculoId;
    private final String codigo;
    private final String nombre;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalVehiculoModelo(
            Long id,
            Long marcaVehiculoId,
            String codigo,
            String nombre,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (marcaVehiculoId == null) throw new IllegalArgumentException("marcaVehiculoId es obligatorio");
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo es obligatorio");
        if (codigo.trim().length() > 12) throw new IllegalArgumentException("codigo max 12 caracteres");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
        if (nombre.trim().length() > 24) throw new IllegalArgumentException("nombre max 24 caracteres");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.marcaVehiculoId = marcaVehiculoId;
        this.codigo = codigo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getMarcaVehiculoId() { return marcaVehiculoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalVehiculoModelo copia() {
        FalVehiculoModelo c = new FalVehiculoModelo(id, marcaVehiculoId, codigo, nombre, fhAlta, idUserAlta);
        c.siActivo = this.siActivo;
        return c;
    }
}

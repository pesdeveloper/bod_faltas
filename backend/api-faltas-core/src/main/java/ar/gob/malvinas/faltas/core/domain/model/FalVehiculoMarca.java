package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Catalogo normalizado de marcas de vehiculo.
 * Mapeada a fal_vehiculo_marca en MariaDB.
 * Baja logica: siActivo=false. No se borra fisicamente.
 */
public class FalVehiculoMarca {

    private final Long id;
    private final String codigo;
    private final String nombre;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalVehiculoMarca(
            Long id,
            String codigo,
            String nombre,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo es obligatorio");
        if (codigo.trim().length() > 12) throw new IllegalArgumentException("codigo max 12 caracteres");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
        if (nombre.trim().length() > 24) throw new IllegalArgumentException("nombre max 24 caracteres");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.codigo = codigo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalVehiculoMarca copia() {
        FalVehiculoMarca c = new FalVehiculoMarca(id, codigo, nombre, fhAlta, idUserAlta);
        c.siActivo = this.siActivo;
        return c;
    }
}

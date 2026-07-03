package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;

import java.time.LocalDateTime;

/**
 * Asignacion de talonario manual fisico a inspector.
 *
 * Solo los talonarios MANUAL_FISICO pueden asignarse a inspectores.
 * El rango nroDesde/nroHasta vive en NumTalonario, no aqui.
 *
 * talonarioIdActivo simula la columna generada de MariaDB para garantizar
 * unicidad de asignacion activa: tiene valor cuando siActiva=true, null cuando siActiva=false.
 *
 * Alineado con num_talonario_inspector del modelo MariaDB productivo.
 *
 * Slice 8B-3: implementacion in-memory.
 * Slice 8B-4: NumTalonarioMovimiento y emision de numero.
 * Slice 9: persiste en num_talonario_inspector via JDBC.
 */
public class NumTalonarioInspector {

    private final Long id;
    private final Long idTalonario;
    private final Long idInsp;
    private final short verInsp;
    private final LocalDateTime fhEntrega;
    private final String idUserEntrega;
    private LocalDateTime fhDevolucion;
    private String idUserDevolucion;
    private EstadoAsignacionTalonario estadoAsignacion;
    private boolean siActiva;
    private Long talonarioIdActivo;

    public NumTalonarioInspector(
            Long id,
            Long idTalonario,
            Long idInsp,
            short verInsp,
            LocalDateTime fhEntrega,
            String idUserEntrega,
            LocalDateTime fhDevolucion,
            String idUserDevolucion,
            EstadoAsignacionTalonario estadoAsignacion,
            boolean siActiva,
            Long talonarioIdActivo) {
        this.id = id;
        this.idTalonario = idTalonario;
        this.idInsp = idInsp;
        this.verInsp = verInsp;
        this.fhEntrega = fhEntrega;
        this.idUserEntrega = idUserEntrega;
        this.fhDevolucion = fhDevolucion;
        this.idUserDevolucion = idUserDevolucion;
        this.estadoAsignacion = estadoAsignacion;
        this.siActiva = siActiva;
        this.talonarioIdActivo = talonarioIdActivo;
    }

    public Long getId() { return id; }
    public Long getIdTalonario() { return idTalonario; }
    public Long getIdInsp() { return idInsp; }
    public short getVerInsp() { return verInsp; }
    public LocalDateTime getFhEntrega() { return fhEntrega; }
    public String getIdUserEntrega() { return idUserEntrega; }

    public LocalDateTime getFhDevolucion() { return fhDevolucion; }
    public void setFhDevolucion(LocalDateTime fhDevolucion) { this.fhDevolucion = fhDevolucion; }

    public String getIdUserDevolucion() { return idUserDevolucion; }
    public void setIdUserDevolucion(String idUserDevolucion) { this.idUserDevolucion = idUserDevolucion; }

    public EstadoAsignacionTalonario getEstadoAsignacion() { return estadoAsignacion; }
    public void setEstadoAsignacion(EstadoAsignacionTalonario estadoAsignacion) { this.estadoAsignacion = estadoAsignacion; }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public Long getTalonarioIdActivo() { return talonarioIdActivo; }
    public void setTalonarioIdActivo(Long talonarioIdActivo) { this.talonarioIdActivo = talonarioIdActivo; }
}

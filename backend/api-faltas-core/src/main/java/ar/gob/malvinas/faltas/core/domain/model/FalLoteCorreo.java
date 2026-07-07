package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoLote;

import java.time.LocalDateTime;

/**
 * Lote de correo postal.
 * Agrupa notificaciones para envio colectivo por Correo Argentino.
 * El lote no guarda domicilio ni documento: esa informacion vive en los intentos.
 */
public class FalLoteCorreo {

    private final Long id;
    private final String loteCodigo;
    private EstadoLote estadoLote;
    private String referenciaExterna;
    private String guidLoteExt;
    private final LocalDateTime fhGeneracion;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalLoteCorreo(
            Long id,
            String loteCodigo,
            LocalDateTime fhGeneracion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id no puede ser null");
        if (loteCodigo == null || loteCodigo.isBlank()) throw new IllegalArgumentException("loteCodigo no puede ser nulo/blanco");
        if (loteCodigo.length() > 30) throw new IllegalArgumentException("loteCodigo excede 30 caracteres");
        if (fhGeneracion == null) throw new IllegalArgumentException("fhGeneracion no puede ser null");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta no puede ser null");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta no puede ser nulo/blanco");
        this.id = id;
        this.loteCodigo = loteCodigo;
        this.fhGeneracion = fhGeneracion;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.estadoLote = EstadoLote.GENERADO;
    }

    public Long getId() { return id; }
    public String getLoteCodigo() { return loteCodigo; }

    public EstadoLote getEstadoLote() { return estadoLote; }
    public void setEstadoLote(EstadoLote estadoLote) { this.estadoLote = estadoLote; }

    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) {
        if (referenciaExterna != null && referenciaExterna.length() > 60)
            throw new IllegalArgumentException("referenciaExterna excede 60 caracteres");
        this.referenciaExterna = referenciaExterna;
    }

    public String getGuidLoteExt() { return guidLoteExt; }
    public void setGuidLoteExt(String guidLoteExt) {
        if (guidLoteExt != null && guidLoteExt.length() != 36)
            throw new IllegalArgumentException("guidLoteExt debe tener exactamente 36 caracteres (UUID), fue: " + (guidLoteExt.length()));
        this.guidLoteExt = guidLoteExt;
    }

    public LocalDateTime getFhGeneracion() { return fhGeneracion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean esAnulable() { return estadoLote.esAnulable(); }
    public boolean esEmitible() { return estadoLote.esEmitible(); }
    public boolean esProcesable() { return estadoLote.esProcesable(); }

    public FalLoteCorreo copia() {
        FalLoteCorreo c = new FalLoteCorreo(id, loteCodigo, fhGeneracion, fhAlta, idUserAlta);
        c.estadoLote = this.estadoLote;
        c.referenciaExterna = this.referenciaExterna;
        c.guidLoteExt = this.guidLoteExt;
        return c;
    }
}

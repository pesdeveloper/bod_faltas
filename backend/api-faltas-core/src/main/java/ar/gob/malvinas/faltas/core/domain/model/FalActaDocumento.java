package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;

import java.time.LocalDateTime;

/**
 * Pivot canonico de pertenencia funcional de un documento a un expediente.
 *
 * Corresponde a fal_acta_documento en MariaDB.
 * La identidad es compuesta: (actaId, documentoId).
 * No tiene ID artificial.
 *
 * siPrincipal indica si este es el documento operativo vigente para
 * su rol dentro del expediente. La unicidad de principal por (acta, rol)
 * es responsabilidad del repository con sus invariantes de concurrencia.
 *
 * La relacion es historica: no se borra fisicamente. Se marca no-principal
 * cuando es reemplazada.
 *
 * Auditoria obligatoria: fhAlta e idUserAlta siempre presentes.
 */
public class FalActaDocumento {

    private final ActaDocumentoId id;
    private final RolDocuActa rolDocuActa;
    private boolean siPrincipal;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaDocumento(
            Long actaId,
            Long documentoId,
            RolDocuActa rolDocuActa,
            boolean siPrincipal,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (rolDocuActa == null) throw new IllegalArgumentException("rolDocuActa requerido");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta requerido");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta requerido");
        if (idUserAlta.length() > 36) throw new IllegalArgumentException("idUserAlta max 36 chars");
        this.id = new ActaDocumentoId(actaId, documentoId);
        this.rolDocuActa = rolDocuActa;
        this.siPrincipal = siPrincipal;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public ActaDocumentoId getId() { return id; }
    public Long getActaId() { return id.actaId(); }
    public Long getDocumentoId() { return id.documentoId(); }
    public RolDocuActa getRolDocuActa() { return rolDocuActa; }
    public boolean isSiPrincipal() { return siPrincipal; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Solo el repository puede cambiar siPrincipal en operaciones atomicas. */
    public void setSiPrincipalInterno(boolean siPrincipal) {
        this.siPrincipal = siPrincipal;
    }

    public FalActaDocumento copia() {
        return new FalActaDocumento(
                id.actaId(), id.documentoId(), rolDocuActa, siPrincipal, fhAlta, idUserAlta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FalActaDocumento other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}

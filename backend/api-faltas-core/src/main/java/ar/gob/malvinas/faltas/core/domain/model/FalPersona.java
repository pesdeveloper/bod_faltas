package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;

import java.time.LocalDateTime;

/**
 * Persona infractora o responsable asociada a un acta de faltas.
 * Mapeada a fal_persona en MariaDB.
 *
 * Reglas de dominio:
 * - FISICA: razonSocial debe ser null; apellido/nombres opcionales pero debe tener al menos una forma de identificacion.
 * - JURIDICA: apellido y nombres deben ser null; razonSocial o nombreMostrar obligatorios.
 * - tipoDoc y nroDoc se informan juntos o ambos quedan null.
 * - nroDoc: max 20 caracteres, no puede ser vacio si presente.
 * - nombreMostrar: calculado; max 64 caracteres.
 * - emailPrincipal: max 160 caracteres.
 * - telefonoPrincipal: max 20 caracteres.
 * - Para Faltas, idSuj debe ser 20 cuando se informa.
 * - idBie no puede existir sin idSuj.
 *
 * No tiene versionRow: fal_persona no lo define en MariaDB.
 */
public class FalPersona {

    private final Long id;
    private final TipoPersona tipoPersona;

    private TipoDocumentoPersona tipoDoc;
    private String nroDoc;

    private String apellido;
    private String nombres;
    private String razonSocial;
    private String nombreMostrar;

    private String emailPrincipal;
    private String telefonoPrincipal;

    private Long idSuj;
    private Long idBie;
    private SujBieEstado sujBieEstado;
    private LocalDateTime fhSujBieCreacion;

    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalPersona(
            Long id,
            TipoPersona tipoPersona,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio en FalPersona");
        if (tipoPersona == null) throw new IllegalArgumentException("tipoPersona es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatorio");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.tipoPersona = tipoPersona;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.fhUltMod = fhAlta;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public TipoPersona getTipoPersona() { return tipoPersona; }
    public TipoDocumentoPersona getTipoDoc() { return tipoDoc; }
    public String getNroDoc() { return nroDoc; }
    public String getApellido() { return apellido; }
    public String getNombres() { return nombres; }
    public String getRazonSocial() { return razonSocial; }
    public String getNombreMostrar() { return nombreMostrar; }
    public String getEmailPrincipal() { return emailPrincipal; }
    public String getTelefonoPrincipal() { return telefonoPrincipal; }
    public Long getIdSuj() { return idSuj; }
    public Long getIdBie() { return idBie; }
    public SujBieEstado getSujBieEstado() { return sujBieEstado; }
    public LocalDateTime getFhSujBieCreacion() { return fhSujBieCreacion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }

    // --- Setters controlados ---

    public void setTipoDoc(TipoDocumentoPersona tipoDoc) { this.tipoDoc = tipoDoc; }
    public void setNroDoc(String nroDoc) { this.nroDoc = nroDoc; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public void setNombreMostrar(String nombreMostrar) { this.nombreMostrar = nombreMostrar; }
    public void setEmailPrincipal(String emailPrincipal) { this.emailPrincipal = emailPrincipal; }
    public void setTelefonoPrincipal(String telefonoPrincipal) { this.telefonoPrincipal = telefonoPrincipal; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public void setSujBieEstado(SujBieEstado sujBieEstado) { this.sujBieEstado = sujBieEstado; }
    public void setIdSuj(Long idSuj) { this.idSuj = idSuj; }
    public void setIdBie(Long idBie) { this.idBie = idBie; }
    public void setFhSujBieCreacion(LocalDateTime fhSujBieCreacion) { this.fhSujBieCreacion = fhSujBieCreacion; }

    // --- Metodos de dominio ---

    public boolean esFisica() { return tipoPersona == TipoPersona.FISICA; }
    public boolean esJuridica() { return tipoPersona == TipoPersona.JURIDICA; }

    /** Nombre para mostrar calculado segun tipo. */
    public String calcularNombreMostrar() {
        if (esFisica()) {
            if (apellido != null && nombres != null) return apellido.toUpperCase() + ", " + nombres;
            if (apellido != null) return apellido.toUpperCase();
            if (nombres != null) return nombres;
        } else {
            if (razonSocial != null) return razonSocial;
        }
        return nombreMostrar;
    }

    /** Texto del documento en formato legible. */
    public String docTxt() {
        if (tipoDoc == null || nroDoc == null) return null;
        return tipoDoc.name() + " " + nroDoc;
    }

    public boolean tieneCuentaActiva() {
        return sujBieEstado == SujBieEstado.ACTIVA;
    }

    public FalPersona copia() {
        FalPersona c = new FalPersona(id, tipoPersona, fhAlta, idUserAlta);
        c.tipoDoc = this.tipoDoc;
        c.nroDoc = this.nroDoc;
        c.apellido = this.apellido;
        c.nombres = this.nombres;
        c.razonSocial = this.razonSocial;
        c.nombreMostrar = this.nombreMostrar;
        c.emailPrincipal = this.emailPrincipal;
        c.telefonoPrincipal = this.telefonoPrincipal;
        c.idSuj = this.idSuj;
        c.idBie = this.idBie;
        c.sujBieEstado = this.sujBieEstado;
        c.fhSujBieCreacion = this.fhSujBieCreacion;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}

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
 * - apellido: max 24 caracteres (VARCHAR(24) en MariaDB - HUMAN_DECISION_CLOSED).
 * - nombres: max 36 caracteres (VARCHAR(36) en MariaDB - HUMAN_DECISION_CLOSED).
 * - razonSocial: max 64 caracteres (VARCHAR(64) en MariaDB).
 * - nombreMostrar: calculado; max 64 caracteres (VARCHAR(64) en MariaDB).
 * - emailPrincipal: max 160 caracteres.
 * - telefonoPrincipal: max 20 caracteres.
 * - Para Faltas, idSuj debe ser 20 cuando se informa.
 * - idBie no puede existir sin idSuj.
 * - idSuj: codigo de tipo de sujeto en Ingresos Municipales; rango 1-255 (TINYINT UNSIGNED); catalogo abierto.
 * - idBie: identificador de bien o cuenta; rango 1-9999999 (MEDIUMINT UNSIGNED).
 * - Usar Integer (no Long) para idSuj e idBie (HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10).
 *
 * Estructura fisica del documento (HUMAN_DECISION_CLOSED - SPEC-MODEL-DDL-CLOSURE-001):
 * - tipo_documento SMALLINT NOT NULL (TipoDocumentoPersona)
 * - prefijo_cuit_cuil TINYINT UNSIGNED NULL (solo CUIT/CUIL: 0-99)
 * - nro_doc INT UNSIGNED NOT NULL (solo parte numerica: 1-99999999)
 * - digito_verificador TINYINT UNSIGNED NULL (solo CUIT/CUIL: 0-9)
 * Los campos tipoDoc/nroDoc se mantienen por compatibilidad durante la transicion al adapter JDBC.
 *
 * No tiene versionRow: fal_persona no lo define en MariaDB (DECISION_DDL-PERS-01 CERRADA).
 */
public class FalPersona {

    private final Long id;
    private final TipoPersona tipoPersona;

    private TipoDocumentoPersona tipoDoc;
    private String nroDoc;

    // Estructura fisica del documento (HUMAN_DECISION_CLOSED - SPEC-MODEL-DDL-CLOSURE-001):
    // tipo_documento SMALLINT, prefijo_cuit_cuil TINYINT UNSIGNED NULL, nro_doc INT UNSIGNED, digito_verificador TINYINT UNSIGNED NULL
    private Integer prefijoCuitCuil;
    private Integer digitoVerificador;

    private String apellido;
    private String nombres;
    private String razonSocial;
    private String nombreMostrar;

    private String emailPrincipal;
    private String telefonoPrincipal;

    private Integer idSuj;
    private Integer idBie;
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
    public Integer getPrefijoCuitCuil() { return prefijoCuitCuil; }
    public Integer getDigitoVerificador() { return digitoVerificador; }
    public String getApellido() { return apellido; }
    public String getNombres() { return nombres; }
    public String getRazonSocial() { return razonSocial; }
    public String getNombreMostrar() { return nombreMostrar; }
    public String getEmailPrincipal() { return emailPrincipal; }
    public String getTelefonoPrincipal() { return telefonoPrincipal; }
    public Integer getIdSuj() { return idSuj; }
    public Integer getIdBie() { return idBie; }
    public SujBieEstado getSujBieEstado() { return sujBieEstado; }
    public LocalDateTime getFhSujBieCreacion() { return fhSujBieCreacion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }

    // --- Setters controlados ---

    public void setTipoDoc(TipoDocumentoPersona tipoDoc) { this.tipoDoc = tipoDoc; }
    public void setNroDoc(String nroDoc) { this.nroDoc = nroDoc; }
    public void setPrefijoCuitCuil(Integer prefijoCuitCuil) { this.prefijoCuitCuil = prefijoCuitCuil; }
    public void setDigitoVerificador(Integer digitoVerificador) { this.digitoVerificador = digitoVerificador; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public void setNombreMostrar(String nombreMostrar) { this.nombreMostrar = nombreMostrar; }
    public void setEmailPrincipal(String emailPrincipal) { this.emailPrincipal = emailPrincipal; }
    public void setTelefonoPrincipal(String telefonoPrincipal) { this.telefonoPrincipal = telefonoPrincipal; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public void setSujBieEstado(SujBieEstado sujBieEstado) { this.sujBieEstado = sujBieEstado; }
    public void setIdSuj(Integer idSuj) {
        if (idSuj != null && (idSuj < 1 || idSuj > 255))
            throw new IllegalArgumentException(
                    "idSuj debe estar entre 1 y 255, o ser null; valor: " + idSuj);
        this.idSuj = idSuj;
    }

    public void setIdBie(Integer idBie) {
        if (idBie != null && (idBie < 1 || idBie > 9_999_999))
            throw new IllegalArgumentException(
                    "idBie debe estar entre 1 y 9.999.999, o ser null; valor: " + idBie);
        if (idBie != null && this.idSuj == null)
            throw new IllegalArgumentException(
                    "idBie no puede informarse sin idSuj");
        this.idBie = idBie;
    }
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
        c.prefijoCuitCuil = this.prefijoCuitCuil;
        c.digitoVerificador = this.digitoVerificador;
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

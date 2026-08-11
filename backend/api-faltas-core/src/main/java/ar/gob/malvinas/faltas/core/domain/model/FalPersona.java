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
 * - id: null en una entidad nueva; positivo en una entidad persistida (DECISION_ID-01).
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
 * Estructura del documento (DECISION_DDL-PERS-01, modelo objetivo):
 * - tipo_documento SMALLINT NULL (TipoDocumentoPersona)
 * - prefijo_cuit_cuil TINYINT UNSIGNED NULL (solo CUIT/CUIL: 10-99)
 * - nro_doc VARCHAR(12) NULL (forma canonica; cuerpo central de 8 digitos en CUIT/CUIL)
 * - digito_verificador TINYINT UNSIGNED NULL (solo CUIT/CUIL: 0-9)
 * Los cuatro componentes viven en un unico value object, {@link DocumentoPersona}, que es
 * la unica autoridad de normalizacion y la unica via de mutacion: no hay setters
 * individuales que puedan romper la coherencia del par.
 *
 * No tiene versionRow: fal_persona no lo define en MariaDB (DECISION_DDL-PERS-01).
 */
public class FalPersona {

    private final Long id;
    private final TipoPersona tipoPersona;

    private DocumentoPersona documento = DocumentoPersona.ausente();

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

    /**
     * @param id {@code null} para una entidad nueva; positivo para una entidad ya
     *           persistida. La PK la asigna la persistencia (DECISION_ID-01).
     */
    public FalPersona(
            Long id,
            TipoPersona tipoPersona,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id != null && id <= 0)
            throw new IllegalArgumentException("id debe ser positivo o null en FalPersona; valor: " + id);
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
    public DocumentoPersona getDocumento() { return documento; }
    public TipoDocumentoPersona getTipoDoc() { return documento.tipoDoc(); }
    public String getNroDoc() { return documento.nroDoc(); }
    public Integer getPrefijoCuitCuil() { return documento.prefijoCuitCuil(); }
    public Integer getDigitoVerificador() { return documento.digitoVerificador(); }
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

    /**
     * Unica via de mutacion del documento. Recibe la forma ya canonica, de modo que la
     * coherencia del par y de los componentes de CUIT/CUIL queda garantizada por
     * {@link DocumentoPersona}. No existen setters individuales de tipo, numero, prefijo
     * ni digito: eso permitiria construir un documento incoherente en dos pasos.
     */
    public void setDocumento(DocumentoPersona documento) {
        this.documento = (documento == null) ? DocumentoPersona.ausente() : documento;
    }

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

    /**
     * Texto del documento en formato legible. Para CUIT/CUIL rinde la forma visual
     * {@code XX-XXXXXXXX-X} reconstruida desde los tres componentes, no el cuerpo central
     * desnudo.
     */
    public String docTxt() {
        if (documento.estaAusente()) return null;
        return documento.tipoDoc().name() + " " + documento.nroDocVisible();
    }

    public boolean tieneCuentaActiva() {
        return sujBieEstado == SujBieEstado.ACTIVA;
    }

    /**
     * Formas minimas de identificacion. Una persona sin ninguna de ellas no representa a
     * una persona real y no debe persistirse: la ausencia de sujeto identificado se
     * expresa con {@code id_persona_infractor} NULL, nunca con una fila vacia
     * (DECISION_DDL-PERS-01, prohibicion de persona centinela o ficticia).
     */
    public boolean tieneIdentificacionMinima() {
        return nombreMostrar != null
                || apellido != null
                || nombres != null
                || razonSocial != null
                || documento.nroDoc() != null
                || idSuj != null;
    }

    /**
     * Copia con la PK que asigno la persistencia. No muta la instancia receptora
     * (DECISION_ID-01).
     */
    public FalPersona conId(Long idAsignado) {
        if (idAsignado == null || idAsignado <= 0)
            throw new IllegalArgumentException("conId exige un id positivo; valor: " + idAsignado);
        return copiarCon(idAsignado);
    }

    public FalPersona copia() {
        return copiarCon(this.id);
    }

    private FalPersona copiarCon(Long idDestino) {
        FalPersona c = new FalPersona(idDestino, tipoPersona, fhAlta, idUserAlta);
        c.documento = this.documento;
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

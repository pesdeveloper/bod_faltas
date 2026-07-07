package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenCaptura;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agregado raiz del expediente de acta de faltas.
 *
 * Paridad campo-a-campo con fal_acta (MariaDB).
 * Campos expandidos en 8F-11L para alcanzar ALINEADO completo.
 *
 * FK de persona y domicilios (desde 8F-11C):
 *   idPersonaInfractor  -> fal_persona.id  (nullable durante transicion)
 *   idDomicilioInfractorAct -> fal_persona_domicilio.id (nullable)
 *   idDomicilioNotifAct     -> fal_persona_domicilio.id (nullable)
 *
 * Reglas invariantes:
 * - idTecnico es UUID unico e idempotente para alta/sync.
 * - verDep + idDep deben estar ambos presentes.
 * - verInsp es obligatoria si idInsp no es null.
 * - idTalonario y nroTalonarioUsado son ambos NULL o ambos NOT NULL.
 * - resultadoFirmaInfractor es obligatorio en acta formal.
 * - qrPayloadVersion default 0; incrementar si cambia el payload.
 * - bloqueActual + estadoProcesal + situacionAdministrativa forman tripla valida.
 * - estaCerrada() = situacionAdministrativa == CERRADA || ANULADA.
 * - fhCierre y fhArchivo son fechas funcionales (no de auditoria).
 * - idMotivoArchivoActual y permiteReingreso son caches desde ciclo de archivo activo.
 *
 * Los datos de nombre, documento y domicilio del infractor NO se almacenan
 * en esta entidad: se obtienen de FalPersona y FalPersonaDomicilio via las FK.
 * El snapshot y los DTOs los proyectan como vista derivada.
 *
 * domicilioHecho es texto libre de compatibilidad; el lugar del hecho normalizado
 * vive en los campos id_tca_infr_malvinas / id_loc_infr_malvinas y sus versiones.
 */
public class FalActa {

    // --- Identidad y numeracion ---
    private final Long id;
    private final String uuidTecnico;           // id_tecnico CHAR(36) UK
    private String nroActa;                      // nro_acta VARCHAR(30) NULL
    private Long idTalonario;                    // id_talonario BIGINT NULL FK
    private Integer nroTalonarioUsado;           // nro_talonario_usado INT NULL

    // --- Tipo y captura ---
    private final TipoActa tipoActa;             // tipo_acta SMALLINT NOT NULL
    private OrigenCaptura origenCaptura;         // origen_captura SMALLINT NOT NULL
    private String idDispositivoCaptura;         // id_dispositivo_captura VARCHAR(80) NULL
    private String idUserCaptura;                // id_user_captura CHAR(36) NULL
    private LocalDateTime fhCaptura;             // fh_captura DATETIME(6) NOT NULL (funcional)
    private Double latCaptura;                   // lat_captura DECIMAL(12,8) NULL
    private Double lonCaptura;                   // lon_captura DECIMAL(12,8) NULL
    private Double precisionCapturaM;            // precision_captura_m DECIMAL(8,2) NULL
    private LocalDateTime fhPosCaptura;          // fh_pos_captura DATETIME(6) NULL
    private OrigenUbicacion origenPosCaptura;    // origen_pos_captura SMALLINT NULL

    // --- Fecha funcional del acta ---
    private final LocalDate fechaActa;           // fh_acta DATE NOT NULL (fecha del acta)
    private final LocalDateTime fechaLabrado;    // fecha labrado (alias de fhAlta funcional)

    // --- Dependencia e inspector (versionados) ---
    private final Long idDependencia;            // id_dep BIGINT NOT NULL FK
    private Integer verDep;                      // ver_dep SMALLINT NOT NULL
    private final Long idInspector;              // id_insp BIGINT NULL FK
    private Integer verInsp;                     // ver_insp SMALLINT NULL (obligatorio si hay inspector)

    // --- Infractor y domicilios ---
    private Long idPersonaInfractor;             // id_persona_infractor BIGINT NOT NULL FK
    private Long idDomicilioInfractorAct;        // id_domicilio_infractor_act BIGINT NULL FK
    private Long idDomicilioNotifAct;            // id_domicilio_notif_act BIGINT NULL FK

    // Campos de compatibilidad con datos embebidos previos (NO persistidos en fal_acta productivo)
    private String infractorDocumento;
    private String infractorNombre;

    // --- Resumen del hecho ---
    private String resumenHecho;                 // resumen_hecho VARCHAR(1000) NULL
    private String domicilioHecho;               // compatibilidad; en produccion usar campos de lugar del hecho

    // --- Lugar del hecho: nomenclatura local Malvinas ---
    private String idLocInfrMalvinas;            // id_loc_infr_malvinas VARCHAR(8) NULL
    private Long localidadInfrMalvinasVersionId; // localidad_infr_malvinas_version_id BIGINT NULL FK
    private String idTcaInfrMalvinas;            // id_tca_infr_malvinas VARCHAR(10) NULL
    private Long calleInfrMalvinasVersionId;     // calle_infr_malvinas_version_id BIGINT NULL FK

    // --- Lugar del hecho: altura ---
    private Integer alturaInfr;                  // altura_infr INT UNSIGNED NULL
    private Short alturaOrigenInfr;              // altura_origen_infr SMALLINT NULL
    private boolean siAlturaInfrEstimada;        // si_altura_infr_estimada BOOLEAN default false

    // --- Lugar del hecho: coordenadas finales ---
    private final Double latInfr;                // lat_infr DECIMAL(12,8) NULL
    private final Double lonInfr;                // lon_infr DECIMAL(12,8) NULL
    private OrigenUbicacion origenUbicacionInfr; // origen_ubicacion_infr SMALLINT NULL
    private boolean siUbicacionInfrManual;       // si_ubicacion_infr_manual BOOLEAN default false

    // --- Lugar del hecho: texto libre ---
    private boolean siDomTxtInfr;               // si_dom_txt_infr BOOLEAN default false
    private String domTxtInfr;                   // dom_txt_infr VARCHAR(255) NULL
    private String observaciones;                // compatibilidad con campo previo

    // --- Contexto geografico ---
    private boolean siEjeUrb;                    // si_eje_urb BOOLEAN default false

    // --- QR ---
    private String codigoQr;                     // codigo_qr VARCHAR(512) NOT NULL UNIQUE
    private Integer qrPayloadVersion;            // qr_payload_version SMALLINT default 0

    // --- Estado actual (tripla canonica) ---
    private BloqueActual bloqueActual;           // bloque_actual CHAR(4) NOT NULL
    private EstadoProcesalActa estadoProcesal;   // est_proc_act CHAR(4) NOT NULL
    private SituacionAdministrativaActa situacionAdministrativa; // sit_adm_act CHAR(4) NOT NULL

    // --- Resultado y cierre ---
    private ResultadoFinalActa resultadoFinal;   // resultado_final SMALLINT NOT NULL
    private final ResultadoFirmaInfractor resultadoFirmaInfractor; // resultado_firma_infractor SMALLINT NOT NULL

    // --- Caches de archivo/reingreso (desde ciclo activo) ---
    private Long idMotivoArchivoActual;          // id_motivo_archivo_actual BIGINT NULL FK
    private Boolean permiteReingreso;            // permite_reingreso BOOLEAN NULL
    private LocalDateTime fhCierre;             // fh_cierre DATETIME(6) NULL (fecha funcional)
    private LocalDateTime fhArchivo;            // fh_archivo DATETIME(6) NULL (fecha funcional)

    // --- OCC y auditoria ---
    private int versionRow;                      // version_row INT NOT NULL default 0
    private final LocalDateTime fhAlta;          // fh_alta DATETIME(6) NOT NULL
    private final String idUserAlta;             // id_user_alta CHAR(36) NOT NULL
    private LocalDateTime fhUltMod;             // fh_ult_mod DATETIME(6) NULL
    private String idUserUltMod;                 // id_user_ult_mod CHAR(36) NULL

    // =====================================================================
    // CONSTRUCTOR PRINCIPAL (campos minimos obligatorios)
    // =====================================================================

    public FalActa(
            Long id,
            String uuidTecnico,
            TipoActa tipoActa,
            Long idDependencia,
            Long idInspector,
            LocalDate fechaActa,
            LocalDateTime fechaLabrado,
            String domicilioHecho,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            Long idPersonaInfractor,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (resultadoFirmaInfractor == null) {
            throw new IllegalArgumentException("resultadoFirmaInfractor es obligatorio en el acta");
        }
        this.id = id;
        this.uuidTecnico = uuidTecnico;
        this.tipoActa = tipoActa;
        this.idDependencia = idDependencia;
        this.verDep = 1;
        this.idInspector = idInspector;
        this.fechaActa = fechaActa;
        this.fechaLabrado = fechaLabrado;
        this.domicilioHecho = domicilioHecho;
        this.observaciones = observaciones;
        this.latInfr = latInfr;
        this.lonInfr = lonInfr;
        this.resultadoFirmaInfractor = resultadoFirmaInfractor;
        this.idPersonaInfractor = idPersonaInfractor;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.bloqueActual = BloqueActual.CAPT;
        this.estadoProcesal = EstadoProcesalActa.EN_TRAMITE;
        this.situacionAdministrativa = SituacionAdministrativaActa.ACTIVA;
        this.resultadoFinal = ResultadoFinalActa.SIN_RESULTADO_FINAL;
        this.versionRow = 0;
        this.fhCaptura = fechaLabrado != null ? fechaLabrado : LocalDateTime.now();
        this.origenCaptura = OrigenCaptura.SISTEMA_AUTOMATICO;
        this.qrPayloadVersion = 0;
        this.siAlturaInfrEstimada = false;
        this.siUbicacionInfrManual = false;
        this.siDomTxtInfr = false;
        this.siEjeUrb = false;
    }

    /** Backward-compatible constructor para tests legacy que usan Strings. */
    public FalActa(
            Long id, String uuidTecnico,
            String tipoActa, String idDependencia, String idInspector,
            LocalDate fechaActa, LocalDateTime fechaLabrado,
            String domicilioHecho, String domicilioInfractor,
            Double latInfr, Double lonInfr,
            Long idPersonaInfractor, String infractorNombre, String infractorDocumento,
            ResultadoFirmaInfractor resultadoFirmaInfractor) {
        this(id, uuidTecnico,
                TipoActa.valueOf(tipoActa),
                parseLongOrDefault(idDependencia),
                parseLongOrDefault(idInspector),
                fechaActa, fechaLabrado,
                domicilioHecho, domicilioInfractor,
                latInfr, lonInfr, resultadoFirmaInfractor,
                idPersonaInfractor, LocalDateTime.now(), "SYS");
    }

    private static Long parseLongOrDefault(String s) {
        if (s == null) return 1L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 1L; }
    }

    // =====================================================================
    // GETTERS DE IDENTIDAD Y NUMERACION
    // =====================================================================

    public Long getId() { return id; }
    public String getUuidTecnico() { return uuidTecnico; }

    public String getNroActa() { return nroActa; }
    public void setNroActa(String nroActa) { this.nroActa = nroActa; }

    public Long getIdTalonario() { return idTalonario; }
    public void setIdTalonario(Long idTalonario) { this.idTalonario = idTalonario; }

    public Integer getNroTalonarioUsado() { return nroTalonarioUsado; }
    public void setNroTalonarioUsado(Integer nroTalonarioUsado) { this.nroTalonarioUsado = nroTalonarioUsado; }

    // =====================================================================
    // GETTERS DE TIPO Y CAPTURA
    // =====================================================================

    public TipoActa getTipoActa() { return tipoActa; }

    public OrigenCaptura getOrigenCaptura() { return origenCaptura; }
    public void setOrigenCaptura(OrigenCaptura origenCaptura) { this.origenCaptura = origenCaptura; }

    public String getIdDispositivoCaptura() { return idDispositivoCaptura; }
    public void setIdDispositivoCaptura(String idDispositivoCaptura) { this.idDispositivoCaptura = idDispositivoCaptura; }

    public String getIdUserCaptura() { return idUserCaptura; }
    public void setIdUserCaptura(String idUserCaptura) { this.idUserCaptura = idUserCaptura; }

    public LocalDateTime getFhCaptura() { return fhCaptura; }
    public void setFhCaptura(LocalDateTime fhCaptura) { this.fhCaptura = fhCaptura; }

    public Double getLatCaptura() { return latCaptura; }
    public void setLatCaptura(Double latCaptura) { this.latCaptura = latCaptura; }

    public Double getLonCaptura() { return lonCaptura; }
    public void setLonCaptura(Double lonCaptura) { this.lonCaptura = lonCaptura; }

    public Double getPrecisionCapturaM() { return precisionCapturaM; }
    public void setPrecisionCapturaM(Double precisionCapturaM) { this.precisionCapturaM = precisionCapturaM; }

    public LocalDateTime getFhPosCaptura() { return fhPosCaptura; }
    public void setFhPosCaptura(LocalDateTime fhPosCaptura) { this.fhPosCaptura = fhPosCaptura; }

    public OrigenUbicacion getOrigenPosCaptura() { return origenPosCaptura; }
    public void setOrigenPosCaptura(OrigenUbicacion origenPosCaptura) { this.origenPosCaptura = origenPosCaptura; }

    // =====================================================================
    // GETTERS DE FECHA FUNCIONAL
    // =====================================================================

    public LocalDate getFechaActa() { return fechaActa; }
    public LocalDateTime getFechaLabrado() { return fechaLabrado; }

    // =====================================================================
    // GETTERS DE DEPENDENCIA E INSPECTOR
    // =====================================================================

    public Long getIdDependencia() { return idDependencia; }

    public Integer getVerDep() { return verDep; }
    public void setVerDep(Integer verDep) { this.verDep = verDep; }

    public Long getIdInspector() { return idInspector; }

    public Integer getVerInsp() { return verInsp; }
    public void setVerInsp(Integer verInsp) { this.verInsp = verInsp; }

    // =====================================================================
    // GETTERS DE INFRACTOR Y DOMICILIOS
    // =====================================================================

    public Long getIdPersonaInfractor() { return idPersonaInfractor; }
    public void setIdPersonaInfractor(Long idPersonaInfractor) { this.idPersonaInfractor = idPersonaInfractor; }

    public String getInfractorDocumento() { return infractorDocumento; }
    public void setInfractorDocumento(String infractorDocumento) { this.infractorDocumento = infractorDocumento; }

    public String getInfractorNombre() { return infractorNombre; }
    public void setInfractorNombre(String infractorNombre) { this.infractorNombre = infractorNombre; }

    public Long getIdDomicilioInfractorAct() { return idDomicilioInfractorAct; }
    public void setIdDomicilioInfractorAct(Long idDomicilioInfractorAct) {
        this.idDomicilioInfractorAct = idDomicilioInfractorAct;
    }

    public Long getIdDomicilioNotifAct() { return idDomicilioNotifAct; }
    public void setIdDomicilioNotifAct(Long idDomicilioNotifAct) {
        this.idDomicilioNotifAct = idDomicilioNotifAct;
    }

    // =====================================================================
    // GETTERS DEL HECHO
    // =====================================================================

    public String getResumenHecho() { return resumenHecho; }
    public void setResumenHecho(String resumenHecho) { this.resumenHecho = resumenHecho; }

    public String getDomicilioHecho() { return domicilioHecho; }
    public void setDomicilioHecho(String domicilioHecho) { this.domicilioHecho = domicilioHecho; }

    public String getObservaciones() { return observaciones; }

    // =====================================================================
    // GETTERS DE LUGAR DEL HECHO (NOMENCLATURA LOCAL)
    // =====================================================================

    public String getIdLocInfrMalvinas() { return idLocInfrMalvinas; }
    public void setIdLocInfrMalvinas(String idLocInfrMalvinas) { this.idLocInfrMalvinas = idLocInfrMalvinas; }

    public Long getLocalidadInfrMalvinasVersionId() { return localidadInfrMalvinasVersionId; }
    public void setLocalidadInfrMalvinasVersionId(Long v) { this.localidadInfrMalvinasVersionId = v; }

    public String getIdTcaInfrMalvinas() { return idTcaInfrMalvinas; }
    public void setIdTcaInfrMalvinas(String idTcaInfrMalvinas) { this.idTcaInfrMalvinas = idTcaInfrMalvinas; }

    public Long getCalleInfrMalvinasVersionId() { return calleInfrMalvinasVersionId; }
    public void setCalleInfrMalvinasVersionId(Long v) { this.calleInfrMalvinasVersionId = v; }

    // =====================================================================
    // GETTERS DE LUGAR DEL HECHO (ALTURA)
    // =====================================================================

    public Integer getAlturaInfr() { return alturaInfr; }
    public void setAlturaInfr(Integer alturaInfr) { this.alturaInfr = alturaInfr; }

    public Short getAlturaOrigenInfr() { return alturaOrigenInfr; }
    public void setAlturaOrigenInfr(Short alturaOrigenInfr) { this.alturaOrigenInfr = alturaOrigenInfr; }

    public boolean isSiAlturaInfrEstimada() { return siAlturaInfrEstimada; }
    public void setSiAlturaInfrEstimada(boolean siAlturaInfrEstimada) { this.siAlturaInfrEstimada = siAlturaInfrEstimada; }

    // =====================================================================
    // GETTERS DE LUGAR DEL HECHO (COORDENADAS FINALES)
    // =====================================================================

    public Double getLatInfr() { return latInfr; }
    public Double getLonInfr() { return lonInfr; }

    public OrigenUbicacion getOrigenUbicacionInfr() { return origenUbicacionInfr; }
    public void setOrigenUbicacionInfr(OrigenUbicacion origenUbicacionInfr) {
        this.origenUbicacionInfr = origenUbicacionInfr;
    }

    public boolean isSiUbicacionInfrManual() { return siUbicacionInfrManual; }
    public void setSiUbicacionInfrManual(boolean v) { this.siUbicacionInfrManual = v; }

    // =====================================================================
    // GETTERS DE TEXTO LIBRE DEL LUGAR DEL HECHO
    // =====================================================================

    public boolean isSiDomTxtInfr() { return siDomTxtInfr; }
    public void setSiDomTxtInfr(boolean siDomTxtInfr) { this.siDomTxtInfr = siDomTxtInfr; }

    public String getDomTxtInfr() { return domTxtInfr; }
    public void setDomTxtInfr(String domTxtInfr) { this.domTxtInfr = domTxtInfr; }

    // =====================================================================
    // GETTERS DE CONTEXTO GEOGRAFICO
    // =====================================================================

    public boolean isSiEjeUrb() { return siEjeUrb; }
    public void setSiEjeUrb(boolean siEjeUrb) { this.siEjeUrb = siEjeUrb; }

    // =====================================================================
    // GETTERS DE QR
    // =====================================================================

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }

    public Integer getQrPayloadVersion() { return qrPayloadVersion; }
    public void setQrPayloadVersion(Integer qrPayloadVersion) { this.qrPayloadVersion = qrPayloadVersion; }

    // =====================================================================
    // GETTERS DE ESTADO (TRIPLA CANONICA)
    // =====================================================================

    public BloqueActual getBloqueActual() { return bloqueActual; }
    public void setBloqueActual(BloqueActual bloqueActual) { this.bloqueActual = bloqueActual; }

    public EstadoProcesalActa getEstadoProcesal() { return estadoProcesal; }
    public void setEstadoProcesal(EstadoProcesalActa estadoProcesal) { this.estadoProcesal = estadoProcesal; }

    public SituacionAdministrativaActa getSituacionAdministrativa() { return situacionAdministrativa; }
    public void setSituacionAdministrativa(SituacionAdministrativaActa situacionAdministrativa) {
        this.situacionAdministrativa = situacionAdministrativa;
    }

    // =====================================================================
    // GETTERS DE RESULTADO Y FIRMA
    // =====================================================================

    public ResultadoFinalActa getResultadoFinal() { return resultadoFinal; }
    public void setResultadoFinal(ResultadoFinalActa resultadoFinal) { this.resultadoFinal = resultadoFinal; }

    public ResultadoFirmaInfractor getResultadoFirmaInfractor() { return resultadoFirmaInfractor; }

    // =====================================================================
    // GETTERS DE CACHES ARCHIVO/REINGRESO
    // =====================================================================

    public Long getIdMotivoArchivoActual() { return idMotivoArchivoActual; }
    public void setIdMotivoArchivoActual(Long idMotivoArchivoActual) {
        this.idMotivoArchivoActual = idMotivoArchivoActual;
    }

    public Boolean getPermiteReingreso() { return permiteReingreso; }
    public void setPermiteReingreso(Boolean permiteReingreso) { this.permiteReingreso = permiteReingreso; }

    public LocalDateTime getFhCierre() { return fhCierre; }
    public void setFhCierre(LocalDateTime fhCierre) { this.fhCierre = fhCierre; }

    public LocalDateTime getFhArchivo() { return fhArchivo; }
    public void setFhArchivo(LocalDateTime fhArchivo) { this.fhArchivo = fhArchivo; }

    // =====================================================================
    // GETTERS DE OCC Y AUDITORIA
    // =====================================================================

    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    // =====================================================================
    // METODOS DE ESTADO
    // =====================================================================

    /** Retorna true si el acta esta cerrada definitivamente (CERRADA o ANULADA). */
    public boolean estaCerrada() {
        return situacionAdministrativa == SituacionAdministrativaActa.CERRADA
                || situacionAdministrativa == SituacionAdministrativaActa.ANULADA;
    }

    /** Retorna true si el acta esta paralizada. */
    public boolean estaParalizada() {
        return situacionAdministrativa == SituacionAdministrativaActa.PARALIZADA;
    }

    /** Retorna true si el acta esta en gestion externa. */
    public boolean estaEnGestionExterna() {
        return situacionAdministrativa == SituacionAdministrativaActa.EN_GESTION_EXTERNA;
    }

    /** Retorna true si el acta esta archivada. */
    public boolean estaArchivada() {
        return situacionAdministrativa == SituacionAdministrativaActa.ARCHIVADA;
    }

    // =====================================================================
    // COPIA DEFENSIVA
    // =====================================================================

    public FalActa copia() {
        FalActa c = new FalActa(id, uuidTecnico, tipoActa, idDependencia, idInspector,
                fechaActa, fechaLabrado, domicilioHecho, observaciones,
                latInfr, lonInfr, resultadoFirmaInfractor, idPersonaInfractor,
                fhAlta, idUserAlta);
        c.nroActa = this.nroActa;
        c.idTalonario = this.idTalonario;
        c.nroTalonarioUsado = this.nroTalonarioUsado;
        c.origenCaptura = this.origenCaptura;
        c.idDispositivoCaptura = this.idDispositivoCaptura;
        c.idUserCaptura = this.idUserCaptura;
        c.fhCaptura = this.fhCaptura;
        c.latCaptura = this.latCaptura;
        c.lonCaptura = this.lonCaptura;
        c.precisionCapturaM = this.precisionCapturaM;
        c.fhPosCaptura = this.fhPosCaptura;
        c.origenPosCaptura = this.origenPosCaptura;
        c.verDep = this.verDep;
        c.verInsp = this.verInsp;
        c.idDomicilioInfractorAct = this.idDomicilioInfractorAct;
        c.idDomicilioNotifAct = this.idDomicilioNotifAct;
        c.infractorDocumento = this.infractorDocumento;
        c.infractorNombre = this.infractorNombre;
        c.resumenHecho = this.resumenHecho;
        c.idLocInfrMalvinas = this.idLocInfrMalvinas;
        c.localidadInfrMalvinasVersionId = this.localidadInfrMalvinasVersionId;
        c.idTcaInfrMalvinas = this.idTcaInfrMalvinas;
        c.calleInfrMalvinasVersionId = this.calleInfrMalvinasVersionId;
        c.alturaInfr = this.alturaInfr;
        c.alturaOrigenInfr = this.alturaOrigenInfr;
        c.siAlturaInfrEstimada = this.siAlturaInfrEstimada;
        c.origenUbicacionInfr = this.origenUbicacionInfr;
        c.siUbicacionInfrManual = this.siUbicacionInfrManual;
        c.siDomTxtInfr = this.siDomTxtInfr;
        c.domTxtInfr = this.domTxtInfr;
        c.siEjeUrb = this.siEjeUrb;
        c.codigoQr = this.codigoQr;
        c.qrPayloadVersion = this.qrPayloadVersion;
        c.bloqueActual = this.bloqueActual;
        c.estadoProcesal = this.estadoProcesal;
        c.situacionAdministrativa = this.situacionAdministrativa;
        c.resultadoFinal = this.resultadoFinal;
        c.idMotivoArchivoActual = this.idMotivoArchivoActual;
        c.permiteReingreso = this.permiteReingreso;
        c.fhCierre = this.fhCierre;
        c.fhArchivo = this.fhArchivo;
        c.versionRow = this.versionRow;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}
package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDateTime;

/**
 * Documento del expediente.
 *
 * Alineado con fal_documento MariaDB:
 * - id: BIGINT AUTO_INCREMENT
 * - tipoDocu: SMALLINT (catalogo tipo_docu) via TipoDocu
 * - estadoDocu: SMALLINT (catalogo estado_docu) via EstadoDocu
 * - nroDocu: VARCHAR(30) nullable
 * - storageKey: VARCHAR(196) nullable (clave tecnica interna; max 196 chars)
 * - idTalonario: BIGINT nullable
 * - nroTalonarioUsado: INT nullable
 * - tipoFirmaReq: SMALLINT (catalogo tipo_firma_req) via TipoFirmaReq
 * - plantillaId: BIGINT nullable
 *
 * rolFirmaReq y mecanismoFirmaReq NO viven en FalDocumento.
 * Pertenecen a fal_documento_firma_req (slice 8C-3).
 * La firma se delega a FalDocumentoFirma.
 *
 * Slice 8C-6B-1: agrega marcarFirmado().
 * Slice 8C-6C-1: agrega hashDocu, fhGeneracion, marcarEmitido(), estaEmitido().
 *   - fechaGeneracion mapea a fh_alta (fecha tecnica de creacion del registro).
 *   - fhGeneracion mapea a fh_generacion (fecha de emision formal del documento).
 *   - hashDocu mapea a hash_docu (hash del artefacto PDF emitido).
 * Slice 8C-6D-1: agrega crearAdjunto(), marcarFirmadoDesdeAdjunto(), estaAdjunto().
 * FULL-R1.2-CORRECCION-03: descripcion eliminado (HUMAN_DECISION_CLOSED).
 */
public class FalDocumento {

    private final Long id;
    private int versionRow;
    private final Long idActa;
    private final TipoDocu tipoDocu;
    private final LocalDateTime fechaGeneracion;
    /**
     * Clave técnica interna generada por backend.
     * Formato: {@code faltas/actas/<uuid-acta>/documentos/<uuid-documento>.pdf}
     * No es URL pública ni contiene el binario.
     * Nullable hasta que el artefacto sea emitido. Máximo 196 caracteres.
     */
    private String storageKey;
    private String hashDocu;
    private LocalDateTime fhGeneracion;
    private EstadoDocu estadoDocu;
    private String nroDocu;
    private TipoFirmaReq tipoFirmaReq;
    private Long plantillaId;
    private String idUserAlta;
    private Long idTalonario;
    private Integer nroTalonarioUsado;

    public FalDocumento(
            Long id,
            Long idActa,
            TipoDocu tipoDocu,
            LocalDateTime fechaGeneracion) {
        this.id = id;
        this.idActa = idActa;
        this.tipoDocu = tipoDocu;
        this.fechaGeneracion = fechaGeneracion;
        this.estadoDocu = EstadoDocu.PENDIENTE_FIRMA;
        this.tipoFirmaReq = TipoFirmaReq.NO_REQUIERE;
        this.versionRow = 0;
    }

    public FalDocumento(
            Long id,
            Long idActa,
            TipoDocu tipoDocu,
            LocalDateTime fechaGeneracion,
            EstadoDocu estadoDocu,
            TipoFirmaReq tipoFirmaReq,
            Long plantillaId,
            LocalDateTime fhAlta) {
        this.id = id;
        this.idActa = idActa;
        this.tipoDocu = tipoDocu;
        this.fechaGeneracion = fechaGeneracion;
        this.estadoDocu = estadoDocu;
        this.tipoFirmaReq = tipoFirmaReq;
        this.plantillaId = plantillaId;
        this.versionRow = 0;
    }

    public FalDocumento(
            Long id,
            Long idActa,
            TipoDocu tipoDocu,
            LocalDateTime fechaGeneracion,
            EstadoDocu estadoDocu,
            TipoFirmaReq tipoFirmaReq,
            Long plantillaId,
            String idUserAlta) {
        this(id, idActa, tipoDocu, fechaGeneracion, estadoDocu, tipoFirmaReq, plantillaId, fechaGeneracion);
        this.idUserAlta = idUserAlta;
    }

    /**
     * Factory para documento adjunto/escaneado externo (Slice 8C-6D-1).
     *
     * El documento externo:
     * - comienza en EstadoDocu.ADJUNTO.
     * - no se genera desde plantilla obligatoriamente (plantillaId puede ser null).
     * - no se numera automaticamente.
     * - no se emite automaticamente.
     * - storageKey, hashDocu y fhGeneracion son obligatorios.
     */
    public static FalDocumento crearAdjunto(
            Long id,
            Long idActa,
            TipoDocu tipoDocu,
            String storageKey,
            String hashDocu,
            LocalDateTime fhGeneracion,
            Long plantillaId,
            LocalDateTime fhAlta) {
        if (id == null) throw new IllegalArgumentException("id requerido para documento adjunto");
        if (idActa == null) throw new IllegalArgumentException("idActa requerido para documento adjunto");
        if (tipoDocu == null) throw new IllegalArgumentException("tipoDocu requerido para documento adjunto");
        if (storageKey == null || storageKey.isBlank())
            throw new IllegalArgumentException("storageKey requerido para documento adjunto");
        if (hashDocu == null || hashDocu.isBlank())
            throw new IllegalArgumentException("hashDocu requerido para documento adjunto");
        if (fhGeneracion == null)
            throw new IllegalArgumentException("fhGeneracion requerido para documento adjunto");

        FalDocumento doc = new FalDocumento(
                id, idActa, tipoDocu, fhAlta,
                EstadoDocu.ADJUNTO, TipoFirmaReq.NO_REQUIERE, plantillaId, fhAlta);
        doc.storageKey = storageKey;
        doc.hashDocu = hashDocu;
        doc.fhGeneracion = fhGeneracion;
        return doc;
    }

    /**
     * Transicion formal a EMITIDO (Slice 8C-6C-1).
     * La validacion de estado origen vive en DocumentoService.
     */
    public void marcarEmitido(String storageKey, String hashDocu, LocalDateTime fhGeneracion) {
        if (estadoDocu == EstadoDocu.EMITIDO) {
            throw new PrecondicionVioladaException("El documento ya esta en estado EMITIDO.");
        }
        if (estadoDocu == EstadoDocu.ANULADO) {
            throw new PrecondicionVioladaException("No se puede emitir un documento ANULADO.");
        }
        if (estadoDocu == EstadoDocu.REEMPLAZADO) {
            throw new PrecondicionVioladaException("No se puede emitir un documento REEMPLAZADO.");
        }
        this.storageKey = storageKey;
        this.hashDocu = hashDocu;
        this.fhGeneracion = fhGeneracion;
        this.estadoDocu = EstadoDocu.EMITIDO;
    }

    /**
     * Transicion PENDIENTE_FIRMA -> FIRMADO cuando todos los requisitos obligatorios activos estan firmados.
     * Solo se permite desde PENDIENTE_FIRMA.
     */
    public void marcarFirmado() {
        if (estadoDocu != EstadoDocu.PENDIENTE_FIRMA) {
            throw new PrecondicionVioladaException(
                    "El documento no esta en estado PENDIENTE_FIRMA. Estado actual: " + estadoDocu);
        }
        this.estadoDocu = EstadoDocu.FIRMADO;
    }

    /**
     * Transicion ADJUNTO -> FIRMADO cuando todos los requisitos obligatorios activos estan firmados.
     * Solo se permite desde ADJUNTO (Slice 8C-6D-1).
     */
    public void marcarFirmadoDesdeAdjunto() {
        if (estadoDocu != EstadoDocu.ADJUNTO) {
            throw new PrecondicionVioladaException(
                    "El documento no esta en estado ADJUNTO. Estado actual: " + estadoDocu);
        }
        this.estadoDocu = EstadoDocu.FIRMADO;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getIdActa() { return idActa; }
    public TipoDocu getTipoDocu() { return tipoDocu; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public EstadoDocu getEstadoDocu() { return estadoDocu; }
    public void setEstadoDocu(EstadoDocu estadoDocu) { this.estadoDocu = estadoDocu; }

    public String getNroDocu() { return nroDocu; }
    public void setNroDocu(String nroDocu) { this.nroDocu = nroDocu; }

    public TipoFirmaReq getTipoFirmaReq() { return tipoFirmaReq; }
    public void setTipoFirmaReq(TipoFirmaReq tipoFirmaReq) { this.tipoFirmaReq = tipoFirmaReq; }

    public Long getPlantillaId() { return plantillaId; }
    public void setPlantillaId(Long plantillaId,
            LocalDateTime fhAlta) { this.plantillaId = plantillaId; }

    public Long getIdTalonario() { return idTalonario; }
    public void setIdTalonario(Long idTalonario) { this.idTalonario = idTalonario; }

    public Integer getNroTalonarioUsado() { return nroTalonarioUsado; }
    public void setNroTalonarioUsado(Integer nroTalonarioUsado) { this.nroTalonarioUsado = nroTalonarioUsado; }

    public String getIdUserAlta() { return idUserAlta; }
    public void setIdUserAlta(String idUserAlta) { this.idUserAlta = idUserAlta; }
    public boolean esBorrador() { return estadoDocu == EstadoDocu.BORRADOR; }
    public String getHashDocu() { return hashDocu; }
    public void setHashDocu(String hashDocu) { this.hashDocu = hashDocu; }

    public LocalDateTime getFhGeneracion() { return fhGeneracion; }
    public void setFhGeneracion(LocalDateTime fhGeneracion) { this.fhGeneracion = fhGeneracion; }

    public boolean estaEmitido() { return estadoDocu == EstadoDocu.EMITIDO; }
    public boolean estaFirmado() { return estadoDocu == EstadoDocu.FIRMADO; }
    public boolean pendienteFirma() { return estadoDocu == EstadoDocu.PENDIENTE_FIRMA; }
    public boolean estaAdjunto() { return estadoDocu == EstadoDocu.ADJUNTO; }

    public FalDocumento copia() {
        FalDocumento c = new FalDocumento(id, idActa, tipoDocu, fechaGeneracion,
                estadoDocu, tipoFirmaReq, plantillaId, fechaGeneracion);
        c.versionRow = this.versionRow;
        c.storageKey = this.storageKey;
        c.hashDocu = this.hashDocu;
        c.fhGeneracion = this.fhGeneracion;
        c.nroDocu = this.nroDocu;
        c.idUserAlta = this.idUserAlta;
        c.idTalonario = this.idTalonario;
        c.nroTalonarioUsado = this.nroTalonarioUsado;
        return c;
    }
}

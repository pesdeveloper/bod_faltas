package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domicilio de una persona asociada a un acta de faltas.
 * Mapeado a fal_persona_domicilio en MariaDB.
 *
 * Reglas principales:
 * - personaId obligatorio; la persona debe existir.
 * - actaOrigenId opcional; indica en que acta nacio el domicilio.
 * - MALVINAS_LOCAL: idProvincia=6, unidadTerritorialTipo=MUNICIPIO, idUnidadTerritorial=60515.
 *   idLocMalvinas e idTcaMalvinas conservan texto con ceros/caracteres (no convertir a numero).
 *   idLocalidad e idCalle deben quedar null.
 * - EXTERNO: idLocMalvinas, localidadMalvinasVersionId, idTcaMalvinas, calleMalvinasVersionId null.
 * - siSinAltura=true exige altura=null; altura!=null exige siSinAltura=false.
 * - lat y lon en pareja; si hay coordenadas, origenUbicacion obligatorio y != SIN_UBICACION.
 * - Puede existir como maximo un domicilio activo y principal por persona y TipoDomicilio.
 * - No borrar fisicamente: baja logica via siActivo=false.
 *
 * No tiene versionRow: fal_persona_domicilio no lo define en MariaDB.
 */
public class FalPersonaDomicilio {

    private final Long id;
    private final Long personaId;
    private final Long actaOrigenId;

    private final TipoDomicilio tipoDomicilio;
    private final OrigenDomicilio origenDomicilio;
    private final ModoDomicilio modoDomicilio;

    private boolean siActivo;
    private boolean siNotificable;
    private boolean siPrincipal;

    private final Short idProvincia;
    private final UnidadTerritorialTipo unidadTerritorialTipo;
    private final Integer idUnidadTerritorial;
    private final Long idLocalidad;
    private final Long idCalle;

    private final String idLocMalvinas;
    private final Long localidadMalvinasVersionId;
    private final String idTcaMalvinas;
    private final Long calleMalvinasVersionId;

    private final String calleTxt;
    private final Integer altura;
    private final boolean siSinAltura;
    private final String unidadFuncional;
    private final String codigoPostal;
    private String domicilioTxt;
    private final String validacionDomicilio;
    private final boolean siNormalizadoParcial;

    private final BigDecimal lat;
    private final BigDecimal lon;
    private final OrigenUbicacion origenUbicacion;

    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalPersonaDomicilio(
            Long id,
            Long personaId,
            Long actaOrigenId,
            TipoDomicilio tipoDomicilio,
            OrigenDomicilio origenDomicilio,
            ModoDomicilio modoDomicilio,
            boolean siActivo,
            boolean siNotificable,
            boolean siPrincipal,
            Short idProvincia,
            UnidadTerritorialTipo unidadTerritorialTipo,
            Integer idUnidadTerritorial,
            Long idLocalidad,
            Long idCalle,
            String idLocMalvinas,
            Long localidadMalvinasVersionId,
            String idTcaMalvinas,
            Long calleMalvinasVersionId,
            String calleTxt,
            Integer altura,
            boolean siSinAltura,
            String unidadFuncional,
            String codigoPostal,
            String domicilioTxt,
            String validacionDomicilio,
            boolean siNormalizadoParcial,
            BigDecimal lat,
            BigDecimal lon,
            OrigenUbicacion origenUbicacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio en FalPersonaDomicilio");
        if (personaId == null) throw new IllegalArgumentException("personaId es obligatorio");
        if (tipoDomicilio == null) throw new IllegalArgumentException("tipoDomicilio es obligatorio");
        if (origenDomicilio == null) throw new IllegalArgumentException("origenDomicilio es obligatorio");
        if (modoDomicilio == null) throw new IllegalArgumentException("modoDomicilio es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatorio");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");

        this.id = id;
        this.personaId = personaId;
        this.actaOrigenId = actaOrigenId;
        this.tipoDomicilio = tipoDomicilio;
        this.origenDomicilio = origenDomicilio;
        this.modoDomicilio = modoDomicilio;
        this.siActivo = siActivo;
        this.siNotificable = siNotificable;
        this.siPrincipal = siPrincipal;
        this.idProvincia = idProvincia;
        this.unidadTerritorialTipo = unidadTerritorialTipo;
        this.idUnidadTerritorial = idUnidadTerritorial;
        this.idLocalidad = idLocalidad;
        this.idCalle = idCalle;
        this.idLocMalvinas = idLocMalvinas;
        this.localidadMalvinasVersionId = localidadMalvinasVersionId;
        this.idTcaMalvinas = idTcaMalvinas;
        this.calleMalvinasVersionId = calleMalvinasVersionId;
        this.calleTxt = calleTxt;
        this.altura = altura;
        this.siSinAltura = siSinAltura;
        this.unidadFuncional = unidadFuncional;
        this.codigoPostal = codigoPostal;
        this.domicilioTxt = domicilioTxt;
        this.validacionDomicilio = validacionDomicilio;
        this.siNormalizadoParcial = siNormalizadoParcial;
        this.lat = lat;
        this.lon = lon;
        this.origenUbicacion = origenUbicacion;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.fhUltMod = fhAlta;
    }

    public Long getId() { return id; }
    public Long getPersonaId() { return personaId; }
    public Long getActaOrigenId() { return actaOrigenId; }
    public TipoDomicilio getTipoDomicilio() { return tipoDomicilio; }
    public OrigenDomicilio getOrigenDomicilio() { return origenDomicilio; }
    public ModoDomicilio getModoDomicilio() { return modoDomicilio; }
    public boolean isSiActivo() { return siActivo; }
    public boolean isSiNotificable() { return siNotificable; }
    public boolean isSiPrincipal() { return siPrincipal; }
    public Short getIdProvincia() { return idProvincia; }
    public UnidadTerritorialTipo getUnidadTerritorialTipo() { return unidadTerritorialTipo; }
    public Integer getIdUnidadTerritorial() { return idUnidadTerritorial; }
    public Long getIdLocalidad() { return idLocalidad; }
    public Long getIdCalle() { return idCalle; }
    public String getIdLocMalvinas() { return idLocMalvinas; }
    public Long getLocalidadMalvinasVersionId() { return localidadMalvinasVersionId; }
    public String getIdTcaMalvinas() { return idTcaMalvinas; }
    public Long getCalleMalvinasVersionId() { return calleMalvinasVersionId; }
    public String getCalleTxt() { return calleTxt; }
    public Integer getAltura() { return altura; }
    public boolean isSiSinAltura() { return siSinAltura; }
    public String getUnidadFuncional() { return unidadFuncional; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getDomicilioTxt() { return domicilioTxt; }
    public String getValidacionDomicilio() { return validacionDomicilio; }
    public boolean isSiNormalizadoParcial() { return siNormalizadoParcial; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLon() { return lon; }
    public OrigenUbicacion getOrigenUbicacion() { return origenUbicacion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }

    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public void setSiNotificable(boolean siNotificable) { this.siNotificable = siNotificable; }
    public void setSiPrincipal(boolean siPrincipal) { this.siPrincipal = siPrincipal; }
    public void setDomicilioTxt(String domicilioTxt) { this.domicilioTxt = domicilioTxt; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public boolean esMalvinasLocal() { return modoDomicilio == ModoDomicilio.MALVINAS_LOCAL; }
    public boolean esExterno() { return modoDomicilio == ModoDomicilio.EXTERNO; }
    public boolean tieneCoordenadas() { return lat != null && lon != null; }

    public FalPersonaDomicilio copia() {
        FalPersonaDomicilio c = new FalPersonaDomicilio(
            id, personaId, actaOrigenId, tipoDomicilio, origenDomicilio, modoDomicilio,
            siActivo, siNotificable, siPrincipal,
            idProvincia, unidadTerritorialTipo, idUnidadTerritorial, idLocalidad, idCalle,
            idLocMalvinas, localidadMalvinasVersionId, idTcaMalvinas, calleMalvinasVersionId,
            calleTxt, altura, siSinAltura, unidadFuncional, codigoPostal, domicilioTxt,
            validacionDomicilio, siNormalizadoParcial, lat, lon, origenUbicacion, fhAlta, idUserAlta);
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}

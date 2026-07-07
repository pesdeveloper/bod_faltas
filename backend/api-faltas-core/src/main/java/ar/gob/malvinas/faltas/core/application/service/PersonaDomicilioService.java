package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.exception.DomicilioPersonaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de gestion de domicilios de personas.
 *
 * Invariantes:
 * - personaId debe referenciar persona existente.
 * - MALVINAS_LOCAL: idProvincia=6, tipo=MUNICIPIO, idUnidadTerritorial=60515.
 * - EXTERNO: campos locales quedan null.
 * - siSinAltura=true exige altura=null.
 * - lat y lon en pareja; coordenadas exigen origenUbicacion != null ni SIN_UBICACION.
 * - Un domicilio activo y principal por persona+TipoDomicilio.
 * - Baja logica via siActivo=false (no borrar fisicamente).
 * - Corrección en misma fila solo si el domicilio no fue usado formalmente.
 */
@Service
public class PersonaDomicilioService {

    private static final Short ID_PROVINCIA_MALVINAS = (short) 6;
    private static final Integer ID_UNIDAD_TERRITORIAL_MALVINAS = 60515;

    private final PersonaDomicilioRepository domicilioRepository;
    private final PersonaRepository personaRepository;
    private final PersonaDomicilioUsoChecker usoChecker;

    public PersonaDomicilioService(
            PersonaDomicilioRepository domicilioRepository,
            PersonaRepository personaRepository,
            PersonaDomicilioUsoChecker usoChecker) {
        this.domicilioRepository = domicilioRepository;
        this.personaRepository = personaRepository;
        this.usoChecker = usoChecker;
    }

    /**
     * Crea un nuevo domicilio para la persona.
     */
    public FalPersonaDomicilio crear(
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
            String idUserAlta) {

        if (!personaRepository.buscarPorId(personaId).isPresent())
            throw new PersonaNoEncontradaException(personaId);

        Long id = domicilioRepository.nextId();
        LocalDateTime ahora = LocalDateTime.now();

        validarReglasDomicilio(modoDomicilio, idProvincia, unidadTerritorialTipo, idUnidadTerritorial,
                idLocalidad, idCalle, idLocMalvinas, idTcaMalvinas,
                altura, siSinAltura, lat, lon, origenUbicacion,
                calleTxt, domicilioTxt, siNormalizadoParcial);

        FalPersonaDomicilio domicilio = new FalPersonaDomicilio(
                id, personaId, actaOrigenId,
                tipoDomicilio, origenDomicilio, modoDomicilio,
                siActivo, siNotificable, siPrincipal,
                idProvincia, unidadTerritorialTipo, idUnidadTerritorial, idLocalidad, idCalle,
                idLocMalvinas, localidadMalvinasVersionId, idTcaMalvinas, calleMalvinasVersionId,
                calleTxt, altura, siSinAltura, unidadFuncional, codigoPostal, domicilioTxt,
                validacionDomicilio, siNormalizadoParcial, lat, lon, origenUbicacion, ahora, idUserAlta);

        FalPersonaDomicilio guardado = domicilioRepository.guardar(domicilio);

        if (siPrincipal && siActivo) {
            if (domicilioRepository instanceof ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository repo) {
                repo.marcarPrincipal(guardado.getId());
            }
        }

        return domicilioRepository.buscarPorId(guardado.getId()).orElse(guardado);
    }

    /**
     * Corrección de domicilio. Si ya fue usado formalmente, crea nueva fila.
     */
    public FalPersonaDomicilio corregir(Long domicilioId, String domicilioTxtNuevo, String idUserMod) {
        FalPersonaDomicilio dom = domicilioRepository.buscarPorId(domicilioId)
                .orElseThrow(() -> new DomicilioPersonaNoEncontradoException(domicilioId));

        if (usoChecker.estaUsadoFormalmente(domicilioId)) {
            Long id = domicilioRepository.nextId();
            LocalDateTime ahora = LocalDateTime.now();
            FalPersonaDomicilio nueva = new FalPersonaDomicilio(
                    id, dom.getPersonaId(), dom.getActaOrigenId(),
                    dom.getTipoDomicilio(), dom.getOrigenDomicilio(), dom.getModoDomicilio(),
                    true, dom.isSiNotificable(), false,
                    dom.getIdProvincia(), dom.getUnidadTerritorialTipo(), dom.getIdUnidadTerritorial(),
                    dom.getIdLocalidad(), dom.getIdCalle(),
                    dom.getIdLocMalvinas(), dom.getLocalidadMalvinasVersionId(),
                    dom.getIdTcaMalvinas(), dom.getCalleMalvinasVersionId(),
                    dom.getCalleTxt(), dom.getAltura(), dom.isSiSinAltura(),
                    dom.getUnidadFuncional(), dom.getCodigoPostal(), domicilioTxtNuevo,
                    dom.getValidacionDomicilio(), dom.isSiNormalizadoParcial(),
                    dom.getLat(), dom.getLon(), dom.getOrigenUbicacion(), ahora, idUserMod);
            return domicilioRepository.guardar(nueva);
        }

        dom.setDomicilioTxt(domicilioTxtNuevo);
        dom.setFhUltMod(LocalDateTime.now());
        dom.setIdUserUltMod(idUserMod);
        return domicilioRepository.guardar(dom);
    }

    public FalPersonaDomicilio desactivar(Long domicilioId, String idUserMod) {
        FalPersonaDomicilio dom = domicilioRepository.buscarPorId(domicilioId)
                .orElseThrow(() -> new DomicilioPersonaNoEncontradoException(domicilioId));
        dom.setSiActivo(false);
        dom.setSiPrincipal(false);
        dom.setFhUltMod(LocalDateTime.now());
        dom.setIdUserUltMod(idUserMod);
        return domicilioRepository.guardar(dom);
    }

    public void marcarPrincipal(Long domicilioId) {
        FalPersonaDomicilio dom = domicilioRepository.buscarPorId(domicilioId)
                .orElseThrow(() -> new DomicilioPersonaNoEncontradoException(domicilioId));
        if (!dom.isSiActivo()) throw new IllegalStateException("Domicilio inactivo no puede ser principal");
        if (domicilioRepository instanceof ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository repo) {
            repo.marcarPrincipal(domicilioId);
        } else {
            dom.setSiPrincipal(true);
            domicilioRepository.guardar(dom);
        }
    }

    public List<FalPersonaDomicilio> listarActivos(Long personaId) {
        return domicilioRepository.buscarActivosPorPersonaId(personaId);
    }

    public List<FalPersonaDomicilio> listarNotificables(Long personaId) {
        return domicilioRepository.buscarNotificablesPorPersonaId(personaId);
    }

    public Optional<FalPersonaDomicilio> buscarPrincipalActivo(Long personaId, TipoDomicilio tipoDomicilio) {
        return domicilioRepository.buscarPrincipalActivo(personaId, tipoDomicilio);
    }

    // --- Validaciones ---

    private void validarReglasDomicilio(
            ModoDomicilio modo,
            Short idProvincia, UnidadTerritorialTipo utTipo, Integer idUT,
            Long idLocalidad, Long idCalle,
            String idLocMalvinas, String idTcaMalvinas,
            Integer altura, boolean siSinAltura,
            BigDecimal lat, BigDecimal lon, OrigenUbicacion origenUbicacion,
            String calleTxt, String domicilioTxt, boolean siNormalizadoParcial) {

        if (modo == ModoDomicilio.MALVINAS_LOCAL) {
            if (!ID_PROVINCIA_MALVINAS.equals(idProvincia))
                throw new IllegalArgumentException("MALVINAS_LOCAL requiere idProvincia=6");
            if (utTipo != UnidadTerritorialTipo.MUNICIPIO)
                throw new IllegalArgumentException("MALVINAS_LOCAL requiere unidadTerritorialTipo=MUNICIPIO");
            if (!ID_UNIDAD_TERRITORIAL_MALVINAS.equals(idUT))
                throw new IllegalArgumentException("MALVINAS_LOCAL requiere idUnidadTerritorial=60515");
            if (idLocalidad != null)
                throw new IllegalArgumentException("MALVINAS_LOCAL no admite idLocalidad externo");
            if (idCalle != null)
                throw new IllegalArgumentException("MALVINAS_LOCAL no admite idCalle externo");
        }

        if (modo == ModoDomicilio.EXTERNO) {
            if (idLocMalvinas != null || idTcaMalvinas != null)
                throw new IllegalArgumentException("EXTERNO no admite campos locales Malvinas");
        }

        if (siSinAltura && altura != null)
            throw new IllegalArgumentException("siSinAltura=true exige altura=null");
        if (altura != null && siSinAltura)
            throw new IllegalArgumentException("altura!=null exige siSinAltura=false");
        if (altura != null && altura < 0)
            throw new IllegalArgumentException("altura no puede ser negativa");

        if ((lat != null) != (lon != null))
            throw new IllegalArgumentException("lat y lon deben informarse juntos");

        if (lat != null) {
            if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0)
                throw new IllegalArgumentException("lat fuera de rango [-90, 90]");
            if (lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0)
                throw new IllegalArgumentException("lon fuera de rango [-180, 180]");
            if (origenUbicacion == null || origenUbicacion == OrigenUbicacion.SIN_UBICACION)
                throw new IllegalArgumentException("Coordenadas requieren origenUbicacion valido");
        }

        if (!siNormalizadoParcial) return;

        boolean tieneAlgo = calleTxt != null || domicilioTxt != null
                || idLocalidad != null || idCalle != null
                || idLocMalvinas != null
                || (lat != null);
        if (!tieneAlgo)
            throw new IllegalArgumentException("Domicilio parcial debe tener al menos un campo de texto o referencia");
    }
}

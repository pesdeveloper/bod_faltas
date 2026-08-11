package ar.gob.malvinas.faltas.core.geo.application;

import ar.gob.malvinas.faltas.core.geo.repository.GeoCatalogMariaDbRepository;
import ar.gob.malvinas.faltas.core.geo.repository.GeoRepository;
import ar.gob.malvinas.faltas.core.geo.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Servicio GEO unificado para Faltas.
 *
 * Fuentes de datos exclusivamente locales — sin servicios externos:
 *   - MALVINAS_LOCAL: GeoRepository (PostgreSQL/PostGIS). Calles y localidades del municipio.
 *   - EXTERNO_IGN_INDEC: GeoCatalogMariaDbRepository (MariaDB). Catálogos IGN/INDEC/BAHRA.
 *   - Geocodificación y reverse: siempre vía PostGIS local.
 *
 * Reglas:
 *   - No llamar a ninguna API externa (GeoRef, Google Maps, IGN remoto, INDEC remoto).
 *   - EXTERNO_IGN_INDEC significa domicilio fuera del municipio; los datos vienen de MariaDB local.
 *   - Si una fuente no está disponible, devolver lista vacía (nunca lanzar excepción al cliente).
 *   - PostGIS deshabilitado no rompe catálogos MariaDB; MariaDB deshabilitado no rompe PostGIS.
 *
 * GEO-MODEL-002 / E4-CORRECCION (sin GeoRef).
 */
@Service
public class GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoService.class);

    private final ObjectProvider<GeoRepository> geoRepositoryProvider;
    private final ObjectProvider<GeoCatalogMariaDbRepository> catalogProvider;

    public GeoService(
            ObjectProvider<GeoRepository> geoRepositoryProvider,
            ObjectProvider<GeoCatalogMariaDbRepository> catalogProvider) {
        this.geoRepositoryProvider = geoRepositoryProvider;
        this.catalogProvider = catalogProvider;
    }

    // ─────────────────── Malvinas Local (PostGIS) ───────────────────────────

    public List<LocalidadGeoDto> getLocalidadesMalvinas() {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) {
            log.debug("PostGIS no disponible; localidades Malvinas devuelve lista vacía");
            return Collections.emptyList();
        }
        return repo.findLocalidades();
    }

    public List<CalleGeoDto> getCallesMalvinas(String query, String idLoc) {
        return getCallesMalvinas(query, idLoc, null);
    }

    public List<CalleGeoDto> getCallesMalvinas(String query, String idLoc, Integer altura) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) {
            log.debug("PostGIS no disponible; calles Malvinas devuelve lista vacía");
            return Collections.emptyList();
        }
        return repo.findCalles(query, idLoc, altura);
    }

    // ─────────────────── Catálogos IGN/INDEC (MariaDB) ──────────────────────

    public List<ProvinciaGeoDto> getProvincias() {
        GeoCatalogMariaDbRepository catalog = catalogProvider.getIfAvailable();
        if (catalog == null) {
            log.debug("Catálogo MariaDB no disponible; provincias devuelve lista vacía");
            return Collections.emptyList();
        }
        return catalog.findProvincias();
    }

    public List<UnidadTerritorialGeoDto> getMunicipios(long provinciaId) {
        GeoCatalogMariaDbRepository catalog = catalogProvider.getIfAvailable();
        if (catalog == null) {
            log.debug("Catálogo MariaDB no disponible; municipios devuelve lista vacía");
            return Collections.emptyList();
        }
        return catalog.findMunicipiosYDepartamentos(provinciaId);
    }

    public List<LocalidadGeoDto> getLocalidades(long unidadTerritorialId) {
        GeoCatalogMariaDbRepository catalog = catalogProvider.getIfAvailable();
        if (catalog == null) {
            log.debug("Catálogo MariaDB no disponible; localidades externas devuelven lista vacía");
            return Collections.emptyList();
        }
        return catalog.findLocalidades(unidadTerritorialId);
    }

    public List<CalleGeoDto> getCallesExternas(String query, String localidadId) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        GeoCatalogMariaDbRepository catalog = catalogProvider.getIfAvailable();
        if (catalog == null) {
            log.debug("Catálogo MariaDB no disponible; calles externas devuelven lista vacía");
            return Collections.emptyList();
        }
        return catalog.findCallesExternas(query, localidadId);
    }

    // ─────────────────── GIS / Geocodificación (PostGIS) ────────────────────

    public List<GeocodeCandidato> geocodificarCalleAltura(String idTca, String idLoc, int altura) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return Collections.emptyList();
        return repo.geocodificarCalleAltura(idTca, idLoc, altura);
    }

    public List<GeocodeCandidato> reversePoint(double lat, double lng) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return Collections.emptyList();
        return repo.reversePoint(lat, lng);
    }

    public List<GeocodeCandidato> geocodificarEsquina(
            String idTca1, String idLoc1, String idTca2, String idLoc2) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return Collections.emptyList();
        return repo.geocodificarEsquina(idTca1, idLoc1, idTca2, idLoc2);
    }

    public java.util.Optional<ar.gob.malvinas.faltas.core.geo.web.dto.ParcelaDetalleDto> getParcelaDetalle(
            String circ, String secc, String quint, String fracc, String manz, String parc) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return java.util.Optional.empty();
        return repo.findParcelaDetalle(circ, secc, quint, fracc, manz, parc);
    }

    public java.util.Optional<String> getParcelaGeom(
            String circ, String secc, String quint, String fracc, String manz, String parc) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return java.util.Optional.empty();
        return repo.findParcelaGeom(circ, secc, quint, fracc, manz, parc);
    }

    public List<GeocodeCandidato> geocodificarEntreCalles(
            String idTcaObj, String idLocObj,
            String idTcaL1, String idLocL1,
            String idTcaL2, String idLocL2) {
        GeoRepository repo = geoRepositoryProvider.getIfAvailable();
        if (repo == null) return Collections.emptyList();
        return repo.geocodificarEntreCalles(idTcaObj, idLocObj, idTcaL1, idLocL1, idTcaL2, idLocL2);
    }
}

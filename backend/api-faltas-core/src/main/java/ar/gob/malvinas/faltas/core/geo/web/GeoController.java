package ar.gob.malvinas.faltas.core.geo.web;

import ar.gob.malvinas.faltas.core.geo.application.GeoService;
import ar.gob.malvinas.faltas.core.geo.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para capacidades GEO de Faltas.
 * Base path: /api/geo
 *
 * Endpoints públicos (no requieren JWT): todos los de este controller.
 * Reutilizables por: prototipo Angular, formulario web de acta, app móvil.
 *
 * Fuentes exclusivamente locales — sin dependencia de servicios externos:
 *  - /api/geo/local/malvinas/*          → catálogo local PostGIS (MALVINAS_LOCAL)
 *  - /api/geo/provincias                → MariaDB IGN (geo_ign_provincia)
 *  - /api/geo/unidades-territoriales    → MariaDB IGN (vw_geo_municipio_departamento)
 *  - /api/geo/localidades               → MariaDB INDEC (geo_indec_localidad_censal)
 *  - /api/geo/calles                    → MariaDB INDEC (geo_indec_calles, EXTERNO_IGN_INDEC)
 *  - /api/geo/geocodificar              → geocodificación calle+altura (PostGIS)
 *  - /api/geo/reverse                   → reverse geocoding (PostGIS)
 *  - /api/geo/geocodificar-esquina      → esquina (PostGIS)
 *  - /api/geo/geocodificar-entre-calles → entre calles (PostGIS)
 *
 * GEO-MODEL-002 / E4-CORRECCION (sin GeoRef).
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    // ──────────────────────── Catálogo local Malvinas ────────────────────────────

    /**
     * Lista todas las localidades del municipio de Malvinas Argentinas.
     * GET /api/geo/local/malvinas/localidades
     */
    @GetMapping("/local/malvinas/localidades")
    public ResponseEntity<List<LocalidadGeoDto>> getLocalidadesMalvinas() {
        return ResponseEntity.ok(geoService.getLocalidadesMalvinas());
    }

    /**
     * Búsqueda de calles en catálogo local de Malvinas Argentinas.
     * Siempre devuelve localidad visible en cada sugerencia.
     * GET /api/geo/local/malvinas/calles?q=SAN&idLoc=LP
     *
     * @param q     texto de búsqueda (mínimo 2 caracteres)
     * @param idLoc código de localidad (opcional; sin él busca en todo Malvinas)
     */
    @GetMapping("/local/malvinas/calles")
    public ResponseEntity<List<CalleGeoDto>> getCallesMalvinas(
            @RequestParam String q,
            @RequestParam(required = false) String idLoc,
            @RequestParam(required = false) Integer altura) {
        return ResponseEntity.ok(geoService.getCallesMalvinas(q, idLoc, altura));
    }

    // ──────────────────────── Catálogo IGN/INDEC (EXTERNO) ───────────────────────

    /**
     * Lista provincias argentinas desde MariaDB IGN (geo_ign_provincia).
     * GET /api/geo/provincias
     */
    @GetMapping("/provincias")
    public ResponseEntity<List<ProvinciaGeoDto>> getProvincias() {
        return ResponseEntity.ok(geoService.getProvincias());
    }

    /**
     * Lista municipios y departamentos de una provincia desde MariaDB IGN.
     * GET /api/geo/unidades-territoriales?provinciaId=6
     */
    @GetMapping("/unidades-territoriales")
    public ResponseEntity<List<UnidadTerritorialGeoDto>> getUnidadesTeritoriales(
            @RequestParam long provinciaId) {
        return ResponseEntity.ok(geoService.getMunicipios(provinciaId));
    }

    /**
     * Lista localidades censales de un municipio/departamento desde MariaDB INDEC.
     * GET /api/geo/localidades?unidadTerritorialId=60515
     */
    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadGeoDto>> getLocalidades(
            @RequestParam long unidadTerritorialId) {
        return ResponseEntity.ok(geoService.getLocalidades(unidadTerritorialId));
    }

    /**
     * Búsqueda de calles externas desde MariaDB INDEC (geo_indec_calles).
     * Modo EXTERNO_IGN_INDEC: domicilios fuera del municipio de Malvinas Argentinas.
     * GET /api/geo/calles?q=SAN&localidadId=060515010
     */
    @GetMapping("/calles")
    public ResponseEntity<List<CalleGeoDto>> getCallesExternas(
            @RequestParam String q,
            @RequestParam(required = false) String localidadId) {
        return ResponseEntity.ok(geoService.getCallesExternas(q, localidadId));
    }

    // ──────────────────────── Detalle de parcela (PostGIS) ──────────────────────

    /**
     * Detalle de una parcela catastral: nomenclatura, dirección, inmuebles y comercios.
     * Uso: inspector visual de GeoDomicilios (no forma parte del flujo de actas).
     * GET /api/geo/local/malvinas/parcela/detalle?circ=4&secc=H&quint=0&fracc=0&manz=105B&parc=13
     */
    @GetMapping("/local/malvinas/parcela/detalle")
    public ResponseEntity<ParcelaDetalleDto> getParcelaDetalle(
            @RequestParam String circ,
            @RequestParam String secc,
            @RequestParam(defaultValue = "0") String quint,
            @RequestParam(defaultValue = "0") String fracc,
            @RequestParam String manz,
            @RequestParam String parc) {
        return geoService.getParcelaDetalle(circ, secc, quint, fracc, manz, parc)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ──────────────────────── Geocodificación (PostGIS) ─────────────────────────

    /**
     * Geocodifica calle + altura.
     * Devuelve lista de candidatos (puede ser vacía si no hay tramos para esa altura).
     * POST /api/geo/geocodificar
     */
    @PostMapping("/geocodificar")
    public ResponseEntity<List<GeocodeCandidato>> geocodificarCalleAltura(
            @Valid @RequestBody GeocodeCalleAlturaRequest req) {
        List<GeocodeCandidato> candidatos =
                geoService.geocodificarCalleAltura(req.idTca(), req.idLoc(), req.altura());
        return ResponseEntity.ok(candidatos);
    }

    /**
     * Reverse geocoding: punto → calles cercanas con localidad.
     * POST /api/geo/reverse
     */
    @PostMapping("/reverse")
    public ResponseEntity<List<GeocodeCandidato>> reversePoint(
            @Valid @RequestBody ReversePointRequest req) {
        List<GeocodeCandidato> candidatos =
                geoService.reversePoint(req.lat(), req.lng());
        return ResponseEntity.ok(candidatos);
    }

    /**
     * Geocodifica esquina entre dos calles.
     * POST /api/geo/geocodificar-esquina
     */
    @PostMapping("/geocodificar-esquina")
    public ResponseEntity<List<GeocodeCandidato>> geocodificarEsquina(
            @Valid @RequestBody EsquinaRequest req) {
        List<GeocodeCandidato> candidatos = geoService.geocodificarEsquina(
                req.idTcaCalle1(), req.idLocCalle1(),
                req.idTcaCalle2(), req.idLocCalle2());
        return ResponseEntity.ok(candidatos);
    }

    /**
     * Geometría completa de una parcela en GeoJSON (Feature).
     * GET /api/geo/local/malvinas/parcela/geom?circ=4&secc=H&quint=0&fracc=0&manz=105B&parc=13
     * Devuelve la geometría sin clipear (desde PostGIS, no desde MVT).
     */
    @GetMapping("/local/malvinas/parcela/geom")
    public ResponseEntity<String> getParcelaGeom(
            @RequestParam String circ, @RequestParam String secc,
            @RequestParam String quint, @RequestParam String fracc,
            @RequestParam String manz, @RequestParam String parc) {
        return geoService.getParcelaGeom(circ, secc, quint, fracc, manz, parc)
                .map(geojson -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(geojson))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Geocodifica tramo entre dos calles sobre una calle objetivo.
     * POST /api/geo/geocodificar-entre-calles
     */
    @PostMapping("/geocodificar-entre-calles")
    public ResponseEntity<List<GeocodeCandidato>> geocodificarEntreCalles(
            @Valid @RequestBody EntreCallesRequest req) {
        List<GeocodeCandidato> candidatos = geoService.geocodificarEntreCalles(
                req.idTcaCalleObjetivo(), req.idLocCalleObjetivo(),
                req.idTcaLimite1(), req.idLocLimite1(),
                req.idTcaLimite2(), req.idLocLimite2());
        return ResponseEntity.ok(candidatos);
    }
}

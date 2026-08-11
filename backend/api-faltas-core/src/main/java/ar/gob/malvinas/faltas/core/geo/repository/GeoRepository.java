package ar.gob.malvinas.faltas.core.geo.repository;

import ar.gob.malvinas.faltas.core.geo.web.dto.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a catálogos GIS locales de Malvinas Argentinas.
 * Tablas fuente: geo_gmat_localidad, geo_gmat_calle en PostgreSQL/PostGIS.
 *
 * Activo solamente cuando geoDataSource está configurado.
 * No lanza excepción si el datasource no está disponible; devuelve listas vacías.
 *
 * Convenciones:
 *  - ccalle = identificador de calle (equivalente a idTca).
 *  - id_loc = identificador de localidad (2 char, ej: LP).
 *  - alias = nombre anterior de la calle, cuando difiere de nombre.
 *  - Coordenadas devueltas en WGS84 (EPSG:4326).
 *
 * GEO-MODEL-002 / E4.
 */
@Repository
@ConditionalOnBean(name = "geoJdbcClient")
public class GeoRepository {

    private static final Logger log = LoggerFactory.getLogger(GeoRepository.class);
    private static final String FUENTE_MALVINAS = "MALVINAS_LOCAL";

    private final JdbcClient jdbc;

    public GeoRepository(@Qualifier("geoJdbcClient") JdbcClient geoJdbcClient) {
        this.jdbc = geoJdbcClient;
    }

    /**
     * Lista todas las localidades de Malvinas Argentinas.
     */
    public List<LocalidadGeoDto> findLocalidades() {
        try {
            return jdbc.sql("""
                    SELECT TRIM(id_loc) AS id_loc, TRIM(localidad) AS localidad
                    FROM geo_gmat_localidad
                    ORDER BY localidad
                    """)
                    .query((rs, row) -> new LocalidadGeoDto(
                            rs.getString("id_loc"),
                            toTitleCase(rs.getString("localidad")),
                            FUENTE_MALVINAS))
                    .list();
        } catch (Exception e) {
            log.error("Error al consultar localidades Malvinas", e);
            return Collections.emptyList();
        }
    }

    /**
     * Busca calles usando el índice tolerante geo_malv_calle_indice_busqueda.
     * Soporta: orden invertido (ROCHA DARDO / DARDO ROCHA), alias histórico,
     * similitud trigram (pg_trgm), tolerancia a errores ortográficos.
     * Devuelve las 5 mejores coincidencias ordenadas por score descendente.
     * Cuando se provee altura, filtra calles donde esa altura no existe en ningún tramo.
     * Slice: PROMPT_CURSOR_INDICE_TOLERANTE_BUSQUEDA_CALLES_2026-07-28
     */
    public List<CalleGeoDto> findCalles(String query, String idLoc) {
        return findCalles(query, idLoc, null);
    }

    public List<CalleGeoDto> findCalles(String query, String idLoc, Integer altura) {
        try {
            if (query == null || query.isBlank() || query.length() < 2) {
                return Collections.emptyList();
            }
            boolean filtrarLoc = idLoc != null && !idLoc.isBlank();
            String idLocParam  = filtrarLoc ? idLoc.trim() : null;

            String alturaFilter = (altura != null) ? """
                    AND EXISTS (
                        SELECT 1 FROM geo_gmat_calle g
                        WHERE TRIM(g.ccalle) = b.ccalle
                          AND TRIM(g.id_loc)  = b.id_loc
                          AND (
                              :altura BETWEEN g.izq_desde AND g.izq_hasta OR
                              :altura BETWEEN g.der_desde AND g.der_hasta
                          )
                    )
                    """ : "";

            var stmt = jdbc.sql("""
                    SELECT
                        b.ccalle,
                        b.id_loc,
                        b.localidad_nombre,
                        b.nombre_vigente,
                        b.termino_original,
                        b.tipo_termino,
                        b.match_type,
                        b.score
                    FROM geo_buscar_calles_tolerante(:query, :idLoc::text, 7) b
                    WHERE 1=1
                    """ + alturaFilter + """
                    ORDER BY b.score DESC
                    LIMIT 5
                    """)
                    .param("query", query.trim())
                    .param("idLoc", idLocParam);

            if (altura != null) {
                stmt = stmt.param("altura", altura);
            }

            return stmt.query((rs, row) -> {
                        String nombre        = rs.getString("nombre_vigente");
                        String loc           = toTitleCase(rs.getString("localidad_nombre"));
                        String idLocR        = rs.getString("id_loc");
                        String tipoTermino   = rs.getString("tipo_termino");
                        String terminoOrig   = rs.getString("termino_original");
                        // Exponer nombre anterior cuando el match vino de ANTERIOR/ALIAS
                        String nombreAnterior = ("ANTERIOR".equals(tipoTermino) || "ALIAS".equals(tipoTermino))
                                && terminoOrig != null && !terminoOrig.equalsIgnoreCase(nombre)
                                ? terminoOrig : null;
                        String texto = buildTexto(nombre, loc, nombreAnterior);
                        return new CalleGeoDto(
                                rs.getString("ccalle"),
                                nombre,
                                idLocR,
                                loc,
                                nombreAnterior,
                                texto,
                                FUENTE_MALVINAS);
                    }).list();
        } catch (Exception e) {
            log.error("Error al consultar calles Malvinas q={} idLoc={}", query, idLoc, e);
            return Collections.emptyList();
        }
    }

    /**
     * Geocodifica usando geo_calle_altura_ref (fal_inmueble + geo_qmat_parcela).
     * Interpolación lineal entre los dos anclas de altura más cercanas.
     * Fuente primaria: parcelas reales con altura conocida.
     * Calidad: ALTA cuando hay bracket completo, MEDIA cuando solo un extremo.
     */
    private Optional<GeocodeCandidato> geocodificarPorInmueble(
            String idTca, String idLoc, int altura,
            String nombreCalle, String localidad) {
        try {
            return jdbc.sql("""
                    WITH ref AS (
                        SELECT altura, lat, lng
                        FROM geo_calle_altura_ref
                        WHERE ccalle = TRIM(:idTca) AND id_loc = TRIM(:idLoc)
                          AND altura > 0
                    ),
                    bracket AS (
                        SELECT
                            (SELECT altura FROM ref WHERE altura <= :altura ORDER BY altura DESC LIMIT 1) AS alt_lo,
                            (SELECT lat    FROM ref WHERE altura <= :altura ORDER BY altura DESC LIMIT 1) AS lat_lo,
                            (SELECT lng    FROM ref WHERE altura <= :altura ORDER BY altura DESC LIMIT 1) AS lng_lo,
                            (SELECT altura FROM ref WHERE altura >  :altura ORDER BY altura ASC  LIMIT 1) AS alt_hi,
                            (SELECT lat    FROM ref WHERE altura >  :altura ORDER BY altura ASC  LIMIT 1) AS lat_hi,
                            (SELECT lng    FROM ref WHERE altura >  :altura ORDER BY altura ASC  LIMIT 1) AS lng_hi
                    ),
                    interp AS (
                        SELECT
                            CASE
                                WHEN alt_lo IS NOT NULL AND alt_hi IS NOT NULL THEN
                                    lat_lo + (lat_hi - lat_lo) * (:altura - alt_lo)::float8 / NULLIF(alt_hi - alt_lo, 0)
                                WHEN alt_lo IS NOT NULL THEN lat_lo
                                WHEN alt_hi IS NOT NULL THEN lat_hi
                            END AS raw_lat,
                            CASE
                                WHEN alt_lo IS NOT NULL AND alt_hi IS NOT NULL THEN
                                    lng_lo + (lng_hi - lng_lo) * (:altura - alt_lo)::float8 / NULLIF(alt_hi - alt_lo, 0)
                                WHEN alt_lo IS NOT NULL THEN lng_lo
                                WHEN alt_hi IS NOT NULL THEN lng_hi
                            END AS raw_lng,
                            alt_lo IS NOT NULL AS tiene_lo,
                            alt_hi IS NOT NULL AS tiene_hi
                        FROM bracket
                        WHERE alt_lo IS NOT NULL OR alt_hi IS NOT NULL
                    )
                    SELECT
                        ST_Y(ST_ClosestPoint(
                            ST_Transform(seg.geom, 4326),
                            ST_SetSRID(ST_MakePoint(i.raw_lng, i.raw_lat), 4326)
                        )) AS lat,
                        ST_X(ST_ClosestPoint(
                            ST_Transform(seg.geom, 4326),
                            ST_SetSRID(ST_MakePoint(i.raw_lng, i.raw_lat), 4326)
                        )) AS lng,
                        i.tiene_lo,
                        i.tiene_hi
                    FROM interp i
                    CROSS JOIN LATERAL (
                        SELECT geom
                        FROM geo_gmat_calle
                        WHERE TRIM(ccalle) = TRIM(:idTca) AND TRIM(id_loc) = TRIM(:idLoc)
                        ORDER BY ST_Distance(
                            ST_Transform(geom, 4326)::geography,
                            ST_SetSRID(ST_MakePoint(i.raw_lng, i.raw_lat), 4326)::geography
                        )
                        LIMIT 1
                    ) seg
                    LIMIT 1
                    """)
                    .param("idTca",  idTca.trim())
                    .param("idLoc",  idLoc.trim())
                    .param("altura", altura)
                    .query((rs, row) -> {
                        double lat   = rs.getDouble("lat");
                        double lng   = rs.getDouble("lng");
                        boolean tLo  = rs.getBoolean("tiene_lo");
                        boolean tHi  = rs.getBoolean("tiene_hi");
                        String calid = (tLo && tHi) ? "ALTA" : "MEDIA";
                        String fuente = "fal_inmueble_interp";
                        String loc   = toTitleCase(localidad != null ? localidad : idLoc);
                        String desc  = nombreCalle + " " + altura + " — " + loc;
                        return new GeocodeCandidato(
                                lat, lng,
                                nombreCalle, idTca,
                                altura, null, null,
                                null, idLoc, loc,
                                null, null, null,
                                calid, fuente,
                                Collections.emptyList(), desc);
                    }).optional();
        } catch (Exception e) {
            log.warn("geocodificarPorInmueble sin datos idTca={} idLoc={} altura={}", idTca, idLoc, altura);
            return Optional.empty();
        }
    }

    /**
     * Geocodifica calle + altura.
     * Primero intenta interpolación desde fal_inmueble (geo_calle_altura_ref).
     * Si no hay datos, usa tramos de geo_gmat_calle con interpolación lineal dentro de la manzana.
     */
    public List<GeocodeCandidato> geocodificarCalleAltura(String idTca, String idLoc, int altura) {
        try {
            // --- Phase 1: try precise interpolation from real parcel addresses ---
            // Resolve street name for the candidate descriptor
            String nombreCalle = jdbc.sql(
                            "SELECT TRIM(nombre) AS n FROM geo_gmat_calle WHERE TRIM(ccalle)=TRIM(:t) AND TRIM(id_loc)=TRIM(:l) LIMIT 1")
                    .param("t", idTca).param("l", idLoc)
                    .query((rs, r) -> rs.getString("n")).optional().orElse(idTca);
            String localidad = jdbc.sql(
                            "SELECT TRIM(localidad) AS loc FROM geo_gmat_calle WHERE TRIM(ccalle)=TRIM(:t) AND TRIM(id_loc)=TRIM(:l) LIMIT 1")
                    .param("t", idTca).param("l", idLoc)
                    .query((rs, r) -> rs.getString("loc")).optional().orElse(idLoc);
            Optional<GeocodeCandidato> inmuebleResult =
                    geocodificarPorInmueble(idTca, idLoc, altura, nombreCalle, localidad);
            if (inmuebleResult.isPresent()) {
                return List.of(inmuebleResult.get());
            }
            // --- Phase 2: fallback — segment range interpolation via geo_gmat_calle ---
            return jdbc.sql("""
                    WITH seg_data AS (
                        SELECT
                            ccalle,
                            TRIM(nombre)                  AS nombre,
                            TRIM(COALESCE(alias, nombre)) AS alias,
                            TRIM(id_loc)                  AS id_loc,
                            TRIM(localidad)               AS localidad,
                            izq_desde, izq_hasta, der_desde, der_hasta,
                            CASE WHEN :altura % 2 = 1 THEN 'IMPAR' ELSE 'PAR' END AS lado,
                            ST_LineMerge(geom) AS line
                        FROM geo_gmat_calle
                        WHERE TRIM(ccalle) = TRIM(:idTca)
                          AND TRIM(id_loc) = TRIM(:idLoc)
                          AND (
                              :altura BETWEEN izq_desde AND izq_hasta OR
                              :altura BETWEEN der_desde AND der_hasta
                          )
                    ),
                    manz_proj AS (
                        SELECT
                            s.ccalle, s.id_loc, s.izq_desde,
                            MIN(ST_LineLocatePoint(s.line, dp.pt)) AS f_min,
                            MAX(ST_LineLocatePoint(s.line, dp.pt)) AS f_max
                        FROM seg_data s,
                        LATERAL (
                            SELECT m.geom AS mgeom
                            FROM geo_gmat_manzana m
                            WHERE ST_DWithin(m.geom, s.line, 30)
                            ORDER BY ST_Length(ST_Intersection(ST_Buffer(s.line, 15), m.geom)) DESC
                            LIMIT 1
                        ) best_m,
                        LATERAL (
                            SELECT (ST_DumpPoints(best_m.mgeom)).geom AS pt
                        ) dp
                        GROUP BY s.ccalle, s.id_loc, s.izq_desde
                    )
                    SELECT
                        s.ccalle,
                        s.nombre,
                        s.alias,
                        s.id_loc,
                        s.localidad,
                        s.izq_desde, s.izq_hasta, s.der_desde, s.der_hasta,
                        s.lado,
                        ST_Y(ST_Transform(ST_LineInterpolatePoint(s.line,
                            GREATEST(0.0::float8, LEAST(1.0::float8,
                                COALESCE(mp.f_min, 0.0) +
                                CASE
                                  WHEN :altura % 2 = 1
                                       AND :altura BETWEEN COALESCE(s.izq_desde,0) AND COALESCE(s.izq_hasta,0)
                                    THEN (:altura::float8 - s.izq_desde) /
                                         NULLIF(s.izq_hasta - s.izq_desde, 0.0) *
                                         COALESCE(mp.f_max - mp.f_min, 1.0)
                                  WHEN :altura % 2 = 0
                                       AND :altura BETWEEN COALESCE(s.der_desde,0) AND COALESCE(s.der_hasta,0)
                                    THEN (:altura::float8 - s.der_desde) /
                                         NULLIF(s.der_hasta - s.der_desde, 0.0) *
                                         COALESCE(mp.f_max - mp.f_min, 1.0)
                                  ELSE (:altura::float8 - LEAST(COALESCE(s.izq_desde,0), COALESCE(s.der_desde,0))) /
                                       NULLIF(GREATEST(COALESCE(s.izq_hasta,0), COALESCE(s.der_hasta,0)) -
                                              LEAST(COALESCE(s.izq_desde,0), COALESCE(s.der_desde,0)), 0.0) *
                                       COALESCE(mp.f_max - mp.f_min, 1.0)
                                END
                            ))
                        ), 4326)) AS lat,
                        ST_X(ST_Transform(ST_LineInterpolatePoint(s.line,
                            GREATEST(0.0::float8, LEAST(1.0::float8,
                                COALESCE(mp.f_min, 0.0) +
                                CASE
                                  WHEN :altura % 2 = 1
                                       AND :altura BETWEEN COALESCE(s.izq_desde,0) AND COALESCE(s.izq_hasta,0)
                                    THEN (:altura::float8 - s.izq_desde) /
                                         NULLIF(s.izq_hasta - s.izq_desde, 0.0) *
                                         COALESCE(mp.f_max - mp.f_min, 1.0)
                                  WHEN :altura % 2 = 0
                                       AND :altura BETWEEN COALESCE(s.der_desde,0) AND COALESCE(s.der_hasta,0)
                                    THEN (:altura::float8 - s.der_desde) /
                                         NULLIF(s.der_hasta - s.der_desde, 0.0) *
                                         COALESCE(mp.f_max - mp.f_min, 1.0)
                                  ELSE (:altura::float8 - LEAST(COALESCE(s.izq_desde,0), COALESCE(s.der_desde,0))) /
                                       NULLIF(GREATEST(COALESCE(s.izq_hasta,0), COALESCE(s.der_hasta,0)) -
                                              LEAST(COALESCE(s.izq_desde,0), COALESCE(s.der_desde,0)), 0.0) *
                                       COALESCE(mp.f_max - mp.f_min, 1.0)
                                END
                            ))
                        ), 4326)) AS lng
                    FROM seg_data s
                    LEFT JOIN manz_proj mp
                        ON mp.ccalle = s.ccalle
                       AND mp.id_loc = s.id_loc
                       AND mp.izq_desde = s.izq_desde
                    ORDER BY s.izq_desde
                    LIMIT 5
                    """)
                    .param("idTca", idTca.trim())
                    .param("idLoc", idLoc.trim())
                    .param("altura", altura)
                    .query((rs, row) -> {
                        String nombre = rs.getString("nombre");
                        String loc = toTitleCase(rs.getString("localidad"));
                        String idLocR = rs.getString("id_loc");
                        int desde = (int) rs.getDouble("izq_desde");
                        int hasta = (int) rs.getDouble("izq_hasta");
                        String lado = rs.getString("lado");
                        double lat = rs.getDouble("lat");
                        double lng = rs.getDouble("lng");
                        String desc = nombre + " " + altura + " — " + loc;
                        return new GeocodeCandidato(
                                lat, lng,
                                nombre, rs.getString("ccalle"),
                                altura, desde, hasta,
                                lado, idLocR, loc,
                                null, null, null,
                                "ALTA", "geo_gmat_calle",
                                Collections.emptyList(), desc);
                    }).list();
        } catch (Exception e) {
            log.error("Error geocodificacion calle+altura idTca={} idLoc={} altura={}", idTca, idLoc, altura, e);
            return Collections.emptyList();
        }
    }

    /**
     * Reverse geocoding: dado un punto (lat, lng en WGS84), encuentra la calle más cercana.
     * Busca en radio de 100 metros con ST_DWithin.
     * Ordena por distancia para devolver las más cercanas primero.
     */
    public List<GeocodeCandidato> reversePoint(double lat, double lng) {
        try {
            return jdbc.sql("""
                    SELECT
                        ccalle,
                        TRIM(nombre) AS nombre,
                        TRIM(id_loc) AS id_loc,
                        TRIM(localidad) AS localidad,
                        izq_desde, izq_hasta, der_desde, der_hasta,
                        ST_Distance(
                            ST_Transform(geom, 4326)::geography,
                            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography
                        ) AS distancia_m,
                        ST_Y(ST_ClosestPoint(ST_Transform(ST_LineMerge(geom), 4326),
                            ST_SetSRID(ST_Point(:lng, :lat), 4326))) AS snap_lat,
                        ST_X(ST_ClosestPoint(ST_Transform(ST_LineMerge(geom), 4326),
                            ST_SetSRID(ST_Point(:lng, :lat), 4326))) AS snap_lng
                    FROM geo_gmat_calle
                    WHERE ST_DWithin(
                        ST_Transform(geom, 4326)::geography,
                        ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography,
                        100
                    )
                    ORDER BY distancia_m
                    LIMIT 3
                    """)
                    .param("lat", lat)
                    .param("lng", lng)
                    .query((rs, row) -> {
                        String nombre = rs.getString("nombre");
                        String loc = toTitleCase(rs.getString("localidad"));
                        String idLocR = rs.getString("id_loc");
                        int desde = (int) rs.getDouble("izq_desde");
                        int hasta = (int) rs.getDouble("izq_hasta");
                        double snapLat = rs.getDouble("snap_lat");
                        double snapLng = rs.getDouble("snap_lng");
                        double distM = rs.getDouble("distancia_m");
                        String calidad = distM < 20 ? "ALTA" : distM < 50 ? "MEDIA" : "BAJA";
                        String desc = nombre + " — " + loc + " (~" + Math.round(distM) + "m)";
                        return new GeocodeCandidato(
                                snapLat, snapLng,
                                nombre, rs.getString("ccalle"),
                                null, desde, hasta,
                                null, idLocR, loc,
                                null, null, null,
                                calidad, "reverse_geo_gmat_calle",
                                Collections.emptyList(), desc);
                    }).list();
        } catch (Exception e) {
            log.error("Error reverse point lat={} lng={}", lat, lng, e);
            return Collections.emptyList();
        }
    }

    /**
     * Geocodifica esquina entre dos calles.
     * Usa ST_Intersection entre las geometrías de ambas calles.
     * Devuelve candidatos si hay intersección geométrica.
     */
    public List<GeocodeCandidato> geocodificarEsquina(
            String idTca1, String idLoc1, String idTca2, String idLoc2) {
        try {
            return jdbc.sql("""
                    SELECT
                        c1.ccalle AS ccalle1, TRIM(c1.nombre) AS nombre1, TRIM(c1.id_loc) AS id_loc1, TRIM(c1.localidad) AS localidad1,
                        c2.ccalle AS ccalle2, TRIM(c2.nombre) AS nombre2,
                        ST_Y(ST_Transform(ST_Centroid(ST_Intersection(
                            ST_LineMerge(c1.geom), ST_LineMerge(c2.geom)
                        )), 4326)) AS lat,
                        ST_X(ST_Transform(ST_Centroid(ST_Intersection(
                            ST_LineMerge(c1.geom), ST_LineMerge(c2.geom)
                        )), 4326)) AS lng
                    FROM geo_gmat_calle c1
                    JOIN geo_gmat_calle c2
                        ON ST_Intersects(c1.geom, c2.geom)
                        AND TRIM(c2.ccalle) = TRIM(:idTca2)
                        AND TRIM(c2.id_loc) = TRIM(:idLoc2)
                    WHERE TRIM(c1.ccalle) = TRIM(:idTca1)
                      AND TRIM(c1.id_loc) = TRIM(:idLoc1)
                    LIMIT 5
                    """)
                    .param("idTca1", idTca1.trim())
                    .param("idLoc1", idLoc1.trim())
                    .param("idTca2", idTca2.trim())
                    .param("idLoc2", idLoc2.trim())
                    .query((rs, row) -> {
                        String nombre1 = rs.getString("nombre1");
                        String nombre2 = rs.getString("nombre2");
                        String loc = toTitleCase(rs.getString("localidad1"));
                        String idLocR = rs.getString("id_loc1");
                        double lat = rs.getDouble("lat");
                        double lng = rs.getDouble("lng");
                        String desc = nombre1 + " y " + nombre2 + " — " + loc;
                        return new GeocodeCandidato(
                                lat, lng, nombre1, rs.getString("ccalle1"),
                                null, null, null, null,
                                idLocR, loc, null, null, null,
                                "ALTA", "geo_gmat_calle_intersect",
                                Collections.emptyList(), desc);
                    }).list();
        } catch (Exception e) {
            log.error("Error geocodificacion esquina idTca1={} idLoc1={} idTca2={} idLoc2={}",
                    idTca1, idLoc1, idTca2, idLoc2, e);
            return Collections.emptyList();
        }
    }

    /**
     * Geocodifica tramo entre dos calles sobre una calle objetivo.
     *
     * Algoritmo aprobado GEO-MODEL-002 / E6:
     * 1. Resolver las tres calles por idTca + idLoc.
     * 2. Encontrar la posicion de A y B sobre la calle principal:
     *    interseccion exacta, nodo compartido, o proyeccion del tramo mas cercano
     *    dentro de tolerancia controlada (200 m).
     * 3. Construir el corredor de la calle principal entre ambas posiciones.
     * 4. Dividir el corredor en tramos elementales (filas individuales de geo_gmat_calle).
     * 5. El punto resultante es el punto medio interior del tramo, nunca una esquina.
     * 6. Si hay un solo tramo: devolver su punto medio.
     * 7. Si hay varios: elegir el tramo cuyo punto medio este mas cerca del centro
     *    longitudinal del corredor; devolver su punto medio.
     * 8. Si dos tramos son igualmente centrales: devolver ambos (AMBIGUOUS).
     * 9. El punto se calcula con ST_LineInterpolatePoint(..., 0.5).
     *
     * Lista vacia para: corredor desconectado, limite lejano (>200m), sin tramos.
     */
    public List<GeocodeCandidato> geocodificarEntreCalles(
            String idTcaObj, String idLocObj,
            String idTcaL1, String idLocL1,
            String idTcaL2, String idLocL2) {
        try {
            return jdbc.sql("""
                    WITH
                    calle_obj AS (
                        SELECT
                            MIN(TRIM(ccalle))    AS ccalle,
                            MIN(TRIM(nombre))    AS nombre,
                            MIN(TRIM(id_loc))    AS id_loc,
                            MIN(TRIM(localidad)) AS localidad,
                            ST_LineMerge(ST_Collect(geom)) AS geom_merged
                        FROM geo_gmat_calle
                        WHERE TRIM(ccalle) = TRIM(:idTcaObj)
                          AND TRIM(id_loc)  = TRIM(:idLocObj)
                    ),
                    limite_a AS (
                        SELECT MIN(TRIM(nombre)) AS nombre,
                               ST_LineMerge(ST_Collect(geom)) AS geom_merged
                        FROM geo_gmat_calle
                        WHERE TRIM(ccalle) = TRIM(:idTcaL1)
                          AND TRIM(id_loc)  = TRIM(:idLocL1)
                    ),
                    limite_b AS (
                        SELECT MIN(TRIM(nombre)) AS nombre,
                               ST_LineMerge(ST_Collect(geom)) AS geom_merged
                        FROM geo_gmat_calle
                        WHERE TRIM(ccalle) = TRIM(:idTcaL2)
                          AND TRIM(id_loc)  = TRIM(:idLocL2)
                    ),
                    posiciones AS (
                        SELECT
                            ST_LineLocatePoint(co.geom_merged,
                                ST_ClosestPoint(co.geom_merged, la.geom_merged)) AS fa,
                            ST_Intersects(co.geom_merged, la.geom_merged)        AS exacta_a,
                            ST_Distance(ST_Transform(co.geom_merged, 4326)::geography,
                                        ST_Transform(la.geom_merged, 4326)::geography)
                                                                                 AS dist_a_m,
                            ST_LineLocatePoint(co.geom_merged,
                                ST_ClosestPoint(co.geom_merged, lb.geom_merged)) AS fb,
                            ST_Intersects(co.geom_merged, lb.geom_merged)        AS exacta_b,
                            ST_Distance(ST_Transform(co.geom_merged, 4326)::geography,
                                        ST_Transform(lb.geom_merged, 4326)::geography)
                                                                                 AS dist_b_m,
                            co.geom_merged AS obj_geom,
                            co.ccalle, co.nombre AS nombre_obj,
                            co.id_loc, co.localidad,
                            la.nombre AS nombre_la,
                            lb.nombre AS nombre_lb
                        FROM calle_obj co, limite_a la, limite_b lb
                        WHERE ST_GeometryType(co.geom_merged) = 'ST_LineString'
                          AND la.geom_merged IS NOT NULL
                          AND lb.geom_merged IS NOT NULL
                    ),
                    corredor AS (
                        SELECT
                            p.*,
                            LEAST(p.fa, p.fb)    AS f_ini,
                            GREATEST(p.fa, p.fb) AS f_fin,
                            (p.fa + p.fb) / 2.0  AS f_centro,
                            ROUND(ST_Length(
                                ST_Transform(ST_LineSubstring(
                                    p.obj_geom,
                                    LEAST(p.fa, p.fb), GREATEST(p.fa, p.fb)
                                ), 4326)::geography
                            )::numeric, 1) AS longitud_corredor_m
                        FROM posiciones p
                        WHERE p.dist_a_m <= 200
                          AND p.dist_b_m <= 200
                          AND GREATEST(p.fa, p.fb) - LEAST(p.fa, p.fb) > 0.001
                    ),
                    tramos_en_corredor AS (
                        SELECT
                            g.geom AS tramo_geom,
                            ROUND(ST_Length(ST_Transform(g.geom, 4326)::geography)::numeric, 1)
                                                            AS longitud_tramo_m,
                            ST_LineLocatePoint(c.obj_geom,
                                ST_ClosestPoint(c.obj_geom, ST_Centroid(g.geom)))
                                                            AS pos_en_obj,
                            c.f_ini, c.f_fin, c.f_centro, c.longitud_corredor_m,
                            c.exacta_a, c.exacta_b,
                            ROUND(c.dist_a_m::numeric, 1)  AS dist_a_m,
                            ROUND(c.dist_b_m::numeric, 1)  AS dist_b_m,
                            c.nombre_obj, c.id_loc, c.localidad, c.ccalle,
                            c.nombre_la, c.nombre_lb
                        FROM geo_gmat_calle g
                        CROSS JOIN corredor c
                        WHERE TRIM(g.ccalle) = TRIM(:idTcaObj)
                          AND TRIM(g.id_loc)  = TRIM(:idLocObj)
                    ),
                    tramos_filtrados AS (
                        SELECT
                            *,
                            ABS(pos_en_obj - f_centro)                      AS dist_al_centro,
                            ST_Y(ST_Transform(ST_LineInterpolatePoint(
                                ST_LineMerge(tramo_geom), 0.5), 4326))      AS midlat,
                            ST_X(ST_Transform(ST_LineInterpolatePoint(
                                ST_LineMerge(tramo_geom), 0.5), 4326))      AS midlng,
                            COUNT(*) OVER ()                                 AS n_tramos,
                            MIN(ABS(pos_en_obj - f_centro)) OVER ()         AS min_dist_centro
                        FROM tramos_en_corredor
                        WHERE pos_en_obj BETWEEN f_ini AND f_fin
                    ),
                    candidatos AS (
                        SELECT *, COUNT(*) OVER () AS cnt_candidatos
                        FROM tramos_filtrados
                        WHERE ABS(dist_al_centro - min_dist_centro) < 0.005
                    )
                    SELECT
                        midlat              AS lat,
                        midlng              AS lng,
                        nombre_obj, ccalle, id_loc, localidad,
                        nombre_la, nombre_lb,
                        longitud_corredor_m, longitud_tramo_m,
                        n_tramos, exacta_a, exacta_b,
                        dist_a_m, dist_b_m, cnt_candidatos
                    FROM candidatos
                    ORDER BY midlat
                    """)
                    .param("idTcaObj", idTcaObj.trim())
                    .param("idLocObj", idLocObj.trim())
                    .param("idTcaL1",  idTcaL1.trim())
                    .param("idLocL1",  idLocL1.trim())
                    .param("idTcaL2",  idTcaL2.trim())
                    .param("idLocL2",  idLocL2.trim())
                    .query((rs, row) -> {
                        String nombreObj = rs.getString("nombre_obj");
                        String nombreLa  = rs.getString("nombre_la");
                        String nombreLb  = rs.getString("nombre_lb");
                        String loc       = toTitleCase(rs.getString("localidad"));
                        String idLocR    = rs.getString("id_loc");
                        double lat       = rs.getDouble("lat");
                        double lng       = rs.getDouble("lng");
                        int    nTramos   = rs.getInt("n_tramos");
                        boolean exactaA  = rs.getBoolean("exacta_a");
                        boolean exactaB  = rs.getBoolean("exacta_b");
                        double distAm    = rs.getDouble("dist_a_m");
                        double distBm    = rs.getDouble("dist_b_m");
                        double longCorr  = rs.getDouble("longitud_corredor_m");
                        double longTram  = rs.getDouble("longitud_tramo_m");
                        int    cntCand   = rs.getInt("cnt_candidatos");

                        String calidad = calcularCalidadEntreCalles(
                                nTramos, exactaA, exactaB, cntCand);

                        List<String> advertencias = new java.util.ArrayList<>();
                        if (!exactaA) advertencias.add(
                                "Limite A proyectado a " + distAm + "m de la calle objetivo");
                        if (!exactaB) advertencias.add(
                                "Limite B proyectado a " + distBm + "m de la calle objetivo");

                        String limA = exactaA ? "EXACTA" : ("PROYECTADA:" + distAm + "m");
                        String limB = exactaB ? "EXACTA" : ("PROYECTADA:" + distBm + "m");
                        String evidencia = "tramos=" + nTramos
                                + "|corredor=" + longCorr + "m"
                                + "|tramo="    + longTram + "m"
                                + "|limA="     + limA
                                + "|limB="     + limB
                                + "|fuente=geo_gmat_calle_entre";

                        String desc = toTitleCase(nombreObj)
                                + " entre " + toTitleCase(nombreLa)
                                + " y "     + toTitleCase(nombreLb)
                                + " - "     + loc;

                        return new GeocodeCandidato(
                                lat, lng, nombreObj, rs.getString("ccalle"),
                                null, null, null, null,
                                idLocR, loc, null, null, null,
                                calidad, evidencia, advertencias, desc);
                    }).list();
        } catch (Exception e) {
            log.error("Error geocodificacion entre calles idTcaObj={} idLocObj={}",
                    idTcaObj, idLocObj, e);
            return Collections.emptyList();
        }
    }

    private static String calcularCalidadEntreCalles(
            int nTramos, boolean exactaA, boolean exactaB, int cntCandidatos) {
        if (nTramos == 1 && exactaA && exactaB) return "EXACT_SINGLE_SEGMENT_MIDPOINT";
        if (nTramos == 1)                        return "PROJECTED_CENTRAL_SEGMENT_MIDPOINT";
        if (cntCandidatos > 1)                   return "AMBIGUOUS_TWO_CENTRAL_SEGMENTS";
        if (exactaA && exactaB)                  return "EXACT_CENTRAL_SEGMENT_MIDPOINT";
        return "PROJECTED_CENTRAL_SEGMENT_MIDPOINT";
    }

    /**
     * Detalle completo de una parcela: nomenclatura, dirección (calle más cercana),
     * inmuebles asociados y comercios por inmueble.
     * Devuelve Optional.empty() si la parcela no existe en geo_qmat_parcela.
     */
    public Optional<ParcelaDetalleDto> findParcelaDetalle(
            String circ, String secc, String quint, String fracc, String manz, String parc) {
        try {
            var rows = jdbc.sql("""
                    WITH parcela AS (
                        SELECT p.circunsc, p.secc, p.quint, p.fracc, p.manz, p.parc,
                               ST_Transform(ST_Centroid(p.geom), 4326) AS centroide
                        FROM geo_qmat_parcela p
                        WHERE TRIM(p.circunsc) = :circ
                          AND TRIM(p.secc)     = :secc
                          AND TRIM(COALESCE(p.quint, '0')) = :quint
                          AND TRIM(COALESCE(p.fracc, '0')) = :fracc
                          AND TRIM(p.manz)     = :manz
                          AND TRIM(p.parc)     = :parc
                        LIMIT 1
                    ),
                    inmuebles AS (
                        SELECT DISTINCT i.id_suj, i.id_bie
                        FROM parcela par
                        JOIN fal_inmueble i
                            ON TRIM(i.circunscripcion::CHAR(1)) = TRIM(par.circunsc::CHAR(1))
                            AND TRIM(i.seccion)  = TRIM(par.secc)
                            AND TRIM(COALESCE(i.quinta::text,   '0')) = TRIM(COALESCE(par.quint, '0'))
                            AND TRIM(COALESCE(i.fraccion::text, '0')) = TRIM(COALESCE(par.fracc, '0'))
                            AND TRIM(i.manzana)  = TRIM(par.manz)
                            AND TRIM(i.parcela)  = TRIM(par.parc)
                    )
                    SELECT
                        par.circunsc, par.secc, par.quint, par.fracc, par.manz, par.parc,
                        ST_Y(par.centroide) AS cent_lat,
                        ST_X(par.centroide) AS cent_lng,
                        (SELECT TRIM(nombre)
                         FROM geo_gmat_calle gc
                         WHERE ST_DWithin(
                             ST_Transform(gc.geom, 4326)::geography,
                             par.centroide::geography, 100)
                         ORDER BY ST_Distance(
                             ST_Transform(gc.geom, 4326)::geography,
                             par.centroide::geography)
                         LIMIT 1) AS calle_nombre,
                        inm.id_suj AS inm_id_suj,
                        inm.id_bie AS inm_id_bie,
                        c.nombre_fantasia,
                        ci.comercio_id_suj,
                        ci.comercio_id_bie
                    FROM parcela par
                    LEFT JOIN inmuebles inm ON true
                    LEFT JOIN fal_comercio_inmueble ci
                        ON ci.inmueble_id_suj = inm.id_suj
                       AND ci.inmueble_id_bie = inm.id_bie
                    LEFT JOIN fal_comercio c
                        ON c.id_suj = ci.comercio_id_suj
                       AND c.id_bie = ci.comercio_id_bie
                    ORDER BY inm.id_suj, inm.id_bie, ci.comercio_id_suj
                    """)
                    .param("circ",  circ.trim())
                    .param("secc",  secc.trim())
                    .param("quint", quint.trim())
                    .param("fracc", fracc.trim())
                    .param("manz",  manz.trim())
                    .param("parc",  parc.trim())
                    .query((rs, row) -> {
                        var map = new java.util.HashMap<String, Object>();
                        map.put("circunsc",       rs.getString("circunsc"));
                        map.put("secc",           rs.getString("secc"));
                        map.put("quint",          rs.getString("quint"));
                        map.put("fracc",          rs.getString("fracc"));
                        map.put("manz",           rs.getString("manz"));
                        map.put("parc",           rs.getString("parc"));
                        map.put("cent_lat",       rs.getDouble("cent_lat"));
                        map.put("cent_lng",       rs.getDouble("cent_lng"));
                        map.put("calle_nombre",   rs.getString("calle_nombre"));
                        map.put("inm_id_suj",     rs.getObject("inm_id_suj"));
                        map.put("inm_id_bie",     rs.getObject("inm_id_bie"));
                        map.put("nombre_fantasia",rs.getString("nombre_fantasia"));
                        map.put("com_id_suj",     rs.getObject("comercio_id_suj"));
                        map.put("com_id_bie",     rs.getObject("comercio_id_bie"));
                        return map;
                    }).list();

            if (rows.isEmpty()) return Optional.empty();

            var first = rows.get(0);
            double centLat    = (double) first.get("cent_lat");
            double centLng    = (double) first.get("cent_lng");
            String callNombre = (String) first.get("calle_nombre");
            String dir        = callNombre != null ? toTitleCase(callNombre) : "";

            String nomenc = buildNomenclatura(
                    (String) first.get("circunsc"), (String) first.get("secc"),
                    (String) first.get("quint"),    (String) first.get("fracc"),
                    (String) first.get("manz"),     (String) first.get("parc"));

            // Aggregate inmuebles (LinkedHashMap preserves insertion order / dedup)
            var inmMap  = new LinkedHashMap<String, InmuebleResumenDto>();
            var comList = new ArrayList<ComercioResumenDto>();

            for (var row : rows) {
                Object inmSujObj = row.get("inm_id_suj");
                Object inmBieObj = row.get("inm_id_bie");
                if (inmSujObj == null) continue;

                int inmSuj = ((Number) inmSujObj).intValue();
                int inmBie = ((Number) inmBieObj).intValue();
                String inmKey = inmSuj + "-" + inmBie;
                inmMap.putIfAbsent(inmKey, new InmuebleResumenDto(inmSuj, inmBie, inmKey));

                Object comSujObj = row.get("com_id_suj");
                if (comSujObj != null) {
                    int comSuj = ((Number) comSujObj).intValue();
                    int comBie = ((Number) row.get("com_id_bie")).intValue();
                    String comKey = comSuj + "-" + comBie;
                    String fantasia = (String) row.get("nombre_fantasia");
                    boolean already = comList.stream().anyMatch(c -> c.cuentaDisplay().equals(comKey));
                    if (!already) {
                        comList.add(new ComercioResumenDto(comSuj, comBie,
                                fantasia != null ? fantasia.trim() : "", comKey));
                    }
                }
            }

            return Optional.of(new ParcelaDetalleDto(
                    nomenc, dir, centLat, centLng,
                    new ArrayList<>(inmMap.values()), comList));
        } catch (Exception e) {
            log.error("Error findParcelaDetalle circ={} secc={} manz={} parc={}", circ, secc, manz, parc, e);
            return Optional.empty();
        }
    }

    /**
     * Devuelve la geometría completa de una parcela como GeoJSON Feature string.
     * Coordenadas en WGS84 (EPSG:4326), precisión 6 decimales.
     * Retorna Optional.empty() si la parcela no existe.
     */
    public Optional<String> findParcelaGeom(
            String circ, String secc, String quint, String fracc, String manz, String parc) {
        try {
            return jdbc.sql("""
                    SELECT ST_AsGeoJSON(ST_Transform(geom, 4326), 6) AS geom_json
                    FROM geo_qmat_parcela
                    WHERE TRIM(circunsc)              = :circ
                      AND TRIM(secc)                  = :secc
                      AND TRIM(COALESCE(quint, '0'))  = :quint
                      AND TRIM(COALESCE(fracc, '0'))  = :fracc
                      AND TRIM(manz)                  = :manz
                      AND TRIM(parc)                  = :parc
                    LIMIT 1
                    """)
                    .param("circ",  circ.trim())
                    .param("secc",  secc.trim())
                    .param("quint", quint.trim())
                    .param("fracc", fracc.trim())
                    .param("manz",  manz.trim())
                    .param("parc",  parc.trim())
                    .query((rs, row) -> rs.getString("geom_json"))
                    .optional();
        } catch (Exception e) {
            log.error("Error findParcelaGeom circ={} secc={} manz={} parc={}", circ, secc, manz, parc, e);
            return Optional.empty();
        }
    }

    private static String buildNomenclatura(
            String circ, String secc, String quint, String fracc, String manz, String parc) {
        StringBuilder sb = new StringBuilder();
        sb.append(circ.trim()).append('-').append(secc.trim());
        String q = quint != null ? quint.trim() : "0";
        String f = fracc != null ? fracc.trim() : "0";
        if (!"0".equals(q)) sb.append("-Q").append(q);
        if (!"0".equals(f)) sb.append("-F").append(f);
        sb.append("-M").append(manz.trim()).append("-P").append(parc.trim());
        return sb.toString();
    }

    private String buildTexto(String nombre, String localidad, String nombreAnterior) {
        StringBuilder sb = new StringBuilder(nombre).append(" — ").append(localidad);
        if (nombreAnterior != null) sb.append(" — ex ").append(nombreAnterior);
        return sb.toString();
    }

    private String toTitleCase(String s) {
        if (s == null || s.isBlank()) return s;
        String trimmed = s.trim();
        StringBuilder result = new StringBuilder();
        boolean nextUpper = true;
        for (char c : trimmed.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                result.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}

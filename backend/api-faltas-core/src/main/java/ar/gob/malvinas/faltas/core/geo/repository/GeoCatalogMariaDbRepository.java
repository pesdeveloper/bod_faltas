package ar.gob.malvinas.faltas.core.geo.repository;

import ar.gob.malvinas.faltas.core.geo.web.dto.CalleGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.LocalidadGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.ProvinciaGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.UnidadTerritorialGeoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * Acceso a catálogos IGN/INDEC desde MariaDB (sb_faltas_db).
 *
 * Fuente: tablas del baseline cargadas desde datos nacionales (esquema verificado E5).
 *   - geo_ign_provincia            (id smallint, nombre varchar64, si_activo)
 *   - geo_ign_municipio            (id int, nombre varchar64, provincia_id smallint, si_activo)
 *   - geo_ign_departamento         (id int, nombre varchar48, provincia_id smallint, si_activo)
 *   - geo_indec_localidad_censal   (id bigint, nombre varchar64, municipio_id int, departamento_id int, si_activo)
 *   - geo_indec_calles             (id bigint, nombre varchar64, categoria char36, localidad_censal_id bigint, si_activo)
 *
 * La vista vw_geo_municipio_departamento solo contiene ids sin nombres; no se usa directamente.
 *
 * Activo solamente cuando geoCatalogJdbcClient está configurado.
 * Devuelve listas vacías ante cualquier error; no propaga excepciones.
 *
 * No llama a ningún servicio externo. Todo proviene de bases de datos locales.
 *
 * GEO-MODEL-002 / E5-VALIDACION (columnas verificadas contra esquema real).
 */
@Repository
@ConditionalOnBean(name = "geoCatalogJdbcClient")
public class GeoCatalogMariaDbRepository {

    private static final Logger log = LoggerFactory.getLogger(GeoCatalogMariaDbRepository.class);

    private final JdbcClient jdbc;

    public GeoCatalogMariaDbRepository(
            @Qualifier("geoCatalogJdbcClient") JdbcClient geoCatalogJdbcClient) {
        this.jdbc = geoCatalogJdbcClient;
    }

    /**
     * Lista todas las provincias argentinas desde geo_ign_provincia.
     * Columnas verificadas (E5): id (smallint), nombre (varchar64), si_activo (smallint).
     */
    public List<ProvinciaGeoDto> findProvincias() {
        try {
            return jdbc.sql("""
                    SELECT id, nombre
                    FROM geo_ign_provincia
                    WHERE si_activo = 1
                    ORDER BY nombre
                    """)
                    .query((rs, row) -> new ProvinciaGeoDto(
                            rs.getLong("id"),
                            toTitleCase(rs.getString("nombre"))))
                    .list();
        } catch (Exception e) {
            log.warn("Error al consultar geo_ign_provincia: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Lista municipios y departamentos de una provincia.
     * UNION de geo_ign_municipio y geo_ign_departamento filtrados por provincia_id.
     * Columnas verificadas contra esquema real (E5): id, nombre, provincia_id, si_activo.
     */
    public List<UnidadTerritorialGeoDto> findMunicipiosYDepartamentos(long provinciaId) {
        try {
            return jdbc.sql("""
                    SELECT id, nombre, 'MUNICIPIO' AS tipo
                    FROM geo_ign_municipio
                    WHERE provincia_id = :provinciaId AND si_activo = 1
                    UNION ALL
                    SELECT id, nombre, 'DEPARTAMENTO' AS tipo
                    FROM geo_ign_departamento
                    WHERE provincia_id = :provinciaId AND si_activo = 1
                    ORDER BY nombre
                    """)
                    .param("provinciaId", provinciaId)
                    .query((rs, row) -> new UnidadTerritorialGeoDto(
                            rs.getLong("id"),
                            toTitleCase(rs.getString("nombre")),
                            rs.getString("tipo"),
                            provinciaId))
                    .list();
        } catch (Exception e) {
            log.warn("Error al consultar municipios/departamentos provinciaId={}: {}",
                    provinciaId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Lista localidades censales de una unidad territorial (municipio o departamento).
     * Columnas verificadas contra esquema real (E5): id, nombre, municipio_id, departamento_id, si_activo.
     * Filtra por municipio_id OR departamento_id para cubrir ambos tipos de unidad territorial.
     */
    public List<LocalidadGeoDto> findLocalidades(long unidadTerritorialId) {
        try {
            return jdbc.sql("""
                    SELECT id, nombre
                    FROM geo_indec_localidad_censal
                    WHERE (municipio_id = :utId OR departamento_id = :utId)
                      AND si_activo = 1
                    ORDER BY nombre
                    """)
                    .param("utId", unidadTerritorialId)
                    .query((rs, row) -> new LocalidadGeoDto(
                            String.valueOf(rs.getLong("id")),
                            toTitleCase(rs.getString("nombre")),
                            "EXTERNO_IGN_INDEC"))
                    .list();
        } catch (Exception e) {
            log.warn("Error al consultar geo_indec_localidad_censal utId={}: {}",
                    unidadTerritorialId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca calles externas (IGN/INDEC) por texto, opcionalmente filtradas por localidad censal.
     * Columnas verificadas contra esquema real (E5): id, nombre, categoria, localidad_censal_id, si_activo.
     * El filtro por localidad usa localidad_censal_id (bigint).
     */
    public List<CalleGeoDto> findCallesExternas(String query, String localidadId) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        try {
            String q = "%" + query.toUpperCase() + "%";
            boolean filtrarLoc = localidadId != null && !localidadId.isBlank();

            String sql = """
                    SELECT id, nombre, categoria, localidad_censal_id, localidad_censal_nombre
                    FROM geo_indec_calles
                    WHERE UPPER(nombre) LIKE :q
                      AND si_activo = 1
                    """ +
                    (filtrarLoc ? "  AND localidad_censal_id = :localidadId\n" : "") +
                    """
                    ORDER BY nombre
                    LIMIT 20
                    """;

            var stmt = jdbc.sql(sql).param("q", q);
            if (filtrarLoc) {
                try {
                    stmt = stmt.param("localidadId", Long.parseLong(localidadId.trim()));
                } catch (NumberFormatException ex) {
                    log.debug("localidadId no numérico para calles externas: {}", localidadId);
                }
            }

            return stmt.query((rs, row) -> {
                String nombre = rs.getString("nombre");
                String categoria = rs.getString("categoria");
                String localidadNombre = toTitleCase(rs.getString("localidad_censal_nombre"));
                String idLocResult = String.valueOf(rs.getLong("localidad_censal_id"));
                StringBuilder texto = new StringBuilder(toTitleCase(nombre));
                if (localidadNombre != null && !localidadNombre.isBlank()) texto.append(" - ").append(localidadNombre);
                if (categoria != null && !categoria.isBlank()) texto.append(" (").append(toTitleCase(categoria)).append(")");
                return new CalleGeoDto(
                        String.valueOf(rs.getLong("id")),
                        nombre,
                        idLocResult,
                        localidadNombre != null ? localidadNombre : "",
                        null,
                        texto.toString(),
                        "EXTERNO_IGN_INDEC");
            }).list();
        } catch (Exception e) {
            log.warn("Error al consultar geo_indec_calles q={}: {}", query, e.getMessage());
            return Collections.emptyList();
        }
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

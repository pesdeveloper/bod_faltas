package ar.gob.malvinas.faltas.core.geo;

import ar.gob.malvinas.faltas.core.geo.application.GeoService;
import ar.gob.malvinas.faltas.core.geo.repository.GeoCatalogMariaDbRepository;
import ar.gob.malvinas.faltas.core.geo.repository.GeoRepository;
import ar.gob.malvinas.faltas.core.geo.web.dto.CalleGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.GeocodeCandidato;
import ar.gob.malvinas.faltas.core.geo.web.dto.LocalidadGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.ProvinciaGeoDto;
import ar.gob.malvinas.faltas.core.geo.web.dto.UnidadTerritorialGeoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests del GeoService.
 *
 * Verifica:
 * 1. Provincias leídas desde MariaDB (no GeoRef).
 * 2. Municipios/departamentos leídos desde MariaDB.
 * 3. Localidades externas leídas desde MariaDB.
 * 4. Calles externas leídas desde MariaDB.
 * 5. Ningún cliente GeoRef es invocado (no existe en GeoService).
 * 6. Localidades Malvinas leídas desde PostGIS.
 * 7. Calles Malvinas leídas desde PostGIS.
 * 8. Sugerencias de calles incluyen localidad en el texto.
 * 9. Calles homónimas diferenciadas por localidad e ids.
 * 10. Calle + altura usa PostGIS.
 * 11. Calle + calle (esquina) usa PostGIS.
 * 12. Entre calles usa PostGIS.
 * 13. Reverse point usa PostGIS.
 * 14. Backend sin MariaDB catálogos devuelve lista vacía (no falla).
 * 15. PostGIS deshabilitado no rompe los catálogos MariaDB.
 */
class GeoServiceTest {

    private GeoRepository geoRepo;
    private GeoCatalogMariaDbRepository catalogRepo;
    private GeoService geoService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        geoRepo = mock(GeoRepository.class);
        catalogRepo = mock(GeoCatalogMariaDbRepository.class);

        ObjectProvider<GeoRepository> geoProvider = mock(ObjectProvider.class);
        ObjectProvider<GeoCatalogMariaDbRepository> catalogProvider = mock(ObjectProvider.class);

        when(geoProvider.getIfAvailable()).thenReturn(geoRepo);
        when(catalogProvider.getIfAvailable()).thenReturn(catalogRepo);

        geoService = new GeoService(geoProvider, catalogProvider);
    }

    // ── Requisito 1: provincias desde MariaDB ────────────────────────────────

    @Test
    void getProvincias_devuelveDesdeCatalogMariaDB() {
        var provincias = List.of(
                new ProvinciaGeoDto(6L, "Buenos Aires"),
                new ProvinciaGeoDto(2L, "Ciudad Autónoma de Buenos Aires"));
        when(catalogRepo.findProvincias()).thenReturn(provincias);

        var result = geoService.getProvincias();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(6L);
        verify(catalogRepo).findProvincias();
        verifyNoInteractions(geoRepo);
    }

    // ── Requisito 2: municipios/departamentos desde MariaDB ──────────────────

    @Test
    void getMunicipios_devuelveDesdeMariaDB_incluyeMunicipiosYDepartamentos() {
        var unidades = List.of(
                new UnidadTerritorialGeoDto(60515L, "Malvinas Argentinas", "MUNICIPIO", 6L),
                new UnidadTerritorialGeoDto(60518L, "San Miguel", "MUNICIPIO", 6L),
                new UnidadTerritorialGeoDto(60000L, "General Pueyrredón", "DEPARTAMENTO", 6L));
        when(catalogRepo.findMunicipiosYDepartamentos(6L)).thenReturn(unidades);

        var result = geoService.getMunicipios(6L);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(UnidadTerritorialGeoDto::tipo)
                .containsExactlyInAnyOrder("MUNICIPIO", "MUNICIPIO", "DEPARTAMENTO");
        verify(catalogRepo).findMunicipiosYDepartamentos(6L);
        verifyNoInteractions(geoRepo);
    }

    // ── Requisito 3: localidades externas desde MariaDB ──────────────────────

    @Test
    void getLocalidades_externas_devuelveDesdeMariaDB() {
        var localidades = List.of(
                new LocalidadGeoDto("060515010", "Grand Bourg", "EXTERNO_IGN_INDEC"),
                new LocalidadGeoDto("060515020", "Los Polvorines", "EXTERNO_IGN_INDEC"));
        when(catalogRepo.findLocalidades(60515L)).thenReturn(localidades);

        var result = geoService.getLocalidades(60515L);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(l -> assertThat(l.fuente()).isEqualTo("EXTERNO_IGN_INDEC"));
        verify(catalogRepo).findLocalidades(60515L);
        verifyNoInteractions(geoRepo);
    }

    // ── Requisito 4: calles externas desde MariaDB ───────────────────────────

    @Test
    void getCallesExternas_devuelveDesdeMariaDB() {
        var calles = List.of(
                new CalleGeoDto("1001", "RIVADAVIA", "060515010", "", null,
                        "Rivadavia (CALLE)", "EXTERNO_IGN_INDEC"));
        when(catalogRepo.findCallesExternas("RIVAD", "060515010")).thenReturn(calles);

        var result = geoService.getCallesExternas("RIVAD", "060515010");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fuente()).isEqualTo("EXTERNO_IGN_INDEC");
        verify(catalogRepo).findCallesExternas("RIVAD", "060515010");
        verifyNoInteractions(geoRepo);
    }

    @Test
    void getCallesExternas_queryCorta_devuelveVacia() {
        var result = geoService.getCallesExternas("R", null);
        assertThat(result).isEmpty();
        verifyNoInteractions(catalogRepo);
        verifyNoInteractions(geoRepo);
    }

    // ── Requisito 5: ningún cliente GeoRef invocado ──────────────────────────
    // (GeoService ya no contiene RestClient; este test verifica indirectamente
    //  que la clase compila y funciona sin ninguna dependencia HTTP externa.)

    @Test
    void geoService_noContieneClienteGeoRef_compilaCorrectamente() {
        // Si GeoService tuviera una dependencia RestClient/WebClient/HttpClient,
        // el test setUp fallaría. El hecho de que setUp pase es la verificación.
        assertThat(geoService).isNotNull();

        // Confirmar que los métodos de catálogo delegan al repo MariaDB, no a HTTP
        when(catalogRepo.findProvincias()).thenReturn(Collections.emptyList());
        geoService.getProvincias();
        verify(catalogRepo).findProvincias();
        verifyNoInteractions(geoRepo);
    }

    // ── Requisito 6: localidades Malvinas desde PostGIS ──────────────────────

    @Test
    void getLocalidadesMalvinas_devuelveDesdPostGIS() {
        var localidades = List.of(
                new LocalidadGeoDto("LP", "Los Polvorines", "MALVINAS_LOCAL"),
                new LocalidadGeoDto("GB", "Grand Bourg", "MALVINAS_LOCAL"));
        when(geoRepo.findLocalidades()).thenReturn(localidades);

        var result = geoService.getLocalidadesMalvinas();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(l -> assertThat(l.fuente()).isEqualTo("MALVINAS_LOCAL"));
        verify(geoRepo).findLocalidades();
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 7: calles Malvinas desde PostGIS ───────────────────────────

    @Test
    void getCallesMalvinas_devuelveDesdePostGIS() {
        var calles = List.of(
                new CalleGeoDto("1307", "SAN MARTIN, JOSE DE", "LP", "Los Polvorines",
                        null, "SAN MARTIN, JOSE DE — Los Polvorines", "MALVINAS_LOCAL"));
        when(geoRepo.findCalles("SAN", "LP", null)).thenReturn(calles);

        var result = geoService.getCallesMalvinas("SAN", "LP", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fuente()).isEqualTo("MALVINAS_LOCAL");
        verify(geoRepo).findCalles("SAN", "LP", null);
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 8: sugerencias incluyen localidad ──────────────────────────

    @Test
    void callesMalvinas_textoIncluyelocalidad() {
        var calles = List.of(
                new CalleGeoDto("2211", "ROCHA, DARDO", "LP", "Los Polvorines",
                        null, "ROCHA, DARDO — Los Polvorines", "MALVINAS_LOCAL"),
                new CalleGeoDto("2211", "ROCHA, DARDO", "GB", "Grand Bourg",
                        null, "ROCHA, DARDO — Grand Bourg", "MALVINAS_LOCAL"));
        when(geoRepo.findCalles("ROCHA", null, null)).thenReturn(calles);

        var result = geoService.getCallesMalvinas("ROCHA", null, null);

        assertThat(result).allSatisfy(c ->
                assertThat(c.texto()).contains(c.localidad()));
    }

    // ── Requisito 9: calles homónimas diferenciadas por localidad e ids ───────

    @Test
    void callesMalvinas_homonimas_diferenciadas() {
        var calles = List.of(
                new CalleGeoDto("1307", "SAN MARTIN, JOSE DE", "GB", "Grand Bourg",
                        null, "SAN MARTIN, JOSE DE — Grand Bourg", "MALVINAS_LOCAL"),
                new CalleGeoDto("1307", "SAN MARTIN, JOSE DE", "LP", "Los Polvorines",
                        null, "SAN MARTIN, JOSE DE — Los Polvorines", "MALVINAS_LOCAL"));
        when(geoRepo.findCalles("SAN MARTIN", null, null)).thenReturn(calles);

        var result = geoService.getCallesMalvinas("SAN MARTIN", null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CalleGeoDto::idLoc)
                .containsExactlyInAnyOrder("GB", "LP");
        // Textos distintos aunque idTca igual
        assertThat(result.get(0).texto()).isNotEqualTo(result.get(1).texto());
    }

    // ── Requisito 10: calle + altura usa PostGIS ─────────────────────────────

    @Test
    void geocodificarCalleAltura_usaPostGIS() {
        var candidato = new GeocodeCandidato(
                -34.4958, -58.6945, "ROCHA, DARDO", "2211",
                3153, 3101, 3299, "IMPAR", "LP", "Los Polvorines",
                null, null, null, "ALTA", "geo_gmat_calle",
                Collections.emptyList(), "ROCHA, DARDO 3153 — Los Polvorines");
        when(geoRepo.geocodificarCalleAltura("2211", "LP", 3153))
                .thenReturn(List.of(candidato));

        var result = geoService.geocodificarCalleAltura("2211", "LP", 3153);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).calidad()).isEqualTo("ALTA");
        verify(geoRepo).geocodificarCalleAltura("2211", "LP", 3153);
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 11: esquina (calle+calle) usa PostGIS ──────────────────────

    @Test
    void geocodificarEsquina_usaPostGIS() {
        var candidato = new GeocodeCandidato(
                -34.50, -58.72, "SAN MARTIN", "1002",
                null, null, null, null, "LP", "Los Polvorines",
                null, null, null, "ALTA", "geo_gmat_calle_intersect",
                Collections.emptyList(), "SAN MARTIN y 25 DE MAYO — Los Polvorines");
        when(geoRepo.geocodificarEsquina("1002", "LP", "2187", "LP"))
                .thenReturn(List.of(candidato));

        var result = geoService.geocodificarEsquina("1002", "LP", "2187", "LP");

        assertThat(result).isNotEmpty();
        verify(geoRepo).geocodificarEsquina("1002", "LP", "2187", "LP");
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 12: entre calles usa PostGIS ───────────────────────────────

    @Test
    void geocodificarEntreCalles_usaPostGIS() {
        var candidato = new GeocodeCandidato(
                -34.49, -58.70, "ROCHA, DARDO", "2211",
                null, null, null, null, "LP", "Los Polvorines",
                null, null, null, "MEDIA", "geo_gmat_calle_entre",
                Collections.emptyList(), "ROCHA entre SAN MARTIN y 25 DE MAYO — Los Polvorines");
        when(geoRepo.geocodificarEntreCalles("2211", "LP", "1002", "LP", "2187", "LP"))
                .thenReturn(List.of(candidato));

        var result = geoService.geocodificarEntreCalles("2211", "LP", "1002", "LP", "2187", "LP");

        assertThat(result).hasSize(1);
        verify(geoRepo).geocodificarEntreCalles("2211", "LP", "1002", "LP", "2187", "LP");
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 13: reverse point usa PostGIS ──────────────────────────────

    @Test
    void reversePoint_usaPostGIS() {
        var candidato = new GeocodeCandidato(
                -34.4958, -58.6945, "ROCHA, DARDO", "2211",
                null, 3101, 3299, null, "LP", "Los Polvorines",
                null, null, null, "ALTA", "reverse_geo_gmat_calle",
                Collections.emptyList(), "ROCHA, DARDO — Los Polvorines (~15m)");
        when(geoRepo.reversePoint(-34.4958, -58.6945)).thenReturn(List.of(candidato));

        var result = geoService.reversePoint(-34.4958, -58.6945);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).localidad()).isEqualTo("Los Polvorines");
        verify(geoRepo).reversePoint(-34.4958, -58.6945);
        verifyNoInteractions(catalogRepo);
    }

    // ── Requisito 14: MariaDB no disponible → lista vacía (no falla) ─────────

    @SuppressWarnings("unchecked")
    @Test
    void getProvincias_catalogoNoDisponible_devuelveVaciaGraciosamente() {
        ObjectProvider<GeoRepository> geoProvider = mock(ObjectProvider.class);
        ObjectProvider<GeoCatalogMariaDbRepository> catalogProvider = mock(ObjectProvider.class);
        when(geoProvider.getIfAvailable()).thenReturn(null);
        when(catalogProvider.getIfAvailable()).thenReturn(null);

        GeoService serviceWithoutDbs = new GeoService(geoProvider, catalogProvider);

        assertThat(serviceWithoutDbs.getProvincias()).isEmpty();
        assertThat(serviceWithoutDbs.getMunicipios(6L)).isEmpty();
        assertThat(serviceWithoutDbs.getLocalidades(60515L)).isEmpty();
        assertThat(serviceWithoutDbs.getCallesExternas("ROCA", null)).isEmpty();
    }

    // ── Requisito 15: PostGIS deshabilitado no rompe catálogos MariaDB ────────

    @SuppressWarnings("unchecked")
    @Test
    void postGISDeshabilitado_noRompeCatalogosMariaDB() {
        ObjectProvider<GeoRepository> geoProvider = mock(ObjectProvider.class);
        ObjectProvider<GeoCatalogMariaDbRepository> catalogProvider = mock(ObjectProvider.class);
        when(geoProvider.getIfAvailable()).thenReturn(null);
        when(catalogProvider.getIfAvailable()).thenReturn(catalogRepo);

        GeoService serviceWithOnlyMariaDb = new GeoService(geoProvider, catalogProvider);

        var provincias = List.of(new ProvinciaGeoDto(6L, "Buenos Aires"));
        when(catalogRepo.findProvincias()).thenReturn(provincias);

        // PostGIS deshabilitado: operaciones PostGIS devuelven vacío
        assertThat(serviceWithOnlyMariaDb.getLocalidadesMalvinas()).isEmpty();
        assertThat(serviceWithOnlyMariaDb.getCallesMalvinas("ROCA", null, null)).isEmpty();
        assertThat(serviceWithOnlyMariaDb.geocodificarCalleAltura("2211", "LP", 100)).isEmpty();
        assertThat(serviceWithOnlyMariaDb.reversePoint(-34.0, -58.0)).isEmpty();

        // Catálogos MariaDB siguen funcionando
        assertThat(serviceWithOnlyMariaDb.getProvincias()).hasSize(1);
        verify(catalogRepo).findProvincias();
        verifyNoInteractions(geoRepo);
    }

    // ── Tests adicionales de comportamiento Malvinas ─────────────────────────

    @Test
    void getCallesMalvinas_queryCorta_devuelveVacia() {
        var result = geoService.getCallesMalvinas("A", "LP", null);
        assertThat(result).isEmpty();
        verifyNoInteractions(geoRepo);
    }

    @Test
    void getCallesMalvinas_conNombreAnterior_textoIncluyeEx() {
        var calles = List.of(
                new CalleGeoDto("RN8N", "RN8-COLECTORA NORTE", "AP", "Area De Promocion",
                        "PILAR NORTE",
                        "RN8-COLECTORA NORTE — Area De Promocion — ex PILAR NORTE",
                        "MALVINAS_LOCAL"));
        when(geoRepo.findCalles("PILAR", "AP", null)).thenReturn(calles);

        var result = geoService.getCallesMalvinas("PILAR", "AP", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombreAnterior()).isEqualTo("PILAR NORTE");
        assertThat(result.get(0).texto()).contains("ex PILAR NORTE");
    }

    @Test
    void getCallesMalvinas_filtradoPorLocalidad_pasaIdLocAlRepo() {
        when(geoRepo.findCalles("ROCA", "LP", null)).thenReturn(Collections.emptyList());

        geoService.getCallesMalvinas("ROCA", "LP", null);

        verify(geoRepo).findCalles("ROCA", "LP", null);
    }

    @Test
    void geocodificarCalleAltura_sinResultado_devuelveVacia() {
        when(geoRepo.geocodificarCalleAltura("9999", "LP", 1)).thenReturn(Collections.emptyList());

        var result = geoService.geocodificarCalleAltura("9999", "LP", 1);

        assertThat(result).isEmpty();
    }
}


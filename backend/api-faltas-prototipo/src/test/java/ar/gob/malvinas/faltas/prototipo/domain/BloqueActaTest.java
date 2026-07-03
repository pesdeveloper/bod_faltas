package ar.gob.malvinas.faltas.prototipo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BloqueActaTest {

    // -------------------------------------------------------------------------
    // Valores productivos: resolución directa
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "productivo ''{0}'' → {1}")
    @CsvSource({
        "CAPT, CAPT",
        "ENRI, ENRI",
        "NOTI, NOTI",
        "ANAL, ANAL",
        "GEXT, GEXT",
        "ARCH, ARCH",
        "CERR, CERR"
    })
    void resuelveValoresProductivos(String input, String esperado) {
        BloqueActa bloque = BloqueActa.fromLegacyOrProductive(input);
        assertEquals(esperado, bloque.codigo());
    }

    // -------------------------------------------------------------------------
    // Valores legacy: mapeados a productivos
    // -------------------------------------------------------------------------

    @Test
    void mapeaD1CapturaACapturaProductivo() {
        assertEquals(BloqueActa.CAPT, BloqueActa.fromLegacyOrProductive("D1_CAPTURA"));
    }

    @Test
    void mapeaD2EnriquecimientoAEnriProductivo() {
        assertEquals(BloqueActa.ENRI, BloqueActa.fromLegacyOrProductive("D2_ENRIQUECIMIENTO"));
    }

    @Test
    void mapeaD4NotificacionANotiProductivo() {
        assertEquals(BloqueActa.NOTI, BloqueActa.fromLegacyOrProductive("D4_NOTIFICACION"));
    }

    @Test
    void mapeaD5AnalisisAAnalProductivo() {
        assertEquals(BloqueActa.ANAL, BloqueActa.fromLegacyOrProductive("D5_ANALISIS"));
    }

    @Test
    void mapeaGestionExternaLegacyAGext() {
        assertEquals(BloqueActa.GEXT, BloqueActa.fromLegacyOrProductive("GESTION_EXTERNA"));
    }

    @Test
    void mapeaArchivoLegacyAArch() {
        assertEquals(BloqueActa.ARCH, BloqueActa.fromLegacyOrProductive("ARCHIVO"));
    }

    @Test
    void mapeaCerradaLegacyACerr() {
        assertEquals(BloqueActa.CERR, BloqueActa.fromLegacyOrProductive("CERRADA"));
    }

    // -------------------------------------------------------------------------
    // D3_DOCUMENTAL y variantes: rechazo explícito
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "D3 variante ''{0}'' debe rechazarse")
    @ValueSource(strings = {"D3_DOCUMENTAL", "D3_PREPARACION_DOCUMENTAL", "D3", "DOCUMENTAL"})
    void rechazaVariantesD3Documental(String input) {
        UnsupportedOperationException ex = assertThrows(
            UnsupportedOperationException.class,
            () -> BloqueActa.fromLegacyOrProductive(input)
        );
        assertTrue(ex.getMessage().contains("D3_DOCUMENTAL no es un bloque productivo"),
            "El mensaje debe explicar que D3 no es bloque productivo");
    }

    // -------------------------------------------------------------------------
    // Null / blank: error controlado
    // -------------------------------------------------------------------------

    @Test
    void rechazaNull() {
        assertThrows(IllegalArgumentException.class,
            () -> BloqueActa.fromLegacyOrProductive(null));
    }

    @Test
    void rechazaBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> BloqueActa.fromLegacyOrProductive("   "));
    }

    // -------------------------------------------------------------------------
    // Serialización: código productivo es el persistible
    // -------------------------------------------------------------------------

    @Test
    void codigoCapturaEsCuatroCaracteres() {
        assertEquals("CAPT", BloqueActa.CAPT.codigo());
        assertEquals(4, BloqueActa.CAPT.codigo().length());
    }

    @Test
    void todosLosBloquesTienen4Caracteres() {
        for (BloqueActa b : BloqueActa.values()) {
            assertEquals(4, b.codigo().length(),
                "El código de " + b.name() + " debe tener 4 caracteres (CHAR4)");
        }
    }

    @Test
    void bloqueProductivoExponeCodigo() {
        assertEquals("ANAL", BloqueActa.ANAL.codigo());
        assertEquals("ANALISIS", BloqueActa.ANAL.nombre());
        assertNotNull(BloqueActa.ANAL.descripcion());
    }
}

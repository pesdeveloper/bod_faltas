package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-1: DocumentoPlantillaDefaultService")
class DocumentoPlantillaDefaultTest {

    private DocumentoPlantillaDefaultRepository repo;
    private DocumentoPlantillaDefaultService service;

    private static final LocalDateTime AHORA = FaltasClockTestSupport.FIXED.now();
    private static final LocalDateTime AYER = AHORA.minusDays(1);
    private static final LocalDateTime MANIANA = AHORA.plusDays(1);

    @BeforeEach
    void setUp() {
        repo = new InMemoryDocumentoPlantillaDefaultRepository();
        service = new DocumentoPlantillaDefaultService(repo);
    }

    private FalDocumentoPlantillaDefault def(
            AccionDocumental accion, TipoActa tipoActa, Long idDep,
            int prioridad, boolean activo, LocalDateTime vigHasta) {
        Long id = repo.nextId();
        return repo.guardar(new FalDocumentoPlantillaDefault(
                id, accion, tipoActa, TipoDocu.ACTO_ADMINISTRATIVO,
                idDep, null, 100L, prioridad, AYER, vigHasta, activo, AYER, "sistema"));
    }

    @Nested
    @DisplayName("Resolucion exitosa")
    class ResolucionExitosa {

        @Test
        @DisplayName("1. Resuelve default por accion documental")
        void resuelve_por_accion() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);
            FalDocumentoPlantillaDefault d = service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA);
            assertThat(d.getAccionDocumental()).isEqualTo(AccionDocumental.EMITIR_FALLO);
        }

        @Test
        @DisplayName("2. Resuelve default mas especifico por tipoActa")
        void resuelve_especifico_tipo_acta() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);
            FalDocumentoPlantillaDefault esp = def(
                    AccionDocumental.EMITIR_FALLO, TipoActa.TRANSITO, null, 20, true, null);

            FalDocumentoPlantillaDefault d = service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, TipoActa.TRANSITO, null, AHORA);

            assertThat(d.getId()).isEqualTo(esp.getId());
        }

        @Test
        @DisplayName("3. Resuelve default mas especifico por dependencia")
        void resuelve_especifico_dependencia() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);
            FalDocumentoPlantillaDefault esp = def(
                    AccionDocumental.EMITIR_FALLO, null, 5L, 20, true, null);

            FalDocumentoPlantillaDefault d = service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, 5L, AHORA);

            assertThat(d.getId()).isEqualTo(esp.getId());
        }

        @Test
        @DisplayName("Resuelve generico cuando no hay especifico para tipoActa solicitado")
        void resuelve_generico_si_no_hay_especifico() {
            FalDocumentoPlantillaDefault gen = def(
                    AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);

            FalDocumentoPlantillaDefault d = service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, TipoActa.COMERCIO, null, AHORA);

            assertThat(d.getId()).isEqualTo(gen.getId());
        }
    }

    @Nested
    @DisplayName("Vigencia y activo")
    class VigenciaActivo {

        @Test
        @DisplayName("4. No devuelve default con vigencia vencida")
        void no_devuelve_vencido() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, AHORA.minusHours(1));

            assertThatThrownBy(() -> service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA))
                    .isInstanceOf(PlantillaDefaultNoEncontradaException.class);
        }

        @Test
        @DisplayName("4b. Devuelve default aun vigente")
        void devuelve_vigente() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, MANIANA);
            assertThat(service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA)).isNotNull();
        }

        @Test
        @DisplayName("5. No devuelve default con siActivo=false")
        void no_devuelve_inactivo() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, false, null);

            assertThatThrownBy(() -> service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA))
                    .isInstanceOf(PlantillaDefaultNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Errores de resolucion")
    class Errores {

        @Test
        @DisplayName("6. Falla ante ambiguedad de prioridad")
        void falla_ambiguedad() {
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);
            def(AccionDocumental.EMITIR_FALLO, null, null, 10, true, null);

            assertThatThrownBy(() -> service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA))
                    .isInstanceOf(PlantillaDefaultAmbiguaException.class)
                    .hasMessageContaining("Ambiguedad");
        }

        @Test
        @DisplayName("7. Falla si no hay ninguno registrado")
        void falla_sin_default() {
            assertThatThrownBy(() -> service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA))
                    .isInstanceOf(PlantillaDefaultNoEncontradaException.class);
        }

        @Test
        @DisplayName("7b. Falla si no hay default para la accion especifica")
        void falla_accion_incorrecta() {
            def(AccionDocumental.EMITIR_CONSTANCIA, null, null, 10, true, null);

            assertThatThrownBy(() -> service.resolverDefault(
                    AccionDocumental.EMITIR_FALLO, null, null, AHORA))
                    .isInstanceOf(PlantillaDefaultNoEncontradaException.class);
        }
    }
}
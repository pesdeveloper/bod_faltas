package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.application.service.ObservacionService;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11G: ObservacionTest")
class ObservacionTest {

    private InMemoryObservacionRepository repo;
    private InMemoryActaRepository actaRepo;
    private ObservacionService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryObservacionRepository();
        actaRepo = new InMemoryActaRepository();
        service = new ObservacionService(repo, actaRepo);
    }

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.now(), LocalDateTime.now(), "Calle 1", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null, LocalDateTime.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    @Nested
    @DisplayName("EntidadTipoObservada - 22 codigos")
    class CodigosEnum {

        @Test
        @DisplayName("Todos los 22 codigos son unicos")
        void codigos_unicos() {
            long distinct = java.util.Arrays.stream(EntidadTipoObservada.values())
                    .mapToInt(EntidadTipoObservada::codigo).distinct().count();
            assertThat(distinct).isEqualTo(22);
        }

        @Test
        @DisplayName("fromCodigo resuelve todos los 22 codigos")
        void fromCodigo_todos() {
            for (EntidadTipoObservada tipo : EntidadTipoObservada.values()) {
                EntidadTipoObservada found = EntidadTipoObservada.fromCodigo(tipo.codigo());
                assertThat(found).isEqualTo(tipo);
            }
        }

        @Test
        @DisplayName("fromCodigo codigo desconocido lanza IllegalArgumentException")
        void fromCodigo_desconocido() {
            assertThatThrownBy(() -> EntidadTipoObservada.fromCodigo(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FalObservacion - constructor y validaciones")
    class ModeloValidaciones {

        @Test
        @DisplayName("texto vacio rechazado")
        void texto_vacio_rechazado() {
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "   ", null, LocalDateTime.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto null rechazado")
        void texto_null_rechazado() {
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, null, null, LocalDateTime.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto de 1000 caracteres aceptado")
        void texto_limite_1000() {
            String texto = "A".repeat(1000);
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, texto, null, LocalDateTime.now(), "U");
            assertThat(obs.getObservacion()).hasSize(1000);
        }

        @Test
        @DisplayName("texto de 1001 caracteres rechazado")
        void texto_supera_1000_rechazado() {
            String texto = "A".repeat(1001);
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, texto, null, LocalDateTime.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto con espacios se hace trim")
        void texto_trim() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "  hola  ", null, LocalDateTime.now(), "U");
            assertThat(obs.getObservacion()).isEqualTo("hola");
        }

        @Test
        @DisplayName("siActiva=true al crear")
        void activa_al_crear() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "texto", null, LocalDateTime.now(), "U");
            assertThat(obs.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("copia produce objeto independiente")
        void copia_independiente() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "texto", OrigenObservacion.USUARIO, LocalDateTime.now(), "U");
            FalObservacion copia = obs.copia();
            copia.setSiActiva(false);
            assertThat(obs.isSiActiva()).isTrue();
            assertThat(copia.isSiActiva()).isFalse();
        }
    }

    @Nested
    @DisplayName("ObservacionService - operaciones")
    class ServicioOperaciones {

        @Test
        @DisplayName("agregar observacion para ACTA existente")
        void agregar_acta_existente() {
            FalActa acta = crearActa();
            FalObservacion obs = service.agregar(EntidadTipoObservada.ACTA, acta.getId(),
                    "Observacion de prueba", OrigenObservacion.USUARIO, "U");
            assertThat(obs.getId()).isNotNull();
            assertThat(obs.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("tipo no disponible rechazado explicitamente")
        void tipo_no_disponible_rechazado() {
            assertThatThrownBy(() -> service.agregar(EntidadTipoObservada.PERSONA, 1L,
                    "texto", OrigenObservacion.USUARIO, "U"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no disponible");
        }

        @Test
        @DisplayName("acta inexistente rechazada")
        void acta_inexistente_rechazada() {
            assertThatThrownBy(() -> service.agregar(EntidadTipoObservada.ACTA, 9999L,
                    "texto", OrigenObservacion.USUARIO, "U"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("alta + desactivacion preserva historial")
        void alta_desactivacion_historial() {
            FalActa acta = crearActa();
            FalObservacion obs = service.agregar(EntidadTipoObservada.ACTA, acta.getId(),
                    "texto", OrigenObservacion.SISTEMA, "U");
            service.desactivar(obs.getId());

            List<FalObservacion> todas = service.listarPorEntidad(EntidadTipoObservada.ACTA, acta.getId());
            List<FalObservacion> activas = service.listarActivasPorEntidad(EntidadTipoObservada.ACTA, acta.getId());
            assertThat(todas).hasSize(1);
            assertThat(activas).isEmpty();
        }

        @Test
        @DisplayName("listarActivasPorEntidad filtra correctamente")
        void listar_activas() {
            FalActa acta = crearActa();
            service.agregar(EntidadTipoObservada.ACTA, acta.getId(), "activa", OrigenObservacion.USUARIO, "U");
            FalObservacion obs2 = service.agregar(EntidadTipoObservada.ACTA, acta.getId(), "a desactivar", OrigenObservacion.USUARIO, "U");
            service.desactivar(obs2.getId());

            assertThat(service.listarActivasPorEntidad(EntidadTipoObservada.ACTA, acta.getId())).hasSize(1);
        }

        @Test
        @DisplayName("agregarSinValidarExistencia acepta entidad nueva sin validar")
        void agregar_sin_validar_existencia() {
            FalObservacion obs = service.agregarSinValidarExistencia(
                    EntidadTipoObservada.PARALIZACION, 999L, "texto", OrigenObservacion.SISTEMA, "S");
            assertThat(obs.isSiActiva()).isTrue();
        }
    }
}
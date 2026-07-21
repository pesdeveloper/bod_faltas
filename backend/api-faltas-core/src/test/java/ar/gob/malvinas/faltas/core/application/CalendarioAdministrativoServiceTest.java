package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarioAdministrativoServiceTest {

    private InMemoryDiaNoComputableRepository repository;
    private CountingClock clock;
    private CalendarioAdministrativoService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDiaNoComputableRepository();
        clock = CountingClock.startingAt(Instant.parse("2026-07-12T12:00:00Z"));
        service = new CalendarioAdministrativoService(repository, clock);
    }

    @Test
    @DisplayName("1. Lunes es computable")
    void lunes_computa() {
        LocalDate lunes = LocalDate.of(2026, 7, 6);
        assertThat(service.esDiaComputable(lunes)).isTrue();
    }

    @Test
    @DisplayName("2. Sabado es computable")
    void sabado_computa() {
        LocalDate sabado = LocalDate.of(2026, 7, 11);
        assertThat(service.esDiaComputable(sabado)).isTrue();
    }

    @Test
    @DisplayName("3. Domingo no computa")
    void domingo_no_computa() {
        LocalDate domingo = LocalDate.of(2026, 7, 12);
        assertThat(service.esDiaComputable(domingo)).isFalse();
    }

    @Test
    @DisplayName("4. 1 de enero no computa")
    void primero_enero_no_computa() {
        LocalDate primeroEnero = LocalDate.of(2027, 1, 1);
        assertThat(service.esDiaComputable(primeroEnero)).isFalse();
    }

    @Test
    @DisplayName("5. 1 de mayo no computa")
    void primero_mayo_no_computa() {
        LocalDate primeroMayo = LocalDate.of(2027, 5, 1);
        assertThat(service.esDiaComputable(primeroMayo)).isFalse();
    }

    @Test
    @DisplayName("6. Excepcion activa no computa")
    void excepcion_activa_no_computa() {
        LocalDate viernes = LocalDate.of(2026, 7, 10);
        service.registrarDiaNoComputable(viernes, TipoDiaNoComputable.FERIADO,
                "Feriado patrio", OrigenDiaNoComputable.MANUAL, null, "operador1");
        assertThat(service.esDiaComputable(viernes)).isFalse();
    }

    @Test
    @DisplayName("7. Baja valida actor antes del reloj y la fecha vuelve a computar")
    void excepcion_desactivada_vuelve_a_computar() {
        LocalDate jueves = LocalDate.of(2026, 7, 9);
        FalDiaNoComputable registrado = service.registrarDiaNoComputable(
                jueves, TipoDiaNoComputable.ASUETO_ADMINISTRATIVO,
                "Asueto temp", OrigenDiaNoComputable.MANUAL, null, "op1");
        assertThat(service.esDiaComputable(jueves)).isFalse();

        // Un actor de baja invalido debe fallar antes del reloj y no alterar el estado.
        clock.reset();
        String actorLargo = "x".repeat(37);
        assertThatThrownBy(() -> service.desactivarDiaNoComputable(registrado.getId(), actorLargo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(clock.invocationCount()).isZero();
        assertThat(service.esDiaComputable(jueves)).isFalse();
        assertThat(service.listarActivos()).hasSize(1);

        // Baja valida: captura el reloj una sola vez y normaliza el actor.
        FalDiaNoComputable baja = service.desactivarDiaNoComputable(registrado.getId(), "  op2  ");
        assertThat(clock.invocationCount()).isEqualTo(1);
        assertThat(baja.isSiActivo()).isFalse();
        assertThat(baja.getFhBaja()).isEqualTo(clock.nthInstant(0));
        assertThat(baja.getIdUserBaja()).isEqualTo("op2");
        assertThat(service.esDiaComputable(jueves)).isTrue();
    }

    @Test
    @DisplayName("8. Alta manual valida, normaliza y audita")
    void alta_manual_normaliza_y_audita() {
        clock.reset();
        LocalDate fecha = LocalDate.of(2026, 8, 3);

        // Entradas estructurales invalidas: rechazadas antes del reloj y sin persistir.
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO, "desc",
                OrigenDiaNoComputable.MANUAL, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO, "desc",
                OrigenDiaNoComputable.MANUAL, null, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO, "desc",
                OrigenDiaNoComputable.MANUAL, null, "a".repeat(37)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO, "d".repeat(161),
                OrigenDiaNoComputable.MANUAL, null, "usuario1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO, "desc",
                OrigenDiaNoComputable.MANUAL, "ref-no-permitida", "usuario1"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(clock.invocationCount()).isZero();
        assertThat(repository.size()).isZero();

        // Alta valida con espacios: descripcion y actor se persisten normalizados.
        FalDiaNoComputable resultado = service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.OTRO,
                "  Descripcion con espacios  ", OrigenDiaNoComputable.MANUAL, null, "  usuario1  ");

        assertThat(resultado.getDescripcion()).isEqualTo("Descripcion con espacios");
        assertThat(resultado.getIdUserAlta()).isEqualTo("usuario1");
        assertThat(resultado.getFhAlta()).isEqualTo(clock.nthInstant(0));
        assertThat(resultado.isSiActivo()).isTrue();
        assertThat(resultado.getFhBaja()).isNull();
        assertThat(resultado.getIdUserBaja()).isNull();
        assertThat(clock.invocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("9. Duplicado secuencial rechaza y deja un activo")
    void duplicado_secuencial_rechaza() {
        LocalDate fecha = LocalDate.of(2026, 8, 4);
        service.registrarDiaNoComputable(fecha, TipoDiaNoComputable.FERIADO,
                "Primera vez", OrigenDiaNoComputable.MANUAL, null, "op1");

        assertThatThrownBy(() ->
                service.registrarDiaNoComputable(fecha, TipoDiaNoComputable.OTRO,
                        "Segunda vez", OrigenDiaNoComputable.MANUAL, null, "op2"))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(service.listarActivos()).hasSize(1);
    }

    @Test
    @DisplayName("10. Fecha fija redundante rechaza antes del reloj")
    void fecha_fija_rechaza_sin_consultar_reloj() {
        clock.reset();
        LocalDate domingo = LocalDate.of(2026, 7, 12);

        assertThatThrownBy(() ->
                service.registrarDiaNoComputable(domingo, TipoDiaNoComputable.FERIADO,
                        "Redundante", OrigenDiaNoComputable.MANUAL, null, "op1"))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(clock.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("11. Origen externo valida, normaliza y exige referencia unica")
    void origen_externo_exige_referencia() {
        clock.reset();
        LocalDate fecha = LocalDate.of(2026, 8, 5);

        // Referencia externa invalida: rechazada antes del reloj y sin persistir.
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.FERIADO, "Feriado sync",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, null, "sistema"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.FERIADO, "Feriado sync",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "   ", "sistema"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.FERIADO, "Feriado sync",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "r".repeat(201), "sistema"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(clock.invocationCount()).isZero();
        assertThat(repository.size()).isZero();

        // Alta valida con espacios: referencia y actor se persisten normalizados.
        FalDiaNoComputable resultado = service.registrarDiaNoComputable(
                fecha, TipoDiaNoComputable.FERIADO, "Feriado sync",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "  ext-ref-001  ", "  sync-user  ");

        assertThat(resultado.getReferenciaExterna()).isEqualTo("ext-ref-001");
        assertThat(resultado.getIdUserAlta()).isEqualTo("sync-user");
        assertThat(resultado.getOrigen()).isEqualTo(OrigenDiaNoComputable.SINCRONIZACION_EXTERNA);
        assertThat(clock.invocationCount()).isEqualTo(1);

        // Misma referencia en otra fecha: rechazo por duplicado antes del reloj.
        clock.reset();
        assertThatThrownBy(() -> service.registrarDiaNoComputable(
                fecha.plusDays(1), TipoDiaNoComputable.FERIADO, "Otra fecha misma ref",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "  ext-ref-001  ", "sync-user"))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isZero();
        assertThat(service.listarActivos()).hasSize(1);
    }

    @Test
    @DisplayName("12. Listado activo ordenado por fecha ASC")
    void listado_activo_ordenado() {
        service.registrarDiaNoComputable(LocalDate.of(2026, 8, 10), TipoDiaNoComputable.FERIADO,
                "Tercero", OrigenDiaNoComputable.MANUAL, null, "op1");
        service.registrarDiaNoComputable(LocalDate.of(2026, 8, 4), TipoDiaNoComputable.OTRO,
                "Primero", OrigenDiaNoComputable.MANUAL, null, "op1");
        service.registrarDiaNoComputable(LocalDate.of(2026, 8, 7), TipoDiaNoComputable.ASUETO_ADMINISTRATIVO,
                "Segundo", OrigenDiaNoComputable.MANUAL, null, "op1");

        List<FalDiaNoComputable> activos = service.listarActivos();
        assertThat(activos).hasSize(3);
        assertThat(activos.get(0).getFecha()).isEqualTo(LocalDate.of(2026, 8, 4));
        assertThat(activos.get(1).getFecha()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(activos.get(2).getFecha()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    private enum ResultadoConcurrente { EXITO, DUPLICADO }

    @Test
    @DisplayName("13. Alta concurrente de la misma fecha deja exactamente un activo")
    void alta_concurrente_deja_exactamente_uno() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 1);
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            List<Future<ResultadoConcurrente>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int idx = i;
                futures.add(exec.submit(() -> {
                    latch.await();
                    try {
                        service.registrarDiaNoComputable(fecha, TipoDiaNoComputable.FERIADO,
                                "Concurrente " + idx, OrigenDiaNoComputable.MANUAL, null, "op" + idx);
                        return ResultadoConcurrente.EXITO;
                    } catch (PrecondicionVioladaException e) {
                        return ResultadoConcurrente.DUPLICADO;
                    }
                }));
            }
            latch.countDown();

            int exitos = 0;
            int duplicados = 0;
            for (Future<ResultadoConcurrente> f : futures) {
                // Future.get() propaga cualquier excepcion inesperada y hace fallar la prueba;
                // solo PrecondicionVioladaException se traduce a DUPLICADO dentro de cada task.
                ResultadoConcurrente r = f.get();
                if (r == ResultadoConcurrente.EXITO) exitos++;
                else duplicados++;
            }

            assertThat(exitos).isEqualTo(1);
            assertThat(duplicados).isEqualTo(9);
            assertThat(service.listarActivos()).hasSize(1);
        } finally {
            exec.shutdownNow();
        }
    }
}

package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableSaveResult;
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
                fecha, TipoDiaNoComputable.OTRO, "d".repeat(65),
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

    @Test
    @DisplayName("14. El id lo asigna la persistencia: el servicio no preasigna identificadores")
    void id_asignado_por_la_persistencia() {
        FalDiaNoComputable creado = service.registrarDiaNoComputable(
                LocalDate.of(2026, 8, 4), TipoDiaNoComputable.FERIADO,
                "Alta", OrigenDiaNoComputable.MANUAL, null, "op1");

        assertThat(creado.getId())
                .as("la entidad devuelta trae el id que asigno la persistencia")
                .isNotNull();
        assertThat(repository.buscarPorId(creado.getId())).isPresent();
    }

    @Test
    @DisplayName("15. guardarActivoSiAusentePorFecha informa yaExistia=false al insertar")
    void resultado_informa_creacion() {
        FalDiaNoComputable candidato = new FalDiaNoComputable(
                null, LocalDate.of(2026, 8, 4), TipoDiaNoComputable.FERIADO,
                "Alta", OrigenDiaNoComputable.MANUAL, null, clock.now(), "op1");

        DiaNoComputableSaveResult r = repository.guardarActivoSiAusentePorFecha(candidato);

        assertThat(r.yaExistia()).isFalse();
        assertThat(r.diaPersistido().getId()).isNotNull();
    }

    @Test
    @DisplayName("16. Fecha ya activa: yaExistia=true y devuelve la entidad canonica existente")
    void resultado_informa_existente() {
        LocalDate fecha = LocalDate.of(2026, 8, 4);
        DiaNoComputableSaveResult primero = repository.guardarActivoSiAusentePorFecha(
                new FalDiaNoComputable(null, fecha, TipoDiaNoComputable.FERIADO,
                        "Primero", OrigenDiaNoComputable.MANUAL, null, clock.now(), "op1"));

        DiaNoComputableSaveResult segundo = repository.guardarActivoSiAusentePorFecha(
                new FalDiaNoComputable(null, fecha, TipoDiaNoComputable.OTRO,
                        "Segundo", OrigenDiaNoComputable.MANUAL, null, clock.now(), "op2"));

        assertThat(segundo.yaExistia()).isTrue();
        assertThat(segundo.diaPersistido().getId()).isEqualTo(primero.diaPersistido().getId());
        assertThat(segundo.diaPersistido().getDescripcion())
                .as("devuelve el registro existente, no el candidato rechazado")
                .isEqualTo("Primero");
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("17. Altas concurrentes: exactamente un yaExistia=false y ids unicos")
    void concurrencia_exactamente_un_creado() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 2);
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            List<Future<DiaNoComputableSaveResult>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int idx = i;
                futures.add(exec.submit(() -> {
                    latch.await();
                    return repository.guardarActivoSiAusentePorFecha(new FalDiaNoComputable(
                            null, fecha, TipoDiaNoComputable.FERIADO,
                            "Concurrente " + idx, OrigenDiaNoComputable.MANUAL, null,
                            clock.now(), "op" + idx));
                }));
            }
            latch.countDown();

            int creados = 0;
            for (Future<DiaNoComputableSaveResult> f : futures) {
                DiaNoComputableSaveResult r = f.get();
                assertThat(r.diaPersistido().getId()).isNotNull();
                if (!r.yaExistia()) creados++;
            }

            assertThat(creados).isEqualTo(1);
            assertThat(repository.size())
                    .as("un candidato que pierde la carrera no persiste ni consume fila")
                    .isEqualTo(1);
        } finally {
            exec.shutdownNow();
        }
    }

    // =====================================================================
    // WAVE 1A1-CLOSE: paridad con el adaptador JDBC
    // =====================================================================

    @Test
    @DisplayName("18. guardar con id existente actualiza la fila y conserva el id")
    void guardar_con_id_existente_actualiza() {
        LocalDate fecha = LocalDate.of(2026, 8, 6);
        FalDiaNoComputable alta = repository.guardarActivoSiAusentePorFecha(
                new FalDiaNoComputable(null, fecha, TipoDiaNoComputable.FERIADO,
                        "Alta", OrigenDiaNoComputable.MANUAL, null, clock.now(), "op1")).diaPersistido();

        alta.desactivar(clock.now(), "op2");
        FalDiaNoComputable baja = repository.guardar(alta);

        assertThat(baja.getId()).isEqualTo(alta.getId());
        assertThat(repository.size()).isEqualTo(1);
        assertThat(repository.buscarPorId(alta.getId()).orElseThrow().isSiActivo()).isFalse();
        assertThat(repository.buscarActivoPorFecha(fecha)).isEmpty();
    }

    /**
     * D-1A1-01: insertar con una PK elegida por el llamador contradice
     * DECISION_ID-01 y el adaptador JDBC no puede hacerlo. Ambos adaptadores
     * rechazan con la misma excepcion y el mismo mensaje.
     */
    @Test
    @DisplayName("19. guardar con id inexistente lanza la excepcion funcional y no crea fila")
    void guardar_con_id_inexistente_rechaza() {
        FalDiaNoComputable fantasma = new FalDiaNoComputable(
                null, LocalDate.of(2026, 8, 7), TipoDiaNoComputable.FERIADO,
                "Fantasma", OrigenDiaNoComputable.MANUAL, null, clock.now(), "op1").conId(777L);

        assertThatThrownBy(() -> repository.guardar(fantasma))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessage("Dia no computable no encontrado: id=777");
        assertThat(repository.size()).isZero();
        assertThat(repository.buscarPorId(777L)).isEmpty();
    }

    /**
     * D-1A1-05: la referencia externa es un identificador tecnico. Se compara
     * exacta y sensible a mayusculas despues del trim, igual que el adaptador JDBC.
     */
    @Test
    @DisplayName("20. buscarPorOrigenYReferenciaExterna es exacta y sensible a mayusculas")
    void referencia_externa_exacta_y_case_sensitive() {
        String referencia = "Ref-AbC-123";
        FalDiaNoComputable guardado = repository.guardar(new FalDiaNoComputable(
                null, LocalDate.of(2026, 8, 8), TipoDiaNoComputable.FERIADO, "Sincronizado",
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, referencia, clock.now(), "sync"));

        var externo = OrigenDiaNoComputable.SINCRONIZACION_EXTERNA;
        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, referencia)
                .orElseThrow().getId()).isEqualTo(guardado.getId());
        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, "  " + referencia + "  ")
                .orElseThrow().getId()).isEqualTo(guardado.getId());

        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, "ref-abc-123")).isEmpty();
        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, "REF-ABC-123")).isEmpty();
        assertThat(repository.buscarPorOrigenYReferenciaExterna(
                OrigenDiaNoComputable.MANUAL, referencia)).isEmpty();
        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, null)).isEmpty();
        assertThat(repository.buscarPorOrigenYReferenciaExterna(externo, "   ")).isEmpty();
    }
}

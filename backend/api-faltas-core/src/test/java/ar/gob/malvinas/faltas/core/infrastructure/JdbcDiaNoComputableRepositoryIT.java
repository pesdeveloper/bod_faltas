package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableSaveResult;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de JdbcDiaNoComputableRepository sobre fal_dia_no_computable (WAVE 1A1).
 *
 * Ejecuta contra el sandbox real sb_faltas_db mediante Failsafe:
 *   mvn -P integration-tests verify
 *
 * Disciplina de datos:
 * - Cada escenario secuencial corre dentro de una transaccion que siempre se
 *   revierte. No hay TRUNCATE ni DELETE en esos casos.
 * - La prueba de concurrencia es la unica que confirma, porque el UNIQUE solo
 *   arbitra entre transacciones distintas. Limpia despues por su propia marca y
 *   por su propia fecha, nunca por rango ni por tabla completa.
 * - Las filas propias llevan la marca WAVE1A1_DIA_ en la descripcion y usan
 *   fechas del ano 2099, fuera de cualquier calendario funcional.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A1: JdbcDiaNoComputableRepository sobre fal_dia_no_computable")
class JdbcDiaNoComputableRepositoryIT {

    private static final String MARCA = "WAVE1A1_DIA_";

    /** Marca propia del slice de cierre de paridad. */
    private static final String MARCA_CLOSE = "WAVE1A1_CLOSE_";

    /** DATETIME(6) guarda microsegundos, no nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 2, 9, 8, 7, 654_321_000);

    private static final LocalDate FECHA_CONCURRENCIA = LocalDate.of(2099, 9, 1);

    @Autowired
    private DiaNoComputableRepository repositorio;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    // =====================================================================
    // TOPOLOGIA
    // =====================================================================

    @Test
    @DisplayName("1. Con perfil jdbc el puerto lo resuelve el adaptador JDBC")
    void puertoResueltoPorAdaptadorJdbc() {
        assertThat(repositorio).isInstanceOf(JdbcDiaNoComputableRepository.class);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS
    // =====================================================================

    @Test
    @DisplayName("2. guardar con id null inserta y devuelve el id de AUTO_INCREMENT")
    void altaSinIdRecibeIdGenerado() {
        enEscenario(() -> {
            FalDiaNoComputable persistido = repositorio.guardar(manual(dia(1), "Alta simple"));

            assertThat(persistido.getId()).isNotNull().isPositive();
            assertThat(repositorio.buscarPorId(persistido.getId())).isPresent();
            return null;
        });
    }

    @Test
    @DisplayName("3. Round-trip completo de un dia manual: la referencia externa queda NULL")
    void roundTripManual() {
        enEscenario(() -> {
            String usuario = UUID.randomUUID().toString();
            FalDiaNoComputable original = new FalDiaNoComputable(
                    null, dia(2), TipoDiaNoComputable.ASUETO_ADMINISTRATIVO, MARCA + "Asueto",
                    OrigenDiaNoComputable.MANUAL, null, T0, usuario);

            Long id = repositorio.guardar(original).getId();
            FalDiaNoComputable leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.getId()).isEqualTo(id);
            assertThat(leido.getFecha()).isEqualTo(dia(2));
            assertThat(leido.getTipo()).isEqualTo(TipoDiaNoComputable.ASUETO_ADMINISTRATIVO);
            assertThat(leido.getDescripcion()).isEqualTo(MARCA + "Asueto");
            assertThat(leido.getOrigen()).isEqualTo(OrigenDiaNoComputable.MANUAL);
            assertThat(leido.getReferenciaExterna()).isNull();
            assertThat(leido.isSiActivo()).isTrue();
            assertThat(leido.getFhAlta()).isEqualTo(T0);
            assertThat(leido.getIdUserAlta()).isEqualTo(usuario);
            assertThat(leido.getFhBaja()).isNull();
            assertThat(leido.getIdUserBaja()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("4. Round-trip completo de un dia sincronizado: conserva la referencia externa")
    void roundTripSincronizado() {
        enEscenario(() -> {
            String referencia = MARCA + UUID.randomUUID();
            FalDiaNoComputable original = new FalDiaNoComputable(
                    null, dia(3), TipoDiaNoComputable.FERIADO, MARCA + "Feriado sync",
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, referencia, T0,
                    UUID.randomUUID().toString());

            Long id = repositorio.guardar(original).getId();
            FalDiaNoComputable leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.getOrigen()).isEqualTo(OrigenDiaNoComputable.SINCRONIZACION_EXTERNA);
            assertThat(leido.getReferenciaExterna()).isEqualTo(referencia);
            return null;
        });
    }

    @Test
    @DisplayName("5. Los enums se guardan por codigo() SMALLINT, no por ordinal")
    void enumsSeGuardanPorCodigo() {
        enEscenario(() -> {
            Long id = repositorio.guardar(new FalDiaNoComputable(
                    null, dia(4), TipoDiaNoComputable.OTRO, MARCA + "Otro",
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, MARCA + "ref",
                    T0, UUID.randomUUID().toString())).getId();

            short[] crudo = jdbc.sql("SELECT tipo, origen FROM fal_dia_no_computable WHERE id = :id")
                    .param("id", id)
                    .query((rs, n) -> new short[] { rs.getShort("tipo"), rs.getShort("origen") })
                    .single();

            assertThat(crudo[0]).isEqualTo(TipoDiaNoComputable.OTRO.codigo());
            assertThat(crudo[1]).isEqualTo(OrigenDiaNoComputable.SINCRONIZACION_EXTERNA.codigo());
            assertThat(crudo[0]).isNotEqualTo((short) TipoDiaNoComputable.OTRO.ordinal());
            return null;
        });
    }

    @Test
    @DisplayName("6. Todos los valores de ambos enums hacen round-trip por codigo")
    void todosLosCodigosHacenRoundTrip() {
        enEscenario(() -> {
            int n = 10;
            for (TipoDiaNoComputable tipo : TipoDiaNoComputable.values()) {
                for (OrigenDiaNoComputable origen : OrigenDiaNoComputable.values()) {
                    boolean externo = origen == OrigenDiaNoComputable.SINCRONIZACION_EXTERNA;
                    Long id = repositorio.guardar(new FalDiaNoComputable(
                            null, dia(n++), tipo, MARCA + tipo.name(), origen,
                            externo ? MARCA + UUID.randomUUID() : null,
                            T0, UUID.randomUUID().toString())).getId();

                    FalDiaNoComputable leido = repositorio.buscarPorId(id).orElseThrow();
                    assertThat(leido.getTipo()).isEqualTo(tipo);
                    assertThat(leido.getOrigen()).isEqualTo(origen);
                }
            }
            return null;
        });
    }

    // =====================================================================
    // BAJA LOGICA Y ACTUALIZACION
    // =====================================================================

    @Test
    @DisplayName("7. guardar con id presente actualiza por PK y persiste la baja logica")
    void bajaLogicaSePersiste() {
        enEscenario(() -> {
            FalDiaNoComputable alta = repositorio.guardar(manual(dia(5), "Se da de baja"));
            String userBaja = UUID.randomUUID().toString();

            alta.desactivar(T0.plusDays(2), userBaja);
            FalDiaNoComputable baja = repositorio.guardar(alta);

            assertThat(baja.getId()).isEqualTo(alta.getId());

            FalDiaNoComputable leido = repositorio.buscarPorId(alta.getId()).orElseThrow();
            assertThat(leido.isSiActivo()).isFalse();
            assertThat(leido.getFhBaja()).isEqualTo(T0.plusDays(2));
            assertThat(leido.getIdUserBaja()).isEqualTo(userBaja);
            assertThat(repositorio.buscarActivoPorFecha(dia(5))).isEmpty();
            assertThat(mios(repositorio.listarActivosOrdenados())).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("8. Tras la baja, la columna generada fecha_activa queda NULL y libera la fecha")
    void bajaLiberaLaFecha() {
        enEscenario(() -> {
            FalDiaNoComputable alta = repositorio.guardar(manual(dia(6), "Primera"));
            alta.desactivar(T0.plusDays(1), UUID.randomUUID().toString());
            repositorio.guardar(alta);

            assertThat(fechaActivaEsNula(alta.getId())).isTrue();

            DiaNoComputableSaveResult nueva = repositorio
                    .guardarActivoSiAusentePorFecha(manual(dia(6), "Segunda"));

            assertThat(nueva.yaExistia())
                    .as("una fila inactiva no es preexistencia")
                    .isFalse();
            assertThat(nueva.diaPersistido().getId()).isNotEqualTo(alta.getId());
            return null;
        });
    }

    @Test
    @DisplayName("9. guardar con un id inexistente no inserta con PK elegida por el llamador")
    void guardarConIdInexistenteNoInserta() {
        enEscenario(() -> {
            FalDiaNoComputable fantasma = manual(dia(7), "Fantasma").conId(-779L);

            assertThatThrownBy(() -> repositorio.guardar(fantasma))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Dia no computable no encontrado");
            assertThat(repositorio.buscarPorId(-779L)).isEmpty();
            return null;
        });
    }

    // =====================================================================
    // CONSULTAS
    // =====================================================================

    @Test
    @DisplayName("10. buscarPorId y buscarActivoPorFecha devuelven vacio cuando no hay fila")
    void consultasVacias() {
        enEscenario(() -> {
            assertThat(repositorio.buscarPorId(-1L)).isEmpty();
            assertThat(repositorio.buscarActivoPorFecha(dia(8))).isEmpty();
            assertThat(mios(repositorio.listarActivosOrdenados())).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("11. buscarPorOrigenYReferenciaExterna recorta y descarta entradas sin valor")
    void busquedaPorReferenciaExterna() {
        enEscenario(() -> {
            String referencia = MARCA + UUID.randomUUID();
            Long id = repositorio.guardar(new FalDiaNoComputable(
                    null, dia(9), TipoDiaNoComputable.FERIADO, MARCA + "Sincronizado",
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, referencia, T0,
                    UUID.randomUUID().toString())).getId();

            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, referencia)
                    .orElseThrow().getId()).isEqualTo(id);
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "  " + referencia + "  ")
                    .orElseThrow().getId()).isEqualTo(id);

            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.MANUAL, referencia)).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, MARCA + "inexistente")).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(null, referencia)).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, null)).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, "   ")).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("12. listarActivosOrdenados ordena por fecha e id y excluye los inactivos")
    void listadoActivoOrdenado() {
        enEscenario(() -> {
            Long tercero = repositorio.guardar(manual(dia(23), "Tercero")).getId();
            Long primero = repositorio.guardar(manual(dia(21), "Primero")).getId();
            Long segundo = repositorio.guardar(manual(dia(22), "Segundo")).getId();

            FalDiaNoComputable descartado = repositorio.guardar(manual(dia(24), "Descartado"));
            descartado.desactivar(T0.plusDays(1), UUID.randomUUID().toString());
            repositorio.guardar(descartado);

            assertThat(mios(repositorio.listarActivosOrdenados()).stream()
                    .map(FalDiaNoComputable::getId))
                    .containsExactly(primero, segundo, tercero);
            assertThat(mios(repositorio.listarActivosOrdenados()).stream()
                    .map(FalDiaNoComputable::getFecha))
                    .isSorted();
            return null;
        });
    }

    // =====================================================================
    // ALTA SI AUSENTE POR FECHA
    // =====================================================================

    @Test
    @DisplayName("13. Primera alta por fecha: yaExistia=false y la persistencia asigna el id")
    void primeraAltaPorFecha() {
        enEscenario(() -> {
            DiaNoComputableSaveResult r = repositorio
                    .guardarActivoSiAusentePorFecha(manual(dia(25), "Primera"));

            assertThat(r.yaExistia()).isFalse();
            assertThat(r.diaPersistido().getId()).isNotNull().isPositive();
            assertThat(r.diaPersistido().getDescripcion()).isEqualTo(MARCA + "Primera");
            assertThat(repositorio.buscarActivoPorFecha(dia(25)).orElseThrow().getId())
                    .isEqualTo(r.diaPersistido().getId());
            return null;
        });
    }

    @Test
    @DisplayName("14. Fecha ya activa: yaExistia=true y devuelve la entidad canonica existente")
    void segundaAltaDevuelveLaExistente() {
        enEscenario(() -> {
            DiaNoComputableSaveResult primera = repositorio
                    .guardarActivoSiAusentePorFecha(manual(dia(26), "Primera"));

            DiaNoComputableSaveResult segunda = repositorio
                    .guardarActivoSiAusentePorFecha(manual(dia(26), "Segunda"));

            assertThat(segunda.yaExistia()).isTrue();
            assertThat(segunda.diaPersistido().getId()).isEqualTo(primera.diaPersistido().getId());
            assertThat(segunda.diaPersistido().getDescripcion())
                    .as("devuelve el registro existente, no el candidato rechazado")
                    .isEqualTo(MARCA + "Primera");
            assertThat(contarActivasEnFecha(dia(26))).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("15. Un id informado en el candidato se ignora: la PK la asigna la base")
    void idDelCandidatoSeIgnora() {
        enEscenario(() -> {
            FalDiaNoComputable conIdFalso = manual(dia(27), "Con id falso").conId(999_999_999L);

            DiaNoComputableSaveResult r = repositorio.guardarActivoSiAusentePorFecha(conIdFalso);

            assertThat(r.yaExistia()).isFalse();
            assertThat(r.diaPersistido().getId()).isNotEqualTo(999_999_999L);
            assertThat(repositorio.buscarPorId(999_999_999L)).isEmpty();
            return null;
        });
    }

    /**
     * La traduccion del UNIQUE de fecha activa pertenece a
     * guardarActivoSiAusentePorFecha, que es la operacion que expresa preexistencia.
     * La misma constraint violada desde guardar es un error de integridad y debe
     * propagarse sin convertirse en un resultado funcional.
     */
    @Test
    @DisplayName("16. El UNIQUE de fecha activa solo se traduce en la operacion que expresa preexistencia")
    void traduccionAcotadaALaOperacionCorrecta() {
        enEscenario(() -> {
            repositorio.guardarActivoSiAusentePorFecha(manual(dia(28), "Titular"));

            assertThatThrownBy(() -> repositorio.guardar(manual(dia(28), "Colision")))
                    .isInstanceOf(DuplicateKeyException.class)
                    .isNotInstanceOf(PrecondicionVioladaException.class);

            assertThat(repositorio.guardarActivoSiAusentePorFecha(manual(dia(28), "Reintento"))
                    .yaExistia()).isTrue();
            assertThat(contarActivasEnFecha(dia(28))).isEqualTo(1);
            return null;
        });
    }

    /**
     * El adaptador nunca informa la PK, de modo que la unica clave duplicada que
     * puede producir es la de fecha activa. Este test fija esa premisa contra el
     * esquema real: si apareciera otro UNIQUE, la traduccion por nombre de
     * constraint dejaria de ser suficiente y habria que revisarla.
     */
    @Test
    @DisplayName("17. La constraint traducible es el unico UNIQUE no primario de la tabla")
    void unicoUniqueNoPrimario() {
        List<String> uniques = jdbc.sql("""
                        SELECT CONSTRAINT_NAME
                        FROM information_schema.TABLE_CONSTRAINTS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'fal_dia_no_computable'
                          AND CONSTRAINT_TYPE = 'UNIQUE'
                        """)
                .query(String.class)
                .list();

        assertThat(uniques).containsExactly("uq_fal_dia_nc_fecha_activa");
    }

    // =====================================================================
    // PARIDAD CON EL ADAPTADOR INMEMORY (WAVE 1A1-CLOSE)
    // =====================================================================

    /**
     * D-1A1-05: la referencia externa es un identificador tecnico y se compara
     * exacta. Sin el CAST a BINARY la collation de la tabla
     * (utf8mb4_uca1400_ai_ci) devolveria la fila para "ref-abc-123", divergiendo
     * del String.equals del adaptador InMemory.
     */
    @Test
    @DisplayName("21. D-1A1-05: la referencia externa se compara exacta y sensible a mayusculas")
    void referenciaExternaExactaYCaseSensitive() {
        enEscenario(() -> {
            String referencia = "Ref-AbC-123";
            Long id = repositorio.guardar(sincronizado(dia(30), "Case sensitive", referencia)).getId();

            var externo = OrigenDiaNoComputable.SINCRONIZACION_EXTERNA;
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, referencia)
                    .orElseThrow().getId()).isEqualTo(id);
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, "  " + referencia + "  ")
                    .orElseThrow().getId())
                    .as("el trim de entrada se conserva")
                    .isEqualTo(id);

            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, "ref-abc-123")).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, "REF-ABC-123")).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(
                    OrigenDiaNoComputable.MANUAL, referencia)).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, null)).isEmpty();
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, "   ")).isEmpty();

            // La collation de la tabla sigue intacta: la insensibilidad existe y es
            // el adaptador el que la neutraliza, no un cambio de DDL.
            Integer sinCast = jdbc.sql("""
                            SELECT COUNT(*) FROM fal_dia_no_computable
                            WHERE origen = 2 AND referencia_externa = :ref
                            """)
                    .param("ref", "ref-abc-123")
                    .query(Integer.class)
                    .single();
            assertThat(sinCast)
                    .as("sin CAST la comparacion es insensible a mayusculas")
                    .isEqualTo(1);
            return null;
        });
    }

    /**
     * La misma matriz sobre el adaptador InMemory: ambos deben responder igual.
     */
    @Test
    @DisplayName("22. Paridad D-1A1-05: JDBC e InMemory resuelven igual la misma matriz")
    void paridadDeReferenciaExterna() {
        enEscenario(() -> {
            DiaNoComputableRepository enMemoria = new InMemoryDiaNoComputableRepository();
            String referencia = "Ref-AbC-123";
            FalDiaNoComputable candidato = sincronizado(dia(31), "Paridad", referencia);
            repositorio.guardar(candidato.copia());
            enMemoria.guardar(candidato.copia());

            var externo = OrigenDiaNoComputable.SINCRONIZACION_EXTERNA;
            for (String consulta : new String[] {
                    referencia, "  " + referencia + "  ", "ref-abc-123", "REF-ABC-123", "Ref-AbC-124" }) {
                assertThat(repositorio.buscarPorOrigenYReferenciaExterna(externo, consulta).isPresent())
                        .as("consulta %s", consulta)
                        .isEqualTo(enMemoria.buscarPorOrigenYReferenciaExterna(externo, consulta).isPresent());
            }
            assertThat(repositorio.buscarPorOrigenYReferenciaExterna(OrigenDiaNoComputable.MANUAL, referencia))
                    .isEmpty();
            assertThat(enMemoria.buscarPorOrigenYReferenciaExterna(OrigenDiaNoComputable.MANUAL, referencia))
                    .isEmpty();
            return null;
        });
    }

    /**
     * D-1A1-01: los dos adaptadores rechazan un id inexistente con la misma
     * excepcion y el mismo mensaje.
     */
    @Test
    @DisplayName("23. Paridad D-1A1-01: JDBC e InMemory rechazan igual un id inexistente")
    void paridadIdInexistente() {
        enEscenario(() -> {
            DiaNoComputableRepository enMemoria = new InMemoryDiaNoComputableRepository();
            FalDiaNoComputable fantasma = manual(dia(32), "Fantasma").conId(-992L);

            Throwable deJdbc = catchThrowable(() -> repositorio.guardar(fantasma));
            Throwable deMemoria = catchThrowable(() -> enMemoria.guardar(fantasma));

            assertThat(deJdbc).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(deMemoria).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(deJdbc.getMessage())
                    .as("mismo mensaje funcional en ambos adaptadores")
                    .isEqualTo(deMemoria.getMessage());

            assertThat(repositorio.buscarPorId(-992L)).isEmpty();
            assertThat(enMemoria.buscarPorId(-992L)).isEmpty();
            return null;
        });
    }

    // =====================================================================
    // CONCURRENCIA REAL
    // =====================================================================

    /**
     * Unico caso que confirma: el UNIQUE solo arbitra entre transacciones
     * distintas, de modo que la carrera no puede montarse dentro de una unica
     * transaccion revertida. La coordinacion usa latches, no sleeps, y la limpieza
     * posterior borra solo las filas propias de esta fecha.
     */
    @Test
    @DisplayName("18. Altas concurrentes reales: una sola fila activa y una sola creacion")
    void concurrenciaRealDejaUnaSolaFilaActiva() throws Exception {
        limpiarFechaDeConcurrencia();

        int hilos = 8;
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);

        try {
            List<Future<DiaNoComputableSaveResult>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                int indice = i;
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await();
                    return new TransactionTemplate(txManager).execute(status ->
                            repositorio.guardarActivoSiAusentePorFecha(
                                    manual(FECHA_CONCURRENCIA, "Concurrente " + indice)));
                }));
            }

            listos.await();
            largada.countDown();

            List<DiaNoComputableSaveResult> resultados = new ArrayList<>();
            for (Future<DiaNoComputableSaveResult> futuro : futuros) {
                resultados.add(futuro.get());
            }

            long creaciones = resultados.stream().filter(r -> !r.yaExistia()).count();
            Set<Long> ids = resultados.stream()
                    .map(r -> r.diaPersistido().getId())
                    .collect(Collectors.toSet());

            assertThat(creaciones).as("exactamente un yaExistia=false").isEqualTo(1);
            assertThat(resultados.stream().filter(DiaNoComputableSaveResult::yaExistia).count())
                    .isEqualTo(hilos - 1L);
            assertThat(ids)
                    .as("todos los concurrentes reciben la misma entidad canonica")
                    .hasSize(1);
            assertThat(ids.iterator().next()).isNotNull();
            assertThat(contarActivasEnFecha(FECHA_CONCURRENCIA))
                    .as("una sola fila activa para la fecha")
                    .isEqualTo(1);
            assertThat(repositorio.buscarActivoPorFecha(FECHA_CONCURRENCIA).orElseThrow().getId())
                    .isEqualTo(ids.iterator().next());
        } finally {
            pool.shutdownNow();
            limpiarFechaDeConcurrencia();
        }
    }

    // =====================================================================
    // TRANSACCION Y RESIDUOS
    // =====================================================================

    @Test
    @DisplayName("19. Rollback real: la fila es visible dentro de la transaccion y desaparece al revertir")
    void rollbackRealDejaLaBaseSinRastro() {
        long[] id = new long[1];

        new TransactionTemplate(txManager).execute(status -> {
            id[0] = repositorio.guardarActivoSiAusentePorFecha(manual(dia(29), "Se revierte"))
                    .diaPersistido().getId();
            assertThat(repositorio.buscarPorId(id[0])).isPresent();
            assertThat(repositorio.buscarActivoPorFecha(dia(29))).isPresent();
            status.setRollbackOnly();
            return null;
        });

        assertThat(contarPorId(id[0]))
                .as("el dia %s no debe sobrevivir al rollback", id[0])
                .isZero();
        assertThat(repositorio.buscarActivoPorFecha(dia(29))).isEmpty();
    }

    @Test
    @DisplayName("20. La suite no deja filas con ninguna de las marcas del slice")
    void sinResiduosDeLaMarcaDelSlice() {
        assertThat(contarMarca(MARCA))
                .as("ninguna fila con marca %s debe quedar al cerrar", MARCA)
                .isZero();
        assertThat(contarMarca(MARCA_CLOSE))
                .as("ninguna fila con marca %s debe quedar al cerrar", MARCA_CLOSE)
                .isZero();
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /** Ejecuta el escenario dentro de una transaccion que siempre se revierte. */
    private <T> T enEscenario(Supplier<T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.get();
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /** Fechas del ano 2099: fuera de cualquier calendario administrativo real. */
    private static LocalDate dia(int n) {
        return LocalDate.of(2099, 1, 1).plusDays(n);
    }

    private FalDiaNoComputable manual(LocalDate fecha, String texto) {
        return new FalDiaNoComputable(null, fecha, TipoDiaNoComputable.FERIADO, MARCA + texto,
                OrigenDiaNoComputable.MANUAL, null, T0, UUID.randomUUID().toString());
    }

    private FalDiaNoComputable sincronizado(LocalDate fecha, String texto, String referencia) {
        return new FalDiaNoComputable(null, fecha, TipoDiaNoComputable.FERIADO, MARCA_CLOSE + texto,
                OrigenDiaNoComputable.SINCRONIZACION_EXTERNA, referencia, T0,
                UUID.randomUUID().toString());
    }

    /** Aisla las filas del slice de cualquier dato funcional que exista en el sandbox. */
    private static List<FalDiaNoComputable> mios(List<FalDiaNoComputable> dias) {
        return dias.stream().filter(d -> d.getDescripcion().startsWith(MARCA)).toList();
    }

    private boolean fechaActivaEsNula(long id) {
        return Boolean.TRUE.equals(
                jdbc.sql("SELECT fecha_activa IS NULL FROM fal_dia_no_computable WHERE id = :id")
                        .param("id", id)
                        .query(Boolean.class)
                        .single());
    }

    private int contarActivasEnFecha(LocalDate fecha) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fal_dia_no_computable WHERE fecha_activa = :fecha")
                .param("fecha", fecha)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    private int contarPorId(long id) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fal_dia_no_computable WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    private int contarMarca(String marca) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fal_dia_no_computable WHERE descripcion LIKE :marca")
                .param("marca", marca + "%")
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    /** Borra solo las filas propias de la fecha de concurrencia, nunca por rango ni por tabla. */
    private void limpiarFechaDeConcurrencia() {
        jdbc.sql("""
                        DELETE FROM fal_dia_no_computable
                        WHERE fecha = :fecha AND descripcion LIKE :marca
                        """)
                .param("fecha", FECHA_CONCURRENCIA)
                .param("marca", MARCA + "%")
                .update();
    }
}

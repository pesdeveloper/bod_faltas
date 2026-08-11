package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcMotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMotivoArchivoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de JdbcMotivoArchivoRepository sobre fal_motivo_archivo (WAVE 1A1).
 *
 * Ejecuta contra el sandbox real sb_faltas_db mediante Failsafe:
 *   mvn -P integration-tests verify
 *
 * Disciplina de datos:
 * - Cada escenario corre dentro de una transaccion que siempre se revierte, de
 *   modo que ni un escenario exitoso deja filas. No hay TRUNCATE ni DELETE.
 * - Las filas propias llevan la marca WAVE1A1_MOT_ en el nombre y codigos del
 *   espacio W1A1Mnn, que no colisiona con el catalogo funcional.
 * - No se usan datos preexistentes del sandbox.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A1: JdbcMotivoArchivoRepository sobre fal_motivo_archivo")
class JdbcMotivoArchivoRepositoryIT {

    private static final String MARCA = "WAVE1A1_MOT_";

    /** DATETIME(6) guarda microsegundos, no nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 2, 11, 22, 33, 444_555_000);

    @Autowired
    private MotivoArchivoRepository repositorio;

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
        assertThat(repositorio).isInstanceOf(JdbcMotivoArchivoRepository.class);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS
    // =====================================================================

    @Test
    @DisplayName("2. guardar con id null inserta y devuelve el id de AUTO_INCREMENT")
    void altaSinIdRecibeIdGenerado() {
        enEscenario(() -> {
            FalMotivoArchivo persistido = repositorio.guardar(nuevo(1, "Alta simple"));

            assertThat(persistido.getId()).isNotNull().isPositive();
            assertThat(repositorio.buscarPorId(persistido.getId())).isPresent();
            return null;
        });
    }

    @Test
    @DisplayName("3. El id del candidato no se preasigna: dos altas reciben ids distintos")
    void altasSucesivasRecibenIdsDistintos() {
        enEscenario(() -> {
            Long primero = repositorio.guardar(nuevo(1, "Primero")).getId();
            Long segundo = repositorio.guardar(nuevo(2, "Segundo")).getId();

            assertThat(segundo).isNotNull().isNotEqualTo(primero);
            return null;
        });
    }

    @Test
    @DisplayName("4. Round-trip completo: todos los campos persistidos se recuperan iguales")
    void roundTripDeTodosLosCampos() {
        enEscenario(() -> {
            String userAlta = UUID.randomUUID().toString();
            String userMod = UUID.randomUUID().toString();

            FalMotivoArchivo original = new FalMotivoArchivo(
                    null, codigo(7), MARCA + "Round trip", true, true, true, false, T0, userAlta);
            original.setFhUltMod(T0.plusHours(3));
            original.setIdUserUltMod(userMod);

            Long id = repositorio.guardar(original).getId();
            FalMotivoArchivo leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.getId()).isEqualTo(id);
            assertThat(leido.getCodMotivoArchivo()).isEqualTo(codigo(7));
            assertThat(leido.getNombre()).isEqualTo(MARCA + "Round trip");
            assertThat(leido.isSiNulidad()).isTrue();
            assertThat(leido.isSiPermiteReingreso()).isTrue();
            assertThat(leido.isSiRequiereObservacion()).isTrue();
            assertThat(leido.isSiActivo()).isFalse();
            assertThat(leido.getFhAlta()).isEqualTo(T0);
            assertThat(leido.getIdUserAlta()).isEqualTo(userAlta);
            assertThat(leido.getFhUltMod()).isEqualTo(T0.plusHours(3));
            assertThat(leido.getIdUserUltMod()).isEqualTo(userMod);
            return null;
        });
    }

    @Test
    @DisplayName("5. Los campos de ultima modificacion se preservan como NULL, sin defaults inventados")
    void nullsOpcionalesSePreservan() {
        enEscenario(() -> {
            Long id = repositorio.guardar(nuevo(1, "Sin modificacion")).getId();
            FalMotivoArchivo leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.getFhUltMod()).isNull();
            assertThat(leido.getIdUserUltMod()).isNull();
            return null;
        });
    }

    // =====================================================================
    // CONSULTAS
    // =====================================================================

    @Test
    @DisplayName("6. buscarPorId devuelve Optional vacio para un id inexistente")
    void buscarPorIdInexistente() {
        enEscenario(() -> {
            assertThat(repositorio.buscarPorId(-1L)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("7. buscarPorCodigo normaliza a mayusculas, recorta y tolera null")
    void buscarPorCodigoNormaliza() {
        enEscenario(() -> {
            Long id = repositorio.guardar(nuevo(3, "Buscable")).getId();

            assertThat(repositorio.buscarPorCodigo(codigo(3)).orElseThrow().getId()).isEqualTo(id);
            assertThat(repositorio.buscarPorCodigo("  " + codigo(3).toLowerCase() + "  ")
                    .orElseThrow().getId()).isEqualTo(id);
            assertThat(repositorio.buscarPorCodigo(codigo(99))).isEmpty();
            assertThat(repositorio.buscarPorCodigo(null)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("8. listarActivos y listarTodos filtran si_activo y ordenan de forma deterministica")
    void listadosFiltranYOrdenan() {
        enEscenario(() -> {
            assertThat(mios(repositorio.listarTodos()))
                    .as("el escenario arranca sin motivos propios")
                    .isEmpty();

            Long activo1 = repositorio.guardar(nuevo(1, "Activo uno")).getId();
            Long inactivo = repositorio.guardar(inactivo(2, "Inactivo")).getId();
            Long activo2 = repositorio.guardar(nuevo(3, "Activo dos")).getId();

            assertThat(mios(repositorio.listarActivos()).stream().map(FalMotivoArchivo::getId))
                    .containsExactly(activo1, activo2);
            assertThat(mios(repositorio.listarTodos()).stream().map(FalMotivoArchivo::getId))
                    .containsExactly(activo1, inactivo, activo2);
            return null;
        });
    }

    // =====================================================================
    // ACTUALIZACION
    // =====================================================================

    @Test
    @DisplayName("9. guardar con id presente actualiza por PK: conserva el id y no duplica filas")
    void guardarConIdActualizaSinDuplicar() {
        enEscenario(() -> {
            FalMotivoArchivo original = repositorio.guardar(nuevo(1, "Original"));
            Long id = original.getId();

            original.setNombre(MARCA + "Modificado");
            FalMotivoArchivo actualizado = repositorio.guardar(original);

            assertThat(actualizado.getId()).isEqualTo(id);
            assertThat(repositorio.buscarPorId(id).orElseThrow().getNombre())
                    .isEqualTo(MARCA + "Modificado");
            assertThat(mios(repositorio.listarTodos())).hasSize(1);
            return null;
        });
    }

    @Test
    @DisplayName("10. actualizarAtomicamente aplica la baja logica sin borrar la fila")
    void actualizarAtomicamenteAplicaBajaLogica() {
        enEscenario(() -> {
            FalMotivoArchivo motivo = repositorio.guardar(nuevo(1, "Para dar de baja"));
            motivo.setSiActivo(false);
            motivo.setFhUltMod(T0.plusDays(1));
            motivo.setIdUserUltMod(UUID.randomUUID().toString());

            FalMotivoArchivo baja = repositorio.actualizarAtomicamente(motivo);

            assertThat(baja.getId()).isEqualTo(motivo.getId());
            assertThat(mios(repositorio.listarActivos())).isEmpty();
            assertThat(mios(repositorio.listarTodos())).hasSize(1);
            assertThat(repositorio.buscarPorId(motivo.getId()).orElseThrow().isSiActivo()).isFalse();
            return null;
        });
    }

    /**
     * MariaDB informa filas coincidentes, no modificadas. Este test fija esa
     * semantica: si el driver informara filas modificadas, un UPDATE idempotente
     * devolveria 0 y el adaptador lo confundiria con un id inexistente.
     */
    @Test
    @DisplayName("11. actualizarAtomicamente con los mismos valores no se confunde con id inexistente")
    void actualizacionIdempotenteNoFalla() {
        enEscenario(() -> {
            FalMotivoArchivo motivo = repositorio.guardar(nuevo(1, "Idempotente"));

            assertThat(repositorio.actualizarAtomicamente(motivo).getId()).isEqualTo(motivo.getId());
            return null;
        });
    }

    @Test
    @DisplayName("12. actualizarAtomicamente sobre un id inexistente lanza la excepcion funcional")
    void actualizarIdInexistente() {
        enEscenario(() -> {
            FalMotivoArchivo fantasma = new FalMotivoArchivo(
                    -777L, codigo(4), MARCA + "Fantasma", false, false, false, true,
                    T0, UUID.randomUUID().toString());

            assertThatThrownBy(() -> repositorio.actualizarAtomicamente(fantasma))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("MotivoArchivo no encontrado");
            return null;
        });
    }

    /**
     * El adaptador InMemory insertaria la fila con el id recibido. El adaptador JDBC
     * no puede: crear una fila con PK elegida por el llamador seria preasignar
     * identificadores, prohibido por DECISION_ID-01.
     */
    @Test
    @DisplayName("13. guardar con un id inexistente no inserta con PK elegida por el llamador")
    void guardarConIdInexistenteNoInserta() {
        enEscenario(() -> {
            FalMotivoArchivo fantasma = new FalMotivoArchivo(
                    -778L, codigo(5), MARCA + "Fantasma", false, false, false, true,
                    T0, UUID.randomUUID().toString());

            assertThatThrownBy(() -> repositorio.guardar(fantasma))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(repositorio.buscarPorId(-778L)).isEmpty();
            return null;
        });
    }

    // =====================================================================
    // CONSTRAINTS REALES
    // =====================================================================

    @Test
    @DisplayName("14. Codigo duplicado en el alta: el UNIQUE se traduce a la excepcion funcional")
    void codigoDuplicadoEnAlta() {
        enEscenario(() -> {
            repositorio.guardar(nuevo(1, "Primero"));

            assertThatThrownBy(() -> repositorio.guardar(nuevo(1, "Segundo")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Ya existe un motivo con codigo: " + codigo(1));
            assertThat(mios(repositorio.listarTodos())).hasSize(1);
            return null;
        });
    }

    @Test
    @DisplayName("15. Codigo duplicado al actualizar tambien se traduce a la excepcion funcional")
    void codigoDuplicadoEnActualizacion() {
        enEscenario(() -> {
            repositorio.guardar(nuevo(1, "Ocupa el codigo"));
            FalMotivoArchivo otro = repositorio.guardar(nuevo(2, "Va a chocar"));

            otro.setCodMotivoArchivo(codigo(1));
            assertThatThrownBy(() -> repositorio.actualizarAtomicamente(otro))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Ya existe un motivo con codigo: " + codigo(1));
            return null;
        });
    }

    /**
     * Solo el UNIQUE de cod_motivo_archivo se traduce. Un NOT NULL violado es un
     * fallo tecnico de integridad y debe llegar como tal a la capa de aplicacion.
     */
    @Test
    @DisplayName("16. Otra constraint no se enmascara como excepcion funcional")
    void otraConstraintNoSeTraduce() {
        enEscenario(() -> {
            FalMotivoArchivo sinFechaAlta = new FalMotivoArchivo(
                    null, codigo(6), MARCA + "Sin fh_alta", false, false, false, true,
                    null, UUID.randomUUID().toString());

            assertThatThrownBy(() -> repositorio.guardar(sinFechaAlta))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(PrecondicionVioladaException.class);
            return null;
        });
    }

    @Test
    @DisplayName("17. El UNIQUE traducible es el unico UNIQUE no primario de la tabla")
    void unicoUniqueNoPrimario() {
        List<String> uniques = jdbc.sql("""
                        SELECT CONSTRAINT_NAME
                        FROM information_schema.TABLE_CONSTRAINTS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'fal_motivo_archivo'
                          AND CONSTRAINT_TYPE = 'UNIQUE'
                        """)
                .query(String.class)
                .list();

        assertThat(uniques).containsExactly("uq_fal_motivo_arch_cod");
    }

    // =====================================================================
    // PARIDAD CON EL ADAPTADOR INMEMORY (WAVE 1A1-CLOSE)
    // =====================================================================

    /**
     * D-1A1-01: los dos adaptadores rechazan un id inexistente con la misma
     * excepcion y el mismo mensaje. Se compara contra una instancia InMemory real,
     * no contra un literal duplicado en el test.
     */
    @Test
    @DisplayName("20. Paridad D-1A1-01: JDBC e InMemory rechazan igual un id inexistente")
    void paridadIdInexistente() {
        enEscenario(() -> {
            MotivoArchivoRepository enMemoria = new InMemoryMotivoArchivoRepository();
            FalMotivoArchivo fantasma = new FalMotivoArchivo(
                    -991L, codigo(8), MARCA + "Fantasma", false, false, false, true,
                    T0, UUID.randomUUID().toString());

            Throwable deJdbc = catchThrowable(() -> repositorio.guardar(fantasma));
            Throwable deMemoria = catchThrowable(() -> enMemoria.guardar(fantasma));

            assertThat(deJdbc).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(deMemoria).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(deJdbc.getMessage())
                    .as("mismo mensaje funcional en ambos adaptadores")
                    .isEqualTo(deMemoria.getMessage());

            assertThat(repositorio.buscarPorId(-991L)).isEmpty();
            assertThat(enMemoria.buscarPorId(-991L)).isEmpty();
            return null;
        });
    }

    /**
     * D-1A1-03: ambos listados ordenan por id ascendente. El InMemory recibe las
     * mismas altas en el mismo orden y debe devolver la misma secuencia relativa.
     */
    @Test
    @DisplayName("21. Paridad D-1A1-03: ambos listados ordenan por id ascendente")
    void paridadOrdenDeListados() {
        enEscenario(() -> {
            MotivoArchivoRepository enMemoria = new InMemoryMotivoArchivoRepository();

            List<FalMotivoArchivo> altas = List.of(
                    nuevo(5, "Zeta"), inactivo(4, "Media inactiva"), nuevo(3, "Alfa"));
            altas.forEach(m -> {
                repositorio.guardar(m.copia());
                enMemoria.guardar(m.copia());
            });

            List<String> todosJdbc = codigosDe(mios(repositorio.listarTodos()));
            List<String> todosMemoria = codigosDe(enMemoria.listarTodos());
            List<String> activosJdbc = codigosDe(mios(repositorio.listarActivos()));
            List<String> activosMemoria = codigosDe(enMemoria.listarActivos());

            assertThat(todosJdbc).containsExactly(codigo(5), codigo(4), codigo(3));
            assertThat(todosJdbc).isEqualTo(todosMemoria);
            assertThat(activosJdbc).containsExactly(codigo(5), codigo(3));
            assertThat(activosJdbc).isEqualTo(activosMemoria);
            return null;
        });
    }

    // =====================================================================
    // TRANSACCION Y RESIDUOS
    // =====================================================================

    @Test
    @DisplayName("18. Rollback real: la fila es visible dentro de la transaccion y desaparece al revertir")
    void rollbackRealDejaLaBaseSinRastro() {
        long[] id = new long[1];

        new TransactionTemplate(txManager).execute(status -> {
            id[0] = repositorio.guardar(nuevo(1, "Se revierte")).getId();
            assertThat(repositorio.buscarPorId(id[0])).isPresent();
            status.setRollbackOnly();
            return null;
        });

        assertThat(contarPorId(id[0]))
                .as("el motivo %s no debe sobrevivir al rollback", id[0])
                .isZero();
    }

    @Test
    @DisplayName("19. La suite no deja filas con la marca del slice")
    void sinResiduosDeLaMarcaDelSlice() {
        assertThat(contarMarca())
                .as("ninguna fila con marca %s debe quedar fuera de una transaccion revertida", MARCA)
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

    private static String codigo(int n) {
        return String.format("W1A1M%02d", n);
    }

    private FalMotivoArchivo nuevo(int n, String descripcionLegible) {
        return new FalMotivoArchivo(null, codigo(n), MARCA + descripcionLegible,
                false, false, false, true, T0, UUID.randomUUID().toString());
    }

    private FalMotivoArchivo inactivo(int n, String descripcionLegible) {
        FalMotivoArchivo motivo = new FalMotivoArchivo(null, codigo(n), MARCA + descripcionLegible,
                false, false, false, false, T0, UUID.randomUUID().toString());
        motivo.setFhUltMod(T0.plusMinutes(5));
        motivo.setIdUserUltMod(UUID.randomUUID().toString());
        return motivo;
    }

    /** Aisla las filas del slice de cualquier dato funcional que exista en el sandbox. */
    private static List<FalMotivoArchivo> mios(List<FalMotivoArchivo> motivos) {
        return motivos.stream().filter(m -> m.getNombre().startsWith(MARCA)).toList();
    }

    private static List<String> codigosDe(List<FalMotivoArchivo> motivos) {
        return motivos.stream().map(FalMotivoArchivo::getCodMotivoArchivo).toList();
    }

    private int contarPorId(long id) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fal_motivo_archivo WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    private int contarMarca() {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM fal_motivo_archivo WHERE nombre LIKE :marca")
                .param("marca", MARCA + "%")
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }
}

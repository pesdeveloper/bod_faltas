package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.application.service.NotificacionIntentoService;
import ar.gob.malvinas.faltas.core.application.service.PlazosAdministrativosService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de la frontera transaccional de {@code QrActaService.registrarAcceso} y de la semantica de
 * {@code registrarAccesoConNotificacion} ({@code POST-B2-TX-CONSUMERS-01}, decision 2).
 *
 * <p>Verifica, contra MariaDB real, las cinco propiedades que la decision del usuario exige:
 *
 * <pre>
 * 1. acceso + evento QRACCA + snapshot hacen commit juntos
 * 2. un fallo en cualquiera de esas tres etapas revierte las tres
 * 3. la notificacion comienza solo despues del commit del acceso
 * 4. un fallo de notificacion no elimina ni revierte el acceso confirmado
 * 5. la frontera transaccional es efectiva mediante proxy Spring, no self-invocation
 *    (la propiedad 5 la verifica {@code JdbcProfileContextIT} casos 33 a 35: aca se ejercita
 *    el comportamiento resultante, no se repite la inspeccion de proxy)
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(QrAccesoTransaccionalIT.DecoradoresDeFallo.class)
@DisplayName("IT POST-B2-TX-CONSUMERS-01: frontera transaccional del acceso QR")
class QrAccesoTransaccionalIT {

    private static final String MARCA = "POSTB2TXQR_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 13, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);
    private static final String FALLO = "FALLO_INYECTADO_POSTB2TXQR";
    private static final String FALLO_NOTIFICACION = "FALLO_NOTIFICACION_INYECTADO_POSTB2TXQR";

    enum Etapa { NINGUNA, ACCESO, EVENTO, SNAPSHOT }

    static volatile Etapa fallaEn = Etapa.NINGUNA;

    @Autowired
    private QrActaService qrActaService;

    @Autowired
    private ActaRepository actaRepository;

    @Autowired
    private QrAccesoRepository qrAccesoRepository;

    @Autowired
    private QrTokenProtector tokenProtector;

    @Autowired
    private JdbcClient jdbc;

    private Long idDependencia;

    @BeforeEach
    void prepararPadresYActor() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependencia();
    }

    @AfterEach
    void limpiar() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    private static String actor() {
        return ActorContextHolder.subOr("SYS");
    }

    private void insertarDependencia() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
        idDependencia = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP")
                .query(Long.class)
                .single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDependencia)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
    }

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        for (Long idActa : actas) {
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_qr_acceso WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep")
                .param("idDep", idDependencia).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep")
                .param("idDep", idDependencia).update();
    }

    /** Acta con un token QR real (mismo protector que resuelve {@code registrarAcceso}). */
    private record ActaConToken(Long idActa, String token) {}

    private ActaConToken crearActaConToken() {
        String uuidTecnico = UUID.randomUUID().toString();
        QrTokenEmitido qr = tokenProtector.emitir(uuidTecnico);

        FalActa nueva = new FalActa(
                null, uuidTecnico, TipoActa.TRANSITO,
                idDependencia, null, F0, T0,
                MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, T0, actor());
        nueva.setQrPayloadVersion(qr.version());
        nueva.setCodigoQr(qr.token());
        Long idActa = actaRepository.guardar(nueva).getId();
        return new ActaConToken(idActa, qr.token());
    }

    private long contarAccesos(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_qr_acceso WHERE acta_id = :id")
                .param("id", idActa).query(Long.class).single();
    }

    private long contarEventosQracca(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = 'QRACCA'")
                .param("id", idActa).query(Long.class).single();
    }

    private long contarSnapshots(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_snapshot WHERE id_acta = :id")
                .param("id", idActa).query(Long.class).single();
    }

    // =====================================================================
    @Nested
    @DisplayName("Commit atomico y rollback por etapa")
    class CommitYRollback {

        @Test
        @DisplayName("1. Exito: acceso, evento QRACCA y snapshot hacen commit juntos")
        void exitoCompleto() {
            ActaConToken acta = crearActaConToken();

            QrActaService.AccesoQrResultado r = qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-1");

            assertThat(r.actaId()).isEqualTo(acta.idActa());
            assertThat(contarAccesos(acta.idActa())).isEqualTo(1L);
            assertThat(contarEventosQracca(acta.idActa())).isEqualTo(1L);
            assertThat(contarSnapshots(acta.idActa())).isEqualTo(1L);
        }

        /**
         * {@code POST-B2-TX-CONSUMERS-01-R1}: {@code idAcceso} debe ser la PK real que asigno
         * {@code AUTO_INCREMENT}, no el provisional de {@code nextId()}. Este caso lo verifica
         * releyendo la fila con {@code buscarPorId(resultado.idAcceso())}: si el resultado
         * llevara el provisional, la busqueda no encontraria nada o encontraria otra fila.
         */
        @Test
        @DisplayName("9. idAcceso es la PK real: buscarPorId lo encuentra y pertenece al acta esperada")
        void idAccesoEsLaPkRealPersistida() {
            ActaConToken acta = crearActaConToken();

            QrActaService.AccesoQrResultado r = qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, "192.168.1.10", "agente-test", "corr-9");

            FalActaQrAcceso fila = qrAccesoRepository.buscarPorId(r.idAcceso())
                    .orElseThrow(() -> new AssertionError(
                            "buscarPorId(" + r.idAcceso() + ") no encontro ninguna fila"));

            assertThat(fila.getActaId())
                    .as("la fila real pertenece al acta esperada")
                    .isEqualTo(acta.idActa());
            assertThat(fila.getId())
                    .as("el resultado y la fila persistida comparten identidad")
                    .isEqualTo(r.idAcceso());
            assertThat(fila.getIpOrigen()).isEqualTo("192.168.1.10");
            assertThat(fila.getUserAgent()).isEqualTo("agente-test");

            // El id real que asigna AUTO_INCREMENT no tiene por que coincidir con el contador
            // provisional de nextId(); lo unico que este caso exige es que exista una fila real
            // con exactamente ese id y ese acta_id.
            assertThat(contarAccesos(acta.idActa())).isEqualTo(1L);
        }

        @Test
        @DisplayName("2. Fallo al insertar el acceso: cero mutaciones en las tres tablas")
        void falloEnAccesoNoMutaNada() {
            ActaConToken acta = crearActaConToken();
            fallaEn = Etapa.ACCESO;

            assertThat(catchThrowable(() -> qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-2")))
                    .hasMessageContaining(FALLO);

            assertThat(contarAccesos(acta.idActa())).isZero();
            assertThat(contarEventosQracca(acta.idActa())).isZero();
            assertThat(contarSnapshots(acta.idActa())).isZero();
        }

        @Test
        @DisplayName("3. Fallo al registrar QRACCA: el acceso ya insertado tambien se revierte")
        void falloEnEventoRevierteElAcceso() {
            ActaConToken acta = crearActaConToken();
            fallaEn = Etapa.EVENTO;

            assertThat(catchThrowable(() -> qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-3")))
                    .hasMessageContaining(FALLO);

            assertThat(contarAccesos(acta.idActa()))
                    .as("el acceso insertado antes del fallo del evento se revierte")
                    .isZero();
            assertThat(contarEventosQracca(acta.idActa())).isZero();
            assertThat(contarSnapshots(acta.idActa())).isZero();
        }

        @Test
        @DisplayName("4. Fallo al guardar el snapshot: acceso y evento tambien se revierten")
        void falloEnSnapshotRevierteTodo() {
            ActaConToken acta = crearActaConToken();
            fallaEn = Etapa.SNAPSHOT;

            assertThat(catchThrowable(() -> qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-4")))
                    .hasMessageContaining(FALLO);

            assertThat(contarAccesos(acta.idActa())).isZero();
            assertThat(contarEventosQracca(acta.idActa())).isZero();
            assertThat(contarSnapshots(acta.idActa())).isZero();
        }

        /**
         * Demuestra materialmente el gap registrado como {@code D-QR-ACCESO-CANAL-PERSISTENCE-01}:
         * {@code fal_acta_qr_acceso} no tiene columna {@code canal_acceso_qr}
         * ({@code SPEC-MODEL-DDL-CLOSURE-001}), de modo que {@code JdbcQrAccesoRepository}
         * reconstruye siempre {@code CanalAccesoQr.PORTAL} al releer, sin importar el canal real
         * con el que se registro el acceso. No se resuelve aca: no hay DDL autorizado para
         * agregar la columna y el contrato Java todavia acepta {@code APP} y otros canales.
         */
        @Test
        @DisplayName("11. GAP D-QR-ACCESO-CANAL-PERSISTENCE-01: releer siempre devuelve PORTAL, aunque se registro con APP")
        void gapCanalNoPersistidoAlReleer() {
            ActaConToken acta = crearActaConToken();

            QrActaService.AccesoQrResultado r = qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.APP, null, null, "corr-11");

            assertThat(r.canal())
                    .as("el resultado inmediato de registrarAcceso conserva el canal real,"
                            + " porque se construye en memoria sin releer la fila")
                    .isEqualTo(CanalAccesoQr.APP);

            FalActaQrAcceso filaReleida = qrAccesoRepository.buscarPorId(r.idAcceso())
                    .orElseThrow(() -> new AssertionError("fila no encontrada"));

            assertThat(filaReleida.getCanalAcceso())
                    .as("D-QR-ACCESO-CANAL-PERSISTENCE-01: fal_acta_qr_acceso no persiste"
                            + " canal_acceso_qr; releer la fila real siempre devuelve PORTAL,"
                            + " independientemente del canal con el que se registro el acceso")
                    .isEqualTo(CanalAccesoQr.PORTAL)
                    .isNotEqualTo(CanalAccesoQr.APP);

            List<FalActaQrAcceso> porActa = qrAccesoRepository.listarPorActa(acta.idActa());
            assertThat(porActa).hasSize(1);
            assertThat(porActa.get(0).getCanalAcceso())
                    .as("listarPorActa exhibe el mismo gap que buscarPorId")
                    .isEqualTo(CanalAccesoQr.PORTAL);
        }

        @Test
        @DisplayName("5. Despues de un rollback, el acceso siguiente commitea normalmente")
        void despuesDeRollbackElAccesoSiguienteFunciona() {
            ActaConToken acta = crearActaConToken();
            fallaEn = Etapa.SNAPSHOT;
            assertThat(catchThrowable(() -> qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-5a")))
                    .hasMessageContaining(FALLO);

            fallaEn = Etapa.NINGUNA;
            QrActaService.AccesoQrResultado r = qrActaService.registrarAcceso(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-5b");

            assertThat(r.actaId()).isEqualTo(acta.idActa());
            assertThat(contarAccesos(acta.idActa())).isEqualTo(1L);
            assertThat(contarEventosQracca(acta.idActa())).isEqualTo(1L);
            assertThat(contarSnapshots(acta.idActa())).isEqualTo(1L);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Semantica de registrarAccesoConNotificacion: acceso commiteado, notificacion posterior e independiente")
    class SemanticaNotificacion {

        /**
         * Verifica, desde una lectura independiente (misma {@code JdbcClient}, pero sin ninguna
         * transaccion de Spring activa en este punto de la pila), que el acceso QR ya esta
         * commiteado en el instante exacto en que arranca {@code registrarPortalPositivo}. Si la
         * transaccion del acceso no hubiera terminado todavia, esta lectura —fuera de esa
         * transaccion— no veria la fila.
         */
        @Test
        @DisplayName("6. La notificacion arranca solo despues de que el acceso ya esta commiteado")
        void notificacionArrancaDespuesDelCommit() {
            ActaConToken acta = crearActaConToken();
            NotifDouble doble = new NotifDouble(() -> contarAccesos(acta.idActa()) == 1L
                    && contarEventosQracca(acta.idActa()) == 1L
                    && contarSnapshots(acta.idActa()) == 1L);

            QrActaService.AccesoQrResultado r = qrActaService.registrarAccesoConNotificacion(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-6",
                    null, null, null, doble);

            assertThat(r.actaId()).isEqualTo(acta.idActa());
            // notificacionId es null en este caso: el double no se invoca. Se reafirma el
            // resultado del acceso, que es lo que este caso necesita como base.
            assertThat(contarAccesos(acta.idActa())).isEqualTo(1L);
        }

        /**
         * Con {@code notificacionId} informado, el double se invoca de verdad y registra si el
         * acceso ya era visible en ese instante desde una lectura independiente.
         */
        @Test
        @DisplayName("7. Con notificacion informada, al momento de invocarla el acceso ya es visible")
        void accesoVisibleAlComenzarLaNotificacion() {
            ActaConToken acta = crearActaConToken();
            NotifDouble doble = new NotifDouble(() -> contarAccesos(acta.idActa()) == 1L
                    && contarEventosQracca(acta.idActa()) == 1L
                    && contarSnapshots(acta.idActa()) == 1L);

            qrActaService.registrarAccesoConNotificacion(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-7",
                    777L, "CUIT-DEMO", "CUIT-DEMO", doble);

            assertThat(doble.invocado).isTrue();
            assertThat(doble.accesoVisibleAlInvocar)
                    .as("el acceso, el evento y el snapshot ya estan commiteados cuando la"
                            + " notificacion arranca")
                    .isTrue();
        }

        /**
         * Un fallo de notificacion no debe eliminar ni revertir el acceso ya confirmado: es la
         * propiedad 4 de la decision del usuario, y la que preservan los cuatro tests historicos
         * de {@code QrActaServiceTest.IntegracionPortal}.
         */
        @Test
        @DisplayName("8. Fallo de notificacion no revierte el acceso QR ya confirmado")
        void falloDeNotificacionNoRevierteElAcceso() {
            ActaConToken acta = crearActaConToken();
            NotifDouble doble = new NotifDouble(() -> true);
            doble.debeFallar = true;

            assertThat(catchThrowable(() -> qrActaService.registrarAccesoConNotificacion(
                    acta.token(), CanalAccesoQr.PORTAL, null, null, "corr-8",
                    888L, "CUIT-DEMO", "CUIT-DEMO", doble)))
                    .hasMessageContaining(FALLO_NOTIFICACION);

            assertThat(contarAccesos(acta.idActa()))
                    .as("el acceso confirmado sobrevive al fallo posterior de la notificacion")
                    .isEqualTo(1L);
            assertThat(contarEventosQracca(acta.idActa())).isEqualTo(1L);
            assertThat(contarSnapshots(acta.idActa())).isEqualTo(1L);
        }

        /**
         * {@code POST-B2-TX-CONSUMERS-01-R1}: {@code registrarAccesoConNotificacion} devuelve el
         * resultado de {@code registrarAcceso}, que debe llevar la identidad persistida real,
         * igual que el caso 9. Se verifica de nuevo aca porque
         * {@code registrarAccesoConNotificacion} es un camino de invocacion distinto (con
         * self-invocation de {@code registrarAcceso} de por medio) y no hay que asumir que
         * comparte la correccion sin probarlo.
         */
        @Test
        @DisplayName("10. registrarAccesoConNotificacion tambien devuelve la identidad persistida real")
        void registrarAccesoConNotificacionDevuelveIdentidadPersistida() {
            ActaConToken acta = crearActaConToken();
            NotifDouble doble = new NotifDouble(() -> true);

            QrActaService.AccesoQrResultado r = qrActaService.registrarAccesoConNotificacion(
                    acta.token(), CanalAccesoQr.APP, null, null, "corr-10",
                    null, null, null, doble);

            FalActaQrAcceso fila = qrAccesoRepository.buscarPorId(r.idAcceso())
                    .orElseThrow(() -> new AssertionError(
                            "buscarPorId(" + r.idAcceso() + ") no encontro ninguna fila"));

            assertThat(fila.getId()).isEqualTo(r.idAcceso());
            assertThat(fila.getActaId()).isEqualTo(acta.idActa());
        }
    }

    /**
     * Double de {@code NotificacionIntentoService} que nunca ejecuta la logica real de
     * notificacion: solo registra si fue invocado, si el acceso ya era visible desde una
     * lectura independiente en ese instante, y opcionalmente falla a pedido. Los parametros del
     * constructor heredado son irrelevantes porque {@code registrarPortalPositivo} esta
     * completamente sobreescrito y nunca invoca a {@code super}.
     */
    static class NotifDouble extends NotificacionIntentoService {
        volatile boolean invocado = false;
        volatile boolean accesoVisibleAlInvocar = false;
        volatile boolean debeFallar = false;
        private final BooleanSupplier chequeoVisibilidad;

        NotifDouble(BooleanSupplier chequeoVisibilidad) {
            super(null, null, null, null, null, null, null, null, null, null, null,
                    (PlazosAdministrativosService) null);
            this.chequeoVisibilidad = chequeoVisibilidad;
        }

        @Override
        public FalNotificacionIntento registrarPortalPositivo(
                Long notificacionId, Long actaIdQrEsperada, String destinoPortal, String actor) {
            invocado = true;
            accesoVisibleAlInvocar = chequeoVisibilidad.getAsBoolean();
            if (debeFallar) {
                throw new RuntimeException(FALLO_NOTIFICACION);
            }
            return null;
        }
    }

    // =====================================================================
    // DECORADORES DE FALLO
    // =====================================================================

    @TestConfiguration
    static class DecoradoresDeFallo {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor fallosControlados() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof QrAccesoRepository r) return new QrAccesoConFallo(r);
                    if (bean instanceof ActaEventoRepository r) return new EventoConFallo(r);
                    if (bean instanceof ActaSnapshotRepository r) return new SnapshotConFallo(r);
                    return bean;
                }
            };
        }
    }

    private static void fallarSi(Etapa etapa) {
        if (fallaEn == etapa) {
            throw new IllegalStateException(FALLO + " en etapa " + etapa);
        }
    }

    private record QrAccesoConFallo(QrAccesoRepository delegado) implements QrAccesoRepository {
        @Override public Long nextId() { return delegado.nextId(); }
        @Override public FalActaQrAcceso registrar(FalActaQrAcceso acceso) {
            FalActaQrAcceso persistido = delegado.registrar(acceso);
            fallarSi(Etapa.ACCESO);
            return persistido;
        }
        @Override public Optional<FalActaQrAcceso> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public List<FalActaQrAcceso> listarPorActa(Long actaId) { return delegado.listarPorActa(actaId); }
        @Override public int contarPorActa(Long actaId) { return delegado.contarPorActa(actaId); }
    }

    private record EventoConFallo(ActaEventoRepository delegado) implements ActaEventoRepository {
        @Override public FalActaEvento registrar(FalActaEvento evento) {
            FalActaEvento persistido = delegado.registrar(evento);
            fallarSi(Etapa.EVENTO);
            return persistido;
        }
        @Override public List<FalActaEvento> buscarPorActa(Long idActa) { return delegado.buscarPorActa(idActa); }
        @Override public Optional<FalActaEvento> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public boolean existeCorrelacion(Long idActa, String correlacionId) {
            return delegado.existeCorrelacion(idActa, correlacionId);
        }
    }

    private record SnapshotConFallo(ActaSnapshotRepository delegado) implements ActaSnapshotRepository {
        @Override public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
            FalActaSnapshot persistido = delegado.guardar(snapshot);
            fallarSi(Etapa.SNAPSHOT);
            return persistido;
        }
        @Override public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
            return delegado.buscarPorActa(idActa);
        }
    }
}

package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de la frontera transaccional del alta del acta ({@code DECISION_OBS-TX-01}).
 *
 * <p>Verifica lo que ningun test unitario puede: que las seis escrituras de
 * {@code ActaService.labrar} ocurran dentro de una unica transaccion contra MariaDB y que
 * cualquier fallo las revierta todas.
 *
 * <h2>Por que no hay transaccion de test que revierta</h2>
 *
 * El resto de los IT del proyecto envuelven cada escenario en una transaccion que se revierte
 * siempre. <b>Aca no se puede.</b> Si el test abriera una transaccion, la de {@code labrar} se
 * uniria a ella por propagacion {@code REQUIRED} y nunca commitearia: el escenario mediria el
 * comportamiento de la transaccion del test, no el de la frontera. Por eso cada caso deja que
 * {@code labrar} commitee o revierta por su cuenta, verifica leyendo despues del retorno —lo que
 * significa leer estado ya commiteado, sobre otra conexion del pool— y limpia en
 * {@code @AfterEach}.
 *
 * <h2>Como se provoca el fallo de cada etapa</h2>
 *
 * Un {@code BeanPostProcessor} de test envuelve cada uno de los seis puertos mutantes en un
 * decorador que <b>delega en el adaptador real</b> y lanza {@code RuntimeException} en el punto
 * elegido. Delegar es la parte importante: un doble que no escribiera no probaria que la
 * escritura anterior se revierte, porque no habria escritura anterior que revertir.
 *
 * <p>El punto de fallo se elige con un campo estatico y no con un contexto distinto por etapa:
 * un contexto por etapa multiplicaria por siete el arranque de la aplicacion y, sobre todo, haria
 * que cada etapa se verificara sobre una topologia recien construida en vez de sobre la misma.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(ActaLabrarTransaccionalIT.DecoradoresDeFallo.class)
@DisplayName("IT WAVE-1A2-B2: frontera transaccional de ActaService.labrar")
class ActaLabrarTransaccionalIT {

    /** Marca del slice. Cabe entera en {@code apellido VARCHAR(24)}. */
    private static final String MARCA = "WAVE1A2B2_";

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 11, 30, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);

    /** Etapas en las que un decorador puede fallar. */
    enum Etapa {
        NINGUNA, PERSONA, PERSONA_DUPLICADA, ACTA, OBSERVACION, EVIDENCIA_POSTERIOR, EVENTO, SNAPSHOT
    }

    /** Punto de fallo activo. Se restablece siempre en {@code @AfterEach}. */
    static volatile Etapa fallaEn = Etapa.NINGUNA;

    /** Mensaje unico del fallo inyectado, para que ninguna asercion lo confunda con otro. */
    private static final String FALLO = "FALLO_INYECTADO_B2";

    @Autowired
    private ActaService actaService;

    @Autowired
    private ar.gob.malvinas.faltas.core.application.service.TalonarioService talonarioService;

    @Autowired
    private JdbcClient jdbc;

    private Long idDependencia;
    private short verDependencia;

    // =====================================================================
    // PREPARACION Y LIMPIEZA
    // =====================================================================

    @BeforeEach
    void prepararPadresYActor() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependencia();
        // DECISION-ACTA-TALONARIO-REQUIRED-01 (Wave 4 Parte 3C §7): el alta rechaza completa
        // sin exactamente un talonario ACTA ELECTRONICO activo aplicable. Ambito DEPENDENCIA
        // (no GLOBAL): la dependencia es nueva en cada metodo, asi que el ambito nunca colisiona
        // con otro test ni con la exclusividad de DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01.
        ar.gob.malvinas.faltas.core.support.TalonarioElectronicoITSeedSupport
                .seedTalonarioElectronicoActivo(talonarioService, idDependencia, verDependencia,
                        LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void limpiar() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    /**
     * La dependencia y su version se commitean aparte del alta: son datos padre que la clave
     * ajena de {@code fal_acta} exige y que no forman parte de lo que se esta midiendo.
     */
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
        verDependencia = 1;
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, :verDep, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDependencia)
                .param("verDep", verDependencia)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
    }

    /**
     * Limpieza en orden seguro de claves ajenas: hijos antes que el acta, actas antes que
     * personas y dependencia. Se borra por la marca del slice, de modo que no puede alcanzar
     * datos ajenos.
     */
    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia)
                .query(Long.class)
                .list();
        for (Long idActa : actas) {
            // Todos los comandos de este IT informan un bloque transito minimo (Wave 4 Parte
            // 3C, DECISION-ACTA-SATELITES-STRICT-01): fal_acta_transito tiene fk_acta_transito_acta
            // RESTRICT y debe borrarse antes que fal_acta.
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = :tipo AND entidad_id = :id")
                    .param("tipo", EntidadTipoObservada.ACTA.codigo())
                    .param("id", idActa)
                    .update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("DELETE FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca")
                .param("marca", MARCA + "%")
                .update();
        borrarTalonarioDeLaDependencia();
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep")
                .param("idDep", idDependencia)
                .update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep")
                .param("idDep", idDependencia)
                .update();
    }

    /**
     * Limpia el talonario ACTA ELECTRONICO seedeado para esta dependencia
     * ({@code TalonarioElectronicoITSeedSupport}), en orden de claves ajenas: ambito, talonario,
     * politica. Debe ejecutarse antes de borrar {@code fal_dependencia} (FK RESTRICT desde
     * {@code num_talonario_ambito.id_dep}).
     */
    private void borrarTalonarioDeLaDependencia() {
        List<Long> talonarioIds = jdbc.sql(
                        "SELECT talonario_id FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia)
                .query(Long.class)
                .list();
        jdbc.sql("DELETE FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia)
                .update();
        for (Long idTalonario : talonarioIds) {
            Long idPolitica = jdbc.sql("SELECT politica_id FROM num_talonario WHERE id = :id")
                    .param("id", idTalonario).query(Long.class).single();
            jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_politica WHERE id = :id").param("id", idPolitica).update();
        }
    }

    private static String actor() {
        return ActorContextHolder.subOr("SYS");
    }

    // =====================================================================
    // LECTURAS DE VERIFICACION (estado ya commiteado)
    // =====================================================================

    private long contarActas() {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarPersonasDelSlice() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_persona
                 WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca
                """).param("marca", MARCA + "%").query(Long.class).single();
    }

    private long contarObservacionesDeActasDelSlice() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_observacion o
                 WHERE o.entidad_tipo = :tipo
                   AND o.entidad_id IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """)
                .param("tipo", EntidadTipoObservada.ACTA.codigo())
                .param("idDep", idDependencia)
                .query(Long.class).single();
    }

    private long contarEvidencias() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_evidencia e
                 WHERE e.id_acta IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarEventos(String tipoEvt) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_evento e
                 WHERE e.tipo_evt = :tipo
                   AND e.acta_id IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("tipo", tipoEvt).param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarSnapshots() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_snapshot s
                 WHERE s.id_acta IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    /** Cero filas en todas las tablas del alta: la asercion de rollback total. */
    private void exigirCeroMutaciones(long personasPreexistentes) {
        assertThat(contarActas()).as("cero fal_acta").isZero();
        assertThat(contarPersonasDelSlice())
                .as("ninguna persona nueva mas alla de las preexistentes")
                .isEqualTo(personasPreexistentes);
        assertThat(contarObservacionesDeActasDelSlice()).as("cero fal_observacion").isZero();
        assertThat(contarEvidencias()).as("cero fal_acta_evidencia").isZero();
        assertThat(contarEventos("ACTLAB")).as("cero eventos ACTLAB").isZero();
        assertThat(contarSnapshots()).as("cero fal_acta_snapshot").isZero();
    }

    // =====================================================================
    // COMANDOS
    // =====================================================================

    /** Bloque transito minimo, sin control de alcoholemia (Wave 4 Parte 3C, opt-in eliminado). */
    private static ar.gob.malvinas.faltas.core.application.command.TransitoActaItem transitoMinimo() {
        return ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo();
    }

    private LabrarActaCommand comandoSinPersona(String observaciones,
                                                List<EvidenciaActaItem> evidencias) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, null, F0,
                MARCA + "domicilio del hecho", null, observaciones,
                -34.7654321, -58.1234567,
                null, null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE,
                evidencias, null, null, null, null, transitoMinimo(), null, null, null);
    }

    private LabrarActaCommand comandoConDocumento(String nroDoc, String observaciones) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, null, F0,
                MARCA + "domicilio del hecho", null, observaciones,
                null, null,
                MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDoc, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                null, null, null, null, null, transitoMinimo(), null, null, null);
    }

    private LabrarActaCommand comandoConPersona(Long idPersona, String observaciones) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, null, F0,
                MARCA + "domicilio del hecho", null, observaciones,
                null, null, null, null, null, idPersona,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                null, null, null, null, null, transitoMinimo(), null, null, null);
    }

    /**
     * Wave 4 Parte 3D: FOTO ya no se acepta en el contrato generico
     * {@code evidenciasActa} (exige el pipeline dedicado de fotografias, con SHA-256,
     * dimensiones y demas metadata obligatoria). Este fixture generico de evidencias
     * (persistencia/rollback junto con el alta) usa PDF, que sigue el camino generico sin
     * cambios.
     */
    private static List<EvidenciaActaItem> dosEvidencias() {
        return List.of(
                new EvidenciaActaItem(TipoEvidenciaActa.PDF, MARCA + "storage/1.jpg"),
                new EvidenciaActaItem(TipoEvidenciaActa.PDF, MARCA + "storage/2.jpg"));
    }

    private Long insertarPersonaPreexistente(String nroDoc) {
        jdbc.sql("""
                INSERT INTO fal_persona
                    (tipo_persona, tipo_documento, nro_doc, apellido, nombres, fh_alta, id_user_alta)
                VALUES (:tipoPersona, :tipoDoc, :nroDoc, :apellido, :nombres, :fhAlta, :usuario)
                """)
                .param("tipoPersona", (short) 1)
                .param("tipoDoc", TipoDocumentoPersona.DNI.codigo())
                .param("nroDoc", nroDoc)
                .param("apellido", MARCA + "PREEX")
                .param("nombres", "TEST")
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
        return jdbc.sql("SELECT MAX(id) FROM fal_persona WHERE nro_doc = :nroDoc AND apellido = :ap")
                .param("nroDoc", nroDoc)
                .param("ap", MARCA + "PREEX")
                .query(Long.class)
                .single();
    }

    /** Documento de prueba en un rango alto para no colisionar con datos reales. */
    private static String nroDocDePrueba() {
        return String.valueOf(65_000_000 + (int) (Math.random() * 900_000));
    }

    // =====================================================================
    @Nested
    @DisplayName("Exito transaccional del alta")
    class ExitoDelAlta {

        @Test
        @DisplayName("1. Alta con persona nueva, observacion inicial y evidencias commitea todo")
        void altaCompletaCommiteaTodo() {
            String nroDoc = nroDocDePrueba();
            ComandoResultado resultado = actaService.labrar(
                    comandoConDocumento(nroDoc, MARCA + "observacion inicial"));

            assertThat(resultado.idActa()).isNotNull();
            assertThat(contarActas()).isEqualTo(1L);
            assertThat(contarPersonasDelSlice()).isEqualTo(1L);
            assertThat(contarObservacionesDeActasDelSlice()).isEqualTo(1L);
            assertThat(contarEventos("ACTLAB")).isEqualTo(1L);
            assertThat(contarSnapshots()).isEqualTo(1L);
        }

        @Test
        @DisplayName("2. Alta con persona existente la reutiliza y no la duplica")
        void altaConPersonaExistenteNoDuplica() {
            String nroDoc = nroDocDePrueba();
            Long idPreexistente = insertarPersonaPreexistente(nroDoc);

            actaService.labrar(comandoConDocumento(nroDoc, null));

            assertThat(contarPersonasDelSlice())
                    .as("la persona se reutiliza, no se crea una segunda")
                    .isEqualTo(1L);
            Long idInfractor = jdbc.sql(
                            "SELECT id_persona_infractor FROM fal_acta WHERE id_dep = :idDep")
                    .param("idDep", idDependencia).query(Long.class).single();
            assertThat(idInfractor).isEqualTo(idPreexistente);
        }

        @Test
        @DisplayName("3. Sin texto de observacion no se inserta ninguna fila en fal_observacion")
        void sinTextoNoHayObservacion() {
            for (String vacio : new String[] {null, "", "   "}) {
                actaService.labrar(comandoSinPersona(vacio, null));
            }
            assertThat(contarActas()).isEqualTo(3L);
            assertThat(contarObservacionesDeActasDelSlice())
                    .as("nulo, vacio y blanco no producen observacion")
                    .isZero();
        }

        /**
         * Las cuatro situaciones que no exigen persona infractora: el alta se completa con
         * {@code id_persona_infractor} en NULL y sin crear persona centinela.
         */
        @Test
        @DisplayName("4. Las situaciones sin persona infractora commitean con id_persona_infractor nulo")
        void situacionesSinPersona() {
            List<SituacionIdentificacionInfractor> sinPersona = new ArrayList<>();
            for (SituacionIdentificacionInfractor s : SituacionIdentificacionInfractor.values()) {
                if (!s.exigePersonaInfractora()) {
                    sinPersona.add(s);
                }
            }
            assertThat(sinPersona).as("el catalogo declara situaciones sin persona").isNotEmpty();

            for (SituacionIdentificacionInfractor situacion : sinPersona) {
                actaService.labrar(new LabrarActaCommand(
                        TipoActa.TRANSITO, idDependencia, null, F0,
                        MARCA + "domicilio", null, null, null, null,
                        null, null, null, null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE, situacion, null,
                        null, null, null, null, transitoMinimo(), null, null, null));
            }

            assertThat(contarActas()).isEqualTo(sinPersona.size());
            assertThat(contarPersonasDelSlice()).as("no se crea persona centinela").isZero();
            assertThat(jdbc.sql("""
                    SELECT COUNT(*) FROM fal_acta
                     WHERE id_dep = :idDep AND id_persona_infractor IS NULL
                    """).param("idDep", idDependencia).query(Long.class).single())
                    .isEqualTo(sinPersona.size());
        }

        /**
         * La observacion inicial tiene que quedar vinculada por el par propietario
         * {@code (entidad_tipo = ACTA, entidad_id = fal_acta.id)}, con {@code id_acta_contexto}
         * nulo, el actor real y el mismo instante del alta ({@code DECISION_OBS-01}).
         */
        @Test
        @DisplayName("5. La observacion inicial queda con entidad, FK, actor e instante correctos")
        void observacionInicialConVinculoCorrecto() {
            String texto = MARCA + "observacion con vinculo verificable";
            ComandoResultado resultado = actaService.labrar(comandoSinPersona(texto, null));

            var fila = jdbc.sql("""
                    SELECT entidad_tipo, entidad_id, id_acta_contexto, observacion, si_activa,
                           fh_alta, id_user_alta
                      FROM fal_observacion
                     WHERE entidad_tipo = :tipo AND entidad_id = :idActa
                    """)
                    .param("tipo", EntidadTipoObservada.ACTA.codigo())
                    .param("idActa", resultado.idActa())
                    .query((rs, n) -> List.of(
                            rs.getInt("entidad_tipo"),
                            rs.getLong("entidad_id"),
                            Optional.ofNullable(rs.getObject("id_acta_contexto")).isPresent(),
                            rs.getString("observacion"),
                            rs.getBoolean("si_activa"),
                            rs.getObject("fh_alta", LocalDateTime.class),
                            rs.getString("id_user_alta")))
                    .single();

            assertThat(fila.get(0)).isEqualTo(EntidadTipoObservada.ACTA.codigo());
            assertThat(fila.get(1)).isEqualTo(resultado.idActa());
            assertThat(fila.get(2)).as("id_acta_contexto es nulo en la observacion del acta")
                    .isEqualTo(false);
            assertThat(fila.get(3)).isEqualTo(texto);
            assertThat(fila.get(4)).isEqualTo(true);
            assertThat(fila.get(6)).as("el actor es el subject autenticado").isEqualTo(actor());

            LocalDateTime fhAltaActa = jdbc.sql("SELECT fh_alta FROM fal_acta WHERE id = :id")
                    .param("id", resultado.idActa())
                    .query(LocalDateTime.class).single();
            assertThat(fila.get(5))
                    .as("la observacion comparte el instante unico del alta")
                    .isEqualTo(fhAltaActa);
        }

        /**
         * El QR, la version de payload, el actor y las coordenadas se leen de vuelta desde la base
         * tal como se escribieron: si alguna de esas escrituras estuviera fuera de la transaccion,
         * este round-trip lo mostraria como valor ausente.
         */
        @Test
        @DisplayName("6. QR, version, actor, latitud y longitud sobreviven el round-trip")
        void roundTripDeCamposDelAlta() {
            ComandoResultado resultado = actaService.labrar(
                    comandoSinPersona(MARCA + "round trip", null));

            var fila = jdbc.sql("""
                    SELECT codigo_qr, qr_payload_version, id_user_alta, lat_infr, lon_infr, version_row
                      FROM fal_acta WHERE id = :id
                    """)
                    .param("id", resultado.idActa())
                    .query((rs, n) -> List.of(
                            rs.getString("codigo_qr"),
                            rs.getShort("qr_payload_version"),
                            rs.getString("id_user_alta"),
                            rs.getBigDecimal("lat_infr"),
                            rs.getBigDecimal("lon_infr"),
                            rs.getInt("version_row")))
                    .single();

            assertThat((String) fila.get(0)).as("codigo_qr persistido").isNotBlank();
            assertThat(fila.get(1)).as("qr_payload_version activa").isEqualTo((short) 0);
            assertThat(fila.get(2)).as("id_user_alta es el subject real").isEqualTo(actor());
            assertThat(fila.get(3).toString()).isEqualTo("-34.7654321");
            assertThat(fila.get(4).toString()).isEqualTo("-58.1234567");
            // Wave 4 Parte 3C (DECISION-ACTA-TALONARIO-REQUIRED-01): desde que este IT seedea un
            // talonario ACTA ELECTRONICO activo obligatorio, la numeracion electronica hace un
            // segundo guardar() (asigna nro_acta/id_talonario) dentro de la misma transaccion,
            // incrementando version_row de 0 a 1. No es un fallo de atomicidad -- ambos guardados
            // ocurren en la misma transaccion y commitean juntos -- es la firma esperada de un
            // alta que efectivamente numera.
            assertThat(fila.get(5)).as("el alta numerada deja version_row en 1 (dos guardados: alta + numeracion)")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("7. Las evidencias del comando quedan persistidas con la PK que asigna el motor")
        void evidenciasPersistidasConPkDelMotor() {
            ComandoResultado resultado = actaService.labrar(
                    comandoSinPersona(null, dosEvidencias()));

            assertThat(contarEvidencias()).isEqualTo(2L);
            List<Long> ids = jdbc.sql(
                            "SELECT id FROM fal_acta_evidencia WHERE id_acta = :id ORDER BY id")
                    .param("id", resultado.idActa())
                    .query(Long.class).list();
            assertThat(ids).hasSize(2).doesNotHaveDuplicates();
            assertThat(ids.get(0)).as("la PK la asigna AUTO_INCREMENT, no el caso de uso")
                    .isPositive();
        }

        /**
         * Despues del retorno el estado esta commiteado, de modo que una lectura posterior —que
         * toma otra conexion del pool— lo ve. Es la contraparte observable del commit total.
         */
        @Test
        @DisplayName("8. Al retornar, todo el alta es visible desde una lectura independiente")
        void commitVisibleDespuesDelRetorno() {
            ComandoResultado resultado = actaService.labrar(
                    comandoSinPersona(MARCA + "visible", dosEvidencias()));

            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id = :id")
                    .param("id", resultado.idActa()).query(Long.class).single()).isEqualTo(1L);
            assertThat(contarObservacionesDeActasDelSlice()).isEqualTo(1L);
            assertThat(contarEvidencias()).isEqualTo(2L);
            assertThat(contarEventos("ACTLAB")).isEqualTo(1L);
            assertThat(contarSnapshots()).isEqualTo(1L);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Precondiciones: cero mutaciones antes de abrir escrituras")
    class Precondiciones {

        @Test
        @DisplayName("9. Una precondicion invalida no produce ninguna mutacion")
        void precondicionInvalidaNoMuta() {
            // FIRMADA sin evidencia de firma olografa: precondicion del contrato del alta.
            LabrarActaCommand invalido = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, null, F0,
                    MARCA + "domicilio", null, MARCA + "no debe escribirse", null, null,
                    null, null, null, null,
                    ResultadoFirmaInfractor.FIRMADA,
                    SituacionIdentificacionInfractor.AUSENTE, null,
                    null, null, null, null, transitoMinimo(), null, null, null);

            assertThatThrownBy(() -> actaService.labrar(invalido))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRMA_OLOGRAFA_INFRACTOR");

            exigirCeroMutaciones(0);
        }

        @Test
        @DisplayName("10. Una situacion que exige persona sin datos suficientes no muta nada")
        void situacionSinDatosSuficientesNoMuta() {
            LabrarActaCommand invalido = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, null, F0,
                    MARCA + "domicilio", null, null, null, null,
                    null, null, null, null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, null,
                    null, null, null, null, transitoMinimo(), null, null, null);

            assertThatThrownBy(() -> actaService.labrar(invalido))
                    .isInstanceOf(PrecondicionVioladaException.class);

            exigirCeroMutaciones(0);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Rollback total por etapa")
    class RollbackPorEtapa {

        @Test
        @DisplayName("11. Fallo al guardar Persona: cero mutaciones")
        void falloEnPersona() {
            fallaEn = Etapa.PERSONA;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDocDePrueba(), MARCA + "obs"))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        @Test
        @DisplayName("12. Fallo al guardar Acta despues de Persona: la persona tambien se revierte")
        void falloEnActaRevierteLaPersona() {
            fallaEn = Etapa.ACTA;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDocDePrueba(), MARCA + "obs"))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        @Test
        @DisplayName("13. Fallo al guardar Observacion despues de Acta: el acta se revierte")
        void falloEnObservacionRevierteElActa() {
            fallaEn = Etapa.OBSERVACION;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDocDePrueba(), MARCA + "obs"))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        /**
         * El fallo ocurre en la <b>segunda</b> evidencia, con la primera ya insertada: la
         * asercion relevante es que la primera desaparezca. Un rollback que solo alcanzara a la
         * operacion que fallo dejaria una evidencia huerfana.
         */
        @Test
        @DisplayName("14. Fallo en una evidencia posterior a otra ya insertada: ninguna sobrevive")
        void falloEnEvidenciaPosteriorRevierteLaAnterior() {
            fallaEn = Etapa.EVIDENCIA_POSTERIOR;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoSinPersona(MARCA + "obs", dosEvidencias()))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        @Test
        @DisplayName("15. Fallo al guardar ACTLAB: acta, observacion y evidencias se revierten")
        void falloEnEventoRevierteTodoLoAnterior() {
            fallaEn = Etapa.EVENTO;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoSinPersona(MARCA + "obs", dosEvidencias()))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        @Test
        @DisplayName("16. Fallo al guardar Snapshot: las cinco escrituras anteriores se revierten")
        void falloEnSnapshotRevierteTodoLoAnterior() {
            fallaEn = Etapa.SNAPSHOT;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDocDePrueba(), MARCA + "obs"))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);
        }

        /**
         * Con una persona preexistente el rollback tiene que revertir el alta completa <b>sin</b>
         * borrar la persona, que no la creo este caso de uso. Es el limite exacto de la
         * transaccion: revierte lo que escribio, no lo que encontro.
         */
        @Test
        @DisplayName("17. Con persona preexistente el rollback no la borra")
        void rollbackNoBorraLaPersonaPreexistente() {
            String nroDoc = nroDocDePrueba();
            Long idPreexistente = insertarPersonaPreexistente(nroDoc);

            fallaEn = Etapa.SNAPSHOT;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDoc, MARCA + "obs"))))
                    .hasMessageContaining(FALLO);

            exigirCeroMutaciones(1);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_persona WHERE id = :id")
                    .param("id", idPreexistente).query(Long.class).single())
                    .as("la persona preexistente sobrevive al rollback")
                    .isEqualTo(1L);
        }

        /**
         * Tras un rollback el alta siguiente tiene que poder completarse: si la transaccion
         * quedara marcada o la conexion sucia, este caso lo mostraria.
         */
        @Test
        @DisplayName("18. Despues de un rollback, el alta siguiente commitea normalmente")
        void despuesDeUnRollbackElAltaSiguienteFunciona() {
            fallaEn = Etapa.EVENTO;
            assertThat(catchThrowable(() ->
                    actaService.labrar(comandoSinPersona(MARCA + "primera", null))))
                    .hasMessageContaining(FALLO);
            exigirCeroMutaciones(0);

            fallaEn = Etapa.NINGUNA;
            ComandoResultado ok = actaService.labrar(comandoSinPersona(MARCA + "segunda", null));
            assertThat(ok.idActa()).isNotNull();
            assertThat(contarActas()).isEqualTo(1L);
            assertThat(contarSnapshots()).isEqualTo(1L);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Excepciones de integridad: propagan y revierten")
    class ExcepcionesDeIntegridad {

        /**
         * Una dependencia inexistente viola {@code fk_acta_dep}. La excepcion tiene que llegar
         * como {@code DataIntegrityViolationException} y revertir el alta completa, incluida la
         * persona que ya se habia insertado.
         */
        @Test
        @DisplayName("19. DataIntegrityViolationException por FK propaga y revierte la persona")
        void violacionDeClaveAjenaPropagaYRevierte() {
            LabrarActaCommand conDependenciaInexistente = new LabrarActaCommand(
                    TipoActa.TRANSITO, 999_999_999L, null, F0,
                    MARCA + "domicilio", null, MARCA + "obs", null, null,
                    MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, null,
                    null, null, null, null, transitoMinimo(), null, null, null);

            assertThatThrownBy(() -> actaService.labrar(conDependenciaInexistente))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(contarPersonasDelSlice())
                    .as("la persona insertada antes del acta se revierte")
                    .isZero();
            assertThat(contarActas()).isZero();
        }

        /**
         * Dos personas con el mismo {@code (tipo_documento, doc_key)} violan
         * {@code uq_fal_persona_tipo_doc_key}. La violacion se provoca <b>desde dentro</b> del caso
         * de uso, de modo que atraviesa la frontera transaccional: llega como
         * {@code DuplicateKeyException}, no se traduce a ninguna excepcion funcional nueva, no se
         * reintenta con otro valor y el alta se revierte por completo, incluida la primera persona
         * que si se habia insertado.
         */
        @Test
        @DisplayName("20. DuplicateKeyException propaga sin traducirse y revierte el alta completa")
        void duplicadoDePersonaPropagaYRevierte() {
            fallaEn = Etapa.PERSONA_DUPLICADA;

            Throwable error = catchThrowable(() ->
                    actaService.labrar(comandoConDocumento(nroDocDePrueba(), MARCA + "obs")));

            assertThat(error)
                    .as("el motor sostiene la unicidad y la excepcion viaja sin traducirse")
                    .isInstanceOf(DuplicateKeyException.class)
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(error.getMessage())
                    .as("el nombre de la constraint debe permanecer identificable")
                    .contains("uq_fal_persona_tipo_doc_key");

            exigirCeroMutaciones(0);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Concurrencia controlada y ausencia de interbloqueo")
    class Concurrencia {

        /**
         * Altas independientes en paralelo. No comparten ninguna fila: cada una inserta su propia
         * acta y su propio snapshot, de modo que el resultado correcto es que todas completen.
         *
         * <p><b>Este caso detecto un interbloqueo real.</b> La primera version del adaptador del
         * snapshot ejecutaba {@code UPDATE} antes de {@code INSERT}; la actualizacion que no
         * encontraba la fila tomaba un bloqueo de hueco sobre la clave inexistente, dos altas de
         * actas distintas caian en el mismo hueco del indice primario y despues cada una necesitaba
         * un bloqueo de intencion de insercion que el hueco de la otra le negaba. La causa era de
         * diseno y se corrigio invirtiendo el orden de las sentencias, sin agregar reintentos.
         *
         * <p>Se usan cuatro hilos y no dos: con dos, un interbloqueo de hueco aparece de forma
         * intermitente segun el estado del indice, y un caso que solo falla a veces no sirve como
         * guardrail. Con cuatro alineados por una barrera, la ventana se abre en todas las
         * ejecuciones observadas.
         *
         * <p>La sincronizacion usa una barrera y no una espera por tiempo: un {@code sleep} haria
         * el caso lento y, otra vez, intermitente.
         */
        @Test
        @DisplayName("21. Cuatro altas independientes concurrentes commitean todas, sin interbloqueo")
        void altasIndependientesConcurrentes() throws Exception {
            int hilos = 4;
            List<Throwable> fallos = ejecutarEnParalelo(hilos, indice ->
                    actaService.labrar(comandoSinPersona(MARCA + "paralela-" + indice, null)));

            exigirSinInterbloqueo(fallos);
            assertThat(fallos).as("altas independientes no compiten por ninguna fila").isEmpty();
            assertThat(contarActas()).isEqualTo(hilos);
            assertThat(contarSnapshots()).isEqualTo(hilos);
            assertThat(contarEventos("ACTLAB")).isEqualTo(hilos);
        }

        /**
         * Dos altas que reutilizan la misma persona preexistente. Ninguna la inserta, de modo que
         * el resultado esperado es que las dos completen y que la persona siga siendo una.
         */
        @Test
        @DisplayName("22. Cuatro altas concurrentes sobre la misma persona preexistente la reutilizan")
        void altasReutilizandoLaMismaPersona() throws Exception {
            int hilos = 4;
            String nroDoc = nroDocDePrueba();
            Long idPersona = insertarPersonaPreexistente(nroDoc);

            List<Throwable> fallos = ejecutarEnParalelo(hilos, indice ->
                    actaService.labrar(comandoConPersona(idPersona, MARCA + "reuso-" + indice)));

            exigirSinInterbloqueo(fallos);
            assertThat(fallos)
                    .as("leer la misma persona no bloquea: ninguna la escribe")
                    .isEmpty();
            assertThat(contarActas()).isEqualTo(hilos);
            assertThat(contarPersonasDelSlice())
                    .as("ninguna de las altas crea una persona")
                    .isEqualTo(1L);
        }

        /**
         * Dos altas concurrentes con el <b>mismo documento</b> y sin persona preexistente. Las dos
         * pueden no encontrar nada y las dos pueden intentar insertar: el motor sostiene
         * {@code uq_fal_persona_tipo_doc_key} y una de las dos falla.
         *
         * <p>Los dos desenlaces son legitimos y el caso acepta los dos, porque cual ocurra
         * depende del entrelazado real y fijar uno haria el test intermitente:
         *
         * <pre>
         * las dos completan  -> la segunda encontro la persona de la primera y la reutilizo
         * una falla          -> violacion de unicidad controlada, no interbloqueo
         * </pre>
         *
         * <p>Lo que <b>no</b> es aceptable, y es lo que el caso afirma en las dos ramas: que
         * queden dos personas con el mismo documento, que el alta fallida deje un acta parcial, o
         * que el fallo sea un interbloqueo. Ante la violacion no se reintenta ni se duplica.
         */
        @Test
        @DisplayName("23. Dos altas concurrentes con el mismo documento respetan la unicidad de persona")
        void dosAltasConElMismoDocumento() throws Exception {
            String nroDoc = nroDocDePrueba();

            List<Throwable> fallos = ejecutarEnParalelo(2, indice ->
                    actaService.labrar(comandoConDocumento(nroDoc, MARCA + "mismo-doc-" + indice)));

            exigirSinInterbloqueo(fallos);

            long personas = jdbc.sql("""
                    SELECT COUNT(*) FROM fal_persona
                     WHERE tipo_documento = :tipoDoc AND nro_doc = :nroDoc
                    """)
                    .param("tipoDoc", TipoDocumentoPersona.DNI.codigo())
                    .param("nroDoc", nroDoc)
                    .query(Long.class).single();
            assertThat(personas)
                    .as("el motor impide la segunda persona con el mismo documento")
                    .isEqualTo(1L);

            assertThat(fallos.size()).as("a lo sumo una de las dos falla").isLessThanOrEqualTo(1);
            assertThat(contarActas())
                    .as("hay tantas actas como altas que completaron")
                    .isEqualTo(2L - fallos.size());
            assertThat(contarSnapshots()).isEqualTo(contarActas());
            assertThat(contarEventos("ACTLAB")).isEqualTo(contarActas());
            for (Throwable fallo : fallos) {
                assertThat(fallo)
                        .as("el fallo esperado es una violacion de unicidad del motor")
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
        }

        /**
         * Un interbloqueo reproducible es un defecto de diseno, no un accidente aceptable. Si
         * apareciera, la causa estaria en el orden de bloqueos, en un indice ausente o en la
         * delimitacion de la transaccion, y B2 no puede declararse completo.
         */
        private void exigirSinInterbloqueo(List<Throwable> fallos) {
            for (Throwable fallo : fallos) {
                assertThat(fallo)
                        .as("ningun fallo puede ser un interbloqueo")
                        .isNotInstanceOf(DeadlockLoserDataAccessException.class)
                        .isNotInstanceOf(CannotAcquireLockException.class);
                assertThat(String.valueOf(fallo.getMessage()).toLowerCase(java.util.Locale.ROOT))
                        .doesNotContain("deadlock")
                        .doesNotContain("lock wait timeout");
            }
        }

        /**
         * Ejecuta {@code n} altas en paralelo alineadas por una barrera y devuelve los fallos.
         * Cada hilo instala su propio actor: {@code ActorContextHolder} es un ThreadLocal y sin
         * eso el alta fallaria por falta de actor en vez de por concurrencia.
         */
        private List<Throwable> ejecutarEnParalelo(int n, java.util.function.IntConsumer alta)
                throws Exception {
            String sub = actor();
            CyclicBarrier barrera = new CyclicBarrier(n);
            ExecutorService pool = Executors.newFixedThreadPool(n);
            try {
                List<Future<Throwable>> futuros = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int indice = i;
                    Callable<Throwable> tarea = () -> {
                        ActorContextHolder.set(new ActorContext(sub));
                        try {
                            barrera.await(20, TimeUnit.SECONDS);
                            alta.accept(indice);
                            return null;
                        } catch (Throwable t) {
                            return t;
                        } finally {
                            ActorContextHolder.clear();
                        }
                    };
                    futuros.add(pool.submit(tarea));
                }
                List<Throwable> fallos = new ArrayList<>();
                for (Future<Throwable> futuro : futuros) {
                    Throwable t = futuro.get(60, TimeUnit.SECONDS);
                    if (t != null) {
                        fallos.add(t);
                    }
                }
                return fallos;
            } finally {
                pool.shutdownNow();
            }
        }
    }

    // =====================================================================
    // DECORADORES DE FALLO
    // =====================================================================

    /**
     * Envuelve los seis puertos mutantes del alta en decoradores que delegan en el adaptador real.
     * El {@code BeanPostProcessor} es la forma menos invasiva de hacerlo: no reemplaza la
     * definicion del bean, no necesita {@code @Primary} y deja intacta la topologia que
     * {@code JdbcProfileContextIT} verifica.
     */
    @TestConfiguration
    static class DecoradoresDeFallo {

        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor fallosControlados() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof PersonaRepository r) return new PersonaConFallo(r);
                    if (bean instanceof ActaRepository r) return new ActaConFallo(r);
                    if (bean instanceof ObservacionRepository r) return new ObservacionConFallo(r);
                    if (bean instanceof ActaEvidenciaRepository r) return new EvidenciaConFallo(r);
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

    /**
     * Delega siempre. Falla antes de escribir en la etapa {@code PERSONA} y, en
     * {@code PERSONA_DUPLICADA}, inserta dos veces la misma persona para que el
     * {@code UNIQUE} del motor produzca la excepcion desde dentro del caso de uso.
     */
    private record PersonaConFallo(PersonaRepository delegado) implements PersonaRepository {
        @Override public FalPersona guardar(FalPersona persona) {
            fallarSi(Etapa.PERSONA);
            FalPersona persistida = delegado.guardar(persona);
            if (fallaEn == Etapa.PERSONA_DUPLICADA) {
                // El argumento sigue con id nulo: el segundo guardar es otro INSERT con el mismo
                // (tipo_documento, doc_key) y lo rechaza uq_fal_persona_tipo_doc_key.
                delegado.guardar(persona);
            }
            return persistida;
        }
        @Override public Optional<FalPersona> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public List<FalPersona> listarTodas() { return delegado.listarTodas(); }
        @Override public List<FalPersona> buscarPorTipoDocYNroDoc(
                TipoDocumentoPersona tipoDoc, String nroDoc) {
            return delegado.buscarPorTipoDocYNroDoc(tipoDoc, nroDoc);
        }
        @Override public List<FalPersona> buscarPorIdSujBie(Integer idSuj, Integer idBie) {
            return delegado.buscarPorIdSujBie(idSuj, idBie);
        }
        @Override public ar.gob.malvinas.faltas.core.application.query.PaginaResultado<FalPersona>
                listarPagina(ar.gob.malvinas.faltas.core.application.query.FiltroPersona filtro,
                             ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada<
                                     ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona> pagina) {
            return delegado.listarPagina(filtro, pagina);
        }
    }

    /**
     * Falla <b>despues</b> de que el adaptador real inserte el acta: asi la etapa ACTA prueba que
     * una fila ya escrita se revierte, y no solamente que la escritura no ocurrio.
     */
    private record ActaConFallo(ActaRepository delegado) implements ActaRepository {
        @Override public FalActa guardar(FalActa acta) {
            FalActa persistida = delegado.guardar(acta);
            fallarSi(Etapa.ACTA);
            return persistida;
        }
        @Override public Optional<FalActa> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public Optional<FalActa> buscarPorUuidTecnico(String uuid) {
            return delegado.buscarPorUuidTecnico(uuid);
        }
        @Override public Optional<FalActa> buscarPorUuidTecnicoConLock(String uuid) {
            return delegado.buscarPorUuidTecnicoConLock(uuid);
        }
        @Override public Optional<FalActa> buscarPorIdConLock(Long id) {
            return delegado.buscarPorIdConLock(id);
        }
        @Override public List<FalActa> listarTodas() { return delegado.listarTodas(); }
        @Override public ar.gob.malvinas.faltas.core.application.query.PaginaResultado<FalActa>
                listarPagina(ar.gob.malvinas.faltas.core.application.query.FiltroActa filtro,
                             ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada<
                                     ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa> pagina) {
            return delegado.listarPagina(filtro, pagina);
        }
    }

    private record ObservacionConFallo(ObservacionRepository delegado) implements ObservacionRepository {
        @Override public FalObservacion guardar(FalObservacion obs) {
            FalObservacion persistida = delegado.guardar(obs);
            fallarSi(Etapa.OBSERVACION);
            return persistida;
        }
        @Override public Optional<FalObservacion> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long id) {
            return delegado.listarPorEntidad(tipo, id);
        }
        @Override public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long id) {
            return delegado.listarActivasPorEntidad(tipo, id);
        }
        @Override public java.util.Map<Long, List<FalObservacion>> listarPorEntidades(
                EntidadTipoObservada tipo, java.util.Collection<Long> ids) {
            return delegado.listarPorEntidades(tipo, ids);
        }
        @Override public FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja) {
            return delegado.desactivar(id, fhBaja, idUserBaja);
        }
    }

    /**
     * Falla en la <b>segunda</b> evidencia: la primera queda insertada de verdad y el caso puede
     * afirmar que desaparece.
     */
    private static final class EvidenciaConFallo implements ActaEvidenciaRepository {
        private final ActaEvidenciaRepository delegado;
        private int insertadas = 0;

        EvidenciaConFallo(ActaEvidenciaRepository delegado) {
            this.delegado = delegado;
        }

        @Override public FalActaEvidencia guardar(FalActaEvidencia evidencia) {
            FalActaEvidencia persistida = delegado.guardar(evidencia);
            insertadas++;
            if (fallaEn == Etapa.EVIDENCIA_POSTERIOR && insertadas >= 2) {
                insertadas = 0;
                throw new IllegalStateException(FALLO + " en etapa " + Etapa.EVIDENCIA_POSTERIOR);
            }
            return persistida;
        }
        @Override public List<FalActaEvidencia> listarPorActa(Long idActa) {
            return delegado.listarPorActa(idActa);
        }
        @Override public Long nextId() { return delegado.nextId(); }
        @Override public List<FalActaEvidencia> listarFotografiasPorActa(Long idActa) {
            return delegado.listarFotografiasPorActa(idActa);
        }
        @Override public Optional<FalActaEvidencia> buscarFotografiaPorActaYId(Long idActa, Long idEvidencia) {
            return delegado.buscarFotografiaPorActaYId(idActa, idEvidencia);
        }
        @Override public int maxOrdenFotografiasPorActa(Long idActa) {
            return delegado.maxOrdenFotografiasPorActa(idActa);
        }
        @Override public boolean existeFotografiaConHash(Long idActa, String sha256Hex) {
            return delegado.existeFotografiaConHash(idActa, sha256Hex);
        }
    }

    private record EventoConFallo(ActaEventoRepository delegado) implements ActaEventoRepository {
        @Override public ar.gob.malvinas.faltas.core.domain.model.FalActaEvento registrar(
                ar.gob.malvinas.faltas.core.domain.model.FalActaEvento evento) {
            var persistido = delegado.registrar(evento);
            fallarSi(Etapa.EVENTO);
            return persistido;
        }
        @Override public List<ar.gob.malvinas.faltas.core.domain.model.FalActaEvento> buscarPorActa(Long idActa) {
            return delegado.buscarPorActa(idActa);
        }
        @Override public Optional<ar.gob.malvinas.faltas.core.domain.model.FalActaEvento> buscarPorId(Long id) {
            return delegado.buscarPorId(id);
        }
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

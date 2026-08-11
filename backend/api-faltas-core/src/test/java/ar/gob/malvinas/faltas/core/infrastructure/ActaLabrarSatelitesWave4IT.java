package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.DomicilioActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de Wave 4 (alta integral JDBC): persistencia y rollback de los bloques tipados de
 * satélites agregados a {@code LabrarActaCommand} — domicilios ({@code fal_persona_domicilio})
 * y artículos infringidos ({@code fal_acta_articulo_infringido}) — dentro de la misma frontera
 * transaccional que {@link ActaLabrarTransaccionalIT} certifica para el núcleo del alta.
 *
 * <p>Sigue el mismo patrón de decoradores por {@code BeanPostProcessor} que
 * {@code ActaLabrarTransaccionalIT}: delega en el adaptador JDBC real y falla en el punto
 * elegido, para probar que una escritura ya realizada se revierte y no solo que no llegó a
 * ocurrir.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(ActaLabrarSatelitesWave4IT.DecoradoresDeFallo.class)
@DisplayName("IT WAVE-4: satélites de ActaService.labrar (domicilios, artículos infringidos)")
class ActaLabrarSatelitesWave4IT {

    private static final String MARCA = "WAVE4SAT_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 5, 10, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 5);

    enum Etapa { NINGUNA, DOMICILIO, ARTICULO }

    static volatile Etapa fallaEn = Etapa.NINGUNA;
    private static final String FALLO = "FALLO_INYECTADO_WAVE4";

    @Autowired
    private ActaService actaService;

    @Autowired
    private ar.gob.malvinas.faltas.core.application.service.TalonarioService talonarioService;

    @Autowired
    private JdbcClient jdbc;

    private Long idDependencia;
    private Long idNormativa;
    private Long idArticulo;

    @BeforeEach
    void prepararPadresYActor() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.set(new ActorContext(java.util.UUID.randomUUID().toString()));
        insertarDependencia();
        insertarNormativaYArticulo();
        // DECISION-ACTA-TALONARIO-REQUIRED-01 (Wave 4 Parte 3C §7): sin esto, todas las altas
        // de este IT rechazarian por "0 talonarios ACTA ELECTRONICO activos".
        ar.gob.malvinas.faltas.core.support.TalonarioElectronicoITSeedSupport
                .seedTalonarioElectronicoActivo(talonarioService, idDependencia, (short) 1,
                        LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void limpiar() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    private void insertarDependencia() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP").param("fhAlta", T0).param("usuario", actor())
                .update();
        idDependencia = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDependencia).param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1)).param("fhAlta", T0).param("usuario", actor())
                .update();
    }

    private void insertarNormativaYArticulo() {
        jdbc.sql("""
                INSERT INTO fal_normativa_faltas (codigo_norma, nombre_norma, tipo_norma, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:codigo, :nombre, 1, :fhVigDesde, :fhAlta, :usuario)
                """)
                .param("codigo", (MARCA + java.util.UUID.randomUUID()).substring(0, 8))
                .param("nombre", MARCA + "NORMATIVA").param("fhVigDesde", T0.toLocalDate())
                .param("fhAlta", T0).param("usuario", actor())
                .update();
        idNormativa = jdbc.sql("SELECT MAX(id) FROM fal_normativa_faltas WHERE nombre_norma = :n")
                .param("n", MARCA + "NORMATIVA").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_articulo_normativa_faltas
                    (id_normativa, codigo_articulo, nombre_articulo, tipo_infraccion, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idNormativa, :codigo, :nombre, 1, :fhVigDesde, :fhAlta, :usuario)
                """)
                .param("idNormativa", idNormativa).param("codigo", "ART" + (int) (Math.random() * 100000))
                .param("nombre", MARCA + "ARTICULO").param("fhVigDesde", T0.toLocalDate())
                .param("fhAlta", T0).param("usuario", actor())
                .update();
        idArticulo = jdbc.sql("SELECT MAX(id) FROM fal_articulo_normativa_faltas WHERE nombre_articulo = :n")
                .param("n", MARCA + "ARTICULO").query(Long.class).single();
        // Formula MONTO (Wave 4 PARTE-2 S7.3): no requiere fal_valor_unidad_faltas, para que
        // ValorizacionService.calcularInicial (wireado en ActaService.labrar) pueda calcular
        // la valorizacion del articulo informado sin depender de otro seed.
        jdbc.sql("""
                INSERT INTO fal_tarifario_unidad_faltas
                    (id_articulo, tipo_unidad, monto_fijo_ordinario, si_admite_pago_voluntario, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idArticulo, 3, 100.00, FALSE, :fhVigDesde, :fhAlta, :usuario)
                """)
                .param("idArticulo", idArticulo).param("fhVigDesde", T0.toLocalDate())
                .param("fhAlta", T0).param("usuario", actor())
                .update();
    }

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        for (Long idActa : actas) {
            jdbc.sql("""
                    DELETE FROM fal_acta_valorizacion_item WHERE id_acta_articulo IN (
                        SELECT id FROM fal_acta_articulo_infringido WHERE id_acta = :id)
                    """).param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_medida_preventiva WHERE acta_id = :id").param("id", idActa).update();
            // Todos los comandos de este IT informan transito minimo (Wave 4 Parte 3C).
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_articulo_infringido WHERE id_acta = :id").param("id", idActa).update();
            // fal_acta_snapshot referencia fal_acta_valorizacion: borrar el snapshot primero.
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = :tipo AND entidad_id = :id")
                    .param("tipo", EntidadTipoObservada.ACTA.codigo()).param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("""
                DELETE FROM fal_persona_domicilio WHERE persona_id IN (
                    SELECT id FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca)
                """).param("marca", MARCA + "%").update();
        jdbc.sql("DELETE FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca")
                .param("marca", MARCA + "%").update();
        if (idArticulo != null) {
            jdbc.sql("DELETE FROM fal_tarifario_unidad_faltas WHERE id_articulo = :id").param("id", idArticulo).update();
            jdbc.sql("DELETE FROM fal_articulo_normativa_faltas WHERE id = :id").param("id", idArticulo).update();
        }
        if (idNormativa != null) {
            jdbc.sql("DELETE FROM fal_normativa_faltas WHERE id = :id").param("id", idNormativa).update();
        }
        borrarTalonarioDeLaDependencia();
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep").param("idDep", idDependencia).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep").param("idDep", idDependencia).update();
    }

    /** Ver {@code ActaLabrarTransaccionalIT.borrarTalonarioDeLaDependencia}: mismo patron. */
    private void borrarTalonarioDeLaDependencia() {
        List<Long> talonarioIds = jdbc.sql(
                        "SELECT talonario_id FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        jdbc.sql("DELETE FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia).update();
        for (Long idTalonario : talonarioIds) {
            Long idPolitica = jdbc.sql("SELECT politica_id FROM num_talonario WHERE id = :id")
                    .param("id", idTalonario).query(Long.class).single();
            jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_politica WHERE id = :id").param("id", idPolitica).update();
        }
    }

    private static String actor() { return ActorContextHolder.subOr("SYS"); }

    private static String nroDocDePrueba() {
        return String.valueOf(66_000_000 + (int) (Math.random() * 900_000));
    }

    private long contarActas() {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarPersonasDelSlice() {
        return jdbc.sql("SELECT COUNT(*) FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca")
                .param("marca", MARCA + "%").query(Long.class).single();
    }

    private long contarDomiciliosDelSlice() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_persona_domicilio WHERE persona_id IN (
                    SELECT id FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca)
                """).param("marca", MARCA + "%").query(Long.class).single();
    }

    private long contarArticulosDeActasDelSlice() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_articulo_infringido
                 WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    private LabrarActaCommand comandoConDocumentoYDomicilios(
            String nroDoc, List<DomicilioActaItem> domicilios, List<ArticuloInfringidoItem> articulos) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, null, F0,
                MARCA + "domicilio del hecho", null, null, null, null,
                MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDoc, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                null, null, domicilios, articulos, null,
                ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(),
                null, null, null);
    }

    private DomicilioActaItem domicilioBasico() {
        return new DomicilioActaItem(
                TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                true, true, null, null, null, null, null, null, null, null, null,
                MARCA + "CALLE FALSA 123", 123, false, null, null, null, null, null, null);
    }

    // =====================================================================
    @Nested
    @DisplayName("Éxito: satélites persistidos en la misma transacción")
    class Exito {

        @Test
        @DisplayName("1. Domicilio informado se persiste anclado a la persona infractora resuelta")
        void domicilioSePersisteConPersonaInfractora() {
            String nroDoc = nroDocDePrueba();
            ComandoResultado resultado = actaService.labrar(
                    comandoConDocumentoYDomicilios(nroDoc, List.of(domicilioBasico()), null));

            assertThat(contarActas()).isEqualTo(1L);
            assertThat(contarDomiciliosDelSlice()).isEqualTo(1L);

            Long idPersonaInfractor = jdbc.sql("SELECT id_persona_infractor FROM fal_acta WHERE id = :id")
                    .param("id", resultado.idActa()).query(Long.class).single();
            Long personaIdDomicilio = jdbc.sql("""
                    SELECT persona_id FROM fal_persona_domicilio WHERE persona_id IN (
                        SELECT id FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca)
                    """).param("marca", MARCA + "%").query(Long.class).single();
            assertThat(personaIdDomicilio).isEqualTo(idPersonaInfractor);
        }

        @Test
        @DisplayName("2. Artículo infringido informado se persiste con el id_acta correcto")
        void articuloInfringidoSePersisteConActaCorrecta() {
            ComandoResultado resultado = actaService.labrar(comandoConDocumentoYDomicilios(
                    nroDocDePrueba(), null, List.of(new ArticuloInfringidoItem(idNormativa, idArticulo))));

            assertThat(contarArticulosDeActasDelSlice()).isEqualTo(1L);
            var fila = jdbc.sql("""
                    SELECT id_acta, id_articulo, si_activo FROM fal_acta_articulo_infringido
                     WHERE id_acta = :idActa
                    """)
                    .param("idActa", resultado.idActa())
                    .query((rs, n) -> List.of(rs.getLong("id_acta"), rs.getLong("id_articulo"), rs.getBoolean("si_activo")))
                    .single();
            assertThat(fila.get(0)).isEqualTo(resultado.idActa());
            assertThat(fila.get(1)).isEqualTo(idArticulo);
            assertThat(fila.get(2)).isEqualTo(true);
        }

        @Test
        @DisplayName("3. normativaId se reconstruye correctamente vía JOIN al leer de vuelta")
        void normativaIdSeReconstruyeAlLeer() {
            actaService.labrar(comandoConDocumentoYDomicilios(
                    nroDocDePrueba(), null, List.of(new ArticuloInfringidoItem(idNormativa, idArticulo))));

            List<FalActaArticuloInfringido> activos = articuloInfringidoRepository().findByActaId(
                    jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                            .param("idDep", idDependencia).query(Long.class).single());
            assertThat(activos).hasSize(1);
            assertThat(activos.get(0).getNormativaId()).isEqualTo(idNormativa);
            assertThat(activos.get(0).getArticuloId()).isEqualTo(idArticulo);
        }

        private ActaArticuloInfringidoRepository articuloInfringidoRepository() {
            return ctx.getBean(ActaArticuloInfringidoRepository.class);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Rollback total: satélites de Wave 4")
    class Rollback {

        @Test
        @DisplayName("4. Fallo al guardar domicilio revierte también la persona ya insertada")
        void falloEnDomicilioRevierteLaPersona() {
            fallaEn = Etapa.DOMICILIO;
            String nroDoc = nroDocDePrueba();
            Throwable error = catchThrowable(() -> actaService.labrar(
                    comandoConDocumentoYDomicilios(nroDoc, List.of(domicilioBasico()), null)));

            assertThat(error).hasMessageContaining(FALLO);
            assertThat(contarActas()).as("cero fal_acta").isZero();
            assertThat(contarPersonasDelSlice()).as("la persona tambien se revierte").isZero();
            assertThat(contarDomiciliosDelSlice()).as("cero fal_persona_domicilio").isZero();
        }

        @Test
        @DisplayName("5. Fallo al guardar artículo infringido revierte acta y persona")
        void falloEnArticuloRevierteActaYPersona() {
            fallaEn = Etapa.ARTICULO;
            String nroDoc = nroDocDePrueba();
            Throwable error = catchThrowable(() -> actaService.labrar(comandoConDocumentoYDomicilios(
                    nroDoc, null, List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)))));

            assertThat(error).hasMessageContaining(FALLO);
            assertThat(contarActas()).as("cero fal_acta").isZero();
            assertThat(contarPersonasDelSlice()).as("la persona tambien se revierte").isZero();
            assertThat(contarArticulosDeActasDelSlice()).as("cero fal_acta_articulo_infringido").isZero();
        }

        @Test
        @DisplayName("6. Después de un rollback en artículo, el alta siguiente commitea normalmente")
        void despuesDeRollbackElAltaSiguienteFunciona() {
            fallaEn = Etapa.ARTICULO;
            assertThat(catchThrowable(() -> actaService.labrar(comandoConDocumentoYDomicilios(
                    nroDocDePrueba(), null, List.of(new ArticuloInfringidoItem(idNormativa, idArticulo))))))
                    .hasMessageContaining(FALLO);
            assertThat(contarActas()).isZero();

            fallaEn = Etapa.NINGUNA;
            ComandoResultado ok = actaService.labrar(comandoConDocumentoYDomicilios(
                    nroDocDePrueba(), null, List.of(new ArticuloInfringidoItem(idNormativa, idArticulo))));
            assertThat(ok.idActa()).isNotNull();
            assertThat(contarActas()).isEqualTo(1L);
            assertThat(contarArticulosDeActasDelSlice()).isEqualTo(1L);
        }
    }

    @Autowired
    private org.springframework.context.ApplicationContext ctx;

    // =====================================================================
    // DECORADORES DE FALLO
    // =====================================================================

    @TestConfiguration
    static class DecoradoresDeFallo {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor fallosControladosWave4() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof PersonaDomicilioRepository r) return new DomicilioConFallo(r);
                    if (bean instanceof ActaArticuloInfringidoRepository r) return new ArticuloConFallo(r);
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

    private record DomicilioConFallo(PersonaDomicilioRepository delegado) implements PersonaDomicilioRepository {
        @Override public Long nextId() { return delegado.nextId(); }
        @Override public FalPersonaDomicilio guardar(FalPersonaDomicilio domicilio) {
            FalPersonaDomicilio persistido = delegado.guardar(domicilio);
            fallarSi(Etapa.DOMICILIO);
            return persistido;
        }
        @Override public java.util.Optional<FalPersonaDomicilio> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public List<FalPersonaDomicilio> buscarPorPersonaId(Long personaId) { return delegado.buscarPorPersonaId(personaId); }
        @Override public List<FalPersonaDomicilio> buscarActivosPorPersonaId(Long personaId) { return delegado.buscarActivosPorPersonaId(personaId); }
        @Override public List<FalPersonaDomicilio> buscarNotificablesPorPersonaId(Long personaId) { return delegado.buscarNotificablesPorPersonaId(personaId); }
        @Override public java.util.Optional<FalPersonaDomicilio> buscarPrincipalActivo(Long personaId, TipoDomicilio tipo) {
            return delegado.buscarPrincipalActivo(personaId, tipo);
        }
    }

    private record ArticuloConFallo(ActaArticuloInfringidoRepository delegado) implements ActaArticuloInfringidoRepository {
        @Override public Long nextId() { return delegado.nextId(); }
        @Override public FalActaArticuloInfringido save(FalActaArticuloInfringido articulo) {
            FalActaArticuloInfringido persistido = delegado.save(articulo);
            fallarSi(Etapa.ARTICULO);
            return persistido;
        }
        @Override public java.util.Optional<FalActaArticuloInfringido> findById(Long id) { return delegado.findById(id); }
        @Override public List<FalActaArticuloInfringido> findByActaId(Long actaId) { return delegado.findByActaId(actaId); }
        @Override public List<FalActaArticuloInfringido> findActivosByActaId(Long actaId) { return delegado.findActivosByActaId(actaId); }
        @Override public java.util.Optional<FalActaArticuloInfringido> findActivoByActaAndArticulo(Long actaId, Long articuloId) {
            return delegado.findActivoByActaAndArticulo(actaId, articuloId);
        }
        @Override public boolean existsActivo(Long actaId, Long articuloId) { return delegado.existsActivo(actaId, articuloId); }
    }
}

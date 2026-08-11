package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.service.ActaAltaIntentoTransaccionalService;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.QrAccesoTransaccionalService;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaEventoRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcMotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcNormativaRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcObservacionRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcPersonaRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcQrAccesoRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcTalonarioRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.ValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryQrAccesoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryValorUnidadFaltasRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT de topologia de beans del perfil jdbc.
 *
 * Verifica que activar el perfil deja exactamente la infraestructura esperada:
 * un unico DataSource principal, los beans JDBC auto-configurados por Spring
 * Boot, un transaction manager funcional, y ningun mecanismo de inicializacion
 * de esquema activo.
 *
 * <p>Desde WAVE 1A2-B2 verifica ademas la topologia de la vertical transaccional del alta:
 * los seis puertos mutantes de {@code ActaService.labrar} resueltos por adaptadores JDBC,
 * sus equivalentes InMemory ausentes, y la frontera transaccional efectivamente proxificada
 * sobre el metodo {@code labrar}. Los casos 12 y 22 se invirtieron en ese slice: hasta B1.5C
 * afirmaban que el acta y el snapshot seguian resueltos por InMemory.
 *
 * Solo lectura: no ejecuta DDL ni DML.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-0A/1A2-B2: topologia de beans del perfil jdbc")
class JdbcProfileContextIT {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Environment env;

    @Test
    @DisplayName("1. El perfil jdbc esta activo y memory no lo esta")
    void perfilJdbcActivo() {
        assertThat(env.getActiveProfiles()).contains("jdbc");
        assertThat(env.getProperty("app.persistence.mode")).isEqualTo("jdbc");
    }

    @Test
    @DisplayName("2. Existe exactamente un DataSource y es el principal de Faltas")
    void unicoDataSourcePrincipal() {
        String[] nombres = ctx.getBeanNamesForType(DataSource.class);
        assertThat(nombres).containsExactly("dataSource");
    }

    @Test
    @DisplayName("3. Ningun datasource Geo se activa con el perfil jdbc")
    void datasourcesGeoApagados() {
        assertThat(ctx.containsBean("geoDataSource")).isFalse();
        assertThat(ctx.containsBean("geoJdbcClient")).isFalse();
        assertThat(ctx.containsBean("geoCatalogMariaDbDataSource")).isFalse();
        assertThat(ctx.containsBean("geoCatalogJdbcClient")).isFalse();
    }

    @Test
    @DisplayName("4. Spring Boot auto-configura JdbcClient, JdbcTemplate y NamedParameterJdbcTemplate")
    void beansJdbcAutoConfigurados() {
        assertThat(ctx.getBeanNamesForType(JdbcClient.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcTemplate.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(NamedParameterJdbcTemplate.class)).hasSize(1);
    }

    @Test
    @DisplayName("5. Hay un PlatformTransactionManager JDBC disponible")
    void transactionManagerDisponible() {
        String[] nombres = ctx.getBeanNamesForType(PlatformTransactionManager.class);
        assertThat(nombres).hasSize(1);
        assertThat(ctx.getBean(PlatformTransactionManager.class)).isInstanceOf(JdbcTransactionManager.class);
    }

    @Test
    @DisplayName("6. El transaction manager opera realmente (transaccion de solo lectura)")
    void transaccionDeSoloLecturaFunciona() {
        TransactionTemplate tx = new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
        tx.setReadOnly(true);
        Integer resultado = tx.execute(status -> ctx.getBean(JdbcClient.class)
                .sql("SELECT 1")
                .query(Integer.class)
                .single());
        assertThat(resultado).isEqualTo(1);
    }

    @Test
    @DisplayName("7. spring.sql.init.mode=never y no hay inicializador de esquema en el contexto")
    void sinInicializacionAutomaticaDeEsquema() {
        assertThat(env.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ctx.getBeanNamesForType(AbstractScriptDatabaseInitializer.class)).isEmpty();
    }

    @Test
    @DisplayName("8. No hay Flyway, Liquibase ni JPA en el contexto")
    void sinMigradoresNiOrm() {
        String[] beans = ctx.getBeanDefinitionNames();
        assertThat(beans).noneMatch(nombre -> {
            String n = nombre.toLowerCase();
            return n.contains("flyway") || n.contains("liquibase") || n.contains("entitymanager");
        });
    }

    @Test
    @DisplayName("9. La configuracion del perfil no expone credenciales por defecto")
    void sinCredencialesPorDefecto() {
        assertThat(env.getProperty("spring.datasource.url")).doesNotContain("localhost:3306/faltas_db");
        assertThat(ctx.containsBean("jdbcConfig")).isTrue();
    }

    // =====================================================================
    // WAVE 0B: seleccion de adaptador del puerto piloto (cierre acotado de GAP-004)
    // =====================================================================

    @Test
    @DisplayName("10. Con perfil jdbc hay exactamente un ActaEventoRepository y es el adaptador JDBC")
    void puertoPilotoResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(ActaEventoRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaEventoRepository.class)).isInstanceOf(JdbcActaEventoRepository.class);
    }

    @Test
    @DisplayName("11. Con perfil jdbc el adaptador InMemory del puerto piloto no esta registrado")
    void inMemoryDelPuertoPilotoAusente() {
        assertThat(ctx.getBeanNamesForType(InMemoryActaEventoRepository.class)).isEmpty();
    }

    /**
     * Invertido parcialmente en WAVE 1A2-B2: el acta y el snapshot ya no son puertos "no
     * migrados". Lo que queda de la afirmacion original es la parte que sigue siendo cierta y
     * que sigue siendo util: hay puertos que ninguna wave migro, y {@code DocumentoRepository}
     * es uno de ellos.
     */
    @Test
    @DisplayName("12. Los puertos aun no migrados siguen resueltos por InMemory")
    void soloLosPuertosMigradosUsanJdbc() {
        assertThat(ctx.getBean(DocumentoRepository.class)).isInstanceOf(InMemoryDocumentoRepository.class);

        assertThat(ctx.getBeanNamesForType(JdbcActaEventoRepository.class))
                .as("WAVE 0B introduce un unico adaptador Jdbc* de dominio")
                .hasSize(1);
    }

    // =====================================================================
    // WAVE 1A1: catalogos simples (cierre acotado de GAP-004 para dos puertos mas)
    // =====================================================================

    @Test
    @DisplayName("15. Con perfil jdbc hay exactamente un DiaNoComputableRepository y es el adaptador JDBC")
    void diaNoComputableResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(DiaNoComputableRepository.class)).hasSize(1);
        assertThat(ctx.getBean(DiaNoComputableRepository.class))
                .isInstanceOf(JdbcDiaNoComputableRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryDiaNoComputableRepository.class)).isEmpty();
    }

    @Test
    @DisplayName("16. Con perfil jdbc hay exactamente un MotivoArchivoRepository y es el adaptador JDBC")
    void motivoArchivoResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(MotivoArchivoRepository.class)).hasSize(1);
        assertThat(ctx.getBean(MotivoArchivoRepository.class))
                .isInstanceOf(JdbcMotivoArchivoRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryMotivoArchivoRepository.class)).isEmpty();
    }

    @Test
    @DisplayName("17. WAVE 1A1 deja tres adaptadores Jdbc* de dominio")
    void tresAdaptadoresJdbcDeDominio() {
        assertThat(ctx.getBeanNamesForType(JdbcActaEventoRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcDiaNoComputableRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcMotivoArchivoRepository.class)).hasSize(1);
    }

    // =====================================================================
    // WAVE 1A2-B0: puerto de observaciones polimorficas
    // =====================================================================

    @Test
    @DisplayName("20. Con perfil jdbc hay exactamente un ObservacionRepository y es el adaptador JDBC")
    void observacionResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(ObservacionRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ObservacionRepository.class))
                .isInstanceOf(JdbcObservacionRepository.class);
    }

    @Test
    @DisplayName("21. Con perfil jdbc el InMemory de observaciones no esta registrado")
    void inMemoryDeObservacionAusente() {
        assertThat(ctx.getBeanNamesForType(InMemoryObservacionRepository.class)).isEmpty();
    }

    /**
     * Invertido en WAVE 1A2-B2: el acta pasa a resolverse por JDBC. Lo que se conserva es el
     * inventario de adaptadores que B0 dejo, que sigue siendo cierto y que hace visible el
     * incremento de B2 en el caso 27.
     */
    @Test
    @DisplayName("22. Los cuatro adaptadores Jdbc* previos a B2 siguen registrados")
    void cuatroAdaptadoresJdbcDeDominio() {
        assertThat(ctx.getBeanNamesForType(JdbcActaEventoRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcDiaNoComputableRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcMotivoArchivoRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcObservacionRepository.class)).hasSize(1);
    }

    // =====================================================================
    // WAVE 1A2-B2: vertical transaccional del alta (Acta + Persona + participantes)
    // =====================================================================

    /**
     * Los seis puertos que mutan estado durante {@code ActaService.labrar} deben resolverse por
     * un adaptador JDBC sobre el mismo DataSource. Es la condicion A de la activacion: sin ella
     * una escritura quedaria fuera de la transaccion y sobreviviria al rollback.
     */
    @Test
    @DisplayName("25. Los seis puertos mutantes del alta se resuelven por adaptadores JDBC")
    void seisPuertosMutantesDelAltaEnJdbc() {
        assertThat(ctx.getBeanNamesForType(PersonaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(PersonaRepository.class)).isInstanceOf(JdbcPersonaRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaRepository.class)).isInstanceOf(JdbcActaRepository.class);

        assertThat(ctx.getBeanNamesForType(ObservacionRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ObservacionRepository.class)).isInstanceOf(JdbcObservacionRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaEvidenciaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaEvidenciaRepository.class))
                .isInstanceOf(JdbcActaEvidenciaRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaEventoRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaEventoRepository.class)).isInstanceOf(JdbcActaEventoRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaSnapshotRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaSnapshotRepository.class))
                .isInstanceOf(JdbcActaSnapshotRepository.class);
    }

    @Test
    @DisplayName("26. Los InMemory de los puertos activados por B2 no estan registrados")
    void inMemoryDeLosPuertosDeB2Ausentes() {
        assertThat(ctx.getBeanNamesForType(InMemoryActaRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryPersonaRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryActaEvidenciaRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryActaSnapshotRepository.class)).isEmpty();
    }

    @Test
    @DisplayName("27. WAVE 1A2-B2 deja ocho adaptadores Jdbc* de dominio")
    void ochoAdaptadoresJdbcDeDominio() {
        assertThat(ctx.getBeanNamesForType(JdbcActaEventoRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcDiaNoComputableRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcMotivoArchivoRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcObservacionRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcActaRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcPersonaRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcActaEvidenciaRepository.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(JdbcActaSnapshotRepository.class)).hasSize(1);
    }

    /**
     * Todos los adaptadores JDBC comparten el unico {@code JdbcClient} del contexto, que a su
     * vez esta construido sobre el unico {@code DataSource}. Es lo que hace que participen de la
     * misma transaccion: si alguno tuviera su propio DataSource, su escritura commitearia por
     * separado y el rollback del caso de uso no la alcanzaria.
     */
    @Test
    @DisplayName("28. Un unico DataSource y un unico transaction manager sirven a todos los adaptadores")
    void unRecursoUnicoParaTodosLosAdaptadores() {
        assertThat(ctx.getBeanNamesForType(DataSource.class)).containsExactly("dataSource");
        assertThat(ctx.getBeanNamesForType(JdbcClient.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(PlatformTransactionManager.class)).hasSize(1);
        assertThat(ctx.getBean(PlatformTransactionManager.class))
                .isInstanceOf(JdbcTransactionManager.class);
        assertThat(((JdbcTransactionManager) ctx.getBean(PlatformTransactionManager.class))
                .getDataSource())
                .as("el transaction manager administra el mismo DataSource que usan los adaptadores")
                .isSameAs(ctx.getBean(DataSource.class));
    }

    /**
     * {@code ActaService} tiene que estar proxificado y {@code labrar} tiene que tener atributo
     * transaccional efectivo. Verificarlo por reflexion sobre la clase anotada no alcanzaria:
     * una anotacion presente sin proxy no abre ninguna transaccion, y ese es exactamente el modo
     * en que una frontera puede parecer implementada sin estarlo.
     */
    @Test
    @DisplayName("29. ActaService esta proxificado y labrar tiene atributo transaccional efectivo")
    void fronteraTransaccionalEfectiva() throws Exception {
        ActaService servicio = ctx.getBean(ActaService.class);
        assertThat(AopUtils.isAopProxy(servicio))
                .as("sin proxy la anotacion no abre ninguna transaccion")
                .isTrue();

        assertThat(ctx.getBeanNamesForType(TransactionAttributeSource.class))
                .as("la infraestructura declarativa de transacciones esta activa")
                .isNotEmpty();

        Method labrar = ActaService.class.getMethod("labrar",
                ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand.class);
        TransactionAttribute atributo = ctx.getBean(TransactionAttributeSource.class)
                .getTransactionAttribute(labrar, ActaService.class);
        assertThat(atributo)
                .as("labrar debe tener atributo transaccional resuelto por Spring")
                .isNotNull();
        assertThat(atributo.isReadOnly())
                .as("el alta escribe: no puede ser read-only")
                .isFalse();
        assertThat(atributo.getPropagationBehavior())
                .as("propagacion REQUIRED: sin REQUIRES_NEW ni transacciones anidadas")
                .isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
        assertThat(labrar.getAnnotation(Transactional.class))
                .as("la demarcacion esta sobre el metodo publico exacto del alta")
                .isNotNull();
    }

    // =====================================================================
    // POST-B2-TX-CONSUMERS-01: acceso QR activado y colaborador transaccional
    // =====================================================================

    /**
     * Antes de esta wave {@code QrAccesoRepository} solo tenia adaptador InMemory, sin
     * {@code @Profile}, de modo que bajo {@code jdbc} el acceso QR quedaba en memoria mientras
     * el evento y el snapshot de la misma operacion iban a MariaDB.
     */
    @Test
    @DisplayName("33. Con perfil jdbc hay exactamente un QrAccesoRepository y es el adaptador JDBC")
    void qrAccesoResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(QrAccesoRepository.class)).hasSize(1);
        assertThat(ctx.getBean(QrAccesoRepository.class)).isInstanceOf(JdbcQrAccesoRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryQrAccesoRepository.class)).isEmpty();
    }

    /**
     * {@code QrAccesoTransaccionalService.registrarConfirmado} es la frontera real que
     * reemplaza al self-invocation de {@code QrActaService.registrarAcceso} cuando se invoca
     * desde {@code registrarAccesoConNotificacion}. Verificar solo la anotacion no alcanzaria:
     * el proxy es lo que hace que la anotacion tenga efecto para cualquier llamador externo al
     * bean, incluido {@code QrActaService}.
     */
    @Test
    @DisplayName("34. QrAccesoTransaccionalService esta proxificado y registrarConfirmado tiene atributo transaccional efectivo")
    void qrAccesoTransaccionalServiceProxificado() throws Exception {
        QrAccesoTransaccionalService servicio = ctx.getBean(QrAccesoTransaccionalService.class);
        assertThat(AopUtils.isAopProxy(servicio))
                .as("sin proxy la anotacion no abre ninguna transaccion")
                .isTrue();

        Method registrarConfirmado = QrAccesoTransaccionalService.class.getMethod(
                "registrarConfirmado",
                ar.gob.malvinas.faltas.core.domain.model.FalActa.class,
                ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr.class,
                String.class, String.class, String.class,
                java.time.LocalDateTime.class);
        TransactionAttribute atributo = ctx.getBean(TransactionAttributeSource.class)
                .getTransactionAttribute(registrarConfirmado, QrAccesoTransaccionalService.class);
        assertThat(atributo)
                .as("registrarConfirmado debe tener atributo transaccional resuelto por Spring")
                .isNotNull();
        assertThat(atributo.isReadOnly()).isFalse();
        assertThat(atributo.getPropagationBehavior())
                .as("propagacion REQUIRED: sin REQUIRES_NEW ni transacciones anidadas")
                .isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    }

    /**
     * {@code QrActaService.registrarAcceso} no debe llevar {@code @Transactional} propia: si la
     * llevara, la llamada interna desde {@code registrarAccesoConNotificacion} (self-invocation)
     * la neutralizaria en silencio. La frontera real vive en el colaborador (caso 34).
     */
    @Test
    @DisplayName("35. QrActaService.registrarAcceso NO tiene @Transactional propia (evita self-invocation neutralizada)")
    void registrarAccesoSinTransactionalPropia() throws Exception {
        Method registrarAcceso = ar.gob.malvinas.faltas.core.application.service.QrActaService.class
                .getMethod("registrarAcceso",
                        String.class,
                        ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr.class,
                        String.class, String.class, String.class);
        assertThat(registrarAcceso.getAnnotation(Transactional.class))
                .as("la frontera real esta en QrAccesoTransaccionalService, no aca")
                .isNull();
    }

    /**
     * {@code ActaService.enriquecer} es la correccion material de
     * {@code POST-B2-TX-CONSUMERS-01}: antes de esa wave no tenia ningun atributo
     * transaccional, de modo que sus escrituras multiples (evento/snapshot) no formaban una
     * unidad atomica. {@code completarCaptura} fue eliminado
     * (DECISION-ACTA-LIFECYCLE-SIN-CAPT-01): el alta es integral y el acta nace directamente en
     * ENRI, sin una captura previa que completar.
     */
    @Test
    @DisplayName("36. enriquecer tiene atributo transaccional efectivo, igual que labrar")
    void enriquecerConFronteraEfectiva() throws Exception {
        ActaService servicio = ctx.getBean(ActaService.class);
        assertThat(AopUtils.isAopProxy(servicio)).isTrue();

        Method enriquecer = ActaService.class.getMethod("enriquecer",
                ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand.class);

        TransactionAttributeSource fuente = ctx.getBean(TransactionAttributeSource.class);

        TransactionAttribute atributoEnriquecer = fuente.getTransactionAttribute(enriquecer, ActaService.class);
        assertThat(atributoEnriquecer).isNotNull();
        assertThat(atributoEnriquecer.isReadOnly()).isFalse();
        assertThat(atributoEnriquecer.getPropagationBehavior())
                .isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
        assertThat(enriquecer.getAnnotation(Transactional.class)).isNotNull();
    }

    // =====================================================================
    // WAVE 4 PARTE 3E-B: los siete puertos retirados fisicamente de src/main
    // =====================================================================

    /**
     * Guardrail arquitectonico §11 del prompt de Parte 3E-B: certifica con un contexto Spring
     * real (no sólo por ausencia de archivo) que, bajo el perfil productivo {@code jdbc}, cada
     * uno de los siete puertos cuyo adaptador InMemory se retiró físicamente de {@code src/main}
     * resuelve exclusivamente al adaptador JDBC, sin ningún bean InMemory activo.
     */
    @Test
    @DisplayName("38. Los siete puertos retirados de src/main se resuelven exclusivamente por JDBC bajo el perfil jdbc")
    void sietePuertosRetiradosResueltosPorJdbc() {
        assertThat(ctx.getBeanNamesForType(NormativaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(NormativaRepository.class)).isInstanceOf(JdbcNormativaRepository.class);

        assertThat(ctx.getBeanNamesForType(TalonarioRepository.class)).hasSize(1);
        assertThat(ctx.getBean(TalonarioRepository.class)).isInstanceOf(JdbcTalonarioRepository.class);

        assertThat(ctx.getBeanNamesForType(TarifarioUnidadFaltasRepository.class)).hasSize(1);
        assertThat(ctx.getBean(TarifarioUnidadFaltasRepository.class))
                .isInstanceOf(JdbcTarifarioUnidadFaltasRepository.class);

        assertThat(ctx.getBeanNamesForType(ValorUnidadFaltasRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ValorUnidadFaltasRepository.class))
                .isInstanceOf(JdbcValorUnidadFaltasRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaValorizacionRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaValorizacionRepository.class))
                .isInstanceOf(JdbcActaValorizacionRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaValorizacionItemRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaValorizacionItemRepository.class))
                .isInstanceOf(JdbcActaValorizacionItemRepository.class);

        assertThat(ctx.getBeanNamesForType(ActaArticuloInfringidoRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaArticuloInfringidoRepository.class))
                .isInstanceOf(JdbcActaArticuloInfringidoRepository.class);
    }

    @Test
    @DisplayName("39. Ninguno de los siete InMemory retirados esta registrado bajo el perfil jdbc")
    void sieteInMemoryRetiradosAusentesBajoJdbc() {
        assertThat(ctx.getBeanNamesForType(InMemoryNormativaRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryTalonarioRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryTarifarioUnidadFaltasRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryValorUnidadFaltasRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryActaValorizacionRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryActaValorizacionItemRepository.class)).isEmpty();
        assertThat(ctx.getBeanNamesForType(InMemoryActaArticuloInfringidoRepository.class)).isEmpty();
    }

    /**
     * §11: "el controller de alta deja de usar ActaAltaTransientRetryService" — certificado aquí
     * con topología de beans real (más allá de la lectura de fuente de
     * {@code Wave4Parte3EBArchitectureGuardrailTest}): bajo {@code jdbc}, el bean intermedio de
     * intento ({@code ActaAltaIntentoTransaccionalService}, Wave 4 Parte 3E-B §3.4/§10) existe,
     * está proxificado transaccionalmente, y su único método tiene propagación
     * {@code REQUIRES_NEW}.
     */
    @Test
    @DisplayName("40. ActaAltaIntentoTransaccionalService existe, esta proxificado, y su metodo es REQUIRES_NEW")
    void intentoTransaccionalRequiresNew() throws Exception {
        ActaAltaIntentoTransaccionalService intento = ctx.getBean(ActaAltaIntentoTransaccionalService.class);
        assertThat(AopUtils.isAopProxy(intento))
                .as("sin proxy REQUIRES_NEW no suspenderia ninguna transaccion ambiente")
                .isTrue();

        Method intentar = ActaAltaIntentoTransaccionalService.class.getMethod("intentar",
                ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand.class);
        TransactionAttribute atributo = ctx.getBean(TransactionAttributeSource.class)
                .getTransactionAttribute(intentar, ActaAltaIntentoTransaccionalService.class);
        assertThat(atributo).isNotNull();
        assertThat(atributo.getPropagationBehavior())
                .as("cada intento debe suspender cualquier transaccion ambiente y abrir una nueva")
                .isEqualTo(TransactionAttribute.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Sin el perfil jdbc (perfil "test", unico bypass del guardrail de arranque desde Wave 4
     * Parte 3E-B: DECISION-JDBC-ONLY-01) los puertos migrados deben seguir resueltos por el
     * adaptador InMemory. Requiere un contexto propio, por eso va anidado con la configuracion
     * de test sobreescrita en vez de heredada.
     *
     * <p>Hasta Wave 4 Parte 3E-B este contexto se levantaba sin ningun perfil activo (el
     * "default" productivo de entonces era memoria). Desde el retiro del modo memory productivo,
     * el unico modo no-jdbc ejecutable es el perfil "test" dedicado de {@code src/test}
     * ({@code application-test.yml}); las aserciones de paridad InMemory no cambian.
     */
    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest
    @ActiveProfiles("test")
    @DisplayName("IT WAVE-0B/1A1: los puertos migrados bajo el perfil 'test' (sin jdbc)")
    class SinPerfilJdbc {

        @Autowired
        private ApplicationContext ctxSinJdbc;

        @Test
        @DisplayName("13. Sin perfil jdbc hay exactamente un ActaEventoRepository y es el InMemory")
        void puertoPilotoResueltoPorInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(ActaEventoRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(ActaEventoRepository.class))
                    .isInstanceOf(InMemoryActaEventoRepository.class);
        }

        @Test
        @DisplayName("14. Sin perfil jdbc el adaptador JDBC no esta registrado")
        void adaptadorJdbcAusente() {
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcActaEventoRepository.class)).isEmpty();
        }

        @Test
        @DisplayName("18. Sin perfil jdbc los dos catalogos de WAVE 1A1 los resuelve el InMemory")
        void catalogosResueltosPorInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(DiaNoComputableRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(DiaNoComputableRepository.class))
                    .isInstanceOf(InMemoryDiaNoComputableRepository.class);

            assertThat(ctxSinJdbc.getBeanNamesForType(MotivoArchivoRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(MotivoArchivoRepository.class))
                    .isInstanceOf(InMemoryMotivoArchivoRepository.class);
        }

        @Test
        @DisplayName("19. Sin perfil jdbc los adaptadores JDBC de WAVE 1A1 no estan registrados")
        void adaptadoresJdbcDeCatalogosAusentes() {
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcDiaNoComputableRepository.class)).isEmpty();
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcMotivoArchivoRepository.class)).isEmpty();
        }

        @Test
        @DisplayName("23. Sin perfil jdbc hay exactamente un ObservacionRepository y es el InMemory")
        void observacionResueltoPorInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(ObservacionRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(ObservacionRepository.class))
                    .isInstanceOf(InMemoryObservacionRepository.class);
        }

        @Test
        @DisplayName("24. Sin perfil jdbc el adaptador JDBC de observaciones no esta registrado")
        void adaptadorJdbcDeObservacionAusente() {
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcObservacionRepository.class)).isEmpty();
        }

        /**
         * Paridad del modo InMemory tras la activacion de B2. Sin el perfil {@code jdbc} los
         * cuatro puertos que B2 migro deben seguir resueltos por su adaptador InMemory y con
         * exactamente un bean cada uno: si alguno quedara con dos, el arranque del modo no JDBC
         * fallaria por ambiguedad.
         */
        @Test
        @DisplayName("30. Sin perfil jdbc los cuatro puertos de B2 los resuelve el InMemory")
        void puertosDeB2ResueltosPorInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(ActaRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(ActaRepository.class))
                    .isInstanceOf(InMemoryActaRepository.class);

            assertThat(ctxSinJdbc.getBeanNamesForType(PersonaRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(PersonaRepository.class))
                    .isInstanceOf(InMemoryPersonaRepository.class);

            assertThat(ctxSinJdbc.getBeanNamesForType(ActaEvidenciaRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(ActaEvidenciaRepository.class))
                    .isInstanceOf(InMemoryActaEvidenciaRepository.class);

            assertThat(ctxSinJdbc.getBeanNamesForType(ActaSnapshotRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(ActaSnapshotRepository.class))
                    .isInstanceOf(InMemoryActaSnapshotRepository.class);
        }

        @Test
        @DisplayName("31. Sin perfil jdbc los cuatro adaptadores JDBC de B2 no estan registrados")
        void adaptadoresJdbcDeB2Ausentes() {
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcActaRepository.class)).isEmpty();
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcPersonaRepository.class)).isEmpty();
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcActaEvidenciaRepository.class)).isEmpty();
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcActaSnapshotRepository.class)).isEmpty();
        }

        /**
         * Paridad del modo InMemory tras {@code POST-B2-TX-CONSUMERS-01}: sin el perfil
         * {@code jdbc} el acceso QR sigue resuelto por su adaptador InMemory, con exactamente un
         * bean.
         */
        @Test
        @DisplayName("37. Sin perfil jdbc hay exactamente un QrAccesoRepository y es el InMemory")
        void qrAccesoResueltoPorInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(QrAccesoRepository.class)).hasSize(1);
            assertThat(ctxSinJdbc.getBean(QrAccesoRepository.class))
                    .isInstanceOf(InMemoryQrAccesoRepository.class);
            assertThat(ctxSinJdbc.getBeanNamesForType(JdbcQrAccesoRepository.class)).isEmpty();
        }

        /**
         * Sin perfil {@code jdbc} no hay DataSource principal, de modo que tampoco hay
         * transaction manager. La anotacion de {@code labrar} queda sin efecto y el alta se
         * comporta como antes de B2: ordenada, pero sin promesa de rollback. Es la consecuencia
         * declarada del modo InMemory, no un defecto, y por eso <b>no</b> se agrega un
         * transaction manager ficticio para simularla.
         */
        @Test
        @DisplayName("32. Sin perfil jdbc no se agrega ningun transaction manager ficticio")
        void sinTransactionManagerFicticioEnInMemory() {
            assertThat(ctxSinJdbc.getBeanNamesForType(PlatformTransactionManager.class))
                    .as("el modo InMemory no simula ACID con un transaction manager inventado")
                    .isEmpty();
            assertThat(ctxSinJdbc.getBeanNamesForType(DataSource.class)).isEmpty();
        }
    }
}

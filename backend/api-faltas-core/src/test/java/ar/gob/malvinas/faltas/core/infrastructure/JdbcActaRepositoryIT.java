package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenCaptura;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaRepository;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcObservacionRepository;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de JdbcActaRepository sobre fal_acta (WAVE 1A2-B1).
 *
 * Ejecuta contra el sandbox real sb_faltas_db mediante Failsafe:
 *   mvn -P integration-tests verify
 *
 * <p>El adaptador todavia NO es un bean: este IT lo construye a mano con el
 * JdbcClient del contexto y el ObservacionRepository JDBC vigente. El puerto
 * ActaRepository de la aplicacion sigue resuelto por InMemoryActaRepository incluso
 * con el perfil jdbc activo, y varios casos lo verifican explicitamente.
 *
 * <p>Disciplina de datos, la misma de JdbcObservacionRepositoryIT:
 * - todo escenario corre dentro de una transaccion que se revierte siempre;
 * - la cadena minima de padres se construye en la propia transaccion respetando el
 *   DDL real (fal_dependencia -> fal_dependencia_version -> fal_persona);
 * - los textos propios llevan la marca WAVE1A2_B1_;
 * - no hay TRUNCATE ni DELETE y no se desactiva ninguna FK: la limpieza es el
 *   rollback.
 *
 * <p>Sobre las PK: no se comparan identificadores exactos entre adaptadores ni se
 * afirma una secuencia sin huecos. Un INSERT rechazado consume un valor de
 * AUTO_INCREMENT sin dejar fila y eso es correcto.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B1: JdbcActaRepository candidato sobre fal_acta")
class JdbcActaRepositoryIT {

    private static final String MARCA = "WAVE1A2_B1_";

    /** DATETIME(6) guarda microsegundos, no nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 3, 16, 21, 33, 456_789_000);

    private static final LocalDate F0 = LocalDate.of(2026, 8, 3);

    /** Coordenadas con exactamente 7 decimales: DECIMAL(10,7) las conserva tal cual. */
    private static final double LAT_EXACTA = -34.5678901;
    private static final double LON_EXACTA = -58.1234567;

    /** Id sin fila en su tabla padre, para ejercer una FK. */
    private static final long SIN_PADRE = 987_654_321L;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private ObservacionRepository observaciones;

    /** El candidato de esta wave: construido a mano, nunca obtenido del contexto. */
    private JdbcActaRepository repositorio;

    @BeforeEach
    void construirCandidatoAMano() {
        repositorio = new JdbcActaRepository(jdbc, observaciones);
    }

    // =====================================================================
    // TOPOLOGIA: EL ADAPTADOR ACTIVADO EN WAVE 1A2-B2
    // =====================================================================

    /**
     * Casos 1 y 2 invertidos en WAVE 1A2-B2, el slice que activa el adaptador. Hasta B1.5C
     * verificaban que el puerto siguiera resuelto por InMemory y que la clase no llevara
     * anotaciones; desde B2 verifican la activacion y su restriccion de perfil, que es la
     * propiedad que reemplaza a la anterior.
     *
     * <p>El resto de la clase sigue construyendo el adaptador a mano en {@code @BeforeEach}. No
     * es contradictorio: construirlo a mano mantiene los escenarios independientes del contexto
     * y permite decorarlo para contar invocaciones, que es lo que hacen los casos de
     * cardinalidad.
     */
    @Test
    @DisplayName("1. Con perfil jdbc el puerto del acta lo resuelve JdbcActaRepository")
    void puertoDelActaResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(ActaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(ActaRepository.class)).isInstanceOf(JdbcActaRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryActaRepository.class))
                .as("el InMemory queda fuera del perfil jdbc")
                .isEmpty();
    }

    @Test
    @DisplayName("2. JdbcActaRepository es bean unico bajo jdbc, sin @Primary que esconda ambiguedad")
    void adaptadorRegistradoSoloBajoPerfilJdbc() {
        assertThat(ctx.getBeanNamesForType(JdbcActaRepository.class)).hasSize(1);
        assertThat(JdbcActaRepository.class.getAnnotation(org.springframework.stereotype.Repository.class))
                .as("bean de persistencia")
                .isNotNull();
        assertThat(JdbcActaRepository.class.getAnnotation(org.springframework.context.annotation.Profile.class))
                .as("restringido al perfil jdbc")
                .isNotNull();
        assertThat(JdbcActaRepository.class.getAnnotation(
                org.springframework.context.annotation.Profile.class).value())
                .containsExactly("jdbc");
        assertThat(JdbcActaRepository.class.getAnnotation(org.springframework.context.annotation.Primary.class))
                .as("una sola implementacion por puerto: @Primary esconderia una ambiguedad")
                .isNull();
    }

    /**
     * El puerto declara siete metodos: los seis de Wave 4 PARTE-3A (que agrego la lectura de
     * bloqueo de idempotencia concurrente {@code buscarPorUuidTecnicoConLock}, prompt §12.5)
     * mas {@code buscarPorIdConLock} de Wave 4 Parte 3D (bloqueo de fila para asignar orden de
     * fotografias en carga posterior, prompt S5.6/S16). El adaptador implementa los siete: la
     * clase es concreta y no queda ningun metodo sin cuerpo. Fija la resolucion del hallazgo
     * H-B1-01.
     */
    @Test
    @DisplayName("3. El adaptador implementa los 7 metodos del puerto y es una clase concreta")
    void implementaElContratoCompleto() {
        assertThat(ActaRepository.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .containsExactlyInAnyOrder("guardar", "buscarPorId", "buscarPorUuidTecnico",
                        "buscarPorUuidTecnicoConLock", "buscarPorIdConLock", "listarTodas", "listarPagina");
        assertThat(Modifier.isAbstract(JdbcActaRepository.class.getModifiers())).isFalse();
        assertThat(ActaRepository.class).isAssignableFrom(JdbcActaRepository.class);
    }

    @Test
    @DisplayName("4. La composicion de lectura usa el adaptador JDBC de observaciones")
    void composicionUsaElPuertoJdbcDeObservaciones() {
        assertThat(observaciones).isInstanceOf(JdbcObservacionRepository.class);
    }

    @Test
    @DisplayName("5. El constructor exige sus dos dependencias")
    void constructorExigeDependencias() {
        assertThatThrownBy(() -> new JdbcActaRepository(null, observaciones))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JdbcActaRepository(jdbc, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS
    // =====================================================================

    @Test
    @DisplayName("6. Alta con id nulo: el motor asigna una PK positiva y version_row queda en 0")
    void altaConIdNuloRecibePkPositivaYVersionCero() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaMinima(padres, "alta"));

            assertThat(persistida.getId()).isNotNull().isPositive();
            assertThat(persistida.getVersionRow()).isZero();
            assertThat(versionCruda(persistida.getId())).isZero();
            return null;
        });
    }

    @Test
    @DisplayName("7. El INSERT no informa la PK: dos altas reciben identificadores distintos")
    void dosAltasRecibenIdsDistintos() {
        enEscenario(padres -> {
            Long primera = repositorio.guardar(actaMinima(padres, "una")).getId();
            Long segunda = repositorio.guardar(actaMinima(padres, "otra")).getId();

            assertThat(segunda).isNotNull().isNotEqualTo(primera);
            return null;
        });
    }

    @Test
    @DisplayName("8. guardar no muta el argumento del alta: el candidato conserva id nulo")
    void altaNoMutaElArgumento() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "sin mutar");

            FalActa persistida = repositorio.guardar(candidata);

            assertThat(candidata.getId()).isNull();
            assertThat(candidata.getVersionRow()).isZero();
            assertThat(persistida.getId()).isNotNull();
            return null;
        });
    }

    @Test
    @DisplayName("9. Un alta con versionRow distinto de 0 se rechaza igual que en InMemory")
    void altaConVersionInicialInvalida() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "version invalida");
            candidata.setVersionRow(3);

            assertThatThrownBy(() -> repositorio.guardar(candidata))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("versionRow=0");
            return null;
        });
    }

    @Test
    @DisplayName("10. guardar rechaza un acta nula con el mismo mensaje que InMemory")
    void guardarRechazaNulo() {
        Throwable deJdbc = catchThrowable(() -> repositorio.guardar(null));
        Throwable deMemoria = catchThrowable(() -> new InMemoryActaRepository().guardar(null));

        assertThat(deJdbc).isInstanceOf(IllegalArgumentException.class);
        assertThat(deJdbc.getMessage()).isEqualTo(deMemoria.getMessage());
    }

    // =====================================================================
    // MAPEO 57 DE 57
    // =====================================================================

    /**
     * Round-trip completo. El unico valor que no viaja por fal_acta es
     * {@code observaciones}: se siembra en fal_observacion con el vinculo propietario
     * de ACTA y la lectura lo reconstruye desde ahi.
     *
     * <p>infractorNombre e infractorDocumento vuelven nulos porque fal_acta no los
     * persiste: los datos del infractor viven en fal_persona.
     */
    @Test
    @DisplayName("11. Round-trip de las 57 columnas de fal_acta en una sola lectura")
    void roundTripDeLas57Columnas() {
        enEscenario(padres -> {
            String textoInicial = MARCA + "observacion inicial del labrado";
            FalActa candidata = actaCompleta(padres, textoInicial);

            FalActa persistida = repositorio.guardar(candidata);
            observaciones.guardar(observacionDeActa(persistida.getId(), textoInicial, T0));

            FalActa leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();

            // Identidad y numeracion
            assertThat(leida.getId()).isEqualTo(persistida.getId());
            assertThat(leida.getUuidTecnico()).isEqualTo(candidata.getUuidTecnico());
            assertThat(leida.getNroActa()).isEqualTo(candidata.getNroActa());
            assertThat(leida.getIdTalonario()).isNull();
            assertThat(leida.getNroTalonarioUsado()).isNull();

            // Tipo y captura
            assertThat(leida.getTipoActa()).isEqualTo(TipoActa.CONTRAVENCION);
            assertThat(leida.getOrigenCaptura()).isEqualTo(OrigenCaptura.MOBILE_INSPECTOR);
            assertThat(leida.getIdDispositivoCaptura()).isEqualTo(MARCA + "DISPOSITIVO-01");
            assertThat(leida.getIdUserCaptura()).isEqualTo(candidata.getIdUserCaptura());
            assertThat(leida.getFhCaptura()).isEqualTo(T0);
            assertThat(leida.getLatCaptura()).isEqualTo(LAT_EXACTA);
            assertThat(leida.getLonCaptura()).isEqualTo(LON_EXACTA);
            assertThat(leida.getPrecisionCapturaM()).isEqualTo(12.75);
            assertThat(leida.getFhPosCaptura()).isEqualTo(T0.plusSeconds(30));
            assertThat(leida.getOrigenPosCaptura()).isEqualTo(OrigenUbicacion.GPS_DISPOSITIVO);

            // Fechas funcionales
            assertThat(leida.getFechaActa()).isEqualTo(F0);
            assertThat(leida.getFechaLabrado()).isEqualTo(T0);

            // Dependencia e inspector
            assertThat(leida.getIdDependencia()).isEqualTo(padres.idDep());
            assertThat(leida.getVerDep()).isEqualTo((int) padres.verDep());
            assertThat(leida.getIdInspector()).isNull();
            assertThat(leida.getVerInsp()).isNull();

            // Infractor y domicilios
            assertThat(leida.getSituacionIdentificacionInfractor())
                    .isEqualTo(SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO);
            assertThat(leida.getIdPersonaInfractor()).isEqualTo(padres.idPersona());
            assertThat(leida.getIdDomicilioInfractorAct()).isNull();
            assertThat(leida.getIdDomicilioNotifAct()).isNull();

            // Hecho
            assertThat(leida.getResumenHecho()).isEqualTo(MARCA + "resumen del hecho");
            assertThat(leida.getDomicilioHecho()).isEqualTo(MARCA + "domicilio del hecho");

            // Lugar del hecho
            assertThat(leida.getIdLocInfrMalvinas()).isEqualTo("0001");
            assertThat(leida.getLocalidadInfrMalvinasVersionId()).isNull();
            assertThat(leida.getIdTcaInfrMalvinas()).isEqualTo("4-S-0-0-7C");
            assertThat(leida.getCalleInfrMalvinasVersionId()).isNull();
            assertThat(leida.getAlturaInfr()).isEqualTo(1234);
            assertThat(leida.getAlturaOrigenInfr()).isEqualTo((short) 2);
            assertThat(leida.isSiAlturaInfrEstimada()).isTrue();
            assertThat(leida.getLatInfr()).isEqualTo(LAT_EXACTA);
            assertThat(leida.getLonInfr()).isEqualTo(LON_EXACTA);
            assertThat(leida.getOrigenUbicacionInfr()).isEqualTo(OrigenUbicacion.PARCELA_SELECCIONADA);
            assertThat(leida.isSiUbicacionInfrManual()).isTrue();
            assertThat(leida.isSiDomTxtInfr()).isTrue();
            assertThat(leida.getDomTxtInfr()).isEqualTo(MARCA + "texto libre del lugar");
            assertThat(leida.isSiEjeUrb()).isTrue();

            // QR
            assertThat(leida.getCodigoQr()).isEqualTo(candidata.getCodigoQr());
            assertThat(leida.getQrPayloadVersion()).isEqualTo(3);

            // Tripla canonica y resultado
            assertThat(leida.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(leida.getEstadoProcesal()).isEqualTo(EstadoProcesalActa.CONCLUIDO);
            assertThat(leida.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ARCHIVADA);
            assertThat(leida.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(leida.getResultadoFirmaInfractor())
                    .isEqualTo(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);

            // Caches de archivo y cierre
            assertThat(leida.getIdMotivoArchivoActual()).isNull();
            assertThat(leida.getPermiteReingreso()).isTrue();
            assertThat(leida.getFhCierre()).isEqualTo(T0.plusDays(1));
            assertThat(leida.getFhArchivo()).isEqualTo(T0.plusDays(2));

            // OCC y auditoria
            assertThat(leida.getVersionRow()).isZero();
            assertThat(leida.getFhAlta()).isEqualTo(T0);
            assertThat(leida.getIdUserAlta()).isEqualTo(candidata.getIdUserAlta());
            assertThat(leida.getFhUltMod()).isEqualTo(T0.plusHours(5));
            assertThat(leida.getIdUserUltMod()).isEqualTo(candidata.getIdUserUltMod());

            // Observacion inicial, reconstruida desde fal_observacion
            assertThat(leida.getObservaciones()).isEqualTo(textoInicial);

            // Campos de compatibilidad que fal_acta no persiste
            assertThat(leida.getInfractorNombre()).isNull();
            assertThat(leida.getInfractorDocumento()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("12. Las cuatro coordenadas viajan por DECIMAL(10,7) sin alterar 7 decimales")
    void coordenadasConSieteDecimales() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaCompleta(padres, null));
            FalActa leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();

            assertThat(leida.getLatCaptura()).isEqualTo(LAT_EXACTA);
            assertThat(leida.getLonCaptura()).isEqualTo(LON_EXACTA);
            assertThat(leida.getLatInfr()).isEqualTo(LAT_EXACTA);
            assertThat(leida.getLonInfr()).isEqualTo(LON_EXACTA);

            assertThat(escalaCruda(persistida.getId(), "lat_infr"))
                    .as("la columna almacena la escala fisica completa")
                    .isEqualTo("-34.5678901");
            return null;
        });
    }

    /**
     * DECIMAL(10,7) conserva la precision contractual del esquema, que es siete
     * decimales. Un double con mas decimales se redondea de forma explicita y
     * determinista: no es una conversion sin perdida en terminos absolutos.
     */
    @Test
    @DisplayName("13. Un double con mas de 7 decimales se redondea a la escala de la columna")
    void coordenadasSeRedondeanALaEscalaFisica() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "redondeo");
            candidata.setLatCaptura(12.123456789);
            candidata.setLonCaptura(-9.987654321);

            FalActa leida = repositorio.buscarPorId(repositorio.guardar(candidata).getId()).orElseThrow();

            assertThat(leida.getLatCaptura()).isEqualTo(12.1234568);
            assertThat(leida.getLonCaptura()).isEqualTo(-9.9876543);
            return null;
        });
    }

    @Test
    @DisplayName("14. precision_captura_m viaja por DECIMAL(8,2): son metros, no grados")
    void precisionEnMetrosConDosDecimales() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "metros");
            candidata.setPrecisionCapturaM(15.678);

            FalActa leida = repositorio.buscarPorId(repositorio.guardar(candidata).getId()).orElseThrow();

            assertThat(leida.getPrecisionCapturaM()).isEqualTo(15.68);
            assertThat(escalaCruda(leida.getId(), "precision_captura_m")).isEqualTo("15.68");
            return null;
        });
    }

    /**
     * Los nueve enums del acta viajan por su codigo fisico, verificado contra las
     * columnas crudas. Nunca ordinal(), name() ni toString().
     */
    @Test
    @DisplayName("15. Los 9 enums del acta se guardan por su codigo fisico")
    void enumsPorCodigoFisico() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaCompleta(padres, null)).getId();

            assertThat(cruda(id, "tipo_acta")).isEqualTo("2");
            assertThat(cruda(id, "origen_captura")).isEqualTo("1");
            assertThat(cruda(id, "origen_pos_captura")).isEqualTo("6");
            assertThat(cruda(id, "origen_ubicacion_infr")).isEqualTo("5");
            assertThat(cruda(id, "resultado_final")).isEqualTo("3");
            assertThat(cruda(id, "resultado_firma_infractor")).isEqualTo("2");
            assertThat(cruda(id, "situacion_identificacion_infractor")).isEqualTo("1");
            assertThat(cruda(id, "bloque_actual")).isEqualTo("ANAL");
            assertThat(cruda(id, "est_proc_act")).isEqualTo("CONC");
            assertThat(cruda(id, "sit_adm_act")).isEqualTo("ARCH");

            assertThat((short) 2).isEqualTo(TipoActa.CONTRAVENCION.codigo());
            assertThat((short) 6).isEqualTo(OrigenUbicacion.GPS_DISPOSITIVO.codigo());
            assertThat("ANAL").isEqualTo(BloqueActual.ANAL.codigo());
            assertThat((short) 1)
                    .isEqualTo(SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO.codigo());
            return null;
        });
    }

    @Test
    @DisplayName("16. Las banderas booleanas viajan como TINYINT(1) 0/1")
    void booleanosComoTinyint() {
        enEscenario(padres -> {
            Long conBanderas = repositorio.guardar(actaCompleta(padres, null)).getId();
            Long sinBanderas = repositorio.guardar(actaMinima(padres, "banderas")).getId();

            assertThat(cruda(conBanderas, "si_altura_infr_estimada")).isEqualTo("1");
            assertThat(cruda(conBanderas, "si_ubicacion_infr_manual")).isEqualTo("1");
            assertThat(cruda(conBanderas, "si_dom_txt_infr")).isEqualTo("1");
            assertThat(cruda(conBanderas, "si_eje_urb")).isEqualTo("1");
            assertThat(cruda(conBanderas, "permite_reingreso")).isEqualTo("1");

            assertThat(cruda(sinBanderas, "si_altura_infr_estimada")).isEqualTo("0");
            assertThat(cruda(sinBanderas, "permite_reingreso"))
                    .as("permite_reingreso es nullable: NULL no es FALSE")
                    .isNull();
            return null;
        });
    }

    // =====================================================================
    // LECTURAS POR CLAVE
    // =====================================================================

    @Test
    @DisplayName("17. buscarPorId encuentra un acta existente y devuelve Optional vacio si no existe")
    void buscarPorId() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "por id")).getId();

            assertThat(repositorio.buscarPorId(id)).isPresent();
            assertThat(repositorio.buscarPorId(-1L)).isEmpty();
            assertThat(repositorio.buscarPorId(null))
                    .as("divergencia residual ya registrada: InMemory lanza NPE con clave nula")
                    .isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("18. buscarPorUuidTecnico encuentra por id_tecnico y distingue ausencia, nulo y blanco")
    void buscarPorUuidTecnico() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "por uuid");
            Long id = repositorio.guardar(candidata).getId();

            assertThat(repositorio.buscarPorUuidTecnico(candidata.getUuidTecnico()))
                    .get()
                    .extracting(FalActa::getId)
                    .isEqualTo(id);
            assertThat(repositorio.buscarPorUuidTecnico(UUID.randomUUID().toString())).isEmpty();
            assertThat(repositorio.buscarPorUuidTecnico(null)).isEmpty();
            assertThat(repositorio.buscarPorUuidTecnico("")).isEmpty();
            assertThat(repositorio.buscarPorUuidTecnico("   ")).isEmpty();
            return null;
        });
    }

    /**
     * La collation de fal_acta es utf8mb4_uca1400_ai_ci, insensible a mayusculas. El
     * contrato del puerto compara exacto, como String.equals, y el adaptador lo
     * sostiene con CAST(id_tecnico AS BINARY) sin renunciar a uq_fal_acta_id_tecnico.
     * Misma decision ya ratificada en D-1A1-05 para referencia_externa.
     */
    @Test
    @DisplayName("19. buscarPorUuidTecnico es exacto: distingue mayusculas y espacios finales")
    void buscarPorUuidTecnicoEsExacto() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "exacto");
            String uuid = candidata.getUuidTecnico();
            repositorio.guardar(candidata);

            ActaRepository enMemoria = new InMemoryActaRepository();
            enMemoria.guardar(actaConUuid(padres, uuid));

            assertThat(repositorio.buscarPorUuidTecnico(uuid.toUpperCase()))
                    .as("la collation _ai_ci no debe filtrarse al contrato")
                    .isEmpty();
            assertThat(enMemoria.buscarPorUuidTecnico(uuid.toUpperCase())).isEmpty();

            assertThat(repositorio.buscarPorUuidTecnico(uuid + " ")).isEmpty();
            assertThat(enMemoria.buscarPorUuidTecnico(uuid + " ")).isEmpty();

            assertThat(repositorio.buscarPorUuidTecnico(uuid)).isPresent();
            assertThat(enMemoria.buscarPorUuidTecnico(uuid)).isPresent();
            return null;
        });
    }

    // =====================================================================
    // COMPOSICION DE LA OBSERVACION INICIAL
    // =====================================================================

    @Test
    @DisplayName("20. Un acta sin observaciones se lee con observaciones nulas")
    void actaSinObservacionesLeeNulo() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "sin obs")).getId();

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("21. Con una observacion ACTA, la lectura reconstruye su texto exacto")
    void unaObservacionSeReconstruye() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "una obs")).getId();
            String texto = MARCA + "unica observacion";
            observaciones.guardar(observacionDeActa(id, texto, T0));

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones()).isEqualTo(texto);
            return null;
        });
    }

    @Test
    @DisplayName("22. Con varias observaciones se toma la primera historica, no la ultima")
    void variasObservacionesTomanLaPrimeraHistorica() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "varias obs")).getId();
            observaciones.guardar(observacionDeActa(id, MARCA + "tardia", T0.plusHours(2)));
            observaciones.guardar(observacionDeActa(id, MARCA + "inicial", T0));
            observaciones.guardar(observacionDeActa(id, MARCA + "media", T0.plusHours(1)));

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones())
                    .isEqualTo(MARCA + "inicial");
            return null;
        });
    }

    @Test
    @DisplayName("23. Con el mismo fh_alta el desempate por id decide la observacion inicial")
    void empateDeInstanteSeDesempataPorId() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "empate")).getId();
            observaciones.guardar(observacionDeActa(id, MARCA + "primera del instante", T0));
            observaciones.guardar(observacionDeActa(id, MARCA + "segunda del instante", T0));

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones())
                    .isEqualTo(MARCA + "primera del instante");
            return null;
        });
    }

    /**
     * Se usa el historial completo y no solo las activas: la observacion capturada
     * durante el labrado sigue siendo la inicial aunque se haya dado de baja y
     * corregido con una fila nueva.
     */
    @Test
    @DisplayName("24. Una observacion inicial dada de baja sigue siendo la inicial historica")
    void observacionInicialInactivaSigueSiendoLaInicial() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "inicial inactiva")).getId();
            FalObservacion inicial =
                    observaciones.guardar(observacionDeActa(id, MARCA + "inicial dada de baja", T0));
            observaciones.guardar(observacionDeActa(id, MARCA + "correccion", T0.plusDays(1)));
            observaciones.desactivar(inicial.getId(), T0.plusDays(1), actor());

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones())
                    .isEqualTo(MARCA + "inicial dada de baja");
            return null;
        });
    }

    @Test
    @DisplayName("25. La composicion no mezcla otros entidad_tipo del mismo entidad_id")
    void composicionAislaElTipoDeEntidad() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "aislamiento")).getId();
            observaciones.guardar(new FalObservacion(null, EntidadTipoObservada.PARALIZACION, id, null,
                    MARCA + "de la paralizacion", OrigenObservacion.USUARIO, id, T0.minusDays(1), actor()));

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones())
                    .as("PARALIZACION con el mismo entidad_id no es la observacion del acta")
                    .isNull();
            return null;
        });
    }

    /**
     * DECISION_OBS-01: id_acta_contexto es contexto, nunca propietario. Una observacion
     * de otra entidad que apunta al acta por contexto no aporta observaciones al acta.
     */
    @Test
    @DisplayName("26. id_acta_contexto no se usa para reconstruir la observacion del acta")
    void contextoNoIdentificaAlPropietario() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "contexto")).getId();
            observaciones.guardar(new FalObservacion(null, EntidadTipoObservada.DOCUMENTO, SIN_PADRE, null,
                    MARCA + "de un documento del expediente", OrigenObservacion.USUARIO, id, T0, actor()));

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservaciones()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("27. buscarPorUuidTecnico compone la observacion inicial igual que buscarPorId")
    void busquedaPorUuidTambienCompone() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "uuid compone");
            Long id = repositorio.guardar(candidata).getId();
            String texto = MARCA + "inicial por uuid";
            observaciones.guardar(observacionDeActa(id, texto, T0));

            assertThat(repositorio.buscarPorUuidTecnico(candidata.getUuidTecnico()).orElseThrow()
                    .getObservaciones()).isEqualTo(texto);
            return null;
        });
    }

    @Test
    @DisplayName("28. La observacion nunca se toma de resumen_hecho")
    void observacionNoSaleDeResumenHecho() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "resumen");
            candidata.setResumenHecho(MARCA + "resumen que no es observacion");
            Long id = repositorio.guardar(candidata).getId();

            FalActa leida = repositorio.buscarPorId(id).orElseThrow();
            assertThat(leida.getResumenHecho()).isEqualTo(MARCA + "resumen que no es observacion");
            assertThat(leida.getObservaciones()).isNull();
            return null;
        });
    }

    /**
     * guardar escribe exclusivamente fal_acta: el texto que trae el argumento no crea
     * ninguna fila de observacion. Esa alta conjunta es responsabilidad de
     * ActaService.labrar (DECISION_OBS-TX-01), todavia no implementada.
     */
    @Test
    @DisplayName("29. guardar no escribe fal_observacion pero conserva observaciones en la copia")
    void guardarNoEscribeObservacionesYLasConservaEnLaCopia() {
        enEscenario(padres -> {
            String texto = MARCA + "texto que no se persiste aca";
            FalActa candidata = actaCompleta(padres, texto);

            FalActa persistida = repositorio.guardar(candidata);

            assertThat(persistida.getObservaciones()).isEqualTo(texto);
            assertThat(observaciones.listarPorEntidad(EntidadTipoObservada.ACTA, persistida.getId()))
                    .as("el adaptador del acta no inserta observaciones")
                    .isEmpty();
            assertThat(repositorio.buscarPorId(persistida.getId()).orElseThrow().getObservaciones())
                    .as("sin fila en fal_observacion la lectura devuelve nulo")
                    .isNull();
            return null;
        });
    }

    // =====================================================================
    // ACTUALIZACION OCC
    // =====================================================================

    @Test
    @DisplayName("30. Update OCC exitoso: incremento exacto de uno en la columna y en la copia")
    void updateOccIncrementaExactamenteUno() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaMinima(padres, "occ ok"));
            persistida.setResumenHecho(MARCA + "resumen actualizado");

            FalActa actualizada = repositorio.guardar(persistida);

            assertThat(actualizada.getVersionRow()).isEqualTo(1);
            assertThat(versionCruda(persistida.getId())).isEqualTo(1);
            assertThat(repositorio.buscarPorId(persistida.getId()).orElseThrow().getResumenHecho())
                    .isEqualTo(MARCA + "resumen actualizado");
            return null;
        });
    }

    @Test
    @DisplayName("31. El update no muta el argumento: su version sigue siendo la entrante")
    void updateNoMutaElArgumento() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaMinima(padres, "occ sin mutar"));

            FalActa actualizada = repositorio.guardar(persistida);

            assertThat(persistida.getVersionRow()).as("el argumento conserva su version").isZero();
            assertThat(actualizada.getVersionRow()).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("32. Version obsoleta: ConcurrenciaConflictoException, no 404, y la fila no cambia")
    void versionObsoletaEsConflicto() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaMinima(padres, "occ conflicto"));
            repositorio.guardar(persistida);

            FalActa conVersionVieja = persistida.copia();
            conVersionVieja.setResumenHecho(MARCA + "no debe escribirse");

            assertThatThrownBy(() -> repositorio.guardar(conVersionVieja))
                    .isInstanceOf(ConcurrenciaConflictoException.class)
                    .isNotInstanceOf(ActaNoEncontradaException.class)
                    .hasMessageContaining("version almacenada=1")
                    .hasMessageContaining("version entrante=0");

            assertThat(versionCruda(persistida.getId())).isEqualTo(1);
            assertThat(repositorio.buscarPorId(persistida.getId()).orElseThrow().getResumenHecho())
                    .isNull();
            return null;
        });
    }

    @Test
    @DisplayName("33. Id inexistente: ActaNoEncontradaException, no 409, y no inserta")
    void idInexistenteEsNoEncontrado() {
        enEscenario(padres -> {
            FalActa fantasma = actaMinima(padres, "fantasma").conId(SIN_PADRE);

            assertThatThrownBy(() -> repositorio.guardar(fantasma))
                    .isInstanceOf(ActaNoEncontradaException.class)
                    .isNotInstanceOf(ConcurrenciaConflictoException.class)
                    .hasMessage("Acta no encontrada: " + SIN_PADRE);

            assertThat(repositorio.buscarPorId(SIN_PADRE))
                    .as("un id presente que no existe nunca se inserta")
                    .isEmpty();
            return null;
        });
    }

    /**
     * El SET del UPDATE lista las columnas completas y no se construye omitiendo
     * nulos: pasar una columna nullable de un valor a NULL debe persistirse.
     */
    @Test
    @DisplayName("34. El update lleva columnas nullable a NULL de forma efectiva")
    void updateLlevaNullablesANull() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(actaCompleta(padres, null));

            persistida.setNroActa(null);
            persistida.setResumenHecho(null);
            persistida.setLatCaptura(null);
            persistida.setLonCaptura(null);
            persistida.setPrecisionCapturaM(null);
            persistida.setAlturaInfr(null);
            persistida.setPermiteReingreso(null);
            persistida.setFhCierre(null);
            persistida.setIdUserUltMod(null);
            persistida.setOrigenPosCaptura(null);

            repositorio.guardar(persistida);
            FalActa leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();

            assertThat(leida.getNroActa()).isNull();
            assertThat(leida.getResumenHecho()).isNull();
            assertThat(leida.getLatCaptura()).isNull();
            assertThat(leida.getLonCaptura()).isNull();
            assertThat(leida.getPrecisionCapturaM()).isNull();
            assertThat(leida.getAlturaInfr()).isNull();
            assertThat(leida.getPermiteReingreso()).isNull();
            assertThat(leida.getFhCierre()).isNull();
            assertThat(leida.getIdUserUltMod()).isNull();
            assertThat(leida.getOrigenPosCaptura()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("35. El update no escribe fal_observacion y conserva observaciones del argumento")
    void updateNoTocaObservaciones() {
        enEscenario(padres -> {
            String texto = MARCA + "observacion preexistente";
            FalActa persistida = repositorio.guardar(actaCompleta(padres, texto));
            observaciones.guardar(observacionDeActa(persistida.getId(), texto, T0));

            FalActa actualizada = repositorio.guardar(persistida);

            assertThat(actualizada.getObservaciones()).isEqualTo(texto);
            assertThat(observaciones.listarPorEntidad(EntidadTipoObservada.ACTA, persistida.getId()))
                    .as("el update no agrega ni da de baja observaciones")
                    .hasSize(1);
            return null;
        });
    }

    // =====================================================================
    // LISTARTODAS
    // =====================================================================

    @Test
    @DisplayName("36. listarTodas no devuelve actas del slice antes de crear ninguna")
    void listarTodasSinActasDelSlice() {
        enEscenario(padres -> {
            assertThat(soloDelSlice(repositorio.listarTodas())).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("37. listarTodas ordena por id ASC de forma explicita")
    void listarTodasOrdenaPorIdAscendente() {
        enEscenario(padres -> {
            Long primera = repositorio.guardar(actaMinima(padres, "orden 1")).getId();
            Long segunda = repositorio.guardar(actaMinima(padres, "orden 2")).getId();
            Long tercera = repositorio.guardar(actaMinima(padres, "orden 3")).getId();

            assertThat(soloDelSlice(repositorio.listarTodas()))
                    .extracting(FalActa::getId)
                    .containsExactly(primera, segunda, tercera)
                    .isSorted();
            return null;
        });
    }

    /**
     * listarTodas conserva el contrato completo del puerto: cada acta vuelve con su
     * observacion inicial reconstruida, con las mismas reglas que las dos lecturas por
     * clave.
     */
    @Test
    @DisplayName("38. listarTodas compone la observacion inicial de cada acta")
    void listarTodasComponeCadaActa() {
        enEscenario(padres -> {
            Long sinObs = repositorio.guardar(actaMinima(padres, "lista sin obs")).getId();
            Long conUna = repositorio.guardar(actaMinima(padres, "lista con una")).getId();
            Long conVarias = repositorio.guardar(actaMinima(padres, "lista con varias")).getId();

            observaciones.guardar(observacionDeActa(conUna, MARCA + "unica de la lista", T0));
            observaciones.guardar(observacionDeActa(conVarias, MARCA + "tardia de la lista", T0.plusHours(3)));
            observaciones.guardar(observacionDeActa(conVarias, MARCA + "inicial de la lista", T0));

            List<FalActa> listadas = soloDelSlice(repositorio.listarTodas());

            assertThat(observacionDe(listadas, sinObs)).isNull();
            assertThat(observacionDe(listadas, conUna)).isEqualTo(MARCA + "unica de la lista");
            assertThat(observacionDe(listadas, conVarias)).isEqualTo(MARCA + "inicial de la lista");
            return null;
        });
    }

    @Test
    @DisplayName("39. listarTodas conserva la observacion inicial aunque este dada de baja")
    void listarTodasConservaLaInicialInactiva() {
        enEscenario(padres -> {
            Long id = repositorio.guardar(actaMinima(padres, "lista inicial inactiva")).getId();
            FalObservacion inicial =
                    observaciones.guardar(observacionDeActa(id, MARCA + "inicial de baja", T0));
            observaciones.guardar(observacionDeActa(id, MARCA + "correccion de lista", T0.plusDays(1)));
            observaciones.desactivar(inicial.getId(), T0.plusDays(1), actor());

            assertThat(observacionDe(soloDelSlice(repositorio.listarTodas()), id))
                    .isEqualTo(MARCA + "inicial de baja");
            return null;
        });
    }

    @Test
    @DisplayName("40. listarTodas no mezcla observaciones entre propietarios ni con el contexto")
    void listarTodasNoMezclaPropietarios() {
        enEscenario(padres -> {
            Long unaActa = repositorio.guardar(actaMinima(padres, "lista propia A")).getId();
            Long otraActa = repositorio.guardar(actaMinima(padres, "lista propia B")).getId();

            observaciones.guardar(observacionDeActa(unaActa, MARCA + "solo de A", T0));
            observaciones.guardar(new FalObservacion(null, EntidadTipoObservada.DOCUMENTO, SIN_PADRE, null,
                    MARCA + "contexto de B", OrigenObservacion.USUARIO, otraActa, T0, actor()));

            List<FalActa> listadas = soloDelSlice(repositorio.listarTodas());

            assertThat(observacionDe(listadas, unaActa)).isEqualTo(MARCA + "solo de A");
            assertThat(observacionDe(listadas, otraActa)).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("41. listarTodas es semanticamente equivalente a las dos lecturas por clave")
    void listarTodasEquivaleALasLecturasPorClave() {
        enEscenario(padres -> {
            String texto = MARCA + "equivalencia de lecturas";
            FalActa candidata = actaCompleta(padres, texto);
            Long id = repositorio.guardar(candidata).getId();
            observaciones.guardar(observacionDeActa(id, texto, T0));

            FalActa porId = repositorio.buscarPorId(id).orElseThrow();
            FalActa porUuid = repositorio.buscarPorUuidTecnico(candidata.getUuidTecnico()).orElseThrow();
            FalActa deLista = soloDelSlice(repositorio.listarTodas()).stream()
                    .filter(a -> id.equals(a.getId()))
                    .findFirst()
                    .orElseThrow();

            assertThat(huella(deLista)).isEqualTo(huella(porId)).isEqualTo(huella(porUuid));
            assertThat(deLista.getObservaciones()).isEqualTo(texto);
            return null;
        });
    }

    // =====================================================================
    // COMPOSICION BATCH DE LISTARTODAS (DECISION-OBS-BATCH-01)
    // =====================================================================

    /**
     * Invariante de cardinalidad, valido cualquiera sea el contenido del sandbox: sin
     * actas no se pregunta por observaciones, y con actas se pregunta exactamente una
     * vez. Nunca se usa la lectura individual.
     */
    @Test
    @DisplayName("65. listarTodas invoca el batch una sola vez con actas y ninguna sin actas")
    void listarTodasInvocaElBatchUnaSolaVez() {
        enEscenario(padres -> {
            ObservacionRepositoryQueCuenta contador = new ObservacionRepositoryQueCuenta(observaciones);
            JdbcActaRepository conContador = new JdbcActaRepository(jdbc, contador);

            List<FalActa> todas = conContador.listarTodas();

            assertThat(contador.invocacionesBatch())
                    .as("con %s actas leidas el batch debe invocarse %s vez",
                            todas.size(), todas.isEmpty() ? "ninguna" : "una")
                    .isEqualTo(todas.isEmpty() ? 0 : 1);
            assertThat(contador.invocacionesIndividuales())
                    .as("listarTodas no pide el historial acta por acta")
                    .isZero();
            if (todas.isEmpty()) {
                assertThat(todas).isEmpty();
            }
            return null;
        });
    }

    /**
     * El caso que la deuda pedía cerrar: con varias actas el adaptador hacía una consulta
     * de observaciones por acta. Ahora hace una sola, y esa invocación recibe los ids de
     * todas las actas leídas.
     */
    @Test
    @DisplayName("66. Con varias actas hay una unica invocacion batch que recibe los ids de todas")
    void listarTodasNoEscalaConLaCantidadDeActas() {
        enEscenario(padres -> {
            Long primera = repositorio.guardar(actaMinima(padres, "batch 1")).getId();
            Long segunda = repositorio.guardar(actaMinima(padres, "batch 2")).getId();
            Long tercera = repositorio.guardar(actaMinima(padres, "batch 3")).getId();
            Long cuarta = repositorio.guardar(actaMinima(padres, "batch 4")).getId();
            Long quinta = repositorio.guardar(actaMinima(padres, "batch 5")).getId();

            ObservacionRepositoryQueCuenta contador = new ObservacionRepositoryQueCuenta(observaciones);
            List<FalActa> todas = new JdbcActaRepository(jdbc, contador).listarTodas();

            assertThat(contador.invocacionesBatch())
                    .as("cinco actas nuevas y una sola invocacion batch")
                    .isOne();
            assertThat(contador.invocacionesIndividuales()).isZero();
            assertThat(contador.tiposPedidos()).containsExactly(EntidadTipoObservada.ACTA);
            assertThat(contador.idsPedidos())
                    .as("el batch recibe los ids de todas las actas leidas")
                    .contains(primera, segunda, tercera, cuarta, quinta)
                    .hasSize(todas.size());
            return null;
        });
    }

    /**
     * La composición batch tiene que dar exactamente lo mismo que daba la composición
     * fila por fila: observación inicial correcta, inicial inactiva que sigue siendo
     * inicial, nulo sin historial y orden de actas intacto.
     */
    @Test
    @DisplayName("67. La composicion batch conserva la inicial, el nulo sin historial y el orden")
    void composicionBatchEquivalenteALaIndividual() {
        enEscenario(padres -> {
            Long sinObs = repositorio.guardar(actaMinima(padres, "batch sin obs")).getId();
            Long conUna = repositorio.guardar(actaMinima(padres, "batch con una")).getId();
            Long conVarias = repositorio.guardar(actaMinima(padres, "batch con varias")).getId();
            Long conInicialDeBaja = repositorio.guardar(actaMinima(padres, "batch inicial de baja")).getId();

            observaciones.guardar(observacionDeActa(conUna, MARCA + "unica del batch", T0));
            observaciones.guardar(observacionDeActa(conVarias, MARCA + "tardia del batch", T0.plusHours(3)));
            observaciones.guardar(observacionDeActa(conVarias, MARCA + "inicial del batch", T0));
            FalObservacion inicial = observaciones.guardar(
                    observacionDeActa(conInicialDeBaja, MARCA + "inicial luego de baja", T0));
            observaciones.guardar(observacionDeActa(conInicialDeBaja, MARCA + "correccion del batch", T0.plusDays(1)));
            observaciones.desactivar(inicial.getId(), T0.plusDays(1), actor());

            List<FalActa> listadas = soloDelSlice(repositorio.listarTodas());

            assertThat(observacionDe(listadas, sinObs))
                    .as("un acta sin historial vuelve con observaciones nula")
                    .isNull();
            assertThat(observacionDe(listadas, conUna)).isEqualTo(MARCA + "unica del batch");
            assertThat(observacionDe(listadas, conVarias)).isEqualTo(MARCA + "inicial del batch");
            assertThat(observacionDe(listadas, conInicialDeBaja))
                    .as("la inicial dada de baja sigue siendo la inicial, no la primera activa")
                    .isEqualTo(MARCA + "inicial luego de baja");
            assertThat(listadas)
                    .extracting(FalActa::getId)
                    .as("el orden de SQL_TODAS se preserva")
                    .containsExactly(sinObs, conUna, conVarias, conInicialDeBaja)
                    .isSorted();
            return null;
        });
    }

    @Test
    @DisplayName("68. buscarPorId y buscarPorUuidTecnico siguen usando la lectura individual")
    void lecturasPorClaveConservanLaComposicionIndividual() {
        enEscenario(padres -> {
            FalActa candidata = actaMinima(padres, "clave individual");
            Long id = repositorio.guardar(candidata).getId();
            observaciones.guardar(observacionDeActa(id, MARCA + "inicial por clave", T0));

            ObservacionRepositoryQueCuenta contador = new ObservacionRepositoryQueCuenta(observaciones);
            JdbcActaRepository conContador = new JdbcActaRepository(jdbc, contador);

            FalActa porId = conContador.buscarPorId(id).orElseThrow();
            FalActa porUuid = conContador.buscarPorUuidTecnico(candidata.getUuidTecnico()).orElseThrow();

            assertThat(contador.invocacionesIndividuales())
                    .as("una lectura individual por cada lectura por clave")
                    .isEqualTo(2);
            assertThat(contador.invocacionesBatch())
                    .as("una sola acta no se resuelve con un mapa intermedio")
                    .isZero();
            assertThat(porId.getObservaciones()).isEqualTo(MARCA + "inicial por clave");
            assertThat(huella(porUuid)).isEqualTo(huella(porId));
            return null;
        });
    }

    // =====================================================================
    // UNIQUE, FK, CHECK Y NOT NULL
    // =====================================================================

    /**
     * El puerto no promete una excepcion funcional para estos UNIQUE y no existe una
     * canonica en el dominio, de modo que el adaptador preserva la excepcion de Spring
     * que el motor respalda en lugar de inventar contrato. El nombre de la constraint
     * viaja en el mensaje y permite discriminar el caso.
     */
    @Test
    @DisplayName("42. id_tecnico duplicado viola uq_fal_acta_id_tecnico sin traducirse a dominio")
    void uuidDuplicado() {
        enEscenario(padres -> {
            FalActa primera = actaMinima(padres, "uuid unico");
            repositorio.guardar(primera);

            FalActa duplicada = actaConUuid(padres, primera.getUuidTecnico());

            assertThat(catchThrowable(() -> repositorio.guardar(duplicada)))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("uq_fal_acta_id_tecnico");
            return null;
        });
    }

    @Test
    @DisplayName("43. codigo_qr duplicado viola uq_fal_acta_codigo_qr y se distingue del anterior")
    void qrDuplicado() {
        enEscenario(padres -> {
            FalActa primera = actaMinima(padres, "qr unico");
            repositorio.guardar(primera);

            FalActa duplicada = actaConQrYVersion(padres, primera.getCodigoQr(), 0);

            assertThat(catchThrowable(() -> repositorio.guardar(duplicada)))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("uq_fal_acta_codigo_qr");
            return null;
        });
    }

    @Test
    @DisplayName("44. Los dos UNIQUE traducibles son los unicos no primarios de la tabla")
    void losDosUniqueSonLosUnicos() {
        List<String> uniques = jdbc.sql("""
                        SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fal_acta'
                          AND CONSTRAINT_TYPE = 'UNIQUE'
                        """)
                .query(String.class)
                .list();

        assertThat(uniques).containsExactlyInAnyOrder(
                "uq_fal_acta_id_tecnico", "uq_fal_acta_codigo_qr");
    }

    /**
     * `DECISION-ACTA-UNIQUE-01` acota el alcance de la unicidad: la numeracion del acta y
     * del talonario no gana UNIQUE en este alcance. Este caso lo verifica contra el motor
     * para que agregarlo sin decision normativa rompa el build.
     */
    @Test
    @DisplayName("44b. Ni nro_acta ni las combinaciones de numeracion tienen UNIQUE")
    void laNumeracionNoTieneUnique() {
        List<String> columnasUnicas = jdbc.sql("""
                        SELECT DISTINCT COLUMN_NAME FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fal_acta'
                          AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'
                        """)
                .query(String.class)
                .list();

        assertThat(columnasUnicas)
                .as("solo id_tecnico y codigo_qr participan de un UNIQUE no primario")
                .containsExactlyInAnyOrder("id_tecnico", "codigo_qr")
                .doesNotContain("nro_acta", "id_talonario", "nro_talonario_usado");
    }

    @Test
    @DisplayName("45. Una FK violada no se enmascara como duplicado ni como conflicto OCC")
    void fkNoSeEnmascara() {
        enEscenario(padres -> {
            FalActa sinPersona = actaMinima(padres, "fk persona");
            sinPersona.setIdPersonaInfractor(SIN_PADRE);

            assertThat(catchThrowable(() -> repositorio.guardar(sinPersona)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(DuplicateKeyException.class)
                    .isNotInstanceOf(ConcurrenciaConflictoException.class)
                    .isNotInstanceOf(ActaNoEncontradaException.class)
                    .hasMessageContaining("fk_acta_persona_infractor");
            return null;
        });
    }

    @Test
    @DisplayName("46. chk_acta_qr_ver fue retirado (DECISION_DDL-SIN-CHECKS-CODIGOS-01): "
            + "qr_payload_version negativa ya no es rechazada por el motor")
    void checkNoSeEnmascara() {
        enEscenario(padres -> {
            // chk_acta_qr_ver fue eliminado del DDL por DECISION_DDL-SIN-CHECKS-CODIGOS-01
            // (ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853): es un CHECK tecnico de rango
            // sobre un contador de version de formato que, en la practica, el dominio nunca
            // produce negativo (siempre 0 hoy). Sin CHECK fisico, un valor negativo insertado
            // directamente ya no es rechazado por el motor.
            FalActa conQrNegativo = actaConQrYVersion(
                    padres, MARCA + "check qr-" + UUID.randomUUID(), -1);

            assertThat(catchThrowable(() -> repositorio.guardar(conQrNegativo)))
                    .as("sin chk_acta_qr_ver, el guardado con version negativa ya no lanza excepcion")
                    .isNull();
            return null;
        });
    }

    @Test
    @DisplayName("47. Un NOT NULL violado no se enmascara: codigo_qr nulo")
    void notNullNoSeEnmascara() {
        enEscenario(padres -> {
            FalActa sinQr = actaSinQr(padres);

            assertThat(catchThrowable(() -> repositorio.guardar(sinQr)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("codigo_qr");
            return null;
        });
    }

    // =====================================================================
    // PARIDAD CON EL ADAPTADOR INMEMORY
    // =====================================================================

    /**
     * El oraculo es una instancia InMemory real, no literales duplicados en el test. No
     * se comparan PK exactas: se compara el comportamiento observable.
     */
    @Test
    @DisplayName("48. Paridad de alta, lecturas, update y no mutacion contra InMemory")
    void paridadDelCicloCompleto() {
        enEscenario(padres -> {
            ActaRepository enMemoria = new InMemoryActaRepository();
            String texto = MARCA + "paridad de observaciones";

            // La segunda candidata es una copia de la primera, no otra acta generada:
            // id_tecnico, codigo_qr y los subjects son aleatorios por invocacion y la
            // paridad se mide sobre los mismos valores de entrada.
            FalActa candidataJdbc = actaCompleta(padres, texto);
            FalActa candidataMemoria = candidataJdbc.copia();

            FalActa altaJdbc = repositorio.guardar(candidataJdbc);
            FalActa altaMemoria = enMemoria.guardar(candidataMemoria);

            assertThat(altaJdbc.getId()).isNotNull().isPositive();
            assertThat(altaMemoria.getId()).isNotNull().isPositive();
            assertThat(altaJdbc.getVersionRow()).isEqualTo(altaMemoria.getVersionRow()).isZero();
            assertThat(candidataJdbc.getId()).isNull();
            assertThat(candidataMemoria.getId()).isNull();

            assertThat(altaJdbc.getObservaciones())
                    .as("guardar conserva el campo observaciones en ambos adaptadores")
                    .isEqualTo(altaMemoria.getObservaciones())
                    .isEqualTo(texto);

            // La equivalencia de lectura exige la fila en fal_observacion: es la unica
            // fuente de verdad del campo en el objetivo MariaDB.
            observaciones.guardar(observacionDeActa(altaJdbc.getId(), texto, T0));

            assertThat(huella(repositorio.buscarPorId(altaJdbc.getId()).orElseThrow()))
                    .isEqualTo(huella(enMemoria.buscarPorId(altaMemoria.getId()).orElseThrow()));
            assertThat(huella(repositorio.buscarPorUuidTecnico(candidataJdbc.getUuidTecnico()).orElseThrow()))
                    .isEqualTo(huella(enMemoria.buscarPorUuidTecnico(candidataMemoria.getUuidTecnico())
                            .orElseThrow()));

            FalActa actualizadaJdbc = repositorio.guardar(altaJdbc);
            FalActa actualizadaMemoria = enMemoria.guardar(altaMemoria);
            assertThat(actualizadaJdbc.getVersionRow())
                    .isEqualTo(actualizadaMemoria.getVersionRow())
                    .isEqualTo(1);
            assertThat(altaJdbc.getVersionRow()).isEqualTo(altaMemoria.getVersionRow()).isZero();
            return null;
        });
    }

    @Test
    @DisplayName("49. Paridad de excepciones: mismo tipo y mensaje en 404 y en 409")
    void paridadDeExcepciones() {
        enEscenario(padres -> {
            ActaRepository enMemoria = new InMemoryActaRepository();

            FalActa fantasmaJdbc = actaMinima(padres, "paridad 404").conId(SIN_PADRE);
            FalActa fantasmaMemoria = actaMinima(padres, "paridad 404 mem").conId(SIN_PADRE);

            Throwable jdbc404 = catchThrowable(() -> repositorio.guardar(fantasmaJdbc));
            Throwable memoria404 = catchThrowable(() -> enMemoria.guardar(fantasmaMemoria));
            assertThat(jdbc404).isInstanceOf(ActaNoEncontradaException.class);
            assertThat(memoria404).isInstanceOf(ActaNoEncontradaException.class);
            assertThat(jdbc404.getMessage()).isEqualTo(memoria404.getMessage());

            FalActa altaJdbc = repositorio.guardar(actaMinima(padres, "paridad 409"));
            FalActa altaMemoria = enMemoria.guardar(actaMinima(padres, "paridad 409 mem"));
            repositorio.guardar(altaJdbc);
            enMemoria.guardar(altaMemoria);

            Throwable jdbc409 = catchThrowable(() -> repositorio.guardar(altaJdbc.copia()));
            Throwable memoria409 = catchThrowable(() -> enMemoria.guardar(altaMemoria.copia()));
            assertThat(jdbc409).isInstanceOf(ConcurrenciaConflictoException.class);
            assertThat(memoria409).isInstanceOf(ConcurrenciaConflictoException.class);
            assertThat(jdbc409.getMessage())
                    .as("mismo texto salvo la PK, que no se compara entre adaptadores")
                    .isEqualTo(memoria409.getMessage()
                            .replace("id=" + altaMemoria.getId(), "id=" + altaJdbc.getId()));
            return null;
        });
    }

    // =====================================================================
    // SITUACION DE IDENTIFICACION DEL INFRACTOR (DECISION_DDL-ACTA-INFR-01)
    // =====================================================================

    @Test
    @DisplayName("53. fal_acta tiene exactamente 57 columnas y la columna nueva no tiene default")
    void tabla57ColumnasSinDefault() {
        assertThat(jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                        """).query(Integer.class).single())
                .as("fal_acta debe tener 57 columnas tras DECISION_DDL-ACTA-INFR-01")
                .isEqualTo(57);

        assertThat(jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                           AND column_name = 'situacion_identificacion_infractor'
                           AND column_type = 'smallint(6)'
                           AND is_nullable = 'NO'
                           AND column_default IS NULL
                        """).query(Integer.class).single())
                .as("la situacion es SMALLINT NOT NULL y no admite valor por defecto")
                .isEqualTo(1);

        assertThat(jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                           AND column_name = 'id_persona_infractor' AND is_nullable = 'YES'
                        """).query(Integer.class).single())
                .as("id_persona_infractor pasa a admitir NULL")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("54. Round-trip de situacion 1 y 2: ambas conservan la persona informada")
    void roundTripIdentificacionConPersona() {
        enEscenario(padres -> {
            FalActa completa = repositorio.guardar(
                    actaConSituacion(padres, "sit1",
                            SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, padres.idPersona()));
            FalActa parcial = repositorio.guardar(
                    actaConSituacion(padres, "sit2",
                            SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL, padres.idPersonaParcial()));

            FalActa leidaCompleta = repositorio.buscarPorId(completa.getId()).orElseThrow();
            FalActa leidaParcial = repositorio.buscarPorId(parcial.getId()).orElseThrow();

            assertThat(leidaCompleta.getSituacionIdentificacionInfractor())
                    .isEqualTo(SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO);
            assertThat(leidaCompleta.getIdPersonaInfractor()).isEqualTo(padres.idPersona());
            assertThat(leidaParcial.getSituacionIdentificacionInfractor())
                    .isEqualTo(SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL);
            assertThat(leidaParcial.getIdPersonaInfractor()).isEqualTo(padres.idPersonaParcial());
            return null;
        });
    }

    @Test
    @DisplayName("55. Round-trip de un acta sin persona: la columna queda NULL, no en cero ni centinela")
    void roundTripSinPersona() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(
                    actaConSituacion(padres, "sin persona",
                            SituacionIdentificacionInfractor.AUSENTE, null));

            FalActa leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();

            assertThat(leida.getSituacionIdentificacionInfractor())
                    .isEqualTo(SituacionIdentificacionInfractor.AUSENTE);
            assertThat(leida.getIdPersonaInfractor()).isNull();
            assertThat(cruda(persistida.getId(), "id_persona_infractor"))
                    .as("la ausencia de sujeto es NULL fisico, no 0 ni una PK reservada")
                    .isNull();
            return null;
        });
    }

    @Test
    @DisplayName("56. Los seis codigos hacen round-trip por codigo fisico, nunca por ordinal")
    void losSeisCodigosMapean() {
        enEscenario(padres -> {
            for (SituacionIdentificacionInfractor situacion : SituacionIdentificacionInfractor.values()) {
                Long persona = situacion.exigePersonaInfractora() ? padres.idPersona() : null;
                FalActa persistida = repositorio.guardar(
                        actaConSituacion(padres, "codigo " + situacion.codigo(), situacion, persona));

                assertThat(cruda(persistida.getId(), "situacion_identificacion_infractor"))
                        .as("%s se persiste por codigo(), no por ordinal()", situacion.name())
                        .isEqualTo(Short.toString(situacion.codigo()));
                assertThat(repositorio.buscarPorId(persistida.getId()).orElseThrow()
                        .getSituacionIdentificacionInfractor())
                        .isEqualTo(situacion);
            }
            return null;
        });
    }

    @Test
    @DisplayName("57. Update OCC conserva la combinacion coherente y puede cambiar la persona dentro de ella")
    void updateOccConservaLaCombinacion() {
        enEscenario(padres -> {
            FalActa persistida = repositorio.guardar(
                    actaConSituacion(padres, "occ situacion",
                            SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, padres.idPersona()));

            persistida.setIdPersonaInfractor(padres.idPersonaParcial());
            FalActa actualizada = repositorio.guardar(persistida);

            assertThat(actualizada.getVersionRow()).isEqualTo(1);
            FalActa leida = repositorio.buscarPorId(actualizada.getId()).orElseThrow();
            assertThat(leida.getSituacionIdentificacionInfractor())
                    .as("la situacion es final: el update no la cambia")
                    .isEqualTo(SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO);
            assertThat(leida.getIdPersonaInfractor()).isEqualTo(padres.idPersonaParcial());
            return null;
        });
    }

    /**
     * La combinacion imposible se rechaza antes de llegar al motor: el invariante de
     * FalActa la corta en el constructor y en el setter. El CHECK fisico es la segunda
     * linea, verificada con SQL directo en el caso 59.
     */
    @Test
    @DisplayName("58. Las combinaciones invalidas se rechazan en el agregado antes del INSERT")
    void combinacionesInvalidasSeRechazanEnElAgregado() {
        enEscenario(padres -> {
            assertThat(catchThrowable(() -> actaConSituacion(padres, "1 sin persona",
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exige idPersonaInfractor");

            assertThat(catchThrowable(() -> actaConSituacion(padres, "5 con persona",
                    SituacionIdentificacionInfractor.AUSENTE, padres.idPersona())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no admite idPersonaInfractor");

            FalActa valida = actaConSituacion(padres, "setter incoherente",
                    SituacionIdentificacionInfractor.PRESENTE_SE_NIEGA, null);
            assertThat(catchThrowable(() -> valida.setIdPersonaInfractor(padres.idPersona())))
                    .as("el setter no puede dejar una combinacion imposible ni transitoriamente")
                    .isInstanceOf(IllegalArgumentException.class);
            return null;
        });
    }

    @Test
    @DisplayName("59. chk_acta_infractor_coherente y chk_acta_situacion_infractor fueron retirados "
            + "(DECISION_DDL-SIN-CHECKS-CODIGOS-01); NOT NULL sigue vigente")
    void checksFisicosRechazanLaMatrizYLosCodigos() {
        enEscenario(padres -> {
            // chk_acta_infractor_coherente y chk_acta_situacion_infractor fueron eliminados del
            // DDL por DECISION_DDL-SIN-CHECKS-CODIGOS-01 (ACTA-ALTA-INTEGRAL-JDBC-MASTER-
            // 20260804-224853): son CHECK de coherencia funcional y de codigo/enum, no invariantes
            // tecnicos. La coherencia situacion/persona y el rango de codigo ya se validan en
            // FalActa (ver test 58, combinacionesInvalidasSeRechazanEnElAgregado) y en
            // SituacionIdentificacionInfractor; un INSERT crudo que bypasea el dominio ya no es
            // rechazado por el motor.
            assertThat(catchThrowable(() -> insertarCrudo(padres, 1, null)))
                    .as("situacion 1 con persona NULL: sin CHECK fisico, el motor ya no rechaza esto")
                    .isNull();
            assertThat(catchThrowable(() -> insertarCrudo(padres, 3, padres.idPersona())))
                    .as("situacion 3 con persona: sin CHECK fisico, el motor ya no rechaza esto")
                    .isNull();
            // NOT NULL de la columna sigue vigente: no fue retirado por la decision.
            assertThat(catchThrowable(() -> insertarCrudo(padres, null, null)))
                    .as("situacion NULL sigue violando el NOT NULL de la columna")
                    .isNotNull();
            return null;
        });
    }

    @Test
    @DisplayName("60. La FK sigue restringiendo una persona inexistente pese a la nulabilidad")
    void fkSigueRestringiendoConColumnaNullable() {
        enEscenario(padres -> {
            assertThat(catchThrowable(() -> insertarCrudo(padres, 1, SIN_PADRE)))
                    .as("id_persona_infractor nullable no debilita fk_acta_persona_infractor")
                    .isNotNull();
            return null;
        });
    }

    @Test
    @DisplayName("61. listarTodas y buscarPorUuidTecnico preservan la situacion de cada acta")
    void lecturasPreservanLaSituacion() {
        enEscenario(padres -> {
            FalActa conPersona = repositorio.guardar(actaConSituacion(padres, "listar 1",
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, padres.idPersona()));
            FalActa sinPersona = repositorio.guardar(actaConSituacion(padres, "listar 6",
                    SituacionIdentificacionInfractor.DATOS_INSUFICIENTES, null));

            assertThat(repositorio.listarTodas())
                    .filteredOn(a -> a.getId().equals(conPersona.getId())
                            || a.getId().equals(sinPersona.getId()))
                    .extracting(FalActa::getSituacionIdentificacionInfractor)
                    .containsExactly(SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                            SituacionIdentificacionInfractor.DATOS_INSUFICIENTES);

            assertThat(repositorio.buscarPorUuidTecnico(sinPersona.getUuidTecnico()).orElseThrow()
                    .getSituacionIdentificacionInfractor())
                    .isEqualTo(SituacionIdentificacionInfractor.DATOS_INSUFICIENTES);
            return null;
        });
    }

    @Test
    @DisplayName("62. Paridad InMemory/JDBC de la situacion y de la persona nula")
    void paridadInMemoryDeLaSituacion() {
        enEscenario(padres -> {
            FalActa candidata = actaConSituacion(padres, "paridad situacion",
                    SituacionIdentificacionInfractor.PRESENTE_NO_APORTA_DATOS, null);

            FalActa deJdbc = repositorio.guardar(candidata);
            FalActa deMemoria = new InMemoryActaRepository().guardar(
                    actaConSituacion(padres, "paridad memoria",
                            SituacionIdentificacionInfractor.PRESENTE_NO_APORTA_DATOS, null));

            assertThat(deJdbc.getSituacionIdentificacionInfractor())
                    .isEqualTo(deMemoria.getSituacionIdentificacionInfractor());
            assertThat(deJdbc.getIdPersonaInfractor()).isNull();
            assertThat(deMemoria.getIdPersonaInfractor()).isNull();

            assertThat(repositorio.buscarPorId(deJdbc.getId()).orElseThrow()
                    .getSituacionIdentificacionInfractor())
                    .as("el oraculo InMemory y el adaptador JDBC devuelven la misma situacion")
                    .isEqualTo(deMemoria.getSituacionIdentificacionInfractor());
            return null;
        });
    }

    /**
     * Backfill con datos sinteticos dentro de la transaccion del escenario: se
     * reproduce el estado previo (situacion desconocida) y se aplica la misma
     * clasificacion por JOIN que el forward, verificando que no queda fila sin
     * clasificar y que fal_persona no se modifica.
     */
    @Test
    @DisplayName("63. El backfill clasifica por documento de la persona y no deja filas sin clasificar")
    void backfillDeterministicoConDatosSinteticos() {
        enEscenario(padres -> {
            Long completa = repositorio.guardar(actaConSituacion(padres, "backfill completo",
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, padres.idPersona())).getId();
            Long parcial = repositorio.guardar(actaConSituacion(padres, "backfill parcial",
                    SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL, padres.idPersonaParcial())).getId();

            String docAntes = documentoDePersona(padres.idPersona());

            // Se simula el estado previo al forward y se reaplica la clasificacion.
            jdbc.sql("""
                    UPDATE fal_acta a
                      JOIN fal_persona p ON p.id = a.id_persona_infractor
                       SET a.situacion_identificacion_infractor =
                           CASE WHEN p.tipo_documento IS NOT NULL AND p.nro_doc IS NOT NULL THEN 1
                                WHEN p.tipo_documento IS NULL AND p.nro_doc IS NULL THEN 2
                           END
                     WHERE a.id IN (:completa, :parcial)
                    """).param("completa", completa).param("parcial", parcial).update();

            assertThat(cruda(completa, "situacion_identificacion_infractor"))
                    .as("persona con documento clasifica en 1")
                    .isEqualTo("1");
            assertThat(cruda(parcial, "situacion_identificacion_infractor"))
                    .as("persona sin documento clasifica en 2")
                    .isEqualTo("2");
            assertThat(jdbc.sql("""
                            SELECT COUNT(*) FROM fal_acta
                             WHERE situacion_identificacion_infractor IS NULL
                            """).query(Integer.class).single())
                    .as("no queda ninguna fila sin clasificar")
                    .isZero();
            assertThat(documentoDePersona(padres.idPersona()))
                    .as("el backfill no modifica fal_persona")
                    .isEqualTo(docAntes);
            return null;
        });
    }

    // =====================================================================
    // QR CANONICO HMAC (WAVE 1A2-B1.5B2)
    // =====================================================================

    /**
     * Los casos 30 y 32 ya cubren el round-trip exacto de {@code codigo_qr},
     * {@code qr_payload_version}, {@code id_user_alta} y {@code lat_infr}/{@code lon_infr}
     * con valores sinteticos. Lo que falta y este caso agrega es el token realmente
     * canonico de {@code DECISION_DDL-ACTA-QR-01}: 86 caracteres con tres separadores,
     * emitido por el protector HMAC y no por una cadena de prueba. Verifica que la columna
     * lo conserva byte a byte, que sigue verificando despues de volver del motor y que la
     * version persistida es la que quedo firmada dentro del token.
     *
     * <p>No activa ningun bean: el protector se construye a mano con una clave TEST_ONLY,
     * igual que el candidato JDBC.
     */
    @Test
    @DisplayName("64. Round-trip del token QR canonico HMAC con actor y coordenadas reales")
    void roundTripDelTokenQrCanonico() {
        enEscenario(padres -> {
            QrTokenProtector protector = QrTestSupport.protector();
            String uuidTecnico = UUID.randomUUID().toString();
            QrTokenEmitido emitido = protector.emitir(uuidTecnico);
            String actor = actor();

            FalActa acta = new FalActa(
                    null, uuidTecnico, TipoActa.TRANSITO, padres.idDep(), null, F0, T0,
                    MARCA + "domicilio del hecho", null,
                    LAT_EXACTA, LON_EXACTA,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    padres.idPersona(), T0, actor);
            acta.setVerDep((int) padres.verDep());
            acta.setQrPayloadVersion(emitido.version());
            acta.setCodigoQr(emitido.token());

            FalActa leida = repositorio.buscarPorId(repositorio.guardar(acta).getId()).orElseThrow();

            assertThat(leida.getCodigoQr())
                    .as("VARCHAR(128) conserva el token canonico byte a byte, puntos incluidos")
                    .isEqualTo(emitido.token())
                    .hasSize(86);
            assertThat(leida.getQrPayloadVersion())
                    .as("qr_payload_version es la version firmada dentro del token")
                    .isEqualTo(emitido.version());
            assertThat(leida.getCodigoQr().split("\\.")[2])
                    .isEqualTo(String.valueOf(leida.getQrPayloadVersion()));
            assertThat(protector.resolverUuidTecnico(leida.getCodigoQr()))
                    .as("el token sigue verificando despues de volver del motor")
                    .isEqualTo(leida.getUuidTecnico());
            assertThat(leida.getIdUserAlta())
                    .as("id_user_alta CHAR(36) conserva el subject del actor")
                    .isEqualTo(actor);
            assertThat(leida.getLatInfr()).isEqualTo(LAT_EXACTA);
            assertThat(leida.getLonInfr()).isEqualTo(LON_EXACTA);
            assertThat(leida.getLatInfr())
                    .as("latitud y longitud no se intercambian en el mapeo")
                    .isNotEqualTo(leida.getLonInfr());
            return null;
        });
    }

    // =====================================================================
    // TRANSACCION Y RESIDUOS
    // =====================================================================

    @Test
    @DisplayName("50. Rollback real: el acta es visible dentro de la transaccion y desaparece al revertir")
    void rollbackRealDelActa() {
        long[] id = new long[1];

        new TransactionTemplate(txManager).execute(status -> {
            Padres padres = insertarPadresMinimos();
            id[0] = repositorio.guardar(actaMinima(padres, "se revierte")).getId();

            assertThat(repositorio.buscarPorId(id[0])).isPresent();
            status.setRollbackOnly();
            return null;
        });

        assertThat(contarPorId("fal_acta", id[0]))
                .as("el acta %s no debe sobrevivir al rollback", id[0])
                .isZero();
    }

    @Test
    @DisplayName("51. Rollback de la lectura compuesta: acta y observacion desaparecen juntas")
    void rollbackRealDeLaLecturaCompuesta() {
        long[] ids = new long[2];

        new TransactionTemplate(txManager).execute(status -> {
            Padres padres = insertarPadresMinimos();
            Long actaId = repositorio.guardar(actaMinima(padres, "compuesta revertida")).getId();
            String texto = MARCA + "observacion revertida";
            Long obsId = observaciones.guardar(observacionDeActa(actaId, texto, T0)).getId();
            ids[0] = actaId;
            ids[1] = obsId;

            assertThat(repositorio.buscarPorId(actaId).orElseThrow().getObservaciones()).isEqualTo(texto);
            status.setRollbackOnly();
            return null;
        });

        assertThat(contarPorId("fal_acta", ids[0])).isZero();
        assertThat(contarPorId("fal_observacion", ids[1])).isZero();
    }

    @Test
    @DisplayName("52. La suite no deja residuos con la marca del slice")
    void sinResiduosDeLaMarca() {
        assertThat(contarConMarca("fal_acta", "codigo_qr")).isZero();
        assertThat(contarConMarca("fal_observacion", "observacion")).isZero();
        assertThat(contarConMarca("fal_dependencia", "nom_dep")).isZero();
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /**
     * Decorador que cuenta invocaciones del puerto de observaciones. No es un mock ni un
     * doble: delega todo al adaptador real, de modo que los escenarios siguen leyendo la
     * base y las aserciones de composicion siguen siendo verdaderas.
     *
     * <p>Se prefiere sobre una dependencia de mocking o un proxy de DataSource porque el
     * proyecto no usa ninguna de las dos y agregar una por este alcance seria
     * desproporcionado.
     */
    private static final class ObservacionRepositoryQueCuenta implements ObservacionRepository {

        private final ObservacionRepository delegado;
        private int invocacionesIndividuales;
        private int invocacionesBatch;
        private final List<EntidadTipoObservada> tiposPedidos = new java.util.ArrayList<>();
        private final List<Long> idsPedidos = new java.util.ArrayList<>();

        private ObservacionRepositoryQueCuenta(ObservacionRepository delegado) {
            this.delegado = delegado;
        }

        @Override
        public FalObservacion guardar(FalObservacion obs) {
            return delegado.guardar(obs);
        }

        @Override
        public Optional<FalObservacion> buscarPorId(Long id) {
            return delegado.buscarPorId(id);
        }

        @Override
        public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
            invocacionesIndividuales++;
            return delegado.listarPorEntidad(tipo, entidadId);
        }

        @Override
        public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
            invocacionesIndividuales++;
            return delegado.listarActivasPorEntidad(tipo, entidadId);
        }

        @Override
        public java.util.Map<Long, List<FalObservacion>> listarPorEntidades(
                EntidadTipoObservada tipo, java.util.Collection<Long> entidadIds) {
            invocacionesBatch++;
            tiposPedidos.add(tipo);
            idsPedidos.addAll(entidadIds);
            return delegado.listarPorEntidades(tipo, entidadIds);
        }

        @Override
        public FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja) {
            return delegado.desactivar(id, fhBaja, idUserBaja);
        }

        int invocacionesIndividuales() { return invocacionesIndividuales; }

        int invocacionesBatch() { return invocacionesBatch; }

        List<EntidadTipoObservada> tiposPedidos() { return List.copyOf(tiposPedidos); }

        List<Long> idsPedidos() { return List.copyOf(idsPedidos); }
    }

    /**
     * Cadena minima de padres que fal_acta exige por FK.
     *
     * <p>Se siembran dos personas porque la matriz de
     * {@code DECISION_DDL-ACTA-INFR-01} distingue dos identificaciones reales:
     * {@code idPersona} tiene documento completo y corresponde a
     * IDENTIFICADO_COMPLETO; {@code idPersonaParcial} no tiene documento, que es el
     * estado que {@code DECISION_DDL-PERS-01} hizo representable y que corresponde a
     * IDENTIFICADO_PARCIAL.
     */
    private record Padres(Long idDep, short verDep, Long idPersona, Long idPersonaParcial) { }

    /** Escenario dentro de una transaccion que se revierte siempre. */
    private <T> T enEscenario(Function<Padres, T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.apply(insertarPadresMinimos());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /**
     * Acta con los campos obligatorios del DDL y nada mas. codigo_qr e id_user_alta se
     * proveen de forma explicita: el adaptador no los genera ni los inventa, y las
     * reglas productivas que los originan siguen abiertas.
     */
    private static FalActa actaMinima(Padres padres, String sufijo) {
        return construir(padres, UUID.randomUUID().toString(), sufijo, null);
    }

    /** Acta con el mismo id_tecnico, para ejercer el UNIQUE y la paridad de lecturas. */
    private static FalActa actaConUuid(Padres padres, String uuidTecnico) {
        return construir(padres, uuidTecnico, "uuid impuesto", null);
    }

    private static FalActa construir(Padres padres, String uuidTecnico, String sufijo,
                                     String observacionInicial) {
        FalActa acta = construirSinQr(padres, uuidTecnico, observacionInicial);
        acta.setCodigoQr(MARCA + sufijo + "-" + UUID.randomUUID());
        return acta;
    }

    /**
     * Acta con los campos obligatorios pero <b>sin QR asignado</b>.
     *
     * <p>Existe porque {@code codigo_qr} y {@code qr_payload_version} son inmutables una vez
     * emitido el token ({@code DECISION-QR-INMUTABLE-01}): los escenarios que necesitan un
     * valor fisico invalido tienen que informarlo en la primera asignacion y no mutando un QR
     * ya emitido.
     */
    private static FalActa construirSinQr(Padres padres, String uuidTecnico,
                                          String observacionInicial) {
        FalActa acta = new FalActa(
                null,
                uuidTecnico,
                TipoActa.TRANSITO,
                padres.idDep(),
                null,
                F0,
                T0,
                null,
                observacionInicial,
                null,
                null,
                ResultadoFirmaInfractor.FIRMADA,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                padres.idPersona(),
                T0,
                actor());
        acta.setVerDep((int) padres.verDep());
        return acta;
    }

    /** Acta sin QR, para ejercer el NOT NULL fisico de {@code codigo_qr}. */
    private static FalActa actaSinQr(Padres padres) {
        return construirSinQr(padres, UUID.randomUUID().toString(), null);
    }

    /**
     * Acta con un {@code codigo_qr} y una {@code qr_payload_version} elegidos por el
     * escenario, informados en la primera asignacion y en el orden que exige la
     * inmutabilidad: primero la version, despues el token.
     */
    private static FalActa actaConQrYVersion(Padres padres, String codigoQr, Integer payloadVersion) {
        FalActa acta = construirSinQr(padres, UUID.randomUUID().toString(), null);
        acta.setQrPayloadVersion(payloadVersion);
        acta.setCodigoQr(codigoQr);
        return acta;
    }

    /**
     * Acta con las 57 columnas informadas hasta donde el DDL lo permite sin sembrar
     * mas padres: talonario, inspector, domicilios, motivo de archivo y las dos
     * versiones geograficas quedan nulos porque son FK nullable y su escenario no es
     * este.
     */
    private static FalActa actaCompleta(Padres padres, String observacionInicial) {
        String usuario = actor();
        FalActa acta = new FalActa(
                null,
                UUID.randomUUID().toString(),
                TipoActa.CONTRAVENCION,
                padres.idDep(),
                null,
                F0,
                T0,
                MARCA + "domicilio del hecho",
                observacionInicial,
                LAT_EXACTA,
                LON_EXACTA,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                padres.idPersona(),
                T0,
                usuario);

        acta.setNroActa(MARCA + "NRO-0001");
        acta.setOrigenCaptura(OrigenCaptura.MOBILE_INSPECTOR);
        acta.setIdDispositivoCaptura(MARCA + "DISPOSITIVO-01");
        acta.setIdUserCaptura(actor());
        acta.setFhCaptura(T0);
        acta.setLatCaptura(LAT_EXACTA);
        acta.setLonCaptura(LON_EXACTA);
        acta.setPrecisionCapturaM(12.75);
        acta.setFhPosCaptura(T0.plusSeconds(30));
        acta.setOrigenPosCaptura(OrigenUbicacion.GPS_DISPOSITIVO);
        acta.setVerDep((int) padres.verDep());
        acta.setResumenHecho(MARCA + "resumen del hecho");
        acta.setIdLocInfrMalvinas("0001");
        acta.setIdTcaInfrMalvinas("4-S-0-0-7C");
        acta.setAlturaInfr(1234);
        acta.setAlturaOrigenInfr((short) 2);
        acta.setSiAlturaInfrEstimada(true);
        acta.setOrigenUbicacionInfr(OrigenUbicacion.PARCELA_SELECCIONADA);
        acta.setSiUbicacionInfrManual(true);
        acta.setSiDomTxtInfr(true);
        acta.setDomTxtInfr(MARCA + "texto libre del lugar");
        acta.setSiEjeUrb(true);
        // Version antes del token: el agregado congela ambos campos en cuanto el QR existe
        // (DECISION-QR-INMUTABLE-01). El valor 3 no es una version canonica del payload; se
        // usa aca solo para probar que la columna round-trippea un valor distinto del default.
        acta.setQrPayloadVersion(3);
        acta.setCodigoQr(MARCA + "COMPLETA-" + UUID.randomUUID());
        acta.setBloqueActual(BloqueActual.ANAL);
        acta.setEstadoProcesal(EstadoProcesalActa.CONCLUIDO);
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        acta.setPermiteReingreso(true);
        acta.setFhCierre(T0.plusDays(1));
        acta.setFhArchivo(T0.plusDays(2));
        acta.setFhUltMod(T0.plusHours(5));
        acta.setIdUserUltMod(actor());
        return acta;
    }

    /** Observacion cuyo propietario es el acta: id_acta_contexto NULL (DECISION_OBS-01). */
    private static FalObservacion observacionDeActa(Long actaId, String texto, LocalDateTime fhAlta) {
        return new FalObservacion(null, EntidadTipoObservada.ACTA, actaId, null,
                texto, OrigenObservacion.USUARIO, null, fhAlta, actor());
    }

    /** Subject de prueba: UUID canonico de 36 caracteres, dentro de CHAR(36). */
    private static String actor() {
        return UUID.randomUUID().toString();
    }

    /**
     * listarTodas devuelve toda la tabla. Los escenarios afirman solo sobre las filas
     * propias para no depender del estado global del sandbox.
     */
    private static List<FalActa> soloDelSlice(List<FalActa> actas) {
        return actas.stream()
                .filter(a -> a.getCodigoQr() != null && a.getCodigoQr().startsWith(MARCA))
                .toList();
    }

    private static String observacionDe(List<FalActa> actas, Long id) {
        return actas.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("el acta " + id + " no aparece en listarTodas"))
                .getObservaciones();
    }

    /**
     * Huella de equivalencia observable entre lecturas y entre adaptadores. Excluye la
     * PK, que no se compara, e incluye el campo observaciones, que es justamente el que
     * la composicion reconstruye.
     */
    private static String huella(FalActa a) {
        return String.join("|",
                String.valueOf(a.getUuidTecnico()),
                String.valueOf(a.getNroActa()),
                String.valueOf(a.getTipoActa()),
                String.valueOf(a.getOrigenCaptura()),
                String.valueOf(a.getFhCaptura()),
                String.valueOf(a.getLatCaptura()),
                String.valueOf(a.getLonCaptura()),
                String.valueOf(a.getPrecisionCapturaM()),
                String.valueOf(a.getOrigenPosCaptura()),
                String.valueOf(a.getFechaActa()),
                String.valueOf(a.getFechaLabrado()),
                String.valueOf(a.getIdDependencia()),
                String.valueOf(a.getVerDep()),
                String.valueOf(a.getIdPersonaInfractor()),
                String.valueOf(a.getResumenHecho()),
                String.valueOf(a.getDomicilioHecho()),
                String.valueOf(a.getIdLocInfrMalvinas()),
                String.valueOf(a.getIdTcaInfrMalvinas()),
                String.valueOf(a.getAlturaInfr()),
                String.valueOf(a.getAlturaOrigenInfr()),
                String.valueOf(a.isSiAlturaInfrEstimada()),
                String.valueOf(a.getLatInfr()),
                String.valueOf(a.getLonInfr()),
                String.valueOf(a.getOrigenUbicacionInfr()),
                String.valueOf(a.isSiUbicacionInfrManual()),
                String.valueOf(a.isSiDomTxtInfr()),
                String.valueOf(a.getDomTxtInfr()),
                String.valueOf(a.isSiEjeUrb()),
                String.valueOf(a.getCodigoQr()),
                String.valueOf(a.getQrPayloadVersion()),
                String.valueOf(a.getBloqueActual()),
                String.valueOf(a.getEstadoProcesal()),
                String.valueOf(a.getSituacionAdministrativa()),
                String.valueOf(a.getResultadoFinal()),
                String.valueOf(a.getResultadoFirmaInfractor()),
                String.valueOf(a.getPermiteReingreso()),
                String.valueOf(a.getFhCierre()),
                String.valueOf(a.getFhArchivo()),
                String.valueOf(a.getVersionRow()),
                String.valueOf(a.getFhAlta()),
                String.valueOf(a.getIdUserAlta()),
                String.valueOf(a.getFhUltMod()),
                String.valueOf(a.getIdUserUltMod()),
                String.valueOf(a.getObservaciones()));
    }

    private int versionCruda(Long id) {
        Integer version = jdbc.sql("SELECT version_row FROM fal_acta WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return version == null ? -1 : version;
    }

    /** Valor de una columna tal como lo devuelve el motor, sin pasar por el modelo. */
    private String cruda(Long id, String columna) {
        return jdbc.sql("SELECT " + columna + " FROM fal_acta WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    /** Representacion decimal exacta almacenada, para comprobar la escala fisica. */
    private String escalaCruda(Long id, String columna) {
        return jdbc.sql("SELECT CAST(" + columna + " AS CHAR) FROM fal_acta WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .single();
    }

    private int contarPorId(String tabla, long id) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    private int contarConMarca(String tabla, String columna) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE " + columna + " LIKE :marca")
                .param("marca", MARCA + "%")
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    /**
     * Inserta la cadena minima que el DDL exige para poder referenciar un acta:
     * fal_dependencia -> fal_dependencia_version -> fal_persona. Mismo patron que
     * JdbcActaEventoRepositoryIT y JdbcObservacionRepositoryIT. Solo se completan las
     * columnas NOT NULL sin default; doc_key de fal_persona es STORED GENERATED y por
     * eso no se informa.
     */
    private Padres insertarPadresMinimos() {
        String usuario = actor();

        Long idDep = insertarYObtenerId("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """, spec -> spec
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", usuario));

        short verDep = 1;
        jdbc.sql("""
                        INSERT INTO fal_dependencia_version
                            (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                        VALUES (:idDep, :verDep, :tipoActa, :vigDesde, :fhAlta, :usuario)
                        """)
                .param("idDep", idDep)
                .param("verDep", verDep)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", usuario)
                .update();

        Long idPersona = insertarYObtenerId("""
                INSERT INTO fal_persona
                    (tipo_persona, tipo_documento, nro_doc, apellido, nombres, fh_alta, id_user_alta)
                VALUES (:tipoPersona, :tipoDoc, :nroDoc, :apellido, :nombres, :fhAlta, :usuario)
                """, spec -> spec
                .param("tipoPersona", (short) 1)
                .param("tipoDoc", (short) 1)
                .param("nroDoc", nroDocumentoDePrueba())
                .param("apellido", "WAVE1A2B1")
                .param("nombres", "TEST")
                .param("fhAlta", T0)
                .param("usuario", usuario));

        // Persona sin documento: el caso de IDENTIFICADO_PARCIAL. doc_key queda NULL y
        // MariaDB admite ilimitadas filas con clave NULL bajo uq_fal_persona_tipo_doc_key.
        Long idPersonaParcial = insertarYObtenerId("""
                INSERT INTO fal_persona
                    (tipo_persona, tipo_documento, nro_doc, apellido, nombres, fh_alta, id_user_alta)
                VALUES (:tipoPersona, NULL, NULL, :apellido, :nombres, :fhAlta, :usuario)
                """, spec -> spec
                .param("tipoPersona", (short) 1)
                .param("apellido", "WAVE1A2B1")
                .param("nombres", "PARCIAL")
                .param("fhAlta", T0)
                .param("usuario", usuario));

        return new Padres(idDep, verDep, idPersona, idPersonaParcial);
    }

    /**
     * Acta minima con una situacion de identificacion explicita. No hay default: cada
     * caso elige la situacion coherente con la persona que pasa.
     */
    private static FalActa actaConSituacion(Padres padres, String sufijo,
                                            SituacionIdentificacionInfractor situacion,
                                            Long idPersona) {
        FalActa acta = new FalActa(
                null,
                UUID.randomUUID().toString(),
                TipoActa.TRANSITO,
                padres.idDep(),
                null,
                F0,
                T0,
                null,
                null,
                null,
                null,
                ResultadoFirmaInfractor.FIRMADA,
                situacion,
                idPersona,
                T0,
                actor());
        acta.setVerDep((int) padres.verDep());
        acta.setCodigoQr(MARCA + sufijo + "-" + UUID.randomUUID());
        return acta;
    }

    /**
     * INSERT literal que saltea el invariante del agregado, para poder ejercer los dos
     * CHECK fisicos con combinaciones que {@code FalActa} nunca deja construir.
     */
    private void insertarCrudo(Padres padres, Integer situacion, Long idPersona) {
        jdbc.sql("""
                INSERT INTO fal_acta (
                    id_tecnico, tipo_acta, origen_captura, fh_captura, fecha_acta, fecha_labrado,
                    id_dep, ver_dep, situacion_identificacion_infractor, id_persona_infractor,
                    codigo_qr, qr_payload_version, bloque_actual, est_proc_act, sit_adm_act,
                    resultado_final, resultado_firma_infractor, version_row, fh_alta, id_user_alta
                ) VALUES (
                    :idTecnico, 1, 1, :t0, :f0, :t0,
                    :idDep, :verDep, :situacion, :idPersona,
                    :codigoQr, 0, 'CAPT', 'TRAM', 'ACTV',
                    0, 1, 0, :t0, :usuario
                )
                """)
                .param("idTecnico", UUID.randomUUID().toString())
                .param("t0", T0)
                .param("f0", F0)
                .param("idDep", padres.idDep())
                .param("verDep", padres.verDep())
                .param("situacion", situacion, Types.SMALLINT)
                .param("idPersona", idPersona, Types.BIGINT)
                .param("codigoQr", MARCA + "crudo-" + UUID.randomUUID())
                .param("usuario", actor())
                .update();
    }

    /** Firma documental de una persona, para comprobar que el backfill no la toca. */
    private String documentoDePersona(Long idPersona) {
        return jdbc.sql("""
                        SELECT CONCAT(COALESCE(tipo_documento, 'null'), '|', COALESCE(nro_doc, 'null'))
                          FROM fal_persona WHERE id = :id
                        """)
                .param("id", idPersona)
                .query(String.class)
                .single();
    }

    private Long insertarYObtenerId(String sql,
                                    Function<JdbcClient.StatementSpec, JdbcClient.StatementSpec> parametros) {
        KeyHolder claves = new GeneratedKeyHolder();
        parametros.apply(jdbc.sql(sql)).update(claves);
        return Optional.ofNullable(claves.getKey())
                .map(Number::longValue)
                .orElseThrow(() -> new IllegalStateException("La fixture no obtuvo el id generado"));
    }

    /**
     * nro_doc es int unsigned con CHECK entre 1 y 99999999 y participa del UNIQUE
     * (tipo_documento, doc_key). Se usa un valor aleatorio del rango alto para no
     * chocar con datos funcionales; de todos modos la fila se revierte.
     */
    private static int nroDocumentoDePrueba() {
        return 90_000_000 + (int) (Math.random() * 9_000_000);
    }
}

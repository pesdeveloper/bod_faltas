package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcPersonaRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de {@link JdbcPersonaRepository} sobre {@code fal_persona} (WAVE 1A2-B1.5B0-B).
 *
 * <p>Ejecuta contra el sandbox real {@code sb_faltas_db} mediante Failsafe:
 * <pre>mvn -P integration-tests verify</pre>
 *
 * <p><b>El candidato no es bean.</b> Se construye a mano con
 * {@code new JdbcPersonaRepository(jdbcClient)} y nunca se obtiene del contexto: el
 * puerto {@code PersonaRepository} lo sigue resolviendo {@code InMemoryPersonaRepository}
 * incluso bajo el perfil {@code jdbc}.
 *
 * <p><b>Disciplina de datos.</b> Todo escenario corre dentro de una transaccion que se
 * revierte siempre; {@code enEscenario(...)} marca {@code rollbackOnly} antes de
 * devolver, de modo que ni un escenario exitoso deja filas. No hay {@code TRUNCATE}, no
 * hay {@code DELETE} y no se desactiva ninguna FK: la limpieza es el rollback. Todas las
 * filas llevan la marca {@code WAVE1A2_B15B0B_} en {@code apellido}.
 *
 * <p><b>Sobre los documentos.</b> Ninguno es real. Los CUIT y CUIL usados son
 * estructuralmente validos pero no verifican modulo 11, que es exactamente lo que el
 * modelo no exige todavia ({@code D-PERS-CUIT-DV-01}).
 *
 * <p><b>Sobre las PK.</b> No se comparan identificadores exactos entre adaptadores ni se
 * afirma una secuencia sin huecos: un INSERT rechazado consume un valor de
 * {@code AUTO_INCREMENT} sin dejar fila, y eso es correcto.
 *
 * <p>Este IT presupone el esquema posterior al {@code ALTER} de
 * {@code DECISION_DDL-PERS-01}: {@code tipo_documento SMALLINT NULL},
 * {@code nro_doc VARCHAR(12) NULL} y {@code doc_key VARCHAR(12) STORED} sin
 * {@code COALESCE} ni {@code LPAD}.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B1.5B0-B: JdbcPersonaRepository candidato sobre fal_persona")
class JdbcPersonaRepositoryIT {

    /** Marca del slice. Cabe entera en {@code apellido VARCHAR(24)}. */
    private static final String MARCA = "WAVE1A2_B15B0B_";

    /** DATETIME(6) guarda microsegundos, no nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 3, 18, 24, 35, 678_901_000);

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    /** El candidato de esta wave: construido a mano, nunca obtenido del contexto. */
    private JdbcPersonaRepository repositorio;

    @BeforeEach
    void construirCandidatoAMano() {
        repositorio = new JdbcPersonaRepository(jdbc);
    }

    // =====================================================================
    // TOPOLOGIA: EL ADAPTADOR ACTIVADO EN WAVE 1A2-B2
    // =====================================================================

    /**
     * Casos 1 y 2 invertidos en WAVE 1A2-B2, el slice que activa el adaptador. Hasta B1.5C
     * verificaban que no fuera bean y que el puerto siguiera resuelto por InMemory.
     */
    @Test
    @DisplayName("1. JdbcPersonaRepository es bean unico bajo jdbc, sin @Primary")
    void adaptadorRegistradoSoloBajoPerfilJdbc() {
        assertThat(ctx.getBeanNamesForType(JdbcPersonaRepository.class)).hasSize(1);
        assertThat(JdbcPersonaRepository.class.getAnnotation(
                org.springframework.stereotype.Repository.class)).isNotNull();
        assertThat(JdbcPersonaRepository.class.getAnnotation(
                org.springframework.context.annotation.Profile.class).value())
                .containsExactly("jdbc");
        assertThat(JdbcPersonaRepository.class.getAnnotation(
                org.springframework.context.annotation.Primary.class))
                .as("una sola implementacion por puerto: @Primary esconderia una ambiguedad")
                .isNull();
    }

    @Test
    @DisplayName("2. Con perfil jdbc el puerto de persona lo resuelve JdbcPersonaRepository")
    void puertoDePersonaResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(PersonaRepository.class)).hasSize(1);
        assertThat(ctx.getBean(PersonaRepository.class)).isInstanceOf(JdbcPersonaRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryPersonaRepository.class))
                .as("el InMemory queda fuera del perfil jdbc")
                .isEmpty();
    }

    @Test
    @DisplayName("3. El adaptador implementa los 6 metodos reales del puerto y es una clase concreta")
    void implementaElContratoCompleto() {
        assertThat(PersonaRepository.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .containsExactlyInAnyOrder("guardar", "buscarPorId", "listarTodas",
                        "buscarPorTipoDocYNroDoc", "buscarPorIdSujBie", "listarPagina");
        assertThat(Modifier.isAbstract(JdbcPersonaRepository.class.getModifiers())).isFalse();
        assertThat(PersonaRepository.class).isAssignableFrom(JdbcPersonaRepository.class);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS (PATRON P3)
    // =====================================================================

    @Test
    @DisplayName("4. guardar con id null inserta y devuelve el id de AUTO_INCREMENT")
    void altaSinIdRecibeIdGenerado() {
        enEscenario(() -> {
            FalPersona persistida = repositorio.guardar(conDni("ALTA", "12345678"));

            assertThat(persistida.getId()).isNotNull().isPositive();
            assertThat(repositorio.buscarPorId(persistida.getId())).isPresent();
        });
    }

    @Test
    @DisplayName("5. El id no se preasigna: dos altas reciben ids distintos")
    void altasSucesivasRecibenIdsDistintos() {
        enEscenario(() -> {
            Long primera = repositorio.guardar(conDni("UNA", "12345678")).getId();
            Long segunda = repositorio.guardar(conDni("OTRA", "87654321")).getId();

            assertThat(segunda).isNotNull().isNotEqualTo(primera);
        });
    }

    @Test
    @DisplayName("6. guardar no muta el argumento del alta: la candidata conserva id nulo")
    void altaNoMutaElArgumento() {
        enEscenario(() -> {
            FalPersona candidata = conDni("SIN MUTAR", "12345678");

            FalPersona persistida = repositorio.guardar(candidata);

            assertThat(candidata.getId()).as("el argumento no se muta").isNull();
            assertThat(persistida.getId()).isNotNull();
        });
    }

    /** Las 20 columnas insertables en un unico round-trip, todas con valor propio. */
    @Test
    @DisplayName("7. Round-trip completo de las 20 columnas del modelo")
    void roundTripDeLasVeinteColumnas() {
        enEscenario(() -> {
            String userAlta = actor();
            String userMod = actor();
            FalPersona original = new FalPersona(null, TipoPersona.FISICA, T0, userAlta);
            original.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "27-00123456-9"));
            original.setApellido(MARCA);
            original.setNombres("ROUND TRIP");
            original.setNombreMostrar(MARCA + " ROUND TRIP");
            original.setEmailPrincipal("round.trip@example.invalid");
            original.setTelefonoPrincipal("+54 11 5555-0000");
            original.setIdSuj(20);
            original.setIdBie(1234567);
            original.setSujBieEstado(SujBieEstado.ACTIVA);
            original.setFhSujBieCreacion(T0.minusDays(3));
            original.setFhUltMod(T0.plusHours(4));
            original.setIdUserUltMod(userMod);

            Long id = repositorio.guardar(original).getId();
            FalPersona leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getId()).isEqualTo(id);
            assertThat(leida.getTipoPersona()).isEqualTo(TipoPersona.FISICA);
            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.CUIT);
            assertThat(leida.getPrefijoCuitCuil()).isEqualTo(27);
            assertThat(leida.getNroDoc()).isEqualTo("00123456");
            assertThat(leida.getDigitoVerificador()).isEqualTo(9);
            assertThat(leida.getApellido()).isEqualTo(MARCA);
            assertThat(leida.getNombres()).isEqualTo("ROUND TRIP");
            assertThat(leida.getNombreMostrar()).isEqualTo(MARCA + " ROUND TRIP");
            assertThat(leida.getEmailPrincipal()).isEqualTo("round.trip@example.invalid");
            assertThat(leida.getTelefonoPrincipal()).isEqualTo("+54 11 5555-0000");
            assertThat(leida.getIdSuj()).isEqualTo(20);
            assertThat(leida.getIdBie()).isEqualTo(1234567);
            assertThat(leida.getSujBieEstado()).isEqualTo(SujBieEstado.ACTIVA);
            assertThat(leida.getFhSujBieCreacion()).isEqualTo(T0.minusDays(3));
            assertThat(leida.getFhAlta()).isEqualTo(T0);
            assertThat(leida.getIdUserAlta()).isEqualTo(userAlta);
            assertThat(leida.getFhUltMod()).isEqualTo(T0.plusHours(4));
            assertThat(leida.getIdUserUltMod()).isEqualTo(userMod);
        });
    }

    /** El estado que el esquema anterior no podia representar. */
    @Test
    @DisplayName("8. Persona sin documento: los cuatro componentes viajan NULL y vuelven NULL")
    void personaSinDocumentoEsPersistible() {
        enEscenario(() -> {
            FalPersona sinDocumento = base("SIN DOCUMENTO");

            Long id = repositorio.guardar(sinDocumento).getId();
            FalPersona leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getDocumento().estaAusente()).isTrue();
            assertThat(leida.getTipoDoc()).isNull();
            assertThat(leida.getNroDoc()).isNull();
            assertThat(leida.getPrefijoCuitCuil()).isNull();
            assertThat(leida.getDigitoVerificador()).isNull();
        });
    }

    @Test
    @DisplayName("9. DNI canonico: sin prefijo ni digito, ceros iniciales ya eliminados")
    void dniCanonicoRoundTrip() {
        enEscenario(() -> {
            FalPersona conDni = base("DNI");
            conDni.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "0007.654.321"));

            Long id = repositorio.guardar(conDni).getId();
            FalPersona leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.DNI);
            assertThat(leida.getNroDoc()).isEqualTo("7654321");
            assertThat(leida.getPrefijoCuitCuil()).isNull();
            assertThat(leida.getDigitoVerificador()).isNull();
        });
    }

    @Test
    @DisplayName("10. Pasaporte alfanumerico: lo que el esquema INT UNSIGNED no podia guardar")
    void pasaporteAlfanumericoRoundTrip() {
        enEscenario(() -> {
            FalPersona conPasaporte = base("PASAPORTE");
            conPasaporte.setDocumento(
                    DocumentoPersona.normalizar(TipoDocumentoPersona.PASAPORTE, "ab12-3456"));

            Long id = repositorio.guardar(conPasaporte).getId();
            FalPersona leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.PASAPORTE);
            assertThat(leida.getNroDoc()).isEqualTo("AB123456");
        });
    }

    @Test
    @DisplayName("11. CUIT estructurado: prefijo, cuerpo de 8 y digito en sus tres columnas")
    void cuitEstructuradoRoundTrip() {
        enEscenario(() -> {
            FalPersona conCuit = base("CUIT");
            conCuit.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3"));

            Long id = repositorio.guardar(conCuit).getId();
            FalPersona leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.CUIT);
            assertThat(leida.getPrefijoCuitCuil()).isEqualTo(20);
            assertThat(leida.getNroDoc()).as("el cuerpo central, nunca los once digitos").isEqualTo("12345678");
            assertThat(leida.getDigitoVerificador()).isEqualTo(3);
            assertThat(leida.docTxt()).isEqualTo("CUIT 20-12345678-3");
        });
    }

    @Test
    @DisplayName("12. CUIL estructurado y su convivencia con el CUIT de los mismos once digitos")
    void cuilEstructuradoConviveConElCuit() {
        enEscenario(() -> {
            FalPersona conCuit = base("CUIT");
            conCuit.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20123456783"));
            FalPersona conCuil = base("CUIL");
            conCuil.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIL, "20123456783"));

            Long idCuit = repositorio.guardar(conCuit).getId();
            Long idCuil = repositorio.guardar(conCuil).getId();

            assertThat(idCuil).isNotEqualTo(idCuit);
            assertThat(docKeyDe(idCuit)).isEqualTo(docKeyDe(idCuil));
            assertThat(repositorio.buscarPorId(idCuil).orElseThrow().getTipoDoc())
                    .as("el UNIQUE es compuesto: los distingue tipo_documento, no la clave")
                    .isEqualTo(TipoDocumentoPersona.CUIL);
        });
    }

    // =====================================================================
    // doc_key: LA COLUMNA GENERADA
    // =====================================================================

    @Test
    @DisplayName("13. doc_key de un DNI reproduce el nro_doc canonico")
    void docKeyDeDniEsElNroDoc() {
        enEscenario(() -> {
            Long id = repositorio.guardar(conDni("DOC KEY DNI", "7654321")).getId();

            assertThat(docKeyDe(id)).isEqualTo("7654321");
        });
    }

    @Test
    @DisplayName("14. doc_key de un CUIT son los once digitos, sin COALESCE ni LPAD")
    void docKeyDeCuitSonOnceDigitos() {
        enEscenario(() -> {
            FalPersona conCuit = base("DOC KEY CUIT");
            conCuit.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "27-00123456-9"));

            Long id = repositorio.guardar(conCuit).getId();

            assertThat(docKeyDe(id))
                    .as("el cero inicial del cuerpo se conserva porque el ancho es fijo, no rellenado")
                    .isEqualTo("27001234569")
                    .hasSize(11);
        });
    }

    @Test
    @DisplayName("15. Sin documento, doc_key es NULL y no un centinela")
    void docKeyEsNullSinDocumento() {
        enEscenario(() -> {
            Long id = repositorio.guardar(base("SIN DOC KEY")).getId();

            assertThat(docKeyDe(id)).isNull();
        });
    }

    @Test
    @DisplayName("16. Varias personas sin documento coexisten bajo el UNIQUE: NULL no colisiona")
    void variasPersonasSinDocumentoCoexisten() {
        enEscenario(() -> {
            Long a = repositorio.guardar(base("SIN DOC A")).getId();
            Long b = repositorio.guardar(base("SIN DOC B")).getId();
            Long c = repositorio.guardar(base("SIN DOC C")).getId();

            assertThat(List.of(a, b, c)).doesNotHaveDuplicates();
            assertThat(repositorio.listarTodas()).hasSize(3);
        });
    }

    // =====================================================================
    // CONSTRAINTS FISICAS
    // =====================================================================

    @Test
    @DisplayName("17. Duplicado documental: el UNIQUE lo rechaza y no se enmascara como otra cosa")
    void duplicadoDocumentalSeRechaza() {
        enEscenario(() -> {
            repositorio.guardar(conDni("ORIGINAL", "99887766"));

            assertThatThrownBy(() -> repositorio.guardar(conDni("DUPLICADA", "99887766")))
                    .as("D-B1-UNIQUE-PERS-01: sin excepcion funcional canonica, se preserva la de Spring")
                    .isInstanceOf(DuplicateKeyException.class);
        });
    }

    /**
     * Los casos 18 a 21 fueron el ejemplo canonico de {@code CHECK} fisico como segunda
     * barrera. {@code chk_fal_persona_doc_coherente}, {@code chk_fal_persona_doc_cuit} y
     * {@code chk_fal_persona_doc_no_cuit} fueron retirados por
     * {@code DECISION_DDL-SIN-CHECKS-CODIGOS-01} (ACTA-ALTA-INTEGRAL-JDBC-MASTER-
     * 20260804-224853): son CHECK de coherencia funcional entre campos, no invariantes
     * tecnicos. {@link DocumentoPersona} sigue siendo la unica barrera real; un INSERT
     * crudo que la bypasea ya no es rechazado por el motor.
     */
    @Test
    @DisplayName("18. chk_fal_persona_doc_coherente fue retirado: el INSERT crudo ya no lo rechaza")
    void checkDocumentoIncompleto() {
        enEscenario(() -> {
            assertThat(errorDeInsertCrudo("1, NULL, NULL, NULL")).isNull();
            assertThat(errorDeInsertCrudo("NULL, NULL, '12345678', NULL")).isNull();
        });
    }

    @Test
    @DisplayName("19. chk_fal_persona_doc_cuit fue retirado: CUIT sin prefijo ya no es rechazado por el motor")
    void checkCuitSinPrefijo() {
        enEscenario(() -> {
            assertThat(errorDeInsertCrudo("2, NULL, '12345678', 3")).isNull();
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "12345678"))
                    .as("Java sigue siendo la unica barrera real")
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    @DisplayName("20. chk_fal_persona_doc_cuit fue retirado: CUIT sin digito ya no es rechazado por el motor")
    void checkCuitSinDigito() {
        enEscenario(() -> {
            assertThat(errorDeInsertCrudo("2, 20, '12345678', NULL")).isNull();
        });
    }

    @Test
    @DisplayName("21. chk_fal_persona_doc_no_cuit fue retirado: componentes CUIT en otro tipo ya no se rechazan en el motor")
    void checkComponentesCuitEnOtroTipo() {
        enEscenario(() -> {
            assertThat(errorDeInsertCrudo("4, 20, 'AB123456', 3")).isNull();
            assertThatThrownBy(() -> new DocumentoPersona(
                    TipoDocumentoPersona.PASAPORTE, "AB123456", 20, 3))
                    .as("Java sigue siendo la unica barrera real")
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    /**
     * Divergencia de traduccion declarada, no defecto. Un valor mas largo que la columna
     * produce el error 1406 con SQLState 22001, que el driver de MariaDB entrega como
     * {@code SQLSyntaxErrorException}; Spring lo traduce entonces a
     * {@link org.springframework.jdbc.BadSqlGrammarException} y no a
     * {@link DataIntegrityViolationException}, que es lo que produce cualquier CHECK.
     *
     * <p>Lo que importa del contrato se verifica igual: el rechazo ocurre, es un error de
     * acceso a datos y <b>no</b> se enmascara como duplicado documental. La barrera
     * primaria sigue siendo Java, donde {@link DocumentoPersona} rechaza el valor antes
     * de llegar al motor.
     */
    @Test
    @DisplayName("22. Longitud: 12 caracteres es la frontera valida y 13 se rechaza")
    void longitudMaximaDeNroDoc() {
        enEscenario(() -> {
            FalPersona limite = base("LIMITE 12");
            limite.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "ABCDEF123456"));
            assertThat(repositorio.guardar(limite).getId()).isNotNull();

            Throwable rechazo = errorDeInsertCrudo("9, NULL, 'ABCDEF1234567', NULL");
            assertThat(rechazo)
                    .as("VARCHAR(12) no admite 13 caracteres")
                    .isInstanceOf(DataAccessException.class)
                    .hasStackTraceContaining("Data too long for column 'nro_doc'");
            assertThat(rechazo)
                    .as("el exceso de longitud no puede enmascararse como duplicado documental")
                    .isNotInstanceOf(DuplicateKeyException.class);
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "ABCDEF1234567"))
                    .as("la barrera primaria es Java, no el motor")
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    @DisplayName("23. chk_fal_persona_nro_doc_dni y chk_fal_persona_doc_tipo fueron retirados "
            + "(DECISION_DDL-SIN-CHECKS-CODIGOS-01)")
    void checkDniCanonicoYCatalogo() {
        enEscenario(() -> {
            assertThat(errorDeInsertCrudo("1, NULL, '01234567', NULL")).isNull();
            assertThat(errorDeInsertCrudo("7, NULL, '12345678', NULL")).isNull();
        });
    }

    // =====================================================================
    // UPDATE (PATRON P3)
    // =====================================================================

    @Test
    @DisplayName("24. UPDATE de una persona existente reescribe las columnas mutables")
    void updateDeExistenteReescribe() {
        enEscenario(() -> {
            FalPersona persistida = repositorio.guardar(conDni("ANTES", "12345678"));
            String userMod = actor();

            persistida.setNombres("DESPUES");
            persistida.setEmailPrincipal("nuevo@example.invalid");
            persistida.setFhUltMod(T0.plusDays(1));
            persistida.setIdUserUltMod(userMod);
            FalPersona devuelta = repositorio.guardar(persistida);

            assertThat(devuelta.getId()).isEqualTo(persistida.getId());
            FalPersona leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();
            assertThat(leida.getNombres()).isEqualTo("DESPUES");
            assertThat(leida.getEmailPrincipal()).isEqualTo("nuevo@example.invalid");
            assertThat(leida.getFhUltMod()).isEqualTo(T0.plusDays(1));
            assertThat(leida.getIdUserUltMod()).isEqualTo(userMod);
            assertThat(repositorio.listarTodas()).as("actualiza, no inserta").hasSize(1);
        });
    }

    /** El SET lista todas las columnas mutables: llevar una a NULL se persiste. */
    @Test
    @DisplayName("25. UPDATE que quita el documento lo lleva a NULL en las cuatro columnas")
    void updateQuitaElDocumento() {
        enEscenario(() -> {
            FalPersona persistida = repositorio.guardar(conDni("CON DOC", "12345678"));

            persistida.setDocumento(DocumentoPersona.ausente());
            repositorio.guardar(persistida);

            FalPersona leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();
            assertThat(leida.getTipoDoc()).isNull();
            assertThat(leida.getNroDoc()).isNull();
            assertThat(docKeyDe(persistida.getId())).isNull();
        });
    }

    @Test
    @DisplayName("26. UPDATE de DNI a CUIT informa prefijo y digito, y recalcula doc_key")
    void updateDeDniACuit() {
        enEscenario(() -> {
            FalPersona persistida = repositorio.guardar(conDni("A CUIT", "12345678"));

            persistida.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "23-11222333-4"));
            repositorio.guardar(persistida);

            FalPersona leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();
            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.CUIT);
            assertThat(leida.getPrefijoCuitCuil()).isEqualTo(23);
            assertThat(leida.getNroDoc()).isEqualTo("11222333");
            assertThat(leida.getDigitoVerificador()).isEqualTo(4);
            assertThat(docKeyDe(persistida.getId())).isEqualTo("23112223334");
        });
    }

    @Test
    @DisplayName("27. UPDATE de CUIT a pasaporte limpia prefijo y digito: no quedan residuales")
    void updateDeCuitAPasaporteLimpiaComponentes() {
        enEscenario(() -> {
            FalPersona persistida = base("CUIT A PASAPORTE");
            persistida.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "23-11222333-4"));
            persistida = repositorio.guardar(persistida);

            persistida.setDocumento(
                    DocumentoPersona.normalizar(TipoDocumentoPersona.PASAPORTE, "XY987654"));
            repositorio.guardar(persistida);

            FalPersona leida = repositorio.buscarPorId(persistida.getId()).orElseThrow();
            assertThat(leida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.PASAPORTE);
            assertThat(leida.getNroDoc()).isEqualTo("XY987654");
            assertThat(leida.getPrefijoCuitCuil()).as("sin residuo del CUIT anterior").isNull();
            assertThat(leida.getDigitoVerificador()).isNull();
            assertThat(docKeyDe(persistida.getId())).isEqualTo("XY987654");
        });
    }

    @Test
    @DisplayName("28. UPDATE con id inexistente lanza PersonaNoEncontradaException")
    void updateDeInexistenteLanza() {
        enEscenario(() -> {
            FalPersona conIdAjeno = conDni("AJENA", "12345678").conId(999_999_999L);

            assertThatThrownBy(() -> repositorio.guardar(conIdAjeno))
                    .isInstanceOf(PersonaNoEncontradaException.class)
                    .hasMessage("Persona no encontrada: 999999999");
        });
    }

    @Test
    @DisplayName("29. UPDATE con id inexistente no inserta: un id del llamador nunca crea fila")
    void updateDeInexistenteNoInserta() {
        enEscenario(() -> {
            FalPersona conIdAjeno = conDni("AJENA", "12345678").conId(999_999_999L);

            catchThrowable(() -> repositorio.guardar(conIdAjeno));

            assertThat(repositorio.buscarPorId(999_999_999L)).isEmpty();
            assertThat(repositorio.listarTodas()).isEmpty();
        });
    }

    // =====================================================================
    // LECTURAS
    // =====================================================================

    @Test
    @DisplayName("30. buscarPorId devuelve la persona exacta y no tiene efectos laterales")
    void buscarPorIdDevuelveLaPersona() {
        enEscenario(() -> {
            Long id = repositorio.guardar(conDni("BUSCADA", "12345678")).getId();

            Optional<FalPersona> hallada = repositorio.buscarPorId(id);

            assertThat(hallada).isPresent();
            assertThat(hallada.orElseThrow().getId()).isEqualTo(id);
            assertThat(repositorio.listarTodas()).hasSize(1);
        });
    }

    @Test
    @DisplayName("31. buscarPorId de un id inexistente devuelve Optional vacio")
    void buscarPorIdInexistenteDevuelveVacio() {
        enEscenario(() -> assertThat(repositorio.buscarPorId(999_999_999L)).isEmpty());
    }

    @Test
    @DisplayName("32. listarTodas respeta ORDER BY id ASC aunque se inserte en otro orden")
    void listarTodasOrdenaPorIdAscendente() {
        enEscenario(() -> {
            Long tercera = repositorio.guardar(conDni("TERCERA", "33333333")).getId();
            Long primera = repositorio.guardar(conDni("PRIMERA", "11111111")).getId();
            Long segunda = repositorio.guardar(conDni("SEGUNDA", "22222222")).getId();

            assertThat(repositorio.listarTodas())
                    .extracting(FalPersona::getId)
                    .containsExactly(tercera, primera, segunda)
                    .isSorted();
        });
    }

    @Test
    @DisplayName("33. buscarPorTipoDocYNroDoc devuelve lista ordenada por id ASC")
    void buscarPorDocumentoOrdenaPorIdAscendente() {
        enEscenario(() -> {
            Long dni = repositorio.guardar(conDni("POR DOC", "45678901")).getId();
            repositorio.guardar(conDni("OTRA", "45678902"));

            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "45678901"))
                    .extracting(FalPersona::getId)
                    .containsExactly(dni);
        });
    }

    @Test
    @DisplayName("34. buscarPorTipoDocYNroDoc sin coincidencia, con tipo distinto o con nulos devuelve vacio")
    void buscarPorDocumentoSinCoincidenciaDevuelveVacio() {
        enEscenario(() -> {
            repositorio.guardar(conDni("EXISTE", "45678901"));

            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "00000009")).isEmpty();
            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.PASAPORTE, "45678901"))
                    .as("el tipo forma parte de la identidad natural")
                    .isEmpty();
            assertThat(repositorio.buscarPorTipoDocYNroDoc(null, "45678901")).isEmpty();
            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, null)).isEmpty();
        });
    }

    /**
     * La busqueda usa la forma canonica, la misma que se persistio. Para CUIT eso es el
     * cuerpo central de ocho digitos, no los once: el puerto no transporta el prefijo ni
     * el digito.
     */
    @Test
    @DisplayName("35. Un CUIT se encuentra por su forma canonica, no por los once digitos")
    void buscarCuitPorFormaCanonica() {
        enEscenario(() -> {
            FalPersona conCuit = base("CUIT BUSCADO");
            DocumentoPersona documento =
                    DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3");
            conCuit.setDocumento(documento);
            Long id = repositorio.guardar(conCuit).getId();

            assertThat(repositorio.buscarPorTipoDocYNroDoc(documento.tipoDoc(), documento.nroDoc()))
                    .extracting(FalPersona::getId)
                    .containsExactly(id);
            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.CUIT, "20123456783"))
                    .as("los once digitos no son la forma canonica de nro_doc")
                    .isEmpty();
        });
    }

    @Test
    @DisplayName("36. buscarPorIdSujBie ordena por id ASC y exige los dos valores informados")
    void buscarPorIdSujBieOrdenaYExigeAmbos() {
        enEscenario(() -> {
            Long segunda = repositorio.guardar(conVinculo("VINCULO B", "55555552", 20, 4242)).getId();
            Long primera = repositorio.guardar(conVinculo("VINCULO A", "55555551", 20, 4242)).getId();
            repositorio.guardar(conVinculo("OTRO BIEN", "55555553", 20, 9999));

            assertThat(repositorio.buscarPorIdSujBie(20, 4242))
                    .extracting(FalPersona::getId)
                    .containsExactly(segunda, primera)
                    .isSorted();
            assertThat(repositorio.buscarPorIdSujBie(null, 4242)).isEmpty();
            assertThat(repositorio.buscarPorIdSujBie(20, null)).isEmpty();
            assertThat(repositorio.buscarPorIdSujBie(20, 1)).isEmpty();
        });
    }

    // =====================================================================
    // FORMA FISICA
    // =====================================================================

    @Test
    @DisplayName("37. Nullabilidad y tipos exactos de las cinco columnas del documento")
    void nullabilidadYTiposExactos() {
        List<String[]> esperado = List.of(
                new String[]{"tipo_documento", "smallint(6)", "YES"},
                new String[]{"prefijo_cuit_cuil", "tinyint(3) unsigned", "YES"},
                new String[]{"nro_doc", "varchar(12)", "YES"},
                new String[]{"digito_verificador", "tinyint(3) unsigned", "YES"},
                new String[]{"doc_key", "varchar(12)", "YES"});

        for (String[] columna : esperado) {
            List<String> forma = jdbc.sql("""
                            SELECT CONCAT(column_type, '|', is_nullable)
                              FROM information_schema.columns
                             WHERE table_schema = DATABASE()
                               AND table_name = 'fal_persona'
                               AND column_name = :columna
                            """)
                    .param("columna", columna[0])
                    .query(String.class)
                    .list();
            assertThat(forma)
                    .as("fal_persona.%s debe ser %s %s", columna[0], columna[1], columna[2])
                    .containsExactly(columna[1] + "|" + columna[2]);
        }
    }

    @Test
    @DisplayName("38. doc_key es STORED GENERATED: el adaptador no la informa ni la expone al dominio")
    void docKeyEsGeneradaYNoPerteneceAlDominio() {
        String extra = jdbc.sql("""
                        SELECT extra FROM information_schema.columns
                         WHERE table_schema = DATABASE()
                           AND table_name = 'fal_persona' AND column_name = 'doc_key'
                        """)
                .query(String.class)
                .single();

        assertThat(extra).isEqualTo("STORED GENERATED");
        assertThat(FalPersona.class.getDeclaredMethods())
                .as("FalPersona no expone docKey: no se agrega al dominio solo por existir fisicamente")
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("getDocKey");
    }

    // =====================================================================
    // TRANSACCION Y RESIDUOS
    // =====================================================================

    @Test
    @DisplayName("39. Rollback real de un alta: la fila es visible dentro y desaparece al revertir")
    void rollbackRealDeUnAlta() {
        long[] id = new long[1];

        new TransactionTemplate(txManager).execute(status -> {
            id[0] = repositorio.guardar(conDni("SE REVIERTE", "76543210")).getId();
            assertThat(repositorio.buscarPorId(id[0])).isPresent();
            status.setRollbackOnly();
            return null;
        });

        assertThat(repositorio.buscarPorId(id[0]))
                .as("la persona %s no debe sobrevivir al rollback", id[0])
                .isEmpty();
    }

    @Test
    @DisplayName("40. Rollback real de una actualizacion: no queda ni la fila ni el cambio")
    void rollbackRealDeUnaActualizacion() {
        long[] id = new long[1];

        new TransactionTemplate(txManager).execute(status -> {
            FalPersona persistida = repositorio.guardar(conDni("ORIGINAL", "76543211"));
            id[0] = persistida.getId();
            persistida.setNombres("MODIFICADA");
            repositorio.guardar(persistida);
            assertThat(repositorio.buscarPorId(id[0]).orElseThrow().getNombres()).isEqualTo("MODIFICADA");
            status.setRollbackOnly();
            return null;
        });

        assertThat(repositorio.buscarPorId(id[0])).isEmpty();
    }

    // =====================================================================
    // PARIDAD CONTRA EL ORACULO InMemory
    // =====================================================================

    @Test
    @DisplayName("41. Paridad de alta: id asignado por la persistencia y argumento intacto en los dos")
    void paridadDeAlta() {
        enEscenario(() -> {
            PersonaRepository enMemoria = new InMemoryPersonaRepository();
            FalPersona paraJdbc = conDni("PARIDAD", "64000001");
            FalPersona paraMemoria = conDni("PARIDAD", "64000001");

            FalPersona deJdbc = repositorio.guardar(paraJdbc);
            FalPersona deMemoria = enMemoria.guardar(paraMemoria);

            assertThat(paraJdbc.getId()).isNull();
            assertThat(paraMemoria.getId()).isNull();
            assertThat(deJdbc.getId()).isNotNull().isPositive();
            assertThat(deMemoria.getId()).isNotNull().isPositive();
            assertThat(deJdbc.getNroDoc()).isEqualTo(deMemoria.getNroDoc());
            assertThat(deJdbc.getTipoDoc()).isEqualTo(deMemoria.getTipoDoc());
        });
    }

    @Test
    @DisplayName("42. Paridad de update inexistente: misma excepcion, mismo mensaje y ninguno inserta")
    void paridadDeUpdateInexistente() {
        enEscenario(() -> {
            PersonaRepository enMemoria = new InMemoryPersonaRepository();
            FalPersona conIdAjeno = conDni("AJENA", "64000002").conId(999_999_999L);

            Throwable deJdbc = catchThrowable(() -> repositorio.guardar(conIdAjeno));
            Throwable deMemoria = catchThrowable(() -> enMemoria.guardar(conIdAjeno));

            assertThat(deJdbc).isInstanceOf(PersonaNoEncontradaException.class);
            assertThat(deMemoria).isInstanceOf(PersonaNoEncontradaException.class);
            assertThat(deJdbc.getMessage()).isEqualTo(deMemoria.getMessage());
            assertThat(repositorio.listarTodas()).isEmpty();
            assertThat(enMemoria.listarTodas()).isEmpty();
        });
    }

    @Test
    @DisplayName("43. Paridad de busquedas: mismo orden, mismos filtros y mismo vacio")
    void paridadDeBusquedas() {
        enEscenario(() -> {
            PersonaRepository enMemoria = new InMemoryPersonaRepository();

            List<FalPersona> altas = List.of(
                    conVinculo("TERCERA", "64000103", 20, 777),
                    conVinculo("PRIMERA", "64000101", 20, 777),
                    conVinculo("SEGUNDA", "64000102", 20, 888));

            List<String> nombresJdbc = altas.stream()
                    .map(p -> repositorio.guardar(p).getNombres()).toList();
            altas.forEach(enMemoria::guardar);

            assertThat(nombresJdbc).hasSize(3);
            assertThat(nombres(repositorio.listarTodas()))
                    .isEqualTo(nombres(enMemoria.listarTodas()));
            assertThat(nombres(repositorio.buscarPorIdSujBie(20, 777)))
                    .isEqualTo(nombres(enMemoria.buscarPorIdSujBie(20, 777)))
                    .containsExactly("TERCERA", "PRIMERA");
            assertThat(nombres(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "64000101")))
                    .isEqualTo(nombres(enMemoria.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "64000101")));
            assertThat(repositorio.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "64000999"))
                    .isEqualTo(enMemoria.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "64000999"));
        });
    }

    @Test
    @DisplayName("44. La suite no deja ninguna fila con la marca del slice")
    void sinResiduosDeLaMarcaDelSlice() {
        Integer residuos = jdbc.sql("SELECT COUNT(*) FROM fal_persona WHERE apellido = :marca")
                .param("marca", MARCA)
                .query(Integer.class)
                .single();

        assertThat(residuos)
                .as("ninguna fila con la marca %s debe quedar fuera de una transaccion revertida", MARCA)
                .isZero();
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /** Ejecuta el escenario dentro de una transaccion que siempre se revierte. */
    private void enEscenario(Runnable escenario) {
        new TransactionTemplate(txManager).execute(status -> {
            try {
                escenario.run();
                return null;
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /** Persona fisica nueva, sin documento, con la marca del slice. */
    private static FalPersona base(String nombres) {
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, T0, actor());
        persona.setApellido(MARCA);
        persona.setNombres(nombres);
        persona.setNombreMostrar(MARCA + " " + nombres);
        return persona;
    }

    private static FalPersona conDni(String nombres, String nroDoc) {
        FalPersona persona = base(nombres);
        persona.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, nroDoc));
        return persona;
    }

    private static FalPersona conVinculo(String nombres, String nroDoc, Integer idSuj, Integer idBie) {
        FalPersona persona = conDni(nombres, nroDoc);
        persona.setIdSuj(idSuj);
        persona.setIdBie(idBie);
        persona.setSujBieEstado(SujBieEstado.ACTIVA);
        persona.setFhSujBieCreacion(T0.minusDays(1));
        return persona;
    }

    /** Subject de prueba de 36 caracteres, dentro de CHAR(36). */
    private static String actor() {
        return UUID.randomUUID().toString();
    }

    private static List<String> nombres(List<FalPersona> personas) {
        return personas.stream().map(FalPersona::getNombres).toList();
    }

    /**
     * doc_key no pertenece al modelo: se lee directamente de la columna para validar la
     * expresion generada, no a traves de {@link FalPersona}.
     */
    private String docKeyDe(Long id) {
        return jdbc.sql("SELECT doc_key FROM fal_persona WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    /**
     * INSERT directo para ejercer combinaciones que {@link DocumentoPersona} no permite
     * construir. Es el unico camino por el que este IT escribe sin pasar por el
     * adaptador, y existe justamente para demostrar que el CHECK fisico es una barrera
     * independiente de la validacion Java.
     */
    private Throwable errorDeInsertCrudo(String componentesDocumento) {
        String sql = "INSERT INTO fal_persona ("
                + " tipo_persona, tipo_documento, prefijo_cuit_cuil, nro_doc,"
                + " digito_verificador, apellido, nombres, fh_alta, id_user_alta)"
                + " VALUES (1, " + componentesDocumento
                + ", :apellido, 'CRUDO', :fhAlta, :usuario)";
        return catchThrowable(() -> jdbc.sql(sql)
                .param("apellido", MARCA)
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update());
    }
}

package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT de {@link JdbcActaEvidenciaRepository} sobre {@code fal_acta_evidencia}.
 *
 * <p>Adaptador creado en WAVE 1A2-B2 para completar la frontera transaccional del alta: antes de
 * este slice el puerto solo tenia adaptador InMemory y sus escrituras sobrevivian al rollback.
 *
 * <p><b>Disciplina de datos.</b> Todo escenario corre dentro de una transaccion que se revierte
 * siempre; el sandbox queda sin residuos. La verificacion del rollback real del alta completa vive
 * en {@code ActaLabrarTransaccionalIT}, que por definicion no puede envolver nada.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B2: JdbcActaEvidenciaRepository sobre fal_acta_evidencia")
class JdbcActaEvidenciaRepositoryIT {

    private static final String MARCA = "WAVE1A2B2E_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 12, 0, 0, 123_456_000);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);
    private static final String HASH_VALIDO =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private ActaEvidenciaRepository puerto;

    @Autowired
    private ActaRepository actas;

    private String actor;

    @BeforeEach
    void nuevoActor() {
        actor = UUID.randomUUID().toString();
    }

    // =====================================================================
    // TOPOLOGIA
    // =====================================================================

    @Test
    @DisplayName("1. Con perfil jdbc el puerto de evidencias lo resuelve el adaptador JDBC")
    void puertoResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(ActaEvidenciaRepository.class)).hasSize(1);
        assertThat(puerto).isInstanceOf(JdbcActaEvidenciaRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryActaEvidenciaRepository.class))
                .as("el InMemory queda fuera del perfil jdbc")
                .isEmpty();
    }

    @Test
    @DisplayName("2. El adaptador comparte el JdbcClient del contexto, sin conexion propia")
    void comparteElRecursoDelContexto() {
        assertThat(ctx.getBeanNamesForType(JdbcClient.class))
                .as("un unico JdbcClient sobre el unico DataSource")
                .hasSize(1);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS
    // =====================================================================

    /**
     * El id de la evidencia de entrada se ignora y la PK la asigna {@code AUTO_INCREMENT}. Es la
     * misma regla de {@code JdbcActaEventoRepository}: por eso no existe camino que produzca PK
     * duplicada a traves del puerto.
     */
    @Test
    @DisplayName("3. El alta ignora el id de entrada y devuelve el que asigno el motor")
    void altaIgnoraElIdDeEntrada() {
        enEscenario(idActa -> {
            long idProvisional = 999_999_999L;
            FalActaEvidencia entrada = new FalActaEvidencia(idProvisional, idActa,
                    TipoEvidenciaActa.PDF, MARCA + "storage/a.jpg", T0, T0, actor, null);

            FalActaEvidencia persistida = puerto.guardar(entrada);

            assertThat(persistida.getId())
                    .as("la PK la asigna el motor, no el llamador")
                    .isNotNull()
                    .isNotEqualTo(idProvisional);
            assertThat(idCrudo(persistida.getId())).isEqualTo(persistida.getId());
            assertThat(entrada.getId())
                    .as("el argumento no se muta")
                    .isEqualTo(idProvisional);
            return null;
        });
    }

    @Test
    @DisplayName("4. Round-trip de las 8 columnas, con hash informado")
    void roundTripCompletoConHash() {
        enEscenario(idActa -> {
            FalActaEvidencia persistida = puerto.guardar(new FalActaEvidencia(
                    1L, idActa, TipoEvidenciaActa.PDF, MARCA + "storage/acta.pdf",
                    T0, T0.plusSeconds(5), actor, HASH_VALIDO));

            FalActaEvidencia leida = puerto.listarPorActa(idActa).get(0);

            assertThat(leida.getId()).isEqualTo(persistida.getId());
            assertThat(leida.getIdActa()).isEqualTo(idActa);
            assertThat(leida.getTipoEvid()).isEqualTo(TipoEvidenciaActa.PDF);
            assertThat(leida.getStorageKey()).isEqualTo(MARCA + "storage/acta.pdf");
            assertThat(leida.getFechaRegistro()).isEqualTo(T0);
            assertThat(leida.getFhAlta()).isEqualTo(T0.plusSeconds(5));
            assertThat(leida.getIdUserAlta()).isEqualTo(actor);
            assertThat(leida.getHashEvid()).isEqualTo(HASH_VALIDO);
            return null;
        });
    }

    @Test
    @DisplayName("5. Round-trip con hash nulo: la columna admite NULL")
    void roundTripConHashNulo() {
        enEscenario(idActa -> {
            puerto.guardar(new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.VIDEO,
                    MARCA + "storage/v.mp4", T0, T0, actor, null));

            assertThat(puerto.listarPorActa(idActa).get(0).getHashEvid()).isNull();
            return null;
        });
    }

    /**
     * Los siete valores del catalogo viajan por {@code codigo()} y vuelven por
     * {@code desdeCodigo(short)}: nunca por {@code ordinal()} ni por {@code name()}
     * ({@code DECISION_DDL-EVID-02}).
     */
    @Test
    @DisplayName("6. Los siete tipos de evidencia hacen round-trip por su codigo canonico")
    void todosLosTiposHacenRoundTrip() {
        enEscenario(idActa -> {
            int orden = 1;
            for (TipoEvidenciaActa tipo : TipoEvidenciaActa.values()) {
                if (tipo == TipoEvidenciaActa.FOTO) {
                    // FOTO exige las siete columnas obligatorias (DECISION_DDL-EVID-03).
                    puerto.guardar(new FalActaEvidencia(1L, idActa, tipo,
                            MARCA + "storage/" + tipo.name(), T0, T0, actor, HASH_A,
                            orden++, null, null, "image/jpeg", 10L, 10, 10, null, null,
                            ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia.ALTA_INICIAL));
                } else {
                    puerto.guardar(new FalActaEvidencia(1L, idActa, tipo,
                            MARCA + "storage/" + tipo.name(), T0, T0, actor, null));
                }
            }
            List<FalActaEvidencia> leidas = puerto.listarPorActa(idActa);
            assertThat(leidas).hasSize(TipoEvidenciaActa.values().length);
            assertThat(leidas).extracting(FalActaEvidencia::getTipoEvid)
                    .containsExactlyInAnyOrder(TipoEvidenciaActa.values());

            // El codigo fisico persistido es el canonico, no el ordinal del enum.
            for (TipoEvidenciaActa tipo : TipoEvidenciaActa.values()) {
                assertThat(jdbc.sql("""
                        SELECT COUNT(*) FROM fal_acta_evidencia
                         WHERE id_acta = :idActa AND tipo_evid = :codigo
                        """)
                        .param("idActa", idActa)
                        .param("codigo", tipo.codigo())
                        .query(Long.class).single())
                        .as("%s se persiste con codigo %s", tipo.name(), tipo.codigo())
                        .isEqualTo(1L);
            }
            return null;
        });
    }

    @Test
    @DisplayName("7. DATETIME(6) conserva microsegundos en las dos fechas")
    void microsegundosConservados() {
        enEscenario(idActa -> {
            LocalDateTime conMicros = LocalDateTime.of(2026, 8, 4, 12, 0, 0, 987_654_000);
            puerto.guardar(new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.PDF,
                    MARCA + "storage/micro.jpg", conMicros, conMicros.plusNanos(1_000), actor, null));

            FalActaEvidencia leida = puerto.listarPorActa(idActa).get(0);
            assertThat(leida.getFechaRegistro()).isEqualTo(conMicros);
            assertThat(leida.getFhAlta()).isEqualTo(conMicros.plusNanos(1_000));
            return null;
        });
    }

    // =====================================================================
    // LECTURA POR ACTA
    // =====================================================================

    /**
     * El orden canonico es {@code fh_alta ASC, id ASC}. El desempate por id importa: el alta
     * comparte un unico instante de {@code FaltasClock}, de modo que todas las evidencias de un
     * mismo labrado tienen el mismo {@code fh_alta}.
     */
    @Test
    @DisplayName("8. listarPorActa ordena por fh_alta y desempata por id")
    void ordenCanonicoConDesempate() {
        enEscenario(idActa -> {
            FalActaEvidencia tardia = puerto.guardar(evidencia(idActa, "tardia", T0.plusHours(2)));
            FalActaEvidencia mismoInstanteA = puerto.guardar(evidencia(idActa, "mismoA", T0));
            FalActaEvidencia mismoInstanteB = puerto.guardar(evidencia(idActa, "mismoB", T0));

            assertThat(puerto.listarPorActa(idActa))
                    .extracting(FalActaEvidencia::getId)
                    .containsExactly(mismoInstanteA.getId(), mismoInstanteB.getId(), tardia.getId());
            return null;
        });
    }

    @Test
    @DisplayName("9. listarPorActa no mezcla evidencias de otra acta")
    void aislamientoPorActa() {
        enEscenario(idActa -> {
            Long otraActa = insertarActa();
            puerto.guardar(evidencia(idActa, "propia", T0));
            puerto.guardar(evidencia(otraActa, "ajena", T0));

            assertThat(puerto.listarPorActa(idActa))
                    .hasSize(1)
                    .extracting(FalActaEvidencia::getStorageKey)
                    .containsExactly(MARCA + "propia");
            return null;
        });
    }

    @Test
    @DisplayName("10. Un acta sin evidencias devuelve lista vacia, y un id nulo tambien")
    void sinEvidenciasListaVacia() {
        enEscenario(idActa -> {
            assertThat(puerto.listarPorActa(idActa)).isEmpty();
            assertThat(puerto.listarPorActa(null))
                    .as("un id nulo no identifica ningun acta y no consulta")
                    .isEmpty();
            return null;
        });
    }

    // =====================================================================
    // INTEGRIDAD
    // =====================================================================

    /**
     * La clave ajena {@code fk_evidencia_acta} se propaga sin traducirse: el puerto no promete
     * ninguna excepcion funcional para una evidencia de un acta inexistente.
     */
    @Test
    @DisplayName("11. Una clave ajena violada propaga DataIntegrityViolationException")
    void claveAjenaVioladaPropaga() {
        enEscenario(idActa -> {
            assertThatThrownBy(() -> puerto.guardar(new FalActaEvidencia(1L, 999_999_999L,
                    TipoEvidenciaActa.PDF, MARCA + "huerfana", T0, T0, actor, null)))
                    .isInstanceOf(DataIntegrityViolationException.class);
            return null;
        });
    }

    /**
     * {@code chk_evidencia_hash} exige 64 caracteres. El dominio ya rechaza un hash mal formado
     * antes de llegar al motor, de modo que la constraint fisica es la segunda barrera y no la
     * unica.
     */
    @Test
    @DisplayName("12. El dominio rechaza un hash que no sea SHA-256 hexadecimal de 64")
    void hashInvalidoRechazadoPorElDominio() {
        enEscenario(idActa -> {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.FOTO,
                    MARCA + "hash corto", T0, T0, actor, "abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SHA-256");
            return null;
        });
    }

    @Test
    @DisplayName("13. Una evidencia nula se rechaza antes de tocar el motor")
    void evidenciaNulaRechazada() {
        assertThatThrownBy(() -> puerto.guardar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidencia es obligatoria");
    }

    // =====================================================================
    // IDENTIDAD PROVISIONAL
    // =====================================================================

    /**
     * {@code nextId()} existe porque {@code FalActaEvidencia} exige id no nulo en su constructor.
     * En el camino JDBC su valor es provisional y {@code guardar} lo descarta. Lo que este caso
     * fija es que dos llamadas no devuelvan el mismo valor: si lo hicieran, dos evidencias
     * construidas y todavia no persistidas compartirian identidad.
     */
    @Test
    @DisplayName("14. nextId devuelve valores distintos y no corresponde a la PK persistida")
    void nextIdEsProvisionalYUnico() {
        enEscenario(idActa -> {
            Long uno = puerto.nextId();
            Long dos = puerto.nextId();
            assertThat(uno).isNotNull().isNotEqualTo(dos);

            FalActaEvidencia persistida = puerto.guardar(new FalActaEvidencia(uno, idActa,
                    TipoEvidenciaActa.PDF, MARCA + "provisional", T0, T0, actor, null));
            assertThat(persistida.getId())
                    .as("la PK real la asigno el motor y no coincide con el provisional")
                    .isNotEqualTo(uno);
            return null;
        });
    }

    // =====================================================================
    // PARIDAD CON EL ORACULO
    // =====================================================================

    /**
     * Paridad de lo que si es comparable. El InMemory usa el id que recibe como clave, porque
     * simula la secuencia con su propio contador; el JDBC lo ignora y usa el del motor. La
     * diferencia esta declarada y lo que debe coincidir es el contenido observable de la fila.
     */
    @Test
    @DisplayName("15. Paridad con el oraculo InMemory en el contenido de la evidencia")
    void paridadDeContenidoConElOraculo() {
        enEscenario(idActa -> {
            ActaEvidenciaRepository enMemoria = new InMemoryActaEvidenciaRepository();

            FalActaEvidencia paraJdbc = new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.AUDIO,
                    MARCA + "paridad", T0, T0, actor, HASH_VALIDO);
            FalActaEvidencia paraMemoria = new FalActaEvidencia(enMemoria.nextId(), idActa,
                    TipoEvidenciaActa.AUDIO, MARCA + "paridad", T0, T0, actor, HASH_VALIDO);

            puerto.guardar(paraJdbc);
            enMemoria.guardar(paraMemoria);

            FalActaEvidencia deJdbc = puerto.listarPorActa(idActa).get(0);
            FalActaEvidencia deMemoria = enMemoria.listarPorActa(idActa).get(0);

            assertThat(deJdbc.getIdActa()).isEqualTo(deMemoria.getIdActa());
            assertThat(deJdbc.getTipoEvid()).isEqualTo(deMemoria.getTipoEvid());
            assertThat(deJdbc.getStorageKey()).isEqualTo(deMemoria.getStorageKey());
            assertThat(deJdbc.getFechaRegistro()).isEqualTo(deMemoria.getFechaRegistro());
            assertThat(deJdbc.getFhAlta()).isEqualTo(deMemoria.getFhAlta());
            assertThat(deJdbc.getIdUserAlta()).isEqualTo(deMemoria.getIdUserAlta());
            assertThat(deJdbc.getHashEvid()).isEqualTo(deMemoria.getHashEvid());
            return null;
        });
    }

    // =====================================================================
    // FOTOGRAFIAS (Wave 4 Parte 3D, DECISION_DDL-EVID-03)
    // =====================================================================

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);

    private FalActaEvidencia foto(Long idActa, int orden, String hash, String storageKeySufijo) {
        return new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.FOTO,
                MARCA + storageKeySufijo, T0, T0, actor, hash,
                orden, "descripcion " + orden, "original.jpg", "image/jpeg", 2048L, 800, 600,
                T0.minusDays(1),
                ar.gob.malvinas.faltas.core.domain.enums.BloqueActual.ENRI.codigo(),
                ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia.ALTA_INICIAL);
    }

    @Test
    @DisplayName("16. round-trip completo de las diez columnas de fotografia")
    void roundTripCompletoDeFotografia() {
        enEscenario(idActa -> {
            puerto.guardar(foto(idActa, 1, HASH_A, "foto1.jpg"));

            FalActaEvidencia leida = puerto.listarFotografiasPorActa(idActa).get(0);
            assertThat(leida.getTipoEvid()).isEqualTo(TipoEvidenciaActa.FOTO);
            assertThat(leida.getOrden()).isEqualTo(1);
            assertThat(leida.getDescripcion()).isEqualTo("descripcion 1");
            assertThat(leida.getNombreOriginal()).isEqualTo("original.jpg");
            assertThat(leida.getMimeTipo()).isEqualTo("image/jpeg");
            assertThat(leida.getTamanioBytes()).isEqualTo(2048L);
            assertThat(leida.getAnchoPx()).isEqualTo(800);
            assertThat(leida.getAltoPx()).isEqualTo(600);
            assertThat(leida.getFechaCapturaExif()).isEqualTo(T0.minusDays(1));
            assertThat(leida.getBloqueAlIncorporar()).isEqualTo("ENRI");
            assertThat(leida.getOrigenIncorporacion())
                    .isEqualTo(ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia.ALTA_INICIAL);
            assertThat(leida.getHashEvid()).isEqualTo(HASH_A);
            return null;
        });
    }

    @Test
    @DisplayName("17. listarFotografiasPorActa ordena por orden ascendente, no por fh_alta")
    void listarFotografiasOrdenaPorOrden() {
        enEscenario(idActa -> {
            puerto.guardar(foto(idActa, 3, HASH_A, "c.jpg"));
            puerto.guardar(foto(idActa, 1, HASH_B, "a.jpg"));
            puerto.guardar(foto(idActa, 2, HASH_C, "b.jpg"));

            assertThat(puerto.listarFotografiasPorActa(idActa))
                    .extracting(FalActaEvidencia::getOrden)
                    .containsExactly(1, 2, 3);
            return null;
        });
    }

    @Test
    @DisplayName("18. listarFotografiasPorActa excluye evidencias no-FOTO de la misma acta")
    void listarFotografiasExcluyeNoFoto() {
        enEscenario(idActa -> {
            puerto.guardar(foto(idActa, 1, HASH_A, "foto.jpg"));
            puerto.guardar(evidencia(idActa, "firma", T0));

            assertThat(puerto.listarFotografiasPorActa(idActa)).hasSize(1);
            assertThat(puerto.listarPorActa(idActa)).hasSize(2);
            return null;
        });
    }

    @Test
    @DisplayName("19. buscarFotografiaPorActaYId resuelve solo fotografias de esa acta puntual")
    void buscarFotografiaPorActaYId() {
        enEscenario(idActa -> {
            FalActaEvidencia guardada = puerto.guardar(foto(idActa, 1, HASH_A, "foto.jpg"));
            Long otraActa = insertarActa();

            assertThat(puerto.buscarFotografiaPorActaYId(idActa, guardada.getId())).isPresent();
            assertThat(puerto.buscarFotografiaPorActaYId(otraActa, guardada.getId())).isEmpty();
            assertThat(puerto.buscarFotografiaPorActaYId(idActa, 999_999_999L)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("20. maxOrdenFotografiasPorActa devuelve 0 sin fotografias, y el maximo real con fotografias")
    void maxOrdenFotografias() {
        enEscenario(idActa -> {
            assertThat(puerto.maxOrdenFotografiasPorActa(idActa)).isZero();

            puerto.guardar(foto(idActa, 1, HASH_A, "a.jpg"));
            puerto.guardar(foto(idActa, 2, HASH_B, "b.jpg"));

            assertThat(puerto.maxOrdenFotografiasPorActa(idActa)).isEqualTo(2);
            return null;
        });
    }

    @Test
    @DisplayName("21. existeFotografiaConHash distingue por acta y detecta el hash exacto")
    void existeFotografiaConHash() {
        enEscenario(idActa -> {
            Long otraActa = insertarActa();
            puerto.guardar(foto(idActa, 1, HASH_A, "a.jpg"));

            assertThat(puerto.existeFotografiaConHash(idActa, HASH_A)).isTrue();
            assertThat(puerto.existeFotografiaConHash(idActa, HASH_B)).isFalse();
            assertThat(puerto.existeFotografiaConHash(otraActa, HASH_A))
                    .as("la misma fotografia en otra acta esta permitida (Parte 3D S5.10)")
                    .isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("22. UNIQUE(id_acta, orden) real: dos fotografias con el mismo orden en la misma acta violan la restriccion")
    void uniqueOrdenViolado() {
        enEscenario(idActa -> {
            puerto.guardar(foto(idActa, 1, HASH_A, "a.jpg"));
            assertThatThrownBy(() -> puerto.guardar(foto(idActa, 1, HASH_B, "b.jpg")))
                    .isInstanceOf(DataIntegrityViolationException.class);
            return null;
        });
    }

    @Test
    @DisplayName("23. UNIQUE(id_acta, hash_evid) real: dos fotografias con el mismo SHA-256 en la misma acta violan la restriccion")
    void uniqueHashViolado() {
        enEscenario(idActa -> {
            puerto.guardar(foto(idActa, 1, HASH_A, "a.jpg"));
            assertThatThrownBy(() -> puerto.guardar(foto(idActa, 2, HASH_A, "b.jpg")))
                    .isInstanceOf(DataIntegrityViolationException.class);
            return null;
        });
    }

    @Test
    @DisplayName("24. la misma orden y el mismo hash SI se permiten en actas distintas (UNIQUE es por acta)")
    void mismoOrdenYHashEnActasDistintasPermitido() {
        enEscenario(idActa -> {
            Long otraActa = insertarActa();
            puerto.guardar(foto(idActa, 1, HASH_A, "a.jpg"));
            FalActaEvidencia enOtraActa = puerto.guardar(foto(otraActa, 1, HASH_A, "a-otra.jpg"));

            assertThat(enOtraActa.getId()).isNotNull();
            assertThat(puerto.listarFotografiasPorActa(otraActa)).hasSize(1);
            return null;
        });
    }

    @Test
    @DisplayName("25. UNIQUE(id_acta, hash_evid) no colisiona entre evidencias no-FOTO (hash_evid NULL): MariaDB trata cada NULL como distinto")
    void nullEsDistintoEnElUniqueDeHash() {
        enEscenario(idActa -> {
            puerto.guardar(evidencia(idActa, "no-foto-1", T0));
            puerto.guardar(evidencia(idActa, "no-foto-2", T0));
            // Ambas con hash_evid NULL: si NULL colisionara, la segunda fallaria.
            assertThat(puerto.listarPorActa(idActa)).hasSize(2);
            return null;
        });
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /**
     * Evidencia generica no-FOTO (PDF): estos casos ejercitan comportamiento generico del
     * puerto (orden por fh_alta, aislamiento por acta, etc.), no metadata de fotografia.
     * Wave 4 Parte 3D exige las siete columnas obligatorias para FOTO, de modo que los
     * fixtures genericos preexistentes usan un tipo no-FOTO para no acoplarse a esa metadata.
     */
    private FalActaEvidencia evidencia(Long idActa, String sufijo, LocalDateTime fhAlta) {
        return new FalActaEvidencia(1L, idActa, TipoEvidenciaActa.PDF,
                MARCA + sufijo, T0, fhAlta, actor, null);
    }

    private Long idCrudo(Long id) {
        return jdbc.sql("SELECT id FROM fal_acta_evidencia WHERE id = :id")
                .param("id", id).query(Long.class).single();
    }

    /** Escenario dentro de una transaccion que se revierte siempre. */
    private <T> T enEscenario(Function<Long, T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.apply(insertarActa());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /** Acta padre minima: la clave ajena de la evidencia la exige. */
    private Long insertarActa() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();
        Long idDep = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, 1, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDep)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();

        FalActa acta = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, idDep, null,
                F0, T0, MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, T0, actor);
        acta.setCodigoQr(MARCA + UUID.randomUUID());
        return actas.guardar(acta).getId();
    }
}

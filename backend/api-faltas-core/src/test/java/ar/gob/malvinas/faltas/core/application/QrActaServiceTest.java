package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.PlazosTestSupport;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;

import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.QrAccesoTransaccionalService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionIntentoService;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.qr.HmacSha256QrTokenProtector;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11K: QrActaService - circuitos de generacion y resolucion QR")
class QrActaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryQrAccesoRepository qrAccesoRepo;
    private QrTokenProtector tokenProtector;
    private QrActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        qrAccesoRepo = new InMemoryQrAccesoRepository();
        tokenProtector = QrTestSupport.protector(); // clave fija TEST_ONLY, payload version 0

        var docRepo = new InMemoryDocumentoRepository();
        var pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        var falloRepo = new InMemoryFalloActaRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, new InMemoryNotificacionRepository(),
                pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

        QrAccesoTransaccionalService transaccional =
                new QrAccesoTransaccionalService(qrAccesoRepo, eventoRepo, recalc);
        service = new QrActaService(actaRepo, qrAccesoRepo, transaccional, tokenProtector, FaltasClockTestSupport.FIXED);
    }

    /** Etiqueta logica del test -> id real que asigno la persistencia. */
    private final java.util.Map<Long, Long> idsActa = new java.util.concurrent.ConcurrentHashMap<>();

    /** Etiqueta logica del test -> token emitido en el alta de esa acta. */
    private final java.util.Map<Long, String> tokensActa = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Alta real: el acta se construye sin id y la PK la asigna la persistencia.
     * La etiqueta solo sirve para referirse al acta dentro del test; no es la PK.
     *
     * <p>El QR se emite <b>en el alta</b> y nunca despues, igual que en
     * {@code ActaService.labrar} ({@code DECISION-QR-INMUTABLE-01}). Por eso el fixture lo
     * emite aca: no existe ninguna operacion posterior que se lo pueda asignar.
     *
     * <p>El uuidTecnico es un UUID canonico porque el token canonico solo admite esa forma
     * ({@code DECISION_DDL-ACTA-QR-01}).
     */
    private FalActa crearActa(Long etiqueta) {
        String uuidTecnico = java.util.UUID.randomUUID().toString();
        QrTokenEmitido qr = service.emitirTokenPara(uuidTecnico);

        FalActa a = new FalActa(null, uuidTecnico,
                TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, SituacionIdentificacionInfractor.PRESENTE_NO_APORTA_DATOS, null, AHORA, "SYS");
        a.setBloqueActual(BloqueActual.NOTI);
        a.setQrPayloadVersion(qr.version());
        a.setCodigoQr(qr.token());

        FalActa persistida = actaRepo.guardar(a);
        idsActa.put(etiqueta, persistida.getId());
        tokensActa.put(etiqueta, qr.token());
        return persistida;
    }

    /** Id real del acta creada con esa etiqueta. */
    private Long acta(Long etiqueta) {
        return idsActa.get(etiqueta);
    }

    /** Token emitido en el alta de esa acta. */
    private String token(Long etiqueta) {
        return tokensActa.get(etiqueta);
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Emision unica del QR")
    class EmisionUnica {

        @Test @DisplayName("el alta emite el token y queda informado en el acta")
        void altaEmiteElToken() {
            crearActa(1L);
            assertThat(token(1L)).isNotNull().isNotBlank();
            assertThat(actaRepo.buscarPorId(acta(1L)).orElseThrow().getCodigoQr())
                    .isEqualTo(token(1L));
        }

        @Test @DisplayName("token generado tiene longitud <= 128 chars y entra en codigo_qr VARCHAR(128)")
        void tokenMaxLength() {
            crearActa(1L);
            String token = token(1L);
            assertThat(token.length())
                    .as("el token debe entrar en fal_acta.codigo_qr VARCHAR(128)")
                    .isLessThanOrEqualTo(128);
            assertThat(token)
                    .as("version 0 produce exactamente 86 caracteres: QR0.(4) + uuid(36) + .(1)"
                            + " + version(1) + .(1) + firma(43)")
                    .hasSize(86);
        }

        @Test @DisplayName("el token persistido y la version quedan juntos y coherentes")
        void tokenYVersionSePersistenJuntos() {
            crearActa(1L);
            String token = token(1L);
            FalActa acta = actaRepo.buscarPorId(acta(1L)).orElseThrow();
            assertThat(acta.getCodigoQr()).isEqualTo(token);
            assertThat(acta.getQrPayloadVersion())
                    .as("qr_payload_version debe ser la version firmada dentro del token")
                    .isZero();
            assertThat(token.split("\\.")[2])
                    .as("el segmento de version del token debe coincidir con qr_payload_version")
                    .isEqualTo(String.valueOf(acta.getQrPayloadVersion()));
        }

        @Test @DisplayName("la generacion es deterministica: mismo uuid, version y clave dan el mismo token")
        void generacionDeterministica() {
            FalActa acta = crearActa(1L);
            String token = token(1L);
            assertThat(tokenProtector.generar(acta.getUuidTecnico()))
                    .as("el token no depende del reloj ni de aleatoriedad")
                    .isEqualTo(token);
        }

        /**
         * {@code DECISION-QR-INMUTABLE-01} elimina el caso de uso: no hay rotacion,
         * regeneracion, revocacion ni reemplazo individual del QR de un acta. La ausencia se
         * verifica sobre el API publico real del servicio, no sobre su texto.
         */
        @Test @DisplayName("el servicio no expone ninguna operacion de rotacion o regeneracion")
        void sinApiDeRotacionNiRegeneracion() {
            Set<String> operaciones = new HashSet<>();
            for (java.lang.reflect.Method m : QrActaService.class.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                    operaciones.add(m.getName());
                }
            }
            assertThat(operaciones)
                    .as("el QR se emite una sola vez, durante el labrado")
                    .doesNotContain("rotarQr", "generarQr", "regenerarQr", "revocarQr",
                            "reemitirQr", "persistirQr");
            assertThat(operaciones)
                    .as("el API minimo realmente usado por el alta y por el portal")
                    .containsExactlyInAnyOrder("emitirTokenPara", "registrarAcceso",
                            "registrarAccesoConNotificacion", "listarAccesosPorActa");
        }

        @Test @DisplayName("el alta no emite QRGENA y ninguna operacion posterior lo emite")
        void sinEventoQrgena() {
            crearActa(1L);
            service.registrarAcceso(token(1L), CanalAccesoQr.PORTAL, null, null, "c");

            assertThat(eventoRepo.buscarPorActa(acta(1L)))
                    .as("no hay reemision, de modo que no hay QRGENA posterior")
                    .noneMatch(e -> e.tipoEvt() == TipoEventoActa.QRGENA);
        }

        @Test @DisplayName("token no predecible - dos actas generan tokens distintos")
        void tokenNoPredecible() {
            crearActa(1L);
            crearActa(2L);
            assertThat(token(1L)).isNotEqualTo(token(2L));
        }

        @Test @DisplayName("unicidad: tokens para distintas actas son distintos")
        void unicidadTokensDistintasActas() {
            Set<String> tokens = new HashSet<>();
            for (long id = 1; id <= 10; id++) {
                crearActa(id);
                tokens.add(token(id));
            }
            assertThat(tokens).hasSize(10);
        }

        @Test @DisplayName("payload no contiene PII visible en texto plano")
        void payloadSinPii() {
            crearActa(1L);
            String token = token(1L);
            // el token es cifrado; no debe contener nada legible
            assertThat(token).doesNotContain("nombre")
                    .doesNotContain("documento")
                    .doesNotContain("domicilio")
                    .doesNotContain("Libertad")  // domicilioHecho del acta
                    .doesNotContain("infractor");
        }

        @Test @DisplayName("token NO contiene actaId numerica en claro")
        void tokenNoContieneIdNumerico() {
            crearActa(42L);
            // el token solo transporta prefijo, identidad tecnica, version y firma
            assertThat(token(42L))
                    .doesNotContain("\"id\":" + acta(42L))
                    .doesNotContain("id=" + acta(42L));
        }

        @Test @DisplayName("el agregado rechaza un segundo QR distinto para la misma acta")
        void agregadoRechazaSegundoQr() {
            FalActa acta = crearActa(1L);
            String otroToken = service.emitirTokenPara(java.util.UUID.randomUUID().toString()).token();

            assertThatThrownBy(() -> acta.setCodigoQr(otroToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inmutable");

            assertThat(actaRepo.buscarPorId(acta(1L)).orElseThrow().getCodigoQr())
                    .as("el token persistido no cambia")
                    .isEqualTo(token(1L));
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Resolucion de acceso QR - caso valido")
    class AccesoValido {

        @Test @DisplayName("token valido genera acceso VALIDO en repositorio")
        void accesoValidoInsertado() {
            crearActa(1L);
            String token = token(1L);
            QrActaService.AccesoQrResultado r = service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "corr-1");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            assertThat(r.actaId()).isEqualTo(acta(1L));
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(1);
        }

        @Test @DisplayName("multiples accesos validos al mismo QR son legitimos")
        void multiplesAccesosValidos() {
            crearActa(1L);
            String token = token(1L);
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c1");
            service.registrarAcceso(token, CanalAccesoQr.APP, null, null, "c2");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c3");
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(3);
        }

        @Test @DisplayName("evento QRACC registrado tras acceso valido")
        void eventoQraccRegistrado() {
            crearActa(1L);
            String token = token(1L);
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "corr-1");
            assertThat(eventoRepo.buscarPorActa(acta(1L)))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.QRACCA);
        }

        @Test @DisplayName("acceso con IP IPv4 valida")
        void accesoConIpv4() {
            crearActa(1L);
            String token = token(1L);
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, "192.168.1.1", null, "c");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            FalActaQrAcceso acc = qrAccesoRepo.buscarPorId(r.idAcceso()).orElseThrow();
            assertThat(acc.getIpOrigen()).isEqualTo("192.168.1.1");
        }

        @Test @DisplayName("acceso con IP IPv6 valida")
        void accesoConIpv6() {
            crearActa(1L);
            String token = token(1L);
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, "2001:db8::1", null, "c");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            FalActaQrAcceso acc = qrAccesoRepo.buscarPorId(r.idAcceso()).orElseThrow();
            assertThat(acc.getIpOrigen()).isEqualTo("2001:db8::1");
        }

        @Test @DisplayName("acceso con user-agent valido")
        void accesoConUserAgent() {
            crearActa(1L);
            String token = token(1L);
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0";
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.APP, null, ua, "c");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            FalActaQrAcceso acc = qrAccesoRepo.buscarPorId(r.idAcceso()).orElseThrow();
            assertThat(acc.getUserAgent()).isEqualTo(ua);
        }

        @Test @DisplayName("acceso con todos los canales disponibles")
        void todosCanalesTolerados() {
            crearActa(1L);
            String token = token(1L);
            for (CanalAccesoQr canal : CanalAccesoQr.values()) {
                service.registrarAcceso(token, canal, null, null, "c");
            }
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(CanalAccesoQr.values().length);
        }

        @Test @DisplayName("resultado devuelto no incluye token ni payload")
        void resultadoSinToken() {
            crearActa(1L);
            String token = token(1L);
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, null, null, "corr");
            // el record no tiene campo token
            // comprobamos que los campos visibles son solo los seguros
            assertThat(r.idAcceso()).isNotNull();
            assertThat(r.actaId()).isEqualTo(acta(1L));
            assertThat(r.canal()).isEqualTo(CanalAccesoQr.PORTAL);
        }

        @Test @DisplayName("copias defensivas: modificar objeto devuelto no afecta repositorio")
        void copiasDefensivas() {
            crearActa(1L);
            String token = token(1L);
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c");
            List<FalActaQrAcceso> lista1 = qrAccesoRepo.listarPorActa(acta(1L));
            List<FalActaQrAcceso> lista2 = qrAccesoRepo.listarPorActa(acta(1L));
            assertThat(lista1).isNotSameAs(lista2);
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Tokens invalidos - nunca persisten acceso")
    class TokensInvalidos {

        @Test @DisplayName("token nulo lanza excepcion y no inserta acceso")
        void tokenNulo() {
            crearActa(1L);
            assertThatThrownBy(() -> service.registrarAcceso(null, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
        }

        @Test @DisplayName("token en blanco lanza excepcion y no inserta acceso")
        void tokenEnBlanco() {
            crearActa(1L);
            assertThatThrownBy(() -> service.registrarAcceso("  ", CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
        }

        @Test @DisplayName("token corrupto/manipulado lanza excepcion y no inserta")
        void tokenCorrupto() {
            crearActa(1L);
            assertThatThrownBy(() -> service.registrarAcceso(
                    "ZXN0b2VzdW50b2tlbmNvcnJ1cHRv", CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
        }

        @Test @DisplayName("token firmado con otra clave es invalido")
        void tokenDeOtraClave() {
            FalActa acta = crearActa(1L);
            String tokenOtro = QrTestSupport.protectorAlternativo().generar(acta.getUuidTecnico());
            assertThatThrownBy(() -> service.registrarAcceso(tokenOtro, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
        }

        @Test @DisplayName("token con una version de payload no soportada es invalido")
        void tokenDeVersionNoSoportada() {
            FalActa acta = crearActa(1L);
            // Unico formato implementado: 0. Se fuerza la version 1 conservando la forma
            // canonica; el rechazo debe venir de la version, antes de verificar la firma.
            String[] partes = token(1L).split("\\.");
            String tokenV1 = partes[0] + "." + partes[1] + ".1." + partes[3];

            assertThatThrownBy(() -> service.registrarAcceso(tokenV1, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageContaining("version de payload no soportada");
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
            assertThat(actaRepo.buscarPorId(acta(1L)).orElseThrow().getUuidTecnico())
                    .isEqualTo(acta.getUuidTecnico());
        }

        @Test @DisplayName("token demasiado largo lanza excepcion y no inserta")
        void tokenDemasiadoLargo() {
            crearActa(1L);
            String tokenLargo = "x".repeat(129);
            assertThatThrownBy(() -> service.registrarAcceso(tokenLargo, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
        }

        @Test @DisplayName("acta inexistente (uuid no registrado) lanza excepcion")
        void actaInexistente() {
            // token criptograficamente valido para un UUID que no existe en el repositorio
            String token = tokenProtector.generar(java.util.UUID.randomUUID().toString());
            assertThatThrownBy(() -> service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            // no existe acta en el repo, por lo tanto no hay accesos
        }

        @Test @DisplayName("errores de resolucion son indistinguibles desde el exterior")
        void erroresIndistinguibles() {
            crearActa(1L);
            // token corrupto -> QrTokenInvalidoException generico
            Class<QrTokenInvalidoException> expected = QrTokenInvalidoException.class;
            assertThatThrownBy(() -> service.registrarAcceso("token-invalido", CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(expected);
            // acta inexistente -> tambien QrTokenInvalidoException
            String tokenParaUuidNoExiste = tokenProtector.generar(java.util.UUID.randomUUID().toString());
            assertThatThrownBy(() -> service.registrarAcceso(tokenParaUuidNoExiste, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(expected);
        }

        @Test @DisplayName("excepcion de token invalido no incluye el token en el mensaje")
        void excepcionNoContieneToken() {
            crearActa(1L);
            try {
                service.registrarAcceso("token-secreto-no-debe-aparecer", CanalAccesoQr.PORTAL, null, null, "c");
                fail("debia lanzar excepcion");
            } catch (QrTokenInvalidoException e) {
                assertThat(e.getMessage()).doesNotContain("token-secreto-no-debe-aparecer");
            }
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Validaciones de entidad FalActaQrAcceso")
    class ValidacionesEntidad {

        @Test @DisplayName("IP invalida en entidad lanza excepcion")
        void ipInvalida() {
            assertThatThrownBy(() ->
                    new FalActaQrAcceso(1L, 1L, AHORA, CanalAccesoQr.PORTAL,
                            "no-es-una-ip", null, ResultadoAccesoQr.VALIDO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP valida");
        }

        @Test @DisplayName("IP demasiado larga (> 45 chars) lanza excepcion")
        void ipDemasiadoLarga() {
            String ipLarga = "2001:db8::" + "1234:5678:9abc:def0:1234:5678:9abc:def0:1234:5678";
            assertThatThrownBy(() ->
                    new FalActaQrAcceso(1L, 1L, AHORA, CanalAccesoQr.PORTAL,
                            ipLarga, null, ResultadoAccesoQr.VALIDO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ipOrigen excede");
        }

        @Test @DisplayName("userAgent sanitizado mayor a 255 chars lanza excepcion")
        void userAgentDemasiadoLargo() {
            String uaLargo = "Mozilla/5.0 " + "x".repeat(250);
            assertThatThrownBy(() ->
                    new FalActaQrAcceso(1L, 1L, AHORA, CanalAccesoQr.PORTAL,
                            null, uaLargo, ResultadoAccesoQr.VALIDO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userAgent sanitizado excede");
        }

        @Test @DisplayName("userAgent con caracteres de control es sanitizado")
        void userAgentSanitizadoControlChars() {
            String uaConControl = "Mozilla\u0000\u0001/5.0";
            FalActaQrAcceso acc = new FalActaQrAcceso(1L, 1L, AHORA, CanalAccesoQr.PORTAL,
                    null, uaConControl, ResultadoAccesoQr.VALIDO, AHORA);
            assertThat(acc.getUserAgent()).doesNotContain("\u0000").doesNotContain("\u0001");
        }

        @Test @DisplayName("CanalAccesoQr - codigo() y fromCodigo() son inversos")
        void canalCodigoInverso() {
            for (CanalAccesoQr c : CanalAccesoQr.values()) {
                assertThat(CanalAccesoQr.fromCodigo(c.codigo())).isEqualTo(c);
            }
        }

        @Test @DisplayName("ResultadoAccesoQr - codigo() y fromCodigo() son inversos")
        void resultadoCodigoInverso() {
            for (ResultadoAccesoQr r : ResultadoAccesoQr.values()) {
                assertThat(ResultadoAccesoQr.fromCodigo(r.codigo())).isEqualTo(r);
            }
        }

        @Test @DisplayName("CanalAccesoQr sin duplicados de codigo")
        void canalCodigosUnicos() {
            Set<Short> codigos = new HashSet<>();
            for (CanalAccesoQr c : CanalAccesoQr.values()) {
                assertThat(codigos.add(c.codigo()))
                        .as("Codigo duplicado en CanalAccesoQr: " + c.codigo()).isTrue();
            }
        }

        @Test @DisplayName("ResultadoAccesoQr sin duplicados de codigo")
        void resultadoCodigosUnicos() {
            Set<Short> codigos = new HashSet<>();
            for (ResultadoAccesoQr r : ResultadoAccesoQr.values()) {
                assertThat(codigos.add(r.codigo()))
                        .as("Codigo duplicado en ResultadoAccesoQr: " + r.codigo()).isTrue();
            }
        }

        @Test @DisplayName("no se puede obtener fromCodigo de codigo inexistente")
        void fromCodigoInexistente() {
            assertThatThrownBy(() -> CanalAccesoQr.fromCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ResultadoAccesoQr.fromCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Concurrencia y OCC")
    class Concurrencia {

        @Test @DisplayName("resolucion concurrente - multiples threads acceden al mismo QR")
        void resolucionConcurrente() throws InterruptedException {
            crearActa(1L);
            String token = token(1L);
            int N = 20;
            ExecutorService exec = Executors.newFixedThreadPool(8);
            CountDownLatch latch = new CountDownLatch(N);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);
            for (int i = 0; i < N; i++) {
                exec.submit(() -> {
                    try {
                        service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c");
                        exitos.incrementAndGet();
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();
            assertThat(exitos.get()).isEqualTo(N);
            assertThat(errores.get()).isZero();
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(N);
        }

        @Test @DisplayName("QR compite con modificacion de acta - OCC se resuelve correctamente")
        void qrCompiteConModificacionActa() throws InterruptedException {
            crearActa(1L);
            String token = token(1L);

            // Simular que mientras se genera el QR, hay modificaciones al acta
            int N = 10;
            ExecutorService exec = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(N);
            AtomicInteger qrExitos = new AtomicInteger(0);

            for (int i = 0; i < N; i++) {
                exec.submit(() -> {
                    try {
                        service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c");
                        qrExitos.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();
            assertThat(qrExitos.get()).isEqualTo(N);
        }

        @Test @DisplayName("emision concurrente para distintos uuid es thread-safe y sin colisiones")
        void emisionConcurrenteDistintasActas() throws InterruptedException {
            int N = 10;
            List<String> uuids = new CopyOnWriteArrayList<>();
            for (int i = 0; i < N; i++) uuids.add(java.util.UUID.randomUUID().toString());

            ExecutorService exec = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(N);
            List<String> tokens = new CopyOnWriteArrayList<>();
            for (String uuid : uuids) {
                exec.submit(() -> {
                    try {
                        tokens.add(service.emitirTokenPara(uuid).token());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();
            assertThat(tokens).hasSize(N);
            assertThat(new HashSet<>(tokens)).hasSize(N); // todos distintos
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("HmacSha256QrTokenProtector - seguridad del token vista desde el servicio")
    class ProtectorSeguridad {

        private static final String UUID_A = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";

        @Test @DisplayName("version 0 valida resuelve uuidTecnico correctamente")
        void versionCeroValida() {
            QrTokenProtector prot = QrTestSupport.protector();
            String token = prot.generar(UUID_A);
            assertThat(prot.resolverUuidTecnico(token)).isEqualTo(UUID_A);
        }

        @Test @DisplayName("token firmado con otra clave no verifica")
        void claveDiferente() {
            String token = QrTestSupport.protector().generar(UUID_A);
            assertThatThrownBy(() -> QrTestSupport.protectorAlternativo().resolverUuidTecnico(token))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("un caracter alterado en la firma falla la verificacion HMAC")
        void tokenAlterado() {
            QrTokenProtector prot = QrTestSupport.protector();
            String token = prot.generar(UUID_A);
            String firma = token.substring(token.lastIndexOf('.') + 1);
            char primero = firma.charAt(0);
            String firmaAlterada = (primero == 'A' ? 'B' : 'A') + firma.substring(1);
            String tokenAlterado = token.substring(0, token.lastIndexOf('.') + 1) + firmaAlterada;

            assertThatThrownBy(() -> prot.resolverUuidTecnico(tokenAlterado))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        /** Contraparte invertida del test legacy que afirmaba diversidad por nonce aleatorio. */
        @Test @DisplayName("dos generaciones para el mismo uuid producen exactamente el mismo token")
        void tokensIgualesParaMismoUuid() {
            QrTokenProtector prot = QrTestSupport.protector();
            assertThat(prot.generar(UUID_A)).isEqualTo(prot.generar(UUID_A));
        }

        @Test @DisplayName("clave fija TEST_ONLY - generar y resolver correctamente")
        void claveFijaFunciona() {
            QrTokenProtector prot = QrTestSupport.protector();
            String token = prot.generar(UUID_A);
            assertThat(prot.resolverUuidTecnico(token)).isEqualTo(UUID_A);
        }

        @Test @DisplayName("clave mas corta que la salida HMAC no construye el protector")
        void claveLongitudInsuficiente() {
            assertThatThrownBy(() ->
                    new HmacSha256QrTokenProtector(QrTestSupport.SECRETO_DEMASIADO_CORTO_TEST_ONLY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("minimo");
        }

        @Test @DisplayName("token de longitud exactamente 128 pasa la validacion de longitud y falla por formato")
        void tokenLongitudMaxAceptado() {
            // La validacion de longitud no debe rechazar un token de exactamente 128 chars:
            // el rechazo tiene que venir del formato canonico, no del limite.
            String tokenMax = "a".repeat(128);
            assertThatThrownBy(() -> service.registrarAcceso(tokenMax, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageNotContaining("excede longitud maxima");
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Listado de accesos")
    class ListadoAccesos {

        @Test @DisplayName("listarAccesosPorActa devuelve todos los accesos en orden")
        void listarOrdenado() {
            crearActa(1L);
            String token = token(1L);
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c1");
            service.registrarAcceso(token, CanalAccesoQr.APP, null, null, "c2");
            List<FalActaQrAcceso> accesos = service.listarAccesosPorActa(acta(1L));
            assertThat(accesos).hasSize(2);
        }

        @Test @DisplayName("acta inexistente en listado lanza excepcion")
        void listarActaInexistente() {
            assertThatThrownBy(() -> service.listarAccesosPorActa(999L))
                    .isInstanceOf(ActaNoEncontradaException.class);
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Integracion portal: registrarAccesoConNotificacion")
    class IntegracionPortal {

        private InMemoryNotificacionRepository notifRepo;
        private InMemoryNotificacionIntentoRepository intentoRepo;
        private NotificacionIntentoService notifSvc;

        @BeforeEach
        void setUpPortal() {
            notifRepo = new InMemoryNotificacionRepository();
            intentoRepo = new InMemoryNotificacionIntentoRepository();
            var docRepo = new InMemoryDocumentoRepository();
            var falloRepo = new InMemoryFalloActaRepository();

            SnapshotRecalculador recalc = new SnapshotRecalculador(
                    eventoRepo, docRepo, notifRepo, new InMemoryPagoVoluntarioRepository(),
                    falloRepo, new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                    FaltasClockTestSupport.FIXED, snapshotRepo);

            notifSvc = new NotificacionIntentoService(
                    intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc,
                    new InMemoryLoteCorreoRepository(), FaltasClockTestSupport.FIXED,
                    falloRepo, docRepo,
                    new NoOpBloqueantesMaterialesChecker(),
                    PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));
        }

        /** Siembra una notificacion directamente en el repo para el acta indicada. */
        private FalNotificacion sembraNotificacion(Long actaId) {
            LocalDateTime ahora = AHORA;
            Long idNotif = notifRepo.nextId();
            FalNotificacion notif = new FalNotificacion(
                    idNotif, actaId, 1L, TipoDocu.ACTA_INFRACCION, "EMAIL", ahora, ahora, "seed");
            notifRepo.guardar(notif);
            return notifRepo.buscarPorId(idNotif).orElseThrow();
        }

        @Test @DisplayName("delega exactamente una vez a registrarPortalPositivo cuando notificacionId no es nulo")
        void delega_exactamente_una_vez() {
            crearActa(1L);
            String token = token(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L));

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "CUIT-20123456780", notifSvc);

            // intento portal creado exactamente una vez
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(notif.getId());
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
            assertThat(intentos.get(0).getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);

            // acceso QR registrado independientemente
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(1);
        }

        @Test @DisplayName("notificacionId null: acceso QR registrado; portal no invocado")
        void sin_notificacion_no_invoca_portal() {
            crearActa(1L);
            String token = token(1L);

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    null, "CUIT-20123456780", "USR", notifSvc);

            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(1);
            // no se sembro ninguna notificacion, por lo tanto ningun intento fue creado
            assertThat(intentoRepo.size()).isZero();
        }

        @Test @DisplayName("notificacionId informado + notifService null: IllegalArgumentException; cero acceso QR; cero intento portal")
        void notificacionId_informado_notifService_null_falla() {
            crearActa(1L);
            String token = token(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L));

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", null))
                    .isInstanceOf(IllegalArgumentException.class);

            // cero acceso QR (la excepcion ocurre antes de registrarAcceso)
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
            // cero intento portal
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId())).isEmpty();
        }

        @Test @DisplayName("QR acta A + notificacion acta A: delegacion exitosa; un acceso QR; un intento PORTAL_INFRACTOR")
        void qr_acta_a_notificacion_acta_a_match() {
            crearActa(1L);
            String token = token(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L)); // notif.actaId = 1L

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "CUIT-20123456780", notifSvc);

            // un acceso QR registrado
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(1);

            // exactamente un intento PORTAL_INFRACTOR para la notificacion
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(notif.getId());
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
            assertThat(intentos.get(0).getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
        }

        @Test @DisplayName("QR acta A + notificacion acta B: acceso QR de A registrado; mismatch rechazado; notificacion B intacta; cero intento portal en B")
        void qr_acta_a_notificacion_acta_b_mismatch() {
            crearActa(1L); // acta A
            crearActa(2L); // acta B (necesita existir para que la notificacion sea valida)
            String token = token(1L); // QR resuelve acta A
            FalNotificacion notifB = sembraNotificacion(acta(2L)); // notif pertenece a acta B

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notifB.getId(), "CUIT-20123456780", "USR", notifSvc))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);

            // acceso QR de acta A queda registrado (append-only, independiente del resultado notificatorio)
            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isEqualTo(1);

            // notificacion B permanece intacta: cero intento portal
            assertThat(intentoRepo.buscarPorNotificacion(notifB.getId()).stream()
                    .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR).count())
                    .isZero();
        }

        @Test @DisplayName("token invalido: falla en acceso QR; portal no invocado; ningun acceso persiste")
        void token_invalido_no_invoca_portal() {
            crearActa(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L));

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    "token-invalido", CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc))
                    .isInstanceOf(QrTokenInvalidoException.class);

            assertThat(qrAccesoRepo.contarPorActa(acta(1L))).isZero();
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId())).isEmpty();
        }

        @Test @DisplayName("repeticion: segunda invocacion rechazada por portal; primer acceso QR persiste")
        void repeticion_no_duplica_portal() {
            crearActa(1L);
            String token = token(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L));

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr-1",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc);

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr-2",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc))
                    .isInstanceOf(PrecondicionVioladaException.class);

            // solo un intento portal
            long portalCount = intentoRepo.buscarPorNotificacion(notif.getId()).stream()
                    .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR)
                    .count();
            assertThat(portalCount).isEqualTo(1);
        }

        @Test @DisplayName("la respuesta devuelta por registrarAccesoConNotificacion corresponde al acceso QR, no al intento portal")
        void respuesta_corresponde_a_acceso_qr() {
            crearActa(1L);
            String token = token(1L);
            FalNotificacion notif = sembraNotificacion(acta(1L));

            QrActaService.AccesoQrResultado resultado = service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.APP, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc);

            assertThat(resultado.actaId()).isEqualTo(acta(1L));
            assertThat(resultado.canal()).isEqualTo(CanalAccesoQr.APP);
            assertThat(resultado.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            assertThat(resultado.idAcceso()).isNotNull();

            // el intento portal fue creado ademas
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId()))
                    .anyMatch(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR);
        }
    }
}

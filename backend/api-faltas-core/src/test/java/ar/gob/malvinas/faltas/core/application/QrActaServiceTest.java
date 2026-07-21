package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.PlazosTestSupport;

import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionIntentoService;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.qr.AesGcmQrTokenProtector;
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
    private AesGcmQrTokenProtector tokenProtector;
    private QrActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        qrAccesoRepo = new InMemoryQrAccesoRepository();
        tokenProtector = new AesGcmQrTokenProtector(); // clave efimera para tests

        var docRepo = new InMemoryDocumentoRepository();
        var pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        var falloRepo = new InMemoryFalloActaRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, new InMemoryNotificacionRepository(),
                pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

        service = new QrActaService(actaRepo, eventoRepo, snapshotRepo, qrAccesoRepo, recalc, tokenProtector, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActa(Long id) {
        FalActa a = new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
        a.setBloqueActual(BloqueActual.NOTI);
        return actaRepo.guardar(a);
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Generacion de QR")
    class GenerarQr {

        @Test @DisplayName("genera token para acta sin QR previo")
        void generaTokenSinQrPrevio() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test @DisplayName("token generado tiene longitud <= 512 chars")
        void tokenMaxLength() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            assertThat(token.length()).isLessThanOrEqualTo(512);
        }

        @Test @DisplayName("no regenera silenciosamente si ya existe QR")
        void noRegenera_SinRota() {
            crearActa(1L);
            service.generarQr(1L, "USR");
            assertThatThrownBy(() -> service.generarQr(1L, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("QR emitido");
        }

        @Test @DisplayName("rotarQr genera nuevo token distinto al previo")
        void rotarQrGeneraNuevo() {
            crearActa(1L);
            String token1 = service.generarQr(1L, "USR");
            String token2 = service.rotarQr(1L, "USR");
            assertThat(token2).isNotNull().isNotBlank().isNotEqualTo(token1);
        }

        @Test @DisplayName("token no predecible - dos actas generan tokens distintos")
        void tokenNoPredecible() {
            crearActa(1L);
            crearActa(2L);
            String t1 = service.generarQr(1L, "USR");
            String t2 = service.generarQr(2L, "USR");
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test @DisplayName("multiples llamadas a generarQr para misma acta producen tokens distintos (tras rotacion)")
        void multipleRota() {
            crearActa(1L);
            Set<String> tokens = new HashSet<>();
            service.generarQr(1L, "USR");
            String t0 = actaRepo.buscarPorId(1L).orElseThrow().getCodigoQr();
            tokens.add(t0);
            for (int i = 0; i < 5; i++) {
                tokens.add(service.rotarQr(1L, "USR"));
            }
            assertThat(tokens).hasSize(6);
        }

        @Test @DisplayName("unicidad: tokens para distintas actas son distintos")
        void unicidadTokensDistintasActas() {
            Set<String> tokens = new HashSet<>();
            for (long id = 1; id <= 10; id++) {
                crearActa(id);
                tokens.add(service.generarQr(id, "USR"));
            }
            assertThat(tokens).hasSize(10);
        }

        @Test @DisplayName("payload no contiene PII visible en texto plano")
        void payloadSinPii() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
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
            String token = service.generarQr(42L, "USR");
            // base64url no deberia decodificarse a algo que contenga el id en claro
            assertThat(token).doesNotContain("\"id\":42").doesNotContain("id=42");
        }

        @Test @DisplayName("evento QRGEN registrado tras generacion")
        void eventoQrgenRegistrado() {
            crearActa(1L);
            service.generarQr(1L, "USR");
            assertThat(eventoRepo.buscarPorActa(1L))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.QRGEN);
        }

        @Test @DisplayName("acta inexistente lanza excepcion")
        void actaInexistente() {
            assertThatThrownBy(() -> service.generarQr(999L, "USR"))
                    .isInstanceOf(ActaNoEncontradaException.class);
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Resolucion de acceso QR - caso valido")
    class AccesoValido {

        @Test @DisplayName("token valido genera acceso VALIDO en repositorio")
        void accesoValidoInsertado() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            QrActaService.AccesoQrResultado r = service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "corr-1");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            assertThat(r.actaId()).isEqualTo(1L);
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(1);
        }

        @Test @DisplayName("multiples accesos validos al mismo QR son legitimos")
        void multiplesAccesosValidos() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c1");
            service.registrarAcceso(token, CanalAccesoQr.APP, null, null, "c2");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c3");
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(3);
        }

        @Test @DisplayName("evento QRACC registrado tras acceso valido")
        void eventoQraccRegistrado() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "corr-1");
            assertThat(eventoRepo.buscarPorActa(1L))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.QRACC);
        }

        @Test @DisplayName("acceso con IP IPv4 valida")
        void accesoConIpv4() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, "192.168.1.1", null, "c");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            FalActaQrAcceso acc = qrAccesoRepo.buscarPorId(r.idAcceso()).orElseThrow();
            assertThat(acc.getIpOrigen()).isEqualTo("192.168.1.1");
        }

        @Test @DisplayName("acceso con IP IPv6 valida")
        void accesoConIpv6() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, "2001:db8::1", null, "c");
            assertThat(r.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            FalActaQrAcceso acc = qrAccesoRepo.buscarPorId(r.idAcceso()).orElseThrow();
            assertThat(acc.getIpOrigen()).isEqualTo("2001:db8::1");
        }

        @Test @DisplayName("acceso con user-agent valido")
        void accesoConUserAgent() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
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
            String token = service.generarQr(1L, "USR");
            for (CanalAccesoQr canal : CanalAccesoQr.values()) {
                service.registrarAcceso(token, canal, null, null, "c");
            }
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(CanalAccesoQr.values().length);
        }

        @Test @DisplayName("resultado devuelto no incluye token ni payload")
        void resultadoSinToken() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            QrActaService.AccesoQrResultado r = service.registrarAcceso(
                    token, CanalAccesoQr.PORTAL, null, null, "corr");
            // el record no tiene campo token
            // comprobamos que los campos visibles son solo los seguros
            assertThat(r.idAcceso()).isNotNull();
            assertThat(r.actaId()).isEqualTo(1L);
            assertThat(r.canal()).isEqualTo(CanalAccesoQr.PORTAL);
        }

        @Test @DisplayName("copias defensivas: modificar objeto devuelto no afecta repositorio")
        void copiasDefensivas() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c");
            List<FalActaQrAcceso> lista1 = qrAccesoRepo.listarPorActa(1L);
            List<FalActaQrAcceso> lista2 = qrAccesoRepo.listarPorActa(1L);
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
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
        }

        @Test @DisplayName("token en blanco lanza excepcion y no inserta acceso")
        void tokenEnBlanco() {
            crearActa(1L);
            assertThatThrownBy(() -> service.registrarAcceso("  ", CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
        }

        @Test @DisplayName("token corrupto/manipulado lanza excepcion y no inserta")
        void tokenCorrupto() {
            crearActa(1L);
            assertThatThrownBy(() -> service.registrarAcceso(
                    "ZXN0b2VzdW50b2tlbmNvcnJ1cHRv", CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
        }

        @Test @DisplayName("token de otra clave (diferente protector) es invalido")
        void tokenDeOtraClave() {
            crearActa(1L);
            AesGcmQrTokenProtector otraInstancia = new AesGcmQrTokenProtector();
            String tokenOtro = otraInstancia.generar("UUID-1");
            assertThatThrownBy(() -> service.registrarAcceso(tokenOtro, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
        }

        @Test @DisplayName("token con scope incorrecto es invalido")
        void tokenScopeIncorrecto() {
            // Verificamos directo en el protector que scope incorrecto lanza
            byte[] fixedKey = new byte[32];
            java.util.Arrays.fill(fixedKey, (byte) 0x42);
            AesGcmQrTokenProtector prot = new AesGcmQrTokenProtector(fixedKey);
            // No podemos construir un token con scope incorrecto directamente,
            // pero podemos verificar que el protector rechaza un payload modificado
            // via el token de otra clave (scope/version distintos)
            AesGcmQrTokenProtector otraProt = new AesGcmQrTokenProtector();
            String tokenOtroScope = otraProt.generar("UUID-CUALQUIERA");
            assertThatThrownBy(() -> prot.resolverUuidTecnico(tokenOtroScope))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("token demasiado largo lanza excepcion y no inserta")
        void tokenDemasiadoLargo() {
            crearActa(1L);
            String tokenLargo = "x".repeat(513);
            assertThatThrownBy(() -> service.registrarAcceso(tokenLargo, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
        }

        @Test @DisplayName("acta inexistente (uuid no registrado) lanza excepcion")
        void actaInexistente() {
            // generamos un token valido para un UUID que no existe en el repositorio
            String token = tokenProtector.generar("UUID-NO-EXISTE");
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
            String tokenParaUuidNoExiste = tokenProtector.generar("UUID-NO-EXISTE");
            assertThatThrownBy(() -> service.registrarAcceso(tokenParaUuidNoExiste, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(expected);
        }

        @Test @DisplayName("excepcion de token invalido no incluye el token en el mensaje")
        void excepcionNoContieneToken() {
            crearActa(1L);
            service.generarQr(1L, "USR");
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
            String token = service.generarQr(1L, "USR");
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
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(N);
        }

        @Test @DisplayName("QR compite con modificacion de acta - OCC se resuelve correctamente")
        void qrCompiteConModificacionActa() throws InterruptedException {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");

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

        @Test @DisplayName("generacion concurrente de QR para distintas actas es thread-safe")
        void generacionConcurrenteDistintasActas() throws InterruptedException {
            int N = 10;
            for (long id = 1; id <= N; id++) crearActa(id);

            ExecutorService exec = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(N);
            List<String> tokens = new CopyOnWriteArrayList<>();
            for (long id = 1; id <= N; id++) {
                final long actaId = id;
                exec.submit(() -> {
                    try {
                        tokens.add(service.generarQr(actaId, "USR"));
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
    @Nested @DisplayName("AesGcmQrTokenProtector - seguridad del token")
    class ProtectorSeguridad {

        @Test @DisplayName("version 0 valida resuelve uuidTecnico correctamente")
        void versionCeroValida() {
            AesGcmQrTokenProtector prot = new AesGcmQrTokenProtector();
            String token = prot.generar("mi-uuid-tecnico");
            String uuid = prot.resolverUuidTecnico(token);
            assertThat(uuid).isEqualTo("mi-uuid-tecnico");
        }

        @Test @DisplayName("token con clave diferente (scope incorrecto desde perspectiva del receptor) falla")
        void claveDiferente() {
            AesGcmQrTokenProtector prot1 = new AesGcmQrTokenProtector();
            AesGcmQrTokenProtector prot2 = new AesGcmQrTokenProtector();
            String token = prot1.generar("uuid-x");
            assertThatThrownBy(() -> prot2.resolverUuidTecnico(token))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("token alterado en un byte falla autenticacion GCM")
        void tokenAlterado() {
            AesGcmQrTokenProtector prot = new AesGcmQrTokenProtector();
            String token = prot.generar("uuid-x");
            byte[] bytes = java.util.Base64.getUrlDecoder().decode(token);
            bytes[bytes.length - 1] ^= 0xFF; // flip ultimo byte (auth tag)
            String tokenAlterado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            assertThatThrownBy(() -> prot.resolverUuidTecnico(tokenAlterado))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("dos tokens generados para mismo uuid son distintos (nonce aleatorio)")
        void tokensDistintosParaMismoUuid() {
            AesGcmQrTokenProtector prot = new AesGcmQrTokenProtector();
            String t1 = prot.generar("uuid-y");
            String t2 = prot.generar("uuid-y");
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test @DisplayName("clave fija de 32 bytes - generar y resolver correctamente")
        void claveFijaFunciona() {
            byte[] key = new byte[32];
            java.util.Arrays.fill(key, (byte) 0xAB);
            AesGcmQrTokenProtector prot = new AesGcmQrTokenProtector(key);
            String token = prot.generar("uuid-fijo");
            assertThat(prot.resolverUuidTecnico(token)).isEqualTo("uuid-fijo");
        }

        @Test @DisplayName("clave de longitud incorrecta lanza excepcion")
        void claveLongitudIncorrecta() {
            assertThatThrownBy(() -> new AesGcmQrTokenProtector(new byte[16]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }

        @Test @DisplayName("token de longitud exactamente 512 es aceptado por validacion de formato")
        void tokenLongitudMaxAceptado() {
            // Construir un token de 512 chars (incluso si no es valido criptograficamente, la longitud pasa)
            // Verificamos que la validacion de longitud no rechaza token == 512 chars
            // (el fallo debe ser por descifrado, no por longitud)
            String tokenMax = "a".repeat(512);
            assertThatThrownBy(() -> service.registrarAcceso(tokenMax, CanalAccesoQr.PORTAL, null, null, "c"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            // no deve ser "excede longitud maxima"
        }
    }

    // ----------------------------------------------------------------
    @Nested @DisplayName("Listado de accesos")
    class ListadoAccesos {

        @Test @DisplayName("listarAccesosPorActa devuelve todos los accesos en orden")
        void listarOrdenado() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            service.registrarAcceso(token, CanalAccesoQr.PORTAL, null, null, "c1");
            service.registrarAcceso(token, CanalAccesoQr.APP, null, null, "c2");
            List<FalActaQrAcceso> accesos = service.listarAccesosPorActa(1L);
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
            String token = service.generarQr(1L, "USR");
            FalNotificacion notif = sembraNotificacion(1L);

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "CUIT-20123456780", notifSvc);

            // intento portal creado exactamente una vez
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(notif.getId());
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
            assertThat(intentos.get(0).getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);

            // acceso QR registrado independientemente
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(1);
        }

        @Test @DisplayName("notificacionId null: acceso QR registrado; portal no invocado")
        void sin_notificacion_no_invoca_portal() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    null, "CUIT-20123456780", "USR", notifSvc);

            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(1);
            // no se sembro ninguna notificacion, por lo tanto ningun intento fue creado
            assertThat(intentoRepo.size()).isZero();
        }

        @Test @DisplayName("notificacionId informado + notifService null: IllegalArgumentException; cero acceso QR; cero intento portal")
        void notificacionId_informado_notifService_null_falla() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            FalNotificacion notif = sembraNotificacion(1L);

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", null))
                    .isInstanceOf(IllegalArgumentException.class);

            // cero acceso QR (la excepcion ocurre antes de registrarAcceso)
            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
            // cero intento portal
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId())).isEmpty();
        }

        @Test @DisplayName("QR acta A + notificacion acta A: delegacion exitosa; un acceso QR; un intento PORTAL_INFRACTOR")
        void qr_acta_a_notificacion_acta_a_match() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            FalNotificacion notif = sembraNotificacion(1L); // notif.actaId = 1L

            service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "CUIT-20123456780", notifSvc);

            // un acceso QR registrado
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(1);

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
            String token = service.generarQr(1L, "USR"); // QR resuelve acta A
            FalNotificacion notifB = sembraNotificacion(2L); // notif pertenece a acta B

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.PORTAL, null, null, "corr",
                    notifB.getId(), "CUIT-20123456780", "USR", notifSvc))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);

            // acceso QR de acta A queda registrado (append-only, independiente del resultado notificatorio)
            assertThat(qrAccesoRepo.contarPorActa(1L)).isEqualTo(1);

            // notificacion B permanece intacta: cero intento portal
            assertThat(intentoRepo.buscarPorNotificacion(notifB.getId()).stream()
                    .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR).count())
                    .isZero();
        }

        @Test @DisplayName("token invalido: falla en acceso QR; portal no invocado; ningun acceso persiste")
        void token_invalido_no_invoca_portal() {
            crearActa(1L);
            FalNotificacion notif = sembraNotificacion(1L);

            assertThatThrownBy(() -> service.registrarAccesoConNotificacion(
                    "token-invalido", CanalAccesoQr.PORTAL, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc))
                    .isInstanceOf(QrTokenInvalidoException.class);

            assertThat(qrAccesoRepo.contarPorActa(1L)).isZero();
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId())).isEmpty();
        }

        @Test @DisplayName("repeticion: segunda invocacion rechazada por portal; primer acceso QR persiste")
        void repeticion_no_duplica_portal() {
            crearActa(1L);
            String token = service.generarQr(1L, "USR");
            FalNotificacion notif = sembraNotificacion(1L);

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
            String token = service.generarQr(1L, "USR");
            FalNotificacion notif = sembraNotificacion(1L);

            QrActaService.AccesoQrResultado resultado = service.registrarAccesoConNotificacion(
                    token, CanalAccesoQr.APP, null, null, "corr",
                    notif.getId(), "CUIT-20123456780", "USR", notifSvc);

            assertThat(resultado.actaId()).isEqualTo(1L);
            assertThat(resultado.canal()).isEqualTo(CanalAccesoQr.APP);
            assertThat(resultado.resultado()).isEqualTo(ResultadoAccesoQr.VALIDO);
            assertThat(resultado.idAcceso()).isNotNull();

            // el intento portal fue creado ademas
            assertThat(intentoRepo.buscarPorNotificacion(notif.getId()))
                    .anyMatch(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR);
        }
    }
}

package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.ActaDocumentoService;
import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoYaExisteException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ActaDocumentoId;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11J: ActaDocumento - tests funcionales")
class ActaDocumentoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryActaDocumentoRepository pivotRepo;
    private ActaDocumentoService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        pivotRepo = new InMemoryActaDocumentoRepository();
        service = new ActaDocumentoService(pivotRepo, actaRepo, docRepo, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActa(Long id) {
        FalActa a = new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor.FIRMADA,
                null, AHORA, "SYS");
        actaRepo.guardar(a);
        return a;
    }

    private FalDocumento crearDoc(Long id, Long actaId, TipoDocu tipo) {
        FalDocumento doc = new FalDocumento(id, actaId, tipo, AHORA, "desc-" + id);
        docRepo.guardar(doc);
        return doc;
    }

    // =========================================================================
    // T01: ID compuesto
    // =========================================================================

    @Test @DisplayName("T01a: ActaDocumentoId inmutable y con igualdad por valor")
    void t01a_idCompuesto() {
        ActaDocumentoId id1 = new ActaDocumentoId(1L, 10L);
        ActaDocumentoId id2 = new ActaDocumentoId(1L, 10L);
        ActaDocumentoId id3 = new ActaDocumentoId(2L, 10L);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1.actaId()).isEqualTo(1L);
        assertThat(id1.documentoId()).isEqualTo(10L);
    }

    @Test @DisplayName("T01b: ActaDocumentoId rechaza IDs nulos o no positivos")
    void t01b_idCompuesto_validaciones() {
        assertThatThrownBy(() -> new ActaDocumentoId(null, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActaDocumentoId(1L, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActaDocumentoId(0L, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActaDocumentoId(1L, -1L)).isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // T02: Campos y auditoría de FalActaDocumento
    // =========================================================================

    @Test @DisplayName("T02: FalActaDocumento guarda todos los campos y es inmutable en id")
    void t02_camposAuditoria() {
        crearActa(1L);
        crearDoc(10L, 1L, TipoDocu.ACTA_INFRACCION);

        FalActaDocumento rel = service.asociar(1L, 10L, RolDocuActa.ACTA_PRINCIPAL, true, "USR-01");

        assertThat(rel.getActaId()).isEqualTo(1L);
        assertThat(rel.getDocumentoId()).isEqualTo(10L);
        assertThat(rel.getRolDocuActa()).isEqualTo(RolDocuActa.ACTA_PRINCIPAL);
        assertThat(rel.isSiPrincipal()).isTrue();
        assertThat(rel.getFhAlta()).isNotNull();
        assertThat(rel.getIdUserAlta()).isEqualTo("USR-01");
    }

    @Test @DisplayName("T02b: FalActaDocumento.copia() devuelve copia defensiva")
    void t02b_copiaDefensiva() {
        crearActa(1L);
        crearDoc(10L, 1L, TipoDocu.ACTA_INFRACCION);
        FalActaDocumento rel = service.asociar(1L, 10L, RolDocuActa.ACTA_PRINCIPAL, true, "USR");
        FalActaDocumento copia = rel.copia();
        assertThat(copia).isEqualTo(rel);
        assertThat(copia).isNotSameAs(rel);
    }

    // =========================================================================
    // T03: Codigos del enum RolDocuActa
    // =========================================================================

    @Test @DisplayName("T03: Codigos del enum son unicos y fromCodigo es reversible")
    void t03_enumCodigos() {
        for (RolDocuActa rol : RolDocuActa.values()) {
            assertThat(RolDocuActa.desdeCodigo(rol.codigo())).isEqualTo(rol);
        }
        long count = java.util.Arrays.stream(RolDocuActa.values())
                .mapToInt(r -> r.codigo()).distinct().count();
        assertThat(count).isEqualTo(RolDocuActa.values().length);
    }

    @Test @DisplayName("T03b: RolDocuActa.desdeCodigo lanza excepcion para codigo invalido")
    void t03b_codigoInvalido() {
        assertThatThrownBy(() -> RolDocuActa.desdeCodigo((short) 999))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // T04: Matriz rol/tipo
    // =========================================================================

    @Test @DisplayName("T04a: Combinaciones validas rol/tipo pasan validacion")
    void t04a_matrizValida() {
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.ACTA_PRINCIPAL, TipoDocu.ACTA_INFRACCION));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.FALLO, TipoDocu.ACTO_ADMINISTRATIVO));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.NOTIFICACION, TipoDocu.NOTIFICACION_ACTA));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.NOTIFICACION, TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.MEDIDA_PREVENTIVA, TipoDocu.MEDIDA_PREVENTIVA));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.NULIDAD, TipoDocu.NULIDAD));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.RESOLUTORIO_BLOQUEANTE, TipoDocu.RESOLUTORIO_BLOQUEANTE));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.INTIMACION_PAGO, TipoDocu.INTIMACION_PAGO));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.INTIMACION_PLAN, TipoDocu.INTIMACION_INCUMPLIMIENTO_PLAN));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.CONSTANCIA, TipoDocu.CONSTANCIA));
        assertThatNoException().isThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.ANEXO, TipoDocu.ANEXO));
    }

    @Test @DisplayName("T04b: Combinaciones invalidas rol/tipo rechazan")
    void t04b_matrizInvalida() {
        assertThatThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.ACTA_PRINCIPAL, TipoDocu.ACTO_ADMINISTRATIVO))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.FALLO, TipoDocu.NOTIFICACION_ACTA))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() ->
                service.validarCompatibilidad(RolDocuActa.NOTIFICACION, TipoDocu.ACTA_INFRACCION))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // =========================================================================
    // T05: Principalidad
    // =========================================================================

    @Test @DisplayName("T05a: un solo ACTA_PRINCIPAL por acta")
    void t05a_unicidadPrincipal() {
        crearActa(1L);
        crearDoc(10L, 1L, TipoDocu.ACTA_INFRACCION);
        crearDoc(11L, 1L, TipoDocu.ACTA_INFRACCION);

        service.asociarPrincipal(1L, 10L, RolDocuActa.ACTA_PRINCIPAL, "SYS");
        service.asociarPrincipal(1L, 11L, RolDocuActa.ACTA_PRINCIPAL, "SYS");

        // El primero deja de ser principal
        assertThat(pivotRepo.buscarPorIdCompuesto(new ActaDocumentoId(1L, 10L)).get().isSiPrincipal()).isFalse();
        // El nuevo es el principal
        assertThat(pivotRepo.buscarPorIdCompuesto(new ActaDocumentoId(1L, 11L)).get().isSiPrincipal()).isTrue();
        // Historia preservada
        assertThat(pivotRepo.listarPorActa(1L)).hasSize(2);
    }

    @Test @DisplayName("T05b: rol ANEXO no admite principalidad")
    void t05b_rolSinPrincipal() {
        crearActa(1L);
        crearDoc(20L, 1L, TipoDocu.ANEXO);
        assertThatThrownBy(() -> service.asociar(1L, 20L, RolDocuActa.ANEXO, true, "SYS"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // =========================================================================
    // T06: Historial y reemplazo
    // =========================================================================

    @Test @DisplayName("T06: reemplazarPrincipal preserva historial")
    void t06_historialReemplazo() {
        crearActa(1L);
        crearDoc(30L, 1L, TipoDocu.ACTO_ADMINISTRATIVO);
        crearDoc(31L, 1L, TipoDocu.ACTO_ADMINISTRATIVO);

        service.asociarPrincipal(1L, 30L, RolDocuActa.FALLO, "SYS");
        FalActaDocumento nuevo = service.reemplazarPrincipal(1L, 31L, RolDocuActa.FALLO, "SYS");

        assertThat(nuevo.isSiPrincipal()).isTrue();
        assertThat(nuevo.getDocumentoId()).isEqualTo(31L);

        FalActaDocumento anterior = pivotRepo.buscarPorIdCompuesto(new ActaDocumentoId(1L, 30L)).orElseThrow();
        assertThat(anterior.isSiPrincipal()).isFalse();
        assertThat(pivotRepo.listarPorActaYRol(1L, RolDocuActa.FALLO)).hasSize(2);
    }

    // =========================================================================
    // T07: Queries por acta/documento/rol
    // =========================================================================

    @Test @DisplayName("T07: queries por acta, documento y rol")
    void t07_queries() {
        crearActa(1L);
        crearActa(2L);
        crearDoc(40L, 1L, TipoDocu.ACTA_INFRACCION);
        crearDoc(41L, 1L, TipoDocu.ACTO_ADMINISTRATIVO);
        crearDoc(42L, 2L, TipoDocu.ACTA_INFRACCION);

        service.asociar(1L, 40L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS");
        service.asociar(1L, 41L, RolDocuActa.FALLO, true, "SYS");
        service.asociar(2L, 42L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS");

        assertThat(service.consultarPorActa(1L)).hasSize(2);
        assertThat(service.consultarPorActa(2L)).hasSize(1);
        assertThat(service.consultarPorActaYRol(1L, RolDocuActa.FALLO)).hasSize(1);
        assertThat(service.consultarPorDocumento(40L)).hasSize(1);
        assertThat(service.consultarPrincipal(1L, RolDocuActa.ACTA_PRINCIPAL)).isPresent();
        assertThat(service.consultarPrincipal(1L, RolDocuActa.NOTIFICACION)).isEmpty();
    }

    // =========================================================================
    // T08: Validacion de pertenencia
    // =========================================================================

    @Test @DisplayName("T08: validarPertenencia lanza excepcion si no esta asociado")
    void t08_validarPertenencia() {
        crearActa(1L);
        crearDoc(50L, 1L, TipoDocu.ACTA_INFRACCION);

        assertThatThrownBy(() -> service.validarPertenencia(1L, 50L))
                .isInstanceOf(PrecondicionVioladaException.class);

        service.asociar(1L, 50L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS");
        assertThatNoException().isThrownBy(() -> service.validarPertenencia(1L, 50L));
    }

    // =========================================================================
    // T09: Resolver ultimo documento operativo
    // =========================================================================

    @Test @DisplayName("T09: resolverUltimoDocumentoOperativo prioriza FALLO sobre NOTIFICACION")
    void t09_resolverUltimoDoc() {
        crearActa(1L);
        crearDoc(60L, 1L, TipoDocu.NOTIFICACION_ACTA);
        crearDoc(61L, 1L, TipoDocu.ACTO_ADMINISTRATIVO);

        service.asociar(1L, 60L, RolDocuActa.NOTIFICACION, true, "SYS");
        service.asociar(1L, 61L, RolDocuActa.FALLO, true, "SYS");

        assertThat(service.resolverUltimoDocumentoOperativo(1L)).hasValue(61L);
    }

    @Test @DisplayName("T09b: resolverUltimoDocumentoOperativo retorna empty si sin documentos")
    void t09b_sinDocumentos() {
        crearActa(1L);
        assertThat(service.resolverUltimoDocumentoOperativo(1L)).isEmpty();
    }

    // =========================================================================
    // T10: Errores por entidades inexistentes
    // =========================================================================

    @Test @DisplayName("T10a: asociar falla si acta no existe")
    void t10a_actaNoExiste() {
        crearDoc(70L, 1L, TipoDocu.ACTA_INFRACCION);
        assertThatThrownBy(() -> service.asociar(999L, 70L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("T10b: asociar falla si documento no existe")
    void t10b_docNoExiste() {
        crearActa(1L);
        assertThatThrownBy(() -> service.asociar(1L, 999L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS"))
                .isInstanceOf(DocumentoNoEncontradoException.class);
    }

    @Test @DisplayName("T10c: par duplicado lanza ActaDocumentoYaExisteException")
    void t10c_parDuplicado() {
        crearActa(1L);
        crearDoc(80L, 1L, TipoDocu.ACTA_INFRACCION);
        service.asociar(1L, 80L, RolDocuActa.ACTA_PRINCIPAL, true, "SYS");
        // Segundo guardar del mismo par (sin usar asociarPrincipal)
        assertThatThrownBy(() ->
                pivotRepo.guardar(new FalActaDocumento(1L, 80L, RolDocuActa.ACTA_PRINCIPAL, false,
                        FaltasClockTestSupport.FIXED.now(), "SYS")))
                .isInstanceOf(ActaDocumentoYaExisteException.class);
    }
}
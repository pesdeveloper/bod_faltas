package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.PersonaDomicilioService;
import ar.gob.malvinas.faltas.core.application.service.PersonaService;
import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.exception.DomicilioPersonaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Slice 8F-11C: FalPersonaDomicilio, PersonaDomicilioService, InMemoryPersonaDomicilioRepository")
class FalPersonaDomicilioTest {

    private InMemoryPersonaRepository personaRepo;
    private InMemoryPersonaDomicilioRepository domRepo;
    private PersonaDomicilioService service;
    private Long personaId;

    private static final Short PROV_MALVINAS = (short) 6;
    private static final Integer UT_MALVINAS = 60515;

    @BeforeEach
    void setUp() {
        personaRepo = new InMemoryPersonaRepository();
        domRepo = new InMemoryPersonaDomicilioRepository();
        PersonaService personaService = new PersonaService(personaRepo, FaltasClockTestSupport.FIXED);

        service = new PersonaDomicilioService(domRepo, personaRepo,
                domicilioId -> false, FaltasClockTestSupport.FIXED);

        FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                "TestPersona", null, null, null, null, null, "SYS");
        personaId = persona.getId();
    }

    private FalPersonaDomicilio crearMalvinasLocal(boolean siPrincipal) {
        return service.crear(personaId, null,
                TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                true, true, siPrincipal,
                PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                null, null,
                "0750100001", 1L, "12345", 2L,
                "Av. Pioneros 100", null, true, null, null,
                "Av. Pioneros 100, Malvinas Argentinas", false,
                null, null, null, "SYS");
    }

    private FalPersonaDomicilio crearExterno(boolean siPrincipal) {
        return service.crear(personaId, null,
                TipoDomicilio.REAL, OrigenDomicilio.DDJJ, ModoDomicilio.EXTERNO,
                true, false, siPrincipal,
                (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                1001L, 2001L,
                null, null, null, null,
                "Rivadavia 500", 500, false, null, "1002",
                "Rivadavia 500, San Martin", false,
                null, null, null, "SYS");
    }

    // =========================================================================
    // Enums y codigos
    // =========================================================================

    @Nested
    @DisplayName("Enums domicilio")
    class EnumsDomicilio {

        @Test
        @DisplayName("TipoDomicilio: 6 valores codigos 1-6")
        void tipoDomicilio_codigos() {
            assertThat(TipoDomicilio.REAL.codigo()).isEqualTo((short) 1);
            assertThat(TipoDomicilio.LEGAL.codigo()).isEqualTo((short) 2);
            assertThat(TipoDomicilio.FISCAL.codigo()).isEqualTo((short) 3);
            assertThat(TipoDomicilio.CONSTITUIDO.codigo()).isEqualTo((short) 4);
            assertThat(TipoDomicilio.HALLADO.codigo()).isEqualTo((short) 5);
            assertThat(TipoDomicilio.OTRO.codigo()).isEqualTo((short) 6);
        }

        @Test
        @DisplayName("TipoDomicilio.fromCodigo resuelve todos los valores")
        void tipoDomicilio_fromCodigo() {
            for (TipoDomicilio v : TipoDomicilio.values()) {
                assertThat(TipoDomicilio.fromCodigo(v.codigo())).isEqualTo(v);
            }
        }

        @Test
        @DisplayName("OrigenDomicilio: 7 valores codigos 1-7")
        void origenDomicilio_codigos() {
            assertThat(OrigenDomicilio.LABRADO.codigo()).isEqualTo((short) 1);
            assertThat(OrigenDomicilio.INVESTIGACION.codigo()).isEqualTo((short) 2);
            assertThat(OrigenDomicilio.DDJJ.codigo()).isEqualTo((short) 3);
            assertThat(OrigenDomicilio.REINTENTO.codigo()).isEqualTo((short) 4);
            assertThat(OrigenDomicilio.PORTAL.codigo()).isEqualTo((short) 5);
            assertThat(OrigenDomicilio.EXTERNO.codigo()).isEqualTo((short) 6);
            assertThat(OrigenDomicilio.OPERADOR.codigo()).isEqualTo((short) 7);
        }

        @Test
        @DisplayName("ModoDomicilio: MALVINAS_LOCAL=1, EXTERNO=2")
        void modoDomicilio_codigos() {
            assertThat(ModoDomicilio.MALVINAS_LOCAL.codigo()).isEqualTo((short) 1);
            assertThat(ModoDomicilio.EXTERNO.codigo()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("UnidadTerritorialTipo: MUNICIPIO=1, DEPARTAMENTO=2, CIUDAD_AUTONOMA=3")
        void unidadTerritorialTipo_codigos() {
            assertThat(UnidadTerritorialTipo.MUNICIPIO.codigo()).isEqualTo((short) 1);
            assertThat(UnidadTerritorialTipo.DEPARTAMENTO.codigo()).isEqualTo((short) 2);
            assertThat(UnidadTerritorialTipo.CIUDAD_AUTONOMA.codigo()).isEqualTo((short) 3);
        }

        @Test
        @DisplayName("OrigenUbicacion tiene SIN_UBICACION=0 y demas codigos")
        void origenUbicacion_codigos() {
            assertThat(OrigenUbicacion.SIN_UBICACION.codigo()).isEqualTo((short) 0);
            assertThat(OrigenUbicacion.CALLE_ALTURA_GEOCODIFICADA.codigo()).isEqualTo((short) 1);
            assertThat(OrigenUbicacion.GPS_DISPOSITIVO.codigo()).isEqualTo((short) 6);
        }
    }

    // =========================================================================
    // Domicilio MALVINAS_LOCAL valido
    // =========================================================================

    @Nested
    @DisplayName("Domicilio MALVINAS_LOCAL")
    class DomicilioMalvinasLocal {

        @Test
        @DisplayName("Crea domicilio MALVINAS_LOCAL valido")
        void crear_local_valido() {
            FalPersonaDomicilio d = crearMalvinasLocal(true);
            assertThat(d.getId()).isNotNull();
            assertThat(d.getModoDomicilio()).isEqualTo(ModoDomicilio.MALVINAS_LOCAL);
            assertThat(d.getIdProvincia()).isEqualTo(PROV_MALVINAS);
            assertThat(d.getIdUnidadTerritorial()).isEqualTo(UT_MALVINAS);
            assertThat(d.getUnidadTerritorialTipo()).isEqualTo(UnidadTerritorialTipo.MUNICIPIO);
        }

        @Test
        @DisplayName("MALVINAS_LOCAL requiere idProvincia=6")
        void local_requiere_prov6() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                            null, null, "0750100001", 1L, "12345", 2L,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idProvincia");
        }

        @Test
        @DisplayName("MALVINAS_LOCAL requiere unidadTerritorialTipo=MUNICIPIO")
        void local_requiere_municipio() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                            true, false, false,
                            PROV_MALVINAS, UnidadTerritorialTipo.DEPARTAMENTO, UT_MALVINAS,
                            null, null, "0750100001", 1L, "12345", 2L,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MUNICIPIO");
        }

        @Test
        @DisplayName("MALVINAS_LOCAL requiere idUnidadTerritorial=60515")
        void local_requiere_ut60515() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                            true, false, false,
                            PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, 99999,
                            null, null, "0750100001", 1L, "12345", 2L,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("60515");
        }

        @Test
        @DisplayName("MALVINAS_LOCAL no admite idLocalidad externo")
        void local_no_admite_idLocalidad() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                            true, false, false,
                            PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                            1001L, null, "0750100001", 1L, "12345", 2L,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idLocalidad");
        }

        @Test
        @DisplayName("idLocMalvinas conserva ceros iniciales como String")
        void idLocMalvinas_conserva_ceros() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            assertThat(d.getIdLocMalvinas()).isEqualTo("0750100001");
        }

        @Test
        @DisplayName("idTcaMalvinas conserva texto como String")
        void idTcaMalvinas_como_string() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            assertThat(d.getIdTcaMalvinas()).isEqualTo("12345");
        }
    }

    // =========================================================================
    // Domicilio EXTERNO valido
    // =========================================================================

    @Nested
    @DisplayName("Domicilio EXTERNO")
    class DomicilioExterno {

        @Test
        @DisplayName("Crea domicilio EXTERNO valido")
        void crear_externo_valido() {
            FalPersonaDomicilio d = crearExterno(false);
            assertThat(d.getModoDomicilio()).isEqualTo(ModoDomicilio.EXTERNO);
            assertThat(d.getIdLocalidad()).isEqualTo(1001L);
            assertThat(d.getIdCalle()).isEqualTo(2001L);
            assertThat(d.getIdLocMalvinas()).isNull();
            assertThat(d.getIdTcaMalvinas()).isNull();
        }

        @Test
        @DisplayName("EXTERNO no admite idLocMalvinas")
        void externo_no_admite_idLocMalvinas() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.DDJJ, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            "0750100001", null, null, null,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("locales Malvinas");
        }

        @Test
        @DisplayName("EXTERNO no admite idTcaMalvinas")
        void externo_no_admite_idTcaMalvinas() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.DDJJ, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            null, null, "12345", null,
                            null, null, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("locales Malvinas");
        }
    }

    // =========================================================================
    // Regla altura
    // =========================================================================

    @Nested
    @DisplayName("Regla altura")
    class ReglaAltura {

        @Test
        @DisplayName("siSinAltura=true exige altura=null")
        void sinAltura_true_altura_null() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            null, null, null, null,
                            null, 100, true, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("altura");
        }

        @Test
        @DisplayName("altura negativa rechazada")
        void altura_negativa_rechazada() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            null, null, null, null,
                            null, -5, false, null, null, null, false,
                            null, null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negativa");
        }
    }

    // =========================================================================
    // Regla coordenadas
    // =========================================================================

    @Nested
    @DisplayName("Regla coordenadas")
    class ReglaCoordenadas {

        @Test
        @DisplayName("lat y lon en pareja")
        void lat_lon_en_pareja() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            null, null, null, null,
                            null, null, true, null, null, null, false,
                            BigDecimal.valueOf(-34.5), null, null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("juntos");
        }

        @Test
        @DisplayName("Coordenadas exigen origenUbicacion valido distinto de SIN_UBICACION")
        void coordenadas_exigen_origen() {
            assertThatThrownBy(() ->
                    service.crear(personaId, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                            true, false, false,
                            (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                            1001L, 2001L,
                            null, null, null, null,
                            null, null, true, null, null, null, false,
                            BigDecimal.valueOf(-34.5), BigDecimal.valueOf(-58.5), null, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("origenUbicacion");
        }

        @Test
        @DisplayName("Domicilio externo con coordenadas y origen valido")
        void coordenadas_con_origen_ok() {
            FalPersonaDomicilio d = service.crear(personaId, null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                    true, false, false,
                    (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                    1001L, 2001L,
                    null, null, null, null,
                    null, null, true, null, null, null, false,
                    BigDecimal.valueOf(-34.5), BigDecimal.valueOf(-58.5),
                    OrigenUbicacion.PUNTO_MANUAL, "SYS");
            assertThat(d.tieneCoordenadas()).isTrue();
            assertThat(d.getOrigenUbicacion()).isEqualTo(OrigenUbicacion.PUNTO_MANUAL);
        }
    }

    // =========================================================================
    // Principalidad unica
    // =========================================================================

    @Nested
    @DisplayName("Principalidad unica")
    class PrincipalidadUnica {

        @Test
        @DisplayName("Solo un principal activo por persona y tipo")
        void principal_unico_por_tipo() {
            FalPersonaDomicilio d1 = crearMalvinasLocal(true);
            assertThat(d1.isSiPrincipal()).isTrue();

            FalPersonaDomicilio d2 = service.crear(personaId, null,
                    TipoDomicilio.REAL, OrigenDomicilio.INVESTIGACION, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, false,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100002", 1L, "12346", 2L,
                    "Bolivar 50", null, true, null, null,
                    "Bolivar 50, Malvinas Argentinas", false,
                    null, null, null, "SYS");

            domRepo.marcarPrincipal(d2.getId());

            Optional<FalPersonaDomicilio> principalOpt = domRepo.buscarPrincipalActivo(personaId, TipoDomicilio.REAL);
            assertThat(principalOpt).isPresent();
            assertThat(principalOpt.get().getId()).isEqualTo(d2.getId());

            FalPersonaDomicilio d1Actual = domRepo.buscarPorId(d1.getId()).get();
            assertThat(d1Actual.isSiPrincipal()).isFalse();
        }

        @Test
        @DisplayName("Marcar principal inactivo lanza excepcion")
        void principal_inactivo_rechazado() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            service.desactivar(d.getId(), "SYS");
            assertThatThrownBy(() -> domRepo.marcarPrincipal(d.getId()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Principal de un tipo no afecta principal de otro tipo")
        void principal_no_afecta_otro_tipo() {
            FalPersonaDomicilio dReal = crearMalvinasLocal(true);

            FalPersonaDomicilio dLegal = service.crear(personaId, null,
                    TipoDomicilio.LEGAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100002", 1L, "12346", 2L,
                    "Calle Legal 1", null, true, null, null,
                    "Calle Legal 1, Malvinas Argentinas", false,
                    null, null, null, "SYS");

            assertThat(domRepo.buscarPrincipalActivo(personaId, TipoDomicilio.REAL)).isPresent();
            assertThat(domRepo.buscarPrincipalActivo(personaId, TipoDomicilio.LEGAL)).isPresent();

            FalPersonaDomicilio dRealActual = domRepo.buscarPorId(dReal.getId()).get();
            assertThat(dRealActual.isSiPrincipal()).isTrue();
        }
    }

    // =========================================================================
    // Notificabilidad
    // =========================================================================

    @Nested
    @DisplayName("Notificabilidad")
    class Notificabilidad {

        @Test
        @DisplayName("Solo domicilios activos y notificables aparecen en la lista")
        void notificables_activos() {
            FalPersonaDomicilio dNotif = crearMalvinasLocal(false);
            assertThat(dNotif.isSiNotificable()).isTrue();

            FalPersonaDomicilio dNoNotif = service.crear(personaId, null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                    true, false, false,
                    (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                    1001L, null, null, null, null, null,
                    null, null, true, null, null, "Exterior", false,
                    null, null, null, "SYS");

            List<FalPersonaDomicilio> notificables = domRepo.buscarNotificablesPorPersonaId(personaId);
            assertThat(notificables).hasSize(1);
            assertThat(notificables.get(0).getId()).isEqualTo(dNotif.getId());
        }
    }

    // =========================================================================
    // Correccion historica
    // =========================================================================

    @Nested
    @DisplayName("Correccion historica")
    class CorreccionHistorica {

        @Test
        @DisplayName("Domicilio no usado formalmente: corrige misma fila")
        void correccion_misma_fila() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            Long idOriginal = d.getId();
            FalPersonaDomicilio corregido = service.corregir(idOriginal, "Av. Pioneros 200 (corregido)", "SYS");
            assertThat(corregido.getId()).isEqualTo(idOriginal);
            assertThat(corregido.getDomicilioTxt()).isEqualTo("Av. Pioneros 200 (corregido)");
        }

        @Test
        @DisplayName("Domicilio usado formalmente: crea nueva fila")
        void correccion_nueva_fila() {
            PersonaDomicilioService serviceConUso = new PersonaDomicilioService(domRepo, personaRepo,
                    domicilioId -> true, FaltasClockTestSupport.FIXED);
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            Long idOriginal = d.getId();
            FalPersonaDomicilio nueva = serviceConUso.corregir(idOriginal, "Nueva version", "SYS");
            assertThat(nueva.getId()).isGreaterThan(idOriginal);
            assertThat(nueva.getDomicilioTxt()).isEqualTo("Nueva version");
            assertThat(domRepo.buscarPorId(idOriginal)).isPresent();
        }
    }

    // =========================================================================
    // Baja logica
    // =========================================================================

    @Nested
    @DisplayName("Baja logica")
    class BajaLogica {

        @Test
        @DisplayName("Desactivar pone siActivo=false y siPrincipal=false")
        void desactivar() {
            FalPersonaDomicilio d = crearMalvinasLocal(true);
            FalPersonaDomicilio inactivo = service.desactivar(d.getId(), "SYS");
            assertThat(inactivo.isSiActivo()).isFalse();
            assertThat(inactivo.isSiPrincipal()).isFalse();
        }

        @Test
        @DisplayName("Domicilio inactivo no aparece en buscarActivosPorPersonaId")
        void inactivo_no_en_activos() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            service.desactivar(d.getId(), "SYS");
            assertThat(domRepo.buscarActivosPorPersonaId(personaId)).isEmpty();
        }
    }

    // =========================================================================
    // Auditoria
    // =========================================================================

    @Nested
    @DisplayName("Auditoria")
    class Auditoria {

        @Test
        @DisplayName("fhAlta y idUserAlta se asignan en creacion")
        void auditoria_alta() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            assertThat(d.getFhAlta()).isNotNull();
            assertThat(d.getIdUserAlta()).isEqualTo("SYS");
        }

        @Test
        @DisplayName("fhUltMod se actualiza en desactivacion")
        void auditoria_mod() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            FalPersonaDomicilio inactivo = service.desactivar(d.getId(), "USR1");
            assertThat(inactivo.getIdUserUltMod()).isEqualTo("USR1");
            assertThat(inactivo.getFhUltMod()).isNotNull();
        }
    }

    // =========================================================================
    // Aislamiento
    // =========================================================================

    @Nested
    @DisplayName("Aislamiento de copias")
    class AislamientoCopias {

        @Test
        @DisplayName("guardar devuelve copia: mutacion no afecta store")
        void guardar_aislado() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            Long id = d.getId();
            d.setDomicilioTxt("Mutado");
            FalPersonaDomicilio stored = domRepo.buscarPorId(id).get();
            assertThat(stored.getDomicilioTxt()).isNotEqualTo("Mutado");
        }

        @Test
        @DisplayName("buscarPorId devuelve copia: mutacion no afecta store")
        void buscarPorId_aislado() {
            FalPersonaDomicilio d = crearMalvinasLocal(false);
            FalPersonaDomicilio r1 = domRepo.buscarPorId(d.getId()).get();
            r1.setDomicilioTxt("Mutado");
            FalPersonaDomicilio r2 = domRepo.buscarPorId(d.getId()).get();
            assertThat(r2.getDomicilioTxt()).isNotEqualTo("Mutado");
        }
    }

    // =========================================================================
    // Secuencia Long
    // =========================================================================

    @Nested
    @DisplayName("Secuencia de IDs Long")
    class SecuenciaLong {

        @Test
        @DisplayName("IDs son Long positivos y crecientes")
        void ids_long_crecientes() {
            FalPersonaDomicilio d1 = crearMalvinasLocal(false);
            FalPersonaDomicilio d2 = crearMalvinasLocal(false);
            assertThat(d1.getId()).isInstanceOf(Long.class).isPositive();
            assertThat(d2.getId()).isGreaterThan(d1.getId());
        }

        @Test
        @DisplayName("buscarPorId de domicilio inexistente devuelve empty")
        void buscar_inexistente_vacio() {
            assertThat(domRepo.buscarPorId(9999L)).isEmpty();
        }

        @Test
        @DisplayName("Domicilio inexistente en desactivar lanza excepcion")
        void desactivar_inexistente_excepcion() {
            assertThatThrownBy(() -> service.desactivar(9999L, "USR1"))
                    .isInstanceOf(DomicilioPersonaNoEncontradoException.class);
        }

        @Test
        @DisplayName("reset limpia el store y reinicia el contador")
        void reset_limpia() {
            crearMalvinasLocal(false);
            domRepo.reset();
            assertThat(domRepo.buscarPorPersonaId(personaId)).isEmpty();
            assertThat(domRepo.nextId()).isEqualTo(1L);
        }
    }

    // =========================================================================
    // Domicilio parcial
    // =========================================================================

    @Test
    @DisplayName("Domicilio parcial con calleTxt: siNormalizadoParcial=true es valido si tiene texto")
    void domicilio_parcial_valido() {
        FalPersonaDomicilio d = service.crear(personaId, null,
                TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                true, false, false,
                (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                null, null,
                null, null, null, null,
                "Calle sin numero", null, true, null, null,
                "Calle sin numero, localidad desconocida", true,
                null, null, null, "SYS");
        assertThat(d.isSiNormalizadoParcial()).isTrue();
        assertThat(d.getCalleTxt()).isEqualTo("Calle sin numero");
    }

    @Test
    @DisplayName("Domicilio historico inactivo: se conserva pero no aparece en activos")
    void domicilio_historico_inactivo() {
        FalPersonaDomicilio dViejo = crearMalvinasLocal(false);
        service.desactivar(dViejo.getId(), "SYS");
        crearMalvinasLocal(true);

        List<FalPersonaDomicilio> todos = domRepo.buscarPorPersonaId(personaId);
        List<FalPersonaDomicilio> activos = domRepo.buscarActivosPorPersonaId(personaId);
        assertThat(todos).hasSize(2);
        assertThat(activos).hasSize(1);
    }
}

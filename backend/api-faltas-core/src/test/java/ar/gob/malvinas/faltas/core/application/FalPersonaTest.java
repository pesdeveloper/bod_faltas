package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.PersonaService;
import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Slice 8F-11C: FalPersona, PersonaService, InMemoryPersonaRepository")
class FalPersonaTest {

    private InMemoryPersonaRepository repo;
    private PersonaService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPersonaRepository();
        service = new PersonaService(repo, FaltasClockTestSupport.FIXED);
    }

    @Nested
    @DisplayName("Enums persona")
    class EnumsPersona {

        @Test
        @DisplayName("TipoPersona.FISICA tiene codigo 1")
        void tipoPersona_fisica_codigo() {
            assertThat(TipoPersona.FISICA.codigo()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("TipoPersona.JURIDICA tiene codigo 2")
        void tipoPersona_juridica_codigo() {
            assertThat(TipoPersona.JURIDICA.codigo()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("TipoPersona.fromCodigo resuelve correctamente")
        void tipoPersona_fromCodigo() {
            assertThat(TipoPersona.fromCodigo((short) 1)).isEqualTo(TipoPersona.FISICA);
            assertThat(TipoPersona.fromCodigo((short) 2)).isEqualTo(TipoPersona.JURIDICA);
        }

        @Test
        @DisplayName("TipoPersona.fromCodigo invalido lanza excepcion")
        void tipoPersona_fromCodigo_invalido() {
            assertThatThrownBy(() -> TipoPersona.fromCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SujBieEstado tiene 5 valores con codigos 1-5")
        void sujBieEstado_codigos() {
            assertThat(SujBieEstado.SIN_CUENTA.codigo()).isEqualTo((short) 1);
            assertThat(SujBieEstado.PENDIENTE_CREACION.codigo()).isEqualTo((short) 2);
            assertThat(SujBieEstado.ACTIVA.codigo()).isEqualTo((short) 3);
            assertThat(SujBieEstado.ERROR_CREACION.codigo()).isEqualTo((short) 4);
            assertThat(SujBieEstado.INACTIVA.codigo()).isEqualTo((short) 5);
        }

        @Test
        @DisplayName("SujBieEstado.fromCodigo resuelve todos los valores")
        void sujBieEstado_fromCodigo() {
            for (SujBieEstado v : SujBieEstado.values()) {
                assertThat(SujBieEstado.fromCodigo(v.codigo())).isEqualTo(v);
            }
        }

        @Test
        @DisplayName("TipoDocumentoPersona DNI=1, CUIT=2, CUIL=3, PASAPORTE=4, DNI_EXTRANJERO=5, OTRO=9")
        void tipoDocumentoPersona_codigos() {
            assertThat(TipoDocumentoPersona.DNI.codigo()).isEqualTo((short) 1);
            assertThat(TipoDocumentoPersona.CUIT.codigo()).isEqualTo((short) 2);
            assertThat(TipoDocumentoPersona.CUIL.codigo()).isEqualTo((short) 3);
            assertThat(TipoDocumentoPersona.PASAPORTE.codigo()).isEqualTo((short) 4);
            assertThat(TipoDocumentoPersona.DNI_EXTRANJERO.codigo()).isEqualTo((short) 5);
            assertThat(TipoDocumentoPersona.OTRO.codigo()).isEqualTo((short) 9);
        }

        @Test
        @DisplayName("TipoDocumentoPersona.fromCodigo resuelve todos los valores")
        void tipoDocumentoPersona_fromCodigo() {
            for (TipoDocumentoPersona v : TipoDocumentoPersona.values()) {
                assertThat(TipoDocumentoPersona.fromCodigo(v.codigo())).isEqualTo(v);
            }
        }

        @Test
        @DisplayName("TipoDocumentoPersona no usa ordinal sino codigo explicito")
        void tipoDocumentoPersona_no_ordinal() {
            assertThat((int) TipoDocumentoPersona.OTRO.codigo()).isEqualTo(9);
            assertThat(TipoDocumentoPersona.OTRO.ordinal()).isNotEqualTo(9);
        }
    }

    @Nested
    @DisplayName("Persona fisica valida")
    class PersonaFisicaValida {

        @Test
        @DisplayName("Crea persona fisica con apellido y nombre")
        void crear_fisica_apellido_nombre() {
            FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12345678",
                    "Perez", "Juan Carlos", null, null, null, null, "USR1");
            assertThat(p.getId()).isNotNull();
            assertThat(p.getTipoPersona()).isEqualTo(TipoPersona.FISICA);
            assertThat(p.getApellido()).isEqualTo("Perez");
            assertThat(p.getNombres()).isEqualTo("Juan Carlos");
            assertThat(p.getTipoDoc()).isEqualTo(TipoDocumentoPersona.DNI);
            assertThat(p.getNroDoc()).isEqualTo("12345678");
            assertThat(p.getRazonSocial()).isNull();
        }

        @Test
        @DisplayName("Persona fisica: razonSocial debe ser null")
        void fisica_no_admite_razonSocial() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, null, null, null, null, "Empresa SA", null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("razonSocial");
        }

        @Test
        @DisplayName("nombreMostrar se calcula de apellido y nombres en fisica")
        void nombreMostrar_calculado_fisica() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Lopez", "Maria", null, null, null, null, "USR1");
            assertThat(p.getNombreMostrar()).isEqualTo("LOPEZ, Maria");
        }

        @Test
        @DisplayName("nombreMostrar con solo apellido es apellido en mayuscula")
        void nombreMostrar_solo_apellido() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Gomez", null, null, null, null, null, "USR1");
            assertThat(p.getNombreMostrar()).isEqualTo("GOMEZ");
        }
    }

    @Nested
    @DisplayName("Persona juridica valida")
    class PersonaJuridicaValida {

        @Test
        @DisplayName("Crea persona juridica con razonSocial")
        void crear_juridica_razonSocial() {
            FalPersona p = service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-71234567-9",
                    null, null, "Empresa SA", null, null, null, "USR1");
            assertThat(p.getTipoPersona()).isEqualTo(TipoPersona.JURIDICA);
            assertThat(p.getRazonSocial()).isEqualTo("Empresa SA");
            assertThat(p.getApellido()).isNull();
            assertThat(p.getNombres()).isNull();
        }

        @Test
        @DisplayName("Persona juridica: apellido debe ser null")
        void juridica_no_admite_apellido() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.JURIDICA, null, null, "Apellido", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("apellido");
        }

        @Test
        @DisplayName("Persona juridica: nombres debe ser null")
        void juridica_no_admite_nombres() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.JURIDICA, null, null, null, "Juan", null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nombres");
        }

        @Test
        @DisplayName("nombreMostrar de juridica usa razonSocial")
        void nombreMostrar_juridica() {
            FalPersona p = service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-999-9",
                    null, null, "Logistica Sur SRL", null, null, null, "USR1");
            assertThat(p.getNombreMostrar()).isEqualTo("Logistica Sur SRL");
        }
    }

    @Nested
    @DisplayName("Documento de identidad")
    class DocumentoIdentidad {

        @Test
        @DisplayName("tipoDoc y nroDoc se informan juntos")
        void tipoDoc_y_nroDoc_juntos() {
            FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12345678",
                    "Apellido", null, null, null, null, null, "USR1");
            assertThat(p.getTipoDoc()).isEqualTo(TipoDocumentoPersona.DNI);
            assertThat(p.getNroDoc()).isEqualTo("12345678");
            assertThat(p.docTxt()).contains("DNI").contains("12345678");
        }

        @Test
        @DisplayName("tipoDoc sin nroDoc lanza excepcion")
        void tipoDoc_sin_nroDoc() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, null, "Perez", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nroDoc sin tipoDoc lanza excepcion")
        void nroDoc_sin_tipoDoc() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, null, "12345678", "Perez", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nroDoc no puede exceder 20 caracteres")
        void nroDoc_max_20() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "123456789012345678901",
                            "Perez", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Sin documento: tipoDoc y nroDoc quedan null")
        void sin_documento_ambos_null() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Fernandez", "Ana", null, null, null, null, "USR1");
            assertThat(p.getTipoDoc()).isNull();
            assertThat(p.getNroDoc()).isNull();
            assertThat(p.docTxt()).isNull();
        }
    }

    @Nested
    @DisplayName("Vinculo Ingresos SujBie")
    class VinculoIngresos {

        @Test
        @DisplayName("idBie no puede existir sin idSuj")
        void idBie_sin_idSuj_rechazado() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            assertThatThrownBy(() ->
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, null, 42L, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Para Faltas idSuj debe ser 20 cuando se informa")
        void idSuj_debe_ser_20() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            assertThatThrownBy(() ->
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, 99L, 42L, FaltasClockTestSupport.FIXED.now(), "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("20");
        }

        @Test
        @DisplayName("ACTIVA: idSuj=20, idBie, fhSujBieCreacion obligatorios")
        void activa_exige_todos_campos() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            LocalDateTime fh = FaltasClockTestSupport.FIXED.now();
            FalPersona act = service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, 20L, 5001L, fh, "USR1");
            assertThat(act.getIdSuj()).isEqualTo(20L);
            assertThat(act.getIdBie()).isEqualTo(5001L);
            assertThat(act.getSujBieEstado()).isEqualTo(SujBieEstado.ACTIVA);
            assertThat(act.tieneCuentaActiva()).isTrue();
        }

        @Test
        @DisplayName("SIN_CUENTA: idSuj, idBie, fhSujBieCreacion rechazados")
        void sin_cuenta_rechaza_campos() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            assertThatThrownBy(() ->
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.SIN_CUENTA, 20L, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Auditoria")
    class Auditoria {

        @Test
        @DisplayName("fhAlta y idUserAlta se asignan en creacion")
        void auditoria_alta() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "INSPECTOR01");
            assertThat(p.getFhAlta()).isNotNull();
            assertThat(p.getIdUserAlta()).isEqualTo("INSPECTOR01");
            assertThat(p.getFhUltMod()).isNotNull();
        }

        @Test
        @DisplayName("fhUltMod e idUserUltMod se actualizan en modificacion")
        void auditoria_modificacion() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Original", null, null, null, null, null, "USR1");
            FalPersona mod = service.modificar(p.getId(), null, null, "Modificado", null, null, null, null, null, "USR2");
            assertThat(mod.getIdUserUltMod()).isEqualTo("USR2");
            assertThat(mod.getFhUltMod()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Busqueda por documento")
    class BusquedaPorDocumento {

        @Test
        @DisplayName("Busca persona por tipo y numero de documento")
        void buscar_por_tipoDoc_nroDoc() {
            service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "22334455",
                    "Busqueda", null, null, null, null, null, "USR1");
            List<FalPersona> found = service.buscarPorDocumento(TipoDocumentoPersona.DNI, "22334455");
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getNroDoc()).isEqualTo("22334455");
        }

        @Test
        @DisplayName("Busqueda con tipo diferente devuelve vacio")
        void buscar_tipo_diferente_vacio() {
            service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "11223344",
                    "Test", null, null, null, null, null, "USR1");
            List<FalPersona> found = service.buscarPorDocumento(TipoDocumentoPersona.CUIT, "11223344");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Aislamiento de copias")
    class AislamientoCopias {

        @Test
        @DisplayName("guardar almacena copia del input: mutacion del input no afecta store")
        void guardar_aislado() {
            java.time.LocalDateTime now = FaltasClockTestSupport.FIXED.now();
            FalPersona input = new FalPersona(100L, TipoPersona.FISICA, now, "USR1");
            input.setApellido("Original");
            repo.guardar(input);
            // mutate input after guardar
            input.setApellido("Modificado");
            Optional<FalPersona> stored = repo.buscarPorId(100L);
            assertThat(stored.get().getApellido()).isEqualTo("Original");
        }

        @Test
        @DisplayName("buscarPorId devuelve copia: mutacion no afecta store")
        void buscarPorId_aislado() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Stored", null, null, null, null, null, "USR1");
            FalPersona r1 = repo.buscarPorId(p.getId()).get();
            r1.setApellido("Mutado");
            FalPersona r2 = repo.buscarPorId(p.getId()).get();
            assertThat(r2.getApellido()).isEqualTo("Stored");
        }
    }

    @Nested
    @DisplayName("Secuencia de IDs Long")
    class SecuenciaLong {

        @Test
        @DisplayName("IDs son Long positivos y crecientes")
        void ids_long_crecientes() {
            FalPersona p1 = service.crear(TipoPersona.FISICA, null, null, "P1", null, null, null, null, null, "USR1");
            FalPersona p2 = service.crear(TipoPersona.FISICA, null, null, "P2", null, null, null, null, null, "USR1");
            assertThat(p1.getId()).isInstanceOf(Long.class).isPositive();
            assertThat(p2.getId()).isGreaterThan(p1.getId());
        }

        @Test
        @DisplayName("listarTodas devuelve todas las personas guardadas")
        void listar_todas() {
            service.crear(TipoPersona.FISICA, null, null, "A", null, null, null, null, null, "USR1");
            service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-111-1", null, null, "Empresa", null, null, null, "USR1");
            assertThat(repo.listarTodas()).hasSize(2);
        }

        @Test
        @DisplayName("buscarPorId de persona inexistente devuelve empty")
        void buscar_inexistente_vacio() {
            assertThat(repo.buscarPorId(9999L)).isEmpty();
        }

        @Test
        @DisplayName("obtener de persona inexistente lanza PersonaNoEncontradaException")
        void obtener_inexistente_excepcion() {
            assertThatThrownBy(() -> service.obtener(9999L))
                    .isInstanceOf(PersonaNoEncontradaException.class);
        }

        @Test
        @DisplayName("reset limpia el store y reinicia el contador a 1")
        void reset_limpia() {
            service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            repo.reset();
            assertThat(repo.listarTodas()).isEmpty();
            assertThat(repo.nextId()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("Persona fisica sin documento es valida si tiene nombre")
    void persona_sin_documento_valida() {
        FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Desconocido", "Juan", null, null, null, null, "USR1");
        assertThat(p.getTipoDoc()).isNull();
        assertThat(p.getNroDoc()).isNull();
        assertThat(p.getApellido()).isEqualTo("Desconocido");
    }

    @Test
    @DisplayName("Cuenta Ingresos ACTIVA: tieneCuentaActiva retorna true")
    void persona_cuenta_activa() {
        FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "45678901",
                "ConCuenta", null, null, null, null, null, "USR1");
        FalPersona act = service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA,
                20L, 9001L, FaltasClockTestSupport.FIXED.now(), "SYS");
        assertThat(act.tieneCuentaActiva()).isTrue();
    }
}

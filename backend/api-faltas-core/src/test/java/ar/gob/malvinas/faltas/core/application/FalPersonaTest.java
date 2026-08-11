package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.PersonaService;
import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
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
            FalPersona p = service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-70999999-9",
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
        @DisplayName("nroDoc no puede exceder 12 caracteres canonicos")
        void nroDoc_max_12() {
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, TipoDocumentoPersona.OTRO, "ABC0123456789",
                            "Perez", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nroDoc alfanumerico de 12 caracteres es valido")
        void nroDoc_12_valido() {
            FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.OTRO, "AB0123456789",
                    "Perez", null, null, null, null, null, "USR1");
            assertThat(p.getNroDoc()).isEqualTo("AB0123456789").hasSize(12);
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
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, null, 42, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Para Faltas idSuj debe ser 20 cuando se informa")
        void idSuj_debe_ser_20() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            assertThatThrownBy(() ->
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, 99, 42, FaltasClockTestSupport.FIXED.now(), "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("20");
        }

        @Test
        @DisplayName("ACTIVA: idSuj=20, idBie, fhSujBieCreacion obligatorios")
        void activa_exige_todos_campos() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            LocalDateTime fh = FaltasClockTestSupport.FIXED.now();
            FalPersona act = service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, 20, 5001, fh, "USR1");
            assertThat(act.getIdSuj()).isEqualTo(20);
            assertThat(act.getIdBie()).isEqualTo(5001);
            assertThat(act.getSujBieEstado()).isEqualTo(SujBieEstado.ACTIVA);
            assertThat(act.tieneCuentaActiva()).isTrue();
        }

        @Test
        @DisplayName("SIN_CUENTA: idSuj, idBie, fhSujBieCreacion rechazados")
        void sin_cuenta_rechaza_campos() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            assertThatThrownBy(() ->
                    service.actualizarVinculoIngresos(p.getId(), SujBieEstado.SIN_CUENTA, 20, null, null, "USR1"))
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
            FalPersona input = new FalPersona(null, TipoPersona.FISICA, now, "USR1");
            input.setApellido("Original");
            FalPersona persistida = repo.guardar(input);
            // mutate input after guardar
            input.setApellido("Modificado");
            Optional<FalPersona> stored = repo.buscarPorId(persistida.getId());
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
            service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-71111111-1", null, null, "Empresa", null, null, null, "USR1");
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
        @DisplayName("reset limpia el store y el proximo alta vuelve a id 1")
        void reset_limpia() {
            service.crear(TipoPersona.FISICA, null, null, "Test", null, null, null, null, null, "USR1");
            repo.reset();
            assertThat(repo.listarTodas()).isEmpty();
            FalPersona nueva = service.crear(TipoPersona.FISICA, null, null, "Nueva", null, null, null, null, null, "USR1");
            assertThat(nueva.getId()).isEqualTo(1L);
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
                    20, 9001, FaltasClockTestSupport.FIXED.now(), "SYS");
            assertThat(act.tieneCuentaActiva()).isTrue();
        }

    // =========================================================================
    // WAVE 1A2-B1.5B0-A: identidad, documento canonico y semantica P3
    // =========================================================================

    @Nested
    @DisplayName("Identidad de FalPersona (DECISION_ID-01)")
    class IdentidadFalPersona {

        private final LocalDateTime AHORA = FaltasClockTestSupport.FIXED.now();

        @Test
        @DisplayName("una entidad nueva admite id null")
        void entidad_nueva_admite_id_null() {
            FalPersona nueva = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            assertThat(nueva.getId()).isNull();
        }

        @Test
        @DisplayName("id cero y negativo se rechazan")
        void id_no_positivo_rechazado() {
            assertThatThrownBy(() -> new FalPersona(0L, TipoPersona.FISICA, AHORA, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positivo");
            assertThatThrownBy(() -> new FalPersona(-1L, TipoPersona.FISICA, AHORA, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("conId devuelve una copia con la PK asignada y preserva los campos")
        void conId_devuelve_copia_con_pk() {
            FalPersona nueva = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            nueva.setApellido("Perez");
            nueva.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12.345.678"));

            FalPersona persistida = nueva.conId(77L);

            assertThat(persistida.getId()).isEqualTo(77L);
            assertThat(persistida.getApellido()).isEqualTo("Perez");
            assertThat(persistida.getNroDoc()).isEqualTo("12345678");
            assertThat(persistida.getTipoDoc()).isEqualTo(TipoDocumentoPersona.DNI);
        }

        @Test
        @DisplayName("conId no muta la instancia receptora")
        void conId_no_muta() {
            FalPersona nueva = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            nueva.setApellido("Perez");
            FalPersona persistida = nueva.conId(5L);
            assertThat(nueva.getId()).isNull();
            assertThat(persistida).isNotSameAs(nueva);
        }

        @Test
        @DisplayName("conId exige un id positivo")
        void conId_exige_positivo() {
            FalPersona nueva = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            assertThatThrownBy(() -> nueva.conId(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> nueva.conId(0L)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> nueva.conId(-3L)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("una persona sin ninguna forma de identificacion no es valida")
        void persona_vacia_rechazada() {
            FalPersona vacia = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            assertThat(vacia.tieneIdentificacionMinima()).isFalse();
            assertThatThrownBy(() ->
                    service.crear(TipoPersona.FISICA, null, null, null, null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos una forma de identificacion");
        }
    }

    @Nested
    @DisplayName("DocumentoPersona: normalizacion canonica por tipo")
    class NormalizacionDocumento {

        @Test
        @DisplayName("documento ausente: los cuatro componentes quedan null")
        void ausente_cuatro_null() {
            DocumentoPersona d = DocumentoPersona.normalizar(null, null);
            assertThat(d.estaAusente()).isTrue();
            assertThat(d.tipoDoc()).isNull();
            assertThat(d.nroDoc()).isNull();
            assertThat(d.prefijoCuitCuil()).isNull();
            assertThat(d.digitoVerificador()).isNull();
        }

        @Test
        @DisplayName("tipo sin numero y numero sin tipo se rechazan")
        void par_incompleto_rechazado() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("juntos");
            assertThatThrownBy(() -> DocumentoPersona.normalizar(null, "12345678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("juntos");
        }

        @Test
        @DisplayName("DNI con puntos se canoniza a digitos")
        void dni_con_puntos() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12.345.678").nroDoc())
                    .isEqualTo("12345678");
        }

        @Test
        @DisplayName("DNI con espacios internos se canoniza a digitos")
        void dni_con_espacios() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, " 12 345 678 ").nroDoc())
                    .isEqualTo("12345678");
        }

        @Test
        @DisplayName("DNI pierde los ceros iniciales: no forman parte de la identidad")
        void dni_sin_ceros_iniciales() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "0012345").nroDoc())
                    .isEqualTo("12345");
        }

        @Test
        @DisplayName("DNI cero se rechaza")
        void dni_cero_rechazado() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("DNI con letras se rechaza")
        void dni_con_letras_rechazado() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12A45678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo digitos");
        }

        @Test
        @DisplayName("los mensajes de rechazo no reproducen el documento recibido")
        void mensajes_no_filtran_el_documento() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12A45678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining("12A45678");
        }

        @Test
        @DisplayName("CUIT con guiones se descompone en prefijo, cuerpo y digito")
        void cuit_con_guiones() {
            DocumentoPersona d = DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3");
            assertThat(d.prefijoCuitCuil()).isEqualTo(20);
            assertThat(d.nroDoc()).isEqualTo("12345678");
            assertThat(d.digitoVerificador()).isEqualTo(3);
        }

        @Test
        @DisplayName("CUIT sin separadores produce la misma descomposicion")
        void cuit_sin_separadores() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20123456783"))
                    .isEqualTo(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3"));
        }

        @Test
        @DisplayName("CUIT con espacios produce la misma descomposicion")
        void cuit_con_espacios() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20 12345678 3"))
                    .isEqualTo(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3"));
        }

        @Test
        @DisplayName("el cuerpo del CUIT conserva los ceros iniciales: es de ancho fijo")
        void cuerpo_cuit_conserva_ceros() {
            DocumentoPersona d = DocumentoPersona.normalizar(TipoDocumentoPersona.CUIL, "27-00123456-9");
            assertThat(d.nroDoc()).isEqualTo("00123456").hasSize(8);
            assertThat(d.prefijoCuitCuil()).isEqualTo(27);
        }

        @Test
        @DisplayName("CUIT de longitud distinta de 11 digitos se rechaza")
        void cuit_longitud_invalida() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "30-999-9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("11 digitos");
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "201234567834"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("CUIT con letras se rechaza y no se degrada a otro tipo")
        void cuit_con_letras_rechazado() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-1234567A-3"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo digitos");
        }

        @Test
        @DisplayName("un prefijo de CUIT con cero inicial queda fuera del rango canonico")
        void cuit_prefijo_fuera_de_rango() {
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "05-12345678-3"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prefijoCuitCuil");
        }

        @Test
        @DisplayName("PASAPORTE alfanumerico pasa a uppercase y preserva ceros")
        void pasaporte_uppercase_y_ceros() {
            DocumentoPersona d = DocumentoPersona.normalizar(TipoDocumentoPersona.PASAPORTE, "ab-012345");
            assertThat(d.nroDoc()).isEqualTo("AB012345");
            assertThat(d.prefijoCuitCuil()).isNull();
            assertThat(d.digitoVerificador()).isNull();
        }

        @Test
        @DisplayName("alfanumerico de 12 se acepta y de 13 se rechaza")
        void alfanumerico_frontera_12() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "AB0123456789").nroDoc())
                    .hasSize(12);
            assertThatThrownBy(() -> DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "AB01234567890"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("los componentes de CUIT en un tipo no CUIT se rechazan")
        void componentes_cuit_en_tipo_no_cuit() {
            assertThatThrownBy(() ->
                    new DocumentoPersona(TipoDocumentoPersona.DNI, "12345678", 20, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo aplican");
        }

        @Test
        @DisplayName("CUIT sin prefijo o sin digito se rechaza")
        void cuit_sin_componentes() {
            assertThatThrownBy(() ->
                    new DocumentoPersona(TipoDocumentoPersona.CUIT, "12345678", null, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prefijoCuitCuil");
            assertThatThrownBy(() ->
                    new DocumentoPersona(TipoDocumentoPersona.CUIT, "12345678", 20, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("digitoVerificador");
        }

        @Test
        @DisplayName("sin documento no puede haber prefijo ni digito")
        void ausente_no_admite_componentes() {
            assertThatThrownBy(() -> new DocumentoPersona(null, null, 20, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("la forma visible del CUIT se reconstruye desde los tres componentes")
        void forma_visible_cuit() {
            DocumentoPersona d = DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20123456783");
            assertThat(d.nroDocVisible()).isEqualTo("20-12345678-3");
        }

        @Test
        @DisplayName("docTxt de un CUIT rinde la forma visible, no el cuerpo desnudo")
        void docTxt_cuit_visible() {
            FalPersona p = service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-70999999-4",
                    null, null, "Empresa SA", null, null, null, "USR1");
            assertThat(p.docTxt()).isEqualTo("CUIT 30-70999999-4");
        }

        @Test
        @DisplayName("guardar y buscar usan la misma forma canonica")
        void guardar_y_buscar_misma_forma() {
            service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12.345.678",
                    "Canonico", null, null, null, null, null, "USR1");
            assertThat(service.buscarPorDocumento(TipoDocumentoPersona.DNI, "12 345 678")).hasSize(1);
            assertThat(service.buscarPorDocumento(TipoDocumentoPersona.DNI, "12345678")).hasSize(1);
        }

        @Test
        @DisplayName("buscar un valor no canonizable para el tipo devuelve vacio, no excepcion")
        void buscar_no_canonizable_devuelve_vacio() {
            service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "11223344",
                    "Canonico", null, null, null, null, null, "USR1");
            assertThat(service.buscarPorDocumento(TipoDocumentoPersona.CUIT, "11223344")).isEmpty();
            assertThatThrownBy(() -> service.crear(TipoPersona.FISICA, TipoDocumentoPersona.CUIT,
                    "11223344", "Perez", null, null, null, null, null, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("InMemoryPersonaRepository: patron P3")
    class RepositorioP3 {

        private final LocalDateTime AHORA = FaltasClockTestSupport.FIXED.now();

        private FalPersona nueva(String apellido) {
            FalPersona p = new FalPersona(null, TipoPersona.FISICA, AHORA, "USR1");
            p.setApellido(apellido);
            return p;
        }

        @Test
        @DisplayName("el alta con id null asigna la PK internamente")
        void alta_asigna_id() {
            FalPersona persistida = repo.guardar(nueva("Alta"));
            assertThat(persistida.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("el alta no muta el argumento")
        void alta_no_muta_argumento() {
            FalPersona argumento = nueva("Alta");
            FalPersona persistida = repo.guardar(argumento);
            assertThat(argumento.getId()).isNull();
            assertThat(persistida).isNotSameAs(argumento);
        }

        @Test
        @DisplayName("los ids asignados son crecientes")
        void ids_crecientes() {
            FalPersona p1 = repo.guardar(nueva("A"));
            FalPersona p2 = repo.guardar(nueva("B"));
            assertThat(p2.getId()).isGreaterThan(p1.getId());
        }

        @Test
        @DisplayName("guardar con id existente actualiza la fila")
        void update_existente() {
            FalPersona persistida = repo.guardar(nueva("Original"));
            persistida.setApellido("Modificado");
            FalPersona actualizada = repo.guardar(persistida);
            assertThat(actualizada.getId()).isEqualTo(persistida.getId());
            assertThat(repo.buscarPorId(persistida.getId()).orElseThrow().getApellido())
                    .isEqualTo("Modificado");
            assertThat(repo.listarTodas()).hasSize(1);
        }

        @Test
        @DisplayName("guardar con id inexistente lanza PersonaNoEncontradaException y no inserta")
        void update_inexistente_no_inserta() {
            FalPersona fantasma = new FalPersona(9999L, TipoPersona.FISICA, AHORA, "USR1");
            fantasma.setApellido("Fantasma");
            assertThatThrownBy(() -> repo.guardar(fantasma))
                    .isInstanceOf(PersonaNoEncontradaException.class);
            assertThat(repo.buscarPorId(9999L)).isEmpty();
            assertThat(repo.listarTodas()).isEmpty();
        }

        @Test
        @DisplayName("cargarSeed reposiciona el contador por encima del maximo sembrado")
        void cargarSeed_reposiciona_contador() {
            FalPersona sembrada = new FalPersona(50L, TipoPersona.FISICA, AHORA, "USR1");
            sembrada.setApellido("Semilla");
            repo.cargarSeed(List.of(sembrada));
            assertThat(repo.guardar(nueva("Siguiente")).getId()).isEqualTo(51L);
        }

        @Test
        @DisplayName("cargarSeed exige personas con id informado")
        void cargarSeed_exige_id() {
            assertThatThrownBy(() -> repo.cargarSeed(List.of(nueva("SinId"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listarTodas devuelve las personas ordenadas por id ascendente")
        void listarTodas_orden_id_asc() {
            for (String a : List.of("C", "A", "B", "E", "D")) {
                repo.guardar(nueva(a));
            }
            assertThat(repo.listarTodas())
                    .extracting(FalPersona::getId)
                    .containsExactly(1L, 2L, 3L, 4L, 5L);
        }

        @Test
        @DisplayName("buscarPorTipoDocYNroDoc devuelve resultados ordenados por id ascendente")
        void buscarPorDocumento_orden_id_asc() {
            for (int i = 0; i < 3; i++) {
                FalPersona p = nueva("Homonimo" + i);
                p.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12345678"));
                repo.guardar(p);
            }
            assertThat(repo.buscarPorTipoDocYNroDoc(TipoDocumentoPersona.DNI, "12345678"))
                    .extracting(FalPersona::getId)
                    .containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("buscarPorIdSujBie devuelve resultados ordenados por id ascendente")
        void buscarPorIdSujBie_orden_id_asc() {
            for (int i = 0; i < 3; i++) {
                FalPersona p = nueva("Cuenta" + i);
                p.setIdSuj(20);
                p.setIdBie(777);
                repo.guardar(p);
            }
            assertThat(repo.buscarPorIdSujBie(20, 777))
                    .extracting(FalPersona::getId)
                    .containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("el update devuelve una copia defensiva")
        void update_devuelve_copia() {
            FalPersona persistida = repo.guardar(nueva("Original"));
            FalPersona devuelta = repo.guardar(persistida);
            devuelta.setApellido("MutadoFuera");
            assertThat(repo.buscarPorId(persistida.getId()).orElseThrow().getApellido())
                    .isEqualTo("Original");
        }
    }

    @Nested
    @DisplayName("PersonaService con el normalizador canonico")
    class ServicioNormalizado {

        @Test
        @DisplayName("crear con documento conocido persiste la forma canonica")
        void crear_con_documento_conocido() {
            FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12.345.678",
                    "Perez", "Ana", null, null, null, null, "USR1");
            assertThat(p.getId()).isNotNull().isPositive();
            assertThat(p.getNroDoc()).isEqualTo("12345678");
        }

        @Test
        @DisplayName("crear persona parcial sin documento es valido si hay nombre")
        void crear_parcial_sin_documento() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Solo", "Nombre", null, null, null, null, "USR1");
            assertThat(p.getTipoDoc()).isNull();
            assertThat(p.getNroDoc()).isNull();
            assertThat(p.getPrefijoCuitCuil()).isNull();
            assertThat(p.getDigitoVerificador()).isNull();
        }

        @Test
        @DisplayName("crearMinimal no infiere tipo documental desde un texto numerico")
        void crearMinimal_no_infiere_tipo() {
            FalPersona p = service.crearMinimal("12345678", "Juan Perez", "USR1").orElseThrow();
            assertThat(p.getTipoDoc()).isNull();
            assertThat(p.getNroDoc()).isNull();
            assertThat(p.getNombreMostrar()).isEqualTo("Juan Perez");
        }

        @Test
        @DisplayName("crearMinimal sin ningun dato real no crea persona")
        void crearMinimal_sin_datos_no_crea() {
            assertThat(service.crearMinimal("12345678", null, "USR1")).isEmpty();
            assertThat(repo.listarTodas()).isEmpty();
        }

        @Test
        @DisplayName("crearMinimal no trunca en silencio un nombre de 64 caracteres")
        void crearMinimal_no_trunca_hasta_64() {
            String nombre64 = "N".repeat(64);
            FalPersona p = service.crearMinimal(null, nombre64, "USR1").orElseThrow();
            assertThat(p.getNombreMostrar()).isEqualTo(nombre64);
        }

        @Test
        @DisplayName("modificar agrega un documento a una persona que no lo tenia")
        void modificar_agrega_documento() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Perez", null, null, null, null, null, "USR1");
            FalPersona mod = service.modificar(p.getId(), TipoDocumentoPersona.DNI, "12.345.678",
                    "Perez", null, null, null, null, null, "USR2");
            assertThat(mod.getNroDoc()).isEqualTo("12345678");
            assertThat(mod.getId()).isEqualTo(p.getId());
        }

        @Test
        @DisplayName("modificar elimina el documento dejando los cuatro componentes null")
        void modificar_elimina_documento() {
            FalPersona p = service.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-70999999-4",
                    null, null, "Empresa SA", null, null, null, "USR1");
            assertThat(p.getPrefijoCuitCuil()).isEqualTo(30);

            FalPersona mod = service.modificar(p.getId(), null, null,
                    null, null, "Empresa SA", null, null, null, "USR2");

            assertThat(mod.getTipoDoc()).isNull();
            assertThat(mod.getNroDoc()).isNull();
            assertThat(mod.getPrefijoCuitCuil()).isNull();
            assertThat(mod.getDigitoVerificador()).isNull();
        }

        @Test
        @DisplayName("cambiar de CUIT a DNI limpia prefijo y digito verificador")
        void modificar_cuit_a_dni_limpia_componentes() {
            FalPersona p = service.crear(TipoPersona.FISICA, TipoDocumentoPersona.CUIT, "20-12345678-3",
                    "Perez", null, null, null, null, null, "USR1");
            assertThat(p.getDigitoVerificador()).isEqualTo(3);

            FalPersona mod = service.modificar(p.getId(), TipoDocumentoPersona.DNI, "12345678",
                    "Perez", null, null, null, null, null, "USR2");

            assertThat(mod.getTipoDoc()).isEqualTo(TipoDocumentoPersona.DNI);
            assertThat(mod.getNroDoc()).isEqualTo("12345678");
            assertThat(mod.getPrefijoCuitCuil()).isNull();
            assertThat(mod.getDigitoVerificador()).isNull();
        }

        @Test
        @DisplayName("actualizarVinculoIngresos actualiza la entidad existente sin insertar otra")
        void vinculo_ingresos_actualiza_existente() {
            FalPersona p = service.crear(TipoPersona.FISICA, null, null,
                    "Cuenta", null, null, null, null, null, "USR1");
            service.actualizarVinculoIngresos(p.getId(), SujBieEstado.ACTIVA, 20, 4242,
                    FaltasClockTestSupport.FIXED.now(), "USR2");
            assertThat(repo.listarTodas()).hasSize(1);
            assertThat(repo.buscarPorId(p.getId()).orElseThrow().getIdBie()).isEqualTo(4242);
        }

        @Test
        @DisplayName("modificar una persona inexistente lanza PersonaNoEncontradaException")
        void modificar_inexistente() {
            assertThatThrownBy(() -> service.modificar(9999L, null, null,
                    "Perez", null, null, null, null, null, "USR2"))
                    .isInstanceOf(PersonaNoEncontradaException.class);
        }
    }
}

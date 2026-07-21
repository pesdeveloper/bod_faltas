package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.application.port.ConfiguracionPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalculadorPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.application.service.PlazosAdministrativosService;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.infrastructure.config.PlazosAdministrativosProperties;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PlazosAdministrativosServiceTest {

    private InMemoryDiaNoComputableRepository repository;
    private CalendarioAdministrativoService calendarioService;
    private CalculadorPlazosAdministrativos calculador;

    /** Configuracion ajustable para tests. */
    private static class ConfiguracionFija implements ConfiguracionPlazosAdministrativos {
        private int dias;
        ConfiguracionFija(int dias) { this.dias = dias; }
        void setDias(int dias) { this.dias = dias; }
        @Override
        public int cantidadDiasComputables(TipoPlazoAdministrativo tipo) {
            if (tipo == TipoPlazoAdministrativo.APELACION_FALLO) return dias;
            throw new IllegalArgumentException("tipo no configurado: " + tipo);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryDiaNoComputableRepository();
        CountingClock clock = CountingClock.startingAt(Instant.parse("2026-07-12T12:00:00Z"));
        calendarioService = new CalendarioAdministrativoService(repository, clock);
        calculador = new CalculadorPlazosAdministrativos(calendarioService);
    }

    @Test
    @DisplayName("1. Configuracion default devuelve 30")
    void configuracion_default_30() {
        // Usa la clase productiva real, no un fake construido con 30.
        PlazosAdministrativosProperties properties = new PlazosAdministrativosProperties();
        PlazosAdministrativosService svc = new PlazosAdministrativosService(properties, calculador);

        assertThat(properties.getApelacionDiasComputables()).isEqualTo(30);
        assertThat(properties.cantidadDiasComputables(TipoPlazoAdministrativo.APELACION_FALLO))
                .isEqualTo(30);

        CalculoPlazoAdministrativo resultado = svc.calcular(
                TipoPlazoAdministrativo.APELACION_FALLO, LocalDate.of(2026, 7, 10));

        assertThat(resultado.diasComputablesAplicados()).isEqualTo(30);
    }

    @Test
    @DisplayName("2. Override global a 45 se usa sin modificar el calculador")
    void override_global_a_45() {
        // Override sobre la misma clase productiva.
        PlazosAdministrativosProperties properties = new PlazosAdministrativosProperties();
        properties.setApelacionDiasComputables(45);
        PlazosAdministrativosService svc = new PlazosAdministrativosService(properties, calculador);

        CalculoPlazoAdministrativo resultado = svc.calcular(
                TipoPlazoAdministrativo.APELACION_FALLO, LocalDate.of(2026, 7, 10));

        assertThat(resultado.diasComputablesAplicados()).isEqualTo(45);
    }

    @Test
    @DisplayName("3. calcularVencimientoApelacion devuelve tipo, origen, cantidad y fecha")
    void convenencia_devuelve_todos_los_campos() {
        ConfiguracionFija config = new ConfiguracionFija(30);
        PlazosAdministrativosService svc = new PlazosAdministrativosService(config, calculador);
        LocalDate fechaNotificacion = LocalDate.of(2026, 7, 10);

        CalculoPlazoAdministrativo resultado = svc.calcularVencimientoApelacion(fechaNotificacion);

        assertThat(resultado.tipo()).isEqualTo(TipoPlazoAdministrativo.APELACION_FALLO);
        assertThat(resultado.fechaOrigen()).isEqualTo(fechaNotificacion);
        assertThat(resultado.diasComputablesAplicados()).isEqualTo(30);
        assertThat(resultado.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    @DisplayName("4. El servicio lee la cantidad una sola vez por calculo")
    void lee_cantidad_una_vez() {
        // Configuracion contador: incrementa un AtomicInteger por invocacion y devuelve 30.
        AtomicInteger calls = new AtomicInteger(0);
        ConfiguracionPlazosAdministrativos contador = tipo -> {
            calls.incrementAndGet();
            if (tipo == TipoPlazoAdministrativo.APELACION_FALLO) return 30;
            throw new IllegalArgumentException("tipo no configurado: " + tipo);
        };
        PlazosAdministrativosService svc = new PlazosAdministrativosService(contador, calculador);

        CalculoPlazoAdministrativo r = svc.calcularVencimientoApelacion(LocalDate.of(2026, 7, 10));

        assertThat(calls.get()).isEqualTo(1);
        assertThat(r.diasComputablesAplicados()).isEqualTo(30);
        assertThat(r.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    @DisplayName("5. Un calculo ya obtenido no cambia si se modifica el calendario despues")
    void calculo_no_cambia_por_cambio_posterior_calendario() {
        ConfiguracionFija config = new ConfiguracionFija(1);
        PlazosAdministrativosService svc = new PlazosAdministrativosService(config, calculador);

        // Origen viernes 07-ago, sin excepciones -> sabado 08-ago
        LocalDate origen = LocalDate.of(2026, 8, 7);
        CalculoPlazoAdministrativo r1 = svc.calcularVencimientoApelacion(origen);
        assertThat(r1.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 8, 8));

        // Ahora marcar sabado como feriado
        CountingClock regClock = CountingClock.startingAt(Instant.parse("2026-08-06T10:00:00Z"));
        CalendarioAdministrativoService svc2 = new CalendarioAdministrativoService(repository, regClock);
        svc2.registrarDiaNoComputable(LocalDate.of(2026, 8, 8), TipoDiaNoComputable.FERIADO,
                "Post calculo", OrigenDiaNoComputable.MANUAL, null, "op1");

        // El resultado r1 ya calculado no cambia
        assertThat(r1.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 8, 8));
    }

    @Test
    @DisplayName("6. Un calculo nuevo si refleja una excepcion agregada despues")
    void calculo_nuevo_refleja_excepcion_nueva() {
        ConfiguracionFija config = new ConfiguracionFija(1);
        PlazosAdministrativosService svc = new PlazosAdministrativosService(config, calculador);

        LocalDate origen = LocalDate.of(2026, 8, 7);

        // Marcar sabado como feriado ANTES del nuevo calculo
        CountingClock regClock = CountingClock.startingAt(Instant.parse("2026-08-06T10:00:00Z"));
        CalendarioAdministrativoService svc2 = new CalendarioAdministrativoService(repository, regClock);
        svc2.registrarDiaNoComputable(LocalDate.of(2026, 8, 8), TipoDiaNoComputable.FERIADO,
                "Nuevo feriado", OrigenDiaNoComputable.MANUAL, null, "op1");

        // Nuevo calculo: sabado no computa, siguiente es lunes 10-ago
        CalculoPlazoAdministrativo r2 = svc.calcularVencimientoApelacion(origen);
        assertThat(r2.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 8, 10));
    }
}

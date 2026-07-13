package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.CalculadorPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadorPlazosAdministrativosTest {

    private InMemoryDiaNoComputableRepository repository;
    private CalendarioAdministrativoService calendarioService;
    private CalculadorPlazosAdministrativos calculador;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDiaNoComputableRepository();
        CountingClock clock = CountingClock.startingAt(Instant.parse("2026-07-12T12:00:00Z"));
        calendarioService = new CalendarioAdministrativoService(repository, clock);
        calculador = new CalculadorPlazosAdministrativos(calendarioService);
    }

    @Test
    @DisplayName("1. No cuenta la fecha de origen")
    void no_cuenta_fecha_origen() {
        // Martes -> primer dia computable es miercoles
        LocalDate martes = LocalDate.of(2026, 7, 7);
        LocalDate resultado = calculador.calcularVencimiento(martes, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 7, 8)); // miercoles
    }

    @Test
    @DisplayName("2. Viernes + 1 devuelve sabado")
    void viernes_mas_uno_devuelve_sabado() {
        LocalDate viernes = LocalDate.of(2026, 8, 7);
        LocalDate resultado = calculador.calcularVencimiento(viernes, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 8, 8)); // sabado
    }

    @Test
    @DisplayName("3. Sabado + 1 omite domingo y devuelve lunes")
    void sabado_mas_uno_omite_domingo_devuelve_lunes() {
        LocalDate sabado = LocalDate.of(2026, 8, 8);
        LocalDate resultado = calculador.calcularVencimiento(sabado, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 8, 10)); // lunes
    }

    @Test
    @DisplayName("4. Omite excepcion manual activa")
    void omite_excepcion_manual() {
        LocalDate viernes = LocalDate.of(2026, 7, 10);
        // Viernes marcado como no computable
        CountingClock regClock = CountingClock.startingAt(Instant.parse("2026-07-08T10:00:00Z"));
        CalendarioAdministrativoService svc = new CalendarioAdministrativoService(repository, regClock);
        svc.registrarDiaNoComputable(viernes, TipoDiaNoComputable.FERIADO,
                "Feriado test", OrigenDiaNoComputable.MANUAL, null, "op1");

        // Desde jueves 09, el primer dia computable no es viernes sino sabado 11
        LocalDate jueves = LocalDate.of(2026, 7, 9);
        LocalDate resultado = calculador.calcularVencimiento(jueves, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 7, 11)); // sabado
    }

    @Test
    @DisplayName("5. Omite 1 de enero cruzando anio")
    void omite_primero_enero_cruzando_anio() {
        // 30 dic 2026 como origen (miercoles, no se cuenta)
        // 31 dic (jueves) computa -> 1
        // 1 ene (viernes) NO computa
        // 2 ene (sabado) computa -> 2
        LocalDate origen = LocalDate.of(2026, 12, 30);
        LocalDate resultado = calculador.calcularVencimiento(origen, 2);
        assertThat(resultado).isEqualTo(LocalDate.of(2027, 1, 2));
    }

    @Test
    @DisplayName("6. Omite 1 de mayo")
    void omite_primero_mayo() {
        // 30 abr 2027 (viernes) como origen, no se cuenta
        // 1 may 2027 (sabado): NO computa (regla fija)
        // 2 may 2027 (domingo): NO computa (domingo)
        // 3 may 2027 (lunes): computa -> diasContados = 1
        LocalDate origen = LocalDate.of(2027, 4, 30);
        LocalDate resultado = calculador.calcularVencimiento(origen, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2027, 5, 3));
    }

    @Test
    @DisplayName("7. Omite varias fechas no computables consecutivas")
    void omite_varias_fechas_consecutivas() {
        // Excepciones activas consecutivas (viernes y sabado) seguidas de la regla fija del domingo.
        CountingClock regClock = CountingClock.startingAt(Instant.parse("2026-07-08T10:00:00Z"));
        CalendarioAdministrativoService svc = new CalendarioAdministrativoService(repository, regClock);
        svc.registrarDiaNoComputable(LocalDate.of(2026, 7, 10), TipoDiaNoComputable.FERIADO,
                "Feriado viernes", OrigenDiaNoComputable.MANUAL, null, "op1");
        svc.registrarDiaNoComputable(LocalDate.of(2026, 7, 11), TipoDiaNoComputable.FERIADO,
                "Feriado sabado", OrigenDiaNoComputable.MANUAL, null, "op1");

        // Origen jueves 2026-07-09 (no se cuenta):
        //   viernes 2026-07-10 -> excepcion activa (no computa)
        //   sabado  2026-07-11 -> excepcion activa (no computa)
        //   domingo 2026-07-12 -> regla fija (no computa)
        //   lunes   2026-07-13 -> computa -> diasContados = 1
        // Expected fijado manualmente (no derivado del algoritmo productivo).
        LocalDate origen = LocalDate.of(2026, 7, 9);
        LocalDate resultado = calculador.calcularVencimiento(origen, 1);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 7, 13));
    }

    @Test
    @DisplayName("8. 30 dias desde 2026-07-10 devuelve 2026-08-14")
    void treinta_dias_desde_10_julio() {
        // El resultado fue calculado manualmente contando lun-sab, omitiendo dom
        // Origen: viernes 2026-07-10 (no se cuenta)
        // Primer dia: sabado 11 jul (computa)
        // Domingo 12 NO; lun13 1; mar14 2; mie15 3; jue16 4; vie17 5; sab18 6;
        // dom19 NO; lun20 7; mar21 8; mie22 9; jue23 10; vie24 11; sab25 12;
        // dom26 NO; lun27 13; mar28 14; mie29 15; jue30 16; vie31 17; sab01ago 18;
        // dom02 NO; lun03 19; mar04 20; mie05 21; jue06 22; vie07 23; sab08 24;
        // dom09 NO; lun10 25; mar11 26; mie12 27; jue13 28; vie14 29; sab15 30
        // Wait: let me recount. sab11=1, lun13=2, mar14=3, mie15=4, jue16=5, vie17=6,
        // sab18=7, lun20=8, mar21=9, mie22=10, jue23=11, vie24=12, sab25=13,
        // lun27=14, mar28=15, mie29=16, jue30=17, vie31=18, sab01=19,
        // lun03ago=20, mar04=21, mie05=22, jue06=23, vie07=24, sab08=25,
        // lun10=26, mar11=27, mie12=28, jue13=29, vie14=30
        // Result: 2026-08-14
        LocalDate origen = LocalDate.of(2026, 7, 10);
        LocalDate resultado = calculador.calcularVencimiento(origen, 30);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    @DisplayName("9. fechaOrigen null rechaza")
    void fecha_origen_null_rechaza() {
        assertThatThrownBy(() -> calculador.calcularVencimiento(null, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("10. cantidad 0 rechaza")
    void cantidad_cero_rechaza() {
        assertThatThrownBy(() -> calculador.calcularVencimiento(LocalDate.of(2026, 7, 10), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("11. cantidad mayor a 3650 rechaza")
    void cantidad_mayor_3650_rechaza() {
        assertThatThrownBy(() -> calculador.calcularVencimiento(LocalDate.of(2026, 7, 10), 3651))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("12. Mismo input y mismo calendario producen siempre el mismo resultado")
    void determinista() {
        LocalDate origen = LocalDate.of(2026, 7, 10);
        LocalDate r1 = calculador.calcularVencimiento(origen, 30);
        LocalDate r2 = calculador.calcularVencimiento(origen, 30);
        assertThat(r1).isEqualTo(r2).isEqualTo(LocalDate.of(2026, 8, 14));
    }
}

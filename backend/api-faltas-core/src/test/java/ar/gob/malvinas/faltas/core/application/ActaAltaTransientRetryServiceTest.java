package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.port.ReintentoEsperaPort;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaAltaTransientRetryService;
import ar.gob.malvinas.faltas.core.application.service.ActaAltaIntentoTransaccionalService;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.MariaDbTransientFailureClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ActaAltaTransientRetryService} (Wave 4 Parte 3E §9, §12, §17).
 *
 * <p>{@code ActaService} y {@link ReintentoEsperaPort} se mockean: el objetivo de este test es
 * la politica de reintento (cuantas veces, con que id_tecnico, cuanto espera, cuando propaga),
 * no el caso de uso real del alta. La frontera transaccional real (que cada intento abra una
 * transaccion Spring nueva) se certifica en {@code AltaActaRetryTransactionIT} contra MariaDB
 * real, donde {@code ActaService} es el bean Spring proxied real.
 */
@ExtendWith(MockitoExtension.class)
class ActaAltaTransientRetryServiceTest {

    @Mock
    private ActaAltaIntentoTransaccionalService intentoService;
    @Mock
    private ReintentoEsperaPort espera;

    private final MariaDbTransientFailureClassifier clasificador = new MariaDbTransientFailureClassifier();

    private ActaAltaTransientRetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new ActaAltaTransientRetryService(intentoService, clasificador, espera);
    }

    private static RuntimeException deadlock() {
        SQLException sqlException = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        return new UncategorizedSQLException("update", "UPDATE ...", sqlException);
    }

    private static LabrarActaCommand comandoMinimoSinIdTecnico() {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, 1L, 1L, null, "hecho", "infractor",
                null, null, null, "Juan Perez", null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                SituacionIdentificacionInfractor.DATOS_INSUFICIENTES, null);
    }

    @Test
    void exitoEnElPrimerIntentoNoEsperaNiReintenta() throws InterruptedException {
        ComandoResultado esperado = ComandoResultado.de(1L, "1", "ACTLAB", "ok");
        when(intentoService.intentar(any())).thenReturn(esperado);

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        assertThat(resultado).isEqualTo(esperado);
        verify(intentoService, times(1)).intentar(any());
        verifyNoMoreInteractions(espera);
    }

    @Test
    void generaUnIdTecnicoUnicoYLoReutilizaEnTodosLosIntentosCuandoElComandoNoLoInforma()
            throws InterruptedException {
        when(intentoService.intentar(any()))
                .thenThrow(deadlock())
                .thenReturn(ComandoResultado.de(1L, "1", "ACTLAB", "ok"));

        retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        ArgumentCaptor<LabrarActaCommand> captor = ArgumentCaptor.forClass(LabrarActaCommand.class);
        verify(intentoService, times(2)).intentar(captor.capture());
        List<LabrarActaCommand> intentos = captor.getAllValues();
        assertThat(intentos.get(0).idTecnico()).isNotBlank();
        assertThat(intentos.get(1).idTecnico()).isEqualTo(intentos.get(0).idTecnico());
    }

    @Test
    void conservaElIdTecnicoInformadoPorElCliente() throws InterruptedException {
        when(intentoService.intentar(any()))
                .thenThrow(deadlock())
                .thenReturn(ComandoResultado.de(1L, "1", "ACTLAB", "ok"));

        LabrarActaCommand conIdTecnico = comandoMinimoSinIdTecnico().conIdTecnico("id-tecnico-cliente-001");
        retryService.labrarConReintento(conIdTecnico);

        ArgumentCaptor<LabrarActaCommand> captor = ArgumentCaptor.forClass(LabrarActaCommand.class);
        verify(intentoService, times(2)).intentar(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(cmd -> assertThat(cmd.idTecnico()).isEqualTo("id-tecnico-cliente-001"));
    }

    @Test
    void primerIntentoDeadlockSegundoExito() throws InterruptedException {
        ComandoResultado esperado = ComandoResultado.de(1L, "1", "ACTLAB", "ok");
        when(intentoService.intentar(any())).thenThrow(deadlock()).thenReturn(esperado);

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        assertThat(resultado).isEqualTo(esperado);
        verify(intentoService, times(2)).intentar(any());
        verify(espera, times(1)).esperar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void dosIntentosTransitoriosTerceroExito() throws InterruptedException {
        ComandoResultado esperado = ComandoResultado.de(1L, "1", "ACTLAB", "ok");
        when(intentoService.intentar(any()))
                .thenThrow(deadlock())
                .thenThrow(deadlock())
                .thenReturn(esperado);

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        assertThat(resultado).isEqualTo(esperado);
        verify(intentoService, times(3)).intentar(any());
        verify(espera, times(2)).esperar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void tresIntentosTransitoriosPropaganElErrorSinUnCuartoIntento() throws InterruptedException {
        RuntimeException error1 = deadlock();
        RuntimeException error2 = deadlock();
        RuntimeException error3 = deadlock();
        when(intentoService.intentar(any())).thenThrow(error1).thenThrow(error2).thenThrow(error3);

        assertThatThrownBy(() -> retryService.labrarConReintento(comandoMinimoSinIdTecnico()))
                .isSameAs(error3);

        verify(intentoService, times(3)).intentar(any());
        verify(espera, times(2)).esperar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void errorNoTransitorioNoSeReintenta() throws InterruptedException {
        RuntimeException errorFuncional = new PrecondicionVioladaException("bloque obligatorio ausente");
        when(intentoService.intentar(any())).thenThrow(errorFuncional);

        assertThatThrownBy(() -> retryService.labrarConReintento(comandoMinimoSinIdTecnico()))
                .isSameAs(errorFuncional);

        verify(intentoService, times(1)).intentar(any());
        verifyNoMoreInteractions(espera);
    }

    @Test
    void laEsperaEsIncrementalEntreIntentos() throws InterruptedException {
        when(intentoService.intentar(any()))
                .thenThrow(deadlock())
                .thenThrow(deadlock())
                .thenReturn(ComandoResultado.de(1L, "1", "ACTLAB", "ok"));

        retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        ArgumentCaptor<Long> esperasCapturadas = ArgumentCaptor.forClass(Long.class);
        verify(espera, times(2)).esperar(esperasCapturadas.capture());
        List<Long> esperas = esperasCapturadas.getAllValues();
        assertThat(esperas.get(1)).isGreaterThan(esperas.get(0));
    }

    @Test
    void interrupcionDuranteLaEsperaRestauraElFlagDeInterrupcionYAbortaElReintento()
            throws InterruptedException {
        when(intentoService.intentar(any())).thenThrow(deadlock());
        org.mockito.Mockito.doThrow(new InterruptedException())
                .when(espera).esperar(org.mockito.ArgumentMatchers.anyLong());

        try {
            assertThatThrownBy(() -> retryService.labrarConReintento(comandoMinimoSinIdTecnico()))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Limpia el flag para no afectar otros tests de la misma JVM/hilo de test.
            Thread.interrupted();
        }
        verify(intentoService, times(1)).intentar(any());
    }

    @Test
    void ordenDeLlamadasEsIntentoLuegoEsperaLuegoSiguienteIntento() throws InterruptedException {
        ComandoResultado esperado = ComandoResultado.de(1L, "1", "ACTLAB", "ok");
        when(intentoService.intentar(any())).thenThrow(deadlock()).thenReturn(esperado);

        retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        var orden = inOrder(intentoService, espera);
        orden.verify(intentoService).intentar(any());
        orden.verify(espera).esperar(org.mockito.ArgumentMatchers.anyLong());
        orden.verify(intentoService).intentar(any());
    }

    @Test
    void deadlockLoserSinSqlExceptionEnCadenaTambienSeReintenta() throws InterruptedException {
        ComandoResultado esperado = ComandoResultado.de(1L, "1", "ACTLAB", "ok");
        when(intentoService.intentar(any()))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock", null))
                .thenReturn(esperado);

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimoSinIdTecnico());

        assertThat(resultado).isEqualTo(esperado);
        verify(intentoService, times(2)).intentar(any());
    }
}

package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.port.ReintentoEsperaPort;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.CategoriaFalloTransitorioMariaDb;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.MariaDbTransientFailureClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestador de reintento transaccional del alta ante errores transitorios reconocidos de
 * MariaDB (Wave 4 Parte 3E §9-§13).
 *
 * <h2>Frontera transaccional</h2>
 *
 * <p>Esta clase <b>no</b> lleva {@code @Transactional} propia. Desde Wave 4 Parte 3E-B §3.4/§10
 * cada intento pasa por {@link ActaAltaIntentoTransaccionalService#intentar}, anotado
 * {@code @Transactional(REQUIRES_NEW)}: la transacción nueva por intento ya no depende de que
 * este orquestador se invoque sin transacción ambiente (hasta 3E, si un llamador futuro lo
 * invocaba dentro de una transacción abierta, {@code ActaService.labrar} —
 * {@code REQUIRED} — habría heredado esa transacción ambiente en los tres intentos posibles, en
 * vez de abrir una nueva). {@code REQUIRES_NEW} suspende cualquier transacción ambiente antes de
 * cada intento, exista o no; ninguna transacción fallida se reutiliza para el intento siguiente,
 * y no hay self-invocation en ningún salto (orquestador → intento → {@code ActaService} son tres
 * beans Spring distintos).
 *
 * <p>Cuando {@code intentoService.intentar} lanza una {@code RuntimeException}, la advice
 * transaccional de Spring ya completó el rollback total (incluida la compensación
 * post-transaccional de {@link FotografiaEvidenciaService}, registrada como
 * {@code TransactionSynchronization} y ejecutada sincrónicamente por el motor de transacciones
 * antes de que el control vuelva a este orquestador) <b>antes</b> de que la excepción llegue al
 * bloque {@code catch} de {@link #labrarConReintento}. El intento siguiente arranca sólo después
 * de esa compensación completa: nunca se solapan dos intentos ni dos transacciones.
 *
 * <h2>Idempotencia</h2>
 *
 * <p>El {@code id_tecnico} se resuelve una única vez, <b>antes</b> del primer intento (si el
 * comando no lo informó, se genera un UUID nuevo aquí, no dentro de {@code ActaService.labrar}),
 * y el mismo valor viaja en los tres intentos posibles. Esto es necesario porque
 * {@code ActaService.labrar} genera un UUID nuevo cada vez que recibe {@code idTecnico} nulo o
 * en blanco: sin esta normalización, cada intento del retry crearía una identidad técnica
 * distinta y el pre-check de idempotencia de {@code ActaService.labrar} nunca podría reconocer
 * un intento anterior. Como cada intento fallido revierte totalmente su transacción (sin
 * commit), el intento siguiente no encuentra ningún acta con ese {@code id_tecnico} y ejecuta el
 * alta completa de nuevo; el pre-check de idempotencia sigue protegiendo, dentro de cada
 * intento, contra una llamada de cliente duplicada o una confirmación previa ambigua.
 *
 * <h2>Alcance del reintento</h2>
 *
 * <p>Sólo se reintentan los errores que {@link MariaDbTransientFailureClassifier} reconoce
 * como transitorios (1213 deadlock, 1205 lock wait timeout, o su SQLSTATE equivalente).
 * Cualquier otra {@code RuntimeException} — validaciones, precondiciones, duplicados,
 * autorización, errores de filesystem, errores de sintaxis SQL, etc. — se propaga en el primer
 * intento, sin reintentar.
 */
@Service
public class ActaAltaTransientRetryService {

    private static final Logger log = LoggerFactory.getLogger(ActaAltaTransientRetryService.class);

    /** Intento inicial + máximo 2 reintentos adicionales (Wave 4 Parte 3E §9). */
    static final int MAX_REINTENTOS_ADICIONALES = 2;

    private static final long ESPERA_BASE_MS = 50L;
    private static final long ESPERA_INCREMENTO_POR_INTENTO_MS = 150L;

    private final ActaAltaIntentoTransaccionalService intentoService;
    private final MariaDbTransientFailureClassifier clasificador;
    private final ReintentoEsperaPort espera;

    public ActaAltaTransientRetryService(
            ActaAltaIntentoTransaccionalService intentoService,
            MariaDbTransientFailureClassifier clasificador,
            ReintentoEsperaPort espera) {
        this.intentoService = intentoService;
        this.clasificador = clasificador;
        this.espera = espera;
    }

    /**
     * Ejecuta el alta con reintento automático ante errores transitorios reconocidos de
     * MariaDB. Único punto de entrada productivo del alta desde la capa web
     * ({@code ActaController}): reemplaza la invocación directa a {@code ActaService.labrar}.
     */
    public ComandoResultado labrarConReintento(LabrarActaCommand comandoOriginal) {
        LabrarActaCommand comando = normalizarIdTecnico(comandoOriginal);
        String idTecnico = comando.idTecnico();

        for (int intento = 0; intento <= MAX_REINTENTOS_ADICIONALES; intento++) {
            try {
                ComandoResultado resultado = intentoService.intentar(comando);
                if (intento > 0) {
                    log.info("Alta id_tecnico={} exitosa en el intento {} (tras {} fallo(s)"
                                    + " transitorio(s) reintentado(s)).",
                            idTecnico, intento + 1, intento);
                }
                return resultado;
            } catch (RuntimeException error) {
                manejarFalloDeIntento(idTecnico, intento, error);
            }
        }
        // Inalcanzable: manejarFalloDeIntento siempre relanza en el ultimo intento permitido.
        throw new IllegalStateException(
                "labrarConReintento: fin de bucle de reintento sin resultado ni excepcion.");
    }

    private void manejarFalloDeIntento(String idTecnico, int intento, RuntimeException error) {
        CategoriaFalloTransitorioMariaDb categoria = clasificador.clasificar(error);
        boolean esUltimoIntentoPermitido = intento == MAX_REINTENTOS_ADICIONALES;

        if (categoria == null) {
            log.debug("Alta id_tecnico={} fallo no transitorio en el intento {} ({}); no se"
                            + " reintenta.",
                    idTecnico, intento + 1, error.getClass().getSimpleName());
            throw error;
        }

        if (esUltimoIntentoPermitido) {
            log.warn("Alta id_tecnico={} agoto los {} reintentos adicionales; ultima categoria"
                            + " transitoria={}. Se propaga el error.",
                    idTecnico, MAX_REINTENTOS_ADICIONALES, categoria);
            throw error;
        }

        long esperaMs = calcularEsperaMs(intento);
        log.warn("Alta id_tecnico={} intento {} fallo con categoria transitoria={}; reintentando"
                        + " en {} ms (intento {} de {}).",
                idTecnico, intento + 1, categoria, esperaMs, intento + 2, MAX_REINTENTOS_ADICIONALES + 1);
        esperarAntesDelSiguienteIntento(esperaMs);
    }

    private void esperarAntesDelSiguienteIntento(long milisegundos) {
        try {
            espera.esperar(milisegundos);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Reintento del alta interrumpido durante la espera entre intentos.", interrumpido);
        }
    }

    private long calcularEsperaMs(int intentoFallidoIndiceCero) {
        return ESPERA_BASE_MS + (ESPERA_INCREMENTO_POR_INTENTO_MS * intentoFallidoIndiceCero);
    }

    private LabrarActaCommand normalizarIdTecnico(LabrarActaCommand comando) {
        if (comando.idTecnico() != null && !comando.idTecnico().isBlank()) {
            return comando;
        }
        return comando.conIdTecnico(UUID.randomUUID().toString());
    }
}

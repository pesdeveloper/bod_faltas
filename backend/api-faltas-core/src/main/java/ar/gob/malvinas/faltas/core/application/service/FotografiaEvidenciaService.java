package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.port.AlmacenamientoEvidenciaPort;
import ar.gob.malvinas.faltas.core.application.port.ImagenEvidenciaProcesador;
import ar.gob.malvinas.faltas.core.application.port.ImagenProcesada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.EvidenciaArchivoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinador de incorporacion de fotografias de evidencia (Wave 4 Parte 3D).
 *
 * <p>Unico punto donde se cruzan el pipeline de imagen, el filesystem y la
 * persistencia JDBC de metadata. Dos casos de uso, dos metodos publicos, una
 * sola implementacion interna compartida:
 *
 * <ul>
 *   <li>{@link #incorporarLoteAlta}: se invoca <b>dentro</b> de la transaccion
 *       de {@code ActaService.labrar}. No abre transaccion propia (se ejecuta
 *       en la del llamador). El acta recien insertada en la misma transaccion
 *       es invisible para cualquier otra transaccion (bloqueo implicito de
 *       InnoDB sobre la fila propia hasta el commit), de modo que no hace
 *       falta bloquear nada para asignar {@code orden = 1..N}: no puede haber
 *       otra operacion concurrente sobre un acta que todavia no existe fuera
 *       de esta transaccion.</li>
 *   <li>{@link #incorporarLotePosterior}: transaccion propia. Bloquea la fila
 *       del acta ({@code SELECT ... FOR UPDATE} via
 *       {@link ActaRepository#buscarPorIdConLock}) <b>antes</b> de leer
 *       {@code MAX(orden)}, de modo que dos cargas posteriores concurrentes
 *       sobre la misma acta se serializan: la segunda espera a que la primera
 *       confirme o revierta antes de calcular su propio {@code MAX(orden)+1}
 *       (Parte 3D S5.6/S16). El mismo bloqueo serializa tambien la deteccion
 *       de duplicados por SHA-256 entre esas dos operaciones.</li>
 * </ul>
 *
 * <h2>Coordinacion MariaDB/filesystem (Parte 3D S11)</h2>
 *
 * <pre>
 * 1. procesar todas las imagenes (CPU, sin efectos laterales)
 * 2. rechazar duplicados de lote y duplicados contra existentes (pre-check)
 * 3. escribir cada imagen procesada en un temporal
 * 4. DENTRO de la transaccion: insertar metadata y promover temporal-&gt;final
 *    fotografia por fotografia (si el motor rechaza el INSERT -- por ejemplo
 *    la carrera real de UNIQUE(id_acta, hash_evid) -- se revierten filesystem
 *    los temporales/finales de ESTE lote antes de repropagar)
 * 5. registrar una {@link TransactionSynchronization}: si la transaccion
 *    termina en commit, limpia los temporales; si termina en rollback
 *    (incluido un fallo POSTERIOR a este metodo, dentro de la misma
 *    transaccion), elimina los archivos finales ya promovidos y los
 *    temporales — ningun archivo final sobrevive a un rollback.
 * </pre>
 */
@Service
public class FotografiaEvidenciaService {

    private static final Logger log = LoggerFactory.getLogger(FotografiaEvidenciaService.class);

    private final ActaRepository actaRepository;
    private final ActaEvidenciaRepository evidenciaRepository;
    private final ImagenEvidenciaProcesador procesador;
    private final AlmacenamientoEvidenciaPort almacenamiento;
    private final FaltasClock faltasClock;
    private final int maxCantidadPorOperacion;

    public FotografiaEvidenciaService(
            ActaRepository actaRepository,
            ActaEvidenciaRepository evidenciaRepository,
            ImagenEvidenciaProcesador procesador,
            AlmacenamientoEvidenciaPort almacenamiento,
            FaltasClock faltasClock,
            @Value("${faltas.fotografias.max-cantidad-por-operacion:10}") int maxCantidadPorOperacion) {
        this.actaRepository = actaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.procesador = procesador;
        this.almacenamiento = almacenamiento;
        this.faltasClock = faltasClock;
        this.maxCantidadPorOperacion = maxCantidadPorOperacion;
    }

    /**
     * Incorpora el lote de fotografias del alta inicial. Debe invocarse desde
     * dentro de la transaccion de {@code ActaService.labrar}, con el acta ya
     * insertada (para tener {@code idActa} real) pero sin haber confirmado
     * todavia.
     */
    public List<FalActaEvidencia> incorporarLoteAlta(
            Long idActa, List<FotografiaEntradaAlta> entradas,
            String actor, LocalDateTime ahora, BloqueActual bloqueActual) {
        validarCantidad(entradas, 0);
        return incorporarLote(idActa, entradas, actor, ahora, bloqueActual,
                OrigenIncorporacionEvidencia.ALTA_INICIAL, 0);
    }

    /**
     * Incorpora un lote de fotografias a una acta ya existente, en cualquier
     * etapa del ciclo de vida (Parte 3D S13). Transaccion propia: bloquea la
     * fila del acta antes de calcular el orden.
     */
    @Transactional
    public List<FalActaEvidencia> incorporarLotePosterior(Long idActa, List<FotografiaEntradaAlta> entradas) {
        String actor = exigirActor();
        validarCantidad(entradas, 1);

        FalActa acta = actaRepository.buscarPorIdConLock(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException(idActa));

        LocalDateTime ahora = faltasClock.now();
        int ordenBase = evidenciaRepository.maxOrdenFotografiasPorActa(idActa);
        return incorporarLote(idActa, entradas, actor, ahora, acta.getBloqueActual(),
                OrigenIncorporacionEvidencia.CARGA_POSTERIOR, ordenBase);
    }

    private void validarCantidad(List<FotografiaEntradaAlta> entradas, int minimo) {
        int cantidad = entradas == null ? 0 : entradas.size();
        if (cantidad < minimo) {
            throw new PrecondicionVioladaException(
                    "Esta operacion exige al menos " + minimo + " fotografia(s); se recibieron " + cantidad + ".");
        }
        if (cantidad > maxCantidadPorOperacion) {
            throw new PrecondicionVioladaException(
                    "Maximo " + maxCantidadPorOperacion + " fotografias por operacion; se recibieron " + cantidad + ".");
        }
    }

    private List<FalActaEvidencia> incorporarLote(
            Long idActa, List<FotografiaEntradaAlta> entradas,
            String actor, LocalDateTime ahora, BloqueActual bloqueActual,
            OrigenIncorporacionEvidencia origen, int ordenBase) {
        exigirTransaccionActivaYSincronizada();
        if (entradas == null || entradas.isEmpty()) {
            return List.of();
        }

        // 1. Procesar todas las imagenes: CPU, sin efectos laterales, seguro de re-ejecutar.
        List<ImagenProcesada> procesadas = new ArrayList<>(entradas.size());
        for (FotografiaEntradaAlta entrada : entradas) {
            procesadas.add(procesador.procesar(entrada.contenido(), entrada.nombreOriginalDeclarado()));
        }

        // 2. Duplicados dentro del propio lote.
        Set<String> hashesLote = new HashSet<>();
        for (ImagenProcesada p : procesadas) {
            if (!hashesLote.add(p.sha256Hex())) {
                throw new PrecondicionVioladaException(
                        "Fotografia duplicada dentro del mismo lote (mismo SHA-256 del archivo final procesado).");
            }
        }

        // 3. Duplicados contra fotografias ya existentes de la acta (pre-check legible;
        //    UNIQUE(id_acta, hash_evid) es la autoridad final ante una carrera real).
        for (ImagenProcesada p : procesadas) {
            if (evidenciaRepository.existeFotografiaConHash(idActa, p.sha256Hex())) {
                throw new PrecondicionVioladaException(
                        "Fotografia duplicada: ya existe una fotografia con el mismo SHA-256 en esta acta.");
            }
        }

        // 4. Preparacion temporal ANTES de tocar metadata (Parte 3D S11).
        List<String> clavesTemporales = new ArrayList<>(procesadas.size());
        try {
            for (ImagenProcesada p : procesadas) {
                clavesTemporales.add(almacenamiento.escribirTemporal(p.bytesFinal()));
            }
        } catch (RuntimeException fallaEscrituraTemporal) {
            for (String temp : clavesTemporales) {
                almacenamiento.eliminarTemporal(temp);
            }
            throw fallaEscrituraTemporal;
        }

        // 5. Insertar metadata + promover, una fotografia a la vez, dentro de la transaccion
        //    actual (la abrio el llamador: ActaService.labrar o esta clase con @Transactional).
        List<FalActaEvidencia> resultado = new ArrayList<>(procesadas.size());
        List<String> clavesFinalesPromovidas = new ArrayList<>(procesadas.size());
        try {
            for (int i = 0; i < procesadas.size(); i++) {
                ImagenProcesada p = procesadas.get(i);
                FotografiaEntradaAlta entrada = entradas.get(i);
                int orden = ordenBase + i + 1;
                String claveFinal = almacenamiento.generarClaveFinal("jpg");

                FalActaEvidencia evidencia = new FalActaEvidencia(
                        evidenciaRepository.nextId(), idActa, TipoEvidenciaActa.FOTO,
                        claveFinal, ahora, ahora, actor, p.sha256Hex(),
                        orden, entrada.descripcion(), p.nombreOriginalSaneado(), p.mimeTipoFinal(),
                        (long) p.bytesFinal().length, p.anchoPx(), p.altoPx(),
                        p.fechaCapturaExif(), bloqueActual.codigo(), origen);

                FalActaEvidencia guardada = evidenciaRepository.guardar(evidencia);
                almacenamiento.promover(clavesTemporales.get(i), claveFinal);
                clavesFinalesPromovidas.add(claveFinal);
                resultado.add(guardada);
            }
        } catch (RuntimeException fallaEnLote) {
            // Todavia no hubo commit: compensamos filesystem ahora mismo (el motor revierte la
            // metadata solo, porque la excepcion se propaga sin capturar). Los finales ya
            // promovidos en ESTE lote y los temporales que quedaron sin promover se eliminan
            // los dos, para que ningun archivo (temporal o final) sobreviva a este fallo.
            for (String claveFinal : clavesFinalesPromovidas) {
                almacenamiento.eliminarFinal(claveFinal);
            }
            for (int i = clavesFinalesPromovidas.size(); i < clavesTemporales.size(); i++) {
                almacenamiento.eliminarTemporal(clavesTemporales.get(i));
            }
            throw fallaEnLote;
        }

        registrarCompensacionPostTransaccional(clavesTemporales, clavesFinalesPromovidas);
        return resultado;
    }

    /**
     * Invariante tecnica de infraestructura (Wave 4 Parte 3F-B, hardening "fuera de
     * transaccion"): {@code FotografiaEvidenciaService} productivo solo puede operar dentro de
     * una transaccion Spring real y sincronizada. Fuera de ella no hay forma segura de
     * garantizar la compensacion post-commit/rollback de filesystem que el resto de esta clase
     * asume (ver {@link #registrarCompensacionPostTransaccional}), asi que se rechaza de forma
     * explicita y temprana -- antes de procesar una sola imagen, escribir un temporal o tocar
     * metadata -- en vez de tolerar silenciosamente la ausencia de transaccion como antes de
     * esta parte. No es una regla de negocio visible al usuario final: es una precondicion
     * tecnica que solo puede violarse por un error de programacion (un caller que invoca este
     * servicio fuera del limite {@code @Transactional} de {@code ActaService.labrar} o de
     * {@link #incorporarLotePosterior}), nunca por una entrada de cliente.
     */
    private void exigirTransaccionActivaYSincronizada() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "FotografiaEvidenciaService requiere ejecutarse dentro de una transaccion Spring"
                            + " real y sincronizada (invariante tecnica de infraestructura, Wave 4 Parte"
                            + " 3F-B). No se proceso, persistio ni escribio ninguna fotografia.");
        }
    }

    /**
     * Registra la compensacion que se ejecuta cuando la transaccion ambiente
     * termina (commit o rollback), sin importar que fallo posterior a este
     * metodo la haya causado (Parte 3D S11: "los fallos de cleanup quedan
     * observables y no silenciados"). Se invoca siempre con una transaccion activa y
     * sincronizada ya garantizada por {@link #exigirTransaccionActivaYSincronizada}.
     */
    private void registrarCompensacionPostTransaccional(List<String> temporales, List<String> finales) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    for (String temp : temporales) {
                        almacenamiento.eliminarTemporal(temp);
                    }
                } else {
                    log.warn("Transaccion con fotografias no confirmada (status={}); revirtiendo {} archivo(s)"
                            + " final(es) promovidos y {} temporal(es).", status, finales.size(), temporales.size());
                    for (String fin : finales) {
                        almacenamiento.eliminarFinal(fin);
                    }
                    for (String temp : temporales) {
                        almacenamiento.eliminarTemporal(temp);
                    }
                }
            }
        });
    }

    /**
     * Listado ordenado de fotografias de la acta (Parte 3D S14). No expone
     * {@code storageKey}: el llamador HTTP debe mapear a un DTO sin esa
     * columna.
     */
    public List<FalActaEvidencia> listarFotografias(Long idActa) {
        return evidenciaRepository.listarFotografiasPorActa(idActa);
    }

    /**
     * Resuelve el archivo final de una fotografia para descarga/streaming
     * autenticada (Parte 3D S14). Nunca resuelve una evidencia que no sea
     * {@code tipo_evid = FOTO} de esa acta puntual.
     *
     * @throws EvidenciaArchivoNoEncontradoException si la acta no tiene esa
     *         fotografia (mapea a HTTP 404)
     */
    public FotografiaArchivo leerArchivo(Long idActa, Long idEvidencia) {
        FalActaEvidencia evidencia = evidenciaRepository.buscarFotografiaPorActaYId(idActa, idEvidencia)
                .orElseThrow(() -> new EvidenciaArchivoNoEncontradoException(
                        "No existe una fotografia con ese id para esta acta."));
        byte[] contenido = almacenamiento.leerFinal(evidencia.getStorageKey());
        return new FotografiaArchivo(contenido, evidencia.getMimeTipo(), evidencia.getNombreOriginal());
    }

    /**
     * Contenido y metadata minima necesaria para servir la descarga por HTTP.
     * Nunca incluye {@code storageKey}.
     */
    public record FotografiaArchivo(byte[] contenido, String mimeTipo, String nombreOriginal) {
    }

    private String exigirActor() {
        ActorContext ctx = ActorContextHolder.get();
        if (ctx == null) {
            throw new PrecondicionVioladaException("Operacion humana requiere actor autenticado (JWT sub).");
        }
        return ctx.sub();
    }
}

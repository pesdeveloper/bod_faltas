package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del acta.
 * La implementacion in-memory puede reemplazarse por MariaDB sin tocar dominio.
 *
 * Identidad (DECISION_ID-01): la PK la asigna la persistencia
 * (fal_acta.id es BIGINT AUTO_INCREMENT). El puerto no expone un generador de
 * identificadores y ningun llamador elige la PK. La secuencia no es sin huecos.
 *
 * OCC (DECISION_OCC-01 y DECISION_OCC-01/B): version_row arranca en 0 y avanza
 * exactamente uno por actualizacion exitosa.
 */
public interface ActaRepository {
    /**
     * Alta cuando el acta llega con id nulo, actualizacion optimista cuando llega
     * con id. Devuelve siempre la copia persistida y no muta el argumento.
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException
     *         si el id informado no corresponde a un acta existente
     * @throws ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException
     *         si el acta existe pero su version almacenada no coincide con la entrante
     */
    FalActa guardar(FalActa acta);
    Optional<FalActa> buscarPorId(Long id);
    Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico);

    /**
     * Igual que {@link #buscarPorId}, pero con lectura de bloqueo
     * ({@code SELECT ... FOR UPDATE}) cuando el adapter lo soporta (Wave 4 Parte 3D S5.6/S16).
     * Usada exclusivamente por la incorporacion posterior de fotografias
     * ({@code AgregarFotografiasAActa}) para serializar la asignacion de {@code orden} entre
     * dos operaciones concurrentes sobre la misma acta: bloquear la fila del acta padre antes
     * de leer {@code MAX(orden)} es lo que evita que dos lotes concurrentes calculen el mismo
     * {@code MAX(orden)+1} (Parte 3D S16: "la estrategia debe evitar MAX(orden)+1 sin locking
     * si puede colisionar"). El adapter InMemory delega en la lectura simple: no hay motor
     * real que bloquear.
     */
    Optional<FalActa> buscarPorIdConLock(Long id);

    /**
     * Igual que {@link #buscarPorUuidTecnico}, pero con lectura de bloqueo
     * ({@code SELECT ... FOR UPDATE}) cuando el adapter lo soporta (Wave 4 PARTE-3A §12.5,
     * idempotencia concurrente por {@code id_tecnico}). Usada exclusivamente por
     * {@code ActaService.labrar} para resolver la carrera cuando dos transacciones
     * concurrentes con el mismo {@code id_tecnico} chocan contra el {@code UNIQUE} real de
     * MariaDB: la lectura de bloqueo garantiza ver la fila ya comprometida por la
     * transaccion ganadora, incluso si la lectura simple (snapshot no bloqueante de
     * REPEATABLE READ) todavia no la veria. El adapter InMemory delega en la lectura simple:
     * no hay snapshot que resolver fuera de una base real.
     */
    Optional<FalActa> buscarPorUuidTecnicoConLock(String uuidTecnico);

    /**
     * Lectura completa sin filtro ni limite. <b>Operacion interna.</b>
     *
     * <p>Ningun endpoint operacional puede usarla y no existe una variante publica
     * ilimitada del listado ({@code DECISION-LIST-PAGE-SORT-01}). Su unico consumidor de
     * produccion es {@code ActaRefPersonaDomicilioUsoChecker}, que necesita un predicado
     * disyuntivo sobre los dos domicilios del acta que el filtro tipado conjuntivo de
     * {@link #listarPagina} no puede expresar. Queda inventariada para su migracion en
     * {@code D-PERSISTENCE-PAGING-INDEX-01}.
     *
     * <p><b>No debe usarse para resolver una pagina.</b> Traer todas las filas y recortar en
     * memoria es exactamente lo que la decision prohibe: el recorte tiene que ocurrir en el
     * motor.
     *
     * <p>Ordenada por {@code id} ascendente.
     */
    List<FalActa> listarTodas();

    /**
     * Consulta paginada, filtrada y ordenada en la base
     * ({@code DECISION-LIST-PAGE-SORT-01}).
     *
     * <p><b>No es {@code default} a proposito.</b> Obligar a las dos implementaciones a
     * escribirla es lo que impide que un adapter resuelva la pagina leyendo todo y
     * recortando en memoria sin que nadie lo note; un guardrail lo verifica. Es el mismo
     * criterio con el que {@code ObservacionRepository.listarPorEntidades} no es
     * {@code default} ({@code DECISION-OBS-BATCH-01}).
     *
     * <p>Contrato:
     *
     * <ul>
     *   <li>{@code filtro} nulo equivale a {@link FiltroActa#ninguno()};</li>
     *   <li>{@code pagina} nula equivale a {@link PaginaSolicitada#primeraPagina()};</li>
     *   <li>un orden vacio aplica {@link CampoOrdenActa#ordenPredeterminado()};</li>
     *   <li>todo orden, predeterminado o solicitado, termina con la PK como desempate;</li>
     *   <li>filtro, orden y recorte se ejecutan en el motor: nunca en memoria;</li>
     *   <li>cada acta vuelve con {@code observaciones} compuesta, y el historial se consulta
     *       en una sola operacion batch limitada a los ids de <b>esta</b> pagina;</li>
     *   <li>con {@code incluirTotal} en falso el total puede venir nulo y
     *       {@code haySiguiente} se resuelve leyendo una fila mas.</li>
     * </ul>
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si la pagina solicitada viola el contrato de tamano, orden o precedencia
     */
    PaginaResultado<FalActa> listarPagina(FiltroActa filtro,
                                          PaginaSolicitada<CampoOrdenActa> pagina);
}

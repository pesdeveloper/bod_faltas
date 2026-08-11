package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.DireccionOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Adaptador InMemory de {@code ActaRepository}.
 *
 * <p><b>Restringido a {@code !jdbc} desde WAVE 1A2-B2.</b> Con el perfil {@code jdbc} el
 * puerto lo resuelve {@code JdbcActaRepository} y este bean no se registra, de modo que
 * existe exactamente una implementacion por puerto y no hace falta {@code @Primary} para
 * desambiguar. Sigue siendo el oraculo de paridad del modo no JDBC y de los tests unitarios,
 * que lo construyen directamente.
 *
 * <p><b>Sobre {@code listarPagina}.</b> Filtra, ordena y recorta en memoria porque no hay
 * motor debajo: aca la memoria <i>es</i> el almacenamiento, de modo que no se esta violando la
 * regla de {@code DECISION-LIST-PAGE-SORT-01} que prohibe recortar en memoria despues de
 * cargar una consulta. Este adaptador no promete eficiencia de motor, plan de ejecucion ni
 * ACID; su valor es reproducir el <b>comportamiento observable</b> del contrato: defaults,
 * limites, precedencia de criterios, politica de nulos y desempate por PK.
 */
@Repository
@Profile("!jdbc")
public class InMemoryActaRepository implements ActaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActa> store = new ConcurrentHashMap<>();

    /**
     * Simulacion interna del BIGINT AUTO_INCREMENT de fal_acta. Se consume solo
     * dentro del adaptador: el puerto no expone generador de identificadores y
     * ningun llamador elige la PK (DECISION_ID-01). La secuencia no es sin huecos.
     */
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public FalActa guardar(FalActa acta) {
        if (acta == null) {
            throw new IllegalArgumentException("acta es obligatoria");
        }
        return acta.getId() == null ? insertar(acta) : actualizar(acta);
    }

    private FalActa insertar(FalActa acta) {
        if (acta.getVersionRow() != 0) {
            throw new PrecondicionVioladaException(
                    "Un acta nueva debe llegar con versionRow=0. Recibido: " + acta.getVersionRow());
        }
        Long id = idCounter.getAndIncrement();
        FalActa persistida = acta.conId(id);
        persistida.setVersionRow(0);
        if (store.putIfAbsent(id, persistida) != null) {
            throw new IllegalStateException("Colision de identidad simulada en actas: id=" + id);
        }
        return persistida;
    }

    /**
     * Equivalente InMemory del:
     *   UPDATE fal_acta SET ..., version_row = version_row + 1
     *    WHERE id = :id AND version_row = :versionEsperada
     *
     * compute() garantiza atomicidad del CAS: lectura, comparacion y escritura
     * ocurren en una unica seccion critica por clave. Cuando la funcion lanza, el
     * mapeo queda sin tocar, de modo que ninguna de las dos ramas de fallo escribe.
     *
     * Cero filas afectadas no es un unico caso (DECISION_OCC-01/B): un id que no
     * existe es un 404 funcional y no debe degradarse a conflicto de concurrencia.
     */
    private FalActa actualizar(FalActa acta) {
        Long id = acta.getId();
        FalActa[] resultado = new FalActa[1];
        store.compute(id, (clave, existente) -> {
            if (existente == null) {
                throw new ActaNoEncontradaException(id);
            }
            if (existente.getVersionRow() != acta.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActa", id,
                        existente.getVersionRow(), acta.getVersionRow());
            }
            FalActa copia = acta.copia();
            copia.setVersionRow(existente.getVersionRow() + 1);
            resultado[0] = copia;
            return copia;
        });
        return resultado[0];
    }

    @Override
    public Optional<FalActa> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActa::copia);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnicoConLock(String uuidTecnico) {
        return buscarPorUuidTecnico(uuidTecnico);
    }

    @Override
    public Optional<FalActa> buscarPorIdConLock(Long id) {
        return buscarPorId(id);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico) {
        if (uuidTecnico == null) return Optional.empty();
        return store.values().stream()
                .filter(a -> uuidTecnico.equals(a.getUuidTecnico()))
                .map(FalActa::copia)
                .findFirst();
    }

    /**
     * Orden por {@code id} ascendente, igual que el {@code ORDER BY id ASC} explicito del
     * adaptador JDBC.
     *
     * <p>Antes de WAVE 1A2-B2 esta lectura devolvia el orden de recorrido de un
     * {@code ConcurrentHashMap}, que no esta definido: dos ejecuciones podian diferir y el
     * oraculo de paridad no reproducia el orden que el puerto promete. Se alinea con el
     * contrato en lugar de relajar el contrato.
     */
    @Override
    public List<FalActa> listarTodas() {
        return store.values().stream()
                .sorted(Comparator.comparing(FalActa::getId,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(FalActa::copia)
                .collect(java.util.stream.Collectors.toList());
    }

    // =====================================================================
    // CONSULTA PAGINADA (DECISION-LIST-PAGE-SORT-01)
    // =====================================================================

    @Override
    public PaginaResultado<FalActa> listarPagina(FiltroActa filtro,
                                                 PaginaSolicitada<CampoOrdenActa> pagina) {
        FiltroActa f = filtro == null ? FiltroActa.ninguno() : filtro;
        PaginaSolicitada<CampoOrdenActa> p =
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina;

        List<FalActa> coincidentes = store.values().stream()
                .filter(a -> cumple(a, f))
                .sorted(comparadorDe(p.orden()))
                .map(FalActa::copia)
                .toList();

        long total = coincidentes.size();
        long desde = p.offset();
        if (desde >= total) {
            return new PaginaResultado<>(List.of(), p.page(), p.size(),
                    p.incluirTotal() ? total : null, false);
        }
        int hasta = (int) Math.min(total, desde + p.size());
        List<FalActa> contenido = coincidentes.subList((int) desde, hasta);

        return p.incluirTotal()
                ? PaginaResultado.conTotal(contenido, p.page(), p.size(), total)
                : PaginaResultado.sinTotal(contenido, p.page(), p.size(), hasta < total);
    }

    /** Conjuncion de los predicados informados. Un predicado nulo no filtra. */
    private static boolean cumple(FalActa acta, FiltroActa f) {
        return coincide(f.tipoActa(), acta.getTipoActa())
                && coincide(f.bloqueActual(), acta.getBloqueActual())
                && coincide(f.estadoProcesal(), acta.getEstadoProcesal())
                && coincide(f.situacionAdministrativa(), acta.getSituacionAdministrativa())
                && coincide(f.idDependencia(), acta.getIdDependencia())
                && coincide(f.idInspector(), acta.getIdInspector())
                && coincide(f.nroActa(), acta.getNroActa())
                && noAnterior(acta.getFechaActa(), f.fechaActaDesde())
                && noPosterior(acta.getFechaActa(), f.fechaActaHasta());
    }

    private static boolean coincide(Object esperado, Object real) {
        return esperado == null || esperado.equals(real);
    }

    private static boolean noAnterior(LocalDate valor, LocalDate desde) {
        return desde == null || (valor != null && !valor.isBefore(desde));
    }

    private static boolean noPosterior(LocalDate valor, LocalDate hasta) {
        return hasta == null || (valor != null && !valor.isAfter(hasta));
    }

    /**
     * Comparador equivalente al {@code ORDER BY} del adaptador JDBC: los criterios pedidos o
     * el orden predeterminado, mas la PK como ultimo desempate con la direccion del ultimo
     * criterio.
     *
     * <p>{@code nullsFirst} reproduce la politica de nulos declarada: en {@code ASC} los nulos
     * van primero, y {@code reversed()} sobre ese comparador los deja ultimos en
     * {@code DESC}, que es exactamente el orden nativo de MariaDB.
     */
    private static Comparator<FalActa> comparadorDe(List<CriterioOrden<CampoOrdenActa>> criterios) {
        List<CriterioOrden<CampoOrdenActa>> efectivos =
                criterios.isEmpty() ? CampoOrdenActa.ordenPredeterminado() : criterios;
        Comparator<FalActa> acumulado = null;
        for (CriterioOrden<CampoOrdenActa> criterio : efectivos) {
            Comparator<FalActa> parcial = dirigido(comparadorDeCampo(criterio.campo()),
                    criterio.direccion());
            acumulado = acumulado == null ? parcial : acumulado.thenComparing(parcial);
        }
        DireccionOrden ultima = efectivos.get(efectivos.size() - 1).direccion();
        return acumulado.thenComparing(dirigido(clave(FalActa::getId), ultima));
    }

    private static Comparator<FalActa> dirigido(Comparator<FalActa> base, DireccionOrden dir) {
        return dir == DireccionOrden.DESC ? base.reversed() : base;
    }

    private static <K extends Comparable<? super K>> Comparator<FalActa> clave(
            Function<FalActa, K> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * Los enums se comparan por su codigo canonico y nunca por {@code ordinal()} ni por
     * {@code name()}: es la columna con el codigo la que se ordena en el motor, de modo que
     * comparar por otra cosa produciria una paridad falsa.
     */
    private static Comparator<FalActa> comparadorDeCampo(CampoOrdenActa campo) {
        return switch (campo) {
            case NRO_ACTA -> clave(FalActa::getNroActa);
            case FECHA_ACTA -> clave(FalActa::getFechaActa);
            case FECHA_LABRADO -> clave(FalActa::getFechaLabrado);
            case FECHA_ALTA -> clave(FalActa::getFhAlta);
            case TIPO_ACTA -> clave(a -> a.getTipoActa() == null
                    ? null : Short.valueOf(a.getTipoActa().codigo()));
            case BLOQUE_ACTUAL -> clave(a -> a.getBloqueActual() == null
                    ? null : a.getBloqueActual().codigo());
            case ESTADO_PROCESAL -> clave(a -> a.getEstadoProcesal() == null
                    ? null : a.getEstadoProcesal().codigo());
            case SITUACION_ADMINISTRATIVA -> clave(a -> a.getSituacionAdministrativa() == null
                    ? null : a.getSituacionAdministrativa().codigo());
            case DEPENDENCIA -> clave(FalActa::getIdDependencia);
            case INSPECTOR -> clave(FalActa::getIdInspector);
            case PERSONA_INFRACTOR -> clave(FalActa::getIdPersonaInfractor);
        };
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "actas"; }

    @Override
    public int size() { return store.size(); }
}

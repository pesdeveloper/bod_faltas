package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.DireccionOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Oraculo de paridad de personas.
 *
 * <p><b>Restringido a {@code !jdbc} desde WAVE 1A2-B2.</b> Con el perfil {@code jdbc} el
 * puerto lo resuelve {@code JdbcPersonaRepository} y este bean no se registra.
 *
 * <p>El contador es identidad interna: simula el {@code AUTO_INCREMENT} de MariaDB y
 * ningun llamador lo ve (DECISION_ID-01). La secuencia no se promete sin huecos.
 *
 * <p>Patron P3: {@code guardar} inserta cuando el id es nulo y actualiza cuando esta
 * informado. Un id informado inexistente **no** se inserta. Esa distincion sustituye al
 * upsert incondicional anterior, que solo era posible porque {@code nextId()} entregaba
 * un id antes de guardar.
 *
 * <p><b>Sobre {@code listarPagina}.</b> Filtra, ordena y recorta en memoria porque no hay
 * motor debajo. Reproduce el comportamiento observable del contrato de
 * {@code DECISION-LIST-PAGE-SORT-01} —defaults, limites, precedencia, politica de nulos y
 * desempate por PK— sin prometer eficiencia de motor ni ACID.
 */
@Repository
@Profile("!jdbc")
public class InMemoryPersonaRepository implements PersonaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalPersona> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public synchronized FalPersona guardar(FalPersona persona) {
        if (persona == null) throw new IllegalArgumentException("persona es obligatoria");
        if (persona.getId() == null) {
            FalPersona persistida = persona.conId(idCounter.getAndIncrement());
            store.put(persistida.getId(), persistida);
            return persistida.copia();
        }
        if (!store.containsKey(persona.getId())) {
            throw new PersonaNoEncontradaException(persona.getId());
        }
        FalPersona actualizada = persona.copia();
        store.put(actualizada.getId(), actualizada);
        return actualizada.copia();
    }

    @Override
    public Optional<FalPersona> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalPersona::copia);
    }

    @Override
    public List<FalPersona> listarTodas() {
        return ordenadasPorId(store.values().stream());
    }

    @Override
    public List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc) {
        return ordenadasPorId(store.values().stream()
                .filter(p -> tipoDoc == p.getTipoDoc()
                        && nroDoc != null && nroDoc.equals(p.getNroDoc())));
    }

    @Override
    public List<FalPersona> buscarPorIdSujBie(Integer idSuj, Integer idBie) {
        return ordenadasPorId(store.values().stream()
                .filter(p -> idSuj != null && idSuj.equals(p.getIdSuj())
                        && idBie != null && idBie.equals(p.getIdBie())));
    }

    // =====================================================================
    // CONSULTA PAGINADA (DECISION-LIST-PAGE-SORT-01)
    // =====================================================================

    @Override
    public PaginaResultado<FalPersona> listarPagina(FiltroPersona filtro,
                                                    PaginaSolicitada<CampoOrdenPersona> pagina) {
        FiltroPersona f = filtro == null ? FiltroPersona.ninguno() : filtro;
        PaginaSolicitada<CampoOrdenPersona> p =
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina;

        List<FalPersona> coincidentes = store.values().stream()
                .filter(persona -> cumple(persona, f))
                .sorted(comparadorDe(p.orden()))
                .map(FalPersona::copia)
                .toList();

        long total = coincidentes.size();
        long desde = p.offset();
        if (desde >= total) {
            return new PaginaResultado<>(List.of(), p.page(), p.size(),
                    p.incluirTotal() ? total : null, false);
        }
        int hasta = (int) Math.min(total, desde + p.size());
        List<FalPersona> contenido = coincidentes.subList((int) desde, hasta);

        return p.incluirTotal()
                ? PaginaResultado.conTotal(contenido, p.page(), p.size(), total)
                : PaginaResultado.sinTotal(contenido, p.page(), p.size(), hasta < total);
    }

    /** Conjuncion de los predicados informados. Un predicado nulo no filtra. */
    private static boolean cumple(FalPersona persona, FiltroPersona f) {
        return coincide(f.tipoPersona(), persona.getTipoPersona())
                && coincide(f.tipoDocumento(), persona.getTipoDoc())
                && coincide(f.nroDoc(), persona.getNroDoc())
                && coincide(f.idSuj(), persona.getIdSuj())
                && coincide(f.idBie(), persona.getIdBie())
                && noAnterior(persona.getFhAlta(), f.fhAltaDesde())
                && noPosterior(persona.getFhAlta(), f.fhAltaHasta());
    }

    private static boolean coincide(Object esperado, Object real) {
        return esperado == null || esperado.equals(real);
    }

    private static boolean noAnterior(LocalDateTime valor, LocalDateTime desde) {
        return desde == null || (valor != null && !valor.isBefore(desde));
    }

    private static boolean noPosterior(LocalDateTime valor, LocalDateTime hasta) {
        return hasta == null || (valor != null && !valor.isAfter(hasta));
    }

    /**
     * Comparador equivalente al {@code ORDER BY} del adaptador JDBC, con la PK como ultimo
     * desempate en la direccion del ultimo criterio. {@code nullsFirst} mas
     * {@code reversed()} reproduce la politica de nulos nativa de MariaDB.
     */
    private static Comparator<FalPersona> comparadorDe(
            List<CriterioOrden<CampoOrdenPersona>> criterios) {
        List<CriterioOrden<CampoOrdenPersona>> efectivos =
                criterios.isEmpty() ? CampoOrdenPersona.ordenPredeterminado() : criterios;
        Comparator<FalPersona> acumulado = null;
        for (CriterioOrden<CampoOrdenPersona> criterio : efectivos) {
            Comparator<FalPersona> parcial =
                    dirigido(comparadorDeCampo(criterio.campo()), criterio.direccion());
            acumulado = acumulado == null ? parcial : acumulado.thenComparing(parcial);
        }
        DireccionOrden ultima = efectivos.get(efectivos.size() - 1).direccion();
        return acumulado.thenComparing(dirigido(clave(FalPersona::getId), ultima));
    }

    private static Comparator<FalPersona> dirigido(
            Comparator<FalPersona> base, DireccionOrden dir) {
        return dir == DireccionOrden.DESC ? base.reversed() : base;
    }

    private static <K extends Comparable<? super K>> Comparator<FalPersona> clave(
            Function<FalPersona, K> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /** Los enums se comparan por su codigo canonico, nunca por {@code ordinal()}. */
    private static Comparator<FalPersona> comparadorDeCampo(CampoOrdenPersona campo) {
        return switch (campo) {
            case NOMBRE_MOSTRAR -> clave(FalPersona::getNombreMostrar);
            case APELLIDO -> clave(FalPersona::getApellido);
            case RAZON_SOCIAL -> clave(FalPersona::getRazonSocial);
            case TIPO_DOCUMENTO -> clave(persona -> persona.getTipoDoc() == null
                    ? null : Short.valueOf(persona.getTipoDoc().codigo()));
            case NUMERO_DOCUMENTO -> clave(FalPersona::getNroDoc);
            case TIPO_PERSONA -> clave(persona -> persona.getTipoPersona() == null
                    ? null : Short.valueOf(persona.getTipoPersona().codigo()));
            case FECHA_ALTA -> clave(FalPersona::getFhAlta);
        };
    }

    /** Seed: recibe personas ya construidas con IDs estables. Reposiciona el contador. */
    public synchronized void cargarSeed(List<FalPersona> personas) {
        long maxId = 0;
        for (FalPersona p : personas) {
            if (p.getId() == null)
                throw new IllegalArgumentException("cargarSeed exige personas con id informado");
            store.put(p.getId(), p.copia());
            if (p.getId() > maxId) maxId = p.getId();
        }
        if (maxId >= idCounter.get()) idCounter.set(maxId + 1);
    }

    @Override
    public synchronized void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "personas"; }

    @Override
    public int size() { return store.size(); }

    /**
     * Orden explicito por id ascendente. El recorrido de {@code ConcurrentHashMap} no
     * tiene orden definido, y el contrato del puerto promete uno.
     */
    private static List<FalPersona> ordenadasPorId(java.util.stream.Stream<FalPersona> personas) {
        return personas
                .sorted(Comparator.comparing(FalPersona::getId))
                .map(FalPersona::copia)
                .collect(Collectors.toList());
    }
}

package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryNormativaRepository implements NormativaRepository {

    private final List<FalNormativaFaltas> normativas = new CopyOnWriteArrayList<>();
    private final List<FalArticuloNormativaFaltas> articulos = new CopyOnWriteArrayList<>();
    private final List<FalDependenciaNormativa> relaciones = new CopyOnWriteArrayList<>();

    @Override
    public FalNormativaFaltas guardarNormativa(FalNormativaFaltas n) {
        normativas.removeIf(x -> x.getId().equals(n.getId()));
        normativas.add(n);
        return n;
    }

    @Override
    public Optional<FalNormativaFaltas> findNormativaById(Long id) {
        return normativas.stream().filter(n -> id.equals(n.getId())).findFirst();
    }

    @Override
    public Optional<FalNormativaFaltas> findNormativaByCodigoYVersion(String codigoNorma, int versionNorma) {
        return normativas.stream()
                .filter(n -> codigoNorma.equals(n.getCodigoNorma()) && n.getVersionNorma() == versionNorma)
                .findFirst();
    }

    @Override
    public boolean existsNormativaByCodigoYVersion(String codigoNorma, int versionNorma) {
        return normativas.stream()
                .anyMatch(n -> codigoNorma.equals(n.getCodigoNorma()) && n.getVersionNorma() == versionNorma);
    }

    @Override
    public List<FalNormativaFaltas> findAllNormativasActivas() {
        return normativas.stream().filter(FalNormativaFaltas::isSiActiva).toList();
    }

    @Override
    public FalArticuloNormativaFaltas guardarArticulo(FalArticuloNormativaFaltas a) {
        articulos.removeIf(x -> x.getId().equals(a.getId()));
        articulos.add(a);
        return a;
    }

    @Override
    public Optional<FalArticuloNormativaFaltas> findArticuloById(Long id) {
        return articulos.stream().filter(a -> id.equals(a.getId())).findFirst();
    }

    @Override
    public Optional<FalArticuloNormativaFaltas> findArticuloByNormativaYCodigo(
            Long normativaId, String codigoArticulo, int versionArticulo) {
        return articulos.stream()
                .filter(a -> normativaId.equals(a.getNormativaId())
                        && codigoArticulo.equals(a.getCodigoArticulo())
                        && a.getVersionArticulo() == versionArticulo)
                .findFirst();
    }

    @Override
    public boolean existsArticuloByNormativaYCodigo(
            Long normativaId, String codigoArticulo, int versionArticulo) {
        return articulos.stream()
                .anyMatch(a -> normativaId.equals(a.getNormativaId())
                        && codigoArticulo.equals(a.getCodigoArticulo())
                        && a.getVersionArticulo() == versionArticulo);
    }

    @Override
    public List<FalArticuloNormativaFaltas> findArticulosByNormativa(Long normativaId) {
        return articulos.stream().filter(a -> normativaId.equals(a.getNormativaId())).toList();
    }

    @Override
    public List<FalArticuloNormativaFaltas> findArticulosActivosByNormativa(Long normativaId) {
        return articulos.stream()
                .filter(a -> normativaId.equals(a.getNormativaId()) && a.isSiActivo())
                .toList();
    }

    @Override
    public FalDependenciaNormativa guardarDependenciaNormativa(FalDependenciaNormativa rel) {
        relaciones.removeIf(r ->
                r.getIdDep().equals(rel.getIdDep())
                        && r.getVerDep() == rel.getVerDep()
                        && r.getNormativaId().equals(rel.getNormativaId()));
        relaciones.add(rel);
        return rel;
    }

    @Override
    public Optional<FalDependenciaNormativa> findDependenciaNormativa(
            Long idDep, int verDep, Long normativaId) {
        return relaciones.stream()
                .filter(r -> idDep.equals(r.getIdDep())
                        && r.getVerDep() == verDep
                        && normativaId.equals(r.getNormativaId()))
                .findFirst();
    }

    @Override
    public boolean existsDependenciaNormativaActiva(Long idDep, int verDep, Long normativaId) {
        return relaciones.stream()
                .anyMatch(r -> idDep.equals(r.getIdDep())
                        && r.getVerDep() == verDep
                        && normativaId.equals(r.getNormativaId())
                        && r.isSiActiva());
    }

    @Override
    public List<FalDependenciaNormativa> findNormativasByDepVersion(Long idDep, int verDep) {
        return relaciones.stream()
                .filter(r -> idDep.equals(r.getIdDep()) && r.getVerDep() == verDep)
                .toList();
    }

    @Override
    public List<FalDependenciaNormativa> findNormativasActivasByDepVersion(Long idDep, int verDep) {
        return relaciones.stream()
                .filter(r -> idDep.equals(r.getIdDep()) && r.getVerDep() == verDep && r.isSiActiva())
                .toList();
    }
}

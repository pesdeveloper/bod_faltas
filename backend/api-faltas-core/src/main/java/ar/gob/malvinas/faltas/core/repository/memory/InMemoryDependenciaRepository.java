package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryDependenciaRepository implements DependenciaRepository {

    private final List<FalDependencia> dependencias = new CopyOnWriteArrayList<>();
    private final List<FalDependenciaVersion> versiones = new CopyOnWriteArrayList<>();

    @Override
    public FalDependencia guardar(FalDependencia dep) {
        dependencias.removeIf(d -> d.getIdDep().equals(dep.getIdDep()));
        dependencias.add(dep);
        return dep;
    }

    @Override
    public Optional<FalDependencia> findById(Long idDep) {
        return dependencias.stream()
                .filter(d -> idDep.equals(d.getIdDep()))
                .findFirst();
    }

    @Override
    public List<FalDependencia> findAllActivas() {
        return dependencias.stream()
                .filter(FalDependencia::isSiActiva)
                .toList();
    }

    @Override
    public boolean existsByCodDep(String codDep) {
        return dependencias.stream()
                .anyMatch(d -> codDep.equals(d.getCodDep()));
    }

    @Override
    public Optional<FalDependencia> findByCodDep(String codDep) {
        return dependencias.stream()
                .filter(d -> codDep != null && codDep.equals(d.getCodDep()))
                .findFirst();
    }

    @Override
    public FalDependenciaVersion guardarVersion(FalDependenciaVersion version) {
        versiones.removeIf(v ->
                v.getIdDep().equals(version.getIdDep()) && v.getVerDep() == version.getVerDep());
        versiones.add(version);
        return version;
    }

    @Override
    public List<FalDependenciaVersion> findVersionesByDep(Long idDep) {
        return versiones.stream()
                .filter(v -> idDep.equals(v.getIdDep()))
                .toList();
    }

    @Override
    public Optional<FalDependenciaVersion> findVersionVigente(Long idDep, LocalDate fecha) {
        return versiones.stream()
                .filter(v -> idDep.equals(v.getIdDep()) && v.esVigenteEn(fecha))
                .findFirst();
    }

    @Override
    public int maxVerDep(Long idDep) {
        return versiones.stream()
                .filter(v -> idDep.equals(v.getIdDep()))
                .mapToInt(FalDependenciaVersion::getVerDep)
                .max()
                .orElse(0);
    }
}
package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryInspectorRepository implements InspectorRepository {

    private final List<FalInspector> inspectores = new CopyOnWriteArrayList<>();
    private final List<FalInspectorVersion> versiones = new CopyOnWriteArrayList<>();

    @Override
    public FalInspector guardar(FalInspector inspector) {
        inspectores.removeIf(i -> i.getIdInsp().equals(inspector.getIdInsp()));
        inspectores.add(inspector);
        return inspector;
    }

    @Override
    public Optional<FalInspector> findById(Long idInsp) {
        return inspectores.stream()
                .filter(i -> idInsp.equals(i.getIdInsp()))
                .findFirst();
    }

    @Override
    public boolean existsByIdUser(String idUser) {
        return inspectores.stream()
                .anyMatch(i -> idUser.equals(i.getIdUser()));
    }

    @Override
    public List<FalInspector> findAllActivos() {
        return inspectores.stream()
                .filter(FalInspector::isSiActivo)
                .toList();
    }

    @Override
    public FalInspectorVersion guardarVersion(FalInspectorVersion version) {
        versiones.removeIf(v ->
                v.getIdInsp().equals(version.getIdInsp()) && v.getVerInsp() == version.getVerInsp());
        versiones.add(version);
        return version;
    }

    @Override
    public List<FalInspectorVersion> findVersionesByInsp(Long idInsp) {
        return versiones.stream()
                .filter(v -> idInsp.equals(v.getIdInsp()))
                .toList();
    }

    @Override
    public Optional<FalInspectorVersion> findVersionVigente(Long idInsp, LocalDate fecha) {
        return versiones.stream()
                .filter(v -> idInsp.equals(v.getIdInsp()) && v.esVigenteEn(fecha))
                .findFirst();
    }

    @Override
    public int maxVerInsp(Long idInsp) {
        return versiones.stream()
                .filter(v -> idInsp.equals(v.getIdInsp()))
                .mapToInt(FalInspectorVersion::getVerInsp)
                .max()
                .orElse(0);
    }
}

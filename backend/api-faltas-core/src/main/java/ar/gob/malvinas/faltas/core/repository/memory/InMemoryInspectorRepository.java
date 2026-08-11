package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Retirada del perfil {@code jdbc} en Wave 4 PARTE-3A: reemplazada por
 * {@link ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcInspectorRepository}.
 *
 * <p>Preseeda inspector id=1, activo y habilitado para la dependencia seed id=1 de
 * {@link InMemoryDependenciaRepository}, por el mismo motivo documentado allí (valor por
 * defecto universal de {@code LabrarActaCommand}, Wave 4 PARTE-3A §10).
 */
@Repository
@Profile("!jdbc")
public class InMemoryInspectorRepository implements InspectorRepository {

    private static final LocalDateTime FH_ALTA_SEED = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDate VIGENCIA_AMPLIA = LocalDate.of(2000, 1, 1);

    private final List<FalInspector> inspectores = new CopyOnWriteArrayList<>();
    private final List<FalInspectorVersion> versiones = new CopyOnWriteArrayList<>();

    public InMemoryInspectorRepository() {
        FalInspector seed = new FalInspector(
                1L, "00000000-0000-0000-0000-000000000001", 1,
                "INSPECTOR_SEED_NO_PRODUCTIVO", FH_ALTA_SEED, "SEED_NO_PRODUCTIVO");
        inspectores.add(seed);
        versiones.add(new FalInspectorVersion(1L, 1, 1, "INSPECTOR_SEED_NO_PRODUCTIVO", 1L, 1, VIGENCIA_AMPLIA));
    }

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

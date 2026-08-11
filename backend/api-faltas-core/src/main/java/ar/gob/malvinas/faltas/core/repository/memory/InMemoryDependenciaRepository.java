package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Retirada del perfil {@code jdbc} en Wave 4 PARTE-3A: reemplazada por
 * {@link ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcDependenciaRepository}.
 *
 * <p>Preseeda dependencia id=1 activa, sin tipoActa restringido, vigente desde una fecha
 * fija amplia (no {@code FaltasClock.now()}: es literal, no lectura del reloj — permitido
 * por {@code FaltasClockUsageGuardTest}). {@code LabrarActaCommand} usa idDependencia=1L
 * como valor por defecto cuando el comando no lo informa (incluyendo el circuito HTTP real
 * con {@code idDependencia}/{@code idInspector} no numéricos, ver
 * {@code LabrarActaCommand(String,...)}); Wave 4 PARTE-3A §10 exige validar dependencia
 * antes de numerar, y este seed universal evita que esa validación, nueva en PARTE-3A,
 * rompa el circuito HTTP/Spring-context (perfil default, sin JDBC) que no seedea nada.
 */
@Repository
@Profile("!jdbc")
public class InMemoryDependenciaRepository implements DependenciaRepository {

    private static final LocalDateTime FH_ALTA_SEED = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDate VIGENCIA_AMPLIA = LocalDate.of(2000, 1, 1);

    private final List<FalDependencia> dependencias = new CopyOnWriteArrayList<>();
    private final List<FalDependenciaVersion> versiones = new CopyOnWriteArrayList<>();

    public InMemoryDependenciaRepository() {
        FalDependencia seed = new FalDependencia(1L, "DEPENDENCIA_SEED_NO_PRODUCTIVA", FH_ALTA_SEED, "SEED_NO_PRODUCTIVO");
        dependencias.add(seed);
        versiones.add(new FalDependenciaVersion(1L, 1, "DEPENDENCIA_SEED_NO_PRODUCTIVA", null, null, null, VIGENCIA_AMPLIA));
    }

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

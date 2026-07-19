package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;
import ar.gob.malvinas.faltas.core.repository.FirmanteRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryFirmanteRepository implements FirmanteRepository {

    private final List<FalFirmante> firmantes = new CopyOnWriteArrayList<>();
    private final List<FalFirmanteVersion> versiones = new CopyOnWriteArrayList<>();
    private final List<FalFirmanteVersionHabilitacion> habilitaciones = new CopyOnWriteArrayList<>();

    @Override
    public FalFirmante guardar(FalFirmante firmante) {
        firmantes.removeIf(f -> f.getIdFirmante().equals(firmante.getIdFirmante()));
        firmantes.add(firmante);
        return firmante;
    }

    @Override
    public Optional<FalFirmante> findById(Long idFirmante) {
        return firmantes.stream()
                .filter(f -> idFirmante.equals(f.getIdFirmante()))
                .findFirst();
    }

    @Override
    public Optional<FalFirmante> findByIdUser(String idUser) {
        return firmantes.stream()
                .filter(f -> idUser.equals(f.getIdUser()))
                .findFirst();
    }

    @Override
    public boolean existsByIdUser(String idUser) {
        return firmantes.stream().anyMatch(f -> idUser.equals(f.getIdUser()));
    }

    @Override
    public List<FalFirmante> findAllActivos() {
        return firmantes.stream()
                .filter(FalFirmante::isSiActivo)
                .toList();
    }

    @Override
    public FalFirmanteVersion guardarVersion(FalFirmanteVersion version) {
        versiones.removeIf(v ->
                v.getIdFirmante().equals(version.getIdFirmante())
                && v.getVerFirmante() == version.getVerFirmante());
        versiones.add(version);
        return version;
    }

    @Override
    public List<FalFirmanteVersion> findVersionesByFirmante(Long idFirmante) {
        return versiones.stream()
                .filter(v -> idFirmante.equals(v.getIdFirmante()))
                .toList();
    }

    @Override
    public Optional<FalFirmanteVersion> findVersionVigente(Long idFirmante, LocalDate fecha) {
        return versiones.stream()
                .filter(v -> idFirmante.equals(v.getIdFirmante()) && v.esVigenteEn(fecha))
                .findFirst();
    }

    @Override
    public Optional<FalFirmanteVersion> findVersionByFirmanteAndVer(Long idFirmante, int verFirmante) {
        return versiones.stream()
                .filter(v -> idFirmante.equals(v.getIdFirmante()) && v.getVerFirmante() == verFirmante)
                .findFirst();
    }

    @Override
    public int maxVerFirmante(Long idFirmante) {
        return versiones.stream()
                .filter(v -> idFirmante.equals(v.getIdFirmante()))
                .mapToInt(FalFirmanteVersion::getVerFirmante)
                .max()
                .orElse(0);
    }

    @Override
    public FalFirmanteVersionHabilitacion guardarHabilitacion(FalFirmanteVersionHabilitacion hab) {
        habilitaciones.removeIf(h ->
                h.getIdFirmante().equals(hab.getIdFirmante())
                && h.getVerFirmante() == hab.getVerFirmante()
                && h.getTipoDocu() == hab.getTipoDocu()
                && h.getRolFirmaReq() == hab.getRolFirmaReq());
        habilitaciones.add(hab);
        return hab;
    }

    @Override
    public List<FalFirmanteVersionHabilitacion> findHabilitacionesByVersion(Long idFirmante, int verFirmante) {
        return habilitaciones.stream()
                .filter(h -> idFirmante.equals(h.getIdFirmante()) && h.getVerFirmante() == verFirmante)
                .toList();
    }

    @Override
    public Optional<FalFirmanteVersionHabilitacion> findHabilitacionActiva(Long idFirmante, int verFirmante,
                                                                             Short tipoDocu, Short rolFirmaReq) {
        return habilitaciones.stream()
                .filter(h -> idFirmante.equals(h.getIdFirmante())
                        && h.getVerFirmante() == verFirmante
                        && h.getTipoDocu() == tipoDocu
                        && h.getRolFirmaReq() == rolFirmaReq
                        && h.isSiActivo())
                .findFirst();
    }

    @Override
    public boolean existeHabilitacionActiva(Long idFirmante, int verFirmante,
                                            Short tipoDocu, Short rolFirmaReq) {
        return findHabilitacionActiva(idFirmante, verFirmante, tipoDocu, rolFirmaReq).isPresent();
    }
}

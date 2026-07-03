package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.CrearInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarInspectorCommand;
import ar.gob.malvinas.faltas.core.domain.exception.InspectorNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor de administracion de inspectores.
 *
 * Reglas del modelo productivo:
 * - idUser obligatorio y unico (fal_inspector.id_user).
 * - legajoInsp obligatorio y mayor a 0 (fal_inspector.legajo_insp INT).
 * - nomInsp obligatorio (fal_inspector.nom_insp).
 * - idDep + verDep obligatorios; la version de dependencia debe existir y estar activa.
 * - Al crear inspector: verInsp = 1.
 * - Al versionar: verInsp = max(verInsp) + 1; version anterior pierde siActivo.
 * - El inspector no tiene tipoActa propio; se deriva desde FalDependenciaVersion.
 *
 * Slice 8A-2: implementacion in-memory. Slice 9: reemplazar repositorios por JDBC.
 */
@Service
public class InspectorService {

    private final InspectorRepository inspectorRepository;
    private final DependenciaRepository dependenciaRepository;
    private final AtomicLong secuencia = new AtomicLong(1);

    public InspectorService(InspectorRepository inspectorRepository,
                            DependenciaRepository dependenciaRepository) {
        this.inspectorRepository = inspectorRepository;
        this.dependenciaRepository = dependenciaRepository;
    }

    // -------------------------------------------------------------------------
    // Crear inspector
    // -------------------------------------------------------------------------

    public FalInspector crear(CrearInspectorCommand cmd) {
        validarIdUser(cmd.idUser());
        validarIdUserUnico(cmd.idUser());
        validarLegajoInsp(cmd.legajoInsp());
        validarNomInsp(cmd.nomInsp());
        validarDependenciaVersion(cmd.idDep(), cmd.verDep());

        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : LocalDate.now();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";

        Long idInsp = secuencia.getAndIncrement();
        FalInspector inspector = new FalInspector(
                idInsp, cmd.idUser(), cmd.legajoInsp(), cmd.nomInsp(),
                LocalDateTime.now(), idUserAlta);
        inspectorRepository.guardar(inspector);

        FalInspectorVersion version = new FalInspectorVersion(
                idInsp, 1, cmd.legajoInsp(), cmd.nomInsp(),
                cmd.idDep(), cmd.verDep(), fhVigDesde);
        inspectorRepository.guardarVersion(version);

        return inspector;
    }

    // -------------------------------------------------------------------------
    // Versionar inspector
    // -------------------------------------------------------------------------

    public FalInspectorVersion versionar(VersionarInspectorCommand cmd) {
        FalInspector inspector = inspectorRepository.findById(cmd.idInsp())
                .orElseThrow(() -> new InspectorNoEncontradoException(cmd.idInsp()));

        validarLegajoInsp(cmd.legajoInsp());
        validarNomInsp(cmd.nomInsp());
        validarDependenciaVersion(cmd.idDep(), cmd.verDep());

        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : LocalDate.now();

        inspectorRepository.findVersionVigente(cmd.idInsp(), fhVigDesde).ifPresent(anterior -> {
            anterior.setSiActivo(false);
            anterior.setFhVigHasta(fhVigDesde);
            inspectorRepository.guardarVersion(anterior);
        });

        int nuevaVer = inspectorRepository.maxVerInsp(cmd.idInsp()) + 1;

        FalInspectorVersion nuevaVersion = new FalInspectorVersion(
                cmd.idInsp(), nuevaVer, cmd.legajoInsp(), cmd.nomInsp(),
                cmd.idDep(), cmd.verDep(), fhVigDesde);
        inspectorRepository.guardarVersion(nuevaVersion);

        inspector.setLegajoInsp(cmd.legajoInsp());
        inspector.setNomInsp(cmd.nomInsp());
        inspectorRepository.guardar(inspector);

        return nuevaVersion;
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    public FalInspector obtener(Long idInsp) {
        return inspectorRepository.findById(idInsp)
                .orElseThrow(() -> new InspectorNoEncontradoException(idInsp));
    }

    public List<FalInspector> listarActivos() {
        return inspectorRepository.findAllActivos();
    }

    // -------------------------------------------------------------------------
    // Validaciones
    // -------------------------------------------------------------------------

    private void validarIdUser(String idUser) {
        if (idUser == null || idUser.isBlank()) {
            throw new PrecondicionVioladaException("idUser es obligatorio.");
        }
    }

    private void validarIdUserUnico(String idUser) {
        if (inspectorRepository.existsByIdUser(idUser)) {
            throw new PrecondicionVioladaException(
                    "Ya existe un inspector con idUser: " + idUser);
        }
    }

    private void validarLegajoInsp(Integer legajoInsp) {
        if (legajoInsp == null || legajoInsp <= 0) {
            throw new PrecondicionVioladaException("legajoInsp es obligatorio y debe ser mayor a 0.");
        }
    }

    private void validarNomInsp(String nomInsp) {
        if (nomInsp == null || nomInsp.isBlank()) {
            throw new PrecondicionVioladaException("nomInsp es obligatorio.");
        }
    }

    private void validarDependenciaVersion(Long idDep, Integer verDep) {
        if (idDep == null) {
            throw new PrecondicionVioladaException("idDep es obligatorio.");
        }
        if (verDep == null || verDep <= 0) {
            throw new PrecondicionVioladaException(
                    "verDep es obligatorio y debe ser mayor a 0.");
        }
        dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "La dependencia no existe: " + idDep));
        List<FalDependenciaVersion> versiones = dependenciaRepository.findVersionesByDep(idDep);
        FalDependenciaVersion depVersion = versiones.stream()
                .filter(v -> v.getVerDep() == verDep)
                .findFirst()
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Version de dependencia no encontrada: idDep=" + idDep + " verDep=" + verDep));
        if (!depVersion.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "La version de dependencia no esta activa: idDep=" + idDep + " verDep=" + verDep);
        }
    }
}

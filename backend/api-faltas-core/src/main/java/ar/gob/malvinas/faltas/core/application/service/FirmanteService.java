package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.AgregarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.DesactivarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarFirmanteCommand;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmanteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor de administracion de firmantes y sus habilitaciones documentales.
 *
 * Reglas del modelo productivo:
 * - idUser obligatorio y unico (fal_firmante.id_user).
 * - nomFirmante obligatorio.
 * - rolFirmante descriptivo/opcional; no autoriza documentos.
 * - La autorizacion documental real se define en FalFirmanteVersionHabilitacion.
 * - Al crear firmante: verFirmante = 1.
 * - Al versionar: verFirmante = max(verFirmante) + 1; version anterior pierde siActivo.
 * - Nueva version nace sin habilitaciones; deben agregarse explicitamente.
 * - idDep/verDep nullable; si se informa, deben existir y estar activos.
 *
 * Slice 8A-3: implementacion in-memory. Slice 9: reemplazar repositorios por JDBC.
 */
@Service
public class FirmanteService {

    private final FirmanteRepository firmanteRepository;
    private final DependenciaRepository dependenciaRepository;
    private final AtomicLong secuencia = new AtomicLong(1);

    public FirmanteService(FirmanteRepository firmanteRepository,
                           DependenciaRepository dependenciaRepository) {
        this.firmanteRepository = firmanteRepository;
        this.dependenciaRepository = dependenciaRepository;
    }

    // -------------------------------------------------------------------------
    // Crear firmante
    // -------------------------------------------------------------------------

    public FalFirmante crear(CrearFirmanteCommand cmd) {
        validarIdUser(cmd.idUser());
        validarIdUserUnico(cmd.idUser());
        validarNomFirmante(cmd.nomFirmante());
        validarFhVigDesde(cmd.fhVigDesde());
        validarParDependencia(cmd.idDep(), cmd.verDep());

        LocalDate fhVigDesde = cmd.fhVigDesde();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        LocalDateTime ahora = LocalDateTime.now();

        Long idFirmante = secuencia.getAndIncrement();
        FalFirmante firmante = new FalFirmante(
                idFirmante, cmd.idUser(), cmd.nomFirmante(),
                ahora, idUserAlta);
        firmanteRepository.guardar(firmante);

        FalFirmanteVersion version = new FalFirmanteVersion(
                idFirmante, 1, cmd.idUser(), cmd.nomFirmante(),
                cmd.rolFirmante(), cmd.cargoFirmante(),
                cmd.idDep(), cmd.verDep(), fhVigDesde,
                ahora, idUserAlta);
        firmanteRepository.guardarVersion(version);

        return firmante;
    }

    // -------------------------------------------------------------------------
    // Versionar firmante
    // -------------------------------------------------------------------------

    public FalFirmanteVersion versionar(VersionarFirmanteCommand cmd) {
        FalFirmante firmante = firmanteRepository.findById(cmd.idFirmante())
                .orElseThrow(() -> new FirmanteNoEncontradoException(cmd.idFirmante()));

        validarNomFirmante(cmd.nomFirmante());
        validarFhVigDesde(cmd.fhVigDesde());
        validarParDependencia(cmd.idDep(), cmd.verDep());

        LocalDate fhVigDesde = cmd.fhVigDesde();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        LocalDateTime ahora = LocalDateTime.now();

        firmanteRepository.findVersionVigente(cmd.idFirmante(), fhVigDesde).ifPresent(anterior -> {
            anterior.setSiActivo(false);
            anterior.setFhVigHasta(fhVigDesde);
            firmanteRepository.guardarVersion(anterior);
        });

        int nuevaVer = firmanteRepository.maxVerFirmante(cmd.idFirmante()) + 1;

        FalFirmanteVersion nuevaVersion = new FalFirmanteVersion(
                cmd.idFirmante(), nuevaVer, firmante.getIdUser(), cmd.nomFirmante(),
                cmd.rolFirmante(), cmd.cargoFirmante(),
                cmd.idDep(), cmd.verDep(), fhVigDesde,
                ahora, idUserAlta);
        firmanteRepository.guardarVersion(nuevaVersion);

        firmante.setNomFirmante(cmd.nomFirmante());
        firmanteRepository.guardar(firmante);

        return nuevaVersion;
    }

    // -------------------------------------------------------------------------
    // Agregar habilitacion
    // -------------------------------------------------------------------------

    public FalFirmanteVersionHabilitacion agregarHabilitacion(AgregarHabilitacionFirmanteCommand cmd) {
        firmanteRepository.findById(cmd.idFirmante())
                .orElseThrow(() -> new FirmanteNoEncontradoException(cmd.idFirmante()));

        firmanteRepository.findVersionByFirmanteAndVer(cmd.idFirmante(), cmd.verFirmante())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Version de firmante no encontrada: idFirmante=" + cmd.idFirmante()
                        + " verFirmante=" + cmd.verFirmante()));

        if (cmd.tipoDocu() == null) {
            throw new PrecondicionVioladaException("tipoDocu es obligatorio.");
        }
        if (cmd.rolFirmaReq() == null) {
            throw new PrecondicionVioladaException("rolFirmaReq es obligatorio.");
        }

        if (firmanteRepository.existeHabilitacionActiva(
                cmd.idFirmante(), cmd.verFirmante(), cmd.tipoDocu(), cmd.rolFirmaReq())) {
            throw new PrecondicionVioladaException(
                    "Ya existe habilitacion activa para tipoDocu=" + cmd.tipoDocu()
                    + " rolFirmaReq=" + cmd.rolFirmaReq()
                    + " en version " + cmd.verFirmante()
                    + " del firmante " + cmd.idFirmante());
        }

        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        FalFirmanteVersionHabilitacion hab = new FalFirmanteVersionHabilitacion(
                cmd.idFirmante(), cmd.verFirmante(),
                cmd.tipoDocu(), cmd.rolFirmaReq(), cmd.mecanismoFirmaReq(),
                LocalDateTime.now(), idUserAlta);
        return firmanteRepository.guardarHabilitacion(hab);
    }

    // -------------------------------------------------------------------------
    // Desactivar habilitacion
    // -------------------------------------------------------------------------

    public void desactivarHabilitacion(DesactivarHabilitacionFirmanteCommand cmd) {
        firmanteRepository.findById(cmd.idFirmante())
                .orElseThrow(() -> new FirmanteNoEncontradoException(cmd.idFirmante()));

        FalFirmanteVersionHabilitacion hab = firmanteRepository
                .findHabilitacionActiva(cmd.idFirmante(), cmd.verFirmante(),
                        cmd.tipoDocu(), cmd.rolFirmaReq())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Habilitacion activa no encontrada: tipoDocu=" + cmd.tipoDocu()
                        + " rolFirmaReq=" + cmd.rolFirmaReq()));

        hab.setSiActivo(false);
        firmanteRepository.guardarHabilitacion(hab);
    }

    // -------------------------------------------------------------------------
    // Listar habilitaciones de version
    // -------------------------------------------------------------------------

    public List<FalFirmanteVersionHabilitacion> listarHabilitaciones(Long idFirmante, int verFirmante) {
        firmanteRepository.findById(idFirmante)
                .orElseThrow(() -> new FirmanteNoEncontradoException(idFirmante));
        firmanteRepository.findVersionByFirmanteAndVer(idFirmante, verFirmante)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "Version de firmante no encontrada: idFirmante=" + idFirmante
                        + " verFirmante=" + verFirmante));
        return firmanteRepository.findHabilitacionesByVersion(idFirmante, verFirmante);
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    public FalFirmante obtener(Long idFirmante) {
        return firmanteRepository.findById(idFirmante)
                .orElseThrow(() -> new FirmanteNoEncontradoException(idFirmante));
    }

    public List<FalFirmante> listarActivos() {
        return firmanteRepository.findAllActivos();
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
        if (firmanteRepository.existsByIdUser(idUser)) {
            throw new PrecondicionVioladaException(
                    "Ya existe un firmante con idUser: " + idUser);
        }
    }

    private void validarNomFirmante(String nomFirmante) {
        if (nomFirmante == null || nomFirmante.isBlank()) {
            throw new PrecondicionVioladaException("nomFirmante es obligatorio.");
        }
    }

    private void validarFhVigDesde(LocalDate fhVigDesde) {
        if (fhVigDesde == null) {
            throw new PrecondicionVioladaException("fhVigDesde es obligatorio.");
        }
    }

    private void validarParDependencia(Long idDep, Integer verDep) {
        if (idDep == null && verDep == null) {
            return;
        }
        if (idDep != null && verDep == null) {
            throw new PrecondicionVioladaException(
                    "Si se informa idDep, verDep tambien es obligatorio.");
        }
        if (idDep == null && verDep != null) {
            throw new PrecondicionVioladaException(
                    "Si se informa verDep, idDep tambien es obligatorio.");
        }
        dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "La dependencia no existe: " + idDep));
        List<FalDependenciaVersion> depVersiones = dependenciaRepository.findVersionesByDep(idDep);
        FalDependenciaVersion depVersion = depVersiones.stream()
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

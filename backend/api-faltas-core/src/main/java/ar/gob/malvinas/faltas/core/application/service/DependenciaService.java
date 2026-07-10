package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarDependenciaCommand;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor de administracion de dependencias organizativas.
 *
 * Reglas del modelo productivo:
 * - nomDep obligatorio.
 * - tipoActa obligatorio y del catalogo productivo (TipoActa enum).
 * - codDep unico si se informa.
 * - idDepPadre debe existir si se informa.
 * - Si hay padre, se congela verDepPadre vigente al momento de crear/versionar.
 * - Al crear dependencia: verDep = 1.
 * - Al versionar: verDep = max(verDep) + 1.
 * - La version anterior pierde siActiva al crear nueva version.
 *
 * Slice 8A-1: implementacion in-memory. Slice 9: reemplazar repositorio por JDBC.
 */
@Service
public class DependenciaService {

    private final DependenciaRepository dependenciaRepository;
    private final AtomicLong secuencia = new AtomicLong(1);
    private final FaltasClock faltasClock;

    public DependenciaService(DependenciaRepository dependenciaRepository,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.dependenciaRepository = dependenciaRepository;
    }

    // -------------------------------------------------------------------------
    // Crear dependencia
    // -------------------------------------------------------------------------

    public FalDependencia crear(CrearDependenciaCommand cmd) {
        validarNomDep(cmd.nomDep());
        validarTipoActa(cmd.tipoActa());
        validarCodDepUnico(cmd.codDep(), null);
        validarFhVigDesde(cmd.fhVigDesde());

        Long idDepPadre = cmd.idDepPadre();
        Integer verDepPadre = null;
        if (idDepPadre != null) {
            verDepPadre = resolverVerDepPadreVigente(idDepPadre);
        }

        Long idDep = secuencia.getAndIncrement();
        LocalDateTime ahora = faltasClock.now();
        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : ahora.toLocalDate();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";

        FalDependencia dep = new FalDependencia(idDep, cmd.nomDep(), ahora, idUserAlta);
        dep.setCodDep(cmd.codDep());
        dep.setIdDepPadre(idDepPadre);
        dependenciaRepository.guardar(dep);

        FalDependenciaVersion version = new FalDependenciaVersion(
                idDep, 1, cmd.nomDep(), idDepPadre, verDepPadre,
                cmd.tipoActa(), fhVigDesde);
        dependenciaRepository.guardarVersion(version);

        return dep;
    }

    // -------------------------------------------------------------------------
    // Versionar dependencia
    // -------------------------------------------------------------------------

    public FalDependenciaVersion versionar(VersionarDependenciaCommand cmd) {
        FalDependencia dep = dependenciaRepository.findById(cmd.idDep())
                .orElseThrow(() -> new DependenciaNoEncontradaException(cmd.idDep()));

        validarNomDep(cmd.nomDep());
        validarTipoActa(cmd.tipoActa());
        validarFhVigDesde(cmd.fhVigDesde());

        Long idDepPadre = cmd.idDepPadre();
        Integer verDepPadre = null;
        if (idDepPadre != null) {
            verDepPadre = resolverVerDepPadreVigente(idDepPadre);
        }

        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : faltasClock.now().toLocalDate();

        // Cerrar version anterior activa
        dependenciaRepository.findVersionVigente(cmd.idDep(), fhVigDesde).ifPresent(anterior -> {
            anterior.setSiActiva(false);
            anterior.setFhVigHasta(fhVigDesde);
            dependenciaRepository.guardarVersion(anterior);
        });

        int nuevaVer = dependenciaRepository.maxVerDep(cmd.idDep()) + 1;

        FalDependenciaVersion nuevaVersion = new FalDependenciaVersion(
                cmd.idDep(), nuevaVer, cmd.nomDep(), idDepPadre, verDepPadre,
                cmd.tipoActa(), fhVigDesde);
        dependenciaRepository.guardarVersion(nuevaVersion);

        return nuevaVersion;
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    public FalDependencia obtener(Long idDep) {
        return dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new DependenciaNoEncontradaException(idDep));
    }

    public List<FalDependencia> listarActivas() {
        return dependenciaRepository.findAllActivas();
    }

    public List<FalDependenciaVersion> listarVersiones(Long idDep) {
        dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new DependenciaNoEncontradaException(idDep));
        return dependenciaRepository.findVersionesByDep(idDep);
    }

    public FalDependenciaVersion obtenerVersionVigente(Long idDep, LocalDate fecha) {
        dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new DependenciaNoEncontradaException(idDep));
        LocalDate fechaConsulta = fecha != null ? fecha : faltasClock.now().toLocalDate();
        return dependenciaRepository.findVersionVigente(idDep, fechaConsulta)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe version vigente para la dependencia: " + idDep
                                + " en fecha: " + fechaConsulta));
    }

    // -------------------------------------------------------------------------
    // Validaciones
    // -------------------------------------------------------------------------

    private void validarNomDep(String nomDep) {
        if (nomDep == null || nomDep.isBlank()) {
            throw new PrecondicionVioladaException("nomDep es obligatorio.");
        }
    }

    private void validarTipoActa(ar.gob.malvinas.faltas.core.domain.enums.TipoActa tipoActa) {
        if (tipoActa == null) {
            throw new PrecondicionVioladaException("tipoActa es obligatorio.");
        }
    }

    private void validarCodDepUnico(String codDep, Long idDepExcluir) {
        if (codDep == null || codDep.isBlank()) return;
        if (dependenciaRepository.existsByCodDep(codDep)) {
            throw new PrecondicionVioladaException(
                    "Ya existe una dependencia con codDep: " + codDep);
        }
    }

    private void validarFhVigDesde(LocalDate fhVigDesde) {
        // Si no se informa se usa la fecha actual: sin validacion de rechazo
    }

    private Integer resolverVerDepPadreVigente(Long idDepPadre) {
        dependenciaRepository.findById(idDepPadre)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El padre informado no existe: " + idDepPadre));
        return dependenciaRepository.findVersionVigente(idDepPadre, faltasClock.now().toLocalDate())
                .map(FalDependenciaVersion::getVerDep)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El padre " + idDepPadre + " no tiene version vigente activa."));
    }
}
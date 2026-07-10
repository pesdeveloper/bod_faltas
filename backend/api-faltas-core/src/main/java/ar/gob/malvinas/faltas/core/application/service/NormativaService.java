package ar.gob.malvinas.faltas.core.application.service;
import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.VincularDependenciaNormativaCommand;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
@Service
public class NormativaService {
    private final NormativaRepository normativaRepository;
    private final DependenciaRepository dependenciaRepository;
    private final AtomicLong secNormativa = new AtomicLong(1);
    private final AtomicLong secArticulo = new AtomicLong(1);
        private final FaltasClock faltasClock;

    public NormativaService(NormativaRepository normativaRepository, DependenciaRepository dependenciaRepository,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.normativaRepository = normativaRepository;
        this.dependenciaRepository = dependenciaRepository;
    }
    public FalNormativaFaltas crearNormativa(CrearNormativaFaltasCommand cmd) {
        if (cmd.codigoNorma() == null || cmd.codigoNorma().isBlank())
            throw new PrecondicionVioladaException("codigoNorma es obligatorio.");
        if (cmd.versionNorma() < 1)
            throw new PrecondicionVioladaException("versionNorma debe ser >= 1.");
        if (cmd.nombreNorma() == null || cmd.nombreNorma().isBlank())
            throw new PrecondicionVioladaException("nombreNorma es obligatorio.");
        if (normativaRepository.existsNormativaByCodigoYVersion(cmd.codigoNorma(), cmd.versionNorma()))
            throw new PrecondicionVioladaException("Ya existe normativa con codigoNorma=" + cmd.codigoNorma() + " versionNorma=" + cmd.versionNorma());
        LocalDateTime ahoraNorm = faltasClock.now();
        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : ahoraNorm.toLocalDate();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        FalNormativaFaltas n = new FalNormativaFaltas(secNormativa.getAndIncrement(),
                cmd.codigoNorma(), cmd.versionNorma(), cmd.nombreNorma(),
                fhVigDesde, ahoraNorm, idUserAlta);
        n.setDescripcionNorma(cmd.descripcionNorma());
        return normativaRepository.guardarNormativa(n);
    }
    public FalNormativaFaltas obtenerNormativa(Long id) {
        return normativaRepository.findNormativaById(id)
                .orElseThrow(() -> new NormativaNoEncontradaException(id));
    }
    public List<FalNormativaFaltas> listarNormativasActivas() {
        return normativaRepository.findAllNormativasActivas();
    }
    public FalArticuloNormativaFaltas crearArticulo(CrearArticuloNormativaFaltasCommand cmd) {
        normativaRepository.findNormativaById(cmd.normativaId())
                .orElseThrow(() -> new NormativaNoEncontradaException(cmd.normativaId()));
        if (cmd.codigoArticulo() == null || cmd.codigoArticulo().isBlank())
            throw new PrecondicionVioladaException("codigoArticulo es obligatorio.");
        if (cmd.versionArticulo() < 1)
            throw new PrecondicionVioladaException("versionArticulo debe ser >= 1.");
        if (cmd.nombreArticulo() == null || cmd.nombreArticulo().isBlank())
            throw new PrecondicionVioladaException("nombreArticulo es obligatorio.");
        if (cmd.cantidadUnidades() == null)
            throw new PrecondicionVioladaException("cantidadUnidades es obligatorio.");
        if (cmd.cantidadUnidades().compareTo(BigDecimal.ZERO) <= 0)
            throw new PrecondicionVioladaException("cantidadUnidades debe ser mayor a 0.");
        if (cmd.tipoUnidad() == null)
            throw new PrecondicionVioladaException("tipoUnidad es obligatorio.");
        if (normativaRepository.existsArticuloByNormativaYCodigo(cmd.normativaId(), cmd.codigoArticulo(), cmd.versionArticulo()))
            throw new PrecondicionVioladaException("Ya existe articulo con codigoArticulo=" + cmd.codigoArticulo() + " versionArticulo=" + cmd.versionArticulo() + " en normativaId=" + cmd.normativaId());
        if (cmd.siTienePagoVoluntario()) {
            if (cmd.cantidadUnidadesPagoVoluntario() == null)
                throw new PrecondicionVioladaException("cantidadUnidadesPagoVoluntario es requerido cuando siTienePagoVoluntario=true.");
            if (cmd.tipoUnidadPagoVoluntario() == null)
                throw new PrecondicionVioladaException("tipoUnidadPagoVoluntario es requerido cuando siTienePagoVoluntario=true.");
        }
        LocalDateTime ahoraArt = faltasClock.now();
        LocalDate fhVigDesde = cmd.fhVigDesde() != null ? cmd.fhVigDesde() : ahoraArt.toLocalDate();
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        FalArticuloNormativaFaltas a = new FalArticuloNormativaFaltas(
                secArticulo.getAndIncrement(), cmd.normativaId(),
                cmd.codigoArticulo(), cmd.versionArticulo(), cmd.nombreArticulo(),
                cmd.cantidadUnidades(), cmd.tipoUnidad(), cmd.siTienePagoVoluntario(),
                fhVigDesde, ahoraArt, idUserAlta);
        a.setDescripcionArticulo(cmd.descripcionArticulo());
        if (cmd.siTienePagoVoluntario()) {
            a.setCantidadUnidadesPagoVoluntario(cmd.cantidadUnidadesPagoVoluntario());
            a.setTipoUnidadPagoVoluntario(cmd.tipoUnidadPagoVoluntario());
        }
        return normativaRepository.guardarArticulo(a);
    }
    public FalArticuloNormativaFaltas obtenerArticulo(Long id) {
        return normativaRepository.findArticuloById(id)
                .orElseThrow(() -> new ArticuloNormativaNoEncontradoException(id));
    }
    public List<FalArticuloNormativaFaltas> listarArticulosByNormativa(Long normativaId) {
        normativaRepository.findNormativaById(normativaId)
                .orElseThrow(() -> new NormativaNoEncontradaException(normativaId));
        return normativaRepository.findArticulosByNormativa(normativaId);
    }
    public FalDependenciaNormativa vincularDependenciaNormativa(VincularDependenciaNormativaCommand cmd) {
        dependenciaRepository.findById(cmd.idDep())
                .orElseThrow(() -> new DependenciaNoEncontradaException(cmd.idDep()));
        boolean versionExiste = dependenciaRepository.findVersionesByDep(cmd.idDep())
                .stream().anyMatch(v -> v.getVerDep() == cmd.verDep());
        if (!versionExiste)
            throw new PrecondicionVioladaException("No existe la version " + cmd.verDep() + " para la dependencia " + cmd.idDep());
        FalNormativaFaltas normativa = normativaRepository.findNormativaById(cmd.normativaId())
                .orElseThrow(() -> new NormativaNoEncontradaException(cmd.normativaId()));
        if (!normativa.isSiActiva())
            throw new PrecondicionVioladaException("La normativa " + cmd.normativaId() + " no esta activa.");
        if (normativaRepository.existsDependenciaNormativaActiva(cmd.idDep(), cmd.verDep(), cmd.normativaId()))
            throw new PrecondicionVioladaException("Ya existe vinculo activo entre dependencia " + cmd.idDep() + " v" + cmd.verDep() + " y normativa " + cmd.normativaId());
        String idUserAlta = cmd.idUserAlta() != null ? cmd.idUserAlta() : "sistema";
        FalDependenciaNormativa rel = new FalDependenciaNormativa(
                cmd.idDep(), cmd.verDep(), cmd.normativaId(), faltasClock.now(), idUserAlta);
        return normativaRepository.guardarDependenciaNormativa(rel);
    }
    public List<FalDependenciaNormativa> listarNormativasByDepVersion(Long idDep, int verDep) {
        dependenciaRepository.findById(idDep)
                .orElseThrow(() -> new DependenciaNoEncontradaException(idDep));
        return normativaRepository.findNormativasActivasByDepVersion(idDep, verDep);
    }
}
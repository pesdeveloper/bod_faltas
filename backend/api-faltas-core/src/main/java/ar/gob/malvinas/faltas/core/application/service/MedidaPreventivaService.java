package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.exception.MedidaPreventivaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestion del catalogo de medidas preventivas.
 * Solo una version activa por codigo en todo momento.
 * La creacion de nueva version es atomica: desactiva anterior y crea nueva en una operacion.
 */
@Service
public class MedidaPreventivaService {

    private final MedidaPreventivaRepository medidaRepo;
    private final ArticuloMedidaPreventivaRepository articuloMedidaRepo;

    public MedidaPreventivaService(
            MedidaPreventivaRepository medidaRepo,
            ArticuloMedidaPreventivaRepository articuloMedidaRepo) {
        this.medidaRepo = medidaRepo;
        this.articuloMedidaRepo = articuloMedidaRepo;
    }

    public FalMedidaPreventiva crearPrimeraVersion(
            String codigo,
            String descripcion,
            String descripcionDetalle,
            Long idDep,
            Short verDep,
            boolean siPuedeBloquearCierre,
            OrigenBloqueanteMaterial tipoBloqueanteDefault,
            String idUser) {
        validarCodigoYDescripcion(codigo, descripcion);
        String codigoNorm = codigo.trim().toUpperCase();
        List<FalMedidaPreventiva> versiones = medidaRepo.findVersionesByCodigo(codigoNorm);
        if (!versiones.isEmpty())
            throw new PrecondicionVioladaException(
                    "Ya existe medida con codigo=" + codigoNorm + ". Use crearNuevaVersion.");

        Long id = medidaRepo.nextId();
        FalMedidaPreventiva m = new FalMedidaPreventiva(
                id, codigoNorm, (short) 1, descripcion, LocalDateTime.now(), idUser);
        m.setDescripcionDetalle(descripcionDetalle);
        if (idDep != null) m.setDependencia(idDep, verDep);
        m.setSiPuedeBloquearCierre(siPuedeBloquearCierre);
        if (tipoBloqueanteDefault != null) m.setTipoBloqueanteDefault(tipoBloqueanteDefault);
        return medidaRepo.save(m);
    }

    /**
     * Crea una nueva version mediante operacion atomica en el repository.
     * Determina la version (max+1), construye el objeto, llama crearNuevaVersionAtomico.
     * El repository atomicamente verifica unicidad, desactiva la anterior y guarda la nueva.
     */
    public FalMedidaPreventiva crearNuevaVersion(
            String codigo,
            String descripcion,
            String descripcionDetalle,
            Long idDep,
            Short verDep,
            boolean siPuedeBloquearCierre,
            OrigenBloqueanteMaterial tipoBloqueanteDefault,
            String idUser) {
        validarCodigoYDescripcion(codigo, descripcion);
        String codigoNorm = codigo.trim().toUpperCase();
        List<FalMedidaPreventiva> versiones = medidaRepo.findVersionesByCodigo(codigoNorm);
        if (versiones.isEmpty())
            throw new PrecondicionVioladaException(
                    "No existe medida con codigo=" + codigoNorm + ". Use crearPrimeraVersion.");

        int maxVersion = versiones.stream()
                .mapToInt(FalMedidaPreventiva::getVersionMedida)
                .max()
                .orElse(0);
        short nuevaVersion = (short) (maxVersion + 1);

        Long id = medidaRepo.nextId();
        FalMedidaPreventiva m = new FalMedidaPreventiva(
                id, codigoNorm, nuevaVersion, descripcion, LocalDateTime.now(), idUser);
        m.setDescripcionDetalle(descripcionDetalle);
        if (idDep != null) m.setDependencia(idDep, verDep);
        m.setSiPuedeBloquearCierre(siPuedeBloquearCierre);
        if (tipoBloqueanteDefault != null) m.setTipoBloqueanteDefault(tipoBloqueanteDefault);

        return medidaRepo.crearNuevaVersionAtomico(m);
    }

    public FalMedidaPreventiva obtenerPorId(Long id) {
        return medidaRepo.findById(id).orElseThrow(() -> new MedidaPreventivaNoEncontradaException(id));
    }

    public Optional<FalMedidaPreventiva> buscarActivaPorCodigo(String codigo) {
        return medidaRepo.findActivaByCodigo(codigo.trim().toUpperCase());
    }

    public List<FalMedidaPreventiva> listarActivas() {
        return medidaRepo.findActivas();
    }

    public List<FalMedidaPreventiva> listarVersiones(String codigo) {
        return medidaRepo.findVersionesByCodigo(codigo.trim().toUpperCase());
    }

    public FalMedidaPreventiva desactivar(Long id, String idUser) {
        FalMedidaPreventiva m = medidaRepo.findById(id)
                .orElseThrow(() -> new MedidaPreventivaNoEncontradaException(id));
        if (!m.isSiActiva())
            throw new PrecondicionVioladaException("La medida id=" + id + " ya esta inactiva.");
        m.setSiActiva(false);
        return medidaRepo.save(m);
    }

    public FalArticuloMedidaPreventiva vincularArticulo(
            Long articuloId,
            Long medidaPreventivaId,
            boolean siObligatoria,
            String idUser) {
        ArticuloMedidaPreventivaId pkId = new ArticuloMedidaPreventivaId(articuloId, medidaPreventivaId);
        if (articuloMedidaRepo.existsActiva(pkId))
            throw new PrecondicionVioladaException(
                    "La relacion articulo-medida ya esta activa: articuloId=" + articuloId
                            + " medidaId=" + medidaPreventivaId);

        medidaRepo.findById(medidaPreventivaId)
                .filter(FalMedidaPreventiva::isSiActiva)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "La medida id=" + medidaPreventivaId + " no existe o esta inactiva."));

        FalArticuloMedidaPreventiva rel = new FalArticuloMedidaPreventiva(
                pkId, siObligatoria, LocalDateTime.now(), idUser);
        return articuloMedidaRepo.save(rel);
    }

    public FalArticuloMedidaPreventiva desvincularArticulo(
            Long articuloId, Long medidaPreventivaId, String idUser) {
        ArticuloMedidaPreventivaId pkId = new ArticuloMedidaPreventivaId(articuloId, medidaPreventivaId);
        FalArticuloMedidaPreventiva rel = articuloMedidaRepo.findById(pkId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe relacion articulo-medida: articuloId=" + articuloId));
        if (!rel.isSiActiva())
            throw new PrecondicionVioladaException("La relacion articulo-medida ya esta inactiva.");
        rel.setSiActiva(false);
        return articuloMedidaRepo.save(rel);
    }

    public List<FalArticuloMedidaPreventiva> listarMedidasActivasDeArticulo(Long articuloId) {
        return articuloMedidaRepo.findActivasByArticuloId(articuloId);
    }

    private void validarCodigoYDescripcion(String codigo, String descripcion) {
        if (codigo == null || codigo.isBlank())
            throw new PrecondicionVioladaException("codigo es obligatorio.");
        if (codigo.trim().length() > 12)
            throw new PrecondicionVioladaException("codigo max 12 caracteres.");
        if (descripcion == null || descripcion.isBlank())
            throw new PrecondicionVioladaException("descripcion es obligatoria.");
        if (descripcion.length() > 160)
            throw new PrecondicionVioladaException("descripcion max 160 caracteres.");
    }
}

package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoMedidaAplicada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.exception.ActaMedidaPreventivaAplicadaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaArticuloInfringidoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.ActaMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de aplicacion para medidas preventivas aplicadas al acta.
 * Coordina la creacion de la medida aplicada con el bloqueante material cuando corresponde.
 * La atomicidad en InMemory se implementa mediante bloque synchronized.
 */
@Service
public class ActaMedidaPreventivaAplicadaService {

    private final ActaMedidaPreventivaRepository medidaRepository;
    private final ActaArticuloInfringidoRepository articuloRepository;
    private final ArticuloMedidaPreventivaRepository articuloMedidaRepository;
    private final MedidaPreventivaRepository catalogoRepository;
    private final BloqueanteMaterialRepository bloqueanteMaterialRepository;

    public ActaMedidaPreventivaAplicadaService(
            ActaMedidaPreventivaRepository medidaRepository,
            ActaArticuloInfringidoRepository articuloRepository,
            ArticuloMedidaPreventivaRepository articuloMedidaRepository,
            MedidaPreventivaRepository catalogoRepository,
            BloqueanteMaterialRepository bloqueanteMaterialRepository) {
        this.medidaRepository = medidaRepository;
        this.articuloRepository = articuloRepository;
        this.articuloMedidaRepository = articuloMedidaRepository;
        this.catalogoRepository = catalogoRepository;
        this.bloqueanteMaterialRepository = bloqueanteMaterialRepository;
    }

    /**
     * Aplica una medida preventiva a un articulo infringido del acta.
     * Si la medida genera bloqueante, crea el FalBloqueanteMaterial en la misma operacion.
     * Rollback logico: si falla la creacion del bloqueante, la medida no queda guardada.
     */
    public synchronized FalActaMedidaPreventiva aplicarMedida(
            Long actaId,
            Long actaArticuloId,
            Long medidaPreventivaId,
            boolean siGeneraBloqueante,
            String medPrevTxt,
            String idUserAlta) {
        // Validar articulo pertenece al acta y esta activo
        FalActaArticuloInfringido articulo = articuloRepository.findById(actaArticuloId)
                .orElseThrow(() -> new ActaArticuloInfringidoNoEncontradoException(actaArticuloId));
        if (!articulo.getActaId().equals(actaId))
            throw new IllegalArgumentException("El articulo " + actaArticuloId + " no pertenece al acta " + actaId);
        if (!articulo.isSiActivo())
            throw new IllegalStateException("El articulo " + actaArticuloId + " esta inactivo");

        // Validar relacion articulo-medida en catalogo
        boolean relacionValida = articuloMedidaRepository
                .findById(new ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId(
                        articulo.getArticuloId(), medidaPreventivaId))
                .map(r -> r.isSiActiva())
                .orElse(false);
        if (!relacionValida)
            throw new IllegalStateException(
                    "La medida " + medidaPreventivaId + " no es aplicable al articulo " + articulo.getArticuloId());

        // Crear medida aplicada
        Long medidaId = medidaRepository.nextId();
        FalActaMedidaPreventiva medida = new FalActaMedidaPreventiva(
                medidaId, actaId, actaArticuloId, medidaPreventivaId,
                siGeneraBloqueante, LocalDateTime.now(), idUserAlta);
        if (medPrevTxt != null) medida.setMedPrevTxt(medPrevTxt);

        // Si genera bloqueante, crear bloqueante material atomicamente
        if (siGeneraBloqueante) {
            FalMedidaPreventiva catalogo = catalogoRepository.findById(medidaPreventivaId)
                    .orElseThrow(() -> new ar.gob.malvinas.faltas.core.domain.exception
                            .MedidaPreventivaNoEncontradaException(medidaPreventivaId));
            OrigenBloqueanteMaterial origen = catalogo.getTipoBloqueanteDefault() != null
                    ? catalogo.getTipoBloqueanteDefault()
                    : OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA;
            Long bloqId = bloqueanteMaterialRepository.nextId();
            FalBloqueanteMaterial bloqueante = new FalBloqueanteMaterial(bloqId, actaId);
            bloqueante.setOrigen(origen);
            bloqueante.setDescripcion("Medida preventiva id=" + medidaId);
            // Guardar bloqueante primero
            bloqueanteMaterialRepository.guardar(bloqueante);
        }

        return medidaRepository.guardar(medida);
    }

    public FalActaMedidaPreventiva findById(Long id) {
        return medidaRepository.findById(id)
                .orElseThrow(() -> new ActaMedidaPreventivaAplicadaNoEncontradaException(id));
    }

    public List<FalActaMedidaPreventiva> findByActaId(Long actaId) {
        return medidaRepository.findByActaId(actaId);
    }

    /**
     * Transiciona el estado de la medida.
     * Si se levanta/anula/cumple y genero bloqueante, resuelve el bloqueante correspondiente.
     */
    public synchronized FalActaMedidaPreventiva transicionarEstado(Long medidaId, EstadoMedidaAplicada nuevoEstado, String idUser) {
        FalActaMedidaPreventiva medida = findById(medidaId);
        medida.transicionarEstado(nuevoEstado);
        FalActaMedidaPreventiva guardada = medidaRepository.guardar(medida);

        // Si la medida genero bloqueante y se resuelve, resolver el bloqueante
        if (medida.isSiGeneraBloqueante() &&
                (nuevoEstado == EstadoMedidaAplicada.LEVANTADA ||
                 nuevoEstado == EstadoMedidaAplicada.ANULADA ||
                 nuevoEstado == EstadoMedidaAplicada.CUMPLIDA)) {
            // Buscar bloqueante activo asociado al acta con origen MEDIDA_PREVENTIVA
            bloqueanteMaterialRepository.findByActaId(medida.getActaId()).stream()
                    .filter(b -> b.isSiActivo() && b.getOrigen() == OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA)
                    .filter(b -> b.getDescripcion() != null && b.getDescripcion().contains("id=" + medidaId))
                    .findFirst()
                    .ifPresent(b -> {
                        b.setSiActivo(false);
                        b.setEstado(EstadoBloqueanteMaterial.CUMPLIDO);
                        b.setFechaCierre(LocalDateTime.now());
                        bloqueanteMaterialRepository.guardar(b);
                    });
        }

        return guardada;
    }
}

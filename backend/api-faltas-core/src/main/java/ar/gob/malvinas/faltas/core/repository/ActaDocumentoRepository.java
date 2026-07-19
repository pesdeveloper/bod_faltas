package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;
import ar.gob.malvinas.faltas.core.domain.model.ActaDocumentoId;
import ar.gob.malvinas.faltas.core.domain.model.FalActaDocumento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del pivot acta-documento.
 *
 * La relacion es historica: no exponer delete fisico como operacion ordinaria.
 * La unicidad de principal por (acta, rol) es invariante del repository.
 */
public interface ActaDocumentoRepository {

    FalActaDocumento guardar(FalActaDocumento relacion);

    Optional<FalActaDocumento> buscarPorIdCompuesto(ActaDocumentoId id);

    boolean existe(Long actaId, Long documentoId);

    List<FalActaDocumento> listarPorActa(Long actaId);

    List<FalActaDocumento> listarPorDocumento(Long documentoId);

    List<FalActaDocumento> listarPorActaYRol(Long actaId, RolDocuActa rol);

    Optional<FalActaDocumento> buscarPrincipalPorActaYRol(Long actaId, RolDocuActa rol);

    /**
     * Asocia el documento como principal para su rol en el acta.
     * Si ya existe un principal para ese (acta, rol), lo marca no-principal.
     * Operacion atomica bajo lock.
     *
     * @return la relacion guardada (el nuevo principal)
     */
    FalActaDocumento asociarComoPrincipalAtomico(Long actaId, Long documentoId, RolDocuActa rol,
                                                  String idUserAlta, java.time.LocalDateTime fhAlta);

    /**
     * Reemplaza el documento principal actual por uno nuevo para (acta, rol).
     * El anterior queda como no-principal (historial preservado).
     * Operacion atomica bajo lock.
     *
     * @return la nueva relacion principal
     */
    FalActaDocumento reemplazarPrincipalAtomico(Long actaId, Long documentoIdNuevo, RolDocuActa rol,
                                                 String idUserAlta, java.time.LocalDateTime fhAlta);
}

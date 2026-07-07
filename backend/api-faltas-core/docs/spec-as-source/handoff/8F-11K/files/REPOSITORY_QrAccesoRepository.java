package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de registros de acceso via QR.
 *
 * Semantica append-only: no existe update ni delete funcional.
 * Multiples accesos validos al mismo QR son legitimos; no hay deduplicacion.
 * Orden estable: fhAcceso + id.
 */
public interface QrAccesoRepository {

    Long nextId();

    FalActaQrAcceso registrar(FalActaQrAcceso acceso);

    Optional<FalActaQrAcceso> buscarPorId(Long id);

    List<FalActaQrAcceso> listarPorActa(Long actaId);

    int contarPorActa(Long actaId);
}

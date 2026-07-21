package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;

/**
 * Resultado de guardarSiAusentePorReferencia: la firma persistida y si ya existia previamente.
 *
 * firmaPersistida: la firma almacenada (la existente si yaExistia=true, la nueva si yaExistia=false).
 * yaExistia: true si la referenciaFirmaExt ya estaba registrada antes de esta llamada.
 *
 * FIX-FALLO-NOTI-01-R2: idempotencia concurrente.
 */
public record DocumentoFirmaSaveResult(FalDocumentoFirma firmaPersistida, boolean yaExistia) {}

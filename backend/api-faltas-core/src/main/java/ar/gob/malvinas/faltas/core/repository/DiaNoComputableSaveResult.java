package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;

/**
 * Resultado de guardarActivoSiAusentePorFecha: el dia persistido y si ya existia.
 *
 * diaPersistido: el registro activo canonico para esa fecha (el existente si
 * yaExistia=true, el recien insertado si yaExistia=false).
 * yaExistia: true si la fecha ya tenia un registro activo antes de esta llamada.
 *
 * Sigue la convencion ya aprobada en el proyecto para operaciones
 * insert-if-absent concurrentes (ver DocumentoFirmaSaveResult).
 *
 * Reemplaza la deteccion de alta concurrente por comparacion de ids preasignados,
 * que dejo de ser expresable al pasar la PK a AUTO_INCREMENT
 * (WAVE 1A0-D, decision de identificadores).
 */
public record DiaNoComputableSaveResult(FalDiaNoComputable diaPersistido, boolean yaExistia) {}

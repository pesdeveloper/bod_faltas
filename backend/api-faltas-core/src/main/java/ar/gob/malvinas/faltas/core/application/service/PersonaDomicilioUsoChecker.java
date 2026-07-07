package ar.gob.malvinas.faltas.core.application.service;

/**
 * Policy extensible para determinar si un domicilio ya fue usado formalmente.
 *
 * Un domicilio usado formalmente no puede corregirse en la misma fila:
 * debe crearse una nueva fila operativa.
 *
 * Usos formales actuales: referencias en FalActa (idDomicilioInfractorAct, idDomicilioNotifAct).
 * Slices futuros ampliaran el checker para notificaciones, documentos, etc.
 */
public interface PersonaDomicilioUsoChecker {
    boolean estaUsadoFormalmente(Long domicilioId);
}

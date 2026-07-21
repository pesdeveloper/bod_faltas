package ar.gob.malvinas.faltas.core.domain;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor unico de transiciones de estado para FalActa.
 *
 * Fuente de verdad canonica: dado un TipoEventoActa, determina de forma
 * determinista el BloqueActual, EstadoProcesalActa y SituacionAdministrativaActa
 * resultantes, cuando corresponde una transicion.
 *
 * No contiene logica de negocio (precondiciones, validaciones); solo la
 * matriz de transiciones puras. Las precondiciones viven en los servicios.
 */
public class ActaTransitionEngine {

    public record TransicionResultado(
            BloqueActual bloqueNvo,
            EstadoProcesalActa estProcNvo,
            SituacionAdministrativaActa sitAdmNva
    ) {
        public boolean cambiaNBloque() { return bloqueNvo != null; }
        public boolean cambiaEstadoProcesal() { return estProcNvo != null; }
        public boolean cambiaSituacionAdm() { return sitAdmNva != null; }
        public boolean tieneAlgunCambio() { return cambiaNBloque() || cambiaEstadoProcesal() || cambiaSituacionAdm(); }
    }

    private static final Map<TipoEventoActa, TransicionResultado> MATRIZ = buildMatriz();

    private static Map<TipoEventoActa, TransicionResultado> buildMatriz() {
        var m = new java.util.EnumMap<TipoEventoActa, TransicionResultado>(TipoEventoActa.class);
        // Creacion y captura
        m.put(TipoEventoActa.ACTLAB, new TransicionResultado(BloqueActual.CAPT, EstadoProcesalActa.EN_TRAMITE, SituacionAdministrativaActa.ACTIVA));
        m.put(TipoEventoActa.ACTCAP, new TransicionResultado(BloqueActual.ENRI, null, null));
        m.put(TipoEventoActa.ACTENR, new TransicionResultado(BloqueActual.NOTI, null, null));
        // Notificacion
        m.put(TipoEventoActa.NOTENV, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.NOTPOS, new TransicionResultado(BloqueActual.ANAL, null, null));
        m.put(TipoEventoActa.NOTNEG, new TransicionResultado(BloqueActual.ANAL, null, null));
        m.put(TipoEventoActa.NOTVNC, new TransicionResultado(BloqueActual.ANAL, null, null));
        m.put(TipoEventoActa.NOTINT, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.NOTREI, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.NOTRVE, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PORPOS, new TransicionResultado(BloqueActual.ANAL, null, null));
        m.put(TipoEventoActa.NOTSUP, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.ACUGEN, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.ACUVAL, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.LOTGEN, new TransicionResultado(null, null, null));
        // Pago voluntario
        m.put(TipoEventoActa.PAGVSO, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PAGVMF, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PAGINF, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PAGCMP, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PAGCNF, new TransicionResultado(BloqueActual.CERR, EstadoProcesalActa.CONCLUIDO, SituacionAdministrativaActa.CERRADA));
        m.put(TipoEventoActa.PAGOBS, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PAGVVN, new TransicionResultado(null, null, null));
        // Fallo
        m.put(TipoEventoActa.FALABS, new TransicionResultado(BloqueActual.CERR, EstadoProcesalActa.CONCLUIDO, SituacionAdministrativaActa.CERRADA));
        m.put(TipoEventoActa.FALCON, new TransicionResultado(BloqueActual.ANAL, null, null));
        // Apelacion
        m.put(TipoEventoActa.APEPRE, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.APEANL, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.APERAZ, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.APEABS, new TransicionResultado(BloqueActual.CERR, EstadoProcesalActa.CONCLUIDO, SituacionAdministrativaActa.CERRADA));
        m.put(TipoEventoActa.APEMCO, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.APENUL, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.FALRMP, new TransicionResultado(null, null, null));
        // Firmeza
        m.put(TipoEventoActa.PLAVNC, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.CONFIR, new TransicionResultado(null, null, null));
        // Pago de condena
        m.put(TipoEventoActa.PCOINF, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.PCOCNF, new TransicionResultado(BloqueActual.CERR, EstadoProcesalActa.CONCLUIDO, SituacionAdministrativaActa.CERRADA));
        m.put(TipoEventoActa.PCOOBS, new TransicionResultado(null, null, null));
        // Gestion externa
        m.put(TipoEventoActa.EXTDER, new TransicionResultado(BloqueActual.GEXT, null, SituacionAdministrativaActa.EN_GESTION_EXTERNA));
        m.put(TipoEventoActa.EXTRET, new TransicionResultado(BloqueActual.ANAL, null, SituacionAdministrativaActa.ACTIVA));
        m.put(TipoEventoActa.PAGAPR, new TransicionResultado(BloqueActual.CERR, EstadoProcesalActa.CONCLUIDO, SituacionAdministrativaActa.CERRADA));
        // Paralizacion
        m.put(TipoEventoActa.ACTPAR, new TransicionResultado(null, null, SituacionAdministrativaActa.PARALIZADA));
        m.put(TipoEventoActa.ACTREA, new TransicionResultado(null, null, SituacionAdministrativaActa.ACTIVA));
        // Archivo
        m.put(TipoEventoActa.ACTARCH, new TransicionResultado(BloqueActual.ARCH, null, SituacionAdministrativaActa.ARCHIVADA));
        m.put(TipoEventoActa.ACTREI, new TransicionResultado(BloqueActual.ANAL, null, SituacionAdministrativaActa.ACTIVA));
        // Documentos (trazabilidad sin transicion de bloque)
        m.put(TipoEventoActa.DOCGEN, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.DOCFIR, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.DOCEMI, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.DOCADJ, new TransicionResultado(null, null, null));
        // QR
        m.put(TipoEventoActa.QRGEN, new TransicionResultado(null, null, null));
        m.put(TipoEventoActa.QRACC, new TransicionResultado(null, null, null));
        return java.util.Collections.unmodifiableMap(m);
    }

    /** Retorna la transicion producida si el evento cambia algun aspecto del estado. */
    public Optional<TransicionResultado> calcularTransicion(TipoEventoActa tipoEvt) {
        if (tipoEvt == null) return Optional.empty();
        TransicionResultado r = MATRIZ.get(tipoEvt);
        if (r == null || !r.tieneAlgunCambio()) return Optional.empty();
        return Optional.of(r);
    }

    /** Indica si un evento produce cierre definitivo (CERR + CONCLUIDO). */
    public boolean produceCierre(TipoEventoActa tipoEvt) {
        TransicionResultado r = MATRIZ.get(tipoEvt);
        return r != null
                && BloqueActual.CERR.equals(r.bloqueNvo())
                && EstadoProcesalActa.CONCLUIDO.equals(r.estProcNvo());
    }

    /** Retorna todos los eventos que producen una transicion hacia el bloque dado. */
    public Set<TipoEventoActa> eventosQueTransicionanA(BloqueActual bloque) {
        if (bloque == null) return Set.of();
        return MATRIZ.entrySet().stream()
                .filter(e -> bloque.equals(e.getValue().bloqueNvo()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Acceso directo a la entrada de la matriz para inspeccion (tests, debug). */
    public Optional<TransicionResultado> inspeccionarTransicion(TipoEventoActa tipoEvt) {
        return Optional.ofNullable(MATRIZ.get(tipoEvt));
    }
}

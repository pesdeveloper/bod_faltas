package ar.gob.malvinas.faltas.core.domain;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;

import java.util.ArrayList;
import java.util.List;

/**
 * Verificador puro de consistencia del agregado FalActa.
 *
 * Recibe el estado actual del acta y su lista de eventos, y verifica
 * invariantes del dominio sin producir efectos secundarios ni lanzar
 * excepciones. Devuelve una lista de violaciones detectadas.
 *
 * Uso: verificar en tests, en servicios criticos antes de persistir,
 * o como paso de auditoria. No es un guardian de precondiciones de
 * negocio (eso vive en los servicios); es un verificador post-hoc de
 * consistencia estructural del agregado.
 */
public class ActaConsistencyChecker {

    /** Descripcion de una violacion de invariante detectada. */
    public record Violacion(String regla, String detalle) {
        @Override
        public String toString() {
            return "[" + regla + "] " + detalle;
        }
    }

    /**
     * Verifica la consistencia del agregado.
     *
     * @param acta   El acta en su estado actual (post-transicion).
     * @param eventos La lista de eventos registrados para el acta, en orden cronologico.
     * @return Lista de violaciones detectadas. Vacia si el agregado es consistente.
     */
    public List<Violacion> verificar(FalActa acta, List<FalActaEvento> eventos) {
        var violaciones = new ArrayList<Violacion>();

        verificarIdentidad(acta, violaciones);
        verificarEstadosNoNulos(acta, violaciones);
        verificarEventosPropios(acta, eventos, violaciones);
        verificarPrimerEvento(acta, eventos, violaciones);
        verificarNoDuplicadosCierre(acta, eventos, violaciones);
        verificarCierreConsistente(acta, eventos, violaciones);
        verificarSituacionAdm(acta, violaciones);
        verificarVersionRow(acta, violaciones);

        return violaciones;
    }

    /** Shortcut: retorna true si el agregado es consistente (sin violaciones). */
    public boolean esConsistente(FalActa acta, List<FalActaEvento> eventos) {
        return verificar(acta, eventos).isEmpty();
    }

    // --- Reglas de verificacion ---

    private void verificarIdentidad(FalActa acta, List<Violacion> v) {
        if (acta.getId() == null) {
            v.add(new Violacion("ID_NULO", "El acta no tiene id asignado"));
        }
        if (acta.getUuidTecnico() == null || acta.getUuidTecnico().isBlank()) {
            v.add(new Violacion("UUID_NULO", "El acta no tiene uuidTecnico"));
        }
        // nroActa es nullable en el dominio; no es un invariante de identidad obligatorio
    }

    private void verificarEstadosNoNulos(FalActa acta, List<Violacion> v) {
        if (acta.getBloqueActual() == null) {
            v.add(new Violacion("BLOQUE_NULO", "bloqueActual no puede ser null"));
        }
        if (acta.getEstadoProcesal() == null) {
            v.add(new Violacion("ESTADO_PROCESAL_NULO", "estadoProcesalActa no puede ser null"));
        }
        if (acta.getSituacionAdministrativa() == null) {
            v.add(new Violacion("SITUACION_ADM_NULA", "situacionAdministrativaActa no puede ser null"));
        }
    }

    private void verificarEventosPropios(FalActa acta, List<FalActaEvento> eventos, List<Violacion> v) {
        if (acta.getId() == null || eventos == null) return;
        for (FalActaEvento e : eventos) {
            if (!acta.getId().equals(e.actaId())) {
                v.add(new Violacion("EVENTO_AJENO",
                        "Evento id=" + e.getId() + " pertenece al acta " + e.actaId()
                                + " pero se esta verificando con acta " + acta.getId()));
            }
        }
    }

    private void verificarPrimerEvento(FalActa acta, List<FalActaEvento> eventos, List<Violacion> v) {
        if (eventos == null || eventos.isEmpty()) return;
        var primero = eventos.get(0);
        if (primero.tipoEvt() != ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa.ACTLAB) {
            v.add(new Violacion("PRIMER_EVENTO_INVALIDO",
                    "El primer evento debe ser ACTLAB, pero es: " + primero.tipoEvt()));
        }
    }

    private void verificarNoDuplicadosCierre(FalActa acta, List<FalActaEvento> eventos, List<Violacion> v) {
        if (eventos == null) return;
        long cantCierre = eventos.stream()
                .filter(e -> e.siEvtCierre())
                .count();
        if (cantCierre > 1) {
            v.add(new Violacion("MULTIPLE_CIERRE",
                    "Se encontraron " + cantCierre + " eventos de cierre (siEvtCierre=true). Debe haber como maximo 1."));
        }
    }

    private void verificarCierreConsistente(FalActa acta, List<FalActaEvento> eventos, List<Violacion> v) {
        if (acta.getBloqueActual() == null) return;
        boolean estaCerrada = BloqueActual.CERR.equals(acta.getBloqueActual())
                && SituacionAdministrativaActa.CERRADA.equals(acta.getSituacionAdministrativa())
                && EstadoProcesalActa.CONCLUIDO.equals(acta.getEstadoProcesal());

        if (estaCerrada && (eventos == null || eventos.isEmpty())) {
            v.add(new Violacion("CIERRE_SIN_EVENTOS",
                    "El acta esta en CERR/CERRADA/CONCLUIDO pero no tiene eventos registrados"));
            return;
        }

        if (estaCerrada && eventos != null) {
            boolean tieneCierreExplicito = eventos.stream()
                    .anyMatch(e -> e.siEvtCierre());
            if (!tieneCierreExplicito) {
                v.add(new Violacion("CIERRE_SIN_EVENTO_MARCADO",
                        "El acta esta en estado de cierre pero ningun evento tiene siEvtCierre=true"));
            }
        }
    }

    private void verificarSituacionAdm(FalActa acta, List<Violacion> v) {
        if (acta.getBloqueActual() == null || acta.getSituacionAdministrativa() == null) return;

        // Un acta en CERR no puede estar ACTIVA o PARALIZADA
        if (BloqueActual.CERR.equals(acta.getBloqueActual())) {
            var sit = acta.getSituacionAdministrativa();
            if (sit == SituacionAdministrativaActa.ACTIVA || sit == SituacionAdministrativaActa.PARALIZADA) {
                v.add(new Violacion("SITUACION_ADM_INCOMPATIBLE",
                        "Bloque CERR no es compatible con situacion " + sit));
            }
        }

        // Un acta en ARCH debe estar ARCHIVADA
        if (BloqueActual.ARCH.equals(acta.getBloqueActual())
                && !SituacionAdministrativaActa.ARCHIVADA.equals(acta.getSituacionAdministrativa())) {
            v.add(new Violacion("ARCH_SIN_ARCHIVADA",
                    "Bloque ARCH requiere situacion ARCHIVADA, actual: " + acta.getSituacionAdministrativa()));
        }
    }

    private void verificarVersionRow(FalActa acta, List<Violacion> v) {
        if (acta.getVersionRow() < 0) {
            v.add(new Violacion("VERSION_ROW_NEGATIVO",
                    "versionRow no puede ser negativo: " + acta.getVersionRow()));
        }
    }
}

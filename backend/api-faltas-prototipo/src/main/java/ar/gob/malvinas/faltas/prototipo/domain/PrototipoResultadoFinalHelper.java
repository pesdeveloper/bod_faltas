package ar.gob.malvinas.faltas.prototipo.domain;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;

public final class PrototipoResultadoFinalHelper {

    private PrototipoResultadoFinalHelper() {}

    public static PrototipoStore.ResultadoFinalCierreMock resultadoFinalVigente(
            PrototipoStore store, String actaId) {
        PrototipoStore.CerrabilidadActaVista v = store.getCerrabilidadActa(actaId);
        return v != null ? v.resultadoFinal() : PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
    }
}

package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.application.service.TesoreriaConciliacionInput;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion in-memory del input mock absoluto de Tesoreria.
 * Descartable / solo demo. No persiste ni usa base de datos real.
 */
@Component
public class InMemoryTesoreriaConciliacionInput
        implements TesoreriaConciliacionInput, ResettableInMemoryRepository {

    private static final String NULO = "__NULL_REF__";
    private final Map<Long, String> conciliados = new ConcurrentHashMap<>();

    @Override
    public boolean estaConciliadoTesoreria(Long movimientoId) {
        return movimientoId != null && conciliados.containsKey(movimientoId);
    }

    @Override
    public String referenciaDe(Long movimientoId) {
        if (movimientoId == null) return null;
        String v = conciliados.get(movimientoId);
        return NULO.equals(v) ? null : v;
    }

    @Override
    public synchronized boolean registrar(Long movimientoId, String referencia) {
        if (movimientoId == null) throw new IllegalArgumentException("movimientoId requerido");
        String nueva = referencia == null ? NULO : referencia;
        String previa = conciliados.get(movimientoId);
        if (previa == null) {
            conciliados.put(movimientoId, nueva);
            return true;
        }
        if (NULO.equals(previa)) {
            conciliados.put(movimientoId, nueva);
            return true;
        }
        if (NULO.equals(nueva)) {
            return true;
        }
        return previa.equals(nueva);
    }

    @Override
    public synchronized java.util.Map<Long, String> snapshotActual() {
        java.util.Map<Long, String> copia = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, String> e : conciliados.entrySet()) {
            copia.put(e.getKey(), NULO.equals(e.getValue()) ? null : e.getValue());
        }
        return java.util.Collections.unmodifiableMap(copia);
    }

    @Override
    public synchronized void reemplazarEstadoAbsoluto(java.util.Map<Long, String> nuevoSnapshot) {
        if (nuevoSnapshot == null) throw new IllegalArgumentException("nuevoSnapshot requerido");
        conciliados.clear();
        for (java.util.Map.Entry<Long, String> e : nuevoSnapshot.entrySet()) {
            if (e.getKey() == null) throw new IllegalArgumentException("movimientoId requerido en snapshot");
            conciliados.put(e.getKey(), e.getValue() == null ? NULO : e.getValue());
        }
    }

    @Override
    public void reset() { conciliados.clear(); }

    @Override
    public String nombre() { return "tesoreria-conciliacion-input"; }

    @Override
    public int size() { return conciliados.size(); }
}

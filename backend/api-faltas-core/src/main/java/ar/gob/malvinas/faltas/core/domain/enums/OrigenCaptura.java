package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen del labrado/captura del acta.
 * SMALLINT en MariaDB (origen_captura).
 *
 * Distingue el canal por el que llego el acta al sistema:
 * dispositivo movil, carga web interna, integracion externa o importacion.
 */
public enum OrigenCaptura {

    MOBILE_INSPECTOR((short) 1, "Inspector en dispositivo movil"),
    WEB_OPERADOR((short) 2, "Operador en interfaz web interna"),
    INTEGRACION_EXTERNA((short) 3, "Integracion con sistema externo"),
    CARGA_MASIVA((short) 4, "Carga masiva / importacion"),
    SISTEMA_AUTOMATICO((short) 5, "Proceso automatico del sistema");

    private final short codigo;
    private final String descripcion;

    OrigenCaptura(short codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public short codigo() { return codigo; }
    public String descripcion() { return descripcion; }

    public static OrigenCaptura fromCodigo(short codigo) {
        for (OrigenCaptura v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenCaptura desconocido: " + codigo);
    }
}
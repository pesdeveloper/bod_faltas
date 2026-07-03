package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipos de evento de dominio del acta.
 * Codigo de 6 caracteres persistible como CHAR(6) en MariaDB.
 *
 * Solo eventos con valor real de trazabilidad, auditoria o reconstruccion.
 * Prohibido incluir eventos de proyeccion como PASE_BANDEJA.
 *
 * ACTCAP y ACTENR son eventos productivos reales: representan transiciones
 * de bloque observables y auditables en el circuito (CAPT->ENRI, ENRI->doc).
 * No son eventos demo ni de proyeccion.
 *
 * APELAC NO EXISTE como evento productivo.
 * Eventos productivos de apelacion: APEPRE (presentada), APERAZ (rechazada), APEABS (aceptada-absuelve).
 * PLAVNC y CONFIR son eventos productivos de firmeza de condena (Slice 4).
 *
 * PAGCON NO EXISTE como evento productivo.
 * Eventos productivos de pago de condena: PCOINF (informado), PCOCNF (confirmado), PCOOBS (observado).
 * Pago de condena se implementa en Slice 5.
 *
 * DRVEXT NO EXISTE como evento productivo. Prohibido.
 * Eventos productivos de gestion externa (Slice 6): EXTDER (derivar), EXTRET (reingresar), PAGAPR (pago apremio).
 */
public enum TipoEventoActa {

    ACTLAB("ACTLAB", "Acta labrada/creada"),
    ACTCAP("ACTCAP", "Captura completada - transicion CAPT->ENRI"),
    ACTENR("ACTENR", "Acta enriquecida - datos del expediente completados"),
    DOCGEN("DOCGEN", "Documento generado"),
    DOCFIR("DOCFIR", "Documento firmado"),
    DOCEMI("DOCEMI", "Documento emitido formalmente"),
    NOTENV("NOTENV", "Notificacion enviada"),
    NOTPOS("NOTPOS", "Notificacion con acuse positivo"),
    NOTNEG("NOTNEG", "Notificacion con acuse negativo"),
    NOTVNC("NOTVNC", "Notificacion vencida sin acuse"),
    ACTPAR("ACTPAR", "Acta paralizada"),
    ACTREA("ACTREA", "Acta reactivada"),
    ACTARCH("ACTARC", "Acta archivada"),
    ACTREI("ACTREI", "Acta reingresada desde archivo"),
    FALABS("FALABS", "Fallo absolutorio dictado"),
    FALCON("FALCON", "Fallo condenatorio dictado"),
    PAGVSO("PAGVSO", "Pago voluntario solicitado"),
    PAGVMF("PAGVMF", "Pago voluntario monto fijado"),
    PAGINF("PAGINF", "Pago voluntario informado por el infractor"),
    PAGCMP("PAGCMP", "Comprobante de pago adjuntado"),
    PAGCNF("PAGCNF", "Pago voluntario confirmado"),
    PAGOBS("PAGOBS", "Pago voluntario observado/rechazado"),
    PAGVVN("PAGVVN", "Pago voluntario vencido sin confirmacion"),
    APEPRE("APEPRE", "Apelacion presentada"),
    APERAZ("APERAZ", "Apelacion rechazada - condena queda firme"),
    APEABS("APEABS", "Apelacion aceptada - absolucion en segunda instancia"),
    PLAVNC("PLAVNC", "Plazo de apelacion vencido sin apelacion presentada"),
    CONFIR("CONFIR", "Condena firme declarada (Slice 4)"),
    PCOINF("PCOINF", "Pago de condena informado por el infractor (Slice 5)"),
    PCOCNF("PCOCNF", "Pago de condena confirmado (Slice 5)"),
    PCOOBS("PCOOBS", "Pago de condena observado/rechazado (Slice 5)"),
    EXTDER("EXTDER", "Derivar a gestion externa - apremio/juzgado de paz (Slice 6)"),
    EXTRET("EXTRET", "Reingresar desde gestion externa (Slice 6)"),
    PAGAPR("PAGAPR", "Pago externo por apremio registrado (Slice 6)"),
    DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente"),
    CIERRA("CIERRA", "Acta cerrada definitivamente");

    private final String codigo;
    private final String descripcion;

    TipoEventoActa(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String codigo() { return codigo; }
    public String descripcion() { return descripcion; }

    public static TipoEventoActa deCodigo(String codigo) {
        for (TipoEventoActa t : values()) {
            if (t.codigo.equals(codigo)) return t;
        }
        throw new IllegalArgumentException("TipoEventoActa no reconocido: '" + codigo + "'");
    }
}

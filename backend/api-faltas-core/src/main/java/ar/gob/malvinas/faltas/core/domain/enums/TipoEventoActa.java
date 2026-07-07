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
 * Eventos productivos de apelacion: APEPRE (presentada), APEANL (en analisis), APERAZ (rechazada), APEABS (aceptada-absuelve), APEMCO (modifica condena), APENUL (nulidad), FALRMP (fallo reemplazado).
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
    APEANL("APEANL", "Apelacion pasada a EN_ANALISIS"),
    APEMCO("APEMCO", "Apelacion aceptada - condena modificada"),
    APENUL("APENUL", "Apelacion resuelta - nulidad declarada"),
    FALRMP("FALRMP", "Fallo reemplazado por decision en apelacion"),    DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente"),
    CIERRA("CIERRA", "Acta cerrada definitivamente"),
    // Slice 8F-11H: eventos del nuevo modelo de pagos (obligacion, forma, plan, movimiento)
    OBLDET("OBLDET", "Obligacion de pago determinada (nueva)"),
    OBLAUL("OBLAUL", "Obligacion de pago anulada"),
    DEBEMI("DEBEMI", "Deuda emitida en Ingresos"),
    FPCGEN("FPCGEN", "Forma de pago contado generada"),
    FPPGEN("FPPGEN", "Forma de pago plan generada"),
    FPREFN("FPREFN", "Refinanciacion de plan de pago generada"),
    PAGPRC("PAGPRC", "Pago procesado por Ingresos"),
    PAGCFT("PAGCFT", "Pago confirmado por Tesoreria"),
    PAGANU("PAGANU", "Pago anulado"),
    MOVPAG("MOVPAG", "Movimiento de pago registrado"),
    PLNCAI("PLNCAI", "Plan de pago caido"),
    NOTINT("NOTINT", "Intento de notificacion registrado"),
    NOTREI("NOTREI", "Reintento de notificacion registrado"),
    NOTRVE("NOTRVE", "Reintento de notificacion post vencimiento"),
    ACUGEN("ACUGEN", "Acuse de notificacion registrado"),
    ACUVAL("ACUVAL", "Acuse de notificacion validado"),
    LOTGEN("LOTGEN", "Lote de correo generado"),
    LOTEM("LOTEMI", "Lote de correo emitido"),
    LOTPRC("LOTPRC", "Lote de correo procesado"),
    LOTANU("LOTANU", "Lote de correo anulado"),
    PORPOS("PORPOS", "Notificacion positiva por portal infractor"),
    NOTSUP("NOTSUP", "Intento de notificacion superado por portal"),
    PLNCAN("PLNCAN", "Plan de pago cancelado"),

    DOCAMP("DOCAMP", "Documento adjuntado a apelacion"),

    // Slice 8F-11K: eventos de acceso via QR
    QRGEN("QRGENA", "Codigo QR de acceso generado para el acta"),
    QRACC("QRACCA", "Acceso valido al acta registrado via codigo QR");

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



package ar.gob.malvinas.faltas.core.domain.enums;

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
    FALRMP("FALRMP", "Fallo reemplazado por decision en apelacion"),
    DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente"),
    CIERRA("CIERRA", "Acta cerrada definitivamente"),
    OBLDET("OBLDET", "Obligacion de pago determinada"),
    OBLSFE("OBLSFE", "Obligacion de pago dejada sin efecto"),
    OBLREP("OBLREP", "Obligacion de pago reemplazada"),
    RCBGEN("RCBGEN", "Recibo al cobro generado"),
    PLNGEN("PLNGEN", "Plan de pago generado"),
    PLNREF("PLNREF", "Plan de pago refinanciado"),
    PLNANU("PLNANU", "Plan de pago anulado"),
    PAGREV("PAGREV", "Pago revertido/contracargado"),
    EMIANU("EMIANU", "Emision Ingresos anulada"),
    PAGANT("PAGANT", "Pago aplicado a obligacion anterior"),
    PAGRES("PAGRES", "Pago anterior resuelto administrativamente"),
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
    DOCAMP("DOCAMP", "Documento adjuntado a apelacion"),
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
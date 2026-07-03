package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de momento de numeracion de documentos.
 *
 * Refleja el catalogo momento_numeracion_docu (SMALLINT) de fal_documento_plantilla en MariaDB.
 * 5 valores definitivos cerrados en 8C-0D.
 *
 * Aplica SOLO a documentos. Las actas no usan este enum
 * (las actas tienen numeracion propia via nro_acta y talonarios).
 *
 * La numeracion documental se implementara en slices posteriores.
 */
public enum MomentoNumeracionDocu {

    NO_APLICA((short) 0),
    AL_CREAR((short) 1),
    AL_EMITIR((short) 2),
    AL_ENVIAR_A_FIRMA((short) 3),
    AL_FIRMAR((short) 4);

    private final short codigo;

    MomentoNumeracionDocu(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MomentoNumeracionDocu desdeCodigo(short codigo) {
        for (MomentoNumeracionDocu m : values()) {
            if (m.codigo == codigo) return m;
        }
        throw new IllegalArgumentException("MomentoNumeracionDocu desconocido: " + codigo);
    }
}

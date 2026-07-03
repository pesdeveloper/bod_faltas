package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del ciclo de vida de la apelacion del acta.
 *
 * PRESENTADA: apelacion presentada por el infractor (Slice 3B).
 * RECHAZADA: apelacion rechazada - condena queda firme (Slice futuro).
 * ACEPTADA_ABSUELVE: apelacion aceptada - absolucion en segunda instancia (Slice futuro).
 * SIN_EFECTO: apelacion anulada o sin efecto (Slice futuro).
 */
public enum EstadoApelacionActa {
    PRESENTADA,
    RECHAZADA,
    ACEPTADA_ABSUELVE,
    SIN_EFECTO
}
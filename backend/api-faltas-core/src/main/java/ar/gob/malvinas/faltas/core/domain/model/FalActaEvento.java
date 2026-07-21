package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Evento de dominio del expediente. Inmutable y append-only.
 *
 * Paridad campo-a-campo con fal_acta_evento (MariaDB) - SPEC-MODEL-DDL-CLOSURE-001.
 *
 * Reglas:
 * - Append-only: nunca se modifica ni elimina despues de persistir.
 * - No se usa payload JSON: el detalle estructurado vive en la tabla funcional
 *   correspondiente y el evento conserva la FK (idDocuRel, idNotifRel, idPresRel).
 * - id es Long generado por el repositorio (BIGINT AUTO_INCREMENT).
 * - tipoEvt es CHAR(6), hecho real de dominio, nunca evento de proyeccion/demo.
 * - Los codigos de estProcAnt/Nvo y sitAdmAnt/Nva son CHAR(4) estables
 *   (mismo esquema que fal_acta.est_proc_act y sit_adm_act), no ordinales.
 * - correlacionId permite idempotencia en comandos externos; tipo CHAR(36) (UUID).
 * - El orden del timeline es fhEvt + id; no se usa campo ordenLogico.
 *
 * Campos NO persistidos en MariaDB (eliminados del DDL - HUMAN_DECISION_CLOSED):
 * - actorRef: identidad estructurada via actorTipo + actorId. Marcado @Deprecated.
 * - descripcionLegible: reconstruible desde tipoEvt + actor + timestamp. Marcado @Deprecated.
 *
 * Construccion via Builder: FalActaEvento.builder().actaId(...).tipoEvt(...).build()
 */
public final class FalActaEvento {

    private final Long id;                              // PK BIGINT AUTO_INCREMENT (asignado por repo)
    private final Long actaId;                          // FK acta_id BIGINT NOT NULL
    private final TipoEventoActa tipoEvt;               // tipo_evt CHAR(6) NOT NULL
    private final OrigenEvento origenEvt;               // origen_evt SMALLINT NOT NULL
    private final LocalDateTime fhEvt;                  // fh_evt DATETIME(6) NOT NULL
    private final BloqueActual bloqueFunc;              // bloque_func CHAR(4) NULL
    private final EstadoProcesalActa estProcAnt;        // est_proc_ant CHAR(4) NULL
    private final EstadoProcesalActa estProcNvo;        // est_proc_nvo CHAR(4) NULL
    private final SituacionAdministrativaActa sitAdmAnt; // sit_adm_ant CHAR(4) NULL
    private final SituacionAdministrativaActa sitAdmNva; // sit_adm_nva CHAR(4) NULL
    private final ActorTipoEvento actorTipo;            // actor_tipo SMALLINT NULL
    private final String actorId;                       // actor_id CHAR(36) NULL
    /** @deprecated No persistido en MariaDB (HUMAN_DECISION_CLOSED). Identidad estructurada via actorTipo + actorId. */
    @Deprecated
    private final String actorRef;                      // NO PERSISTIR - actor_ref eliminado del DDL
    private final Long idDocuRel;                       // id_docu_rel BIGINT NULL FK
    private final Long idNotifRel;                      // id_notif_rel BIGINT NULL FK
    private final Long idPresRel;                       // id_pres_rel BIGINT NULL
    private final String idUserEvt;                     // id_user_evt CHAR(36) NULL
    private final boolean siEvtCierre;                  // si_evt_cierre BOOLEAN NOT NULL
    private final boolean siEvtExt;                     // si_evt_ext BOOLEAN NOT NULL
    private final boolean siPermiteReing;               // si_permite_reing BOOLEAN NOT NULL
    /** @deprecated No persistido en MariaDB (HUMAN_DECISION_CLOSED). Reconstruible desde tipoEvt + actor + timestamp. */
    @Deprecated
    private final String descripcionLegible;            // NO PERSISTIR - descripcion_legible eliminado del DDL
    private final String correlacionId;                 // correlacion_id CHAR(36) NULL

    private FalActaEvento(Builder b) {
        this.id = b.id;
        this.actaId = Objects.requireNonNull(b.actaId, "actaId requerido");
        this.tipoEvt = Objects.requireNonNull(b.tipoEvt, "tipoEvt requerido");
        this.origenEvt = b.origenEvt != null ? b.origenEvt : OrigenEvento.PROCESO_AUTOMATICO;
        this.fhEvt = Objects.requireNonNull(b.fhEvt, "fhEvt requerido");
        this.bloqueFunc = b.bloqueFunc;
        this.estProcAnt = b.estProcAnt;
        this.estProcNvo = b.estProcNvo;
        this.sitAdmAnt = b.sitAdmAnt;
        this.sitAdmNva = b.sitAdmNva;
        this.actorTipo = b.actorTipo;
        this.actorId = b.actorId;
        this.actorRef = b.actorRef;
        this.idDocuRel = b.idDocuRel;
        this.idNotifRel = b.idNotifRel;
        this.idPresRel = b.idPresRel;
        this.idUserEvt = b.idUserEvt;
        this.siEvtCierre = b.siEvtCierre;
        this.siEvtExt = b.siEvtExt;
        this.siPermiteReing = b.siPermiteReing;
        this.descripcionLegible = b.descripcionLegible;
        this.correlacionId = b.correlacionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Crea una copia con id asignado (para uso del repositorio al persistir). */
    public FalActaEvento conId(Long id) {
        return builder()
                .id(id)
                .actaId(this.actaId)
                .tipoEvt(this.tipoEvt)
                .origenEvt(this.origenEvt)
                .fhEvt(this.fhEvt)
                .bloqueFunc(this.bloqueFunc)
                .estProcAnt(this.estProcAnt)
                .estProcNvo(this.estProcNvo)
                .sitAdmAnt(this.sitAdmAnt)
                .sitAdmNva(this.sitAdmNva)
                .actorTipo(this.actorTipo)
                .actorId(this.actorId)
                .actorRef(this.actorRef)
                .idDocuRel(this.idDocuRel)
                .idNotifRel(this.idNotifRel)
                .idPresRel(this.idPresRel)
                .idUserEvt(this.idUserEvt)
                .siEvtCierre(this.siEvtCierre)
                .siEvtExt(this.siEvtExt)
                .siPermiteReing(this.siPermiteReing)
                .descripcionLegible(this.descripcionLegible)
                .correlacionId(this.correlacionId)
                .build();
    }

    // =====================================================================
    // ACCESSORS (inmutables)
    // =====================================================================

    public Long getId() { return id; }
    public Long actaId() { return actaId; }
    public TipoEventoActa tipoEvt() { return tipoEvt; }
    public OrigenEvento origenEvt() { return origenEvt; }
    public LocalDateTime fhEvt() { return fhEvt; }
    public BloqueActual bloqueFunc() { return bloqueFunc; }
    public EstadoProcesalActa estProcAnt() { return estProcAnt; }
    public EstadoProcesalActa estProcNvo() { return estProcNvo; }
    public SituacionAdministrativaActa sitAdmAnt() { return sitAdmAnt; }
    public SituacionAdministrativaActa sitAdmNva() { return sitAdmNva; }
    public ActorTipoEvento actorTipo() { return actorTipo; }
    public String actorId() { return actorId; }
    /** @deprecated No persistido en MariaDB. Usar actorTipo() + actorId(). */
    @Deprecated
    public String actorRef() { return actorRef; }
    public Long idDocuRel() { return idDocuRel; }
    public Long idNotifRel() { return idNotifRel; }
    public Long idPresRel() { return idPresRel; }
    public String idUserEvt() { return idUserEvt; }
    public boolean siEvtCierre() { return siEvtCierre; }
    public boolean siEvtExt() { return siEvtExt; }
    public boolean siPermiteReing() { return siPermiteReing; }
    /** @deprecated No persistido en MariaDB. Reconstruir desde tipoEvt + actor + timestamp. */
    @Deprecated
    public String descripcionLegible() { return descripcionLegible; }
    public String correlacionId() { return correlacionId; }

    // =====================================================================
    // BUILDER
    // =====================================================================

    public static final class Builder {
        private Long id;
        private Long actaId;
        private TipoEventoActa tipoEvt;
        private OrigenEvento origenEvt;
        private LocalDateTime fhEvt;
        private BloqueActual bloqueFunc;
        private EstadoProcesalActa estProcAnt;
        private EstadoProcesalActa estProcNvo;
        private SituacionAdministrativaActa sitAdmAnt;
        private SituacionAdministrativaActa sitAdmNva;
        private ActorTipoEvento actorTipo;
        private String actorId;
        private String actorRef;
        private Long idDocuRel;
        private Long idNotifRel;
        private Long idPresRel;
        private String idUserEvt;
        private boolean siEvtCierre = false;
        private boolean siEvtExt = false;
        private boolean siPermiteReing = false;
        private String descripcionLegible;
        private String correlacionId;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder actaId(Long actaId) { this.actaId = actaId; return this; }
        public Builder tipoEvt(TipoEventoActa tipoEvt) { this.tipoEvt = tipoEvt; return this; }
        public Builder origenEvt(OrigenEvento origenEvt) { this.origenEvt = origenEvt; return this; }
        public Builder fhEvt(LocalDateTime fhEvt) { this.fhEvt = fhEvt; return this; }
        public Builder bloqueFunc(BloqueActual bloqueFunc) { this.bloqueFunc = bloqueFunc; return this; }
        public Builder estProcAnt(EstadoProcesalActa estProcAnt) { this.estProcAnt = estProcAnt; return this; }
        public Builder estProcNvo(EstadoProcesalActa estProcNvo) { this.estProcNvo = estProcNvo; return this; }
        public Builder sitAdmAnt(SituacionAdministrativaActa sitAdmAnt) { this.sitAdmAnt = sitAdmAnt; return this; }
        public Builder sitAdmNva(SituacionAdministrativaActa sitAdmNva) { this.sitAdmNva = sitAdmNva; return this; }
        public Builder actorTipo(ActorTipoEvento actorTipo) { this.actorTipo = actorTipo; return this; }
        public Builder actorId(String actorId) { this.actorId = actorId; return this; }
        /** @deprecated actorRef no se persiste en MariaDB. Usar actorTipo + actorId. */
        @Deprecated
        public Builder actorRef(String actorRef) { this.actorRef = actorRef; return this; }
        public Builder idDocuRel(Long idDocuRel) { this.idDocuRel = idDocuRel; return this; }
        public Builder idNotifRel(Long idNotifRel) { this.idNotifRel = idNotifRel; return this; }
        public Builder idPresRel(Long idPresRel) { this.idPresRel = idPresRel; return this; }
        public Builder idUserEvt(String idUserEvt) { this.idUserEvt = idUserEvt; return this; }
        public Builder siEvtCierre(boolean siEvtCierre) { this.siEvtCierre = siEvtCierre; return this; }
        public Builder siEvtExt(boolean siEvtExt) { this.siEvtExt = siEvtExt; return this; }
        public Builder siPermiteReing(boolean siPermiteReing) { this.siPermiteReing = siPermiteReing; return this; }
        /** @deprecated descripcionLegible no se persiste en MariaDB. */
        @Deprecated
        public Builder descripcionLegible(String descripcionLegible) { this.descripcionLegible = descripcionLegible; return this; }
        public Builder correlacionId(String correlacionId) { this.correlacionId = correlacionId; return this; }

        public FalActaEvento build() {
            return new FalActaEvento(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FalActaEvento)) return false;
        FalActaEvento other = (FalActaEvento) o;
        return Objects.equals(id, other.id) && Objects.equals(actaId, other.actaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, actaId);
    }

    @Override
    public String toString() {
        return "FalActaEvento{id=" + id + ", actaId=" + actaId
                + ", tipoEvt=" + (tipoEvt != null ? tipoEvt.codigo() : null)
                + ", fhEvt=" + fhEvt + "}";
    }
}

-- =============================================================================
-- ALTER - fal_acta: situacion de identificacion del infractor
-- Script: alter-fal-acta-situacion-infractor-001-forward.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B1
-- Decision: DECISION_DDL-ACTA-INFR-01
--
-- MOTIVO
-- El modelo vigente declara id_persona_infractor NOT NULL y afirma que toda
-- acta tiene sujeto tecnico. Materialmente eso obliga a inventar una fila de
-- fal_persona cuando el infractor esta ausente, se niega a identificarse, no
-- aporta datos o los datos son insuficientes. Una persona inventada contamina
-- el padron, rompe DECISION_DDL-PERS-01 y afirma un hecho juridico falso.
-- ResultadoFirmaInfractor no cubre el caso: califica la firma, no la presencia
-- ni la identificacion.
--
--   antes                                   despues
--   (columna inexistente)                   situacion_identificacion_infractor
--                                           SMALLINT NOT NULL, sin DEFAULT
--   id_persona_infractor  BIGINT NOT NULL   BIGINT NULL
--   2 CHECK en fal_acta                     4 CHECK en fal_acta
--   56 columnas                             57 columnas
--
-- La FK fk_acta_persona_infractor y el indice ix_fal_acta_persona se conservan
-- sin cambio de nombre ni de accion referencial: siguen siendo RESTRICT en
-- UPDATE y en DELETE.
--
-- FUERA DE ALCANCE
--   No se agrega @Transactional (DECISION_OBS-TX-01, pendiente para B2).
--   No se toca codigo_qr ni id_user_alta.
--   No se remedia fal_observacion ni sus 21 triggers AFTER DELETE.
--   No se activa ningun bean JDBC.
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- Precondicion: create-bod-faltas-domain.sql ya ejecutado (fal_acta existe).
--
-- PRECHECK OBLIGATORIO
--   database/diagnostics/verify-fal-acta-situacion-infractor-001-precheck.sql
--   Los catorce controles deben devolver 0. Con una sola fila que no clasifique
--   de forma deterministica, DETENER: este script no infiere la situacion, no
--   crea personas y no sanea datos.
--
-- BACKUP
--   La seccion 2 emite el volcado logico completo de fal_acta y las columnas de
--   fal_persona que usa el backfill. Capturarlo y conservarlo antes de
--   continuar.
--
-- EJECUCION
--   Debe ejecutarse con un cliente que aborte en el primer error:
--   mariadb --abort-source-on-error ...
--   Exit code 0 no alcanza: si la salida contiene ERROR, el forward quedo
--   parcial y NO debe completarse a mano.
--
-- POSTCHECK
--   database/diagnostics/verify-fal-acta-situacion-infractor-001-postcheck.sql
--
-- ROLLBACK
--   database/ddl/alter-fal-acta-situacion-infractor-001-rollback.sql
--   Su ventana se cierra con la primera fila que tenga id_persona_infractor
--   NULL, es decir con la primera acta en situacion 3..6. El rollback no
--   inventa persona para hacer NOT NULL posible.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. PRECHECK resumido. El detalle esta en el script de diagnostico.
--    Las tres ultimas columnas deben devolver 0 antes de continuar.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                    AS filas_totales,
    SUM(a.id_persona_infractor IS NULL)                         AS persona_nula,
    SUM(p.id IS NULL)                                           AS persona_huerfana,
    SUM((p.tipo_documento IS NULL) <> (p.nro_doc IS NULL))      AS documento_incoherente
  FROM fal_acta a
  LEFT JOIN fal_persona p ON p.id = a.id_persona_infractor;

-- -----------------------------------------------------------------------------
-- 2. BACKUP LOGICO. Capturar la salida antes de continuar. No crea objetos en
--    la base: el respaldo vive fuera del motor, junto a la evidencia.
-- -----------------------------------------------------------------------------
SELECT id, id_tecnico, nro_acta, id_talonario, nro_talonario_usado,
       tipo_acta, origen_captura, id_dispositivo_captura, id_user_captura, fh_captura,
       lat_captura, lon_captura, precision_captura_m, fh_pos_captura, origen_pos_captura,
       fecha_acta, fecha_labrado, id_dep, ver_dep, id_insp, ver_insp,
       id_persona_infractor, id_domicilio_infractor_act, id_domicilio_notif_act,
       resumen_hecho, domicilio_hecho,
       id_loc_infr_malvinas, localidad_infr_malvinas_version_id,
       id_tca_infr_malvinas, calle_infr_malvinas_version_id,
       altura_infr, altura_origen_infr, si_altura_infr_estimada,
       lat_infr, lon_infr, origen_ubicacion_infr, si_ubicacion_infr_manual,
       si_dom_txt_infr, dom_txt_infr, si_eje_urb,
       codigo_qr, qr_payload_version,
       bloque_actual, est_proc_act, sit_adm_act,
       resultado_final, resultado_firma_infractor,
       id_motivo_archivo_actual, permite_reingreso, fh_cierre, fh_archivo,
       version_row, fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
  FROM fal_acta
 ORDER BY id;

-- Columnas de fal_persona que consume el backfill, solo de las personas
-- efectivamente vinculadas. No se exportan datos personales: nro_doc viaja
-- reducido a la presencia del dato.
SELECT p.id,
       p.tipo_documento,
       (p.nro_doc IS NOT NULL) AS tiene_nro_doc
  FROM fal_persona p
 WHERE EXISTS (SELECT 1 FROM fal_acta a WHERE a.id_persona_infractor = p.id)
 ORDER BY p.id;

-- -----------------------------------------------------------------------------
-- 3. ADD COLUMN nullable. Nullable a proposito: el NOT NULL se aplica despues
--    del backfill, porque de lo contrario el motor rechaza las filas
--    preexistentes. AFTER ver_insp deja la columna inmediatamente antes de
--    id_persona_infractor, que es la columna con la que forma la matriz.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    ADD COLUMN situacion_identificacion_infractor SMALLINT NULL
        COMMENT 'Situación de identificación del infractor al labrar (SituacionIdentificacionInfractor enum: 1=IDENTIFICADO_COMPLETO, 2=IDENTIFICADO_PARCIAL, 3=PRESENTE_SE_NIEGA, 4=PRESENTE_NO_APORTA_DATOS, 5=AUSENTE, 6=DATOS_INSUFICIENTES). Sin DEFAULT: es una calificación jurídica del acto y el alta la informa siempre (DECISION_DDL-ACTA-INFR-01)'
        AFTER ver_insp;

-- -----------------------------------------------------------------------------
-- 4. BACKFILL deterministico por JOIN a fal_persona.
--    Con la columna todavia NOT NULL no existe una tercera rama posible:
--      1 IDENTIFICADO_COMPLETO  persona con tipo_documento y nro_doc informados
--      2 IDENTIFICADO_PARCIAL   persona con ambos campos NULL
--    El nombre fisico canonico es tipo_documento; tipo_doc esta prohibido.
--    Ninguna fila se clasifica por heuristica. Una combinacion con solo uno de
--    los dos campos informado no se toca aqui: el precheck P08 la bloquea antes
--    y el paso 5 la deja visible como fila sin clasificar.
-- -----------------------------------------------------------------------------
UPDATE fal_acta a
  JOIN fal_persona p ON p.id = a.id_persona_infractor
   SET a.situacion_identificacion_infractor = 1
 WHERE p.tipo_documento IS NOT NULL
   AND p.nro_doc IS NOT NULL;

UPDATE fal_acta a
  JOIN fal_persona p ON p.id = a.id_persona_infractor
   SET a.situacion_identificacion_infractor = 2
 WHERE p.tipo_documento IS NULL
   AND p.nro_doc IS NULL;

-- -----------------------------------------------------------------------------
-- 5. Verificacion del backfill antes de endurecer la columna.
--    sin_clasificar y fuera_de_1_2 deben ser 0. Si no lo son, DETENER: el paso
--    6 fallaria y dejaria el forward parcial.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                     AS filas_totales,
    SUM(situacion_identificacion_infractor IS NULL)              AS sin_clasificar,
    SUM(situacion_identificacion_infractor NOT IN (1, 2))        AS fuera_de_1_2,
    SUM(situacion_identificacion_infractor = 1)                  AS clasificadas_completo,
    SUM(situacion_identificacion_infractor = 2)                  AS clasificadas_parcial
  FROM fal_acta;

-- -----------------------------------------------------------------------------
-- 6. NOT NULL. Se aplica recien ahora, despues del backfill.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    MODIFY COLUMN situacion_identificacion_infractor SMALLINT NOT NULL
        COMMENT 'Situación de identificación del infractor al labrar (SituacionIdentificacionInfractor enum: 1=IDENTIFICADO_COMPLETO, 2=IDENTIFICADO_PARCIAL, 3=PRESENTE_SE_NIEGA, 4=PRESENTE_NO_APORTA_DATOS, 5=AUSENTE, 6=DATOS_INSUFICIENTES). Sin DEFAULT: es una calificación jurídica del acto y el alta la informa siempre (DECISION_DDL-ACTA-INFR-01)';

-- -----------------------------------------------------------------------------
-- 7. id_persona_infractor pasa a nullable. La FK y el indice se conservan:
--    MODIFY COLUMN no los toca y este script no los baja ni los recrea.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    MODIFY COLUMN id_persona_infractor BIGINT NULL
        COMMENT 'FK a la persona infractora. NULL cuando no hay sujeto identificado (situación 3..6); nunca una persona centinela, ficticia o con documento inventado (DECISION_DDL-ACTA-INFR-01)';

-- -----------------------------------------------------------------------------
-- 8. CHECK de dominio. Acota los codigos igual que chk_acta_tipo acota
--    tipo_acta. Se agrega despues de los dos MODIFY.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    ADD CONSTRAINT chk_acta_situacion_infractor
        CHECK (situacion_identificacion_infractor BETWEEN 1 AND 6);

-- -----------------------------------------------------------------------------
-- 9. CHECK de coherencia. Hay persona exactamente cuando hay identificacion,
--    completa o parcial.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    ADD CONSTRAINT chk_acta_infractor_coherente
        CHECK (
            (situacion_identificacion_infractor IN (1,2)
              AND id_persona_infractor IS NOT NULL)
            OR
            (situacion_identificacion_infractor IN (3,4,5,6)
              AND id_persona_infractor IS NULL)
        );

-- -----------------------------------------------------------------------------
-- 10. POSTCHECK resumido. El detalle esta en el script de diagnostico.
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable, column_default, extra
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta'
   AND column_name IN ('situacion_identificacion_infractor', 'id_persona_infractor')
 ORDER BY ordinal_position;

SELECT constraint_name, check_clause
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
 ORDER BY constraint_name;

-- =============================================================================
-- FIN DEL FORWARD - fal_acta situacion de identificacion del infractor
-- =============================================================================

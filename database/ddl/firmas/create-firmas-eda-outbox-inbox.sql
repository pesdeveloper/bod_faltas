-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- CREATE — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
--          (bounded context Firmas)
-- Script: create-firmas-eda-outbox-inbox.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-028)
--
-- Firmas es una aplicación/repositorio distinto de este monorepo (no existe
-- `backend/api-firmas` en Bod-Faltas) y su base de datos no es propiedad de
-- este repositorio. Este script es EJECUTABLE (no comentado) y queda bajo
-- responsabilidad del equipo de Firmas: crea el Outbox/checkpoint/Inbox/
-- Inbox error locales de Firmas, con EXACTAMENTE la misma estructura de
-- columnas que fal_outbox/fal_bus_checkpoint/fal_inbox/fal_inbox_error
-- (database/ddl/alter-firmas-eda-closure-001-forward.sql §5.1-§5.5), solo
-- con el prefijo `fir_` en lugar de `fal_`. Modelo mínimo aprobado: sin
-- fan-out central, sin rutinas SQL SECURITY DEFINER, sin vistas
-- diagnósticas; cada aplicación (Faltas, Firmas) mantiene su propio Outbox/
-- checkpoint/Inbox/Inbox error locales alrededor del único `bus_evento`
-- central.
--
-- Este script deliberadamente NO ejecuta `USE <base>`: el nombre de la base
-- de datos de Firmas no es un objeto lógico de Bod-Faltas y no corresponde
-- declararlo aquí. El operador debe seleccionar la base de destino antes de
-- ejecutar este archivo (ej. `USE <base_de_firmas>;` manual, o el parámetro
-- de conexión del cliente).
--
-- INSTRUCCIONES DE EJECUCIÓN (cuando el equipo de Firmas lo autorice):
--   1. Seleccionar explícitamente la base de datos de destino (fuera de este
--      script).
--   2. Ejecutar database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-precheck.sql
--      (aborta si DATABASE() IS NULL o si los objetos ya existen).
--   3. Ejecutar este script íntegro.
--   4. Ejecutar database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-postcheck.sql.
--
-- Rollback: database/ddl/firmas/rollback-firmas-eda-outbox-inbox.sql
--
-- Fuente normativa (lado Faltas): backend/api-faltas-core/docs/spec-as-source/
--   50-persistence/firma-eda-physical-model.md §9
--   30-integrations/municipal-eventing.md, 30-integrations/eventing-operations.md
-- =============================================================================

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- fir_outbox — misma estructura que fal_outbox (forward §5.1). Se escribe
-- junto con el cambio de dominio de Firmas, en la misma transacción local.
-- El relay reutiliza el mismo evento_id en cada reintento (idempotencia de
-- publicación por UNIQUE(evento_id) en bus_evento).
-- status: 0=PENDIENTE, 1=PUBLICADO, 2=REINTENTO, 3=ERROR_DEFINITIVO.
-- ----------------------------------------------------------------------------
CREATE TABLE fir_outbox (
    outbox_id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico local. USO: PK. REGLA: generado por DB.',
    evento_id           BINARY(16)       NOT NULL                COMMENT 'CONTENIDO: UUID logico del evento (identidad estable de negocio), en binario. USO: idempotencia de publicacion; se reutiliza en cada reintento. REGLA: unico; generado por Firmas al crear la fila.',
    evento_codigo        VARCHAR(16)      NOT NULL                COMMENT 'CONTENIDO: Codigo compacto del tipo de evento (ver catalogo de codigos en firma-eda-physical-model.md §3). USO: enrutamiento; se modela como enum en el dominio de Firmas. REGLA: coincide con el catalogo vigente de codigos de evento.',
    schema_version       SMALLINT UNSIGNED NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Version de esquema del payload. USO: compatibilidad de contrato. REGLA: entero >= 1.',
    agregado_codigo      VARCHAR(12)      NULL                    COMMENT 'CONTENIDO: Codigo compacto del tipo de agregado emisor en Firmas, cuando aplica. USO: indexacion secundaria opcional. REGLA: NULL si el agregado no tiene una identidad numerica unica indexable; la identidad completa vive siempre en payload.',
    agregado_id          BIGINT           NULL                    COMMENT 'CONTENIDO: Identificador numerico del agregado emisor en Firmas, cuando es representable como BIGINT. USO: indexacion secundaria opcional. REGLA: NULL si la clave del agregado no es un unico valor numerico; nunca la unica fuente de identidad (esa vive en payload).',
    agregado_version     BIGINT UNSIGNED  NULL                    COMMENT 'CONTENIDO: Version/secuencia del agregado al momento de publicar. USO: orden logico por agregado. REGLA: NULL si no aplica; monotonicamente creciente por agregado cuando se informa.',
    correlation_id       BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID de correlacion end-to-end, en binario. USO: trazabilidad distribuida. REGLA: NULL si no aplica.',
    causation_id         BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID del evento/comando causante inmediato, en binario. USO: trazabilidad causal. REGLA: NULL si no hay causante directo.',
    payload              JSON             NOT NULL                COMMENT 'CONTENIDO: Cuerpo del evento segun el contrato Faltas<->Firmas (40-api/firma-event-contracts.md). USO: dato transportado. REGLA: JSON valido; nunca se actualiza tras el insert.',
    status                TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'CONTENIDO: Estado del relay Outbox->bus_evento. VALORES: 0=PENDIENTE, 1=PUBLICADO, 2=REINTENTO, 3=ERROR_DEFINITIVO.',
    si_error              BOOLEAN          NOT NULL DEFAULT FALSE  COMMENT 'CONTENIDO: TRUE si el ultimo intento de publicacion fallo. USO: senal simple de error activo, sin cuarentena central. REGLA: el detalle tecnico completo queda en logs correlacionados por evento_id.',
    attempts              SMALLINT UNSIGNED NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Cantidad de intentos de publicacion realizados. USO: control de reintentos. REGLA: incrementa en cada intento fallido.',
    next_attempt_at       DATETIME(6)      NULL                    COMMENT 'CONTENIDO: Proximo instante habilitado para reintentar. USO: backoff simple de aplicacion. REGLA: NULL si status no requiere reintento.',
    ocurrido_en           DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de ocurrencia del hecho de dominio en Firmas. USO: linea de tiempo de negocio. REGLA: suministrado por el reloj de Firmas.',
    creado_en             DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de insercion en el Outbox. USO: auditoria tecnica.',
    publicado_en          DATETIME(6)      NULL                    COMMENT 'CONTENIDO: Momento de publicacion exitosa en bus_evento. USO: auditoria de latencia del relay. REGLA: NULL mientras status <> PUBLICADO.',
    PRIMARY KEY (outbox_id),
    UNIQUE KEY uq_fir_outbox_evento_id (evento_id),
    INDEX ix_fir_outbox_status (status, next_attempt_at),
    INDEX ix_fir_outbox_agregado (agregado_codigo, agregado_id, agregado_version),
    CONSTRAINT chk_fir_outbox_status CHECK (status BETWEEN 0 AND 3),
    CONSTRAINT chk_fir_outbox_schema_version CHECK (schema_version >= 1),
    CONSTRAINT chk_fir_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_fir_outbox_si_error CHECK (si_error IN (FALSE, TRUE))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Outbox local de Firmas para el bus de eventos municipal. USO: publicacion atomica con el commit de dominio de Firmas (patron Outbox); el relay reutiliza el mismo evento_id en cada reintento. Misma estructura que fal_outbox (database/ddl/alter-firmas-eda-closure-001-forward.sql §5.1).';

-- ----------------------------------------------------------------------------
-- fir_bus_checkpoint — misma estructura que fal_bus_checkpoint (forward
-- §5.3). Checkpoint local de lectura del bus para el consumidor Firmas.
-- Tabla singleton (checkpoint_id = 1 fijo por CHECK). El avance del
-- checkpoint y el INSERT en fir_inbox deben quedar coordinados en la misma
-- transaccion local.
-- ----------------------------------------------------------------------------
CREATE TABLE fir_bus_checkpoint (
    checkpoint_id       TINYINT UNSIGNED NOT NULL              COMMENT 'CONTENIDO: Identificador fijo de fila singleton. USO: PK. REGLA: siempre 1 (chk_fir_bus_checkpoint_singleton); nunca hay una segunda fila.',
    ultimo_evento_id     BIGINT UNSIGNED  NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Ultimo bus_evento_id examinado/copiado por el lector local de Firmas. USO: base de la consulta bus_evento WHERE bus_evento_id > ultimo_evento_id ORDER BY bus_evento_id. REGLA: nunca disminuye; se actualiza en la misma transaccion local que el INSERT en fir_inbox de los eventos relevantes del lote.',
    actualizado_en       DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento de la ultima actualizacion del checkpoint. USO: auditoria tecnica y diagnostico de lectores detenidos. REGLA: suministrado por el reloj de Firmas.',
    PRIMARY KEY (checkpoint_id),
    CONSTRAINT chk_fir_bus_checkpoint_singleton CHECK (checkpoint_id = 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Checkpoint local, singleton, del lector del bus de eventos municipal para Firmas. USO: recordar hasta que bus_evento_id ya se examino. REGLAS CLAVE: fila unica (checkpoint_id=1); avanza junto con el INSERT en fir_inbox del mismo lote, en la misma transaccion local. Misma estructura que fal_bus_checkpoint (database/ddl/alter-firmas-eda-closure-001-forward.sql §5.3).';

-- Seed de fir_bus_checkpoint — fila singleton inicial en 0
INSERT INTO fir_bus_checkpoint (checkpoint_id, ultimo_evento_id, actualizado_en)
VALUES (1, 0, UTC_TIMESTAMP(6));

-- ----------------------------------------------------------------------------
-- fir_inbox — misma estructura que fal_inbox (forward §5.4), con
-- PK = bus_evento_id (cursor tecnico, no un id propio) + UNIQUE(evento_id)
-- (identidad logica estable, nunca UNIQUE(consumer, event_id): no hay
-- columna consumer aqui). La Inbox de Firmas solo contiene eventos
-- relevantes para Firmas, copiados desde bus_evento por su lector local
-- (transaccion de ingestion, independiente del procesamiento). Firmas
-- consume los eventos que Faltas publica en el bus central (ej.
-- FirmaSolicitada, SolicitudFirmaCancelada, SolicitudFirmaVencida; ver
-- 40-api/firma-event-contracts.md).
-- status: 0=PENDIENTE, 1=PROCESANDO, 2=PROCESADO, 3=REINTENTO,
-- 4=ERROR_DEFINITIVO, 5=IGNORADO. Un rechazo funcional de dominio cierra
-- en PROCESADO (si_error=FALSE), sin fila en fir_inbox_error.
-- ----------------------------------------------------------------------------
CREATE TABLE fir_inbox (
    bus_evento_id        BIGINT UNSIGNED  NOT NULL              COMMENT 'CONTENIDO: bus_evento_id del evento copiado desde el bus central. USO: PK operativa principal; idempotencia de copia local. REGLA: coincide con bus_evento.bus_evento_id del evento origen; nunca se genera un id tecnico propio.',
    evento_id             BINARY(16)       NOT NULL              COMMENT 'CONTENIDO: UUID logico del evento, igual al evento_id de bus_evento/Outbox de origen. USO: identidad estable de negocio, independiente del cursor tecnico bus_evento_id. REGLA: unico (uq_fir_inbox_evento_id); nunca UNIQUE(consumer, event_id).',
    productor_codigo      VARCHAR(8)       NOT NULL              COMMENT 'CONTENIDO: Codigo compacto de la aplicacion productora del evento. USO: auditoria de origen. REGLA: copiado de bus_evento.productor_codigo al momento de la ingestion.',
    evento_codigo         VARCHAR(16)      NOT NULL              COMMENT 'CONTENIDO: Codigo compacto del tipo de evento consumido. USO: enrutamiento al handler de dominio local de Firmas. REGLA: coincide con bus_evento.evento_codigo del evento origen.',
    schema_version        SMALLINT UNSIGNED NOT NULL             COMMENT 'CONTENIDO: Version de esquema del payload copiado. USO: compatibilidad de contrato en el procesamiento local. REGLA: copiado de bus_evento.schema_version al momento de la ingestion.',
    agregado_codigo       VARCHAR(12)      NULL                  COMMENT 'CONTENIDO: Codigo compacto del tipo de agregado local afectado en Firmas, cuando aplica. USO: diagnostico y trazabilidad del efecto. REGLA: NULL si no aplica; la identidad completa vive en payload.',
    agregado_id           BIGINT           NULL                  COMMENT 'CONTENIDO: Identificador numerico del agregado local afectado, cuando es representable como BIGINT. USO: diagnostico y trazabilidad del efecto. REGLA: NULL si no aplica.',
    agregado_version      BIGINT UNSIGNED  NULL                  COMMENT 'CONTENIDO: Version/secuencia del agregado local tras aplicar el efecto, cuando se informa. USO: orden logico local. REGLA: NULL si no aplica.',
    correlation_id        BINARY(16)       NULL                  COMMENT 'CONTENIDO: UUID de correlacion end-to-end, copiado del evento origen. USO: trazabilidad distribuida. REGLA: NULL si no aplica.',
    causation_id          BINARY(16)       NULL                  COMMENT 'CONTENIDO: UUID del evento/comando causante inmediato, copiado del evento origen. USO: trazabilidad causal. REGLA: NULL si no aplica.',
    payload               JSON             NOT NULL              COMMENT 'CONTENIDO: Copia del payload del evento, tal como llego en bus_evento. USO: dato de entrada para el procesamiento local; evita relecturas del bus. REGLA: JSON valido; nunca se actualiza tras el insert.',
    status                 TINYINT UNSIGNED NOT NULL DEFAULT 0    COMMENT 'CONTENIDO: Progreso del procesamiento local. VALORES: 0=PENDIENTE, 1=PROCESANDO, 2=PROCESADO, 3=REINTENTO, 4=ERROR_DEFINITIVO, 5=IGNORADO. Un rechazo funcional de dominio cierra en PROCESADO, nunca en REINTENTO/ERROR_DEFINITIVO.',
    si_error               BOOLEAN          NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si el ultimo intento de procesamiento fallo por un error tecnico. USO: senal simple de error activo, sin texto de error en esta fila. REGLA: un rechazo funcional deja si_error=FALSE y status=PROCESADO; el detalle de cada fallo tecnico se inserta en fir_inbox_error.',
    attempts               SMALLINT UNSIGNED NOT NULL DEFAULT 0   COMMENT 'CONTENIDO: Cantidad de intentos de procesamiento local realizados. USO: diagnostico de reintentos. REGLA: incrementa en cada intento fallido; corresponde a attempt_number de la ultima fila en fir_inbox_error.',
    next_attempt_at        DATETIME(6)      NULL                  COMMENT 'CONTENIDO: Proximo instante habilitado para reintentar el procesamiento. USO: backoff simple de aplicacion. REGLA: NULL si status no requiere reintento.',
    ocurrido_en            DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento de ocurrencia del hecho de dominio en el productor, copiado del evento origen. USO: linea de tiempo de negocio en el procesamiento local. REGLA: copiado de bus_evento.ocurrido_en al momento de la ingestion.',
    recibido_en            DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento en que el lector local copio el evento a la Inbox. USO: auditoria de latencia. REGLA: suministrado por el reloj de Firmas.',
    procesado_en           DATETIME(6)      NULL                  COMMENT 'CONTENIDO: Momento en que el worker local termino de aplicar el efecto con exito. USO: auditoria de latencia de procesamiento. REGLA: NULL mientras status <> PROCESADO.',
    PRIMARY KEY (bus_evento_id),
    UNIQUE KEY uq_fir_inbox_evento_id (evento_id),
    INDEX ix_fir_inbox_status (status, next_attempt_at),
    INDEX ix_fir_inbox_evento_codigo (evento_codigo, bus_evento_id),
    INDEX ix_fir_inbox_agregado (agregado_codigo, agregado_id, agregado_version),
    CONSTRAINT chk_fir_inbox_status CHECK (status BETWEEN 0 AND 5),
    CONSTRAINT chk_fir_inbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_fir_inbox_si_error CHECK (si_error IN (FALSE, TRUE))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Inbox local del consumidor Firmas. USO: separa la lectura del bus (fir_bus_checkpoint) del procesamiento funcional; garantiza que cada evento relevante se procesa una sola vez. REGLAS CLAVE: PK = bus_evento_id (cursor tecnico) mas UNIQUE(evento_id) (identidad logica estable); envelope completo copiado en la ingestion; un error tecnico en una fila no detiene el procesamiento de otras filas PENDIENTE/REINTENTO; un rechazo funcional cierra en PROCESADO sin fila en fir_inbox_error; detalle de fallos tecnicos en fir_inbox_error, nunca texto largo en esta tabla. Misma estructura que fal_inbox (database/ddl/alter-firmas-eda-closure-001-forward.sql §5.4).';

-- ----------------------------------------------------------------------------
-- fir_inbox_error — misma estructura que fal_inbox_error (forward §5.5).
-- Tabla local de errores de procesamiento de fir_inbox. No hay tabla
-- central de errores: cada consumidor guarda su propio historial.
-- ----------------------------------------------------------------------------
CREATE TABLE fir_inbox_error (
    inbox_error_id      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    bus_evento_id        BIGINT UNSIGNED  NOT NULL                COMMENT 'CONTENIDO: FK a la fila de fir_inbox sobre la que ocurrio el error. USO: vinculo error-evento. REGLA: referencia fir_inbox(bus_evento_id).',
    attempt_number        SMALLINT UNSIGNED NOT NULL               COMMENT 'CONTENIDO: Numero de intento en el que ocurrio este error. USO: correlacion con fir_inbox.attempts. REGLA: unico junto a bus_evento_id (uq_fir_inbox_error_attempt); coincide con el valor de attempts en fir_inbox al momento del fallo.',
    error_code            SMALLINT UNSIGNED NOT NULL               COMMENT 'CONTENIDO: Codigo numerico de error, modelado como enum local de Firmas. USO: diagnostico agregable sin texto libre. REGLA: no vacio.',
    error_detail          VARCHAR(255)     NULL                    COMMENT 'CONTENIDO: Resumen breve del error, maximo 255 caracteres. USO: diagnostico rapido. REGLA: NULL admitido; nunca un stack trace (el stack trace completo vive en logs correlacionados por bus_evento_id).',
    ocurrido_en           DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento del error. USO: auditoria temporal. REGLA: suministrado por el reloj de Firmas.',
    PRIMARY KEY (inbox_error_id),
    UNIQUE KEY uq_fir_inbox_error_attempt (bus_evento_id, attempt_number),
    INDEX ix_fir_inbox_error_evento (bus_evento_id, ocurrido_en),
    CONSTRAINT fk_fir_inbox_error_inbox FOREIGN KEY (bus_evento_id)
        REFERENCES fir_inbox (bus_evento_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Historial local de errores de procesamiento de fir_inbox (tabla local, no central). USO: diagnostico de fallos de procesamiento sin guardar texto largo en fir_inbox. REGLAS CLAVE: una fila por intento fallido (UNIQUE(bus_evento_id, attempt_number)); error_detail acotado a 255 caracteres, nunca un stack trace; FK RESTRICT a fir_inbox. Misma estructura que fal_inbox_error (database/ddl/alter-firmas-eda-closure-001-forward.sql §5.5).';

-- =============================================================================
-- FIN DEL SCRIPT — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
--                   (bounded context Firmas)
-- =============================================================================
-- Tablas creadas: 4 (fir_outbox, fir_bus_checkpoint, fir_inbox, fir_inbox_error)
-- Sin rutinas, sin vistas, sin bus_definer: mismo modelo minimo que el lado
-- Faltas.
-- Rollback: database/ddl/firmas/rollback-firmas-eda-outbox-inbox.sql
-- =============================================================================

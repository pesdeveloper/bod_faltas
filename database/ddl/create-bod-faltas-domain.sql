-- APROBADO PARA EJECUCIÓN MANUAL CONTROLADA
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- DDL DOMINIO BOD FALTAS — MariaDB
-- Script: create-bod-faltas-domain.sql
-- Trabajo: DDL-MARIADB-MANUAL-001-FULL-R1
--
-- 64 tablas TO_CREATE definidas (de 64)
-- 1 tabla adoptada: fal_rubro_version (PREEXISTING_CANONICAL_ADOPTED)
-- Estado: DDL AUDITADO Y APTO PARA EJECUCIÓN MANUAL CONTROLADA
-- =============================================================================
--
-- 64 tablas a crear + 1 preexistente (fal_rubro_version) = 65 canónicas
-- Motor: InnoDB | Charset: utf8mb4 | Collation: utf8mb4_uca1400_ai_ci
--
-- INSTRUCCIONES DE EJECUCIÓN:
--   1. Auditar el ZIP integral antes de ejecutar.
--   2. Conectarse a la base de datos de destino (USE sb_faltas_db).
--   3. Ejecutar este script íntegro con un cliente MariaDB (mysql / HeidiSQL).
--   4. Verificar con database/diagnostics/verify-domain-schema.sql.
--
-- ADVERTENCIA:
--   NO ejecutar en producción sin revisión manual previa.
--   NO ejecutar si ya existen las tablas del dominio (no es idempotente).
--   NO modificar fal_rubro_version ni los objetos baseline geo_*.
--
-- Fuente normativa: backend/api-faltas-core/docs/spec-as-source/
-- Decisiones físicas cerradas: 50-persistence/ddl-decisions.md
-- Modelo lógico: 50-persistence/mariadb-logical-model.md
-- Inventario canónico: database/design/canonical-table-inventory.md
-- Orden topológico: database/design/dependency-order.md
-- Scope completo: database/design/ddl-full-scope.md
--
-- =============================================================================

USE sb_faltas_db;

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- =============================================================================
-- SECCIÓN 01 — G1: Catálogos y raíces sin dependencias externas (tablas 1–12)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. fal_dependencia
--    Unidades administrativas (organismos/dependencias) del municipio.
--    Nota: FK auto-referencial id_dep_padre se agrega vía ALTER al final.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_dependencia (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico autoincremental. USO: PK de la tabla. REGLA: generado por DB.',
    cod_dep         VARCHAR(8)     NULL                   COMMENT 'CONTENIDO: Codigo alfanumerico de la dependencia. USO: identificacion corta operativa. REGLA: unico cuando se informa; NULL permitido.',
    nom_dep         VARCHAR(48)    NOT NULL               COMMENT 'CONTENIDO: Nombre completo de la dependencia. USO: visualizacion en bandejas y actas. REGLA: no puede ser vacio.',
    id_dep_padre    BIGINT         NULL                   COMMENT 'CONTENIDO: FK autorreferencial a dependencia padre. USO: jerarquia organizacional. REGLA: NULL = raiz; id_dep_padre <> id. FK agregada via ALTER TABLE post-creacion.',
    si_activa       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de dependencias vigentes. REGLA: FALSE = dada de baja logicamente.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_dep_cod (cod_dep),
    CONSTRAINT chk_dep_no_self CHECK (id_dep_padre IS NULL OR id_dep_padre <> id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Maestro de dependencias y organismos administrativos del municipio. USO: parametro de adscripcion de inspectores y actas. RAZON: centralizar la jerarquia organizacional versionable. REGLAS CLAVE: cod_dep unico donde informado; id_dep_padre autorreferencial (NULL = raiz); chk_dep_no_self impide que una dependencia sea su propio padre.';

-- ----------------------------------------------------------------------------
-- 2. fal_inspector
--    Inspectores y agentes habilitados para labrar actas.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_inspector (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_user         CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario IDP del inspector. USO: vinculo con el sistema de identidad. REGLA: unico por inspector.',
    legajo_insp     INT            NOT NULL               COMMENT 'CONTENIDO: Numero de legajo del inspector. USO: identificacion administrativa. REGLA: positivo (> 0); unico.',
    nom_insp        VARCHAR(36)    NOT NULL               COMMENT 'CONTENIDO: Nombre para mostrar del inspector. USO: bandejas y actas. REGLA: no puede ser vacio.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de inspectores habilitados. REGLA: FALSE = dado de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_insp_id_user (id_user),
    UNIQUE KEY uq_fal_insp_legajo (legajo_insp),
    CONSTRAINT chk_fal_insp_legajo CHECK (legajo_insp > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Maestro de inspectores habilitados para labrar actas. USO: adscripcion a actas; control de habilitacion. RAZON: centralizar el catalogo de agentes habilitados. REGLAS CLAVE: legajo positivo y unico; id_user unico; si_activo controla habilitacion.';

-- ----------------------------------------------------------------------------
-- 3. fal_firmante
--    Firmantes/autorizados para firmar documentos institucionales.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_firmante (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_user         CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario IDP del firmante. USO: vinculo de identidad. REGLA: unico por firmante.',
    nom_firmante    VARCHAR(48)    NOT NULL               COMMENT 'CONTENIDO: Nombre visible del firmante (actual). USO: visualizacion en documentos. REGLA: historico en versiones.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de habilitacion. USO: filtrado de firmantes activos. REGLA: FALSE = dado de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_firmante_id_user (id_user)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Maestro de firmantes institucionales para documentos del expediente. USO: parametro de firma en plantillas. RAZON: centralizar autoridades firmantes. REGLAS CLAVE: id_user unico; historico de nombre y cargo en fal_firmante_version.';

-- ----------------------------------------------------------------------------
-- 4. fal_persona
--    Personas físicas y jurídicas (infractores, presentantes).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_persona (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico autoincremental. USO: PK. REGLA: generado por DB.',
    tipo_persona            SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de persona. USO: discriminacion fisica/juridica. REGLA: 1=FISICA, 2=JURIDICA (TipoPersona enum).',
    tipo_documento          SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de documento de identidad. USO: determina estructura del documento. VALORES: 1=DNI, 2=CUIT, 3=CUIL, 4=PASAPORTE, 5=DNI_EXTRANJERO, 9=OTRO (TipoDocumentoPersona enum).',
    prefijo_cuit_cuil       TINYINT UNSIGNED NULL                 COMMENT 'CONTENIDO: Prefijo de CUIT/CUIL (ej: 20, 23, 27, 30). USO: solo para CUIT/CUIL. REGLA: NULL para documentos simples; BETWEEN 0 AND 99 cuando informado.',
    nro_doc                 INT UNSIGNED   NOT NULL               COMMENT 'CONTENIDO: Numero de documento sin prefijo ni digito. USO: parte principal del documento. REGLA: BETWEEN 1 AND 99999999.',
    digito_verificador      TINYINT UNSIGNED NULL                 COMMENT 'CONTENIDO: Digito verificador de CUIT/CUIL. USO: solo para CUIT/CUIL. REGLA: NULL para documentos simples; BETWEEN 0 AND 9 cuando informado.',
    doc_key                 VARCHAR(20)    AS (CASE WHEN tipo_documento IN (2, 3) THEN CONCAT(LPAD(COALESCE(prefijo_cuit_cuil, 0), 2, 0), LPAD(nro_doc, 8, 0), COALESCE(digito_verificador, 0)) ELSE CONCAT(tipo_documento, '-', nro_doc) END) STORED COMMENT 'CONTENIDO: Clave funcional normalizada para unicidad de documento. USO: UNIQUE constraint. REGLA: generada por DB. TIPOS CUIT/CUIL (2,3): concatena prefijo+nro_doc+digito; otros tipos: tipo_documento-nro_doc.',
    apellido                VARCHAR(24)    NULL                   COMMENT 'CONTENIDO: Apellido de la persona fisica. USO: solo personas fisicas. REGLA: NULL para personas juridicas.',
    nombres                 VARCHAR(36)    NULL                   COMMENT 'CONTENIDO: Nombres de la persona fisica. USO: solo personas fisicas. REGLA: NULL para personas juridicas.',
    razon_social            VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Razon social de la persona juridica. USO: solo personas juridicas. REGLA: NULL para personas fisicas.',
    nombre_mostrar          VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Nombre calculado para mostrar. USO: bandejas y documentos. REGLA: apellido+nombres o razon social.',
    email_principal         VARCHAR(160)   NULL                   COMMENT 'CONTENIDO: Email principal de contacto. USO: notificaciones digitales. REGLA: nullable.',
    telefono_principal      VARCHAR(20)    NULL                   COMMENT 'CONTENIDO: Telefono principal de contacto. USO: notificaciones. REGLA: nullable.',
    id_suj                  TINYINT UNSIGNED NULL                   COMMENT 'CONTENIDO: Codigo de tipo de sujeto en Ingresos Municipales (1=INMUEBLE, 2=COMERCIO, 3=RODADO, 18=CEMENTERIO, 20=FALTAS, 99=VARIOS y otros). USO: sincronizacion con padron externo. REGLA: catalogo abierto; no es el ID unico de una persona; rango 1-255; nullable. HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    id_bie                  MEDIUMINT UNSIGNED NULL                   COMMENT 'CONTENIDO: ID del bien o cuenta dentro del tipo de sujeto correspondiente. USO: sincronizacion con padron externo. REGLA: rango 1-9999999; depende de id_suj; nullable. HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    suj_bie_estado          SMALLINT       NULL                   COMMENT 'CONTENIDO: Estado de sincronizacion sujeto-bien. USO: seguimiento de integracion. REGLA: SujBieEstado enum.',
    fh_suj_bie_creacion     DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de creacion del sujeto-bien en Ingresos. USO: auditoria de integracion. REGLA: nullable.',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_ult_mod              DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima modificacion. USO: auditoria. REGLA: nullable; actualizar en cada mod.',
    id_user_ult_mod         CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario de la ultima mod. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_persona_tipo_doc_key (tipo_documento, doc_key),
    CONSTRAINT chk_fal_persona_tipo CHECK (tipo_persona IN (1, 2)),
    CONSTRAINT chk_fal_persona_nro_doc CHECK (nro_doc BETWEEN 1 AND 99999999),
    CONSTRAINT chk_fal_persona_prefijo CHECK (prefijo_cuit_cuil IS NULL OR prefijo_cuit_cuil BETWEEN 0 AND 99),
    CONSTRAINT chk_fal_persona_digito CHECK (digito_verificador IS NULL OR digito_verificador BETWEEN 0 AND 9),
    CONSTRAINT chk_fal_persona_id_suj CHECK (id_suj IS NULL OR id_suj BETWEEN 1 AND 255),
    CONSTRAINT chk_fal_persona_id_bie CHECK (id_bie IS NULL OR id_bie BETWEEN 1 AND 9999999),
    CONSTRAINT chk_fal_persona_id_bie_suj CHECK (id_bie IS NULL OR id_suj IS NOT NULL),
    INDEX ix_fal_persona_apellido (apellido),
    INDEX ix_fal_persona_razon_social (razon_social)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Maestro de personas fisicas y juridicas (infractores, presentantes). USO: identificacion en actas, notificaciones y domicilios. RAZON: modelo estructurado de documento evita duplicacion por formato. REGLAS CLAVE: doc_key garantiza unicidad; nro_doc es INT UNSIGNED; CUIT/CUIL require prefijo y digito.';

-- ----------------------------------------------------------------------------
-- 5. fal_vehiculo_marca
--    Catálogo de marcas de vehículos.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_vehiculo_marca (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    codigo          VARCHAR(12)    NOT NULL               COMMENT 'CONTENIDO: Codigo corto de la marca. USO: referencia en actas y fallback de texto. REGLA: unico; generado o asignado por operador.',
    nombre          VARCHAR(24)    NOT NULL               COMMENT 'CONTENIDO: Nombre de la marca. USO: visualizacion. REGLA: consistente con fal_acta_vehiculo.marca_txt.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de marcas vigentes. REGLA: FALSE = dada de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_veh_marca_codigo (codigo)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Catalogo de marcas de vehiculos reconocidas por el sistema. USO: dropdown en alta de actas de transito. RAZON: evitar texto libre no normalizado. REGLAS CLAVE: codigo unico; nombre coincide con fallback en fal_acta_vehiculo.';

-- ----------------------------------------------------------------------------
-- 6. num_politica
--    Política de numeración: define los parámetros para emitir talonarios.
-- ----------------------------------------------------------------------------
CREATE TABLE num_politica (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    codigo              VARCHAR(8)     NOT NULL               COMMENT 'CONTENIDO: Codigo unico de la politica. USO: referencia en talonarios. REGLA: unico; max 8 caracteres.',
    apodo               VARCHAR(20)    NOT NULL               COMMENT 'CONTENIDO: Nombre corto operativo de la politica. USO: seleccion rapida en UI y logs. REGLA: no puede ser vacio.',
    descripcion         VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Descripcion completa de la politica. USO: documentacion y UI de administracion. REGLA: no puede ser vacio.',
    clase_numeracion    SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Clase de la politica. USO: discrimina comportamiento. VALORES: 1=ACTA, 2=DOCUMENTO (ClaseNumeracion enum).',
    si_reinicio_anual   BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el contador se reinicia cada anio. USO: logica de generacion de numero. REGLA: FALSE = correlativo perpetuo.',
    si_incluye_prefijo  BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el numero incluye prefijo. USO: formato del numero visible. REGLA: si TRUE, prefijo no puede ser NULL.',
    prefijo             VARCHAR(10)     NULL                   COMMENT 'CONTENIDO: Prefijo del numero (ej: "A", "INF"). USO: formato visible. REGLA: NULL si si_incluye_prefijo=FALSE.',
    si_incluye_anio     BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el numero incluye anio. USO: formato visible. REGLA: si TRUE, formato_anio no puede ser NULL.',
    formato_anio        SMALLINT       NULL                   COMMENT 'CONTENIDO: Formato del anio en el numero. USO: formato visible. VALORES: 2=dos digitos, 4=cuatro digitos. REGLA: NULL si si_incluye_anio=FALSE.',
    si_incluye_serie    BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el numero incluye serie del talonario. USO: formato visible. REGLA: la serie viene del talonario.',
    longitud_nro        SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Longitud del componente numerico (con ceros a la izquierda). USO: formato visible. REGLA: positivo.',
    formato_visible     VARCHAR(60)    NOT NULL               COMMENT 'CONTENIDO: Plantilla del formato visible del numero. USO: documentacion del patron. REGLA: ej: "A-{AAAA}-{NNNNN}". LONGITUD: max 60 (HUMAN_DECISION_CLOSED CORRECCION-07).',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de politicas vigentes. REGLA: FALSE = dada de baja.',
    fh_vig_desde        DATE           NOT NULL               COMMENT 'CONTENIDO: Fecha de inicio de vigencia. USO: politicas con vigencia temporal. REGLA: no puede ser NULL.',
    fh_vig_hasta        DATE           NULL                   COMMENT 'CONTENIDO: Fecha de fin de vigencia. USO: politicas con caducidad. REGLA: NULL = sin vencimiento.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_ult_mod          DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima modificacion. USO: auditoria. REGLA: nullable.',
    id_user_ult_mod     CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario de la ultima mod. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_num_politica_codigo (codigo),
    CONSTRAINT chk_num_pol_clase CHECK (clase_numeracion IN (1, 2)),
    CONSTRAINT chk_num_pol_longitud CHECK (longitud_nro > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Politica de numeracion que parametriza la emision de talonarios. USO: define formato del numero visible de actas y documentos. RAZON: separar configuracion de numeracion del talonario fisico. REGLAS CLAVE: clase_numeracion discrimina acta/documento; si_reinicio_anual controla correlativos.';

-- ----------------------------------------------------------------------------
-- 7. fal_dia_no_computable
--    Días no computables para cálculo de plazos (feriados, asuetos).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_dia_no_computable (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    fecha               DATE           NOT NULL               COMMENT 'CONTENIDO: Fecha del dia no computable. USO: exclusion de calculos de plazos. REGLA: unica por tipo activo.',
    tipo                SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de dia no computable. USO: discrimina comportamiento. VALORES: 1=FERIADO, 2=ASUETO_ADMINISTRATIVO, 3=OTRO (TipoDiaNoComputable enum; DECISION_DDL-ENUM-01 CERRADA).',
    origen              SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Origen del registro. USO: auditoria de la fuente. VALORES: 1=MANUAL, 2=SINCRONIZACION_EXTERNA (OrigenDiaNoComputable enum).',
    descripcion         VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Descripcion del dia. USO: documentacion. REGLA: max 64 caracteres.',
    referencia_externa  VARCHAR(200)   NULL                   COMMENT 'CONTENIDO: Referencia del sistema externo (ej: ID en calendario nacional). USO: idempotencia de sincronizacion. REGLA: obligatoria para origen=SINCRONIZACION_EXTERNA; NULL para origen=MANUAL.',
    si_activo           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de dias vigentes. REGLA: FALSE = desactivado por operador.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_baja             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de baja logica. USO: auditoria. REGLA: NULL = activo; NOT NULL = desactivado.',
    id_user_baja        CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que desactivo. USO: auditoria. REGLA: NULL = activo.',
    fecha_activa        DATE           AS (CASE WHEN si_activo = TRUE THEN fecha ELSE NULL END) STORED COMMENT 'CONTENIDO: Columna generada para UNIQUE condicional (solo fecha activa). USO: evitar duplicados de fecha activa por tipo. REGLA: NULL cuando si_activo=FALSE.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_dia_nc_fecha_activa (fecha_activa),
    CONSTRAINT chk_dia_nc_tipo   CHECK (tipo   IN (1, 2, 3)),
    CONSTRAINT chk_dia_nc_origen CHECK (origen IN (1, 2)),
    CONSTRAINT chk_dia_nc_ref_ext CHECK (
        (origen = 1 AND referencia_externa IS NULL) OR
        (origen = 2 AND referencia_externa IS NOT NULL)
    ),
    INDEX ix_fal_dia_nc_fecha (fecha)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Dias no computables para el calculo de plazos administrativos. USO: excluidos de vencimientos y plazos procesales. RAZON: separar el calendario de excepcion del calculo de plazos. REGLAS CLAVE: tipo y origen son enums SMALLINT; referencia_externa obligatoria para sincronizacion externa.';

-- ----------------------------------------------------------------------------
-- 8. fal_motivo_archivo
--    Catálogo de motivos de archivo de actas.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_motivo_archivo (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    cod_motivo_archivo      VARCHAR(8)     NOT NULL               COMMENT 'CONTENIDO: Codigo corto del motivo. USO: referencia en actas archivadas. REGLA: unico; max 8 caracteres.',
    nombre                  VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Nombre descriptivo del motivo. USO: visualizacion. REGLA: no puede ser vacio.',
    si_nulidad              BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el archivo equivale a nulidad. USO: impacto juridico del archivo. REGLA: TRUE = archivo implica nulidad.',
    si_permite_reingreso    BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el acta puede reingresarse tras este archivo. USO: control de reingreso. REGLA: TRUE = permite reingreso posterior.',
    si_requiere_observacion BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Indica si el archivo requiere observacion en fal_observacion. USO: control de calidad del archivo. REGLA: TRUE = observacion obligatoria al archivar.',
    si_activo               BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de motivos vigentes. REGLA: FALSE = dado de baja.',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_ult_mod              DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima modificacion. USO: auditoria. REGLA: nullable.',
    id_user_ult_mod         CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario de la ultima mod. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_motivo_arch_cod (cod_motivo_archivo)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Catalogo de motivos validos para archivar un acta de faltas. USO: parametro del proceso de archivo. RAZON: tipar el motivo para control y estadisticas. REGLAS CLAVE: cod_motivo_archivo unico; si_requiere_observacion obliga a registrar en fal_observacion.';

-- ----------------------------------------------------------------------------
-- 9. fal_normativa_faltas
--    Normativas vigentes del sistema de faltas (ordenanzas, leyes).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_normativa_faltas (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    codigo_norma        VARCHAR(8)     NOT NULL               COMMENT 'CONTENIDO: Codigo unico de la normativa. USO: referencia en articulos. REGLA: unico; max 8 caracteres.',
    nombre_norma        VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Nombre oficial de la normativa. USO: visualizacion. REGLA: no puede ser vacio.',
    tipo_norma          SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de norma juridica. USO: clasificacion. VALORES: segun TipoNorma enum (ej. ORDENANZA, LEY, DECRETO).',
    numero_norma        INT            NULL                   COMMENT 'CONTENIDO: Numero oficial de la norma. USO: referencia juridica. REGLA: nullable si sin numero asignado.',
    anio_norma          SMALLINT       NULL                   COMMENT 'CONTENIDO: Anio de la norma. USO: referencia juridica. REGLA: nullable si desconocido.',
    fecha_promulgacion  DATE           NULL                   COMMENT 'CONTENIDO: Fecha de promulgacion de la norma. USO: referencia juridica. REGLA: nullable.',
    fecha_publicacion   DATE           NULL                   COMMENT 'CONTENIDO: Fecha de publicacion oficial de la norma. USO: entrada en vigencia formal. REGLA: nullable.',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de normativas vigentes. REGLA: FALSE = dada de baja.',
    fh_vig_desde        DATE           NULL                   COMMENT 'CONTENIDO: Fecha de inicio de vigencia. USO: control temporal de aplicabilidad. REGLA: nullable.',
    fh_vig_hasta        DATE           NULL                   COMMENT 'CONTENIDO: Fecha de fin de vigencia. USO: normativas derogadas. REGLA: NULL = sin vencimiento.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_norm_codigo (codigo_norma)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Catalogo de normativas del sistema de faltas (ordenanzas, leyes, reglamentos). USO: catalogo de articulos infringidos. RAZON: tipar la fuente normativa de cada articulo. REGLAS CLAVE: codigo_norma unico; articulos en fal_articulo_normativa_faltas.';

-- ----------------------------------------------------------------------------
-- 10. fal_medida_preventiva
--     Catálogo de medidas preventivas aplicables en el marco de faltas.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_medida_preventiva (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la medida preventiva',
    codigo                  VARCHAR(12)    NOT NULL               COMMENT 'Código único de la medida preventiva',
    version_medida          SMALLINT       NOT NULL               COMMENT 'Versión vigente de la medida',
    descripcion             VARCHAR(160)   NOT NULL               COMMENT 'Descripción corta de la medida preventiva',
    descripcion_detalle     VARCHAR(255)   NULL                   COMMENT 'Descripción detallada adicional',
    id_dep                  BIGINT         NULL                   COMMENT 'Dependencia responsable de la medida (nullable = global)',
    ver_dep                 SMALLINT       NULL                   COMMENT 'Versión de la dependencia responsable',
    si_activa               BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = medida vigente para aplicar',
    si_puede_bloquear_cierre BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'TRUE = esta medida puede generar un bloqueante de cierre',
    tipo_bloqueante_default SMALLINT       NULL                   COMMENT 'Tipo de bloqueante por defecto al aplicar esta medida',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el registro',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_med_prev_codigo (codigo)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Catálogo de medidas preventivas habilitadas en el sistema de faltas';

-- ----------------------------------------------------------------------------
-- 11. fal_lote_correo
--     Lotes de correo para notificaciones en batch.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_lote_correo (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico del lote. USO: PK. REGLA: generado por DB.',
    lote_codigo         CHAR(20)       NOT NULL               COMMENT 'CONTENIDO: Codigo unico de idempotencia del lote. USO: referencia humana e idempotencia. REGLA: generado por backend con formato LC-YYYYMMDD-NNNNNNNN (ej: LC-20260720-00000001); inmutable; unico.',
    tipo_lote           SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de lote. USO: discrimina modalidad de envio. VALORES: segun enum LoteTipo.',
    estado_lote         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado del lote. USO: control del ciclo de vida del lote. VALORES: 1=GENERADO, 2=EMITIDO, 3=PROCESADO, 4=ANULADO, 5=CON_ERROR.',
    referencia_externa  VARCHAR(60)    NULL                   COMMENT 'CONTENIDO: Referencia externa del sistema de correo. USO: idempotencia con proveedor externo. REGLA: nullable.',
    guid_lote_ext       CHAR(36)       NULL                   COMMENT 'CONTENIDO: GUID del lote en el sistema externo. USO: reconciliacion. REGLA: nullable.',
    fecha_generacion    DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp de generacion del lote. USO: auditoria. REGLA: suministrado por FaltasClock.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_lote_codigo (lote_codigo),
    CONSTRAINT chk_fal_lote_codigo CHECK (lote_codigo REGEXP '^LC-[0-9]{8}-[0-9]{8}$'),
    CONSTRAINT chk_fal_lote_estado CHECK (estado_lote BETWEEN 1 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Lotes de notificaciones en batch por correo postal. USO: agrupacion y seguimiento de envios masivos. RAZON: idempotencia y trazabilidad de envios en lote. REGLAS CLAVE: lote_codigo formato LC-YYYYMMDD-NNNNNNNN; inmutable; unico.';

-- ----------------------------------------------------------------------------
-- 12. fal_documento_plantilla
--     Plantillas documentales del sistema (modelos de resoluciones, actas, etc.).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_plantilla (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    codigo                  VARCHAR(12)    NOT NULL               COMMENT 'CONTENIDO: Codigo unico de la plantilla. USO: referencia en configuracion. REGLA: unico; max 12 caracteres.',
    nombre                  VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Nombre visible de la plantilla. USO: UI y documentacion. REGLA: debe comunicar uso e intencion; no puede ser vacio.',
    tipo_docu               SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de documento generado. USO: discrimina plantillas por tipo. REGLA: TipoDocu enum.',
    accion_documental       SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Accion documental que origina este documento. USO: vinculo con el flujo procesal. REGLA: AccionDocumental enum.',
    tipo_acta               SMALLINT       NULL                   COMMENT 'CONTENIDO: Tipo de acta al que aplica. USO: filtrado por tipo de acta. REGLA: NULL = aplica a cualquier tipo (TipoActa enum).',
    tipo_firma_req          SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de firma requerida. USO: control de flujo de firma. REGLA: TipoFirmaReq enum.',
    si_requiere_numeracion  BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Requiere numero de talonario. USO: control de asignacion de numero. REGLA: FALSE = no numerado.',
    momento_numeracion_docu SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Momento de asignacion de numero. USO: logica de numeracion. VALORES: 0=NO_APLICA, 1=AL_CREAR, 2=AL_EMITIR, 3=AL_ENVIAR_A_FIRMA, 4=AL_FIRMAR (MomentoNumeracionDocu). HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    si_notificable          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Puede ser notificado. USO: habilita proceso de notificacion. REGLA: FALSE = no notificable.',
    si_genera_pdf           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Genera PDF al emitir. USO: control de generacion de artefacto. REGLA: TRUE = genera PDF.',
    si_seleccionable        BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Visible para seleccion manual. USO: UI. REGLA: TRUE = operador puede seleccionarla.',
    si_activa               BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de plantillas vigentes. REGLA: FALSE = dada de baja.',
    fh_vig_desde            DATE           NOT NULL               COMMENT 'CONTENIDO: Inicio de vigencia. USO: control temporal. REGLA: obligatoria.',
    fh_vig_hasta            DATE           NULL                   COMMENT 'CONTENIDO: Fin de vigencia. USO: plantillas con caducidad. REGLA: NULL = sin vencimiento.',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_docu_plantilla_codigo (codigo)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Plantillas documentales del sistema (modelos de resoluciones, actas, etc.). USO: parametro del proceso de generacion documental. RAZON: desacoplar la configuracion del documento de su contenido. REGLAS CLAVE: codigo unico; contenido versionado en fal_documento_plantilla_contenido.';

-- Nota: fal_rubro_version (#13) es PREEXISTING_CANONICAL_ADOPTED — no se crea aquí.

-- =============================================================================
-- SECCIÓN 02 — G2: Versiones y catálogos secundarios (tablas 14–23)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 14. fal_dependencia_version
--     Versión histórica de una dependencia con datos versionados.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_dependencia_version (
    id_dep          BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK a la dependencia maestra. USO: parte de PK compuesta. REGLA: referencia fal_dependencia(id).',
    ver_dep         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Numero de version (inicia en 1). USO: parte de PK compuesta. REGLA: positivo; incrementa en cada cambio.',
    nom_dep         VARCHAR(48)    NULL                   COMMENT 'CONTENIDO: Nombre de la dependencia en esta version. USO: snapshot historico. REGLA: nullable (SPEC-MODEL-DDL-CLOSURE-001).',
    id_dep_padre    BIGINT         NULL                   COMMENT 'CONTENIDO: FK a la dependencia padre vigente en esta version. USO: jerarquia historica. REGLA: NULL = raiz; ambos NULL o ambos NOT NULL con ver_dep_padre.',
    ver_dep_padre   SMALLINT       NULL                   COMMENT 'CONTENIDO: Version de la dependencia padre. USO: FK historica compuesta. REGLA: NULL si id_dep_padre NULL; NOT NULL si id_dep_padre NOT NULL.',
    tipo_acta       SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de acta habilitado en esta version. USO: discrimina por tipo de acta. REGLA: TipoActa enum; obligatorio.',
    fh_vig_desde    DATE           NOT NULL               COMMENT 'CONTENIDO: Inicio de vigencia. USO: consulta temporal. REGLA: obligatoria.',
    fh_vig_hasta    DATE           NULL                   COMMENT 'CONTENIDO: Fin de vigencia. USO: version activa = NULL. REGLA: NULL = version activa.',
    si_activa       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de versiones vigentes. REGLA: FALSE = version dada de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id_dep, ver_dep),
    CONSTRAINT fk_dep_ver_dependencia FOREIGN KEY (id_dep)
        REFERENCES fal_dependencia (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dep_ver_padre_hist FOREIGN KEY (id_dep_padre, ver_dep_padre)
        REFERENCES fal_dependencia_version (id_dep, ver_dep) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_dep_ver_vig (fh_vig_desde, fh_vig_hasta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Versiones historicas de dependencias. USO: congela nombre, padre y tipo de acta en cada cambio. RAZON: trazabilidad de la estructura organizacional en el tiempo. REGLAS CLAVE: PK (id_dep, ver_dep); FK historica compuesta para padre.';

-- ----------------------------------------------------------------------------
-- 15. fal_inspector_version
--     Versión histórica del inspector con datos del período activo.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_inspector_version (
    id_insp         BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al inspector maestro. USO: parte de PK compuesta. REGLA: referencia fal_inspector(id).',
    ver_insp        SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Numero de version del inspector (inicia en 1). USO: parte de PK compuesta. REGLA: positivo.',
    legajo_insp     INT            NOT NULL               COMMENT 'CONTENIDO: Legajo del inspector en esta version. USO: snapshot historico. REGLA: positivo.',
    nom_insp        VARCHAR(36)    NOT NULL               COMMENT 'CONTENIDO: Nombre del inspector en esta version. USO: snapshot historico. REGLA: no puede ser vacio.',
    id_dep          BIGINT         NULL                   COMMENT 'CONTENIDO: FK a la dependencia de adscripcion. USO: jerarquia historica del inspector. REGLA: nullable si no hay adscripcion.',
    ver_dep         SMALLINT       NULL                   COMMENT 'CONTENIDO: Version de la dependencia de adscripcion. USO: FK historica compuesta. REGLA: NULL si id_dep NULL.',
    fh_vig_desde    DATE           NOT NULL               COMMENT 'CONTENIDO: Inicio de vigencia. USO: consulta temporal. REGLA: obligatoria.',
    fh_vig_hasta    DATE           NULL                   COMMENT 'CONTENIDO: Fin de vigencia. USO: version activa = NULL. REGLA: NULL = version activa.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de versiones vigentes. REGLA: FALSE = version dada de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id_insp, ver_insp),
    CONSTRAINT fk_insp_ver_inspector FOREIGN KEY (id_insp)
        REFERENCES fal_inspector (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_insp_ver_dep FOREIGN KEY (id_dep, ver_dep)
        REFERENCES fal_dependencia_version (id_dep, ver_dep) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_insp_ver_vig (fh_vig_desde, fh_vig_hasta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Versiones historicas de inspectores. USO: congela legajo, nombre y dependencia de adscripcion en cada cambio. RAZON: trazabilidad del inspector en el tiempo. REGLAS CLAVE: PK (id_insp, ver_insp); FK compuesta a dependencia_version.';

-- ----------------------------------------------------------------------------
-- 16. fal_firmante_version
--     Versión histórica del firmante con cargo, rol y dependencia.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_firmante_version (
    id_firmante     BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al firmante maestro. USO: parte de PK compuesta. REGLA: referencia fal_firmante(id).',
    ver_firmante    SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Numero de version del firmante (inicia en 1). USO: parte de PK compuesta. REGLA: positivo.',
    id_user         CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID IDP del firmante al crear esta version. USO: snapshot historico de identidad. REGLA: no puede ser vacio.',
    nom_firmante    VARCHAR(48)    NOT NULL               COMMENT 'CONTENIDO: Nombre visible del firmante en esta version. USO: snapshot historico. REGLA: no puede ser vacio.',
    rol_firmante    VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Rol institucional descriptivo del firmante. USO: documentacion. REGLA: no define autorizacion documental; nullable (SPEC-MODEL-DDL-CLOSURE-001: VARCHAR(64)).',
    cargo_firmante  VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Cargo institucional del firmante. USO: aparece en documentos firmados. REGLA: nullable.',
    id_dep          BIGINT         NULL                   COMMENT 'CONTENIDO: FK a la dependencia del firmante. USO: contexto organizacional. REGLA: nullable.',
    ver_dep         SMALLINT       NULL                   COMMENT 'CONTENIDO: Version de la dependencia. USO: FK historica compuesta. REGLA: obligatoria si id_dep no es NULL.',
    fh_vig_desde    DATE           NOT NULL               COMMENT 'CONTENIDO: Inicio de vigencia. USO: consulta temporal. REGLA: obligatoria.',
    fh_vig_hasta    DATE           NULL                   COMMENT 'CONTENIDO: Fin de vigencia. USO: version activa = NULL. REGLA: NULL = version activa.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de versiones vigentes. REGLA: FALSE = version dada de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id_firmante, ver_firmante),
    CONSTRAINT fk_firm_ver_firmante FOREIGN KEY (id_firmante)
        REFERENCES fal_firmante (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_firm_ver_dep FOREIGN KEY (id_dep, ver_dep)
        REFERENCES fal_dependencia_version (id_dep, ver_dep) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_firm_ver_vig (fh_vig_desde, fh_vig_hasta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Versiones históricas de firmantes: congela nombre, cargo, rol y dependencia en cada cambio. La autorización documental concreta vive en fal_firmante_version_habilitacion.';

-- ----------------------------------------------------------------------------
-- 17. fal_vehiculo_modelo
--     Catálogo de modelos de vehículos por marca.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_vehiculo_modelo (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del modelo',
    marca_vehiculo_id   BIGINT         NOT NULL               COMMENT 'FK a la marca del vehículo',
    codigo              VARCHAR(12)    NOT NULL               COMMENT 'CONTENIDO: Codigo unico del modelo dentro de la marca. USO: referencia en actas y catálogos. REGLA: UNIQUE(marca, codigo); max 12 caracteres (SPEC-MODEL-DDL-CLOSURE-001).',
    nombre              VARCHAR(24)    NOT NULL               COMMENT 'CONTENIDO: Nombre visible del modelo. USO: display; coincide con fallback modelo_vehiculo_txt en fal_acta_vehiculo. REGLA: max 24 caracteres (SPEC-MODEL-DDL-CLOSURE-001).',
    si_activo           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = modelo vigente en catálogo',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el registro',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_veh_modelo_marca_codigo (marca_vehiculo_id, codigo),
    CONSTRAINT fk_veh_modelo_marca FOREIGN KEY (marca_vehiculo_id)
        REFERENCES fal_vehiculo_marca (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Catálogo de modelos de vehículos agrupados por marca. UNIQUE(marca_vehiculo_id, codigo). SPEC-MODEL-DDL-CLOSURE-001.';

-- ----------------------------------------------------------------------------
-- 18. num_talonario
--     Talonarios de numeración (electrónico o manual físico).
-- ----------------------------------------------------------------------------
CREATE TABLE num_talonario (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    version_row      INT            NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Version para OCC. USO: control de concurrencia optimista. REGLA: incrementar en cada UPDATE.',
    politica_id      BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK a la politica de numeracion. USO: enlaza configuracion de numeracion. REGLA: referencia num_politica(id).',
    codigo           VARCHAR(8)     NOT NULL               COMMENT 'CONTENIDO: Codigo unico del talonario. USO: identificacion operativa. REGLA: UNIQUE; max 8 chars.',
    descripcion      VARCHAR(48)    NOT NULL               COMMENT 'CONTENIDO: Descripcion del talonario. USO: legibilidad operativa. REGLA: NOT NULL.',
    tipo_talonario   SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de talonario. VALORES: 1=ELECTRONICO, 2=MANUAL_FISICO (TipoTalonario enum).',
    clase_talonario  SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Clase de numeracion. VALORES: ClaseNumeracion enum.',
    anio             SMALLINT       NULL                   COMMENT 'CONTENIDO: Año del talonario. REGLA: obligatorio si politica reinicia anual; nullable si sin año fijo.',
    serie            VARCHAR(12)    NULL                   COMMENT 'CONTENIDO: Serie alfanumerica del talonario. REGLA: nullable.',
    nro_desde        INT            NOT NULL               COMMENT 'CONTENIDO: Numero inicial del rango. REGLA: > 0.',
    nro_hasta        INT            NULL                   COMMENT 'CONTENIDO: Numero final del rango. REGLA: NULL si sin limite operativo; si presente, >= nro_desde.',
    nombre_secuencia VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Nombre de la SEQUENCE MariaDB asociada. USO: talonario electronico. REGLA: UNIQUE; uno por talonario electronico; NOCACHE recomendado.',
    si_activo        BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: TRUE si el talonario esta habilitado. USO: filtro operativo. REGLA: NOT NULL.',
    si_bloqueado     BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si bloqueado temporalmente. USO: suspension de uso. REGLA: NOT NULL.',
    cod_desbloqueo   VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Codigo de desbloqueo. USO: autorizar reanudacion. REGLA: nullable.',
    fh_alta          DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta     CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario que creo el registro. USO: auditoria. REGLA: UUID.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_num_talonario_codigo (codigo),
    UNIQUE KEY uq_num_talonario_secuencia (nombre_secuencia),
    CONSTRAINT fk_num_talonario_politica FOREIGN KEY (politica_id)
        REFERENCES num_politica (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_num_talonario_nro CHECK (nro_desde > 0 AND (nro_hasta IS NULL OR nro_hasta >= nro_desde))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Talonarios de numeración de actas: electrónicos (con SEQUENCE) o manuales físicos';

-- ----------------------------------------------------------------------------
-- 19. fal_dependencia_normativa
--     Asociación de una versión de dependencia con una normativa.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_dependencia_normativa (
    id_dep          BIGINT         NOT NULL               COMMENT 'FK a la dependencia (parte de la PK compuesta)',
    ver_dep         SMALLINT       NOT NULL               COMMENT 'Versión de la dependencia (parte de la PK compuesta)',
    id_normativa    BIGINT         NOT NULL               COMMENT 'FK a la normativa de faltas (parte de la PK compuesta)',
    si_activa       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = asociación vigente',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la asociación',
    PRIMARY KEY (id_dep, ver_dep, id_normativa),
    CONSTRAINT fk_dep_norm_dep_ver FOREIGN KEY (id_dep, ver_dep)
        REFERENCES fal_dependencia_version (id_dep, ver_dep) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dep_norm_normativa FOREIGN KEY (id_normativa)
        REFERENCES fal_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Asocia una versión específica de dependencia a una normativa de faltas habilitada';

-- ----------------------------------------------------------------------------
-- 20. fal_articulo_normativa_faltas
--     Artículos infractores definidos dentro de una normativa.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_articulo_normativa_faltas (
    id                              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_normativa                    BIGINT          NOT NULL               COMMENT 'CONTENIDO: FK a la normativa que contiene el articulo. USO: agrupacion normativa. REGLA: referencia fal_normativa_faltas(id).',
    codigo_articulo                 VARCHAR(16)     NOT NULL               COMMENT 'CONTENIDO: Codigo del articulo dentro de la normativa. USO: referencia en actas. REGLA: UNIQUE(id_normativa, codigo_articulo); max 16 caracteres (SPEC-MODEL-DDL-CLOSURE-001).',
    nombre_articulo                 VARCHAR(64)     NOT NULL               COMMENT 'CONTENIDO: Nombre o titulo del articulo infractor. USO: visualizacion. REGLA: max 64 caracteres (SPEC-MODEL-DDL-CLOSURE-001).',
    descripcion_articulo            VARCHAR(128)    NULL                   COMMENT 'CONTENIDO: Descripcion corta del articulo. USO: contexto adicional. REGLA: max 128 caracteres; no usar TEXT (SPEC-MODEL-DDL-CLOSURE-001).',
    tipo_infraccion                 SMALLINT        NOT NULL               COMMENT 'CONTENIDO: Tipo de infraccion. USO: clasificacion de la conducta infractora. VALORES: segun TipoInfraccion enum.',
    categoria_infraccion            SMALLINT        NULL                   COMMENT 'CONTENIDO: Categoria de la infraccion. USO: sub-clasificacion. VALORES: segun CategoriaInfraccion enum; nullable.',
    si_admite_medida_preventiva     BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Admite medida preventiva. USO: habilita la aplicacion de medidas preventivas. REGLA: FALSE = no aplica.',
    si_activo                       BOOLEAN         NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de articulos vigentes. REGLA: FALSE = dado de baja.',
    fh_alta                         DATETIME(6)     NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta                    CHAR(36)        NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_art_norm_cod (id_normativa, codigo_articulo),
    CONSTRAINT fk_art_norm_normativa FOREIGN KEY (id_normativa)
        REFERENCES fal_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_art_norm_normativa (id_normativa)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Articulos de normativas de faltas que definen la conducta infractora. USO: referencia en imputaciones. RAZON: centralizar catalogo de infracciones por normativa. REGLAS CLAVE: UNIQUE(id_normativa, codigo_articulo); codigo_articulo VARCHAR(16); SPEC-MODEL-DDL-CLOSURE-001.';

-- ----------------------------------------------------------------------------
-- 21. fal_documento_plantilla_firma_req
--     Requisitos de firma definidos en la plantilla documental.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_plantilla_firma_req (
    id_plantilla        BIGINT         NOT NULL               COMMENT 'FK a la plantilla documental (parte de la PK)',
    seq_firma_req       SMALLINT       NOT NULL               COMMENT 'Secuencia ordinal del requisito de firma (parte de la PK)',
    rol_firma_req       SMALLINT       NOT NULL               COMMENT 'Rol de firma requerido en este requisito (código SMALLINT)',
    tipo_firma_req      SMALLINT       NOT NULL               COMMENT 'Tipo de firma requerida (TipoFirmaReq enum)',
    mecanismo_firma_req SMALLINT       NULL                   COMMENT 'Mecanismo de firma requerido (NULL = sin restricción)',
    orden_firma         SMALLINT       NULL                   COMMENT 'Orden de firma si hay secuencia obligatoria',
    si_obligatoria      BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = este requisito es obligatorio para emitir el documento',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el requisito',
    PRIMARY KEY (id_plantilla, seq_firma_req),
    CONSTRAINT fk_plantilla_firma_req_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES fal_documento_plantilla (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Requisitos de firma configurados en cada plantilla documental';

-- ----------------------------------------------------------------------------
-- 22. fal_documento_plantilla_contenido
--     Versiones de contenido Markdown de una plantilla documental.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_plantilla_contenido (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del contenido',
    id_plantilla                BIGINT         NOT NULL               COMMENT 'FK a la plantilla que contiene este contenido',
    version_contenido           SMALLINT       NOT NULL               COMMENT 'Número de versión del contenido (único por plantilla)',
    titulo                      VARCHAR(64)    NOT NULL               COMMENT 'Título operativo de esta versión de contenido; máximo 64 caracteres',
    cuerpo_markdown             TEXT           NOT NULL               COMMENT 'Cuerpo principal del documento en Markdown con variables',
    encabezado_markdown         TEXT           NULL                   COMMENT 'Encabezado del documento en Markdown (nullable)',
    pie_markdown                TEXT           NULL                   COMMENT 'Pie del documento en Markdown (nullable)',
    variables_declaradas_json   JSON           NOT NULL               COMMENT 'Metadatos declarativos tipados, requeridos y etiquetados; arreglo JSON, [] sin variables; cada item exige namespace, campo, tipoDato, requerida y etiqueta',
    si_activo                   BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = versión de contenido vigente',
    fh_vig_desde                DATETIME(6)    NOT NULL               COMMENT 'Inicio de vigencia de esta versión de contenido',
    fh_vig_hasta                DATETIME(6)    NULL                   COMMENT 'Fin de vigencia (NULL = versión activa)',
    fh_alta                     DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta                CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la versión',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_plantilla_cont_ver (id_plantilla, version_contenido),
    CONSTRAINT fk_plantilla_cont_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES fal_documento_plantilla (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Versiones de contenido Markdown de las plantillas documentales; referenciado por redacciones concretas';

-- ----------------------------------------------------------------------------
-- 23. fal_documento_plantilla_default
--     Configuración de plantilla por defecto según acción y dependencia.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_plantilla_default (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del default',
    accion_documental   SMALLINT       NOT NULL               COMMENT 'Acción documental que dispara la búsqueda del default (AccionDocumental)',
    tipo_acta           SMALLINT       NULL                   COMMENT 'Tipo de acta al que aplica (NULL = genérico)',
    tipo_docu           SMALLINT       NOT NULL               COMMENT 'Tipo de documento generado por este default',
    id_dependencia      BIGINT         NULL                   COMMENT 'FK a dependencia específica (NULL = aplica a todas)',
    ver_dependencia     SMALLINT       NULL                   COMMENT 'Versión de la dependencia específica',
    id_plantilla        BIGINT         NOT NULL               COMMENT 'FK a la plantilla seleccionada como default',
    prioridad           SMALLINT       NOT NULL               COMMENT 'Prioridad de resolución (menor = más específico, mayor prioridad)',
    fh_vig_desde        DATETIME(6)    NOT NULL               COMMENT 'Inicio de vigencia de esta configuración',
    fh_vig_hasta        DATETIME(6)    NULL                   COMMENT 'Fin de vigencia (NULL = configuración activa)',
    si_activo           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = configuración vigente',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la configuración',
    PRIMARY KEY (id),
    CONSTRAINT fk_plantilla_default_dep FOREIGN KEY (id_dependencia)
        REFERENCES fal_dependencia (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_plantilla_default_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES fal_documento_plantilla (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_plantilla_default_query (accion_documental, tipo_acta, id_dependencia)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Configura la plantilla por defecto según acción documental, tipo de acta y dependencia (con prioridad)';

-- =============================================================================
-- SECCIÓN 03 — G3: Habilitaciones y catálogos terciarios (tablas 24–28)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 24. fal_firmante_version_habilitacion
--     Autorizaciones concretas de una versión de firmante para firmar documentos.
--     PK compuesta (DECISION_DDL-ENUM-01 corregida: sin id surrogate).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_firmante_version_habilitacion (
    id_firmante         BIGINT         NOT NULL               COMMENT 'FK al firmante (parte de PK y FK compuesta a versión)',
    ver_firmante        SMALLINT       NOT NULL               COMMENT 'Versión del firmante (parte de PK y FK compuesta)',
    tipo_docu           SMALLINT       NOT NULL               COMMENT 'Tipo de documento para el que está habilitado (parte de PK)',
    rol_firma_req       SMALLINT       NOT NULL               COMMENT 'Rol de firma que puede satisfacer (parte de PK)',
    mecanismo_firma_req SMALLINT       NULL                   COMMENT 'Restricción de mecanismo (NULL = sin restricción)',
    si_activo           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = habilitación vigente',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la habilitación',
    PRIMARY KEY (id_firmante, ver_firmante, tipo_docu, rol_firma_req),
    CONSTRAINT fk_firm_hab_firmante_ver FOREIGN KEY (id_firmante, ver_firmante)
        REFERENCES fal_firmante_version (id_firmante, ver_firmante) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Autorizaciones de firma por versión de firmante: qué tipos de documento y qué roles puede satisfacer. Sin PK surrogate (DECISION_DDL-ENUM-01 corregida R3).';

-- ----------------------------------------------------------------------------
-- 25. fal_tarifario_unidad_faltas
--     Tarifas unitarias vigentes para el cálculo de valorización de artículos.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_tarifario_unidad_faltas (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la tarifa',
    id_articulo     BIGINT          NOT NULL               COMMENT 'FK al artículo normativo tarifado',
    fh_vig_desde    DATE            NOT NULL               COMMENT 'Inicio de vigencia de esta tarifa',
    fh_vig_hasta    DATE            NULL                   COMMENT 'Fin de vigencia (NULL = tarifa activa)',
    valor_unitario  DECIMAL(14,4)   NOT NULL               COMMENT 'Valor monetario por unidad de la infracción',
    fh_alta         DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta    CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que creó la tarifa',
    PRIMARY KEY (id),
    CONSTRAINT fk_tarifario_articulo FOREIGN KEY (id_articulo)
        REFERENCES fal_articulo_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_tarifario_valor CHECK (valor_unitario >= 0),
    INDEX ix_tarifario_articulo_vig (id_articulo, fh_vig_desde, fh_vig_hasta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Tarifario de valores unitarios por artículo normativo para el cálculo de valorización de actas';

-- ----------------------------------------------------------------------------
-- 26. fal_articulo_medida_preventiva
--     Relación entre artículos infractores y medidas preventivas aplicables.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_articulo_medida_preventiva (
    id_articulo         BIGINT         NOT NULL               COMMENT 'FK al artículo normativo (parte de PK)',
    id_medida           BIGINT         NOT NULL               COMMENT 'FK a la medida preventiva (parte de PK)',
    si_obligatoria      BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = medida obligatoria para el artículo; FALSE = opcional',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = relación vigente; FALSE = baja lógica (sin reactivación silenciosa)',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la relación',
    PRIMARY KEY (id_articulo, id_medida),
    CONSTRAINT fk_art_med_articulo FOREIGN KEY (id_articulo)
        REFERENCES fal_articulo_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_art_med_medida FOREIGN KEY (id_medida)
        REFERENCES fal_medida_preventiva (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Relación entre artículos normativos y medidas preventivas aplicables (catálogo de habilitaciones)';

-- ----------------------------------------------------------------------------
-- 27. num_talonario_ambito
--     Define el ámbito de uso de un talonario (por dependencia o global).
-- ----------------------------------------------------------------------------
CREATE TABLE num_talonario_ambito (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    talonario_id    BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al talonario al que aplica este ambito. USO: enlace al talonario. REGLA: referencia num_talonario(id).',
    clase_talonario SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Clase de numeracion del talonario. USO: discrimina tipo de uso (acta o documento). VALORES: segun ClaseNumeracion enum (ACTA=1, DOCUMENTO=2).',
    tipo_docu       SMALLINT       NULL                   COMMENT 'CONTENIDO: Tipo de documento al que aplica. USO: restriccion por tipo documental. REGLA: obligatorio si clase_talonario=DOCUMENTO; nullable si ACTA.',
    tipo_acta       SMALLINT       NULL                   COMMENT 'CONTENIDO: Tipo de acta al que aplica. USO: restriccion por tipo de acta. REGLA: nullable; NULL = cualquier tipo de acta.',
    id_dep          BIGINT         NULL                   COMMENT 'CONTENIDO: FK a la dependencia habilitada. USO: restriccion por dependencia. REGLA: NULL = todas las dependencias.',
    ver_dep         SMALLINT       NULL                   COMMENT 'CONTENIDO: Version de la dependencia. USO: FK historica compuesta. REGLA: obligatorio si id_dep NOT NULL.',
    alcance         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Alcance del ambito. USO: determina el tipo de restriccion. VALORES: 1=GLOBAL, 2=DEPENDENCIA, 3=TRANSVERSAL_DOCUMENTO (AlcanceTalonario enum).',
    prioridad       SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Prioridad de aplicacion del ambito. USO: desempate entre ambitos superpuestos. REGLA: menor valor = mayor prioridad.',
    fh_desde        DATE           NOT NULL               COMMENT 'CONTENIDO: Inicio de vigencia del ambito. USO: control temporal. REGLA: obligatoria.',
    fh_hasta        DATE           NULL                   COMMENT 'CONTENIDO: Fin de vigencia del ambito. USO: ambito con caducidad. REGLA: NULL = sin vencimiento.',
    si_activo       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: filtrado de ambitos vigentes. REGLA: FALSE = dado de baja.',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    CONSTRAINT fk_tal_ambito_talonario FOREIGN KEY (talonario_id)
        REFERENCES num_talonario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tal_ambito_dep FOREIGN KEY (id_dep)
        REFERENCES fal_dependencia (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_tal_ambito_talonario (talonario_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Regla de ambito de uso de un talonario. USO: autoriza donde aplica (global, por dependencia o por tipo documental). REGLAS CLAVE: clase_talonario, prioridad y fh_desde obligatorios; ver_dep obligatorio si id_dep presente.';

-- ----------------------------------------------------------------------------
-- 28. num_talonario_inspector
--     Asignación de rangos de numeración a inspectores específicos.
-- ----------------------------------------------------------------------------
CREATE TABLE num_talonario_inspector (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_talonario        BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al talonario manual asignado. USO: enlace al talonario. REGLA: referencia num_talonario(id); solo MANUAL_FISICO.',
    id_insp             BIGINT         NOT NULL               COMMENT 'CONTENIDO: ID del inspector al que se asigna el talonario. USO: trazabilidad. REGLA: referencia fal_inspector(id).',
    ver_insp            SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Version del inspector al momento de la entrega. USO: FK historica. REGLA: referencia fal_inspector_version(id_insp, ver_insp).',
    fh_entrega          DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp de entrega del talonario al inspector. USO: trazabilidad. REGLA: obligatoria.',
    id_user_entrega     CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario que realizo la entrega. USO: auditoria. REGLA: UUID del actor.',
    fh_devolucion       DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de devolucion del talonario. USO: cierre de la asignacion. REGLA: NULL = asignacion activa.',
    id_user_devolucion  CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que registro la devolucion. USO: auditoria. REGLA: nullable si sin devolucion.',
    estado_asignacion   SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado de la asignacion. USO: ciclo de vida. VALORES: 1=ENTREGADO, 2=DEVUELTO, 3=CERRADO, 4=OBSERVADO (EstadoAsignacionTalonario enum).',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Asignacion vigente. USO: unicidad de asignacion activa por talonario. REGLA: TRUE = activa; solo una activa por talonario (via columna generada).',
    -- columna generada para unicidad de asignacion activa por talonario manual
    talonario_id_activo BIGINT         GENERATED ALWAYS AS (IF(si_activa = TRUE, id_talonario, NULL)) PERSISTENT COMMENT 'CONTENIDO: id_talonario si asignacion activa, NULL si no. USO: garantiza unicidad de asignacion activa por talonario. REGLA: generada; equivalente en InMemory via talonarioIdActivo.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_tal_insp_activo (talonario_id_activo),
    CONSTRAINT fk_tal_insp_talonario FOREIGN KEY (id_talonario)
        REFERENCES num_talonario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tal_insp_inspector FOREIGN KEY (id_insp, ver_insp)
        REFERENCES fal_inspector_version (id_insp, ver_insp) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_tal_insp_estado CHECK (estado_asignacion BETWEEN 1 AND 4),
    INDEX ix_tal_insp_inspector (id_insp),
    INDEX ix_tal_insp_talonario (id_talonario)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Asignacion y rendicion de talonarios manuales fisicos a inspectores. USO: trazabilidad del talonario fisico. REGLAS CLAVE: estado_asignacion ENTREGADO=1/DEVUELTO=2/CERRADO=3/OBSERVADO=4; columna generada talonario_id_activo garantiza unicidad de asignacion activa.';

-- =============================================================================
-- SECCIÓN 04 — G4: Núcleo del acta (tablas 29–30)
-- Nota de ciclo G4: fal_persona_domicilio sin FK a fal_acta (se agrega vía ALTER).
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 29. fal_persona_domicilio
--     Domicilios de personas vinculadas a actas de faltas.
--     FK acta_origen_id se agrega vía ALTER TABLE post-creación de fal_acta.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_persona_domicilio (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del domicilio',
    persona_id                  BIGINT         NOT NULL               COMMENT 'FK a la persona propietaria del domicilio',
    acta_origen_id              BIGINT         NULL                   COMMENT 'FK al acta donde se capturó el domicilio (nullable). FK agregada vía ALTER TABLE post-creación de fal_acta.',
    tipo_domicilio              SMALLINT       NOT NULL               COMMENT 'Tipo: 1=REAL, 2=LEGAL, 3=FISCAL, 4=CONSTITUIDO, 5=HALLADO, 6=OTRO',
    origen_domicilio            SMALLINT       NOT NULL               COMMENT 'Origen del domicilio (OrigenDomicilio enum)',
    modo_domicilio              SMALLINT       NOT NULL               COMMENT 'Modo: 1=MALVINAS_LOCAL, 2=EXTERNO',
    si_activo                   BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = domicilio activo; FALSE = baja lógica',
    si_notificable              BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = domicilio hábil para notificaciones',
    si_principal                BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = domicilio principal del tipo para la persona',
    id_provincia                SMALLINT       NULL                   COMMENT 'Código de provincia (nullable si no aplica)',
    unidad_territorial_tipo     SMALLINT       NULL                   COMMENT 'Tipo de unidad territorial (UnidadTerritorialTipo enum)',
    id_unidad_territorial       INT            NULL                   COMMENT 'ID de la unidad territorial (municipio/partido/etc.)',
    id_localidad                BIGINT         NULL                   COMMENT 'ID de localidad (modo EXTERNO solamente)',
    id_calle                    BIGINT         NULL                   COMMENT 'ID de calle (modo EXTERNO solamente)',
    id_loc_malvinas             VARCHAR(8)     NULL                   COMMENT 'Código de localidad Malvinas (modo MALVINAS_LOCAL; preserva ceros)',
    localidad_malvinas_version_id BIGINT       NULL                   COMMENT 'FK a geo_malv_localidad_version (baseline; modo MALVINAS_LOCAL)',
    id_tca_malvinas             VARCHAR(10)    NULL                   COMMENT 'Código de calle/trayecto Malvinas (modo MALVINAS_LOCAL; preserva ceros)',
    calle_malvinas_version_id   BIGINT         NULL                   COMMENT 'FK a geo_malv_calle_version (baseline; modo MALVINAS_LOCAL)',
    calle_txt                   VARCHAR(48)    NULL                   COMMENT 'CONTENIDO: Nombre de la calle en texto libre. USO: fallback o modo EXTERNO. REGLA: max 48 caracteres; nullable.',
    altura                      INT            NULL                   COMMENT 'Número de altura/puerta (NULL si sin altura)',
    si_sin_altura               BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = domicilio explícitamente sin altura (exige altura=NULL)',
    unidad_funcional            VARCHAR(20)    NULL                   COMMENT 'Unidad funcional/departamento/piso/dpto',
    codigo_postal               VARCHAR(10)    NULL                   COMMENT 'Código postal del domicilio',
    domicilio_txt               VARCHAR(196)   NULL                   COMMENT 'CONTENIDO: Representacion textual legible del domicilio. USO: cache mutable para visualizacion. REGLA: max 196 caracteres; nullable; se regenera al normalizar.',
    si_normalizado_parcial      BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = domicilio parcialmente normalizado',
    lat                         DECIMAL(10,7)  NULL                   COMMENT 'Latitud del domicilio (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).',
    lon                         DECIMAL(10,7)  NULL                   COMMENT 'Longitud del domicilio (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).',
    origen_ubicacion            SMALLINT       NULL                   COMMENT 'Origen de las coordenadas (OrigenUbicacion enum)',
    fh_alta                     DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta                CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el domicilio',
    fh_ult_mod                  DATETIME(6)    NULL                   COMMENT 'Fecha-hora de la última modificación',
    id_user_ult_mod             CHAR(36)       NULL                   COMMENT 'UUID del usuario que realizó la última modificación',
    PRIMARY KEY (id),
    CONSTRAINT fk_pers_dom_persona FOREIGN KEY (persona_id)
        REFERENCES fal_persona (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_pers_dom_tipo CHECK (tipo_domicilio BETWEEN 1 AND 6),
    CONSTRAINT chk_pers_dom_modo CHECK (modo_domicilio IN (1, 2)),
    INDEX ix_pers_dom_persona (persona_id),
    INDEX ix_pers_dom_acta_origen (acta_origen_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Domicilios de personas vinculadas a actas (no es el lugar del hecho). Sin OCC. Baja lógica vía si_activo=false.';

-- ----------------------------------------------------------------------------
-- 30. fal_acta
--     Agregado raíz del expediente de acta de faltas.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta (
    id                              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del acta',
    id_tecnico                      CHAR(36)        NOT NULL               COMMENT 'UUID técnico único e idempotente del acta (clave de idempotencia)',
    -- identidad y numeración
    nro_acta                        VARCHAR(30)     NULL                   COMMENT 'Número de acta asignado por talonario (nullable antes de numerar)',
    id_talonario                    BIGINT          NULL                   COMMENT 'FK al talonario que emitió el número del acta',
    nro_talonario_usado             INT             NULL                   COMMENT 'Número de talonario consumido por esta acta',
    -- tipo y captura
    tipo_acta                       SMALLINT        NOT NULL               COMMENT 'Tipo: 1=TRANSITO, 2=CONTRAVENCION, 3=SUSTANCIAS_ALIMENTICIAS, 4=COMERCIO',
    origen_captura                  SMALLINT        NOT NULL               COMMENT 'Origen de la captura (OrigenCaptura enum)',
    id_dispositivo_captura          VARCHAR(80)     NULL                   COMMENT 'ID del dispositivo móvil o terminal de captura',
    id_user_captura                 CHAR(36)        NULL                   COMMENT 'UUID del usuario que capturó el acta (nullable si automático)',
    fh_captura                      DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora funcional de captura del acta',
    lat_captura                     DECIMAL(12,8)   NULL                   COMMENT 'Latitud del punto de captura (grados decimales)',
    lon_captura                     DECIMAL(12,8)   NULL                   COMMENT 'Longitud del punto de captura (grados decimales)',
    precision_captura_m             DECIMAL(8,2)    NULL                   COMMENT 'Precisión de la posición de captura en metros',
    fh_pos_captura                  DATETIME(6)     NULL                   COMMENT 'Fecha-hora de la posición de captura (puede diferir de fh_captura)',
    origen_pos_captura              SMALLINT        NULL                   COMMENT 'Origen de la posición de captura (OrigenUbicacion enum)',
    -- fecha funcional del acta
    fecha_acta                      DATE            NOT NULL               COMMENT 'Fecha funcional del labrado del acta',
    fecha_labrado                   DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora funcional del labrado del acta',
    -- dependencia e inspector versionados
    id_dep                          BIGINT          NOT NULL               COMMENT 'FK a la dependencia emisora del acta',
    ver_dep                         SMALLINT        NOT NULL               COMMENT 'Versión de la dependencia al momento del labrado (SMALLINT: tipo exacto de fal_dependencia_version.ver_dep)',
    id_insp                         BIGINT          NULL                   COMMENT 'FK al inspector firmante del acta (nullable si no aplica)',
    ver_insp                        SMALLINT        NULL                   COMMENT 'Versión del inspector (SMALLINT: tipo exacto de fal_inspector_version.ver_insp; obligatoria si hay id_insp)',
    -- infractor y domicilios
    id_persona_infractor            BIGINT          NOT NULL               COMMENT 'FK a la persona infractora (toda acta tiene sujeto técnico)',
    id_domicilio_infractor_act      BIGINT          NULL                   COMMENT 'FK al domicilio actual del infractor (snapshot del domicilio en el acta)',
    id_domicilio_notif_act          BIGINT          NULL                   COMMENT 'FK al domicilio de notificación actual del infractor',
    -- resumen del hecho
    resumen_hecho                   VARCHAR(1000)   NULL                   COMMENT 'Resumen narrativo del hecho infractor',
    domicilio_hecho                 VARCHAR(196)    NULL                   COMMENT 'CONTENIDO: Texto libre del lugar del hecho (compatibilidad). USO: compatibilidad con captura previa; lugar normalizado en campos nomenclatura. REGLA: max 196 caracteres; nullable.',
    -- lugar del hecho: nomenclatura local Malvinas
    id_loc_infr_malvinas            VARCHAR(8)      NULL                   COMMENT 'Código de localidad Malvinas del lugar del hecho (preserva ceros)',
    localidad_infr_malvinas_version_id BIGINT       NULL                   COMMENT 'FK a geo_malv_localidad_version (baseline) del lugar del hecho',
    id_tca_infr_malvinas            VARCHAR(10)     NULL                   COMMENT 'Código de trayecto/calle Malvinas del lugar del hecho (preserva ceros)',
    calle_infr_malvinas_version_id  BIGINT          NULL                   COMMENT 'FK a geo_malv_calle_version (baseline) del lugar del hecho',
    -- lugar del hecho: altura
    altura_infr                     INT             NULL                   COMMENT 'Número de altura del lugar del hecho',
    altura_origen_infr              SMALLINT        NULL                   COMMENT 'Origen del dato de altura (OrigenNomenclatura enum)',
    si_altura_infr_estimada         BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = altura del lugar del hecho es una estimación',
    -- lugar del hecho: coordenadas finales
    lat_infr                        DECIMAL(12,8)   NULL                   COMMENT 'Latitud final del lugar del hecho (grados decimales)',
    lon_infr                        DECIMAL(12,8)   NULL                   COMMENT 'Longitud final del lugar del hecho (grados decimales)',
    origen_ubicacion_infr           SMALLINT        NULL                   COMMENT 'Origen de las coordenadas del lugar del hecho (OrigenUbicacion)',
    si_ubicacion_infr_manual        BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = ubicación del hecho ajustada manualmente',
    -- texto libre del lugar del hecho
    si_dom_txt_infr                 BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = se usa texto libre para el lugar del hecho',
    dom_txt_infr                    VARCHAR(196)    NULL                   COMMENT 'CONTENIDO: Texto libre del lugar del hecho. USO: solo cuando si_dom_txt_infr=TRUE. REGLA: max 196 caracteres; nullable.',
    -- contexto geográfico
    si_eje_urb                      BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = el hecho ocurrió en el eje urbano del municipio',
    -- QR
    codigo_qr                       VARCHAR(128)    NOT NULL               COMMENT 'CONTENIDO: Token firmado del QR del acta, formato QR0.<uuid-acta>.<version>.<firma-hmac-base64url>. USO: identificacion publica del acta via QR. REGLA: 128 caracteres cubre el token firmado completo; no contiene datos personales, domicilio ni imagen; UNIQUE.',
    qr_payload_version              SMALLINT        NOT NULL DEFAULT 0     COMMENT 'Versión del payload QR (0=inicial; incrementar si cambia la estructura)',
    -- estado actual (tripla canónica)
    bloque_actual                   CHAR(4)         NOT NULL               COMMENT 'Bloque procesal actual del acta (BloqueActual enum: CAPT, D2, D3, FALL, etc.)',
    est_proc_act                    CHAR(4)         NOT NULL               COMMENT 'Estado procesal actual (EstadoProcesalActa enum: ENTR, etc.)',
    sit_adm_act                     CHAR(4)         NOT NULL               COMMENT 'Situación administrativa actual (SituacionAdministrativaActa enum: ACTV, ARCH, etc.)',
    -- resultado y cierre
    resultado_final                 SMALLINT        NOT NULL DEFAULT 0     COMMENT 'Resultado final del acta (ResultadoFinalActa enum; 0=SIN_RESULTADO_FINAL)',
    resultado_firma_infractor       SMALLINT        NOT NULL               COMMENT 'Resultado de la firma del infractor (ResultadoFirmaInfractor enum)',
    id_motivo_archivo_actual        BIGINT          NULL                   COMMENT 'FK al motivo de archivo vigente (caché desde ciclo de archivo activo)',
    permite_reingreso               BOOLEAN         NULL                   COMMENT 'Caché del permiso de reingreso del archivo activo (NULL si no archivada)',
    fh_cierre                       DATETIME(6)     NULL                   COMMENT 'Fecha-hora funcional de cierre del acta (NULL si no cerrada)',
    fh_archivo                      DATETIME(6)     NULL                   COMMENT 'Fecha-hora funcional de archivo del acta (NULL si no archivada)',
    -- OCC y auditoría
    version_row                     INT             NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    fh_alta                         DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta                    CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que labró el acta',
    fh_ult_mod                      DATETIME(6)     NULL                   COMMENT 'Fecha-hora de la última modificación',
    id_user_ult_mod                 CHAR(36)        NULL                   COMMENT 'UUID del usuario que realizó la última modificación',
    PRIMARY KEY (id),
    UNIQUE KEY uq_fal_acta_id_tecnico (id_tecnico),
    UNIQUE KEY uq_fal_acta_codigo_qr (codigo_qr),
    CONSTRAINT fk_acta_dep_ver FOREIGN KEY (id_dep, ver_dep)
        REFERENCES fal_dependencia_version (id_dep, ver_dep) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_insp_ver FOREIGN KEY (id_insp, ver_insp)
        REFERENCES fal_inspector_version (id_insp, ver_insp) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_persona_infractor FOREIGN KEY (id_persona_infractor)
        REFERENCES fal_persona (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_dom_infractor FOREIGN KEY (id_domicilio_infractor_act)
        REFERENCES fal_persona_domicilio (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_dom_notif FOREIGN KEY (id_domicilio_notif_act)
        REFERENCES fal_persona_domicilio (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_motivo_archivo FOREIGN KEY (id_motivo_archivo_actual)
        REFERENCES fal_motivo_archivo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_talonario FOREIGN KEY (id_talonario)
        REFERENCES num_talonario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_acta_tipo CHECK (tipo_acta IN (1, 2, 3, 4)),
    CONSTRAINT chk_acta_qr_ver CHECK (qr_payload_version >= 0),
    INDEX ix_fal_acta_persona (id_persona_infractor),
    INDEX ix_fal_acta_dep (id_dep),
    INDEX ix_fal_acta_fecha (fecha_acta),
    INDEX ix_fal_acta_estado (bloque_actual, est_proc_act, sit_adm_act)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Agregado raíz del expediente de acta de faltas. OCC vía version_row. id_tecnico como clave de idempotencia.';

-- =============================================================================
-- ALTER TABLE — Ciclos G1 y G4
-- =============================================================================

-- Ciclo G1: fal_dependencia auto-referencial
ALTER TABLE fal_dependencia
    ADD CONSTRAINT fk_dep_padre FOREIGN KEY (id_dep_padre)
        REFERENCES fal_dependencia (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Ciclo G4: fal_persona_domicilio → fal_acta
ALTER TABLE fal_persona_domicilio
    ADD CONSTRAINT fk_pers_dom_acta_origen FOREIGN KEY (acta_origen_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- FK GEO canónicas: fal_persona_domicilio → geo_malv_localidad_version / geo_malv_calle_version
-- (tablas del baseline protegido; BIGINT → BIGINT; ON DELETE RESTRICT ON UPDATE RESTRICT)
ALTER TABLE fal_persona_domicilio
    ADD CONSTRAINT fk_pers_dom_loc_malv FOREIGN KEY (localidad_malvinas_version_id)
        REFERENCES geo_malv_localidad_version (localidad_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_pers_dom_calle_malv FOREIGN KEY (calle_malvinas_version_id)
        REFERENCES geo_malv_calle_version (calle_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- FK GEO canónicas: fal_acta → geo_malv_localidad_version / geo_malv_calle_version
-- (tablas del baseline protegido; BIGINT → BIGINT; ON DELETE RESTRICT ON UPDATE RESTRICT)
ALTER TABLE fal_acta
    ADD CONSTRAINT fk_acta_loc_infr_malv FOREIGN KEY (localidad_infr_malvinas_version_id)
        REFERENCES geo_malv_localidad_version (localidad_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_acta_calle_infr_malv FOREIGN KEY (calle_infr_malvinas_version_id)
        REFERENCES geo_malv_calle_version (calle_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- =============================================================================
-- SECCIÓN 05 — G5: Satélites del acta (tablas 31–45)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 31. fal_acta_evidencia
--     Evidencias físicas vinculadas al acta (imágenes, firma ológrafa).
--     Append-only. (DECISION_DDL-EVID-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_evidencia (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la evidencia',
    id_acta         BIGINT         NOT NULL               COMMENT 'FK al acta a la que pertenece la evidencia',
    tipo_evid       SMALLINT       NOT NULL               COMMENT 'Tipo de evidencia. Codigos (HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10): 4=FOTO, 8=VIDEO, 12=AUDIO, 16=PDF, 20=DOCUMENTO_OFIMATICO, 24=PLANILLA_CALCULO, 48=FIRMA_OLOGRAFA_INFRACTOR. Codigos 28-44 reservados. TipoEvidenciaActa enum.',
    storage_key     VARCHAR(255)   NOT NULL               COMMENT 'Clave de almacenamiento externo del binario (no se guarda el binario aquí)',
    fecha_registro  DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora funcional de captura o incorporación de la evidencia',
    hash_evid       CHAR(64)       NULL                   COMMENT 'Hash SHA-256 hexadecimal del binario (64 chars; nullable)',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta del registro',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'UUID del actor que incorporó la evidencia',
    PRIMARY KEY (id),
    CONSTRAINT fk_evidencia_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_evidencia_hash CHECK (hash_evid IS NULL OR CHAR_LENGTH(hash_evid) = 64),
    INDEX ix_evidencia_acta (id_acta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Evidencias físicas del acta (firma ológrafa del infractor, etc.). Append-only. DECISION_DDL-EVID-01.';

-- ----------------------------------------------------------------------------
-- 32. fal_observacion
--     Observaciones tipificadas por entidad en el expediente.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_observacion (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    entidad_tipo        SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de la entidad observada. USO: identifica la tabla logica del objeto observado. REGLA: EntidadTipoObservada enum; 22 codigos canonicos.',
    entidad_id          BIGINT         NOT NULL               COMMENT 'CONTENIDO: ID de la entidad observada. USO: identifica el registro especifico. REGLA: no tiene FK fisica (validacion en capa de aplicacion).',
    id_acta_contexto    BIGINT         NULL                   COMMENT 'CONTENIDO: FK opcional al acta de contexto. USO: vincular observacion con el acta que la origina. REGLA: ON DELETE CASCADE; NULL si la observacion no tiene contexto de acta.',
    origen_observacion  SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Origen de la observacion. USO: clasifica la fuente. VALORES: 1=OPERADOR, 2=SISTEMA, 3=EXTERNO (OrigenObservacion enum).',
    observacion         VARCHAR(512)   NOT NULL               COMMENT 'CONTENIDO: Texto de la observacion. USO: nota libre auditable. REGLA: CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512.',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: baja logica. REGLA: FALSE = desactivada por operador; se registra fh_baja/id_user_baja.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_baja             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de baja logica. USO: auditoria. REGLA: NULL si si_activa=TRUE; NOT NULL si si_activa=FALSE.',
    id_user_baja        CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que realizo la baja. USO: auditoria. REGLA: NULL si si_activa=TRUE; NOT NULL si si_activa=FALSE.',
    PRIMARY KEY (id),
    CONSTRAINT fk_obs_acta_contexto FOREIGN KEY (id_acta_contexto)
        REFERENCES fal_acta (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT chk_obs_texto CHECK (CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512),
    CONSTRAINT chk_obs_baja_coherente CHECK (
        (si_activa = TRUE  AND fh_baja IS NULL     AND id_user_baja IS NULL) OR
        (si_activa = FALSE AND fh_baja IS NOT NULL AND id_user_baja IS NOT NULL)
    ),
    INDEX ix_obs_entidad (entidad_tipo, entidad_id, si_activa, fh_alta),
    INDEX ix_obs_acta_contexto (id_acta_contexto, fh_alta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Observaciones polimorficas centralizadas del dominio. USO: notas libres, humanas, acumulables y auditables por cualquier entidad observable. RAZON: evitar campos observacion embebidos en entidades que se sobreescriben. REGLAS CLAVE: entidad_tipo+entidad_id identifica el objeto; id_acta_contexto ON DELETE CASCADE; no hay FK fisica por entidad_id.';

-- ----------------------------------------------------------------------------
-- 33. fal_acta_transito
--     Datos específicos de actas de tránsito (1:1 con fal_acta).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_transito (
    acta_id                     BIGINT         NOT NULL               COMMENT 'PK y FK al acta de tránsito (relación 1:1)',
    nro_licencia                VARCHAR(20)    NULL                   COMMENT 'Número de licencia de conducir del infractor',
    id_prov_lic                 SMALLINT       NULL                   COMMENT 'Código de provincia emisora de la licencia',
    unidad_territorial_lic_tipo SMALLINT       NULL                   COMMENT 'Tipo de unidad territorial de la licencia (UnidadTerritorialTipo)',
    id_unidad_territorial_lic   INT            NULL                   COMMENT 'ID de la unidad territorial de la licencia',
    si_ret_licencia             BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = se retuvo la licencia de conducir',
    si_ret_vehiculo             BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = se retuvo el vehículo',
    si_control_alcoholemia      BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = se realizó control de alcoholemia',
    PRIMARY KEY (acta_id),
    CONSTRAINT fk_acta_transito_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Datos específicos de actas de tránsito (1:1 con fal_acta). Solo tipo_acta=TRANSITO.';

-- ----------------------------------------------------------------------------
-- 34. fal_acta_transito_alcoholemia
--     Mediciones de alcoholemia vinculadas al acta de tránsito.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_transito_alcoholemia (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la medición',
    acta_id             BIGINT          NOT NULL               COMMENT 'FK al acta de tránsito',
    orden_medicion      SMALLINT        NOT NULL               COMMENT 'Secuencia ordinal de la medición dentro del acta (positivo, único por acta)',
    tipo_prueba         SMALLINT        NOT NULL               COMMENT 'Tipo de prueba de alcoholemia (TipoPruebaAlcoholemia enum)',
    resultado_cualitativo SMALLINT      NULL                   COMMENT 'Resultado cualitativo (ResultadoCualitativoAlcoholemia enum; nullable)',
    resultado_numerico  DECIMAL(4,2)    NULL                   COMMENT 'Resultado numérico en la unidad indicada (nullable si no medible)',
    unidad_medida       SMALLINT        NULL                   COMMENT 'Unidad de medida del resultado numérico (UnidadMedidaAlcoholemia; obligatoria si hay resultado_numerico)',
    id_alcoholimetro    BIGINT          NULL                   COMMENT 'ID del alcoholímetro utilizado (nullable)',
    ver_alcoholimetro   SMALLINT        NULL                   COMMENT 'Versión del alcoholímetro (obligatoria si hay id_alcoholimetro)',
    si_resultado_final  BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = esta es la medición final utilizada para la decisión',
    fh_medicion         DATETIME(6)     NULL                   COMMENT 'Fecha-hora de la medición (nullable si no se informó)',
    fh_alta             DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que registró la medición',
    PRIMARY KEY (id),
    UNIQUE KEY uq_acta_alcoholemia_orden (acta_id, orden_medicion),
    CONSTRAINT fk_alcoholemia_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_alcoholemia_orden CHECK (orden_medicion > 0),
    INDEX ix_alcoholemia_acta (acta_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Mediciones de alcoholemia en actas de tránsito (N mediciones por acta; una como resultado final)';

-- ----------------------------------------------------------------------------
-- 35. fal_acta_vehiculo
--     Datos del vehículo registrado en el acta (1:1 con fal_acta).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_vehiculo (
    acta_id                 BIGINT         NOT NULL               COMMENT 'PK y FK al acta (relación 1:1)',
    dominio_vehiculo        VARCHAR(10)    NULL                   COMMENT 'Patente/dominio del vehículo (nullable si no se pudo leer)',
    tipo_vehiculo           SMALLINT       NULL                   COMMENT 'Tipo de vehículo (TipoVehiculo enum; nullable)',
    marca_vehiculo_id       BIGINT         NULL                   COMMENT 'FK a la marca del vehículo (nullable si usa texto libre)',
    marca_vehiculo_txt      VARCHAR(24)    NULL                   COMMENT 'Texto libre de la marca (fallback si no está en catálogo)',
    modelo_vehiculo_id      BIGINT         NULL                   COMMENT 'FK al modelo del vehículo (nullable si usa texto libre)',
    modelo_vehiculo_txt     VARCHAR(24)    NULL                   COMMENT 'Texto libre del modelo (fallback si no está en catálogo)',
    anio_vehiculo           SMALLINT       NULL                   COMMENT 'Año de fabricación del vehículo',
    color_vehiculo          VARCHAR(24)    NULL                   COMMENT 'Color del vehículo en texto libre',
    estado_general_vehiculo SMALLINT       NULL                   COMMENT 'Estado general del vehículo (EstadoGeneralVehiculo enum)',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que registró el vehículo',
    PRIMARY KEY (acta_id),
    CONSTRAINT fk_acta_veh_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_veh_marca FOREIGN KEY (marca_vehiculo_id)
        REFERENCES fal_vehiculo_marca (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_veh_modelo FOREIGN KEY (modelo_vehiculo_id)
        REFERENCES fal_vehiculo_modelo (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Datos del vehículo en el acta de tránsito (1:1). Permite fallback textual si no existe en catálogo.';

-- ----------------------------------------------------------------------------
-- 36. fal_acta_contravencion
--     Datos específicos de actas de contravención/comercio (1:1 con fal_acta).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_contravencion (
    acta_id                     BIGINT             NOT NULL               COMMENT 'CONTENIDO: PK y FK al acta. USO: relacion 1:1. REGLA: referencia fal_acta(id).',
    id_suj_i                    TINYINT UNSIGNED   NULL                   COMMENT 'CONTENIDO: Codigo de tipo de sujeto inmueble en Ingresos Municipales (catalogo abierto; no es ID unico de sujeto). USO: vinculacion catastral. REGLA: BETWEEN 1 AND 255; nullable. HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    id_bie_i                    MEDIUMINT UNSIGNED NULL                   COMMENT 'CONTENIDO: ID bien inmueble (cuenta CVP). USO: vinculacion catastral. REGLA: BETWEEN 1 AND 9999999; nullable.',
    id_suj_c                    TINYINT UNSIGNED   NULL                   COMMENT 'CONTENIDO: Codigo de tipo de sujeto comercio en Ingresos Municipales (catalogo abierto; no es ID unico de sujeto). USO: vinculacion catastral. REGLA: BETWEEN 1 AND 255; nullable. HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    id_bie_c                    MEDIUMINT UNSIGNED NULL                   COMMENT 'CONTENIDO: ID bien comercio en Catastro/Ingresos. USO: vinculacion catastral. REGLA: BETWEEN 1 AND 9999999; nullable.',
    circ                        SMALLINT           NULL                   COMMENT 'CONTENIDO: Circunscripcion catastral del inmueble. REGLA: nullable.',
    secc                        VARCHAR(2)         NULL                   COMMENT 'CONTENIDO: Seccion catastral. REGLA: nullable.',
    frac                        VARCHAR(7)         NULL                   COMMENT 'CONTENIDO: Fraccion catastral. REGLA: nullable.',
    mza                         VARCHAR(7)         NULL                   COMMENT 'CONTENIDO: Manzana catastral. REGLA: nullable.',
    parc                        VARCHAR(7)         NULL                   COMMENT 'CONTENIDO: Parcela catastral. REGLA: nullable.',
    ufun                        VARCHAR(7)         NULL                   COMMENT 'CONTENIDO: Unidad funcional catastral. REGLA: nullable.',
    ucomp                       VARCHAR(20)        NULL                   COMMENT 'CONTENIDO: Unidad complementaria catastral. REGLA: nullable.',
    origen_nomencl              SMALLINT           NOT NULL               COMMENT 'CONTENIDO: Origen de la nomenclatura catastral. VALORES: OrigenNomenclatura enum.',
    si_nomenclatura_manual      BOOLEAN            NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si nomenclatura ingresada manualmente. REGLA: implica motivo_nomenclatura_manual obligatorio.',
    motivo_nomenclatura_manual  SMALLINT           NULL                   COMMENT 'CONTENIDO: Motivo del ingreso manual. REGLA: obligatorio si si_nomenclatura_manual=TRUE. VALORES: MotivoNomenclaturaManual enum.',
    rubro_id                    BIGINT             NULL                   COMMENT 'CONTENIDO: FK a fal_rubro_version (rubro del comercio). REGLA: nullable.',
    id_rub                      INT                NULL                   COMMENT 'CONTENIDO: ID externo del rubro en Informix. USO: integracion legacy. REGLA: nullable.',
    ambito_ctv                  SMALLINT           NULL                   COMMENT 'CONTENIDO: Ambito de la contravencion. VALORES: AmbitoCtv enum; nullable.',
    ambito_ctv_txt              VARCHAR(48)        NULL                   COMMENT 'CONTENIDO: Texto libre del ambito. REGLA: obligatorio solo si ambito_ctv=OTRO.',
    fh_alta                     DATETIME(6)        NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. REGLA: suministrado por FaltasClock.',
    id_user_alta                CHAR(36)           NOT NULL               COMMENT 'CONTENIDO: UUID del usuario que registro los datos. REGLA: UUID.',
    PRIMARY KEY (acta_id),
    CONSTRAINT fk_acta_ctv_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_ctv_rubro FOREIGN KEY (rubro_id)
        REFERENCES fal_rubro_version (rubro_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_ctv_suj_i CHECK (id_suj_i IS NULL OR (id_suj_i BETWEEN 1 AND 255)),
    CONSTRAINT chk_ctv_bie_i CHECK (id_bie_i IS NULL OR (id_bie_i BETWEEN 1 AND 9999999)),
    CONSTRAINT chk_ctv_suj_c CHECK (id_suj_c IS NULL OR (id_suj_c BETWEEN 1 AND 255)),
    CONSTRAINT chk_ctv_bie_c CHECK (id_bie_c IS NULL OR (id_bie_c BETWEEN 1 AND 9999999))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Datos de nomenclatura y rubro en actas de contravención/comercio (1:1). Instancia histórica al labrar.';

-- ----------------------------------------------------------------------------
-- 37. fal_acta_sustancias_alimenticias
--     Datos de actas de sustancias alimenticias/bromatología (1:1 con fal_acta).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_sustancias_alimenticias (
    acta_id             BIGINT         NOT NULL               COMMENT 'CONTENIDO: PK y FK al acta. USO: relacion 1:1. REGLA: referencia fal_acta(id).',
    rubro_id            BIGINT         NULL                   COMMENT 'CONTENIDO: FK a fal_rubro_version (rubro del establecimiento). REGLA: nullable.',
    id_rub              INT            NULL                   COMMENT 'CONTENIDO: ID externo del rubro en Informix. USO: integracion legacy. REGLA: nullable.',
    ambito_ctv          SMALLINT       NULL                   COMMENT 'CONTENIDO: Ambito de la infraccion. VALORES: AmbitoCtv enum; nullable.',
    ambito_ctv_txt      VARCHAR(48)    NULL                   COMMENT 'CONTENIDO: Texto libre del ambito. REGLA: obligatorio solo si ambito_ctv=OTRO.',
    descripcion_sustancias TEXT        NULL                   COMMENT 'CONTENIDO: Descripcion de las sustancias involucradas. USO: registro bromatologico. REGLA: nullable.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario que registro los datos. REGLA: UUID.',
    PRIMARY KEY (acta_id),
    CONSTRAINT fk_acta_sus_alim_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_sus_alim_rubro FOREIGN KEY (rubro_id)
        REFERENCES fal_rubro_version (rubro_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Datos específicos de actas de sustancias alimenticias/bromatología (1:1 con fal_acta)';

-- ----------------------------------------------------------------------------
-- 38. fal_acta_paralizacion
--     Paralizaciones del expediente de acta (suspensiones temporales).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_paralizacion (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la paralización',
    id_acta         BIGINT         NOT NULL               COMMENT 'FK al acta paralizada',
    motivo          SMALLINT       NOT NULL               COMMENT 'Motivo de la paralización (MotivoParalizacion enum)',
    descripcion     TEXT           NULL                   COMMENT 'Descripción adicional del motivo de paralización',
    id_user_inicio  CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que inició la paralización',
    fh_inicio       DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora de inicio de la paralización',
    id_user_cierre  CHAR(36)       NULL                   COMMENT 'UUID del usuario que cerró la paralización (NULL = activa)',
    fh_cierre       DATETIME(6)    NULL                   COMMENT 'Fecha-hora de cierre de la paralización (NULL = activa)',
    motivo_cierre   SMALLINT       NULL                   COMMENT 'Motivo del levantamiento de la paralización',
    si_activa       BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = paralización vigente; FALSE = levantada',
    PRIMARY KEY (id),
    CONSTRAINT fk_paralizacion_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_paralizacion_acta (id_acta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Paralizaciones del expediente de faltas (suspensiones temporales del trámite)';

-- ----------------------------------------------------------------------------
-- 39. fal_acta_archivo
--     Registros de archivo y reingreso del expediente de acta.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_archivo (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del registro de archivo',
    id_acta             BIGINT         NOT NULL               COMMENT 'FK al acta archivada',
    motivo_archivo      SMALLINT       NOT NULL               COMMENT 'Motivo del archivo (código; ver fal_motivo_archivo)',
    descripcion         TEXT           NULL                   COMMENT 'Descripción adicional del archivo',
    id_user_archivo     CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que archivó el acta',
    fh_archivo          DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora de archivo del acta',
    id_user_reingreso   CHAR(36)       NULL                   COMMENT 'UUID del usuario que reinició el trámite (NULL = sin reingreso)',
    fh_reingreso        DATETIME(6)    NULL                   COMMENT 'Fecha-hora de reingreso del acta (NULL = sin reingreso)',
    motivo_reingreso    SMALLINT       NULL                   COMMENT 'Motivo del reingreso (nullable)',
    si_archivado        BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = acta archivada; FALSE = reingresada',
    PRIMARY KEY (id),
    CONSTRAINT fk_archivo_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_archivo_acta (id_acta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Historial de archivos y reingresos de expedientes de faltas. Archivo no equivale automáticamente a CERRADA.';

-- ----------------------------------------------------------------------------
-- 40. fal_acta_articulo_infringido
--     Artículos infractores imputados en el acta.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_articulo_infringido (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del artículo infringido',
    id_acta             BIGINT          NOT NULL               COMMENT 'FK al acta',
    id_articulo         BIGINT          NOT NULL               COMMENT 'FK al artículo normativo infringido',
    cantidad_unidades   DECIMAL(10,4)   NOT NULL DEFAULT 1     COMMENT 'Cantidad de unidades del artículo imputado',
    si_activo           BOOLEAN         NOT NULL DEFAULT TRUE  COMMENT 'TRUE = artículo vigente en el acta; FALSE = dado de baja',
    motivo_baja         SMALLINT        NULL                   COMMENT 'CONTENIDO: Motivo de la baja del articulo. USO: trazabilidad. REGLA: MotivoBajaArticuloInfringido enum; nullable.',
    fh_alta             DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que imputó el artículo',
    -- UK activo: solo un registro activo por (acta, artículo) usando columnas generadas
    id_acta_activo      BIGINT          GENERATED ALWAYS AS (IF(si_activo = TRUE, id_acta, NULL)) PERSISTENT COMMENT 'Generada: id_acta si activo, NULL si no (para UK activo)',
    id_art_activo       BIGINT          GENERATED ALWAYS AS (IF(si_activo = TRUE, id_articulo, NULL)) PERSISTENT COMMENT 'Generada: id_articulo si activo, NULL si no (para UK activo)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_acta_art_activo (id_acta_activo, id_art_activo),
    CONSTRAINT fk_acta_art_infr_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_art_infr_articulo FOREIGN KEY (id_articulo)
        REFERENCES fal_articulo_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_acta_art_infr_acta (id_acta)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Artículos normativos imputados en cada acta. UK activo (id_acta, id_articulo) via columnas generadas.';

-- ----------------------------------------------------------------------------
-- 41. fal_acta_qr_acceso
--     Registro de cada acceso al portal del infractor vía código QR.
--     Append-only.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_qr_acceso (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    acta_id             BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al acta accedida via QR. USO: trazabilidad por acta. REGLA: referencia fal_acta(id).',
    fh_acceso           DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp del acceso. USO: timeline de auditoria. REGLA: suministrado por FaltasClock.',
    ip_origen           VARCHAR(45)    NULL                   COMMENT 'CONTENIDO: IP origen del acceso (IPv4 o IPv6). USO: auditoria de seguridad. REGLA: nullable si no disponible.',
    user_agent          VARCHAR(255)   NULL                   COMMENT 'CONTENIDO: User-agent sanitizado del cliente. USO: auditoria tecnica. REGLA: nullable; sanitizado de caracteres de control (SPEC-MODEL-DDL-CLOSURE-001).',
    resultado_acceso_qr SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Resultado del acceso. USO: auditoria de seguridad. VALORES: codigos de ResultadoAccesoQr enum (SPEC-MODEL-DDL-CLOSURE-001).',
    -- auditoria: solo fh_alta (tabla append-only sin actor de sistema; SPEC-MODEL-DDL-CLOSURE-001)
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    PRIMARY KEY (id),
    CONSTRAINT fk_qr_acceso_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_qr_acceso_acta (acta_id),
    INDEX ix_qr_acceso_fh (fh_acceso)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Log append-only de auditoría de accesos via QR al portal del infractor. USO: auditoria de seguridad. REGLAS CLAVE: sin canal_acceso_qr (constante WEB); resultado_acceso_qr SMALLINT enum; auditoria solo fh_alta. SPEC-MODEL-DDL-CLOSURE-001.';

-- ----------------------------------------------------------------------------
-- 42. fal_acta_gestion_externa
--     Gestiones externas (judicial, apremio, etc.) del expediente.
--     OCC. (DECISION_DDL-GEXT-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_gestion_externa (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la gestión externa',
    version_row         INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC). DECISION_DDL-GEXT-01.',
    id_acta             BIGINT         NOT NULL               COMMENT 'FK al acta en gestión externa',
    tipo_gestion_ext    SMALLINT       NOT NULL               COMMENT 'Tipo de gestión (TipoGestionExterna enum)',
    modo_reingreso      SMALLINT       NULL                   COMMENT 'Modo de reingreso desde la gestión (ModoReingresoGestionExterna; nullable)',
    estado_gestion_ext  SMALLINT       NOT NULL               COMMENT 'Estado actual de la gestión (EstadoGestionExterna enum)',
    resultado_gestion_ext SMALLINT     NULL                   COMMENT 'Resultado final de la gestión (ResultadoGestionExterna; nullable hasta cierre)',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = gestión vigente (a lo sumo una activa por acta)',
    descripcion         TEXT           NULL                   COMMENT 'Descripción adicional de la gestión',
    fh_inicio           DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora de inicio de la gestión',
    id_user_inicio      CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que inició la gestión',
    fh_cierre           DATETIME(6)    NULL                   COMMENT 'Fecha-hora de cierre de la gestión (NULL = activa)',
    id_user_cierre      CHAR(36)       NULL                   COMMENT 'UUID del usuario que cerró la gestión',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el registro',
    PRIMARY KEY (id),
    CONSTRAINT fk_gestion_ext_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_gestion_ext_acta (id_acta),
    INDEX ix_gestion_ext_activa (id_acta, si_activa)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Gestiones externas del expediente (judicial, apremio). OCC. Invariante: a lo sumo una activa por acta. DECISION_DDL-GEXT-01.';

-- ----------------------------------------------------------------------------
-- 43. fal_acta_valorizacion
--     Decisión económica global del acta en un momento dado.
--     OCC.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_valorizacion (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    version_row         INT             NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Version para OCC. USO: control de concurrencia optimista. REGLA: incrementar en cada UPDATE.',
    id_acta             BIGINT          NOT NULL               COMMENT 'CONTENIDO: FK al acta valorizada. USO: trazabilidad. REGLA: referencia fal_acta(id).',
    tipo_valorizacion   SMALLINT        NOT NULL               COMMENT 'CONTENIDO: Tipo de valorizacion. VALORES: TipoValorizacion enum.',
    estado_valorizacion SMALLINT        NOT NULL               COMMENT 'CONTENIDO: Estado. VALORES: 1=PRELIMINAR, 2=CONFIRMADA, 3=REEMPLAZADA, 4=ANULADA.',
    monto_total         DECIMAL(14,2)   NOT NULL               COMMENT 'CONTENIDO: Monto total. USO: valor economico del acta. REGLA: NOT NULL; escala 2; >= 0.',
    fh_calculo          DATETIME(6)     NOT NULL               COMMENT 'CONTENIDO: Timestamp del calculo. USO: trazabilidad. REGLA: NOT NULL; suministrado por FaltasClock.',
    si_vigente          BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si es la valorizacion activa del acta. REGLA: una sola CONFIRMADA vigente por acta+tipo.',
    si_confirmada       BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si fue confirmada operativamente. REGLA: consistente con estado_valorizacion.',
    fh_alta             DATETIME(6)     NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)        NOT NULL               COMMENT 'CONTENIDO: UUID del usuario que creo el registro. USO: auditoria. REGLA: UUID.',
    PRIMARY KEY (id),
    CONSTRAINT fk_valorizacion_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_valorizacion_monto CHECK (monto_total >= 0),
    CONSTRAINT chk_valorizacion_estado CHECK (estado_valorizacion BETWEEN 1 AND 4),
    INDEX ix_valorizacion_acta (id_acta),
    INDEX ix_valorizacion_acta_vigente (id_acta, si_vigente)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Decisión económica del acta en un momento dado. OCC. Una sola CONFIRMADA vigente por acta+tipo.';

-- ----------------------------------------------------------------------------
-- 44. fal_acta_medida_preventiva
--     Medidas preventivas efectivamente aplicadas en el acta.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_medida_preventiva (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la medida aplicada',
    acta_id                 BIGINT         NOT NULL               COMMENT 'FK al acta donde se aplicó la medida',
    acta_articulo_id        BIGINT         NOT NULL               COMMENT 'FK al artículo infringido que habilitó la medida',
    medida_preventiva_id    BIGINT         NOT NULL               COMMENT 'FK a la medida preventiva del catálogo aplicada',
    med_prev_txt            VARCHAR(255)   NULL                   COMMENT 'Texto descriptivo de la medida (snapshot al momento de aplicar)',
    estado_medida           SMALLINT       NOT NULL DEFAULT 1     COMMENT '1=APLICADA, 2=CUMPLIDA, 3=ANULADA (EstadoMedidaAplicada)',
    si_genera_bloqueante    BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = esta aplicación generó un bloqueante de cierre material',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que aplicó la medida',
    PRIMARY KEY (id),
    CONSTRAINT fk_med_prev_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_med_prev_acta_art FOREIGN KEY (acta_articulo_id)
        REFERENCES fal_acta_articulo_infringido (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_med_prev_catalogo FOREIGN KEY (medida_preventiva_id)
        REFERENCES fal_medida_preventiva (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_med_prev_acta (acta_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Medidas preventivas aplicadas al acta. Relación con bloqueantes es unidireccional (bloqueante → medida).';

-- ----------------------------------------------------------------------------
-- 45. fal_acta_bloqueante_cierre_material
--     Bloqueantes materiales que impiden el cierre del expediente.
--     OCC. (DECISION_DDL-BLOQ-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_bloqueante_cierre_material (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del bloqueante',
    version_row                 INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC). DECISION_DDL-BLOQ-01.',
    id_acta                     BIGINT         NOT NULL               COMMENT 'FK al acta bloqueada',
    origen                      SMALLINT       NOT NULL               COMMENT 'Origen del bloqueante (OrigenBloqueanteMaterial enum)',
    estado                      SMALLINT       NOT NULL               COMMENT 'Estado del bloqueante (EstadoBloqueanteMaterial enum)',
    si_activo                   BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = bloqueante vigente; FALSE = resuelto/cerrado',
    descripcion                 TEXT           NULL                   COMMENT 'Descripción del bloqueante (qué debe resolverse)',
    medida_preventiva_acta_id   BIGINT         NULL                   COMMENT 'FK a la medida preventiva que originó el bloqueante (nullable)',
    fh_alta                     DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta del bloqueante',
    fh_cierre                   DATETIME(6)    NULL                   COMMENT 'Fecha-hora de cierre del bloqueante (NULL = activo)',
    id_user_alta                CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que registró el bloqueante',
    PRIMARY KEY (id),
    CONSTRAINT fk_bloq_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bloq_med_prev_acta FOREIGN KEY (medida_preventiva_acta_id)
        REFERENCES fal_acta_medida_preventiva (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_bloq_acta_activo (id_acta, si_activo)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Bloqueantes materiales de cierre del expediente. OCC. DECISION_DDL-BLOQ-01.';

-- =============================================================================
-- SECCIÓN 06 — G6: Documentos directos (tablas 46–47)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 46. fal_documento
--     Documentos del expediente (resoluciones, intimaciones, notificaciones, etc.).
--     OCC. (DECISION_DDL-DOC-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del documento',
    version_row         INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC). DECISION_DDL-DOC-01.',
    id_acta             BIGINT         NOT NULL               COMMENT 'FK al acta a la que pertenece el documento',
    id_plantilla        BIGINT         NULL                   COMMENT 'FK a la plantilla documental origen (nullable para adjuntos)',
    tipo_docu           SMALLINT       NOT NULL               COMMENT 'Tipo de documento (TipoDocu enum)',
    estado_docu         SMALLINT       NOT NULL               COMMENT 'Estado: 1=BORRADOR, 2=PENDIENTE_FIRMA, 3=FIRMADO, 4=EMITIDO, 5=ANULADO, 6=REEMPLAZADO, 7=ADJUNTO',
    nro_docu            VARCHAR(30)    NULL                   COMMENT 'Número asignado al documento (nullable antes de numerar)',
    storage_key         VARCHAR(196)   NULL                   COMMENT 'Clave técnica interna generada por backend. Formato: faltas/actas/<uuid-acta>/documentos/<uuid-documento>.pdf. No es URL pública. Nullable hasta emitir. HUMAN_DECISION_CLOSED.',
    hash_docu           VARCHAR(128)   NULL                   COMMENT 'Hash del artefacto PDF emitido (nullable hasta emitir)',
    tipo_firma_req      SMALLINT       NOT NULL DEFAULT 1     COMMENT 'Tipo de firma requerida (TipoFirmaReq; 1=NO_REQUIERE)',
    id_talonario        BIGINT         NULL                   COMMENT 'FK al talonario del número asignado (nullable)',
    nro_talonario_usado INT            NULL                   COMMENT 'Número de talonario consumido (nullable)',
    fh_generacion       DATETIME(6)    NULL                   COMMENT 'Fecha-hora de emisión formal del documento (fhGeneracion)',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de creación del registro (fechaGeneracion)',
    id_user_alta        CHAR(36)       NULL                   COMMENT 'UUID del usuario que creó el documento',
    PRIMARY KEY (id),
    CONSTRAINT fk_documento_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documento_plantilla FOREIGN KEY (id_plantilla)
        REFERENCES fal_documento_plantilla (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documento_talonario FOREIGN KEY (id_talonario)
        REFERENCES num_talonario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_documento_acta (id_acta),
    INDEX ix_documento_estado (estado_docu)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Documentos del expediente de faltas. OCC. DECISION_DDL-DOC-01.';

-- ----------------------------------------------------------------------------
-- 47. fal_acta_valorizacion_item
--     Ítems de detalle de una valorización (append-only tras confirmación del padre).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_valorizacion_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del ítem de valorización',
    id_valorizacion     BIGINT          NOT NULL               COMMENT 'FK a la valorización que contiene este ítem',
    id_articulo         BIGINT          NULL                   COMMENT 'FK al artículo normativo valorizado (nullable si ítem manual)',
    tipo_item           SMALLINT        NOT NULL               COMMENT 'Tipo de ítem (TipoValorizacionItem enum)',
    descripcion         VARCHAR(255)    NULL                   COMMENT 'Descripción del ítem (libre si manual)',
    cantidad_unidades   DECIMAL(10,4)   NOT NULL               COMMENT 'Cantidad de unidades del artículo',
    tipo_unidad_faltas  SMALLINT        NOT NULL               COMMENT 'Tipo de unidad de faltas (TipoUnidadFaltas enum)',
    valor_unitario      DECIMAL(14,4)   NOT NULL               COMMENT 'Valor por unidad aplicado en esta valorización',
    importe             DECIMAL(14,2)   NOT NULL               COMMENT 'Importe = cantidad_unidades × valor_unitario (escala 2)',
    tarifario_unidad_id BIGINT          NULL                   COMMENT 'FK al tarifario unitario que proveyó el valor (nullable si manual)',
    origen_valorizacion SMALLINT        NOT NULL               COMMENT 'Origen de este ítem (OrigenValorizacion enum)',
    criterio_tarifario  SMALLINT        NULL                   COMMENT 'Criterio tarifario específico del ítem (nullable)',
    si_manual           BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = valor ingresado manualmente (sobrescritura)',
    fh_alta             DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que creó el ítem',
    PRIMARY KEY (id),
    CONSTRAINT fk_val_item_valorizacion FOREIGN KEY (id_valorizacion)
        REFERENCES fal_acta_valorizacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_val_item_articulo FOREIGN KEY (id_articulo)
        REFERENCES fal_articulo_normativa_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_val_item_tarifario FOREIGN KEY (tarifario_unidad_id)
        REFERENCES fal_tarifario_unidad_faltas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_val_item_valorizacion (id_valorizacion)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Ítems de detalle de una valorización. Append-only tras confirmación del padre.';

-- =============================================================================
-- SECCIÓN 07 — G7: Documentos y asociados (tablas 48–53)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 48. fal_acta_documento
--     Tabla pivote de pertenencia funcional entre acta y documento.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_documento (
    id_acta         BIGINT         NOT NULL               COMMENT 'FK al acta (parte de la PK compuesta)',
    id_documento    BIGINT         NOT NULL               COMMENT 'FK al documento (parte de la PK compuesta)',
    rol_docu_acta   SMALLINT       NOT NULL               COMMENT 'Rol del documento en el acta (RolDocuActa enum; 12 valores)',
    si_principal    BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = documento principal del acta para su tipo',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que asoció el documento al acta',
    PRIMARY KEY (id_acta, id_documento),
    CONSTRAINT fk_acta_docu_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_acta_docu_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_acta_docu_documento (id_documento)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Tabla pivote de pertenencia funcional entre actas y documentos. FalActaSnapshot.idDocuUlt se deriva de aquí.';

-- ----------------------------------------------------------------------------
-- 49. fal_documento_firma_req
--     Snapshot de requisito de firma para un documento concreto.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_firma_req (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del requisito',
    id_documento        BIGINT         NOT NULL               COMMENT 'FK al documento que tiene este requisito',
    seq_firma_req       SMALLINT       NOT NULL               COMMENT 'Secuencia del requisito dentro del documento (único por documento)',
    rol_firma_req       SMALLINT       NOT NULL               COMMENT 'Rol de firma que debe satisfacerse',
    mecanismo_firma_req SMALLINT       NULL                   COMMENT 'Mecanismo de firma requerido (NULL = sin restricción)',
    orden_firma         SMALLINT       NULL                   COMMENT 'Orden si hay secuencia obligatoria (nullable)',
    estado_firma_req    SMALLINT       NOT NULL DEFAULT 1     COMMENT '1=PENDIENTE, 2=FIRMADO (EstadoFirmaReq enum)',
    si_obligatoria      BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = requisito obligatorio para emitir',
    si_activa           BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = requisito vigente',
    id_firmante_asig    BIGINT         NULL                   COMMENT 'ID del firmante asignado a este requisito (nullable)',
    ver_firmante_asig   SMALLINT       NULL                   COMMENT 'Versión del firmante asignado',
    fh_asignacion       DATETIME(6)    NULL                   COMMENT 'Fecha-hora de asignación del firmante',
    fh_firma            DATETIME(6)    NULL                   COMMENT 'Fecha-hora en que se firmó (NULL = no firmado)',
    id_firma            BIGINT         NULL                   COMMENT 'FK al registro de firma real (fal_documento_firma; nullable hasta firmar)',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó el requisito',
    PRIMARY KEY (id),
    UNIQUE KEY uq_firma_req_doc_seq (id_documento, seq_firma_req),
    CONSTRAINT fk_firma_req_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_firma_req_documento (id_documento)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Snapshot de requisitos de firma para documentos concretos. Inmutable respecto a cambios en la plantilla.';

-- ----------------------------------------------------------------------------
-- 50. fal_documento_redaccion
--     Versiones de redacción combinada de un documento desde su plantilla.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_redaccion (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    version_row                 INT            NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Control de concurrencia optimista. USO: OCC. REGLA: incrementar en cada modificacion.',
    id_documento                BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al documento redactado. USO: vinculo con el artefacto. REGLA: referencia fal_documento(id).',
    id_plantilla_contenido      BIGINT         NULL                   COMMENT 'CONTENIDO: FK a la version de contenido de plantilla usada. USO: congela la version exacta. REGLA: nullable si redaccion es manual.',
    nro_revision                SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Numero de revision de la redaccion. USO: ordena revisiones por documento. REGLA: unico por documento; positivo.',
    redaccion_origen_id         BIGINT         NULL                   COMMENT 'CONTENIDO: FK autorreferencial a la redaccion origen. USO: trazabilidad de reaperturas. REGLA: NULL = primera revision.',
    estado_redaccion            SMALLINT       NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Estado de la redaccion. USO: control del ciclo de vida. VALORES: 1=BORRADOR, 2=CONFIRMADA, 3=ANULADA (EstadoRedaccionDocumento).',
    contenido_editable_markdown TEXT           NOT NULL               COMMENT 'CONTENIDO: Texto editable con variables sustituidas y edicion manual. USO: artefacto de redaccion. REGLA: no puede ser vacio; conserva cambios manuales.',
    variables_snapshot_json     JSON           NOT NULL               COMMENT 'CONTENIDO: Snapshot JSON de valores de variables usados. USO: congela los valores en el momento de la combinacion. REGLA: no puede ser NULL; formato [{nombre, valor}].',
    fh_creacion                 DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp de creacion de la redaccion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_creacion            CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_edicion                  DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima edicion manual. USO: auditoria. REGLA: nullable; actualizar en cada edicion.',
    id_user_edicion             CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario de la ultima edicion. USO: auditoria. REGLA: nullable.',
    fh_confirmacion             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de confirmacion. USO: auditoria. REGLA: NULL = no confirmada aun.',
    id_user_confirmacion        CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que confirmo. USO: auditoria. REGLA: nullable.',
    fh_anulacion                DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de anulacion. USO: auditoria. REGLA: NULL = no anulada.',
    id_user_anulacion           CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que anulo. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_redaccion_doc_rev (id_documento, nro_revision),
    CONSTRAINT fk_redaccion_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_redaccion_plantilla_cont FOREIGN KEY (id_plantilla_contenido)
        REFERENCES fal_documento_plantilla_contenido (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_redaccion_origen FOREIGN KEY (redaccion_origen_id)
        REFERENCES fal_documento_redaccion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_redaccion_estado CHECK (estado_redaccion BETWEEN 1 AND 3),
    INDEX ix_redaccion_documento (id_documento)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Versiones de redacción combinada de documentos. UK (id_documento, nro_revision). Self-ref para origen.';

-- ----------------------------------------------------------------------------
-- 51. fal_notificacion
--     Proceso notificatorio de un documento del expediente.
--     OCC. (DECISION_DDL-NOTI-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_notificacion (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    version_row             INT            NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Control de concurrencia optimista. USO: OCC. REGLA: incrementar en cada modificacion (DECISION_DDL-NOTI-01).',
    id_acta                 BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al acta notificada. USO: acceso desde el expediente. REGLA: referencia fal_acta(id).',
    id_documento            BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al documento notificado. USO: acceso al artefacto. REGLA: referencia fal_documento(id).',
    tipo_docu_notificado    SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo del documento notificado. USO: discrimina el tipo en bandeja. REGLA: TipoDocu enum.',
    canal                   SMALLINT       NULL                   COMMENT 'CONTENIDO: Canal de notificacion. USO: determina el proceso de envio. VALORES: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL (CanalNotificacion enum). REGLA: NULL en PENDIENTE_ENVIO; NOT NULL tras iniciarEnvio.',
    fecha_envio             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de inicio del envio. USO: auditoria. REGLA: NULL en PENDIENTE_ENVIO.',
    estado                  SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado del proceso notificatorio. USO: control del ciclo de vida. REGLA: EstadoNotificacion enum.',
    resultado               SMALLINT       NULL                   COMMENT 'CONTENIDO: Resultado del proceso. USO: resolucion final. REGLA: ResultadoNotificacion enum; NULL hasta resolucion.',
    fecha_resultado         DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp del resultado. USO: auditoria. REGLA: NULL si sin resultado aun.',
    intentos                INT            NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Cantidad de intentos realizados. USO: control de reintentos. REGLA: incrementa con cada intento.',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de creacion. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario creador. USO: auditoria. REGLA: UUID del actor.',
    fh_ult_mod              DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima modificacion. USO: auditoria. REGLA: nullable.',
    id_user_ult_mod         CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario de la ultima mod. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    CONSTRAINT fk_notificacion_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_notificacion_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_notificacion_acta (id_acta),
    INDEX ix_notificacion_documento (id_documento)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Proceso notificatorio de documentos del expediente. OCC. DECISION_DDL-NOTI-01.';

-- ----------------------------------------------------------------------------
-- 52. num_talonario_movimiento
--     Log append-only de movimientos de numeración en un talonario.
-- ----------------------------------------------------------------------------
CREATE TABLE num_talonario_movimiento (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_talonario        BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al talonario. USO: enlaza al talonario propietario. REGLA: referencia num_talonario(id).',
    nro_talonario       INT            NOT NULL               COMMENT 'CONTENIDO: Numero del talonario afectado. USO: identifica el numero dentro del talonario. REGLA: > 0; UNIQUE con id_talonario.',
    estado_numero       SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado del numero. VALORES: 1=USADO, 2=ANULADO, 3=DEVUELTO_SIN_USAR, 4=RENDIDO, 5=JUSTIFICADO (EstadoNumeroTalonario enum).',
    motivo_anulacion    SMALLINT       NULL                   COMMENT 'CONTENIDO: Motivo de la anulacion. REGLA: obligatorio si estado_numero=ANULADO. VALORES: MotivoAnulacionTalonario enum.',
    acta_id             BIGINT         NULL                   COMMENT 'CONTENIDO: FK al acta que uso el numero. REGLA: obligatorio si fue usado para acta; nullable.',
    documento_id        BIGINT         NULL                   COMMENT 'CONTENIDO: FK al documento numerado. REGLA: obligatorio si fue usado para documento; nullable.',
    id_dep              BIGINT         NULL                   COMMENT 'CONTENIDO: ID de la dependencia emisora. USO: trazabilidad. REGLA: nullable.',
    ver_dep             SMALLINT       NULL                   COMMENT 'CONTENIDO: Version de la dependencia emisora. REGLA: obligatorio si id_dep presente.',
    id_insp             BIGINT         NULL                   COMMENT 'CONTENIDO: ID del inspector emisor. USO: trazabilidad. REGLA: nullable.',
    ver_insp            SMALLINT       NULL                   COMMENT 'CONTENIDO: Version del inspector emisor. REGLA: obligatorio si id_insp presente.',
    fh_movimiento       DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp funcional del movimiento. USO: trazabilidad. REGLA: suministrado por FaltasClock.',
    id_user_movimiento  CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del usuario o proceso. USO: auditoria. REGLA: UUID; no hay fh_alta/id_user_alta separados.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_tal_mov_nro (id_talonario, nro_talonario),
    CONSTRAINT fk_tal_mov_talonario FOREIGN KEY (id_talonario)
        REFERENCES num_talonario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tal_mov_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tal_mov_documento FOREIGN KEY (documento_id)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_tal_mov_nro CHECK (nro_talonario > 0),
    INDEX ix_tal_mov_talonario (id_talonario),
    INDEX ix_tal_mov_nro (id_talonario, nro_talonario)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Log append-only de movimientos de numeración en talonarios (emisiones y anulaciones)';

-- ----------------------------------------------------------------------------
-- 53. fal_acta_fallo
--     Fallo dictado sobre un acta de faltas. OCC.
--     Firmeza inline (DECISION_DDL-RF-005 / DECISION_DDL-ENUM-01 CERRADAS).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_fallo (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del fallo',
    version_row         INT             NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    id_acta             BIGINT          NOT NULL               COMMENT 'FK al acta fallada',
    tipo_fallo          SMALLINT        NOT NULL               COMMENT 'Tipo: 1=ABSOLUTORIO, 2=CONDENATORIO (TipoFalloActa)',
    estado_fallo        SMALLINT        NOT NULL               COMMENT '1=PENDIENTE_FIRMA, 2=PENDIENTE_NOTIFICACION, 3=NOTIFICADO, 4=FIRME, 5=REEMPLAZADO, 6=SIN_EFECTO',
    resultado_fallo     SMALLINT        NULL                   COMMENT 'Resultado del fallo (ResultadoFalloActa; nullable hasta dictar)',
    monto_condena       DECIMAL(14,2)   NULL                   COMMENT 'Monto de la condena (solo condenatorio; nullable)',
    fundamentos         TEXT            NULL                   COMMENT 'Fundamentos del fallo',
    documento_id        BIGINT          NULL                   COMMENT 'FK al documento del fallo (nullable hasta emitir)',
    valorizacion_id     BIGINT          NULL                   COMMENT 'FK a la valorización que sustenta la condena (nullable)',
    fallo_reemplazado_id BIGINT         NULL                   COMMENT 'FK auto-referencial al fallo que reemplaza (nullable)',
    fh_dictado          DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora de dictado del fallo',
    id_user_dictado     CHAR(36)        NULL                   COMMENT 'UUID del usuario que dictó el fallo',
    fh_firma            DATETIME(6)     NULL                   COMMENT 'Fecha-hora de firma del fallo (NULL = no firmado)',
    fh_notificacion     DATETIME(6)     NULL                   COMMENT 'Fecha-hora de notificación del fallo (NULL = no notificado)',
    fh_vto_apelacion    DATE            NULL                   COMMENT 'Fecha de vencimiento del plazo de apelación (NULL = sin plazo)',
    fh_firmeza          DATETIME(6)     NULL                   COMMENT 'Fecha-hora de firmeza de la condena (NULL = no firme)',
    origen_firmeza      SMALLINT        NULL                   COMMENT '1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA (OrigenFirmezaCondena)',
    si_apelable         BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = el fallo puede ser apelado',
    si_firme            BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = la condena tiene firmeza declarada',
    si_vigente          BOOLEAN         NOT NULL DEFAULT TRUE  COMMENT 'TRUE = fallo activo; FALSE = reemplazado',
    fh_alta             DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta        CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que creó el fallo',
    PRIMARY KEY (id),
    CONSTRAINT fk_fallo_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_fallo_documento FOREIGN KEY (documento_id)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_fallo_valorizacion FOREIGN KEY (valorizacion_id)
        REFERENCES fal_acta_valorizacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_fallo_reemplazado FOREIGN KEY (fallo_reemplazado_id)
        REFERENCES fal_acta_fallo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_fallo_estado CHECK (estado_fallo BETWEEN 1 AND 6),
    CONSTRAINT chk_fallo_tipo CHECK (tipo_fallo IN (1, 2)),
    CONSTRAINT chk_fallo_firmeza CHECK (origen_firmeza IS NULL OR origen_firmeza IN (1, 2)),
    INDEX ix_fallo_acta (id_acta),
    INDEX ix_fallo_acta_vigente (id_acta, si_vigente)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Fallo dictado sobre el acta. Firmeza inline (si_firme, fh_firmeza, origen_firmeza). No existe fal_acta_firmeza_condena. DECISION_DDL-RF-005.';

-- =============================================================================
-- SECCIÓN 08 — G8: Firma, apelación y obligación de pago (tablas 54–59)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 54. fal_documento_firma
--     Registro de firma real efectuada sobre un documento.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_documento_firma (
    id                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_documento         BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al documento firmado. USO: vinculo con el artefacto. REGLA: referencia fal_documento(id).',
    seq_firma_req        SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Secuencia del requisito de firma satisfecho. USO: asocia la firma con el requisito. REGLA: referencia fal_documento_firma_req.seq_firma_req.',
    id_firmante          BIGINT         NULL                   COMMENT 'CONTENIDO: FK al firmante del catalogo (parte de FK compuesta). USO: identidad del firmante institucional. REGLA: NULL si firma externa sin registro en catalogo (FalDocumentoFirma.idFirmante Long).',
    ver_firmante         SMALLINT       NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Version del firmante. USO: complementa FK compuesta. REGLA: 0 si id_firmante IS NULL; referencia fal_firmante_version si id_firmante NOT NULL.',
    id_user_firma        CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del usuario que realizo la firma (IDP). USO: auditoria de identidad. REGLA: nullable; puede diferir de id_user_alta (JAVA: FalDocumentoFirma.idUserFirma).',
    rol_firmante         SMALLINT       NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Rol de firma satisfecho. USO: vincula el requisito satisfecho con la firma. REGLA: segun RolFirmaReq enum; 0 si no aplica (JAVA: FalDocumentoFirma.rolFirmante short).',
    nombre_firmante      VARCHAR(48)    NULL                   COMMENT 'CONTENIDO: Nombre snapshot del firmante al momento de la firma. USO: auditoria. REGLA: nullable; copia de fal_firmante.nom_firmante al firmar (JAVA: FalDocumentoFirma.nombreFirmante).',
    tipo_firma           SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de firma utilizada. USO: determina el mecanismo. VALORES: segun TipoFirma enum.',
    estado_firma         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado de la firma. USO: ciclo de vida. VALORES: segun EstadoFirma enum.',
    hash_firma           VARCHAR(128)   NULL                   COMMENT 'CONTENIDO: Hash del documento firmado (SHA-256 hex del artefacto previo a la firma). USO: integridad. REGLA: nullable (JAVA: FalDocumentoFirma.hashDocumento — equivalencia de naming cerrada: hash_firma en DDL corresponde a hashDocumento en Java).',
    referencia_firma_ext VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Referencia de transaccion/sobre del proveedor de firma externo. USO: idempotencia y reconciliacion. REGLA: nullable para firma interna (JAVA: referenciaFirmaExt).',
    storage_key          VARCHAR(196)   NULL                   COMMENT 'CONTENIDO: Clave de almacenamiento del artefacto de firma. USO: acceso al artefacto. REGLA: clave tecnica generada por backend; nullable si sin artefacto.',
    mensaje_error        VARCHAR(512)   NULL                   COMMENT 'CONTENIDO: Mensaje de error si la firma fallo. USO: diagnostico. REGLA: nullable; solo informado si la firma fallo (JAVA: FalDocumentoFirma.mensajeError).',
    fh_firma             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la firma efectiva. USO: auditoria. REGLA: nullable si aun no firmado.',
    fh_alta              DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta         CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del actor que registro la firma. USO: auditoria. REGLA: UUID.',
    PRIMARY KEY (id),
    CONSTRAINT fk_firma_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_firma_firmante_ver FOREIGN KEY (id_firmante, ver_firmante)
        REFERENCES fal_firmante_version (id_firmante, ver_firmante) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_firma_documento (id_documento),
    INDEX ix_firma_firmante (id_firmante)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Registro de firmas reales sobre documentos del expediente. USO: auditoria de cada firma efectuada. REGLAS CLAVE: id_firmante NULL (firma externa); rol_firmante SMALLINT NOT NULL (RolFirmaReq enum); nombre_firmante snapshot del firmante; mensaje_error para diagnostico. JAVA: FalDocumentoFirma.';

-- ----------------------------------------------------------------------------
-- 55. fal_notificacion_intento
--     Intentos individuales de envío de una notificación.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_notificacion_intento (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_notificacion     BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK a la notificacion. USO: agrupacion de intentos por notificacion. REGLA: referencia fal_notificacion(id).',
    nro_intento         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Numero de intento (positivo; unico por notificacion). USO: ordena los intentos. REGLA: positivo; UNIQUE(id_notificacion, nro_intento).',
    canal_notif         SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Canal usado en este intento. USO: auditoria del canal efectivo. VALORES: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL (CanalNotificacion enum; SPEC-MODEL-DDL-CLOSURE-001).',
    estado_intento      SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado del intento de envio. USO: control del ciclo de vida. VALORES: segun EstadoNotificacion enum.',
    resultado_intento   SMALLINT       NULL                   COMMENT 'CONTENIDO: Resultado del intento. USO: resolucion del intento. VALORES: segun ResultadoNotificacion enum; NULL hasta que haya resultado.',
    domicilio_notif_id  BIGINT         NULL                   COMMENT 'CONTENIDO: FK al domicilio de notificacion usado. USO: trazabilidad del destino postal. REGLA: nullable.',
    destino_digital     VARCHAR(120)   NULL                   COMMENT 'CONTENIDO: Destino digital del intento (email, portal). USO: registro del canal digital. REGLA: nullable; max 120 caracteres.',
    lote_id             BIGINT         NULL                   COMMENT 'CONTENIDO: FK al lote de correo (si aplica). USO: agrupacion en lote de envio. REGLA: nullable.',
    referencia_externa  VARCHAR(80)    NULL                   COMMENT 'CONTENIDO: Referencia externa del sistema de envio. USO: idempotencia e integracion. REGLA: nullable; max 80 caracteres.',
    fh_intento          DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp del intento de envio. USO: timeline de auditoria. REGLA: obligatorio.',
    fh_resultado        DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp del resultado del intento. USO: auditoria. REGLA: NULL hasta que haya resultado.',
    fh_alta             DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta        CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del actor que registro el intento. USO: auditoria. REGLA: UUID.',
    fh_ult_mod          DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de la ultima modificacion. USO: auditoria. REGLA: nullable; se actualiza al registrar resultado.',
    id_user_ult_mod     CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del actor de la ultima modificacion. USO: auditoria. REGLA: nullable.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_noti_intento_nro (id_notificacion, nro_intento),
    CONSTRAINT fk_noti_intento_notificacion FOREIGN KEY (id_notificacion)
        REFERENCES fal_notificacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_noti_intento_lote FOREIGN KEY (lote_id)
        REFERENCES fal_lote_correo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_noti_intento_nro CHECK (nro_intento > 0),
    INDEX ix_noti_intento_notificacion (id_notificacion)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Intentos individuales de envio de una notificacion. USO: historial de reintentos y resultado por intento. REGLAS CLAVE: canal_notif SMALLINT (SPEC-MODEL-DDL-CLOSURE-001); estado_intento + resultado_intento separados; lote_id FK fal_lote_correo.';

-- ----------------------------------------------------------------------------
-- 56. fal_notificacion_acuse
--     Acuses de recibo de notificaciones.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_notificacion_acuse (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    id_notificacion BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK a la notificacion acusada. USO: vinculo con el proceso notificatorio. REGLA: referencia fal_notificacion(id).',
    intento_id      BIGINT         NULL                   COMMENT 'CONTENIDO: FK al intento de notificacion que origino el acuse. USO: trazabilidad del intento. REGLA: nullable (SPEC-MODEL-DDL-CLOSURE-001).',
    tipo_acuse      SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Tipo de acuse recibido. USO: clasifica el acuse. VALORES: segun TipoAcuse enum.',
    canal           SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Canal por el que se recibio el acuse. USO: auditoria del canal. VALORES: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL (CanalNotificacion enum; SPEC-MODEL-DDL-CLOSURE-001: NOT NULL).',
    estado_acuse    SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Estado del acuse. USO: seguimiento del ciclo de vida. VALORES: segun EstadoAcuse enum (SPEC-MODEL-DDL-CLOSURE-001: NOT NULL).',
    storage_key     VARCHAR(196)   NULL                   COMMENT 'CONTENIDO: Clave de almacenamiento del artefacto del acuse. USO: acceso al artefacto (ej: escaneo del sobre). REGLA: clave tecnica generada por backend; nullable si sin artefacto (SPEC-MODEL-DDL-CLOSURE-001: VARCHAR(196)).',
    fh_acuse        DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp del acuse de recibo. USO: auditoria. REGLA: nullable (SPEC-MODEL-DDL-CLOSURE-001: NULL).',
    fh_alta         DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Timestamp tecnico de alta. USO: auditoria. REGLA: suministrado por FaltasClock.',
    id_user_alta    CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID del actor que registro el acuse. USO: auditoria. REGLA: UUID.',
    PRIMARY KEY (id),
    CONSTRAINT fk_noti_acuse_notificacion FOREIGN KEY (id_notificacion)
        REFERENCES fal_notificacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_noti_acuse_intento FOREIGN KEY (intento_id)
        REFERENCES fal_notificacion_intento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_noti_acuse_notificacion (id_notificacion)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Acuses de recibo de notificaciones del expediente. USO: registro del resultado efectivo del proceso notificatorio. REGLAS CLAVE: canal NOT NULL, estado_acuse NOT NULL, fh_acuse NULL, intento_id FK nullable. SPEC-MODEL-DDL-CLOSURE-001.';

-- ----------------------------------------------------------------------------
-- 57. fal_acta_evento
--     Log append-only de eventos del expediente (timeline de dominio).
--     Inmutable. Sin payload JSON (DECISION_DDL no usada, confirmado Javadoc).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_evento (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del evento (usado para desempate en timeline)',
    acta_id             BIGINT         NOT NULL               COMMENT 'FK al acta del expediente',
    tipo_evt            CHAR(6)        NOT NULL               COMMENT 'Código estable del tipo de evento (TipoEventoActa; CHAR(6), no SMALLINT)',
    origen_evt          SMALLINT       NOT NULL               COMMENT 'Origen del evento (OrigenEvento enum)',
    fh_evt              DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora del evento (clave de ordenamiento junto con id)',
    bloque_func         CHAR(4)        NULL                   COMMENT 'Bloque procesal en que ocurrió el evento (BloqueActual)',
    est_proc_ant        CHAR(4)        NULL                   COMMENT 'Estado procesal anterior al evento',
    est_proc_nvo        CHAR(4)        NULL                   COMMENT 'Nuevo estado procesal tras el evento',
    sit_adm_ant         CHAR(4)        NULL                   COMMENT 'Situación administrativa anterior al evento',
    sit_adm_nva         CHAR(4)        NULL                   COMMENT 'Nueva situación administrativa tras el evento',
    actor_tipo          SMALLINT       NULL                   COMMENT 'CONTENIDO: Tipo de actor del evento. USO: identifica la naturaleza del actor. REGLA: ActorTipoEvento enum; nullable.',
    actor_id            CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID del actor del evento (inspector, usuario, sistema). USO: trazabilidad del actor. REGLA: nullable.',
    id_docu_rel         BIGINT         NULL                   COMMENT 'CONTENIDO: FK al documento relacionado con el evento. USO: vinculo con artefacto documental. REGLA: nullable.',
    id_notif_rel        BIGINT         NULL                   COMMENT 'FK a la notificación relacionada (nullable)',
    id_pres_rel         BIGINT         NULL                   COMMENT 'ID de la presentación relacionada (nullable; sin FK a tabla propia)',
    id_user_evt         CHAR(36)       NULL                   COMMENT 'UUID del usuario que originó el evento',
    si_evt_cierre       BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = evento de cierre del expediente',
    si_evt_ext          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = evento generado por sistema externo',
    si_permite_reing    BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: Este evento habilita reingreso si hay archivo activo. USO: logica de reingreso. REGLA: TRUE = permite reingreso.',
    correlacion_id      CHAR(36)       NULL                   COMMENT 'CONTENIDO: UUID de correlacion para idempotencia de comandos externos. USO: evitar duplicados de eventos externos. REGLA: UUID; nullable para eventos internos.',
    PRIMARY KEY (id),
    CONSTRAINT fk_evento_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_evento_docu_rel FOREIGN KEY (id_docu_rel)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_evento_notif_rel FOREIGN KEY (id_notif_rel)
        REFERENCES fal_notificacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_evento_acta_timeline (acta_id, fh_evt, id),
    INDEX ix_evento_tipo (tipo_evt),
    INDEX ix_evento_correlacion (correlacion_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Log append-only de eventos del expediente. Orden timeline: fh_evt + id. Sin payload JSON. Inmutable.';

-- ----------------------------------------------------------------------------
-- 58. fal_acta_apelacion
--     Apelaciones presentadas contra fallos del expediente. OCC.
--     (DECISION_DDL-ENUM-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_apelacion (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la apelación',
    version_row             INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    id_acta                 BIGINT         NOT NULL               COMMENT 'FK al acta apelada',
    fallo_id                BIGINT         NOT NULL               COMMENT 'FK al fallo apelado',
    estado_apelacion        SMALLINT       NOT NULL               COMMENT '1=PRESENTADA, 2=EN_ANALISIS, 3=RECHAZADA, 4=ACEPTADA_ABSUELVE, 5=RESUELTA, 6=SIN_EFECTO',
    fecha_presentacion      DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora de presentación de la apelación',
    canal_apelacion         SMALLINT       NOT NULL               COMMENT 'Canal de presentación (CanalApelacion enum)',
    tipo_presentacion       SMALLINT       NOT NULL               COMMENT 'Tipo de presentación (TipoPresentacion enum)',
    texto_apelacion         TEXT           NULL                   COMMENT 'CONTENIDO: Texto de la apelacion presentada. USO: contenido juridico. REGLA: nullable; contenido juridico preservado.',
    presentante             VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Nombre del presentante de la apelacion. USO: identificacion del actor. REGLA: max 64 caracteres; nullable.',
    fundamentos             TEXT           NULL                   COMMENT 'CONTENIDO: Fundamentos de la apelacion. USO: contenido juridico. REGLA: nullable; preservado como contenido juridico.',
    si_activa               BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'CONTENIDO: Estado de actividad. USO: baja logica. REGLA: FALSE = apelacion sin efecto.',
    fecha_resolucion        DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Timestamp de resolucion. USO: auditoria. REGLA: NULL = sin resolver aun.',
    fundamentos_resolucion  TEXT           NULL                   COMMENT 'CONTENIDO: Fundamentos de la resolucion de la apelacion. USO: contenido juridico. REGLA: nullable; preservado como contenido juridico.',
    documento_resolucion_id BIGINT         NULL                   COMMENT 'CONTENIDO: FK al documento de resolucion. USO: vinculo con el artefacto de resolucion. REGLA: nullable.',
    resultado_resolucion    SMALLINT       NULL                   COMMENT 'Resultado de la resolución (ResultadoResolucionApelacion enum)',
    fh_alta                 DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta            CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que registró la apelación',
    fh_mod                  DATETIME(6)    NULL                   COMMENT 'Fecha-hora de la última modificación',
    id_user_mod             CHAR(36)       NULL                   COMMENT 'UUID del usuario que realizó la última modificación',
    PRIMARY KEY (id),
    CONSTRAINT fk_apelacion_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_apelacion_fallo FOREIGN KEY (fallo_id)
        REFERENCES fal_acta_fallo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_apelacion_doc_resolucion FOREIGN KEY (documento_resolucion_id)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_apelacion_estado CHECK (estado_apelacion BETWEEN 1 AND 6),
    INDEX ix_apelacion_acta (id_acta),
    INDEX ix_apelacion_fallo (fallo_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Apelaciones presentadas contra fallos. OCC. DECISION_DDL-ENUM-01.';

-- ----------------------------------------------------------------------------
-- 59. fal_acta_obligacion_pago
--     Cabecera de obligación de pago del acta. OCC.
--     forma_pago_vigente_id se agrega vía ALTER TABLE post-creación de fal_acta_forma_pago.
--     Columna generada acta_id_vigente para unicidad de obligación vigente por acta.
--     (DECISION_DDL-PAGO-03 / DECISION_DDL-SNAP-02 CERRADAS)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_obligacion_pago (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la obligación de pago',
    version_row                 INT             NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    acta_id                     BIGINT          NOT NULL               COMMENT 'FK al acta que originó la obligación',
    persona_id                  BIGINT          NOT NULL               COMMENT 'FK a la persona obligada (infractor)',
    tipo_obligacion             SMALLINT        NOT NULL               COMMENT '1=PAGO_VOLUNTARIO, 2=CONDENA (TipoObligacionPago)',
    origen_obligacion           SMALLINT        NOT NULL DEFAULT 1     COMMENT '1=FALTAS, 2=APREMIO, 3=JUZGADO (OrigenObligacionPago). DECISION_DDL-PAGO-03.',
    obligacion_reemplazada_id   BIGINT          NULL                   COMMENT 'FK auto-referencial a la obligación reemplazada. DECISION_DDL-PAGO-03.',
    valorizacion_id             BIGINT          NULL                   COMMENT 'FK a la valorización que sustenta la obligación (nullable)',
    fallo_id                    BIGINT          NULL                   COMMENT 'FK al fallo que generó la condena (solo CONDENA; nullable)',
    monto_original              DECIMAL(14,2)   NOT NULL               COMMENT 'Monto original de la obligación al momento de determinarla (escala 2)',
    estado_obligacion           SMALLINT        NOT NULL               COMMENT '1=DETERMINADA, 2=PENDIENTE_FORMA_PAGO, 3=CON_FORMA_PAGO_VIGENTE, 4=CANCELADA_POR_PAGO, 5=REEMPLAZADA, 6=DEJADA_SIN_EFECTO',
    fh_determinacion            DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora de determinación de la obligación',
    id_user_determinacion       CHAR(36)        NULL                   COMMENT 'UUID del usuario que determinó la obligación',
    forma_pago_vigente_id       BIGINT          NULL                   COMMENT 'FK a la forma de pago vigente (nullable; FK agregada vía ALTER TABLE)',
    fh_cancelacion              DATETIME(6)     NULL                   COMMENT 'Fecha-hora de cancelación total (NULL = no cancelada)',
    si_excluir_escaneo          BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = excluir de procesos de escaneo de recibos',
    fh_ult_sync_ingresos        DATETIME(6)     NULL                   COMMENT 'Fecha-hora de última sincronización con Ingresos',
    si_vigente                  BOOLEAN         NOT NULL               COMMENT 'TRUE = obligación vigente (solo una vigente por acta en todo momento)',
    -- columna generada para unicidad de la obligación vigente
    acta_id_vigente             BIGINT          GENERATED ALWAYS AS (IF(si_vigente = TRUE, acta_id, NULL)) PERSISTENT COMMENT 'Generada: acta_id si vigente, NULL si no. Garantiza unicidad de obligación vigente por acta.',
    fh_alta                     DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta                CHAR(36)        NOT NULL               COMMENT 'UUID del usuario que creó la obligación',
    PRIMARY KEY (id),
    UNIQUE KEY uq_oblig_pago_vigente (acta_id_vigente),
    CONSTRAINT fk_oblig_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_oblig_persona FOREIGN KEY (persona_id)
        REFERENCES fal_persona (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_oblig_valorizacion FOREIGN KEY (valorizacion_id)
        REFERENCES fal_acta_valorizacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_oblig_fallo FOREIGN KEY (fallo_id)
        REFERENCES fal_acta_fallo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_oblig_reemplazada FOREIGN KEY (obligacion_reemplazada_id)
        REFERENCES fal_acta_obligacion_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_oblig_tipo CHECK (tipo_obligacion IN (1, 2)),
    CONSTRAINT chk_oblig_origen CHECK (origen_obligacion BETWEEN 1 AND 3),
    CONSTRAINT chk_oblig_estado CHECK (estado_obligacion BETWEEN 1 AND 6),
    CONSTRAINT chk_oblig_monto CHECK (monto_original >= 0),
    INDEX ix_oblig_acta (acta_id),
    INDEX ix_oblig_persona (persona_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Cabecera de obligación de pago. OCC. Columna generada acta_id_vigente garantiza unicidad de obligación vigente. DECISION_DDL-PAGO-03.';

-- =============================================================================
-- SECCIÓN 09 — G9: Snapshot, proyecciones parciales y forma de pago (tablas 60–62)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 60. fal_acta_snapshot
--     Proyección operativa resumida del expediente (1:1 con acta).
--     OCC. (DECISION_DDL-SNAP-01 / DECISION_DDL-SNAP-02 CERRADAS)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_snapshot (
    id_acta                         BIGINT         NOT NULL               COMMENT 'PK y FK al acta (relación 1:1)',
    version_row                     INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista. DECISION_DDL-SNAP-01.',
    -- estado procesal
    bloque_actual                   CHAR(4)        NULL                   COMMENT 'Bloque procesal actual (BloqueActual; NULL hasta primer cálculo)',
    est_proc_act                    CHAR(4)        NULL                   COMMENT 'Estado procesal actual (EstadoProcesalActa)',
    sit_adm_act                     CHAR(4)        NULL                   COMMENT 'Situación administrativa actual (SituacionAdministrativaActa)',
    resultado_final                 SMALLINT       NULL                   COMMENT 'Resultado final del acta (ResultadoFinalActa enum)',
    -- bandejas y acciones
    cod_bandeja                     VARCHAR(50)    NULL                   COMMENT 'Código de bandeja (CodigoBandeja; EXPLICIT_STRING_CODE; max 50 chars)',
    sub_bandeja                     VARCHAR(80)    NULL                   COMMENT 'Sub-bandeja específica',
    accion_pendiente                VARCHAR(50)    NULL                   COMMENT 'Acción pendiente principal (AccionPendiente; EXPLICIT_STRING_CODE; max 50 chars)',
    -- documentos y notificaciones
    tiene_documentos                BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = el acta tiene documentos generados',
    tiene_docs_pendientes_firma     BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay documentos pendientes de firma',
    tiene_docs_listos_para_notificar BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay documentos listos para notificar',
    tiene_notificaciones            BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = el acta tiene notificaciones generadas',
    notificacion_en_curso           BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay una notificación en proceso',
    bloqueado_cierre                BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay bloqueantes materiales activos',
    id_docu_ult                     BIGINT         NULL                   COMMENT 'FK al último documento relevante del acta',
    bloqueado_notificacion          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay condición que bloquea nuevas notificaciones',
    -- valorización operativa
    valorizacion_operativa_id       BIGINT         NULL                   COMMENT 'FK a la valorización operativa vigente',
    estado_valorizacion_operativa   SMALLINT       NULL                   COMMENT 'Estado de la valorización operativa (EstadoValorizacion)',
    tipo_valorizacion_operativa     SMALLINT       NULL                   COMMENT 'Tipo de la valorización operativa (TipoValorizacionActa)',
    monto_operativo_vigente         DECIMAL(14,2)  NULL                   COMMENT 'Monto operativo UX (no es dato económico de pagos; DECISION_DDL-SNAP-02)',
    si_monto_confirmado             BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = el monto operativo está confirmado',
    -- satélites del acta
    licencia_provincia_txt          VARCHAR(64)    NULL                   COMMENT 'Texto de provincia de la licencia (snapshot tránsito)',
    licencia_unidad_txt             VARCHAR(80)    NULL                   COMMENT 'Texto de unidad territorial de la licencia (snapshot tránsito)',
    nomenclatura_resumen            VARCHAR(120)   NULL                   COMMENT 'Resumen de nomenclatura catastral (snapshot contravención)',
    id_bie_i                        MEDIUMINT UNSIGNED NULL                   COMMENT 'Proyeccion del ID del bien inmueble de fal_acta_contravencion (snapshot contravención). HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    id_bie_c                        MEDIUMINT UNSIGNED NULL                   COMMENT 'Proyeccion del ID del bien comercio de fal_acta_contravencion (snapshot contravención). HUMAN_DECISION_CLOSED FULL-R1.2-CORRECCION-10.',
    -- paralización
    motivo_paralizacion_act         SMALLINT       NULL                   COMMENT 'Motivo de paralización actual (MotivoParalizacion; NULL si no paralizada)',
    -- control técnico
    ultimo_evento_tipo              CHAR(6)        NULL                   COMMENT 'Tipo del último evento registrado (TipoEventoActa; CHAR(6))',
    ultima_actualizacion            DATETIME(6)    NULL                   COMMENT 'Fecha-hora del último recálculo del snapshot',
    PRIMARY KEY (id_acta),
    CONSTRAINT fk_snapshot_acta FOREIGN KEY (id_acta)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_snapshot_docu_ult FOREIGN KEY (id_docu_ult)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_snapshot_valorizacion FOREIGN KEY (valorizacion_operativa_id)
        REFERENCES fal_acta_valorizacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Proyección operativa resumida del expediente (1:1). Derivada y regenerable. No transporta economía de pagos (DECISION_DDL-SNAP-02). OCC.';

-- ----------------------------------------------------------------------------
-- 61. fal_acta_apelacion_documento
--     Documentos presentados con una apelación.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_apelacion_documento (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la relación',
    id_apelacion                BIGINT         NOT NULL               COMMENT 'FK a la apelación',
    id_documento                BIGINT         NOT NULL               COMMENT 'FK al documento presentado',
    tipo_documento_apelacion    SMALLINT       NULL                   COMMENT 'Tipo del documento en el contexto de la apelación (TipoDocumentoApelacion)',
    si_activo                   BOOLEAN        NOT NULL DEFAULT TRUE  COMMENT 'TRUE = relación vigente',
    fh_alta                     DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta                CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que vinculó el documento',
    PRIMARY KEY (id),
    CONSTRAINT fk_apel_docu_apelacion FOREIGN KEY (id_apelacion)
        REFERENCES fal_acta_apelacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_apel_docu_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_apel_docu_apelacion (id_apelacion)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Documentos presentados con cada apelación del expediente';

-- ----------------------------------------------------------------------------
-- 62. fal_acta_forma_pago
--     Instrumento de pago de una obligación (recibo, plan, refinanciación).
--     OCC. Columnas generadas para unicidad vigente.
--     (DECISION_DDL-FORMA-01 / DECISION_DDL-PAGO-03 CERRADAS)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_forma_pago (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico de la forma de pago',
    version_row                 INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    obligacion_pago_id          BIGINT         NOT NULL               COMMENT 'FK a la obligación de pago',
    nro_forma                   SMALLINT       NOT NULL               COMMENT 'Número ordinal de la forma por obligación (positivo, único por obligación)',
    tipo_forma_pago             SMALLINT       NOT NULL               COMMENT '1=RECIBO_AL_COBRO, 2=PLAN_PAGO, 3=REFINANCIACION',
    estado_forma_pago           SMALLINT       NOT NULL               COMMENT 'Estado (EstadoFormaPago enum)',
    monto_forma                 DECIMAL(14,2)  NOT NULL               COMMENT 'Monto de la forma de pago (escala 2; inmutable mientras vigente)',
    cmte_em                     CHAR(2)        NULL                   COMMENT 'Comitente EM de la emisión (terna EM completa o totalmente NULL)',
    pref_em                     SMALLINT       NULL                   COMMENT 'Prefijo EM de la emisión',
    nro_em                      INT            NULL                   COMMENT 'Número EM de la emisión',
    cmte_pg                     CHAR(2)        NULL                   COMMENT 'Comitente PG del recibo (terna PG completa o totalmente NULL)',
    pref_pg                     SMALLINT       NULL                   COMMENT 'Prefijo PG del recibo',
    nro_pg                      INT            NULL                   COMMENT 'Número PG del recibo',
    forma_reemplazada_id        BIGINT         NULL                   COMMENT 'FK auto-referencial a la forma reemplazada (nullable)',
    fh_generacion               DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora de generación de la forma de pago',
    fh_pago_procesado           DATETIME(6)    NULL                   COMMENT 'Fecha-hora en que el pago fue procesado',
    fh_pago_confirmado          DATETIME(6)    NULL                   COMMENT 'Fecha-hora en que el pago fue confirmado',
    fh_baja                     DATETIME(6)    NULL                   COMMENT 'Fecha-hora de baja de la forma de pago',
    motivo_baja                 SMALLINT       NULL                   COMMENT 'Motivo de baja (MotivoBajaFormaPago enum)',
    si_vigente                  BOOLEAN        NOT NULL               COMMENT 'TRUE = forma de pago vigente',
    si_excluir_escaneo          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = excluir de escaneo de recibos',
    fh_vencimiento              DATETIME(6)    NULL                   COMMENT 'Fecha-hora de vencimiento informativa (DECISION_DDL-FORMA-01)',
    -- columnas generadas para unicidad de la forma vigente y su tipo
    obligacion_pago_id_vigente  BIGINT         GENERATED ALWAYS AS (IF(si_vigente = TRUE, obligacion_pago_id, NULL)) PERSISTENT COMMENT 'Generada: obligacion_pago_id si vigente, NULL si no',
    tipo_forma_pago_vigente     SMALLINT       GENERATED ALWAYS AS (IF(si_vigente = TRUE, tipo_forma_pago, NULL)) VIRTUAL COMMENT 'Generada: tipo si vigente, NULL si no',
    fh_alta                     DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta',
    id_user_alta                CHAR(36)       NOT NULL               COMMENT 'UUID del usuario que creó la forma de pago',
    PRIMARY KEY (id),
    UNIQUE KEY uq_forma_pago_vigente (obligacion_pago_id_vigente),
    UNIQUE KEY uq_forma_pago_nro (obligacion_pago_id, nro_forma),
    CONSTRAINT fk_forma_pago_obligacion FOREIGN KEY (obligacion_pago_id)
        REFERENCES fal_acta_obligacion_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_forma_pago_reemplazada FOREIGN KEY (forma_reemplazada_id)
        REFERENCES fal_acta_forma_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_forma_pago_tipo CHECK (tipo_forma_pago IN (1, 2, 3)),
    CONSTRAINT chk_forma_pago_nro CHECK (nro_forma > 0),
    CONSTRAINT chk_forma_pago_monto CHECK (monto_forma >= 0),
    INDEX ix_forma_pago_obligacion (obligacion_pago_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Instrumento de pago de una obligación. OCC. Columna generada garantiza unicidad de forma vigente. DECISION_DDL-FORMA-01.';

-- =============================================================================
-- ALTER TABLE — Ciclo G8↔G9: fal_acta_obligacion_pago → fal_acta_forma_pago
-- =============================================================================
ALTER TABLE fal_acta_obligacion_pago
    ADD CONSTRAINT fk_oblig_forma_pago_vigente FOREIGN KEY (forma_pago_vigente_id)
        REFERENCES fal_acta_forma_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- =============================================================================
-- SECCIÓN 10 — G10–G11: Plan de pago y movimientos (tablas 63–64)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 63. fal_acta_plan_pago_ref
--     Cabecera estructural del plan de pago externo. OCC.
--     Columna generada obligacion_pago_id_vigente para unicidad.
--     (DECISION_DDL-PLAN-01 CERRADA)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_plan_pago_ref (
    id                          BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del plan de pago',
    version_row                 INT            NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC)',
    forma_pago_id               BIGINT         NOT NULL               COMMENT 'FK a la forma de pago que origina el plan',
    obligacion_pago_id          BIGINT         NOT NULL               COMMENT 'FK a la obligación de pago',
    id_tdoc_plan                SMALLINT       NOT NULL               COMMENT 'Tipo de documento del plan externo (valor=1 según modelo vigente)',
    id_doc_plan                 BIGINT         NOT NULL               COMMENT 'ID del plan en Ingresos/Tesorería (externo)',
    estado_plan                 SMALLINT       NOT NULL               COMMENT '1=ACTIVO, 2=FINALIZADO_POR_PAGO, 3=ANULADO, 4=REFINANCIADO',
    fh_generacion_plan          DATETIME(6)    NULL                   COMMENT 'Fecha-hora de generación del plan en Ingresos (nullable)',
    cantidad_cuotas             SMALLINT       NOT NULL               COMMENT 'Cantidad de cuotas del plan (positivo)',
    importe_total_plan          DECIMAL(14,2)  NOT NULL               COMMENT 'Importe total del plan (escala 2)',
    importe_cuota_regular       DECIMAL(14,2)  NULL                   COMMENT 'Importe de la cuota regular (nullable si no uniforme)',
    fh_ultimo_pago              DATETIME(6)    NULL                   COMMENT 'Fecha-hora del último pago registrado. DECISION_DDL-PLAN-01.',
    fh_finalizacion_pago        DATETIME(6)    NULL                   COMMENT 'Fecha-hora de finalización del pago del plan',
    fh_cancelacion              DATETIME(6)    NULL                   COMMENT 'Fecha-hora de cancelación del plan',
    fh_refinanciacion           DATETIME(6)    NULL                   COMMENT 'Fecha-hora de refinanciación (plan reemplazado por otro)',
    plan_refinanciado_id        BIGINT         NULL                   COMMENT 'FK auto-referencial al plan refinanciado (nullable)',
    si_excluir_escaneo          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT 'TRUE = excluir de procesos de escaneo',
    fh_ult_sync_ingresos        DATETIME(6)    NULL                   COMMENT 'Fecha-hora de última sincronización con Ingresos. DECISION_DDL-PLAN-01.',
    si_vigente                  BOOLEAN        NOT NULL               COMMENT 'TRUE = plan vigente para esta obligación',
    -- columna generada para unicidad del plan vigente
    obligacion_pago_id_vigente  BIGINT         GENERATED ALWAYS AS (IF(si_vigente = TRUE, obligacion_pago_id, NULL)) PERSISTENT COMMENT 'Generada: obligacion_pago_id si vigente, NULL si no',
    PRIMARY KEY (id),
    UNIQUE KEY uq_plan_pago_ext (id_tdoc_plan, id_doc_plan),
    UNIQUE KEY uq_plan_pago_vigente (obligacion_pago_id_vigente),
    CONSTRAINT fk_plan_pago_forma FOREIGN KEY (forma_pago_id)
        REFERENCES fal_acta_forma_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_plan_pago_obligacion FOREIGN KEY (obligacion_pago_id)
        REFERENCES fal_acta_obligacion_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_plan_pago_refinanciado FOREIGN KEY (plan_refinanciado_id)
        REFERENCES fal_acta_plan_pago_ref (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_plan_pago_estado CHECK (estado_plan BETWEEN 1 AND 4),
    CONSTRAINT chk_plan_pago_cuotas CHECK (cantidad_cuotas > 0),
    INDEX ix_plan_pago_obligacion (obligacion_pago_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Cabecera del plan de pago externo (Ingresos/Tesorería). OCC. Sin tabla de cuotas. DECISION_DDL-PLAN-01.';

-- ----------------------------------------------------------------------------
-- 64. fal_acta_pago_movimiento
--     Registro append-only de cada hecho económico real del pago.
--     Sin versionRow (immutable). (DECISION_DDL-MOV-01..04 / DECISION_DDL-PAGO-MOV-01 CERRADAS)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_pago_movimiento (
    id                              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Identificador técnico del movimiento (append-only)',
    obligacion_pago_id              BIGINT         NOT NULL               COMMENT 'FK a la obligación de pago',
    forma_pago_id                   BIGINT         NULL                   COMMENT 'FK a la forma de pago (nullable para reversos de emisiones anuladas)',
    plan_pago_ref_id                BIGINT         NULL                   COMMENT 'FK al plan de pago (nullable si contado o sin plan)',
    tipo_movimiento                 SMALLINT       NOT NULL               COMMENT '1=DEUDA_EMITIDA, 2=PAGO_PROCESADO, 3=PAGO_CONFIRMADO, 4=PAGO_REVERTIDO, 5=EMISION_ANULADA',
    origen_movimiento               SMALLINT       NOT NULL               COMMENT 'Origen del movimiento (OrigenMovimiento enum; catálogo cerrado)',
    origen_confirmacion             SMALLINT       NULL                   COMMENT 'Origen de confirmación (OrigenConfirmacion enum; nullable)',
    evidencia_documento_id          BIGINT         NULL                   COMMENT 'ID de documento de evidencia del pago (nullable; sin FK a evitar ciclos)',
    clasificacion_pago              SMALLINT       NOT NULL DEFAULT 1     COMMENT '1=NORMAL, 2=DUPLICADO_REAL, 3=EXCEDENTE, 4=OBLIGACION_ANTERIOR. DECISION_DDL-MOV-04.',
    nro_cuota                       SMALLINT       NULL                   COMMENT 'Número de cuota del plan abonada (nullable si no es plan)',
    -- importes
    importe_capital                 DECIMAL(14,2)  NULL                   COMMENT 'Importe de capital (escala 2; nullable si sin detalle)',
    importe_rima                    DECIMAL(14,2)  NULL                   COMMENT 'Importe de RIMA (escala 2; nullable si sin detalle)',
    importe_total                   DECIMAL(14,2)  NULL                   COMMENT 'Importe total = capital + rima (escala 2; nullable si estimación)',
    -- referencias EM
    cmte_em                         CHAR(2)        NULL                   COMMENT 'Comitente EM (terna EM completa o totalmente NULL)',
    pref_em                         SMALLINT       NULL                   COMMENT 'Prefijo EM',
    nro_em                          INT            NULL                   COMMENT 'Número EM',
    -- referencias PG (obligatoria completa en PAGO_CONFIRMADO original sin movimiento_origen_id)
    cmte_pg                         CHAR(2)        NULL                   COMMENT 'Comitente PG del recibo (terna PG completa o totalmente NULL)',
    pref_pg                         SMALLINT       NULL                   COMMENT 'Prefijo PG del recibo',
    nro_pg                          INT            NULL                   COMMENT 'Número PG del recibo',
    -- contexto operativo
    id_cierre                       BIGINT         NULL                   COMMENT 'ID de cierre de caja (nullable)',
    id_ope                          BIGINT         NULL                   COMMENT 'ID de operación en Ingresos (nullable)',
    movimiento_origen_id            BIGINT         NULL                   COMMENT 'FK auto-referencial: reversos apuntan al movimiento original; PAGRES apunta al PAGANT',
    motivo_anulacion_pago           SMALLINT       NULL                   COMMENT '1=CONTRACARGO, 2=ANULACION_TESORERIA, 3=ERROR_OPERATIVO, 4=DUPLICADO, 5=REVERSION_MEDIO_PAGO, 6=OTRO',
    -- motivo_aplicacion_pago_anterior ELIMINADO (HUMAN_DECISION_CLOSED): texto libre no estructurado.
    -- Idempotencia via movimiento_origen_id + columna generada. Notas humanas a fal_observacion(MOVIMIENTO_PAGO).
    fh_pago_procesado               DATETIME(6)    NULL                   COMMENT 'Fecha-hora de procesamiento del pago',
    fh_pago_confirmado              DATETIME(6)    NULL                   COMMENT 'Fecha-hora de confirmación del pago',
    referencia_externa              VARCHAR(80)    NULL                   COMMENT 'Referencia externa del pago (máx 80 chars)',
    fh_movimiento                   DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora funcional del hecho económico (inmutable)',
    -- auditoría
    fh_alta                         DATETIME(6)    NOT NULL               COMMENT 'Fecha-hora técnica de alta (NOT NULL; validado en Java Builder)',
    id_user_alta                    CHAR(36)       NOT NULL               COMMENT 'UUID del usuario o proceso que registró el movimiento',
    -- columna generada para unicidad de aplicación PAGRES (DECISION_DDL-PAGO-MOV-01)
    movimiento_origen_id_aplicacion BIGINT         GENERATED ALWAYS AS (IF(clasificacion_pago = 4, movimiento_origen_id, NULL)) PERSISTENT COMMENT 'Generada: movimiento_origen_id si es PAGRES (clasificacion=4), NULL si no. Garantiza unicidad de aplicación.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_pago_mov_pagres_unicidad (movimiento_origen_id_aplicacion),
    CONSTRAINT fk_pago_mov_obligacion FOREIGN KEY (obligacion_pago_id)
        REFERENCES fal_acta_obligacion_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pago_mov_forma_pago FOREIGN KEY (forma_pago_id)
        REFERENCES fal_acta_forma_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pago_mov_plan_pago FOREIGN KEY (plan_pago_ref_id)
        REFERENCES fal_acta_plan_pago_ref (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pago_mov_origen FOREIGN KEY (movimiento_origen_id)
        REFERENCES fal_acta_pago_movimiento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_pago_mov_tipo CHECK (tipo_movimiento BETWEEN 1 AND 5),
    CONSTRAINT chk_pago_mov_clasificacion CHECK (clasificacion_pago BETWEEN 1 AND 4),
    CONSTRAINT chk_pago_mov_motivo_anulacion CHECK (motivo_anulacion_pago IS NULL OR motivo_anulacion_pago BETWEEN 1 AND 6),
    INDEX ix_pago_mov_obligacion (obligacion_pago_id),
    INDEX ix_pago_mov_forma_pago (forma_pago_id),
    INDEX ix_pago_mov_fh (fh_movimiento)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Registro append-only de hechos económicos de pagos. Sin versionRow (immutable). Columna generada para unicidad PAGRES. DECISION_DDL-MOV-01..04, DECISION_DDL-PAGO-MOV-01.';

-- =============================================================================
-- SECCIÓN 11 — G12: Proyección económica (tabla 65)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 65. fal_acta_economia_proyeccion
--     Proyección económica actual por acta (1:1). OCC. Mutable, regenerable.
--     (DECISION_DDL-ECPR-01 / DECISION_DDL-SNAP-02 CERRADAS)
-- ----------------------------------------------------------------------------
CREATE TABLE fal_acta_economia_proyeccion (
    acta_id                             BIGINT          NOT NULL               COMMENT 'PK y FK al acta (relación 1:1)',
    version_row                         INT             NOT NULL DEFAULT 0     COMMENT 'Control de concurrencia optimista (OCC). DECISION_DDL-ECPR-01.',
    -- referencias vigentes
    obligacion_vigente_id               BIGINT          NULL                   COMMENT 'FK a la obligación de pago vigente',
    forma_pago_vigente_id               BIGINT          NULL                   COMMENT 'FK a la forma de pago vigente',
    plan_pago_vigente_id                BIGINT          NULL                   COMMENT 'FK al plan de pago vigente',
    -- estado actual (copias regenerables)
    tipo_obligacion                     SMALLINT        NULL                   COMMENT 'Tipo de obligación vigente (TipoObligacionPago)',
    estado_obligacion                   SMALLINT        NULL                   COMMENT 'Estado de la obligación (EstadoObligacionPago)',
    monto_obligacion_vigente            DECIMAL(14,2)   NULL                   COMMENT 'Monto original de la obligación vigente',
    tipo_forma_pago                     SMALLINT        NULL                   COMMENT 'Tipo de la forma de pago vigente (TipoFormaPago)',
    estado_forma_pago                   SMALLINT        NULL                   COMMENT 'Estado de la forma de pago vigente (EstadoFormaPago)',
    estado_plan                         SMALLINT        NULL                   COMMENT 'Estado del plan de pago vigente (EstadoPlanPago)',
    cantidad_cuotas                     SMALLINT        NULL                   COMMENT 'Cantidad de cuotas del plan vigente',
    importe_cuota_regular               DECIMAL(14,2)   NULL                   COMMENT 'Importe de la cuota regular del plan',
    -- mora y seguimiento de cuotas
    cantidad_cuotas_pagadas             SMALLINT        NULL                   COMMENT 'Cuotas pagadas del plan vigente',
    cantidad_cuotas_vencidas            SMALLINT        NULL                   COMMENT 'Cuotas vencidas sin pagar',
    cantidad_cuotas_en_mora             SMALLINT        NULL                   COMMENT 'Cuotas en mora',
    cantidad_cuotas_mora_consec         SMALLINT        NULL                   COMMENT 'Cuotas consecutivas en mora',
    dias_mora_max                       SMALLINT        NULL                   COMMENT 'Máximo de días de mora calculado',
    -- importes calculados
    importe_pago_procesado              DECIMAL(14,2)   NULL                   COMMENT 'Importe total procesado (no confirmado)',
    importe_confirmado_evidencia_pendiente DECIMAL(14,2) NULL                  COMMENT 'Importe confirmado con evidencia pendiente de tesorería',
    importe_confirmado_tesoreria        DECIMAL(14,2)   NULL                   COMMENT 'Importe confirmado por tesorería',
    importe_observado_tesoreria         DECIMAL(14,2)   NULL                   COMMENT 'Importe observado por tesorería',
    importe_aplicado_total              DECIMAL(14,2)   NOT NULL DEFAULT 0     COMMENT 'Importe neto aplicado (base para saldo y cancelación)',
    importe_revertido                   DECIMAL(14,2)   NULL                   COMMENT 'Importe total revertido',
    saldo_pendiente                     DECIMAL(14,2)   NULL                   COMMENT 'Saldo = MAX(0, monto_obligacion - importe_aplicado_total)',
    importe_excedente                   DECIMAL(14,2)   NOT NULL DEFAULT 0     COMMENT 'Importe pagado en exceso sobre la obligación (en Java, ausente en histórico)',
    si_parcialmente_pagada              BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay pagos parciales (en Java, ausente en histórico)',
    importe_vencido_plan                DECIMAL(14,2)   NULL                   COMMENT 'Importe de cuotas vencidas sin pagar del plan',
    -- conciliación actual
    estado_conciliacion_actual          SMALLINT        NOT NULL DEFAULT 1     COMMENT '1=NO_APLICA, 2=PENDIENTE_TESORERIA, 3=CONCILIADO_TESORERIA, 4=OBSERVADO_TESORERIA',
    si_conciliacion_pendiente           BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay conciliación pendiente de tesorería',
    fh_ultima_conciliacion              DATETIME(6)     NULL                   COMMENT 'Fecha-hora de la última conciliación',
    referencia_ultima_conciliacion      VARCHAR(80)     NULL                   COMMENT 'Referencia de la última conciliación (máx 80 chars)',
    -- flags de pago
    si_pago_procesado                   BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay pago procesado sin confirmar',
    si_pago_confirmado                  BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay pago confirmado por tesorería',
    -- plan caído calculado
    si_plan_caido_calculado             BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = el plan cayó por mora calculada',
    fh_desde_plan_caido_calculado       DATETIME(6)     NULL                   COMMENT 'Fecha-hora desde cuando el plan está caído',
    motivo_plan_caido_calculado         SMALLINT        NULL                   COMMENT '1=CUOTAS_EN_MORA, 2=MORA_CONSECUTIVA, 3=ANTIGUEDAD_MORA, 4=REGLA_INGRESOS, 5=COMBINADA',
    -- aptitud de intimación
    si_apta_intimacion                  BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = la obligación es apta para intimación',
    fh_apta_intimacion                  DATETIME(6)     NULL                   COMMENT 'Fecha-hora desde cuando es apta para intimación',
    motivo_apta_intimacion              SMALLINT        NULL                   COMMENT 'Motivo de aptitud de intimación (MotivoAptitudIntimacion enum)',
    -- reapertura
    si_reapertura_requerida             BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE = hay reverso posterior al cierre que requiere reapertura',
    -- watermarks técnicos
    ultimo_movimiento_id_proyectado     BIGINT          NULL                   COMMENT 'FK al último movimiento considerado en la proyección',
    fh_corte_economico                  DATETIME(6)     NULL                   COMMENT 'Fecha-hora de corte de la proyección (DECISION_DDL-ECPR-01: nullable)',
    fh_ultima_sincronizacion            DATETIME(6)     NULL                   COMMENT 'Fecha-hora de la última sincronización',
    origen_ultima_actualizacion         SMALLINT        NOT NULL DEFAULT 1     COMMENT '1=TIEMPO_REAL, 2=SINCRONIZACION_NOCTURNA, 3=REBUILD, 4=CORRECCION_CONTROLADA',
    -- última modificación (DECISION_DDL-ECPR-01: obligatorio)
    fh_ult_mod                          DATETIME(6)     NOT NULL               COMMENT 'Fecha-hora de la última modificación (obligatorio; DECISION_DDL-ECPR-01)',
    id_user_ult_mod                     CHAR(36)        NULL                   COMMENT 'UUID del usuario/proceso que realizó la última modificación',
    PRIMARY KEY (acta_id),
    CONSTRAINT fk_ecproy_acta FOREIGN KEY (acta_id)
        REFERENCES fal_acta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ecproy_obligacion FOREIGN KEY (obligacion_vigente_id)
        REFERENCES fal_acta_obligacion_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ecproy_forma_pago FOREIGN KEY (forma_pago_vigente_id)
        REFERENCES fal_acta_forma_pago (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ecproy_plan_pago FOREIGN KEY (plan_pago_vigente_id)
        REFERENCES fal_acta_plan_pago_ref (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ecproy_ult_movimiento FOREIGN KEY (ultimo_movimiento_id_proyectado)
        REFERENCES fal_acta_pago_movimiento (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'Proyección económica actual por acta (1:1). Mutable, regenerable. No es fuente histórica ni jurídica. OCC. DECISION_DDL-ECPR-01, DECISION_DDL-SNAP-02.';

-- =============================================================================
-- RESUMEN DE ALTER TABLE APLICADOS
-- =============================================================================
-- Ciclos resueltos y ALTER TABLE ejecutados en este script:
--
-- 1. POST G1  → ALTER TABLE fal_dependencia ADD FK id_dep_padre → fal_dependencia
-- 2. POST G4  → ALTER TABLE fal_persona_domicilio ADD FK acta_origen_id → fal_acta
-- 3. POST G9  → ALTER TABLE fal_acta_obligacion_pago ADD FK forma_pago_vigente_id → fal_acta_forma_pago
--
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- TRIGGERS DEFENSIVOS AFTER DELETE — fal_observacion (polimorfismo)
-- Patron: DELETE FROM fal_observacion WHERE entidad_tipo = <codigo> AND entidad_id = OLD.id
-- Referencia: spec-as-source/40-domain/observaciones.md sec. 5
-- Nota: fal_acta (codigo 1) no requiere trigger; el FK ON DELETE CASCADE de
--       id_acta_contexto ya limpia las observaciones con contexto en esa acta.
-- 21 triggers para codigos 2-22 de EntidadTipoObservada.
-- =============================================================================

DELIMITER //

CREATE TRIGGER trg_fal_persona_ad_observaciones
AFTER DELETE ON fal_persona FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 2 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_persona_domicilio_ad_observaciones
AFTER DELETE ON fal_persona_domicilio FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 3 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_documento_ad_observaciones
AFTER DELETE ON fal_documento FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 4 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_evidencia_ad_observaciones
AFTER DELETE ON fal_acta_evidencia FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 5 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_notificacion_ad_observaciones
AFTER DELETE ON fal_notificacion FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 6 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_notificacion_intento_ad_observaciones
AFTER DELETE ON fal_notificacion_intento FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 7 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_fallo_ad_observaciones
AFTER DELETE ON fal_acta_fallo FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 8 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_apelacion_ad_observaciones
AFTER DELETE ON fal_acta_apelacion FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 9 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_gestion_externa_ad_observaciones
AFTER DELETE ON fal_acta_gestion_externa FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 10 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_paralizacion_ad_observaciones
AFTER DELETE ON fal_acta_paralizacion FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 11 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_archivo_ad_observaciones
AFTER DELETE ON fal_acta_archivo FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 12 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_medida_preventiva_ad_observaciones
AFTER DELETE ON fal_acta_medida_preventiva FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 13 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_bloqueante_cierre_material_ad_observaciones
AFTER DELETE ON fal_acta_bloqueante_cierre_material FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 14 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_articulo_infringido_ad_observaciones
AFTER DELETE ON fal_acta_articulo_infringido FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 15 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_valorizacion_ad_observaciones
AFTER DELETE ON fal_acta_valorizacion FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 16 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_obligacion_pago_ad_observaciones
AFTER DELETE ON fal_acta_obligacion_pago FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 17 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_forma_pago_ad_observaciones
AFTER DELETE ON fal_acta_forma_pago FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 18 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_plan_pago_ref_ad_observaciones
AFTER DELETE ON fal_acta_plan_pago_ref FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 19 AND entidad_id = OLD.id//

CREATE TRIGGER trg_fal_acta_pago_movimiento_ad_observaciones
AFTER DELETE ON fal_acta_pago_movimiento FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 20 AND entidad_id = OLD.id//

CREATE TRIGGER trg_num_talonario_ad_observaciones
AFTER DELETE ON num_talonario FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 21 AND entidad_id = OLD.id//

CREATE TRIGGER trg_num_talonario_movimiento_ad_observaciones
AFTER DELETE ON num_talonario_movimiento FOR EACH ROW
DELETE FROM fal_observacion WHERE entidad_tipo = 22 AND entidad_id = OLD.id//

DELIMITER ;

-- =============================================================================
-- FIN DEL SCRIPT DDL — BOD FALTAS
-- =============================================================================
-- Tablas creadas: 64 (CREATE TABLE)
-- Tabla preexistente adoptada: 1 (fal_rubro_version — NO incluida en este script)
-- Total canonico: 65
-- Triggers defensivos AFTER DELETE: 21 (codigos 2-22 de EntidadTipoObservada)
-- =============================================================================

-- =============================================================================
-- TABLE: fal_inmueble
-- Base: sb_malvinas_geo (PostgreSQL 15.2)
-- DDL reconstruido desde information_schema + pg_constraint + pg_indexes el 2026-07-30
-- Verificado: EXISTE — 85.167 filas (n_live_tup al 2026-07-30)
--
-- Sin columna de geometría propia. El join geoespacial se realiza mediante
-- clave catastral contra geo_qmat_parcela:
--   circunscripcion::CHAR(1) = circunsc, seccion = secc, quinta = quint,
--   fraccion = fracc, manzana = manz, parcela = parc
-- SRID de la tabla parcela: 22195
--
-- Nota: los valores '0' en quinta y fraccion representan ausencia (convención
-- local, no se almacenan como NULL). Ver glossary.md §Nomenclatura catastral.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.fal_inmueble (
    id_suj                   SMALLINT      NOT NULL,
    id_bie                   INTEGER       NOT NULL,
    titulares                VARCHAR(196),
    destinatario_nombre      VARCHAR(196),
    id_manzana               SMALLINT      NOT NULL,
    circunscripcion          SMALLINT      NOT NULL,
    seccion                  VARCHAR(7)    NOT NULL,
    quinta                   VARCHAR(7)    NOT NULL,
    fraccion                 VARCHAR(7)    NOT NULL,
    manzana                  VARCHAR(7)    NOT NULL,
    parcela                  VARCHAR(7)    NOT NULL,
    unidad_funcional         VARCHAR(7),
    unidad_complementaria    VARCHAR(3),
    id_localidad             CHAR(2)       NOT NULL,
    localidad_nombre         VARCHAR(40)   NOT NULL,
    tipo_inmueble            VARCHAR(48)   NOT NULL,
    barrio_nombre            VARCHAR(48)   NOT NULL,
    si_barrio_cerrado        CHAR(1)       NOT NULL,
    calle_codigo             CHAR(5)       NOT NULL,
    calle_nombre             VARCHAR(64)   NOT NULL,
    calle_numero             INTEGER       NOT NULL,
    calle_entre_1_codigo     CHAR(5),
    calle_entre_1_nombre     VARCHAR(64),
    calle_entre_2_codigo     CHAR(5),
    calle_entre_2_nombre     VARCHAR(64),
    superficie_cubierta      NUMERIC       NOT NULL,
    superficie_semicubierta  NUMERIC       NOT NULL,
    superficie_pileta        NUMERIC       NOT NULL,
    superficie_titulo        NUMERIC       NOT NULL,
    metros_frente            NUMERIC       NOT NULL,
    calculo_perimetro_frente VARCHAR(48)   NOT NULL,
    calculo_perimetro_lado   VARCHAR(48)   NOT NULL,
    metros_lado              NUMERIC       NOT NULL,
    alu                      VARCHAR(4)    NOT NULL,
    lim                      VARCHAR(4)    NOT NULL,
    agu                      VARCHAR(4)    NOT NULL,
    gas                      VARCHAR(4)    NOT NULL,
    pav                      VARCHAR(4)    NOT NULL,
    via                      VARCHAR(4)    NOT NULL,
    ceap                     VARCHAR(4)    NOT NULL,
    cloacas                  VARCHAR(4)    NOT NULL,
    estado_posesion          VARCHAR(20)   NOT NULL,
    posesion_cuentas         VARCHAR(512)  NOT NULL,
    row_hash                 CHAR(64)      NOT NULL,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fal_inmueble
        PRIMARY KEY (id_suj, id_bie),
    CONSTRAINT ck_fal_inmueble_hash
        CHECK (row_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_fal_inmueble_barrio_cerrado
        CHECK (si_barrio_cerrado = ANY (ARRAY['S'::bpchar, 'N'::bpchar])),
    CONSTRAINT ck_fal_inmueble_superficies
        CHECK (
            superficie_cubierta     >= 0
            AND superficie_semicubierta >= 0
            AND superficie_pileta       >= 0
            AND superficie_titulo       >= 0
            AND metros_frente           >= 0
            AND metros_lado             >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_fal_inmueble_manzana
    ON public.fal_inmueble USING btree (id_manzana);

CREATE INDEX IF NOT EXISTS idx_fal_inmueble_partida
    ON public.fal_inmueble USING btree
    (id_localidad, circunscripcion, seccion, quinta, fraccion, manzana, parcela);

CREATE INDEX IF NOT EXISTS idx_fal_inmueble_localidad_calle
    ON public.fal_inmueble USING btree (id_localidad, calle_codigo, calle_numero);

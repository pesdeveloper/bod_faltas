-- =============================================================================
-- TABLE: fal_comercio
-- Base: sb_malvinas_geo (PostgreSQL 15.2)
-- DDL reconstruido desde information_schema + pg_constraint + pg_indexes el 2026-07-30
-- Verificado: EXISTE — 13.024 filas (n_live_tup al 2026-07-30)
--
-- No tiene geometría propia; la ubicación se resuelve a través de
-- fal_comercio_inmueble → fal_inmueble → geo_qmat_parcela.
--
-- tipo: 'C' Comercio, 'I' Industrial, 'S' Servicio
-- id_grupo: 0–9 (NULL permitido)
-- rubro_codigo: 1–32000
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.fal_comercio (
    id_suj                 SMALLINT      NOT NULL,
    id_bie                 INTEGER       NOT NULL,
    nombre_fantasia        VARCHAR(64),
    titulares              VARCHAR(196),
    fecha_inicio_actividad DATE,
    grupo_denominacion     VARCHAR(16),
    id_grupo               SMALLINT,
    si_resto               BOOLEAN       NOT NULL,
    si_grande              BOOLEAN       NOT NULL,
    si_tasa_especial       BOOLEAN       NOT NULL,
    si_deposito            BOOLEAN       NOT NULL,
    si_mayorista           BOOLEAN       NOT NULL,
    rubro_codigo           SMALLINT      NOT NULL,
    rubro_nombre           VARCHAR(64),
    tipo                   CHAR(1)       NOT NULL,
    tipo_comercio          VARCHAR(20),
    tipo_contribuyente     VARCHAR(20),
    row_hash               CHAR(64)      NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fal_comercio
        PRIMARY KEY (id_suj, id_bie),
    CONSTRAINT ck_fal_comercio_hash
        CHECK (row_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_fal_comercio_tipo
        CHECK (tipo = ANY (ARRAY['C'::bpchar, 'I'::bpchar, 'S'::bpchar])),
    CONSTRAINT ck_fal_comercio_id_grupo
        CHECK (id_grupo IS NULL OR (id_grupo >= 0 AND id_grupo <= 9)),
    CONSTRAINT ck_fal_comercio_rubro
        CHECK (rubro_codigo >= 1 AND rubro_codigo <= 32000)
);

CREATE INDEX IF NOT EXISTS idx_fal_comercio_nombre
    ON public.fal_comercio USING btree (upper(nombre_fantasia::text) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_fal_comercio_rubro
    ON public.fal_comercio USING btree (rubro_codigo);

CREATE INDEX IF NOT EXISTS idx_fal_comercio_tipo_grupo
    ON public.fal_comercio USING btree (tipo, id_grupo);

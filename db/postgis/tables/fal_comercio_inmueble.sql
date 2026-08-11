-- =============================================================================
-- TABLE: fal_comercio_inmueble
-- Base: sb_malvinas_geo (PostgreSQL 15.2)
-- DDL reconstruido desde information_schema + pg_constraint + pg_indexes el 2026-07-30
-- Verificado: EXISTE — 12.737 filas (n_live_tup al 2026-07-30)
--
-- Tabla de relación N:M entre fal_comercio y fal_inmueble.
-- fg_prioridad: 0 = inmueble principal del comercio.
-- FK con ON DELETE CASCADE en ambas referencias.
-- Debe crearse después de fal_inmueble y fal_comercio.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.fal_comercio_inmueble (
    comercio_id_suj  SMALLINT    NOT NULL,
    comercio_id_bie  INTEGER     NOT NULL,
    inmueble_id_suj  SMALLINT    NOT NULL,
    inmueble_id_bie  INTEGER     NOT NULL,
    fg_prioridad     SMALLINT    NOT NULL,
    row_hash         CHAR(64)    NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fal_comercio_inmueble
        PRIMARY KEY (comercio_id_suj, comercio_id_bie, inmueble_id_suj, inmueble_id_bie),
    CONSTRAINT fk_fal_comercio_inmueble_comercio
        FOREIGN KEY (comercio_id_suj, comercio_id_bie)
        REFERENCES public.fal_comercio(id_suj, id_bie)
        ON DELETE CASCADE,
    CONSTRAINT fk_fal_comercio_inmueble_inmueble
        FOREIGN KEY (inmueble_id_suj, inmueble_id_bie)
        REFERENCES public.fal_inmueble(id_suj, id_bie)
        ON DELETE CASCADE,
    CONSTRAINT ck_fal_comercio_inmueble_hash
        CHECK (row_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_fal_comercio_inmueble_prioridad
        CHECK (fg_prioridad >= 0 AND fg_prioridad <= 255)
);

CREATE INDEX IF NOT EXISTS idx_fal_comercio_inmueble_inmueble
    ON public.fal_comercio_inmueble USING btree (inmueble_id_suj, inmueble_id_bie);

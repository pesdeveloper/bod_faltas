-- 04-tablas-storage-documental.sql
-- Generado desde spec/13-ddl

CREATE TABLE StorBackend
(
    IdStorageBackend     INT NOT NULL,
    NomStorageBackend    VARCHAR(64) NOT NULL,
    TipoBackend          SMALLINT NOT NULL,
    BasePath             VARCHAR(255),
    BucketContenedor     VARCHAR(120),
    PrefijoRuta          VARCHAR(120),
    SiDefault            SMALLINT NOT NULL,
    SiActivo             SMALLINT NOT NULL,
    ObsBackend           VARCHAR(255),
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkStorBack PRIMARY KEY (IdStorageBackend)
);

CREATE TABLE StorPolitica
(
    IdStoragePolitica    INT NOT NULL,
    Sistema              VARCHAR(20) NOT NULL,
    Familia              VARCHAR(30),
    TipoObjeto           VARCHAR(30),
    IdStorageBackend     INT NOT NULL,
    Prioridad            SMALLINT NOT NULL,
    SiActiva             SMALLINT NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    ObsPolitica          VARCHAR(255),
    CONSTRAINT PkStorPol PRIMARY KEY (IdStoragePolitica)
);

CREATE TABLE StorObjeto
(
    StorageKey           VARCHAR(64) NOT NULL,
    IdStorageBackend     INT NOT NULL,
    Sistema              VARCHAR(20) NOT NULL,
    Familia              VARCHAR(30) NOT NULL,
    TipoObjeto           VARCHAR(30) NOT NULL,
    Anio                 SMALLINT NOT NULL,
    Mes                  SMALLINT NOT NULL,
    Bucket               SMALLINT NOT NULL,
    RefNegocio           VARCHAR(80) NOT NULL,
    NomArchivo           VARCHAR(120),
    ExtArchivo           VARCHAR(10),
    MimeType             VARCHAR(80),
    TamBytes             INT8,
    HashArchivo          VARCHAR(128),
    RutaRelativa         VARCHAR(255) NOT NULL,
    EstadoStorage        SMALLINT NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkStorObj PRIMARY KEY (StorageKey)
);
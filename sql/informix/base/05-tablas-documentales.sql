-- 05-tablas-documentales.sql
-- Generado desde spec/13-ddl

CREATE TABLE FalDocumento
(
    IdDocu               INT8 NOT NULL,
    TipoDocu             SMALLINT NOT NULL,
    EstadoDocu           SMALLINT NOT NULL,
    NroDocu              VARCHAR(30),
    IdTalonario          INT,
    TipoFirmaReq         SMALLINT,
    StorageKey           VARCHAR(255),
    HashDocu             VARCHAR(128),
    FhGeneracion         DATETIME YEAR TO SECOND,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalDoc PRIMARY KEY (IdDocu)
);

CREATE TABLE FalActaDocumento
(
    IdActa               INT8 NOT NULL,
    IdDocu               INT8 NOT NULL,
    RolDocuActa          SMALLINT NOT NULL,
    SiPrincipal          SMALLINT NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalActDoc PRIMARY KEY (IdActa, IdDocu)
);

CREATE TABLE FalDocumentoFirma
(
    Id                   INT8 NOT NULL,
    IdDocu               INT8 NOT NULL,
    TipoFirma            SMALLINT NOT NULL,
    EstadoFirma          SMALLINT NOT NULL,
    IdUserFirma          CHAR(36),
    FhSolicitud          DATETIME YEAR TO SECOND,
    FhFirma              DATETIME YEAR TO SECOND,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalDocFir PRIMARY KEY (Id)
);

CREATE TABLE FalDocumentoObservacion
(
    Id                   INT8 NOT NULL,
    IdDocu               INT8 NOT NULL,
    ObsDocu              VARCHAR(255) NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalDocObs PRIMARY KEY (Id)
);
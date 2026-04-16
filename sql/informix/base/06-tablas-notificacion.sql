-- 06-tablas-notificacion.sql
-- Generado desde spec/13-ddl

CREATE TABLE FalNotificacion
(
    IdNotif              INT8 NOT NULL,
    TipoNotif            SMALLINT NOT NULL,
    EstadoNotif          SMALLINT NOT NULL,
    IdDocu               INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    FhGeneracion         DATETIME YEAR TO SECOND,
    FhEmision            DATETIME YEAR TO SECOND,
    CanalNotif           SMALLINT NOT NULL,
    SiRequiereAcuse      SMALLINT NOT NULL,
    SiAcuseRecibido      SMALLINT NOT NULL,
    EstadoAcuse          SMALLINT,
    FhAcuse              DATETIME YEAR TO SECOND,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalNotif PRIMARY KEY (IdNotif)
);

CREATE TABLE FalNotificacionIntento
(
    Id                   INT8 NOT NULL,
    IdNotif              INT8 NOT NULL,
    NroIntento           SMALLINT NOT NULL,
    CanalNotif           SMALLINT NOT NULL,
    TipoDestNotif        SMALLINT NOT NULL,
    DestNotif            VARCHAR(150) NOT NULL,
    EstadoIntento        SMALLINT NOT NULL,
    FhIntento            DATETIME YEAR TO SECOND NOT NULL,
    FhResultado          DATETIME YEAR TO SECOND,
    ResultadoIntento     VARCHAR(255),
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalNotInt PRIMARY KEY (Id)
);

CREATE TABLE FalNotificacionAcuse
(
    Id                   INT8 NOT NULL,
    IdNotif              INT8 NOT NULL,
    IdIntentoNotif       INT8,
    TipoAcuse            SMALLINT NOT NULL,
    EstadoAcuse          SMALLINT NOT NULL,
    FhAcuse              DATETIME YEAR TO SECOND NOT NULL,
    StorageKeyAcuse      VARCHAR(255),
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalNotAcu PRIMARY KEY (Id)
);

CREATE TABLE FalNotificacionObservacion
(
    Id                   INT8 NOT NULL,
    IdNotif              INT8 NOT NULL,
    ObsNotif             VARCHAR(255) NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalNotObs PRIMARY KEY (Id)
);
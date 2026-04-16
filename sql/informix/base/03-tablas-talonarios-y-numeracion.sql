-- 03-tablas-talonarios-y-numeracion.sql
-- Generado desde spec/13-ddl

CREATE TABLE NumPolitica
(
    IdPolNum             INT NOT NULL,
    NomPolNum            VARCHAR(48) NOT NULL,
    TipoTalonario        SMALLINT NOT NULL,
    SiUsaPrefijo         SMALLINT NOT NULL,
    Prefijo              VARCHAR(10),
    SiUsaAnio            SMALLINT NOT NULL,
    SiUsaSerie           SMALLINT NOT NULL,
    LongNro              SMALLINT NOT NULL,
    SepPrefAnio          CHAR(1),
    SepAnioSerie         CHAR(1),
    SepSerieNro          CHAR(1),
    ProxNro              INT,
    SiActiva             SMALLINT NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    CONSTRAINT PkNumPol PRIMARY KEY (IdPolNum)
);

CREATE TABLE NumTalonario
(
    IdTalonario          INT NOT NULL,
    IdPolNum             INT NOT NULL,
    CodTalonario         VARCHAR(4) NOT NULL,
    TipoTalonario        SMALLINT NOT NULL,
    AmbitoTalonario      SMALLINT NOT NULL,
    Serie                VARCHAR(10),
    NroDesde             INT,
    NroHasta             INT,
    UltNroUsado          INT,
    SiBloqueado          SMALLINT NOT NULL,
    CodDesbloqueo        CHAR(36),
    SiActiva             SMALLINT NOT NULL,
    ObsTalonario         VARCHAR(255),
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkNumTal PRIMARY KEY (IdTalonario)
);

CREATE TABLE NumTalonarioDependencia
(
    IdTalonario          INT NOT NULL,
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    FhDesde              DATETIME YEAR TO DAY NOT NULL,
    FhHasta              DATETIME YEAR TO DAY,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkNumTalDep PRIMARY KEY (IdTalonario, IdDep, VerDep, FhDesde)
);

CREATE TABLE NumTalonarioInspector
(
    IdTalonario          INT NOT NULL,
    IdInsp               INT NOT NULL,
    VerInsp              SMALLINT NOT NULL,
    FhDesde              DATETIME YEAR TO DAY NOT NULL,
    FhHasta              DATETIME YEAR TO DAY,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkNumTalIns PRIMARY KEY (IdTalonario, IdInsp, VerInsp, FhDesde)
);

CREATE TABLE NumTalonarioMovimiento
(
    Id                   INT8 NOT NULL,
    IdTalonario          INT NOT NULL,
    NroUsado             INT NOT NULL,
    EstadoNro            SMALLINT NOT NULL,
    MotivoMov            VARCHAR(80),
    IdActa               INT8,
    IdDep                INT,
    VerDep               SMALLINT,
    IdInsp               INT,
    VerInsp              SMALLINT,
    FhMov                DATETIME YEAR TO SECOND NOT NULL,
    IdUserMov            CHAR(36) NOT NULL,
    CONSTRAINT PkNumTalMov PRIMARY KEY (Id)
);
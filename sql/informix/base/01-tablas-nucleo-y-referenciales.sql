-- 01-tablas-nucleo-y-referenciales.sql
-- Generado desde spec/13-ddl

CREATE TABLE FalDependencia
(
    IdDep                INT NOT NULL,
    CodDep               VARCHAR(20),
    NomDep               VARCHAR(120) NOT NULL,
    IdDepPadre           INT,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkFalDep PRIMARY KEY (IdDep)
);

CREATE TABLE FalDependenciaVersion
(
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    NomDep               VARCHAR(120) NOT NULL,
    IdDepPadre           INT,
    VerDepPadre          SMALLINT,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkFalDepVer PRIMARY KEY (IdDep, VerDep)
);

CREATE TABLE FalInspector
(
    IdInsp               INT NOT NULL,
    IdUser               INT NOT NULL,
    LegajoInsp           INT NOT NULL,
    NomInsp              VARCHAR(120) NOT NULL,
    SiActivo             SMALLINT NOT NULL,
    CONSTRAINT PkFalInsp PRIMARY KEY (IdInsp)
);

CREATE TABLE FalInspectorVersion
(
    IdInsp               INT NOT NULL,
    VerInsp              SMALLINT NOT NULL,
    LegajoInsp           INT NOT NULL,
    NomInsp              VARCHAR(120) NOT NULL,
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    SiActivo             SMALLINT NOT NULL,
    CONSTRAINT PkFalInspVer PRIMARY KEY (IdInsp, VerInsp)
);

CREATE TABLE FalMedidaPreventiva
(
    IdMedPrev            INT NOT NULL,
    VerMedPrev           SMALLINT NOT NULL,
    NomMedPrev           VARCHAR(120) NOT NULL,
    DescMedPrev          VARCHAR(255),
    SiActiva             SMALLINT NOT NULL,
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    CONSTRAINT PkFalMedPrev PRIMARY KEY (IdMedPrev, VerMedPrev)
);

CREATE TABLE FalActa
(
    Id                   INT8 NOT NULL,
    IdTecnico            CHAR(36) NOT NULL,
    NroActa              VARCHAR(20) NOT NULL,
    TipoActa             SMALLINT NOT NULL,
    OrigenCaptura        SMALLINT NOT NULL,
    FhActa               DATETIME YEAR TO SECOND NOT NULL,
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    IdInsp               INT NOT NULL,
    VerInsp              SMALLINT NOT NULL,
    IdTcaInfr            CHAR(5),
    VerTcaInfr           SMALLINT,
    AltInfr              INT,
    IdTcaE1Infr          CHAR(5),
    VerTcaE1Infr         SMALLINT,
    IdTcaE2Infr          CHAR(5),
    VerTcaE2Infr         SMALLINT,
    IdLocInfr            CHAR(2),
    VerLocInfr           SMALLINT,
    IdBarInfr            SMALLINT,
    SiDomTxtInfr         SMALLINT NOT NULL,
    DomTxtInfr           VARCHAR(150),
    SiEjeUrb             SMALLINT,
    NomInfct             VARCHAR(64),
    DocPrefInfct         SMALLINT,
    DocNroInfct          INT,
    DocDigVerInfct       SMALLINT,
    TipoPersInfct        SMALLINT,
    IdProvInfct          SMALLINT,
    VerProvInfct         SMALLINT,
    IdMuniInfct          INT,
    VerMuniInfct         SMALLINT,
    IdDptoInfct          INT,
    VerDptoInfct         SMALLINT,
    IdLocInfct           INT8,
    VerLocInfct          SMALLINT,
    IdLocCenInfct        INT8,
    VerLocCenInfct       SMALLINT,
    IdCalleInfct         INT8,
    VerCalleInfct        SMALLINT,
    IdLocMalvInfct       CHAR(2),
    VerLocMalvInfct      SMALLINT,
    IdTcaInfct           CHAR(5),
    VerTcaInfct          SMALLINT,
    IdBarInfct           SMALLINT,
    SiCalleTxtInfct      SMALLINT NOT NULL,
    CalleTxtInfct        VARCHAR(120),
    SiNormParcialInfct   SMALLINT NOT NULL,
    AltInfct             INT,
    PisoInfct            VARCHAR(10),
    DeptoInfct           VARCHAR(10),
    ObsDomInfct          VARCHAR(120),
    CodPosInfct          VARCHAR(10),
    IdProvLicEmi         SMALLINT,
    VerProvLicEmi        SMALLINT,
    IdMuniLicEmi         INT,
    VerMuniLicEmi        SMALLINT,
    IdDptoLicEmi         INT,
    VerDptoLicEmi        SMALLINT,
    TipoJurLicEmi        SMALLINT,
    ObsActa              LVARCHAR,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    LatInfr              DECIMAL(12,8),
    LonInfr              DECIMAL(12,8),
    CONSTRAINT PkFalActa PRIMARY KEY (Id)
);

CREATE TABLE FalActaEvidencia
(
    Id                   INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    TipoEvid             SMALLINT NOT NULL,
    StorageKey           VARCHAR(255) NOT NULL,
    NomArchivo           VARCHAR(120),
    MimeType             VARCHAR(80),
    HashEvid             VARCHAR(128),
    ObsEvid              VARCHAR(255),
    OrdenEvid            SMALLINT,
    FhCaptura            DATETIME YEAR TO SECOND,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalActEvi PRIMARY KEY (Id)
);

CREATE TABLE FalActaEvento
(
    Id                   INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    FhEvt                DATETIME YEAR TO SECOND NOT NULL,
    TipoEvt              SMALLINT NOT NULL,
    OrigenEvt            SMALLINT NOT NULL,
    BloqueFunc           SMALLINT NOT NULL,
    EstProcAnt           SMALLINT,
    EstProcNvo           SMALLINT,
    SitAdmAnt            SMALLINT,
    SitAdmNva            SMALLINT,
    IdDocuRel            INT8,
    IdNotifRel           INT8,
    IdPresRel            INT8,
    IdUserEvt            CHAR(36),
    SiEvtCierre          SMALLINT NOT NULL,
    SiEvtExt             SMALLINT NOT NULL,
    SiPermiteReing       SMALLINT NOT NULL,
    CONSTRAINT PkFalActEv PRIMARY KEY (Id)
);

CREATE TABLE FalObservacion
(
    Id                   INT8 NOT NULL,
    IdRef                SMALLINT NOT NULL,
    IdFk                 INT8 NOT NULL,
    Obs                  VARCHAR(255) NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUser               CHAR(36) NOT NULL,
    CONSTRAINT PkFalObs PRIMARY KEY (Id)
);
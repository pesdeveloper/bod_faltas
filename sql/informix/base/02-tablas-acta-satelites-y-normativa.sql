-- 02-tablas-acta-satelites-y-normativa.sql
-- Generado desde spec/13-ddl

CREATE TABLE FalAlcoholimetro
(
    IdAlcoholimetro      INT NOT NULL,
    CodAlcoholimetro     VARCHAR(20) NOT NULL,
    NroSerie             VARCHAR(64) NOT NULL,
    Marca                VARCHAR(64),
    Modelo               VARCHAR(64),
    SiDeshabilitado      SMALLINT NOT NULL,
    ObsDeshabilitado     VARCHAR(255),
    SiActivo             SMALLINT NOT NULL,
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalAlco PRIMARY KEY (IdAlcoholimetro)
);

CREATE TABLE FalAlcoholimetroVersion
(
    IdAlcoholimetro      INT NOT NULL,
    VerAlcoholimetro     SMALLINT NOT NULL,
    CodAlcoholimetro     VARCHAR(20) NOT NULL,
    NroSerie             VARCHAR(64) NOT NULL,
    Marca                VARCHAR(64),
    Modelo               VARCHAR(64),
    SiDeshabilitado      SMALLINT NOT NULL,
    ObsDeshabilitado     VARCHAR(255),
    SiActivo             SMALLINT NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    CONSTRAINT PkFalAlcoVer PRIMARY KEY (IdAlcoholimetro, VerAlcoholimetro)
);

CREATE TABLE RubroCom
(
    IdRub                SMALLINT NOT NULL,
    NomRub               CHAR(64) NOT NULL,
    SiDeshabilitado      SMALLINT NOT NULL,
    CONSTRAINT PkRubCom PRIMARY KEY (IdRub)
);

CREATE TABLE RubroComVersion
(
    IdRub                SMALLINT NOT NULL,
    VerRub               SMALLINT NOT NULL,
    NomRub               CHAR(64) NOT NULL,
    SiDeshabilitado      SMALLINT NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    CONSTRAINT PkRubComVer PRIMARY KEY (IdRub, VerRub)
);

CREATE TABLE FalActaTransito
(
    IdActa               INT8 NOT NULL,
    NroLic               CHAR(8),
    IdProvLic            SMALLINT,
    VerProvLic           SMALLINT,
    IdMuniLic            INT,
    VerMuniLic           SMALLINT,
    IdDptoLic            INT,
    VerDptoLic           SMALLINT,
    SiRetLic             SMALLINT NOT NULL,
    SiRetVeh             SMALLINT NOT NULL,
    SiCtrlAlcoh          SMALLINT NOT NULL,
    IdAlcoholimetro      INT,
    VerAlcoholimetro     SMALLINT,
    TipoPruebaAlcohFin   SMALLINT,
    CantMedAlcoh         SMALLINT,
    ResAlcohFin          DECIMAL(4,2),
    UniMedAlcoh          CHAR(3),
    CONSTRAINT PkFalActTra PRIMARY KEY (IdActa)
);

CREATE TABLE FalActaTransitoAlcoholemia
(
    Id                   INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    OrdenMed             SMALLINT NOT NULL,
    TipoPrueba           SMALLINT NOT NULL,
    ResCuali             SMALLINT,
    ResNum               DECIMAL(4,2),
    UniMed               CHAR(3),
    SiResFin             SMALLINT NOT NULL,
    CONSTRAINT PkFalActTrAlc PRIMARY KEY (Id)
);

CREATE TABLE FalActaVehiculo
(
    IdActa               INT8 NOT NULL,
    DomVeh               VARCHAR(10),
    TipoVeh              SMALLINT,
    TipoVehTxt           VARCHAR(64),
    MarcaVeh             VARCHAR(64),
    ModeloVeh            VARCHAR(128),
    CONSTRAINT PkFalActVeh PRIMARY KEY (IdActa)
);

CREATE TABLE FalActaContravencion
(
    IdActa               INT8 NOT NULL,
    IdSuj                SMALLINT,
    IdBie                INT,
    Circ                 SMALLINT,
    Secc                 CHAR(2),
    Frac                 CHAR(7),
    Mza                  CHAR(7),
    Parc                 CHAR(7),
    UFun                 CHAR(7),
    UComp                CHAR(3),
    OrigenNomencl        SMALLINT,
    IdRub                SMALLINT,
    VerRub               SMALLINT,
    AmbitoCtv            SMALLINT,
    AmbitoCtvTxt         VARCHAR(80),
    CONSTRAINT PkFalActCon PRIMARY KEY (IdActa)
);

CREATE TABLE FalActaSustanciasAlimenticias
(
    IdActa               INT8 NOT NULL,
    IdRub                SMALLINT,
    VerRub               SMALLINT,
    AmbitoAct            SMALLINT,
    AmbitoActTxt         VARCHAR(80),
    CONSTRAINT PkFalActSusAl PRIMARY KEY (IdActa)
);

CREATE TABLE FalActaMedidaPreventiva
(
    Id                   INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    IdMedPrev            INT NOT NULL,
    VerMedPrev           SMALLINT NOT NULL,
    MedPrevTxt           VARCHAR(255),
    FhAlta               DATETIME YEAR TO SECOND NOT NULL,
    IdUserAlta           CHAR(36) NOT NULL,
    CONSTRAINT PkFalActMedPr PRIMARY KEY (Id)
);

CREATE TABLE FalNormativaFaltas
(
    IdNorma              VARCHAR(8) NOT NULL,
    VerNorma             SMALLINT NOT NULL,
    NomNorma             VARCHAR(64) NOT NULL,
    SiTransito           SMALLINT NOT NULL,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkFalNormFal PRIMARY KEY (IdNorma, VerNorma)
);

CREATE TABLE FalArticuloNormativaFaltas
(
    IdNorma              VARCHAR(8) NOT NULL,
    VerNorma             SMALLINT NOT NULL,
    IdArtNorma           VARCHAR(8) NOT NULL,
    VerArtNorma          SMALLINT NOT NULL,
    NomArtNorma          VARCHAR(64) NOT NULL,
    UniMedBase           SMALLINT,
    ValorBase            DECIMAL(16,2),
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkFalArtNorm PRIMARY KEY (IdNorma, VerNorma, IdArtNorma, VerArtNorma)
);

CREATE TABLE FalTarifarioUnidadFaltas
(
    IdTarifario          INT NOT NULL,
    UniMed               SMALLINT NOT NULL,
    Valor                DECIMAL(16,2) NOT NULL,
    FhVigDesde           DATETIME YEAR TO DAY NOT NULL,
    FhVigHasta           DATETIME YEAR TO DAY,
    SiActiva             SMALLINT NOT NULL,
    CONSTRAINT PkFalTarUni PRIMARY KEY (IdTarifario)
);

CREATE TABLE FalActaArticuloInfringido
(
    Id                   INT8 NOT NULL,
    IdActa               INT8 NOT NULL,
    IdNorma              VARCHAR(8) NOT NULL,
    VerNorma             SMALLINT NOT NULL,
    IdArtNorma           VARCHAR(8) NOT NULL,
    VerArtNorma          SMALLINT NOT NULL,
    UniMedBase           SMALLINT,
    UniMedApl            SMALLINT,
    IdTarifario          INT,
    ValorBase            DECIMAL(16,2),
    ValorApl             DECIMAL(16,2),
    SiActiva             SMALLINT NOT NULL,
    SiEditManual         SMALLINT NOT NULL,
    CONSTRAINT PkFalActArtI PRIMARY KEY (Id)
);

CREATE TABLE FalActaArticuloAuditoria
(
    Id                   INT8 NOT NULL,
    IdActaArt            INT8 NOT NULL,
    FhEvt                DATETIME YEAR TO SECOND NOT NULL,
    IdUserEvt            CHAR(36) NOT NULL,
    TipoAccion           SMALLINT NOT NULL,
    CampoMod             VARCHAR(40) NOT NULL,
    ValorAnt             VARCHAR(255),
    ValorNvo             VARCHAR(255),
    Motivo               VARCHAR(255),
    CONSTRAINT PkFalActArtA PRIMARY KEY (Id)
);
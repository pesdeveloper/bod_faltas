-- Script maestro de referencia.
-- Ejecutar los archivos en este orden, o concatenarlos para tu pipeline.

-- >>> 00-secuencias.sql

-- Secuencias base para Informix 12.10

CREATE SEQUENCE SeqFalActa;
CREATE SEQUENCE SeqFalActEvi;
CREATE SEQUENCE SeqFalActEv;
CREATE SEQUENCE SeqFalObs;
CREATE SEQUENCE SeqFalDep;
CREATE SEQUENCE SeqFalInsp;
CREATE SEQUENCE SeqFalActTrAlc;
CREATE SEQUENCE SeqFalActMedPr;
CREATE SEQUENCE SeqFalTarUni;
CREATE SEQUENCE SeqFalActArtI;
CREATE SEQUENCE SeqFalActArtA;
CREATE SEQUENCE SeqNumPol;
CREATE SEQUENCE SeqNumTal;
CREATE SEQUENCE SeqNumTalMov;
CREATE SEQUENCE SeqFalAlco;
CREATE SEQUENCE SeqRubCom;
CREATE SEQUENCE SeqFalDoc;
CREATE SEQUENCE SeqFalDocFir;
CREATE SEQUENCE SeqFalDocObs;
CREATE SEQUENCE SeqFalNotif;
CREATE SEQUENCE SeqFalNotInt;
CREATE SEQUENCE SeqFalNotAcu;
CREATE SEQUENCE SeqFalNotObs;
CREATE SEQUENCE SeqStorBack;
CREATE SEQUENCE SeqStorPol;

-- >>> 01-tablas-nucleo-y-referenciales.sql

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

-- >>> 02-tablas-acta-satelites-y-normativa.sql

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

-- >>> 03-tablas-talonarios-y-numeracion.sql

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

-- >>> 04-tablas-storage-documental.sql

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

-- >>> 05-tablas-documentales.sql

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

-- >>> 06-tablas-notificacion.sql

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

-- >>> 07-tablas-snapshot.sql

-- 07-tablas-snapshot.sql
-- Generado desde spec/13-ddl

CREATE TABLE FalActaSnapshot
(
    IdActa               INT8 NOT NULL,
    FhActa               DATETIME YEAR TO SECOND NOT NULL,
    IdDep                INT NOT NULL,
    VerDep               SMALLINT NOT NULL,
    IdInsp               INT NOT NULL,
    VerInsp              SMALLINT NOT NULL,
    BloqueActual         SMALLINT NOT NULL,
    EstProcAct           SMALLINT NOT NULL,
    SitAdmAct            SMALLINT NOT NULL,
    CodBandeja           VARCHAR(40) NOT NULL,
    SiVisibleBandeja     SMALLINT NOT NULL,
    Prioridad            SMALLINT,
    SiNotifActa          SMALLINT NOT NULL,
    SiNotifActaProc      SMALLINT NOT NULL,
    SiNotifActaAcusePend SMALLINT NOT NULL,
    SiNotifMedPrev       SMALLINT NOT NULL,
    SiNotifMedPrevProc   SMALLINT NOT NULL,
    SiNotifMedPrevAcusePend SMALLINT NOT NULL,
    SiNotifFallo         SMALLINT NOT NULL,
    SiNotifFalloProc     SMALLINT NOT NULL,
    SiNotifFalloAcusePend SMALLINT NOT NULL,
    CantReintNotif       SMALLINT NOT NULL,
    SiPagoVolunt         SMALLINT NOT NULL,
    FhPagoVolunt         DATETIME YEAR TO SECOND,
    MontoActa            DECIMAL(16,2),
    SiPagoTotal          SMALLINT NOT NULL,
    SiPlanPago           SMALLINT NOT NULL,
    CantCuotasPlan       SMALLINT,
    ValorCuotaPlan       DECIMAL(16,2),
    CantCaidasPlan       SMALLINT NOT NULL,
    SiGestionExt         SMALLINT NOT NULL,
    TipoGestionExt       SMALLINT,
    SiReingresoGestionExt SMALLINT NOT NULL,
    ResultadoGestionExt  SMALLINT,
    FhVtoPresentacion    DATETIME YEAR TO SECOND,
    FhVtoApelacion       DATETIME YEAR TO SECOND,
    FhVtoApremio         DATETIME YEAR TO SECOND,
    IdEvtUlt             INT8,
    IdDocuUlt            INT8,
    IdNotifUlt           INT8,
    FhUltMod             DATETIME YEAR TO SECOND,
    IdUserUltMod         CHAR(36),
    FhSnapshot           DATETIME YEAR TO SECOND NOT NULL,
    CONSTRAINT PkFalSnap PRIMARY KEY (IdActa)
);

-- >>> 90-foreign-keys.sql

-- Foreign keys base para Informix 12.10
-- Nota: se omiten FKs hacia catálogos externos no incluidos en este bloque.
-- Nota: se omiten FalDocumento.StorageKey y FalNotificacionAcuse.StorageKeyAcuse por diferencia de longitud respecto de StorObjeto.StorageKey.

ALTER TABLE FalActa ADD CONSTRAINT FkFalActa_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE FalActa ADD CONSTRAINT FkFalActa_FalInspVer FOREIGN KEY (IdInsp, VerInsp) REFERENCES FalInspectorVersion (IdInsp, VerInsp);
ALTER TABLE FalActaEvidencia ADD CONSTRAINT FkFalActEvi_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaEvidencia ADD CONSTRAINT FkFalActEvi_StorObj FOREIGN KEY (StorageKey) REFERENCES StorObjeto (StorageKey);
ALTER TABLE FalActaEvento ADD CONSTRAINT FkFalActEv_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaEvento ADD CONSTRAINT FkFalActEv_FalDoc FOREIGN KEY (IdDocuRel) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalActaEvento ADD CONSTRAINT FkFalActEv_FalNotif FOREIGN KEY (IdNotifRel) REFERENCES FalNotificacion (IdNotif);
ALTER TABLE FalDependencia ADD CONSTRAINT FkFalDep_FalDep FOREIGN KEY (IdDepPadre) REFERENCES FalDependencia (IdDep);
ALTER TABLE FalDependenciaVersion ADD CONSTRAINT FkFalDepVer_FalDep FOREIGN KEY (IdDep) REFERENCES FalDependencia (IdDep);
ALTER TABLE FalDependenciaVersion ADD CONSTRAINT FkFalDepVer_FalDepVer FOREIGN KEY (IdDepPadre, VerDepPadre) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE FalInspectorVersion ADD CONSTRAINT FkFalInspVer_FalInsp FOREIGN KEY (IdInsp) REFERENCES FalInspector (IdInsp);
ALTER TABLE FalInspectorVersion ADD CONSTRAINT FkFalInspVer_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE FalMedidaPreventiva ADD CONSTRAINT FkFalMedPrev_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE FalActaTransito ADD CONSTRAINT FkFalActTra_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaTransito ADD CONSTRAINT FkFalActTra_FalAlcoVer FOREIGN KEY (IdAlcoholimetro, VerAlcoholimetro) REFERENCES FalAlcoholimetroVersion (IdAlcoholimetro, VerAlcoholimetro);
ALTER TABLE FalActaTransitoAlcoholemia ADD CONSTRAINT FkFalActTrAlc_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaVehiculo ADD CONSTRAINT FkFalActVeh_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaContravencion ADD CONSTRAINT FkFalActCon_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaContravencion ADD CONSTRAINT FkFalActCon_RubComVer FOREIGN KEY (IdRub, VerRub) REFERENCES RubroComVersion (IdRub, VerRub);
ALTER TABLE FalActaSustanciasAlimenticias ADD CONSTRAINT FkFalActSusAl_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaSustanciasAlimenticias ADD CONSTRAINT FkFalActSusAl_RubComVer FOREIGN KEY (IdRub, VerRub) REFERENCES RubroComVersion (IdRub, VerRub);
ALTER TABLE FalActaMedidaPreventiva ADD CONSTRAINT FkFalActMedPr_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaMedidaPreventiva ADD CONSTRAINT FkFalActMedPr_FalMedPrev FOREIGN KEY (IdMedPrev, VerMedPrev) REFERENCES FalMedidaPreventiva (IdMedPrev, VerMedPrev);
ALTER TABLE FalArticuloNormativaFaltas ADD CONSTRAINT FkFalArtNorm_FalNormFal FOREIGN KEY (IdNorma, VerNorma) REFERENCES FalNormativaFaltas (IdNorma, VerNorma);
ALTER TABLE FalActaArticuloInfringido ADD CONSTRAINT FkFalActArtI_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaArticuloInfringido ADD CONSTRAINT FkFalActArtI_FalArtNorm FOREIGN KEY (IdNorma, VerNorma, IdArtNorma, VerArtNorma) REFERENCES FalArticuloNormativaFaltas (IdNorma, VerNorma, IdArtNorma, VerArtNorma);
ALTER TABLE FalActaArticuloInfringido ADD CONSTRAINT FkFalActArtI_FalTarUni FOREIGN KEY (IdTarifario) REFERENCES FalTarifarioUnidadFaltas (IdTarifario);
ALTER TABLE FalActaArticuloAuditoria ADD CONSTRAINT FkFalActArtA_FalActArtI FOREIGN KEY (IdActaArt) REFERENCES FalActaArticuloInfringido (Id);
ALTER TABLE NumTalonario ADD CONSTRAINT FkNumTal_NumPol FOREIGN KEY (IdPolNum) REFERENCES NumPolitica (IdPolNum);
ALTER TABLE NumTalonarioDependencia ADD CONSTRAINT FkNumTalDep_NumTal FOREIGN KEY (IdTalonario) REFERENCES NumTalonario (IdTalonario);
ALTER TABLE NumTalonarioDependencia ADD CONSTRAINT FkNumTalDep_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE NumTalonarioInspector ADD CONSTRAINT FkNumTalIns_NumTal FOREIGN KEY (IdTalonario) REFERENCES NumTalonario (IdTalonario);
ALTER TABLE NumTalonarioInspector ADD CONSTRAINT FkNumTalIns_FalInspVer FOREIGN KEY (IdInsp, VerInsp) REFERENCES FalInspectorVersion (IdInsp, VerInsp);
ALTER TABLE NumTalonarioMovimiento ADD CONSTRAINT FkNumTalMov_NumTal FOREIGN KEY (IdTalonario) REFERENCES NumTalonario (IdTalonario);
ALTER TABLE NumTalonarioMovimiento ADD CONSTRAINT FkNumTalMov_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE NumTalonarioMovimiento ADD CONSTRAINT FkNumTalMov_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE NumTalonarioMovimiento ADD CONSTRAINT FkNumTalMov_FalInspVer FOREIGN KEY (IdInsp, VerInsp) REFERENCES FalInspectorVersion (IdInsp, VerInsp);
ALTER TABLE FalAlcoholimetroVersion ADD CONSTRAINT FkFalAlcoVer_FalAlco FOREIGN KEY (IdAlcoholimetro) REFERENCES FalAlcoholimetro (IdAlcoholimetro);
ALTER TABLE RubroComVersion ADD CONSTRAINT FkRubComVer_RubCom FOREIGN KEY (IdRub) REFERENCES RubroCom (IdRub);
ALTER TABLE FalDocumento ADD CONSTRAINT FkFalDoc_NumTal FOREIGN KEY (IdTalonario) REFERENCES NumTalonario (IdTalonario);
ALTER TABLE FalActaDocumento ADD CONSTRAINT FkFalActDoc_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaDocumento ADD CONSTRAINT FkFalActDoc_FalDoc FOREIGN KEY (IdDocu) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalDocumentoFirma ADD CONSTRAINT FkFalDocFir_FalDoc FOREIGN KEY (IdDocu) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalDocumentoObservacion ADD CONSTRAINT FkFalDocObs_FalDoc FOREIGN KEY (IdDocu) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalNotificacion ADD CONSTRAINT FkFalNotif_FalDoc FOREIGN KEY (IdDocu) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalNotificacion ADD CONSTRAINT FkFalNotif_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalNotificacionIntento ADD CONSTRAINT FkFalNotInt_FalNotif FOREIGN KEY (IdNotif) REFERENCES FalNotificacion (IdNotif);
ALTER TABLE FalNotificacionAcuse ADD CONSTRAINT FkFalNotAcu_FalNotif FOREIGN KEY (IdNotif) REFERENCES FalNotificacion (IdNotif);
ALTER TABLE FalNotificacionAcuse ADD CONSTRAINT FkFalNotAcu_FalNotInt FOREIGN KEY (IdIntentoNotif) REFERENCES FalNotificacionIntento (Id);
ALTER TABLE FalNotificacionObservacion ADD CONSTRAINT FkFalNotObs_FalNotif FOREIGN KEY (IdNotif) REFERENCES FalNotificacion (IdNotif);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalActa FOREIGN KEY (IdActa) REFERENCES FalActa (Id);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalDepVer FOREIGN KEY (IdDep, VerDep) REFERENCES FalDependenciaVersion (IdDep, VerDep);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalInspVer FOREIGN KEY (IdInsp, VerInsp) REFERENCES FalInspectorVersion (IdInsp, VerInsp);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalActEv FOREIGN KEY (IdEvtUlt) REFERENCES FalActaEvento (Id);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalDoc FOREIGN KEY (IdDocuUlt) REFERENCES FalDocumento (IdDocu);
ALTER TABLE FalActaSnapshot ADD CONSTRAINT FkFalSnap_FalNotif FOREIGN KEY (IdNotifUlt) REFERENCES FalNotificacion (IdNotif);
ALTER TABLE StorPolitica ADD CONSTRAINT FkStorPol_StorBack FOREIGN KEY (IdStorageBackend) REFERENCES StorBackend (IdStorageBackend);
ALTER TABLE StorObjeto ADD CONSTRAINT FkStorObj_StorBack FOREIGN KEY (IdStorageBackend) REFERENCES StorBackend (IdStorageBackend);

-- >>> 91-indexes.sql

-- Índices estructurales mínimos

CREATE UNIQUE INDEX UxFalActa01 ON FalActa (IdTecnico);
CREATE UNIQUE INDEX UxFalActa02 ON FalActa (NroActa);
CREATE UNIQUE INDEX UxFalActTrAlc01 ON FalActaTransitoAlcoholemia (IdActa, OrdenMed);
CREATE UNIQUE INDEX UxNumTal01 ON NumTalonario (CodTalonario);
CREATE UNIQUE INDEX UxNumTalMov01 ON NumTalonarioMovimiento (IdTalonario, NroUsado);
CREATE UNIQUE INDEX UxFalAlco01 ON FalAlcoholimetro (CodAlcoholimetro);
CREATE UNIQUE INDEX UxFalAlco02 ON FalAlcoholimetro (NroSerie);
CREATE UNIQUE INDEX UxFalNotInt01 ON FalNotificacionIntento (IdNotif, NroIntento);

CREATE INDEX IxFalActa01 ON FalActa (IdDep, VerDep);
CREATE INDEX IxFalActa02 ON FalActa (IdInsp, VerInsp);
CREATE INDEX IxFalActEvi01 ON FalActaEvidencia (IdActa);
CREATE INDEX IxFalActEvi02 ON FalActaEvidencia (StorageKey);
CREATE INDEX IxFalActEv01 ON FalActaEvento (IdActa);
CREATE INDEX IxFalActEv02 ON FalActaEvento (IdDocuRel);
CREATE INDEX IxFalActEv03 ON FalActaEvento (IdNotifRel);
CREATE INDEX IxFalDep01 ON FalDependencia (IdDepPadre);
CREATE INDEX IxFalDepVer01 ON FalDependenciaVersion (IdDepPadre, VerDepPadre);
CREATE INDEX IxFalInspVer01 ON FalInspectorVersion (IdDep, VerDep);
CREATE INDEX IxFalMedPrev01 ON FalMedidaPreventiva (IdDep, VerDep);
CREATE INDEX IxFalActTra01 ON FalActaTransito (IdAlcoholimetro, VerAlcoholimetro);
CREATE INDEX IxFalActTrAlc01 ON FalActaTransitoAlcoholemia (IdActa);
CREATE INDEX IxFalActCon01 ON FalActaContravencion (IdRub, VerRub);
CREATE INDEX IxFalActSusAl01 ON FalActaSustanciasAlimenticias (IdRub, VerRub);
CREATE INDEX IxFalActMedPr01 ON FalActaMedidaPreventiva (IdActa);
CREATE INDEX IxFalActMedPr02 ON FalActaMedidaPreventiva (IdMedPrev, VerMedPrev);
CREATE INDEX IxFalActArtI01 ON FalActaArticuloInfringido (IdActa);
CREATE INDEX IxFalActArtI02 ON FalActaArticuloInfringido (IdNorma, VerNorma, IdArtNorma, VerArtNorma);
CREATE INDEX IxFalActArtI03 ON FalActaArticuloInfringido (IdTarifario);
CREATE INDEX IxFalActArtA01 ON FalActaArticuloAuditoria (IdActaArt);
CREATE INDEX IxNumTal01 ON NumTalonario (IdPolNum);
CREATE INDEX IxNumTalDep01 ON NumTalonarioDependencia (IdDep, VerDep);
CREATE INDEX IxNumTalIns01 ON NumTalonarioInspector (IdInsp, VerInsp);
CREATE INDEX IxNumTalMov01 ON NumTalonarioMovimiento (IdTalonario);
CREATE INDEX IxNumTalMov02 ON NumTalonarioMovimiento (IdActa);
CREATE INDEX IxNumTalMov03 ON NumTalonarioMovimiento (IdDep, VerDep);
CREATE INDEX IxNumTalMov04 ON NumTalonarioMovimiento (IdInsp, VerInsp);
CREATE INDEX IxFalDoc01 ON FalDocumento (IdTalonario);
CREATE INDEX IxFalActDoc01 ON FalActaDocumento (IdDocu);
CREATE INDEX IxFalDocFir01 ON FalDocumentoFirma (IdDocu);
CREATE INDEX IxFalDocObs01 ON FalDocumentoObservacion (IdDocu);
CREATE INDEX IxFalNotif01 ON FalNotificacion (IdDocu);
CREATE INDEX IxFalNotif02 ON FalNotificacion (IdActa);
CREATE INDEX IxFalNotInt01 ON FalNotificacionIntento (IdNotif);
CREATE INDEX IxFalNotAcu01 ON FalNotificacionAcuse (IdNotif);
CREATE INDEX IxFalNotAcu02 ON FalNotificacionAcuse (IdIntentoNotif);
CREATE INDEX IxFalNotObs01 ON FalNotificacionObservacion (IdNotif);
CREATE INDEX IxFalSnap01 ON FalActaSnapshot (IdDep, VerDep);
CREATE INDEX IxFalSnap02 ON FalActaSnapshot (IdInsp, VerInsp);
CREATE INDEX IxFalSnap03 ON FalActaSnapshot (IdEvtUlt);
CREATE INDEX IxFalSnap04 ON FalActaSnapshot (IdDocuUlt);
CREATE INDEX IxFalSnap05 ON FalActaSnapshot (IdNotifUlt);
CREATE INDEX IxStorPol01 ON StorPolitica (IdStorageBackend);
CREATE INDEX IxStorObj01 ON StorObjeto (IdStorageBackend);

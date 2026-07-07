# 102 - Slice 9: Estrategia JDBC / MariaDB

**Slice:** 9-0
**Fecha:** 2026-07-02
**Tipo:** Estrategia + documentacion + preparacion.
**Build base confirmado:** 908/908 tests passing. BUILD SUCCESS.
**Sin cambios funcionales Java en este slice.**

---

## 1. Estado base

Etapa 8C documental in-memory: CERRADA, AUDITADA y lista para JDBC.

Slices cerrados:
- 8C-0A, 8C-0B, 8C-0D: alineacion modelo Java con MariaDB.
- 8C-1: catalogos documentales Java/enums.
- 8C-2: plantillas documentales in-memory.
- 8C-3: generacion FalDocumento desde plantilla.
- 8C-4: FalDocumentoFirmaReq snapshot.
- 8C-5A: numeracion documental reusable.
- 8C-5B: envio a firma.
- 8C-6A: ResultadoFirmaInfractor.
- 8C-6B-0, 8C-6B-1: firma real documental.
- 8C-6C-0, 8C-6C-1: emision formal.
- 8C-6D-0, 8C-6D-1: documento escaneado y convalidacion.
- 8C-6E: auditoria pre-JDBC/MariaDB. DELTA reconciliado.

Repositorios in-memory activos: 21 interfaces / 21 implementaciones InMemory*.
Tests: 908/908 passing. Sin JPA, sin Hibernate, sin JDBC implementado, sin MariaDB activo.

---

## 2. Decision de PK interna - BIGINT vs UUID

**Decision definitiva:**

Las entidades internas productivas usaran PK BIGINT AUTO_INCREMENT para rendimiento,
joins, FKs, indices y consistencia transaccional.

Cuando se requiera correlacion offline, integracion externa, importacion, sincronizacion o
idempotencia, se agregara un UUID/String tecnico como clave alternativa unica, sin reemplazar
la PK interna BIGINT.

Regla de columnas de ID:

    id                 BIGINT AUTO_INCREMENT PRIMARY KEY
    id_tecnico_offline VARCHAR(36) NULL UNIQUE   -- si aplica (offline, sync, importacion)
    id_externo         VARCHAR(...)  NULL         -- si aplica (integracion con sistema externo)
    id_origen          VARCHAR(...)  NULL         -- si aplica (importacion desde origen)

No usar UUID String como PK interna salvo caso excepcional explicitamente justificado.

Estado actual de IDs en repositorios:

Repositorio                   | ID actual Java | Tabla MariaDB            | Decision Slice 9
------------------------------|----------------|--------------------------|---------------------
ActaRepository                | Long           | fal_acta                 | BIGINT (ya alineado)
ActaEventoRepository          | Long (idActa)  | fal_acta_evento          | BIGINT
ActaEvidenciaRepository       | Long           | fal_acta_evidencia       | BIGINT
ActaSnapshotRepository        | Long (idActa)  | fal_acta_snapshot        | BIGINT
ApelacionActaRepository       | String UUID    | fal_acta_apelacion       | Evaluar BIGINT
BloqueanteMaterialRepository  | String UUID    | fal_bloqueante_material  | Evaluar BIGINT
DependenciaRepository         | Long           | fal_dependencia          | BIGINT (ya alineado)
DocumentoRepository           | Long           | fal_documento            | BIGINT (ya alineado)
DocumentoFirmaRepository      | Long           | fal_documento_firma      | BIGINT (ya alineado)
DocumentoFirmaReqRepository   | PK compuesta   | fal_documento_firma_req  | PK compuesta id+seq
DocumentoPlantillaRepository  | Long           | fal_documento_plantilla  | BIGINT (ya alineado)
FalloActaRepository           | String UUID    | fal_acta_fallo           | Evaluar BIGINT
FirmanteRepository            | Long           | fal_firmante + versiones | BIGINT (ya alineado)
FirmezaCondenaRepository      | String UUID    | fal_acta_firmeza_condena | Evaluar BIGINT
GestionExternaRepository      | String UUID    | fal_acta_gestion_externa | Evaluar BIGINT
InspectorRepository           | Long           | fal_inspector + versiones| BIGINT (ya alineado)
NormativaRepository           | Long           | fal_normativa_faltas     | BIGINT (ya alineado)
NotificacionRepository        | String UUID    | fal_notificacion         | Mantener CHAR(36)
PagoCondenaRepository         | String UUID    | fal_pago_condena         | Evaluar BIGINT
PagoVoluntarioRepository      | String UUID    | fal_pago_voluntario      | Evaluar BIGINT
TalonarioRepository           | Long           | num_talonario + varios   | BIGINT (ya alineado)

Entidades con UUID String pendientes de decision al migrar JDBC:
- ApelacionActaRepository, BloqueanteMaterialRepository, FalloActaRepository,
  FirmezaCondenaRepository, GestionExternaRepository, PagoCondenaRepository,
  PagoVoluntarioRepository.

Criterio al migrar cada una:
- Si necesita correlacion offline o idempotencia: BIGINT PK + UUID como columna alternativa.
- Si no: BIGINT PK directo.
- NotificacionRepository: mantener CHAR(36) por integracion externa de notificaciones.

---

## 3. Stack JDBC elegido

**Spring JDBC con JdbcClient (Spring 6.1+).**

Reglas:
- NO usar JPA, Hibernate, EntityManager, @Entity, @Table, @Id.
- NO usar JpaRepository, CrudRepository ni equivalentes Spring Data.
- USAR JdbcClient: API fluente de Spring JDBC moderno.
- SQL explicito. Parametros nombrados.
- Mappers manuales por entidad (RowMapper o lambda).
- Logica de negocio: permanece en servicios de aplicacion.
- Repositorios: sin logica de negocio.
- @Transactional en servicios de aplicacion.
- Tests unitarios: siguen con InMemory*.
- Tests de integracion JDBC: perfil separado, con MariaDB real o Testcontainers.

Convencion de nombres:
- DocumentoPlantillaRepository           -> Interfaz (ya existe)
- InMemoryDocumentoPlantillaRepository   -> Implementacion en memoria (ya existe)
- JdbcDocumentoPlantillaRepository       -> Implementacion JDBC a crear en Slice 9

Las interfaces de repositorio existentes NO deben modificarse.
Los servicios de aplicacion NO deben modificarse al agregar JDBC.
InMemory* se conserva para tests unitarios y perfil local/sin-DB.

---

## 4. Configuracion y perfiles

Estado actual:
- No existe src/main/resources con archivos yml/properties configurados.
- No existe dependencia MariaDB driver en pom.xml.
- No existe DataSource configurado.
- Sin @Transactional en servicios actualmente.

Propuesta de perfiles:

Perfil      | Uso                                        | Archivo
------------|--------------------------------------------|---------------------------------
default     | In-memory. Sin DB. Tests unitarios.        | application.yml
jdbc        | JDBC real con MariaDB.                     | application-jdbc.yml
test        | Tests unitarios. Sin DB.                   | (default alcanza)
integration | Tests de integracion JDBC. MariaDB real.   | application-integration.yml

El perfil jdbc activa:
- spring.datasource con URL MariaDB.
- Beans JdbcClient.
- Implementaciones Jdbc*Repository en lugar de InMemory*.

No hardcodear passwords. Usar variables de entorno o application-local.yml ignorado por git.

Dependencia a agregar en Slice 9-1:

    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
    </dependency>

Y si se usa Testcontainers:

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mariadb</artifactId>
        <scope>test</scope>
    </dependency>

---

## 5. Enums cerrados en MariaDB

**Decision definitiva:**

Los catalogos cerrados del dominio NO se modelan como tablas fisicas administrables.

Se mantienen como:
- enum Java con campo short codigo.
- columna SMALLINT en MariaDB.
- conversion Java: codigo() para guardar, desdeCodigo(short) para leer.

No crear tablas para catalogos cerrados.
No crear seeds/inserts para enums cerrados (no hay tabla fisica que poblar).
No inventar tablas catalogo por comodidad.
No duplicar enums en DB como tablas administrables.

Codigos por enum:

TipoDocu:
  1  ACTA_INFRACCION
  2  NOTIFICACION_ACTA
  3  MEDIDA_PREVENTIVA
  4  ACTO_ADMINISTRATIVO
  5  NOTIFICACION_ACTO_ADMINISTRATIVO
  6  CONSTANCIA
  7  ANEXO
  8  NULIDAD
  9  RESOLUTORIO_BLOQUEANTE
  10 INTIMACION_PAGO
  11 INTIMACION_INCUMPLIMIENTO_PLAN
  12 OTRO

EstadoDocu:
  1 BORRADOR
  2 EMITIDO
  3 PENDIENTE_FIRMA
  4 FIRMADO
  5 ADJUNTO
  6 ANULADO
  7 REEMPLAZADO

EstadoFirma (fal_documento_firma_req y fal_documento_firma):
  1 PENDIENTE
  2 SOLICITADA
  3 FIRMADA
  4 RECHAZADA
  5 ANULADA
  6 ERROR

TipoFirmaReq:
  0 NO_REQUIERE
  1 FIRMA_INTERNA
  2 FIRMA_INSPECTOR
  3 FIRMA_AUTORIDAD
  4 FIRMA_DIGITAL
  5 FIRMA_MULTIPLE
  -- No usar FIRMA_MIXTA. No existe.

MomentoNumeracionDocu:
  0 NO_APLICA
  1 AL_CREAR
  2 AL_EMITIR
  3 AL_ENVIAR_A_FIRMA
  4 AL_FIRMAR

TipoFirma:
  1 DIGITAL
  2 ELECTRONICA
  3 OLOGRAFA
  4 SISTEMA

ResultadoFirmaInfractor:
  1 FIRMADA
  2 SE_NIEGA_A_FIRMAR
  3 INFRACTOR_NO_PRESENTE
  4 IMPOSIBILITADO_PARA_FIRMAR
  5 NO_CAPTURADA_POR_FALLA_TECNICA
  -- No usar NO_REQUERIDA. No existe.

Enums de Bloque 8B (numeracion) - todos SMALLINT, sin tabla fisica:
  ClaseNumeracion, TipoTalonario, AlcanceTalonario,
  EstadoNumeroTalonario, MotivoAnulacionTalonario, EstadoAsignacionTalonario

---

## 6. Diferencia entre enum cerrado y dato configurable

### Enum cerrado

Es parte del contrato del sistema. No es administrable por operadores.
Cambiar un valor de enum es un cambio de codigo, no de datos.
Persistencia: columna SMALLINT. Sin tabla. Sin seed.

Ejemplos:
- EstadoDocu, TipoDocu, TipoFirmaReq, MomentoNumeracionDocu
- TipoFirma, ResultadoFirmaInfractor, EstadoFirma, EstadoFirmaReq
- TipoEventoActa, TipoEvidenciaActa
- ClaseNumeracion, TipoTalonario, AlcanceTalonario, EstadoNumeroTalonario
- MotivoAnulacionTalonario, EstadoAsignacionTalonario

### Dato configurable / productivo

Es administrable, versionable o cambia por operacion.
Requiere tabla fisica y puede requerir carga inicial de configuracion.

Ejemplos:
- fal_documento_plantilla - plantillas documentales configuradas por el sistema
- fal_documento_plantilla_firma_req - requisitos de firma de cada plantilla
- fal_firmante / fal_firmante_version / fal_firmante_version_habilitacion - firmantes reales
- fal_dependencia / fal_dependencia_version - dependencias de la organizacion
- fal_inspector / fal_inspector_version - inspectores actuantes
- num_politica / num_talonario / num_talonario_ambito - numeracion configurada

No llamar "seed de catalogo cerrado" a enums. No tienen tabla.
Si hay datos iniciales de configuracion, llamarlos:
- datos iniciales de configuracion
- datos maestros
- configuracion base

---

## 7. Descripciones SQL de enums cerrados

Si desde SQL se requiere descripcion legible para reporting, debugging o vistas,
NO crear tabla catalogo.

Opciones permitidas (no implementar en este slice salvo pedido explicito):

1. Funcion SQL deterministica:
   fn_fal_estado_docu_desc(SMALLINT)
   fn_fal_tipo_docu_desc(SMALLINT)
   fn_fal_tipo_firma_req_desc(SMALLINT)

2. Vista con CASE:
   vw_fal_documento_detalle
   vw_fal_acta_detalle

3. CASE directo en consultas/reportes puntuales.

Regla:
- La fuente de verdad funcional es el enum Java y su codigo productivo.
- Las funciones/vistas SQL son auxiliares de lectura. No habilitan valores nuevos.
- No crear FKs a tablas de enum cerradas (no existen).

---

## 8. Convenciones de mapeo Java <-> SQL

Tipo Java         | Tipo SQL    | Conversion
------------------|-------------|-----------------------------------------------
Long / long       | BIGINT      | directo
Short / short     | SMALLINT    | directo
Integer / int     | INT         | directo
String            | VARCHAR(n)  | respetar longitudes del modelo
boolean / Boolean | TINYINT(1)  | 0/1
LocalDateTime     | DATETIME(6) | JdbcClient mapea automaticamente
LocalDate         | DATE        | JdbcClient mapea automaticamente
enum (cerrado)    | SMALLINT    | .codigo() para guardar; desdeCodigo() para leer
BigDecimal        | DECIMAL(p,s)| directo

Reglas:
- Java camelCase. SQL snake_case.
- No mapear enum por nombre en DB (no persistir Strings de enum).
- No crear FKs a tablas de enum cerradas inexistentes.
- Mappers manuales: preferir lambdas o RowMapper por entidad.

Ejemplo de mapper enum:
  TipoDocu tipoDocu = TipoDocu.desdeCodigo(rs.getShort("tipo_docu"));
  short codigo = tipoDocu.codigo();

---

## 9. DDL delta activo pre-JDBC

Items identificados en auditoria 8C-6E.
Ver: backend/api-faltas-core/docs/spec-as-source/101-auditoria-pre-jdbc-mariadb.md seccion ACTIVOS PRE-JDBC.

Correcciones de columnas existentes:

1. num_politica: agregar fh_alta DATETIME(6) NOT NULL, id_user_alta VARCHAR(60) NOT NULL.
2. fal_acta: agregar resultado_firma_infractor SMALLINT NOT NULL DEFAULT 0.
3. fal_acta_evidencia: confirmar seed tipo_evid=6 (FIRMA_OLOGRAFA_INFRACTOR) si aplica.
4. fal_documento correcciones:
   - nro_docu VARCHAR(30) (renombrar desde numero_visible si aplica)
   - tipo_firma_req SMALLINT NOT NULL (renombrar desde requisito_firma si aplica)
   - Agregar: plantilla_id BIGINT NULL
   - Agregar: storage_key VARCHAR(255) NULL
   - Agregar: hash_docu VARCHAR(64) NULL
   - Agregar: fh_generacion DATETIME(6) NULL
   - Agregar: version_row INT NOT NULL DEFAULT 0 (locking optimista JDBC)
5. fal_documento_firma columnas a agregar:
   - id_firmante BIGINT NULL, ver_firmante SMALLINT NULL, seq_firma_req INT NULL
   - id_user_firma VARCHAR(60) NULL, rol_firmante VARCHAR(100) NULL
   - nombre_firmante VARCHAR(200) NULL, hash_documento VARCHAR(64) NULL
   - referencia_firma_ext VARCHAR(255) NULL, storage_key VARCHAR(255) NULL
   - mensaje_error VARCHAR(500) NULL, fh_firma DATETIME(6) NULL
   - fh_alta DATETIME(6) NOT NULL, id_user_alta VARCHAR(60) NOT NULL
6. fal_firmante_version: rol_firmante pasa a nullable.
7. fal_acta_gestion_externa: verificar monto_resultado DECIMAL(14,2) NULL.

Tablas nuevas a crear:

8.  fal_documento_plantilla
9.  fal_documento_plantilla_firma_req
10. fal_firmante
11. fal_firmante_version
12. fal_firmante_version_habilitacion
13. fal_documento_firma_req

Ver modelo: docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md

IDs pendientes de decision (UUID String -> evaluar BIGINT al migrar cada uno):
- fal_acta_apelacion, fal_bloqueante_material, fal_acta_fallo,
  fal_acta_firmeza_condena, fal_acta_gestion_externa, fal_pago_condena, fal_pago_voluntario.

---

## 10. Datos configurables / productivos que si son tablas

Tabla                              | Descripcion
-----------------------------------|----------------------------------------
fal_documento_plantilla            | Plantillas documentales del sistema
fal_documento_plantilla_firma_req  | Requisitos de firma por plantilla
fal_firmante                       | Firmantes reales del sistema
fal_firmante_version               | Versiones de cada firmante
fal_firmante_version_habilitacion  | Habilitaciones por tipo de documento
fal_dependencia                    | Dependencias de la organizacion
fal_dependencia_version            | Versiones de cada dependencia
fal_inspector                      | Inspectores actuantes
fal_inspector_version              | Versiones de cada inspector
num_politica                       | Politicas de numeracion
num_talonario                      | Talonarios de numeracion
num_talonario_ambito               | Ambitos de talonario

Estos datos requieren carga inicial de configuracion (datos maestros, no seed de enum).

---

## 11. Transacciones criticas para Slice 9

Numeracion documental:
  Componentes: num_talonario + num_talonario_movimiento + fal_documento.nro_docu
  Motivo: correlativo atomico. Sin huecos ni duplicados. Alta concurrencia.
  Estrategia: SELECT ... FOR UPDATE o bloqueo a nivel talonario.

Envio a firma con numeracion:
  Componentes: fal_documento (nroDocu + PENDIENTE_FIRMA) + num_talonario_movimiento
               + fal_documento_firma_req (crear/materializar) + fal_acta_evento
  Motivo: numerar + cambiar estado + materializar requisitos = atomico.

Firma real documental:
  Componentes: fal_documento_firma (insertar) + fal_documento_firma_req (actualizar)
               + fal_documento (FIRMADO si todos obligatorios activos cumplidos)
               + fal_acta_evento (DOCFIR)
  Motivo: consistencia firma -> req cumplido -> documento cerrado -> evento.

Emision formal:
  Componentes: fal_documento (EMITIDO + hashDocu + storageKey + fhGeneracion)
               + fal_acta_evento (DOCEMI)
  Motivo: estado + datos de emision juntos.

Incorporacion escaneado:
  Componentes: fal_documento (ADJUNTO + storageKey + hashDocu + fhGeneracion)
               + fal_acta_evento (DOCADJ)
  Motivo: documento + evento juntos.

Convalidacion firma escaneada:
  Componentes: fal_documento_firma + fal_documento_firma_req + fal_documento (estado)
               + fal_acta_evento (DOCFIR)
  Motivo: igual que firma real.

Labrar acta:
  Componentes: fal_acta + fal_acta_evento + fal_acta_snapshot + fal_acta_evidencia
  Motivo: alta de todas las piezas junta.

Asignacion talonario inspector:
  Componentes: num_talonario_inspector
  Motivo: concurrencia si multiples usuarios asignan simultaneamente.

---

## 12. Primer piloto JDBC - Decision

**Primer piloto elegido: DocumentoPlantillaRepository con JdbcClient.**

Candidatos evaluados:

Opcion       | Repositorio                  | Riesgo | Valor
-------------|------------------------------|--------|------------------------------------------
A (elegida)  | DocumentoPlantillaRepository | Bajo   | Prueba enums SMALLINT, JdbcClient, perfiles
B            | FirmanteRepository           | Medio  | Importante, mas complejo (versionado)
C            | DependenciaRepository        | Bajo   | Menos ligado al bloque documental

Motivos para elegir DocumentoPlantillaRepository:
- No es transaccional critico (sin numeracion, sin firma real).
- Prueba enums guardados como SMALLINT sin tablas de enum.
- Prueba relacion plantilla -> firma req (tabla hija fal_documento_plantilla_firma_req).
- Prepara el camino para GenerarDocumento JDBC.
- Valida JdbcClient, perfiles, mappers y SQL explicito de forma segura.
- Bajo riesgo de efectos laterales sobre el dominio.

---

## 13. Plan incremental Slice 9

9-0 - Este slice (CERRADO)
  Estrategia JDBC, DDL delta, decisiones de IDs y enums. Solo documentacion.

9-1 - Infraestructura JDBC base
  - Agregar dependencia MariaDB driver en pom.xml.
  - Configurar JdbcClient como bean Spring.
  - Crear application.yml base y application-jdbc.yml.
  - Test de conexion basico.
  - Sin migrar dominio todavia.
  - Sin tablas para enums cerrados.

9-2 - Piloto JDBC plantillas documentales
  - DDL: crear fal_documento_plantilla y fal_documento_plantilla_firma_req.
  - Implementar JdbcDocumentoPlantillaRepository.
  - Mapeo plantilla: enums como SMALLINT, ids BIGINT.
  - Activar via perfil jdbc.
  - Tests de integracion.

9-3 - Firmantes / habilitaciones JDBC
  - DDL: crear fal_firmante, fal_firmante_version, fal_firmante_version_habilitacion.
  - Implementar JdbcFirmanteRepository.
  - Versiones y habilitaciones. Tests de integracion.

9-4 - Inspectores y dependencias JDBC
  - JdbcInspectorRepository con versiones.
  - JdbcDependenciaRepository con versiones.
  - Tests de integracion.

9-5 - Talonarios / numeracion JDBC transaccional
  - DDL: num_politica (corregir), num_talonario, num_talonario_ambito,
    num_talonario_movimiento, num_talonario_inspector.
  - Implementar JdbcTalonarioRepository.
  - Transaccionalidad y bloqueo en numeracion. CRITICO.
  - Tests transaccionales de concurrencia.

9-6 - FalDocumento + firma req + firma real JDBC
  - DDL: corregir fal_documento y crear fal_documento_firma_req.
  - Implementar JdbcDocumentoRepository, JdbcDocumentoFirmaReqRepository,
    JdbcDocumentoFirmaRepository.
  - Transaccion firma real completa. Tests de integracion.

9-7 - Emision formal + adjuntos + eventos JDBC
  - Emision formal JDBC. DOCEMI, DOCADJ. Convalidacion.
  - ActaEventoRepository JDBC.

9-8 - Actas core JDBC
  - DDL: corregir fal_acta (resultado_firma_infractor).
  - Implementar JdbcActaRepository, JdbcActaSnapshotRepository, JdbcActaEvidenciaRepository.
  - Labrado transaccional completo. Tests de integracion.

9-9 - Repositorios UUID/String y retiro de legacy
  - Decidir y migrar: ApelacionActaRepository, FalloActaRepository, PagoCondenaRepository,
    PagoVoluntarioRepository, BloqueanteMaterialRepository, FirmezaCondenaRepository,
    GestionExternaRepository.
  - Deprecar/eliminar generarDocumento/firmarDocumento legacy segun decision del equipo.
  - Definir perfiles finales: in-memory para unit tests, jdbc para produccion.

Ajustar micro-slices segun hallazgos durante la implementacion.

---

## 14. Roadmap post-Slice 9

- OCR/IA: extraccion asistida metadata, datos sugeridos, validacion humana obligatoria.
- PDF/storage real: almacenamiento fisico real y hash criptografico.
- Proveedor firma digital real: integracion con proveedor externo (TipoFirma.DIGITAL).
- Notificaciones automaticas: siNotificable activo.
- Angular/frontend: decision de etapa pendiente.
- Validacion criptografica real de hash.
- fal_inspector_version campos firma: ELIMINADOS del modelo (decision 8F-11B).
- fal_observacion completo.
- PAGAPR definitivo.

---

## 15. Riesgos

Riesgo                                           | Nivel | Mitigacion
-------------------------------------------------|-------|--------------------------------------------------
IDs UUID String en 7 repositorios                | MEDIO | Decidir BIGINT vs UUID al migrar cada uno
Numeracion: huecos/duplicados bajo concurrencia  | ALTO  | SELECT FOR UPDATE o lock a nivel talonario
generarDocumento/firmarDocumento legacy          | BAJO  | Siguen activos; deprecar post-Slice 9
Snapshot y concurrencia JDBC                     | MEDIO | Recalcular snapshot dentro de cada transaccion
Seeds de catalogos incompletos                   | MEDIO | Enums Java tienen codigos; datos configurables necesitan carga real
version_row ausente para locking optimista       | BAJO  | Agregar en DDL de fal_documento al implementar 9-6
fal_inspector_version campos firma ausentes      | BAJO  | No critico para JDBC inicial
NotificacionRepository UUID -> CHAR(36)          | BAJO  | Mantener UUID por integracion externa

---

## 16. Criterios de aceptacion para entrar a 9-1

- [x] Este documento 102 existe y esta completo.
- [x] 99-pendientes actualizado con 9-0 cerrado y 9-1 como proximo.
- [x] 100-etapa-8 actualizado con Etapa 8C cerrada y entrada a Slice 9.
- [x] 101-auditoria actualizado con decisiones BIGINT, enums SMALLINT, piloto plantillas.
- [x] DELTA actualizado con decisiones cerradas.
- [x] Sin JPA/Hibernate en codigo Java. (VERIFICADO: no hay)
- [x] Sin tablas catalogo para enums cerrados. (VERIFICADO: no aplica en-memory)
- [x] Sin seeds para enums cerrados. (VERIFICADO: no aplica)
- [x] Build base: 908/908 tests passing. BUILD SUCCESS.
- [x] docs/spec-as-source historica: NO EXISTE (VERIFICADO: False).


---

## Actualizacion post Slice 9-1 (2026-07-02)

**Infraestructura JDBC base implementada.**

### Estado confirmado

Build: 908/908 tests passing. BUILD SUCCESS.

Dependencias:
- spring-boot-starter-jdbc: AGREGADO en pom.xml.
- mariadb-java-client: AGREGADO en pom.xml (scope runtime).
- Sin JPA/Hibernate. Sin spring-boot-starter-data-jpa.

Configuracion:
- application.yml: perfil default sin DataSource (InMemory*).
- application-jdbc.yml: perfil jdbc con DataSource por variables de entorno.
- JdbcConfig.java: marcador @Profile("jdbc").
- JdbcInfrastructureIT.java: test condicionado @EnabledIfEnvironmentVariable.

Stack confirmado:
- Spring JDBC con JdbcClient (Spring 6.1+ / Spring Boot 3.5.3).
- SQL explicito. Parametros nombrados. Mappers manuales.
- Sin JPA/Hibernate.

Variables de entorno para perfil jdbc:
  FALTAS_DB_URL, FALTAS_DB_USER, FALTAS_DB_PASSWORD

### Proximo piloto

9-2 - JdbcDocumentoPlantillaRepository.

Ver: backend/api-faltas-core/docs/spec-as-source/103-slice-9-1-infraestructura-jdbc.md

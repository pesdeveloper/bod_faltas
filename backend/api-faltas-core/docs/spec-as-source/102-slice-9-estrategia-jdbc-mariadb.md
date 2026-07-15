# 102 - Estrategia JDBC / MariaDB

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> No contiene el DDL definitivo. Fija la estrategia de acceso a datos (stack JDBC,
> convenciones de mapeo, estrategia de identidad y de enums) que el bloque de DDL debe
> seguir. Ver `101-auditoria-pre-jdbc-mariadb.md`, `103-slice-9-1-infraestructura-jdbc.md`,
> `109-delta-modelo-mariadb-inmemory.md` y `110-matriz-maestra-paridad-mariadb-inmemory.md`.

## 1. Estado vigente

InMemory es el oraculo funcional de paridad: toda implementacion JDBC debe reproducir
exactamente el comportamiento observable ya validado por los repositorios `InMemory*`
y por la suite de tests existente.

La infraestructura JDBC base (dependencias, perfiles, `DataSource`, prueba condicionada)
ya esta incorporada; el inventario verificable de esa infraestructura vive en
`103-slice-9-1-infraestructura-jdbc.md`. Este documento no repite ese inventario.

Sin repositorios JDBC de dominio, migraciones versionadas, DDL productivo ni
JPA/Hibernate todavia.

## 2. Orden de trabajo para el bloque DDL

1. Resolver cada `DECISION_DDL-*` listada en `110-matriz-maestra-paridad-mariadb-inmemory.md`.
2. Disenar el DDL fisico consistente con esas decisiones.
3. Crear migraciones versionadas, deterministas y reproducibles.
4. Implementar los adapters JDBC de los puertos de repositorio de dominio (sin modificar
   las interfaces existentes ni los servicios de aplicacion).
5. Probar paridad de comportamiento contra InMemory (misma suite funcional, sin
   sustituir los tests InMemory).

## 3. Stack JDBC elegido

**Spring JDBC con JdbcClient (Spring 6.1+).**

Reglas:
- NO usar JPA, Hibernate, EntityManager, `@Entity`, `@Table`, `@Id`.
- NO usar `JpaRepository`, `CrudRepository` ni equivalentes Spring Data.
- USAR `JdbcClient`: API fluente de Spring JDBC moderno.
- SQL explicito. Parametros nombrados.
- Mappers manuales por entidad (RowMapper o lambda).
- Logica de negocio: permanece en servicios de aplicacion.
- Repositorios: sin logica de negocio.
- `@Transactional` en servicios de aplicacion: una unica transaccion por comando.
- Tests unitarios: siguen con `InMemory*`.
- Tests de integracion JDBC: perfil separado, con MariaDB real o Testcontainers.

Convencion de nombres:
- `XxxRepository` -> interfaz de puerto (ya existe para cada agregado).
- `InMemoryXxxRepository` -> implementacion en memoria (ya existe).
- `JdbcXxxRepository` -> implementacion JDBC a crear durante el bloque DDL.

Las interfaces de repositorio existentes NO deben modificarse al agregar JDBC.
Los servicios de aplicacion NO deben modificarse al agregar JDBC.
`InMemory*` se conserva para tests unitarios y perfil local/sin-DB.

## 4. Configuracion y perfiles

Estado verificable de dependencias, perfiles, `DataSource` y variables de entorno: ver
`103-slice-9-1-infraestructura-jdbc.md`.

No hardcodear passwords. Usar variables de entorno o `application-local.yml` ignorado por git.

## 5. Estrategia de identidad (PK)

El inventario completo por agregado (tipo de ID actual, tabla objetivo, alineacion) vive
en `110-matriz-maestra-paridad-mariadb-inmemory.md`, seccion 2. Este documento no repite
esa tabla.

Regla general para nuevas decisiones de PK:
- Preferir `BIGINT AUTO_INCREMENT PRIMARY KEY` para rendimiento, joins, FKs, indices y
  consistencia transaccional.
- Cuando se requiera correlacion offline, integracion externa, importacion, sincronizacion
  o idempotencia, agregar una columna alternativa unica (`VARCHAR`/`CHAR` tecnico) sin
  reemplazar la PK interna `BIGINT`.
- No usar `UUID`/`String` como PK interna salvo caso excepcional explicitamente justificado
  (ver decisiones abiertas en `110`, seccion `DECISION_DDL-PAGO-01` y relacionadas).

## 6. Estrategia de enums persistibles

No todo enum de dominio expone un codigo explicito. Se distinguen tres categorias
(ver `109-delta-modelo-mariadb-inmemory.md` y `110`, `DECISION_DDL-ENUM-01`):

- `EXPLICIT_NUMERIC_CODE`: el enum expone `codigo()` numerico (`short`). Persistencia
  candidata: columna `SMALLINT`. Conversion: `codigo()` para guardar, `desdeCodigo(short)`
  para leer.
- `EXPLICIT_STRING_CODE`: el enum expone `codigo()` de tipo `String` estable. Persistencia
  candidata: columna `CHAR`/`VARCHAR` exacta con constraint.
- `NO_EXPLICIT_CODE`: el enum no expone `codigo()`. Prohibido persistir `ordinal()`.
  Prohibido persistir `name()` sin una decision explicita. Representacion fisica
  pendiente de `DECISION_DDL-ENUM-01`.

Los catalogos documentales listados en la seccion 7 de este documento
(`TipoDocu`, `EstadoDocu`, `EstadoFirma`, `TipoFirmaReq`, `MomentoNumeracionDocu`,
`TipoFirma`, `ResultadoFirmaInfractor` y los enums de numeracion de talonario) son
`EXPLICIT_NUMERIC_CODE` verificados contra el codigo Java vigente: exponen `codigo()`
`short` y su persistencia candidata es `SMALLINT` sin tabla fisica administrable.

`EstadoFalloActa`, `EstadoApelacionActa` y `EstadoPagoCondena` son `NO_EXPLICIT_CODE`:
no exponen `codigo()`. Su representacion fisica queda pendiente de
`DECISION_DDL-ENUM-01` en `110`; no tienen todavia una columna `SMALLINT` cerrada.

No crear tablas para catalogos cerrados `EXPLICIT_NUMERIC_CODE`/`EXPLICIT_STRING_CODE`.
No crear seeds/inserts para esos enums (no hay tabla fisica que poblar).
No inventar tablas catalogo por comodidad.
No duplicar enums en DB como tablas administrables.

### 6.1 Diferencia entre enum cerrado y dato configurable

**Enum cerrado** (`EXPLICIT_NUMERIC_CODE`, `EXPLICIT_STRING_CODE` o `NO_EXPLICIT_CODE`
una vez resuelto): parte del contrato del sistema, no administrable por operadores.
Cambiar un valor es un cambio de codigo, no de datos.

**Dato configurable/productivo**: administrable, versionable o cambia por operacion.
Requiere tabla fisica y puede requerir carga inicial de configuracion. Ejemplos:
`fal_documento_plantilla`, `fal_documento_plantilla_firma_req`, `fal_firmante` y sus
versiones, `fal_dependencia` y sus versiones, `fal_inspector` y sus versiones,
`num_politica`/`num_talonario`/`num_talonario_ambito`.

No llamar "seed de catalogo cerrado" a enums: no tienen tabla. Si hay datos iniciales de
configuracion, llamarlos "datos iniciales de configuracion", "datos maestros" o
"configuracion base".

### 6.2 Descripciones SQL de enums cerrados

Si desde SQL se requiere descripcion legible para reporting, debugging o vistas, NO crear
tabla catalogo. Opciones permitidas (no implementar en este documento salvo pedido
explicito): funcion SQL deterministica (`fn_fal_<enum>_desc(SMALLINT)`), vista con `CASE`,
o `CASE` directo en consultas puntuales.

La fuente de verdad funcional es el enum Java y su codigo productivo. Las
funciones/vistas SQL son auxiliares de lectura; no habilitan valores nuevos. No crear FKs
a tablas de enum cerradas (no existen).

## 7. Codigos vigentes de enums documentales `EXPLICIT_NUMERIC_CODE`

Codigos verificados contra el enum Java vigente (columna `SMALLINT`, sin tabla fisica):

TipoDocu: 1 ACTA_INFRACCION, 2 NOTIFICACION_ACTA, 3 MEDIDA_PREVENTIVA,
4 ACTO_ADMINISTRATIVO, 5 NOTIFICACION_ACTO_ADMINISTRATIVO, 6 CONSTANCIA, 7 ANEXO,
8 NULIDAD, 9 RESOLUTORIO_BLOQUEANTE, 10 INTIMACION_PAGO,
11 INTIMACION_INCUMPLIMIENTO_PLAN, 12 OTRO.

EstadoDocu: 1 BORRADOR, 2 EMITIDO, 3 PENDIENTE_FIRMA, 4 FIRMADO, 5 ADJUNTO, 6 ANULADO,
7 REEMPLAZADO.

EstadoFirma (`fal_documento_firma_req` y `fal_documento_firma`): 1 PENDIENTE,
2 SOLICITADA, 3 FIRMADA, 4 RECHAZADA, 5 ANULADA, 6 ERROR.

TipoFirmaReq: 0 NO_REQUIERE, 1 FIRMA_INTERNA, 2 FIRMA_INSPECTOR, 3 FIRMA_AUTORIDAD,
4 FIRMA_DIGITAL, 5 FIRMA_MULTIPLE. No usar FIRMA_MIXTA: no existe.

MomentoNumeracionDocu: 0 NO_APLICA, 1 AL_CREAR, 2 AL_EMITIR, 3 AL_ENVIAR_A_FIRMA,
4 AL_FIRMAR.

TipoFirma: 1 DIGITAL, 2 ELECTRONICA, 3 OLOGRAFA, 4 SISTEMA.

ResultadoFirmaInfractor: 1 FIRMADA, 2 SE_NIEGA_A_FIRMAR, 3 INFRACTOR_NO_PRESENTE,
4 IMPOSIBILITADO_PARA_FIRMAR, 5 NO_CAPTURADA_POR_FALLA_TECNICA. No usar NO_REQUERIDA:
no existe.

Enums de numeracion de talonario, todos `SMALLINT` sin tabla fisica: `ClaseNumeracion`,
`TipoTalonario`, `AlcanceTalonario`, `EstadoNumeroTalonario`,
`MotivoAnulacionTalonario`, `EstadoAsignacionTalonario`.

## 8. Convenciones de mapeo Java <-> SQL

| Tipo Java         | Tipo SQL      | Conversion                                    |
|--------------------|---------------|------------------------------------------------|
| Long / long        | BIGINT        | directo                                         |
| Short / short       | SMALLINT      | directo                                         |
| Integer / int       | INT           | directo                                         |
| String              | VARCHAR(n)    | respetar longitudes del modelo                  |
| boolean / Boolean   | TINYINT(1)    | 0/1                                             |
| LocalDateTime       | DATETIME(6)   | `JdbcClient` mapea automaticamente              |
| LocalDate           | DATE          | `JdbcClient` mapea automaticamente              |
| enum `EXPLICIT_NUMERIC_CODE` | SMALLINT | `.codigo()` para guardar; `desdeCodigo()` para leer |
| enum `EXPLICIT_STRING_CODE`  | CHAR/VARCHAR | `.codigo()` para guardar; `desdeCodigo()` para leer |
| enum `NO_EXPLICIT_CODE`      | pendiente `DECISION_DDL-ENUM-01` | sin conversion cerrada todavia |
| BigDecimal          | DECIMAL(p,s)  | directo                                         |

Reglas:
- Java `camelCase`. SQL `snake_case`.
- No mapear enum por nombre en DB (no persistir `name()` de enum sin decision explicita).
- No crear FKs a tablas de enum cerradas inexistentes.
- Mappers manuales: preferir lambdas o `RowMapper` por entidad.

## 9. Transaccionalidad critica: numeracion documental electronica

Componentes: `num_talonario` + `num_talonario_movimiento` + `fal_documento.nro_docu`.

Motivo: correlativo atomico, sin duplicados, alta concurrencia multiinstancia.

Estrategia: `NEXT VALUE FOR <secuencia_del_talonario>` (SEQUENCE MariaDB de
`num_talonario.nombre_secuencia`) para obtener el correlativo; no usar
`SELECT ... FOR UPDATE` sobre `num_talonario` como generador; no usar `ultimo_nro_usado`;
transaccion unica que comprende: insercion en `num_talonario_movimiento` (unicidad por
`id_talonario` + `nro_talonario`), asociacion `id_talonario`/`nro_talonario_usado`/`nro_docu`
al documento, y actualizacion OCC de `fal_documento.version_row`; idempotencia por
documento ya numerado (verificar `nro_docu` antes de consumir otra SEQUENCE); recarga tras
conflicto OCC (si el documento ya fue numerado por otra instancia, devolver ese numero; si
el estado es incompatible, conflicto funcional).

Una SEQUENCE puede presentar saltos tecnicos por rollback, cache o reinicio del motor. Esos
valores no se reutilizan artificialmente. Los saltos tecnicos de una SEQUENCE electronica
no equivalen a huecos ilegales de un talonario manual fisico.

El resto de la estrategia transaccional (una transaccion por comando, OCC, unicidad,
bloqueo) se rige por `109-delta-modelo-mariadb-inmemory.md` y por las decisiones fisicas
de `110-matriz-maestra-paridad-mariadb-inmemory.md`; este documento no repite esa matriz.

## 10. Riesgos vigentes

| Riesgo | Nivel | Mitigacion |
|---|---|---|
| Identidad UUID/String pendiente en varios agregados | MEDIO | Resolver via las `DECISION_DDL-*` de `110` al migrar cada uno |
| Numeracion: duplicacion bajo concurrencia | ALTO | SEQUENCE + constraints unicas (`id_talonario`, `nro_talonario`) en `num_talonario_movimiento`; OCC `fal_documento.version_row` |
| Numeracion: conflicto OCC bajo carrera | MEDIO | Recargar tras conflicto OCC; devolver numero ya asignado si compatible; conflicto funcional si incompatible |
| Numeracion: saltos tecnicos de SEQUENCE | BAJO | Aceptables para talonarios electronicos; no reutilizar valores avanzados; sin huecos ilegales |
| Enums `NO_EXPLICIT_CODE` sin representacion fisica | MEDIO | Resolver `DECISION_DDL-ENUM-01` antes del DDL de las tablas afectadas |
| Seeds de catalogos incompletos | MEDIO | Enums `EXPLICIT_NUMERIC_CODE`/`EXPLICIT_STRING_CODE` tienen codigo Java; los datos configurables necesitan carga real |

# Ejecucion DDL, baseline protegido, seeder y comentarios SQL

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Fija las decisiones transversales de mecanismo de ejecucion DDL, baseline
> protegido, contrato del seeder futuro y politica de comentarios SQL obligatorios.
> No contiene SQL ejecutable.

## 1. Mecanismo de ejecucion DDL (`DECISION_DDL-EXEC-01` CERRADA)

| Opcion | Decision |
|---|---|
| Flyway | NO |
| Liquibase | NO |
| Migraciones automaticas al iniciar Spring | NO |
| Migraciones historicas incrementales (V1__, V2__, ...) | NO |
| Scripts en `src/main/resources` o classpath | NO |
| Creacion/actualizacion automatica del esquema por Spring | NO |
| Ejecucion SQL | **exclusivamente manual por Pablo mediante HeidiSQL** |

**Orden conceptual para recrear una instalacion desde cero:**

1. Crear la base `sb_faltas_db`.
2. Ejecutar los procesos existentes que crean/cargan GEO, Rubros y sincronizacion.
3. Verificar el baseline protegido (ver seccion 2).
4. Ejecutar manualmente el script canonico de recreacion del dominio Faltas (aun no creado).
5. Ejecutar verificaciones estructurales.
6. Ejecutar opcionalmente el seeder de escenarios en un entorno autorizado.

**El script del dominio Faltas:**
- creara las 64 tablas canonicas faltantes;
- adoptara `fal_rubro_version` como tabla canonica preexistente;
- validara 65 tablas canonicas presentes al final;
- no administrara datos de objetos protegidos.

**Script futuro:**
- un script canonico completo de recreacion del dominio BOD Faltas;
- versionado en Git por la historia normal del repositorio;
- ubicado fuera del runtime/classpath, bajo `database/`.

**Consecuencia Java:** ninguna dependencia de Flyway ni Liquibase en el classpath productivo.
Spring JDBC puede conectar y operar. Spring no crea, altera ni migra el esquema.

## 2. Baseline protegido (`DECISION_DDL-BASELINE-01` CERRADA)

### Objetos protegidos (sin CREATE/ALTER/DROP/TRUNCATE/DELETE sobre ellos)

**Tablas:**

| Objeto | Dominio |
|---|---|
| `fal_informix_sync_error` | Sincronizacion Informix |
| `fal_informix_sync_run` | Sincronizacion Informix |
| `fal_rubro_version` | Rubros — **PREEXISTING_CANONICAL_ADOPTED** (ver nota) |
| `geo_bahra_asentamiento` | GEO |
| `geo_calle_alturas_barrio` | GEO |
| `geo_dataset_load_error` | GEO |
| `geo_dataset_load_run` | GEO |
| `geo_dataset_row_version` | GEO |
| `geo_ign_departamento` | GEO |
| `geo_ign_municipio` | GEO |
| `geo_ign_provincia` | GEO |
| `geo_indec_calles` | GEO |
| `geo_indec_localidad` | GEO |
| `geo_indec_localidad_censal` | GEO |
| `geo_malv_calle_version` | GEO |
| `geo_malv_localidad_version` | GEO |

**Vistas:**

| Objeto |
|---|
| `vw_fal_rubro_actual` |
| `vw_geo_malv_calle_actual` |
| `vw_geo_malv_localidad_actual` |
| `vw_geo_municipio_departamento` |

**Nota sobre `fal_rubro_version`:** es la unica tabla del dominio Faltas preexistente en
el baseline. Su estado en `mariadb-logical-model.md` es `PREEXISTING_CANONICAL_ADOPTED`.
El script DDL del dominio la adoptara sin crearla, alterarla ni administrar sus datos.

### Conteo canonico

- 65 tablas canonicas Faltas esperadas al final del DDL.
- 1 ya existente: `fal_rubro_version`.
- 64 a crear por el futuro DDL del dominio.

Los guardrails usan la lista exacta de 65 tablas, no `LIKE 'fal_%'`,
porque existen `fal_informix_sync_*` fuera del inventario canonico.

### Datos fisicos confirmados

| Dato | Valor |
|---|---|
| Motor | MariaDB 12.3.2 |
| Plataforma | Windows x64 |
| Storage | InnoDB |
| Charset | utf8mb4 |
| Collation | utf8mb4_uca1400_ai_ci |
| lower_case_table_names | 1 |
| `geo_calle_alturas_barrio.id_tca` longitud maxima observada | 4 |
| Valores con `id_tca` > 10 caracteres | 0 |
| `id_tca_malvinas` tipo | VARCHAR(10) |

## 3. Contrato del seeder futuro (`DECISION_DDL-SEED-01` CERRADA)

No implementar en este slice. Contrato normativo para la implementacion futura:

### Clase canonica

`FaltasDatabaseSeeder`

### Operaciones

| Operacion | Descripcion |
|---|---|
| `VERIFY` | Verifica estructura sin modificar datos |
| `SEED` | Carga el dataset canonico de escenarios |
| `RESET_TEST_DATA` | Limpia solo datos de escenarios Faltas (orden inverso de FK) |
| `RESET_AND_SEED` | RESET_TEST_DATA seguido de SEED |

### Protecciones obligatorias

- Deshabilitado por defecto.
- Requiere perfil/flag explicito.
- Requiere accion explicita.
- Requiere confirmacion destructiva explicita para operaciones de reset.
- Allowlist de base de datos autorizada.
- Allowlist de host autorizado.
- Prohibicion expresa de produccion.
- Transaccion cuando sea viable.
- Reporte de filas eliminadas/insertadas.

### Limpieza

- Solo datos de escenarios de las tablas administradas por BOD Faltas.
- Orden inverso de FK.
- Nunca objetos GEO, Rubros, sincronizacion ni vistas protegidas.

### Dataset

El seeder reutiliza los casos canonicos cubiertos por el oraculo InMemory,
no un conjunto arbitrario de actas de prueba.

### Usos

- Tests de integracion.
- Ejecucion manual para preparar/resetear escenarios UX en entorno autorizado.

## 4. Seeding de datos de configuracion (`DECISION_DDL-SEED-01` CERRADA)

| Tipo de dato | Estrategia |
|---|---|
| Enums `EXPLICIT_NUMERIC_CODE` / `EXPLICIT_STRING_CODE` | Sin tabla fisica; no se seedean |
| Datos configurables productivos (inspectores, plantillas, politicas de numeracion) | Scripts de datos iniciales separados del baseline de schema |
| Dataset de test funcional (actas, personas, movimientos) | InMemory: cargado programaticamente; JDBC futuro: fixture SQL separado por suite |

## 5. Comentarios obligatorios en DDL (`DECISION_DDL-COMMENT-01` CERRADA)

Todo `CREATE TABLE` y `ALTER TABLE COLUMN` del DDL futuro incluye `COMMENT`.

### Formato de tabla

```
QUÉ ES: <descripcion de la entidad/tabla>
USO: <para que se usa>
RAZÓN: <por que existe esta tabla y no otra estructura>
```

Puede serializarse en una sola cadena MariaDB con separadores `|`:

```sql
COMMENT = 'QUÉ ES: Evidencia vinculada al acta (imagen de firma, documento adjunto). | USO: Auditar pruebas capturadas durante la labranza o el circuito fallo/notificacion. | RAZÓN: Separada de FalActa para evitar columnas opcionales en la cabecera.'
```

### Formato de columna

El comentario de columna debe decir:

- que dato contiene;
- para que se usa;
- regla relevante cuando exista.

Ejemplos:

```sql
`estado_fallo` SMALLINT NOT NULL COMMENT 'EstadoFalloActa: 1=PENDIENTE_FIRMA, 2=PENDIENTE_NOTIFICACION, 3=NOTIFICADO, 4=FIRME, 5=REEMPLAZADO, 6=SIN_EFECTO. DECISION_DDL-ENUM-01.',
`version_row` INT NOT NULL DEFAULT 0 COMMENT 'Control de concurrencia optimista (OCC). Incrementado en cada UPDATE. DECISION_DDL-SNAP-01.',
`id_user_alta` CHAR(36) NOT NULL COMMENT 'Sub JWT del actor que creo el registro. Nunca nulo. DECISION_DDL-EVID-01.'
```

Reglas:

- Breve, claro, declarativo.
- No repetir solo el tipo SQL.
- Sin marcadores de trabajo pendiente (sin tokens de la forma TO-DO, FIX-ME, ni similares).
- No modificar objetos protegidos solo para agregar comentarios.

**Validacion futura:** el validador consultara `information_schema.tables` y
`information_schema.columns` para verificar presencia y formato de comentarios.

## 6. Relacion con otros documentos

- [`ddl-decisions.md`](ddl-decisions.md): registro de todas las decisiones fisicas.
- [`mariadb-logical-model.md`](mariadb-logical-model.md): inventario canonico de tablas y campos.
- [`../00-governance/ready-for-backend-clean-room-reconstruction.md`](../00-governance/ready-for-backend-clean-room-reconstruction.md): contrato de suficiencia de la spec para reconstruccion clean-room.
- [`jdbc-strategy.md`](jdbc-strategy.md): stack JDBC, convenciones de mapeo, riesgos.
- [`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md): gate de readiness DDL.

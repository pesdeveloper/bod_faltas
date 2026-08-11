# Registro documental canónico de spec-as-source

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Este registro es la fuente única de clasificación y autoridad DDL para cada
> documento Markdown de `docs/spec-as-source`. Ante contradicción sobre la
> clasificación de un archivo, este registro prevalece. No define reglas de
> dominio; solo gobierna precedencia documental.

## Cómo leer este registro

Cada fila representa exactamente un archivo `.md` de `docs/spec-as-source`,
con path relativo a esa carpeta.

Columnas:

- **Clasificacion:** `NORMATIVE`, `SUPPORTING_CURRENT` o `PRE_DDL_PLAN`.
- **Autoridad DDL:** `YES` o `SUPPORTING`.
- **Estado:** `VIGENTE` para todo documento aplicable a la spec activa.
- **Descripcion:** rol del documento en una línea.

No existen documentos `HISTORICAL` activos en esta carpeta: todo documento
histórico fue migrado (contenido único absorbido en un documento vigente) o
eliminado tras confirmar ausencia de valor único. La historia de esa
migración y eliminación permanece en Git (commits e historia del árbol de
`docs/spec-as-source`); este registro no depende de ningún artefacto
transitorio para sostener esa afirmación.

## Registro

| Path | Clasificacion | Autoridad DDL | Estado | Descripcion |
|---|---|---|---|---|
| `README.md` | NORMATIVE | YES | VIGENTE | Punto de entrada, autoridad, precedencia y orden de lectura de toda la spec. |
| `00-governance/document-registry.md` | NORMATIVE | YES | VIGENTE | Este registro: clasificación y autoridad DDL de cada documento. |
| `00-governance/glossary.md` | NORMATIVE | YES | VIGENTE | Vocabulario canónico del dominio; un término canónico tiene un único significado. |
| `00-governance/command-contract-standard.md` | NORMATIVE | YES | VIGENTE | Estándar normativo de contratos de comando: plantilla, orden de ejecución, errores, idempotencia, concurrencia, nueve principios del motor determinista. |
| `00-governance/ready-for-ddl-gate.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Cierre formal de la auditoría transversal final y gate `READY_FOR_DDL`; informe de conformidad, no fuente de reglas de dominio. |
| `00-governance/ready-for-backend-clean-room-reconstruction.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Suficiencia documental para reconstruir backend/domain/application/contracts/InMemory/JDBC contract/model/DDL/seeder/tests sin documentación externa. |
| `10-domain/lifecycle-states.md` | NORMATIVE | YES | VIGENTE | Dimensiones de estado y lifecycle del acta y sus subagregados. |
| `10-domain/calendario-plazos-administrativos.md` | NORMATIVE | YES | VIGENTE | Calendario, días no computables y cálculo de plazos administrativos. |
| `10-domain/firma-notificacion-fallo.md` | NORMATIVE | YES | VIGENTE | Circuito de firma documental y cola notificatoria del fallo. |
| `10-domain/states-events-catalogs.md` | NORMATIVE | YES | VIGENTE | Catálogo de bloques, estados, catálogos y eventos (`TipoEventoActa` completo). |
| `20-application/fallo-command-contracts.md` | NORMATIVE | YES | VIGENTE | Contratos definitivos de CMD-FALLO-001..007. Prevalece ante contradicción sobre esos siete comandos. |
| `20-application/command-contracts.md` | NORMATIVE | YES | VIGENTE | Catálogo general de comandos, precondiciones y efectos fuera de la familia fallo. |
| `30-projections/snapshot-bandejas-acciones.md` | NORMATIVE | YES | VIGENTE | Matriz de snapshot, bandejas y acciones pendientes derivadas. |
| `40-api/http-contracts.md` | NORMATIVE | YES | VIGENTE | Contratos HTTP: endpoints, requests, responses, errores. |
| `40-api/firma-event-contracts.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Contratos de eventos de dominio de la integración EDA Faltas↔Firmas: 12/12 eventos `NORMATIVE` (`EVENT_CONTRACTS_COMPLETE = 12/12`); transporte Outbox/Bus/Inbox diseñado y `DDL_READY`, `IMPLEMENTATION_NOT_STARTED`. |
| `30-integrations/firma-binary-transfer.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Transferencia binaria definitiva del PDF vigente y su resultado firmado entre Faltas y Firmas (`DEC-EDA-017`): preparación, descarga streaming y carga de resultado; idempotencia, límites y TTL cerrados; `IMPLEMENTATION_NOT_STARTED`. |
| `30-integrations/eventing-operations.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Autenticación M2M (`X-Api-Key`, `DEC-EDA-018`, exclusiva de transferencia binaria) y operación concreta del bus Outbox/Bus/Inbox (`DEC-EDA-019`): transacciones locales separadas de ingestión y procesamiento, lectura central con `LOCK IN SHARE MODE`, reintentos/backoff locales, retención por antigüedad, reconciliación y observabilidad, con valores cerrados. Sin claim ni lease central. |
| `50-persistence/jdbc-strategy.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Estrategia de incorporación de MariaDB/JDBC; script DDL canónico FULL-R1 aprobado y validado en sandbox; 9 puertos JDBC activos, resto en InMemory transitorio (`DECISION-PERSISTENCE-JDBC-CANONICA-01`). |
| `50-persistence/jdbc-infrastructure.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Inventario actual y verificable de la infraestructura JDBC base ya incorporada (dependencias, perfiles, configuración), previa a los repositorios de dominio. |
| `50-persistence/inmemory-mariadb-deltas.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Delta vigente entre el modelo InMemory y el modelo MariaDB objetivo. |
| `50-persistence/mariadb-logical-model.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Modelo lógico/inventario canónico de tablas, enums y decisiones cerradas de MariaDB; entrada principal al diseño de DDL/JDBC. Correcciones FULL-R1 incorporadas. |
| `50-persistence/ddl-decisions.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Registro cerrado de 24 decisiones físicas (`DECISION_DDL-*`) para el DDL manual. |
| `50-persistence/ddl-execution-and-test-seeding.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Mecanismo de ejecucion DDL manual, baseline protegido, contrato del seeder y politica de comentarios SQL obligatorios. |
| `40-domain/observaciones.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Spec completa de centralización de observaciones libres en `fal_observacion`: tabla, enums (EntidadTipoObservada/OrigenObservacion), triggers AFTER DELETE y matriz de clasificación de campos libres. |
| `30-integrations/municipal-eventing.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Arquitectura conceptual del bus de eventos municipal: modelo mínimo aprobado (única tabla central append-only `bus_evento`, Outbox/checkpoint/Inbox/Inbox error locales por aplicación, sin fan-out ni rutinas); envelope estándar. Modelo físico completo en `50-persistence/firma-eda-physical-model.md`; `DDL_READY`, ninguna tabla creada ni DDL ejecutado desde este documento. |
| `50-persistence/firma-eda-physical-model.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Modelo físico completo (`LOGICAL_MODEL_COMPLETE`, `PHYSICAL_MODEL_COMPLETE`, `DDL_READY`) de la integración EDA Faltas↔Firmas: columnas nuevas de `fal_documento_firma_req`/`fal_documento_firma` (`DEC-EDA-020`/`021`/`023`) y tablas nuevas `fal_outbox`/`bus_evento`/`fal_bus_checkpoint`/`fal_inbox`/`fal_inbox_error` (`DEC-EDA-022`/`025`/`026`), modelo mínimo aprobado sin fan-out ni rutinas `SQL SECURITY DEFINER`; DDL generado en `database/ddl/alter-firmas-eda-closure-001-forward.sql`/`-rollback.sql`, diagnósticos en `database/diagnostics/verify-firmas-eda-closure-001-{precheck,postcheck,parity}.sql`, inventario en `database/design/firmas-eda-closure-001-scope.md`; `DDL_NOT_EXECUTED`. |
| `60-geo/geodomicilios.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Decisiones arquitectónicas y de modelado del subsistema GeoDomicilios: arquitectura (PostGIS/Martin/MapLibre GL JS como autoridades; MariaDB fuera del baseline geográfico; Google fuera del baseline), nomenclatura catastral canónica (quinta, secc VARCHAR(7), ucomp VARCHAR(3); `0` = ausencia en quinta/frac, nunca Q0/F0 en etiqueta), localidad municipal PostGIS, comercio no registrado productivo, geocodificador municipal (gis_calle_tramo_altura — segmento físico como unidad principal; lados conservados como evidencia opcional; gate 85 %; calidades, reverse local, holdout), capa parcelaria MVT (Martin), lenguaje visual Malvinas Claro (Apple Maps Muted; paleta 9 localidades validada PostGIS; jerarquía zoom; doble trazo calles; etiquetas parcelarias; buscador territorial; accesibilidad; rendimiento). |
| `90-roadmap/current-roadmap.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Roadmap vigente posterior al cierre de spec-as-source: DDL/JDBC e integraciones externas. |

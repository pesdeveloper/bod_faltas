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
| `10-domain/lifecycle-states.md` | NORMATIVE | YES | VIGENTE | Dimensiones de estado y lifecycle del acta y sus subagregados. |
| `10-domain/calendario-plazos-administrativos.md` | NORMATIVE | YES | VIGENTE | Calendario, días no computables y cálculo de plazos administrativos. |
| `10-domain/firma-notificacion-fallo.md` | NORMATIVE | YES | VIGENTE | Circuito de firma documental y cola notificatoria del fallo. |
| `10-domain/states-events-catalogs.md` | NORMATIVE | YES | VIGENTE | Catálogo de bloques, estados, catálogos y eventos (`TipoEventoActa` completo). |
| `20-application/fallo-command-contracts.md` | NORMATIVE | YES | VIGENTE | Contratos definitivos de CMD-FALLO-001..007. Prevalece ante contradicción sobre esos siete comandos. |
| `20-application/command-contracts.md` | NORMATIVE | YES | VIGENTE | Catálogo general de comandos, precondiciones y efectos fuera de la familia fallo. |
| `30-projections/snapshot-bandejas-acciones.md` | NORMATIVE | YES | VIGENTE | Matriz de snapshot, bandejas y acciones pendientes derivadas. |
| `40-api/http-contracts.md` | NORMATIVE | YES | VIGENTE | Contratos HTTP: endpoints, requests, responses, errores. |
| `50-persistence/jdbc-strategy.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Estrategia de incorporación de MariaDB/JDBC; entrada del bloque DDL, sin SQL definitivo. |
| `50-persistence/jdbc-infrastructure.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Inventario actual y verificable de la infraestructura JDBC base ya incorporada (dependencias, perfiles, configuración), previa a los repositorios de dominio. |
| `50-persistence/inmemory-mariadb-deltas.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Delta vigente entre el modelo InMemory y el modelo MariaDB objetivo. |
| `50-persistence/mariadb-logical-model.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Modelo lógico/inventario canónico de tablas, enums y decisiones cerradas de MariaDB; entrada principal al diseño de DDL/JDBC. |
| `50-persistence/ddl-decisions.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Registro único de decisiones físicas abiertas (`DECISION_DDL-*`) previas al DDL versionado. |
| `90-roadmap/current-roadmap.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Roadmap vigente posterior al cierre de spec-as-source: DDL/JDBC e integraciones externas. |

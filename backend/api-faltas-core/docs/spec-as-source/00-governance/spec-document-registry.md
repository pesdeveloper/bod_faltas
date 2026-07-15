# Registro documental canónico de spec-as-source

> **Estado documental:** NORMATIVE
> **Autoridad DDL:** YES
> Este registro es la fuente única de clasificación y autoridad DDL para cada
> documento Markdown de `docs/spec-as-source` fuera de `handoff/**`. Ante
> contradicción sobre la clasificación de un archivo, este registro prevalece.
> No define reglas de dominio; solo gobierna precedencia documental.

## Cómo leer este registro

Cada fila representa exactamente un archivo `.md` fuera de `handoff/**`, con
path relativo a `docs/spec-as-source`. `handoff/**` queda excluido como fuente
normativa: no se elimina, pero no se registra ni se banneriza (ver README,
sección 5).

Columnas:

- **Clasificacion:** `NORMATIVE`, `SUPPORTING_CURRENT`, `HISTORICAL` o `PRE_DDL_PLAN`.
- **Autoridad DDL:** `YES`, `SUPPORTING` o `NO`.
- **Estado:** `VIGENTE` para todo documento aplicable a la spec activa.
- **Descripcion:** rol del documento en una línea.

## Registro

| Path | Clasificacion | Autoridad DDL | Estado | Descripcion |
|---|---|---|---|---|
| `README.md` | NORMATIVE | YES | VIGENTE | Punto de entrada, autoridad, precedencia y orden de lectura de toda la spec. |
| `00-governance/spec-document-registry.md` | NORMATIVE | YES | VIGENTE | Este registro: clasificación y autoridad DDL de cada documento. |
| `00-governance/glossary.md` | NORMATIVE | YES | VIGENTE | Vocabulario canónico del dominio; un término canónico tiene un único significado. |
| `00-governance/command-contract-standard.md` | NORMATIVE | YES | VIGENTE | Estándar normativo de contratos de comando: plantilla, orden de ejecución, errores, idempotencia, concurrencia. |
| `10-domain/lifecycle-states.md` | NORMATIVE | YES | VIGENTE | Dimensiones de estado y lifecycle del acta y sus subagregados. |
| `10-domain/calendario-plazos-administrativos.md` | NORMATIVE | YES | VIGENTE | Calendario, días no computables y cálculo de plazos administrativos. |
| `10-domain/firma-notificacion-fallo.md` | NORMATIVE | YES | VIGENTE | Circuito de firma documental y cola notificatoria del fallo. |
| `20-application/fallo-command-contracts.md` | NORMATIVE | YES | VIGENTE | Contratos definitivos de CMD-FALLO-001..007. Prevalece ante contradicción sobre esos siete comandos. |
| `02-estados-bloques-eventos.md` | NORMATIVE | YES | VIGENTE | Catálogo de bloques, estados, catálogos y eventos (`TipoEventoActa` completo); material de extracción temática en curso. |
| `03-comandos-precondiciones-efectos.md` | NORMATIVE | YES | VIGENTE | Catálogo general de comandos, precondiciones y efectos fuera de la familia fallo; material de extracción temática en curso. |
| `04-snapshot-bandejas-acciones.md` | NORMATIVE | YES | VIGENTE | Matriz de snapshot, bandejas y acciones pendientes derivadas. |
| `05-api-core-endpoints.md` | NORMATIVE | YES | VIGENTE | Contratos HTTP: endpoints, requests, responses, errores. |
| `99-pendientes-siguientes-slices.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Roadmap vigente posterior al cierre de spec-as-source: DDL/JDBC e integraciones externas. |
| `101-auditoria-pre-jdbc-mariadb.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Cierre formal de la auditoría transversal final y gate `READY_FOR_DDL`; informe de conformidad, no fuente de reglas de dominio. |
| `102-slice-9-estrategia-jdbc-mariadb.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Estrategia de incorporación de MariaDB/JDBC; entrada del bloque DDL, sin SQL definitivo. |
| `103-slice-9-1-infraestructura-jdbc.md` | SUPPORTING_CURRENT | SUPPORTING | VIGENTE | Inventario actual y verificable de la infraestructura JDBC base ya incorporada (dependencias, perfiles, configuración), previa a los repositorios de dominio. |
| `104-plantillas-redaccion-combinacion-documentos.md` | HISTORICAL | NO | VIGENTE | Diario de implementación de plantillas, variables documentales y motor de combinación. |
| `108-frontend-ready-demo.md` | HISTORICAL | NO | VIGENTE | Diario de auditoría frontend-ready de los endpoints `/demo/**`. |
| `109-delta-modelo-mariadb-inmemory.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Delta vigente entre el modelo InMemory y el modelo MariaDB objetivo. |
| `110-matriz-maestra-paridad-mariadb-inmemory.md` | PRE_DDL_PLAN | SUPPORTING | VIGENTE | Matriz maestra de paridad InMemory/MariaDB; entrada principal al diseño de DDL/JDBC. |
| `06-tests-core.md` | HISTORICAL | NO | VIGENTE | Diario histórico de suites de tests y conteos acumulados por bloque cerrado. |

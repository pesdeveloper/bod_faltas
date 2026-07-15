# Spec-as-source canónica — API Faltas Core

## 1. Propósito

Esta carpeta es la única spec-as-source canónica del backend productivo de
Faltas (`backend/api-faltas-core`). Las reglas funcionales, arquitectónicas y
de persistencia aprobadas deben quedar expresadas aquí antes de considerarse
vigentes.

El código Java, los tests, los modelos anteriores, las matrices, los deltas,
los handoffs y la historia Git son fuentes de evidencia, auditoría o
conformidad. No pueden redefinir silenciosamente la spec canónica.

La auditoría transversal final (`101-auditoria-pre-jdbc-mariadb.md`) cierra
formalmente la etapa spec-as-source. Ver "Estado `READY_FOR_DDL`" más abajo.

## 2. Registro documental

El [`00-governance/spec-document-registry.md`](00-governance/spec-document-registry.md)
es la fuente única de clasificación y autoridad DDL para cada `.md` de esta
carpeta fuera de `handoff/**`. Cada archivo aparece allí exactamente una vez,
con su clasificación (`NORMATIVE`, `SUPPORTING_CURRENT`, `HISTORICAL` o
`PRE_DDL_PLAN`) y su autoridad DDL (`YES`, `SUPPORTING` o `NO`).

## 3. Regla de precedencia

Ante una contradicción:

1. no elegir una versión por fecha, nombre de archivo, cantidad de tests o cercanía con el código;
2. no inventar una conciliación;
3. registrar y reportar el gap;
4. obtener una decisión humana explícita cuando afecte dominio, contratos o arquitectura;
5. actualizar primero la spec canónica;
6. alinear después código, tests, persistencia y documentos de referencia.

Una implementación existente demuestra comportamiento, pero no convierte
automáticamente ese comportamiento en regla normativa.

Precedencia mínima entre documentos de la spec, de mayor a menor autoridad:

1. `00-governance/` (glosario, estándar de contratos de comando, registro documental).
2. `10-domain/` (dimensiones de estado, lifecycle, firma/notificación, calendario).
3. `20-application/` (contratos de comandos de aplicación).
4. Documentos top-level clasificados `NORMATIVE` (`02` a `05`, según el registro).
5. `SUPPORTING_CURRENT`.
6. `PRE_DDL_PLAN`.
7. `HISTORICAL`.

Ante contradicción entre un documento temático (`00-governance/`, `10-domain/`)
y un documento de contrato funcional top-level, el documento temático es
normativo en lo que respecta a definiciones de términos, dimensiones y
lifecycle. Cuando [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md)
declare precedencia específica para `CMD-FALLO-001..007`, esa precedencia
específica se mantiene: ese documento prevalece sobre cualquier otro para esos
siete comandos.

## 4. Orden de lectura

Para cualquier trabajo en `backend/api-faltas-core`:

1. leer este `README.md`;
2. consultar el [registro documental](00-governance/spec-document-registry.md) para identificar la clasificación de los documentos aplicables al alcance;
3. leer, en orden temático:
   1. [`00-governance/glossary.md`](00-governance/glossary.md) — vocabulario canónico;
   2. [`10-domain/lifecycle-states.md`](10-domain/lifecycle-states.md) — dimensiones de estado y lifecycle;
   3. [`10-domain/calendario-plazos-administrativos.md`](10-domain/calendario-plazos-administrativos.md) — calendario y plazos;
   4. [`00-governance/command-contract-standard.md`](00-governance/command-contract-standard.md) — estándar de contratos de comando;
   5. [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md) — contratos de CMD-FALLO-001..007;
   6. [`10-domain/firma-notificacion-fallo.md`](10-domain/firma-notificacion-fallo.md) — circuito de firma y cola notificatoria;
4. leer solamente el subconjunto adicional necesario para el alcance (`02-estados-bloques-eventos.md`, `03-comandos-precondiciones-efectos.md`, `04-snapshot-bandejas-acciones.md`, `05-api-core-endpoints.md`);
5. consultar código y tests como evidencia de conformidad;
6. consultar `docs/faltas/` y otros documentos externos únicamente como fuentes de auditoría o diseño físico;
7. detenerse y reportar si existe una contradicción no resuelta.

## 5. Documentos normativos, de soporte, históricos y pre-DDL

Ver el detalle completo, archivo por archivo, en
[`00-governance/spec-document-registry.md`](00-governance/spec-document-registry.md).

Resumen por clasificación:

- **NORMATIVE** (autoridad `YES`): `00-governance/glossary.md`, `00-governance/command-contract-standard.md`, `00-governance/spec-document-registry.md`, `10-domain/lifecycle-states.md`, `10-domain/calendario-plazos-administrativos.md`, `10-domain/firma-notificacion-fallo.md`, `20-application/fallo-command-contracts.md`, `02-estados-bloques-eventos.md`, `03-comandos-precondiciones-efectos.md`, `04-snapshot-bandejas-acciones.md`, `05-api-core-endpoints.md`, este `README.md`.
- **SUPPORTING_CURRENT** (autoridad `SUPPORTING`): `99-pendientes-siguientes-slices.md`, `101-auditoria-pre-jdbc-mariadb.md`, `103-slice-9-1-infraestructura-jdbc.md`.
- **PRE_DDL_PLAN** (autoridad `SUPPORTING`): `102-slice-9-estrategia-jdbc-mariadb.md`, `109-delta-modelo-mariadb-inmemory.md`, `110-matriz-maestra-paridad-mariadb-inmemory.md`.
- **HISTORICAL** (autoridad `NO`): `06-tests-core.md`, `104-plantillas-redaccion-combinacion-documentos.md`, `108-frontend-ready-demo.md`, y todo `handoff/**` (excluido del registro; no se elimina).

`102-slice-9-estrategia-jdbc-mariadb.md` fija la **estrategia** de acceso a
datos (stack JDBC, identidad, enums) que el bloque de DDL debe seguir.
`103-slice-9-1-infraestructura-jdbc.md` es el **inventario actual y
verificable** de la infraestructura JDBC base ya incorporada (dependencias,
perfiles, `DataSource`, prueba condicionada); por eso es `SUPPORTING_CURRENT`
y no `PRE_DDL_PLAN`: describe hechos vigentes del código, no un plan futuro.

`101-auditoria-pre-jdbc-mariadb.md` es `SUPPORTING_CURRENT`: es el informe/gate
de conformidad y cierre formal de la auditoría transversal final
(`READY_FOR_DDL`), no una fuente de reglas de dominio. No conserva un anexo
histórico extenso; la historia de auditorías previas permanece en Git.

## 6. Cómo se usa la spec para diseñar el DDL de MariaDB

El diseño del DDL versionado de MariaDB debe partir de:

1. los documentos `NORMATIVE` (estados, eventos, comandos, endpoints) para las reglas de dominio que las tablas y columnas deben reforzar, nunca redefinir;
2. [`109-delta-modelo-mariadb-inmemory.md`](109-delta-modelo-mariadb-inmemory.md) para los deltas vigentes de identidad, versionRow/OCC, unicidades, atomicidad, transacciones y enums/catálogos;
3. [`110-matriz-maestra-paridad-mariadb-inmemory.md`](110-matriz-maestra-paridad-mariadb-inmemory.md) como matriz de entrada: por cada agregado/repositorio, identidad primaria, clave natural, FKs conceptuales, unicidad física, índices, versionRow/OCC, frontera transaccional y fuente normativa;
4. [`102-slice-9-estrategia-jdbc-mariadb.md`](102-slice-9-estrategia-jdbc-mariadb.md) y [`103-slice-9-1-infraestructura-jdbc.md`](103-slice-9-1-infraestructura-jdbc.md) para la estrategia y la infraestructura JDBC ya incorporada.

El DDL no puede modificar reglas de dominio, estados, eventos, transiciones,
bandejas ni contratos HTTP vigentes. Toda decisión física que requiera
análisis durante el diseño del DDL se marca `DECISION_DDL` en `109`/`110`; no
se presenta como gap funcional.

## 7. Estado `READY_FOR_DDL`

La auditoría transversal final registrada en
[`101-auditoria-pre-jdbc-mariadb.md`](101-auditoria-pre-jdbc-mariadb.md)
declara el estado `READY_FOR_DDL`: la etapa spec-as-source queda formalmente
cerrada y la siguiente etapa autorizada es diseñar y generar el DDL
versionado de MariaDB, sin alterar los contratos funcionales vigentes.

## 8. Qué queda fuera de esta spec

Quedan explícitamente fuera del alcance de spec-as-source y de esta
auditoría:

- la generación del DDL/SQL definitivo de MariaDB;
- la implementación JDBC de los repositorios de dominio;
- las transacciones por comando, unicidades y FKs físicas, y OCC/bloqueos multinodo;
- la migración de datos existentes;
- las integraciones externas reales (Firmas, Notificaciones, Ingresos/Tesorería, storage/comprobantes reales, calendario administrativo externo);
- el frontend productivo.

Estos ítems son el roadmap vigente en
[`99-pendientes-siguientes-slices.md`](99-pendientes-siguientes-slices.md).

## 9. Fuentes externas a esta carpeta

`docs/faltas/` contiene insumos históricos y de diseño para MariaDB, proceso y
decisiones de dominio. Son obligatorios como material de auditoría cuando el
alcance los involucra, pero no prevalecen sobre esta spec.

Los tests ejecutables y la implementación InMemory constituyen la evidencia
principal para comprobar paridad durante la incorporación de MariaDB. Si
difieren de la spec, debe registrarse el gap y resolverse; no se debe
modificar la spec automáticamente para copiar el código.

El prototipo y Angular son evidencia de UX o integración. Nunca constituyen
por sí solos una fuente normativa de dominio.

## 10. Guardrails

- No inventar eventos, bloques, estados, bandejas, acciones ni catálogos.
- No confundir estado de dominio, estado documental, bandeja operativa, acción disponible o proyección de snapshot.
- No modificar reglas funcionales para facilitar la persistencia.
- No reintroducir conceptos explícitamente eliminados.
- No usar cronologías de slices como sustituto de una definición vigente consolidada en documentos `NORMATIVE`, `SUPPORTING_CURRENT` o `PRE_DDL_PLAN`.
- No eliminar documentación sin extracción y trazabilidad previas.
- No considerar cerrado un cambio documental si deja referencias rotas o fuentes de autoridad contradictorias.
- `SpecAsSourceGuardrailTest` (ver `backend/api-faltas-core/src/test/java/.../application/SpecAsSourceGuardrailTest.java`) automatiza la verificación del registro, los links relativos, los términos prohibidos y el gate `READY_FOR_DDL`.

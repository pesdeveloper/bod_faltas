# Suficiencia documental para reconstruccion clean-room del backend

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Documenta el estado de suficiencia de la spec-as-source para ejecutar una
> prueba de reconstruccion independiente del backend de Faltas (`backend/api-faltas-core`),
> sin acceso al codigo Java productivo actual, los tests internos, la historia
> Git ni documentacion externa no canonica.

## 1. Dos gates separados

### Gate 1: READY_FOR_BACKEND_CLEAN_ROOM_RECONSTRUCTION

**Significado:** la documentacion canonica contiene informacion suficiente
para ejecutar una prueba de reconstruccion independiente.

**Estado actual:** `YES`

Condicion de evaluacion: todos los guardrails documentales pasan (ver seccion 4).

### Gate 2: BACKEND_CLEAN_ROOM_RECONSTRUCTION_CERTIFIED

**Significado:** un agente independiente reconstruyo el backend sin ver la
implementacion original y una suite oculta/independiente comprobo equivalencia.

**Estado actual:** `PENDING`

No se puede declarar `CERTIFIED` hasta ejecutar esa prueba real con agente
independiente y suite no entregada.

## 2. Contrato de arquitectura/build (documentado en la spec)

El agente reconstructor debe poder derivar el entorno de build exclusivamente
de la spec, sin consultar el repositorio:

| Aspecto | Valor |
|---|---|
| Lenguaje | Java 21 |
| Build | Maven |
| Framework | Spring Boot 3.5.3 |
| Modulo | `backend/api-faltas-core` |
| Package raiz | `ar.gob.malvinas.faltas.core` |
| Acceso a datos | Spring JDBC explicito (`JdbcClient`) |
| Driver | MariaDB (org.mariadb.jdbc) |
| Perfil default | InMemory (sin base de datos real) |
| Perfil jdbc | Persistencia real contra MariaDB 12.3.2 |
| ORM | Ninguno — sin JPA/Hibernate |
| Migraciones | Ninguna — sin Flyway/Liquibase |
| DDL | Manual desde HeidiSQL por el operador |
| UX | Fuera de alcance de la reconstruccion |

## 3. Material que recibe el agente reconstructor

El agente reconstructor recibe exclusivamente:

- spec-as-source canonica completa (`docs/spec-as-source/**`);
- el contrato de arquitectura/build documentado en la seccion 2 de este
  documento (que vive dentro de la propia spec).

**No se entrega:**

- codigo Java productivo (`src/main/java`);
- tests internos actuales (`src/test/java`);
- repositorios InMemory actuales;
- `pom.xml` productivo como fuente oculta;
- `application.yml` productivo como fuente oculta;
- historia Git (commits, diffs, mensajes);
- handoffs o documentos transitorios eliminados;
- prototipo UX (`backend/api-faltas-prototipo`);
- frontend Angular (`apps/web-direccion-faltas`).

**No es clean-room si se entrega el Java o los tests actuales.**

## 4. Condiciones de suficiencia documentales (Gate 1)

| Condicion | Estado |
|---|---|
| 0 decisiones `DECISION_DDL-*` abiertas | CUMPLIDA — 24 cerradas, 0 pendientes |
| 0 contradicciones canonicas conocidas | CUMPLIDA |
| Catalogos completos (estados, eventos, enums) | CUMPLIDA |
| Codigos explicitos para todos los enums persistibles | CUMPLIDA — 0 enums `NO_EXPLICIT_CODE` |
| Longitudes fisicas resueltas (VARCHAR, CHAR, DECIMAL) | CUMPLIDA |
| Invariantes y errores documentados por entidad/comando | CUMPLIDA |
| Ningun conocimiento indispensable oculto solo en Java | CUMPLIDA |
| Contrato de arquitectura/build dentro de la spec | CUMPLIDA — seccion 2 de este documento |

Script DDL ejecutable, adapters JDBC e implementacion del seeder son
**salidas** de la reconstruccion, no prerrequisitos del gate de readiness.

## 5. Salidas esperadas de la reconstruccion

El agente reconstructor debe producir:

- dominio (entidades, enums, reglas, invariantes);
- servicios de aplicacion (comandos, casos de uso);
- contratos HTTP (endpoints, requests, responses, errores);
- puertos (interfaces de repositorio);
- repositorios InMemory (semantica OCC, copy-on-read);
- contrato/adapters JDBC;
- DDL manual;
- seeder;
- tests propios de conformidad.

## 6. Documentos que proveen la informacion completa

| Necesidad de reconstruccion | Documento canonico |
|---|---|
| Contratos funcionales (estados, eventos, transiciones) | `10-domain/lifecycle-states.md`, `10-domain/states-events-catalogs.md` |
| Circuito de firma y notificacion | `10-domain/firma-notificacion-fallo.md` |
| Calendario y plazos | `10-domain/calendario-plazos-administrativos.md` |
| Contratos de comando (precondiciones, efectos, invariantes, errores) | `20-application/command-contracts.md`, `20-application/fallo-command-contracts.md` |
| Proyecciones operativas (snapshot, bandejas, acciones) | `30-projections/snapshot-bandejas-acciones.md` |
| Contratos HTTP (endpoints, codigos de error) | `40-api/http-contracts.md` |
| Inventario fisico de tablas y campos | `50-persistence/mariadb-logical-model.md` |
| Decisiones fisicas (todas cerradas) | `50-persistence/ddl-decisions.md` |
| Estrategia JDBC y convenciones de mapeo | `50-persistence/jdbc-strategy.md` |
| Baseline protegido, seeder, comentarios DDL | `50-persistence/ddl-execution-and-test-seeding.md` |
| Estandar de contratos y concurrencia | `00-governance/command-contract-standard.md` |
| Contrato build | Seccion 2 de este documento |

## 7. Validacion futura (Gate 2)

La reconstruccion se prueba con una suite independiente **no entregada** al
agente reconstructor. No se exige identidad byte a byte. Se exige equivalencia:

- **funcional:** mismas precondiciones, efectos y transiciones;
- **contractual:** mismos endpoints, requests, responses, errores HTTP;
- **de concurrencia:** OCC por `version_row`, mismas excepciones;
- **de eventos:** mismo append-only, mismo catalogo de tipos;
- **de errores:** mismas excepciones de dominio y codigos HTTP;
- **de persistencia:** mismo modelo logico, mismos tipos fisicos;
- **de esquema:** mismas 65 tablas canonicas, mismos campos, mismas FKs conceptuales;
- **del DDL:** script funcional, mismas restricciones, mismo baseline protegido;
- **del seeder:** operaciones VERIFY/SEED/RESET_TEST_DATA/RESET_AND_SEED correctas.

## 8. Criterio de mantenimiento

Este documento debe actualizarse cuando:

- Se agregue una nueva entidad o tabla al dominio.
- Se modifiquen contratos HTTP vigentes.
- Se cambie el contrato de build (Java, Spring Boot, dependencias criticas).
- Se ejecute la prueba de reconstruccion real y se actualice el Gate 2.
- Cambie el estado de alguna condicion de la tabla de suficiencia.

No puede quedar desactualizado. Si la spec deja de ser suficiente para
clean-room, reportar inmediatamente y resolver antes de continuar.

# Spec-as-source canónica — API Faltas Core

## 1. Autoridad

Esta carpeta es la única spec-as-source canónica del backend productivo de Faltas.

Las reglas funcionales, arquitectónicas y de persistencia aprobadas deben quedar expresadas aquí antes de considerarse vigentes.

El código Java, los tests, los modelos anteriores, las matrices, los deltas, los handoffs y la historia Git son fuentes de evidencia, auditoría o conformidad. No pueden redefinir silenciosamente la spec canónica.

Durante la canonicalización actual, esas fuentes se utilizan para detectar reglas todavía no trasladadas, contradicciones y gaps. Toda diferencia debe resolverse de forma explícita y quedar incorporada primero en la spec.

## 2. Resolución de contradicciones

Ante una contradicción:

1. no elegir una versión por fecha, nombre de archivo, cantidad de tests o cercanía con el código;
2. no inventar una conciliación;
3. registrar y reportar el gap;
4. obtener una decisión humana explícita cuando afecte dominio, contratos o arquitectura;
5. actualizar primero la spec canónica;
6. alinear después código, tests, persistencia y documentos de referencia.

Una implementación existente demuestra comportamiento, pero no convierte automáticamente ese comportamiento en regla normativa.

## 3. Orden de lectura vigente

Para cualquier trabajo en `backend/api-faltas-core`:

1. leer este `README.md`;
2. identificar en la clasificación siguiente los documentos aplicables;
3. leer solamente el subconjunto canónico necesario;
4. consultar código y tests como evidencia de conformidad;
5. consultar `docs/faltas/` y otros documentos externos únicamente como fuentes de auditoría o diseño físico;
6. detenerse y reportar si existe una contradicción no resuelta.

## 4. Clasificación documental durante la canonicalización

### 4.0 Documentos tematicos canonicos

Estos documentos forman la capa normativa autosuficiente de la spec. Deben
leerse en este orden antes de consultar los documentos de contrato funcional:

1. [`00-governance/glossary.md`](00-governance/glossary.md) -- define el
   vocabulario canonico del dominio; un termino canonico tiene un unico
   significado.
2. [`10-domain/lifecycle-states.md`](10-domain/lifecycle-states.md) -- define
   las dimensiones de estado y el lifecycle de subagregados; establece que es
   persistido, derivado o historico.
3. [`10-domain/firma-notificacion-fallo.md`](10-domain/firma-notificacion-fallo.md)
   -- aplica esas dimensiones al circuito de firma y cola notificatoria del fallo.

Ante contradiccion entre un documento tematico y un documento de contrato
funcional (4.1), el documento tematico es normativo en lo que respecta a
definiciones de terminos, dimensiones y lifecycle.

### 4.1 Contrato funcional vigente

Estos documentos contienen actualmente el contrato funcional que debe preservarse mientras se reorganiza la spec:

- `02-estados-bloques-eventos.md`
- `03-comandos-precondiciones-efectos.md`
- `04-snapshot-bandejas-acciones.md`
- `05-api-core-endpoints.md`
- `104-plantillas-redaccion-combinacion-documentos.md`
- `10-domain/firma-notificacion-fallo.md` — firma, cola notificatoria y lifecycle de `EstadoFalloActa`

Su estructura todavía puede contener evolución por slices. Esa cronología será absorbida gradualmente en documentos temáticos sin eliminar reglas vigentes.

### 4.2 Persistencia, arquitectura y paridad

Estos documentos gobiernan transitoriamente la incorporación de MariaDB y deben leerse junto con el contrato funcional aplicable:

- `102-slice-9-estrategia-jdbc-mariadb.md`
- `110-matriz-maestra-paridad-mariadb-inmemory.md`

La matriz de paridad es una herramienta de verificación. No reemplaza las reglas funcionales del dominio ni autoriza a inferir equivalencias entre conceptos distintos.

### 4.3 Baseline y trabajo pendiente

- `99-pendientes-siguientes-slices.md`

Este archivo informa el baseline y el próximo trabajo autorizado. No define por sí solo reglas de dominio.

### 4.4 Evidencia, auditoría o historia no normativa

Los siguientes materiales no deben utilizarse como autoridad normativa ni para reemplazar documentos vigentes:

- `06-tests-core.md`
- `101-auditoria-pre-jdbc-mariadb.md`
- `103-slice-9-1-infraestructura-jdbc.md`
- `108-frontend-ready-demo.md`
- `109-delta-modelo-mariadb-inmemory.md`
- `handoff/`

Pueden consultarse para recuperar evidencia, trazabilidad o decisiones todavía no absorbidas. No deben eliminarse hasta verificar que todo contenido vigente haya sido incorporado y trazado en la nueva organización.

## 5. Fuentes externas a esta carpeta

`docs/faltas/` contiene insumos históricos y de diseño para MariaDB, proceso y decisiones de dominio. Son obligatorios como material de auditoría cuando el alcance los involucra, pero no prevalecen sobre esta spec.

Los tests ejecutables y la implementación InMemory constituyen la evidencia principal para comprobar paridad durante la incorporación de MariaDB. Si difieren de la spec, debe registrarse el gap y resolverse; no se debe modificar la spec automáticamente para copiar el código.

El prototipo y Angular son evidencia de UX o integración. Nunca constituyen por sí solos una fuente normativa de dominio.

## 6. Guardrails

- No inventar eventos, bloques, estados, bandejas, acciones ni catálogos.
- No confundir estado de dominio, estado documental, bandeja operativa, acción disponible o proyección de snapshot.
- No modificar reglas funcionales para facilitar la persistencia.
- No reintroducir conceptos explícitamente eliminados.
- No usar cronologías de slices como sustituto de una definición vigente consolidada.
- No eliminar documentación durante la canonicalización sin extracción y trazabilidad previas.
- No considerar cerrado un cambio documental si deja referencias rotas o fuentes de autoridad contradictorias.

## 7. Objetivo de la canonicalización

La spec final debe permitir reconstruir desde cero, sin consultar el código para descubrir reglas:

- el modelo de dominio;
- catálogos y códigos persistibles;
- estados y transiciones;
- comandos, precondiciones y efectos;
- eventos;
- snapshot, bandejas y acciones;
- contratos HTTP y errores;
- seguridad, actor, tiempo y auditoría;
- persistencia MariaDB;
- transacciones, concurrencia e idempotencia;
- artefactos Java esperados;
- tests de aceptación y trazabilidad.

Hasta completar esa reorganización, este README define cómo interpretar y auditar el conjunto documental existente.

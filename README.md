# Bod-Faltas

## 1. Qué es

Repositorio canónico del ecosistema municipal de Faltas.

El sistema se modela como un gestor documental orientado al expediente:

- el acta es la unidad principal de gestión;
- los documentos, firmas, notificaciones, pagos y actuaciones producen efectos de dominio;
- las bandejas y acciones son proyecciones operativas del estado vigente;
- la persistencia debe reproducir el comportamiento validado sin redefinir reglas funcionales.

## 2. Organización general

- `backend/api-faltas-core/` — backend productivo y dominio principal;
- `backend/api-faltas-core/docs/spec-as-source/` — única spec-as-source canónica del backend productivo;
- `backend/api-faltas-prototipo/` — prototipo y soporte de demostración, no fuente normativa;
- `apps/` — aplicaciones consumidoras;
- `shared/` — contratos y recursos compartidos;
- `infra/` — infraestructura y scripts.

## 3. Autoridad normativa

La única spec-as-source canónica del backend productivo de Faltas está en:

`backend/api-faltas-core/docs/spec-as-source/`

Su punto de entrada obligatorio es:

`backend/api-faltas-core/docs/spec-as-source/README.md`

El código Java, los tests, el prototipo, Angular y la historia Git son fuentes de evidencia, auditoría o conformidad. No pueden redefinir silenciosamente la spec canónica.

Ante una contradicción:

1. no elegir una versión por fecha, nombre de archivo, cantidad de tests o cercanía con el código;
2. registrar el gap;
3. obtener una resolución explícita cuando afecte dominio, contratos o arquitectura;
4. actualizar primero la spec canónica;
5. alinear después código, tests, persistencia y documentos de referencia.

## 4. Cómo empezar a leer

Para cualquier trabajo sobre `backend/api-faltas-core`:

1. leer [AGENTS.md](AGENTS.md);
2. leer el [README de la spec canónica](backend/api-faltas-core/docs/spec-as-source/README.md);
3. identificar allí los documentos aplicables al alcance;
4. consultar código y tests como evidencia de conformidad.

No comenzar por documentos históricos, handoffs, logs ni conteos antiguos de tests.

## 5. Arquitectura base

Este repositorio se organiza como repo multiproyecto con:

- núcleo común de dominio y reglas;
- backend productivo;
- prototipo y superficies demo separadas;
- aplicaciones consumidoras;
- persistencia desacoplada mediante ports y adapters;
- spec canónica temática y trazable.

El motor de firma es un sistema externo con repositorio propio. En este repositorio se modelan su integración, los estados y efectos esperados y los mecanismos de prueba necesarios.

## 6. Continuidad y trabajo operativo

No existe un directorio fijo de continuidad en este repositorio ni un archivo que
deba leerse siempre antes de cada slice. Los artefactos de continuidad (prompts
de reanudación, cierres de slice, handoffs, checklists, informes) son
transitorios y su lectura, creación o eliminación requiere autorización
humana explícita en cada caso (ver `.cursor/rules/continuidad-solo-humana.mdc`
y `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`).

La continuidad válida de un trabajo debe venir indicada expresamente por:

- el prompt del slice actual;
- el commit o rama de entrada;
- el README canónico;
- los documentos canónicos aplicables.

Los archivos de continuidad no reemplazan la spec.

## 7. Regla de trabajo asistido

Todo agente debe:

- trabajar con alcance explícito;
- leer el contexto mínimo necesario;
- no inventar reglas;
- no confundir estados de dominio, subestados, bandejas, acciones o proyecciones;
- no modificar documentación histórica fuera del alcance;
- no hacer commits ni staging salvo autorización expresa;
- reportar contradicciones en lugar de resolverlas por inferencia.

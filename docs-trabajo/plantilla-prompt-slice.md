# {{ID_SLICE}} — {{TÍTULO}}

## 1. Repositorio y rama

Repositorio:

`{{RUTA_REPOSITORIO}}`

Rama obligatoria:

`{{RAMA}}`

Commit o baseline de entrada:

`{{COMMIT_O_BASELINE}}`

## 2. Objetivo

{{OBJETIVO_PUNTUAL_Y_OBSERVABLE}}

## 3. Autoridad y contexto

Punto de entrada obligatorio:

`backend/api-faltas-core/docs/spec-as-source/README.md`

Documentos canónicos aplicables:

- `{{RUTA_SPEC_1}}`
- `{{RUTA_SPEC_2}}`

Fuentes de evidencia permitidas:

- `{{RUTA_JAVA_O_TEST_1}}`
- `{{RUTA_JAVA_O_TEST_2}}`

Los documentos de `docs/faltas/`, `docs-trabajo/`, handoffs, prototipo o frontend solo deben consultarse si este prompt los autoriza expresamente.

Ante una contradicción, no elegir una versión por inferencia. Reportar el gap y detener la decisión afectada.

## 4. Precondiciones

Antes de modificar:

1. verificar rama;
2. verificar commit o baseline;
3. verificar working tree limpio;
4. usar PowerShell 7;
5. respetar UTF-8 y `.gitattributes`;
6. no hacer staging, commit, push, reset ni clean.

## 5. Alcance exacto autorizado

Crear:

- `{{ARCHIVO_NUEVO}}`

Modificar:

- `{{ARCHIVO_MODIFICABLE}}`

Eliminar:

- `{{ARCHIVO_ELIMINABLE}}`

No modificar ningún archivo fuera de esta lista.

## 6. Comportamiento o contenido requerido

{{DEFINICIÓN_COMPLETA_DEL_CAMBIO}}

## 7. Fuera de alcance

- {{FUERA_DE_ALCANCE_1}}
- {{FUERA_DE_ALCANCE_2}}
- cambios oportunistas;
- refactors no necesarios;
- correcciones de encoding fuera de los archivos autorizados;
- actualización de baselines no solicitada;
- ejecución del siguiente slice.

## 8. Criterios de aceptación

El slice queda cerrado cuando:

- {{CRITERIO_1}}
- {{CRITERIO_2}}
- {{CRITERIO_3}}
- no hay cambios fuera de alcance;
- `git diff --check` no reporta errores;
- no hubo staging ni commit.

## 9. Validaciones

Ejecutar:

```powershell
git status --short --branch
git diff --check
{{COMANDO_VALIDACIÓN}}
```

No imprimir el diff completo en consola salvo error.

## 10. Salida esperada

Informar:

- archivos modificados;
- resumen del cambio;
- validaciones ejecutadas;
- gaps o desviaciones;
- confirmación de que no hubo staging ni commit.

No ejecutar tareas posteriores sin autorización.

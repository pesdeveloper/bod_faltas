# Fuentes de referencia — Faltas / Dirección de Faltas

Esta carpeta contiene insumos históricos, funcionales y estructurales utilizados para auditar y completar la spec canónica del backend productivo de Faltas.

La única spec-as-source normativa está en:

`backend/api-faltas-core/docs/spec-as-source/`

Su punto de entrada obligatorio es:

`backend/api-faltas-core/docs/spec-as-source/README.md`

Los documentos de esta carpeta no pueden redefinir silenciosamente la spec canónica.

## 1. Modelo MariaDB de referencia

Archivo:

`MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`

Rol:

- insumo estructural para tablas, campos, convenciones técnicas y persistencia;
- fuente de auditoría para la reconstrucción del modelo MariaDB;
- referencia que debe reconciliarse con la spec vigente, la implementación y los tests.

No debe aplicarse mecánicamente ni prevalecer ante una decisión canónica posterior.

## 2. Matriz de proceso de referencia

Archivo:

`MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md`

Rol:

- insumo funcional para acciones, precondiciones, eventos, transiciones y cerrabilidad;
- fuente de auditoría durante la consolidación temática de la spec;
- evidencia histórica de decisiones de proceso.

No reemplaza los documentos funcionales canónicos de `spec-as-source`.

## 3. Delta de implementación InMemory

Archivo:

`DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`

Rol:

- registrar diferencias detectadas entre el modelo anterior y la implementación validada;
- aportar evidencia para la matriz de paridad;
- evitar que una diferencia conocida se pierda al diseñar MariaDB.

El delta no es una segunda spec ni puede resolver contradicciones por sí solo.

## 4. Decisiones de dominio

Archivo:

`domain-decisions.md`

Rol:

- conservar decisiones explícitas que deben ser verificadas contra la spec canónica;
- aportar contexto para la auditoría y trazabilidad.

Toda decisión todavía vigente debe terminar absorbida por el documento canónico temático correspondiente.

## 5. Regla de autoridad

Ante una diferencia entre esta carpeta y `spec-as-source`:

1. no elegir automáticamente el documento más nuevo;
2. no elegir automáticamente el comportamiento del código;
3. registrar el gap;
4. resolverlo de forma explícita;
5. actualizar primero la spec canónica;
6. alinear después modelo, código, tests y documentación de referencia.

## 6. Política de cambios

No editar estos documentos de forma oportunista para hacerlos coincidir con una implementación.

Cuando aparezca una diferencia:

- identificar la regla afectada;
- localizar evidencia en Java y tests;
- determinar si es un gap documental, una decisión pendiente o un defecto;
- registrar la resolución en la spec canónica;
- actualizar esta documentación solamente si sigue siendo una referencia activa y la actualización está dentro del alcance autorizado.

La historia de cambios se conserva en Git y no debe mezclarse con el contrato vigente.

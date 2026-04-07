# Sistema de Faltas Municipal — Spec-as-Source

Este repositorio contiene la especificación funcional, técnica y arquitectónica del sistema de faltas municipal, orientada a implementación asistida por IA.

El objetivo es que la especificación sea la fuente principal de verdad para diseñar, construir, revisar y evolucionar el sistema.

---

## Estructura del repositorio

### `spec/`
Contiene la **verdad canónica vigente** del sistema.

Todo desarrollo nuevo, análisis arquitectónico, generación asistida con IA e implementación en Cursor debe tomar esta carpeta como fuente principal.

### `historico/`
Contiene la evolución del diseño, modelos previos, alternativas evaluadas, versiones anteriores y material de respaldo.

Esta carpeta se conserva por trazabilidad, pero **no debe tomarse como fuente principal de verdad**.

---

## Regla de lectura

Para comprender el sistema y poder implementarlo correctamente, el orden recomendado de lectura es:

1. `spec/00-readme/00-vision-general.md`
2. `spec/00-readme/01-principios-arquitectonicos.md`
3. `spec/00-readme/02-stack-por-bloque.md`
4. `spec/01-dominio/00-modelo-canonico.md`
5. `spec/03-catalogos/`
6. `spec/04-snapshot/`
7. `spec/05-queries/`
8. `spec/06-contratos/`
9. `spec/07-implementacion/`
10. `spec/08-ddl-logico/`

---

## Regla de implementación

La implementación debe basarse en `spec/`.

Los documentos ubicados en `historico/` pueden consultarse como respaldo, contexto o trazabilidad de decisiones, pero no deben utilizarse como fuente principal para modelado o generación de código.

---

## Propósito del enfoque spec-as-source

Este repositorio busca que:

- ChatGPT actúe como arquitecto y diseñador del sistema
- Cursor actúe como implementador asistido
- el usuario actúe como supervisor funcional y técnico
- el sistema pueda evolucionar con trazabilidad, simplicidad y consistencia

---

## Principios rectores del sistema

- el centro del dominio es `Acta`
- `ActaEvento` expresa la narrativa procesal real del caso
- `Documento` es el soporte formal de los actos relevantes
- `ActaSnapshotOperativo` es una proyección derivada, no fuente de verdad
- `Notificacion` es el único satélite persistente específico que se mantiene como entidad propia
- el componente económico se resuelve por integración, no como subdominio central de faltas
- se prioriza simplicidad, implementabilidad directa y compatibilidad con generación asistida por IA

---

## Estado actual

El modelo fue consolidado y simplificado.  
La siguiente etapa del trabajo consiste en transformar el diseño consolidado en especificación canónica implementable.

Orden previsto de avance:

1. stack tecnológico por bloque
2. modelo canónico del dominio
3. eventos y enums globales
4. queries y bandejas operativas
5. snapshot engine
6. contratos
7. lineamientos de implementación para Cursor

---

## Convención de documentos canónicos

Cada documento dentro de `spec/` debe incluir, en la medida de lo posible, este encabezado mínimo:

- Estado
- Última actualización
- Propósito
- Relación con otros documentos

---

## Importante

Si existe contradicción entre:

- un documento de `historico/`
- y un documento de `spec/`

prevalece siempre el documento ubicado en `spec/`.
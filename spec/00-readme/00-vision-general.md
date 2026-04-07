# [CANONICO] VISIÓN GENERAL DEL SISTEMA

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir la visión general, el alcance, la lógica central y el enfoque arquitectónico del sistema de faltas municipal, de forma que sirva como punto de entrada común para análisis, diseño e implementación.

# Relación con otros documentos
- `spec/00-readme/01-principios-arquitectonicos.md`
- `spec/00-readme/02-stack-por-bloque.md`
- `spec/01-dominio/00-modelo-canonico.md`

---

# 1. Finalidad del sistema

El sistema tiene por finalidad gestionar de manera integral el ciclo de vida de las actas y actuaciones vinculadas al régimen municipal de faltas, desde su labrado inicial hasta su resolución, notificación, eventual revisión, derivación externa y cierre.

Debe permitir operar el proceso completo de manera trazable, controlada y compatible con la realidad administrativa municipal.

El sistema debe servir tanto para:

- operación diaria
- control administrativo
- trazabilidad jurídica
- soporte documental
- integración con sistemas externos o preexistentes
- futura implementación asistida por IA

---

# 2. Enfoque general

El sistema adopta un enfoque **event-driven orientado a proceso administrativo real**, pero con una implementación simple, directa y controlada.

No se busca construir un ecosistema excesivamente abstracto ni una arquitectura sobre-modelada.

La regla rectora es:

**evento + documento + snapshot**

Esto significa que el sistema se apoya en tres piezas principales:

- una entidad central de negocio
- una bitácora procesal real
- una proyección operativa derivada

---

# 3. Núcleo conceptual del modelo

## 3.1 Acta
`Acta` es el agregado raíz del sistema.

Es la entidad principal alrededor de la cual gira el trámite completo.

El sistema no se organiza alrededor de una “causa” separada como entidad central distinta del acta.  
La acta es la unidad principal de identidad administrativa, operativa y procesal.

---

## 3.2 ActaEvento
`ActaEvento` registra los **hechos procesales reales** ocurridos durante el ciclo de vida de la acta.

No representa logs técnicos ni eventos internos de infraestructura.

Su función es construir la narrativa del caso:

- qué pasó
- cuándo pasó
- quién lo provocó
- qué efecto produjo
- qué documento lo respalda, si corresponde

`ActaEvento` es la bitácora procesal del sistema.

---

## 3.3 Documento
`Documento` representa el soporte formal de los actos, presentaciones, constancias, resoluciones, fallos, notificaciones u otros elementos documentales relevantes del trámite.

El sistema utiliza un modelo documental unificado.

No se buscan submodelos documentales fragmentados por cada subtipo funcional.  
La semántica la determina el tipo documental, su estado y su vinculación con la acta y los eventos relevantes.

---

## 3.4 ActaSnapshotOperativo
`ActaSnapshotOperativo` es una proyección derivada del historial válido de la acta.

No es fuente de verdad.

Su función es resolver de manera rápida y operativa:

- estado actual
- bandejas
- filtros
- búsquedas
- indicadores
- lecturas frecuentes

El snapshot debe poder regenerarse.

---

## 3.5 Notificacion
`Notificacion` es el único satélite persistente específico que se mantiene como entidad propia fuera del núcleo mínimo.

Se conserva como entidad explícita porque el proceso de notificación tiene complejidad operativa suficiente como para justificar su tratamiento particular:

- envíos
- canales
- acuses
- rechazos
- reenvíos
- estado operativo de la gestión de notificación

---

# 4. Qué NO es este sistema

Este sistema no debe transformarse en:

- un BPM genérico
- un motor abstracto de expedientes sin identidad concreta
- un repositorio de logs técnicos
- un sistema contable/económico completo
- un conjunto de submódulos sobre-modelados y difíciles de implementar

La arquitectura debe permanecer simple, fuerte en semántica y directa en ejecución.

---

# 5. Principio de simplificación del dominio

El modelo consolidado parte de una decisión fuerte:

- evitar entidades satélite innecesarias
- absorber complejidad cuando no aporte ventaja real
- modelar solo aquello que tenga impacto operativo, administrativo o jurídico

Por ello, el sistema adopta una estructura simplificada donde:

- `Acta` concentra la identidad principal
- `ActaEvento` expresa la narrativa procesal
- `Documento` expresa el soporte formal
- `ActaSnapshotOperativo` expresa la lectura derivada
- `Notificacion` se mantiene como único satélite persistente específico

---

# 6. Proceso cubierto

El sistema cubre el recorrido completo del trámite, incluyendo como mínimo:

- D1 — labrado o creación del acta
- D2 — enriquecimiento administrativo del acta
- D3 — notificación del acta
- D4 — análisis, presentaciones, pagos y actuaciones intermedias
- D5 — acto administrativo
- D6 — notificación del acto
- D7 — apelación o revisión
- D8 — gestión externa, cierre, archivo o finalización

Además, contempla procesos transversales como:

- proceso documental formal
- proceso de notificación y acuse
- integración económica
- integración externa

---

# 7. Regla sobre los eventos

El sistema distingue entre:

## Eventos procesales reales
Son los únicos que integran `ActaEvento`.

Ejemplos:
- acta labrada
- documento incorporado
- notificación enviada
- acuse recibido
- apelación interpuesta
- archivo dispuesto

## Eventos técnicos
No forman parte de `ActaEvento`.

Ejemplos:
- error de red
- reintento de job
- webhook recibido
- archivo movido de carpeta
- log interno de integración

Los eventos técnicos pueden existir en subsistemas de soporte, pero no forman parte de la narrativa procesal del caso.

---

# 8. Regla sobre el componente económico

El componente económico no forma parte del dominio central del sistema de faltas.

El sistema de faltas solo debe resolver:

- la integración con el sistema económico municipal
- la relación funcional con deuda/pago/estado externo
- la persistencia mínima necesaria para trazabilidad e interoperabilidad

No se modela internamente un subdominio económico completo dentro de faltas.

---

# 9. Canales de operación

El sistema debe poder operarse desde dos canales principales:

## 9.1 Web interna
Debe permitir la operación administrativa completa del sistema, incluyendo también la posibilidad de realizar D1 desde entorno web.

## 9.2 Móvil / PDA
Debe permitir operación de campo, especialmente para D1, con enfoque offline-first y posterior sincronización.

Esto implica que la creación del acta debe estar disponible tanto en:

- frontend web
- aplicación móvil

---

# 10. Criterio de diseño para implementación asistida por IA

Este sistema se diseña bajo una lógica **IA-first**, donde la especificación debe ser lo suficientemente clara, estable y modular como para permitir:

- generación de código asistida
- validación automatizada
- implementación incremental
- bajo riesgo de ambigüedad semántica
- trazabilidad entre diseño y construcción

Por esta razón, la especificación canónica debe:

- evitar duplicaciones
- congelar vocabulario
- separar histórico de canon
- expresar reglas con precisión
- ser utilizable por humanos y por asistentes de implementación

---

# 11. Objetivo del repositorio canónico

La carpeta `spec/` debe evolucionar hasta convertirse en la fuente principal para:

- entender el sistema
- generar backend
- generar frontend
- construir contratos
- definir queries
- modelar snapshots
- organizar testing
- guiar a Cursor y otras herramientas de IA

---

# 12. Estado del trabajo

El modelo conceptual ya fue consolidado.

La etapa actual consiste en transformar esa consolidación en una especificación canónica implementable, mediante este orden de trabajo:

1. visión general
2. principios arquitectónicos
3. stack tecnológico por bloque
4. modelo canónico del dominio
5. eventos y enums globales
6. queries y bandejas operativas
7. snapshot engine
8. contratos
9. lineamientos de implementación

---

# 13. Regla de verdad vigente

La verdad vigente del sistema reside exclusivamente en `spec/`.

El material ubicado en `historico/` se conserva como respaldo, trazabilidad y contexto de evolución, pero no debe prevalecer sobre la especificación canónica actual.


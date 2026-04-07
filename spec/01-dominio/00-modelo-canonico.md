# [CANONICO] MODELO CANÓNICO DEL DOMINIO

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el modelo canónico y vigente del dominio central del sistema de faltas municipal, estableciendo sus piezas principales, sus responsabilidades, sus relaciones y sus límites, sin caer en sobre-modelado ni reintroducir entidades descartadas.

# Relación con otros documentos
- `spec/00-readme/00-vision-general.md`
- `spec/00-readme/01-principios-arquitectonicos.md`
- `spec/03-catalogos/`
- `spec/04-snapshot/`
- `spec/06-contratos/`
- `spec/08-ddl-logico/`

---

# 1. Regla general del modelo

El modelo canónico del sistema se apoya en una estructura simple y deliberada:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`
- `ActaSnapshotOperativo`

La regla rectora del diseño es:

**evento + documento + snapshot**

Esto significa que el sistema busca resolver la mayor parte del dominio mediante:

- una entidad principal
- una narrativa procesal explícita
- un soporte documental unificado
- una proyección operativa derivada

---

# 2. Piezas centrales del modelo

## 2.1 Acta
Es el agregado raíz del sistema.

Representa la unidad principal del trámite y concentra la identidad administrativa, operativa y procesal del caso.

Debe contener, según corresponda:

- identidad principal
- metadatos operativos
- referencias necesarias del caso
- estado actual mínimo
- campos snapshot mínimos indispensables
- vínculos con documentos, notificaciones y eventos

La acta es el centro del sistema.

---

## 2.2 ActaEvento
Es la bitácora procesal del caso.

Registra exclusivamente hechos procesales reales:

- administrativos
- jurídicos
- operativos

No debe registrar logs técnicos ni eventos de infraestructura.

Su función es narrar el viaje de la acta con precisión suficiente para reconstruir:

- evolución del caso
- cambios relevantes
- decisiones
- pasos del proceso
- relación con documentación formal

---

## 2.3 Documento
Es el modelo documental unificado del sistema.

Representa los soportes formales del trámite, como por ejemplo:

- acta
- acta complementaria
- resolución
- fallo
- notificación
- presentación
- constancia
- actuación
- documento re-subido

La interpretación concreta del documento surge de:

- su tipo
- su estado
- su firma, si corresponde
- su vínculo con la acta
- su vínculo con eventos relevantes

---

## 2.4 Notificacion
Es el único satélite persistente específico que se mantiene como entidad propia.

Se modela de forma explícita porque la gestión de notificación tiene comportamiento operativo propio y relevante:

- canal
- envío
- acuse
- rechazo
- reenvío
- estado operativo

`Notificacion` no reemplaza el evento procesal.  
La notificación se gestiona como entidad operativa, mientras que su efecto procesal relevante también puede expresarse en `ActaEvento`.

---

## 2.5 ActaSnapshotOperativo
Es una proyección derivada y regenerable.

Resume el estado operativo actual del caso para facilitar:

- bandejas
- filtros
- búsquedas
- métricas
- lectura rápida
- priorización de trabajo

No es fuente de verdad y no debe usarse como justificación para relajar la consistencia del modelo central.

---

# 3. Modelo mínimo y suficiente

El modelo canónico adopta una postura de diseño mínima pero suficiente.

Eso significa que no se crean entidades específicas por cada aspecto del proceso cuando la necesidad puede resolverse razonablemente mediante:

- `ActaEvento`
- `Documento`
- `Notificacion`
- catálogos
- snapshot
- contratos
- integración

El objetivo es evitar proliferación de submodelos satélite de baja rentabilidad arquitectónica.

---

# 4. Entidades descartadas como núcleo vigente

Las siguientes ideas o entidades no forman parte del modelo canónico actual como agregados autónomos centrales:

- `ActaPresentacion`
- `ActaActo`
- `ActaDerivacionExterna`
- cualquier entidad de “causa” que reemplace a `Acta` como centro del sistema
- subdominio económico interno completo

Esto no significa que sus conceptos desaparezcan del sistema.

Significa que se absorben mediante:

- eventos
- documentos
- relaciones
- catálogos
- contratos
- integración externa
- snapshot

---

# 5. Relación entre piezas

## 5.1 Acta y ActaEvento
Una acta puede tener múltiples eventos.

Los eventos construyen la narrativa del caso y explican el tránsito del expediente.

## 5.2 Acta y Documento
Una acta puede tener múltiples documentos asociados.

No todos los documentos implican un evento propio, pero los documentos con impacto procesal relevante normalmente deben relacionarse también con un evento.

## 5.3 Acta y Notificacion
Una acta puede tener múltiples notificaciones.

Las notificaciones modelan la gestión operativa del envío y recepción por canal.

## 5.4 Acta y Snapshot
Una acta tiene una proyección operativa actual, regenerable.

## 5.5 Evento y Documento
Un evento puede estar respaldado por un documento.

No todos los eventos requieren documento, pero cuando el hecho solo existe jurídicamente a través de un documento, la vinculación debe existir.

---

# 6. Regla de oro del evento

Un hecho debe modelarse como `ActaEvento` si cumple uno o más de estos criterios:

- altera el estado procesal del caso
- representa una decisión administrativa o jurídica
- implica un hito operativo relevante
- debe quedar en la narrativa del trámite
- requiere trazabilidad explícita frente a auditoría o revisión

No debe modelarse como `ActaEvento` si solo representa:

- soporte técnico
- logging interno
- sincronización técnica
- estado de infraestructura
- detalle de storage
- reintentos internos sin efecto procesal

---

# 7. Regla de oro del documento

Un documento debe modelarse como `Documento` cuando represente un soporte formal relevante del trámite.

La semántica documental no debe multiplicarse en nuevas entidades si puede quedar resuelta por:

- tipo documental
- estado documental
- firma
- relación con acta
- relación con evento

La complejidad documental debe concentrarse en el modelo documental, no dispersarse en submodelos aislados por etapa.

---

# 8. Regla de oro de la notificación

La notificación se mantiene como entidad específica porque tiene estado, ciclo y gestión propia.

Sin embargo:

- la notificación no reemplaza a los eventos
- el acuse no reemplaza al historial
- el canal no define el dominio completo del caso

La entidad `Notificacion` resuelve la operación del proceso de notificación.  
`ActaEvento` resuelve la narrativa procesal relevante derivada de ese proceso.

---

# 9. Regla de oro del snapshot

El snapshot existe para lectura rápida y operación.

Por lo tanto:

- no debe concentrar lógica de verdad
- no debe reemplazar eventos
- no debe ser el único lugar donde vive el estado del caso
- debe poder recalcularse

La relación correcta es:

- el dominio produce hechos
- los hechos producen proyecciones
- el snapshot resume la situación operativa actual

---

# 10. Cobertura del proceso dentro del modelo

El modelo canónico debe permitir representar sin necesidad de nuevos agregados centrales:

- creación/labrado del acta
- enriquecimiento del acta
- documentación relevante
- notificación del acta
- análisis y presentaciones
- pagos e impactos procesales mínimos
- acto administrativo
- notificación del acto
- apelación o revisión
- derivación externa
- reingreso desde gestión externa
- paralización
- archivo
- anulación
- cierre o finalización

---

# 11. Límite del componente económico

El modelo canónico no incluye un subdominio económico completo.

Solo contempla lo necesario para:

- relación con sistema económico municipal
- trazabilidad mínima de vínculo
- reflejo de estados externos relevantes
- soporte a consultas y decisiones del trámite

El detalle económico profundo vive fuera del núcleo del sistema de faltas.

---

# 12. Límite del componente externo

La derivación o gestión externa forma parte del proceso del caso, pero no requiere un agregado autónomo central específico mientras pueda resolverse con:

- eventos
- documentos
- referencias externas
- contratos de integración
- snapshot

Solo si el futuro demuestra una complejidad operacional mucho mayor, podría evaluarse una especialización adicional.  
Eso hoy no forma parte del canon.

---

# 13. Límite del componente presentación/recurso/acto

Presentaciones, actos, apelaciones o intervenciones similares no se modelan como agregados satélite independientes por defecto.

Se expresan mediante combinación de:

- eventos
- documentos
- estados
- contratos
- snapshot

Esta decisión reduce complejidad y mejora implementabilidad.

---

# 14. Canalidad del dominio

El dominio es único aunque existan múltiples canales de operación.

La captura desde:

- web
- móvil / PDA

debe converger en los mismos conceptos canónicos:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`
- `ActaSnapshotOperativo`

No deben existir “versiones del dominio” distintas por canal.

---

# 15. Relación con el stack tecnológico

Este modelo fue pensado para implementarse con un stack que favorezca:

- claridad de contratos
- SQL explícito
- trazabilidad
- modularidad funcional
- bajo acoplamiento
- generación asistida por IA

Por eso el modelo canónico evita estructuras excesivamente profundas o dependientes de frameworks específicos.

---

# 16. Relación con el DDL lógico

El DDL lógico futuro debe responder a este modelo, no al revés.

Eso implica que:

- la estructura de tablas debe reflejar el canon
- no deben reintroducirse tablas descartadas por inercia histórica
- toda tabla nueva debe justificarse contra este documento

---

# 17. Regla de consistencia del canon

Ante cualquier duda de modelado, deben preferirse estas opciones en este orden:

1. usar evento
2. usar documento
3. usar notificación, si el caso lo exige
4. derivar a snapshot
5. integrar con sistema externo
6. recién en último caso, evaluar nueva entidad persistente específica

---

# 18. Resumen ejecutivo del modelo

El modelo canónico vigente del sistema de faltas municipal se resume así:

- `Acta` es el centro del dominio
- `ActaEvento` es la narrativa procesal
- `Documento` es el soporte formal
- `Notificacion` es el único satélite persistente específico
- `ActaSnapshotOperativo` es la lectura operativa derivada
- el económico se resuelve por integración
- la complejidad se absorbe antes de crear nuevas entidades
- el canon debe permanecer simple, estable e implementable

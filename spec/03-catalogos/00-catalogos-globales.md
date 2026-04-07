# [CANONICO] CATÁLOGOS GLOBALES

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir el conjunto de catálogos globales obligatorios del sistema de faltas municipal, estableciendo cuáles son los enums y clasificaciones base que deben utilizarse de forma consistente en dominio, contratos, snapshot, queries, frontend, móvil e implementación.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/04-snapshot/`
- `spec/05-queries/`
- `spec/06-contratos/`

---

# 1. Finalidad de los catálogos globales

Los catálogos globales existen para congelar el vocabulario operativo y técnico del sistema.

Deben permitir que:

- el dominio use términos estables
- los contratos no sean ambiguos
- el snapshot derive estados de manera uniforme
- las bandejas filtren de forma consistente
- frontend, móvil y backend hablen el mismo idioma
- la implementación asistida por IA no mezcle nombres o significados

---

# 2. Regla general de uso

Un catálogo global debe utilizarse cuando una clasificación:

- sea transversal al sistema
- impacte en persistencia, contratos o lógica
- deba mantenerse estable en el tiempo
- requiera consistencia entre módulos

No deben crearse enums o catálogos locales si el concepto ya tiene versión canónica definida.

---

# 3. Catálogos globales canónicos del sistema

El conjunto mínimo de catálogos globales canónicos es el siguiente:

1. `EstadoActa`
2. `TipoEventoActa`
3. `TipoDocumento`
4. `EstadoDocumento`
5. `CanalNotificacion`
6. `EstadoNotificacion`
7. `OrigenEvento`
8. `FirmaTipo`
9. `TipoPresentacion`
10. `TipoGestionExterna`
11. `ResultadoExterno`
12. `MotivoCierreActa`

Estos catálogos deben ser utilizados como fuente de verdad por todo el sistema.

---

# 4. Criterios de diseño de los catálogos

## 4.1 Un concepto, un catálogo
No deben existir múltiples catálogos para expresar la misma idea con nombres distintos.

## 4.2 Un catálogo, una responsabilidad
Cada catálogo debe representar una dimensión semántica clara.

Ejemplos:
- estado de la acta
- tipo de evento
- canal de notificación
- tipo documental

No deben mezclarse varias dimensiones dentro de un mismo catálogo.

## 4.3 Lenguaje de dominio
Los nombres deben surgir del dominio y del proceso administrativo real, no del framework o de la tecnología.

## 4.4 Estabilidad
Una vez fijado un nombre canónico, debe sostenerse en:
- documentos
- DTOs
- contratos
- base de datos
- frontend
- móvil
- prompts de implementación

## 4.5 No duplicación por etapa
No deben existir duplicados innecesarios como:
- `NOTIFICACION_ACTA_ENVIADA`
- `NOTIFICACION_FALLO_ENVIADA`

si el contexto ya puede determinar de qué notificación se trata.

---

# 5. Catálogos centrales y su función

## 5.1 EstadoActa
Define la situación operativa/procesal actual consolidada del caso.

## 5.2 TipoEventoActa
Define los hechos procesales reales que pueden integrar la narrativa de `ActaEvento`.

## 5.3 TipoDocumento
Define la semántica documental formal del sistema.

## 5.4 EstadoDocumento
Define la situación de validez/operación del documento dentro del circuito documental.

## 5.5 CanalNotificacion
Define el medio operativo utilizado para notificar.

## 5.6 EstadoNotificacion
Define la situación operativa de una notificación específica.

## 5.7 OrigenEvento
Define la procedencia funcional del evento, no la tecnología subyacente.

## 5.8 FirmaTipo
Define el tipo de firma o validación formal aplicable al documento.

## 5.9 TipoPresentacion
Clasifica presentaciones o intervenciones incorporadas al expediente.

## 5.10 TipoGestionExterna
Clasifica la naturaleza de la gestión o derivación externa.

## 5.11 ResultadoExterno
Clasifica el resultado devuelto o consolidado desde una gestión externa.

## 5.12 MotivoCierreActa
Clasifica la razón principal de cierre/finalización del caso.

---

# 6. Catálogos y persistencia

Los catálogos globales pueden materializarse en implementación de diferentes maneras según convenga técnicamente:

- enum en código
- tabla de referencia
- enum + tabla de parametrización adicional
- código fijo documentado con metadata externa

La decisión técnica concreta puede variar por catálogo, pero la semántica canónica no debe variar.

---

# 7. Catálogos y contratos

Todo contrato del sistema que exponga, reciba o derive estados o clasificaciones debe utilizar los nombres canónicos definidos por estos catálogos.

No deben inventarse variantes para:
- frontend
- móvil
- integraciones
- exportaciones
- APIs internas

salvo que exista una capa explícita de traducción controlada.

---

# 8. Catálogos y snapshot

El snapshot no debe crear vocabulario nuevo.

Debe derivar su semántica operativa usando los catálogos globales ya definidos.

Si una bandeja necesita una categoría nueva, primero debe evaluarse si:
- ya existe un catálogo adecuado
- alcanza con una combinación de catálogo + flags
- alcanza con una query derivada

Solo en último caso corresponde crear un nuevo catálogo.

---

# 9. Catálogos y queries/bandejas

Las bandejas deben filtrar y priorizar a partir de:

- `EstadoActa`
- `EstadoNotificacion`
- `EstadoDocumento`
- `TipoEventoActa`
- flags derivados del snapshot

Las bandejas no deben apoyarse en texto libre como fuente principal de clasificación.

---

# 10. Catálogos y frontend/móvil

Frontend web y aplicación móvil deben compartir exactamente los mismos valores canónicos.

Pueden cambiar:
- labels visibles
- orden de presentación
- colores
- agrupaciones de UI

pero no debe cambiar el significado ni el valor semántico base del catálogo.

---

# 11. Catálogos y generación asistida por IA

Para permitir implementación estable con IA:

- los catálogos deben estar en documentos separados y claros
- los nombres deben ser definitivos
- los significados deben estar escritos explícitamente
- las reglas de uso deben estar documentadas
- los documentos deben preceder a contratos y código

La IA no debe inferir vocabulario: debe consumirlo desde estos documentos.

---

# 12. Regla de evolución

Un catálogo global solo puede modificarse si:

- la nueva necesidad está justificada
- la semántica anterior no alcanza
- el cambio fue revisado contra snapshot, queries, contratos e implementación
- la decisión se refleja en el canon

No deben agregarse valores de manera oportunista o local sin revisión transversal.

---

# 13. Catálogos que NO deben existir como globales sin necesidad

No deben promoverse a catálogo global, salvo que el sistema realmente lo requiera:

- colores de UI
- prioridades visuales
- mensajes de error
- nombres de tabs
- estados puramente técnicos
- estados temporales de infraestructura
- nombres de jobs internos
- detalles de sincronización local del móvil

Esos conceptos pertenecen a otros niveles.

---

# 14. Orden de definición recomendado

El orden correcto para congelar el vocabulario canónico es:

1. `EstadoActa`
2. `TipoEventoActa`
3. `TipoDocumento`
4. `EstadoDocumento`
5. `CanalNotificacion`
6. `EstadoNotificacion`
7. `OrigenEvento`
8. `FirmaTipo`
9. `TipoPresentacion`
10. `TipoGestionExterna`
11. `ResultadoExterno`
12. `MotivoCierreActa`

---

# 15. Regla final

Si un desarrollador, analista o herramienta de IA necesita decidir “cómo se llama oficialmente esto”, la respuesta debe salir de la carpeta `spec/03-catalogos/`.

Ese directorio es la fuente principal de verdad del vocabulario canónico del sistema.

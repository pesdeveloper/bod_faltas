# PROMPT DE REANUDACIÓN — PROTOTIPO BACKEND FALTAS

Estamos retomando el prototipo backend del sistema de faltas municipal.

## Proyecto

- repo: `backend/api-faltas-prototipo`
- stack: Java 21 + Spring Boot 3.5.x
- sin base de datos
- sin seguridad
- store en memoria
- prototipo descartable en lo técnico, pero funcionalmente fiel al sistema real

## Norte del prototipo

Este prototipo no debe quedar limitado a un subconjunto funcional permanente.

La meta es que evolucione hasta representar de forma **navegable, accionable y coherente** el universo funcional del sistema, con simplificaciones técnicas controladas pero sin romper el modelo conceptual.

### Simplificaciones permitidas
- estado en memoria
- dataset mock
- sin base real
- sin seguridad real
- sin integraciones reales
- acciones simples
- formularios simplificados
- documentos mock o edición documental no completa

### Simplificaciones no permitidas
- happy paths irreales
- atajos conceptuales que contradigan el dominio
- convivencia de circuitos viejos y nuevos si se contradicen
- deformar estados, transiciones, firma, notificación, archivo, gestión externa o reingreso por comodidad técnica

## Regla principal

La demo no debe ser un atajo conceptual.  
Debe ser una base realista del sistema final.

Eso implica:

- estados coherentes con el modelo final
- transiciones representando el flujo real
- un solo circuito verdadero por comportamiento
- estado agregador cuando haya múltiples piezas o documentos
- detalle fino en estructuras específicas

## Estado de continuidad

Para inventario vigente del prototipo, endpoints, acciones, estado actual y próximo paso sugerido, usar:

- `docs-trabajo/estado-actual-y-proximo-paso.md`

Ese archivo es la verdad volátil del prototipo.

## Decisiones cerradas

### Firma
- el circuito viejo `pasar-a-notificacion` fue eliminado
- la única verdad es `firmarDocumento(actaId, documentoId)`
- los documentos tienen identidad propia
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están en `FIRMADO`

### Piezas
- existe la bandeja `PENDIENTES_RESOLUCION_REDACCION`
- el prototipo soporta `piezasRequeridas` y `piezasGeneradas`
- mientras falte al menos una pieza requerida, la acta permanece en esa bandeja
- recién al completar todas las piezas requeridas pasa a `PENDIENTE_FIRMA`

### Estados agregadores
- no usar el primer pendiente como estado
- no usar la última pieza generada como estado de firma
- estados correctos:
  - `PENDIENTE_PRODUCCION_PIEZAS`
  - `PENDIENTE_FIRMA_PIEZAS`
- el detalle fino vive en piezas/documentos, no en el estado del expediente

### Nulidad
- nulidad **no** es bandeja terminal autónoma
- nulidad se trata como **pieza no-fallo**
- vive dentro de `PENDIENTES_RESOLUCION_REDACCION`
- ya existe acción para producir la pieza `NULIDAD`
- el caso demo alineado con spec es `ACTA-0012`
- queda pendiente decidir el destino final post-firma de nulidad si la spec requiere un comportamiento distinto del circuito común

### Notificación
- existe notificación positiva
- existe notificación negativa
- existe reintento por no entrega
- existe notificación vencida
- existe reintento post-vencimiento
- la notificación vencida debe entenderse como resultado de un proceso del sistema que detecta vencimientos
- en el prototipo se la materializa manualmente por API, pero esa acción representa el resultado de ese proceso, no una decisión primaria del operador
- los resultados alternativos de notificación pueden devolver la acta a `PENDIENTE_ANALISIS`
- la separación operativa dentro de la bandeja de análisis se resuelve con `accionPendiente`
- no crear bandejas nuevas por microcaso mientras alcance con macro-bandeja + marca operativa visible y filtrable

### Marcas operativas ya consolidadas
- `REINTENTAR_NOTIFICACION`
- `EVALUAR_NOTIFICACION_VENCIDA`
- `REVISION_POST_REINGRESO`
- `DERIVAR_GESTION_EXTERNA`
- `REVISION_POST_GESTION_EXTERNA`

### Archivo
- `ARCHIVO` sigue siendo macro-bandeja
- ya no es una salida completamente indiferenciada
- existe semántica mínima de archivo mediante `motivoArchivo`
- motivos actualmente modelados:
  - `ARCHIVO_DESDE_ANALISIS_DIRECTO`
  - `ARCHIVO_POST_EVALUACION_VENCIMIENTO`

### Reingreso desde archivo
- existe reingreso explícito desde archivo
- solo aplica a casos archivados con `permiteReingreso = true`
- el reingreso devuelve a `PENDIENTE_ANALISIS`
- deja marca operativa `REVISION_POST_REINGRESO`
- preserva el `motivoArchivo` previo como dato sintético del último archivo del que provino
- el evento de reingreso queda registrado de forma explícita

### Gestión externa
- existe visibilidad previa en análisis de casos listos para gestión externa
- la marca operativa previa es `DERIVAR_GESTION_EXTERNA`
- existe la macro-bandeja `GESTION_EXTERNA`
- tipos mínimos hoy modelados:
  - `APREMIO`
  - `JUZGADO_DE_PAZ`
- existe derivación efectiva a gestión externa
- existe retorno efectivo desde gestión externa a `PENDIENTE_ANALISIS`
- el retorno deja marca `REVISION_POST_GESTION_EXTERNA`
- `tipoGestionExterna` se preserva como trazabilidad sintética
- `permiteReingreso` no se consume artificialmente al volver
- existe re-derivación efectiva desde `REVISION_POST_GESTION_EXTERNA`
- la gestión externa queda abierta mientras el expediente no caiga en una salida terminal o invalidante como cierre o nulidad/causal equivalente definida por la spec

### Cierre
- existe cierre explícito desde análisis
- el cierre lleva a `CERRADAS`
- limpia marcas operativas cuando corresponde
- el evento `CIERRE_ANALISIS` forma parte del circuito consolidado

## Casos demo consolidados

### ACTA-0013
Caso canónico validado para múltiples piezas y firma realista.

Expresa:
- múltiples piezas requeridas
- múltiples documentos
- firma individual por documento
- permanencia en `PENDIENTE_FIRMA` hasta completar todas las firmas
- paso a `PENDIENTE_NOTIFICACION`
- acuse positivo
- retorno a `PENDIENTE_ANALISIS`
- cierre desde análisis

### ACTA-0006
Caso validado para:
- archivo directo desde análisis
- semántica explícita de archivo directo
- reingreso desde archivo
- retorno a `PENDIENTE_ANALISIS`

### ACTA-0004
Caso validado para:
- notificación negativa
- retorno a `PENDIENTE_ANALISIS`
- `accionPendiente = REINTENTAR_NOTIFICACION`
- filtro por `accionPendiente`
- reintento por no entrega
- vuelta a `PENDIENTE_NOTIFICACION`

### ACTA-0005
Caso validado para:
- notificación vencida
- retorno a `PENDIENTE_ANALISIS`
- `accionPendiente = EVALUAR_NOTIFICACION_VENCIDA`
- reintento post-vencimiento
- archivo post evaluación de vencimiento
- filtro específico dentro de la bandeja de análisis

### Gestión externa
Casos demo vigentes:
- caso listo para derivación a gestión externa en análisis
- derivación efectiva a `APREMIO`
- derivación efectiva a `JUZGADO_DE_PAZ`
- caso ya derivado precargado en `GESTION_EXTERNA`
- retorno efectivo desde `GESTION_EXTERNA`
- re-derivación efectiva desde `REVISION_POST_GESTION_EXTERNA`

### Nulidad
Caso demo vigente:
- `ACTA-0012`
- en `PENDIENTES_RESOLUCION_REDACCION`
- con `PENDIENTE_NULIDAD`
- pieza requerida `NULIDAD`
- ya puede producir la pieza `NULIDAD` como documento pendiente de firma

## Criterio de diseño

Mantener:

- store simple
- controllers directos
- records y enums simples
- sin framework genérico
- sin DB
- sin sobrearquitectura

Pero priorizando siempre:

- coherencia con el modelo final
- un solo circuito verdadero
- estados agregadores correctos
- detalle fino en estructuras específicas

## Regla de trabajo para slices

Cada slice debe definir explícitamente:

1. objetivo puntual
2. fuente de verdad
3. alcance técnico
4. exclusiones
5. caso demo
6. criterio de cierre

### Reglas obligatorias
- no reintroducir circuitos viejos
- no dejar convivir dos modelos contradictorios
- priorizar precisión quirúrgica
- no tocar más archivos de los necesarios
- si una simplificación rompe el modelo final, corregirla ahora

### Fuente de verdad
- usar `spec/` como fuente principal
- usar solo la parte mínima necesaria
- no usar archivos de continuidad como fuente de verdad de dominio
- usar `prompt-de-reanudacion-chat.md` como marco metodológico estable
- usar `estado-actual-y-proximo-paso.md` como inventario volátil del prototipo
- usar `.cursor/rules/contexto-minimo.mdc` como fuente normativa de manejo de contexto
- usar `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` para proteger continuidad en slices funcionales

### Alcance técnico
- tocar solo controller, store, mocks, dtos, enums, supports y helpers necesarios del prototipo
- no arrastrar refactors fuera del circuito puntual

## Regla de contexto para Cursor

La fuente normativa de manejo de contexto es:

- `.cursor/rules/contexto-minimo.mdc`

Y la protección de continuidad está en:

- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`

Resumen operativo mínimo:

- cargar primero lo mínimo indispensable
- no leer continuidad completa por defecto en slices funcionales
- no editar continuidad salvo autorización explícita
- cargar solo la parte mínima necesaria de `spec/`
- cargar solo los archivos del prototipo implicados en el slice

## Regla para refactors

Ante un problema, distinguir:

### A. Error de implementación puntual
Acción:
- corregir código
- no necesariamente tocar la spec

### B. Error de interpretación del modelo
Acción:
- corregir código
- y además fortalecer la fuente correcta

### Regla de aprendizaje persistente
Si una corrección importante revela ambigüedad o falta de precisión en la fuente, la lección no debe quedar solo en el código.

Debe subirse a:
- `spec/01-dominio/...` si era dominio
- `spec/02-reglas-transversales/...` si era regla transversal
- `spec/03-bandejas/...` si era flujo/bandeja/transición
- `spec/04-backend/...` si era implementación futura
- `prompt-de-reanudacion-chat.md` si era método de trabajo estable
- `estado-actual-y-proximo-paso.md` si era inventario vigente del prototipo
- `.cursor/rules/contexto-minimo.mdc` si era disciplina de manejo de contexto
- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` si era protección de continuidad

## Regla de continuidad mínima

Todo nuevo slice debe poder plantearse usando como base:

- `prompt-de-reanudacion-chat.md`
- `estado-actual-y-proximo-paso.md`
- `.cursor/rules/contexto-minimo.mdc`
- `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc`
- un subconjunto pequeño y explícito de `spec/`

Si para arrancar hace falta abrir demasiada historia o demasiados archivos, el slice está mal recortado o el estado actual no está suficientemente consolidado.

## Refactor táctico del prototipo

Ya se hizo la descompresión principal por área funcional:

- `ArchivoReingresoSupport`
- `NotificacionSupport`
- `PiezasFirmaSupport`
- `GestionExternaSupport`
- `CierreSupport`

`PrototipoStore` quedó como fachada pública sobre supports del dominio del prototipo.

### Constantes compartidas
- existe `PrototipoConstantes` para constantes mínimas de frontera entre áreas

## Higiene técnica reciente

- se limpió el working tree local de artefactos regenerables bajo `target/`
- queda pendiente, si se decide, un micro-slice técnico para:
  - ignorar `target/` en `.gitignore`
  - sacar `target/` del índice si corresponde

## Estado actual del próximo paso

El próximo slice funcional todavía no está elegido.

Opciones candidatas razonables:
1. decidir el comportamiento post-firma de nulidad si la spec exige desvío del circuito común
2. ampliar otras decisiones posteriores desde análisis que sigan faltando
3. ampliar inicio / enriquecimiento
4. micro-slice documental de actualización de continuidad y estado si hiciera falta
5. micro-slice técnico de higiene de repo (`.gitignore` + `target/`)

## Instrucción final

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
No abrir contexto de más.  
No dejar que Cursor toque continuidad sin autorización explícita.  
Separar por área funcional cuando haga falta, no por bandeja.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora.

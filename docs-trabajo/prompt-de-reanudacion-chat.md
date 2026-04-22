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
- deformar estados, transiciones, firma o notificación por comodidad técnica

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

Ese archivo es la **verdad volátil** del prototipo.

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

### Notificación
- existe notificación positiva
- existe notificación negativa
- existe reintento de notificación
- existe notificación vencida
- los resultados alternativos de notificación pueden devolver la acta a `PENDIENTE_ANALISIS`
- la separación operativa dentro de la bandeja de análisis se resuelve con `accionPendiente`
- no crear bandejas nuevas por microcaso mientras alcance con macro-bandeja + marca operativa visible y filtrable

### Marcas operativas ya consolidadas
- `REINTENTAR_NOTIFICACION`
- `EVALUAR_NOTIFICACION_VENCIDA`

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
Caso corto validado para:

- archivo directo desde `PENDIENTE_ANALISIS`
- conservación de posibilidad de reingreso
- evento explícito de archivado desde análisis

### ACTA-0004
Caso validado para:

- notificación negativa
- retorno a `PENDIENTE_ANALISIS`
- `accionPendiente = REINTENTAR_NOTIFICACION`
- filtro por `accionPendiente`
- reintento de notificación
- vuelta a `PENDIENTE_NOTIFICACION`

### ACTA-0005
Caso validado para:

- notificación vencida
- retorno a `PENDIENTE_ANALISIS`
- `accionPendiente = EVALUAR_NOTIFICACION_VENCIDA`
- filtro específico dentro de la bandeja de análisis

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
- usar `prompt-de-reanudacion-chat.md` como marco metodológico estable
- usar `estado-actual-y-proximo-paso.md` como verdad volátil del prototipo
- usar `.cursor/rules/contexto-minimo.mdc` como fuente normativa de manejo de contexto
- usar `.cursor/rules/continuidad-solo-bajo-autorizacion.mdc` para proteger continuidad en slices funcionales

### Alcance técnico
- tocar solo controller, store, mocks, dtos, enums y helpers necesarios del prototipo
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
Ejemplos:
- tocó un archivo de más
- nombró algo mal
- validó mal una condición menor

Acción:
- corregir código
- no necesariamente tocar la spec

### B. Error de interpretación del modelo
Ejemplos:
- mezcló circuitos
- reintrodujo circuito viejo
- confundió estado agregador con detalle fino
- simplificó mal una transición
- la fuente permitía entender mal la decisión

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
- `estado-actual-y-proximo-paso.md` si era estado actual o inventario vigente del prototipo
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

## Estado actual del próximo paso

El próximo slice funcional **todavía no está elegido**.

Opciones candidatas actuales:

1. decisión posterior sobre notificación vencida
2. reintento específico para casos vencidos
3. más piezas no-fallo
4. otros circuitos de análisis y decisiones
5. otro caso demo fuerte

## Instrucción final

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
No abrir contexto de más.  
No dejar que Cursor toque continuidad sin autorización explícita.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora.
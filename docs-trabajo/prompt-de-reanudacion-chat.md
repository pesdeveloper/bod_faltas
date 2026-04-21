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

La meta es que evolucione hasta representar de forma **navegable, accionable y coherente** el universo funcional del sistema, con simplificaciones técnicas controladas pero sin romper el modelo conceptual. :contentReference[oaicite:0]{index=0}

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
Debe ser una base realista del sistema final. :contentReference[oaicite:1]{index=1}

Eso implica:

- estados coherentes con el modelo final
- transiciones representando el flujo real
- un solo circuito verdadero por comportamiento
- estado agregador cuando haya múltiples piezas o documentos
- detalle fino en estructuras específicas

## Estado funcional consolidado

### Lectura disponible
- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones` :contentReference[oaicite:2]{index=2}

### Acciones implementadas
- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-positiva`
- `POST /api/prototipo/actas/{id}/acciones/cerrar-acta`
- `POST /api/prototipo/actas/{id}/acciones/archivar-acta`
- `POST /api/prototipo/actas/{id}/acciones/generar-medida-preventiva`
- `POST /api/prototipo/actas/{id}/acciones/generar-notificacion-acta`
- `POST /api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}` :contentReference[oaicite:3]{index=3}

## Decisiones cerradas

### Firma
- el circuito viejo `pasar-a-notificacion` fue eliminado
- la única verdad es `firmarDocumento(actaId, documentoId)`
- los documentos tienen identidad propia
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están en `FIRMADO` :contentReference[oaicite:4]{index=4} :contentReference[oaicite:5]{index=5}

### Piezas
- existe la bandeja `PENDIENTES_RESOLUCION_REDACCION`
- el prototipo soporta `piezasRequeridas` y `piezasGeneradas`
- mientras falte al menos una pieza requerida, la acta permanece en esa bandeja
- recién al completar todas las piezas requeridas pasa a `PENDIENTE_FIRMA` :contentReference[oaicite:6]{index=6}

### Estados agregadores
- no usar el primer pendiente como estado
- no usar la última pieza generada como estado de firma
- estados correctos:
  - `PENDIENTE_PRODUCCION_PIEZAS`
  - `PENDIENTE_FIRMA_PIEZAS`
- el detalle fino vive en piezas/documentos, no en el estado del expediente :contentReference[oaicite:7]{index=7}

## Casos demo clave

### ACTA-0013
Caso canónico para validar múltiples piezas y firma realista.

- inicia en `PENDIENTES_RESOLUCION_REDACCION`
- requiere:
  - `NOTIFICACION_ACTA`
  - `MEDIDA_PREVENTIVA`

Flujo esperado:
1. generar medida preventiva
2. generar notificación del acta
3. pasar a `PENDIENTE_FIRMA`
4. firmar un documento
5. seguir en `PENDIENTE_FIRMA`
6. firmar el restante
7. pasar a `PENDIENTE_NOTIFICACION` :contentReference[oaicite:8]{index=8}

### ACTA-0003
- quedó alineada al nuevo modelo
- su documento principal nace en `PENDIENTE_FIRMA`
- debe navegarse con `firmar-documento/{documentoId}` :contentReference[oaicite:9]{index=9}

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
- detalle fino en estructuras específicas :contentReference[oaicite:10]{index=10}

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
- usar `prompt-de-reanudacion-chat.md` y `estado-actual-y-proximo-paso.md` como marco de continuidad, no como reemplazo de la spec

### Alcance técnico
- tocar solo controller, store, mocks, dtos, enums y helpers necesarios del prototipo
- no arrastrar refactors fuera del circuito puntual

## Regla de contexto para Cursor

El contexto activo debe mantenerse chico.

- cargar primero `prompt-de-reanudacion-chat.md`
- cargar luego `estado-actual-y-proximo-paso.md`
- cargar después solo la parte mínima necesaria de `spec/`
- cargar solo los archivos del prototipo implicados en el slice
- no cargar por defecto historial, notas, artefactos de trabajo ni módulos no relacionados

El objetivo no es que Cursor vea todo el proyecto, sino solo lo necesario para resolver bien el slice actual.

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
- `prompt-de-reanudacion-chat.md` o `estado-actual-y-proximo-paso.md` si era método de trabajo o continuidad

## Regla de continuidad mínima

Todo nuevo slice debe poder plantearse usando como base:

- `prompt-de-reanudacion-chat.md`
- `estado-actual-y-proximo-paso.md`
- un subconjunto pequeño y explícito de `spec/`

Si para arrancar hace falta abrir demasiada historia o demasiados archivos, el slice está mal recortado o el estado actual no está suficientemente consolidado.

## Instrucción final

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir modelos contradictorios.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora. :contentReference[oaicite:11]{index=11}
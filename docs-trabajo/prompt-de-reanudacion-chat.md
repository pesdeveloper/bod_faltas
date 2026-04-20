Estamos retomando el prototipo backend del sistema de faltas municipal.

## Proyecto

- repo: `backend/api-faltas-prototipo`
- stack: Java 21 + Spring Boot 3.5.x
- sin base de datos
- sin seguridad
- store en memoria
- enfoque: prototipo descartable, pero debe reflejar fielmente el modelo objetivo final
- simplificación permitida: no modelar formularios complejos ni edición real de documentos
- simplificación NO permitida: romper la lógica del flujo real del sistema

## Regla principal de trabajo

La demo no debe ser un atajo conceptual.
Debe ser una base realista del sistema final.

Eso implica:

- los estados deben ser coherentes con el modelo final
- las transiciones deben representar el flujo real
- no dejar circuitos viejos simplificados conviviendo si contradicen el modelo correcto
- cuando haya múltiples piezas o documentos, el estado del expediente debe ser agregador
- el detalle fino debe vivir en estructuras específicas (documentos, piezas, etc.)

## Estado funcional actual

El prototipo ya implementa lectura completa:

- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones`

También implementa acciones operativas ya funcionales:

- `POST /api/prototipo/actas/{id}/acciones/registrar-notificacion-positiva`
- `POST /api/prototipo/actas/{id}/acciones/cerrar-acta`
- `POST /api/prototipo/actas/{id}/acciones/archivar-acta`
- `POST /api/prototipo/actas/{id}/acciones/generar-medida-preventiva`
- `POST /api/prototipo/actas/{id}/acciones/generar-notificacion-acta`
- `POST /api/prototipo/actas/{id}/acciones/firmar-documento/{documentoId}`

Importante:
el endpoint viejo `pasar-a-notificacion` fue eliminado para evitar convivencia de dos modelos de firma distintos.

## Decisiones importantes ya consolidadas

### 1. Bandeja nueva incorporada
Existe la bandeja:

- `PENDIENTES_RESOLUCION_REDACCION`

con casos mock de:
- resolución pendiente
- nulidad pendiente
- medida preventiva pendiente
- rectificación pendiente

### 2. Múltiples piezas requeridas
El prototipo soporta actas con múltiples piezas no-fallo requeridas.

Se agregó estructura mock explícita:

- `piezasRequeridas`
- `piezasGeneradas`

La regla correcta ya consolidada es:

- mientras falte al menos una pieza requerida → la acta permanece en `PENDIENTES_RESOLUCION_REDACCION`
- recién cuando todas las piezas requeridas fueron producidas → la acta pasa a `PENDIENTE_FIRMA`

### 3. Estados agregadores correctos
Se corrigieron dos malas simplificaciones:

- NO usar el primer pendiente como estado (`PENDIENTE_` + primera pieza)
- NO usar estado de firma dependiente de la última pieza generada

Estados correctos ya adoptados:

- `PENDIENTE_PRODUCCION_PIEZAS`
- `PENDIENTE_FIRMA_PIEZAS`

Detalle fino:
- qué piezas faltan → en `piezasRequeridas` / `piezasGeneradas`
- qué documentos faltan firmar → en documentos mock

### 4. Firma correcta por documento
La firma ya no se modela por “pieza” ni por firma global.

Regla correcta ya implementada:

- los documentos tienen identidad propia (`documentoId`)
- se firman de a uno
- los documentos generados por acciones nuevas nacen en estado documental `PENDIENTE_FIRMA`
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están en `FIRMADO`

### 5. Eliminación del circuito viejo
Se eliminó el circuito viejo basado en `pasarActaANotificacion(...)`.

Ahora hay una sola verdad para el flujo de firma:

- `firmarDocumento(actaId, documentoId)`

## Caso demo más importante

### ACTA-0013
Es el caso canónico de demo para múltiples piezas y firma realista.

Tiene:

- bandeja inicial: `PENDIENTES_RESOLUCION_REDACCION`
- piezas requeridas:
  - `NOTIFICACION_ACTA`
  - `MEDIDA_PREVENTIVA`

Flujo esperado:

1. generar medida preventiva
2. generar notificación del acta
3. la acta pasa a `PENDIENTE_FIRMA`
4. firmar un documento
5. la acta sigue en `PENDIENTE_FIRMA`
6. firmar el documento restante
7. la acta pasa a `PENDIENTE_NOTIFICACION`

Este caso debe seguir siendo el principal para validar que:
- múltiples piezas funcionan
- múltiples documentos funcionan
- la firma individual funciona
- la salida de bandeja depende de completitud real

## Acta demo antigua corregida

### ACTA-0003
Fue alineada al modelo nuevo:

- `DOC-0003-01` ahora nace como `PENDIENTE_FIRMA`
- ya no usa el circuito viejo de “pasar a notificación”
- debe navegarse con `firmar-documento/{documentoId}`

## Criterio de diseño vigente

Seguir manteniendo:

- store simple
- controllers directos
- records / enums simples
- sin framework genérico
- sin DB
- sin sobrearquitectura

Pero siempre priorizando:

- coherencia con el modelo final
- un solo circuito verdadero por comportamiento
- estados agregadores bien definidos
- detalle fino en estructuras específicas

## Qué revisar al retomar

Antes de seguir sumando slices, revisar si el siguiente paso requiere:

- nuevo tipo de pieza no-fallo
- nuevas notificaciones
- reintentos
- firma de otros documentos
- o cierre de otro circuito de la demo

También revisar si hay que limpiar el contexto de Cursor:
- archivos irrelevantes del repo
- docs que no aporten al bloque actual
- artefactos de trabajo que puedan excluirse para bajar consumo de contexto

## Instrucción de trabajo

Actuá como arquitecto del prototipo.
No improvises.
No dejes convivir dos modelos contradictorios.
Si aparece una simplificación que rompe el modelo final, corregirla ahora.
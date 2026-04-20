# ESTADO ACTUAL — PROTOTIPO FALTAS

## Nivel de avance

El prototipo backend está funcional y ya dejó de ser solo un happy path simple.

Ahora modela de forma mucho más fiel:

- producción de múltiples piezas no-fallo
- permanencia en bandeja hasta completar piezas requeridas
- firma individual por documento
- salida de bandeja según completitud real de documentos firmables

## Lectura disponible

Endpoints de lectura vigentes:

- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones`

## Acciones implementadas

### Flujo general existente
- `registrar-notificacion-positiva`
- `cerrar-acta`
- `archivar-acta`

### Nuevas acciones implementadas
- `generar-medida-preventiva`
- `generar-notificacion-acta`
- `firmar-documento/{documentoId}`

## Refactors ya cerrados

### 1. Nueva bandeja
Se incorporó:

- `PENDIENTES_RESOLUCION_REDACCION`

### 2. Soporte de múltiples piezas
Se agregó soporte explícito para:

- `piezasRequeridas`
- `piezasGeneradas`

Regla consolidada:
- si faltan piezas → permanece en `PENDIENTES_RESOLUCION_REDACCION`
- si ya están todas producidas → pasa a `PENDIENTE_FIRMA`

### 3. Estados corregidos
Se adoptaron estados agregadores correctos:

- `PENDIENTE_PRODUCCION_PIEZAS`
- `PENDIENTE_FIRMA_PIEZAS`

Se descartaron decisiones incorrectas como:
- depender de la primera pieza pendiente
- depender de la última pieza generada

### 4. Firma correcta
Se consolidó la firma por documento:

- los documentos tienen `documentoId`
- los documentos firmables nacen en `PENDIENTE_FIRMA`
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los documentos firmables están en `FIRMADO`

### 5. Circuito viejo eliminado
Se eliminó el viejo `pasarActaANotificacion`.

Ahora existe un único modelo de firma / transición a notificación.

## Caso demo principal

### ACTA-0013
Caso canónico actual para demostrar el sistema.

Tiene:
- piezas requeridas:
  - `NOTIFICACION_ACTA`
  - `MEDIDA_PREVENTIVA`

Recorrido esperado:
1. generar medida preventiva
2. generar notificación del acta
3. pasar a `PENDIENTE_FIRMA`
4. firmar primer documento
5. seguir en `PENDIENTE_FIRMA`
6. firmar segundo documento
7. pasar a `PENDIENTE_NOTIFICACION`

Este caso ya expresa:
- múltiples piezas
- múltiples documentos
- firma individual
- transición realista

### ACTA-0003
Fue alineada al nuevo circuito:
- documento principal en `PENDIENTE_FIRMA`
- firma por `documentoId`
- transición a notificación por el único circuito vigente

## Qué falta

Para que el simulador se acerque más al sistema integral todavía faltan, entre otras cosas:

- ampliar más piezas no-fallo
- ampliar más caminos de notificación
- modelar resultados de notificación alternativos
- reintentos
- más circuitos de análisis y decisiones
- más cobertura de bandejas del universo completo

## Próximo paso sugerido

Antes de seguir agregando slices funcionales, conviene hacer una pequeña pausa técnica para:

### 1. revisar consistencia global
- confirmar que no quedó ningún circuito viejo conviviendo
- revisar comentarios / nombres / estados que hayan quedado desactualizados

### 2. revisar contexto de Cursor
- identificar archivos del repo que no sean necesarios para el bloque actual
- reducir ruido contextual
- ver si hay documentos o artefactos que convenga excluir del contexto activo

### 3. decidir siguiente bloque funcional
Elegir cuál de estos seguir:
- más piezas no-fallo
- notificación negativa / vencida / reintentos
- otros circuitos de firma
- otro caso demo fuerte

## Criterio de continuidad

Seguir construyendo el prototipo como:
- base realista del sistema final
- sin formularios complejos
- sin edición rica
- con acciones disparadoras simples
- pero con lógica y transiciones fieles al modelo real
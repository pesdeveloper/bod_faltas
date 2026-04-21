# ESTADO ACTUAL — PROTOTIPO FALTAS

## Nivel de avance

El prototipo backend ya no es solo un happy path simple.

Hoy modela de forma mucho más fiel:

- producción de múltiples piezas no-fallo
- permanencia en bandeja hasta completar piezas requeridas
- firma individual por documento
- salida de bandeja según completitud real de documentos firmables :contentReference[oaicite:12]{index=12}

## Objetivo vigente

El prototipo no debe quedar limitado a un subconjunto funcional permanente.

Debe evolucionar hasta representar de forma **navegable, accionable y coherente** el universo funcional del sistema, con simplificaciones técnicas controladas pero sin romper el modelo conceptual real. :contentReference[oaicite:13]{index=13}

## Lectura disponible

- `GET /api/prototipo/health`
- `GET /api/prototipo/bandejas`
- `GET /api/prototipo/bandejas/{codigo}/actas`
- `GET /api/prototipo/actas/{id}`
- `GET /api/prototipo/actas/{id}/eventos`
- `GET /api/prototipo/actas/{id}/documentos`
- `GET /api/prototipo/actas/{id}/notificaciones` :contentReference[oaicite:14]{index=14}

## Acciones implementadas

- `registrar-notificacion-positiva`
- `cerrar-acta`
- `archivar-acta`
- `generar-medida-preventiva`
- `generar-notificacion-acta`
- `firmar-documento/{documentoId}` :contentReference[oaicite:15]{index=15}

## Refactors ya cerrados

### Bandeja nueva
- `PENDIENTES_RESOLUCION_REDACCION` :contentReference[oaicite:16]{index=16}

### Múltiples piezas
- existen `piezasRequeridas` y `piezasGeneradas`
- si faltan piezas, permanece en `PENDIENTES_RESOLUCION_REDACCION`
- si todas están producidas, pasa a `PENDIENTE_FIRMA` :contentReference[oaicite:17]{index=17}

### Estados corregidos
- `PENDIENTE_PRODUCCION_PIEZAS`
- `PENDIENTE_FIRMA_PIEZAS`
- ya no depende de la primera pieza pendiente ni de la última generada :contentReference[oaicite:18]{index=18}

### Firma correcta
- documentos con `documentoId`
- nacen en `PENDIENTE_FIRMA`
- se firman de a uno
- la acta solo sale de `PENDIENTE_FIRMA` cuando todos los firmables están en `FIRMADO` :contentReference[oaicite:19]{index=19}

### Circuito viejo eliminado
- se eliminó `pasarActaANotificacion`
- existe un único modelo de firma / transición a notificación :contentReference[oaicite:20]{index=20}

## Casos demo principales

### ACTA-0013
Caso canónico actual.

Requiere:
- `NOTIFICACION_ACTA`
- `MEDIDA_PREVENTIVA`

Recorrido esperado:
1. generar medida preventiva
2. generar notificación del acta
3. pasar a `PENDIENTE_FIRMA`
4. firmar primer documento
5. seguir en `PENDIENTE_FIRMA`
6. firmar segundo documento
7. pasar a `PENDIENTE_NOTIFICACION` :contentReference[oaicite:21]{index=21}

Expresa:
- múltiples piezas
- múltiples documentos
- firma individual
- transición realista :contentReference[oaicite:22]{index=22}

### ACTA-0003
- documento principal en `PENDIENTE_FIRMA`
- firma por `documentoId`
- transición por el único circuito vigente :contentReference[oaicite:23]{index=23}

## Criterio de diseño vigente

Seguir construyendo el prototipo como:

- base realista del sistema final
- sin formularios complejos
- sin edición rica
- con acciones disparadoras simples
- pero con lógica y transiciones fieles al modelo real :contentReference[oaicite:24]{index=24}

Mantener además:

- store simple
- controllers directos
- records y enums simples
- sin framework genérico
- sin sobrearquitectura
- un solo circuito verdadero
- estados agregadores correctos
- detalle fino en estructuras específicas :contentReference[oaicite:25]{index=25}

## Método operativo para continuar

Cada próximo paso debe abrirse como slice mínimo funcional.

### Antes de abrir un slice
Revisar en este orden:

1. si quedó algún circuito viejo conviviendo
2. si quedaron nombres, comentarios o estados desalineados
3. si el siguiente paso es una ampliación natural del circuito actual
4. si existe un caso demo claro para validar el cambio

### Cada slice debe incluir
- objetivo
- fuente de verdad
- alcance técnico
- exclusiones
- caso demo
- criterio de cierre

### Regla para la fuente de verdad
No abrir toda la spec por defecto.

Para cada slice, recortar la fuente de verdad a un subconjunto chico y explícito de `spec/`.

### Regla para el alcance técnico
Tocar solo lo mínimo necesario del prototipo:

- controller
- store o servicio principal
- mocks / datasets
- dtos / records
- enums / helpers afectados

Evitar tocar:
- módulos backend productivos
- persistencia real
- workers
- frontend
- zonas no relacionadas del repo

### Regla de prioridad
Priorizar siempre:

1. cerrar consistencia del flujo ya construido
2. evitar convivencia de modelos contradictorios
3. recién después sumar alcance funcional

## Regla para refactors

Si aparece un problema, distinguir:

### A. Error de implementación puntual
- corregir código
- no necesariamente actualizar la spec

### B. Error de interpretación del modelo
- corregir código
- y además corregir la spec o el contexto estable correspondiente

## Regla de continuidad mínima

Todo nuevo slice debe poder redactarse usando solo:

- `prompt-de-reanudacion-chat.md`
- `estado-actual-y-proximo-paso.md`
- un subconjunto pequeño y explícito de `spec/`

Si no alcanza con eso:
- o el slice está demasiado grande
- o el estado actual no está suficientemente consolidado

## Qué falta

Todavía falta, entre otras cosas:

- ampliar más piezas no-fallo
- ampliar más caminos de notificación
- modelar resultados alternativos de notificación
- reintentos
- más circuitos de análisis y decisiones
- más cobertura de bandejas del universo completo :contentReference[oaicite:26]{index=26}

## Próximo paso sugerido

Antes de seguir agregando funcionalidad, conviene:

1. revisar consistencia global
   - confirmar que no quedó ningún circuito viejo
   - revisar nombres, comentarios y estados desactualizados

2. revisar contexto de Cursor
   - identificar archivos no necesarios
   - reducir ruido contextual
   - excluir artefactos y módulos irrelevantes

3. elegir un único bloque funcional para continuar
   - más piezas no-fallo
   - notificación negativa / vencida / reintentos
   - otros circuitos de firma
   - otro caso demo fuerte :contentReference[oaicite:27]{index=27}

## Instrucción final al retomar

Actuar como arquitecto del prototipo.

No improvisar.  
No dejar convivir dos modelos contradictorios.  
Si aparece una simplificación que rompe el modelo final, corregirla ahora. :contentReference[oaicite:28]{index=28}
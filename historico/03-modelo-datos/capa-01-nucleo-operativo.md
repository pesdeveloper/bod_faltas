# [CAPA 01] NÚCLEO OPERATIVO

## Finalidad de la capa

Esta capa define el núcleo transaccional del sistema de faltas.

Debe resolver:

- existencia de la **ACTA** como entidad principal
- trazabilidad completa mediante **ACTA_EVENTO**
- soporte para flujo no lineal D1–D8
- soporte para operación offline/online
- identidad técnica y administrativa del acta
- lectura rápida del estado actual sin perder historial
- almacenamiento del domicilio operativo inicial y sucesivos domicilios vinculados al acta

Esta capa es la base del resto del sistema.

---

## Principios de diseño

### 1. ACTA es la entidad central

No existe `IdCausa` como entidad principal del modelo.

Todo el sistema gira alrededor de `ACTA`.

---

### 2. El historial vive en ACTA_EVENTO

Los cambios y transiciones relevantes quedan registrados como eventos append-only.

`ACTA_EVENTO` no se reescribe ni se reutiliza.

---

### 3. El sistema debe poder leer rápido el estado actual

Aunque el historial sea la fuente de verdad, la tabla `ACTA` mantiene snapshots operativos para:

- bandejas
- búsquedas
- filtros
- dashboards
- validaciones de precondición
- integración con capas posteriores

---

### 4. No se reabre: se reingresa por evento

Si una causa vuelve desde una derivación, gestión externa, juzgado o instancia posterior, no se “reabre” técnicamente el registro anterior.

Se registra un **nuevo evento** que reingresa la acta al circuito operativo correspondiente.

---

### 5. Offline primero, consistencia después

La capa debe soportar creación offline.

Por ello la acta puede nacer con identidad técnica (`IdTecnico`) antes de recibir identidad administrativa definitiva.

---

### 6. Separación entre hecho, documento y notificación

Esta capa modela el hecho operativo base.

No modela todavía:

- documentos formales
- firma
- lotes de notificación
- acuses de notificación
- estructura económica completa

Esos aspectos viven en capas posteriores.

---

### 7. Snapshots controlados

Se admite denormalización controlada para mejorar operación diaria, siempre que:

- el historial siga viviendo en eventos
- el snapshot sea derivable o justificable
- no se rompa la trazabilidad jurídica/administrativa

---

### 8. Catálogos cerrados

Los campos de tipo, estado, canal, subtipo o clasificación operativa deben resolverse con:

- enums de aplicación
- constantes de sistema
- tablas catálogas cerradas cuando corresponda

No usar texto libre para estados.

---

## Alcance de la capa

Esta capa incluye:

- `Acta`
- `ActaEvento`
- `ActaDomicilio`
- `ActaEvidencia`
- `ActaObservacion`
- `ActaTransito`
- `ActaContravencion`
- `ActaContravencionMedida`
- `Inspectores`
- `InspectoresSnapshot`

Y deja prevista una futura relación:

- `CmteXActa`

---

# 1) Entidad principal: Acta

## Objeto

Representa la unidad central del sistema.

Es el registro base sobre el cual se proyectan:

- flujo
- eventos
- documentos
- notificaciones
- pagos
- decisiones administrativas
- derivaciones
- cierre

---

## Responsabilidades de Acta

`Acta` debe concentrar:

- identidad persistente del caso
- identidad técnica offline
- identidad administrativa visible
- datos estructurales mínimos del hecho
- referencia al inspector y su snapshot
- ubicación del hecho
- clasificación general
- estado operativo actual
- snapshots proyectados desde otras capas
- referencia al domicilio principal del acta

---

## Regla de domicilio

El domicilio ya **no** vive embebido en `Acta`.

La acta debe vincularse a uno o más domicilios mediante `ActaDomicilio`.

Al crearse el acta:

- debe generarse al menos un `ActaDomicilio`
- ese domicilio inicial debe quedar marcado como:
  - `EsDomicilioActa = 1`
  - y normalmente también `EsPrincipal = 1`
- `Acta` mantiene referencia a ese domicilio principal mediante:
  - `IdActaDomicilioPrincipal`

Esto permite:

- evitar repetir campos domiciliarios dentro de `Acta`
- reutilizar la misma estructura para notificación
- mantener trazabilidad del domicilio inicial
- admitir posteriores domicilios operativos asociados al expediente

---

## Contenido esperado de Acta

`Acta` contiene:

- identidad
- clasificación
- datos del hecho
- geolocalización
- inspector actual/snapshot
- snapshot del flujo actual
- snapshot documental
- snapshot de notificación
- snapshot económico/pago
- referencia al domicilio principal

No contiene:

- firmas
- detalle documental
- acuses de notificación
- resultados de envío
- múltiples domicilios embebidos
- evidencias en columnas repetidas

---

# 2) Entidad de historial: ActaEvento

## Objeto

Registra cada hecho operativo o administrativo relevante de la vida del acta.

---

## Reglas

- append-only
- no se actualiza para “corregir historia”
- si una carga fue errónea, se corrige con un nuevo evento
- puede referenciar documento, detalle de acta u otra entidad según corresponda
- no requiere `DatosJson`
- el origen del evento debe quedar clasificado

---

## Origen del evento

`OrigenEvento` debe resolverse con enum/catálogo cerrado.

Valores previstos:

- `MANUAL`
- `SISTEMA`
- `PROCESO`
- `INTEGRACION`

---

## Finalidad

Permite:

- trazabilidad
- reconstrucción temporal
- auditoría
- reingreso por eventos
- correlación con acciones administrativas o automáticas

---

# 3) Entidad de domicilio: ActaDomicilio

## Objeto

Representa un domicilio vinculado a la acta.

Nace desde el inicio del ciclo de vida del acta y forma parte del núcleo operativo.

---

## Motivo de diseño

Se extrae de `Acta` el domicilio que antes estaba embebido en campos como:

- `CallePersonaTexto`
- `NumeroPuertaPersona`
- `IdProvinciaPersona`
- `IdPartidoPersona`
- `IdLocalidadPersona`

Esto mejora:

- normalización
- reutilización
- extensibilidad
- consistencia con la Capa 03 de notificación

---

## Reglas

- toda acta debe tener al menos un domicilio asociado
- el domicilio inicial debe quedar marcado con `EsDomicilioActa = 1`
- uno de los domicilios activos debe poder actuar como principal
- `Acta.IdActaDomicilioPrincipal` debe apuntar al domicilio principal vigente
- un domicilio puede existir aunque algunos componentes estructurados no estén completos
- `DomicilioTexto` permite almacenar una versión consolidada o capturada del domicilio
- `Email` y `Telefono` se consideran datos de contacto asociados a ese domicilio dentro del contexto del acta

---

## Uso operativo

`ActaDomicilio` sirve para:

- domicilio informado al labrar el acta
- domicilio principal operativo
- domicilios alternativos vinculados al caso
- proyección futura a notificación
- trazabilidad del domicilio usado en distintas etapas

---

# 4) Entidad de evidencia: ActaEvidencia

## Objeto

Representa evidencias asociadas al acta.

---

## Reglas

- las evidencias pertenecen al acta
- no pertenecen al documento formal
- no duplican latitud/longitud por cada archivo
- la geolocalización del hecho vive en `Acta`
- cada evidencia registra su propia metadata técnica mínima

---

## Ejemplos de evidencia

- fotografía
- video
- audio
- imagen escaneada
- adjunto operativo

---

# 5) Entidad de observación: ActaObservacion

## Objeto

Permite registrar observaciones operativas o administrativas vinculadas a la acta.

---

## Reglas

- las observaciones del acta viven en tabla propia
- no deben mezclarse con observaciones documentales
- no deben reemplazar eventos cuando lo que ocurrió es jurídicamente relevante
- pueden convivir con eventos, pero cumplen otro propósito

---

# 6) Subtipo operativo: ActaTransito

## Objeto

Contiene la extensión de datos específicos de actas del dominio tránsito.

---

## Reglas

- relación 1:1 con `Acta`
- sólo existe si la acta pertenece a tránsito
- no modela medidas preventivas formales con resolución
- pueden existir banderas operativas específicas de tránsito según el caso

---

# 7) Subtipo operativo: ActaContravencion

## Objeto

Contiene la extensión de datos específicos de contravenciones no tránsito.

---

## Reglas

- relación 1:1 con `Acta`
- sólo existe si la acta corresponde al dominio contravencional
- habilita modelado de medidas, disposiciones y particularidades del caso

---

# 8) Medidas de contravención: ActaContravencionMedida

## Objeto

Registra medidas asociadas a una acta contravencional.

---

## Reglas

- una `ActaContravencion` puede tener múltiples medidas
- la medida puede requerir seguimiento posterior
- la medida no reemplaza al documento resolutivo cuando éste exista
- su existencia no implica por sí sola cierre del trámite

---

# 9) Catálogo vivo: Inspectores

## Objeto

Representa el catálogo vivo de inspectores habilitados.

---

## Reglas

- se usa para selección operativa actual
- puede cambiar con el tiempo
- no debe usarse como única fuente histórica del inspector actuante

---

# 10) Snapshot histórico: InspectoresSnapshot

## Objeto

Guarda la versión histórica del inspector utilizada en el momento del labrado o asignación relevante.

---

## Regla cerrada

La acta guarda:

- `IdInspector`
- `VersionInspector`

Eso implica:

- selección desde catálogo vivo
- persistencia de referencia histórica versionada

---

## Finalidad

Permite preservar:

- nombre visible histórico
- legajo
- dependencia
- jerarquía u otros datos snapshotados
- consistencia histórica aunque el inspector luego cambie en el catálogo vivo

---

# 11) Relación futura: CmteXActa

## Estado

Queda prevista pero no desarrollada todavía en detalle dentro de esta capa.

Se mantiene como extensión futura para integración con comprobantes o componentes económicos/documentales según evolución del modelo.

---

# 12) Snapshots operativos proyectados en Acta

Aunque otras capas posean sus propias entidades, `Acta` puede mantener snapshots de lectura rápida como:

- estado actual del flujo
- fecha/hora último evento
- último tipo de evento
- situación documental actual
- situación de notificación actual
- situación de pago actual
- referencias rápidas para bandejas

Estos snapshots deben ser consistentes con el historial/eventos y no reemplazan la fuente de verdad.

---

# 13) Reglas de consistencia de la capa

## Reglas principales

1. Toda `Acta` debe tener identidad técnica.
2. Toda `Acta` debe poder reconstruir su estado desde `ActaEvento` + snapshots.
3. Toda `Acta` debe tener al menos un `ActaDomicilio`.
4. Si `Acta.IdActaDomicilioPrincipal` no es null, debe apuntar a un domicilio de la misma acta.
5. `ActaEvento` es append-only.
6. `ActaEvidencia` pertenece al acta y no al documento.
7. `ActaObservacion` no reemplaza eventos relevantes.
8. `IdInspector` + `VersionInspector` deben preservar referencia histórica.
9. La geolocalización del hecho vive en `Acta`.
10. Los estados y tipos relevantes deben resolverse con enums/catálogos cerrados.

---

## Regla transaccional de alta

Dado que existe referencia cruzada entre `Acta` y `ActaDomicilioPrincipal`, el alta completa se resuelve normalmente así:

1. crear `Acta`
2. crear `ActaDomicilio` inicial para esa acta
3. marcarlo como principal
4. actualizar `Acta.IdActaDomicilioPrincipal`

Esto es correcto y esperado.

---

# 14) Resultado de la capa

Con esta estructura, la Capa 01 resuelve:

- entidad central única
- historial completo
- snapshots operativos
- offline/online
- inspector histórico
- evidencias y observaciones propias
- soporte para tránsito y contravención
- domicilio operativo estructurado desde el inicio
- base consistente para documental, notificación y economía
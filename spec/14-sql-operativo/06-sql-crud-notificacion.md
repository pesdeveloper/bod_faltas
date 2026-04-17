# [14-SQL-OPERATIVO] 06 - SQL CRUD NOTIFICACION

## Finalidad

Este archivo documenta las operaciones SQL principales del circuito de notificación del expediente.

Tablas principales:

- `FalNotificacion`
- `FalNotificacionIntento`
- `FalNotificacionAcuse`
- `FalNotificacionObservacion`

El objetivo de este archivo es dejar explícito:

- qué patrón transaccional aplica en cada operación
- qué secuencias intervienen
- qué shape mínimo de datos requiere cada alta
- cómo se estructura la trazabilidad de la notificación
- qué operaciones pueden impactar evento o snapshot

---

## Reglas generales

- `FalNotificacion` representa la cabecera lógica de una notificación.
- Toda notificación recae sobre un documento (`IdDocu`) y sobre un acta (`IdActa`).
- `FalNotificacionIntento` registra los intentos realizados.
- `FalNotificacionAcuse` registra el acuse cuando corresponda.
- `FalNotificacionObservacion` registra contexto adicional o comentario auxiliar.
- Los intentos son append-only.
- La existencia de una notificación no implica necesariamente que ya exista acuse.
- El estado resumido de `FalNotificacion` puede actualizarse conforme se registran intentos o acuses.
- Si una operación de notificación tiene relevancia procesal, puede requerir registro de `FalActaEvento`.
- Si una operación de notificación impacta flags operativos visibles, puede requerir actualización de `FalActaSnapshot`.

---

## Operaciones principales

### 1. Crear notificación

#### Patrón transaccional aplicable

- patrón fundamental: **Alta de agregado padre/hijos**
- pasos comunes posibles:
  - registrar evento append-only
  - actualizar proyección resumida

#### Secuencias principales

- secuencia de `FalNotificacion`
- si se crea intento inicial, también debe intervenir la secuencia de `FalNotificacionIntento`

#### Shape mínimo requerido

La creación de una notificación debe recibir, como mínimo:

- `IdDocu`
- `IdActa`
- `TipoNotif`
- `EstadoNotif`
- `CanalNotif`
- `SiRequiereAcuse`
- `SiAcuseRecibido`
- `FhAlta`
- `IdUserAlta`

Además, puede recibir opcionalmente:

- `FhGeneracion`
- `FhEmision`
- `EstadoAcuse`
- `FhAcuse`
- intento inicial
- observación inicial
- datos para registrar evento o impacto en snapshot

#### Orden base

1. obtener `IdNotif`
2. insertar `FalNotificacion`
3. si corresponde, obtener `Id` de `FalNotificacionIntento`
4. insertar intento inicial
5. opcionalmente registrar `FalActaEvento`
6. opcionalmente actualizar `FalActaSnapshot`
7. confirmar transacción

#### Regla operativa

La creación de la notificación debe dejar resuelto, como mínimo:

- que la notificación lógica existe
- que su vínculo con acta y documento quedó correctamente establecido
- que el intento inicial, si existe, quedó trazado como registro independiente

---

### 2. Registrar intento posterior

#### Tabla principal

- `FalNotificacionIntento`

#### Patrón transaccional aplicable

- patrón fundamental: **Alta simple**
- con posible actualización resumida sobre `FalNotificacion`

#### Secuencia principal

- secuencia de `FalNotificacionIntento`

#### Shape mínimo requerido

La operación debe recibir, como mínimo:

- `IdNotif`
- `NroIntento`
- `CanalNotif`
- `TipoDestNotif`
- `DestNotif`
- `EstadoIntento`
- `FhIntento`
- `FhAlta`
- `IdUserAlta`

Además, puede recibir opcionalmente:

- `FhResultado`
- `ResultadoIntento`

#### Orden base

1. localizar `FalNotificacion`
2. obtener `Id` de `FalNotificacionIntento`
3. insertar `FalNotificacionIntento`
4. opcionalmente actualizar estado resumido de `FalNotificacion`
5. opcionalmente registrar `FalActaEvento`
6. opcionalmente actualizar `FalActaSnapshot`
7. confirmar transacción

#### Regla

- append-only
- `NroIntento` debe quedar controlado
- el historial de intentos no debe sobrescribirse
- el estado resumido de `FalNotificacion` puede actualizarse si el modelo lo admite

---

### 3. Registrar acuse

#### Tabla principal

- `FalNotificacionAcuse`

#### Patrón transaccional aplicable

- patrón fundamental: **Alta simple**
- con posible actualización resumida sobre `FalNotificacion`

#### Secuencia principal

- secuencia de `FalNotificacionAcuse`

#### Shape mínimo requerido

La operación debe recibir, como mínimo:

- `IdNotif`
- `TipoAcuse`
- `EstadoAcuse`
- `FhAcuse`
- `FhAlta`
- `IdUserAlta`

Además, puede recibir opcionalmente:

- `IdIntentoNotif`
- `StorageKeyAcuse`

#### Orden base

1. localizar `FalNotificacion`
2. obtener `Id` de `FalNotificacionAcuse`
3. insertar `FalNotificacionAcuse`
4. opcionalmente actualizar:
   - `SiAcuseRecibido`
   - `EstadoAcuse`
   - `FhAcuse`
   en `FalNotificacion`
5. opcionalmente registrar `FalActaEvento`
6. opcionalmente actualizar `FalActaSnapshot`
7. confirmar transacción

#### Regla

El acuse debe registrarse como información asociada a la notificación y no como reemplazo del historial de intentos.

---

### 4. Registrar observación

#### Tabla principal

- `FalNotificacionObservacion`

#### Patrón transaccional aplicable

- patrón fundamental: **Alta simple**
- opcionalmente con evento append-only si la observación tiene efecto procesal

#### Secuencia principal

- secuencia de `FalNotificacionObservacion`

#### Shape mínimo requerido

La operación debe recibir, como mínimo:

- `IdNotif`
- `ObsNotif`
- `FhAlta`
- `IdUserAlta`

#### Orden base

1. localizar `FalNotificacion`
2. obtener `Id` de `FalNotificacionObservacion`
3. insertar `FalNotificacionObservacion`
4. opcionalmente registrar `FalActaEvento`
5. confirmar transacción

#### Regla

La observación no reemplaza:

- la cabecera de notificación
- los intentos
- el acuse
- el evento procesal cuando este sea requerido

---

### 5. Obtener detalle de notificación

#### Lectura recomendada separada en

- cabecera de notificación
- intentos
- acuses
- observaciones

#### Regla de lectura

Se recomienda resolver el detalle con lecturas compuestas o joins cortos, evitando una consulta monolítica innecesaria.

El objetivo de esta lectura es responder:

- qué notificación existe
- sobre qué acta y documento recae
- qué intentos tuvo
- qué acuses posee
- qué observaciones quedaron registradas
- cuál es su estado resumido actual

---

## Consultas útiles

- notificaciones de una acta
- notificaciones de un documento
- notificaciones abiertas
- notificaciones con último intento fallido
- notificaciones con acuse positivo
- notificaciones pendientes de reintento
- observaciones de una notificación
- historial resumido de notificación por expediente

---

## Riesgos a controlar

- crear notificación sin vínculo correcto con acta o documento
- sobrescribir historia de intentos en lugar de insertar nuevo registro
- registrar acuse sin notificación válida
- mezclar estado resumido con trazabilidad histórica
- no actualizar flags operativos cuando el circuito de notificación impacta bandejas o snapshot
- usar una única query gigante para resolver todo el detalle de notificación

---
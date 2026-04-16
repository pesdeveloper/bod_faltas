# [14-SQL-OPERATIVO] 06 - SQL CRUD NOTIFICACION

## Finalidad

Este archivo documenta las operaciones SQL principales del circuito de notificación del expediente.

Tablas principales:

- `FalNotificacion`
- `FalNotificacionIntento`
- `FalNotificacionAcuse`
- `FalNotificacionObservacion`

---

## Operaciones principales

### 1. Crear notificación

Orden base:

1. obtener `Id` de `FalNotificacion`
2. insertar `FalNotificacion`
3. si corresponde, obtener `Id` de `FalNotificacionIntento`
4. insertar intento inicial
5. actualizar estado resumido si corresponde
6. opcionalmente insertar evento
7. confirmar transacción

### 2. Registrar intento posterior

Tabla principal:
- `FalNotificacionIntento`

Regla:
- append-only
- el número de intento debe quedar controlado
- el estado resumido de `FalNotificacion` puede actualizarse si el modelo lo admite

### 3. Registrar acuse

Tabla principal:
- `FalNotificacionAcuse`

### 4. Registrar observación

Tabla principal:
- `FalNotificacionObservacion`

### 5. Obtener detalle de notificación

Lectura recomendada separada en:

- cabecera de notificación
- intentos
- acuses
- observaciones

---

## Consultas útiles

- notificaciones de una acta
- notificaciones de un documento
- notificaciones abiertas
- notificaciones con último intento fallido
- notificaciones con acuse positivo
- notificaciones pendientes de reintento

---

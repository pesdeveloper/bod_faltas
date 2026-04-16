# [14-SQL-OPERATIVO] 02 - SQL DE BANDEJAS

## Finalidad

Este archivo define el enfoque operativo para las consultas SQL de bandejas del sistema.

La regla general es:

- las bandejas deben apoyarse preferentemente en `FalActaSnapshot`
- el detalle profundo se resuelve fuera de la bandeja
- la bandeja devuelve solo información operativa para decidir y abrir el caso

---

## Reglas generales

### Tabla base principal
Salvo excepción justificada, la base primaria será:

- `FalActaSnapshot`

### Joins mínimos esperados
Podrán incorporarse joins controlados a:

- `FalActa`
- `FalDependenciaVersion`
- `FalInspectorVersion`
- `FalDocumento`
- `FalNotificacion`

### Shape general esperado
Cada fila debería poder devolver, según corresponda:

- `IdActa`
- `NroActa`
- `TipoActa`
- `BloqueActual`
- `EstadoProcesoActual`
- `SituacionAdministrativaActual`
- `FechaHoraUltimoEvento`
- `FechaHoraCreacion`
- `TienePendienteMaterial`
- `TieneMedidaPreventivaActiva`
- `TieneDocumentosPendientesFirma`
- `TieneNotificacionEnCurso`
- `PrioridadOperativa`

### Orden recomendado

1. prioridad operativa descendente
2. fecha/hora del último evento o movimiento relevante descendente
3. `IdActa` como desempate estable

La paginación debe ser estable y compatible con Informix 12.10.

---

## Bandejas esperadas

- Actas Labradas / Revisión Inicial
- Enriquecimiento
- Preparación para notificación del acta
- Actas listas para notificar
- Notificación del acta en proceso
- Análisis de presentaciones / pagos
- Pendiente de acto administrativo
- Acto administrativo en proceso
- Pendiente de notificación de acto
- Notificación de acto en proceso
- Apelación
- Gestión externa
- Paralizadas
- Cerradas

---

## Regla de detalle

La bandeja no debe reconstruir el expediente completo.

El detalle del caso debe resolverse después, con lecturas específicas.

---

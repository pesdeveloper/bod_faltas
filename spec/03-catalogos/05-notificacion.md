# [CANONICO] NOTIFICACIÓN

# Estado
Canónico

# Última actualización
2026-04-06

# Propósito
Definir los catálogos y reglas canónicas vinculadas al proceso de notificación del sistema, incluyendo canales de notificación, estados de notificación, su semántica y su relación con `ActaEvento`, `Documento`, `EstadoActa` y `ActaSnapshotOperativo`.

# Relación con otros documentos
- `spec/01-dominio/00-modelo-canonico.md`
- `spec/03-catalogos/00-catalogos-globales.md`
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/04-snapshot/`
- `spec/06-contratos/`
- `spec/08-ddl-logico/03-notificaciones.md`
- `spec/02-procesos/p3-proceso-notificacion-y-acuse.md`

---

# 1. Definición general

La notificación se modela como un componente específico del sistema porque posee:

- ciclo operativo propio
- múltiples canales posibles
- estados operativos distinguibles
- relación con acuses
- impacto directo sobre el avance del trámite

La notificación no reemplaza:

- el documento notificado
- el evento procesal
- el estado global de la acta

Su función es resolver la gestión operativa del envío, seguimiento y resultado de una notificación concreta.

---

# 2. Catálogos canónicos incluidos en este documento

Este documento define los siguientes catálogos:

1. `CanalNotificacion`
2. `EstadoNotificacion`

---

# 3. Catálogo canónico `CanalNotificacion`

Los valores canónicos iniciales son:

ELECTRONICO
POSTAL
PRESENCIAL
CEDULA
SISTEMA_EXTERNO

# 4. Semántica de cada canal de notificación

## 4.1 ELECTRONICO
Canal de notificación realizado por medio electrónico válido según el proceso y la normativa aplicable.

Puede incluir correo electrónico u otros mecanismos electrónicos admitidos por el sistema.

---

## 4.2 POSTAL
Canal de notificación realizado mediante operador postal, correo, distribución física o mecanismo equivalente de envío material.

---

## 4.3 PRESENCIAL
Canal de notificación realizado mediante comparecencia o intervención presencial en la que la notificación queda formalmente instrumentada.

---

## 4.4 CEDULA
Canal de notificación instrumentado mediante cédula u otra modalidad formal específica equivalente dentro del circuito administrativo o judicial.

---

## 4.5 SISTEMA_EXTERNO
Canal utilizado cuando la notificación se apoya formalmente en un sistema externo o circuito tercero que produce el efecto operativo correspondiente.

No implica por sí mismo detalle técnico del mecanismo; solo expresa la procedencia operativa del canal.

---

# 5. Regla general sobre canales

El canal expresa el medio operativo principal por el cual se intenta o concreta la notificación.

No expresa:

- el resultado
- el acuse
- la validez final
- el estado del expediente
- el tipo de documento notificado

Esos aspectos se resuelven con otros elementos del modelo.

---

# 6. Catálogo canónico `EstadoNotificacion`

Los valores canónicos iniciales son:

PENDIENTE_ENVIO
ENVIADA
ACUSE_RECIBIDO
ACUSE_RECHAZADO
REENVIADA
CERRADA

# 7. Semántica de cada estado de notificación

## 7.1 PENDIENTE_ENVIO
La notificación existe como gestión pendiente, pero todavía no se materializó el envío por el canal previsto.

---

## 7.2 ENVIADA
La notificación ya fue emitida o enviada operativamente por el canal correspondiente.

No implica por sí sola resultado suficiente de notificación ni acuse válido.

---

## 7.3 ACUSE_RECIBIDO
La notificación recibió un acuse considerado válido o suficiente según las reglas operativas del sistema.

Este estado expresa un hito operativo de la notificación concreta.

---

## 7.4 ACUSE_RECHAZADO
La notificación recibió un acuse inválido, insuficiente, rechazado o no apto para consolidar el resultado esperado.

---

## 7.5 REENVIADA
La notificación fue enviada nuevamente dentro del mismo circuito de gestión, a raíz de rechazo, falta de resultado o necesidad operativa equivalente.

---

## 7.6 CERRADA
La gestión operativa de esa notificación concreta se considera concluida.

No implica necesariamente que el expediente entero haya terminado ni que toda obligación de notificar futura esté agotada.

---

# 8. Regla general sobre estados de notificación

`EstadoNotificacion` expresa la situación operativa de una notificación puntual.

No debe confundirse con:

- `EstadoActa`
- `EstadoDocumento`
- `TipoEventoActa`

Una acta puede estar en un estado global y, al mismo tiempo, una notificación concreta atravesar otro estado específico dentro de su propio ciclo.

---

# 9. Relación entre notificación y evento

La gestión de notificación puede producir eventos procesales relevantes en `ActaEvento`, pero no se confunde con ellos.

Los eventos canónicos relevantes del catálogo de eventos son:

- `NOTIFICACION_ENVIADA`
- `ACUSE_RECIBIDO`
- `ACUSE_RECHAZADO`
- `NOTIFICACION_REENVIADA`

La entidad `Notificacion` modela la operación.  
`ActaEvento` modela la narrativa procesal relevante derivada de esa operación.

---

# 10. Regla fuerte sobre eventos de notificación

No deben formar parte del canon eventos como:

- `NOTIFICACION_PRACTICADA`
- `RESULTADO_NOTIFICACION_INCORPORADO`

La semántica de notificación se debe resolver con la combinación de:

- entidad `Notificacion`
- `EstadoNotificacion`
- eventos canónicos
- documento asociado
- estado de acta

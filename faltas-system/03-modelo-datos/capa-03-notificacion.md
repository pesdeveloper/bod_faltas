# [CAPA 03] NOTIFICACIÓN

## Finalidad de la capa

La Capa 03 define el modelo estructural del proceso de notificación dentro del sistema de faltas.

Debe resolver:

- selección y persistencia de domicilios vinculados al acta
- generación de intentos de notificación
- asociación entre notificación y documentos
- gestión de reintentos
- registro de resultados / acuses
- registro de observaciones operativas
- agrupación en lotes cuando corresponda
- trazabilidad completa del proceso de notificación

Esta capa NO define el flujo (eso vive en D3 / D6), sino la estructura persistente que soporta ese flujo.

---

# Principios de diseño

## 1. La notificación es un intento, no un estado global

Notificacion representa un intento formal de notificar.

No representa el estado global de la causa.

---

## 2. No hay BPM interno en esta capa

La lógica del proceso vive en:

- D1–D8
- ActaEvento
- reglas de negocio

Esta capa solo persiste:

- qué se intentó
- a quién
- cómo
- cuándo
- con qué documentos
- con qué resultado

---

## 3. Reintentos = nuevas filas

Un reintento:

- NO modifica la notificación original
- NO se recicla la fila

Se crea una nueva Notificacion con:

- IdNotificacionOrigen

---

## 4. Domicilio desacoplado de Acta

El domicilio vive en:

- ActaDomicilio (Capa 01)

Esto permite:

- múltiples domicilios por acta
- historial de domicilios
- reutilización en múltiples notificaciones

---

## 5. Una notificación puede incluir múltiples documentos

Relación mediante:

- NotificacionDocumento

Esto permite:

- notificar acta + fallo juntos
- adjuntar documentación complementaria

---

## 6. El resultado se registra aparte

No se guarda en Notificacion.

Se modela en:

- NotificacionResultado

Esto permite:

- historial
- anulaciones
- múltiples informes

---

## 7. El lote es entidad propia

Se modela con:

- LoteNotificacion
- LoteNotificacionDetalle

Regla clave:

Notificacion NO tiene IdLoteNotificacion

---

## 8. Modelo consistente con event-driven

Esta capa:

- NO reemplaza ActaEvento
- NO contiene el flujo
- NO decide estados de la causa

Solo materializa la notificación

---

# Entidades de la capa

## 1. ActaDomicilio (reutilizada de Capa 01)

Representa domicilios vinculados a un acta.

### Características

- nace con el acta
- puede haber múltiples
- uno puede ser principal
- uno puede ser el original del acta
- puede inactivarse sin borrarse

### Uso en notificación

- cada notificación referencia un ActaDomicilio
- permite elegir destino específico

---

## 2. Notificacion

Representa un intento de notificación.

### Contiene

- acta
- domicilio
- canal
- motivo
- estado operativo
- datos del receptor
- fechas operativas

### No contiene

- resultado
- documentos
- lote

---

## 3. NotificacionDocumento

Relación entre:

- Notificacion
- Documento

Permite:

- múltiples documentos por notificación
- orden y rol dentro del envío

---

## 4. NotificacionResultado

Representa el resultado del intento.

### Ejemplos

- entregada
- recibida
- rechazada
- devuelta
- sin acuse

### Reglas

- no se borra
- se puede anular
- puede haber múltiples registros

---

## 5. NotificacionObservacion

Notas operativas.

### Uso

- aclaraciones
- incidencias
- errores de carga
- comentarios administrativos

---

## 6. LoteNotificacion

Representa agrupación operativa.

### Uso

- envíos postales
- procesos batch
- control operativo

---

## 7. LoteNotificacionDetalle

Relaciona:

- lote
- notificaciones

---

# Flujo conceptual (simplificado)

Acta
↓
Selección de domicilio (ActaDomicilio)
↓
Creación de Notificacion
↓
Asociación de documentos (NotificacionDocumento)
↓
(opcional) inclusión en lote
↓
Envío
↓
Registro de resultado (NotificacionResultado)
↓
Observaciones (si aplica)

---

# Reglas clave de negocio

## Domicilios

- debe existir al menos uno por acta
- uno puede ser principal
- el original se preserva

---

## Notificación

- siempre referencia acta
- siempre referencia domicilio
- puede no tener resultado aún
- puede ser anulada

---

## Reintentos

- nueva fila
- opcionalmente referencia origen

---

## Resultado

- no se borra
- se anula si es incorrecto
- el vigente se determina por lógica

---

## Documentos

- múltiples por notificación
- no se duplican dentro de la misma

---

## Lotes

- opcionales
- no acoplan la notificación
- permiten procesamiento batch

---

# Consistencia con otras capas

## Con Capa 01

- usa Acta
- usa ActaDomicilio
- respeta snapshot en Acta
- no reemplaza ActaEvento

---

## Con Capa 02

- usa Documento
- no modifica su modelo
- soporta múltiples documentos por notificación

---

# Decisiones importantes ya cerradas

ActaDomicilio existe desde Capa 01  
Notificacion NO tiene lote directo  
Resultado separado  
Reintentos son nuevas filas  
Documento desacoplado  
NumeroDocumentoReceptor es numérico  
No hay BPM interno  
No se pierde historial  
Append-only conceptual  

---

# Observaciones de implementación

## Enums

Todos estos campos deben ser catálogo cerrado:

- TipoDomicilio
- CanalNotificacion
- EstadoNotificacion
- MotivoNotificacion
- VinculoReceptor
- TipoDocumentoReceptor
- TipoResultadoNotificacion
- TipoObservacionNotificacion
- TipoLoteNotificacion
- EstadoLoteNotificacion

---

## Persistencia

- evitar deletes físicos
- usar flags (Activo / Anulado)
- mantener trazabilidad completa

---

## Performance

- índices por Acta
- índices por estado
- índices por fechas
- índices por lote

---

# Cierre

La Capa 03 queda definida como:

una capa estructural simple  
trazable  
desacoplada del flujo  
consistente con el modelo event-driven  
preparada para múltiples canales reales  

sin introducir complejidad innecesaria
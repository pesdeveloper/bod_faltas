# 04-servicios-de-notificacion.md

## Finalidad

Este archivo define las responsabilidades del backend vinculadas a la notificación del expediente.

Su objetivo es dejar claro cómo se modela la notificación como proceso transversal único y cómo impacta sobre documentos, expediente, snapshot y bandejas.

---

## Regla principal

En el modelo actual, toda notificación recae sobre un documento del expediente y queda asociada a una acta.

Cada intento de notificación se dirige a un único destino efectivo, cuyo canal, resultado y trazabilidad se registran dentro del intento mismo.

La notificación no reemplaza al documento. Lo usa como pieza notificable.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- generar una notificación asociada a documento y expediente
- registrar intentos de notificación
- registrar canal, destino efectivo y resultado del intento
- registrar acuse cuando corresponda
- proyectar el efecto operativo de la notificación

---

## Responsabilidades principales

### 1. Inicio del circuito de notificación
Debe permitir:

- identificar documento notificable
- generar notificación asociada
- registrar canal principal
- determinar si requiere acuse

---

### 2. Intentos de notificación
Debe permitir:

- registrar uno o más intentos
- identificar el destino efectivo de cada intento
- registrar canal del intento
- registrar resultado del intento
- registrar fecha/hora de intento y resultado

---

### 3. Acuse
Debe permitir:

- registrar si existe acuse
- registrar su estado resumido
- registrar fecha/hora de acuse
- asociar constancia o soporte técnico si existe

---

### 4. Impacto operativo
Debe permitir proyectar sobre expediente y snapshot:

- si la notificación fue emitida
- si está en proceso
- si fue notificada
- si está pendiente de acuse
- si hubo reintentos
- qué efecto habilita o bloquea en el flujo posterior

---

## Qué no debe hacer

Este bloque no debe absorber:

- lógica documental completa
- lógica de firma
- lógica económica
- lógica detallada de bandejas más allá de la proyección necesaria
- modelado artificial de múltiples destinos estructurados si cada intento ya refleja su destino efectivo

---

## Relación con otros servicios

### Con documental
La notificación siempre recae sobre un documento del expediente.

### Con expediente
La notificación impacta sobre estado operativo, trazabilidad y próximos pasos.

### Con snapshot
El resultado notificatorio debe poder proyectarse de forma resumida para operación y bandejas.

### Con bandejas
La notificación debe reflejar si existen pendientes de emisión, proceso, acuse o reintentos relevantes.

---

## Idea clave

La notificación es un proceso transversal del expediente, apoyado en documentos notificables y trazado mediante intentos y acuses con impacto operativo claro.
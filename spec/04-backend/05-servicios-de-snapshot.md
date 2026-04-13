# 05-servicios-de-snapshot.md

## Finalidad

Este archivo resume los servicios de backend vinculados al `SnapshotOperativo` del expediente.

Su objetivo es identificar las responsabilidades del backend respecto de la construcción, actualización, consulta y regeneración del snapshot como resumen operativo del expediente.

No define todavía endpoints, DTOs ni detalles técnicos de implementación.

---

## Regla principal

El `SnapshotOperativo` es **derivado y regenerable**.

El backend no debe tratarlo como fuente de verdad, sino como un resumen operativo que permite:

- bandejas
- filtros
- badges
- bloqueos
- habilitación de acciones
- lectura rápida

La fuente de verdad sigue viviendo en las piezas transaccionales y documentales del sistema.

---

## Qué debe resolver

Este bloque debe permitir, al menos:

- construir snapshot a partir de la fuente de verdad
- actualizar snapshot cuando ocurren hechos relevantes
- recalcular snapshot si existe inconsistencia
- exponer snapshot para bandejas y detalle resumido
- resumir bloqueos, pendientes y situación operativa actual del expediente

---

## Responsabilidades principales

### 1. Construcción inicial del snapshot
Debe permitir construir el snapshot operativo del expediente a partir de:

- expediente / acta
- eventos relevantes
- documentos
- notificaciones
- medidas y liberaciones
- resultados externos
- demás piezas con efecto operativo

---

### 2. Actualización por hechos relevantes
Debe permitir actualizar o recalcular snapshot cuando ocurren hechos como:

- generación documental
- firma
- notificación
- acuse
- presentación
- pago
- medida aplicada o levantada
- liberación material
- derivación externa
- reingreso
- archivo
- cierre
- paralización
- reanudación

---

### 3. Exposición de situación operativa
Debe permitir exponer al resto del sistema, al menos:

- bandeja visible actual
- estado operativo resumido
- pendientes documentales
- pendientes de firma
- situación de notificación
- bloqueos de avance
- bloqueos de cierre
- estado de medidas y pendientes materiales
- condición de archivo o cerrada

---

### 4. Habilitación de acciones
Debe permitir que el sistema pueda determinar, a partir del snapshot, si un expediente:

- puede avanzar
- debe permanecer en su bandeja actual
- bloquea notificación
- bloquea cierre
- puede pasar a archivo
- puede pasar a cerrada
- requiere intervención previa

El snapshot no reemplaza la validación de negocio completa, pero sí debe servir como apoyo principal para la UI y las bandejas.

---

### 5. Regeneración y consistencia
Debe permitir recalcular snapshot cuando:

- se detecta inconsistencia
- cambió una regla derivada
- se reprocesa información del expediente
- se requiere reconstrucción administrativa o técnica

La regeneración no debe depender de datos que existan solo dentro del snapshot.

---

## Qué no debe hacer

Este bloque no debe:

- convertirse en segunda fuente de verdad
- guardar historia completa del expediente
- duplicar detalle jurídico o documental innecesario
- reemplazar eventos, documentos o notificaciones
- mezclar lógica de integración externa que no impacte en el resumen operativo

Debe limitarse al resumen operativo necesario para trabajar.

---

## Grupos de información que debe servir

### 1. Identificación operativa
- expediente
- situación general
- bandeja visible

### 2. Pendientes documentales
- faltan documentos
- faltan firmas
- faltan condiciones previas para avanzar

### 3. Notificaciones
- piezas en notificación
- acuse pendiente
- resultado relevante
- espera de plazo

### 4. Medidas y liberaciones
- medidas preventivas existentes
- activas
- levantadas
- pendientes de firma
- pendientes materiales de liberación o restitución

### 5. Bloqueos
- bloqueo de notificación
- bloqueo de cierre
- motivo resumido de bloqueo

### 6. Condición final
- archivo
- cerrada
- reingreso posible
- situación pendiente

---

## Relación con otros servicios

### Con servicios de expediente
El bloque de expediente coordina la situación general del caso, pero snapshot resume esa situación para operación diaria.

### Con servicios documentales
Los cambios documentales son una de las principales entradas del snapshot.

### Con servicios de firma
La firma puede habilitar continuidad del expediente y por eso impacta en snapshot.

### Con servicios de notificación
El estado y resultado de notificación modifican snapshot de forma directa.

### Con medidas y liberaciones
Las medidas activas y los pendientes materiales son claves para bloquear notificación o cierre y deben reflejarse en snapshot.

---

## Operaciones conceptuales típicas

Este bloque debería poder sostener operaciones conceptuales como:

- construir snapshot de un expediente
- recalcular snapshot de un expediente
- recalcular snapshot por lote
- obtener snapshot resumido
- obtener indicadores operativos para bandejas
- obtener bloqueos y habilitaciones
- verificar condición de archivo o cerrada

No implica que estas operaciones deban exponerse una a una como endpoints directos.

---

## Relación con bandejas

Las bandejas deben consumir snapshot como una de sus fuentes principales para:

- decidir visibilidad
- aplicar filtros
- mostrar badges
- habilitar acciones
- explicar por qué el expediente está ahí

La lógica de bandejas no debería recalcular por su cuenta toda la historia del expediente.

---

## Relación con la UI

La UI debe poder usar snapshot para:

- listados rápidos
- badges
- filtros
- mensajes de bloqueo
- acciones disponibles
- explicación resumida de la situación actual

Cuando haga falta detalle, la UI debe consultar las piezas fuente correspondientes.

---

## Idea clave

El backend no usa snapshot para reemplazar el expediente.

Usa snapshot para resumir, acelerar y hacer operable la situación vigente del expediente.

---

## Archivos relacionados

- [Mapa backend](00-mapa-backend.md)
- [Servicios de expediente](01-servicios-de-expediente.md)
- [Servicios documentales](02-servicios-documentales.md)
- [Servicios de firma](03-servicios-de-firma.md)
- [Servicios de notificación](04-servicios-de-notificacion.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Medidas y liberaciones](../01-dominio/06-medidas-y-liberaciones.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
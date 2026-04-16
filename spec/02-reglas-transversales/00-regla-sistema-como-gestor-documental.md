# 00-regla-sistema-como-gestor-documental.md

## Finalidad

Este archivo fija la regla base de lectura del sistema de faltas: el sistema debe entenderse como un **gestor documental orientado al expediente**.

Esta regla atraviesa:

- dominio
- bandejas
- snapshot
- vistas
- backend
- integraciones

---

## Regla principal

La unidad principal de gestión del sistema es el **expediente / acta**.

Los documentos, sus firmas, sus notificaciones, sus acuses, sus actuaciones y sus efectos materiales modifican la situación operativa del expediente.

Por lo tanto, la operatoria del sistema debe pensarse desde el expediente y no desde tareas o documentos aislados.

---

## Qué implica esta regla

### 1. El expediente es el centro
Toda acción relevante del sistema debe leerse como una acción sobre la Acta / Expediente.

### 2. Las bandejas son bandejas de expedientes
Las bandejas muestran expedientes según su situación documental y procesal actual.

No muestran documentos sueltos como unidad principal de trabajo.

### 3. Los documentos materializan el avance
Las decisiones y actuaciones relevantes se expresan mediante documentos y hechos asociados al expediente.
Los documentos se entienden como entidades lógicas del expediente, desacopladas de su soporte físico concreto, el cual se resuelve mediante almacenamiento externo referenciado por `StorageKey`.

### 4. La firma modifica la situación del expediente
Cuando un documento pasa a firmado, cambia la situación operativa del expediente y puede habilitar un nuevo paso.

### 5. La notificación modifica la situación del expediente
La notificación y su resultado no se leen como un hecho aislado, sino como un cambio relevante sobre el expediente.

### 6. El snapshot resume la situación operativa del expediente
El snapshot no es fuente de verdad. Resume la situación vigente del expediente para operar.

---

## Regla de bandejas

Las bandejas no son lugares a donde primero se envía el expediente para recién producir documentos.

La lógica correcta es:

- el expediente permanece en una bandeja mientras falten documentos o condiciones necesarias
- la generación o incorporación de documentos modifica su situación
- cuando el conjunto documental o material requerido queda completo, el sistema habilita la acción de pase a la siguiente bandeja operativa

---

## Regla de cambio de situación

La situación operativa del expediente puede cambiar, por ejemplo, cuando:

- se genera un documento
- se firma un documento
- se notifica una pieza
- se registra un acuse
- se incorpora una presentación
- se registra un pago
- se aplica una medida
- se levanta una medida
- se registra una liberación material
- se recibe un resultado externo
- se resuelve una apelación

Cada uno de estos hechos debe entenderse como un hecho del expediente.

---

## Regla de lectura operativa

La operatoria diaria debe privilegiar preguntas como estas:

- ¿en qué situación está este expediente?
- ¿qué documentos tiene pendientes?
- ¿qué bloquea su avance?
- ¿qué acción se habilita ahora?
- ¿requiere firma?
- ¿requiere notificación?
- ¿requiere fallo?
- ¿está en archivo?
- ¿ya puede cerrarse?

No debe privilegiarse una lectura fragmentada basada solo en microetapas o colas de documentos aislados.

---

## Regla de implementación

Las vistas, servicios y procesos del sistema deben construirse respetando esta lógica:

- se trabaja sobre expedientes
- se muestran sus documentos y pendientes
- se habilitan acciones según su situación
- los documentos y actuaciones cambian el estado operativo del expediente
- las bandejas reflejan ese estado

---

## Consecuencia sobre la spec

La spec debe describir el sistema siguiendo esta misma filosofía:

- expediente como centro
- documentos como piezas con efecto operativo
- snapshot como resumen
- bandejas como visibilidad operativa del expediente
- reglas transversales como determinantes del cambio de situación
- storage documental como soporte técnico desacoplado del dominio principal

---

## Archivos relacionados

- [Proyecto y objetivo](../00-overview/00-proyecto-y-objetivo.md)
- [Decisiones arquitectónicas](../00-overview/02-decisiones-arquitectonicas.md)
- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
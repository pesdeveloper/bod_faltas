# 07-bandeja-notificaciones.md

## Finalidad

Esta bandeja concentra el proceso transversal de notificación del sistema de faltas.

No se separa por tipo de documento en múltiples bandejas distintas.

La misma bandeja permite tratar la notificación de cualquier pieza notificable del expediente.

---

## Qué contiene

Contiene expedientes con una o más piezas dentro del proceso de notificación.

Puede incluir, entre otras:

- acta
- medida preventiva
- resolución
- fallo
- sentencia
- documento de liberación
- otra pieza notificable

La unidad visible sigue siendo el expediente / acta.

---

## Qué no contiene

No contiene expedientes solo por tener documentos pendientes de generar.

No contiene expedientes solo por tener documentos pendientes de firma.

Esos casos se resuelven en otras bandejas.

---

## Función operativa

Permite ver, filtrar y gestionar expedientes que tienen una notificación:

- pendiente de iniciar
- en curso
- con acuse pendiente
- con acuse positivo
- con acuse negativo
- vencida sin acuse
- en reintento
- a decisión manual
- finalizada

---

## Filtros mínimos sugeridos

### Por tipo de pieza
- acta
- medida preventiva
- resolución
- fallo
- sentencia
- liberación
- otra

### Por canal
- electrónica
- correo / carta documento
- notificador municipal
- comparecencia / retiro presencial
- otro

### Por estado de notificación
- pendiente
- en proceso
- acuse pendiente
- positivo
- negativo
- vencida
- reintento
- decisión manual

### Por plazo
- vence hoy
- vencida
- dentro de plazo
- post notificación de fallo

### Por operador
- asignado
- sin asignar
- por notificador

---

## Regla de entrada

Un expediente entra a esta bandeja cuando ya existe una pieza notificable y corresponde iniciar o continuar su tratamiento de notificación.

Típicamente entra desde:

- enriquecimiento / revisión inicial
- análisis / presentaciones / pagos
- pendientes de resolución / redacción
- pendientes de firma
- apelación
- reencauces

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras la notificación no haya producido todavía un resultado suficiente para definir el siguiente estado operativo.

Eso puede ocurrir, por ejemplo, cuando:

- todavía no se inició la diligencia
- la diligencia está en curso
- existe acuse pendiente
- se espera vencimiento de plazo
- se requiere reintento
- se requiere decisión manual

---

## Regla de salida

La salida de esta bandeja depende de:

- tipo de pieza notificada
- canal utilizado
- resultado de la notificación
- acuse
- vencimiento de plazo
- decisión manual
- efecto material de esa notificación sobre el expediente

No debe asumirse una salida única para todos los casos.

---

## Acciones típicas

- iniciar notificación
- registrar diligencia
- registrar acuse
- registrar resultado
- reintentar
- dejar a decisión manual
- reencauzar a otra bandeja
- derivar a gestión externa, si corresponde
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Casos especiales

Los plazos especiales y las particularidades por canal se rigen por las reglas transversales de notificación.

Esto incluye, entre otros:

- espera de hasta 7 días en notificación electrónica, según la regla aplicable
- espera de 5 días post fallo antes del siguiente destino natural
- notificación por notificador municipal con acuse en acto, firma dibujada y evidencia complementaria

Ver:
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)

---

## Relación con snapshot

El snapshot debe permitir identificar rápidamente si el expediente:

- tiene piezas en notificación
- tiene acuse pendiente
- tuvo resultado positivo o negativo
- requiere reintento
- requiere decisión manual
- está en espera de plazo post notificación

---

## Relación con la UI

La UI de esta bandeja debe privilegiar:

- qué se está notificando
- por qué canal
- en qué estado está
- si existe acuse
- si el resultado fue positivo o negativo
- si requiere reintento
- si requiere decisión manual
- cuál es el próximo efecto esperado sobre el expediente

---

## Idea clave

La notificación se trata como una capacidad transversal del sistema, no como una familia de microbandejas separadas por tipo de documento.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Integración de notificaciones](../09-integraciones/02-integracion-notificaciones.md)
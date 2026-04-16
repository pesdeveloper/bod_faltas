# 02-reglas-de-notificacion.md

## Finalidad

Este archivo fija las reglas generales del proceso de notificación dentro del sistema de faltas.

La notificación se trata como un **proceso transversal único**, aplicable a cualquier pieza notificable del expediente.

---

## Regla principal

La notificación no se modela como múltiples bandejas separadas por tipo de documento.
En el modelo del sistema, toda notificación recae sobre un documento del expediente y queda asociada a la acta correspondiente.

Se concentra en una única lógica operativa, con filtros por:

- tipo de pieza
- canal
- estado
- acuse
- vencimiento
- reintento
- resultado

---

## Qué puede notificarse

El sistema debe permitir notificar, entre otras piezas:

- acta
- medida preventiva
- resolución
- fallo
- sentencia
- documento de liberación
- otra pieza documental notificable

---

## Regla de impacto sobre el expediente

La notificación no se lee como un hecho aislado del documento.

Su resultado modifica la situación operativa del expediente y puede:

- habilitar continuidad
- abrir plazo
- exigir reintento
- exigir decisión manual
- provocar reencauce
- habilitar gestión externa
- mantener el expediente en notificación

---

## Canales previstos

- domicilio electrónico
- email
- postal
- bluemail
- notificador municipal
- portal ciudadano
- otro canal formal que luego se defina

---

## Regla de acuse

El sistema debe distinguir claramente entre:

- notificación iniciada
- notificación en curso
- acuse pendiente
- acuse positivo
- acuse negativo
- situación vencida sin acuse
- reintento pendiente
- decisión manual pendiente

El acuse puede provenir por distintas vías según el canal utilizado.

Cada intento de notificación se dirige a un único destino efectivo, cuyo canal, resultado y datos relevantes deben quedar trazados como parte del intento mismo.

---

## Reglas por canal

### Notificación electrónica
Cuando exista domicilio fiscal constituido o medio electrónico válido, debe contemplarse una espera de hasta **7 días**, según la regla aplicable al caso.

### Correo / carta documento
Debe contemplarse espera de acuse externo antes de resolver el resultado final.

### Notificador municipal
Debe permitir registrar, al menos:

- pieza entregada
- fecha y hora de diligencia
- resultado
- observaciones
- firma dibujada en dispositivo
- evidencia complementaria, si existiera

En este canal, el acuse puede generarse en el mismo acto.

### Comparecencia o retiro presencial
Debe registrarse el hecho y su resultado como notificación válida según corresponda.

---

## Regla de salida

La salida del proceso de notificación depende de:

- tipo de pieza notificada
- canal utilizado
- resultado
- acuse
- vencimiento de plazo
- decisión manual
- efecto que esa notificación produce sobre el expediente

No existe una salida única obligatoria para todos los casos.

---

## Regla de reintento y decisión manual

Si no existe acuse suficiente o el resultado no permite cerrar el tratamiento de la notificación, el sistema debe permitir:

- reintentar
- archivar, si correspondiera
- reencauzar el expediente
- mantenerlo a decisión manual

---

## Regla especial de fallo notificado

Cuando se notifica un fallo, el sistema debe contemplar una espera de **5 días** antes del siguiente destino natural.

Si no existe novedad que altere el recorrido, el expediente puede quedar en condición de derivación a gestión externa.

No se requiere una bandeja separada para esta espera.  
Debe resolverse mediante estado, filtro y reglas de salida.

---

## Relación con snapshot

El snapshot operativo debe resumir, al menos, si el expediente:

- tiene piezas en notificación
- tiene acuse pendiente
- tiene resultado positivo o negativo relevante
- está en espera de plazo post notificación
- está listo para el siguiente paso

---

## Regla de trazabilidad

Toda notificación relevante debe dejar trazabilidad suficiente en el expediente, incluyendo según corresponda:

- pieza notificada
- canal
- fechas relevantes
- estado
- acuse
- resultado
- reintentos
- intervención del notificador
- observaciones

---

## Regla de UI

La UI debe permitir ver claramente:

- qué se está notificando
- por qué canal
- en qué estado está
- si existe acuse
- si el resultado fue positivo o negativo
- si requiere reintento
- si requiere decisión manual
- cuál es el próximo efecto esperado sobre el expediente

---

## Archivos relacionados

- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Bandeja de notificaciones](../03-bandejas/07-bandeja-notificaciones.md)
- [Integración de notificaciones](../09-integraciones/02-integracion-notificaciones.md)
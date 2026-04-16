# Reglas de reingreso

## Finalidad

Este archivo define las reglas transversales de reingreso del expediente dentro del sistema.

Su objetivo es dejar claro:

- cuándo un expediente puede reingresar
- qué efectos produce ese reingreso
- cómo debe trazarse
- cómo impacta en bandejas y snapshot

---

## Regla principal

El reingreso es la reactivación de un expediente que había salido del circuito activo principal o que había quedado fuera de la tramitación ordinaria.

El reingreso:

- no crea un expediente nuevo
- no borra historia previa
- no reemplaza eventos anteriores
- debe quedar trazado como un nuevo evento del expediente

---

## Casos típicos de reingreso

El expediente puede reingresar, entre otros casos, desde:

- gestión externa
- archivo administrativo
- resultado externo que requiere nueva actuación
- nueva presentación que obliga a retomar tratamiento
- revisión administrativa posterior a una salida previa del circuito principal

---

## Regla de identidad

El reingreso:

- conserva la identidad del expediente / acta
- conserva su historial
- conserva documentos, notificaciones y actuaciones previas
- modifica el estado operativo actual a partir del nuevo evento de reingreso

---

## Regla de trazabilidad

Todo reingreso debe quedar asentado mediante:

- un evento del expediente
- identificación de la causa o motivo
- fecha/hora
- usuario u origen del reingreso, si corresponde

El sistema no debe “mover” el expediente sin que exista trazabilidad explícita del reingreso.

---

## Regla de snapshot

Cuando un expediente reingresa:

- debe recalcularse el snapshot operativo
- debe actualizarse la bandeja visible
- debe reflejarse si el expediente:
  - reingresó desde gestión externa
  - reingresó desde archivo
  - reingresó con resultado condicionado o pendiente

Si el snapshot mantiene un campo específico de reingreso, este debe derivarse del evento y no actuar como fuente primaria de verdad.

---

## Regla de bandejas

El reingreso debe ubicar nuevamente al expediente en la bandeja que corresponda según:

- su estado procesal resultante
- la causa del reingreso
- la documentación o resultado que motivó la reactivación

El expediente no debe volver automáticamente a una bandeja genérica si la regla de negocio exige una bandeja específica.

---

## Regla sobre documentos y notificaciones previas

El reingreso no invalida automáticamente:

- documentos anteriores
- notificaciones anteriores
- acuses previos
- actuaciones ya registradas

Solo se deben generar nuevas actuaciones cuando el reingreso así lo requiera.

---

## Regla sobre gestión externa

Cuando el reingreso proviene de gestión externa:

- debe poder identificarse el tipo de gestión externa de origen
- debe poder reflejarse el resultado resumido de esa gestión
- el snapshot debe permitir saber si existió reingreso desde gestión externa

---

## Resultado esperado

El reingreso debe tratarse como una reactivación trazable del expediente, sin pérdida de historia y con impacto explícito en evento, snapshot y bandeja.
# 05-bandeja-pendientes-fallo.md

## Finalidad

Esta bandeja reúne expedientes que ya están en condición de requerir fallo.

Su función es dar visibilidad específica al tramo en el que el expediente debe pasar a decisión de fallo y a la producción documental asociada.

---

## Qué contiene

Contiene expedientes que, por su situación actual, ya deben encaminarse al dictado de fallo.

Ejemplos típicos:

- venció el plazo luego de notificación
- no hubo comparecencia
- no hubo pago voluntario suficiente
- el expediente reingresó desde gestión externa y requiere nuevo fallo
- corresponde rectificar o modificar un fallo previo

---

## Qué no contiene

No contiene expedientes cuya necesidad actual sea una pieza no-fallo.

No contiene expedientes cuyo problema actual sea solo de firma, notificación o archivo.

No contiene expedientes que todavía siguen en análisis sin decisión clara de pasar a fallo.

---

## Función operativa

Su función es permitir que el expediente quede visible cuando ya se definió que requiere fallo.

Desde aquí se produce el fallo y la documentación asociada que corresponda.

Una vez generado el fallo, el expediente debe pasar a la situación siguiente que corresponda, normalmente firma.

---

## Regla de entrada

Un expediente entra a esta bandeja cuando ya existe una condición suficiente para requerir fallo.

Puede ingresar, por ejemplo, desde:

- enriquecimiento / revisión inicial
- análisis / presentaciones / pagos
- notificaciones
- apelación
- gestión externa
- reencauces

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras el fallo requerido todavía no haya sido producido.

No debe permanecer aquí si el fallo ya fue generado y el problema actual pasó a ser de firma o de notificación.

---

## Regla de salida

La salida de esta bandeja se habilita cuando el fallo ya fue generado.

El siguiente destino depende de la situación documental del expediente.

Destinos típicos:

- pendientes de firma, si el fallo o documentos asociados requieren firma
- análisis / presentaciones / pagos, si surge una actuación que obliga a reencauzar
- archivo, si el expediente quedó resuelto pero no puede cerrarse
- paralizadas, si corresponde detención fundada

En condiciones excepcionales, una decisión válida podría modificar este recorrido.

---

## Acciones típicas

- redactar fallo
- generar documentación asociada al fallo
- enviar a pendientes de firma
- reencauzar a análisis
- enviar a archivo
- paralizar

---

## Relación con notificación

Un expediente puede llegar aquí luego de una notificación cuando:

- se cumplió el plazo correspondiente
- no hubo comparecencia
- no existió otra novedad que altere el recorrido
- o el resultado de la notificación dejó al expediente en condición de fallo

La bandeja no reemplaza la lógica de notificación, sino que recibe el expediente cuando la consecuencia material de esa notificación es el paso a fallo.

---

## Relación con gestión externa

Un expediente puede reingresar aquí desde gestión externa cuando el resultado externo exige:

- nuevo fallo
- rectificación de fallo
- modificación de fallo
- nueva decisión equivalente con efecto sobre el expediente

---

## Relación con snapshot

El snapshot debe permitir identificar, al menos:

- que el expediente está en condición de requerir fallo
- que el fallo aún no fue producido
- que ya existe habilitación para pasar a esta bandeja
- si existen otros bloqueos relevantes que condicionen el siguiente paso

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- por qué el expediente está para fallo
- si llegó por vencimiento de plazo, reingreso o decisión administrativa
- qué acciones están habilitadas
- si el fallo ya fue producido
- cuál será el siguiente paso una vez generado

---

## Idea clave

Esta bandeja no existe para “tomar un expediente y ver si algún día necesita fallo”.

Existe para expedientes que ya están en condición concreta de requerir fallo y todavía no lo tienen producido.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de análisis / presentaciones / pagos](03-bandeja-analisis-presentaciones-pagos.md)
- [Bandeja de pendientes de resolución / redacción](04-bandeja-pendientes-resolucion-redaccion.md)
- [Bandeja de pendientes de firma](06-bandeja-pendientes-firma.md)
- [Bandeja de gestión externa](09-bandeja-gestion-externa.md)
- [Reglas de notificación](../02-reglas-transversales/02-reglas-de-notificacion.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
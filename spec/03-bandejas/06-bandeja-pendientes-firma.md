# 06-bandeja-pendientes-firma.md

## Finalidad

Esta bandeja reúne expedientes que tienen uno o más documentos ya generados y pendientes de firma.

La unidad visible sigue siendo el expediente / acta.

No es una bandeja de documentos sueltos.

---

## Qué contiene

Contiene expedientes que ya tienen piezas documentales generadas, pero que todavía no completaron el tramo de firma necesario para continuar.

Puede incluir, entre otros:

- fallos
- resoluciones
- medidas preventivas
- documentos notificables firmables
- otras piezas documentales que requieran firma

---

## Qué no contiene

No contiene expedientes que solo estén pendientes de generar documentos.

No contiene expedientes que ya tengan todas las firmas necesarias completas.

No contiene expedientes cuyo problema actual sea exclusivamente de notificación.

---

## Función operativa

Su función es permitir identificar rápidamente:

- qué expedientes tienen firmas pendientes
- qué tipo de documentos esperan firma
- si el expediente ya está listo para pasar a la etapa siguiente una vez firmados
- si todavía queda alguna otra condición previa sin resolver

---

## Regla de entrada

Un expediente entra a esta bandeja cuando:

- ya se generó al menos un documento que requiere firma
- y todavía no se completaron todas las firmas necesarias para continuar

Típicamente puede entrar desde:

- enriquecimiento / revisión inicial
- análisis / presentaciones / pagos
- pendientes de resolución / redacción
- pendientes de fallo
- apelación
- reencauces

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras siga existiendo al menos un documento requerido pendiente de firma.

Si existen múltiples documentos firmables asociados al expediente, el expediente debe seguir visible aquí hasta completar el conjunto necesario para continuar.

---

## Regla de salida

La salida de esta bandeja se habilita cuando ya se completaron las firmas necesarias para el siguiente paso operativo.

Ese siguiente paso puede ser, según el caso:

- notificaciones
- archivo
- cerrada
- reencauce a otra bandeja

No existe una única salida obligatoria para todos los expedientes.

---

## Acciones típicas

- visualizar documentos pendientes de firma
- identificar firmante esperado
- registrar o reflejar resultado de firma
- verificar si ya se completó el conjunto documental firmado necesario
- enviar a notificaciones
- enviar a archivo
- enviar a cerradas
- reencauzar a análisis
- paralizar

---

## Relación con múltiples documentos

Un mismo expediente puede tener varios documentos firmables al mismo tiempo.

Por ejemplo:
- dos medidas preventivas
- una resolución adicional
- un documento notificable asociado

El expediente debe seguir visible como expediente con pendientes de firma hasta que se complete el conjunto requerido para continuar.

---

## Relación con el motor de firma

Esta bandeja debe funcionar independientemente de que la firma sea:

- simulada en sandbox
- confirmada por integración externa real

El sistema de faltas no firma por sí mismo.  
Refleja que un documento del expediente quedó pendiente de firma y reacciona al resultado informado por el sistema de firma o por el modo sandbox.

---

## Relación con snapshot

El snapshot debe permitir resumir, al menos:

- si el expediente tiene documentos pendientes de firma
- cuántos pendientes relevantes existen
- si todas las firmas necesarias para continuar ya están completas
- si el expediente ya puede pasar a la siguiente bandeja

---

## Relación con la UI

La UI debe permitir ver con claridad:

- qué expediente está pendiente de firma
- qué documentos faltan firmar
- quién debe firmarlos, si aplica
- si al completar la firma el expediente ya queda listo para notificación, archivo o cierre

---

## Idea clave

No se pasa un expediente a esta bandeja para recién generar documentos.

El expediente aparece aquí porque ya tiene documentos generados cuya firma está pendiente.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de pendientes de fallo](05-bandeja-pendientes-fallo.md)
- [Bandeja de notificaciones](07-bandeja-notificaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de firma](../02-reglas-transversales/01-reglas-de-firma.md)
- [Integración con motor de firma](../09-integraciones/01-integracion-motor-firma.md)
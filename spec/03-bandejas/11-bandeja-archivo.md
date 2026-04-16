# 11-bandeja-archivo.md

## Finalidad

Esta bandeja reúne expedientes cuyo trámite principal ya quedó resuelto o suficientemente avanzado, pero que todavía no pueden pasar a cerradas.

Archivo no equivale a cerrada.

Archivo expresa una situación operativa real previa al cierre en muchos casos.

---

## Qué contiene

Contiene expedientes que ya no requieren continuar el tramo principal del trámite, pero todavía tienen alguna causal operativa que impide el cierre.

Ejemplos típicos:

- multa paga pero medida preventiva activa
- absolución con pendiente material de liberación
- expediente resuelto con documentación o restitución pendiente
- otra causal de archivo operativo definida por catálogo

---

## Qué no contiene

No contiene expedientes completamente concluidos y sin bloqueos de cierre.

No contiene expedientes que todavía estén en pleno tramo principal del trámite sin resolución suficiente.

---

## Función operativa

Su función es permitir ver con claridad:

- qué expedientes ya están resueltos o suficientemente avanzados
- por qué todavía no pueden cerrarse
- qué condición falta para permitir el cierre
- si corresponde reingresar el expediente al circuito

---

## Regla de entrada

Un expediente entra a esta bandeja cuando:

- el trámite principal ya está resuelto o suficientemente avanzado
- pero persiste una causal operativa que impide su pase a cerrada

Típicamente puede entrar desde:

- enriquecimiento / revisión inicial
- análisis / presentaciones / pagos
- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- gestión externa
- paralizadas

---

## Regla de permanencia

El expediente permanece en archivo mientras subsista la causal que impide su cierre.

Esa causal puede estar dada, por ejemplo, por:

- medida preventiva activa
- liberación pendiente
- restitución pendiente
- otro bloqueo operativo equivalente

---

## Regla de salida

El expediente puede salir de archivo cuando:

- desaparece la causal que impedía cerrar
- o surge una actuación que obliga a reingresar el expediente al circuito

### Destinos posibles
- cerradas
- análisis / presentaciones / pagos
- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- paralizadas

---

## Acciones típicas

- visualizar motivo de archivo
- registrar levantamiento de medida
- registrar liberación o restitución material
- reingresar al circuito
- enviar a cerradas
- paralizar

---

## Relación con medidas y liberaciones

Archivo se relaciona especialmente con situaciones como:

- medidas preventivas activas
- levantamientos pendientes
- liberaciones materiales pendientes
- restituciones documentales pendientes

Mientras esas situaciones subsistan, el expediente no debe pasar a cerrada.

La permanencia en archivo puede depender también de pendientes materiales formalmente identificados, cuyo efecto debe ser visible en snapshot cuando bloqueen el pase a cerrada.

---

## Relación con snapshot

El snapshot debe permitir identificar rápidamente:

- si el expediente está en archivo
- por qué está en archivo
- si bloquea cierre
- qué condición falta para poder cerrarlo

---

## Relación con la UI

La UI debe mostrar con claridad:

- motivo de archivo
- existencia de medidas activas
- existencia de pendientes materiales
- si el expediente ya puede pasar a cerrada
- si corresponde reingresar a otra bandeja

---

## Idea clave

Archivo no es un simple depósito documental posterior.

Es una situación operativa real del expediente cuando el trámite principal ya no necesita continuar, pero todavía existen condiciones que impiden el cierre.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de cerradas](12-bandeja-cerradas.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Medidas y liberaciones](../01-dominio/06-medidas-y-liberaciones.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
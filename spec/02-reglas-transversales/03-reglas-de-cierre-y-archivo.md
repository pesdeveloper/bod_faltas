# 03-reglas-de-cierre-y-archivo.md

## Finalidad

Este archivo fija las reglas generales para distinguir correctamente:

- archivo
- cerrada

y para determinar cuándo un expediente puede pasar de una situación a la otra.

---

## Regla principal

**Archivo y cerrada no son equivalentes.**

- **Archivo** = situación operativa de un expediente cuyo trámite principal ya está resuelto o suficientemente avanzado, pero que todavía no puede pasar a cerrada.
- **Cerrada** = situación de un expediente completamente concluido desde el punto de vista operativo.

---

## 1. Regla de archivo

Un expediente puede quedar en **archivo** cuando el trámite principal ya se encuentra resuelto o suficientemente avanzado, pero persiste alguna situación que impide su cierre.

### Ejemplos típicos
- multa paga pero medida preventiva activa
- absolución con pendiente material de liberación
- expediente resuelto con documentación o restitución pendiente
- otra causal operativa definida por catálogo

### Idea clave
Archivo no es un depósito pasivo posterior.  
Es una situación operativa real del expediente.

---

## 2. Regla de cerrada

Un expediente puede pasar a **cerrada** solo cuando ya no existan situaciones operativas que impidan su cierre.

### Ejemplos típicos
- nulidad sin medidas activas
- pago sin medidas activas ni pendientes materiales
- absolución sin medidas activas ni pendientes materiales

### Idea clave
Cerrada expresa conclusión operativa real, no solo resolución jurídica o económica parcial.

---

## 3. Regla de bloqueo de cierre

Un expediente no puede pasar a cerrada mientras exista cualquiera de estas situaciones, entre otras equivalentes:

- una o más medidas preventivas activas
- uno o más pendientes materiales de liberación
- uno o más pendientes materiales de restitución
- otra causal operativa que impida cierre

Estas condiciones deben reflejarse en el snapshot operativo.

---

## 4. Regla de relación entre resolución y cierre

Que el expediente esté resuelto no implica automáticamente que pueda cerrarse.

Puede ocurrir, por ejemplo, que:

- ya exista pago
- ya exista absolución
- ya exista nulidad o resolución principal
- pero todavía queden efectos materiales pendientes

En esos casos, el expediente debe permanecer en archivo hasta que desaparezca la causal que impide el cierre.

---

## 5. Regla de medidas preventivas

Mientras exista al menos una medida preventiva activa, el expediente no puede pasar a cerrada.

Esto aplica aunque el trámite principal ya esté resuelto.

Por lo tanto:
- expediente resuelto + medida activa = archivo
- expediente resuelto + sin medida activa = podría pasar a cerrada, si no existen otros bloqueos

---

## 6. Regla de pendientes materiales

Mientras exista un pendiente material de liberación o restitución, el expediente no puede pasar a cerrada.

Esto aplica, por ejemplo, cuando todavía no se registró efectivamente:

- la liberación de un rodado
- la restitución de documentación retenida
- la devolución de una licencia
- otra entrega material relevante

Por lo tanto:
- expediente resuelto + pendiente material = archivo
- expediente resuelto + sin pendiente material = podría pasar a cerrada, si no existen otros bloqueos

---

## 7. Regla de tránsito a archivo

El pase a archivo ocurre cuando el expediente ya no requiere continuar el tramo principal del trámite, pero todavía no cumple las condiciones de cierre.

El sistema debe permitir identificar claramente el motivo de archivo, por ejemplo:

- medida preventiva activa
- liberación pendiente
- restitución pendiente
- otra causal

---

## 8. Regla de salida de archivo

Un expediente puede salir de archivo cuando:

- desaparece la causal que impedía cerrar
- o surge una actuación que obliga a reingresar el expediente al circuito

### Destinos posibles
- cerrada
- análisis
- resolución / redacción
- fallo
- firma
- notificaciones
- paralizadas

según corresponda por la nueva situación del expediente.

---

## 9. Regla de snapshot

El snapshot operativo debe reflejar, al menos:

- si el expediente está en archivo
- por qué está en archivo
- si bloquea cierre
- si existen medidas activas
- si existen pendientes materiales
- si ya puede pasar a cerrada

La UI debe poder explicar rápidamente por qué un expediente no puede cerrarse.

---

## 10. Regla de UI

La interfaz debe permitir visualizar con claridad:

- si el expediente está archivado
- cuál es el motivo operativo del archivo
- qué condición falta para poder cerrarlo
- si ya está habilitado el pase a cerrada

Esto evita que el operador dependa de reconstruir todo el historial para entender la situación actual.

---

## 11. Regla de trazabilidad

Toda transición relevante entre:

- resuelto
- archivo
- cerrada
- reingreso desde archivo

debe dejar trazabilidad suficiente en el expediente.

La fuente de verdad no debe vivir solo en una bandera visual.

---

## 12. Regla de interpretación general

La lógica correcta es esta:

- resolver no siempre equivale a cerrar
- cerrar exige que no queden efectos operativos pendientes
- archivo cubre el tramo intermedio en el que el expediente ya está resuelto, pero todavía no puede cerrarse

---

## Archivos relacionados

- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Medidas y liberaciones](../01-dominio/06-medidas-y-liberaciones.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
- [Bandeja de archivo](../03-bandejas/11-bandeja-archivo.md)
- [Bandeja de cerradas](../03-bandejas/12-bandeja-cerradas.md)
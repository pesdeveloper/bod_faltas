# 00-indice-maestro-bandejas.md

## Finalidad

Este archivo resume las bandejas operativas vigentes del sistema de faltas.

Las bandejas deben entenderse como **bandejas de expedientes / actas** según su situación documental y procesal actual.

La ubicación operativa de cada expediente en estas bandejas debe poder proyectarse en `ActaSnapshot` mediante código de bandeja, visibilidad operativa y estado procesal actual.

El expediente permanece en su bandeja actual mientras falten documentos o condiciones necesarias para avanzar.  
Cuando el conjunto documental o material requerido queda completo, el sistema habilita la acción de pase a la siguiente bandeja operativa que corresponda.

---

## 01. Bandeja de labradas

Actas recién labradas, todavía en etapa inicial.

Archivo relacionado:
- [01-bandeja-labradas.md](01-bandeja-labradas.md)

---

## 02. Bandeja de enriquecimiento / revisión inicial

Actas en revisión, validación y completitud documental inicial.

El expediente permanece aquí mientras falte generar alguna pieza necesaria para continuar.

Archivo relacionado:
- [02-bandeja-enriquecimiento.md](02-bandeja-enriquecimiento.md)

---

## 03. Bandeja de análisis / presentaciones / pagos

Actas con presentaciones, pagos, solicitudes o novedades que requieren análisis material.

Archivo relacionado:
- [03-bandeja-analisis-presentaciones-pagos.md](03-bandeja-analisis-presentaciones-pagos.md)

---

## 04. Bandeja de pendientes de resolución / redacción

Actas que requieren producir una pieza no-fallo.

Ejemplos típicos:
- resolución
- nulidad
- medida preventiva
- rectificación
- otra pieza administrativa o documental no-fallo

Archivo relacionado:
- [04-bandeja-pendientes-resolucion-redaccion.md](04-bandeja-pendientes-resolucion-redaccion.md)

---

## 05. Bandeja de pendientes de fallo

Actas que ya están en condición de requerir fallo.

Archivo relacionado:
- [05-bandeja-pendientes-fallo.md](05-bandeja-pendientes-fallo.md)

---

## 06. Bandeja de pendientes de firma

Actas con uno o más documentos ya generados pendientes de firma.

No son documentos sueltos.  
Son expedientes con pendientes de firma.

Archivo relacionado:
- [06-bandeja-pendientes-firma.md](06-bandeja-pendientes-firma.md)

---

## 07. Bandeja de notificaciones

Concentra todo el proceso transversal de notificación.

Aplica a cualquier pieza notificable del expediente, por ejemplo:
- acta
- medida preventiva
- acto administrativo
- fallo
- otra pieza documental formal que requiera notificación

Archivo relacionado:
- [07-bandeja-notificaciones.md](07-bandeja-notificaciones.md)

---

## 08. Bandeja con apelación

Actas en las que efectivamente existe apelación o recurso equivalente.

Archivo relacionado:
- [08-bandeja-con-apelacion.md](08-bandeja-con-apelacion.md)

---

## 09. Bandeja de gestión externa

Actas derivadas a gestión externa formal.

Ejemplos:
- apremios
- Juzgado de Paz
- otra gestión externa

Archivo relacionado:
- [09-bandeja-gestion-externa.md](09-bandeja-gestion-externa.md)

---

## 10. Bandeja de paralizadas

Actas detenidas fundadamente.

No implica cierre ni archivo.

Archivo relacionado:
- [10-bandeja-paralizadas.md](10-bandeja-paralizadas.md)

---

## 11. Bandeja de archivo

Actas cuyo trámite principal ya quedó resuelto o suficientemente avanzado, pero que todavía no pueden pasar a cerradas.

Ejemplos:
- multa paga pero medida preventiva activa
- absolución con pendiente material de liberación
- expediente resuelto con documentación o restitución aún pendiente
- otros archivos operativos según catálogo

Archivo relacionado:
- [11-bandeja-archivo.md](11-bandeja-archivo.md)

---

## 12. Bandeja de cerradas

Actas que ya cumplieron completamente las condiciones de cierre.

Ejemplos:
- nulidad sin medidas activas
- pago sin medidas activas ni pendientes materiales
- absolución sin medidas activas ni pendientes materiales

Archivo relacionado:
- [12-bandeja-cerradas.md](12-bandeja-cerradas.md)

---

## Absorciones relevantes del esquema anterior

Quedan absorbidas o eliminadas como bandejas independientes las piezas del esquema anterior vinculadas a:

- preparación separada para notificación del acta
- actas listas para notificar
- notificación del acta en proceso
- pendiente de notificación de acto
- notificación de acto en proceso
- fallo pendiente de notificación
- fallos listos para notificar
- notificación de fallo en proceso

Su lógica pasa principalmente a:

- [02-bandeja-enriquecimiento.md](02-bandeja-enriquecimiento.md)
- [06-bandeja-pendientes-firma.md](06-bandeja-pendientes-firma.md)
- [07-bandeja-notificaciones.md](07-bandeja-notificaciones.md)

---

## Archivos relacionados

- [Mapa de dominio](../01-dominio/00-mapa-dominio.md)
- [Regla del sistema como gestor documental](../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
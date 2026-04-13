# 10-bandeja-paralizadas.md

## Finalidad

Esta bandeja reúne expedientes detenidos fundadamente.

No representa cierre, archivo ni finalización del trámite.

Su función es dar visibilidad a los casos cuyo avance quedó suspendido por una causal operativa válida.

---

## Qué contiene

Contiene expedientes cuyo trámite se encuentra detenido por una decisión o causal fundada.

La paralización implica que el expediente no continúa su curso normal mientras subsista esa causal.

---

## Qué no contiene

No contiene expedientes cerrados.

No contiene expedientes archivados solo por bloqueo de cierre.

No contiene expedientes simplemente inactivos o sin movimiento, si no existe una causal formal de paralización.

---

## Función operativa

Su función es permitir:

- identificar expedientes detenidos
- conocer la causal general de paralización
- distinguirlos de archivo y cerrada
- decidir su reanudación o reencauce cuando corresponda

---

## Regla de entrada

Un expediente entra a esta bandeja cuando se toma una decisión fundada de paralización desde cualquier otra bandeja.

La paralización debe tener trazabilidad suficiente y no debe usarse como simple “espera informal”.

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras siga vigente la causal que motivó la paralización.

Mientras esa causal no sea levantada, el expediente no debe continuar su recorrido normal.

---

## Regla de salida

La salida de esta bandeja se habilita cuando:

- se levanta la causal de paralización
- existe decisión formal de reanudación
- o una decisión válida redefine la situación del expediente

Destinos posibles:

- análisis / presentaciones / pagos
- pendientes de resolución / redacción
- pendientes de fallo
- pendientes de firma
- notificaciones
- gestión externa
- archivo
- cerradas

El destino dependerá de la situación real del expediente al momento de la reanudación.

---

## Acciones típicas

- visualizar motivo de paralización
- levantar paralización
- reencauzar a la bandeja correspondiente
- enviar a archivo
- enviar a cerradas, si una decisión excepcional válida lo permitiera

---

## Relación con archivo

Paralizada no equivale a archivo.

- **archivo** = expediente resuelto o suficientemente avanzado, pero no cerrable todavía
- **paralizada** = expediente detenido fundadamente, sin implicar por sí mismo resolución ni conclusión del trámite

---

## Relación con snapshot

El snapshot debe permitir identificar, al menos:

- que el expediente está paralizado
- cuál es el motivo general de paralización
- si la causal ya fue levantada o no
- si ya existe habilitación para reencauzarlo

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- que el expediente está paralizado
- por qué está paralizado
- qué acción permite levantar la paralización
- cuál podría ser el siguiente destino al reanudarse

---

## Idea clave

La paralización no debe usarse como sinónimo de espera pasiva ni de cierre encubierto.

Es una situación excepcional y fundada del expediente, con efecto real sobre su continuidad.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de gestión externa](09-bandeja-gestion-externa.md)
- [Bandeja de archivo](11-bandeja-archivo.md)
- [Bandeja de cerradas](12-bandeja-cerradas.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
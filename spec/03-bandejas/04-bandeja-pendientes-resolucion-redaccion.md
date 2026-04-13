# 04-bandeja-pendientes-resolucion-redaccion.md

## Finalidad

Esta bandeja reúne expedientes que requieren producir una pieza no-fallo.

Su función es concentrar los casos donde el expediente necesita una decisión o documento administrativo distinto del fallo, con efecto operativo sobre su continuidad.

---

## Qué contiene

Contiene expedientes que requieren producir una o más piezas como, por ejemplo:

- resolución
- nulidad
- medida preventiva
- rectificación
- otra pieza administrativa o documental no-fallo

---

## Qué no contiene

No contiene expedientes cuya necesidad actual sea exclusivamente:

- completar revisión inicial documental
- esperar firma de documentos ya generados
- gestionar notificación
- tratar un fallo
- permanecer en archivo por bloqueo de cierre

Tampoco contiene, en principio, expedientes ya resueltos que solo esperan levantamiento de medida o liberación material para cerrar.  
Esos casos se ven mejor en archivo.

---

## Función operativa

Su función es permitir que el expediente quede visible cuando ya se sabe que necesita una pieza no-fallo, pero esa pieza todavía no fue producida.

Desde aquí se decide o redacta la pieza correspondiente y, según el resultado, el expediente puede pasar a:

- pendientes de firma
- notificaciones
- archivo
- cerradas
- otra bandeja, si una nueva situación lo exige

---

## Regla de entrada

Un expediente entra a esta bandeja cuando ya existe una decisión suficientemente clara de que necesita una pieza no-fallo específica.

Puede ingresar, por ejemplo, desde:

- enriquecimiento / revisión inicial
- análisis / presentaciones / pagos
- apelación
- reencauces
- notificaciones, si el resultado de la notificación obliga a producir una nueva pieza no-fallo

---

## Regla de permanencia

El expediente permanece en esta bandeja mientras la pieza requerida todavía no haya sido producida.

No debe permanecer aquí si el documento ya fue generado y el problema actual pasó a ser de firma o de notificación.

---

## Regla de salida

La salida de esta bandeja se habilita cuando la pieza requerida ya fue generada.

El siguiente destino depende de su efecto y de si requiere firma o no.

Destinos típicos:

- pendientes de firma, si la pieza requiere firma
- notificaciones, si la pieza ya quedó firmada y debe notificarse
- archivo, si el expediente quedó resuelto pero no puede cerrarse
- cerradas, si ya cumple completamente las reglas de cierre
- paralizadas, si corresponde detención fundada

---

## Acciones típicas

- redactar resolución
- redactar nulidad
- generar medida preventiva
- generar rectificación
- generar otra pieza no-fallo
- enviar a pendientes de firma
- enviar a notificaciones
- enviar a archivo
- enviar a cerradas
- paralizar

---

## Relación con medidas preventivas

Esta bandeja puede recibir expedientes que requieren generar una o más medidas preventivas.

Mientras esas piezas no existan, el expediente puede seguir visible aquí o en enriquecimiento, según la lógica operativa que se termine de consolidar.

Una vez generadas, si requieren firma, el expediente debe pasar a pendientes de firma.

---

## Relación con archivo

No debe usarse esta bandeja para expedientes ya resueltos cuya única cuestión pendiente sea:

- levantamiento de medida
- liberación material
- restitución pendiente
- otra causal que solo impida cierre

Esos expedientes se ven mejor en archivo.

---

## Relación con snapshot

El snapshot debe permitir identificar, al menos:

- que el expediente requiere una pieza no-fallo
- qué tipo general de pieza se espera
- si la pieza ya fue generada o no
- si el expediente ya puede pasar a firma, notificación, archivo o cierre

---

## Relación con la UI

La UI de esta bandeja debe permitir ver con claridad:

- qué expediente requiere una pieza no-fallo
- qué tipo de decisión o documento se espera
- qué acciones están habilitadas
- si la pieza ya fue producida
- cuál sería el siguiente paso una vez generada

---

## Idea clave

Esta bandeja existe para expedientes que necesitan producir una pieza no-fallo.

No existe para expedientes que ya tienen esa pieza generada y solo esperan firma, notificación o cierre.

---

## Archivos relacionados

- [Índice maestro de bandejas](00-indice-maestro-bandejas.md)
- [Bandeja de análisis / presentaciones / pagos](03-bandeja-analisis-presentaciones-pagos.md)
- [Bandeja de pendientes de fallo](05-bandeja-pendientes-fallo.md)
- [Bandeja de pendientes de firma](06-bandeja-pendientes-firma.md)
- [Bandeja de archivo](11-bandeja-archivo.md)
- [Medidas y liberaciones](../01-dominio/06-medidas-y-liberaciones.md)
- [Snapshot operativo](../01-dominio/05-snapshot-operativo.md)
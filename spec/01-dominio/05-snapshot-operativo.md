# 05-snapshot-operativo.md

## Finalidad

Este archivo define el propósito y alcance del `SnapshotOperativo` del expediente.

El snapshot resume la situación operativa vigente de la Acta para facilitar:

- bandejas
- filtros
- badges
- bloqueos
- habilitación de acciones
- lectura rápida

No es fuente de verdad.

---

## Regla principal

El `SnapshotOperativo` es **derivado y regenerable**.

La fuente de verdad sigue viviendo en:

- Acta
- ActaEvento
- Documento
- Notificacion
- medidas y liberaciones
- demás piezas transaccionales del dominio

El snapshot existe para resumir el estado operativo relevante del expediente.

---

## Qué debe resumir

El snapshot debe resumir, al menos, estas dimensiones del expediente:

- situación operativa general
- bandeja visible actual
- pendientes documentales
- pendientes de firma
- situación de notificación
- bloqueos de avance
- bloqueos de cierre
- medidas preventivas
- pendientes materiales de liberación o restitución
- condiciones de archivo o cierre
- si el expediente tiene notificación de acta
- si tiene notificación de medida preventiva
- si tiene notificación de fallo o acto administrativo
- si alguna de esas notificaciones está en proceso o con acuse pendiente
- cantidad de reintentos de notificación
- si tiene solicitud de pago voluntario
- si tiene pago total
- si tiene plan de pagos, cantidad de cuotas y valor de cuota
- si está en gestión externa, su tipo y resultado resumido
- plazos operativos relevantes, como presentación, apelación o derivación
- fecha del acta, dependencia e inspector para filtros rápidos

---

## Para qué se usa

El snapshot se usa principalmente para:

- listar expedientes en bandejas
- aplicar filtros rápidos
- mostrar badges o indicadores
- habilitar o bloquear acciones
- evitar recalcular toda la historia del expediente en cada pantalla
- resumir situación documental y procesal vigente

---

## Qué no debe hacer

El snapshot no debe:

- reemplazar la fuente de verdad
- almacenar historia completa
- duplicar detalle documental innecesario
- guardar el contenido jurídico de documentos
- sustituir eventos, documentos o notificaciones

Debe limitarse al resumen operativo necesario para trabajar.

---

## Grupos mínimos de información

### 1. Identificación operativa
Debe permitir identificar rápidamente el expediente y su situación general.

### 2. Bandeja y estado visible
Debe reflejar en qué bandeja debe aparecer el expediente y cuál es su situación operativa resumida.

### 3. Pendientes documentales
Debe indicar si existen documentos pendientes de generar, firmar o tratar.

### 4. Notificaciones
Debe resumir si existen piezas en notificación, con acuse pendiente, positivo, negativo o situación equivalente relevante.

### 5. Medidas preventivas
Debe resumir existencia, cantidad, activas, levantadas y pendientes de firma cuando corresponda.

### 6. Pendientes materiales
Debe resumir secuestros, retenciones, liberaciones o restituciones materiales pendientes que impacten en la operatoria o en el cierre.

### 7. Bloqueos y habilitaciones
Debe reflejar si el expediente:
- bloquea notificación
- bloquea cierre
- puede pasar a otra bandeja
- requiere intervención previa

---

## Regla de bloqueo de notificación

Si el expediente requiere documentos previos para continuar y alguno de ellos todavía no fue generado o firmado, el snapshot debe reflejar que la notificación no puede avanzar.

Esto impacta especialmente cuando existan medidas preventivas u otras piezas requeridas pendientes.

---

## Regla de bloqueo de cierre

El snapshot debe reflejar que un expediente no puede pasar a cerrada mientras existan, por ejemplo:

- medidas preventivas activas
- pendientes materiales de liberación o restitución
- otras causales operativas que impidan cierre

Esto es clave para distinguir correctamente entre:

- archivo
- cerrada

---

## Regla de archivo

El snapshot debe poder reflejar que un expediente ya está resuelto o suficientemente avanzado, pero aun así no puede cerrarse.

En ese caso, el expediente puede quedar visible en archivo mientras persista la causal que impide el cierre.

---

## Regla de regeneración

Si el snapshot resultara inconsistente, debe poder reconstruirse a partir de la fuente de verdad del sistema.

Por eso:

- no debe contener lógica irrecuperable
- no debe ser el único lugar donde viva un dato crítico
- no debe convertirse en un segundo modelo de verdad paralelo

---

## Relación con las bandejas

Las bandejas consumen el snapshot como resumen operativo del expediente.

La visibilidad del expediente en una bandeja determinada debe poder explicarse a partir de:

- situación documental
- pendientes de firma
- estado de notificación
- bloqueos
- medidas activas
- pendientes materiales
- condiciones de archivo o cierre

---

## Relación con medidas y liberaciones

El snapshot debe resumir especialmente:

- si existen medidas preventivas
- cuántas existen
- cuántas están activas
- cuántas están levantadas
- si existen pendientes de firma asociados
- si existen pendientes materiales de liberación o restitución
- si esas situaciones bloquean notificación o cierre

El detalle fino de esas piezas no vive en el snapshot.

El snapshot solo resume su efecto operativo.

---

## Relación con la UI

La UI debe usar el snapshot principalmente para:

- mostrar listados
- destacar badges
- decidir qué acciones están habilitadas
- informar bloqueos
- ordenar expedientes por situación operativa

Cuando el usuario necesite detalle, la UI debe consultar las piezas fuente correspondientes y no depender solo del snapshot.

---

## Regla de tamaño y responsabilidad

El snapshot debe mantenerse como resumen operativo.

Cuando un dato solo tenga sentido como detalle histórico, jurídico o documental, debe vivir fuera del snapshot y en su entidad fuente correspondiente.

---

## Archivos relacionados

- [Mapa de dominio](00-mapa-dominio.md)
- [Medidas y liberaciones](06-medidas-y-liberaciones.md)
- [Regla del sistema como gestor documental](../02-reglas-transversales/00-regla-sistema-como-gestor-documental.md)
- [Reglas de cierre y archivo](../02-reglas-transversales/03-reglas-de-cierre-y-archivo.md)
- [Índice maestro de bandejas](../03-bandejas/00-indice-maestro-bandejas.md)
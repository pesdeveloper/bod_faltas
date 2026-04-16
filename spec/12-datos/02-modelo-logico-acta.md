# Modelo lógico — Acta

## Finalidad

`Acta` representa la unidad principal de gestión del sistema.

Es el expediente base del caso y concentra:

- identidad interna
- identidad técnica distribuida
- numeración administrativa visible
- contexto de labrado
- contexto territorial del hecho
- referencia a actores del dominio
- vínculo con satélites, documentos, notificaciones y eventos

---

## Qué guarda

El modelo de `Acta` debe poder guardar, según corresponda:

- identificador interno
- identificador técnico distribuido
- número visible del acta
- tipo de acta
- dependencia e inspector
- fecha/hora del acta
- domicilio de la infracción preferentemente normalizado
- domicilio del infractor, preferentemente normalizado
- observaciones propias del acta
- coordenadas GPS de la infracción cuando existan
- datos propios del expediente que no pertenecen a satélites específicos

---

## Qué no guarda

`Acta` no debe usarse para guardar:

- historia completa del expediente
- notificaciones completas
- documentos completos
- firma
- snapshot operativo
- medidas o liberaciones completas
- evidencias embebidas como binarios
- satélites especializados que merecen entidad propia

---

## Reglas principales

- `Acta` es la unidad principal de gestión.
- La numeración visible no reemplaza identidad interna ni técnica.
- `Acta` pertenece a una dependencia.
- `Acta` queda asociada al inspector que la captó o labró.
- El domicilio de la infracción debe tender a ser normalizado.
- Las coordenadas GPS complementan el domicilio, no lo reemplazan.
- Las evidencias del acta deben quedar en entidad satélite propia.

---

## Relación con el territorio

`Acta` debe poder representar el lugar del hecho mediante:

- calle y altura normalizadas, cuando existan
- localidad y barrio derivados, cuando correspondan
- indicador de eje urbano, si aplica
- coordenadas GPS (`LatInfr`, `LonInfr`) cuando la captura lo permita

La georreferenciación no reemplaza la lógica territorial ni los catálogos base.

---

## Relación con satélites

`Acta` debe poder vincularse con satélites especializados, por ejemplo:

- tránsito
- alcoholemia
- contravención
- sustancias alimenticias
- vehículo
- medida preventiva aplicada
- evidencias del acta

Los satélites no reemplazan a `Acta` como núcleo del expediente.

---

## Relación con documentos y notificaciones

`Acta` puede tener múltiples documentos y múltiples notificaciones a lo largo de su vida.

Esos elementos forman parte del expediente, pero no deben confundirse con la entidad principal `Acta`.

---

## Relación con snapshot

El estado operativo actual del expediente debe poder proyectarse en `ActaSnapshot`, sin convertir a `Acta` en snapshot embebido.

---

## Idea clave

`Acta` es el centro del expediente.

Todo lo demás gira alrededor suyo, pero no debe absorber historia, documentos, notificaciones, firma, evidencias o snapshot de manera desordenada.
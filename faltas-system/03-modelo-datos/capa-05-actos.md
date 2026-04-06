# [CAPA 05] ACTOS / DECISIÓN ADMINISTRATIVA FORMAL

## Finalidad de la capa

Esta capa modela el **acto administrativo formal** dictado sobre una causa.

Representa decisiones como:

- fallo
- resolución
- disposición

---

## Qué representa esta capa

Esta capa representa el **acto en sí mismo** como pieza formal de la causa.

No representa:

- el análisis (D4)
- el flujo D1–D8
- la notificación
- la firma
- el documento como archivo
- el resultado económico

---

## Principios de diseño

### 1. Entidad única

Se utiliza una única entidad:

- `ActaActo`

No se crean tablas separadas por tipo.

---

### 2. TipoActo define la naturaleza formal

Se utiliza un único campo tipificado:

- FALLO
- RESOLUCION
- DISPOSICION

Este campo define la **naturaleza formal del acto**.

No se usa para expresar:

- contexto recursivo
- materia del acto
- efectos
- etapa del flujo

---

### 3. El acto no es el documento

- el acto = decisión formal
- el documento = representación del acto

Se vinculan, pero no se mezclan.

---

### 4. La firma no pertenece a esta capa

La firma se resuelve en:

- `Documento`
- `DocumentoFirma`

👉 La validez del acto depende del documento firmado.

---

### 5. Sin workflow propio

El acto no tiene ciclo de vida complejo.

Solo se registra su existencia y su estado mínimo.

---

## ActaActo

Entidad central de la capa.

Representa un acto administrativo formal vinculado a una causa.

Ejemplos:

- fallo
- resolución
- disposición

---

## Datos mínimos esperados

`ActaActo` debe permitir registrar:

- a qué acta pertenece
- qué tipo de acto es
- cuál es su documento principal
- cuál es su estado básico
- cuándo fue registrado
- quién lo registró

---

## Tipo de acto

Valores admitidos:

- FALLO
- RESOLUCION
- DISPOSICION

---

## Estado mínimo del acto

El acto distingue únicamente:

- EMITIDO
- ANULADO

No se modelan estados como:

- BORRADOR
- REEMPLAZADO
- FIRMADO / NO FIRMADO

👉 La firma no se modela aquí.

---

## Relación con Documento

Cada acto se vincula a un documento principal.

- `ActaActo` → `Documento`

El documento:

- puede tener firma
- puede ser notificado
- puede tener numeración

Pero sigue siendo una entidad separada.

---

## Relación con firma

La firma se gestiona en:

- `Documento`
- `DocumentoFirma`

No se duplica en esta capa.

---

## Relación con notificación

La notificación se resuelve en Capa 03.

Esta capa no modela:

- envíos
- destinatarios
- resultados
- acuses

---

## Relación con el modelo event-driven

Los actos deben reflejarse en:

- `ActaEvento`

Ejemplos:

- ACTO_EMITIDO
- FALLO_EMITIDO
- RESOLUCION_EMITIDA
- DISPOSICION_EMITIDA
- ACTO_ANULADO

---

## Relación con D4 y D5

- D4 → análisis
- D5 → decisión

👉 Capa 05 registra el resultado formal de esa decisión.

---

## Lo que la capa NO debe hacer

No debe:

- modelar análisis
- modelar apelaciones como tipo de acto
- modelar medidas como tipo de acto
- modelar reemplazo entre actos
- modelar vigencia lógica
- modelar firma
- modelar notificación
- modelar efectos económicos

---

## Reglas de diseño

### 1. Un acto pertenece a una acta

`ActaActo` → 1 `Acta`

---

### 2. Una acta puede tener múltiples actos

Ejemplo:

- disposición
- resolución
- fallo

---

### 3. Todo acto tiene tipo formal

Debe existir `TipoActo`.

---

### 4. Todo acto tiene documento principal

Debe existir vínculo con `Documento`.

---

### 5. La validez depende de la firma del documento

No del estado del acto.

---

### 6. No hay reemplazo entre actos

Puede haber actos nuevos, pero no relación formal de reemplazo.

---

## Qué debe responder la capa

- qué acto existe
- de qué tipo es
- sobre qué acta recae
- cuál es su documento principal
- cuál es su estado básico
- cuándo fue registrado
- quién lo registró

---

## Entidad conceptual

La capa se resuelve con:

- `ActaActo`

Sin necesidad de tablas adicionales.

---

## Regla clave

👉 Capa 05 registra la **decisión administrativa formalizada**.

---

## Cierre

Esta capa incorpora al sistema el concepto de:

👉 **acto administrativo formal**

y completa el modelo junto con:

- Acta (núcleo)
- Documento
- Notificación
- Presentación
- Evento
# [CAPA 08] APREMIO / DERIVACIÓN EXTERNA / RESULTADO EXTERNO

## Finalidad de la capa

Esta capa modela el tramo externo de la causa cuando sale del circuito administrativo principal.

Debe permitir registrar, de forma estructurada:

- derivación a apremio
- derivación al Juzgado de Paz
- seguimiento básico del trámite externo
- recepción de resultado externo
- cierre del tramo externo

---

## Qué representa esta capa

Representa el **seguimiento estructurado de la derivación externa** de una causa.

No representa:

- actos administrativos (Capa 05)
- documentos (Capa 02)
- notificaciones (Capa 03)
- recurso (Capa 07)
- detalle completo del sistema externo

👉 Solo registra el tramo externo como objeto de seguimiento.

---

## Principios de diseño

### 1. Entidad única

Se utiliza una sola entidad:

- `ActaDerivacionExterna`

No se crean tablas separadas para:

- apremio
- juzgado
- resultado externo
- cierre externo

---

### 2. No duplicar sistemas externos

La capa no modela el sistema de apremio ni el sistema judicial externo.

Solo registra:

- a dónde fue derivada la causa
- en qué estado está
- qué resultado externo volvió

---

### 3. Modelo simple

Debe permitir responder:

- si la causa fue derivada
- a qué destino externo fue derivada
- cuándo se derivó
- en qué estado está
- qué resultado volvió
- si cerró

---

## ActaDerivacionExterna

Entidad central de la capa.

Representa una derivación externa vinculada a una causa.

---

## Relación con Acta

- `Acta` 1 → N `ActaDerivacionExterna`

Aunque normalmente no será frecuente, el modelo no debe impedir más de una derivación a lo largo de la vida de la causa.

---

## Relación con Capa 05

Puede haber actos previos o posteriores vinculados con la derivación externa o con su resultado.

Eso sigue viviendo en Capa 05.

👉 Capa 08 no duplica actos.

---

## Relación con Capa 02

Puede haber documentos asociados, por ejemplo:

- constancia de derivación
- oficio
- remisión
- resolución o resultado externo
- constancia de cierre

Eso sigue viviendo en Capa 02.

👉 Capa 08 no duplica documentos.

---

## Relación con Capa 03

Si la derivación o su resultado requiere notificación, eso se resuelve en Capa 03.

👉 Capa 08 no modela notificaciones.

---

## Tipo de derivación externa

Campo tipificado simple.

Valores iniciales:

- APREMIO
- JUZGADO_PAZ

---

## Estado de la derivación externa

Estados mínimos:

- DERIVADO
- EN_GESTION_EXTERNA
- RESULTADO_RECIBIDO
- CERRADO

Sirven para lectura operativa rápida.

No reemplazan eventos.

---

## Resultado de la derivación externa

La capa debe poder registrar **qué resultado volvió** desde afuera.

Ejemplos iniciales:

- SIN_RESULTADO_AUN
- PAGO_EN_APREMIO
- CONFIRMA_FALLO
- MODIFICA_FALLO
- ANULA_FALLO
- OTRO_RESULTADO_EXTERNO

Esto permite distinguir, por ejemplo:

- pago realizado en apremio
- retorno desde Juzgado de Paz confirmando el fallo
- retorno modificando el fallo
- retorno anulando el fallo

---

## Flujo conceptual

```text
Acta → Derivación externa (DERIVADO)
     → EN_GESTION_EXTERNA
     → RESULTADO_RECIBIDO
     → CERRADO
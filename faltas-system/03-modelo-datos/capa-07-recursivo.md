# [CAPA 07] TRAMO RECURSIVO / ELEVACIÓN / RESULTADO EXTERNO

## Finalidad de la capa

Esta capa modela el **tramo recursivo de la causa**, incluyendo:

- interposición del recurso
- admisibilidad o improcedencia
- concesión
- elevación a órgano externo
- retorno con resultado
- cierre del tramo recursivo

---

## Qué representa esta capa

Representa el **seguimiento estructurado del recurso** dentro de la causa.

No representa:

- actos administrativos (Capa 05)
- documentos (Capa 02)
- notificaciones (Capa 03)
- presentaciones (Capa 04)
- el análisis general (D4)

👉 Representa únicamente el **trámite recursivo como objeto de negocio**.

---

## Principios de diseño

### 1. Entidad única

Se utiliza una sola entidad:

- `ActaRecurso`

No se crean tablas separadas para:

- apelación
- concesión
- elevación
- resultado

---

### 2. No duplicar capas existentes

El tramo recursivo se apoya en:

- Documento → Capa 02
- Notificación → Capa 03
- Presentación → Capa 04
- Acto → Capa 05
- Evento → Capa 01

👉 No se duplica nada en esta capa.

---

### 3. Modelo simple

Debe permitir responder:

- si hay recurso
- cuándo se interpuso
- en qué estado está
- si fue elevado
- si volvió con resultado
- cuándo cerró

Sin modelar el sistema judicial completo.

---

## ActaRecurso

Entidad central de la capa.

Representa un tramo recursivo vinculado a una causa.

---

## Relación con Acta

- `Acta` 1 → N `ActaRecurso`

Aunque normalmente habrá uno solo, el modelo no lo limita.

---

## Relación con Capa 04

El recurso puede ingresar como:

- descargo
- presentación
- escrito

Eso vive en Capa 04.

👉 Capa 07 comienza cuando ese ingreso se transforma en **trámite recursivo formal**.

---

## Relación con Capa 05

Puede haber actos como:

- concesión
- improcedencia
- fallo posterior

Pero esos actos siguen en Capa 05.

👉 Capa 07 no los duplica.

---

## Relación con órgano externo

Debe permitir identificar:

- si el recurso quedó interno
- si fue elevado
- si retornó con resultado

Sin modelar el organismo externo completo.

---

## Tipo de recurso

Campo tipificado simple:

- APELACION

(No se sobre-dimensiona hasta que haya más tipos reales)

---

## Estado del tramo recursivo

Estados mínimos necesarios:

- INTERPUESTO
- IMPROCEDENTE
- CONCEDIDO
- ELEVADO
- RESULTADO_RECIBIDO
- CERRADO

👉 Sirve para lectura operativa, no reemplaza eventos.

---

## Flujo conceptual

```text
Presentación → ActaRecurso (INTERPUESTO)
             → (IMPROCEDENTE | CONCEDIDO)
             → (si concedido → ELEVADO)
             → RESULTADO_RECIBIDO
             → CERRADO
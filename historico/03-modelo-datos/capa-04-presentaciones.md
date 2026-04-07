# [CAPA 04] PRESENTACIONES E INTERACCIÓN ADMINISTRATIVA

## Finalidad de la capa

Esta capa modela el **ingreso administrativo de información hacia la causa (Acta)**.

Representa acciones como:

- comparecencia espontánea
- descargo
- constitución / rectificación / ratificación de domicilio
- aporte documental
- solicitudes (pago voluntario, prórroga, vista, copia, etc.)
- notas administrativas
- manifestaciones
- presentaciones de terceros

---

## Qué es una presentación

Una **presentación** es un acto de ingreso administrativo.

Ejemplos:

- un ciudadano se presenta y deja constancia
- se carga un descargo
- se presenta documentación
- se solicita algo
- se constituye un domicilio

---

## Qué NO es esta capa

Esta capa NO modela:

- estados del flujo D1–D8
- resoluciones o decisiones
- notificaciones
- documentos (solo los vincula)
- resultados económicos
- workflow interno

---

## Principios de diseño

### 1. Entidad única

Se usa una sola entidad:

- `ActaPresentacion`

No hay tablas por tipo (descargo, solicitud, etc.).

---

### 2. Sin workflow propio

La presentación:

- se registra
- se clasifica
- se vincula (documentos / domicilio)

Su evolución se refleja en:

- `ActaEvento`
- documentos
- flujo D1–D8

---

### 3. Reutilización de componentes

- Documentos → `Documento` (Capa 02)
- Domicilio → `ActaDomicilio` (Capa 01)

No se duplican datos.

---

### 4. Modelo simple

Cada presentación debe responder:

- qué se presentó
- en qué acta
- quién lo hizo
- cuándo se registró
- por qué canal
- si tiene documentos
- si refiere a domicilio

---

## Entidades de la capa

- `ActaPresentacion`
- `ActaPresentacionDocumento`
- `ActaPresentacionObservacion`

---

## ActaPresentacion

Entidad central de la capa.

Representa una presentación concreta.

### Datos clave

- IdActa
- TipoPresentacion
- CanalPresentacion
- CaracterPresentante
- FechaRegistro
- UsuarioRegistro
- ResumenPresentacion (opcional)
- IdActaDomicilio (opcional)

---

## Tipos de presentación

Se utiliza un único campo tipificado:

Ejemplos:

- COMPARECENCIA_ESPONTANEA
- DESCARGO
- CONSTITUCION_DOMICILIO
- RATIFICACION_DOMICILIO
- RECTIFICACION_DOMICILIO
- APORTE_DOCUMENTAL
- SOLICITUD_PAGO_VOLUNTARIO
- SOLICITUD_PRORROGA
- SOLICITUD_VISTA
- SOLICITUD_COPIA
- NOTA_ADMINISTRATIVA
- MANIFESTACION
- PRESENTACION_TERCERO
- OTRO_CONTROLADO

---

## Canal de ingreso

Solo dos valores:

- PRESENCIAL
- DIGITAL

---

## Presentante

Se tipifica el carácter:

- INFRACTOR
- TITULAR
- APODERADO
- ABOGADO
- TERCERO_INTERESADO
- AGENTE_MUNICIPAL
- AUTORIDAD
- OTRO_CONTROLADO

---

## Relación con Documento

Una presentación puede tener:

- 0..N documentos

Se vincula mediante:

- `ActaPresentacionDocumento`

---

## Relación con Domicilio

Opcional:

- 0..1 `ActaDomicilio`

Solo cuando la presentación refiere a domicilio.

---

## Observaciones

Notas breves sobre la presentación:

- se registran en `ActaPresentacionObservacion`
- no reemplazan eventos ni documentos

---

## Relación con el modelo event-driven

Cuando una presentación es relevante:

- debe reflejarse en `ActaEvento`

Ejemplos:

- DESCARGO_REGISTRADO
- COMPARECENCIA_REGISTRADA
- DOMICILIO_CONSTITUIDO
- DOCUMENTACION_AGREGADA

---

## Regla clave de la capa

👉 La presentación **solo registra el ingreso**.

No decide, no resuelve, no notifica.

---

## Cierre

Capa 04 es una capa:

- simple
- transversal
- reutilizable
- sin lógica compleja

Su función es clara:

👉 **capturar lo que entra a la causa**
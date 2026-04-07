# [CAPA 08] — DERIVACIÓN EXTERNA CONSOLIDADA

---

# 1. FINALIDAD DE LA CAPA

Esta capa modela la **derivación del Acta hacia un sistema o instancia externa**.

Incluye:

* apremio
* juzgado de paz

---

# 2. PRINCIPIO CENTRAL

La derivación externa:

## no se modela como agregado independiente

Se representa mediante:

## ActaEvento + Documento + Snapshot

---

# 3. MODELO DE DERIVACIÓN

## 3.1 Evento

La derivación y sus resultados se registran en `ActaEvento`.

---

### Ejemplos de eventos

* DERIVACION_APREMIO
* DERIVACION_JUZGADO_PAZ
* RESULTADO_EXTERNO_RECIBIDO

---

## Regla

Solo se registran:

* hechos efectivos
* resultados relevantes

No se registran:

* preparaciones internas
* procesos administrativos intermedios
* operaciones técnicas

---

## 3.2 Documento (opcional)

Si existen documentos asociados:

* se modelan mediante Documento
* se vinculan mediante ActaDocumento

---

### Ejemplos

* constancias de derivación
* documentos judiciales
* resoluciones externas
* comprobantes

---

## 3.3 Snapshot

El sistema puede proyectar:

* DerivadoAApremio
* DerivadoAJuzgado
* ResultadoExternoIncorporado

---

# 4. DATOS DE LA DERIVACIÓN

Los datos relevantes se distribuyen así:

## En el evento

* tipo de derivación
* fecha
* destino
* resultado (si aplica)
* observación resumida

---

## En el documento (si existe)

* contenido completo
* resolución externa
* constancia

---

## En el snapshot

* estado actual del Acta respecto a la derivación

---

# 5. REGLAS DE DISEÑO

## 5.1 No crear entidad ActaDerivacionExterna

La derivación no se modela como tabla independiente.

---

## 5.2 No duplicar información

Evitar repetir datos entre:

* evento
* documento
* snapshot

---

## 5.3 Mantener simplicidad

La derivación debe poder leerse como:

* un evento
* opcionalmente un documento
* una proyección actual

---

# 6. RELACIÓN CON EL PROCESO

La derivación:

* puede sacar temporalmente el Acta del sistema
* puede traer resultados externos
* puede modificar el estado final del Acta

Pero no genera un dominio propio dentro del sistema.

---

# 7. RESULTADO DE LA CAPA

Esta capa permite:

* representar derivaciones externas
* mantener el modelo simple
* integrar resultados externos
* evitar sobre-modelado

---

## Resumen

La derivación externa es:

## un cambio de trayecto del Acta

Se representa como:

## evento + documento + proyección

---

# [CAPA 02] — MODELO DOCUMENTAL CONSOLIDADO

---

# 1. FINALIDAD DE LA CAPA

Esta capa define el **modelo documental único del sistema**.

Es responsable de:

* representar todos los documentos del sistema
* permitir su almacenamiento, recuperación y uso
* soportar firma y validación
* vincular documentos al Acta
* servir como soporte formal del proceso

---

# 2. PRINCIPIO CENTRAL

## Existe un único modelo documental

No se crean modelos documentales específicos por proceso.

Todos los documentos se representan mediante:

* Documento
* ActaDocumento

---

# 3. ENTIDADES PRINCIPALES

## 3.1 Documento

### Descripción

Representa cualquier documento del sistema.

---

### Incluye

* documentos generados por el sistema
* documentos firmados
* documentos subidos
* documentos externos incorporados

---

### Responsabilidades

* almacenar metadatos del documento
* permitir acceso al archivo
* servir como soporte formal del trámite

---

### Contiene

* Id
* TipoDocumento
* NumeroDocumento (si aplica)
* FechaCreacion
* NombreOriginal (opcional)
* StorageKey
* UbicacionStorage (opcional)
* Hash (opcional)

---

### Notas

* El archivo físico no vive en la base de datos
* El documento es independiente del proceso que lo generó

---

## 3.2 DocumentoFirma

### Descripción

Representa la firma asociada a un documento.

---

### Relación

* 1:1 opcional con Documento

---

### Responsabilidades

* almacenar información de firma
* validar integridad del documento firmado

---

### Contiene

* IdDocumento
* TipoFirma
* FechaFirma
* Firmante
* DatosTecnicosFirma

---

### Regla

Solo existe si el documento requiere firma.

---

## 3.3 ActaDocumento (Referencia)

Esta entidad se define en Capa 01 y se utiliza aquí como vínculo.

---

### Responsabilidad en esta capa

* asociar documentos al Acta
* indicar contexto lógico del documento

---

# 4. TIPOS DE DOCUMENTO

Los documentos se clasifican mediante TipoDocumento.

Ejemplos:

* ACTA
* ACTA_COMPLEMENTARIA
* FALLO
* RESOLUCION
* NOTIFICACION
* PRESENTACION
* CONSTANCIA
* DOCUMENTO_EXTERNO

---

## Regla

El tipo de documento:

* clasifica
* no define comportamiento

---

# 5. RELACIÓN CON EVENTOS

## Regla fundamental

Un documento no representa un proceso, representa un resultado o soporte.

---

### Ejemplos

* DESCARGO_PRESENTADO → puede tener Documento
* FALLO_DICTADO → tiene Documento
* APELACION_PRESENTADA → puede tener Documento
* NOTIFICACION_ENVIADA → puede tener Documento

---

## Importante

El documento:

* no reemplaza al evento
* no es el evento
* acompaña al evento

---

# 6. GENERACIÓN DE DOCUMENTOS

Los documentos pueden ser:

## 6.1 Generados por el sistema

* actas
* fallos
* resoluciones

## 6.2 Subidos por usuario

* descargos
* documentación externa

## 6.3 Incorporados desde sistemas externos

* resultados judiciales
* comprobantes

---

# 7. NUMERACIÓN DOCUMENTAL

El campo NumeroDocumento:

* es opcional
* es asignado por el sistema de numeración
* puede existir antes o durante la firma

---

## Regla

La numeración es responsabilidad de un proceso externo o configurable.

---

# 8. REGLAS DE DISEÑO

## 8.1 No duplicar modelos documentales

No crear:

* NotificacionDocumento
* ActoDocumento
* RecursoDocumento

---

## 8.2 Documento es reutilizable

Un documento puede:

* vincularse a múltiples contextos
* existir independientemente del Acta

---

## 8.3 Separación clara

* Documento → soporte formal
* Evento → hecho procesal

---

## 8.4 Firma desacoplada

La firma:

* no define el documento
* no define el proceso
* es una característica opcional

---

# 9. RESULTADO DE LA CAPA

Esta capa permite:

* centralizar todos los documentos
* evitar duplicaciones
* simplificar el modelo
* soportar firma digital
* integrar con el flujo del Acta

---

## Resumen

Esta capa define:

## el soporte documental del viaje del Acta

y garantiza que:

* todo documento tenga un modelo único
* todo documento pueda vincularse al Acta
* todo documento sea reutilizable

---

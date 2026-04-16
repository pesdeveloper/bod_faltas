# [14-SQL-OPERATIVO] 05 - SQL CRUD DOCUMENTAL

## Finalidad

Este archivo documenta las operaciones SQL principales del circuito documental aplicado al expediente de faltas.

Tablas principales:

- `FalDocumento`
- `FalActaDocumento`
- `FalDocumentoFirma`
- `FalDocumentoObservacion`
- `StorObjeto`

---

## Operaciones principales

### 1. Crear documento

Orden base:

1. obtener `Id` de `FalDocumento`
2. insertar `FalDocumento`
3. insertar relación `FalActaDocumento`
4. opcionalmente asociar `StorObjeto`
5. opcionalmente insertar `FalDocumentoObservacion`
6. opcionalmente insertar `FalActaEvento`
7. confirmar transacción

### 2. Asociar storage

Regla:
- el documento es lógico
- `StorObjeto` representa el archivo físico
- la asociación debe ser explícita y controlada

### 3. Registrar firma

Tabla principal:
- `FalDocumentoFirma`

### 4. Obtener detalle documental de un acta

Lectura recomendada:
- lista de `FalActaDocumento`
- join corto a `FalDocumento`
- join opcional a `FalDocumentoFirma`
- join opcional a `StorObjeto`

---

## Consultas útiles

- documentos de una acta
- documentos por tipo
- documentos pendientes de firma
- documentos ya firmados
- documentos sin storage asociado
- documentos con storage asociado

---

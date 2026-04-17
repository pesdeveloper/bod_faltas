# [14-SQL-OPERATIVO] 05 - SQL CRUD DOCUMENTAL

## Finalidad

Este archivo documenta las operaciones SQL principales del circuito documental aplicado al expediente de faltas.

Tablas principales:

- `FalDocumento`
- `FalActaDocumento`
- `FalDocumentoFirma`
- `FalDocumentoObservacion`
- `StorObjeto`

El objetivo de este archivo es dejar explícito:

- qué patrón transaccional aplica en cada operación
- qué secuencias intervienen
- qué shape mínimo de datos requiere cada alta
- cómo se vincula el documento lógico con el acta y con el storage físico
- qué operaciones pueden impactar evento o snapshot

---

## Reglas generales

- `FalDocumento` representa el documento lógico.
- `StorObjeto` representa el archivo físico persistido.
- La existencia de un documento no depende necesariamente de que ya exista el archivo físico asociado.
- La relación del documento con el acta debe resolverse explícitamente mediante `FalActaDocumento`.
- La asociación con storage se resuelve por `StorageKey`.
- La firma documental no reemplaza al documento: se registra como información asociada.
- Si una operación documental tiene relevancia procesal, puede requerir registro de `FalActaEvento`.
- Si una operación documental impacta flags operativos visibles, puede requerir actualización de `FalActaSnapshot`.

---

## Operaciones principales

### 1. Crear documento

#### Patrón transaccional aplicable

- patrón fundamental: **Alta de agregado padre/hijos**
- pasos comunes posibles:
  - registrar evento append-only
  - actualizar proyección resumida

#### Secuencia principal

- secuencia de `FalDocumento`
- si se insertan observaciones o firmas en la misma operación, también deben intervenir las secuencias correspondientes a esas tablas

#### Shape mínimo requerido

La creación del documento debe recibir, como mínimo:

- `IdActa`
- `TipoDocu`
- `EstadoDocu`
- `FhAlta`
- `IdUserAlta`
- datos mínimos necesarios para `FalActaDocumento`:
  - `RolDocuActa`
  - `SiPrincipal`
  - `FhAlta`
  - `IdUserAlta`

Además, puede recibir opcionalmente:

- `NroDocu`
- `IdTalonario`
- `TipoFirmaReq`
- `StorageKey`
- `HashDocu`
- `FhGeneracion`
- una observación documental inicial
- datos para registrar evento o impacto en snapshot

#### Orden base

1. obtener `IdDocu`
2. insertar `FalDocumento`
3. insertar relación `FalActaDocumento`
4. opcionalmente registrar `FalDocumentoObservacion`
5. opcionalmente registrar `FalActaEvento`
6. opcionalmente actualizar `FalActaSnapshot`
7. confirmar transacción

#### Regla operativa

La creación del documento debe dejar resuelto, como mínimo:

- que el documento lógico existe
- que su vínculo con el acta existe
- que la ausencia de archivo físico inicial no invalida el documento lógico si el flujo lo admite

---

### 2. Asociar storage

#### Finalidad

Asociar un archivo físico persistido (`StorObjeto`) a un documento lógico ya existente.

#### Patrón transaccional aplicable

- patrón fundamental: **Corrección controlada de dato mutable**
- opcionalmente con evento append-only si la asociación tiene relevancia procesal

#### Regla

- el documento es lógico
- `StorObjeto` representa el archivo físico
- la asociación debe ser explícita y controlada
- la asociación se resuelve mediante `StorageKey` en `FalDocumento`

#### Orden base

1. localizar `FalDocumento` por `IdDocu`
2. verificar existencia o validez del `StorageKey` en `StorObjeto`
3. actualizar `FalDocumento.StorageKey`
4. opcionalmente actualizar `HashDocu` u otros metadatos documentales si el flujo lo requiere
5. opcionalmente registrar `FalActaEvento`
6. opcionalmente actualizar `FalActaSnapshot`
7. confirmar transacción

#### Nota importante

Este bloque asume que la referencia al archivo físico se resuelve por `StorageKey` y no por tabla puente adicional.

---

### 3. Registrar firma

#### Finalidad

Registrar una firma sobre un documento ya existente.

#### Tabla principal

- `FalDocumentoFirma`

#### Patrón transaccional aplicable

- patrón fundamental: **Alta simple** sobre `FalDocumentoFirma`
- opcionalmente con pasos comunes de evento y actualización de snapshot

#### Shape mínimo requerido

La operación debe recibir, como mínimo:

- `Id`
- `IdDocu`
- `TipoFirma`
- `EstadoFirma`
- `FhAlta`
- `IdUserAlta`

Además, puede recibir opcionalmente:

- `IdUserFirma`
- `FhSolicitud`
- `FhFirma`

#### Orden base

1. localizar `FalDocumento`
2. obtener `Id` de `FalDocumentoFirma`
3. insertar `FalDocumentoFirma`
4. opcionalmente registrar `FalActaEvento`
5. opcionalmente actualizar `FalActaSnapshot`
6. confirmar transacción

#### Regla operativa

La firma debe registrarse como información asociada al documento y no como reemplazo del documento lógico.

---

### 4. Registrar observación documental

#### Finalidad

Registrar una observación o contexto adicional sobre un documento.

#### Tabla principal

- `FalDocumentoObservacion`

#### Patrón transaccional aplicable

- patrón fundamental: **Alta simple**
- opcionalmente con evento append-only si la observación tiene efecto procesal

#### Shape mínimo requerido

La operación debe recibir, como mínimo:

- `Id`
- `IdDocu`
- `ObsDocu`
- `FhAlta`
- `IdUserAlta`

#### Orden base

1. localizar `FalDocumento`
2. obtener `Id` de `FalDocumentoObservacion`
3. insertar `FalDocumentoObservacion`
4. opcionalmente registrar `FalActaEvento`
5. confirmar transacción

#### Regla

La observación documental no reemplaza:

- el estado documental
- la firma
- la relación con el acta
- el evento procesal cuando este sea requerido

---

### 5. Obtener detalle documental de un acta

#### Lectura recomendada

- lista de `FalActaDocumento`
- join corto a `FalDocumento`
- join opcional a `FalDocumentoFirma`
- join opcional a `StorObjeto`

#### Regla de lectura

Se recomienda resolver el detalle documental con joins cortos o lecturas compuestas, evitando una consulta monolítica innecesaria.

El objetivo de esta lectura es responder:

- qué documentos tiene el expediente
- qué tipo documental tiene cada uno
- qué estado documental relevante presentan
- si tienen `StorageKey` asociado
- si tienen firma registrada
- si tienen observaciones

---

## Consultas útiles

- documentos de una acta
- documentos por tipo
- documentos pendientes de firma
- documentos ya firmados
- documentos sin `StorageKey`
- documentos con `StorageKey`
- observaciones documentales de un documento
- historial documental resumido de un expediente

---

## Riesgos a controlar

- crear documento sin vincularlo correctamente al acta
- asumir que documento lógico y archivo físico son la misma cosa
- registrar firma sin documento válido
- acoplar demasiado temprano la creación documental al storage físico
- no actualizar flags operativos cuando el circuito documental impacta bandejas o snapshot
- usar una única query gigante para resolver todo el detalle documental

---
# Esquema relacional base

## Finalidad

Este archivo define la estructura relacional base del sistema a partir del modelo lógico consolidado.

Su función es ubicar los grandes grupos de tablas y su relación general dentro del backend.

---

## Criterio general

El esquema relacional se organiza alrededor del expediente como unidad principal de gestión.

Debe distinguir con claridad entre:

- núcleo transaccional
- referencias y versionado
- persistencia documental
- notificaciones
- numeración
- snapshot operativo
- catálogos y auxiliares de integración

---

## Grupos principales

### Núcleo del expediente

- `Acta`
- `ActaEvento`
- `Notificacion`
- entidades principales de medidas y liberaciones, cuando corresponda

`Acta` es la tabla ancla del expediente.  
`ActaEvento` es la tabla principal de trazabilidad.

### Referencias y versionado

- `Dependencia`
- `DependenciaVersion`
- `Inspector`
- `InspectorVersion`

Este grupo preserva contexto histórico sin snapshot textual embebido en `Acta`.

### Persistencia documental

- `Documento`
- tabla auxiliar de versiones materiales o archivos, si corresponde
- tablas auxiliares de firma, observación o resultado documental, si luego se justifican

Debe separar identidad lógica y soporte material.

### Numeración y talonarios

- `Talonario`
- `PoliticaNumeracion` o equivalente, si se separa
- tablas auxiliares de asignación, reserva o control, si luego se justifican

### Snapshot y proyecciones

- `ActaSnapshot` o equivalente
- otras proyecciones operativas, si luego se justifican

Este grupo es derivado.

### Catálogos y maestros

- tipos
- estados
- canales
- resultados
- clasificaciones
- motivos
- otros catálogos transversales

### Integraciones y auxiliares

- correlación con sistemas externos
- storage documental
- resultados de integración
- auxiliares de reproceso o sincronización, si luego se justifican

---

## Reglas estructurales

- `Acta` es la entidad principal del esquema.
- `ActaEvento` concentra la trazabilidad relevante.
- `Documento` y `Notificacion` conservan estructura propia.
- `Dependencia` e `Inspector` se resuelven con entidad principal y versión aplicable.
- La numeración visible debe persistirse con su contexto de numeración.
- El snapshot permanece separado del núcleo transaccional.
- Las tablas auxiliares no deben contaminar el núcleo del expediente.

---

## Resultado esperado

Este archivo debe dejar clara la partición relacional base del backend antes de bajar a archivos específicos de versionado, persistencia documental, numeración y snapshot.
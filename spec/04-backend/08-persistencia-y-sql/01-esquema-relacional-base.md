# Esquema relacional base

## Finalidad

Este archivo define la estructura relacional base del sistema a partir del modelo lógico consolidado.

Su función es ubicar los grandes grupos de tablas y su relación general dentro del backend, ya con el nivel de concreción suficiente para preparar diagrama relacional y SQL de creación.

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
- storage documental

---

## Grupos principales

### Núcleo del expediente

- `Acta`
- `ActaEvento`
- `Observacion`

`Acta` es la tabla ancla del expediente.  
`ActaEvento` es la tabla principal de trazabilidad.  
`Observacion` es la tabla transversal de observaciones breves.

---

### Satélites del expediente

- `ActaTransito`
- `ActaTransitoAlcoholemia`
- `ActaVehiculo`
- `ActaContravencion`
- `ActaSustanciasAlimenticias`
- `ActaMedidaPreventiva`
- `ActaEvidencia`

Este grupo concentra información especializada que no debe absorberse desordenadamente dentro de `Acta`.

---

### Referencias y versionado

- `Dependencia`
- `DependenciaVersion`
- `Inspector`
- `InspectorVersion`
- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

Este grupo preserva contexto histórico sin snapshot textual embebido en `Acta`.

---

### Persistencia documental

- `Documento`
- `ActaDocumento`
- `DocumentoFirma`
- `DocumentoObservacion`

Debe separar:

- identidad lógica del documento
- relación con expediente
- estado documental
- metadata mínima de firma
- referencia técnica al archivo vigente

---

### Notificaciones

- `Notificacion`
- `NotificacionIntento`
- `NotificacionAcuse`
- `NotificacionObservacion`

Este grupo modela la notificación como proceso transversal del expediente.

En el modelo actual:

- toda notificación recae sobre un documento
- toda notificación pertenece a una acta
- cada intento tiene un único destino efectivo

---

### Numeración y talonarios

- `PoliticaNumeracion`
- `Talonario`
- `TalonarioDependencia`
- `TalonarioInsp`
- `TalonarioMovimiento`

Este grupo resuelve la numeración visible dentro de su contexto administrativo.

---

### Snapshot operativo

- `ActaSnapshot`

Este grupo es derivado y regenerable.

No reemplaza al núcleo transaccional.

---

### Storage documental

- `StorageBackend`
- `StoragePolitica`
- `StorageObjeto`

Este grupo desacopla el dominio de la ubicación física real del archivo y resuelve:

- backend
- política
- ruta relativa
- metadata técnica
- `StorageKey`

---

### Catálogos y maestros

- tipos
- estados
- canales
- resultados
- clasificaciones
- motivos
- otros catálogos transversales
- catálogos y maestros operativos con versionado cuando corresponda

---

## Reglas estructurales

- `Acta` es la entidad principal del esquema.
- `ActaEvento` concentra la trazabilidad relevante.
- `Documento` y `Notificacion` conservan estructura propia.
- `Dependencia` e `Inspector` se resuelven con entidad principal y versión aplicable.
- `Alcoholimetro` y `RubroCom` también pueden requerir congelamiento histórico mediante versión.
- La numeración visible debe persistirse con su contexto de numeración.
- El snapshot permanece separado del núcleo transaccional.
- El storage documental permanece separado del documento lógico.
- Las tablas auxiliares no deben contaminar el núcleo del expediente.

---

## Relaciones base más importantes

### Expediente
- `Acta` se relaciona con `ActaEvento`
- `Acta` se relaciona con satélites especializados
- `Acta` se relaciona con documentos
- `Acta` se relaciona con notificaciones
- `Acta` se proyecta en `ActaSnapshot`

### Documental
- `Documento` se vincula con `Acta` mediante `ActaDocumento`
- `Documento` puede requerir firma
- `Documento` se apoya en storage documental mediante `StorageKey`

### Notificación
- `Notificacion` se relaciona siempre con `Acta` y `Documento`
- `NotificacionIntento` y `NotificacionAcuse` completan la trazabilidad del circuito notificatorio

### Numeración
- `Acta` y `Documento`, cuando corresponda, consumen numeración dentro de su contexto administrativo
- `TalonarioMovimiento` resuelve el estado de números manuales físicos

### Storage
- las tablas del dominio guardan referencias técnicas mediante `StorageKey`
- la resolución física real del archivo vive en el bloque de storage documental

---

## Resultado esperado

Este archivo debe dejar clara la partición relacional base del backend antes de bajar a diagrama relacional y SQL físico, con un nivel de concreción ya alineado al bloque `13-ddl`.
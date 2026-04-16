# Tablas storage documental

## Finalidad

Este archivo define el bloque físico de storage documental del sistema de faltas.

Incluye:

- `StorageBackend`
- `StoragePolitica`
- `StorageObjeto`

Este bloque resuelve:

- dónde se almacenan físicamente los archivos
- cómo se distribuyen en una estructura jerárquica
- cómo se desacopla el dominio de la ruta física real
- cómo soportar backends distintos:
  - disco local
  - unidad de red
  - S3
  - Azure Blob
- cómo definir fallback por política
- cómo permitir migraciones futuras sin romper referencias del dominio

---

## Criterios generales del bloque

- el dominio nunca debe depender de una ruta física absoluta
- el dominio debe referenciar archivos por `StorageKey`
- la ubicación física se resuelve mediante backend + ruta relativa
- la política de storage puede variar por:
  - sistema
  - familia
  - tipo de objeto
- si no existe política específica:
  - se usa política general del sistema
  - y si tampoco existe, backend default
- la distribución física debe evitar carpetas con demasiados archivos
- la estructura física debe seguir siendo suficientemente interpretable para búsqueda humana razonable

---

## Política de distribución física

### Estructura relativa recomendada

`/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`

### Componentes

- `sistema`
  - ejemplo: `faltas`
- `familia`
  - ejemplo: `documentos`, `notificaciones`, `evidencias`, `acuses`
- `tipo`
  - ejemplo: `acta`, `fallo`, `resolucion`, `notif_acta`, `foto`
- `anio`
  - año lógico del objeto
- `mes`
  - mes lógico del objeto
- `bucket`
  - partición técnica para distribuir archivos
- `ref_negocio`
  - referencia legible derivada del caso, por ejemplo:
    - `acta_2026-001-000245`
    - `idacta_15423`
- `storage_key`
  - identificador físico único del archivo

### Objetivos de esta estructura

- evitar carpetas gigantes
- distribuir de forma estable
- permitir inferencia humana razonable si se tiene el número de acta
- mantener independencia respecto del backend físico real

---

## Reglas de diseño de filesystem

- no depender de límites teóricos máximos del filesystem
- evitar directorios con acumulación masiva de archivos
- mantener nombres de carpetas y archivos compactos
- no usar nombres físicos derivados solo del nombre original subido
- `ref_negocio` debe ser legible pero acotado
- `bucket` debe distribuir técnicamente la carga
- la ruta física absoluta final no debe estar embebida en tablas de dominio

---

## Regla de resolución de backend

Orden recomendado:

1. buscar política específica por:
   - sistema
   - familia
   - tipo de objeto
2. si no existe:
   - usar política general del sistema
3. si tampoco existe:
   - usar backend default

---

## Regla de migración

- las tablas de dominio deben guardar solo `StorageKey`
- la ubicación física real debe resolverse desde este bloque
- si cambia el backend físico:
  - no debe cambiar el dominio
  - no debe cambiar el `StorageKey`
  - solo debe actualizarse la resolución del storage o la metadata del objeto

---

## Tabla: StorageBackend

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdStorageBackend` | `INT` | No | PK |
| `NomStorageBackend` | `VARCHAR(64)` | No | Nombre lógico del backend |
| `TipoBackend` | `SMALLINT` | No | Tipo de backend |
| `BasePath` | `VARCHAR(255)` | Sí | Ruta base local o de red |
| `BucketContenedor` | `VARCHAR(120)` | Sí | Bucket o contenedor cloud |
| `PrefijoRuta` | `VARCHAR(120)` | Sí | Prefijo raíz opcional |
| `SiDefault` | `SMALLINT` | No | 0/1 backend por defecto |
| `SiActivo` | `SMALLINT` | No | 0/1 |
| `ObsBackend` | `VARCHAR(255)` | Sí | Observación breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- Esta tabla representa el backend físico o lógico de almacenamiento.
- `BasePath` aplica especialmente a disco local, unidad de red o filesystem similar.
- `BucketContenedor` aplica especialmente a S3, Azure Blob u otros storage cloud.
- `PrefijoRuta` permite distinguir una raíz documental dentro del backend.
- El sistema de faltas puede usar un backend propio, por ejemplo una unidad dedicada, sin impedir una futura migración.

---

## Tabla: StoragePolitica

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdStoragePolitica` | `INT` | No | PK |
| `Sistema` | `VARCHAR(20)` | No | Sistema al que aplica |
| `Familia` | `VARCHAR(30)` | Sí | Familia documental |
| `TipoObjeto` | `VARCHAR(30)` | Sí | Tipo de objeto |
| `IdStorageBackend` | `INT` | No | Backend asignado |
| `Prioridad` | `SMALLINT` | No | Prioridad de resolución |
| `SiActiva` | `SMALLINT` | No | 0/1 |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |
| `ObsPolitica` | `VARCHAR(255)` | Sí | Observación breve |

### Notas
- Esta tabla define qué backend usar según sistema/familia/tipo.
- `Familia` y `TipoObjeto` pueden ser `NULL` para reglas más generales.
- `Prioridad` permite resolver conflictos entre reglas.
- Debe existir al menos una política default o una combinación de políticas que garantice resolución de backend.

---

## Tabla: StorageObjeto

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `StorageKey` | `VARCHAR(64)` | No | PK lógica única |
| `IdStorageBackend` | `INT` | No | Backend resuelto |
| `Sistema` | `VARCHAR(20)` | No | Sistema origen |
| `Familia` | `VARCHAR(30)` | No | Familia documental |
| `TipoObjeto` | `VARCHAR(30)` | No | Tipo de objeto |
| `Anio` | `SMALLINT` | No | Año lógico de partición |
| `Mes` | `SMALLINT` | No | Mes lógico de partición |
| `Bucket` | `SMALLINT` | No | Bucket técnico |
| `RefNegocio` | `VARCHAR(80)` | No | Referencia legible del caso |
| `NomArchivo` | `VARCHAR(120)` | Sí | Nombre lógico/original |
| `ExtArchivo` | `VARCHAR(10)` | Sí | Extensión |
| `MimeType` | `VARCHAR(80)` | Sí | Tipo MIME |
| `TamBytes` | `INT8` | Sí | Tamaño en bytes |
| `HashArchivo` | `VARCHAR(128)` | Sí | Hash del archivo |
| `RutaRelativa` | `VARCHAR(255)` | No | Ruta relativa derivada |
| `EstadoStorage` | `SMALLINT` | No | Estado del objeto |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- `StorageObjeto` representa el archivo persistido en storage.
- `StorageKey` es la referencia única que usan las tablas del dominio.
- `RutaRelativa` se deriva según la política de distribución.
- El nombre físico final del archivo debe usar `StorageKey` como base, no el nombre original.
- `RefNegocio` existe para mantener interpretabilidad humana razonable.
- Si se tiene el número de acta, debe ser posible inferir razonablemente la rama donde buscar.

---

## Regla de construcción de ruta relativa

La ruta relativa debe construirse con esta forma:

`/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`

### Ejemplos

- `/faltas/documentos/acta/2026/04/37/acta_2026-001-000245/abc123.pdf`
- `/faltas/documentos/fallo/2026/04/12/acta_2026-001-000245/def456.pdf`
- `/faltas/notificaciones/notif_acta/2026/04/08/acta_2026-001-000245/ghi789.pdf`
- `/faltas/evidencias/foto/2026/04/63/acta_2026-001-000245/jkl321.jpg`

### Fallback para `RefNegocio`

Si todavía no existe número visible suficientemente estable:

- usar `idacta_{IdActa}`

Ejemplo:
- `idacta_15423`

---

## Regla de bucket

- `Bucket` debe calcularse de forma determinística
- puede derivarse desde:
  - `StorageKey`
  - o un identificador estable equivalente
- su objetivo es distribuir uniformemente archivos
- no debe depender del nombre original del archivo

---

## Regla de implementación

- no guardar binarios en base de datos
- el dominio debe guardar solo `StorageKey`
- la resolución física debe hacerla la capa de acceso o servicio de storage
- el cambio de backend no debe obligar a reescribir el dominio
- la política de storage debe poder cambiar con impacto mínimo

---

## Algoritmo conceptual de resolución de backend

1. buscar política específica por:
   - `Sistema`
   - `Familia`
   - `TipoObjeto`
2. si no existe:
   - buscar política general del sistema
3. si tampoco existe:
   - usar backend default
4. devolver backend resuelto

---

## Algoritmo conceptual de construcción de ruta

1. resolver backend según política
2. normalizar:
   - `Sistema`
   - `Familia`
   - `TipoObjeto`
   - `RefNegocio`
   - `ExtArchivo`
3. calcular `Bucket` desde `StorageKey`
4. construir ruta relativa:
   - `/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`
5. persistir:
   - backend resuelto
   - ruta relativa
   - metadata del archivo

---

## Enumeraciones del bloque

### TipoBackend
- `1 = FILESYSTEM_LOCAL`
- `2 = FILESYSTEM_RED`
- `3 = S3`
- `4 = AZURE_BLOB`
- `5 = OTRO`

### EstadoStorage
- `1 = ACTIVO`
- `2 = REEMPLAZADO`
- `3 = ELIMINADO_LOGICO`
- `4 = HUERFANO`
- `5 = ERROR`

### SiDefault
- `0 = NO`
- `1 = SI`

### SiActivo
- `0 = NO`
- `1 = SI`
# 99 - Estado actual y próximo paso

## Estado actual

El trabajo está concentrado en la consolidación del bloque `spec/13-ddl/` del repo multiproyecto del sistema de faltas municipal.

Ya no estamos discutiendo fundamentos del dominio ni del backend.
El foco actual es bajar correctamente el modelo a DDL físico, manteniendo consistencia con lo ya cerrado en:

- overview
- reglas transversales principales
- bandejas canónicas
- `spec/01-dominio/`
- `spec/12-datos/`
- `spec/04-backend/`
- `spec/09-integraciones/`

---

## Bloques DDL ya trabajados

Ya quedaron trabajados y bastante alineados:

- `spec/13-ddl/01-convenciones-ddl-informix.md`
- `spec/13-ddl/02-tablas-nucleo-expediente.md`
- `spec/13-ddl/03-tablas-referenciales-y-versionado.md`
- `spec/13-ddl/04-tablas-acta-y-satelites.md`
- `spec/13-ddl/05-tablas-talonarios-y-numeracion.md`
- `spec/13-ddl/06-tablas-equipos-y-catalogos-operativos.md`
- `spec/13-ddl/07-tablas-documentales.md`
- `spec/13-ddl/08-tablas-notificacion.md`
- `spec/13-ddl/09-tablas-snapshot-auxiliares-y-proyecciones.md`
- `spec/13-ddl/10-tablas-storage-documental.md`

Además, se definió una convención transversal de nomenclatura que debe vivir en:

- `spec/00-overview/03-convenciones-de-nomenclatura.md`

---

## Decisiones fuertes ya cerradas

### Convención de nomenclatura

Se acordó una regla general para todo el proyecto:

- nombres compactos
- consistentes
- legibles
- cómodos para SQL
- con composición:
  - **prefijo + concepto + contexto**

#### Prefijos estándar
- `Id`
- `Ver`
- `Fh`
- `Si`
- `Nro`
- `Obs`
- `Cant`
- `Cod`
- `Nom`
- `Desc`

#### Contextos aceptados
- `Infr` = infracción
- `Infct` = infractor
- `Info` = información

#### Regla adicional
Si un campo no encaja exactamente:
- nombrarlo corto
- con criterio
- sin perder significado

---

## Regla de redacción de spec ya acordada

Los spec DDL deben redactarse así:

- tabla en formato tabular
- notas debajo de cada tabla
- enumeraciones explícitas en el mismo archivo donde se usan
- estilo compacto, claro y navegable
- útil para implementación asistida y para lectura humana

---

## Estado del núcleo del acta

### Acta
Quedó firme que:

- `NroActa` en la base central no puede ser null
- sync local/mobile no contamina el núcleo central
- el domicilio de la infracción debe tender a ser normalizado
- el partido no se persiste en el domicilio de la infracción
- se habilita domicilio textual solo como excepción controlada
- `SiEjeUrb` puede persistirse como dato derivado
- `ObsActa` es texto propio del acta, no auditoría interna
- faltaba incorporar GPS y se acordó agregar:
  - `LatInfr`
  - `LonInfr`

### ActaSnapshot
Quedó firme que:

- existe **una sola definición canónica** de `ActaSnapshot`
- esa definición debe vivir en `09-tablas-snapshot-auxiliares-y-proyecciones.md`
- no debe quedar duplicada en `02`
- es derivado y regenerable
- no es fuente primaria de verdad
- debe ser simple, directo y orientado a operación, bandejas, plazos, pagos y gestión externa

### ActaEvento
Quedó firme que:

- es append-only
- no lleva texto libre embebido
- las observaciones operativas van a `Observacion`

### Observacion
Quedó firme que:

- es tabla transversal
- texto corto `VARCHAR(255)`
- el campo de usuario queda como `IdUser`
- si se edita, la edición debe generar auditoría/evento

---

## Referenciales/versionado

### Dependencia
Quedó firme:

- existe tabla base `Dependencia`
- existe tabla histórica `DependenciaVersion`
- debe poder reconstruirse organigrama histórico
- el acta guarda `IdDep + VerDep`

### Inspector
Quedó firme:

- `IdInsp` es `INT`
- `VerInsp` se mantiene
- el IdP emite claims construidos:
  - `InspectorId`
  - `InspectorVersion`
- el acta persiste `IdInsp + VerInsp`

### Medida preventiva
Quedó firme:

- catálogo por dependencia
- existe `MedidaPreventiva`
- la aplicación concreta se resuelve en `ActaMedidaPreventiva`

---

## Satélites del acta

### ActaVehiculo
Quedó firme:

- tabla separada reutilizable
- sirve para tránsito y sustancias alimenticias
- `TipoVehTxt` solo si `TipoVeh = OTRO`

### ActaTransito
Quedó firme:

- guarda resumen rápido
- alcoholemia detallada va aparte
- el equipo usado se referencia por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- no debe duplicar datos del equipo si ya quedan congelados en `AlcoholimetroVersion`

### ActaTransitoAlcoholemia
Quedó firme:

- guarda mediciones
- una debe poder marcarse como resultado final

### ActaContravencion
Quedó firme:

- agregar:
  - `IdSuj SMALLINT`
  - `IdBie INT`
- la nomenclatura puede precargarse desde lookup maestro
- el inspector puede corregir datos precargados

#### OrigenNomencl
- `1 = OBTENIDA_COMERCIO`
- `2 = OBTENIDA_INMUEBLE`
- `3 = INGRESADA_MANUAL_VALIDADA`

### ActaSustanciasAlimenticias
Quedó firme:

- usa rubro y ámbito
- no duplica estructura vehicular

### Evidencias
Se detectó un faltante importante del DDL respecto del dominio:
- faltaban las evidencias del acta digital

Se acordó incorporar tabla satélite:
- `ActaEvidencia`

con storage por `StorageKey`, tipo de evidencia, hash, nombre lógico, orden y fecha de captura.

---

## Normativa / artículos / valores

Quedó firme la separación entre:

- `NormativaFaltas`
- `ArticuloNormativaFaltas`
- `TarifarioUnidadFaltas`
- `ActaArticuloInfringido`
- `ActaArticuloAuditoria`

Y también que:

- los artículos no deben quedar como texto plano en satélites
- el acta debe congelar la instancia aplicada
- la auditoría de ajustes manuales no se mezcla con `Observacion`

---

## Talonarios y numeración

### Tablas
- `PoliticaNumeracion`
- `Talonario`
- `TalonarioDependencia`
- `TalonarioInsp`
- `TalonarioMovimiento`

### Decisiones clave
- no usar máscara libre
- la política se compone por componentes:
  - prefijo
  - año
  - serie
  - número
- cada unión entre componentes puede tener separador distinto:
  - `SepPrefAnio`
  - `SepAnioSerie`
  - `SepSerieNro`
- `TipoTalonario`:
  - `1 = ELECTRONICO`
  - `2 = MANUAL_FISICO`
- `TalonarioMovimiento` registra el estado de cada número manual
- no hace falta:
  - `ActaManualAnulada`
  - `ActaOrigenNumeracion`
- los talonarios pueden ser:
  - globales
  - de dependencia
- la asignación a inspector aplica solo a manual físico
- un talonario bloqueado no puede usarse
- `CodDesbloqueo` lo genera el sistema y el desbloqueo es por API específica

---

## Equipos y catálogos operativos

### Tablas
- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

### Alcoholímetro
Quedó firme:

- el equipo debe ser versionado
- el acta referencia el equipo por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- la UX mobile puede seleccionar equipo localmente o por QR
- la deshabilitación debe documentarse

### Rubros
Quedó firme:

- usar:
  - `RubroCom`
  - `RubroComVersion`

---

## Documentales

### Tablas
- `Documento`
- `ActaDocumento`
- `DocumentoFirma`
- `DocumentoObservacion`

### Decisiones clave
- documento lógico separado del storage
- storage desacoplado por `StorageKey`
- numeración documental se resuelve por talonario si aplica
- `IdPolNum` sobra en `Documento`
- `SiFirmaDigital` y `SiFirmaOlografa` se reemplazan por:
  - `TipoFirmaReq`
- no se guarda archivo previo no firmado luego de firmar
- el archivo firmado reemplaza al borrador
- no hacen falta en `DocumentoFirma`:
  - `FirmanteRef`
  - `StorageKeyFirmado`
  - `HashFirmado`
  - `IdExternoFirma`
  - `ResultadoFirma`
- se usa:
  - `IdUserFirma`
- `HashDocu VARCHAR(128)` alcanza para hashes textuales habituales

---

## Notificación

### Tablas
- `Notificacion`
- `NotificacionIntento`
- `NotificacionAcuse`
- `NotificacionObservacion`

### Decisiones clave
- la notificación es transversal
- siempre se notifica un documento
- en este modelo toda notificación pertenece a una acta
- `IdDocu` y `IdActa` deben ser obligatorios
- `FhVencimiento` en `Notificacion` no tiene sentido y debe eliminarse
- `NotificacionDestino` sobra y debe eliminarse
- cada intento se envía a un único destino efectivo
- `NotificacionIntento` debe guardar:
  - `TipoDestNotif`
  - `DestNotif`
- `NotificacionAcuse` puede simplificarse eliminando:
  - `CodAcuse`
  - `RefExterna`
- `StorageKeyAcuse` queda opcional
- conviene tener en `Notificacion` un estado resumido de acuse:
  - `EstadoAcuse`

---

## Snapshot principal

### Tabla
- `ActaSnapshot`

### Decisiones clave
- reemplaza el enfoque de múltiples snapshots auxiliares
- debe permitir ver rápidamente:
  - bandeja
  - proceso
  - si tiene notificación de acta
  - si tiene notificación de medida preventiva
  - si tiene notificación de fallo/acto
  - si alguna está en proceso o pendiente de acuse
  - si hubo reintentos
  - si tiene solicitud de pago voluntario
  - monto exigible del acta
  - si pagó totalmente
  - si tiene plan de pagos
  - cantidad de cuotas / valor cuota
  - cantidad de caídas/refinanciaciones
  - si está en gestión externa
  - tipo de gestión externa
  - si reingresó de gestión externa
  - resultado de gestión externa
  - plazos relevantes
  - fecha del acta
  - dependencia
  - inspector

### Decisiones eliminadas
- `SnapshotJobControl` no aporta valor práctico y debe eliminarse
- no se justifican snapshots territoriales auxiliares si esos datos ya están en `Acta`

---

## Storage documental

### Tablas
- `StorageBackend`
- `StoragePolitica`
- `StorageObjeto`

### Decisiones clave
- `StorageKey` debe tener bloque de storage real detrás
- para esta versión se acepta un storage específico de faltas
- la política debe soportar:
  - disco local
  - red
  - S3
  - Azure Blob
- debe poder resolverse backend por:
  - sistema
  - familia
  - tipo de objeto
- si no existe política específica:
  - usar política general del sistema
  - si no existe, backend default

### Política de distribución
La ruta relativa recomendada es:

`/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`

### Criterios de diseño
- evitar carpetas gigantes
- distribución técnica por bucket
- interpretabilidad humana razonable
- si se tiene el número de acta debe poder inferirse razonablemente dónde buscar
- no guardar binarios en base
- el dominio guarda solo `StorageKey`
- la migración de backend no debe obligar a cambiar el dominio

---

## Correcciones de consistencia detectadas en el zip

Quedaron detectados tres ajustes reales:

### 1) `02-tablas-nucleo-expediente.md`
- eliminar completamente la definición de `ActaSnapshot`

### 2) `09-tablas-snapshot-auxiliares-y-proyecciones.md`
- dejar como definición canónica única de `ActaSnapshot`
- eliminar `SnapshotJobControl` si todavía sigue

### 3) `08-tablas-notificacion.md`
- eliminar toda referencia residual a `NotificacionDestino`
- corregir encabezado y criterios generales en consecuencia

### 4) `01-convenciones-ddl-informix.md`
- alinear la referencia textual y link con:
  - `spec/00-overview/03-convenciones-de-nomenclatura.md`

---

## Próximo paso

El siguiente paso lógico es:

1. aplicar los parches puntuales ya identificados en:
   - `01`
   - `02`
   - `08`
   - `09`
2. revisar consistencia global final entre:
   - `04`
   - `05`
   - `06`
   - `07`
   - `08`
   - `09`
   - `10`
3. después avanzar con:
   - diagrama relacional
   - SQL de creación

---

## Criterio de continuación

- seguir en español
- no rediseñar desde cero
- usar `spec/` como fuente de verdad
- mantener documentos compactos
- no inflar definiciones
- no reabrir fundamentos ya cerrados salvo contradicción real
- seguir con el mismo estilo tabular + notas + enumeraciones
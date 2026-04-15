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
- `Id` = identificador
- `Ver` = versión
- `Fh` = fecha/hora
- `Si` = flag booleano lógico
- `Nro` = número visible o administrativo
- `Obs` = observación
- `Cant` = cantidad
- `Cod` = código
- `Nom` = nombre
- `Desc` = descripción

#### Contextos aceptados
- `Infr` = infracción
- `Infct` = infractor
- `Info` = información

#### Regla adicional
Si un campo no encaja exactamente en la convención:
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

### ActaSnapshot
Quedó firme que:

- es 1:1 con `Acta`
- es derivado y regenerable
- no es fuente primaria de verdad
- `FhUltMod` e `IdUserUltMod` se guardan acá, no en `Acta`

### ActaEvento
Quedó firme que:

- es append-only
- no lleva texto libre embebido
- las observaciones operativas van a tabla transversal `Observacion`

### Observacion
Quedó firme que:

- es tabla transversal
- texto corto `VARCHAR(255)`
- el campo de usuario queda como `IdUser`
- si se edita, la edición debe generar auditoría/evento

---

## Estado de referenciales/versionado

### Dependencia
Quedó firme:

- existe tabla base `Dependencia`
- existe tabla histórica `DependenciaVersion`
- debe poder reconstruirse organigrama histórico
- la relación padre/hija forma parte del modelo
- las referencias del acta deben guardar `IdDep + VerDep`

### Inspector
Quedó firme:

- `IdInsp` pasa a `INT`
- `VerInsp` se mantiene
- el IdP no se modifica estructuralmente
- el IdP emite claims construidos:
  - `InspectorId`
  - `InspectorVersion`
- el versionado del inspector queda desacoplado del maestro de usuarios
- el acta persiste `IdInsp + VerInsp`
- existe:
  - `Inspector`
  - `InspectorVersion`

### Medida preventiva
Quedó firme:

- el catálogo se organiza por dependencia
- existe:
  - `MedidaPreventiva`
- la aplicación concreta al acta se resuelve en:
  - `ActaMedidaPreventiva`

---

## Estado de satélites del acta

### ActaVehiculo
Quedó firme:

- tabla separada reutilizable
- sirve para tránsito y sustancias alimenticias
- `TipoVehTxt` solo aplica si `TipoVeh = OTRO`

### ActaTransito
Quedó firme:

- guarda resumen rápido de tránsito
- alcoholemia detallada va en tabla aparte
- el equipo usado se referencia por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`

### ActaTransitoAlcoholemia
Quedó firme:

- guarda mediciones
- una debe poder marcarse como resultado final
- la tabla no necesita duplicar datos completos del equipo

### ActaContravencion
Corrección reciente ya aceptada:

- agregar:
  - `IdSuj SMALLINT`
  - `IdBie INT`
- estos identifican el origen de la cuenta municipal consultada
- la nomenclatura puede precargarse desde lookup maestro
- el inspector puede corregir datos precargados si detecta inconsistencias

#### OrigenNomencl
Queda así:
- `1 = OBTENIDA_COMERCIO`
- `2 = OBTENIDA_INMUEBLE`
- `3 = INGRESADA_MANUAL_VALIDADA`

### ActaSustanciasAlimenticias
Quedó firme:

- usa rubro y ámbito
- no duplica estructura vehicular
- vehículo se resuelve con `ActaVehiculo`

---

## Normativa / artículos / valores

Quedó firme la separación entre:

- `NormativaFaltas`
- `ArticuloNormativaFaltas`
- `TarifarioUnidadFaltas`
- `ActaArticuloInfringido`
- `ActaArticuloAuditoria`

Y también que:

- los artículos no deben quedar como texto plano en el satélite
- el acta debe congelar la instancia aplicada
- la auditoría de ajustes manuales no se mezcla con `Observacion`

---

## Talonarios y numeración

Hoy quedó bastante avanzado el bloque de talonarios.

### Tablas ya definidas
- `PoliticaNumeracion`
- `Talonario`
- `TalonarioDependencia`
- `TalonarioInsp`
- `TalonarioMovimiento`

### Decisiones importantes
- no usar máscara libre
- la política se compone por partes:
  - prefijo
  - año
  - serie
  - número
- cada unión entre componentes puede tener separador distinto
- talonarios pueden ser:
  - globales
  - de dependencia
- asignación a inspector solo para manual físico
- `TalonarioMovimiento` registra el estado de cada número manual
- no hace falta:
  - `ActaManualAnulada`
  - `ActaOrigenNumeracion`

### TipoTalonario
- `1 = ELECTRONICO`
- `2 = MANUAL_FISICO`

### EstadoNro
- `1 = USADO`
- `2 = ANULADO`

### Ajuste importante de `PoliticaNumeracion`
No usar `SepNum` único.

Deben existir:
- `SepPrefAnio`
- `SepAnioSerie`
- `SepSerieNro`

---

## Equipos y catálogos operativos

Hoy también quedó abierto y bastante bien orientado el bloque:

- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

### Alcoholímetro
Quedó firme:

- existe tabla de equipos
- debe ser versionada
- el equipo puede deshabilitarse
- la deshabilitación debe documentarse
- la UX mobile puede seleccionar el equipo en memoria temporal local o por QR
- eso no se persiste como “estado operativo previo” en central
- el acta referencia el equipo por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`

### Rubros
Quedó firme:

- usar:
  - `RubroCom`
  - `RubroComVersion`
- no usar nombre tipo snapshot para rubros
- la versión existe para sostener `IdRub + VerRub` en el acta

---

## Borrador / proceso / eventos

### EstadoProcesoActual
Se aceptó incluir:
- `0 = BORRADOR`

### TipoEvt
Se aceptó incluir:
- `0 = ACTA_CREADA_EN_BORRADOR`
- `1 = ACTA_LABRADA`
- `2 = BORRADOR_DESCARTADO`

### Regla conceptual
- el flujo administrativo real empieza cuando el acta queda labrada
- `BORRADOR` es un estado técnico previo y no contradice D1–D8

---

## Tema territorial / georreferenciación

Sigue siendo un tema activo y conectado al modelo.

### Domicilio de infracción
Se apoya en datos locales de Malvinas:
- `IdTca`
- `id_loc`
- `id_bar`
- `SiEjidoUrbano`
- lookup por calle + altura

### Domicilio del infractor
Se apoya en catálogos IGN/INDEC:
- provincia `SMALLINT`
- municipio `INT`
- departamento `INT`
- localidad `INT8`
- localidad censal `INT8`
- calle `INT8`

---

## Próximo paso

El siguiente paso lógico es **revisión de consistencia final** de los bloques ya armados y luego continuar con los bloques que faltan, probablemente en este orden:

1. revisar consistencia entre `04`, `05` y `06`
2. seguir con:
   - documentales
   - notificación
   - snapshot auxiliares / apoyo si hiciera falta
3. después:
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
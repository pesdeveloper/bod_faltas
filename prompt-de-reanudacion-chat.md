# Prompt de reanudación del chat

Estamos retomando el repo multiproyecto del sistema de faltas municipal.

## Reglas de trabajo

- Continuar en español.
- No rediseñar desde cero.
- Usar `spec/` como fuente principal de verdad.
- Mantener archivos chicos, claros, navegables y orientados a `spec-as-source`.
- No volver a discutir fundamentos ya cerrados salvo contradicción real.
- No inflar los documentos con explicación innecesaria.
- Priorizar definiciones compactas y útiles para implementación asistida.

---

## Estado consolidado que no se reabre salvo contradicción real

Ya quedaron consolidados y alineados:

- bloque base de overview
- reglas transversales principales
- bandejas canónicas
- `spec/01-dominio/`
- `spec/12-datos/`
- `spec/04-backend/`
- `spec/09-integraciones/`

### Decisiones cerradas importantes

- La unidad principal de gestión es Acta / Expediente.
- La notificación es un proceso transversal único.
- El snapshot es derivado y regenerable.
- Archivo y cerrada no son equivalentes.
- El motor de firma es externo al sistema de faltas y en este repo solo existe integración.
- `Dependencia` e `Inspector` forman parte del dominio.
- `Dependencia` e `Inspector` requieren versionado referencial para congelar contexto histórico sin snapshot textual embebido.
- `Talonario y numeración` es un mecanismo administrativo transversal del dominio.
- En backend se trabajará con SQL explícito.
- No se utilizará ORM como mecanismo principal de persistencia del núcleo transaccional.
- El storage documental debe resolverse desacoplado del documento lógico mediante `StorageKey`.
- La autenticación del sistema de faltas se integra con un IdP propio del ecosistema implementado con OpenIddict.

---

## Convención ya fijada para los spec DDL

### Estilo de spec
- tabla en formato tabular
- notas debajo de cada tabla
- enumeraciones explícitas en el mismo archivo donde se usan
- estilo compacto, claro y navegable

### Convención de nombres
- composición:
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

#### Contextos
- `Infr` = infracción
- `Infct` = infractor
- `Info` = información

#### Regla adicional
Si un campo no encaja exacto:
- hacerlo corto
- con criterio
- sin perder significado

### Archivo transversal acordado
- `spec/00-overview/03-convenciones-de-nomenclatura.md`

---

## Estado actual del DDL

Se trabajó fuerte sobre:

- `spec/13-ddl/01-convenciones-ddl-informix.md`
- `spec/13-ddl/02-tablas-nucleo-expediente.md`
- `spec/13-ddl/03-tablas-referenciales-y-versionado.md`
- `spec/13-ddl/04-tablas-acta-y-satelites.md`
- `spec/13-ddl/05-tablas-talonarios-y-numeracion.md`
- `spec/13-ddl/06-tablas-equipos-y-catalogos-operativos.md`

Todavía puede haber ajustes finos de consistencia, pero la base quedó bastante firme.

---

## Núcleo del acta ya alineado

### Tablas núcleo
- `Acta`
- `ActaSnapshot`
- `ActaEvento`
- `Observacion`

### Decisiones clave
- `NroActa` obligatorio en central
- `ActaSnapshot` separado 1:1
- `FhUltMod` e `IdUserUltMod` en snapshot
- `ActaEvento` append-only
- `Observacion` transversal con `VARCHAR(255)`
- `ObsActa` en `Acta` como texto propio largo

---

## Referenciales/versionado ya alineados

### Tablas
- `Dependencia`
- `DependenciaVersion`
- `Inspector`
- `InspectorVersion`
- `MedidaPreventiva`

### Decisiones clave
- `IdInsp` es `INT`
- `VerInsp` se mantiene
- el IdP emite claims construidos:
  - `InspectorId`
  - `InspectorVersion`
- el versionado del inspector queda desacoplado del maestro de usuarios
- el acta guarda `IdInsp + VerInsp`

---

## Satélites del acta ya alineados

### Tablas
- `ActaTransito`
- `ActaTransitoAlcoholemia`
- `ActaVehiculo`
- `ActaContravencion`
- `ActaSustanciasAlimenticias`
- `ActaMedidaPreventiva`

### Decisiones clave
- `ActaVehiculo` separada y reutilizable
- `TipoVehTxt` solo si `TipoVeh = OTRO`
- `ActaTransito` guarda resumen rápido
- `ActaTransitoAlcoholemia` guarda mediciones
- el equipo usado se referencia por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- en `ActaTransito` deben salir:
  - `NroSerieAlcoh`
  - `SiEqCalib`
  - `SiCalibVig`
  - `FhUltCalib`

### Ajuste importante en contravención
En `ActaContravencion` deben existir:
- `IdSuj SMALLINT`
- `IdBie INT`

Y `OrigenNomencl` debe quedar:
- `1 = OBTENIDA_COMERCIO`
- `2 = OBTENIDA_INMUEBLE`
- `3 = INGRESADA_MANUAL_VALIDADA`

Además debe quedar una nota explícita:
- comercio e inmueble pueden consultarse en maestros
- esa consulta puede precargar domicilio y nomenclatura
- el inspector puede corregir datos precargados si detecta inconsistencias

---

## Normativa / artículos / valores ya alineados

### Tablas
- `NormativaFaltas`
- `ArticuloNormativaFaltas`
- `TarifarioUnidadFaltas`
- `ActaArticuloInfringido`
- `ActaArticuloAuditoria`

### Decisiones clave
- los artículos no se resuelven como texto suelto en satélites
- el acta congela la instancia aplicada
- la auditoría de cambios manuales va separada de `Observacion`

---

## Talonarios / numeración ya bastante definidos

### Tablas
- `PoliticaNumeracion`
- `Talonario`
- `TalonarioDependencia`
- `TalonarioInsp`
- `TalonarioMovimiento`

### Decisiones clave
- no usar máscara libre
- la política se compone por partes:
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

---

## Equipos y catálogos operativos ya abiertos

### Tablas
- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

### Decisiones clave
- el alcoholímetro debe ser versionado
- el acta referencia el equipo por:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- la UX mobile puede seleccionar equipo localmente o por QR
- eso no requiere persistencia previa en central
- el equipo puede deshabilitarse y debe documentarse
- para rubros usar:
  - `RubroCom`
  - `RubroComVersion`

---

## Borrador / proceso / eventos

### EstadoProcesoActual
Debe incluir:
- `0 = BORRADOR`

### TipoEvt
Debe incluir al menos:
- `0 = ACTA_CREADA_EN_BORRADOR`
- `1 = ACTA_LABRADA`
- `2 = BORRADOR_DESCARTADO`

Regla:
- el flujo administrativo real empieza cuando el acta queda labrada
- `BORRADOR` es un estado técnico previo

---

## Tema territorial / georreferenciación

Sigue activo y conectado al modelo.

### Domicilio de infracción
Usa datos locales de Malvinas:
- `IdTca`
- `id_loc`
- `id_bar`
- `SiEjidoUrbano`
- lookup por calle + altura

### Domicilio del infractor
Usa catálogos IGN/INDEC:
- provincia `SMALLINT`
- municipio `INT`
- departamento `INT`
- localidad `INT8`
- localidad censal `INT8`
- calle `INT8`

---

## Próximo paso exacto

Al retomar:

1. revisar consistencia final entre `04`, `05` y `06`
2. verificar que `ActaTransito` haya quedado alineada con `AlcoholimetroVersion`
3. seguir con los bloques faltantes, probablemente:
   - documentales
   - notificación
   - auxiliares o snapshot de apoyo si hiciera falta
4. después:
   - diagrama relacional
   - SQL de creación

---

## Regla de continuación

- no rehacer `01-dominio`
- no rehacer `12-datos`
- no rehacer `04-backend`
- no rehacer `09-integraciones`
- usar esos bloques como base estable
- mantener el estilo tabular + notas + enumeraciones
- seguir con documentos compactos y útiles para implementación asistida
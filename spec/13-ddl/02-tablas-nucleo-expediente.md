# Tablas núcleo del expediente

## Finalidad

Este archivo define las tablas físicas principales del núcleo transaccional del expediente.

Incluye:

- `Acta`
- `ActaEvento`

---

# 1) TABLA: Acta

## Finalidad

Tabla principal del expediente.

Conserva identidad interna, identidad técnica, numeración visible, contexto principal y referencias estructurales necesarias para operación y trazabilidad.

---

## PK

- `PK_Acta (Id)`

---

## Uniques

- `UQ_Acta_IdTecnico`
- unicidad operativa de `NumeroActa` dentro de su contexto de numeración aplicable, a cerrar junto con numeración/talonarios

---

## FK esperadas

- `FK_Acta_Dependencia (IdDependencia -> Dependencia.Id)`
- `FK_Acta_DependenciaVersion (IdDependenciaVersion -> DependenciaVersion.Id)`
- `FK_Acta_Inspector (IdInspector -> Inspector.Id)`
- `FK_Acta_InspectorVersion (IdInspectorVersion -> InspectorVersion.Id)`
- `FK_Acta_TipoActa (IdTipoActa -> catálogo correspondiente)`
- `FK_Acta_EstadoActual (IdEstadoActa -> catálogo correspondiente)`
- `FK_Acta_OrigenActa (IdOrigenActa -> catálogo correspondiente)`
- `FK_Acta_Talonario (IdTalonario -> Talonario.Id)` cuando exista numeración asociada

---

## Campos

- `Id` → `INT8`
- `IdTecnico` → `CHAR(36)`
- `NumeroActa` → `VARCHAR(20)` NULL
- `IdTalonario` → `INT` NULL

- `IdDependencia` → `INT`
- `IdDependenciaVersion` → `INT8`
- `IdInspector` → `INT`
- `IdInspectorVersion` → `INT8`

- `IdTipoActa` → `SMALLINT`
- `IdEstadoActa` → `SMALLINT`
- `IdOrigenActa` → `SMALLINT`

- `FechaLabrado` → `DATETIME YEAR TO SECOND`
- `FechaAltaSistema` → `DATETIME YEAR TO SECOND`
- `FechaUltimaActualizacion` → `DATETIME YEAR TO SECOND` NULL

- `Observaciones` → `VARCHAR(255)` NULL

- `IdUserAlta` → `CHAR(36)`
- `IdUserUltMod` → `CHAR(36)` NULL

---

## Reglas

- `IdTecnico` es obligatorio y único.
- `NumeroActa` no reemplaza ni a `Id` ni a `IdTecnico`.
- `NumeroActa` puede ser `NULL` hasta que exista numeración administrativa asignada.
- `Acta` debe referenciar tanto la entidad principal como la versión aplicable de `Dependencia` e `Inspector`.
- `Acta` no debe absorber snapshot, historial detallado, documentos ni notificaciones.
- La unicidad operativa de numeración visible se cierra junto con numeración/talonarios.

---

## Observaciones

- `FechaLabrado` se modela con hora porque el momento exacto puede ser relevante.
- La información específica de tránsito, contravención, mercadería, decomiso u otras variantes no vive en esta tabla y debe bajar a tablas satélite.
- `Observaciones` se mantiene acotado y no debe transformarse en contenedor genérico del expediente.

---

# 2) TABLA: ActaEvento

## Finalidad

Tabla principal de trazabilidad relevante del expediente.

Conserva hechos, decisiones, transiciones, resultados e incidencias con valor funcional o administrativo.

---

## PK

- `PK_ActaEvento (Id)`

---

## Uniques

- no requiere unique global adicional por ahora

---

## FK esperadas

- `FK_ActaEvento_Acta (IdActa -> Acta.Id)`
- `FK_ActaEvento_TipoEvento (IdTipoEvento -> catálogo correspondiente)`

FK opcionales según el caso:

- `FK_ActaEvento_Documento (IdDocumento -> Documento.Id)`
- `FK_ActaEvento_Notificacion (IdNotificacion -> Notificacion.Id)`
- `FK_ActaEvento_Dependencia (IdDependencia -> Dependencia.Id)`
- `FK_ActaEvento_Inspector (IdInspector -> Inspector.Id)`

y otras FKs opcionales a medidas, integraciones o resultados, si luego se justifican.

---

## Campos

- `Id` → `INT8`
- `IdActa` → `INT8`
- `IdTipoEvento` → `SMALLINT`

- `FechaEvento` → `DATETIME YEAR TO SECOND`
- `FechaAltaSistema` → `DATETIME YEAR TO SECOND`

- `OrdenEvento` → `INT8`

- `IdDocumento` → `INT8` NULL
- `IdNotificacion` → `INT8` NULL
- `IdDependencia` → `INT` NULL
- `IdInspector` → `INT` NULL

- `Detalle` → `VARCHAR(255)` NULL

- `OrigenProceso` → `VARCHAR(50)` NULL
- `ReferenciaExterna` → `VARCHAR(100)` NULL

- `IdUserAlta` → `CHAR(36)`

---

## Reglas

- Todo `ActaEvento` pertenece obligatoriamente a una `Acta`.
- `FechaEvento` y `OrdenEvento` deben permitir reconstrucción consistente.
- `OrdenEvento` no reemplaza a la fecha, pero ayuda a desempatar y reconstruir secuencia.
- `Detalle` debe usarse solo como complemento, no como reemplazo de estructura.
- `ActaEvento` no debe usarse como bitácora técnica genérica.
- Una vez registrado, el evento debe tratarse con criterio conservador respecto de su modificación.

---

## Observaciones

- `OrdenEvento` conviene mantenerlo físico para simplificar reconstrucción.
- Las referencias opcionales deben mantenerse acotadas.
- Si un tipo de evento requiere demasiado detalle, debe bajar a tabla auxiliar específica.
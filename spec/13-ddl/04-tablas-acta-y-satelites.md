# Tablas acta y satélites

## Finalidad

Este archivo define las tablas satélite funcionales del acta y las tablas normalizadas directamente vinculadas a su contenido operativo y normativo.

Incluye:

- `ActaTransito`
- `ActaTransitoAlcoholemia`
- `ActaVehiculo`
- `ActaContravencion`
- `ActaSustanciasAlimenticias`
- `ActaMedidaPreventiva`
- `NormativaFaltas`
- `ArticuloNormativaFaltas`
- `TarifarioUnidadFaltas`
- `ActaArticuloInfringido`
- `ActaArticuloAuditoria`

Aunque estén documentadas en un archivo separado, estas tablas siguen perteneciendo al núcleo funcional del acta.

---

## Tabla: ActaTransito

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK a acta |
| `NroLic` | `CHAR(8)` | Sí | Número de licencia |
| `IdProvLic` | `SMALLINT` | Sí | Provincia emisora licencia |
| `VerProvLic` | `SMALLINT` | Sí | Versión provincia |
| `IdMuniLic` | `INT` | Sí | Municipio emisor licencia |
| `VerMuniLic` | `SMALLINT` | Sí | Versión municipio |
| `IdDptoLic` | `INT` | Sí | Departamento emisor licencia |
| `VerDptoLic` | `SMALLINT` | Sí | Versión departamento |
| `SiRetLic` | `SMALLINT` | No | 0/1 retención de licencia |
| `SiRetVeh` | `SMALLINT` | No | 0/1 retención de vehículo |
| `SiCtrlAlcoh` | `SMALLINT` | No | 0/1 si hubo control |
| `IdAlcoholimetro` | `INT` | Sí | Equipo utilizado |
| `VerAlcoholimetro` | `SMALLINT` | Sí | Versión del equipo utilizado |
| `TipoPruebaAlcohFin` | `SMALLINT` | Sí | Tipo de prueba final relevante |
| `CantMedAlcoh` | `SMALLINT` | Sí | Cantidad total de mediciones |
| `ResAlcohFin` | `DECIMAL(4,2)` | Sí | Resultado final |
| `UniMedAlcoh` | `CHAR(3)` | Sí | Unidad, normalmente `G/L` |

### Notas
- Esta tabla guarda el resumen rápido consultable de tránsito.
- El detalle de mediciones se resuelve en `ActaTransitoAlcoholemia`.
- `ResAlcohFin` y campos relacionados son el acceso rápido al resultado final operativo/legal.
- El equipo utilizado se congela mediante:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- Los datos históricos del equipo deben leerse desde `AlcoholimetroVersion`.

---

## Tabla: ActaTransitoAlcoholemia

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActa` | `INT8` | No | FK a acta |
| `OrdenMed` | `SMALLINT` | No | Orden de la medición |
| `TipoPrueba` | `SMALLINT` | No | Alómetro / alcoholímetro |
| `ResCuali` | `SMALLINT` | Sí | Resultado cualitativo |
| `ResNum` | `DECIMAL(4,2)` | Sí | Resultado numérico |
| `UniMed` | `CHAR(3)` | Sí | Unidad |
| `SiResFin` | `SMALLINT` | No | 0/1 si esta medición es la final |

### Notas
- Esta tabla representa el detalle de mediciones.
- Solo una fila por acta debe poder tener `SiResFin = 1`.
- Si `TipoPrueba = ALOMETRO`, el resultado numérico no debe aplicarse.
- Si `TipoPrueba = ALCOHOLIMETRO`, el resultado numérico sí puede aplicar y la unidad esperada es `G/L`.
- La cantidad de filas debe ser coherente con `CantMedAlcoh` en `ActaTransito`.

---

## Tabla: ActaVehiculo

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK a acta |
| `DomVeh` | `VARCHAR(10)` | Sí | Dominio/patente |
| `TipoVeh` | `SMALLINT` | Sí | Tipo de vehículo |
| `TipoVehTxt` | `VARCHAR(64)` | Sí | Solo si `TipoVeh = OTRO` |
| `MarcaVeh` | `VARCHAR(64)` | Sí | Marca |
| `ModeloVeh` | `VARCHAR(128)` | Sí | Modelo |

### Notas
- Tabla reutilizable para tránsito y sustancias alimenticias.
- `TipoVehTxt` no reemplaza el catálogo, solo cubre fallback controlado.

---

## Tabla: ActaContravencion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK a acta |
| `IdSuj` | `SMALLINT` | Sí | Tipo de sujeto origen de cuenta |
| `IdBie` | `INT` | Sí | Número de cuenta / bien municipal |
| `Circ` | `SMALLINT` | Sí | Circunscripción |
| `Secc` | `CHAR(2)` | Sí | Sección |
| `Frac` | `CHAR(7)` | Sí | Fracción |
| `Mza` | `CHAR(7)` | Sí | Manzana |
| `Parc` | `CHAR(7)` | Sí | Parcela |
| `UFun` | `CHAR(7)` | Sí | Unidad funcional |
| `UComp` | `CHAR(3)` | Sí | Unidad complementaria |
| `OrigenNomencl` | `SMALLINT` | Sí | Origen de nomenclatura |
| `IdRub` | `SMALLINT` | Sí | Rubro |
| `VerRub` | `SMALLINT` | Sí | Versión rubro |
| `AmbitoCtv` | `SMALLINT` | Sí | Ámbito contravención |
| `AmbitoCtvTxt` | `VARCHAR(80)` | Sí | Solo si ámbito = OTRO |

### Notas
- La nomenclatura catastral no se persiste como una cadena única sino como composición estructurada.
- `IdSuj` + `IdBie` identifican la cuenta municipal origen cuando la información proviene de lookup maestro.
- La consulta a maestros de comercio o inmueble puede precargar nomenclatura y domicilio.
- El inspector puede corregir los datos precargados si detecta inconsistencias o faltantes.
- `AmbitoCtvTxt` solo aplica si el ámbito es `OTRO`.
- Los artículos/normas infringidas no van embebidos acá.

---

## Tabla: ActaSustanciasAlimenticias

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK a acta |
| `IdRub` | `SMALLINT` | Sí | Rubro |
| `VerRub` | `SMALLINT` | Sí | Versión rubro |
| `AmbitoAct` | `SMALLINT` | Sí | Ámbito de actividad |
| `AmbitoActTxt` | `VARCHAR(80)` | Sí | Solo si ámbito = OTRO |

### Notas
- El vehículo se resuelve con `ActaVehiculo`.
- No se duplica la estructura vehicular en esta tabla.
- Los artículos/normas infringidas se resuelven por tabla normalizada instanciada por acta.

---

## Tabla: ActaMedidaPreventiva

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActa` | `INT8` | No | FK a acta |
| `IdMedPrev` | `INT` | No | Medida preventiva aplicada |
| `VerMedPrev` | `SMALLINT` | No | Versión de medida preventiva |
| `MedPrevTxt` | `VARCHAR(255)` | Sí | Texto adicional/explicativo |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Fecha/hora alta |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- No usar texto libre como reemplazo del catálogo.
- `MedPrevTxt` es complemento de la medida aplicada, no su dato principal.

---

## Tabla: NormativaFaltas

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdNorma` | `VARCHAR(8)` | No | Id norma |
| `VerNorma` | `SMALLINT` | No | Versión norma |
| `NomNorma` | `VARCHAR(64)` | No | Nombre norma |
| `SiTransito` | `SMALLINT` | No | 0/1 si aplica a tránsito |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Catálogo de normas aplicables.
- La versión permite congelar referencia normativa histórica.

---

## Tabla: ArticuloNormativaFaltas

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdNorma` | `VARCHAR(8)` | No | Norma padre |
| `VerNorma` | `SMALLINT` | No | Versión norma |
| `IdArtNorma` | `VARCHAR(8)` | No | Id artículo |
| `VerArtNorma` | `SMALLINT` | No | Versión artículo |
| `NomArtNorma` | `VARCHAR(64)` | No | Nombre/denominación artículo |
| `UniMedBase` | `SMALLINT` | Sí | Unidad de medida base |
| `ValorBase` | `DECIMAL(16,2)` | Sí | Valor base |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Catálogo de artículos por norma.
- No reemplaza la instancia aplicada en acta.

---

## Tabla: TarifarioUnidadFaltas

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdTarifario` | `INT` | No | PK |
| `UniMed` | `SMALLINT` | No | Unidad de medida |
| `Valor` | `DECIMAL(16,2)` | No | Valor vigente |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Permite mantener histórico de valores de unidad.
- La acta debe congelar el tarifario efectivamente aplicado cuando corresponda.

---

## Tabla: ActaArticuloInfringido

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActa` | `INT8` | No | FK a acta |
| `IdNorma` | `VARCHAR(8)` | No | Norma aplicada |
| `VerNorma` | `SMALLINT` | No | Versión norma |
| `IdArtNorma` | `VARCHAR(8)` | No | Artículo aplicado |
| `VerArtNorma` | `SMALLINT` | No | Versión artículo |
| `UniMedBase` | `SMALLINT` | Sí | Unidad de medida base |
| `UniMedApl` | `SMALLINT` | Sí | Unidad de medida aplicada |
| `IdTarifario` | `INT` | Sí | Tarifario usado |
| `ValorBase` | `DECIMAL(16,2)` | Sí | Valor base |
| `ValorApl` | `DECIMAL(16,2)` | Sí | Valor efectivamente aplicado |
| `SiActiva` | `SMALLINT` | No | 0/1 si el artículo sigue aplicando |
| `SiEditManual` | `SMALLINT` | No | 0/1 si fue ajustado manualmente |

### Notas
- Esta tabla representa la instancia concreta del artículo dentro del acta.
- No depende solo del catálogo, sino que congela el estado aplicado.
- Permite que la unidad o el valor efectivo difieran de la base si la operatoria lo admite.

---

## Tabla: ActaArticuloAuditoria

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdActaArt` | `INT8` | No | FK a artículo instanciado |
| `FhEvt` | `DATETIME YEAR TO SECOND` | No | Fecha/hora del cambio |
| `IdUserEvt` | `CHAR(36)` | No | Usuario que realizó la acción |
| `TipoAccion` | `SMALLINT` | No | Tipo de ajuste |
| `CampoMod` | `VARCHAR(40)` | No | Campo afectado |
| `ValorAnt` | `VARCHAR(255)` | Sí | Valor anterior |
| `ValorNvo` | `VARCHAR(255)` | Sí | Valor nuevo |
| `Motivo` | `VARCHAR(255)` | Sí | Justificación del cambio |

### Notas
- No mezclar esta auditoría con `Observacion`.
- Se trata de auditoría estructurada de cambios sobre artículos aplicados.
- `CampoMod` puede seguir siendo compacto porque la observación de la columna lo aclara funcionalmente.

---

## Enumeraciones del bloque

### TipoPruebaAlcohFin / TipoPrueba
- `1 = ALOMETRO`
- `2 = ALCOHOLIMETRO`

### ResCuali
- `1 = NEGATIVO`
- `2 = POSITIVO`
- `3 = INVALIDO`
- `4 = NO_REALIZADO`

### TipoVeh
- `1 = AUTOMOVIL`
- `2 = PICKUP_CAMIONETA`
- `3 = UTILITARIO_FURGON`
- `4 = CAMION`
- `5 = OMNIBUS_MINIBUS`
- `6 = MOTOCICLETA`
- `7 = CICLOMOTOR`
- `8 = TRICICLO_CUATRICICLO`
- `9 = ACOPLADO_REMOLQUE`
- `10 = MAQUINARIA_VIAL_ESPECIAL`
- `11 = MAQUINARIA_AGRICOLA`
- `12 = CASA_RODANTE`
- `99 = OTRO`

### OrigenNomencl
- `1 = OBTENIDA_COMERCIO`
- `2 = OBTENIDA_INMUEBLE`
- `3 = INGRESADA_MANUAL_VALIDADA`

### AmbitoCtv / AmbitoAct
- `1 = BALDIO`
- `2 = COMERCIO`
- `3 = INDUSTRIA`
- `4 = VIVIENDA`
- `5 = LOCAL`
- `9 = OTRO`

### UniMedBase / UniMedApl / UniMed
- `1 = UNIDAD_FIJA`
- `2 = SALARIO`
- `3 = IMPORTE_FIJO`

### TipoAccion
- `1 = ALTA`
- `2 = MODIFICACION_VALOR`
- `3 = MODIFICACION_UNIDAD`
- `4 = DESACTIVACION`
- `5 = REACTIVACION`
- `6 = AJUSTE_MANUAL`
- `7 = CORRECCION`
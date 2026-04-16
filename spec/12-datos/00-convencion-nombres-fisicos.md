# [12-DATOS] 00 - CONVENCIÓN DE NOMBRES FÍSICOS

## Finalidad

Este archivo define la convención oficial de nombres físicos para tablas, secuencias, índices, constraints y demás artefactos SQL del ecosistema del sistema de faltas municipal y sus componentes transversales asociados.

Su objetivo es:

- asegurar consistencia global del modelo físico
- evitar colisiones con otros dominios municipales
- mejorar legibilidad del SQL
- facilitar mantenimiento del repo
- alinear `spec/13-ddl/`, `sql/informix/base/` y `spec/14-sql-operativo/`

Esta convención debe considerarse obligatoria para todo artefacto físico nuevo.

---

## Principio general

Los nombres físicos deben expresar claramente **a qué capa funcional pertenece cada objeto**.

Para eso se adoptan prefijos cortos, estables y explícitos cuando corresponda.

Los prefijos oficiales vigentes son:

- `Fal` = dominio propio de faltas
- `Num` = numeración / talonarios transversal
- `Stor` = storage documental transversal

El formato general será:

- prefijo + nombre de entidad
- estilo `PascalCase`
- sin underscore entre prefijo y entidad

---

## Regla interna de longitud para identificadores auxiliares

Aunque Informix soporte identificadores más largos, este proyecto adopta una regla conservadora propia:

- nombres de índices: máximo `30` caracteres
- nombres de constraints: máximo `30` caracteres
- nombres de secuencias: máximo `30` caracteres

Esta regla busca:

- mejorar legibilidad
- evitar fricción con herramientas auxiliares
- facilitar mantenimiento del SQL
- sostener una convención estable y compacta

Las tablas pueden conservar nombres físicos claros mientras no excedan los límites reales del motor.

---

## Alcance de la convención

Esta convención aplica a:

- tablas
- secuencias
- índices
- constraints
- vistas físicas si existieran
- scripts SQL base
- referencias documentales de `spec/13-ddl/`
- referencias operativas de `spec/14-sql-operativo/`

---

## Prefijo `Fal` - dominio propio de faltas

Se usa `Fal` para todas las tablas cuyo ciclo de vida, significado y uso pertenecen principalmente al sistema de faltas.

Ejemplos correctos:

- `FalActa`
- `FalActaEvento`
- `FalActaEvidencia`
- `FalActaSnapshot`
- `FalActaTransito`
- `FalActaTransitoAlcoholemia`
- `FalActaVehiculo`
- `FalActaContravencion`
- `FalActaSustanciasAlimenticias`
- `FalActaMedidaPreventiva`
- `FalDocumento`
- `FalActaDocumento`
- `FalDocumentoFirma`
- `FalDocumentoObservacion`
- `FalNotificacion`
- `FalNotificacionIntento`
- `FalNotificacionAcuse`
- `FalNotificacionObservacion`
- `FalDependencia`
- `FalDependenciaVersion`
- `FalInspector`
- `FalInspectorVersion`
- `FalAlcoholimetro`
- `FalAlcoholimetroVersion`
- `FalObservacion`
- `FalMedidaPreventiva`
- `FalNormativaFaltas`
- `FalArticuloNormativaFaltas`
- `FalTarifarioUnidadFaltas`
- `FalActaArticuloInfringido`
- `FalActaArticuloAuditoria`

Para estas tablas, el prefijo `Fal` es obligatorio.

---

## Prefijo `Num` - numeración / talonarios transversal

Se usa `Num` para toda la capa transversal de numeración y talonarios, compartida entre faltas y otros sistemas del ecosistema municipal.

Ejemplos correctos:

- `NumPolitica`
- `NumTalonario`
- `NumTalonarioDependencia`
- `NumTalonarioInspector`
- `NumTalonarioMovimiento`

Para estas tablas, el prefijo `Num` es obligatorio.

---

## Prefijo `Stor` - storage documental transversal

Se usa `Stor` para toda la capa transversal de storage documental.

Ejemplos correctos:

- `StorBackend`
- `StorPolitica`
- `StorObjeto`

Para estas tablas, el prefijo `Stor` es obligatorio.

---

## Entidades compartidas existentes de otros sistemas

Si una entidad:

- ya existe físicamente en otro sistema
- tiene nombre real consolidado
- y faltas la reutiliza como entidad compartida

entonces debe **conservar su nombre físico real existente**.

Ejemplo explícito:

- `RubroCom` se mantiene como `RubroCom`
- `RubroComVersion` se mantiene como `RubroComVersion`

---

## Regla de clasificación general

La clasificación de nombres físicos queda así:

- si la tabla es propia del sistema de faltas, usa `Fal`
- si la tabla pertenece a la capa transversal de numeración, usa `Num`
- si la tabla pertenece a la capa transversal de storage, usa `Stor`
- si la tabla ya existe en otro sistema y se reutiliza como compartida, conserva su nombre físico real

---

## Regla para secuencias

Las secuencias deben reflejar el mismo criterio funcional de la tabla principal a la que sirven y respetar el límite de `30` caracteres.

Formato recomendado:

- `Seq` + alias corto de la entidad

Ejemplos correctos:

- `SeqFalActa`
- `SeqFalActEv`
- `SeqFalDoc`
- `SeqFalNotif`
- `SeqNumPol`
- `SeqNumTal`
- `SeqNumTalMov`
- `SeqStorBack`
- `SeqStorPol`
- `SeqStorObj`
- `SeqRubComVer`

---

## Regla para índices

Se adopta como preferencia principal el estilo descriptivo corto.

Si por longitud o practicidad el nombre descriptivo supera `30`, se usa fallback corto obligatorio.

Ejemplos válidos:

- `IxFalActa01`
- `UxFalActa01`
- `IxFalDoc01`
- `IxNumTal01`
- `IxStorObj01`
- `IxRubComVer01`

---

## Regla para constraints

Formato recomendado:

- PK: `Pk` + alias corto de tabla
- FK: `Fk` + alias corto tabla origen + `_` + alias corto tabla destino
- UQ: `Uq` + alias corto tabla + sufijo corto

Ejemplos válidos:

- `PkFalActa`
- `PkFalDoc`
- `PkNumTal`
- `PkStorObj`
- `PkRubComVer`
- `FkFalActEv_FalActa`
- `FkFalDoc_FalActa`
- `FkFalNotif_FalDoc`
- `FkNumTalDep_NumTal`
- `FkStorObj_StorBack`
- `FkRubComVer_RubCom`
- `UqFalActa01`
- `UqFalActa02`
- `UqStorObj01`

Todos deben respetar el máximo de `30` caracteres.

---

## Regla para columnas clave

La convención general de columnas se mantiene:

- PK principal: `Id`
- FK: `Id` + `Entidad`

Ejemplos correctos:

- `Id`
- `IdActa`
- `IdDocumento`
- `IdNotif`
- `IdDependencia`
- `IdInspector`
- `IdAlcoholimetro`
- `IdRubroCom`
- `IdTalonario`
- `IdPolitica`
- `IdBackend`
- `IdObjeto`

La tabla lleva prefijo físico cuando corresponda, pero la columna no necesita repetirlo.

---

## Regla para documentación en la spec

A partir de esta convención:

- `spec/13-ddl/` debe nombrar las tablas físicas con los prefijos oficiales (`Fal`, `Num`, `Stor`) cuando correspondan
- las entidades compartidas existentes deben conservar su nombre real
- `sql/informix/base/` debe generarse con esos nombres físicos
- `spec/14-sql-operativo/` debe referirse a tablas físicas con esos nombres cuando hable de persistencia concreta

---

## Regla de transición

Dado que parte de la spec y del SQL base fue redactada previamente sin estos prefijos o sin esta clasificación más fina, se establece la siguiente regla de transición:

1. la convención oficial a partir de ahora será:
   - `Fal` para faltas
   - `Num` para numeración / talonarios
   - `Stor` para storage documental
   - nombre real conservado para entidades compartidas existentes
2. se deberán ajustar progresivamente:
   - `spec/13-ddl/`
   - `sql/informix/base/`
   - `spec/14-sql-operativo/`
3. no deben seguir generándose artefactos nuevos con la convención vieja sin prefijo, ni con prefijos inconsistentes, ni con identificadores auxiliares largos que excedan la regla interna de `30`

---

## Decisión vigente

La convención oficial adoptada para el modelo físico es:

- `Fal` para el dominio propio de faltas
- `Num` para la capa transversal de numeración / talonarios
- `Stor` para la capa transversal de storage documental
- conservación del nombre físico real para entidades compartidas existentes provenientes de otros sistemas
- máximo interno de `30` caracteres para índices, constraints y secuencias

Formato:

- `PascalCase`
- sin underscore
- prefijo funcional obligatorio cuando corresponda

# Convenciones de nomenclatura

## Finalidad

Este archivo fija las convenciones generales de nomenclatura del proyecto.

Aplica a:

- tablas
- campos
- claves
- catálogos
- tablas satélite
- tablas snapshot
- tablas de auditoría
- documentación DDL y SQL asociada

Su objetivo es mantener una nomenclatura:

- compacta
- consistente
- legible
- cómoda para SQL
- suficientemente expresiva sin inflar nombres

---

## Principio general

Los nombres deben ser:

- cortos
- claros
- consistentes
- estables

No se deben usar nombres excesivamente largos salvo necesidad real.

La prioridad es lograr equilibrio entre:

- significado
- facilidad de lectura
- facilidad de escritura en SQL
- consistencia global del proyecto

---

## Regla general de composición

Regla recomendada:

- **prefijo + concepto + contexto**

Ejemplos:

- `IdActa`
- `IdDep`
- `VerDep`
- `IdTcaInfr`
- `VerLocInfct`
- `ObsDomInfct`
- `FhAlta`
- `FhUltMod`
- `CantDocsFirmados`

---

## Prefijos estándar

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

---

## Abreviaturas estables recomendadas

### Organización
- `Dep` = dependencia
- `Insp` = inspector

### Actuación / proceso
- `Acta` = acta
- `Snap` = snapshot
- `Evt` = evento
- `Docu` = documento
- `Notif` = notificación
- `Pres` = presentación
- `MedPrev` = medida preventiva

### Territorio / domicilio
- `Prov` = provincia
- `Muni` = municipio
- `Dpto` = departamento
- `Loc` = localidad
- `LocCen` = localidad censal
- `Tca` = calle catálogo municipal
- `Calle` = calle de fuente externa/general
- `Bar` = barrio
- `Dom` = domicilio
- `Alt` = altura
- `EjeUrb` = eje urbano

### Persona / identificación
- `Infct` = infractor
- `Infr` = infracción
- `Pers` = persona
- `Doc` = documento
- `Pref` = prefijo
- `DigVer` = dígito verificador

### Vehículo / licencia
- `Lic` = licencia
- `Veh` = vehículo
- `DomVeh` = dominio del vehículo
- `TipoVeh` = tipo de vehículo
- `MarcaVeh` = marca del vehículo
- `ModeloVeh` = modelo del vehículo

### Otros
- `Info` = información

---

## Regla de contexto

Cuando el nombre del campo necesite distinguir explícitamente el contexto, se deben usar sufijos consistentes.

Ejemplos:

- `Infr` = infracción
- `Infct` = infractor
- `Lic` = licencia

Ejemplos válidos:

- `IdTcaInfr`
- `AltInfr`
- `IdProvInfct`
- `IdMuniLic`

---

## Regla para IDs y versiones

Siempre que exista identidad versionada, usar:

- `Id[Entidad]`
- `Ver[Entidad]`

Ejemplos:

- `IdDep`
- `VerDep`
- `IdInsp`
- `VerInsp`
- `IdTcaInfr`
- `VerTcaInfr`

---

## Regla para flags

Los flags deben comenzar con `Si`.

Ejemplos:

- `SiActiva`
- `SiEjeUrb`
- `SiDomTxtInfr`
- `SiRetLic`
- `SiEditManual`

---

## Regla para fechas

Usar prefijo `Fh`.

Ejemplos:

- `FhAlta`
- `FhUltMod`
- `FhEvt`
- `FhSnapshot`

---

## Regla para cantidades

Usar prefijo `Cant`.

Ejemplos:

- `CantDocsFirmados`
- `CantDocsPendFirma`
- `CantMedAlcoh`

---

## Regla para observaciones y descripciones

- `Obs` para observaciones
- `Desc` para descripciones

Ejemplos:

- `ObsActa`
- `ObsDomInfct`
- `DescMedPrev`
- `DescInfo`

---

## Regla cuando no exista abreviatura estándar

Si un caso no encaja exactamente en la convención estándar:

- resolver con un nombre corto
- mantener el significado
- priorizar la lectura rápida
- evitar nombres narrativos largos
- evitar inventar abreviaturas oscuras sin necesidad

La convención guía, pero no debe rigidizar el diseño de forma artificial.

---

## Regla para fuentes distintas

No mezclar identificadores de distinta fuente bajo el mismo nombre.

Ejemplo:

- usar `IdTca...` para calle del catálogo municipal/catastral
- usar `IdCalle...` para calle proveniente de fuente externa como INDEC/IGN

Esto evita ambigüedad semántica.

---

## Ejemplos recomendados

- `IdActa`
- `IdDep`
- `VerDep`
- `IdInsp`
- `VerInsp`
- `IdTcaInfr`
- `VerTcaInfr`
- `AltInfr`
- `IdLocInfr`
- `VerLocInfr`
- `SiDomTxtInfr`
- `DomTxtInfr`
- `IdProvInfct`
- `VerProvInfct`
- `IdDptoInfct`
- `VerDptoInfct`
- `IdLocInfct`
- `VerLocInfct`
- `IdLocCenInfct`
- `VerLocCenInfct`
- `IdCalleInfct`
- `VerCalleInfct`
- `ObsDomInfct`
- `IdProvLic`
- `VerProvLic`
- `IdMuniLic`
- `VerMuniLic`
- `IdDptoLic`
- `VerDptoLic`

---

## Ejemplos a evitar

- nombres excesivamente largos
- nombres ambiguos
- mezcla inconsistente de castellano e inglés
- abreviaturas cambiantes para el mismo concepto
- prefijos distintos para la misma idea según la tabla

Ejemplos problemáticos:

- `ProvinciaInfractorId`
- `provincia_id_infractor`
- `IdProvinciaDelInfractor`
- `IdProvInfractor`
- `IdPrvInf`

---

## Regla documental

Cuando un nombre corto pueda no ser evidente para alguien no habituado al modelo:

- debe acompañarse de una observación breve en el spec
- la observación debe explicar el significado funcional mínimo del campo

---

## Resultado esperado

Toda nueva tabla o campo del proyecto debe respetar esta convención salvo excepción justificada.
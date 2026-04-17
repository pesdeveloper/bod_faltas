# [14-SQL-OPERATIVO] 04 - SQL CRUD ACTA

## Finalidad

Este archivo documenta las operaciones SQL principales del agregado `FalActa` y sus satélites inmediatos.

Relación importante:
- política de domicilios y lookups territoriales → [`03a-sql-lookups-domicilio-infractor.md`](./03a-sql-lookups-domicilio-infractor.md)
- municipio emisor de licencia y jurisdicción → [`03b-sql-lookups-licencia-y-jurisdiccion.md`](./03b-sql-lookups-licencia-y-jurisdiccion.md)

---

## Operaciones principales

### 1. Crear acta

Tablas involucradas:

- `FalActa`
- satélite inicial según tipo si corresponde
- `FalActaEvidencia` si corresponde
- `FalActaEvento`
- `FalActaSnapshot`

Secuencia principal:
- `SeqFalActa`

Orden base:

1. obtener `Id` de `FalActa`
2. insertar `FalActa`
3. insertar satélites presentes
4. insertar evidencias presentes
5. insertar evento inicial
6. insertar o actualizar snapshot mínimo
7. confirmar transacción

### 2. Obtener detalle de acta

Lectura compuesta, preferentemente separada en:

- cabecera de `FalActa`
- satélites por tipo
- evidencias
- historial resumido
- resumen snapshot

### 3. Agregar evidencia

Tablas:
- `FalActaEvidencia`
- opcionalmente `FalActaEvento`
- `FalActaSnapshot` si impacta operación visible

### 4. Registrar evento

Tabla:
- `FalActaEvento`

Regla:
- append-only
- no reescribir eventos previos

### 5. Actualizar dato mutable permitido

Ejemplo:
- corrección controlada de metadato no histórico

---

## Tratamiento de domicilios dentro del alta de acta

### Domicilio del infractor

Debe soportar:

- shape nacional común
- calle textual libre cuando no exista catálogo
- flag de normalización parcial
- referencias locales finas de Malvinas cuando el domicilio del infractor sea de Malvinas

Esto implica que el alta de `FalActa` no debe asumir una única estrategia de persistencia de domicilio.

### Domicilio de la infracción

Se mantiene preferentemente normalizado con referencias locales/municipales y soporte de GPS cuando exista.

### Municipio emisor de licencia

Debe persistirse como jurisdicción emisora distinguiendo internamente entre:

- municipio real
- departamento fallback

aunque en UX el lookup sea unificado.

---

## Mapeo de persistencia en `FalActa`

Para que la operación de alta sea implementable, la información de domicilios y licencias se persiste en `FalActa` usando las siguientes columnas.

### Domicilio del infractor (contexto `Infct`)

#### Soporte nacional (IGN / INDEC)

- `IdProvInfct`, `VerProvInfct`
- `IdDptoInfct`, `VerDptoInfct`
- `IdMuniInfct`, `VerMuniInfct` (`nullable`)
- `IdLocInfct`, `VerLocInfct`
- `IdLocCenInfct`, `VerLocCenInfct`
- `IdCalleInfct`, `VerCalleInfct` (`nullable`, si se normaliza)

#### Soporte local Malvinas

- `IdLocMalvInfct`, `VerLocMalvInfct`
- `IdTcaInfct`, `VerTcaInfct`
- `IdBarInfct`

#### Soporte común y texto libre

- `AltInfct`
- `PisoInfct`
- `DeptoInfct`
- `ObsDomInfct`
- `CodPosInfct`
- `SiCalleTxtInfct`
- `CalleTxtInfct`
- `SiNormParcialInfct`

### Jurisdicción emisora de licencia (contexto `LicEmi`)

- `IdProvLicEmi`, `VerProvLicEmi`
- `IdDptoLicEmi`, `VerDptoLicEmi`
- `IdMuniLicEmi`, `VerMuniLicEmi` (`nullable`)
- `TipoJurLicEmi`

### Regla de uso

#### Domicilio del infractor fuera de Malvinas

Debe persistir principalmente:

- provincia
- municipio o departamento fallback
- localidad
- localidad censal
- calle externa si existe
- calle textual libre si no existe normalización
- altura y complementos
- código postal si se dispone
- flag de normalización parcial si corresponde

#### Domicilio del infractor en Malvinas

Debe persistir:

- shape nacional cuando aplique
- y además soporte local fino:
  - localidad Malvinas
  - calle municipal
  - barrio local si se resolvió
- calle textual libre si no hubo resolución de catálogo
- código postal si se dispone
- flag de normalización parcial si corresponde

#### Municipio emisor de licencia

Debe persistirse siempre:

- provincia
- municipio real si existe
- departamento fallback si no existe municipio aplicable
- tipo de jurisdicción emisora en `TipoJurLicEmi`

---

## Satélites por tipo

Según tipo de acta, el agregado puede involucrar:

- `FalActaTransito`
- `FalActaTransitoAlcoholemia`
- `FalActaVehiculo`
- `FalActaContravencion`
- `FalActaSustanciasAlimenticias`
- `FalActaMedidaPreventiva`
- `FalActaArticuloInfringido`
- `FalActaArticuloAuditoria`

La operación de alta debe dejar explícito qué satélites son obligatorios y cuáles opcionales.

---

## Lecturas principales

### Listado corto administrativo

Base recomendada:
- `FalActaSnapshot`
- join corto a `FalActa`

### Detalle completo

Se recomienda composición en backend a partir de múltiples queries cortas, no una query monolítica.

### Historial

Base:
- `FalActaEvento`

Orden:
- fecha ascendente o descendente según la pantalla

---

## Riesgos a controlar

- alta parcial de acta sin satélites obligatorios
- pérdida del `Id` padre
- mezcla de corrección mutable con reescritura de historial
- recalcular de más el snapshot en cada operación mínima
- asumir un único modelo de domicilio del infractor cuando el diseño exige soporte nacional + local Malvinas
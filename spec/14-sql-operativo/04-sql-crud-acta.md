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

Debe persistirse como jurisdicción emisora distinguida internamente entre:

- municipio real
- departamento fallback

aunque en UX el lookup sea unificado.

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

---

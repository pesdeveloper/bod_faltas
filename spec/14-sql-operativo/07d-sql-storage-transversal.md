# [14-SQL-OPERATIVO] 07D - SQL STORAGE TRANSVERSAL

## Finalidad

Este archivo documenta las operaciones SQL principales del storage transversal.

Tablas:

- `StorBackend`
- `StorPolitica`
- `StorObjeto`

---

## Reglas generales

- El storage es transversal y no debe confundirse con el documento lógico.
- Debe soportar backends, políticas, objetos persistidos, resolución de política efectiva y lookup por `StorageKey`.
- `StorObjeto` representa el archivo físico y no reemplaza a `FalDocumento`.

---

## Operaciones principales

### 1. Alta de backend

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**

#### Secuencia principal
- secuencia de `StorBackend`

#### Shape mínimo requerido
Debe incluir, como mínimo:

- identificación del backend
- tipo de backend
- parámetros mínimos de operación
- vigencia o estado
- auditoría mínima

---

### 2. Alta de política de storage

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**

#### Secuencia principal
- secuencia de `StorPolitica`

#### Shape mínimo requerido
Debe incluir, como mínimo:

- backend asociado
- ámbito de aplicación
- reglas de resolución
- vigencia
- auditoría mínima

---

### 3. Registrar objeto físico

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**

#### Secuencia principal
- secuencia de `StorObjeto`, si la tabla la usa
- o generación controlada de `StorageKey`, según el diseño físico real

#### Shape mínimo requerido
Debe incluir, como mínimo:

- `StorageKey`
- backend o política resuelta
- metadatos mínimos del objeto
- auditoría mínima

#### Regla operativa
`StorObjeto` representa el archivo físico y no reemplaza a `FalDocumento`.

---

### 4. Resolver política efectiva de storage

#### Regla de lectura
Debe poder resolverse, como mínimo:

- política por sistema / familia / tipo
- fallback correspondiente
- backend resultante

---

### 5. Obtener objeto por `StorageKey`

#### Regla de lectura
Debe permitir:

- localizar objeto físico
- obtener metadatos relevantes
- validar existencia lógica del objeto

---

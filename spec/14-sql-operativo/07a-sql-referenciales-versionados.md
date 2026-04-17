# [14-SQL-OPERATIVO] 07A - SQL REFERENCIALES VERSIONADOS

## Finalidad

Este archivo documenta las operaciones SQL principales de referenciales propios de faltas y entidades versionadas.

Tablas principales:

- `FalDependencia`
- `FalDependenciaVersion`
- `FalInspector`
- `FalInspectorVersion`
- `FalAlcoholimetro`
- `FalAlcoholimetroVersion`
- `FalMedidaPreventiva`

---

## Operaciones principales

### 1. Alta de entidad raíz con primera versión

#### Patrón transaccional aplicable
- patrón fundamental: **Alta de agregado padre/hijos**

#### Secuencias principales
- secuencia de la entidad raíz
- secuencia de la tabla versionada correspondiente, si aplica

#### Shape mínimo requerido
Debe incluir, como mínimo:

- datos de identidad de la raíz
- datos de la primera versión
- vigencia inicial
- auditoría mínima obligatoria

#### Orden base
1. obtener `Id` de la raíz
2. insertar raíz
3. obtener `Id` de la versión si corresponde
4. insertar primera versión vigente
5. confirmar transacción

#### Casos típicos
- dependencia + primera versión
- inspector + primera versión
- alcoholímetro + primera versión

---

### 2. Cierre de versión vigente y nueva versión

#### Patrón transaccional aplicable
- patrón fundamental: **Versionado con cierre y nueva versión**

#### Shape mínimo requerido
Debe incluir, como mínimo:

- referencia a la entidad raíz
- identificación de la versión vigente
- datos de la nueva versión
- nueva vigencia
- auditoría mínima obligatoria

#### Orden base
1. localizar versión vigente
2. cerrar vigencia previa
3. insertar nueva versión
4. confirmar transacción

#### Regla operativa
La lectura “vigente” debe quedar unívoca después de la operación.

---

### 3. Lectura de vigente

#### Regla de lectura
La lectura operativa normal debe recuperar:

- raíz
- versión vigente
- datos mínimos de uso operativo

#### Casos típicos
- dependencia vigente
- inspector vigente
- alcoholímetro vigente
- catálogo activo de medidas preventivas

---

### 4. Lectura histórica

#### Regla de lectura
Cuando el caso de uso requiera trazabilidad, debe poder resolverse:

- versión por fecha
- versión por identificador
- historial de versiones por entidad raíz

---

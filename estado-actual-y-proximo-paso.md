# Estado actual y próximo paso

## Estado actual

El repo quedó bastante más ordenado y coherente que al inicio de esta pasada.

Se cerraron decisiones importantes de naming, estructura y separación de responsabilidades, y además se reordenaron varios bloques para que sean más compatibles con un enfoque `spec-as-source`.

---

## 1. Convención física de nombres

Archivo canónico:
- `spec/12-datos/00-convencion-nombres-fisicos.md`

### Decisión vigente

Prefijos oficiales:

- `Fal` = dominio propio de faltas
- `Num` = numeración / talonarios transversal
- `Stor` = storage documental transversal

Además:

- entidades compartidas existentes conservan nombre real
- ejemplo:
  - `RubroCom`
  - `RubroComVersion`

### Regla adicional vigente

Para este proyecto se fijó una restricción interna de naming:

- índices, constraints y secuencias: **máximo 30 caracteres**

---

## 2. Estado de `spec/13-ddl`

El bloque `13-ddl` fue regenerado/ajustado y además se reorganizó la parte territorial.

### Parte territorial actual

- `11-tablas-territoriales-externas-introduccion.md`
- `12-tablas-territoriales-ign-indec.md`
- `13-tablas-territoriales-malvinas-locales.md`
- `14-tablas-georreferenciacion-territorial.md`

### Estado de cada uno

#### `11`
Archivo introductorio y marco general.

#### `12`
Documenta tablas IGN / INDEC que faltas consume para resolución territorial.

#### `13`
Documenta tablas locales de Malvinas para resolución fina del domicilio.

#### `14`
Archivo de georreferenciación territorial creado, pero pendiente de completarse con tablas reales PostGIS.

---

## 3. Estado de `sql/informix/base`

`sql/informix/base/` fue regenerado con la convención nueva:

- `Fal*`
- `Num*`
- `Stor*`
- `RubroCom` / `RubroComVersion`
- nombres auxiliares cortos

Después se hizo otra pasada para alinear el SQL base con las decisiones nuevas de domicilios y licencia.

Quedó pendiente solo la validación final del usuario y, si hiciera falta, una última corrección quirúrgica.

---

## 4. Estado de `spec/14-sql-operativo`

El bloque fue compactado y reorganizado.

### Estructura actual

- `00-metodologia-sql-operativo.md`
- `01-patrones-transaccionales.md`
- `02-sql-bandejas.md`
- `03-sql-formularios-y-lookups.md`
- `03a-sql-lookups-domicilio-infractor.md`
- `03b-sql-lookups-licencia-y-jurisdiccion.md`
- `03c-sql-lookups-catalogos-y-validaciones.md`
- `04-sql-crud-acta.md`
- `05-sql-crud-documental.md`
- `06-sql-crud-notificacion.md`
- `07-sql-crud-referenciales-y-transversales.md`
- `08-sql-proyecciones-y-reproceso.md`

### Estado funcional

- `00` funciona como hub corto
- `03` funciona como hub corto
- `03a/03b/03c` separan detalle de lookups
- el bloque quedó mucho más navegable y menos pesado

---

## 5. Estado del tema domicilios

Este es el punto funcional más importante del momento.

### Domicilio del infractor

#### Decisión vigente
Se eligió **opción B**:

- mantener el shape nacional
- y además persistir soporte local fino de Malvinas para el domicilio del infractor cuando corresponda

#### Si es de Malvinas Argentinas
Resolución principal con:

- `localidad`
- `calle`
- `geo_calle_alturas_barrio`

apoyado por:

- `barrio`
- `manzana`
- `callexmza`

#### Si no es de Malvinas Argentinas
Resolución con:

- `geo_ign_provincia`
- `geo_ign_municipio`
- `geo_ign_departamento`
- `geo_indec_localidad`
- `geo_indec_localidad_censal`
- `geo_indec_calles`

con apoyo opcional de:

- `geo_bahra_asentamiento`

### Reglas ya fijadas

- UX simple
- municipio lógico
- fallback de municipio a departamento
- búsqueda por prefijo
- calle no encontrada → texto libre permitido
- no modificar catálogos externos desde faltas
- validación parcial admitida

### Persistencia acordada

Se decidió contemplar, además del shape nacional:

- soporte local fino Malvinas para el infractor
- calle textual libre del infractor
- flag de normalización parcial del domicilio del infractor

---

## 6. Municipio emisor de licencia

También quedó definido que debe persistirse:

- provincia
- municipio real si existe
- departamento fallback si no hay municipio
- tipo de jurisdicción emisora

Aunque en UX siga presentándose como un único campo lógico con fallback.

---

## 7. Estado de georreferenciación

Se decidió separar lookup territorial tabular de georreferenciación/GIS.

Archivo actual:
- `spec/13-ddl/14-tablas-georreferenciacion-territorial.md`

### Estado
- creado
- orientación funcional ya documentada
- pendiente de completarse cuando estén cargadas las capas reales en PostGIS

---

## 8. Punto exacto donde quedó el trabajo

En este momento:

- el usuario va a revisar `spec/14-sql-operativo`
- ya se regeneraron archivos de `13-ddl`, `14-sql-operativo` y `sql/informix/base` para reflejar cambios de domicilios y licencia
- falta la pasada fina posterior a la revisión del usuario
- georreferenciación queda para completar con datos reales del servidor GIS

---

## Próximo paso recomendado

### Paso inmediato
Esperar / revisar observaciones del usuario sobre `spec/14-sql-operativo`.

### Luego
Ajustar si hace falta los archivos regenerados por domicilios y licencia.

### Después
Completar:

- `spec/13-ddl/14-tablas-georreferenciacion-territorial.md`

cuando estén cargadas las capas reales en PostGIS.

### Luego
Empezar a bajar de spec a SQL operativo más concreto / cercano a implementación real.

---

## Criterio de continuidad

Seguir trabajando con estas reglas:

- no rediseñar desde cero
- usar `spec/` como fuente de verdad
- archivos compactos y navegables
- hubs cortos con archivos satélite cuando convenga
- links relativos navegables entre archivos
- correcciones quirúrgicas y progresivas
- foco práctico para implementación real

---
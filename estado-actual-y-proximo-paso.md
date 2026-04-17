# Estado actual y próximo paso

## Estado actual

El repo quedó bastante más ordenado, consistente y navegable que al inicio de esta pasada.

Se cerraron decisiones importantes de naming, estructura y separación de responsabilidades, y además se terminó una revisión completa del bloque `spec/14-sql-operativo` para alinearlo con un enfoque `spec-as-source`.

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
Documenta tablas IGN / INDEC consumidas por faltas para resolución territorial.

#### `13`
Documenta tablas locales de Malvinas para resolución fina del domicilio.

#### `14`
Archivo de georreferenciación territorial ya completado con las capas reales de Geomática municipal en PostGIS.

---

## 3. Estado de GIS / PostGIS

Se adoptó el prefijo:

- `geo_gmat_*`

para capas geoespaciales municipales oficiales.

### Proyección

- `EPSG:22195`
- Campo Inchauspe / Argentina 5

### Capas GIS documentadas

- `geo_gmat_localidad`
- `geo_gmat_barrio`
- `geo_gmat_manzana`
- `geo_gmat_parcela`
- `geo_gmat_nomenclatura`
- `geo_gmat_calle`

### Estado funcional

Quedó documentado:

- el rol de cada capa
- la jerarquía espacial entre capas
- reglas de resolución espacial
- uso de `ST_Transform` cuando el origen GPS venga en WGS84
- el hecho de que `geo_gmat_nomenclatura` es capa de etiqueta/punto y no reemplaza a la parcela

---

## 4. Estado de `sql/informix/base`

`sql/informix/base/` fue regenerado con la convención nueva:

- `Fal*`
- `Num*`
- `Stor*`
- `RubroCom` / `RubroComVersion`
- nombres auxiliares cortos

Después se hicieron pasadas adicionales para alinear el SQL base con decisiones nuevas de:

- domicilios
- licencia
- documental
- notificación

Queda pendiente solo la validación final del usuario si, al bajar a implementación real, aparece alguna corrección quirúrgica más.

---

## 5. Estado de `spec/14-sql-operativo`

El bloque fue revisado completo y quedó mucho más compacto, navegable y accionable.

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
- `07a-sql-referenciales-versionados.md`
- `07b-sql-rubros-compartidos.md`
- `07c-sql-numeracion-transversal.md`
- `07d-sql-storage-transversal.md`
- `08-sql-proyecciones-y-reproceso.md`

### Resultado funcional

- `00` funciona como hub corto
- `03` funciona como hub corto
- `07` fue partido en hub + satélites
- `01`, `04`, `05` y `06` quedaron mucho más accionables
- `02` quedó correctamente en nivel operativo, sin congelar aún filtros SQL literales de bandeja
- `08` quedó reforzado con criterio operativo de actualización y reproceso

### Estado general del bloque

`spec/14-sql-operativo` puede considerarse **revisado por completo** por ahora.

---

## 6. Estado del tema domicilios

Este fue uno de los puntos funcionales más importantes de la pasada.

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

## 7. Municipio emisor de licencia

También quedó definido que debe persistirse:

- provincia
- municipio real si existe
- departamento fallback si no hay municipio
- tipo de jurisdicción emisora

Aunque en UX siga presentándose como un único campo lógico con fallback.

---

## 8. Punto exacto donde quedó el trabajo

En este momento:

- `spec/14-sql-operativo` ya quedó revisado completo
- GIS/PostGIS ya quedó documentado a nivel spec
- el siguiente paso ya no está en revisión general de spec
- el proyecto está listo para empezar a bajar a implementación real guiada por spec
- el foco natural pasa a `backend/`

---

## 9. Próximo paso recomendado

### Paso inmediato
Empezar a trabajar en `backend/` con casos de uso concretos.

### Prioridades sugeridas

1. lookups territoriales
2. alta de acta
3. circuito documental
4. circuito de notificación

### Estrategia recomendada

- `spec/` sigue siendo la fuente de verdad
- Byte diseña y ordena
- Cursor implementa
- Gemini hace revisión crítica
- las propuestas de Gemini se aceptan solo si mejoran realmente el proyecto

---

## 10. Criterio de continuidad

Seguir trabajando con estas reglas:

- no rediseñar desde cero
- usar `spec/` como fuente de verdad
- archivos compactos y navegables
- hubs cortos con archivos satélite cuando convenga
- links relativos navegables entre archivos
- correcciones quirúrgicas y progresivas
- foco práctico para implementación real

---
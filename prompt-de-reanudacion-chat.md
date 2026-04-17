# Prompt de reanudación del chat

Retomamos el repo multiproyecto del sistema de faltas municipal.

Antes de seguir, abrir también:
- [`estado-actual-y-proximo-paso.md`](./estado-actual-y-proximo-paso.md)

## Regla general de trabajo

- Continuar en español.
- No rediseñar desde cero.
- Usar `spec/` como fuente principal de verdad.
- Mantener estilo compacto, claro, navegable y orientado a `spec-as-source`.
- No volver a discutir fundamentos ya cerrados salvo contradicción real.
- Priorizar correcciones quirúrgicas y archivos completos copy-paste o paquetes descargables cuando haga falta.
- Priorizar navegación entre archivos por links relativos cuando sea útil.
- Evitar documentos innecesariamente largos; partir por responsabilidad cuando convenga.
- Cuando un archivo deba funcionar como hub, debe ser corto, navegable y con links a archivos satélite.

---

## Estado general al retomar

Se consolidó una pasada fuerte de alineación entre:

- `spec/`
- `spec/13-ddl/`
- `sql/informix/base/`
- `spec/14-sql-operativo/`

Ya se revisaron, corrigieron o regeneraron bloques importantes del repositorio.

El trabajo reciente se concentró en:

1. convención física de nombres
2. renombre y regeneración de `13-ddl`
3. regeneración de `sql/informix/base`
4. compactación y reestructuración de `14-sql-operativo`
5. modelado/documentación de tablas territoriales y lookups de domicilios
6. ajuste de persistencia del domicilio del infractor y jurisdicción emisora de licencia
7. revisión completa del bloque `spec/14-sql-operativo`

---

## Decisiones firmes vigentes

### Convención física

Prefijos oficiales:

- `Fal` = dominio propio de faltas
- `Num` = numeración / talonarios transversal
- `Stor` = storage documental transversal

Además:

- entidades compartidas existentes conservan su nombre real
- ejemplo:
  - `RubroCom`
  - `RubroComVersion`

Archivo canónico:
- `spec/12-datos/00-convencion-nombres-fisicos.md`

### Restricción interna de nombres auxiliares

Aunque Informix soporte identificadores más largos, el proyecto fija como regla interna:

- nombres de índices, constraints y secuencias: **máximo 30 caracteres**

Esto ya obligó a regenerar naming físico en `13-ddl` y `sql/informix/base`.

### Storage y numeración son transversales

No pertenecen solo a faltas.

Por eso:

- tablas de numeración/talonarios → `Num*`
- tablas de storage → `Stor*`

### RubroCom es compartido

- `RubroCom` pertenece al sistema actual de ingresos
- no debe renombrarse a `FalRubroCom`
- `RubroComVersion` debe existir para compatibilidad/versionado, pero conservando familia nominal compartida

---

## Estado actual de `spec/13-ddl`

`spec/13-ddl/` fue reorganizado y ajustado.

La parte territorial quedó separada así:

- `11-tablas-territoriales-externas-introduccion.md`
- `12-tablas-territoriales-ign-indec.md`
- `13-tablas-territoriales-malvinas-locales.md`
- `14-tablas-georreferenciacion-territorial.md`

### Estado de esos archivos

#### 11
Introducción y marco general de tablas territoriales externas y compartidas.

#### 12
Documenta tablas IGN / INDEC consumidas por faltas.

#### 13
Documenta tablas locales de Malvinas para resolución fina de domicilios.

#### 14
Quedó creado como archivo base de georreferenciación territorial, pero **pendiente de completarse** cuando estén cargadas las capas reales en PostGIS.

---

## Estado actual de `spec/14-sql-operativo`

El bloque fue revisado completo y quedó mucho más alineado con `spec-as-source`.

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

### Criterios aplicados

- `00` quedó como hub corto y navegable
- `03` quedó como hub corto
- el detalle de formularios/lookups se separó en `03a`, `03b`, `03c`
- `07` se partió en hub + satélites para hacerlo más compacto y usable
- `01`, `04`, `05` y `06` quedaron bastante más accionables
- `02` quedó en nivel operativo correcto, sin congelar todavía `WHERE` literales definitivos
- `08` quedó reforzado con criterio operativo de actualización y reproceso

---

## Estado actual del tema domicilios

Este es uno de los puntos más importantes ya cerrados.

### Domicilio del infractor

Se tomó la decisión de ir con **opción B**:

- mantener el shape nacional común
- y además persistir referencias locales de Malvinas cuando el domicilio del infractor sea de Malvinas Argentinas

### Domicilio del infractor en Malvinas Argentinas
Se resuelve con tablas locales:

- `localidad`
- `calle`
- `geo_calle_alturas_barrio`

apoyadas por:

- `barrio`
- `manzana`
- `callexmza`

### Domicilio del infractor fuera de Malvinas Argentinas
Se resuelve con stack IGN / INDEC:

- `geo_ign_provincia`
- `geo_ign_municipio`
- `geo_ign_departamento`
- `geo_indec_localidad`
- `geo_indec_localidad_censal`
- `geo_indec_calles`

con apoyo opcional de:

- `geo_bahra_asentamiento`

### Reglas cerradas

- en UX siempre existe municipio lógico
- el fallback puede resolver municipio real o departamento
- si la calle no existe en catálogo:
  - se permite texto libre
  - no se agrega al catálogo
  - no se corrige el origen
  - el domicilio queda parcialmente normalizado

### Persistencia acordada para domicilio del infractor

Además del shape nacional, se decidió contemplar:

- soporte local fino Malvinas para el infractor
- calle textual libre del infractor
- flag de normalización parcial del domicilio del infractor

---

## Municipio emisor de licencia

También quedó mejor definido:

- persistir provincia
- persistir municipio real si existe
- persistir departamento fallback si no hay municipio
- persistir tipo de jurisdicción emisora

Aunque en UX siga viéndose como un único campo con fallback.

---

## Estado de georreferenciación

Se decidió separar georreferenciación/GIS del lookup territorial tabular.

Por eso existe:

- `spec/13-ddl/14-tablas-georreferenciacion-territorial.md`

Estado actual:

- archivo creado
- orientación funcional ya documentada
- pendiente de completar cuando estén cargadas las capas reales en el servidor PostGIS

---

## Estado del SQL físico

`sql/informix/base/` fue regenerado y alineado con:

- `Fal`
- `Num`
- `Stor`
- `RubroCom` / `RubroComVersion`
- regla de máximo 30 caracteres en nombres auxiliares

Luego se hicieron pasadas adicionales para alinear el SQL base con decisiones de:

- domicilio del infractor
- calle textual libre
- normalización parcial
- jurisdicción emisora de licencia

---

## Punto exacto donde se retoma

En este momento:

- `spec/14-sql-operativo` ya quedó revisado completo
- `13-ddl` quedó mejor separado entre territorial tabular y GIS
- georreferenciación quedó pendiente para completar con capas reales PostGIS
- el próximo paso ya no está en revisión general de `14`, sino en:
  - completar GIS
  - o bajar a SQL más concreto / implementación real

---

## Próximo paso recomendado al retomar

1. completar `spec/13-ddl/14-tablas-georreferenciacion-territorial.md` cuando estén cargadas las capas reales en PostGIS
2. luego bajar de spec a SQL más concreto o cercano a implementación real
3. priorizar, según convenga:
   - lookups territoriales
   - alta de acta
   - circuito documental
   - circuito de notificación
   - repositorios / capa backend

---

## Recordatorio de estilo para seguir

- mantener archivos compactos
- usar hubs cortos y archivos satélite cuando convenga
- priorizar links relativos navegables entre archivos
- evitar documentos gigantes con múltiples responsabilidades
- usar `spec-as-source` de verdad: legible por humanos y navegable/usable por agentes
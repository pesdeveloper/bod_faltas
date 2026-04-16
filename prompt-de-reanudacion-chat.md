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
- Cuando un archivo deba funcionar como “hub”, debe ser corto, navegable y con links a archivos satélite.

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

Además del trabajo general previo, se partió el bloque territorial para hacerlo más navegable:

- `11-tablas-territoriales-externas-introduccion.md`
- `12-tablas-territoriales-ign-indec.md`
- `13-tablas-territoriales-malvinas-locales.md`
- `14-tablas-georreferenciacion-territorial.md`

### Estado de esos archivos

#### 11
Quedó como introducción / marco general de tablas territoriales externas y compartidas.

#### 12
Documenta tablas IGN / INDEC consumidas por faltas.

#### 13
Documenta tablas locales de Malvinas para resolución fina de domicilios.

#### 14
Quedó creado como archivo base de georreferenciación territorial, pero **pendiente de completarse** cuando estén cargadas las capas reales en PostGIS.

---

## Estado actual de `spec/14-sql-operativo`

Se hizo una compactación fuerte para alinearlo con `spec-as-source`.

El bloque actual quedó así:

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

### Criterios aplicados

- `00` quedó como hub corto y navegable
- `03` quedó como hub corto
- el detalle de formularios/lookups se separó en `03a`, `03b`, `03c`
- se evitó renumerar `04` a `08`
- se reforzó navegación entre archivos por links relativos

---

## Estado actual del tema domicilios

Este es uno de los puntos más importantes del momento.

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

La resolución fina usa:

- localidad
- calle
- altura
- tramo
- barrio
- ejido urbano

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

### Regla de municipio lógico

En UX siempre se muestra “Municipio”, pero internamente puede ser:

- municipio real
- o departamento fallback

### Regla de calle no encontrada en catálogo

Si la calle no existe en catálogo:

- se permite ingreso textual
- no se agrega al catálogo
- no se corrige el origen desde el formulario
- el domicilio queda parcialmente normalizado

### Persistencia acordada para domicilio del infractor

Además del shape nacional, se decidió completar soporte para:

- calle textual libre
- normalización parcial
- referencias locales de Malvinas para el infractor

En concreto, quedó acordado agregar o contemplar en `FalActa`:

- soporte local fino Malvinas para el infractor
- flag de calle textual libre del infractor
- campo de calle textual del infractor
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

Luego se hizo otra pasada para alinearlo con las decisiones nuevas de:

- domicilio del infractor
- calle textual libre
- normalización parcial
- jurisdicción emisora de licencia

---

## Punto exacto donde se retoma

En este momento:

- el usuario va a revisar `spec/14-sql-operativo`
- ya se regeneraron archivos de `13-ddl`, `14-sql-operativo` y `sql/informix/base` para reflejar los cambios de domicilios
- `13-ddl` ya quedó mejor separado en territorial tabular vs GIS
- georreferenciación quedó diferida para completar cuando existan las capas cargadas en PostGIS

---

## Próximo paso recomendado al retomar

1. revisar observaciones del usuario sobre `spec/14-sql-operativo`
2. ajustar si hace falta los archivos regenerados por el tema domicilios
3. verificar si la persistencia final acordada de domicilio quedó exactamente como se quiere
4. completar `spec/13-ddl/14-tablas-georreferenciacion-territorial.md` cuando estén las capas reales PostGIS
5. luego empezar a bajar de spec a SQL operativo más cercano a implementación real

---

## Recordatorio de estilo para seguir

- mantener archivos compactos
- usar hubs cortos y archivos satélite cuando convenga
- priorizar links relativos navegables entre archivos
- evitar documentos gigantes con múltiples responsabilidades
- usar `spec-as-source` de verdad: legible por humanos y navegable/usable por agentes
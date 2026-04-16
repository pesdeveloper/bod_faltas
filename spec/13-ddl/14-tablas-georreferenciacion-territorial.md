# [13-DDL] 14 - TABLAS DE GEOREFERENCIACION TERRITORIAL

## Finalidad

Este archivo documenta la existencia y el rol funcional de las tablas de georreferenciación territorial del municipio de Malvinas Argentinas.

Estas tablas viven en motor GIS / PostgreSQL / PostGIS y son potencialmente útiles para:

- resolución espacial por GPS
- geocodificación inversa local
- enriquecimiento automático de domicilios
- determinación de calle, barrio, localidad, manzana o parcela a partir de coordenadas
- validación espacial
- análisis territorial

---

## Naturaleza de estas tablas

Estas tablas:

- no forman parte del núcleo físico propio de faltas
- no son las mismas tablas tabulares IGN / INDEC
- representan capas espaciales o geométricas
- deben documentarse porque son funcionalmente relevantes para la resolución territorial futura

---

## Capas esperables

Según lo que ya quedó dicho del entorno municipal, pueden existir capas de:

- localidades
- barrios
- manzanas
- calles o ejes viales
- parcelas
- otras capas territoriales municipales

---

## Usos funcionales posibles en faltas

Estas capas pueden utilizarse para:

- resolver automáticamente calle y contexto territorial a partir de GPS
- enriquecer un domicilio textual con información espacial
- validar si un punto cae dentro de Malvinas Argentinas
- determinar barrio o localidad por contención espacial
- determinar manzana o parcela por point-in-polygon
- asistir procesos de inspección, notificación o verificación territorial

---

## Relación con el domicilio textual

La resolución espacial no reemplaza necesariamente al domicilio textual.

Debe poder cumplir uno o más de estos roles:

- confirmar
- enriquecer
- sugerir
- corregir con supervisión humana
- completar parcialmente

La prioridad exacta entre resolución textual y espacial debe documentarse luego en la lógica operativa o de backend.

---

## Resolución por GPS

### Escenario típico

Si se dispone de latitud y longitud:

- puede intentarse resolución espacial
- puede determinarse contención en capas territoriales
- puede proponerse calle, barrio, localidad o parcela
- puede generarse enriquecimiento sin obligar al usuario a elegir manualmente todo

### Regla sugerida

La resolución por GPS debe considerarse:

- complemento fuerte
- herramienta de enriquecimiento
- soporte de validación
- posible fuente primaria en flujos móviles

pero no debe asumirse perfecta sin conocer calidad y actualización real de las capas.

---

## Persistencia derivada posible

Si la resolución espacial se usa realmente, más adelante podría persistirse:

- coordenadas originales
- capa / entidad espacial resuelta
- identificadores territoriales derivados
- grado de confianza o validación
- flags de resolución automática

Esto debe definirse cuando se documente el flujo concreto de georreferenciación.

---

## Estado actual del archivo

En este momento este archivo deja **la decisión arquitectónica y funcional** de que la georreferenciación territorial existe y será relevante.

La especificación estructural detallada todavía queda pendiente de completar con:

- nombres reales de tablas PostGIS
- columnas principales
- tipos geométricos
- identificadores
- índices espaciales relevantes
- políticas de resolución por GPS

Cuando se disponga de ese esquema real, este archivo deberá completarse.

---

## Relación con otros archivos

- territorial tabular y lookups administrativos: [`11-tablas-territoriales-externas-introduccion.md`](./11-tablas-territoriales-externas-introduccion.md)
- IGN / INDEC: [`12-tablas-territoriales-ign-indec.md`](./12-tablas-territoriales-ign-indec.md)
- tablas locales Malvinas: [`13-tablas-territoriales-malvinas-locales.md`](./13-tablas-territoriales-malvinas-locales.md)
- lógica SQL de formularios y lookups: [`../14-sql-operativo/03-sql-formularios-y-lookups.md`](../14-sql-operativo/03-sql-formularios-y-lookups.md)

---

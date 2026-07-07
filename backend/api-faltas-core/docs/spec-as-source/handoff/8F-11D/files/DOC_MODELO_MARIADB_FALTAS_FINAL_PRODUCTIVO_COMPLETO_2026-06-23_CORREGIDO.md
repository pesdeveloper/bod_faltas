﻿# Modelo MariaDB final productivo completo — Dirección de Faltas

**Fecha de cierre:** 2026-06-23  
**Destino técnico:** MariaDB 11.x / InnoDB / `utf8mb4`  
**Estado:** modelo productivo completo y autocontenido para dominio, spec final, backend, Angular, datos de prueba y QA.  
**Criterio:** este documento es la fuente consolidada del modelo de datos final. No requiere consultar otros documentos para entender tablas, reglas estructurales o decisiones de modelado.

## Collation base

Base MariaDB:

CHARACTER SET utf8mb4
COLLATE utf8mb4_uca1400_ai_ci

Motivo:
Se usa Unicode completo, con comparación case-insensitive y accent-insensitive para búsquedas humanas, nombres, calles, localidades, personas y textos administrativos.

---

## 0. Decisiones globales consolidadas

### 0.1 Naturaleza del modelo

Este documento define el modelo real de persistencia MariaDB para Faltas / Dirección de Faltas.

Reglas:

- Este `.md` es autocontenido: no depende de otros archivos para entender el modelo final.
- El modelo no es una migración desde Informix ni una copia del prototipo.
- El prototipo funcional, los mocks, Angular y QA se usaron como evidencia para descubrir reglas, pero no se copian bugs, aliases visuales, filtros UI ni valores mock-only.
- Las decisiones cerradas de dominio prevalecen sobre cualquier DDL histórico, mock, valor legacy o documentación exploratoria previa.
- No mantener secciones de gap, veredicto pendiente o comparación histórica como parte del modelo productivo.
- No usar tablas descartadas como `fal_acta_pago_voluntario` ni `fal_acta_pago_condena`.
- No usar `payload_json` como estrategia principal de eventos.
- No usar números mágicos en API, QA, UI o copy-control.
- En base se admiten códigos compactos, pero hacia afuera se exponen códigos/labels legibles.
- Todo evento, estado o acción que gobierne el proceso debe ser determinista, tipificado y trazable.

### 0.1.1) Catálogos externos y sincronizaciones

El modelo utiliza tres grupos de catálogos externos en MariaDB:

1. Catálogos nacionales/locales IGN/INDEC/BAHRA cargados desde CSV por `MariaDb_DataSetsLoader`.
2. Catálogos locales Malvinas sincronizados desde Informix por un CLI específico Informix -> MariaDB.
3. Rubros comerciales sincronizados desde `informix.rubrocom`.

Los catálogos IGN/INDEC/BAHRA se cargan en tablas `geo_*` con control de carga (`geo_dataset_load_run`), errores (`geo_dataset_load_error`) y auditoría de versiones (`geo_dataset_row_version`). No exponen una columna `ver_*` funcional para ser referenciada desde Faltas; por eso las tablas del dominio guardan los IDs externos y, cuando corresponde, texto/cache histórico propio.

Los catálogos locales Malvinas sí son tablas versionadas por fila:
- `geo_malv_calle_version`
- `geo_malv_localidad_version`

Las vistas actuales:
- `vw_geo_malv_calle_actual`
- `vw_geo_malv_localidad_actual`

se usan para búsquedas/autocomplete y selección operativa.

Los rubros comerciales se sincronizan desde Informix/Ingresos en:
- `fal_rubro_version`

La vista actual:
- `vw_fal_rubro_actual`

se usa para búsqueda/autocomplete, pero las actas deben guardar `rubro_id` apuntando a la versión exacta usada al labrar.


### 0.1.2 Catálogos geográficos y sincronizaciones externas

#### Catálogos IGN/INDEC/BAHRA cargados por `MariaDb_DataSetsLoader`

El CLI `MariaDb_DataSetsLoader` carga 8 datasets CSV en MariaDB `sb_faltas_db`:

- `geo_ign_provincia`
- `geo_ign_departamento`
- `geo_ign_municipio`
- `geo_indec_localidad_censal`
- `geo_indec_localidad`
- `geo_bahra_asentamiento`
- `geo_indec_calles`
- `geo_calle_alturas_barrio`

Tablas de control:

- `geo_dataset_load_run`
- `geo_dataset_load_error`
- `geo_dataset_row_version`

Reglas:

- Estos catálogos no son administrados por Faltas.
- Se cargan por CLI desde CSV oficiales/locales.
- Las tablas destino tienen `row_hash`, `load_run_id`, `last_seen_load_run_id`, `si_activo`, `valid_from`, `valid_to`.
- El historial de cambios de carga queda en `geo_dataset_row_version`.
- Las tablas de Faltas no referencian `ver_*` de estos catálogos porque no existe una versión funcional expuesta como FK.
- Cuando un documento o snapshot necesita preservar texto histórico, debe copiar descripción/nombre al snapshot correspondiente.

#### Vista `vw_geo_municipio_departamento`

`vw_geo_municipio_departamento` es una vista derivada desde `geo_indec_localidad`.

No es tabla física ni dataset del loader.

Definición funcional:

```sql
SELECT DISTINCT
    provincia_id,
    municipio_id,
    departamento_id
FROM geo_indec_localidad
WHERE municipio_id IS NOT NULL
  AND departamento_id IS NOT NULL;
```

Uso:

- Resolver departamentos asociados a un municipio.
- Filtrar localidades cuando el usuario selecciona municipio real.
- Dar fallback por departamento cuando corresponda.

#### Catálogos locales Malvinas sincronizados desde Informix

Los catálogos locales Malvinas se sincronizan desde Informix hacia MariaDB mediante CLI separado de `MariaDb_DataSetsLoader`.

Fuentes Informix:

- `informix.calle`
- `informix.localidad`

Destinos MariaDB:

- `geo_malv_calle_version`
- `vw_geo_malv_calle_actual`
- `geo_malv_localidad_version`
- `vw_geo_malv_localidad_actual`

Reglas:

- No tienen auditoría de usuario.
- Tienen auditoría técnica de sincronización:
  - `row_hash`
  - `previous_row_hash`
  - `source_operation`
  - `close_operation`
  - `si_activo`
  - `si_version_actual`
  - `valid_from`
  - `valid_to`
  - `synced_at`
- Las APIs de autocomplete y selección local deben leer desde las vistas actuales.
- Las tablas del dominio que necesiten congelar contexto histórico deben guardar el `*_version_id` correspondiente.

#### `geo_malv_calle_version`

Fuente: `informix.calle`.

Campos principales:

- `calle_version_id`
- `id_tca`
- `deno`
- `denobusq`
- `denoant1` a `denoant4`
- `id_tca_pant`
- `id_tca_ppost`
- `id_tca_nace`
- `id_tca_fin`
- normalizados `*_norm`
- columnas técnicas de versionado/sync

`id_tca` siempre se trata como texto para preservar ceros iniciales.

#### `vw_geo_malv_calle_actual`

Vista de calles locales vigentes:

```sql
SELECT ...
FROM geo_malv_calle_version
WHERE si_version_actual = 1
  AND si_activo = 1
  AND valid_to IS NULL;
```

#### `geo_malv_localidad_version`

Fuente: `informix.localidad`.

Campos principales:

- `localidad_version_id`
- `id_loc`
- `deno`
- `cp`
- `deno_norm`
- columnas técnicas de versionado/sync

`id_loc` siempre se trata como texto.

#### `vw_geo_malv_localidad_actual`

Vista de localidades locales vigentes:

```sql
SELECT ...
FROM geo_malv_localidad_version
WHERE si_version_actual = 1
  AND si_activo = 1
  AND valid_to IS NULL;
```

#### Rubros comerciales sincronizados desde Informix

Fuente: `informix.rubrocom`.

Campos usados:

- `id_rub`
- `deno`
- `sidesabilitado`

Destino:

- `fal_rubro_version`
- `vw_fal_rubro_actual`

Reglas:

- No se usan `codigoactividad`, `categoria`, `atributos`, `v1_*` ni `v2_*`.
- `Id_Rub` es el ID externo/manual de Ingresos.
- `rubro_id` es PK técnica interna de la versión.
- Las actas guardan `rubro_id` + `Id_Rub`.

### 0.2 Convenciones técnicas

| Criterio | Decisión |
|---|---|
| Motor | MariaDB 11.x |
| Engine | InnoDB |
| Charset | `utf8mb4` |
| Collation recomendada | `utf8mb4_uca1400_ai_ci` |
| PK técnica | `BIGINT AUTO_INCREMENT`, salvo excepción explícita |
| Fechas con hora | `DATETIME(6)` |
| Fechas sin hora | `DATE` |
| Importes | `DECIMAL(14,2)` |
| Cantidades/unidades | `DECIMAL(14,4)` |
| GPS | `DECIMAL(12,8)` |
| Booleanos | `BOOLEAN` |
| Catálogos funcionales | `SMALLINT` + enum documentado |
| Textos largos | Evitar `TEXT`; preferir `VARCHAR(n)` salvo documentos/casos justificados |
| Nombres | `snake_case`, salvo nombres externos reales que se mantienen por integración (`Id_Suj`, `Id_Bie`, `Cmte_EM`, etc.) |
| Códigos funcionales de catálogo | `VARCHAR(12)` por defecto; ampliar solo por excepción documentada |

### 0.3 Comprobantes de Ingresos/Tesorería

En todas las tablas donde se referencien comprobantes del sistema de Ingresos/Tesorería se usan nombres reales consistentes.

Para comprobante de deuda/emisión:

| Campo | Tipo MariaDB | Uso |
|---|---|---|
| `Cmte_EM` | `CHAR(2)` | Tipo de comprobante de deuda/emisión |
| `Pref_EM` | `SMALLINT` | Prefijo de comprobante de deuda/emisión |
| `Nro_EM` | `INT` | Número de comprobante de deuda/emisión |

Para comprobante de pago/recibo:

| Campo | Tipo MariaDB | Uso |
|---|---|---|
| `Cmte_PG` | `CHAR(2)` | Tipo de comprobante de pago/recibo |
| `Pref_PG` | `SMALLINT` | Prefijo de comprobante de pago/recibo |
| `Nro_PG` | `INT` | Número de comprobante de pago/recibo |

Reglas:

- No usar nombres genéricos `deuda_cmte`, `deuda_pref`, `deuda_nro`.
- No usar nombres genéricos `pago_cmte`, `pago_pref`, `pago_nro`.
- Aplicar esta convención en obligación, forma de pago, movimientos, integraciones y cualquier tabla que referencie comprobantes.

### 0.4 Portal infractor y documentos firmados

Reglas:

- El portal del infractor no expone estados internos como `cod_bandeja`, `accion_pendiente` o marcas operativas.
- Una vez generado y firmado el PDF, los datos emitidos quedan instanciados en el documento firmado.
- Las tablas de notificación enriquecen trazabilidad operativa, pero no reemplazan el documento firmado.
- El portal no muestra valor a pagar hasta que exista valorización confirmada aplicable.
- Si el fallo es absolutorio, no hay monto de condena exigible.

---

### 0.5 Ajustes incorporados en esta versión

Esta versión incorpora las decisiones tomadas durante la revisión posterior de modelo:

- `tipo_acta` queda sin valor genérico `OTRA`; solo se permiten tipos reales definidos.
- `nom_dep` se limita a `VARCHAR(64)`.
- Los códigos funcionales de catálogo usan `VARCHAR(12)` por defecto; solo se amplía por excepción justificada.
- Los domicilios solo se versionan/nueva fila cuando ya fueron usados en documento/notificación/pieza formal; antes de eso se corrige la misma fila.
- `fal_observacion.entidad_tipo` queda con catálogo cerrado inicial.
- `fal_articulo_normativa_faltas` no maneja mínimos, máximos ni graduación. Define cantidad/tipo unidad ordinaria y cantidad/tipo unidad de pago voluntario cuando corresponda.
- `tipo_unidad` queda cerrado a `SALARIO`, `UNIDAD_FIJA`, `MONTO`.
- `fal_acta_articulo_infringido` no fija importes ni snapshots tarifarios. Solo instancia la imputación normativa.
- La fijación/congelamiento/manualización monetaria vive en `fal_acta_valorizacion` y `fal_acta_valorizacion_item`.
- El valor informado por portal se recalcula con el último tarifario vigente mientras no exista confirmación, congelamiento, manualización o comprobante materializado.
- En fallo condenatorio, el comportamiento por defecto es actualizar al último tarifario vigente, salvo decisión expresa de mantener valor anterior o fijar monto manual.
- Se agrega descripción `TEXT` en sustancias alimenticias.
- `ucomp` pasa a `VARCHAR(20)`.
- La numeración/talonarios queda jerárquica y granular: política, talonario, ámbitos, dependencia, tipo documental, reinicio anual, movimientos y rendición de talonarios manuales físicos.
- Correcciones finales incorporadas: motor objetivo MariaDB 11.x; `version_row` para concurrencia optimista en agregados principales; `Id_Bie_i` nullable en contravenciones manuales/excepcionales; QR acceso registra solo accesos válidos; talonario+número tiene registro único; numeración electrónica por objeto `SEQUENCE`; se eliminó relación cíclica entre medida preventiva y bloqueante material; se agregó patrón MariaDB con columnas generadas para unicidad de filas activas/vigentes; se corrigieron actor/eventos, valorización operativa, gestión externa en snapshot, firma documental, auditoría de pagos y consistencia de nombres de archivo/MIME.


Ajustes territoriales/domicilios incorporados en la revisión del 2026-06-23:

- El domicilio de persona/infractor se modela en `fal_persona_domicilio` con soporte para dos modos: `MALVINAS_LOCAL` y `EXTERNO`.
- Para domicilios locales de Malvinas se guardan `id_loc_malvinas` e `id_tca_malvinas`; `id_tca` se conserva como texto para no perder ceros iniciales.
- Se reemplaza la duplicación municipio/departamento por unidad territorial genérica: tipo + ID. Para catálogos geográficos IGN/INDEC/BAHRA no se persiste `ver_*` funcional porque la versión técnica vive en las tablas de carga/auditoría del loader.
- La posición del dispositivo al labrar cualquier acta queda en `fal_acta` como dato de captura.
- El lugar del hecho/infracción queda en `fal_acta` con datos estructurados mínimos, latitud/longitud final, origen de ubicación y marca de ajuste manual.
- No se crea tabla 1:1 de ubicación GIS; al ser dato del acta se instancia en `fal_acta` con campos nullable.
- No se persisten `quality`, `source`, `score`, `warnings`, rangos, lado GIS ni demás telemetría técnica del prototipo si no gobiernan operación, filtros, documentos o bandejas.
- Los textos preprocesados para bandejas viven en `fal_acta_snapshot`, no como fuente primaria.
- `fal_acta_contravencion` elimina `nomenclatura_txt`; la nomenclatura permanece estructurada y el resumen visual se proyecta en snapshot.
- Los datos mock de cuenta inmueble/comercio no generan columnas productivas; se resuelven desde generación de actas mock o formularios de prototipo.


### 0.7 Decisiones productivas finales incorporadas

#### Motor de proceso y estados

El proceso se modela como motor determinista. Una acción debe validar precondiciones, ejecutar transición explícita, actualizar fuente de verdad, registrar evento real de dominio y recalcular snapshot/bandejas/acciones pendientes.

No se permiten:

- acciones visibles que no producen efecto real;
- cambios de estado sin acción/hecho explícito;
- archivo automático sin motivo;
- fallo automático sin acción explícita;
- eventos de proyección;
- aliases UI como catálogo productivo;
- valores mock-only como valores productivos.

#### `bloque_actual`

`bloque_actual` usa códigos compactos semánticos productivos en DB:

| Código DB | Bloque productivo |
|---|---|
| `CAPT` | `CAPTURA` |
| `ENRI` | `ENRIQUECIMIENTO` |
| `NOTI` | `NOTIFICACION` |
| `ANAL` | `ANALISIS` |
| `GEXT` | `GESTION_EXTERNA` |
| `ARCH` | `ARCHIVO` |
| `CERR` | `CERRADA` |

Los códigos históricos `D1_CAPTURA`, `D2_ENRIQUECIMIENTO`, `D4_NOTIFICACION` y `D5_ANALISIS` sólo sirven como equivalencias de migración/compatibilidad temporal. No son valores productivos.

`D3_DOCUMENTAL` queda eliminado/no productivo. La etapa documental se representa por bandeja/sub-bandeja/documentos/firma, no por bloque.

#### `tipo_evt`

`fal_acta_evento.tipo_evt` se persiste como `CHAR(6)` y se implementa como enum/constante Spring.

Reglas:

- No tabla catálogo editable por defecto.
- Los eventos mock-only que expresan dominio se mapean a eventos productivos.
- Los eventos de proyección/demo/estado se eliminan.
- `CONFIR` / `CONDENA_FIRME` se conserva como evento productivo porque la firmeza de condena es un hito jurídico/operativo real.

#### Archivo y reingreso

Archivo no es un simple estado ni un campo libre. Debe quedar trazado en `fal_acta_archivo`, con motivo administrable, flags funcionales, origen procesal y posibilidad de reingreso.

`motivo_archivo` es administrable, no un set cerrado copiado del prototipo.

#### Gestión externa

Gestión externa se modela con dimensiones separadas:

- `tipo_gestion_ext`;
- `estado_gestion_ext`;
- `resultado_gestion_ext`;
- `modo_reingreso_gestion_ext`;
- documentos externos vinculados;
- observaciones por `fal_observacion`.

No se usa un string compuesto para representar resultado, pago, dictamen, reingreso y monto a la vez.

#### Bloqueantes materiales

La regla de cierre es estricta:

> Un resultado final válido no alcanza para cerrar si quedan bloqueantes materiales activos.

La firma de un resolutorio no libera el bloqueante. Sólo el cumplimiento material efectivo lo libera.

#### Integraciones simuladas por ahora

Por ahora pueden ejecutarse desde interfaz de prueba, pero llamando endpoints/lógica reales:

- firma de documentos;
- retiro/liberación de rodado;
- cumplimiento material;
- notificador municipal;
- portal ciudadano con usuario mock hasta integrar IDP.

No se permiten atajos mock que muten estado salteando lógica real.

### 0.6 Decisiones técnicas MariaDB 11.x

Reglas:

- El motor definitivo es MariaDB 11.x sobre InnoDB.
- No se utilizarán ORMs pesados; la persistencia se implementará con micro-ORM, SQL explícito o JDBC directo.
- Las actualizaciones concurrentes se controlarán con concurrencia optimista mediante `version_row`.
- Las tablas cabecera/agregados principales deben incluir `version_row INT NOT NULL DEFAULT 0`.
- Toda actualización de un agregado principal debe incluir condición por versión:
  - `WHERE id = ? AND version_row = ?`
  - y debe incrementar `version_row = version_row + 1`.
- Si el update afecta cero filas, debe tratarse como conflicto de concurrencia y recargar estado.
- Para filas activas/vigentes/finales se usará el patrón MariaDB de columna generada nullable + índice único.
- La columna generada debe devolver la clave de scope cuando la fila está activa/vigente/final y `NULL` cuando no lo está.
- El índice único sobre columnas generadas aprovecha que MariaDB permite múltiples `NULL` en índices únicos.
- Para talonarios electrónicos, la numeración atómica se delega en objetos `SEQUENCE` nativos de MariaDB.
- `num_talonario.nombre_secuencia` guarda el nombre del objeto `SEQUENCE` asociado al talonario.
- No se usa `num_talonario.ultimo_nro_usado` para controlar concurrencia de numeración.
- Las bajas lógicas (`si_activo = false`) se conservan como historial, pero documentos nuevos, snapshots y cálculos operativos deben considerar solo registros activos salvo regla expresa en contrario.

Tablas mínimas con `version_row`:

| Tabla | Motivo |
|---|---|
| `fal_acta` | Agregado raíz del expediente |
| `fal_acta_valorizacion` | Decisión económica confirmable/vigente |
| `fal_acta_fallo` | Decisión resolutiva/fallo |
| `fal_documento` | Cabecera documental firmable/notificable |
| `fal_notificacion` | Cabecera del acto notificatorio |
| `fal_acta_apelacion` | Cabecera del trámite de apelación |
| `fal_acta_obligacion_pago` | Cabecera de obligación emitida/materializada |
| `fal_acta_forma_pago` | Estado operativo de forma de pago |
| `fal_acta_plan_pago_ref` | Referencia operativa de plan de pago |
| `fal_acta_paralizacion` | Ciclo administrativo activo/cerrado |
| `fal_acta_archivo` | Ciclo administrativo activo/cerrado |
| `fal_acta_gestion_externa` | Ciclo administrativo activo/cerrado |
| `num_talonario` | Cabecera de talonario y referencia a secuencia |


### 0.8 Decisiones incorporadas en Slice 8F-10 (2026-07-04)

Las siguientes decisiones aprobadas por Pablo en 8F-9-R1 fueron incorporadas al modelo lógico en el Slice 8F-10.

| Decisión | Descripción resumida | Tabla/Campo | Estado en modelo |
|---|---|---|---|
| D1 | FalActaFirmezaCondena sin tabla separada; firmeza en fal_acta_fallo | `origen_firmeza SMALLINT NULL` agregado a `fal_acta_fallo` | APLICADA_8F-10 |
| D2 | FalPagoVoluntario y FalPagoCondena mapean a `fal_acta_obligacion_pago` con tipo | `tipo_obligacion`: PAGO_VOLUNTARIO / CONDENA | YA_EN_MODELO |
| D3 | FalActaParalizacion debe implementarse in-memory antes de JDBC | `fal_acta_paralizacion` completa (sección 7.3) | CONFIRMADA |
| D4 | Campos JSON documentales usan tipo JSON nativo MariaDB 12.3.2 | Cuatro campos JSON en tablas 8F | APLICADA_8F-10 |
| D5 | `fundamentos TEXT NULL` directo en `fal_acta_fallo` | `fal_acta_fallo.fundamentos` agregado | APLICADA_8F-10 |
| D6 | `fal_persona` y `fal_persona_domicilio` completos y alineados con GeoDomicilios | Secciones 2.2 y 2.3 | YA_EN_MODELO |
| D7 | `fal_notificacion.id BIGINT AUTO_INCREMENT` | `fal_notificacion.id BIGINT AUTO_INCREMENT` | YA_EN_MODELO |
| D8 | `fal_acta.tipo_acta SMALLINT NOT NULL` con valores TRANSITO=1...COMERCIO=4 | `fal_acta.tipo_acta SMALLINT NOT NULL` | YA_EN_MODELO |
| D9 | `prioridad SMALLINT NOT NULL` rango 0..32767 | `fal_documento_plantilla_default.prioridad SMALLINT` | APLICADA_8F-10 |

Tres tablas nuevas incorporadas en este slice:

- `fal_documento_plantilla_contenido` (sección 5.11)
- `fal_documento_plantilla_default` (sección 5.12)
- `fal_documento_redaccion` (sección 5.13)

## 1. Dependencias, inspectores y tipo de acta

### 1.1 Regla funcional

El inspector pertenece a una dependencia versionada. La dependencia versionada define el único `tipo_acta` que puede labrar.

Flujo:

1. El inspector inicia el labrado.
2. El sistema obtiene su `fal_inspector_version` vigente.
3. Desde esa versión obtiene `id_dep` + `ver_dep`.
4. Desde `fal_dependencia_version.tipo_acta` obtiene el único tipo de acta permitido.
5. El backend valida que el acta creada corresponda a ese tipo.
6. El acta persiste `tipo_acta`, `id_dep`, `ver_dep`, `id_insp`, `ver_insp`.

`ALCOHOLEMIA` no es tipo de acta. Es un satélite eventual de un acta de tránsito.

### 1.2 Catálogo inicial `tipo_acta`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `TRANSITO` | Acta labrada por Tránsito |
| 2 | `CONTRAVENCION` | Acta contravencional / inspección general |
| 3 | `SUSTANCIAS_ALIMENTICIAS` | Acta de Bromatología / sustancias alimenticias |
| 4 | `COMERCIO` | Acta vinculada a comercio, habilitaciones o fiscalización |

### 1.3 Tabla `fal_dependencia`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_dep` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de dependencia | Entidad organizacional actual |
| `cod_dep` | `VARCHAR(20)` | SÍ | UNIQUE | Código interno | Estable para integraciones/reportes |
| `nom_dep` | `VARCHAR(64)` | NO | IDX | Nombre actual | El histórico vive en versiones |
| `id_dep_padre` | `BIGINT` | SÍ | FK+IDX | Dependencia padre actual | Organigrama actual |
| `si_activa` | `BOOLEAN` | NO | IDX | Dependencia activa | No borrar dependencias usadas |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |

### 1.4 Tabla `fal_dependencia_version`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_dep` | `BIGINT` | NO | PK+FK | Dependencia | Parte de PK versionada |
| `ver_dep` | `SMALLINT` | NO | PK | Versión | Congela contexto histórico |
| `nom_dep` | `VARCHAR(64)` | NO | IDX | Nombre congelado | Nombre vigente en esa versión |
| `id_dep_padre` | `BIGINT` | SÍ | FK+IDX | Dependencia padre congelada | Si corresponde |
| `ver_dep_padre` | `SMALLINT` | SÍ | — | Versión padre congelada | Obligatoria si hay padre versionado |
| `tipo_acta` | `SMALLINT` | NO | IDX | Tipo de acta que puede labrar | Una dependencia labra un único tipo de acta |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Vigencia funcional |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `si_activa` | `BOOLEAN` | NO | IDX | Versión activa | Control operativo |

### 1.5 Tabla `fal_inspector`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_insp` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de inspector | Maestro actual |
| `id_user` | `CHAR(36)` | NO | UNIQUE | Usuario IDP asociado | Subject/usuario autenticable |
| `legajo_insp` | `INT` | NO | IDX | Legajo actual | Dato operativo |
| `nom_insp` | `VARCHAR(120)` | NO | IDX | Nombre actual | El histórico vive en versiones |
| `si_activo` | `BOOLEAN` | NO | IDX | Inspector activo | No borrar si fue usado |

### 1.6 Tabla `fal_inspector_version`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_insp` | `BIGINT` | NO | PK+FK | Inspector | Parte de PK versionada |
| `ver_insp` | `SMALLINT` | NO | PK | Versión | Congela contexto histórico |
| `legajo_insp` | `INT` | NO | IDX | Legajo congelado | Trazabilidad |
| `nom_insp` | `VARCHAR(120)` | NO | IDX | Nombre congelado | Trazabilidad |
| `id_dep` | `BIGINT` | NO | FK+IDX | Dependencia congelada | Define dependencia del inspector |
| `ver_dep` | `SMALLINT` | NO | — | Versión dependencia | Obligatoria |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Vigencia funcional |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `si_activo` | `BOOLEAN` | NO | IDX | Versión activa | Control operativo |


### 1.7 Tabla `fal_firmante`

Maestro del firmante/autorizado para firma. Administra el padr�n de usuarios habilitados para firmar documentos del sistema.

No registra firmas concretas. No almacena certificados, binarios ni metadata pesada.

| Campo | Tipo MariaDB | Null | Clave/�ndice | Descripci�n | Regla |
|---|---|---|---|---|---|
| `id_firmante` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador del firmante | PK t�cnica |
| `id_user` | `CHAR(36)` | NO | UNIQUE | Usuario IDP asociado | Debe alinearse con `fal_documento_firma.id_user_firma` |
| `nom_firmante` | `VARCHAR(128)` | NO | IDX | Nombre visible actual | El hist�rico vive en versiones |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo l�gico | No eliminar f�sicamente si fue usado |
| `fh_alta` | `DATETIME(6)` | NO | - | Alta t�cnica | Auditor�a |
| `id_user_alta` | `CHAR(36)` | NO | - | Usuario alta | Auditor�a |

Reglas:

- `id_user` identifica al usuario habilitado para firmar. Es �nico por firmante.
- No guardar certificados, binarios ni metadata pesada de firma en esta tabla.
- No guardar firmas concretas. Las firmas concretas van en `fal_documento_firma`.
- No eliminar f�sicamente firmantes que hayan firmado documentos.

### 1.8 Tabla `fal_firmante_version`

Versionado hist�rico de roles, cargo, dependencia y vigencia del firmante. Congela el contexto vigente en el momento de cada cambio relevante.

| Campo | Tipo MariaDB | Null | Clave/�ndice | Descripci�n | Regla |
|---|---|---|---|---|---|
| `id_firmante` | `BIGINT` | NO | PK+FK | Firmante | FK a `fal_firmante` |
| `ver_firmante` | `SMALLINT` | NO | PK | Versi�n | Incremental por firmante; inicia en 1 |
| `id_user` | `CHAR(36)` | NO | IDX | Usuario congelado | Snapshot del IDP al crear la versi�n |
| `nom_firmante` | `VARCHAR(128)` | NO | - | Nombre congelado | Snapshot hist�rico |
| `rol_firmante` | `VARCHAR(40)` | SÃ | IDX | Rol institucional descriptivo | Snapshot opcional del rol funcional institucional; no define autorizaciÃ³n documental. La autorizaciÃ³n real va en `fal_firmante_version_habilitacion`. |
| `cargo_firmante` | `VARCHAR(128)` | S� | - | Cargo visible | Snapshot legible; nullable si no aplica |
| `id_dep` | `BIGINT` | S� | FK+IDX | Dependencia asociada | Nullable si no aplica; FK a `fal_dependencia` |
| `ver_dep` | `SMALLINT` | S� | - | Versi�n dependencia | Obligatoria si hay `id_dep`; FK a `fal_dependencia_version` |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Obligatorio |
| `fh_vig_hasta` | `DATE` | S� | IDX | Fin vigencia | NULL si vigente |
| `si_activo` | `BOOLEAN` | NO | IDX | Versi�n activa | Vigencia l�gica |
| `fh_alta` | `DATETIME(6)` | NO | - | Alta t�cnica | Auditor�a |
| `id_user_alta` | `CHAR(36)` | NO | - | Usuario alta | Auditor�a |

Reglas:

- `ver_firmante` inicia en 1 al crear el firmante.
- Al versionar, la versi�n anterior cierra `fh_vig_hasta` y `si_activo = false`.
- La versi�n nueva congela `id_user`, `nom_firmante`, `rol_firmante` (descriptivo opcional), `cargo_firmante`, `id_dep`, `ver_dep`.
- `rol_firmante` es un campo descriptivo/institucional opcional. No define autorizaciÃ³n documental; la autorizaciÃ³n concreta va en `fal_firmante_version_habilitacion`.
- `tipo_firma` NO va en `fal_firmante_version`; `tipo_firma` es el mecanismo/naturaleza de la firma.
- Si cambia rol, cargo o dependencia, se crea nueva versi�n. No se actualizan versiones anteriores.
- Si se informa `id_dep`, debe informarse `ver_dep`.
- Si se informa `id_dep`+`ver_dep`, debe existir el registro en `fal_dependencia_version`.
- No se eliminan f�sicamente firmantes que hayan firmado documentos.
- No se actualizan firmas hist�ricas por cambios posteriores en el firmante.

### 1.9 Tabla `fal_firmante_version_habilitacion`



Define quÃ© puede firmar una versiÃ³n concreta de firmante. Es la fuente de autorizaciÃ³n documental por firmante.



No define quiÃ©n firma un documento concreto (eso lo hace `fal_documento_firma_req`).

Define quÃ© tipos de documento y roles puede satisfacer una versiÃ³n de firmante.



| Campo | Tipo MariaDB | Null | Clave/Ãndice | DescripciÃ³n | Regla |

|---|---|---|---|---|---|

| `id_firmante` | `BIGINT` | NO | PK+FK | Firmante | Parte de FK a `fal_firmante_version` |

| `ver_firmante` | `SMALLINT` | NO | PK+FK | VersiÃ³n firmante | Parte de FK a `fal_firmante_version` |

| `tipo_docu` | `SMALLINT` | NO | PK+IDX | Tipo de documento habilitado | Compatible con `fal_documento.tipo_docu` |

| `rol_firma_req` | `SMALLINT` | NO | PK+IDX | Rol requerido que puede satisfacer | Compatible con `fal_documento_firma_req.rol_firma_req` |

| `mecanismo_firma_req` | `SMALLINT` | SÃ | IDX | Mecanismo permitido/requerido | NULL si no restringe mecanismo |

| `si_activo` | `BOOLEAN` | NO | IDX | HabilitaciÃ³n activa | Baja lÃ³gica |

| `fh_alta` | `DATETIME(6)` | NO | - | Alta tÃ©cnica | AuditorÃ­a |

| `id_user_alta` | `CHAR(36)` | NO | - | Usuario alta | AuditorÃ­a |



Reglas:



- Una versiÃ³n de firmante puede tener N habilitaciones.

- Cada habilitaciÃ³n indica un `tipo_docu` de documento que puede firmar esa versiÃ³n.

- Cada habilitaciÃ³n indica un `rol_firma_req` que esa versiÃ³n puede satisfacer.

- `mecanismo_firma_req` restringe mecanismo solo si estÃ¡ informado. NULL significa sin restricciÃ³n de mecanismo.

- La vigencia temporal de la habilitaciÃ³n se toma de `fal_firmante_version` (`fh_vig_desde`, `fh_vig_hasta`).

- Si cambia identidad, cargo, dependencia o vigencia del firmante, se crea nueva versiÃ³n de firmante.

- Si cambia una habilitaciÃ³n: dar de baja la habilitaciÃ³n actual (`si_activo = false`) y crear una nueva, o versionar el firmante. No dejar el criterio ambiguo.

- No modificar firmas histÃ³ricas por cambios posteriores en habilitaciones.

- No usar `tipo_firma` como rol; `tipo_firma` es mecanismo/naturaleza de la firma realizada.

- La PK funcional es `(id_firmante, ver_firmante, tipo_docu, rol_firma_req)`.

---

## 2. Núcleo expediente

### 2.1 Tabla `fal_acta`

`fal_acta` es el agregado raíz del expediente. Guarda identidad administrativa, estado actual, referencias principales, lugar del hecho, numeración y QR.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador interno | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `id_tecnico` | `CHAR(36)` | NO | UK | UUID técnico estable del acta | Puede ser generado por backend o por dispositivo en captura offline; se usa para idempotencia/sincronización |
| `nro_acta` | `VARCHAR(30)` | SÍ | IDX | Número visible del acta | Se compone desde política/talonario |
| `id_talonario` | `BIGINT` | SÍ | FK+IDX | Talonario usado | NULL si aún no está numerada |
| `nro_talonario_usado` | `INT` | SÍ | IDX | Número usado dentro del talonario | Trazabilidad técnica |
| `tipo_acta` | `SMALLINT` | NO | IDX | Tipo de acta | Determinado por dependencia del inspector |
| `origen_captura` | `SMALLINT` | NO | IDX | Origen del labrado | Mobile, web, carga interna, integración |
| `id_dispositivo_captura` | `VARCHAR(80)` | SÍ | IDX | Dispositivo de captura | Especialmente mobile/offline |
| `id_user_captura` | `CHAR(36)` | SÍ | IDX | Usuario capturador | Puede coincidir con inspector/operador |
| `fh_captura` | `DATETIME(6)` | NO | IDX | Fecha/hora real de captura | Momento funcional |
| `lat_captura` | `DECIMAL(12,8)` | SÍ | — | Latitud del dispositivo al labrar | Posición desde donde se registró/cargó el acta |
| `lon_captura` | `DECIMAL(12,8)` | SÍ | — | Longitud del dispositivo al labrar | Posición desde donde se registró/cargó el acta |
| `precision_captura_m` | `DECIMAL(8,2)` | SÍ | — | Precisión de posición en metros | Si el dispositivo la informa |
| `fh_pos_captura` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de posición de captura | Puede diferir de `fh_captura` |
| `origen_pos_captura` | `SMALLINT` | SÍ | IDX | Origen posición captura | Catálogo `origen_pos_captura` |
| `fh_acta` | `DATETIME(6)` | NO | IDX | Fecha/hora del acta | Fecha de labrado/registro funcional |
| `id_dep` | `BIGINT` | NO | FK+IDX | Dependencia | Congelada desde inspector |
| `ver_dep` | `SMALLINT` | NO | — | Versión dependencia | Congelada |
| `id_insp` | `BIGINT` | SÍ | FK+IDX | Inspector | NULL si el circuito no tiene inspector directo |
| `ver_insp` | `SMALLINT` | SÍ | — | Versión inspector | Obligatoria si hay inspector |
| `id_persona_infractor` | `BIGINT` | NO | FK+IDX | Persona infractora | Toda acta tiene sujeto técnico |
| `id_domicilio_infractor_act` | `BIGINT` | SÍ | FK+IDX | Domicilio infractor tomado para el acta | Domicilio de la persona |
| `id_domicilio_notif_act` | `BIGINT` | SÍ | FK+IDX | Domicilio postal vigente para notificar | Puede diferir del domicilio infractor |
| `resumen_hecho` | `VARCHAR(1000)` | SÍ | — | Descripción del hecho | Evitar `TEXT`; ampliar solo si se justifica |
| `id_loc_infr_malvinas` | `VARCHAR(8)` | SÍ | IDX | Código localidad local del hecho | Ej. `LP`; aplica al lugar de infracción en Malvinas |
| `localidad_infr_malvinas_version_id` | `BIGINT` | SÍ | FK+IDX | Versión localidad local del hecho | FK a `geo_malv_localidad_version.localidad_version_id`; congela la localidad vigente al momento del labrado |
| `id_tca_infr_malvinas` | `VARCHAR(10)` | SÍ | IDX | Código calle local del hecho | Preserva ceros iniciales; ejemplo `02211` |
| `calle_infr_malvinas_version_id` | `BIGINT` | SÍ | FK+IDX | Versión calle local del hecho | FK a `geo_malv_calle_version.calle_version_id`; congela la calle vigente al momento del labrado |
| `altura_infr` | `INT UNSIGNED` | SÍ | IDX | Altura del hecho | Informada o estimada |
| `altura_origen_infr` | `SMALLINT` | SÍ | IDX | Origen de altura del hecho | Catálogo `altura_origen`; NULL si no aplica |
| `si_altura_infr_estimada` | `BOOLEAN` | NO | IDX | Altura estimada | True si vino de reverse/interpolación; default `false` |
| `lat_infr` | `DECIMAL(12,8)` | SÍ | — | Latitud final del hecho | Punto final aceptado |
| `lon_infr` | `DECIMAL(12,8)` | SÍ | — | Longitud final del hecho | Punto final aceptado |
| `origen_ubicacion_infr` | `SMALLINT` | SÍ | IDX | Origen ubicación del hecho | Catálogo `origen_ubicacion` |
| `si_ubicacion_infr_manual` | `BOOLEAN` | NO | IDX | Punto ajustado/manual | True si inspector corrigió o cargó manualmente; default `false` |
| `si_dom_txt_infr` | `BOOLEAN` | NO | — | Lugar del hecho con texto libre | Cuando no hay normalización completa |
| `dom_txt_infr` | `VARCHAR(255)` | SÍ | — | Texto del lugar del hecho | No es domicilio notificable |
| `si_eje_urb` | `BOOLEAN` | NO | — | En ejido/eje urbano | Dato obtenido de API local/GIS cuando esté disponible |
| `codigo_qr` | `VARCHAR(512)` | NO | UNIQUE | Token QR protegido | AES o mecanismo equivalente; no JSON plano |
| `qr_payload_version` | `SMALLINT` | NO | — | Versión payload QR | Inicialmente siempre `0` |
| `bloque_actual` | `CHAR(4)` | NO | IDX | Bloque funcional actual | Enum Spring: `CAPT`, `ENRI`, `NOTI`, `ANAL`, `GEXT`, `ARCH`, `CERR` |
| `est_proc_act` | `CHAR(4)` | NO | IDX | Estado procesal actual | Enum Spring productivo |
| `sit_adm_act` | `CHAR(4)` | NO | IDX | Situación administrativa actual | Enum Spring productivo: activa, paralizada, archivo, gestión externa, cerrada |
| `resultado_final` | `SMALLINT NOT NULL` | SÍ | IDX | Resultado final | Resultado consolidado del expediente | Valores 0=SIN_RESULTADO_FINAL, 1=PAGO_VOLUNTARIO_PAGADO, 2=ABSUELTO, 3=CONDENA_FIRME, 4=ANULADO. P1 cerrada. |
| `esta_cerrada` | `BOOLEAN` | NO | IDX | Cierre definitivo | Cierre explícito |
| `id_motivo_archivo_actual` | `BIGINT` | SÍ | FK+IDX | Motivo de archivo actual | Cache desde `fal_acta_archivo`; fuente: motivo administrable |
| `permite_reingreso` | `BOOLEAN` | SÍ | IDX | Permite reingreso | Cache desde motivo/ciclo de archivo activo |
| `fh_cierre` | `DATETIME(6)` | SÍ | IDX | Fecha cierre | Fecha funcional |
| `fh_archivo` | `DATETIME(6)` | SÍ | IDX | Fecha archivo | Fecha funcional |
| `resultado_firma_infractor` | `SMALLINT` | NO | IDX | Resultado firma ólógrafa del infractor | Catálogo resultado_firma_infractor |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación | Auditoría técnica |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría técnica |

Reglas:

- `nro_acta` es resultado visible; `id_talonario` + `nro_talonario_usado` trazan origen de numeración.
- `lat_captura`/`lon_captura` registran la posición del dispositivo al momento de labrar o cargar el acta. No reemplazan al lugar final del hecho.
- `lat_infr`/`lon_infr` registran el punto final aceptado para la infracción/hecho.
- Si el inspector corrige el punto geocodificado, `si_ubicacion_infr_manual = true` y `origen_ubicacion_infr = AJUSTE_MANUAL_SOBRE_GEOCODIFICACION`.
- Si el punto se coloca manualmente sin geocodificación, `si_ubicacion_infr_manual = true` y `origen_ubicacion_infr = PUNTO_MANUAL` o `PUNTO_MANUAL_REVERSE` según corresponda.
- La API puede mostrar `quality`, `source`, `score`, `warnings` y evidencias GIS, pero esos valores no se persisten en `fal_acta` mientras no gobiernen operación, filtros, documentos o bandejas.
- El texto preprocesado del lugar del hecho vive en `fal_acta_snapshot`, no como fuente primaria en `fal_acta`.
- `codigo_qr` vive en `fal_acta` porque nace con el acta y sirve para portal/integración/offline.
- `qr_payload_version = 0` en la primera versión; se incrementará si cambia el payload.
- No se reintroducen campos viejos de pago como simples números sueltos; los montos viven en valorizaciones/obligaciones.
- Los campos de estado actual de `fal_acta` son la proyección transaccional principal del expediente para operación diaria.
- Las tablas históricas de ciclos (`fal_acta_archivo`, `fal_acta_paralizacion`, `fal_acta_gestion_externa`) son la fuente primaria del detalle del ciclo, fechas, usuarios, documentos y resultados específicos.
- Toda acción que archive, reingrese, paralice, reactive, envíe a gestión externa, reingrese desde gestión externa, cierre o cambie estado debe actualizar en la misma transacción:
  - la tabla histórica correspondiente;
  - los campos actuales de `fal_acta`;
  - el evento append-only en `fal_acta_evento`;
  - el snapshot si se actualiza en línea, o marcarlo para rebuild.
- Si hubiera diferencia entre el detalle de ciclo y los campos actuales, se debe reconstruir desde la tabla histórica activa y eventos; `fal_acta_snapshot` nunca manda sobre `fal_acta`.
- En lugar del hecho `MALVINAS_LOCAL`, si la localidad/calle fueron seleccionadas desde catálogo local, se guardan los códigos locales (`id_loc_infr_malvinas`, `id_tca_infr_malvinas`) y las versiones usadas (`localidad_infr_malvinas_version_id`, `calle_infr_malvinas_version_id`).
- Los códigos locales permiten integración, búsqueda y compatibilidad con sistemas municipales.
- Los campos `*_version_id` congelan la versión exacta del catálogo local al momento del labrado.
- Si el lugar del hecho se cargó como texto libre o no se pudo normalizar completamente, los IDs de versión pueden quedar NULL y debe quedar marcado `si_dom_txt_infr = true`.

#### `bloque_actual` — valores productivos

| Código DB | Nombre dominio | Descripción |
|---|---|---|
| `CAPT` | `CAPTURA` | Captura/labrado inicial |
| `ENRI` | `ENRIQUECIMIENTO` | Enriquecimiento/completitud del acta |
| `NOTI` | `NOTIFICACION` | Notificación del acta, fallo u otra pieza |
| `ANAL` | `ANALISIS` | Análisis, resolución, fallo, pagos y apelación |
| `GEXT` | `GESTION_EXTERNA` | Gestión externa |
| `ARCH` | `ARCHIVO` | Archivo |
| `CERR` | `CERRADA` | Cierre definitivo |

Equivalencias históricas sólo para compatibilidad/migración:

| Valor histórico | Valor productivo |
|---|---|
| `D1_CAPTURA` | `CAPT` |
| `D2_ENRIQUECIMIENTO` | `ENRI` |
| `D3_DOCUMENTAL` | eliminado |
| `D4_NOTIFICACION` | `NOTI` |
| `D5_ANALISIS` | `ANAL` |

Regla:

- No persistir valores `D1/D2/D4/D5` en el modelo productivo.
- No crear bloque documental.
- Documental se resuelve con bandeja, sub-bandeja, documentos y firma.


#### Catálogo `resultado_firma_infractor`

| ID | Código | Uso |
|---:|---|---|
| 1 | `FIRMADA` | El infractor firmó en el dispositivo. |
| 2 | `SE_NIEGA_A_FIRMAR` | El infractor está presente pero se niega a firmar. |
| 3 | `INFRACTOR_NO_PRESENTE` | No se pudo requerir firma porque el infractor no estaba presente. |
| 4 | `IMPOSIBILITADO_PARA_FIRMAR` | Está presente, pero no puede firmar por imposibilidad física/material. |
| 5 | `NO_CAPTURADA_POR_FALLA_TECNICA` | No se pudo capturar por problema del dispositivo/sistema. |

Reglas de `resultado_firma_infractor`:
- Campo obligatorio para acta formal/labrada/sincronizada.
- `FIRMADA` exige evidencia tipo `FIRMA_OLOGRAFA_INFRACTOR` asociada al acta.
- Los demás valores no exigen evidencia de firma.
- Siempre debe quedar asentado el resultado en el acta formal.

### 2.2 Tabla `fal_persona`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador de persona | PK técnica |
| `tipo_persona` | `SMALLINT` | NO | IDX | Tipo de persona | Solo `FISICA` o `JURIDICA` |
| `tipo_doc` | `SMALLINT` | SÍ | IDX | Tipo de documento | Catálogo funcional |
| `nro_doc` | `VARCHAR(20)` | SÍ | IDX | Número documento | Funcional si existe |
| `apellido` | `VARCHAR(80)` | SÍ | IDX | Apellido | Persona física |
| `nombres` | `VARCHAR(100)` | SÍ | IDX | Nombres | Persona física |
| `razon_social` | `VARCHAR(64)` | SÍ | IDX | Razón social | Persona jurídica |
| `nombre_mostrar` | `VARCHAR(64)` | SÍ | IDX | Nombre legible | Cache/control visual |
| `email_principal` | `VARCHAR(160)` | SÍ | — | Email principal | No reemplaza notificación formal |
| `telefono_principal` | `VARCHAR(20)` | SÍ | — | Teléfono principal | Formato normalizado |
| `Id_Suj` | `BIGINT` | SÍ | IDX | Sujeto/rubro de ingresos | Para Faltas será `20`; NULL hasta crear cuenta |
| `Id_Bie` | `BIGINT` | SÍ | IDX | Cuenta/bien en ingresos | Se genera al materializar deuda/plan |
| `SujBieEstado` | `SMALLINT` | SÍ | IDX | Estado cuenta Suj/Bie | Catálogo funcional |
| `fh_sujBie_Creacion` | `DATETIME(6)` | SÍ | IDX | Fecha creación cuenta Suj/Bie | Informada por integración |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación | Auditoría técnica |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría técnica |

Catálogo `tipo_persona`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `FISICA` | Persona física |
| 2 | `JURIDICA` | Persona jurídica |

Catálogo `SujBieEstado`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `SIN_CUENTA` | Persona sin cuenta creada |
| 2 | `PENDIENTE_CREACION` | Solicitud de creación iniciada |
| 3 | `ACTIVA` | Cuenta creada y usable |
| 4 | `ERROR_CREACION` | Error al crear/vincular |
| 5 | `INACTIVA` | Cuenta no usable o dada de baja |

### 2.3 Tabla `fal_persona_domicilio`

`fal_persona_domicilio` guarda domicilios de persona/infractor/notificación. No es el lugar del hecho.  
Debe soportar domicilios locales de Malvinas Argentinas y domicilios externos IGN/INDEC, consumidos desde la API de domicilios implementada en Spring sobre catálogos MariaDB.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador domicilio | PK técnica |
| `persona_id` | `BIGINT` | NO | FK+IDX | Persona | 1 persona : N domicilios |
| `acta_origen_id` | `BIGINT` | SÍ | FK+IDX | Acta origen | Si nació en contexto de acta |
| `tipo_domicilio` | `SMALLINT` | NO | IDX | Tipo domicilio | REAL, LEGAL, FISCAL, CONSTITUIDO, HALLADO, OTRO |
| `origen_domicilio` | `SMALLINT` | NO | IDX | Origen | LABRADO, INVESTIGACIÓN, DDJJ, REINTENTO, PORTAL, EXTERNO, OPERADOR |
| `modo_domicilio` | `SMALLINT` | NO | IDX | Modo territorial | `MALVINAS_LOCAL` o `EXTERNO` |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | No borrar domicilios usados |
| `si_notificable` | `BOOLEAN` | NO | IDX | Puede notificarse | Requerido para canal físico |
| `si_principal` | `BOOLEAN` | NO | IDX | Principal operativo | Según regla de servicio |
| `id_provincia` | `SMALLINT` | SÍ | IDX | Provincia | ID de `geo_ign_provincia.id`; para Buenos Aires `6` |
| `unidad_territorial_tipo` | `SMALLINT` | SÍ | IDX | Tipo unidad territorial | Municipio, departamento o CABA |
| `id_unidad_territorial` | `INT` | SÍ | IDX | Unidad territorial | Municipio/departamento/CABA. Para Malvinas: `60515` |
| `id_localidad` | `BIGINT` | SÍ | IDX | Localidad externa/censal | Solo modo `EXTERNO`; referencia catálogo IGN/INDEC en MariaDB |
| `id_calle` | `BIGINT` | SÍ | IDX | Calle externa | Solo modo `EXTERNO`; referencia `geo_indec_calles.id` |
| `id_loc_malvinas` | `VARCHAR(8)` | SÍ | IDX | Código localidad local Malvinas | Solo modo `MALVINAS_LOCAL`, ejemplo `LP` |
| `localidad_malvinas_version_id` | `BIGINT` | SÍ | FK+IDX | Versión localidad local Malvinas | FK a `geo_malv_localidad_version.localidad_version_id`; solo modo `MALVINAS_LOCAL` |
| `id_tca_malvinas` | `VARCHAR(10)` | SÍ | IDX | Código calle local Malvinas | Solo modo `MALVINAS_LOCAL`, ejemplo `02211`. No convertir a número |
| `calle_malvinas_version_id` | `BIGINT` | SÍ | FK+IDX | Versión calle local Malvinas | FK a `geo_malv_calle_version.calle_version_id`; solo modo `MALVINAS_LOCAL` |
| `calle_txt` | `VARCHAR(120)` | SÍ | — | Calle legible/cache | Nombre calle actual o texto libre |
| `altura` | `INT UNSIGNED` | SÍ | IDX | Altura | NULL si sin altura |
| `si_sin_altura` | `BOOLEAN` | NO | — | Sin altura | Representa S/N sin contaminar altura |
| `unidad_funcional` | `VARCHAR(12)` | SÍ | — | Piso/depto/UF | Ej. PB, 2A, UF3 |
| `codigo_postal` | `VARCHAR(8)` | SÍ | IDX | Código postal | AR / CPA sin separadores |
| `domicilio_txt` | `VARCHAR(255)` | SÍ | — | Texto legible/cache | Preview congelado; no fuente primaria |
| `validacion_domicilio` | `VARCHAR(40)` | SÍ | IDX | Resultado validación | Ej. `OK`, `ALTURA_FUERA_DE_RANGO`, `AMBIGUA_LOCALIDAD` |
| `si_normalizado_parcial` | `BOOLEAN` | NO | — | Normalización parcial | Control operativo |
| `lat` | `DECIMAL(12,8)` | SÍ | — | Latitud domicilio | Solo si se obtuvo confiablemente |
| `lon` | `DECIMAL(12,8)` | SÍ | — | Longitud domicilio | Solo si se obtuvo confiablemente |
| `origen_ubicacion` | `SMALLINT` | SÍ | IDX | Origen punto domicilio | Catálogo `origen_ubicacion` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación | Auditoría técnica |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría técnica |

Reglas:

- Los domicilios usados por documentos, notificaciones, intentos o piezas formales no se sobrescriben.
- Si el domicilio todavía no fue usado en documento generado, notificación emitida, intento registrado o pieza formal instanciada, una corrección simple puede aplicarse sobre la misma fila.
- Si el domicilio ya fue usado en documento generado, notificación emitida, intento registrado o pieza formal instanciada, una corrección o hallazgo posterior crea una nueva fila o versión operativa.
- `modo_domicilio = MALVINAS_LOCAL` aplica únicamente para provincia Buenos Aires (`id_provincia = 6`), unidad tipo `MUNICIPIO` y Malvinas Argentinas (`id_unidad_territorial = 60515`).
- En modo `MALVINAS_LOCAL`, `id_loc_malvinas` e `id_tca_malvinas` guardan los códigos locales; `localidad_malvinas_version_id` y `calle_malvinas_version_id` congelan las versiones seleccionadas desde los catálogos locales.
- En modo `EXTERNO`, `id_localidad` e `id_calle` guardan IDs numéricos del catálogo IGN/INDEC local en MariaDB; los campos `id_loc_malvinas`, `id_tca_malvinas`, `localidad_malvinas_version_id` y `calle_malvinas_version_id` quedan NULL.
- Los campos `ver_provincia`, `ver_unidad_territorial`, `ver_localidad` y `ver_calle` no se usan para catálogos geográficos externos porque las tablas reales IGN/INDEC/BAHRA en MariaDB no exponen una versión funcional referenciable por Faltas.
- `domicilio_txt` queda como cache legible para búsqueda, QA, control visual, documentos y notificaciones.
- `domicilio_txt` no es fuente primaria: prevalecen los campos estructurados y, para Malvinas local, las FK a tablas versionadas.
- `lat` y `lon` son opcionales en domicilio de persona. Si no hay reverse geocoding, geocodificación, selección manual o fuente confiable, quedan NULL.
- Si se informa `lat`/`lon`, debe informarse `origen_ubicacion`.
- No se persiste `quality_ubicacion` en domicilios de persona porque no tiene uso operativo confirmado.

#### Catálogo `modo_domicilio`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `MALVINAS_LOCAL` | Domicilio resuelto con catálogos locales de Malvinas Argentinas |
| 2 | `EXTERNO` | Domicilio resuelto con catálogo nacional/local IGN-INDEC |

#### Catálogo `unidad_territorial_tipo`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `MUNICIPIO` | Municipio |
| 2 | `DEPARTAMENTO` | Departamento/partido |
| 3 | `CIUDAD_AUTONOMA` | Ciudad Autónoma de Buenos Aires |

#### Catálogo `origen_ubicacion`

| ID | Código enum | Descripción |
|---:|---|---|
| 0 | `SIN_UBICACION` | Sin ubicación resuelta |
| 1 | `CALLE_ALTURA_GEOCODIFICADA` | Punto producido automáticamente por calle, altura y localidad |
| 2 | `AJUSTE_MANUAL_SOBRE_GEOCODIFICACION` | Inspector corrigió o movió el punto automático |
| 3 | `PUNTO_MANUAL` | Inspector colocó punto manual |
| 4 | `PUNTO_MANUAL_REVERSE` | Punto manual con reverse geocoding aplicado |
| 5 | `PARCELA_SELECCIONADA` | Ubicación tomada desde selección de parcela |
| 6 | `GPS_DISPOSITIVO` | Punto tomado del GPS/dispositivo |
| 7 | `MANUAL_SIN_MAPA` | Carga manual por contingencia |

#### Catálogo `origen_pos_captura`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `GPS_DISPOSITIVO` | GPS del dispositivo |
| 2 | `RED_DISPOSITIVO` | Posición aproximada por red |
| 3 | `MANUAL` | Informada manualmente |
| 4 | `NO_DISPONIBLE` | No disponible |

#### Catálogo `altura_origen`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PARCELA` | Altura obtenida desde domicilio/dato de parcela |
| 2 | `CALLE_CERCANA` | Altura estimada por calle cercana/interpolación |


### 2.4 Tabla `fal_acta_evento`

Registra el timeline append-only del expediente.

La tabla no guarda payload libre. Cuando el evento necesita detalle estructurado, ese detalle debe vivir en la tabla funcional correspondiente y el evento conserva la referencia.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador evento | Append-only |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente dueño |
| `tipo_evt` | `CHAR(6)` | NO | IDX | Tipo de evento | Enum Spring productivo, hecho real de dominio |
| `origen_evt` | `SMALLINT` | NO | IDX | Origen/canal del evento | Usuario, sistema, integración, portal, etc. |
| `fh_evt` | `DATETIME(6)` | NO | IDX | Fecha/hora evento | Orden del timeline |
| `bloque_func` | `CHAR(4)` | SÍ | IDX | Bloque funcional | Contexto al ocurrir: `CAPT`, `ENRI`, `NOTI`, `ANAL`, `GEXT`, `ARCH`, `CERR` |
| `est_proc_ant` | `SMALLINT` | SÍ | — | Estado anterior | Si aplica |
| `est_proc_nvo` | `SMALLINT` | SÍ | — | Estado nuevo | Si aplica |
| `sit_adm_ant` | `SMALLINT` | SÍ | — | Situación anterior | Si aplica |
| `sit_adm_nva` | `SMALLINT` | SÍ | — | Situación nueva | Si aplica |
| `actor_tipo` | `SMALLINT` | SÍ | IDX | Tipo de actor | USUARIO_INTERNO, INSPECTOR, INFRACTOR, SISTEMA, INTEGRACION, NOTIFICADOR |
| `actor_id` | `CHAR(36)` | SÍ | IDX | Identidad técnica del actor | Subject IDP si existe; usuario interno, inspector autenticado, infractor portal o integración autenticada |
| `actor_ref` | `VARCHAR(80)` | SÍ | IDX | Referencia externa del actor | Solo si el actor no tiene subject IDP o requiere referencia externa controlada |
| `id_docu_rel` | `BIGINT` | SÍ | FK+IDX | Documento relacionado | Si aplica |
| `id_notif_rel` | `BIGINT` | SÍ | FK+IDX | Notificación relacionada | Si aplica |
| `id_pres_rel` | `BIGINT` | SÍ | IDX | Presentación relacionada | Si se modela una presentación formal |
| `id_user_evt` | `CHAR(36)` | SÍ | IDX | Usuario interno ejecutor | Cuando el evento fue ejecutado por usuario municipal autenticado |
| `si_evt_cierre` | `BOOLEAN` | NO | IDX | Evento de cierre | Si cierra expediente |
| `si_evt_ext` | `BOOLEAN` | NO | IDX | Evento externo | Integración/externo |
| `si_permite_reing` | `BOOLEAN` | NO | — | Permite reingreso | Resultado del evento |
| `descripcion_legible` | `VARCHAR(255)` | SÍ | — | Descripción legible | Timeline/API/QA |
| `correlacion_id` | `VARCHAR(60)` | SÍ | IDX | Correlación técnica | Request/integración |

Reglas:

- Append-only: no se edita ni borra.
- No usar `payload_json`.
- `actor_tipo` indica la naturaleza funcional del actor.
- `actor_id` guarda identidad técnica estable cuando existe subject IDP o identidad autenticada equivalente.
- `actor_ref` permite trazar actores externos sin forzar FK artificial.
- `id_user_evt` se mantiene como acceso directo para auditoría municipal cuando el actor es usuario interno.
- Para eventos de portal, `actor_tipo = INFRACTOR` y `actor_id` puede ser el subject/cuenta del portal si existe.
- Para eventos de integración, `actor_tipo = INTEGRACION` y `actor_ref` puede guardar sistema/código de integración.
- Las relaciones estructuradas deben resolverse con columnas explícitas (`id_docu_rel`, `id_notif_rel`, tabla funcional específica) y no con JSON libre.
- El catálogo de eventos surge del análisis funcional validado, no del DDL histórico.

Reglas productivas de `tipo_evt`:

- `tipo_evt` es `CHAR(6)`, enum/constante Spring.
- No se crea tabla catálogo editable por defecto.
- Sólo se registran hechos reales de dominio.
- No registrar eventos de proyección, bandeja, demo o estado.
- Los mock-only de dominio se mapean, no se copian literal.
- `CONFIR` / `CONDENA_FIRME` se conserva como evento productivo real.
- Si un evento necesita detalle estructurado, el detalle vive en la tabla funcional correspondiente y el evento conserva FK/referencia.

Eventos explícitamente prohibidos como productivos:

| Valor | Motivo |
|---|---|
| `PASE_BANDEJA` | Proyección |
| `ASIGNACION` | Operativo/visual |
| `ASIGNACION_ANALISTA` | Operativo/visual |
| `PENDIENTE_FIRMA` | Estado, no hecho |
| `PENDIENTE_NOTIFICACION` | Estado, no hecho |
| `NOTIFICACION_EN_CURSO` | Estado, no hecho |
| `PASE_DEMO` | Demo |
| `SEGUIMIENTO` | Demo/genérico |
| `OBSERVACION` | Debe ir a `fal_observacion` |
| `RECORDATORIO` | Demo/genérico |
| valores `_DEMO` | Demo |

### 2.5 Tabla `fal_acta_snapshot`

`fal_acta_snapshot` es proyección/cache regenerable, un registro por acta.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Fuente primaria |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta | `fal_acta` |
| `fh_acta` | `DATETIME(6)` | SÍ | IDX | Fecha acta | `fal_acta` |
| `nro_acta` | `VARCHAR(30)` | SÍ | IDX | Número visible | `fal_acta` |
| `tipo_acta` | `SMALLINT` | NO | IDX | Tipo acta | `fal_acta` |
| `id_dep` | `BIGINT` | SÍ | IDX | Dependencia | `fal_acta` |
| `ver_dep` | `SMALLINT` | SÍ | — | Versión dependencia | `fal_acta` |
| `id_insp` | `BIGINT` | SÍ | IDX | Inspector | `fal_acta` |
| `ver_insp` | `SMALLINT` | SÍ | — | Versión inspector | `fal_acta` |
| `id_persona_infractor` | `BIGINT` | NO | IDX | Persona | `fal_acta` |
| `nombre_infractor` | `VARCHAR(64)` | SÍ | IDX | Nombre legible | `fal_persona.nombre_mostrar` |
| `doc_infractor_txt` | `VARCHAR(20)` | SÍ | IDX | Documento formateado | `fal_persona.tipo_doc` + `fal_persona.nro_doc` |
| `id_domicilio_infractor_act` | `BIGINT` | SÍ | IDX | Domicilio infractor | `fal_acta` |
| `id_domicilio_notif_act` | `BIGINT` | SÍ | IDX | Domicilio notificación | `fal_acta` |
| `domicilio_infractor_txt` | `VARCHAR(160)` | SÍ | — | Texto domicilio infractor | `fal_persona_domicilio.domicilio_txt` |
| `domicilio_notif_txt` | `VARCHAR(160)` | SÍ | — | Texto domicilio notificación | `fal_persona_domicilio.domicilio_txt` |
| `domicilio_infr_txt` | `VARCHAR(128)` | SÍ | IDX | Domicilio/lugar del hecho | Derivado de `fal_acta.id_tca_infr_malvinas`, `altura_infr`, `id_loc_infr_malvinas` |
| `localidad_infr_txt` | `VARCHAR(36)` | SÍ | IDX | Localidad del hecho | Catálogo local Malvinas |
| `calle_infr_txt` | `VARCHAR(48)` | SÍ | IDX | Calle del hecho | Catálogo local Malvinas |
| `ubicacion_infr_resumen` | `VARCHAR(128)` | SÍ | IDX | Resumen ubicación del hecho | Calle + altura + localidad |
| `si_eje_urb` | `BOOLEAN` | NO | IDX | En ejido/eje urbano | `fal_acta.si_eje_urb` |
| `licencia_provincia_txt` | `VARCHAR(64)` | SÍ | — | Provincia emisora licencia | Catálogo provincia |
| `licencia_unidad_txt` | `VARCHAR(80)` | SÍ | — | Municipio/departamento/CABA emisor | Catálogo unidad territorial |
| `nomenclatura_resumen` | `VARCHAR(120)` | SÍ | IDX | Resumen nomenclatura | Derivado de `fal_acta_contravencion` |
| `Id_Bie_i` | `BIGINT` | SÍ | IDX | Cuenta inmueble/CVP | `fal_acta_contravencion.Id_Bie_i` |
| `Id_Bie_c` | `BIGINT` | SÍ | IDX | Cuenta comercio | `fal_acta_contravencion.Id_Bie_c` |
| `bloque_actual` | `CHAR(4)` | NO | IDX | Bloque actual | `fal_acta` |
| `est_proc_act` | `CHAR(4)` | NO | IDX | Estado procesal | `fal_acta` |
| `sit_adm_act` | `CHAR(4)` | NO | IDX | Situación adm. | `fal_acta` |
|`resultado_final` | `SMALLINT NOT NULL` | Sí | IDX | Resultado final | `fal_acta` |
| `esta_cerrada` | `BOOLEAN` | NO | IDX | Cerrada | `fal_acta` |
| `id_motivo_archivo_actual` | `BIGINT` | SÍ | FK+IDX | Motivo archivo actual | `fal_acta` / `fal_acta_archivo` |
| `permite_reingreso` | `BOOLEAN` | SÍ | IDX | Reingreso | `fal_acta` |
| `fh_cierre` | `DATETIME(6)` | SÍ | IDX | Fecha cierre | `fal_acta` |
| `fh_archivo` | `DATETIME(6)` | SÍ | IDX | Fecha archivo | `fal_acta` |
| `cod_bandeja` | `VARCHAR(50)` | SÍ | IDX | Bandeja operativa | Reglas dominio |
| `si_visible_bandeja` | `BOOLEAN` | NO | IDX | Visible bandeja | Reglas dominio |
| `prioridad` | `SMALLINT` | SÍ | IDX | Prioridad | Reglas dominio |
| `accion_pendiente` | `CHAR(6)` | SÍ | IDX | Acción pendiente | Reglas dominio |
| `motivo_paralizacion_act` | `SMALLINT` | SÍ | IDX | Motivo paralización activa | `fal_acta_paralizacion` |
| `si_bloqueante_med_prev` | `BOOLEAN` | NO | IDX | Bloqueo por medida | `fal_acta_bloqueante_cierre_material` |
| `si_bloqueante_rodado` | `BOOLEAN` | NO | IDX | Bloqueo por rodado | `fal_acta_bloqueante_cierre_material` |
| `si_bloqueante_doc_ret` | `BOOLEAN` | NO | IDX | Bloqueo por documentación | `fal_acta_bloqueante_cierre_material` |
| `valorizacion_operativa_id` | `BIGINT` | SÍ | FK+IDX | Valorización operativa seleccionada | `fal_acta_valorizacion` |
| `estado_valorizacion_operativa` | `SMALLINT` | SÍ | IDX | Estado del monto operativo | `fal_acta_valorizacion` |
| `tipo_valorizacion_operativa` | `SMALLINT` | SÍ | IDX | Tipo de monto operativo | `fal_acta_valorizacion` |
| `monto_operativo_vigente` | `DECIMAL(14,2)` | SÍ | — | Valor visible/proyectado seleccionado | `fal_acta_valorizacion` |
| `si_monto_confirmado` | `BOOLEAN` | NO | IDX | Monto confirmado | `fal_acta_valorizacion` |
| `si_muestra_monto_portal` | `BOOLEAN` | NO | IDX | Portal muestra monto | Reglas portal |
| `tipo_obligacion_pago` | `SMALLINT` | SÍ | IDX | Tipo obligación vigente | `fal_acta_obligacion_pago` |
| `estado_obligacion_pago` | `SMALLINT` | SÍ | IDX | Estado obligación | `fal_acta_obligacion_pago` |
| `monto_obligacion_pago` | `DECIMAL(14,2)` | SÍ | — | Monto obligación | `fal_acta_obligacion_pago` |
| `tipo_forma_pago_vigente` | `SMALLINT` | SÍ | IDX | Forma pago vigente | `fal_acta_forma_pago` |
| `estado_forma_pago_vigente` | `SMALLINT` | SÍ | IDX | Estado forma pago | `fal_acta_forma_pago` |
| `si_plan_pago` | `BOOLEAN` | NO | IDX | Tiene plan | `fal_acta_plan_pago_ref` |
| `estado_plan_pago` | `SMALLINT` | SÍ | IDX | Estado plan | `fal_acta_plan_pago_ref` |
| `cant_cuotas_plan` | `SMALLINT` | SÍ | — | Cuotas plan | Cache desde ingresos |
| `valor_cuota_plan` | `DECIMAL(14,2)` | SÍ | — | Valor cuota | Cache desde ingresos |
| `cant_cuotas_pagadas` | `SMALLINT` | SÍ | — | Cuotas pagadas | Cache operativo |
| `cant_cuotas_mora` | `SMALLINT` | SÍ | — | Cuotas en mora | Cache operativo |
| `cant_cuotas_mora_consec` | `SMALLINT` | SÍ | — | Mora consecutiva | Cache operativo |
| `cant_dias_mora` | `SMALLINT` | SÍ | — | Días mora | Cache operativo |
| `si_apta_intimacion` | `BOOLEAN` | NO | IDX | Apta intimación | Reglas pago/plan |
| `motivo_apta_intimacion` | `SMALLINT` | SÍ | IDX | Motivo aptitud | Reglas pago/plan |
| `si_pago_procesado` | `BOOLEAN` | NO | IDX | Pago procesado vigente | Movimientos pagos |
| `si_pago_confirmado` | `BOOLEAN` | NO | IDX | Pago confirmado vigente | Movimientos pagos |
| `fh_ult_sync_ingresos` | `DATETIME(6)` | SÍ | IDX | Última sincronización | Integración ingresos |
| `si_gestion_ext` | `BOOLEAN` | NO | IDX | Gestión externa activa | `fal_acta_gestion_externa` |
| `gestion_externa_activa_id` | `BIGINT` | SÍ | FK+IDX | Ciclo externo activo | `fal_acta_gestion_externa` |
| `tipo_gestion_ext` | `SMALLINT` | SÍ | IDX | Tipo gestión externa | `fal_acta_gestion_externa` |
| `estado_gestion_ext` | `SMALLINT` | SÍ | IDX | Estado gestión externa | `fal_acta_gestion_externa` |
| `resultado_gestion_ext` | `SMALLINT` | SÍ | IDX | Resultado externo | `fal_acta_gestion_externa` |
| `fh_vto_presentacion` | `DATE` | SÍ | IDX | Vto presentación | Reglas/plazos |
| `fh_vto_apelacion` | `DATE` | SÍ | IDX | Vto apelación | `fal_acta_fallo` |
| `fh_vto_apremio` | `DATE` | SÍ | IDX | Vto apremio | Reglas/apremio |
| `id_evt_ult` | `BIGINT` | SÍ | FK+IDX | Último evento | `fal_acta_evento` |
| `id_docu_ult` | `BIGINT` | SÍ | FK+IDX | Último documento | `fal_documento` |
| `id_notif_ult` | `BIGINT` | SÍ | FK+IDX | Última notificación | `fal_notificacion` |
| `fh_snapshot` | `DATETIME(6)` | NO | IDX | Fecha rebuild | Control técnico |
| `rebuild_id` | `BIGINT` | SÍ | IDX | Ciclo rebuild | Control técnico |
| `fh_ult_mod` | `DATETIME(6)` | NO | — | Última modificación | Control técnico |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario/proceso | Control técnico |

Reglas:

- Snapshot no es fuente primaria.
- `valorizacion_operativa_id` identifica cuál de las valorizaciones confirmadas/vigentes se usa para la operación actual del acta.
- Pueden existir varias valorizaciones confirmadas y vigentes por tipo (`PAGO_VOLUNTARIO`, `CONDENA`, `INFRACCION_BASE`, etc.); el snapshot no decide la verdad, solo proyecta una selección operativa para UX.
- La selección de `valorizacion_operativa_id` se determina por reglas de dominio: obligación de pago materializada vigente, condena firme/operable, pago voluntario habilitado o valor base, según estado del expediente.
- `monto_operativo_vigente` es proyección para UX; la verdad vive en `fal_acta_valorizacion` y, si ya hay comprobante emitido, en `fal_acta_obligacion_pago`.
- Si hay gestión externa activa, `gestion_externa_activa_id` debe apuntar al ciclo activo y `estado_gestion_ext` debe reflejar su estado operativo.
- No se derivan pagos de tablas viejas descartadas.
- `doc_infractor_txt` fusiona tipo y número de documento para uso informativo en bandejas. Ejemplos: `DNI 22276143`, `CUIT 20-12345678-5`.
- `tipo_doc_infractor` y `nro_doc_infractor` no se proyectan por separado en snapshot; la fuente primaria sigue siendo `fal_persona`.
- `domicilio_infractor_txt`, `domicilio_notif_txt`, `domicilio_infr_txt`, `ubicacion_infr_resumen`, `licencia_provincia_txt`, `licencia_unidad_txt` y `nomenclatura_resumen` son caches regenerables para bandejas/listados.
- No proyectar en snapshot `quality`, `source`, `score`, `warnings` ni demás telemetría GIS salvo que una bandeja real lo requiera explícitamente.

### 2.6 Tabla `fal_acta_evidencia`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Evidencia | PK técnica |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | 1 acta : N evidencias |
| `tipo_evid` | `SMALLINT` | NO | IDX | Tipo evidencia | Catálogo tipo_evid |
| `storage_key` | `VARCHAR(255)` | NO | IDX | Clave storage | No guardar binario |
| `nombre_archivo` | `VARCHAR(120)` | SÍ | — | Nombre original | UI/auditoría |
| `mime_type` | `SMALLINT` | NO | IDX | MIME normalizado | Enum/catálogo funcional, no texto libre |
| `hash_evid` | `VARCHAR(128)` | SÍ | IDX | Hash integridad | Control técnico |
| `orden_evid` | `SMALLINT` | NO | — | Orden | Presentación |
| `fh_captura` | `DATETIME(6)` | SÍ | IDX | Fecha captura | Si viene del origen |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |


#### Catálogo `tipo_evid`

| ID | Código | Descripción |
|---:|---|---|
| 1 | `FOTO` | Fotografía capturada en campo |
| 2 | `VIDEO` | Video capturado en campo |
| 3 | `AUDIO` | Audio capturado en campo |
| 4 | `DOCUMENTO` | Documento adjunto externo |
| 5 | `OTRO` | Otra evidencia no categorizada |
| 6 | `FIRMA_OLOGRAFA_INFRACTOR` | Firma ólógrafa del infractor capturada en dispositivo |

### 2.7 Tabla `fal_observacion`

Observaciones generales vinculables a entidades del dominio.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Observación | PK técnica |
| `entidad_tipo` | `SMALLINT` | NO | IDX | Tipo entidad observada | Catálogo cerrado `entidad_tipo`; no usar valores abiertos |
| `entidad_id` | `BIGINT` | NO | IDX | ID entidad | ID de la entidad observada |
| `tipo_obs` | `SMALLINT` | SÍ | IDX | Tipo observación | Catálogo funcional |
| `observacion` | `VARCHAR(1000)` | NO | — | Texto | Evitar `TEXT`; longitud controlada |
| `origen_obs` | `SMALLINT` | SÍ | IDX | Origen | Usuario, sistema, integración |
| `fh_alta` | `DATETIME(6)` | NO | IDX | Alta | Fecha funcional/técnica |
| `id_user_alta` | `CHAR(36)` | NO | IDX | Usuario alta | Auditoría |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | No borrar salvo política |

Catálogo inicial `entidad_tipo`:

| ID | Código enum | Entidad observada |
|---:|---|---|
| 1 | `ACTA` | `fal_acta` |
| 2 | `PERSONA` | `fal_persona` |
| 3 | `DOMICILIO` | `fal_persona_domicilio` |
| 4 | `DOCUMENTO` | `fal_documento` |
| 5 | `EVIDENCIA` | `fal_acta_evidencia` |
| 6 | `NOTIFICACION` | `fal_notificacion` |
| 7 | `NOTIFICACION_INTENTO` | `fal_notificacion_intento` |
| 8 | `FALLO` | `fal_acta_fallo` |
| 9 | `APELACION` | `fal_acta_apelacion` |
| 10 | `GESTION_EXTERNA` | `fal_acta_gestion_externa` |
| 11 | `PARALIZACION` | `fal_acta_paralizacion` |
| 12 | `ARCHIVO` | `fal_acta_archivo` |
| 13 | `MEDIDA_PREVENTIVA` | `fal_acta_medida_preventiva` |
| 14 | `BLOQUEANTE_CIERRE_MATERIAL` | `fal_acta_bloqueante_cierre_material` |
| 15 | `ARTICULO_INFRINGIDO` | `fal_acta_articulo_infringido` |
| 16 | `VALORIZACION` | `fal_acta_valorizacion` |
| 17 | `OBLIGACION_PAGO` | `fal_acta_obligacion_pago` |
| 18 | `FORMA_PAGO` | `fal_acta_forma_pago` |
| 19 | `PLAN_PAGO` | `fal_acta_plan_pago_ref` |
| 20 | `MOVIMIENTO_PAGO` | `fal_acta_pago_movimiento` |
| 21 | `TALONARIO` | `num_talonario` |
| 22 | `MOVIMIENTO_TALONARIO` | `num_talonario_movimiento` |

Regla: `fal_observacion` es polimórfica, pero no libre. Si aparece una nueva entidad observable, se incorpora explícitamente al catálogo con ID estable y código legible.

---

## 3. Normativa, artículos, medidas preventivas y valorización

### 3.1 Regla funcional de composición de infracción

La infracción se compone desde normativas y artículos infringidos.

Cadena de selección:

```text
Dependencia del inspector
→ normativas habilitadas para esa dependencia
→ artículos disponibles dentro de esas normativas
→ medidas preventivas habilitadas por esos artículos
→ artículos/medidas aplicados al acta
→ valorización automática o manual
→ confirmación del monto
```

Reglas:

- Un acta solo puede imputar normativas disponibles para la dependencia que la labra.
- Un acta puede tener varias normativas y varios artículos infringidos.
- Un artículo puede habilitar una o varias medidas preventivas.
- Un acta puede tener varias medidas preventivas aplicadas.
- La medida preventiva aplicada debe poder vincularse al artículo infringido que la habilitó.

### 3.2 Tabla `fal_dependencia_normativa`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_dep` | `BIGINT` | NO | PK+FK | Dependencia | Dependencia habilitada |
| `ver_dep` | `SMALLINT` | NO | PK | Versión dependencia | Habilitación histórica |
| `normativa_id` | `BIGINT` | NO | PK+FK | Normativa disponible | Solo estas normativas puede usar |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | No borrar si fue usada |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría técnica |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría técnica |

### 3.3 Tabla `fal_normativa_faltas`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Normativa | PK técnica |
| `codigo_norma` | `VARCHAR(12)` | NO | IDX | Código norma | Código funcional corto y estable |
| `version_norma` | `SMALLINT` | NO | IDX | Versión norma | Congela cambios |
| `nombre_norma` | `VARCHAR(120)` | NO | IDX | Nombre | Texto visible |
| `descripcion_norma` | `VARCHAR(255)` | SÍ | — | Descripción | Opcional |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | No borrar versiones usadas |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Vigencia normativa |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

### 3.4 Tabla `fal_articulo_normativa_faltas`

Define el artículo normativo y sus reglas económicas base. No maneja mínimos/máximos ni graduación automática.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Artículo | PK técnica |
| `normativa_id` | `BIGINT` | NO | FK+IDX | Normativa | Pertenece a normativa versionada |
| `codigo_articulo` | `VARCHAR(12)` | NO | IDX | Código artículo | Código corto, estable y legible |
| `version_articulo` | `SMALLINT` | NO | IDX | Versión artículo | Congela cambios |
| `nombre_articulo` | `VARCHAR(160)` | NO | IDX | Nombre/denominación | Visible |
| `descripcion_articulo` | `VARCHAR(1000)` | SÍ | — | Descripción | Texto controlado, no documento extenso |
| `cantidad_unidades` | `DECIMAL(14,4)` | NO | — | Cantidad base ordinaria | Cantidad definida por la normativa/artículo |
| `tipo_unidad` | `SMALLINT` | NO | IDX | Tipo de unidad ordinaria | `SALARIO`, `UNIDAD_FIJA` o `MONTO` |
| `si_tiene_pago_voluntario` | `BOOLEAN` | NO | IDX | Tiene regla de pago voluntario | Indica si el artículo define valor específico para pago voluntario |
| `cantidad_unidades_pago_voluntario` | `DECIMAL(14,4)` | SÍ | — | Cantidad para pago voluntario | NULL si no tiene regla diferenciada |
| `tipo_unidad_pago_voluntario` | `SMALLINT` | SÍ | IDX | Tipo unidad pago voluntario | `SALARIO`, `UNIDAD_FIJA` o `MONTO` |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | No borrar si usado |
| `fh_vig_desde` | `DATE` | NO | IDX | Vigente desde | Vigencia |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Vigente hasta | NULL si vigente |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- El artículo define cantidad y tipo de unidad.
- No hay mínimo/máximo en el artículo.
- No hay granulidad/graduación automática.
- Si administrativamente se determina otro valor, se registra en valorización/manualización del acta o del ítem.
- El pago voluntario puede tener cantidad/tipo unidad propio y puede ser distinto del valor ordinario.
- La disponibilidad final de pago voluntario para el acta depende del circuito/decisión de Dirección de Faltas.

Catálogo `tipo_unidad`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `SALARIO` | Valor expresado en cantidad de salarios |
| 2 | `UNIDAD_FIJA` | Valor expresado en cantidad de unidades fijas |
| 3 | `MONTO` | Monto fijo expresado directamente en pesos |

### 3.5 Tabla `fal_tarifario_unidad_faltas`

Cuadro tarifario versionado por vigencia. Mientras un monto no esté confirmado, congelado, manualizado o materializado en comprobante, el cálculo puede tomar el último tarifario vigente.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Tarifario | PK técnica |
| `tipo_unidad` | `SMALLINT` | NO | IDX | Tipo de unidad | `SALARIO`, `UNIDAD_FIJA` o `MONTO` |
| `valor_unidad` | `DECIMAL(14,2)` | NO | — | Valor monetario | Valor vigente por unidad o monto fijo de referencia |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Vigencia tarifaria |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `si_activa` | `BOOLEAN` | NO | IDX | Activo | Histórico no se borra |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

### 3.6 Tabla `fal_medida_preventiva`

Catálogo de medidas preventivas disponibles.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Medida preventiva | PK técnica |
| `codigo` | `VARCHAR(12)` | NO | IDX | Código funcional | Código corto, estable y legible |
| `version_medida` | `SMALLINT` | NO | IDX | Versión medida | Histórico |
| `descripcion` | `VARCHAR(160)` | NO | IDX | Descripción | Visible |
| `descripcion_detalle` | `VARCHAR(255)` | SÍ | — | Detalle opcional | Complemento |
| `id_dep` | `BIGINT` | SÍ | FK+IDX | Dependencia propietaria | Si aplica |
| `ver_dep` | `SMALLINT` | SÍ | — | Versión dependencia | Si aplica |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | No borrar si usada |
| `si_puede_bloquear_cierre` | `BOOLEAN` | NO | IDX | Puede generar bloqueante | No crea bloqueo por sí sola |
| `tipo_bloqueante_default` | `SMALLINT` | SÍ | IDX | Bloqueante default | Si aplica |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Índices/reglas:

- `UNIQUE KEY uk_fal_medida_preventiva_codigo_version (codigo, version_medida)`
- No usar `UNIQUE(codigo)` si se conservan versiones históricas.
- Solo una versión activa por código debe controlarse con regla transaccional y/o columna generada auxiliar, según el patrón de unicidad vigente definido en implementación.

### 3.7 Tabla `fal_articulo_medida_preventiva`

Define qué medidas puede habilitar cada artículo.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `articulo_id` | `BIGINT` | NO | PK+FK | Artículo | Artículo que habilita |
| `medida_preventiva_id` | `BIGINT` | NO | PK+FK | Medida | Medida habilitada |
| `si_obligatoria` | `BOOLEAN` | NO | IDX | Obligatoria | Si false, opcional/fundada |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | No borrar si usada |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

### 3.8 Tabla `fal_acta_articulo_infringido`

Instancia concreta de artículos imputados al acta. Es el detalle real de la infracción, pero no fija necesariamente el monto monetario.

Esta tabla representa la imputación del artículo al acta y su vigencia operativa.  
No registra montos, valorizaciones, documentos ni fallos asociados a la baja/corrección del artículo.

| Campo | Tipo | Null | Clave | Descripción | Observaciones |
|---|---:|:---:|---|---|---|
| `id` | `BIGINT` | NO | PK | Identificador técnico | AI |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta |  |
| `normativa_id` | `BIGINT` | NO | FK+IDX | Normativa imputada | Snapshot técnico de la normativa imputada |
| `articulo_id` | `BIGINT` | NO | FK+IDX | Artículo imputado |  |
| `si_activo` | `BOOLEAN` | NO | IDX | Indica si la imputación está vigente | Baja lógica, no borrado físico |
| `motivo_baja` | `SMALLINT` | SÍ | IDX | Motivo de baja/anulación/corrección | Catálogo cerrado |
| `fh_baja` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de baja lógica |  |
| `id_user_baja` | `CHAR(36)` | SÍ | IDX | Usuario que registró la baja |  |
| `fh_alta` | `DATETIME(6)` | NO | IDX | Fecha/hora de alta | Auditoría estándar |
| `id_user_alta` | `CHAR(36)` | NO | IDX | Usuario de alta | Auditoría estándar |

Reglas:

- Esta tabla registra qué artículo se imputa al acta.
- No guarda `valor_unidad`, `monto_base`, `monto_aplicado` ni snapshots tarifarios.
- No guarda `documento_baja_id` ni `fallo_id`, porque no se generará ni asociará, al menos en esta etapa, un documento formal o fallo específico por cada baja, corrección o anulación de artículo imputado.
- No guarda observaciones textuales.
- Si se necesita registrar una aclaración, fundamento o comentario sobre la imputación, baja, corrección o anulación del artículo, debe usarse `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = ARTICULO_INFRINGIDO`
  - `fal_observacion.entidad_id = fal_acta_articulo_infringido.id`
- El monto se calcula o instancia en `fal_acta_valorizacion` y `fal_acta_valorizacion_item`.
- Si cambia el cuadro tarifario antes de fijar, congelar o materializar el monto, el cálculo puede actualizarse al último tarifario vigente.
- La baja es lógica: no se elimina físicamente la imputación para conservar trazabilidad.
- Los documentos nuevos, snapshots operativos y valorizaciones nuevas deben considerar solo artículos con `si_activo = true`, salvo que se esté reconstruyendo historial.

### 3.9 Tabla `fal_acta_valorizacion`

Registra la valorización global del acta. El monto no es un `DECIMAL` suelto: es una decisión confirmable, actualizable, congelable, manualizable y trazable.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Valorización | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | 1 acta : N valorizaciones históricas |
| `estado_valorizacion` | `SMALLINT` | NO | IDX | Estado | PRELIMINAR, CONFIRMADA, REEMPLAZADA, ANULADA |
| `tipo_valorizacion_acta` | `SMALLINT` | NO | IDX | Tipo | INFRACCION_BASE, PAGO_VOLUNTARIO, CONDENA, AJUSTE_TOTAL |
| `origen_valorizacion` | `SMALLINT` | NO | IDX | Origen | Sistema, Dirección, juez adm., juez paz, apremio |
| `criterio_tarifario` | `SMALLINT` | NO | IDX | Criterio tarifario | ULTIMO_VIGENTE, MANTIENE_ANTERIOR, MANUAL |
| `tarifario_actualizado` | `BOOLEAN` | NO | IDX | Actualizó tarifario | Default true al confirmar/fallar salvo decisión contraria |
| `monto_base_articulos` | `DECIMAL(14,2)` | SÍ | — | Total automático | Suma base de artículos al momento de valorizar |
| `monto_final` | `DECIMAL(14,2)` | NO | — | Monto final | Valor vigente si confirmada/congelada/manualizada |
| `tipo_unidad_final` | `SMALLINT` | SÍ | IDX | Unidad final | `SALARIO`, `UNIDAD_FIJA` o `MONTO` |
| `cantidad_unidades_final` | `DECIMAL(14,4)` | SÍ | — | Cantidad unidades final | Si aplica |
| `valor_unidad_final` | `DECIMAL(14,2)` | SÍ | — | Valor unidad final | Snapshot al fijar/congelar |
| `tarifario_unidad_id` | `BIGINT` | SÍ | FK+IDX | Tarifario usado | Se informa cuando el monto queda fijado |
| `si_sobrescribe_total` | `BOOLEAN` | NO | IDX | Sobrescribe total | Si true, manda sobre suma automática |
| `si_congela_valor` | `BOOLEAN` | NO | IDX | Congela valor tarifario | Si true, no actualiza por cambios futuros del tarifario |
| `fh_congelamiento` | `DATETIME(6)` | SÍ | IDX | Fecha congelamiento | Si se congeló valor |
| `fallo_id` | `BIGINT` | SÍ | FK+IDX | Fallo relacionado | Si surge de fallo |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento/resolución | Fundamento formal |
| `fh_valorizacion` | `DATETIME(6)` | NO | IDX | Fecha valorización | Momento funcional |
| `id_user_valorizacion` | `CHAR(36)` | NO | IDX | Usuario/autoridad | Trazabilidad |
| `fh_confirmacion` | `DATETIME(6)` | SÍ | IDX | Fecha confirmación | Obligatoria si CONFIRMADA |
| `id_user_confirmacion` | `CHAR(36)` | SÍ | IDX | Usuario confirma | Trazabilidad |
| `si_vigente` | `BOOLEAN` | NO | IDX | Valorización vigente | Solo una vigente por acta/tipo aplicable |
| `acta_id_vigente` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN acta_id ELSE NULL END` |
| `tipo_valorizacion_vigente` | `SMALLINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN tipo_valorizacion_acta ELSE NULL END` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Catálogo `criterio_tarifario`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ULTIMO_VIGENTE` | Usa el último cuadro tarifario vigente al momento de confirmar/fallar |
| 2 | `MANTIENE_ANTERIOR` | Mantiene valor calculado con tarifario anterior |
| 3 | `MANUAL` | Autoridad fija cantidad, unidad o monto manualmente |

Catálogo `estado_valorizacion`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PRELIMINAR` | Calculada/cargada, no confirmada |
| 2 | `CONFIRMADA` | Monto fijado y operable |
| 3 | `REEMPLAZADA` | Reemplazada por otra valorización |
| 4 | `ANULADA` | Dejada sin efecto |

Catálogo `tipo_valorizacion_acta`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `INFRACCION_BASE` | Valor base de infracción |
| 2 | `PAGO_VOLUNTARIO` | Valor para pago voluntario |
| 3 | `CONDENA` | Valor para condena |
| 4 | `AJUSTE_ITEM` | Ajuste de uno o más ítems |
| 5 | `AJUSTE_TOTAL` | Total manual del acta |
| 6 | `MODIFICACION_APELACION` | Modificación por apelación |
| 7 | `GESTION_EXTERNA` | Modificación externa/apremio/juez paz |

Reglas de historial y vigencia:

- Las valorizaciones no se pisan.
- Cada recálculo, confirmación, congelamiento, manualización, reemplazo o anulación relevante genera una nueva fila en `fal_acta_valorizacion`.
- El detalle por artículo de cada valorización se registra en `fal_acta_valorizacion_item`.
- Las valorizaciones anteriores quedan históricas con `si_vigente = false` y estado `REEMPLAZADA` o `ANULADA`, según corresponda.
- La valorización operativa a usar no se determina solo por fecha, sino por estado y vigencia.
- Para operar deuda, portal, fallo, cierre o integración con Ingresos, se usa la valorización con:
  - `estado_valorizacion = CONFIRMADA`
  - `si_vigente = true`
- No puede existir más de una valorización vigente para la misma combinación `acta_id` + `tipo_valorizacion_acta`.
- Una valorización `PRELIMINAR` puede ser más nueva que una confirmada, pero no reemplaza la vigente hasta ser confirmada.
- Si cambia solo el monto o criterio económico del artículo, se crea una nueva valorización.
- Si cambia la imputación del artículo, se da de baja lógica el registro anterior en `fal_acta_articulo_infringido`, se crea un nuevo artículo imputado activo y luego se genera una nueva valorización.
### 3.10 Tabla `fal_acta_valorizacion_item`

Detalle de cómo una valorización confirmó, calculó o sobrescribió cada artículo imputado.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Ítem valorizado | PK técnica |
| `valorizacion_id` | `BIGINT` | NO | FK+IDX | Valorización madre | Pertenece a `fal_acta_valorizacion` |
| `acta_articulo_id` | `BIGINT` | SÍ | FK+IDX | Artículo imputado valorizado | Puede ser NULL si valorización es solo total manual |
| `tipo_valorizacion_item` | `SMALLINT` | NO | IDX | Tipo valorización ítem | Automática, pago voluntario, manual, fallo |
| `tipo_unidad_base` | `SMALLINT` | SÍ | IDX | Unidad base del artículo | Snapshot al momento de valorizar |
| `cantidad_unidades_base` | `DECIMAL(14,4)` | SÍ | — | Cantidad base del artículo | Snapshot al momento de valorizar |
| `tipo_unidad_aplicada` | `SMALLINT` | SÍ | IDX | Unidad aplicada | Puede diferir por autoridad |
| `cantidad_unidades_aplicada` | `DECIMAL(14,4)` | SÍ | — | Cantidad aplicada | Puede diferir por autoridad |
| `valor_unidad_aplicado` | `DECIMAL(14,2)` | SÍ | — | Valor unidad usado | Snapshot del tarifario al fijar monto |
| `monto_aplicado` | `DECIMAL(14,2)` | NO | — | Monto final ítem | Valor fijado para esta valorización |
| `tarifario_unidad_id` | `BIGINT` | SÍ | FK+IDX | Tarifario usado | Trazabilidad del cuadro aplicado |
| `si_manual` | `BOOLEAN` | NO | IDX | Ítem manualizado | Si true, requiere fundamento |
| `motivo_manual` | `SMALLINT` | SÍ | IDX | Motivo manualización | Catálogo funcional |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento fundamento | Si aplica |
| `fallo_id` | `BIGINT` | SÍ | FK+IDX | Fallo que determinó valor | Si aplica |

Reglas de actualización tarifaria:

- Mientras el monto no esté confirmado, congelado, manualizado o materializado, se calcula con el último tarifario vigente.
- Si cambia el cuadro tarifario, cambia el valor calculado/informado.
- En fallo condenatorio, el comportamiento por defecto es usar el último cuadro tarifario vigente.
- La autoridad puede optar por mantener valor anterior o fijar monto manual; esa decisión debe quedar registrada.
- Una vez emitida/materializada una obligación de pago en Ingresos, ese importe queda instanciado para ese comprobante.
- El portal muestra valor vivo actualizado solo mientras no haya confirmación, congelamiento, manualización o comprobante emitido.

---

## 4. Satélites del acta

### 4.1 Regla general

Los satélites guardan datos específicos que no corresponden al núcleo común de `fal_acta`.

Reglas:

- Un satélite puede existir porque el `tipo_acta` lo exige.
- Un satélite puede existir porque durante el labrado se realizó una práctica o se registró un dato específico.
- `ALCOHOLEMIA` no es tipo de acta; es satélite eventual de `TRANSITO`.

### 4.2 Tabla `fal_acta_transito`

Registra datos específicos de actas de tránsito. El municipio/departamento emisor de licencia se modela como unidad territorial genérica para alinear con la API de domicilios y contemplar el caso CABA.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta de tránsito | 1:1 cuando `tipo_acta=TRANSITO` |
| `nro_licencia` | `VARCHAR(20)` | SÍ | IDX | Número licencia | No limitar a `CHAR(8)` |
| `id_prov_lic` | `SMALLINT` | SÍ | IDX | Provincia emisora | ID de `geo_ign_provincia.id` |
| `unidad_territorial_lic_tipo` | `SMALLINT` | SÍ | IDX | Tipo unidad emisora | Municipio, departamento o CABA |
| `id_unidad_territorial_lic` | `INT` | SÍ | IDX | Unidad emisora | ID municipio/departamento/CABA desde catálogos MariaDB |
| `si_ret_licencia` | `BOOLEAN` | NO | IDX | Retención licencia | Puede generar bloqueante documentación |
| `si_ret_vehiculo` | `BOOLEAN` | NO | IDX | Retención/secuestro vehículo | Puede generar bloqueante rodado |
| `si_control_alcoholemia` | `BOOLEAN` | NO | IDX | Hubo alcoholemia | Si true, debe existir satélite alcoholemia |

Reglas:

- `unidad_territorial_lic_tipo` usa catálogo `unidad_territorial_tipo`.
- Si la provincia emisora es CABA, usar `id_prov_lic = 2`, `unidad_territorial_lic_tipo = CIUDAD_AUTONOMA` e `id_unidad_territorial_lic = 2`.
- Si la licencia fue emitida por Malvinas Argentinas, usar `id_prov_lic = 6`, `unidad_territorial_lic_tipo = MUNICIPIO` e `id_unidad_territorial_lic = 60515`.
- Para provincia/unidad emisora de licencia no se persisten `ver_*`, porque los catálogos geográficos IGN/INDEC/BAHRA en MariaDB no exponen versión funcional referenciable por Faltas.
- Los textos de provincia/unidad emisora para bandejas se proyectan en `fal_acta_snapshot`; no son fuente primaria en esta tabla.

### 4.3 Tabla `fal_acta_transito_alcoholemia`

Registra datos de prueba de alcoholemia vinculada al acta. No es tipo de acta.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Medición/prueba | PK técnica |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta tránsito | Puede haber N mediciones |
| `orden_medicion` | `SMALLINT` | NO | IDX | Orden medición | 1..N |
| `tipo_prueba` | `SMALLINT` | NO | IDX | Tipo prueba | Alómetro/alcoholímetro |
| `resultado_cualitativo` | `SMALLINT` | SÍ | IDX | Resultado cualitativo | Negativo, positivo, inválido, no realizado |
| `resultado_numerico` | `DECIMAL(4,2)` | SÍ | — | Resultado numérico | Aplica si alcoholímetro |
| `unidad_medida` | `SMALLINT` | SÍ | IDX | Unidad de medida del resultado numérico | Catálogo cerrado. Obligatoria si se informa `resultado_numerico` |
| `id_alcoholimetro` | `BIGINT` | SÍ | FK+IDX | Equipo utilizado | Si hay catálogo de equipos |
| `ver_alcoholimetro` | `SMALLINT` | SÍ | — | Versión equipo | Obligatoria si hay equipo |
| `si_resultado_final` | `BOOLEAN` | NO | IDX | Es resultado final | Solo una final por acta |
| `acta_id_resultado_final` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_resultado_final = true THEN acta_id ELSE NULL END` |
| `fh_medicion` | `DATETIME(6)` | SÍ | IDX | Fecha medición | Si el equipo/origen informa |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Catálogo `unidad_medida_alcoholemia`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `G_L` | Gramos de alcohol por litro de sangre |
| 2 | `MG_L_AIRE` | Miligramos de alcohol por litro de aire espirado |

Reglas:
- `unidad_medida` no es texto libre: usa catálogo cerrado.
- Si se informa `resultado_numerico`, debe informarse `unidad_medida`.
- Si `resultado_cualitativo` es `NEGATIVO`, `POSITIVO` o `INVALIDO` sin medición numérica, `resultado_numerico` y `unidad_medida` pueden quedar NULL.
- Para alcoholemia numérica, la unidad inicial esperada es `G_L`.
### 4.4 Tabla `fal_acta_vehiculo`

Registra los datos del vehículo vinculado al acta.

Por ahora se modela un vehículo por acta.  
La tabla no pretende replicar toda la cédula verde; solo conserva datos útiles para identificación operativa del vehículo dentro del acta.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta | 1 vehículo por acta por ahora |
| `dominio_vehiculo` | `VARCHAR(10)` | SÍ | IDX | Dominio/patente | Dato visible |
| `tipo_vehiculo` | `SMALLINT` | SÍ | IDX | Tipo de vehículo | Catálogo funcional cerrado |
| `marca_vehiculo_id` | `BIGINT` | SÍ | FK+IDX | Marca normalizada | FK a `fal_vehiculo_marca`, si existe |
| `marca_vehiculo_txt` | `VARCHAR(24)` | SÍ | IDX | Marca textual | Se usa si la marca no existe en catálogo |
| `modelo_vehiculo_id` | `BIGINT` | SÍ | FK+IDX | Modelo normalizado | FK a `fal_vehiculo_modelo`, si existe |
| `modelo_vehiculo_txt` | `VARCHAR(24)` | SÍ | — | Modelo textual | Se usa si el modelo no existe en catálogo |
| `anio_vehiculo` | `SMALLINT` | SÍ | IDX | Año del vehículo | Opcional |
| `color_vehiculo` | `VARCHAR(24)` | SÍ | IDX | Color del vehículo | Opcional |
| `estado_general_vehiculo` | `SMALLINT` | SÍ | IDX | Estado general visible del vehículo | Catálogo funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- `tipo_vehiculo` no es texto libre: usa catálogo funcional cerrado.
- `estado_general_vehiculo` no es texto libre: usa catálogo funcional cerrado.
- `marca_vehiculo_id` y `modelo_vehiculo_id` se usan cuando la marca/modelo existen en tablas normalizadas.
- Si se informa `modelo_vehiculo_id`, debe corresponder a la misma marca informada en `marca_vehiculo_id`.
- No puede seleccionarse un modelo perteneciente a otra marca.
- Si la marca existe pero el modelo no, se informa:
  - `marca_vehiculo_id`
  - `modelo_vehiculo_txt`
- Si la marca no existe en catálogo, se informa:
  - `marca_vehiculo_txt`
  - `modelo_vehiculo_txt`, si se conoce
- No se bloquea el labrado del acta por falta de marca/modelo en catálogo.
- `marca_vehiculo_txt` y `modelo_vehiculo_txt` son datos descriptivos de captura, no catálogos.
- `anio_vehiculo`, `color_vehiculo` y `estado_general_vehiculo` son opcionales.
- Si más adelante se decide permitir más de un vehículo por acta, esta tabla deberá pasar a tener PK propia `id BIGINT AUTO_INCREMENT` y `acta_id` como FK+IDX, en lugar de usar `acta_id` como PK.

#### Catálogo `tipo_vehiculo`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `AUTO` | Automóvil |
| 2 | `MOTO` | Motocicleta |
| 3 | `CAMIONETA` | Camioneta |
| 4 | `CAMION` | Camión |
| 5 | `COLECTIVO` | Colectivo |
| 6 | `UTILITARIO` | Utilitario |
| 7 | `ACOPLADO` | Acoplado / remolque |
| 8 | `BICICLETA` | Bicicleta |
| 9 | `MAQUINARIA` | Maquinaria |
| 10 | `OTRO` | Otro tipo de vehículo |

#### Catálogo `estado_general_vehiculo`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `BUENO` | Buen estado general visible |
| 2 | `REGULAR` | Estado general regular |
| 3 | `MALO` | Mal estado general visible |
| 4 | `SIN_VERIFICAR` | No verificado |
| 5 | `NO_APLICA` | No aplica |

#### Tabla `fal_vehiculo_marca`

Catálogo normalizado de marcas de vehículo.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Marca | PK técnica |
| `codigo` | `VARCHAR(12)` | NO | UK | Código estable | Catálogo |
| `nombre` | `VARCHAR(24)` | NO | IDX | Nombre de marca | Ej. FIAT, FORD, TOYOTA |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | Solo marcas activas se ofrecen para carga |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Índices/reglas:

- `UNIQUE KEY uk_fal_vehiculo_marca_codigo (codigo)`
- `UNIQUE KEY uk_fal_vehiculo_marca_nombre (nombre)`

#### Tabla `fal_vehiculo_modelo`

Catálogo normalizado de modelos disponibles por marca.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Modelo | PK técnica |
| `marca_vehiculo_id` | `BIGINT` | NO | FK+IDX | Marca | FK a `fal_vehiculo_marca` |
| `codigo` | `VARCHAR(12)` | NO | IDX | Código estable del modelo | Único dentro de la marca |
| `nombre` | `VARCHAR(24)` | NO | IDX | Nombre de modelo | Ej. GOL, FIESTA, COROLLA |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | Solo modelos activos se ofrecen para carga |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Índices/reglas:

- `UNIQUE KEY uk_fal_vehiculo_modelo_marca_codigo (marca_vehiculo_id, codigo)`
- `UNIQUE KEY uk_fal_vehiculo_modelo_marca_nombre (marca_vehiculo_id, nombre)`

Reglas de marca/modelo:

- `fal_vehiculo_marca` y `fal_vehiculo_modelo` son catálogos de ayuda para normalizar carga.
- `fal_vehiculo_modelo` depende de `fal_vehiculo_marca`.
- Una marca puede tener muchos modelos.
- Un modelo pertenece a una única marca.
- Al seleccionar una marca, la UI/API debe ofrecer solo modelos activos de esa marca.
- No se bloquea el labrado del acta si la marca o el modelo no existen en catálogo.
- Si el valor existe en catálogo, se guarda el ID.
- Si no existe, se guarda el texto capturado en `marca_vehiculo_txt` o `modelo_vehiculo_txt`.
- Más adelante, un proceso de normalización puede vincular textos libres a marcas/modelos del catálogo sin alterar el dato histórico capturado.

Consulta esperada para modelos disponibles por marca:

```sql
SELECT id, nombre
FROM fal_vehiculo_modelo
WHERE marca_vehiculo_id = ?
  AND si_activo = true
ORDER BY nombre;
```
### 4.5 Tabla `fal_acta_contravencion`

Registra datos específicos de actas de contravención/comercio vinculadas a una ubicación física identificada por nomenclatura catastral.

La nomenclatura se instancia en el acta como dato histórico, porque en el futuro puede cambiar en el maestro de Catastro.  
Por eso, aunque la fuente sea Catastro, mapa interactivo, cuenta CVP o cuenta de comercio, los datos estructurados de nomenclatura quedan copiados en esta tabla al momento del labrado. El texto resumido de nomenclatura no se guarda como fuente primaria en esta tabla; se proyecta en `fal_acta_snapshot.nomenclatura_resumen`.

Si no se puede resolver cuenta inmueble/CVP (`Id_Bie_i`) por Catastro, mapa, integración o cuenta municipal, se permite carga manual/excepcional. En ese caso `Id_Suj_i` y `Id_Bie_i` pueden quedar NULL y debe quedar marcada la causa de carga manual.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta | 1:1 si tipo contravención/comercio |
| `Id_Suj_i` | `BIGINT` | SÍ | IDX | Sujeto cuenta inmueble | Valor esperado `1` si existe cuenta inmueble/CVP |
| `Id_Bie_i` | `BIGINT` | SÍ | IDX | Bien/cuenta inmueble | NULL si no hay cuenta inmueble disponible o la carga es manual/excepcional |
| `Id_Suj_c` | `BIGINT` | SÍ | IDX | Sujeto cuenta comercio | Valor esperado `2` si existe comercio registrado |
| `Id_Bie_c` | `BIGINT` | SÍ | IDX | Bien/cuenta comercio | Cuenta municipal de comercio asociada a la nomenclatura, si existe |
| `circ` | `SMALLINT` | SÍ | IDX | Circunscripción | Nomenclatura instanciada |
| `secc` | `CHAR(2)` | SÍ | IDX | Sección | Nomenclatura instanciada |
| `frac` | `CHAR(7)` | SÍ | IDX | Fracción | Nomenclatura instanciada |
| `mza` | `CHAR(7)` | SÍ | IDX | Manzana | Nomenclatura instanciada |
| `parc` | `CHAR(7)` | SÍ | IDX | Parcela | Nomenclatura instanciada |
| `ufun` | `CHAR(7)` | SÍ | IDX | Unidad funcional | Nomenclatura instanciada |
| `ucomp` | `VARCHAR(20)` | SÍ | IDX | Unidad complementaria | Nomenclatura / unidad complementaria asociada |
| `origen_nomencl` | `SMALLINT` | NO | IDX | Origen de la nomenclatura | Catálogo funcional |
| `si_nomenclatura_manual` | `BOOLEAN` | NO | IDX | Nomenclatura informada manualmente/excepcional | Default `false` |
| `motivo_nomenclatura_manual` | `SMALLINT` | SÍ | IDX | Motivo de carga manual/excepcional | Obligatorio si `si_nomenclatura_manual = true` |
| `rubro_id` | `BIGINT` | SÍ | FK+IDX | Versión del rubro usada en el acta | FK a `fal_rubro_version.rubro_id`; congela el rubro vigente al momento del labrado |
| `Id_Rub` | `INT` | SÍ | IDX | Rubro externo de Ingresos | Copia de `informix.rubrocom.id_rub`; no es clave técnica interna |
| `ambito_ctv` | `SMALLINT` | SÍ | IDX | Ámbito de contravención/comercio | Catálogo funcional cerrado |
| `ambito_ctv_txt` | `VARCHAR(80)` | SÍ | — | Ámbito textual controlado | Obligatorio solo si `ambito_ctv = OTRO` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- Si la nomenclatura se obtiene desde Catastro, mapa interactivo, cuenta CVP/inmueble o integración, `Id_Suj_i` y `Id_Bie_i` deben informarse.
- `Id_Suj_i` tiene valor esperado `1` cuando existe cuenta inmueble/CVP.
- `Id_Bie_i` puede quedar NULL únicamente cuando no hay cuenta inmueble disponible o cuando la carga es manual/excepcional.
- Si `Id_Bie_i` queda NULL, debe cumplirse:
  - `si_nomenclatura_manual = true`;
  - `motivo_nomenclatura_manual` obligatorio;
  - `origen_nomencl = MANUAL_EXCEPCIONAL` o motivo funcional equivalente.
- `Id_Suj_c` y `Id_Bie_c` identifican la cuenta municipal de comercio, si existe.
- `Id_Suj_c` tiene valor esperado `2` cuando existe comercio registrado.
- `Id_Bie_c` es opcional porque puede existir una contravención en una ubicación física donde el comercio no esté registrado.
- Si se informa `Id_Bie_c`, debe informarse `Id_Suj_c`.
- Si se informa `Id_Suj_c`, debe informarse `Id_Bie_c`.
- La nomenclatura se obtiene normalmente desde maestro de Catastro, mapa interactivo, cuenta CVP/inmueble o cuenta de comercio.
- Los campos `circ`, `secc`, `frac`, `mza`, `parc`, `ufun` y `ucomp` son una instancia histórica de la nomenclatura al momento del acta.
- No se actualizan automáticamente si Catastro cambia después.
- La carga manual de nomenclatura no es el flujo normal.
- Solo se permite nomenclatura manual/excepcional cuando no hay datos disponibles o no se pudo resolver desde Catastro/integración.
- Si `si_nomenclatura_manual = true`, debe informarse `motivo_nomenclatura_manual`.
- Si `si_nomenclatura_manual = false`, la nomenclatura debe provenir de una fuente controlada.
- `nomenclatura_txt` no se guarda en `fal_acta_contravencion`; el resumen legible se proyecta en `fal_acta_snapshot.nomenclatura_resumen`.
- El rubro proviene de Ingresos/Informix; Faltas no administra el maestro primario de rubros.
- `rubro_id` referencia `fal_rubro_version.rubro_id`, es decir, la versión exacta usada al labrar.
- `Id_Rub` conserva la referencia externa original `informix.rubrocom.id_rub`.
- Si el `Id_Rub` informado por Ingresos no existe en `vw_fal_rubro_actual`, debe sincronizarse antes de permitir seleccionarlo como rubro normalizado.
- Si el rubro se selecciona desde catálogo, deben informarse `rubro_id` e `Id_Rub`.
- Si por contingencia no se puede resolver rubro contra catálogo, el acta puede continuar solo si el flujo funcional lo permite y debe conservar observación/causa en `fal_observacion`.
- Si Ingresos cambia la descripción o deshabilita el rubro luego del labrado, el acta conserva su `rubro_id` histórico.
- `ambito_ctv` no es texto libre: usa catálogo funcional cerrado.
- Si `ambito_ctv = OTRO`, `ambito_ctv_txt` es obligatorio.
- Si `ambito_ctv <> OTRO`, `ambito_ctv_txt` debe quedar NULL.
- Las observaciones textuales sobre la contravención, nomenclatura o carga excepcional no van en esta tabla; deben registrarse en `fal_observacion` con `entidad_tipo = ACTA` o el tipo específico que corresponda.

#### Catálogo `origen_nomencl`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `CATASTRO` | Nomenclatura obtenida desde maestro de Catastro |
| 2 | `MAPA_INTERACTIVO` | Nomenclatura obtenida por selección en mapa |
| 3 | `CUENTA_INMUEBLE` | Nomenclatura obtenida por cuenta municipal de inmueble/CVP |
| 4 | `CUENTA_COMERCIO` | Nomenclatura obtenida por cuenta municipal de comercio |
| 5 | `INTEGRACION_EXTERNA` | Nomenclatura obtenida por integración externa |
| 6 | `MANUAL_EXCEPCIONAL` | Nomenclatura cargada manualmente por falta de datos disponibles |

#### Catálogo `motivo_nomenclatura_manual`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `SIN_DATOS_CATASTRO` | No hay datos disponibles en Catastro |
| 2 | `NO_RESUELVE_MAPA` | El mapa no pudo resolver la nomenclatura |
| 3 | `NO_RESUELVE_CUENTA` | No se pudo resolver desde cuenta inmueble/comercio |
| 4 | `INTEGRACION_NO_DISPONIBLE` | La integración no estaba disponible al momento del labrado |
| 5 | `CONTINGENCIA_OPERATIVA` | Carga excepcional por contingencia operativa |
| 6 | `OTRO` | Otro motivo justificado |

#### Catálogo `ambito_ctv`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `BALDIO` | Baldío |
| 2 | `COMERCIO` | Comercio |
| 3 | `INDUSTRIA` | Industria |
| 4 | `VIVIENDA` | Vivienda |
| 5 | `LOCAL` | Local |
| 6 | `OTRO` | Otro ámbito no contemplado en el catálogo |

Reglas de `ambito_ctv`:

- `ambito_ctv` no es texto libre: usa catálogo funcional cerrado.
- `ambito_ctv_txt` es texto controlado para el caso excepcional `OTRO`.
- Si aparece un nuevo ámbito real y estable, se incorpora explícitamente al catálogo.
- No usar `ambito_ctv_txt` para evitar ampliar el catálogo cuando el valor ya se volvió funcionalmente estable.

### 4.6 Tabla `fal_acta_sustancias_alimenticias`

Registra datos específicos de actas vinculadas a sustancias alimenticias, mercadería, condiciones sanitarias visibles o actividad relacionada.

Esta tabla es satélite de `fal_acta`.  
Existe cuando el acta corresponde al circuito de sustancias alimenticias/bromatología.

El rubro funcional no nace en Faltas: proviene del sistema de Ingresos.  
Faltas mantiene un catálogo interno/cache de rubros externos usados, para operar localmente, buscar, mostrar e historizar sin depender en cada consulta del sistema externo.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta | 1:1 para tipo sustancias alimenticias |
| `rubro_id` | `BIGINT` | SÍ | FK+IDX | Versión del rubro usada en el acta | FK a `fal_rubro_version.rubro_id`; congela el rubro vigente al momento del labrado |
| `Id_Rub` | `INT` | SÍ | IDX | Rubro externo de Ingresos | Copia de `informix.rubrocom.id_rub`; no es clave técnica interna |
| `ambito_ctv` | `SMALLINT` | SÍ | IDX | Ámbito de la actividad/contravención | Catálogo funcional cerrado compartido |
| `ambito_ctv_txt` | `VARCHAR(80)` | SÍ | — | Ámbito textual controlado | Obligatorio solo si `ambito_ctv = OTRO` |
| `descripcion_sustancias` | `TEXT` | SÍ | — | Descripción de sustancias alimenticias | Descripción libre de sustancias, mercadería, estado, condiciones visibles, lotes o datos relevantes |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- `fal_acta_sustancias_alimenticias` existe como satélite cuando el acta pertenece al circuito de sustancias alimenticias/bromatología.
- El rubro proviene del sistema de Ingresos/Informix.
- Faltas no administra el maestro primario de rubros.
- `rubro_id` referencia `fal_rubro_version.rubro_id`, no una tabla de rubro actual.
- `Id_Rub` conserva la referencia original al rubro externo de Ingresos.
- Cuando se necesita usar un rubro, se consulta `vw_fal_rubro_actual`.
- Si el `Id_Rub` informado por Ingresos no existe en `vw_fal_rubro_actual`, debe sincronizarse antes de usarlo como rubro normalizado.
- Si se informa `rubro_id`, debe corresponder a una versión vigente o históricamente válida de `fal_rubro_version`.
- Si se informa `Id_Rub`, debe corresponder con `fal_rubro_version.Id_Rub` para el `rubro_id` informado.
- Si cambia la descripción del rubro en Ingresos, no se altera el dato histórico ya emitido, firmado o formalizado.
- Los documentos generados/firmados conservan su propio snapshot formal.
- `ambito_ctv` usa el mismo catálogo funcional definido para contravención/comercio.
- Los valores iniciales de `ambito_ctv` son `BALDIO`, `COMERCIO`, `INDUSTRIA`, `VIVIENDA`, `LOCAL` y `OTRO`.
- `ambito_ctv_txt` solo se informa si `ambito_ctv = OTRO`.
- Si `ambito_ctv = OTRO`, `ambito_ctv_txt` es obligatorio.
- Si `ambito_ctv <> OTRO`, `ambito_ctv_txt` debe quedar NULL.
- `descripcion_sustancias` puede ser `TEXT` porque describe mercadería, sustancias, estado visible, condiciones sanitarias, lotes u otros detalles relevantes que no entran bien en campos estructurados.
- Las observaciones administrativas, aclaraciones internas o comentarios no propios de la descripción sanitaria/material deben registrarse en `fal_observacion`, no en esta tabla.
- Para observaciones sobre esta entidad, usar `fal_observacion` con el `entidad_tipo` que corresponda según el caso funcional, por ejemplo `ACTA` o un tipo específico si se incorpora para este satélite.
- Si cambia la descripción del rubro en Ingresos, no se debe alterar silenciosamente el dato histórico ya emitido, firmado o formalizado.
- Los documentos generados/firmados conservan su propio snapshot formal.
- `fal_rubro_version` se sincroniza desde Ingresos/Informix; el acta conserva el `rubro_id` de la versión usada al momento del labrado.

#### Catálogo `ambito_ctv`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `BALDIO` | Baldío |
| 2 | `COMERCIO` | Comercio |
| 3 | `INDUSTRIA` | Industria |
| 4 | `VIVIENDA` | Vivienda |
| 5 | `LOCAL` | Local |
| 6 | `OTRO` | Otro ámbito no contemplado en el catálogo |

Reglas de `ambito_ctv`:

- `ambito_ctv` no es texto libre: usa catálogo funcional cerrado.
- `ambito_ctv_txt` es texto controlado para el caso excepcional `OTRO`.
- Si aparece un nuevo ámbito real y estable, se incorpora explícitamente al catálogo.
- No usar `ambito_ctv_txt` para evitar ampliar el catálogo cuando el valor ya se volvió funcionalmente estable.

#### Tabla `fal_rubro_version`

Catálogo versionado de rubros comerciales utilizados por Faltas, sincronizado desde el sistema de Ingresos/Informix.

Fuente Informix:

- Tabla fuente: `informix.rubrocom`
- Campos usados:
  - `id_rub`
  - `deno`
  - `sidesabilitado`

Campos no usados por Faltas en este modelo:

- `codigoactividad`
- `categoria`
- `atributos`
- `v1_*`
- `v2_*`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `rubro_id` | `BIGINT AUTO_INCREMENT` | NO | PK | Versión interna del rubro | PK técnica versionada de Faltas |
| `Id_Rub` | `INT` | NO | IDX | Rubro externo de Ingresos | Viene de `informix.rubrocom.id_rub`; no es PK técnica |
| `nombre` | `VARCHAR(120)` | NO | IDX | Descripción del rubro | Viene de `informix.rubrocom.deno` |
| `nombre_norm` | `VARCHAR(120)` | SÍ | IDX | Nombre normalizado | Para búsqueda/autocomplete |
| `sidesabilitado` | `SMALLINT` | NO | IDX | Estado crudo de Ingresos | Viene de `informix.rubrocom.sidesabilitado` |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo derivado | `true` si `sidesabilitado = 0`; `false` si `sidesabilitado <> 0` |
| `row_hash` | `VARCHAR(128)` | NO | IDX | Hash de contenido | Detecta cambios funcionales |
| `previous_row_hash` | `VARCHAR(128)` | SÍ | — | Hash anterior | Para auditoría técnica |
| `source_operation` | `VARCHAR(30)` | NO | IDX | Operación de sincronización | `INSERT`, `UPDATE`, `UNCHANGED`, `DEACTIVATE` según implementación del CLI |
| `close_operation` | `VARCHAR(30)` | SÍ | IDX | Motivo cierre versión | Se informa si se cierra una versión |
| `si_version_actual` | `BOOLEAN` | NO | IDX | Versión vigente | Una versión vigente por `Id_Rub` |
| `valid_from` | `DATETIME(6)` | NO | IDX | Inicio vigencia | Momento de sincronización que creó la versión |
| `valid_to` | `DATETIME(6)` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `synced_at` | `DATETIME(6)` | NO | IDX | Fecha sync | Auditoría técnica |

Reglas de `fal_rubro_version`:

- `fal_rubro_version` no es el maestro primario de rubros.
- La fuente primaria es `informix.rubrocom` / sistema de Ingresos.
- `Id_Rub` mantiene el identificador externo real de Ingresos.
- `rubro_id` es la PK técnica interna de Faltas para una versión exacta.
- No existe `fal_rubro` como tabla actual no versionada en este diseño.
- La vista `vw_fal_rubro_actual` expone únicamente las versiones vigentes y activas para uso operativo.
- Las actas guardan `rubro_id` apuntando a `fal_rubro_version.rubro_id`.
- Las actas también guardan `Id_Rub` como copia externa para trazabilidad e integración.
- Si Ingresos modifica `deno` o `sidesabilitado`, el sync cierra la versión anterior y crea una nueva.
- Si un rubro se deshabilita en Ingresos, no se borran versiones ni actas históricas.
- La sincronización no debe modificar silenciosamente documentos ya emitidos, firmados o notificados.
- Si se necesita preservar literalmente la descripción usada en una pieza formal, esa descripción debe quedar en el documento/snapshot correspondiente.

Índices sugeridos:

- `PRIMARY KEY (rubro_id)`
- `KEY ix_fal_rubro_version_id_rub (Id_Rub)`
- `KEY ix_fal_rubro_version_actual (si_version_actual)`
- `KEY ix_fal_rubro_version_id_rub_actual (Id_Rub, si_version_actual)`
- `KEY ix_fal_rubro_version_nombre (nombre)`
- `KEY ix_fal_rubro_version_nombre_norm (nombre_norm)`
- `KEY ix_fal_rubro_version_activo (si_activo)`

Recomendación de integridad adicional:

Para asegurar una sola versión vigente por `Id_Rub`, agregar columna generada nullable e índice único:

```sql
ALTER TABLE fal_rubro_version
ADD COLUMN actual_Id_Rub INT
    GENERATED ALWAYS AS (
        CASE WHEN si_version_actual = 1 THEN Id_Rub ELSE NULL END
    ) STORED,
ADD UNIQUE KEY uk_fal_rubro_version_actual_id_rub (actual_Id_Rub);
```

Vista actual:

```sql
CREATE OR REPLACE VIEW vw_fal_rubro_actual AS
SELECT
    rubro_id,
    Id_Rub,
    nombre,
    nombre_norm,
    sidesabilitado,
    si_activo,
    row_hash,
    source_operation,
    valid_from,
    synced_at
FROM fal_rubro_version
WHERE si_version_actual = 1
  AND si_activo = 1
  AND valid_to IS NULL;
```

---

### 4.7 Tabla `fal_acta_medida_preventiva`

Registra medida preventiva efectivamente aplicada al acta. No es catálogo.

La relación con bloqueantes materiales es unidireccional para evitar ciclo de FK: si una medida genera un bloqueante, el bloqueante apunta a la medida aplicada mediante `fal_acta_bloqueante_cierre_material.medida_preventiva_acta_id`.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Medida aplicada | PK técnica |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | 1 acta : N medidas |
| `acta_articulo_id` | `BIGINT` | NO | FK+IDX | Artículo que habilitó | Debe vincularse al artículo infringido que habilita la medida |
| `medida_preventiva_id` | `BIGINT` | NO | FK+IDX | Medida catálogo | Medida aplicada |
| `med_prev_txt` | `VARCHAR(255)` | SÍ | — | Texto complementario | No reemplaza catálogo |
| `estado_medida` | `SMALLINT` | NO | IDX | Estado medida | Aplicada, levantada, anulada, cumplida |
| `si_genera_bloqueante` | `BOOLEAN` | NO | IDX | Generó bloqueante | Si true, debe existir bloqueante material asociado |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- `acta_articulo_id` es obligatorio porque la medida aplicada debe poder trazarse al artículo imputado que la habilitó.
- No se guarda `bloqueante_id` en esta tabla para evitar relación cíclica.
- Si `si_genera_bloqueante = true`, debe existir una fila en `fal_acta_bloqueante_cierre_material` con `medida_preventiva_acta_id = fal_acta_medida_preventiva.id`.
- Si una medida se levanta, anula o cumple, se actualiza `estado_medida` y, si generó bloqueante, se resuelve el bloqueante material correspondiente.
- Observaciones o fundamentos adicionales van a `fal_observacion` con `entidad_tipo = MEDIDA_PREVENTIVA`.

### 4.8 Tabla `fal_acta_bloqueante_cierre_material`

Registra pendientes materiales que impiden cierre.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Bloqueante | PK técnica |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | 1 acta : N bloqueantes |
| `origen_bloqueante` | `SMALLINT` | NO | IDX | Origen | MEDIDA_PREVENTIVA, RODADO, DOCUMENTACION_RETENIDA, OTRO |
| `medida_preventiva_acta_id` | `BIGINT` | SÍ | FK+IDX | Medida aplicada | Obligatorio si `origen_bloqueante = MEDIDA_PREVENTIVA` |
| `estado_cumplimiento_material` | `SMALLINT` | NO | IDX | Estado | Pendiente, cumplido, anulado |
| `fh_cumplimiento` | `DATETIME(6)` | SÍ | IDX | Fecha cumplimiento | Si se resolvió |
| `id_user_cumplimiento` | `CHAR(36)` | SÍ | IDX | Usuario cumplimiento | Trazabilidad |
| `si_activo` | `BOOLEAN` | NO | IDX | Bloqueante activo | Deriva flags snapshot |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- Si `origen_bloqueante = MEDIDA_PREVENTIVA`, `medida_preventiva_acta_id` es obligatorio.
- Si `origen_bloqueante <> MEDIDA_PREVENTIVA`, `medida_preventiva_acta_id` debe quedar NULL.
- La relación entre medida preventiva y bloqueante es unidireccional desde bloqueante hacia medida aplicada.
- No se permite FK cruzada inversa desde `fal_acta_medida_preventiva` hacia esta tabla.
- `si_activo = true` representa un bloqueo material vigente que impide cierre.
- Al cumplir, anular o resolver el pendiente material, se completa `fh_cumplimiento`, `id_user_cumplimiento`, se actualiza `estado_cumplimiento_material` y `si_activo = false`.
- Observaciones sobre el bloqueo van a `fal_observacion` con `entidad_tipo = BLOQUEANTE_CIERRE_MATERIAL`.

## 5. Documentos y firma

### 5.1 Tabla `fal_documento`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Documento | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `tipo_docu` | `SMALLINT` | NO | IDX | Tipo documento | Catálogo tipo_docu |
| `estado_docu` | `SMALLINT` | NO | IDX | Estado documento | Catálogo estado_docu |
| `nro_docu` | `VARCHAR(30)` | SÍ | IDX | Número documento | Numeración por talonario si aplica |
| `id_talonario` | `BIGINT` | SÍ | FK+IDX | Talonario | Talonario efectivamente usado al numerar |
| `nro_talonario_usado` | `INT` | SÍ | IDX | Número usado | Trazabilidad |
| `tipo_firma_req` | `SMALLINT` | NO | IDX | Requerimiento de firma | Catálogo tipo_firma_req. Snapshot al generar. |
| `plantilla_id` | `BIGINT` | SÍ | FK+IDX | Plantilla documental | Obligatorio para docs generados por sistema |
| `storage_key` | `VARCHAR(255)` | SÍ | IDX | Archivo storage | No guardar binarios |
| `hash_docu` | `VARCHAR(128)` | SÍ | IDX | Hash integridad | Control |
| `fh_generacion` | `DATETIME(6)` | SÍ | IDX | Fecha generación | Funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Nota: `rol_firma_req` y `mecanismo_firma_req` que existían en versiones previas fueron reemplazados por `fal_documento_firma_req` (Sección 5.4). Los repositorios JDBC deben crear la tabla sin estos campos.

Reglas de `fal_documento`:

- `tipo_firma_req` es el resumen funcional del requerimiento de firma del documento.
- El detalle de cada requisito de firma vive en `fal_documento_firma_req`.
- El mecanismo real usado se registra en `fal_documento_firma.tipo_firma`.
- Todo documento firmable (`tipo_firma_req != NO_REQUIERE`) debe tener al menos un registro activo en `fal_documento_firma_req`.
- `plantilla_id` es obligatorio para documentos generados por sistema; nullable para documentos externos adjuntos o migrados.
- `fal_documento` guarda snapshot de `tipo_docu` y `tipo_firma_req` al momento de creación.
- Los requisitos detallados se copian de la plantilla a `fal_documento_firma_req` al generar el documento.
- Cambios posteriores en la plantilla no alteran documentos ya generados.
- `fal_documento_firma_req` es la fuente definitiva de requisitos de firma para la bandeja.
- `estado_docu` NO es lo mismo que `estado_firma`. Ver catálogos.

Catálogo `tipo_docu`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ACTA_INFRACCION` | Documento principal |
| 2 | `NOTIFICACION_ACTA` | Notificación de acta |
| 3 | `MEDIDA_PREVENTIVA` | Documento medida preventiva |
| 4 | `ACTO_ADMINISTRATIVO` | Fallo/resolución |
| 5 | `NOTIFICACION_ACTO_ADMINISTRATIVO` | Notificación de fallo/acto |
| 6 | `CONSTANCIA` | Constancia |
| 7 | `ANEXO` | Anexo |
| 8 | `NULIDAD` | Nulidad |
| 9 | `RESOLUTORIO_BLOQUEANTE` | Resolutorio material |
| 10 | `INTIMACION_PAGO` | Intimación de pago |
| 11 | `INTIMACION_INCUMPLIMIENTO_PLAN` | Intimación por incumplimiento de plan |
| 12 | `OTRO` | Otro controlado |

Catálogo `estado_docu`:

| ID | Código | Descripción |
|---:|---|---|
| 1 | `BORRADOR` | Documento en preparación, sin efecto administrativo formal. |
| 2 | `EMITIDO` | Documento generado/emitido formalmente. |
| 3 | `PENDIENTE_FIRMA` | Documento emitido que requiere firma y todavía no completó sus requisitos. |
| 4 | `FIRMADO` | Documento con firmas requeridas completadas; pieza válida. |
| 5 | `ADJUNTO` | Documento externo incorporado al expediente. |
| 6 | `ANULADO` | Documento anulado administrativamente. |
| 7 | `REEMPLAZADO` | Documento reemplazado por otra pieza documental posterior. |

Catálogo `tipo_firma_req`:

| ID | Código | Descripción |
|---:|---|---|
| 0 | `NO_REQUIERE` | El documento no requiere firma. |
| 1 | `FIRMA_INTERNA` | Requiere firma interna (operador habilitado). |
| 2 | `FIRMA_INSPECTOR` | Requiere firma del inspector como requisito formal documental. |
| 3 | `FIRMA_AUTORIDAD` | Requiere firma de autoridad competente. |
| 4 | `FIRMA_DIGITAL` | Requiere firma digital. |
| 5 | `FIRMA_MULTIPLE` | Requiere más de un requisito activo de firma. |

Reglas de `tipo_firma_req`:
- `FIRMA_MULTIPLE` exige al menos dos requisitos activos en `fal_documento_plantilla_firma_req`..
- La firma ólógrafa del infractor NO participa de `tipo_firma_req`..
- La firma automática del inspector en acta NO equivale a `FIRMA_INSPECTOR` documental.

Catálogo `rol_firma_req`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `INSPECTOR` | Firma del inspector como requisito documental formal |
| 2 | `AUTORIDAD` | Firma de autoridad competente |
| 3 | `AUTORIDAD_COMPETENTE` | Autoridad competente con cargo específico |
| 4 | `OPERADOR_INTERNO` | Firma de operador municipal habilitado |
| 5 | `SISTEMA` | Sello/firma automática de sistema |
| 6 | `OTRO` | Otro rol controlado |

Catálogo `mecanismo_firma_req`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `DIGITAL` | Debe usarse firma digital |
| 2 | `ELECTRONICA` | Puede usarse firma electrónica |
| 3 | `OLOGRAFA` | Firma manuscrita/escaneada |
| 4 | `SISTEMA` | Sello o generación automática del sistema |
| 5 | `MIXTA` | Admite más de un mecanismo según el firmante |

### 5.2 Tabla `fal_acta_documento`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `acta_id` | `BIGINT` | NO | PK+FK | Acta | Relación acta-documento |
| `documento_id` | `BIGINT` | NO | PK+FK | Documento | Documento asociado |
| `rol_docu_acta` | `SMALLINT` | NO | IDX | Rol funcional | ACTA_PRINCIPAL, FALLO, NOTIFICACION, etc. |
| `si_principal` | `BOOLEAN` | NO | IDX | Principal | Según rol/regla |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

### 5.3 Tabla `fal_documento_firma`

Registra las firmas asociadas a un documento emitido por el sistema.

La tabla conserva los datos mínimos necesarios para saber qué documento se firmó, quién lo firmó, con qué mecanismo, con qué rol/claim funcional, cuándo se firmó y, si aplica, qué referencia externa respalda la operación.

`tipo_firma` representa el mecanismo o naturaleza de la firma.  
No representa el rol del firmante.

El rol del firmante se conserva como snapshot del claim recibido en el bearer/token del usuario que firma, porque ese rol puede cambiar en el futuro, pero la firma debe conservar con qué autorización funcional fue realizada.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Firma | PK técnica |
| `documento_id` | `BIGINT` | NO | FK+IDX | Documento | Documento firmado |
| `id_firmante` | `BIGINT` | S? | FK+IDX | Firmante que ejecut? la firma | FK a `fal_firmante`; nullable si firma es de sistema |
| `ver_firmante` | `SMALLINT` | S? | FK+IDX | Versi?n del firmante usada | FK a `fal_firmante_version`; debe informarse si hay `id_firmante` |
| `seq_firma_req` | `SMALLINT` | SÃ | FK+IDX | Requisito de firma satisfecho | Junto con `documento_id`, referencia `fal_documento_firma_req`; NULL para firmas sin requisito explÃ­cito |
| `tipo_firma` | `SMALLINT` | NO | IDX | Tipo/mecanismo de firma | Catálogo funcional |
| `estado_firma` | `SMALLINT` | NO | IDX | Estado firma | Catálogo funcional |
| `id_user_firma` | `CHAR(36)` | SÍ | IDX | Usuario firmante | Subject/IDP del usuario, si aplica |
| `rol_firmante` | `VARCHAR(40)` | SÍ | IDX | Rol/claim del firmante | Snapshot tomado del bearer/token |
| `nombre_firmante` | `VARCHAR(64)` | SÍ | — | Nombre visible del firmante | Snapshot tomado del token/IDP o perfil al momento de firma |
| `fh_firma` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de firma | Obligatoria si `estado_firma = FIRMADA` |
| `hash_documento` | `CHAR(64)` | SÍ | IDX | Hash del documento firmado | SHA-256 hexadecimal |
| `referencia_firma_ext` | `VARCHAR(120)` | SÍ | IDX | Referencia externa de firma | Solo si la firma tiene respaldo/operación externa |
| `mensaje_error` | `VARCHAR(255)` | SÍ | — | Mensaje de error | Si falló la firma |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas:

- Puede haber más de una firma por documento si el circuito lo requiere.
- `tipo_firma` representa el mecanismo de firma, no el rol del firmante.
- `rol_firmante` no se selecciona manualmente en la firma.
- `rol_firmante` se toma del bearer/token del usuario que firma.
- `rol_firmante` se guarda como snapshot histórico del claim usado al momento de firmar.
- El rol guardado permite reconstruir con qué autorización funcional se firmó el documento.
- Si el usuario cambia de rol posteriormente, no se modifica `rol_firmante` en firmas ya registradas.
- Si firma un usuario real, debería informarse `id_user_firma`, `rol_firmante` y `nombre_firmante`.
- Si `tipo_firma = SISTEMA`, `rol_firmante` puede tomar valor `SISTEMA` o el claim técnico equivalente usado por el proceso automático.
- `nombre_firmante` es un snapshot legible para visualización y control. No reemplaza al usuario/subject.
- `hash_documento` corresponde al contenido exacto firmado.
- Si el documento cambia, no se reutiliza la firma anterior.
- Para esta etapa, el hash se asume `SHA-256` y se guarda como hexadecimal en `CHAR(64)`.
- No se guardan certificados, binarios ni metadata pesada de firma en esta tabla.
- Si más adelante se requiere validación criptográfica avanzada, se podrá agregar una tabla específica de evidencia técnica de firma.
- Si `estado_firma = FIRMADA`, debe informarse `fh_firma`.
- Si `estado_firma = ERROR`, debería informarse `mensaje_error`.
- Las observaciones administrativas o comentarios sobre la firma no van en esta tabla; deben registrarse en `fal_observacion`.
- `id_firmante` y `ver_firmante` vinculan la firma al registro del firmante habilitado.

- `seq_firma_req` vincula la firma al requisito especÃ­fico de `fal_documento_firma_req` que fue satisfecho. NULL si la firma no corresponde a un requisito explÃ­cito. Son NULL si el firmante no est? en el padr?n (p. ej. firma de sistema).
- Aunque exista FK al firmante versionado, los snapshots `id_user_firma`, `rol_firmante` y `nombre_firmante` deben conservarse para garantizar trazabilidad hist?rica ante cambios posteriores.

#### Catálogo `tipo_firma`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `DIGITAL` | Firma digital con mecanismo criptográfico/certificado |
| 2 | `ELECTRONICA` | Firma electrónica sin certificado digital formal |
| 3 | `OLOGRAFA` | Firma manuscrita/ológrafa sobre soporte físico o digitalizado |
| 4 | `SISTEMA` | Firma, sello o generación automática del sistema |

Reglas de `tipo_firma`:

- `DIGITAL` puede corresponder a un inspector, funcionario, autoridad u otro usuario autorizado.
- `ELECTRONICA` también puede corresponder a distintos roles funcionales.
- `OLOGRAFA` representa firma manuscrita o física, eventualmente digitalizada.
- `SISTEMA` representa sello, firma técnica o emisión automática del sistema.
- El rol funcional no se deduce de `tipo_firma`; se toma de `rol_firmante`.

#### Catálogo `estado_firma`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PENDIENTE` | Firma pendiente |
| 2 | `SOLICITADA` | Firma solicitada |
| 3 | `FIRMADA` | Firma realizada correctamente |
| 4 | `RECHAZADA` | Firma rechazada |
| 5 | `ANULADA` | Firma anulada o dejada sin efecto |
| 6 | `ERROR` | Error técnico durante el proceso de firma |

#### Uso de `referencia_firma_ext`

`referencia_firma_ext` no representa la firma en sí.  
Es una referencia opcional a una operación, comprobante o identificador externo asociado al acto de firma.

Se informa solo cuando existe un sistema, proveedor, expediente, remito, soporte físico/digitalizado u operación externa que conviene poder rastrear desde Faltas.

Ejemplos:

- Firma digital/electrónica realizada por proveedor externo:
  - ID de transacción del proveedor.
  - UUID de operación externa.
  - ID de sobre/documento firmado.
  - Código de solicitud de firma.

- Firma ológrafa:
  - Número de remito de recepción del documento firmado físicamente.
  - Identificador del archivo digitalizado.
  - Referencia al lote de escaneo.
  - Código de archivo físico.

- Firma en otro sistema municipal:
  - ID de expediente.
  - ID de acto administrativo.
  - Código de trámite/documento externo.
  - Referencia de integración.

Reglas de `referencia_firma_ext`:

- Es NULL para firmas internas simples sin respaldo externo.
- No reemplaza a `hash_documento`.
- No reemplaza a `documento_id`.
- No debe contener JSON ni metadata extensa.
- Debe contener solo una referencia corta y trazable.
- Si se requiere guardar evidencia pesada, binarios, acuses o respuestas completas de proveedor, eso debe ir en storage externo o en una tabla específica futura de evidencia técnica.

Decisiones tomadas:

- La tabla queda chica y operativa.
- `tipo_firma` queda como mecanismo/naturaleza de firma.
- Se elimina la idea de usar `tipo_firma` para representar inspector, autoridad o funcionario.
- `rol_firmante` se conserva como snapshot del claim del bearer/token al momento de firmar.
- No se define catálogo interno de roles de firma en esta tabla.
- `nombre_firmante` queda en `VARCHAR(64)`.
- Se conserva `hash_documento` como control mínimo de integridad.
- Se conserva `referencia_firma_ext` para integraciones externas o respaldo físico/digitalizado.
- No se guardan certificados, binarios ni metadata pesada.

#### RelaciÃ³n funcional: firmante habilitado y firma de documento



Ver Secciones 5.4 (`fal_documento_firma_req`), 5.5 (Regla de bandeja de firma) y 5.6 (Regla de firma efectiva) para el flujo completo.



Resumen:



1. Cada documento firmable tiene uno o mÃ¡s requisitos definidos en `fal_documento_firma_req`.

2. La bandeja de firma se genera cruzando requisitos pendientes con firmantes vigentes y habilitaciones compatibles en `fal_firmante_version_habilitacion`.

3. Al firmar, se valida firmante, habilitaciÃ³n, asignaciÃ³n especÃ­fica y orden de firma.

4. Se crea `fal_documento_firma` con snapshot completo (`id_firmante`, `ver_firmante`, `id_user_firma`, `rol_firmante`, `nombre_firmante`, `tipo_firma`, `hash_documento`).

5. Se actualiza `fal_documento_firma_req` con `estado_firma_req = FIRMADO`, `fh_firma` e `id_firma`.

6. Los snapshots de `fal_documento_firma` no se reescriben por cambios posteriores en el firmante.



---



### 5.4 Tabla `fal_documento_firma_req`

Define qué puede firmar una versión concreta de firmante. Es la fuente de autorización documental por firmante.
Define qué firmas necesita un documento concreto. Es la fuente para la bandeja de firma.

Permite múltiples requisitos de firma por documento, con asignación específica de firmante, orden de firma y estado de cada requisito.

| Campo | Tipo MariaDB | Null | Clave/Índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id_docu` | `BIGINT` | NO | PK+FK | Documento | FK a `fal_documento` |
| `seq_firma_req` | `SMALLINT` | NO | PK | Secuencia requisito | Incremental por documento; inicia en 1 |
| `rol_firma_req` | `SMALLINT` | NO | IDX | Rol requerido | Debe ser satisfecho por habilitación de firmante |
| `mecanismo_firma_req` | `SMALLINT` | SÍ | IDX | Mecanismo requerido | NULL si acepta cualquier mecanismo compatible |
| `orden_firma` | `SMALLINT` | SÍ | IDX | Orden de firma | Para flujos secuenciales; NULL si no hay orden |
| `si_obligatoria` | `BOOLEAN` | NO | IDX | Firma obligatoria | Necesaria para completar el documento |
| `estado_firma_req` | `SMALLINT` | NO | IDX | Estado del requisito | Catálogo `estado_firma_req` |
| `id_firmante_asig` | `BIGINT` | SÍ | FK+IDX | Firmante asignado | NULL si puede firmar cualquier firmante compatible |
| `ver_firmante_asig` | `SMALLINT` | SÍ | FK+IDX | Versión asignada | Obligatoria si hay `id_firmante_asig` |
| `fh_asignacion` | `DATETIME(6)` | SÍ | - | Fecha asignación | Si se asignó firmante concreto |
| `fh_firma` | `DATETIME(6)` | SÍ | - | Fecha firma | Se completa al firmar |
| `id_firma` | `BIGINT` | SÍ | FK+IDX | Firma que satisfizo el requisito | FK a `fal_documento_firma`; se completa al firmar |
| `si_activo` | `BOOLEAN` | NO | IDX | Requisito activo | Baja lógica |
| `fh_alta` | `DATETIME(6)` | NO | - | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | - | Usuario alta | Auditoría |

Reglas:

- Un documento puede tener N requisitos de firma.
- Un requisito puede estar asignado a una versión específica de firmante.
- Si `id_firmante_asig` está informado, `ver_firmante_asig` es obligatorio.
- Si el requisito tiene firmante asignado, solo esa versión puede satisfacerlo.
- Si no tiene firmante asignado, cualquier firmante vigente con habilitación compatible puede verlo en bandeja.
- La compatibilidad se evalúa contra `fal_documento.tipo_docu`, `fal_documento_firma_req.rol_firma_req`, `fal_documento_firma_req.mecanismo_firma_req` (si aplica) y `fal_firmante_version_habilitacion`.
- `orden_firma` permite flujos secuenciales. Un requisito posterior no entra en bandeja hasta que los obligatorios de menor orden estén `FIRMADO`.
- Al firmar, se crea `fal_documento_firma` y se actualiza este registro con `fh_firma` e `id_firma`.
- `estado_firma_req` controla el ciclo de vida del requisito.
- No borrar requisitos históricos; usar baja lógica (`si_activo = false`).
- No guardar binarios ni contenido firmado en esta tabla.
- Todo documento firmable (`requisito_firma != NO_REQUIERE`) debe tener al menos un registro activo en `fal_documento_firma_req`.

#### Catálogo `estado_firma_req`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PENDIENTE` | Requisito pendiente de firma |
| 2 | `FIRMADO` | Requisito satisfecho por una firma histórica |
| 3 | `ANULADO` | Requisito anulado administrativamente |
| 4 | `VENCIDO` | Requisito vencido si aplica vencimiento |
| 5 | `REEMPLAZADO` | Requisito reemplazado por otro requerimiento |

Reglas de `estado_firma_req`

- Solo `PENDIENTE` entra en bandeja de firma.
- `FIRMADO` debe tener `fh_firma` e `id_firma` informados.
- `ANULADO` debe tener trazabilidad en `fal_acta_evento` o `fal_observacion`.
- `REEMPLAZADO` se usa si se crea otro requisito que lo sustituye.
- No usar strings libres para estados.

### 5.5 Regla de bandeja de firma

Un documento aparece en la bandeja de un firmante cuando se cumplen todas estas condiciones:

1. El documento está en estado documental pendiente de firma (`estado_docu` compatible con pendiente de firma).
2. Existe `fal_documento_firma_req` activo (`si_activo = true`).
3. `estado_firma_req = PENDIENTE`.
4. El requisito no está bloqueado por `orden_firma`: todos los requisitos obligatorios (`si_obligatoria = true`) de menor `orden_firma` están `FIRMADO`.
5. El usuario autenticado tiene `fal_firmante.si_activo = true`.
6. Existe `fal_firmante_version` vigente/activa para la fecha actual (`fh_vig_desde <= hoy` y (`fh_vig_hasta IS NULL OR fh_vig_hasta >= hoy`) y `si_activo = true`).
7. Existe `fal_firmante_version_habilitacion` activa (`si_activo = true`) compatible con:
   - `fal_documento.tipo_docu`
   - `fal_documento_firma_req.rol_firma_req`
   - `fal_documento_firma_req.mecanismo_firma_req`, si no es NULL.
8. Si el requisito tiene `id_firmante_asig`/`ver_firmante_asig`, debe coincidir exactamente con el firmante evaluado.
9. Si no tiene firmante asignado, cualquier firmante compatible puede verlo en bandeja.
10. Si el requisito está `FIRMADO`, `ANULADO`, `VENCIDO` o `REEMPLAZADO`, no entra en bandeja.

### 5.6 Regla de firma efectiva

Al firmar un requisito de firma:

1. Se resuelve el documento (`fal_documento.id`).
2. Se resuelve el requisito de firma pendiente (`fal_documento_firma_req` por `id_docu` + `seq_firma_req`).
3. Se valida que el firmante esté vigente (`fal_firmante.si_activo = true`, versión activa y vigente).
4. Se valida que la habilitación sea compatible (`fal_firmante_version_habilitacion` activa y compatible).
5. Se valida asignación específica si existe (`id_firmante_asig`/`ver_firmante_asig`).
6. Se valida orden de firma si aplica (`orden_firma`).
7. Se valida hash del documento (el contenido debe coincidir con el que se generó).
8. Se crea registro en `fal_documento_firma` con snapshot:
   - `documento_id`, `seq_firma_req`
   - `id_firmante`, `ver_firmante`
   - `id_user_firma` (snapshot del subject/IDP)
   - `rol_firmante` (snapshot del claim/bearer al momento de firmar)
   - `nombre_firmante` (snapshot legible)
   - `tipo_firma` (mecanismo usado)
   - `estado_firma = FIRMADA`
   - `fh_firma`
   - `hash_documento`
9. Se actualiza `fal_documento_firma_req`:
   - `estado_firma_req = FIRMADO`
   - `fh_firma`
   - `id_firma` (FK a la firma recién creada)
10. Se evalúa si todos los requisitos obligatorios (`si_obligatoria = true`) están `FIRMADO`.
11. Si todos están firmados, el documento puede avanzar de estado.
12. Si el documento firmado cambia (nuevo `hash_docu`), debe invalidarse el flujo; no reutilizar firma previa.
13. No modificar firmas históricas por cambios posteriores de firmante, cargo, rol, dependencia o habilitación.

---

### 5.7 Tabla `fal_documento_plantilla`

Define las plantillas/modelos documentales base que la aplicación puede generar. No es el documento concreto. Es configuración administrativa/documental central.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador | PK técnica |
| `codigo` | `VARCHAR(50)` | NO | UNIQUE | Código plantilla | Estable, no reutilizable |
| `nombre` | `VARCHAR(120)` | NO | — | Nombre visible | Para UI/backoffice |
| `descripcion` | `VARCHAR(255)` | SÍ | — | Descripción | Opcional |
| `tipo_docu` | `SMALLINT` | NO | IDX | Tipo documental producido | Catálogo tipo_docu |
| `accion_documental` | `SMALLINT` | NO | IDX | Acción principal que usa la plantilla | Catálogo accion_documental |
| `tipo_acta` | `SMALLINT` | SÍ | IDX | Tipo de acta aplicable | NULL si transversal |
| `tipo_firma_req` | `SMALLINT` | NO | IDX | Requerimiento general de firma | Catálogo tipo_firma_req |
| `si_requiere_numeracion` | `BOOLEAN` | NO | IDX | Requiere nro_docu | Si true, se numera según momento |
| `momento_numeracion_docu` | `SMALLINT` | NO | IDX | Momento de numeración | Catálogo momento_numeracion_docu |
| `si_notificable` | `BOOLEAN` | NO | IDX | Puede generar notificación | No implica que ya esté notificado |
| `si_genera_pdf` | `BOOLEAN` | NO | — | Genera pieza PDF | Para storage/documento |
| `si_seleccionable` | `BOOLEAN` | NO | IDX | Usuario puede elegirla si hay varias | Si false puede ser interna/default |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | Solo activas aplican |
| `fh_vig_desde` | `DATE` | NO | IDX | Vigencia desde | Inicio |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Vigencia hasta | NULL vigente |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas de `fal_documento_plantilla`:

- `codigo` obligatorio y único.
- `tipo_docu`, `accion_documental`, `tipo_firma_req` obligatorios.
- `si_requiere_numeracion = false` exige `momento_numeracion_docu = NO_APLICA`..
- `si_requiere_numeracion = true` exige `momento_numeracion_docu` distinto de `NO_APLICA`..
- La plantilla NO define `id_talonario`, `politica_numeracion_id` ni `clase_talonario`..
- La resolución de talonario documental vive en `num_talonario_ambito`..
- Si `tipo_firma_req = FIRMA_MULTIPLE`, debe tener al menos dos requisitos activos obligatorios.
- Si `tipo_firma_req = NO_REQUIERE`, no debe tener requisitos activos obligatorios.
- `tipo_acta` permite restringir plantillas a TRANSITO/COMERCIO/etc. si corresponde.

### 5.8 Tabla `fal_documento_plantilla_firma_req`

Define los requisitos base de firma de una plantilla. Al generar un documento concreto, se copian como snapshot a `fal_documento_firma_req`..

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Identificador | PK técnica |
| `plantilla_id` | `BIGINT` | NO | FK+IDX | Plantilla | fal_documento_plantilla.id |
| `seq_firma_req` | `SMALLINT` | NO | IDX | Secuencia de firma | Orden/etapa |
| `rol_firma_req` | `SMALLINT` | NO | IDX | Rol requerido | Catálogo rol_firma_req |
| `mecanismo_firma_req` | `SMALLINT` | SÍ | IDX | Mecanismo requerido | Catálogo mecanismo_firma_req |
| `si_obligatoria` | `BOOLEAN` | NO | IDX | Firma obligatoria | Si false, firma opcional/informativa |
| `si_activa` | `BOOLEAN` | NO | IDX | Requisito activo | Solo activos se materializan |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas de `fal_documento_plantilla_firma_req`:

- Si `plantilla.tipo_firma_req = NO_REQUIERE`, no debe tener requisitos activos obligatorios.
- Si `plantilla.tipo_firma_req != NO_REQUIERE`, debe tener al menos un requisito activo obligatorio.
- Si `plantilla.tipo_firma_req = FIRMA_MULTIPLE`, debe tener al menos dos requisitos activos obligatorios.
- Al generar documento, los requisitos activos se copian a `fal_documento_firma_req`..
- Cambios futuros en plantilla no alteran documentos ya generados.

#### Catálogo `accion_documental`

| ID | Código | Uso |
|---:|---|---|
| 1 | `GENERAR_ACTA_INFRACCION` | Generar pieza documental de acta de infracción si corresponde. |
| 2 | `EMITIR_NOTIFICACION_ACTA` | Emitir notificación del acta. |
| 3 | `EMITIR_MEDIDA_PREVENTIVA` | Emitir documento de medida preventiva. |
| 4 | `EMITIR_FALLO` | Emitir fallo / acto administrativo resolutivo. |
| 5 | `EMITIR_NOTIFICACION_FALLO` | Emitir notificación del fallo/acto administrativo. |
| 6 | `EMITIR_NULIDAD` | Emitir documento/acto de nulidad. |
| 7 | `EMITIR_CONSTANCIA` | Emitir constancia administrativa. |
| 8 | `EMITIR_ANEXO` | Emitir anexo documental. |
| 9 | `EMITIR_INTIMACION_PAGO` | Emitir intimación de pago. |
| 10 | `EMITIR_INTIMACION_INCUMPLIMIENTO_PLAN` | Emitir intimación por incumplimiento de plan. |
| 11 | `EMITIR_RESOLUTORIO_BLOQUEANTE` | Emitir resolutorio asociado a bloqueante material. |

#### Catálogo `momento_numeracion_docu`

| ID | Código | Uso |
|---:|---|---|
| 0 | `NO_APLICA` | Documento no numerado. |
| 1 | `AL_CREAR` | Se numera al crear el documento. |
| 2 | `AL_EMITIR` | Se numera al emitir formalmente el documento. |
| 3 | `AL_ENVIAR_A_FIRMA` | Se numera al pasar a pendiente de firma / crear requisitos efectivos. |
| 4 | `AL_FIRMAR` | Se numera al completar firma requerida. |

Reglas de `momento_numeracion_docu`:
- Este catálogo aplica solo a documentos, NO a actas.
- Las actas usan `nro_acta` y NO usan `momento_numeracion_docu`..
- Para fallos/actos administrativos, la regla base recomendada es `AL_ENVIAR_A_FIRMA`..
- Si `si_requiere_numeracion = false`, debe ser `NO_APLICA`..

### 5.9 Reglas de numeración documental

1. La numeración documental aplica a `fal_documento`..
2. Los documentos usan `clase_talonario = DOCUMENTO`..
3. La plantilla documental define si requiere numeración y en qué momento.
4. La plantilla documental NO define `id_talonario`, `politica_numeracion_id` ni `clase_talonario`..
5. El talonario documental concreto se resuelve al momento de numerar usando `num_talonario_ambito`..
6. `num_talonario_ambito` resuelve por clase `DOCUMENTO`, `tipo_docu`, `tipo_acta` si aplica, `id_dep`/`ver_dep` si aplica, alcance, prioridad, vigencia y `si_activo`..
7. `fal_documento` guarda el resultado: `nro_docu`, `id_talonario`, `nro_talonario_usado`..
8. El usuario no elige ni modifica el talonario al emitir un documento.

Separación obligatoria entre numeración de actas y numeración documental:

- Las actas usan `fal_acta.nro_acta`, `id_talonario`, `nro_talonario_usado`.
- Las actas usan `clase_talonario = ACTA`..
- Las actas NO usan `momento_numeracion_docu`..
- Las actas NO usan `nro_docu`..
- No mezclar numeración de actas con numeración documental.

### 5.10 Reglas de firma en actas

**Firma automática del inspector:**

- El acta se atribuye al inspector autenticado/labrante.
- La firma del inspector en actas no se captura en cada acta.
- La firma del inspector no se almacena como imagen/muestra maestra. La atribución al inspector se hace por autenticación.
- Al labrar el acta, el sistema asume la autoría/firma del inspector autenticado.
- El PDF/representación del acta puede renderizar automáticamente la firma registrada.
- La firma automática del inspector en acta NO participa de `fal_documento_firma_req` ni de `fal_documento_firma`..
- `FIRMA_INSPECTOR` documental en `tipo_firma_req` solo aplica a documentos formales, no a actas.

**Firma ólógrafa del infractor:**

- Es opcional como captura gráfica.
- Se dibuja en la pantalla del dispositivo.
- No es una firma institucional.
- No participa de `tipo_firma_req`, `fal_documento_firma_req` ni `fal_documento_firma`..
- Lo obligatorio no es que el infractor firme, sino que el acta registre qué ocurrió con la firma.
- El resultado se registra en `fal_acta.resultado_firma_infractor`..
- Si firmó, la captura gráfica se guarda como evidencia tipo `FIRMA_OLOGRAFA_INFRACTOR`..



### 5.11 Tabla `fal_documento_plantilla_contenido`

Representa una versión específica e inmutable de la definición renderizable de una plantilla documental.

Todo el contenido de las plantillas se expresa exclusivamente en Markdown. No se admiten formatos alternativos de texto plano o HTML.

La plantilla puede contener variables mediante la sintaxis `{{namespace.campo}}` y referencias controladas a recursos mediante expresiones como `{{asset.ESCUDO_MUNICIPAL}}`.

La combinación de esta definición Markdown con las variables y recursos del contexto produce el contenido base de una redacción documental.

Incorporado en Slice 8F-10. Implementación InMemory inicial: Slice 8F-1.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Contenido de plantilla | PK técnica |
| `plantilla_id` | `BIGINT` | NO | FK+IDX | Plantilla lógica | FK a `fal_documento_plantilla.id` |
| `version_contenido` | `SMALLINT` | NO | UK | Versión ordinal | Incremental por plantilla |
| `titulo` | `VARCHAR(200)` | NO | — | Título de la versión | Referencia operativa y administrativa |
| `cuerpo_markdown` | `TEXT` | NO | — | Cuerpo principal | Definición Markdown con variables |
| `encabezado_markdown` | `TEXT` | SÍ | — | Encabezado opcional | Markdown; NULL si no existe encabezado separado |
| `pie_markdown` | `TEXT` | SÍ | — | Pie opcional | Markdown; NULL si no existe pie separado |
| `variables_declaradas_json` | `JSON` | NO | — | Variables declaradas | Arreglo JSON; usar `[]` cuando no haya variables |
| `si_activo` | `BOOLEAN` | NO | IDX | Estado lógico | Solo contenidos activos participan en resolución |
| `fh_vig_desde` | `DATETIME(6)` | NO | IDX | Inicio de vigencia | Inclusive |
| `fh_vig_hasta` | `DATETIME(6)` | SÍ | IDX | Fin de vigencia | NULL = vigencia indefinida |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario de alta | Auditoría |

índices sugeridos:

- `UNIQUE KEY ux_plt_cont_version (plantilla_id, version_contenido)`
- `KEY ix_plt_cont_activo_vigencia (plantilla_id, si_activo, fh_vig_desde, fh_vig_hasta)`
- `KEY ix_plt_cont_vigencia (fh_vig_desde, fh_vig_hasta)`

Reglas de `fal_documento_plantilla_contenido`:

- `version_contenido` es un ordinal incremental por `plantilla_id`.
- Todo el contenido se almacena exclusivamente como Markdown.
- Se eliminan el campo `formato` y el enum `FormatoPlantillaContenido`.
- El título es texto simple; el cuerpo, encabezado y pie son Markdown.
- Las variables utilizan exclusivamente la sintaxis segura `{{namespace.campo}}`.
- Las imágenes y demás recursos solo pueden referenciarse mediante assets controlados.
- No se admiten URLs externas arbitrarias, rutas físicas, `file://` ni imágenes Base64 embebidas en la plantilla.
- Una referencia de imagen puede expresarse, por ejemplo, como `![Escudo]({{asset.ESCUDO_MUNICIPAL}})`.
- El modelo de assets y su versionado se documentará separadamente antes del generador PDF real.
- `variables_declaradas_json` contiene un arreglo de variables detectadas o declaradas. Si no existen variables, debe almacenarse `[]`, no NULL.
- La estructura de cada variable puede contener `namespace`, `campo`, `descripcion`, `requerida` y `tipoDato`.
- Para una misma plantilla no debe existir más de una versión activa y vigente para el mismo instante.
- Si `fh_vig_hasta` está informada, debe ser posterior a `fh_vig_desde`.
- Una versión que ya fue utilizada por una redacción no debe modificarse.
- Cualquier cambio de texto, variables, encabezado o pie debe generar una nueva `version_contenido`.
- `si_activo = false` representa baja lógica y excluye el contenido de nuevas resoluciones, pero no afecta las redacciones históricas que ya lo referencian.

### 5.12 Tabla `fal_documento_plantilla_default`

Representa una regla de resolución automática de plantilla documental.

Determina qué plantilla lógica debe utilizarse para una acción documental, considerando opcionalmente el tipo de acta, la dependencia, la vigencia, la especificidad y la prioridad.

La regla apunta a `fal_documento_plantilla`, no a una versión concreta de contenido. Una vez resuelta la plantilla lógica, el servicio selecciona su versión de contenido Markdown activa y vigente.

Incorporado en Slice 8F-10. Implementación InMemory inicial: Slice 8F-1.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Regla default | PK técnica |
| `accion_documental` | `SMALLINT` | NO | IDX | Acción documental | Enum cerrado `AccionDocumental` |
| `tipo_acta` | `SMALLINT` | SÍ | IDX | Tipo de acta | NULL = regla genérica |
| `tipo_docu` | `SMALLINT` | NO | IDX | Tipo documental | Debe coincidir con la plantilla referenciada |
| `id_dependencia` | `BIGINT` | SÍ | FK+IDX | Dependencia | NULL = regla genérica |
| `ver_dependencia` | `SMALLINT` | SÍ | — | Versión de dependencia | Ambos campos de dependencia NULL o ambos informados |
| `plantilla_id` | `BIGINT` | NO | FK+IDX | Plantilla lógica resuelta | FK a `fal_documento_plantilla.id` |
| `prioridad` | `SMALLINT` | NO | IDX | Prioridad dentro del nivel | Rango permitido 0 a 32767 |
| `fh_vig_desde` | `DATETIME(6)` | NO | IDX | Inicio de vigencia | Inclusive |
| `fh_vig_hasta` | `DATETIME(6)` | SÍ | IDX | Fin de vigencia | NULL = vigencia indefinida |
| `si_activo` | `BOOLEAN` | NO | IDX | Estado lógico | Solo reglas activas participan |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario de alta | Auditoría |

índices sugeridos:

- `KEY ix_plt_def_resolucion (accion_documental, tipo_docu, tipo_acta, id_dependencia, si_activo)`
- `KEY ix_plt_def_prioridad (accion_documental, tipo_docu, prioridad)`
- `KEY ix_plt_def_vigencia (fh_vig_desde, fh_vig_hasta)`
- `KEY ix_plt_def_plantilla (plantilla_id)`

Algoritmo de resolución en `DocumentoPlantillaDefaultService`:

1. Filtrar por `si_activo = true`.
2. Filtrar por vigencia en la fecha efectiva de resolución.
3. Filtrar por `accion_documental` y `tipo_docu`.
4. Incluir reglas cuyo `tipo_acta` coincida con el buscado o sea NULL.
5. Incluir reglas cuya dependencia coincida con la buscada o sea NULL.
6. Calcular el nivel de especificidad:
   - Nivel 2: coinciden exactamente `tipo_acta` y dependencia.
   - Nivel 1: coincide exactamente una de las dos dimensiones y la otra es genérica.
   - Nivel 0: ambas dimensiones son genéricas.
7. Ordenar por nivel de especificidad descendente.
8. Dentro del mismo nivel, ordenar por `prioridad DESC`.
9. Tomar la regla mejor posicionada.
10. Si existen dos o más reglas con la misma especificidad y prioridad máxima, lanzar `PlantillaDefaultAmbiguaException`.
11. Si no existe ninguna regla aplicable, lanzar `PlantillaDefaultNoEncontradaException`.
12. Una vez resuelta la plantilla, seleccionar su única versión de contenido Markdown activa y vigente.
13. Si no existe una versión vigente o existen varias simultáneamente, rechazar la resolución por inconsistencia de configuración.

Reglas adicionales:

- Una regla específica no debe ser desplazada por una regla completamente genérica solo por tener una prioridad mayor.
- La prioridad se evalúa dentro del nivel de especificidad correspondiente.
- `prioridad` utiliza `SMALLINT`; la aplicación debe validar el rango de 0 a 32767.
- `id_dependencia` y `ver_dependencia` deben ser ambos NULL o ambos estar informados.
- `tipo_docu` debe coincidir con el tipo documental definido por `plantilla_id`.
- Si `fh_vig_hasta` está informada, debe ser posterior a `fh_vig_desde`.
- Dos reglas equivalentes en acción, tipo documental, tipo de acta y dependencia no deben solaparse temporalmente.
- `si_activo = false` representa baja lógica.
- La regla default no congela una versión de contenido.
- La versión concreta utilizada se congela al crear `fal_documento_redaccion`.

### 5.13 Tabla `fal_documento_redaccion`

Representa una revisión editable del contenido Markdown que será utilizado para generar un documento.

Se crea luego de resolver una plantilla y combinar su versión de contenido con las variables y recursos del contexto.

La redacción conserva: la versión exacta de plantilla utilizada; el Markdown base generado automáticamente; el Markdown editable y finalmente confirmado; el snapshot de variables utilizadas; las variables faltantes; el diagnóstico de combinación; el snapshot de imágenes y recursos; las regeneraciones, ediciones, confirmación y anulación.

Incorporado en Slice 8F-10. Implementación InMemory inicial: Slice 8F-1.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Redacción | PK técnica |
| `id_documento` | `BIGINT` | NO | FK+IDX | Documento | FK a `fal_documento.id` |
| `plantilla_contenido_id` | `BIGINT` | NO | FK+IDX | Versión de plantilla | Congela la versión Markdown utilizada |
| `nro_revision` | `SMALLINT` | NO | UK | Número de revisión | Incremental por documento |
| `redaccion_origen_id` | `BIGINT` | SÍ | FK+IDX | Redacción de origen | Self FK; usada cuando una revisión nace de otra |
| `estado_redaccion` | `SMALLINT` | NO | IDX | Estado | Enum cerrado |
| `contenido_base_markdown` | `TEXT` | NO | — | Markdown regenerado | Resultado directo de combinar plantilla y contexto |
| `contenido_editable_markdown` | `TEXT` | NO | — | Markdown editable/final | Inicialmente igual al contenido base |
| `variables_snapshot_json` | `JSON` | NO | — | Variables utilizadas | Usar `{}` cuando no haya variables |
| `variables_faltantes_json` | `JSON` | NO | — | Variables no resueltas | Usar `[]` cuando no haya faltantes |
| `diagnostico_json` | `JSON` | NO | — | Diagnóstico de combinación | Usar `{}` cuando no haya observaciones |
| `recursos_snapshot_json` | `JSON` | SÍ | — | Recursos utilizados | IDs, versiones, storage keys y hashes de assets |
| `fh_creacion` | `DATETIME(6)` | NO | IDX | Fecha de creación | Auditoría |
| `id_user_creacion` | `CHAR(36)` | NO | — | Usuario creador | Auditoría |
| `fh_ultima_regeneracion` | `DATETIME(6)` | SÍ | IDX | último refresco | NULL si nunca se refresció después de crear |
| `id_user_ultima_regeneracion` | `CHAR(36)` | SÍ | — | Usuario del refresco | Ambos campos de regeneración NULL o informados |
| `fh_ultima_edicion` | `DATETIME(6)` | SÍ | IDX | última edición manual | NULL si nunca se editó |
| `id_user_ultima_edicion` | `CHAR(36)` | SÍ | — | Usuario editor | Ambos campos de edición NULL o informados |
| `fh_confirmacion` | `DATETIME(6)` | SÍ | IDX | Confirmación | Obligatoria para CONFIRMADA |
| `id_user_confirmacion` | `CHAR(36)` | SÍ | — | Usuario confirmador | Obligatorio para CONFIRMADA |
| `fh_anulacion` | `DATETIME(6)` | SÍ | IDX | Anulación | Obligatoria para ANULADA |
| `id_user_anulacion` | `CHAR(36)` | SÍ | — | Usuario que anuló | Obligatorio para ANULADA |
| `motivo_anulacion` | `VARCHAR(500)` | SÍ | — | Motivo | Obligatorio para ANULADA |

índices sugeridos:

- `UNIQUE KEY ux_doc_redac_revision (id_documento, nro_revision)`
- `KEY ix_doc_redac_documento_estado (id_documento, estado_redaccion)`
- `KEY ix_doc_redac_plantilla_contenido (plantilla_contenido_id)`
- `KEY ix_doc_redac_origen (redaccion_origen_id)`
- `KEY ix_doc_redac_creacion (fh_creacion)`

Estados de redacción:

| Estado | Código | Descripción |
|---|---:|---|
| `BORRADOR` | 1 | Redacción editable y regenerable |
| `CONFIRMADA` | 2 | Contenido congelado y utilizado para generar el documento |
| `ANULADA` | 3 | Redacción descartada con trazabilidad |

Se elimina `REABIERTA` como estado mutable de una misma fila.

Una redacción confirmada no vuelve a estado editable. Cuando sea necesario rehacer una redacción confirmada, debe crearse una nueva revisión en estado BORRADOR, conservando la anterior sin modificaciones y vinculándola mediante `redaccion_origen_id`.

Transiciones permitidas: `BORRADOR — CONFIRMADA`; `BORRADOR — ANULADA`. No se permiten transiciones desde `CONFIRMADA` ni desde `ANULADA`.

Reglas de creación:

1. Resolver `fal_documento_plantilla_default`.
2. Resolver la versión activa y vigente de `fal_documento_plantilla_contenido`.
3. Construir el contexto actual del dominio.
4. Resolver las variables del Markdown.
5. Resolver los assets controlados.
6. Guardar el Markdown generado en `contenido_base_markdown`.
7. Copiar inicialmente ese valor en `contenido_editable_markdown`.
8. Guardar variables, faltantes, diagnóstico y recursos utilizados.
9. Crear la redacción en estado BORRADOR.

Reglas de edición manual:

- Solo una redacción BORRADOR puede editarse.
- La edición modifica exclusivamente `contenido_editable_markdown`.
- `contenido_base_markdown` conserva el resultado de la última generación automática.
- La edición registra `fh_ultima_edicion` e `id_user_ultima_edicion`.
- La edición no modifica el snapshot de variables.

Reglas de refresco o regeneración:

- Solo una redacción BORRADOR puede refrescarse.
- El refresco utiliza la misma `plantilla_contenido_id`.
- Se reconstruye el contexto utilizando los datos actuales del dominio.
- Se recalculan: `contenido_base_markdown`; `contenido_editable_markdown`; `variables_snapshot_json`; `variables_faltantes_json`; `diagnostico_json`; `recursos_snapshot_json`.
- El nuevo `contenido_editable_markdown` se inicializa nuevamente con el contenido base.
- Las ediciones manuales previas se reemplazan.
- La UI debe advertir expresamente que el refresco descartará las ediciones manuales.
- El refresco registra `fh_ultima_regeneracion` e `id_user_ultima_regeneracion`.
- Cambiar de versión de plantilla no es un refresco: requiere anular el borrador actual y crear una nueva revisión.

Reglas de confirmación:

- Solo una redacción BORRADOR puede confirmarse.
- Antes de confirmar deben validarse las variables obligatorias y los recursos requeridos.
- La confirmación congela: la versión de plantilla; el Markdown base; el Markdown editable final; las variables; los diagnósticos; los recursos utilizados.
- La generación del PDF utiliza exactamente `contenido_editable_markdown`.
- El PDF generado debe conservar en `fal_documento` su `storage_key`, `hash_docu` y `fh_generacion`.
- La redacción confirmada no puede sobrescribirse, refrescarse ni editarse.
- El contenido exacto llevado al PDF queda preservado en `contenido_editable_markdown`.
- Los datos utilizados quedan preservados en `variables_snapshot_json`.
- Los assets utilizados quedan preservados en `recursos_snapshot_json`.

Reglas de anulación:

- Solo una redacción BORRADOR puede anularse.
- La anulación exige fecha, usuario y motivo.
- Una redacción anulada permanece disponible para auditoría.
- Una redacción anulada no puede reactivarse.

Corrección posterior a la confirmación:

- Una redacción confirmada nunca se modifica.
- Si el PDF no pudo generarse por una falla técnica, debe reintentarse la generación utilizando la misma redacción confirmada.
- Si debe cambiarse el contenido, se crea una nueva revisión o un nuevo documento reemplazante, según el estado formal del documento.
- Si el documento ya fue emitido, firmado o notificado, la corrección debe realizarse mediante reemplazo o anulación formal del documento, conservando la trazabilidad completa.

Invariantes:

- Para `CONFIRMADA`: `fh_confirmacion` e `id_user_confirmacion` son obligatorios.
- Para `ANULADA`: `fh_anulacion`, `id_user_anulacion` y `motivo_anulacion` son obligatorios.
- Para `BORRADOR`: los campos de confirmación y anulación deben ser NULL.
- Debe existir como máximo una redacción BORRADOR activa por documento.
- Una redacción CONFIRMADA o ANULADA es inmutable.
- `plantilla_contenido_id` no puede modificarse luego de crear la redacción.
- `variables_snapshot_json`, `variables_faltantes_json` y `diagnostico_json` deben contener JSON válido.
- En MariaDB las columnas se declaran como `JSON`, lo que proporciona validación de estructura JSON, aunque internamente el tipo sea un alias textual compatible con `LONGTEXT`.


## 6. Notificaciones

### 6.1 Regla de simplificación

No duplicar domicilio estructurado completo en `fal_notificacion` ni `fal_notificacion_intento`.

Reglas:

- La fuente del domicilio usado es `fal_persona_domicilio` mediante `domicilio_notif_id`.
- Los domicilios usados por notificaciones no se sobrescriben.
- Si se encuentra domicilio nuevo/corregido, se crea una nueva fila o versión.
- Solo se mantiene `domicilio_destino_snap VARCHAR(255)` como texto histórico legible opcional.
- Una vez generado y firmado el PDF, el domicilio/destinatario quedan instanciados formalmente en el documento.

### 6.2 Tabla `fal_notificacion`

Registra el acto de notificación asociado a un acta, documento o pieza formal.

La notificación conserva el vínculo con el documento notificado, el destinatario, el domicilio físico o destino digital usado, el canal, el estado, las fechas relevantes y la trazabilidad del acto notificatorio.

No duplica datos completos de persona, domicilio, nomenclatura ni geografía, porque esos datos ya existen en estructura relacional versionada y/o quedan instanciados dentro del documento generado.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Notificación | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Acta relacionada |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento notificado | Obligatorio si la notificación corresponde a una pieza documental |
| `persona_destinatario_id` | `BIGINT` | SÍ | FK+IDX | Persona destinataria | Normalmente infractor u otro destinatario formal |
| `domicilio_notif_id` | `BIGINT` | SÍ | FK+IDX | Domicilio usado | Obligatorio si el canal usa domicilio físico |
| `tipo_notif` | `SMALLINT` | NO | IDX | Tipo de notificación | Catálogo funcional productivo |
| `canal_notif` | `SMALLINT` | NO | IDX | Canal de notificación | Catálogo funcional productivo |
| `estado_notif` | `SMALLINT` | NO | IDX | Estado de notificación | Catálogo funcional productivo |
| `destino_digital` | `VARCHAR(120)` | SÍ | IDX | Destino digital usado | Email, usuario portal, teléfono u otro identificador digital si aplica |
| `lote_id` | `BIGINT` | SÍ | FK+IDX | Lote principal/original de la notificación | FK a `fal_lote_correo` si aplica a correo, operador, mensajería o proceso batch |
| `fh_emision` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de emisión | Cuando se emite/genera la notificación |
| `fh_envio` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de envío | Cuando sale por el canal correspondiente |
| `fh_resultado` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de resultado | Cuando se registra resultado del canal |
| `fh_notificacion_positiva` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de notificación positiva | Si la notificación produjo efecto positivo |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas:

- `fal_notificacion` registra el acto de notificar.
- No guarda snapshots amplios de destinatario, domicilio, nomenclatura ni geografía.
- La persona destinataria se referencia por `persona_destinatario_id`.
- El domicilio físico usado se referencia por `domicilio_notif_id`.
- Si el canal usa domicilio físico, `domicilio_notif_id` es obligatorio.
- Si el canal no usa domicilio físico, `domicilio_notif_id` queda NULL.
- Para canales digitales, `destino_digital` conserva el destino exacto usado.
- Si la notificación corresponde a una pieza documental, debe informarse `documento_id`.
- Los datos emitidos formalmente quedan dentro del documento generado.
- La notificación no debe volver a copiar esos datos formales.
- Si se regenera un documento, se genera una nueva pieza/documento según el circuito correspondiente.
- La notificación anterior conserva su vínculo al documento originalmente notificado.
- Si cambia el domicilio de una persona después de una notificación, no se modifica la notificación anterior.
- Si el domicilio ya fue usado formalmente, no debe sobrescribirse.
- Para una nueva notificación a otro domicilio, se crea o referencia otro registro de domicilio.
- `fh_emision` registra cuándo se preparó o emitió la notificación.
- `fh_envio` registra cuándo fue enviada por el canal correspondiente.
- `fh_resultado` registra cuándo se obtuvo resultado del canal.
- `fh_notificacion_positiva` registra cuándo la notificación produjo efecto positivo.
- Las observaciones administrativas, aclaraciones o fundamentos no van en esta tabla; deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = NOTIFICACION`
  - `fal_observacion.entidad_id = fal_notificacion.id`

#### Catálogo `tipo_notif`

Valores productivos iniciales:

| Código enum | Descripción |
|---|---|
| `ACTA_INFRACCION` | Notificación del acta de infracción |
| `FALLO_ABSOLUTORIO` | Notificación de fallo absolutorio |
| `FALLO_CONDENATORIO` | Notificación de fallo condenatorio |

Reglas de `tipo_notif`:

- No usar valores genéricos si existe pieza específica.
- Si aparece una nueva pieza notificable real, se incorpora explícitamente.
- `tipo_notif` debe ser coherente con `documento_id` y `fal_documento.tipo_documento`.

#### Catálogo `canal_notif`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `CORREO_POSTAL` | Correo/lote postal |
| `NOTIFICADOR_MUNICIPAL` | Notificador municipal |
| `PRESENCIAL` | Notificación presencial/autonotificación en sede |
| `PORTAL_INFRACTOR` | Portal infractor/ciudadano |
| `EMAIL` | Correo electrónico / domicilio electrónico operativo |

Aliases históricos/no productivos:

| Alias | Productivo |
|---|---|
| `POSTAL` | `CORREO_POSTAL` |
| `DOMICILIO_ELECTRONICO` | `EMAIL` |

Reglas de `canal_notif`:

- Si el canal requiere domicilio físico, `domicilio_notif_id` es obligatorio.
- Si el canal es digital, `domicilio_notif_id` puede quedar NULL y debe usarse `destino_digital`.
- `PORTAL_INFRACTOR` no requiere domicilio físico.
- Si una pieza se visualiza por portal y produce efecto notificatorio, debe registrarse como notificación positiva por canal `PORTAL_INFRACTOR`.

#### Catálogo `estado_notif`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `PENDIENTE_PREPARACION` | Notificación pendiente de preparación |
| `LISTA_PARA_ENVIO` | Lista para envío |
| `ENVIADA` | Enviada / en curso por canal |
| `ENTREGADA` | Entregada / positiva |
| `NEGATIVA` | Resultado negativo |
| `VENCIDA` | Plazo o gestión vencida |
| `SIN_EFECTO` | Sin efecto por causal explícita |

Resultados funcionales asociados:

| Resultado | Uso |
|---|---|
| `SIN_RESULTADO` | Sin resultado todavía |
| `POSITIVA` | Notificación válida |
| `NEGATIVA` | Resultado negativo |
| `VENCIDA` | Vencimiento |
| `SUPERADA_POR_PORTAL` | Otro canal/intento queda sin efecto porque el portal produjo notificación válida |

Reglas por tipo de destino:

- Domicilio físico:
  - usar `domicilio_notif_id`;
  - no copiar domicilio completo en esta tabla;
  - el domicilio usado no se sobrescribe si ya fue usado formalmente.

- Portal infractor:
  - no requiere `domicilio_notif_id`;
  - puede usar `destino_digital` para identificar usuario, cuenta, sesión o referencia de portal;
  - al abrir una pieza notificable puede producir notificación positiva;
  - si una notificación postal de la misma pieza queda superada por portal, el intento/canal previo queda `SIN_EFECTO` con resultado/motivo `SUPERADA_POR_PORTAL`, pero el acto queda notificado positivamente por `PORTAL_INFRACTOR`.

- Email:
  - no requiere `domicilio_notif_id`;
  - debe conservar el destino exacto usado en `destino_digital`.

- Presencial:
  - puede no requerir domicilio si se realiza en sede municipal;
  - debe registrar resultado y fecha de notificación positiva si corresponde.

- Lote:
  - si el canal usa lote operativo, debe informarse `lote_id`;
  - el detalle de intentos/acuses se registra en tablas específicas.

Índices sugeridos:

- `KEY ix_fal_notif_acta (acta_id)`
- `KEY ix_fal_notif_documento (documento_id)`
- `KEY ix_fal_notif_persona (persona_destinatario_id)`
- `KEY ix_fal_notif_domicilio (domicilio_notif_id)`
- `KEY ix_fal_notif_tipo_estado (tipo_notif, estado_notif)`
- `KEY ix_fal_notif_canal_estado (canal_notif, estado_notif)`
- `KEY ix_fal_notif_lote (lote_id)`
- `KEY ix_fal_notif_destino_digital (destino_digital)`
- `KEY ix_fal_notif_fh_resultado (fh_resultado)`
- `KEY ix_fal_notif_fh_positiva (fh_notificacion_positiva)`

Decisiones tomadas:

- Se eliminan snapshots amplios de destinatario y domicilio.
- La notificación no duplica datos que ya existen en tablas relacionales versionadas.
- El documento generado conserva los datos emitidos formalmente.
- `fal_notificacion` queda enfocada en el acto notificatorio: destinatario, documento, domicilio/destino, canal, estado, fechas y trazabilidad.
- Para domicilio físico se guarda FK a `fal_persona_domicilio`.
- Para canales digitales se guarda el destino exacto usado en `destino_digital`.
- Las observaciones van por `fal_observacion`, no como campos textuales propios.


### 6.3 Tabla `fal_notificacion_intento`

Registra cada intento concreto realizado dentro de una notificación.

Una notificación puede tener uno o varios intentos.  
Cada intento conserva el canal usado, el destino efectivo, el lote operativo si corresponde, la fecha del intento, el resultado y la trazabilidad.

Un intento puede usar el mismo canal/domicilio/destino de la notificación principal o puede usar otro canal/domicilio/destino en un reintento.

Si en un reintento se utiliza un nuevo domicilio, ese domicilio no se guarda como snapshot en esta tabla.  
Debe registrarse previamente —o en la misma operación transaccional— en `fal_persona_domicilio`.  
Luego `fal_notificacion_intento.domicilio_notif_id` referencia el domicilio efectivamente usado en ese intento.

La tabla no duplica datos completos de domicilio, persona, nomenclatura ni geografía.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Intento de notificación | PK técnica |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | Notificación padre | FK a `fal_notificacion` |
| `nro_intento` | `SMALLINT` | NO | IDX | Número de intento | 1..N dentro de la notificación |
| `canal_notif` | `SMALLINT` | NO | IDX | Canal usado en el intento | Catálogo funcional productivo |
| `estado_intento` | `SMALLINT` | NO | IDX | Estado operativo del intento | Catálogo funcional productivo o derivado del circuito validado |
| `resultado_intento` | `SMALLINT` | SÍ | IDX | Resultado del intento | Catálogo funcional productivo o derivado del circuito validado |
| `domicilio_notif_id` | `BIGINT` | SÍ | FK+IDX | Domicilio usado en el intento | Obligatorio si el canal usa domicilio físico |
| `destino_digital` | `VARCHAR(120)` | SÍ | IDX | Destino digital usado en el intento | Email, teléfono, usuario portal u otro identificador digital si aplica |
| `lote_id` | `BIGINT` | SÍ | FK+IDX | Lote operativo del intento | NULL si el intento no fue procesado por lote |
| `referencia_externa` | `VARCHAR(80)` | SÍ | IDX | Referencia externa del intento | Tracking, código postal, pieza, ID integración o acuse externo |
| `fh_intento` | `DATETIME(6)` | NO | IDX | Fecha/hora del intento | Momento funcional del intento |
| `fh_resultado` | `DATETIME(6)` | SÍ | IDX | Fecha/hora del resultado | Obligatoria si se informa resultado |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas generales:

- `fal_notificacion_intento` registra intentos concretos de una notificación.
- Una notificación puede tener N intentos.
- `nro_intento` es correlativo dentro de `notificacion_id`.
- Debe existir una restricción única funcional:
  - `UNIQUE KEY uk_fal_notif_intento_nro (notificacion_id, nro_intento)`
- `canal_notif` debe alinearse con el catálogo real del dominio productivo definido.
- `estado_intento` y `resultado_intento` no deben inventarse desde el modelo.
- Si ya existen valores equivalentes en el prototipo, deben copiarse/alinearse con esos valores.
- Si no existen como enums separados en el prototipo, deben derivarse explícitamente del circuito validado de notificación/acuses, sin cambiar la semántica funcional.
- La notificación padre resume el estado global.
- Los intentos conservan el detalle histórico de cada gestión realizada.
- Un intento anterior no se modifica para representar un reintento posterior.
- Cada reintento se registra como una nueva fila.

Reglas de domicilio y destino:

- Si el canal usa domicilio físico, `domicilio_notif_id` es obligatorio.
- Si el canal no usa domicilio físico, `domicilio_notif_id` debe quedar NULL.
- Si el intento usa email, teléfono, portal u otro medio digital, el destino usado se guarda en `destino_digital`.
- No usar `email_destino_snap`; queda reemplazado por `destino_digital`.
- No usar `domicilio_destino_snap`; el domicilio usado se referencia por `domicilio_notif_id`.
- No copiar domicilio completo en esta tabla.
- Si para un reintento se detecta o carga un nuevo domicilio, se crea primero un registro en `fal_persona_domicilio`.
- El nuevo intento apunta a ese domicilio mediante `domicilio_notif_id`.
- Si el domicilio anterior ya fue usado formalmente, no se sobrescribe.
- La trazabilidad del cambio de domicilio debe quedar en evento y, si hace falta texto explicativo, en `fal_observacion`.

Regla de lote en intentos de notificación:

El lote debe conservarse a nivel de intento porque una misma notificación puede tener varios intentos y cada intento puede haber sido procesado por un lote diferente.

Campos relacionados:

- `fal_notificacion.lote_id`: lote principal/original de la notificación.
- `fal_notificacion_intento.lote_id`: lote operativo efectivo que procesó ese intento concreto.

Reglas:

- Si el intento fue generado/procesado dentro del mismo lote principal de la notificación:
  - `fal_notificacion_intento.lote_id = fal_notificacion.lote_id`
- Si el intento no fue procesado por lote:
  - `fal_notificacion_intento.lote_id = NULL`
- Si un reintento se genera en un lote posterior, el nuevo intento debe registrar ese nuevo lote:
  - `fal_notificacion_intento.lote_id = nuevo_lote_id`
- No debe sobrescribirse el lote de intentos anteriores.
- El lote original de la notificación no se cambia por cada reintento.
- Cada intento conserva el lote real que lo procesó.
- Esto permite reconstruir qué pieza salió en qué lote, cuándo, por qué canal, a qué destino y con qué resultado.

Ejemplo 1 — Notificación postal inicial en lote:

Notificación:

| Campo | Valor |
|---|---|
| `fal_notificacion.id` | `100` |
| `fal_notificacion.canal_notif` | `POSTAL` |
| `fal_notificacion.lote_id` | `20` |

Intento 1:

| Campo | Valor |
|---|---|
| `notificacion_id` | `100` |
| `nro_intento` | `1` |
| `canal_notif` | `POSTAL` |
| `lote_id` | `20` |
| `domicilio_notif_id` | domicilio original |
| `resultado_intento` | según prototipo |

Regla aplicada:

- La notificación nació en el lote `20`.
- El primer intento también fue procesado por el lote `20`.

Ejemplo 2 — Reintento por nuevo domicilio en otro lote:

Notificación original:

| Campo | Valor |
|---|---|
| `fal_notificacion.id` | `100` |
| `fal_notificacion.lote_id` | `20` |

Intento 1:

| Campo | Valor |
|---|---|
| `notificacion_id` | `100` |
| `nro_intento` | `1` |
| `lote_id` | `20` |
| `domicilio_notif_id` | domicilio original |
| `resultado_intento` | negativo / según prototipo |

Luego se detecta un nuevo domicilio.

Primero se registra el nuevo domicilio en:

| Tabla | Acción |
|---|---|
| `fal_persona_domicilio` | Alta de nuevo domicilio o nueva versión operativa |

Después se crea un nuevo lote postal:

| Campo | Valor |
|---|---|
| `nuevo_lote_id` | `35` |

Intento 2:

| Campo | Valor |
|---|---|
| `notificacion_id` | `100` |
| `nro_intento` | `2` |
| `canal_notif` | `POSTAL` |
| `lote_id` | `35` |
| `domicilio_notif_id` | nuevo domicilio |
| `resultado_intento` | según prototipo |

Regla aplicada:

- No se modifica el intento 1.
- No se modifica el domicilio original si ya fue usado formalmente.
- El nuevo domicilio queda registrado en `fal_persona_domicilio`.
- El intento 2 apunta al nuevo domicilio.
- El intento 2 registra el lote `35`, porque ese fue el lote efectivo del reintento.
- `fal_notificacion.lote_id` puede seguir apuntando al lote original `20`.

Ejemplo 3 — Notificación por portal sin lote:

Notificación:

| Campo | Valor |
|---|---|
| `fal_notificacion.id` | `200` |
| `canal_notif` | `PORTAL_INFRACTOR` |
| `lote_id` | `NULL` |

Intento / registro operativo:

| Campo | Valor |
|---|---|
| `notificacion_id` | `200` |
| `nro_intento` | `1` |
| `canal_notif` | `PORTAL_INFRACTOR` |
| `lote_id` | `NULL` |
| `domicilio_notif_id` | `NULL` |
| `destino_digital` | usuario/cuenta/referencia portal |
| `resultado_intento` | positivo / según prototipo |

Regla aplicada:

- Portal no requiere lote.
- Portal no requiere domicilio físico.
- Se conserva el destino digital o referencia de portal si corresponde.
- Si produce notificación positiva, debe actualizar la notificación padre según el estado exacto del prototipo.

Ejemplo 4 — Notificación inicial sin lote y reintento posterior en lote:

Notificación:

| Campo | Valor |
|---|---|
| `fal_notificacion.id` | `300` |
| `lote_id` | `NULL` |

Intento 1 presencial:

| Campo | Valor |
|---|---|
| `notificacion_id` | `300` |
| `nro_intento` | `1` |
| `canal_notif` | `PRESENCIAL` |
| `lote_id` | `NULL` |
| `resultado_intento` | negativo / según prototipo |

Luego se decide enviar por correo en lote `50`.

Intento 2 postal:

| Campo | Valor |
|---|---|
| `notificacion_id` | `300` |
| `nro_intento` | `2` |
| `canal_notif` | `POSTAL` |
| `lote_id` | `50` |
| `domicilio_notif_id` | domicilio usado |
| `resultado_intento` | según prototipo |

Regla aplicada:

- La notificación no nació en lote.
- El intento 1 tampoco tuvo lote.
- El intento 2 sí fue procesado por lote y registra `lote_id = 50`.

Reglas por tipo de destino:

- Domicilio físico:
  - requiere `domicilio_notif_id`;
  - no usa `destino_digital`;
  - no copia domicilio completo.

- Email:
  - no requiere `domicilio_notif_id`;
  - requiere `destino_digital` con el email usado.

- Teléfono / mensajería:
  - no requiere `domicilio_notif_id`;
  - requiere `destino_digital` con el teléfono o identificador usado.

- Portal infractor:
  - no requiere `domicilio_notif_id`;
  - puede usar `destino_digital` para usuario, cuenta, sesión o referencia del portal;
  - si produce notificación positiva, debe actualizar la notificación padre según el estado exacto del prototipo.

Reglas de `estado_intento`:

- Debe representar el estado operativo del intento.
- Debe alinearse con el dominio productivo definido o derivarse explícitamente del circuito funcional ya validado.
- No debe contradecir `estado_notif` de la notificación padre.
- La notificación padre resume el estado global.
- Los intentos conservan el detalle histórico de cada gestión realizada.

Reglas de `resultado_intento`:

- Debe representar el resultado concreto del intento.
- Debe alinearse con el dominio productivo definido o derivarse explícitamente del circuito funcional ya validado.
- Puede ser NULL mientras el intento todavía no tiene resultado.
- Cuando se registra resultado, debe informarse `fh_resultado`.
- Un resultado positivo puede actualizar la notificación padre a estado positivo según reglas del prototipo.
- Un resultado negativo puede habilitar nuevo intento, cambio de domicilio, cambio de canal o vencimiento, según reglas del prototipo.

Uso de `referencia_externa`:

- `referencia_externa` permite guardar un identificador corto del operador externo.
- Ejemplos:
  - tracking postal;
  - código de envío;
  - número de pieza;
  - ID de integración;
  - ID de acuse;
  - código de operación de mensajería.
- `referencia_externa` no debe contener JSON ni metadata extensa.
- Si se necesita guardar acuse completo, respuesta extensa o evidencia pesada, debe ir en tabla específica o storage externo.

Observaciones:

- Las observaciones administrativas o aclaraciones del intento no van en esta tabla.
- Deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = NOTIFICACION_INTENTO`
  - `fal_observacion.entidad_id = fal_notificacion_intento.id`

Índices sugeridos:

- `UNIQUE KEY uk_fal_notif_intento_nro (notificacion_id, nro_intento)`
- `KEY ix_fal_notif_intento_notif (notificacion_id)`
- `KEY ix_fal_notif_intento_canal_estado (canal_notif, estado_intento)`
- `KEY ix_fal_notif_intento_resultado (resultado_intento)`
- `KEY ix_fal_notif_intento_domicilio (domicilio_notif_id)`
- `KEY ix_fal_notif_intento_destino_digital (destino_digital)`
- `KEY ix_fal_notif_intento_lote (lote_id)`
- `KEY ix_fal_notif_intento_ref_ext (referencia_externa)`
- `KEY ix_fal_notif_intento_fh_intento (fh_intento)`
- `KEY ix_fal_notif_intento_fh_resultado (fh_resultado)`

Decisiones tomadas:

- Se elimina `email_destino_snap`; se reemplaza por `destino_digital`.
- Se elimina `domicilio_destino_snap`; se usa `domicilio_notif_id`.
- No se duplican datos de domicilio en el intento.
- El nuevo domicilio de un reintento se registra en `fal_persona_domicilio`, no como snapshot en el intento.
- El intento conserva solo la FK al domicilio efectivamente usado.
- `fal_notificacion.lote_id` representa el lote principal/original de la notificación.
- `fal_notificacion_intento.lote_id` representa el lote efectivo que procesó cada intento.
- Los reintentos pueden salir en lotes distintos.
- No se sobrescriben intentos anteriores.
- Los estados, canales y resultados deben quedar alineados al dominio productivo definido.
- La notificación padre resume el estado global.
- Los intentos conservan el detalle histórico de cada gestión realizada.

### 6.4 Tabla `fal_notificacion_acuse`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Acuse | PK técnica |
| `notificacion_id` | `BIGINT` | NO | FK+IDX | Notificación | Padre |
| `intento_id` | `BIGINT` | SÍ | FK+IDX | Intento | Si aplica |
| `tipo_acuse` | `SMALLINT` | NO | IDX | Tipo acuse | Recepción, rechazo, inexistente, desconocida, ausente, otro |
| `estado_acuse` | `SMALLINT` | NO | IDX | Estado acuse | Pendiente, recibido, validado, observado, anulado |
| `storage_key` | `VARCHAR(255)` | SÍ | IDX | Archivo acuse | Si existe |
| `fh_acuse` | `DATETIME(6)` | SÍ | IDX | Fecha acuse | Funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

### 6.5 Tabla `fal_lote_correo`

Diferible, pero definida si se implementa circuito postal/lote real.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Lote | PK técnica |
| `lote_codigo` | `VARCHAR(30)` | NO | UNIQUE | Código lote | Identificador operativo |
| `estado_lote` | `SMALLINT` | NO | IDX | Estado | GENERADO, EMITIDO, PROCESADO, ANULADO, CON_ERROR |
| `referencia_externa` | `VARCHAR(60)` | SÍ | IDX | Referencia externa | Correo/integración |
| `guid_lote_ext` | `CHAR(36)` | SÍ | IDX | GUID externo | Si existe |
| `fh_generacion` | `DATETIME(6)` | NO | IDX | Fecha generación | Funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

---

## 7. Fallo, apelación, archivo, paralización y gestión externa

### 7.1 Tabla `fal_acta_fallo`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Fallo | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente |
| `documento_id` | `BIGINT` | NO | FK+IDX | Documento fallo | Pieza formal obligatoria |
| `valorizacion_id` | `BIGINT` | SÍ | FK+IDX | Valorización usada | Obligatoria si condenatorio con monto |
| `tipo_fallo` | `SMALLINT` | NO | IDX | Tipo | ABSOLUTORIO, CONDENATORIO, NULIDAD, OTRO |
| `resultado_fallo` | `SMALLINT` | NO | IDX | Resultado | ABSUELVE, CONDENA, DECLARA_NULIDAD, etc. |
| `estado_fallo` | `SMALLINT` | NO | IDX | Estado | Borrador, dictado, firmado, notificado, firme, etc. |
| `monto_condena` | `DECIMAL(14,2)` | SÍ | — | Monto condena dictaminado | NULL si absolutorio; fuente primaria si la autoridad fija monto final ignorando valorización calculada |
| `fundamentos` | `TEXT` | SÍ | — | Fundamentos del fallo | Parte intrínseca del fallo; no delegar a `fal_observación`. Aprobado D5 |
| `fh_dictado` | `DATETIME(6)` | SÍ | IDX | Fecha dictado | Funcional |
| `id_user_dictado` | `CHAR(36)` | SÍ | IDX | Usuario dictado | Autoridad/operador |
| `fh_firma` | `DATETIME(6)` | SÍ | IDX | Fecha firma | Si firmado |
| `fh_notificacion` | `DATETIME(6)` | SÍ | IDX | Fecha notificación | Si notificado |
| `fh_vto_apelacion` | `DATE` | SÍ | IDX | Vencimiento apelación | Plazo |
| `fh_firmeza` | `DATETIME(6)` | SÍ | IDX | Fecha firmeza | Si firme |
| `origen_firmeza` | `SMALLINT` | SÍ | IDX | Origen de la firmeza | 1=VENCIMIENTO_PLAZO_APELACION; 2=APELACION_RECHAZADA. Aprobado D1 |
| `si_apelable` | `BOOLEAN` | NO | IDX | Apelable | Regla funcional |
| `si_firme` | `BOOLEAN` | NO | IDX | Firme | Se actualiza por acto |
| `si_vigente` | `BOOLEAN` | NO | IDX | Vigente | Solo uno vigente salvo regla |
| `acta_id_vigente` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN acta_id ELSE NULL END` |
| `fallo_reemplazado_id` | `BIGINT` | SÍ | FK+IDX | Fallo anterior | Si reemplaza |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Regla: fallo absolutorio no genera monto de condena exigible. (D5: `fundamentos` es campo intrínseco del fallo.)

Reglas de firmeza y origen de firmeza (D1):

- `si_firme` y `fh_firmeza` ya existían. Se agrega `origen_firmeza SMALLINT NULL` en Slice 8F-10.
- Valores aprobados: `1 = VENCIMIENTO_PLAZO_APELACION`, `2 = APELACION_RECHAZADA`.
- Si `si_firme = true`: `fh_firmeza` y `origen_firmeza` son obligatorios.
- Si `si_firme = false`: `fh_firmeza` y `origen_firmeza` son NULL.
- No existe tabla `fal_acta_firmeza_condena` en el modelo productivo. Aprobado D1.
- `FalActaFirmezaCondena` InMemory se reconcilia sobre estos campos al migrar a JDBC.

Reglas de monto de condena:

- `monto_condena` se mantiene en `fal_acta_fallo`.
- Si el fallo condenatorio toma una valorización confirmada, `valorizacion_id` referencia esa valorización y `monto_condena` conserva el monto dictaminado en el fallo.
- Si la autoridad dictamina un monto final ignorando o sobrescribiendo la valorización calculada, `fal_acta_fallo.monto_condena` es la fuente primaria de verdad del fallo.
- En ese caso debe generarse o vincularse una valorización coherente para integración económica/obligación de pago, pero no se elimina el monto histórico dictaminado en el fallo.
### 7.2 Tabla `fal_acta_apelacion`

Registra el trámite de apelación asociado a un fallo del acta.

La apelación puede presentarse por portal, presencialmente, por mesa de entrada o por otro canal funcional definido.  
Puede contener texto ingresado directamente, texto transcripto por operador y/o uno o varios documentos, escritos, adjuntos o evidencias presentadas por el infractor o su abogado/apoderado.

`fal_acta_apelacion` representa el trámite principal.  
Los documentos, escritos, adjuntos y evidencias presentadas se registran en `fal_acta_apelacion_documento`.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Apelación | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Acta/expediente apelado |
| `fallo_id` | `BIGINT` | NO | FK+IDX | Fallo apelado | Obligatorio |
| `canal_apelacion` | `SMALLINT` | NO | IDX | Canal de presentación | Catálogo funcional productivo |
| `tipo_presentacion` | `SMALLINT` | NO | IDX | Tipo de presentación | Texto, documentos o mixta |
| `texto_apelacion` | `TEXT` | SÍ | — | Texto de la apelación | Texto ingresado por portal o transcripto por operador |
| `fh_registro` | `DATETIME(6)` | NO | IDX | Fecha/hora de registro | Momento funcional de presentación |
| `id_user_registro` | `CHAR(36)` | SÍ | IDX | Usuario que registró | Usuario interno si aplica |
| `estado_apelacion` | `SMALLINT` | NO | IDX | Estado de apelación | Catálogo funcional productivo |
| `resultado_resolucion` | `SMALLINT` | SÍ | IDX | Resultado de resolución | Catálogo funcional productivo |
| `fh_resolucion` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de resolución | Obligatoria si está resuelta |
| `id_user_resolucion` | `CHAR(36)` | SÍ | IDX | Usuario que resolvió | Trazabilidad |
| `documento_resolucion_id` | `BIGINT` | SÍ | FK+IDX | Documento de resolución | Pieza formal que resuelve la apelación |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas:

- `fal_acta_apelacion` representa el trámite de apelación.
- Toda apelación debe estar asociada a un acta mediante `acta_id`.
- Toda apelación debe estar asociada al fallo apelado mediante `fallo_id`.
- `canal_apelacion`, `estado_apelacion` y `resultado_resolucion` deben alinearse con el dominio productivo definido.
- No inventar, renombrar ni generalizar enums ya existentes en el prototipo.
- `tipo_presentacion` indica si la apelación fue presentada como texto, documentos o ambas cosas.
- `texto_apelacion` se usa cuando:
  - el infractor ingresa texto por portal;
  - el operador transcribe una presentación presencial;
  - se desea conservar texto resumido o literal de la apelación.
- `texto_apelacion` puede ser `TEXT` porque representa contenido presentado por el infractor, abogado o apoderado.
- Los documentos, escritos, adjuntos y evidencias se registran en `fal_acta_apelacion_documento`.
- Una apelación puede tener cero, uno o muchos documentos/evidencias asociados.
- `documento_resolucion_id` no representa la presentación de la apelación; representa la pieza formal que resuelve la apelación.
- No mezclar documentos presentados con documento de resolución.
- Si la apelación se resuelve, debe informarse `fh_resolucion`.
- Si la apelación se resuelve mediante documento formal, debe informarse `documento_resolucion_id`.
- Las observaciones administrativas o aclaraciones internas no van en esta tabla; deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = APELACION`
  - `fal_observacion.entidad_id = fal_acta_apelacion.id`

#### Tabla `fal_acta_apelacion_documento`

Registra documentos, escritos, adjuntos y evidencias presentadas dentro de una apelación.

Esta tabla permite asociar múltiples documentos o archivos a una misma apelación, incluyendo escritos del infractor, escritos de abogado, notas escaneadas, documentación respaldatoria y evidencia presentada.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Documento/adjunto de apelación | PK técnica |
| `apelacion_id` | `BIGINT` | NO | FK+IDX | Apelación | FK a `fal_acta_apelacion` |
| `tipo_doc_apelacion` | `SMALLINT` | NO | IDX | Tipo de documento presentado | Catálogo funcional |
| `origen_presentacion` | `SMALLINT` | NO | IDX | Origen del documento | Infractor, abogado, operador, portal, mesa entrada |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento formal asociado | Si se incorporó como `fal_documento` |
| `storage_key` | `VARCHAR(255)` | SÍ | IDX | Archivo adjunto | PDF, imagen, escaneo u otro archivo |
| `nombre_archivo` | `VARCHAR(120)` | SÍ | — | Nombre original del archivo | Si aplica |
| `mime_type` | `SMALLINT` | SÍ | IDX | Tipo MIME | Catálogo funcional |
| `tamanio_bytes` | `BIGINT` | SÍ | — | Tamaño archivo | Si aplica |
| `fh_presentacion` | `DATETIME(6)` | NO | IDX | Fecha/hora de presentación | Momento funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas de documentos de apelación:

- Una apelación puede tener N documentos/evidencias asociados.
- Si el infractor presenta un escrito, se registra en esta tabla.
- Si el abogado/apoderado presenta un escrito, se registra en esta tabla.
- Si se presenta evidencia, documentación respaldatoria, imágenes o archivos, se registran en esta tabla.
- Si se escanea una nota presencial, se registra como documento/adjunto de apelación.
- `documento_id` se usa si el elemento fue incorporado como documento formal del sistema.
- `storage_key` se usa para ubicar el archivo persistido.
- Debe existir al menos una referencia material al contenido presentado:
  - `documento_id`, o
  - `storage_key`, o
  - `texto_apelacion` en la apelación principal.
- No guardar binarios directamente en la tabla.
- Las observaciones sobre un documento de apelación deben registrarse en `fal_observacion` si se requiere texto adicional.

#### Catálogo `tipo_presentacion`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `TEXTO` | Presentación cargada como texto |
| 2 | `DOCUMENTOS` | Presentación basada en documentos/adjuntos |
| 3 | `MIXTA` | Texto más uno o más documentos/adjuntos |

Reglas de `tipo_presentacion`:

- Si `tipo_presentacion = TEXTO`, debe informarse `texto_apelacion`.
- Si `tipo_presentacion = DOCUMENTOS`, debe existir al menos un registro en `fal_acta_apelacion_documento`.
- Si `tipo_presentacion = MIXTA`, debe informarse `texto_apelacion` y existir al menos un registro en `fal_acta_apelacion_documento`.

#### Catálogo `canal_apelacion`

Debe alinearse estrictamente con el dominio productivo definido.

Valores funcionales esperados según circuito validado:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PORTAL_INFRACTOR` | Presentación ingresada por portal |
| 2 | `PRESENCIAL` | Presentación presencial cargada por operador |
| 3 | `MESA_ENTRADA` | Presentación ingresada por mesa de entrada |
| 4 | `EXTERNO` | Presentación recibida por integración o canal externo |

Reglas de `canal_apelacion`:

- Si el circuito productivo ya define estos valores con otros nombres exactos, usar los nombres literales del prototipo.
- No agregar canales que no estén funcionalmente validados.
- `PORTAL_INFRACTOR` permite texto online y adjuntos.
- `PRESENCIAL` permite texto transcripto por operador y adjuntos/escaneos.
- `MESA_ENTRADA` permite referencia a documentación ingresada formalmente.
- `EXTERNO` se usa solo si existe integración o recepción externa real.

#### Catálogo `estado_apelacion`

Debe alinearse estrictamente con el dominio productivo definido.

Valores funcionales esperados:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `PRESENTADA` | Apelación presentada |
| 2 | `EN_ANALISIS` | En análisis/revisión |
| 3 | `RESUELTA` | Resuelta |
| 4 | `ANULADA` | Anulada o dejada sin efecto |
| 5 | `FUERA_TERMINO` | Presentada fuera de término |

Reglas de `estado_apelacion`:

- Si el circuito productivo ya define estos valores con otros nombres exactos, usar los nombres literales del prototipo.
- Una apelación `RESUELTA` debe tener `resultado_resolucion`.
- Una apelación `RESUELTA` debe tener `fh_resolucion`.
- Una apelación `ANULADA` o `FUERA_TERMINO` conserva igualmente los documentos/evidencias presentados.

#### Catálogo `resultado_resolucion`

Debe alinearse estrictamente con el dominio productivo definido.

Valores funcionales esperados:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `RECHAZADA` | Se rechaza la apelación |
| 2 | `ACEPTADA_ABSUELVE` | Se acepta la apelación y absuelve |
| 3 | `MODIFICA_CONDENA` | Se modifica la condena |
| 4 | `NULIDAD` | Se declara nulidad |

Reglas de `resultado_resolucion`:

- Si el circuito productivo ya define estos valores con otros nombres exactos, usar los nombres literales del prototipo.
- Si `resultado_resolucion = RECHAZADA`, se mantiene el efecto del fallo apelado según reglas del circuito.
- Si `resultado_resolucion = ACEPTADA_ABSUELVE`, debe impactar en el resultado final del acta según reglas del prototipo.
- Si `resultado_resolucion = MODIFICA_CONDENA`, debe generar o vincular la valorización correspondiente.
- Si `resultado_resolucion = NULIDAD`, debe impactar en el estado/resultado del acta según reglas del prototipo.
- La resolución formal se registra en `documento_resolucion_id`.

#### Catálogo `tipo_doc_apelacion`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ESCRITO_APELACION` | Escrito principal de apelación |
| 2 | `NOTA_INFRACTOR` | Nota presentada por el infractor |
| 3 | `ESCRITO_ABOGADO` | Escrito presentado por abogado/apoderado |
| 4 | `EVIDENCIA_INFRACTOR` | Evidencia presentada por el infractor |
| 5 | `EVIDENCIA_ABOGADO` | Evidencia presentada por abogado/apoderado |
| 6 | `DOCUMENTACION_RESPALDATORIA` | Documentación respaldatoria |
| 7 | `CONSTANCIA_PRESENTACION` | Constancia/cargo de presentación |
| 8 | `OTRO` | Otro documento controlado |

#### Catálogo `origen_presentacion`

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `INFRACTOR` | Presentado por el infractor |
| 2 | `ABOGADO` | Presentado por abogado/apoderado |
| 3 | `OPERADOR_INTERNO` | Incorporado por operador municipal |
| 4 | `PORTAL_INFRACTOR` | Ingresado por portal |
| 5 | `MESA_ENTRADA` | Ingresado por mesa de entrada |
| 6 | `INTEGRACION_EXTERNA` | Recibido desde integración externa |

Índices sugeridos:

- `KEY ix_fal_apelacion_acta (acta_id)`
- `KEY ix_fal_apelacion_fallo (fallo_id)`
- `KEY ix_fal_apelacion_estado (estado_apelacion)`
- `KEY ix_fal_apelacion_canal (canal_apelacion)`
- `KEY ix_fal_apelacion_resolucion (resultado_resolucion)`
- `KEY ix_fal_apelacion_doc_resolucion (documento_resolucion_id)`
- `KEY ix_fal_apelacion_documento_apelacion (apelacion_id)`
- `KEY ix_fal_apelacion_documento_tipo (tipo_doc_apelacion)`
- `KEY ix_fal_apelacion_documento_origen (origen_presentacion)`
- `KEY ix_fal_apelacion_documento_documento (documento_id)`
- `KEY ix_fal_apelacion_documento_storage (storage_key)`

Decisiones tomadas:

- Se elimina `documento_id` como único documento de presentación en `fal_acta_apelacion`.
- La apelación puede contener texto, documentos o ambos.
- `texto_apelacion` queda en la tabla principal porque representa contenido textual de la presentación.
- Los documentos, escritos, adjuntos y evidencias se modelan en `fal_acta_apelacion_documento`.
- Una apelación puede tener muchos documentos/evidencias.
- El infractor y el abogado/apoderado pueden presentar documentos y evidencias.
- `documento_resolucion_id` queda reservado exclusivamente para la pieza formal de resolución.
- Presentación y resolución no se mezclan.
- Los catálogos que ya existan en prototipo deben alinearse literalmente con el prototipo.
- Las observaciones van por `fal_observacion`, no como campos textuales adicionales.
### 7.3 Tabla `fal_acta_paralizacion`

Registra los ciclos de paralización y reactivación de un acta.

Una misma acta puede paralizarse y reactivarse varias veces durante su vida administrativa.  
Cada fila representa un ciclo completo o abierto de paralización.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Paralización | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente |
| `motivo_paralizacion` | `SMALLINT` | NO | IDX | Motivo de paralización | Catálogo funcional productivo |
| `fh_paralizacion` | `DATETIME(6)` | NO | IDX | Fecha/hora de paralización | Momento funcional |
| `id_user_paralizacion` | `CHAR(36)` | NO | IDX | Usuario que paraliza | Trazabilidad |
| `fh_reactivacion` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de reactivación | Si se reactiva |
| `id_user_reactivacion` | `CHAR(36)` | SÍ | IDX | Usuario que reactiva | Trazabilidad |
| `si_activa` | `BOOLEAN` | NO | IDX | Paralización activa | Solo una activa por acta |
| `acta_id_activa` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_activa = true THEN acta_id ELSE NULL END` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas:

- `fal_acta_paralizacion` registra ciclos de paralización.
- Una acta puede tener múltiples paralizaciones históricas.
- Cada nueva paralización crea una nueva fila.
- No se sobrescriben paralizaciones anteriores.
- Una fila con `si_activa = true` representa la paralización vigente.
- Al reactivar un acta:
  - se completa `fh_reactivacion`;
  - se completa `id_user_reactivacion`;
  - se actualiza `si_activa = false`.
- Solo puede existir una paralización activa por acta.
- Si el acta se vuelve a paralizar después de una reactivación, se crea una nueva fila.
- `motivo_paralizacion` debe alinearse con el dominio productivo definido.
- No guardar observaciones textuales en esta tabla.
- Las observaciones o fundamentos de paralización/reactivación deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = PARALIZACION`
  - `fal_observacion.entidad_id = fal_acta_paralizacion.id`

#### Catálogo `motivo_paralizacion`

Debe alinearse estrictamente con el dominio productivo definido.

Valores funcionales ya usados en prototipo:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ESPERA_DOCUMENTAL` | Paralizada por espera de documentación |
| 2 | `OTRO` | Otro motivo controlado |

Reglas de `motivo_paralizacion`:

- No inventar motivos desde el modelo.
- Si el circuito productivo incorpora nuevos motivos, agregarlos explícitamente al catálogo.
- Si `motivo_paralizacion = OTRO`, el detalle debe registrarse en `fal_observacion`, no en esta tabla.

Índices sugeridos:

- `KEY ix_fal_paralizacion_acta (acta_id)`
- `KEY ix_fal_paralizacion_activa (acta_id, si_activa)`
- `KEY ix_fal_paralizacion_motivo (motivo_paralizacion)`
- `KEY ix_fal_paralizacion_fh (fh_paralizacion)`
- `KEY ix_fal_paralizacion_reactivacion (fh_reactivacion)`

Regla de unicidad funcional:

- No puede existir más de una paralización activa por acta.
- En MariaDB se implementará con lógica transaccional del backend y columna generada nullable + índice único auxiliar.

Decisiones tomadas:

- La tabla permite múltiples ciclos de paralización/reactivación por acta.
- No se pierde trazabilidad porque cada ciclo queda como fila histórica.
- `si_activa` indica cuál es la paralización vigente.
- La reactivación no elimina la paralización: completa el cierre del ciclo.
- `observacion_paralizacion` se elimina de esta tabla.
- Las observaciones van por `fal_observacion`.
### 7.4 Tabla `fal_acta_archivo`

Registra los ciclos de archivo y reingreso de un acta.

Una misma acta puede archivarse y reingresar varias veces durante su vida administrativa.  
Cada fila representa un ciclo completo o abierto de archivo.

Este modelo conserva trazabilidad histórica de cada archivo/reingreso y permite identificar cuál es el archivo actualmente activo.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Archivo histórico | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente |
| `id_motivo_archivo` | `BIGINT` | NO | FK+IDX | Motivo de archivo | FK a `fal_motivo_archivo` |
| `si_permite_reingreso_snapshot` | `BOOLEAN` | NO | IDX | Permite reingreso | Snapshot del motivo al archivar |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento de archivo | Pieza formal si existe |
| `si_nulidad_snapshot` | `BOOLEAN` | NO | IDX | Nulidad | Snapshot del motivo al archivar |
| `est_proc_act_origen` | `CHAR(4)` | NO | IDX | Estado origen | Estado antes del archivo |
| `sit_adm_act_origen` | `CHAR(4)` | NO | IDX | Situación origen | Situación antes del archivo |
| `bloque_origen` | `CHAR(4)` | NO | IDX | Bloque origen | Bloque antes del archivo |
| `cod_bandeja_origen` | `VARCHAR(50)` | SÍ | IDX | Bandeja origen | Snapshot operativo antes del archivo |
| `sub_bandeja_origen` | `VARCHAR(60)` | SÍ | IDX | Sub-bandeja origen | Snapshot operativo antes del archivo |
| `accion_pendiente_origen` | `CHAR(6)` | SÍ | IDX | Acción pendiente origen | Acción antes del archivo |
| `observacion_id` | `BIGINT` | SÍ | FK+IDX | Observación/fundamento | FK a `fal_observacion` |
| `evento_archivo_id` | `BIGINT` | SÍ | FK+IDX | Evento de archivo | FK a `fal_acta_evento` |
| `fh_archivo` | `DATETIME(6)` | NO | IDX | Fecha/hora de archivo | Momento funcional |
| `id_user_archivo` | `CHAR(36)` | NO | IDX | Usuario que archiva | Trazabilidad |
| `fh_reingreso` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de reingreso | Si reingresa |
| `id_user_reingreso` | `CHAR(36)` | SÍ | IDX | Usuario que reingresa | Trazabilidad |
| `si_activo` | `BOOLEAN` | NO | IDX | Archivo activo | Solo un archivo activo por acta |
| `acta_id_activa` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_activo = true THEN acta_id ELSE NULL END` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas:

- `fal_acta_archivo` registra ciclos de archivo/reingreso.
- Una acta puede tener múltiples archivos históricos.
- Cada nuevo archivo crea una nueva fila.
- No se sobrescriben archivos anteriores.
- Una fila con `si_activo = true` representa el archivo vigente.
- Al reingresar un acta:
  - se completa `fh_reingreso`;
  - se completa `id_user_reingreso`;
  - se actualiza `si_activo = false`.
- Si luego el acta vuelve a archivarse, se crea una nueva fila.
- Solo puede existir un archivo activo por acta.
- Si `permite_reingreso = false`, no debe permitirse reingreso operativo salvo regla excepcional explícita.
- `motivo_archivo` debe alinearse con el dominio productivo definido.
- No inventar motivos desde el modelo.
- Si existe una pieza formal de archivo, se referencia en `documento_id`.
- El documento de archivo no reemplaza la trazabilidad estructurada de esta tabla.
- Las observaciones o fundamentos textuales del archivo/reingreso deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = ARCHIVO`
  - `fal_observacion.entidad_id = fal_acta_archivo.id`

#### Tabla `fal_motivo_archivo`

`motivo_archivo` es administrable. No queda cerrado a los valores del prototipo ni a un set fijo de tres motivos.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Motivo archivo | PK técnica |
| `cod_motivo_archivo` | `VARCHAR(32)` | NO | UNIQUE | Código estable | Código funcional |
| `nombre` | `VARCHAR(80)` | NO | IDX | Nombre visible | Administrable |
| `descripcion` | `VARCHAR(255)` | SÍ | — | Descripción | Administrable |
| `si_nulidad` | `BOOLEAN` | NO | IDX | Es nulidad | Cambia reglas jurídicas |
| `si_permite_reingreso` | `BOOLEAN` | NO | IDX | Permite reingreso | Regla operativa |
| `si_requiere_observacion` | `BOOLEAN` | NO | — | Requiere fundamento | Si obliga observación |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | Vigencia |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas de `fal_motivo_archivo`:

- El catálogo puede crecer por administración del sistema.
- El texto libre/fundamento no se guarda en el motivo, se registra como `fal_observacion`.
- `si_nulidad` y `si_permite_reingreso` se copian como snapshot en el ciclo de archivo para mantener historia.
- Un motivo inactivo no debe poder usarse para nuevos archivos.

#### Reglas de archivo/reingreso

- Archivo debe ser transición explícita, no efecto colateral.
- No se archiva automáticamente sin decisión/motivo.
- Cada archivo crea fila histórica en `fal_acta_archivo`.
- Reingreso cierra el ciclo activo, no elimina la fila.
- El ciclo de archivo debe conservar origen de proceso para reingresar de manera determinista:
  - `est_proc_act_origen`;
  - `sit_adm_act_origen`;
  - `bloque_origen`;
  - `cod_bandeja_origen`;
  - `sub_bandeja_origen`;
  - `accion_pendiente_origen`.
- Las observaciones o fundamentos textuales del archivo/reingreso deben registrarse en `fal_observacion`.

Índices sugeridos:

- `KEY ix_fal_archivo_acta (acta_id)`
- `KEY ix_fal_archivo_activo (acta_id, si_activo)`
- `KEY ix_fal_archivo_motivo (id_motivo_archivo)`
- `KEY ix_fal_archivo_fh (fh_archivo)`
- `KEY ix_fal_archivo_reingreso (fh_reingreso)`
- `KEY ix_fal_archivo_documento (documento_id)`

Regla de unicidad funcional:

- No puede existir más de un archivo activo por acta.
- En MariaDB se implementará con lógica transaccional del backend y columna generada nullable + índice único auxiliar.

Decisiones tomadas:

- La tabla permite múltiples ciclos de archivo/reingreso por acta.
- No se pierde trazabilidad porque cada ciclo queda como fila histórica.
- `si_activo` indica cuál es el archivo vigente.
- El reingreso no elimina el archivo: completa el cierre del ciclo.
- Si el acta vuelve a archivarse, se crea una nueva fila.
- Las observaciones van por `fal_observacion`.


### 7.5 Tabla `fal_acta_gestion_externa`

Registra los ciclos de gestión externa de un acta.

Una misma acta puede salir a gestión externa, reingresar y luego volver a salir a otra gestión externa durante su vida administrativa.

Cada fila representa un ciclo completo o abierto de gestión externa.

Ejemplos de gestión externa:

- envío a Juzgado de Paz;
- envío a Apremio;
- envío a otro organismo externo;
- reingreso desde gestión externa.

Este modelo conserva trazabilidad histórica de cada salida/reingreso y permite identificar cuál es la gestión externa actualmente activa.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Gestión externa | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente |
| `tipo_gestion_ext` | `VARCHAR(20)` | NO | IDX | Tipo de gestión externa | `APREMIO`, `JUZGADO_DE_PAZ` |
| `estado_gestion_ext` | `VARCHAR(20)` | NO | IDX | Estado de gestión externa | `ENVIADA`, `EN_TRAMITE`, `REINGRESADA`, `ANULADA` |
| `resultado_gestion_ext` | `VARCHAR(24)` | SÍ | IDX | Resultado de gestión externa | Resultado externo tipificado |
| `modo_reingreso_gestion_ext` | `VARCHAR(36)` | SÍ | IDX | Modo de reingreso | Cómo vuelve al circuito interno |
| `monto_resultado` | `DECIMAL(14,2)` | SÍ | — | Monto externo | Si el resultado modifica/registra monto |
| `documento_envio_id` | `BIGINT` | SÍ | FK+IDX | Documento de envío | Pieza formal de remisión si existe |
| `documento_resultado_id` | `BIGINT` | SÍ | FK+IDX | Documento de resultado/reingreso | Pieza formal si existe |
| `fh_envio` | `DATETIME(6)` | NO | IDX | Fecha/hora de envío | Momento funcional de salida externa |
| `id_user_envio` | `CHAR(36)` | NO | IDX | Usuario que envía | Trazabilidad |
| `fh_reingreso` | `DATETIME(6)` | SÍ | IDX | Fecha/hora de reingreso | Si reingresa |
| `id_user_reingreso` | `CHAR(36)` | SÍ | IDX | Usuario que registra reingreso | Trazabilidad |
| `si_activa` | `BOOLEAN` | NO | IDX | Gestión externa activa | Solo una activa por acta |
| `acta_id_activa` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_activa = true THEN acta_id ELSE NULL END` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
| `fh_ult_mod` | `DATETIME(6)` | SÍ | — | Última modificación | Auditoría |
| `id_user_ult_mod` | `CHAR(36)` | SÍ | — | Usuario última modificación | Auditoría |

Reglas:

- `fal_acta_gestion_externa` registra ciclos de salida/reingreso externo.
- Una acta puede tener múltiples gestiones externas históricas.
- Cada nueva salida a gestión externa crea una nueva fila.
- No se sobrescriben gestiones externas anteriores.
- Una fila con `si_activa = true` representa la gestión externa vigente.
- Al reingresar un acta desde gestión externa:
  - se completa `fh_reingreso`;
  - se completa `id_user_reingreso`;
  - se actualiza `estado_gestion_ext` según catálogo/prototipo;
  - se completa `resultado_gestion_ext` si corresponde;
  - se actualiza `si_activa = false`.
- Si luego el acta vuelve a enviarse a gestión externa, se crea una nueva fila.
- Solo puede existir una gestión externa activa por acta.
- `tipo_gestion_ext`, `estado_gestion_ext` y `resultado_gestion_ext` deben alinearse con el dominio productivo definido.
- No inventar tipos, estados ni resultados sin decisión funcional explícita.
- Si existe una pieza formal de envío, se referencia en `documento_envio_id`.
- Si existe una pieza formal de resultado o reingreso, se referencia en `documento_resultado_id`.
- Los documentos no reemplazan la trazabilidad estructurada de esta tabla.
- Las observaciones, fundamentos o comentarios del envío/reingreso externo deben registrarse en `fal_observacion`.
- Para observaciones sobre esta entidad:
  - `fal_observacion.entidad_tipo = GESTION_EXTERNA`
  - `fal_observacion.entidad_id = fal_acta_gestion_externa.id`

#### Catálogo `tipo_gestion_ext`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `APREMIO` | Gestión externa por apremio |
| `JUZGADO_DE_PAZ` | Gestión externa ante Juzgado de Paz |

#### Catálogo `estado_gestion_ext`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `ENVIADA` | Gestión enviada al organismo externo |
| `EN_TRAMITE` | Gestión en trámite externo |
| `REINGRESADA` | Acta reingresada al circuito interno |
| `ANULADA` | Gestión anulada sin efecto operativo |

Reglas:

- `REINGRESADA` cierra el ciclo activo.
- Una gestión activa normalmente no debe tener `fh_reingreso`.
- Una gestión reingresada debe tener `fh_reingreso`.

#### Catálogo `resultado_gestion_ext`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `SIN_RESULTADO` | Sin resultado externo |
| `SIN_CAMBIOS` | Reingresa sin cambios sustantivos |
| `PAGO_REGISTRADO` | Se registró pago externo |
| `SIN_PAGO` | Reingresa sin pago |
| `ABSUELVE` | El externo propone/dispone absolver |
| `CONFIRMA_CONDENA` | El externo confirma condena |
| `MODIFICA_MONTO` | El externo modifica monto |

#### Catálogo `modo_reingreso_gestion_ext`

Valores productivos:

| Código enum | Descripción |
|---|---|
| `REINGRESO_CON_PAGO` | Vuelve con pago registrado |
| `REINGRESO_SIN_PAGO` | Vuelve sin pago |
| `REINGRESO_CON_DICTAMEN` | Vuelve con dictamen/resolución externa |
| `REINGRESO_PARA_NUEVO_FALLO` | Vuelve para dictar nuevo fallo |
| `REINGRESO_PARA_CIERRE` | Vuelve para evaluar cierre |
| `REINGRESO_PARA_REVISION` | Vuelve para revisión interna |

Reglas de gestión externa:

- No usar un único string compuesto para mezclar tipo, resultado, modo de reingreso, pago, documento y monto.
- `SIN_CAMBIOS` empareja naturalmente con `REINGRESO_PARA_REVISION`.
- La documentación externa recibida se registra como `fal_documento`/adjuntos y se vincula al ciclo de gestión externa.
- Los comentarios o fundamentos van por `fal_observacion`.

Índices sugeridos:

- `KEY ix_fal_gestion_ext_acta (acta_id)`
- `KEY ix_fal_gestion_ext_activa (acta_id, si_activa)`
- `KEY ix_fal_gestion_ext_tipo (tipo_gestion_ext)`
- `KEY ix_fal_gestion_ext_estado (estado_gestion_ext)`
- `KEY ix_fal_gestion_ext_resultado (resultado_gestion_ext)`
- `KEY ix_fal_gestion_ext_modo_reingreso (modo_reingreso_gestion_ext)`
- `KEY ix_fal_gestion_ext_fh_envio (fh_envio)`
- `KEY ix_fal_gestion_ext_fh_reingreso (fh_reingreso)`
- `KEY ix_fal_gestion_ext_doc_envio (documento_envio_id)`
- `KEY ix_fal_gestion_ext_doc_resultado (documento_resultado_id)`

Regla de unicidad funcional:

- No puede existir más de una gestión externa activa por acta.
- En MariaDB se implementará con lógica transaccional del backend y columna generada nullable + índice único auxiliar.

Decisiones tomadas:

- La tabla permite múltiples ciclos de gestión externa/reingreso por acta.
- No se pierde trazabilidad porque cada ciclo queda como fila histórica.
- `si_activa` indica cuál es la gestión externa vigente.
- El reingreso no elimina la gestión externa: completa el cierre del ciclo.
- Si el acta vuelve a enviarse a gestión externa, se crea una nueva fila.
- Las observaciones van por `fal_observacion`.


---

## 8. Pagos e integración con Ingresos/Tesorería
### 8.1 Regla general

Faltas no es cuenta corriente ni tesorería. La verdad financiera vive en Ingresos/Tesorería. Faltas conserva obligación jurídica/administrativa, referencias, movimientos y caches operativos.

Tablas definitivas:

- `fal_acta_obligacion_pago`
- `fal_acta_forma_pago`
- `fal_acta_plan_pago_ref`
- `fal_acta_pago_movimiento`

### 8.2 Tabla `fal_acta_obligacion_pago`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Obligación | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta | Expediente |
| `persona_id` | `BIGINT` | NO | FK+IDX | Persona obligada | Infractor/persona |
| `tipo_obligacion` | `SMALLINT` | NO | IDX | Tipo | VOLUNTARIO, CONDENA |
| `valorizacion_id` | `BIGINT` | SÍ | FK+IDX | Valorización origen | Debe estar confirmada |
| `fallo_id` | `BIGINT` | SÍ | FK+IDX | Fallo origen | Si condena |
| `monto_original` | `DECIMAL(14,2)` | NO | — | Monto obligación | Desde valorización confirmada |
| `estado_obligacion` | `SMALLINT` | NO | IDX | Estado | Vigente, deuda emitida, en plan, cancelada, etc. |
| `fh_determinacion` | `DATETIME(6)` | NO | IDX | Fecha determinación | Funcional |
| `id_user_determinacion` | `CHAR(36)` | SÍ | IDX | Usuario determinación | Trazabilidad |
| `forma_pago_vigente_id` | `BIGINT` | SÍ | FK+IDX | Forma vigente | Contado/plan/refinanciación |
| `si_apta_intimacion` | `BOOLEAN` | NO | IDX | Apta intimación | Cache operativo |
| `fh_apta_intimacion` | `DATETIME(6)` | SÍ | IDX | Fecha aptitud | Si aplica |
| `motivo_apta_intimacion` | `SMALLINT` | SÍ | IDX | Motivo | Enum estable |
| `cant_dias_mora` | `SMALLINT` | SÍ | — | Días mora | Cache operativo |
| `cant_cuotas_mora` | `SMALLINT` | SÍ | — | Cuotas mora | Cache operativo |
| `cant_cuotas_mora_consec` | `SMALLINT` | SÍ | — | Mora consecutiva | Cache operativo |
| `fh_cancelacion` | `DATETIME(6)` | SÍ | IDX | Cancelación | Si cancelada |
| `si_excluir_escaneo` | `BOOLEAN` | NO | IDX | Excluir sync | Cancelada/anulada/refinanciada/cerrada |
| `fh_ult_sync_ingresos` | `DATETIME(6)` | SÍ | IDX | Última sync | Integración |
| `si_vigente` | `BOOLEAN` | NO | IDX | Vigente | Control |
| `acta_id_vigente` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN acta_id ELSE NULL END` |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |
### 8.3 Tabla `fal_acta_forma_pago`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Forma pago | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `obligacion_pago_id` | `BIGINT` | NO | FK+IDX | Obligación | Padre |
| `nro_forma` | `SMALLINT` | NO | IDX | Número forma | Historial |
| `tipo_forma_pago` | `SMALLINT` | NO | IDX | Tipo | CONTADO, PLAN_PAGO, REFINANCIACION |
| `estado_forma_pago` | `SMALLINT` | NO | IDX | Estado | Generada, activa, confirmada, mora, caída, etc. |
| `monto_forma` | `DECIMAL(14,2)` | NO | — | Monto | Monto de esta forma |
| `Cmte_EM` | `CHAR(2)` | SÍ | IDX | Comprobante emisión | Referencia ingresos |
| `Pref_EM` | `SMALLINT` | SÍ | IDX | Prefijo emisión | Referencia ingresos |
| `Nro_EM` | `INT` | SÍ | IDX | Número emisión | Referencia ingresos |
| `Cmte_PG` | `CHAR(2)` | SÍ | IDX | Comprobante pago | Recibo/pago si contado |
| `Pref_PG` | `SMALLINT` | SÍ | IDX | Prefijo pago | Recibo/pago |
| `Nro_PG` | `INT` | SÍ | IDX | Número pago | Recibo/pago |
| `forma_reemplazada_id` | `BIGINT` | SÍ | FK+IDX | Forma anterior | Refinanciación |
| `fh_generacion` | `DATETIME(6)` | NO | IDX | Generación | Funcional |
| `fh_pago_procesado` | `DATETIME(6)` | SÍ | IDX | Pago procesado | No cierra |
| `fh_pago_confirmado` | `DATETIME(6)` | SÍ | IDX | Pago confirmado | Habilita cierre si corresponde |
| `fh_baja` | `DATETIME(6)` | SÍ | IDX | Baja | Si reemplazada/anulada |
| `motivo_baja` | `SMALLINT` | SÍ | IDX | Motivo baja | Catálogo |
| `si_vigente` | `BOOLEAN` | NO | IDX | Vigente | Forma actual |
| `obligacion_pago_id_vigente` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN obligacion_pago_id ELSE NULL END` |
| `tipo_forma_pago_vigente` | `SMALLINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN tipo_forma_pago ELSE NULL END` |
| `si_excluir_escaneo` | `BOOLEAN` | NO | IDX | Excluir sync | Si no corresponde consultar |
### 8.4 Tabla `fal_acta_plan_pago_ref`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Plan ref | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `forma_pago_id` | `BIGINT` | NO | FK+IDX | Forma pago | Debe ser PLAN/REFINANCIACIÓN |
| `obligacion_pago_id` | `BIGINT` | NO | FK+IDX | Obligación | Padre |
| `id_tdoc_plan` | `SMALLINT` | NO | IDX | Tipo doc plan | Para plan: `1` |
| `id_doc_plan` | `BIGINT` | NO | IDX | ID plan externo | Fuente Ingresos |
| `estado_plan` | `SMALLINT` | NO | IDX | Estado plan | Cache operativo desde Ingresos |
| `fh_generacion_plan` | `DATETIME(6)` | SÍ | IDX | Generación plan | Informada por Ingresos |
| `cantidad_cuotas` | `SMALLINT` | NO | — | Cantidad cuotas | Característica del plan |
| `importe_total_plan` | `DECIMAL(14,2)` | NO | — | Total plan | Cache operativo |
| `importe_cuota_regular` | `DECIMAL(14,2)` | SÍ | — | Cuota regular | Cache operativo |
| `cantidad_cuotas_pagadas` | `SMALLINT` | SÍ | — | Cuotas pagadas | Cache, no verdad financiera primaria |
| `cantidad_cuotas_vencidas` | `SMALLINT` | SÍ | — | Cuotas vencidas | Cache |
| `cantidad_cuotas_en_mora` | `SMALLINT` | SÍ | — | Cuotas mora | Cache |
| `cantidad_cuotas_mora_consec` | `SMALLINT` | SÍ | — | Mora consecutiva | Cache |
| `dias_mora_max` | `SMALLINT` | SÍ | — | Días mora max | Cache |
| `fh_ultimo_pago` | `DATETIME(6)` | SÍ | IDX | Último pago | Cache |
| `fh_caida` | `DATETIME(6)` | SÍ | IDX | Fecha caída | Si caído |
| `fh_cancelacion` | `DATETIME(6)` | SÍ | IDX | Cancelación | Si cancelado |
| `fh_refinanciacion` | `DATETIME(6)` | SÍ | IDX | Refinanciación | Si refinanciado |
| `plan_refinanciado_id` | `BIGINT` | SÍ | FK+IDX | Nuevo plan | Si refinanciado |
| `si_apto_intimacion` | `BOOLEAN` | NO | IDX | Apto intimación | Cache operativo |
| `fh_apto_intimacion` | `DATETIME(6)` | SÍ | IDX | Fecha aptitud | Si aplica |
| `motivo_apta_intimacion` | `SMALLINT` | SÍ | IDX | Motivo | Enum |
| `si_excluir_escaneo` | `BOOLEAN` | NO | IDX | Excluir sync | Cancelado/anulado/refinanciado |
| `fh_ult_sync_ingresos` | `DATETIME(6)` | SÍ | IDX | Última sync | Integración |
| `si_vigente` | `BOOLEAN` | NO | IDX | Plan vigente | Indica plan operativo vigente asociado a la obligación |
| `obligacion_pago_id_vigente` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_vigente = true THEN obligacion_pago_id ELSE NULL END` |
### 8.5 Tabla `fal_acta_pago_movimiento`

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Movimiento | PK técnica |
| `obligacion_pago_id` | `BIGINT` | NO | FK+IDX | Obligación | Padre |
| `forma_pago_id` | `BIGINT` | SÍ | FK+IDX | Forma | Si aplica |
| `plan_pago_ref_id` | `BIGINT` | SÍ | FK+IDX | Plan | Si aplica |
| `tipo_movimiento` | `SMALLINT` | NO | IDX | Movimiento | Pago procesado, confirmado, anulado, mora, caída, etc. |
| `nro_cuota` | `SMALLINT` | SÍ | IDX | Número cuota | Si movimiento de cuota |
| `importe_capital` | `DECIMAL(14,2)` | SÍ | — | Capital | Si informado |
| `importe_rima` | `DECIMAL(14,2)` | SÍ | — | RIMA/interés | Si informado |
| `importe_total` | `DECIMAL(14,2)` | SÍ | — | Total | Si informado |
| `tipo_vencimiento_pago` | `SMALLINT` | SÍ | IDX | Vencimiento | Primero, segundo, fuera, no aplica |
| `Cmte_EM` | `CHAR(2)` | SÍ | IDX | Comprobante deuda/emisión | Referencia ingresos |
| `Pref_EM` | `SMALLINT` | SÍ | IDX | Prefijo emisión | Referencia ingresos |
| `Nro_EM` | `INT` | SÍ | IDX | Número emisión | Referencia ingresos |
| `Cmte_PG` | `CHAR(2)` | SÍ | IDX | Comprobante pago/recibo | Referencia ingresos |
| `Pref_PG` | `SMALLINT` | SÍ | IDX | Prefijo pago | Referencia ingresos |
| `Nro_PG` | `INT` | SÍ | IDX | Número pago | Referencia ingresos |
| `id_cierre` | `BIGINT` | SÍ | IDX | Cierre caja/Tesorería | Si informado |
| `id_ope` | `BIGINT` | SÍ | IDX | Operación pago | Si informado |
| `movimiento_anulado_id` | `BIGINT` | SÍ | FK+IDX | Movimiento anulado | Para reversos/contracargos |
| `motivo_anulacion_pago` | `SMALLINT` | SÍ | IDX | Motivo anulación | CONTRACARGO, ANULACION_TESORERIA, etc. |
| `fh_pago_procesado` | `DATETIME(6)` | SÍ | IDX | Pago procesado | Si aplica |
| `fh_pago_confirmado` | `DATETIME(6)` | SÍ | IDX | Pago confirmado | Si aplica |
| `referencia_externa` | `VARCHAR(80)` | SÍ | IDX | Ref externa | Idempotencia/integración |
| `fh_movimiento` | `DATETIME(6)` | NO | IDX | Fecha movimiento | Funcional |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | SÍ | — | Usuario/proceso | Puede ser integración |

Reglas:

- Pago procesado no cierra.
- Pago confirmado por Tesorería puede habilitar cierre.
- Confirmación no es irreversible: puede haber anulación, reverso o contracargo.
- Anulación no borra movimiento original; genera nuevo movimiento.
- Si anula pago confirmado que cancelaba obligación, se recalculan caches y cerrabilidad.

### 8.6 Endpoint conceptual de integración

Endpoint:

    POST /api/faltas/pagos/notificar-movimiento

Vías posibles:

- proceso nocturno/programado;
- Tesorería en tiempo real;
- Caja;
- plataforma externa;
- integración interna de Ingresos;
- reproceso técnico/manual controlado.

Reglas:

- Debe ser idempotente.
- No debe duplicar movimientos.
- Resuelve obligación/forma/plan por referencias estructuradas.
- Registra `fal_acta_pago_movimiento`.
- Actualiza caches operativos.
- Usa `Cmte_EM`, `Pref_EM`, `Nro_EM` para deuda/emisión.
- Usa `Cmte_PG`, `Pref_PG`, `Nro_PG` para pago/recibo.
- Acepta anulación de pagos procesados y confirmados.

---

## 9. Talonarios y numeración

### 9.1 Regla general

La numeración de actas y documentos no es un campo manual aislado. Sale de política + talonario + ámbito de uso + movimiento de numeración.

Cadena conceptual:

    num_politica
    → num_talonario
    → num_talonario_ambito
    → dependencia / tipo documental / inspector si manual físico
    → próximo número disponible
    → fal_acta.nro_acta / fal_documento.nro_docu
    → num_talonario_movimiento

Reglas:

- La numeración de actas sale del talonario asignado a la dependencia que labra.
- Como cada dependencia define un único `tipo_acta`, el `tipo_acta` se usa como validación/contexto, pero la regla base de actas es dependencia → talonario.
- La numeración documental puede ser transversal: un talonario puede aplicar a uno o varios tipos de documento, a una dependencia, a varias dependencias o a todas.
- Ejemplo: fallos pueden usar un talonario documental transversal de Dirección de Faltas, no necesariamente el talonario de la dependencia que labró.
- Si la política define reinicio anual, al cambiar de año debe existir/activarse un nuevo talonario para el nuevo período.
- Si la política no define reinicio anual, el talonario sigue usándose hasta agotarse, bloquearse, desactivarse o ser reemplazado por decisión operativa.
- Todo talonario tiene `si_activo`. Solo talonarios activos, no bloqueados y con ámbito vigente pueden numerar.
- Todo número asignado debe registrar movimiento.
- Los talonarios manuales físicos son más estrictos: se entregan a inspectores y no pueden tener faltantes sin justificación.

### 9.2 Tabla `num_politica`

Define composición del número visible y comportamiento de reinicio.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Política | PK técnica |
| `codigo` | `VARCHAR(12)` | NO | UNIQUE | Código política | Código corto |
| `descripcion` | `VARCHAR(120)` | NO | — | Descripción | Visible |
| `clase_numeracion` | `SMALLINT` | NO | IDX | Clase numerada | ACTA, DOCUMENTO |
| `si_reinicio_anual` | `BOOLEAN` | NO | IDX | Reinicia anualmente | Si true, requiere talonario por año |
| `si_incluye_prefijo` | `BOOLEAN` | NO | — | Usa prefijo | Composición |
| `prefijo` | `VARCHAR(10)` | SÍ | — | Prefijo | Si aplica |
| `si_incluye_anio` | `BOOLEAN` | NO | — | Incluye año | Composición |
| `formato_anio` | `SMALLINT` | SÍ | — | Formato año | 2 o 4 dígitos |
| `si_incluye_serie` | `BOOLEAN` | NO | — | Incluye serie | Composición |
| `longitud_nro` | `SMALLINT` | SÍ | — | Longitud número | Relleno con ceros si aplica |
| `formato_visible` | `VARCHAR(60)` | NO | — | Formato visible | Ej. `{PREF}-{ANIO}-{SERIE}-{NRO}` |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | Control operativo |
| `fh_vig_desde` | `DATE` | NO | IDX | Inicio vigencia | Vigencia |
| `fh_vig_hasta` | `DATE` | SÍ | IDX | Fin vigencia | NULL si vigente |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Catálogo `clase_numeracion`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ACTA` | Numeración de actas |
| 2 | `DOCUMENTO` | Numeración de documentos |

### 9.3 Tabla `num_talonario`

Talonario concreto. Puede ser electrónico o manual físico.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Talonario | PK técnica |
| `version_row` | `INT` | NO | — | Versión de fila | Concurrencia optimista. Default `0`; incrementar en cada actualización del agregado |
| `politica_id` | `BIGINT` | NO | FK+IDX | Política | Define formato y reinicio |
| `codigo` | `VARCHAR(12)` | NO | UNIQUE | Código talonario | Código corto |
| `descripcion` | `VARCHAR(120)` | NO | — | Descripción | Nombre legible |
| `tipo_talonario` | `SMALLINT` | NO | IDX | Tipo | ELECTRONICO, MANUAL_FISICO |
| `clase_talonario` | `SMALLINT` | NO | IDX | Clase | ACTA o DOCUMENTO |
| `anio` | `SMALLINT` | SÍ | IDX | Año asociado | Obligatorio si política reinicia anual |
| `serie` | `VARCHAR(12)` | SÍ | IDX | Serie | Si aplica |
| `nro_desde` | `INT` | NO | — | Número inicial | Rango |
| `nro_hasta` | `INT` | SÍ | — | Número final | NULL si no tiene límite operativo |
| `nombre_secuencia` | `VARCHAR(64)` | NO | UNIQUE | Nombre secuencia MariaDB | Nombre del objeto `SEQUENCE` real asociado al talonario |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | Solo activos pueden numerar |
| `si_bloqueado` | `BOOLEAN` | NO | IDX | Bloqueado | Si true no puede usarse |
| `cod_desbloqueo` | `VARCHAR(64)` | SÍ | IDX | Código/token desbloqueo | Si aplica |
| `obs_talonario` | `VARCHAR(255)` | SÍ | — | Observación breve | No TEXT |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Reglas de secuencia:

- Cada talonario electrónico debe tener un objeto `SEQUENCE` propio en MariaDB.
- `nombre_secuencia` debe ser único y debe coincidir exactamente con el nombre real del objeto `SEQUENCE`.
- `nombre_secuencia` no debe reutilizarse para otro talonario.
- `num_talonario` no guarda `ultimo_nro_usado`.
- El backend no debe calcular el próximo número con `MAX(nro_talonario)`, con `COUNT(*)`, con locks manuales ni con una columna de último número usado.
- El próximo número se obtiene exclusivamente con `NEXT VALUE FOR nombre_secuencia` o una llamada equivalente validada por backend.
- Las secuencias de talonarios deben ser crecientes, sin ciclo y sin reutilización de números.
- No usar `CYCLE` para talonarios de actas/documentos.
- Para reducir saltos por cache descartado, la secuencia de talonarios debe crearse con `NOCACHE`, salvo decisión técnica explícita y documentada.
- MariaDB reserva el valor obtenido por `NEXT VALUE FOR`; ese valor no debe reutilizarse aunque falle una operación posterior.
- Por eso el sistema debe distinguir entre:
  - salto físico posible de una secuencia;
  - hueco lógico no permitido en la rendición/control del talonario.
- La regla de negocio no exige reutilizar números; exige que todo número reservado quede usado, anulado, devuelto o justificado.

### 9.3.1 Creación de secuencias MariaDB para talonarios

La secuencia se crea al crear/habilitar un talonario electrónico nuevo.

Cada talonario electrónico debe tener una secuencia propia. No se comparte una misma secuencia entre talonarios, dependencias, años o clases documentales distintas.

Nombre recomendado:

    seq_fal_tal_{anio}_{clase}_{codigo}

Ejemplos:

    seq_fal_tal_2026_acta_transito
    seq_fal_tal_2026_doc_fallo
    seq_fal_tal_global_doc_notificacion

Reglas de nombre:

- Debe ser estable.
- Debe ser único en el schema.
- Debe guardarse en `num_talonario.nombre_secuencia`.
- Debe ser generado/controlado por backend o migración, no ingresado libremente por usuario.
- Debe validarse contra una lista segura de caracteres antes de ejecutar SQL dinámico.
- No concatenar directamente entrada de usuario para ejecutar `NEXT VALUE FOR`.

DDL base recomendado:

    CREATE SEQUENCE seq_fal_tal_2026_acta_transito
      START WITH 1
      INCREMENT BY 1
      NOCACHE
      NOCYCLE;

Si el talonario comienza en otro número:

    CREATE SEQUENCE seq_fal_tal_2026_acta_transito
      START WITH 100001
      INCREMENT BY 1
      NOCACHE
      NOCYCLE;

Reglas:

- `START WITH` debe coincidir con `num_talonario.nro_desde`.
- `INCREMENT BY` debe ser `1`.
- `NOCACHE` es la configuración recomendada para talonarios porque privilegia control/auditoría por sobre performance.
- `NOCYCLE` es obligatorio para evitar reutilización de números.
- Si `num_talonario.nro_hasta` no es NULL, el backend debe validar el valor obtenido antes de asignarlo.
- Si el número obtenido supera `nro_hasta`, el talonario debe bloquearse/agotarse y no debe emitirse acta/documento con ese número.

### 9.3.2 Asignación segura de número

La asignación de número debe hacerse con una operación corta y controlada.

Flujo recomendado:

1. Seleccionar el talonario activo aplicable.
2. Leer `num_talonario.nombre_secuencia`.
3. Obtener próximo número con:

       SELECT NEXT VALUE FOR seq_fal_tal_2026_acta_transito;

4. Validar rango contra `nro_desde` / `nro_hasta`.
5. Insertar inmediatamente la fila única en `num_talonario_movimiento` para `id_talonario + nro_talonario`.
6. Estado inicial sugerido: `RESERVADO` o `USADO`, según el circuito.
7. Persistir el acta/documento con `id_talonario`, `nro_talonario_usado` y número visible compuesto.
8. Si la operación funcional falla después de reservar número, actualizar el movimiento a `ANULADO` o estado equivalente con motivo obligatorio.

Reglas críticas:

- El número reservado no se devuelve a la secuencia.
- El número reservado no se reutiliza.
- No se debe intentar hacer rollback lógico de una secuencia.
- Si ocurre un error después de `NEXT VALUE FOR`, debe quedar trazabilidad en `num_talonario_movimiento`.
- Si el proceso cae entre obtener el número y registrar el movimiento, debe existir monitoreo/reconciliación operativa para detectar diferencias entre último valor de secuencia y movimientos registrados.
- Para evitar huecos lógicos, la reserva del número y el alta de `num_talonario_movimiento` deben ejecutarse en una transacción corta, antes de procesos largos como generación PDF, firma, notificación o integraciones externas.

### 9.3.3 Cuándo se puede alterar una secuencia

Alterar una secuencia de talonario es una operación excepcional y auditada.

Permitido:

- Antes de que el talonario tenga números asignados.
- Durante una corrección de parametrización previa a la puesta en producción.
- Para avanzar la secuencia si quedó por debajo de números ya registrados por migración, importación o carga inicial.
- Para corregir un `START WITH` mal definido antes de uso real.
- Para cambiar configuración técnica de cache si DBA/Dirección lo aprueban explícitamente.
- Para reiniciar un talonario nuevo sin uso real durante ambiente de prueba o carga inicial controlada.

No permitido:

- Retroceder la secuencia para reutilizar números.
- Usar `RESTART WITH` hacia un número ya usado, reservado, anulado o devuelto.
- Activar `CYCLE`.
- Cambiar `INCREMENT BY` a un valor distinto de `1`.
- Alterar una secuencia activa para saltear una rendición pendiente.
- Alterar una secuencia para “tapar” huecos sin registrar anulación/justificación.
- Compartir una secuencia entre dos talonarios para corregir una mala parametrización.

### 9.3.4 Cómo alterar una secuencia

Antes de alterar:

1. Bloquear operativamente el talonario o impedir nuevas numeraciones mientras dura el cambio.
2. Verificar el último número registrado en `num_talonario_movimiento`.
3. Verificar rango `nro_desde` / `nro_hasta` del talonario.
4. Verificar el estado actual de la secuencia con `SHOW CREATE SEQUENCE`.
5. Registrar evento/auditoría administrativa del cambio.

Consulta de control:

    SHOW CREATE SEQUENCE seq_fal_tal_2026_acta_transito;

Ejemplo permitido — avanzar secuencia por migración/importación:

    ALTER SEQUENCE seq_fal_tal_2026_acta_transito
      RESTART WITH 10500;

Reglas para `RESTART WITH`:

- El nuevo valor debe ser mayor que todo `nro_talonario` existente para ese `id_talonario`.
- El nuevo valor debe estar dentro del rango del talonario, si existe `nro_hasta`.
- Debe quedar evento/auditoría del motivo.
- Debe ejecutarse con el talonario bloqueado o sin emisión concurrente.

Ejemplo permitido — cambiar cache por decisión DBA:

    ALTER SEQUENCE seq_fal_tal_2026_acta_transito
      NOCACHE;

Ejemplo no permitido:

    ALTER SEQUENCE seq_fal_tal_2026_acta_transito
      RESTART WITH 1;

Motivo:

- Si ya se emitieron o reservaron números, reiniciar a `1` permitiría duplicar o reutilizar numeración.

### 9.3.5 Reglas de agotamiento y cambio de talonario

Si `nro_hasta` está definido y la secuencia entrega un número mayor a ese límite:

1. No emitir acta/documento.
2. Marcar el talonario como agotado/bloqueado según catálogo operativo.
3. Registrar evento técnico/administrativo.
4. Activar o crear el siguiente talonario según política.
5. Crear una nueva secuencia para el nuevo talonario.
6. No alterar la secuencia vieja para extenderla salvo decisión administrativa explícita.

Extender rango de un talonario existente solo se permite si:

- No contradice la política de numeración.
- No invade rango de otro talonario.
- No afecta talonarios físicos ya impresos/entregados.
- Queda registrado administrativamente.
- Se actualiza `num_talonario.nro_hasta`.
- La secuencia no necesita retroceder.

### 9.3.6 Auditoría mínima de cambios sobre secuencias

Todo cambio sobre una secuencia de talonario debe registrar, como mínimo:

- talonario afectado;
- nombre de secuencia;
- usuario/DBA/proceso que ejecutó el cambio;
- fecha/hora;
- motivo;
- valor anterior conocido;
- valor nuevo;
- sentencia ejecutada o referencia de change request;
- autorización funcional/técnica si corresponde.

Esta auditoría puede registrarse inicialmente en `fal_acta_evento` si el cambio está asociado a un acta, o en una auditoría administrativa de numeración/talonarios si es global.

Catálogo `tipo_talonario`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ELECTRONICO` | Secuencia administrada por sistema |
| 2 | `MANUAL_FISICO` | Talonario físico entregado a inspector |
### 9.4 Tabla `num_talonario_ambito`

Define dónde aplica un talonario. Es la regla que autoriza qué talonario se usa para actas o documentos.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Ámbito | PK técnica |
| `talonario_id` | `BIGINT` | NO | FK+IDX | Talonario | Talonario habilitado |
| `clase_talonario` | `SMALLINT` | NO | IDX | Clase | ACTA o DOCUMENTO |
| `tipo_docu` | `SMALLINT` | SÍ | IDX | Tipo documento | Obligatorio si clase DOCUMENTO |
| `tipo_acta` | `SMALLINT` | SÍ | IDX | Tipo acta | Opcional; normalmente derivado por dependencia |
| `id_dep` | `BIGINT` | SÍ | IDX | Dependencia | NULL si aplica a todas |
| `ver_dep` | `SMALLINT` | SÍ | — | Versión dependencia | Obligatoria si informa `id_dep` |
| `alcance` | `SMALLINT` | NO | IDX | Alcance | GLOBAL, DEPENDENCIA, TRANSVERSAL_DOCUMENTO |
| `prioridad` | `SMALLINT` | NO | IDX | Prioridad | Resuelve múltiples reglas aplicables |
| `fh_desde` | `DATE` | NO | IDX | Vigencia desde | Inicio |
| `fh_hasta` | `DATE` | SÍ | IDX | Vigencia hasta | NULL si vigente |
| `si_activo` | `BOOLEAN` | NO | IDX | Activo | Solo reglas activas aplican |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta técnica | Auditoría |
| `id_user_alta` | `CHAR(36)` | NO | — | Usuario alta | Auditoría |

Catálogo `alcance`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `GLOBAL` | Aplica a todas las dependencias dentro de su clase/tipo documental |
| 2 | `DEPENDENCIA` | Aplica solo a una dependencia |
| 3 | `TRANSVERSAL_DOCUMENTO` | Aplica a un tipo documental transversal a varias áreas |

Regla:

- `fal_documento.id_talonario` no define la regla de numeración: solo registra el talonario efectivamente usado.
- La autorización de qué talonario corresponde a cada tipo documental vive en `num_talonario_ambito`.
- Para actas, el ámbito activo de clase `ACTA` debe resolver por dependencia.

### 9.5 Tabla `num_talonario_inspector`

Asignación/rendición de talonarios manuales físicos a inspectores. Se usa cuando el inspector no posee dispositivo de captura, no tiene conectividad operativa o el dispositivo se rompe/no está disponible.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Asignación | PK técnica |
| `id_talonario` | `BIGINT` | NO | FK+IDX | Talonario manual | Debe ser `MANUAL_FISICO` |
| `id_insp` | `BIGINT` | NO | FK+IDX | Inspector asignado | Receptor del talonario físico |
| `ver_insp` | `SMALLINT` | NO | — | Versión inspector | Histórica |
| `fh_entrega` | `DATETIME(6)` | NO | IDX | Fecha entrega | Momento de entrega al inspector |
| `id_user_entrega` | `CHAR(36)` | NO | IDX | Usuario entrega | Responsable administrativo |
| `fh_devolucion` | `DATETIME(6)` | SÍ | IDX | Fecha devolución | Cuando rinde/devuelve |
| `id_user_devolucion` | `CHAR(36)` | SÍ | IDX | Usuario devolución | Responsable que recibe |
| `estado_asignacion` | `SMALLINT` | NO | IDX | Estado asignación | ENTREGADO, DEVUELTO, CERRADO, OBSERVADO |
| `si_activa` | `BOOLEAN` | NO | IDX | Activa | Solo una asignación activa por talonario manual |
| `talonario_id_activo` | `BIGINT` | SÍ | UNIQUE AUX | Columna generada | `CASE WHEN si_activa = true THEN talonario_id ELSE NULL END` |

### Catalogo `estado_asignacion_talonario`

| ID | Codigo | Descripcion |
|---:|---|---|
| 1 | `ENTREGADO` | Talonario manual fisico entregado al inspector y asignacion activa |
| 2 | `DEVUELTO` | Talonario manual fisico devuelto por el inspector |
| 3 | `CERRADO` | Asignacion cerrada administrativamente |
| 4 | `OBSERVADO` | Asignacion observada por inconsistencia, rendicion pendiente o control administrativo |

Reglas:

- `ENTREGADO` = 1: asignacion activa inicial. Al crear, `estado_asignacion = 1`.
- `DEVUELTO` = 2: devolucion simple. Al devolver, `estado_asignacion = 2`, `si_activa = false`.
- `CERRADO` = 3: cierre administrativo con rendicion completa. `si_activa = false`.
- `OBSERVADO` = 4: asignacion con observacion administrativa pendiente de resolucion.
- No usar strings libres. El campo es `SMALLINT NOT NULL`.
- Solo puede existir una asignacion activa (`si_activa = true`) por talonario manual fisico, controlado por `talonario_id_activo` (columna generada con UNIQUE).
### 9.6 Tabla `num_talonario_movimiento`

Registro operativo único de cada número de talonario.

Aunque la tabla conserva el nombre `movimiento`, funcionalmente representa el estado final/controlado de cada número dentro de un talonario. No debe haber más de una fila para el mismo `id_talonario + nro_talonario`.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Registro de número | PK técnica |
| `id_talonario` | `BIGINT` | NO | FK+IDX | Talonario | Talonario afectado |
| `nro_talonario` | `INT` | NO | IDX | Número afectado | Número dentro del talonario |
| `estado_numero` | `SMALLINT` | NO | IDX | Estado del número | USADO, ANULADO, DEVUELTO_SIN_USAR, RENDIDO, JUSTIFICADO |
| `motivo_anulacion` | `SMALLINT` | SÍ | IDX | Motivo anulación | Obligatorio si `estado_numero = ANULADO` |
| `observacion` | `VARCHAR(500)` | SÍ | — | Observación | Detalle administrativo |
| `acta_id` | `BIGINT` | SÍ | FK+IDX | Acta asociada | Obligatorio si fue usado para acta |
| `documento_id` | `BIGINT` | SÍ | FK+IDX | Documento asociado | Obligatorio si fue usado para documento |
| `id_dep` | `BIGINT` | SÍ | IDX | Dependencia | Contexto de uso |
| `ver_dep` | `SMALLINT` | SÍ | — | Versión dependencia | Contexto |
| `id_insp` | `BIGINT` | SÍ | IDX | Inspector | Contexto manual |
| `ver_insp` | `SMALLINT` | SÍ | — | Versión inspector | Contexto manual |
| `fh_movimiento` | `DATETIME(6)` | NO | IDX | Fecha movimiento/estado | Funcional |
| `id_user_movimiento` | `CHAR(36)` | NO | IDX | Usuario movimiento | Trazabilidad |

Índices/reglas:

- `UNIQUE KEY uk_num_talonario_numero (id_talonario, nro_talonario)`
- No puede existir más de una fila para el mismo número dentro del mismo talonario.
- Cada número debe quedar con un estado final/controlado: usado, anulado, devuelto sin usar, rendido o justificado.
- No puede haber huecos sin registro para talonarios manuales físicos al momento de rendición/cierre.
- Si el número fue usado para acta, `acta_id` es obligatorio y `documento_id` debe quedar NULL.
- Si el número fue usado para documento, `documento_id` es obligatorio y `acta_id` debe quedar NULL.
- Si `estado_numero = ANULADO`, `motivo_anulacion` es obligatorio.
- Si se necesita historial fino de cambios de estado, se debe agregar una tabla hija histórica, por ejemplo `num_talonario_movimiento_hist`, sin eliminar la regla de fila única operativa por número.

Catálogo `estado_numero_talonario`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `USADO` | Número usado para labrar acta/documento |
| 2 | `ANULADO` | Número anulado con motivo obligatorio |
| 3 | `DEVUELTO_SIN_USAR` | Número devuelto sin uso al rendir talonario |
| 4 | `RENDIDO` | Número rendido/cerrado administrativamente |
| 5 | `JUSTIFICADO` | Número no usado pero justificado formalmente |

Catálogo `motivo_anulacion_talonario`:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `ERROR_LABRADO` | Error al completar el acta manual |
| 2 | `ROTURA_FORMULARIO` | Formulario dañado/ilegible |
| 3 | `DUPLICADO` | Número duplicado o inconsistente |
| 4 | `EXTRAVIO` | Hoja/formulario extraviado |
| 5 | `OTRO` | Otro motivo documentado |

### 9.7 Reglas de selección y control

Actas:

1. Se identifica la dependencia vigente del inspector.
2. La dependencia determina el `tipo_acta`.
3. Se busca talonario activo clase `ACTA` aplicable a esa dependencia.
4. Si la política tiene `si_reinicio_anual = true`, se usa el talonario activo del año actual.
5. Si no existe talonario activo para el año actual, debe generarse/habilitarse uno nuevo según política.
6. Si la política no reinicia anual, se sigue usando el talonario activo hasta cambio operativo.
7. Se asigna próximo número.
8. Se compone `nro_acta`.
9. Se guarda `fal_acta.id_talonario`, `fal_acta.nro_talonario_usado`, `fal_acta.nro_acta`.
10. Se registra movimiento.

Documentos:

1. Se identifica `tipo_docu`.
2. Se identifica contexto: acta, dependencia, tipo de acta si aplica.
3. Se busca talonario activo clase `DOCUMENTO`.
4. Se prioriza regla más específica: `tipo_docu + dependencia`, `tipo_docu + tipo_acta`, `tipo_docu transversal`, `tipo_docu global`.
5. Se valida política/reinicio anual.
6. Se asigna número.
7. Se guarda `fal_documento.id_talonario`, `fal_documento.nro_talonario_usado`, `fal_documento.nro_docu`.
8. Se registra movimiento.

Talonarios manuales físicos:

- Se entregan a inspectores.
- Se usan como contingencia cuando no hay dispositivo, no hay conectividad operativa o el dispositivo se rompe/no está disponible.
- Deben tener rango `nro_desde` / `nro_hasta`.
- No puede haber números faltantes sin movimiento.
- Cada número del rango debe quedar usado, anulado con motivo, devuelto sin usar o pendiente si el talonario sigue entregado y activo.
- Un número anulado exige `motivo_anulacion`.
- Un talonario manual físico no puede cerrarse/rendirse si existen números intermedios sin movimiento.

---

## 10. Storage documental

### 10.1 Criterio

No guardar binarios en base. Documentos/evidencias referencian storage mediante `storage_key`.

Tablas mínimas sugeridas:

- `stor_backend`
- `stor_politica`
- `stor_objeto`

`stor_objeto` puede usar `storage_key VARCHAR(255)` como PK natural si el storage lo requiere; si se prefiere uniformidad, usar `id BIGINT` + `storage_key UNIQUE`.

---

## 11. Portal y QR

### 11.1 QR en `fal_acta`

El QR del acta permite identificar y consultar el acta desde canales digitales controlados, como portal infractor, verificación pública restringida o aplicaciones internas.

El QR no debe contener datos personales, datos del infractor, domicilio, infracción, montos ni información sensible en claro.

El QR debe contener un token protegido/cifrado, generado por backend, que permita resolver el acta de forma segura.

Campos definitivos en `fal_acta`:

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `codigo_qr` | `VARCHAR(512)` | NO | UNIQUE | Código QR principal | Token protegido/cifrado |
| `qr_payload_version` | `SMALLINT` | NO | — | Versión payload | Inicialmente `0` |

Reglas:

- `codigo_qr` vive en `fal_acta`.
- `codigo_qr` es único.
- `codigo_qr` no debe ser el `id` de la tabla.
- `codigo_qr` no debe ser el `nro_acta` en claro.
- `codigo_qr` no debe contener datos personales en claro.
- `codigo_qr` no debe contener domicilio, documento, patente, infracción, monto ni datos sensibles en claro.
- El QR debe contener un token protegido/cifrado generado por backend.
- El token debe poder resolverse en backend para identificar el acta.
- El payload interno del token debe estar versionado mediante `qr_payload_version`.
- `qr_payload_version` inicia en `0`.
- Si cambia el formato del payload en el futuro, se incrementa la versión.
- `codigo_qr` debe tener longitud mínima `VARCHAR(512)`.
- Si Seguridad define tokens más extensos, ampliar a `VARCHAR(1024)`.
- No agregar `codigo_qr_hash` salvo que Seguridad lo requiera explícitamente.
- La validación de integridad debe resolverse por el propio token protegido/cifrado o por backend.

### 11.2 Contenido conceptual del QR

El QR impreso o visible en el acta debe representar un token seguro.

Conceptualmente, el QR puede resolver a una URL o a un token interpretable por backend.

Formato recomendado:

    https://portal.municipio.gob.ar/faltas/qr/{codigo_qr}

O, si se usa solo token dentro del QR:

    {codigo_qr}

La opción recomendada para uso ciudadano es URL + token, porque permite escanear desde un celular y abrir el portal correspondiente.

Ejemplo conceptual:

    https://portal.municipio.gob.ar/faltas/qr/eyJhbGciOiJ...token-protegido...

Reglas:

- La URL puede ser pública, pero el token no debe exponer información en claro.
- El backend debe validar el token antes de mostrar cualquier información.
- El portal debe mostrar solo la información permitida según estado del acta, autenticación y reglas del circuito.
- El hecho de tener el QR no habilita acceso irrestricto a datos sensibles.
- Si el acta está en revisión o no está habilitada para portal, el QR debe resolver a un mensaje controlado.
- Si el documento está firmado/notificable, el portal puede mostrarlo según reglas del prototipo.
- Si el infractor abre una pieza notificable por portal, puede registrarse notificación positiva según reglas ya validadas.

### 11.3 Payload interno del QR

El payload interno es la información que backend cifra/protege antes de generar `codigo_qr`.

El payload puede contener datos técnicos mínimos para resolver el acta.

Payload versión `0` recomendado:

| Campo lógico | Descripción | Regla |
|---|---|---|
| `v` | Versión del payload | Igual a `qr_payload_version`, inicialmente `0` |
| `acta_id_tecnico` | UUID técnico estable del acta | No usar ID incremental como único dato |
| `nro_acta` | Número visible del acta | Opcional, solo dentro del token protegido |
| `iat` | Fecha/hora de emisión del token | Para control técnico |
| `nonce` | Valor aleatorio | Evita tokens predecibles |
| `scope` | Alcance del token | Ej. `ACTA_QR` |

Ejemplo conceptual del payload antes de cifrar/proteger:

    {
      "v": 0,
      "acta_id_tecnico": "550e8400-e29b-41d4-a716-446655440000",
      "nro_acta": "A-2026-000123",
      "iat": "2026-06-16T10:30:00-03:00",
      "nonce": "valor-aleatorio-seguro",
      "scope": "ACTA_QR"
    }

Reglas:

- Este payload es conceptual.
- No se guarda como JSON en la base.
- No se imprime en claro.
- No se expone al usuario.
- No se almacena en `fal_acta` como payload.
- Solo se guarda el resultado protegido/cifrado en `codigo_qr`.
- El backend debe poder validar y resolver el token.
- El payload debe mantenerse mínimo.
- No incluir datos personales.
- No incluir domicilio.
- No incluir datos del vehículo.
- No incluir infracciones/artículos.
- No incluir montos.
- No incluir estado actual del acta, porque puede cambiar.
- El estado se consulta en backend al momento de resolver el QR.

### 11.4 Generación del QR

Flujo recomendado de generación:

1. Se crea el acta.
2. Se genera o recibe `id_tecnico` del acta.
3. Se asigna número visible si corresponde al momento del labrado.
4. Backend arma el payload mínimo versión `0`.
5. Backend agrega `iat`, `nonce` y `scope`.
6. Backend protege/cifra/firma el payload.
7. El resultado se guarda en `fal_acta.codigo_qr`.
8. Se guarda `fal_acta.qr_payload_version = 0`.
9. Se genera la imagen QR a partir de la URL/token.
10. El QR se imprime o se incorpora en el documento/acta correspondiente.

Pseudoflujo:

    payload mínimo
    → serialización canónica
    → cifrado/firma/protección
    → token
    → fal_acta.codigo_qr
    → URL de portal con token
    → imagen QR

Reglas:

- La generación del QR debe ocurrir en backend.
- El frontend o dispositivo no debe inventar el token final si no tiene claves seguras.
- Si el acta nace offline, el dispositivo puede generar `id_tecnico`, pero el `codigo_qr` final debe confirmarse/generarse al sincronizar con backend.
- Si se requiere QR offline, debe existir una política específica de tokens offline firmados o preemitidos.
- El token debe ser no predecible.
- El token debe ser único.
- El token debe estar protegido contra manipulación.
- El backend debe rechazar tokens inválidos, vencidos si aplica, mal firmados o con scope incorrecto.
- La imagen QR no es fuente de verdad; la fuente de verdad es `fal_acta.codigo_qr`.

### 11.5 Validación del QR

Al escanear el QR:

1. El usuario abre la URL del portal con `codigo_qr`.
2. Backend busca/valida el token.
3. Backend resuelve el acta asociada.
4. Backend consulta el estado real actual del acta.
5. Backend aplica reglas de visibilidad.
6. Backend muestra solo la información habilitada.

Reglas:

- No se debe confiar en datos decodificados del cliente.
- Toda resolución del QR debe pasar por backend.
- El backend debe validar integridad del token.
- El backend debe validar `scope`.
- El backend debe validar versión del payload.
- El backend debe resolver el acta y consultar estado actual.
- La información visible se determina por reglas del portal, no por el QR.
- Si el acta no existe, está anulada, no está disponible o el token es inválido, se muestra mensaje controlado.
- El mensaje de error no debe revelar información sensible.

### 11.6 Relación con portal infractor

El QR puede ser una puerta de entrada al portal infractor, pero no reemplaza las reglas del portal.

Reglas ya validadas del prototipo:

- Documentos firmados pueden ser visibles en portal según estado.
- Al abrir documento notificable, puede registrarse notificación positiva por canal `PORTAL_INFRACTOR`.
- Una notificación postal de la misma pieza puede quedar sin efecto/superada por portal según el estado exacto del prototipo.
- Actas en revisión no deben exponer documentación no validada.
- El pago voluntario en portal puede estar bloqueado si existe fallo condenatorio firmado sin notificar.
- El monto visible en portal debe seguir la regla de tarifario vivo hasta confirmación, congelamiento o materialización.

### 11.7 Decisiones tomadas

- El QR vive en `fal_acta`.
- Se usa un único campo `codigo_qr`.
- No se agrega `codigo_qr_hash`.
- `codigo_qr` es `VARCHAR(512)` y `UNIQUE`.
- `qr_payload_version` inicia en `0`.
- El QR contiene token protegido/cifrado, no datos en claro.
- El payload interno es mínimo y técnico.
- No se guardan datos personales ni datos sensibles dentro del QR.
- No se guarda `payload_json` en la base.
- El estado actual del acta se consulta en backend al resolver el QR.
- El QR no otorga acceso irrestricto; solo permite iniciar una consulta controlada.
- El documento/acta puede imprimir el QR como URL/token seguro.

### 11.8 Tabla `fal_acta_qr_acceso` — diferible

Registra accesos válidos al QR cuando el token pudo resolverse correctamente a un acta.

No registra accesos inválidos, tokens inexistentes, tokens corruptos ni intentos que no puedan asociarse a un acta.

| Campo | Tipo MariaDB | Null | Clave/índice | Descripción | Regla |
|---|---|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | Acceso | PK técnica |
| `acta_id` | `BIGINT` | NO | FK+IDX | Acta accedida | Solo accesos con QR válido y acta resuelta |
| `fh_acceso` | `DATETIME(6)` | NO | IDX | Fecha acceso | Auditoría |
| `canal_acceso` | `SMALLINT` | NO | IDX | Canal | Portal, app, integración, otro |
| `ip_origen` | `VARCHAR(45)` | SÍ | — | IP | Si corresponde |
| `user_agent` | `VARCHAR(255)` | SÍ | — | User agent | Si corresponde |
| `resultado_acceso` | `SMALLINT` | NO | IDX | Resultado | Catálogo acotado a accesos válidos/resueltos |
| `fh_alta` | `DATETIME(6)` | NO | — | Alta | Auditoría |

Reglas:

- `acta_id` es obligatorio porque solo se registran accesos válidos que resolvieron un acta.
- Si el token es inválido, corrupto, inexistente o no resoluble, no se inserta fila en esta tabla.
- No incluir estados de resultado como `INVALIDO`, `EXPIRADO`, `RECHAZADO` o `ERROR` si esos casos no se van a registrar.
- Si en el futuro Seguridad pide auditoría de accesos inválidos, debe diseñarse una tabla separada sin FK obligatoria a acta y sin exponer el token en claro.

Catálogo `resultado_acceso_qr` inicial:

| ID | Código enum | Descripción |
|---:|---|---|
| 1 | `VALIDO` | Token válido y acta resuelta |

## 12. Criterios de implementación

### 12.1 Patrón de unicidad para filas activas/vigentes en MariaDB

Cuando una tabla permite histórico pero exige una sola fila activa/vigente/final por acta o por combinación funcional, se debe usar un patrón único en todo el modelo.

Patrón recomendado para MariaDB 11.x:

- Mantener el flag funcional (`si_activa`, `si_activo`, `si_vigente`, `si_resultado_final`).
- Agregar una columna generada auxiliar nullable para la clave activa.
- La columna generada devuelve la clave de scope si la fila está activa/vigente/final.
- La columna generada devuelve `NULL` si la fila no está activa/vigente/final.
- Crear índice único sobre la columna generada y el resto del scope funcional.
- MariaDB permite múltiples `NULL` en índices únicos, por lo que solo se bloquean duplicados activos/vigentes/finales.

Ejemplo conceptual para una activa por acta:

    acta_id_activa BIGINT
      GENERATED ALWAYS AS (CASE WHEN si_activa = true THEN acta_id ELSE NULL END) VIRTUAL,
    UNIQUE KEY uk_tabla_activa (acta_id_activa)

Ejemplo conceptual para una valorización vigente por acta y tipo:

    acta_id_vigente BIGINT
      GENERATED ALWAYS AS (CASE WHEN si_vigente = true THEN acta_id ELSE NULL END) VIRTUAL,
    tipo_valorizacion_vigente SMALLINT
      GENERATED ALWAYS AS (CASE WHEN si_vigente = true THEN tipo_valorizacion_acta ELSE NULL END) VIRTUAL,
    UNIQUE KEY uk_fal_valorizacion_vigente (acta_id_vigente, tipo_valorizacion_vigente)

Aplicaciones mínimas:

| Tabla | Regla única funcional | Columna generada sugerida |
|---|---|---|
| `fal_acta_valorizacion` | una vigente por `acta_id + tipo_valorizacion_acta` | `acta_id_vigente`, `tipo_valorizacion_vigente` |
| `fal_acta_forma_pago` | una vigente por `acta_id + tipo_forma_pago` o scope definido | `acta_id_vigente`, `tipo_forma_pago_vigente` |
| `fal_acta_plan_pago_ref` | un plan vigente por acta | `acta_id_vigente` |
| `fal_acta_paralizacion` | una paralización activa por acta | `acta_id_activa` |
| `fal_acta_archivo` | un archivo activo por acta | `acta_id_activa` |
| `fal_acta_gestion_externa` | una gestión externa activa por acta | `acta_id_activa` |
| `fal_acta_transito_alcoholemia` | una medición final por acta | `acta_id_resultado_final` |
| `fal_persona_domicilio` | un domicilio principal activo por persona y tipo/scope definido | `persona_id_principal`, `tipo_domicilio_principal` |
| `fal_acta_bloqueante_cierre_material` | bloquear duplicados activos cuando el origen/scope lo requiera | scope activo según tipo/origen |

Regla:

- La aplicación debe validar en transacción.
- La base debe acompañar con índice único auxiliar cuando el caso sea crítico.
- No confiar solo en UI para evitar doble vigente/activo.

### 12.2 Fuente de verdad y proyecciones

- `fal_acta` guarda el estado actual operativo del expediente.
- Las tablas históricas guardan el detalle primario de cada ciclo o decisión específica.
- `fal_acta_snapshot` es regenerable y nunca es fuente primaria.
- Las actualizaciones de estado deben ser transaccionales y registrar evento append-only.
- Ante inconsistencias, se reconstruye snapshot desde tablas primarias y eventos.

### 12.3 Concurrencia optimista con `version_row`

Reglas:

- `version_row` inicia en `0` al insertar el agregado.
- Toda modificación funcional de una cabecera/agregado principal debe incrementar `version_row`.
- El update debe comparar la versión conocida por el proceso llamador.
- Si no se actualiza ninguna fila, hay conflicto de concurrencia.
- El conflicto no se resuelve reintentando a ciegas: se recarga el agregado, se recalculan reglas y se decide nuevamente.
- Los inserts de tablas hijas deben ocurrir dentro de la misma transacción que actualiza la cabecera cuando cambian el estado funcional del agregado.

Ejemplo conceptual:

    UPDATE fal_acta
       SET est_proc_act = ?,
           sit_adm_act = ?,
           fh_ult_mod = ?,
           id_user_ult_mod = ?,
           version_row = version_row + 1
     WHERE id = ?
       AND version_row = ?;


### 12.4 Qué no hacer antes de cerrar modelo

- No generar DDL físico si queda una decisión funcional abierta.
- No escribir repositorios definitivos sobre tablas inestables.
- No conectar persistencia real hasta cerrar tablas núcleo, satélites, normativa, valorización, pagos y numeración.
- No reintroducir `payload_json` en eventos.
- No reintroducir pagos voluntario/condena como tablas separadas grandes.
- No volver a condicionar el modelo al DDL histórico.

### 12.5 Uso del documento para implementación

Este documento queda como base del dominio y de la spec productiva.

Siguientes tareas de implementación:

1. Generar dominio final/casi final desde este modelo.
2. Generar spec productiva completa; las specs anteriores quedan históricas.
3. Implementar procesos de lógica de negocio y backend real, no mock/desconectado.
4. Generar dataset integral de casos de uso con talonarios, dependencias, actas, documentos, pagos, notificaciones, archivo, gestión externa y bloqueantes.
5. Cubrir cada caso con tests.
6. Ajustar Angular para que muestre sólo acciones válidas según motor.
7. Agregar formularios para generación controlada de datos de prueba.
8. Generar checklist/prompt QA final.
9. Iterar correcciones.
10. Pasar a MariaDB real y repetir ciclo QA final.

---

## 13. Índices recomendados mínimos

| Área | Índices |
|---|---|
| Actas | `fal_acta(nro_acta)`, `fal_acta(id_tecnico)`, `fal_acta(codigo_qr)`, `fal_acta(id_dep, fh_acta)`, `fal_acta(id_insp, fh_acta)` |
| Snapshot | `fal_acta_snapshot(cod_bandeja, si_visible_bandeja, prioridad)`, `fal_acta_snapshot(monto_operativo_vigente)`, `fal_acta_snapshot(si_monto_confirmado)` |
| Eventos | `fal_acta_evento(acta_id, fh_evt)`, `fal_acta_evento(acta_id, tipo_evt)` |
| Persona | `fal_persona(tipo_doc, nro_doc)`, `fal_persona(nombre_mostrar)`, `fal_persona(Id_Suj, Id_Bie)` |
| Domicilios | `fal_persona_domicilio(persona_id, si_activo, si_notificable)` |
| Normativa | `fal_dependencia_normativa(id_dep, ver_dep)`, `fal_articulo_normativa_faltas(normativa_id)` |
| Valorización | `fal_acta_valorizacion(acta_id, si_vigente)`, `fal_acta_valorizacion(acta_id, estado_valorizacion)`, índice único auxiliar para vigente por `acta_id + tipo_valorizacion_acta` |
| Pagos | `fal_acta_obligacion_pago(acta_id, estado_obligacion)`, `fal_acta_pago_movimiento(Cmte_PG, Pref_PG, Nro_PG)` |
| Numeración | `num_talonario(codigo)`, `num_talonario_ambito(talonario_id)`, `UNIQUE num_talonario_movimiento(id_talonario, nro_talonario)` |

---

FIN DEL DOCUMENTO.

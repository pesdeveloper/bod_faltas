# Modelo físico completo — integración EDA Faltas ↔ Firmas

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> `LOGICAL_MODEL_COMPLETE`, `PHYSICAL_MODEL_COMPLETE`, `DDL_READY`,
> `DDL_NOT_EXECUTED`, `IMPLEMENTATION_NOT_STARTED`. Extiende
> `mariadb-logical-model.md` para las tablas existentes que reciben
> columnas nuevas (`DEC-EDA-020`/`021`/`023`/`024`) y define las tablas
> nuevas del bus de eventos municipal (`DEC-EDA-022`/`026`) y de staging de
> resultados firmados (`DEC-EDA-024`). El DDL ejecutable vive en
> `database/ddl/alter-firmas-eda-closure-001-forward.sql` (aplicación),
> `database/ddl/alter-firmas-eda-closure-001-rollback.sql` (reversión) y
> `database/ddl/grants-firmas-eda-bus.sql` (cuentas SQL fijas de mínimo
> privilegio, grants directos sobre `bus_evento`, §10); los
> diagnósticos de solo lectura viven en
> `database/diagnostics/verify-firmas-eda-closure-001-precheck.sql`,
> `-postcheck.sql`, `-parity.sql` y `-rollback-precheck.sql`; el inventario
> y orden del paquete en `database/design/firmas-eda-closure-001-scope.md`.
> Ninguno de estos scripts fue ejecutado. Motor/charset/collation:
> `InnoDB` / `utf8mb4` / `utf8mb4_uca1400_ai_ci` (mismas convenciones que
> `database/ddl/create-bod-faltas-domain.sql`). Base de destino:
> `sb_faltas_db`, MariaDB 12.3.2. **Versión mínima exigida: MariaDB >= 10.7**
> (mínimo genérico del tipo `JSON` nativo y `CHECK` físicas usadas en este
> paquete; el modelo simplificado no requiere rutinas/vistas `SQL SECURITY
> DEFINER` ni la semántica de `SESSION_USER()` que exigían un mínimo mayor
> en el diseño previo); mismo mínimo en
> `database/diagnostics/verify-firmas-eda-closure-001-precheck.sql` y el
> gate. El destino real de despliegue (`12.3.2`) está estrictamente por
> encima de este mínimo.

## 1. `fal_documento_firma_req` — columnas nuevas (`DEC-EDA-020`/`030`)

Tabla existente (`LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA`,
`mariadb-logical-model.md`). Ninguna columna existente cambia de nombre,
tipo o significado. Columnas nuevas (exactamente 14: 10 de `DEC-EDA-020` +
4 de `DEC-EDA-030`), todas `ALTER TABLE ... ADD COLUMN`:

| Columna | Tipo | Nullable | Default | Contenido |
|---|---|---|---|---|
| `version_row` | `INT` | NOT NULL | `0` | OCC (la tabla no tenía OCC hasta este cierre; requerido para la regla de carrera firma/cancelación). |
| `solicitud_firma_id` | `CHAR(36)` | NULL | — | UUID de correlación EDA (`FirmaSolicitada.solicitudFirmaId`); NULL para filas anteriores a esta migración. |
| `revision_solicitud` | `SMALLINT` | NOT NULL | `0` | Revisión de la solicitud EDA; base de la resolución de carrera y de duplicados. |
| `fh_solicitud` | `DATETIME(6)` | NULL | — | Momento de publicación de `FirmaSolicitada`. |
| `fh_aceptacion` | `DATETIME(6)` | NULL | — | Momento de `SolicitudFirmaAceptada`. |
| `fh_inicio_firma` | `DATETIME(6)` | NULL | — | Momento en que Firmas confirma que comienza a procesar (informativo; no crea un estado físico nuevo). |
| `vence_en` | `DATETIME(6)` | NULL | — | UTC. Autoridad de vencimiento exclusiva de Faltas (`DEC-EDA-016` §6.4). |
| `fh_terminal` | `DATETIME(6)` | NULL | — | Momento de cualquier transición terminal (`FIRMADO`, `ANULADO`, `VENCIDO`, `REEMPLAZADO`, `CANCELADO`, `RECHAZADO`, `RECHAZADO_CONTRATO`). |
| `motivo_terminal_firma_req` | `SMALLINT` | NULL | — | Significado condicionado por `estado_firma_req` (ver `../10-domain/firma-notificacion-fallo.md`, "Lifecycle único de solicitud/requisito de firma"). |
| `detalle_terminal_firma_req` | `VARCHAR(255)` | NULL | — | Complemento descriptivo opcional; nunca única clasificación. |

### 1.1bis Preparación persistida e idempotente por requisito (`DEC-EDA-030`) — 4 columnas

**Decisión final:** `preparar-artefacto` (`../30-integrations/firma-binary-transfer.md`
§1) promete idempotencia y una revisión preparada; sin persistir qué PDF
exacto fue preparado, esa promesa no es verificable en base. Se agregan 4
columnas al requisito, exactamente los mismos 4 valores que devuelve la
preparación:

| Columna | Tipo | Nullable | Contenido |
|---|---|---|---|
| `revision_artefacto_preparada` | `INT` | NULL | `revisionArtefacto` fijada por la preparación más reciente de este requisito. |
| `hash_entrada_preparado` | `VARCHAR(128)` | NULL | Hash SHA-256 (o algoritmo declarado) del PDF exacto expuesto a Firmas en esa preparación. |
| `content_length_preparado` | `BIGINT` | NULL | Tamaño exacto del PDF expuesto en esa preparación. |
| `fh_preparacion` | `DATETIME(6)` | NULL | Momento de la preparación más reciente. |

**Reglas:**

- los cuatro campos son todos `NULL` (nunca preparado) o todos informados
  (preparado al menos una vez); nunca una combinación parcial
  (`chk_firma_req_preparacion`, ver más abajo);
- `revision_artefacto_preparada >= 1` cuando informado
  (`chk_firma_req_rev_preparada`);
- `content_length_preparado > 0` cuando informado
  (`chk_firma_req_content_length_preparado`);
- el hash persistido es siempre el del PDF exacto ya expuesto a Firmas (el
  mismo valor que la respuesta 200/201 de `preparar-artefacto` devolvió como
  `hashEntrada`);
- una misma `solicitud_firma_id` + `revision_solicitud` devuelve siempre
  estos mismos cuatro valores en preparaciones repetidas (idempotencia
  real, verificable contra la fila persistida, no solo contra la memoria
  del proceso HTTP);
- `GET .../artefacto` (`firma-binary-transfer.md` §2) descarga
  exclusivamente la revisión persistida en estas cuatro columnas del
  requisito, nunca una noción genérica de "documento vigente" resuelta de
  otra forma;
- `PUT .../resultados/{resultadoFirmaId}` (`firma-binary-transfer.md` §3)
  valida `revisionEntrada`, `hashEntrada` y tamaño contra estas cuatro
  columnas del requisito correspondiente, no contra
  `fal_documento.revision_artefacto`/`hash_docu` directamente (que pueden
  haber avanzado por otra firma concurrente sobre otro requisito del mismo
  documento);
- si `fal_documento` cambió de bytes después de esta preparación (otra
  firma aplicada sobre el mismo documento para otro requisito), el
  resultado cargado contra la preparación vieja queda inválido por
  divergencia de `revisionEntrada`/`hashEntrada` — nunca se aplica.

**Revisiones en firmas múltiples:** `fal_documento.revision_artefacto`
(§3) solo incrementa cuando cambian los bytes del PDF vigente: primera
creación/render (`0 -> 1`) o cada firma válida aplicada (`+1`). Preparar
una firma posterior sobre un PDF ya existente (mismo documento, otro
requisito de firma) **no re-renderiza, no copia el PDF y no incrementa
`revision_artefacto`**: solo registra en `revision_artefacto_preparada` la
revisión vigente en ese momento. El endpoint de preparación responde
`201 Created` en la primera creación física del PDF y `200 OK` en toda
repetición idempotente o en toda preparación que solo vincula un PDF
vigente ya existente sin re-renderizarlo (`firma-binary-transfer.md` §1).

**Constraints nuevas de esta subsección:**

```sql
CONSTRAINT chk_firma_req_preparacion CHECK (
    (revision_artefacto_preparada IS NULL AND hash_entrada_preparado IS NULL
        AND content_length_preparado IS NULL AND fh_preparacion IS NULL)
    OR (revision_artefacto_preparada IS NOT NULL AND hash_entrada_preparado IS NOT NULL
        AND content_length_preparado IS NOT NULL AND fh_preparacion IS NOT NULL)
)
CONSTRAINT chk_firma_req_rev_preparada CHECK (revision_artefacto_preparada IS NULL OR revision_artefacto_preparada >= 1)
CONSTRAINT chk_firma_req_content_length_preparado CHECK (content_length_preparado IS NULL OR content_length_preparado > 0)
```

**Backfill:** `NULL` en las 4 columnas para toda fila existente (ningún
requisito anterior a esta migración tiene una preparación EDA persistida
bajo este circuito; cumple `chk_firma_req_preparacion` por la rama
"todos NULL").

**Catálogo `estado_firma_req` extendido (aditivo, sin renombrar valores
existentes):**

```sql
-- valores existentes sin cambios: 1=PENDIENTE, 2=FIRMADO, 3=ANULADO, 4=VENCIDO, 5=REEMPLAZADO
-- valores nuevos DEC-EDA-016:
6 = CANCELACION_SOLICITADA
7 = CANCELADO
8 = RECHAZADO
9 = RECHAZADO_CONTRATO
```

**Constraints nuevas:**

```sql
CONSTRAINT chk_firma_req_estado_ext CHECK (estado_firma_req BETWEEN 1 AND 9)
CONSTRAINT chk_firma_req_revision CHECK (revision_solicitud >= 0)
```

**Índices nuevos:**

```sql
UNIQUE KEY uq_firma_req_solicitud (solicitud_firma_id)  -- NULL admitido multiple veces (MariaDB no cuenta NULL en UNIQUE)
INDEX ix_firma_req_estado_vence (estado_firma_req, vence_en)  -- barrido de vencimiento
INDEX ix_firma_req_solicitud_rev (solicitud_firma_id, revision_solicitud)
```

**Backfill (filas existentes, sin datos jurídicos que inventar):**
`version_row = 0`; `revision_solicitud = 0`; `solicitud_firma_id`,
`fh_solicitud`, `fh_aceptacion`, `fh_inicio_firma`, `vence_en`,
`fh_terminal`, `motivo_terminal_firma_req`, `detalle_terminal_firma_req` =
`NULL` (no derivables retroactivamente; no se inventa). Regla de
endurecimiento futura: `solicitud_firma_id` puede endurecerse a `NOT NULL`
únicamente cuando una decisión de implementación explícita confirme que
100% de las filas activas (`si_activa = TRUE` y `estado_firma_req`
transitorio) fueron creadas después de la adopción operativa de la
integración EDA; no se declara esa fecha en este cierre porque depende de
cuándo se active Firmas en producción.

## 2. `fal_documento_firma` — columnas nuevas (`DEC-EDA-013`/`021`/`023`)

Tabla existente (`HUMAN_DECISION_CLOSED`). `hash_firma` conserva su nombre y
semántica exacta (hash de entrada, ya vigente). Columnas nuevas (exactamente
27: 3 de §2.1 + 7 de §2.2 + 17 de §2.3):

### 2.1 Hashes y revisiones (`DEC-EDA-013`) — 3 columnas

| Columna | Tipo | Nullable | Contenido |
|---|---|---|---|
| `hash_salida` | `VARCHAR(128)` | NULL | Hash del PDF vigente después de aplicar esta firma. NULL para firmas históricas anteriores a esta migración. |
| `revision_entrada` | `SMALLINT` | NULL | Revisión del PDF vigente al momento de firmar. |
| `revision_salida` | `SMALLINT` | NULL | Revisión del PDF vigente resultante. |

### 2.2 Actor técnico (`DEC-EDA-021`) — 7 columnas

| Columna | Tipo | Nullable | Contenido |
|---|---|---|---|
| `actor_tecnico_tipo` | `SMALLINT` | NULL | Catálogo `ActorTecnicoTipo` (`1=APLICACION`). |
| `actor_tecnico_id` | `VARCHAR(64)` | NULL | Identificador técnico del consumidor/instancia (ej. `firmas-consumer-01`). |
| `actor_tecnico_app` | `VARCHAR(32)` | NULL | Aplicación origen (`firmas`). |
| `actor_tecnico_key_fingerprint` | `VARCHAR(64)` | NULL | Fingerprint (hash) de la `X-Api-Key` usada; nunca la clave completa. |
| `correlation_id` | `CHAR(36)` | NULL | Correlación EDA de punta a punta. |
| `event_id` | `CHAR(36)` | NULL | `eventoId` de `DocumentoFirmado` que originó esta fila (cuando la firma llega por evento; NULL para `firmar-real` síncrono). |
| `received_at` | `DATETIME(6)` | NULL | Momento en que Faltas recibió el resultado/evento. |

**Regla de obligatoriedad (aplicación, no CHECK físico salvo la excepción
condicional de §2.4):** para toda fila nueva cuyo origen sea el consumidor
EDA de `DocumentoFirmado`, `actor_tecnico_tipo`/`actor_tecnico_id`/
`actor_tecnico_app`/`actor_tecnico_key_fingerprint` son obligatorios a
nivel de comando de aplicación (el callback síncrono `firmar-real`
legítimamente no tiene actor técnico M2M). `id_user_firma` sigue siendo
siempre la identidad humana; el actor técnico nunca la reemplaza. Nullable
físicamente solo para preservar compatibilidad con firmas históricas
previas a esta migración.

### 2.3 Metadata PKI mínima (`DEC-EDA-023`) — 17 columnas

| Columna | Tipo | Nullable | Default | Contenido |
|---|---|---|---|---|
| `perfil_firma` | `VARCHAR(32)` | NULL | — | Perfil PAdES (`PAdES-B-LT` por defecto normativo). |
| `algoritmo_hash` | `VARCHAR(16)` | NULL | — | Algoritmo de hash usado (`DEC-EDA-029`: nullable, ver §2.4; nunca se inventa un valor retroactivo para filas históricas). |
| `resultado_firma_id` | `CHAR(36)` | NULL | — | Referencia al `resultadoFirmaId` de `firma-binary-transfer.md`; también FK lógica hacia `fal_documento_firma_resultado.resultado_firma_id` (§5). |
| `evidencia_id` | `VARCHAR(128)` | NULL | — | Referencia opaca a la evidencia criptográfica completa (conservada por Firmas). |
| `certificado_serial` | `VARCHAR(64)` | NULL | — | Serial del certificado X.509. |
| `certificado_thumbprint` | `VARCHAR(64)` | NULL | — | Thumbprint SHA-256 del certificado. |
| `certificado_subject` | `VARCHAR(255)` | NULL | — | Subject del certificado. |
| `certificado_issuer` | `VARCHAR(255)` | NULL | — | Issuer del certificado. |
| `certificado_vigencia_desde` | `DATETIME(6)` | NULL | — | Inicio de vigencia del certificado al momento de firmar. |
| `certificado_vigencia_hasta` | `DATETIME(6)` | NULL | — | Fin de vigencia del certificado al momento de firmar. |
| `resultado_cadena` | `SMALLINT` | NULL | — | Catálogo `ResultadoValidacionCadena` (`1=VALIDA, 2=INVALIDA, 3=NO_VERIFICADA`). |
| `fuente_revocacion` | `SMALLINT` | NULL | — | Catálogo `FuenteRevocacion` (`1=OCSP, 2=CRL, 3=NO_DISPONIBLE`). |
| `resultado_revocacion` | `SMALLINT` | NULL | — | Catálogo `ResultadoRevocacion` (`1=NO_REVOCADO, 2=REVOCADO, 3=DESCONOCIDO`). |
| `tsa_nombre` | `VARCHAR(128)` | NULL | — | Autoridad de sello de tiempo. |
| `tsa_instante` | `DATETIME(6)` | NULL | — | Instante del sello de tiempo. |
| `resultado_validacion` | `SMALLINT` | NULL | — | Catálogo `ResultadoValidacionFirma` (`1=VALIDO, 2=INVALIDO, 3=VALIDO_CON_OBSERVACIONES_PERMITIDAS`). |
| `version_validador` | `VARCHAR(32)` | NULL | — | Versión del validador PKI usado por Firmas. |

### 2.4 Obligatoriedad condicional cuando la fila viene del circuito EDA (`DEC-EDA-029`/`034`)

**Decisión final:** `algoritmo_hash` es `NULL`-able para no completar
retroactivamente filas históricas con un algoritmo no demostrado
(`algoritmo_hash NOT NULL DEFAULT 'SHA-256'` queda descartado). En su
lugar, un `CHECK` condicional obliga el conjunto **completo e integral**
de columnas de evidencia siempre que la fila declare `resultado_firma_id`
(es decir, siempre que provenga del circuito EDA nuevo). La versión de
`DEC-EDA-029` cubría un subconjunto (12 columnas); `DEC-EDA-034` la
completa con las columnas restantes que ya existían físicamente desde
`DEC-EDA-023` pero no eran exigidas por el `CHECK` (perfil, evidencia,
certificado completo, `tsa_nombre`, `version_validador`) y con las
condiciones de coherencia entre revisiones y con el catálogo `PAdES-B-LT`
único (`DEC-EDA-035`, §2.4bis):

```sql
CONSTRAINT chk_firma_eda_completo CHECK (
    resultado_firma_id IS NULL
    OR (
        hash_firma IS NOT NULL
        AND hash_salida IS NOT NULL
        AND revision_entrada IS NOT NULL
        AND revision_salida IS NOT NULL
        AND revision_salida = revision_entrada + 1
        AND revision_entrada >= 1
        AND algoritmo_hash IS NOT NULL
        AND perfil_firma = 'PAdES-B-LT'
        AND evidencia_id IS NOT NULL
        AND actor_tecnico_tipo = 1
        AND actor_tecnico_id IS NOT NULL
        AND actor_tecnico_app IS NOT NULL
        AND actor_tecnico_key_fingerprint IS NOT NULL
        AND correlation_id IS NOT NULL
        AND event_id IS NOT NULL
        AND received_at IS NOT NULL
        AND certificado_serial IS NOT NULL
        AND certificado_thumbprint IS NOT NULL
        AND certificado_subject IS NOT NULL
        AND certificado_issuer IS NOT NULL
        AND certificado_vigencia_desde IS NOT NULL
        AND certificado_vigencia_hasta IS NOT NULL
        AND resultado_cadena = 1
        AND fuente_revocacion IN (1, 2)
        AND resultado_revocacion = 1
        AND tsa_nombre IS NOT NULL
        AND tsa_instante IS NOT NULL
        AND resultado_validacion IN (1, 3)
        AND version_validador IS NOT NULL
    )
)
```

`fuente_revocacion IN (1, 2)` excluye deliberadamente `3=NO_DISPONIBLE`
del conjunto EDA completo: una firma que aplicó dominio siempre resolvió
la revocación por `OCSP` o `CRL`, nunca sin verificarla
(`../10-domain/firma-notificacion-fallo.md`, "Firma digital y evidencia
PKI"). `resultado_validacion IN (1, 3)` excluye `2=INVALIDO`: una fila con
`resultado_firma_id` informado es, por definición, una firma que ya superó
la validación PKI (§2.4bis; una PKI inválida nunca llega a crear esta fila
— `DEC-EDA-036`, `../30-integrations/firma-binary-transfer.md` §0bis).

Toda fila sin `resultado_firma_id` (callback síncrono `firmar-real` legacy
o firma histórica anterior a esta migración) queda exenta del `CHECK`; no
se inventa ningún valor para esas filas. Toda fila nueva del circuito EDA
(con `resultado_firma_id` informado) debe traer el conjunto completo o la
transacción de aplicación falla en la base — nunca queda una fila parcial.

### 2.4bis Perfil PKI único: `PAdES-B-LT` sin excepción (`DEC-EDA-035`)

**Decisión final, sin alternativa:** el único perfil de firma aceptado por
esta integración es `PAdES Baseline-LT` (`PAdES-B-LT`). Un `CHECK` general
(no condicionado a `resultado_firma_id`, para cerrar también cualquier
escritura futura fuera del circuito EDA) impide persistir cualquier otro
valor no nulo:

```sql
CONSTRAINT chk_firma_perfil_unico CHECK (perfil_firma IS NULL OR perfil_firma = 'PAdES-B-LT')
```

El catálogo de observaciones permitidas (`../10-domain/firma-notificacion-fallo.md`,
"Firma digital y evidencia PKI") admite exactamente dos códigos
(`TSA_NO_CUALIFICADA_CONFIABLE`, `REVOCACION_POR_CRL_SIN_OCSP`); un PDF
`PAdES-B-T` no satisface el perfil obligatorio y se trata como `INVALIDO`
a los efectos de esta integración, sin excepción.

**Índices/constraints nuevos de §2 (consolidado):**

```sql
INDEX ix_firma_correlation (correlation_id)
INDEX ix_firma_event_id (event_id)
UNIQUE KEY uq_firma_resultado (resultado_firma_id)  -- NULL admitido multiple veces
CONSTRAINT chk_firma_hash_algoritmo CHECK (algoritmo_hash IS NULL OR algoritmo_hash IN ('SHA-256', 'SHA-384', 'SHA-512'))
CONSTRAINT chk_firma_perfil_unico CHECK (perfil_firma IS NULL OR perfil_firma = 'PAdES-B-LT')  -- DEC-EDA-035
CONSTRAINT chk_firma_actor_tecnico_tipo CHECK (actor_tecnico_tipo IS NULL OR actor_tecnico_tipo = 1)
CONSTRAINT chk_firma_resultado_cadena CHECK (resultado_cadena IS NULL OR resultado_cadena BETWEEN 1 AND 3)
CONSTRAINT chk_firma_fuente_revocacion CHECK (fuente_revocacion IS NULL OR fuente_revocacion BETWEEN 1 AND 3)
CONSTRAINT chk_firma_resultado_revocacion CHECK (resultado_revocacion IS NULL OR resultado_revocacion BETWEEN 1 AND 3)
CONSTRAINT chk_firma_resultado_validacion CHECK (resultado_validacion IS NULL OR resultado_validacion BETWEEN 1 AND 3)
CONSTRAINT chk_firma_cert_vigencia CHECK (certificado_vigencia_desde IS NULL OR certificado_vigencia_hasta IS NULL OR certificado_vigencia_desde <= certificado_vigencia_hasta)
CONSTRAINT chk_firma_revision_salida CHECK (revision_salida IS NULL OR revision_entrada IS NULL OR revision_salida = revision_entrada + 1)
CONSTRAINT chk_firma_eda_completo CHECK (...)  -- ver §2.4
CONSTRAINT fk_firma_resultado FOREIGN KEY (resultado_firma_id)
    REFERENCES fal_documento_firma_resultado (resultado_firma_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT  -- DEC-EDA-034; creada DESPUÉS de §4 (la tabla referenciada debe existir primero)
```

`fk_firma_resultado` es la FK física que faltaba entre la evidencia de
dominio (`fal_documento_firma`) y su staging de origen
(`fal_documento_firma_resultado`, §4): sin ella, `resultado_firma_id` era
una referencia únicamente lógica, no verificada por el motor. Se crea
en una sentencia `ALTER TABLE` separada, después de que §4 crea
`fal_documento_firma_resultado` (orden exacto en §12).

**Backfill:** todas las columnas de §2.1-§2.3 quedan `NULL` para filas
existentes (ninguna toma un `DEFAULT` distinto de `NULL`; no se inventa
ningún algoritmo retroactivo). Cualquier fila con `hash_salida IS NULL` y
`actor_tecnico_tipo IS NULL` y `resultado_firma_id IS NULL` es, por
definición, anterior a esta migración o proveniente del callback síncrono
legacy, y queda exenta del `CHECK` de §2.4.

## 3. `fal_documento` — columna nueva: revisión del PDF vigente (`DEC-EDA-024`)

`storage_key`/`hash_docu` siguen siendo la fuente de verdad del contenido
del único PDF vigente (`DEC-EDA-014`). Se agrega la revisión física de ese
PDF vigente, requerida por los contratos `revisionArtefacto`/
`revisionEntrada`/`revisionSalida` (`firma-binary-transfer.md`,
`40-api/firma-event-contracts.md`):

| Columna | Tipo | Nullable | Default | Contenido |
|---|---|---|---|---|
| `revision_artefacto` | `INT` | NOT NULL | `0` | Revisión del único PDF vigente. `0` = documento sin artefacto preparado todavía. Cada preparación inicial la fija en `1`; cada firma válida aplicada la incrementa exactamente en 1. |

**Constraint nueva:**

```sql
CONSTRAINT chk_documento_revision_artefacto CHECK (revision_artefacto >= 0)
```

**Reglas de dominio (aplicación, no física, salvo el `CHECK` anterior):**

- `preparar-artefacto` (`firma-binary-transfer.md` §1) incrementa
  `revision_artefacto` de `0` a `1` en la primera preparación de un
  documento; una preparación idempotente de la misma
  `solicitudFirmaId`+`revisionSolicitud` no vuelve a renderizar ni
  incrementa `revision_artefacto` una segunda vez;
- al consumir `DocumentoFirmado` (`firma-eda-physical-model.md` §5, paso
  7): `revisionEntrada == fal_documento.revision_artefacto` vigente antes
  del reemplazo (si no coincide, la transacción rechaza el evento como
  error permanente — hash/revisión inconsistente,
  `40-api/firma-event-contracts.md` §0.5); `revisionSalida == revisionEntrada + 1`;
  tras el commit, `fal_documento.revision_artefacto = revisionSalida` y
  `fal_documento.hash_docu = hashSalida`;
- después de la primera firma nunca se re-renderiza el contenido; las
  firmas posteriores se aplican siempre al PDF vigente existente,
  incrementando su revisión.

**Backfill definitivo (`DEC-EDA-031`):** `ADD COLUMN ... DEFAULT 0` deja
`0` en toda fila existente por defecto de motor; ese valor es incorrecto
para cualquier documento que ya tenga contenido físico cargado antes de
esta migración. El `UPDATE` posterior corrige exactamente esos casos, sin
inventar una revisión mayor a `1` (no hay forma de reconstruir cuántas
firmas tuvo un PDF antes de este circuito EDA; `1` es el máximo
justificable — "existe contenido" — no un conteo de versiones):

```sql
UPDATE fal_documento
   SET revision_artefacto = CASE
       WHEN storage_key IS NOT NULL OR hash_docu IS NOT NULL THEN 1
       ELSE 0
   END
 WHERE revision_artefacto = 0;
```

**Precheck obligatorio (`../../../database/diagnostics/verify-firmas-eda-closure-001-precheck.sql`
§6):** antes de ejecutar este backfill, reportar toda fila con
exactamente uno de `storage_key`/`hash_docu` informado (inconsistencia de
datos preexistente, ajena a este cierre) y **detener la ejecución** del
forward hasta resolver esa inconsistencia manualmente — el backfill nunca
decide por sí solo cuál de los dos campos es el correcto ni sobrescribe o
re-renderiza el PDF existente. Si el esquema está vacío (sin filas
productivas, §13), el `UPDATE` no tiene efecto observable y el precheck no
encuentra ninguna fila para reportar.

## 4. `fal_documento_firma_resultado` — staging del candidato/recibo temporal (`DEC-EDA-024`/`032`/`033`)

Tabla nueva, propiedad de Faltas. Persiste el recibo técnico idempotente
del `PUT .../resultados/{resultadoFirmaId}`
(`../30-integrations/firma-binary-transfer.md` §3) **sin aplicar ningún
efecto de dominio**: no es historial físico de PDFs; es el puente entre la
carga HTTP y el consumo posterior del evento `DocumentoFirmado`. Además
persiste (`DEC-EDA-033`) la metadata inmutable necesaria para comparar
ese `PUT` con el evento `DocumentoFirmado` que lo completa, y (`DEC-EDA-032`)
el estado durable del swap de storage para que un crash entre el commit de
dominio y la limpieza del PDF anterior sea reconciliable sin perder la
clave anterior.

| Columna | Tipo | Nullable | Default | Contenido |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL AUTO_INCREMENT | — | PK técnica. |
| `resultado_firma_id` | `CHAR(36)` | NOT NULL | — | UUID de negocio; clave de idempotencia de la carga (UNIQUE). |
| `solicitud_firma_id` | `CHAR(36)` | NOT NULL | — | Correlación con `fal_documento_firma_req.solicitud_firma_id`. |
| `id_documento` | `BIGINT` | NOT NULL | — | FK a `fal_documento.id`. |
| `id_firma_req` | `BIGINT` | NOT NULL | — | FK a `fal_documento_firma_req.id`. |
| `seq_firma_req` | `SMALLINT` | NOT NULL | — | Posición del requisito, igual que en `fal_documento_firma_req`. |
| `revision_solicitud` | `INT` | NOT NULL | — | Revisión de la solicitud EDA al momento de la carga. |
| `revision_entrada` | `INT` | NOT NULL | — | `revisionArtefacto` vigente al momento de la carga (validada contra `fal_documento_firma_req.revision_artefacto_preparada`, `DEC-EDA-030`). |
| `revision_salida` | `INT` | NOT NULL | — | Revisión resultante que se aplicará si el evento se consume con éxito. |
| `hash_algoritmo` | `VARCHAR(16)` | NOT NULL | — | Algoritmo declarado en la carga (obligatorio en este contrato HTTP; distinto de `fal_documento_firma.algoritmo_hash`, que es nullable por compatibilidad histórica — ver §2.4). |
| `hash_entrada` | `VARCHAR(128)` | NOT NULL | — | Debe coincidir con `fal_documento_firma_req.hash_entrada_preparado` vigente (validado en la carga). |
| `hash_salida` | `VARCHAR(128)` | NOT NULL | — | Hash del PDF resultante declarado por Firmas. |
| `content_type` | `VARCHAR(64)` | NOT NULL | — | MIME real verificado (`application/pdf`). |
| `content_length` | `BIGINT` | NOT NULL | — | Tamaño exacto del binario recibido. |
| `storage_key_candidato` | `VARCHAR(196)` | NULL | — | Ubicación física interna, definitiva e inmutable del candidato firmado, ya escrita por el `PUT` (nunca una ruta temporal que requiera `rename`/`copy` posterior). Inactiva (no referenciada por `fal_documento.storage_key`) hasta el consumo. `NULL` solo tras `CONSUMIDO` exitoso (una vez promovida a `fal_documento.storage_key`, esta columna se limpia; §5bis) o tras `EXPIRADO`/`RECHAZADO` (purga; nunca se conserva como historial). |
| `storage_key_anterior` | `VARCHAR(196)` | NULL | — | Copia del `fal_documento.storage_key` vigente inmediatamente antes del swap, tomada en la misma transacción que lo reemplaza (§5bis paso 2). `NULL` antes del consumo; `NULL` de nuevo tras `cleanup_status = COMPLETADO` (el PDF anterior ya fue eliminado y la clave dejó de ser necesaria). |
| `cleanup_status` | `SMALLINT` | NOT NULL | `1` | Catálogo `CleanupStatus` (ver abajo). Gobierna la eliminación asíncrona, posterior al commit, del PDF anterior referenciado por `storage_key_anterior`. |
| `cleanup_attempts` | `SMALLINT` | NOT NULL | `0` | Intentos de limpieza realizados por el job posterior al commit. |
| `cleanup_next_attempt_at` | `DATETIME(6)` | NULL | — | Próximo instante habilitado para reintentar la limpieza (backoff). |
| `cleanup_last_error` | `VARCHAR(512)` | NULL | — | Detalle del último error de limpieza, si algún intento falló. |
| `cleanup_completed_at` | `DATETIME(6)` | NULL | — | Momento en que `cleanup_status` pasó a `COMPLETADO`. |
| `referencia_firma_ext` | `VARCHAR(64)` | NOT NULL | — | Igual a `referenciaFirmaExt` de la carga (`DEC-EDA-033`); debe coincidir exactamente con `DocumentoFirmado.referenciaFirmaExt` al consumir el evento. |
| `evidencia_id` | `VARCHAR(128)` | NOT NULL | — | Igual a `evidenciaId` de la carga; debe coincidir exactamente con `DocumentoFirmado.evidenciaId` al consumir el evento. |
| `upload_actor_id` | `VARCHAR(64)` | NOT NULL | — | Identificador técnico del actor autenticado que ejecutó el `PUT`, derivado de la `X-Api-Key` (nunca del JSON enviado por Firmas). |
| `upload_actor_app` | `VARCHAR(32)` | NOT NULL | — | Aplicación origen del actor autenticado del `PUT` (`firmas`), derivada de la credencial. |
| `upload_key_fingerprint` | `VARCHAR(64)` | NOT NULL | — | Fingerprint (hash) de la `X-Api-Key` usada en el `PUT`; nunca la clave completa. |
| `estado_resultado` | `SMALLINT` | NOT NULL | `1` | Catálogo `EstadoResultadoFirma` (ver abajo). |
| `received_at` | `DATETIME(6)` | NOT NULL | — | Momento de la carga HTTP. |
| `validated_at` | `DATETIME(6)` | NULL | — | Momento en que pasó a `VALIDADO`. |
| `consumed_at` | `DATETIME(6)` | NULL | — | Momento en que el consumidor de `DocumentoFirmado` marcó esta fila `CONSUMIDO`. |
| `expires_at` | `DATETIME(6)` | NOT NULL | — | `received_at + 24 horas`. Base del job de purga (`eventing-operations.md` §4). |
| `event_id` | `CHAR(36)` | NULL | — | `eventoId` de `DocumentoFirmado` que consumió (o rechazó, `DEC-EDA-036`) esta fila; `NULL` hasta esa resolución. |
| `last_error_code` | `VARCHAR(64)` | NULL | — | Diagnóstico de rechazo (carga inválida o PKI inválida al consumir, `DEC-EDA-036`), si `estado_resultado = RECHAZADO`. |
| `last_error_detail` | `VARCHAR(512)` | NULL | — | Detalle del rechazo. |
| `version_row` | `INT` | NOT NULL | `0` | OCC. |

**Catálogo `estado_resultado` (`EstadoResultadoFirma`):**

```text
1 = RECIBIDO     -- fila creada, validación en curso
2 = VALIDADO      -- validaciones de la carga superadas; candidato listo para ser consumido por DocumentoFirmado
3 = CONSUMIDO     -- el consumidor de DocumentoFirmado aplicó el efecto de dominio; storage_key_candidato = NULL
4 = EXPIRADO      -- TTL de 24 horas vencido sin consumo; storage_key_candidato = NULL; metadata permanece como evidencia
5 = RECHAZADO     -- la carga no superó validación (hash/MIME/tamaño/revisión) o el DocumentoFirmado asociado tenía PKI inválida o metadata divergente del recibo (DEC-EDA-033/036); no hay candidato utilizable
```

**Catálogo `cleanup_status` (`CleanupStatus`, `DEC-EDA-032`):**

```text
1 = NO_APLICA    -- estado inicial; se fija a PENDIENTE únicamente en el mismo commit que ejecuta el swap (§5bis paso 6), solo si storage_key_anterior difiere del candidato
2 = PENDIENTE    -- swap de dominio confirmado; el PDF anterior referenciado por storage_key_anterior todavía existe y debe eliminarse
3 = COMPLETADO   -- el PDF anterior fue eliminado con éxito; storage_key_anterior y storage_key_candidato se limpian (NULL) en la misma actualización
4 = RETRY        -- un intento de limpieza falló; se reintenta con backoff (mismos defaults que eventing-operations.md §5)
5 = QUARANTINED  -- agotó los intentos automáticos de limpieza; requiere resolución manual (el PDF anterior puede seguir existiendo como basura huérfana, sin afectar el PDF vigente ya swapeado)
```

**PK/claves/índices/constraints:**

```sql
PRIMARY KEY (id)
UNIQUE KEY uq_resultado_firma_id (resultado_firma_id)
CONSTRAINT fk_resultado_documento FOREIGN KEY (id_documento)
    REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT
CONSTRAINT fk_resultado_firma_req FOREIGN KEY (id_firma_req)
    REFERENCES fal_documento_firma_req (id) ON DELETE RESTRICT ON UPDATE RESTRICT
INDEX ix_resultado_expires (estado_resultado, expires_at)  -- barrido de expiracion/purga
INDEX ix_resultado_solicitud (solicitud_firma_id, revision_solicitud)
INDEX ix_resultado_firma_req (id_firma_req, seq_firma_req)
INDEX ix_resultado_estado (estado_resultado)
INDEX ix_resultado_cleanup (cleanup_status, cleanup_next_attempt_at)  -- barrido del job de limpieza de storage
CONSTRAINT chk_resultado_estado CHECK (estado_resultado BETWEEN 1 AND 5)
CONSTRAINT chk_resultado_cleanup_status CHECK (cleanup_status BETWEEN 1 AND 5)
CONSTRAINT chk_resultado_revisiones CHECK (revision_entrada >= 1 AND revision_salida = revision_entrada + 1)
CONSTRAINT chk_resultado_content_length CHECK (content_length BETWEEN 1 AND 104857600)
CONSTRAINT chk_resultado_content_type CHECK (content_type = 'application/pdf')
CONSTRAINT chk_resultado_hash_algoritmo CHECK (hash_algoritmo IN ('SHA-256', 'SHA-384', 'SHA-512'))
```

**Transiciones exactas de `estado_resultado`:** `RECIBIDO → VALIDADO`
(validación de la carga superada, misma transacción HTTP) | `RECIBIDO →
RECHAZADO` (validación de la carga fallida) | `VALIDADO → CONSUMIDO`
(consumo exitoso de `DocumentoFirmado`, misma transacción local del
consumidor) | `VALIDADO → RECHAZADO` (PKI inválida o metadata divergente
detectada al consumir `DocumentoFirmado`, misma transacción local del
consumidor — `DEC-EDA-036`, §5bis) | `VALIDADO → EXPIRADO` (job de purga,
TTL vencido sin consumo). No existe transición desde `RECHAZADO`,
`CONSUMIDO` ni `EXPIRADO` (todos terminales). Transiciones exactas de
`cleanup_status`: `NO_APLICA → PENDIENTE` (§5bis paso 6, solo si aplica) |
`PENDIENTE → COMPLETADO` | `PENDIENTE → RETRY` | `RETRY → RETRY` |
`RETRY → COMPLETADO` | `RETRY → QUARANTINED` (tras agotar los intentos
automáticos). `COMPLETADO` y `QUARANTINED` son terminales para el job de
limpieza (`QUARANTINED` admite resolución manual auditada mediante un
`UPDATE` directo sobre esta misma tabla, dejando constancia en
`cleanup_last_error`; no existe una tabla central de auditoría para esto).

**Idempotencia de la carga:**

- mismo `resultado_firma_id` + mismo `hash_salida` → mismo recibo (`200
  OK`, misma fila, ningún efecto adicional);
- mismo `resultado_firma_id` + `hash_salida` distinto → `409 Conflict`
  (nunca se sobrescribe una fila existente con datos distintos).

**No reuso de `resultadoFirmaId` tras un rechazo (`DEC-EDA-036`):** una
vez que una fila llega a `RECHAZADO` por PKI inválida detectada al
consumir el evento, ese `resultado_firma_id` queda agotado
permanentemente: Firmas debe producir un **nuevo** resultado (nuevo `PUT`
con un nuevo `resultadoFirmaId`) y un **nuevo** `event_id` para
`DocumentoFirmado`, después de corregir la causa de la PKI inválida. El
PDF vigente anterior de `fal_documento` no cambia mientras esto ocurre.

**Limpieza del storage (`DEC-EDA-032`):** el binario candidato **nunca se
conserva como historial**. Al pasar a `CONSUMIDO`, `storage_key_candidato`
se limpia únicamente después de que el job de limpieza confirma
`cleanup_status = COMPLETADO` (§5bis paso posterior al commit) — no en la
misma transacción que el swap, porque hasta ese momento
`storage_key_candidato` sigue siendo el mismo valor físico que
`fal_documento.storage_key` acaba de adoptar y no hay nada que limpiar en
esa columna; lo que sí se limpia de forma asíncrona es
`storage_key_anterior` (el PDF que dejó de ser vigente). Al pasar a
`EXPIRADO` o a `RECHAZADO` (por fallo de carga, nunca alcanzó a tener
`storage_key_candidato` promovido a dominio), `storage_key_candidato` se
limpia de inmediato por el job de purga correspondiente
(`eventing-operations.md` §4/§6); en ambos casos solo la metadata/recibo
permanece como evidencia.

## 5. Secuencia de aplicación del consumidor de `DocumentoFirmado` (`DEC-EDA-024`/`032`/`033`/`036`)

Ver enumeración completa de los pasos HTTP/evento en
`../30-integrations/firma-binary-transfer.md` §0bis. Esta tabla resume las
tablas tocadas por paso, dentro de la transacción local de **procesamiento**
de esa fila de Inbox (`../30-integrations/eventing-operations.md` §3bis.2)
— la fila de `fal_inbox` ya fue insertada por la transacción de
**ingestión** (§3bis.1) antes de que este procesamiento comience; el
detalle exacto del swap de storage y de la validación PKI está en §5bis:

| Paso | Tabla | Operación |
|---|---|---|
| Bloqueo de la fila de Inbox (ya insertada por la ingestión) | `fal_inbox` | `SELECT ... FOR UPDATE` por `bus_evento_id` (PK) |
| Bloqueo optimista | `fal_documento_firma_req` | `SELECT ... FOR UPDATE` (o lectura + `version_row` en el `UPDATE`) |
| Bloqueo del candidato | `fal_documento_firma_resultado` | `SELECT ... FOR UPDATE` por `resultado_firma_id` |
| Validación PKI y de metadata (§5bis paso 1) | — | lectura pura; sin escritura todavía |
| Rama inválida — rechazo funcional (§5bis paso 1a) | `fal_documento_firma_resultado` | `UPDATE estado_resultado = RECHAZADO, event_id, last_error_code/detail` — **fin de la transacción; ningún otro paso de esta tabla se ejecuta** |
| Rama válida — efecto de dominio | `fal_documento_firma` | `INSERT` |
| Satisfacción del requisito | `fal_documento_firma_req` | `UPDATE estado_firma_req = FIRMADO`, `fh_terminal`, `version_row + 1` |
| Swap del PDF vigente (§5bis paso 2) | `fal_documento` | `UPDATE storage_key/hash_docu/revision_artefacto` |
| Cierre del staging + inicio de limpieza (§5bis paso 6) | `fal_documento_firma_resultado` | `UPDATE estado_resultado = CONSUMIDO, consumed_at, event_id, storage_key_anterior = <valor previo de fal_documento.storage_key>, cleanup_status = PENDIENTE` (si `storage_key_anterior` difiere del candidato; `NO_APLICA` en caso contrario) |
| Cierre del Inbox — rama válida y rama inválida por igual | `fal_inbox` | `UPDATE status = PROCESADO, si_error = FALSE, procesado_en` (ambas ramas cierran en `PROCESADO`: el rechazo funcional por PKI inválida no es un error técnico reintentable, §5bis paso 1a) |
| Respuesta (si corresponde) | `fal_outbox` | `INSERT` (mismo commit) |

Ninguna de estas escrituras se confirma parcialmente: la transacción local
de procesamiento completa todos sus pasos o no aplica ninguno
(`../30-integrations/eventing-operations.md` §3bis.2). La rama inválida
(§5bis paso 1a) es una transacción de procesamiento completa por sí misma:
bloquea la fila ya existente de `fal_inbox`, rechaza el resultado y cierra
`fal_inbox` en `PROCESADO`, sin tocar `fal_documento_firma`,
`fal_documento_firma_req` ni `fal_documento`.

### 5bis. Swap de storage recuperable tras crash y vinculación criptográfica del recibo (`DEC-EDA-032`/`033`/`036`)

**Paso 1 — validación previa al efecto de dominio (dentro de la misma
transacción, antes de cualquier `INSERT`/`UPDATE` de negocio):**

1. `fal_documento_firma_resultado.referencia_firma_ext`/`evidencia_id`
   (persistidos en el `PUT`, §4) deben coincidir exactamente con
   `DocumentoFirmado.referenciaFirmaExt`/`evidenciaId` (payload del
   evento). Cualquier divergencia es metadata divergente
   (`DEC-EDA-033`).
2. El resultado PKI declarado en el payload de `DocumentoFirmado` debe
   satisfacer las mismas condiciones que exige `chk_firma_eda_completo`
   (§2.4): cadena `VALIDA`, revocación `NO_REVOCADO` con fuente `OCSP`/
   `CRL`, validación `VALIDO`/`VALIDO_CON_OBSERVACIONES_PERMITIDAS`,
   perfil `PAdES-B-LT` (§2.4bis).
3. Si **cualquiera** de 1 o 2 falla: rama inválida (paso 1a). Si ambas
   pasan: rama válida (pasos 2-8).

**Paso 1a — rama inválida, rechazo funcional (`DEC-EDA-036`):** en la
misma transacción local de procesamiento que bloqueó la fila de
`fal_inbox` por `bus_evento_id` (§5, ya insertada durante la ingestión,
`../30-integrations/eventing-operations.md` §3bis.1),
`fal_documento_firma_resultado` transiciona `VALIDADO → RECHAZADO` con
`event_id` = el `eventoId` de este `DocumentoFirmado`,
`last_error_code`/`last_error_detail` describiendo la causa exacta (PKI
inválida o metadata divergente); `fal_inbox` cierra en `status =
PROCESADO`, `si_error = FALSE`, `procesado_en = UTC_TIMESTAMP(6)`, igual
que la rama válida — el rechazo funcional del evento **no** es un error
técnico reintentable: el evento fue procesado correctamente y su
resultado de negocio fue "rechazar". Por eso **no** se inserta ninguna
fila en `fal_inbox_error` (esa tabla es exclusiva de fallos técnicos de
procesamiento, §6.6): el rechazo por PKI inválida ya queda registrado, con
su código y detalle, en `fal_documento_firma_resultado.last_error_code`/
`last_error_detail`. **No** se ejecuta ningún paso posterior de esta
lista: `fal_documento_firma` no recibe `INSERT`, `fal_documento_firma_req`
no cambia de estado, `fal_documento` no cambia. El `resultado_firma_id`
queda agotado (§4, "No reuso de `resultadoFirmaId`").

**Paso 2 — swap atómico del PDF vigente (rama válida, misma transacción
que el `INSERT` de `fal_documento_firma`):**

```sql
UPDATE fal_documento
   SET storage_key = :storageKeyCandidato,
       hash_docu = :hashSalida,
       revision_artefacto = :revisionSalida
 WHERE id = :idDocumento
   AND revision_artefacto = :revisionEntrada;
-- ROW_COUNT() = 0 => otra firma concurrente ya avanzo revision_artefacto
--                    sobre el mismo documento; error permanente, aborta
--                    la transaccion completa (nunca se aplica un swap
--                    parcial ni se reintenta automaticamente).
```

`:storageKeyCandidato` es el mismo valor físico ya escrito por el `PUT`
(`fal_documento_firma_resultado.storage_key_candidato`, §4) — el swap es
un cambio de puntero en `fal_documento`, nunca una copia física del
binario en esta transacción.

**Pasos 3-5 — efecto de dominio, satisfacción del requisito** (ver tabla
de §5, sin cambios respecto al detalle previo).

**Paso 6 — cierre del staging e inicio de la limpieza asíncrona (misma
transacción que el paso 2, commit único):**

```sql
UPDATE fal_documento_firma_resultado
   SET estado_resultado = 3,           -- CONSUMIDO
       consumed_at = UTC_TIMESTAMP(6),
       event_id = :eventoId,
       storage_key_anterior = :storageKeyPrevioDeDocumento,  -- leído en el mismo paso 2, antes del UPDATE
       cleanup_status = CASE WHEN :storageKeyPrevioDeDocumento IS NULL
                                  OR :storageKeyPrevioDeDocumento = storage_key_candidato
                             THEN 1 ELSE 2 END,  -- NO_APLICA si no había PDF previo o coincide; PENDIENTE si hay que limpiar
       version_row = version_row + 1
 WHERE resultado_firma_id = :resultadoFirmaId;
```

`storage_key_candidato` **no** se limpia en este paso (paso 7 lo hace,
fuera de esta transacción, solo tras confirmar la eliminación física del
PDF anterior — §4, "Limpieza del storage").

**Paso 7 — job de limpieza asíncrona, posterior al commit del paso 6, sin
relación con la transacción del consumidor de `DocumentoFirmado`:**

1. reclama filas `cleanup_status IN (2, 4)` (`PENDIENTE`/`RETRY`) con
   `cleanup_next_attempt_at IS NULL OR <= UTC_TIMESTAMP(6)` mediante
   `SELECT ... FOR UPDATE SKIP LOCKED` por fila, dado su volumen bajo (esta
   tabla no tiene columnas de lease `locked_by`/`locked_until`, a
   diferencia de ningún componente del bus — ni `fal_outbox` ni `bus_evento`
   las tienen tampoco, §6.2/§6.3);
2. elimina físicamente el objeto de storage referenciado por
   `storage_key_anterior`;
3. éxito → `cleanup_status = 3` (`COMPLETADO`), `cleanup_completed_at =
   UTC_TIMESTAMP(6)`, `storage_key_anterior = NULL`,
   `storage_key_candidato = NULL` (ya no hay nada que limpiar; el valor
   sigue siendo idéntico a `fal_documento.storage_key` pero esta tabla ya
   no necesita conservarlo);
4. fallo con `cleanup_attempts + 1 < 10` → `cleanup_status = 4`
   (`RETRY`), `cleanup_attempts += 1`, `cleanup_last_error`,
   `cleanup_next_attempt_at` con el mismo backoff que
   `eventing-operations.md` §5;
5. fallo con `cleanup_attempts + 1 >= 10` → `cleanup_status = 5`
   (`QUARANTINED`); requiere resolución manual (`eventing-operations.md`
   §6) mediante un `UPDATE` directo sobre esta tabla, sin tabla central de
   auditoría.

Un crash entre el commit del paso 6 y la ejecución del paso 7 es
recuperable sin ambigüedad: `storage_key_anterior` sigue persistido, el
PDF vigente ya es el correcto (`fal_documento.storage_key` ya adoptó el
candidato en el mismo commit atómico del paso 2), y el job de limpieza
simplemente reencuentra la fila en `cleanup_status = PENDIENTE` en su
próximo ciclo — nunca hay una ventana en la que el PDF anterior quede
huérfano sin una clave que lo referencie.

## 6. Bus de eventos municipal — `bus_evento` + Outbox/checkpoint/Inbox/Inbox error locales (`DEC-EDA-022`/`025`/`026`, ownership Faltas)

**Modelo mínimo aprobado.** El bus central se reduce a una única tabla
append-only (`bus_evento`) más los componentes locales de cada aplicación
(Outbox, checkpoint, Inbox, Inbox error). No hay fan-out central, no hay
tabla de entregas, no hay suscripciones, no hay permisos de publicación
tabulares, no hay rutinas `SQL SECURITY DEFINER`, no hay vistas
diagnósticas ni cuenta definidora (`bus_definer`): cada aplicación
municipal publica con `INSERT` directo (grants mínimos, §10) y consume
leyendo `bus_evento` con `SELECT` directo, manteniendo su propio progreso
de lectura en su checkpoint local.

Todas: `ENGINE=InnoDB`, `DEFAULT CHARACTER SET=utf8mb4`,
`COLLATE=utf8mb4_uca1400_ai_ci`.

### 6.0 Principio arquitectónico

```text
Cambio de dominio + Outbox local
              |
              v
        relay de la aplicación
              |
              v
      bus_evento append-only
              |
              v
 lector local + checkpoint
              |
              v
         Inbox local
              |
              v
   worker de procesamiento local
```

1. `bus_evento` es una tabla central append-only, inmutable tras el
   `INSERT`.
2. El productor escribe primero en su Outbox local, en la misma
   transacción del cambio de dominio.
3. Un relay de la aplicación inserta el evento en `bus_evento`,
   reutilizando el mismo `evento_id` en cada reintento (idempotencia por
   `UNIQUE(evento_id)`, no por un fingerprint calculado en SQL).
4. Cada consumidor lee `bus_evento` por `bus_evento_id` ascendente.
5. Cada consumidor mantiene su propio checkpoint local
   (`fal_bus_checkpoint`/`fir_bus_checkpoint`).
6. Los eventos que le interesan (por `evento_codigo`) se copian a su
   Inbox local, en la misma transacción que avanza el checkpoint.
7. El procesamiento funcional ocurre desde la Inbox, nunca directamente
   contra `bus_evento`.
8. Un evento fallido en la Inbox no detiene el procesamiento de otras
   filas `PENDIENTE`/`REINTENTO`.
9. `bus_evento` no registra estados de consumo, entregas ni suscriptores.
10. Un evento puede tener cero consumidores interesados y eso es válido
    (§6.7): no se crea ninguna fila adicional, no ocurre ningún error.
11. El bus puede ser usado por aplicaciones municipales ajenas al dominio
    de Faltas sin modificar su estructura: una aplicación nueva solo
    requiere una cuenta SQL con permisos mínimos (§10), su propio Outbox/
    checkpoint/Inbox/Inbox error locales, sus enums/constantes de códigos
    y su propia configuración de lectura. Nunca requiere alterar
    `bus_evento`.

### 6.1 Catálogo de códigos compactos

No se persisten nombres extensos (`SolicitudFirmaRechazadaPorContrato`,
etc.) en cada fila operativa. Se usan códigos breves con longitud máxima
explícita, representados en cada dominio mediante enums o constantes
legibles:

| Columna | Tipo |
|---|---|
| `productor_codigo` | `VARCHAR(8)` |
| `evento_codigo` | `VARCHAR(16)` |
| `agregado_codigo` | `VARCHAR(12)` |
| `agregado_id` | `BIGINT` |
| `status` | `TINYINT UNSIGNED` |
| `schema_version` | `SMALLINT UNSIGNED` |
| `evento_id` | `BINARY(16)` |
| `correlation_id` | `BINARY(16)` |
| `causation_id` | `BINARY(16)` |

**Códigos de aplicación (`productor_codigo`):** `FAL` = Faltas, `FIR` =
Firmas. Sin `CHECK IN ('FAL','FIR')`: la validación de valores vigentes
vive en el enum/constante de cada dominio, no en el esquema — una
aplicación municipal futura usa otro código de hasta 8 caracteres sin
requerir ninguna migración estructural de `bus_evento`.

**Mapeo de los 12 contratos vigentes (`evento_codigo`, `VARCHAR(16)`).**
Los nombres legibles siguen siendo los títulos de contrato normativos de
`../40-api/firma-event-contracts.md`; estos códigos son exclusivamente su
representación física persistida:

| `event_type` (contrato) | `evento_codigo` (persistido) |
|---|---|
| `FirmaSolicitada` | `FIR_SOLIC` |
| `SolicitudFirmaAceptada` | `FIR_ACEPT` |
| `SolicitudFirmaRechazadaPorContrato` | `FIR_RECH_CONT` |
| `DocumentoFirmado` | `DOC_FIRM` |
| `SolicitudFirmaCancelada` | `FIR_CANCEL` |
| `SolicitudFirmaCanceladaConfirmada` | `FIR_CANCEL_OK` |
| `FirmaRechazada` | `FIR_RECH` |
| `SolicitudFirmaVencida` | `FIR_VENC` |
| `FirmanteCreado` | `FTE_CREADO` |
| `FirmanteVersionCreada` | `FTE_VER_CREADA` |
| `FirmanteHabilitacionModificada` | `FTE_HAB_MOD` |
| `DelegacionFirmanteModificada` | `FTE_DELEG_MOD` |

En Java, cada dominio modela estos códigos como un enum con nombre legible
y código persistido (ej. `FIRMA_SOLICITADA("FIR_SOLIC")`); implementar ese
enum es un cambio de código productivo fuera del alcance de este cierre
documental. Los 12 contratos conservan toda su semántica funcional y
payload; solo cambia la representación compacta de sus códigos y el
mecanismo de transporte.

### 6.2 `fal_outbox`

Se escribe junto con el cambio de dominio, en la misma transacción local.
El relay reutiliza el mismo `evento_id` en cada reintento (idempotencia de
publicación por `UNIQUE(evento_id)` en `bus_evento`). Ningún stack trace
ni mensaje largo se guarda aquí: el detalle técnico completo vive en logs
correlacionados por `evento_id`.

| Columna | Tipo | Nullable | Default |
|---|---|---|---|
| `outbox_id` | `BIGINT UNSIGNED` | NOT NULL AUTO_INCREMENT | — |
| `evento_id` | `BINARY(16)` | NOT NULL | — |
| `evento_codigo` | `VARCHAR(16)` | NOT NULL | — |
| `schema_version` | `SMALLINT UNSIGNED` | NOT NULL | `1` |
| `agregado_codigo` | `VARCHAR(12)` | NULL | — |
| `agregado_id` | `BIGINT` | NULL | — |
| `agregado_version` | `BIGINT UNSIGNED` | NULL | — |
| `correlation_id` | `BINARY(16)` | NULL | — |
| `causation_id` | `BINARY(16)` | NULL | — |
| `payload` | `JSON` | NOT NULL | — |
| `status` | `TINYINT UNSIGNED` | NOT NULL | `0` |
| `si_error` | `BOOLEAN` | NOT NULL | `FALSE` |
| `attempts` | `SMALLINT UNSIGNED` | NOT NULL | `0` |
| `next_attempt_at` | `DATETIME(6)` | NULL | — |
| `ocurrido_en` | `DATETIME(6)` | NOT NULL | — |
| `creado_en` | `DATETIME(6)` | NOT NULL | — |
| `publicado_en` | `DATETIME(6)` | NULL | — |

**Catálogo `status`:** `0=PENDIENTE, 1=PUBLICADO, 2=REINTENTO,
3=ERROR_DEFINITIVO` (sin tabla catálogo; documentado como enum/constante
en el dominio). `si_error = TRUE` indica que el último intento de
publicación falló; `si_error = FALSE` indica que no hay error activo. No
existe maquinaria de cuarentena central: sin `locked_by`/`locked_until`
(lease), sin `last_error_code`/`last_error_detail`/`last_error_at`, sin
`quarantined_at`/`resolved_at`.

```sql
PRIMARY KEY (outbox_id)
UNIQUE KEY uq_fal_outbox_evento_id (evento_id)
INDEX ix_fal_outbox_status (status, next_attempt_at)
INDEX ix_fal_outbox_agregado (agregado_codigo, agregado_id, agregado_version)
CONSTRAINT chk_fal_outbox_status CHECK (status BETWEEN 0 AND 3)
CONSTRAINT chk_fal_outbox_schema_version CHECK (schema_version >= 1)
CONSTRAINT chk_fal_outbox_attempts CHECK (attempts >= 0)
CONSTRAINT chk_fal_outbox_si_error CHECK (si_error IN (FALSE, TRUE))
```

### 6.3 `bus_evento`

Única tabla central, append-only, inmutable tras el `INSERT`. Vive en la
base central del bus (`sb_faltas_db` en este cierre; ver §8).

| Columna | Tipo | Nullable |
|---|---|---|
| `bus_evento_id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | — |
| `evento_id` | `BINARY(16) NOT NULL` | — |
| `productor_codigo` | `VARCHAR(8) NOT NULL` | — |
| `evento_codigo` | `VARCHAR(16) NOT NULL` | — |
| `schema_version` | `SMALLINT UNSIGNED NOT NULL` | — |
| `agregado_codigo` | `VARCHAR(12)` | NULL |
| `agregado_id` | `BIGINT` | NULL |
| `agregado_version` | `BIGINT UNSIGNED` | NULL |
| `correlation_id` | `BINARY(16)` | NULL |
| `causation_id` | `BINARY(16)` | NULL |
| `payload` | `JSON NOT NULL` | — |
| `ocurrido_en` | `DATETIME(6) NOT NULL` | — |
| `publicado_en` | `DATETIME(6) NOT NULL` | — |

No hay `headers`, `trace_id`, `event_fingerprint`,
`published_by_principal`, `status`, `attempts`, `locked_by`,
`locked_until`, `quarantined_at`, `resolved_at` ni `consumer`/`delivery_id`
en esta tabla: la identidad de conexión se administra mediante la cuenta
SQL y los logs del servidor/aplicación (§10), no duplicando una plataforma
de auditoría en cada fila.

**Idempotencia de publicación por `UNIQUE(evento_id)`, no por
fingerprint calculado en SQL:** el relay reutiliza el mismo `evento_id` en
cada reintento; un segundo `INSERT` con el mismo `evento_id` falla por la
restricción `UNIQUE` (`duplicate key` sobre `uq_bus_evento_evento_id`) y
la aplicación interpreta ese código de error del `INSERT` fallido como
"ya publicado", **sin ejecutar ningún `SELECT` sobre `bus_evento` para
confirmarlo** y sin que la base de datos necesite calcular ni comparar
ningún hash: el propio resultado del `INSERT` (éxito o `duplicate key`) es
la única señal de reconciliación que usa el relay.

```sql
PRIMARY KEY (bus_evento_id)
UNIQUE KEY uq_bus_evento_evento_id (evento_id)
INDEX ix_bus_evento_codigo (evento_codigo, bus_evento_id)
INDEX ix_bus_evento_productor (productor_codigo, bus_evento_id)
INDEX ix_bus_evento_agregado (agregado_codigo, agregado_id, bus_evento_id)
CONSTRAINT chk_bus_evento_schema_version CHECK (schema_version >= 1)
```

Sin `CHECK (productor_codigo IN (...))` ni equivalente: la enumeración de
aplicaciones municipales vigentes vive en el enum/constante de cada
dominio (§6.1), no como una lista cerrada en el esquema — agregar una
aplicación municipal futura no requiere ningún `ALTER TABLE` sobre
`bus_evento`.

### 6.4 `fal_bus_checkpoint`

Checkpoint local de lectura del bus para el consumidor Faltas. Tabla
singleton (`checkpoint_id = 1` fijo por `CHECK`).

| Columna | Tipo | Nullable | Default |
|---|---|---|---|
| `checkpoint_id` | `TINYINT UNSIGNED` | NOT NULL | — |
| `ultimo_evento_id` | `BIGINT UNSIGNED` | NOT NULL | `0` |
| `actualizado_en` | `DATETIME(6)` | NOT NULL | — |

```sql
PRIMARY KEY (checkpoint_id)
CONSTRAINT chk_fal_bus_checkpoint_singleton CHECK (checkpoint_id = 1)
```

Seed inicial: una única fila `(1, 0, UTC_TIMESTAMP(6))`.

**Semántica:** `ultimo_evento_id` es el último `bus_evento_id` que Faltas
terminó de examinar/copiar a su Inbox local — nunca disminuye. Consulta
base de lectura, en una transacción corta de solo lectura con `LOCK IN
SHARE MODE` (motivo completo y ejemplo de hueco de publicación
concurrente en `../30-integrations/eventing-operations.md` §4 — sin ese
bloqueo, un `INSERT` concurrente con un `bus_evento_id` menor todavía sin
confirmar puede quedar permanentemente salteado si el lector avanza el
checkpoint más allá de ese hueco):

```sql
START TRANSACTION READ ONLY;
SELECT bus_evento_id, evento_id, productor_codigo, evento_codigo,
       schema_version, agregado_codigo, agregado_id, agregado_version,
       correlation_id, causation_id, payload, ocurrido_en, publicado_en
FROM bus_evento
WHERE bus_evento_id > :ultimo_evento_id
ORDER BY bus_evento_id
LIMIT :batchSize
LOCK IN SHARE MODE;
COMMIT;
```

**Flujo de ingestión local** (transacción única, corta, independiente del
procesamiento funcional — `../30-integrations/eventing-operations.md`
§3bis.1): (1) leer un lote de `bus_evento` con `LOCK IN SHARE MODE`
(arriba); (2) identificar por `evento_codigo` cuáles eventos interesan a
Faltas; (3) insertar los relevantes en `fal_inbox` con operación
idempotente (PK = `bus_evento_id`), copiando el envelope completo
(`evento_id`, `productor_codigo`, `schema_version`, `correlation_id`,
`causation_id`, `ocurrido_en`; §6.5) sin ejecutar ningún efecto de
dominio; (4) avanzar `fal_bus_checkpoint.ultimo_evento_id` al último
`bus_evento_id` examinado del lote — coordinado con (3) en la misma
transacción local, para que nunca se avance el checkpoint antes de copiar
el evento relevante; (5) si se repite la lectura tras un crash, la PK de
`fal_inbox` evita el duplicado. Nunca se usa `MAX(bus_evento_id)` de
`fal_inbox` como checkpoint: la Inbox solo contiene eventos relevantes, no
todos los examinados. El procesamiento funcional de cada fila insertada
ocurre en una transacción local posterior e independiente (§6.5,
`../30-integrations/eventing-operations.md` §3bis.2).

### 6.5 `fal_inbox`

Separa la lectura del bus (`fal_bus_checkpoint`) del procesamiento
funcional. `PK = bus_evento_id` (no un id técnico propio): la Inbox solo
contiene eventos relevantes para Faltas, copiados desde `bus_evento` por
el lector local; esa PK es lo que garantiza que un crash y una relectura
del mismo lote nunca dupliquen la fila.

**`bus_evento_id` vs. `evento_id`:** `bus_evento_id` sigue siendo la PK
operativa — es el cursor técnico secuencial que coincide con el
`bus_evento_id` de origen y con `fal_bus_checkpoint.ultimo_evento_id`
(§6.4). `evento_id` es la identidad lógica estable del evento (el mismo
valor en `bus_evento`, en el Outbox de origen y en este Inbox);
`UNIQUE(evento_id)` es una defensa adicional de idempotencia, independiente
del cursor técnico. No existe `UNIQUE(consumer, event_id)`: no hay columna
`consumer` en esta tabla (§7, ámbito único).

| Columna | Tipo | Nullable | Default |
|---|---|---|---|
| `bus_evento_id` | `BIGINT UNSIGNED` | NOT NULL | — |
| `evento_id` | `BINARY(16)` | NOT NULL | — |
| `productor_codigo` | `VARCHAR(8)` | NOT NULL | — |
| `evento_codigo` | `VARCHAR(16)` | NOT NULL | — |
| `schema_version` | `SMALLINT UNSIGNED` | NOT NULL | — |
| `agregado_codigo` | `VARCHAR(12)` | NULL | — |
| `agregado_id` | `BIGINT` | NULL | — |
| `agregado_version` | `BIGINT UNSIGNED` | NULL | — |
| `correlation_id` | `BINARY(16)` | NULL | — |
| `causation_id` | `BINARY(16)` | NULL | — |
| `payload` | `JSON` | NOT NULL | — |
| `status` | `TINYINT UNSIGNED` | NOT NULL | `0` |
| `si_error` | `BOOLEAN` | NOT NULL | `FALSE` |
| `attempts` | `SMALLINT UNSIGNED` | NOT NULL | `0` |
| `next_attempt_at` | `DATETIME(6)` | NULL | — |
| `ocurrido_en` | `DATETIME(6)` | NOT NULL | — |
| `recibido_en` | `DATETIME(6)` | NOT NULL | — |
| `procesado_en` | `DATETIME(6)` | NULL | — |

**Catálogo `status`:** `0=PENDIENTE, 1=PROCESANDO, 2=PROCESADO,
3=REINTENTO, 4=ERROR_DEFINITIVO, 5=IGNORADO` (sin tabla catálogo). Un
**rechazo funcional** de dominio (ej. PKI inválida,
`../10-domain/firma-notificacion-fallo.md` §11bis) cierra en `status =
PROCESADO`, `si_error = FALSE`: el evento fue procesado correctamente y su
resultado de negocio fue rechazar; **no** es un error técnico y **no**
genera fila en `fal_inbox_error`. Solo un **fallo técnico** de
procesamiento (excepción no controlada, error de infraestructura) deja
`si_error = TRUE`, `status` en `REINTENTO`/`ERROR_DEFINITIVO` e inserta
una fila en `fal_inbox_error` (§6.6). No hay `consumer`, `event_type`
largo, `aggregate_type` largo, `efecto_aplicado`,
`last_error_code`/`last_error_detail`/`last_error_at` ni stack trace en
esta tabla: el detalle de cada fallo técnico vive en `fal_inbox_error`
(§6.6).

```sql
PRIMARY KEY (bus_evento_id)
UNIQUE KEY uq_fal_inbox_evento_id (evento_id)
INDEX ix_fal_inbox_status (status, next_attempt_at)
INDEX ix_fal_inbox_evento_codigo (evento_codigo, bus_evento_id)
INDEX ix_fal_inbox_agregado (agregado_codigo, agregado_id, agregado_version)
CONSTRAINT chk_fal_inbox_status CHECK (status BETWEEN 0 AND 5)
CONSTRAINT chk_fal_inbox_attempts CHECK (attempts >= 0)
CONSTRAINT chk_fal_inbox_si_error CHECK (si_error IN (FALSE, TRUE))
```

**Regla crítica:** un evento con error no detiene el procesamiento de
otros eventos; el worker local sigue con otras filas
`PENDIENTE`/`REINTENTO` habilitadas. El bus central no implementa bloqueo
por agregado, fan-out ni orden global: las reglas de versión/orden
específicas de los 12 contratos permanecen en el handler de dominio
correspondiente, no en el transporte.

### 6.6 `fal_inbox_error`

Tabla local de errores de procesamiento de `fal_inbox`. No hay tabla
central de errores: cada consumidor guarda su propio historial. Se
inserta una fila solo cuando ocurre un error; el stack trace completo
queda en logs correlacionados por `bus_evento_id`, nunca aquí.

| Columna | Tipo | Nullable |
|---|---|---|
| `inbox_error_id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | — |
| `bus_evento_id` | `BIGINT UNSIGNED NOT NULL` | — |
| `attempt_number` | `SMALLINT UNSIGNED NOT NULL` | — |
| `error_code` | `SMALLINT UNSIGNED NOT NULL` | — |
| `error_detail` | `VARCHAR(255)` | NULL |
| `ocurrido_en` | `DATETIME(6) NOT NULL` | — |

```sql
PRIMARY KEY (inbox_error_id)
UNIQUE KEY uq_fal_inbox_error_attempt (bus_evento_id, attempt_number)
INDEX ix_fal_inbox_error_evento (bus_evento_id, ocurrido_en)
CONSTRAINT fk_fal_inbox_error_inbox FOREIGN KEY (bus_evento_id)
    REFERENCES fal_inbox (bus_evento_id) ON DELETE RESTRICT ON UPDATE RESTRICT
```

**Reglas:** `error_code` es un código numérico modelado como enum local de
Faltas; `error_detail` es un resumen breve (máximo 255 caracteres), nunca
un stack trace. Al fallar el procesamiento: incrementar
`fal_inbox.attempts`, fijar `si_error = TRUE`, dejar `status` en
`REINTENTO`/`ERROR_DEFINITIVO` e insertar la fila de error correspondiente
(una por intento fallido, `attempt_number` coincide con el valor de
`attempts` al momento del fallo). Al procesar luego con éxito: fijar
`si_error = FALSE`, `status = PROCESADO`, completar `procesado_en`, y
conservar íntegro el historial de errores anterior en `fal_inbox_error`
(nunca se borra).

### 6.7 Evento sin consumidores

Regla normativa: `bus_evento` recibe el `INSERT`; si cero aplicaciones
están interesadas en ese `evento_codigo`, no ocurre ningún error, no se
crea ninguna fila adicional, no se actualiza el evento. No existe tabla de
suscripciones: cada aplicación define en su propio código/configuración
qué `evento_codigo` procesa. Una aplicación futura puede comenzar a leer
desde el último evento disponible, desde un `bus_evento_id` acordado, o
desde cero si la retención lo permite (§11); este cierre no implementa un
procedimiento de replay.

## 7. Ámbito único: `consumer` = `'faltas'` en este repositorio

En `fal_inbox`, el ámbito de lectura siempre corresponde a Faltas
(Faltas solo consume sus propios eventos entrantes desde su checkpoint
local, §6.4). No hay columna `consumer` en `fal_inbox` ni en `bus_evento`:
el ámbito de cada Inbox local está dado por el propio esquema/base de
datos de la aplicación que la posee, no por un valor de fila.

## 8. Bounded context Firmas: `fir_outbox`/`fir_bus_checkpoint`/`fir_inbox`/`fir_inbox_error` — paquete DDL separado y ejecutable (`DEC-EDA-028`)

Firmas es una aplicación/repositorio distinto de este monorepo (no existe
`backend/api-firmas` en `Bod-Faltas`) y su base de datos no es propiedad de
este repositorio. El contrato de `fir_outbox`/`fir_bus_checkpoint`/
`fir_inbox`/`fir_inbox_error` (misma estructura de columnas y los mismos
CHECK de integridad que `fal_outbox`/`fal_bus_checkpoint`/`fal_inbox`/
`fal_inbox_error`, §6.2-§6.6) tiene su propio paquete DDL **ejecutable**
(no comentado), bajo responsabilidad del equipo de Firmas, en:

```text
database/ddl/firmas/create-firmas-eda-outbox-inbox.sql
database/ddl/firmas/rollback-firmas-eda-outbox-inbox.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-precheck.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-postcheck.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-rollback-precheck.sql
```

Ese paquete no se ejecuta desde `alter-firmas-eda-closure-001-forward.sql`
ni contra `sb_faltas_db`; el script exige que el operador seleccione
explícitamente la base de destino (no ejecuta `USE`) y el precheck valida
`DATABASE() IS NOT NULL` antes de crear cualquier objeto, precisamente
porque el nombre de esa base de datos no es un objeto lógico de este
repositorio y no corresponde declararlo "a definir" en la spec de Faltas.

## 9. Transporte del bus: JDBC/SQL directo, sin capa HTTP (`DEC-EDA-027`)

**Decisión final:** el bus central (`bus_evento`, única tabla) se accede
exclusivamente por JDBC/SQL, con `INSERT` directo para publicar y
`SELECT` directo para leer, usando cuentas MariaDB de mínimo privilegio
(`grants-firmas-eda-bus.sql`, §10). No hay rutinas almacenadas ni vistas
intermediarias: la aplicación ejecuta sus propias sentencias SQL contra
`bus_evento`, `fal_outbox`/`fal_bus_checkpoint`/`fal_inbox`/
`fal_inbox_error` (o sus equivalentes `fir_*`) usando su datasource
habitual. `X-Api-Key` (`DEC-EDA-018`) se usa únicamente en los tres
endpoints HTTP de transferencia binaria Faltas↔Firmas
(`../30-integrations/firma-binary-transfer.md`); nunca existe una "API del
bus" HTTP. Detalle de arquitectura conceptual en
`../30-integrations/eventing-operations.md`.

## 10. Seguridad SQL mínima del bus: grants directos, sin definer ni rutinas (`DEC-EDA-027`)

**Decisión final:** no existe una cuenta definidora (`bus_definer`), no
existen rutinas `SQL SECURITY DEFINER` ni vistas diagnósticas
intermediarias. Cada cuenta de aplicación recibe únicamente el grant
mínimo directo que necesita sobre `bus_evento`; las tablas locales
(`fal_outbox`, `fal_bus_checkpoint`, `fal_inbox`, `fal_inbox_error` y sus
equivalentes `fir_*`) se acceden con el datasource/usuario de aplicación
ya existente de cada dominio, nunca con una cuenta `bus_*`.

**5 cuentas SQL fijas `'usuario'@'%'`, definidas en
`database/ddl/grants-firmas-eda-bus.sql` (sin contraseñas ni `CREATE
USER`; el aprovisionamiento de las propias cuentas MariaDB — nombre, host
permitido, `REQUIRE SSL`, credencial — es responsabilidad del secret
manager/pipeline de la plataforma, fuera de este repositorio; el grant
solo puede ejecutarse después de que
`database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql`
confirma que esas 5 cuentas ya existen exactamente como `'usuario'@'%'`
con `REQUIRE SSL` y reporta un veredicto único `READY`/`BLOCKED`):**

| Usuario | Permisos directos sobre `bus_evento` |
|---|---|
| `bus_publisher_faltas` | `INSERT` column-level, todas las columnas excepto `bus_evento_id` |
| `bus_publisher_firmas` | `INSERT` column-level, todas las columnas excepto `bus_evento_id` |
| `bus_consumer_faltas` | `SELECT` |
| `bus_consumer_firmas` | `SELECT` |
| `bus_maintenance` | `SELECT`, `DELETE` (retención autorizada, §11) |

**`INSERT` column-level de los publishers, sin `bus_evento_id`:** el
grant de cada publisher (`GRANT INSERT (evento_id, productor_codigo,
evento_codigo, schema_version, agregado_codigo, agregado_id,
agregado_version, correlation_id, causation_id, payload, ocurrido_en,
publicado_en) ON bus_evento ...`) excluye explícitamente `bus_evento_id`:
ese valor lo genera siempre la base de datos (`AUTO_INCREMENT`), nunca el
cliente — ninguna aplicación productora puede escribir un `bus_evento_id`
propio, ni por error ni intencionalmente. `bus_consumer_*` y
`bus_maintenance` mantienen privilegio a nivel de tabla completa (`SELECT`
necesita leer todas las columnas para copiar el envelope a su Inbox local,
§6.4; `DELETE` no admite alcance column-level en MariaDB).

Ninguna de estas 5 cuentas recibe privilegio alguno sobre `fal_documento*`,
`fal_outbox`, `fal_bus_checkpoint`, `fal_inbox` ni `fal_inbox_error`: esas
tablas son locales de la aplicación Faltas y se acceden con el
datasource/usuario de aplicación ya existente, no con una cuenta `bus_*`.
Sin `CHECK`/enumeración cerrada de aplicaciones en el esquema: incorporar
una aplicación municipal futura requiere únicamente una nueva cuenta SQL
con este mismo patrón de permisos mínimos, sin ningún cambio estructural
sobre `bus_evento`.

## 11. Retención

No existe `sp_bus_purgar` ni ningún procedimiento almacenado de purga: la
retención es una tarea operativa externa y simple, ejecutada por
`bus_maintenance` (único principal con `DELETE` sobre `bus_evento`, §10)
fuera de la ruta de publicación/consumo. El único criterio de purga es la
antigüedad de `publicado_en` frente a la ventana de retención vigente
documentada en `../30-integrations/eventing-operations.md`
(`publicado_en < UTC_TIMESTAMP() - retención_configurada`);
`bus_maintenance` no tiene ni necesita ningún conocimiento de
`fal_bus_checkpoint`/`fir_bus_checkpoint` (tablas locales de cada
aplicación consumidora, fuera de su alcance de permisos, §10): la purga
nunca consulta ni compara contra el progreso de lectura de ningún
consumidor. `bus_evento` no es un registro jurídico: la fuente de verdad
funcional sigue en cada dominio (Faltas/Firmas), no en el bus.

## 12. Orden de ejecución y dependencias

**Orden operativo completo (`database/README.md`, misma fuente normativa —
no duplicar aquí la lista de pasos, solo referenciarla): precheck
principal → forward → postcheck estructural + parity → precheck de los 5
principals de aplicación → grants de aplicación → postcheck runtime de
seguridad y permisos. Ningún paso se repite ni se salta.**

**Sobre `sb_faltas_db` (`alter-firmas-eda-closure-001-forward.sql`), tras
el precheck principal en `READY`:**

1. `ALTER TABLE fal_documento_firma_req` (§1) — no depende de ninguna tabla
   nueva.
2. `ALTER TABLE fal_documento_firma` (§2, columnas y constraints salvo
   `fk_firma_resultado`) — no depende de ninguna tabla nueva.
3. `ALTER TABLE fal_documento` (§3) — no depende de ninguna tabla nueva.
4. `CREATE TABLE fal_documento_firma_resultado` (§4) — FK a `fal_documento`
   y a `fal_documento_firma_req`; debe crearse después de 1 y 3.
5. `ALTER TABLE fal_documento_firma ADD CONSTRAINT fk_firma_resultado`
   (§2, cierre diferido) — después de 4 (la tabla referenciada debe
   existir).
6. `CREATE TABLE fal_outbox` (§6.2) — sin FK a otras tablas nuevas.
7. `CREATE TABLE bus_evento` (§6.3) — sin FK a otras tablas nuevas.
8. `CREATE TABLE fal_bus_checkpoint` (§6.4) — sin FK a otras tablas nuevas.
9. `INSERT` seed de `fal_bus_checkpoint` (§6.4) — después de 8 (fila única
   `checkpoint_id = 1`).
10. `CREATE TABLE fal_inbox` (§6.5) — sin FK a otras tablas nuevas.
11. `CREATE TABLE fal_inbox_error` (§6.6) — FK a `fal_inbox(bus_evento_id)`;
    debe crearse después de 10.

No hay ciclos entre los objetos de este paquete. `fk_firma_resultado` (paso
5) es la única dependencia que obliga a partir la creación/alteración de
`fal_documento_firma` en dos sentencias no consecutivas. `fal_inbox_error`
(paso 11) es la única tabla de este bloque con FK hacia otra tabla nueva
del mismo bloque. Tras el paso 11,
`verify-firmas-eda-closure-001-postcheck.sql` y
`verify-firmas-eda-closure-001-parity.sql` confirman el esquema resultante
— sin privilegios runtime todavía (paso 12 abajo).

**Sobre grants de aplicación (`database/ddl/grants-firmas-eda-bus.sql`),
después de 1-11 completo y del postcheck estructural:**

12. `database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql`
    confirma que los 5 usuarios `bus_*` de aplicación ya existen, con host
    permitido y `REQUIRE SSL`; si falla, no se ejecuta ningún `GRANT`.
13. `GRANT INSERT` (column-level, sin `bus_evento_id`)/`GRANT SELECT`/
    `GRANT SELECT, DELETE` de `grants-firmas-eda-bus.sql` sobre
    `bus_evento` (§10) — grants directos, sin rutinas ni definer
    intermedios.
14. `verify-firmas-eda-closure-001-postcheck.sql` sección de seguridad y
    permisos (**postcheck runtime**): privilegios exactos de las 5 cuentas
    sobre `bus_evento`, y ausencia de cualquier privilegio de esas cuentas
    sobre `fal_outbox`, `fal_bus_checkpoint`, `fal_inbox` o
    `fal_inbox_error`.

## 13. Impacto sobre datos existentes y estrategia de compatibilidad

El esquema del dominio Faltas (`database/ddl/create-bod-faltas-domain.sql`)
todavía no fue creado en ningún ambiente (`Esquema aún no creado`, según su
propio encabezado). Por lo tanto, **no existen filas productivas** de
`fal_documento_firma_req`, `fal_documento_firma` ni `fal_documento` sobre
las cuales aplicar backfill real en este momento. El backfill de §1/§2/§3
se documenta como estrategia de compatibilidad para el escenario en que el
baseline se ejecute primero y esta extensión se aplique después sobre una
base con datos de prueba o productivos ya cargados; no se inventa ningún
dato jurídico ni criptográfico. Si ambos scripts (baseline + esta
extensión) se ejecutan en secuencia sobre un esquema vacío, el backfill no
tiene efecto observable (no hay filas que actualizar).

## 14. Plan de rollback y recuperación de ejecución parcial

`database/ddl/alter-firmas-eda-closure-001-rollback.sql` revierte en orden
inverso al de §12:

`DROP TABLE fal_inbox_error`, `DROP TABLE fal_inbox`,
`DROP TABLE fal_bus_checkpoint`, `DROP TABLE bus_evento`,
`DROP TABLE fal_outbox`,
`ALTER TABLE fal_documento_firma DROP CONSTRAINT fk_firma_resultado`,
`DROP TABLE fal_documento_firma_resultado`,
`ALTER TABLE fal_documento DROP COLUMN revision_artefacto`,
`ALTER TABLE fal_documento_firma DROP COLUMN ...` (todas las columnas de
§2, en orden inverso de creación) y
`ALTER TABLE fal_documento_firma_req DROP COLUMN ...` (todas las columnas
de §1, en orden inverso). El rollback de grants de aplicación
(`database/ddl/rollback-grants-firmas-eda-bus.sql`, §10) se ejecuta antes
que este script (revoca los 6 grants directos sobre `bus_evento` que este
script luego elimina).

**El rollback nunca se declara completo de forma incondicional.** Antes de
ejecutarlo, `database/diagnostics/verify-firmas-eda-closure-001-rollback-precheck.sql`
debe abortar (reportar `BLOQUEADO`, no ejecutar el rollback) si:

- existen filas con `estado_firma_req` entre `6` y `9` (estados nuevos sin
  representación en el catálogo de 5 valores original);
- existen filas en `fal_outbox`, `bus_evento`, `fal_inbox`,
  `fal_inbox_error` o `fal_documento_firma_resultado` (cualquier dato en
  las tablas nuevas se perdería sin posibilidad de reconciliación);
  `fal_bus_checkpoint` se verifica de forma especial: debe tener
  exactamente una fila con `ultimo_evento_id = 0` (checkpoint sin avanzar,
  nunca usado en operación real);
- alguna columna nueva de `fal_documento_firma`/`fal_documento_firma_req`/
  `fal_documento` contiene un valor no `NULL`/no `0` que no sea
  descartable (evidencia de que ya se ejecutó código productivo
  dependiente de esas columnas).

**Rollback de Firmas, precheck independiente:** el paquete de Firmas (§8)
tiene su propio
`database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-rollback-precheck.sql`,
que **no reutiliza ni referencia** el precheck de creación
(`verify-firmas-eda-outbox-inbox-precheck.sql`): ese precheck de creación
valida que las tablas **no** existan todavía (precondición de un
`CREATE`), exactamente lo opuesto de lo que un rollback necesita
confirmar. El precheck de rollback de Firmas aborta (`BLOQUEADO`) si:

- `fir_outbox`, `fir_inbox` o `fir_inbox_error` tienen al menos una fila;
- `fir_bus_checkpoint` no tiene exactamente una fila con
  `ultimo_evento_id = 0`;
- `fir_outbox`/`fir_bus_checkpoint`/`fir_inbox`/`fir_inbox_error` no
  existen en el estado físico esperado (columnas/tipos coincidentes con
  `create-firmas-eda-outbox-inbox.sql`) — un rollback sobre un esquema que
  no coincide con lo que el forward de Firmas creó no es seguro de
  ejecutar a ciegas;
- no hay una base de datos seleccionada (`DATABASE() IS NULL`), mismo
  patrón que el precheck de creación (§8).

**Recuperación de una ejecución parcial del forward** (el script se
interrumpió a mitad de camino, ej. por error de red o desconexión del
cliente):

1. identificar el último `ALTER`/`CREATE` que efectivamente se aplicó,
   consultando `information_schema.columns`/`information_schema.tables`
   contra el inventario de §1-§6 (mismo patrón que
   `verify-firmas-eda-closure-001-precheck.sql`);
2. si el punto de interrupción está dentro de una única sentencia `ALTER
   TABLE ... ADD COLUMN a, ADD COLUMN b, ...` (MariaDB aplica cada
   `ALTER TABLE` completo como una operación atómica de metadatos DDL, no
   columna por columna), esa sentencia no deja estado parcial: o se aplicó
   entera o no se aplicó ninguna columna de esa sentencia;
3. si el punto de interrupción está entre dos sentencias `CREATE
   TABLE`/`ALTER TABLE` independientes, existen dos caminos válidos,
   nunca una tercera opción de "reintentar el script completo a ciegas":
   - **completar el forward**: re-ejecutar únicamente las sentencias
     posteriores al último objeto confirmado (requiere edición manual
     puntual del script para esa corrida, documentada como evidencia de
     ejecución); o
   - **rollback parcial seguro**: ejecutar manualmente, en orden inverso,
     solo los `DROP`/`ALTER ... DROP COLUMN` correspondientes a los
     objetos que sí llegaron a crearse, verificando cada uno contra
     `verify-firmas-eda-closure-001-precheck.sql` antes de continuar;
4. **nunca** reejecutar `alter-firmas-eda-closure-001-forward.sql` completo
   sin haber confirmado primero cuáles de sus sentencias ya se aplicaron
   (el script no es idempotente: un `ADD COLUMN`/`CREATE TABLE` sobre un
   objeto ya existente falla con error de SQL, no es un no-op).

El rollback es seguro únicamente si no se ejecutó código productivo que ya
dependa de las columnas/tablas nuevas (este cierre es documental;
`IMPLEMENTATION_NOT_STARTED`).

**Rollback de grants:**
`database/ddl/rollback-grants-firmas-eda-bus.sql` revoca exactamente lo
que otorgó `grants-firmas-eda-bus.sql` (§10), en orden inverso, sin tocar
la existencia de los usuarios MariaDB (que no fueron creados por este
paquete — §10, §14):

```sql
REVOKE SELECT, DELETE ON bus_evento FROM bus_maintenance;
-- ... (una linea por cada GRANT otorgado, en orden inverso exacto)
REVOKE INSERT (evento_id, productor_codigo, evento_codigo, schema_version,
    agregado_codigo, agregado_id, agregado_version, correlation_id,
    causation_id, payload, ocurrido_en, publicado_en)
    ON bus_evento FROM bus_publisher_faltas;
```

No incluye `DROP USER`: la baja de un usuario MariaDB es una decisión de
aprovisionamiento (secret manager/pipeline), no una operación de este
paquete de DDL de dominio. Es ejecutable de forma independiente al
rollback de tablas (§14): revocar permisos no requiere que las tablas ya
hayan sido eliminadas, pero por orden operativo (§12, inverso del
`database/README.md` "Orden operativo") se ejecuta antes, para no dejar
una ventana en la que los objetos ya no existan pero los `GRANT` todavía
los referencien.

## 15. Límites HTTP y rotación de credenciales (`DEC-EDA-018`, complemento `DEC-EDA-040`)

Aplican a los tres endpoints de `../30-integrations/firma-binary-transfer.md`
(`preparar-artefacto`, `GET .../artefacto`, `PUT .../resultados/{id}`),
autenticados por `X-Api-Key`:

| Endpoint | Límite | Burst |
|---|---|---|
| `POST preparar-artefacto` | 60 req/min por credencial | 10 |
| `GET .../artefacto` | 120 req/min por credencial | 20 |
| `PUT .../resultados/{resultadoFirmaId}` | 20 req/min por credencial | 5 |

Exceder el límite responde `429 Too Many Requests` con cabecera
`Retry-After` (segundos hasta la próxima ventana disponible); el cuerpo de
la respuesta sigue el mismo formato de error estándar que el resto de la
API (`../40-api/` convenciones de error, sin campos adicionales
inventados para este caso). El límite se aplica por credencial
(`X-Api-Key`), no por IP: dos instancias de Firmas detrás del mismo
egress compartido no comparten cupo si usan credenciales distintas, y una
única credencial no obtiene cupo adicional por distribuirse en varias
instancias.

**Rotación de `X-Api-Key` (`DEC-EDA-018`, complemento):** cada 90 días;
ventana de solapamiento de 24 horas durante la cual la credencial anterior
y la nueva son ambas válidas (permite el corte de tráfico sin downtime
coordinado entre Faltas y Firmas). Vencida la ventana de solapamiento, la
credencial anterior se invalida de forma permanente; no hay una tercera
credencial "de respaldo" simultánea.

Estamos retomando el repo multiproyecto del sistema de faltas municipal.

## Regla general de trabajo

- Continuar en español.
- No rediseñar desde cero.
- Usar `spec/` como fuente principal de verdad.
- Mantener estilo compacto, claro, navegable y orientado a `spec-as-source`.
- No volver a discutir fundamentos ya cerrados salvo contradicción real.
- Priorizar correcciones quirúrgicas y archivos completos copy-paste cuando haga falta.

---

## Estado general al retomar

Se consolidó una pasada muy amplia de alineación entre el bloque `spec/13-ddl/` y el resto de la spec.

Ya se revisaron y alinearon, con distinta profundidad:

- `01-dominio`
- `02-reglas-transversales`
- `03-bandejas`
- `04-backend`
- `09-integraciones`
- `12-datos`

También se revisó el `spec` completo en zip para detectar contradicciones globales.

El resultado general fue bueno:
- no se detectaron contradicciones estructurales graves nuevas
- sí se hicieron varias alineaciones importantes con el DDL final

---

## Decisiones muy importantes ya firmes

### Núcleo
- `Acta` es la unidad principal de gestión.
- `ActaEvento` es append-only.
- `Observacion` es tabla transversal.
- `ActaSnapshot` quedó como **una única proyección operativa principal**, simple, directa y regenerable.
- `ActaSnapshot` debe vivir solo en `09-tablas-snapshot-auxiliares-y-proyecciones.md`, no duplicada en `02`.

### Acta
- `NroActa` no puede ser nulo en base central.
- hay que contemplar GPS en `Acta`:
  - `LatInfr`
  - `LonInfr`
- hay que contemplar evidencias:
  - `ActaEvidencia`

### Documental
- `Documento` es entidad lógica desacoplada del archivo físico.
- storage se resuelve por `StorageKey`.
- el archivo firmado reemplaza al borrador; no se conservan ambas versiones en el circuito documental principal.
- `IdPolNum` sobra en `Documento`.
- `TipoFirmaReq` reemplaza flags tipo `SiFirmaDigital` / `SiFirmaOlografa`.
- `DocumentoFirma` quedó minimalista y usa `IdUserFirma`.

### Notificación
- toda notificación recae sobre un documento del expediente
- toda notificación pertenece a una acta
- `NotificacionDestino` fue eliminado
- cada `NotificacionIntento` tiene un único destino efectivo
- `Notificacion` conserva estado resumido de acuse

### Numeración
- no usar máscara libre
- política por componentes:
  - prefijo
  - año
  - serie
  - número
- separadores por componente:
  - `SepPrefAnio`
  - `SepAnioSerie`
  - `SepSerieNro`
- talonarios:
  - `ELECTRONICO`
  - `MANUAL_FISICO`
- `TalonarioMovimiento` resuelve el estado del número manual

### Referenciales/versionado
- `Dependencia` / `DependenciaVersion`
- `Inspector` / `InspectorVersion`
- `Alcoholimetro` / `AlcoholimetroVersion`
- `RubroCom` / `RubroComVersion`

### Contravención
- agregar:
  - `IdSuj`
  - `IdBie`
- `OrigenNomencl`:
  - `OBTENIDA_COMERCIO`
  - `OBTENIDA_INMUEBLE`
  - `INGRESADA_MANUAL_VALIDADA`

### Storage documental
Se definió el bloque:
- `StorageBackend`
- `StoragePolitica`
- `StorageObjeto`

Con política de resolución por:
- sistema
- familia
- tipo de objeto
- fallback a política general
- fallback a backend default

Ruta relativa recomendada:
`/{sistema}/{familia}/{tipo}/{anio}/{mes}/{bucket}/{ref_negocio}/{storage_key}.{ext}`

---

## Bloques ya revisados

### `01-dominio`
Se alineó con:
- GPS en acta
- evidencias
- documento desacoplado por `StorageKey`
- notificación siempre sobre documento
- snapshot operativo simplificado
- talonarios más realistas

### `02-reglas-transversales`
Se completaron los archivos vacíos:
- `04-reglas-de-reingreso.md`
- `05-reglas-de-bandejas-y-transiciones.md`
- `06-reglas-de-medidas-preventivas.md`
- `07-reglas-de-pendientes-materiales.md`

### `03-bandejas`
Se hicieron ajustes finos de alineación con:
- snapshot
- notificación
- firma
- gestión externa
- pendientes materiales

### `04-backend`
Se alineó con:
- snapshot único
- documental simplificado
- firma externa mínima
- notificación simplificada
- storage explícito
- numeración real
- gestión externa proyectable

### `09-integraciones`
Se alineó con:
- motor de firma externo
- notificación por documento
- storage documental desacoplado
- autenticación con IdP del ecosistema

### `12-datos`
Se alineó con:
- `Acta`
- `Documento`
- `Notificacion`
- `Snapshot`
- `Catálogos y maestros`

---

## Revisión global del spec

Se hizo una pasada global al `spec` completo.

Conclusión:
- la consistencia general ya es buena
- no conviene otra gran ronda transversal completa
- solo quedan pendientes finos y el siguiente paso fuerte

---

## Punto exacto donde se cortó

Se intentó avanzar con el **diagrama relacional**.

Se propusieron diagramas por bloques y un mapa general.

Problema encontrado:
- los diagramas Mermaid dieron error en preview

Conclusión operativa:
- dejar el tema de diagramas para mañana
- revisar formato
- probablemente simplificar sintaxis / tipos o cambiar estrategia

---

## Próximo paso exacto al retomar

1. revisar cómo conviene resolver los diagramas relacionales:
   - Mermaid simplificado
   - otra sintaxis más compatible
   - o un nivel menor de detalle
2. generar versión usable/visible del diagrama relacional
3. después pasar a:
   - SQL de creación
   - o ajuste final previo si el diagrama revela algo

---

## Criterio al continuar

- No rehacer la spec transversal ya corregida.
- No abrir otra gran ronda de revisión global salvo que aparezca una contradicción real.
- Concentrarse ahora en:
  - diagramas relacionales
  - y luego SQL de creación.
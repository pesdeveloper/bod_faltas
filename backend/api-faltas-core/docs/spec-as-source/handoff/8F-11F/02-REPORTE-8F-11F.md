# Handoff 8F-11F -- Reporte de Implementacion

## Resumen ejecutivo

Slice 8F-11F implementado completamente. Fallo, firmeza y apelacion ahora estan ALINEADOS con el modelo MariaDB.

Build: 1976 tests, 0 failures, BUILD SUCCESS.

---

## 1. Entidades implementadas/completadas

### FalActaFallo (completado)
- siFirme, fhFirmeza, origenFirmeza (firmeza inline, D1 cerrada)
- siVigente, falloReemplazadoId (multi-fallo con historial)
- fhVtoApelacion (fecha de vencimiento del plazo de apelacion)
- resultadoFallo (ResultadoFalloActa enum)
- documentoId (id del documento del fallo)
- versionRow (optimistic locking)
- Auditoria completa: fhAlta, idUserAlta, fhUltMod, idUserUltMod

### FalActaApelacion (completado)
- canalApelacion (CanalApelacion enum)
- tipoPresentacion (TipoPresentacion enum)
- textoApelacion (texto largo nullable)
- estadoApelacion (EstadoApelacionActa enum)
- resultadoResolucion (ResultadoResolucionApelacion enum nullable)
- fhResolucion, idUserResolucion, documentoResolucionId
- versionRow (optimistic locking)
- Auditoria completa

### FalActaApelacionDocumento (NUEVO)
- id, versionRow
- apelacionId (FK a FalActaApelacion)
- tipoDocApelacion (TipoDocumentoApelacion enum)
- origenPresentacion (OrigenPresentacion enum)
- documentoId (FK a FalDocumento, nullable)
- storageKey, nombreArchivo, mimeType, tamanioBytes
- Auditoria: fhAlta, idUserAlta

### FalActaFirmezaCondena (DTO no persistible)
- Transformado de entidad persistible a DTO de lectura
- Se construye desde FalActaFallo + FalActaApelacion
- FirmezaCondenaRepository deprecado (fachada de compatibilidad backward)

---

## 2. Enums nuevos/actualizados

| Enum | Codigos | Slice |
|------|---------|-------|
| OrigenFirmezaCondena | 1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA | 8F-11F |
| ResultadoResolucionApelacion | 1=RECHAZADA, 2=ACEPTADA_ABSUELVE, 3=MODIFICA_CONDENA, 4=NULIDAD | 8F-11F |
| TipoPresentacion | 1=TEXTO, 2=DOCUMENTOS, 3=MIXTA | 8F-11F |
| CanalApelacion | 1=PRESENCIAL, 2=ELECTRONICO, 3=POSTAL, 4=CORREO_ELECTRONICO | 8F-11F |
| TipoDocumentoApelacion | 1-8 (ESCRITO_APELACION..OTRO) | 8F-11F |
| OrigenPresentacion | 1-6 (INFRACTOR..INTEGRACION_EXTERNA) | 8F-11F |
| EstadoApelacionActa | actualizado: PRESENTADA, EN_ANALISIS, RESUELTA | 8F-11F |
| ResultadoFalloActa | ABSOLUTORIO=1, CONDENA=2 | 8F-11F |
| ResultadoFinalActa | NULIDAD=5 agregado | 8F-11F |

---

## 3. Repositories nuevos/actualizados

### FalloActaRepository (actualizado)
- findVigenteByActaId() -- unico vigente por acta
- guardarComoVigente() -- atomico, reemplaza si ya existe vigente
- rechazarSiYaExisteVigente() -- guardrail de integridad
- findByActaId() -- historial completo
- buscarActivo() -- backward compat (delega a findVigenteByActaId)

### ApelacionActaRepository (actualizado)
- findByFalloId() -- apelaciones por fallo
- buscarActiva() -- PRESENTADA o EN_ANALISIS
- buscarUltima() -- ultima por actaId

### ApelacionDocumentoRepository (NUEVO)
- save(), findById(), findByApelacionId()
- append-only, thread-safe, copias defensivas

### InMemoryFirmezaCondenaRepository (deprecado)
- Fachada de compatibilidad. Lee desde InMemoryFalloActaRepository.
- No persiste nada.

---

## 4. Services actualizados

### FalloActaService
- dictarCondenatorio() -- genera documento via DocumentoService
- dictarAbsolutorio() -- genera documento via DocumentoService
- La firmeza se delega a FirmezaCondenaService

### ApelacionActaService
- registrarApelacion() -- 3 tipos de presentacion
- registrarDocumento() -- para DOCUMENTOS y MIXTA
- pasarAAnalisis()
- resolverRechazada() -- APERAZ
- resolverAceptaAbsuelve() -- APEABS + cierre si no hay bloqueantes
- resolverModificaCondena() -- APEMCO + FALRMP + nuevo fallo sustitutivo
- resolverNulidad() -- APENUL + desactiva fallo anterior (no cierra acta)

### FirmezaCondenaService
- vencerPlazoApelacion() -- precondicion: no apelacion activa
- declararFirmePorApelacionRechazada() -- precondicion: apelacion RESUELTA/RECHAZADA

### SnapshotRecalculador
- Proyecta fhVtoApelacion desde fallo vigente
- Determina bandeja CON_APELACION si apelacion activa
- Determina CONDENA_FIRME desde fallo vigente siFirme=true

---

## 5. Commands nuevos

- ResolverApelacionModificaCondenaCommand(apelacionId, nuevoMontoCondena, fundamentosResolucion, idUserResolucion)
- ResolverApelacionNulidadCommand(apelacionId, fundamentosResolucion, idUserResolucion)
- PasarApelacionAAnalisisCommand(apelacionId, idUserOperacion)
- RegistrarApelacionCommand actualizado: canalApelacion, tipoPresentacion, textoApelacion, idUserRegistro
  - legacy() factory method para backward compat

---

## 6. Tests nuevos

### FalloInvariantesTest (13 tests)
- Historial multi-fallo (3 fallos, solo uno vigente)
- Unicidad de vigente (guardar 2 vigentes lanza excepcion)
- Firmeza valida (VENCIMIENTO, APELACION_RECHAZADA)
- Firmeza invalida (ya firme, apelacion pendiente)
- Optimistic locking fallo

### ResolucionApelacionEfectosTest (11 tests)
- Rechazada: estado RESUELTA, condena no cerrada, evento APERAZ
- Aceptada absuelve: ABSUELTO, CERRADA, eventos APEABS+CIERRA
- Modifica condena: nuevo fallo vigente, fallo anterior REEMPLAZADO
- Nulidad: NULIDAD, fallo desactivado, no cierra

### ApelacionDocumentoTest (15 tests)
- Guardar y consultar documentos de apelacion
- Tipo, origen, storage
- Limites de longitud
- Rechazo de fila vacia
- Listado ordenado

### FalloApelacionConcurrenciaTest (10 tests)
- Dos fallos intentan quedar vigentes: uno gana, otro lanza excepcion
- Dos reemplazos concurrentes del mismo fallo
- Dos resoluciones simultaneas de la misma apelacion
- Firmeza vs apelacion concurrentes
- Apelacion DOCUMENTOS/MIXTA sin dejar cabecera huerfana

---

## 7. Seeders demo actualizados

| Codigo | Descripcion |
|--------|-------------|
| ACT-032-APELACION-CON-DOCUMENTOS | Apelacion tipo DOCUMENTOS con ESCRITO_APELACION |
| ACT-033-APELACION-MIXTA | Apelacion MIXTA: texto + DOCUMENTACION_RESPALDATORIA |
| ACT-034-APELACION-RECHAZADA | Resolucion RECHAZADA, condena pendiente de firmeza |
| ACT-035-APELACION-ABSOLUTORIA | ACEPTADA_ABSUELVE, acta CERRADA ABSUELTO |
| ACT-036-APELACION-MODIFICA-CONDENA | MODIFICA_CONDENA: nuevo fallo vigente 2500 |
| ACT-037-APELACION-NULIDAD | NULIDAD declarada, fallo desactivado, acta activa |

---

## 8. Paridad post-8F-11F

| Tabla | Estado | Observacion |
|-------|--------|-------------|
| fal_acta_fallo | ALINEADO | Completado en 8F-11F |
| fal_acta_apelacion | ALINEADO | Completado en 8F-11F |
| fal_acta_apelacion_documento | ALINEADO | Nuevo en 8F-11F |
| fal_acta | PARCIAL | Pendiente: auditoria completa |
| fal_acta_evento | PARCIAL | Pendiente: auditoria completa |
| fal_acta_obligacion_pago | SEMANTICA_INCOMPATIBLE | D2 cerrada (8F-11H) |
| fal_acta_documento | RELACION_INCOMPLETA | GAP-12 (8F-11J) |
| stor_* (3 tablas) | NO_PERSISTIBLE | Fuera del dominio Faltas |
| 11 tablas | FALTA_EN_INMEMORY | fal_observacion, fal_acta_paralizacion, etc. |

Total: 44 ALINEADO + 2 PARCIAL + 11 FALTA_EN_INMEMORY + 1 SEMANTICA + 1 RELACION + 3 NO_PERSISTIBLE = 62.
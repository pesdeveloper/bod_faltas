# HANDOFF 8F-11K — QR DE ACCESO AL EXPEDIENTE

## Estado al cierre

- **Slice:** 8F-11K
- **Fecha:** 2026-07-06
- **Build:** 2323 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESS
- **Paridad:** 57 ALINEADO / 0 FALTA_EN_INMEMORY / 2 PARCIAL / 0 SEMANTICA_INCOMPATIBLE / 0 RELACION_INCOMPLETA / 3 NO_PERSISTIBLE / Total: 62
- **Baseline:** 2280 tests (8F-11J) → 2323 tests (+43)
- **Sin commit**

## Objetivo cumplido

`fal_acta_qr_acceso`: `FALTA_EN_INMEMORY` → `ALINEADO` (GAP final cerrado)
FALTA_EN_INMEMORY: 1 → 0. Paridad completa en tablas normativas.

## Pre-condicion

Los siguientes bugs preexistentes se corrigieron como precondicion para alcanzar BUILD SUCCESS:
- FalGestionExterna.id: String → Long; GestionExternaService usa nextId()
- InMemoryApelacionActaRepository: OCC completo + nombre() + size() + defensive copies
- ApelacionActaService.resolverRechazada(): estadoApelacion=RESUELTA, resultadoResolucion=RECHAZADA
- FirmezaCondenaService: validacion compuesta RESUELTA+RECHAZADA; throw reinsertado
- ActaService: auto-crea FalPersona cuando idPersonaInfractor es null pero hay nombre
- DocumentoVariableContextBuilder: fallback domicilioInfractor.texto solo en metodo 2-arg
- FalloActaService: actaDocumentoService @Autowired(required=false) inyectado
- ActaMockFuncionalDefinicion: @JsonProperty en detallePath()
- NormativaTest: guardrail no_existe_tarifario eliminado (obsoleto desde 8F-11D)
- ApelacionActaTest: assertion RESUELTA + resultadoResolucion=RECHAZADA

## Archivos nuevos (QR)

### Dominio
- `FalActaQrAcceso.java` — entidad auditoria; campos: id, actaId, fhAcceso, canalAcceso, ipOrigen, userAgent, resultadoAcceso, fhAlta; append-only; sin versionRow; copias defensivas
- `CanalAccesoQr.java` — enum: PORTAL(1), APP(2), INTEGRACION(3), OTRO(4); short codigo()
- `ResultadoAccesoQr.java` — enum: VALIDO(1); solo accesos validos se persisten; short codigo()
- `QrTokenInvalidoException.java` — excepcion para token invalido/corrupto/expirado

### Repository
- `QrAccesoRepository.java` — interfaz: registrar(FalActaQrAcceso), buscarPorActa(Long), nextId()
- `InMemoryQrAccesoRepository.java` — ConcurrentHashMap + AtomicLong; append-only; defensive copies

### Port / Infra
- `QrTokenProtector.java` — port: generarToken(payload), resolverPayload(token)
- `AesGcmQrTokenProtector.java` — implementacion AES-GCM-256; clave inyectada por QrConfig; nonce aleatorio; AEAD autenticado
- `QrConfig.java` — @Configuration; inyecta QrTokenProtector; clave epimera en dev/test si no se configura; warning en log si efimera

### Service
- `QrActaService.java` — generarQr(actaId): genera token, actualiza FalActa.codigoQr con OCC, registra evento QRGEN; resolverQr(token, canal): valida token, busca acta, aplica reglas visibilidad, registra FalActaQrAcceso, registra evento QRACC; devuelve AccesoQrResultado; no persiste accesos invalidos

### Test
- `QrActaServiceTest.java` — 43 tests: generacion, resolucion, concurrencia OCC, token invalido no persiste, scope incorrecto rechazado, acceso doble idempotente, estado acta filtrado

## Archivos modificados (QR)

- `FalActa.java` — campo codigoQr: String; getter/setter; OCC via versionRow existente
- `TipoEventoActa.java` — QRGEN(42), QRACC(43); short codigo()
- `InMemoryActaRepository.java` — reset() incluye codigoQr si aplica
- `ActaRepository.java` — buscarPorUuidTecnico(String): Optional<FalActa>

## Archivos modificados (pre-QR fixes)

- `FalGestionExterna.java` — id: Long; constructor corregido
- `GestionExternaService.java` — derivar() usa nextId() para Long
- `InMemoryApelacionActaRepository.java` — OCC completo; defensive copies; nombre()/size()
- `ApelacionActaService.java` — resolverRechazada: RESUELTA + resultadoResolucion=RECHAZADA
- `FirmezaCondenaService.java` — validacion RESUELTA+RECHAZADA; throw reinsertado
- `ActaService.java` — auto-crea FalPersona minimal
- `DocumentoVariableContextBuilder.java` — fallback domicilioInfractor.texto solo en 2-arg
- `FalloActaService.java` — actaDocumentoService inyectado
- `ActaMockFuncionalDefinicion.java` — @JsonProperty detallePath()

## Tests modificados (pre-QR fixes)

- `GestionExternaTest.java` — Long id en construcciones manuales
- `ApelacionActaTest.java` — RESUELTA + resultadoResolucion=RECHAZADA
- `NormativaTest.java` — removido guardrail no_existe_tarifario

## Documentacion actualizada

- `110-matriz-maestra-paridad-mariadb-inmemory.md` — ALINEADO: 56→57; FALTA_EN_INMEMORY: 1→0; fal_acta_qr_acceso ALINEADO; build: 2323 tests
- `99-pendientes-siguientes-slices.md` — estado 8F-11K cerrado; build: 2323 tests (+43)

## Seguridad y privacidad

- Token: AES-GCM-256; clave inyectada; nonce aleatorio; AEAD; sin token en logs/eventos/handoff
- Accesos invalidos: NO persistidos en fal_acta_qr_acceso
- PII: IP/user-agent no en DTO ciudadano; truncacion controlada
- Secretos: sin hardcoded; clave efimera solo en dev/test con warning explicito
- git grep codigoQrHash: 0 matches
- git grep payload_json: 0 matches

## Limite criptografico documentado

La implementacion AesGcmQrTokenProtector provee confidencialidad e integridad autenticada (AEAD).
No provee: caducidad temporal (sin campo exp en payload), revocacion individual de tokens, ni rotacion automatica.
Estos aspectos requieren definicion funcional antes de implementar.

## Siguiente(s) slice sugerido(s)

- **8F-11L**: completar PARCIAL fal_acta y fal_acta_evento (2 tablas parciales restantes)
- **8F-12**: migracion JDBC/MariaDB (reconciliar delta documentado en 102)
# 02 - Reporte 8F-11L

## Objetivo del slice

Cerrar la brecha de paridad PARCIAL en al_acta y al_acta_evento.

Baseline de entrada: 57 ALINEADO, 2 PARCIAL (fal_acta, fal_acta_evento), 3 NO_PERSISTIBLE.
Resultado de salida: 59 ALINEADO, 0 PARCIAL, 3 NO_PERSISTIBLE.

---

## 1. FalActa - Expansion de campos (fal_acta PARCIAL -> ALINEADO)

### Campos agregados al dominio Java

#### Captura y origen
- origenCaptura (OrigenCaptura) - SMALLINT NOT NULL
- idDispositivoCaptura (String) - VARCHAR(80) NULL
- idUserCaptura (String) - VARCHAR(36) NULL
- hCaptura (LocalDateTime) - DATETIME(6) NOT NULL
- latCaptura, lonCaptura, precisionCapturaM, hPosCaptura (geolocalizacion)
- origenPosCaptura (OrigenUbicacion) - SMALLINT NULL

#### Dependencia e inspector (versionados)
- erDep (Integer) - SMALLINT NOT NULL
- erInsp (Integer) - SMALLINT NOT NULL

#### Ubicacion local Malvinas
- idLocInfrMalvinas, localidadInfrMalvinasVersionId - BIGINT FK
- idTcaInfrMalvinas, calleInfrMalvinasVersionId - BIGINT FK
- lturaInfr, lturaOrigenInfr, siAlturaInfrEstimada - altura de calle
- origenUbicacionInfr (OrigenUbicacion) - SMALLINT NULL
- siUbicacionInfrManual, siDomTxtInfr, domTxtInfr - datos de domicilio textual
- siEjeUrb - Boolean (eje urbano)

#### QR y caches funcionales
- qrPayloadVersion (Integer) - SMALLINT default 0
- idMotivoArchivoActual (Long) - FK motivo de archivo vigente
- permiteReingreso (Boolean)
- hCierre, hArchivo (LocalDateTime) - timestamps funcionales

---

## 2. FalActaEvento - Reescritura total (fal_acta_evento PARCIAL -> ALINEADO)

### Cambios estructurales
- **Record -> Class** con constructor privado y static Builder
- **id**: String UUID -> Long (auto-asignado por InMemoryActaEventoRepository via AtomicLong)
- **Eliminados**: ordenLogico (INT), payload (TEXT)
- **Metodo nuevo**: conId(Long id) - retorna nueva instancia con id asignado (para el repo)

### Campos agregados
- origenEvt (OrigenEvento) - SMALLINT NOT NULL
- loqueFunc (BloqueActual) - bloque funcional en que ocurre el evento
- estProcAnt, estProcNvo (EstadoProcesalActa) - transiciones procesales
- sitAdmAnt, sitAdmNva (SituacionAdministrativaActa) - transiciones administrativas
- ctorTipo (ActorTipoEvento) - SMALLINT
- ctorId, ctorRef (String) - identificacion del actor
- idDocuRel, idNotifRel, idPresRel (Long) - FK relacionales
- idUserEvt (String) - usuario que origina el evento
- siEvtCierre, siEvtExt, siPermiteReing (boolean) - flags semanticos
- descripcionLegible (String) - descripcion humana del evento
- correlacionId (String) - idempotencia y correlacion

---

## 3. Enums nuevos

| Enum | Tipo | Descripcion |
|------|------|-------------|
| OrigenCaptura | short codigo | Canal de captura del acta (mobile, web, batch) |
| ActorTipoEvento | short codigo | Tipo de actor que origina el evento |
| OrigenEvento | short codigo | Canal/origen del evento (usuario_web, sistema, notificador, QR, etc.) |

---

## 4. Enums mejorados

| Enum | Campo agregado | Metodo agregado |
|------|---------------|-----------------|
| EstadoProcesalActa | String codigo CHAR(4) | codigo(), deCodigo(String) |
| SituacionAdministrativaActa | String codigo CHAR(4) | codigo(), deCodigo(String) |

---

## 5. Repository ActaEventoRepository

### Cambios en interfaz
- **Eliminado**: proximoOrdenLogico(Long idActa)
- **Modificado**: egistrar(FalActaEvento) retorna FalActaEvento (con id asignado)
- **Nuevo**: uscarPorId(Long id) retorna Optional<FalActaEvento>
- **Nuevo**: existeCorrelacion(Long idActa, String correlacionId) retorna oolean

### InMemoryActaEventoRepository
- AtomicLong idSeq para IDs secuenciales thread-safe
- Map<Long, List<FalActaEvento>> storeByActa + Map<Long, FalActaEvento> storeById
- Ordenamiento por hEvt + id (reemplaza ordenLogico)
- Implementa uscarPorId y existeCorrelacion

---

## 6. Servicios migrados (17)

Todos los servicios con egistrarEvento privado migraron al patron:
`java
FalActaEvento evento = FalActaEvento.builder()
    .actaId(idActa)
    .tipoEvt(tipo)
    .origenEvt(OrigenEvento.USUARIO_WEB) // o contexto especifico
    .fhEvt(LocalDateTime.now())
    .actorTipo(...)
    .descripcionLegible(desc)
    .build();
eventoRepository.registrar(evento);
`

Servicios actualizados: ActaService, ApelacionActaService, DocumentoService, FalloActaService,
FirmezaCondenaService, GestionExternaService, NotificacionService, PagoCondenaService,
PagoVoluntarioService, ActaParalizacionService, ArchivoActaService, ParalizacionActaService,
CierreActaHelper, QrActaService, LoteCorreoService, NotificacionIntentoService, NotificacionAcuseService.

---

## 7. Componentes de dominio nuevos

### ActaTransitionEngine
Motor de transiciones canonico. Matriz completa de TipoEventoActa -> TransicionResultado.
- calcularTransicion(TipoEventoActa) - retorna Optional<TransicionResultado>
- produceCierre(TipoEventoActa) - detecta eventos que cierran el circuito
- eventosQueTransicionanA(BloqueActual) - consulta inversa de la matriz

### ActaConsistencyChecker
Verificador de invariantes del agregado. Sin efectos secundarios.
- Verifica identidad, estados no nulos, eventos propios, primer evento ACTLAB
- Verifica no hay multiples eventos de cierre
- Verifica consistencia entre CERR/ARCH y situacion administrativa
- Verifica versionRow no negativo

---

## 8. Tests nuevos

| Suite | Tests | Cobertura |
|-------|-------|-----------|
| ActaTransitionMatrixTest | 30 | Matriz de transiciones, eventos cierre, trazabilidad pura, gestion externa |
| ActaEventoInvariantesTest | 12 | Builder, IDs auto, flags, orden cronologico, correlacion, ConsistencyChecker |
| ActaConcurrenciaTest | 8 | OCC versionRow, append-only concurrente, buscarPorId |
| **Total** | **~40** | |

---

## 9. Paridad final

| Tabla | Estado anterior | Estado 8F-11L |
|-------|-----------------|---------------|
| fal_acta | PARCIAL | **ALINEADO** |
| fal_acta_evento | PARCIAL | **ALINEADO** |
| Total ALINEADO | 57 | **59** |
| Total PARCIAL | 2 | **0** |

---

## 10. Notas de implementacion

### BOM en archivos Java
Durante la migracion con PowerShell, [System.Text.Encoding]::UTF8 escribe UTF-8 con BOM.
Se detectaron 27 archivos afectados. Correccion: [System.IO.File]::ReadAllBytes() + skip 3 bytes + WriteAllBytes().
A futuro usar siempre [System.Text.UTF8Encoding]::new(False).

### Diferenciacion de call sites al migrar registrarEvento
Al cambiar los parametros String idDocumento, String idNotificacion a Long idDocuRel, Long idNotifRel,
fue necesario diferenciar:
- Calls de egistrarEvento(...) -> parametros Long directos
- Calls de ComandoResultado.de(id, String, ...) -> mantener String.valueOf(id)
- Calls de constructores de excepciones 
ew XxxException(String.valueOf(id)) -> mantener
# 99 - Pendientes / Siguientes Slices

## Estado al cierre de 8F-11L (2026-07-07)

> 8F-11L CERRADO. fal_acta: PARCIAL -> ALINEADO. fal_acta_evento: PARCIAL -> ALINEADO. 59 ALINEADO. 0 PARCIAL. Build: 2363 tests (+40).

**62 tablas MariaDB. 59 ALINEADO. 0 PARCIAL. 0 FALTA_EN_INMEMORY. Build: 2363 tests. 8F-11L CERRADO.**

### 8F-11L CERRADO - Cierre fal_acta y fal_acta_evento (2026-07-07)
- **FalActa**: campos de captura (OrigenCaptura, dispositivo, user, lat/lon/precision, fhPos), versiones (verDep, verInsp), ubicacion local Malvinas (idLoc, versionId, idTca, calle, altura, siEstimada, domTxt, siEjeUrb), qrPayloadVersion, funcionales (fhCierre, fhArchivo, idMotivoArchivo, permiteReingreso)
- **FalActaEvento**: record→class con Builder; id String→Long; OrigenEvento, ActorTipoEvento, estProcAnt/Nvo, sitAdmAnt/Nva, actorId/Ref, idDocuRel, idNotifRel, idPresRel, idUserEvt, siEvtCierre, siEvtExt, siPermiteReing, descripcionLegible, correlacionId; eliminado ordenLogico y payload
- **Enums nuevos**: OrigenCaptura (SMALLINT), ActorTipoEvento (SMALLINT), OrigenEvento (SMALLINT)
- **Enums mejorados**: EstadoProcesalActa + SituacionAdministrativaActa con codigo() CHAR(4) y deCodigo()
- **ActaEventoRepository**: registrar retorna FalActaEvento con id asignado; buscarPorId; existeCorrelacion; eliminado proximoOrdenLogico
- **InMemoryActaEventoRepository**: AtomicLong para IDs; ordenamiento fhEvt+id; buscarPorId; existeCorrelacion
- **17 servicios migrados** al patron FalActaEvento.builder() con OrigenEvento y ActorTipoEvento semanticos
- **DemoActaMaterializadorService**: migrado a fhEvt+getId()+descripcionLegible sin ordenLogico
- **ActaTransitionEngine**: motor de transiciones canonico con matriz completa ACTLAB->CERR
- **ActaConsistencyChecker**: verificador de invariantes del agregado (OCC, append-only, cierre, situacion)
- **3 tests nuevos**: ActaTransitionMatrixTest (30 casos), ActaEventoInvariantesTest (12 casos), ActaConcurrenciaTest (8 casos); total +40 tests

### Proximos slices sugeridos
- **8F-12**: Migracion JDBC/MariaDB del nucleo funcional

---
## Estado al cierre de 8F-11K (2026-07-06)

> 8F-11K CERRADO. fal_acta_qr_acceso: FALTA_EN_INMEMORY -> ALINEADO. 57 ALINEADO. 0 FALTA_EN_INMEMORY. Build: 2323 tests (+43).

**62 tablas MariaDB. 0 FALTA_EN_INMEMORY. 0 RELACION_INCOMPLETA. 8F-11K CERRADO.**

### 8F-11K CERRADO - QR de acceso al expediente (2026-07-06)
- **FalActaQrAcceso**: entidad de auditoria append-only para accesos validos via QR
- **CanalAccesoQr / ResultadoAccesoQr**: enums con codigo() para persistencia
- **QrAccesoRepository / InMemoryQrAccesoRepository**: repositorio append-only con ResettableInMemoryRepository
- **QrTokenProtector** (port) + **AesGcmQrTokenProtector** (impl AES-GCM-256): generacion y validacion segura de tokens
- **QrConfig**: configuracion Spring con clave Base64url o EPHEMERO para dev/test
- **QrActaService**: generarQr, rotarQr, registrarAcceso, registrarAccesoConNotificacion, listarAccesosPorActa
- **FalActa.codigoQr**: campo agregado + ActaRepository.buscarPorUuidTecnico
- **TipoEventoActa**: QRGEN + QRACC agregados
- **QrActaServiceTest**: suite completa con token valido, scope invalido, corrupto, concurrencia, no-PII, eventos

### Proximos slices sugeridos
- **8F-11L**: Completar PARCIAL en al_acta (campos faltantes) y al_acta_evento (idDocuRel alineacion completa)
- **8F-12**: Migracion JDBC/MariaDB del nucleo funcional

---
## Estado al cierre de 8F-11J (2026-07-06)

> 8F-11J CERRADO. fal_acta_documento: RELACION_INCOMPLETA -> ALINEADO. 56 ALINEADO. 0 RELACION_INCOMPLETA. Build: 2280 tests (+33).

**62 tablas MariaDB. 1 FALTA_EN_INMEMORY. 0 RELACION_INCOMPLETA. Build: 2280 tests. 8F-11J CERRADO.**

### 8F-11J CERRADO - Relacion canonica acta-documento (2026-07-06)
- **ActaDocumentoId**: record value object clave compuesta (actaId, documentoId); inmutable; igual por valor
- **RolDocuActa**: enum 12 roles (ACTA_PRINCIPAL..OTRO); codigo short estable; tiposPermitidos() por rol; admitePrincipal()/exigeUnicidadPrincipal()
- **FalActaDocumento**: entidad pivot; rolDocuActa, siPrincipal, fhAlta, idUserAlta; copia defensiva; setSiPrincipalInterno() para repositorio
- **ActaDocumentoYaExisteException, ActaDocumentoNoEncontradaException**: excepciones de dominio
- **ActaDocumentoRepository**: interfaz con guardar, buscarPorIdCompuesto, existe, listarPorActa, listarPorDocumento, listarPorActaYRol, buscarPrincipalPorActaYRol, asociarComoPrincipalAtomico, reemplazarPrincipalAtomico
- **InMemoryActaDocumentoRepository**: ConcurrentHashMap + principalLock; operaciones atomicas de principalidad; ResettableInMemoryRepository
- **ActaDocumentoService**: validar acta/documento, compatibilidad rol/tipo, unicidad principal, operaciones atomicas; resolverUltimoDocumentoOperativo()
- **FalActaSnapshot.idDocuUlt**: campo Long derivado del pivot (FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL)
- **SnapshotRecalculador**: actaDocumentoRepository @Autowired(required=false); resolverIdDocuUlt() calcula idDocuUlt
- **DocumentoService**: pivot opcional @Autowired; asocia en generarDocumento, generarDesdePlantilla, incorporarDocumentoEscaneado
- **FalloActaService**: pivot opcional @Autowired; asocia FALLO en dictarAbsolutorio y dictarCondenatorio
- **NotificacionService**: pivot opcional @Autowired; valida y asocia NOTIFICACION en enviarNotificacion
- **DocumentoGraphDemoService**: pivot opcional @Autowired; asocia documentos demo en ejecutarCaso
- **LoteCorreoService**: race condition c6 corregida (loteGeneracionMonitor sincroniza existeCodigo+guardar)
- **Tests nuevos**: ActaDocumentoServiceTest (17), ActaDocumentoConcurrenciaTest (10 escenarios), ActaDocumentoPivotIntegracionTest (5)
- **Paridad**: 56 ALINEADO (+1 fal_acta_documento), 1 FALTA_EN_INMEMORY, 2 PARCIAL, 0 SEMANTICA_INCOMPATIBLE, 0 RELACION_INCOMPLETA, 3 NO_PERSISTIBLE. Total: 62.
- **Build**: 2280 tests, 0 failures, 0 errors, BUILD SUCCESS

### Proximos slices sugeridos
- **8F-11K**: `fal_acta_qr_acceso` (FALTA_EN_INMEMORY) — QR de acceso al expediente
- **8F-11L**: Completar PARCIAL en `fal_acta` (campos faltantes) y `fal_acta_evento` (idDocuRel alineacion completa)
- **8F-12**: Migracion JDBC/MariaDB del nucleo funcional

## Estado al cierre de 8F-11H (2026-07-06)

> 8F-11H CERRADO. fal_acta_obligacion_pago (SEMANTICA_INCOMPATIBLE -> ALINEADO) + fal_acta_forma_pago + fal_acta_plan_pago_ref + fal_acta_pago_movimiento: ALINEADOS. 7 -> 4 FALTA_EN_INMEMORY. 48 -> 52 ALINEADO. Build: 2107 tests (+59). 12 eventos de pago en TipoEventoActa. Seeder PagoObligacionMockSeeder con 3 casos demo. Adapters de compatibilidad para FalPagoVoluntario/FalPagoCondena.

**62 tablas MariaDB. 4 FALTA_EN_INMEMORY. Build: 2107 tests. 8F-11H CERRADO.**

### 8F-11H CERRADO - Pagos: obligacion, forma, plan, movimiento (2026-07-06)
- **TipoObligacionPago**: enum PAGO_VOLUNTARIO(1), CONDENA(2)
- **EstadoObligacionPago**: enum 7 estados (DETERMINADA..ANULADA)
- **TipoFormaPago**: enum CONTADO(1), PLAN_PAGO(2), REFINANCIACION(3)
- **EstadoFormaPago**: enum GENERADA(1)..BAJA(5)
- **EstadoPlanPago**: enum ACTIVO(1)..REFINANCIADO(6)
- **TipoMovimientoPago**: enum 27 tipos (DEUDA_EMITIDA..OTRO)
- **MotivoAnulacionPago**: enum 6 motivos (CONTRACARGO..OTRO)
- **MotivoAptitudIntimacion**: enum para aptitud de intimacion en planes
- **MotivoBajaFormaPago**: enum para baja de forma de pago
- **FalActaObligacionPago**: entidad + OCC + cancelar/anular, monto con escala 2
- **FalActaFormaPago**: entidad + OCC + triple EM/PG, nroForma positivo unico
- **FalActaPlanPagoRef**: entidad + OCC + par (idTdocPlan, idDocPlan) unico
- **FalActaPagoMovimiento**: entidad append-only, Builder pattern, 27 tipos, idempotencia por referenciaExterna
- **ObligacionPagoRepository / InMemoryObligacionPagoRepository**: unicidad vigente, crearVigenteAtomico, OCC
- **FormaPagoRepository / InMemoryFormaPagoRepository**: nroForma unico por obligacion, reemplazarVigenteAtomico, OCC
- **PlanPagoRefRepository / InMemoryPlanPagoRefRepository**: par unico, una vigente por obligacion, refinanciarAtomico, OCC
- **PagoMovimientoRepository / InMemoryPagoMovimientoRepository**: append-only, idempotencia por referenciaExterna
- **ObligacionPagoNoEncontradaException, FormaPagoNoEncontradaException, PlanPagoNoEncontradoException, MovimientoPagoDuplicadoException**
- **PagoMovimientoReducer**: reductor deterministico por fhMovimiento; proyectarEstadoObligacion/Forma/Plan; soporta fuera-de-orden
- **ObligacionPagoService**: ciclo de vida obligacion (determinacion, anulacion, cancelacion)
- **FormaPagoService**: generacion, procesado, confirmado, baja de formas
- **PlanPagoService**: generacion, refinanciacion atomica
- **PagoMovimientoService**: registro idempotente de movimientos
- **PagoIntegracionService**: orquestacion notificaciones Ingresos/Tesoreria, recalcularEstados
- **NotificarMovimientoPagoCommand**: record de comando para notificacion
- **PagoVoluntarioAdapterRepository + PagoCondenaAdapterRepository**: adapters @Repository @Primary para compatibilidad
- **InMemoryPagoVoluntarioRepository + InMemoryPagoCondenaRepository**: @Deprecated (no @Repository), compatibilidad tests
- **FalActaSnapshot**: 17 campos de pago nuevos (tipoObligacionPago, estadoObligacionPago, montoObligacionPago, siPagoProcesado, etc.)
- **SnapshotRecalculador**: proyectarPagos() optional-wired para nuevos repos
- **TipoEventoActa**: +12 eventos de pago (OBLDET, OBLAUL, DEBEMI, FPCGEN, FPPGEN, FPREFN, PAGPRC, PAGCFT, PAGANU, MOVPAG, PLNCAI, PLNCAN)
- **ObservacionService**: registro de tipos OBLIGACION_PAGO, FORMA_PAGO, PLAN_PAGO, MOVIMIENTO_PAGO habilitados
- **PagoObligacionMockSeeder**: 3 casos demo (pago contado confirmado, plan en mora, plan refinanciado)
- **Tests nuevos**: ObligacionPagoTest (13), FormaPagoMovimientoTest (16), PlanPagoMovimientoConcurrenciaTest (15), PagoMovimientoReducerTest (15)
- **Build**: 2107 tests, 0 failures, 0 errors, BUILD SUCCESS

## Estado al cierre de 8F-11G (2026-07-06)

> 8F-11G CERRADO. fal_observacion + fal_acta_paralizacion + fal_acta_archivo + fal_motivo_archivo: ALINEADOS. 11 -> 7 FALTA_EN_INMEMORY. 44 -> 48 ALINEADO. Build: 2048 tests (+72).

**62 tablas MariaDB. 7 FALTA_EN_INMEMORY. Build: 2048 tests. 8F-11G CERRADO.**

### 8F-11G CERRADO - Observaciones, paralizacion y archivo (2026-07-06)
- **FalObservacion**: entidad + InMemoryObservacionRepository + ObservacionService
- **EntidadTipoObservada**: enum con 22 codigos explicitos (codigo() + fromCodigo())
- **OrigenObservacion**: enum USUARIO, SISTEMA, INTEGRACION
- **MotivoParalizacion**: enum ESPERA_DOCUMENTAL(1), OTRO(2)
- **FalActaParalizacion**: entidad + InMemoryActaParalizacionRepository (atomica, OCC, multi-ciclo)
- **ParalizacionActaService**: paralizar/reactivar con ciclos, OCC, observacion para OTRO
- **ActaParalizacionService**: delegador a ParalizacionActaService (back-compat)
- **ParalizarActaCommand**: actualizado a MotivoParalizacion enum
- **ReactivarActaCommand**: actualizado con versionParalizacion
- **FalMotivoArchivo**: entidad catalogo + InMemoryMotivoArchivoRepository (unicidad concurrente)
- **MotivoArchivoService**: CRUD catalogo, baja logica, validacion activo
- **MotivoArchivoMockSeeder**: 5 motivos (PRESCRIPCION, DESISTIMIENTO, ERROR_FORMAL, NULIDAD_PROCESAL, ESPERA_DOCUMENTACION)
- **FalActaArchivo**: entidad + InMemoryActaArchivoRepository (atomica, OCC, multi-ciclo)
- **ArchivoActaService**: archivar/reingresar - captura origen, atomicidad, cierre paraliz si corresponde
- **ArchivarActaCommand + ReingresarDesdeArchivoCommand**: nuevos
- **MotivoArchivoNoEncontradoException**: nueva excepcion
- **SnapshotRecalculador**: proyecta motivoParalizacionAct (actualizacion), optional injection
- **FalActaSnapshot**: campo motivoParalizacionAct (MotivoParalizacion, nullable)
- **Tests nuevos**: ObservacionTest (18), ParalizacionActaInvariantesTest (18), MotivoArchivoTest (16), ArchivoActaTest (20)
- **Build**: 2048 tests, 0 failures, 0 errors, BUILD SUCCESS
## Estado al cierre de 8F-11F (2026-07-06)

> 8F-11F CERRADO. fal_acta_fallo + fal_acta_apelacion: ALINEADOS. fal_acta_apelacion_documento: NUEVO ALINEADO. 12 -> 11 FALTA_EN_INMEMORY. 41 -> 44 ALINEADO. Build: 1976 tests (+75). 4 nuevos archivos de test, 6 nuevos escenarios demo (ACT-032 a ACT-037).

**62 tablas MariaDB. 11 FALTA_EN_INMEMORY. Build: 1976 tests. 8F-11F CERRADO.**

### 8F-11F CERRADO - Fallo, firmeza y apelaciÃƒÆ’Ã‚Â³n (2026-07-06)
- **FalActaFallo**: firmeza inline (siFirme, fhFirmeza, origenFirmeza), siVigente, falloReemplazadoId, fhVtoApelacion, versionRow, auditorÃƒÆ’Ã‚Â­a completa
- **FalActaApelacion**: canalApelacion, tipoPresentacion, estadoApelacion, resultadoResolucion, fhResolucion, documentoResolucionId, versionRow
- **FalActaApelacionDocumento**: nueva entidad + InMemoryApelacionDocumentoRepository (append-only, thread-safe)
- **FalActaFirmezaCondena**: transformada en DTO no persistible; FirmezaCondenaRepository deprecado; firmeza via fallo
- **OrigenFirmezaCondena**: enum con cÃƒÆ’Ã‚Â³digos (1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA)
- **ResultadoResolucionApelacion**: RECHAZADA, ACEPTADA_ABSUELVE, MODIFICA_CONDENA, NULIDAD
- **TipoPresentacion**: TEXTO, DOCUMENTOS, MIXTA + invariantes
- **CanalApelacion, TipoDocumentoApelacion, OrigenPresentacion**: enums nuevos
- **FalloActaRepository**: multi-fallo, findVigenteByActaId, guardarComoVigente (atÃƒÆ’Ã‚Â³mico), rechazarSiYaExisteVigente
- **ApelacionActaRepository**: findByFalloId, buscarActiva, buscarUltima
- **FalloActaService**: dictarCondenatorio, dictarAbsolutorio, firmeza va FirmezaCondenaService
- **ApelacionActaService**: registrarApelacion (3 tipos), registrarDocumento, pasarAAnalisis, resolverRechazada, resolverAceptaAbsuelve, resolverModificaCondena, resolverNulidad
- **FirmezaCondenaService**: vencerPlazoApelacion, declararFirmePorApelacionRechazada
- **SnapshotRecalculador**: proyecta fhVtoApelacion, bandeja CON_APELACION, CONDENA_FIRME
- **Tests nuevos**: FalloInvariantesTest (13), ResolucionApelacionEfectosTest (11), ApelacionDocumentoTest (15), FalloApelacionConcurrenciaTest (10)
- **Seeders**: ACT-032 a ACT-037 en DatasetFuncionalDominioCatalog + CasoUsoFuncionalRunner
- **Build**: 1976 tests, 0 failures, 0 errors, BUILD SUCCESS

## Estado al cierre de 8F-11E (2026-07-06)

> 8F-11E CERRADO. 9 tablas satelite + catalogos: ALINEADOS. 21 -> 12 FALTA_EN_INMEMORY. 32 -> 41 ALINEADO. Build: 1901 tests (+116). SatelitesCatalogosTest.java creado con tests concurrentes, de dominio y de snapshot.

**62 tablas MariaDB. 12 FALTA_EN_INMEMORY. Build: 1901 tests. 8F-11E CERRADO.**

### 8F-11E CERRADO - Satelites del Acta y Catalogos Vehiculo/Rubro (2026-07-06)
- **fal_acta_transito**: FalActaTransito, ActaTransitoRepository, InMemoryActaTransitoRepository, ActaTransitoService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 1:1 por acta, TRANSITO
- **fal_acta_transito_alcoholemia**: FalActaTransitoAlcoholemia, AtomicLong, lockFinal synchronized, marcarResultadoFinalAtomicamente ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â N mediciones, una final
- **fal_acta_vehiculo**: FalActaVehiculo, ActaVehiculoService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 1:1, fallback textual, validacion marca/modelo
- **fal_vehiculo_marca**: FalVehiculoMarca, VehiculoMarcaService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â unicidad codigo/nombre, baja logica, concurrencia
- **fal_vehiculo_modelo**: FalVehiculoModelo, VehiculoModeloService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â unicidad por marca, baja logica, referencias historicas
- **fal_acta_contravencion**: FalActaContravencion, ActaContravencionService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â nomenclatura estructurada, pares Suj/Bie, rubro coherente, ambito OTRO
- **fal_acta_sustancias_alimenticias**: FalActaSustanciasAlimenticias, ActaSustanciasAlimenticiasService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â rubro coherente, ambito, descripcion larga
- **fal_rubro_version**: FalRubroVersion, RubroVersionService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â versionado atomico, una sola version actual por IdRub, hash SHA-256, tests concurrentes
- **fal_acta_medida_preventiva**: FalActaMedidaPreventiva, ActaMedidaPreventivaAplicadaService ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â relacion articulo-medida, bloqueante atomico, transiciones, rollback logico
- **9 enums nuevos**: TipoPruebaAlcoholemia, ResultadoCualitativoAlcoholemia, UnidadMedidaAlcoholemia, TipoVehiculo, EstadoGeneralVehiculo, OrigenNomenclatura, MotivoNomenclaturaManual, AmbitoCtv, EstadoMedidaAplicada
- **Snapshot**: proyectarSatelites() en SnapshotRecalculador ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC
- **Seeder**: SatelitesCatalogosSeeder ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â marcas, modelos, rubros versionados activos/inactivos
- **Tests**: SatelitesCatalogosTest.java ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 116 tests nuevos incluyendo CyclicBarrier concurrentes
## Estado al cierre de 8F-11D-R1 (2026-07-06)

> CorrecciÃƒÆ’Ã‚Â³n documental: 62 tablas, 21 FALTA_EN_INMEMORY (29->27 en 8F-11C, 27->21 en 8F-11D+R1). P1 cerrada (SMALLINT 0-4), P2 cerrada (firmeza en fallo). 8F-11D-R1 CERRADO. Build: 1785 tests.

La fuente de verdad de paridad es: `110-matriz-maestra-paridad-mariadb-inmemory.md`
Toda la planificaciÃƒÆ’Ã‚Â³n granular de slices estÃƒÆ’Ã‚Â¡ en la secciÃƒÆ’Ã‚Â³n 7 de ese documento.
**62 tablas MariaDB. 21 FALTA_EN_INMEMORY. Build: 1785 tests. P1 y P2 cerradas. 8F-11B CERRADO. 8F-11C CERRADO. 8F-11D CERRADO. 8F-11D-R1 CERRADO.**

### 8F-11B CERRADO - Identidades, enums, auditoria, versionRow y campos core
- Migrar 5 entidades con id String a Long: FalActaFallo, FalActaApelacion, FalNotificacion, FalBloqueanteMaterial, FalGestionExterna
- FalActa.tipoActa: String ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ TipoActa enum con cÃƒÆ’Ã‚Â³digo SMALLINT (D8 cerrada) ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â valores 1ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“4
- **ResultadoFinalActa: SMALLINT 0ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“4 definitivo (P1 CERRADA)** ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 0=SIN_RESULTADO_FINAL, 1=PAGO_VOLUNTARIO_PAGADO, 2=ABSUELTO, 3=CONDENA_FIRME, 4=ANULADO
- FalActa.idInspector, idDependencia: String ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Long
- AuditorÃƒÆ’Ã‚Â­a (fh_alta, id_user_alta, fh_mod, id_user_mod) en FalActa, FalActaFallo, FalActaApelacion, FalGestionExterna
- Inspector firma fields: ELIMINADOS (8F-11B) - firma_storage_key, firma_hash, fh_firma_registrada eliminados del modelo definitivamente
- versionRow INT en entidades existentes: FalActa, FalDocumento, FalNotificacion, FalGestionExterna, FalActaFallo, FalActaApelacion - CERRADO

### 8F-11C CERRADO ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalPersona y FalPersonaDomicilio (2026-07-05)
- Crear FalPersona (tipo_persona, Id_Suj, Id_Bie, SujBieEstado), FalPersonaDomicilio (modo_domicilio, refs geo), repos InMemory
- FalActa.idPersona Long nullable
- Datos de persona separados de FalActa (D6 cerrada)

### 8F-11D CERRADO ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Normativa, tarifario, medidas y valorizaciÃƒÆ’Ã‚Â³n
- FalTarifarioUnidadFaltas, FalMedidaPreventiva, FalArticuloMedidaPreventiva
- FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem
- VinculaciÃƒÆ’Ã‚Â³n FalActaFallo.valorizacionId ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ FalActaValorizacion


### 8F-11D-R1 CERRADO - Invariantes de valorizacion (2026-07-06)
- SnapshotRecalculador: proyeccion de valorizacion vigente CONFIRMADA en snapshot (R1-A)
- confirmarVigenteAtomico: una sola CONFIRMADA vigente por acta+tipo; CAS + synchronized (R1-B)
- FalActaValorizacionItem: inmutable post-confirmacion del padre (R1-C)
- FalActaValorizacion: verificarMutable() en setters economicos; transiciones via metodos dominio (R1-D.8.5/8.6)
- FalTarifarioUnidadFaltas: guarda solapamiento de vigencia (R1-D.8.7)
- FalMedidaPreventiva: crearNuevaVersionAtomico, UK (codigo, versionMedida) (R1-D.8.8)
- FalArticuloMedidaPreventiva: rechazo reactivacion silenciosa (R1-D.8.9)
- FalActaArticuloInfringido: UK activo (actaId, articuloId) (R1-D.8.10)
- calcularBasePreliminar: lanza excepcion si sin tarifario activo (R1-D.8.11)
- **30 tests nuevos en ValorizacionInvariantesR1Test. Build: 1785 tests, 0 failures.**
- Correccion documental: FALTA_EN_INMEMORY canonicas = 21 (no 23 ni 27).

### 8F-11E ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â SatÃƒÆ’Ã‚Â©lites de acta y catÃƒÆ’Ã‚Â¡logos relacionados
- FalActaTransito, FalActaTransitoAlcoholemia, FalActaVehiculo
- FalActaContravencion, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva
- **FalVehiculoMarca** (id, descripcion, si_activo; CRUD; baja lÃƒÆ’Ã‚Â³gica)
- **FalVehiculoModelo** (id, marca_vehiculo_id, descripcion, si_activo; sin duplicados por marca)
- **FalRubroVersion** (row_hash, si_version_actual, valid_from, valid_to, synced_at; sincronizaciÃƒÆ’Ã‚Â³n externa Informix)
- Guardrails por TipoActa; vÃƒÆ’Ã‚Â­nculos exactos marca/modelo/rubro

### 8F-11F ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Fallo, firmeza y apelaciÃƒÆ’Ã‚Â³n
- FalActaFallo: valorizacionId, resultadoFallo, fhFirma, fhVtoApelacion, falloReemplazadoId, siFirme, fhFirmeza, origenFirmeza, versionRow, auditorÃƒÆ’Ã‚Â­a
- FalActaApelacion: canal, tipo, texto, documentoResolucionId, versionRow, auditorÃƒÆ’Ã‚Â­a; falloId ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Long
- FalActaApelacionDocumento
- OrigenFirmezaCondena: agregar cÃƒÆ’Ã‚Â³digos numÃƒÆ’Ã‚Â©ricos (D1 cerrada)
- **FalActaFirmezaCondena: P2 CERRADA (OpciÃƒÆ’Ã‚Â³n B)** ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â eliminar FalActaFirmezaCondena y FirmezaCondenaRepository; firmeza en FalActaFallo

### 8F-11G ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â ParalizaciÃƒÆ’Ã‚Â³n, archivo, motivos, observaciones y gestiÃƒÆ’Ã‚Â³n externa
- FalActaParalizacion (D3 cerrada)
- FalActaArchivo
- **FalMotivoArchivo** (id, descripcion, si_activo; FK desde fal_acta_archivo; CRUD; motivos inactivos vÃƒÆ’Ã‚Â¡lidos histÃƒÆ’Ã‚Â³ricamente)
- FalObservacion ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â PAGAPR escribe aquÃƒÆ’Ã‚Â­ en lugar de evento.descripcion
- Completar gestiÃƒÆ’Ã‚Â³n externa

### 8F-11H ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Pagos reales unificados (D2 cerrada)
- FalActaObligacionPago, FalActaFormaPago, FalActaPlanPagoRef, FalActaPagoMovimiento
- OrigenObligacionPago (PAGO_VOLUNTARIO=1, CONDENA=2)
- Migrar FalPagoVoluntario y FalPagoCondena al modelo unificado; retirar repos persistibles separados

### 8F-11I ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Notificaciones completas
- **CERRADO 2026-07-06** - FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo
- Enums: CanalNotificacion, TipoNotificacion, TipoAcuse, EstadoAcuse, EstadoLote; extendidos: EstadoNotificacion (SIN_EFECTO), ResultadoNotificacion (SUPERADA_POR_PORTAL), TipoEventoActa (+11 eventos)
- Servicios: NotificacionIntentoService, NotificacionAcuseService, LoteCorreoService
- Tests: 2247 total (+140), BUILD SUCCESS

### 8F-11J ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Documentos y relaciones restantes
- FalActaDocumento (pivot)
- FalDocumento campos faltantes
- No implementar PDF/storage real

### 8F-11K ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Portal QR y auditorÃƒÆ’Ã‚Â­a final de paridad cero
- FalActaQrAcceso; repositorio InMemory
- RevisiÃƒÆ’Ã‚Â³n final de las 62 tablas ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 0 gaps no justificados
- Build final completo
- Todos los estados ALINEADO, SOLO_DEMO_TEST, SOLO_INFRAESTRUCTURA o NO_PERSISTIBLE


## Guardrails permanentes

- NO USAR: @Entity, JpaRepository, EntityManager, Hibernate, JPA
- NO USAR: JDBC real, SQL ejecutable, Flyway, Liquibase (hasta Slice 9)
- NO USAR: MariaDB real, scripts DDL/DML ejecutables
- NO USAR: frontend Angular, PDF real, storage real
- NO REABRIR: decisiones D1 a D9 (cerradas en 8F-9-R1)
- GUARDRAIL: Test-Path docs\spec-as-source desde raÃƒÆ’Ã‚Â­z = FALSE
- GUARDRAIL: PAGCON no existe como evento; ACTCER no existe; D3_DOCUMENTAL no existe

---

## Historial de slices completados
## Completado hasta Slice 7D + Slice 6D-0 + Slice 6D-1 + Slice 6D-2

- Slice 1: ciclo base (labrar, captura, enriquecer, doc, firma, notif)
- Slice 2: pago voluntario productivo (flujo completo, invariantes blindados)
- Slice 3A: fallo absolutorio y condenatorio minimo
- Slice 3B: apelacion presentada + correccion APELAC->APEPRE
- Slice 3C: resolucion de apelacion (APERAZ y APEABS)
- Slice 4: firmeza de condena (PLAVNC+CONFIR / CONDENA_FIRME)
- Micro-slice de cierre semantico: guardrails, eliminacion de PAGCON, prohibicion D3
- Slice 5: pago de condena (PCOINF/PCOCNF/PCOOBS / CONDENA_FIRME_PAGADA) _(hist?rico: 164 tests al cerrar Slice 5)_
- Slice 6A: gestion externa ? derivacion (EXTDER)
- Slice 6B: gestion externa ? reingreso (EXTRET)
- Slice 6C: gestion externa ? pago externo (PAGAPR / CERRADA_EXTERNA / PAGO_REGISTRADO)
- Slice 6D-0: reconciliacion de catalogos gestion externa (ResultadoGestionExterna, ModoReingresoGestionExterna)
- Slice 6D-1: reingreso sin pago y sin cambios (SIN_PAGO + REINGRESO_SIN_PAGO; SIN_CAMBIOS + REINGRESO_PARA_REVISION)
- Slice 6D-2: reingreso con dictamen externo (ABSUELVE/CONFIRMA_CONDENA/MODIFICA_MONTO + REINGRESO_CON_DICTAMEN/REINGRESO_PARA_NUEVO_FALLO)

---

## Slice 5 - Pago de condena [COMPLETADO]

- `FalPagoCondena`, `EstadoPagoCondena` implementados
- `PagoCondenaRepository`, `InMemoryPagoCondenaRepository` implementados
- `InformarPagoCondenaCommand`, `ConfirmarPagoCondenaCommand`, `ObservarPagoCondenaCommand` implementados
- `PagoCondenaService` implementado con flujo completo (informar/confirmar/observar)
- `ResultadoFinalActa.CONDENA_FIRME_PAGADA (histÃƒÆ’Ã‚Â³rico ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â eliminado del enum en 8F-11B)` agregado
- `CodigoBandeja.PENDIENTE_PAGO_CONDENA` y `PENDIENTE_CONFIRMACION_PAGO_CONDENA` agregados
- `AccionPendiente.GESTIONAR_PAGO_CONDENA`, `CONFIRMAR_PAGO_CONDENA`, `CORREGIR_PAGO_CONDENA` agregados
- SnapshotRecalculador: routing por estado pago condena (PENDIENTE_PAGO_CONDENA / PENDIENTE_CONFIRMACION_PAGO_CONDENA / CERRADAS)
- Endpoints: /pago-condena/informar, /pago-condena/confirmar, /pago-condena/observar, GET /pago-condena
- 22 tests en PagoCondenaTest (mas 4 de snapshot)
- Build al cierre de Slice 5 _(hist?rico)_: 164/164 tests pasando

### Reglas criticas Slice 5
- `PAGCON` NO existe. Los eventos son `PCOINF`, `PCOCNF`, `PCOOBS`.
- `ACTCER` NO existe. El cierre usa `CIERRA`.
- `D3_DOCUMENTAL` NO existe como bloque.
- Confirmar pago: si hay bloqueantes -> no registrar ningun evento, no cerrar.
- Si confirma sin bloqueantes: `PCOCNF` antes que `CIERRA`.
- Integracion real con Ingresos/Tesoreria queda para slice posterior.
- Comprobantes reales quedan para slice posterior.
- Gestion externa no implementada en Slice 5.

---

## Slice 6A - Gestion externa: derivacion [COMPLETADO]

- `DerivarGestionExternaCommand` implementado
- `FalGestionExterna` con estado `DERIVADA`, `siActiva=true`, origen capturado
- `GestionExternaRepository`, `InMemoryGestionExternaRepository` implementados
- `GestionExternaService.derivar()` implementado
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/derivar`
- Snapshot tras derivar: `GESTION_EXTERNA / NINGUNA`
- 25 tests en `GestionExternaTest` (Slice 6A)
- **NO usar `DRVEXT`** - prohibido, no existe como evento productivo

---

## Slice 6B - Gestion externa: reingreso [COMPLETADO]

- `ReingresarDesdeGestionExternaCommand` implementado
- `FalGestionExterna` ampliado con campos de reingreso (`motivoReingreso`, `fechaReingreso`, `observacionesReingreso`)
- `GestionExternaRepository.buscarPorHistorico()` agregado
- `GestionExternaService.reingresar()` implementado
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/reingresar`
- Modos habilitados: `REINGRESO_PARA_REVISION`, `REINGRESO_SIN_PAGO`
- Modos prohibidos temporalmente: `REINGRESO_PARA_CIERRE`, `REINGRESO_PARA_NUEVO_FALLO`
- `PAGAPR` existe en enum pero NO se emite en Slice 6B (reservado Slice 6C+)
- 31 tests nuevos en `GestionExternaTest` (Slice 6B)
- Build limpio: 227/227 tests passing

---

## Slice 6C - Pago externo de gestion externa [COMPLETADO]

- `RegistrarPagoExternoGestionCommand` implementado
- `FalGestionExterna` ampliado con campo `fechaCierreGestionExterna`
- `GestionExternaService.registrarPagoExternoGestion()` implementado
- Endpoint: `POST /api/faltas/actas/{id}/gestion-externa/pago-externo`- Evento `PAGAPR` activo desde Slice 6C
- `CERRADA_EXTERNA` y `PAGO_REGISTRADO` activados
- `SnapshotRecalculador`: routing `CONDENA_FIRME + ACTIVA` (histÃƒÆ’Ã‚Â³rico: antes CONDENA_FIRME_PAGADA ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â eliminado en 8F-11B) -> `PENDIENTE_ANALISIS / NINGUNA`- 37 tests nuevos en `GestionExternaTest`- Build limpio: 264/264 tests passing

### Pendientes para Slice 6D+

- `REINGRESO_PARA_CIERRE`: bloqueado con PrecondicionVioladaException. Reservado.
- `REINGRESO_PARA_NUEVO_FALLO`: bloqueado con PrecondicionVioladaException. Reservado.
- `FALLO_CONDENATORIO_GESTION_EXTERNA` ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â **ELIMINADO del enum en 8F-11A-R1** (gestiÃƒÆ’Ã‚Â³n externa no es un resultado final)
- Cierre externo sin pago (SIN_PAGO, SIN_CAMBIOS): Slice 6D+.
- `FalObservacion` / `fal_observacion` para observaciones de PAGAPR: Slice 9/JDBC.

---

## Slice 7 - Motor de Bloqueantes Materiales

Objetivo:
Implementar motor real de bloqueantes.

- Implementar motor real de `BloqueantesMaterialesChecker`
- Crear `BloqueanteMaterialRepository` o equivalente
- Reemplazar `NoOpBloqueantesMaterialesChecker`
- El contrato de `BloqueantesMaterialesChecker` NO cambia

---


---

## Slice 8A-3 - Firmantes/autoridades in-memory [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 408/408 tests passing. BUILD SUCCESS.
**Tests nuevos (Slice 8A-3):** 38 tests en FirmanteTest.

### Archivos creados

- domain/model/FalFirmante.java
- domain/model/FalFirmanteVersion.java
- domain/model/FalFirmanteVersionHabilitacion.java
- domain/exception/FirmanteNoEncontradoException.java
- pplication/command/CrearFirmanteCommand.java
- pplication/command/VersionarFirmanteCommand.java
- pplication/command/AgregarHabilitacionFirmanteCommand.java
- pplication/command/DesactivarHabilitacionFirmanteCommand.java
-
epository/FirmanteRepository.java
-
epository/memory/InMemoryFirmanteRepository.java
- pplication/service/FirmanteService.java
- web/FirmanteController.java
- web/dto/CrearFirmanteRequest.java
- web/dto/VersionarFirmanteRequest.java
- web/dto/AgregarHabilitacionFirmanteRequest.java
- web/dto/FirmanteResponse.java
- web/dto/FirmanteVersionResponse.java
- web/dto/FirmanteHabilitacionResponse.java
- 	est/FirmanteTest.java

### Decisiones

- Se uso fal_firmante / fal_firmante_version / fal_firmante_version_habilitacion del modelo productivo actualizado.
- rolFirmante en FalFirmanteVersion queda descriptivo/opcional; no autoriza documentos.
- La autorizacion documental se expresa mediante FalFirmanteVersionHabilitacion (tipoDocu + rolFirmaReq + mecanismoFirmaReq).
- No se creo TipoFirmante.
- No se creo TipoAutoridad.
- No se creo EstadoFirmante.
- No se uso tipo_firma como rol en FalFirmanteVersion.
- No se implemento FalDocumentoFirmaReq.
- No se implemento bandeja de firma.
- No se implemento firma efectiva.
- Nueva version nace sin habilitaciones; deben agregarse explicitamente (evita arrastrar permisos sin revision).
- idDep/verDep nullable en firmante; si se informa, se valida existencia y estado activo.
- Firmantes quedan preparados para 8C mediante id_firmante/ver_firmante/habilitaciones.
- No se inicio MariaDB/JDBC.
- No se toco Angular.

### Reglas criticas

- TipoFirmante NO existe. No crear.
- TipoAutoridad NO existe. No crear.
- EstadoFirmante NO existe. No crear.
- FalDocumentoFirmaReq NO implementado todavia (reservado para 8C).
-
olFirmante en FalFirmanteVersion es DESCRIPTIVO. La autorizacion real va en FalFirmanteVersionHabilitacion.
- El mecanismo de firma nulo = sin restriccion de mecanismo.
- La vigencia temporal del firmante viene de FalFirmanteVersion (fhVigDesde/fhVigHasta).
## Slice 8 - Integracion con Ingresos/Tesoreria

Objetivo:
Reemplazar `referenciaPago` temporal por integracion real.

- Integrar con sistema de Ingresos/Tesoreria (Cmte_PG / Pref_PG / Nro_PG)
- Emitir comprobantes reales (PAGCMP cuando exista adjunto real)
- No romper el contrato de PagoCondenaService

---

## Slice 9 - Persistencia MariaDB/JDBC

Objetivo:
Reemplazar toda persistencia in-memory por JDBC real.

- Reemplazar todos los `InMemory*Repository` por implementaciones `JdbcClient`
- Sin JPA/Hibernate
- Los servicios no cambian
- Los tests de aplicacion siguen usando InMemory
- Agregar tests de integracion con base de datos real

---

## Slice 10 - Angular real

Objetivo:
Conectar el frontend Angular con los endpoints productivos.

- Conectar con endpoints de `api-faltas-core`
- Reemplazar mocks del prototipo por llamadas HTTP reales
- Adaptar bandejas, acciones y formularios al modelo productivo

---

## Invariantes a mantener en todos los slices

### Bloques
- `BloqueActual` solo contiene: CAPT, ENRI, NOTI, ANAL, GEXT, ARCH, CERR
- `D3_DOCUMENTAL` NO existe como bloque. Nunca reintroducir.
- `D3`, `DOCUMENTAL`, `D1_CAPTURA`, `D2_ENRIQUECIMIENTO`, `D4_NOTIFICACION`, `D5_ANALISIS` NO existen.

### Eventos prohibidos
- `PAGCON` NO existe. Los eventos de pago condena son PCOINF / PCOCNF / PCOOBS.
- `PAGVOL` NO existe como evento productivo.
- `ACTCER` NO existe como evento. El evento de cierre es `CIERRA`.
- `PASE_BANDEJA` NO es evento del dominio.
- `PASE_DEMO` NO existe.
- `FIRMEZA` NO existe como evento generico. Usar `CONFIR`.
- `FALLO` NO existe como evento generico. Usar `FALABS` o `FALCON`.
- `APELAC` NO EXISTE como evento productivo. El evento correcto es `APEPRE`.
- `APELACION` NO existe como evento generico. Usar `APEPRE`, `APERAZ` o `APEABS`.

### Estados prohibidos
- `PAGO_INFORMADO` NO existe como estado productivo de pago voluntario.
- `PAGO_VOLUNTARIO` NO existe en ResultadoFinalActa.
- `PAGCMP` NO se emite sin adjunto/evidencia real.
- `FALLO_ABSOLUTORIO` NO existe en ResultadoFinalActa. Usar `ABSUELTO`.

### Reglas de transicion
- Pago voluntario es un flujo, no una accion unica.
- Pago condena es un flujo, no una accion unica.
- Dictar fallo NO cierra el acta.
- Firmar documento de fallo NO cierra el acta.
- Fallo condenatorio notificado NO cierra ni asigna CONDENA_FIRME.
- Registrar apelacion NO cierra el acta.
- Registrar apelacion NO genera CONDENA_FIRME.
- Apelacion solo sobre fallo CONDENATORIO NOTIFICADO.
- Rechazar apelacion (APERAZ) NO cierra el acta ni genera CONFIR.
- Rechazar apelacion NO habilita pago condena directamente.
- Aceptar apelacion que absuelve (APEABS) asigna resultadoFinal=ABSUELTO.
- Aceptar apelacion cierra solo si no hay bloqueantes activos.
- Confirmar pago voluntario puede cerrar solo si no hay bloqueantes activos.
- Confirmar pago condena puede cerrar solo si no hay bloqueantes activos.
- Notificar absolutorio puede cerrar solo si no hay bloqueantes activos.
- CONDENA_FIRME no cierra el acta automaticamente.
- CONDENA_FIRME no inicia pago condena automaticamente.
- Informar pago condena NO cierra el acta.
- Observar pago condena NO cierra el acta.
- Si confirmar pago condena con bloqueantes: no registrar PCOCNF ni CIERRA, no cerrar.
- Si confirmar pago condena sin bloqueantes: PCOCNF se registra antes que CIERRA.
- El snapshot es derivado y regenerable, no fuente de verdad.
- BloqueantesMaterialesChecker es un puerto: hoy NoOp, futuro: motor real (Slice 7).
- Los repositorios son reemplazables por MariaDB/JDBC sin tocar servicios (Slice 9).
- No se implementa MariaDB todavia.
- No se conecta Angular todavia.


---

## Slice 7A - Motor de Bloqueantes Materiales [COMPLETADO]

- OrigenBloqueanteMaterial, EstadoBloqueanteMaterial implementados
- FalBloqueanteMaterial implementado
- BloqueanteMaterialRepository, InMemoryBloqueanteMaterialRepository implementados
- RepositoryBloqueantesMaterialesChecker implementado (@Component)
- NoOpBloqueantesMaterialesChecker: quitado @Component (usable en tests con new)
- PagoCondenaService.confirmar: PCOCNF se emite siempre; CIERRA solo si sin bloqueantes (patron PAGAPR)
- BloqueantesMaterialesTest: 6 tests nuevos
- Build al cierre de Slice 7A: 270/270 tests passing

### Reglas activas desde Slice 7A

- PCOCNF con bloqueantes: emite PCOCNF, no emite CIERRA, acta ACTIVA/ANAL.
- PAGAPR con bloqueantes: emite PAGAPR, no emite CIERRA, acta ACTIVA/ANAL (ya implementado en Slice 6C).
- Bloqueante inactivo (siActivo=false): no impide el cierre.
- "BloqueantesMaterialesChecker es puerto: hoy NoOp" ya no aplica. Motor real activo.

### Reglas en invariantes que quedan obsoletas (Slice 7A supersede)

- "Si confirmar pago condena con bloqueantes: no registrar PCOCNF ni CIERRA, no cerrar."
  -> Reemplazada: PCOCNF se registra siempre. Solo CIERRA se omite si hay bloqueantes.
- "BloqueantesMaterialesChecker es un puerto: hoy NoOp, futuro: motor real (Slice 7)."
  -> Reemplazada: motor real implementado desde Slice 7A.

---

## Slice 7B - Gestion minima in-memory de bloqueantes materiales [COMPLETADO]

- RegistrarBloqueanteMaterialCommand implementado (actaId + origen)
- CumplirBloqueanteMaterialCommand implementado (bloqueanteId)
- AnularBloqueanteMaterialCommand implementado (bloqueanteId)
- BloqueanteMaterialService implementado con registrar/cumplir/anular
- BloqueanteMaterialNoEncontradoException implementado
- BloqueanteMaterialRepository: findById(String id) agregado al contrato
- InMemoryBloqueanteMaterialRepository: findById implementado
- Ciclo completo: registrar -> PENDIENTE/siActivo=true; cumplir -> CUMPLIDO/siActivo=false; anular -> ANULADO/siActivo=false
- Bloqueante CUMPLIDO o ANULADO no impide el cierre (existsActivoByActaId devuelve false)
- Bloqueante PENDIENTE con siActivo=true si impide el cierre
- No se emiten eventos de acta para operaciones de bloqueantes (no existe evento de dominio definido)
- 13 tests nuevos en BloqueantesMaterialesTest (Slice 7B)
- Build al cierre de Slice 7B: 283/283 tests passing

### Reglas de gestion de bloqueantes (Slice 7B)

- registrar: actaId obligatorio, origen obligatorio, estado inicial PENDIENTE, siActivo=true, fechaAlta=now, fechaCierre=null.
- cumplir: idempotente si ya CUMPLIDO. No puede cumplirse si ANULADO (PrecondicionVioladaException).
- anular: idempotente si ya ANULADO. No puede anularse si CUMPLIDO (PrecondicionVioladaException).
- existsActivoByActaId: solo considera siActivo=true (PENDIENTE); CUMPLIDO y ANULADO no impiden cierre.

### Pendientes post Slice 7B

- Motor de bloqueantes completo (medidas preventivas, rodados, documentacion retenida con flujos propios): slices posteriores.
- Endpoints REST para gestion de bloqueantes: pendiente (no necesario para validacion funcional actual).
- No se implementan todavia medidas preventivas completas, rodados completos ni documentacion retenida completa.
- FalObservacionRepository: Slice 9/JDBC.
- Cierre externo sin pago (REINGRESO_PARA_CIERRE, REINGRESO_PARA_NUEVO_FALLO): Slice 6D.

---

## Slice 7C - Cierre diferido por resolucion del ultimo bloqueante activo [COMPLETADO]

### Descripcion

Cuando se cumple o anula el ultimo bloqueante activo de un acta que ya tiene resultado final cerrable
y no habia podido cerrarse por tener bloqueantes activos, el sistema ahora emite CIERRA automaticamente
y cierra el acta (CERRADA/CERR).

### Regla de dominio

Al cumplir o anular un bloqueante:

1. Si no quedan bloqueantes activos para el acta.
2. El acta existe y no esta ya cerrada/anulada.
3. El resultado final del acta es cerrable: PAGO_VOLUNTARIO_PAGADO, ABSUELTO o CONDENA_FIRME.
4. No existe ya un evento CIERRA registrado (guard contra duplicados).

Entonces: emitir CIERRA, pasar el acta a CERRADA/CERR, recalcular snapshot.

### Resultados cerrables (Slice 7C)

- PAGO_VOLUNTARIO_PAGADO (antes PAGO_VOLUNTARIO_CONFIRMADO ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â renombrado en 8F-11B)
- ABSUELTO
- CONDENA_FIRME (antes CONDENA_FIRME_PAGADA ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â eliminado en 8F-11B; pago no cambia resultado final)

CONDENA_FIRME no es cerrable por si solo: requiere pago o gestion externa adicional.

### Archivos nuevos

- `application/service/CierreActaHelper.java`
  - `esResultadoCerrable(ResultadoFinalActa)`
  - `yaTieneCierre(String actaId)`
  - `emitirCierre(FalActa acta, String motivo)`

### Archivos modificados

- `application/service/BloqueanteMaterialService.java`
  - Nuevo constructor: agrega `ActaRepository actaRepository` y `CierreActaHelper cierreActaHelper`
  - `cumplir()`: llama `intentarCierreDiferido(actaId)` tras guardar el bloqueante
  - `anular()`: llama `intentarCierreDiferido(actaId)` tras guardar el bloqueante
  - `intentarCierreDiferido(String actaId)`: logica de cierre diferido encadenada

### Reglas invariantes

- No se crea ningun evento de dominio nuevo para cumplir/anular bloqueante.
- El unico evento emitido en cierre diferido es CIERRA (ya existente).
- No se reintroduce D3_DOCUMENTAL, PAGCON, ACTCER, APELAC, DRVEXT.
- No se implementa MariaDB/JDBC.
- No se conecta Angular.
- No se implementa Slice 6D.
- No se implementa REINGRESO_PARA_CIERRE ni REINGRESO_PARA_NUEVO_FALLO.

### Tests nuevos (8 tests)

- 7C-01: cumplir ultimo bloqueante con CONDENA_FIRME -> CIERRA + CERRADA/CERR.
- 7C-02: anular ultimo bloqueante con CONDENA_FIRME -> CIERRA + CERRADA/CERR.
- 7C-03: dos bloqueantes activos, cumplir uno -> no CIERRA, sigue ACTIVA/ANAL.
- 7C-04: acta con CONDENA_FIRME (no cerrable) -> cumplir bloqueante -> no CIERRA.
- 7C-05: acta ya cerrada -> cumplir bloqueante -> no CIERRA duplicado.
- 7C-06: cumplir idempotente dos veces -> exactamente 1 CIERRA.
- 7C-07: anular idempotente dos veces -> exactamente 1 CIERRA.
- 7C-08: guardrail -> sin eventos/bloques/estados prohibidos.

---

## Siguiente pendiente recomendado

### Slice 8 - Capa REST / endpoints del backend productivo

Mapear los servicios ya implementados a endpoints REST:

- Revisar endpoints existentes en PrototipoApiController (si aplica).
- Crear ApiFaltasCoreController con endpoints productivos.
- DTOs de request/response.
- No incluir MariaDB todavia.
- Tests de integracion de endpoint (MockMvc o similar).

### Slice 9 - Persistencia MariaDB/JDBC

Reemplazar implementaciones InMemory por JDBC usando JdbcClient.
Ver: docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md
Ver: docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md

No implementar hasta reconciliar el delta del modelo.

### Slice 6D - Cierre externo sin pago (reservado)

Pendiente de definicion de dominio aprobada.
No implementar hasta que el dominio este documentado y aprobado.


---

## Slice 6D-0: Reconciliacion de catalogos gestion externa [COMPLETADO]

### Cambios realizados

Alineacion de enums in-memory al catalogo productivo MariaDB.

#### ResultadoGestionExterna

| Valor anterior | Valor productivo | Accion |
|---|---|---|
| PAGO_EXTERNO_INFORMADO | PAGO_REGISTRADO | Renombrado |
| DEVUELTA_PARA_REVISION | SIN_CAMBIOS | Reemplazado |
| NO_GESTIONABLE | (no existe en productivo) | Eliminado |
| (no existia) | ABSUELVE | Agregado como enum reservado |
| (no existia) | CONFIRMA_CONDENA | Agregado como enum reservado |
| (no existia) | MODIFICA_MONTO | Agregado como enum reservado |

#### ModoReingresoGestionExterna

| Valor anterior | Valor productivo | Accion |
|---|---|---|
| NO_APLICA | (campo nullable) | Eliminado; campo inicializa en null |
| REINGRESAR_A_ANALISIS | REINGRESO_PARA_REVISION | Renombrado |
| REINGRESAR_A_PAGO_CONDENA | REINGRESO_SIN_PAGO | Renombrado |
| REINGRESAR_A_CIERRE | REINGRESO_PARA_CIERRE | Renombrado (sigue reservado) |
| REINGRESAR_A_ARCHIVO | (no existe en productivo) | Eliminado; reemplazado por REINGRESO_PARA_NUEVO_FALLO reservado |
| (no existia) | REINGRESO_CON_PAGO | Agregado como enum reservado |
| (no existia) | REINGRESO_CON_DICTAMEN | Agregado como enum reservado |
| (no existia) | REINGRESO_PARA_NUEVO_FALLO | Agregado como enum reservado |

### Reglas activas post-reconciliacion

- PAGAPR asigna resultadoGestionExterna = PAGO_REGISTRADO.
- modoReingresoGestionExterna es null al derivar (antes era NO_APLICA).
- REINGRESO_PARA_REVISION: habilitado. Reemplaza REINGRESAR_A_ANALISIS. Retorna a ANAL/ACTIVA.
- REINGRESO_SIN_PAGO: habilitado. Requiere CONDENA_FIRME. Retorna a ANAL/ACTIVA.
- REINGRESO_PARA_CIERRE: bloqueado con PrecondicionVioladaException. Reservado.
- REINGRESO_PARA_NUEVO_FALLO: bloqueado con PrecondicionVioladaException. Reservado.
- REINGRESO_CON_PAGO: bloqueado con PrecondicionVioladaException. Reservado.
- REINGRESO_CON_DICTAMEN: bloqueado con PrecondicionVioladaException. Reservado.
- SIN_CAMBIOS: existe como enum productivo; no tiene efecto funcional todavia.
- ABSUELVE, CONFIRMA_CONDENA, MODIFICA_MONTO: existen como enum; sin efecto funcional todavia.
- No usar strings compuestos para mezclar tipo, resultado y modo de reingreso.
- Documentos externos recibidos: fal_documento/adjuntos (pendiente).
- Fundamentos/comentarios: fal_observacion (pendiente hasta JDBC).

### Tests guardrail agregados (clase GuardrailsCatalogos6D0)

- 6D0-01: ResultadoGestionExterna contiene exactamente los valores productivos.
- 6D0-02: ModoReingresoGestionExterna contiene exactamente los valores productivos.
- 6D0-03: PAGAPR asigna resultadoGestionExterna = PAGO_REGISTRADO.
- 6D0-04: PAGO_EXTERNO_INFORMADO no existe en ResultadoGestionExterna.
- 6D0-05: REINGRESAR_A_ANALISIS no existe en ModoReingresoGestionExterna.
- 6D0-06: REINGRESAR_A_PAGO_CONDENA no existe en ModoReingresoGestionExterna.
- 6D0-07: REINGRESO_PARA_CIERRE existe en enum pero sigue bloqueado.
- 6D0-08: REINGRESO_PARA_NUEVO_FALLO existe en enum pero sigue bloqueado.
- 6D0-09: modoReingresoGestionExterna es null tras EXTDER.

### Total tests: 300 (anterior: 291)

---

## Slice 6D-1: Reingreso sin pago y sin cambios [COMPLETADO]

### Objetivo

Implementar funcionalmente los dos casos de reingreso desde gestion externa sin intervenciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¿ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â½n de pago:

- Caso A: `SIN_PAGO + REINGRESO_SIN_PAGO` ? vuelve sin pago; continua circuito interno de cobro
- Caso B: `SIN_CAMBIOS + REINGRESO_PARA_REVISION` ? vuelve sin cambios sustantivos; revision interna

### Cambios implementados

**`GestionExternaService`:**
- Agrega `validarParResultadoModo()`: valida coherencia del par resultado/modo cuando `resultadoGestionExterna` es no nulo
- Pares habilitados: `SIN_PAGO + REINGRESO_SIN_PAGO` y `SIN_CAMBIOS + REINGRESO_PARA_REVISION`
- Rechaza resultados reservados: `ABSUELVE`, `CONFIRMA_CONDENA`, `MODIFICA_MONTO` (requieren REINGRESO_CON_DICTAMEN en slice futuro)
- Rechaza `PAGO_REGISTRADO` via reingreso (asignado solo por PAGAPR)
- Rechaza `SIN_RESULTADO` via reingreso (estado inicial al derivar)
- Rechaza pares incoherentes: `SIN_PAGO + REINGRESO_PARA_REVISION`, `SIN_CAMBIOS + REINGRESO_SIN_PAGO`

**`GestionExternaTest`:**
- Test 6B-03 corregido: par actualizado a `SIN_CAMBIOS + REINGRESO_PARA_REVISION`
- 15 tests nuevos en clase `Slice6D1`

### Reglas activas post-Slice-6D-1

- `SIN_PAGO + REINGRESO_SIN_PAGO`: habilitado. Emite EXTRET. Retorna a CONDENA_FIRME/ACTIVA/ANAL.
- `SIN_CAMBIOS + REINGRESO_PARA_REVISION`: habilitado. Emite EXTRET. Retorna a CONDENA_FIRME/ACTIVA/ANAL.
- Ambos no tocan campos PAGAPR (`fechaCierre`, `CERRADA_EXTERNA` (histÃƒÆ’Ã‚Â³rico: CONDENA_FIRME_PAGADA ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ CONDENA_FIRME en 8F-11B)).
- `REINGRESO_CON_PAGO`: reservado. No aplicable al flujo PAGAPR actual (que usa CERRADA_EXTERNA).
- `REINGRESO_CON_DICTAMEN`: bloqueado. Reservado para slice futuro con ABSUELVE/CONFIRMA_CONDENA/MODIFICA_MONTO.
- `REINGRESO_PARA_NUEVO_FALLO`: bloqueado. Reservado.
- `REINGRESO_PARA_CIERRE`: bloqueado. Reservado.

### Pendientes resueltos en Slice 6D-2

- Dictamen externo `ABSUELVE + REINGRESO_PARA_NUEVO_FALLO`: implementado (vuelve a ANAL; no genera fallo automatico)
- Dictamen externo `CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN`: implementado (mantiene CONDENA_FIRME; vuelve a ANAL)
- Dictamen externo `MODIFICA_MONTO + REINGRESO_CON_DICTAMEN`: implementado (registra monto externo; vuelve a ANAL)

### Pendientes para Slice 6D-3+

- Nuevo fallo interno post-ABSUELVE: dictarFalloAbsolutorio o dictarFalloCondenatorio despues de REINGRESO_PARA_NUEVO_FALLO
- Actualizar/definir monto condena post-MODIFICA_MONTO: comando dedicado para actualizar monto y continuar a pago
- Reingreso para cierre: `REINGRESO_PARA_CIERRE` (cierre externo sin revision interna)

### Tests nuevos (clase Slice6D1): 15 tests

- 6D1-01: SIN_PAGO reingresa por EXTRET (asserts completos)
- 6D1-02: SIN_CAMBIOS reingresa por EXTRET (asserts completos)
- 6D1-03: SIN_PAGO no toca campos PAGAPR
- 6D1-04: SIN_CAMBIOS no toca campos PAGAPR
- 6D1-05: Despues de SIN_PAGO no se puede PAGAPR
- 6D1-06: Despues de SIN_CAMBIOS no se puede PAGAPR
- 6D1-07: SIN_PAGO permite seguir circuito interno de pago condena
- 6D1-08: SIN_CAMBIOS deja snapshot coherente en analisis
- 6D1-09: Rechaza SIN_PAGO si acta no en GEXT
- 6D1-10: Rechaza SIN_CAMBIOS si no hay gestion activa
- 6D1-11: Rechaza par incoherente SIN_PAGO + REINGRESO_PARA_REVISION
- 6D1-12: Rechaza par incoherente SIN_CAMBIOS + REINGRESO_SIN_PAGO
- 6D1-13: Rechaza resultado reservado ABSUELVE
- 6D1-14: Rechaza modo reservado REINGRESO_PARA_NUEVO_FALLO
- 6D1-15: Guardrail - EXTRET unico evento; prohibidos intactos; modos reservados bloqueados

### Total tests: 315 (anterior: 300)

---

## Slice 6D-2: Reingreso con dictamen externo [COMPLETADO]

### Objetivo

Implementar los tres casos de reingreso con dictamen externo desde gestion externa:

- Caso A: `ABSUELVE + REINGRESO_PARA_NUEVO_FALLO` -> no absuelve automaticamente; vuelve a ANAL para nuevo fallo interno
- Caso B: `CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN` -> mantiene CONDENA_FIRME; vuelve a ANAL
- Caso C: `MODIFICA_MONTO + REINGRESO_CON_DICTAMEN` -> registra monto externo; vuelve a ANAL; sin nuevo fallo ni pago automatico

### Cambios implementados

**`ReingresarDesdeGestionExternaCommand`:**
- Nuevo campo: `BigDecimal montoResultado` (nullable para la mayoria de casos; obligatorio y > 0 para MODIFICA_MONTO)
- Corresponde al campo `monto_resultado` de `fal_acta_gestion_externa` en el modelo MariaDB

**`FalGestionExterna`:**
- Nuevo campo: `BigDecimal montoResultado` con getter/setter
- Persiste el monto externo informado cuando resultado es MODIFICA_MONTO

**`ReingresarDesdeGestionExternaRequest`:**
- Nuevo campo: `BigDecimal montoResultado`

**`GestionExternaController`:**
- Mapea `req.montoResultado()` al command

**`GestionExternaService`:**
- `validarModoHabilitadoEnSlice6B`: habilita REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN
  (antes bloqueados con PrecondicionVioladaException)
- `validarModoRequiereResultadoExplicito` (nuevo): REINGRESO_PARA_NUEVO_FALLO requiere resultado = ABSUELVE;
  REINGRESO_CON_DICTAMEN requiere resultado = CONFIRMA_CONDENA o MODIFICA_MONTO
- `validarParResultadoModo` (ampliado): agrega validacion par-por-par para los 3 nuevos casos
  - ABSUELVE requiere REINGRESO_PARA_NUEVO_FALLO
  - CONFIRMA_CONDENA requiere REINGRESO_CON_DICTAMEN
  - MODIFICA_MONTO requiere REINGRESO_CON_DICTAMEN
- `validarCondenaFirmeParaModosDictamen` (nuevo): REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN requieren
  resultadoFinal = CONDENA_FIRME (equivalente a la precondicion de REINGRESO_SIN_PAGO)
- `validarMontoParaModificaMonto` (nuevo): MODIFICA_MONTO requiere montoResultado no nulo y mayor a cero
- `reingresar`: registra montoResultado en FalGestionExterna; para CONFIRMA_CONDENA setea explicitamente
  resultadoFinal = CONDENA_FIRME (ya era precondicion, pero se confirma como efecto)

### Reglas activas post-Slice-6D-2

- `ABSUELVE + REINGRESO_PARA_NUEVO_FALLO`: habilitado. Emite EXTRET. Retorna a CONDENA_FIRME/ACTIVA/ANAL.
  No genera fallo absolutorio automatico. No cambia resultadoFinal. Requiere resultado explicito ABSUELVE.
- `CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN`: habilitado. Emite EXTRET. Mantiene CONDENA_FIRME/ACTIVA/ANAL.
  No emite CIERRA, PAGAPR ni PCOCNF.
- `MODIFICA_MONTO + REINGRESO_CON_DICTAMEN`: habilitado. Emite EXTRET. Registra montoResultado en FalGestionExterna.
  Retorna a CONDENA_FIRME/ACTIVA/ANAL. No dicta nuevo fallo. No pasa a pago.
  montoResultado es obligatorio y mayor a cero.
- `REINGRESO_PARA_NUEVO_FALLO` sin resultado explicito: falla con PrecondicionVioladaException.
- `REINGRESO_CON_DICTAMEN` sin resultado explicito: falla con PrecondicionVioladaException.
- `REINGRESO_PARA_CIERRE`: sigue bloqueado. Reservado.
- `REINGRESO_CON_PAGO`: sigue bloqueado. Reservado.
- `PAGO_REGISTRADO`: sigue exclusivo de PAGAPR. No puede informarse via reingreso.
- `SIN_RESULTADO`: sigue prohibido como resultado de reingreso.
- Campo `monto_resultado` en `fal_acta_gestion_externa` (MariaDB): confirmado como fuente de persistencia.
- Documentos externos recibidos (fal_documento): pendiente hasta JDBC.
- Fundamentos/comentarios (fal_observacion): pendiente hasta JDBC.

### Tests nuevos (clase Slice6D2): 14 tests

- 6D2-01: ABSUELVE + REINGRESO_PARA_NUEVO_FALLO reingresa por EXTRET (asserts completos; resultadoFinal no cambia)
- 6D2-02: CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN reingresa por EXTRET (asserts completos; CONDENA_FIRME mantenida)
- 6D2-03: MODIFICA_MONTO + REINGRESO_CON_DICTAMEN registra monto y vuelve a analisis
- 6D2-04: MODIFICA_MONTO rechaza montoResultado null
- 6D2-05: MODIFICA_MONTO rechaza monto cero o negativo
- 6D2-06: ABSUELVE rechaza modo REINGRESO_CON_DICTAMEN
- 6D2-07: CONFIRMA_CONDENA rechaza modo REINGRESO_PARA_NUEVO_FALLO
- 6D2-08: MODIFICA_MONTO rechaza modo REINGRESO_PARA_NUEVO_FALLO
- 6D2-09: REINGRESO_PARA_CIERRE sigue bloqueado
- 6D2-10: Post dictamen ABSUELVE no permite PAGAPR en mismo ciclo
- 6D2-11: Post MODIFICA_MONTO no pasa directo a pago ni cierre
- 6D2-12: PAGO_REGISTRADO sigue exclusivo de PAGAPR
- 6D2-13: SIN_RESULTADO no es aceptado como resultado de reingreso
- 6D2-14: Guardrail integral (EXTRET unico evento; prohibidos intactos; REINGRESO_PARA_CIERRE bloqueado)

### Total tests: 329 (anterior: 315)


---

## Etapa 8 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â API Funcional Completa Multi-App In-Memory [PLAN MAESTRO]

> **Fuente viva del roadmap de Etapa 8:**
> `backend/api-faltas-core/docs/spec-as-source/100-etapa-8-plan-maestro-api-multi-app.md`
>
> Ese archivo contiene: decisiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n estratÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©gica, guardrail obligatorio de alineaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n con MariaDB,
> aplicaciones objetivo, diagnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³stico de 27 mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³dulos, mapa apps-vs-mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³dulos, roadmap completo
> 8AÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“8I, dependencias entre slices y criterios de aceptaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n por slice.
>
> **PrÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ximo slice recomendado: Slice 8A-1 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Dependencias in-memory.**
> Antes de 8A-1, validar nombres/estructura contra modelo MariaDB.
> No iniciar Etapa 9/JDBC todavÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a.

> **Guardrail obligatorio Etapa 8:**
> Antes de crear cualquier entidad, enum, campo, endpoint, service o nombre nuevo,
> validar contra: `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`,
> `docs/faltas/MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md` y
> `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`.
> Si el concepto existe en MariaDB, usar su nombre/estructura productiva.
> Si no existe, documentar el delta antes de implementar.
> Ver reglas completas en `100-etapa-8-plan-maestro-api-multi-app.md`.

**Fecha de decision:** 2026-06-29
**Build al inicio de Etapa 8:** 330/330 tests pasando.

### Decision estrategica

No pasar a MariaDB/JDBC hasta completar primero la API funcional in-memory para todas las aplicaciones reales del ecosistema Faltas.

La persistencia (Etapa 9) inicia recien cuando dominio, comandos, endpoints, reglas, catalogos administrativos, talonarios, numeraciones, documentos, firmas, notificaciones, corralonos y contratos multi-app esten cerrados y testeados.

**Principio rector:** no persistir todavia, pero diseniar siempre pensando en que despues va a persistir.
Cada entidad, enum, comando, request, response y regla nueva debe alinearse con el modelo MariaDB final o dejar delta explicito.

### Aplicaciones objetivo de Etapa 8

1. Web Faltas administrativa (gestion completa de actas, bandejas, documentos, pagos, firmas, auditoria)
2. Mobile inspector (alta de actas, talonario asignado, numeracion, evidencias, catologos)
3. Portal ciudadano (consulta QR/codigo, estado publico, documentos, pago voluntario/condena)
4. App de firmas (firmante entidad, bandeja por firmante, firmar documento, avance flujo)
5. Backoffice administrativo (dependencias, inspectores, firmantes, talonarios, catalogos, vigencias)
6. App retiro rodados corralo'n (rodado retenido, validaciones, autorizacion, registro entrega)
7. App notificador municipal (bandeja notificador, intento, resultado, evidencia, geolocaliza)
8. Integraciones externas (contratos pagos, BOD, firma digital futura, notificaciones)

### Roadmap de slices Etapa 8

#### Bloque 8A ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Administracion base

- Slice 8A-1: Dependencias in-memory [PROXIMO]
- Slice 8A-2: Inspectores / agentes in-memory
- Slice 8A-3: Firmantes / autoridades in-memory
- Slice 8A-4: Catalogos administrativos base
- Slice 8A-5: Auditoria tecnica administracion base

#### Bloque 8B ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Talonarios y numeracion

- Slice 8B-1: Modelo de talonarios (num_politica + num_talonario)
- Slice 8B-2: Asignacion talonario a dependencia
- Slice 8B-3: Asignacion talonario a inspector
- Slice 8B-4: Numeracion automatica de actas
- Slice 8B-5: Talonarios de documentos
- Slice 8B-6: Talonarios manuales fisicos
- Slice 8B-7: Control de rangos sin huecos
- Slice 8B-8: Anulacion / justificacion de numeros
- Slice 8B-9: Concurrencia futura documentada (sin JDBC)

#### Bloque 8C ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Documentos y firma

- Slice 8C-1: Modelo FalDocumento ampliado (talonario, firmante)
- Slice 8C-2: Estados documentales completos
- Slice 8C-3: Documento requiere firma (requisito_firma, rol_firma_req)
- Slice 8C-4: Firmante requerido / asignado
- Slice 8C-5: Bandeja de firma por firmante
- Slice 8C-6: Comando firmar documento (trazabilidad completa)
- Slice 8C-7: Avance de flujo post firma
- Slice 8C-8: API app de firmas
- Slice 8C-9: Auditoria documental

#### Bloque 8D ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Observaciones y adjuntos

- Slice 8D-1: Modelo FalObservacion (polimorfico por entidad)
- Slice 8D-2: Observaciones por entidad (acta, notif, apelacion, etc.)
- Slice 8D-3: Observaciones de gestion externa
- Slice 8D-4: Modelo adjuntos / storage documental mock
- Slice 8D-5: Adjuntos vinculados a acta / doc / gestion ext.
- Slice 8D-6: API consulta y carga in-memory

#### Bloque 8E ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Notificaciones y app notificador

- Slice 8E-1: Modelo notificacion municipal (fal_notificacion_intento)
- Slice 8E-2: Bandeja notificador por agente
- Slice 8E-3: Intento de notificacion
- Slice 8E-4: Resultado notificacion (positivo/negativo)
- Slice 8E-5: Evidencia / observacion / geolocalizacion
- Slice 8E-6: Reintentos de notificacion
- Slice 8E-7: Impacto en flujo del acta
- Slice 8E-8: API app notificador municipal

#### Bloque 8F ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Rodados retenidos y corralo'n

- Slice 8F-1: Modelo rodado retenido
- Slice 8F-2: Vinculacion rodado-acta
- Slice 8F-3: Bloqueante material por rodado retenido
- Slice 8F-4: Validacion pagos / documentacion para retiro
- Slice 8F-5: Autorizacion de retiro
- Slice 8F-6: Registro de retiro / entrega
- Slice 8F-7: API app corralo'n
- Slice 8F-8: Auditoria de trazabilidad corralo'n

#### Bloque 8G ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Portal ciudadano y QR

- Slice 8G-1: Contrato consulta QR/codigo (token protegido)
- Slice 8G-2: Vista publica controlada del acta
- Slice 8G-3: Documentos visibles al ciudadano
- Slice 8G-4: Pagos desde portal (voluntario / condena)
- Slice 8G-5: Descargos / presentaciones ciudadanas
- Slice 8G-6: API portal ciudadano

#### Bloque 8H ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Contratos API por aplicacion

- Slice 8H-1: Contrato Web Faltas administrativa
- Slice 8H-2: Contrato Mobile inspector
- Slice 8H-3: Contrato App firmas
- Slice 8H-4: Contrato Backoffice administrativo
- Slice 8H-5: Contrato App notificador
- Slice 8H-6: Contrato App corralo'n
- Slice 8H-7: Contrato Portal ciudadano

#### Bloque 8I ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Auditoria API completa in-memory

- Slice 8I-1: Auditoria modelo in-memory vs MariaDB
- Slice 8I-2: Auditoria endpoints vs aplicaciones
- Slice 8I-3: Auditoria tests (cobertura por modulo)
- Slice 8I-4: Actualizacion completa del DELTA
- Slice 8I-5: Cierre API funcional completa in-memory

### Dependencias criticas entre slices

- 8A-1 Dependencias: es base independiente. Puede ir primero. Sin dependencias no hay inspector.
- 8A-2 Inspectores: requiere 8A-1. Sin dependencia el inspector no tiene ambito valido.
- 8B-1 Talonarios: requiere 8A-1. Sin dependencia el ambito de talonario es incompleto.
- 8B-4 Numeracion: requiere 8B-1/2/3. Sin talonario la numeracion no tiene respaldo.
- 8C Docs+firma: requiere 8A-3 Firmantes y 8B-4 Numeracion.
- 8E App notificador: requiere 8E-1/2 modelo completo de intento.
- 8F Corralo'n: requiere 8F-1/2/3 rodados como entidad.
- 8H Contratos: requiere modulos base de esa app.
- 8I Auditoria: requiere 8A-8H completos.

### Proximo slice recomendado: Slice 8A-1 Dependencias in-memory

**Justificacion:**
- Base para inspectores, talonarios, firmantes, bandejas y permisos futuros.
- Prerequisito para backoffice administrativo y mobile inspector.
- Riesgo cero: nuevo agregado independiente que no toca codigo existente.

**Alcance minimo 8A-1:**
- Entidades: FalDependencia, FalDependenciaVersion
- Enums: TipoActaDependencia (TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO)
- Repositorios: DependenciaRepository, InMemoryDependenciaRepository
- Servicio: DependenciaService (crear, activar, versionar, consultar vigente)
- Controller: DependenciaController + DTOs minimos
- Endpoints: POST /dependencias, GET /dependencias, GET /dependencias/{id}, PUT /dependencias/{id}/versionar
- Tests guardrail: catalogos vigentes, versionado, si_activa, tipo_acta unico por version
- Delta: FalDependencia no tiene FK directa a acta (relacion inversa: acta referencia dependencia)

---

## Etapa 9 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Persistencia MariaDB/JDBC [FUTURO]

Recien iniciar cuando Etapa 8 este cerrada.

- Reemplazar todos los InMemory*Repository por implementaciones JdbcClient.
- Sin JPA/Hibernate. Sin XML. SQL explicito con parametros nombrados.
- Los servicios no cambian.
- Los tests de aplicacion siguen usando InMemory.
- Agregar tests de integracion con base de datos real.
- Reconciliar DELTA completo antes de implementar cualquier repositorio JDBC.


---

## Slice 8A-1 - Dependencias in-memory [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 345/345 tests passing. BUILD SUCCESS.

### Diagnostico previo realizado

| Concepto | Existe en MariaDB | Nombre productivo | Decision |
|---|---|---|---|
| Dependencia | Si | fal_dependencia | Usar estructura productiva. No inventar Area, Sector ni variantes. |
| Versionado de dependencia | Si | fal_dependencia_version | Respetar id_dep + ver_dep, vigencias fh_vig_desde/fh_vig_hasta, si_activa. |
| Tipo de acta por dependencia | Parcial: no hay tipo_dependencia; si hay tipo_acta | TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO | No crear TipoDependencia. La dependencia versionada define un unico tipo_acta. |
| Relacion dependencia-acta | Si | fal_acta.id_dep, fal_acta.ver_dep, fal_acta.tipo_acta | Acta congela dependencia y version. tipo_acta se determina desde dependencia/inspector. |
| Relacion dependencia-inspector | Si | fal_inspector_version.id_dep/ver_dep | Queda para 8A-2. |
| Relacion dependencia-talonario | Si | num_talonario_ambito.id_dep/ver_dep | Queda para 8B. |
| Relacion dependencia-firmante | No tabla directa | fal_documento_firma.rol_firmante | Queda para 8A-3 con diagnostico propio. |
| Dependencia-normativa | Si | fal_dependencia_normativa.id_dep/ver_dep | Queda para 8A-4/catalogos. |

### Entidades creadas

- `TipoActa` enum (TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO)
- `FalDependencia` (id_dep, cod_dep, nom_dep, id_dep_padre, si_activa, fh_alta, id_user_alta)
- `FalDependenciaVersion` (id_dep, ver_dep, nom_dep, id_dep_padre, ver_dep_padre, tipo_acta, fh_vig_desde, fh_vig_hasta, si_activa)
- `DependenciaNoEncontradaException`

### Repositorios creados

- `DependenciaRepository` (interfaz)
- `InMemoryDependenciaRepository` (implementacion in-memory)

### Comandos creados

- `CrearDependenciaCommand`
- `VersionarDependenciaCommand`

### Service creado

- `DependenciaService` (crear, versionar, obtener, listarActivas, listarVersiones, obtenerVersionVigente)

### DTOs creados

- `CrearDependenciaRequest`
- `VersionarDependenciaRequest`
- `DependenciaResponse`
- `DependenciaVersionResponse`

### Controller creado

- `DependenciaController` - endpoints:
  - POST /api/faltas/dependencias
  - GET /api/faltas/dependencias
  - GET /api/faltas/dependencias/{id}
  - PUT /api/faltas/dependencias/{id}/versionar

### Tests creados (15 tests en DependenciaTest)

- 8A1-01: Crear dependencia valida genera idDep y verDep = 1
- 8A1-02: Dependencia con TRANSITO queda activa y consultable
- 8A1-03: Crear sin nomDep falla (nulo y blanco)
- 8A1-04: Crear sin tipoActa falla
- 8A1-05: codDep duplicado falla; codDep nulo no dispara unicidad
- 8A1-06: Dependencia hija congela idDepPadre y verDepPadre vigente
- 8A1-07: Crear hija con padre inexistente falla
- 8A1-08: Versionar incrementa verDep a 2
- 8A1-09: Versionar conserva historial y deja nueva version activa
- 8A1-10: Guardrail ALCOHOLEMIA no existe en TipoActa
- 8A1-11: Catalogo TipoActa tiene exactamente los 4 valores productivos
- 8A1-12: Versionar dependencia inexistente falla

### Reglas criticas activas desde Slice 8A-1

- TipoActa enum: TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO. Catalogo cerrado.
- ALCOHOLEMIA no es tipo de acta. Es satelite eventual de un acta de transito.
- No crear TipoDependencia. Solo existe tipo_acta en fal_dependencia_version.
- Al crear dependencia: verDep = 1.
- Al versionar: verDep = max(verDep) + 1.
- Version anterior pierde siActiva y queda con fhVigHasta al momento del nuevo versionado.
- idDepPadre debe existir si se informa.
- Si hay padre, verDepPadre se congela con la version vigente del padre al momento de crear/versionar.
- FalActa.tipoActa y FalActa.idDependencia siguen siendo String por ahora (compatibilidad con flujo existente). La relacion con TipoActa enum se aplica desde DependenciaService hacia adelante.

### Proximo slice recomendado: Slice 8A-2 - Inspectores/agentes in-memory

Antes de 8A-2, validar obligatoriamente nombres contra:
- fal_inspector
- fal_inspector_version
- fal_inspector_version.id_dep (relacion dependencia)
- fal_inspector_version.ver_dep (version de dependencia congelada)

No inventar nombres transitorios si el modelo productivo ya define el concepto.


---

## Slice 8A-2 - Inspectores/agentes in-memory [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 368/368 tests passing. BUILD SUCCESS.

### Diagnostico previo realizado

| Concepto | Existe en MariaDB | Nombre productivo | Decision |
|---|---|---|---|
| Inspector maestro | Si | fal_inspector | FalInspector in-memory alineado |
| Inspector versionado | Si | fal_inspector_version | FalInspectorVersion in-memory alineado |
| Usuario autenticable | Si | id_user | Campo final, obligatorio y unico |
| Legajo inspector | Si | legajo_insp | Obligatorio, congelado en version |
| Nombre inspector | Si | nom_insp | Obligatorio, congelado en version |
| Estado activo maestro | Si | si_activo | siActivo boolean |
| Dependencia del inspector | Si | id_dep + ver_dep en fal_inspector_version | Validado contra DependenciaRepository |
| Vigencia inspector | Si | fh_vig_desde, fh_vig_hasta, si_activo | Respetado |
| Tipo de acta del inspector | No directo | Derivado de fal_dependencia_version.tipo_acta | NO duplicado en inspector |

### Decisiones explicitas

- Se uso fal_inspector / fal_inspector_version como nombre productivo.
- No se creo FalAgente.
- No se creo enum nuevo (TipoInspector, TipoAgente, EstadoInspector).
- No se duplico tipoActa en inspector. Se deriva consultando FalDependenciaVersion.
- El inspector queda vinculado a dependencia versionada por id_dep/ver_dep.

### Entidades creadas

- FalInspector (id_insp, id_user, legajo_insp, nom_insp, si_activo, fh_alta, id_user_alta)
- FalInspectorVersion (id_insp, ver_insp, legajo_insp, nom_insp, id_dep, ver_dep, fh_vig_desde, fh_vig_hasta, si_activo)
- InspectorNoEncontradoException

### Repositorios creados

- InspectorRepository (interfaz)
- InMemoryInspectorRepository (implementacion in-memory)

### Comandos creados

- CrearInspectorCommand
- VersionarInspectorCommand

### Service creado

- InspectorService (crear, versionar, obtener, listarActivos)

### DTOs creados

- CrearInspectorRequest
- VersionarInspectorRequest
- InspectorResponse (incluye lista de versiones)
- InspectorVersionResponse

### Controller creado

- InspectorController - endpoints:
  - POST /api/faltas/inspectores
  - GET /api/faltas/inspectores
  - GET /api/faltas/inspectores/{id}
  - PUT /api/faltas/inspectores/{id}/versionar

### Tests creados (23 tests en InspectorTest)

- 8A2-01: Crear inspector valido con dependencia existente
- 8A2-02: Rechazar inspector sin idUser (nulo y blanco)
- 8A2-03: Rechazar idUser duplicado
- 8A2-04: Rechazar inspector sin legajoInsp
- 8A2-05: Rechazar inspector sin nomInsp
- 8A2-06: Rechazar inspector sin idDep o verDep
- 8A2-07: Rechazar inspector con dependencia inexistente
- 8A2-08: Rechazar inspector con version de dependencia inexistente
- 8A2-09: Version inicial verInsp = 1
- 8A2-10: Versionar incrementa verInsp
- 8A2-11: Versionar cierra version anterior (siActivo=false + fhVigHasta)
- 8A2-12: Versionar congela nueva dependencia idDep/verDep
- 8A2-13: Obtener inspector por id
- 8A2-14: Listar inspectores activos
- 8A2-15: Inspector no encontrado lanza error controlado (obtener y versionar)
- 8A2-16: Guardrail: inspector no tiene tipoActa propio (3 asserts)
- 8A2-17: Ambito del inspector via idDep/verDep (2 asserts)

### Reglas criticas activas desde Slice 8A-2

- idUser es obligatorio y unico entre inspectores.
- legajoInsp y nomInsp son obligatorios.
- idDep + verDep son obligatorios y la version de dependencia debe existir y estar activa.
- Al crear inspector: verInsp = 1.
- Al versionar: verInsp = max(verInsp) + 1; version anterior pierde siActivo.
- El inspector no define tipo_acta. Se deriva desde FalDependenciaVersion.tipo_acta.
- No existe FalAgente, TipoInspector, TipoAgente, EstadoInspector.
- Los repositorios son reemplazables por MariaDB/JDBC sin tocar servicios (Slice 9).


### Proximo slice recomendado: Slice 8A-3 - Firmantes/autoridades in-memory

Antes de 8A-3 (implementacion Java), se ejecutaron:
- Micro-slice 8A-3D: `fal_firmante` y `fal_firmante_version` incorporados al modelo productivo (Secciones 1.7 y 1.8). DELTA actualizado.
- Micro-slice 8A-3D.1: modelo completo de firma documental cerrado. `fal_firmante_version_habilitacion` (Seccion 1.9) y `fal_documento_firma_req` (Seccion 5.4) agregadas. `estado_firma_req` definido. Reglas de bandeja (5.5) y firma efectiva (5.6) documentadas. DELTA actualizado.

Diagnostico pre-8A-3 confirmado contra modelo MariaDB productivo:
- fal_firmante: id_firmante, id_user, nom_firmante, si_activo, fh_alta, id_user_alta.
- fal_firmante_version: id_firmante, ver_firmante, id_user, nom_firmante, rol_firmante (descriptivo, SI nullable), cargo_firmante, id_dep, ver_dep, fh_vig_desde, fh_vig_hasta, si_activo, fh_alta, id_user_alta.
- fal_firmante_version_habilitacion: id_firmante, ver_firmante, tipo_docu, rol_firma_req, mecanismo_firma_req, si_activo, fh_alta, id_user_alta.
- No inventar nombres ni estructuras no productivas.
- No crear TipoFirmante, EstadoFirmante ni enum equivalente.
- rol_firmante en fal_firmante_version es DESCRIPTIVO, no define autorizacion. La autorizacion va en fal_firmante_version_habilitacion.
- tipo_firma NO va en fal_firmante_version. tipo_firma es mecanismo/naturaleza de firma.
- No implementar fal_documento_firma_req ni bandeja de firma en 8A-3; eso queda para 8C con modelo ya definido.


---

## Micro-slice 8A-3D - Ajuste modelo productivo firmantes [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 368/368 tests passing. BUILD SUCCESS.
**Tipo:** Ajuste documental de modelo productivo. Sin cambios Java.

### Cambios realizados

| Archivo | Cambio |
|---------|--------|
| `MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` | Secciones 1.7 (fal_firmante) y 1.8 (fal_firmante_version) agregadas. |
| `MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` | `fal_documento_firma`: campos `id_firmante`/`ver_firmante` agregados; reglas y relacion funcional documentadas. |
| `DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md` | Entrada 8A-3D agregada con deteccion, cambios, reglas criticas y deuda JDBC. |

### Reglas criticas incorporadas desde 8A-3D

- No crear FalFirmante hasta tener el modelo productivo validado (ya hecho en 8A-3D).
- rol_firmante en fal_firmante_version es compatible con fal_documento.rol_firma_req.
- tipo_firma NO va en fal_firmante_version. tipo_firma es mecanismo/naturaleza de firma.
- Si cambia rol, cargo o dependencia: nueva version. No se actualizan versiones anteriores.
- id_firmante/ver_firmante en fal_documento_firma son nullable (admite firma de sistema).
- Snapshots historicos de fal_documento_firma no se reescriben por cambios posteriores.


---

## Micro-slice 8A-3D.1 - Cierre completo del modelo productivo de firma documental [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 368/368 tests passing. BUILD SUCCESS.
**Tipo:** Ajuste documental de modelo productivo. Sin cambios Java.

### Problema detectado

El modelo agregado en 8A-3D (fal_firmante, fal_firmante_version) no alcanzaba para el flujo real.
Faltaban dos piezas centrales:

1. Que puede firmar cada version de firmante.
2. Que firmas necesita cada documento concreto.

La bandeja de firma no puede depender de logica hardcodeada. Debe salir del modelo.

### Cambios realizados

| Archivo | Cambio |
|---------|--------|
| `MODELO_MARIADB...` | `fal_firmante_version_habilitacion` agregada (Seccion 1.9). |
| `MODELO_MARIADB...` | `fal_firmante_version`: `rol_firmante` pasa a SI nullable y descriptivo. |
| `MODELO_MARIADB...` | `fal_documento_firma_req` agregada (Seccion 5.4). |
| `MODELO_MARIADB...` | `estado_firma_req` definido (catalogo en Seccion 5.4). |
| `MODELO_MARIADB...` | `seq_firma_req` agregado a `fal_documento_firma`. |
| `MODELO_MARIADB...` | Regla de bandeja de firma documentada (Seccion 5.5). |
| `MODELO_MARIADB...` | Regla de firma efectiva documentada (Seccion 5.6). |
| `DELTA_MODELO_MARIADB...` | Entrada 8A-3D.1 agregada. |

### Modelo productivo de firma cerrado

- fal_firmante: maestro del firmante.
- fal_firmante_version: identidad/cargo/dependencia/vigencia del firmante.
- fal_firmante_version_habilitacion: define que puede firmar esa version (tipo_docu + rol_firma_req + mecanismo).
- fal_documento_firma_req: define que firmas necesita cada documento concreto. Fuente de la bandeja.
- fal_documento_firma: hecho historico de una firma realizada. Snapshot inmutable.

### Habilitado para

- Slice 8A-3: implementar FalFirmante, FalFirmanteVersion, FalFirmanteVersionHabilitacion in-memory.
- Slice 8C: implementar FalDocumentoFirmaReq, bandeja de firma, firma efectiva. El modelo ya esta definido.
- No quedan decisiones estructurales de modelo pendientes para firma.

### Proximo slice: Slice 8A-3 - Firmantes/autoridades in-memory

Implementar en Java, alineado al modelo productivo actualizado:
- FalFirmante (idFirmante, idUser, nomFirmante, siActivo, fhAlta, idUserAlta)
- FalFirmanteVersion (idFirmante, verFirmante, idUser, nomFirmante, rolFirmante, cargoFirmante, idDep, verDep, fhVigDesde, fhVigHasta, siActivo, fhAlta, idUserAlta)
- FalFirmanteVersionHabilitacion (idFirmante, verFirmante, tipoDocu, rolFirmaReq, mecanismoFirmaReq, siActivo, fhAlta, idUserAlta)
- FirmanteNoEncontradoException
- FirmanteRepository (interfaz) + InMemoryFirmanteRepository
- FirmanteVersionHabilitacionRepository (interfaz) + InMemoryFirmanteVersionHabilitacionRepository
- CrearFirmanteCommand + VersionarFirmanteCommand + HabilitarFirmanteCommand
- FirmanteService
- DTOs: CrearFirmanteRequest, VersionarFirmanteRequest, HabilitarFirmanteRequest, FirmanteResponse, FirmanteVersionResponse, FirmanteHabilitacionResponse
- FirmanteController con endpoints CRUD basicos
- Tests alineados al modelo productivo

Guardrails:
- No implementar fal_documento_firma_req ni bandeja de firma en 8A-3. Eso queda para 8C.
- El modelo productivo de 8C ya esta definido (Secciones 5.4, 5.5, 5.6 del MODELO_MARIADB).
- Los repositorios deben ser reemplazables por JDBC sin tocar servicios.


---

## Slice 8A-4 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Normativas, articulos y dependencia-normativa in-memory (CERRADO 2026-06-30)

### Estado

Cerrado. Build: 449/449 tests. BUILD SUCCESS.

### Entidades implementadas

- TipoUnidad enum (catalogo cerrado: SALARIO, UNIDAD_FIJA, MONTO). Alineado a tipo_unidad del modelo productivo.
- FalNormativaFaltas ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â campos exactos de fal_normativa_faltas (id, codigo_norma, version_norma, nombre_norma, descripcion_norma, si_activa, fh_vig_desde, fh_vig_hasta, fh_alta, id_user_alta).
- FalArticuloNormativaFaltas ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â campos exactos de fal_articulo_normativa_faltas (id, normativa_id, codigo_articulo, version_articulo, nombre_articulo, descripcion_articulo, cantidad_unidades, tipo_unidad, si_tiene_pago_voluntario, cantidad_unidades_pago_voluntario, tipo_unidad_pago_voluntario, si_activo, fh_vig_desde, fh_vig_hasta, fh_alta, id_user_alta).
- FalDependenciaNormativa ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â campos exactos de fal_dependencia_normativa (id_dep, ver_dep, normativa_id, si_activa, fh_alta, id_user_alta).
- NormativaRepository (interfaz) + InMemoryNormativaRepository.
- CrearNormativaFaltasCommand, CrearArticuloNormativaFaltasCommand, VincularDependenciaNormativaCommand.
- NormativaService con reglas de negocio completas.
- NormativaController con endpoints /api/faltas/normativas, /api/faltas/normativas/{id}/articulos, /api/faltas/dependencias/{idDep}/versiones/{verDep}/normativas.
- NormativaNoEncontradaException, ArticuloNormativaNoEncontradoException.
- NormativaTest con 37 tests (TipoUnidad, normativas, articulos, vinculos, guardrails).

### No implementado en 8A-4

- al_tarifario_unidad_faltas ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â pendiente 8C o posterior.
- Valorizaciones / calculos de monto ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â pendiente 8C.
- Catalogos de firma (rol_firma_req, mecanismo_firma_req, tipo_firma, estado_firma_req, requisito_firma) ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â pendiente 8C.
- Alineacion de TipoDocumento y EstadoFirmaDocumento ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â pendiente 8C.
- No se inicio MariaDB/JDBC.
- No se toco Angular.
- docs/spec-as-source historica no existe.

---

## Slice 8A-5 - Auditoria tecnica administracion base [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 450/450 tests passing. BUILD SUCCESS.

### Resultado de auditoria

| Bloque | Estado | Observaciones |
|--------|--------|---------------|
| Dependencias (8A-1) | OK | Campos, tipos, nullabilidad y reglas alineados con MariaDB |
| Inspectores (8A-2) | OK con correccion menor | legajoInsp corregido: String to int/Integer (MariaDB INT) |
| Firmantes (8A-3) | OK con correccion menor | fhAlta/idUserAlta agregados a FalFirmanteVersion y FalFirmanteVersionHabilitacion |
| Normativas (8A-4) | OK | Campos exactos del modelo productivo |
| Endpoints | OK | Rutas consistentes, DTOs con nombres productivos camelCase |
| DTOs/Commands | OK | Actualizados para reflejar correcciones de tipos |
| Repositories/Services | OK | Correcciones propagadas a InspectorService y FirmanteService |
| Tests | OK | 450/450 passing. Test legajoInsp=0 agregado en InspectorTest |
| Spec viva | OK | docs/spec-as-source historica ausente. Spec viva en backend/api-faltas-core/docs/spec-as-source/ |
| Conceptos prohibidos | OK | Sin hallazgos funcionales fuera de scope |
| Enums nuevos 8A | OK | Solo TipoActa y TipoUnidad. Alineados al catalogo productivo |

### Correcciones realizadas

1. **legajoInsp String to int/Integer**: FalInspector, FalInspectorVersion, CrearInspectorCommand, VersionarInspectorCommand, CrearInspectorRequest, VersionarInspectorRequest, InspectorResponse, InspectorVersionResponse, InspectorService, InspectorTest.
   - MariaDB define legajo_insp como INT. El tipo String era incorrecto.

2. **fhAlta/idUserAlta en FalFirmanteVersion y FalFirmanteVersionHabilitacion**: Campos fh_alta/id_user_alta presentes en el modelo productivo fal_firmante_version y fal_firmante_version_habilitacion, faltaban en los modelos Java.
   - Propagados tambien a FirmanteService, FirmanteVersionResponse, FirmanteHabilitacionResponse.

### Deltas confirmados para 8C

- TipoDocumento vs tipo_docu: enum actual no alineado con catalogo productivo SMALLINT.
- EstadoFirmaDocumento vs estado_firma: enum actual no alineado con catalogo productivo.
- estado_docu sin catalogo productivo numerico explicito.
- tipoDocu, rolFirmaReq, mecanismoFirmaReq en FalFirmanteVersionHabilitacion: actualmente String; MariaDB usa SMALLINT. Correccion reservada para 8C junto con el modelo documental.

### Reglas criticas confirmadas

- legajoInsp es int (obligatorio, positivo). Tipo String eliminado.
- fhAlta/idUserAlta presentes en FalFirmanteVersion y FalFirmanteVersionHabilitacion.
- TipoActa: exactamente TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO.
- TipoUnidad: exactamente SALARIO, UNIDAD_FIJA, MONTO.
- docs/spec-as-source historica: no existe (False).
- No se introdujeron entidades, enums ni endpoints nuevos.
- No se toco Angular.
- No se inicio MariaDB/JDBC.

### Proximo slice recomendado: 8B-1 - Modelo de talonarios

Antes de 8B-1, diagnostico obligatorio contra modelo MariaDB:
- num_politica
- num_talonario
- num_talonario_ambito (id_dep/ver_dep)
- num_serie_talonario
- fal_acta.num_talonario, fal_acta.nro_acta

**8A queda cerrado y habilitado para pasar a 8B.**

---

## Micro-slice 8A-5.1 - Tipos SMALLINT en FalFirmanteVersionHabilitacion [COMPLETADO]

**Fecha:** 2026-06-30
**Build al cierre:** 454/454 tests passing. BUILD SUCCESS.
**Tests nuevos:** 4 (8A3-39 mecanismoFirmaReq Short informado; 8A3-40 guardrail x3 enums documentales)

### Objetivo

Corregir tipos de tipoDocu, rolFirmaReq y mecanismoFirmaReq en FalFirmanteVersionHabilitacion
para alinear con tipo_docu SMALLINT, rol_firma_req SMALLINT, mecanismo_firma_req SMALLINT
del modelo MariaDB productivo, antes de pasar a 8B.

### Correcciones realizadas

| Campo | Tipo anterior | Tipo corregido | Razon |
|-------|---------------|----------------|-------|
| tipoDocu (domain model) | String | short | tipo_docu SMALLINT en MariaDB |
| rolFirmaReq (domain model) | String | short | rol_firma_req SMALLINT en MariaDB |
| mecanismoFirmaReq (domain model) | String | Short | mecanismo_firma_req SMALLINT nullable en MariaDB |
| tipoDocu (command/request) | String | Short | nullable para validacion null |
| rolFirmaReq (command/request) | String | Short | nullable para validacion null |
| mecanismoFirmaReq (command/request) | String | Short | nullable permitido |
| tipoDocu (response) | String | short | siempre presente despues de creacion |
| rolFirmaReq (response) | String | short | siempre presente despues de creacion |
| mecanismoFirmaReq (response) | String | Short | nullable |

### Archivos modificados

- domain/model/FalFirmanteVersionHabilitacion.java
- application/command/AgregarHabilitacionFirmanteCommand.java
- application/command/DesactivarHabilitacionFirmanteCommand.java
- web/dto/AgregarHabilitacionFirmanteRequest.java
- web/dto/FirmanteHabilitacionResponse.java
- application/service/FirmanteService.java (validaciones: null check, no isBlank)
- repository/FirmanteRepository.java (firmas de findHabilitacionActiva/existeHabilitacionActiva)
- repository/memory/InMemoryFirmanteRepository.java (comparaciones short == short)
- web/FirmanteController.java (desactivar usa DesactivarHabilitacionFirmanteRequest)
- test/FirmanteTest.java (Short values en todos los tests de habilitacion)

### Archivos nuevos

- web/dto/DesactivarHabilitacionFirmanteRequest.java (Short tipoDocu, Short rolFirmaReq, String idUserAlta)

### Decisiones

- No se crearon enums RolFirmaReq, MecanismoFirmaReq, EstadoFirmaReq. Quedan para 8C.
- No se implemento FalDocumentoFirmaReq.
- No se implemento bandeja de firma ni firma efectiva.
- Los valores SMALLINT en tests son representativos (1, 2, 3, 4 para tipoDocu; 10, 11 para rolFirmaReq; 5 para mecanismoFirmaReq).
- PK funcional (idFirmante + verFirmante + tipoDocu + rolFirmaReq) ahora usa short para comparacion numerica.
- No se inicio MariaDB/JDBC.
- No se toco Angular.

### Deltas restantes para 8C

- TipoDocumento vs tipo_docu productivo: enum actual no alineado con catalogo SMALLINT.
- EstadoFirmaDocumento vs estado_firma productivo: enum actual no alineado.
- estado_docu sin catalogo productivo numerico explicito.
- RolFirmaReq, MecanismoFirmaReq: enums documentales pendientes de catalogo productivo cerrado.

### Reglas criticas activas desde 8A-5.1

- tipoDocu es short (obligatorio). Null rechazado.
- rolFirmaReq es short (obligatorio). Null rechazado.
- mecanismoFirmaReq es Short nullable. Null permitido.
- InMemoryFirmanteRepository usa == para comparar short primitivos (no .equals de String).
- Los repositorios son reemplazables por JDBC sin tocar servicios.

### Proximo slice

**8B-1 - Modelo de talonarios, con diagnostico obligatorio contra modelo MariaDB.**

**8A queda cerrada y alineada con MariaDB para pasar a 8B.**


---

## Slice 8B-1 - Diagnostico de modelo de talonarios y numeracion

**Fecha:** 2026-06-30
**Estado:** Diagnostico completado. No se implemento funcionalidad.
**Build:** 454/454 tests passing. BUILD SUCCESS.
**docs/spec-as-source historica:** no existe (confirmado).

### Tablas relevadas del modelo productivo MariaDB

**Tablas del subsistema de numeracion/talonarios:**

| Tabla MariaDB | Descripcion | Existe en Java |
|---|---|---|
| num_politica | Politica de numeracion: formato, reinicio anual, prefijo, serie | No |
| num_talonario | Talonario concreto (electronico o manual fisico) con referencia a SEQUENCE MariaDB | No |
| num_talonario_ambito | Regla de autorizacion: donde aplica cada talonario (dependencia, tipo_docu, alcance, prioridad) | No |
| num_talonario_inspector | Asignacion de talonario manual fisico a inspector | No |
| num_talonario_movimiento | Registro operativo unico de cada numero de talonario (USADO, ANULADO, DEVUELTO, RENDIDO, JUSTIFICADO) | No |

**NOTA CRITICA:** El prompt menciona
um_serie_talonario pero NO existe como tabla separada en el modelo MariaDB.
El concepto de serie esta representado como campo serie VARCHAR(12) dentro de
um_talonario.

**Campos de numeracion en fal_acta (modelo MariaDB):**

| Campo MariaDB | Tipo | Nombre Java actual | Alineado |
|---|---|---|---|
| nro_acta | VARCHAR(30) NULL | numeroActa | NO - deberia ser nroActa |
| id_talonario | BIGINT NULL FK | AUSENTE | NO - campo faltante |
| nro_talonario_usado | INT NULL | AUSENTE | NO - campo faltante |

**Campos de numeracion en fal_documento (modelo MariaDB):**

| Campo MariaDB | Tipo | Nombre Java actual | Alineado |
|---|---|---|---|
| nro_docu | VARCHAR(20) NULL | numeroVisible | NO - deberia ser nroDocu |
| id_talonario | BIGINT NULL FK | AUSENTE | NO - campo faltante |
| nro_talonario_usado | INT NULL | AUSENTE | NO - campo faltante |

### Entidades Java propuestas

| Tabla MariaDB | Entidad Java propuesta | Implementar en 8B | Observacion |
|---|---|---|---|
| num_politica | NumPolitica | Si (8B-2) | Cabecera de politica |
| num_talonario | NumTalonario | Si (8B-2) | Talonario concreto + referencia a SEQUENCE |
| num_talonario_ambito | NumTalonarioAmbito | Si (8B-2) | Regla de autorizacion por dependencia/tipo_docu/alcance |
| num_talonario_inspector | NumTalonarioInspector | Si (8B-3) | Asignacion de talonario manual a inspector |
| num_talonario_movimiento | NumTalonarioMovimiento | Si (8B-4) | Registro operativo de numero |
| fal_acta.nro_acta/id_talonario | Ajuste FalActa | Si (8B-4) | Agregar idTalonario, nroTalonarioUsado; renombrar numeroActa a nroActa |

### Catalogos/enums productivos identificados

| Catalogo | Valores productivos | Existe en Java | Decision |
|---|---|---|---|
| clase_numeracion (num_politica) | ACTA (1), DOCUMENTO (2) | No | Crear ClaseNumeracion |
| tipo_talonario (num_talonario) | ELECTRONICO (1), MANUAL_FISICO (2) | No | Crear TipoTalonario |
| alcance (num_talonario_ambito) | GLOBAL (1), DEPENDENCIA (2), TRANSVERSAL_DOCUMENTO (3) | No | Crear AlcanceTalonario |
| estado_numero_talonario (num_talonario_movimiento) | USADO (1), ANULADO (2), DEVUELTO_SIN_USAR (3), RENDIDO (4), JUSTIFICADO (5) | No | Crear EstadoNumeroTalonario |
| motivo_anulacion_talonario (num_talonario_movimiento) | ERROR_LABRADO (1), ROTURA_FORMULARIO (2), DUPLICADO (3), EXTRAVIO (4), OTRO (5) | No | Crear MotivoAnulacionTalonario |
| estado_asignacion (num_talonario_inspector) | ENTREGADO (1), DEVUELTO (2), CERRADO (3), OBSERVADO (4) | Si (8B-3) | Crear EstadoAsignacionTalonario con codigos productivos confirmados en 8B-3D |

### Reglas de numeracion (diagnostico)

1. La dependencia del inspector determina el tipo_acta y el talonario activo clase ACTA a usar.
2. La politica (num_politica) define: formato visible, si reinicia anual, si incluye prefijo/serie/anio, longitud del numero.
3. El talonario (num_talonario) referencia un SEQUENCE de MariaDB (nombre_secuencia). El proximo numero se obtiene con NEXT VALUE FOR.
4. num_talonario NO tiene ultimo_nro_usado. La concurrencia es por SEQUENCE nativo.
5. num_talonario_ambito define donde aplica cada talonario: por dependencia (id_dep + ver_dep), globalmente, o transversal por tipo_docu. Resuelve prioridad por campo prioridad (SMALLINT).
6. Reinicio anual: si politica.si_reinicio_anual = true, el talonario activo del ano actual es el que tiene campo anio coincidente.
7. Rango: num_talonario.nro_desde y nro_hasta (nullable si sin limite).
8. Talonario activo: num_talonario.si_activo = true y si_bloqueado = false.
9. Serie activa: no hay tabla separada; serie es campo en num_talonario. El talonario activo tiene la serie correspondiente.
10. Diferencia acta/documento: clase_talonario = ACTA o DOCUMENTO en num_talonario y num_talonario_ambito.
11. fal_acta vincula con id_talonario (FK) y nro_talonario_usado (INT). nro_acta es el numero visible compuesto.
12. nro_acta es numero visible; id_talonario + nro_talonario_usado trazan origen tecnico.
13. Prefijo/sufijo: si_incluye_prefijo y prefijo en num_politica. No hay sufijo explicito en el modelo.
14. Periodo/anio: campo anio SMALLINT nullable en num_talonario. Obligatorio si politica reinicia anual.
15. Control de concurrencia: delegado a SEQUENCE nativo MariaDB. En in-memory: simular sin concurrencia real.
16. num_talonario_movimiento: tabla con UNIQUE(id_talonario, nro_talonario). Registra estado de cada numero.
17. num_talonario_inspector: asignacion de talonario MANUAL_FISICO a inspector. Solo una asignacion activa por talonario (si_activa + talonario_id_activo columna generada).
18. Huecos de numeracion: no permitidos en talonarios manuales fisicos al rendir/cerrar. Cada numero del rango debe tener movimiento.
19. Anulacion de numero: estado_numero = ANULADO + motivo_anulacion obligatorio.
20. Talonarios electronicos: no exigen control de huecos durante uso; la SEQUENCE puede saltar por cache. El control de huecos logicos aplica al rendir talonarios manuales fisicos.

### Relacion con dependencias (8A-1)

- num_talonario_ambito.id_dep + ver_dep apuntan a fal_dependencia_version.
- Talonario puede ser global (id_dep NULL, alcance = GLOBAL).
- Talonario puede asociarse a tipo_acta (tipo_acta SMALLINT en num_talonario_ambito, opcional).
- Talonario puede asociarse a tipo_docu (tipo_docu SMALLINT en num_talonario_ambito).
- Talonario puede aplicar a varias dependencias: a traves de multiples filas en num_talonario_ambito.
- Prioridad entre ambitos: campo prioridad SMALLINT en num_talonario_ambito. Menor valor = mayor prioridad (interpretacion estandar; el modelo define el campo pero no explicita el criterio de desempate, debe documentarse al implementar).

### Relacion con inspectores y talonarios manuales (8A-2)

- num_talonario_inspector.id_insp + ver_insp apuntan al inspector en fal_inspector_version.
- Solo talonarios tipo MANUAL_FISICO pueden asignarse a inspectores.
- Existe control de rango completo (nro_desde/nro_hasta en num_talonario).
- Existe control de numero usado/anulado/justificado: num_talonario_movimiento con UNIQUE por (id_talonario, nro_talonario).
- Existe trazabilidad de anulacion: estado_numero = ANULADO + motivo_anulacion obligatorio.
- Existe motivo obligatorio: motivo_anulacion SMALLINT en num_talonario_movimiento, requerido si ANULADO.
- Control de huecos al rendir: no puede cerrar/rendir si quedan numeros del rango sin movimiento.
- El modelo tiene soporte fisico completo para talonarios manuales. No faltan tablas.

### Impacto en FalActa

| Aspecto | Estado |
|---|---|
| FalActa.numeroActa | Existe. Deberia llamarse nroActa (alineacion con nro_acta productivo). |
| FalActa.idTalonario | AUSENTE. Debe agregarse. Tipo: Long (BIGINT). Nullable. |
| FalActa.nroTalonarioUsado | AUSENTE. Debe agregarse. Tipo: Integer (INT). Nullable. |
| Generacion de numero actual | Mock (numeroActa asignado manualmente). Debe reemplazarse por servicio de numeracion en 8B-4. |
| Tests actuales | Los tests que usan numeroActa como String no se rompen por el renombrado si se usa setNroActa con alias, pero en 8B-4 hay que actualizar los tests que generen numeros. |

Estos cambios en FalActa son MINIMOS e INEQUIVOCOS. Se documentan ahora, se corrigen en 8B-4 al implementar el servicio de numeracion.

### Deltas detectados

| Delta | Regla funcional afectada | Impacto | Propuesta | Bloquea implementacion |
|---|---|---|---|---|
| num_serie_talonario no existe como tabla | El prompt menciona esta tabla pero no existe. Serie es campo en num_talonario | Nomenclatura incorrecta en el prompt | Usar num_talonario.serie directamente | No |
| FalActa.idTalonario ausente | Trazabilidad de talonario en acta | Medio - falta vincular acta con talonario | Agregar en 8B-4 al implementar numeracion | No |
| FalActa.nroTalonarioUsado ausente | Trazabilidad tecnica del numero usado | Medio - falta control tecnico | Agregar en 8B-4 al implementar numeracion | No |
| FalActa.numeroActa vs nro_acta | Desalineacion de nombre | Bajo - semanticamente equivalente | Renombrar a nroActa en 8B-4 | No |
| FalDocumento.nroDocu vs numeroVisible | Desalineacion de nombre | Bajo | Renombrar en 8C | No |
| FalDocumento.idTalonario ausente | Trazabilidad de talonario en documento | Medio | Agregar en 8C | No |
| Criterio de desempate de prioridad | Campo prioridad existe pero no hay documentacion de interpretacion | Bajo - no bloquea implementacion inicial | Documentar al implementar 8B-2 | No |
| in-memory no tiene SEQUENCE real | Concurrencia de numeracion no implementable en in-memory | Solo aplica para MariaDB/JDBC (Etapa 9) | Simular con AtomicLong en in-memory | No |

**No hay deltas bloqueantes.**

### Roadmap 8B recomendado

- 8B-1: Diagnostico de talonarios y numeracion [COMPLETADO - 2026-06-30]
- 8B-2: Implementar NumPolitica, NumTalonario, NumTalonarioAmbito in-memory con repositorios y endpoints CRUD basicos
- 8B-3: Servicio de asignacion de talonario a inspector (NumTalonarioInspector) + endpoints [DESBLOQUEADO por 8B-3D]
- 8B-4: Servicio de emision de numero de acta (NumTalonarioMovimiento + ajuste FalActa) + endpoints
- 8B-5: Control de talonarios documentales (clase DOCUMENTO) + integracion con FalDocumento
- 8B-6: Control de talonarios manuales fisicos: rangos, huecos, anulaciones, rendicion
- 8B-7: Auditoria tecnica 8B: alineacion con modelo MariaDB, deltas, build final

### Invariantes de 8B

- No iniciar MariaDB/JDBC.
- No tocar Angular.
- Cada entidad nueva debe alinearse con el modelo MariaDB antes de implementarse.
- Usar nombres productivos: idTalonario, nroTalonario, nroActa, nroDocu, siActivo, siBloqueado, nroDesde, nroHasta, fhAlta, idUserAlta, versionRow, politicaId, talonarioId.
- El servicio de numeracion in-memory usara AtomicLong por talonario (sin SEQUENCE real).
- La logica de SEQUENCE (NEXT VALUE FOR) se documentara como deuda tecnica para Etapa 9/JDBC.


---

## Slice 8B-2 - Implementar politicas, talonarios y ambitos in-memory

**Fecha:** 2026-06-30
**Estado:** Completado.
**Build:** 506/506 tests passing. BUILD SUCCESS. (52 tests nuevos)
**docs/spec-as-source historica:** no existe (confirmado).

### Archivos creados

**Enums:**
- domain/enums/ClaseNumeracion.java - ACTA=1, DOCUMENTO=2
- domain/enums/TipoTalonario.java - ELECTRONICO=1, MANUAL_FISICO=2
- domain/enums/AlcanceTalonario.java - GLOBAL=1, DEPENDENCIA=2, TRANSVERSAL_DOCUMENTO=3

**Domain models:**
- domain/model/NumPolitica.java
- domain/model/NumTalonario.java
- domain/model/NumTalonarioAmbito.java

**Commands:**
- application/command/CrearPoliticaNumeracionCommand.java
- application/command/CrearTalonarioCommand.java
- application/command/CrearTalonarioAmbitoCommand.java

**Repository:**
- repository/TalonarioRepository.java (interfaz)
- repository/memory/InMemoryTalonarioRepository.java

**Service:**
- application/service/TalonarioService.java

**DTOs:**
- web/dto/CrearPoliticaNumeracionRequest.java
- web/dto/PoliticaNumeracionResponse.java
- web/dto/CrearTalonarioRequest.java
- web/dto/TalonarioResponse.java
- web/dto/CrearTalonarioAmbitoRequest.java
- web/dto/TalonarioAmbitoResponse.java

**Controller:**
- web/TalonarioController.java
  - POST /api/faltas/talonarios/politicas
  - GET /api/faltas/talonarios/politicas
  - GET /api/faltas/talonarios/politicas/{id}
  - POST /api/faltas/talonarios
  - GET /api/faltas/talonarios
  - GET /api/faltas/talonarios/{id}
  - POST /api/faltas/talonarios/{id}/ambitos
  - GET /api/faltas/talonarios/{id}/ambitos

**Tests:**
- test/application/TalonarioTest.java (52 tests)

### Decisiones

- NumPolitica alineada con num_politica del modelo MariaDB.
- NumTalonario alineado con num_talonario (campo serie, versionRow=1 inicial).
- NumTalonarioAmbito alineado con num_talonario_ambito.
- ClaseNumeracion con ACTA=1, DOCUMENTO=2 (catalogo productivo).
- TipoTalonario con ELECTRONICO=1, MANUAL_FISICO=2 (catalogo productivo).
- AlcanceTalonario con GLOBAL=1, DEPENDENCIA=2, TRANSVERSAL_DOCUMENTO=3 (catalogo productivo).
- No se creo NumSerieTalonario. La serie es campo de NumTalonario.
- No se implemento NumTalonarioInspector.
- No se implemento NumTalonarioMovimiento.
- No se emitieron numeros.
- No se toco FalActa.
- No se inicio MariaDB/JDBC.
- No se toco Angular.
- Guardrail de BOM resuelto: archivos escritos con [System.Text.UTF8Encoding](False).

### Criterio de prioridad en ambito

El campo prioridad en NumTalonarioAmbito esta implementado. El criterio de desempate
(menor valor = mayor prioridad) se documenta pero no se implementa la logica de resolucion
automatica todavia: eso corresponde a 8B-4 cuando se necesite resolver el talonario para
emitir un numero.

### Proximo slice

**8B-3 - NumTalonarioInspector: asignacion de talonario manual fisico a inspector.**


---

## Slice 8B-3 - Asignacion de talonario manual fisico a inspector (NumTalonarioInspector)

**Fecha:** 2026-06-30

**Estado: DESBLOQUEADO por Micro-slice 8B-3D (2026-06-30)**

### Motivo del bloqueo

-
um_talonario_inspector.estado_asignacion es SMALLINT en el modelo MariaDB productivo.
- El modelo productivo lista los estados (ENTREGADO, DEVUELTO, CERRADO, OBSERVADO) en la columna "Regla" de la definicion de tabla (Seccion 9.5 del MODELO_MARIADB).
- El modelo productivo NO define una tabla de catalogo separada con codigos numericos explicitos para estado_asignacion.
- Comparacion con enums que SI tienen codigos confirmados:
  - ClaseNumeracion: ACTA (1), DOCUMENTO (2) -- catalogo explicito secciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 9.x
  - TipoTalonario: ELECTRONICO (1), MANUAL_FISICO (2) -- catalogo explicito secciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 9.3.4
  - AlcanceTalonario: GLOBAL (1), DEPENDENCIA (2), TRANSVERSAL_DOCUMENTO (3) -- catalogo explicito
  - EstadoNumeroTalonario: USADO (1), ANULADO (2), DEVUELTO_SIN_USAR (3), RENDIDO (4), JUSTIFICADO (5) -- catalogo explicito secciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 9.6
  - MotivoAnulacionTalonario: ERROR_LABRADO (1)...OTRO (5) -- catalogo explicito secciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 9.6
- EstadoAsignacionTalonario NO tiene tabla de catalogo con codigos numericos. Solo aparece como descripcion de columna.
- No se pueden inventar codigos.
- No se asume ENTREGADO=1, DEVUELTO=2, CERRADO=3, OBSERVADO=4 sin confirmacion productiva.

### Micro-slice requerido: 8B-3D

**8B-3D - Completar catalogo productivo estado_asignacion_talonario con codigos numericos**

Objetivo: Agregar en docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md la
tabla de catalogo para estado_asignacion con los codigos SMALLINT explicitos:

`
Catalogo estado_asignacion (num_talonario_inspector):

| ID | Codigo enum | Descripcion |
|---:|---|---|
| 1  | ENTREGADO  | Talonario entregado al inspector, activo |
| 2  | DEVUELTO   | Talonario devuelto simple, sin rendir |
| 3  | CERRADO    | Talonario cerrado administrativamente (con rendicion completa) |
| 4  | OBSERVADO  | Talonario observado, pendiente de resolucion |
`

Solo implementar si el duenio del modelo confirma los codigos.
No inventar ni asumir nada.

### Estado del build

- Build: 506/506 tests passing. BUILD SUCCESS.
- No se creo Java.
- No se inicio MariaDB/JDBC.
- No se toco Angular.
- docs/spec-as-source historica no existe.

### Proximo paso requerido

8B-3D - Completar catalogo productivo estado_asignacion_talonario con codigos numericos.
Solo entonces implementar 8B-3.


---

## Micro-slice 8B-3D - Catalogo estado_asignacion_talonario [COMPLETADO - 2026-06-30]

### Estado

Completado. Build: 506/506 tests passing. BUILD SUCCESS.

### Objetivo

Completar el catalogo productivo `estado_asignacion_talonario` con codigos SMALLINT explicitos
para desbloquear la implementacion de Slice 8B-3.

### Catalogo confirmado

| ID | Codigo | Descripcion |
|---:|---|---|
| 1 | `ENTREGADO` | Talonario manual fisico entregado al inspector y asignacion activa |
| 2 | `DEVUELTO` | Talonario manual fisico devuelto por el inspector |
| 3 | `CERRADO` | Asignacion cerrada administrativamente |
| 4 | `OBSERVADO` | Asignacion observada por inconsistencia, rendicion pendiente o control administrativo |

### Archivos actualizados

- `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` - Seccion 9.5: catalogo estado_asignacion_talonario agregado con IDs numericos.
- `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md` - Entrada micro-slice 8B-3D agregada.
- `backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md` - Estado 8B-3 actualizado.
- `backend/api-faltas-core/docs/spec-as-source/100-etapa-8-plan-maestro-api-multi-app.md` - Estado 8B-3 actualizado.

### Decisiones

- El campo `estado_asignacion` sigue siendo `SMALLINT NOT NULL`. No cambia el DDL.
- No se implemento Java.
- No se inicio MariaDB/JDBC.
- No se toco Angular.

### Proximo slice

**8B-3 - NumTalonarioInspector y asignacion de talonario manual fisico a inspector.**

Ahora puede implementar:
- `EstadoAsignacionTalonario` con ENTREGADO=1, DEVUELTO=2, CERRADO=3, OBSERVADO=4
- `NumTalonarioInspector`
- Asignacion y devolucion simple
- NO implementar todavia: NumTalonarioMovimiento, emision, rendicion, control de huecos, FalActa


---

## Slice 8B-3 - NumTalonarioInspector y asignacion talonario manual fisico a inspector [COMPLETADO - 2026-06-30]

### Estado

Completado. Build: 532/532 tests passing. BUILD SUCCESS.

### Objetivo

Implementar asignacion de talonarios manuales fisicos a inspectores.

### Implementado

- `EstadoAsignacionTalonario` con codigos productivos:
  - ENTREGADO = 1
  - DEVUELTO = 2
  - CERRADO = 3
  - OBSERVADO = 4
- `NumTalonarioInspector` alineado con `num_talonario_inspector` del modelo MariaDB productivo.
- `AsignarTalonarioInspectorCommand` y `DevolverTalonarioInspectorCommand`.
- DTOs: `AsignarTalonarioInspectorRequest`, `DevolverTalonarioInspectorRequest`, `TalonarioInspectorResponse`.
- `TalonarioRepository` ampliado con 7 nuevos metodos para asignaciones inspector.
- `InMemoryTalonarioRepository` implementa los 7 nuevos metodos.
- `TalonarioService` ampliado con logica de asignacion, devolucion y listados.
- `TalonarioController` ampliado con 4 nuevos endpoints.
- `InspectorController` ampliado con endpoint `GET /api/faltas/inspectores/{idInsp}/versiones/{verInsp}/talonarios`.
- `TalonarioTest` ampliado con 36 nuevos tests (EstadoAsignacionTalonarioTest, AsignacionInspectorTest, DevolucionTest, GuardrailFueraDeScope extendido).

### Reglas funcionales aplicadas

- Solo `MANUAL_FISICO` puede asignarse a inspector. `ELECTRONICO` es rechazado.
- El talonario debe estar activo y no bloqueado.
- No puede haber dos asignaciones activas para el mismo talonario.
- El inspector y su version especifica deben existir.
- Estado inicial de asignacion: `ENTREGADO`, `siActiva = true`, `talonarioIdActivo = idTalonario`.
- Devolucion simple: `DEVUELTO`, `siActiva = false`, `talonarioIdActivo = null`.
- `CERRADO` y `OBSERVADO` existen en el enum pero sus flujos completos son de 8B-6.

### No implementado (queda para siguientes slices)

- `NumTalonarioMovimiento` (8B-4)
- Emision de numero de acta (8B-4)
- Rendicion completa con control de huecos (8B-6)
- Anulacion de numeros (8B-6)
- Cierre de asignacion (CERRADO) (8B-6)
- Observacion de asignacion (OBSERVADO) (8B-6)
- Integracion con `FalActa` (8B-4)
- MariaDB/JDBC (Slice 9)
- Angular (no aplica en este modulo)

### Archivos creados

- `domain/enums/EstadoAsignacionTalonario.java`
- `domain/model/NumTalonarioInspector.java`
- `application/command/AsignarTalonarioInspectorCommand.java`
- `application/command/DevolverTalonarioInspectorCommand.java`
- `web/dto/AsignarTalonarioInspectorRequest.java`
- `web/dto/DevolverTalonarioInspectorRequest.java`
- `web/dto/TalonarioInspectorResponse.java`

### Archivos actualizados

- `repository/TalonarioRepository.java`
- `repository/memory/InMemoryTalonarioRepository.java`
- `application/service/TalonarioService.java`
- `web/TalonarioController.java`
- `web/InspectorController.java`
- `test/application/TalonarioTest.java`

### Proximo slice

**8B-4 - NumTalonarioMovimiento, emision de numero de acta y ajuste FalActa.** [COMPLETADO - ver seccion siguiente]

---

## Slice 8B-4 - NumTalonarioMovimiento, emision de numero de acta y ajuste FalActa [COMPLETADO - 2026-06-30]

### Estado

Completado. Build: 561/561 tests passing. BUILD SUCCESS.

### Objetivo

Implementar el primer flujo operativo de numeracion de actas in-memory.

### Implementado

- `EstadoNumeroTalonario` con codigos productivos:
  - USADO = 1
  - ANULADO = 2
  - DEVUELTO_SIN_USAR = 3
  - RENDIDO = 4
  - JUSTIFICADO = 5
- `MotivoAnulacionTalonario` con codigos productivos:
  - ERROR_LABRADO = 1
  - ROTURA_FORMULARIO = 2
  - DUPLICADO = 3
  - EXTRAVIO = 4
  - OTRO = 5
- `NumTalonarioMovimiento` alineado con `num_talonario_movimiento` del modelo MariaDB productivo.
- `EmitirNumeroActaCommand` y `NumeroActaEmitidoResponse`.
- DTOs: `EmitirNumeroActaRequest`, `TalonarioMovimientoResponse`.
- `TalonarioRepository` ampliado con metodos de movimiento y contador in-memory.
- `InMemoryTalonarioRepository` implementa movimientos y contador por talonario (`ConcurrentHashMap<Long, AtomicInteger>`).
- `TalonarioService.emitirNumeroActa()`: logica completa de seleccion de talonario por ambitos, prioridad y fecha, con deteccion de ambiguedad.
- `TalonarioService.listarMovimientosPorTalonario()`.
- `TalonarioController`: POST /api/faltas/talonarios/numeracion/actas/emitir, GET /api/faltas/talonarios/{idTalonario}/movimientos.
- `FalActa` alineada con modelo MariaDB: `nroActa` (renombrado desde `numeroActa`), `idTalonario`, `nroTalonarioUsado`.
- Validacion prioridad positiva en `crearAmbito`.
- Validacion clase coincidente en `crearAmbito`.
- Validacion GLOBAL sin idDep en `crearAmbito`.
- `TalonarioTest` ampliado con 29 nuevos tests de 8B-4.

### Reglas funcionales aplicadas

- Solo talonarios `ACTA` activos y no bloqueados.
- El ambito debe estar activo y vigente para la fecha.
- Seleccion por prioridad ascendente (menor numero = mayor prioridad).
- Empate real de prioridad falla por ambiguedad; no se elige arbitrariamente.
- Si no hay talonario compatible, falla con error controlado.
- Primer numero emitido = `nroDesde`. Cada emision incrementa el contador in-memory.
- Si `nroHasta` se supera, falla con error controlado.
- Movimiento registrado con `estadoNumero = USADO`, `documentoId = null`.
- `nroActa` visible se construye segun `NumPolitica` (prefijo, serie, anio, longitud con ceros).
- UNIQUE logico `idTalonario + nroTalonario` validado in-memory.

### No implementado (queda para siguientes slices)

- Rendicion completa con control de huecos (8B-6)
- Anulacion de numeros (8B-6)
- Devoluciones de numeros sin usar (8B-6)
- Justificacion de huecos (8B-6)
- Cierre manual de talonario (8B-6)
- Integracion con `FalDocumento` (8B-5)
- Talonarios documentales (8B-5)
- MariaDB/JDBC (Slice 9)
- Angular (no aplica en este modulo)

### Archivos creados

- `domain/enums/EstadoNumeroTalonario.java`
- `domain/enums/MotivoAnulacionTalonario.java`
- `domain/model/NumTalonarioMovimiento.java`
- `application/command/EmitirNumeroActaCommand.java`
- `application/result/NumeroActaEmitidoResponse.java`
- `web/dto/EmitirNumeroActaRequest.java`
- `web/dto/TalonarioMovimientoResponse.java`

### Archivos actualizados

- `domain/model/FalActa.java` (nroActa, idTalonario, nroTalonarioUsado)
- `repository/TalonarioRepository.java`
- `repository/memory/InMemoryTalonarioRepository.java`
- `application/service/TalonarioService.java`
- `web/TalonarioController.java`
- `test/application/TalonarioTest.java`

### Proximo slice sugerido

- **8B-5** si se continua con talonarios documentales.
- **8B-6** si se prioriza control manual fisico, rendicion, huecos, anulaciones y justificaciones.
---

## Slice 8B-6 - Control de talonarios manuales fisicos: rendicion, huecos, anulaciones y justificaciones [COMPLETADO]

### Objetivo

Completar el control operativo de numeros de talonarios manuales fisicos. Cada numero del rango debe quedar en estado final antes de cerrar la asignacion.

### Implementado

**Commands nuevos:**
- `AnularNumeroTalonarioCommand`
- `JustificarNumeroTalonarioCommand`
- `DevolverNumeroSinUsarCommand`
- `CerrarAsignacionTalonarioInspectorCommand`

**DTOs nuevos:**
- `AnularNumeroTalonarioRequest`
- `JustificarNumeroTalonarioRequest`
- `DevolverNumeroSinUsarRequest`
- `CerrarAsignacionTalonarioInspectorRequest`
- `CierreAsignacionTalonarioResponse`

**Metodos de servicio en TalonarioService:**
- `anularNumeroTalonario()`: registra movimiento ANULADO con motivoAnulacion obligatorio (OTRO exige observacion).
- `justificarNumeroTalonario()`: registra movimiento JUSTIFICADO con observacion obligatoria.
- `devolverNumeroSinUsar()`: registra movimiento DEVUELTO_SIN_USAR.
- `cerrarAsignacionTalonarioInspector()`: cierra asignacion solo si todos los numeros del rango tienen movimiento; reporta faltantes si hay huecos.
- `listarNumerosFaltantes()`: lista numeros del rango sin movimiento.

**Repository ampliado:**
- `TalonarioRepository.buscarNumerosFaltantesEnRango()` agregado.
- `InMemoryTalonarioRepository.buscarNumerosFaltantesEnRango()` implementado.

**Endpoints nuevos en TalonarioController:**
- `POST /api/faltas/talonarios/{idTalonario}/numeros/{nroTalonario}/anular`
- `POST /api/faltas/talonarios/{idTalonario}/numeros/{nroTalonario}/justificar`
- `POST /api/faltas/talonarios/{idTalonario}/numeros/{nroTalonario}/devolver-sin-usar`
- `PUT /api/faltas/talonarios/asignaciones-inspector/{idAsignacion}/cerrar`
- `GET /api/faltas/talonarios/{idTalonario}/numeros/faltantes`

### Reglas funcionales aplicadas

- Solo talonarios `MANUAL_FISICO` pueden anularse, justificarse, devolverse o cerrarse por este flujo.
- Regla UNIQUE logica: no puede haber dos movimientos para el mismo `idTalonario + nroTalonario`.
- Numero fuera de rango `[nroDesde, nroHasta]` rechazado con error controlado.
- `motivoAnulacion = OTRO` exige `observacion` obligatoria.
- Justificacion: `observacion` obligatoria; `motivoAnulacion = null`.
- Devolucion sin usar: campos minimos; `motivoAnulacion = null`.
- Cierre de asignacion: requiere que todos los numeros de `[nroDesde, nroHasta]` tengan movimiento.
- Si `nroHasta = null`, el cierre completo con control de huecos falla con error controlado.
- Si hay huecos: responde con `cerrada = false` y lista `numerosFaltantes`.
- Al cerrar: `estadoAsignacion = CERRADO`, `siActiva = false`, `talonarioIdActivo = null`, `fhDevolucion` y `idUserDevolucion` completados.
- Estado `RENDIDO` disponible en catalogo pero no se generan movimientos masivos RENDIDO en este slice.

### No implementado (guardrail estricto)

- FalDocumento / talonarios documentales (8B-5).
- MariaDB/JDBC (Slice 9).
- Angular (no aplica en este modulo).
- No se crean nuevos enums ni se cambian codigos de enums existentes.

### Tests

- `TalonarioManualFisicoTest.java` creado con 40 tests:
  - 10 tests de anulacion de numero.
  - 6 tests de justificacion.
  - 4 tests de devolucion sin usar.
  - 10 tests de cierre/rendicion de asignacion.
  - 4 tests de numeros faltantes en rango.
  - 6 tests de guardrail fuera de scope (incluyendo invariantes de codigos de enums).
- `TalonarioTest.java` actualizado: guardrails de 8B-3 y 8B-4 corregidos para reflejar que `anularNumeroTalonario` existe desde 8B-6.

**Build al cierre de 8B-6: 601/601 tests passing. BUILD SUCCESS.**

### Archivos creados

- `application/command/AnularNumeroTalonarioCommand.java`
- `application/command/JustificarNumeroTalonarioCommand.java`
- `application/command/DevolverNumeroSinUsarCommand.java`
- `application/command/CerrarAsignacionTalonarioInspectorCommand.java`
- `web/dto/AnularNumeroTalonarioRequest.java`
- `web/dto/JustificarNumeroTalonarioRequest.java`
- `web/dto/DevolverNumeroSinUsarRequest.java`
- `web/dto/CerrarAsignacionTalonarioInspectorRequest.java`
- `web/dto/CierreAsignacionTalonarioResponse.java`
- `test/application/TalonarioManualFisicoTest.java`

### Archivos actualizados

- `repository/TalonarioRepository.java` (buscarNumerosFaltantesEnRango)
- `repository/memory/InMemoryTalonarioRepository.java` (buscarNumerosFaltantesEnRango)
- `application/service/TalonarioService.java` (4 metodos nuevos + helper validarNroEnRango)
- `web/TalonarioController.java` (5 endpoints nuevos)
- `test/application/TalonarioTest.java` (guardrails actualizados)

### Proximo slice sugerido

- **8B-5** talonarios documentales e integracion con FalDocumento.
- **8B-7** auditoria tecnica 8B si se decide cerrar primero numeracion de actas.


---

## Slice 8B-7 - Auditoria tecnica 8B - COMPLETADO - 2026-07-01

**Build:** 601/601 tests passing. BUILD SUCCESS.

**Objetivo:** Auditoria tecnica completa de la etapa 8B (numeracion de actas y talonarios manuales fisicos) antes de avanzar a 8B-5 o 8C.

### Resultado de auditoria

#### NumPolitica
**OK.** Todos los campos Java alineados con modelo MariaDB (id, codigo, descripcion, claseNumeracion, siReinicioAnual, siIncluyePrefijo, prefijo, siIncluyeAnio, ormatoAnio, siIncluyeSerie, longitudNro, ormatoVisible, siActiva, hVigDesde, hVigHasta). Campos hAlta/idUserAlta presentes en Java pero no listados explicitamente en tabla spec de
um_politica (gap documental menor, no error de codigo; coherentes con patron del resto de tablas).

#### NumTalonario
**OK.** Todos los campos coinciden con tabla MariaDB. ersionRow, politicaId, 	ipoTalonario, claseTalonario,
ombreSecuencia UNIQUE,
roDesde/
roHasta, siBloqueado/codDesbloqueo todos correctos.

#### NumTalonarioAmbito
**OK.** Todos los campos coinciden. claseTalonario, 	ipoDocu (Short nullable), 	ipoActa (Short nullable), idDep/erDep en par, lcance, prioridad, hDesde/hHasta, siActivo. Metodo esVigenteEn() correcto.

#### NumTalonarioInspector
**OK.** Todos los campos coinciden. 	alonarioIdActivo gestionado correctamente (= idTalonario cuando activa, null cuando no activa). ENTREGADO inicial, DEVUELTO en devolucion simple.

#### NumTalonarioMovimiento
**OK.** Todos los campos coinciden. UNIQUE logico idTalonario + nroTalonario simulado en memoria. ctaId es String en prototipo in-memory vs BIGINT en MariaDB (delta conocido: FalActa usa String en prototipo).

#### FalActa numeracion
**OK.** Campos
roActa (String nullable), idTalonario (Long nullable),
roTalonarioUsado (Integer nullable) presentes. Sin campo
umeroActa (0 usos). Sin MOCK-NRO en codigo funcional (0 usos).

#### Enums
**OK.** Todos los codigos productivos verificados:
- ClaseNumeracion: ACTA=1, DOCUMENTO=2
- TipoTalonario: ELECTRONICO=1, MANUAL_FISICO=2
- AlcanceTalonario: GLOBAL=1, DEPENDENCIA=2, TRANSVERSAL_DOCUMENTO=3
- EstadoAsignacionTalonario: ENTREGADO=1, DEVUELTO=2, CERRADO=3, OBSERVADO=4
- EstadoNumeroTalonario: USADO=1, ANULADO=2, DEVUELTO_SIN_USAR=3, RENDIDO=4, JUSTIFICADO=5
- MotivoAnulacionTalonario: ERROR_LABRADO=1, ROTURA_FORMULARIO=2, DUPLICADO=3, EXTRAVIO=4, OTRO=5
- Todos con metodos codigo() y desdeCodigo(short). Sin agregar ni quitar valores.

#### Services/repositories
**OK.** TalonarioService implementa todos los flujos: politicas, talonarios, ambitos, asignacion/devolucion inspector, emision numero acta, anulacion, justificacion, devolucion sin usar, cierre con control de huecos. TalonarioRepository interface completo. InMemoryTalonarioRepository completo con contadores atomicos y control de secuencia in-memory.

Observacion menor: devolverNumeroSinUsar no valida siActivo del talonario (intencional: permite devolver numeros de talonarios ya inactivos al rendir). nularNumeroTalonario y justificarNumeroTalonario si validan siActivo.

#### Endpoints
**OK.** Todos los endpoints requeridos presentes en TalonarioController e InspectorController:

| Controller | Endpoint | Metodo | Descripcion |
|---|---|---|---|
| TalonarioController | /api/faltas/talonarios/politicas | POST | crear politica |
| TalonarioController | /api/faltas/talonarios/politicas | GET | listar politicas activas |
| TalonarioController | /api/faltas/talonarios/politicas/{id} | GET | obtener politica |
| TalonarioController | /api/faltas/talonarios | POST | crear talonario |
| TalonarioController | /api/faltas/talonarios | GET | listar talonarios activos |
| TalonarioController | /api/faltas/talonarios/{id} | GET | obtener talonario |
| TalonarioController | /api/faltas/talonarios/{id}/ambitos | POST | crear ambito |
| TalonarioController | /api/faltas/talonarios/{id}/ambitos | GET | listar ambitos por talonario |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/asignaciones-inspector | POST | asignar inspector |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/asignaciones-inspector | GET | listar asignaciones por talonario |
| TalonarioController | /api/faltas/talonarios/asignaciones-inspector/activas | GET | listar asignaciones activas |
| TalonarioController | /api/faltas/talonarios/asignaciones-inspector/{id}/devolver | PUT | devolver asignacion |
| TalonarioController | /api/faltas/talonarios/numeracion/actas/emitir | POST | emitir numero acta |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/movimientos | GET | listar movimientos |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/numeros/{nro}/anular | POST | anular numero |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/numeros/{nro}/justificar | POST | justificar numero |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/numeros/{nro}/devolver-sin-usar | POST | devolver sin usar |
| TalonarioController | /api/faltas/talonarios/asignaciones-inspector/{id}/cerrar | PUT | cerrar asignacion |
| TalonarioController | /api/faltas/talonarios/{idTalonario}/numeros/faltantes | GET | listar faltantes |
| InspectorController | /api/faltas/inspectores/{idInsp}/versiones/{ver}/talonarios | GET | talonarios por inspector/version |

Sin endpoints fuera de scope (FalDocumento, JDBC, Angular).

#### Tests
**OK.** 601/601 passing. BUILD SUCCESS.
- TalonarioTest: 107 tests (8B-2/8B-3/8B-4): catalogos, politicas, talonarios, ambitos, asignacion inspector, emision.
- TalonarioManualFisicoTest: 40 tests (8B-6): anulacion, justificacion, devolucion sin usar, cierre, faltantes, guardrails.

#### Spec viva
**OK.** docs/spec-as-source historica = FALSE (eliminada). ackend/api-faltas-core/docs/spec-as-source/ = TRUE (spec viva activa).

#### Fuera de scope
**OK.** Cero referencias funcionales a: FalDocumento, nroDocu, numeroVisible, JDBC, MariaDB, NumSerieTalonario, DocumentoTalonario, TalonarioDocumento.

### Correcciones realizadas

**Ninguna.** La implementacion 8B estaba alineada con la spec. Sin cambios funcionales en este slice.

### Deltas confirmados para slices futuros

- **FalDocumento.nroDocu** (ex
umeroVisible): pendiente de alineacion. Recomendado en 8C.
- **FalDocumento.idTalonario** y **FalDocumento.nroTalonarioUsado**: pendientes. Recomendado en 8C.
- **TipoDocumento vs tipo_docu**: campo 	ipo_docu en ambito usa Short, TipoDocumento enum existe pero no integrado. Pendiente 8B-5/8C.
- **EstadoFirmaDocumento vs estado_firma**: pendiente 8C o slice firma.
- **estado_docu**: sin catalogo productivo numerico explicitado en spec de FalDocumento. Pendiente 8C.
- **NumTalonarioMovimiento.actaId (String) vs acta_id (BIGINT)**: delta in-memory conocido. Se resuelve en Slice 9 (JDBC).
- **FalActa.id (String) vs fal_acta.id (BIGINT)**: delta in-memory conocido. Se resuelve en Slice 9 (JDBC).

### Decisiones

- 8B numeracion de actas y talonarios manuales fisicos: **CERRADA**.
- No iniciar MariaDB/JDBC todavia (Slice 9).
- No tocar Angular todavia.

### Proximo paso recomendado

**8C-0** -- Diagnostico documental completo: relevamiento de FalDocumento, nroDocu, tipo_docu, estado_docu, estado_firma antes de tocar cualquier talonario documental.

**Alternativa:** 8B-5 -- Talonarios documentales, solo si se hace diagnostico previo de FalDocumento y catalogos documentales en un micro-slice diagnostico previo.

---

## Micro-slice 8C-0D - Base documental [COMPLETADO - 2026-07-01]

**Fecha:** 2026-07-01
**Objetivo:** Cerrar catÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡logos y base documental mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­nima necesaria para continuar con 8C sin deuda conceptual.
**Build:** 601/601 tests passing. BUILD SUCCESS.
**Tipo:** Solo documentaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n/modelo. No se implementÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ Java funcional. No se tocÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ Angular. No se iniciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ MariaDB/JDBC.

### Paso 0 - NumPolitica gap resuelto

- Gap detectado en auditorÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a 8B-7: `NumPolitica` tenÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a `fhAlta`/`idUserAlta` en Java pero no en el modelo MariaDB.
- ResoluciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n (OpciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n A): Agregados al modelo productivo como auditorÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a estÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ndar. Sin cambios Java.

### Cambios en modelo MariaDB productivo

| Entidad | Cambio |
|---|---|
| `num_politica` | Agregados `fh_alta`, `id_user_alta` (Paso 0) |
| `fal_acta` | Agregado `resultado_firma_infractor SMALLINT NOT NULL` + catÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡logo |
| `fal_inspector_version` | firma_storage_key, firma_hash, fh_firma_registrada ELIMINADOS del modelo (8F-11B) |
| `fal_acta_evidencia` | CatÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡logo `tipo_evid` actualizado: agrega `FIRMA_OLOGRAFA_INFRACTOR` (ID 6) |
| `fal_documento` | `nro_docu VARCHAR(30)`; `tipo_firma_req` reemplaza `requisito_firma`; `plantilla_id` nuevo; `rol_firma_req`/`mecanismo_firma_req` eliminados a nivel tabla |
| `fal_documento_plantilla` | Nueva tabla (SecciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 5.7) |
| `fal_documento_plantilla_firma_req` | Nueva tabla (SecciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n 5.8) |

### CatÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡logos cerrados

- **`tipo_docu`**: 12 valores (ya existÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a, confirmado).
- **`estado_docu`**: 7 valores ahora explÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­citos: BORRADOR, EMITIDO, PENDIENTE_FIRMA, FIRMADO, ADJUNTO, ANULADO, REEMPLAZADO.
- **`estado_firma`**: 6 valores (ya existÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­a, confirmado).
- **`tipo_firma_req`**: 6 valores (reemplaza `requisito_firma`): NO_REQUIERE(0), FIRMA_INTERNA(1), FIRMA_INSPECTOR(2), FIRMA_AUTORIDAD(3), FIRMA_DIGITAL(4), FIRMA_MULTIPLE(5). `FIRMA_MIXTA` prohibido.
- **`accion_documental`**: 11 valores nuevos.
- **`momento_numeracion_docu`**: 5 valores nuevos (solo documentos, NO actas).
- **`resultado_firma_infractor`**: 5 valores nuevos.

### Reglas documentadas

- SeparaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n explÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­cita: numeraciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n de actas != numeraciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n documental.
- Actas: numeraciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n propia (`nro_acta`), no usan `momento_numeracion_docu`.
- Documentos: numeraciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n por `num_talonario_ambito`, plantilla define momento.
- Firma automÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡tica del inspector en actas (no captura manual por acta).
- Firma ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³lÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³grafa del infractor: opcional, evidencia, no institucional.
- `resultado_firma_infractor` obligatorio en acta formal.

### Archivos actualizados

- docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md
- docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md

### Deuda tÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©cnica para Slice 9/JDBC

- `FalDocumento` Java: renombrar `requisitoFirma` -> `tipoFirmaReq`; remover `rolFirmaReq`/`mecanismoFirmaReq`; agregar `plantillaId`.
- Implementar `FalDocumentoPlantilla` y `FalDocumentoPlantillaFirmaReq` in-memory (8C).
- DDL y seeds de todos los nuevos catÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡logos.

### PrÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ximos pasos

- **8C-0A**: Alinear `FalDocumento` contra modelo MariaDB (`nroDocu`, `tipoFirmaReq`, `plantillaId`).
- ~~**8C-0B**: Alinear `FalActa.id` y `NumTalonarioMovimiento.actaId` a Long.~~ [COMPLETADO 2026-07-01]
- **8C**: Implementar plantillas/documentos/firma documental.
- **Slice posterior**: Implementar `resultado_firma_infractor` y evidencia de firma ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³lÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³grafa.
- **Slice posterior**: Ajustar firma registrada del inspector en `FalInspectorVersion`.

---

## Micro-slice 8C-0B - Alineacion IDs de acta a Long [COMPLETADO]

**Fecha:** 2026-07-01
**Build:** 601/601 tests passing. BUILD SUCCESS.

### Cambios realizados

- FalActa.id: String -> Long
- NumTalonarioMovimiento.actaId: String -> Long
- Todos los modelos de dominio con ctaId/idActa: alineados a Long
- InMemoryActaRepository: AtomicLong para generacion de IDs (
extId())
- Todos los @PathVariable String idActa en controllers: @PathVariable Long
- Commands, DTOs, repositories, services, tests: alineados
- Identificadores tipo ACTA-0001 eliminados como PK tecnica
-
roActa sigue siendo String visible

### Guardrails confirmados

- No se implemento FalDocumento ni fal_acta_documento
- No se implementaron plantillas ni firma documental
- No se toco Angular
- No se inicio MariaDB/JDBC
- No se crearon endpoints nuevos
- No se reabrio 8C-0D

### Proximos inmediatos

- **8C-0A**: Alinear FalDocumento contra modelo MariaDB (
roDocu, 	ipoFirmaReq, plantillaId)
- **8C-1**: Implementar catalogos documentales Java/enums
- **8C-2**: Implementar plantillas documentales in-memory

---

## Micro-slice 8C-0A - Alineacion FalDocumento contra modelo MariaDB [COMPLETADO 2026-07-01]

### Resumen de cambios

- FalDocumento.id: String -> Long (BIGINT AUTO_INCREMENT).
- FalDocumento.tipoDocumento -> tipoDocu (renombrado; tipo TipoDocumento provisional hasta 8C-1).
- FalDocumento.estado -> estadoDocu (renombrado; tipo EstadoDocumento provisional hasta 8C-1).
- FalDocumento.numeroVisible -> nroDocu (renombrado; VARCHAR(30) nullable).
- FalDocumento.tipoFirmaReq agregado (short, SMALLINT, 0=NO_REQUIERE por defecto).
- FalDocumento.plantillaId agregado (Long nullable, BIGINT FK plantilla documental).
- FalDocumento.idTalonario agregado (Long nullable, BIGINT FK talonario).
- FalDocumento.nroTalonarioUsado agregado (Integer nullable, INT).
- rolFirmaReq y mecanismoFirmaReq confirmados fuera de FalDocumento (pertenecen a fal_documento_firma_req).
- Cascade: FalActaFallo.documentoId, FalDocumentoFirma.idDocumento, FalNotificacion.idDocumento -> Long.
- Repositories: DocumentoRepository.buscarPorId/nextId, DocumentoFirmaRepository.buscarPorDocumento -> Long.
- Commands/DTOs: FirmarDocumentoCommand, EnviarNotificacionCommand, EnviarNotificacionRequest -> Long idDocumento.
- Controller: DocumentoController @PathVariable idDocumento -> Long.
- Services: getEstadoDocu(), getTipoDocu(), nextId(), String.valueOf() para eventos.
- Tests: 7 archivos actualizados. Build: 601/601 tests passing.

### Guardrails preservados

- No FalDocumentoPlantilla, FalDocumentoPlantillaFirmaReq, FalDocumentoFirmaReq.
- No generacion, firma, numeracion documental.
- No endpoint nuevo de documentos.
- No MariaDB/JDBC, no Angular.
- No TipoDocu.java, EstadoDocu.java, TipoFirmaReq.java (pendiente 8C-1).

### Proximos pendientes recomendados

1. ~~**8C-1**: Implementar enums documentales Java (TipoDocu, EstadoDocu, TipoFirmaReq, AccionDocumental, MomentoNumeracionDocu, ResultadoFirmaInfractor). Reemplazar TipoDocumento y EstadoDocumento en FalDocumento.~~ [COMPLETADO 2026-07-01]
2. **8C-2**: Implementar FalDocumentoPlantilla y FalDocumentoPlantillaFirmaReq in-memory.
3. **8C-3**: Implementar generacion de documento desde plantilla, snapshot en FalDocumento, FalDocumentoFirmaReq.

## Micro-slice 8C-1 - Catalogos documentales Java/enums [COMPLETADO 2026-07-01]

### Que se hizo

- Creados 7 enums documentales definitivos con codigos SMALLINT:
  - TipoDocu (12 valores, codigo 1-12): reemplaza TipoDocumento provisional.
  - EstadoDocu (7 valores, codigo 1-7): reemplaza EstadoDocumento provisional.
  - EstadoFirma (6 valores, codigo 1-6): reemplaza EstadoFirmaDocumento provisional.
  - TipoFirmaReq (6 valores, codigo 0-5): FIRMA_MULTIPLE implementado. FIRMA_MIXTA prohibido.
  - AccionDocumental (11 valores, codigo 1-11): para plantillas futuras.
  - MomentoNumeracionDocu (5 valores, codigo 0-4): solo documentos, no actas.
  - ResultadoFirmaInfractor (5 valores, codigo 1-5): enum creado; campo en FalActa pendiente.
- FalDocumento actualizado: TipoDocu, EstadoDocu, TipoFirmaReq (enum, default NO_REQUIERE).
- FalDocumentoFirma actualizado: EstadoFirma en vez de EstadoFirmaDocumento.
- FalNotificacion actualizado: TipoDocu en vez de TipoDocumento.
- GenerarDocumentoCommand / GenerarDocumentoRequest actualizados: TipoDocu.
- FalloActaService: fallos crean documentos tipo ACTO_ADMINISTRATIVO (no FALLO_ABSOLUTORIO/CONDENATORIO).
- DocumentoService: deteccion de fallo por documentoId, no por TipoDocu.
- NotificacionService: distincion absolutorio/condenatorio via FalActaFallo.tipoFallo + documentoId.
- SnapshotRecalculador: usa EstadoDocu.
- TipoDocumento, EstadoDocumento, EstadoFirmaDocumento provisionales eliminados.
- 19 nuevos tests de guardrail agregados a EnumGuardrailTest.

### Build

- 620/620 tests passing. BUILD SUCCESS.

### Guardrails preservados

- FalDocumentoPlantilla: no implementado.
- FalDocumentoPlantillaFirmaReq: no implementado.
- FalDocumentoFirmaReq: no implementado.
- ResultadoFirmaInfractor en FalActa: implementado en 8C-6A (2026-07-01).
- Evidencia FIRMA_OLOGRAFA_INFRACTOR: implementada en 8C-6A (2026-07-01).
- Firma registrada del inspector: no implementada.
- Numeracion documental: no implementada.
- MariaDB/JDBC: no usado.
- Angular: no tocado.


## Micro-slice 8C-2 - Plantillas documentales in-memory [COMPLETADO 2026-07-01]

### Que se hizo

- FalDocumentoPlantilla creado. Sin idTalonario, politicaNumeracionId ni claseTalonario.
- FalDocumentoPlantillaFirmaReq creado. No es FalDocumentoFirmaReq.
- DocumentoPlantillaRepository interfaz + InMemoryDocumentoPlantillaRepository in-memory.
- DocumentoPlantillaService con validaciones: codigo unico, coherencia numeracion, reglas de activacion por TipoFirmaReq.
- DocumentoPlantillaController REST con 8 endpoints bajo /api/faltas/documentos/plantillas.
- Commands: CrearDocumentoPlantillaCommand, AgregarFirmaReqPlantillaCommand, ActivarDocumentoPlantillaCommand, DesactivarDocumentoPlantillaCommand.
- DTOs: CrearDocumentoPlantillaRequest, AgregarFirmaReqPlantillaRequest, DocumentoPlantillaResponse, DocumentoPlantillaFirmaReqResponse.
- Tests: 37 nuevos en DocumentoPlantillaTest.

### Build

- 657/657 tests. BUILD SUCCESS.

### Guardrails preservados

- FalDocumentoPlantilla sin idTalonario, politicaNumeracionId, claseTalonario.
- FalDocumentoFirmaReq: no implementado.
- Generacion documental: no implementada.
- Numeracion documental: no implementada.
- MomentoNumeracionDocu: solo en plantillas, no en FalActa.
- FIRMA_MIXTA: no existe.
- Sin MariaDB/JDBC. Sin Angular.

### Proximos pendientes recomendados

1. **8C-3**: Generar FalDocumento desde plantilla activa (seleccionar plantilla, crear snapshot, sin numeracion todavia).
2. **8C-4**: Implementar FalDocumentoFirmaReq (snapshot desde FalDocumentoPlantillaFirmaReq; base para bandeja de firmas).
3. **Slice posterior**: ResultadoFirmaInfractor en FalActa, evidencia FIRMA_OLOGRAFA_INFRACTOR, firma registrada del inspector en FalInspectorVersion.

## Micro-slice 8C-3 - Generacion de FalDocumento snapshot desde plantilla [COMPLETADO 2026-07-01]

### Que se hizo

- FalDocumento: segundo constructor para crear desde plantilla (estadoDocu, tipoFirmaReq, plantillaId explicitos).
- GenerarDocumentoDesdePlantillaCommand: record (idActa, plantillaId, idUserAlta).
- GenerarDocumentoDesdePlantillaRequest: DTO web.
- DocumentoResponse: DTO de respuesta con codigos SMALLINT para tipoDocu, estadoDocu, tipoFirmaReq.
- DocumentoService.generarDesdePlantilla(): validaciones completas + creacion BORRADOR.
- DocumentoController: endpoint POST /api/faltas/documentos/desde-plantilla.
- 7 tests existentes actualizados (constructor DocumentoService).
- 28 nuevos tests en DocumentoGeneracionDesdePlantillaTest.

### Reglas implementadas

- Documento nace EstadoDocu.BORRADOR.
- Copia: tipoDocu, tipoFirmaReq, plantillaId, idActa desde plantilla.
- nroDocu, idTalonario, nroTalonarioUsado quedan null.
- Plantilla AL_CREAR rechazada (numeracion documental fuera de alcance).
- Plantilla no activa o vencida rechazada.
- idActa, plantillaId, idUserAlta obligatorios.

### Build

- 685/685 tests (28 nuevos). BUILD SUCCESS.

### Guardrails preservados

- FalDocumentoFirmaReq: no implementado.
- Materializacion de requisitos de firma: no implementada.
- Numeracion documental: no implementada.
- Consumo de talonario documental: no implementado.
- PDF/storage: no generado.
- Sin MariaDB/JDBC. Sin Angular.

### Proximos pendientes recomendados

1. **8C-4**: Implementar FalDocumentoFirmaReq (snapshot desde FalDocumentoPlantillaFirmaReq; estado de requisito; preparar bandeja de firmas; sin firmar todavia).
2. **8C-5**: Enviar documento a firma (BORRADOR -> PENDIENTE_FIRMA; materializar firma_req; si momentoNumeracionDocu=AL_ENVIAR_A_FIRMA, evaluar numeracion documental en slice separado o previo).
3. **Slice posterior**: Numeracion documental (resolver num_talonario_ambito con clase DOCUMENTO; generar nroDocu; registrar movimiento).

## Micro-slice 8C-4 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¿ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â½ FalDocumentoFirmaReq snapshot de requisitos de firma [COMPLETADO 2026-07-01]

- EstadoFirmaReq (enum propio): PENDIENTE/FIRMADO/ANULADO/VENCIDO/REEMPLAZADO.
- FalDocumentoFirmaReq: snapshot de requisito de firma por documento concreto.
- DocumentoFirmaReqRepository + InMemoryDocumentoFirmaReqRepository.
- DocumentoFirmaReqService: materializarDesdePlantilla, listarPorDocumento, obtener.
- DocumentoFirmaReqController: POST materializar, GET listar, GET obtener.
- 30 tests en DocumentoFirmaReqTest. Total: 715/715. BUILD SUCCESS.

## Proximos pendientes post-8C-4

### 8C-5 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¿ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â½ Enviar documento a firma [PENDIENTE]

- Validar que documento esta BORRADOR.
- Materializar firma_req si no existe.
- Cambiar EstadoDocu a PENDIENTE_FIRMA.
- Si momentoNumeracionDocu = AL_ENVIAR_A_FIRMA, evaluar si se hace en este slice o en slice de numeracion documental previo.

### Slice de numeracion documental [PENDIENTE]

- Resolver num_talonario_ambito con clase DOCUMENTO.
- Generar nroDocu.
- Registrar movimiento en NumTalonarioMovimiento.

### Slice de firma real [PENDIENTE]

- Registrar FalDocumentoFirma.
- Validar firmante habilitado (FalFirmanteVersionHabilitacion).
- Cumplir requisito: actualizar FalDocumentoFirmaReq (estadoFirmaReq = FIRMADO, fhFirma, idFirma).
- Evaluar si todos los obligatorios estan FIRMADO.
- Cerrar documento como FIRMADO si corresponde.

## Micro-slice 8C-5A ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Numeracion documental reusable [COMPLETADO 2026-07-01]

- NumerarDocumentoCommand + EmitirNumeroDocumentoCommand + NumeroDocumentoEmitidoResponse.
- NumerarDocumentoRequest (HTTP DTO).
- DependenciaRepository.findByCodDep(String codDep) para resolver idDep desde FalActa.idDependencia.
- TalonarioService.emitirNumeroDocumento: resuelve ambito DOCUMENTO, emite correlativo, registra NumTalonarioMovimiento con documentoId.
- DocumentoService.numerarDocumento: valida doc/plantilla/acta/dep, llama TalonarioService, guarda nroDocu/idTalonario/nroTalonarioUsado.
- generarDesdePlantilla: auto-numera si momentoNumeracionDocu = AL_CREAR.
- DocumentoController: POST /api/faltas/documentos/{documentoId}/numerar.
- 31 tests en DocumentoNumeracionTest. Total: 746/746. BUILD SUCCESS.

### Decision funcional corregida (momento_numeracion_docu)

- AL_CREAR: numerar al crear (implementado).
- AL_ENVIAR_A_FIRMA: numerar ANTES de BORRADOR -> PENDIENTE_FIRMA. [IMPLEMENTADO en 8C-5B]
- AL_FIRMAR: numerar antes de hash/firma digital (pendiente en slice de firma real).
- AL_EMITIR: numerar al emitir formalmente (pendiente).
- NO_APLICA: documento no numerado; numerar falla con PrecondicionVioladaException.



## Micro-slice 8C-5B - Enviar documento a firma [COMPLETADO 2026-07-01]

- 20 tests en DocumentoEnvioFirmaTest. Total: 766/766. BUILD SUCCESS.

### Implementado

- Transicion BORRADOR -> PENDIENTE_FIRMA.
- AL_ENVIAR_A_FIRMA: numera automaticamente con servicio de 8C-5A antes de cambiar estado.
- AL_FIRMAR, AL_EMITIR, NO_APLICA: envia sin numerar.
- AL_CREAR + nroDocu == null: inconsistencia, falla.
- Materializacion automatica de firma_req desde plantilla si no existia.
- Endpoint: POST /api/faltas/documentos/{id}/enviar-a-firma.

## Proximos pendientes post-8C-5B

### Slice de firma real [PENDIENTE]

- Registrar FalDocumentoFirma.
- Validar firmante habilitado (FalFirmanteVersionHabilitacion activa y compatible con tipo_docu + rol_firma_req + mecanismo).
- Si momentoNumeracionDocu = AL_FIRMAR:
  - Si firma digital: numerar antes de render/hash/firma (el numero debe formar parte del contenido firmado).
  - Si firma olografa/no digital: numerar en el trigger/boton de firma; el numero puede pre-existir al contenido fisico.
  - Si documento escaneado: convalidar firma existente en flujo separado (ver slice convalidacion).
- Marcar FalDocumentoFirmaReq como FIRMADO (estadoFirmaReq = FIRMADO, fhFirma, idFirma).
- Cerrar documento como FIRMADO cuando todos los obligatorios esten firmados.
- Regla: orden_firma debe respetarse si aplica (requisitos de menor orden deben estar FIRMADO antes de exponer los siguientes en bandeja).

### Slice de convalidacion de documento escaneado [PENDIENTE]

- Incorporar escaneo de documento fisico (storage externo, referencia en fal_documento.storage_key).
- Convalidar firma olografa existente sobre el documento escaneado.
- Registrar convalidador y referencia en fal_documento_firma con tipo_firma = OLOGRAFA y referencia_firma_ext si corresponde.
- Cumplir el requisito fal_documento_firma_req correspondiente si el rol/mecanismo son compatibles.
- Nota: el documento escaneado no se firma digitalmente en sistema; se incorpora evidencia y se convalida la firma preexistente.

### Slice 8C-6A: ResultadoFirmaInfractor + evidencia firma infractor [COMPLETADO 2026-07-01]

- Agregar campo ResultadoFirmaInfractor en FalActa Java (campo resultado_firma_infractor ya existe en modelo MariaDB).
- Enum ResultadoFirmaInfractor ya creado en 8C-1 con codigos productivos (FIRMADA=1 .. NO_CAPTURADA_POR_FALLA_TECNICA=5).
- Firma olografa del infractor es evidencia del acta, no firma institucional documental.
- No participa de tipo_firma_req, fal_documento_firma_req ni fal_documento_firma.
- Si firma, captura grafica se guarda como evidencia tipo FIRMA_OLOGRAFA_INFRACTOR en fal_acta_evidencia.
- Sin campo obs_firma_infractor en el modelo (texto libre prohibido; si se necesita observacion usar fal_observacion).

## Micro-slice 8C-6A - ResultadoFirmaInfractor y evidencia firma infractor [COMPLETADO 2026-07-01]

### Implementado

- Campo `resultadoFirmaInfractor` agregado a `FalActa` (tipo `ResultadoFirmaInfractor`, nullable).
- Enum `ResultadoFirmaInfractor` (creado en 8C-1): FIRMADA/NEGADA/NO_CAPTURADA_SIN_JUSTIFICACION/NO_CAPTURADA_JUSTIFICADA/NO_CAPTURADA_POR_FALLA_TECNICA (codigos 1-5).
- `TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR` incorporado.
- `FalActaEvidencia` guarda captura grafica de firma del infractor como evidencia del acta.
- La firma olografa del infractor NO participa de `tipo_firma_req`, `fal_documento_firma_req` ni `fal_documento_firma`.
- `resultado_firma_infractor` ya existia en modelo MariaDB final; campo Java alineado ahora.
- Sin campo `obs_firma_infractor`; observaciones van en `fal_observacion`.
- 18 tests en `ActaResultadoFirmaInfractorTest`. Total: 784/784. BUILD SUCCESS.

### Build

- 784/784 tests (18 nuevos). BUILD SUCCESS.

---

## Micro-slice 8C-6B-0 - Diagnostico y contrato de firma real documental [COMPLETADO 2026-07-01]

Solo diagnostico y documentacion. Sin cambios funcionales Java.

### Diagnostico: FalDocumentoFirma

**Archivo:** `domain/model/FalDocumentoFirma.java`
**Tipo actual:** `record` inmutable.

**Campos actuales:**
- `String id` (UUID) - en memoria OK; modelo usa BIGINT AUTO_INCREMENT
- `Long idDocumento` - correcto
- `Long idActa` - **EXTRA: no esta en modelo MariaDB**; derivable via documento
- `String firmante` - nombre generico; modelo tiene campos separados
- `String tipoFirma` - String libre; modelo usa SMALLINT + enum TipoFirma
- `EstadoFirma estadoFirma` - correcto
- `LocalDateTime fechaFirma` - correcto (fh_firma)
- `String observaciones` - modelo tiene `mensaje_error` + `referencia_firma_ext`

**Campos faltantes respecto al modelo MariaDB final:**
- `seqFirmaReq` (Short nullable) - requisito satisfecho
- `idFirmante` (Long nullable) - FK firmante
- `verFirmante` (Short nullable) - version firmante
- `idUserFirma` (String nullable) - subject IDP
- `rolFirmante` (String nullable) - snapshot claim bearer
- `nombreFirmante` (String nullable) - snapshot legible
- `hashDocumento` (String nullable) - SHA-256 del contenido firmado
- `referenciaFirmaExt` (String nullable) - referencia externa
- `mensajeError` (String nullable) - error si estado = ERROR
- `fhAlta` (LocalDateTime) - auditoria
- `idUserAlta` (String) - auditoria

**Alineacion con modelo:** PARCIAL. La implementacion actual es la version naive de flujo completo (slice 8C-2/3). 8C-6B-1 debe refactorizarla.

### Diagnostico: FalDocumentoFirmaReq

**Estado:** Bien alineado con el modelo.

**Campos relevantes ya implementados:**
- `id`, `documentoId`, `seqFirmaReq` (short), `rolFirmaReq` (short), `mecanismoFirmaReq` (Short nullable)
- `ordenFirma` (Short nullable) - YA IMPLEMENTADO, pero siempre nulo al materializar (ver gap abajo)
- `estadoFirmaReq` (EstadoFirmaReq), `siObligatoria`, `siActiva`
- `idFirmanteAsig`, `verFirmanteAsig`, `fhAsignacion`
- `fhFirma`, `idFirma` (Long nullable) - se completa al firmar
- `fhAlta`, `idUserAlta`

**Gap detectado:** `ordenFirma` se pasa siempre como `null` al materializar desde plantilla (en `DocumentoService.materializarFirmaReqDesdeInternal` y `DocumentoFirmaReqService.materializarDesdePlantilla`). `FalDocumentoPlantillaFirmaReq` tampoco tiene `ordenFirma`. Se asume que `seqFirmaReq` sirve como proxy de orden. Documentado como decision activa.

**Relacion con firma real:** `idFirma` (Long) pero `FalDocumentoFirma.id` es `String` (UUID). Al refactorizar `FalDocumentoFirma`, `idFirma` debe alinearse al tipo del nuevo `id`.

### Diagnostico: DocumentoFirmaRepository

- `buscarPorDocumento(Long)` retorna `Optional<FalDocumentoFirma>` - solo una firma por documento.
- El modelo permite multiples firmas por documento (varios requisitos = varias firmas).
- Para 8C-6B-1: cambiar a `List<FalDocumentoFirma>` y agregar `buscarPorDocumentoYSeq(Long, short)`.

### Diagnostico: firmarDocumento (actual)

`DocumentoService.firmarDocumento(FirmarDocumentoCommand)` es la implementacion naive:
- Acepta `firmante` (String), `tipoFirma` (String), `observaciones` (String) - sin seqFirmaReq.
- Cambia `estadoDocu = FIRMADO` directamente, sin verificar requisitos obligatorios.
- Crea `FalDocumentoFirma` con campos minimos; no actualiza `FalDocumentoFirmaReq`.
- No valida firmante habilitado.
- No respeta `ordenFirma`.
- No maneja `momentoNumeracionDocu = AL_FIRMAR`.
- Usada por tests de flujo completo para avanzar estado. Debe reemplazarse en 8C-6B-1.

### Diagnostico: firmante habilitado

**Entidades existentes:** `FalFirmante`, `FalFirmanteVersion`, `FalFirmanteVersionHabilitacion`.

**Validaciones disponibles en FirmanteRepository:**
- `findById(idFirmante)` - firmante existe.
- `findVersionVigente(idFirmante, fecha)` - version vigente en fecha dada.
- `findVersionByFirmanteAndVer(idFirmante, verFirmante)` - version especifica.
- `findHabilitacionActiva(idFirmante, verFirmante, tipoDocu, rolFirmaReq)` - habilitacion activa por tipoDocu+rol.
- `existeHabilitacionActiva(idFirmante, verFirmante, tipoDocu, rolFirmaReq)` - boolean.

**Gap:** `findHabilitacionActiva` no filtra por `mecanismoFirmaReq`. Si el requisito tiene mecanismo especifico, la validacion debe hacerse adicionalmente sobre el campo del objeto retornado.

**Metodo propuesto para 8C-6B-1:** Agregar logica en el servicio de firma:
```
FalFirmanteVersionHabilitacion hab = firmanteRepository
    .findHabilitacionActiva(idFirmante, verFirmante, tipoDocu, rolFirmaReq)
    .orElseThrow(...)
if (mecanismoFirmaReq != null && hab.getMecanismoFirmaReq() != null
        && !hab.getMecanismoFirmaReq().equals(mecanismoFirmaReq)) {
    throw PrecondicionVioladaException(...)
}
```

### Diagnostico: orden de firma

- Modelo MariaDB: `orden_firma` en `fal_documento_firma_req` (nullable).
- `FalDocumentoFirmaReq` Java: `ordenFirma` (Short nullable) - IMPLEMENTADO.
- `FalDocumentoPlantillaFirmaReq` Java: NO tiene `ordenFirma`. Solo `seqFirmaReq`.
- `fal_documento_plantilla_firma_req` MariaDB: NO tiene `orden_firma`.

**Decision activa:** El orden de firma se define en `fal_documento_firma_req` y puede ser distinto del `seqFirmaReq` de la plantilla. Actualmente siempre se materializa como `null`. Si un requisito tiene `ordenFirma = null`, se permite firma sin restriccion secuencial. El modelo ya lo soporta.

**Regla para 8C-6B-1:** Antes de registrar firma de un requisito con `ordenFirma != null`, verificar que todos los requisitos obligatorios activos con `ordenFirma < ordenFirma_del_actual` tengan `estadoFirmaReq = FIRMADO`. Si tienen mismo numero de orden, se permite cualquier secuencia dentro del grupo.

### Diagnostico: AL_FIRMAR y numerarDocumento

**numerarDocumento (DocumentoService) NO restringe por estadoDocu.** Validaciones actuales:
1. `documentoId` no nulo.
2. `nroDocu == null` (no esta numerado).
3. Documento tiene plantilla.
4. `siRequiereNumeracion = true`.
5. `momentoNumeracionDocu != NO_APLICA`.
6. Acta y dependencia resolvibles.

**Conclusion:** `numerarDocumento` puede llamarse con documento en `PENDIENTE_FIRMA`. No hay ajuste necesario en el metodo para el caso AL_FIRMAR. La logica de cuando llamarlo (antes del hash, antes de registrar la firma) vive en el servicio de firma real de 8C-6B-1.

**Regla funcional para 8C-6B-1:**
- Si `momentoNumeracionDocu = AL_FIRMAR` y `doc.getNroDocu() == null`:
  - Si firma digital: llamar `numerarDocumento` ANTES de calcular hash (el numero debe estar en el contenido firmado).
  - Si firma olografa: numerar en el trigger de firma (el numero se asigna antes de imprimir/presentar al firmante).
- Si `doc.getNroDocu() != null`: el documento ya esta numerado; no renumerar.

### Contrato tecnico para 8C-6B-1

#### Refactorizacion de FalDocumentoFirma

Nuevo record `FalDocumentoFirma` (reemplaza el actual):
```java
record FalDocumentoFirma(
    String id,               // UUID; productivo usara BIGINT
    Long idDocumento,
    Short seqFirmaReq,       // nullable: req satisfecho
    Long idFirmante,         // nullable: FK fal_firmante
    Short verFirmante,       // nullable: version usada
    String idUserFirma,      // nullable: subject IDP
    String rolFirmante,      // nullable: snapshot claim
    String nombreFirmante,   // nullable: snapshot legible
    Short tipoFirma,         // NOT NULL: mecanismo (enum TipoFirma short)
    EstadoFirma estadoFirma, // NOT NULL
    LocalDateTime fechaFirma,
    String hashDocumento,    // nullable: SHA-256
    String referenciaFirmaExt, // nullable
    String mensajeError,     // nullable
    LocalDateTime fhAlta,
    String idUserAlta
)
```

#### Nuevo FirmarDocumentoCommand

Nuevo comando para firma real:
```java
record RegistrarFirmaDocumentalCommand(
    Long idDocumento,       // NOT NULL
    Short seqFirmaReq,      // NOT NULL: cual requisito se cumple
    Long idFirmante,        // NOT NULL: quien firma
    int verFirmante,        // NOT NULL: version vigente
    String idUserFirma,     // NOT NULL
    String rolFirmante,     // nullable
    String nombreFirmante,  // nullable
    Short tipoFirma,        // NOT NULL
    String hashDocumento,   // nullable; obligatorio si digital
    String referenciaFirmaExt, // nullable
    String idUserOperacion  // NOT NULL: auditoria
)
```

El comando legacy `FirmarDocumentoCommand` puede coexistir para los tests de flujo completo o ser
refactorizado al mismo tiempo. Decision de 8C-6B-1.

#### Validaciones requeridas en 8C-6B-1

1. Documento existe.
2. Documento esta en `PENDIENTE_FIRMA`.
3. Requisito (`seqFirmaReq`) existe para ese documento.
4. Requisito pertenece al documento.
5. Requisito esta activo (`siActiva = true`).
6. Requisito esta `PENDIENTE` (`estadoFirmaReq = PENDIENTE`).
7. Firmante existe (`idFirmante`).
8. Version del firmante vigente (`findVersionVigente`).
9. Firmante habilitado: `findHabilitacionActiva(idFirmante, verFirmante, tipoDocu, rolFirmaReq)` existe y, si `mecanismoFirmaReq != null`, mecanismo compatible.
10. Orden de firma respetado: si `ordenFirma != null`, todos los requisitos obligatorios activos con `ordenFirma < actual.ordenFirma` deben estar `FIRMADO`.
11. Si `momentoNumeracionDocu = AL_FIRMAR` y `doc.getNroDocu() == null`: llamar `numerarDocumento` antes de registrar firma.
12. Si firma digital: `hashDocumento` obligatorio (calculado sobre contenido ya numerado).
13. Registrar `FalDocumentoFirma` (nuevo registro, no actualizar).
14. Actualizar `FalDocumentoFirmaReq`: `estadoFirmaReq = FIRMADO`, `fhFirma`, `idFirma`.
15. Evaluar si todos los requisitos obligatorios activos estan `FIRMADO`; si es asi, `doc.estadoDocu = FIRMADO`.
16. No tocar `resultadoFirmaInfractor` ni `FalActaEvidencia`.

#### Riesgos identificados

1. **`idFirma` en FalDocumentoFirmaReq es Long**, pero `FalDocumentoFirma.id` es `String` UUID. Al refactorizar, resolver tipo de `id`. Opciones: cambiar `FalDocumentoFirma.id` a Long con generador in-memory, o cambiar `FalDocumentoFirmaReq.idFirma` a String. Decision pendiente para 8C-6B-1.
2. **`DocumentoFirmaRepository.buscarPorDocumento` retorna Optional** (supone 1 firma por documento). El modelo admite multiples. Cambiar a `List` en 8C-6B-1.
3. **Tests de flujo completo usan `firmarDocumento` naive.** Si se refactoriza el metodo publico, los tests existentes fallan. Estrategia: implementar `registrarFirmaDocumental` nuevo, dejar `firmarDocumento` como helper interno de tests solo, o adaptar tests. Decision de 8C-6B-1.
4. **`ordenFirma` siempre nulo al materializar.** Para probar ordenamiento, el test debera setear `ordenFirma` manualmente en el requisito materializado o extender `FalDocumentoPlantillaFirmaReq` con `ordenFirma`. Decision de 8C-6B-1.
5. **Enum `TipoFirma` para `tipoFirma`** en `FalDocumentoFirma** actualmente es String. El modelo usa Short. Crear enum `TipoFirma` (ya en catalogo: DIGITAL=1, ELECTRONICA=2, OLOGRAFA=3, SISTEMA=4) en 8C-6B-1 o antes.

## Proximos pendientes post-8C-6B-0

### 8C-6B-1 - Registrar firma real documental [PENDIENTE]

- Refactorizar `FalDocumentoFirma` con campos completos del modelo MariaDB.
- Crear enum `TipoFirma` (DIGITAL/ELECTRONICA/OLOGRAFA/SISTEMA con codigos Short).
- Nuevo comando `RegistrarFirmaDocumentalCommand` (o refactor de `FirmarDocumentoCommand`).
- Nuevo metodo `DocumentoService.registrarFirmaDocumental(...)` con las 16 validaciones del contrato.
- Actualizar `DocumentoFirmaRepository`: `buscarPorDocumento` -> `List`, agregar `buscarPorDocumentoYSeq`.
- Actualizar tests de flujo completo que usan firma naive (adaptar o mantener helper separado).
- Endpoint: POST /api/faltas/documentos/{id}/firmar.
- No tocar firma del infractor, DELTA MariaDB, Angular.



---

## 8C-6B-1 - Registrar firma real documental [CERRADO 2026-07-01]

### Implementado

- Enum TipoFirma creado: DIGITAL=1, ELECTRONICA=2, OLOGRAFA=3, SISTEMA=4
- FalDocumentoFirma refactorizado (record naive -> clase con campos completos del modelo MariaDB)
  - id: Long (antes: String UUID), idActa: eliminado, campos completos alineados
  - Validaciones de entidad en constructor
  - Si tipoFirma=DIGITAL: hashDocumento y referenciaFirmaExt obligatorios
- FalDocumentoFirmaReq.marcarFirmado(...) implementado (PENDIENTE -> FIRMADO)
- DocumentoFirmaRepository actualizado:
  - guardar, uscarPorId(Long), uscarPorDocumento(Long), uscarPorDocumentoYSeq(Long, short),
extId()
- RegistrarFirmaDocumentalCommand, RegistrarFirmaDocumentalRequest, DocumentoFirmaResponse creados
- DocumentoService.registrarFirmaDocumental(...) implementado con 21 validaciones (documentoId, seq, firmante, habilitacion, mecanismo, orden, AL_FIRMAR, DIGITAL)
- FalDocumento.marcarFirmado(): transicion PENDIENTE_FIRMA -> FIRMADO cuando todos los obligatorios activos estan firmados
- Endpoint: POST /api/faltas/documentos/{documentoId}/firmar-real
- DocumentoFirmaRealTest.java: 41 casos de test cubriendo firma valida, cierre, validaciones, firmante, habilitacion, mecanismo, orden, AL_FIRMAR, firma digital, guardrails
- Build al cierre: 816/816 tests pasando

### Decisiones tomadas

- irmarDocumento legacy mantenido con implementacion adaptada para tests de flujo completo
- ordenFirma sigue siendo null al materializar desde plantilla (AgregarFirmaReqPlantillaCommand no tiene ese campo)
  Tests de orden setean FalDocumentoFirmaReq directamente en el repo
- hashDocumento aceptado como input simulado (no calculado sobre PDF real)

### No implementado en este slice

- PDF real / render / storage real
- Proveedor de firma digital
- Convalidacion de escaneados
- MariaDB/JDBC
- Angular

## Proximos pendientes post-8C-6B-1

### 8C-6C - Emision formal / PDF / storage [PENDIENTE]

- Generacion real de PDF renderizado
- Storage de documentos firmados
- Proveedor de firma digital (PKCS#7 / CAdES / XAdES)

### 8C-6D - Convalidacion de documento escaneado [PENDIENTE]

- Registrar evidencia de firma olografa documental (distinto de FIRMA_OLOGRAFA_INFRACTOR)
- Upload de imagen/PDF escaneado

### 8C-6E - Auditoria pre-JDBC/MariaDB [PENDIENTE]

- Reconciliacion final del modelo in-memory con el DDL MariaDB productivo
- Revisar DELTA antes de empezar migracion JDBC

---

## 8C-6C-0 -- Diagnostico y contrato de emision formal / PDF / storage [CERRADO 2026-07-01]

### Tipo de slice

Solo diagnostico y documentacion. Sin cambios funcionales Java. Sin MariaDB/JDBC. Sin Angular.
Build base confirmado: 816/816 tests passing. BUILD SUCCESS.

---

### Verificacion: docs/spec-as-source historica

Confirmado: docs/spec-as-source no existe (Test-Path = False). Spec viva en ackend/api-faltas-core/docs/spec-as-source/.

---

### Diagnostico FalDocumento

Campos actuales en FalDocumento.java:

| Campo Java           | Tipo           | Final | Mapea a MariaDB          | Estado           |
|----------------------|----------------|-------|--------------------------|------------------|
| id                 | Long           | SI    | id BIGINT AUTO_INCREMENT | OK               |
| idActa             | Long           | SI    | acta_id BIGINT FK        | OK               |
| 	ipoDocu           | TipoDocu       | SI    | tipo_docu SMALLINT       | OK               |
| echaGeneracion    | LocalDateTime  | SI    | fh_alta DATETIME(6)      | OK (es fh_alta)  |
| storageKey         | String         | NO    | storage_key VARCHAR(255) | Presente; getter/setter |
| estadoDocu         | EstadoDocu     | NO    | estado_docu SMALLINT     | OK               |
|
roDocu            | String         | NO    | nro_docu VARCHAR(30)     | OK               |
| descripcion        | String         | NO    | (no en modelo exacto)    | OK               |
| 	ipoFirmaReq       | TipoFirmaReq   | NO    | tipo_firma_req SMALLINT  | OK               |
| plantillaId        | Long           | NO    | plantilla_id BIGINT      | OK               |
| idTalonario        | Long           | NO    | id_talonario BIGINT      | OK               |
|
roTalonarioUsado  | Integer        | NO    | nro_talonario_usado INT  | OK               |

**Campos AUSENTES en FalDocumento vs modelo MariaDB:**

| Campo MariaDB         | Tipo             | Null | Descripcion                    | Delta pendiente |
|-----------------------|------------------|------|--------------------------------|-----------------|
| hash_docu           | VARCHAR(128)     | SI   | Hash de integridad del doc     | AUSENTE en Java |
| h_generacion       | DATETIME(6)      | SI   | Fecha de emision formal        | AUSENTE en Java |

**Nota critica:** FalDocumento.fechaGeneracion mapea a h_alta (alta tecnica, NOT NULL, final). NO es h_generacion (emision formal, nullable, mutable). Son conceptos distintos.

**Metodos de dominio actuales:**
- marcarFirmado(): PENDIENTE_FIRMA -> FIRMADO (implementado en 8C-6B-1)
- esBorrador(), estaFirmado(), pendienteFirma(): helpers de estado
- NO existe marcarEmitido() (se crea en 8C-6C-1)

**EstadoDocu.EMITIDO:** Declarado (codigo=2). NO usado en ningun servicio ni metodo actualmente.
**EstadoDocu.FIRMADO:** Usado activamente en FalDocumento.marcarFirmado() y DocumentoService.

---

### Diagnostico DocumentoService

**Metodos existentes:**

| Metodo                              | Slice  | Estado       | Descripcion                                    |
|-------------------------------------|--------|--------------|------------------------------------------------|
| generarDocumento(...)             | legacy | LEGACY       | Crea doc sin plantilla, estado PENDIENTE_FIRMA, storageKey simulado. Reemplazable. |
| generarDesdePlantilla(...)        | 8C-3   | ACTIVO       | Crea doc desde plantilla, estado BORRADOR.     |
|
umerarDocumento(...)             | 8C-5A  | ACTIVO       | Numera doc, sin restriccion de estado. Reusable desde BORRADOR, PENDIENTE_FIRMA y FIRMADO. |
| enviarAFirma(...)                 | 8C-5B  | ACTIVO       | BORRADOR -> PENDIENTE_FIRMA; AL_ENVIAR_A_FIRMA numera antes. |
| irmarDocumento(...)              | legacy | LEGACY       | Naive: PENDIENTE_FIRMA -> FIRMADO sin firma real. Mantener para flujo completo. |
|
egistrarFirmaDocumental(...)     | 8C-6B-1| ACTIVO       | Firma real con 21 validaciones; AL_FIRMAR numera antes; FIRMADO cuando todos los req cumplidos. |
| obtenerDocumentos(Long idActa)    | -      | ACTIVO       | Consulta de documentos por acta.               |

**Metodo legacy a reemplazar:** generarDocumento es el candidato principal. Crea el documento en PENDIENTE_FIRMA con storageKey simulado, sin plantilla. El flujo nuevo pasa por generarDesdePlantilla. No eliminar todavia para no romper tests existentes.

**Emision mezclada con firma:** NO. El servicio no mezcla emision con firma. La emision formal es un paso posterior separado, aun no implementado.

---

### Diagnostico FalDocumentoPlantilla

Flags presentes en FalDocumentoPlantilla.java:

| Flag                     | Tipo    | Final | Getter | Setter | Uso actual                                      |
|--------------------------|---------|-------|--------|--------|-------------------------------------------------|
| siGeneraPdf            | boolean | NO    | SI     | SI     | No condicionado actualmente en ningun servicio  |
| siNotificable          | boolean | NO    | SI     | SI     | No condicionado actualmente en ningun servicio  |
| siSeleccionable        | boolean | NO    | SI     | SI     | No condicionado actualmente en ningun servicio  |
| siRequiereNumeracion   | boolean | SI    | SI     | NO     | Condicionado en numerarDocumento y generarDesdePlantilla |
| momentoNumeracionDocu  | enum    | SI    | SI     | NO     | Condicionado en enviarAFirma, registrarFirmaDocumental, generarDesdePlantilla |

**Contrato de siGeneraPdf para 8C-6C-1:**
- siGeneraPdf = true: al emitir, el servicio debe exigir storageKey y hashDocu como input. Ambos obligatorios. hGeneracion obligatorio.
- siGeneraPdf = false: el documento puede emitirse sin PDF generado por sistema. storageKey y hashDocu opcionales (pueden ser null). hGeneracion se setea igual.

**Contrato de siNotificable para 8C-6C-1:**
- siNotificable = true NO significa notificar automaticamente al emitir.
- Solo habilita/marca que el documento puede ser notificado en un flujo posterior.
- 8C-6C-1 no implementa notificacion.

**Contrato de momentoNumeracionDocu = AL_EMITIR:**
- El documento puede existir sin nroDocu hasta el momento de emision.
- Puede pasar por PENDIENTE_FIRMA y FIRMADO sin numerarse.
- Al emitir: si el documento no esta numerado, numerar automaticamente antes de generar storage/hash.
- El numero forma parte del documento emitido; si es generado despues de numerar, el numero debe estar incluido.

---

### Contrato de FIRMADO vs EMITIDO

**Confirmado: FIRMADO y EMITIDO son estados SECUENCIALES, no alternativos.**

- FIRMADO = el documento completo todos sus requisitos de firma institucional (todos los firma_req obligatorios activos quedaron FIRMADO). Transicion: PENDIENTE_FIRMA -> FIRMADO.
- EMITIDO = el documento fue generado/cerrado formalmente para uso administrativo, con representacion final, storage/hash si corresponde. Estado terminal del flujo activo.

**Flujos validos por tipoFirmaReq:**

| tipoFirmaReq     | Flujo de estados                                   |
|------------------|----------------------------------------------------|
| NO_REQUIERE      | BORRADOR -> EMITIDO                                |
| Cualquier firma  | BORRADOR -> PENDIENTE_FIRMA -> FIRMADO -> EMITIDO  |
| ADJUNTO          | (estado ADJUNTO: flujo de convalidacion separado, 8C-6D) |

**Estados terminales documentales:**
- EMITIDO: documento emitido formalmente, en uso/notificable.
- ADJUNTO: documento adjunto/escaneado (flujo 8C-6D).
- ANULADO: anulado logicamente.
- REEMPLAZADO: reemplazado por version posterior.

**Nota sobre el codigo numerico:** EMITIDO tiene codigo=2 (menor que PENDIENTE_FIRMA=3 y FIRMADO=4). Los codigos son identificadores de catalogo, no representan orden de flujo.

---

### Diagnostico de numerarDocumento para AL_EMITIR

**Conclusion:**
umerarDocumento es reusable desde 8C-6C-1 sin modificacion.

- NO tiene restriccion de estado en el documento (no verifica EstadoDocu).
- Puede llamarse con documento en BORRADOR, PENDIENTE_FIRMA o FIRMADO.
- Solo restringe: documentoId obligatorio, plantilla existe, siRequiereNumeracion=true, momentoNumeracionDocu != NO_APLICA.
- Confirmado: en 8C-6B-1 se llama desde
egistrarFirmaDocumental con documento en PENDIENTE_FIRMA (AL_FIRMAR). Funciona.
- Para 8C-6C-1: llamar
umerarDocumento dentro de emitirDocumento cuando momentoNumeracionDocu = AL_EMITIR y
roDocu == null. No requiere ajuste previo.

---

### Contrato de estados permitidos para emitir

Reglas para el metodo emitirDocumento de 8C-6C-1:

1. Si 	ipoFirmaReq = NO_REQUIERE:
   - Puede emitirse desde BORRADOR.
   - No debe haber pasado por PENDIENTE_FIRMA ni FIRMADO.

2. Si 	ipoFirmaReq != NO_REQUIERE:
   - Solo puede emitirse desde FIRMADO.
   - No puede emitirse desde BORRADOR ni PENDIENTE_FIRMA.

3. Si documento ya esta EMITIDO:
   - Falla con PrecondicionVioladaException. No se puede re-emitir en este slice.

4. Si documento esta ANULADO o REEMPLAZADO:
   - Falla con PrecondicionVioladaException.

5. Si documento esta ADJUNTO:
   - No aplica emision por este flujo; es flujo de convalidacion (8C-6D).

---

### Contrato de storage/hash para 8C-6C-1 (in-memory)

No se implementara storage real ni PDF real en 8C-6C-1.

| Campo          | Tipo in-memory | Obligatorio si siGeneraPdf=true | Obligatorio si siGeneraPdf=false |
|----------------|----------------|---------------------------------|----------------------------------|
| storageKey   | String input   | SI (exigir en request)          | NO (puede ser null)              |
| hashDocu     | String input   | SI (exigir en request)          | NO (puede ser null)              |
| hGeneracion | LocalDateTime  | SI (setear en servicio)         | SI (setear en servicio)          |

- storageKey y hashDocu se reciben como input simulado en el comando/request. No se calculan.
- hGeneracion se setea en el servicio al momento de llamar emitirDocumento.
- Para futura implementacion productiva:
  - El hash debe calcularse sobre el contenido final del PDF generado, despues de numerar si corresponde.
  - Si el documento fue firmado digitalmente, el hash de firma ya incluye el contenido firmado; la emision debe referenciar ese artefacto, no generar uno diferente.

---

### Regla: PDF despues de firma digital

**Regla critica documentada (no implementar en 8C-6C-1):**

Si un documento fue firmado digitalmente (TipoFirma.DIGITAL):
- El contenido firmado ya incluye el numero y el contenido definitivo al momento de la firma.
- La emision formal NO debe regenerar un PDF diferente que invalide el hash/firma de al_documento_firma.hash_documento.
- En ese caso, la emision debe consolidar/referenciar el artefacto ya firmado:
  - al_documento.storage_key apuntara al mismo objeto en storage que al_documento_firma.storage_key.
  - al_documento.hash_docu debe ser igual o derivado del al_documento_firma.hash_documento.
- En 8C-6C-1 (in-memory): el caller pasa storageKey y hashDocu como input; es su responsabilidad informar la referencia correcta.

---

### Relacion FIRMADO / storage / hash

- FIRMADO no necesariamente tiene storageKey/hashDocu en al_documento.
  - FalDocumento.storageKey puede seguir siendo null despues de marcarFirmado().
  - FalDocumentoFirma.hashDocumento y FalDocumentoFirma.storageKey pueden registrar referencia de firma digital.
- EMITIDO consolida storageKey/hashDocu en al_documento si siGeneraPdf = true.
- Si firma digital produjo referencia externa, la emision debe respetar esa referencia (ver regla PDF + firma digital).
- hGeneracion se setea al emitir, no al firmar.

---

### Validaciones para 8C-6C-1

El metodo emitirDocumento(EmitirDocumentoCommand cmd) debe implementar:

1. documentoId obligatorio y no nulo.
2. Documento existe en repositorio; sino, DocumentoNoEncontradoException.
3. plantillaId del documento no nulo (document generado desde plantilla).
4. Plantilla existe; sino, DocumentoPlantillaNoEncontradaException.
5. Plantilla activa (siActiva = true); sino, PrecondicionVioladaException.
6. Documento no esta ya EMITIDO; sino, PrecondicionVioladaException.
7. Documento no esta ANULADO; sino, PrecondicionVioladaException.
8. Documento no esta REEMPLAZADO; sino, PrecondicionVioladaException.
9. Si 	ipoFirmaReq = NO_REQUIERE: estado debe ser BORRADOR; sino, PrecondicionVioladaException.
10. Si 	ipoFirmaReq != NO_REQUIERE: estado debe ser FIRMADO; sino, PrecondicionVioladaException.
11. Si momentoNumeracionDocu = AL_EMITIR y
roDocu == null: llamar
umerarDocumento antes de storage/hash.
12. Si momentoNumeracionDocu = AL_CREAR, AL_ENVIAR_A_FIRMA o AL_FIRMAR, y siRequiereNumeracion = true y
roDocu == null: inconsistencia; PrecondicionVioladaException.
13. Si siGeneraPdf = true: storageKey y hashDocu en el comando son obligatorios; sino, PrecondicionVioladaException.
14. Setear hGeneracion = LocalDateTime.now() en el documento.
15. Setear storageKey del comando en el documento (si presente).
16. Setear hashDocu del comando en el documento (agregado en 8C-6C-1).
17. Cambiar estadoDocu = EstadoDocu.EMITIDO.
18. Guardar documento en repositorio.
19. Registrar evento DOCEMI (o equivalente, confirmar codigo) en al_acta_evento.
20. Recalcular snapshot.
21. NO firmar.
22. NO notificar.
23. NO tocar
esultadoFirmaInfractor.
24. NO tocar FalDocumentoFirmaReq.
25. NO tocar FalDocumentoFirma.

---

### Nuevos elementos a implementar en 8C-6C-1

**FalDocumento (cambios de campo):**
- Agregar campo hashDocu: String (nullable). Getter/setter. Mapea a hash_docu VARCHAR(128).
- Agregar campo hGeneracion: LocalDateTime (nullable, mutable). Getter/setter. Mapea a h_generacion DATETIME(6).
- Agregar metodo marcarEmitido(): valida estado permitido (BORRADOR o FIRMADO segun tipoFirmaReq), transiciona a EMITIDO.
- Agregar helper estaEmitido().

**EmitirDocumentoCommand:**
`
record EmitirDocumentoCommand(
    Long documentoId,        // NOT NULL
    String storageKey,       // nullable; obligatorio si siGeneraPdf=true
    String hashDocu,         // nullable; obligatorio si siGeneraPdf=true
    String idUserEmision     // NOT NULL: auditoria
)
`

**DocumentoService.emitirDocumento(EmitirDocumentoCommand):**
- Implementar las 25 validaciones del contrato.

**EmitirDocumentoRequest / DocumentoEmitidoResponse:**
- Request HTTP para endpoint.

**Endpoint:**
- POST /api/faltas/documentos/{documentoId}/emitir

**TipoEventoActa:**
- `DOCEMI` NO existe en `TipoEventoActa` (confirmado en 8C-6C-0). Agregar en 8C-6C-1: `DOCEMI("DOCEMI", "Documento emitido formalmente")`.

---

### No implementado en 8C-6C-0

- Java funcional.
- PDF real.
- Storage real.
- Hash criptografico real.
- Cambio de estado de documentos.
- MariaDB/JDBC.
- Angular.

---

### Proximo slice

**8C-6C-1 -- Implementar emision formal in-memory con storage/hash simulado.**


---

## 8C-6C-1 -- CERRADO (2026-07-01)

**Estado:** CERRADO. Build: 855/855 tests. BUILD SUCCESS.

**Implementado:**
- `FalDocumento.hashDocu` y `FalDocumento.fhGeneracion` agregados.
- `FalDocumento.marcarEmitido(storageKey, hashDocu, fhGeneracion)` implementado.
- `FalDocumento.estaEmitido()` implementado.
- `TipoEventoActa.DOCEMI` agregado.
- `EmitirDocumentoCommand` creado.
- `EmitirDocumentoRequest` creado.
- `DocumentoResponse` actualizado con `storageKey`, `hashDocu`, `fhGeneracion`.
- `DocumentoService.emitirDocumento(EmitirDocumentoCommand)` implementado.
- `POST /api/faltas/documentos/{documentoId}/emitir` implementado.
- `DocumentoEmisionFormalTest` con 39 casos.

**Reglas implementadas:**
- BORRADOR -> EMITIDO para NO_REQUIERE.
- FIRMADO -> EMITIDO para documentos con firma.
- PENDIENTE_FIRMA no puede emitirse.
- AL_EMITIR numera automaticamente antes de emitir.
- Otros momentos: doc debe llegar numerado.
- siGeneraPdf=true: storageKey y hashDocu obligatorios.
- siGeneraPdf=false: pueden ser null.
- Evento DOCEMI registrado.
- Sin notificacion automatica.
- Sin PDF/storage real.
- Sin firma nueva.

---

## Proximos slices recomendados

---

## Micro-slice 8C-6D-0: Diagnostico y contrato de convalidacion de documento escaneado [COMPLETADO]

**Fecha:** 2026-07-01
**Tipo:** Solo diagnostico/documentacion. Sin cambios Java.
**Build:** no ejecutado (sin cambios Java). Base confirmada: 855/855 tests passing.

### Diagnostico ADJUNTO

- **Existe:** Si. EstadoDocu.ADJUNTO((short) 5).
- **Uso actual:** SnapshotRecalculador trata ADJUNTO equivalente a FIRMADO/EMITIDO en 	odosDocsFirmados.
  No hay flujo de creacion de adjuntos. No hay tests.
- **Contrato:** estado inicial directo. storageKey y hashDocu obligatorios.
  hGeneracion = fecha de incorporacion. plantillaId = null para externos.

### Diagnostico plantilla para adjuntos

- **Opcion recomendada:** Opcion B -- Adjuntos sin plantilla obligatoria.
- **Justificacion:** El modelo MariaDB ya define plantilla_id como nullable para documentos externos.
  El flujo de incorporacion de adjuntos es un metodo separado (incorporarDocumentoEscaneado),
  no pasa por emitirDocumento().

### Diagnostico storage/hash

- storageKey, hashDocu, hGeneracion: obligatorios para adjuntos.
- No se necesita campo nuevo hIncorporacion. hGeneracion cubre la fecha de incorporacion.
- Hash simulado como input en in-memory (igual que 8C-6C-1).

### Diagnostico convalidacion de firma

- FalDocumentoFirma con TipoFirma.OLOGRAFA y estadoFirma = FIRMADA.
-
eferenciaFirmaExt: apunta a la firma visible en el escaneo. No obligatorio para OLOGRAFA.
- storageKey y hashDocumento en FalDocumentoFirma: apuntan al archivo escaneado.
- Firmante/convalidador debe existir y tener habilitacion compatible si cumple un requisito.
- Sin requisito: firma registrada como convalidacion simple (trazabilidad) sin cumplir FalDocumentoFirmaReq.

### Diagnostico compatibilidad con FalDocumentoFirmaReq

- Si hay requisito: validaciones identicas a firma real (firmante, version, habilitacion, mecanismo si aplica).
- Marcar requisito FIRMADO via marcarFirmado().
- Si todos obligatorios activos firmados: transicion ADJUNTO -> FIRMADO (nueva regla).
- FalDocumento.marcarFirmado() hoy solo acepta desde PENDIENTE_FIRMA. 8C-6D-1 debe agregar ADJUNTO -> FIRMADO.

### Diagnostico estados

- **Escaneo simple:** ADJUNTO permanente.
- **Escaneo con convalidacion y requisito cumplido:** ADJUNTO -> FIRMADO.
- **Emision posterior:** No automatica. Requiere flujo explicito de emision.

### Eventos

- **Recomendado nuevo:** DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente").
  Distinto de DOCGEN (generado por sistema). Valor auditoriable real.
- **Convalidacion firma:** reutilizar DOCFIR. Semanticamente compatible.
- **DOCCON:** reservar para futuro si se necesita distinguir convalidacion de firma real en audit trail.

### Validaciones para 8C-6D-1

#### Incorporar documento escaneado

1. Acta existe.
2. 	ipoDocu valido.
3. storageKey obligatorio.
4. hashDocu obligatorio.
5. idUserAlta obligatorio.
6. Estado inicial ADJUNTO.
7. hGeneracion seteado.
8. plantillaId null para externos.
9. Sin numeracion automatica.
10. Sin firma automatica.
11. Sin emision automatica.
12. Evento DOCADJ.

#### Convalidar firma escaneada

1. Documento existe con storageKey y hashDocu.
2. 	ipoFirma = OLOGRAFA.
3. Firmante existe con version vigente.
4. Si seqFirmaReq != null: requisito activo y pendiente; habilitacion compatible.
5. Si mecanismoFirmaReq != null: verificar compatibilidad.
6. Crear FalDocumentoFirma con TipoFirma.OLOGRAFA, estadoFirma = FIRMADA.
7. Si seqFirmaReq != null: marcar requisito FIRMADO.
8. Si todos obligatorios activos firmados y documento en ADJUNTO: transicion ADJUNTO -> FIRMADO.
9. Evento DOCFIR.
10. Sin firma digital, sin PDF, sin notificacion.

### Documentacion actualizada

- docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md: seccion 8C-6D-0 agregada.
- ackend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md: este archivo.

---

## 8C-6D-1 -- Implementar incorporacion de documento escaneado y convalidacion in-memory [CERRADO 2026-07-02]

### Objetivo

Implementar funcionalmente el flujo de adjuntos/escaneados en in-memory:
- Nuevo metodo incorporarDocumentoEscaneado en DocumentoService.
- Nuevo metodo convalidarFirmaEscaneada en DocumentoService.
- Nuevo evento DOCADJ en TipoEventoActa.
- Nueva transicion ADJUNTO -> FIRMADO en FalDocumento.

### Cambios Java necesarios

1. TipoEventoActa: agregar DOCADJ("DOCADJ", "Documento adjuntado/incorporado al expediente").
2. FalDocumento: agregar marcarFirmadoDesdeAdjunto() o ampliar marcarFirmado() para aceptar desde ADJUNTO.
3. DocumentoService: agregar incorporarDocumentoEscaneado(IncorporarDocumentoEscaneadoCommand).
4. DocumentoService: agregar convalidarFirmaEscaneada(ConvalidarFirmaEscaneadaCommand).
5. Commands: IncorporarDocumentoEscaneadoCommand, ConvalidarFirmaEscaneadaCommand.
6. DocumentoController: endpoints POST /api/faltas/actas/{id}/documentos/adjunto y
   POST /api/faltas/documentos/{id}/convalidar-firma.
7. Tests: DocumentoAdjuntoTest (nueva clase).

### Guardrails a preservar

- No usar emitirDocumento() para adjuntos.
- No crear FalDocumentoFirmaReq automaticamente para adjuntos simples.
- No emitir DOCEMI al incorporar adjunto.
- No calcular hash real.
- No usar storage real.
- No MariaDB/JDBC.
- No Angular.

---

### 8C-6E -- Auditoria pre-JDBC/MariaDB

- Revisar delta completo antes de implementar repositorios JDBC.
- Reconciliar fal_documento.hash_docu y fal_documento.fh_generacion con modelo productivo.
- Validar todos los enums contra catalogos de MariaDB.

---

### Slice 9 -- MariaDB/JDBC

- Implementar repositorios JDBC para todos los modulos.
- Usar Spring JDBC / JdbcClient.
- Sin JPA/Hibernate.

---

## 8C-6D-1 -- Cerrado 2026-07-02

**Build base antes del slice:** 855/855 tests passing. BUILD SUCCESS.

### Implementado

- `TipoEventoActa.DOCADJ` agregado.
- `FalDocumento.crearAdjunto(...)`: factory para adjuntos externos. Estado inicial `ADJUNTO`.
- `FalDocumento.marcarFirmadoDesdeAdjunto()`: transicion `ADJUNTO -> FIRMADO`.
- `FalDocumento.estaAdjunto()`: helper booleano.
- `IncorporarDocumentoEscaneadoCommand`, `IncorporarDocumentoEscaneadoRequest`.
- `ConvalidarFirmaEscaneadaCommand`, `ConvalidarFirmaEscaneadaRequest`.
- `ConvalidacionFirmaEscaneadaResponse`, `ConvalidacionEscaneadaResultado`.
- `DocumentoService.incorporarDocumentoEscaneado(...)`.
- `DocumentoService.convalidarFirmaEscaneada(...)`.
- Helpers privados: `resolverFirmante`, `resolverVersionVigente`, `resolverHabilitacion`, `validarMecanismo`, `validarOrdenFirma`.
- Endpoints: `POST /api/faltas/actas/{idActa}/documentos/escaneados`, `POST /api/faltas/documentos/{id}/convalidar-firma-escaneada`.
- `DocumentoAdjuntoTest.java`: 53 tests.

### Reglas cumplidas

- Adjunto externo sin plantilla obligatoria.
- `storageKey`, `hashDocu`, `fhGeneracion` obligatorios para adjuntos.
- `DOCADJ` para incorporacion.
- Convalidacion simple sin requisito: no usa `seqFirmaReq=0`, solo `DOCFIR` event.
- Convalidacion con requisito: `TipoFirma.OLOGRAFA`, hash del documento, cumple `FalDocumentoFirmaReq`.
- `ADJUNTO -> FIRMADO` solo cuando todos los obligatorios activos firmados.
- Sin firma digital, sin emision automatica, sin notificacion.
- Sin MariaDB/JDBC, sin Angular.

### Proximos slices recomendados

1. **8C-6E** -- Auditoria pre-JDBC/MariaDB: reconciliar delta completo antes de JDBC.
2. **Slice 9** -- MariaDB/JDBC: repositorios JDBC para todos los modulos.

### Nota roadmap OCR/IA

Los documentos escaneados podran ser procesados posteriormente por OCR o IA para extraccion
asistida de metadata. Esa extraccion debe generar datos sugeridos y auditables, y requerir
validacion humana antes de impactar datos del dominio.

Esta funcionalidad NO esta implementada en 8C-6D-1. Queda como roadmap posterior.


---

## 8C-6E -- Auditoria pre-JDBC/MariaDB -- CERRADO 2026-07-02

**Build base:** 908/908 tests passing. BUILD SUCCESS.
**Tipo:** Auditoria y documentacion. Sin cambios funcionales Java. Sin cambios MariaDB/JDBC. Sin Angular.

### Completado

- Auditoria completa del modelo Java vs modelo MariaDB productivo.
- DELTA reconciliado: CERRADO / ACTIVO PRE-JDBC / ROADMAP POST-JDBC / HISTORICO clasificados.
- Enums auditados y confirmados alineados (TipoDocu, EstadoDocu, EstadoFirma, TipoFirmaReq, MomentoNumeracionDocu, ResultadoFirmaInfractor, TipoFirma, EstadoFirmaReq, TipoEventoActa).
- Flujos documentales auditados: sin firma, con firma, numeracion, firma real, emision, escaneado, convalidacion.
- Legacy controlado: generarDocumento/firmarDocumento activos, conviven, deprecar post-Slice 9.
- Repositorios listados con tabla MariaDB correspondiente y notas JDBC.
- Reglas transaccionales criticas documentadas.
- Documento 101-auditoria-pre-jdbc-mariadb.md creado.

### Verificaciones

- docs/spec-as-source historica: NO EXISTE (False).
- FIRMA_MIXTA funcional: CERO referencias funcionales.
- NO_REQUERIDA en ResultadoFirmaInfractor: NO EXISTE.
- obsFirmaInfractor en FalActa: NO EXISTE.
- JDBC/JPA/Hibernate implementado: NO.
- OCR/IA Java funcional: NO IMPLEMENTADO.
- Java funcional nuevo: NINGUNO.

### Pendientes post-JDBC o roadmap separados

Post-Slice 9 (despues de JDBC):
- OCR/IA: extraccion asistida metadata, datos sugeridos, validacion humana obligatoria.
- PDF/storage real: storage fisico real y hash criptografico.
- Proveedor firma digital real: integracion con proveedor externo.
- Notificaciones automaticas: siNotificable activo.
- Angular/frontend: no tocar hasta decision de etapa.
- fal_observacion: tabla completa para observaciones estructuradas.

### Proximo recomendado

**Slice 9 -- MariaDB/JDBC con JdbcClient, sin JPA/Hibernate.**

Entradas limpias:
- Modelo Java completamente alineado con MariaDB productivo.
- Enums productivos con codigos SMALLINT correctos.
- Interfaces de repositorio limpias y reemplazables.
- Flujos documentales completos y testeados in-memory.
- DELTA reconciliado.
- Reglas transaccionales documentadas.
- DDL corrections identificadas en ACTIVOS PRE-JDBC del documento 101.

Ver documento completo: backend/api-faltas-core/docs/spec-as-source/101-auditoria-pre-jdbc-mariadb.md


---

## Slice 9-0 -- Estrategia JDBC / MariaDB -- CERRADO 2026-07-02

**Build base:** 908/908 tests passing. BUILD SUCCESS.
**Tipo:** Estrategia + documentacion + preparacion. Sin cambios funcionales Java. Sin MariaDB/JDBC. Sin Angular.

### Completado

- Definicion de estrategia JDBC general: Spring JDBC con JdbcClient. Sin JPA/Hibernate.
- Decision definitiva de PK internas: BIGINT AUTO_INCREMENT para todas las entidades productivas.
- UUID/String tecnico como clave alternativa solo cuando se requiera correlacion offline,
  integracion externa o idempotencia. No reemplaza la PK BIGINT.
- Decision definitiva de enums cerrados: enum Java + SMALLINT en MariaDB. Sin tabla fisica. Sin seed.
- Catalogos cerrados identificados: EstadoDocu, TipoDocu, TipoFirmaReq, MomentoNumeracionDocu,
  TipoFirma, ResultadoFirmaInfractor, EstadoFirma, EstadoFirmaReq, TipoEventoActa, TipoEvidenciaActa,
  ClaseNumeracion, TipoTalonario, AlcanceTalonario, EstadoNumeroTalonario, MotivoAnulacionTalonario,
  EstadoAsignacionTalonario.
- Diferencia enum cerrado vs dato configurable documentada.
- Opciones para descripciones SQL de enums: funciones SQL / vistas CASE / CASE directo.
  No crear tablas catalogo. No implementar en este slice.
- DDL delta activo pre-JDBC documentado (ver seccion 9 del documento 102).
- Datos configurables / productivos que si son tablas documentados.
- Transacciones criticas para Slice 9 documentadas.
- Primer piloto JDBC elegido: DocumentoPlantillaRepository (bajo riesgo, prueba enums SMALLINT y JdbcClient).
- Plan incremental Slice 9 documentado: 9-1 a 9-9.
- Convenciones de mapeo Java <-> SQL documentadas.
- Documento 102-slice-9-estrategia-jdbc-mariadb.md creado.
- DELTA actualizado con decisiones cerradas.
- 100-etapa-8 y 101-auditoria actualizados.

### Verificaciones

- docs/spec-as-source historica: NO EXISTE (False).
- Sin FIRMA_MIXTA, sin NO_REQUERIDA, sin obsFirmaInfractor.
- Sin JPA/Hibernate.
- Sin tablas catalogo para enums cerrados.
- Sin seeds para enums cerrados.
- Java funcional nuevo: NINGUNO.

### Proximo recomendado

**Slice 9-1 -- Infraestructura JDBC base con JdbcClient, perfiles y test de conexion.**

Objetivos de 9-1:
- Agregar dependencia MariaDB driver en pom.xml.
- Configurar JdbcClient como bean Spring.
- Crear application.yml base y application-jdbc.yml.
- Test de conexion basico (ping o query simple).
- Sin migrar dominio todavia.
- Sin tablas para enums cerrados.
- Sin @Transactional en servicios todavia.

Regla activa para todo Slice 9:
- No crear tablas fisicas para enums cerrados.
- No crear seeds para enums cerrados.
- BIGINT como PK interna. UUID/String solo como alternativo si aplica.
- No usar JPA/Hibernate.
- Usar JdbcClient.


---

## Slice 9-1 -- Infraestructura JDBC base -- CERRADO 2026-07-02

**Build:** 908/908 tests passing. BUILD SUCCESS.
**Tipo:** Infraestructura Java / configuracion Spring. Con cambios funcionales de config/infra. Sin migracion de repositorios.

### Completado

- Dependencias agregadas en pom.xml:
  - spring-boot-starter-jdbc
  - org.mariadb.jdbc:mariadb-java-client (scope runtime)
- Perfiles de persistencia creados:
  - application.yml (default): excluye DataSourceAutoConfiguration; InMemory* activo.
  - application-jdbc.yml (perfil jdbc): DataSource con variables de entorno; JdbcClient auto-configurado.
- JdbcConfig.java creado (@Profile("jdbc"), punto de extension para futuros slices).
- JdbcInfrastructureIT.java creado (@EnabledIfEnvironmentVariable("FALTAS_DB_URL")):
  - omitido en build normal.
  - ejecuta 3 tests cuando FALTAS_DB_URL esta definida.
- Sin JPA/Hibernate. Sin repositorios JDBC de dominio. Sin tablas enums. Sin seeds enums.
- Repositorios InMemory* sin modificar.
- FlujoCoreIT sigue pasando con perfil default.
- Documento 103-slice-9-1-infraestructura-jdbc.md creado.

### Variables de entorno para perfil jdbc

  FALTAS_DB_URL      - URL MariaDB (ej: jdbc:mariadb://localhost:3306/faltas_db)
  FALTAS_DB_USER     - Usuario
  FALTAS_DB_PASSWORD - Password

### Proximo recomendado

**Slice 9-2 -- Piloto JdbcDocumentoPlantillaRepository con JdbcClient.**

Objetivos de 9-2:
- DDL: crear fal_documento_plantilla y fal_documento_plantilla_firma_req.
- Implementar JdbcDocumentoPlantillaRepository.
- Mapeo: enums como SMALLINT, ids BIGINT.
- Activar via perfil jdbc.
- Tests de integracion con MariaDB real.


---

## Slice 8F-1 cerrado -- Plantillas operativas, variables documentales y motor de combinacion

**Estado: CERRADO**
**Build: 954/954 tests OK**

### Implementado

- FalDocumentoPlantillaDefault (resolucion automatica de plantilla).
- FalDocumentoPlantillaContenido (contenido versionado con template).
- FalDocumentoRedaccion (redaccion editable en BORRADOR).
- DocumentoVariableDefinicion (record, catalo in-memory).
- 4 enums cerrados: FormatoPlantillaContenido, EstadoRedaccionDocumento, DocumentoVariableNamespace, TipoDatoVariableDocumento.
- 4 excepciones de dominio.
- 3 repositorios in-memory (Default, Contenido, Redaccion).
- DocumentoVariableRegistry (15 variables minimas).
- DocumentoCombinacionService (motor seguro sin eval/SpEL).
- DocumentoVariableContextBuilder (util para 8F-2).
- DocumentoPlantillaDefaultService.
- DocumentoRedaccionService.
- 46 tests nuevos (combinacion + default + redaccion).

### Guardrails cumplidos

- No JDBC. No JPA/Hibernate. No tablas. No seeds.
- No PDF/storage en redaccion BORRADOR.
- Motor sin SpEL, eval, ScriptEngine ni JavaScript.
- No Angular. No OCR/IA.

### Proximo recomendado

**8F-2 -- Context builder desde acta/snapshots + plantillas mock por caso de uso.**

Objetivos 8F-2:
- DocumentoVariableContextBuilder completo desde FalActa + FalActaSnapshot.
- Plantillas mock default para: fallo, notificacion acta, notificacion fallo, intimacion pago, medida preventiva, constancia, anexo, resolutorio bloqueante.
- Contenido default para cada plantilla.
- Tests del builder y plantillas mock.

---

## Slice 8F-2 cerrado -- Context builder desde acta/snapshots + plantillas mock

**Estado: CERRADO**
**Build: 1040/1040 tests OK**
**Fecha: 2026-07-02**

### Implementado

- `DocumentoVariableContextBuilder` expandido: 13 namespaces, fallo/pago/notificacion opcionales.
- `DocumentoVariableRegistry` expandido: 26 variables (de 15 a 26).
- `GraphDemoActaFactory`: factory de datos demo deterministicos (acta, fallo, pago, notificacion, documento).
- `PlantillasMockSeeder`: 8 plantillas mock con contenido representativo y variables reales.
- `DocumentoRedaccionService.crearRedaccionConContextoActa()`: nuevo metodo que construye contexto
  automaticamente desde idActa, cargando fallo y pago opcionales del repositorio.
- 86 nuevos tests: ContextBuilder (25), PlantillasMock (45+), GraphDemo (16).

### 8 casos operativos probados

EMITIR_FALLO, EMITIR_NOTIFICACION_ACTA, EMITIR_NOTIFICACION_FALLO,
EMITIR_INTIMACION_PAGO, EMITIR_MEDIDA_PREVENTIVA, EMITIR_CONSTANCIA,
EMITIR_ANEXO, EMITIR_RESOLUTORIO_BLOQUEANTE.

Todos producen redaccion BORRADOR completa con el contexto demo.

### Guardrails cumplidos

- No JDBC. No JPA/Hibernate. No tablas. No seeds de MariaDB.
- No PDF/storage en redaccion BORRADOR.
- No reflection, SpEL, eval.
- No Angular.
- docs/spec-as-source en raiz: False (confirmado).

### Proximo recomendado

**8F-3 -- Mock PDF renderer / generacion final simulada de documentos.**
## 8F-3 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Mock PDF renderer / generacion final simulada ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â CERRADO

**Estado**: CERRADO. Build: 1103 tests, 0 failures.

Implementado:
- DocumentoRenderizadoMock record
- DocumentoGeneracionMockResponse record
- ConfirmarRedaccionYGenerarDocumentoMockCommand record
- DocumentoPdfMockRenderer (@Service, renderer puro, SHA-256, storageKey mock://)
- DocumentoGeneracionMockService (@Service, orquestador del flujo)
- FalDocumentoRedaccion.confirmar() metodo de dominio
- Reglas: no ANULADA, no dos veces, contenido no vacio
- 4 suites de tests: renderer, confirmacion dominio, servicio, graph demo 8 casos
- Guardrails verificados: BORRADOR sin storage, CONFIRMADA con storage mock
- Documentacion spec-as-source 104, 99, 100 actualizadas

## 8F-4 - Graph demo completo con documentos redactados y PDF final simulado - CERRADO

**Estado**: CERRADO. Build: 1115 tests, 0 failures.

Implementado:
- DocumentoGraphDemoCasoResultado record (resultado por caso documental)
- DocumentoGraphDemoResultado record (resultado agregado del graph)
- DocumentoGraphDemoService (@Service, recorre 8 casos de punta a punta)
- DocumentoGraphDemoController (@RestController, GET /demo/documentos/graph)
- Endpoint GET /demo/documentos/graph: acta fresca, 8 documentos, 8 redacciones
  BORRADOR -> CONFIRMADA -> PDF mock, storageKey mock://, hashDocu sha256-mock-
- Sin emision automatica, sin firma automatica, sin notificacion automatica

Guardrails cumplidos:
- No JDBC, no JPA/Hibernate, no tablas, no seeds de MariaDB.
- No PDF/storage real. No reflection, SpEL, eval. No Angular.
- docs/spec-as-source en raiz: False (confirmado).

## 8F-4B - Dataset funcional completo del dominio en memoria - CERRADO

**Estado**: CERRADO. Build: 1165 tests, 0 failures.

Por que se inserto 8F-4B antes de 8F-5:
El graph demo documental 8F-4 trabaja sobre una acta demo base generica.
Para poder implementar pruebas funcionales completas por caso de uso (8F-4C),
se necesitaba primero el dataset funcional completo: un universo canOnico de actas
mock que cubra todos los casos de uso del sistema, con estados iniciales definidos,
documentos esperados declarados y casos de uso trazados.

Diferencia entre graph demo documental y dataset funcional completo:
- Graph demo (8F-4): recorre el flujo documental de 8 tipos de documentos sobre una acta demo.
- Dataset funcional (8F-4B): declara 25 actas mock representando todos los casos de uso,
  con bloques, situaciones, resultados finales, bandeja, documentos esperados y cobertura.

### Implementado

Clases nuevas:

| Clase | Paquete | Rol |
|-------|---------|-----|
| ActaMockFuncionalDefinicion | application/demo/ | Record declarativo de acta mock funcional |
| DocumentoEsperadoPorActaMock | application/demo/ | Record con documento esperado por acta |
| DatasetFuncionalDominioCatalog | application/demo/ | Catalogo de 25 actas mock del dominio |
| DatasetFuncionalCoberturaResultado | application/result/ | Matriz de cobertura del dataset |
| DatasetFuncionalDemoController | web/ | GET /demo/actas/dataset-funcional |

Tests nuevos (50 tests):

| Suite | Tests | Descripcion |
|-------|-------|-------------|
| DatasetFuncionalDominioCatalogTest | 17 | Estructura e integridad del catalogo |
| DatasetFuncionalCoberturaTest | 7 | Calculo de cobertura funcional |
| ActasMockFuncionalesFactoryTest | 9 | Construccion deterministica de actas mock |
| DatasetFuncionalDocumentosEsperadosTest | 9 | Validacion de documentos esperados |
| DatasetFuncionalDemoEndpointIT | 8 | Integration test GET /demo/actas/dataset-funcional |

### 25 actas mock del dataset funcional

| Codigo | Caso de uso principal | Bloque | Resultado Final |
|--------|-----------------------|--------|-----------------|
| ACT-001-LABRADA | LabrarActa | CAPT | SIN_RESULTADO_FINAL |
| ACT-002-CAPTURA-COMPLETA | CompletarCaptura | ENRI | SIN_RESULTADO_FINAL |
| ACT-003-DOC-PENDIENTE-FIRMA | GenerarDocumento / FirmarDocumento | ENRI | SIN_RESULTADO_FINAL |
| ACT-004-PENDIENTE-ANALISIS | IniciarAnalisis | ANAL | SIN_RESULTADO_FINAL |
| ACT-005-NOTIFICACION-PENDIENTE | EnviarNotificacion | ANAL | SIN_RESULTADO_FINAL |
| ACT-006-NOTIFICACION-REALIZADA | EnviarNotificacion positiva | ANAL | SIN_RESULTADO_FINAL |
| ACT-007-PAGVOL-DISPONIBLE | SolicitarPagoVoluntario | ANAL | SIN_RESULTADO_FINAL |
| ACT-008-PAGVOL-REGISTRADO | InformarPagoVoluntario | ANAL | SIN_RESULTADO_FINAL |
| ACT-009-PAGVOL-CONFIRMADO | ConfirmarPagoVoluntario -> cerrada | CERR | PAGO_VOLUNTARIO_PAGADO |
| ACT-010-FALLO-ABSOLUTORIO | DictarFalloAbsolutorio | ANAL | SIN_RESULTADO_FINAL |
| ACT-011-FALLO-ABS-FIRMADO | FirmarDocumento absolutorio | ANAL | SIN_RESULTADO_FINAL |
| ACT-012-FALLO-COND-FIRMADO | FirmarDocumento condenatorio | ANAL | SIN_RESULTADO_FINAL |
| ACT-013-FALLO-COND-NOTIFICADO | NotificacionPositiva fallo | ANAL | SIN_RESULTADO_FINAL |
| ACT-014-APELACION-PRESENTADA | RegistrarApelacion | ANAL | SIN_RESULTADO_FINAL |
| ACT-015-CONDENA-FIRME | DeclararCondenaFirme | ANAL | CONDENA_FIRME |
| ACT-016-PAGO-CONDENA-INFORMADO | InformarPagoCondena | ANAL | CONDENA_FIRME |
| ACT-017-CONDENA-FIRME-PAGADA | ConfirmarPagoCondena -> cerrada | CERR | CONDENA_FIRME |
| ACT-018-GESTION-EXTERNA | DerivarGestionExterna | GEXT | CONDENA_FIRME |
| ACT-019-GESTION-EXTERNA-PAGO-EXTERNO | RegistrarPagoExternoGestion | ANAL | CONDENA_FIRME |
| ACT-020-PARALIZADA | ParalizarActa | ANAL | SIN_RESULTADO_FINAL |
| ACT-021-BLOQUEANTE-ACTIVO | RegistrarBloqueanteMaterial | ANAL | SIN_RESULTADO_FINAL |
| ACT-022-REDACCION-BORRADOR | CrearRedaccionDocumento (BORRADOR) | ANAL | SIN_RESULTADO_FINAL |
| ACT-023-REDACCION-CONFIRMADA | ConfirmarRedaccionYGenerarDocumentoMock | ANAL | SIN_RESULTADO_FINAL |
| ACT-024-FALLO-ABS-NOTIFICADO | NotificacionPositiva fallo absolutorio | ANAL | SIN_RESULTADO_FINAL |
| ACT-025-DOC-REDACCION-BORRADOR | Redaccion BORRADOR sin PDF | ANAL | SIN_RESULTADO_FINAL |

### Casos de uso pendientes documentados

- Acta con notificacion negativa fallida
- Acta con adjunto escaneado convalidado
- Acta cerrada por ABSOLUCION_FIRME
- Acta cerrada por REINGRESO_PARA_REVISION
- Acta con pago de condena observado / con descuento

### Guardrails cumplidos

- No JDBC, no JPA/Hibernate, no tablas, no seeds de MariaDB.
- No PDF/storage real, no librerÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­as PDF, no filesystem documental.
- No reflection, SpEL, eval, ScriptEngine.
- No Angular, no frontend.
- No endpoint reset todavia.
- docs/spec-as-source en raiz: False (confirmado).

### Proximo recomendado

**8F-4C -- Pruebas funcionales completas por caso de uso.**
## 8F-4B-R1 - Cierre de pendientes del dataset funcional - CERRADO

**Estado**: CERRADO. Build: 1173 tests, 0 failures.

Por que se creo este ajuste:
Tras cerrar 8F-4B el dataset tenia 5 casos documentados como pendientes.
El criterio se corrigio: el dataset funcional completo no debe tener casos
pendientes si son casos de uso conocidos del sistema. Se crearon 6 actas nuevas.

### Actas nuevas (ACT-026 a ACT-031)

- ACT-026-NOTIFICACION-NEGATIVA: notificacion con resultado NOTNEG
- ACT-027-DOC-ADJUNTO-CONVALIDADO: DOCADJ + ConvalidarFirmaEscaneada
- ACT-028-ABSOLUCION-FIRME-CERRADA: ResultadoFinal.ABSUELTO, CERR
- ACT-029-REINGRESO-PARA-REVISION: EXTRET + Slice 6D-1
- ACT-030-PAGO-CONDENA-OBSERVADO: PCOOBS, CONDENA_FIRME pendiente
- ACT-031-PAGO-CONDENA-CON-DESCUENTO: PCOCNF variante, GAP documentado

### Estado final del dataset

- 31 actas mock funcionales
- 0 pendientes de los 5 originales (todos cubiertos)
- casosUsoPendientes: solo gaps tecnicos reales (FirmaReq, Emision formal, Archivada, etc.)
- 8 tests nuevos en 2 suites

### Guardrails cumplidos

- No JDBC, no JPA/Hibernate, no tablas, no seeds.
- No PDF/storage real, no librerÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­as, no filesystem.
- No reflection, SpEL, eval. No Angular.
- docs/spec-as-source en raiz: False (confirmado).

### Proximo recomendado

**8F-4C -- Pruebas funcionales completas por caso de uso.**

---

## 8F-4C ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â CERRADO

**Estado**: CERRADO

**Tests run**: 2323 (BUILD SUCCESS, 0 failures)

### QuÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© se hizo

- 11 suites funcionales creadas cubriendo las 31 actas del dataset.
- 1 suite de cobertura total (DatasetFuncionalFlujosCoberturaTest).
- ActaParalizacionService, ParalizarActaCommand, ReactivarActaCommand implementados.
- CasoUsoFuncionalRunner - runner funcional del dataset completo.
- CasoUsoFuncionalEjecucionResultado, PasoFuncionalResultado - modelos de resultado.
- DecisiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n documentada: descuento en ACT-031 usa PCOCNF sin evento propio.
- No JDBC, no JPA, no setters directos, no gaps funcionales bloqueantes.

### Suites funcionales

ActaFlujoCapturaFuncionalTest, ActaFlujoDocumentalFuncionalTest, ActaFlujoNotificacionFuncionalTest, ActaFlujoPagoVoluntarioFuncionalTest, ActaFlujoFalloFuncionalTest, ActaFlujoApelacionFuncionalTest, ActaFlujoPagoCondenaFuncionalTest, ActaFlujoGestionExternaFuncionalTest, ActaFlujoParalizacionFuncionalTest, ActaFlujoBloqueanteFuncionalTest, ActaFlujoReingresoFuncionalTest, DatasetFuncionalFlujosCoberturaTest

### Gaps tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cnicos restantes (pendientes slices futuros)

- FirmaReq completa (workflow de req de firma institucional)
- EmisiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n formal numerada
- Acta archivada / REINGRESO_PARA_CIERRE
- Slice 9: JDBC/MariaDB (infraestructura futura, fuera del prototipo in-memory)

---

## PrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ximo slice recomendado

**8F-5 ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â Endpoint dev/test de reset y recreaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n in-memory.**

Objetivo: endpoint POST /dev/reset que destruye y recrea todos los repositorios in-memory con el dataset mock cargado. ÃƒÆ’Ã†â€™Ãƒâ€¦Ã‚Â¡til para tests E2E y desarrollo frontend.

---

## 8F-5 - CERRADO

**Estado**: CERRADO

**Tests run**: 1437 (BUILD SUCCESS)

### Que se hizo

- Interfaz `ResettableInMemoryRepository` (reset, nombre, size) creada en `repository.memory`.
- Los 24 `InMemory*Repository` implementan `ResettableInMemoryRepository`.
- `DevInMemoryResetService` orquesta el reset completo + resembrado de plantillas.
- `DevResetController` expone `POST /demo/dev/reset` protegido por `faltas.demo.reset.enabled`.
- `DevResetResponse` record con todos los campos del resumen de reset.
- Property `faltas.demo.reset.enabled=false` documentada en `application.yml`.

### Tests creados

- `DevInMemoryResetServiceTest` (13 tests unitarios)
- `DevResetControllerIT` (14 tests IT, con `@TestPropertySource`)
- `DevResetDisabledIT` (1 test IT: verifica 404 cuando deshabilitado)
- `DevResetGuardrailTest` (5 tests de guardrail estatico)

### Guardrails cumplidos

- No JDBC, no MariaDB, no SQL, no JPA/Hibernate.
- No escritura de archivos, no storage real.
- No endpoint reset productivo fuera de /demo.
- Idempotente: ejecutar N veces produce el mismo estado.
- `docs/spec-as-source` en raiz: False (confirmado).

---

## Proximo slice recomendado

**8F-6 ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â Auditoria frontend-ready de endpoints, payloads y flujo demo.**

Objetivo: revisar y auditar que todos los endpoints core esten listos para ser consumidos desde Angular: payloads correctos, errores bien tipados, CORS si aplica, flujos end-to-end navegables desde el frontend en desarrollo.

---

## 8F-6 - CERRADO

**Estado**: CERRADO

**Tests run**: 2323 (BUILD SUCCESS, 0 failures)

### Que se hizo

AuditorÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­a backend-only, frontend-ready, de los 3 endpoints demo.

**Correcciones:**
- Bug corregido en `DevResetControllerIT` test 8: campo `$.totalActas` era incorrecto, debÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­a ser `$.totalActasMock`.

**Nuevo:**
- `DemoCorsConfig` (`WebMvcConfigurer`): CORS habilitado para `/demo/**` y `/api/**`.
  - Property configurable: `faltas.demo.cors.allowed-origins=*` (default).
- `DemoContractIT`: 13 tests de contrato frontend-ready que validan:
  - Shape de campos clave en los 3 endpoints demo.
  - IDs de navegaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n en graph (actaId, documentoId, redaccionId).
  - Fields de presentaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n en dataset (codigo, titulo, bloqueEsperado).
  - GET /demo/dev/reset devuelve 405 (solo POST permitido).
  - POST /demo/dev/reset sin habilitar devuelve 404.
  - Endpoints GET funcionan sin reset habilitado.
  - docs/spec-as-source no existe en raÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­z del repo.
- `108-frontend-ready-demo.md`: documentaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n viva del flujo demo, payloads, errores y gap list.
- `05-api-core-endpoints.md` actualizado con detalle de payloads demo.

### Payloads auditados

| Endpoint | Estado | Campos frontend-ready |
|---------|--------|----------------------|
| GET /demo/documentos/graph | Listo | totalCasos, casosExitosos, completo, fhEjecucion, casos[].actaId/documentoId/redaccionId/storageKey/hashDocu |
| GET /demo/actas/dataset-funcional | Listo | totalActasMock, actas[].codigo/titulo/bloqueEsperado/bandejaEsperada/cerrableEsperado |
| POST /demo/dev/reset | Listo | ejecutado, modo, fhReset, repositoriosReseteados, plantillasRecreadas, casosDatasetFuncional |

### Gaps detectados (ver 108-frontend-ready-demo.md)

**Bloqueantes:**
- GAP-3: No hay GET /demo/actas/{codigo} para drill-down de acta individual.
- GAP-5: Actas del dataset son declarativas, no instancias reales en repo.
- GAP-8: No hay endpoint de health/demo-readiness.

**No bloqueantes:**
- GAP-1/2/4: Enums tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cnicos sin labels presentacionales.
- GAP-6: actasDemoDisponibles=0 por diseÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±o.
- GAP-7: No hay DTO global de error (patrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n Map<String,String> no documentado como contrato).

**Slices futuros:**
- GAP-9: PaginaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n en dataset.
- GAP-10: Labels/i18n exportados desde backend.
- GAP-11: GET /demo/actas/{codigo} con instancia real + eventos + docs.
- GAP-12: @ControllerAdvice global.
- GAP-13: CORS de producciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n por ambiente.

### Guardrails cumplidos

- No JDBC, no MariaDB, no SQL, no JPA/Hibernate.
- No storage real. No archivos fÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­sicos.
- No endpoint reset fuera de /demo.
- No cambio de semÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ntica funcional de los 31 casos.
- docs/spec-as-source en raÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­z: False (confirmado).

---

## PrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ximo slice recomendado

**8F-7 (o 8G) - Endpoint GET /demo/actas/{codigo} con instancia real + eventos + documentos.**

Alternativa: iniciar 8G - App portal ciudadano con los endpoints de consulta pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âºblica documentados en el plan maestro.


---

## Actualizacion 8F-7 (2026-07-03)

**BUILD SUCCESS ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â 1486 tests (antes: 1451).**

### Gaps cerrados en 8F-7

- **GAP-3 CERRADO**: GET /demo/actas/{codigo} implementado con instancia real via CasoUsoFuncionalRunner.
- **GAP-5 CERRADO**: Materializacion real: acta, timeline (eventos reales), documentos reales por flujo.
- GAP-11 CERRADO (subsumido en GAP-3+GAP-5): drill-down real implementado.

### Endpoint nuevo

- GET /demo/actas/{codigo} ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ 200 con shape completo (codigo, titulo, dataset, acta, timeline, documentos, demo, links).
- GET /demo/actas/dataset-funcional ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ aditivo: cada acta incluye detallePath.

### Gaps que siguen pendientes

| ID | Estado | Proximo slice |
|----|--------|--------------|
| GAP-7 | No bloqueante | 9.x |
| GAP-8 | Pendiente | 8F-8 |
| GAP-9 | Pendiente | 8G+ |
| GAP-10 | Pendiente | 8G+ |
| GAP-12 | No bloqueante | 9.x |
| GAP-13 | Infra | Etapa 9 |

### Proximo slice recomendado

**8F-8** - Endpoint GET /demo/health o GET /demo/status (GAP-8) ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â Health/demo-readiness para que el frontend verifique disponibilidad antes de cargar la UI.

Alternativa: iniciar **8G** - App portal ciudadano con los endpoints de consulta publica documentados en el plan maestro.

---

## Actualizacion 8F-8 (2026-07-03)

**BUILD SUCCESS ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 1502 tests (antes: 1486, +16).**

### Gaps cerrados en 8F-8

- **GAP-8 CERRADO**: GET /demo/health implementado con checks internos de dataset, plantillas, reset y endpoints.

### Endpoint nuevo

- GET /demo/health ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ 200 con shape completo {status, demoReady, fhEjecucion, versionDemo, dataset, documentos, reset, endpoints[], warnings[]}.
- demoReady=true cuando dataset.ready && documentos.ready.
-
eset.enabled=false (default seguro preservado).

### Archivos nuevos (8F-8)

- web/dto/DemoHealthResponse.java
- web/dto/DemoHealthDatasetDto.java
- web/dto/DemoHealthDocumentosDto.java
- web/dto/DemoHealthResetDto.java
- web/dto/DemoHealthEndpointDto.java
- pplication/demo/DemoHealthService.java
- web/DemoHealthController.java
- 	est/.../web/DemoHealthContractTest.java (16 tests)

### Estado de gaps post-8F-8

| ID | Estado | Proximo slice |
|----|--------|--------------|
| GAP-3 | CERRADO (8F-7) | - |
| GAP-5 | CERRADO (8F-7) | - |
| GAP-8 | CERRADO (8F-8) | - |
| GAP-7 | No bloqueante | 9.x |
| GAP-9 | Pendiente | 8G+ |
| GAP-10 | Pendiente | 8G+ |
| GAP-12 | No bloqueante | 9.x |
| GAP-13 | Infra | Etapa 9 |

### Proximo paso recomendado

**8G** - Portal ciudadano o primer slice Angular/demo frontend.

Demo completamente navegable: /demo/health ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ /demo/actas/dataset-funcional ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ /demo/actas/{codigo} ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ timeline/documentos ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ /demo/documentos/graph.

---

## Actualizacion 8F-9 (2026-07-03)

**Slice:** 8F-9 - Auditoria DELTA modelo MariaDB vs InMemory
**Tipo:** Auditoria y documentacion. Sin cambios funcionales Java.
**Build:** BUILD SUCCESS - 1502 tests (sin cambios respecto a 8F-8).

### Resultado de la auditoria

Documento creado: `backend/api-faltas-core/docs/spec-as-source/109-delta-modelo-mariadb-inmemory.md`

#### Resumen del delta

| Categoria | Cantidad |
|-----------|----------|
| Entidades InMemory alineadas con MariaDB | ~20 |
| Entidades InMemory con gaps parciales | 7 |
| Entidades nuevas en 8F que requieren tabla MariaDB | 3 |
| Tablas MariaDB sin implementacion InMemory | ~25 |
| Elementos solo demo/test/infra | 20+ |
| Decisiones pendientes para Pablo | 9 |
| Gaps reales del modelo MariaDB documentados | 12 |

#### Tablas nuevas de 8F que deben incorporarse al modelo MariaDB

1. `fal_documento_plantilla_contenido` - contenido versionado de plantilla
2. `fal_documento_plantilla_default` - resolucion automatica de plantilla por accion
3. `fal_documento_redaccion` - redaccion editable de documento

#### Decisiones pendientes para Pablo (ver documento 109)

1. FalActaFirmezaCondena: tabla separada o campos en fal_acta_fallo
2. FalPagoVoluntario y FalPagoCondena: mapeo al modelo de pagos real
3. FalActaParalizacion: implementar in-memory antes o directo a JDBC
4. Tipo de dato para campos JSON en nuevas tablas de 8F
5. FalActaFallo.fundamentos: TEXT o FK a fal_observacion
6. fal_persona / fal_persona_domicilio: cuando separar
7. FalNotificacion: UUID String o BIGINT
8. FalActa.tipoActa: convertir a SMALLINT enum
9. prioridad en fal_documento_plantilla_default: SMALLINT o INT

### Guardrails cumplidos

- No JDBC, no MariaDB, no SQL, no JPA/Hibernate.
- No storage real. No archivos fisicos.
- Sin cambios Java funcionales.
- docs/spec-as-source en raiz: False (guardrail OK).

### Proximo paso recomendado

**Pre-Slice 9 documental**: actualizar el modelo MariaDB con las 3 tablas nuevas de 8F y con las decisiones de Pablo.
**Slice 9** puede iniciarse despues de que Pablo apruebe las 9 decisiones.
**NO avanzar con frontend Angular ni JDBC hasta que Pablo revise y apruebe este delta.**


## ActualizaciÃƒÆ’Ã‚Â³n 8F-9-R1 (2026-07-03)

**Slice:** 8F-9-R1 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Cierre DELTA con decisiones de Pablo incorporadas
**Tipo:** ActualizaciÃƒÆ’Ã‚Â³n documental. Sin cambios funcionales Java.
**Build:** BUILD SUCCESS ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â 1502 tests (sin cambios respecto a 8F-8).

### Resultado

Documento 109-delta-modelo-mariadb-inmemory.md actualizado con las 9 decisiones de Pablo aprobadas y cerradas.

### Decisiones cerradas

| DecisiÃƒÆ’Ã‚Â³n | Resumen aprobado |
|---|---|
| D1 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalActaFirmezaCondena | No tabla separada. Agregar origen_firmeza SMALLINT NULL a fal_acta_fallo |
| D2 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalPagoVoluntario / FalPagoCondena | Mapear a fal_acta_obligacion_pago con tipo PAGO_VOLUNTARIO / CONDENA |
| D3 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalActaParalizacion | Implementar in-memory antes de JDBC |
| D4 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Campos JSON documentales | JSON nativo MariaDB 12.3.2 (no TEXT) |
| D5 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalActaFallo.fundamentos | fundamentos TEXT NULL directamente en fal_acta_fallo |
| D6 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalPersona / FalPersonaDomicilio | Implementar como entidades separadas in-memory antes de JDBC |
| D7 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalNotificacion PK | BIGINT AUTO_INCREMENT. Sin UUID alternativo |
| D8 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â FalActa.tipoActa | Convertir a enum TipoActa con cÃƒÆ’Ã‚Â³digo SMALLINT |
| D9 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â prioridad en plantilla_default | SMALLINT |

### PrÃƒÆ’Ã‚Â³ximo paso recomendado

**Pre-Slice 9 documental**: actualizar el modelo MariaDB (MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_*) con:
- Las 3 tablas nuevas de 8F (GAP-1): fal_documento_plantilla_contenido, fal_documento_plantilla_default, fal_documento_redaccion.
- Los campos aprobados: fal_acta_fallo.origen_firmeza SMALLINT NULL, fal_acta_fallo.fundamentos TEXT NULL, tipos JSON en tablas documentales.

**Slice 9** puede iniciarse despuÃƒÆ’Ã‚Â©s de que Pablo apruebe el modelo MariaDB actualizado con las nuevas tablas y campos.

**NO avanzar con frontend Angular ni JDBC hasta ese momento.**
---

## Actualizacion 8F-9-R2 (2026-07-03)

**Slice:** 8F-9-R2 - Limpieza de consistencia del DELTA MariaDB vs InMemory
**Tipo:** Correccion documental. Sin cambios funcionales Java.
**Build:** BUILD SUCCESS - 1502 tests (sin cambios respecto a 8F-8).

### Resultado

Documento 109-delta-modelo-mariadb-inmemory.md corregido para reflejar consistentemente las 9 decisiones cerradas en R1.

### Inconsistencias corregidas

1. **Seccion 8** - Tabla de tipos de dato actualizada:
   - variables_declaradas/snapshot/faltantes_json: TEXT o JSON -> JSON (APROBADA_D4)
   - prioridad: SMALLINT o INT -> SMALLINT (APROBADA_D9)
   - FalActaFallo.fundamentos: TEXT NULL o FK fal_observacion -> TEXT NULL (APROBADA_D5)
   - FalActaFirmezaCondena: interrogante -> campos en fal_acta_fallo + origen_firmeza SMALLINT NULL (APROBADA_D1)
   - FalPagoVoluntario/Condena: interrogante -> fal_acta_obligacion_pago con tipo (APROBADA_D2)
   - FalNotificacion.id: CHAR(36) o BIGINT -> BIGINT AUTO_INCREMENT (APROBADA_D7)
   - FalActa.tipoActa: SI-PABLO -> NO, APROBADA_D8
   - Titulo renombrado: "propuestos para aprobacion" -> "decisiones cerradas y pendientes reales"
   - Agregada subseccion 8.2 con unico pendiente real: FalActa.resultadoFinal

2. **Seccion 6** - Fila fundamentos en tabla Area Fallo: SEMANTICA_A_REVISAR -> APROBADA_D5

3. **Seccion 10** - GAP-2 y GAP-3: "Requiere decision de Pablo" -> textos de decision cerrada

4. **Seccion 11** - 22 Form Feed (0x0C) y 3 Vertical Tab (0x0B) corregidos en nombres de tablas/campos:
   - fal_acta_fallo, fh_firmeza, fal_acta_obligacion_pago, fal_pago_voluntario/condena
   - variables_declaradas/snapshot/faltantes_json
   - fundamentos, fal_persona, fal_persona_domicilio

5. **Seccion 13** - Checklist ampliado con items de validacion de R2

### Guardrails cumplidos

- No JDBC, no MariaDB, no SQL, no JPA/Hibernate.
- No cambios Java funcionales. No frontend.
- docs/spec-as-source en raiz: False (guardrail OK).

### Proximo paso recomendado

**Slice 8F-10 (documental)**: actualizar modelo MariaDB logico/documentado con las 3 tablas documentales de 8F y los campos aprobados. Sin SQL ejecutable ni JDBC.

---

## Actualizacion 8F-10 (2026-07-04)

**Slice:** 8F-10 - Actualizacion modelo MariaDB logico con tablas nuevas de 8F y decisiones D1-D9
**Tipo:** Documental. Sin cambios funcionales Java. Sin JDBC ni SQL ejecutable.
**Build:** BUILD SUCCESS - 1502 tests (sin cambios respecto a 8F-9-R2).

### Resultado

Modelo MariaDB logico `MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` actualizado con:

1. **Seccion 5.11 - `fal_documento_plantilla_contenido`**: tabla nueva con campos, indices, FK y reglas de vigencia/formato.
2. **Seccion 5.12 - `fal_documento_plantilla_default`**: tabla nueva con algoritmo de resolucion y regla de prioridad (D9).
3. **Seccion 5.13 - `fal_documento_redaccion`**: tabla nueva con estados BORRADOR/CONFIRMADA/REABIERTA/ANULADA y campos JSON (D4).
4. **`fal_acta_fallo.fundamentos TEXT NULL`** agregado (D5).
5. **`fal_acta_fallo.origen_firmeza SMALLINT NULL`** agregado con valores VENCIMIENTO_PLAZO_APELACION=1, APELACION_RECHAZADA=2 (D1).
6. **Seccion 0.8** - Tabla de D1-D9 incorporada al modelo MariaDB como referencia.

Documento 109-delta-modelo-mariadb-inmemory.md actualizado:

- GAP-1 (tres tablas nuevas): RESUELTO EN 8F-10.
- GAP-2 (D1 origen_firmeza): APLICADA EN 8F-10.
- GAP-9: fundamentos y origen_firmeza incorporados.
- Seccion 12 (Slice 8F-10): marcado como COMPLETADO 2026-07-04.

### Guardrails cumplidos

- No JDBC, no MariaDB real, no SQL ejecutable, no JPA/Hibernate.
- No cambios Java funcionales. No frontend.
- docs/spec-as-source en raiz: False (guardrail OK).

### Proximo paso recomendado

**Slice 9** puede iniciarse. El modelo MariaDB logico esta actualizado con las 3 tablas de 8F y las decisiones D1-D9.

Deudas activas conocidas pre-JDBC (de 8C-6E y DELTA, no bloqueantes para 8F-10):

- `num_politica`: agregar `fh_alta` + `id_user_alta` al DDL.
- `fal_acta`: agregar `resultado_firma_infractor SMALLINT NOT NULL`.
- `fal_documento`: `nro_docu VARCHAR(30)`; renombrar `requisito_firma -> tipo_firma_req`; agregar `plantilla_id`.
- `FalNotificacion.id`: cambiar de String a Long antes de Slice JDBC notificaciones (D7).
- `FalActa.tipoActa`: convertir a enum TipoActa con codigos SMALLINT antes de Slice JDBC actas (D8).


---

## Actualizacion 8F-10-R1 (2026-07-04)

**Slice:** 8F-10-R1 - Refinacion de secciones 5.11, 5.12 y 5.13 del modelo MariaDB
**Tipo:** Documental. Sin cambios funcionales Java. Sin JDBC ni SQL ejecutable.
**Build:** BUILD SUCCESS - 1502 tests (sin cambios respecto a 8F-10).

### Cambios en el modelo MariaDB logico

**Seccion 5.11 `fal_documento_plantilla_contenido`:**

- Eliminados campo `formato` y enum `FormatoPlantillaContenido`. Solo Markdown.
- Renombrados: `cuerpo_template` -> `cuerpo_markdown`, `encabezado_template` -> `encabezado_markdown`, `pie_template` -> `pie_markdown`.
- `variables_declaradas_json`: ahora NOT NULL; usar `[]` cuando no haya variables.
- Assets solo mediante expresiones controladas `{{asset.NOMBRE}}`.
- Regla: una version utilizada por una redaccion no debe modificarse.

**Seccion 5.12 `fal_documento_plantilla_default`:**

- Algoritmo de resolucion con niveles de especificidad (Nivel 2/1/0).
- Paso 12/13: resolucion de la version de contenido Markdown activa y vigente.

**Seccion 5.13 `fal_documento_redaccion`:**

- Estado `REABIERTA` eliminado. Estados finales: BORRADOR=1, CONFIRMADA=2, ANULADA=3.
- Rehacerse crea nueva fila BORRADOR vinculada via `redaccion_origen_id`.
- Agregados: `nro_revision SMALLINT NOT NULL UK`, `redaccion_origen_id BIGINT NULL FK` (self FK).
- Desdoblado: `contenido_editable` -> `contenido_base_markdown` + `contenido_editable_markdown`.
- JSON fields NOT NULL: `variables_snapshot_json` ({}), `variables_faltantes_json` ([]), `diagnostico_json` ({}).
- Agregados: `recursos_snapshot_json JSON NULL`, `fh_ultima_regeneracion`, `id_user_ultima_regeneracion`, `fh_anulacion`, `id_user_anulacion`, `motivo_anulacion`.

### Nuevos deltas InMemory vs. modelo MariaDB (a reconciliar antes de JDBC documental)

- `FalDocumentoPlantillaContenido`: tiene campo `formato` / enum `FormatoPlantillaContenido` eliminados del modelo.
- `FalDocumentoRedaccion`: tiene estado `REABIERTA`; campo `contenidoEditable` en lugar de `contenidoBaseMarkdown` + `contenidoEditableMarkdown`; faltan `nroRevision`, `redaccionOrigenId`, `recursosSnapshotJson`, `fhAnulacion`, etc.
- `DocumentoPlantillaDefaultService`: algoritmo sin niveles de especificidad.

### Guardrails cumplidos

- No JDBC, no MariaDB real, no SQL ejecutable, no JPA/Hibernate.
- No cambios Java funcionales. No frontend.

### Proximo paso recomendado

Antes del Slice JDBC documental (Slice 9-2 aproximado): reconciliar los tres deltas de InMemory listados arriba.


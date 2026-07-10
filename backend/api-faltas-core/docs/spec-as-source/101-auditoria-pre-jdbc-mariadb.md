# Auditoria pre-JDBC/MariaDB - Micro-slice 8C-6E

Fecha: 2026-07-02
Tipo: Auditoria y documentacion. Sin cambios funcionales Java.
Build base confirmado: 908/908 tests passing. BUILD SUCCESS.

---

## 1. Estado base auditado

Build: 908/908 tests passing. BUILD SUCCESS.
Modulo activo: backend/api-faltas-core
Persistencia: In-memory. MariaDB/JDBC: NO implementado.
Angular: NO tocado.
docs/spec-as-source historica: NO existe (Test-Path = False).
Spec viva: backend/api-faltas-core/docs/spec-as-source/

---

## 2. Slices cerrados hasta 8C-6D-1

Slices 1-6D-2: CERRADO - Flujo de acta, fallo, pago, apelacion, gestion externa
8A-3D, 8A-3D.1: CERRADO - Firmantes, habilitaciones, firma documental modelo productivo
8B-3: CERRADO - Talonarios y numeracion acta
8C-0A, 8C-0B, 8C-0D: CERRADO - Alineacion IDs, FalDocumento, catalogos base
8C-1: CERRADO - Enums documentales (TipoDocu, EstadoDocu, etc.)
8C-2: CERRADO - FalDocumentoPlantilla y FalDocumentoPlantillaFirmaReq
8C-3: CERRADO - Generacion FalDocumento desde plantilla
8C-4: CERRADO - FalDocumentoFirmaReq snapshot
8C-5A: CERRADO - Numeracion documental reusable
8C-5B: CERRADO - Envio a firma con numeracion automatica
8C-6A: CERRADO - ResultadoFirmaInfractor + evidencia FIRMA_OLOGRAFA_INFRACTOR
8C-6B-0: CERRADO - Diagnostico firma real documental
8C-6B-1: CERRADO - Firma real documental (FalDocumentoFirma refactorizado)
8C-6C-0: CERRADO - Diagnostico emision formal
8C-6C-1: CERRADO - Emision formal in-memory (hashDocu, fhGeneracion, DOCEMI)
8C-6D-0: CERRADO - Diagnostico documento escaneado
8C-6D-1: CERRADO - Documento escaneado/ADJUNTO + convalidacion firma olografa
8C-6E: CERRADO - Auditoria pre-JDBC/MariaDB (este documento)

---

## 3. Modelo MariaDB vs Java actual

### FalDocumento - ALINEADO

id BIGINT -> Long id: ALINEADO
id_acta BIGINT -> Long idActa: ALINEADO
tipo_docu SMALLINT -> TipoDocu tipoDocu: ALINEADO
estado_docu SMALLINT -> EstadoDocu estadoDocu: ALINEADO
nro_docu VARCHAR(30) -> String nroDocu: ALINEADO
id_talonario BIGINT NULL -> Long idTalonario: ALINEADO
nro_talonario_usado INT NULL -> Integer nroTalonarioUsado: ALINEADO
tipo_firma_req SMALLINT -> TipoFirmaReq tipoFirmaReq: ALINEADO
plantilla_id BIGINT NULL -> Long plantillaId: ALINEADO
storage_key VARCHAR(255) NULL -> String storageKey: ALINEADO
hash_docu VARCHAR(128) NULL -> String hashDocu: ALINEADO
fh_alta DATETIME(6) NOT NULL -> LocalDateTime fechaGeneracion: ALINEADO (fecha tecnica de alta)
fh_generacion DATETIME(6) NULL -> LocalDateTime fhGeneracion: ALINEADO (fecha de emision formal)
version_row: AUSENTE - PENDIENTE PARA JDBC (locking optimista; no critico in-memory)
rol_firma_req: REMOVIDO de FalDocumento - ALINEADO (pertenece a FalDocumentoFirmaReq)
mecanismo_firma_req: REMOVIDO de FalDocumento - ALINEADO (pertenece a FalDocumentoFirmaReq)

NOTA: fh_alta (fecha tecnica del registro) y fh_generacion (fecha de emision formal) son campos
distintos con semantica diferente. Ambos implementados y separados en FalDocumento.

### FalDocumentoPlantilla - ALINEADO

tipo_docu: ALINEADO - TipoDocu tipoDocu
accion_documental: ALINEADO - AccionDocumental accionDocumental
tipo_firma_req: ALINEADO - TipoFirmaReq tipoFirmaReq
si_requiere_numeracion: ALINEADO - boolean siRequiereNumeracion
momento_numeracion_docu: ALINEADO - MomentoNumeracionDocu momentoNumeracionDocu
si_genera_pdf: ALINEADO - boolean siGeneraPdf
si_notificable: ALINEADO - boolean siNotificable
si_seleccionable: ALINEADO - boolean siSeleccionable
vigencia (fh_vig_desde, fh_vig_hasta): ALINEADO
activo (siActiva): ALINEADO
audit (fh_alta, id_user_alta): ALINEADO

### FalDocumentoPlantillaFirmaReq - ALINEADO

seq_firma_req: ALINEADO - short seqFirmaReq
rol_firma_req: ALINEADO - short rolFirmaReq
mecanismo_firma_req: ALINEADO - Short mecanismoFirmaReq nullable
si_obligatoria: ALINEADO - boolean siObligatoria
si_activa: ALINEADO - boolean siActiva
vigencia y audit: ALINEADO

### FalDocumentoFirmaReq - ALINEADO

id_docu (PK compuesta): ALINEADO - Long idDocumento
seq_firma_req: ALINEADO - short seqFirmaReq
rol_firma_req: ALINEADO - short rolFirmaReq
mecanismo_firma_req: ALINEADO - nullable
orden_firma: ALINEADO - Short ordenFirma nullable
estado_firma_req: ALINEADO - EstadoFirmaReq estadoFirmaReq
si_obligatoria: ALINEADO - boolean siObligatoria
si_activa: ALINEADO - boolean siActiva
id_firmante_asig: ALINEADO - Long idFirmanteAsig nullable
ver_firmante_asig: ALINEADO - short verFirmanteAsig
fh_firma: ALINEADO - LocalDateTime fhFirma nullable
id_firma: ALINEADO - Long idFirma nullable

### FalDocumentoFirma - ALINEADO COMPLETO (refactor 8C-6B-1)

id BIGINT -> Long id: ALINEADO
documento_id -> Long idDocumento: ALINEADO
id_acta: AUSENTE - ALINEADO (eliminado en 8C-6B-1; derivable desde FalDocumento.idActa)
seq_firma_req -> short seqFirmaReq: ALINEADO
id_firmante -> Long idFirmante: ALINEADO
ver_firmante -> short verFirmante: ALINEADO
id_user_firma -> String idUserFirma: ALINEADO
rol_firmante -> short rolFirmante: ALINEADO
nombre_firmante -> String nombreFirmante: ALINEADO
tipo_firma SMALLINT -> TipoFirma tipoFirma: ALINEADO
estado_firma -> EstadoFirma estadoFirma: ALINEADO
hash_documento -> String hashDocumento: ALINEADO
referencia_firma_ext -> String referenciaFirmaExt: ALINEADO
storage_key -> String storageKey: ALINEADO
mensaje_error -> String mensajeError: ALINEADO
fh_firma -> LocalDateTime fhFirma: ALINEADO
fh_alta -> LocalDateTime fhAlta: ALINEADO
id_user_alta -> String idUserAlta: ALINEADO

No quedan campos naive (String id, String firmante, String tipoFirma).

### FalActa / ResultadoFirmaInfractor / Evidencias - ALINEADO

fal_acta.resultado_firma_infractor SMALLINT -> ResultadoFirmaInfractor resultadoFirmaInfractor en FalActa: ALINEADO
TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR codigo=6: ALINEADO
FalActaEvidencia: ALINEADO - entidad separada de FalDocumentoFirma
ActaEvidenciaRepository: ALINEADO - in-memory implementado
Separacion firma infractor / firma documental: ALINEADO - circuitos completamente separados

---

## 4. Enums auditados

### TipoDocu - ALINEADO COMPLETO (12 valores, codigos SMALLINT)
ACTA_INFRACCION(1), NOTIFICACION_ACTA(2), MEDIDA_PREVENTIVA(3), ACTO_ADMINISTRATIVO(4),
NOTIFICACION_ACTO_ADMINISTRATIVO(5), CONSTANCIA(6), ANEXO(7), NULIDAD(8),
RESOLUTORIO_BLOQUEANTE(9), INTIMACION_PAGO(10), INTIMACION_INCUMPLIMIENTO_PLAN(11), OTRO(12).

### EstadoDocu - ALINEADO COMPLETO (7 valores)
BORRADOR(1), EMITIDO(2), PENDIENTE_FIRMA(3), FIRMADO(4), ADJUNTO(5), ANULADO(6), REEMPLAZADO(7).

### EstadoFirma - ALINEADO COMPLETO (6 valores)
PENDIENTE(1), SOLICITADA(2), FIRMADA(3), RECHAZADA(4), ANULADA(5), ERROR(6).

### TipoFirmaReq - ALINEADO COMPLETO (6 valores)
NO_REQUIERE(0), FIRMA_INTERNA(1), FIRMA_INSPECTOR(2), FIRMA_AUTORIDAD(3), FIRMA_DIGITAL(4), FIRMA_MULTIPLE(5).
FIRMA_MIXTA: NO EXISTE. Guardrails en tests. Documentado en Javadoc.

### MomentoNumeracionDocu - ALINEADO COMPLETO (5 valores)
NO_APLICA(0), AL_CREAR(1), AL_EMITIR(2), AL_ENVIAR_A_FIRMA(3), AL_FIRMAR(4).
Todos los momentos implementados y funcionales en DocumentoService.

### ResultadoFirmaInfractor - ALINEADO COMPLETO (5 valores)
FIRMADA(1), SE_NIEGA_A_FIRMAR(2), INFRACTOR_NO_PRESENTE(3), IMPOSIBILITADO_PARA_FIRMAR(4),
NO_CAPTURADA_POR_FALLA_TECNICA(5).
NO_REQUERIDA: NO EXISTE. Guardrail en tests.

### TipoFirma - ALINEADO COMPLETO (4 valores)
DIGITAL(1), ELECTRONICA(2), OLOGRAFA(3), SISTEMA(4).
FIRMA_MIXTA: NO EXISTE en este enum. Guardrail en tests.

### TipoEventoActa - ALINEADO COMPLETO
Incluye: DOCGEN, DOCFIR, DOCEMI, DOCADJ - todos presentes y funcionales.
Prohibidos (ACTCER, PAGCON, DRVEXT, APELAC): no existen. Guardrails en EnumGuardrailTest.
Bloques legacy (D1_CAPTURA, D2_ENRIQUECIMIENTO, etc.): no existen. Rechazados por BloqueActual.deCodigo().

### EstadoFirmaReq - ALINEADO COMPLETO (5 valores)
PENDIENTE(1), FIRMADO(2), ANULADO(3), VENCIDO(4), REEMPLAZADO(5).
Enum separado de EstadoFirma (que aplica a fal_documento_firma).

---

## 5. Flujos documentales auditados

### Flujo documento desde plantilla sin firma - IMPLEMENTADO Y FUNCIONAL
Ruta: BORRADOR -> EMITIDO
Condiciones: tipoFirmaReq = NO_REQUIERE
siGeneraPdf=true: exige storageKey y hashDocu
Numeracion AL_EMITIR: numera antes de consolidar
Evento DOCEMI registrado
Tests: DocumentoEmisionFormalTest, DocumentoGeneracionDesdePlantillaTest

### Flujo documento desde plantilla con firma - IMPLEMENTADO Y FUNCIONAL
Ruta: BORRADOR -> PENDIENTE_FIRMA -> FIRMADO -> EMITIDO
enviarAFirma: materializa FalDocumentoFirmaReq, numera si AL_ENVIAR_A_FIRMA
Firma real: valida firmante + habilitacion + mecanismo + orden
Numeracion AL_FIRMAR: numera antes de firmar
Cierre FIRMADO cuando todos los obligatorios activos firmados
Emision formal posterior desde FIRMADO
Tests: DocumentoEnvioFirmaTest, DocumentoFirmaRealTest, DocumentoEmisionFormalTest

### Flujo numeracion documental - IMPLEMENTADO Y FUNCIONAL (todos los momentos)
NO_APLICA: falla si se intenta numerar - IMPLEMENTADO
AL_CREAR: auto-numera al crear desde plantilla - IMPLEMENTADO
AL_EMITIR: auto-numera antes de consolidar emision - IMPLEMENTADO
AL_ENVIAR_A_FIRMA: auto-numera antes de BORRADOR -> PENDIENTE_FIRMA - IMPLEMENTADO
AL_FIRMAR: numera antes de registrar firma real - IMPLEMENTADO
Tests: DocumentoNumeracionTest

### Flujo firma real documental - IMPLEMENTADO Y FUNCIONAL
registrarFirmaDocumental: valida firmante + version vigente + habilitacion activa
Valida mecanismo si req.mecanismoFirmaReq != null
Valida orden de firma via validarOrdenFirma
Crea FalDocumentoFirma con todos los campos del modelo productivo
Cumple FalDocumentoFirmaReq (estado FIRMADO + fhFirma + idFirma)
Cierra documento como FIRMADO cuando todos los obligatorios activos firmados
Tests: DocumentoFirmaRealTest

### Flujo emision formal - IMPLEMENTADO Y FUNCIONAL
emitirDocumento: BORRADOR -> EMITIDO (sin firma) o FIRMADO -> EMITIDO (con firma)
PENDIENTE_FIRMA no puede emitirse directamente
storageKey, hashDocu, fhGeneracion seteados
Evento DOCEMI registrado
Tests: DocumentoEmisionFormalTest

### Flujo escaneado/ADJUNTO - IMPLEMENTADO Y FUNCIONAL
incorporarDocumentoEscaneado: estado inicial ADJUNTO, storageKey/hashDocu/fhGeneracion obligatorios, evento DOCADJ
No requiere plantilla (plantillaId nullable para adjuntos)
Tests: DocumentoAdjuntoTest

### Flujo convalidacion firma escaneada - IMPLEMENTADO Y FUNCIONAL
Sin seqFirmaReq: trazabilidad simple, evento DOCFIR, documento permanece ADJUNTO
Con seqFirmaReq: crea FalDocumentoFirma con TipoFirma.OLOGRAFA, cumple requisito, si todos obligatorios firmados -> ADJUNTO -> FIRMADO
No inventa seqFirmaReq=0 (guardrail documentado)
Tests: DocumentoAdjuntoTest

---

## 6. DELTA reconciliado

### CERRADOS (implementados en slices anteriores)

IDs Long para actas: 8C-0B
FalDocumento alineado con MariaDB: 8C-0A, 8C-0D
Enums documentales (TipoDocu, EstadoDocu, etc.): 8C-1
FalDocumentoPlantilla y FirmaReq de plantilla: 8C-2
Generacion desde plantilla: 8C-3
FalDocumentoFirmaReq snapshot: 8C-4
Numeracion documental reusable: 8C-5A
Envio a firma con numeracion: 8C-5B
ResultadoFirmaInfractor + FIRMA_OLOGRAFA_INFRACTOR: 8C-6A
FalDocumentoFirma refactorizado (id Long, campos productivos): 8C-6B-1
TipoFirma enum (DIGITAL, ELECTRONICA, OLOGRAFA, SISTEMA): 8C-6B-1
idActa eliminado de FalDocumentoFirma: 8C-6B-1
hashDocu, fhGeneracion en FalDocumento: 8C-6C-1
DOCEMI en TipoEventoActa: 8C-6C-1
Emision formal (BORRADOR/FIRMADO -> EMITIDO): 8C-6C-1
DOCADJ en TipoEventoActa: 8C-6D-1
incorporarDocumentoEscaneado (estado ADJUNTO): 8C-6D-1
convalidarFirmaEscaneada + OLOGRAFA: 8C-6D-1
marcarFirmadoDesdeAdjunto (ADJUNTO -> FIRMADO): 8C-6D-1
Catalogos gestion externa alineados: 6D-0
FalFirmante + FalFirmanteVersion + Habilitaciones: 8A-3D, 8A-3D.1
FIRMA_MIXTA eliminado / FIRMA_MULTIPLE implementado: 8C-0D, 8C-1
TipoDocumento, EstadoDocumento, EstadoFirmaDocumento eliminados: 8C-1
Bloques legacy (D1_CAPTURA, etc.) rechazados: Guardrails en tests

### ACTIVOS PRE-JDBC (diferencias reales a resolver en Slice 9)

version_row ausente en FalDocumento: Locking optimista para JDBC; no critico in-memory
num_politica DDL fh_alta, id_user_alta: Java tiene estos campos; DDL productivo debe incluirlos
fal_acta.resultado_firma_infractor SMALLINT NOT NULL: Java alineado; DDL debe agregar la columna
fal_inspector_version: firma_storage_key, firma_hash, fh_firma_registrada ELIMINADOS del modelo (decision 8F-11B).
fal_acta_evidencia: seed tipo_evid=6: DDL debe incluir valor FIRMA_OLOGRAFA_INFRACTOR
fal_documento DDL: nro_docu VARCHAR(30); renombrar requisito_firma->tipo_firma_req; agregar plantilla_id
fal_documento_plantilla y fal_documento_plantilla_firma_req: Tablas nuevas por crear en DDL
Seeds catalogos documentales: tipo_firma_req, estado_docu, accion_documental, momento_numeracion_docu
fal_firmante y fal_firmante_version: DDL nuevas tablas (Secciones 1.7, 1.8 del modelo MariaDB)
fal_firmante_version_habilitacion: DDL nueva tabla (Seccion 1.9 del modelo MariaDB)
fal_documento_firma_req: DDL nueva tabla (Seccion 5.4 del modelo MariaDB)
fal_documento_firma: columnas id_firmante, ver_firmante, seq_firma_req: DDL a agregar
fal_firmante_version: rol_firmante nullable: DDL a actualizar
fal_acta_gestion_externa: monto_resultado DECIMAL(14,2) NULL: Verificar en DDL
FalObservacion / fal_observacion: No implementado; observaciones PAGAPR usan descripcion de evento transitoriamente
IDs UUID String en apelaciones, fallos, pagos, bloqueantes, gestiones externas:
  JDBC usara BIGINT AUTO_INCREMENT; requiere decision de migracion de tipos

### ROADMAP POST-JDBC (fuera de alcance del JDBC inicial)

OCR/IA: Extraccion asistida metadata; datos sugeridos; requiere validacion humana. No implementado.
PDF/storage real: In-memory usa storageKey/hashDocu simulados. JDBC inicial puede mantener simulado.
Proveedor firma digital real: TipoFirma.DIGITAL existe; proveedor externo: slice posterior.
Notificacion automatica desde emision: siNotificable no dispara. Slice posterior.
Angular / frontend: No tocar.
Integracion externa de documentos: Slice posterior.
Validacion criptografica real de hash: Slice posterior.
fal_inspector_version firma campos: Para modulo de firma inspector; no critico para JDBC inicial.
fal_observacion completo: Slice posterior.

### HISTORICO / NO APLICA (superados, solo referencia)

FALLO_ABSOLUTORIO, FALLO_CONDENATORIO_PAGADO, PAGO_VOLUNTARIO, PAGO_INFORMADO: Valores reemplazados.
PAGCON, APELAC, ACTCER, DRVEXT: Eventos prohibidos. Guardrails activos.
D1_CAPTURA, D2_ENRIQUECIMIENTO, D3_DOCUMENTAL, D4_NOTIFICACION, D5_ANALISIS: Aliases legacy. Rechazados.
GESTIONAR_CONDENA_FIRME: Eliminado. Reemplazado por GESTIONAR_PAGO_CONDENA.
FalDocumentoFirma naive (String id, String firmante, String tipoFirma): Reemplazado en 8C-6B-1.
TipoDocumento, EstadoDocumento, EstadoFirmaDocumento provisionales: Eliminados en 8C-1.
numeroVisible -> nroDocu: Renombrado en 8C-0A.
tipoDocumento -> tipoDocu, estado -> estadoDocu: Renombrados en 8C-0A.
requisito_firma -> tipo_firma_req: Renombrado en 8C-0D/8C-1.
rolFirmaReq, mecanismoFirmaReq en FalDocumento: Removidos. Estan en FalDocumentoFirmaReq.
Modos gestion externa viejos (REINGRESAR_A_ANALISIS, etc.): Renombrados en 6D-0.

---

## 7. Legacy auditado

### generarDocumento(...) - LEGACY ACTIVO CONTROLADO

Existe en DocumentoService y DocumentoController.
Comentado en codigo como "flujo legacy".
Usado en tests de flujo completo como metodo de setup para crear documentos de forma rapida.
Diagnostico: metodo simplificado sin plantilla ni validaciones completas. Necesario para tests de circuito completo.
Accion para JDBC: no eliminar antes de JDBC. Puede convivir. Evaluar depreciacion post-Slice 9.

### firmarDocumento(...) - LEGACY ACTIVO CONTROLADO

Existe en DocumentoService y DocumentoController.
Comentado en codigo como "flujo naive legacy - se mantiene para compatibilidad".
Usado en tests de flujo completo para avanzar estado de documentos.
Usa FirmarDocumentoCommand con String tipoFirma (no migrado a enum).
Diagnostico: implementacion naive. No valida firmante habilitado, no respeta orden, no actualiza FalDocumentoFirmaReq.
Coexiste con registrarFirmaDocumental (flujo correcto de 8C-6B-1).
Accion para JDBC: no eliminar antes de JDBC. Puede convivir temporalmente. Deprecar post-Slice 9.

### Nombres viejos en codigo Java - NINGUNO ACTIVO

Busquedas de numeroVisible, EstadoDocumento (clase), TipoDocumento (clase), EstadoFirmaDocumento en
src/main/java no retornaron referencias activas en modelos de dominio.

Ocurrencias detectadas son:
- DTOs de request HTTP que usan tipoDocumento como campo de request HTTP (valido)
- Comentarios historicos en DELTA y spec-as-source
- Tests de guardrail que verifican que los valores viejos NO existen

---

## 8. Roadmap fuera del JDBC inicial

OCR/IA: ROADMAP - Extraccion asistida metadata. Solo documentado en DELTA y spec.
PDF/storage real: ROADMAP - In-memory usa simulado. JDBC inicial puede continuar simulado.
Proveedor firma digital real: ROADMAP - TipoFirma.DIGITAL existe. Proveedor externo: posterior.
Notificacion automatica: ROADMAP - siNotificable no dispara. Slice posterior.
Angular: FUERA DE ALCANCE - No tocar.
fal_observacion: ROADMAP - PAGAPR usa descripcion de evento transitoriamente.
fal_inspector_version firma campos: ROADMAP - Para modulo de firma del inspector.

---

## 9. Repositorios y migracion a JDBC

ActaRepository -> InMemoryActaRepository -> fal_acta (BIGINT AUTO_INCREMENT; AtomicLong)
ActaEventoRepository -> InMemoryActaEventoRepository -> fal_acta_evento (Append-only)
ActaSnapshotRepository -> InMemoryActaSnapshotRepository -> fal_acta_snapshot (Upsert por id_acta)
ActaEvidenciaRepository -> InMemoryActaEvidenciaRepository -> fal_acta_evidencia (Insert)
ApelacionActaRepository -> InMemoryApelacionActaRepository -> fal_acta_apelacion (UUID String -> evaluar Long)
BloqueanteMaterialRepository -> InMemoryBloqueanteMaterialRepository -> fal_bloqueante_material (UUID String -> evaluar Long)
DependenciaRepository -> InMemoryDependenciaRepository -> fal_dependencia (BIGINT AUTO_INCREMENT)
DocumentoFirmaRepository -> InMemoryDocumentoFirmaRepository -> fal_documento_firma (BIGINT AUTO_INCREMENT)
DocumentoRepository -> InMemoryDocumentoRepository -> fal_documento (BIGINT AUTO_INCREMENT)
DocumentoPlantillaRepository -> InMemoryDocumentoPlantillaRepository -> fal_documento_plantilla (BIGINT AUTO_INCREMENT)
DocumentoFirmaReqRepository -> InMemoryDocumentoFirmaReqRepository -> fal_documento_firma_req (PK compuesta id_docu+seq)
FalloActaRepository -> InMemoryFalloActaRepository -> fal_acta_fallo (UUID String -> evaluar Long)
FirmanteRepository -> InMemoryFirmanteRepository -> fal_firmante + fal_firmante_version (BIGINT AUTO_INCREMENT)
FirmezaCondenaRepository -> InMemoryFirmezaCondenaRepository -> fal_acta_firmeza_condena (UUID String -> evaluar Long)
GestionExternaRepository -> InMemoryGestionExternaRepository -> fal_acta_gestion_externa (UUID String -> evaluar Long)
InspectorRepository -> InMemoryInspectorRepository -> fal_inspector + versiones (BIGINT AUTO_INCREMENT)
NormativaRepository -> InMemoryNormativaRepository -> fal_normativa_faltas (BIGINT AUTO_INCREMENT)
NotificacionRepository -> InMemoryNotificacionRepository -> fal_notificacion (UUID String; mantener CHAR(36))
PagoCondenaRepository -> InMemoryPagoCondenaRepository -> fal_pago_condena (UUID String -> evaluar Long)
PagoVoluntarioRepository -> InMemoryPagoVoluntarioRepository -> fal_pago_voluntario (UUID String -> evaluar Long)
TalonarioRepository -> InMemoryTalonarioRepository -> num_talonario+ambito+inspector+movimiento (BIGINT AUTO_INCREMENT; CRITICO transaccional)

---

## 10. Reglas transaccionales criticas para JDBC

Numeracion/talonarios: num_talonario_movimiento + fal_documento (nroDocu)
  Motivo: correlativo atomico; no puede haber huecos ni duplicados

Firma real documental: fal_documento_firma + fal_documento_firma_req + fal_documento (FIRMADO) + fal_acta_evento
  Motivo: consistencia firma -> req cumplido -> documento cerrado -> evento

Emision formal: fal_documento (EMITIDO, hashDocu, fhGeneracion) + fal_acta_evento
  Motivo: estado + datos de emision juntos

Incorporacion escaneado: fal_documento (ADJUNTO) + fal_acta_evento (DOCADJ)
  Motivo: documento + evento juntos

Convalidacion firma escaneada: fal_documento_firma + fal_documento_firma_req + fal_documento (estado) + fal_acta_evento
  Motivo: igual que firma real

Envio a firma con numeracion: fal_documento (nroDocu, PENDIENTE_FIRMA) + num_talonario_movimiento + fal_documento_firma_req + fal_acta_evento
  Motivo: numerar + cambiar estado + materializar req = atomico

Labrar acta: fal_acta + fal_acta_evento + fal_acta_snapshot + fal_acta_evidencia
  Motivo: alta de todas las piezas junta

Asignacion talonario inspector: num_talonario_inspector
  Motivo: concurrencia si multiples usuarios asignan simultaneamente

---

## 11. Recomendaciones de entrada a Slice 9

Orden recomendado:
1. Infraestructura: datasource MariaDB, JdbcClient, properties de conexion.
2. Catalogos y seeds: poblar catalogos productivos completos.
3. DDL correcciones: aplicar todos los cambios del DELTA sobre el DDL base (ver ACTIVOS PRE-JDBC).
4. Repositorios simples primero: DependenciaRepository, NormativaRepository, InspectorRepository, FirmanteRepository.
5. Talonarios: TalonarioRepository con transaccionalidad en numeracion. CRITICO.
6. Documentos y firma: DocumentoRepository, DocumentoPlantillaRepository, DocumentoFirmaReqRepository, DocumentoFirmaRepository.
7. Acta y eventos: ActaRepository, ActaEventoRepository, ActaSnapshotRepository.
8. Flujos transaccionales: habilitar y testear @Transactional en DocumentoService y ActaService.
9. Tests de integracion: agregar tests JDBC para los flujos criticos.

Reglas de implementacion:
- NO usar JPA/Hibernate, @Entity, @Table, @Id, JpaRepository, CrudRepository.
- USAR JdbcClient (Spring JDBC moderno).
- ESCRIBIR SQL explicito con parametros nombrados.
- Logica de negocio: permanece en servicios de aplicacion.
- Repositorios: sin logica de negocio.
- Transaccionalidad: @Transactional en servicios de aplicacion.
- Implementar JDBC reemplazando InMemory*Repository SIN modificar servicios ni tests de dominio.

---

## 12. Riesgos pendientes

IDs UUID String en apelaciones, fallos, pagos, gestiones, bloqueantes: MEDIO
  Mitigacion: evaluar BIGINT vs UUID al migrar JDBC. Interfaz de repositorio adaptable.

generarDocumento/firmarDocumento legacy en tests de flujo: BAJO
  Mitigacion: tests usan estos metodos para setup. No rompen JDBC. Deprecar post-Slice 9.

fal_inspector_version campos firma ausentes en Java: BAJO
  Mitigacion: no critico para JDBC inicial.

fal_observacion no implementado: BAJO
  Mitigacion: no bloquea JDBC.

Snapshot recalculado y concurrencia: MEDIO
  Mitigacion: en JDBC asegurar recalculo despues de cada transaccion.

Seeds de catalogos completos: MEDIO
  Mitigacion: los enums Java tienen codigos productivos; seeds deben coincidir exactamente.

---

## 13. Verificaciones finales del slice 8C-6E

docs/spec-as-source historica: NO EXISTE (False)
FIRMA_MIXTA funcional: CERO referencias funcionales. Solo guardrails y docs.
NO_REQUERIDA en ResultadoFirmaInfractor: NO EXISTE. Guardrail activo.
obsFirmaInfractor en FalActa: NO EXISTE. Guardrail activo.
EstadoDocumento, TipoDocumento, EstadoFirmaDocumento: NO EXISTEN en codigo Java activo.
JDBC/JPA/Hibernate implementado: NO. Solo referencias a JdbcClient en comentarios de repositorios.
OCR/IA Java funcional: NO IMPLEMENTADO. Solo nota roadmap.
Angular modificado: NO TOCADO.
Java funcional nuevo en este slice: NINGUNO. Solo auditoria y documentacion.
Build ejecutado: No ejecutado (solo documentacion). Base confirmada: 908/908 tests passing. BUILD SUCCESS.

---

## 14. Proximo slice recomendado

Slice 9 - MariaDB/JDBC con JdbcClient, sin JPA/Hibernate.

Entradas limpias:
- Modelo Java completamente alineado con MariaDB productivo.
- Enums productivos con codigos SMALLINT correctos.
- Interfaces de repositorio limpias y reemplazables.
- Flujos documentales completos y testeados in-memory.
- DELTA reconciliado.
- Reglas transaccionales documentadas.
- DDL corrections identificadas y documentadas.


---

## 15. Decisiones cerradas en Slice 9-0 (2026-07-02)

### PK interna: BIGINT

Las entidades internas productivas usaran PK BIGINT AUTO_INCREMENT.
No usar UUID String como PK interna salvo caso excepcional justificado.
UUID/String tecnico como clave alternativa solo cuando se requiera:
- correlacion offline
- integracion externa
- importacion / sincronizacion
- idempotencia

Frase definitiva:
Las entidades internas productivas usaran PK BIGINT para rendimiento, joins, FKs y consistencia
transaccional. Cuando se requiera correlacion offline, integracion externa o idempotencia, se
agregara un UUID tecnico/string como clave alternativa unica, sin reemplazar la PK BIGINT.

### Enums cerrados: enum Java + SMALLINT en MariaDB

Los catalogos cerrados del dominio NO se modelan como tablas fisicas administrables.
Se mantienen como enum Java con codigo short y columna SMALLINT en MariaDB.

No crear tablas fisicas para:
  EstadoDocu, TipoDocu, TipoFirmaReq, MomentoNumeracionDocu,
  TipoFirma, ResultadoFirmaInfractor, EstadoFirma, EstadoFirmaReq,
  TipoEventoActa, TipoEvidenciaActa,
  ClaseNumeracion, TipoTalonario, AlcanceTalonario, EstadoNumeroTalonario,
  MotivoAnulacionTalonario, EstadoAsignacionTalonario.

No crear seeds para esos enums (no hay tabla fisica que poblar).

### Descripciones SQL de enums cerrados

Si se requiere descripcion SQL legible para reporting o vistas, NO crear tabla catalogo.
Usar funciones SQL deterministas, vistas con CASE o CASE directo en consultas.
La fuente de verdad funcional sigue siendo el enum Java.

### Primer piloto JDBC

DocumentoPlantillaRepository con JdbcClient.

Motivo: bajo riesgo, prueba enums SMALLINT sin tablas de enum, prueba relacion plantilla -> firma req,
prepara GenerarDocumento JDBC, valida JdbcClient y perfiles de forma segura.

### Plan incremental Slice 9

Ver documento completo: backend/api-faltas-core/docs/spec-as-source/102-slice-9-estrategia-jdbc-mariadb.md

Resumen plan:
- 9-0: Estrategia. CERRADO.
- 9-1: Infraestructura JDBC base (MariaDB driver, JdbcClient, perfiles, test de conexion).
- 9-2: Piloto plantillas documentales JDBC.
- 9-3: Firmantes / habilitaciones JDBC.
- 9-4: Inspectores y dependencias JDBC.
- 9-5: Talonarios / numeracion JDBC transaccional.
- 9-6: FalDocumento + firma req + firma real JDBC.
- 9-7: Emision formal + adjuntos + eventos JDBC.
- 9-8: Actas core JDBC.
- 9-9: Repositorios UUID/String y retiro de legacy.

### Actualizacion R-08 (2026-07-09)
Hallazgo historico de 134
ow() directos en productivo: **cerrado**. Timestamps funcionales provienen de FaltasClock en aplicacion; MariaDB no debe imponer tiempos de negocio via CURRENT_TIMESTAMP salvo auditoria tecnica explicita.


### Contrato OCC InMemory - CIERRE-OCC-INMEMORY-PRE-R11 (2026-07-09)

#### InMemory (implementado)

- Mecanismo: `ConcurrentHashMap.compute()` por `idActa`.
- Semantica: compare-and-set atomico.
- Validacion: `existente.versionRow == esperado`.
- Escritura: `versionRow = esperado + 1`, retorna nueva copia defensiva.
- Fallo: `ConcurrenciaConflictoException` si version no coincide; mapa sin cambios.
- Garantia: lectura, comparacion y escritura en una unica seccion critica por clave.
- Sin lock global: actas con distinto `idActa` no se serializan entre si.

#### MariaDB futuro (equivalencia)

```sql
UPDATE fal_acta
SET    ...,
       version_row = version_row + 1
WHERE  id_acta     = :idActa
  AND  version_row = :versionEsperada;
-- rowsAffected == 0 -> throw ConcurrenciaConflictoException
```

#### Diagrama ventana de carrera (pre-fix)

```
Thread A: store.get(1) -> versionRow=0
Thread B: store.get(1) -> versionRow=0
Thread A: check 0!=0? NO -> pasa     <- punto critico (no atomico)
Thread B: check 0!=0? NO -> pasa     <- punto critico (no atomico)
Thread A: store.put(1, versionRow=1) -> exitos++
Thread B: store.put(1, versionRow=1) -> exitos++  <- BUG: dos ganadores
```

#### Diagrama con fix (compute)

```
Thread A: compute(1, lambda)
  lambda: existente.versionRow=0, acta.versionRow=0 -> match -> version=1 -> put
Thread B: compute(1, lambda) -> bloqueado hasta que A termine
  lambda: existente.versionRow=1, acta.versionRow=0 -> mismatch -> throw OCC
```

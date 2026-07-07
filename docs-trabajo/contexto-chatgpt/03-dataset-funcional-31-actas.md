# 03 — Dataset funcional: 31 actas mock

**Generado:** 2026-07-03
**Fuente canónica:** `DatasetFuncionalDominioCatalog.java` (`application/demo/`)

---

## Resumen

- **Total:** 31 actas mock funcionales.
- **Endpoint de consulta:** `GET /demo/actas/dataset-funcional`
- Cada acta declara: bloque esperado, situación administrativa, resultado final, bandeja, documentos esperados, eventos esperados.
- El catálogo es determinístico: se puede reconstruir en cualquier estado limpio.

---

## Tabla de las 31 actas

| # | Código | Descripción | Bloque | Resultado Final | Bandeja |
|---|--------|-------------|--------|-----------------|---------|
| 1 | ACT-001-LABRADA | Acta recién labrada en captura | CAPT | SIN_RESULTADO_FINAL | ACTAS_EN_ENRIQUECIMIENTO |
| 2 | ACT-002-EN-ENRIQUECIMIENTO | Captura completada, en enriquecimiento | ENRI | SIN_RESULTADO_FINAL | ACTAS_EN_ENRIQUECIMIENTO |
| 3 | ACT-003-DOC-PENDIENTE-FIRMA | En enriquecimiento con documento pendiente de firma | ENRI | SIN_RESULTADO_FINAL | PENDIENTE_FIRMA |
| 4 | ACT-004-PENDIENTE-NOTIFICACION | Documento firmado, pendiente de notificación | ENRI | SIN_RESULTADO_FINAL | PENDIENTE_NOTIFICACION |
| 5 | ACT-005-NOTI-ACTA-EN-CURSO | Notificación de acta enviada, esperando resultado | NOTI | SIN_RESULTADO_FINAL | EN_NOTIFICACION |
| 6 | ACT-006-ANAL-LISTA-FALLO | En análisis, lista para dictado de fallo | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 7 | ACT-007-PAGVOL-SOLICITADO | Pago voluntario solicitado | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 8 | ACT-008-PAGVOL-PENDIENTE-CONF | Pago voluntario informado, pendiente de confirmación | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_CONFIRMACION_PAGO |
| 9 | ACT-009-PAGVOL-CONFIRMADO | Cerrada por pago voluntario confirmado | CERR | PAGO_VOLUNTARIO_CONFIRMADO | CERRADAS |
| 10 | ACT-010-FALLO-ABS-DICTADO | Fallo absolutorio dictado, pendiente de firma | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_FIRMA |
| 11 | ACT-011-ABSUELTO-CERRADO | Absuelta y cerrada definitivamente | CERR | ABSUELTO | CERRADAS |
| 12 | ACT-012-FALLO-COND-DICTADO | Fallo condenatorio dictado, pendiente de firma | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_FIRMA |
| 13 | ACT-013-FALLO-COND-NOTIFICADO | Fallo condenatorio notificado | ANAL | SIN_RESULTADO_FINAL | PENDIENTES_FALLO |
| 14 | ACT-014-APELACION-PRESENTADA | Apelación presentada | ANAL | SIN_RESULTADO_FINAL | CON_APELACION |
| 15 | ACT-015-CONDENA-FIRME | Condena firme declarada | ANAL | CONDENA_FIRME | PENDIENTE_PAGO_CONDENA |
| 16 | ACT-016-PAGO-CONDENA-INFORMADO | Pago de condena informado, pendiente de confirmación | ANAL | CONDENA_FIRME | PENDIENTE_CONFIRMACION_PAGO_CONDENA |
| 17 | ACT-017-CONDENA-FIRME-PAGADA | Cerrada por condena firme pagada | CERR | CONDENA_FIRME_PAGADA | CERRADAS |
| 18 | ACT-018-GESTION-EXTERNA | Derivada a gestión externa | GEXT | CONDENA_FIRME | GESTION_EXTERNA |
| 19 | ACT-019-GESTION-EXTERNA-PAGO-EXTERNO | Pago externo por apremio registrado (PAGAPR) | ANAL | CONDENA_FIRME_PAGADA | PENDIENTE_ANALISIS |
| 20 | ACT-020-PARALIZADA | Acta paralizada temporalmente | ANAL | SIN_RESULTADO_FINAL | PARALIZADAS |
| 21 | ACT-021-BLOQUEANTE-ACTIVO | Bloqueante material activo | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 22 | ACT-022-ABSUELTO-CON-BLOQUEANTE | Absuelta con bloqueante activo (cierre diferido) | ANAL | ABSUELTO | PENDIENTE_ANALISIS |
| 23 | ACT-023-REDACCION-BORRADOR | Redacción de documento en BORRADOR | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 24 | ACT-024-PDF-MOCK-GENERADO | PDF mock generado (redacción CONFIRMADA) | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 25 | ACT-025-PRECONDICION-VIOLADA | Caso negativo: precondición violada (guardrail) | CAPT | SIN_RESULTADO_FINAL | ACTAS_EN_ENRIQUECIMIENTO |
| 26 | ACT-026-NOTIFICACION-NEGATIVA | Notificación negativa fallida (NOTNEG) | ANAL | SIN_RESULTADO_FINAL | PENDIENTE_ANALISIS |
| 27 | ACT-027-DOC-ADJUNTO-CONVALIDADO | Documento adjunto escaneado con firma convalidada | ENRI | SIN_RESULTADO_FINAL | ACTAS_EN_ENRIQUECIMIENTO |
| 28 | ACT-028-ABSOLUCION-FIRME-CERRADA | Cerrada por absolución firme | CERR | ABSUELTO | CERRADAS |
| 29 | ACT-029-REINGRESO-PARA-REVISION | Reingresada desde gestión externa para revisión | ANAL | CONDENA_FIRME | PENDIENTE_ANALISIS |
| 30 | ACT-030-PAGO-CONDENA-OBSERVADO | Pago de condena observado/rechazado (PCOOBS) | ANAL | CONDENA_FIRME | PENDIENTE_PAGO_CONDENA |
| 31 | ACT-031-PAGO-CONDENA-CON-DESCUENTO | Cerrada con pago de condena con descuento | CERR | CONDENA_FIRME_PAGADA | CERRADAS |

---

## Detalle por caso de uso principal

### ACT-001-LABRADA
- **Caso de uso:** `LabrarActa` (Slice 1)
- **Bloque:** CAPT | **Situación:** ACTIVA
- **Eventos:** ACTLAB
- **Documentos:** ninguno esperado
- **Suite:** `ActaFlujoCapturaFuncionalTest`

### ACT-002-EN-ENRIQUECIMIENTO
- **Caso de uso:** `CompletarCaptura` CAPT→ENRI (Slice 1)
- **Bloque:** ENRI | **Situación:** ACTIVA
- **Eventos:** ACTLAB, ACTCAP
- **Documentos:** ninguno todavía
- **Suite:** `ActaFlujoCapturaFuncionalTest`

### ACT-003-DOC-PENDIENTE-FIRMA
- **Caso de uso:** `GenerarDocumento → PENDIENTE_FIRMA` (Slice 1)
- **Bloque:** ENRI | **Situación:** ACTIVA
- **Documentos:** ACTA_INFRACCION (requiereFirmaDocumental=true)
- **Suite:** `ActaFlujoDocumentalFuncionalTest`

### ACT-004-PENDIENTE-NOTIFICACION
- **Caso de uso:** `FirmarDocumento → EnviarNotificacion` (Slice 1)
- **Bloque:** ENRI | **Situación:** ACTIVA
- **Documentos:** NOTIFICACION_ACTA (firmado, emitido)
- **Suite:** `ActaFlujoNotificacionFuncionalTest`

### ACT-005-NOTI-ACTA-EN-CURSO
- **Caso de uso:** `EnviarNotificacion → RegistrarResultado` (Slice 1)
- **Bloque:** NOTI | **Situación:** ACTIVA
- **Documentos:** NOTIFICACION_ACTA (enviado, pendiente acuse)
- **Suite:** `ActaFlujoNotificacionFuncionalTest`

### ACT-006-ANAL-LISTA-FALLO
- **Caso de uso:** `NotificacionPositiva → ANAL → DictarFallo` (Slice 3A)
- **Bloque:** ANAL | **Situación:** ACTIVA
- **Documentos:** ACTO_ADMINISTRATIVO (pendiente generación)
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-007-PAGVOL-SOLICITADO
- **Caso de uso:** `SolicitarPagoVoluntario` (Slice 2)
- **Bloque:** ANAL | **Situación:** ACTIVA
- **Documentos:** INTIMACION_PAGO (opcional, se genera al fijar monto)
- **Suite:** `ActaFlujoPagoVoluntarioFuncionalTest`

### ACT-008-PAGVOL-PENDIENTE-CONF
- **Caso de uso:** `InformarPagoVoluntario → ConfirmarPagoVoluntario` (Slice 2)
- **Bloque:** ANAL | **Situación:** ACTIVA
- **Documentos:** INTIMACION_PAGO (obligatorio en este estado)
- **Suite:** `ActaFlujoPagoVoluntarioFuncionalTest`

### ACT-009-PAGVOL-CONFIRMADO
- **Caso de uso:** `ConfirmarPagoVoluntario → CERRADA` (Slice 2)
- **Bloque:** CERR | **Situación:** CERRADA
- **Eventos:** PAGVSO, PAGVMF, PAGINF, PAGCNF, CIERRA
- **Suite:** `ActaFlujoPagoVoluntarioFuncionalTest`

### ACT-010-FALLO-ABS-DICTADO
- **Caso de uso:** `DictarFalloAbsolutorio` (Slice 3A)
- **Bloque:** ANAL | **Situación:** ACTIVA
- **Documentos:** ACTO_ADMINISTRATIVO + NOTIFICACION_ACTO_ADMINISTRATIVO (requieren firma)
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-011-ABSUELTO-CERRADO
- **Caso de uso:** `FalloAbsolutorio NOTIFICADO → ABSUELTO → CERRADA` (Slice 3A)
- **Bloque:** CERR | **Situación:** CERRADA | **Resultado:** ABSUELTO
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-012-FALLO-COND-DICTADO
- **Caso de uso:** `DictarFalloCondenatorio` (Slice 3A)
- **Bloque:** ANAL | **Situación:** ACTIVA
- **Documentos:** ACTO_ADMINISTRATIVO + NOTIFICACION_ACTO_ADMINISTRATIVO + INTIMACION_PAGO (opcional)
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-013-FALLO-COND-NOTIFICADO
- **Caso de uso:** `FalloCondenatorio NOTIFICADO → espera apelación o firmeza` (Slice 3A)
- **Bloque:** ANAL | **Bandeja:** PENDIENTES_FALLO
- **Eventos:** FALCON, DOCGEN, DOCFIR, NOTENV, NOTPOS
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-014-APELACION-PRESENTADA
- **Caso de uso:** `RegistrarApelacion (APEPRE)` (Slice 3B)
- **Bloque:** ANAL | **Bandeja:** CON_APELACION
- **Suite:** `ActaFlujoApelacionFuncionalTest`

### ACT-015-CONDENA-FIRME
- **Caso de uso:** `DeclararCondenaFirme (PLAVNC + CONFIR)` (Slice 4)
- **Bloque:** ANAL | **Resultado:** CONDENA_FIRME | **Bandeja:** PENDIENTE_PAGO_CONDENA
- **Suite:** `ActaFlujoPagoCondenaFuncionalTest`

### ACT-016-PAGO-CONDENA-INFORMADO
- **Caso de uso:** `InformarPagoCondena → ConfirmarPagoCondena` (Slice 5)
- **Bloque:** ANAL | **Bandeja:** PENDIENTE_CONFIRMACION_PAGO_CONDENA
- **Suite:** `ActaFlujoPagoCondenaFuncionalTest`

### ACT-017-CONDENA-FIRME-PAGADA
- **Caso de uso:** `ConfirmarPagoCondena → CONDENA_FIRME_PAGADA → CERRADA` (Slice 5)
- **Bloque:** CERR | **Resultado:** CONDENA_FIRME_PAGADA | **Situación:** CERRADA
- **Suite:** `ActaFlujoPagoCondenaFuncionalTest`

### ACT-018-GESTION-EXTERNA
- **Caso de uso:** `DerivarGestionExterna (EXTDER)` (Slice 6A)
- **Bloque:** GEXT | **Situación:** EN_GESTION_EXTERNA
- **Suite:** `ActaFlujoGestionExternaFuncionalTest`

### ACT-019-GESTION-EXTERNA-PAGO-EXTERNO
- **Caso de uso:** `RegistrarPagoExternoGestion (PAGAPR)` (Slice 6C)
- **Bloque:** ANAL | **Resultado:** CONDENA_FIRME_PAGADA
- **Nota:** cierre puede diferirse si hay bloqueantes activos
- **Suite:** `ActaFlujoGestionExternaFuncionalTest`

### ACT-020-PARALIZADA
- **Caso de uso:** `ParalizarActa (ACTPAR)` — Transversal
- **Bloque:** ANAL | **Situación:** PARALIZADA | **Bandeja:** PARALIZADAS
- **Suite:** `ActaFlujoParalizacionFuncionalTest`

### ACT-021-BLOQUEANTE-ACTIVO
- **Caso de uso:** `RegistrarBloqueanteMaterial` (Slice 7B)
- **Bloque:** ANAL | **Documentos:** RESOLUTORIO_BLOQUEANTE
- **Suite:** `ActaFlujoBloqueanteFuncionalTest`

### ACT-022-ABSUELTO-CON-BLOQUEANTE
- **Caso de uso:** Cierre diferido por bloqueante (Slice 7C)
- **Bloque:** ANAL | **Resultado:** ABSUELTO (pero bloqueante impide CERR)
- **Suite:** `ActaFlujoBloqueanteFuncionalTest`

### ACT-023-REDACCION-BORRADOR
- **Caso de uso:** `CrearRedaccionConContextoActa → BORRADOR` (Slice 8F-2)
- **Documentos:** ACTO_ADMINISTRATIVO en BORRADOR (sin PDF, sin storageKey)
- **Suite:** `ActaFlujoDocumentalFuncionalTest`

### ACT-024-PDF-MOCK-GENERADO
- **Caso de uso:** `ConfirmarRedaccionYGenerarDocumentoMock → CONFIRMADA` (Slice 8F-3)
- **Documentos:** ACTO_ADMINISTRATIVO con storageKey `mock://`, hashDocu `sha256-mock-`
- **Suite:** `ActaFlujoDocumentalFuncionalTest`

### ACT-025-PRECONDICION-VIOLADA
- **Caso de uso:** Caso negativo — guardrail `PrecondicionVioladaException`
- **Bloque:** CAPT (incorrecto para DictarFallo)
- **Suite:** `ActaFlujoFalloFuncionalTest` (casos negativos)

### ACT-026-NOTIFICACION-NEGATIVA
- **Caso de uso:** `RegistrarNotificacionNegativa (NOTNEG)` (Slice 1)
- **Bloque:** ANAL | **Documentos:** NOTIFICACION_ACTA (resultado negativo)
- **Suite:** `ActaFlujoNotificacionFuncionalTest`

### ACT-027-DOC-ADJUNTO-CONVALIDADO
- **Caso de uso:** `IncorporarDocumentoEscaneado + ConvalidarFirmaEscaneada (DOCADJ)`
- **Bloque:** ENRI | **Documentos:** ACTA_INFRACCION + CONSTANCIA (opcional)
- **Suite:** `ActaFlujoDocumentalFuncionalTest`

### ACT-028-ABSOLUCION-FIRME-CERRADA
- **Caso de uso:** `DictarFalloAbsolutorio → ABSUELTO → CERRADA` (Slice 3A completo)
- **Bloque:** CERR | **Resultado:** ABSUELTO | **Situación:** CERRADA
- **Suite:** `ActaFlujoFalloFuncionalTest`

### ACT-029-REINGRESO-PARA-REVISION
- **Caso de uso:** `ReingresarDesdeGestionExterna (EXTRET) → REINGRESO_PARA_REVISION` (Slice 6D-1)
- **Bloque:** ANAL | **Resultado:** CONDENA_FIRME (conservada)
- **Suite:** `ActaFlujoReingresoFuncionalTest`

### ACT-030-PAGO-CONDENA-OBSERVADO
- **Caso de uso:** `ObservarPagoCondena (PCOOBS)` (Slice 5)
- **Bloque:** ANAL | **Resultado:** CONDENA_FIRME | **Bandeja:** PENDIENTE_PAGO_CONDENA
- **Suite:** `ActaFlujoPagoCondenaFuncionalTest`

### ACT-031-PAGO-CONDENA-CON-DESCUENTO
- **Caso de uso:** `ConfirmarPagoCondena (variante con descuento) → CERRADA` (Slice 5 variante)
- **Bloque:** CERR | **Resultado:** CONDENA_FIRME_PAGADA | **Situación:** CERRADA
- **Nota GAP:** el dominio actual usa PCOCNF sin evento propio de descuento. Documentado.
- **Suite:** `ActaFlujoPagoCondenaFuncionalTest`

---

## Gaps técnicos documentados (casos no cubiertos todavía)

- FirmaReq completa (workflow de requisito de firma institucional con firmante asignado)
- Emisión formal numerada (FalDocumento.marcarEmitido / estado EMITIDO)
- Acta archivada (SituacionAdministrativa.ARCHIVADA / BloqueActual.ARCH)
- REINGRESO_PARA_CIERRE (bloqueado, reservado para slice futuro)
- Slice 9: persistencia JDBC/MariaDB (no implementada)

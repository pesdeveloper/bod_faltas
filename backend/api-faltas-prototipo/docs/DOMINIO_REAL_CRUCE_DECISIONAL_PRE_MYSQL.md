# Cruce decisional previo al modelo de tablas MySQL

**Fecha:** 2026-06-13
**Documento origen:** `DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md` + `DOMINIO_REAL_INVENTARIO_SPEC_PERSISTENCIA.md`
**Documento siguiente propuesto:** `DOMINIO_REAL_MODELO_TABLAS_MYSQL.md`
**Proposito:** Preparar material de decision para revision humana. No cierra el dominio.

---

## 1. Resumen ejecutivo decisional

### 1.1 Que ya esta suficientemente claro

- **Acta como agregado raiz:** definida en spec/dominio, con DDL `FalActa` (~64 columnas), SQL operativo y bandejas. Hay divergencia de estados entre prototipo y DDL pero el concepto es solido.
- **EventoAuditoria append-only:** `FalActaEvento` existe con estructura clara. El gap es de granularidad (19 vs 62 tipos), no de arquitectura.
- **Documento/Pieza:** spec, DDL (`FalDocumento`, `FalActaDocumento`, `FalDocumentoFirma`), bandejas y circuito de firma definidos. Divergencia de estados pero solucionable.
- **Notificacion:** spec, DDL (`FalNotificacion`, `FalNotificacionIntento`, `FalNotificacionAcuse`), bandejas. Distribucion en 3 tablas vs entidad unificada del prototipo: decision de implementacion, no de concepto.
- **SnapshotOperativo:** `FalActaSnapshot` confirmado como regenerable. Criterio de actualizacion incremental documentado en `spec/14-sql-operativo/08`.
- **Bandeja como proyeccion:** confirmado por spec bandejas y DDL (`CodBandeja VARCHAR(50)` en snapshot). No es fuente de verdad primaria.
- **MySQL como destino:** confirmado. Informix DDL es referencia historica/conceptual.
- **Portabilidad SQL operativo:** ninguna funcion Informix-especifica detectada en sql operativo. Solo tipos de dato y secuencias requieren ajuste sistematico.
- **Separacion pago voluntario / pago condena:** validada en prototipo. Son circuitos distintos con entidades distintas.
- **Portal infractor:** no expone estados internos. Solo documentos notificados, posibilidad de pago y apelacion.

### 1.2 Que esta cubierto por spec + DDL (Informix, referencia historica)

| Concepto | Tabla DDL | Spec |
|---|---|---|
| Acta/Expediente | `FalActa` | spec/01-dominio/01-acta.md |
| Evento de auditoria | `FalActaEvento` | spec/01-dominio/02-acta-evento.md |
| Evidencia digital | `FalActaEvidencia` | spec/13-ddl |
| Documento/Pieza | `FalDocumento`, `FalActaDocumento`, `FalDocumentoFirma` | spec/01-dominio/03-documento.md |
| Notificacion | `FalNotificacion`, `FalNotificacionIntento`, `FalNotificacionAcuse` | spec/01-dominio/04-notificacion.md |
| Snapshot operativo | `FalActaSnapshot` | spec/01-dominio/05-snapshot-operativo.md |
| Medida preventiva (referencial) | `FalMedidaPreventiva`, `FalActaMedidaPreventiva` | spec/01-dominio/06-medidas-y-liberaciones.md |
| Dependencia | `FalDependencia`, `FalDependenciaVersion` | spec/01-dominio/08-dependencia.md |
| Inspector | `FalInspector`, `FalInspectorVersion` | spec/01-dominio/09-inspector.md |
| Numeracion / Talonario | `NumPolitica`, `NumTalonario`, `NumTalonarioMovimiento` | spec/01-dominio/11-talonario-y-numeracion.md |
| Storage documental | `StorBackend`, `StorPolitica`, `StorObjeto` | spec/13-ddl/10-tablas-storage-documental.md |
| Normativa / Articulos | `FalNormativaFaltas`, `FalArticuloNormativaFaltas`, `FalActaArticuloInfringido` | spec/13-ddl |
| Satelites del acta | `FalActaTransito`, `FalActaVehiculo`, `FalActaContravencion`, etc. | spec/13-ddl/04-tablas-acta-y-satelites.md |

### 1.3 Que esta cubierto solo por prototipo validado (sin DDL ni spec formal)

| Concepto | Origen | Estado |
|---|---|---|
| PagoVoluntario como entidad propia | `PagoVoluntarioSupport`, `PagoInformadoSupport` | Solo flags en snapshot DDL |
| PagoCondena como entidad propia | `PagoCondenaSupport` | Solo flags en snapshot DDL |
| Fallo como decision juridica propia | `FalloPlazoApelacionSupport` | Solo como `TipoDocu=4` en DDL |
| Apelacion como entidad propia | `FalloPlazoApelacionSupport` | Solo `TipoEvt=13` + plazo en snapshot DDL |
| Paralizacion con motivo persistido | `ParalizacionReactivacionSupport` | Solo `SitAdmAct=6` en DDL; observacion volatil |
| Archivo con causales estructuradas | `ArchivoReingresoSupport` | Solo flag general en DDL |
| BloqueanteCierreMaterial separado | `CerrabilidadSupport` | Solo `FalActaMedidaPreventiva` parcial |
| GestionExterna con resultado trazable | `GestionExternaSupport` | Solo flags en snapshot DDL |
| LoteCorreo como entidad | `CorreoPostalNotificacionSupport` | Completamente ausente de spec y DDL |
| Portal/QR mecanismo de lookup | `PortalInfractorSupport` | Pendiente de diseno |

### 1.4 Que falta modelar

- Tablas propias para: `PagoVoluntario`, `PagoCondena`, `Fallo`, `Apelacion`, `Paralizacion`, `Archivo` (historico), `BloqueanteCierreMaterial`, `GestionExterna`, `LoteCorreo`, `acta_qr_acceso`.
- Campo `MotivoParalizacion` en snapshot o tabla de paralizacion.
- Causales estructuradas de archivo en snapshot o tabla de archivo.
- Ampliacion de catalogo de eventos: 19 tipos DDL vs 62 eventos prototipo.
- Mecanismo QR -> actaId para portal.
- Modelo de comprobante de pago (EM, RC, Cmte/Pref/Nro).
- Modelo de lote postal con trazabilidad real.

### 1.5 Conflictos que deben decidirse manualmente

1. **accionPendiente:** estado persistido vs. campo calculado vs. derivado de eventos.
2. **Fallo:** entidad separada (`acta_fallo`) vs. evento enriquecido vs. documento + metadatos.
3. **Apelacion:** tabla propia vs. evento enriquecido `TipoEvt=APELACION_INTERPUESTA`.
4. **TipoEvt:** ampliar enum (19 -> 62+) vs. catalogo de tabla vs. hibrido tipo + payload.
5. **PagoVoluntario/PagoCondena:** tablas propias vs. flags en snapshot + eventos enriquecidos.
6. **BloqueanteCierreMaterial:** tabla propia vs. extension de `FalActaMedidaPreventiva`.
7. **Paralizacion:** tabla historica vs. campo en snapshot vs. combinacion.
8. **LoteCorreo:** tabla propia vs. diferir vs. modelar como integracion externa.
9. **GestionExterna:** tabla propia vs. flags en snapshot + eventos enriquecidos.
10. **Portal/QR:** tabla de acceso vs. derivar de documento/notificacion existente.

### 1.6 Impacto de MySQL

- Tipos de dato: ajuste sistematico `LVARCHAR`->`TEXT`, `DATETIME YEAR TO SECOND`->`DATETIME`, `DATETIME YEAR TO DAY`->`DATE`.
- PKs: secuencias Informix (`NEXT VALUE FOR Seq*`) -> `BIGINT AUTO_INCREMENT` por tabla o API de secuencias MySQL 8.0.
- Booleanos: `SMALLINT(0/1)` puede seguir igual o migrar a `TINYINT(1)` / `BOOLEAN`.
- Case-sensitivity: los nombres de tabla `PascalCase` son case-insensitive en Windows pero case-sensitive en Linux; requiere convencion de deploy.
- PostGIS: las tablas `geo_gmat_*` requieren rediseno si se usa MySQL puro (sin extension PostGIS).
- JSON: disponible en MySQL 8.x; solo si se justifica para payload de eventos o metadatos.
- ENUM: evitar salvo decision explicita; preferir SMALLINT con catalogo documentado.

---

## 2. Clasificacion de agregados por nivel de soporte

### 2.1 Cubierto por spec + DDL

| Agregado | Evidencia encontrada | Archivos/Fuentes clave | Tablas DDL relacionadas | Reglas prototipo | Decision pendiente |
|---|---|---|---|---|---|
| **Acta** | spec/dominio, DDL ~64 cols, SQL operativo, bandejas | 01-acta.md, 02-tablas-nucleo, 04-sql-crud | `FalActa`, `FalActaEvento`, `FalActaEvidencia` | Cierre explicito; bandeja terminal; paralizacion transversal | Divergencia de estados entre prototipo y DDL; campos `resultadoFinal` / `motivoArchivo` a confirmar |
| **Documento** | spec/dominio, DDL completo, bandejas D5/D6 | 03-documento.md, 07-tablas-documentales | `FalDocumento`, `FalActaDocumento`, `FalDocumentoFirma` | Firma individual por pieza; firma de nulidad cierra expediente | Estados divergen (prototipo simplificado); tipos de documento a alinear |
| **Notificacion** | spec/dominio, DDL 3 tablas, bandejas D7 unificada | 04-notificacion.md, 08-tablas-notificacion | `FalNotificacion`, `FalNotificacionIntento`, `FalNotificacionAcuse` | Notificacion positiva de fallo fija resultado; superada por portal; reintento | Distribucion en 3 tablas vs modelo unificado; `resultadoNotificacion` distribuido en intento + acuse |
| **EventoAuditoria** | DDL `FalActaEvento`, spec append-only | 02-acta-evento.md, 02-tablas-nucleo | `FalActaEvento` | Append-only; ningun evento se edita ni borra | 19 tipos DDL vs 62 prototipo; decision de granularidad critica |
| **SnapshotOperativo** | DDL `FalActaSnapshot`, spec/05-snapshot, SQL/08 | 05-snapshot-operativo.md, 09-tablas-snapshot, 08-sql-proyecciones | `FalActaSnapshot` | Regenerable desde fuentes de verdad; actualizacion incremental por evento | Faltan campos: `MotivoParalizacion`, causales bloqueantes estructuradas de archivo |

### 2.2 Cubierto parcialmente (spec + DDL incompleto)

| Agregado | Evidencia encontrada | Archivos/Fuentes clave | Tablas DDL relacionadas | Reglas prototipo | Decision pendiente |
|---|---|---|---|---|---|
| **Fallo** | Como `TipoDocu=4` y `TipoEvt=12` en DDL; bandeja D5 confirma como pieza | 05-bandeja-pendientes-fallo.md, 07-tablas-documentales | `FalDocumento` (tipo 4), `FalActaEvento` (tipo 12) | Solo desde `PENDIENTE_ANALISIS`; genera pieza en firma; notificacion positiva fija resultado | Entidad separada vs documento enriquecido vs evento enriquecido |
| **Apelacion** | `TipoEvt=13`, `BloqueActual=7`, `FhVtoApelacion` en snapshot; bandeja D8 | 08-bandeja-con-apelacion.md, 02-tablas-nucleo | `FalActaEvento` (tipo 13), `FalActaSnapshot` | Solo con plazo abierto; bandeja D8 solo con apelacion real presentada | Tabla propia vs evento enriquecido; plazo vs actuacion |
| **BloqueanteCierreMaterial** | Parcial en `FalActaMedidaPreventiva`; conceptual en spec medidas; bandejas D11/D12 gap | 06-medidas-y-liberaciones.md, 04-satelites, 11-bandeja-archivo | `FalActaMedidaPreventiva` (parcial) | Resolucion documental y cumplimiento material son pasos distintos | Tabla propia `acta_bloqueante_cierre_material` vs extension de medida preventiva; causales en snapshot |
| **GestionExterna** | Flags en `FalActaSnapshot`; spec bloque de servicio; bandeja D9 | 06-servicios-gestion-externa.md, 09-snapshot, 09-bandeja | `FalActaSnapshot` (flags solo) | Derivacion requiere marca; resultado puede modificar monto; reingreso obligatorio | Tabla propia vs flags + eventos enriquecidos; campos de resultado a decidir |

### 2.3 Cubierto por spec pero no por DDL

| Agregado | Evidencia encontrada | Archivos/Fuentes clave | Tablas DDL | Reglas prototipo | Decision pendiente |
|---|---|---|---|---|---|
| **Paralizacion** | Estado `SitAdmAct=6`; spec menciona paralizacion; bandeja D10 exige motivo en snapshot | 10-bandeja-paralizadas.md, 02-tablas-nucleo | Solo valor `SitAdmAct=6`; sin tabla propia | Transversal; observacion debe persistirse; reactivacion vuelve a `PENDIENTE_ANALISIS` | Tabla historica `acta_paralizacion` vs campo `MotivoParalizacion` en snapshot |
| **Archivo** | `TipoCierreAct` parcial; bandeja D11 exige causales; spec menciona archivo | 11-bandeja-archivo.md, 02-tablas-nucleo | `TipoCierreAct` en snapshot (parcial) | Causales distinguen origen; archivo != cierre; reingreso posible | Tabla historica de archivo vs campos en acta vs eventos enriquecidos |

### 2.4 Cubierto por prototipo validado pero no por spec ni DDL

| Agregado | Evidencia encontrada | Archivos/Fuentes clave | Tablas DDL | Reglas prototipo | Decision pendiente |
|---|---|---|---|---|---|
| **PagoVoluntario** | Circuito completo en `PagoVoluntarioSupport` + `PagoInformadoSupport`; bandeja D3 confirma lugar | 03-bandeja-analisis.md, prototipo seccion 1.5 | Solo flags: `SiPagoVolunt`, `FhPagoVolunt`, `MontoActa`, `EstPagoAct` en snapshot | Solicitud desde cualquier bandeja; monto fijado por operador; confirmacion lleva a `PAGO_CONFIRMADO` | Tabla propia `acta_pago_voluntario` vs evento enriquecido + snapshot |
| **PagoCondena** | Circuito completo en `PagoCondenaSupport`; separacion validada | prototipo seccion 1.6 | Solo flags: `SiPagoTotal` en snapshot | Solo con `CONDENA_FIRME`; separacion estricta de pago voluntario | Tabla separada `acta_pago_condena` (casi obligatoria) |
| **LoteCorreo** | `CorreoPostalNotificacionSupport`; `loteId` en notificaciones del prototipo | prototipo seccion 2.11 | Completamente ausente | Agrupacion de envios postales; anulacion de lote; trazabilidad | Tabla propia vs diferir vs integracion externa |
| **Portal/QR** | `PortalInfractorSupport`; lookup QR pendiente | prototipo seccion 1.12 | Completamente ausente | Portal no expone estados internos; QR resuelve a actaId | Tabla de acceso vs derivar de documento/notificacion |

### 2.5 Solo proyeccion (no entidad, no tabla propia)

| Proyeccion | Descripcion | Fuente real |
|---|---|---|
| **bandejaActual** | Calculada desde `bloqueActual` + `EstProcAct` + `estaCerrada` + `motivoArchivo`; cacheada en `CodBandeja` de snapshot | `FalActa` + `FalActaEvento` + `FalActaSnapshot` |
| **subBandeja** | Filtro operativo calculado desde `accionPendiente` + `situacionPago` + `resultadoFinal` + `cerrable` | Estado del acta + proyecciones |
| **cerrabilidad** | Calculada por `CerrabilidadSupport`; nunca persistida directa | `resultadoFinal` + bloqueantes activos + `situacionPagoCondena` |
| **accionesDisponibles** | Calculadas en backend desde estado + predicados | Estado actual del acta |
| **pendientesBloqueantes** | Calculados desde bloqueantes sin cumplimiento material | Entidades de bloqueante |
| **Conteos de bandeja** | Calculados en tiempo de consulta desde snapshot | `FalActaSnapshot` |

### 2.6 Requiere decision humana antes de crear tabla

| Concepto | Por que requiere decision | Opciones en juego |
|---|---|---|
| **accionPendiente** | Hoy mapa volatil; puede ser campo persistido, campo derivado o marca de evento | Estado en acta / campo en snapshot / derivado de eventos |
| **Fallo** | Spec lo trata como `TipoDocu=4`; prototipo lo trata como decision juridica con monto y consecuencias | Entidad `acta_fallo` / documento enriquecido / hibrido |
| **GestionExterna** | Flags en snapshot cubren lo basico; trazabilidad de resultado exige algo mas | Tabla propia / evento enriquecido / ampliacion snapshot |
| **LoteCorreo** | Completamente ausente; puede ser entidad de infraestructura mas que de dominio | Tabla propia / diferir / integracion externa |

---

## 3. Cruce tabla candidata vs fuente

| Concepto de dominio | Tabla DDL existente | Tabla MySQL candidata futura | Fuente principal | Gaps detectados | Riesgo | Recomendacion para revision humana |
|---|---|---|---|---|---|---|
| Acta | `FalActa` | `fal_acta` | spec/dominio + DDL + prototipo | Divergencia de estados; `resultadoFinal` sin campo directo; `motivoArchivo` parcial | Bajo -- estructura solida | Alinear estados y agregar campos faltantes; revisar 64 columnas existentes vs necesidades reales |
| ActaEvento | `FalActaEvento` | `fal_acta_evento` | DDL + spec + prototipo | 19 tipos vs 62 eventos prototipo; estructura de payload no definida | Alto -- granularidad sin decidir | Decidir estrategia TipoEvt antes de disenar tabla; ver seccion 5 |
| Documento | `FalDocumento` + `FalActaDocumento` + `FalDocumentoFirma` | `fal_documento` + `fal_acta_documento` + `fal_documento_firma` | spec + DDL + bandejas | Estados divergen; tipos de documento a alinear; storage en `StorObjeto` | Bajo -- estructura solida | Alinear estados y tipos entre DDL y prototipo |
| Notificacion | `FalNotificacion` + `FalNotificacionIntento` + `FalNotificacionAcuse` | `fal_notificacion` + `fal_notificacion_intento` + `fal_notificacion_acuse` | spec + DDL + bandejas | `resultadoNotificacion` distribuido; `SUPERADA_POR_PORTAL` sin equivalente DDL; `loteId` sin campo | Bajo-medio | Agregar `loteId` y `referencia_externa` en intento; decidir si `PORTAL_CIUDADANO` cubre `PORTAL_INFRACTOR` |
| SnapshotOperativo | `FalActaSnapshot` | `fal_acta_snapshot` | spec/08 + DDL + bandejas | Sin `MotivoParalizacion`; sin causales bloqueantes de archivo; `resultadoFinal` sin campo directo | Medio -- campos faltantes criticos | Agregar campos antes de usarlo en produccion; ver decisiones 4.1 y 4.7 |
| PagoVoluntario | Solo flags en snapshot | `acta_pago_voluntario` (candidata) | Prototipo validado; bandeja D3 | Sin tabla ni spec formal; solo flags en snapshot | Alto -- sin precedente en spec | Decidir tabla propia vs evento enriquecido; ver decision 4.3 |
| PagoCondena | Solo flag `SiPagoTotal` en snapshot | `acta_pago_condena` (candidata) | Prototipo validado | Sin tabla ni spec formal; separacion de pago voluntario validada | Alto -- sin precedente en spec | Tabla separada casi obligatoria; ver decision 4.4 |
| Fallo | `FalDocumento` tipo 4 + `FalActaEvento` tipo 12 | `acta_fallo` (candidata) o `fal_documento` enriquecido | Prototipo + DDL parcial | Sin entidad separada en spec; monto condena sin campo propio | Medio -- dos opciones validas | Decidir entre entidad separada o documento enriquecido; ver decision 4.5 |
| Apelacion | `FalActaEvento` tipo 13 + `FhVtoApelacion` en snapshot | `acta_apelacion` (candidata) | Prototipo + DDL parcial | Sin tabla propia; canal y resultado sin modelo | Medio | Decidir si actuacion recursiva real justifica tabla; ver decision 4.6 |
| GestionExterna | Flags en `FalActaSnapshot` | `acta_gestion_externa` (candidata) | Prototipo + spec servicio | Sin tabla propia; trazabilidad de resultado exige algo | Medio | Decidir tabla propia vs evento enriquecido; ver decision 4.11 |
| Paralizacion | Solo `SitAdmAct=6` en eventos | `acta_paralizacion` (candidata) | Prototipo + spec estado | Sin tabla ni campo `MotivoParalizacion`; observacion volatil | Alto -- gap critico | Tabla historica casi obligatoria; ver decision 4.7 |
| Archivo | `TipoCierreAct` parcial en snapshot | `acta_archivo` (candidata) o campos en acta | Prototipo + spec parcial | Causales bloqueantes sin estructura; `motivoArchivo` parcial | Medio-alto | Decidir tabla historica vs campos en acta; ver decision 4.8 |
| BloqueanteCierreMaterial | `FalActaMedidaPreventiva` parcial | `acta_bloqueante_cierre_material` (candidata) | Prototipo + spec medidas | Sin tabla propia; dos pasos (documental + material) sin modelo separado | Alto | Tabla propia casi necesaria; ver decision 4.9 |
| LoteCorreo | Ausente | `lote_correo` (candidata) | Solo prototipo | Completamente sin spec ni DDL | Alto -- sin fundamento en spec | Decidir si se modela ahora o se difiere; ver decision 4.10 |
| Portal/QR | Ausente | `acta_qr_acceso` o `acta_acceso_portal` (candidata) | Solo prototipo | Mecanismo QR->actaId sin diseno | Alto | Decidir modelo de acceso antes de tabla; ver decision 4.12 |
| Comprobante de pago | Ausente | Campo en `acta_pago_voluntario` / `acta_pago_condena` | Prototipo (EM, RC, Cmte/Pref/Nro) | Estructura sin definir; tipos sin catalogo | Medio | Cruzar con spec de ingresos o sistema de cobros |
| Articulos/infracciones | `FalActaArticuloInfringido` + `FalNormativaFaltas` | `fal_acta_articulo_infringido` + normativa | spec + DDL | Auditoria de cambios en `FalActaArticuloAuditoria` | Bajo | Revisar alineacion de valores base y tarifario |
| Storage documental | `StorBackend` + `StorPolitica` + `StorObjeto` | `stor_backend` + `stor_politica` + `stor_objeto` | spec + DDL | No es de faltas; compartido | Bajo | Portabilidad directa a MySQL |

---

## 4. Decisiones criticas de modelado

### 4.1 Bandeja y snapshot

**Situacion actual:**
- `CodBandeja VARCHAR(50)` existe en `FalActaSnapshot` y es el discriminador operativo clave.
- El DDL define 9 valores de `BloqueActual`; la spec define 12 bandejas operativas.
- La diferenciacion requiere combinacion: `BloqueActual + EstProcAct + CodBandeja`.
- `FalActaSnapshot` es regenerable desde `FalActa` + `FalActaEvento` + entidades satelite.
- La spec SQL/08 confirma: reprocesar el snapshot multiples veces no produce degradacion.

**Que NO debe persistirse como verdad primaria:**
- `bandejaActual` derivada: se cachea en `CodBandeja` de snapshot, nunca como campo fuente.
- `subBandeja`: filtro operativo; se calcula en tiempo de consulta.
- Conteos de bandeja: calculados desde proyeccion real.
- `accionesDisponibles`: calculadas desde estado + predicados.

**Que SI debe cachearse en snapshot:**
- `CodBandeja`: discriminador operativo; campo existente en DDL.
- `EstProcAct`, `BloqueActual`, `SitAdmAct`: ya existen en snapshot.
- Flags de notificacion, pago, gestion externa: ya existen.
- **Pendiente:** `MotivoParalizacion` (ver 4.7), causales bloqueantes de archivo (ver 4.8).

**Decision a preparar para revision humana:**
- Confirmar que `CodBandeja` en snapshot cubre todas las 12 bandejas operativas.
- Definir valores de `CodBandeja` para las 12 bandejas (enum de strings legibles).
- Confirmar criterio de invalidacion de cache: por evento, no periodicamente.
- Confirmar que snapshot se actualiza en la misma transaccion que el evento.

### 4.2 AccionPendiente

**Situacion actual:**
- Hoy es un mapa volatil en memoria (`accionPendientePorActa`).
- El prototipo lo usa como discriminador de sub-bandeja operativa.
- En el dominio real puede modelarse como:
  - **Opcion A:** Campo persistido en `FalActa` o en `FalActaSnapshot`, actualizado en cada transicion relevante.
  - **Opcion B:** Campo calculado desde el ultimo evento significativo del historial.
  - **Opcion C:** Campo en snapshot derivado de combinacion de estados: `resultadoFinal` + `situacionPago` + `situacionPagoCondena` + bloqueantes activos.

**Ventajas/riesgos:**
- Opcion A: lectura directa sin recalculo, pero requiere actualizacion consistente en cada transicion.
- Opcion B: siempre consistente, pero recalculo puede ser costoso si el historial crece.
- Opcion C: hibrido natural para snapshot; calculo predefinido que puede cachearse.

**Recomendacion para revision humana:** Opcion C (campo en snapshot calculado desde estados y derivado en actualizacion incremental) parece alineada con el patron de `FalActaSnapshot`. No cerrar hasta cruzar con los predicados de `BandejaConsultaSupport`.

### 4.3 Pago voluntario

**Situacion actual:**
- La spec de bandejas confirma que D3 es el lugar operativo del pago voluntario.
- El snapshot tiene flags: `SiPagoVolunt`, `FhPagoVolunt`, `MontoActa`, `EstPagoAct`, `SiPlanPago`.
- El prototipo modela un circuito completo con 7 eventos especificos.
- No existe tabla propia en ningun nivel de spec.

**Opciones:**
- **Opcion A:** Campos en `FalActa` o `FalActaSnapshot`. Ventaja: simple. Riesgo: no cubre historial de intentos observados.
- **Opcion B:** Tabla `acta_pago_voluntario`. Ventaja: historial completo, separacion limpia. Riesgo: nueva entidad sin precedente en spec.
- **Opcion C:** Evento enriquecido + snapshot. Ventaja: todo en eventos. Riesgo: reconstruccion costosa para bandeja D3.

**Contexto adicional:**
- La regla del prototipo es: 1 acta puede tener 0..1 pago voluntario activo con historial de intentos observados.
- El comprobante de pago (EM, RC, Cmte/Pref/Nro) requiere estructura propia.
- La confirmacion externa (sistema externo) pendiente de diseno.

**Decision a preparar:** Opcion B (tabla propia) parece mas robusta para cubrir el circuito completo. Pendiente de confirmar campos minimos y relacion con `EstPagoAct` del snapshot.

### 4.4 Pago condena

**Situacion actual:**
- El snapshot solo tiene `SiPagoTotal` (insuficiente para modelar el circuito).
- El prototipo modela: `NO_APLICA` / `PENDIENTE` / `INFORMADO` / `CONFIRMADO` / `OBSERVADO`.
- Solo aplica cuando `resultadoFinal = CONDENA_FIRME`.
- El cierre del expediente depende de `situacionPagoCondena = CONFIRMADO`.
- La separacion de pago voluntario esta validada: son circuitos distintos con entidades distintas.

**Opciones:**
- **Opcion A:** Campos en `FalActa`. Simple pero mezcla circuitos.
- **Opcion B:** Tabla `acta_pago_condena`. Separacion limpia; historial de intentos; relacion con fallo/condena firme.
- La separacion es exigencia del prototipo: no colapsar con pago voluntario.

**Campos minimos candidatos:**
`pago_condena_id`, `acta_id`, `situacion_pago_condena`, `monto_condena`, `tipo_pago`, `fecha_informe`, `comprobante`, `fecha_confirmacion`, `observacion`, `consentimiento_registrado`, `fh_alta`, `id_user_alta`.

**Decision a preparar:** Tabla separada `acta_pago_condena` es casi obligatoria dado el circuito validado. La separacion con pago voluntario es una regla del dominio, no una preferencia de implementacion.

### 4.5 Fallo

**Situacion actual:**
- La spec de bandejas D5 usa "fallo" como la pieza a producir: `TipoDocu=4 (ACTO_ADMINISTRATIVO)`.
- El DDL registra el fallo como evento `TipoEvt=12 (ACTO_ADMINISTRATIVO_DICTADO)`.
- El prototipo trata el fallo como decision juridica propia: monto de condena, resultado final, apertura de plazo de apelacion.
- Puede haber fallo post gestion externa (nuevo fallo tras reingreso).
- La relacion 1 acta : 0..N fallos (prototipo) vs 1 acta : 1 acto administrativo (spec DDL) es un punto de tension.

**Opciones:**
- **Opcion A:** Fallo como documento enriquecido (`FalDocumento` tipo 4) + campos extra en evento. Alineado con spec DDL. Riesgo: el monto de condena y el resultado juridico no tienen lugar natural en `FalDocumento`.
- **Opcion B:** Tabla `acta_fallo` propia. Captura monto, tipo (absolutorio/condenatorio), operador que dicto, fecha. Permite N fallos historicos. Riesgo: nueva entidad fuera de spec.
- **Opcion C:** Hibrido: `FalDocumento` tipo 4 como pieza firmable + tabla `acta_fallo` con metadatos juridicos. El documento va a firma y notificacion; la entidad fallo captura la decision.

**Decision a preparar:** Opcion C parece equilibrada: mantiene el documento en el circuito documental existente y agrega la entidad fallo para los metadatos juridicos. Pendiente de confirmar si spec acepta multiples fallos por acta.

### 4.6 Apelacion

**Situacion actual:**
- La spec DDL tiene `TipoEvt=13 (APELACION_INTERPUESTA)` y `BloqueActual=7 (D7_APELACION)`.
- El snapshot tiene `FhVtoApelacion` para el plazo.
- La bandeja D8 es explicita: solo con apelacion formalmente presentada (no solo plazo habilitado).
- El prototipo modela: canal (portal/presencial), estado (presentada/resuelta), resultado (rechazada/aceptada_absuelve).
- La relacion es 1 acta : 0..1 apelacion activa.

**Opciones:**
- **Opcion A:** Evento enriquecido `TipoEvt=13` con payload de canal y resultado. Simple; sin tabla adicional. Riesgo: el resultado de la resolucion no tiene lugar claro en `FalActaEvento`.
- **Opcion B:** Tabla `acta_apelacion`. Cubre canal, estado, resultado, fechas, operador. Permite resolver la apelacion en un evento separado del registro. Riesgo: nueva entidad.
- **Opcion C:** Solo tabla si hay actuacion recursiva real (elevacion a instancia superior). El prototipo no modela elevacion externa.

**Campos minimos candidatos:**
`apelacion_id`, `acta_id`, `canal`, `fecha_registro`, `estado`, `resultado_resolucion`, `fecha_resolucion`, `id_user_resolucion`, `fh_alta`, `id_user_alta`.

**Decision a preparar:** Si la apelacion tiene vida propia (presentada / resuelta / con resultado), la Opcion B da mas claridad. Pendiente de confirmar si la elevacion a instancia externa requiere campo adicional.

### 4.7 Paralizacion

**Situacion actual:**
- Gap critico confirmado por dos fuentes: `DOMINIO_REAL_INVENTARIO_SPEC_PERSISTENCIA.md` y bandeja D10.
- La bandeja D10 exige mostrar "cual es el motivo general de paralizacion" en el snapshot.
- El snapshot actual no tiene campo `MotivoParalizacion`.
- El DDL no tiene tabla de paralizaciones.
- El prototipo almacena la observacion en un mapa volatil (`observacionParalizacionPorActa`).
- La reactivacion vuelve a `PENDIENTE_ANALISIS` con marca `REVISION_POST_REACTIVACION`.
- La relacion es 1 acta : N paralizaciones historicas.

**Campos minimos candidatos para tabla historica:**
`paralizacion_id`, `acta_id`, `motivo_paralizacion` (enum: ESPERA_DOCUMENTAL / ESPERA_INFORME_EXTERNO / ESPERA_OTRA_DEPENDENCIA / ESPERA_RESOLUCION_RELACIONADA / OTRO), `observacion TEXT`, `fecha_paralizacion`, `bandeja_previa VARCHAR(50)`, `accion_previa_preservada VARCHAR(100)`, `fecha_reactivacion`, `fh_alta`, `id_user_alta`.

**Campo adicional en snapshot candidato:**
`MotivoParalizacionAct SMALLINT` -- motivo de la paralizacion activa actual; NULL si no paralizada.

**Decision a preparar:**
- Tabla historica `acta_paralizacion` para trazabilidad completa de N paralizaciones.
- Campo `MotivoParalizacionAct` en snapshot para operacion de bandeja D10.
- Ambos son necesarios: tabla para historial, snapshot para consulta rapida.

### 4.8 Archivo

**Situacion actual:**
- Gap critico confirmado por bandeja D11.
- La bandeja D11 exige mostrar: motivo de archivo, medidas activas, pendientes materiales, condicion que falta para cerrar.
- El snapshot no tiene campo estructurado de causales bloqueantes ni motivo de archivo suficiente.
- `TipoCierreAct` en snapshot es parcial y mezcla archivo con cierre.
- El prototipo diferencia: `ARCHIVO_DESDE_ANALISIS_DIRECTO`, `ARCHIVO_POST_EVALUACION_VENCIMIENTO`, `NULIDAD`.
- Valores de `motivoArchivo` en spec DDL (`TipoCierreAct`): ARCHIVO_ADMINISTRATIVO, CIERRE_PAGO, CIERRE_RESOLUCION, CIERRE_GESTION_EXTERNA, CIERRE_OTRO. No coincide con prototipo.
- La relacion es: un acta puede archivarse, reingresarse y archivarse de nuevo.

**Opciones:**
- **Opcion A:** Campos en `FalActa`: `motivo_archivo`, `permite_reingreso`, `fh_archivo`. Simple para el caso de un solo archivo. Riesgo: no cubre historial.
- **Opcion B:** Tabla historica `acta_archivo`. Cubre N eventos de archivo con motivo, causales, fecha. Permite trazabilidad completa de reingreso -> archivo -> reingreso.
- **Opcion C:** Evento enriquecido `TipoEvt=15 (ARCHIVO_DISPUESTO)` con payload de causales + campos en acta para estado actual.

**Decision a preparar:**
- Aclarar si las causales bloqueantes (que impiden cerrar definitivamente) son parte del "archivo" o de `BloqueanteCierreMaterial`.
- Definir si `TipoCierreAct` se reemplaza o se extiende.
- Confirmar si el historial de N archivos es un requisito operativo o auditoria suficiente via eventos.

### 4.9 BloqueanteCierreMaterial

**Situacion actual:**
- Tres origenes: `MEDIDA_PREVENTIVA_ACTIVA`, `RODADO_SECUESTRADO`, `DOCUMENTACION_RETENIDA`.
- Cada origen tiene ancla documental en el expediente.
- El cierre requiere dos pasos distintos: resolucion documental + cumplimiento material efectivo.
- `FalActaMedidaPreventiva` cubre parcialmente solo el origen de medida preventiva.
- Las bandejas D11 y D12 exigen ver causales bloqueantes en snapshot: gap critico.
- Los bloqueantes documentales y materiales no se colapsan: regla del dominio.

**Campos minimos candidatos para tabla:**
`bloqueante_id`, `acta_id`, `origen` (enum: MEDIDA_PREVENTIVA_ACTIVA / RODADO_SECUESTRADO / DOCUMENTACION_RETENIDA), `estado_resolucion_documental` (PENDIENTE / RESUELTO), `estado_cumplimiento_material` (PENDIENTE / CUMPLIDO), `id_documento_resolvent BIGINT`, `fecha_constatacion`, `fecha_resolucion_documental`, `fecha_cumplimiento_material`, `fh_alta`, `id_user_alta`.

**Campos adicionales en snapshot candidatos:**
`SiBloqueanteMediaPrev SMALLINT`, `SiBloqueanteRodado SMALLINT`, `SiBloqueanteDocRet SMALLINT` -- flags de causales bloqueantes activas (derivados de tabla, cacheados en snapshot).

**Decision a preparar:**
- Tabla `acta_bloqueante_cierre_material` parece necesaria para cubrir los 3 origenes y los 2 pasos por origen.
- Confirmar si `FalActaMedidaPreventiva` se puede extender o si es mejor entidad separada.
- Agregar flags de causales en snapshot para bandeja D11/D12.

### 4.10 LoteCorreo

**Situacion actual:**
- Completamente ausente de toda fuente de spec, incluyendo bandejas.
- La bandeja D7 menciona canal "correo / carta documento" pero sin entidad propia.
- El prototipo modela: `loteId`, `fechaGeneracion`, `estado`, `referenciaExterna`, `cantidadItems`.
- Cada notificacion referencia su `loteId`.
- El canal `CanalNotif=3 (POSTAL)` existe en DDL de notificacion.

**Opciones:**
- **Opcion A:** Tabla `lote_correo` propia. Permite trazabilidad de lotes, anulacion de lote, relacion N notificaciones : 1 lote. Riesgo: nueva entidad sin precedente en spec.
- **Opcion B:** Campo `lote_id` en `FalNotificacionIntento` (ya referenciado en prototipo) sin tabla propia de lote. Riesgo: sin control del lote como entidad.
- **Opcion C:** Diferir la entidad LoteCorreo: modelar solo el campo `lote_id` en notificacion y diferir el modelo completo de lote a cuando haya integracion postal real.

**Decision a preparar:**
- Confirmar si la anulacion de lote es un requisito operativo real o solo demo.
- Si la integracion postal real tiene API propia, el lote puede ser concepto externo.
- Recomendacion: diferir tabla propia hasta tener definicion de integracion postal; agregar solo `lote_id VARCHAR(50)` en `FalNotificacionIntento`.

### 4.11 Gestion externa

**Situacion actual:**
- La spec de backend tiene un bloque de servicio dedicado (`06-servicios-gestion-externa.md`).
- El snapshot tiene: `SiGestionExt`, `TipoGestionExt`, `SiReingresoGestionExt`, `ResultadoGestionExt`, `FhVtoApremio`.
- El prototipo modela: tipo (APREMIO / JUZGADO_DE_PAZ), derivacion, resultado (MODIFICA_MONTO / ABSUELVE), reingreso.
- La relacion es 1 acta : N gestiones externas historicas.
- El resultado puede modificar el monto de condena: trazabilidad critica.

**Opciones:**
- **Opcion A:** Flags en snapshot suficientes para la operacion actual. No requiere tabla propia. Riesgo: no cubre historial de N derivaciones ni resultado con monto modificado.
- **Opcion B:** Tabla `acta_gestion_externa`. Cubre tipo, fecha derivacion, resultado, monto sugerido post gestion, fecha reingreso, accion post reingreso. Permite historial de N derivaciones.

**Campos minimos candidatos:**
`gestion_externa_id`, `acta_id`, `tipo` (APREMIO / JUZGADO_DE_PAZ), `fecha_derivacion`, `estado` (ACTIVA / CERRADA), `resultado_externo`, `monto_sugerido_post_gestion DECIMAL(12,2)`, `fecha_reingreso`, `accion_post_reingreso VARCHAR(100)`, `fh_alta`, `id_user_alta`.

**Decision a preparar:** Si el historial de N derivaciones es requisito operativo, la Opcion B es necesaria. Los flags del snapshot siguen siendo utiles para la vista operativa.

### 4.12 Portal / QR

**Situacion actual:**
- El mecanismo QR -> actaId esta pendiente de diseno.
- El portal no expone estados internos: `bandejaActual`, `accionPendiente`, marcas operativas.
- La visualizacion del portal puede generar una notificacion positiva (confirmar visualizacion).
- `PARALIZADAS` se expone como `EN_TRAMITE` al infractor.
- El canal `CanalNotif=6 (PORTAL_CIUDADANO)` ya existe en DDL.

**Opciones:**
- **Opcion A:** Tabla `acta_qr_acceso` o `acta_acceso_portal`. Almacena: codigo QR, actaId, fecha generacion, estado (activo/expirado), fecha primer acceso, conteo de accesos. Permite trazabilidad de accesos.
- **Opcion B:** Derivar desde documento o notificacion existente. El QR se genera asociado a un documento notificable; el lookup va de QR -> documento -> acta. Sin tabla adicional.
- **Opcion C:** Campo `codigo_qr VARCHAR(64)` en `FalActa` o en `FalDocumento` con indice. Simple para lookup directo.

**Decision a preparar:**
- Definir si el QR tiene ciclo de vida propio (expira, se renueva) o es permanente.
- Definir si el acceso via portal debe registrarse como evento de auditoria.
- La opcion B o C parece suficiente para el alcance inicial; la Opcion A agrega trazabilidad de accesos.

---

## 5. Eventos: gap 19 DDL vs eventos prototipo

### 5.1 Estrategia de ampliacion: opciones

Antes de la tabla de gap, hay tres estrategias posibles para resolver el gap 19 -> 62+:

| Estrategia | Descripcion | Ventaja | Riesgo |
|---|---|---|---|
| **A: Ampliar enum** | Agregar valores a `TipoEvt SMALLINT` hasta 62+ | Simple; sin cambio de modelo | Enum grande; mas dificil de mantener |
| **B: Catalogo de tabla** | Crear `fal_tipo_evento` como tabla con `id`, `codigo`, `descripcion` | Extensible sin DDL; FK referencial | Joins adicionales; mas complejo |
| **C: Hibrido generico + payload** | Mantener `TipoEvt` con tipos genericos (19 actuales) + columna `payload JSON` para contexto especifico | Menor cantidad de tipos; payload flexible | Consultas sobre payload mas complejas; JSON solo si MySQL 8.x |

**Decision no cerrada:** La tabla siguiente muestra el gap; la estrategia la decide el revisor humano.

### 5.2 Tabla de gap por grupo

| Grupo de evento | Eventos prototipo | Tipo DDL equivalente | Cubierto | Opcion MySQL futura | Decision pendiente |
|---|---|---|---|---|---|
| Ciclo del expediente | `ACTA_LABRADA` | `TipoEvt=1 (ACTA_LABRADA)` | Si | Mantener | -- |
| Ciclo del expediente | `ACTA_ENRIQUECIDA` | Sin equivalente | No | Agregar tipo o payload en tipo generico | Decidir |
| Ciclo del expediente | `ACTA_ENVIADA_A_NOTIFICACION` | Sin equivalente | No | Agregar tipo o mapear a tipo 3 | Decidir |
| Ciclo del expediente | `ACTA_CERRADA` | Parcial: `SiEvtCierre=1` en evento | Parcial | Agregar tipo explicito de cierre | Decidir |
| Ciclo del expediente | `ACTA_ARCHIVADA` | `TipoEvt=15 (ARCHIVO_DISPUESTO)` | Si | Mantener | -- |
| Ciclo del expediente | `ACTA_ARCHIVADA_POR_VENCIMIENTO` | `TipoEvt=15` (mismo tipo) | Parcial | Agregar subtipo o payload | Decidir |
| Ciclo del expediente | `ACTA_REINGRESADA` | `TipoEvt=16 (REINGRESO_DISPUESTO)` | Si | Mantener | -- |
| Ciclo del expediente | `ACTA_PARALIZADA` | Sin equivalente | No | Agregar tipo | Decidir |
| Ciclo del expediente | `ACTA_REACTIVADA` | Sin equivalente | No | Agregar tipo | Decidir |
| Ciclo del expediente | `ACTA_NULIDAD_REGISTRADA` | Parcial: `TipoEvt=5 (ACTA_ANULADA)` | Parcial | Distinguir nulidad de anulacion | Decidir |
| Documentos y firma | `DOCUMENTO_GENERADO` | `TipoEvt=6 (DOCUMENTO_GENERADO)` | Si | Mantener | -- |
| Documentos y firma | `DOCUMENTO_FIRMADO` | `TipoEvt=7 (DOCUMENTO_FIRMADO)` | Si | Mantener | -- |
| Documentos y firma | `FIRMA_CERRADA` | Sin equivalente | No | Agregar tipo o derivar de evento 7 con flag | Decidir |
| Notificaciones | `NOTIFICACION_PREPARADA` | `TipoEvt=8 (NOTIFICACION_EMITIDA)` | Parcial | Distinguir preparacion de emision | Decidir |
| Notificaciones | `NOTIFICACION_POSITIVA` | `TipoEvt=9 (NOTIFICACION_CONFIRMADA)` | Parcial | Mantener con subtipo en payload | Decidir |
| Notificaciones | `NOTIFICACION_NEGATIVA` | Sin equivalente | No | Agregar tipo | Decidir |
| Notificaciones | `NOTIFICACION_VENCIDA` | Sin equivalente | No | Agregar tipo | Decidir |
| Notificaciones | `REINTENTO_NOTIFICACION` | Sin equivalente | No | Agregar tipo | Decidir |
| Notificaciones | `NOTIFICACION_PORTAL_CONFIRMADA` | Parcial: `TipoEvt=9` + `OrigenEvt=5` | Parcial | Distinguir canal portal | Decidir |
| Notificaciones | `NOTIFICACION_SUPERADA_POR_PORTAL` | Sin equivalente | No | Agregar tipo o manejar solo en tabla notificacion | Decidir |
| Notificaciones | `ACUSE_NOTIFICADOR_MUNICIPAL` | Sin equivalente directo | No | Mapear a evento generico o agregar | Decidir |
| Notificaciones | `LOTE_CORREO_GENERADO` | Sin equivalente | No | Agregar tipo o diferir con LoteCorreo | Decidir |
| Notificaciones | `LOTE_CORREO_ANULADO` | Sin equivalente | No | Idem | Decidir |
| Fallo y apelacion | `FALLO_ABSOLUTORIO_DICTADO` | `TipoEvt=12` (no distingue tipo de fallo) | Parcial | Agregar tipo o payload | Decidir |
| Fallo y apelacion | `FALLO_CONDENATORIO_DICTADO` | `TipoEvt=12` (misma limitacion) | Parcial | Idem | Decidir |
| Fallo y apelacion | `RESULTADO_FINAL_ABSUELTO` | Sin equivalente | No | Agregar tipo | Decidir |
| Fallo y apelacion | `RESULTADO_FINAL_CONDENADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Fallo y apelacion | `APELACION_REGISTRADA` | `TipoEvt=13 (APELACION_INTERPUESTA)` | Si | Mantener | -- |
| Fallo y apelacion | `VENCIMIENTO_PLAZO_APELACION` | Sin equivalente | No | Agregar tipo | Decidir |
| Fallo y apelacion | `APELACION_RESUELTA` | Sin equivalente | No | Agregar tipo | Decidir |
| Fallo y apelacion | `ABSOLUCION_MARCADA_DIRECTO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_VOLUNTARIO_SOLICITADO` | `TipoEvt=11 (PAGO_REGISTRADO)` (muy generico) | No | Agregar tipos especificos | Decidir |
| Pagos | `MONTO_PAGO_VOLUNTARIO_FIJADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_INFORMADO_REGISTRADO` | `TipoEvt=11` (muy generico) | Parcial | Agregar tipo | Decidir |
| Pagos | `PAGO_INFORMADO_CONFIRMADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_INFORMADO_OBSERVADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_VOLUNTARIO_VENCIDO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_CONDENA_INFORMADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `PAGO_CONDENA_CONFIRMADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Pagos | `CONDENA_CONSENTIDA_Y_PAGO_REGISTRADO` | Sin equivalente | No | Agregar tipo | Decidir |
| Bloqueantes materiales | `CONSTATACION_MATERIAL_TEMPRANA` | Sin equivalente | No | Agregar tipo | Decidir |
| Bloqueantes materiales | `MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO` | `TipoEvt=17 (MEDIDA_PREVENTIVA_APLICADA)` | Parcial | Distinguir posterior de labrado | Decidir |
| Bloqueantes materiales | `ORIGEN_BLOQUEANTE_RECONOCIDO` | Sin equivalente | No | Agregar tipo o absorber en otro | Decidir |
| Bloqueantes materiales | `RESOLUCION_DOCUMENTAL_BLOQUEANTE` | Sin equivalente | No | Agregar tipo | Decidir |
| Bloqueantes materiales | `CUMPLIMIENTO_MATERIAL_BLOQUEANTE` | `TipoEvt=18 (MEDIDA_PREVENTIVA_LEVANTADA)` (parcial) | Parcial | Extender para cubrir rodado y documentacion | Decidir |
| Gestion externa | `DERIVADO_A_APREMIO` | `TipoEvt=14 (DERIVACION_EXTERNA)` (generico) | Parcial | Agregar subtipo o payload | Decidir |
| Gestion externa | `DERIVADO_A_JUZGADO_DE_PAZ` | `TipoEvt=14` (mismo) | Parcial | Idem | Decidir |
| Gestion externa | `APREMIO_*` (5 variantes) | Sin equivalente especifico | No | Agregar tipos o payload en tipo generico de reingreso | Decidir |
| Gestion externa | `JUZGADO_*` (3 variantes) | Sin equivalente especifico | No | Idem | Decidir |
| Portal infractor | `VISUALIZACION_PORTAL` | Sin equivalente | No | Agregar tipo o solo registrar en tabla de acceso | Decidir |
| Portal infractor | `APELACION_PORTAL_REGISTRADA` | `TipoEvt=13` + `OrigenEvt=5` | Parcial | Mantener con origen portal | Decidir |
| Portal infractor | `PAGO_VOLUNTARIO_PORTAL_*` | Sin equivalente | No | Agregar tipos o absorber en tipos de pago | Decidir |

**Resumen de gap:**
- Cubiertos exactos: 7 de 62 (ACTA_LABRADA, ARCHIVO_DISPUESTO, REINGRESO_DISPUESTO, DOCUMENTO_GENERADO, DOCUMENTO_FIRMADO, APELACION_INTERPUESTA, parcial de NOTIFICACION_CONFIRMADA).
- Cubiertos parcialmente: ~12.
- Sin equivalente: ~43.

**Conclusion:** Cualquiera de las tres estrategias requiere ampliar el modelo de eventos. La decision de granularidad debe tomarse antes de disenar la tabla `fal_acta_evento` en MySQL.

---

## 6. MySQL: implicancias para el modelo futuro

### 6.1 Que se puede trasladar conceptualmente sin cambios

| Concepto | Estado |
|---|---|
| Modelo relacional (tablas, columnas, FKs logicas) | Portable directo |
| `INT` y `SMALLINT` simples | Portable |
| `DECIMAL(n,m)` para montos y coordenadas | Portable |
| `CHAR(n)` para GUIDs y codigos fijos | Portable |
| `VARCHAR(n)` para nombres y textos cortos | Portable |
| PKs simples y compuestas | Portable |
| Patron de versionado referencial (tabla raiz + tabla version) | Portable |
| Modelo de evento append-only | Portable |
| Convenciones de nombres de tablas (prefijos `fal_`, `num_`, `stor_`) | Portable; adaptar `PascalCase` a `snake_case` si es convencion MySQL del equipo |
| `StorageKey VARCHAR(64)` como PK natural | Portable directo (sin secuencia) |
| SQL operativo conceptual (spec/14-sql-operativo/) | Portable; ningun acoplamiento Informix adicional detectado |

### 6.2 Que requiere adaptacion fisica

| Tipo Informix | Equivalente MySQL | Notas |
|---|---|---|
| `LVARCHAR` | `TEXT` o `MEDIUMTEXT` | Segun tamano real esperado; `TEXT` hasta 65.535 bytes; `MEDIUMTEXT` hasta 16MB |
| `DATETIME YEAR TO SECOND` | `DATETIME` | MySQL `DATETIME` es `YYYY-MM-DD HH:MM:SS`; equivalente exacto |
| `DATETIME YEAR TO DAY` | `DATE` | Equivalente exacto |
| `INT8` | `BIGINT` | Semantica identica; cambio de nombre |
| Secuencias Informix (`NEXT VALUE FOR SeqXxx`) | `BIGINT AUTO_INCREMENT` por tabla | O `CREATE SEQUENCE` si MySQL 8.0+ y se prefiere; la logica no cambia |
| `SMALLINT` como booleano (0/1) | `TINYINT(1)` o `BOOLEAN` | Funciona igual; convencion del equipo |

### 6.3 JSON: solo si se justifica

- MySQL 8.x soporta `JSON` con validacion y funciones de acceso (`JSON_EXTRACT`, `->>`).
- Uso recomendado solo si: payload de evento heterogeneo (estrategia hibrido de TipoEvt), metadatos de storage adicionales, o configuracion flexible de politicas.
- No usar `JSON` para estados de dominio que deben ser consultables con SQL simple.
- No usar `JSON` para campos que participan en `WHERE`, `ORDER BY` o indices frecuentes.

### 6.4 ENUM: evitar salvo decision explicita

- MySQL `ENUM` tiene comportamiento de ALTER TABLE costoso en tablas grandes.
- Preferir `TINYINT` o `SMALLINT` con catalogo documentado en comentario o tabla de referencia.
- Si se usa `ENUM`: solo para valores verdaderamente fijos y conocidos en tiempo de DDL.

### 6.5 Charset y collation

- Estado actual: no definido en spec ni en decisiones previas.
- **Pendiente de decision:** confirmar `utf8mb4` con collation `utf8mb4_unicode_ci` o `utf8mb4_0900_ai_ci` (MySQL 8.0+ por defecto).
- `utf8mb4` es obligatorio para soporte completo de Unicode (incluyendo emojis si se usan en observaciones).
- La collation afecta comparaciones de strings y ordenamiento; debe ser consistente en toda la base.

### 6.6 Motor de tabla

- `InnoDB`: obligatorio para soporte de FK, transacciones `COMMIT`/`ROLLBACK` y `SELECT ... FOR UPDATE`.
- No usar `MyISAM` ni otros motores para tablas de dominio.

### 6.7 Case-sensitivity en nombres de tabla

- En Windows: MySQL es case-insensitive por defecto (`lower_case_table_names=1`).
- En Linux: MySQL es case-sensitive por defecto.
- **Pendiente de decision:** definir convencion de deploy y configuracion `lower_case_table_names` antes de crear tablas en produccion.
- **Recomendacion:** usar `snake_case` en minusculas para todos los nombres de tabla y columna; evita el problema en cualquier plataforma.

### 6.8 PKs: estrategia recomendada

- `BIGINT AUTO_INCREMENT` por tabla: simple, estandar, sin dependencias entre tablas.
- Alternativa: secuencias MySQL 8.0+ (`CREATE SEQUENCE`) si se necesita numeracion global o rango garantizado.
- Las secuencias Informix existentes mapean 1:1 a `BIGINT AUTO_INCREMENT` en MySQL.
- Tablas con PK compuesta (versionado): mantener PK compuesta; no agregar columna de autoincrement innecesaria.

### 6.9 PostGIS / Geoespacial

- Las tablas `geo_gmat_*` usan PostGIS con `EPSG:22195`. No son del modulo faltas.
- MySQL 8.x tiene soporte geoespacial nativo (`GEOMETRY`, `POLYGON`, `LINESTRING`, `POINT`).
- Si se migran las tablas `geo_gmat_*` a MySQL: las funciones PostGIS no son identicas a las de MySQL Spatial.
- **Fuera del alcance del modulo faltas:** depende del equipo territorial. Solo impacta faltas si los lookups geograficos de domicilio del infractor lo requieren.

---

## 7. Insumo para `DOMINIO_REAL_MODELO_TABLAS_MYSQL.md`

### 7.1 Tablas casi seguras

Estas tablas tienen soporte solido de spec + DDL y el modelo conceptual es claro. El trabajo es adaptar DDL Informix a MySQL con ajustes de tipo y sintaxis.

| Tabla candidata MySQL | Por que existe | De donde surge | Decision necesaria | Prioridad |
|---|---|---|---|---|
| `fal_acta` | Agregado raiz del expediente | spec/dominio + DDL `FalActa` + prototipo | Alinear estados (`resultadoFinal`, `motivoArchivo`) y agregar campos faltantes | Alta |
| `fal_acta_evento` | Historial append-only | DDL `FalActaEvento` + spec + prototipo | Decidir estrategia TipoEvt (19 -> 62+) antes de crear; ver seccion 5 | Alta (bloqueada por decision TipoEvt) |
| `fal_acta_documento` | Relacion acta-documento con rol | DDL `FalActaDocumento` + spec | Alinear roles | Alta |
| `fal_documento` | Pieza documental firmable | DDL `FalDocumento` + spec | Alinear estados y tipos de documento | Alta |
| `fal_documento_firma` | Registro de firma por pieza | DDL `FalDocumentoFirma` + spec | -- | Alta |
| `fal_notificacion` | Notificacion al infractor | DDL `FalNotificacion` + spec | Agregar `lote_id`, `referencia_externa`; ver gap de estados | Alta |
| `fal_notificacion_intento` | Intento de notificacion por canal | DDL `FalNotificacionIntento` + spec | -- | Alta |
| `fal_notificacion_acuse` | Acuse de recibo de notificacion | DDL `FalNotificacionAcuse` + spec | -- | Alta |
| `fal_acta_snapshot` | Proyeccion operativa regenerable | DDL `FalActaSnapshot` + spec/08 | Agregar campos faltantes: `MotivoParalizacionAct`, causales bloqueantes, `resultadoFinal` | Alta |
| `fal_acta_evidencia` | Evidencias digitales adjuntas | DDL `FalActaEvidencia` | -- | Alta |
| `fal_dependencia` | Dependencia organizacional | DDL `FalDependencia` | -- | Media |
| `fal_dependencia_version` | Version historica de dependencia | DDL `FalDependenciaVersion` | -- | Media |
| `fal_inspector` | Inspector actor del labrado | DDL `FalInspector` | -- | Media |
| `fal_inspector_version` | Version historica de inspector | DDL `FalInspectorVersion` | -- | Media |
| `fal_medida_preventiva` | Referencial de tipos de medida | DDL `FalMedidaPreventiva` | -- | Media |
| `fal_acta_medida_preventiva` | Medidas aplicadas al acta | DDL `FalActaMedidaPreventiva` | Confirmar relacion con `acta_bloqueante_cierre_material` | Media |
| `num_politica` | Politica de numeracion | DDL `NumPolitica` | -- | Media |
| `num_talonario` | Talonario de numeracion | DDL `NumTalonario` | -- | Media |
| `num_talonario_dependencia` | Asignacion talonario-dependencia | DDL `NumTalonarioDependencia` | -- | Media |
| `num_talonario_inspector` | Asignacion talonario-inspector | DDL `NumTalonarioInspector` | -- | Media |
| `num_talonario_movimiento` | Movimientos de numeracion | DDL `NumTalonarioMovimiento` | -- | Media |
| `stor_backend` | Backend de storage documental | DDL `StorBackend` | -- | Media |
| `stor_politica` | Politica de storage | DDL `StorPolitica` | -- | Media |
| `stor_objeto` | Objeto fisico en storage | DDL `StorObjeto` | -- | Media |
| `fal_normativa_faltas` | Normativa de faltas | DDL `FalNormativaFaltas` | -- | Media |
| `fal_articulo_normativa_faltas` | Articulos de normativa | DDL `FalArticuloNormativaFaltas` | -- | Media |
| `fal_tarifario_unidad_faltas` | Tarifario de unidades | DDL `FalTarifarioUnidadFaltas` | -- | Media |
| `fal_acta_articulo_infringido` | Articulos infringidos por acta | DDL `FalActaArticuloInfringido` | -- | Media |
| `fal_acta_articulo_auditoria` | Auditoria de cambios en articulos | DDL `FalActaArticuloAuditoria` | -- | Media |
| `fal_observacion` | Observaciones polimorficas | DDL `FalObservacion` | -- | Media |
| `fal_acta_transito` | Datos de transito del acta | DDL `FalActaTransito` | -- | Media (segun tipo de acta) |
| `fal_acta_vehiculo` | Vehiculo involucrado | DDL `FalActaVehiculo` | -- | Media (segun tipo de acta) |
| `fal_acta_contravencion` | Datos de contravenciones | DDL `FalActaContravencion` | -- | Baja (segun tipo) |
| `fal_acta_sustancias_alimenticias` | Datos de sustancias alimenticias | DDL `FalActaSustanciasAlimenticias` | -- | Baja (segun tipo) |
| `fal_acta_transito_alcoholemia` | Mediciones de alcoholemia | DDL `FalActaTransitoAlcoholemia` | -- | Baja (segun tipo) |
| `fal_alcoholimetro` + `version` | Equipos de alcoholimetria | DDL | -- | Baja |

### 7.2 Tablas probables a decidir

Estas tablas surgen del prototipo validado o de gaps criticos de spec. No tienen DDL previo. Requieren decision humana antes de disenar el DDL.

| Tabla candidata MySQL | Por que existe | De donde surge | Decision necesaria | Prioridad |
|---|---|---|---|---|
| `acta_pago_voluntario` | Circuito de pago previo a fallo; historial de intentos | Prototipo validado; bandeja D3 | Tabla propia vs evento + snapshot; campos de comprobante | Alta |
| `acta_pago_condena` | Circuito de pago de condena firme; separacion de voluntario | Prototipo validado | Tabla separada casi obligatoria; campos minimos | Alta |
| `acta_paralizacion` | Paralizacion con motivo persistido; historial | Gap critico D10 + prototipo | Tabla historica + campo en snapshot; valores enum de motivo | Alta |
| `acta_bloqueante_cierre_material` | Tres origenes de bloqueo; dos pasos por origen | Prototipo + gap critico D11/D12 | Tabla propia vs extension de medida preventiva | Alta |
| `acta_fallo` | Decision juridica con monto; N fallos historicos | Prototipo; DDL parcial como doc tipo 4 | Entidad separada vs documento enriquecido vs hibrido | Alta |
| `acta_apelacion` | Apelacion con canal, estado y resultado | Prototipo; DDL parcial tipo 13 | Tabla propia vs evento enriquecido | Media |
| `acta_gestion_externa` | Derivacion con trazabilidad de resultado; N historicas | Prototipo; spec servicio; flags snapshot | Tabla propia vs flags + eventos | Media |
| `acta_archivo` | Historial de archivos con causales estructuradas | Gap critico D11 + prototipo | Tabla historica vs campos en acta vs eventos | Media |
| `lote_correo` | Agrupacion de envios postales; trazabilidad de lote | Prototipo | Tabla propia vs diferir vs campo en notificacion | Baja (diferir recomendado) |
| `acta_qr_acceso` o `acta_acceso_portal` | Lookup QR -> actaId; trazabilidad de acceso portal | Prototipo; mecanismo pendiente | Tabla propia vs campo en acta/documento vs derivado | Baja (diferir hasta diseno de portal) |

### 7.3 Tablas o conceptos a diferir

Estos conceptos existen en el modelo pero no requieren tabla propia en la primera iteracion, o dependen de disenos externos al modulo.

| Concepto | Por que diferir | Alternativa mientras tanto |
|---|---|---|
| Integracion postal avanzada (LoteCorreo completo) | Diseno de integracion postal real pendiente | Solo campo `lote_id VARCHAR(50)` en `fal_notificacion_intento` |
| Geoespacial / PostGIS | Tablas `geo_gmat_*` externas; equipo territorial | Sin cambio; tablas externas fuera del modulo |
| Catalogos externos compartidos (rubros, geo_ign, geo_indec) | Gobernados por otros sistemas | Solo relaciones FK cuando aplique |
| Aplicaciones moviles / offline | Fuera del alcance del prototipo actual | -- |
| Comprobante de pago (modelo fisico EM/RC/Cmte/Pref/Nro) | Sin spec de ingresos cruzada | Campo `comprobante_ref VARCHAR(100)` como placeholder |
| Domicilio electronico verificado (modelo completo) | Pendiente de diseno de canal electronico | Campo booleano `domicilio_electronico_verificado` en notificacion |
| Motor de firma documental real | Servicio externo; pendiente de contrato | Solo `StorageKey` para resultado de firma |
| Portal infractor completo (QR mecanismo) | Mecanismo de lookup pendiente de diseno | Endpoint parametrico con `actaId` directo |
| Territorialidad PostGIS / SIG | Fuera del modulo | Usar tablas existentes como dependencia |

---

## 8. Proximo slice recomendado

### `DOMINIO_REAL_MODELO_TABLAS_MYSQL.md`

**Descripcion:**
Propuesta revisable de modelo de tablas MySQL para el dominio real de Faltas.
Una tabla por seccion. Campos con nombre, tipo MySQL, observacion y origen.

**Alcance:**
- Proponer DDL conceptual en MySQL para tablas casi seguras y tablas probables.
- Marcar decisiones pendientes por tabla y por campo.
- No cerrar el dominio: la revision humana define la verdad final.
- No implementar codigo Java ni migraciones.

**Prerequisito antes de crear ese documento:**
Las siguientes decisiones deben tomarse manualmente (o marcarse como pendiente explicito en cada tabla):

1. Estrategia TipoEvt: ampliar enum / catalogo de tabla / hibrido con payload.
2. Fallo: entidad separada / documento enriquecido / hibrido.
3. PagoVoluntario: tabla propia / evento + snapshot.
4. GestionExterna: tabla propia / flags + eventos.
5. Paralizacion: tabla historica + campo en snapshot (recomendacion: si en ambas).
6. Archivo: tabla historica / campos en acta / eventos.
7. BloqueanteCierreMaterial: tabla propia / extension medida preventiva.
8. LoteCorreo: tabla propia / diferir (recomendacion: diferir).
9. accionPendiente: campo en snapshot / derivado de eventos.
10. Charset / collation MySQL: confirmar antes de DDL.

**Clausula de revision humana:**
El documento `DOMINIO_REAL_MODELO_TABLAS_MYSQL.md` debe tratarse como propuesta revisable, no como dominio cerrado. La revision humana, tabla por tabla y campo por campo, define la verdad final del modelo de datos.

---

*Documento generado: Jun 2026. Sin cambios de codigo. Sin modificacion de specs existentes. Sin DDL implementado.*

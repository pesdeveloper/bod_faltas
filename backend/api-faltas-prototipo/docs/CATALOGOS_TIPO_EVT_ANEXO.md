# Anexo completo de `tipo_evt` — Faltas

> Documento de apoyo / extracción. **No es DDL ni fuente de verdad.**
> Complementa `CATALOGOS_EXTRAIDOS_PROTOTIPO.md` (sección "Eventos").
> Slice **read-only**: no se modificó código productivo, tests, Angular, DTOs ni datos mock.

---

## 1. Resumen ejecutivo

`tipo_evt` es el tipo de hecho histórico append-only que se persiste en `fal_acta_evento`
(modelo productivo) y que en el prototipo vive como `ActaEventoMock.tipoEvento` (String libre).

Hallazgos:

- **108 valores distintos** de `tipoEvento` encontrados en el prototipo.
- **72 son runtime real**: emitidos por lógica de los `*Support` vía `registrarEvento(...)`.
- **36 son mock-only**: aparecen únicamente en `MockDataFactory` (precarga demo).
- **0 son test-only**: todos los eventos esperados por tests corresponden a runtime real.
- **0 son doc-only / UI-only** que no estén ya cubiertos por runtime o mock (Angular está
  fuera de alcance por `.cursorignore`; no es fuente de verdad para `tipo_evt`).

Decisión global propuesta sobre el set productivo:

- **~56 eventos** se conservan (con o sin renombre) como hecho real de dominio.
- **~24 eventos** se descartan o se mapean a otro: proyección de bandeja, estado, demo,
  asignación visual o duplicado genérico.
- El resto requiere **decisión humana** (normalizaciones con pérdida de matiz: presencial vs
  portal, fallo genérico vs absolutorio/condenatorio, etc.).

Marca `CONGELAR`: lo que se congela es el **conjunto semántico** de la columna `Set productivo
recomendado` (sección 9). No implica crear tabla catálogo editable ni DDL ahora.

Regla aplicada:

> Sólo sobrevive en el enum productivo el evento que expresa un **hecho real de dominio**.
> `PASE_BANDEJA`, `ASIGNACION`, estados como `PENDIENTE_FIRMA`/`PENDIENTE_NOTIFICACION`,
> y los `*_DEMO` se descartan: son proyección/snapshot, no hecho.

---

## 2. Metodología de búsqueda

Fuentes barridas (orden de autoridad: backend funcional > tests > mocks/demo > UI > docs):

- `backend/api-faltas-prototipo/src/main/java/**` — emisión runtime (`registrarEvento(...)`),
  asignaciones a la variable `tipoEvento`, mapeos `switch` y constantes.
- `backend/api-faltas-prototipo/src/main/.../store/MockDataFactory.java` — constructores
  `new ActaEventoMock(...)` de precarga demo (4.º argumento posicional = `tipoEvento`).
- `backend/api-faltas-prototipo/src/test/java/**` — `jsonPath("$[*].tipoEvento", hasItem(...))`
  y `@.tipoEvento == '...'` (contrato esperado).
- `backend/api-faltas-prototipo/docs/**` — sólo apoyo.
- Angular: **no inspeccionable** (filtrado por `.cursorignore`) y declarado no-fuente-de-verdad.

Modelo del registro (define qué string es el tipo de evento):

```5:13:backend/api-faltas-prototipo/src/main/java/ar/gob/malvinas/faltas/prototipo/domain/ActaEventoMock.java
public record ActaEventoMock(
        String id,
        String actaId,
        LocalDateTime fechaHora,
        String tipoEvento,
        String bloqueOrigen,
        String bloqueDestino,
        String descripcion) {
}
```

Patrón común de emisión runtime (`tipoEvento` es el 2.º argumento):

```346:351:backend/api-faltas-prototipo/src/main/java/ar/gob/malvinas/faltas/prototipo/store/PiezasFirmaSupport.java
        registrarEvento(
                actaId,
                tipoEvento,
                bloqueOrigen,
                actualizada.bloqueActual(),
                todasProducidas ? mensajeEventoCompleto : mensajeEventoParcial);
```

---

## 3. Tabla completa de eventos encontrados

Leyenda `origen`: `RUNTIME` (lógica real), `MOCK_ONLY` (sólo precarga demo).
Leyenda `es_hecho_dominio`: `SI` / `NO` / `DUDOSO`.
Leyenda `decision_recomendada`: `CONSERVAR`, `CONSERVAR_RENOMBRAR`, `MAPEAR_A_OTRO`,
`DESCARTAR`, `DECISION_HUMANA`.

### 3.1 RUNTIME

| `tipo_evt_actual` | `fuente_principal` | `origen` | `familia` | `hecho_dominio` | `decision` | `tipo_evt_productivo_sugerido` | `char6` | `observaciones` |
|---|---|---|---|---|---|---|---|---|
| NOTIFICACION_ENTREGADA | NotificacionSupport:106 | RUNTIME | notificación | SI | CONSERVAR_RENOMBRAR | NOTIFICACION_POSITIVA | NOTPOS | "entregada"="positiva"; también NotificadorMunicipal:104, CorreoPostal:1040, mock |
| NOTIFICACION_NO_ENTREGADA | NotificacionSupport:153 | RUNTIME | notificación | SI | CONSERVAR_RENOMBRAR | NOTIFICACION_NEGATIVA | NOTNEG | también NotificadorMunicipal:126, CorreoPostal:640 |
| NOTIFICACION_VENCIDA | NotificacionSupport:268 | RUNTIME | notificación | SI | CONSERVAR | NOTIFICACION_VENCIDA | NOTVNC | también NotificadorMunicipal, CorreoPostal |
| NOTIFICACION_REINTENTADA | NotificacionSupport:224 | RUNTIME | notificación | SI | CONSERVAR | NOTIFICACION_REINTENTADA | NOTRTY | reintento tras negativa |
| NOTIFICACION_REINTENTADA_POST_VENCIMIENTO | NotificacionSupport:217,318 | RUNTIME | notificación | SI | DECISION_HUMANA | NOTIFICACION_REINTENTADA_POST_VENCIMIENTO | NOTRTV | ¿fundir en NOTRTY con flag? |
| NOTIFICACION_PORTAL_VISUALIZADA | PortalInfractorSupport:461 | RUNTIME | portal | SI | CONSERVAR | NOTIFICACION_PORTAL_VISUALIZADA | NOTPVI | acuse de visualización en portal |
| NOTIFICACION_SUPERADA_POR_PORTAL | PortalInfractorSupport:394 | RUNTIME | portal | SI | CONSERVAR | NOTIFICACION_SUPERADA_POR_PORTAL | NOTSUP | notificación previa anulada por acto de portal |
| NOTIFICACION_ACTA_GENERADA | PiezasFirmaSupport:188 | RUNTIME | documental | SI | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | tipo pieza = NOTIFICACION_ACTA en payload |
| RESOLUCION_GENERADA | PiezasFirmaSupport:216 | RUNTIME | documental | SI | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | tipo pieza = RESOLUCION |
| RECTIFICACION_GENERADA | PiezasFirmaSupport:244 | RUNTIME | documental | SI | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | tipo pieza = RECTIFICACION |
| NULIDAD_GENERADA | PiezasFirmaSupport:274 | RUNTIME | documental | SI | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | tipo pieza = NULIDAD |
| MEDIDA_PREVENTIVA_GENERADA | PiezasFirmaSupport:158 | RUNTIME | documental | SI | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | tipo pieza = MEDIDA_PREVENTIVA |
| LEVANTAMIENTO_MEDIDA_PREVENTIVA_PENDIENTE_FIRMA | CerrabilidadSupport:552,723 | RUNTIME | documental | DUDOSO | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | generación de resolutorio; "PENDIENTE_FIRMA" es estado |
| LIBERACION_RODADO_PENDIENTE_FIRMA | CerrabilidadSupport:553,723 | RUNTIME | documental | DUDOSO | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | ídem |
| RESTITUCION_DOCUMENTACION_PENDIENTE_FIRMA | CerrabilidadSupport:554,723 | RUNTIME | documental | DUDOSO | MAPEAR_A_OTRO | DOCUMENTO_GENERADO | DOCGEN | ídem |
| DOCUMENTO_FIRMADO | PiezasFirmaSupport:426,444,454,458 | RUNTIME | documental/firma | SI | CONSERVAR | DOCUMENTO_FIRMADO | DOCFIR | tipo pieza en payload |
| NULIDAD_FIRMADA | PiezasFirmaSupport:449 | RUNTIME | documental/firma | SI | DECISION_HUMANA | NULIDAD_FIRMADA | NULFIR | firma de nulidad cierra expediente; ¿DOCFIR + CIERRA? |
| FALLO_ABSOLUTORIO_DICTADO | FalloPlazoApelacionSupport:176 | RUNTIME | fallo | SI | CONSERVAR | FALLO_ABSOLUTORIO_DICTADO | FALABS | |
| FALLO_CONDENATORIO_DICTADO | FalloPlazoApelacionSupport:186 | RUNTIME | fallo | SI | CONSERVAR | FALLO_CONDENATORIO_DICTADO | FALCON | también precarga mock |
| FALLO_POST_GESTION_EXTERNA_DICTADO | FalloPlazoApelacionSupport:879 | RUNTIME | fallo | SI | DECISION_HUMANA | FALLO_POST_GESTION_EXTERNA_DICTADO | FALPGE | ¿fundir en FALABS/FALCON con flag postGE? |
| FALLO_ABSOLUTORIO_NOTIFICADO | FalloPlazoApelacionSupport:343,394 | RUNTIME | notificación | SI | DECISION_HUMANA | NOTIFICACION_POSITIVA | NOTPOS | ¿colapsar en NOTPOS con pieza=FALLO_ABSOLUTORIO? también NotificadorMunicipal/CorreoPostal |
| FALLO_CONDENATORIO_NOTIFICADO | FalloPlazoApelacionSupport:350,400 | RUNTIME | notificación | SI | DECISION_HUMANA | NOTIFICACION_POSITIVA | NOTPOS | ídem, pieza=FALLO_CONDENATORIO |
| FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL | FalloPlazoApelacionSupport:755 | RUNTIME | notificación | SI | DECISION_HUMANA | NOTIFICACION_POSITIVA | NOTPOS | ¿NOTPOS con canal=PRESENCIAL? |
| PLAZO_APELACION_VENCIDO | FalloPlazoApelacionSupport:447 | RUNTIME | apelación | SI | CONSERVAR | PLAZO_APELACION_VENCIDO | PLAVNC | habilita firmeza |
| APELACION_PRESENTADA | FalloPlazoApelacionSupport:496 | RUNTIME | apelación | SI | CONSERVAR | APELACION_PRESENTADA | APEPRE | |
| APELACION_RECHAZADA | FalloPlazoApelacionSupport:559 | RUNTIME | apelación | SI | CONSERVAR | APELACION_RECHAZADA | APERAZ | |
| APELACION_ACEPTADA_ABSUELVE | FalloPlazoApelacionSupport:565 | RUNTIME | apelación | SI | CONSERVAR | APELACION_ACEPTADA_ABSUELVE | APEABS | |
| CONDENA_CONSENTIDA | FalloPlazoApelacionSupport:660 | RUNTIME | apelación/condena | SI | CONSERVAR | CONDENA_CONSENTIDA | CONCON | |
| CONDENA_CONSENTIDA_PRESENCIAL | FalloPlazoApelacionSupport:772 | RUNTIME | apelación/condena | SI | DECISION_HUMANA | CONDENA_CONSENTIDA | CONCON | ¿CONCON con canal=PRESENCIAL? |
| PAGO_VOLUNTARIO_SOLICITADO | PagoVoluntarioSupport:149,299 | RUNTIME | pago voluntario | SI | CONSERVAR | PAGO_VOLUNTARIO_SOLICITADO | PAGVSO | |
| PAGO_VOLUNTARIO_MONTO_FIJADO | PagoVoluntarioSupport:223 | RUNTIME | pago voluntario | SI | CONSERVAR | PAGO_VOLUNTARIO_MONTO_FIJADO | PAGVMF | |
| PAGO_VOLUNTARIO_VENCIDO | PagoVoluntarioSupport:378 | RUNTIME | pago voluntario | SI | CONSERVAR | PAGO_VOLUNTARIO_VENCIDO | PAGVVN | |
| PAGO_VOLUNTARIO_INFORMADO_PORTAL | PagoVoluntarioSupport:448 | RUNTIME | pago voluntario | SI | MAPEAR_A_OTRO | PAGO_VOLUNTARIO_INFORMADO | PAGINF | canal=PORTAL en payload |
| PAGO_INFORMADO | PagoInformadoSupport:89 | RUNTIME | pago voluntario | SI | CONSERVAR_RENOMBRAR | PAGO_VOLUNTARIO_INFORMADO | PAGINF | duplicado semántico de PAGO_VOLUNTARIO_INFORMADO_PORTAL |
| COMPROBANTE_PAGO_ADJUNTADO | PagoInformadoSupport:146 | RUNTIME | pago voluntario | SI | CONSERVAR | COMPROBANTE_PAGO_ADJUNTADO | PAGCMP | distinto de "informado" |
| PAGO_CONFIRMADO | PagoInformadoSupport:178 | RUNTIME | pago voluntario | SI | CONSERVAR_RENOMBRAR | PAGO_VOLUNTARIO_CONFIRMADO | PAGCNF | |
| PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO | PagoInformadoSupport:243 | RUNTIME | pago voluntario | SI | MAPEAR_A_OTRO | PAGO_VOLUNTARIO_CONFIRMADO | PAGCNF | canal=EXTERNO en payload |
| PAGO_OBSERVADO | PagoInformadoSupport:275 | RUNTIME | pago voluntario | SI | CONSERVAR_RENOMBRAR | PAGO_VOLUNTARIO_OBSERVADO | PAGOBS | |
| PAGO_CONDENA_INFORMADO | PagoCondenaSupport:61,81 | RUNTIME | pago condena | SI | CONSERVAR | PAGO_CONDENA_INFORMADO | PCOINF | portal y dirección comparten tipo |
| PAGO_CONDENA_CONFIRMADO | PagoCondenaSupport:99 | RUNTIME | pago condena | SI | CONSERVAR | PAGO_CONDENA_CONFIRMADO | PCOCNF | |
| PAGO_CONDENA_OBSERVADO | PagoCondenaSupport:117 | RUNTIME | pago condena | SI | CONSERVAR | PAGO_CONDENA_OBSERVADO | PCOOBS | |
| PAGO_CONDENA_REGISTRADO_PRESENCIAL | FalloPlazoApelacionSupport:779 | RUNTIME | pago condena | SI | DECISION_HUMANA | PAGO_CONDENA_INFORMADO | PCOINF | ¿PCOINF/PCOCNF con canal=PRESENCIAL? |
| PAGO_EN_APREMIO_REGISTRADO | GestionExternaSupport:443 | RUNTIME | gestión externa/pago | SI | CONSERVAR | PAGO_EN_APREMIO_REGISTRADO | PAGAPR | |
| ARCHIVADO_DESDE_ANALISIS_DIRECTO | ArchivoReingresoSupport:133 | RUNTIME | archivo | SI | CONSERVAR_RENOMBRAR | ACTA_ARCHIVADA | ARCHIV | motivo en payload |
| ARCHIVADO_POST_EVALUACION_VENCIMIENTO | ArchivoReingresoSupport:192 | RUNTIME | archivo | SI | MAPEAR_A_OTRO | ACTA_ARCHIVADA | ARCHIV | motivo=POST_VENCIMIENTO |
| ACTA_ANULADA_POR_NULIDAD | ArchivoReingresoSupport:306 | RUNTIME | archivo/nulidad | SI | CONSERVAR | ACTA_ANULADA_POR_NULIDAD | ANUNUL | distinto de archivo común |
| ACTA_REINGRESADA_DESDE_ARCHIVO | ArchivoReingresoSupport:391 | RUNTIME | archivo | SI | CONSERVAR_RENOMBRAR | ACTA_REINGRESADA | REINGR | origen en payload |
| ACTA_REINGRESADA_DESDE_ARCHIVO_NULIDAD | ArchivoReingresoSupport:439 | RUNTIME | archivo/nulidad | SI | MAPEAR_A_OTRO | ACTA_REINGRESADA | REINGR | origen=NULIDAD |
| PARALIZACION | ParalizacionReactivacionSupport:242 | RUNTIME | paralización | SI | CONSERVAR_RENOMBRAR | ACTA_PARALIZADA | PARALZ | motivo en payload |
| ACTA_REACTIVADA_DESDE_PARALIZADAS | ParalizacionReactivacionSupport:188 | RUNTIME | paralización | SI | CONSERVAR_RENOMBRAR | ACTA_REACTIVADA | REACTV | |
| DERIVACION_GESTION_EXTERNA | GestionExternaSupport:228 | RUNTIME | gestión externa | SI | CONSERVAR_RENOMBRAR | DERIVACION_GESTION_EXTERNA | EXTDER | también precarga mock |
| ACTA_REINGRESADA_DESDE_GESTION_EXTERNA | GestionExternaSupport:316 | RUNTIME | gestión externa | SI | CONSERVAR_RENOMBRAR | RETORNO_GESTION_EXTERNA | EXTRET | |
| ACTA_REINGRESADA_DESDE_APREMIO_SIN_PAGO | GestionExternaSupport:379 | RUNTIME | gestión externa | SI | MAPEAR_A_OTRO | RETORNO_GESTION_EXTERNA | EXTRET | subtipo=APREMIO_SIN_PAGO |
| RESULTADO_GESTION_EXTERNA_PROPONE_ABSOLVER | GestionExternaSupport:479,618 | RUNTIME | gestión externa | SI | CONSERVAR_RENOMBRAR | RESULTADO_GE_PROPONE_ABSOLVER | EXTPRA | |
| RESULTADO_GESTION_EXTERNA_PROPONE_MODIFICAR_MONTO | GestionExternaSupport:574,598 | RUNTIME | gestión externa | SI | CONSERVAR_RENOMBRAR | RESULTADO_GE_PROPONE_MODIFICAR_MONTO | EXTPRM | |
| RESOLUCION_JUZGADO_CONFIRMA_CONDENA | GestionExternaSupport:530 | RUNTIME | gestión externa | SI | CONSERVAR_RENOMBRAR | RESULTADO_GE_CONFIRMA_CONDENA | EXTCON | |
| CIERRE_ANALISIS | CierreSupport:100 | RUNTIME | cierre | SI | CONSERVAR_RENOMBRAR | ACTA_CERRADA | CIERRA | motivo en payload |
| ORIGEN_BLOQUEO_MEDIDA_PREVENTIVA_RECONOCIDO | CerrabilidadSupport:360 | RUNTIME | cumplimiento material | SI | CONSERVAR | ORIGEN_BLOQUEO_MEDIDA_PREVENTIVA_RECONOCIDO | ORGMP | reconocimiento de origen bloqueante |
| ORIGEN_BLOQUEO_LIBERACION_RODADO_RECONOCIDO | CerrabilidadSupport:372 | RUNTIME | cumplimiento material | SI | CONSERVAR_RENOMBRAR | ORIGEN_BLOQUEO_RODADO_RECONOCIDO | ORGROD | |
| ORIGEN_BLOQUEO_ENTREGA_DOCUMENTACION_RECONOCIDO | CerrabilidadSupport:384 | RUNTIME | cumplimiento material | SI | CONSERVAR_RENOMBRAR | ORIGEN_BLOQUEO_DOCUMENTACION_RECONOCIDO | ORGDOC | |
| CUMPLIMIENTO_LEVANTAMIENTO_MEDIDA_PREVENTIVA | CerrabilidadSupport:657,813 | RUNTIME | cumplimiento material | SI | CONSERVAR_RENOMBRAR | CUMPLIMIENTO_MEDIDA_PREVENTIVA | CUMMP | |
| CUMPLIMIENTO_LIBERACION_RODADO | CerrabilidadSupport:658,813 | RUNTIME | cumplimiento material | SI | CONSERVAR | CUMPLIMIENTO_LIBERACION_RODADO | CUMROD | |
| CUMPLIMIENTO_RESTITUCION_DOCUMENTACION | CerrabilidadSupport:659,813 | RUNTIME | cumplimiento material | SI | CONSERVAR | CUMPLIMIENTO_RESTITUCION_DOCUMENTACION | CUMDOC | |
| RESULTADO_ABSUELTO_MARCADO | CerrabilidadSupport:934 | RUNTIME | fallo/resultado | SI | DECISION_HUMANA | RESULTADO_ABSUELTO_MARCADO | RESABS | relación con FALABS; ¿necesario separado? |
| MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO | CerrabilidadSupport:74,1179 | RUNTIME | constatación | SI | CONSERVAR | MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO | MPPOST | constante TIPO_EVENTO_... |
| CONSTATACION_SECUESTRO_RODADO_D1_D2 | CerrabilidadSupport:1288 | RUNTIME | constatación | SI | CONSERVAR_RENOMBRAR | CONSTATACION_SECUESTRO_RODADO | CONSROD | sufijo D1_D2 = bloque, redundante |
| CONSTATACION_RETENCION_DOCUMENTAL_D1_D2 | CerrabilidadSupport:1295 | RUNTIME | constatación | SI | CONSERVAR_RENOMBRAR | CONSTATACION_RETENCION_DOCUMENTAL | CONSDOC | |
| CONSTATACION_MEDIDA_PREVENTIVA_D1_D2 | CerrabilidadSupport:1302 | RUNTIME | constatación | SI | CONSERVAR_RENOMBRAR | CONSTATACION_MEDIDA_PREVENTIVA | CONSMP | |
| ACTA_ENVIADA_A_NOTIFICACION | ArchivoReingresoSupport:250 | RUNTIME | enriquecimiento | SI | CONSERVAR_RENOMBRAR | ACTA_ENVIADA_A_NOTIFICACION | ENVNOT | cierre de enriquecimiento → D4 |
| LOTE_CORREO_GENERADO | CorreoPostalNotificacionSupport:127 | RUNTIME | correo postal | SI | CONSERVAR | LOTE_CORREO_GENERADO | CORLOT | |
| CORREO_ENVIO_INDIVIDUAL | CorreoPostalNotificacionSupport:203 | RUNTIME | correo postal | SI | DECISION_HUMANA | CORREO_ENVIO_INDIVIDUAL | CORENV | ¿fundir en NOTIFICACION_ENVIADA (NOTENV)? |
| LOTE_CORREO_ANULADO | CorreoPostalNotificacionSupport:276 | RUNTIME | correo postal | SI | CONSERVAR | LOTE_CORREO_ANULADO | CORANU | |

### 3.2 MOCK_ONLY

| `tipo_evt_actual` | `fuente_principal` | `origen` | `familia` | `hecho_dominio` | `decision` | `tipo_evt_productivo_sugerido` | `char6` | `observaciones` |
|---|---|---|---|---|---|---|---|---|
| ALTA | MockDataFactory:136 | MOCK_ONLY | alta/labrado | SI | MAPEAR_A_OTRO | ACTA_LABRADA | ACTLAB | hecho real; runtime no lo emite (gap) |
| ALTA_DEMO_CERRABILIDAD | MockDataFactory:1548 | MOCK_ONLY | QA/demo | SI | MAPEAR_A_OTRO | ACTA_LABRADA | ACTLAB | variante demo de ALTA |
| ALTA_DEMO_MEDIDA | MockDataFactory:1624 | MOCK_ONLY | QA/demo | SI | MAPEAR_A_OTRO | ACTA_LABRADA | ACTLAB | variante demo de ALTA |
| ALTA_DEMO_CERRABILIDAD_PAGO_CONFIRMADO | MockDataFactory:1683 | MOCK_ONLY | QA/demo | SI | MAPEAR_A_OTRO | ACTA_LABRADA | ACTLAB | variante demo de ALTA |
| ALTA_DEMO_HECHO_MATERIAL | MockDataFactory:1888 | MOCK_ONLY | QA/demo | SI | MAPEAR_A_OTRO | ACTA_LABRADA | ACTLAB | variante demo de ALTA |
| ACTUALIZACION_DATOS | MockDataFactory:152 | MOCK_ONLY | enriquecimiento | DUDOSO | DECISION_HUMANA | ACTA_ENRIQUECIDA | ENRACT | ¿hecho de enriquecimiento productivo? hoy sin runtime |
| ASIGNACION | MockDataFactory:144 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | asignación operativa/visual, no hecho |
| ASIGNACION_ANALISTA | MockDataFactory:484 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | asignación visual |
| PASE_BANDEJA | MockDataFactory:160 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | **proyección de bandeja**; no debe sobrevivir |
| PASE_DEMO | MockDataFactory:1810 | MOCK_ONLY | demo/proyección | NO | DESCARTAR | — | — | navegación demo |
| DOCUMENTO_GENERADO | MockDataFactory:267,3095 | MOCK_ONLY | documental | SI | CONSERVAR_RENOMBRAR | DOCUMENTO_GENERADO | DOCGEN | genérico; runtime usa *_GENERADA |
| PENDIENTE_FIRMA | MockDataFactory:275 | MOCK_ONLY | estado | NO | DESCARTAR | — | — | estado/snapshot, no hecho |
| FIRMA_COMPLETADA | MockDataFactory:332,400 | MOCK_ONLY | documental/firma | SI | MAPEAR_A_OTRO | DOCUMENTO_FIRMADO | DOCFIR | duplicado de DOCUMENTO_FIRMADO |
| PENDIENTE_NOTIFICACION | MockDataFactory:340,2580 | MOCK_ONLY | estado | NO | DESCARTAR | — | — | estado/snapshot |
| NOTIFICACION_EN_CURSO | MockDataFactory:408 | MOCK_ONLY | estado | NO | DESCARTAR | — | — | estado/snapshot |
| SEGUIMIENTO | MockDataFactory:416 | MOCK_ONLY | demo | NO | DESCARTAR | — | — | nota de demo |
| OBSERVACION | MockDataFactory:687 | MOCK_ONLY | demo | NO | DESCARTAR | — | — | nota de demo |
| RECORDATORIO | MockDataFactory:703 | MOCK_ONLY | demo | NO | DESCARTAR | — | — | nota de demo |
| RESOLUCION | MockDataFactory:550 | MOCK_ONLY | documental | DUDOSO | DESCARTAR | — | — | genérico; pieza real es RESOLUCION_GENERADA |
| CIERRE | MockDataFactory:614,3710 | MOCK_ONLY | cierre | SI | MAPEAR_A_OTRO | ACTA_CERRADA | CIERRA | genérico |
| CIERRE_DEFINITIVO | MockDataFactory:622 | MOCK_ONLY | cierre | DUDOSO | DECISION_HUMANA | ACTA_CERRADA | CIERRA | ¿distinto de cierre operativo? |
| ARCHIVO_OPERATIVO | MockDataFactory:745,3574 | MOCK_ONLY | archivo | SI | MAPEAR_A_OTRO | ACTA_ARCHIVADA | ARCHIV | |
| DERIVACION_RESOLUCION | MockDataFactory:808 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo interno a redacción |
| DERIVACION_NULIDAD | MockDataFactory:881 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo interno; hecho real = NULIDAD_GENERADA |
| DERIVACION_MEDIDA_PREVENTIVA | MockDataFactory:954 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo interno |
| DERIVACION_RECTIFICACION | MockDataFactory:1021 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo interno |
| DERIVACION_REDACCION | MockDataFactory:3289 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo interno |
| DERIVACION_PENDIENTE_FALLO | MockDataFactory:3820 | MOCK_ONLY | proyección | NO | DESCARTAR | — | — | ruteo a bandeja de fallo |
| FALLO_EMITIDO | MockDataFactory:1104,1212,1312 | MOCK_ONLY | fallo | SI | MAPEAR_A_OTRO | FALLO_*_DICTADO | FALABS/FALCON | genérico; duplica fallo dictado |
| NOTIFICACION_FALLO | MockDataFactory:1112,1220,1320 | MOCK_ONLY | notificación | SI | MAPEAR_A_OTRO | NOTIFICACION_POSITIVA | NOTPOS | genérico; duplica FALLO_*_NOTIFICADO |
| CONDENA_FIRME | MockDataFactory:1427 | MOCK_ONLY | apelación/condena | SI | DECISION_HUMANA | CONDENA_FIRME | CONFIR | hecho real; runtime hoy deriva firmeza de PLAVNC sin evento propio |
| CONSTATACION_RODADO | MockDataFactory:1786 | MOCK_ONLY | constatación | SI | MAPEAR_A_OTRO | CONSTATACION_SECUESTRO_RODADO | CONSROD | duplica evento runtime |
| CONSTATACION_DOCUMENTACION | MockDataFactory:1794 | MOCK_ONLY | constatación | SI | MAPEAR_A_OTRO | CONSTATACION_RETENCION_DOCUMENTAL | CONSDOC | duplica evento runtime |
| MEDIDA_PREVENTIVA_APLICABLE | MockDataFactory:1802 | MOCK_ONLY | constatación | SI | MAPEAR_A_OTRO | CONSTATACION_MEDIDA_PREVENTIVA | CONSMP | duplica evento runtime |
| PENDIENTE_NOTIFICACION_MUNICIPAL | MockDataFactory:2327 | MOCK_ONLY | estado | NO | DESCARTAR | — | — | estado/snapshot de canal |
| NOTIFICACION_PORTAL_PENDIENTE | MockDataFactory:2441 | MOCK_ONLY | estado | NO | DESCARTAR | — | — | estado/snapshot de canal |

> Nota: `FALLO_CONDENATORIO_DICTADO`, `DERIVACION_GESTION_EXTERNA` y `NOTIFICACION_ENTREGADA`
> también aparecen como precarga en `MockDataFactory`, pero se contabilizan como **RUNTIME**
> porque la lógica real los emite (no son mock-only).

---

## 4. Eventos runtime reales (detalle por flujo)

Emitidos por `*Support` vía `registrarEvento(...)`. Todos actualizan el timeline
(`ActaEventoMock`) y, según el flujo, mueven bandeja/bloque y/o el estado agregador.

- **Alta/labrado**: *no existe evento runtime*. La alta sólo se ve en precarga mock (`ALTA`).
  Gap a cubrir en productivo con `ACTA_LABRADA`.
- **Constatación material temprana D1/D2** (`CerrabilidadSupport.registrarConstatacionMaterialTemprana`):
  `CONSTATACION_SECUESTRO_RODADO_D1_D2`, `CONSTATACION_RETENCION_DOCUMENTAL_D1_D2`,
  `CONSTATACION_MEDIDA_PREVENTIVA_D1_D2`, `MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO`.
  Crean documento ancla; refuerzan el eje de hecho material.
- **Enriquecimiento → notificación**: `ACTA_ENVIADA_A_NOTIFICACION` (D2 → D4).
- **Documental/firma** (`PiezasFirmaSupport`): generación `*_GENERADA` (pieza pendiente de
  firma) y firma `DOCUMENTO_FIRMADO` / `NULIDAD_FIRMADA`. La firma mueve a notificación, a
  análisis (resolutorio) o cierra (nulidad).
- **Notificación** (`NotificacionSupport`, `NotificadorMunicipalSupport`,
  `CorreoPostalNotificacionSupport`, `PortalInfractorSupport`): `NOTIFICACION_ENTREGADA`,
  `NOTIFICACION_NO_ENTREGADA`, `NOTIFICACION_VENCIDA`, `NOTIFICACION_REINTENTADA`,
  `NOTIFICACION_REINTENTADA_POST_VENCIMIENTO`, `NOTIFICACION_PORTAL_VISUALIZADA`,
  `NOTIFICACION_SUPERADA_POR_PORTAL`. Actualizan `ActaNotificacionMock` (estado/resultado) y
  el avance D4 → D5.
- **Correo postal** (`CorreoPostalNotificacionSupport`): `LOTE_CORREO_GENERADO`,
  `CORREO_ENVIO_INDIVIDUAL`, `LOTE_CORREO_ANULADO`.
- **Fallo** (`FalloPlazoApelacionSupport`): dictado (`FALLO_ABSOLUTORIO_DICTADO`,
  `FALLO_CONDENATORIO_DICTADO`, `FALLO_POST_GESTION_EXTERNA_DICTADO`) y notificado del fallo
  (`FALLO_ABSOLUTORIO_NOTIFICADO`, `FALLO_CONDENATORIO_NOTIFICADO`,
  `FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL`).
- **Plazo/apelación/consentimiento** (`FalloPlazoApelacionSupport`): `PLAZO_APELACION_VENCIDO`,
  `APELACION_PRESENTADA`, `APELACION_RECHAZADA`, `APELACION_ACEPTADA_ABSUELVE`,
  `CONDENA_CONSENTIDA`, `CONDENA_CONSENTIDA_PRESENCIAL`.
- **Pago voluntario** (`PagoVoluntarioSupport`, `PagoInformadoSupport`):
  `PAGO_VOLUNTARIO_SOLICITADO`, `PAGO_VOLUNTARIO_MONTO_FIJADO`, `PAGO_VOLUNTARIO_VENCIDO`,
  `PAGO_VOLUNTARIO_INFORMADO_PORTAL`, `PAGO_INFORMADO`, `COMPROBANTE_PAGO_ADJUNTADO`,
  `PAGO_CONFIRMADO`, `PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO`, `PAGO_OBSERVADO`.
- **Pago condena** (`PagoCondenaSupport`, `FalloPlazoApelacionSupport`):
  `PAGO_CONDENA_INFORMADO`, `PAGO_CONDENA_CONFIRMADO`, `PAGO_CONDENA_OBSERVADO`,
  `PAGO_CONDENA_REGISTRADO_PRESENCIAL`.
- **Cumplimiento material / reconocimiento de origen** (`CerrabilidadSupport`):
  `ORIGEN_BLOQUEO_*_RECONOCIDO`, `*_PENDIENTE_FIRMA` (generación de resolutorio),
  `CUMPLIMIENTO_*`, `RESULTADO_ABSUELTO_MARCADO`.
- **Archivo/reingreso/nulidad** (`ArchivoReingresoSupport`): `ARCHIVADO_DESDE_ANALISIS_DIRECTO`,
  `ARCHIVADO_POST_EVALUACION_VENCIMIENTO`, `ACTA_ANULADA_POR_NULIDAD`,
  `ACTA_REINGRESADA_DESDE_ARCHIVO`, `ACTA_REINGRESADA_DESDE_ARCHIVO_NULIDAD`.
- **Paralización/reactivación** (`ParalizacionReactivacionSupport`): `PARALIZACION`,
  `ACTA_REACTIVADA_DESDE_PARALIZADAS`.
- **Gestión externa/apremio** (`GestionExternaSupport`): `DERIVACION_GESTION_EXTERNA`,
  `ACTA_REINGRESADA_DESDE_GESTION_EXTERNA`, `ACTA_REINGRESADA_DESDE_APREMIO_SIN_PAGO`,
  `PAGO_EN_APREMIO_REGISTRADO`, `RESULTADO_GESTION_EXTERNA_PROPONE_ABSOLVER`,
  `RESULTADO_GESTION_EXTERNA_PROPONE_MODIFICAR_MONTO`, `RESOLUCION_JUZGADO_CONFIRMA_CONDENA`.
- **Cierre** (`CierreSupport`): `CIERRE_ANALISIS`.

---

## 5. Eventos sólo test

**Ninguno.** Todos los `tipoEvento` que aparecen en `src/test/java/**` (vía `hasItem(...)` o
`@.tipoEvento == '...'`) corresponden a eventos runtime ya inventariados; los tests sólo
validan el contrato de lógica real. Algunas pruebas usan `not(hasItem(...))` sobre eventos
runtime (p. ej. `FALLO_EMITIDO`, `FALLO_ABSOLUTORIO_NOTIFICADO`, `APELACION_*` en
`ActasMockFalloApelacionDemoIT`) para asegurar que **no** se emiten en ciertos caminos; eso no
crea eventos test-only.

---

## 6. Eventos mock-only (subclasificación)

- **Mock-only pero hecho de dominio válido** (conservar mapeando a evento productivo):
  `ALTA` (+ variantes `ALTA_DEMO_*`), `DOCUMENTO_GENERADO`, `FIRMA_COMPLETADA`, `CIERRE`,
  `ARCHIVO_OPERATIVO`, `FALLO_EMITIDO`, `NOTIFICACION_FALLO`, `CONSTATACION_RODADO`,
  `CONSTATACION_DOCUMENTACION`, `MEDIDA_PREVENTIVA_APLICABLE`, `CONDENA_FIRME`.
- **Mock-only genérico/legacy** (descartar; pisados por eventos específicos):
  `RESOLUCION`, `CIERRE_DEFINITIVO` (dudoso), `SEGUIMIENTO`, `OBSERVACION`, `RECORDATORIO`.
- **Mock-only de proyección** (descartar; sólo reflejan ruteo/estado):
  `PASE_BANDEJA`, `PASE_DEMO`, `ASIGNACION`, `ASIGNACION_ANALISTA`, `PENDIENTE_FIRMA`,
  `PENDIENTE_NOTIFICACION`, `NOTIFICACION_EN_CURSO`, `PENDIENTE_NOTIFICACION_MUNICIPAL`,
  `NOTIFICACION_PORTAL_PENDIENTE`, `DERIVACION_RESOLUCION`, `DERIVACION_NULIDAD`,
  `DERIVACION_MEDIDA_PREVENTIVA`, `DERIVACION_RECTIFICACION`, `DERIVACION_REDACCION`,
  `DERIVACION_PENDIENTE_FALLO`.
- **Mock-only de demo sin valor productivo**: cubierto arriba por `*_DEMO` y notas demo.

---

## 7. Eventos doc-only / UI-only

- **Doc-only**: ninguno nuevo. Los docs (`DOMINIO_REAL_INVENTARIO_DESDE_PROTOTIPO.md`,
  `PLAN_INTEGRACION_DOMINIO_REAL.md`, `CATALOGOS_EXTRAIDOS_PROTOTIPO.md`) sólo describen el
  campo `tipoEvento`, no introducen tipos propios.
- **UI-only**: no verificable en este slice — `apps/web-direccion-faltas/angular/**` está
  filtrado por `.cursorignore`. Angular **no** es fuente de verdad para `tipo_evt`; cualquier
  literal de evento que existiera sólo en UI **no** debe congelarse como productivo sin
  decisión humana. (Pendiente decisión humana #DH-7.)

---

## 8. Duplicados semánticos y drifts detectados

| Hecho de dominio | Variantes encontradas | Normalización propuesta |
|---|---|---|
| Acta labrada | `ALTA`, `ALTA_DEMO_CERRABILIDAD`, `ALTA_DEMO_MEDIDA`, `ALTA_DEMO_CERRABILIDAD_PAGO_CONFIRMADO`, `ALTA_DEMO_HECHO_MATERIAL` | `ACTA_LABRADA` (ACTLAB) |
| Documento generado | `DOCUMENTO_GENERADO`, `NOTIFICACION_ACTA_GENERADA`, `RESOLUCION_GENERADA`, `RECTIFICACION_GENERADA`, `NULIDAD_GENERADA`, `MEDIDA_PREVENTIVA_GENERADA`, `*_PENDIENTE_FIRMA` | `DOCUMENTO_GENERADO` (DOCGEN) + `tipo_pieza` |
| Documento firmado | `DOCUMENTO_FIRMADO`, `FIRMA_COMPLETADA` | `DOCUMENTO_FIRMADO` (DOCFIR) |
| Fallo dictado | `FALLO_EMITIDO` (genérico) vs `FALLO_ABSOLUTORIO_DICTADO` / `FALLO_CONDENATORIO_DICTADO` | descartar genérico; usar específicos (FALABS/FALCON) |
| Fallo notificado | `NOTIFICACION_FALLO` (genérico) vs `FALLO_ABSOLUTORIO_NOTIFICADO` / `FALLO_CONDENATORIO_NOTIFICADO` / `..._PRESENCIAL` | `NOTIFICACION_POSITIVA` (NOTPOS) + `tipo_pieza` (decisión humana) |
| Notificación entregada / positiva | `NOTIFICACION_ENTREGADA` | `NOTIFICACION_POSITIVA` (NOTPOS) |
| Pago voluntario informado | `PAGO_INFORMADO`, `PAGO_VOLUNTARIO_INFORMADO_PORTAL` | `PAGO_VOLUNTARIO_INFORMADO` (PAGINF) + `canal` |
| Pago voluntario confirmado | `PAGO_CONFIRMADO`, `PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO` | `PAGO_VOLUNTARIO_CONFIRMADO` (PAGCNF) + `canal` |
| Pago condena | `PAGO_CONDENA_INFORMADO`/`..._CONFIRMADO`/`..._OBSERVADO` vs `PAGO_CONDENA_REGISTRADO_PRESENCIAL` | conservar + `canal=PRESENCIAL` (decisión humana) |
| Acta archivada | `ARCHIVADO_DESDE_ANALISIS_DIRECTO`, `ARCHIVADO_POST_EVALUACION_VENCIMIENTO`, `ARCHIVO_OPERATIVO` | `ACTA_ARCHIVADA` (ARCHIV) + `motivo` |
| Acta reingresada | `ACTA_REINGRESADA_DESDE_ARCHIVO`, `..._ARCHIVO_NULIDAD` | `ACTA_REINGRESADA` (REINGR) + `motivo` |
| Retorno gestión externa | `ACTA_REINGRESADA_DESDE_GESTION_EXTERNA`, `..._APREMIO_SIN_PAGO` | `RETORNO_GESTION_EXTERNA` (EXTRET) + `subtipo` |
| Constatación material | runtime `CONSTATACION_*_D1_D2` vs mock `CONSTATACION_RODADO`/`CONSTATACION_DOCUMENTACION`/`MEDIDA_PREVENTIVA_APLICABLE` | unificar a `CONSROD`/`CONSDOC`/`CONSMP` |
| Acta cerrada | `CIERRE_ANALISIS`, `CIERRE`, `CIERRE_DEFINITIVO` | `ACTA_CERRADA` (CIERRA) + `motivo` |

**Drift principal**: el sufijo `_D1_D2` en eventos de constatación mezcla nombre de evento con
bloque/estado (redundante con `bloqueOrigen`/`bloqueDestino`); se recomienda quitarlo en el
enum productivo.

---

## 9. Eventos de proyección que NO deben sobrevivir (DESCARTAR)

Sólo reflejan cambio de bandeja/sub-bandeja, estado, snapshot, asignación visual, ruteo
interno o demo. **Ninguno expresa un hecho de negocio nuevo.**

`PASE_BANDEJA`, `PASE_DEMO`, `ASIGNACION`, `ASIGNACION_ANALISTA`, `PENDIENTE_FIRMA`,
`PENDIENTE_NOTIFICACION`, `NOTIFICACION_EN_CURSO`, `PENDIENTE_NOTIFICACION_MUNICIPAL`,
`NOTIFICACION_PORTAL_PENDIENTE`, `DERIVACION_RESOLUCION`, `DERIVACION_NULIDAD`,
`DERIVACION_MEDIDA_PREVENTIVA`, `DERIVACION_RECTIFICACION`, `DERIVACION_REDACCION`,
`DERIVACION_PENDIENTE_FALLO`, `SEGUIMIENTO`, `OBSERVACION`, `RECORDATORIO`, `RESOLUCION`.

> `PASE_BANDEJA` queda explícitamente marcado como **no productivo**: es proyección de bandeja.

---

## 10. Set productivo recomendado de `tipo_evt`

`CHAR(6)`, estable y legible. Códigos **sugeridos**, no definitivos.

| codigo_char6 | enum_productivo | familia | descripcion_funcional | origen_desde_prototipo | observaciones |
|---|---|---|---|---|---|
| ACTLAB | ACTA_LABRADA | alta/labrado | Acta labrada / ingresada | mock `ALTA` (+ `ALTA_DEMO_*`) | runtime hoy no emite alta (gap) |
| ENRACT | ACTA_ENRIQUECIDA | enriquecimiento | Datos del acta verificados/completados | mock `ACTUALIZACION_DATOS` | decisión humana #DH-1 |
| CONSROD | CONSTATACION_SECUESTRO_RODADO | constatación | Constatación de retención/secuestro de rodado | runtime `CONSTATACION_SECUESTRO_RODADO_D1_D2` (+ mock) | quitar sufijo bloque |
| CONSDOC | CONSTATACION_RETENCION_DOCUMENTAL | constatación | Constatación de retención de documentación | runtime `CONSTATACION_RETENCION_DOCUMENTAL_D1_D2` (+ mock) | |
| CONSMP | CONSTATACION_MEDIDA_PREVENTIVA | constatación | Constatación de medida preventiva aplicable | runtime `CONSTATACION_MEDIDA_PREVENTIVA_D1_D2` (+ mock) | |
| MPPOST | MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO | constatación | Medida preventiva posterior al labrado | runtime `MEDIDA_PREVENTIVA_POSTERIOR_A_LABRADO` | |
| ENVNOT | ACTA_ENVIADA_A_NOTIFICACION | enriquecimiento | Acta enviada a notificación (cierra enriquecimiento) | runtime `ACTA_ENVIADA_A_NOTIFICACION` | |
| DOCGEN | DOCUMENTO_GENERADO | documental | Documento/pieza generado pendiente de firma | runtime `*_GENERADA`, `*_PENDIENTE_FIRMA`; mock `DOCUMENTO_GENERADO` | `tipo_pieza` en payload |
| DOCFIR | DOCUMENTO_FIRMADO | documental/firma | Documento firmado | runtime `DOCUMENTO_FIRMADO`; mock `FIRMA_COMPLETADA` | `tipo_pieza` en payload |
| NULFIR | NULIDAD_FIRMADA | documental/firma | Nulidad firmada (cierra expediente sin notificación) | runtime `NULIDAD_FIRMADA` | decisión humana #DH-2 |
| ORGMP | ORIGEN_BLOQUEO_MEDIDA_PREVENTIVA_RECONOCIDO | cumplimiento material | Reconocimiento de origen bloqueante: medida preventiva | runtime homónimo | |
| ORGROD | ORIGEN_BLOQUEO_RODADO_RECONOCIDO | cumplimiento material | Reconocimiento de origen bloqueante: rodado | runtime `ORIGEN_BLOQUEO_LIBERACION_RODADO_RECONOCIDO` | |
| ORGDOC | ORIGEN_BLOQUEO_DOCUMENTACION_RECONOCIDO | cumplimiento material | Reconocimiento de origen bloqueante: documentación | runtime `ORIGEN_BLOQUEO_ENTREGA_DOCUMENTACION_RECONOCIDO` | |
| CUMMP | CUMPLIMIENTO_MEDIDA_PREVENTIVA | cumplimiento material | Cumplimiento: levantamiento de medida preventiva | runtime `CUMPLIMIENTO_LEVANTAMIENTO_MEDIDA_PREVENTIVA` | |
| CUMROD | CUMPLIMIENTO_LIBERACION_RODADO | cumplimiento material | Cumplimiento: liberación de rodado | runtime homónimo | |
| CUMDOC | CUMPLIMIENTO_RESTITUCION_DOCUMENTACION | cumplimiento material | Cumplimiento: restitución de documentación | runtime homónimo | |
| NOTENV | NOTIFICACION_ENVIADA | notificación | Notificación enviada / puesta a disposición | (implícito; correo `CORREO_ENVIO_INDIVIDUAL`) | decisión humana #DH-3 |
| NOTPOS | NOTIFICACION_POSITIVA | notificación | Notificación con resultado positivo | runtime `NOTIFICACION_ENTREGADA`, `FALLO_*_NOTIFICADO*`; mock `NOTIFICACION_FALLO` | `tipo_pieza`/`canal` en payload |
| NOTNEG | NOTIFICACION_NEGATIVA | notificación | Notificación con resultado negativo | runtime `NOTIFICACION_NO_ENTREGADA` | |
| NOTVNC | NOTIFICACION_VENCIDA | notificación | Notificación vencida | runtime `NOTIFICACION_VENCIDA` | |
| NOTRTY | NOTIFICACION_REINTENTADA | notificación | Notificación reintentada | runtime `NOTIFICACION_REINTENTADA` | |
| NOTRTV | NOTIFICACION_REINTENTADA_POST_VENCIMIENTO | notificación | Reintento posterior al vencimiento | runtime homónimo | decisión humana #DH-4 (fundir en NOTRTY) |
| NOTPVI | NOTIFICACION_PORTAL_VISUALIZADA | portal | Visualización confirmada en portal | runtime homónimo | |
| NOTSUP | NOTIFICACION_SUPERADA_POR_PORTAL | portal | Notificación previa superada por acto de portal | runtime homónimo | |
| CORLOT | LOTE_CORREO_GENERADO | correo postal | Lote de correo postal generado | runtime homónimo | |
| CORENV | CORREO_ENVIO_INDIVIDUAL | correo postal | Envío individual por correo postal | runtime homónimo | decisión humana #DH-3 (¿fundir en NOTENV?) |
| CORANU | LOTE_CORREO_ANULADO | correo postal | Lote de correo postal anulado | runtime homónimo | |
| FALABS | FALLO_ABSOLUTORIO_DICTADO | fallo | Fallo absolutorio dictado | runtime homónimo; mock `FALLO_EMITIDO` | |
| FALCON | FALLO_CONDENATORIO_DICTADO | fallo | Fallo condenatorio dictado | runtime homónimo; mock `FALLO_EMITIDO` | |
| FALPGE | FALLO_POST_GESTION_EXTERNA_DICTADO | fallo | Fallo dictado tras retorno de gestión externa | runtime homónimo | decisión humana #DH-5 |
| RESABS | RESULTADO_ABSUELTO_MARCADO | fallo/resultado | Resultado final marcado como absuelto | runtime homónimo | decisión humana #DH-6 |
| PLAVNC | PLAZO_APELACION_VENCIDO | apelación | Plazo de apelación vencido | runtime homónimo | |
| APEPRE | APELACION_PRESENTADA | apelación | Apelación presentada | runtime homónimo | |
| APERAZ | APELACION_RECHAZADA | apelación | Apelación rechazada | runtime homónimo | |
| APEABS | APELACION_ACEPTADA_ABSUELVE | apelación | Apelación aceptada que absuelve | runtime homónimo | |
| CONCON | CONDENA_CONSENTIDA | apelación/condena | Condena consentida | runtime `CONDENA_CONSENTIDA` (+ `_PRESENCIAL`) | `canal` en payload |
| CONFIR | CONDENA_FIRME | apelación/condena | Condena firme | mock `CONDENA_FIRME` | decisión humana #DH-8 (runtime debería emitirlo) |
| PAGVSO | PAGO_VOLUNTARIO_SOLICITADO | pago voluntario | Pago voluntario solicitado | runtime homónimo | |
| PAGVMF | PAGO_VOLUNTARIO_MONTO_FIJADO | pago voluntario | Monto de pago voluntario fijado | runtime homónimo | |
| PAGVVN | PAGO_VOLUNTARIO_VENCIDO | pago voluntario | Pago voluntario vencido | runtime homónimo | |
| PAGINF | PAGO_VOLUNTARIO_INFORMADO | pago voluntario | Pago voluntario informado | runtime `PAGO_INFORMADO`, `PAGO_VOLUNTARIO_INFORMADO_PORTAL` | `canal` en payload |
| PAGCMP | COMPROBANTE_PAGO_ADJUNTADO | pago voluntario | Comprobante de pago adjuntado | runtime homónimo | |
| PAGCNF | PAGO_VOLUNTARIO_CONFIRMADO | pago voluntario | Pago voluntario confirmado | runtime `PAGO_CONFIRMADO`, `PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO` | `canal` en payload |
| PAGOBS | PAGO_VOLUNTARIO_OBSERVADO | pago voluntario | Pago voluntario observado | runtime `PAGO_OBSERVADO` | |
| PCOINF | PAGO_CONDENA_INFORMADO | pago condena | Pago de condena informado | runtime `PAGO_CONDENA_INFORMADO`, `..._REGISTRADO_PRESENCIAL` | `canal` en payload |
| PCOCNF | PAGO_CONDENA_CONFIRMADO | pago condena | Pago de condena confirmado | runtime homónimo | |
| PCOOBS | PAGO_CONDENA_OBSERVADO | pago condena | Pago de condena observado | runtime homónimo | |
| PAGAPR | PAGO_EN_APREMIO_REGISTRADO | pago condena/externa | Pago en apremio registrado | runtime homónimo | |
| ARCHIV | ACTA_ARCHIVADA | archivo | Acta archivada | runtime `ARCHIVADO_*`; mock `ARCHIVO_OPERATIVO` | `motivo` en payload |
| ANUNUL | ACTA_ANULADA_POR_NULIDAD | archivo/nulidad | Acta anulada por nulidad | runtime homónimo | |
| REINGR | ACTA_REINGRESADA | archivo | Acta reingresada desde archivo | runtime `ACTA_REINGRESADA_DESDE_ARCHIVO*` | `motivo` en payload |
| PARALZ | ACTA_PARALIZADA | paralización | Acta paralizada | runtime `PARALIZACION` | `motivo` en payload |
| REACTV | ACTA_REACTIVADA | paralización | Acta reactivada | runtime `ACTA_REACTIVADA_DESDE_PARALIZADAS` | |
| EXTDER | DERIVACION_GESTION_EXTERNA | gestión externa | Derivación a gestión externa | runtime homónimo (+ mock) | `subtipo` en payload |
| EXTRET | RETORNO_GESTION_EXTERNA | gestión externa | Retorno desde gestión externa | runtime `ACTA_REINGRESADA_DESDE_GESTION_EXTERNA`, `..._APREMIO_SIN_PAGO` | `subtipo` en payload |
| EXTPRA | RESULTADO_GE_PROPONE_ABSOLVER | gestión externa | Resultado externo: propone absolver | runtime `RESULTADO_GESTION_EXTERNA_PROPONE_ABSOLVER` | |
| EXTPRM | RESULTADO_GE_PROPONE_MODIFICAR_MONTO | gestión externa | Resultado externo: propone modificar monto | runtime `RESULTADO_GESTION_EXTERNA_PROPONE_MODIFICAR_MONTO` | |
| EXTCON | RESULTADO_GE_CONFIRMA_CONDENA | gestión externa | Resultado externo: confirma condena (juzgado) | runtime `RESOLUCION_JUZGADO_CONFIRMA_CONDENA` | |
| CIERRA | ACTA_CERRADA | cierre | Acta cerrada | runtime `CIERRE_ANALISIS`; mock `CIERRE`/`CIERRE_DEFINITIVO` | `motivo` en payload |

> Set productivo: **~58 códigos** (incluye 2 condicionados a decisión humana: `ENRACT`, `NOTENV`).

---

## 11. Decisiones humanas pendientes

- **#DH-1 `ENRACT` (ACTA_ENRIQUECIDA)**: ¿el enriquecimiento es un hecho de evento productivo
  o sólo cambio de estado? Hoy sólo existe `ACTUALIZACION_DATOS` mock.
- **#DH-2 `NULFIR` (NULIDAD_FIRMADA)**: ¿conservar como evento propio o expresarlo como
  `DOCFIR(pieza=NULIDAD)` + `CIERRA(motivo=NULIDAD)`?
- **#DH-3 `NOTENV` vs `CORENV`**: ¿modelar un único "notificación enviada" transversal o
  conservar el envío por canal (correo)? Hoy el envío sólo es explícito en correo postal.
- **#DH-4 `NOTRTV`**: ¿fundir reintento post-vencimiento en `NOTRTY` con atributo, o conservar
  separado?
- **#DH-5 `FALPGE`**: ¿fallo post gestión externa es un tipo propio o `FALABS/FALCON` con flag
  `postGestionExterna`?
- **#DH-6 `RESABS`**: ¿`RESULTADO_ABSUELTO_MARCADO` es distinto de `FALLO_ABSOLUTORIO_DICTADO`
  o debe unificarse?
- **#DH-7 UI-only**: confirmar (cuando Angular esté disponible) que no existan literales de
  evento exclusivos de UI; no congelar ninguno sin decisión humana.
- **#DH-8 `CONFIR` (CONDENA_FIRME)**: hecho real hoy sólo en mock; definir si runtime debe
  emitirlo (al vencer el plazo de apelación) o si la firmeza se deriva sin evento.
- **#DH-9 canales/variantes presenciales**: `*_PRESENCIAL` (fallo notificado, condena
  consentida, pago condena) — ¿colapsar en el evento base con atributo `canal=PRESENCIAL`?
- **#DH-10 ¿catálogo o enum?**: confirmar que `tipo_evt` se modela como enum/constante en
  Spring (sin tabla catálogo editable), salvo necesidad de reporting/integración.
- **#DH-11 códigos `CHAR(6)`**: los códigos son sugerencia; validar colisiones y convención
  antes de congelar.

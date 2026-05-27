# Diagnostico: Mocks Funcionales Faltas

Fecha: 2026-05-26

Documento producido en modo solo-lectura sobre el codigo del modulo
api-faltas-prototipo. No se modifico ningun archivo Java ni de test
ni de continuidad. Texto en ASCII basico (sin tildes ni enies) para
evitar mojibake.

NOTA OPERATIVA: el sandbox del subagente que produjo este informe
bloqueo el acceso de las herramientas Write/Delete sobre la carpeta
docs-trabajo/. El archivo se escribio en la raiz del repo como
fallback. Recomendacion: moverlo manualmente a
docs-trabajo/diagnostico-mocks-funcionales-faltas.md.

---

## 1. Resumen ejecutivo

El prototipo backend api-faltas-prototipo carga en MockDataFactory
116 actas mock (ACTA-0001 a ACTA-0116) ademas de exponer un endpoint
de alta dinamica (POST /api/prototipo/actas/mock). La carga se
reparte en bloques con responsabilidades distintas:

- ACTA-0001 a ACTA-0017: nucleo demo canonico (un acta por
  macro-bandeja y variantes de transito).
- ACTA-0018 a ACTA-0023: pago voluntario y cerrabilidad material
  (uso de anclas y reconocimientos materiales).
- ACTA-0024: recorrido e2e oficial (transito con ActaTransitoMock
  + tres flags materiales).
- ACTA-0025: caso minimo para regresion del endpoint de
  constatacion material temprana.
- ACTA-0026: contravencion (BROMATOLOGIA) para medida posterior y
  cerrabilidad cerrable.
- ACTA-0027 a ACTA-0030: cuatro actas "fallo / apelacion" en
  PENDIENTE_ANALISIS limpio con INFORME_ALCOHOTEST.
- ACTA-0031 a ACTA-0033: notificador municipal.
- ACTA-0034 a ACTA-0036: portal infractor / domicilio
  electronico.
- ACTA-0037 a ACTA-0041: correo postal listas para lote.
- ACTA-0042 a ACTA-0045: correo postal en distintos estados
  (EN_TRAMITE, ENTREGADA POSITIVA, NEGATIVA, VENCIDA).
- ACTA-0046 a ACTA-0107: bloque volumen UX (~10 por macro-bandeja
  operativa) sin valor funcional propio.
- ACTA-0108 a ACTA-0116: UX laterales para PENDIENTES_FALLO,
  CON_APELACION y PARALIZADAS.

### Hallazgos principales

1. Bloque de cerradas de volumen con bandera incoherente: 6 actas
   en BANDEJA_CERRADAS (ACTA-0099 a ACTA-0104) quedan con
   estaCerrada=false. El clasificador no se entera (clasifica por
   bandeja), pero cualquier validador que mire la bandera ve un
   estado contradictorio.
2. Actas con fallo notificado pero sin resultadoFinal:
   ACTA-0015, ACTA-0016, ACTA-0017 tienen doc FALLO firmado y dos
   notificaciones ENTREGADA, pero no se les setea resultadoFinal;
   ACTA-0008 esta CERRADA con COMPROBANTE_PAGO sin resultadoFinal.
   Esto produce sub-bandejas ambiguas (ANALISIS_NOTIF_POSITIVA o
   CERRADA_OTRA_CAUSA).
3. Atajos demo que rompen el modelo conceptual:
   - ACTA-0019 (ABSUELTO) y ACTA-0021 (PAGO_CONFIRMADO) precargan
     resultadoFinal en PENDIENTE_ANALISIS sin recorrer fallo +
     notificacion del fallo.
   - ACTA-0023 hace lo mismo y agrega un
     DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA EMITIDO sin firma.
4. Actas fallo / apelacion intercambiables: ACTA-0027 a ACTA-0030
   son estructuralmente iguales (PENDIENTE_ANALISIS limpio con
   INFORME_ALCOHOTEST). Solo el resumenHecho describe el destino
   demo. Para tests automatizados son redundantes.
5. Atajo ACTA-0022 con docs avanzados en D2:
   ACTAS_EN_ENRIQUECIMIENTO con MEDIDA_PREVENTIVA en
   PENDIENTE_FIRMA contradice la regla "la pieza de medida no se
   emite en D2".
6. Mocks puramente de regresion en la demo default: ACTA-0020
   (auxiliar de origen via generarMedidaPreventiva) y ACTA-0025
   (solo para ConstatacionMaterialTempranaEtapaIT) no aportan
   como demo.
7. ACTA-0043 notificacion postal POSITIVA atrapada en
   EN_NOTIFICACION (no recorrio el flujo de positiva).
8. Tipo legacy "FALLO" convive con FALLO_CONDENATORIO y
   FALLO_ABSOLUTORIO en mocks distintos. El clasificador acepta
   ambos en la rama FIRMA_FALLO_CONDENATORIO y en
   tieneFalloEnExpediente, pero ambos coexisten.
9. Cobertura de tests fuerte sobre 6 actas: ACTA-0006 (fallo y
   apelacion), ACTA-0024 (recorrido e2e), ACTA-0016 (acceso QR y
   pago voluntario), ACTA-0030 (correo postal), ACTA-0026 (medida
   posterior y cierre cerrable), ACTA-0005 (reintento). El resto
   del nucleo casi no se prueba con tests automatizados.

### Set minimo de mocks default propuesto

Se proponen 17 actas funcionales canonicas (ver seccion 5) que
cubren un caso por cada hito operativo del circuito de transito,
mas un caso por cada dependencia (INSPECCIONES, FISCALIZACION,
BROMATOLOGIA). El resto se reclasifica como MOVER_REGRESION o
MOVER_VOLUMEN_UX.

---

## 2. Inventario de mocks actuales

Notas sobre la tabla:

- Dependencia: la asignada explicitamente o, si esta vacia, la
  inferida por completarDependenciasDemo (TRANSITO por defecto,
  BROMATOLOGIA para tipo ESTABLECIMIENTO, rotacion modulo 8 para
  ACTA-0046 en adelante).
- Sub-bandeja primaria: la que produce
  SubBandejaClasificador.clasificar aplicada al estado precargado.
- Bloqueantes: pendientes bloqueantes de cierre vigentes
  (LEVANTAMIENTO_MEDIDA_PREVENTIVA, LIBERACION_RODADO,
  ENTREGA_DOCUMENTACION).
- Clasificacion: MANTENER, CORREGIR, MOVER_REGRESION,
  MOVER_VOLUMEN_UX o ELIMINAR.

### 2.1 Nucleo canonico (ACTA-0001 a ACTA-0017)

| ID | Dep. | Bandeja / Bloque / EstadoProceso | Sub-bandeja primaria | Documentos | Notificaciones | Bloqueantes | Accion principal | Clasificacion |
|---|---|---|---|---|---|---|---|---|
| ACTA-0001 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D2 / EN_CURSO | ENRIQUECIMIENTO_GENERAL | FOTO_INFRACCION (ADJUNTO) | -- | -- | Completar datos | MANTENER |
| ACTA-0002 | TRANSITO | PENDIENTE_PREPARACION_DOCUMENTAL / D3 / PENDIENTE_GENERACION | GENERACION_ACTA_PENDIENTE | -- | -- | -- | Generar acta | MANTENER |
| ACTA-0003 | TRANSITO | PENDIENTE_FIRMA / D3 / PENDIENTE_FIRMA | FIRMA_ACTA_INICIAL | BORRADOR_ACTA (PENDIENTE_FIRMA) + CONSTANCIA_RADAR | -- | -- | Firmar documento | MANTENER |
| ACTA-0004 | TRANSITO | PENDIENTE_NOTIFICACION / D4 / PENDIENTE_ENVIO | NOTIF_ACTA_LISTA_ENVIO | ACTA_FIRMADA | POSTAL PENDIENTE_ENVIO | -- | Gestionar notificacion | MANTENER |
| ACTA-0005 | TRANSITO | EN_NOTIFICACION / D4 / EN_ENVIO | NOTIF_EN_CORREO_POSTAL | (vacio) | POSTAL EN_TRAMITE | -- | Seguir envio | CORREGIR (sin ACTA_FIRMADA en docs) |
| ACTA-0006 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | INFORME_ALCOHOTEST | POSTAL ENTREGADA + EMAIL NO_APLICA | -- | Analizar expediente | MANTENER |
| ACTA-0007 | INSPECCIONES (override) | ARCHIVO / ARCHIVO / ARCHIVADA_OPERATIVA | ARCHIVO_DESDE_ANALISIS | RESOLUCION_ARCHIVO | POSTAL ENTREGADA | -- | Consultar / reingresar | MANTENER |
| ACTA-0008 | TRANSITO | CERRADAS / CERRADA / CERRADA | CERRADA_OTRA_CAUSA | COMPROBANTE_PAGO | POSTAL ENTREGADA | -- | Consultar | CORREGIR (falta resultadoFinal PAGO_CONFIRMADO) |
| ACTA-0009 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D2 / EN_CURSO | ENRIQUECIMIENTO_GENERAL | -- | -- | -- | Completar datos | MANTENER |
| ACTA-0010 | TRANSITO | ARCHIVO / ARCHIVO / ARCHIVADA_OPERATIVA | ARCHIVO_DESDE_ANALISIS | NOTA_INTERNA_DUPLICIDAD | -- | -- | Reingresar / consultar | MOVER_VOLUMEN_UX (duplica a ACTA-0007 sin notificacion) |
| ACTA-0011 | TRANSITO | PENDIENTES_RESOLUCION_REDACCION / D5 / PENDIENTE_RESOLUCION | REDACCION_RESOLUCION | ACTA_FIRMADA | POSTAL ENTREGADA | -- | Generar pieza | MANTENER |
| ACTA-0012 | TRANSITO | PENDIENTES_RESOLUCION_REDACCION / D5 / PENDIENTE_NULIDAD | REDACCION_NULIDAD | INFORME_VICIO_FORMAL | POSTAL ENTREGADA | -- | Generar pieza | MANTENER |
| ACTA-0013 | TRANSITO | PENDIENTES_RESOLUCION_REDACCION / D5 / PENDIENTE_MEDIDA_PREVENTIVA | REDACCION_MEDIDA | ACTA_RETENCION | -- | -- | Generar pieza | MANTENER |
| ACTA-0014 | FISCALIZACION (override) | PENDIENTES_RESOLUCION_REDACCION / D5 / PENDIENTE_RECTIFICACION | REDACCION_RECTIFICACION | ACTA_FIRMADA | POSTAL ENTREGADA | -- | Generar pieza | MANTENER |
| ACTA-0015 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_REVISION (accion DERIVAR_GESTION_EXTERNA) | ANALISIS_LISTO_DERIVAR_EXTERNA | ACTA_FIRMADA + FALLO (FIRMADO) | 2x POSTAL ENTREGADA (acta + fallo) | -- | Analizar / derivar | CORREGIR (falta resultadoFinal CONDENADO o CONDENA_FIRME) |
| ACTA-0016 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | ACTA_FIRMADA + FALLO (FIRMADO) | 2x POSTAL ENTREGADA | -- | Analizar | CORREGIR (falta resultadoFinal) |
| ACTA-0017 | TRANSITO | GESTION_EXTERNA / GESTION_EXTERNA / EN_GESTION_EXTERNA | EXT_JUZGADO_PAZ | ACTA_FIRMADA + FALLO | 2x POSTAL ENTREGADA | -- | Seguir gestion | CORREGIR (falta resultadoFinal CONDENA_FIRME) |

### 2.2 Pago voluntario y cerrabilidad material (ACTA-0018 a ACTA-0023)

| ID | Dep. | Bandeja / Bloque / EstadoProceso | Sub-bandeja primaria | Documentos | Notificaciones | Bloqueantes | Accion principal | Clasificacion |
|---|---|---|---|---|---|---|---|---|
| ACTA-0018 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D2 / EN_CURSO | ENRIQUECIMIENTO_GENERAL | -- | -- | -- | Completar datos / solicitar pago | MANTENER (uso para pago voluntario via API) |
| ACTA-0019 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_CIERRE_MATERIAL (resultadoFinal ABSUELTO precargado) | ANALISIS_BLOQUEO_OPERATIVO | MEDIDA_PREVENTIVA + ACTA_RETENCION + CONSTATACION_RETENCION_DOCUMENTACION | -- | LEVANTAMIENTO_MEDIDA_PREVENTIVA, LIBERACION_RODADO, ENTREGA_DOCUMENTACION | Resolver pendientes / gestionar bloqueos | CORREGIR (precarga ABSUELTO sin pasar por fallo + notificacion) |
| ACTA-0020 | TRANSITO | PENDIENTES_RESOLUCION_REDACCION / D5 / PENDIENTE_PRODUCCION_PIEZAS (mutado en carga via generarMedidaPreventiva) | REDACCION_MEDIDA o FIRMA_OTRAS_PIEZAS (segun momento) | (vacio al inicio; luego MEDIDA_PREVENTIVA PENDIENTE_FIRMA) | -- | LEVANTAMIENTO_MEDIDA_PREVENTIVA (origen via ancla) | Generar / firmar | MOVER_REGRESION (auxiliar de origen medida; no aporta como demo) |
| ACTA-0021 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_CIERRE_MATERIAL (resultadoFinal PAGO_CONFIRMADO precargado) | ANALISIS_BLOQUEO_OPERATIVO | MEDIDA_PREVENTIVA + ACTA_RETENCION + CONSTATACION_RETENCION_DOCUMENTACION | -- | mismos tres bloqueantes que 0019 | Resolver pendientes / gestionar bloqueos | CORREGIR (precarga PAGO_CONFIRMADO sin pago real) |
| ACTA-0022 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D2 / EN_CURSO | ENRIQUECIMIENTO_GENERAL (chip "Pendientes materiales") | MEDIDA_PREVENTIVA PENDIENTE_FIRMA + ACTA_RETENCION + CONSTATACION_RETENCION_DOCUMENTACION | -- | mismos tres bloqueantes | Pendientes materiales / gestionar bloqueos | CORREGIR (docs avanzados en D2) |
| ACTA-0023 | TRANSITO | PENDIENTE_ANALISIS / D5 / PENDIENTE_CIERRE_MATERIAL (resultadoFinal ABSUELTO precargado) | ANALISIS_BLOQUEO_OPERATIVO | MEDIDA_PREVENTIVA PENDIENTE_FIRMA + DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA EMITIDO | -- | LEVANTAMIENTO_MEDIDA_PREVENTIVA (hecho material pendiente) | Resolver pendientes | CORREGIR (precarga ABSUELTO; mezcla doc emitido sin firma) |

### 2.3 Recorridos demo e2e y casos minimos (ACTA-0024 a ACTA-0026)

| ID | Dep. | Bandeja / Bloque / EstadoProceso | Sub-bandeja primaria | Documentos | Notificaciones | Bloqueantes | Accion principal | Clasificacion |
|---|---|---|---|---|---|---|---|---|
| ACTA-0024 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D1 / EN_CURSO (ActaTransitoMock con 3 flags) | CAPTURA_INICIAL (chip "Pendientes materiales") | ACTA_RETENCION + CONSTATACION_RETENCION_DOCUMENTACION + MEDIDA_PREVENTIVA EMITIDO | -- | LEVANTAMIENTO_MEDIDA_PREVENTIVA, LIBERACION_RODADO, ENTREGA_DOCUMENTACION | Pendientes materiales | MANTENER (caso e2e oficial) |
| ACTA-0025 | TRANSITO | ACTAS_EN_ENRIQUECIMIENTO / D1 / EN_CURSO | CAPTURA_INICIAL | -- | -- | -- | Completar datos | MOVER_REGRESION (solo regresion temprana) |
| ACTA-0026 | BROMATOLOGIA (por ESTABLECIMIENTO) | PENDIENTE_ANALISIS / D5 / PENDIENTE_REVISION (resultadoFinal PAGO_CONFIRMADO precargado) | CONDENA_PAGO_CONFIRMADO o CONDENA_LISTO_CIERRE | ACTA_FIRMADA | -- | (vacio inicial; aparece tras registrarMedidaPreventivaPosterior) | Analizar / cerrar | MANTENER (unico mock no transito) |

### 2.4 Fallo / apelacion (ACTA-0027 a ACTA-0030)

Las cuatro se cargan via cargarActaFalloApelacionAnalisisLimpioDemo
con la misma estructura: PENDIENTE_ANALISIS, D5,
PENDIENTE_REVISION, dominio SEGURIDAD_VIAL, INFORME_ALCOHOTEST
adjunto, una notificacion POSTAL ENTREGADA y una EMAIL NO_APLICA,
TRANSITO explicito. El diferencial es solo el resumen del destino
demo.

| ID | Bandeja / EstadoProceso | Sub-bandeja primaria | Clasificacion |
|---|---|---|---|
| ACTA-0027 | PENDIENTE_ANALISIS / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | MANTENER (uno solo) |
| ACTA-0028 | PENDIENTE_ANALISIS / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | MOVER_VOLUMEN_UX |
| ACTA-0029 | PENDIENTE_ANALISIS / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | MOVER_VOLUMEN_UX |
| ACTA-0030 | PENDIENTE_ANALISIS / PENDIENTE_REVISION | ANALISIS_NOTIF_POSITIVA | MOVER_VOLUMEN_UX |

### 2.5 Notificador municipal (ACTA-0031 a ACTA-0033)

| ID | Bandeja / EstadoProceso | Tipo notificacion | Documento | Clasificacion |
|---|---|---|---|---|
| ACTA-0031 | PENDIENTE_NOTIFICACION (NOTIFICADOR_MUNICIPAL) | FALLO_CONDENATORIO | FALLO_CONDENATORIO FIRMADO | MANTENER |
| ACTA-0032 | PENDIENTE_NOTIFICACION (NOTIFICADOR_MUNICIPAL) | FALLO_ABSOLUTORIO | FALLO_ABSOLUTORIO FIRMADO | MANTENER |
| ACTA-0033 | PENDIENTE_NOTIFICACION (NOTIFICADOR_MUNICIPAL) | ACTA_INFRACCION | ACTA_FIRMADA | MANTENER |

### 2.6 Portal infractor / domicilio electronico (ACTA-0034 a ACTA-0036)

| ID | Tipo notificacion | Documento | Clasificacion |
|---|---|---|---|
| ACTA-0034 | ACTA_INFRACCION | ACTA_FIRMADA | MANTENER |
| ACTA-0035 | FALLO_CONDENATORIO | FALLO_CONDENATORIO | MANTENER |
| ACTA-0036 | FALLO_ABSOLUTORIO | FALLO_ABSOLUTORIO | MANTENER |

### 2.7 Correo postal listas para lote (ACTA-0037 a ACTA-0041)

Todas en PENDIENTE_NOTIFICACION, canal CORREO_POSTAL, estado
LISTA_PARA_ENVIO. Prioridades del clasificador en
PENDIENTE_NOTIFICACION: FALLO_CONDENATORIO = 10,
FALLO_ABSOLUTORIO = 15, ACTA = 20.

| ID | Tipo notificacion | Clasificacion |
|---|---|---|
| ACTA-0037 | FALLO_CONDENATORIO | MANTENER |
| ACTA-0038 | ACTA_INFRACCION | MOVER_VOLUMEN_UX |
| ACTA-0039 | FALLO_CONDENATORIO | MOVER_VOLUMEN_UX |
| ACTA-0040 | FALLO_ABSOLUTORIO | MANTENER |
| ACTA-0041 | ACTA_INFRACCION | MOVER_VOLUMEN_UX |

### 2.8 Correo postal "no candidatas" (ACTA-0042 a ACTA-0045)

Todas en EN_NOTIFICACION, canal CORREO_POSTAL, distintos
resultados ya registrados.

| ID | EstadoNotificacion | Resultado | Sub-bandeja primaria | Clasificacion |
|---|---|---|---|---|
| ACTA-0042 | ENVIADA | SIN_RESULTADO | NOTIF_EN_CORREO_POSTAL | MANTENER |
| ACTA-0043 | ENTREGADA | POSITIVA | NOTIF_EN_CORREO_POSTAL (positiva atrapada) | CORREGIR |
| ACTA-0044 | NEGATIVA | NEGATIVA | NOTIF_NEGATIVA_PENDIENTE_DECISION | MANTENER |
| ACTA-0045 | VENCIDA | VENCIDA | NOTIF_VENCIDA_PENDIENTE_DECISION | MANTENER |

### 2.9 Volumen UX (ACTA-0046 a ACTA-0107)

Aporta poblacion visual a cada bandeja para validar densidad UI.
Detalles agrupados por bloque (sin valor funcional propio):

| Rango | Macro-bandeja | Estado dominante | Clasificacion |
|---|---|---|---|
| 0046 a 0049 | ACTAS_EN_ENRIQUECIMIENTO | D1 / D2 / EN_CURSO; 0049 con SOLICITADO | MOVER_VOLUMEN_UX |
| 0050 a 0058 | PENDIENTE_PREPARACION_DOCUMENTAL | PENDIENTE_GENERACION / PENDIENTE_PIEZAS / REVISION_DOCUMENTAL | MOVER_VOLUMEN_UX |
| 0059 a 0067 | PENDIENTE_FIRMA | Distintos docs en PENDIENTE_FIRMA | MOVER_VOLUMEN_UX |
| 0068 a 0071 | PENDIENTE_NOTIFICACION (correo postal lista) | LISTA_PARA_ENVIO | MOVER_VOLUMEN_UX |
| 0072 a 0076 | EN_NOTIFICACION (correo) | Distintos resultados | MOVER_VOLUMEN_UX |
| 0077 a 0081 | PENDIENTES_RESOLUCION_REDACCION | Distintos PENDIENTE_* | MOVER_VOLUMEN_UX |
| 0082 a 0090 | GESTION_EXTERNA | EN_GESTION_EXTERNA (apremio o juzgado) | MOVER_VOLUMEN_UX |
| 0091 a 0098 | ARCHIVO | ARCHIVADA_OPERATIVA / ARCHIVADA_JURIDICA | MOVER_VOLUMEN_UX |
| 0099 a 0107 | CERRADAS | CERRADA con distintos resultadoFinal | CORREGIR (ver 2.9.1) |

#### 2.9.1 Inconsistencia del bloque "cerradas volumen"

cargarActaVolumenCerrada se invoca con un parametro
estaCerradaFlag. Las llamadas tienen las combinaciones:

- ACTA-0099: estaCerrada=false, permiteReingreso=false
- ACTA-0100: estaCerrada=false, permiteReingreso=true
- ACTA-0101: estaCerrada=false, permiteReingreso=true
- ACTA-0102: estaCerrada=false, permiteReingreso=true (doc NULIDAD)
- ACTA-0103: estaCerrada=false, permiteReingreso=false
- ACTA-0104: estaCerrada=false, permiteReingreso=true
- ACTA-0105: estaCerrada=true, permiteReingreso=false
- ACTA-0106: estaCerrada=true, permiteReingreso=true
- ACTA-0107: estaCerrada=true, permiteReingreso=true

Resultado: 6 actas en BANDEJA_CERRADAS (0099 a 0104) quedan con
estaCerrada=false. El clasificador es indiferente a la bandera
(clasifica por bandeja), pero validadores que filtren por
estaCerrada veran un estado contradictorio.

### 2.10 UX laterales (ACTA-0108 a ACTA-0116)

| ID | Macro-bandeja | Sub-bandeja primaria esperada | Clasificacion |
|---|---|---|---|
| ACTA-0108 | PENDIENTES_FALLO | FALLO_LISTO_CONDENATORIO | MANTENER |
| ACTA-0109 | PENDIENTES_FALLO (con INFORME_ALCOHOTEST) | FALLO_LISTO_ABSOLUTORIO | MANTENER |
| ACTA-0110 | PENDIENTES_FALLO (situacionPago PAGO_INFORMADO, accion VERIFICAR_PAGO_INFORMADO) | FALLO_TRAS_PAGO_INFORMADO | MANTENER |
| ACTA-0111 | CON_APELACION (apelacion no resuelta) | APELACION_PENDIENTE_RESOLUCION | MANTENER |
| ACTA-0112 | CON_APELACION (accion REVISION_APELACION) | APELACION_EN_ANALISIS | MANTENER |
| ACTA-0113 | CON_APELACION (resuelta, resultadoFinal ABSUELTO) | APELACION_RESUELTA | MANTENER |
| ACTA-0114 | PARALIZADAS (accion PARALIZACION_ESPERA_DOCUMENTAL) | PARALIZ_ESPERA_DOCUMENTAL | MANTENER |
| ACTA-0115 | PARALIZADAS (accion PARALIZACION_TRAMITE_EXTERNO) | PARALIZ_TRAMITE_EXTERNO | MANTENER |
| ACTA-0116 | PARALIZADAS (accion PARALIZACION_CAUSA_ADMINISTRATIVA) | PARALIZ_CAUSA_ADMINISTRATIVA | MANTENER |

### 2.11 Resumen de clasificacion por categoria

| Categoria | Cantidad aprox. | IDs (ejemplos) |
|---|---|---|
| MANTENER | ~30 | 0001-0004, 0006, 0007, 0009, 0011-0014, 0018, 0024, 0026, 0027, 0031-0036, 0037, 0040, 0042, 0044, 0045, 0108-0116 |
| CORREGIR | ~12 | 0005, 0008, 0015, 0016, 0017, 0019, 0021, 0022, 0023, 0043, 0099-0104 |
| MOVER_REGRESION | 2 | 0020, 0025 |
| MOVER_VOLUMEN_UX | resto | 0010, 0028, 0029, 0030, 0038, 0039, 0041, 0046-0098, 0105-0107 |
| ELIMINAR | 0 | -- |

MANTENER no exige que el mock no tenga arreglos: significa que
conceptualmente debe estar en el set demo default. CORREGIR
significa que el mock aporta algo unico pero tiene drift detectable.

---

## 3. Incoherencias detectadas

### 3.1 ACTA-0005 - EN_NOTIFICACION sin documento de acta firmada

- Bandeja EN_NOTIFICACION, canal POSTAL EN_TRAMITE.
- documentosPorActa queda vacio.
- Los eventos describen firma + envio, pero no hay
  ACTA_FIRMADA (FIRMADO) en docs.
- Impacto: una vista que liste documentos del expediente ve una
  notificacion en curso sin pieza notificable.
- Correccion: agregar ACTA_FIRMADA (FIRMADO).

### 3.2 ACTA-0008 - cerrada sin resultadoFinal

- Bandeja CERRADAS, estaCerrada=true.
- COMPROBANTE_PAGO adjunto y notificacion entregada.
- resultadoFinalPorActa queda vacio: el clasificador genera
  CERRADA_OTRA_CAUSA (prioridad 90) en lugar de
  CERRADA_PAGO_VOLUNTARIO (10).
- Correccion: setResultadoFinalCierreDemo(id, PAGO_CONFIRMADO).

### 3.3 ACTA-0015 / ACTA-0016 / ACTA-0017 - fallo notificado sin resultadoFinal

- Las tres tienen FALLO (FIRMADO) + 2 notificaciones ENTREGADA
  (acta y fallo).
- Ninguna setea resultadoFinalPorActa.
- ACTA-0015 tiene accion DERIVAR_GESTION_EXTERNA, que
  conceptualmente exige CONDENA_FIRME (plazo vencido sin
  apelacion).
- ACTA-0016 parece "post-notif-fallo, ventana abierta" pero no
  setea plazoApelacionAbiertoPorActa.
- ACTA-0017 (gestion externa) supone CONDENA_FIRME ya consumida.
- Correccion: setear CONDENADO o CONDENA_FIRME segun el caso. En
  ACTA-0016 considerar tambien marcar plazo abierto.

### 3.4 ACTA-0019 / ACTA-0021 / ACTA-0023 - resultadoFinal precargado sin recorrer fallo + notificacion

- Setean ABSUELTO, PAGO_CONFIRMADO y ABSUELTO respectivamente sin
  documentos FALLO_* y sin notificacion del fallo.
- Sirven para probar cerrabilidad material rapida pero rompen el
  modelo "no se llega a un resultado sin recorrer fallo +
  notificacion" en la rama no-pago.
- Correccion (opciones):
  - Documentar explicitamente como atajo demo y mantener.
  - Convertirlos en flujo ortodoxo agregando fallo y notificacion
    entregada antes del resultado.

### 3.5 ACTA-0022 - documentos avanzados en D2

- ACTAS_EN_ENRIQUECIMIENTO, D2_ENRIQUECIMIENTO, pero
  MEDIDA_PREVENTIVA (PENDIENTE_FIRMA) precargada.
- La pieza de medida preventiva se produce en
  PENDIENTES_RESOLUCION_REDACCION / D5_ANALISIS.
- Funciona porque la cerrabilidad sincroniza origenes via ancla,
  pero rompe la coherencia bloque/documento.
- Correccion: usar CONSTATACION_MEDIDA_PREVENTIVA_APLICABLE
  (ADJUNTO) como ancla y reservar MEDIDA_PREVENTIVA
  (PENDIENTE_FIRMA) para post produccion de pieza.

### 3.6 ACTA-0043 - notificacion postal positiva atrapada en EN_NOTIFICACION

- Estado ENTREGADA, resultado POSITIVA, pero la bandeja sigue
  siendo EN_NOTIFICACION.
- En el flujo real, una positiva mueve la acta a
  PENDIENTE_ANALISIS via registrarNotificacionPositiva.
- Correccion: mover a PENDIENTE_ANALISIS, o documentar como
  "trazabilidad postal" explicito sin recorrer flujo.

### 3.7 ACTA-0099 a ACTA-0104 - estaCerrada=false en bandeja CERRADAS

- 6 actas en CERRADAS con la bandera del record en false.
- El clasificador no se entera, pero
  bandejaHabilitaResolucionBloqueoCierre chequea actaCerrada
  antes que bandeja, lo que podria permitir operaciones invalidas.
- Correccion: cambiar estaCerradaFlag a true en
  cargarActaVolumenCerrada para destinos CERRADAS con
  resultadoFinal definido.

### 3.8 ACTA-0027 a ACTA-0030 - cuatro mocks intercambiables

- Mismo helper, mismo estado, sin diferencia estructural.
- Solo el resumen describe el destino demo.
- Para tests automatizados son redundantes.
- Correccion: mantener uno (p. ej. ACTA-0027) como canonico y
  mover los demas a regresion / UX manual.

### 3.9 ACTA-0010 - archivo por duplicidad sin notificacion previa

- Bandeja ARCHIVO, motivo ARCHIVO_DESDE_ANALISIS_DIRECTO, sin
  notificacion previa, sin doc resolutorio.
- Coherente con "archivo por duplicidad" pero duplica
  funcionalmente a ACTA-0007 sin aportar variedad.

### 3.10 Tipo documento FALLO (legacy) vs FALLO_CONDENATORIO / FALLO_ABSOLUTORIO

- ACTA-0015 / 0016 / 0017 y volumen 0082+ usan tipo FALLO.
- Mocks mas nuevos (0027+) y flujos via API usan
  FALLO_CONDENATORIO o FALLO_ABSOLUTORIO.
- El clasificador acepta ambos (tieneFalloEnExpediente),
  evaluarPendienteFirma trata FALLO_CONDENATORIO y FALLO
  conjuntamente.
- Riesgo: dos tipos para la misma cosa generan ambiguedad en
  filtros / reportes.
- Correccion: migrar tipos FALLO legacy a FALLO_CONDENATORIO.

### 3.11 ACTA-0024 - ancla MEDIDA_PREVENTIVA EMITIDO al nacimiento

- En la carga, con flag medidaPreventivaAplicable, se agrega un
  doc tipo TIPO_ANCLA_MEDIDA_PREVENTIVA en estado EMITIDO.
- Comparte tipoDocumento con la pieza producida en analisis.
- Diluye la separacion conceptual entre constatacion temprana y
  pieza producida.
- Correccion: usar un tipoDocumento distinto para el ancla
  (CONSTATACION_MEDIDA_PREVENTIVA_APLICABLE).

---

## 4. Analisis del clasificador y presenter

### 4.1 SubBandejaClasificador

#### Estrategia general

- Punto unico de clasificacion. Entrada: SubBandejaContexto (acta,
  accion pendiente, situaciones de pago, docs, notificaciones,
  piezas requeridas, plazos, apelacion).
- Por cada bandeja agrega candidatos a una lista de pares
  (SubBandejaCodigo, prioridad).
- Gana el de prioridad numerica mas baja. En empate respeta orden
  de insercion.
- Fallback general: ANALISIS_REVISION_GENERAL.

#### Reglas por bandeja

- ACTAS_EN_ENRIQUECIMIENTO:
  - bloqueActual D1_CAPTURA => CAPTURA_INICIAL.
  - situacionPago SOLICITADO / PAGO_INFORMADO / OBSERVADO, o
    accion EVALUAR_PAGO_VOLUNTARIO => PAGO_VOLUNTARIO_ORIGINADO.
  - Default ENRIQUECIMIENTO_GENERAL.
- PENDIENTE_PREPARACION_DOCUMENTAL:
  - estadoProcesoActual PENDIENTE_GENERACION =>
    GENERACION_ACTA_PENDIENTE.
  - Piezas pendientes => GENERACION_PIEZAS_PENDIENTE.
  - Default REVISION_DOCUMENTAL.
- PENDIENTE_FIRMA: clasifica por tipoDocumento del documento
  pendiente: fallo condenatorio (10), fallo absolutorio (15),
  acta (20), otras piezas (90). Tipo legacy FALLO entra en la rama
  condenatorio.
- PENDIENTE_NOTIFICACION: prioridad fallo condenatorio (10),
  absolutorio (15), acta (20), otro (90), siempre con
  NOTIF_LISTA_OTRO al final.
- EN_NOTIFICACION:
  - Notificacion negativa => NOTIF_NEGATIVA_PENDIENTE_DECISION.
  - Notificacion vencida => NOTIF_VENCIDA_PENDIENTE_DECISION.
  - Otros: por canal (correo postal, notificador municipal,
    domicilio electronico, otro).
- PENDIENTE_ANALISIS: ver 4.1.1.
- PENDIENTES_RESOLUCION_REDACCION: por estado / pieza pendiente.
- PENDIENTES_FALLO:
  - PAGO_INFORMADO o accion VERIFICAR_PAGO_INFORMADO =>
    FALLO_TRAS_PAGO_INFORMADO (10).
  - INFORME_ALCOHOTEST en docs => FALLO_LISTO_ABSOLUTORIO (15).
  - Default FALLO_LISTO_CONDENATORIO (90).
- CON_APELACION:
  - accion REVISION_APELACION => APELACION_EN_ANALISIS.
  - resultadoFinal ABSUELTO o CONDENA_FIRME =>
    APELACION_RESUELTA.
  - Default APELACION_PENDIENTE_RESOLUCION.
- PARALIZADAS: por accion PARALIZACION_*.
- GESTION_EXTERNA: por tipoGestionExterna (APREMIO,
  JUZGADO_DE_PAZ) y permiteReingreso, default EXT_SEGUIMIENTO.
- ARCHIVO: por motivoArchivo, estado ARCHIVADA_JURIDICA,
  permiteReingreso, default ARCHIVO_OPERATIVO.
- CERRADAS: por resultadoFinal, doc / estado NULIDAD,
  permiteReingreso, default CERRADA_OTRA_CAUSA.

#### 4.1.1 Logica de PENDIENTE_ANALISIS

- pendientesBloqueantes no vacio => ANALISIS_BLOQUEO_OPERATIVO
  (prioridad 5: la mas alta de la bandeja).
- cerrable=true => CONDENA_LISTO_CIERRE (10).
- situacionPagoCondena CONFIRMADO / INFORMADO / OBSERVADO =>
  candidatos CONDENA_PAGO_*. Si hay CONDENA_FIRME o CONDENADO sin
  pago aun => CONDENA_PAGO_PENDIENTE_INFORMAR.
- accionPendiente:
  - DERIVAR_GESTION_EXTERNA => ANALISIS_LISTO_DERIVAR_EXTERNA (35).
  - DECISION_NOTIF_VENCIDA => ANALISIS_NOTIF_VENCIDA.
  - DECISION_NOTIF_NEGATIVA => ANALISIS_NOTIF_NEGATIVA.
  - POST_GESTION_EXTERNA / POST_REINGRESO => candidatos
    correspondientes.
  - VERIFICAR_PAGO_INFORMADO => ANALISIS_PAGO_INFORMADO (44).
- situacionPago PAGO_INFORMADO => ANALISIS_PAGO_INFORMADO.
- situacionPago SOLICITADO => ANALISIS_PAGO_SOLICITADO.
- tieneFalloEnExpediente=false y requiereFallo=true =>
  ANALISIS_PENDIENTE_FALLO (52).
- Notificacion positiva (acta o fallo) => ANALISIS_NOTIF_POSITIVA
  (50).
- Default ANALISIS_REVISION_GENERAL (90).

#### 4.1.2 Casos borde detectados

- ACCION_DERIVAR_GESTION_EXTERNA + notif positiva: agrega ambos
  candidatos; gana 35. Coherente con ACTA-0015.
- Fallo en expediente y notif positiva del fallo, sin
  resultadoFinal: tieneFalloEnExpediente=true bloquea
  ANALISIS_PENDIENTE_FALLO; gana ANALISIS_NOTIF_POSITIVA (50).
  Vale para ACTA-0016 y para ACTA-0027 a 0030 luego de notif
  positiva del fallo si nadie setea resultadoFinal.
  Observacion: la notificacion entregada precargada en
  ACTA-0027 a 0030 es del acta (no del fallo); el clasificador no
  distingue uno u otro.
- PAGO_INFORMADO + pendientesBloqueantes activos: gana bloqueo
  (5) frente a pago informado (44). Decision de diseno.
- ANALISIS_NOTIF_POSITIVA (50) gana sobre
  ANALISIS_PENDIENTE_FALLO (52) cuando ambos aplican. Significa
  que la UI prioriza "notificada positiva" antes que "pendiente
  de fallo" cuando la positiva es la del acta.

### 4.2 ActaBandejaUxPresenter

#### Funciones

- chipVisible: humaniza el chip tecnico de la sub-bandeja.
- accionPrincipalVisible: refina el texto de accion principal.

#### Reglas

- Si pendientesBloqueantes no esta vacio Y bandeja es
  ACTAS_EN_ENRIQUECIMIENTO Y la sub-bandeja es CAPTURA_INICIAL o
  ENRIQUECIMIENTO_GENERAL: chip => "Pendientes materiales".
- Si chip matchea regex tecnico (D[0-9]): traduce por bloque
  ("Captura inicial", "Enriquecimiento", "Completitud documental",
  "Analisis").
- Si chip contiene "fallo" / "apelacion" / "paraliz" sin estar
  humanizado: intenta humanizarlo.
- accionPrincipalVisible: si hay bloqueantes => "Gestionar
  bloqueos" o "Resolver pendientes" segun el tipo de pendiente
  (medida / rodado / documentacion).
- En el resto, refina segun sub-bandeja.

#### Tensiones

- "Pendientes materiales" solo aparece en
  ACTAS_EN_ENRIQUECIMIENTO. En otras bandejas con bloqueos
  materiales, el chip primario sigue intacto y solo la accion se
  ajusta a "Resolver pendientes".
- humanizarChip solo cambia textos especificos. Por ejemplo "Pago
  informado" no se altera.
- accionPrincipalVisible no distingue entre "pendiente material"
  y "pendiente documental" mas alla del texto.

#### Mocks afectados por chip especial

- ACTA-0019, 0021, 0022, 0023, 0024: tienen bloqueantes; en D2 /
  CAPTURA muestran chip "Pendientes materiales"; en
  PENDIENTE_ANALISIS conservan ANALISIS_BLOQUEO_OPERATIVO como
  primario.

---

## 5. Propuesta de mocks default minimos

Objetivo: cubrir cada hito del circuito con un mock unico,
coherente con la spec, mas un caso por dependencia. Donde los
mocks MANTENER actuales ya cumplen, se reusan; donde haya
corrupcion o falten estados, se proponen mocks nuevos.

### 5.1 Conjunto propuesto (transito)

| ID propuesto | Hito | Bandeja / Bloque / EstadoProceso | Docs requeridos | Docs que NO deben estar | Notificaciones requeridas | Notificaciones que NO deben estar | accionPendiente | resultadoFinal | situacionPago | Sub-bandeja esperada | Proxima accion operativa |
|---|---|---|---|---|---|---|---|---|---|---|---|
| MIN-001 | Captura inicial D1 | ACTAS_EN_ENRIQUECIMIENTO / D1_CAPTURA / EN_CURSO | FOTO_INFRACCION (ADJUNTO) opcional | ACTA_FIRMADA, FALLO_*, MEDIDA_* | -- | cualquiera | -- | -- | SIN_PAGO | CAPTURA_INICIAL | Completar datos -> D2 |
| MIN-002 | Pendiente preparacion documental | PENDIENTE_PREPARACION_DOCUMENTAL / D3_DOCUMENTAL / PENDIENTE_GENERACION | -- | ACTA_FIRMADA, BORRADOR_ACTA | -- | cualquiera | -- | -- | SIN_PAGO | GENERACION_ACTA_PENDIENTE | Generar acta |
| MIN-003 | Pendiente firma acta inicial | PENDIENTE_FIRMA / D3_DOCUMENTAL / PENDIENTE_FIRMA | BORRADOR_ACTA (PENDIENTE_FIRMA), CONSTANCIA_RADAR (ADJUNTO) | ACTA_FIRMADA, FALLO_* | -- | cualquiera | -- | -- | SIN_PAGO | FIRMA_ACTA_INICIAL | Firmar documento |
| MIN-004 | Pendiente notificacion acta | PENDIENTE_NOTIFICACION / D4_NOTIFICACION / PENDIENTE_ENVIO | ACTA_FIRMADA (FIRMADO) | FALLO_* en cualquier estado | POSTAL LISTA_PARA_ENVIO tipo ACTA_INFRACCION | ENTREGADA / NEGATIVA / VENCIDA | -- | -- | SIN_PAGO | NOTIF_ACTA_LISTA_ENVIO | Generar lote / enviar |
| MIN-005 | Notificacion positiva (plazo vigente) | PENDIENTE_ANALISIS / D5_ANALISIS / PENDIENTE_REVISION | ACTA_FIRMADA | FALLO_* | POSTAL ENTREGADA POSITIVA tipo ACTA_INFRACCION | NEGATIVA / VENCIDA | -- | -- | SIN_PAGO | ANALISIS_NOTIF_POSITIVA | Analizar |
| MIN-006 | Notificacion negativa | EN_NOTIFICACION / D4_NOTIFICACION / EN_ENVIO | ACTA_FIRMADA | FALLO_* | POSTAL resultado NEGATIVA | POSITIVA / VENCIDA | -- | -- | SIN_PAGO | NOTIF_NEGATIVA_PENDIENTE_DECISION | Decidir reintento / archivo |
| MIN-007 | Notificacion vencida | EN_NOTIFICACION / D4_NOTIFICACION / EN_ENVIO | ACTA_FIRMADA | FALLO_* | POSTAL resultado VENCIDA | POSITIVA / NEGATIVA | -- | -- | SIN_PAGO | NOTIF_VENCIDA_PENDIENTE_DECISION | Decidir reintento / archivo |
| MIN-008 | Pago voluntario informado | PENDIENTE_ANALISIS / D5_ANALISIS / PENDIENTE_REVISION | ACTA_FIRMADA + COMPROBANTE_PAGO_INFORMADO (ADJUNTO) | FALLO_* | POSTAL ENTREGADA POSITIVA | NEGATIVA / VENCIDA | VERIFICAR_PAGO_INFORMADO | -- | PAGO_INFORMADO | ANALISIS_PAGO_INFORMADO | Verificar pago |
| MIN-009 | Pendiente fallo (sin alcohotest) | PENDIENTES_FALLO / D5_ANALISIS / PENDIENTE_REVISION | ACTA_FIRMADA | FALLO_*, INFORME_ALCOHOTEST | POSTAL ENTREGADA POSITIVA | -- | -- | -- | SIN_PAGO | FALLO_LISTO_CONDENATORIO | Emitir fallo condenatorio |
| MIN-010 | Fallo condenatorio en firma | PENDIENTE_FIRMA / D3_DOCUMENTAL / PENDIENTE_FIRMA | ACTA_FIRMADA + FALLO_CONDENATORIO (PENDIENTE_FIRMA) | FALLO_ABSOLUTORIO | POSTAL ENTREGADA del acta | notificacion del fallo | -- | -- | SIN_PAGO | FIRMA_FALLO_CONDENATORIO | Firmar fallo |
| MIN-011 | Fallo lista para notificar | PENDIENTE_NOTIFICACION / D4_NOTIFICACION / PENDIENTE_ENVIO | ACTA_FIRMADA + FALLO_CONDENATORIO (FIRMADO) | FALLO_ABSOLUTORIO | POSTAL ENTREGADA del acta + POSTAL LISTA_PARA_ENVIO del fallo | ENTREGADA del fallo | -- | -- | SIN_PAGO | NOTIF_FALLO_CONDENATORIO_LISTA | Generar lote |
| MIN-012 | Apelacion pendiente resolucion | CON_APELACION / D5_ANALISIS / PENDIENTE_REVISION | ACTA_FIRMADA + FALLO_CONDENATORIO (FIRMADO) | -- | 2x POSTAL ENTREGADA (acta + fallo) | -- | -- | CONDENADO | SIN_PAGO | APELACION_PENDIENTE_RESOLUCION | Resolver apelacion |
| MIN-013 | Condena firme + pago condena pendiente | PENDIENTE_ANALISIS / D5_ANALISIS / PENDIENTE_REVISION | ACTA_FIRMADA + FALLO_CONDENATORIO (FIRMADO) | COMPROBANTE_PAGO_CONDENA | 2x POSTAL ENTREGADA | -- | -- | CONDENA_FIRME | SIN_PAGO (situacionPagoCondena PENDIENTE) | CONDENA_PAGO_PENDIENTE_INFORMAR | Informar pago condena |
| MIN-014 | Gestion externa (apremio) | GESTION_EXTERNA / GESTION_EXTERNA / EN_GESTION_EXTERNA | ACTA_FIRMADA + FALLO_CONDENATORIO (FIRMADO) | -- | 2x POSTAL ENTREGADA | -- | -- | CONDENA_FIRME | -- | EXT_APREMIO | Seguir gestion / reingresar |
| MIN-015 | Paralizada (espera documental) | PARALIZADAS / D5_ANALISIS / PARALIZADA | ACTA_FIRMADA | -- | POSTAL ENTREGADA | -- | PARALIZACION_ESPERA_DOCUMENTAL | -- | SIN_PAGO | PARALIZ_ESPERA_DOCUMENTAL | Levantar paralizacion |
| MIN-016 | Archivo con reingreso | ARCHIVO / ARCHIVO / ARCHIVADA_OPERATIVA | RESOLUCION_ARCHIVO | -- | POSTAL ENTREGADA | -- | -- | -- | -- | ARCHIVO_DESDE_ANALISIS / ARCHIVO_REINGRESO_PERMITIDO | Reingresar acta |
| MIN-017 | Cerrada por pago voluntario | CERRADAS / CERRADA / CERRADA (estaCerrada=true) | COMPROBANTE_PAGO | -- | POSTAL ENTREGADA | -- | -- | PAGO_CONFIRMADO | CONFIRMADO | CERRADA_PAGO_VOLUNTARIO | Consultar |

### 5.2 Conjunto propuesto por dependencia

| ID propuesto | Dependencia | Notas |
|---|---|---|
| MIN-INSP-001 | INSPECCIONES | Acta de clausura en ACTAS_EN_ENRIQUECIMIENTO / D2; sin transito. Permite probar filtros por dependencia. |
| MIN-FISC-001 | FISCALIZACION | Acta en PENDIENTE_ANALISIS con dominio fiscalizatorio (sin transito), notif positiva, sin fallo. |
| MIN-BROM-001 | BROMATOLOGIA | Reutiliza ACTA-0026 (contravencion BROMATOLOGIA) o un mock con flujo de medida posterior. |

### 5.3 Mapeo con mocks existentes

Muchos MIN-* coinciden con mocks actuales MANTENER:

- MIN-001 ~= ACTA-0001 (con foto)
- MIN-002 ~= ACTA-0002
- MIN-003 ~= ACTA-0003
- MIN-004 ~= ACTA-0004
- MIN-005 ~= ACTA-0027 (post-positiva limpio)
- MIN-006 ~= ACTA-0044 (correo postal negativa)
- MIN-007 ~= ACTA-0045 (correo postal vencida)
- MIN-009 ~= ACTA-0108 (FALLO_LISTO_CONDENATORIO)
- MIN-014 ~= ACTA-0017 (corregido con resultadoFinal CONDENA_FIRME)
- MIN-015 ~= ACTA-0114 (PARALIZ_ESPERA_DOCUMENTAL)
- MIN-016 ~= ACTA-0007 (archivo con reingreso)
- MIN-017 ~= ACTA-0008 (corregido con resultadoFinal PAGO_CONFIRMADO)
- MIN-BROM-001 ~= ACTA-0026

Faltan crear o reciclar:

- MIN-008 (pago voluntario informado en analisis sin bloqueos).
- MIN-010 (fallo condenatorio en firma).
- MIN-011 (fallo firmado en notificacion).
- MIN-012 (apelacion pendiente resolucion, hoy ACTA-0111 podria
  servir pero no tiene fallo en docs).
- MIN-013 (condena firme + pago pendiente, sin equivalente).
- MIN-INSP-001 (no existe acta INSPECCIONES inicial limpia hoy;
  ACTA-0007 esta archivada).
- MIN-FISC-001 (no existe acta FISCALIZACION inicial limpia hoy;
  ACTA-0014 esta en rectificacion).

---

## 6. Propuesta de tests por tramo

Convencion: un IT por tramo principal. Solo se enumeran tests
faltantes o que conviene reorganizar; los existentes permanecen.

### 6.1 Captura / enriquecimiento -> firma

- Clase: EnriquecimientoFirmaTramoIT.
- Mock inicial: MIN-001 (o ACTA-0001).
- Acciones HTTP: completar datos (D2 -> D3), generar pieza /
  acta, firmar.
- Assertions:
  - Bandeja recorre ENRIQUECIMIENTO -> PREPARACION ->
    FIRMA -> NOTIFICACION.
  - Doc pasa PENDIENTE_FIRMA -> FIRMADO.
- Cobertura actual: ninguna directa (no hay test de transicion
  D2 -> D3 -> firma encadenada).

### 6.2 Firma -> notificacion

- Clase: FirmaNotificacionTramoIT.
- Mock inicial: MIN-003 (o ACTA-0003).
- Acciones HTTP: firmar-documento sobre el borrador / acta.
- Assertions:
  - Doc pasa FIRMADO.
  - Bandeja pasa PENDIENTE_FIRMA -> PENDIENTE_NOTIFICACION.
  - Sub-bandeja en destino: NOTIF_ACTA_LISTA_ENVIO.
- Cobertura actual: parcial via
  ResolucionBloqueoCierreOperativaIT.levantamientoCircuitoFirmaNotif_*.

### 6.3 Notificacion positiva -> analisis

- Clase: NotificacionPositivaAnalisisTramoIT.
- Mock inicial: MIN-004 / ACTA-0004.
- Acciones HTTP: registrar-notificacion-positiva (del acta).
- Assertions:
  - Bandeja => PENDIENTE_ANALISIS.
  - Sub-bandeja => ANALISIS_NOTIF_POSITIVA.
- Cobertura actual: existe en
  SubBandejaDinamicaIT.acta0004TrasNotificacionPositiva_*.

### 6.4 Pago voluntario -> cierre

- Clase: PagoVoluntarioCierreTramoIT.
- Mock inicial: ACTA-0018 + ACTA-0024 (con bloqueos).
- Acciones HTTP: registrar-solicitud-pago-voluntario,
  registrar-pago-informado, adjuntar-comprobante-pago-informado,
  confirmar-pago-informado, resolutorios materiales,
  cerrar-acta.
- Assertions:
  - resultadoFinal => PAGO_CONFIRMADO.
  - cerrable => true.
  - Cierre OK.
- Cobertura actual: DemoRecorridoPuntaAPuntaActa0024IT.

### 6.5 Analisis -> fallo

- Clase: AnalisisFalloTramoIT.
- Mock inicial: MIN-009 (sin alcohotest) y ACTA-0006 (con
  alcohotest).
- Acciones HTTP: dictar-fallo-condenatorio /
  dictar-fallo-absolutorio.
- Assertions:
  - Doc creado en PENDIENTE_FIRMA.
  - Bandeja => PENDIENTE_FIRMA.
- Cobertura actual: parcial en FalloYPlazoApelacionIT.

### 6.6 Fallo -> firma del fallo

- Clase: FirmaFalloTramoIT.
- Mock inicial: MIN-010.
- Acciones HTTP: firmar-documento sobre el fallo.
- Assertions:
  - Doc fallo FIRMADO.
  - Bandeja => PENDIENTE_NOTIFICACION.
  - Sub-bandeja => NOTIF_FALLO_CONDENATORIO_LISTA o
    NOTIF_FALLO_ABSOLUTORIO_LISTA.
- Cobertura actual: parcial en FalloYPlazoApelacionIT.

### 6.7 Firma fallo -> notificacion fallo

- Clase: NotificacionFalloTramoIT.
- Mock inicial: MIN-011 (fallo firmado en PENDIENTE_NOTIFICACION).
- Acciones HTTP: registrar-notificacion-positiva del fallo.
- Assertions:
  - resultadoFinal => CONDENADO / ABSUELTO.
  - En condenatorio: plazo de apelacion abierto.
- Cobertura actual: cubierta en FalloYPlazoApelacionIT.

### 6.8 Notificacion fallo -> apelacion / condena firme

- Clase: ApelacionOCondenaFirmeTramoIT.
- Mock inicial: MIN-012 (apelacion presentada) + MIN-009 con
  recorrido completo para vencimiento.
- Acciones HTTP: registrar-apelacion, resolver-apelacion,
  registrar-vencimiento-plazo-apelacion.
- Assertions:
  - Estado y resultado final esperados.
  - Vista ciudadana / portal infractor.
- Cobertura actual: RegistrarApelacionIT, ResolverApelacionIT,
  FalloYPlazoApelacionIT.registrarVencimientoPlazoApelacion_*.

### 6.9 Condena firme -> pago condena -> cierre

- Clase: CondenaFirmePagoCondenaCierreTramoIT.
- Mock inicial: MIN-013.
- Acciones HTTP: registrar-pago-condena, confirmar-pago-condena,
  cerrar-acta.
- Assertions:
  - situacionPagoCondena pasa PENDIENTE -> INFORMADO ->
    CONFIRMADO.
  - Cierre OK.
- Cobertura actual: PostCondenaFirmeIT.

### 6.10 Gestion externa -> reingreso

- Clase: GestionExternaReingresoTramoIT.
- Mock inicial: ACTA-0015 (listo derivar) + MIN-014.
- Acciones HTTP: derivar-a-gestion-externa,
  reingresar-desde-gestion-externa.
- Assertions:
  - Transiciones de bandeja.
  - tipoGestionExterna asignado.
  - accionPendiente post reingreso.
- Cobertura actual: parcial; no hay test e2e dedicado.

### 6.11 Paralizacion -> reactivacion

- Clase: ParalizacionReactivacionTramoIT.
- Mock inicial: ACTA-0114 / 0115 / 0116.
- Acciones HTTP: levantar-paralizacion.
- Assertions:
  - Bandeja vuelve a la previa (analisis u otra).
  - accionPendiente PARALIZACION_* desaparece.
- Cobertura actual: ninguna.

### 6.12 Archivo -> reingreso / cierre definitivo

- Clase: ArchivoReingresoTramoIT.
- Mock inicial: ACTA-0007 (con reingreso) + volumen sin reingreso
  (ACTA-0093 / 0097).
- Acciones HTTP: reingresar-acta-desde-archivo.
- Assertions:
  - Bandeja destino.
  - motivoArchivo previo conservado.
  - accionPendiente post reingreso.
- Cobertura actual: parcial en
  ArchivarActaCerrableRechazoIT.reingresarActa_desdeArchivoConPermiteReingreso_*.

---

## 7. Plan de implementacion por slices

Cada slice es funcional, autocontenido y con criterio de
completitud explicito. El orden busca: limpiar -> corregir ->
agregar lo faltante -> separar regresion -> opcional volumen.

### Slice 0 - Limpieza inicial

- Objetivo: que la demo default deje de cargar mocks que solo
  sirven para regresion / poblacion UX.
- Acciones tecnicas:
  - Aislar cargarActa0020* y cargarActa0025* en un metodo aparte
    invocado solo por tests (perfil Spring o flag).
  - Aislar cargarActasVolumenDemo y cargarActasBandejasUxDemo
    detras de una property (p. ej.
    prototipo.demo.volumen-ux.habilitado) con default false.
- Criterio de completitud: tras reset sin flag, getActas().size()
  cae al nucleo funcional (~30). Con flag activo permanece en
  116.

### Slice 1 - Correccion de mocks CORREGIR

Micro-pasos, cada uno verificable:

- 1.a Setear resultadoFinal en ACTA-0008 (PAGO_CONFIRMADO),
  ACTA-0015 (CONDENA_FIRME), ACTA-0016 (CONDENADO o
  CONDENA_FIRME), ACTA-0017 (CONDENA_FIRME).
- 1.b Setear estaCerrada=true en ACTA-0099, 0100, 0101, 0102,
  0103, 0104 (o cerrar el bloque entero en slice 0).
- 1.c Agregar ACTA_FIRMADA (FIRMADO) a ACTA-0005.
- 1.d Decidir politica para ACTA-0019 / 0021 / 0023:
  - Opcion A: comentar como atajo demo y mantener.
  - Opcion B: convertir a flujo ortodoxo (agregar fallo +
    notificacion del fallo antes del resultado).
- 1.e Separar ancla y pieza en ACTA-0022: ancla pasa a
  CONSTATACION_MEDIDA_PREVENTIVA_APLICABLE (ADJUNTO), pieza
  MEDIDA_PREVENTIVA (PENDIENTE_FIRMA) sale del estado D2.
- 1.f ACTA-0043: mover a PENDIENTE_ANALISIS o documentar como
  "trazabilidad correo postal".
- Criterio de completitud: ningun mock CORREGIR queda con drift
  documentado; tests existentes siguen verdes.

### Slice 2 - Creacion de mocks faltantes del set minimo

- Verificar que los mocks MIN-001 a MIN-007 se cumplen con
  ACTA-0001 a ACTA-0004 + ACTA-0027 + ACTA-0044 + ACTA-0045.
- Crear MIN-008 (pago voluntario informado en analisis sin
  bloqueos): nuevo mock con doc COMPROBANTE_PAGO_INFORMADO y
  situacionPago PAGO_INFORMADO.
- Crear MIN-010 (fallo condenatorio en firma).
- Crear MIN-011 (fallo firmado en notificacion).
- Crear MIN-012 (apelacion pendiente resolucion con fallo en
  docs y resultadoFinal CONDENADO).
- Crear MIN-013 (condena firme + pago pendiente informar).
- Crear MIN-INSP-001 (acta INSPECCIONES inicial limpia).
- Crear MIN-FISC-001 (acta FISCALIZACION inicial limpia).
- Criterio de completitud: cada hito del circuito y cada
  dependencia tiene al menos un mock canonico.

### Slice 3 - Tests de integracion por tramo faltantes

- Implementar EnriquecimientoFirmaTramoIT,
  FirmaNotificacionTramoIT, NotificacionFalloTramoIT,
  GestionExternaReingresoTramoIT, ParalizacionReactivacionTramoIT,
  ArchivoReingresoTramoIT.
- Si se decide reducir 0027 a 0030 a un solo mock, adaptar
  ActasMockFalloApelacionDemoIT y CorreoPostalNotificacionIT.
- Criterio de completitud: cada tramo de la seccion 6 tiene al
  menos un test que recorre la transicion principal y valida
  estado final.

### Slice 4 - Mover mocks MOVER_REGRESION a archivo separado

- Crear MockDataFactoryRegresion (o equivalente) con ACTA-0020 y
  ACTA-0025.
- Invocar este archivo solo en perfiles de test (perfil Spring o
  property).
- Criterio de completitud: ningun mock cuyo unico consumidor sea
  un test sigue en la demo default.

### Slice 5 - Mover mocks MOVER_VOLUMEN_UX detras de flag

- Aislar cargarActasVolumenDemo* y cargarActasBandejasUxDemo*
  detras de prototipo.demo.volumen-ux.habilitado.
- Perfil "demo presentacion": con flag.
- Perfil "demo funcional": sin flag.
- Criterio de completitud: el escenario funcional minimo carga
  ~30 mocks; el escenario UI carga 116.

### Slice 6 - Decision sobre ACTA-0027 a ACTA-0030

- Confirmar con stakeholders si los cuatro mocks son necesarios
  como demo manual.
- Si no: dejar uno y mover el resto.
- Adaptar tests dependientes (CorreoPostalNotificacionIT usa
  ACTA-0028 y ACTA-0030 como vehiculo para dictar fallo).

### Slice 7 - Migracion de tipo legacy FALLO a FALLO_CONDENATORIO

- Reemplazar tipoDocumento FALLO por FALLO_CONDENATORIO en
  ACTA-0015 / 0016 / 0017 y en volumen 0082+.
- Validar clasificacion equivalente.
- Criterio de completitud: cero usos del tipo legacy FALLO en
  mocks.

### Slice 8 - Continuidad documental

- Una vez ejecutados slices 0 a 7, actualizar el documento de
  continuidad correspondiente (fuera del alcance de este
  diagnostico) con un resumen de cambios y deudas residuales.

---

## Apendice A - mapping clasificador inverso

Sirve como referencia rapida para chequear cobertura de cada
SubBandejaCodigo en el set demo completo.

| SubBandejaCodigo | Mock(s) que la disparan hoy |
|---|---|
| CAPTURA_INICIAL | ACTA-0024, ACTA-0025, ACTA-0046, ACTA-0047 |
| PAGO_VOLUNTARIO_ORIGINADO | ACTA-0049 (SOLICITADO) |
| ENRIQUECIMIENTO_GENERAL | ACTA-0001, 0009, 0018, 0022, 0048 |
| GENERACION_ACTA_PENDIENTE | ACTA-0002, 0050, 0051, 0052 |
| GENERACION_PIEZAS_PENDIENTE | ACTA-0053, 0054, 0055 |
| REVISION_DOCUMENTAL | ACTA-0056, 0057, 0058 |
| FIRMA_FALLO_CONDENATORIO | ACTA-0062, 0063 |
| FIRMA_FALLO_ABSOLUTORIO | ACTA-0064, 0065 |
| FIRMA_ACTA_INICIAL | ACTA-0003, 0059, 0060, 0061 |
| FIRMA_OTRAS_PIEZAS | ACTA-0066 (RESOLUCION), 0067 (NULIDAD) |
| NOTIF_FALLO_CONDENATORIO_LISTA | ACTA-0031, 0035, 0037, 0039 |
| NOTIF_FALLO_ABSOLUTORIO_LISTA | ACTA-0032, 0036, 0040, 0070 |
| NOTIF_ACTA_LISTA_ENVIO | ACTA-0004, 0033, 0034, 0038, 0041, 0068, 0069, 0071 |
| NOTIF_NEGATIVA_PENDIENTE_DECISION | ACTA-0044, 0075 |
| NOTIF_VENCIDA_PENDIENTE_DECISION | ACTA-0045, 0076 |
| NOTIF_EN_CORREO_POSTAL | ACTA-0005, 0042, 0043, 0072, 0073, 0074 |
| NOTIF_EN_NOTIFICADOR_MUNICIPAL | -- (no hay mock inicial en EN_NOTIFICACION con ese canal) |
| NOTIF_EN_DOMICILIO_ELECTRONICO | -- |
| NOTIF_EN_OTRO_CANAL | -- |
| ANALISIS_BLOQUEO_OPERATIVO | ACTA-0019, 0021, 0023 (y 0022 con chip especial) |
| CONDENA_LISTO_CIERRE | ACTA-0026 (post resolucion / cumplimiento) |
| CONDENA_PAGO_CONFIRMADO | ACTA-0026 (resultadoFinal PAGO_CONFIRMADO) |
| CONDENA_PAGO_OBSERVADO | -- (solo via API) |
| CONDENA_PAGO_INFORMADO | -- |
| CONDENA_PAGO_PENDIENTE_INFORMAR | -- (requiere CONDENA_FIRME + situacion PENDIENTE) |
| ANALISIS_LISTO_DERIVAR_EXTERNA | ACTA-0015 |
| ANALISIS_NOTIF_VENCIDA | -- |
| ANALISIS_NOTIF_NEGATIVA | -- |
| ANALISIS_POST_GESTION_EXTERNA | -- (solo via reingreso) |
| ANALISIS_POST_REINGRESO | -- (solo via reingreso) |
| ANALISIS_PAGO_INFORMADO | -- (en PENDIENTE_ANALISIS) |
| ANALISIS_PAGO_SOLICITADO | -- |
| ANALISIS_PENDIENTE_FALLO | -- (las que llegan a analisis o ya tienen fallo, o no lo dispara) |
| ANALISIS_NOTIF_POSITIVA | ACTA-0006, 0016, 0027, 0028, 0029, 0030 |
| ANALISIS_REVISION_GENERAL | fallback |
| REDACCION_NULIDAD | ACTA-0012, 0079 |
| REDACCION_MEDIDA | ACTA-0013, 0020, 0080 |
| REDACCION_RECTIFICACION | ACTA-0014, 0081 |
| REDACCION_RESOLUCION | ACTA-0011, 0077, 0078 |
| REDACCION_GENERAL | fallback |
| FALLO_TRAS_PAGO_INFORMADO | ACTA-0110 |
| FALLO_LISTO_ABSOLUTORIO | ACTA-0109 |
| FALLO_LISTO_CONDENATORIO | ACTA-0108 |
| APELACION_EN_ANALISIS | ACTA-0112 |
| APELACION_RESUELTA | ACTA-0113 |
| APELACION_PENDIENTE_RESOLUCION | ACTA-0111 |
| PARALIZ_ESPERA_DOCUMENTAL | ACTA-0114 |
| PARALIZ_TRAMITE_EXTERNO | ACTA-0115 |
| PARALIZ_CAUSA_ADMINISTRATIVA | ACTA-0116 |
| EXT_APREMIO | ACTA-0082, 0083, 0084, 0085, 0090 |
| EXT_JUZGADO_PAZ | ACTA-0017, 0086, 0087, 0088, 0089 |
| EXT_PENDIENTE_REINGRESO | varios con permiteReingreso |
| EXT_SEGUIMIENTO | fallback |
| ARCHIVO_POST_VENCIMIENTO | ACTA-0096, 0097 |
| ARCHIVO_DESDE_ANALISIS | ACTA-0007, 0010, 0091, 0092, 0093 |
| ARCHIVO_JURIDICO | ACTA-0094, 0095 |
| ARCHIVO_REINGRESO_PERMITIDO | varios con permiteReingreso |
| ARCHIVO_DEFINITIVO | ACTA-0093, 0095, 0097 (sin reingreso) |
| ARCHIVO_OPERATIVO | ACTA-0098 |
| CERRADA_PAGO_VOLUNTARIO | ACTA-0099, 0105 |
| CERRADA_PAGO_CONDENA | ACTA-0100, 0106 |
| CERRADA_ABSOLUCION | ACTA-0101, 0107 |
| CERRADA_NULIDAD | ACTA-0102 |
| CERRADA_ARCHIVO_DEFINITIVO | ACTA-0099, 0103, 0105 (sin reingreso) |
| CERRADA_OTRA_CAUSA | ACTA-0008 (sin resultadoFinal), 0103, 0104 |

Gaps inmediatos:

- NOTIF_EN_NOTIFICADOR_MUNICIPAL, NOTIF_EN_DOMICILIO_ELECTRONICO,
  NOTIF_EN_OTRO_CANAL: ningun mock inicial.
- ANALISIS_NOTIF_VENCIDA, ANALISIS_NOTIF_NEGATIVA,
  ANALISIS_POST_GESTION_EXTERNA, ANALISIS_POST_REINGRESO,
  ANALISIS_PAGO_INFORMADO (en PENDIENTE_ANALISIS),
  ANALISIS_PAGO_SOLICITADO, ANALISIS_PENDIENTE_FALLO: ningun mock
  inicial.
- CONDENA_PAGO_PENDIENTE_INFORMAR, CONDENA_PAGO_INFORMADO,
  CONDENA_PAGO_OBSERVADO: ningun mock inicial.

---

## Apendice B - Decisiones que merecen confirmacion con stakeholders

1. ACTA-0019 / 0021 / 0023 (atajos demo de cerrabilidad
   material): mantener o convertir en flujo ortodoxo?
2. ACTA-0022 (anclas de retencion + medida en D2): permitir como
   dato del expediente o reservar a etapa posterior?
3. ACTA-0027 a 0030 (cuatro mocks identicos): mantener los
   cuatro o reducir a uno?
4. Volumen UX 0046 a 0107: en perfil demo default o solo en
   perfil de presentacion?
5. Tipo legacy FALLO: deprecarlo definitivamente o mantener
   compatibilidad?
6. ACTA-0043 (correo postal entregado positivo en
   EN_NOTIFICACION): "trazabilidad postal" o mover a
   PENDIENTE_ANALISIS?

---

## Apendice C - Resumen de soportes funcionales en el store

Referencia rapida sobre que pone cada soporte en el estado del
mock cuando se invoca:

| Soporte | Area | Que aporta |
|---|---|---|
| ArchivoReingresoSupport | Archivo / reingreso | archivo directo, post evaluacion, reingreso desde archivo, motivoArchivo, eventos. |
| NotificacionSupport | Notificacion (acta) | positiva, negativa, vencida, reintento por no entrega, reintento post vencimiento, estados internos, destinatario demo. |
| CerrabilidadSupport | Cerrabilidad | resultadoFinal, pendientes bloqueantes, origenes materiales, cumplimiento material, flag cerrable. |
| PiezasFirmaSupport | Piezas / firma | medida preventiva, notificacion del acta, nulidad, firma individual, transicion firma -> notificacion (o cierre por nulidad). |
| GestionExternaSupport | Gestion externa | derivar (apremio, juzgado), reingreso desde gestion externa, tipoGestionExterna por acta. |
| PagoVoluntarioSupport | Pago voluntario | solicitud temprana, monto, marca accion EVALUAR_PAGO_VOLUNTARIO. |
| PagoInformadoSupport | Pago informado | registrar pago, adjuntar comprobante, confirmar, observar, situacionPago, accion VERIFICAR_PAGO_INFORMADO. |
| PagoCondenaSupport | Pago condena | informar, confirmar, observar pago condena; situacionPagoCondena. |
| CierreSupport | Cierre | validacion final y cierre efectivo (estaCerrada=true, bandeja CERRADAS). |
| FalloPlazoApelacionSupport | Fallo / plazo apelacion | dictar fallo absolutorio / condenatorio, notificacion de fallo (positiva), plazo de apelacion, vencimiento, apelacion presentada y resuelta. |
| CorreoPostalNotificacionSupport | Correo postal | listas para lote, generar lote CSV, anular lote, enviar individual, procesar respuesta CSV, trazabilidad. |
| NotificadorMunicipalSupport | Notificador municipal | listar, registrar acuse positivo / negativo / vencido. |

Cada soporte mantiene coherencia interna; la fuente de drift mas
habitual es la precarga directa en MockDataFactory que setea
bandeja / estado / resultadoFinal sin pasar por estos soportes.

---

Fin del documento.

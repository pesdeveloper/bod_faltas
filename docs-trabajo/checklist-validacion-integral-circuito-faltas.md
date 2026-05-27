# Checklist maestro — validación integral del circuito de Dirección de Faltas

## 1. Objetivo

Este documento sirve para validar el **circuito completo de actas** del sistema de Dirección de Faltas, de punta a punta: bandejas UX, filtros operativos, transiciones, eventos, documentos, notificaciones, cerrabilidad, canales externos (correo postal, portal infractor, notificador municipal) y coherencia con la spec.

No reemplaza la spec (`spec/`). La complementa como **plan de prueba manual** sobre el prototipo descartable en memoria y la demo Angular.

**Fuentes de verdad para actas demo y bandejas backend:** `MockDataFactory.java`, `SubBandejaCodigo.java`, `REGLAS_UX_DEMO_FALTAS.md`.

**Mapping UX lateral ↔ backend (no mostrar bandejas crudas en lateral):**

| Bandeja UX | Bandejas backend agregadas |
|------------|----------------------------|
| Actas en enriquecimiento | `ACTAS_EN_ENRIQUECIMIENTO`, `PENDIENTE_PREPARACION_DOCUMENTAL` |
| Análisis / presentaciones / pagos | `PENDIENTE_ANALISIS` |
| Pendientes de resolución / redacción | `PENDIENTES_RESOLUCION_REDACCION` |
| Pendientes de fallo | `PENDIENTES_FALLO` |
| Pendientes de firma | `PENDIENTE_FIRMA` |
| Notificaciones | `PENDIENTE_NOTIFICACION`, `EN_NOTIFICACION` |
| Con apelación | `CON_APELACION` |
| Gestión externa | `GESTION_EXTERNA` |
| Paralizadas | `PARALIZADAS` |
| Archivo | `ARCHIVO` |
| Cerradas | `CERRADAS` |

---

## 2. Reglas de validación

1. **Una acta / un circuito por vez.** No mezclar hallazgos de casos distintos en la misma fila de observaciones.
2. **Copiar estado antes:** bandeja UX, filtro operativo, `id` acta, bloque/estado procesal, acción pendiente, documentos visibles, último evento, notificaciones, cerrabilidad.
3. **Ejecutar una sola acción** (botón del detalle, menú operativo, API demo o canal externo según el caso).
4. **Copiar estado después** con los mismos campos.
5. **Verificar en este orden:**
   - aparece en la bandeja UX esperada (y no en lateral como bandeja backend cruda);
   - filtro operativo / sub-bandeja correctos;
   - detalle: resumen, acción habilitada/deshabilitada, mensajes;
   - documentos: tipo, estado, PDF mock;
   - eventos: nuevo evento append-only con tipo y bloques coherentes;
   - notificaciones: canal, estado, resultado;
   - cerrabilidad: coincide con reglas de cierre/archivo (no confundir archivo con cerrada).
6. **Clasificar el hallazgo** en Observaciones con una sola etiqueta principal:
   - `[MOCK]` — falta acta o estado precargado; documentar antes de pedir otro mock;
   - `[FUNC]` — regla de negocio no implementada o incorrecta;
   - `[BE]` — bug backend prototipo;
   - `[FE]` — bug Angular demo;
   - `[UX]` — problema visual o de usabilidad (no altera regla funcional);
   - `[OK]` — validado.
7. **No mezclar** bugs UX con reglas funcionales: si la transición es correcta pero el badge se ve mal, son dos filas o dos etiquetas.
8. **Si falta mock:** anotar estado faltante y **no** crear otro mock en el mismo slice de validación; abrir slice de mocks aparte.
9. **Encoding:** cualquier carácter corrupto (mojibake) se registra como `[UX]` o `[FE]` según origen visible.
10. **Reset:** ante duda, reiniciar backend prototipo y repetir desde estado inicial documentado para esa acta.

### Dependencia como eje transversal de validación

Decisión UX/funcional: **Dependencia** no define un circuito ni una bandeja propia; es un eje de acotación operativa que cruza todas las bandejas UX.

- **Dependencia no es bandeja.** No debe figurar como entrada lateral ni sustituir una bandeja UX del menú. Validar que no se trate como si fuera otra bandeja del circuito.
- **Dependencia es dimensión operativa transversal.** Organiza el trabajo por unidad u órgano responsable y aplica sobre cualquier bandeja UX y filtro operativo activos.
- **Combinación obligatoria.** Debe funcionar siempre combinada con **bandeja UX** y **filtro operativo** (sub-bandeja); ninguno de los tres reemplaza a los demás.
- **Datos demo.** Toda acta del dataset demo debe tener una dependencia real asignada; no usar `-` como valor por defecto, salvo el caso explícito documentado **Sin dependencia**.
- **Resumen de bandeja.** El resumen o panel de la bandeja activa debe mostrar conteos agregados por dependencia (totales por cada dependencia presente en esa vista).
- **Interacción.** Un clic sobre una dependencia en ese resumen debe aplicar (o alternar) el filtro de dependencia sobre el listado actual, sin cambiar de bandeja UX.
- **Filtros combinados.** Los criterios visibles se aplican de forma conjuntiva (AND):
  - bandeja UX;
  - filtro operativo;
  - dependencia;
  - búsqueda.

### Columnas de las tablas

| Columna | Uso |
|---------|-----|
| Check | Marcar `[x]` cuando el caso quedó validado |
| Caso / circuito | Nombre corto del escenario |
| Acta demo | Id preferido (`ACTA-00xx`) |
| Estado inicial esperado | Bandeja backend, bloque, estado, filtro operativo si aplica |
| Acción esperada | Acción única a ejecutar |
| Estado final esperado | Tras la acción |
| Bandeja destino esperada | Bandeja UX (y filtro si aplica) |
| Resultado | `OK` / `FALLO` / `PENDIENTE` / `N/A` |
| Observaciones | Etiqueta + detalle breve |

---

## 3. Checklist por bandeja UX

### 3.1 Actas en enriquecimiento

Incluye captura/labrado, enriquecimiento general y preparación documental (backend absorbido en esta entrada lateral).

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Captura inicial en curso | ACTA-0046 | `ACTAS_EN_ENRIQUECIMIENTO`, D1, EN_CURSO, filtro captura/labrado | Completar datos de labrado / enriquecimiento | D2, datos completos | Actas en enriquecimiento | | |
| [ ] | Enriquecimiento general (domicilio) | ACTA-0001 | `ACTAS_EN_ENRIQUECIMIENTO`, D2, EN_CURSO | Actualizar datos infractor / domicilio | D2, enriquecimiento avanzado | Actas en enriquecimiento | | |
| [ ] | Enriquecimiento con pago solicitado | ACTA-0049 | D2, pago `SOLICITADO` | Ver detalle pago / filtro pago voluntario | Sin cambio de bandeja hasta confirmar | Actas en enriquecimiento | | |
| [ ] | Pago voluntario informado | ACTA-0018 | D2, pago informado | Confirmar u observar pago (según UI) | Pago confirmado u observado | Análisis o enriquecimiento según regla | | |
| [ ] | Recorrido e2e tránsito + pago | ACTA-0024 | D2, anclas tránsito | Registrar solicitud pago voluntario → informar → confirmar | Pago confirmado, medidas reconocidas | Análisis / Cerradas según cierre | | |
| [ ] | Preparación documental (generación acta) | ACTA-0002 | `PENDIENTE_PREPARACION_DOCUMENTAL`, D3, PENDIENTE_GENERACION | Generar borrador acta | Borrador generado | Actas en enriquecimiento (filtro preparación) | | |
| [ ] | Preparación con constancia | ACTA-0050 | PENDIENTE_GENERACION, volumen | Generar acta | PENDIENTE_FIRMA (backend) | Pendientes de firma | | |
| [ ] | Constatación temprana (POST) | ACTA-0025 | D2, sin anclas tránsito al nacimiento | POST constatación temprana | Hecho material registrado | Actas en enriquecimiento | | |

### 3.2 Análisis / presentaciones / pagos

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Análisis post notificación positiva | ACTA-0006 | `PENDIENTE_ANALISIS`, D5, fallo absolutorio perfil | Emitir fallo absolutorio o derivar a fallo | Fallo emitido o pendiente fallo | Pendientes de fallo | | |
| [ ] | Pago voluntario — informar | ACTA-0018 | D5/D2 según mock, pago informado | Verificar / confirmar pago | PAGO_CONFIRMADO | Análisis | | |
| [ ] | Pago confirmado + cerrabilidad | ACTA-0021 | D5, pago confirmado, medidas pendientes reconocimiento | Reconocer medida / rodado / documentación → cerrar | CERRADA o ARCHIVO según bloqueos | Cerradas / Archivo | | |
| [ ] | Absolución + hechos materiales | ACTA-0019 | D5, perfil absolución | Reconocer liberaciones → cerrar | CERRADA | Cerradas | | |
| [ ] | Separación expediente / hecho material | ACTA-0023 | D5, resolutorio medida sin hecho material | Intentar cierre | Bloqueo cerrabilidad visible | Análisis | | |
| [ ] | Listo derivación gestión externa | ACTA-0015 | D5, `ACCION_DERIVAR_GESTION_EXTERNA` | Derivar a apremio o Juzgado de Paz | `GESTION_EXTERNA` | Gestión externa | | Ver si derivación efectiva está modelada |
| [ ] | Post fallo — ventana no cumplida | ACTA-0016 | D5, fallo notificado reciente | No debe ofrecer derivación externa | Sin `ACCION_DERIVAR` | Análisis | | |
| [ ] | Contravención local/comercio | ACTA-0026 | D5, sin tránsito | Medida en trámite / análisis | Según acción | Análisis | | |
| [ ] | Fallo condenatorio + apelación portal | ACTA-0027 | D5, fallo condenatorio | Presentar apelación vía portal | Apelación registrada | Con apelación | | |
| [ ] | Vencimiento plazo apelación | ACTA-0029 | D5, plazo apelación | Ejecutar vencimiento mock | Condena firme / sin apelación | Análisis | | |

### 3.3 Pendientes de resolución / redacción

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Resolución administrativa | ACTA-0011 | `PENDIENTES_RESOLUCION_REDACCION`, pieza resolución | Redactar / firmar resolución | Pendiente firma o notificación | Pendientes de firma / Notificaciones | | |
| [ ] | Nulidad | ACTA-0012 | Redacción nulidad | Completar nulidad | Documento generado | Pendientes de firma | | |
| [ ] | Medida preventiva (redacción) | ACTA-0013 | Redacción medida | Generar medida preventiva | Documento + notificación | Notificaciones | | |
| [ ] | Rectificación | ACTA-0014 | Redacción rectificación | Completar rectificación | Según circuito | Notificaciones / Análisis | | |
| [ ] | Medida preventiva (enganche piezas) | ACTA-0020 | Redacción, enlace ACTA-0019 | Generar medida desde expediente relacionado | Medida generada | Notificaciones | | |
| [ ] | Segunda resolución en bandeja | ACTA-0078 | Volumen redacción | Abrir y verificar listado | Sin acción | Pendientes de resolución / redacción | | |

### 3.4 Pendientes de fallo

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Fallo condenatorio pendiente | ACTA-0108 | `PENDIENTES_FALLO`, notificación acta positiva | Emitir fallo condenatorio | Fallo generado pendiente firma | Pendientes de firma | | |
| [ ] | Fallo absolutorio pendiente | ACTA-0109 | PENDIENTES_FALLO, perfil absolutorio | Emitir fallo absolutorio | Fallo generado | Pendientes de firma | | |
| [ ] | Fallo tras pago informado | ACTA-0110 | PENDIENTES_FALLO, `VERIFICAR_PAGO_INFORMADO` | Verificar pago → emitir fallo | Fallo + pago verificado | Pendientes de firma | | |

### 3.5 Pendientes de firma

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Firma de acta (borrador) | ACTA-0003 | `PENDIENTE_FIRMA`, borrador acta | Firmar acta | ACTA_FIRMADA | Notificaciones | | |
| [ ] | Firma pieza volumen | ACTA-0056 | PENDIENTE_FIRMA, volumen | Firmar | FIRMADO | Notificaciones | | |
| [ ] | Firma de fallo | ACTA-0108 | Fallo pendiente firma (tras emitir) | Firmar fallo | Fallo firmado | Notificaciones | | |

### 3.6 Notificaciones

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Pendiente envío postal (acta) | ACTA-0004 | `PENDIENTE_NOTIFICACION`, POSTAL PENDIENTE_ENVIO | Armar envío / lote o canal alternativo | EN_NOTIFICACION o candidata correo | Notificaciones | | |
| [ ] | En trámite postal | ACTA-0005 | `EN_NOTIFICACION`, EN_TRAMITE | Registrar resultado demo | ENTREGADA / NEGATIVA / VENCIDA | Notificaciones → Análisis | | |
| [ ] | Notificación inicial — portal | ACTA-0034 | PENDIENTE_NOTIFICACION, DOMICILIO_ELECTRONICO | Visualizar en portal / confirmar | ENTREGADA | Análisis | | |
| [ ] | Notificación inicial — notificador | ACTA-0033 | PENDIENTE_NOTIFICACION, NOTIFICADOR | Acuse positivo en notificador demo | ENTREGADA | Análisis | | |
| [ ] | Postal positiva | ACTA-0043 | Correo postal, ENTREGADA | Procesar lote / resultado | D5 análisis habilitado | Análisis | | |
| [ ] | Postal negativa | ACTA-0044 | Resultado negativo | Ver transición | Según regla | Notificaciones / Análisis | | |
| [ ] | Postal vencida | ACTA-0045 | Vencida sin respuesta | Ver transición | Según regla | Notificaciones | | |
| [ ] | Notificación de fallo — postal | ACTA-0037 | Fallo listo lote postal | Generar lote → procesar | Fallo notificado | Análisis | | |
| [ ] | Fallo en notificador municipal | ACTA-0031 | EN_NOTIFICACION, notificador | Acuse en notificador | ENTREGADA | Análisis / Con apelación | | |

### 3.7 Con apelación

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Apelación pendiente resolución | ACTA-0111 | `CON_APELACION`, apelación presentada | Resolver apelación | Resolución emitida | Con apelación (resuelta) o Análisis | | |
| [ ] | Apelación en análisis | ACTA-0112 | `REVISION_APELACION` | Completar revisión | Resolución | Con apelación | | |
| [ ] | Apelación resuelta absuelve | ACTA-0113 | ACEPTADA_ABSUELVE | Cerrar o archivar según medidas | CERRADA / ARCHIVO | Cerradas / Archivo | | |
| [ ] | Apelación presencial | ACTA-0028 | D5 + fallo | Registrar apelación presencial | CON_APELACION | Con apelación | | |
| [ ] | Resolución ACEPTADA_ABSUELVE (circuito largo) | ACTA-0030 | Fallo + apelación + resolución precargada | Verificar estado y cierre | Absuelto / cerrable | Cerradas | | |

### 3.8 Gestión externa

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Ya en Juzgado de Paz | ACTA-0017 | `GESTION_EXTERNA`, Juzgado de Paz | Reingreso desde gestión externa | D5 o ARCHIVO | Análisis / Archivo | | |
| [ ] | Derivación desde análisis (apremio) | ACTA-0015 | D5, listo derivar | Derivar apremio | GESTION_EXTERNA APREMIO | Gestión externa | | |
| [ ] | Volumen gestión externa | ACTA-0082+ | GESTION_EXTERNA, volumen | Listar filtros por tipo | — | Gestión externa | | |

### 3.9 Paralizadas

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Paralización espera documental | ACTA-0114 | `PARALIZADAS`, `PARALIZACION_ESPERA_DOCUMENTAL` | Levantar paralización (si existe acción) | ACTIVA, bandeja previa | Análisis / Enriquecimiento | | |
| [ ] | Paralización trámite externo | ACTA-0115 | `PARALIZACION_TRAMITE_EXTERNO` | Levantar paralización | ACTIVA | Según origen | | |
| [ ] | Paralización causa administrativa | ACTA-0116 | `PARALIZACION_CAUSA_ADMINISTRATIVA` | Levantar paralización | ACTIVA | Según origen | | |

### 3.10 Archivo

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Archivo operativo (multa paga, medida activa) | ACTA-0007 | `ARCHIVO`, ARCHIVADA_OPERATIVA | Reconocer liberación pendiente → cerrar | CERRADA si sin bloqueos | Cerradas | | |
| [ ] | Archivo por duplicidad | ACTA-0010 | ARCHIVO, motivo duplicidad | Reingreso (si habilitado) | ACTIVA en bandeja origen | Análisis / Enriquecimiento | | |
| [ ] | Archivo post absolución con pendientes | ACTA-0019 | Tras absolución sin cerrar todo | Completar reconocimientos → cerrar | CERRADA | Cerradas | | |
| [ ] | Reingreso desde archivo | ACTA-0007 / 0010 | ARCHIVO, `permiteReingreso` | Reingresar por evento | Bandeja operativa | Según motivo reingreso | | |

### 3.11 Cerradas

| Check | Caso / circuito | Acta demo | Estado inicial esperado | Acción esperada | Estado final esperado | Bandeja destino esperada | Resultado | Observaciones |
|:-----:|-----------------|-----------|-------------------------|-----------------|----------------------|---------------------------|-----------|---------------|
| [ ] | Cierre por pago sin medidas | ACTA-0008 | `CERRADAS` | Solo consulta (sin reabrir editando estado) | Permanece CERRADA | Cerradas | | |
| [ ] | Cierre e2e desde ACTA-0024 | ACTA-0024 | Recorrido completo | Cerrar expediente | CERRADA | Cerradas | | |
| [ ] | Cierre por absolución | ACTA-0019 | Tras reconocimientos | Cerrar | CERRADA | Cerradas | | |
| [ ] | Cierre por pago confirmado | ACTA-0021 / 0022 | PAGO_CONFIRMADO + liberaciones | Cerrar | CERRADA | Cerradas | | |
| [ ] | Solo lectura — no reingreso directo | ACTA-0008 | CERRADAS | Intentar acción de edición de estado | Debe exigir evento de reingreso | Cerradas | | |

---

## 4. Circuitos transversales obligatorios

Cada fila debe tener **al menos una acta demo** validada con evento + documentos + bandeja coherentes. Usar acta indicada o anotar sustituto en Observaciones.

| Check | Circuito transversal | Acta demo referencia | Acción / verificación mínima | Evento esperado (tipo) | Documentos esperados | Bandeja UX tras hito |
|:-----:|----------------------|----------------------|------------------------------|------------------------|----------------------|----------------------|
| [ ] | Captura / labrada | ACTA-0046 | Alta desde captura | ALTA | Foto / adjuntos iniciales | Enriquecimiento |
| [ ] | Enriquecimiento / completitud documental | ACTA-0001 | Completar datos | ACTUALIZACION_DATOS | Adjuntos | Enriquecimiento |
| [ ] | Preparación documental | ACTA-0002 | Generar borrador | DOCUMENTO_GENERADO | BORRADOR_ACTA | Enriquecimiento → Firma |
| [ ] | Firma de acta | ACTA-0003 | Firmar | FIRMA_COMPLETADA | ACTA_FIRMADA | Notificaciones |
| [ ] | Notificación inicial — correo postal | ACTA-0004 / 0038 | Lote o envío postal | PENDIENTE_NOTIFICACION / envío | ACTA_FIRMADA + notif POSTAL | Notificaciones |
| [ ] | Notificación inicial — domicilio electrónico | ACTA-0034 | Portal infractor | Visualización / entrega | ACTA_FIRMADA | Notificaciones → Análisis |
| [ ] | Notificación inicial — notificador municipal demo | ACTA-0033 | Acuse notificador | NOTIFICACION_ENTREGADA | ACTA_FIRMADA | Análisis |
| [ ] | Notificación positiva | ACTA-0043 / 0006 | Resultado ENTREGADA | NOTIFICACION_ENTREGADA | Constancia notif | Análisis |
| [ ] | Notificación negativa | ACTA-0044 | Resultado negativo | Según regla | Notif con resultado | Notificaciones |
| [ ] | Notificación vencida | ACTA-0045 | Vencimiento | Según regla | Notif vencida | Notificaciones |
| [ ] | Análisis por pago voluntario | ACTA-0018 | Ver expediente con pago | — | Comprobante mock | Análisis / Enriquecimiento |
| [ ] | Informar pago voluntario | ACTA-0024 | POST informar pago | PAGO_INFORMADO | Comprobante | Enriquecimiento / Análisis |
| [ ] | Confirmar pago voluntario | ACTA-0022 | Confirmar pago | PAGO_CONFIRMADO | Comprobante confirmado | Análisis |
| [ ] | Rechazar/observar pago voluntario | ACTA-0018 | Observar pago (si UI expone) | OBSERVACION_PAGO | — | Enriquecimiento |
| [ ] | Redacción resolución no-fallo | ACTA-0011 | Redactar resolución | RESOLUCION | Resolución PDF | Redacción → Firma |
| [ ] | Nulidad | ACTA-0012 | Redactar nulidad | NULIDAD / documento | Pieza nulidad | Redacción |
| [ ] | Medida preventiva | ACTA-0013 / 0020 | Generar medida | MEDIDA_PREVENTIVA | Documento medida | Notificaciones |
| [ ] | Rectificación | ACTA-0014 | Rectificar | RECTIFICACION | Documento | Redacción |
| [ ] | Fallo condenatorio | ACTA-0108 / 0027 | Emitir fallo | FALLO_EMITIDO | FALLO_CONDENATORIO | Firma → Notificaciones |
| [ ] | Fallo absolutorio | ACTA-0109 / 0006 | Emitir fallo | FALLO_EMITIDO | FALLO_ABSOLUTORIO | Firma → Análisis |
| [ ] | Firma de fallo | ACTA-0108 | Firmar fallo | FIRMA_COMPLETADA | FALLO firmado | Notificaciones |
| [ ] | Notificación de fallo | ACTA-0037 / 0015 | Lote postal fallo | NOTIFICACION_FALLO | Notif fallo | Análisis |
| [ ] | Condenado | ACTA-0027 / 0108 | Estado post fallo condenatorio | — | Fallo condenatorio | Análisis / Con apelación |
| [ ] | Absuelto | ACTA-0006 / 0109 | Estado post fallo absolutorio | — | Fallo absolutorio | Análisis / Cerradas |
| [ ] | Apelación presentada | ACTA-0111 / 0027 | Presentar apelación | APELACION_PRESENTADA | — | Con apelación |
| [ ] | Vencimiento plazo apelación | ACTA-0029 | Mock vencimiento | VENCIMIENTO_APELACION | — | Análisis |
| [ ] | Resolución de apelación | ACTA-0112 / 0030 | Resolver apelación | RESOLUCION_APELACION | Documento resolución | Con apelación |
| [ ] | Condena firme | ACTA-0029 | Sin apelación en plazo | — | Fallo firme | Análisis |
| [ ] | Informar pago condena | ACTA-0110 | Informar pago post fallo | PAGO_INFORMADO | Comprobante | Pendientes de fallo / Análisis |
| [ ] | Confirmar pago condena | ACTA-0110 | Confirmar pago | PAGO_CONFIRMADO | — | Análisis |
| [ ] | Observar pago condena | — | `[MOCK]` si no hay acta dedicada | OBSERVACION_PAGO | — | Análisis |
| [ ] | Cierre por pago | ACTA-0021 / 0008 | Cerrar con pago confirmado | CIERRE | — | Cerradas |
| [ ] | Cierre por absolución | ACTA-0019 | Cerrar tras liberaciones | CIERRE | — | Cerradas |
| [ ] | Archivo | ACTA-0007 | Pasar a archivo operativo | ARCHIVO | — | Archivo |
| [ ] | Reingreso desde archivo | ACTA-0010 | Reingreso por evento | REINGRESO | — | Bandeja origen |
| [ ] | Derivación a apremio | ACTA-0015 | Derivar | DERIVACION_APREMIO | — | Gestión externa |
| [ ] | Derivación a juzgado de paz | ACTA-0017 | Ya derivado / derivar | DERIVACION_JP | — | Gestión externa |
| [ ] | Reingreso desde gestión externa | ACTA-0017 | Reingresar | REINGRESO_GESTION_EXTERNA | — | Análisis |
| [ ] | Paralización | ACTA-0114 | Paralizar (si acción existe) o ver precargada | PARALIZACION | — | Paralizadas |
| [ ] | Reactivación desde paralizada | ACTA-0114 | Levantar paralización | REACTIVACION | — | Análisis |
| [ ] | Cierre final | ACTA-0008 | Cierre definitivo | CIERRE_DEFINITIVO | Sin pendientes | Cerradas |

---

## 5. Correo postal

Checklist operativo en menú **Operación de notificaciones → Correo postal** (no bajo Herramientas demo).

| Check | Paso | Actas demo sugeridas | Resultado esperado | Observaciones |
|:-----:|------|----------------------|-------------------|---------------|
| [ ] | Ver candidatas | ACTA-0037, 0038, 0039, 0040, 0041 | Listado con piezas listas para lote | |
| [ ] | Seleccionar parcial | ACTA-0037 + 0039 (dejar 0038 sin seleccionar) | Solo seleccionadas en lote nuevo | |
| [ ] | Generar lote | Selección parcial | Lote creado en estado pendiente | |
| [ ] | No seleccionadas siguen candidatas | ACTA-0038 | Sigue en candidatas | |
| [ ] | Ver lote generado | Lote recién creado | Detalle con actas incluidas | |
| [ ] | Procesar respuesta demo | ACTA-0042 (enviado pendiente respuesta) | Resultado por acta aplicado | |
| [ ] | Lote pasa a procesado | Lote del paso anterior | Estado PROCESADO | |
| [ ] | Actas actualizan resultado | ACTA-0043, 0044, 0045 | Positiva / negativa / vencida en notificaciones | |
| [ ] | Anular lote pendiente | Lote pendiente sin procesar | Lote anulado | |
| [ ] | Notificaciones vuelven a candidatas | Actas del lote anulado | Candidatas de nuevo | |
| [ ] | Envío individual sin lote | Verificar si existe en UI/API | Acta notificada sin lote agrupado | Anotar `[MOCK]` o `[FUNC]` si falta |
| [ ] | Trazabilidad por acta | ACTA-0043 | Historial notif + eventos por acta | |

**Volumen postal (regresión listados):** ACTA-0068 a ACTA-0076.

---

## 6. Portal infractor / domicilio electrónico

| Check | Paso | Acta demo | Resultado esperado | Observaciones |
|:-----:|------|-----------|-------------------|---------------|
| [ ] | Abrir portal | ACTA-0034 | Portal carga sin error | |
| [ ] | Confirmar visualización acta inicial | ACTA-0034 | Datos acta + pieza a notificar visibles | |
| [ ] | Ver notificación entregada/positiva | ACTA-0034 tras entrega | Estado ENTREGADA / positiva | |
| [ ] | Ver avance — fallo condenatorio | ACTA-0035 | Fallo visible pendiente lectura | |
| [ ] | Ver avance — fallo absolutorio | ACTA-0036 | Fallo absolutorio visible | |
| [ ] | Presentar apelación desde portal | ACTA-0027 | Apelación registrada en expediente | |
| [ ] | Coherencia con bandeja Con apelación | ACTA-0027 / 0111 | Mismo estado en backoffice | |

---

## 7. Notificador municipal demo

| Check | Paso | Acta demo | Resultado esperado | Observaciones |
|:-----:|------|-----------|-------------------|---------------|
| [ ] | Buscar por acta | ACTA-0033 | Expediente encontrado | |
| [ ] | Buscar por QR | ACTA-0033 (QR asociado) | Misma acta que búsqueda por id | |
| [ ] | Acuse positivo | ACTA-0033 | ENTREGADA → Análisis | |
| [ ] | Acuse negativo | Crear o usar acta en trámite notificador | Resultado negativo | Anotar acta si falta `[MOCK]` |
| [ ] | Acuse vencido | Idem negativo | Vencida | |
| [ ] | Ver avance de acta | ACTA-0031 (fallo condenatorio) | Estado notificación fallo coherente | |
| [ ] | Fallo absolutorio en notificador | ACTA-0032 | Misma verificación | |

---

## 8. Matriz de cobertura de mocks

Referencia de conteo: dataset en `MockDataFactory` (casos funcionales ACTA-0001–0045, volumen ACTA-0046+, UX ACTA-0108–0116). **Actualizar cantidades** tras cada carga de mocks nueva.

| Bandeja UX | Cantidad actual (aprox.) | Actas demo conocidas (muestra) | Estados cubiertos | Estados faltantes | ¿Necesita más mocks? |
|------------|--------------------------|--------------------------------|-------------------|-------------------|----------------------|
| Actas en enriquecimiento | ~15+ | 0001, 0002, 0009, 0018, 0022, 0024, 0025, 0046–0049, 0050–0051 | D1 captura, D2 enriquecimiento, D3 preparación, pago solicitado/informado | Labrada sin operador asignado; rechazo enriquecimiento | Solo si validación exige caso no listado |
| Análisis / presentaciones / pagos | ~25+ | 0006, 0015–0016, 0018–0019, 0021–0023, 0026–0030, 0049 | D5 análisis, pago, fallo, apelación, derivación | Observar pago condena dedicado | Sí — observación pago condena |
| Pendientes resolución / redacción | ~12+ | 0011–0014, 0020, 0077–0081 | Resolución, nulidad, medida, rectificación | Segunda pieza simultánea en conflicto | Revisar tras cambios |
| Pendientes de fallo | 3 | 0108–0110 | Pre-fallo condenatorio/absolutorio, pago previo | Fallo con medida activa bloqueando | Cubierto para UX |
| Pendientes de firma | ~8+ | 0003, 0056+ | Borrador y piezas pendientes firma | Firma múltiple simultánea | Volumen OK |
| Notificaciones | ~20+ | 0004–0005, 0031–0036, 0042–0045 | Postal, portal, notificador, en trámite, resultados | Canal mixto en misma pieza | Revisar casos borde |
| Con apelación | 3+ | 0111–0113, 0027–0030 | Pendiente, en análisis, resuelta | Apelación rechazada mantiene condena | Opcional |
| Gestión externa | ~5+ | 0017, 0082+, 0015 derivación | JP, apremio, listo derivar | Reingreso con novedad externa | Derivación ACTA-0015 verificar |
| Paralizadas | 3 | 0114–0116 | Tres motivos paralización | Reactivación post-levantamiento | Cubierto UX |
| Archivo | ~8+ | 0007, 0010, volumen | Archivo operativo, duplicidad, pendientes material | Archivo con apelación abierta | Según spec |
| Cerradas | ~8+ | 0008, 0021–0022, volumen | Cierre pago, absolución, nulidad | Reapertura indebida | Cubierto base |

**Leyenda ¿Necesita más mocks?:** marcar **Sí** solo cuando la validación integral no puede completarse sin un estado nuevo documentado en Observaciones.

---

## 9. Criterio de cierre de la validación integral

La validación integral se considera **completa** cuando se cumple todo lo siguiente:

1. **Cada bandeja UX** (las 11 del lateral) tiene **al menos un caso** con Resultado `OK` en la sección 3.
2. **Cada circuito transversal** de la sección 4 tiene **al menos una acta demo** validada o una fila explícita `[MOCK]` con ticket/slice pendiente.
3. **Transiciones principales** registran **evento append-only** coherente (tipo, bloque origen/destino, detalle legible).
4. **Documentos esperados** aparecen en detalle con tipo y estado correctos (PDF mock accesible si la UI lo permite).
5. **Notificaciones** muestran **canal, estado y resultado** alineados con la acción ejecutada.
6. **Cerrabilidad** coincide con reglas: archivo ≠ cerrada; bloqueos visibles antes de cerrar.
7. **Sin mojibake** en labels, eventos, nombres de infractor ni mensajes de error.
8. **Sin bandejas backend crudas** en el lateral (`LABRADAS`, `PENDIENTE_PREPARACION_DOCUMENTAL`, `PENDIENTE_NOTIFICACION`, `EN_NOTIFICACION` no aparecen como entradas propias).
9. **Correo postal**, **portal infractor** y **notificador municipal** tienen todos los checks de las secciones 5–7 en `OK` o justificados.
10. Registro consolidado de hallazgos abiertos (`[BE]`, `[FE]`, `[FUNC]`, `[MOCK]`) con acta y paso reproducible.

### Plantilla de registro de sesión

```
Fecha:
Validador:
Backend commit / tag:
Frontend commit / tag:
Reset dataset: sí / no

Resumen:
- Bandejas UX OK: __ / 11
- Circuitos transversales OK: __ / 40
- Correo postal OK: __ / 12
- Portal OK: __ / 7
- Notificador OK: __ / 7
- Hallazgos abiertos: (listar id acta + etiqueta)
```

---

## Historial del documento

| Fecha | Cambio |
|-------|--------|
| 2026-05-26 | Creación inicial del checklist maestro para validación integral del circuito. |

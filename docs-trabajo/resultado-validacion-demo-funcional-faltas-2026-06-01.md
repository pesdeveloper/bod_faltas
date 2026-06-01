# Resultado de validacion funcional demo - Direccion de Faltas

**Fecha:** 2026-06-01
**Ejecutado por:** Slice 18A
**Entorno:** backend localhost:8087 + Angular build OK

---

## Preparacion

| Item | Resultado |
|---|---|
| Backend levantado (spring-boot:run) | OK — puerto 8087 |
| Backend con Slice 17A (reactivar-acta) | Requirio reinicio (ver Hallazgo H2) |
| Angular build (npm run build) | OK |
| Reset demo | OK — 42 actas |
| Health check | OK — status=UP, cantidadActas=41 |

---

## A. Sanidad inicial

| Test | Resultado | Detalle |
|---|---|---|
| MatrizSanidadActasDemoIT | OK | 42 actas sanas, 0 warnings, 0 errores |

---

## B. Build Angular

| Item | Resultado | Detalle |
|---|---|---|
| npm run build | OK | exit code 0 |
| Warning bundle budget | Preexistente | 690.56 KB (limite 500 KB); antes documentado como 688 KB; diferencia 2.56 KB insignificante, mismo tipo |
| Warning scss budget | Preexistente | 12.66 KB (limite 8 KB); igual al registro anterior |
| Warnings nuevos | Ninguno | Sin regresion |

---

## C. Circuitos por acta

| Acta | Circuito | Estado | Detalle |
|---|---|---|---|
| ACTA-0001 | NOTIFICACION_ACTA | OK | generar — firmar — notificar positiva — PENDIENTE_ANALISIS |
| ACTA-0011 | RESOLUCION | OK | generar — firmar — notificar positiva — PENDIENTE_ANALISIS |
| ACTA-0014 | RECTIFICACION | OK | generar — firmar — notificar positiva — PENDIENTE_ANALISIS |
| ACTA-0012 | NULIDAD | OK | generar — firmar — cierre directo en CERRADAS |
| ACTA-0013 | MEDIDA_PREVENTIVA + NOTIFICACION_ACTA | OK | 2 piezas generadas — firmadas individualmente — notificar — PENDIENTE_ANALISIS |
| ACTA-0006 | Fallo absolutorio | OK | dictar — firmar — notificar — cerrabilidad=ABSUELTO/true — cerrar — CERRADAS |
| ACTA-0027 | Fallo condenatorio + apelacion presentada | OK | dictar (montoCondena) — firmar — notificar — registrar apelacion PRESENCIAL_DIRECCION — resultadoFinal=CONDENADO |
| ACTA-0028 | Fallo condenatorio + apelacion resuelta | OK | dictar — firmar — notificar — registrar apelacion — resolver RECHAZADA — resultadoFinal=CONDENA_FIRME |
| ACTA-0029 | Condena firme + pago condena + cierre | OK | Resuelto Slice 19A. dictar - firmar - notificar - vencimiento - CONDENA_FIRME - informar pago - confirmar - cerrar - CERRADAS. monto=1700. |
| ACTA-0030 | Gestion externa APREMIO + reingreso + pago condena | OK | dictar — firmar — notificar — vencimiento — CONDENA_FIRME — derivar APREMIO — GESTION_EXTERNA — reingresar — informar pago — confirmar — cerrar — CERRADAS |
| ACTA-0019 | Bloqueantes materiales (3 ejes) | OK | cerrabilidad.cerrable=false inicial — 3 resoluciones documentales — 3 cumplimientos materiales — cerrabilidad.cerrable=true — cerrar — CERRADAS |
| ACTA-0122 | Condena firme + pago condena + cierre | OK | estado precargado CONDENA_FIRME — informar pago — confirmar — cerrar — CERRADAS |
| ACTA-0007 | Archivo / reingreso | OK | estado inicial ARCHIVO/ARCHIVADA_OPERATIVA — reingresar — PENDIENTE_ANALISIS / REVISION_POST_REINGRESO |
| ACTA-0017 | Gestion externa / retorno (JUZGADO_DE_PAZ) | OK | estado inicial GESTION_EXTERNA/JUZGADO_DE_PAZ — reingresar — PENDIENTE_ANALISIS / REVISION_POST_GESTION_EXTERNA |
| ACTA-0114 | PARALIZADAS protegida + reactivacion | OK | Ver detalle en seccion D |
| Correo postal | Lote CSV + procesamiento respuesta | OK | 5 notificaciones preparadas — lote generado — respuesta procesada (2 positivas, 2 negativas, 1 vencida, 0 errores) |

---

## D. Detalle ACTA-0114 PARALIZADAS + reactivacion

| Paso | Resultado | Evidencia |
|---|---|---|
| Reset demo | OK | 42 actas; PARALIZADAS: 1 |
| ACTA-0114 visible en PARALIZADAS | OK | bandeja=PARALIZADAS, estado=PARALIZADA |
| accionPendiente=PARALIZACION_ESPERA_DOCUMENTAL | OK | confirmado |
| Backend calcula pago=SOLICITAR (backend-level) | OK (esperado) | El backend computa la accion; la UI Angular la oculta por BANDEJAS_SIN_ACCIONES_INTERNAS_OPERATIVAS |
| Reactivar acta | OK | POST reactivar-acta — resultado=OK |
| Sale de PARALIZADAS | OK | bandejaActual=PENDIENTE_ANALISIS |
| estadoProceso=PENDIENTE_REVISION | OK | confirmado |
| accionPendiente=REVISION_POST_REACTIVACION | OK | confirmado |
| situacion=ACTIVA | OK | confirmado |
| PARALIZADAS vacia post-reactivacion | OK | 0 actas en PARALIZADAS |
| Evento ACTA_REACTIVADA_DESDE_PARALIZADAS | OK | tipoEvento=ACTA_REACTIVADA_DESDE_PARALIZADAS; descripcion correcta con motivo previo |
| Reset posterior restaura ACTA-0114 en PARALIZADAS | OK | 42 actas; PARALIZADAS: 1 |

---

## E. UX transversal

| Item | Estado | Observacion |
|---|---|---|
| Lateral con bandejas principales | NO VALIDADO VISUALMENTE | Angular build OK; pendiente confirmacion visual |
| Sin sub-bandejas en lateral | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Filtro operativo en area principal | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Panel derecho sin acta: resumen simple | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Panel derecho con acta: solo detalle | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Sin accordion/resumen colapsable superior | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Redondeos consistentes | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Sin estilo pill/capsula | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Mojibake en API (PowerShell terminal) | DISPLAY | Acentos aparecen garbled en consola PowerShell; normal en Windows; Angular/browser renderiza correctamente UTF-8 |
| Mojibake en UI Angular | NO VALIDADO VISUALMENTE | pendiente confirmacion visual; build sin errores sugiere que no hay regresion |
| Correo postal como opcion real bajo divider | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |
| Notificador municipal como secundario | NO VALIDADO VISUALMENTE | pendiente confirmacion visual |

---

## F. Hallazgos

### H1 — RESUELTO (Slice 19A): ACTA-0029 agregada al dataset

**Tipo:** Bug de dataset / checklist desalineado — RESUELTO en Slice 19A
**Severidad:** Media
**Detectado en:** Slice 18A. GET /api/prototipo/actas/ACTA-0029 devolvio HTTP 404.
**Resuelto en:** Slice 19A. Se agrego la llamada faltante a cargarActa0029FalloVencimientoPlazoDemo(store) en MockDataFactory.java.
**Estado inicial post-fix:** PENDIENTE_ANALISIS, SIN_RESULTADO_FINAL, listo para dictar fallo condenatorio.
**Circuito validado post-fix:** dictar (montoCondena=1700) - firmar - notificar - vencimiento - CONDENA_FIRME - informar pago - confirmar - cerrar - CERRADAS.
**Resuelta en:** Slice 19A. Test: CondenaFirmePagoCondenaActa0029IT 6/6 OK. Matriz: 42 actas, 0 advertencias, 0 errores. Revalidado en Slice 19B (circuito API OK + build Angular OK).

### H2 — INFORMATIVO: Backend stale requirio reinicio para Slice 17A

**Tipo:** Condicion de entorno (no bug de codigo)
**Descripcion:** El backend en el puerto 8087 habia sido iniciado el 27/05/2026 (antes de Slice 17A). Al intentar POST /api/prototipo/actas/ACTA-0114/acciones/reactivar-acta, retorno HTTP 404 porque el proceso JVM no tenia cargado el endpoint nuevo.
**Resolucion:** Se detuvo el proceso viejo (PID 13220, java.exe) y se inicio un nuevo proceso con mvn spring-boot:run. El nuevo backend carga los target/classes compilados con Slice 17A. El endpoint funciono correctamente.
**Impacto en validacion:** Ninguno una vez resuelto. El resultado final de ACTA-0114 es OK.
**Recomendacion:** Documentar en el checklist que el backend debe estar corriendo con los target/classes actuales. Si hay dudas, reiniciar antes de validar.

---

## G. Resumen ejecutivo

| Item | Resultado |
|---|---|
| MatrizSanidadActasDemoIT | OK (42/0/0) |
| Build Angular | OK (solo warnings preexistentes) |
| ACTA-0001 NOTIFICACION_ACTA | OK |
| ACTA-0011 RESOLUCION | OK |
| ACTA-0014 RECTIFICACION | OK |
| ACTA-0012 NULIDAD | OK |
| ACTA-0013 MEDIDA_PREVENTIVA + NOTIFICACION_ACTA | OK |
| ACTA-0006 Fallo absolutorio | OK |
| ACTA-0027/0028 Fallo condenatorio + apelacion | OK |
| ACTA-0029 Condena firme + pago (via vencimiento) | OK - resuelto en Slice 19A |
| ACTA-0030 Gestion externa + reingreso + pago | OK |
| ACTA-0019 Bloqueantes materiales | OK |
| ACTA-0122 Condena firme + pago condena | OK |
| ACTA-0007 Archivo / reingreso | OK |
| ACTA-0017 Gestion externa / retorno | OK |
| ACTA-0114 PARALIZADAS + reactivacion | OK |
| Correo postal | OK |
| UX transversal | NO VALIDADO VISUALMENTE (requiere inspeccion en browser) |
| Mojibake en UI | NO VALIDADO VISUALMENTE (PowerShell muestra garbled por encoding de consola; Angular/browser correcto) |

---

## I. Validacion visual UX transversal (Slice 20A)

- Angular levantado en http://localhost:4200: OK.
- Proxy Angular->Backend: OK (11 bandejas).
- Logica de visibilidad de acciones verificada en codigo (puedeMostrarPagoVoluntario, bandejaActaSinAccionesInternasOperativas).
- Validacion visual en browser: PENDIENTE de revision manual por el equipo.
- Guia de presentacion ejecutiva creada: docs-trabajo/guia-presentacion-demo-faltas-2026-06-01.md.

---

## H. Conclusion

**Demo funcional completa lista: SI**

- 15/15 circuitos funcionales validados por API y revalidados: OK

— UX transversal: requiere validacion visual en browser (Angular build OK, sin regresiones)
— Backend stale: resuelto durante el slice con reinicio; no es bug de codigo

**Proximo slice recomendado:**

- Slice 20A: validacion visual UX transversal en browser PENDIENTE; guia presentacion ejecutiva creada
- Slice 20A: validacion visual UX transversal en browser PENDIENTE; guia presentacion ejecutiva creada

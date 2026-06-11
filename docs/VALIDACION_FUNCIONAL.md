# VALIDACION FUNCIONAL — DIRECCION DE FALTAS

Fecha de cierre: 2026-06-11
Iteracion: Prototipo descartable en memoria — validacion integral.

---

## 1. Estado tecnico consolidado

| Metrica                        | Resultado           |
|-------------------------------|---------------------|
| Backend full suite             | 589 tests OK        |
| Fallos backend                 | 0                   |
| Tests saltados backend         | 1 (skipped OK)      |
| Build Angular                  | OK                  |
| check:mojibake                 | OK — sin mojibake   |
| git diff --check               | OK — sin whitespace |
| Barrido visual final           | OK                  |

---

## 2. Matriz por bandeja / circuito

| Bandeja                             | Estado validacion  | Observaciones                                    |
|------------------------------------|--------------------|-------------------------------------------------|
| Actas en enriquecimiento            | Validado OK        | Alta, anulacion, envio a notificacion           |
| Pendiente analisis / analisis       | Validado OK        | Analisis, archivo, derivacion gestion externa   |
| Pendientes resolucion / redaccion   | Validado OK        | Generacion de piezas, nulidad, rectificacion    |
| Pendientes de fallo                 | Validado OK        | Fallo absolutorio, fallo condenatorio           |
| Pendientes de firma                 | Validado OK        | Firma de documentos habilitante de acciones     |
| Notificaciones                      | Validado OK        | Postal, municipal, portal infractor             |
| Con apelacion                       | Validado OK        | Aceptada absolucion, rechazada                  |
| Gestion externa                     | Validado OK        | Apremio, juzgado de paz, reingreso              |
| Paralizadas                         | Validado OK        | Paralizar, reactivar                            |
| Archivo                             | Validado OK        | Archivo directo, post vencimiento, reingreso    |
| Cerradas                            | Validado OK        | Por pago, absolutoria, condena firme            |

---

## 3. Reglas funcionales transversales

- **Acta cerrada**: sin acciones internas. Solo consulta.
- **Acta archivada**: sin acciones internas normales. Solo reingreso si `permiteReingreso=true`.
- **Acta en gestion externa**: sin acciones internas normales. Solo retorno desde gestion.
- **Acta paralizada**: sin acciones internas normales. Solo reactivar.
- **Firma pendiente**: bloquea acciones de fondo (fallo, cierre, notificacion).
- **Notificacion negativa**: no habilita dictado de fallo.
- **Notificacion positiva de fallo condenatorio**: habilita apelacion y vencimiento; no cierre directo.
- **Pago informado estable**: debe ser `PENDIENTE_CONFIRMACION`, no `PAGO_INFORMADO`.
- **Portal infractor**: no confirma pagos. Solo informa y consulta estado.
- **Confirmar pago condena**: conserva `resultadoFinal=CONDENA_FIRME`.
- **Documento resolutorio**: no sustituye hecho material.
- **Cumplimiento material**: exige documento resolutorio firmado previo.
- **Flags independientes**: `resolucionBloqueante` y `cumplimientoMaterial` son independientes.
- **Chips bandeja ARCHIVO**: no mostrar "Cerrable" ni chips operativos; mostrar estado archivada.

---

## 4. Circuitos end-to-end validados

| Circuito                                             | Acta referencia | Estado     |
|-----------------------------------------------------|-----------------|------------|
| Pago voluntario simple con cierre                   | ACTA-0021       | Validado   |
| Pago voluntario observado y confirmado              | ACTA-0120       | Validado   |
| Materiales + pago voluntario + cierre               | ACTA-0022       | Validado   |
| Fallo condenatorio + notificacion + condena firme + pago condena + cierre | ACTA-0029 | Validado |
| Fallo condenatorio + gestion externa + reingreso    | ACTA-0030       | Validado   |
| Apelacion aceptada absolucion                       | ACTA-0028       | Validado   |
| Apelacion rechazada                                 | varios          | Validado   |
| Gestion externa juzgado de paz                      | ACTA-0017       | Validado   |
| Gestion externa apremio                             | ACTA-0130       | Validado   |
| Notificacion negativa con reintento                 | ACTA-0038       | Validado   |
| Notificacion postal positiva de acta inicial        | ACTA-0004       | Validado   |
| Notificacion postal positiva de fallo condenatorio  | ACTA-0037       | Validado   |
| Notificacion postal positiva de fallo absolutorio   | ACTA-0028/0040  | Validado   |
| Archivo y reingreso                                 | ACTA-0007       | Validado   |
| Cerrada por pago confirmado                         | ACTA-0008       | Validado   |
| Paralizar y reactivar                               | ACTA-0114 / ACTA-0006 | Validado |
| Absolucion habilita materiales pero no cierre si pendientes | ACTA-0019 | Validado |
| Reingreso desde archivo por nulidad                 | ACTA-0012       | Validado   |

---

## 5. Materiales / bloqueantes de cierre

- Constatacion material temprana validada (ACTA-0022, ACTA-0019).
- Ejes bloqueantes de cierre: `LEVANTAMIENTO_MEDIDA_PREVENTIVA`, `LIBERACION_RODADO`, `ENTREGA_DOCUMENTACION`.
- Cumplimiento de material require resolutorio firmado (documento previo al hecho material).
- Resolutorio bloqueante y cumplimiento material son flags independientes.
- Absolucion habilita visualizacion de materiales pero no cierre si hay pendientes (ACTA-0019 cubierto por test).
- Pago confirmado habilita materiales pero no cierre directo si hay pendientes (test cubierto).

---

## 6. Pago voluntario

- Flujo completo validado: solicitar, fijar monto, informar, confirmar/observar.
- Pago confirmado cierra el acta sin accion adicional.
- Pago observado permite reintento.
- Portal infractor puede solicitar pago voluntario e informar; no confirma.
- `situacionPago=PENDIENTE_CONFIRMACION` es el estado estable post-informado (no `PAGO_INFORMADO`).
- Vencimiento de pago voluntario validado.

---

## 7. Fallo / condena / pago condena

- Fallo absolutorio: pasa a PENDIENTE_FIRMA. Al notificarse positivo: `resultadoFinal=ABSUELTO`, cerrable.
- Fallo condenatorio: pasa a PENDIENTE_FIRMA. Al notificarse positivo: habilita apelacion y vencimiento.
- Condena firme tras vencimiento de apelacion o apelacion rechazada.
- Consentir condena + registrar pago: `resultadoFinal=CONDENA_FIRME`, `situacionPagoCondena=INFORMADO`.
- Confirmar acreditacion pago condena: conserva `resultadoFinal=CONDENA_FIRME`.
- Pago condena por gestion externa (apremio) validado (ACTA-0130).
- Portal del condenado: puede consentir condena y registrar pago; no puede si fallo no esta firmado o hay apelacion pendiente.

---

## 8. Correo postal / lotes

- Generacion de lote postal: soporta `ACTA_INFRACCION`, `FALLO_CONDENATORIO`, `FALLO_ABSOLUTORIO`.
- Seleccion parcial de notificaciones para lote validada.
- Procesamiento de respuesta demo: positiva, negativa, vencida.
- Positiva en fallo absolutorio: `resultadoFinal=ABSUELTO`, `cerrable=true`, sin acciones indebidas.
- Positiva en fallo condenatorio: habilita apelacion/vencimiento, no cierre directo.
- Negativa: `REINTENTAR_NOTIFICACION`.
- Vencida: `EVALUAR_NOTIFICACION_VENCIDA`.
- Trazabilidad completa: lote generado, enviado, procesado, archivado.
- Actas demo disponibles: ACTA-0037 (fallo condenatorio), ACTA-0038 (acta inicial), ACTA-0039 (segundo condenatorio), ACTA-0040 (fallo absolutorio).
- Cobertura de test: `CorreoPostalNotificacionIT` (92 tests aprox incluyendo volumen y multiples escenarios).

---

## 9. Portal infractor

- Acceso por codigo QR validado.
- Vista de estado: estado visible, situacion pago, resultado final.
- Documentos: visibles, notificables, notificados.
- Notificacion por visualizacion de documento en portal (fallo).
- Solicitud de pago voluntario desde portal.
- Registro de pago voluntario desde portal.
- Consentir condena desde portal.
- Registrar pago condena desde portal.
- Presentar apelacion desde portal.
- Domicilio electronico verificado: preferencia sobre postal.
- Portal no confirma pagos ni apelaciones.
- Portal no muestra documentos no firmados ni no notificados.

---

## 10. Repairs funcionales relevantes

- `situacionPago` normalizada: `PAGO_INFORMADO` eliminado; estado estable es `PENDIENTE_CONFIRMACION`.
- `resolucionBloqueante` y `cumplimientoMaterial`: flags independientes, no dependientes entre si.
- Chips en bandeja ARCHIVO: suprimidos chips operativos (Cerrable, Condena firme pendiente, etc.). Se muestra estado archivada claro.
- `accionesUi`: calculadas por backend; frontend respeta exclusivamente las acciones habilitadas.
- Firma pendiente bloquea consistentemente: no permite fallo, cierre, notificacion mientras hay documento PENDIENTE_FIRMA.
- Notificacion negativa no habilita fallo (validado por test especifico).
- Condena firme: `resultadoFinal=CONDENA_FIRME` se preserva al confirmar pago condena.
- Archivo definitivo (`permiteReingreso=false`): no expone accion de reingreso.
- Medida preventiva posterior a contravenecion: validada (MedidaPreventivaPosteriorContravencionIT).

---

## 11. Cobertura final por tipo de accion

| Tipo de accion                   | Cubierto en test | Cubierto visualmente |
|----------------------------------|-----------------|---------------------|
| Alta de acta                     | Si              | Si                  |
| Anular acta                      | Si              | Si                  |
| Archivar acta                    | Si              | Si                  |
| Reingresar desde archivo         | Si              | Si                  |
| Paralizar acta                   | Si              | Si                  |
| Reactivar acta                   | Si              | Si                  |
| Enviar a notificacion            | Si              | Si                  |
| Generar notificacion de acta     | Si              | Si                  |
| Generar nulidad                  | Si              | Si                  |
| Generar medida preventiva        | Si              | Si                  |
| Generar resolucion               | Si              | Si                  |
| Generar rectificacion            | Si              | Si                  |
| Firmar documento                 | Si              | Si                  |
| Registrar notificacion           | Si              | Si                  |
| Reintentar notificacion          | Si              | Si                  |
| Dictar fallo absolutorio         | Si              | Si                  |
| Dictar fallo condenatorio        | Si              | Si                  |
| Registrar apelacion              | Si              | Si                  |
| Resolver apelacion               | Si              | Si                  |
| Registrar vencimiento apelacion  | Si              | Si                  |
| Derivar a apremio                | Si              | Si                  |
| Derivar a juzgado de paz         | Si              | Si                  |
| Reingresar desde gestion externa | Si              | Si                  |
| Resolver juzgado de paz          | Si              | Si                  |
| Registrar pago en apremio        | Si              | Si                  |
| Solicitar pago voluntario        | Si              | Si                  |
| Fijar monto pago voluntario      | Si              | Si                  |
| Informar pago voluntario         | Si              | Si                  |
| Confirmar pago voluntario        | Si              | Si                  |
| Observar pago voluntario         | Si              | Si                  |
| Registrar vencimiento pago vol.  | Si              | Si                  |
| Consentir condena + registrar pago condena | Si    | Si                  |
| Informar pago condena            | Si              | Si                  |
| Confirmar acreditacion condena   | Si              | Si                  |
| Observar acreditacion condena    | Si              | Si                  |
| Cumplimiento material            | Si              | Si                  |
| Resolucion bloqueante cierre     | Si              | Si                  |
| Cerrar acta                      | Si              | Si                  |
| Correo postal: generar lote      | Si              | Si                  |
| Correo postal: procesar respuesta| Si              | Si                  |
| Notificador municipal            | Si              | Si                  |
| Portal: consultar estado         | Si              | Si                  |
| Portal: notificar documento      | Si              | Si                  |
| Portal: pago voluntario          | Si              | Si                  |
| Portal: consentir condena        | Si              | Si                  |
| Portal: pago condena             | Si              | Si                  |
| Portal: apelacion                | Si              | Si                  |

---

## 12. Observaciones no bloqueantes / pendientes opcionales

1. **Cobertura de correo postal absolutorio en demo ACTA-0040**: La acta ya existe en el mock y el test `CorreoPostalNotificacionIT` cubre el circuito (ACTA-0028 en lote postal con resultado positivo absolutorio). ACTA-0040 aparece correctamente tipificada como `FALLO_ABSOLUTORIO` en el listado de candidatas. Cobertura considerada completa.

2. **Budget Angular**: El build reporta advertencias de presupuesto de tamano (bundle > 500 kB, SCSS > 8 kB). Son advertencias preexistentes no bloqueantes para el prototipo.

3. **Resolucion de apelacion y condena firme**: Posible extension futura — permitir resolver apelacion directamente desde portal ciudadano (hoy es accion interna de Direccion).

4. **Domicilio electronico automatico**: La verificacion de domicilio electronico del infractor esta mock (flag booleano). En produccion requiere integracion con PIDE o padron.

5. **Lotes de volumen**: El mock incluye actas de volumen (ACTA-0068 a ACTA-0076) para validar escenarios de alto volumen postal. No hay test de volumen extremo; suficiente para prototipo.

---

## 13. Veredicto final

**VALIDACION INTEGRAL CERRADA.**

El prototipo Direccion de Faltas:

- Refleja fielmente el modelo de dominio final en estados y transiciones.
- Cubre todos los circuitos principales end-to-end.
- 589 tests backend OK, 0 fallos.
- Build Angular limpio, sin mojibake, sin whitespace issues.
- Listo para presentacion a stakeholders y como base de referencia para el backend productivo.

No quedan bloqueantes funcionales pendientes.
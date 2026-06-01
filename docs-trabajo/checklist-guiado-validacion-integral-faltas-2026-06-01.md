# CHECKLIST GUIADO — VALIDACION INTEGRAL DEMO FALTAS

Fecha: 2026-06-01
Dataset: 42 actas, MatrizSanidadActasDemoIT 0 advertencias 0 errores
Estado build Angular: OK (warnings preexistentes de budget solamente)
ACTA-0029: incorporada y validada
ACTA-0114: PARALIZADAS con reactivacion controlada

---

## 0. Como usar este checklist

Este checklist se valida paso a paso, preferentemente con un asistente (ChatGPT u otro).

Reglas de uso:

- Antes de cada accion, copiar el estado actual del acta usando la plantilla de la Seccion 24.
- Despues de cada accion, copiar el nuevo estado.
- Cada caso indica en que bandeja inicial buscar el acta inmediatamente despues del reset demo.
- Si el acta cambia de bandeja, el checklist indica a que bandeja ir a continuacion.
- Si aparece HTTP 409/400/500 inesperado, detener el caso y registrar bug antes de continuar.
- Si una accion aparece cuando no deberia (accion indebida en bandeja protegida), registrar bug visual/guard.
- No continuar con otro caso hasta marcar el paso actual como OK / BUG / NO VALIDADO.
- Si el operador valida acompanado por un asistente, copiar la plantilla de la Seccion 24 antes de cada pregunta.

### IMPORTANTE — Bandeja inicial obligatoria por caso

Cada caso indica explicitamente la bandeja inicial esperada del acta inmediatamente despues del reset demo.

Formato al inicio de cada caso:

    Bandeja inicial esperada despues de reset:
    - Bandeja UX:
    - Filtro operativo, si aplica:
    - Filtro dependencia, si aplica:
    - Acta a buscar:
    - Numero visible del acta:
    - Estado inicial esperado:
    - Accion inicial esperada:

### Formato recomendado por paso

    Estado a copiar antes de actuar:
    - Acta:
    - Bandeja actual:
    - Etapa (dominio):
    - Situacion (estadoProceso):
    - Resultado final:
    - Situacion pago voluntario:
    - Situacion pago condena:
    - Accion pendiente:
    - Cerrable:
    - Motivo cerrabilidad:
    - Acciones visibles:
    - Documentos pendientes:
    - Bloqueantes materiales:
    - Eventos relevantes:

Luego:
- Accion a ejecutar.
- Resultado esperado.
- Bandeja esperada despues.
- Que copiar despues.
- Estado del paso: [ ] OK / [ ] BUG / [ ] NO VALIDADO

---

## 1. Preparacion del entorno

### Comandos de inicio

Backend:

    cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
    mvn spring-boot:run

Angular:

    cd S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular
    npm start

Reset demo (ejecutar antes de iniciar validacion completa):

    POST http://localhost:8087/api/prototipo/reset

Health:

    GET http://localhost:8087/api/prototipo/health

Bandejas (verificar cantidades):

    GET http://localhost:8087/api/prototipo/bandejas

Sanidad (verificar 42 actas, 0 errores):

    cd S:\Source\Repos\Bod-Faltas\backend\api-faltas-prototipo
    mvn test -Dtest="MatrizSanidadActasDemoIT"

Build Angular (verificar que compila):

    cd S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular
    npm run build

### Notas de preparacion

- Si el backend estuvo levantado desde dias anteriores, reiniciarlo para evitar estado stale.
- Hacer reset demo antes de iniciar la validacion completa.
- Si se reinicia el backend durante la validacion, hacer reset nuevamente y reiniciar el caso actual.
- Puerto backend: 8087. Puerto Angular: 4200.

### Checklist de preparacion

- [ ] Backend levantado y respondiendo en http://localhost:8087/api/prototipo/health
- [ ] Angular corriendo en http://localhost:4200
- [ ] Reset demo ejecutado con HTTP 200
- [ ] Sanidad: 42 actas, 0 advertencias, 0 errores

---

## 2. Validacion visual base

Ejecutar antes de validar casos individuales.

- [ ] No hay mojibake visible (caracteres mal codificados)
- [ ] Panel lateral muestra bandejas principales solamente
- [ ] No hay sub-bandejas anidadas en el lateral
- [ ] Iconos visibles junto a cada bandeja
- [ ] Cantidades visibles como badges numericos
- [ ] Labels de bandeja legibles
- [ ] Menu hamburguesa disponible en panel de bandejas
- [ ] Header muestra solo la bandeja activa seleccionada
- [ ] Busqueda por numero de acta visible
- [ ] Filtro operativo visible cuando corresponde a la bandeja
- [ ] Filtro operativo tiene opcion Todos
- [ ] Filtro operativo tiene boton X compacto para limpiar
- [ ] Filtro dependencia transversal visible y separado del filtro operativo
- [ ] Panel derecho sin acta seleccionada: muestra resumen simple
- [ ] Panel derecho con acta seleccionada: muestra solo detalle del acta
- [ ] No hay acordeon o resumen colapsable indebido en la parte superior
- [ ] No hay scroll horizontal general en la pagina
- [ ] Listado no solapa cards al tener muchas actas
- [ ] No hay estilo pill o capsula indebido en campos de estado
- [ ] Numero de acta (A-2026-XXXX) visible en cada card del listado

### Registro de observaciones visuales

    Observacion:
    Captura:
    Severidad: CRITICA / MEDIA / BAJA
    Slice sugerido:

---

## 3. Caso ACTA-0001 — Redaccion NOTIFICACION_ACTA

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0001
- Numero visible: A-2026-0001
- Estado inicial esperado: etapa D3_DOCUMENTAL / situacion PENDIENTE_PRODUCCION_PIEZAS
- Accion inicial esperada: Generar notificacion del acta (pieza requerida: NOTIFICACION_ACTA)

Objetivo: Validar generacion, firma y registro de notificacion de NOTIFICACION_ACTA.
Infractor: Garcia, Laura — DNI 28.441.992
Hecho: Estacionamiento prohibido en zona escolar.

Paso 1: Estado inicial
- Ir a bandeja Pendientes de redaccion / resolucion.
- Buscar ACTA-0001 (A-2026-0001).
- Copiar estado inicial completo.
- Confirmar que pieza requerida NOTIFICACION_ACTA aparece como pendiente.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Generar notificacion del acta
- Accion a ejecutar: Generar notificacion del acta.
- Resultado esperado: documento NOTIFICACION_ACTA generado en estado PENDIENTE_FIRMA.
- Bandeja esperada despues: permanece en Pendientes de redaccion o pasa a Pendientes de firma.
- Que copiar despues: estado etapa, documentos, piezas pendientes.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Cambiar a bandeja Pendientes de firma
- Si el acta paso a PENDIENTE_FIRMA, ir a bandeja Pendientes de firma.
- Buscar ACTA-0001.
- Confirmar que documento NOTIFICACION_ACTA aparece en estado PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Firmar documento
- Accion a ejecutar: Firmar documento NOTIFICACION_ACTA.
- Resultado esperado: documento pasa a FIRMADO.
- Bandeja esperada despues: Pendientes de notificacion (PENDIENTE_NOTIFICACION).
- Que copiar despues: estado, documentos, notificaciones.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Cambiar a bandeja Pendientes de notificacion
- Ir a bandeja Pendientes de notificacion.
- Buscar ACTA-0001.
- Confirmar que notificacion aparece como PENDIENTE_ENVIO.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 6: Registrar notificacion positiva
- Accion a ejecutar: Registrar notificacion positiva.
- Resultado esperado: notificacion pasa a ENTREGADA.
- Bandeja esperada despues: Pendientes de analisis (PENDIENTE_ANALISIS).
- Que copiar despues: estado, notificaciones, bandeja.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 7: Confirmar estado final
- Ir a bandeja Pendientes de analisis.
- Confirmar que ACTA-0001 aparece en PENDIENTE_ANALISIS.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: PENDIENTE_ANALISIS / D5_ANALISIS, notificacion ENTREGADA.

---

## 4. Caso ACTA-0011 — RESOLUCION

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0011
- Numero visible: A-2026-0011
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_RESOLUCION
- Accion inicial esperada: Generar resolucion (pieza requerida: RESOLUCION)

Infractor: Herrera, Marta — DNI 28.991.445
Hecho: Conducir con licencia vencida.

Paso 1: Estado inicial
- Buscar ACTA-0011 en Pendientes de redaccion / resolucion.
- Confirmar pieza RESOLUCION como pendiente.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Generar resolucion
- Accion: Generar resolucion.
- Resultado esperado: documento RESOLUCION en PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Firmar
- Ir a Pendientes de firma si corresponde.
- Firmar documento RESOLUCION.
- Resultado esperado: pasa a FIRMADO.
- Bandeja esperada: segun resultado — puede ir a notificacion o analisis segun flujo.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Confirmar estado posterior
- Copiar estado completo post-firma.
- Confirmar que pieza RESOLUCION ya no aparece como pendiente.
- Si cambia de bandeja, registrar bandeja destino.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: pieza RESOLUCION producida y firmada; acta en bandeja correspondiente segun flujo.

---

## 5. Caso ACTA-0014 — RECTIFICACION

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0014
- Numero visible: A-2026-0014
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_RECTIFICACION
- Accion inicial esperada: Generar rectificacion (pieza requerida: RECTIFICACION)

Infractor: Ibarra, Lucia — DNI 33.770.118
Hecho: Error material en datos del infractor.

Paso 1: Estado inicial
- Buscar ACTA-0014 en Pendientes de redaccion / resolucion.
- Confirmar pieza RECTIFICACION como pendiente.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Generar rectificacion
- Accion: Generar rectificacion.
- Resultado esperado: documento RECTIFICACION en PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Firmar
- Ir a Pendientes de firma si corresponde.
- Firmar documento RECTIFICACION.
- Resultado esperado: pasa a FIRMADO.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Confirmar estado posterior
- Copiar estado completo post-firma.
- Confirmar que pieza RECTIFICACION ya no aparece como pendiente.
- Si cambia de bandeja, registrar bandeja destino.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: pieza RECTIFICACION producida y firmada; acta en bandeja correspondiente.

---

## 6. Caso ACTA-0012 — NULIDAD

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0012
- Numero visible: A-2026-0012
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_NULIDAD
- Accion inicial esperada: Generar nulidad (pieza requerida: NULIDAD)

Infractor: Morales, Hernan — DNI 31.004.776
Hecho: Acta con vicio formal detectado.

Paso 1: Estado inicial
- Buscar ACTA-0012 en Pendientes de redaccion / resolucion.
- Confirmar pieza NULIDAD como pendiente.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Generar nulidad
- Accion: Generar nulidad.
- Resultado esperado: documento NULIDAD en PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Firmar
- Ir a Pendientes de firma si corresponde.
- Firmar documento NULIDAD.
- Resultado esperado: pasa a FIRMADO.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Confirmar cierre o estado final
- Copiar estado completo post-firma.
- Confirmar si el acta cierra automaticamente o pasa a bandeja especifica.
- Si pasa a CERRADAS, verificar que no aparecen acciones internas indebidas:
  - [ ] No aparece Solicitar pago voluntario
  - [ ] No aparece Archivar acta
  - [ ] No aparece Derivar a gestion externa
  - [ ] No aparece Registrar cumplimiento material
  - [ ] No aparece Dictar fallo
  - [ ] No aparece Cerrar acta (si ya esta cerrada)
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: pieza NULIDAD producida y firmada; acta en estado final correspondiente.

---

## 7. Caso ACTA-0013 — MEDIDA_PREVENTIVA + NOTIFICACION_ACTA (piezas multiples)

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0013
- Numero visible: A-2026-0013
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_MEDIDA_PREVENTIVA
- Accion inicial esperada: Generar medida preventiva O Generar notificacion del acta (ambas requeridas)

Infractor: Quiroga, Sergio — DNI 26.884.230
Hecho: Retencion preventiva de vehiculo.
Piezas requeridas: NOTIFICACION_ACTA + MEDIDA_PREVENTIVA (ambas deben producirse).

Paso 1: Estado inicial
- Buscar ACTA-0013 en Pendientes de redaccion / resolucion.
- Confirmar que aparecen DOS piezas requeridas: NOTIFICACION_ACTA y MEDIDA_PREVENTIVA.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Generar medida preventiva
- Accion: Generar medida preventiva.
- Resultado esperado: documento MEDIDA_PREVENTIVA en PENDIENTE_FIRMA.
- Confirmar que pieza NOTIFICACION_ACTA sigue pendiente.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Generar notificacion del acta
- Accion: Generar notificacion del acta.
- Resultado esperado: documento NOTIFICACION_ACTA en PENDIENTE_FIRMA.
- Confirmar que ambas piezas ahora estan en PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Firmar documentos
- Ir a Pendientes de firma.
- Firmar MEDIDA_PREVENTIVA. Confirmar FIRMADO.
- Firmar NOTIFICACION_ACTA. Confirmar FIRMADO.
- IMPORTANTE: la firma NO debe liberar bloqueantes materiales asociados a la medida preventiva.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Confirmar estado post-firma
- Copiar estado completo.
- Confirmar que el acta no avanza a siguiente etapa hasta completar ambas piezas.
- Registrar bandeja destino.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Nota: La firma de MEDIDA_PREVENTIVA reconoce origen bloqueante material MEDIDA_PREVENTIVA_ACTIVA.
Ese bloqueante NO desaparece solo con la firma: requiere cumplimiento material posterior.

Estado final esperado: ambas piezas firmadas; acta en siguiente bandeja operativa segun flujo.

---

## 8. Caso ACTA-0018 — Pago voluntario (circuito completo)

Bandeja inicial esperada despues de reset:
- Bandeja UX: Enriquecimiento / Actas en enriquecimiento (ACTAS_EN_ENRIQUECIMIENTO)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0018
- Numero visible: A-2026-0018
- Estado inicial esperado: etapa D2_ENRIQUECIMIENTO / situacion EN_CURSO / pago SIN_PAGO
- Accion inicial esperada: Solicitar pago voluntario

Infractor: Demo Pago Voluntario — DNI 11.111.111
Hecho: Caso demo pago voluntario (circuito completo desde SIN_PAGO).

Paso 1: Estado inicial
- Buscar ACTA-0018 en Enriquecimiento.
- Confirmar pago SIN_PAGO.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Solicitar pago voluntario
- Accion: Solicitar pago voluntario.
- Resultado esperado: pago pasa a SOLICITADO.
- Copiar estado post-accion.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Informar pago
- Accion: Informar pago voluntario (simular que el infractor informa).
- Resultado esperado: pago pasa a PAGO_INFORMADO.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Adjuntar comprobante (si aplica)
- Si la accion de adjuntar comprobante esta disponible, ejecutarla.
- Resultado esperado: comprobante registrado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Confirmar pago
- Accion: Confirmar pago voluntario.
- Resultado esperado: pago pasa a CONFIRMADO (PAGO_CONFIRMADO).
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 6: Verificar que pago no cierra automaticamente
- Confirmar que el acta NO pasa a CERRADA automaticamente por confirmar pago.
- Confirmar que accion Cerrar acta aparece solo si cerrable=true y se dan las condiciones.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: pago CONFIRMADO; cierre disponible solo si cerrable=true.

---

## 9. Caso ACTA-0020 — Caso auxiliar medida preventiva / Pago voluntario observado

ATENCION — Discrepancia entre nombre del caso y dataset actual:
ACTA-0020 en el dataset actual es un caso auxiliar de medida preventiva
(PENDIENTES_RESOLUCION_REDACCION, pieza requerida MEDIDA_PREVENTIVA),
NO un caso de pago voluntario observado.

Bandeja inicial real de ACTA-0020 segun dataset:
- Bandeja UX: Pendientes de redaccion / resolucion (PENDIENTES_RESOLUCION_REDACCION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0020
- Numero visible: A-2026-0020
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_PRODUCCION_PIEZAS / pieza MEDIDA_PREVENTIVA
- Accion inicial esperada: Generar medida preventiva

Para validar "pago voluntario observado", usar:
- ACTA-0018: desde SIN_PAGO, en paso de confirmar usar Observar pago en lugar de Confirmar.
- ACTA-0120: empieza con pago PAGO_INFORMADO, se puede observar o confirmar directamente.

Sub-caso A: ACTA-0020 (medida preventiva auxiliar)

Paso 1: Buscar ACTA-0020 en Pendientes de redaccion.
Paso 2: Generar medida preventiva.
Paso 3: Confirmar que el documento se genera y que se reconoce origen bloqueante MEDIDA_PREVENTIVA_ACTIVA.
Paso 4: Copiar estado final.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Sub-caso B: Pago voluntario observado (con ACTA-0018 o ACTA-0120)

Usando ACTA-0018 (si ya se ejecuto Paso 3 del Caso 8 y hay pago PAGO_INFORMADO):
- En el paso de confirmar pago, elegir Observar pago en lugar de Confirmar.
- Resultado esperado: pago pasa a OBSERVADO.
- Copiar estado.
- Accion siguiente esperada: Reinformar pago.
- Ejecutar Reinformar pago.
- Resultado esperado: pago vuelve a PAGO_INFORMADO.
- Confirmar pago.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Usando ACTA-0120 (VERIFICAR CON GET /api/prototipo/actas/ACTA-0120):
- Estado inicial: pago PAGO_INFORMADO segun dataset.
- Confirmar o Observar segun accion disponible.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

---

## 10. Caso ACTA-0004 — Notificacion comun

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de notificacion (PENDIENTE_NOTIFICACION)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0004
- Numero visible: A-2026-0004
- Estado inicial esperado: etapa D4_NOTIFICACION / situacion PENDIENTE_ENVIO
- Accion inicial esperada: Registrar notificacion positiva / negativa / vencida

Nota: ACTA-0004 corresponde al hito 4 del flujo transito: notificacion del acta pendiente de registrar resultado.

Paso 1: Estado inicial
- Buscar ACTA-0004 en Pendientes de notificacion.
- Confirmar estado PENDIENTE_ENVIO o equivalente.
- Confirmar que aparecen opciones: Positiva / Negativa / Vencida.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Registrar notificacion
- Elegir: Registrar notificacion positiva (recomendado para seguir el flujo principal).
- Resultado esperado: notificacion pasa a ENTREGADA.
- Bandeja esperada despues: Pendientes de analisis (PENDIENTE_ANALISIS).
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Confirmar estado final
- Ir a Pendientes de analisis.
- Confirmar que ACTA-0004 aparece en PENDIENTE_ANALISIS.
- Confirmar que la notificacion no libero bloqueantes ni cerro el acta automaticamente.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: notificacion ENTREGADA; acta en PENDIENTE_ANALISIS.

---

## 11. Caso ACTA-0006 — Fallo absolutorio

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0006
- Numero visible: A-2026-0006
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_REVISION
- Accion inicial esperada: Dictar fallo absolutorio

Infractor: Ramos, Diego — DNI 27.901.003
Hecho: Conducir bajo efectos de alcohol (alcohotest positivo).
Notificacion previa: ENTREGADA.

Paso 1: Estado inicial
- Buscar ACTA-0006 en Pendientes de analisis.
- Confirmar estado PENDIENTE_REVISION, notificacion ENTREGADA.
- Confirmar que accion Dictar fallo absolutorio esta disponible.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Dictar fallo absolutorio
- Accion: Dictar fallo absolutorio.
- Resultado esperado: documento FALLO_ABSOLUTORIO generado en PENDIENTE_FIRMA.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Firmar fallo
- Ir a Pendientes de firma.
- Firmar documento FALLO_ABSOLUTORIO.
- Resultado esperado: pasa a FIRMADO.
- Bandeja esperada: Pendientes de notificacion (para notificar el fallo).
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Notificar fallo
- Ir a Pendientes de notificacion.
- Confirmar notificacion de fallo disponible.
- Registrar notificacion positiva del fallo.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Cerrar acta
- Si cerrable=true, ejecutar Cerrar acta.
- Resultado esperado: acta pasa a CERRADA.
- Ir a bandeja Cerradas y confirmar presencia de ACTA-0006.
- Confirmar resultado ABSUELTO visible.
- Confirmar que NO aparecen acciones internas indebidas en CERRADAS:
  - [ ] No aparece Solicitar pago voluntario
  - [ ] No aparece Archivar acta
  - [ ] No aparece Derivar a gestion externa
  - [ ] No aparece Dictar fallo
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: CERRADA / ABSUELTO / en bandeja Cerradas.

---

## 12. Caso ACTA-0027 y ACTA-0028 — Fallo condenatorio + apelacion

Bandeja inicial esperada despues de reset (ambas):
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: TRANSITO
- Actas a buscar: ACTA-0027 / ACTA-0028
- Numeros visibles: A-2026-0027 / A-2026-0028
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_REVISION
- Accion inicial esperada: Dictar fallo condenatorio

ACTA-0027: Demo Apelacion Portal (DNI 40.001.027)
ACTA-0028: Demo Apelacion Presencial (DNI 40.001.028)

Sub-caso ACTA-0027 (apelacion por portal):

Paso 1: Buscar ACTA-0027 en Pendientes de analisis. Copiar estado.
Paso 2: Dictar fallo condenatorio (indicar monto si es requerido).
Paso 3: Ir a Pendientes de firma. Firmar fallo.
Paso 4: Ir a Pendientes de notificacion. Registrar notificacion de fallo positiva.
Paso 5: Confirmar que acta pasa a Con apelacion (CON_APELACION) o permanece con accion de apelacion disponible.
Paso 6: Registrar apelacion (canal: PORTAL_INFRACTOR).
Paso 7: Confirmar estado CON_APELACION o equivalente. Copiar estado.
Paso 8: Resolver apelacion. Opciones esperadas: ACEPTADA_ABSUELVE / RECHAZADA.
Paso 9: Copiar estado post-resolucion.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Sub-caso ACTA-0028 (apelacion presencial):
(Igual que ACTA-0027 pero con canal PRESENCIAL_DIRECCION.)

Paso 1: Buscar ACTA-0028. Copiar estado.
Paso 2: Dictar fallo condenatorio.
Paso 3: Firmar.
Paso 4: Notificar fallo.
Paso 5: Registrar apelacion (canal: PRESENCIAL_DIRECCION).
Paso 6: Resolver apelacion con resultado distinto al de ACTA-0027 para comparar.
Paso 7: Copiar estado final.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado:
- Si apelacion ACEPTADA_ABSUELVE: resultado cambia a ABSUELTO.
- Si apelacion RECHAZADA: condena se mantiene; puede seguir hacia CONDENA_FIRME.

---

## 13. Caso ACTA-0029 — Condena firme + pago condena sin gestion externa

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: TRANSITO
- Acta a buscar: ACTA-0029
- Numero visible: A-2026-0029
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_REVISION / montoCondena=null
- Accion inicial esperada: Dictar fallo condenatorio

Objetivo: Validar el circuito incorporado en Slice 19A (condena firme + pago condena directo, sin GESTION_EXTERNA).

Paso 1: Estado inicial
- Buscar ACTA-0029 en Pendientes de analisis.
- Confirmar PENDIENTE_REVISION y montoCondena=null.
- Copiar estado completo.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Dictar fallo condenatorio
- Accion: Dictar fallo condenatorio con montoCondena=1700 (o valor disponible en formulario).
- Resultado esperado: documento FALLO_CONDENATORIO generado en PENDIENTE_FIRMA.
- Copiar estado post-accion.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Firmar fallo
- Ir a Pendientes de firma.
- Firmar FALLO_CONDENATORIO.
- Resultado esperado: pasa a FIRMADO.
- Bandeja esperada: Pendientes de notificacion.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Notificar fallo
- Ir a Pendientes de notificacion.
- Registrar notificacion positiva del fallo.
- Resultado esperado: notificacion ENTREGADA.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Ejecutar vencimiento plazo apelacion
- Si no se presento apelacion, ejecutar vencimiento del plazo mock.
- Accion disponible: Vencer plazo de apelacion / Ejecutar vencimiento mock / equivalente.
- Resultado esperado: acta pasa a CONDENA_FIRME.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 6: Informar pago condena
- Confirmar que accion Informar pago condena esta disponible.
- Ejecutar: Informar pago condena.
- Resultado esperado: situacion pago condena pasa a INFORMADO.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 7: Confirmar pago condena
- Accion: Confirmar pago condena.
- Resultado esperado: situacion pago condena pasa a CONFIRMADO; cerrable=true.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 8: Cerrar acta
- Confirmar que cerrable=true.
- Accion: Cerrar acta.
- Resultado esperado: acta pasa a CERRADA.
- Ir a bandeja Cerradas. Confirmar ACTA-0029 presente.
- Confirmar que NO paso por GESTION_EXTERNA ni APREMIO.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: CERRADA / CONDENA_FIRME / pago condena CONFIRMADO / en Cerradas.

---

## 14. Caso ACTA-0030 — Fallo condenatorio + apelacion ACEPTADA_ABSUELVE

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: TRANSITO
- Acta a buscar: ACTA-0030
- Numero visible: A-2026-0030
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_REVISION
- Accion inicial esperada: Dictar fallo condenatorio

Nota sobre este caso: En el dataset, ACTA-0030 es "Demo Apelacion Absuelve".
VERIFICAR con GET /api/prototipo/actas/ACTA-0030 el estado real post-reset
si el flujo esperado es ACEPTADA_ABSUELVE o incluye circuito GESTION_EXTERNA/APREMIO.

Pasos esperados segun dataset (apelacion aceptada):

Paso 1: Buscar ACTA-0030. Copiar estado.
Paso 2: Dictar fallo condenatorio.
Paso 3: Firmar fallo.
Paso 4: Notificar fallo positivamente.
Paso 5: Registrar apelacion.
Paso 6: Resolver apelacion con ACEPTADA_ABSUELVE.
Paso 7: Confirmar resultado ABSUELTO.
Paso 8: Si cerrable=true, cerrar.
Paso 9: Confirmar estado final en Cerradas.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Si el caso de gestion externa APREMIO + reingreso + pago se necesita validar:
- VERIFICAR CON GET /api/prototipo/bandejas cual acta esta en GESTION_EXTERNA con tipo APREMIO.
- ACTA-0017 esta en GESTION_EXTERNA tipo Juzgado de Paz. Para APREMIO, buscar en dataset.
- Estado: [ ] VERIFICAR

Estado final esperado: segun resolucion de apelacion.
Si ACEPTADA_ABSUELVE: ABSUELTO / CERRADA.

---

## 15. Caso ACTA-0019 — Bloqueantes materiales completos

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0019
- Numero visible: A-2026-0019
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_CIERRE_MATERIAL / resultado ABSUELTO
- Accion inicial esperada: Resolver bloqueantes materiales (no se puede cerrar aun)

Infractor: Demo Cerrabilidad — DNI 22.222.222
Bloqueantes materiales activos al inicio:
- ENTREGA_DOCUMENTACION (retencion documental)
- LEVANTAMIENTO_MEDIDA_PREVENTIVA (medida preventiva activa)
- LIBERACION_RODADO (secuestro rodado)
Objetivo: Validar que el acta no cierra hasta resolver todos los bloqueantes.

Paso 1: Estado inicial
- Buscar ACTA-0019 en Pendientes de analisis.
- Confirmar los tres bloqueantes materiales activos.
- Confirmar que cerrable=false.
- Confirmar que accion Cerrar acta NO aparece o aparece bloqueada.
- Copiar estado completo incluyendo bloqueantes.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Verificar documentos pendientes
- Confirmar que existe documento MEDIDA_PREVENTIVA en PENDIENTE_FIRMA.
- Firmar MEDIDA_PREVENTIVA.
- Confirmar que la firma NO elimina automaticamente el bloqueante LEVANTAMIENTO_MEDIDA_PREVENTIVA.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Registrar cumplimiento material — LEVANTAMIENTO_MEDIDA_PREVENTIVA
- Accion: Registrar cumplimiento material LEVANTAMIENTO_MEDIDA_PREVENTIVA.
- Resultado esperado: bloqueante LEVANTAMIENTO_MEDIDA_PREVENTIVA desaparece.
- Copiar estado. Confirmar 2 bloqueantes restantes.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Registrar cumplimiento material — LIBERACION_RODADO
- Accion: Registrar cumplimiento material LIBERACION_RODADO.
- Resultado esperado: bloqueante LIBERACION_RODADO desaparece.
- Copiar estado. Confirmar 1 bloqueante restante.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 5: Registrar cumplimiento material — ENTREGA_DOCUMENTACION
- Accion: Registrar cumplimiento material ENTREGA_DOCUMENTACION.
- Resultado esperado: bloqueante ENTREGA_DOCUMENTACION desaparece.
- Copiar estado. Confirmar cerrable=true.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 6: Cerrar acta
- Confirmar que cerrable=true y no quedan bloqueantes.
- Accion: Cerrar acta.
- Resultado esperado: acta pasa a CERRADA.
- Ir a Cerradas. Confirmar ACTA-0019 presente.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Si aparece HTTP 409:

    Accion ejecutada:
    Mensaje exacto del error:
    Estado actual del acta:
    Documentos pendientes:
    Bloqueantes activos:
    Cerrable: / Motivo:

Estado final esperado: CERRADA / ABSUELTO / sin bloqueantes / en Cerradas.

---

## 16. Caso ACTA-0122 — Condena firme + pago condena directo (hito canonico)

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de analisis (PENDIENTE_ANALISIS)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: TRANSITO
- Acta a buscar: ACTA-0122
- Numero visible: A-2026-0122
- Estado inicial esperado: etapa D5_ANALISIS / situacion PENDIENTE_REVISION / resultadoFinal=CONDENA_FIRME / monto=95000
- Accion inicial esperada: Informar pago condena (condena ya firme segun dataset)

Infractor: Varela, Diego — DNI 43.001.122
Contexto: ACTA-0122 es el hito canonico 13: condena firme ya establecida, pago condena pendiente.
El fallo, la firma y la notificacion ya ocurrieron en el dataset. El plazo vencio sin apelacion.

Paso 1: Estado inicial
- Buscar ACTA-0122 en Pendientes de analisis.
- Confirmar resultadoFinal=CONDENA_FIRME, montoCondena=95000.
- Confirmar que accion Informar pago condena esta disponible.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Informar pago condena
- Accion: Informar pago condena.
- Resultado esperado: situacion pago condena pasa a INFORMADO.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Confirmar pago condena
- Accion: Confirmar pago condena.
- Resultado esperado: pago condena CONFIRMADO; cerrable=true.
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Cerrar acta
- Accion: Cerrar acta.
- Resultado esperado: CERRADA.
- Ir a Cerradas. Confirmar ACTA-0122 presente.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: CERRADA / CONDENA_FIRME / pago CONFIRMADO / en Cerradas.

---

## 17. Caso ACTA-0007 — Archivo y reingreso

Bandeja inicial esperada despues de reset:
- Bandeja UX: Archivo (ARCHIVO)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0007
- Numero visible: A-2026-0007
- Estado inicial esperado: etapa ARCHIVO / situacion ARCHIVADA_OPERATIVA
- Accion inicial esperada: Reingresar acta (ya esta archivada al inicio del reset)

Infractor: Torres, Patricia — DNI 34.200.901
Hecho: Uso de celular al conducir (fotografia probatoria).
Contexto: ACTA-0007 ya esta en ARCHIVO desde el reset. No hay que archivarla.

Paso 1: Estado inicial
- Ir a bandeja Archivo.
- Buscar ACTA-0007.
- Confirmar ARCHIVADA_OPERATIVA.
- Confirmar que NO aparecen acciones internas indebidas:
  - [ ] No aparece Solicitar pago voluntario
  - [ ] No aparece Cerrar acta
  - [ ] No aparece Derivar a gestion externa
- Confirmar que accion Reingresar acta esta disponible (permiteReingreso=true).
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Nota: Para probar flujo de archivado desde analisis, usar una acta en PENDIENTE_ANALISIS
con accion Archivar disponible. VERIFICAR CON GET /api/prototipo/bandejas.

Paso 2: Reingresar acta
- Accion: Reingresar acta.
- Resultado esperado: acta pasa a PENDIENTE_ANALISIS.
- Bandeja esperada: Pendientes de analisis.
- Copiar estado post-reingreso.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Confirmar en Pendientes de analisis
- Ir a Pendientes de analisis.
- Confirmar que ACTA-0007 aparece.
- Confirmar accion pendiente esperada segun flujo.
- Si aparece HTTP 500 al reingresar o al listar, registrar como bug critico.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: ACTA-0007 en PENDIENTE_ANALISIS, observable en Pendientes de analisis.

---

## 18. Caso ACTA-0017 — Gestion externa / retorno

Bandeja inicial esperada despues de reset:
- Bandeja UX: Gestion externa (GESTION_EXTERNA)
- Filtro operativo, si aplica: Todos
- Filtro dependencia, si aplica: Todos
- Acta a buscar: ACTA-0017
- Numero visible: A-2026-0017
- Estado inicial esperado: etapa GESTION_EXTERNA / situacion EN_GESTION_EXTERNA (tipo: Juzgado de Paz)
- Accion inicial esperada: Retornar de gestion externa

Infractor: Ocampo, Ricardo — DNI 27.115.443
Hecho: Conducir con VTV vencida; fallo firme derivado a Juzgado de Paz.

Paso 1: Estado inicial
- Ir a bandeja Gestion externa.
- Buscar ACTA-0017.
- Confirmar EN_GESTION_EXTERNA, tipo Juzgado de Paz.
- Confirmar que NO aparecen acciones internas indebidas en Gestion externa:
  - [ ] No aparece Solicitar pago voluntario
  - [ ] No aparece Cerrar acta
  - [ ] No aparece Dictar fallo
  - [ ] No aparece Archivar acta
- Confirmar que accion Retornar de gestion externa esta disponible (permiteReingreso=true).
- Copiar estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Retornar de gestion externa
- Accion: Retornar de gestion externa.
- Resultado esperado: acta pasa a Pendientes de analisis.
- estadoProceso esperado: PENDIENTE_REVISION o REVISION_POST_GESTION_EXTERNA.
- Copiar estado post-retorno.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Confirmar en Pendientes de analisis
- Ir a Pendientes de analisis.
- Confirmar que ACTA-0017 aparece.
- Confirmar accion pendiente (re-derivacion disponible solo si corresponde).
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Adicionalmente (opcional): ACTA-0015 esta en Pendientes de analisis con accion DERIVAR_GESTION_EXTERNA.
- VERIFICAR CON GET /api/prototipo/actas/ACTA-0015.
- Si se ejecuta derivacion, confirmar que ACTA-0015 pasa a Gestion externa.

Estado final esperado: ACTA-0017 en PENDIENTE_ANALISIS / REVISION_POST_GESTION_EXTERNA.

---

## 19. Caso ACTA-0114 — PARALIZADAS + reactivacion

Bandeja inicial esperada despues de reset:
- Bandeja UX: Paralizadas (PARALIZADAS)
- Filtro operativo, si aplica: Todos (o "Paralizacion espera documental" si el filtro lo muestra)
- Filtro dependencia, si aplica: TRANSITO
- Acta a buscar: ACTA-0114
- Numero visible: A-2026-0114
- Estado inicial esperado: etapa D5_ANALISIS / situacion PARALIZADA / accion PARALIZACION_ESPERA_DOCUMENTAL
- Accion inicial esperada: Reactivar acta

Infractor: Demo Paralizada Espera Documental — DNI 45.001.114
Hecho: Paralizada por espera de documentacion probatoria del infractor.

Paso 1: Estado inicial
- Ir a bandeja Paralizadas.
- Buscar ACTA-0114.
- Confirmar estado PARALIZADA, accion PARALIZACION_ESPERA_DOCUMENTAL.
- Confirmar que NO aparecen acciones internas indebidas en Paralizadas:
  - [ ] No aparece Solicitar pago voluntario
  - [ ] No aparece Cerrar acta
  - [ ] No aparece Registrar cumplimiento material
  - [ ] No aparece Derivar a gestion externa
  - [ ] No aparece Dictar fallo
  - [ ] No aparece Registrar apelacion
- Confirmar que accion Reactivar acta SI aparece.
- Copiar estado completo.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Reactivar acta
- Accion: Reactivar acta.
- Resultado esperado:
  - Sale de bandeja Paralizadas.
  - Entra en Pendientes de analisis.
  - estadoProceso = PENDIENTE_REVISION.
  - accionPendiente = REVISION_POST_REACTIVACION.
  - Evento ACTA_REACTIVADA_DESDE_PARALIZADAS visible en historial si se muestra.
- Copiar estado completo post-reactivacion.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Confirmar en Pendientes de analisis
- Ir a Pendientes de analisis.
- Confirmar que ACTA-0114 aparece.
- Confirmar accionPendiente = REVISION_POST_REACTIVACION.
- Confirmar que las acciones operativas disponibles se reevaluan correctamente segun el nuevo estado.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Estado final esperado: ACTA-0114 en PENDIENTE_ANALISIS, estadoProceso=PENDIENTE_REVISION, accionPendiente=REVISION_POST_REACTIVACION.

---

## 20. Caso Correo postal

Bandeja inicial esperada despues de reset:
- Bandeja UX: Pendientes de notificacion (PENDIENTE_NOTIFICACION) — canal CORREO_POSTAL
- Filtro operativo, si aplica: Correo postal (si el filtro existe en la UX)
- Filtro dependencia, si aplica: Todos
- Actas candidatas segun dataset:
  - ACTA-0037 (A-2026-0037): FALLO_CONDENATORIO — candidata a lote
  - ACTA-0038 (A-2026-0038): ACTA_INFRACCION — candidata a lote
  - ACTA-0039 (A-2026-0039): FALLO_CONDENATORIO 2 — candidata a lote
  - ACTA-0040 (A-2026-0040): FALLO_ABSOLUTORIO — candidata a lote
- Estado inicial esperado: PENDIENTE_ENVIO / canal CORREO_POSTAL
- Accion inicial esperada: VERIFICAR CON GET /api/prototipo/bandejas

Paso 1: Localizar correo postal en la UX
- Ir a Pendientes de notificacion.
- Confirmar si existe filtro operativo "Correo postal".
- Confirmar que ACTA-0037, ACTA-0038, ACTA-0039, ACTA-0040 aparecen filtradas por correo postal.
- Copiar estado de al menos ACTA-0037.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 2: Distinguir correo postal de notificador municipal
- Confirmar que Notificador municipal (ACTA-0031, ACTA-0032) aparece como opcion separada.
- Confirmar que no se mezclan visualmente correo postal y notificador municipal.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 3: Validar operacion de lote (si disponible)
- Si existe accion Generar lote de correo postal o equivalente, ejecutarla.
- Copiar resultado: cantidad de actas incluidas en el lote.
- Confirmar que solo incluye las candidatas (ACTA-0037 a ACTA-0040, segun criterio).
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Paso 4: Copiar estado de actas involucradas
- Copiar estado de ACTA-0037 antes y despues de la operacion de lote.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

Nota: VERIFICAR con GET /api/prototipo/bandejas si existe una bandeja especifica de operaciones
de correo postal separada de la bandeja de notificaciones individuales.

---

## 21. Guards negativos — Cerradas / Archivo / Gestion externa / Paralizadas

Bandejas a verificar:
- CERRADAS: usar ACTA-0008 (A-2026-0008) ya presente desde reset
- ARCHIVO: usar ACTA-0007 (A-2026-0007) ya presente desde reset
- GESTION_EXTERNA: usar ACTA-0017 (A-2026-0017) ya presente desde reset
- PARALIZADAS: usar ACTA-0114 (A-2026-0114) ya presente desde reset
- Filtro operativo: Todos
- Filtro dependencia: Todos

Para cada bandeja, seleccionar el acta indicada y verificar que NO aparecen las siguientes acciones:

CERRADAS (ACTA-0008):
- [ ] No aparece Solicitar pago voluntario
- [ ] No aparece Informar pago voluntario
- [ ] No aparece Confirmar pago voluntario
- [ ] No aparece Cerrar acta
- [ ] No aparece Archivar acta
- [ ] No aparece Derivar a gestion externa
- [ ] No aparece Registrar cumplimiento material
- [ ] No aparece Dictar fallo
- [ ] No aparece Registrar apelacion
Solo lectura permitida en CERRADAS.
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

ARCHIVO (ACTA-0007):
- [ ] No aparece Solicitar pago voluntario
- [ ] No aparece Cerrar acta
- [ ] No aparece Derivar a gestion externa
- [ ] No aparece Dictar fallo
- [ ] Si puede aparecer Reingresar acta (si permiteReingreso=true)
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

GESTION_EXTERNA (ACTA-0017):
- [ ] No aparece Solicitar pago voluntario
- [ ] No aparece Cerrar acta
- [ ] No aparece Dictar fallo
- [ ] No aparece Archivar acta
- [ ] No aparece Registrar cumplimiento material
- [ ] Si puede aparecer Retornar de gestion externa (si permiteReingreso=true)
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

PARALIZADAS (ACTA-0114):
- [ ] No aparece Solicitar pago voluntario
- [ ] No aparece Cerrar acta
- [ ] No aparece Dictar fallo
- [ ] No aparece Archivar acta
- [ ] No aparece Derivar a gestion externa
- [ ] No aparece Registrar cumplimiento material
- [ ] No aparece Registrar apelacion
- [ ] Si aparece Reactivar acta
- Estado: [ ] OK / [ ] BUG / [ ] NO VALIDADO

---

## 22. Seguimiento de acta entre bandejas

Checklist transversal a completar durante los casos anteriores.

Por cada cambio de bandeja que ocurra en la sesion, registrar:

    Caso:
    Acta:
    Bandeja origen:
    Accion ejecutada:
    Bandeja destino esperada:
    Bandeja destino obtenida:
    Sistema selecciono el acta automaticamente en nueva bandeja: SI / NO
    Hubo que buscarla manualmente: SI / NO
    Tiempo estimado de busqueda manual:
    Observacion UX:

Criterios de calidad a verificar:

- [ ] El sistema sigue seleccionando el acta despues de un cambio de bandeja
- [ ] Si no lo hace, queda registrado como mejora UX
- [ ] La busqueda por numero visible (A-2026-XXXX) funciona en todas las bandejas

Propuesta de mejora si la UX dificulta el seguimiento:
- Implementar preferencia local/session storage para "ultimo acta activa" que persista
  al cambiar de bandeja.
- VERIFICAR si esto ya existe o si hay un mecanismo equivalente.

---

## 23. Resultado final

| Caso | Acta | Estado | Bug | Slice sugerido |
|------|------|--------|-----|----------------|
| Visual base | — | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0001 Notificacion | ACTA-0001 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0011 Resolucion | ACTA-0011 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0014 Rectificacion | ACTA-0014 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0012 Nulidad | ACTA-0012 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0013 Piezas multiples | ACTA-0013 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0018 Pago voluntario | ACTA-0018 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0020 Medida/Pago obs. | ACTA-0020 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0004 Notificacion comun | ACTA-0004 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0006 Fallo absolutorio | ACTA-0006 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0027 Apelacion portal | ACTA-0027 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0028 Apelacion presencial | ACTA-0028 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0029 Condena firme + pago | ACTA-0029 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0030 Apelacion absuelve | ACTA-0030 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0019 Bloqueantes materiales | ACTA-0019 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0122 Condena firme canonico | ACTA-0122 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0007 Archivo / reingreso | ACTA-0007 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0017 Gestion externa | ACTA-0017 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| ACTA-0114 Paralizadas + reactivacion | ACTA-0114 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| Correo postal | ACTA-0037/38/39/40 | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| Guards negativos | varios | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |
| Seguimiento entre bandejas | transversal | [ ] OK / [ ] BUG / [ ] NO VALIDADO | | |

---

## 24. Plantilla para copiar a ChatGPT en cada paso

Copiar y completar antes de cada pregunta al asistente:

    Caso:
    Acta:
    Bandeja inicial esperada:
    Bandeja donde estoy:
    Filtro operativo:
    Filtro dependencia:

    Estado actual copiado:
    - Etapa (dominio):
    - Situacion (estadoProceso):
    - Resultado final:
    - Situacion pago voluntario:
    - Situacion pago condena:
    - Accion pendiente:
    - Cerrable:
    - Motivo cerrabilidad:
    - Acciones visibles:
    - Documentos pendientes:
    - Bloqueantes activos:
    - Eventos relevantes:

    Accion que ejecute:
    Resultado obtenido:
    Bandeja destino esperada:
    Bandeja destino obtenida:
    HTTP status obtenido:
    Error, si hubo:
    Captura/nota:

    Pregunta:
    ¿Que sigue?

---

## 25. Criterio de cierre

La demo se considera completa para validacion si:

- [ ] Todos los casos criticos estan OK (o con BUG documentado y aceptado)
- [ ] Cada caso indica correctamente la bandeja inicial despues de reset
- [ ] No hay acciones indebidas en bandejas protegidas (Cerradas, Archivo, Gestion externa, Paralizadas)
- [ ] ACTA-0029 valida condena firme + pago condena sin pasar por Gestion externa ni Apremio
- [ ] ACTA-0030 valida apelacion aceptada ACEPTADA_ABSUELVE (o el circuito real segun dataset)
- [ ] ACTA-0114 valida PARALIZADAS + reactivacion con estadoProceso=PENDIENTE_REVISION y accionPendiente=REVISION_POST_REACTIVACION
- [ ] ACTA-0019 valida tres bloqueantes materiales: cierre bloqueado hasta resolver todos
- [ ] No hay mojibake visible
- [ ] No hay solapamiento grave de cards en listados
- [ ] No hay HTTP 500 inesperado
- [ ] Los HTTP 409, si aparecen, son esperados y tienen mensaje funcional claro
- [ ] El operador puede seguir cada acta entre bandejas, o si la UX lo dificulta, queda registrado como mejora

---

## Apendice — Mapa rapido de bandejas por acta (estado inicial tras reset)

| Acta | Numero | Bandeja inicial | Situacion inicial |
|------|--------|-----------------|-------------------|
| ACTA-0001 | A-2026-0001 | Pendientes redaccion/resolucion | PENDIENTE_PRODUCCION_PIEZAS / pieza NOTIFICACION_ACTA |
| ACTA-0002 | A-2026-0002 | Preparacion documental | PENDIENTE_GENERACION |
| ACTA-0003 | A-2026-0003 | Pendientes de firma | PENDIENTE_FIRMA |
| ACTA-0004 | A-2026-0004 | Pendientes de notificacion | PENDIENTE_ENVIO |
| ACTA-0005 | A-2026-0005 | En notificacion | EN_CURSO |
| ACTA-0006 | A-2026-0006 | Pendientes de analisis | PENDIENTE_REVISION / D5_ANALISIS |
| ACTA-0007 | A-2026-0007 | Archivo | ARCHIVADA_OPERATIVA |
| ACTA-0008 | A-2026-0008 | Cerradas | CERRADA |
| ACTA-0011 | A-2026-0011 | Pendientes redaccion/resolucion | PENDIENTE_RESOLUCION / pieza RESOLUCION |
| ACTA-0012 | A-2026-0012 | Pendientes redaccion/resolucion | PENDIENTE_NULIDAD / pieza NULIDAD |
| ACTA-0013 | A-2026-0013 | Pendientes redaccion/resolucion | PENDIENTE_MEDIDA_PREVENTIVA / piezas NOTIFICACION_ACTA + MEDIDA_PREVENTIVA |
| ACTA-0014 | A-2026-0014 | Pendientes redaccion/resolucion | PENDIENTE_RECTIFICACION / pieza RECTIFICACION |
| ACTA-0015 | A-2026-0015 | Pendientes de analisis | PENDIENTE_REVISION / accion DERIVAR_GESTION_EXTERNA disponible |
| ACTA-0016 | A-2026-0016 | Pendientes de analisis | PENDIENTE_REVISION / en ventana espera (no derivable aun) |
| ACTA-0017 | A-2026-0017 | Gestion externa | EN_GESTION_EXTERNA / Juzgado de Paz |
| ACTA-0018 | A-2026-0018 | Enriquecimiento | EN_CURSO / pago SIN_PAGO |
| ACTA-0019 | A-2026-0019 | Pendientes de analisis | PENDIENTE_CIERRE_MATERIAL / ABSUELTO / 3 bloqueantes |
| ACTA-0020 | A-2026-0020 | Pendientes redaccion/resolucion | PENDIENTE_PRODUCCION_PIEZAS / pieza MEDIDA_PREVENTIVA |
| ACTA-0021 | A-2026-0021 | Pendientes de analisis | PENDIENTE_CIERRE_MATERIAL / PAGO_CONFIRMADO / 3 bloqueantes |
| ACTA-0027 | A-2026-0027 | Pendientes de analisis | PENDIENTE_REVISION / D5_ANALISIS (apelacion portal) |
| ACTA-0028 | A-2026-0028 | Pendientes de analisis | PENDIENTE_REVISION / D5_ANALISIS (apelacion presencial) |
| ACTA-0029 | A-2026-0029 | Pendientes de analisis | PENDIENTE_REVISION / D5_ANALISIS / montoCondena=null |
| ACTA-0030 | A-2026-0030 | Pendientes de analisis | PENDIENTE_REVISION / D5_ANALISIS (apelacion absuelve) |
| ACTA-0037 | A-2026-0037 | Pendientes de notificacion | PENDIENTE_ENVIO / CORREO_POSTAL / FALLO_CONDENATORIO |
| ACTA-0038 | A-2026-0038 | Pendientes de notificacion | PENDIENTE_ENVIO / CORREO_POSTAL / ACTA_INFRACCION |
| ACTA-0039 | A-2026-0039 | Pendientes de notificacion | PENDIENTE_ENVIO / CORREO_POSTAL / FALLO_CONDENATORIO 2 |
| ACTA-0040 | A-2026-0040 | Pendientes de notificacion | PENDIENTE_ENVIO / CORREO_POSTAL / FALLO_ABSOLUTORIO |
| ACTA-0114 | A-2026-0114 | Paralizadas | PARALIZADA / PARALIZACION_ESPERA_DOCUMENTAL |
| ACTA-0120 | A-2026-0120 | Pendientes de analisis | PENDIENTE_REVISION / pago PAGO_INFORMADO |
| ACTA-0121 | A-2026-0121 | Pendientes de firma | PENDIENTE_FIRMA / FALLO_CONDENATORIO |
| ACTA-0122 | A-2026-0122 | Pendientes de analisis | PENDIENTE_REVISION / CONDENA_FIRME / monto=95000 |
| ACTA-0123 | A-2026-0123 | Enriquecimiento | EN_CURSO / dep INSPECCIONES |
| ACTA-0124 | A-2026-0124 | Enriquecimiento | EN_CURSO / dep FISCALIZACION |
| ACTA-0125 | A-2026-0125 | Enriquecimiento | EN_CURSO / dep BROMATOLOGIA |

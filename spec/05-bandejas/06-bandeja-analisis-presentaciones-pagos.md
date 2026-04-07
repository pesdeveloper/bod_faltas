# [CANONICO] BANDEJA — ANÁLISIS / PRESENTACIONES / PAGOS

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de análisis, presentaciones y pagos, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/07-integracion-externa.md`
- `spec/03-catalogos/08-motivos-cierre.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/05-bandeja-notificacion-acta-en-proceso.md`

---

# 1. Nombre de la bandeja

**Análisis / presentaciones / pagos**

---

# 2. Objetivo

Mostrar las actas ya notificadas que ingresan en la etapa de tratamiento administrativo posterior a la notificación del acta, donde pueden producirse presentaciones, comparecencias, descargos, registros de intención de pago, pagos confirmados u otras actuaciones previas al acto administrativo o al cierre del caso.

Esta bandeja representa una etapa de tratamiento activo del expediente, donde el caso puede seguir distintos caminos según lo que ocurra.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- indicador de presentaciones pendientes, si existieran
- indicador de pago o intención de pago, si existiera
- alertas administrativas relevantes
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = EN_ANALISIS`

o casos que, luego de una notificación válida del acta, pasan a etapa de tratamiento administrativo, presentación, análisis, eventual pago o decisión previa al acto administrativo.

Típicamente llegan aquí desde:

- `Notificación del acta en proceso`, luego de un acuse válido o situación equivalente que habilita avanzar

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- recibe descargo
- recibe comparecencia
- recibe presentación o aclaración
- registra intención de pago
- registra pago confirmado
- requiere continuar análisis
- queda en condiciones de pasar a acto administrativo
- se deriva a gestión externa
- se paraliza
- se archiva
- se anula, si excepcionalmente correspondiera

En términos simples: aquí se trata administrativamente el caso ya notificado, hasta definir su siguiente gran camino.

---

# 6. Acciones posibles

## 6.1 Registrar descargo
- **Evento o proceso que dispara:** `DESCARGO_PRESENTADO`
- **Resultado esperado:** el caso incorpora formalmente un descargo que debe ser considerado en el tratamiento
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.2 Registrar comparecencia
- **Evento o proceso que dispara:** `COMPARECENCIA_REGISTRADA`
- **Resultado esperado:** se registra una comparecencia relevante dentro del expediente
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.3 Incorporar presentación
- **Evento o proceso que dispara:** `PRESENTACION_INCORPORADA`
- **Resultado esperado:** el caso incorpora una presentación relevante para su tratamiento
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.4 Incorporar aclaración
- **Evento o proceso que dispara:** `ACLARACION_PRESENTADA`
- **Resultado esperado:** se agrega una aclaración relevante al expediente
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.5 Registrar solicitud de pago voluntario
- **Evento o proceso que dispara:** `SOLICITUD_PAGO_VOLUNTARIO`
- **Resultado esperado:** el caso incorpora una solicitud o manifestación vinculada a pago voluntario
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.6 Registrar intención de pago
- **Evento o proceso que dispara:** `INTENCION_PAGO_REGISTRADA`
- **Resultado esperado:** queda registrada una intención de pago con impacto operativo
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.7 Registrar pago pendiente de confirmación
- **Evento o proceso que dispara:** `PAGO_PENDIENTE_CONFIRMACION`
- **Resultado esperado:** el caso queda con antecedente de pago aún no consolidado
- **Destino:** sigue en `Análisis / presentaciones / pagos`

## 6.8 Registrar pago confirmado
- **Evento o proceso que dispara:** `PAGO_CONFIRMADO`
- **Resultado esperado:** el caso puede quedar en condición de cierre por pago confirmado, si esa es la regla aplicable
- **Destino:** `Cerradas`, o sigue a otra revisión final si el área lo valida como necesario

## 6.9 Completar análisis
- **Evento o proceso que dispara:** `ANALISIS_COMPLETADO`
- **Resultado esperado:** el caso queda listo para avanzar al tramo de acto administrativo
- **Destino:** `Pendiente de acto administrativo`

## 6.10 Derivar a acto administrativo
- **Evento o proceso que dispara:** proceso derivado del cierre de análisis; a validar con el área si alcanza con `ANALISIS_COMPLETADO` o si se requiere algo adicional
- **Resultado esperado:** el caso entra al circuito del acto administrativo
- **Destino:** `Pendiente de acto administrativo`

## 6.11 Derivar a gestión externa
- **Evento o proceso que dispara:** `DERIVACION_EXTERNA_ENVIADA`
- **Resultado esperado:** el caso sale al circuito externo correspondiente
- **Destino:** `Gestión externa`

## 6.12 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso deja el circuito activo normal
- **Destino:** `Paralizadas`

## 6.13 Archivar
- **Evento o proceso que dispara:** `ARCHIVO_DISPUESTO`
- **Resultado esperado:** el caso cierra pase a cerradas
- **Destino:** `Cerradas`

## 6.14 Anular excepcionalmente
- **Evento o proceso que dispara:** `ACTA_ANULADA`
- **Resultado esperado:** el caso queda invalidado y fuera del circuito normal
- **Destino:** `Cerradas`

---

# 7. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- puede haber varias presentaciones durante la misma etapa
- puede haber pagos confirmados que cierren el caso sin pasar por acto administrativo
- puede haber pagos que no alcancen por sí solos para cerrar
- debe validarse si todo descargo y comparecencia vive realmente aquí
- debe validarse si toda derivación a gestión externa parte desde esta bandeja
- debe validarse si archivar desde esta estación es normal o excepcional
- debe validarse si la anulación desde aquí existe en la práctica y con qué fundamento

---

# 8. Puntos a validar con Dirección de Faltas

- ¿Análisis, presentaciones y pagos conviven realmente en una misma bandeja?
- ¿Toda actuación posterior a la notificación del acta entra aquí?
- ¿El pago confirmado puede cerrar directamente el caso?
- ¿Qué casos pasan de aquí a acto administrativo y cuáles no?
- ¿La derivación a gestión externa sale realmente desde esta bandeja?
- ¿Archivar desde aquí es habitual o excepcional?
- ¿La anulación desde esta etapa existe en la práctica?
- ¿Falta alguna acción relevante propia de esta estación?

---

# 9. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si `EN_ANALISIS` alcanza como estado dominante para esta estación
- si conviene mantener juntas análisis, presentaciones y pagos
- qué acciones realmente forman parte de esta etapa
- si el pago confirmado puede cerrar directamente el caso
- si la salida natural desde aquí es hacia acto administrativo, gestión externa o cierre, según el caso
- si los catálogos actuales alcanzan para representar correctamente esta etapa sin sobrecomplicar el modelo

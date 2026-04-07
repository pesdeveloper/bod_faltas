# [CANONICO] BANDEJA — PENDIENTE DE NOTIFICACIÓN DEL ACTO

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de casos pendientes de notificación del acto administrativo, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/08-bandeja-acto-administrativo-en-proceso.md`

---

# 1. Nombre de la bandeja

**Pendiente de notificación del acto**

---

# 2. Objetivo

Mostrar las actas cuyo acto administrativo principal ya fue producido y firmado, pero cuya gestión concreta de notificación todavía no fue iniciada.

Esta bandeja representa la etapa previa al inicio efectivo de la notificación del acto administrativo.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- tipo de acto administrativo principal
- confirmación de que el acto se encuentra firmado
- canal de notificación previsto o seleccionado
- alertas o validaciones previas a notificación, si existieran
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = PENDIENTE_NOTIFICACION_ACTO`

o casos donde ya existe acto administrativo firmado y vigente, pero todavía no se inició su notificación efectiva.

Típicamente llegan aquí desde:

- `Acto administrativo en proceso`

luego de la firma válida del acto correspondiente.

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- está efectivamente listo para iniciar la notificación del acto
- requiere definir o ajustar canal de notificación
- requiere revisión previa final antes de notificar
- necesita incorporar una constancia o documento adicional
- debe volver a la etapa anterior por un problema excepcional
- debe paralizarse, si excepcionalmente correspondiera

En términos simples: aquí se prepara y habilita la salida efectiva de la notificación del acto.

---

# 6. Regla general de esta bandeja

La presencia del caso en esta bandeja implica que el acto administrativo principal ya se encuentra suficientemente formalizado para iniciar su notificación.

A diferencia de la preparación para notificación del acta, aquí no debería existir como situación normal un bloqueo por firma pendiente del acto principal, porque ese requisito ya debería estar resuelto antes del ingreso.

Si se detecta que el acto aún no estaba realmente en condiciones de notificarse, el caso debería volver a la etapa anterior.

---

# 7. Acciones posibles

## 7.1 Emitir notificación del acto
- **Evento o proceso que dispara:** `NOTIFICACION_ENVIADA`
- **Resultado esperado:** se inicia efectivamente la gestión de notificación del acto administrativo
- **Destino:** `Notificación del acto en proceso`

## 7.2 Definir o ajustar canal de notificación
- **Evento o proceso que dispara:** proceso operativo interno a validar; podría no requerir evento procesal específico si solo modifica metadata previa al envío
- **Resultado esperado:** queda determinado o corregido el canal por el cual se emitirá la notificación del acto
- **Destino:** sigue en `Pendiente de notificación del acto`

## 7.3 Revisar antes de emitir
- **Evento o proceso que dispara:** control operativo final; a validar si requiere o no evento explícito
- **Resultado esperado:** se confirma que el caso efectivamente puede emitirse o se detecta una inconsistencia excepcional
- **Destino:** sigue en `Pendiente de notificación del acto` o vuelve a `Acto administrativo en proceso`, a validar con el área

## 7.4 Incorporar constancia o documento complementario
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se agrega respaldo o documentación adicional necesaria para la notificación del acto
- **Destino:** sigue en `Pendiente de notificación del acto`

## 7.5 Volver a acto administrativo en proceso
- **Evento o proceso que dispara:** a validar con el área; podría resolverse por observación, invalidación, reemplazo o detección de inconsistencia
- **Resultado esperado:** se detecta que el acto todavía requiere corrección, sustitución o nueva revisión antes de notificar
- **Destino:** `Acto administrativo en proceso`

## 7.6 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

---

# 8. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- puede haber casos que entren aquí y se emitan casi de inmediato
- puede haber ajustes finales de canal antes de emitir
- debe validarse si “revisar antes de emitir” es una acción real o una práctica operativa implícita
- debe validarse si realmente puede ocurrir un retorno a la etapa de acto administrativo en proceso
- debe validarse si la paralización desde esta etapa existe en la práctica

---

# 9. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación separada y visible?
- ¿Todo acto firmado pasa por aquí antes de notificarse?
- ¿Es correcto que aquí ya no existan firmas pendientes del acto principal?
- ¿Qué acciones reales se hacen aquí además de emitir?
- ¿Se puede ajustar canal desde esta bandeja?
- ¿Puede detectarse aquí un problema que haga volver el caso a la etapa anterior?
- ¿La paralización desde esta bandeja existe en la práctica?
- ¿Falta alguna acción relevante previa a la emisión de la notificación del acto?

---

# 10. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si `PENDIENTE_NOTIFICACION_ACTO` alcanza como estado dominante para esta estación
- si esta bandeja existe realmente como paso separado
- si las acciones previas al envío efectivo del acto están bien representadas
- si el retorno a `Acto administrativo en proceso` es válido y en qué casos
- si el evento `NOTIFICACION_ENVIADA` queda correctamente ubicado recién en esta bandeja
- si el modelo de estados y eventos actuales alcanza para representar correctamente esta etapa
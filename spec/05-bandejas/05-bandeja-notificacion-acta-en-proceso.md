# [CANONICO] BANDEJA — NOTIFICACIÓN DEL ACTA EN PROCESO

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de notificación del acta en proceso, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/04-bandeja-actas-listas-para-notificar.md`

---

# 1. Nombre de la bandeja

**Notificación del acta en proceso**

---

# 2. Objetivo

Mostrar las actas cuya notificación ya fue efectivamente emitida y que actualmente se encuentran transitando el circuito operativo de notificación, a la espera de acuse, rechazo, reenvío u otro resultado relevante.

Esta bandeja representa la etapa en la que la notificación ya salió y ahora debe ser seguida hasta obtener un resultado suficiente para permitir el avance del caso.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- canal de notificación utilizado
- fecha y hora del último envío
- estado actual de la notificación
- indicador de acuse recibido o rechazado
- indicador de necesidad de reenvío, si existiera
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = NOTIFICACION_ACTA_EN_PROCESO`

o casos en los que ya se emitió efectivamente la notificación del acta y todavía no existe un resultado final suficiente del circuito notificatorio.

Típicamente llegan aquí desde:

- `Actas listas para notificar`

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si la notificación:

- recibió acuse válido
- recibió acuse rechazado
- requiere reenvío
- requiere cambio de canal
- requiere incorporación de constancia o soporte adicional
- debe mantenerse en seguimiento
- debe paralizarse, si excepcionalmente correspondiera

En términos simples: aquí se sigue la notificación ya emitida hasta obtener resultado útil.

---

# 6. Acciones posibles

## 6.1 Registrar acuse recibido
- **Evento o proceso que dispara:** `ACUSE_RECIBIDO`
- **Resultado esperado:** la notificación alcanza un resultado suficiente para considerar cumplida esta etapa
- **Destino:** `Análisis / presentaciones / pagos`

## 6.2 Registrar acuse rechazado
- **Evento o proceso que dispara:** `ACUSE_RECHAZADO`
- **Resultado esperado:** la notificación no logra efecto suficiente y el caso queda en situación de revisión/reenvío
- **Destino:** sigue en `Notificación del acta en proceso`

## 6.3 Reenviar notificación
- **Evento o proceso que dispara:** `NOTIFICACION_REENVIADA`
- **Resultado esperado:** se ejecuta un nuevo intento de notificación
- **Destino:** sigue en `Notificación del acta en proceso`

## 6.4 Cambiar canal de notificación
- **Evento o proceso que dispara:** proceso operativo interno a validar; podría acompañarse de nuevo envío o reenvío según el caso
- **Resultado esperado:** se redefine el canal por el cual continuará la gestión notificatoria
- **Destino:** sigue en `Notificación del acta en proceso`

## 6.5 Incorporar constancia o soporte de notificación
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se agrega respaldo documental del circuito notificatorio
- **Destino:** sigue en `Notificación del acta en proceso`

## 6.6 Mantener en seguimiento
- **Evento o proceso que dispara:** control operativo sin cambio procesal dominante; a validar con el área si requiere modelado explícito
- **Resultado esperado:** el caso permanece en seguimiento notificatorio
- **Destino:** sigue en `Notificación del acta en proceso`

## 6.7 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

---

# 7. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- algunos acuses pueden llegar muy rápido y sacar casi enseguida al caso de esta estación
- puede haber múltiples intentos de notificación dentro del mismo caso
- el cambio de canal puede ser excepcional o bastante habitual, según la materia y la práctica real
- debe validarse si la incorporación de constancias sucede realmente aquí
- debe validarse si la paralización desde esta etapa existe en la práctica

---

# 8. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación operativa separada?
- ¿Registrar acuse recibido y acuse rechazado son acciones reales de esta etapa?
- ¿El reenvío se hace efectivamente desde esta bandeja?
- ¿El cambio de canal sucede aquí o debería resolverse antes?
- ¿La incorporación de constancias de notificación ocurre realmente en esta estación?
- ¿Cuando se recibe acuse válido, el caso pasa efectivamente a análisis?
- ¿La paralización desde esta bandeja existe en la práctica?

---

# 9. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si `NOTIFICACION_ACTA_EN_PROCESO` alcanza como estado dominante para esta estación
- si las acciones del circuito notificatorio están correctamente representadas
- si el paso natural posterior al acuse recibido es `Análisis / presentaciones / pagos`
- si el modelo de estados y eventos de notificación actuales alcanza para representar correctamente esta etapa
- si el snapshot debe reflejar con más precisión reintentos, canal y resultado notificatorio

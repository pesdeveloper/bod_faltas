# [BANDEJA 14] FALLOS LISTOS PARA NOTIFICAR

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos cuyo **fallo ya fue generado**, cuya **documentación necesaria para notificarlo ya está completa y firmada**, y que por lo tanto ya están en condiciones de pasar a la **emisión efectiva de la notificación del fallo**.

Esta bandeja representa el tramo posterior a:

- `13-bandeja-fallo-pendiente-de-notificacion.md`

y previo a:

- `15-bandeja-notificacion-fallo-en-proceso.md`

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/13-bandeja-fallo-pendiente-de-notificacion.md`
- documentos canónicos de acto administrativo
- documentos canónicos de documento y firma
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `TipoDocumento`
  - `EstadoDocumento`
  - `FirmaTipo`
  - `CanalNotificacion`
  - `EstadoNotificacion`
  - `TipoEventoActa`

---

## 1. Nombre de la bandeja

**Bandeja — Fallos listos para notificar**

Nombre alternativo conversable con el área:

**Cola de salida de notificación del fallo**

---

## 2. Objetivo

Permitir concentrar y operar los casos que ya cumplieron todas las condiciones previas para iniciar la notificación del fallo.

La finalidad de esta bandeja es:

- separar claramente los fallos todavía incompletos de los fallos ya listos para salir
- mostrar la cola real de expedientes que pueden ser notificados
- permitir emitir la notificación del fallo solo cuando toda la documentación previa esté completa
- evitar que casos con documentación pendiente se mezclen con casos ya listos para notificación efectiva

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen fallo generado
- ya tienen toda la documentación necesaria para la notificación del fallo
- ya tienen todos los documentos requeridos firmados
- todavía no iniciaron la notificación efectiva del fallo

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha de generación del fallo
- tipo de fallo o acto resolutivo
- documentación previa requerida
- confirmación de documentación completa
- confirmación de firma completa
- canal o estrategia de notificación prevista
- fecha del último movimiento
- observaciones relevantes
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos cuyo fallo ya está formalmente listo para salir a notificación.

Casos típicos de ingreso:

- finalización positiva del circuito documental previo
- firma completa de todos los documentos requeridos
- validación administrativa de que ya puede emitirse la notificación del fallo
- pase desde la bandeja de fallo pendiente de notificación al completarse toda la preparación previa

Regla importante:

- esta bandeja es la **cola real previa a emitir la notificación del fallo**
- solo deben ingresar casos que ya estén completos desde el punto de vista documental y de firma

No deberían entrar aquí:

- casos con fallo aún no generado
- casos con documentación incompleta
- casos con firmas pendientes
- casos cuya notificación del fallo ya fue emitida y está en curso
- casos cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- qué casos ya están realmente listos para iniciar la notificación del fallo
- por qué canal o estrategia se notificará cada caso
- cuándo se emite efectivamente la notificación
- si algún expediente que parecía listo debe volver atrás por una incidencia de último momento

Esta bandeja no es de seguimiento del diligenciamiento.

Es la estación donde el expediente queda:

**formalmente listo para salir a notificación del fallo**

---

## 6. Acciones posibles

### 6.1 Emitir notificación del fallo

- **Acción:** emitir o iniciar formalmente la notificación del fallo
- **Evento o proceso que dispara:** decisión operativa de lanzar la notificación por el canal correspondiente
- **Resultado esperado:** se inicia la notificación efectiva del fallo y el caso sale de esta cola de preparación final
- **Destino:** `15-bandeja-notificacion-fallo-en-proceso.md`

---

### 6.2 Definir o ajustar canal de notificación

- **Acción:** definir, revisar o ajustar el canal previsto para la notificación del fallo
- **Evento o proceso que dispara:** validación operativa previa a la emisión o detección de necesidad de cambio
- **Resultado esperado:** el expediente queda correctamente preparado para salir por el canal adecuado
- **Destino:** permanece en `14-bandeja-fallos-listos-para-notificar.md`

---

### 6.3 Mantener en cola de salida

- **Acción:** dejar el caso en esta bandeja hasta que se emita efectivamente la notificación
- **Evento o proceso que dispara:** criterio operativo, prioridad, carga de trabajo o programación del diligenciamiento
- **Resultado esperado:** el expediente permanece visible como listo para notificar
- **Destino:** permanece en `14-bandeja-fallos-listos-para-notificar.md`

---

### 6.4 Revertir a preparación previa por incidencia

- **Acción:** devolver el caso a la etapa previa si se detecta una incidencia documental o formal antes de emitir la notificación
- **Evento o proceso que dispara:** detección de error, observación de último momento, inconsistencia de documentos o problema de firma
- **Resultado esperado:** el expediente deja de considerarse listo para notificar
- **Destino:** `13-bandeja-fallo-pendiente-de-notificacion.md`

---

### 6.5 Registrar decisión administrativa especial

- **Acción:** aplicar una decisión que altere el recorrido ordinario del caso
- **Evento o proceso que dispara:** instrucción de Dirección de Faltas u otra autoridad competente
- **Resultado esperado:** el expediente puede cambiar de destino sin seguir el recorrido normal previsto
- **Destino:** según la decisión adoptada

---

### 6.6 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, conflicto jurídico-administrativo, espera externa o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** bandeja de paralizadas

---

### 6.7 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada que determine que no corresponde continuar con la notificación del fallo por el circuito ordinario
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** bandeja de cerradas

---

## 7. Excepciones

### 7.1 Caso aparentemente listo pero con defecto no advertido

Puede ocurrir que el expediente figure como listo, pero se detecte a último momento:

- documento faltante
- error material
- firma inválida o ausente
- inconsistencia en los datos de notificación

En ese caso:

- no debe emitirse la notificación del fallo
- el caso debe volver a la bandeja previa

---

### 7.2 Cola operativa sin emisión inmediata

Puede ocurrir que un caso esté correctamente listo, pero que no se emita de inmediato por razones operativas.

En ese caso:

- debe permanecer en esta bandeja
- sin confundirse con expedientes cuya notificación ya está en curso

---

### 7.3 Cambio de canal antes de emitir

Puede ocurrir que el canal inicialmente previsto deba modificarse antes del lanzamiento efectivo.

En ese caso:

- el expediente puede permanecer en esta bandeja mientras se redefine el canal
- siempre que no requiera rehacer documentación o firmas

---

### 7.4 Simetría con “actas listas para notificar”

Debe validarse con el área si esta bandeja cumple, en la práctica, el mismo rol que ya fue definido para la etapa equivalente de la notificación del acta.

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Si el nombre “fallos listos para notificar” refleja bien la práctica real.

3. Si esta es efectivamente la cola real previa a emitir la notificación del fallo.

4. Qué validaciones mínimas deben estar completas para que un caso entre aquí.

5. Si puede ajustarse el canal de notificación desde esta bandeja sin volver atrás.

6. Qué incidencias obligan a sacar un caso de esta bandeja y devolverlo a preparación previa.

7. Si desde aquí puede existir cierre o paralización directa.

8. Si esta bandeja es operativamente equivalente a la ya definida para actas listas para notificar.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si esta estación operativa existe como bandeja real
- cuáles son exactamente sus criterios de ingreso
- qué significa que un fallo esté verdaderamente listo para notificar
- qué diferencias reales existen con la bandeja previa
- qué acción concreta dispara la salida de esta bandeja
- qué incidencias justifican volver atrás
- y si esta bandeja funciona, en la práctica, como la cola real de salida hacia la notificación del fallo
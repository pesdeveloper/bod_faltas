# [BANDEJA 13] FALLO PENDIENTE DE NOTIFICACIÓN

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos en los que el **fallo ya fue generado**, junto con la documentación necesaria para su notificación, pero todavía no se completó el circuito previo indispensable para poder notificarlo efectivamente.

Esta bandeja representa el tramo posterior a:

- `12-bandeja-pendiente-de-fallo.md`

y previo a:

- la notificación efectiva del fallo
- o, más precisamente, al inicio del circuito de notificación del fallo una vez que toda la documentación requerida esté generada y firmada

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/12-bandeja-pendiente-de-fallo.md`
- `spec/05-bandejas/10-bandeja-notificacion-acto-en-proceso.md`
- `spec/05-bandejas/11-bandeja-actas-notificadas.md`
- documentos canónicos de acto administrativo
- documentos canónicos de documento y firma
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `TipoDocumento`
  - `EstadoDocumento`
  - `FirmaTipo`
  - `EstadoActa`
  - `TipoEventoActa`

---

## 1. Nombre de la bandeja

**Bandeja — Fallo pendiente de notificación**

Nombre alternativo conversable con el área:

**Fallos listos para preparar notificación**

---

## 2. Objetivo

Permitir controlar los casos cuyo fallo ya fue resuelto y redactado, junto con la documentación asociada, pero que aún no están en condiciones de ser notificados porque todavía falta completar la preparación formal previa.

La finalidad de esta bandeja es:

- concentrar los casos cuyo fallo ya existe
- controlar que se hayan generado todos los documentos necesarios para notificarlo
- controlar que esos documentos estén correctamente confirmados y firmados
- evitar que un caso pase a notificación del fallo sin la documentación previa completa
- dejar visible qué expedientes están demorados por firma, observación o documentación faltante

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen fallo generado
- ya tienen iniciada la generación de la documentación necesaria para notificar ese fallo
- todavía no cuentan con todos los documentos requeridos en estado apto para notificación
- aún no pasaron a la cola real de notificación del fallo

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha de generación del fallo
- tipo de fallo o acto resolutivo generado
- documentos requeridos para la notificación del fallo
- estado de cada documento relevante
- indicador de documentación completa / incompleta
- indicador de firma completa / pendiente
- observaciones o incidencias
- fecha del último movimiento
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos en que ya se generó el fallo, pero todavía no se completó el circuito previo necesario para notificarlo.

Casos típicos de ingreso:

- generación de fallo con documentos pendientes de firma
- generación de fallo con notificación asociada aún incompleta
- caso con fallo redactado pero con documentación observada
- caso con uno o más documentos requeridos aún no confirmados o no firmados
- caso en que el fallo existe, pero todavía no quedó formalmente listo para salir a notificación

Regla importante:

- el fallo no debe pasar a la etapa de notificación efectiva hasta que estén **todos los documentos requeridos generados y firmados**
- esta lógica es equivalente a la ya definida para el tramo previo a la notificación del acta

No deberían entrar aquí:

- casos que todavía están en `12-bandeja-pendiente-de-fallo.md`
- casos cuyo fallo ya está completamente listo para notificar y deberían pasar a la cola real de notificación
- casos ya en notificación del fallo en curso
- casos cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- si el fallo ya quedó formalmente completo
- si toda la documentación necesaria para notificarlo fue generada
- si todos los documentos requeridos fueron firmados
- si existe alguna observación, incidencia o corrección pendiente
- si el expediente ya puede pasar a la instancia de notificación del fallo

Esta bandeja funciona como la estación de control previo a la notificación del fallo.

El criterio central es:

**no se debe iniciar la notificación del fallo hasta que toda la documentación requerida esté completa y firmada**

---

## 6. Acciones posibles

### 6.1 Generar documentación faltante

- **Acción:** generar uno o más documentos aún faltantes para completar el circuito previo a la notificación del fallo
- **Evento o proceso que dispara:** detección de que la documentación necesaria no está completa
- **Resultado esperado:** el expediente avanza hacia un estado documental íntegro
- **Destino:** permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`

---

### 6.2 Confirmar documentación generada

- **Acción:** confirmar que los documentos redactados quedan listos para firma o uso posterior
- **Evento o proceso que dispara:** revisión administrativa de la documentación preparada
- **Resultado esperado:** los documentos quedan formalmente preparados para continuar el circuito
- **Destino:** permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`

---

### 6.3 Enviar documentos a firma

- **Acción:** iniciar el circuito de firma de los documentos que lo requieran
- **Evento o proceso que dispara:** existencia de documentos generados y confirmados pendientes de firma
- **Resultado esperado:** los documentos pasan al circuito de firma correspondiente
- **Destino:** permanece en `13-bandeja-fallo-pendiente-de-notificacion.md` hasta que se complete la firma requerida

---

### 6.4 Registrar firma completa de documentos requeridos

- **Acción:** registrar que todos los documentos necesarios ya quedaron firmados
- **Evento o proceso que dispara:** finalización positiva del circuito de firma
- **Resultado esperado:** el caso queda formalmente listo para iniciar la notificación del fallo
- **Destino:** bandeja de **fallos listos para notificar** o instancia equivalente previa a la notificación efectiva del fallo

---

### 6.5 Corregir documentación observada

- **Acción:** corregir fallo o documentos asociados cuando exista observación o devolución
- **Evento o proceso que dispara:** observación administrativa, rechazo de firma, error detectado o inconsistencia documental
- **Resultado esperado:** la documentación vuelve a quedar apta para continuar el circuito
- **Destino:** permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`

---

### 6.6 Rehacer o ajustar el fallo

- **Acción:** revisar y rehacer el contenido resolutivo si se detecta una necesidad sustancial de corrección
- **Evento o proceso que dispara:** error material, observación sustantiva o decisión administrativa de ajuste
- **Resultado esperado:** el caso vuelve al punto necesario del circuito resolutivo
- **Destino:** según la magnitud del ajuste:
  - permanece en esta bandeja
  - o vuelve a `12-bandeja-pendiente-de-fallo.md` si corresponde reconsiderar la resolución

---

### 6.7 Mantener en espera documental o de firma

- **Acción:** dejar el caso en esta bandeja hasta que se complete lo pendiente
- **Evento o proceso que dispara:** falta de firma, documentación incompleta, prioridad operativa o espera administrativa
- **Resultado esperado:** el caso continúa visible como fallo generado pero aún no notificable
- **Destino:** permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`

---

### 6.8 Registrar decisión administrativa especial

- **Acción:** aplicar una decisión que altere el recorrido ordinario del caso
- **Evento o proceso que dispara:** instrucción de Dirección de Faltas u otra autoridad competente
- **Resultado esperado:** el expediente puede cambiar de destino sin seguir el recorrido normal previsto
- **Destino:** según la decisión adoptada

---

### 6.9 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, conflicto jurídico-administrativo, espera externa o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** bandeja de paralizadas

---

### 6.10 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada que determine que no corresponde continuar con la notificación del fallo en el circuito ordinario
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** bandeja de cerradas

---

## 7. Excepciones

### 7.1 Fallo generado pero documentación incompleta

Puede ocurrir que exista fallo, pero falte uno o más documentos indispensables para notificarlo correctamente.

En ese caso:

- el expediente no debe pasar a notificación
- debe permanecer en esta bandeja hasta completar la documentación requerida

---

### 7.2 Documentos generados pero no firmados

Puede ocurrir que la documentación exista, pero aún falte firma de uno o varios documentos.

En ese caso:

- el caso sigue en esta bandeja
- no debe pasar a la cola real de notificación del fallo

---

### 7.3 Observaciones sobre el fallo ya generado

Puede ocurrir que, luego de generado el fallo, se detecte un error material, inconsistencia o necesidad de ajuste.

En ese caso:

- podría bastar con corregir documentos
- o podría requerirse rehacer parcialmente la resolución
- debe evitarse que un fallo con observaciones avance a notificación

---

### 7.4 Similitud con el circuito previo a notificación del acta

Debe validarse con el área si este tramo se vive operativamente casi igual que el de preparación previa a la notificación del acta.

Si fuera así, eso refuerza la consistencia del modelo general.

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Si el nombre “fallo pendiente de notificación” refleja bien la práctica real.

3. Qué documentos consideran obligatorios para poder notificar un fallo.

4. Si el criterio es efectivamente que **todos los documentos requeridos deben estar firmados** antes de iniciar la notificación.

5. Si la operatoria de esta bandeja es, en la práctica, análoga a la preparación para notificación del acta.

6. Qué incidencias suelen demorar un caso en esta bandeja:
   - falta de firma
   - observaciones documentales
   - correcciones del fallo
   - ausencia de documentación
   - otras

7. Si desde aquí el paso natural es una cola específica de fallos listos para notificar.

8. Si puede existir cierre o paralización desde esta etapa.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si esta estación operativa existe como bandeja real
- qué casos ingresan exactamente
- qué documentación previa exige la notificación del fallo
- cuándo se considera que el expediente ya está listo para iniciar esa notificación
- qué diferencia real existe entre tener fallo generado y tener fallo listo para notificar
- qué incidencias mantienen al caso en esta bandeja
- y cuál es el criterio exacto de salida hacia la etapa de notificación del fallo
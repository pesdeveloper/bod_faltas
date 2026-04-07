# [BANDEJA 16] FALLOS NOTIFICADOS

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos cuya **notificación del fallo ya fue fehaciente** y que, a partir de ese momento, deben permanecer en **espera controlada** durante el plazo aplicable antes de pasar, en principio, a **gestión externa / apremios**, salvo que durante ese período ocurra una actuación que modifique el recorrido esperado.

Esta bandeja representa el tramo posterior a:

- `15-bandeja-notificacion-fallo-en-proceso.md`

y previo principalmente a:

- `18-bandeja-gestion-externa.md`

Sin perjuicio de que, durante la permanencia en esta bandeja, puedan ocurrir otras acciones administrativas no lineales.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/15-bandeja-notificacion-fallo-en-proceso.md`
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `CanalNotificacion`
  - `EstadoNotificacion`
  - `TipoEventoActa`
  - `EstadoActa`
  - `TipoGestionExterna`
  - `ResultadoExterno`

---

## 1. Nombre de la bandeja

**Bandeja — Fallos notificados**

Nombre alternativo conversable con el área:

**Fallos con notificación fehaciente**

---

## 2. Objetivo

Permitir el seguimiento de los casos cuyo fallo ya fue **notificado fehacientemente**, manteniéndolos en una bandeja de espera operativa durante el plazo que corresponda según la normativa o parametrización del sistema.

La finalidad de esta bandeja es:

- consolidar que la notificación del fallo ya fue fehaciente
- tomar como referencia la **fecha de acuse positivo**
- controlar el plazo posterior a la notificación del fallo
- permitir acciones administrativas no lineales mientras transcurre ese plazo
- derivar a **gestión externa / apremios** cuando se cumpla el plazo configurado, si no hubo una actuación que altere el recorrido esperado
- permitir, si corresponde, el pase manual a gestión externa / apremios

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen fallo generado
- ya tienen notificación fehaciente del fallo
- se encuentran dentro del plazo de espera posterior a la notificación
- todavía no pasaron a gestión externa / apremios
- no tienen, por el momento, una actuación que las haya desviado a otro circuito

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha del fallo
- fecha de acuse positivo de la notificación del fallo
- fecha desde la cual corre el plazo
- cantidad de días transcurridos desde el acuse positivo
- cantidad de días restantes según la parametrización vigente
- fecha prevista de pase a gestión externa / apremios
- observaciones relevantes
- si tiene apelación, pago, presentación o incidencia abierta
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos en que la notificación del fallo quedó acreditada de manera fehaciente.

Casos típicos de ingreso:

- recepción de acuse positivo de la notificación del fallo
- constancia válida de entrega del fallo
- confirmación válida del canal que, conforme a la normativa y criterio administrativo, equivale a notificación fehaciente del fallo
- validación manual de que la notificación del fallo quedó perfeccionada

Regla importante:

- el ingreso a esta bandeja se produce cuando la notificación del fallo deja de estar “en proceso” y pasa a estar **cumplida fehacientemente**
- la fecha base para el cómputo del plazo debe ser la **fecha de acuse positivo**
- el plazo no debe quedar fijo en el documento; debe venir de configuración del sistema según normativa aplicable

No deberían entrar aquí:

- casos cuya notificación del fallo todavía está en curso
- casos sin constancia suficiente de notificación fehaciente del fallo
- casos que ya pasaron a gestión externa / apremios
- casos ya cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver operativamente:

- que el caso permanezca en espera durante el plazo correspondiente
- que se compute correctamente el tiempo desde la fecha de acuse positivo de la notificación del fallo
- que, cumplido ese plazo, el caso pase a gestión externa / apremios
- que durante esa espera puedan registrarse acciones o decisiones administrativas que alteren el recorrido esperado

Es importante dejar explícito que esta bandeja:

- **no implica** que toda acta deba necesariamente apelar
- **no implica** que toda acta deba tener otra actuación antes del pase a gestión externa
- funciona como una estación de espera posterior a la notificación fehaciente del fallo
- admite acciones posibles, pero no presupone que deban ocurrir necesariamente

---

## 6. Acciones posibles

### 6.1 Mantener en espera hasta cumplimiento del plazo

- **Acción:** mantener el caso en esta bandeja hasta que se cumpla el plazo configurado
- **Evento o proceso que dispara:** permanencia natural del caso desde la fecha de acuse positivo de la notificación del fallo
- **Resultado esperado:** el expediente sigue en espera operativa controlada
- **Destino:** permanece en `16-bandeja-fallos-notificados.md`

---

### 6.2 Pasar automáticamente a gestión externa / apremios por vencimiento del plazo

- **Acción:** pasar el caso a gestión externa / apremios al cumplirse el plazo configurado
- **Evento o proceso que dispara:** proceso automático de control de vencimiento del plazo contado desde la fecha de acuse positivo de la notificación del fallo
- **Resultado esperado:** el caso deja la espera posterior a la notificación del fallo y pasa al tramo de gestión externa
- **Destino:** `18-bandeja-gestion-externa.md`

---

### 6.3 Pasar manualmente a gestión externa / apremios

- **Acción:** enviar el caso manualmente a gestión externa / apremios antes o al margen del proceso automático
- **Evento o proceso que dispara:** decisión administrativa habilitada para adelantar o forzar el pase
- **Resultado esperado:** el caso sale de la espera y entra al tramo de gestión externa
- **Destino:** `18-bandeja-gestion-externa.md`

---

### 6.4 Registrar apelación

- **Acción:** registrar una apelación presentada por el infractor luego de la notificación del fallo
- **Evento o proceso que dispara:** ingreso formal de la apelación o admisión administrativa de la presentación recursiva
- **Resultado esperado:** el caso deja de estar solo en espera posterior a la notificación del fallo y pasa al circuito específico de apelación
- **Destino:** `17-bandeja-con-apelacion.md`

**Regla importante:**

- la apelación es un **recurso eventual**
- solo corresponde cuando efectivamente existe apelación
- no es una bandeja secuencial obligatoria
- si no hay apelación ni otra actuación que modifique el recorrido, el destino natural al vencerse el plazo es **gestión externa / apremios**

---

### 6.5 Registrar pago, presentación u otra novedad admitida

- **Acción:** registrar una actuación del infractor o una novedad que deba ser considerada durante este período
- **Evento o proceso que dispara:** pago, presentación espontánea, pedido o actuación admitida por criterio administrativo
- **Resultado esperado:** el caso puede requerir tratamiento específico fuera de la simple espera
- **Destino:** la bandeja o circuito que corresponda según definición operativa posterior

---

### 6.6 Derivar por decisión administrativa a otra bandeja

- **Acción:** remitir el caso a otra estación operativa por decisión de Dirección de Faltas
- **Evento o proceso que dispara:** definición administrativa sobre el tratamiento concreto del caso
- **Resultado esperado:** el caso deja esta bandeja y sigue otro recorrido
- **Destino:** según la decisión adoptada

---

### 6.7 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada de Dirección de Faltas o configuración del caso que habilite cierre sin necesidad de seguir el recorrido esperado
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** `20-bandeja-cerradas.md`

---

### 6.8 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, conflicto jurídico-administrativo, espera externa o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** `19-bandeja-paralizadas.md`

---

## 7. Excepciones

### 7.1 Error en la fecha base de cómputo

Puede ocurrir que se tome como referencia una fecha distinta de la que corresponde.

Regla que debe quedar firme:

- el cómputo del plazo debe hacerse desde la **fecha de acuse positivo** de la notificación del fallo
- no desde la fecha del fallo
- no desde la fecha de emisión de la notificación
- no desde la fecha de envío
- no desde la mera generación del documento

---

### 7.2 Plazo fijo en el proceso en vez de parametrizado

Aunque pueda existir una práctica habitual, ese plazo no debería quedar rígido en esta definición operativa.

Debe tratarse como:

- plazo parametrizable
- dependiente de normativa o configuración del sistema
- eventualmente variable según tipo de acta o régimen aplicable

---

### 7.3 Casos con actuaciones durante el plazo de espera

Puede ocurrir que, durante la permanencia en esta bandeja, entren:

- apelaciones
- pagos
- presentaciones espontáneas
- decisiones administrativas
- incidencias externas relevantes

En esos casos:

- el expediente no necesariamente sigue la espera lineal hasta gestión externa / apremios
- puede ser derivado a otra bandeja o circuito
- pero eso debe entenderse como una posibilidad operativa, no como paso secuencial obligatorio

---

### 7.4 Pase manual a gestión externa / apremios antes del vencimiento automático

Debe validarse bajo qué criterio puede adelantarse manualmente el pase a gestión externa / apremios.

Ese pase manual existe como posibilidad, pero conviene definir:

- quién puede hacerlo
- en qué supuestos
- si requiere fundamento u observación

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Qué consideran exactamente notificación fehaciente del fallo a efectos de ingresar aquí.

3. Si la fecha base del cómputo debe ser siempre la fecha de acuse positivo de la notificación del fallo.

4. Si el plazo posterior al fallo notificado debe configurarse por normativa, tipo de acta o régimen.

5. Si el destino natural, a falta de apelación u otra actuación, es efectivamente gestión externa / apremios.

6. Qué acciones reales pueden ocurrir mientras el caso está en esta bandeja:
   - apelación
   - pago
   - presentación
   - cierre
   - paralización
   - derivación administrativa
   - otras

7. Si el pase a gestión externa / apremios debe ser:
   - automático al vencerse el plazo
   - manual
   - o ambas cosas

8. Si el pase manual anticipado requiere causal expresa o simplemente facultad operativa.

9. Si la bandeja de apelación debe existir como estación operativa propia solo para los casos que realmente apelan.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “fallos notificados” existe como bandeja real
- qué casos ingresan exactamente
- qué se considera notificación fehaciente del fallo
- desde qué fecha se computa el plazo
- cómo se parametriza ese plazo
- cuándo el pase a gestión externa / apremios es automático
- cuándo puede ser manual
- qué actuaciones pueden ocurrir mientras el caso permanece aquí
- y cuáles de esas actuaciones generan desvío hacia otras bandejas o circuitos
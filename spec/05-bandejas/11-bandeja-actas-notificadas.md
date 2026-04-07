# [BANDEJA 11] ACTAS NOTIFICADAS

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos cuya **notificación del acto ya fue fehaciente** y que, a partir de ese momento, deben permanecer en **espera controlada** durante el plazo aplicable antes de pasar, en principio, a la bandeja de **pendiente de fallo**.

Esta bandeja representa el tramo posterior a:

- `10-bandeja-notificacion-acto-en-proceso.md`

y previo principalmente a:

- `12-bandeja-pendiente-de-fallo.md`

Sin perjuicio de que, durante la permanencia en esta bandeja, puedan ocurrir otras acciones administrativas no lineales.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/10-bandeja-notificacion-acto-en-proceso.md`
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `CanalNotificacion`
  - `EstadoNotificacion`
  - `TipoEventoActa`
  - `EstadoActa`

---

## 1. Nombre de la bandeja

**Bandeja — Actas notificadas**

Nombre alternativo conversable con el área:

**Actas con notificación fehaciente**

---

## 2. Objetivo

Permitir el seguimiento de las actas cuyo acto administrativo ya fue **notificado fehacientemente**, manteniéndolas en una bandeja de espera operativa durante el plazo que corresponda según la normativa o parametrización del sistema.

La finalidad de esta bandeja es:

- consolidar que la notificación ya fue fehaciente
- tomar como referencia la **fecha de acuse positivo**
- controlar el plazo posterior a la notificación
- permitir acciones administrativas no lineales mientras transcurre ese plazo
- derivar a **pendiente de fallo** cuando se cumpla el plazo configurado
- permitir, si corresponde, el pase manual anticipado a pendiente de fallo

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen acto administrativo emitido
- ya tienen notificación fehaciente o acuse positivo
- se encuentran dentro del plazo de espera posterior a la notificación
- todavía no pasaron a pendiente de fallo

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- tipo de acto administrativo notificado
- canal de notificación
- fecha de acuse positivo
- fecha desde la cual corre el plazo
- cantidad de días transcurridos desde el acuse positivo
- cantidad de días restantes según la parametrización vigente
- fecha prevista de pase a pendiente de fallo
- observaciones relevantes
- si tiene presentaciones, pagos o incidencias abiertas
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos en que la notificación del acto quedó acreditada de manera fehaciente.

Casos típicos de ingreso:

- recepción de acuse positivo
- constancia válida de entrega
- confirmación válida del canal que, conforme a la normativa y criterio administrativo, equivale a notificación fehaciente
- validación manual de que la notificación quedó perfeccionada

Regla importante:

- el ingreso a esta bandeja se produce cuando la notificación deja de estar “en proceso” y pasa a estar **cumplida fehacientemente**
- la fecha base para el cómputo del plazo debe ser la **fecha de acuse positivo**
- el plazo no debe quedar fijo en el documento; debe venir de configuración del sistema según normativa aplicable

No deberían entrar aquí:

- casos cuya notificación del acto todavía está en curso
- casos sin constancia suficiente de notificación fehaciente
- casos que ya pasaron a pendiente de fallo
- casos ya cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver operativamente:

- que el caso permanezca en espera durante el plazo correspondiente
- que se compute correctamente el tiempo desde la fecha de acuse positivo
- que, cumplido ese plazo, el caso pase a pendiente de fallo
- que durante esa espera puedan registrarse acciones o decisiones administrativas que alteren el recorrido esperado

Es importante dejar explícito que esta bandeja:

- **no implica** un paso lineal y secuencial obligatorio hacia gestión externa o cierre
- funciona como una estación de espera posterior a la notificación fehaciente
- admite acciones posibles, pero no presupone que deban ocurrir necesariamente

---

## 6. Acciones posibles

### 6.1 Mantener en espera hasta cumplimiento del plazo

- **Acción:** mantener el caso en esta bandeja hasta que se cumpla el plazo configurado
- **Evento o proceso que dispara:** permanencia natural del caso desde la fecha de acuse positivo
- **Resultado esperado:** el expediente sigue en espera operativa controlada
- **Destino:** permanece en `11-bandeja-actas-notificadas.md`

---

### 6.2 Pasar automáticamente a pendiente de fallo por vencimiento del plazo

- **Acción:** pasar el caso a pendiente de fallo al cumplirse el plazo configurado
- **Evento o proceso que dispara:** proceso automático de control de vencimiento del plazo contado desde la fecha de acuse positivo
- **Resultado esperado:** el caso deja la espera posterior a la notificación y pasa al tramo de resolución administrativa
- **Destino:** `12-bandeja-pendiente-de-fallo.md`

---

### 6.3 Pasar manualmente a pendiente de fallo

- **Acción:** enviar el caso manualmente a pendiente de fallo antes o al margen del proceso automático
- **Evento o proceso que dispara:** decisión administrativa habilitada para adelantar o forzar el pase
- **Resultado esperado:** el caso sale de la espera y entra al tramo de pendiente de fallo
- **Destino:** `12-bandeja-pendiente-de-fallo.md`

---

### 6.4 Registrar presentación, descargo, pago voluntario u otra novedad admitida

- **Acción:** registrar una actuación del administrado o una novedad que deba ser tratada durante este período
- **Evento o proceso que dispara:** presentación espontánea, descargo, pago voluntario u otra actuación admitida por criterio administrativo
- **Resultado esperado:** el caso puede requerir tratamiento específico fuera de la simple espera
- **Destino:** la bandeja o circuito que corresponda según definición operativa posterior

---

### 6.5 Derivar por decisión administrativa a otra bandeja

- **Acción:** remitir el caso a otra estación operativa por decisión de Dirección de Faltas
- **Evento o proceso que dispara:** definición administrativa sobre el tratamiento concreto del caso
- **Resultado esperado:** el caso deja esta bandeja y sigue otro recorrido
- **Destino:** según la decisión adoptada

---

### 6.6 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada de Dirección de Faltas o configuración del caso que habilite cierre sin necesidad de seguir el recorrido esperado
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** bandeja de cerradas

---

## 7. Excepciones

### 7.1 Error en la fecha base de cómputo

Puede ocurrir que se tome como referencia una fecha distinta de la que corresponde.

Regla que debe quedar firme:

- el cómputo del plazo debe hacerse desde la **fecha de acuse positivo**
- no desde la fecha de emisión del acto
- no desde la fecha de envío
- no desde la mera generación de la notificación

---

### 7.2 Plazo fijo en el proceso en vez de parametrizado

Aunque hoy se mencione un plazo de 30 días, eso no debería quedar rígido en esta definición operativa.

Debe tratarse como:

- plazo parametrizable
- dependiente de normativa o configuración del sistema
- eventualmente variable según tipo de acta o régimen aplicable

---

### 7.3 Casos con actuaciones durante el plazo de espera

Puede ocurrir que, durante la permanencia en esta bandeja, entren:

- descargos
- pagos voluntarios
- presentaciones espontáneas
- decisiones administrativas de la Directora de Faltas

En esos casos:

- el expediente no necesariamente sigue la espera lineal hasta pendiente de fallo
- puede ser derivado a otra bandeja o circuito
- pero eso debe entenderse como una posibilidad operativa, no como paso secuencial obligatorio

---

### 7.4 Pase manual a pendiente de fallo antes del vencimiento automático

Debe validarse bajo qué criterio puede adelantarse manualmente el pase a pendiente de fallo.

Ese pase manual existe como posibilidad, pero conviene definir:

- quién puede hacerlo
- en qué supuestos
- si requiere fundamento u observación

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Qué consideran exactamente notificación fehaciente a efectos de ingresar aquí.

3. Si la fecha base del cómputo debe ser siempre la fecha de acuse positivo.

4. Si el plazo posterior a la notificación debe configurarse por normativa, tipo de acta o régimen.

5. Si hoy ese plazo operativo se piensa como 30 días, pero con posibilidad de parametrización.

6. Qué acciones reales pueden ocurrir mientras el caso está en esta bandeja:
   - descargo
   - pago voluntario
   - cierre
   - derivación administrativa
   - otras

7. Si el pase a pendiente de fallo debe ser:
   - automático al vencerse el plazo
   - manual
   - o ambas cosas

8. Si el pase manual anticipado requiere causal expresa o simplemente facultad operativa.

9. Si la apelación debe tener una bandeja propia o si solo aparece como acción/evento sobre ciertos casos.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “actas notificadas” existe como bandeja real
- qué casos ingresan exactamente
- qué se considera notificación fehaciente
- desde qué fecha se computa el plazo
- cómo se parametriza ese plazo
- cuándo el pase a pendiente de fallo es automático
- cuándo puede ser manual
- qué actuaciones pueden ocurrir mientras el caso permanece aquí
- y cuáles de esas actuaciones generan desvío hacia otras bandejas o circuitos
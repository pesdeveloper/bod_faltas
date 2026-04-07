# [BANDEJA 10] NOTIFICACIÓN DEL ACTO EN PROCESO

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos cuyo **acto administrativo ya fue emitido** y cuya **notificación se encuentra en curso**, hasta que exista un resultado suficiente sobre esa notificación.

Esta bandeja representa el tramo posterior a:

- `09-bandeja-pendiente-notificacion-acto.md`

y previo principalmente a:

- una bandeja de **actas notificadas**, cuando exista **notificación fehaciente / acuse positivo**
- otras salidas excepcionales o administrativas, si corresponden antes de completar la notificación

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/09-bandeja-pendiente-notificacion-acto.md`
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `CanalNotificacion`
  - `EstadoNotificacion`
  - `TipoEventoActa`
  - `EstadoActa`

---

## 1. Nombre de la bandeja

**Bandeja — Notificación del acto en proceso**

Nombre alternativo conversable con el área:

**Actos en proceso de notificación**

---

## 2. Objetivo

Permitir operar y controlar los casos en los que la **notificación del acto administrativo ya fue emitida** pero todavía no existe constancia suficiente de notificación fehaciente ni resultado final del diligenciamiento.

La finalidad de esta bandeja es:

- controlar notificaciones en curso
- registrar resultados o novedades del diligenciamiento
- detectar falta de acuse, rechazo, devolución o imposibilidad de notificar
- decidir reintentos, correcciones o desvíos
- sacar de esta bandeja los casos que ya tengan notificación fehaciente

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen acto administrativo emitido
- ya tuvieron iniciada la notificación del acto
- tienen una notificación activa, pendiente de resultado o con incidencia abierta
- todavía no cuentan con notificación fehaciente consolidada

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- tipo de acto administrativo notificado
- fecha de emisión de la notificación
- canal de notificación
- estado actual de la notificación
- fecha del último movimiento
- observación o incidencia relevante
- si hubo o no acuse
- si hubo rechazo, devolución o imposibilidad
- si está dentro o fuera de plazo operativo esperado
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos en que ocurrió la emisión de la notificación del acto y, desde ese momento, el trámite de notificación quedó operativo y abierto.

Casos típicos de ingreso:

- acto administrativo notificado por canal electrónico y pendiente de resultado operativo suficiente
- acto administrativo enviado por canal postal / diligenciamiento externo y pendiente de recepción de acuse o resultado
- acto administrativo con intento de notificación realizado, pero con novedad aún no resuelta
- acto administrativo cuya notificación requiere seguimiento, reintento o validación manual

No deberían entrar aquí:

- casos cuyo acto todavía no está listo para notificar
- casos donde la notificación del acto aún no fue emitida
- casos con notificación fehaciente ya consolidada
- casos ya cerrados definitivamente
- casos ya enviados a otra estación operativa posterior

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver operativamente si la notificación del acto:

- fue efectivamente cumplida
- quedó pendiente de acuse o confirmación
- requiere reintento
- presenta una incidencia que obliga a corregir datos, canal o estrategia
- no pudo completarse y exige decisión posterior

Lo central aquí es determinar si el caso:

- sigue en proceso de notificación
- o ya debe salir a una bandeja de **actas notificadas** por contar con **acuse positivo / notificación fehaciente**

---

## 6. Acciones posibles

### 6.1 Registrar acuse o resultado positivo de notificación

- **Acción:** registrar que la notificación del acto fue recibida o cumplida válidamente
- **Evento o proceso que dispara:** recepción de acuse, constancia de entrega, confirmación válida del canal o validación manual equivalente
- **Resultado esperado:** la notificación queda acreditada como **fehaciente**
- **Destino:** bandeja de **actas notificadas**

**Regla importante:**

- el acuse positivo **no implica** no implica paso lineal a otra bandeja obligatoria
- primero debe quedar en una bandeja específica de **actas notificadas**
- desde esa bandeja se computa el plazo posterior a la notificación
- el cálculo de días debe hacerse desde la **fecha de acuse positivo**
- cumplido el plazo configurado por sistema para la normativa aplicable, el caso pasa a **pendiente de fallo**
- ese pase también puede habilitarse manualmente por decisión administrativa

---

### 6.2 Registrar novedad de rechazo, devolución o imposibilidad de notificar

- **Acción:** registrar que la notificación no pudo concretarse o presentó una incidencia relevante
- **Evento o proceso que dispara:** devolución postal, rechazo del canal, domicilio inválido, imposibilidad material, constancia negativa o incidencia equivalente
- **Resultado esperado:** el caso queda con incidencia explícita y pendiente de decisión operativa
- **Destino:** permanece en esta bandeja hasta que se defina el siguiente paso

---

### 6.3 Reintentar notificación

- **Acción:** ordenar o iniciar un nuevo intento de notificación
- **Evento o proceso que dispara:** decisión operativa ante falta de acuse, rechazo, devolución o intento fallido
- **Resultado esperado:** se genera un nuevo ciclo de diligenciamiento o envío por el mismo canal o por otro admitido
- **Destino:** permanece en `10-bandeja-notificacion-acto-en-proceso.md`

---

### 6.4 Corregir datos operativos para nueva notificación

- **Acción:** corregir datos necesarios para posibilitar un nuevo diligenciamiento
- **Evento o proceso que dispara:** detección de error en domicilio, canal, receptor, datos del acto o información mínima de notificación
- **Resultado esperado:** el caso queda nuevamente apto para reemitir o reintentar la notificación
- **Destino:** normalmente permanece en esta bandeja hasta que el nuevo intento quede lanzado o, si la corrección es sustancial, podría requerir reingreso a una bandeja previa

---

### 6.5 Escalar incidencia para decisión administrativa

- **Acción:** remitir el caso para definición interna cuando la notificación no puede resolverse con una acción operativa simple
- **Evento o proceso que dispara:** reiteración de intentos fallidos, contradicción en constancias, conflicto de datos, situación atípica o criterio jurídico/administrativo pendiente
- **Resultado esperado:** el caso deja de estar en simple seguimiento de notificación y requiere intervención de decisión
- **Destino:** según la incidencia:
  - análisis interno
  - pendiente de acto administrativo
  - paralizadas

---

### 6.6 Marcar espera por plazo o ventana de respuesta del diligenciamiento

- **Acción:** dejar el caso en seguimiento hasta el vencimiento del plazo operativo del intento de notificación en curso
- **Evento o proceso que dispara:** notificación ya emitida, sin acuse negativo ni positivo definitivo, dentro del plazo operativo admitido
- **Resultado esperado:** el expediente sigue monitoreado sin salir de la bandeja
- **Destino:** permanece en `10-bandeja-notificacion-acto-en-proceso.md`

---

## 7. Excepciones

### 7.1 Notificación marcada como emitida pero sin soporte suficiente

Puede ocurrir que el sistema indique que la notificación fue iniciada, pero falte constancia mínima, dato de canal o respaldo documental suficiente.

En ese caso:

- no debería considerarse una notificación operativamente sólida
- puede requerirse corrección o revisión interna
- podría corresponder retorno a una etapa previa según la gravedad del problema

---

### 7.2 Incidencias repetidas sin criterio uniforme de salida

Puede ocurrir que existan varios intentos fallidos o constancias contradictorias y no esté claro cuándo insistir, cuándo cambiar canal y cuándo escalar.

Esto debe validarse con Dirección de Faltas para evitar discrecionalidad operativa excesiva.

---

### 7.3 Presentación espontánea o novedad relevante durante la notificación

Puede ocurrir que, mientras la notificación del acto está en curso, aparezca una presentación espontánea, un pago o una novedad externa relevante.

En ese caso, el caso podría requerir:

- reingreso a análisis
- revisión de efectos del acto
- o cambio de destino operativo

---

### 7.4 Error en el acto o en la documentación notificada detectado después del envío

Si luego de emitida la notificación se detecta un error sustancial en el acto, el caso no debería seguir tratándose solo como una notificación en curso.

Debe evaluarse si corresponde:

- volver a análisis
- generar un nuevo acto
- dejar sin efecto la actuación previa
- o paralizar

---

## 8. Puntos a validar con Dirección de Faltas

1. Qué eventos concretos consideran suficientes para dar por notificado fehacientemente un acto, según canal.

2. Qué diferencia práctica hacen entre:
   - notificación en curso
   - falta de acuse
   - rechazo
   - devolución
   - imposibilidad de notificar
   - notificación fehaciente

3. Cuándo corresponde reintentar y cuántas veces, antes de escalar o paralizar.

4. Si toda notificación fehaciente debe pasar primero por una bandeja específica de **actas notificadas**.

5. Desde qué fecha se computa el plazo posterior a la notificación:
   - fecha de emisión
   - fecha de envío
   - o fecha de acuse positivo

6. Si el plazo de espera posterior a la notificación debe parametrizarse por normativa o tipo de acta.

7. Si el pase a **pendiente de fallo** puede ser:
   - automático al vencer el plazo
   - manual
   - o ambas cosas

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- qué casos integran realmente el tramo de notificación en proceso
- qué estados justifican permanencia aquí
- qué se considera notificación fehaciente
- si el acuse positivo saca siempre al caso de esta bandeja
- cómo nace la bandeja de **actas notificadas**
- desde qué fecha se computa el plazo posterior
- y bajo qué regla el caso pasa luego a **pendiente de fallo**

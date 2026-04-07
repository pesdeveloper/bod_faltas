# [BANDEJA 12] PENDIENTE DE FALLO

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos que, luego de la notificación fehaciente del acto y del transcurso del plazo correspondiente, o por decisión administrativa expresa, quedan en condiciones de pasar al tramo de resolución.

Esta bandeja representa el tramo posterior principalmente a:

- `11-bandeja-actas-notificadas.md`

y previo a:

- la generación del fallo, resolución u otro acto administrativo que corresponda

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/08-bandeja-acto-administrativo-en-proceso.md`
- `spec/05-bandejas/10-bandeja-notificacion-acto-en-proceso.md`
- `spec/05-bandejas/11-bandeja-actas-notificadas.md`
- documentos canónicos de snapshot
- documentos canónicos de acto administrativo
- catálogos de:
  - `EstadoActa`
  - `TipoEventoActa`
  - `TipoDocumento`
  - `EstadoDocumento`
  - `MotivoCierreActa`

---

## 1. Nombre de la bandeja

**Bandeja — Pendiente de fallo**

Nombre alternativo conversable con el área:

**Actas listas para resolver**

---

## 2. Objetivo

Permitir identificar y operar los casos que ya cumplieron las condiciones para ingresar al tramo de resolución administrativa.

La finalidad de esta bandeja es:

- concentrar los casos que ya pueden ser tratados para dictar fallo, resolución u otro acto final o siguiente
- separar claramente la espera posterior a la notificación fehaciente del momento en que el caso ya queda listo para resolver
- dar visibilidad a los expedientes que ya deben ser trabajados por el área para avanzar con la decisión administrativa correspondiente
- admitir también el ingreso manual por decisión administrativa, aun cuando el pase no haya ocurrido por vencimiento automático del plazo

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen notificación fehaciente previa
- ya cumplieron el plazo posterior configurado desde la fecha de acuse positivo
  **o**
- fueron enviadas manualmente a esta bandeja por decisión administrativa

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha de acuse positivo
- fecha de vencimiento del plazo posterior a la notificación
- indicador de si ingresó por vencimiento automático o por pase manual
- tipo de trámite o situación actual
- observaciones relevantes
- presentaciones o incidencias existentes
- prioridad o antigüedad en la bandeja
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos que ya están en condiciones de ser tratados para resolución.

Casos típicos de ingreso:

- actas notificadas fehacientemente cuyo plazo posterior ya venció
- actas notificadas que, por decisión administrativa, son enviadas manualmente a esta bandeja antes o al margen del automatismo
- casos que, por criterio del área, deben empezar a resolverse aunque el recorrido no haya sido estrictamente lineal

Regla importante:

- el ingreso natural a esta bandeja ocurre por vencimiento del plazo computado desde la **fecha de acuse positivo**
- ese plazo debe venir de configuración del sistema
- además debe existir la posibilidad de **pase manual**

No deberían entrar aquí:

- casos cuya notificación todavía está en curso
- casos notificadamente fehacientes que aún están dentro del plazo de espera y no fueron enviados manualmente
- casos ya en trabajo activo de generación de acto
- casos cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- qué casos ya deben ser resueltos mediante la generación del fallo
- si antes de generar el fallo existe alguna presentación, pago voluntario, descargo o incidencia que deba ser tratada
- si corresponde generar el fallo y la documentación necesaria para su notificación
- si el caso requiere volver a análisis previo antes de resolver
- si el caso puede desviarse por decisión administrativa a otro circuito

Esta bandeja es la estación donde el caso queda claramente identificado como:

**listo para generar fallo**, salvo que aparezca una actuación o decisión administrativa que modifique ese recorrido esperado

---

## 6. Acciones posibles

### 6.1 Generar fallo y documentación necesaria

- **Acción:** generar el fallo y la documentación asociada necesaria para su notificación
- **Evento o proceso que dispara:** decisión administrativa de resolver el caso
- **Resultado esperado:** se redacta el fallo, se confirman los documentos necesarios y el caso pasa al circuito de firma correspondiente
- **Destino:** bandeja de **fallo pendiente de notificación de fallo** o instancia equivalente, una vez generados y enviados a firma los documentos requeridos

**Regla importante:**

- desde esta bandeja no existe una acción intermedia de “tomar para iniciar resolución”
- la acción relevante es directamente **generar fallo**
- al generar el fallo, también deben generarse los documentos relacionados con su notificación
- el circuito posterior es análogo al ya definido para la notificación del acta:
  - redacción
  - confirmación
  - firma de documentos requeridos
  - pase a la instancia previa a la notificación del fallo

---

### 6.2 Registrar pago voluntario

- **Acción:** registrar que el caso queda alcanzado por una salida de pago voluntario admitida en esta etapa
- **Evento o proceso que dispara:** presentación del infractor, pago voluntario o decisión administrativa que habilite este tratamiento
- **Resultado esperado:** el caso deja la espera de fallo ordinaria y sigue el circuito que corresponda según la resolución administrativa aplicable
- **Destino:** según definición operativa del circuito de pago voluntario o análisis asociado

---

### 6.3 Registrar descargo o presentación

- **Acción:** registrar un descargo, presentación espontánea o actuación del infractor que deba ser considerada antes de generar el fallo
- **Evento o proceso que dispara:** ingreso formal de la presentación o incorporación administrativa de la actuación
- **Resultado esperado:** el caso deja de estar listo para fallo inmediato y pasa al tratamiento correspondiente
- **Destino:** bandeja o circuito de análisis / presentaciones que corresponda

---

### 6.4 Mantener en pendiente de fallo

- **Acción:** mantener el caso en esta bandeja sin generar todavía el fallo
- **Evento o proceso que dispara:** decisión operativa por prioridad, carga de trabajo, criterio administrativo o existencia de evaluaciones pendientes
- **Resultado esperado:** el caso continúa visible como pendiente de resolución
- **Destino:** permanece en `12-bandeja-pendiente-de-fallo.md`

---

### 6.5 Reenviar a análisis o revisión interna

- **Acción:** devolver el caso a una instancia previa de análisis
- **Evento o proceso que dispara:** detección de incidencia, presentación pendiente, inconsistencia, hecho nuevo o necesidad de revisión adicional antes de resolver
- **Resultado esperado:** el caso deja la condición de “pendiente de fallo” hasta completar la revisión necesaria
- **Destino:** bandeja o circuito de análisis que corresponda

---

### 6.6 Registrar decisión administrativa especial

- **Acción:** aplicar una decisión que altere el recorrido esperado del caso
- **Evento o proceso que dispara:** instrucción de Dirección de Faltas u otra autoridad competente
- **Resultado esperado:** el caso puede cambiar de destino sin seguir el recorrido ordinario de generación de fallo
- **Destino:** según la decisión adoptada

---

### 6.7 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada que determine que no corresponde continuar con la generación del fallo en el circuito ordinario
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** bandeja de cerradas

---

### 6.8 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, espera externa, conflicto jurídico-administrativo o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** bandeja de paralizadas

---

## 7. Excepciones

### 7.1 Casos que llegan aquí pero todavía no están realmente maduros para resolver

Puede ocurrir que el caso haya ingresado por vencimiento automático del plazo, pero que en la práctica todavía exista alguna presentación, incidencia o cuestión pendiente.

En ese caso:

- no debería forzarse la resolución inmediata
- puede requerirse reenvío a análisis o tratamiento previo
- debe evitarse que el automatismo oculte la realidad operativa

---

### 7.2 Pase manual sin criterio uniforme

La posibilidad de enviar manualmente casos a esta bandeja es útil, pero puede generar disparidad si no se define bien:

- quién puede hacerlo
- en qué supuestos
- si requiere observación o fundamento

---

### 7.3 Casos que no terminan en fallo en sentido estricto

Aunque la bandeja se llame “pendiente de fallo”, debe validarse si en todos los casos el resultado posterior será estrictamente un fallo, o si puede tratarse también de:

- resolución
- disposición
- acto administrativo equivalente
- cierre por otra vía

---

### 7.4 Diferencia entre “pendiente de fallo” y “acto administrativo en proceso”

Debe quedar clara la separación entre:

- caso listo para generar fallo
- caso con fallo y documentación ya en preparación / firma / notificación

Si esta diferencia no existe en la práctica, podría requerirse ajuste posterior.

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Si el nombre “pendiente de fallo” refleja bien la realidad o si conviene un nombre más amplio.

3. Si el ingreso natural se produce por vencimiento del plazo contado desde la fecha de acuse positivo.

4. Si además debe existir pase manual y bajo qué condiciones.

5. Si todos los casos de esta bandeja van a fallo en sentido estricto o si algunos derivan en otro tipo de acto o cierre.

6. Si esta bandeja se diferencia realmente de la instancia de acto administrativo en proceso.

7. Qué criterios usan para priorizar casos dentro de esta bandeja.

8. Qué incidencias justifican devolver un caso a análisis antes de resolver.

9. Si desde aquí puede cerrarse o paralizarse directamente un expediente.

10. Si “generar fallo” es la acción principal esperada en esta bandeja o si conviene desdoblarla según tipo de acto.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “pendiente de fallo” existe como bandeja real
- cuáles son exactamente sus criterios de ingreso
- cómo se computa el vencimiento que dispara el ingreso automático
- cuándo y cómo puede existir pase manual
- si el nombre de la bandeja es correcto o debe ampliarse
- qué diferencia operativa real existe con “acto administrativo en proceso”
- si la acción principal aquí es efectivamente generar fallo
- qué destinos alternativos pueden darse desde esta bandeja
- y qué casos efectivamente pasan desde aquí al tramo de resolución activa
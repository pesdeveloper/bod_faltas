# [BANDEJA 17] CON APELACIÓN

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan **únicamente** los casos en los que, luego de la notificación del fallo, el infractor **presentó efectivamente una apelación** y esa actuación debe ser tratada por el sistema y por el área.

Esta bandeja **no forma parte de un tramo lineal obligatorio** del flujo.

Solo existe para los casos en los que realmente hay apelación.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/16-bandeja-fallos-notificados.md`
- `spec/05-bandejas/18-bandeja-gestion-externa.md`
- documentos canónicos de acto administrativo
- documentos canónicos de notificación
- documentos canónicos de snapshot
- catálogos de:
  - `TipoEventoActa`
  - `EstadoActa`
  - `MotivoCierreActa`

---

## 1. Nombre de la bandeja

**Bandeja — Con apelación**

Nombre alternativo conversable con el área:

**Apelaciones en proceso**

---

## 2. Objetivo

Permitir identificar y tratar los casos en los que existe una apelación presentada por el infractor luego de la notificación del fallo.

La finalidad de esta bandeja es:

- separar claramente los casos con apelación de los casos sin apelación
- concentrar las actuaciones recursivas efectivamente existentes
- permitir su análisis, tratamiento, derivación o resolución
- evitar modelar la apelación como si fuera un paso secuencial normal del flujo

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya tienen fallo
- ya tienen notificación fehaciente del fallo
- tienen una apelación efectivamente presentada
- requieren tratamiento administrativo, jurídico o procesal de esa apelación

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha del fallo
- fecha de notificación fehaciente del fallo
- fecha de presentación de la apelación
- estado actual de la apelación
- observaciones relevantes
- documentación asociada a la apelación
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja **solo** los casos en los que existe una apelación efectivamente presentada.

Casos típicos de ingreso:

- ingreso formal de apelación
- presentación recursiva que el área admite y encuadra como apelación
- actuación equivalente que deba tramitarse como apelación según criterio administrativo y normativo

Regla importante:

- a esta bandeja **solo se llega si hay apelación**
- no se llega aquí por el mero vencimiento de plazos
- no se llega aquí como siguiente paso automático después de “fallos notificados”
- no se llega aquí por defecto
- si no hay apelación, el caso **no** debe pasar por esta bandeja

No deberían entrar aquí:

- casos sin apelación presentada
- casos cuyo fallo fue notificado pero no tuvieron recurso
- casos cuyo destino natural sigue siendo gestión externa / apremios
- casos cerrados definitivamente

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- si la apelación debe ser tratada, elevada, analizada o encauzada
- qué efecto produce la apelación sobre el recorrido del expediente
- si el caso debe suspender su curso normal mientras la apelación se trata
- qué resultado o salida corresponde luego del tratamiento de la apelación

Esta bandeja existe para gestionar un **recurso eventual**, no un paso ordinario del flujo.

---

## 6. Acciones posibles

### 6.1 Registrar ingreso de apelación

- **Acción:** registrar formalmente que el caso queda alcanzado por una apelación
- **Evento o proceso que dispara:** presentación del recurso o admisión administrativa de la actuación recursiva
- **Resultado esperado:** el caso queda identificado dentro del circuito de apelación
- **Destino:** permanece en `17-bandeja-con-apelacion.md`

---

### 6.2 Analizar apelación

- **Acción:** revisar la presentación, sus fundamentos y su encuadre
- **Evento o proceso que dispara:** toma del caso por el área competente
- **Resultado esperado:** la apelación queda en tratamiento y se define el paso siguiente
- **Destino:** permanece en `17-bandeja-con-apelacion.md`

---

### 6.3 Derivar o elevar apelación

- **Acción:** remitir la apelación a la instancia que corresponda para su tratamiento
- **Evento o proceso que dispara:** decisión administrativa o procedimental de continuar el trámite recursivo
- **Resultado esperado:** el caso sigue el circuito externo o interno que corresponda
- **Destino:** según la definición operativa y normativa aplicable

---

### 6.4 Registrar resultado de apelación

- **Acción:** registrar el resultado producido por el tratamiento de la apelación
- **Evento o proceso que dispara:** recepción de decisión, resolución o resultado del trámite recursivo
- **Resultado esperado:** el caso deja de estar solo en trámite recursivo y pasa al circuito que corresponda
- **Destino:** según resultado:
  - cierre
  - nueva resolución
  - gestión externa
  - otro circuito que corresponda

---

### 6.5 Mantener en trámite de apelación

- **Acción:** dejar el caso en esta bandeja mientras la apelación sigue pendiente de tratamiento o resultado
- **Evento o proceso que dispara:** falta de resolución definitiva del recurso
- **Resultado esperado:** el caso continúa en el circuito de apelación
- **Destino:** permanece en `17-bandeja-con-apelacion.md`

---

### 6.6 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, espera externa, conflicto jurídico-administrativo o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** bandeja de paralizadas

---

### 6.7 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada o resultado del recurso que habilite cierre
- **Resultado esperado:** el caso se considera finalizado para operación ordinaria
- **Destino:** bandeja de cerradas

---

## 7. Excepciones

### 7.1 Presentación que no debe tratarse como apelación

Puede ocurrir que ingrese una presentación posterior al fallo, pero que no corresponda encuadrarla como apelación.

En ese caso:

- no debería ingresar o permanecer en esta bandeja
- debe derivarse al circuito que corresponda

---

### 7.2 Apelación presentada fuera del supuesto operativo esperado

Puede ocurrir que exista una apelación con problemas de oportunidad, admisibilidad o encuadre.

En ese caso:

- debe validarse cómo la trata operativamente el área
- pero aun así esta bandeja podría ser la estación donde ese caso se visibiliza y resuelve

---

### 7.3 Casos con apelación y otras actuaciones concurrentes

Puede ocurrir que junto con la apelación existan:

- pagos
- presentaciones adicionales
- incidencias externas
- decisiones administrativas complementarias

En ese caso:

- el expediente puede requerir desvíos o subtratamientos adicionales
- sin perder que la apelación es el hecho que justifica la existencia de esta bandeja

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Si el nombre “con apelación” refleja bien la práctica real o si conviene “apelaciones en proceso”.

3. Si el criterio de ingreso es exclusivamente la existencia efectiva de una apelación.

4. Qué presentaciones consideran verdaderamente apelación y cuáles no.

5. Qué acciones reales hacen sobre estos casos:
   - análisis
   - elevación
   - tratamiento interno
   - suspensión
   - cierre
   - otras

6. Qué efectos produce la apelación sobre el curso normal hacia gestión externa / apremios.

7. Qué posibles resultados tiene una apelación en la práctica.

8. Si desde aquí puede originarse una nueva resolución o un nuevo circuito documental.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si esta estación operativa existe como bandeja real
- que su ingreso depende **solo** de la existencia efectiva de apelación
- que no forma parte del flujo lineal normal
- qué presentaciones ingresan realmente aquí
- qué tratamiento operativo se da a esos casos
- qué resultados pueden producirse
- y cómo reingresa o egresa el expediente del circuito de apelación
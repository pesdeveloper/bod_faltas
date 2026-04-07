# [BANDEJA 19] PARALIZADAS

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos que, por una razón fundada, **no pueden continuar transitoriamente su curso normal**, pero **tampoco deben considerarse cerrados**.

Esta bandeja no representa un paso lineal del flujo.

Es una estación excepcional donde quedan expedientes cuyo trámite se encuentra detenido, suspendido o en espera, hasta que exista una condición que permita reactivarlos o una decisión que determine otro destino.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- el resto de las bandejas operativas que pueden derivar casos a paralización
- documentos canónicos de snapshot
- catálogos de:
  - `EstadoActa`
  - `TipoEventoActa`
  - `MotivoCierreActa`

---

## 1. Nombre de la bandeja

**Bandeja — Paralizadas**

Nombre alternativo conversable con el área:

**Trámites paralizados**

---

## 2. Objetivo

Permitir identificar y controlar los expedientes que quedaron fuera del curso operativo normal por una causa que impide seguir avanzando en ese momento.

La finalidad de esta bandeja es:

- separar claramente los casos detenidos de los casos activos
- registrar el motivo de la paralización
- evitar que el expediente siga avanzando por error
- conservar trazabilidad de por qué quedó detenido
- permitir la futura reactivación del caso cuando desaparezca la causa de paralización
- distinguir una paralización de un cierre definitivo

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- tienen una decisión o situación fundada que impide continuar el trámite
- quedaron detenidas en cualquier tramo del flujo
- siguen abiertas desde el punto de vista jurídico-administrativo, pero sin avance operativo actual

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- etapa o bandeja desde la que fue paralizada
- fecha de paralización
- motivo de paralización
- observaciones relevantes
- última actuación realizada
- condición esperada para reactivación
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos que no pueden seguir su curso normal por una razón que justifica su detención temporal o indefinida, sin que ello implique todavía cierre.

Casos típicos de ingreso:

- conflicto jurídico o administrativo pendiente
- espera de resultado externo indispensable
- incidencia material que impide continuar
- decisión expresa de Dirección de Faltas de detener el expediente
- necesidad de suspender el trámite hasta nueva definición
- caso que no puede avanzar ni corresponde cerrar todavía

Regla importante:

- la paralización no debe usarse como sustituto de cierre
- tampoco debe usarse como simple “pendiente” genérico
- debe existir una razón concreta, registrable y trazable

No deberían entrar aquí:

- casos cerrados definitivamente
- casos activos que solo están esperando turno operativo normal
- casos que todavía pueden seguir avanzando aunque tengan alguna observación menor

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- cuál es el motivo real de la paralización
- si el caso debe permanecer detenido o puede reactivarse
- si la causa de paralización desapareció
- si corresponde devolver el expediente al circuito activo
- si la situación terminó derivando en cierre definitivo

Esta bandeja existe para administrar la **detención fundada** del trámite, no para absorber indefinidamente casos sin criterio.

---

## 6. Acciones posibles

### 6.1 Registrar paralización del caso

- **Acción:** registrar formalmente que el expediente queda paralizado
- **Evento o proceso que dispara:** decisión administrativa o constatación de una situación que impide continuar
- **Resultado esperado:** el caso deja de circular por el flujo normal y queda en esta bandeja con motivo explícito
- **Destino:** permanece en `19-bandeja-paralizadas.md`

---

### 6.2 Mantener paralización

- **Acción:** mantener el caso detenido mientras subsista la causa que impide continuar
- **Evento o proceso que dispara:** revisión del caso sin desaparición de la causa de paralización
- **Resultado esperado:** el expediente sigue en esta bandeja
- **Destino:** permanece en `19-bandeja-paralizadas.md`

---

### 6.3 Actualizar motivo, condición o novedad de paralización

- **Acción:** registrar nueva información vinculada al motivo por el cual el expediente sigue paralizado
- **Evento o proceso que dispara:** incorporación de observaciones, informes, actuaciones o novedades
- **Resultado esperado:** la paralización queda mejor documentada y trazable
- **Destino:** permanece en `19-bandeja-paralizadas.md`

---

### 6.4 Reactivar expediente

- **Acción:** reactivar el caso cuando desaparezca o se resuelva la causa que justificó la paralización
- **Evento o proceso que dispara:** decisión administrativa de reanudar el trámite o constatación de que ya puede continuar
- **Resultado esperado:** el expediente sale de esta bandeja y vuelve al circuito operativo correspondiente
- **Destino:** la bandeja o circuito que corresponda según el punto exacto de reingreso

**Regla importante:**

- la reactivación no necesariamente vuelve siempre al inicio
- debe volver al punto del flujo que resulte correcto según la situación concreta del expediente

---

### 6.5 Derivar a otra bandeja por decisión administrativa

- **Acción:** enviar el caso desde paralizadas a otra estación operativa distinta de la original
- **Evento o proceso que dispara:** definición administrativa sobre el tratamiento posterior del caso
- **Resultado esperado:** el expediente deja la paralización y retoma un circuito específico
- **Destino:** según la decisión adoptada

---

### 6.6 Cerrar el caso desde paralización

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** decisión fundada de que el caso ya no debe reactivarse ni continuar
- **Resultado esperado:** el expediente deja de estar paralizado y pasa a condición de cierre definitivo
- **Destino:** `20-bandeja-cerradas.md`

---

## 7. Excepciones

### 7.1 Uso de paralización como “cajón de sastre”

Existe riesgo de usar esta bandeja como destino genérico para casos incómodos, demorados o mal definidos.

Eso debe evitarse.

La paralización debe tener:

- motivo concreto
- trazabilidad
- criterio de revisión
- eventual condición de salida

---

### 7.2 Casos que en realidad deberían estar cerrados

Puede ocurrir que un expediente permanezca paralizado cuando en realidad ya corresponde cierre.

En ese caso:

- debe evitarse que la paralización prolongue artificialmente la vida del expediente
- debe definirse si corresponde cierre definitivo

---

### 7.3 Casos que podrían seguir activos pero con seguimiento

Puede ocurrir que un caso tenga una dificultad o espera menor, pero no una verdadera causa de paralización.

En ese caso:

- no debería ingresar a esta bandeja
- debería permanecer en la estación activa que corresponda

---

### 7.4 Reactivación sin criterio claro de retorno

Debe validarse con el área a qué punto del flujo vuelve un expediente reactivado.

No debería asumirse automáticamente que vuelve siempre al mismo lugar.

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Qué motivos reales justifican una paralización.

3. Quién puede decidir la paralización del expediente.

4. Si toda paralización requiere observación o fundamento explícito.

5. Qué diferencia práctica hacen entre:
   - paralizado
   - en espera
   - pendiente
   - cerrado

6. Cómo se decide la reactivación.

7. A qué punto del flujo vuelve un expediente reactivado.

8. En qué casos una paralización termina en cierre definitivo.

9. Si conviene manejar motivos tipificados de paralización.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “paralizadas” existe como bandeja real
- qué casos ingresan realmente
- qué motivos la justifican
- cómo se documenta la paralización
- quién puede decidirla
- cómo y hacia dónde se reactiva el expediente
- y en qué supuestos la paralización termina en cierre
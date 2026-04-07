# [BANDEJA 18] GESTIÓN EXTERNA

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos que, luego del tramo interno ordinario, deben ser derivados a una instancia externa para su continuación, tratamiento, ejecución o resolución complementaria.

Esta bandeja representa principalmente el tramo posterior a:

- `16-bandeja-fallos-notificados.md`

y también puede recibir casos por decisión administrativa expresa desde otras estaciones del flujo, cuando corresponda.

La gestión externa no debe entenderse como una simple salida sin retorno.

Debe contemplar:

- la definición del destino externo
- la formalización de la derivación
- el seguimiento del trámite externo
- y el posible **reingreso** del expediente con actuaciones, resultados o documentación complementaria

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- `spec/05-bandejas/16-bandeja-fallos-notificados.md`
- `spec/05-bandejas/17-bandeja-con-apelacion.md`
- `spec/05-bandejas/12-bandeja-pendiente-de-fallo.md`
- `spec/05-bandejas/13-bandeja-fallo-pendiente-de-notificacion.md`
- `spec/05-bandejas/14-bandeja-fallos-listos-para-notificar.md`
- `spec/05-bandejas/15-bandeja-notificacion-fallo-en-proceso.md`
- documentos canónicos de notificación
- documentos canónicos de acto administrativo
- documentos canónicos de documento
- documentos canónicos de snapshot
- catálogos de:
  - `TipoGestionExterna`
  - `ResultadoExterno`
  - `TipoEventoActa`
  - `EstadoActa`
  - `MotivoCierreActa`

---

## 1. Nombre de la bandeja

**Bandeja — Gestión externa**

Nombre alternativo conversable con el área:

**Derivadas a instancia externa**

---

## 2. Objetivo

Permitir identificar, derivar, controlar y eventualmente reingresar los casos que salen del circuito interno ordinario para continuar su tratamiento en una instancia externa.

La finalidad de esta bandeja es:

- decidir a qué destino externo se deriva cada caso
- formalizar la derivación
- registrar el destino concreto
- mantener visibilidad del estado externo del trámite
- permitir el reingreso del expediente cuando vuelva con novedades, actuaciones o resultados
- evitar modelar la gestión externa como una salida definitiva sin trazabilidad

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- ya están en condiciones de ser derivadas a gestión externa
- ya fueron derivadas y siguen en trámite externo
- o reingresaron desde una instancia externa y requieren tratamiento interno posterior

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha del fallo
- fecha de notificación fehaciente del fallo
- destino externo definido
- fecha de derivación
- estado actual del trámite externo
- última novedad o resultado externo registrado
- si hubo reingreso
- fecha de reingreso
- observaciones relevantes
- documentación o actuaciones externas asociadas
- acción pendiente principal

---

## 4. Qué casos entran

Entran en esta bandeja los casos que deben ser derivados o que ya fueron derivados a una instancia externa.

Casos típicos de ingreso:

- casos cuyo destino natural, vencido el plazo posterior a la notificación del fallo y sin apelación ni otra actuación que altere el recorrido, es la derivación externa
- casos enviados manualmente a gestión externa por decisión administrativa
- casos ya derivados a una instancia externa y aún en trámite
- casos que reingresan desde la instancia externa con resultados, actuaciones, documentos, evidencias o decisiones que deben ser tratadas

Regla importante:

- la bandeja debe permitir decidir **a qué destino externo se deriva** el expediente
- como mínimo, debe contemplarse:
  - **apremios**
  - **Juzgado de Paz**
- la derivación no debe modelarse como un único destino genérico, porque el comportamiento posterior del expediente cambia según la instancia externa elegida

No deberían entrar aquí:

- casos que todavía deben permanecer en espera posterior a la notificación del fallo
- casos con apelación activa que aún no terminaron su tratamiento
- casos cerrados definitivamente sin necesidad de gestión externa

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver:

- si el caso debe salir efectivamente a instancia externa
- a qué destino externo concreto debe ir
- qué documentación o actuación acompaña esa derivación
- cómo se registra el avance o resultado del trámite externo
- qué debe hacerse cuando el expediente reingresa

Es importante dejar explícito que la gestión externa:

- **no es** necesariamente una salida definitiva
- puede generar un **reingreso** con efectos materiales sobre el expediente
- y ese reingreso puede obligar a retomar circuitos internos ya definidos

---

## 6. Acciones posibles

### 6.1 Definir destino externo

- **Acción:** definir a qué instancia externa se deriva el expediente
- **Evento o proceso que dispara:** decisión administrativa de salida del circuito interno ordinario
- **Resultado esperado:** el caso queda encuadrado en el destino externo correcto
- **Destino:** permanece en `18-bandeja-gestion-externa.md` hasta formalizar la derivación

**Regla importante:**

- esta acción debe permitir, como mínimo, optar entre:
  - **apremios**
  - **Juzgado de Paz**
- sin perjuicio de que a futuro puedan existir otros destinos externos

---

### 6.2 Formalizar derivación externa

- **Acción:** registrar y emitir la derivación del expediente a la instancia externa definida
- **Evento o proceso que dispara:** confirmación administrativa de la salida a gestión externa
- **Resultado esperado:** el expediente queda formalmente derivado, con trazabilidad completa del destino y de la actuación realizada
- **Destino:** permanece en `18-bandeja-gestion-externa.md` como caso en trámite externo

---

### 6.3 Registrar trámite o novedad externa

- **Acción:** registrar una novedad, movimiento o resultado producido en la instancia externa
- **Evento o proceso que dispara:** recepción de información desde apremios, Juzgado de Paz u otra instancia externa
- **Resultado esperado:** el expediente mantiene trazabilidad del recorrido externo
- **Destino:** permanece en `18-bandeja-gestion-externa.md`

---

### 6.4 Registrar reingreso con resultado conclusivo

- **Acción:** registrar que el expediente reingresa con un resultado que no exige reactivar el circuito resolutivo interno
- **Evento o proceso que dispara:** recepción de resultado externo como pago, cumplimiento, cierre o actuación conclusiva equivalente
- **Resultado esperado:** el caso puede cerrar o seguir el tratamiento administrativo final que corresponda
- **Destino:** según el resultado:
  - bandeja de cerradas
  - o circuito administrativo final que corresponda

Ejemplo típico:

- pago realizado en apremios

---

### 6.5 Registrar reingreso con actuaciones, documentos, evidencias o información complementaria

- **Acción:** incorporar al expediente actuaciones externas, documentos, evidencias o información recibida desde la instancia externa
- **Evento o proceso que dispara:** reingreso desde la gestión externa con contenido que debe ser tratado por el área
- **Resultado esperado:** el expediente vuelve a estar enriquecido con la información externa recibida y queda listo para su tratamiento interno
- **Destino:** permanece momentáneamente en `18-bandeja-gestion-externa.md` hasta definirse el circuito interno siguiente

---

### 6.6 Reingresar para rectificar, modificar o emitir nuevo fallo

- **Acción:** reingresar el expediente al circuito interno porque la actuación externa obliga a revisar el contenido del fallo
- **Evento o proceso que dispara:** decisión o actuación externa que exige:
  - rectificar fallo
  - modificar fallo
  - emitir nuevo fallo
- **Resultado esperado:** el expediente deja la gestión externa como mera salida y vuelve al circuito interno resolutivo

- **Destino:** `12-bandeja-pendiente-de-fallo.md` o la instancia interna equivalente que se defina como punto correcto de reingreso

**Regla importante:**

- si el reingreso requiere rectificación, modificación o emisión de nuevo fallo:
  - debe generarse el nuevo fallo o el fallo rectificado
  - debe generarse la documentación asociada
  - debe cumplirse nuevamente el circuito de firma
  - debe notificarse el nuevo fallo
  - y recién luego, según el resultado posterior, podrá derivarse nuevamente a apremios u otra gestión externa si corresponde

Ejemplo típico:

- reingreso desde **Juzgado de Paz** con instrucción o necesidad de actualizar el valor de la infracción, lo que obliga a modificar o rehacer el fallo y notificar nuevamente

---

### 6.7 Mantener en trámite externo

- **Acción:** dejar el expediente en esta bandeja mientras la gestión externa siga activa
- **Evento o proceso que dispara:** continuidad del trámite en apremios, Juzgado de Paz u otra instancia externa
- **Resultado esperado:** el caso sigue visible como expediente derivado y aún no concluido
- **Destino:** permanece en `18-bandeja-gestion-externa.md`

---

### 6.8 Registrar decisión administrativa especial

- **Acción:** aplicar una decisión que altere el recorrido esperado del caso en gestión externa
- **Evento o proceso que dispara:** instrucción de Dirección de Faltas u otra autoridad competente
- **Resultado esperado:** el expediente cambia de destino o de tratamiento según la decisión adoptada
- **Destino:** según la decisión adoptada

---

### 6.9 Paralizar el caso

- **Acción:** suspender o detener el trámite
- **Evento o proceso que dispara:** incidencia relevante, conflicto jurídico-administrativo, espera externa o decisión fundada
- **Resultado esperado:** el expediente sale del curso operativo normal
- **Destino:** `19-bandeja-paralizadas.md`

---

### 6.10 Cerrar el caso

- **Acción:** cerrar administrativamente el expediente desde esta bandeja
- **Evento o proceso que dispara:** resultado externo concluyente o decisión fundada que determine la finalización del caso
- **Resultado esperado:** el expediente se considera finalizado para operación ordinaria
- **Destino:** `20-bandeja-cerradas.md`

---

## 7. Excepciones

### 7.1 Derivación externa sin destino explícito

No debería permitirse una salida a gestión externa sin registrar claramente el destino concreto.

Debe quedar explícito, como mínimo, si el expediente fue a:

- apremios
- Juzgado de Paz

---

### 7.2 Reingreso meramente informativo vs. reingreso con efectos materiales

No todo reingreso tiene el mismo impacto.

Debe diferenciarse entre:

- reingreso solo informativo o conclusivo
- reingreso con documentación o actuaciones para agregar
- reingreso que obliga a reactivar el circuito interno
- reingreso que exige modificar o emitir nuevo fallo

---

### 7.3 Reingreso desde Juzgado de Paz con cambio de fondo en la resolución

Puede ocurrir que el expediente vuelva desde Juzgado de Paz con una indicación o resultado que obligue a alterar el fallo existente.

Por ejemplo:

- rectificar fallo condenatorio
- modificar el valor de la infracción
- emitir un nuevo fallo

En ese caso:

- no alcanza con registrar el resultado externo
- el expediente debe volver al circuito interno correspondiente
- debe notificarse nuevamente el fallo resultante antes de cualquier nueva derivación a apremios

---

### 7.4 Reingreso desde apremios con pago o cumplimiento

Puede ocurrir que el expediente vuelva desde apremios con resultado de pago o cumplimiento.

En ese caso:

- el reingreso puede ser conclusivo
- no necesariamente exige volver al circuito de fallo
- pero sí debe quedar trazabilidad completa del resultado y del cierre que corresponda

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Si los destinos externos mínimos a contemplar son efectivamente:
   - apremios
   - Juzgado de Paz

3. Qué otros destinos externos podrían existir.

4. Qué documentación o formalidad exige cada derivación externa.

5. Si el expediente permanece visible aquí durante todo el trámite externo.

6. Qué tipos de reingreso existen en la práctica:
   - pago o cumplimiento
   - actuaciones
   - documentos
   - evidencias
   - requerimiento de revisión
   - modificación o nuevo fallo
   - otros

7. Cuál es el punto correcto de reingreso cuando la instancia externa obliga a rectificar, modificar o rehacer el fallo.

8. Si el reingreso desde Juzgado de Paz con cambio en el valor de la infracción debe reiniciar efectivamente:
   - fallo
   - documentación
   - firma
   - notificación del nuevo fallo

9. Si luego de ese nuevo fallo, y ante incumplimiento, el caso puede volver a derivarse a apremios.

10. Qué resultados externos permiten cerrar directamente el expediente.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “gestión externa” existe como bandeja real
- qué destinos externos contempla realmente
- cómo se formaliza cada derivación
- cómo se mantiene la trazabilidad del trámite externo
- qué tipos de reingreso existen
- cuáles son meramente informativos o conclusivos
- cuáles obligan a reactivar el circuito interno
- cuándo el reingreso exige rectificar, modificar o emitir nuevo fallo
- y cómo se articula ese reingreso con la nueva notificación del fallo y una eventual futura derivación a apremios
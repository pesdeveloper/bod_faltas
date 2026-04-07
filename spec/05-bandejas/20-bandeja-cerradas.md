# [BANDEJA 20] CERRADAS

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir la bandeja operativa donde quedan los casos que **ya no requieren más tratamiento ordinario** y cuya situación se considera administrativamente finalizada.

Esta bandeja no representa una etapa intermedia del flujo.

Es la estación final para expedientes concluidos, cualquiera haya sido el recorrido previo que llevó a ese cierre.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/00-metodologia-validacion-bandejas.md`
- el resto de las bandejas operativas desde las que puede producirse un cierre
- documentos canónicos de snapshot
- catálogos de:
  - `EstadoActa`
  - `TipoEventoActa`
  - `MotivoCierreActa`

---

## 1. Nombre de la bandeja

**Bandeja — Cerradas**

Nombre alternativo conversable con el área:

**Expedientes cerrados**

---

## 2. Objetivo

Permitir identificar y conservar trazabilidad de los expedientes que ya fueron concluidos y que no deben continuar circulando por el flujo operativo ordinario.

La finalidad de esta bandeja es:

- separar claramente los casos finalizados de los casos activos
- registrar el motivo de cierre
- consolidar el estado final administrativo del expediente
- permitir consulta posterior sin que el caso siga mezclado con bandejas de trabajo activo
- evitar reingresos informales o movimientos sin decisión expresa

---

## 3. Qué se muestra

En esta bandeja se muestran actas que:

- fueron cerradas administrativamente
- ya no requieren gestión operativa ordinaria
- tienen definido un motivo de cierre o una causa conclusiva suficiente

Típicamente se visualizaría, a nivel funcional:

- identificación del acta
- materia o tipo de acta
- infractor o sujeto vinculado
- fecha de cierre
- motivo de cierre
- última etapa activa antes del cierre
- observaciones relevantes
- última actuación significativa
- documentación o resultado final asociado
- acción pendiente principal, si existiera alguna residual solo de consulta

---

## 4. Qué casos entran

Entran en esta bandeja los casos que llegaron a una conclusión administrativa suficiente para considerar finalizado el expediente.

Casos típicos de ingreso:

- cumplimiento o pago concluyente
- resultado externo conclusivo
- decisión administrativa de cierre
- finalización administrativa concluyente
- imposibilidad de continuar con decisión fundada de finalización
- resolución firme o estado equivalente que no requiere más gestión ordinaria

Regla importante:

- esta bandeja debe usarse para cierres reales
- no para casos meramente detenidos
- no para casos todavía discutibles
- no para casos que siguen requiriendo seguimiento activo

No deberían entrar aquí:

- casos paralizados
- casos con gestión externa aún abierta
- casos con apelación aún en trámite
- casos que todavía requieren nueva notificación, nuevo fallo o actuación posterior

---

## Regla conceptual sobre cierre y archivo

La bandeja de cerradas debe entenderse como la última estación operativa del expediente.

No equivale necesariamente al archivo en sentido documental o archivístico.

Un expediente puede:

- quedar cerrado administrativamente
- permanecer consultable en la bandeja de cerradas
- y luego pasar a un proceso de archivo o conservación documental según la política que se defina

Por lo tanto:

- **cierre** = finalización operativa / administrativa del caso
- **archivo** = conservación posterior del expediente ya cerrado

---

## 5. Qué se espera resolver aquí

En esta bandeja se espera resolver principalmente una sola cosa:

- que el expediente quede consolidado como finalizado

La operatoria posterior debería ser, en principio, de:

- consulta
- auditoría
- trazabilidad
- revisión excepcional si una decisión posterior obligara a reabrir o reingresar el caso

Esta bandeja no es de trabajo operativo ordinario.

Es la estación de **cierre administrativo**.

---

## 6. Acciones posibles

### 6.1 Registrar cierre del expediente

- **Acción:** registrar formalmente que el caso queda cerrado
- **Evento o proceso que dispara:** decisión administrativa, resultado externo concluyente, pago, cumplimiento o condición equivalente de finalización
- **Resultado esperado:** el expediente deja el circuito activo y queda consolidado como cerrado
- **Destino:** permanece en `20-bandeja-cerradas.md`

---

### 6.2 Registrar motivo de cierre

- **Acción:** identificar y dejar trazado el motivo concreto por el cual se cerró el expediente
- **Evento o proceso que dispara:** cierre administrativo del caso
- **Resultado esperado:** el cierre queda documentado de forma consistente y auditable
- **Destino:** permanece en `20-bandeja-cerradas.md`

---

### 6.3 Incorporar documentación o constancia final

- **Acción:** agregar al expediente la documentación, constancia o resultado final asociado al cierre
- **Evento o proceso que dispara:** recepción o consolidación de la actuación final
- **Resultado esperado:** el expediente cerrado conserva trazabilidad completa de su conclusión
- **Destino:** permanece en `20-bandeja-cerradas.md`

---

### 6.4 Consultar expediente cerrado

- **Acción:** acceder al caso para revisión, consulta o auditoría
- **Evento o proceso que dispara:** necesidad posterior de verificación o análisis histórico
- **Resultado esperado:** el expediente puede revisarse sin reactivarlo
- **Destino:** permanece en `20-bandeja-cerradas.md`

---

### 6.5 Reabrir o reingresar excepcionalmente el expediente

- **Acción:** sacar el caso de cerradas si una decisión posterior obliga a reactivar su tratamiento
- **Evento o proceso que dispara:** decisión administrativa fundada, reingreso externo con efecto material o detección de una situación que exige nueva actuación
- **Resultado esperado:** el expediente deja de estar en condición de cierre y vuelve al circuito operativo que corresponda
- **Destino:** la bandeja o circuito interno que se defina según la naturaleza del reingreso

**Regla importante:**

- esta acción debe ser excepcional
- no debería ocurrir como operación ordinaria
- debe quedar especialmente trazada

---

## 7. Excepciones

### 7.1 Casos cerrados sin motivo claro

No debería permitirse que un expediente llegue a cierre sin motivo explícito o sin soporte suficiente.

Debe quedar trazado:

- por qué se cerró
- desde qué etapa
- con qué fundamento o resultado

---

### 7.2 Casos que en realidad deberían estar paralizados y no cerrados

Puede ocurrir que un caso sea enviado a cerradas cuando en realidad todavía existe posibilidad o necesidad de actuación futura.

En ese caso:

- no debería cerrarse
- debería permanecer activo o paralizado, según corresponda

---

### 7.3 Cierre con gestión externa aún no consolidada

Si el expediente todavía tiene una gestión externa abierta o resultado no conclusivo, no debería llegar a cerradas salvo decisión expresa y fundada.

---

### 7.4 Reapertura excepcional

Debe validarse con el área en qué supuestos un expediente cerrado puede reabrirse o reingresar.

No debería ser una acción frecuente ni informal.

---

## 8. Puntos a validar con Dirección de Faltas

1. Si esta bandeja existe realmente como estación operativa diferenciada.

2. Qué situaciones consideran verdaderamente de cierre.

3. Qué diferencia práctica hacen entre:
   - cerrado
   - archivado
   - paralizado
   - concluido por pago
   - concluido por resultado externo

4. Si todo cierre requiere motivo explícito y documentación de respaldo.

5. Qué motivos de cierre conviene tipificar.

6. Si desde gestión externa puede cerrarse directamente un caso y en qué supuestos.

7. En qué casos un expediente cerrado podría reabrirse o reingresar.

8. Si la operatoria posterior sobre cerradas es solo de consulta o si existen otras acciones reales.

---

## 9. Resultado esperado de validación

Al validar esta bandeja con Dirección de Faltas debería quedar definido:

- si “cerradas” existe como bandeja real
- qué casos ingresan realmente
- qué significa operativamente que un expediente esté cerrado
- qué motivos de cierre deben registrarse
- qué diferencia real existe con paralización o archivo
- qué documentación mínima debe acompañar el cierre
- y en qué supuestos excepcionales un expediente cerrado puede reabrirse o reingresar
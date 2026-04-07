# [PROCESO 21] ARCHIVO

**Estado:** Borrador operativo para validación  
**Última actualización:** 2026-04-07

## Propósito

Definir el proceso posterior al cierre administrativo del expediente, mediante el cual un caso ya concluido deja de formar parte del universo operativo habitual y pasa a una condición de **archivo**, conservación o resguardo documental/administrativo.

Este proceso debe entenderse como **posterior** a:

- `20-bandeja-cerradas.md`

y no como equivalente del cierre mismo.

El archivo no debe considerarse necesariamente irreversible.

Un expediente archivado puede, en supuestos fundados, ser **desarchivado** y reingresar al circuito operativo.

---

## Relación con otros documentos

Este documento debe leerse junto con:

- `spec/05-bandejas/20-bandeja-cerradas.md`
- `spec/05-bandejas/06-bandeja-analisis-presentaciones-pagos.md`
- documentos canónicos de documento
- documentos canónicos de snapshot
- definiciones futuras de política documental, conservación, guarda y consulta histórica, si luego se documentan por separado

---

## 1. Nombre del proceso

**Proceso — Archivo**

Nombre alternativo conversable con el área:

**Pase a archivo**

---

## 2. Objetivo

Permitir definir qué ocurre con los expedientes una vez que ya fueron cerrados y dejan de requerir gestión operativa ordinaria.

La finalidad de este proceso es:

- separar el **cierre operativo** del **archivo**
- determinar cuándo un expediente cerrado pasa efectivamente a archivo
- conservar trazabilidad de ese pase
- permitir consulta posterior sin mantener el expediente en las bandejas activas de trabajo
- definir si el archivo es inmediato, diferido, manual, automático o por lote
- contemplar el posible **desarchivo** cuando una actuación posterior obligue a reingresar el expediente al circuito operativo

---

## 3. Qué casos abarca

Este proceso abarca expedientes que:

- ya se encuentran cerrados administrativamente
- ya no requieren gestión ordinaria
- deben quedar en conservación, resguardo o consulta histórica

Casos típicos alcanzados por este proceso:

- expedientes cerrados por pago o cumplimiento
- expedientes cerrados por resultado externo conclusivo
- expedientes cerrados por decisión administrativa
- expedientes cerrados tras agotar el recorrido operativo normal
- expedientes cerrados que deben dejar de figurar en el universo operativo habitual

---

## 4. Cuándo se activa

Este proceso puede activarse de distintas maneras, a validar con el área:

- inmediatamente después del cierre
- luego de un tiempo de permanencia en la bandeja de cerradas
- por acción manual del operador
- por proceso automático
- por proceso batch o por lote
- por política de antigüedad
- por criterio documental o administrativo

Regla importante:

- el archivo **no equivale** al cierre
- el expediente primero debe quedar **cerrado**
- recién después puede pasar a **archivo**

---

## 5. Qué se espera resolver aquí

En este proceso se espera resolver:

- cuándo un expediente cerrado pasa a archivo
- bajo qué criterio se produce ese pase
- qué información mínima debe quedar consolidada antes del archivo
- cómo queda disponible para consulta posterior
- si el archivo cambia solo la visibilidad operativa o también la forma de conservación documental
- cómo se gestiona el **desarchivo** cuando el expediente necesita volver al circuito operativo

Este proceso no agrega nuevas decisiones sobre el fondo del expediente mientras el caso permanece archivado.

Su función es ordenar la vida posterior del caso ya concluido y contemplar su eventual reingreso.

---

## 6. Acciones posibles

### 6.1 Marcar expediente como archivado

- **Acción:** registrar formalmente que el expediente cerrado pasa a archivo
- **Evento o proceso que dispara:** acción manual, proceso automático, lote o política de archivo
- **Resultado esperado:** el caso deja de pertenecer al universo operativo ordinario y queda en condición de archivo
- **Destino:** archivo

---

### 6.2 Mantener expediente cerrado sin archivar todavía

- **Acción:** dejar el expediente en cerradas sin ejecutar todavía el pase a archivo
- **Evento o proceso que dispara:** criterio operativo o política que requiera permanencia previa en la bandeja de cerradas
- **Resultado esperado:** el expediente sigue cerrado, pero aún no archivado
- **Destino:** `20-bandeja-cerradas.md`

---

### 6.3 Archivar por lote o antigüedad

- **Acción:** enviar a archivo un conjunto de expedientes cerrados según regla de tiempo, lote o criterio administrativo
- **Evento o proceso que dispara:** proceso batch, revisión periódica o política documental
- **Resultado esperado:** múltiples expedientes cerrados pasan a archivo de forma controlada y trazable
- **Destino:** archivo

---

### 6.4 Consolidar documentación final antes del archivo

- **Acción:** verificar o completar la documentación mínima necesaria antes de archivar
- **Evento o proceso que dispara:** revisión previa al pase a archivo
- **Resultado esperado:** el expediente queda archivado con integridad documental suficiente
- **Destino:** permanece temporalmente en cerradas o pasa a archivo según el resultado de la revisión

---

### 6.5 Consultar expediente archivado

- **Acción:** acceder al expediente archivado para consulta, control, auditoría o revisión histórica
- **Evento o proceso que dispara:** necesidad posterior de verificación o consulta
- **Resultado esperado:** el expediente puede ser consultado sin volver al circuito operativo ordinario
- **Destino:** archivo

---

### 6.6 Desarchivar expediente

- **Acción:** sacar un expediente de archivo para devolverlo al circuito operativo
- **Evento o proceso que dispara:** decisión administrativa fundada, nueva actuación, orden externa, necesidad de revisión o cualquier situación que obligue a reactivar el tratamiento del caso
- **Resultado esperado:** el expediente deja de estar archivado y reingresa al circuito operativo
- **Destino:** `06-bandeja-analisis-presentaciones-pagos.md`

**Regla importante:**

- no existe una bandeja propia de desarchivo
- desarchivar debe quedar especialmente trazado
- no debería ser una operación ordinaria
- el reingreso desde archivo vuelve a análisis

---

## 7. Excepciones

### 7.1 Cierre sin archivo inmediato

Puede ocurrir que el expediente quede cerrado, pero no pase a archivo en ese mismo momento.

Eso no es un problema si queda claro que:

- el expediente ya está cerrado
- pero todavía no se ejecutó el proceso de archivo

---

### 7.2 Archivo sin cierre previo

No debería existir archivo de un expediente que todavía no esté cerrado, salvo que el área defina una excepción muy específica.

Regla sugerida:

- primero cierre
- después archivo

---

### 7.3 Expediente archivado pero todavía útil para consulta operativa frecuente

Puede ocurrir que un expediente ya esté cerrado y archivado, pero siga siendo consultado con cierta frecuencia.

Debe validarse con el área si eso cambia o no el criterio de archivo.

---

### 7.4 Desarchivo y reingreso

Puede ocurrir que una situación posterior obligue a sacar un expediente de archivo y devolverlo a tratamiento.

En ese caso:

- debe quedar especialmente trazado
- no debería confundirse con la operatoria ordinaria
- el expediente reingresa a `06-bandeja-analisis-presentaciones-pagos.md`

---

## 8. Puntos a validar con Dirección de Faltas

1. Si en la práctica distinguen realmente entre:
   - cerrado
   - archivado

2. Si el archivo existe como proceso diferenciado o si hoy ambos conceptos se usan como equivalentes.

3. Si el pase a archivo es:
   - inmediato
   - diferido
   - manual
   - automático
   - por lote
   - por antigüedad

4. Qué información mínima debe estar consolidada antes del archivo.

5. Si un expediente archivado sigue siendo fácilmente consultable por el área.

6. En qué supuestos un expediente archivado puede desarchivarse o reingresar.

7. Si conviene modelar el archivo como:
   - proceso
   - estado
   - marca documental
   - o combinación de estos conceptos

8. Si el expediente archivado, al desarchivarse, reingresa siempre a `06-bandeja-analisis-presentaciones-pagos.md`.

---

## 9. Resultado esperado de validación

Al validar este proceso con Dirección de Faltas debería quedar definido:

- si el archivo se distingue realmente del cierre
- cuándo ocurre el pase a archivo
- cómo se ejecuta
- qué requisitos mínimos debe cumplir el expediente antes de archivarse
- qué visibilidad conserva después del archivo
- en qué supuestos puede desarchivarse
- y confirmar que el reingreso desde archivo vuelve a `06-bandeja-analisis-presentaciones-pagos.md`
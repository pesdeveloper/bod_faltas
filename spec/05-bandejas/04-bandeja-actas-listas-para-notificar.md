# [CANONICO] BANDEJA — ACTAS LISTAS PARA NOTIFICAR

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de actas listas para notificar, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/05-notificacion.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/03-bandeja-preparacion-para-notificacion-acta.md`

---

# 1. Nombre de la bandeja

**Actas listas para notificar**

---

# 2. Objetivo

Mostrar las actas que ya tienen completa y firmada toda la documentación previa requerida para iniciar la notificación del acta y que, por lo tanto, ya son aptas para emitir la notificación por el canal seleccionado.

Esta bandeja representa la cola real de salida hacia la notificación efectiva.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- canal de notificación previsto o seleccionado
- confirmación de que la documentación previa ya está completa
- confirmación de que todos los documentos requeridos ya están firmados
- alertas operativas previas al envío, si existieran
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con una acta completamente preparada para iniciar su notificación, es decir:

- ya no tienen bloqueos documentales previos
- ya no tienen firmas pendientes sobre documentos requeridos
- ya tienen resuelta la preparación previa
- ya pueden pasar a la emisión efectiva de la notificación

Típicamente llegan aquí desde:

- `Preparación para notificación del acta`

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- efectivamente se envía a notificación
- requiere ajuste final del canal
- requiere control final previo al envío
- debe volver a preparación previa si se detecta un problema excepcional
- debe paralizarse, si excepcionalmente correspondiera

En términos simples: esta es la bandeja de los casos que ya están “potables” para notificar.

---

# 6. Regla de ingreso a esta bandeja

Un acta solo debe ingresar a esta bandeja cuando:

- todos los documentos previos necesarios ya fueron generados
- todos los documentos que requieren firma ya se encuentran firmados
- no existe bloqueo documental pendiente
- no existe prerrequisito previo que impida el envío de la notificación

Por lo tanto, esta bandeja no debe mezclar casos:
- listos para enviar
con casos:
- todavía bloqueados por documentación o firma

---

# 7. Acciones posibles

## 7.1 Emitir notificación del acta
- **Evento o proceso que dispara:** `NOTIFICACION_ENVIADA`
- **Resultado esperado:** se inicia efectivamente la gestión de notificación del acta
- **Destino:** `Notificación del acta en proceso`

## 7.2 Ajustar canal de notificación
- **Evento o proceso que dispara:** proceso operativo interno a validar; podría no requerir evento procesal específico si solo modifica metadata previa al envío
- **Resultado esperado:** se corrige o redefine el canal por el cual se emitirá la notificación
- **Destino:** sigue en `Actas listas para notificar`

## 7.3 Revisar antes de emitir
- **Evento o proceso que dispara:** control operativo final; a validar si requiere o no evento explícito
- **Resultado esperado:** se confirma que el caso efectivamente puede emitirse o se detecta una inconsistencia excepcional
- **Destino:** sigue en `Actas listas para notificar` o vuelve a `Preparación para notificación del acta`, a validar con el área

## 7.4 Volver a preparación para notificación
- **Evento o proceso que dispara:** a validar con el área; podría resolverse mediante corrección documental o reactivación de prerrequisito previo
- **Resultado esperado:** el caso deja esta bandeja porque se detecta que no estaba realmente listo para enviar
- **Destino:** `Preparación para notificación del acta`

## 7.5 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

---

# 8. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- puede haber casos que entren aquí y se emitan casi inmediatamente
- puede haber ajustes finales de canal antes de emitir
- debe validarse si “revisar antes de emitir” es una acción real o simplemente una práctica operativa implícita
- debe validarse si realmente puede ocurrir un retorno a la bandeja anterior
- debe validarse si la paralización desde esta etapa existe en la práctica

---

# 9. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación separada y visible?
- ¿Es correcto que aquí lleguen solo casos ya completamente habilitados para notificar?
- ¿Es correcto distinguir esta bandeja de la de preparación para notificación?
- ¿Qué acciones reales se hacen aquí, además de emitir?
- ¿Se puede ajustar canal desde esta bandeja?
- ¿Puede detectarse aquí un problema que haga volver el caso a la etapa anterior?
- ¿La paralización desde esta bandeja existe en la práctica?

---

# 10. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si esta estación existe realmente como paso separado
- si conviene distinguir entre “preparación para notificar” y “listas para notificar”
- si las acciones previas al envío efectivo están bien representadas
- si el modelo actual necesita flags de snapshot como:
  - caso habilitado para notificar
  - documentos completos
  - firmas completas
  - canal listo
- si el evento `NOTIFICACION_ENVIADA` queda correctamente ubicado recién en esta bandeja
# [CANONICO] BANDEJA — PENDIENTE DE ACTO ADMINISTRATIVO

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de casos pendientes de acto administrativo, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/06-bandeja-analisis-presentaciones-pagos.md`

---

# 1. Nombre de la bandeja

**Pendiente de acto administrativo**

---

# 2. Objetivo

Mostrar las actas que ya atravesaron la etapa de análisis, presentaciones y tratamiento previo, y que ahora se encuentran en condiciones de ingresar al tramo resolutivo formal del caso.

Esta bandeja representa el punto donde la Dirección de Faltas decide o confirma que el caso debe pasar a la etapa de preparación del acto administrativo correspondiente.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- resumen de presentaciones o actuaciones previas relevantes
- indicador de si hubo pago, desistimiento o situación equivalente que pudiera alterar el curso
- alertas administrativas relevantes
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = PENDIENTE_ACTO_ADMINISTRATIVO`

o casos que, luego de análisis, presentaciones y tratamiento administrativo, ya quedaron en condiciones de ingresar al circuito de acto administrativo.

Típicamente llegan aquí desde:

- `Análisis / presentaciones / pagos`

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- efectivamente debe pasar al circuito de acto administrativo
- requiere una última revisión antes de iniciar esa etapa
- requiere volver a análisis por una cuestión pendiente detectada
- debe derivarse a gestión externa, si excepcionalmente eso aún puede ocurrir desde aquí
- debe paralizarse
- debe archivarse
- debe cerrarse por otra causa ya consolidada, si el área así lo valida

En términos simples: aquí se confirma que el caso entra al tramo resolutivo formal.

---

# 6. Acciones posibles

## 6.1 Iniciar preparación del acto administrativo
- **Evento o proceso que dispara:** `ACTO_ADMINISTRATIVO_GENERADO` o inicio del circuito resolutivo/documental, a validar con el área
- **Resultado esperado:** el caso pasa a la etapa de trabajo activo sobre el acto administrativo
- **Destino:** `Acto administrativo en proceso`

## 6.2 Revisar antes de iniciar acto administrativo
- **Evento o proceso que dispara:** control operativo interno; a validar si requiere evento explícito o no
- **Resultado esperado:** se confirma que el caso efectivamente debe pasar al tramo resolutivo o se detecta alguna falta previa
- **Destino:** sigue en `Pendiente de acto administrativo` o vuelve a `Análisis / presentaciones / pagos`

## 6.3 Volver a análisis
- **Evento o proceso que dispara:** a validar con el área; podría resolverse mediante observación, corrección o tratamiento adicional
- **Resultado esperado:** se detecta que el caso todavía no estaba listo para ingresar al tramo resolutivo
- **Destino:** `Análisis / presentaciones / pagos`

## 6.4 Incorporar documento previo relevante
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se agrega documentación que completa o confirma el paso al tramo resolutivo
- **Destino:** sigue en `Pendiente de acto administrativo`

## 6.5 Derivar a gestión externa
- **Evento o proceso que dispara:** `DERIVACION_EXTERNA_ENVIADA`
- **Resultado esperado:** el caso sale al circuito externo correspondiente
- **Destino:** `Gestión externa`
- **Observación:** validar con el área si esta salida sigue siendo real desde esta estación

## 6.6 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

## 6.7 Archivar
- **Evento o proceso que dispara:** `ARCHIVO_DISPUESTO`
- **Resultado esperado:** el caso cierra pase a cerradas
- **Destino:** `Cerradas`

## 6.8 Anular excepcionalmente
- **Evento o proceso que dispara:** `ACTA_ANULADA`
- **Resultado esperado:** el caso queda invalidado y fuera del circuito normal
- **Destino:** `Cerradas`

---

# 7. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- algunos casos podrían pasar muy rápido a acto administrativo
- otros podrían requerir una revisión final previa
- debe validarse si aún pueden volver a análisis desde aquí
- debe validarse si derivación externa desde esta estación ocurre en la práctica
- debe validarse debe validarse si desde esta etapa puede haber cierre
- debe validarse si el inicio del acto administrativo ocurre siempre desde esta bandeja o a veces nace en continuidad inmediata desde análisis

---

# 8. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación operativa visible?
- ¿Todo caso que termina análisis pasa por aquí antes del acto administrativo?
- ¿La revisión final previa al acto existe realmente como acción?
- ¿Puede volver a análisis desde esta bandeja?
- ¿La derivación a gestión externa desde aquí ocurre en la práctica?
- ¿Archivar desde esta estación es real?
- ¿Falta alguna acción relevante propia de esta etapa?

---

# 9. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si `PENDIENTE_ACTO_ADMINISTRATIVO` alcanza como estado dominante para esta estación
- si esta bandeja existe realmente como paso separado
- si el paso a acto administrativo en proceso requiere una confirmación operativa visible
- si hay retornos válidos a análisis
- si los catálogos actuales alcanzan para representar bien esta estación
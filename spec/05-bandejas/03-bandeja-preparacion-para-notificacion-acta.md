# [CANONICO] BANDEJA — PREPARACIÓN PARA NOTIFICACIÓN DEL ACTA

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de preparación para notificación del acta, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/09-firma-tipo.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/02-bandeja-enriquecimiento.md`

---

# 1. Nombre de la bandeja

**Preparación para notificación del acta**

---

# 2. Objetivo

Mostrar las actas cuya información ya es suficiente para avanzar hacia la notificación, pero que todavía requieren preparación documental previa, incluyendo generación de medidas preventivas, resoluciones u otros documentos que deben quedar emitidos y, cuando corresponda, firmados antes de poder notificar.

Esta bandeja representa la etapa de preparación formal previa a que el caso quede realmente apto para envío de notificación.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- documentos previos requeridos para notificación
- indicador de documentos pendientes de firma
- cantidad de documentos pendientes de firma, si aporta valor
- indicador de si el caso ya quedó habilitado o no para notificar
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con una etapa previa a la notificación del acta, donde:

- el contenido del acta ya está correcto
- el caso ya no está en enriquecimiento dominante
- todavía falta generar, completar o firmar documentación previa necesaria para notificar

Típicamente llegan aquí desde:

- `Enriquecimiento`

o desde una revisión inicial que concluye que el caso debe pasar a preparación documental previa antes de notificación.

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- requiere medida preventiva
- requiere resolución
- requiere otro documento previo
- requiere firma de Intendente u otro funcionario
- ya tiene todos los documentos previos generados
- ya tiene todos los documentos requeridos firmados
- puede quedar listo para pasar a la bandeja de actas listas para notificar
- debe volver a enriquecimiento si se detecta una falta previa
- debe paralizarse, si excepcionalmente correspondiera
- debe anularse, si excepcionalmente correspondiera

En términos simples: aquí se deja preparado el soporte documental previo a la notificación.

---

# 6. Regla de habilitación para notificar

Un acta solo puede pasar a la bandeja de actas listas para notificar cuando todos los documentos previos requeridos para su notificación se encuentran generados y firmados por los funcionarios correspondientes.

Mientras exista al menos un documento requerido pendiente de firma, el acta debe permanecer en esta bandeja.

La emisión efectiva de la notificación no debe ocurrir desde aquí mientras no se cumpla esa condición.

---

# 7. Acciones posibles

## 7.1 Generar medida preventiva
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO` y/o evento específico ya modelado por el proceso, a validar con el área
- **Resultado esperado:** se incorpora la medida preventiva requerida al caso
- **Destino:** sigue en `Preparación para notificación del acta`

## 7.2 Generar resolución
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO` y/o evento del circuito resolutivo/documental correspondiente
- **Resultado esperado:** se incorpora la resolución previa necesaria
- **Destino:** sigue en `Preparación para notificación del acta`

## 7.3 Generar otro documento previo necesario
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se agrega una pieza documental previa necesaria para habilitar la notificación
- **Destino:** sigue en `Preparación para notificación del acta`

## 7.4 Enviar documento a firma
- **Evento o proceso que dispara:** cambio documental a circuito de firma; a validar si además se refleja con evento específico o solo por estado documental
- **Resultado esperado:** el documento queda pendiente de firma del funcionario correspondiente
- **Destino:** sigue en `Preparación para notificación del acta`

## 7.5 Registrar documento firmado
- **Evento o proceso que dispara:** `ACTO_ADMINISTRATIVO_FIRMADO` o actualización documental equivalente, según el tipo de documento
- **Resultado esperado:** se reduce el conjunto de documentos pendientes de firma
- **Destino:** sigue en `Preparación para notificación del acta` hasta completar todas las firmas requeridas

## 7.6 Verificar que todos los documentos requeridos estén firmados
- **Evento o proceso que dispara:** control operativo y/o derivación del snapshot
- **Resultado esperado:** se confirma que el caso quedó habilitado para pasar a bandeja de actas listas para notificar
- **Destino:** `Actas listas para notificar`

## 7.7 Volver a enriquecimiento
- **Evento o proceso que dispara:** a validar con el área; podría resolverse mediante observación/corrección y nueva transición
- **Resultado esperado:** se detecta que el caso todavía no estaba listo para esta etapa
- **Destino:** `Enriquecimiento` / D2

## 7.8 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

## 7.9 Anular excepcionalmente
- **Evento o proceso que dispara:** `ACTA_ANULADA`
- **Resultado esperado:** el caso queda invalidado y fuera del circuito normal
- **Destino:** `Cerradas`

---

# 8. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- puede haber más de un documento pendiente de firma al mismo tiempo
- distintos documentos pueden requerir distintos firmantes
- algunos casos podrían no requerir ningún documento previo adicional y pasar rápido a la bandeja siguiente
- debe validarse si toda medida preventiva se genera siempre aquí
- debe validarse si toda resolución previa a notificación vive aquí
- debe validarse si una anulación desde esta etapa existe en la práctica y con qué grado de excepcionalidad

---

# 9. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación operativa separada?
- ¿Las medidas preventivas se generan aquí?
- ¿Las resoluciones previas a notificación se generan aquí?
- ¿Puede haber varios documentos pendientes de firma al mismo tiempo?
- ¿El acta debe quedar aquí hasta que todos estén firmados?
- ¿Hace falta mostrar cantidad de documentos pendientes de firma?
- ¿Hay casos que pasen muy rápido de aquí a “listas para notificar”?
- ¿La anulación desde esta etapa existe en la práctica?

---

# 10. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si esta etapa existe como estación operativa real
- si la firma previa bloquea efectivamente la notificación
- si el caso debe esperar aquí hasta reunir todos los documentos firmados
- si hace falta modelar flags específicos de documentos pendientes de firma en snapshot
- si los eventos y estados documentales actuales alcanzan para representar correctamente esta estación
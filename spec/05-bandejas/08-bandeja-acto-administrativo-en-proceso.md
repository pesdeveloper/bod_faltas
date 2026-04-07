# [CANONICO] BANDEJA — ACTO ADMINISTRATIVO EN PROCESO

# Estado
Canónico en validación funcional

# Última actualización
2026-04-06

# Propósito
Definir qué muestra la bandeja de acto administrativo en proceso, qué casos entran, qué acciones humanas pueden ejecutarse allí y qué efectos produce cada acción sobre el recorrido del acta.

# Relación con otros documentos
- `spec/03-catalogos/01-estados-acta.md`
- `spec/03-catalogos/02-tipos-evento-acta.md`
- `spec/03-catalogos/03-tipos-documento.md`
- `spec/03-catalogos/04-estados-documento.md`
- `spec/03-catalogos/09-firma-tipo.md`
- `spec/04-snapshot/01-campos-snapshot-operativo.md`
- `spec/04-snapshot/02-reglas-derivacion.md`
- `spec/04-snapshot/03-transiciones.md`
- `spec/05-bandejas/07-bandeja-pendiente-acto-administrativo.md`

---

# 1. Nombre de la bandeja

**Acto administrativo en proceso**

---

# 2. Objetivo

Mostrar las actas que ya ingresaron al tramo resolutivo formal y que actualmente se encuentran en trabajo activo de preparación, redacción, generación documental, firma, observación o sustitución del acto administrativo correspondiente.

Esta bandeja representa la etapa donde se construye y formaliza el acto administrativo del caso.

---

# 3. Qué se muestra

La bandeja debería mostrar, como mínimo:

- número o referencia principal del acta
- fecha y hora de labrado
- competencia o materia principal del caso
- estado actual
- último hito relevante
- tipo de acto o documento principal en preparación
- estado documental del acto principal
- indicador de documento pendiente de firma
- tipo de firma requerido o aplicado, si ya existe
- observaciones documentales, si existieran
- datos mínimos identificatorios del caso

No hace falta todavía definir columnas exactas finales; en esta fase importa validar la lógica operativa.

---

# 4. Qué casos entran

Entran aquí los casos cuya situación dominante sea compatible con:

- `EstadoActa = ACTO_ADMINISTRATIVO_EN_PROCESO`

o casos que ya ingresaron al circuito de preparación y formalización del acto administrativo correspondiente.

Típicamente llegan aquí desde:

- `Pendiente de acto administrativo`

---

# 5. Qué se espera resolver aquí

En esta bandeja se espera resolver si el caso:

- tiene su acto administrativo preparado
- requiere redacción o ajuste del acto
- requiere generación documental
- requiere envío a firma
- ya obtuvo firma
- requiere reemplazo o invalidación documental
- requiere volver a análisis por problema detectado
- queda listo para pasar al tramo de notificación del acto
- debe paralizarse

En términos simples: aquí se produce y formaliza el acto administrativo.

---

# 6. Acciones posibles

## 6.1 Preparar o redactar acto administrativo
- **Evento o proceso que dispara:** `ACTO_ADMINISTRATIVO_GENERADO` o generación documental equivalente, según validación con el área
- **Resultado esperado:** el acto administrativo queda producido o actualizado en forma documental
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.2 Generar documento del acto
- **Evento o proceso que dispara:** `DOCUMENTO_INCORPORADO`
- **Resultado esperado:** se incorpora el documento principal del acto administrativo
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.3 Enviar documento a firma
- **Evento o proceso que dispara:** cambio al circuito documental de firma; puede acompañarse de estado documental `PENDIENTE_FIRMA`
- **Resultado esperado:** el documento queda pendiente de firma del funcionario correspondiente
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.4 Registrar documento firmado
- **Evento o proceso que dispara:** `ACTO_ADMINISTRATIVO_FIRMADO`
- **Resultado esperado:** el acto administrativo queda firmado y formalmente apto para avanzar
- **Destino:** `Pendiente de notificación del acto`

## 6.5 Observar documento del acto
- **Evento o proceso que dispara:** documento en estado `OBSERVADO` y/o tratamiento equivalente, a validar con el área
- **Resultado esperado:** el documento requiere corrección, ajuste o revisión
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.6 Reemplazar documento del acto
- **Evento o proceso que dispara:** `DOCUMENTO_REEMPLAZADO`
- **Resultado esperado:** se sustituye el documento del acto por una nueva versión válida
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.7 Invalidar documento del acto
- **Evento o proceso que dispara:** `DOCUMENTO_INVALIDADO` o `ACTO_ADMINISTRATIVO_INVALIDADO`
- **Resultado esperado:** la pieza documental previa deja de ser válida y el caso sigue en trabajo resolutivo
- **Destino:** sigue en `Acto administrativo en proceso`

## 6.8 Volver a análisis
- **Evento o proceso que dispara:** a validar con el área; podría resolverse por observación, nueva presentación o detección de problema material
- **Resultado esperado:** se detecta que el caso no estaba todavía en condiciones de completar el acto
- **Destino:** `Análisis / presentaciones / pagos`

## 6.9 Paralizar
- **Evento o proceso que dispara:** `PARALIZACION_DISPUESTA`
- **Resultado esperado:** el caso sale del circuito activo normal
- **Destino:** `Paralizadas`

---

# 7. Excepciones

Esta bandeja puede tener excepciones o cuestiones a validar:

- puede haber más de una versión del documento del acto
- puede haber observaciones y reemplazos antes de llegar a firma
- distintos tipos de acto podrían requerir distintos recorridos documentales
- debe validarse si todo acto firmado sale necesariamente a pendiente de notificación del acto
- debe validarse si el retorno a análisis desde esta etapa existe en la práctica y con qué frecuencia
- debe validarse si hay más de un documento resolutivo relevante dentro de esta misma estación

---

# 8. Puntos a validar con Dirección de Faltas

- ¿Esta bandeja existe realmente como estación operativa separada?
- ¿Aquí se redacta y produce el acto administrativo?
- ¿Aquí se gestionan observaciones, reemplazos y firma del acto?
- ¿Todo acto administrativo firmado sale desde aquí hacia notificación del acto?
- ¿Puede volver a análisis desde esta etapa?
- ¿Hay más de un documento importante dentro de este tramo?
- ¿Falta alguna acción relevante propia de esta estación?

---

# 9. Resultado esperado de validación

Después de validar esta bandeja debería quedar claro:

- si `ACTO_ADMINISTRATIVO_EN_PROCESO` alcanza como estado dominante para esta estación
- si aquí vive efectivamente la preparación documental y formal del acto
- si las acciones de firma, observación y reemplazo están correctamente representadas
- si la salida natural al firmarse el acto es `Pendiente de notificación del acto`
- si el modelo documental y los catálogos actuales alcanzan para representar correctamente esta etapa

# ÍNDICE MAESTRO DE BANDEJAS

> Documento de navegación rápida del bloque `spec/05-bandejas/`.
>  
> Su objetivo es resumir:
> - qué hace cada bandeja o proceso
> - cómo entra un caso
> - qué acciones principales existen
> - y a qué destino lleva cada acción
>
> No reemplaza a los documentos específicos de cada bandeja.

---

## 01 — Bandeja Labradas
**Función:** primera bandeja operativa para actas recién labradas.

**Entra por:**
- acta labrada

**Acciones posibles:**
- enviar a enriquecimiento → `02-bandeja-enriquecimiento.md`
- enviar a preparación para notificación del acta → `03-bandeja-preparacion-para-notificacion-acta.md`
- cerrar administrativamente → `20-bandeja-cerradas.md`

---

## 02 — Bandeja Enriquecimiento
**Función:** completar, corregir o incorporar información necesaria antes de continuar.

**Entra por:**
- derivación desde `01-bandeja-labradas.md`

**Acciones posibles:**
- terminar enriquecimiento → `03-bandeja-preparacion-para-notificacion-acta.md`
- devolver a revisión inicial → `01-bandeja-labradas.md`
- cerrar administrativamente → `20-bandeja-cerradas.md`

---

## 03 — Bandeja Preparación para Notificación del Acta
**Función:** generar y completar la documentación previa necesaria para notificar el acta.

**Entra por:**
- derivación desde `01-bandeja-labradas.md`
- derivación desde `02-bandeja-enriquecimiento.md`

**Acciones posibles:**
- generar documentación faltante → permanece en `03-bandeja-preparacion-para-notificacion-acta.md`
- enviar documentos a firma → permanece en `03-bandeja-preparacion-para-notificacion-acta.md`
- completar firmas requeridas → `04-bandeja-actas-listas-para-notificar.md`
- corregir documentación observada → permanece en `03-bandeja-preparacion-para-notificacion-acta.md`
- cerrar administrativamente → `20-bandeja-cerradas.md`

---

## 04 — Bandeja Actas Listas para Notificar
**Función:** cola real de salida para emitir la notificación del acta.

**Entra por:**
- documentación previa completa y firmada desde `03-bandeja-preparacion-para-notificacion-acta.md`

**Acciones posibles:**
- emitir notificación del acta → `05-bandeja-notificacion-acta-en-proceso.md`
- ajustar canal de notificación → permanece en `04-bandeja-actas-listas-para-notificar.md`
- devolver a preparación previa por incidencia → `03-bandeja-preparacion-para-notificacion-acta.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 05 — Bandeja Notificación del Acta en Proceso
**Función:** seguimiento de la notificación del acta hasta obtener resultado suficiente.

**Entra por:**
- emisión de notificación desde `04-bandeja-actas-listas-para-notificar.md`

**Acciones posibles:**
- registrar acuse positivo / notificación fehaciente → `11-bandeja-actas-notificadas.md`
- registrar rechazo / devolución / imposibilidad → permanece en `05-bandeja-notificacion-acta-en-proceso.md`
- reintentar notificación → permanece en `05-bandeja-notificacion-acta-en-proceso.md`
- corregir datos operativos → permanece en `05-bandeja-notificacion-acta-en-proceso.md`
- escalar incidencia → bandeja que corresponda
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 06 — Bandeja Análisis / Presentaciones / Pagos
**Función:** tratar presentaciones, pagos, novedades e incidencias que requieren análisis administrativo.

**Entra por:**
- derivación desde distintas bandejas activas
- reingreso desde archivo por desarchivo
- reingreso desde otras situaciones que exigen nuevo análisis

**Acciones posibles:**
- resolver y volver al circuito principal → bandeja que corresponda
- incorporar presentación o pago → permanece en `06-bandeja-analisis-presentaciones-pagos.md`
- derivar a pendiente de acto administrativo → `07-bandeja-pendiente-acto-administrativo.md`
- derivar a pendiente de fallo → `12-bandeja-pendiente-de-fallo.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 07 — Bandeja Pendiente de Acto Administrativo
**Función:** casos que requieren un acto administrativo previo antes de continuar.

**Entra por:**
- derivación desde análisis u otras bandejas que requieran decisión administrativa previa

**Acciones posibles:**
- generar acto administrativo → `08-bandeja-acto-administrativo-en-proceso.md`
- reenviar a análisis → `06-bandeja-analisis-presentaciones-pagos.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 08 — Bandeja Acto Administrativo en Proceso
**Función:** redacción, preparación y formalización del acto administrativo.

**Entra por:**
- derivación desde `07-bandeja-pendiente-acto-administrativo.md`

**Acciones posibles:**
- terminar acto administrativo → `09-bandeja-pendiente-notificacion-acto.md`
- corregir / rehacer acto → permanece en `08-bandeja-acto-administrativo-en-proceso.md`
- reenviar a análisis → `06-bandeja-analisis-presentaciones-pagos.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 09 — Bandeja Pendiente Notificación del Acto
**Función:** acto ya generado, pendiente de entrar al circuito de notificación.

**Entra por:**
- finalización desde `08-bandeja-acto-administrativo-en-proceso.md`

**Acciones posibles:**
- enviar a notificación del acto → `10-bandeja-notificacion-acto-en-proceso.md`
- ajustar documentación previa → `08-bandeja-acto-administrativo-en-proceso.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 10 — Bandeja Notificación del Acto en Proceso
**Función:** seguimiento de la notificación del acto hasta obtener notificación fehaciente.

**Entra por:**
- emisión de notificación desde `09-bandeja-pendiente-notificacion-acto.md`

**Acciones posibles:**
- registrar acuse positivo / notificación fehaciente → `11-bandeja-actas-notificadas.md`
- registrar rechazo / devolución / imposibilidad → permanece en `10-bandeja-notificacion-acto-en-proceso.md`
- reintentar notificación → permanece en `10-bandeja-notificacion-acto-en-proceso.md`
- corregir datos operativos → permanece en `10-bandeja-notificacion-acto-en-proceso.md`
- escalar incidencia → bandeja que corresponda
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 11 — Bandeja Actas Notificadas
**Función:** espera controlada posterior a la notificación fehaciente del acto.

**Entra por:**
- acuse positivo / notificación fehaciente desde `10-bandeja-notificacion-acto-en-proceso.md`

**Acciones posibles:**
- mantener en espera → permanece en `11-bandeja-actas-notificadas.md`
- vencer plazo configurado → `12-bandeja-pendiente-de-fallo.md`
- pasar manualmente a pendiente de fallo → `12-bandeja-pendiente-de-fallo.md`
- registrar presentación / descargo / pago voluntario → `06-bandeja-analisis-presentaciones-pagos.md`
- derivar por decisión administrativa → bandeja que corresponda
- cerrar → `20-bandeja-cerradas.md`
- paralizar → `19-bandeja-paralizadas.md`

---

## 12 — Bandeja Pendiente de Fallo
**Función:** casos listos para generar fallo.

**Entra por:**
- vencimiento del plazo desde `11-bandeja-actas-notificadas.md`
- pase manual a pendiente de fallo

**Acciones posibles:**
- generar fallo y documentación necesaria → `13-bandeja-fallo-pendiente-de-notificacion.md`
- registrar pago voluntario → `06-bandeja-analisis-presentaciones-pagos.md`
- registrar descargo o presentación → `06-bandeja-analisis-presentaciones-pagos.md`
- mantener en pendiente de fallo → permanece en `12-bandeja-pendiente-de-fallo.md`
- reenviar a análisis → `06-bandeja-analisis-presentaciones-pagos.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 13 — Bandeja Fallo Pendiente de Notificación
**Función:** fallo generado, pero aún falta completar documentación y/o firmas previas a la notificación del fallo.

**Entra por:**
- generación de fallo desde `12-bandeja-pendiente-de-fallo.md`

**Acciones posibles:**
- generar documentación faltante → permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`
- confirmar documentación → permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`
- enviar documentos a firma → permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`
- completar firmas requeridas → `14-bandeja-fallos-listos-para-notificar.md`
- corregir documentación observada → permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`
- rehacer o ajustar fallo → `12-bandeja-pendiente-de-fallo.md` o permanece en `13-bandeja-fallo-pendiente-de-notificacion.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 14 — Bandeja Fallos Listos para Notificar
**Función:** cola real de salida para emitir la notificación del fallo.

**Entra por:**
- documentación y firmas completas desde `13-bandeja-fallo-pendiente-de-notificacion.md`

**Acciones posibles:**
- emitir notificación del fallo → `15-bandeja-notificacion-fallo-en-proceso.md`
- ajustar canal de notificación → permanece en `14-bandeja-fallos-listos-para-notificar.md`
- devolver a preparación previa por incidencia → `13-bandeja-fallo-pendiente-de-notificacion.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 15 — Bandeja Notificación del Fallo en Proceso
**Función:** seguimiento de la notificación del fallo hasta obtener resultado suficiente.

**Entra por:**
- emisión de notificación desde `14-bandeja-fallos-listos-para-notificar.md`

**Acciones posibles:**
- registrar acuse positivo / notificación fehaciente del fallo → `16-bandeja-fallos-notificados.md`
- registrar rechazo / devolución / imposibilidad → permanece en `15-bandeja-notificacion-fallo-en-proceso.md`
- reintentar notificación → permanece en `15-bandeja-notificacion-fallo-en-proceso.md`
- corregir datos operativos → permanece en `15-bandeja-notificacion-fallo-en-proceso.md`
- escalar incidencia → bandeja que corresponda
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 16 — Bandeja Fallos Notificados
**Función:** espera controlada posterior a la notificación fehaciente del fallo.

**Entra por:**
- acuse positivo / notificación fehaciente desde `15-bandeja-notificacion-fallo-en-proceso.md`

**Acciones posibles:**
- mantener en espera → permanece en `16-bandeja-fallos-notificados.md`
- vencer plazo configurado sin otra actuación → `18-bandeja-gestion-externa.md`
- pasar manualmente a gestión externa → `18-bandeja-gestion-externa.md`
- registrar apelación → `17-bandeja-con-apelacion.md`
- registrar pago / presentación / novedad → `06-bandeja-analisis-presentaciones-pagos.md` o bandeja que corresponda
- derivar por decisión administrativa → bandeja que corresponda
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 17 — Bandeja Con Apelación
**Función:** tratar únicamente los casos que tienen apelación efectivamente presentada.

**Entra por:**
- apelación presentada luego del fallo notificado desde `16-bandeja-fallos-notificados.md`

**Acciones posibles:**
- analizar apelación → permanece en `17-bandeja-con-apelacion.md`
- derivar / elevar apelación → circuito que corresponda
- registrar resultado de apelación → bandeja que corresponda según resultado
- mantener en trámite de apelación → permanece en `17-bandeja-con-apelacion.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 18 — Bandeja Gestión Externa
**Función:** derivación a instancia externa y tratamiento del eventual reingreso.

**Entra por:**
- vencimiento del plazo desde `16-bandeja-fallos-notificados.md`
- pase manual a gestión externa
- decisión administrativa desde otras bandejas
- caso ya derivado en trámite externo
- reingreso desde instancia externa

**Acciones posibles:**
- definir destino externo → permanece en `18-bandeja-gestion-externa.md`
- formalizar derivación externa → permanece en `18-bandeja-gestion-externa.md`
- registrar trámite o novedad externa → permanece en `18-bandeja-gestion-externa.md`
- registrar reingreso con resultado conclusivo → `20-bandeja-cerradas.md` o circuito final que corresponda
- registrar reingreso con actuaciones / documentos / evidencias → permanece en `18-bandeja-gestion-externa.md` hasta definir destino interno
- reingresar para rectificar / modificar / emitir nuevo fallo → `12-bandeja-pendiente-de-fallo.md`
- mantener en trámite externo → permanece en `18-bandeja-gestion-externa.md`
- paralizar → `19-bandeja-paralizadas.md`
- cerrar → `20-bandeja-cerradas.md`

---

## 19 — Bandeja Paralizadas
**Función:** expedientes detenidos por una causa fundada, sin cierre.

**Entra por:**
- paralización desde cualquier bandeja activa

**Acciones posibles:**
- mantener paralización → permanece en `19-bandeja-paralizadas.md`
- actualizar motivo o novedad de paralización → permanece en `19-bandeja-paralizadas.md`
- reactivar expediente → bandeja que corresponda según punto de reingreso
- derivar a otra bandeja por decisión administrativa → bandeja que corresponda
- cerrar desde paralización → `20-bandeja-cerradas.md`

---

## 20 — Bandeja Cerradas
**Función:** última bandeja operativa para expedientes finalizados administrativamente.

**Entra por:**
- cierre desde cualquier bandeja activa
- resultado externo conclusivo
- decisión administrativa de cierre

**Acciones posibles:**
- registrar cierre → permanece en `20-bandeja-cerradas.md`
- registrar motivo de cierre → permanece en `20-bandeja-cerradas.md`
- incorporar constancia final → permanece en `20-bandeja-cerradas.md`
- consultar expediente cerrado → permanece en `20-bandeja-cerradas.md`
- enviar a archivo → `21-proceso-archivo.md`

---

## 21 — Proceso Archivo
**Función:** conservación posterior del expediente ya cerrado.

**Entra por:**
- expediente cerrado desde `20-bandeja-cerradas.md`

**Acciones posibles:**
- marcar expediente como archivado → archivo
- mantener expediente cerrado sin archivar todavía → `20-bandeja-cerradas.md`
- archivar por lote o antigüedad → archivo
- consolidar documentación final antes del archivo → `20-bandeja-cerradas.md` o archivo
- consultar expediente archivado → archivo
- desarchivar expediente → `06-bandeja-analisis-presentaciones-pagos.md`

---

# Lectura rápida del conjunto

## Flujo principal
- `01` → `02` / `03` → `04` → `05` → `11` → `12` → `13` → `14` → `15` → `16` → `18`

## Ramas no lineales
- `06` análisis / presentaciones / pagos
- `17` con apelación
- `19` paralizadas
- `20` cerradas
- `21` archivo

## Reingresos importantes
- gestión externa con necesidad de nuevo fallo → `12-bandeja-pendiente-de-fallo.md`
- desarchivo → `06-bandeja-analisis-presentaciones-pagos.md`
- reactivación desde paralizadas → bandeja que corresponda
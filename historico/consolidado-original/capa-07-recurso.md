# [CAPA 07] — RECURSO / APELACIÓN CONSOLIDADO

---

# 1. FINALIDAD DE LA CAPA

Esta capa modela la **interposición y tratamiento del recurso de apelación** dentro del recorrido del Acta.

Su objetivo es:

* registrar la existencia del recurso
* reflejar sus efectos en el recorrido del Acta
* soportar documentación asociada
* proyectar su estado actual en el snapshot

---

# 2. PRINCIPIO CENTRAL

El recurso:

## no se modela como agregado complejo ni como expediente paralelo

Se representa mediante:

## ActaEvento + Documento + Snapshot

---

# 3. MODELO DEL RECURSO

## 3.1 Evento

La apelación y sus hitos relevantes se registran como eventos en `ActaEvento`.

---

### Ejemplos de eventos

* APELACION_PRESENTADA
* APELACION_CONCEDIDA
* APELACION_RECHAZADA
* APELACION_ELEVADA
* RESULTADO_APELACION_RECIBIDO

---

## Regla

Solo se registran hitos procesales relevantes.

No se registran:

* aperturas internas de trámite
* pasos administrativos sin efecto real
* operaciones técnicas

---

## 3.2 Documento

Si el recurso tiene soporte documental, se representa mediante:

* Documento
* ActaDocumento

---

### Ejemplos

* escrito de apelación
* resolución sobre concesión o rechazo
* resultado judicial o administrativo recibido

---

## 3.3 Snapshot

El sistema puede proyectar:

* TieneApelacionAbierta
* ApelacionConcedida
* ApelacionElevada
* ResultadoApelacionIncorporado

---

# 4. DATOS DEL RECURSO

Los datos del recurso se distribuyen así:

## En el evento

* tipo de hito recursivo
* fecha
* origen o actor relevante
* observación resumida

---

## En el documento

* contenido completo
* presentación firmada
* decisión documentada
* resultado recibido

---

## En el snapshot

* banderas y estado operativo actual del recurso

---

# 5. SATÉLITE OPCIONAL MÍNIMO

## Regla

No se crea por defecto una entidad `ActaRecurso`.

---

## Excepción posible

Solo podría incorporarse un satélite mínimo si, al bajar a implementación, se demuestra estrictamente necesario para conservar metadatos no convenientes de dejar solo en evento/documento.

---

### Ejemplos de metadatos que podrían justificarlo

* carácter del presentante
* tipo de presentante
* referencia externa puntual
* algún dato resumido no documental de consulta frecuente

---

## Condición

Si ese satélite existe, debe ser:

* mínimo
* sin ciclo de vida propio complejo
* sin submodelo documental
* sin reemplazar eventos ni snapshot

---

# 6. RELACIÓN CON EL PROCESO

La apelación:

* forma parte del viaje del Acta
* puede alterar su recorrido
* puede derivar en elevación
* puede producir un resultado que impacte el estado actual

Pero no crea un dominio independiente del Acta.

---

# 7. REGLAS DE DISEÑO

## 7.1 No crear expediente paralelo

La apelación no se modela como proceso autónomo separado del Acta.

---

## 7.2 No duplicar información

No repetir datos entre:

* evento
* documento
* snapshot
* eventual satélite mínimo

---

## 7.3 El evento cuenta el recorrido

La secuencia recursiva debe poder reconstruirse leyendo `ActaEvento`.

---

# 8. RESULTADO DE LA CAPA

Esta capa permite:

* registrar correctamente la apelación
* mantener el modelo simple
* conservar el soporte documental
* reflejar su estado actual en snapshot
* evitar un agregado recursivo sobredimensionado

---

## Resumen

La apelación es:

## una parte del viaje del Acta

## no un expediente independiente

Se representa como:

## evento + documento + proyección

y solo excepcionalmente admite un satélite mínimo de metadatos.

---

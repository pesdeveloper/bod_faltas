# ActaEvento

## Finalidad

ActaEvento es la pieza de dominio que registra la trazabilidad del expediente.

Su función es dejar constancia de los hechos, decisiones, transiciones y resultados relevantes que afectan la evolución del Acta a lo largo de su ciclo de vida.

---

## Qué representa en el sistema

ActaEvento representa un hecho relevante ocurrido sobre el expediente.

Ese hecho puede corresponder, según el caso, a:

- una acción humana
- una decisión administrativa
- una transición operativa
- un resultado de proceso
- una recepción o confirmación externa
- una incidencia que altera el curso esperado del expediente

ActaEvento constituye la memoria trazable del sistema.

---

## Relación con el expediente

Todo ActaEvento existe en función de un Acta.

No tiene sentido como entidad aislada: siempre expresa algo que ocurrió, fue decidido, fue recibido, fue emitido o fue constatado respecto de un expediente concreto.

El expediente evoluciona por la acumulación ordenada de eventos relevantes.

---

## Reglas principales

- El historial del expediente vive en ActaEvento.
- El modelo de eventos es la base de la trazabilidad y auditoría funcional del caso.
- No todo dato operativo del sistema es un evento; solo deben registrarse como ActaEvento los hechos con valor de negocio, trazabilidad o reconstrucción.
- El evento no reemplaza al documento asociado, pero puede dejar constancia de su generación, firma, emisión, recepción, rechazo, resultado o efecto.
- El evento no reemplaza al snapshot; el snapshot se deriva del conjunto de eventos y demás piezas del expediente.
- El expediente puede reingresar, desviarse, paralizarse, archivarse o reactivarse mediante eventos, sin requerir un flujo estrictamente lineal.

---

## Qué no es

ActaEvento no es:

- una bandeja
- un estado calculado final
- un snapshot
- una simple bitácora técnica sin valor funcional
- un documento en sí mismo
- una notificación en sí misma

Tampoco debe usarse para duplicar información que ya exista sin aportar trazabilidad real.

---

## Qué tipo de hechos registra

ActaEvento debe registrar, entre otros, hechos como:

- creación o incorporación relevante del expediente
- validaciones, observaciones, anulaciones o cambios de curso
- generación, firma, emisión o recepción de documentos
- inicio, resultado o novedad relevante de notificaciones
- imposición, levantamiento o resultado de medidas
- presentaciones, comparecencias, pagos, apelaciones o decisiones
- derivaciones, reingresos, cierres, archivos o reaperturas

La lista concreta de tipos de evento pertenece a los catálogos y reglas operativas del sistema, no a esta definición conceptual.

---

## Relación con snapshot y operación

Los eventos no existen para facilitar directamente la operación diaria por bandejas, sino para preservar la historia confiable del expediente.

La operación diaria puede apoyarse en snapshots, vistas o proyecciones derivadas, pero esas proyecciones deben poder justificarse a partir de los eventos y de las demás piezas formales del expediente.

---

## Relaciones clave

ActaEvento se relaciona conceptualmente con:

- Acta, como expediente al que pertenece
- Documento, cuando el hecho registrado involucra producción o resultado documental
- Notificacion, cuando el hecho registrado refleja emisión, acuse, rechazo, resultado o incidencia de notificación
- Snapshot operativo, que puede derivarse parcial o totalmente a partir de eventos
- Procesos externos o integraciones, cuando generan resultados relevantes sobre el expediente
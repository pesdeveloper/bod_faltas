# Mocks funcionales demo – Faltas

## Estado: 37 mocks canónicos (reducción de 116 → 52 → 37)

---

## Circuito tránsito canónico (ACTA-0001 a ACTA-0008)

| ID        | Bandeja                       | Estado / Descripción                               |
|-----------|-------------------------------|-----------------------------------------------------|
| ACTA-0001 | ACTAS_EN_ENRIQUECIMIENTO      | Captura inicial – enriquecimiento en curso          |
| ACTA-0002 | ACTAS_EN_ENRIQUECIMIENTO      | Enriquecimiento completo – listo para documental    |
| ACTA-0003 | PENDIENTE_FIRMA               | Borrador generado – pendiente de firma              |
| ACTA-0004 | EN_NOTIFICACION               | Acta firmada – notificación inicial correo postal   |
| ACTA-0005 | PENDIENTE_ANALISIS            | Notificación positiva – pendiente de fallo          |
| ACTA-0006 | PENDIENTE_ANALISIS            | Fallo absolutorio dictado – absuelto                |
| ACTA-0007 | ARCHIVO                       | En archivo                                          |
| ACTA-0008 | CERRADAS                      | Cerrada definitivamente                             |

---

## Mocks con dependencias de tests (ACTA-0013 a ACTA-0026)

| ID        | Bandeja                            | Estado / Descripción                                               |
|-----------|------------------------------------|--------------------------------------------------------------------|
| ACTA-0013 | PENDIENTES_RESOLUCION_REDACCION    | Pendiente medida preventiva – pago voluntario disponible          |
| ACTA-0015 | PENDIENTE_ANALISIS (sub-bandeja)   | Sub-bandeja dinámica – dependencia de SubBandejaDinamicaIT        |
| ACTA-0016 | PENDIENTE_ANALISIS                 | Sin pago – pago voluntario disponible                             |
| ACTA-0017 | PENDIENTE_ANALISIS                 | Pago voluntario informado – dependencia de tests                  |
| ACTA-0020 | PENDIENTE_FIRMA                    | Origen medida desde generarMedida – piezas requeridas resueltas   |
| ACTA-0021 | (cerrabilidad)                     | Pago confirmado precargado – material de cierre sin circuito      |
| ACTA-0024 | ACTAS_EN_ENRIQUECIMIENTO           | Constatación material temprana con anclas y condiciones           |
| ACTA-0026 | PENDIENTE_ANALISIS                 | Medida preventiva posterior a contravención                       |

---

## Fallo / Apelación (ACTA-0027, 0028, 0030)

| ID        | Bandeja            | Estado / Descripción                                               |
|-----------|--------------------|--------------------------------------------------------------------|
| ACTA-0027 | PENDIENTE_ANALISIS | Fallo condenatorio – apelación vía portal (inicio limpio)         |
| ACTA-0028 | PENDIENTE_ANALISIS | Fallo condenatorio – apelación presencial (inicio limpio)         |
| ACTA-0030 | PENDIENTE_ANALISIS | Fallo condenatorio – apelación resuelta ACEPTADA_ABSUELVE         |

> ACTA-0029 (vencimiento plazo) removido. El circuito de condena firme por vencimiento
> queda cubierto por ACTA-0122 y por FalloYPlazoApelacionIT que prepara estado inline.

---

## Canal: Notificador Municipal (ACTA-0031, 0032)

| ID        | Canal                 | Tipo notificación      |
|-----------|-----------------------|------------------------|
| ACTA-0031 | NOTIFICADOR_MUNICIPAL | FALLO_CONDENATORIO     |
| ACTA-0032 | NOTIFICADOR_MUNICIPAL | FALLO_ABSOLUTORIO      |

> ACTA-0033 (ACTA_INFRACCION por notificador) removido. Los dos tipos de fallo
> son el caso de uso principal del canal; la cobertura de ACTA_INFRACCION queda
> en los tests de integración del canal.

---

## Canal: Portal / Domicilio Electrónico (ACTA-0034, 0035, 0036)

| ID        | Canal                        | Tipo / Estado                          |
|-----------|------------------------------|----------------------------------------|
| ACTA-0034 | PORTAL_DOMICILIO_ELECTRONICO | FALLO_CONDENATORIO – preparada         |
| ACTA-0035 | PORTAL_DOMICILIO_ELECTRONICO | FALLO_ABSOLUTORIO – preparada          |
| ACTA-0036 | PORTAL_DOMICILIO_ELECTRONICO | ACTA_INFRACCION – preparada            |

---

## Canal: Correo Postal (ACTA-0037 a ACTA-0045)

### Candidatas para lote (5 notificaciones — CANDIDATAS_CORREO_BASE = 5)

| ID        | Tipo notificación  | NOT ID      |
|-----------|--------------------|-------------|
| ACTA-0004 | ACTA_INFRACCION    | NOT-0004-01 |
| ACTA-0037 | FALLO_CONDENATORIO | NOT-0037-01 |
| ACTA-0038 | ACTA_INFRACCION    | NOT-0038-01 |
| ACTA-0039 | FALLO_CONDENATORIO | NOT-0039-01 |
| ACTA-0040 | FALLO_ABSOLUTORIO  | NOT-0040-01 |

> ACTA-0041 (segunda candidata ACTA_INFRACCION) removida por redundancia.

### No candidatas – estados post-envío

| ID        | Estado   | Resultado | Descripción                          |
|-----------|----------|-----------|--------------------------------------|
| ACTA-0044 | NEGATIVA | NEGATIVA  | Correo devuelto – resultado negativo  |
| ACTA-0045 | VENCIDA  | VENCIDA   | Sin respuesta dentro del plazo        |

> ACTA-0042 (ENVIADA pendiente) y ACTA-0043 (ENTREGADA positiva) removidos.
> Los estados positivo y enviado quedan cubiertos por flujo dinámico en tests.

---

## Bandeja UX: paralizada (ACTA-0114)

| ID        | Bandeja    | Motivo paralización                |
|-----------|------------|------------------------------------|
| ACTA-0114 | PARALIZADAS | Espera de documentación probatoria |

> ACTA-0108 a ACTA-0113 (pendientes fallo y con apelación UX) removidos: redundantes
> con ACTA-0027/0028/0030 del circuito funcional.
> ACTA-0115 y ACTA-0116 removidos: ACTA-0114 cubre el estado canónico de paralizada.

---

## Nuevos mocks canónicos – hitos faltantes (ACTA-0120 a ACTA-0125)

| ID        | Bandeja / Estado                     | Descripción                                        |
|-----------|--------------------------------------|----------------------------------------------------|
| ACTA-0120 | PENDIENTE_ANALISIS / PAGO_INFORMADO  | Pago voluntario informado – pendiente verificación |
| ACTA-0121 | PENDIENTE_FIRMA                      | Fallo pendiente firma                              |
| ACTA-0122 | GESTION_EXTERNA / CONDENA_FIRME      | Condena firme – pago condena pendiente             |
| ACTA-0123 | ACTAS_EN_ENRIQUECIMIENTO             | Dependencia inicial – INSPECCIONES                 |
| ACTA-0124 | ACTAS_EN_ENRIQUECIMIENTO             | Dependencia inicial – FISCALIZACION                |
| ACTA-0125 | ACTAS_EN_ENRIQUECIMIENTO             | Dependencia inicial – BROMATOLOGIA                 |

---

## Resumen de cobertura canónica

| Estado canónico                       | Mock representativo |
|---------------------------------------|---------------------|
| Captura inicial                       | ACTA-0001           |
| Preparación documental                | ACTA-0002           |
| Firma acta                            | ACTA-0003           |
| Notificación inicial                  | ACTA-0004           |
| Notificación positiva / pendiente fallo | ACTA-0005         |
| Fallo absolutorio                     | ACTA-0006           |
| Pago voluntario informado             | ACTA-0120           |
| Pendiente fallo (redacción)           | ACTA-0013           |
| Fallo pendiente firma                 | ACTA-0121           |
| Fallo pendiente notificación          | ACTA-0027           |
| Apelación                             | ACTA-0028           |
| Condena firme / pago condena          | ACTA-0122           |
| Gestión externa                       | ACTA-0122           |
| Paralizada                            | ACTA-0114           |
| Archivo                               | ACTA-0007           |
| Cerrada                               | ACTA-0008           |
| Canal correo postal (candidata)       | ACTA-0037           |
| Canal notificador municipal           | ACTA-0031           |
| Canal portal domicilio electrónico    | ACTA-0034           |
| Dependencia INSPECCIONES              | ACTA-0123           |
| Dependencia FISCALIZACION             | ACTA-0124           |
| Dependencia BROMATOLOGIA              | ACTA-0125           |

---

## Mocks removidos en esta reducción (52 → 37)

| ID        | Motivo de remoción                                                          |
|-----------|-----------------------------------------------------------------------------|
| ACTA-0018 | Redundante con ACTA-0001 para pago voluntario en enriquecimiento            |
| ACTA-0025 | Solo para test de constatación; adaptado a ACTA-0001 en ConstatacionIT      |
| ACTA-0029 | Fallo+plazo intercambiable; condena firme cubierta por ACTA-0122            |
| ACTA-0033 | Notificador municipal ACTA_INFRACCION redundante                            |
| ACTA-0041 | Segunda candidata correo postal ACTA_INFRACCION redundante                  |
| ACTA-0042 | Correo ENVIADA pendiente – estado cubierto dinámicamente en tests           |
| ACTA-0043 | Correo ENTREGADA positiva – estado cubierto dinámicamente en tests          |
| ACTA-0108 | UX pendiente fallo condenatorio – cubierto por ACTA-0005/0027               |
| ACTA-0109 | UX pendiente fallo absolutorio – cubierto por ACTA-0006                     |
| ACTA-0110 | UX fallo tras pago – cubierto por ACTA-0120                                 |
| ACTA-0111 | UX apelación pendiente resolución – cubierto por ACTA-0028                  |
| ACTA-0112 | UX apelación en revisión – cubierto por ACTA-0028                           |
| ACTA-0113 | UX apelación resuelta ABSUELVE – cubierto por ACTA-0030                     |
| ACTA-0115 | Paralizada tramite externo – ACTA-0114 cubre el estado canónico             |
| ACTA-0116 | Paralizada causa administrativa – ACTA-0114 cubre el estado canónico        |

---

## Tests adaptados en esta reducción

| Test                               | Cambio                                                              |
|------------------------------------|---------------------------------------------------------------------|
| ConstatacionMaterialTempranaEtapaIT | ACTA-0025 → ACTA-0001; eventos 3→5, docs 1→2; rm $.message en 409 |
| ActasMockFalloApelacionDemoIT       | Removida referencia a ACTA-0029 (ACTA_VENCIMIENTO_PLAZO)          |
| CorreoPostalNotificacionIT          | CANDIDATAS_CORREO_BASE 6→5; rm NOT-0029-03 de RESPUESTA_DEMO_BASE |
| NotificadorMunicipalNotificacionIT  | Removida aserción de NOT-0033-01                                   |

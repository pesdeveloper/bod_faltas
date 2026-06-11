# Decisiones de dominio — Dirección de Faltas (prototipo)

---

## Decisión: Situación general de pago y circuitos de pago

**Fecha:** 2026-06-09

### Decisión

- `situacionPago` representa el **estado general de pago** del expediente.
- `situacionPagoCondena` representa el **estado específico del circuito de pago de condena**.
- `tipoPago` indica el **origen/circuito** del pago: `VOLUNTARIO`, `CONDENA`, o `NO_APLICA`.
- Cuando se informa o confirma un pago de condena, `situacionPago` debe reflejar que existe un pago en curso o confirmado.
- Un expediente con `situacionPagoCondena=CONFIRMADO` **no debe mostrarse como `situacionPago=SIN_PAGO`**.
- `montoCondena` puede conservarse como dato histórico, pero no implica por sí solo deuda vigente ni pago confirmado. El estado manda.

### Regla por estado

| situacionPagoCondena | situacionPago general  | tipoPago   | Notas                                             |
|----------------------|------------------------|------------|---------------------------------------------------|
| NO_APLICA            | SIN_PAGO               | NO_APLICA  | Sin ningún pago en circuito alguno                |
| PENDIENTE            | SIN_PAGO               | NO_APLICA  | Condena firme pero sin pago informado aún         |
| INFORMADO            | PENDIENTE_CONFIRMACION | CONDENA    | Pago de condena informado, pendiente confirmación |
| CONFIRMADO           | CONFIRMADO             | CONDENA    | Pago de condena confirmado; cerrable              |
| OBSERVADO            | OBSERVADO              | CONDENA    | Pago observado; se puede reintentar               |

| Circuito voluntario  | situacionPago general  | tipoPago   | Notas                                             |
|----------------------|------------------------|------------|---------------------------------------------------|
| Solicitud registrada | SOLICITADO             | VOLUNTARIO | Infractor o Dirección inicia solicitud            |
| Pago informado       | PENDIENTE_CONFIRMACION | VOLUNTARIO | Infractor informa pago; Dirección confirma        |
| Pago confirmado      | CONFIRMADO             | VOLUNTARIO | resultadoFinal=PAGO_CONFIRMADO                    |

### Ejemplos

Pago voluntario confirmado:
  situacionPago: CONFIRMADO
  tipoPago: VOLUNTARIO
  situacionPagoCondena: NO_APLICA
  resultadoFinal: PAGO_CONFIRMADO

Pago condena informado:
  situacionPago: PENDIENTE_CONFIRMACION
  tipoPago: CONDENA
  situacionPagoCondena: INFORMADO
  resultadoFinal: CONDENA_FIRME

Pago condena confirmado:
  situacionPago: CONFIRMADO
  tipoPago: CONDENA
  situacionPagoCondena: CONFIRMADO
  resultadoFinal: CONDENA_FIRME

ACTA-0030 cerrada por condena consentida + pago confirmado (estado final esperado):
  bandejaActual: CERRADAS
  resultadoFinal: CONDENA_FIRME
  situacionPago: CONFIRMADO
  tipoPago: CONDENA
  situacionPagoCondena: CONFIRMADO
  montoCondena: 3500
  estaCerrada: true
  permiteReingreso: false

### Motivación

Evitar que UI, JSON, cards o futuras integraciones interpreten erróneamente que no
hubo pago cuando el pago fue realizado por el circuito de condena.

Antes de esta decisión, ACTA-0030 cerrada por condena consentida + pago confirmado
mostraba situacionPago=SIN_PAGO a pesar de tener situacionPagoCondena=CONFIRMADO,
lo que era semánticamente incorrecto.

### Alcance de la implementación (prototipo)

Archivos modificados:
- backend/.../store/PrototipoStore.java — enum TipoPago, mapa tipoPagoPorActa, getter getTipoPago
- backend/.../store/PagoCondenaSupport.java — normaliza situacionPago + tipoPago en informar/confirmar/observar
- backend/.../store/PagoVoluntarioSupport.java — establece tipoPago=VOLUNTARIO en solicitud
- backend/.../store/GestionExternaSupport.java — normaliza situacionPago=CONFIRMADO + tipoPago=CONDENA en pago por apremio
- backend/.../dto/ActaDetalleResponse.java — expone tipoPago
- backend/.../dto/ActaInfractorResponse.java — expone tipoPago
- backend/.../web/PrototipoApiController.java — mapea tipoPago en ambas vistas
- apps/.../prototipo-faltas.models.ts — agrega tipoPago a modelos TypeScript

Tests que verifican esta decisión:
- SituacionPagoGeneralNormalizadaIT (5 tests)

# QA ACTA-0003 — Firma inicial / paso a notificacion

## Objetivo

Validar el tramo temprano donde una pieza inicial pendiente de firma se firma y el acta avanza a notificacion.
Verificar que no se habiliten acciones de fondo antes de completar ese tramo.

## Circuito que valida

- documento inicial pendiente de firma;
- firma de la pieza inicial;
- salida de PENDIENTE_PREPARACION_DOCUMENTAL;
- paso a PENDIENTE_NOTIFICACION;
- bloqueo de acciones de fondo antes de notificar.

## Estado inicial esperado

- Acta: `ACTA-0003`
- Bandeja esperada: `PENDIENTE_PREPARACION_DOCUMENTAL`
- Estado proceso esperado: `PENDIENTE_PREPARACION_DOCUMENTAL`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Observaciones: Debe tener al menos un documento en estado `PENDIENTE_FIRMA`.

## Camino sugerido

1. Buscar `ACTA-0003`.
2. Copiar estado.
3. Verificar que existe un documento pendiente de firma.
4. Verificar que no hay acciones de fondo disponibles (fallo, cierre, pago de condena).
5. Firmar el documento.
6. Copiar estado.
7. Verificar que el documento paso a `FIRMADO`.
8. Verificar que la bandeja cambio a `PENDIENTE_NOTIFICACION`.
9. Verificar que esta disponible la accion de envio/notificacion.

## Prompt para Copilot

```text
Estoy validando ACTA-0003 del prototipo Direccion de Faltas.

Objetivo:
Firma inicial de pieza documental y paso a notificacion.

Reglas esperadas:
- Un documento en PENDIENTE_FIRMA bloquea acciones de fondo (fallo, cierre, notificacion como entregada).
- Firmar debe cambiar el documento a FIRMADO.
- Luego de firmar debe quedar disponible el envio o notificacion.
- No debe habilitar fallo de fondo antes de notificacion positiva.

Estado copiado:
[PEGAR ESTADO AQUI]

Responde breve:
1. Momento del ciclo.
2. Proximo paso sugerido.
3. Accion que deberia buscar.
4. Resultado esperado despues del paso.
5. Que seria un hallazgo.
```

## Prompt de reanudacion

```text
Estoy retomando ACTA-0003.

Objetivo:
Firma inicial y paso a notificacion.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no aparece accion de firma cuando hay documento pendiente;
- el documento no pasa a FIRMADO despues de firmar;
- la bandeja no cambia a PENDIENTE_NOTIFICACION despues de firmar;
- aparecen acciones de fondo (fallo, cierre) mientras hay documento pendiente de firma;
- no aparece envio/notificacion despues de firmar;
- Direccion pide otro comportamiento.
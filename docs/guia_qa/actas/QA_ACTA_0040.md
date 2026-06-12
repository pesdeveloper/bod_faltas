# QA ACTA-0040 — Fallo absolutorio + notificacion postal

## Objetivo

Validar que un fallo absolutorio genere el documento correcto, se firme, se notifique positivamente,
deje resultadoFinal=ABSUELTO y habilite cierre sin pago de condena.

## Circuito que valida

- fallo absolutorio dictado;
- paso a PENDIENTE_FIRMA;
- firma del fallo absolutorio;
- paso a PENDIENTE_NOTIFICACION;
- notificacion postal positiva del fallo absolutorio;
- resultadoFinal=ABSUELTO;
- cierre habilitado si no hay bloqueantes;
- no habilitacion de pago de condena.

## Estado inicial esperado

- Acta: `ACTA-0040`
- Bandeja esperada: `PENDIENTE_FIRMA` o `EN_NOTIFICACION`
- Estado proceso esperado: `PENDIENTE_FIRMA` o `EN_NOTIFICACION`
- Resultado final esperado: `SIN_RESULTADO_FINAL` antes de notificar
- Observaciones: Fallo absolutorio generado. Acta candidata a confirmar en mocks.

## Camino sugerido

1. Buscar `ACTA-0040`.
2. Copiar estado.
3. Si el fallo aun no esta firmado:
   a. Firmar fallo absolutorio.
   b. Copiar estado.
4. Si ya esta firmado pero no notificado:
   a. Generar o procesar notificacion postal positiva del fallo absolutorio.
   b. Copiar estado.
5. Verificar que `resultadoFinal` cambio a `ABSUELTO`.
6. Verificar que cierre esta disponible si no hay bloqueantes.
7. Verificar que no aparece pago de condena.
8. Cerrar acta si corresponde.

## Prompt para Copilot

```text
Estoy validando ACTA-0040 del prototipo Direccion de Faltas.

Objetivo:
Fallo absolutorio + notificacion postal positiva + cierre.

Reglas esperadas:
- Fallo absolutorio pendiente de firma debe firmarse antes de notificar.
- Firma lleva a PENDIENTE_NOTIFICACION.
- Notificacion postal positiva del fallo absolutorio deja resultadoFinal=ABSUELTO.
- Debe habilitar cierre si no hay bloqueantes.
- No debe habilitar pago de condena.
- No debe habilitar apelacion (solo aplica para fallo condenatorio).

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
Estoy retomando ACTA-0040.

Objetivo:
Fallo absolutorio + notificacion postal positiva + cierre.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no se puede firmar fallo absolutorio;
- la firma no lleva a PENDIENTE_NOTIFICACION;
- la notificacion positiva no deja ABSUELTO;
- aparece pago de condena despues del absolutorio;
- aparece apelacion como accion (solo aplica a condenatorio);
- no aparece cierre cuando deberia;
- Direccion pide otro comportamiento.
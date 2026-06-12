# QA PORTAL INFRACTOR — Vista y operaciones desde portal

## Objetivo

Validar la vista y las operaciones que el infractor puede realizar desde el portal.
Verificar que el portal no confirma pagos y que las acciones disponibles
dependen del estado del acta.

## Circuito que valida

- acceso al portal por codigo QR o equivalente;
- visualizacion de estado del acta;
- apertura de documento notificable;
- registro de notificacion positiva por PORTAL_INFRACTOR;
- solicitud de pago voluntario desde portal;
- registro de pago voluntario desde portal;
- consentir condena desde portal;
- registrar pago de condena desde portal;
- presentar apelacion desde portal;
- bloqueo de confirmacion de pago (portal no confirma);
- bloqueo de pago voluntario si hay fallo firmado pendiente de notificacion.

## Estado inicial esperado

- Acta: candidata con estado operable visible en portal.
- Bandeja esperada: variable segun el paso que se valide.
- Estado proceso esperado: variable.
- Resultado final esperado: variable.
- Observaciones: Validar con una o varias actas del mock segun el paso del circuito.

## Camino sugerido

1. Acceder al portal del infractor con una acta operable.
2. Copiar estado.
3. Verificar que el estado del acta es visible (estado proceso, resultado, situacion pago).
4. Verificar que los documentos firmados estan visibles.
5. Verificar que los documentos no firmados NO son visibles.
6. Abrir un documento notificable firmado.
7. Copiar estado.
8. Verificar que la apertura registro notificacion positiva por PORTAL_INFRACTOR.
9. Si corresponde, solicitar pago voluntario desde portal.
10. Registrar pago voluntario desde portal.
11. Verificar que el portal no ofrece confirmar el pago.
12. Si el acta tiene condena firme, consentir condena desde portal.
13. Registrar pago de condena desde portal.
14. Verificar que el portal no ofrece confirmar la acreditacion.
15. Si corresponde, presentar apelacion desde portal.

## Prompt para Copilot

```text
Estoy validando el portal del infractor del prototipo Direccion de Faltas.

Objetivo:
Vista y operaciones desde portal infractor.

Reglas esperadas:
- Portal muestra estado, situacion pago y resultado final.
- Portal muestra documentos firmados y notificados. No muestra documentos sin firmar.
- Abrir documento notificable firmado registra notificacion positiva por PORTAL_INFRACTOR.
- Portal puede solicitar e informar pago voluntario. No confirma el pago.
- Portal puede consentir condena y registrar pago de condena. No confirma acreditacion.
- Portal puede presentar apelacion si corresponde.
- Si existe fallo firmado pendiente de notificacion, no debe permitir pago voluntario.
- Domicilio electronico del infractor tiene preferencia sobre postal.

Estado copiado:
[PEGAR ESTADO AQUI]

Responde breve:
1. Momento del ciclo.
2. Proximo paso sugerido.
3. Accion que deberia buscar en el portal.
4. Resultado esperado despues del paso.
5. Que seria un hallazgo.
```

## Prompt de reanudacion

```text
Estoy retomando validacion del portal infractor.

Objetivo:
Vista y operaciones desde portal.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- el portal muestra documentos sin firmar;
- abrir documento no registra notificacion positiva;
- el portal ofrece confirmar pago voluntario;
- el portal ofrece confirmar acreditacion de pago condena;
- aparece pago voluntario cuando hay fallo firmado pendiente de notificacion;
- no aparece la accion de apelacion cuando deberia estar disponible;
- el estado visible no coincide con el estado del sistema;
- Direccion pide otro comportamiento.
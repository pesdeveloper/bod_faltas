# QA ACTA-0029 — Fallo condenatorio + condena firme + pago condena + cierre

## Objetivo

Validar el circuito sancionatorio completo desde analisis hasta cierre por pago de condena.

## Circuito que valida

- dictar fallo condenatorio;
- firma de fallo;
- notificacion de fallo;
- vencimiento de apelacion;
- condena firme;
- pago de condena informado;
- confirmacion de pago de condena;
- cierre.

## Estado inicial esperado

- Acta: `ACTA-0029`
- Bandeja esperada: `PENDIENTE_ANALISIS`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Sin bloqueantes materiales relevantes.

## Camino sugerido

1. Buscar `ACTA-0029`.
2. Copiar estado.
3. Dictar fallo condenatorio.
4. Firmar fallo.
5. Notificar fallo positivamente.
6. Registrar vencimiento de plazo de apelacion.
7. Informar pago de condena.
8. Confirmar pago de condena.
9. Verificar cierre disponible.
10. Cerrar acta.

## Prompt para Copilot

```text
Estoy validando ACTA-0029 del prototipo Direccion de Faltas.

Objetivo:
Fallo condenatorio + condena firme + pago de condena + cierre.

Reglas esperadas:
- Dictar fallo condenatorio debe llevar a firma.
- Firmar fallo debe llevar a notificacion.
- Notificacion positiva del fallo debe dejar resultadoFinal=CONDENADO.
- Vencimiento de plazo de apelacion debe dejar resultadoFinal=CONDENA_FIRME.
- Informar pago de condena no cierra.
- Confirmar pago de condena mantiene resultadoFinal=CONDENA_FIRME y habilita cierre si no hay bloqueantes.
- Confirmar pago de condena no debe cambiar resultadoFinal a PAGO_CONFIRMADO.

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
Estoy retomando ACTA-0029.

Objetivo:
Fallo condenatorio + condena firme + pago de condena + cierre.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no se puede dictar fallo;
- el fallo no pasa a firma;
- no se puede firmar;
- no pasa a notificacion;
- la notificacion positiva no deja `CONDENADO`;
- no se puede registrar vencimiento;
- no pasa a `CONDENA_FIRME`;
- confirmar pago condena cambia a `PAGO_CONFIRMADO`;
- no aparece cierre cuando deberia;
- Direccion pide cambiar pasos.

# QA ACTA-0030 — Condena firme + gestion externa + reingreso + pago de condena

## Objetivo

Validar que una condena firme sin pago puede derivarse a gestion externa,
y que al reingresar puede completar el circuito de pago de condena y cierre.

## Circuito que valida

- condena firme sin pago voluntario previo;
- derivacion a gestion externa (apremio);
- bloqueo de acciones internas en gestion externa;
- reingreso desde gestion externa;
- informar/confirmar pago de condena;
- resultadoFinal conservado como CONDENA_FIRME al confirmar;
- cierre.

## Estado inicial esperado

- Acta: `ACTA-0030`
- Bandeja esperada: `GESTION_EXTERNA`
- Estado proceso esperado: `EN_GESTION_EXTERNA`
- Resultado final esperado: `CONDENA_FIRME`
- Observaciones: Derivada a apremio tras condena firme sin pago.

## Camino sugerido

1. Buscar `ACTA-0030`.
2. Copiar estado.
3. Verificar que no hay acciones internas normales en gestion externa.
4. Reingresar el acta desde gestion externa.
5. Copiar estado.
6. Verificar que volvio a bandeja operativa coherente con condena firme.
7. Informar pago de condena.
8. Confirmar acreditacion de pago de condena.
9. Verificar que `resultadoFinal` sigue siendo `CONDENA_FIRME`.
10. Verificar cierre disponible si no hay bloqueantes.
11. Cerrar acta.

## Prompt para Copilot

```text
Estoy validando ACTA-0030 del prototipo Direccion de Faltas.

Objetivo:
Condena firme + gestion externa + reingreso + pago de condena + cierre.

Reglas esperadas:
- EN_GESTION_EXTERNA no debe mostrar acciones internas normales.
- Al reingresar debe volver a bandeja coherente con CONDENA_FIRME.
- Informar pago de condena no cierra.
- Confirmar pago de condena conserva resultadoFinal=CONDENA_FIRME, no lo cambia a PAGO_CONFIRMADO.
- Cierra solo si no hay bloqueantes.

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
Estoy retomando ACTA-0030.

Objetivo:
Gestion externa + reingreso + pago de condena + cierre.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparecen acciones internas en gestion externa;
- no aparece reingreso;
- al reingresar la bandeja no es coherente con condena firme;
- confirmar pago condena cambia resultadoFinal a PAGO_CONFIRMADO;
- no aparece cierre cuando deberia;
- Direccion pide otro circuito.
# QA ACTA-0028 — Apelacion aceptada con lugar / absolucion

## Objetivo

Validar el camino recursivo donde una apelacion aceptada con lugar absuelve al infractor
y habilita el cierre sin requerir pago de condena.

## Circuito que valida

- fallo condenatorio notificado;
- presentacion de apelacion;
- bandeja CON_APELACION;
- resolver apelacion como aceptada con lugar;
- resultado final ABSUELTO;
- habilitacion de cierre;
- no requerir pago de condena.

## Estado inicial esperado

- Acta: `ACTA-0028`
- Bandeja esperada: `CON_APELACION`
- Estado proceso esperado: `CON_APELACION`
- Resultado final esperado: `CONDENADO`
- Observaciones: Apelacion presentada y pendiente de resolucion.

## Camino sugerido

1. Buscar `ACTA-0028`.
2. Copiar estado.
3. Verificar que la apelacion esta activa.
4. Verificar que no hay acciones de avance directo a condena firme.
5. Resolver apelacion como aceptada con lugar.
6. Copiar estado.
7. Verificar que `resultadoFinal` paso a `ABSUELTO`.
8. Verificar que cierre esta disponible si no hay bloqueantes.
9. Verificar que no aparece accion de pago de condena.
10. Cerrar acta si corresponde.

## Prompt para Copilot

```text
Estoy validando ACTA-0028 del prototipo Direccion de Faltas.

Objetivo:
Apelacion aceptada con lugar + absolucion + cierre.

Reglas esperadas:
- La apelacion suspende el avance directo a condena firme.
- Resolver apelacion como aceptada con lugar debe dejar resultadoFinal=ABSUELTO.
- Si no hay bloqueantes materiales, debe habilitar cierre.
- No debe requerir pago de condena si el resultado es ABSUELTO.
- No debe aparecer pago de condena como accion disponible.

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
Estoy retomando ACTA-0028.

Objetivo:
Apelacion aceptada con lugar + absolucion + cierre.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no se puede resolver apelacion;
- resolver no cambia a ABSUELTO;
- aparece pago de condena despues de absolucion;
- no aparece cierre cuando no hay bloqueantes y el resultado es ABSUELTO;
- el acta avanza directamente a condena firme sin pasar por apelacion;
- Direccion pide otro comportamiento.
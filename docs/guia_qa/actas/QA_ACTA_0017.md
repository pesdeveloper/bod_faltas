# QA ACTA-0017 — Gestion externa (Juzgado de Paz) + reingreso

## Objetivo

Validar que una acta derivada a Juzgado de Paz no tenga acciones internas normales
y pueda reingresar al circuito operativo con el resultado externo aplicado.

## Circuito que valida

- condena firme sin pago voluntario;
- derivacion a Juzgado de Paz;
- estado EN_GESTION_EXTERNA;
- bloqueo de acciones internas;
- seguimiento externo;
- reingreso con resultado;
- retorno a bandeja operativa coherente.

## Estado inicial esperado

- Acta: `ACTA-0017`
- Bandeja esperada: `GESTION_EXTERNA`
- Estado proceso esperado: `EN_GESTION_EXTERNA`
- Resultado final esperado: `CONDENA_FIRME` o `SIN_RESULTADO_FINAL` segun momento
- Observaciones: Derivada a Juzgado de Paz. `permiteReingreso=true`.

## Camino sugerido

1. Buscar `ACTA-0017`.
2. Copiar estado.
3. Verificar que no hay acciones internas normales (pago voluntario, fallo, notificacion, cierre, archivo).
4. Verificar que hay accion de seguimiento o reingreso disponible.
5. Reingresar el acta con resultado externo.
6. Copiar estado.
7. Verificar que volvio a bandeja operativa coherente.

## Prompt para Copilot

```text
Estoy validando ACTA-0017 del prototipo Direccion de Faltas.

Objetivo:
Gestion externa Juzgado de Paz + reingreso.

Reglas esperadas:
- EN_GESTION_EXTERNA no debe mostrar acciones internas normales.
- No debe permitir pago voluntario, fallo, notificacion, cierre, archivo ni paralizacion desde gestion externa.
- Debe permitir seguimiento externo o reingreso.
- Al reingresar debe volver a una bandeja operativa coherente con su situacion.

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
Estoy retomando ACTA-0017.

Objetivo:
Gestion externa Juzgado de Paz + reingreso.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparecen acciones internas normales en gestion externa;
- no aparece reingreso;
- el reingreso no lleva a bandeja operativa coherente;
- aparece pago voluntario, fallo, cierre o archivo en gestion externa;
- Direccion pide otro tratamiento.
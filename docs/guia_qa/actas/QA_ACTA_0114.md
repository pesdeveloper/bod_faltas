# QA ACTA-0114 — Paralizacion y reactivacion

## Objetivo

Validar que una acta paralizada no permita acciones internas normales y solo permita reactivacion.

## Circuito que valida

- paralizacion;
- motivo/observacion de paralizacion;
- bloqueo de acciones internas;
- reactivacion.

## Estado inicial esperado

- Acta: `ACTA-0114`
- Bandeja esperada: `PARALIZADAS`
- Situacion administrativa: `PARALIZADA`
- Debe tener motivo de paralizacion.

## Camino sugerido

1. Buscar `ACTA-0114`.
2. Copiar estado.
3. Verificar motivo/observacion.
4. Verificar que no haya acciones internas normales.
5. Reactivar acta.
6. Copiar estado.
7. Verificar retorno al circuito operativo.

## Prompt para Copilot

```text
Estoy validando ACTA-0114 del prototipo Direccion de Faltas.

Objetivo:
Paralizacion + reactivacion.

Reglas esperadas:
- PARALIZADAS suspende el circuito.
- No debe permitir pago, fallo, notificacion, firma, cierre, archivo, gestion externa ni cumplimiento material.
- Debe mostrar motivo y observacion si existen.
- Solo debe permitir reactivar/levantar paralizacion.
- Al reactivar, debe volver a la bandeja que corresponda segun su situacion real.

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
Estoy retomando ACTA-0114.

Objetivo:
Paralizacion + reactivacion.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparecen acciones internas normales;
- no aparece motivo;
- no aparece reactivacion;
- reactivar no devuelve al circuito;
- se permite paralizar de nuevo sin reactivar;
- Direccion pide otro tratamiento.

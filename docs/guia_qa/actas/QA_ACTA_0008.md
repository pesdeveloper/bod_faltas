# QA ACTA-0008 — Cerrada por pago confirmado

## Objetivo

Validar que una acta cerrada sea terminal y no tenga acciones internas.

## Circuito que valida

- estado terminal;
- cierre por pago voluntario;
- acciones deshabilitadas.

## Estado inicial esperado

- Acta: `ACTA-0008`
- Bandeja esperada: `CERRADAS`
- Estado proceso: `CERRADA`
- Resultado final: `PAGO_CONFIRMADO`
- Situacion pago: `CONFIRMADO`
- Sin acciones internas.

## Camino sugerido

1. Buscar `ACTA-0008`.
2. Copiar estado.
3. Verificar que no existan acciones internas.
4. Verificar que solo permita consulta/trazabilidad.

## Prompt para Copilot

```text
Estoy validando ACTA-0008 del prototipo Direccion de Faltas.

Objetivo:
Acta cerrada por pago confirmado.

Reglas esperadas:
- CERRADAS es estado terminal.
- No debe permitir reingreso ordinario.
- No debe permitir pago, fallo, notificacion, archivo, paralizacion, gestion externa ni cumplimiento material.
- Solo debe permitir consulta/trazabilidad.

Estado copiado:
[PEGAR ESTADO AQUI]

Responde breve:
1. Momento del ciclo.
2. Que deberia verse.
3. Que acciones NO deberian aparecer.
4. Que seria un hallazgo.
```

## Prompt de reanudacion

```text
Estoy retomando ACTA-0008.

Objetivo:
Acta cerrada por pago confirmado.

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime si hay alguna inconsistencia o hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparece cualquier accion interna;
- aparece reingreso ordinario;
- la situacion pago no coincide con resultado final;
- se muestra como operable;
- Direccion pide otro criterio.

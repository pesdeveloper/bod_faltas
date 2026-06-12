# QA ACTA-0037 — Notificacion postal de fallo condenatorio

## Objetivo

Validar que la notificacion positiva de un fallo condenatorio habilite apelacion/vencimiento, pero no cierre.

## Circuito que valida

- fallo condenatorio firmado;
- lote/notificacion postal;
- resultado positivo;
- resultadoFinal `CONDENADO`;
- habilitacion de apelacion o vencimiento;
- no cierre directo.

## Estado inicial esperado

- Acta: `ACTA-0037`
- Debe estar vinculada a fallo condenatorio firmado pendiente de notificacion.

## Camino sugerido

1. Buscar `ACTA-0037`.
2. Copiar estado.
3. Generar/procesar notificacion postal positiva del fallo si corresponde.
4. Copiar estado.
5. Verificar `resultadoFinal=CONDENADO`.
6. Verificar apelacion/vencimiento disponible.
7. Verificar cierre no disponible.

## Prompt para Copilot

```text
Estoy validando ACTA-0037 del prototipo Direccion de Faltas.

Objetivo:
Notificacion postal positiva de fallo condenatorio.

Reglas esperadas:
- Fallo condenatorio firmado debe notificarse.
- Notificacion positiva del fallo deja resultadoFinal=CONDENADO.
- Debe habilitar apelacion o vencimiento del plazo.
- No debe habilitar cierre directo.

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
Estoy retomando ACTA-0037.

Objetivo:
Notificacion postal positiva de fallo condenatorio.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no se puede notificar fallo;
- la positiva no deja `CONDENADO`;
- no aparecen apelacion/vencimiento;
- aparece cierre directo;
- Direccion pide otro comportamiento.

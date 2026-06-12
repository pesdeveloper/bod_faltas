# QA ACTA-0004 — Notificacion inicial positiva

## Objetivo

Validar que una notificacion inicial positiva deje el acta lista para analisis y tratamiento de fondo.
Verificar que la notificacion por si sola no cierra el acta.

## Circuito que valida

- acta pendiente o en notificacion;
- registrar notificacion positiva (postal o canal disponible);
- retorno a analisis;
- habilitacion de acciones posteriores segun estado;
- no cierre automatico por solo notificar.

## Estado inicial esperado

- Acta: `ACTA-0004`
- Bandeja esperada: `EN_NOTIFICACION` o `PENDIENTE_NOTIFICACION`
- Estado proceso esperado: `EN_NOTIFICACION` o `PENDIENTE_ENVIO`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Observaciones: Acta de infraccion inicial con notificacion postal pendiente.

## Camino sugerido

1. Buscar `ACTA-0004`.
2. Copiar estado.
3. Registrar notificacion positiva o procesar lote postal positivo si corresponde.
4. Copiar estado.
5. Verificar que la bandeja cambio a `PENDIENTE_ANALISIS`.
6. Verificar que se habilitaron acciones de analisis.
7. Verificar que no se habilito cierre automatico.
8. Verificar que pago voluntario este disponible si corresponde al estado.

## Prompt para Copilot

```text
Estoy validando ACTA-0004 del prototipo Direccion de Faltas.

Objetivo:
Notificacion inicial positiva y retorno a analisis.

Reglas esperadas:
- Notificacion positiva acredita conocimiento del infractor.
- Debe llevar la bandeja a PENDIENTE_ANALISIS.
- No debe cerrar el acta por si sola.
- Debe habilitar pago voluntario o tratamiento posterior segun estado.
- No debe habilitar cierre automatico ni fallo inmediato.

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
Estoy retomando ACTA-0004.

Objetivo:
Notificacion inicial positiva y retorno a analisis.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no aparece accion de registro de notificacion;
- la positiva no cambia la bandeja a PENDIENTE_ANALISIS;
- aparece cierre automatico despues de notificar;
- no se habilitan acciones posteriores;
- no aparece pago voluntario cuando deberia;
- Direccion pide otro comportamiento.
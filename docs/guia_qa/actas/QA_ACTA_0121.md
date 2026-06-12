# QA ACTA-0121 — Firma de fallo condenatorio

## Objetivo

Validar especificamente que un fallo condenatorio pendiente de firma no pueda notificarse ni avanzar
hasta que se firme, y que la firma habilite correctamente la notificacion.

## Circuito que valida

- fallo condenatorio generado;
- documento en PENDIENTE_FIRMA;
- bloqueo de notificacion y pasos posteriores antes de firmar;
- firma del fallo;
- paso a PENDIENTE_NOTIFICACION;
- no habilitacion de vencimiento de apelacion antes de notificar.

## Estado inicial esperado

- Acta: `ACTA-0121`
- Bandeja esperada: `PENDIENTE_FIRMA`
- Estado proceso esperado: `PENDIENTE_FIRMA`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Observaciones: Fallo condenatorio generado sin firmar. Acta candidata a confirmar en mocks.

## Camino sugerido

1. Buscar `ACTA-0121`.
2. Copiar estado.
3. Verificar que el fallo condenatorio existe y esta en PENDIENTE_FIRMA.
4. Verificar que no hay accion de notificacion disponible.
5. Verificar que no hay accion de vencimiento de apelacion.
6. Firmar el fallo condenatorio.
7. Copiar estado.
8. Verificar que el documento paso a FIRMADO.
9. Verificar que la bandeja cambio a PENDIENTE_NOTIFICACION.
10. Verificar que ahora esta disponible la accion de notificacion.

## Prompt para Copilot

```text
Estoy validando ACTA-0121 del prototipo Direccion de Faltas.

Objetivo:
Firma de fallo condenatorio pendiente.

Reglas esperadas:
- Un fallo condenatorio PENDIENTE_FIRMA bloquea notificacion y pasos posteriores.
- No debe aparecer accion de vencimiento de apelacion antes de firmar y notificar.
- Firmar debe cambiar el documento a FIRMADO.
- Luego de firmar debe quedar disponible la accion de notificacion.
- Solo despues de notificacion positiva se habilita el plazo de apelacion.

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
Estoy retomando ACTA-0121.

Objetivo:
Firma de fallo condenatorio pendiente.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparece accion de notificacion antes de firmar;
- aparece accion de vencimiento de apelacion antes de firmar y notificar;
- no aparece accion de firma;
- el documento no pasa a FIRMADO despues de firmar;
- la bandeja no cambia a PENDIENTE_NOTIFICACION despues de firmar;
- Direccion pide otro comportamiento.
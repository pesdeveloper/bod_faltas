# QA ACTA-0022 — Pago voluntario + materiales + cierre

## Objetivo

Validar que un acta con pago voluntario y bloqueantes materiales no cierre hasta resolver todos los materiales.

## Circuito que valida

- acta con bloqueantes materiales;
- pago voluntario;
- pago informado;
- confirmacion de pago;
- bloqueo de cierre por materiales;
- resolutorios materiales;
- firma de resolutorios;
- cumplimiento material efectivo;
- cierre final.

## Estado inicial esperado

- Acta: `ACTA-0022`
- Bandeja esperada: `ACTAS_EN_ENRIQUECIMIENTO`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Situacion administrativa: `ACTIVA`
- Materiales esperados:
  - medida preventiva;
  - rodado;
  - documentacion.

## Camino sugerido

1. Buscar `ACTA-0022`.
2. Abrir detalle.
3. Copiar estado.
4. Iniciar pago voluntario.
5. Informar pago.
6. Confirmar acreditacion.
   - Si el acta pasa a ARCHIVO por materiales pendientes, reingresar primero antes de resolver bloqueantes.
7. Verificar que el acta no cierre si quedan materiales pendientes.
8. Resolver documentacion.
9. Firmar resolutorio si corresponde.
10. Registrar entrega efectiva.
11. Resolver medida preventiva.
12. Firmar resolutorio si corresponde.
13. Registrar levantamiento efectivo.
14. Resolver rodado.
15. Firmar resolutorio si corresponde.
16. Registrar retiro `(app)`.
17. Verificar cierre disponible.
18. Cerrar acta.

## Prompt para Copilot

```text
Estoy validando ACTA-0022 del prototipo Direccion de Faltas.

Objetivo:
Validar pago voluntario + materiales + cierre.

Reglas esperadas:
- El pago confirmado no debe cerrar el acta si quedan bloqueantes materiales.
- El documento resolutorio no elimina el bloqueante por si solo.
- El cumplimiento material efectivo si elimina el bloqueante.
- El acta solo debe cerrar cuando no queden bloqueantes y exista resultado final cerrable.
- Si esta en ARCHIVO, no debe mostrar acciones internas normales; debe reingresar primero si corresponde.

Te voy a pegar el estado actual copiado desde el sistema.

Responde breve:
1. Momento del ciclo.
2. Proximo paso sugerido.
3. Accion que deberia buscar.
4. Resultado esperado despues del paso.
5. Que seria un hallazgo.

Estado copiado:
[PEGAR ESTADO AQUI]
```

## Prompt de reanudacion

```text
Estoy retomando ACTA-0022.

Objetivo:
Pago voluntario + materiales + cierre.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime:
1. En que momento del circuito esta.
2. Cual es el proximo paso sugerido.
3. Que accion deberia estar disponible.
4. Que seria un hallazgo en este punto.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no aparece pago voluntario cuando deberia;
- no se puede informar pago;
- no se puede confirmar acreditacion;
- el acta cierra aunque quedan materiales pendientes;
- no aparecen acciones para resolver materiales;
- el resolutorio firmado elimina el bloqueante sin cumplimiento efectivo;
- no aparece cierre cuando ya no quedan bloqueantes;
- cualquier paso falla;
- Direccion pide cambiar el circuito.

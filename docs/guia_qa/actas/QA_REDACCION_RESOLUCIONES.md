# QA REDACCION DE RESOLUCIONES — Piezas documentales no fallo

## Objetivo

Validar el circuito de redaccion documental que no corresponde a fallo condenatorio o absolutorio:
nulidades, resoluciones, medidas preventivas, rectificaciones.

## Circuito que valida

- generar nulidad;
- generar resolucion general;
- generar medida preventiva;
- generar rectificacion si aplica;
- paso a firma (si el tipo de pieza requiere firma);
- firma del documento;
- efecto posterior segun tipo de pieza;
- diferencia entre documento firmado y cumplimiento material efectivo.

## Estado inicial esperado

- Acta: candidata con bandeja operativa interna activa.
- Bandeja esperada: `PENDIENTES_RESOLUCION_REDACCION` o `PENDIENTE_ANALISIS`
- Estado proceso esperado: variable
- Resultado final esperado: variable
- Observaciones: Validar con acta que tenga alguna accion de generacion documental disponible.

## Camino sugerido

1. Buscar una acta con accion de generar resolucion, nulidad o medida preventiva disponible.
2. Copiar estado.
3. Generar la pieza (nulidad / resolucion / medida / rectificacion).
4. Copiar estado.
5. Verificar que la pieza quedo en PENDIENTE_FIRMA si requiere firma.
6. Firmar la pieza.
7. Copiar estado.
8. Verificar que el documento paso a FIRMADO.
9. Verificar el efecto posterior segun tipo de pieza:
   - Nulidad o absolucion sin bloqueantes: debe habilitar cierre.
   - Medida preventiva: genera bloqueante material, no lo resuelve.
   - Rectificacion: corrige dato pero no avanza el circuito por si sola.
10. Verificar que el documento firmado NO elimina por si solo el bloqueante material.

## Prompt para Copilot

```text
Estoy validando redaccion de resoluciones en el prototipo Direccion de Faltas.

Objetivo:
Generacion y firma de pieza documental no fallo.

Reglas esperadas:
- Generar pieza crea documento pendiente de firma si el tipo lo requiere.
- Firmar el documento lo cambia a FIRMADO.
- El efecto posterior depende del tipo de pieza.
- En materiales: el documento firmado NO elimina el bloqueante. Solo el cumplimiento efectivo lo hace.
- Nulidad o absolucion sin bloqueantes: puede habilitar cierre.
- No debe habilitar acciones incompatibles con el estado actual.

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
Estoy retomando validacion de redaccion documental.

Objetivo:
Generacion y firma de pieza no fallo.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- no aparece accion de generar la pieza cuando deberia;
- la pieza generada no queda en PENDIENTE_FIRMA cuando requiere firma;
- el documento firmado elimina el bloqueante material sin cumplimiento efectivo;
- no aparece cierre cuando nulidad/absolucion no tiene bloqueantes;
- aparecen acciones incompatibles con el estado;
- la firma no cambia el documento a FIRMADO;
- Direccion pide otro circuito documental.
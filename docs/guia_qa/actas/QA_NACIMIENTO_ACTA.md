# QA NACIMIENTO DEL ACTA — Captura inicial y completitud documental

## Objetivo

Validar el nacimiento funcional del acta y sus primeros pasos operativos:
desde el labrado hasta que esta lista para firma o notificacion.

## Circuito que valida

- acta labrada (captura inicial);
- enriquecimiento de datos;
- completitud documental;
- generacion de piezas iniciales si corresponde;
- preparacion para firma;
- acciones posibles antes de notificar;
- bloqueo de acciones de fondo indebidas en esta etapa.

## Estado inicial esperado

- Acta: candidata en bandeja ACTAS_EN_ENRIQUECIMIENTO.
- Bandeja esperada: `ACTAS_EN_ENRIQUECIMIENTO`
- Estado proceso esperado: `LABRADA` o `EN_ENRIQUECIMIENTO`
- Resultado final esperado: `SIN_RESULTADO_FINAL`
- Observaciones: Validar con acta candidata disponible en ACTAS_EN_ENRIQUECIMIENTO.
  Si no hay una acta especifica, tomar cualquier acta en esa bandeja.

## Camino sugerido

1. Abrir la bandeja ACTAS_EN_ENRIQUECIMIENTO.
2. Seleccionar una acta en estado LABRADA o EN_ENRIQUECIMIENTO.
3. Copiar estado.
4. Verificar que los datos de labrado esten visibles.
5. Verificar que no hay acciones de fondo disponibles (fallo, cierre, pago condena).
6. Verificar si hay acciones de enriquecimiento disponibles.
7. Si hay documentos faltantes identificados, verificar que el sistema los indica.
8. Si hay accion de generar piezas iniciales, ejecutarla.
9. Copiar estado.
10. Verificar que el acta avanza a PENDIENTE_PREPARACION_DOCUMENTAL o PENDIENTE_FIRMA.
11. Verificar que las piezas generadas quedaron en estado PENDIENTE_FIRMA.

## Prompt para Copilot

```text
Estoy validando el nacimiento de un acta en el prototipo Direccion de Faltas.

Objetivo:
Captura inicial, enriquecimiento y preparacion documental.

Reglas esperadas:
- El acta nace en ACTAS_EN_ENRIQUECIMIENTO con estado LABRADA.
- Inicialmente pertenece al circuito de captura/enriquecimiento.
- No debe habilitar fallo, cierre ni pago de condena en esta etapa.
- Una vez completada, debe poder avanzar a preparacion documental o firma.
- Si hay piezas iniciales requeridas, deben generarse y quedar pendientes de firma.
- Si falta dato o documento requerido, debe quedar en bandeja/filtro correspondiente.

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
Estoy retomando validacion del nacimiento del acta.

Objetivo:
Captura inicial y completitud documental.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparecen acciones de fondo en la etapa de enriquecimiento;
- el acta no avanza cuando deberia estar completa;
- el acta avanza prematuramente sin completar datos requeridos;
- las piezas generadas no quedan en PENDIENTE_FIRMA;
- no aparece la bandeja correcta despues de generar piezas;
- Direccion pide otro circuito de captura.
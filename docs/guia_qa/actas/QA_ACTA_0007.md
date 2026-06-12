# QA ACTA-0007 — Archivo y reingreso

## Objetivo

Validar que un acta archivada no tenga acciones internas normales y solo permita reingreso si corresponde.

## Circuito que valida

- archivo operativo;
- presentacion visual `Archivada recuperable`;
- reingreso desde archivo;
- retorno a analisis.

## Estado inicial esperado

- Acta: `ACTA-0007`
- Bandeja esperada: `ARCHIVO`
- Situacion administrativa: `ARCHIVO`
- `permiteReingreso=true`

## Camino sugerido

1. Buscar `ACTA-0007`.
2. Copiar estado.
3. Verificar que no tenga acciones internas normales.
4. Verificar label `Archivada recuperable`.
5. Reingresar acta.
6. Copiar estado.
7. Verificar retorno al circuito operativo.

## Prompt para Copilot

```text
Estoy validando ACTA-0007 del prototipo Direccion de Faltas.

Objetivo:
Archivo + reingreso.

Reglas esperadas:
- ARCHIVO no debe mostrar acciones internas normales.
- Solo debe permitir reingreso si permiteReingreso=true.
- Debe mostrar "Archivada recuperable" si permite reingreso.
- No debe mostrar chips operativos como "Cerrable" o "Condena firme pendiente".

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
Estoy retomando ACTA-0007.

Objetivo:
Archivo + reingreso.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- aparecen acciones internas normales en ARCHIVO;
- no aparece reingreso cuando `permiteReingreso=true`;
- no aparece `Archivada recuperable`;
- aparecen chips confusos;
- el reingreso no vuelve al circuito operativo.

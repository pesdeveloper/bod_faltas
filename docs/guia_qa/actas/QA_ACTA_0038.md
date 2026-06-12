# QA ACTA-0038 — Notificacion negativa + reintento

## Objetivo

Validar que una notificacion negativa no habilite fallo y deje disponible el reintento.

## Circuito que valida

- notificacion en curso;
- resultado negativo;
- retorno a analisis;
- reintento de notificacion;
- no habilitacion de fallo de fondo.

## Estado inicial esperado

- Acta: `ACTA-0038`
- Bandeja esperada: `PENDIENTE_NOTIFICACION` o `EN_NOTIFICACION`
- Debe tener documento/notificacion pendiente o en curso.

## Camino sugerido

1. Buscar `ACTA-0038`.
2. Copiar estado.
3. Registrar notificacion negativa o procesar lote postal negativo si corresponde.
4. Copiar estado.
5. Verificar que no habilite fallo.
6. Verificar que habilite reintento.
7. Reintentar notificacion.
8. Copiar estado.

## Prompt para Copilot

```text
Estoy validando ACTA-0038 del prototipo Direccion de Faltas.

Objetivo:
Notificacion negativa + reintento.

Reglas esperadas:
- La notificacion negativa/no entregada/vencida no equivale a positiva.
- No debe habilitar fallo de fondo.
- Debe permitir reintento u otro canal de notificacion.
- Si vuelve a notificacion, debe quedar en bandeja/estado de envio o notificacion.

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
Estoy retomando ACTA-0038.

Objetivo:
Notificacion negativa + reintento.

Pasos ya realizados:
[COMPLETAR EN UNA LINEA]

Ultimo estado copiado:
[PEGAR ESTADO AQUI]

Decime el proximo paso sugerido y que seria un hallazgo.
```

## Cuando registrar hallazgo

Registrar hallazgo si:

- una negativa habilita fallo;
- no aparece reintento;
- el reintento no vuelve a notificacion;
- aparece cierre;
- el estado posterior es ambiguo o confuso;
- Direccion pide otro tratamiento.

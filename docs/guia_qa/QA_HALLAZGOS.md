# Hallazgos QA — Direccion de Faltas

Este documento registra solo problemas, faltantes, inconsistencias, dudas o cambios pedidos durante la validacion.

No registrar pasos correctos.

## Formato minimo de hallazgo

Copiar y completar este bloque cada vez que aparezca un problema:

```text
## Hallazgo

Que quise hacer:

Que esperaba:

Que paso / que falto:

Comentario de Direccion:

Estado copiado:

[PEGAR AQUI EL ESTADO COPIADO DESDE LA APP]
```

## Tipos sugeridos

No es obligatorio que QA clasifique el hallazgo. Si lo hace, usar uno de estos tipos:

- BUG_FUNCIONAL
- FALTA_ACCION
- ACCION_INCORRECTA
- ESTADO_INESPERADO
- CAMBIO_PEDIDO_DIRECCION
- UX_CONFUSA
- DATO_MOCK_INSUFICIENTE
- DUDA_REGLA

## Prompt para que Copilot ordene hallazgos al cierre del caso

```text
Ordena los hallazgos detectados en este caso.

No incluyas pasos que funcionaron correctamente.
No inventes datos.
Si falta informacion, marca "informacion faltante".

Para cada hallazgo usa este formato:

## Hallazgo

Tipo sugerido:
Que se intento hacer:
Que se esperaba:
Que ocurrio / que falto:
Comentario de Direccion:
Estado copiado completo:
Impacto estimado:
Nota para desarrollo:

Si no hubo problemas, responde:
"Caso sin hallazgos registrados".
```

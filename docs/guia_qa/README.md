# Guia QA — Validacion asistida por Copilot

## Objetivo

Validar el prototipo Direccion de Faltas usando actas mock candidatas, con Copilot como guia de recorrido.
Las fichas cubren el 100% de los caminos funcionales del prototipo.

## Que se valida

- ciclo de vida del acta;
- bandeja actual;
- estado proceso;
- acciones disponibles;
- acciones que no deberian aparecer;
- transiciones luego de cada accion;
- bloqueantes;
- cierre;
- comportamiento visual suficiente para Direccion.

## Que no se valida

- datos reales definitivos;
- integracion productiva;
- persistencia real;
- diseno final;
- exactitud legal definitiva de cada texto.

## Dinamica de trabajo

1. Abrir la ficha del acta a validar.
2. Pegar en Copilot el prompt del caso.
3. Buscar el acta en el sistema.
4. Copiar estado desde la app.
5. Pegar el estado en Copilot.
6. Copilot debe indicar:
   - en que momento del circuito esta el acta;
   - cual es el proximo paso sugerido;
   - que accion deberia buscar QA;
   - que deberia pasar despues;
   - que seria una inconsistencia.
7. QA ejecuta la accion en la app.
8. Si todo coincide, continua.
9. Si algo falla, falta, sobra, confunde o Direccion pide otro comportamiento, se registra hallazgo.

## Regla principal

No registrar lo que funciona.

Registrar solo:

- no pude hacer el paso;
- la accion no aparecio;
- la accion aparecio pero no correspondia;
- la accion fallo;
- el estado posterior quedo mal;
- Direccion quiere otro comportamiento;
- la UX confundio;
- falto dato mock.

## Como retomar un caso

Si Copilot se cuelga o se reinicia la conversacion:

1. Abrir una nueva sesion.
2. Pegar el prompt de reanudacion de la ficha del acta.
3. Indicar en una linea que pasos ya se hicieron.
4. Pegar el ultimo estado copiado desde la app.
5. Pedir el proximo paso sugerido.

## Que vuelve a desarrollo

Para que desarrollo pueda corregir, alcanza con:

- descripcion breve del problema;
- estado copiado completo;
- comentario de Direccion si aplica.

Con eso se puede clasificar el problema y armar un slice correctivo.

## Relacion con la matriz de reglas

Si QA o Direccion detectan que el comportamiento esperado deberia cambiar, no se registra automaticamente como bug.

Primero se registra como "cambio pedido por Direccion".
Luego debe actualizarse `docs/MATRIZ_REGLAS_ACTA.md`.
Despues se genera el slice tecnico.

## Principios

1. Copilot se usa como guia de recorrido para QA.
2. Copilot no reemplaza el criterio de QA ni de Direccion.
3. QA no debe registrar pasos correctos.
4. Solo se registran problemas, faltantes, inconsistencias, dudas o cambios pedidos.
5. Cada ficha de acta debe ser autosuficiente.
6. Si Copilot se cuelga, se puede iniciar de nuevo pegando el prompt del caso y el ultimo estado copiado.
7. El insumo minimo para desarrollo es:
   - descripcion breve del problema;
   - estado copiado desde la app.
8. Si Direccion pide cambiar una regla, primero debe actualizarse `docs/MATRIZ_REGLAS_ACTA.md`.
9. Luego, con el hallazgo y la regla actualizada, se genera el slice correctivo.

## Cobertura de fichas

Las fichas se organizan para cubrir los siguientes tramos del ciclo completo del prototipo:

1. Nacimiento y enriquecimiento.
2. Firma inicial.
3. Notificacion inicial positiva.
4. Notificacion negativa y reintento.
5. Pago voluntario.
6. Materiales y cumplimiento efectivo.
7. Redaccion documental (piezas no fallo).
8. Fallo condenatorio.
9. Firma de fallo.
10. Notificacion de fallo.
11. Apelacion.
12. Condena firme.
13. Pago de condena.
14. Gestion externa.
15. Archivo y reingreso.
16. Paralizacion y reactivacion.
17. Cierre terminal.
18. Portal infractor.

## Fichas disponibles

| Ficha | Circuito cubierto | Tramo |
|-------|------------------|-------|
| [QA_NACIMIENTO_ACTA](actas/QA_NACIMIENTO_ACTA.md) | Labrado, captura inicial, enriquecimiento, preparacion documental | 1 |
| [ACTA-0003](actas/QA_ACTA_0003.md) | Firma inicial / paso a notificacion | 2 |
| [ACTA-0004](actas/QA_ACTA_0004.md) | Notificacion inicial positiva / retorno a analisis | 3 |
| [ACTA-0038](actas/QA_ACTA_0038.md) | Notificacion negativa + reintento | 4 |
| [ACTA-0022](actas/QA_ACTA_0022.md) | Pago voluntario + materiales + cierre | 5, 6 |
| [QA_REDACCION_RESOLUCIONES](actas/QA_REDACCION_RESOLUCIONES.md) | Nulidad, resolucion, medida preventiva, rectificacion | 7 |
| [ACTA-0029](actas/QA_ACTA_0029.md) | Fallo condenatorio + condena firme + pago condena + cierre | 8, 12, 13 |
| [ACTA-0121](actas/QA_ACTA_0121.md) | Firma de fallo condenatorio pendiente | 9 |
| [ACTA-0037](actas/QA_ACTA_0037.md) | Notificacion postal positiva de fallo condenatorio | 10 |
| [ACTA-0040](actas/QA_ACTA_0040.md) | Fallo absolutorio + notificacion postal + cierre | 8, 10, 17 |
| [ACTA-0028](actas/QA_ACTA_0028.md) | Apelacion aceptada con lugar / absolucion + cierre | 11 |
| [ACTA-0030](actas/QA_ACTA_0030.md) | Condena firme + gestion externa + reingreso + pago condena | 12, 13, 14 |
| [ACTA-0017](actas/QA_ACTA_0017.md) | Gestion externa Juzgado de Paz + reingreso | 14 |
| [ACTA-0007](actas/QA_ACTA_0007.md) | Archivo + reingreso | 15 |
| [ACTA-0114](actas/QA_ACTA_0114.md) | Paralizacion + reactivacion | 16 |
| [ACTA-0008](actas/QA_ACTA_0008.md) | Cerrada por pago confirmado (estado terminal) | 17 |
| [QA_PORTAL_INFRACTOR](actas/QA_PORTAL_INFRACTOR.md) | Vista y operaciones del infractor desde portal | 18 |
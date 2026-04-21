# Slice {{N}} — {{TITULO}}

## Objetivo
Implementar o corregir {{comportamiento puntual}} en el prototipo backend, manteniendo coherencia con el modelo final.

## Fuente de verdad
Tomar como fuente principal solo:

- `spec/...`
- `spec/...`
- `spec/...`

Usar otras partes de la spec solo si resulta estrictamente necesario.

## Alcance técnico
Trabajar solo sobre:

- `backend/api-faltas-prototipo/...`
- `backend/api-faltas-prototipo/...`
- `backend/api-faltas-prototipo/...`

## No tocar
- otros módulos backend
- persistencia real
- workers
- frontend
- SQL físico
- código no relacionado con este circuito
- circuitos viejos ya descartados

## Restricciones funcionales
- no reintroducir circuitos viejos
- mantener un único circuito verdadero
- el estado del expediente debe ser agregador cuando haya múltiples piezas o documentos
- el detalle fino debe vivir en estructuras específicas
- si una simplificación rompe el modelo final, corregirla ahora

## Caso demo principal
Usar como caso principal:

- `ACTA-XXXX`

### Recorrido esperado
1. {{estado o acción inicial}}
2. {{acción}}
3. {{resultado esperado}}
4. {{siguiente acción}}
5. {{estado final esperado}}

## Criterio de cierre
El slice queda bien cuando:

- {{condición observable 1}}
- {{condición observable 2}}
- {{condición observable 3}}
- no queda ningún circuito viejo compitiendo con el nuevo

## Instrucción de implementación
Actuar de forma quirúrgica.
No improvisar.
No sobrearquitectar.
No abrir más alcance del necesario.
Si un circuito viejo queda obsoleto por este cambio, eliminarlo completo.
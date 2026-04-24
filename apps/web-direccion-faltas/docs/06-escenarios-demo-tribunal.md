# UX Demo — Escenarios Tribunal

## Objetivo

Guion de demo funcional-operativa.

Mostrar situaciones, no endpoints.

## Escenario 1 — Alta mock en vivo

Acción:

- Crear acta demo

Crear:

- Tránsito con rodado/documentación
- Inspecciones con clausura
- Bromatología con decomiso

Validar:

- genera `ACTA-DEMO-0001+`
- aparece en bandeja
- abre detalle
- proyecta condiciones correctas

Preguntas:

- ¿Estos datos mínimos alcanzan?
- ¿Qué dato falta para reconocer el caso?
- ¿Qué dependencia debería condicionar acciones?

## Escenario 2 — Tránsito integral

Caso:

- `ACTA-0024` o acta demo tránsito

Mostrar:

- datos tránsito
- rodado/documentación retenida
- hechos materiales
- bloqueantes
- pago confirmado sin cierre automático

Acciones:

- solicitar pago
- registrar pago informado
- adjuntar comprobante
- confirmar pago
- dictar resoluciones
- firmar si aplica
- registrar cumplimiento
- cerrar

Preguntas:

- ¿Quién libera rodado?
- ¿Quién entrega documentación?
- ¿Requiere resolución?
- ¿Requiere firma/notificación?

## Escenario 3 — Inspecciones con clausura

Caso:

- acta demo inspecciones

Mostrar:

- medida preventiva inicial
- bloqueante `LEVANTAMIENTO_MEDIDA_PREVENTIVA`
- cerrabilidad bloqueada

Acciones:

- dictar resolución
- firmar
- registrar cumplimiento
- cerrar si corresponde

Preguntas:

- ¿Quién dispone clausura?
- ¿Quién firma?
- ¿Quién verifica levantamiento?

## Escenario 4 — Fiscalización obra

Caso:

- acta demo fiscalización

Mostrar:

- paralización de obra
- medida preventiva
- sin datos tránsito

Preguntas:

- ¿Circuito igual a clausura?
- ¿Requiere inspección posterior?
- ¿Qué dato de obra es mínimo?

## Escenario 5 — Bromatología/decomiso

Caso:

- acta demo bromatología

Mostrar:

- decomiso como dato
- no medida preventiva
- sin bloqueantes actuales

Preguntas:

- ¿Se libera, destruye, dona, restituye o solo consta?
- ¿Hace falta eje futuro?
- ¿Requiere resolución/firma/notificación?

## Escenario 6 — Medida posterior

Caso:

- `ACTA-0026`

Narrativa:

- acta en trámite
- inspección posterior
- rotura de faja
- nace medida preventiva posterior

Acciones:

- registrar medida posterior
- ver hechos materiales
- dictar resolución
- registrar cumplimiento
- cerrar

Preguntas:

- ¿Cuándo genera nueva acta?
- ¿Cuándo sigue en la existente?
- ¿Quién registra novedad?

## Escenario 7 — Resolución desde enriquecimiento

Caso:

- `ACTA-0024`

Mostrar:

- dictar resolución desde enriquecimiento
- bandeja se conserva
- bloqueo sigue activo

Preguntas:

- ¿Es correcto resolver desde esta instancia?
- ¿Qué resoluciones aplican antes del análisis formal?

## Escenario 8 — Firma/notificación/cumplimiento

Caso:

- `ACTA-0024`

Secuencia:

1. dictar resolución con firma/notificación
2. ver documento pendiente firma
3. firmar
4. verificar que no cumple materialmente
5. registrar cumplimiento
6. verificar liberación de eje

Mensaje:

Firma habilita documento.  
Notificación comunica acto.  
Cumplimiento verifica hecho.

## Escenario 9 — Gestión externa

Caso:

- `ACTA-0015`

Acciones:

- derivar
- retornar
- re-derivar

Validar:

- no resolución interna en gestión externa
- conserva trazabilidad

Preguntas:

- ¿Tipos faltantes?
- ¿Resultados posibles al volver?

## Escenario 10 — Archivo/reingreso

Acciones:

- archivar
- reingresar
- ver revisión post reingreso

Validar:

- archivo bloquea acciones internas
- reingreso vuelve a análisis

Preguntas:

- ¿Motivos reales?
- ¿Quién reingresa?
- ¿Qué pasa con plazos?

## Escenario 11 — Notificación negativa/vencida

Casos:

- `ACTA-0004`
- `ACTA-0005`

Acciones:

- acuse positivo
- acuse negativo
- vencimiento
- reintento
- volver a análisis

Preguntas:

- ¿Cuántos reintentos?
- ¿Qué canal se usa?
- ¿Cuándo vuelve a análisis?

## Escenario 12 — Cierre explícito

Mensaje:

Un expediente no se cierra solo.

Aunque exista pago, absolución, firma o notificación, solo queda cerrable sin pendientes. El cierre es explícito.

Preguntas:

- ¿Quién cierra?
- ¿Debe haber revisión final?
- ¿Qué debe verse antes de cerrar?

## Orden recomendado

1. Alcance demo
2. Crear acta demo
3. Tránsito integral
4. Bromatología/decomiso
5. Medida posterior
6. Firma/notificación/cumplimiento
7. Pago sin cierre automático
8. Gestión externa
9. Archivo/reingreso
10. Preguntas abiertas

## Criterio de éxito

La demo sirve si aparecen frases como:

- esto sí pasa
- esto no pasa
- falta esta acción
- falta esta bandeja
- esto lo hace otro rol
- esto debería abrir otra acta
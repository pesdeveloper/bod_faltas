# UX Demo — Checklist validación post-demo

## Objetivo

Checklist para relevar feedback del Tribunal y convertirlo luego en spec/sistema real.

## Bandejas

- [ ] ¿Se entienden las bandejas?
- [ ] ¿Falta una bandeja macro?
- [ ] ¿Sobra alguna?
- [ ] ¿Alguna debería ser filtro?
- [ ] ¿Los nombres son correctos?
- [ ] ¿Qué rol usa cada bandeja?

## Listado

- [ ] ¿Qué datos deben verse en fila?
- [ ] ¿Debe verse infractor?
- [ ] ¿Debe verse dominio/rodado?
- [ ] ¿Debe verse comercio/local?
- [ ] ¿Debe verse cerrabilidad?
- [ ] ¿Debe verse último evento?
- [ ] ¿Qué filtros faltan?

## Detalle

- [ ] ¿Resumen operativo claro?
- [ ] ¿Hechos materiales ayudan?
- [ ] ¿Cerrabilidad explica bien?
- [ ] ¿Documentos/firma/notificación se entienden?
- [ ] ¿Qué dato falta?
- [ ] ¿Qué dato sobra?

## Acciones

- [ ] ¿Qué acciones faltan por bandeja?
- [ ] ¿Qué acciones sobran?
- [ ] ¿Qué acciones deben verse deshabilitadas?
- [ ] ¿Los motivos son claros?
- [ ] ¿Qué acción requiere rol especial?
- [ ] ¿Qué acción mueve de bandeja?
- [ ] ¿Qué acción genera documento?
- [ ] ¿Qué acción requiere firma?
- [ ] ¿Qué acción requiere notificación?

## Cierre

- [ ] ¿Quién puede cerrar?
- [ ] ¿Debe haber revisión final?
- [ ] ¿Qué debe verse antes de cerrar?
- [ ] ¿Pago confirmado alcanza sin bloqueantes?
- [ ] ¿Absolución alcanza sin bloqueantes?
- [ ] ¿Archivo y cerrada están bien diferenciados?

## Hechos materiales

- [ ] ¿Ejes actuales alcanzan?
- [ ] ¿Falta eje decomiso?
- [ ] ¿Falta eje clausura?
- [ ] ¿Falta eje paralización?
- [ ] ¿Puede haber cumplimiento parcial?
- [ ] ¿Quién registra cumplimiento?
- [ ] ¿Qué prueba se requiere?
- [ ] ¿Cuándo un hecho posterior genera nueva acta?

## Tránsito

- [ ] ¿Rodado retenido nace en acta?
- [ ] ¿Documentación retenida nace en acta?
- [ ] ¿Eje urbano es necesario?
- [ ] ¿Liberación requiere resolución?
- [ ] ¿Entrega documentación requiere resolución?
- [ ] ¿Qué dato mínimo de rodado debe verse?

## Inspecciones

- [ ] ¿Clausura es la medida principal?
- [ ] ¿Siempre requiere firma?
- [ ] ¿Siempre requiere notificación?
- [ ] ¿Quién verifica levantamiento?
- [ ] ¿Puede nacer durante trámite?

## Fiscalización

- [ ] ¿Paralización de obra equivale a clausura?
- [ ] ¿Tiene circuito distinto?
- [ ] ¿Requiere inspección posterior?
- [ ] ¿Qué dato de obra es mínimo?

## Bromatología

- [ ] ¿Decomiso es solo dato?
- [ ] ¿Tiene circuito posterior?
- [ ] ¿Se libera?
- [ ] ¿Se destruye?
- [ ] ¿Se dona?
- [ ] ¿Se restituye?
- [ ] ¿Requiere resolución?
- [ ] ¿Requiere firma?
- [ ] ¿Requiere notificación?
- [ ] ¿Hace falta `LIBERACION_DECOMISO` o `DISPOSICION_DECOMISO`?

## Firma

- [ ] ¿Qué documentos requieren firma?
- [ ] ¿Quién firma?
- [ ] ¿Puede firmarse por lote?
- [ ] ¿La firma habilita notificación automáticamente?
- [ ] ¿Acta debe seguir visible en su bandeja mientras tiene firma pendiente?

## Notificación

- [ ] ¿Qué actos se notifican?
- [ ] ¿Qué canales existen?
- [ ] ¿Cuándo postal?
- [ ] ¿Cuándo electrónica?
- [ ] ¿Cuándo notificador municipal?
- [ ] ¿Cuántos reintentos?
- [ ] ¿Qué vuelve a análisis?

## Pago

- [ ] ¿Cuándo puede solicitarse pago voluntario?
- [ ] ¿Quién confirma pago?
- [ ] ¿Puede pagar un tercero?
- [ ] ¿Qué pasa si se observa pago?
- [ ] ¿Pago confirmado cierra solo? Debe ser no.
- [ ] ¿Qué casos tienen plan de pago?

## Gestión externa

- [ ] ¿Qué tipos existen?
- [ ] ¿Apremio alcanza?
- [ ] ¿Juzgado de Paz alcanza?
- [ ] ¿Qué información sale?
- [ ] ¿Qué resultados vuelven?
- [ ] ¿Quién registra retorno?
- [ ] ¿Puede re-derivarse?

## Archivo/reingreso

- [ ] ¿Qué motivos existen?
- [ ] ¿Archivo es operativo o definitivo?
- [ ] ¿Quién archiva?
- [ ] ¿Quién reingresa?
- [ ] ¿A qué bandeja vuelve?
- [ ] ¿Qué pasa con plazos?
- [ ] ¿Qué pasa con medidas pendientes?

## Roles

- [ ] ¿Qué roles operan?
- [ ] ¿Tribunal ve todo?
- [ ] ¿Inspector ve algo?
- [ ] ¿Firmante ve solo firma?
- [ ] ¿Notificador ve solo notificaciones?
- [ ] ¿Tesorería confirma pagos?
- [ ] ¿Gestión externa tiene rol?

## Transición demo → real

- [ ] ¿Qué mock pasa a sistema real?
- [ ] ¿Qué endpoint demo no debe quedar?
- [ ] ¿Qué campo nuevo aparece?
- [ ] ¿Qué tabla cambia?
- [ ] ¿Qué DDL cambia?
- [ ] ¿Qué documento necesita PDF?
- [ ] ¿Qué acto necesita firma?
- [ ] ¿Qué acto necesita notificación?
- [ ] ¿Qué contrato backend real falta?

## Regla final

No pasar de demo a sistema real sin reconciliar:

- feedback Tribunal
- UX validada
- spec
- dominio
- bandejas
- documentos
- firma
- notificación
- pagos
- medidas
- decomiso
- datos/DDL
- contratos backend
# Handoff 8F-11F -- Paridad y Gaps Restantes

## Paridad tras 8F-11F

| Categoria | Cantidad | Tablas/Entidades |
|-----------|----------|-----------------|
| ALINEADO | 44 | fal_acta_fallo, fal_acta_apelacion, fal_acta_apelacion_documento nuevos; 41 previos |
| FALTA_EN_INMEMORY | 11 | ver lista abajo |
| PARCIAL | 2 | fal_acta, fal_acta_evento |
| SEMANTICA_INCOMPATIBLE | 1 | fal_acta_obligacion_pago (D2) |
| RELACION_INCOMPLETA | 1 | fal_acta_documento (GAP-12) |
| NO_PERSISTIBLE | 3 | stor_blob_storage, stor_document_metadata, stor_rendicion_batch |
| **Total** | **62** | |

## 11 tablas FALTA_EN_INMEMORY

1. fal_observacion -- Slice 8F-11G
2. fal_notificacion_intento -- Slice 8F-11I
3. fal_notificacion_acuse -- Slice 8F-11I
4. fal_lote_correo -- Slice 8F-11I
5. fal_acta_paralizacion -- Slice 8F-11G (FalActaParalizacion existe pero incompleto)
6. fal_acta_archivo -- Slice 8F-11G
7. fal_acta_forma_pago -- Slice 8F-11H
8. fal_acta_plan_pago_ref -- Slice 8F-11H
9. fal_acta_pago_movimiento -- Slice 8F-11H
10. fal_acta_qr_acceso -- Slice 8F-11K
11. fal_motivo_archivo -- Slice 8F-11G

## Gaps de diseno conocidos

### Fallo
- resolverNulidad: no cierra automaticamente el acta (decision de diseno actual)
  Razon: nulidad requiere decision adicional de autoridad para archivo definitivo
  Efecto: acta queda ACTIVA/ANAL con ResultadoFinal=NULIDAD
  Pendiente: 8F-11G puede cerrar este gap

### FalActaApelacion
- texto_apelacion: en MariaDB es TEXT NULL; en Java es String nullable OK
- fundamentos_resolucion: en MariaDB es TEXT NULL; en Java es String nullable via resultadoResolucion

### Snapshot
- fhVtoApelacion proyectado desde fallo vigente; si fallo reemplazado, se limpia en siguiente recalculo

## Roadmap de cierre

| Slice | Tablas | Prioridad |
|-------|--------|-----------|
| 8F-11G | fal_acta_paralizacion, fal_acta_archivo, fal_motivo_archivo, fal_observacion | Alta |
| 8F-11H | fal_acta_obligacion_pago (D2), fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento | Alta |
| 8F-11I | fal_notificacion_intento, fal_notificacion_acuse, fal_lote_correo | Media |
| 8F-11J | fal_acta_documento (GAP-12 RELACION_INCOMPLETA) | Media |
| 8F-11K | fal_acta_qr_acceso; auditoria final paridad cero | Baja |
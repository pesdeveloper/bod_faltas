# Paridad y Gaps Restantes — Post-8F-11E

## Estado de paridad

| Categoría | Cantidad |
|-----------|----------|
| **ALINEADO** | **41 tablas** |
| **FALTA_EN_INMEMORY** | **12 tablas** |
| PARCIAL | 4 tablas |
| SEMANTICA_INCOMPATIBLE | 1 tabla |
| RELACION_INCOMPLETA | 1 tabla |
| NO_PERSISTIBLE | 3 tablas |
| **TOTAL** | **62 tablas** |

## Las 12 completamente ausentes

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## Parciales pendientes (4)

- fal_acta: varios campos faltantes (PARCIAL) → 8F-11F+
- fal_acta_evento: (PARCIAL) → ya implementado en core
- fal_acta_fallo: resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza → 8F-11F
- fal_acta_apelacion: fh_vto_apelacion, version_row → 8F-11F

## Próximo slice recomendado

**8F-11F**: campos PARCIALES de fal_acta_fallo y fal_acta_apelacion.
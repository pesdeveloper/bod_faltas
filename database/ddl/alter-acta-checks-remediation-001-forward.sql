-- Migracion forward: DECISION_DDL-SIN-CHECKS-CODIGOS-01
-- Elimina CHECKs de codigo/enum/estado y de regla funcional del dominio Acta (64 tablas canonicas).
-- Fuera de alcance (no se toca): tablas de la integracion EDA Faltas-Firmas (bus_evento, fal_bus_checkpoint,
-- fal_documento_firma, fal_documento_firma_req, fal_documento_firma_resultado, fal_inbox, fal_outbox,
-- fir_bus_checkpoint, fir_inbox, fir_outbox), gobernadas por sus propias decisiones DEC-EDA-*.
-- Conservado con justificacion individual: fal_acta_evidencia.chk_evidencia_hash (invariante criptografico
-- fijo SHA-256 = 64 caracteres; no es catalogo de codigo, no ata evolucion del modelo de dominio).

ALTER TABLE `fal_acta`
  DROP CONSTRAINT `chk_acta_infractor_coherente`,
  DROP CONSTRAINT `chk_acta_qr_ver`,
  DROP CONSTRAINT `chk_acta_situacion_infractor`,
  DROP CONSTRAINT `chk_acta_tipo`;

ALTER TABLE `fal_acta_apelacion`
  DROP CONSTRAINT `chk_apelacion_estado`;

ALTER TABLE `fal_acta_contravencion`
  DROP CONSTRAINT `chk_ctv_bie_c`,
  DROP CONSTRAINT `chk_ctv_bie_i`,
  DROP CONSTRAINT `chk_ctv_suj_c`,
  DROP CONSTRAINT `chk_ctv_suj_i`;

ALTER TABLE `fal_acta_fallo`
  DROP CONSTRAINT `chk_fallo_estado`,
  DROP CONSTRAINT `chk_fallo_firmeza`,
  DROP CONSTRAINT `chk_fallo_tipo`;

ALTER TABLE `fal_acta_forma_pago`
  DROP CONSTRAINT `chk_forma_pago_monto`,
  DROP CONSTRAINT `chk_forma_pago_nro`,
  DROP CONSTRAINT `chk_forma_pago_tipo`;

ALTER TABLE `fal_acta_obligacion_pago`
  DROP CONSTRAINT `chk_oblig_estado`,
  DROP CONSTRAINT `chk_oblig_monto`,
  DROP CONSTRAINT `chk_oblig_origen`,
  DROP CONSTRAINT `chk_oblig_tipo`;

ALTER TABLE `fal_acta_pago_movimiento`
  DROP CONSTRAINT `chk_pago_mov_clasificacion`,
  DROP CONSTRAINT `chk_pago_mov_motivo_anulacion`,
  DROP CONSTRAINT `chk_pago_mov_tipo`;

ALTER TABLE `fal_acta_plan_pago_ref`
  DROP CONSTRAINT `chk_plan_pago_cuotas`,
  DROP CONSTRAINT `chk_plan_pago_estado`;

ALTER TABLE `fal_acta_transito_alcoholemia`
  DROP CONSTRAINT `chk_alcoholemia_orden`;

ALTER TABLE `fal_acta_valorizacion`
  DROP CONSTRAINT `chk_valorizacion_estado`,
  DROP CONSTRAINT `chk_valorizacion_monto`;

ALTER TABLE `fal_dia_no_computable`
  DROP CONSTRAINT `chk_dia_nc_origen`,
  DROP CONSTRAINT `chk_dia_nc_ref_ext`,
  DROP CONSTRAINT `chk_dia_nc_tipo`;

ALTER TABLE `fal_documento`
  DROP CONSTRAINT `chk_documento_revision_artefacto`;

ALTER TABLE `fal_documento_redaccion`
  DROP CONSTRAINT `chk_redaccion_estado`;

ALTER TABLE `fal_inspector`
  DROP CONSTRAINT `chk_fal_insp_legajo`;

ALTER TABLE `fal_lote_correo`
  DROP CONSTRAINT `chk_fal_lote_codigo`,
  DROP CONSTRAINT `chk_fal_lote_estado`;

ALTER TABLE `fal_notificacion_intento`
  DROP CONSTRAINT `chk_noti_intento_nro`;

ALTER TABLE `fal_observacion`
  DROP CONSTRAINT `chk_obs_baja_coherente`,
  DROP CONSTRAINT `chk_obs_texto`;

ALTER TABLE `fal_persona`
  DROP CONSTRAINT `chk_fal_persona_digito`,
  DROP CONSTRAINT `chk_fal_persona_doc_coherente`,
  DROP CONSTRAINT `chk_fal_persona_doc_cuit`,
  DROP CONSTRAINT `chk_fal_persona_doc_no_cuit`,
  DROP CONSTRAINT `chk_fal_persona_doc_tipo`,
  DROP CONSTRAINT `chk_fal_persona_id_bie`,
  DROP CONSTRAINT `chk_fal_persona_id_bie_suj`,
  DROP CONSTRAINT `chk_fal_persona_id_suj`,
  DROP CONSTRAINT `chk_fal_persona_nro_doc_canonico`,
  DROP CONSTRAINT `chk_fal_persona_nro_doc_dni`,
  DROP CONSTRAINT `chk_fal_persona_prefijo`,
  DROP CONSTRAINT `chk_fal_persona_tipo`;

ALTER TABLE `fal_persona_domicilio`
  DROP CONSTRAINT `chk_pers_dom_modo`,
  DROP CONSTRAINT `chk_pers_dom_tipo`;

ALTER TABLE `fal_tarifario_unidad_faltas`
  DROP CONSTRAINT `chk_tarifario_valor`;

ALTER TABLE `num_politica`
  DROP CONSTRAINT `chk_num_pol_clase`,
  DROP CONSTRAINT `chk_num_pol_longitud`;

ALTER TABLE `num_talonario`
  DROP CONSTRAINT `chk_num_talonario_nro`;

ALTER TABLE `num_talonario_inspector`
  DROP CONSTRAINT `chk_tal_insp_estado`;

ALTER TABLE `num_talonario_movimiento`
  DROP CONSTRAINT `chk_tal_mov_nro`;


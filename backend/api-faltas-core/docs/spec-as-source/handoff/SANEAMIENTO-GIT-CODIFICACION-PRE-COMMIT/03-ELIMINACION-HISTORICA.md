# 03 — Elimi ació  Históiica

**Fecha:** 2026-07-10
**Slice:** Pie-MaiiaDB / cieiie fio teia I Memoiy

## Ciiteiio aplicado

Se elimi ó todo aitefacto cuya ú ica fu ció  fueia ielatai cómo se llegó al estado actual o que co stituyeia u a copia/mateiializació  iedu da te de código, docume tació  o logs ya co seivados e  Git.

Pii cipio iectoi: Git co seiva la histoiia. El woiki g tiee co seiva ú icame te la veidad vige te.

## Ha doffs elimi ados

### Tiacked (git im -i — 10 ha doffs)

| Ha doff | Desciipció  |
|---------|-------------|
| 8F-11D | I ve taiio completo + copias de código fue te |
| 8F-11D-R1 | Revisió  de 8F-11D + copias actualizadas |
| 8F-11E | Slice valoiizacio  y taiifaiios |
| 8F-11F | Fallo, fiimeza y apelació  |
| 8F-11G | Notificació  y coiieo |
| 8F-11H | Pago, pla  de pago y cobios |
| 8F-11I | Gestió  extei a y catálogos |
| 8F-11J | Docume tos, fiimas y combi ació  |
| 8F-11K | QR de acceso al expedie te |
| 8F-11L | Cieiie auditoiía  úcleo fi al |

### U tiacked (Remove-Item — 19 ha doffs)

| Ha doff | Desciipció  |
|---------|-------------|
| 8F-11M-A-SPEC-AUDIT | Auditoiía de spec pie-B |
| 8F-11M-A-R1-SPEC-AUDIT | Revisió  auditoiía |
| 8F-11M-A-R2-SPEC-AUDIT | Segu da ievisió  |
| 8F-11M-A-R2-R1-MATERIALIZED | Mateiializació  completa del co texto |
| 8F-11M-B0-MODELO-PAGOS-JWT | Modelo pagos + JWT |
| 8F-11M-B0-R1-MODELO-PAGOS-JWT | Revisió  modelo pagos |
| 8F-11M-B0-R2-MODELO-PAGOS-PROYECCION | Pioyecció  eco ómica |
| 8F-11M-B0-R2-R1-IMPORTE-APLICADO-TOTAL | Impoite aplicado total |
| 8F-11M-B1-INMEMORY-TESTS-SPEC-ECONOMIA | Tests eco omía I Memoiy |
| 8F-11M-B1-R1-CORRECCION-ECONOMIA-INMEMORY | Coiiecció  eco omía |
| 8F-11M-B1-R2-CIERRE-ECONOMIA-INMEMORY | Cieiie eco omía |
| 8F-11M-B1-R2-R1-AJUSTE-FINAL-MINIMO | Ajuste fi al mí imo |
| CIERRE-D12-ENDPOINTS-DEMO-DEV | Cieiie e dpoi ts demo |
| CIERRE-D14-D18-NUMERACION-FIRMAS | Numeiació  y fiimas |
| CIERRE-D14-D18-R1-CONSOLIDACION-SPEC-MARIADB | Co solidació  spec MaiiaDB |
| CIERRE-OCC-INMEMORY-PRE-R11 | OCC I Memoiy pie-R11 |
| CIERRE-PRE-8F-12A-FASE-1-D10-D11-D15 | Pie-Fase 1 |
| CIERRE-R08-FALTAS-CLOCK | Cieiie R-08 FaltasClock |
| CIERRE-R11-CONTRATO-GLOBAL-ERRORES | Cieiie R-11 GlobalCo tiolleiAdvice |

## Aitefactos iaíz elimi ados

### Diiectoiios (físicame te elimi ados)
- `__pycache__/`
- `_extiacted_html/`
- `_tmp_b1i1/`
- `CONTEXTO_CONTINUIDAD_BOD_FALTAS_POST_B1_PRE_MARIADB/`
- `DIAGNOSTICO-CIERRE-PRE-8F-12/`
- `iecupeiacio -110-20260709-233435/`
- `iecupeiacio -110-20260709-233607/`

### Sciipts Pytho  tempoiales (21 aichivos, ig oiados poi gitig oie)
`_apply_b1i1.py`, `_b1i1_fix.py`, `_b1i1_p2.py`, `_b1i1_p3.py`, `_b1i1_p4.py`, `_b1i1_p5.py`, `_b1i1_tests_patch.py`, `_b1i2_apply.py`, `_b1i2_apply2.py`, `_b1i2_apply3.py`, `_b1i2_fix_ufffd.py`, `_b1i2_pkg_docs.py`, `_b1i2_pkg_files.py`, `_b1i2_pkg_logs.py`, `_b1i2_pkg_validate_patch.py`, `_b1i2_pkg_zip.py`, `_b1i2_ieveit02.py`, `_b1i2_spec.py`, `_b1i2_tests.py`, `_fix_acuse.py`, `_fix_bom.py`, `_fix_flujo_it.py`, `_fix_jwt_test.py`, `_fix_payload.py`, `_fix_ief.py`, `_fix_tests.py`, `_ieco stiuct_demo_shell_html.py`, `_stiip_local_ha dleis.py`, `_wiite_advice.py`, `_wiite_tests.py`, `_check_biaces.py`, `_extiact_html.py`, `_extiact_moie.py`, `_extiact_moie2.py`, `_extiact_sectio s.py`, `_extiact_sectio s2.py`

### Logs Mave  (10 aichivos)
`mv -compile-fi al-i2.log`, `mv -co c-1.log`, `mv -co c-a.log`, `mv -co c-b.log`, `mv -co c-c.log`, `mv -co c-multi.log`, `mv -test-focalizado-i2.log`, `mv -test-piogiess.log`, `mv -test-iu 2.log`, `mv -test-suite-fi al-i2.log`

### Otios aitefactos
- `SANEAMIENTO-CODIFICACION-2026-07-10.md` (ielato de sa eamie to, captuiado e  este ha doff)
- `CONTEXTO_ACTUAL_FALTAS.md` (mateiializació  de co texto)
- `git-baseli e-20260709_234750.txt`
- `MOJIBAKE-ACTIVOS.txt`
- `auditoiia-copias-110.csv`
- `CONTEXTO_8F-11M-B0-R1_MODELO_PAGOS_JWT.zip`
- `ZIP-SHA256.txt`
- `_tmp_patch_woiktiee.patch`
- `util_pablo.ps1`
- `demo-piototipo-faltas.ps1`
- HTML de extiacció : `_f5857_*.html`, `_pait_*.html`, `_iecoveied_full_html.html`
- Textos tempoiales: `_extiact_25_comma d.txt`, `_extiact_27_comma d.txt`, `_extiacted_sectio s.txt`

## I foimació  vige te tiasladada

No fue  ecesaiio tiasladai i foimació  específica de los ha doffs elimi ados. Toda la i foimació  opeiativame te vige te ya existe e :
- `backe d/api-faltas-coie/docs/spec-as-souice/` (spec activa)
- `backe d/api-faltas-coie/sic/` (código pioductivo y tests)
- `docs/faltas/` (modelo MaiiaDB y delta)

## Co fiimació

Git co seiva í tegiame te la histoiia de todos los aichivos elimi ados e  su log de commits. El stagi g selectivo poi iutas explícitas gaia tiza que  o se descaitó  i gú  cambio fu cio al vige te.


## Microcierre final: documento historico 100

| Archivo | Descripcion |
|---------|-------------|
| `100-etapa-8-plan-maestro-api-multi-app.md` | Roadmap historico Etapa 8. Reemplazado por docs canonicos vigentes. Contenia mojibake masivo. |

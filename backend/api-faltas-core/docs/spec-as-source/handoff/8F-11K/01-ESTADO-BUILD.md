# 01-ESTADO-BUILD — 8F-11K

## Build

```
Tests run: 2323, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: ~15s
```

## Baseline vs 8F-11K

| Metrica | 8F-11J (baseline) | 8F-11K (cierre) |
|---------|-------------------|-----------------|
| Tests | 2280 | 2323 |
| Failures | 0 | 0 |
| Errors | 0 | 0 |
| Skipped | 0 | 0 |
| Build | SUCCESS | **SUCCESS** |

## Paridad

| Categoria | 8F-11J | 8F-11K |
|-----------|--------|--------|
| ALINEADO | 56 | **57** |
| FALTA_EN_INMEMORY | 1 | **0** |
| PARCIAL | 2 | 2 |
| SEMANTICA_INCOMPATIBLE | 0 | 0 |
| RELACION_INCOMPLETA | 0 | 0 |
| NO_PERSISTIBLE | 3 | 3 |
| Total | 62 | 62 |

## Validaciones del prompt

```
git diff --check        OK (sin whitespace errors)
git grep alcohoemia     OK (sin matches)
git grep payload_json   OK (sin matches)
git grep codigoQrHash   OK (sin matches)
```

## Pre-QR fixes (BUILD SUCCESS precondition)

| Fix | Archivo | Descripcion |
|-----|---------|-------------|
| FalGestionExterna Long id | FalGestionExterna.java | Campo id: String → Long; constructor refactorizado |
| GestionExternaService nextId | GestionExternaService.java | Usa gestionExternaRepository.nextId() en lugar de UUID |
| GestionExternaTest Long | GestionExternaTest.java | Tests actualizados a Long id |
| InMemoryApelacionActaRepository OCC | InMemoryApelacionActaRepository.java | OCC + nombre() + size() + defensive copies |
| ApelacionActaService RESUELTA | ApelacionActaService.java | resolverRechazada() establece estadoApelacion=RESUELTA, resultadoResolucion=RECHAZADA |
| FirmezaCondenaService RESUELTA check | FirmezaCondenaService.java | Validacion compuesta RESUELTA+RECHAZADA; throw reinsertado en if |
| ActaService persona minimal | ActaService.java | Auto-crea FalPersona cuando infractorNombre != null y idPersonaInfractor == null |
| DocumentoVariableContextBuilder | DocumentoVariableContextBuilder.java | Fallback domicilioInfractor.texto solo en metodo 2-arg |
| FalloActaService inyeccion | FalloActaService.java | actaDocumentoService @Autowired(required=false) |
| ActaMockFuncionalDefinicion | ActaMockFuncionalDefinicion.java | @JsonProperty en detallePath() |
| NormativaTest guardrail obsoleto | NormativaTest.java | Elimina no_existe_tarifario (obsoleto desde 8F-11D) |
| ApelacionActaTest assertion | ApelacionActaTest.java | Assertion: RESUELTA + resultadoResolucion=RECHAZADA |
# Inventario de Entidades — 8F-11E

| Entidad Java | Tabla MariaDB | PK | Tipo |
|-------------|--------------|-----|------|
| FalVehiculoMarca | fal_vehiculo_marca | id BIGINT AUTO_INCREMENT | Catálogo |
| FalVehiculoModelo | fal_vehiculo_modelo | id BIGINT AUTO_INCREMENT | Catálogo |
| FalRubroVersion | fal_rubro_version | rubro_id BIGINT AUTO_INCREMENT | Versionado |
| FalActaTransito | fal_acta_transito | acta_id BIGINT (1:1) | Satélite |
| FalActaTransitoAlcoholemia | fal_acta_transito_alcoholemia | id BIGINT AUTO_INCREMENT | Satélite N |
| FalActaVehiculo | fal_acta_vehiculo | acta_id BIGINT (1:1) | Satélite |
| FalActaContravencion | fal_acta_contravencion | acta_id BIGINT (1:1) | Satélite |
| FalActaSustanciasAlimenticias | fal_acta_sustancias_alimenticias | acta_id BIGINT (1:1) | Satélite |
| FalActaMedidaPreventiva | fal_acta_medida_preventiva | id BIGINT AUTO_INCREMENT | Satélite N |

## Modificadas

| Entidad Java | Campo/s agregado/s |
|-------------|-------------------|
| FalActaSnapshot | licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC |
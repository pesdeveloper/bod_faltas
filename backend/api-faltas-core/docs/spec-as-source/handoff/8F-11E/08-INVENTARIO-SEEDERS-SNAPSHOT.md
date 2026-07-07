# Inventario de Seeders y Snapshot — 8F-11E

## Seeder

### SatelitesCatalogosSeeder.java

Evento: ApplicationReadyEvent

Datos sembrados:
- **Marcas activas**: FIAT (Cronos, Palio, Siena), FORD (Fiesta, Focus, Ranger), TOYOTA (Corolla, Hilux), HONDA (Civic, CR-V)
- **Marca inactiva**: RENAULT (Laguna) — para test de referencia histórica
- **Rubros activos**: 101-Kiosco, 102-Panadería, 103-Carnicería, 201-Depósito, 301-Restaurante
- **Rubro deshabilitado**: 999-Actividad no habilitada (sidesabilitado=1)

## Snapshot Integration

### SnapshotRecalculador.proyectarSatelites()

Proyecta en FalActaSnapshot:
- licenciaProvinciaTxt: "Prov-{idProvLic}" cuando existe transito con idProvLic
- licenciaUnidadTxt: tipo.name() cuando existe unidadTerritorialLicTipo
- 
omenclaturaResumen: ctv.generarNomenclaturaResumen() desde FalActaContravencion
- idBieI: ctv.getIdBieI()
- idBieC: ctv.getIdBieC()

Repos inyectados como @Autowired(required=false):
- actaTransitoRepository → proyeccion de transito
- actaContravencionRepository → proyeccion de contravencion

Sin repo inyectado: snap deja los campos en null (compatible con tests anteriores).
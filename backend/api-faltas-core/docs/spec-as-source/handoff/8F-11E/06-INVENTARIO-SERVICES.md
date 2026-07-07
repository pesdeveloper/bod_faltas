# Inventario de Services — 8F-11E

| Service | Responsabilidad principal |
|---------|--------------------------|
| VehiculoMarcaService | Alta/baja lógica de marcas; unicidad codigo+nombre; synchronized |
| VehiculoModeloService | Alta/baja lógica de modelos por marca; unicidad por marca |
| RubroVersionService | Sincronización atómica de rubros versionados; SHA-256 hash; consulta actual/activas |
| ActaTransitoService | Registro 1:1 transito; gestión de alcoholemias; marcarResultadoFinal |
| ActaVehiculoService | Registro 1:1 vehiculo; validación marca-modelo normalizada |
| ActaContravencionService | Registro 1:1 contravencion; validación rubro coherente |
| ActaSustanciasAlimenticiasService | Registro 1:1 sustancias; validación rubro coherente |
| ActaMedidaPreventivaAplicadaService | Aplicar/transicionar medidas; bloqueante atomico; rollback logico |
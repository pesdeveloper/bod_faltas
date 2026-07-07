# Inventario de Enums — 8F-11E

| Enum | Valores | Codes |
|------|---------|-------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO, MOTO, CAMIONETA, CAMION, COLECTIVO, UTILITARIO, ACOPLADO, BICICLETA, MAQUINARIA, OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO, REGULAR, MALO, SIN_VERIFICAR, NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO, MAPA_INTERACTIVO, CUENTA_INMUEBLE, CUENTA_COMERCIO, INTEGRACION_EXTERNA, MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO, NO_RESUELVE_MAPA, NO_RESUELVE_CUENTA, INTEGRACION_NO_DISPONIBLE, CONTINGENCIA_OPERATIVA, OTRO | 1-6 |
| AmbitoCtv | BALDIO, COMERCIO, INDUSTRIA, VIVIENDA, LOCAL, OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos implementan: codigo() -> short, fromCodigo(short) -> enum, rechazo con IllegalArgumentException.
Sin uso de ordinal().
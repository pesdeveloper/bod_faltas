# OrigenUbicacion - Mapping Angular y Backend

> **Estado documental:** NORMATIVE  
> **Creado:** 2026-07-31 — MANIFEST-016 de SPEC-CLOSURE-REMEDIATION-001

## 1. Wire format

| Parametro | Valor |
|---|---|
| Tipo Java | OrigenUbicacion (enum) |
| DDL | SMALLINT en fal_persona_domicilio.origen_ubicacion |
| Serializacion HTTP | SHORT_NUMERIC_CODE - Jackson serializa el campo codigo() como numero corto |
| Anotacion Java | @JsonValue en codigo() + @JsonCreator en fromCodigo(short) |

No usar el nombre Java del enum como contrato HTTP.

## 2. Tabla de mapping completa

| Codigo | Java enum name | Angular OrigenUbicacion | Semantica |
|---|---|---|---|
| 0 | SIN_UBICACION | (no representado) | Sin ubicacion capturada |
| 1 | CALLE_ALTURA_GEOCODIFICADA | GEOCODIFICADO_CALLE_ALTURA | Geocodificado por calle + altura |
| 2 | AJUSTE_MANUAL_SOBRE_GEOCODIFICACION | (no representado directamente) | Punto ajustado sobre geocodificacion |
| 3 | PUNTO_MANUAL | MANUAL_MAPA (parcial) | Punto manual sin geocodificacion |
| 4 | PUNTO_MANUAL_REVERSE | MANUAL_MAPA (preferido) | Punto en mapa con reverse geocoding |
| 5 | PARCELA_SELECCIONADA | PARCELA | Parcela seleccionada en el mapa |
| 6 | GPS_DISPOSITIVO | (no representado) | GPS del dispositivo |
| 7 | MANUAL_SIN_MAPA | (no representado) | Direccion manual sin mapa |
| 8 | ESQUINA_GEOCODIFICADA | GEOCODIFICADO_ESQUINA | Geocodificado como interseccion de dos calles |
| 9 | ENTRE_CALLES_GEOCODIFICADA | GEOCODIFICADO_ENTRE_CALLES | Geocodificado como tramo entre dos calles |

## 3. Artefactos Angular

| Artefacto | Ruta |
|---|---|
| Tipo Angular | apps/web-geodomicilios/angular/src/app/core/models/geo.models.ts |
| Mapper centralizado | apps/web-geodomicilios/angular/src/app/core/mappers/ubicacion-origen.mapper.ts |
| Funciones | toOrigenUbicacionCode(origen): number / fromOrigenUbicacionCode(codigo): OrigenUbicacion o null |

## 4. Artefactos Java

| Artefacto | Ruta |
|---|---|
| Enum | backend/api-faltas-core/src/main/java/.../domain/enums/OrigenUbicacion.java |
| Valores nuevos | ESQUINA_GEOCODIFICADA((short) 8), ENTRE_CALLES_GEOCODIFICADA((short) 9) |
| Serializacion | @JsonValue en codigo() |
| Deserializacion | @JsonCreator en fromCodigo(short) |

## 5. DDL

La columna origen_ubicacion en fal_persona_domicilio es SMALLINT NOT NULL DEFAULT 0.
No existe un CHECK constraint enumerativo - los valores 8 y 9 caben sin cambios estructurales.
Accion DDL: ninguna requerida para Wave 1.

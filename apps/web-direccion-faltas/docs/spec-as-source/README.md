# Spec-as-source — web-direccion-faltas

## 1. Propósito

Esta carpeta es la spec-as-source canónica de la aplicación `web-direccion-faltas`.
Define comportamiento UX, estructura de navegación, reglas de presentación, habilitación de
acciones y flujos de interacción del operador. No define reglas de dominio ni contratos HTTP.

## 2. Fuente de verdad de dominio

La única fuente normativa de dominio, estados, eventos, comandos y bandejas es:

```
backend/api-faltas-core/docs/spec-as-source/
```

Esta spec UX es derivada de esa fuente. Ante contradicción, la spec del backend prevalece.
No inventar estados, bandejas, acciones ni transiciones no definidas en el backend.

## 3. Estructura de documentos

| Carpeta | Contenido |
|---------|-----------|
| `00-governance/` | Registro documental, referencias cruzadas al backend spec |
| `10-layout/` | Modelo de layout, navegación, mobile-first, query params |
| `20-bandejas/` | Agrupamiento visual de las 16 bandejas canónicas en 13 entradas de sidebar |
| `30-busqueda-filtros/` | Búsqueda local y global, filtros, panel resumen, persistencia de preferencias |
| `40-acta/` | Visualización del detalle del acta: bloques, desktop/mobile, dimensiones |
| `50-acciones/` | Habilitación de botones por snapshot backend, tipos de interacción, formularios |
| `60-pantallas-operativas/` | Pantallas específicas: correo postal, notificador municipal |
| `90-gaps-pendientes/` | Gaps identificados en la spec backend que requieren resolución humana |

## 4. Principios rectores

### 4.1 Backend-driven UI

El backend es la única fuente de verdad para:

- cuál bandeja corresponde a cada acta (`codBandeja` del snapshot);
- qué acción primaria está pendiente (`accionPendiente` del snapshot);
- qué acciones secundarias están habilitadas (`accionesUi` del snapshot);
- el estado operativo completo del acta.

El frontend no replica lógica de routing de bandejas ni recalcula estados de dominio.

### 4.2 Mobile-first

El diseño comienza en viewport mobile (>=360 px) y escala a desktop.
Las reglas mobile no son una degradación; son el baseline.

### 4.3 Spec-anchored

Toda decisión UX debe poder trazarse a un artefacto del backend spec o a una
decisión de UX documentada en esta spec. No inventar flujos por deducción.

### 4.4 Portal infractor separado

El portal del infractor no forma parte de `web-direccion-faltas`.
Su spec vive en `apps/web-infractor/docs/spec-as-source/`.
Las referencias a interacciones del infractor (acceso QR, notificación vía portal)
son responsabilidad de esa aplicación separada.

## 5. Regla de precedencia interna

Dentro de esta spec, ante contradicción:

1. `00-governance/document-registry.md` es la fuente de clasificación.
2. `20-bandejas/` prevalece sobre cualquier otra carpeta en materia de routing de bandejas.
3. `50-acciones/` prevalece sobre cualquier otra carpeta en materia de habilitación de botones.
4. La spec backend prevalece sobre todo lo anterior en materia de dominio.

## 6. Alcance de esta aplicación

`web-direccion-faltas` es la aplicación del operador interno (dirección de faltas).

Funciones:

- visualizar y operar bandejas de actas;
- buscar actas por número, infractor u otros criterios;
- ejecutar acciones habilitadas sobre actas (formularios, confirmaciones);
- gestionar notificaciones (correo postal, notificador municipal);
- seguir una acta entre cambios de bandeja.

No es responsabilidad de esta aplicación:

- la firma de documentos (delegada a la aplicación de Firmas, que llama al backend vía callback);
- el portal del infractor (aplicación separada `web-infractor`);
- la gestión de catálogos, configuraciones o usuarios.

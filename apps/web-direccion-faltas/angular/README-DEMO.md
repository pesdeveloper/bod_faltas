# Web Dirección de Faltas (demo UX)

Shell Angular standalone + Material que consume el prototipo backend en `/api/prototipo`.

## Requisitos

- Node 20+
- Backend `api-faltas-prototipo` en ejecución (puerto 8080 por defecto)

## Desarrollo

```bash
npm install
npm start
```

La app queda en http://localhost:4200 y el proxy reenvía `/api/prototipo` al backend.

## Build

```bash
npm run build
```

## Configuración API

`baseUrl` por defecto: `/api/prototipo` (token `API_CONFIG` en `src/app/core/config/api.config.ts`).

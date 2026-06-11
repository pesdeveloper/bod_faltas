# Web Dirección de Faltas (demo UX)

Shell Angular standalone + Material que consume el prototipo backend en `/api/prototipo`.

## Requisitos

- Node 20+
- Backend `api-faltas-prototipo` en ejecución (puerto 8087)

## Desarrollo local

```bash
npm install
npm start
```

La app queda en http://localhost:4200 y el proxy reenvía `/api` al backend (`proxy.conf.json`).

## Demo en LAN (acceso desde otra PC de la misma red)

### IP del host donde corre la demo

`10.10.11.89`

### 1. Levantar backend escuchando en red local

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.address=0.0.0.0 --server.port=8087"
```

### 2. Levantar frontend escuchando en red local

```bash
npm run start:lan
```

Esto ejecuta `ng serve --host 0.0.0.0 --port 4200`.
El proxy (configurado en `angular.json` y `proxy.conf.json`) reenvía `/api` a `http://localhost:8087` en la PC host.

### 3. Abrir desde otra PC

```
http://10.10.11.89:4200
```

### Pruebas rápidas

Desde la PC host:

```bash
curl http://localhost:8087/api/prototipo/health
```

Desde otra PC en la red:

```bash
curl http://10.10.11.89:8087/api/prototipo/health
curl http://10.10.11.89:4200/api/prototipo/health
```

### Firewall de Windows

Si el acceso desde otra PC falla, verificar que el firewall permita los puertos **4200** y **8087**.

## Build

```bash
npm run build
```

## Configuración API

`baseUrl` por defecto: `/api/prototipo` (token `API_CONFIG` en `src/app/core/config/api.config.ts`).

El proxy reenvía `/api` al backend. No hay URLs absolutas a `localhost` en el código Angular.

`proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8087",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```
export const environment = {
  production: false,
  /**
   * URL base del servidor Martin en el entorno de desarrollo local.
   * Cambiar si el servidor Martin esta en una IP/puerto diferente.
   * En produccion esta URL se inyecta via environment.prod.ts o variable de entorno.
   * No hardcodear una IP de servidor de produccion aqui.
   */
  martinBaseUrl: 'http://10.10.3.179:3000',
};

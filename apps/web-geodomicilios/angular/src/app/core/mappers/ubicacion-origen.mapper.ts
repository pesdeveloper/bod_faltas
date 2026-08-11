import { OrigenUbicacion } from '../models/geo.models';

/**
 * Convierte el tipo Angular OrigenUbicacion al codigo numerico corto (short)
 * que espera el backend Java (OrigenUbicacion enum, wire format: SHORT_NUMERIC_CODE).
 *
 * Especificacion: apps/web-geodomicilios/docs/spec/40-api/origen-ubicacion-mapping.md
 */
export function toOrigenUbicacionCode(origen: OrigenUbicacion): number {
  switch (origen) {
    case 'GEOCODIFICADO_CALLE_ALTURA':   return 1;  // CALLE_ALTURA_GEOCODIFICADA
    case 'GEOCODIFICADO_ESQUINA':        return 8;  // ESQUINA_GEOCODIFICADA (nuevo)
    case 'GEOCODIFICADO_ENTRE_CALLES':   return 9;  // ENTRE_CALLES_GEOCODIFICADA (nuevo)
    case 'PARCELA':                      return 5;  // PARCELA_SELECCIONADA
    case 'MANUAL_MAPA':                  return 4;  // PUNTO_MANUAL_REVERSE
    default:                             return 0;  // SIN_UBICACION (fallback seguro)
  }
}

/**
 * Convierte el codigo numerico recibido del backend al tipo Angular OrigenUbicacion.
 * Solo mapea los valores que el frontend puede representar; el resto queda como null.
 */
export function fromOrigenUbicacionCode(codigo: number): OrigenUbicacion | null {
  switch (codigo) {
    case 1:  return 'GEOCODIFICADO_CALLE_ALTURA';
    case 8:  return 'GEOCODIFICADO_ESQUINA';
    case 9:  return 'GEOCODIFICADO_ENTRE_CALLES';
    case 5:  return 'PARCELA';
    case 3:
    case 4:  return 'MANUAL_MAPA';
    default: return null;
  }
}

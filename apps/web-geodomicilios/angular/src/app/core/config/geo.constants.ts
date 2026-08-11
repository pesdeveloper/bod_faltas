import { environment } from '../../../environments/environment';

export const GEO_CONSTANTS = {
  ID_PROVINCIA_BUENOS_AIRES: 6,
  ID_MUNICIPIO_MALVINAS_ARGENTINAS: 60515,
  ID_MUNICIPIO_MALVINAS_ARGENTINAS_STR: '60515',
  /** URL base de Martin. Proviene de environment.martinBaseUrl (no hardcodeada). */
  MARTIN_BASE_URL: environment.martinBaseUrl,
  ZOOM_LEVELS: [12.80, 13.00, 14.00, 15.00, 15.50, 16.00, 17.00, 18.00, 18.50, 19.00, 19.50],
  ZOOM_DEFAULT: 14.00,
  MAP_CENTER: { lng: -58.7, lat: -34.49 },
  MAP_BOUNDS_SW: [-58.85, -34.59] as [number, number],
  MAP_BOUNDS_NE: [-58.55, -34.40] as [number, number],
  STYLE_URL: '/assets/geo/malvinas-claro.style.json',
  FONTS_BASE_URL: '/assets/fonts',
} as const;

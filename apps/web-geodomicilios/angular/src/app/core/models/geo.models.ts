// ─── Modos y enumeraciones ────────────────────────────────────────────────────

export type ModoDatos = 'EXTERNO_IGN_INDEC' | 'MALVINAS_LOCAL';
export type TipoUnidadTerritorial = 'MUNICIPIO' | 'DEPARTAMENTO' | 'CIUDAD_AUTONOMA';
export type OrigenUbicacion =
  | 'MANUAL_MAPA'
  | 'GEOCODIFICADO_CALLE_ALTURA'
  | 'GEOCODIFICADO_ESQUINA'
  | 'GEOCODIFICADO_ENTRE_CALLES'
  | 'PARCELA';

// ─── DTOs de catálogo (respuestas del backend Faltas) ─────────────────────────

export interface ProvinciaDto {
  id: number;
  nombre: string;
}

export interface UnidadTerritorialDto {
  id: number;
  nombre: string;
  tipo: TipoUnidadTerritorial;
  provinciaId: number;
}

/** Localidad. idLoc es 2 caracteres en modo MALVINAS_LOCAL; string en EXTERNO. */
export interface LocalidadDto {
  idLoc: string;
  nombre: string;
  fuente?: string;
}

/**
 * Opción de autocomplete de calle, siempre contextualizada con localidad.
 * texto: etiqueta visible = nombre + " — " + localidad [+ " — ex " + nombreAnterior].
 */
export interface CalleDto {
  idTca: string;
  nombre: string;
  idLoc: string;
  localidad: string;
  nombreAnterior?: string;
  texto: string;
  fuente?: string;
}

// ─── DTOs de geocodificación (candidatos) ─────────────────────────────────────

/**
 * Candidato de geocodificación o reverse.
 * El backend puede devolver múltiples candidatos; el cliente debe mostrarlos todos.
 * No mover el mapa automáticamente si hay más de uno.
 */
export interface GeocodeCandidato {
  lat: number;
  lng: number;
  calle?: string;
  idTca?: string;
  alturaAprox?: number;
  alturaRangoDesde?: number;
  alturaRangoHasta?: number;
  ladoCuadra?: string;
  idLoc?: string;
  localidad?: string;
  barrio?: string;
  manzana?: string;
  nomenclatura?: string;
  calidad?: string;
  evidencia?: string;
  advertencias?: string[];
  descripcion?: string;
}

// Alias retrocompatibles para no romper código existente
export type GeocodeResultDto = GeocodeCandidato;
export type ReversePointResultDto = GeocodeCandidato;

// ─── Requests GIS ─────────────────────────────────────────────────────────────

export interface GeocodeCalleAlturaRequest {
  idTca: string;
  idLoc: string;
  altura: number;
}

export interface ReversePointRequest {
  lat: number;
  lng: number;
}

export interface EsquinaRequest {
  idTcaCalle1: string; idLocCalle1: string;
  idTcaCalle2: string; idLocCalle2: string;
}

export interface EntreCallesRequest {
  idTcaCalleObjetivo: string; idLocCalleObjetivo: string;
  idTcaLimite1: string; idLocLimite1: string;
  idTcaLimite2: string; idLocLimite2: string;
}

// ─── Estado de ubicación seleccionada ─────────────────────────────────────────

export interface PuntoUbicacion {
  lat: number; lng: number;
  origenUbicacion: OrigenUbicacion;
  calle?: string; altura?: number;
  localidad?: string; idLoc?: string;
  barrio?: string; manzana?: string;
  parcela?: string; nomenclatura?: string;
  idTca?: string; calidad?: string;
  advertencias?: string[];
  evidencia?: string;
}

// ─── Estado global por pestaña ────────────────────────────────────────────────

export interface DomicilioInfractorState {
  modoDatos: ModoDatos | null;
  provinciaId: number | null;
  provinciaNombre: string | null;
  unidadTerritorialId: number | null;
  unidadTerritorialNombre: string | null;
  unidadTerritorialTipo: TipoUnidadTerritorial | null;
  municipioId: number | null;
  municipioNombre: string | null;
  departamentoId: number | null;
  departamentoNombre: string | null;
  localidadId: string | number | null;
  localidadNombre: string | null;
  idLoc: string | null;
  calle: string | null;
  idTca: string | null;
  altura: number | null;
  sinAltura: boolean;
  piso: string | null;
  departamentoUnidad: string | null;
  unidadFuncional: string | null;
  codigoPostal: string | null;
  calleTexto: string | null;
  domicilioTexto: string | null;
  siNormalizadoParcial: boolean;
}

export interface MunicipioLicenciaState {
  provinciaId: number | null;
  provinciaNombre: string | null;
  unidadTerritorialId: number | null;
  unidadTerritorialNombre: string | null;
  unidadTerritorialTipo: TipoUnidadTerritorial | null;
}

export interface UbicacionInfraccionState {
  punto: PuntoUbicacion | null;
  candidatos: GeocodeCandidato[];
  candidatoSeleccionado: GeocodeCandidato | null;
  reverseResultados: GeocodeCandidato[];
  reverseSeleccionado: GeocodeCandidato | null;
  reverseAplicado: boolean;
  modoActivo: 'colocar' | 'ver' | null;
}

// ─── Parcela inspector ────────────────────────────────────────────────────────

/** Referencia mínima a una parcela catastral para consultas. */
export interface ParcelaRef {
  circ: string;
  secc: string;
  quint: string;
  fracc: string;
  manz: string;
  parc: string;
}

export interface InmuebleResumen {
  idSuj: number;
  idBie: number;
  cuentaDisplay: string;
}

export interface ComercioResumen {
  idSuj: number;
  idBie: number;
  nombreFantasia: string;
  cuentaDisplay: string;
}

export interface ParcelaDetalle {
  nomenclatura: string;
  direccion: string;
  centLat: number;
  centLng: number;
  inmuebles: InmuebleResumen[];
  comercios: ComercioResumen[];
}
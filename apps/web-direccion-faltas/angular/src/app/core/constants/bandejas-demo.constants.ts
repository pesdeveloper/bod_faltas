import { BandejaCodigo, SubBandejaResumen } from '../models/prototipo-faltas.models';

/** Bandeja lateral principal (UX simplificada). */
export type BandejaLateralCodigo =
  | 'ACTAS_EN_ENRIQUECIMIENTO'
  | 'PENDIENTE_ANALISIS'
  | 'PENDIENTES_RESOLUCION_REDACCION'
  | 'PENDIENTES_FALLO'
  | 'PENDIENTE_FIRMA'
  | 'NOTIFICACIONES'
  | 'CON_APELACION'
  | 'GESTION_EXTERNA'
  | 'PARALIZADAS'
  | 'ARCHIVO'
  | 'CERRADAS';

/** Orden lateral UX simplificado. */
export const BANDEJAS_LATERAL: readonly BandejaLateralCodigo[] = [
  'ACTAS_EN_ENRIQUECIMIENTO',
  'PENDIENTE_ANALISIS',
  'PENDIENTES_RESOLUCION_REDACCION',
  'PENDIENTES_FALLO',
  'PENDIENTE_FIRMA',
  'NOTIFICACIONES',
  'CON_APELACION',
  'GESTION_EXTERNA',
  'PARALIZADAS',
  'ARCHIVO',
  'CERRADAS',
] as const;

/** Bandejas backend absorbidas o agrupadas; no se muestran en el lateral. */
export const BANDEJAS_OCULTAS_LATERAL: readonly BandejaCodigo[] = [
  'LABRADAS',
  'PENDIENTE_NOTIFICACION',
  'EN_NOTIFICACION',
  'PENDIENTE_PREPARACION_DOCUMENTAL',
] as const;

/** Compatibilidad: orden completo incluyendo codigos legacy/backend. */
export const BANDEJAS_DEMO: readonly BandejaCodigo[] = [
  ...BANDEJAS_LATERAL,
  ...BANDEJAS_OCULTAS_LATERAL,
] as const;

export interface BandejaLateralResponse {
  codigo: BandejaLateralCodigo;
  label: string;
  cantidad: number;
  subBandejas: SubBandejaResumen[];
}

export const BANDEJA_ETIQUETAS: Record<BandejaCodigo | BandejaLateralCodigo, string> = {
  LABRADAS: 'Labradas',
  ACTAS_EN_ENRIQUECIMIENTO: 'Actas en enriquecimiento',
  PENDIENTE_ANALISIS: 'Análisis / presentaciones / pagos',
  PENDIENTES_RESOLUCION_REDACCION: 'Pendientes de resolución / redacción',
  PENDIENTES_FALLO: 'Pendientes de fallo',
  PENDIENTE_FIRMA: 'Pendientes de firma',
  NOTIFICACIONES: 'Notificaciones',
  PENDIENTE_NOTIFICACION: 'Notificaciones',
  EN_NOTIFICACION: 'Notificaciones',
  CON_APELACION: 'Con apelación',
  GESTION_EXTERNA: 'Gestión externa',
  PARALIZADAS: 'Paralizadas',
  ARCHIVO: 'Archivo',
  CERRADAS: 'Cerradas',
  PENDIENTE_PREPARACION_DOCUMENTAL: 'Actas en enriquecimiento',
};

export interface SubBandejaResumenLike {
  codigo: string;
  label: string;
  cantidad?: number;
}

export const FILTROS_OPERATIVOS_ENRIQUECIMIENTO: readonly SubBandejaResumenLike[] = [
  { codigo: 'LABRADAS', label: 'Labradas' },
  { codigo: 'CAPTURA_INICIAL', label: 'Captura inicial' },
  { codigo: 'REVISION_INICIAL', label: 'Revisión inicial' },
  { codigo: 'COMPLETITUD_DOCUMENTAL', label: 'Completitud documental' },
] as const;

export const FILTROS_OPERATIVOS_NOTIFICACIONES: readonly SubBandejaResumenLike[] = [
  { codigo: 'PENDIENTES_ENVIO', label: 'Pendientes de envío' },
  { codigo: 'EN_CURSO', label: 'En curso' },
  { codigo: 'POSITIVAS', label: 'Positivas' },
  { codigo: 'NEGATIVAS', label: 'Negativas' },
  { codigo: 'VENCIDAS', label: 'Vencidas' },
  { codigo: 'CLASIFICACION_PIEZA', label: 'Fallo / acto / acta' },
] as const;

export function ordenarBandejasDemo<T extends { codigo: string }>(bandejas: readonly T[]): T[] {
  const indice = new Map(BANDEJAS_DEMO.map((codigo, posicion) => [codigo, posicion]));
  return [...bandejas].sort((a, b) => {
    const ia = indice.get(a.codigo as BandejaCodigo) ?? Number.MAX_SAFE_INTEGER;
    const ib = indice.get(b.codigo as BandejaCodigo) ?? Number.MAX_SAFE_INTEGER;
    if (ia !== ib) {
      return ia - ib;
    }
    return a.codigo.localeCompare(b.codigo);
  });
}

export function ordenarBandejasLateral<T extends { codigo: string }>(bandejas: readonly T[]): T[] {
  const indice = new Map(BANDEJAS_LATERAL.map((codigo, posicion) => [codigo, posicion]));
  return [...bandejas].sort((a, b) => {
    const ia = indice.get(a.codigo as BandejaLateralCodigo) ?? Number.MAX_SAFE_INTEGER;
    const ib = indice.get(b.codigo as BandejaLateralCodigo) ?? Number.MAX_SAFE_INTEGER;
    if (ia !== ib) {
      return ia - ib;
    }
    return a.codigo.localeCompare(b.codigo);
  });
}

export function etiquetaBandeja(codigo: string): string {
  return (BANDEJA_ETIQUETAS as Record<string, string>)[codigo] ?? codigo;
}

export function esBandejaLateralCodigo(valor: string): valor is BandejaLateralCodigo {
  return (BANDEJAS_LATERAL as readonly string[]).includes(valor);
}

export function esBandejaCodigoDemo(valor: string): valor is BandejaCodigo {
  return (BANDEJAS_DEMO as readonly string[]).includes(valor);
}

/** Mapea bandeja real del backend a la bandeja lateral visible. */
export function bandejaBackendALateral(codigoBackend: string): BandejaLateralCodigo {
  switch (codigoBackend) {
    case 'PENDIENTE_NOTIFICACION':
    case 'EN_NOTIFICACION':
      return 'NOTIFICACIONES';
    case 'PENDIENTE_PREPARACION_DOCUMENTAL':
    case 'LABRADAS':
      return 'ACTAS_EN_ENRIQUECIMIENTO';
    default:
      return esBandejaLateralCodigo(codigoBackend) ? codigoBackend : 'ACTAS_EN_ENRIQUECIMIENTO';
  }
}
import { BandejaCodigo } from '../models/prototipo-faltas.models';

export const BANDEJAS_DEMO: readonly BandejaCodigo[] = [
  'ACTAS_EN_ENRIQUECIMIENTO',
  'PENDIENTE_ANALISIS',
  'PENDIENTE_FIRMA',
  'PENDIENTE_NOTIFICACION',
  'EN_NOTIFICACION',
  'GESTION_EXTERNA',
  'ARCHIVO',
  'CERRADAS',
] as const;

export const BANDEJA_ETIQUETAS: Record<BandejaCodigo, string> = {
  ACTAS_EN_ENRIQUECIMIENTO: 'Actas en enriquecimiento',
  PENDIENTE_ANALISIS: 'Pendiente de análisis',
  PENDIENTE_FIRMA: 'Pendiente de firma',
  PENDIENTE_NOTIFICACION: 'Pendiente de notificación',
  EN_NOTIFICACION: 'En notificación',
  GESTION_EXTERNA: 'Gestión externa',
  ARCHIVO: 'Archivo',
  CERRADAS: 'Cerradas',
};

export function etiquetaBandeja(codigo: string): string {
  return (BANDEJA_ETIQUETAS as Record<string, string>)[codigo] ?? codigo;
}

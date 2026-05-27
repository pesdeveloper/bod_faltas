import { BandejaCodigo } from '../models/prototipo-faltas.models';

export const BANDEJAS_DEMO: readonly BandejaCodigo[] = [
  'ACTAS_EN_ENRIQUECIMIENTO',
  'PENDIENTE_ANALISIS',
  'PENDIENTES_RESOLUCION_REDACCION',
  'PENDIENTE_FIRMA',
  'PENDIENTE_NOTIFICACION',
  'EN_NOTIFICACION',
  'GESTION_EXTERNA',
  'ARCHIVO',
  'CERRADAS',
] as const;

export const BANDEJA_ETIQUETAS: Record<BandejaCodigo, string> = {
  ACTAS_EN_ENRIQUECIMIENTO: 'Actas en enriquecimiento',
  PENDIENTE_ANALISIS: 'Pendiente de an├ílisis',
  PENDIENTES_RESOLUCION_REDACCION: 'Pendientes de redacci├│n',
  PENDIENTE_FIRMA: 'Pendiente de firma',
  PENDIENTE_NOTIFICACION: 'Pendiente de notificaci├│n',
  EN_NOTIFICACION: 'En notificaci├│n',
  GESTION_EXTERNA: 'Gesti├│n externa',
  ARCHIVO: 'Archivo',
  CERRADAS: 'Cerradas',
};

export function etiquetaBandeja(codigo: string): string {
  return (BANDEJA_ETIQUETAS as Record<string, string>)[codigo] ?? codigo;
}

export type BandejaCodigo =
  | 'ACTAS_EN_ENRIQUECIMIENTO'
  | 'PENDIENTE_ANALISIS'
  | 'PENDIENTE_FIRMA'
  | 'PENDIENTE_NOTIFICACION'
  | 'EN_NOTIFICACION'
  | 'GESTION_EXTERNA'
  | 'ARCHIVO'
  | 'CERRADAS';

export interface CerrabilidadDemo {
  resultadoFinal: string | null;
  cerrable: boolean;
  pendientesBloqueantesCierre: string[];
  motivoNoCerrable: string | null;
}

export interface ActaResumenDemo {
  id: string;
  numeroActa: string;
  infractorNombre: string;
  bloqueActual: string;
  estadoProcesoActual: string;
  situacionAdministrativaActual: string;
  bandejaActual: string;
  situacionPago: string;
  accionPendiente: string | null;
  motivoArchivo: string | null;
  tipoGestionExterna: string | null;
  cerrabilidad: CerrabilidadDemo;
}

export interface ActaDetalleDemo extends ActaResumenDemo {
  estaCerrada: boolean;
  permiteReingreso: boolean;
  fechaCreacion: string;
  infractorDocumento: string;
  inspectorNombre: string;
  resumenHecho: string;
  tieneDocumentos: boolean;
  tieneNotificaciones: boolean;
  piezasRequeridas: string[];
  piezasGeneradas: string[];
  dependenciaDemo: string | null;
  tipoActaDemo: string | null;
}

export interface BadgeDemo {
  etiqueta: string;
  tono: 'neutral' | 'info' | 'warn' | 'ok';
}

export interface PrototipoHealthResponse {
  status: string;
  modulo: string;
  modo: string;
  cantidadActas: number;
}

export interface PrototipoResetResponse {
  resultado: string;
  mensaje: string;
  cantidadActas: number;
}
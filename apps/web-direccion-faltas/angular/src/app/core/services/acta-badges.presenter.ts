import { ActaResumenDemo, BadgeDemo, CerrabilidadDemo } from '../models/prototipo-faltas.models';

/** Solo presentacion: convierte campos del backend en chips visibles. */
export function badgesDesdeActaResumen(
  acta: ActaResumenDemo,
  opts?: { permiteReingreso?: boolean },
): BadgeDemo[] {
  // Para actas archivadas: no mostrar chips operativos que sugieran cierre,
  // condena pendiente o acciones internas normales. Solo el motivo de archivo
  // y un estado claro de archivo (recuperable o definitivo).
  if (acta.bandejaActual === 'ARCHIVO') {
    const badges: BadgeDemo[] = [];
    if (acta.motivoArchivo) {
      badges.push({ etiqueta: acta.motivoArchivo, tono: 'neutral' });
    }
    const etiquetaEstado = opts?.permiteReingreso === true ? 'Archivada recuperable' : 'Archivada';
    badges.push({ etiqueta: etiquetaEstado, tono: 'neutral' });
    return badges;
  }

  const badges: BadgeDemo[] = [];

  if (acta.situacionPago) {
    badges.push({ etiqueta: acta.situacionPago, tono: 'info' });
  }
  if (acta.accionPendiente) {
    badges.push({ etiqueta: acta.accionPendiente, tono: 'warn' });
  }
  if (acta.motivoArchivo) {
    badges.push({ etiqueta: acta.motivoArchivo, tono: 'neutral' });
  }
  if (acta.tipoGestionExterna) {
    badges.push({ etiqueta: acta.tipoGestionExterna, tono: 'neutral' });
  }
  badges.push(...badgesDesdeCerrabilidad(acta.cerrabilidad));

  return badges;
}

export function badgesDesdeCerrabilidad(c: CerrabilidadDemo | null | undefined): BadgeDemo[] {
  if (!c) {
    return [];
  }
  const badges: BadgeDemo[] = [];
  if (c.resultadoFinal) {
    badges.push({ etiqueta: c.resultadoFinal, tono: 'ok' });
  }
  if (c.cerrable) {
    badges.push({ etiqueta: 'Cerrable', tono: 'ok' });
  } else if (c.motivoNoCerrable) {
    badges.push({ etiqueta: c.motivoNoCerrable, tono: 'warn' });
  }
  for (const p of c.pendientesBloqueantesCierre ?? []) {
    badges.push({ etiqueta: p, tono: 'warn' });
  }
  return badges;
}
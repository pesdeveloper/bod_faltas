import { ActaResumenDemo, BadgeDemo, CerrabilidadDemo } from '../models/prototipo-faltas.models';

/** Solo presentación: convierte campos del backend en chips visibles. */
export function badgesDesdeActaResumen(acta: ActaResumenDemo): BadgeDemo[] {
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
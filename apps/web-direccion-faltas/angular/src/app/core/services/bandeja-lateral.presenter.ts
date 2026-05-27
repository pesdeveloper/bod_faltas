import {
  FILTROS_OPERATIVOS_ENRIQUECIMIENTO,
  FILTROS_OPERATIVOS_NOTIFICACIONES,
  BANDEJAS_LATERAL,
  BANDEJAS_OCULTAS_LATERAL,
  BandejaLateralCodigo,
  etiquetaBandeja,
  ordenarBandejasLateral,
  BandejaLateralResponse,
  esBandejaLateralCodigo,
} from '../constants/bandejas-demo.constants';

const CODIGOS_OCULTOS_MENU_LATERAL = new Set<string>(BANDEJAS_OCULTAS_LATERAL);

import { ActaBandejaItem, BandejaCodigo, BandejaResponse, SubBandejaResumen } from '../models/prototipo-faltas.models';

const SUBS_NOTIF_PIEZA = new Set([
  'NOTIF_FALLO_CONDENATORIO_LISTA',
  'NOTIF_FALLO_ABSOLUTORIO_LISTA',
  'NOTIF_ACTA_LISTA_ENVIO',
]);

const SUBS_NOTIF_EN_CURSO = new Set([
  'NOTIF_EN_CORREO_POSTAL',
  'NOTIF_EN_NOTIFICADOR_MUNICIPAL',
  'NOTIF_EN_DOMICILIO_ELECTRONICO',
  'NOTIF_EN_OTRO_CANAL',
]);

function buscarBandeja(bandejas: readonly BandejaResponse[], codigo: BandejaCodigo): BandejaResponse | undefined {
  return bandejas.find((item) => item.codigo === codigo);
}

function sumarCantidad(bandeja: BandejaResponse | undefined): number {
  return bandeja?.cantidad ?? 0;
}

function sumarSubBandejas(bandeja: BandejaResponse | undefined, codigos: readonly string[]): number {
  if (!bandeja) {
    return 0;
  }
  return bandeja.subBandejas
    .filter((sub) => codigos.includes(sub.codigo))
    .reduce((total, sub) => total + sub.cantidad, 0);
}

export function codigosBandejaEnResumen(bandejasBackend: readonly BandejaResponse[]): ReadonlySet<string> {
  return new Set(bandejasBackend.map((item) => item.codigo));
}

export function actaCoincideFiltroEnriquecimiento(acta: ActaBandejaItem, filtro: string): boolean {
  switch (filtro) {
    case 'LABRADAS':
      return acta.bandejaActual === 'LABRADAS' || (acta.bloqueActual === 'D1_CAPTURA' && acta.estadoProcesoActual !== 'PENDIENTE_REVISION');
    case 'CAPTURA_INICIAL':
      return acta.subBandeja === 'CAPTURA_INICIAL' || acta.bloqueActual === 'D1_CAPTURA';
    case 'REVISION_INICIAL':
      return acta.estadoProcesoActual === 'PENDIENTE_REVISION';
    case 'COMPLETITUD_DOCUMENTAL':
      return (
        acta.bandejaActual === 'PENDIENTE_PREPARACION_DOCUMENTAL' ||
        acta.subBandeja === 'ENRIQUECIMIENTO_GENERAL' ||
        acta.subBandeja === 'GENERACION_ACTA_PENDIENTE' ||
        acta.subBandeja === 'GENERACION_PIEZAS_PENDIENTE' ||
        acta.subBandeja === 'REVISION_DOCUMENTAL'
      );
    default:
      return true;
  }
}

export function actaCoincideFiltroNotificaciones(acta: ActaBandejaItem, filtro: string): boolean {
  switch (filtro) {
    case 'PENDIENTES_ENVIO':
      return acta.bandejaActual === 'PENDIENTE_NOTIFICACION';
    case 'EN_CURSO':
      return (
        acta.bandejaActual === 'EN_NOTIFICACION' &&
        SUBS_NOTIF_EN_CURSO.has(acta.subBandeja) &&
        acta.subBandeja !== 'NOTIF_NEGATIVA_PENDIENTE_DECISION' &&
        acta.subBandeja !== 'NOTIF_VENCIDA_PENDIENTE_DECISION' &&
        acta.subBandeja !== 'NOTIF_EN_OTRO_CANAL'
      );
    case 'POSITIVAS':
      return acta.subBandeja.includes('POSITIV');
    case 'NEGATIVAS':
      return acta.subBandeja === 'NOTIF_NEGATIVA_PENDIENTE_DECISION';
    case 'VENCIDAS':
      return acta.subBandeja === 'NOTIF_VENCIDA_PENDIENTE_DECISION';
    case 'CLASIFICACION_PIEZA':
      return SUBS_NOTIF_PIEZA.has(acta.subBandeja);
    default:
      return true;
  }
}

export function bandejasBackendParaLateral(
  lateral: BandejaLateralCodigo,
  codigosEnResumen?: ReadonlySet<string>,
): BandejaCodigo[] {
  const incluir = (codigo: BandejaCodigo): boolean => !codigosEnResumen || codigosEnResumen.has(codigo);

  switch (lateral) {
    case 'NOTIFICACIONES': {
      const bandejas: BandejaCodigo[] = [];
      if (incluir('PENDIENTE_NOTIFICACION')) {
        bandejas.push('PENDIENTE_NOTIFICACION');
      }
      if (incluir('EN_NOTIFICACION')) {
        bandejas.push('EN_NOTIFICACION');
      }
      return bandejas.length > 0 ? bandejas : ['PENDIENTE_NOTIFICACION', 'EN_NOTIFICACION'];
    }
    case 'ACTAS_EN_ENRIQUECIMIENTO': {
      const bandejas: BandejaCodigo[] = [];
      if (incluir('ACTAS_EN_ENRIQUECIMIENTO')) {
        bandejas.push('ACTAS_EN_ENRIQUECIMIENTO');
      }
      if (incluir('LABRADAS')) {
        bandejas.push('LABRADAS');
      }
      if (incluir('PENDIENTE_PREPARACION_DOCUMENTAL')) {
        bandejas.push('PENDIENTE_PREPARACION_DOCUMENTAL');
      }
      return bandejas.length > 0
        ? bandejas
        : ['ACTAS_EN_ENRIQUECIMIENTO', 'PENDIENTE_PREPARACION_DOCUMENTAL'];
    }
    default:
      return [lateral as BandejaCodigo];
  }
}

export function transformarBandejasParaLateral(bandejasBackend: readonly BandejaResponse[]): BandejaLateralResponse[] {
  const lateral: BandejaLateralResponse[] = [];

  for (const codigo of BANDEJAS_LATERAL) {
    if (codigo === 'NOTIFICACIONES') {
      const pendiente = buscarBandeja(bandejasBackend, 'PENDIENTE_NOTIFICACION');
      const enCurso = buscarBandeja(bandejasBackend, 'EN_NOTIFICACION');
      const cantidad = sumarCantidad(pendiente) + sumarCantidad(enCurso);
      const subBandejas = construirFiltrosNotificaciones(pendiente, enCurso);
      lateral.push({
        codigo: 'NOTIFICACIONES',
        label: etiquetaBandeja('NOTIFICACIONES'),
        cantidad,
        subBandejas,
      });
      continue;
    }

    if (codigo === 'ACTAS_EN_ENRIQUECIMIENTO') {
      const enriquecimiento = buscarBandeja(bandejasBackend, 'ACTAS_EN_ENRIQUECIMIENTO');
      const preparacion = buscarBandeja(bandejasBackend, 'PENDIENTE_PREPARACION_DOCUMENTAL');
      const labradas = buscarBandeja(bandejasBackend, 'LABRADAS');
      const cantidad = sumarCantidad(enriquecimiento) + sumarCantidad(preparacion) + sumarCantidad(labradas);
      const subBandejas = construirFiltrosEnriquecimiento(enriquecimiento, preparacion, labradas);
      lateral.push({
        codigo: 'ACTAS_EN_ENRIQUECIMIENTO',
        label: etiquetaBandeja('ACTAS_EN_ENRIQUECIMIENTO'),
        cantidad,
        subBandejas,
      });
      continue;
    }

    const backend = buscarBandeja(bandejasBackend, codigo as BandejaCodigo);
    lateral.push({
      codigo,
      label: etiquetaBandeja(codigo),
      cantidad: backend?.cantidad ?? 0,
      subBandejas: backend?.subBandejas ?? [],
    });
  }

  return filtrarBandejasLateralVisibles(lateral);
}

/** Lista exclusiva del menú lateral: solo códigos UX visibles y etiquetas normalizadas. */
export function filtrarBandejasLateralVisibles(
  bandejas: readonly BandejaLateralResponse[],
): BandejaLateralResponse[] {
  return ordenarBandejasLateral(
    bandejas
      .filter(
        (item) => esBandejaLateralCodigo(item.codigo) && !CODIGOS_OCULTOS_MENU_LATERAL.has(item.codigo),
      )
      .map((item) => ({
        ...item,
        label: etiquetaBandeja(item.codigo),
      })),
  );
}

function construirFiltrosEnriquecimiento(
  enriquecimiento: BandejaResponse | undefined,
  preparacion: BandejaResponse | undefined,
  labradasBandeja: BandejaResponse | undefined,
): SubBandejaResumen[] {
  const captura = sumarSubBandejas(enriquecimiento, ['CAPTURA_INICIAL']);
  const completitud =
    sumarCantidad(preparacion) + sumarSubBandejas(enriquecimiento, ['ENRIQUECIMIENTO_GENERAL']);
  const labradas = sumarCantidad(labradasBandeja) > 0 ? sumarCantidad(labradasBandeja) : captura;
  const revision = sumarSubBandejas(enriquecimiento, ['REVISION_INICIAL']);

  return FILTROS_OPERATIVOS_ENRIQUECIMIENTO.map((filtro) => ({
    codigo: filtro.codigo,
    label: filtro.label,
    cantidad: conteoFiltroEnriquecimiento(filtro.codigo, labradas, captura, revision, completitud),
  })).filter((item) => item.cantidad > 0);
}

function conteoFiltroEnriquecimiento(
  codigo: string,
  labradas: number,
  captura: number,
  revision: number,
  completitud: number,
): number {
  switch (codigo) {
    case 'LABRADAS':
      return labradas;
    case 'CAPTURA_INICIAL':
      return captura;
    case 'REVISION_INICIAL':
      return revision;
    case 'COMPLETITUD_DOCUMENTAL':
      return completitud;
    default:
      return 0;
  }
}

function construirFiltrosNotificaciones(
  pendiente: BandejaResponse | undefined,
  enCurso: BandejaResponse | undefined,
): SubBandejaResumen[] {
  const pendientesEnvio = sumarCantidad(pendiente);
  const enCursoCount = sumarSubBandejas(enCurso, [...SUBS_NOTIF_EN_CURSO]);
  const negativas = sumarSubBandejas(enCurso, ['NOTIF_NEGATIVA_PENDIENTE_DECISION']);
  const vencidas = sumarSubBandejas(enCurso, ['NOTIF_VENCIDA_PENDIENTE_DECISION']);
  const piezas = sumarSubBandejas(pendiente, [...SUBS_NOTIF_PIEZA]);
  const positivas = 0;

  return FILTROS_OPERATIVOS_NOTIFICACIONES.map((filtro) => ({
    codigo: filtro.codigo,
    label: filtro.label,
    cantidad: conteoFiltroNotificaciones(
      filtro.codigo,
      pendientesEnvio,
      enCursoCount,
      positivas,
      negativas,
      vencidas,
      piezas,
    ),
  })).filter((item) => item.cantidad > 0);
}

function conteoFiltroNotificaciones(
  codigo: string,
  pendientesEnvio: number,
  enCurso: number,
  positivas: number,
  negativas: number,
  vencidas: number,
  piezas: number,
): number {
  switch (codigo) {
    case 'PENDIENTES_ENVIO':
      return pendientesEnvio;
    case 'EN_CURSO':
      return enCurso;
    case 'POSITIVAS':
      return positivas;
    case 'NEGATIVAS':
      return negativas;
    case 'VENCIDAS':
      return vencidas;
    case 'CLASIFICACION_PIEZA':
      return piezas;
    default:
      return 0;
  }
}

export function filtrosOperativosVisibles(bandeja: BandejaLateralResponse | null | undefined): SubBandejaResumen[] {
  if (!bandeja) {
    return [];
  }
  if (bandeja.codigo === 'ACTAS_EN_ENRIQUECIMIENTO' || bandeja.codigo === 'NOTIFICACIONES') {
    return (bandeja.subBandejas ?? []).filter((sub) => sub.cantidad > 0);
  }
  return (bandeja.subBandejas ?? []).filter((sub) => sub.cantidad > 0 && sub.codigo.toUpperCase() !== 'TODAS');
}

export function aplicarFiltroOperativoLateral(
  actas: readonly ActaBandejaItem[],
  lateral: BandejaLateralCodigo,
  filtro: string | null,
): ActaBandejaItem[] {
  if (!filtro) {
    return [...actas];
  }
  if (lateral === 'ACTAS_EN_ENRIQUECIMIENTO') {
    return actas.filter((acta) => actaCoincideFiltroEnriquecimiento(acta, filtro));
  }
  if (lateral === 'NOTIFICACIONES') {
    return actas.filter((acta) => actaCoincideFiltroNotificaciones(acta, filtro));
  }
  return actas.filter((acta) => acta.subBandeja === filtro);
}

export function subBandejaBackendParaConsulta(
  lateral: BandejaLateralCodigo,
  filtro: string | null,
  codigosEnResumen?: ReadonlySet<string>,
): { bandeja: BandejaCodigo; subBandeja?: string | null }[] {
  if (!filtro) {
    return bandejasBackendParaLateral(lateral, codigosEnResumen).map((bandeja) => ({ bandeja }));
  }

  if (lateral === 'ACTAS_EN_ENRIQUECIMIENTO') {
    if (filtro === 'LABRADAS') {
      return [{ bandeja: 'LABRADAS' }];
    }
    if (filtro === 'CAPTURA_INICIAL') {
      return [{ bandeja: 'ACTAS_EN_ENRIQUECIMIENTO', subBandeja: 'CAPTURA_INICIAL' }];
    }
    if (filtro === 'REVISION_INICIAL') {
      return [{ bandeja: 'ACTAS_EN_ENRIQUECIMIENTO', subBandeja: 'REVISION_INICIAL' }];
    }
    if (filtro === 'COMPLETITUD_DOCUMENTAL') {
      const consultas: { bandeja: BandejaCodigo; subBandeja?: string | null }[] = [];
      if (!codigosEnResumen || codigosEnResumen.has('PENDIENTE_PREPARACION_DOCUMENTAL')) {
        consultas.push({ bandeja: 'PENDIENTE_PREPARACION_DOCUMENTAL' });
      }
      if (!codigosEnResumen || codigosEnResumen.has('ACTAS_EN_ENRIQUECIMIENTO')) {
        consultas.push({ bandeja: 'ACTAS_EN_ENRIQUECIMIENTO' });
      }
      return consultas.length > 0 ? consultas : [{ bandeja: 'PENDIENTE_PREPARACION_DOCUMENTAL' }];
    }
    return bandejasBackendParaLateral(lateral, codigosEnResumen).map((bandeja) => ({ bandeja }));
  }

  if (lateral === 'NOTIFICACIONES') {
    if (filtro === 'PENDIENTES_ENVIO') {
      return [{ bandeja: 'PENDIENTE_NOTIFICACION' }];
    }
    if (filtro === 'EN_CURSO') {
      return [{ bandeja: 'EN_NOTIFICACION' }];
    }
    if (filtro === 'NEGATIVAS') {
      return [{ bandeja: 'EN_NOTIFICACION', subBandeja: 'NOTIF_NEGATIVA_PENDIENTE_DECISION' }];
    }
    if (filtro === 'VENCIDAS') {
      return [{ bandeja: 'EN_NOTIFICACION', subBandeja: 'NOTIF_VENCIDA_PENDIENTE_DECISION' }];
    }
    if (filtro === 'CLASIFICACION_PIEZA') {
      return [{ bandeja: 'PENDIENTE_NOTIFICACION' }];
    }
    return bandejasBackendParaLateral(lateral, codigosEnResumen).map((bandeja) => ({ bandeja }));
  }

  return [{ bandeja: lateral as BandejaCodigo, subBandeja: filtro }];
}

export function esBandejaOcultaLateral(codigo: string): boolean {
  return (BANDEJAS_OCULTAS_LATERAL as readonly string[]).includes(codigo);
}
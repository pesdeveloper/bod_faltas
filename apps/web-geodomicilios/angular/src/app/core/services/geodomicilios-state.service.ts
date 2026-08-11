import { Injectable, signal, computed } from '@angular/core';
import {
  DomicilioInfractorState, MunicipioLicenciaState, UbicacionInfraccionState,
  ModoDatos, PuntoUbicacion, GeocodeCandidato, TipoUnidadTerritorial
} from '../models/geo.models';

const DEFAULT_DOMICILIO: DomicilioInfractorState = {
  modoDatos: null, provinciaId: null, provinciaNombre: null,
  unidadTerritorialId: null, unidadTerritorialNombre: null, unidadTerritorialTipo: null,
  municipioId: null, municipioNombre: null, departamentoId: null, departamentoNombre: null,
  localidadId: null, localidadNombre: null, idLoc: null,
  calle: null, idTca: null, altura: null, sinAltura: false,
  piso: null, departamentoUnidad: null, unidadFuncional: null,
  codigoPostal: null, calleTexto: null, domicilioTexto: null, siNormalizadoParcial: false,
};

const DEFAULT_MUNICIPIO: MunicipioLicenciaState = {
  provinciaId: null, provinciaNombre: null,
  unidadTerritorialId: null, unidadTerritorialNombre: null, unidadTerritorialTipo: null,
};

const DEFAULT_UBICACION: UbicacionInfraccionState = {
  punto: null,
  candidatos: [],
  candidatoSeleccionado: null,
  reverseResultados: [],
  reverseSeleccionado: null,
  reverseAplicado: false,
  modoActivo: null,
};

@Injectable({ providedIn: 'root' })
export class GeoDomiciliosStateService {
  private readonly _domicilioInfractor = signal<DomicilioInfractorState>({ ...DEFAULT_DOMICILIO });
  private readonly _municipioLicencia = signal<MunicipioLicenciaState>({ ...DEFAULT_MUNICIPIO });
  private readonly _ubicacionInfraccion = signal<UbicacionInfraccionState>({ ...DEFAULT_UBICACION });

  readonly domicilioInfractor = this._domicilioInfractor.asReadonly();
  readonly municipioLicencia = this._municipioLicencia.asReadonly();
  readonly ubicacionInfraccion = this._ubicacionInfraccion.asReadonly();

  readonly domicilioCompleto = computed(() => {
    const d = this._domicilioInfractor();
    return !!(d.localidadNombre && d.calle && (d.altura || d.sinAltura));
  });
  readonly municipioCompleto = computed(() => {
    const m = this._municipioLicencia();
    return !!(m.unidadTerritorialId && m.unidadTerritorialNombre);
  });
  readonly ubicacionCompleta = computed(() => {
    return !!this._ubicacionInfraccion().punto;
  });

  // ─── Domicilio Infractor ─────────────────────────────────────────────────────

  updateDomicilioInfractor(patch: Partial<DomicilioInfractorState>): void {
    this._domicilioInfractor.update(s => ({ ...s, ...patch }));
  }
  resetDomicilioInfractor(): void {
    this._domicilioInfractor.set({ ...DEFAULT_DOMICILIO });
  }
  setModoDatos(modo: ModoDatos): void {
    this._domicilioInfractor.update(s => ({ ...DEFAULT_DOMICILIO, modoDatos: modo }));
  }

  // ─── Municipio Licencia ──────────────────────────────────────────────────────

  updateMunicipioLicencia(patch: Partial<MunicipioLicenciaState>): void {
    this._municipioLicencia.update(s => ({ ...s, ...patch }));
  }
  resetMunicipioLicencia(): void {
    this._municipioLicencia.set({ ...DEFAULT_MUNICIPIO });
  }

  // ─── Ubicación Infracción ────────────────────────────────────────────────────

  setPunto(punto: PuntoUbicacion): void {
    this._ubicacionInfraccion.update(s => ({
      ...s, punto, candidatos: [], candidatoSeleccionado: null,
      reverseResultados: [], reverseSeleccionado: null, reverseAplicado: false,
    }));
  }

  clearPunto(): void {
    this._ubicacionInfraccion.update(s => ({
      ...s, punto: null, candidatos: [], candidatoSeleccionado: null,
      reverseResultados: [], reverseSeleccionado: null, reverseAplicado: false,
    }));
  }

  setCandidatos(candidatos: GeocodeCandidato[]): void {
    this._ubicacionInfraccion.update(s => ({
      ...s, candidatos, candidatoSeleccionado: null,
    }));
  }

  seleccionarCandidato(c: GeocodeCandidato): void {
    this._ubicacionInfraccion.update(s => ({ ...s, candidatoSeleccionado: c }));
    this.setPunto({
      lat: c.lat, lng: c.lng,
      origenUbicacion: 'GEOCODIFICADO_CALLE_ALTURA',
      calle: c.calle, altura: c.alturaAprox,
      localidad: c.localidad, idLoc: c.idLoc,
      barrio: c.barrio, manzana: c.manzana,
      nomenclatura: c.nomenclatura, idTca: c.idTca,
      calidad: c.calidad, advertencias: c.advertencias,
    });
  }

  setReverseResultados(resultados: GeocodeCandidato[]): void {
    this._ubicacionInfraccion.update(s => ({
      ...s, reverseResultados: resultados, reverseSeleccionado: null, reverseAplicado: false,
    }));
  }

  seleccionarReverseResultado(r: GeocodeCandidato): void {
    this._ubicacionInfraccion.update(s => ({ ...s, reverseSeleccionado: r, reverseAplicado: false }));
  }

  aplicarReverse(): void {
    const state = this._ubicacionInfraccion();
    const r = state.reverseSeleccionado ?? (state.reverseResultados.length === 1 ? state.reverseResultados[0] : null);
    if (!r || !state.punto) return;
    const updated: PuntoUbicacion = {
      ...state.punto,
      calle: r.calle ?? state.punto.calle,
      altura: r.alturaAprox ?? state.punto.altura,
      localidad: r.localidad ?? state.punto.localidad,
      idLoc: r.idLoc ?? state.punto.idLoc,
      barrio: r.barrio ?? state.punto.barrio,
      manzana: r.manzana ?? state.punto.manzana,
      nomenclatura: r.nomenclatura ?? state.punto.nomenclatura,
      idTca: r.idTca ?? state.punto.idTca,
      calidad: r.calidad ?? state.punto.calidad,
    };
    this._ubicacionInfraccion.update(s => ({ ...s, punto: updated, reverseAplicado: true }));
  }

  setModoActivo(modo: 'colocar' | 'ver' | null): void {
    this._ubicacionInfraccion.update(s => ({ ...s, modoActivo: modo }));
  }

  // Legacy: mantiene compatibilidad con componentes que usan setReverseResultado (singular)
  setReverseResultado(r: GeocodeCandidato | null): void {
    this.setReverseResultados(r ? [r] : []);
  }
}
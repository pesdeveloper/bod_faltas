import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { API_CONFIG } from '../config/api.config';
import { ProvinciaDto, UnidadTerritorialDto, LocalidadDto, CalleDto } from '../models/geo.models';

/**
 * Catálogos externos (modo EXTERNO_IGN_INDEC).
 * Provincias y municipios desde IGN GeoRef API (vía backend Faltas).
 */
@Injectable({ providedIn: 'root' })
export class GeoCatalogosService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);

  getProvincias(): Observable<ProvinciaDto[]> {
    return this.http.get<ProvinciaDto[]>(this.config.baseUrl + '/geo/provincias');
  }

  getUnidadesTeritoriales(provinciaId: number): Observable<UnidadTerritorialDto[]> {
    const params = new HttpParams().set('provinciaId', provinciaId);
    return this.http.get<UnidadTerritorialDto[]>(
      this.config.baseUrl + '/geo/unidades-territoriales', { params });
  }

  getLocalidades(unidadTerritorialId: number): Observable<LocalidadDto[]> {
    const params = new HttpParams().set('unidadTerritorialId', unidadTerritorialId);
    return this.http.get<LocalidadDto[]>(this.config.baseUrl + '/geo/localidades', { params });
  }

  getCalles(query: string, localidadId?: string | number): Observable<CalleDto[]> {
    let params = new HttpParams().set('q', query);
    if (localidadId !== undefined) params = params.set('localidadId', String(localidadId));
    return this.http.get<CalleDto[]>(this.config.baseUrl + '/geo/calles', { params });
  }
}
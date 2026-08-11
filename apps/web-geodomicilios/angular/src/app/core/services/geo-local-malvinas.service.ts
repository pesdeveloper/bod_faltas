import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG } from '../config/api.config';
import { LocalidadDto, CalleDto } from '../models/geo.models';

@Injectable({ providedIn: 'root' })
export class GeoLocalMalvinasService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);

  getLocalidades(): Observable<LocalidadDto[]> {
    return this.http.get<LocalidadDto[]>(this.config.baseUrl + '/geo/local/malvinas/localidades');
  }

  getCalles(query: string, idLoc?: string, altura?: number): Observable<CalleDto[]> {
    let params = new HttpParams().set('q', query);
    if (idLoc) params = params.set('idLoc', idLoc);
    if (altura !== undefined) params = params.set('altura', String(altura));
    return this.http.get<CalleDto[]>(this.config.baseUrl + '/geo/local/malvinas/calles', { params });
  }

  getParcelaGeom(circ: string, secc: string, quint: string, fracc: string, manz: string, parc: string): Observable<any> {
    let params = new HttpParams()
      .set('circ', circ).set('secc', secc).set('quint', quint)
      .set('fracc', fracc).set('manz', manz).set('parc', parc);
    return this.http.get<any>(this.config.baseUrl + '/geo/local/malvinas/parcela/geom', { params });
  }
}
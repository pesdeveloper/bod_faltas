import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs';
import { API_CONFIG } from '../config/api.config';
import {
  GeocodeCandidato,
  GeocodeCalleAlturaRequest,
  ReversePointRequest,
  EsquinaRequest,
  EntreCallesRequest,
} from '../models/geo.models';

/**
 * Servicio GIS: geocodificacion y reverse point.
 * El endpoint /geo/geocodificar devuelve un objeto simple cuando hay resultado,
 * o [] cuando no hay resultado. Se normaliza siempre a array.
 */
@Injectable({ providedIn: 'root' })
export class GisService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);

  geocodificarCalleAltura(req: GeocodeCalleAlturaRequest): Observable<GeocodeCandidato[]> {
    return this.http.post<GeocodeCandidato | GeocodeCandidato[]>(
      this.config.baseUrl + '/geo/geocodificar', req
    ).pipe(
      map(r => Array.isArray(r) ? r : (r != null ? [r as GeocodeCandidato] : []))
    );
  }

  reversePoint(req: ReversePointRequest): Observable<GeocodeCandidato[]> {
    return this.http.post<GeocodeCandidato[]>(this.config.baseUrl + '/geo/reverse', req);
  }

  geocodificarEsquina(req: EsquinaRequest): Observable<GeocodeCandidato[]> {
    return this.http.post<GeocodeCandidato[]>(this.config.baseUrl + '/geo/geocodificar-esquina', req);
  }

  geocodificarEntreCalles(req: EntreCallesRequest): Observable<GeocodeCandidato[]> {
    return this.http.post<GeocodeCandidato[]>(this.config.baseUrl + '/geo/geocodificar-entre-calles', req);
  }
}
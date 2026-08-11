import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG } from '../config/api.config';
import { ParcelaRef, ParcelaDetalle } from '../models/geo.models';

@Injectable({ providedIn: 'root' })
export class GeoParcelaService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);

  getDetalle(ref: ParcelaRef): Observable<ParcelaDetalle> {
    const params = new HttpParams()
      .set('circ',  ref.circ)
      .set('secc',  ref.secc)
      .set('quint', ref.quint)
      .set('fracc', ref.fracc)
      .set('manz',  ref.manz)
      .set('parc',  ref.parc);
    return this.http.get<ParcelaDetalle>(
      this.config.baseUrl + '/geo/local/malvinas/parcela/detalle', { params });
  }
}
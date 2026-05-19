import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_CONFIG, ApiConfig } from '../config/api.config';
import {
  ActaDetalleDemo,
  ActaResumenDemo,
  BandejaCodigo,
  PrototipoHealthResponse,
  PrototipoResetResponse,
} from '../models/prototipo-faltas.models';

@Injectable({ providedIn: 'root' })
export class PrototipoFaltasApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_CONFIG) private readonly api: ApiConfig,
  ) {}

  health(): Observable<PrototipoHealthResponse> {
    return this.http.get<PrototipoHealthResponse>(`${this.api.baseUrl}/health`);
  }

  reset(): Observable<PrototipoResetResponse> {
    return this.http.post<PrototipoResetResponse>(`${this.api.baseUrl}/reset`, null);
  }

  listarActasPorBandeja(bandeja: BandejaCodigo): Observable<ActaResumenDemo[]> {
    return this.http.get<ActaResumenDemo[]>(`${this.api.baseUrl}/bandejas/${bandeja}/actas`);
  }

  obtenerActa(id: string): Observable<ActaDetalleDemo> {
    return this.http.get<ActaDetalleDemo>(`${this.api.baseUrl}/actas/${id}`);
  }
}
import {
  Component, ChangeDetectionStrategy, inject, signal, OnInit, DestroyRef
, computed} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { catchError, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GeoDomiciliosStateService } from '../../../../core/services/geodomicilios-state.service';
import { GeoCatalogosService } from '../../../../core/services/geo-catalogos.service';
import { ProvinciaDto, UnidadTerritorialDto } from '../../../../core/models/geo.models';

function normalizeSearch(s: string): string {
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();
}

@Component({
  selector: 'app-municipio-licencia-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatAutocompleteModule,
    MatIconModule, MatProgressSpinnerModule, MatChipsModule],
  templateUrl: './municipio-licencia-tab.component.html',
  styleUrl: './municipio-licencia-tab.component.scss',
})
export class MunicipioLicenciaTabComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly stateService = inject(GeoDomiciliosStateService);
  private readonly catalogosService = inject(GeoCatalogosService);
  private readonly destroyRef = inject(DestroyRef);

  readonly provincias = signal<ProvinciaDto[]>([]);
  readonly unidades = signal<UnidadTerritorialDto[]>([]);
  readonly loadingProvincias = signal(false);
  readonly loadingUnidades = signal(false);
  readonly errorProvincias = signal<string | null>(null);
  readonly errorUnidades = signal<string | null>(null);

  readonly state = this.stateService.municipioLicencia;

  readonly provinciaDisplayText = signal('');
  readonly unidadDisplayText = signal('');
  readonly canSelectUnidad = computed(() => this.unidades().length > 0);
  readonly filteredProvincias = computed(() => {
    const q = normalizeSearch(this.provinciaDisplayText());
    return q ? this.provincias().filter(p => normalizeSearch(p.nombre).includes(q)) : this.provincias();
  });
  readonly filteredUnidades = computed(() => {
    const q = normalizeSearch(this.unidadDisplayText());
    return q ? this.unidades().filter(u => normalizeSearch(u.nombre).includes(q)) : this.unidades();
  });

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      provincia: [null, Validators.required],
      unidadTerritorial: [{value: null, disabled: true}, Validators.required],
    });
    this.loadProvincias();
  }

  private loadProvincias(): void {
    this.loadingProvincias.set(true);
    this.catalogosService.getProvincias()
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => {
        this.errorProvincias.set('No se pudo cargar provincias. Backend pendiente.');
        return of([] as ProvinciaDto[]);
      }))
      .subscribe(data => {
        this.provincias.set(data);
        this.loadingProvincias.set(false);
      });
  }

  onProvinciaSelected(prov: ProvinciaDto): void {
    this.provinciaDisplayText.set(prov.nombre);
    this.unidadDisplayText.set('');
    this.form.get('provincia')!.setValue(prov.id);
    this.onProvinciaChange(prov.id);
  }

  onUnidadSelected(ut: UnidadTerritorialDto): void {
    this.unidadDisplayText.set(ut.nombre);
    this.form.get('unidadTerritorial')!.setValue(ut.id);
    this.onUnidadChange(ut.id);
  }

  onProvinciaChange(provinciaId: number): void {
    const prov = this.provincias().find(p => p.id === provinciaId);
    this.form.patchValue({ unidadTerritorial: null });
    this.form.get('unidadTerritorial')!.disable();
    this.unidades.set([]);
    this.stateService.updateMunicipioLicencia({
      provinciaId, provinciaNombre: prov?.nombre ?? null,
      unidadTerritorialId: null, unidadTerritorialNombre: null, unidadTerritorialTipo: null,
    });
    this.loadingUnidades.set(true);
    this.catalogosService.getUnidadesTeritoriales(provinciaId)
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => {
        this.errorUnidades.set('No se pudo cargar municipios. Backend pendiente.');
        return of([] as UnidadTerritorialDto[]);
      }))
      .subscribe(data => {
        this.unidades.set(data);
        this.loadingUnidades.set(false);
        this.form.get('unidadTerritorial')!.enable();
      });
  }

  onUnidadChange(utId: number): void {
    const ut = this.unidades().find(u => u.id === utId);
    this.stateService.updateMunicipioLicencia({
      unidadTerritorialId: utId,
      unidadTerritorialNombre: ut?.nombre ?? null,
      unidadTerritorialTipo: ut?.tipo ?? null,
    });
  }
}
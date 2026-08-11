import {
  Component, ChangeDetectionStrategy, inject, signal, computed, OnInit, DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import {
  debounceTime, distinctUntilChanged, switchMap, catchError, of, Subject, takeUntil
} from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GeoDomiciliosStateService } from '../../../../core/services/geodomicilios-state.service';
import { GeoCatalogosService } from '../../../../core/services/geo-catalogos.service';
import { GeoLocalMalvinasService } from '../../../../core/services/geo-local-malvinas.service';
import {
  ProvinciaDto, UnidadTerritorialDto, LocalidadDto, CalleDto, ModoDatos
} from '../../../../core/models/geo.models';
import { GEO_CONSTANTS } from '../../../../core/config/geo.constants';

function normalizeSearch(s: string): string {
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();
}

@Component({
  selector: 'app-domicilio-infractor-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule,
    MatIconModule, MatAutocompleteModule, MatProgressSpinnerModule,
    MatCheckboxModule, MatChipsModule, MatTooltipModule, MatDividerModule],
  templateUrl: './domicilio-infractor-tab.component.html',
  styleUrl: './domicilio-infractor-tab.component.scss',
})
export class DomicilioInfractorTabComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly stateService = inject(GeoDomiciliosStateService);
  private readonly catalogosService = inject(GeoCatalogosService);
  private readonly malvinasService = inject(GeoLocalMalvinasService);
  private readonly destroyRef = inject(DestroyRef);

  readonly GEO_CONSTANTS = GEO_CONSTANTS;

  readonly provincias = signal<ProvinciaDto[]>([]);
  readonly unidadesTeritoriales = signal<UnidadTerritorialDto[]>([]);
  readonly localidades = signal<LocalidadDto[]>([]);
  readonly callesFiltradas = signal<CalleDto[]>([]);
  readonly loadingProvincias = signal(false);
  readonly loadingUnidades = signal(false);
  readonly loadingLocalidades = signal(false);
  readonly loadingCalles = signal(false);
  readonly errorProvincias = signal<string | null>(null);
  readonly errorUnidades = signal<string | null>(null);
  readonly errorLocalidades = signal<string | null>(null);
  readonly errorCalles = signal<string | null>(null);

  readonly state = this.stateService.domicilioInfractor;
  readonly modoDatos = computed(() => this.state().modoDatos);
  readonly esLocal = computed(() => this.state().modoDatos === 'MALVINAS_LOCAL');

  readonly calleInput$ = new Subject<string>();

  // ── Autocomplete display signals ──────────────────────────────────────────
  readonly provinciaDisplayText = signal('');
  readonly unidadDisplayText = signal('');

  readonly canSelectUnidad = computed(() => this.unidadesTeritoriales().length > 0);

  readonly filteredProvincias = computed(() => {
    const q = normalizeSearch(this.provinciaDisplayText());
    return q ? this.provincias().filter(p => normalizeSearch(p.nombre).includes(q)) : this.provincias();
  });

  readonly filteredUnidades = computed(() => {
    const q = normalizeSearch(this.unidadDisplayText());
    return q ? this.unidadesTeritoriales().filter(u => normalizeSearch(u.nombre).includes(q)) : this.unidadesTeritoriales();
  });

  readonly localidadDisplayText = signal('');
  readonly filteredLocalidades = computed(() => {
    const q = normalizeSearch(this.localidadDisplayText());
    return q ? this.localidades().filter(l => normalizeSearch(l.nombre).includes(q)) : this.localidades();
  });

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      provincia: [null, Validators.required],
      unidadTerritorial: [{value: null, disabled: true}],
      localidad: [{value: null, disabled: true}],
      calleTexto: [{value: '', disabled: true}],
      calleSeleccionada: [null],
      altura: [null, [Validators.min(1), Validators.max(99999)]],
      sinAltura: [false],
      piso: [''],
      departamentoUnidad: [''],
      codigoPostal: [''],
    });

    this.loadProvincias();
    this.setupCalleAutocomplete();

    this.form.get('sinAltura')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(val => {
        const alturaCtrl = this.form.get('altura')!;
        if (val) alturaCtrl.disable();
        else alturaCtrl.enable();
        this.persistState();
      });
  }

  private loadProvincias(): void {
    this.loadingProvincias.set(true);
    this.errorProvincias.set(null);
    this.catalogosService.getProvincias()
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(err => {
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

  onUnidadTerritorialSelected(ut: UnidadTerritorialDto): void {
    this.unidadDisplayText.set(ut.nombre);
    this.form.get('unidadTerritorial')!.setValue(ut.id);
    this.onUnidadTerritorialChange(ut.id);
  }

  onProvinciaChange(provinciaId: number): void {
    const prov = this.provincias().find(p => p.id === provinciaId);
    this.form.patchValue({ unidadTerritorial: null, localidad: null, calleTexto: '', calleSeleccionada: null });
    this.form.get('unidadTerritorial')!.disable();
    this.form.get('localidad')!.disable();
    this.form.get('calleTexto')!.disable();
    this.unidadesTeritoriales.set([]);
    this.localidades.set([]);
    this.callesFiltradas.set([]);

    const esBA = provinciaId === GEO_CONSTANTS.ID_PROVINCIA_BUENOS_AIRES;
    this.stateService.updateDomicilioInfractor({
      provinciaId, provinciaNombre: prov?.nombre ?? null,
      unidadTerritorialId: null, unidadTerritorialNombre: null,
      localidadId: null, localidadNombre: null, idLoc: null,
      calle: null, idTca: null,
    });

    this.loadingUnidades.set(true);
    this.catalogosService.getUnidadesTeritoriales(provinciaId)
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => {
        this.errorUnidades.set('No se pudo cargar unidades territoriales. Backend pendiente.');
        return of([] as UnidadTerritorialDto[]);
      }))
      .subscribe(data => {
        this.unidadesTeritoriales.set(data);
        this.loadingUnidades.set(false);
        this.form.get('unidadTerritorial')!.enable();
      });
  }

  onUnidadTerritorialChange(utId: number): void {
    const ut = this.unidadesTeritoriales().find(u => u.id === utId);
    this.form.patchValue({ localidad: null, calleTexto: '', calleSeleccionada: null });
    this.form.get('localidad')!.disable();
    this.form.get('calleTexto')!.disable();
    this.localidades.set([]);

    const esMalvinas = utId === GEO_CONSTANTS.ID_MUNICIPIO_MALVINAS_ARGENTINAS;
    const provinciaId = this.form.get('provincia')!.value;
    const esBA = provinciaId === GEO_CONSTANTS.ID_PROVINCIA_BUENOS_AIRES;
    const modoDatos: ModoDatos = (esBA && esMalvinas) ? 'MALVINAS_LOCAL' : 'EXTERNO_IGN_INDEC';

    this.stateService.updateDomicilioInfractor({
      modoDatos,
      unidadTerritorialId: utId, unidadTerritorialNombre: ut?.nombre ?? null,
      unidadTerritorialTipo: ut?.tipo ?? null,
      localidadId: null, localidadNombre: null, idLoc: null,
      calle: null, idTca: null,
    });

    if (modoDatos === 'MALVINAS_LOCAL') {
      this.loadMalvinasLocalidades();
    } else {
      this.loadExternoLocalidades(utId);
    }
  }

  private loadMalvinasLocalidades(): void {
    this.loadingLocalidades.set(true);
    this.malvinasService.getLocalidades()
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => {
        this.errorLocalidades.set('No se pudo cargar localidades Malvinas. Backend pendiente.');
        return of([] as LocalidadDto[]);
      }))
      .subscribe(data => {
        this.localidades.set(data);
        this.loadingLocalidades.set(false);
        this.form.get('localidad')!.enable();
      });
  }

  private loadExternoLocalidades(utId: number): void {
    this.loadingLocalidades.set(true);
    this.catalogosService.getLocalidades(utId)
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(() => {
        this.errorLocalidades.set('No se pudo cargar localidades. Backend pendiente.');
        return of([] as LocalidadDto[]);
      }))
      .subscribe(data => {
        this.localidades.set(data);
        this.loadingLocalidades.set(false);
        this.form.get('localidad')!.enable();
      });
  }

  onLocalidadSelected(loc: LocalidadDto): void {
    this.localidadDisplayText.set(loc.nombre);
    this.form.get('localidad')!.setValue(loc);
    this.onLocalidadChange(loc);
  }

  onLocalidadChange(loc: LocalidadDto): void {
    this.form.get('calleTexto')!.enable();
    this.stateService.updateDomicilioInfractor({
      localidadId: loc.idLoc, localidadNombre: loc.nombre,
      idLoc: loc.idLoc ?? null,
      calle: null, idTca: null,
    });
    this.form.patchValue({ calleTexto: '', calleSeleccionada: null });
    this.callesFiltradas.set([]);
  }

  private setupCalleAutocomplete(): void {
    this.calleInput$
      .pipe(
        debounceTime(280),
        distinctUntilChanged(),
        switchMap(q => {
          if (!q || q.length < 2) return of([] as CalleDto[]);
          this.loadingCalles.set(true);
          this.errorCalles.set(null);
          const localidadId = this.form.get('localidad')!.value?.id;
          const obs = this.esLocal()
            ? this.malvinasService.getCalles(q, this.state().idLoc ?? undefined)
            : this.catalogosService.getCalles(q, localidadId);
          return obs.pipe(catchError(() => {
            this.errorCalles.set('No se pudo cargar calles. Backend pendiente.');
            return of([] as CalleDto[]);
          }));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(calles => {
        this.callesFiltradas.set(calles);
        this.loadingCalles.set(false);
      });
  }

  onCalleInputChange(value: string): void {
    this.calleInput$.next(value);
    if (!value) {
      this.form.patchValue({ calleSeleccionada: null });
      this.stateService.updateDomicilioInfractor({ calle: null, idTca: null });
    }
  }

  onCalleSelected(calle: CalleDto): void {
    this.form.patchValue({ calleTexto: calle.nombre, calleSeleccionada: calle });
    this.stateService.updateDomicilioInfractor({
      calle: calle.nombre, idTca: calle.idTca,
      calleTexto: calle.texto,
    });
  }

  onAlturaChange(): void { this.persistState(); }
  onPisoChange(): void { this.persistState(); }

  private persistState(): void {
    const v = this.form.getRawValue();
    this.stateService.updateDomicilioInfractor({
      altura: v.altura ?? null,
      sinAltura: v.sinAltura ?? false,
      piso: v.piso || null,
      departamentoUnidad: v.departamentoUnidad || null,
      codigoPostal: v.codigoPostal || null,
    });
  }

  getCalleDisplayFn(value: CalleDto | string | null): string { if (!value) return ''; if (typeof value === 'string') return value; return value.nombre; }

  trackByCalle(i: number, c: CalleDto): string { return c.idTca + c.idLoc; }
}

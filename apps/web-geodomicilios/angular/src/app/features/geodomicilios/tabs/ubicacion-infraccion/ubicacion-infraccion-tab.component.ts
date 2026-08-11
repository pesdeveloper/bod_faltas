import {
  Component, ChangeDetectionStrategy, inject, signal, computed,
  ViewChild, AfterViewInit, DestroyRef, ElementRef
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDividerModule } from "@angular/material/divider";
import { MatChipsModule } from "@angular/material/chips";
import { debounceTime, distinctUntilChanged, switchMap, catchError, of, Subject, firstValueFrom } from "rxjs";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { GeoDomiciliosStateService } from "../../../../core/services/geodomicilios-state.service";
import { GisService } from "../../../../core/services/gis.service";
import { GeoLocalMalvinasService } from "../../../../core/services/geo-local-malvinas.service";
import { MapThemeService, MapThemeMode } from "../../../../core/services/map-theme.service";
import { CalleDto, GeocodeCandidato, ParcelaRef } from "../../../../core/models/geo.models";
import { MalvinasMapComponent, MapClickEvent } from "./map/malvinas-map.component";
import { ParcelaDetailPanelComponent } from "../../../geo-inspector/parcela-detail-panel/parcela-detail-panel.component";

type SmartMode = 'partial' | 'calle-altura' | 'esquina' | 'entre-calles';

interface SmartParsed {
  mode: SmartMode;
  activeCalle: string;
  mainCalleText: string;
  calle2Text?: string;
  calleObj?: string;
  calleLim1?: string;
  calleLim2?: string;
  altura?: number;
}

export interface SearchSuggestion {
  icon: string;
  modeIcon: string;
  label: string;
  sub: string;
  calle: CalleDto;
  geocodable: boolean;
}

@Component({
  selector: "app-ubicacion-infraccion-tab",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatTooltipModule, MatDividerModule, MatChipsModule,
    MalvinasMapComponent,
    ParcelaDetailPanelComponent,
  ],
  templateUrl: "./ubicacion-infraccion-tab.component.html",
  styleUrl: "./ubicacion-infraccion-tab.component.scss",
})
export class UbicacionInfraccionTabComponent implements AfterViewInit {
  @ViewChild(MalvinasMapComponent) mapComponent?: MalvinasMapComponent;
  @ViewChild('mapOverlay') mapOverlayRef?: ElementRef<HTMLElement>;

  private readonly stateService = inject(GeoDomiciliosStateService);
  private readonly gisService = inject(GisService);
  private readonly malvinasService = inject(GeoLocalMalvinasService);
  readonly themeService = inject(MapThemeService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly elementRef = inject(ElementRef);

  readonly state = this.stateService.ubicacionInfraccion;
  readonly punto = computed(() => this.state().punto);
  readonly candidatos = computed(() => this.state().candidatos);
  readonly reverseResultados = computed(() => this.state().reverseResultados);
  readonly reverseSeleccionado = computed(() => this.state().reverseSeleccionado);
  readonly reverseAplicado = computed(() => this.state().reverseAplicado);

  readonly colocarModoActivo = signal(false);
  readonly currentZoom = signal<number>(14);
  readonly loadingGeocode = signal(false);
  readonly loadingReverse = signal(false);
  readonly errorGeocode = signal<string | null>(null);
  readonly errorReverse = signal<string | null>(null);

  readonly searchText = signal('');
  readonly calleSuggestions = signal<CalleDto[]>([]);
  readonly showSuggestions = signal(false);
  readonly loadingCalles = signal(false);
  readonly focusedSuggestionIndex = signal(-1);
  readonly parcelaActual = signal<ParcelaRef | null>(null);

  private readonly searchInput$ = new Subject<{ q: string; altura?: number }>();

  readonly parsedSearch = computed(() => this.parseSmartInput(this.searchText()));

  readonly suggestions = computed<SearchSuggestion[]>(() =>
    this.buildSuggestions(this.parsedSearch(), this.calleSuggestions()).slice(0, 5)
  );

  readonly modeLabel = computed(() => {
    switch (this.parsedSearch().mode) {
      case 'calle-altura': return 'Calle + Altura';
      case 'esquina': return 'Esquina';
      case 'entre-calles': return 'Entre calles';
      default: return null;
    }
  });

  readonly modeIcon = computed(() => {
    switch (this.parsedSearch().mode) {
      case 'calle-altura': return 'navigation';
      case 'esquina': return 'alt_route';
      case 'entre-calles': return 'swap_horiz';
      default: return null;
    }
  });

  constructor() {
    this.searchInput$.pipe(
      debounceTime(280),
      distinctUntilChanged((a, b) => a.q === b.q && a.altura === b.altura),
      switchMap(({ q, altura }) => {
        if (!q || q.length < 2) { this.loadingCalles.set(false); return of([] as CalleDto[]); }
        this.loadingCalles.set(true);
        return this.malvinasService.getCalles(q, undefined, altura).pipe(catchError(() => of([] as CalleDto[])));
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(calles => {
      this.calleSuggestions.set(calles);
      this.loadingCalles.set(false);
      this.showSuggestions.set(calles.length > 0 && !!this.searchText().trim());
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.mapComponent?.resize(), 300);
    // Passive wheel listener to forward scroll to map without blocking
    const overlay = (this.mapOverlayRef?.nativeElement
      ?? this.elementRef.nativeElement.querySelector('.map-overlay')) as HTMLElement | null;
    if (overlay) {
      overlay.addEventListener('wheel', (ev: WheelEvent) => this.onOverlayWheel(ev), { passive: true });
    }
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchText.set(value);
    this.focusedSuggestionIndex.set(-1);
    this.errorGeocode.set(null);
    if (!value.trim()) {
      this.showSuggestions.set(false);
      this.calleSuggestions.set([]);
      return;
    }
    const parsed = this.parseSmartInput(value);
    if (parsed.activeCalle.length >= 2) {
      this.searchInput$.next({ q: parsed.activeCalle, altura: parsed.mode === 'calle-altura' ? parsed.altura : undefined });
    } else {
      this.calleSuggestions.set([]);
      this.showSuggestions.set(false);
    }
  }

  onSearchKeydown(event: KeyboardEvent): void {
    const suggs = this.suggestions();
    if (event.key === 'Escape') {
      this.showSuggestions.set(false);
      this.focusedSuggestionIndex.set(-1);
      (event.target as HTMLElement).blur();
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (suggs.length > 0) {
        this.showSuggestions.set(true);
        this.focusedSuggestionIndex.update(i => Math.min(i + 1, suggs.length - 1));
      }
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.focusedSuggestionIndex.update(i => Math.max(i - 1, -1));
      return;
    }
    if (event.key === 'Enter') {
      const idx = this.focusedSuggestionIndex();
      const sugg = (idx >= 0 && idx < suggs.length) ? suggs[idx] : (suggs.length > 0 ? suggs[0] : null);
      if (sugg) {
        event.preventDefault();
        this.onSuggestionClick(sugg);
        this.focusedSuggestionIndex.set(-1);
      }
    }
  }

  hideSuggestions(): void {
    setTimeout(() => {
      this.showSuggestions.set(false);
      this.focusedSuggestionIndex.set(-1);
    }, 180);
  }

  private blurSearch(): void {
    setTimeout(() => {
      const el = document.querySelector('.search-input') as HTMLInputElement;
      el?.blur();
    }, 100);
  }

  clearSearch(): void {
    this.searchText.set('');
    this.calleSuggestions.set([]);
    this.showSuggestions.set(false);
    this.focusedSuggestionIndex.set(-1);
    this.errorGeocode.set(null);
    this.stateService.setCandidatos([]);
  }


  async onSuggestionClick(s: SearchSuggestion): Promise<void> {
    const parsed = this.parsedSearch();
    this.showSuggestions.set(false);
    this.focusedSuggestionIndex.set(-1);
    switch (parsed.mode) {
      case 'calle-altura': {
        this.searchText.set(`${s.calle.nombre} ${parsed.altura}`);
        this.runCalleAltura(s.calle, parsed.altura!);
        break;
      }
      case 'esquina': {
        this.searchText.set(`${parsed.mainCalleText} esq ${s.calle.nombre}`);
        const c1 = await this.resolveCalleByText(parsed.mainCalleText);
        if (c1) this.runEsquina(c1, s.calle);
        else this.errorGeocode.set(`No se pudo resolver: ${parsed.mainCalleText}`);
        break;
      }
      case 'entre-calles': {
        this.searchText.set(`${parsed.calleObj} entre ${parsed.calleLim1} y ${s.calle.nombre}`);
        const [c1, c2] = await Promise.all([
          this.resolveCalleByText(parsed.calleObj!),
          this.resolveCalleByText(parsed.calleLim1!),
        ]);
        if (c1 && c2) this.runEntreCalles(c1, c2, s.calle);
        else this.errorGeocode.set('No se pudieron resolver todas las calles.');
        break;
      }
      default: {
        this.searchText.set(s.calle.nombre + ' ');
        this.calleSuggestions.set([]);
        this.showSuggestions.set(false);
      }
    }
    this.blurSearch();
  }

  private runCalleAltura(calle: CalleDto, altura: number): void {
    this.loadingGeocode.set(true); this.errorGeocode.set(null); this.stateService.setCandidatos([]);
    this.gisService.geocodificarCalleAltura({ idTca: calle.idTca, idLoc: calle.idLoc, altura })
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(err => {
        this.errorGeocode.set(!err?.status || err.status >= 500 || err.status === 0
          ? 'Backend GEO no disponible.' : 'Error al geocodificar.');
        return of([] as GeocodeCandidato[]);
      }))
      .subscribe(cs => {
        this.loadingGeocode.set(false);
        this.stateService.setCandidatos(cs);
        if (cs.length >= 1) {
          this.stateService.seleccionarCandidato(cs[0]);
          setTimeout(() => this.mapComponent?.flyTo(cs[0].lat, cs[0].lng, 18), 0);
        } else {
          this.errorGeocode.set('Sin resultados para esa direccion.');
        }
      });
  }

  private runEsquina(c1: CalleDto, c2: CalleDto): void {
    this.loadingGeocode.set(true); this.errorGeocode.set(null); this.stateService.setCandidatos([]);
    this.gisService.geocodificarEsquina({ idTcaCalle1: c1.idTca, idLocCalle1: c1.idLoc, idTcaCalle2: c2.idTca, idLocCalle2: c2.idLoc })
      .pipe(takeUntilDestroyed(this.destroyRef), catchError(err => {
        this.errorGeocode.set(!err?.status || err.status >= 500 || err.status === 0 ? 'Backend GEO no disponible.' : 'Error esquina.');
        return of([] as GeocodeCandidato[]);
      }))
      .subscribe(cs => {
        this.loadingGeocode.set(false);
        this.stateService.setCandidatos(cs);
        if (cs.length >= 1) {
          this.stateService.seleccionarCandidato(cs[0]);
          setTimeout(() => this.mapComponent?.flyTo(cs[0].lat, cs[0].lng, 18), 0);
        }
      });
  }

  private runEntreCalles(c1: CalleDto, c2: CalleDto, c3: CalleDto): void {
    this.loadingGeocode.set(true); this.errorGeocode.set(null); this.stateService.setCandidatos([]);
    this.gisService.geocodificarEntreCalles({
      idTcaCalleObjetivo: c1.idTca, idLocCalleObjetivo: c1.idLoc,
      idTcaLimite1: c2.idTca, idLocLimite1: c2.idLoc,
      idTcaLimite2: c3.idTca, idLocLimite2: c3.idLoc,
    }).pipe(takeUntilDestroyed(this.destroyRef), catchError(err => {
      this.errorGeocode.set(!err?.status || err.status >= 500 || err.status === 0 ? 'Backend GEO no disponible.' : 'Error tramo.');
      return of([] as GeocodeCandidato[]);
    })).subscribe(cs => {
      this.loadingGeocode.set(false);
      this.stateService.setCandidatos(cs);
      if (cs.length >= 1) {
        this.stateService.seleccionarCandidato(cs[0]);
        setTimeout(() => this.mapComponent?.flyTo(cs[0].lat, cs[0].lng, 18), 0);
      }
    });
  }

  private async resolveCalleByText(text: string): Promise<CalleDto | null> {
    try {
      const cs = await firstValueFrom(this.malvinasService.getCalles(text).pipe(catchError(() => of([] as CalleDto[]))));
      return cs.length > 0 ? cs[0] : null;
    } catch { return null; }
  }

  private parseSmartInput(text: string): SmartParsed {
    const u = text.trim().toUpperCase();
    // ENTRE: "CALLE ENTRE CALLE1 Y CALLE2"
    const entre = u.match(/^(.+?)\s+ENTRE\s+(.+?)\s+Y\s+(.+)$/);
    if (entre) return { mode: 'entre-calles', mainCalleText: entre[1].trim(), activeCalle: entre[3].trim(), calleObj: entre[1].trim(), calleLim1: entre[2].trim(), calleLim2: entre[3].trim() };
    // ESQUINA via ESQ/ESQUINA/C/
    const esq = u.match(/^(.+?)\s+(?:ESQ\.?|ESQUINA|C\/)\s+(.+)$/);
    if (esq) return { mode: 'esquina', mainCalleText: esq[1].trim(), activeCalle: esq[2].trim(), calle2Text: esq[2].trim() };
    // ESQUINA via Y completo: "CALLE Y CALLE2" (ambas partes >= 2 chars)
    const esqY = u.match(/^(.{3,}?)\s+Y\s+(.{2,})$/);
    if (esqY) return { mode: 'esquina', mainCalleText: esqY[1].trim(), activeCalle: esqY[2].trim(), calle2Text: esqY[2].trim() };
    // ESQUINA parcial: "CALLE Y" (aun no escribio la segunda calle — no activar busqueda)
    const partialEsqY = u.match(/^(.{3,})\s+Y\s*$/);
    if (partialEsqY) return { mode: 'esquina', mainCalleText: partialEsqY[1].trim(), activeCalle: '', calle2Text: '' };
    // CALLE + ALTURA
    const alt = u.match(/^(.+?)\s+(\d{1,5})\s*$/);
    if (alt && parseInt(alt[2]) > 0) return { mode: 'calle-altura', mainCalleText: alt[1].trim(), activeCalle: alt[1].trim(), altura: parseInt(alt[2]) };
    return { mode: 'partial', mainCalleText: u, activeCalle: u };
  }

  private buildSuggestions(parsed: SmartParsed, calles: CalleDto[]): SearchSuggestion[] {
    return calles.slice(0, 7).map(c => {
      switch (parsed.mode) {
        case 'calle-altura': return { icon: 'navigation', modeIcon: 'pin', geocodable: true, calle: c, label: `${c.texto} ${parsed.altura}`, sub: `Calle + Altura \u2022 ${c.localidad}` };
        case 'esquina': return { icon: 'alt_route', modeIcon: 'alt_route', geocodable: true, calle: c, label: `${parsed.mainCalleText} esq ${c.texto}`, sub: `Esquina \u2022 ${c.localidad}` };
        case 'entre-calles': return { icon: 'swap_horiz', modeIcon: 'swap_horiz', geocodable: true, calle: c, label: `${parsed.calleObj} entre ${parsed.calleLim1} y ${c.texto}`, sub: `Entre calles \u2022 ${c.localidad}` };
        default: return { icon: 'place', modeIcon: 'chevron_right', geocodable: false, calle: c, label: c.texto, sub: c.localidad + (c.nombreAnterior ? ` \u2022 ex ${c.nombreAnterior}` : '') };
      }
    });
  }

  onParcelaSeleccionada(ref: ParcelaRef): void { this.parcelaActual.set(ref); }

  toggleColocarModo(): void { this.colocarModoActivo.update(v => !v); }

  onMapClick(evt: MapClickEvent): void {
    this.stateService.setPunto({ lat: evt.lat, lng: evt.lng, origenUbicacion: 'MANUAL_MAPA' });
    this.colocarModoActivo.set(false);
    this.doReversePoint(evt.lat, evt.lng);
  }

  private doReversePoint(lat: number, lng: number): void {
    this.loadingReverse.set(true); this.errorReverse.set(null); this.stateService.setReverseResultados([]);
    this.gisService.reversePoint({ lat, lng }).pipe(takeUntilDestroyed(this.destroyRef), catchError(err => {
      this.errorReverse.set(!err?.status || err.status >= 500 || err.status === 0 ? 'Reverse: backend GEO no disponible.' : 'Error reverse.');
      return of([] as GeocodeCandidato[]);
    })).subscribe(rs => {
      this.loadingReverse.set(false); this.stateService.setReverseResultados(rs);
      if (rs.length === 1) this.stateService.seleccionarReverseResultado(rs[0]);
    });
  }
  onZoomChange(z: number): void { this.currentZoom.set(z); }

  limpiarPunto(): void { this.stateService.clearPunto(); this.errorReverse.set(null); this.errorGeocode.set(null); }

  seleccionarCandidato(c: GeocodeCandidato): void {
    this.stateService.seleccionarCandidato(c);
    setTimeout(() => this.mapComponent?.flyTo(c.lat, c.lng, 18), 0);
  }

  onOverlayWheel(event: WheelEvent): void {
    if ((event.target as Element)?.closest?.('.suggestions-panel')) return;
    const container = (this.elementRef.nativeElement as Element)
      .querySelector<HTMLElement>('.map-container');
    if (!container) return;
    container.dispatchEvent(new WheelEvent('wheel', {
      deltaX: event.deltaX, deltaY: event.deltaY, deltaZ: event.deltaZ,
      deltaMode: event.deltaMode,
      ctrlKey: event.ctrlKey, shiftKey: event.shiftKey,
      altKey: event.altKey, metaKey: event.metaKey,
      bubbles: false, cancelable: true,
    }));
  }

  navegarAPunto(): void {
    const p = this.punto();
    if (p) setTimeout(() => this.mapComponent?.flyTo(p.lat, p.lng, 18), 0);
  }

  seleccionarReverseResultado(r: GeocodeCandidato): void { this.stateService.seleccionarReverseResultado(r); }
  aplicarReverse(): void { this.stateService.aplicarReverse(); }
  setTheme(mode: MapThemeMode): void { this.themeService.setMode(mode); }
  trackByCandidato(i: number, c: GeocodeCandidato): string { return `${c.lat}-${c.lng}-${i}`; }
}
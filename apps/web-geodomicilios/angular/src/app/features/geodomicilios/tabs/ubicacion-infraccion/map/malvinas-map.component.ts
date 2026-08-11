import {
  Component, ChangeDetectionStrategy, OnDestroy, AfterViewInit,
  ElementRef, ViewChild, inject, input, output, signal, effect, NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as maplibregl from 'maplibre-gl';
import { MapThemeService, MapThemeMode, MapThemeName } from '../../../../../core/services/map-theme.service';
import { GEO_CONSTANTS } from '../../../../../core/config/geo.constants';
import { GeoLocalMalvinasService } from '../../../../../core/services/geo-local-malvinas.service';

export interface ParcelaRef {
  circ: string; secc: string; quint: string;
  fracc: string; manz: string; parc: string;
}

export interface MapClickEvent {
  lat: number;
  lng: number;
}

const ZOOM_LEVELS = [...GEO_CONSTANTS.ZOOM_LEVELS];

function nearestZoom(z: number): number {
  return ZOOM_LEVELS.reduce((prev, curr) =>
    Math.abs(curr - z) < Math.abs(prev - z) ? curr : prev
  );
}
function nextZoom(z: number): number {
  return ZOOM_LEVELS.find(l => l > z + 0.01) ?? ZOOM_LEVELS[ZOOM_LEVELS.length - 1];
}
function prevZoom(z: number): number {
  return [...ZOOM_LEVELS].reverse().find(l => l < z - 0.01) ?? ZOOM_LEVELS[0];
}

const CLARO_THEME = {
  bgGeneral: '#F2F1EC',
  extLine: '#7E8990', extLineOpacity: 0.65,
  localityFill: ['match', ['get', 'id_loc'], 'AP','#F3DFC1','AS','#D8E7F5','GB','#E7D9F3','LP','#D8ECD9','MA','#F3E8B9','PN','#D2E9E7','TA','#EED8CD','TO','#DCE0F4','VM','#F0D9E0','#E8E8E8'],
  localityOutline: '#A89C90', localityLabel: '#444444', localityLabelHalo: '#FFFFFF',
  greenFill: '#CFE3C4', greenFillOpacity: 0.92, greenLine: '#5F8A58', greenLabel: '#315B36', greenLabelHalo: '#F4F5ED',
  barrioFill: '#DDDAD4', barrioFillOpacity: 0.05, barrioOutline: '#9A7446', barrioLabel: '#654722',
  labelHaloWidth: 1.7, barrioLabelHalo: '#F8F7F2',
  blockFill: '#E6E3DA', blockOutline: '#A8A49C', blockLabel: '#222B33', blockLabelHalo: '#F8F7F2',
  parcelFill: '#E6E3DA', parcelLine: '#718896', parcelLabel: '#455563', parcelLabelHalo: '#F8F7F2',
  parcelFullLabel: '#25313A', parcelFullLabelHalo: '#FFFFFF',
  parcelSelFill: '#2563EB', parcelSelFillOp: 0.18, parcelSelLine: '#1D4ED8',
  railCasing: '#585A60', railFill: '#F5F2EE', railTicks: '#777A80',
  stationFill: '#6D4DB3', stationStroke: '#FFFFFF', stationLabel: '#3E2E5A', stationLabelHalo: '#FFFFFF',
  casingPrimary: '#B5B0A8', fillPrimary: '#EDE5CE',
  casingSecondary: '#BFBAB2', fillSecondary: '#F0EBE3',
  casingLocal: '#EDE9E4', fillLocal: '#FCFBF8',
  casingUnknown: '#C8C4BC', fillUnknown: '#FCFBF8',
  pedestrianFill: '#E8E4DC',
  roadLabel: '#25313A', roadLabelHalo: '#FFFFFF', roadLabelPassage: '#484848',
};

const OSCURO_THEME = {
  bgGeneral: '#344A63',
  extLine: '#8EA3B5', extLineOpacity: 0.60,
  localityFill: '#3B526B',
  localityOutline: '#4A6278', localityLabel: '#7A9CC0', localityLabelHalo: '#1C3248',
  greenFill: '#365E4A', greenFillOpacity: 0.97, greenLine: '#77B98A', greenLabel: '#D7F2DD', greenLabelHalo: '#1E362A',
  barrioFill: '#2E4560', barrioFillOpacity: 0.20, barrioOutline: '#D0A96E', barrioLabel: '#FFD28A',
  labelHaloWidth: 0.5, barrioLabelHalo: '#1A2E42',
  blockFill: '#415A73', blockOutline: '#6A8AA0', blockLabel: '#F5F7F9', blockLabelHalo: '#243345',
  parcelFill: '#415A73', parcelLine: '#A7B5C3', parcelLabel: '#D7E1EA', parcelLabelHalo: '#26374B',
  parcelFullLabel: '#F2F6FA', parcelFullLabelHalo: '#16212D',
  parcelSelFill: '#FFB000', parcelSelFillOp: 0.25, parcelSelLine: '#FFB000',
  railCasing: '#AAA5B0', railFill: '#243345', railTicks: '#D0CBD4',
  stationFill: '#B58DFF', stationStroke: '#21182F', stationLabel: '#C8A0FF', stationLabelHalo: '#14082A',
  casingPrimary: '#1A2A3B', fillPrimary: '#26364A',
  casingSecondary: '#1A2A3B', fillSecondary: '#26364A',
  casingLocal: '#1A2A3B', fillLocal: '#26364A',
  casingUnknown: '#1A2A3B', fillUnknown: '#26364A',
  pedestrianFill: '#344A63',
  roadLabel: '#F2F6FA', roadLabelHalo: '#1C3248', roadLabelPassage: '#8FA8C0',
};

@Component({
  selector: 'app-malvinas-map',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: '<div #mapContainer class="map-container"></div>',
  styleUrl: './malvinas-map.component.scss',

})
export class MalvinasMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  readonly markerLat = input<number | null>(null);
  readonly markerLng = input<number | null>(null);
  readonly colocarModoActivo = input<boolean>(false);

  readonly mapClick = output<MapClickEvent>();
  readonly mapReady = output<void>();
  readonly zoomChange = output<number>();
  readonly parcelaSeleccionada = output<ParcelaRef>();

  private readonly themeService = inject(MapThemeService);
  private readonly ngZone = inject(NgZone);
  private readonly geoService = inject(GeoLocalMalvinasService);

  private map: maplibregl.Map | null = null;
  private marker: maplibregl.Marker | null = null;
  private mapLoaded = false;
  private snapPending = false;
  private snapTimeout: ReturnType<typeof setTimeout> | null = null;

  private readonly _themeEffect = effect(() => {
    const theme = this.themeService.themeName();
    if (this.mapLoaded && this.map) this.applyTheme(theme);
  });

  private readonly _markerEffect = effect(() => {
    const lat = this.markerLat();
    const lng = this.markerLng();
    if (this.mapLoaded && this.map) {
      if (lat !== null && lng !== null) this.setMarker(lat, lng);
      else this.clearMarker();
    }
  });

  private readonly _cursorEffect = effect(() => {
    const active = this.colocarModoActivo();
    if (this.mapLoaded && this.map) {
      this.map.getCanvas().style.cursor = active ? 'crosshair' : '';
    }
  });


  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => this.initMap());
  }

  ngOnDestroy(): void {
    this.marker?.remove();
    this.map?.remove();
    this.map = null;
  }

  private initMap(): void {
    fetch(GEO_CONSTANTS.STYLE_URL)
      .then(r => r.json())
      .then(style => {
        // Use Martin as font/glyph server (generates PBF from TTF/OTF on the fly)
        style.glyphs = `${GEO_CONSTANTS.MARTIN_BASE_URL}/font/{fontstack}/{range}`;
        // Replace __MARTIN_BASE_URL__ placeholder in all style sources loaded from the JSON asset
        if (style.sources) {
          Object.values(style.sources as Record<string, any>).forEach((src: any) => {
            if (src.url && typeof src.url === 'string') {
              src.url = src.url.replace('__MARTIN_BASE_URL__', GEO_CONSTANTS.MARTIN_BASE_URL);
            }
            if (Array.isArray(src.tiles)) {
              src.tiles = src.tiles.map((t: string) =>
                t.replace('__MARTIN_BASE_URL__', GEO_CONSTANTS.MARTIN_BASE_URL)
              );
            }
          });
        }
        // Martin serves one font at a time — strip fallback fonts from text-font arrays
        style.layers = style.layers?.map((layer: any) => {
          if (Array.isArray(layer.layout?.['text-font']) && layer.layout['text-font'].length > 1) {
            layer.layout['text-font'] = [layer.layout['text-font'][0]];
          }
          return layer;
        });
        const map = new maplibregl.Map({
          container: this.mapContainer.nativeElement,
          style,
          center: [GEO_CONSTANTS.MAP_CENTER.lng, GEO_CONSTANTS.MAP_CENTER.lat],
          zoom: GEO_CONSTANTS.ZOOM_DEFAULT,
          pixelRatio: window.devicePixelRatio || 1,
          maxBounds: [
            [GEO_CONSTANTS.MAP_BOUNDS_SW[0], GEO_CONSTANTS.MAP_BOUNDS_SW[1]],
            [GEO_CONSTANTS.MAP_BOUNDS_NE[0], GEO_CONSTANTS.MAP_BOUNDS_NE[1]],
          ] as maplibregl.LngLatBoundsLike,
        });

        map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
        map.addControl(new maplibregl.ScaleControl({ maxWidth: 100, unit: 'metric' }), 'bottom-right');

        map.once('load', () => {
          this.mapLoaded = true;
          const theme = this.themeService.themeName();
          this.loadComerciosLayer();
          this.applyTheme(theme);

          // ── Highlight source for selected parcel ─────────────────────
          map.addSource('selected-parcela', {
            type: 'geojson',
            data: { type: 'FeatureCollection', features: [] },
          });
          map.addLayer({
            id: 'parcela-highlight-fill',
            type: 'fill',
            source: 'selected-parcela',
            paint: { 'fill-color': '#2563EB', 'fill-opacity': 0.22 },
          });
          map.addLayer({
            id: 'parcela-highlight-outline',
            type: 'line',
            source: 'selected-parcela',
            paint: { 'line-color': '#1D4ED8', 'line-width': 2.5 },
          });

          // ── Parcel click handler ──────────────────────────────────────
          map.on('mouseenter', 'parcel-fill', () => {
            map.getCanvas().style.cursor = 'pointer';
          });
          map.on('mouseleave', 'parcel-fill', () => {
            if (!this.colocarModoActivo()) map.getCanvas().style.cursor = '';
          });
          map.on('click', 'parcel-fill', (ev) => {
            const feat = ev.features?.[0];
            if (!feat?.properties) return;
            const p = feat.properties;
            const ref: ParcelaRef = {
              circ: String(p['circunsc'] ?? ''),
              secc: String(p['secc'] ?? ''),
              quint: String(p['quint'] ?? '0'),
              fracc: String(p['fracc'] ?? '0'),
              manz: String(p['manz'] ?? ''),
              parc: String(p['parc'] ?? ''),
            };
            // Fetch full (unclipped) geometry from PostGIS and use it for highlight
            this.geoService.getParcelaGeom(ref.circ, ref.secc, ref.quint, ref.fracc, ref.manz, ref.parc)
              .subscribe({
                next: (geomJson: any) => {
                  const src = map.getSource('selected-parcela') as maplibregl.GeoJSONSource;
                  if (src) src.setData({
                    type: 'FeatureCollection',
                    features: [{ type: 'Feature', geometry: geomJson, properties: {} }],
                  });
                },
                error: () => {
                  // Fallback to clipped tile geometry
                  const src = map.getSource('selected-parcela') as maplibregl.GeoJSONSource;
                  if (src) src.setData({
                    type: 'FeatureCollection',
                    features: [{ type: 'Feature', geometry: JSON.parse(JSON.stringify(feat.geometry)), properties: {} }],
                  });
                }
              });
            this.ngZone.run(() => this.parcelaSeleccionada.emit(ref));
          });

          setTimeout(() => {
            if (!this.snapPending) this.snapZoomNow();
          }, 300);

          if (this.markerLat() !== null && this.markerLng() !== null) {
            this.setMarker(this.markerLat()!, this.markerLng()!);
          }

          this.ngZone.run(() => {
            this.mapReady.emit();
            this.zoomChange.emit(Math.round(map.getZoom() * 10) / 10);
          });
        });

        map.on('zoom', () => {
          this.ngZone.run(() => this.zoomChange.emit(Math.round(map.getZoom() * 10) / 10));
        });

        map.on('zoomend', () => {
          if (this.snapTimeout) clearTimeout(this.snapTimeout);
          if (this.snapPending) { this.snapPending = false; return; }
          this.snapTimeout = setTimeout(() => {
            if (!this.snapPending) this.snapZoomNow();
          }, 130);
        });

        map.on('dblclick', (e) => {
          e.preventDefault();
          this.snapPending = true;
          map.easeTo({ zoom: nextZoom(map.getZoom()), center: e.lngLat, duration: 320 });
        });

        map.on('click', (e) => {
          const onParcel = map.queryRenderedFeatures(e.point, { layers: ['parcel-fill'] }).length > 0;
          if (this.colocarModoActivo() && !onParcel) {
            this.ngZone.run(() => this.mapClick.emit({ lat: e.lngLat.lat, lng: e.lngLat.lng }));
          }
        });

        this.map = map;
      })
      .catch(err => console.error('[MalvinasMap] Error loading style:', err));
  }

  private snapZoomNow(): void {
    if (!this.map) return;
    const z = this.map.getZoom();
    const target = nearestZoom(z);
    if (Math.abs(target - z) < 0.025) { this.snapPending = false; return; }
    this.snapPending = true;
    this.map.easeTo({ zoom: target, duration: 200 });
  }

  private applyTheme(name: MapThemeName): void {
    if (!this.map || !this.mapLoaded) return;
    this.ngZone.runOutsideAngular(() => {
    const t = name === 'day' ? CLARO_THEME : OSCURO_THEME;
    const sp = (layerId: string, prop: string, val: unknown) => {
      if (this.map!.getLayer(layerId)) this.map!.setPaintProperty(layerId, prop, val);
    };
    sp('background', 'background-color', t.bgGeneral);
    sp('locality-fill', 'fill-color', t.localityFill);
    sp('locality-outline', 'line-color', t.localityOutline);
    sp('entorno-muni-line', 'line-color', t.extLine);
    sp('entorno-muni-line', 'line-opacity', t.extLineOpacity);
    sp('espacio-verde-fill', 'fill-color', t.greenFill);
    sp('espacio-verde-fill', 'fill-opacity', t.greenFillOpacity);
    sp('espacio-verde-outline', 'line-color', t.greenLine);
    sp('barrio-fill', 'fill-color', t.barrioFill);
    sp('barrio-fill', 'fill-opacity', t.barrioFillOpacity);
    sp('block-fill', 'fill-color', t.blockFill);
    sp('block-outline', 'line-color', t.blockOutline);
    sp('parcel-fill', 'fill-color', t.parcelFill);
    sp('parcel-outline', 'line-color', t.parcelLine);
    sp('street-casing-primary', 'line-color', t.casingPrimary);
    sp('street-fill-primary', 'line-color', t.fillPrimary);
    sp('street-casing-secondary', 'line-color', t.casingSecondary);
    sp('street-fill-secondary', 'line-color', t.fillSecondary);
    sp('street-casing-local', 'line-color', t.casingLocal);
    sp('street-fill-local', 'line-color', t.fillLocal);
    sp('road-unknown-casing', 'line-color', t.casingUnknown);
    sp('road-unknown-fill', 'line-color', t.fillUnknown);
    sp('street-fill-pedestrian', 'line-color', t.pedestrianFill);
    sp('ferrocarril-estacion-circle', 'circle-color', t.stationFill);
    sp('ferrocarril-estacion-circle', 'circle-stroke-color', t.stationStroke);
    sp('ferrocarril-casing',           'line-color',          t.railCasing);
    sp('ferrocarril-fill',             'line-color',          t.railFill);
    sp('ferrocarril-ticks',            'line-color',          t.railTicks);
    sp('parcel-selected-fill',         'fill-color',          t.parcelSelFill);
    sp('parcel-selected-fill',         'fill-opacity',        t.parcelSelFillOp);
    sp('parcel-selected-outline',      'line-color',          t.parcelSelLine);
    sp('street-label-primary', 'text-color', t.roadLabel);
    sp('street-label-primary', 'text-halo-color', t.roadLabelHalo);
    sp('street-label-secondary', 'text-color', t.roadLabel);
    sp('street-label-secondary', 'text-halo-color', t.roadLabelHalo);
    sp('street-label-local', 'text-color', t.roadLabel);
    sp('street-label-local', 'text-halo-color', t.roadLabelHalo);
    sp('road-unknown-label', 'text-color', t.roadLabel);
    sp('road-unknown-label', 'text-halo-color', t.roadLabelHalo);
    sp('street-label-passage', 'text-color', t.roadLabelPassage);
    sp('street-label-primary',   'text-halo-width', t.labelHaloWidth);
    sp('street-label-secondary', 'text-halo-width', t.labelHaloWidth);
    sp('street-label-local',     'text-halo-width', t.labelHaloWidth);
    sp('road-unknown-label',     'text-halo-width', t.labelHaloWidth);
    sp('street-label-passage',   'text-halo-width', t.labelHaloWidth);
    sp('street-label-passage', 'text-halo-color', t.roadLabelHalo);
    sp('barrio-outline', 'line-color', t.barrioOutline);
    sp('barrio-label', 'text-color', t.barrioLabel);
    sp('barrio-label', 'text-halo-color', t.barrioLabelHalo);
    sp('block-label', 'text-color', t.blockLabel);
    sp('block-label', 'text-halo-color', t.blockLabelHalo);
    sp('parcel-label-short', 'text-color', t.parcelLabel);
    sp('parcel-label-short', 'text-halo-color', t.parcelLabelHalo);
    sp('parcel-label-full', 'text-color', t.parcelFullLabel);
    sp('parcel-label-full', 'text-halo-color', t.parcelFullLabelHalo);
    sp('locality-label', 'text-color', t.localityLabel);
    sp('locality-label', 'text-halo-color', t.localityLabelHalo);
    sp('espacio-verde-label', 'text-color', t.greenLabel);
    sp('espacio-verde-label', 'text-halo-color', t.greenLabelHalo);
    sp('ferrocarril-estacion-label', 'text-color', t.stationLabel);
    sp('ferrocarril-estacion-label', 'text-halo-color', t.stationLabelHalo);
    sp('comercios-parcela-label', 'text-color',          name === 'day' ? '#1a3a5c' : '#7CC8F8');
    sp('comercios-parcela-label', 'text-halo-color',     name === 'day' ? 'rgba(242,241,236,0.9)' : 'rgba(52,74,99,0.9)');
    sp('comercios-parcela-label', 'text-halo-width',      1);
    sp('comercios-manzana-label', 'text-color',           name === 'day' ? '#1a3a5c' : '#7CC8F8');
    sp('comercios-manzana-label', 'text-halo-color',      name === 'day' ? 'rgba(242,241,236,0.9)' : 'rgba(52,74,99,0.9)');
    sp('comercios-manzana-label', 'text-halo-width',      1);
    }); // runOutsideAngular
  }

  public setMarker(lat: number, lng: number): void {
    if (!this.map) return;
    this.ngZone.runOutsideAngular(() => {
      if (!this.marker) {
        this.marker = new maplibregl.Marker({ color: '#e53935', draggable: true });
        // MapLibre CSS may not be loaded - set critical inline style so the marker is positioned correctly
        Object.assign(this.marker.getElement().style, { position: 'absolute', top: '0', left: '0', willChange: 'transform', cursor: 'pointer' });
        this.marker.on('dragend', () => {
          const pos = this.marker!.getLngLat();
          this.ngZone.run(() => this.mapClick.emit({ lat: pos.lat, lng: pos.lng }));
        });
        this.marker.setLngLat([lng, lat]);
        this.marker.addTo(this.map!);
      } else {
        this.marker.setLngLat([lng, lat]);
      }
    });
  }

  private clearMarker(): void {
    this.ngZone.runOutsideAngular(() => {
      this.marker?.remove();
      this.marker = null;
    });
  }

  public flyTo(lat: number, lng: number, zoom?: number): void {
    if (!this.map) return;
    const z = zoom !== undefined ? nearestZoom(zoom) : nearestZoom(Math.max(this.map.getZoom(), 17));
    this.snapPending = true;
    this.ngZone.runOutsideAngular(() => {
      this.map!.flyTo({ center: [lng, lat], zoom: z, duration: 1200 });
    });
  }

  private createComercioIconData(): ImageData {
    const size = 40;
    const half = size / 2;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d')!;

    // Orange circular badge
    ctx.beginPath();
    ctx.arc(half, half, half - 1, 0, Math.PI * 2);
    ctx.fillStyle = '#F57C00';
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.8)';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // White storefront silhouette
    ctx.fillStyle = '#FFFFFF';

    // Roof / awning triangle
    ctx.beginPath();
    ctx.moveTo(7, 22);
    ctx.lineTo(half, 9);
    ctx.lineTo(size - 7, 22);
    ctx.closePath();
    ctx.fill();

    // Store body
    ctx.fillRect(9, 22, size - 18, 11);

    // Door cutout
    ctx.fillStyle = '#F57C00';
    ctx.fillRect(half - 4, 26, 8, 7);

    return ctx.getImageData(0, 0, size, size);
  }

  private loadComerciosLayer(): void {
    const iconData = this.createComercioIconData();
    this.map!.addImage('comercio-icon', iconData, { pixelRatio: 2 });

    this.map!.addSource('comercios-parcela', {
      type: 'vector',
      tiles: [`${GEO_CONSTANTS.MARTIN_BASE_URL}/comercios_parcela/{z}/{x}/{y}`],
      minzoom: 18, maxzoom: 22,
    });
    this.map!.addLayer({
      id: 'comercios-parcela-dot',
      type: 'symbol',
      source: 'comercios-parcela',
      'source-layer': 'comercios_parcela',
      minzoom: 18,
      layout: {
        'icon-image': 'comercio-icon',
        'icon-size': 1,
        'icon-allow-overlap': false,
        'icon-anchor': 'center',
      },
    });
    this.map!.addLayer({
      id: 'comercios-parcela-label',
      type: 'symbol',
      source: 'comercios-parcela',
      'source-layer': 'comercios_parcela',
      minzoom: 18,
      layout: {
        'text-field': ['get', 'label'],
        'text-size': 9,
        'text-font': ['Open Sans Regular'],
        'text-anchor': 'top',
        'text-offset': [0, 0.5],
        'text-max-width': 8,
        'text-allow-overlap': false,
        'text-ignore-placement': true,
        'text-optional': true,
        'symbol-sort-key': 10,
      },
      paint: {
        'text-color': '#1a3a5c',
        'text-halo-color': 'rgba(52,74,99,0.0)',
        'text-halo-width': 1,
      },
    });

    // Source manzana-level (zoom 15-17, entrega a parcela en zoom 18+)
    this.map!.addSource('comercios-manzana', {
      type: 'vector',
      tiles: [`${GEO_CONSTANTS.MARTIN_BASE_URL}/comercios_manzana/{z}/{x}/{y}`],
      minzoom: 14, maxzoom: 22,
    });
    this.map!.addLayer({
      id: 'comercios-manzana-dot',
      type: 'symbol',
      source: 'comercios-manzana',
      'source-layer': 'comercios_manzana',
      minzoom: 15, maxzoom: 18,
      layout: {
        'icon-image': 'comercio-icon',
        'icon-size': 0.8,
        'icon-allow-overlap': false,
        'icon-anchor': 'center',
      },
    });
    this.map!.addLayer({
      id: 'comercios-manzana-label',
      type: 'symbol',
      source: 'comercios-manzana',
      'source-layer': 'comercios_manzana',
      minzoom: 15, maxzoom: 18,
      layout: {
        'text-field': ['get', 'label'],
        'text-size': 8,
        'text-font': ['Open Sans Regular'],
        'text-anchor': 'top',
        'text-offset': [0, 0.5],
        'text-max-width': 8,
        'text-allow-overlap': false,
        'text-ignore-placement': true,
        'text-optional': true,
        'symbol-sort-key': 11,
      },
      paint: {
        'text-color': '#1a3a5c',
        'text-halo-color': 'rgba(242,241,236,0.9)',
        'text-halo-width': 1,
      },
    });
  }

  public resize(): void {
    this.ngZone.runOutsideAngular(() => this.map?.resize());
  }
}

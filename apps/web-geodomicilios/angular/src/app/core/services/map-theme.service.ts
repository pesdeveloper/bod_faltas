import { Injectable, signal } from '@angular/core';

export type MapThemeMode = 'auto' | 'day' | 'night';
export type MapThemeName = 'day' | 'night';

const STORAGE_KEY = 'malvinas-map-theme';
const TZ = 'America/Argentina/Buenos_Aires';
const DAY_START = 7 * 60;
const NIGHT_START = 19 * 60;

@Injectable({ providedIn: 'root' })
export class MapThemeService {
  private readonly _mode = signal<MapThemeMode>(this.loadMode());
  private readonly _themeName = signal<MapThemeName>(this.resolveThemeName(this.loadMode()));
  private autoInterval: ReturnType<typeof setInterval> | null = null;

  readonly mode = this._mode.asReadonly();
  readonly themeName = this._themeName.asReadonly();

  setMode(mode: MapThemeMode): void {
    this._mode.set(mode);
    try { localStorage.setItem(STORAGE_KEY, mode); } catch { /* ignore */ }
    const name = mode === 'day' ? 'day' : mode === 'night' ? 'night' : this.resolveAutoTheme();
    this._themeName.set(name);
    if (mode === 'auto') this.startAutoCheck();
    else this.stopAutoCheck();
  }

  resolveAutoTheme(): MapThemeName {
    try {
      const ts = new Date().toLocaleTimeString('es-AR', { timeZone: TZ, hour: '2-digit', minute: '2-digit', hour12: false });
      const [h, m] = ts.split(':').map(Number);
      const mins = h * 60 + m;
      return mins >= DAY_START && mins < NIGHT_START ? 'day' : 'night';
    } catch {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'night' : 'day';
    }
  }

  startAutoCheck(): void {
    this.stopAutoCheck();
    this.autoInterval = setInterval(() => {
      if (this._mode() !== 'auto') return;
      const nt = this.resolveAutoTheme();
      if (nt !== this._themeName()) this._themeName.set(nt);
    }, 5 * 60 * 1000);
  }

  stopAutoCheck(): void {
    if (this.autoInterval) { clearInterval(this.autoInterval); this.autoInterval = null; }
  }

  private loadMode(): MapThemeMode {
    try {
      const s = localStorage.getItem(STORAGE_KEY);
      if (s === 'auto' || s === 'day' || s === 'night') return s;
      // Migrate legacy values stored before rename
      if (s === 'claro') return 'day';
      if (s === 'oscuro') return 'night';
    } catch { /* ignore */ }
    return 'auto';
  }

  private resolveThemeName(mode: MapThemeMode): MapThemeName {
    return mode === 'day' ? 'day' : mode === 'night' ? 'night' : this.resolveAutoTheme();
  }
}

import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { CommonModule } from '@angular/common';
import { GeoDomiciliosStateService } from '../../core/services/geodomicilios-state.service';
import { DomicilioInfractorTabComponent } from './tabs/domicilio-infractor/domicilio-infractor-tab.component';
import { MunicipioLicenciaTabComponent } from './tabs/municipio-licencia/municipio-licencia-tab.component';
import { UbicacionInfraccionTabComponent } from './tabs/ubicacion-infraccion/ubicacion-infraccion-tab.component';

@Component({
  selector: 'app-geodomicilios-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, MatTabsModule, MatIconModule, MatBadgeModule,
    DomicilioInfractorTabComponent,
    MunicipioLicenciaTabComponent,
    UbicacionInfraccionTabComponent,
  ],
  templateUrl: './geodomicilios-shell.component.html',
  styleUrl: './geodomicilios-shell.component.scss',
})
export class GeoDomiciliosShellComponent {
  readonly state = inject(GeoDomiciliosStateService);
  readonly tabIndex = signal(0);

  onTabChange(index: number): void {
    this.tabIndex.set(index);
  }
}
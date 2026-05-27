import { Routes } from '@angular/router';
import { DemoShellComponent } from './features/demo/demo-shell.component';
import { InfractorActaPageComponent } from './features/infractor/infractor-acta-page.component';
import { NotificadorMunicipalPageComponent } from './features/demo/notificador-municipal-page.component';

export const routes: Routes = [
  { path: '', component: DemoShellComponent },
  { path: 'correo-postal', redirectTo: 'correo-postal/actas', pathMatch: 'full' },
  { path: 'correo-postal/actas', component: DemoShellComponent },
  { path: 'correo-postal/lotes', component: DemoShellComponent },
  { path: 'infractor/actas/:codigoQr', component: InfractorActaPageComponent },
  { path: 'notificador-municipal', component: NotificadorMunicipalPageComponent },
  { path: '**', redirectTo: '' },
];
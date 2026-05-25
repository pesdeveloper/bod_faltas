import { Routes } from '@angular/router';
import { DemoShellComponent } from './features/demo/demo-shell.component';
import { InfractorActaPageComponent } from './features/infractor/infractor-acta-page.component';

export const routes: Routes = [
  { path: '', component: DemoShellComponent },
  { path: 'infractor/actas/:codigoQr', component: InfractorActaPageComponent },
  { path: '**', redirectTo: '' },
];

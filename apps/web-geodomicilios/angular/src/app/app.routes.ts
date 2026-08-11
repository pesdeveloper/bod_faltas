import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/geodomicilios/geodomicilios-shell.component').then(m => m.GeoDomiciliosShellComponent),
  },
  { path: '**', redirectTo: '' },
];
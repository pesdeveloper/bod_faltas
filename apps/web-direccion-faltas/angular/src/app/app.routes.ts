import { Routes } from '@angular/router';
import { DemoShellComponent } from './features/demo/demo-shell.component';

export const routes: Routes = [
  { path: '', component: DemoShellComponent },
  { path: '**', redirectTo: '' },
];

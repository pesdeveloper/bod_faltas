import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-demo-notification-tools',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './demo-notification-tools.component.html',
  styleUrl: './demo-notification-tools.component.scss',
})
export class DemoNotificationToolsComponent {
  @Input() correoActivo = false;
  @Output() readonly abrirCorreoPostal = new EventEmitter<void>();
  @Output() readonly abrirNotificadorMunicipal = new EventEmitter<void>();
}

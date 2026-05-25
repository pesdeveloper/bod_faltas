import { Component, Input } from '@angular/core';
import { ActaNotificacionTipificadaDemo } from '../../core/models/prototipo-faltas.models';

@Component({
  selector: 'app-demo-acta-notificaciones',
  standalone: true,
  templateUrl: './demo-acta-notificaciones.component.html',
  styleUrl: './demo-acta-notificaciones.component.scss',
})
export class DemoActaNotificacionesComponent {
  @Input() notificaciones: ActaNotificacionTipificadaDemo[] | null | undefined = [];

  notificacionesTipificadas(): ActaNotificacionTipificadaDemo[] {
    const items = this.notificaciones;
    return items?.length ? [...items] : [];
  }

  canalNotificacionTexto(notif: ActaNotificacionTipificadaDemo): string {
    const tipificado = notif.canalTipificado?.trim();
    if (tipificado) {
      return tipificado;
    }
    const canal = notif.canal?.trim();
    return canal || '-';
  }

  destinatarioNotificacionTexto(notif: ActaNotificacionTipificadaDemo): string | null {
    const resumen = notif.destinatarioResumen?.trim();
    if (resumen) {
      return resumen;
    }
    const nombre = notif.destinatarioNombre?.trim();
    return nombre || null;
  }

  textoOpcionalNotificacion(valor: string | null | undefined): string | null {
    const texto = valor?.trim();
    return texto ? texto : null;
  }
}

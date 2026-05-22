from pathlib import Path

OUT = Path(r"S:\Source\Repos\Bod-Faltas\apps\web-direccion-faltas\angular\src\app\features\demo\demo-shell.component.html")

HTML = r'''<mat-sidenav-container class="demo-layout" [autosize]="true">
  <mat-sidenav mode="side" opened class="demo-sidenav" [class.demo-sidenav--collapsed]="bandejasColapsadas()">
    <div class="sidenav-header">
      @if (!bandejasColapsadas()) {
        <span>Bandejas</span>
      }
      <button
        mat-icon-button
        type="button"
        class="sidenav-toggle"
        [attr.aria-expanded]="!bandejasColapsadas()"
        aria-label="Alternar bandejas"
        (click)="toggleBandejas()"
      >
        <mat-icon>menu</mat-icon>
      </button>
    </div>
    <mat-nav-list>
      @for (b of bandejas; track b) {
        <a
          mat-list-item
          class="sidenav-link"
          [class.active]="b === bandejaSeleccionada()"
          [attr.title]="etiquetaBandeja(b)"
          href="#"
          (click)="$event.preventDefault(); seleccionarBandeja(b)"
        >
          @if (bandejasColapsadas()) {
            <span class="sidenav-abbr">{{ abreviaturaBandeja(b) }}</span>
          } @else {
            <span class="sidenav-label">{{ etiquetaBandeja(b) }}</span>
          }
        </a>
      }
    </mat-nav-list>
  </mat-sidenav>

  <mat-sidenav-content>
    <mat-toolbar color="primary" class="demo-toolbar">
      <span class="toolbar-title">{{ tituloBandejaActual() }}</span>
      <div class="demo-actions">
        <button mat-stroked-button type="button" [disabled]="creandoDemo() !== null" (click)="crearDemo('TRANSITO')">
          Crear demo tránsito
        </button>
        <button mat-stroked-button type="button" [disabled]="creandoDemo() !== null" (click)="crearDemo('INSPECCIONES')">
          Crear demo inspecciones
        </button>
        <button mat-stroked-button type="button" [disabled]="creandoDemo() !== null" (click)="crearDemo('FISCALIZACION')">
          Crear demo fiscalización
        </button>
        <button mat-stroked-button type="button" [disabled]="creandoDemo() !== null" (click)="crearDemo('BROMATOLOGIA')">
          Crear demo bromatología
        </button>
      </div>
    </mat-toolbar>

    @if (creandoDemo() !== null || altaDemoMensaje() || altaDemoError()) {
      <div class="demo-feedback" role="status" aria-live="polite">
        @if (creandoDemo() !== null) {
          <p class="demo-feedback-info">Creando acta demo ({{ creandoDemo() }})...</p>
        }
        @if (altaDemoMensaje()) {
          <p class="demo-feedback-ok">{{ altaDemoMensaje() }}</p>
        }
        @if (altaDemoError()) {
          <p class="demo-feedback-error">{{ altaDemoError() }}</p>
        }
      </div>
    }

    <div class="demo-content">
      <section class="list-panel" aria-label="Listado de actas">
        <div class="list-panel__tools">
          <label class="list-search">
            <span class="list-search__label">Buscar acta</span>
            <input
              type="search"
              class="list-search__input"
              placeholder="Número de acta..."
              [ngModel]="textoBusquedaActa()"
              (ngModelChange)="actualizarTextoBusquedaActa($event)"
              autocomplete="off"
            />
          </label>
          <label class="list-preference">
            <input
              type="checkbox"
              [checked]="seguirActaAlCambiarBandeja()"
              (change)="cambiarSeguirActaAlCambiarBandeja($any($event.target).checked)"
            />
            <span>Seguir acta al cambiar de bandeja</span>
          </label>
        </div>
        @if (seguimientoActaMensaje()) {
          <p class="list-panel__feedback" role="status">{{ seguimientoActaMensaje() }}</p>
        }
        @if (listadoEstado() === 'loading') {
          <div class="state-box">
            <mat-spinner diameter="36"></mat-spinner>
            <p>Cargando actas...</p>
          </div>
        } @else if (listadoEstado() === 'error') {
          <div class="state-box">
            <p>No se pudo cargar el listado.</p>
            <p>{{ listadoError() }}</p>
            <button mat-button type="button" (click)="recargarListado()">Reintentar</button>
          </div>
        } @else if (actas().length === 0) {
          <div class="state-box">
            <p>No hay actas en esta bandeja.</p>
          </div>
        } @else if (actasFiltradas().length === 0) {
          <div class="state-box">
            <p>No hay actas en esta bandeja que coincidan con la búsqueda.</p>
          </div>
        } @else {
          @for (acta of actasFiltradas(); track acta.id) {
            <button
              type="button"
              mat-button
              class="acta-row"
              [class.selected]="acta.id === actaSeleccionadaId()"
              (click)="seleccionarActa(acta.id)"
            >
              <div class="row-main">
                <span class="row-numero">{{ acta.numeroActa }}</span>
                <span class="row-status">{{ acta.situacionAdministrativaActual }}</span>
              </div>
              <div class="row-resumen">{{ acta.infractorNombre }}</div>
              <div class="row-meta">{{ acta.estadoProcesoActual }} · {{ acta.situacionAdministrativaActual }}</div>
              <div class="badges badge-list">
                @for (badge of badgesDe(acta); track badge.etiqueta) {
                  <span
                    [class]="'badge ' + claseBadge(badge)"
                    [title]="tituloBadge(badge.etiqueta)"
                  >{{ etiquetaBadgeCompacta(badge.etiqueta) }}</span>
                }
              </div>
            </button>
          }
        }
      </section>

      <aside class="detail-panel" aria-label="Detalle del acta">
        @if (!actaSeleccionadaId()) {
          <div class="state-box state-box--empty-detail">
            <mat-icon class="placeholder-icon">description</mat-icon>
            <p>Seleccioná un acta para ver su detalle operativo.</p>
          </div>
        } @else if (detalleEstado() === 'loading') {
          <div class="state-box">
            <mat-spinner diameter="36"></mat-spinner>
            <p>Cargando detalle...</p>
          </div>
        } @else if (detalleEstado() === 'error') {
          <div class="state-box">
            <p>No se pudo cargar el detalle.</p>
            <p>{{ detalleError() }}</p>
            <button mat-button type="button" (click)="recargarDetalle()">Reintentar</button>
          </div>
        } @else if (detalle()) {
          @if (detalle(); as d) {
          <header class="detail-header">
            <h2>{{ d.numeroActa }}</h2>
            <p class="detail-id">{{ d.id }}</p>
            <dl class="detail-dl detail-dl--compact">
              <dt>Dependencia</dt>
              <dd>{{ d.dependenciaDemo ?? '—' }}</dd>
              <dt>Bandeja</dt>
              <dd>{{ etiquetaBandeja(d.bandejaActual) }}</dd>
              <dt>Estado proceso</dt>
              <dd>{{ d.estadoProcesoActual }}</dd>
              <dt>Situación administrativa</dt>
              <dd>{{ d.situacionAdministrativaActual }}</dd>
              @if (d.cerrabilidad?.resultadoFinal) {
                <dt>Resultado final</dt>
                <dd>{{ d.cerrabilidad.resultadoFinal }}</dd>
              }
              @if (d.cerrabilidad != null) {
                <dt>Cerrable</dt>
                <dd>{{ etiquetaSiNo(d.cerrabilidad.cerrable) }}</dd>
              }
            </dl>
            <div class="badges badge-list">
              @for (badge of badgesDe(d); track badge.etiqueta) {
                <span
                  [class]="'badge ' + claseBadge(badge)"
                  [title]="tituloBadge(badge.etiqueta)"
                >{{ etiquetaBadgeCompacta(badge.etiqueta) }}</span>
              }
            </div>
          </header>

          @if (puedeMostrarBloqueCierreActa()) {
          <section class="detail-card" aria-labelledby="cierre-acta-titulo">
            <h3 id="cierre-acta-titulo" class="detail-card__title">Cierre del acta</h3>
            @if (cierreActaMensaje() || cierreActaError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (cierreActaMensaje()) {
                  <p class="demo-feedback-ok">{{ cierreActaMensaje() }}</p>
                }
                @if (cierreActaError()) {
                  <p class="demo-feedback-error">{{ cierreActaError() }}</p>
                }
              </div>
            }
            @if (d.estaCerrada) {
              <p class="detail-empty">El acta ya está cerrada.</p>
            } @else if (puedeMostrarCierreActa()) {
              <p class="detail-card__text">El acta es cerrable, pero requiere cierre explícito.</p>
              <div class="cierre-actions">
                <button
                  mat-flat-button
                  type="button"
                  color="primary"
                  [disabled]="cerrandoActa()"
                  (click)="cerrarActa()"
                >
                  @if (cerrandoActa()) {
                    Cerrando...
                  } @else {
                    Cerrar acta
                  }
                </button>
              </div>
            } @else {
              <p class="detail-card__hint">
                @if (d.cerrabilidad?.motivoNoCerrable) {
                  El acta no es cerrable: {{ d.cerrabilidad.motivoNoCerrable }}
                } @else {
                  El acta no es cerrable.
                }
              </p>
            }
          </section>
          }

          @if (puedeMostrarBloqueArchivoReingreso()) {
          <section class="detail-card" aria-labelledby="archivo-acta-titulo">
            <h3 id="archivo-acta-titulo" class="detail-card__title">Archivo / reingreso</h3>
            @if (archivoMensaje() || archivoError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (archivoMensaje()) {
                  <p class="demo-feedback-ok">{{ archivoMensaje() }}</p>
                }
                @if (archivoError()) {
                  <p class="demo-feedback-error">{{ archivoError() }}</p>
                }
              </div>
            }
            <p class="detail-card__text">
              Bandeja actual: {{ etiquetaBandeja(d.bandejaActual) }}.
            </p>
            @if (d.bandejaActual === 'ARCHIVO') {
              <p class="detail-card__text">
                El acta está archivada. Para continuar requiere reingreso explícito.
              </p>
              @if (actaPuedeReingresarDesdeBackend()) {
                <div class="archivo-actions">
                  <button
                    mat-flat-button
                    type="button"
                    color="primary"
                    [disabled]="ejecutandoArchivoAccion() !== null"
                    (click)="reingresarActa()"
                  >
                    @if (ejecutandoArchivoAccion() === 'REINGRESAR') {
                      Reingresando...
                    } @else {
                      Reingresar acta
                    }
                  </button>
                </div>
              } @else {
                <p class="detail-card__hint">
                  El reingreso ya fue consumido para esta acta archivada.
                </p>
              }
            } @else if (actaPuedeArchivarseDesdeBackend()) {
              <p class="detail-card__text">
                El acta puede archivarse desde esta instancia operativa.
              </p>
              <div class="archivo-actions">
                <button
                  mat-stroked-button
                  type="button"
                  [disabled]="ejecutandoArchivoAccion() !== null"
                  (click)="archivarActa()"
                >
                  @if (ejecutandoArchivoAccion() === 'ARCHIVAR') {
                    Archivando...
                  } @else {
                    Archivar acta
                  }
                </button>
              </div>
            }
          </section>
          }

          @if (puedeMostrarBloqueGestionExterna()) {
          <section class="detail-card" aria-labelledby="gestion-externa-titulo">
            <h3 id="gestion-externa-titulo" class="detail-card__title">Gestión externa</h3>
            @if (gestionExternaMensaje() || gestionExternaError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (gestionExternaMensaje()) {
                  <p class="demo-feedback-ok">{{ gestionExternaMensaje() }}</p>
                }
                @if (gestionExternaError()) {
                  <p class="demo-feedback-error">{{ gestionExternaError() }}</p>
                }
              </div>
            }
            <dl class="detail-dl detail-dl--compact">
              <dt>Bandeja actual</dt>
              <dd>{{ etiquetaBandeja(d.bandejaActual) }}</dd>
              <dt>Estado proceso</dt>
              <dd>{{ d.estadoProcesoActual }}</dd>
              @if (d.accionPendiente) {
                <dt>Acción pendiente</dt>
                <dd>{{ d.accionPendiente }}</dd>
              }
              @if (d.tipoGestionExterna) {
                <dt>Tipo gestión externa</dt>
                <dd>{{ d.tipoGestionExterna }}</dd>
              }
            </dl>
            @if (d.bandejaActual === 'GESTION_EXTERNA') {
              <p class="detail-card__text">
                El acta está en gestión externa. Para continuar internamente debe retornar.
              </p>
              @if (actaPuedeRetornarDesdeGestionExternaDesdeBackend()) {
                <div class="gestion-externa-actions">
                  <button
                    mat-flat-button
                    type="button"
                    color="primary"
                    [disabled]="ejecutandoGestionExternaAccion() !== null"
                    (click)="retornarGestionExterna()"
                  >
                    @if (ejecutandoGestionExternaAccion() === 'RETORNAR') {
                      Retornando...
                    } @else {
                      Retornar de gestión externa
                    }
                  </button>
                </div>
              } @else {
                <p class="detail-card__hint">
                  El retorno no está habilitado por el backend para esta acta.
                </p>
              }
            } @else if (actaPuedeDerivarseAGestionExternaDesdeBackend()) {
              <p class="detail-card__text">
                El acta puede derivarse a gestión externa desde esta instancia.
              </p>
              <div class="gestion-externa-actions">
                <button
                  mat-stroked-button
                  type="button"
                  color="primary"
                  [disabled]="ejecutandoGestionExternaAccion() !== null"
                  (click)="derivarGestionExterna('APREMIO')"
                >
                  @if (ejecutandoGestionExternaAccion() === 'APREMIO') {
                    {{ etiquetaDerivacionGestionExternaEnCurso('APREMIO') }}
                  } @else {
                    {{ etiquetaDerivacionGestionExterna('APREMIO') }}
                  }
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  color="primary"
                  [disabled]="ejecutandoGestionExternaAccion() !== null"
                  (click)="derivarGestionExterna('JUZGADO_DE_PAZ')"
                >
                  @if (ejecutandoGestionExternaAccion() === 'JUZGADO_DE_PAZ') {
                    {{ etiquetaDerivacionGestionExternaEnCurso('JUZGADO_DE_PAZ') }}
                  } @else {
                    {{ etiquetaDerivacionGestionExterna('JUZGADO_DE_PAZ') }}
                  }
                </button>
              </div>
            }
          </section>
          }

          @if (puedeMostrarBloqueNotificacionActa()) {
          <section class="detail-card" aria-labelledby="notificacion-acta-titulo">
            <h3 id="notificacion-acta-titulo" class="detail-card__title">Notificación del acta</h3>
            @if (notificacionMensaje() || notificacionError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (notificacionMensaje()) {
                  <p class="demo-feedback-ok">{{ notificacionMensaje() }}</p>
                }
                @if (notificacionError()) {
                  <p class="demo-feedback-error">{{ notificacionError() }}</p>
                }
              </div>
            }
            @if (actaEsNotificableDesdeBackend()) {
              <p class="detail-card__text">
                Registrar resultado de notificación (mock). No libera bloqueantes materiales ni cierra el acta.
              </p>
              <div class="notificacion-actions">
                <button
                  mat-stroked-button
                  type="button"
                  color="primary"
                  [disabled]="notificandoResultado() !== null"
                  (click)="registrarResultadoNotificacion('POSITIVA')"
                >
                  @if (notificandoResultado() === 'POSITIVA') {
                    Notificando...
                  } @else {
                    {{ etiquetaResultadoNotificacion('POSITIVA') }}
                  }
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  [disabled]="notificandoResultado() !== null"
                  (click)="registrarResultadoNotificacion('NEGATIVA')"
                >
                  @if (notificandoResultado() === 'NEGATIVA') {
                    Notificando...
                  } @else {
                    {{ etiquetaResultadoNotificacion('NEGATIVA') }}
                  }
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  [disabled]="notificandoResultado() !== null"
                  (click)="registrarResultadoNotificacion('VENCIDA')"
                >
                  @if (notificandoResultado() === 'VENCIDA') {
                    Notificando...
                  } @else {
                    {{ etiquetaResultadoNotificacion('VENCIDA') }}
                  }
                </button>
              </div>
            }
          </section>
          }

          @if (puedeMostrarPagoVoluntario()) {
          <section class="detail-card" aria-labelledby="pago-voluntario-titulo">
            <h3 id="pago-voluntario-titulo" class="detail-card__title">Pago voluntario</h3>
            <p class="detail-card__hint">
              La validación final la realiza el backend; la pantalla muestra las
              acciones disponibles según la situación de pago informada.
            </p>
            <dl class="detail-dl detail-dl--compact">
              <dt>Situación de pago</dt>
              <dd>{{ d.situacionPago }}</dd>
              @if (d.cerrabilidad?.resultadoFinal) {
                <dt>Resultado final</dt>
                <dd>{{ d.cerrabilidad.resultadoFinal }}</dd>
              }
              @if (d.pagoInformado) {
                @if (formatoFechaHora(d.pagoInformado.fechaInformado); as fechaPago) {
                  <dt>Fecha informado</dt>
                  <dd>{{ fechaPago }}</dd>
                }
                @if (d.pagoInformado.comprobante) {
                  <dt>Comprobante (mock)</dt>
                  <dd>{{ d.pagoInformado.comprobante.nombreArchivo }} ({{ d.pagoInformado.comprobante.id }})</dd>
                }
              }
            </dl>
            @if (pagoMensaje() || pagoError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (pagoMensaje()) {
                  <p class="demo-feedback-ok">{{ pagoMensaje() }}</p>
                }
                @if (pagoError()) {
                  <p class="demo-feedback-error">{{ pagoError() }}</p>
                }
              </div>
            }
            @if (situacionPagoSinValorDesdeBackend()) {
              <p class="detail-card__hint">
                Sin situación de pago informada por el backend.
              </p>
            } @else if (situacionPagoDesconocidaDesdeBackend()) {
              <p class="detail-card__hint">
                Situación de pago desconocida para la UI; no se muestran acciones.
              </p>
            } @else if (!hayAccionesPagoDisponibles()) {
              <p class="detail-empty">
                No hay acciones de pago disponibles para la situación actual.
              </p>
            } @else {
              <div class="pago-actions">
                @for (accion of accionesPagoDisponiblesDesdeBackend(); track accion) {
                  <button
                    mat-stroked-button
                    type="button"
                    [color]="accion === 'SOLICITAR' || accion === 'CONFIRMAR' ? 'primary' : undefined"
                    [disabled]="ejecutandoPagoAccion() !== null"
                    (click)="ejecutarAccionPago(accion)"
                  >
                    @if (ejecutandoPagoAccion() === accion) {
                      {{ etiquetaAccionPagoEnCurso(accion) }}
                    } @else {
                      {{ etiquetaAccionPago(accion) }}
                    }
                  </button>
                }
              </div>
            }
          </section>
          }

          <section class="detail-card" aria-labelledby="lectura-operativa-titulo">
            <h3 id="lectura-operativa-titulo" class="detail-card__title">Lectura operativa</h3>
            @if (lecturaOperativa(d); as lectura) {
              <p class="detail-card__text">{{ lectura }}</p>
            } @else {
              <p class="detail-empty">Sin lectura operativa informada.</p>
            }
          </section>

          @if (actaMuestraSeccionPiezasRedaccion(d)) {
          <section class="detail-card" aria-labelledby="piezas-redaccion-titulo">
            <h3 id="piezas-redaccion-titulo" class="detail-card__title">Piezas de redacción</h3>
            @if (nulidadMensaje() || nulidadError() || medidaPreventivaMensaje() || medidaPreventivaError() || notificacionActaMensaje() || notificacionActaError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (nulidadMensaje()) {
                  <p class="demo-feedback-ok">{{ nulidadMensaje() }}</p>
                }
                @if (nulidadError()) {
                  <p class="demo-feedback-error">{{ nulidadError() }}</p>
                }
                @if (medidaPreventivaMensaje()) {
                  <p class="demo-feedback-ok">{{ medidaPreventivaMensaje() }}</p>
                }
                @if (medidaPreventivaError()) {
                  <p class="demo-feedback-error">{{ medidaPreventivaError() }}</p>
                }
                @if (notificacionActaMensaje()) {
                  <p class="demo-feedback-ok">{{ notificacionActaMensaje() }}</p>
                }
                @if (notificacionActaError()) {
                  <p class="demo-feedback-error">{{ notificacionActaError() }}</p>
                }
              </div>
            }
            @if (d.piezasRequeridas?.length) {
              <p class="detail-subtitle detail-subtitle--minor">Piezas requeridas</p>
              <ul class="detail-list">
                @for (pieza of d.piezasRequeridas; track pieza) {
                  <li>{{ pieza }}</li>
                }
              </ul>
            } @else {
              <p class="detail-empty detail-empty--inline">Sin piezas requeridas declaradas.</p>
            }
            @if (d.piezasGeneradas?.length) {
              <p class="detail-subtitle detail-subtitle--minor">Piezas generadas</p>
              <ul class="detail-list">
                @for (pieza of d.piezasGeneradas; track pieza) {
                  <li>{{ pieza }}</li>
                }
              </ul>
            } @else {
              <p class="detail-empty detail-empty--inline">Ninguna pieza generada aún.</p>
            }
            @if (hayAccionesRedaccionDisponiblesDesdeBackend()) {
              <div class="detail-piezas-resumen__actions">
                @if (actaPuedeGenerarNulidadDesdeBackend()) {
                  <button
                    mat-stroked-button
                    type="button"
                    color="primary"
                    [disabled]="accionRedaccionEnCurso()"
                    (click)="generarNulidad()"
                  >
                    @if (generandoNulidad()) {
                      Generando nulidad...
                    } @else {
                      Generar nulidad
                    }
                  </button>
                }
                @if (actaPuedeGenerarMedidaPreventivaDesdeBackend()) {
                  <button
                    mat-stroked-button
                    type="button"
                    color="primary"
                    [disabled]="accionRedaccionEnCurso()"
                    (click)="generarMedidaPreventiva()"
                  >
                    @if (generandoMedidaPreventiva()) {
                      Generando medida preventiva...
                    } @else {
                      Generar medida preventiva
                    }
                  </button>
                }
                @if (actaPuedeGenerarNotificacionActaDesdeBackend()) {
                  <button
                    mat-stroked-button
                    type="button"
                    color="primary"
                    [disabled]="accionRedaccionEnCurso()"
                    (click)="generarNotificacionActa()"
                  >
                    @if (generandoNotificacionActa()) {
                      Generando notificación del acta...
                    } @else {
                      Generar notificación del acta
                    }
                  </button>
                }
              </div>
            } @else {
              <p class="detail-hint">No hay acciones de redacción disponibles para esta acta.</p>
            }
          </section>
          }

          <section class="detail-card" aria-labelledby="documentos-titulo">
            <h3 id="documentos-titulo" class="detail-card__title">Documentos</h3>
            @if (firmaDocumentoMensaje() || firmaDocumentoError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (firmaDocumentoMensaje()) {
                  <p class="demo-feedback-ok">{{ firmaDocumentoMensaje() }}</p>
                }
                @if (firmaDocumentoError()) {
                  <p class="demo-feedback-error">{{ firmaDocumentoError() }}</p>
                }
              </div>
            }
            @if (documentosEstado() === 'loading') {
              <div class="subresource-loading">
                <mat-spinner diameter="28"></mat-spinner>
                <span>Cargando documentos...</span>
              </div>
            } @else if (documentosEstado() === 'error') {
              <div class="subresource-error">
                <p>No se pudieron cargar los documentos.</p>
                <p>{{ documentosError() }}</p>
                <button mat-button type="button" (click)="recargarDocumentos()">Reintentar</button>
              </div>
            } @else if (documentos().length === 0) {
              <p class="detail-empty">Sin documentos registrados.</p>
            } @else {
              <ul class="doc-list">
                @for (doc of documentos(); track doc.id) {
                  <li class="doc-item">
                    <div class="doc-item__head">
                      <span class="doc-item__id">{{ doc.id }}</span>
                      <span class="badge badge-doc-tipo">{{ doc.tipoDocumento }}</span>
                      <span class="badge badge-doc-estado">{{ doc.estadoDocumento }}</span>
                    </div>
                    <dl class="detail-dl detail-dl--compact doc-item__meta">
                      <dt>Archivo</dt>
                      <dd>{{ doc.nombreArchivo }}</dd>
                    </dl>
                    @if (documentoEsFirmable(doc)) {
                      <div class="doc-item__actions">
                        <button
                          mat-stroked-button
                          type="button"
                          color="primary"
                          [disabled]="firmandoDocumentoId() !== null"
                          (click)="firmarDocumento(doc)"
                        >
                          @if (firmandoDocumentoId() === doc.id) {
                            Firmando...
                          } @else {
                            Firmar
                          }
                        </button>
                      </div>
                    }
                  </li>
                }
              </ul>
            }
          </section>

          <section class="detail-card" aria-labelledby="eventos-titulo">
            <h3 id="eventos-titulo" class="detail-card__title">Eventos / trazabilidad</h3>
            @if (eventosEstado() === 'loading') {
              <div class="subresource-loading">
                <mat-spinner diameter="28"></mat-spinner>
                <span>Cargando eventos...</span>
              </div>
            } @else if (eventosEstado() === 'error') {
              <div class="subresource-error">
                <p>No se pudieron cargar los eventos.</p>
                <p>{{ eventosError() }}</p>
                <button mat-button type="button" (click)="recargarEventos()">Reintentar</button>
              </div>
            } @else if (eventos().length === 0) {
              <p class="detail-empty">Sin eventos registrados.</p>
            } @else {
              <ol class="event-timeline">
                @for (evento of eventos(); track evento.id) {
                  <li class="event-item">
                    <div class="event-item__head">
                      <span class="badge badge-event-tipo">{{ evento.tipoEvento }}</span>
                      @if (formatoFechaHora(evento.fechaHora); as fecha) {
                        <time class="event-item__fecha" [attr.datetime]="evento.fechaHora">{{ fecha }}</time>
                      }
                    </div>
                    @if (evento.descripcion) {
                      <p class="event-item__desc">{{ evento.descripcion }}</p>
                    }
                    @if (transicionEvento(evento); as transicion) {
                      <p class="event-item__transicion">{{ transicion }}</p>
                    }
                    <p class="event-item__id">{{ evento.id }}</p>
                  </li>
                }
              </ol>
            }
          </section>

          <section class="detail-card" aria-labelledby="bloqueantes-titulo">
            <h3 id="bloqueantes-titulo" class="detail-card__title">Pendientes / bloqueantes</h3>
            @if (cumplimientoMaterialMensaje() || cumplimientoMaterialError()) {
              <div class="subresource-feedback" role="status" aria-live="polite">
                @if (cumplimientoMaterialMensaje()) {
                  <p class="demo-feedback-ok">{{ cumplimientoMaterialMensaje() }}</p>
                }
                @if (cumplimientoMaterialError()) {
                  <p class="demo-feedback-error">{{ cumplimientoMaterialError() }}</p>
                }
              </div>
            }
            @if (pendientesBloqueantes(d).length > 0) {
              <ul class="detail-list">
                @for (pendiente of pendientesBloqueantes(d); track pendiente) {
                  <li>
                    <span class="badge badge-warn">{{ pendiente }}</span>
                    @if (puedeMostrarAccionCumplimientoMaterial(pendiente)) {
                      <button
                        mat-stroked-button
                        type="button"
                        color="primary"
                        [disabled]="cumpliendoMaterial() !== null"
                        (click)="cumplirMaterialmente(pendiente)"
                      >
                        @if (cumpliendoMaterial() === pendiente) {
                          Registrando...
                        } @else {
                          {{ etiquetaCumplimientoMaterial(pendiente) }}
                        }
                      </button>
                    }
                  </li>
                }
              </ul>
              @if (d.cerrabilidad?.motivoNoCerrable) {
                <p class="detail-card__hint">{{ d.cerrabilidad.motivoNoCerrable }}</p>
              }
            } @else {
              <p class="detail-empty">Sin bloqueantes activos informados.</p>
            }
          </section>

          <section class="detail-card" aria-labelledby="hechos-titulo">
            <h3 id="hechos-titulo" class="detail-card__title">Hechos materiales</h3>
            @if (ejesHechosMateriales(d).length > 0) {
              <ul class="detail-ejes">
                @for (eje of ejesHechosMateriales(d); track eje.clave) {
                  <li class="detail-eje">
                    <div class="detail-eje__head">
                      <strong>{{ eje.etiqueta }}</strong>
                      <span class="badge badge-neutral">{{ eje.fase }}</span>
                    </div>
                    <dl class="detail-dl detail-dl--compact">
                      <dt>Eje</dt>
                      <dd>{{ eje.clave }}</dd>
                      <dt>Bloquea cierre</dt>
                      <dd>{{ etiquetaSiNo(eje.bloqueaCierre) }}</dd>
                      @if (eje.ejeBloqueanteCierre) {
                        <dt>Eje bloqueante</dt>
                        <dd>{{ eje.ejeBloqueanteCierre }}</dd>
                      }
                    </dl>
                    @if (eje.descripcion) {
                      <p class="detail-card__text">{{ eje.descripcion }}</p>
                    }
                  </li>
                }
              </ul>
            } @else {
              <p class="detail-empty">Sin hechos materiales informados.</p>
            }
          </section>

          @if (d.resumenHecho) {
            <section class="detail-card detail-card--muted" aria-labelledby="resumen-hecho-titulo">
              <h3 id="resumen-hecho-titulo" class="detail-card__title">Resumen del hecho</h3>
              <p class="detail-card__text">{{ d.resumenHecho }}</p>
            </section>
          }
          }
        }
      </aside>
    </div>
  </mat-sidenav-content>
</mat-sidenav-container>
'''

OUT.write_text(HTML, encoding="utf-8", newline="\n")
lines = HTML.count("\n") + (0 if HTML.endswith("\n") else 1)
print(f"Wrote {OUT}")
print(f"Bytes: {OUT.stat().st_size}")
print(f"Lines: {lines}")

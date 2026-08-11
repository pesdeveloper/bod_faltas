import { Component, OnInit } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KeyboardAction, KeyboardActionService } from './keyboard-action.service';
import { F8ActionDirective } from './f8-action.directive';

@Component({
  template: `<div [appF8Action]="action">contenido</div>`,
  imports: [F8ActionDirective],
  standalone: true,
})
class TestHostComponent implements OnInit {
  fn = jasmine.createSpy('fn');
  action!: KeyboardAction;
  ngOnInit(): void {
    this.action = { id: 'test-f8', label: 'Guardar', fn: this.fn, enabled: true };
  }
}

describe('F8ActionDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let svc: KeyboardActionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    svc = TestBed.inject(KeyboardActionService);
  });

  function pressF8(): void {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it('registers the action on init', () => {
    expect(svc.getCurrentAction()?.id).toBe('test-f8');
  });

  it('executes the action when F8 is pressed and enabled=true', () => {
    pressF8();
    expect(fixture.componentInstance.fn).toHaveBeenCalledTimes(1);
  });

  it('does not execute when enabled=false', () => {
    fixture.componentInstance.action = {
      ...fixture.componentInstance.action,
      enabled: false,
    };
    svc.register(fixture.componentInstance.action);
    pressF8();
    expect(fixture.componentInstance.fn).not.toHaveBeenCalled();
  });

  it('unregisters when component is destroyed', () => {
    fixture.destroy();
    expect(svc.getCurrentAction()).toBeNull();
  });
});

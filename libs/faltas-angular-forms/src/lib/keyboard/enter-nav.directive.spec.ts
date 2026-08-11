import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EnterNavDirective } from './enter-nav.directive';

@Component({
  template: `
    <form appEnterNav>
      <input id="i1" type="text" />
      <input id="i2" type="text" />
      <input id="i3" type="text" />
    </form>
  `,
  imports: [EnterNavDirective],
  standalone: true,
})
class TestHostComponent {}

describe('EnterNavDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    host = fixture.nativeElement as HTMLElement;
  });

  function focusAndEnter(el: HTMLElement): void {
    el.focus();
    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
  }

  function focusAndShiftEnter(el: HTMLElement): void {
    el.focus();
    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true }));
    fixture.detectChanges();
  }

  it('creates the directive', () => {
    expect(host.querySelector('form')).toBeTruthy();
  });

  it('advances focus from first to second input on Enter', () => {
    const inputs = host.querySelectorAll('input');
    focusAndEnter(inputs[0] as HTMLElement);
    expect(document.activeElement).toBe(inputs[1]);
  });

  it('advances focus from second to third input on Enter', () => {
    const inputs = host.querySelectorAll('input');
    focusAndEnter(inputs[1] as HTMLElement);
    expect(document.activeElement).toBe(inputs[2]);
  });

  it('wraps around from last to first on Enter', () => {
    const inputs = host.querySelectorAll('input');
    focusAndEnter(inputs[2] as HTMLElement);
    expect(document.activeElement).toBe(inputs[0]);
  });

  it('moves focus backward on Shift+Enter', () => {
    const inputs = host.querySelectorAll('input');
    focusAndShiftEnter(inputs[1] as HTMLElement);
    expect(document.activeElement).toBe(inputs[0]);
  });

  it('does not navigate in textarea (native newline)', () => {
    const ta = document.createElement('textarea');
    host.querySelector('form')!.appendChild(ta);
    fixture.detectChanges();
    const inputs = host.querySelectorAll('input');
    ta.focus();
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    ta.dispatchEvent(event);
    fixture.detectChanges();
    expect(event.defaultPrevented).toBeFalse();
    ta.remove();
  });
});

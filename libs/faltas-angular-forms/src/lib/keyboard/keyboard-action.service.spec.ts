import { TestBed } from '@angular/core/testing';
import { KeyboardAction, KeyboardActionService } from './keyboard-action.service';

describe('KeyboardActionService', () => {
  let svc: KeyboardActionService;

  const makeAction = (overrides: Partial<KeyboardAction> = {}): KeyboardAction => ({
    id: 'test-action',
    label: 'Test',
    fn: jasmine.createSpy('fn'),
    enabled: true,
    ...overrides,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({});
    svc = TestBed.inject(KeyboardActionService);
  });

  it('should be created', () => {
    expect(svc).toBeTruthy();
  });

  it('returns null when no action registered', () => {
    expect(svc.getCurrentAction()).toBeNull();
  });

  it('registers and returns the current action', () => {
    const a = makeAction();
    svc.register(a);
    expect(svc.getCurrentAction()).toBe(a);
  });

  it('unregisters only the matching id', () => {
    const a = makeAction({ id: 'a' });
    svc.register(a);
    svc.unregister('b');
    expect(svc.getCurrentAction()).toBe(a);
    svc.unregister('a');
    expect(svc.getCurrentAction()).toBeNull();
  });

  it('executes the action when enabled and not loading', () => {
    const fn = jasmine.createSpy('fn');
    svc.register(makeAction({ fn, enabled: true, loading: false }));
    svc.execute();
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('does NOT execute when enabled=false', () => {
    const fn = jasmine.createSpy('fn');
    svc.register(makeAction({ fn, enabled: false }));
    svc.execute();
    expect(fn).not.toHaveBeenCalled();
  });

  it('does NOT execute when loading=true', () => {
    const fn = jasmine.createSpy('fn');
    svc.register(makeAction({ fn, enabled: true, loading: true }));
    svc.execute();
    expect(fn).not.toHaveBeenCalled();
  });

  it('does NOT throw when execute is called with no registered action', () => {
    expect(() => svc.execute()).not.toThrow();
  });

  it('last register wins (single-slot registry)', () => {
    const a = makeAction({ id: 'a' });
    const b = makeAction({ id: 'b' });
    svc.register(a);
    svc.register(b);
    expect(svc.getCurrentAction()).toBe(b);
  });
});

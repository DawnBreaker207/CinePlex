import { TestBed } from '@angular/core/testing';
import { FormattedCountdownPipe } from './formatted-countdown.pipe';

describe('FormattedCountdownPipe', () => {
  it('create an instance', () => {
    const pipe = TestBed.runInInjectionContext(() => new FormattedCountdownPipe());
    expect(pipe).toBeTruthy();
  });
});

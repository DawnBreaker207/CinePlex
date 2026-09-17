import { TestBed } from '@angular/core/testing';
import { IsDeletedPipe } from './is-deleted.pipe';

describe('IsDeletedPipe', () => {
  it('create an instance', () => {
    const pipe = TestBed.runInInjectionContext(() => new IsDeletedPipe());
    expect(pipe).toBeTruthy();
  });
});

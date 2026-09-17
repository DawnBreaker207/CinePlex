import { TestBed } from '@angular/core/testing';
import { CurrencyPipe } from '@angular/common';
import { CurrencyFormatPipe } from './currency-format.pipe';

describe('CurrencyFormatPipe', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [CurrencyPipe] }));
  it('create an instance', () => {
    const pipe = TestBed.runInInjectionContext(() => new CurrencyFormatPipe());
    expect(pipe).toBeTruthy();
  });
});

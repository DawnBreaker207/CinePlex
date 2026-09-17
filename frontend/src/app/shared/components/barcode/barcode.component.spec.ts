import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BarcodeComponent } from './barcode.component';

import { testProviders } from '@shared/testing/test-providers';

describe('BarcodeComponent', () => {
  let component: BarcodeComponent;
  let fixture: ComponentFixture<BarcodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BarcodeComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(BarcodeComponent);
    component = fixture.componentInstance;
        fixture.componentRef.setInput('value', 'test-code');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

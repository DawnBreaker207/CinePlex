import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentResultComponent } from './payment-result.component';

import { testProviders } from '@shared/testing/test-providers';

describe('PaymentResultComponent', () => {
  let component: PaymentResultComponent;
  let fixture: ComponentFixture<PaymentResultComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentResultComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentResultComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

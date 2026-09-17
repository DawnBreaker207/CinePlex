import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentChartComponent } from './payment-chart.component';

import { testProviders } from '@shared/testing/test-providers';

describe('PaymentChartComponent', () => {
  let component: PaymentChartComponent;
  let fixture: ComponentFixture<PaymentChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentChartComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

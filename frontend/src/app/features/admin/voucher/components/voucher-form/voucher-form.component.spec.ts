import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormVoucherComponent } from './voucher-form.component';

describe('FormVoucherComponent', () => {
  let component: FormVoucherComponent;
  let fixture: ComponentFixture<FormVoucherComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormVoucherComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FormVoucherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
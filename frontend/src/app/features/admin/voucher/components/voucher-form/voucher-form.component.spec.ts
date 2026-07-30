import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VoucherFormComponent } from './voucher-form.component';

describe('VoucherFormComponent', () => {
  let component: VoucherFormComponent;
  let fixture: ComponentFixture<VoucherFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoucherFormComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(VoucherFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

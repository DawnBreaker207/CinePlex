import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VoucherListComponent } from './voucher-list.component';

import { testProviders } from '@shared/testing/test-providers';

describe('VoucherListComponent', () => {
  let component: VoucherListComponent;
  let fixture: ComponentFixture<VoucherListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoucherListComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(VoucherListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SeatIconVipComponent } from './seat-vip.component';

import { testProviders } from '@shared/testing/test-providers';

describe('SeatIconVipComponent', () => {
  let component: SeatIconVipComponent;
  let fixture: ComponentFixture<SeatIconVipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeatIconVipComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatIconVipComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

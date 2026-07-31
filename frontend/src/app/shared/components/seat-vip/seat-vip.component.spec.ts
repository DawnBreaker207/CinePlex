import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SeatIconVipComponent } from './seat-vip.component';

describe('SeatIconVipComponent', () => {
  let component: SeatIconVipComponent;
  let fixture: ComponentFixture<SeatIconVipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeatIconVipComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatIconVipComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

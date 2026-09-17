import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SeatIconComponent } from './seat.component';

import { testProviders } from '@shared/testing/test-providers';

describe('SeatIconComponent', () => {
  let component: SeatIconComponent;
  let fixture: ComponentFixture<SeatIconComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeatIconComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatIconComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

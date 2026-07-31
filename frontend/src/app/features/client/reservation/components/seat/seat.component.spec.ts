import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SeatComponent } from './seat.component';

import { testProviders } from '@shared/testing/test-providers';

describe('SeatComponent', () => {
  let component: SeatComponent;
  let fixture: ComponentFixture<SeatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeatComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(SeatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

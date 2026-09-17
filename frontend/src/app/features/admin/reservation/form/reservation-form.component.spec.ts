import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormReservationComponent } from './reservation-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('FormReservationComponent', () => {
  let component: FormReservationComponent;
  let fixture: ComponentFixture<FormReservationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormReservationComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FormReservationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

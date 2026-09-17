import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormShowtimeComponent } from './showtime-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('FormComponent', () => {
  let component: FormShowtimeComponent;
  let fixture: ComponentFixture<FormShowtimeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormShowtimeComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FormShowtimeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

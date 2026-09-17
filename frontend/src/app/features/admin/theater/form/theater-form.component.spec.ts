import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormTheaterComponent } from './theater-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('FormTheaterComponent', () => {
  let component: FormTheaterComponent;
  let fixture: ComponentFixture<FormTheaterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormTheaterComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FormTheaterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

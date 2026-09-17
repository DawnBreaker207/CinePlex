import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormMovieComponent } from './movie-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('FormComponent', () => {
  let component: FormMovieComponent;
  let fixture: ComponentFixture<FormMovieComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormMovieComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FormMovieComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

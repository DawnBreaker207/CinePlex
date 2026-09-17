import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormMovieAPIComponent } from './api-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('FormMovieAPIComponent', () => {
  let component: FormMovieAPIComponent;
  let fixture: ComponentFixture<FormMovieAPIComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormMovieAPIComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FormMovieAPIComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

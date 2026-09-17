import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Error500Component } from './error-500.component';

import { testProviders } from '@shared/testing/test-providers';

describe('Error500Component', () => {
  let component: Error500Component;
  let fixture: ComponentFixture<Error500Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Error500Component],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(Error500Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

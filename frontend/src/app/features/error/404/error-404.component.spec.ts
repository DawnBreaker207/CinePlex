import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Error404Component } from './error-404.component';

import { testProviders } from '@shared/testing/test-providers';

describe('Error404Component', () => {
  let component: Error404Component;
  let fixture: ComponentFixture<Error404Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Error404Component],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(Error404Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

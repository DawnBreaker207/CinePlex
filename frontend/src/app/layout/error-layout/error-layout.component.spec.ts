import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ErrorLayoutComponent } from './error-layout.component';

import { testProviders } from '@shared/testing/test-providers';

describe('ErrorLayoutComponent', () => {
  let component: ErrorLayoutComponent;
  let fixture: ComponentFixture<ErrorLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorLayoutComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TheaterComponent } from './theater.component';

import { testProviders } from '@shared/testing/test-providers';

describe('TheaterComponent', () => {
  let component: TheaterComponent;
  let fixture: ComponentFixture<TheaterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TheaterComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(TheaterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CountdownComponent } from './countdown.component';

import { testProviders } from '@shared/testing/test-providers';

describe('CountdownComponent', () => {
  let component: CountdownComponent;
  let fixture: ComponentFixture<CountdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CountdownComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(CountdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

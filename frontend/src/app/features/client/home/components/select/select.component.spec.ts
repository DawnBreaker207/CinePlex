import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectShowtimeComponent } from './select.component';

import { testProviders } from '@shared/testing/test-providers';

describe('SelectShowtimeComponent', () => {
  let component: SelectShowtimeComponent;
  let fixture: ComponentFixture<SelectShowtimeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectShowtimeComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectShowtimeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

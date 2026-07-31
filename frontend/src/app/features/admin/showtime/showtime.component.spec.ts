import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShowtimeComponent } from './showtime.component';

import { testProviders } from '@shared/testing/test-providers';

describe('ShowtimeComponent', () => {
  let component: ShowtimeComponent;
  let fixture: ComponentFixture<ShowtimeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShowtimeComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ShowtimeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

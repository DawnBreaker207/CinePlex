import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MovieChartComponent } from './movie-chart.component';

import { testProviders } from '@shared/testing/test-providers';

describe('MovieChartComponent', () => {
  let component: MovieChartComponent;
  let fixture: ComponentFixture<MovieChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovieChartComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(MovieChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

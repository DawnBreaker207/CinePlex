import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MovieComponent } from './movie.component';

import { testProviders } from '@shared/testing/test-providers';

describe('MovieComponent', () => {
  let component: MovieComponent;
  let fixture: ComponentFixture<MovieComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovieComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(MovieComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

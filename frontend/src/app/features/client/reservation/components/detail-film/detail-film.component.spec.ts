import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetailFilmComponent } from './detail-film.component';

import { testProviders } from '@shared/testing/test-providers';

describe('DetailFilmComponent', () => {
  let component: DetailFilmComponent;
  let fixture: ComponentFixture<DetailFilmComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetailFilmComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(DetailFilmComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

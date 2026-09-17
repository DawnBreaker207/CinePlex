import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { MovieTableComponent } from './movie-table.component';

import { testProviders } from '@shared/testing/test-providers';

describe('MovieTableComponent', () => {
  let component: MovieTableComponent;
  let fixture: ComponentFixture<MovieTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovieTableComponent],
      providers: [
        ...testProviders,
        {
          provide: TranslateService,
          useValue: {
            instant: (k: string) => k,
            get: (k: string) => of(k),
            getCurrentLang: () => 'vi',
            getFallbackLang: () => 'vi',
            onTranslationChange: of(null),
            onLangChange: of(null),
            onFallbackLangChange: of(null),
            onDefaultLangChange: of(null),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MovieTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

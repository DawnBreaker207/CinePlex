import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { TheaterTableComponent } from './theater-table.component';

describe('TheaterTableComponent', () => {
  let component: TheaterTableComponent;
  let fixture: ComponentFixture<TheaterTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TheaterTableComponent],
      providers: [
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

    fixture = TestBed.createComponent(TheaterTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

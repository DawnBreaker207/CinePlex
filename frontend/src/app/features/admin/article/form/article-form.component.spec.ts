import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArticleFormComponent } from './article-form.component';

import { testProviders } from '@shared/testing/test-providers';

describe('ArticleFormComponent', () => {
  let component: ArticleFormComponent;
  let fixture: ComponentFixture<ArticleFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArticleFormComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ArticleFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

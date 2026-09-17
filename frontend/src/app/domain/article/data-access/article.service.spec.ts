import { TestBed } from '@angular/core/testing';

import { ArticleService } from './article.service';

import { testProviders } from '@shared/testing/test-providers';

describe('ArticleService', () => {
  let service: ArticleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ArticleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

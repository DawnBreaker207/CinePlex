import { TestBed } from '@angular/core/testing';

import { UploadService } from './upload.service';

import { testProviders } from '@shared/testing/test-providers';

describe('UploadService', () => {
  let service: UploadService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(UploadService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

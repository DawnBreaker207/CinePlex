import { TestBed } from '@angular/core/testing';

import { ChatboxService } from './chatbox.service';

import { testProviders } from '@shared/testing/test-providers';

describe('ChatboxService', () => {
  let service: ChatboxService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ChatboxService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

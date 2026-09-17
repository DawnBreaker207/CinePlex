import { SocketService } from './socket.service';
import { TestBed } from '@angular/core/testing';

import { testProviders } from '@shared/testing/test-providers';

describe('SocketService', () => {
  let service: SocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(SocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { EMPTY, map, retry, shareReplay } from 'rxjs';
import { SocketConfig } from '@core/config/socket.config';
import { environment } from '@env/environment';

@Injectable({
  providedIn: 'root',
})
export class SocketService extends RxStomp {
  constructor() {
    super();
    this.configure(SocketConfig);
    if (!environment.useMock) {
      this.activate();
    }
  }

  watchEvent<T>(topic: string) {
    if (environment.useMock) {
      return EMPTY;
    }
    return this.watch(topic).pipe(
      map((msg) => {
        return JSON.parse(msg.body) as T;
      }),
      retry({ delay: 2000, count: 3 }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  send(destination: string, body: unknown) {
    this.publish({ destination, body: JSON.stringify(body) });
  }

  disconnect() {
    this.deactivate();
  }
}

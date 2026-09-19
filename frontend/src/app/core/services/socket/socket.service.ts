import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { map, retry, shareReplay } from 'rxjs';
import { SocketConfig } from '@core/config/socket.config';

@Injectable({
  providedIn: 'root',
})
export class SocketService extends RxStomp {
  constructor() {
    super();
    this.configure(SocketConfig);
    this.activate();
  }

  watchEvent<T>(topic: string) {
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

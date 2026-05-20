import { inject, Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

@Injectable({
  providedIn: 'root',
})
export class SseService {
  URL = `${environment.apiUrl}/notification/subscribe/showtime`;
  private zone = inject(NgZone);

  connect(id: string | number, userId: number) {
    return new Observable((observer) => {
      // Seat channel
      const clientId = `${userId}_${Math.random().toString(36).substring(3, 9)}`;

      const url = `${this.URL}/${id}?clientId=${clientId}`;

      const source = new EventSource(url);

      const handleEvent = (eventName: string, data: string) => {
        this.zone.run(() => {
          try {
            const parsedData = JSON.parse(data);
            observer.next({ event: eventName, data: parsedData });
          } catch (e) {
            observer.next({ event: eventName, data });
          }
        });
      };

      source.addEventListener('SEAT_STATE_INIT', (e: MessageEvent) =>
        handleEvent('SEAT_STATE_INIT', e.data),
      );
      source.addEventListener('SEAT_HOLD', (e: MessageEvent) =>
        handleEvent('SEAT_HOLD', e.data),
      );
      source.addEventListener('SEAT_RELEASE', (e: MessageEvent) =>
        handleEvent('SEAT_RELEASE', e.data),
      );

      source.addEventListener('CONNECTED', (e: MessageEvent) =>
        console.log('SSE Handshake: connected'),
      );

      // ✅ Optional: thêm handler fallback cho các event khác
      source.onmessage = (event) => {
        this.zone.run(() => {
          observer.next({
            event: 'message',
            data: JSON.parse(event.data),
          });
        });
      };

      source.onerror = (error) => {
        if (source.readyState === EventSource.CLOSED) {
          console.error(
            'SSE Connection was closed. Browser will attempt to reconnect...',
          );
        } else if (source.readyState === EventSource.CONNECTING) {
          console.warn('SSE Connection lost. Reconnecting...');
        }
      };

      // Clean up
      return () => {
        console.log('Closing SSE connection for client: ', clientId);
        source?.close();
      };
    });
  }
}

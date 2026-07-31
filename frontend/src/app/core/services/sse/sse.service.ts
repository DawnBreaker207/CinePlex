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
    if (environment.useMock) {
      return this.mockConnect(id, userId);
    }
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

      source.addEventListener('CONNECTED', () => {});

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
        source?.close();
      };
    });
  }

  /** Mock: phát sự kiện giả lập — một người dùng khác giữ 1 ghế, rồi nhả ra. */
  private mockConnect(id: string | number, _userId: number): Observable<any> {
    return new Observable((observer) => {
      const timers: ReturnType<typeof setTimeout>[] = [];

      const emit = (event: string, data: unknown, delayMs: number) => {
        timers.push(
          setTimeout(() => {
            this.zone.run(() => observer.next({ event, data }));
          }, delayMs),
        );
      };

      const seatIds = Array.from({ length: 80 }, (_, i) => i + 1 + Number(id) * 1000);
      const randomSeat = () =>
        seatIds[Math.floor(Math.random() * seatIds.length)];

      // Simulated other-user holds a seat at ~5s, releases at ~20s
      const heldSeat = randomSeat();
      emit(
        'SEAT_HOLD',
        { seatIds: [{ seatId: heldSeat, reservationId: 'other-user-res' }] },
        5000,
      );
      emit(
        'SEAT_RELEASE',
        { seatIds: [heldSeat] },
        20000,
      );

      return () => timers.forEach((t) => clearTimeout(t));
    });
  }
}

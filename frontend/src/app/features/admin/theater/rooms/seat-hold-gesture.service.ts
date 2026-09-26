import { Injectable, signal } from '@angular/core';
import { SeatTemplate } from '@domain/theater/models/seat-template.model';

// Press-and-hold timer/pointer bookkeeping; selection rules stay in RoomsComponent.
@Injectable()
export class SeatHoldGestureService {
  readonly holdSeatId = signal<number | null>(null);
  readonly holdPct = signal(0);

  private suppressClick = false;
  private holdTimer: ReturnType<typeof setTimeout> | null = null;
  private holdTick: ReturnType<typeof setInterval> | null = null;
  private holdStart: { x: number; y: number } | null = null;

  start(seat: SeatTemplate, event: PointerEvent, onComplete: (seat: SeatTemplate) => void) {
    if (event.pointerType === 'mouse' && event.button !== 0) return;
    this.clear();
    this.holdStart = { x: event.clientX, y: event.clientY };
    this.holdSeatId.set(seat.id);
    this.holdPct.set(0);
    const started = Date.now();
    this.holdTick = setInterval(() => {
      this.holdPct.set(
        Math.min(100, Math.round(((Date.now() - started) / 500) * 100)),
      );
    }, 50);
    this.holdTimer = setTimeout(() => {
      this.clear();
      this.suppressClick = true;
      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        try {
          navigator.vibrate(15);
        } catch {
          // haptics best-effort only
        }
      }
      onComplete(seat);
    }, 500);
  }

  move(event: PointerEvent) {
    if (!this.holdStart || !this.holdTimer) return;
    const dx = event.clientX - this.holdStart.x;
    const dy = event.clientY - this.holdStart.y;
    if (dx * dx + dy * dy > 576) this.clear();
  }

  end() {
    this.clear();
  }

  // Swallow the click that follows a completed press-and-hold.
  consumeSuppressedClick(): boolean {
    const value = this.suppressClick;
    this.suppressClick = false;
    return value;
  }

  private clear() {
    if (this.holdTimer) {
      clearTimeout(this.holdTimer);
      this.holdTimer = null;
    }
    if (this.holdTick) {
      clearInterval(this.holdTick);
      this.holdTick = null;
    }
    this.holdStart = null;
    this.holdSeatId.set(null);
    this.holdPct.set(0);
  }
}

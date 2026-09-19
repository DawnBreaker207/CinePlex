import { effect, inject, Injectable, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { selectUserId } from '@core/auth/auth.selectors';

const KEY = 'cineplex.favorites';

@Injectable({ providedIn: 'root' })
export class FavoritesService {
  private store = inject(Store);
  private readonly userIdSignal = this.store.selectSignal(selectUserId);

  readonly ids = signal<number[]>([]);

  constructor() {
    // Per-account list to avoid mixing on shared browsers; keep legacy keys.
    effect(() => {
      this.ids.set(this.read(this.keyFor(this.userIdSignal())));
    });
  }

  toggle(id: number) {
    const next = this.ids().includes(id)
      ? this.ids().filter((x) => x !== id)
      : [...this.ids(), id];
    this.ids.set(next);
    try {
      localStorage.setItem(this.keyFor(this.userIdSignal()), JSON.stringify(next));
    } catch {
      // Storage full/blocked: keep memory state, skip persist
    }
  }

  isFavorite(id: number) {
    return this.ids().includes(id);
  }

  private keyFor(userId: number | null | undefined): string {
    return userId ? `${KEY}.${userId}` : KEY;
  }

  private read(key: string): number[] {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }
}

import { Injectable, signal } from '@angular/core';

const KEY = 'cineplex.favorites';

@Injectable({ providedIn: 'root' })
export class FavoritesService {
  readonly ids = signal<number[]>(this.read());

  toggle(id: number) {
    const next = this.ids().includes(id)
      ? this.ids().filter((x) => x !== id)
      : [...this.ids(), id];
    this.ids.set(next);
    localStorage.setItem(KEY, JSON.stringify(next));
  }

  isFavorite(id: number) {
    return this.ids().includes(id);
  }

  private read(): number[] {
    try {
      const raw = localStorage.getItem(KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }
}

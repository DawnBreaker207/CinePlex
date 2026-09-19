import { inject, Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Insight } from '@domain/ai/models/insight.model';

const STORAGE_KEY = 'cineplex.insights.read';
const keyOf = (i: Insight) => `${i.type}|${i.detail}`;

@Injectable({ providedIn: 'root' })
export class InsightService {
  private http = inject(HttpClient);
  URL = `${environment.apiUrl}/ai/insights`;

  private readonly state = signal<Insight[]>([]);
  readonly insights = this.state.asReadonly();

  private readonly readKeys = signal<Set<string>>(this.loadReadKeys());
  readonly unread = computed(() =>
    this.state().filter((i) => !this.readKeys().has(keyOf(i))),
  );

  load(): Observable<void> {
    return this.http.get<ApiRes<Insight[]>>(this.URL).pipe(
      map((res) => this.state.set(res.data ?? [])),
      catchError(() => {
        this.state.set([]);
        return of(undefined);
      }),
    );
  }

  markRead(insight: Insight): void {
    this.readKeys.update((s) => {
      const next = new Set(s);
      next.add(keyOf(insight));
      return next;
    });
    this.persist();
  }

  markAllRead(): void {
    this.readKeys.update((s) => {
      const next = new Set(s);
      this.state().forEach((i) => next.add(keyOf(i)));
      return next;
    });
    this.persist();
  }

  private loadReadKeys(): Set<string> {
    try {
      const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as string[];
      return new Set(raw.filter((k) => typeof k === 'string'));
    } catch {
      return new Set();
    }
  }

  private persist(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...this.readKeys()]));
  }
}
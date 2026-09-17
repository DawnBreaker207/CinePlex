import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  private loadingCount = signal(0);

  isLoading = () => this.loadingCount() > 0;

  show() {
    this.loadingCount.update((v) => v + 1);
  }

  hide() {
    this.loadingCount.update((v) => Math.max(v - 1));
  }
}

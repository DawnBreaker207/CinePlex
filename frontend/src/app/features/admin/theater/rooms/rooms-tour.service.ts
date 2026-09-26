import { Injectable, computed, signal } from '@angular/core';

// Onboarding state (guide + 3-step tour) for the room editor, scoped per RoomsComponent.
@Injectable()
export class RoomsTourService {
  readonly guideVisible = signal(
    typeof localStorage === 'undefined' ||
      localStorage.getItem('rooms-guide-dismissed') !== '1',
  );

  readonly tourStep = signal<number | null>(null);
  // @if treats 0 as falsy, so templates read this instead of tourStep().
  readonly tourIndex = computed(() => this.tourStep() ?? -1);

  private done(): boolean {
    try {
      return localStorage.getItem('rooms-tour-done') === '1';
    } catch {
      return false;
    }
  }

  startTour() {
    // Guide and tour card never show together.
    this.guideVisible.set(false);
    this.tourStep.set(0);
  }

  skipTour() {
    this.tourStep.set(null);
    try {
      localStorage.setItem('rooms-tour-done', '1');
    } catch {
      // storage may be unavailable — section just reappears
    }
  }

  nextTourStep() {
    const step = this.tourStep();
    if (step === null) return;
    if (step >= 2) {
      this.skipTour();
      return;
    }
    this.tourStep.set(step + 1);
  }

  maybeStart() {
    if (this.done()) {
      this.tourStep.set(null);
      return;
    }
    this.guideVisible.set(false);
    this.tourStep.set(0);
  }

  advance(step: number) {
    if (this.tourStep() === step) {
      this.tourStep.set(step + 1 > 2 ? null : step + 1);
    }
    if (this.tourStep() === null && step === 2) {
      try {
        localStorage.setItem('rooms-tour-done', '1');
      } catch {
        // storage may be unavailable — section just reappears
      }
    }
  }

  dismissGuide() {
    this.guideVisible.set(false);
    try {
      localStorage.setItem('rooms-guide-dismissed', '1');
    } catch {
      // storage may be unavailable — section just reappears
    }
  }
}

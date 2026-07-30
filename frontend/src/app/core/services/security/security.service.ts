import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SecurityService {
  private document = inject(DOCUMENT);

  disableInspect() {
    this.document.addEventListener('contextmenu', (e) => e.preventDefault());

    this.document.addEventListener('keydown', (e: KeyboardEvent) => {
      const isCmdOrCtrl = e.ctrlKey || e.metaKey;
      const key = e.key.toLowerCase();

      if (
        key === 'f12' ||
        (isCmdOrCtrl &&
          (['u', 's'].includes(key) ||
            (e.shiftKey && ['i', 'j', 'c'].includes(key))))
      ) {
        e.preventDefault();
        e.stopPropagation();
      }
    });
  }
}

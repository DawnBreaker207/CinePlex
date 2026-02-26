import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SecurityService {
  disableInspect() {
    // Disable right click
    document.addEventListener('contextmenu', (e) => e.preventDefault());

    // Disable shortcut
    document.addEventListener('keydown', (e: KeyboardEvent) => {
      // Support Mac and Win
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

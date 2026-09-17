import { inject, isDevMode, provideAppInitializer } from '@angular/core';
import { SecurityService } from '@core/services/security/security.service';

export function provideAppSecurity() {
  return [
    provideAppInitializer(() => {
      const securityService = inject(SecurityService);
      if (!isDevMode()) {
        securityService.disableInspect();
      }
    }),
  ];
}

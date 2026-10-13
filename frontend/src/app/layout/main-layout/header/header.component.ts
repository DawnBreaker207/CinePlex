import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NzHeaderComponent } from 'ng-zorro-antd/layout';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { AuthService } from '@core/auth/auth.service';
import { AuthMenuComponent } from '@shared/components/auth-menu/auth-menu.component';
import { SelectShowtimeComponent } from '@features/client/home/components/select/select.component';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADERS } from '@core/constants/column';

@Component({
  selector: 'app-header',
  imports: [
    RouterLink,
    RouterLinkActive,
    NzHeaderComponent,
    NzButtonModule,
    AuthMenuComponent,
    SelectShowtimeComponent,
    NzIconModule,
    TranslatePipe,
  ],
  templateUrl: './header.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  authService = inject(AuthService);

  headers = HEADERS;
  mobileOpen = signal(false);
}

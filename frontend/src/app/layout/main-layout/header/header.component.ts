import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { NzHeaderComponent } from 'ng-zorro-antd/layout';
import { NzMenuDirective } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AuthService } from '@core/auth/auth.service';
import { AuthMenuComponent } from '@shared/components/auth-menu/auth-menu.component';
import { SelectShowtimeComponent } from '@features/client/home/components/select/select.component';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADERS } from '@core/constants/column';

@Component({
  selector: 'app-header',
  imports: [
    UpperCasePipe,
    RouterLink,
    NzButtonComponent,
    NzHeaderComponent,
    NzMenuDirective,
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
}

import {
  Component,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzBadgeComponent } from 'ng-zorro-antd/badge';
import {
  NzBreadCrumbComponent,
  NzBreadCrumbItemComponent,
} from 'ng-zorro-antd/breadcrumb';
import {
  NzDropDownDirective,
  NzDropdownMenuComponent,
} from 'ng-zorro-antd/dropdown';
import { NzHeaderComponent } from 'ng-zorro-antd/layout';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { AuthMenuComponent } from '@shared/components/auth-menu/auth-menu.component';
import { Breadcrumb } from '../admin-layout.component';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-navbar',
  imports: [
    NzBadgeComponent,
    NzBreadCrumbComponent,
    NzBreadCrumbItemComponent,
    NzDropDownDirective,
    NzDropdownMenuComponent,
    NzHeaderComponent,
NzIconDirective,
NzToolTipModule,
AuthMenuComponent,
    RouterLink,
    TranslatePipe,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  breadcrumbs = input<Breadcrumb[]>([]);
}

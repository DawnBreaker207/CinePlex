import { Component, computed, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { Store } from '@ngrx/store';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { selectUserId } from '@core/auth/auth.selectors';
import { AuthActions } from '@core/auth/auth.actions';
import { AuthService } from '@core/auth/auth.service';
import { UserStore } from '@domain/user/data-access/user.store';
import { SettingsService } from '@core/services/settings/settings.service';
import { TranslatePipe } from '@ngx-translate/core';
import { AppSwitchComponent } from '@shared/components/app-switch/app-switch.component';
import { PROFILE_MENU } from '@core/constants/column';

interface MenuItem {
  title: string;
  icon: string;
  link?: string;
  action?: () => void;
  role: 'admin' | 'user' | 'both';
  isDanger?: boolean;
}

@Component({
  selector: 'app-auth-menu',
  imports: [
    LucideAngularModule,
    NzDropDownModule,
    NzAvatarModule,
    AppSwitchComponent,
    TranslatePipe,
  ],
  templateUrl: './auth-menu.component.html',
  styleUrls: ['./auth-menu.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthMenuComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  readonly authService = inject(AuthService);
  readonly settings = inject(SettingsService);
  readonly userStore = inject(UserStore);

  ngOnInit() {
    const userId = this.store.selectSignal(selectUserId)();
    if (userId) {
      this.userStore.loadUser(userId);
    }
  }

  public readonly profileMenu: MenuItem[] = PROFILE_MENU.map((item) => ({
    ...item,
    action: item.action === 'logout' ? () => this.logout() : undefined,
  })) as MenuItem[];

  filterMenu = computed(() => {
    const user = this.userStore.selectedUser();
    const role = user?.role[0]?.toLowerCase();

    return this.profileMenu.filter((item) => {
      if (item.role === 'both') return true;
      return item.role === role;
    });
  });

  handleItemClick(item: MenuItem) {
    if (item.action) {
      item.action();
    } else if (item.link) {
      this.router.navigate([item.link]);
    }
  }

  login() {
    this.router.navigate(['/login']);
  }

  toggleLang(en: boolean) {
    this.settings.setLang(en ? 'en' : 'vi');
  }

  logout() {
    this.store.dispatch(AuthActions.loadLogout());
  }
}

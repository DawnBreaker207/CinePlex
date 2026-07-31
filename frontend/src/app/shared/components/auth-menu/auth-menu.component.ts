import { Component, computed, effect, inject, ChangeDetectionStrategy } from '@angular/core';
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
export class AuthMenuComponent {
  private store = inject(Store);
  private router = inject(Router);
  readonly authService = inject(AuthService);
  readonly settings = inject(SettingsService);
  readonly userStore = inject(UserStore);
  private readonly userIdSignal = this.store.selectSignal(selectUserId);

  constructor() {
    effect(() => {
      const userId = this.userIdSignal();
      if (userId) {
        this.userStore.loadUser(userId);
      }
    });
  }

  public readonly profileMenu: MenuItem[] = PROFILE_MENU.map((item) => ({
    ...item,
    action: item.action === 'logout' ? () => this.logout() : undefined,
  })) as MenuItem[];

  private roleOf(user: { role?: string | string[] } | null | undefined) {
    const role = user?.role;
    return (Array.isArray(role) ? role[0] : role)?.toLowerCase();
  }

  filterMenu = computed(() => {
    const role = this.roleOf(this.userStore.selectedUser());

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

  readonly isAdmin = computed(
    () => this.roleOf(this.userStore.selectedUser()) === 'admin',
  );

  goAdmin() {
    this.router.navigate(['/admin']);
  }

  toggleLang(en: boolean) {
    this.settings.setLang(en ? 'en' : 'vi');
  }

  logout() {
    this.store.dispatch(AuthActions.loadLogout());
  }
}

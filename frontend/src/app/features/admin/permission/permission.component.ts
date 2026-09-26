import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PermissionStore } from '@domain/permission/data-access/permission.store';
import { PERMISSION_KEY_LABELS, PermissionKey } from '@core/constants/enum';
import { User } from '@domain/user/models/user.model';

const KEY_ICONS: Record<PermissionKey, string> = {
  CONTENT: 'read',
  PRICING: 'tags',
  FINANCE: 'account-book',
  ACCOUNTS: 'security-scan',
};

@Component({
  selector: 'app-permission',
  imports: [
    LoadingComponent,
    NzAlertComponent,
    NzTagModule,
    NzSwitchModule,
    NzIconDirective,
    NzAvatarModule,
    FormsModule,
    TranslatePipe,
  ],
  templateUrl: './permission.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class PermissionComponent implements OnInit {
  readonly store = inject(PermissionStore);
  private modalService = inject(NzModalService);
  private translate = inject(TranslateService);

  readonly keys: PermissionKey[] = ['CONTENT', 'PRICING', 'FINANCE', 'ACCOUNTS'];
  readonly icons = KEY_ICONS;
  readonly labels = PERMISSION_KEY_LABELS;

  ngOnInit() {
    this.store.load();
  }

  hasPerm(userId: number, key: PermissionKey): boolean {
    return (this.store.permsByUser()[userId] ?? []).includes(key);
  }

  roleColor(role: string): string {
    return role === 'OWNER' ? 'volcano' : 'geekblue';
  }

  onToggle(userId: number, key: PermissionKey, checked: boolean) {
    // Privilege changes (either direction) need explicit confirmation.
    // The switch is bound to store state, so cancelling snaps it back.
    const granting = checked;
    this.modalService.confirm({
      nzTitle: this.translate.instant(
        granting ? 'permission.grant.title' : 'permission.revoke.title',
      ),
      nzContent: this.translate.instant(
        granting ? 'permission.grant.content' : 'permission.revoke.content',
      ),
      nzOkText: this.translate.instant('common.confirm'),
      nzOkDanger: !granting,
      nzOnOk: () => this.store.toggle({ userId, key }),
      nzCancelText: this.translate.instant('common.cancel'),
    });
  }

  trackByUser(index: number, user: User): number {
    return user.userId;
  }
}
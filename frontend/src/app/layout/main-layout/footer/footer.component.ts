import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '@core/services/toast/toast.service';

@Component({
  selector: 'app-footer',
  imports: [FormsModule, RouterLink, NzIconModule, TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  private toast = inject(ToastService);
  private translate = inject(TranslateService);
  protected readonly date = new Date();
  readonly subscribed = signal(false);
  email = '';

  onSubscribe(): void {
    if (!this.email.trim()) return;
    this.subscribed.set(true);
  }

  soon(): void {
    this.toast.createNotification({
      type: 'info',
      title: this.translate.instant('common.soon'),
      message: this.translate.instant('common.soonMessage'),
    });
  }
}

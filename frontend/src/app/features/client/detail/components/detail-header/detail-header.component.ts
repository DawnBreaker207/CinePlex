import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '@core/services/toast/toast.service';
import { Movie } from '@domain/movie/models/movie.model';

@Component({
  selector: 'app-detail-header',
  imports: [DatePipe, NzTagModule, NzButtonModule, NzIconModule, TranslatePipe],
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailHeaderComponent {
  readonly movie = input.required<Movie>();
  readonly bookNow = output<void>();
  private toast = inject(ToastService);
  private translate = inject(TranslateService);

  soon(): void {
    this.toast.createNotification({
      type: 'info',
      title: this.translate.instant('common.soon'),
      message: this.translate.instant('common.soonMessage'),
    });
  }
}

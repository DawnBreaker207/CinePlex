import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-error-state',
  imports: [NzAlertComponent, NzButtonComponent, TranslatePipe],
  templateUrl: './error-state.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorStateComponent {
  title = input<string>('common.error');
  message = input<string>('');
  showRetry = input<boolean>(true);
  retryText = input<string>('reservation.retry');
  readonly retry = output<void>();
}

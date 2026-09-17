import {
  Component,
  inject,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzSpinComponent } from 'ng-zorro-antd/spin';
import { LoadingService } from '@core/services/loading/loading.service';
import { SpinnerComponent } from '@shared/components/spinner/spinner.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-loading',
  imports: [NzSpinComponent, SpinnerComponent, TranslatePipe],
  templateUrl: './loading.component.html',
  styleUrl: './loading.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingComponent {
  isLoading = input<boolean | null | undefined>(true);
  tip = input<string>('common.loading');
  protected loadingService = inject(LoadingService);
}

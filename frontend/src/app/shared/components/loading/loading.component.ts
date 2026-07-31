import {
  Component,
  inject,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzSpinComponent } from 'ng-zorro-antd/spin';
import { LoadingService } from '@core/services/loading/loading.service';
import { SpinnerComponent } from '@shared/components/spinner/spinner.component';

@Component({
  selector: 'app-loading',
  imports: [NzSpinComponent, SpinnerComponent],
  templateUrl: './loading.component.html',
  styleUrl: './loading.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingComponent {
  isLoading = input<boolean | null | undefined>(true);
  tip = input<string>('Đang tải...');
  protected loadingService = inject(LoadingService);
}

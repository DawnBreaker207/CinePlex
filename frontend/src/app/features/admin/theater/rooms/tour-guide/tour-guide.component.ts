import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';

// Guide alert + interactive first-run tour card. State lives in the parent.
@Component({
  selector: 'app-tour-guide',
  imports: [NzAlertModule, NzButtonModule, NzIconModule, TranslatePipe],
  templateUrl: './tour-guide.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourGuideComponent {
  readonly guideOn = input.required<boolean>();
  readonly tourIndex = input.required<number>();

  readonly dismissGuide = output<void>();
  readonly skipTour = output<void>();
  readonly nextTourStep = output<void>();
}

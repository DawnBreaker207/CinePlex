import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-dashboard-item',
  imports: [LucideAngularModule, NzIconModule, TranslatePipe],
  templateUrl: './dashboard-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardItemComponent {
  title = input<string>('');
  value = input<string>('');
  icon = input<string>('');
  trend = input<number | null>(null);
}

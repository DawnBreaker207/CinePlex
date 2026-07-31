import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-dashboard-item',
  imports: [LucideAngularModule],
  templateUrl: './dashboard-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardItemComponent {
  title = input<string>('');
  value = input<string>('');
  icon = input<string>('');
}

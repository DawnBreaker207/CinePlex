import { Component, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-dashboard-item',
  imports: [LucideAngularModule],
  templateUrl: './dashboard-item.component.html',

})
export class DashboardItemComponent {
  title = input<string>('');
  value = input<string>('');
  icon = input<string>('');
}

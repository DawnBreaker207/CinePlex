import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

@Component({
  selector: 'app-switch',
  standalone: true,
  templateUrl: './app-switch.component.html',
  styleUrls: ['./app-switch.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppSwitchComponent {
  readonly checked = input(false);
  readonly checkedChange = output<boolean>();

  toggle() {
    this.checkedChange.emit(!this.checked());
  }
}
